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
        val encodedId = encodePathSegment(id)

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
        manifestUrl: String,
        type: String, // "series" or "movie"
        id: String, // tt1234567:1:5
        extraParams: Map<String, String>? = null
    ): List<SubtitleResponseItem> = withContext(Dispatchers.IO) {
        val normalizedManifest = normalizeManifestUrl(manifestUrl)
        if (normalizedManifest.isBlank()) return@withContext emptyList()

        val withoutFragment = normalizedManifest.substringBefore("#")
        val query = withoutFragment.substringAfter("?", "")
        val path = withoutFragment.substringBefore("?").trimEnd('/')
        val baseUrl = path.removeSuffix("/manifest.json").trimEnd('/')

        // Stremio subtitle URL formatı (NuvioTV SubtitleRepositoryImpl referansı):
        // {baseUrl}/subtitles/{type}/{id}/{videoHash=x&videoSize=y&filename=z}.json
        // NOT: id segmenti encode edilmez (içindeki ':' Stremio ID'nin parçası)
        // NOT: extraParams path segment içine eklenir, query string'e değil
        val encodedType = encodePathSegment(type)
        val extraParamStr = buildSubtitleExtraParams(extraParams)

        val resourceUrl = if (extraParamStr.isNotBlank()) {
            "$baseUrl/subtitles/$encodedType/$id/$extraParamStr.json"
        } else {
            "$baseUrl/subtitles/$encodedType/$id.json"
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
            parts.add("filename=${URLEncoder.encode(it, "UTF-8")}")
        }
        return parts.joinToString("&")
    }
}


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
    val behaviorHints: StreamBehaviorHintsDto?
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
