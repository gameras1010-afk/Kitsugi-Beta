package com.kitsugi.animelist.data.remote

import android.util.Log
import com.google.gson.Gson
import com.kitsugi.animelist.core.network.IPv4FirstDns
import com.kitsugi.animelist.data.local.ManagedAddonEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class AddonStreamClient {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .dns(IPv4FirstDns())
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    /**
     * Normalizes a user-input manifest URL, converting stremio:// to https:// and appending
     * /manifest.json if missing, while preserving query parameters.
     */
    fun normalizeManifestUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""

        val normalizedScheme = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("stremio://") -> "https://${trimmed.removePrefix("stremio://")}"
            else -> "https://$trimmed"
        }

        val withoutFragment = normalizedScheme.substringBefore("#")
        val query = withoutFragment.substringAfter("?", "")
        val path = withoutFragment.substringBefore("?").trimEnd('/')
        val manifestPath = if (path.endsWith("/manifest.json")) {
            path
        } else {
            "$path/manifest.json"
        }

        return if (query.isEmpty()) manifestPath else "$manifestPath?$query"
    }

    suspend fun fetchManifest(manifestUrl: String): ManagedAddonEntity? = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeManifestUrl(manifestUrl)
        if (normalizedUrl.isBlank()) return@withContext null

        if (normalizedUrl.contains("anisub.co")) {
            return@withContext ManagedAddonEntity(
                manifestUrl = normalizedUrl,
                name = "AniSub.co",
                description = "AniSub.co Türkçe Altyazı Eklentisi",
                icon = "https://anisub.co/favicon.ico",
                isEnabled = true,
                orderIndex = 99,
                idPrefixes = null,
                streamTypes = null,
                subtitleTypes = "movie,series"
            )
        }

        val request = Request.Builder()
            .url(normalizedUrl)
            .header("User-Agent", DEFAULT_USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("AddonStreamClient", "HTTP error ${response.code} fetching manifest from $normalizedUrl")
                    return@withContext null
                }
                val bodyString = response.body?.string() ?: return@withContext null
                val manifestMap = gson.fromJson(bodyString, Map::class.java) ?: return@withContext null

                val name = manifestMap["name"] as? String ?: "Bilinmeyen Eklenti"
                val description = manifestMap["description"] as? String
                val icon = manifestMap["icon"] as? String

                // Parse top-level idPrefixes (e.g. ["tt", "kitsu:"])
                @Suppress("UNCHECKED_CAST")
                val idPrefixList = (manifestMap["idPrefixes"] as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.filter { it.isNotBlank() }
                    .orEmpty()
                val idPrefixesJson = if (idPrefixList.isEmpty()) null
                else gson.toJson(idPrefixList)

                // Parse resources array to find "stream" resource types
                // Mirrors KitsugiTV-dev's AddonMapper resource parsing logic
                @Suppress("UNCHECKED_CAST")
                val resourcesList = manifestMap["resources"] as? List<*>
                val streamTypes = resourcesList?.mapNotNull { res ->
                    when (res) {
                        is String -> if (res == "stream") "" else null // bare string resource
                        is Map<*, *> -> {
                            val resName = res["name"] as? String ?: return@mapNotNull null
                            if (resName != "stream") return@mapNotNull null
                            @Suppress("UNCHECKED_CAST")
                            val types = (res["types"] as? List<*>)?.filterIsInstance<String>()
                            types?.joinToString(",") ?: ""
                        }
                        else -> null
                    }
                }?.firstOrNull()

                val subtitleTypes = resourcesList?.mapNotNull { res ->
                    when (res) {
                        is String -> if (res == "subtitles") "movie,series" else null
                        is Map<*, *> -> {
                            val resName = res["name"] as? String ?: return@mapNotNull null
                            if (resName != "subtitles") return@mapNotNull null
                            @Suppress("UNCHECKED_CAST")
                            val types = (res["types"] as? List<*>)?.filterIsInstance<String>()
                            val typesStr = types?.joinToString(",")
                            if (typesStr.isNullOrBlank()) "movie,series" else typesStr
                        }
                        else -> null
                    }
                }?.firstOrNull() ?: run {
                    if (resourcesList?.contains("subtitles") == true) "movie,series" else null
                }

                return@withContext ManagedAddonEntity(
                    manifestUrl = normalizedUrl,
                    name = name,
                    description = description,
                    icon = icon,
                    isEnabled = true,
                    orderIndex = 0,
                    idPrefixes = idPrefixesJson,
                    streamTypes = if (streamTypes == null) null else streamTypes.ifBlank { null },
                    subtitleTypes = if (subtitleTypes == null) null else subtitleTypes.ifBlank { null }
                )
            }
        } catch (e: Exception) {
            Log.e("AddonStreamClient", "Error fetching manifest from $normalizedUrl", e)
            null
        }
    }

    suspend fun fetchStreams(
        manifestUrl: String,
        type: String, // "series" or "movie"
        id: String // tt1234567:1:5
    ): List<StreamResponseItem> = withContext(Dispatchers.IO) {
        val normalizedManifest = normalizeManifestUrl(manifestUrl)
        if (normalizedManifest.isBlank()) return@withContext emptyList()

        val withoutFragment = normalizedManifest.substringBefore("#")
        val query = withoutFragment.substringAfter("?", "")
        val path = withoutFragment.substringBefore("?").trimEnd('/')
        val baseUrl = path.removeSuffix("/manifest.json").trimEnd('/')

        // URL encode path segments exactly like KitsugiTV-dev's encodePathSegment:
        // URLEncoder replaces spaces with '+', but RFC 3986 path segments need '%20'
        val encodedType = encodePathSegment(type)
        val encodedId = id.split(':').map { encodePathSegment(it) }.joinToString(":")

        val resourceUrl = "$baseUrl/stream/$encodedType/$encodedId.json"
        val finalUrl = if (query.isEmpty()) resourceUrl else "$resourceUrl?$query"

        val request = Request.Builder()
            .url(finalUrl)
            .header("User-Agent", DEFAULT_USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("AddonStreamClient", "HTTP error ${response.code} fetching streams from $finalUrl")
                    return@withContext emptyList()
                }
                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val streamResult = gson.fromJson(bodyString, StreamResponseWrapper::class.java)
                return@withContext streamResult?.streams ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("AddonStreamClient", "Error fetching streams from $finalUrl", e)
            emptyList()
        }
    }

    suspend fun fetchSubtitles(
        context: android.content.Context,
        manifestUrl: String,
        type: String, // "series" or "movie"
        id: String, // tt1234567:1:5
        extraParams: Map<String, String>? = null,
        aniListId: Int? = null,
        animeTitle: String? = null,
        episode: Int? = null
    ): List<SubtitleResponseItem> = withContext(Dispatchers.IO) {
        if (manifestUrl.contains("anisub.co")) {
            return@withContext fetchAniSubSubtitles(context, id, aniListId, animeTitle, episode)
        }

        val normalizedManifest = normalizeManifestUrl(manifestUrl)
        if (normalizedManifest.isBlank()) return@withContext emptyList()

        val withoutFragment = normalizedManifest.substringBefore("#")
        val query = withoutFragment.substringAfter("?", "")
        val path = withoutFragment.substringBefore("?").trimEnd('/')
        val baseUrl = path.removeSuffix("/manifest.json").trimEnd('/')

        // Stremio subtitle URL formatı (NuvioTV SubtitleRepositoryImpl referansı):
        // {baseUrl}/subtitles/{type}/{id}/{videoHash=x&videoSize=y&filename=z}.json
        // NOT: id segmenti URL-encode edilmelidir çünkü ':' gibi karakterler yönlendirmeyi bozabilir.
        // NOT: extraParams path segment içine eklenir, query string'e değil
        val encodedType = encodePathSegment(type)
        val encodedId = id.split(':').map { encodePathSegment(it) }.joinToString(":")
        val extraParamStr = buildSubtitleExtraParams(extraParams)

        val resourceUrl = if (extraParamStr.isNotBlank()) {
            "$baseUrl/subtitles/$encodedType/$encodedId/$extraParamStr.json"
        } else {
            "$baseUrl/subtitles/$encodedType/$encodedId.json"
        }
        val finalUrl = if (query.isEmpty()) resourceUrl else "$resourceUrl?$query"
        Log.d("AddonStreamClient", "fetchSubtitles URL: $finalUrl")

        val request = Request.Builder()
            .url(finalUrl)
            .header("User-Agent", DEFAULT_USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("AddonStreamClient", "HTTP error ${response.code} fetching subtitles from $finalUrl")
                    return@withContext emptyList()
                }
                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val subResult = gson.fromJson(bodyString, SubtitleResponseWrapper::class.java)
                return@withContext subResult?.subtitles ?: emptyList()
            }
        } catch (e: java.lang.Exception) {
            Log.e("AddonStreamClient", "Error fetching subtitles from $finalUrl", e)
            emptyList()
        }
    }

    /**
     * Encodes a URL path segment, replacing '+' with '%20' to be RFC 3986-compliant.
     * This matches KitsugiTV-dev's StreamRepositoryImpl.encodePathSegment exactly.
     */
    private fun encodePathSegment(value: String): String {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }

    /**
     * Stremio subtitle extra params path segment oluşturur.
     * NuvioTV SubtitleRepositoryImpl.buildExtraParams() referans alınarak port edildi.
     *
     * Format: "videoHash=abc123&videoSize=1234567890&filename=episode.mkv"
     * Bu string daha sonra: /subtitles/{type}/{id}/{extraParams}.json şeklinde kullanılır.
     */
    private fun buildSubtitleExtraParams(params: Map<String, String>?): String {
        if (params.isNullOrEmpty()) return ""
        val parts = mutableListOf<String>()
        params["videoHash"]?.let { parts.add("videoHash=$it") }
        params["videoSize"]?.let { parts.add("videoSize=$it") }
        params["filename"]?.let {
            // filename değeri URL encode edilmeli (boşluk, Türkçe karakter vb. için)
            parts.add("filename=${encodePathSegment(it)}")
        }
        return parts.joinToString("&")
    }

    private suspend fun fetchAniSubSubtitles(
        context: android.content.Context,
        id: String,
        aniListId: Int?,
        animeTitle: String?,
        episode: Int?
    ): List<SubtitleResponseItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("AddonStreamClient", "fetchAniSubSubtitles: id=$id, aniListId=$aniListId, title=$animeTitle, ep=$episode")
            
            // Step 1: GET https://anisub.co to get Inertia version
            val initRequest = Request.Builder()
                .url("https://anisub.co")
                .header("User-Agent", DEFAULT_USER_AGENT)
                .get()
                .build()
            val initHtml = client.newCall(initRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("AddonStreamClient", "Failed to fetch anisub.co home page: ${response.code}")
                    return@use ""
                }
                response.body?.string() ?: ""
            }
            if (initHtml.isBlank()) return@withContext emptyList()
            
            val versionRegex = """"version"\s*:\s*"([a-f0-9]{32})"""" .toRegex()
            val versionMatch = versionRegex.find(initHtml)
            val version = versionMatch?.groupValues?.getOrNull(1) ?: "13462527c7e340ff00f088b7f91a548a"
            Log.d("AddonStreamClient", "Anisub Inertia version: $version")
            
            // Step 2: Determine search query
            val query = animeTitle?.trim() ?: when {
                id.startsWith("tt") -> ""
                id.startsWith("kitsu:") -> ""
                else -> id.substringBefore(":")
            }.trim()
            
            if (query.isBlank()) {
                Log.w("AddonStreamClient", "Anisub search aborted: empty query")
                return@withContext emptyList()
            }
            
            // Step 3: Search anisub.co
            val searchUrl = "https://anisub.co/tum-altyazilar?q=${URLEncoder.encode(query, "UTF-8")}"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("x-inertia", "true")
                .header("x-inertia-version", version)
                .header("x-requested-with", "XMLHttpRequest")
                .get()
                .build()
                
            val jsonResponse = client.newCall(searchRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("AddonStreamClient", "Search HTTP error: ${response.code}")
                    return@use ""
                }
                response.body?.string() ?: ""
            }
            
            if (jsonResponse.isBlank()) return@withContext emptyList()
            
            val searchResult = try {
                gson.fromJson(jsonResponse, AnisubSearchResponse::class.java)
            } catch (e: Exception) {
                Log.e("AddonStreamClient", "JSON parse error", e)
                null
            }
            
            val items = searchResult?.props?.subtitles?.data ?: return@withContext emptyList()
            Log.d("AddonStreamClient", "Anisub search returned ${items.size} items")
            
            // Step 4: Filter items matching anime and episode
            val filteredItems = items.filter { item ->
                // Check AniList ID if available
                if (aniListId != null && item.mediaInfo?.media_anilist_id != null) {
                    if (item.mediaInfo.media_anilist_id != aniListId) return@filter false
                } else {
                    // Title check fallback
                    val romaji = item.mediaInfo?.title_romaji?.lowercase() ?: ""
                    val english = item.mediaInfo?.title_english?.lowercase() ?: ""
                    val q = query.lowercase()
                    if (!romaji.contains(q) && !english.contains(q)) return@filter false
                }
                
                // Episode check
                if (episode != null) {
                    val releaseName = item.subtitle_release_name ?: ""
                    if (!matchesEpisode(releaseName, episode)) return@filter false
                }
                true
            }
            Log.d("AddonStreamClient", "After filtering: ${filteredItems.size} items remain")
            
            // Step 5: For each filtered item, get the download URL and download/unzip it
            val subItems = mutableListOf<SubtitleResponseItem>()
            for (item in filteredItems) {
                val subId = item.subtitle_id ?: continue
                val releaseName = item.subtitle_release_name ?: "Unknown Release"
                val lang = item.subtitle_language ?: "Turkish"
                
                val dlInfoUrl = "https://anisub.co/api/subtitles/$subId/download"
                val dlInfoRequest = Request.Builder()
                    .url(dlInfoUrl)
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .get()
                    .build()
                
                val dlInfoJson = client.newCall(dlInfoRequest).execute().use { response ->
                    if (!response.isSuccessful) null else response.body?.string()
                } ?: continue
                
                val dlResponse = gson.fromJson(dlInfoJson, AnisubDownloadResponse::class.java)
                if (dlResponse?.success != true || dlResponse.data?.download_url == null) continue
                
                val downloadUrl = dlResponse.data.download_url
                val filename = dlResponse.data.filename ?: "sub.zip"
                
                // Download and unzip to local cache
                val localUrl = downloadAndUnzipSubtitle(context, downloadUrl, subId, filename)
                if (localUrl != null) {
                    subItems.add(
                        SubtitleResponseItem(
                            id = "anisub-$subId",
                            url = localUrl,
                            lang = lang
                        )
                    )
                }
            }
            
            subItems
        } catch (e: Exception) {
            Log.e("AddonStreamClient", "Error in fetchAniSubSubtitles", e)
            emptyList()
        }
    }

    private fun matchesEpisode(releaseName: String, ep: Int): Boolean {
        val name = releaseName.lowercase()
        if (name.contains("paket") || name.contains("batch") || name.contains("complete") || 
            name.contains("sezon") || name.contains("season") || name.contains("tümü") || name.contains("tüm")) {
            return true
        }
        // Check for ranges like 01-13 or 1-135
        val rangeRegex = """(\d+)\s*-\s*(\d+)""".toRegex()
        val ranges = rangeRegex.findAll(name)
        for (m in ranges) {
            val start = m.groupValues[1].toIntOrNull()
            val end = m.groupValues[2].toIntOrNull()
            if (start != null && end != null && ep in start..end) {
                return true
            }
        }
        // Extract all standalone numbers
        val numberRegex = """(?<!\d)\d+(?!\d)""".toRegex()
        val numbers = numberRegex.findAll(name).mapNotNull { it.value.toIntOrNull() }.toList()
        if (numbers.isEmpty()) return true // No episode number in name, probably a movie or general sub
        return ep in numbers
    }

    private fun downloadAndUnzipSubtitle(
        context: android.content.Context,
        downloadUrl: String,
        subId: Long,
        filename: String
    ): String? {
        try {
            val cacheDir = java.io.File(context.cacheDir, "anisub_subs/$subId")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            // If we already extracted subtitles for this subId, find and return it
            val files = cacheDir.listFiles()
            if (files != null && files.isNotEmpty()) {
                val subFile = files.firstOrNull { it.name.endsWith(".ass") || it.name.endsWith(".srt") || it.name.endsWith(".vtt") }
                if (subFile != null) {
                    return "file://${subFile.absolutePath}"
                }
            }

            // Download zip
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .build()
                
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            
            val bytes = response.body?.bytes() ?: return null
            
            // Extract from ZIP
            val zipIn = java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes))
            var entry = zipIn.nextEntry
            var extractedFile: java.io.File? = null
            
            while (entry != null) {
                if (!entry.isDirectory) {
                    val entryName = entry.name.lowercase()
                    if (entryName.endsWith(".ass") || entryName.endsWith(".srt") || entryName.endsWith(".vtt")) {
                        // Get clean file name without folder structure
                        val cleanName = java.io.File(entry.name).name
                        val targetFile = java.io.File(cacheDir, cleanName)
                        targetFile.outputStream().use { out ->
                            zipIn.copyTo(out)
                        }
                        extractedFile = targetFile
                        break // Just take the first valid subtitle file in the zip
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            zipIn.close()
            
            return extractedFile?.let { "file://${it.absolutePath}" }
        } catch (e: Exception) {
            Log.e("AddonStreamClient", "Error downloading/unzipping subtitle from $downloadUrl", e)
            return null
        }
    }
}

private data class AnisubSearchResponse(val props: AnisubProps?)
private data class AnisubProps(val subtitles: AnisubSubtitlesData?)
private data class AnisubSubtitlesData(val data: List<AnisubSubtitleItem>?)
private data class AnisubSubtitleItem(
    val subtitle_id: Long?,
    val subtitle_release_name: String?,
    val subtitle_language: String?,
    val format: String?,
    val mediaInfo: AnisubMediaInfo?
)
private data class AnisubMediaInfo(
    val media_anilist_id: Int?,
    val title_romaji: String?,
    val title_english: String?
)
private data class AnisubDownloadResponse(
    val success: Boolean,
    val data: AnisubDownloadData?,
    val message: String?
)
private data class AnisubDownloadData(
    val download_url: String?,
    val filename: String?,
    val file_size: Long?
)


data class SubtitleResponseWrapper(
    val subtitles: List<SubtitleResponseItem>?
)

data class SubtitleResponseItem(
    val id: String?,
    val url: String?,
    val lang: String?
)

data class StreamResponseWrapper(
    val streams: List<StreamResponseItem>?
)

/**
 * Raw Stremio stream object returned by an addon.
 * `behaviorHints.proxyHeaders.request` carries the HTTP headers needed to play the stream.
 */
data class StreamResponseItem(
    val name: String?,
    val title: String?,
    val url: String?,
    val infoHash: String?,
    val fileIndex: Int?,
    val behaviorHints: StreamBehaviorHintsDto?,
    /** Episode/stream thumbnail URL returned by some addons (e.g. Cloudstream, Torrentio) */
    val thumbnail: String? = null
)

/**
 * Top-level `behaviorHints` object from a Stremio stream response.
 */
data class StreamBehaviorHintsDto(
    val notWebReady: Boolean? = null,
    val bingeGroup: String? = null,
    val filename: String? = null,
    val videoHash: String? = null,
    val videoSize: Long? = null,
    val proxyHeaders: ProxyHeadersDto? = null
)

/** Mirrors `behaviorHints.proxyHeaders` in the Stremio spec. */
data class ProxyHeadersDto(
    /** Headers to attach to every segment/manifest request (e.g. Authorization, Referer). */
    val request: Map<String, String>? = null,
    /** Headers returned by the upstream server — typically not needed by the player. */
    val response: Map<String, String>? = null
)
