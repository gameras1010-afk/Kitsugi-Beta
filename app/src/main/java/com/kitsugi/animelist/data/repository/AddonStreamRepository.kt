package com.kitsugi.animelist.data.repository

import android.content.Context
import android.util.Log
import com.kitsugi.animelist.data.local.AddonPresets
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.remote.AddonStreamClient
import com.kitsugi.animelist.data.remote.DebridResolver
import com.kitsugi.animelist.data.remote.KitsugiIdResolver
import com.kitsugi.animelist.data.remote.StreamResponseItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class AddonStreamRepository(private val context: Context) {
    private val database = KitsugiDatabase.getDatabase(context.applicationContext)
    private val addonDao = database.managedAddonDao()
    private val addonClient = AddonStreamClient()
    private val debridResolver = DebridResolver(context)

    /**
     * Artık varsayılan eklenti seed edilmez.
     * Kullanıcı eklentilerini manuel olarak ekler.
     */
    suspend fun seedPresetsIfEmpty() {
        // No-op: varsayılan eklentiler kaldırıldı.
    }

    /**
     * Fetches stream sources from all active, compatible addons in parallel for the given episode.
     *
     * Resolves both IMDb and Kitsu IDs in a single ARM API call, then routes each addon to the
     * correct video ID based on the ID prefixes it declares in its manifest:
     *   - Addons that support "kitsu:" prefix  → queried with "kitsu:{kitsuId}:{episode}"
     *   - Addons that support "tt" prefix       → queried with "{imdbId}:{season}:{episode}"
     *   - Addons with no prefix restriction     → queried with IMDb ID as a safe default
     *
     * Results from all addons are combined and de-duplicated by infoHash/URL before returning.
     */
    suspend fun getStreamsForEpisode(
        malId: Int?,
        aniListId: Int?,
        season: Int,
        episode: Int,
        tmdbId: Int? = null
    ): List<StreamSource> = coroutineScope {
        // Step 1: Resolve IMDb + Kitsu IDs in one ARM API call
        val resolvedIds = KitsugiIdResolver.resolveIds(malId, aniListId, tmdbId)
        val imdbId   = resolvedIds.imdbId
        val kitsuId  = resolvedIds.kitsuId

        Log.d("AddonStreamRepository", "Resolved IDs → imdb=$imdbId kitsu=$kitsuId")

        if (imdbId == null && kitsuId == null) {
            Log.w("AddonStreamRepository", "Could not resolve any ID for MAL=$malId AniList=$aniListId")
            return@coroutineScope emptyList()
        }

        val imdbVideoId  = imdbId?.let { "$it:$season:$episode" }
        val kitsuVideoId = kitsuId?.let { "kitsu:$it:$episode" }
        val contentType  = "series"

        // Step 2: Build a list of (addon, videoId) pairs — each addon is queried
        // with the video ID format it actually understands.
        data class AddonTask(
            val addonName: String,
            val manifestUrl: String,
            val videoId: String
        )

        val allAddons = addonDao.getEnabledAddons()
        val tasks = mutableListOf<AddonTask>()

        for (addon in allAddons) {
            val prefixesRaw = addon.idPrefixes
            val prefixList = if (!prefixesRaw.isNullOrBlank()) {
                try {
                    com.google.gson.Gson()
                        .fromJson(prefixesRaw, Array<String>::class.java)
                        .filter { it.isNotBlank() }
                } catch (_: Exception) { emptyList() }
            } else emptyList()

            if (prefixList.isEmpty()) {
                if (kitsuVideoId != null && addon.supportsStreamResource(contentType, kitsuVideoId)) {
                    tasks += AddonTask(addon.name, addon.manifestUrl, kitsuVideoId)
                }
                if (imdbVideoId != null && addon.supportsStreamResource(contentType, imdbVideoId)) {
                    tasks += AddonTask(addon.name, addon.manifestUrl, imdbVideoId)
                }
            } else {
                val supportsKitsu = prefixList.any { it == "kitsu:" }
                val supportsImdb  = prefixList.any { it == "tt" }

                // Prefer Kitsu for addons that support it (richer anime metadata)
                if (supportsKitsu && kitsuVideoId != null) {
                    if (addon.supportsStreamResource(contentType, kitsuVideoId)) {
                        tasks += AddonTask(addon.name, addon.manifestUrl, kitsuVideoId)
                        // Also add the IMDb query for Torrentio-style addons that support both
                        if (supportsImdb && imdbVideoId != null && prefixList.any { it == "tt" }) {
                            if (addon.supportsStreamResource(contentType, imdbVideoId)) {
                                tasks += AddonTask(addon.name, addon.manifestUrl, imdbVideoId)
                            }
                        }
                    }
                } else if (supportsImdb && imdbVideoId != null) {
                    if (addon.supportsStreamResource(contentType, imdbVideoId)) {
                        tasks += AddonTask(addon.name, addon.manifestUrl, imdbVideoId)
                    }
                }
            }
        }

        if (tasks.isEmpty()) {
            Log.w("AddonStreamRepository", "No compatible addon/ID pairs found — imdb=$imdbVideoId kitsu=$kitsuVideoId")
            return@coroutineScope emptyList()
        }

        Log.d("AddonStreamRepository", "Querying ${tasks.size} addon/ID task(s) in parallel")

        // Step 3: Query all tasks in parallel
        val deferredStreams = tasks.map { task ->
            async {
                Log.d("AddonStreamRepository", "Fetching streams: ${task.addonName} / ${task.videoId}")
                val streams = addonClient.fetchStreams(task.manifestUrl, contentType, task.videoId)
                streams.map { responseItem ->
                    val nameText = responseItem.name ?: "Bilinmeyen Akış"
                    val titleText = responseItem.title ?: "İsimsiz Dosya"
                    val parsedQuality = StreamSorter.parseQualityFromTitle("$titleText $nameText")
                    val parsedQualityValue = StreamSorter.parseQualityValue(parsedQuality)
                    StreamSource(
                        addonName = task.addonName,
                        name      = nameText,
                        title     = titleText,
                        url       = responseItem.url,
                        infoHash  = responseItem.infoHash,
                        fileIndex = responseItem.fileIndex,
                        requestHeaders = responseItem.behaviorHints?.proxyHeaders?.request,
                        quality   = parsedQuality,
                        qualityValue = parsedQualityValue
                    )
                }
            }
        }

        val allStreams = deferredStreams.awaitAll().flatten()

        // Step 4: De-duplicate by infoHash (or url as fallback) so that addons queried with
        // both IMDb and Kitsu IDs don't return the same torrent twice.
        val seen = LinkedHashSet<String>()
        val deduped = mutableListOf<StreamSource>()
        for (stream in allStreams) {
            val key = stream.infoHash?.lowercase()
                ?: stream.url?.lowercase()
                ?: "${stream.addonName}:${stream.name}:${stream.title}"
            if (seen.add(key)) {
                deduped += stream
            }
        }

        Log.d("AddonStreamRepository", "Total streams: ${allStreams.size} → after de-dup: ${deduped.size}")

        // Step 5: Filter to only streams relevant to this episode (removes Torrentio's
        // whole-season dumps). CS and direct-HTTP streams pass through unfiltered.
        val filtered = EpisodeStreamFilter.filterForEpisode(deduped, season, episode)
        Log.d("AddonStreamRepository", "After episode filter: ${filtered.size}")

        // Step 6: Sort by quality — Cached Debrid > resolution > quality type > size
        val sorted = StreamSorter.sort(filtered)
        Log.d("AddonStreamRepository", "Sorted ${sorted.size} streams, top=${sorted.firstOrNull()?.addonName}")

        sorted
    }

    /**
     * Resolves a torrent stream via Debrid if it's not a direct HTTP link.
     */
    suspend fun resolveStreamUrl(source: StreamSource): String? {
        if (!source.url.isNullOrBlank()) {
            return source.url // Direct link, no debrid required
        }
        val hash = source.infoHash
        if (!hash.isNullOrBlank()) {
            return debridResolver.resolveHash(hash, source.fileIndex)
        }
        return null
    }

    fun getDebridApiKey(): String? = debridResolver.getApiKey()

    fun setDebridApiKey(key: String?) {
        debridResolver.setApiKey(key)
    }
}

data class StreamSource(
    val addonName: String,
    val name: String,
    val title: String,
    val url: String?,
    val infoHash: String?,
    val fileIndex: Int?,
    /** HTTP request headers to attach when playing the stream (from behaviorHints.proxyHeaders.request). */
    val requestHeaders: Map<String, String>? = null,
    val isCS: Boolean = false,
    val quality: String? = null,
    val qualityValue: Int? = null,
    val subtitles: List<com.kitsugi.animelist.core.player.SubtitleInput> = emptyList()
) : java.io.Serializable

/**
 * Checks whether this addon can serve streams for [type] and [videoId].
 *
 * Mirrors KitsugiTV-dev's Addon.supportsStreamResource():
 * - If streamTypes is null/empty → no type filter (legacy addon, accept all)
 * - If idPrefixes is null/empty  → no prefix filter (accept all IDs)
 * - Otherwise, the requested type must be in streamTypes AND videoId must
 *   start with at least one declared prefix.
 */
private fun com.kitsugi.animelist.data.local.ManagedAddonEntity.supportsStreamResource(
    type: String,
    videoId: String
): Boolean {
    if (streamTypes == null && subtitleTypes != null) return false
    // Type check
    val types = streamTypes?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
    if (!types.isNullOrEmpty()) {
        val matches = types.any {
            it.equals(type, ignoreCase = true) ||
            (type.equals("series", ignoreCase = true) && it.equals("anime", ignoreCase = true)) ||
            (type.equals("movie", ignoreCase = true) && (it.equals("anime", ignoreCase = true) || it.equals("animemovie", ignoreCase = true)))
        }
        if (!matches) return false
    }

    // ID prefix check — parse the stored JSON array (e.g. ["tt","kitsu:"])
    val prefixesJson = idPrefixes
    if (!prefixesJson.isNullOrBlank()) {
        try {
            val prefixList = com.google.gson.Gson()
                .fromJson(prefixesJson, Array<String>::class.java)
                .filter { it.isNotBlank() }
            if (prefixList.isNotEmpty() && prefixList.none { videoId.startsWith(it) }) {
                return false
            }
        } catch (_: Exception) {
            // Malformed JSON — fall through and accept
        }
    }

    return true
}
