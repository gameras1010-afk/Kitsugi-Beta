package com.kitsugi.animelist.data.trailer

import android.content.Context
import android.util.Log
import com.kitsugi.animelist.data.remote.TmdbApiClient
import com.kitsugi.animelist.data.settings.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "TrailerService"
private const val TMDB_TRAILER_FALLBACK_LANGUAGE = "en-US"
private val YOUTUBE_SOURCE_CACHE_TTL: Duration = Duration.ofHours(3)
private val YOUTUBE_VIDEO_ID_REGEX = Regex("^[a-zA-Z0-9_-]{11}$")

/**
 * Application-scoped trailer resolution service.
 *
 * Adapted from KitsugiTV-dev TrailerService. Key differences:
 * - Uses APP's [TmdbApiClient] + [SettingsDataStore] instead of REF's TmdbApi/TmdbSettingsDataStore.
 * - No Hilt — instantiated as a lazy singleton via [TrailerServiceHolder].
 * - Honors tmdbEnabled AND tmdbUseTrailers settings gates (matching APP DataStore keys).
 * - 3-hour TTL YouTube session cache with URL expiry extraction.
 *
 * Usage: TrailerServiceHolder.get(context).getTrailerPlaybackSource(...)
 */
class TrailerService(
    private val context: Context,
    private val inAppYouTubeExtractor: InAppYouTubeExtractor = InAppYouTubeExtractor()
) {
    private val tmdbClient = TmdbApiClient()

    // Cache: "title|year|tmdbId|type" -> TrailerPlaybackSource (NEGATIVE_CACHE sentinel for misses)
    private val cache = ConcurrentHashMap<String, TrailerPlaybackSource>()
    private val NEGATIVE_CACHE = TrailerPlaybackSource(videoUrl = "")

    // Time-bound cache: youtubeVideoId -> resolved playback source (success-only)
    private val youtubeSourceCache = ConcurrentHashMap<String, CachedTrailerPlaybackSource>()

    private data class CachedTrailerPlaybackSource(
        val playbackSource: TrailerPlaybackSource,
        val cachedAt: Instant,
        val expiresAt: Instant? = null
    )

    // ─── Settings helpers ──────────────────────────────────────────────────────

    private suspend fun readSettings() = runCatching {
        SettingsDataStore(context).settingsFlow.first()
    }.getOrNull()

    private suspend fun isTrailersEnabled(): Boolean {
        val settings = readSettings() ?: return true
        return settings.tmdbEnabled && settings.tmdbUseTrailers
    }

    private suspend fun preferredLanguage(): String {
        val settings = readSettings() ?: return TMDB_TRAILER_FALLBACK_LANGUAGE
        return normalizeTrailerLanguage(settings.tmdbLanguage)
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Resolve a trailer by TMDB id (preferred) or title+year.
     * Returns [TrailerPlaybackSource] with video (and optional audio) URL, or null.
     *
     * Respects [SettingsDataStore.tmdbEnabled] and [SettingsDataStore.tmdbUseTrailers] gates.
     */
    suspend fun getTrailerPlaybackSource(
        title: String,
        year: String? = null,
        tmdbId: String? = null,
        type: String? = null
    ): TrailerPlaybackSource? = withContext(Dispatchers.IO) {
        if (!isTrailersEnabled()) {
            Log.d(TAG, "Trailers disabled in settings; skipping lookup")
            return@withContext null
        }

        val cacheKey = "$title|$year|$tmdbId|$type"
        cache[cacheKey]?.let { cached ->
            val hit = cached !== NEGATIVE_CACHE
            Log.d(TAG, "Cache hit for $cacheKey: $hit")
            return@withContext if (hit) cached else null
        }

        try {
            Log.d(TAG, "Searching trailer: title=$title year=$year tmdbId=$tmdbId type=$type")
            val source = resolveFromTmdbId(tmdbId = tmdbId, type = type, title = title, year = year)
            if (source != null) {
                cache[cacheKey] = source
                return@withContext source
            }

            Log.w(TAG, "TMDB path exhausted for $title")
            if (tmdbId != null) cache[cacheKey] = NEGATIVE_CACHE
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching trailer for $title: ${e.message}", e)
            null
        }
    }

    /**
     * Convenience for callers that only need the primary video URL.
     */
    suspend fun getTrailerUrl(
        title: String,
        year: String? = null,
        tmdbId: String? = null,
        type: String? = null
    ): String? = getTrailerPlaybackSource(
        title = title,
        year = year,
        tmdbId = tmdbId,
        type = type
    )?.videoUrl

    /**
     * Returns a bare YouTube watch URL for external player use (no extraction).
     * Useful for the "Dış uygulamada aç" fallback.
     */
    suspend fun getExternalTrailerUrl(tmdbId: String?, type: String?): String? = withContext(Dispatchers.IO) {
        if (!isTrailersEnabled()) return@withContext null
        val numericTmdbId = tmdbId?.toIntOrNull() ?: return@withContext null
        val isMovie = normalizeMediaType(type) != "tv"
        val lang = preferredLanguage()

        try {
            // Use TmdbApiClient.fetchVideos directly — it already handles TR-first, EN fallback
            val (primaryTrailer, _) = tmdbClient.fetchVideos(numericTmdbId, isMovie)
            primaryTrailer
        } catch (e: Exception) {
            Log.w(TAG, "getExternalTrailerUrl failed for tmdbId=$tmdbId: ${e.message}")
            null
        }
    }

    /**
     * Resolve a playable [TrailerPlaybackSource] from a raw YouTube URL.
     * Tries in-app extraction first; falls back to the raw URL as-is for external player.
     */
    suspend fun getTrailerPlaybackSourceFromYouTubeUrl(
        youtubeUrl: String,
        title: String? = null,
        year: String? = null
    ): TrailerPlaybackSource? = withContext(Dispatchers.IO) {
        try {
            val youtubeKey = extractYouTubeVideoId(youtubeUrl)
            if (!youtubeKey.isNullOrBlank()) {
                getValidCachedYoutubeSource(youtubeKey)?.let { cached ->
                    Log.d(TAG, "YouTube cache hit for key=${obfuscate(youtubeKey)}")
                    return@withContext cached
                }
            }

            Log.d(TAG, "Attempting in-app extraction for ${summarizeUrl(youtubeUrl)}")
            val localSource = inAppYouTubeExtractor.extractPlaybackSource(youtubeUrl)
            if (localSource != null) {
                if (!youtubeKey.isNullOrBlank()) {
                    youtubeSourceCache[youtubeKey] = CachedTrailerPlaybackSource(
                        playbackSource = localSource,
                        cachedAt = Instant.now(),
                        expiresAt = extractUrlExpireInstant(localSource)
                    )
                }
                Log.d(TAG, "In-app extraction success (audioPresent=${!localSource.audioUrl.isNullOrBlank()})")
                return@withContext localSource
            }

            // In-app extraction failed — return YouTube URL as-is so Media3 can attempt HLS
            Log.w(TAG, "In-app extraction failed for ${summarizeUrl(youtubeUrl)}; returning raw URL")
            TrailerPlaybackSource(videoUrl = youtubeUrl)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getTrailerPlaybackSourceFromYouTubeUrl error: ${e.message}", e)
            null
        }
    }

    /** Clear all in-memory caches. Useful after settings change. */
    fun clearCache() {
        cache.clear()
        youtubeSourceCache.clear()
        Log.d(TAG, "Cache cleared")
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private suspend fun resolveFromTmdbId(
        tmdbId: String?,
        type: String?,
        title: String?,
        year: String?
    ): TrailerPlaybackSource? {
        val numericId = tmdbId?.toIntOrNull() ?: return null
        val isMovie = normalizeMediaType(type) != "tv"

        Log.d(TAG, "TMDB trailer lookup: tmdbId=$numericId isMovie=$isMovie")
        val (youtubeUrl, _) = try {
            tmdbClient.fetchVideos(numericId, isMovie)
        } catch (e: Exception) {
            Log.w(TAG, "fetchVideos failed: ${e.message}")
            return null
        }

        if (youtubeUrl.isNullOrBlank()) {
            Log.w(TAG, "No YouTube trailer key from TMDB for tmdbId=$numericId")
            return null
        }

        Log.d(TAG, "TMDB resolved YouTube URL: ${summarizeUrl(youtubeUrl)}")
        return getTrailerPlaybackSourceFromYouTubeUrl(
            youtubeUrl = youtubeUrl,
            title = title,
            year = year
        )
    }

    private fun getValidCachedYoutubeSource(youtubeKey: String): TrailerPlaybackSource? {
        val cached = youtubeSourceCache[youtubeKey] ?: return null
        val now = Instant.now()
        val expired = cached.expiresAt?.let { now.isAfter(it) }
            ?: (Duration.between(cached.cachedAt, now) > YOUTUBE_SOURCE_CACHE_TTL)
        if (!expired) return cached.playbackSource
        youtubeSourceCache.remove(youtubeKey, cached)
        return null
    }

    private fun extractUrlExpireInstant(source: TrailerPlaybackSource): Instant? {
        val expireRegex = Regex("/expire/(\\d+)/")
        val match = expireRegex.find(source.videoUrl) ?: return null
        val epoch = match.groupValues[1].toLongOrNull() ?: return null
        return Instant.ofEpochSecond(epoch)
    }

    private fun extractYouTubeVideoId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.matches(YOUTUBE_VIDEO_ID_REGEX)) return trimmed
        return runCatching {
            val uri = URI(trimmed)
            val host = uri.host?.lowercase()?.removePrefix("www.") ?: return@runCatching null
            when {
                host == "youtu.be" -> {
                    uri.path?.trim('/')?.substringBefore('/')?.trim()
                        ?.takeIf { it.matches(YOUTUBE_VIDEO_ID_REGEX) }
                }
                host == "youtube.com" || host.endsWith(".youtube.com") -> {
                    val path = uri.path.orEmpty()
                    val query = uri.rawQuery.orEmpty()
                    if (path.startsWith("/watch")) {
                        query.split("&").asSequence()
                            .mapNotNull { e ->
                                val idx = e.indexOf('=')
                                if (idx <= 0) null else if (e.substring(0, idx) == "v") e.substring(idx + 1) else null
                            }
                            .firstOrNull { it.matches(YOUTUBE_VIDEO_ID_REGEX) }
                    } else {
                        val segments = path.trim('/').split("/")
                        when (segments.firstOrNull()?.lowercase()) {
                            "embed", "shorts", "live" -> segments.getOrNull(1)
                                ?.takeIf { it.matches(YOUTUBE_VIDEO_ID_REGEX) }
                            else -> null
                        }
                    }
                }
                else -> null
            }
        }.getOrNull()
    }

    private fun summarizeUrl(url: String): String = runCatching {
        val uri = URI(url)
        "${uri.host}${uri.path}"
    }.getOrDefault(url.take(80))

    private fun obfuscate(key: String): String =
        if (key.length <= 4) "****" else "***${key.takeLast(4)}"
}

// ─── Internal helpers (package-visible for tests) ─────────────────────────────

internal fun normalizeTrailerLanguage(language: String?): String {
    val normalized = language
        ?.trim()
        ?.replace('_', '-')
        ?.takeIf { it.isNotBlank() }
        ?: return TMDB_TRAILER_FALLBACK_LANGUAGE

    if (normalized.contains('-')) {
        val parts = normalized.split("-", limit = 2)
        val locale = parts[0].lowercase()
        val region = parts.getOrNull(1)?.uppercase()?.takeIf { it.isNotBlank() }
        return if (region != null) "$locale-$region" else locale
    }
    if (normalized.equals("en", ignoreCase = true)) return TMDB_TRAILER_FALLBACK_LANGUAGE
    return normalized.lowercase()
}

internal fun normalizeMediaType(type: String?): String? = when (type?.lowercase()) {
    "movie", "film" -> "movie"
    "tv", "series", "show", "tvshow" -> "tv"
    else -> null
}

// ─── Application-level singleton holder ───────────────────────────────────────

object TrailerServiceHolder {
    @Volatile
    private var instance: TrailerService? = null

    fun get(context: Context): TrailerService =
        instance ?: synchronized(this) {
            instance ?: TrailerService(context.applicationContext).also { instance = it }
        }
}
