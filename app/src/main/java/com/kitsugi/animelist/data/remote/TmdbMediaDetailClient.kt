package com.kitsugi.animelist.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.utils.*

/**
 * TMDB API'sinden medya detaylarını, görsellerini, izleme sağlayıcılarını
 * ve videolarını (fragman/tema) çeken istemci.
 *
 * [TmdbApiClient] tarafından delegate olarak kullanılır; doğrudan çağrılmamalıdır.
 */
internal object TmdbMediaDetailClient {

    private const val TAG = "TmdbMediaDetailClient"
    private const val IMG_W500 = "https://image.tmdb.org/t/p/w500"
    private const val IMG_W780 = "https://image.tmdb.org/t/p/w780"
    private const val IMG_W1280 = "https://image.tmdb.org/t/p/w1280"

    suspend fun fetchMediaDetail(
        tmdbId: Int,
        isMovie: Boolean,
        apiKey: String,
        executeGet: suspend (String) -> String?
    ): KitsugiMediaDetail? {
        val typePath = if (isMovie) "movie" else "tv"
        val lang = "tr-TR"

        val trUrl = "https://api.themoviedb.org/3/$typePath/$tmdbId?api_key=$apiKey&language=$lang"
        val enUrl = "https://api.themoviedb.org/3/$typePath/$tmdbId?api_key=$apiKey&language=en-US"

        return try {
            val trResponse = executeGet(trUrl)
            val trJson = trResponse?.let { JSONObject(it) }

            val trOverview = trJson?.optString("overview", "")
            val finalOverview = if (trOverview.isNullOrBlank()) {
                val enResponse = executeGet(enUrl)
                val enJson = enResponse?.let { JSONObject(it) }
                enJson?.optString("overview", "").orEmpty()
            } else {
                trOverview
            }

            val finalJson = trJson ?: JSONObject(executeGet(enUrl) ?: return null)

            val title = if (isMovie) finalJson.optString("title", "") else finalJson.optString("name", "")
            val originalTitle = if (isMovie) finalJson.optString("original_title", "") else finalJson.optString("original_name", "")
            val posterPath = finalJson.optNullableString("poster_path") ?: ""
            val genresArray = finalJson.optJSONArray("genres")
            val genresList = mutableListOf<String>()
            if (genresArray != null) {
                for (i in 0 until genresArray.length()) {
                    genresList.add(genresArray.getJSONObject(i).optString("name", ""))
                }
            }

            val rating = finalJson.optDouble("vote_average", 0.0)
            val voteCount = finalJson.optInt("vote_count", 0).takeIf { it > 0 }
            val tmdbPopularity = finalJson.optDouble("popularity", 0.0).toInt().takeIf { it > 0 }
            val nextEpisodeObj = finalJson.optJSONObject("next_episode_to_air")
            val nextAiring = if (nextEpisodeObj != null) {
                val ep = nextEpisodeObj.optInt("episode_number")
                val dateStr = nextEpisodeObj.optString("air_date")
                if (ep > 0 && dateStr.isNotBlank()) "Bölüm $ep, $dateStr tarihinde yayında" else null
            } else null

            val releaseDate = if (isMovie) finalJson.optString("release_date", "") else finalJson.optString("first_air_date", "")
            val status = finalJson.optString("status", "").toTurkishStatus()
            val year = releaseDate.split("-").firstOrNull()?.toIntOrNull()

            val totalEpisodes = if (isMovie) 1 else finalJson.optInt("number_of_episodes", 0)
            val totalSeasonsVal = if (isMovie) null else finalJson.optInt("number_of_seasons", 1)

            val durationVal = if (isMovie) {
                val runtime = finalJson.optInt("runtime", 0)
                if (runtime > 0) "$runtime dk" else null
            } else {
                val runTimes = finalJson.optJSONArray("episode_run_time")
                if (runTimes != null && runTimes.length() > 0) {
                    val runTime = runTimes.optInt(0, 0)
                    if (runTime > 0) "$runTime dk" else null
                } else null
            }

            val endDateVal = if (isMovie) {
                releaseDate
            } else {
                finalJson.optString("last_air_date", "").takeIf { it.isNotEmpty() }
            }

            val productionCompanies = finalJson.optJSONArray("production_companies")
            val studiosList = mutableListOf<KitsugiStudio>()
            val producersList = mutableListOf<KitsugiStudio>()
            if (productionCompanies != null) {
                for (i in 0 until productionCompanies.length()) {
                    val company = productionCompanies.getJSONObject(i)
                    val id = company.optInt("id", 0)
                    val name = company.optString("name", "")
                    if (name.isNotEmpty()) {
                        if (i == 0) {
                            studiosList.add(KitsugiStudio(id = id, name = name, isMain = true))
                        } else {
                            producersList.add(KitsugiStudio(id = id, name = name, isMain = false))
                        }
                    }
                }
            }

            val watchProviders = fetchWatchProviders(tmdbId, isMovie, apiKey, executeGet)
            val mediaImages = fetchMediaImages(tmdbId, isMovie, apiKey, executeGet)
            val (trailer, videoThemes) = fetchVideos(tmdbId, isMovie, apiKey, executeGet)

            val score100 = (rating * 10).toInt().coerceIn(0, 100)

            KitsugiMediaDetail(
                synopsis = finalOverview,
                genres = genresList,
                status = status,
                season = null,
                sourceMaterial = null,
                studios = studiosList,
                producers = producersList,
                rating = rating.toString(),
                broadcast = null,
                episodeDuration = durationVal,
                startDate = releaseDate,
                endDate = endDateVal,
                titleEnglish = title,
                titleJapanese = null,
                titleRomaji = title,
                titleNative = originalTitle,
                synonyms = emptyList(),
                openings = videoThemes,
                endings = emptyList(),
                trailerUrl = trailer,
                title = title,
                imageUrl = if (posterPath.isNotEmpty()) "$IMG_W500$posterPath" else null,
                score = (rating).toInt().coerceIn(0, 10),
                year = year,
                total = totalEpisodes,
                isAdult = finalJson.optBoolean("adult", false),
                realMalId = null,
                tags = emptyList(),
                externalLinks = watchProviders,
                streamingLinks = watchProviders,
                streamingEpisodes = emptyList(),
                tmdbId = tmdbId,
                tmdbSeason = 1,
                pictures = mediaImages,
                totalSeasons = totalSeasonsVal,
                nextAiringEpisode = nextAiring,
                meanScore = score100,
                averageScore = score100,
                popularity = tmdbPopularity,
                scoredBy = voteCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TMDB details: ${e.message}", e)
            null
        }
    }

    suspend fun fetchMediaImages(
        tmdbId: Int,
        isMovie: Boolean,
        apiKey: String,
        executeGet: suspend (String) -> String?
    ): List<String> = withContext(Dispatchers.IO) {
        val typePath = if (isMovie) "movie" else "tv"
        val url = "https://api.themoviedb.org/3/$typePath/$tmdbId/images?api_key=$apiKey"
        try {
            val responseText = executeGet(url) ?: return@withContext emptyList()
            val root = JSONObject(responseText)
            val list = mutableListOf<String>()

            val backdrops = root.optJSONArray("backdrops")
            if (backdrops != null) {
                for (i in 0 until minOf(backdrops.length(), 15)) {
                    val path = backdrops.getJSONObject(i).optNullableString("file_path") ?: ""
                    if (path.isNotEmpty()) list.add("$IMG_W1280$path")
                }
            }

            val posters = root.optJSONArray("posters")
            if (posters != null) {
                for (i in 0 until minOf(posters.length(), 15)) {
                    val path = posters.getJSONObject(i).optNullableString("file_path") ?: ""
                    if (path.isNotEmpty()) list.add("$IMG_W780$path")
                }
            }
            list.distinct()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TMDB images: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchWatchProviders(
        tmdbId: Int,
        isMovie: Boolean,
        apiKey: String,
        executeGet: suspend (String) -> String?
    ): List<KitsugiExternalLink> = withContext(Dispatchers.IO) {
        val typePath = if (isMovie) "movie" else "tv"
        val url = "https://api.themoviedb.org/3/$typePath/$tmdbId/watch/providers?api_key=$apiKey"
        try {
            val responseText = executeGet(url) ?: return@withContext emptyList()
            val root = JSONObject(responseText)
            val results = root.optJSONObject("results") ?: return@withContext emptyList()
            val providerObj = results.optJSONObject("TR")
                ?: results.optJSONObject("US")
                ?: results.keys().asSequence().firstOrNull()?.let { results.optJSONObject(it) }

            if (providerObj != null) {
                val tmdbLink = providerObj.optNullableString("link") ?: ""
                val list = mutableListOf<KitsugiExternalLink>()
                val flatrate = providerObj.optJSONArray("flatrate")
                if (flatrate != null) {
                    for (i in 0 until flatrate.length()) {
                        val item = flatrate.getJSONObject(i)
                        val name = item.optString("provider_name", "Streaming")
                        list.add(KitsugiExternalLink(site = name, url = tmdbLink, language = "TR"))
                    }
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching watch providers: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchVideos(
        tmdbId: Int,
        isMovie: Boolean,
        apiKey: String,
        executeGet: suspend (String) -> String?
    ): Pair<String?, List<KitsugiTheme>> = withContext(Dispatchers.IO) {
        val typePath = if (isMovie) "movie" else "tv"
        val trUrl = "https://api.themoviedb.org/3/$typePath/$tmdbId/videos?api_key=$apiKey&language=tr-TR"
        val enUrl = "https://api.themoviedb.org/3/$typePath/$tmdbId/videos?api_key=$apiKey&language=en-US"

        try {
            var responseText = executeGet(trUrl)
            var root = responseText?.let { JSONObject(it) }
            var results = root?.optJSONArray("results")

            fun parseList(arr: org.json.JSONArray?): List<Pair<String, KitsugiTheme>> {
                if (arr == null) return emptyList()
                val list = mutableListOf<Pair<String, KitsugiTheme>>()
                for (i in 0 until arr.length()) {
                    val video = arr.getJSONObject(i)
                    val site = video.optString("site", "")
                    val type = video.optString("type", "")
                    val key = video.optString("key", "")
                    val name = video.optString("name", "")
                    if (site.equals("YouTube", ignoreCase = true) && key.isNotEmpty()) {
                        val typeTr = when (type.lowercase()) {
                            "trailer" -> "Fragman"
                            "teaser" -> "Teaser"
                            "clip" -> "Klip"
                            "featurette" -> "Özel Bakış"
                            "behind the scenes" -> "Kamera Arkası"
                            "bloopers" -> "Çekim Hataları"
                            else -> type
                        }
                        list.add(Pair(type, KitsugiTheme(label = "$typeTr - $name", videoUrl = "https://www.youtube.com/watch?v=$key")))
                    }
                }
                return list
            }

            var allParsed = parseList(results)
            if (allParsed.isEmpty()) {
                responseText = executeGet(enUrl)
                root = responseText?.let { JSONObject(it) }
                results = root?.optJSONArray("results")
                allParsed = parseList(results)
            }

            val primaryTrailer = allParsed.firstOrNull { it.first.equals("Trailer", ignoreCase = true) }?.second?.videoUrl
                ?: allParsed.firstOrNull { it.first.equals("Teaser", ignoreCase = true) }?.second?.videoUrl
                ?: allParsed.firstOrNull { it.first.equals("Clip", ignoreCase = true) }?.second?.videoUrl
                ?: allParsed.firstOrNull()?.second?.videoUrl

            val otherThemes = allParsed.map { it.second }
            Pair(primaryTrailer, otherThemes)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TMDB videos: ${e.message}", e)
            Pair(null, emptyList())
        }
    }
}
