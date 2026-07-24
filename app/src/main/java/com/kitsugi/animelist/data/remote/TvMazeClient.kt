package com.kitsugi.animelist.data.remote

import android.util.Log
import com.kitsugi.animelist.core.network.KitsugiHttpClient
import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object TvMazeClient {
    private const val TAG = "TvMazeClient"

    suspend fun fetchShowDetailByTitle(title: String): KitsugiMediaDetail? = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(title, "UTF-8")
        val url = "https://api.tvmaze.com/singlesearch/shows?q=$encoded"
        executeGet(url)
    }

    suspend fun fetchShowDetailByImdb(imdbId: String): KitsugiMediaDetail? = withContext(Dispatchers.IO) {
        val url = "https://api.tvmaze.com/lookup/shows?imdb=$imdbId"
        executeGet(url)
    }

    private suspend fun executeGet(urlStr: String): KitsugiMediaDetail? {
        try {
            val request = Request.Builder()
                .url(urlStr)
                .build()
            KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP ${response.code} for $urlStr")
                    return null
                }
                val body = response.body?.string() ?: return null
                return parseShow(JSONObject(body))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from TVmaze: ${e.message}", e)
            return null
        }
    }

    private fun parseShow(json: JSONObject): KitsugiMediaDetail {
        val name = json.optString("name", "")
        val summary = json.optString("summary", "").replace(Regex("<[^>]*>"), "").trim()
        val genres = mutableListOf<String>()
        val genresArr = json.optJSONArray("genres")
        if (genresArr != null) {
            for (i in 0 until genresArr.length()) {
                genres.add(genresArr.getString(i))
            }
        }
        val statusVal = json.optString("status", "").toTurkishStatus()
        val ratingObj = json.optJSONObject("rating")
        val ratingAvg = ratingObj?.optDouble("average", 0.0) ?: 0.0
        val scoreVal = if (ratingAvg > 0.0) ratingAvg.toInt() else null
        
        val imageObj = json.optJSONObject("image")
        val imageUrl = imageObj?.optString("medium") ?: imageObj?.optString("original")

        val premiered = json.optString("premiered", "")
        val yearVal = premiered.take(4).toIntOrNull()

        val runtime = json.optInt("runtime", 0)
        val duration = if (runtime > 0) "$runtime dk" else null

        val externals = json.optJSONObject("externals")
        val imdbId = externals?.optString("imdb", null)
        val tvdbId = externals?.optInt("thetvdb", 0).takeIf { it != null && it > 0 }

        val links = mutableListOf<KitsugiExternalLink>()
        val officialSite = json.optString("officialSite", "")
        if (officialSite.isNotEmpty()) {
            links.add(KitsugiExternalLink("Official Site", officialSite, "EN"))
        }
        if (!imdbId.isNullOrBlank()) {
            links.add(KitsugiExternalLink("IMDb", "https://www.imdb.com/title/$imdbId", "EN"))
        }

        return KitsugiMediaDetail(
            synopsis = summary,
            genres = genres,
            status = statusVal,
            titleEnglish = name,
            titleRomaji = name,
            title = name,
            imageUrl = imageUrl,
            score = scoreVal,
            year = yearVal,
            episodeDuration = duration,
            startDate = premiered,
            externalLinks = links,
            tmdbId = tvdbId // TVDB ID as a fallback cross-reference
        )
    }

    private fun String.toTurkishStatus(): String {
        return when (this.lowercase()) {
            "running" -> "Devam Ediyor"
            "ended" -> "Tamamlandı"
            "to be determined" -> "Bilinmiyor"
            "in development" -> "Yapım Aşamasında"
            else -> this
        }
    }
}
