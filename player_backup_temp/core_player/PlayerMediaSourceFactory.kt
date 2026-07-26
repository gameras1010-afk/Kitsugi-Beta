package com.kitsugi.animelist.core.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.ts.TsExtractor
import com.kitsugi.animelist.core.player.dvmkv.DolbyVisionCompatibility
import com.kitsugi.animelist.data.cloudstream.CsVideoInterceptorFactory
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.data.settings.Dv7HandlingMode
import com.kitsugi.animelist.data.trailer.YoutubeChunkedDataSourceFactory
import java.util.Locale

/**
 * T1.13 – PlayerMediaSourceFactory
 *
 * Centralises all DataSource / MediaSource construction logic that was previously
 * scattered across [Media3PlayerEngine.prepare].  Every call site describes
 * *what* it wants (headers, subs, caching, parallel-range) and this factory
 * decides *how* to build it – keeping the engine lean and testable.
 */
@OptIn(UnstableApi::class)
class PlayerMediaSourceFactory(
    private val context: Context,
    private val settings: AppSettings
) {

    private val TAG = "PlayerMediaSourceFactory"
    private var customExtractorsFactory: ExtractorsFactory? = null
    private var customSubtitleParserFactory: SubtitleParser.Factory? = null

    fun configureSubtitleParsing(
        extractorsFactory: ExtractorsFactory?,
        subtitleParserFactory: SubtitleParser.Factory?
    ) {
        customExtractorsFactory = extractorsFactory
        customSubtitleParserFactory = subtitleParserFactory
    }

    // ── Extractors ────────────────────────────────────────────────────────────

    /**
     * Builds the [ExtractorsFactory] appropriate for the current DV / HDR mode.
     */
    fun buildExtractorsFactory(
        isConvertToDv81: Boolean,
        stripDvRpu: Boolean,
    ): ExtractorsFactory {
        val base = DefaultExtractorsFactory()
            .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS)
            .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)

        return if (isConvertToDv81 || stripDvRpu || settings.stripHdr10PlusSei) {
            DolbyVisionExtractorsFactory(
                delegate = base,
                config = DolbyVisionConversionConfig(
                    active = isConvertToDv81,
                    forcedMode = -1,
                    preserveMapping = false,
                    dv5Enabled = false,
                    manualDv81 = isConvertToDv81
                ),
                stripDvRpu = stripDvRpu,
                stripHdr10PlusSei = settings.stripHdr10PlusSei
            )
        } else {
            base
        }
    }

    // ── MediaItem ─────────────────────────────────────────────────────────────

    private fun isHlsUrl(videoUrl: String): Boolean {
        if (videoUrl.isBlank()) return false
        val clean = videoUrl.lowercase(Locale.ROOT)
        if (clean.contains(".mp4") || clean.contains(".mkv") || clean.contains(".webm") || clean.contains(".avi")) {
            return clean.contains(".m3u8") || clean.contains("m3u8")
        }
        return clean.contains(".m3u8") || 
               clean.contains("m3u8") ||
               clean.contains("/hls/") ||
               clean.contains("/hls") ||
               clean.contains("master.txt") ||
               clean.contains("playlist.txt") ||
               clean.contains("index-v") ||
               clean.contains("index-a") ||
               clean.contains("imgsapi") ||
               clean.contains("molystream") ||
               clean.contains("macellan") ||
               clean.contains("vmeas") ||
               clean.contains("vidpapi") ||
               clean.contains("hdplayersystem") ||
               clean.contains("imagestoo") ||
               clean.contains("pichive") ||
               clean.contains("dizilla") ||
               clean.contains("/stream/") ||
               clean.contains("/player/") ||
               clean.contains("/embed/")
    }

    /**
     * Constructs a [MediaItem] for [videoUrl] with optional subtitle configurations.
     */
    fun buildMediaItem(
        videoUrl: String,
        subtitles: List<SubtitleInput> = emptyList()
    ): MediaItem {
        val videoUri = Uri.parse(videoUrl)
        val isM3u8  = isHlsUrl(videoUrl)
        val isDash  = videoUrl.contains(".mpd",  ignoreCase = true)
        val isSs    = videoUrl.contains(".ism",   ignoreCase = true)

        val builder = MediaItem.Builder().setUri(videoUri)
        when {
            isM3u8 -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            isDash  -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
            isSs    -> builder.setMimeType(MimeTypes.APPLICATION_SS)
        }

        if (subtitles.isNotEmpty()) {
            val subConfigs = subtitles.map { sub ->
                val mime = guessSubtitleMimeType(sub.url)
                val resolvedUrl = if (sub.url.startsWith("/")) "file://${sub.url}" else sub.url
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(resolvedUrl))
                    .setMimeType(mime)
                    .setLanguage(sub.lang)
                    .setLabel(sub.name)
                    .build()
            }
            builder.setSubtitleConfigurations(subConfigs)
        }
        return builder.build()
    }

    // ── DataSource factory ────────────────────────────────────────────────────

    /**
     * Builds the OkHttp-backed [DataSource.Factory], optionally wrapping it
     * in [ParallelRangeDataSource.Factory] when parallel range is enabled.
     *
     * @param addonName  CloudStream addon name (used to look up the API provider)
     * @param headers    Request headers forwarded to OkHttp interceptors
     * @param videoUrl   Raw video URL (used to build the [ExtractorLink])
     * @param streamTitle Human-readable stream title
     * @param isCS       Whether this is a CloudStream source
     * @param qualityValue Video quality hint (e.g. 1080)
     */
    fun buildDataSourceFactory(
        addonName: String?,
        headers: Map<String, String>,
        videoUrl: String,
        streamTitle: String?,
        isCS: Boolean,
        qualityValue: Int?
    ): androidx.media3.datasource.DataSource.Factory {
        val provider = if (isCS && !addonName.isNullOrBlank()) {
            runCatching {
                com.lagradost.cloudstream3.APIHolder.getApiFromNameNull(addonName)
            }.getOrNull()
        } else null

        @Suppress("DEPRECATION")
        val csLink = if (provider != null) {
            runCatching {
                val isM3u8Link = isHlsUrl(videoUrl)
                com.lagradost.cloudstream3.utils.ExtractorLink(
                    source    = addonName!!,
                    name      = streamTitle ?: addonName,
                    url       = videoUrl,
                    referer   = headers.entries.firstOrNull { it.key.equals("referer", ignoreCase = true) }?.value ?: "",
                    quality   = qualityValue ?: 1080,
                    headers   = headers,
                    extractorData = null,
                    type      = if (isM3u8Link)
                        com.lagradost.cloudstream3.utils.ExtractorLinkType.M3U8
                    else
                        com.lagradost.cloudstream3.utils.ExtractorLinkType.VIDEO,
                    audioTracks = emptyList()
                )
            }.getOrNull()
        } else null

        val videoInterceptor = runCatching {
            csLink?.let { provider?.getVideoInterceptor(it) }
        }.getOrNull()

        val effectiveHeaders = headers.toMutableMap()
        if (!effectiveHeaders.keys.any { it.equals("referer", ignoreCase = true) }) {
            val videoHost = runCatching { Uri.parse(videoUrl).host }.getOrNull()
            val fallbackRef = if (!videoHost.isNullOrBlank()) "https://$videoHost/" else provider?.mainUrl ?: "https://google.com"
            effectiveHeaders["Referer"] = fallbackRef
        }
        if (!effectiveHeaders.keys.any { it.equals("user-agent", ignoreCase = true) }) {
            effectiveHeaders["User-Agent"] = com.lagradost.cloudstream3.network.CloudflareKiller.UNIFIED_USER_AGENT
        }

        val okHttpClient = CsVideoInterceptorFactory.buildClient(
            interceptor = videoInterceptor,
            headers     = effectiveHeaders,
            ignoreSSL   = true
        )
        val base = DefaultDataSource.Factory(context, OkHttpDataSource.Factory(okHttpClient))

        val isHlsOrDash = isHlsUrl(videoUrl) || videoUrl.contains(".mpd", ignoreCase = true)

        return if (settings.parallelRangeEnabled && !isHlsOrDash) {
            Log.d(TAG, "ParallelRangeDataSource enabled (${ParallelRangeDataSource.DEFAULT_PARALLEL_CONNECTIONS} connections)")
            ParallelRangeDataSource.Factory(
                upstreamFactory    = base,
                parallelConnections = ParallelRangeDataSource.DEFAULT_PARALLEL_CONNECTIONS,
                chunkSizeBytes     = ParallelRangeDataSource.DEFAULT_CHUNK_SIZE_BYTES
            )
        } else {
            base
        }
    }

    // ── Complete MediaSource ──────────────────────────────────────────────────

    /**
     * Creates the final [MediaSource] ready to hand to [ExoPlayer.setMediaSource].
     *
     * For YouTube-style streams with a separate [audioUrl], builds a
     * [MergingMediaSource].  Otherwise builds a standard progressive /
     * adaptive source backed by the OkHttp data-source factory.
     */
    fun create(
        videoUrl: String,
        audioUrl: String?,
        headers: Map<String, String>,
        subtitles: List<SubtitleInput>,
        isCS: Boolean,
        addonName: String?,
        streamTitle: String?,
        qualityValue: Int?,
        extractorsFactory: ExtractorsFactory,
    ): MediaSource {
        val mediaItem = buildMediaItem(videoUrl, subtitles)

        // YouTube-style dual-stream (video + audio)
        val activeExtractorsFactory = customExtractorsFactory ?: extractorsFactory
        if (!audioUrl.isNullOrBlank()) {
            Log.d(TAG, "Building MergingMediaSource (YouTube/dual-stream)")
            val factory = DefaultMediaSourceFactory(
                YoutubeChunkedDataSourceFactory(),
                activeExtractorsFactory
            )
            customSubtitleParserFactory?.let {
                factory.setSubtitleParserFactory(it)
            }
            return MergingMediaSource(
                factory.createMediaSource(mediaItem),
                factory.createMediaSource(MediaItem.fromUri(Uri.parse(audioUrl)))
            )
        }

        // Standard progressive / adaptive
        val dsFactory = buildDataSourceFactory(
            addonName   = addonName,
            headers     = headers,
            videoUrl    = videoUrl,
            streamTitle = streamTitle,
            isCS        = isCS,
            qualityValue = qualityValue
        )
        Log.d(TAG, "Building standard MediaSource: $videoUrl")
        val factory = DefaultMediaSourceFactory(dsFactory, activeExtractorsFactory)
        customSubtitleParserFactory?.let {
            factory.setSubtitleParserFactory(it)
        }
        return factory.createMediaSource(mediaItem)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun guessSubtitleMimeType(url: String): String = when {
        url.startsWith("/") || url.startsWith("file://") -> guessLocalSubtitleMime(url.removePrefix("file://"))
        url.contains(".vtt", ignoreCase = true)          -> MimeTypes.TEXT_VTT
        url.contains(".srt", ignoreCase = true)          -> MimeTypes.APPLICATION_SUBRIP
        url.contains(".ass", ignoreCase = true)
            || url.contains(".ssa", ignoreCase = true)   -> MimeTypes.TEXT_SSA
        else                                              -> MimeTypes.TEXT_VTT
    }

    private fun guessLocalSubtitleMime(path: String): String {
        return try {
            java.io.File(path).bufferedReader().use { reader ->
                val head = (1..10).mapNotNull { reader.readLine()?.trim() }.joinToString("\n")
                when {
                    head.contains("[Script Info]") || head.contains("[V4+ Styles]") -> MimeTypes.TEXT_SSA
                    head.contains("WEBVTT")                                          -> MimeTypes.TEXT_VTT
                    head.contains("-->")                                             -> MimeTypes.APPLICATION_SUBRIP
                    else                                                             -> MimeTypes.APPLICATION_SUBRIP
                }
            }
        } catch (e: Exception) {
            when {
                path.endsWith(".vtt", ignoreCase = true)                     -> MimeTypes.TEXT_VTT
                path.endsWith(".ass", ignoreCase = true)
                    || path.endsWith(".ssa", ignoreCase = true)              -> MimeTypes.TEXT_SSA
                else                                                          -> MimeTypes.APPLICATION_SUBRIP
            }
        }
    }
}
