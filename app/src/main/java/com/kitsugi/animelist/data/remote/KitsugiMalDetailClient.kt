package com.kitsugi.animelist.data.remote

import android.util.Log
import com.kitsugi.animelist.model.MediaType
import org.json.JSONObject
import java.net.URL
import okhttp3.Request
import com.kitsugi.animelist.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MyAnimeList / Jikan API'sinden anime/manga detay bilgisi çeker.
 * Tüm Jikan spesifik parsing mantığını barındırır.
 */
internal object KitsugiMalDetailClient {

    private const val TAG = "KitsugiMalDetail"
    private const val MAX_RETRIES = 3

    suspend fun fetchDetail(malId: Int, mediaType: MediaType): KitsugiMediaDetail? {
        if (malId <= 0) {
            Log.w(TAG, "fetchDetail: malId=$malId is invalid (<=0), skipping Jikan/MAL call")
            return null
        }
        val endpoint = when (mediaType) {
            MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "anime"
            MediaType.Manga -> "manga"
        }

        var isFromMalv2 = false
        // 1. Resmi MAL v2 API'sini birincil olarak dene
        var detail = runCatching {
            fetchOfficialMalDetail(malId, mediaType, endpoint)
        }.getOrNull()

        if (detail != null) {
            isFromMalv2 = true
            Log.d(TAG, "fetchDetail: Successfully loaded primary details from MAL v2 API for malId=$malId")
        } else {
            Log.w(TAG, "fetchDetail: MAL v2 API failed for malId=$malId — falling back to Jikan API")
            // 2. MAL v2 başarısız olursa komple Jikan'a düş (eski yedek davranış)
            val url = URL("https://api.jikan.moe/v4/$endpoint/$malId/full")
            detail = runCatching {
                KitsugiApiBase.runWithRateLimit {
                    parseFullDetail(url, mediaType)
                }
            }.getOrNull()
        }

        if (detail != null) {
            return coroutineScope {
                if (mediaType != MediaType.Manga) {
                    val themesDeferred = async(Dispatchers.IO) {
                        runCatching { KitsugiAnimeThemesClient.fetchAnimeThemes(malId, "MyAnimeList") }
                            .getOrElse { Pair(emptyList(), emptyList()) }
                    }
                    val picturesDeferred = async(Dispatchers.IO) {
                        fetchPictures(malId, endpoint)
                    }
                    val resolvedTmdbDeferred = if (detail.tmdbId == null) {
                        async(Dispatchers.IO) {
                            runCatching { KitsugiIdResolver.resolveIds(malId = malId, aniListId = null).tmdbId }.getOrNull()
                        }
                    } else null

                    // Jikan'dan ek bilgileri (trailer, streaming/external links) paralel çek
                    val jikanSupplementDeferred = if (isFromMalv2) {
                        async(Dispatchers.IO) {
                            fetchJikanSupplement(malId, endpoint)
                        }
                    } else null

                    val themes = themesDeferred.await()
                    val pictures = picturesDeferred.await()
                    val resolvedTmdb = resolvedTmdbDeferred?.await()
                    val jikanSupplement = jikanSupplementDeferred?.await()

                    var mergedDetail = detail

                    if (themes.first.isNotEmpty() || themes.second.isNotEmpty()) {
                        mergedDetail = mergedDetail.copy(openings = themes.first, endings = themes.second)
                    }
                    if (pictures.isNotEmpty()) {
                        mergedDetail = mergedDetail.copy(pictures = pictures)
                    }
                    if (mergedDetail.tmdbId == null && resolvedTmdb != null && resolvedTmdb > 0) {
                        mergedDetail = mergedDetail.copy(tmdbId = resolvedTmdb)
                    }
                    if (jikanSupplement != null) {
                        mergedDetail = mergedDetail.copy(
                            trailerUrl = jikanSupplement.trailerUrl ?: mergedDetail.trailerUrl,
                            streamingLinks = jikanSupplement.streamingLinks,
                            externalLinks = jikanSupplement.externalLinks
                        )
                    }

                    mergedDetail
                } else {
                    val picturesDeferred = async(Dispatchers.IO) {
                        fetchPictures(malId, endpoint)
                    }
                    val jikanSupplementDeferred = if (isFromMalv2) {
                        async(Dispatchers.IO) {
                            fetchJikanSupplement(malId, endpoint)
                        }
                    } else null

                    val pictures = picturesDeferred.await()
                    val jikanSupplement = jikanSupplementDeferred?.await()

                    var mergedDetail = detail
                    if (pictures.isNotEmpty()) {
                        mergedDetail = mergedDetail.copy(pictures = pictures)
                    }
                    if (jikanSupplement != null) {
                        mergedDetail = mergedDetail.copy(
                            externalLinks = jikanSupplement.externalLinks
                        )
                    }
                    mergedDetail
                }
            }
        }
        return detail
    }

    suspend fun fetchSynopsis(malId: Int, mediaType: MediaType): String? {
        val endpoint = when (mediaType) {
            MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "anime"
            MediaType.Manga -> "manga"
        }
        val url = URL("https://api.jikan.moe/v4/$endpoint/$malId/full")
        return runCatching {
            KitsugiApiBase.runWithRateLimit {
                requestSynopsis(url)
            }
        }.getOrNull()
    }

    // ─── Private ─────────────────────────────────────────────────────────────

    private fun fetchPictures(malId: Int, endpoint: String): List<String> {
        val url = URL("https://api.jikan.moe/v4/$endpoint/$malId/pictures")
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "KitsugiAnimeList/1.0")
            .build()
        return runCatching {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching emptyList()
                val text = response.body?.string() ?: return@runCatching emptyList()
                val dataArr = org.json.JSONObject(text).optJSONArray("data") ?: return@runCatching emptyList()
                val urls = mutableListOf<String>()
                for (i in 0 until dataArr.length()) {
                    val obj = dataArr.getJSONObject(i)
                    val webp = obj.optJSONObject("webp")
                    val jpg = obj.optJSONObject("jpg")
                    val picUrl = webp?.optString("large_image_url")?.takeIf { it.isNotBlank() }
                        ?: webp?.optString("image_url")?.takeIf { it.isNotBlank() }
                        ?: jpg?.optString("large_image_url")?.takeIf { it.isNotBlank() }
                        ?: jpg?.optString("image_url")?.takeIf { it.isNotBlank() }
                    if (!picUrl.isNullOrBlank()) urls.add(picUrl)
                }
                urls
            }
        }.getOrElse { emptyList() }
    }

    private suspend fun parseFullDetail(url: URL, mediaType: MediaType): KitsugiMediaDetail? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "KitsugiAnimeList/1.0")
            .build()

        var lastException: Exception? = null
        for (attempt in 0 until MAX_RETRIES) {
            try {
                val result = com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                    val code = response.code
                    Log.d(TAG, "parseFullDetail attempt=${attempt+1} → HTTP $code for $url")

                    // Handle rate-limit / server errors with retry
                    if (code == 429 || code in 500..599) {
                        val retryAfterSec = response.header("Retry-After")?.toLongOrNull()
                        val backoffMs = if (retryAfterSec != null) {
                            retryAfterSec * 1_000L
                        } else {
                            1_000L * (1 shl attempt) // 1s, 2s, 4s
                        }
                        Log.w(TAG, "parseFullDetail: HTTP $code from Jikan, backing off ${backoffMs}ms (attempt ${attempt+1}/$MAX_RETRIES)")
                        delay(backoffMs)
                        return@use null // trigger retry
                    }

                    if (!response.isSuccessful) {
                        Log.e(TAG, "parseFullDetail: HTTP $code for $url — non-retryable failure")
                        return null // permanent failure, don't retry
                    }

                    val text = response.body?.string()
                    if (text.isNullOrBlank()) {
                        Log.e(TAG, "parseFullDetail: empty/null response body for $url")
                        return null
                    }
                    Log.d(TAG, "parseFullDetail: response body size=${text.length} chars")

                    val root = runCatching { JSONObject(text) }.getOrElse { e ->
                        Log.e(TAG, "parseFullDetail: JSON root parse error for $url: ${e.message}")
                        return null
                    }
                    val data = root.optJSONObject("data")
                    if (data == null) {
                        Log.e(TAG, "parseFullDetail: 'data' object missing in JSON. Root keys: ${root.keys().asSequence().take(10).toList()}")
                        return null
                    }

                val synopsis = data.optNullableString("synopsis")?.cleanApiText()

                val genres = mutableListOf<String>()
                data.optJSONArray("genres")?.let { arr ->
                    for (i in 0 until arr.length()) arr.getJSONObject(i).optNullableString("name")?.let { genres.add(it) }
                }
                data.optJSONArray("explicit_genres")?.let { arr ->
                    for (i in 0 until arr.length()) arr.getJSONObject(i).optNullableString("name")?.let { genres.add(it) }
                }
                data.optJSONArray("themes")?.let { arr ->
                    for (i in 0 until arr.length()) arr.getJSONObject(i).optNullableString("name")?.let { genres.add(it) }
                }
                data.optJSONArray("demographics")?.let { arr ->
                    for (i in 0 until arr.length()) arr.getJSONObject(i).optNullableString("name")?.let { genres.add(it) }
                }

                val status = data.optNullableString("status")?.toTurkishStatus()
                val sourceMaterial = data.optNullableString("source")?.toTurkishSourceMaterial()
                val rating = data.optNullableString("rating")?.toTurkishRating()

                val dateContainerKey = when (mediaType) {
                    MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "aired"
                    MediaType.Manga -> "published"
                }

                val season = buildString {
                    val s = data.optNullableString("season")
                    val y = data.optJSONObject(dateContainerKey)?.optJSONObject("prop")?.optJSONObject("from")?.optInt("year")
                    if (s != null) append(s.replaceFirstChar { it.uppercase() })
                    if (y != null && y > 0) { if (isNotEmpty()) append(" "); append(y) }
                }.takeIf { it.isNotBlank() }?.toTurkishSeason()

                val broadcast = if (mediaType == MediaType.Anime) {
                    data.optJSONObject("broadcast")?.let {
                        val day = it.optNullableString("day")
                        val time = it.optNullableString("time")
                        if (day != null && time != null) "$day $time" else day ?: time
                    }?.toTurkishBroadcast()
                } else null

                val episodeDuration = if (mediaType == MediaType.Anime) {
                    data.optNullableString("duration")?.toTurkishDuration()
                } else null

                val studios = mutableListOf<KitsugiStudio>()
                if (mediaType == MediaType.Anime) {
                    data.optJSONArray("studios")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val id = obj.optInt("mal_id")
                            val name = obj.optNullableString("name")
                            if (!name.isNullOrBlank()) {
                                studios.add(KitsugiStudio(id = id, name = name, isMain = true))
                            }
                        }
                    }
                }

                val airedFrom = data.optJSONObject(dateContainerKey)?.optNullableString("from")?.take(10)
                val airedTo = data.optJSONObject(dateContainerKey)?.optNullableString("to")?.take(10)

                val titles = data.optJSONArray("titles")
                var titleEnglish: String? = null
                var titleJapanese: String? = null
                val synonyms = mutableListOf<String>()
                if (titles != null) {
                    for (i in 0 until titles.length()) {
                        val t = titles.getJSONObject(i)
                        when (t.optString("type")) {
                            "English" -> titleEnglish = t.optNullableString("title")
                            "Japanese" -> titleJapanese = t.optNullableString("title")
                            "Synonym" -> t.optNullableString("title")?.let { synonyms.add(it) }
                        }
                    }
                }

                val openings = mutableListOf<KitsugiTheme>()
                val endings = mutableListOf<KitsugiTheme>()
                if (mediaType == MediaType.Anime) {
                    data.optJSONObject("theme")?.let { theme ->
                        theme.optJSONArray("openings")?.let { arr ->
                            for (i in 0 until arr.length()) openings.add(KitsugiTheme(label = arr.getString(i), videoUrl = null))
                        }
                        theme.optJSONArray("endings")?.let { arr ->
                            for (i in 0 until arr.length()) endings.add(KitsugiTheme(label = arr.getString(i), videoUrl = null))
                        }
                    }
                }

                val trailerObj = data.optJSONObject("trailer")
                val trailerUrl = run {
                    val ytId = trailerObj?.optNullableString("youtube_id")
                    if (!ytId.isNullOrBlank()) return@run "https://www.youtube.com/watch?v=$ytId"
                    val directUrl = trailerObj?.optNullableString("url")
                    if (!directUrl.isNullOrBlank()) return@run directUrl
                    val embedUrl = trailerObj?.optNullableString("embed_url")
                    if (!embedUrl.isNullOrBlank()) {
                        val idRegex = Regex("(?:youtube\\.com|youtube-nocookie\\.com)/embed/([A-Za-z0-9_-]{11})")
                        val match = idRegex.find(embedUrl)
                        if (match != null) return@run "https://www.youtube.com/watch?v=${match.groupValues[1]}"
                    }
                    null
                }

                val jikanTitle = data.optNullableString("title")
                    ?: titleEnglish
                    ?: data.optNullableString("title_english")
                    ?: "Başlıksız"

                val images = data.optJSONObject("images")
                val webp = images?.optJSONObject("webp")
                val jpg = images?.optJSONObject("jpg")
                val imageUrl = webp?.optNullableString("large_image_url")
                    ?: webp?.optNullableString("image_url")
                    ?: jpg?.optNullableString("large_image_url")
                    ?: jpg?.optNullableString("image_url")

                val scoreDouble = data.optDouble("score", Double.NaN)
                val score = if (scoreDouble.isNaN()) null else scoreDouble.toInt().coerceIn(0, 10)
                val meanScoreInt = if (scoreDouble.isNaN()) null else (scoreDouble * 10).toInt()
                val scoredBy = data.optionalPositiveInt("scored_by")
                val members = data.optionalPositiveInt("members")
                val favorites = data.optionalPositiveInt("favorites")
                val rank = data.optionalPositiveInt("rank")
                val popularityRank = data.optionalPositiveInt("popularity")

                val directYear = data.optionalPositiveInt("year")
                val year = if (directYear != null) {
                    directYear
                } else {
                    val fromDate = data.optJSONObject(dateContainerKey)?.optString("from").orEmpty()
                    fromDate.takeIf { it.length >= 4 }?.take(4)?.toIntOrNull()
                }

                val total = when (mediaType) {
                    MediaType.Anime, MediaType.Movie, MediaType.TvShow -> data.optionalPositiveInt("episodes")
                    MediaType.Manga -> data.optionalPositiveInt("chapters")
                }

                val ratingString = data.optString("rating").lowercase()
                val isAdult = ratingString.contains("hentai") ||
                        ratingString.contains("rx") ||
                        genres.any { tag -> tag.lowercase().contains("hentai") }

                val producers = mutableListOf<KitsugiStudio>()
                if (mediaType == MediaType.Manga) {
                    data.optJSONArray("authors")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val id = obj.optInt("mal_id")
                            val name = obj.optNullableString("name")
                            if (!name.isNullOrBlank()) studios.add(KitsugiStudio(id = id, name = name, isMain = true))
                        }
                    }
                    data.optJSONArray("serializations")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val id = obj.optInt("mal_id")
                            val name = obj.optNullableString("name")
                            if (!name.isNullOrBlank()) producers.add(KitsugiStudio(id = id, name = name, isMain = false))
                        }
                    }
                } else {
                    data.optJSONArray("producers")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val id = obj.optInt("mal_id")
                            val name = obj.optNullableString("name")
                            if (!name.isNullOrBlank()) producers.add(KitsugiStudio(id = id, name = name, isMain = false))
                        }
                    }
                    data.optJSONArray("licensors")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val id = obj.optInt("mal_id")
                            val name = obj.optNullableString("name")
                            if (!name.isNullOrBlank()) producers.add(KitsugiStudio(id = id, name = name, isMain = false))
                        }
                    }
                }

                val streamingLinks = mutableListOf<KitsugiExternalLink>()
                data.optJSONArray("streaming")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val name = obj.optNullableString("name")
                        val url = obj.optNullableString("url")
                        if (!name.isNullOrBlank() && !url.isNullOrBlank()) {
                            streamingLinks.add(KitsugiExternalLink(site = name, url = url))
                        }
                    }
                }

                val externalLinks = mutableListOf<KitsugiExternalLink>()
                data.optJSONArray("external")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val name = obj.optNullableString("name")
                        val url = obj.optNullableString("url")
                        if (!name.isNullOrBlank() && !url.isNullOrBlank()) {
                            externalLinks.add(KitsugiExternalLink(site = name, url = url))
                        }
                    }
                }

                KitsugiMediaDetail(
                    synopsis = synopsis,
                    genres = genres.filter { it.isNotBlank() }.toTurkishGenres(),
                    status = status,
                    season = season,
                    sourceMaterial = sourceMaterial,
                    studios = studios,
                    producers = producers,
                    rating = rating,
                    broadcast = broadcast,
                    episodeDuration = episodeDuration,
                    startDate = airedFrom,
                    endDate = airedTo,
                    titleEnglish = titleEnglish,
                    titleJapanese = titleJapanese,
                    synonyms = synonyms,
                    openings = openings,
                    endings = endings,
                    trailerUrl = trailerUrl,
                    title = jikanTitle,
                    imageUrl = imageUrl,
                    score = score,
                    year = year,
                    total = total,
                    isAdult = isAdult,
                    streamingLinks = streamingLinks,
                    externalLinks = externalLinks,
                    meanScore = meanScoreInt,
                    averageScore = meanScoreInt,
                    popularity = members,
                    favorites = favorites,
                    rank = rank,
                    popularityRank = popularityRank,
                    scoredBy = scoredBy,
                    members = members
                )
                } // end use{}
                if (result != null) return result
                // result==null means a retryable code (429/5xx), loop continues
            } catch (e: Exception) {
                Log.e(TAG, "parseFullDetail: Exception on attempt ${attempt+1} for $url: ${e.message}", e)
                lastException = e
            }
        }
        Log.e(TAG, "parseFullDetail: All $MAX_RETRIES attempts failed for $url. Last error: ${lastException?.message}")
        return null
    }

    /**
     * Resmi MyAnimeList v2 API'sini kullanarak detay çeker.
     * Jikan başarısız olduğunda devreye girer (MoeList yaklaşımı).
     * Endpoint: https://api.myanimelist.net/v2/{anime|manga}/{id}?fields=...
     */
    private fun fetchOfficialMalDetail(malId: Int, mediaType: MediaType, endpoint: String): KitsugiMediaDetail? {
        val clientId = com.kitsugi.animelist.BuildConfig.MAL_CLIENT_ID
        val fields = buildString {
            append("id,title,main_picture,alternative_titles,start_date,end_date,synopsis")
            append(",mean,rank,popularity,num_list_users,num_scoring_users,nsfw,status")
            append(",media_type,genres,num_favorites")
            if (mediaType == MediaType.Manga) {
                append(",num_chapters,num_volumes,authors{first_name,last_name},serialization{name}")
            } else {
                append(",num_episodes,start_season,broadcast,source,average_episode_duration")
                append(",studios{id,name},producers{id,name}")
                append(",opening_themes,ending_themes,rating")
            }
        }
        val urlStr = "https://api.myanimelist.net/v2/$endpoint/$malId?fields=$fields"
        Log.d(TAG, "fetchOfficialMalDetail: GET $urlStr")

        val request = Request.Builder()
            .url(urlStr)
            .header("X-MAL-CLIENT-ID", clientId)
            .header("Accept", "application/json")
            .header("User-Agent", "KitsugiAnimeList/1.0")
            .build()

        return try {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                val code = response.code
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchOfficialMalDetail: HTTP $code for malId=$malId")
                    return null
                }
                val text = response.body?.string()
                if (text.isNullOrBlank()) {
                    Log.w(TAG, "fetchOfficialMalDetail: empty body for malId=$malId")
                    return null
                }
                Log.d(TAG, "fetchOfficialMalDetail: OK, body size=${text.length} chars")
                parseOfficialMalDetailJson(text, mediaType)
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchOfficialMalDetail: Exception for malId=$malId: ${e.message}")
            null
        }
    }

    /**
     * Resmi MAL v2 API JSON yanıtını KitsugiMediaDetail'e dönüştürür.
     * JSON yapısı doğrudan api.myanimelist.net/v2 root düzeyindedir (Jikan'ın "data" sarmalayıcısından farklı).
     */
    private fun parseOfficialMalDetailJson(text: String, mediaType: MediaType): KitsugiMediaDetail? {
        return try {
            val d = JSONObject(text)

            val title = d.optNullableString("title") ?: "Başlıksız"
            val altTitles = d.optJSONObject("alternative_titles")
            val titleEnglish = altTitles?.optNullableString("en")
            val titleJapanese = altTitles?.optNullableString("ja")
            val synonyms = mutableListOf<String>()
            altTitles?.optJSONArray("synonyms")?.let { arr ->
                for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let { synonyms.add(it) }
            }

            val mainPic = d.optJSONObject("main_picture")
            val imageUrl = mainPic?.optNullableString("large") ?: mainPic?.optNullableString("medium")

            val synopsis = d.optNullableString("synopsis")?.cleanApiText()

            val startDate = d.optNullableString("start_date")?.take(10)
            val endDate = d.optNullableString("end_date")?.take(10)
            val year = startDate?.take(4)?.toIntOrNull()

            val meanDouble = d.optDouble("mean", Double.NaN)
            val score = if (meanDouble.isNaN()) null else meanDouble.toInt().coerceIn(0, 10)
            val meanScoreInt = if (meanDouble.isNaN()) null else (meanDouble * 10).toInt()

            val rank = d.optionalPositiveInt("rank")
            val popularityRank = d.optionalPositiveInt("popularity")
            val members = d.optionalPositiveInt("num_list_users")
            val scoredBy = d.optionalPositiveInt("num_scoring_users")
            val favorites = d.optionalPositiveInt("num_favorites")

            val nsfw = d.optString("nsfw", "white")
            val isAdult = nsfw != "white"

            val statusRaw = d.optString("status", "")
            // MAL v2 status değerleri: "finished_airing", "currently_airing", "not_yet_aired"
            // Bunları Jikan/TMDB formatına çevirip Türkçeleştiriyoruz
            val statusStr = when (statusRaw) {
                "finished_airing"  -> "Finished Airing"
                "currently_airing" -> "Currently Airing"
                "not_yet_aired"    -> "Not yet aired"
                "finished"         -> "Finished"
                "publishing"       -> "Publishing"
                "discontinued"     -> "Discontinued"
                "on_hiatus"        -> "On Hiatus"
                else               -> statusRaw
            }.toTurkishStatus()

            val mediaTypeRaw = d.optString("media_type", "")

            // MAL v2 rating değerleri: "g", "pg", "pg_13", "r", "r+", "rx"
            // Jikan formatına çevirip Türkçeleştiriyoruz
            val ratingRaw = d.optString("rating", "")
            val rating = when (ratingRaw.lowercase()) {
                "g"    -> "G - All Ages"
                "pg"   -> "PG - Children"
                "pg_13" -> "PG-13 - Teens 13 or older"
                "r"    -> "R - 17+ (violence & profanity)"
                "r+"   -> "R+ - Mild Nudity"
                "rx"   -> "Rx - Hentai"
                else   -> null
            }?.toTurkishRating()

            val genres = mutableListOf<String>()
            d.optJSONArray("genres")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.optNullableString("name")?.let { genres.add(it) }
                }
            }

            val total: Int? = if (mediaType == MediaType.Manga) {
                d.optionalPositiveInt("num_chapters")
            } else {
                d.optionalPositiveInt("num_episodes")
            }

            // Studios & producers (anime only)
            val studios = mutableListOf<KitsugiStudio>()
            val producers = mutableListOf<KitsugiStudio>()
            if (mediaType != MediaType.Manga) {
                d.optJSONArray("studios")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optInt("id")
                        val name = obj.optNullableString("name")
                        if (!name.isNullOrBlank()) studios.add(KitsugiStudio(id = id, name = name, isMain = true))
                    }
                }
                d.optJSONArray("producers")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optInt("id")
                        val name = obj.optNullableString("name")
                        if (!name.isNullOrBlank()) producers.add(KitsugiStudio(id = id, name = name, isMain = false))
                    }
                }
            } else {
                // Manga: authors → studios, serializations → producers
                d.optJSONArray("authors")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)?.optJSONObject("node") ?: continue
                        val id = obj.optInt("id")
                        val first = obj.optString("first_name", "")
                        val last = obj.optString("last_name", "")
                        val name = "$first $last".trim()
                        if (name.isNotBlank()) studios.add(KitsugiStudio(id = id, name = name, isMain = true))
                    }
                }
                d.optJSONArray("serialization")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)?.optJSONObject("node") ?: continue
                        val id = obj.optInt("id")
                        val name = obj.optNullableString("name")
                        if (!name.isNullOrBlank()) producers.add(KitsugiStudio(id = id, name = name, isMain = false))
                    }
                }
            }

            // Season
            val seasonObj = d.optJSONObject("start_season")
            val season = if (seasonObj != null) {
                val s = seasonObj.optNullableString("season")?.replaceFirstChar { it.uppercase() }
                val y = seasonObj.optInt("year").takeIf { it > 0 }
                buildString {
                    if (s != null) append(s)
                    if (y != null) { if (isNotEmpty()) append(" "); append(y) }
                }.takeIf { it.isNotBlank() }?.toTurkishSeason()
            } else null

            // Broadcast
            val broadcast = if (mediaType != MediaType.Manga) {
                d.optJSONObject("broadcast")?.let {
                    val day = it.optNullableString("day_of_the_week")
                        ?.replaceFirstChar { c -> c.uppercase() }?.let { d2 -> "${d2}s" } // "monday" → "Mondays"
                    val time = it.optNullableString("start_time")
                    if (day != null && time != null) "$day $time" else day ?: time
                }?.toTurkishBroadcast()
            } else null

            // Episode duration (in seconds → convert to "X min" string)
            val episodeDuration = if (mediaType != MediaType.Manga) {
                val secs = d.optInt("average_episode_duration", 0)
                if (secs > 0) "${secs / 60} min".toTurkishDuration() else null
            } else null

            // Source material
            val sourceMaterial = d.optNullableString("source")?.let {
                // MAL v2: "manga", "original", "light_novel", etc.
                it.split("_").joinToString(" ") { w -> w.replaceFirstChar { c -> c.uppercase() } }
            }?.toTurkishSourceMaterial()

            // Opening / Ending themes (MAL v2: opening_themes / ending_themes are arrays of strings)
            val openings = mutableListOf<KitsugiTheme>()
            val endings = mutableListOf<KitsugiTheme>()
            d.optJSONArray("opening_themes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    val text2 = obj?.optNullableString("text") ?: continue
                    openings.add(KitsugiTheme(label = text2, videoUrl = null))
                }
            }
            d.optJSONArray("ending_themes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    val text2 = obj?.optNullableString("text") ?: continue
                    endings.add(KitsugiTheme(label = text2, videoUrl = null))
                }
            }

            KitsugiMediaDetail(
                synopsis = synopsis,
                genres = genres.filter { it.isNotBlank() }.toTurkishGenres(),
                status = statusStr,
                season = season,
                sourceMaterial = sourceMaterial,
                studios = studios,
                producers = producers,
                rating = rating,
                broadcast = broadcast,
                episodeDuration = episodeDuration,
                startDate = startDate,
                endDate = endDate,
                titleEnglish = titleEnglish,
                titleJapanese = titleJapanese,
                synonyms = synonyms,
                openings = openings,
                endings = endings,
                trailerUrl = null, // MAL v2 standart alanlarında trailer URL yok
                title = title,
                imageUrl = imageUrl,
                score = score,
                year = year,
                total = total,
                isAdult = isAdult,
                meanScore = meanScoreInt,
                averageScore = meanScoreInt,
                popularity = members,
                favorites = favorites,
                rank = rank,
                popularityRank = popularityRank,
                scoredBy = scoredBy,
                members = members
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseOfficialMalDetailJson: Parse error: ${e.message}")
            null
        }
    }

    private fun requestSynopsis(url: URL): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "KitsugiAnimeList/1.0")
            .build()
        return try {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val responseText = response.body?.string() ?: return null
                val root = JSONObject(responseText)
                val data = root.optJSONObject("data") ?: return null
                data.optString("synopsis")
                    .cleanApiText()
                    .takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchJikanSupplement(malId: Int, endpoint: String): JikanSupplement? {
        val url = URL("https://api.jikan.moe/v4/$endpoint/$malId/full")
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "KitsugiAnimeList/1.0")
            .build()

        return try {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                val code = response.code
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchJikanSupplement: HTTP $code for malId=$malId")
                    return null
                }
                val text = response.body?.string() ?: return null
                val root = JSONObject(text)
                val data = root.optJSONObject("data") ?: return null

                val trailerObj = data.optJSONObject("trailer")
                val trailerUrl = run {
                    val ytId = trailerObj?.optNullableString("youtube_id")
                    if (!ytId.isNullOrBlank()) return@run "https://www.youtube.com/watch?v=$ytId"
                    val directUrl = trailerObj?.optNullableString("url")
                    if (!directUrl.isNullOrBlank()) return@run directUrl
                    val embedUrl = trailerObj?.optNullableString("embed_url")
                    if (!embedUrl.isNullOrBlank()) {
                        val idRegex = Regex("(?:youtube\\.com|youtube-nocookie\\.com)/embed/([A-Za-z0-9_-]{11})")
                        val match = idRegex.find(embedUrl)
                        if (match != null) return@run "https://www.youtube.com/watch?v=${match.groupValues[1]}"
                    }
                    null
                }

                val streamingLinks = mutableListOf<KitsugiExternalLink>()
                data.optJSONArray("streaming")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val name = obj.optNullableString("name")
                        val urlLink = obj.optNullableString("url")
                        if (!name.isNullOrBlank() && !urlLink.isNullOrBlank()) {
                            streamingLinks.add(KitsugiExternalLink(site = name, url = urlLink))
                        }
                    }
                }

                val externalLinks = mutableListOf<KitsugiExternalLink>()
                data.optJSONArray("external")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val name = obj.optNullableString("name")
                        val urlLink = obj.optNullableString("url")
                        if (!name.isNullOrBlank() && !urlLink.isNullOrBlank()) {
                            externalLinks.add(KitsugiExternalLink(site = name, url = urlLink))
                        }
                    }
                }

                JikanSupplement(
                    trailerUrl = trailerUrl,
                    streamingLinks = streamingLinks,
                    externalLinks = externalLinks
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchJikanSupplement: Failed for malId=$malId: ${e.message}")
            null
        }
    }
}

private data class JikanSupplement(
    val trailerUrl: String?,
    val streamingLinks: List<KitsugiExternalLink>,
    val externalLinks: List<KitsugiExternalLink>
)
