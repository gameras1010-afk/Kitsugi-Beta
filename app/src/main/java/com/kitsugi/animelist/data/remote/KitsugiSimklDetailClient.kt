package com.kitsugi.animelist.data.remote

import com.kitsugi.animelist.model.MediaType
import org.json.JSONObject
import okhttp3.Request
import com.kitsugi.animelist.utils.*

/**
 * Simkl API'sinden medya detaylarını çeker.
 */
internal object KitsugiSimklDetailClient {

    suspend fun fetchSimklDetailDirect(
        simklId: Int,
        mediaType: MediaType
    ): KitsugiMediaDetail? {
        // T3-01: BuildConfig'den alınır
        val clientId = com.kitsugi.animelist.BuildConfig.SIMKL_CLIENT_ID
        val typePath = when (mediaType) {
            MediaType.Movie -> "movies"
            MediaType.TvShow -> "tv"
            MediaType.Anime -> "anime"
            else -> "anime"
        }
        val urlString = "https://api.simkl.com/$typePath/$simklId?client_id=$clientId&extended=full"
        val request = Request.Builder()
            .url(urlString)
            .header("Accept", "application/json")
            .build()

        return try {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val text = response.body?.string() ?: return null
                val obj = JSONObject(text)

                val title = obj.optString("title", "")
                val overview = obj.optString("overview", "")
                val poster = obj.optString("poster", "")
                val year = obj.optInt("year", 0)
                val runtime = obj.optInt("runtime", 0)
                val genresArray = obj.optJSONArray("genres")
                val genresList = mutableListOf<String>()
                if (genresArray != null) {
                    for (i in 0 until genresArray.length()) {
                        genresList.add(genresArray.getString(i))
                    }
                }

                val ids = obj.optJSONObject("ids")
                val tmdbId = ids?.optInt("tmdb", 0) ?: 0
                val realMalId = ids?.optInt("mal", 0) ?: 0

                val ratingObj = obj.optJSONObject("ratings")?.optJSONObject("simkl")
                val ratingScore = ratingObj?.optDouble("rating", 0.0) ?: 0.0

                // Simkl pictures: büyük poster (_w.jpg) + orta poster (_m.jpg)
                val simklPictures = if (poster.isNotEmpty()) {
                    listOfNotNull(
                        "https://simkl.in/posters/${poster}_w.jpg",
                        "https://simkl.in/posters/${poster}_m.jpg"
                    )
                } else emptyList()

                KitsugiMediaDetail(
                    synopsis = overview,
                    genres = genresList.toTurkishGenres(),
                    status = obj.optString("status", "").toTurkishStatus(),
                    season = null,
                    sourceMaterial = null,
                    studios = emptyList(),
                    producers = emptyList(),
                    rating = if (ratingScore > 0.0) ratingScore.toString() else null,
                    broadcast = null,
                    episodeDuration = if (runtime > 0) "$runtime min".toTurkishDuration() else null,
                    startDate = null,
                    endDate = null,
                    titleEnglish = title,
                    titleJapanese = null,
                    titleRomaji = title,
                    titleNative = null,
                    synonyms = emptyList(),
                    openings = emptyList(),
                    endings = emptyList(),
                    trailerUrl = null,
                    title = title,
                    imageUrl = if (poster.isNotEmpty()) "https://simkl.in/posters/${poster}_m.jpg" else null,
                    score = if (ratingScore > 0.0) (ratingScore * 10).toInt() else null,
                    year = if (year > 0) year else null,
                    total = null,
                    isAdult = false,
                    realMalId = if (realMalId > 0) realMalId else null,
                    tags = emptyList(),
                    externalLinks = emptyList(),
                    streamingLinks = emptyList(),
                    streamingEpisodes = emptyList(),
                    tmdbId = if (tmdbId > 0) tmdbId else null,
                    tmdbSeason = 1,
                    pictures = simklPictures
                )
            }
        } catch (e: Exception) {
            // T3-05: printStackTrace → Log.e
            android.util.Log.e("KitsugiSimklDetailClient", "fetchSimklDetailDirect simklId=$simklId failed: ${e.message}", e)
            null
        }
    }
}
