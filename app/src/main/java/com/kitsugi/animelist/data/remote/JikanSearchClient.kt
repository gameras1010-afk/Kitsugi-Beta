package com.kitsugi.animelist.data.remote

import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import okhttp3.Request
import com.kitsugi.animelist.utils.*

class JikanSearchClient {
    private val aniListSearchClient = AniListSearchClient()

    suspend fun search(
        query: String,
        mediaType: MediaType,
        showAdultContent: Boolean = false,
        status: String? = null,
        format: String? = null,
        genreId: Int? = null,
        sort: String? = null,
        orderBy: String? = null
    ): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            if (query.isBlank() && status == null && format == null && genreId == null) {
                return@withContext emptyList()
            }

            val queryParams = mutableListOf<String>()
            if (query.isNotBlank()) {
                queryParams.add("q=${URLEncoder.encode(query.trim(), "UTF-8")}")
            }
            queryParams.add("limit=24")
            queryParams.add("sfw=${!showAdultContent}")

            if (!status.isNullOrBlank()) {
                queryParams.add("status=$status")
            }
            if (!format.isNullOrBlank()) {
                queryParams.add("type=$format")
            }
            if (genreId != null && genreId > 0) {
                queryParams.add("genres=$genreId")
            }
            if (!orderBy.isNullOrBlank()) {
                queryParams.add("order_by=$orderBy")
                if (!sort.isNullOrBlank()) {
                    queryParams.add("sort=$sort")
                }
            }

            val endpoint = when (mediaType) {
                MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "anime"
                MediaType.Manga -> "manga"
            }

            val url = URL("https://api.jikan.moe/v4/$endpoint?${queryParams.joinToString("&")}")

            requestAndParseWithFallback(
                url = url,
                mediaType = mediaType,
                fallback = {
                    val officialResults = searchOfficialMal(query, mediaType, showAdultContent)
                    if (officialResults.isNotEmpty()) {
                        officialResults
                    } else {
                        aniListSearchClient.requestAniList(
                            mediaType = mediaType,
                            search = query.trim().takeIf { it.isNotBlank() },
                            status = if (status == "airing" || status == "publishing") "RELEASING"
                                     else if (status == "complete") "FINISHED"
                                     else if (status == "upcoming") "NOT_YET_RELEASED"
                                     else null,
                            sort = if (orderBy == "score") listOf("SCORE_DESC") else listOf("POPULARITY_DESC"),
                            perPage = 24,
                            format = format?.uppercase(),
                            showAdultContent = showAdultContent
                        )
                    }
                }
            )
        }
    }

    suspend fun searchMALOnly(
        query: String,
        mediaType: MediaType,
        showAdultContent: Boolean = false,
        status: String? = null,
        format: String? = null,
        genreId: Int? = null,
        sort: String? = null,
        orderBy: String? = null
    ): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            if (query.isBlank() && status == null && format == null && genreId == null) {
                return@withContext emptyList()
            }

            val queryParams = mutableListOf<String>()
            if (query.isNotBlank()) {
                queryParams.add("q=${URLEncoder.encode(query.trim(), "UTF-8")}")
            }
            queryParams.add("limit=24")
            queryParams.add("sfw=${!showAdultContent}")

            if (!status.isNullOrBlank()) {
                queryParams.add("status=$status")
            }
            if (!format.isNullOrBlank()) {
                queryParams.add("type=$format")
            }
            if (genreId != null && genreId > 0) {
                queryParams.add("genres=$genreId")
            }
            if (!orderBy.isNullOrBlank()) {
                queryParams.add("order_by=$orderBy")
                if (!sort.isNullOrBlank()) {
                    queryParams.add("sort=$sort")
                }
            }

            val endpoint = when (mediaType) {
                MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "anime"
                MediaType.Manga -> "manga"
            }

            val url = URL("https://api.jikan.moe/v4/$endpoint?${queryParams.joinToString("&")}")

            requestAndParseWithFallback(
                url = url,
                mediaType = mediaType,
                fallback = {
                    val officialResults = searchOfficialMal(query, mediaType, showAdultContent)
                    if (officialResults.isEmpty()) {
                        throw IllegalStateException("MyAnimeList (MAL) sunucuları şu anda yanıt vermiyor. Lütfen daha sonra tekrar dene.")
                    }
                    officialResults
                }
            )
        }
    }

    private fun getOfficialMalRankingOrSeason(
        urlStr: String,
        mediaType: MediaType
    ): List<JikanSearchResult> {
        // T3-01: BuildConfig'den alınır
        val clientId = com.kitsugi.animelist.BuildConfig.MAL_CLIENT_ID
        val request = Request.Builder()
            .url(urlStr)
            .header("X-MAL-CLIENT-ID", clientId)
            .header("User-Agent", "KitsugiAnimeList/1.0")
            .build()

        return try {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.w("JikanSearchClient", "Official MAL API request failed: ${response.code} for URL: $urlStr")
                    emptyList()
                } else {
                    val responseText = response.body?.string().orEmpty()
                    parseOfficialMalResponse(responseText, mediaType)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("JikanSearchClient", "Official MAL API request error: ${e.message} for URL: $urlStr")
            emptyList()
        }
    }

    suspend fun topAnime(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            val offset = (page - 1) * 20
            val fields = "id,title,main_picture,alternative_titles,start_date,mean,num_episodes,media_type,genres,nsfw"
            val url = "https://api.myanimelist.net/v2/anime/ranking?ranking_type=all&limit=20&offset=$offset&fields=$fields"

            val results = getOfficialMalRankingOrSeason(url, MediaType.Anime)
            if (results.isNotEmpty()) {
                results
            } else {
                runCatching { aniListSearchClient.aniListTopAnime(page, showAdultContent) }.getOrDefault(emptyList())
            }
        }
    }

    suspend fun airingAnime(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            val offset = (page - 1) * 20
            val fields = "id,title,main_picture,alternative_titles,start_date,mean,num_episodes,media_type,genres,nsfw"
            val url = "https://api.myanimelist.net/v2/anime/ranking?ranking_type=airing&limit=20&offset=$offset&fields=$fields"

            val results = getOfficialMalRankingOrSeason(url, MediaType.Anime)
            if (results.isNotEmpty()) {
                results
            } else {
                runCatching { aniListSearchClient.aniListAiringAnime(page, showAdultContent) }.getOrDefault(emptyList())
            }
        }
    }

    suspend fun upcomingAnime(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            val offset = (page - 1) * 20
            val fields = "id,title,main_picture,alternative_titles,start_date,mean,num_episodes,media_type,genres,nsfw"
            val url = "https://api.myanimelist.net/v2/anime/ranking?ranking_type=upcoming&limit=20&offset=$offset&fields=$fields"

            val results = getOfficialMalRankingOrSeason(url, MediaType.Anime)
            if (results.isNotEmpty()) {
                results
            } else {
                runCatching { aniListSearchClient.aniListUpcomingAnime(page, showAdultContent) }.getOrDefault(emptyList())
            }
        }
    }

    suspend fun topManga(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            val offset = (page - 1) * 20
            val fields = "id,title,main_picture,alternative_titles,start_date,mean,num_chapters,media_type,genres,nsfw"
            val url = "https://api.myanimelist.net/v2/manga/ranking?ranking_type=all&limit=20&offset=$offset&fields=$fields"

            val results = getOfficialMalRankingOrSeason(url, MediaType.Manga)
            if (results.isNotEmpty()) {
                results
            } else {
                runCatching { aniListSearchClient.aniListTopManga(page, showAdultContent) }.getOrDefault(emptyList())
            }
        }
    }

    suspend fun publishingManga(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            val offset = (page - 1) * 20
            val fields = "id,title,main_picture,alternative_titles,start_date,mean,num_chapters,media_type,genres,nsfw"
            val url = "https://api.myanimelist.net/v2/manga/ranking?ranking_type=manga&limit=20&offset=$offset&fields=$fields"

            val results = getOfficialMalRankingOrSeason(url, MediaType.Manga)
            if (results.isNotEmpty()) {
                results
            } else {
                runCatching { aniListSearchClient.aniListPublishingManga(page, showAdultContent) }.getOrDefault(emptyList())
            }
        }
    }

    suspend fun completedManga(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            val offset = (page - 1) * 20
            val fields = "id,title,main_picture,alternative_titles,start_date,mean,num_chapters,media_type,genres,nsfw"
            val url = "https://api.myanimelist.net/v2/manga/ranking?ranking_type=manga&limit=20&offset=$offset&fields=$fields"

            val results = getOfficialMalRankingOrSeason(url, MediaType.Manga)
            if (results.isNotEmpty()) {
                results
            } else {
                emptyList()
            }
        }
    }

    suspend fun trendingAnime(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            val offset = (page - 1) * 20
            val fields = "id,title,main_picture,alternative_titles,start_date,mean,num_episodes,media_type,genres,nsfw"
            val url = "https://api.myanimelist.net/v2/anime/ranking?ranking_type=bypopularity&limit=20&offset=$offset&fields=$fields"

            val results = getOfficialMalRankingOrSeason(url, MediaType.Anime)
            if (results.isNotEmpty()) {
                results
            } else {
                runCatching { aniListSearchClient.aniListTrendingAnime(page, showAdultContent) }.getOrDefault(emptyList())
            }
        }
    }

    suspend fun movieAnime(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            val offset = (page - 1) * 20
            val fields = "id,title,main_picture,alternative_titles,start_date,mean,num_episodes,media_type,genres,nsfw"
            val url = "https://api.myanimelist.net/v2/anime/ranking?ranking_type=movie&limit=20&offset=$offset&fields=$fields"

            val results = getOfficialMalRankingOrSeason(url, MediaType.Anime)
            if (results.isNotEmpty()) {
                results
            } else {
                runCatching { aniListSearchClient.aniListMovieAnime(page, showAdultContent) }.getOrDefault(emptyList())
            }
        }
    }

    suspend fun trendingManga(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            val offset = (page - 1) * 20
            val fields = "id,title,main_picture,alternative_titles,start_date,mean,num_chapters,media_type,genres,nsfw"
            val url = "https://api.myanimelist.net/v2/manga/ranking?ranking_type=bypopularity&limit=20&offset=$offset&fields=$fields"

            val results = getOfficialMalRankingOrSeason(url, MediaType.Manga)
            if (results.isNotEmpty()) {
                results
            } else {
                runCatching { aniListSearchClient.aniListTrendingManga(page, showAdultContent) }.getOrDefault(emptyList())
            }
        }
    }

    suspend fun newlyAddedAnime(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            val url = URL("https://api.jikan.moe/v4/anime?order_by=start_date&sort=desc&limit=20&sfw=${!showAdultContent}&page=$page")
            requestAndParseWithFallback(
                url = url,
                mediaType = MediaType.Anime,
                fallback = {
                    runCatching { aniListSearchClient.aniListNewlyAddedAnime(page, showAdultContent) }.getOrDefault(emptyList())
                }
            )
        }
    }

    suspend fun newlyAddedManga(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            val url = URL("https://api.jikan.moe/v4/manga?order_by=start_date&sort=desc&limit=20&sfw=${!showAdultContent}&page=$page")
            requestAndParseWithFallback(
                url = url,
                mediaType = MediaType.Manga,
                fallback = {
                    runCatching { aniListSearchClient.aniListNewlyAddedManga(page, showAdultContent) }.getOrDefault(emptyList())
                }
            )
        }
    }

    suspend fun seasonalAnime(
        page: Int = 1,
        showAdultContent: Boolean = false,
        year: Int? = null,
        season: String? = null,
        sort: String? = null
    ): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            val calendar = java.util.Calendar.getInstance()
            val targetYear = year ?: calendar.get(java.util.Calendar.YEAR)
            val targetSeason = season?.lowercase() ?: run {
                val month = calendar.get(java.util.Calendar.MONTH)
                when (month) {
                    java.util.Calendar.DECEMBER, java.util.Calendar.JANUARY, java.util.Calendar.FEBRUARY -> "winter"
                    java.util.Calendar.MARCH, java.util.Calendar.APRIL, java.util.Calendar.MAY -> "spring"
                    java.util.Calendar.JUNE, java.util.Calendar.JULY, java.util.Calendar.AUGUST -> "summer"
                    else -> "fall"
                }
            }

            val malSort = when (sort) {
                "SCORE_DESC", "score" -> "anime_score"
                "START_DATE_DESC", "start_date" -> "anime_start_date"
                "END_DATE_DESC" -> "anime_start_date"
                else -> "anime_num_list_users"
            }

            val offset = (page - 1) * 20
            val fields = "id,title,main_picture,alternative_titles,start_date,mean,num_episodes,media_type,genres,nsfw"
            val url = "https://api.myanimelist.net/v2/anime/season/$targetYear/$targetSeason?limit=20&offset=$offset&fields=$fields&sort=$malSort"

            val results = getOfficialMalRankingOrSeason(url, MediaType.Anime)
            if (results.isNotEmpty()) {
                results
            } else {
                val aniListSort = when (sort) {
                    "SCORE_DESC", "score" -> listOf("SCORE_DESC")
                    "START_DATE_DESC", "start_date" -> listOf("START_DATE_DESC")
                    "END_DATE_DESC" -> listOf("END_DATE_DESC")
                    else -> listOf("POPULARITY_DESC")
                }
                runCatching {
                    aniListSearchClient.aniListSeasonalAnime(
                        page = page,
                        showAdultContent = showAdultContent,
                        year = targetYear,
                        season = targetSeason,
                        sort = aniListSort
                    )
                }.getOrDefault(emptyList())
            }
        }
    }

    private suspend fun requestAndParseWithFallback(
        url: URL,
        mediaType: MediaType,
        fallback: suspend () -> List<JikanSearchResult>
    ): List<JikanSearchResult> {
        var lastError: Throwable? = null
        var attempt = 0

        while (true) {
            try {
                return KitsugiApiBase.runWithRateLimit {
                    requestAndParseJikan(
                        url = url,
                        mediaType = mediaType
                    )
                }
            } catch (error: Throwable) {
                lastError = error
                attempt += 1

                val category = KitsugiApiBase.classifyNetworkError(error)

                val shouldRetry = when (category) {
                    KitsugiApiBase.NetworkErrorCategory.RateLimited -> attempt <= 2
                    KitsugiApiBase.NetworkErrorCategory.GatewayProblem -> attempt <= 1
                    KitsugiApiBase.NetworkErrorCategory.Other -> attempt <= 1
                }

                if (!shouldRetry) {
                    break
                }

                val delayMs = when (category) {
                    KitsugiApiBase.NetworkErrorCategory.RateLimited -> 1_400L * attempt
                    KitsugiApiBase.NetworkErrorCategory.GatewayProblem -> 350L
                    KitsugiApiBase.NetworkErrorCategory.Other -> 450L
                }

                delay(delayMs)
            }
        }

        return runCatching {
            fallback()
        }.getOrElse { fallbackError ->
            val fallbackMsg = fallbackError.message.orEmpty()
            val jikanMsg = lastError?.message.orEmpty()
            val displayMsg = if (fallbackMsg.startsWith("Jikan API (MAL)") || fallbackMsg.contains("API hatası")) {
                jikanMsg.ifBlank { fallbackMsg }
            } else {
                "$jikanMsg | Alternatif Arama (AniList): $fallbackMsg"
            }
            throw IllegalStateException(displayMsg)
        }
    }

    private fun requestAndParseJikan(
        url: URL,
        mediaType: MediaType
    ): List<JikanSearchResult> {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "KitsugiAnimeList/1.0")
            .build()

        try {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val cleanMsg = when (code) {
                        429 -> "Çok fazla istek yapıldı (Rate Limit). Lütfen birkaç saniye sonra tekrar dene."
                        504, 503, 502 -> "MyAnimeList (MAL) sunucuları şu anda yanıt vermiyor veya bakımda. Lütfen daha sonra tekrar dene."
                        else -> "Sunucu hatası oluştu (Kod: $code). Lütfen daha sonra tekrar dene."
                    }
                    throw IllegalStateException(cleanMsg)
                }

                val responseText = response.body?.string().orEmpty()
                return parseJikanResponse(
                    jsonText = responseText,
                    mediaType = mediaType
                )
            }
        } catch (e: Exception) {
            if (e is IllegalStateException) throw e
            throw IllegalStateException("MyAnimeList (MAL) bağlantı hatası. İnternet bağlantını kontrol edip tekrar dene.", e)
        }
    }

    private fun parseJikanResponse(
        jsonText: String,
        mediaType: MediaType
    ): List<JikanSearchResult> {
        val root = JSONObject(jsonText)
        val dataArray = root.optJSONArray("data") ?: return emptyList()

        val results = mutableListOf<JikanSearchResult>()

        for (index in 0 until dataArray.length()) {
            val item = dataArray.optJSONObject(index) ?: continue

            val malId = item.optInt("mal_id", 0)
            val titleEnglish = item.optNullableString("title_english")
            val titleJapanese = item.optNullableString("title_japanese")
            val title = item.optNullableString("title")
                ?: titleEnglish
                ?: "Başlıksız"

            val itemType = item.optNullableString("type").orEmpty()
            val year = extractYearFromJikan(item, mediaType)

            val scoreDouble = item.optDouble("score", Double.NaN)
            val score = if (scoreDouble.isNaN()) {
                null
            } else {
                scoreDouble.toInt().coerceIn(0, 10)
            }
            val rawScore = if (scoreDouble.isNaN()) null else scoreDouble
            val rankVal = item.optionalPositiveInt("rank")
            val membersVal = item.optionalPositiveInt("members")
            val favoritesVal = item.optionalPositiveInt("favorites")

            val total = when (mediaType) {
                MediaType.Anime, MediaType.Movie, MediaType.TvShow -> item.optionalPositiveInt("episodes")
                MediaType.Manga -> item.optionalPositiveInt("chapters")
            }

            val genres = item.namesFromObjectArray("genres")
            val themes = item.namesFromObjectArray("themes")
            val demographics = item.namesFromObjectArray("demographics")

            val subtitleParts = buildList {
                if (itemType.isNotBlank()) add(itemType.toTurkishMediaTypeString())
                if (year != null && year > 0) add(year.toString())
                addAll(genres.take(3).toTurkishGenres())
            }

            val subtitle = if (subtitleParts.isEmpty()) {
                when (mediaType) {
                    MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "API ile eklenen anime"
                    MediaType.Manga -> "API ile eklenen manga"
                }
            } else {
                subtitleParts.joinToString(", ")
            }

            val isAdult = isAdultJikanItem(
                item = item,
                genres = genres,
                themes = themes,
                demographics = demographics
            )

            val imageUrl = extractJikanImageUrl(item)

            if (malId > 0 && title.isNotBlank()) {
                results.add(
                    JikanSearchResult(
                        malId = malId,
                        title = title,
                        subtitle = subtitle,
                        type = mediaType,
                        total = total,
                        score = score,
                        isAdult = isAdult,
                        imageUrl = imageUrl,
                        year = year,
                        source = "jikan",
                        titleEnglish = titleEnglish,
                        titleJapanese = titleJapanese,
                        rank = rankVal,
                        members = membersVal,
                        favorites = favoritesVal,
                        rawScoreDouble = rawScore
                    )
                )
            }
        }

        return results
    }

    private fun extractJikanImageUrl(item: JSONObject): String? {
        val images = item.optJSONObject("images") ?: return null
        val webp = images.optJSONObject("webp")
        val jpg = images.optJSONObject("jpg")

        return webp?.optNullableString("large_image_url")
            ?: webp?.optNullableString("image_url")
            ?: jpg?.optNullableString("large_image_url")
            ?: jpg?.optNullableString("image_url")
    }

    private fun extractYearFromJikan(
        item: JSONObject,
        mediaType: MediaType
    ): Int? {
        val directYear = item.optionalPositiveInt("year")
        if (directYear != null) return directYear

        val dateContainerKey = when (mediaType) {
            MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "aired"
            MediaType.Manga -> "published"
        }

        val fromDate = item
            .optJSONObject(dateContainerKey)
            ?.optString("from")
            .orEmpty()

        return fromDate
            .takeIf { it.length >= 4 }
            ?.take(4)
            ?.toIntOrNull()
    }

    private fun isAdultJikanItem(
        item: JSONObject,
        genres: List<String>,
        themes: List<String>,
        demographics: List<String>
    ): Boolean {
        val rating = item.optString("rating").lowercase()
        val allTags = genres + themes + demographics

        return rating.contains("hentai") ||
                rating.contains("rx") ||
                allTags.any { tag ->
                    tag.lowercase().contains("hentai")
                }
    }

    private fun searchOfficialMal(
        query: String,
        mediaType: MediaType,
        showAdultContent: Boolean
    ): List<JikanSearchResult> {
        // T3-01: BuildConfig'den alınır
        val clientId = com.kitsugi.animelist.BuildConfig.MAL_CLIENT_ID
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val endpoint = if (mediaType == MediaType.Manga) "manga" else "anime"
        val fields = "id,title,main_picture,alternative_titles,start_date,mean,${if (mediaType == MediaType.Manga) "num_chapters" else "num_episodes"},media_type,genres,nsfw"
        val url = "https://api.myanimelist.net/v2/$endpoint?q=$encodedQuery&limit=12&fields=$fields"

        val request = Request.Builder()
            .url(url)
            .header("X-MAL-CLIENT-ID", clientId)
            .header("User-Agent", "KitsugiAnimeList/1.0")
            .build()

        return try {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.w("JikanSearchClient", "Official MAL API search failed with HTTP code: ${response.code}")
                    return emptyList()
                }
                val responseText = response.body?.string().orEmpty()
                parseOfficialMalResponse(responseText, mediaType)
            }
        } catch (e: Exception) {
            android.util.Log.e("JikanSearchClient", "Official MAL API search error: ${e.message}")
            emptyList()
        }
    }

    private fun parseOfficialMalResponse(
        jsonText: String,
        mediaType: MediaType
    ): List<JikanSearchResult> {
        val root = JSONObject(jsonText)
        val dataArray = root.optJSONArray("data") ?: return emptyList()
        val results = mutableListOf<JikanSearchResult>()

        for (i in 0 until dataArray.length()) {
            val itemObj = dataArray.optJSONObject(i) ?: continue
            val node = itemObj.optJSONObject("node") ?: continue

            val malId = node.optInt("id", 0)
            val title = node.optString("title", "Başlıksız")

            val altTitles = node.optJSONObject("alternative_titles")
            val titleEnglish = altTitles?.optNullableString("en")
            val titleJapanese = altTitles?.optNullableString("ja")

            val mainPic = node.optJSONObject("main_picture")
            val imageUrl = mainPic?.optNullableString("large") ?: mainPic?.optNullableString("medium")

            val startDate = node.optString("start_date", "")
            val year = startDate.take(4).toIntOrNull()

            val scoreDouble = node.optDouble("mean", Double.NaN)
            val score = if (scoreDouble.isNaN()) null else scoreDouble.toInt().coerceIn(0, 10)

            val total = if (mediaType == MediaType.Manga) {
                node.optionalPositiveInt("num_chapters")
            } else {
                node.optionalPositiveInt("num_episodes")
            }

            val rawType = node.optString("media_type", "")
            val nsfw = node.optString("nsfw", "white")
            val isAdult = nsfw != "white"

            val genresList = mutableListOf<String>()
            val genresArray = node.optJSONArray("genres")
            if (genresArray != null) {
                for (j in 0 until genresArray.length()) {
                    val genreObj = genresArray.optJSONObject(j) ?: continue
                    val genreName = genreObj.optString("name", "")
                    if (genreName.isNotBlank()) genresList.add(genreName)
                }
            }

            val subtitleParts = buildList {
                if (rawType.isNotBlank()) add(rawType.uppercase().toTurkishMediaTypeString())
                if (year != null && year > 0) add(year.toString())
                addAll(genresList.take(3).toTurkishGenres())
            }

            val subtitle = if (subtitleParts.isEmpty()) {
                if (mediaType == MediaType.Manga) "Manga" else "Anime"
            } else {
                subtitleParts.joinToString(", ")
            }

            if (malId > 0 && title.isNotBlank()) {
                results.add(
                    JikanSearchResult(
                        malId = malId,
                        title = title,
                        subtitle = subtitle,
                        type = mediaType,
                        total = total,
                        score = score,
                        isAdult = isAdult,
                        imageUrl = imageUrl,
                        year = year,
                        source = "jikan",
                        titleEnglish = titleEnglish,
                        titleJapanese = titleJapanese
                    )
                )
            }
        }
        return results
    }
}
