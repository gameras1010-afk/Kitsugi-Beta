package com.kitsugi.animelist.data.remote

import android.util.Log
import com.kitsugi.animelist.core.network.KitsugiHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object KitsuClient {
    private const val TAG = "KitsuClient"

    suspend fun fetchAnimeDetail(kitsuIdOrSlug: String): KitsugiMediaDetail? = withContext(Dispatchers.IO) {
        val url = "https://kitsu.io/api/edge/anime/$kitsuIdOrSlug"
        executeGet(url)
    }

    suspend fun fetchAnimeDetailByTitle(title: String): KitsugiMediaDetail? = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(title, "UTF-8")
        val url = "https://kitsu.io/api/edge/anime?filter[text]=$encoded&page[limit]=1"
        executeGet(url, isArrayResponse = true)
    }

    private suspend fun executeGet(urlStr: String, isArrayResponse: Boolean = false): KitsugiMediaDetail? {
        try {
            val request = Request.Builder()
                .url(urlStr)
                .header("Accept", "application/vnd.api+json")
                .header("Content-Type", "application/vnd.api+json")
                .build()
            KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP ${response.code} for $urlStr")
                    return null
                }
                val body = response.body?.string() ?: return null
                val root = JSONObject(body)
                val dataObj = if (isArrayResponse) {
                    val arr = root.optJSONArray("data")
                    if (arr != null && arr.length() > 0) arr.getJSONObject(0) else null
                } else {
                    root.optJSONObject("data")
                }
                return dataObj?.let { parseKitsuAnime(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Kitsu: ${e.message}", e)
            return null
        }
    }

    private fun parseKitsuAnime(data: JSONObject): KitsugiMediaDetail {
        val id = data.optString("id", "")
        val attributes = data.getJSONObject("attributes")

        val canonicalTitle = attributes.optString("canonicalTitle", "")
        val titlesObj = attributes.optJSONObject("titles")
        val titleEnglish = titlesObj?.optString("en", "").takeIf { !it.isNullOrBlank() } ?: canonicalTitle
        val titleRomaji = titlesObj?.optString("en_jp", "").takeIf { !it.isNullOrBlank() } ?: canonicalTitle
        val titleNative = titlesObj?.optString("ja_jp", "").takeIf { !it.isNullOrBlank() }

        val synopsis = attributes.optString("synopsis", "")
        val statusVal = attributes.optString("status", "").toTurkishStatus()
        val startDate = attributes.optString("startDate", "")
        val endDate = attributes.optString("endDate", "")
        val yearVal = startDate.take(4).toIntOrNull()

        val episodeCount = attributes.optInt("episodeCount", 0)
        val episodeLength = attributes.optInt("episodeLength", 0)
        val duration = if (episodeLength > 0) "$episodeLength dk" else null

        val averageRating = attributes.optString("averageRating", "0")
        val ratingDouble = averageRating.toDoubleOrNull() ?: 0.0
        val scoreVal = if (ratingDouble > 0) (ratingDouble / 10.0).toInt().coerceIn(1, 10) else null

        val posterObj = attributes.optJSONObject("posterImage")
        val imageUrl = posterObj?.optString("medium") ?: posterObj?.optString("original")

        val youtubeVideoId = attributes.optString("youtubeVideoId", "")
        val trailerUrl = if (youtubeVideoId.isNotEmpty()) "https://www.youtube.com/watch?v=$youtubeVideoId" else null

        val links = mutableListOf<KitsugiExternalLink>()
        links.add(KitsugiExternalLink("Kitsu", "https://kitsu.io/anime/$id", "EN"))

        return KitsugiMediaDetail(
            synopsis = synopsis,
            status = statusVal,
            titleEnglish = titleEnglish,
            titleRomaji = titleRomaji,
            titleNative = titleNative,
            title = canonicalTitle,
            imageUrl = imageUrl,
            score = scoreVal,
            year = yearVal,
            total = if (episodeCount > 0) episodeCount else null,
            episodeDuration = duration,
            startDate = startDate,
            endDate = endDate,
            trailerUrl = trailerUrl,
            externalLinks = links
        )
    }

    private fun String.toTurkishStatus(): String {
        return when (this.lowercase()) {
            "current" -> "Devam Ediyor"
            "finished" -> "Tamamlandı"
            "tba" -> "Bilinmiyor"
            "unreleased" -> "Yayınlanmadı"
            "upcoming" -> "Yakında"
            else -> this
        }
    }
}
