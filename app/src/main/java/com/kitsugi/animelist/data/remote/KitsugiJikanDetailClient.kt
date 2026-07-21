package com.kitsugi.animelist.data.remote

import com.kitsugi.animelist.model.MediaType
import org.json.JSONObject
import java.net.URL
import okhttp3.Request
import com.kitsugi.animelist.utils.*

/**
 * MyAnimeList / Jikan API'sinden anime/manga detay bilgisi çeker.
 * Tüm Jikan spesifik parsing mantığını barındırır.
 */
internal object KitsugiJikanDetailClient {

    suspend fun fetchDetail(malId: Int, mediaType: MediaType): KitsugiMediaDetail? {
        val endpoint = when (mediaType) {
            MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "anime"
            MediaType.Manga -> "manga"
        }
        val url = URL("https://api.jikan.moe/v4/$endpoint/$malId/full")

        val detail = runCatching {
            KitsugiApiBase.runWithRateLimit {
                parseFullDetail(url, mediaType)
            }
        }.getOrNull()

        if (detail != null) {
            if (mediaType != MediaType.Manga) {
                val themes = KitsugiAnimeThemesClient.fetchAnimeThemes(malId, "MyAnimeList")
                val pictures = fetchPictures(malId, endpoint)
                val withThemes = if (themes.first.isNotEmpty() || themes.second.isNotEmpty()) {
                    detail.copy(openings = themes.first, endings = themes.second)
                } else detail
                val withPictures = if (pictures.isNotEmpty()) withThemes.copy(pictures = pictures) else withThemes

                // ARM üzerinden TMDB ID resolve et ve detail içine göm.
                if (withPictures.tmdbId == null) {
                    val resolvedTmdb = runCatching {
                        KitsugiIdResolver.resolveIds(malId = malId, aniListId = null).tmdbId
                    }.getOrNull()
                    return if (resolvedTmdb != null && resolvedTmdb > 0) {
                        withPictures.copy(tmdbId = resolvedTmdb)
                    } else {
                        withPictures
                    }
                }
                return withPictures
            } else {
                val pictures = fetchPictures(malId, endpoint)
                return if (pictures.isNotEmpty()) detail.copy(pictures = pictures) else detail
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

    private fun parseFullDetail(url: URL, mediaType: MediaType): KitsugiMediaDetail? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "KitsugiAnimeList/1.0")
            .build()
        return runCatching {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val text = response.body?.string() ?: return@runCatching null
                val data = JSONObject(text).optJSONObject("data") ?: return@runCatching null

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
            }
        }.getOrNull()
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
}
