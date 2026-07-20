package com.kitsugi.animelist.data.remote

import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.kitsugi.animelist.utils.*

class AniListSearchClient(
    private val accessToken: String? = null
) {

    suspend fun searchAniList(
        query: String,
        mediaType: MediaType,
        showAdultContent: Boolean = false,
        status: String? = null,
        format: String? = null,
        season: String? = null,
        genres: List<String>? = null,
        excludedGenres: List<String>? = null,
        tags: List<String>? = null,
        minYear: Int? = null,
        maxYear: Int? = null,
        minScore: Int? = null,
        maxScore: Int? = null,
        sort: List<String> = listOf("POPULARITY_DESC")
    ): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            if (query.isBlank() && status == null && format == null && season == null &&
                genres.isNullOrEmpty() && excludedGenres.isNullOrEmpty() && tags.isNullOrEmpty() &&
                minYear == null && maxYear == null && minScore == null && maxScore == null
            ) {
                return@withContext emptyList()
            }
            requestAniList(
                mediaType = mediaType,
                search = query.trim().takeIf { it.isNotBlank() },
                status = status,
                sort = sort,
                perPage = 24,
                format = format,
                season = season,
                genres = genres,
                excludedGenres = excludedGenres,
                tags = tags,
                minYear = minYear,
                maxYear = maxYear,
                minScore = minScore,
                maxScore = maxScore,
                showAdultContent = showAdultContent
            )
        }
    }

    suspend fun aniListTopAnime(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            requestAniList(
                mediaType = MediaType.Anime,
                search = null,
                status = null,
                sort = listOf("POPULARITY_DESC"),
                perPage = 20,
                page = page,
                showAdultContent = showAdultContent
            )
        }
    }

    suspend fun aniListAiringAnime(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            requestAniList(
                mediaType = MediaType.Anime,
                search = null,
                status = "RELEASING",
                sort = listOf("POPULARITY_DESC"),
                perPage = 20,
                page = page,
                showAdultContent = showAdultContent
            )
        }
    }

    suspend fun aniListUpcomingAnime(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            requestAniList(
                mediaType = MediaType.Anime,
                search = null,
                status = "NOT_YET_RELEASED",
                sort = listOf("POPULARITY_DESC"),
                perPage = 20,
                page = page,
                showAdultContent = showAdultContent
            )
        }
    }

    suspend fun aniListTopManga(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            requestAniList(
                mediaType = MediaType.Manga,
                search = null,
                status = null,
                sort = listOf("POPULARITY_DESC"),
                perPage = 20,
                page = page,
                showAdultContent = showAdultContent
            )
        }
    }

    suspend fun aniListPublishingManga(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            requestAniList(
                mediaType = MediaType.Manga,
                search = null,
                status = "RELEASING",
                sort = listOf("POPULARITY_DESC"),
                perPage = 20,
                page = page,
                showAdultContent = showAdultContent
            )
        }
    }

    suspend fun aniListTrendingAnime(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            requestAniList(
                mediaType = MediaType.Anime,
                search = null,
                status = null,
                sort = listOf("TRENDING_DESC"),
                perPage = 20,
                page = page,
                showAdultContent = showAdultContent
            )
        }
    }

    suspend fun aniListMovieAnime(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            requestAniList(
                mediaType = MediaType.Anime,
                search = null,
                status = null,
                sort = listOf("POPULARITY_DESC"),
                perPage = 20,
                format = "MOVIE",
                page = page,
                showAdultContent = showAdultContent
            )
        }
    }

    suspend fun aniListSeasonalAnime(page: Int = 1, showAdultContent: Boolean = false): List<JikanSearchResult> {
        return withContext(Dispatchers.IO) {
            val (season, year) = getCurrentSeasonAndYear()
            requestAniList(
                mediaType = MediaType.Anime,
                search = null,
                status = null,
                sort = listOf("POPULARITY_DESC"),
                perPage = 20,
                season = season,
                seasonYear = year,
                page = page,
                showAdultContent = showAdultContent
            )
        }
    }

    internal suspend fun requestAniList(
        mediaType: MediaType,
        search: String?,
        status: String?,
        sort: List<String>,
        perPage: Int,
        format: String? = null,
        season: String? = null,
        seasonYear: Int? = null,
        genres: List<String>? = null,
        excludedGenres: List<String>? = null,
        tags: List<String>? = null,
        minYear: Int? = null,
        maxYear: Int? = null,
        minScore: Int? = null,
        maxScore: Int? = null,
        page: Int = 1,
        showAdultContent: Boolean = false
    ): List<JikanSearchResult> {
        val query = """
            query (
                ${'$'}page: Int,
                ${'$'}perPage: Int,
                ${'$'}search: String,
                ${'$'}type: MediaType,
                ${'$'}status: MediaStatus,
                ${'$'}format: MediaFormat,
                ${'$'}season: MediaSeason,
                ${'$'}seasonYear: Int,
                ${'$'}genres: [String],
                ${'$'}excludedGenres: [String],
                ${'$'}tags: [String],
                ${'$'}startDateGreater: FuzzyDateInt,
                ${'$'}startDateLess: FuzzyDateInt,
                ${'$'}averageScoreGreater: Int,
                ${'$'}averageScoreLess: Int,
                ${'$'}sort: [MediaSort],
                ${'$'}isAdult: Boolean
            ) {
                Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                    media(
                        search: ${'$'}search,
                        type: ${'$'}type,
                        status: ${'$'}status,
                        format: ${'$'}format,
                        season: ${'$'}season,
                        seasonYear: ${'$'}seasonYear,
                        genre_in: ${'$'}genres,
                        genre_not_in: ${'$'}excludedGenres,
                        tag_in: ${'$'}tags,
                        startDate_greater: ${'$'}startDateGreater,
                        startDate_lesser: ${'$'}startDateLess,
                        averageScore_greater: ${'$'}averageScoreGreater,
                        averageScore_lesser: ${'$'}averageScoreLess,
                        sort: ${'$'}sort,
                        isAdult: ${'$'}isAdult
                    ) {
                        id
                        idMal
                        title {
                            romaji
                            english
                            native
                        }
                        format
                        episodes
                        chapters
                        averageScore
                        isAdult
                        genres
                        startDate {
                            year
                        }
                        coverImage {
                            extraLarge
                            large
                        }
                        bannerImage
                    }
                }
            }
        """.trimIndent()

        val variables = JSONObject()
            .put("page", page)
            .put("perPage", perPage)
            .put(
                "type",
                when (mediaType) {
                    MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "ANIME"
                    MediaType.Manga -> "MANGA"
                }
            )

        if (!sort.isNullOrEmpty()) {
            variables.put("sort", JSONArray(sort))
        }

        if (!showAdultContent) {
            variables.put("isAdult", false)
        }

        if (!search.isNullOrBlank()) {
            variables.put("search", search)
        }

        if (!status.isNullOrBlank()) {
            variables.put("status", status)
        }

        if (!format.isNullOrBlank()) {
            variables.put("format", format)
        }

        if (!season.isNullOrBlank()) {
            variables.put("season", season)
        }

        if (seasonYear != null && seasonYear > 0) {
            variables.put("seasonYear", seasonYear)
        }

        if (!genres.isNullOrEmpty()) {
            variables.put("genres", JSONArray(genres))
        }

        if (!excludedGenres.isNullOrEmpty()) {
            variables.put("excludedGenres", JSONArray(excludedGenres))
        }

        if (!tags.isNullOrEmpty()) {
            variables.put("tags", JSONArray(tags))
        }

        if (minYear != null && minYear > 0) {
            variables.put("startDateGreater", minYear * 10000)
        }

        if (maxYear != null && maxYear > 0) {
            variables.put("startDateLess", (maxYear + 1) * 10000 - 1)
        }

        if (minScore != null && minScore > 0) {
            variables.put("averageScoreGreater", minScore)
        }

        if (maxScore != null && maxScore > 0) {
            variables.put("averageScoreLess", maxScore)
        }

        // Tüm AniList trafiği tek merkezden (rate-limit + 429 retry uygulayan)
        // KitsugiApiBase.executeAniListQuery üzerinden geçer. Böylece 429 yeme
        // riski minimuma iner ve geçici hatalar otomatik tekrar denenir.
        val responseText = KitsugiApiBase.executeAniListQuery(
            query = query,
            variables = variables,
            accessToken = accessToken
        ) ?: return emptyList()

        return parseAniListResponse(
            jsonText = responseText,
            mediaType = mediaType
        )
    }

    private fun parseAniListResponse(
        jsonText: String,
        mediaType: MediaType
    ): List<JikanSearchResult> {
        val root = JSONObject(jsonText)

        val errors = root.optJSONArray("errors")
        if (errors != null && errors.length() > 0) {
            throw IllegalStateException(errors.toString())
        }

        val mediaArray = root
            .optJSONObject("data")
            ?.optJSONObject("Page")
            ?.optJSONArray("media")
            ?: return emptyList()

        val results = mutableListOf<JikanSearchResult>()

        for (index in 0 until mediaArray.length()) {
            val item = mediaArray.optJSONObject(index) ?: continue

            val aniListId = item.optInt("id", 0)
            val idMal = item.optionalPositiveInt("idMal")

            val fallbackId = if (aniListId > 0) {
                100_000_000 + aniListId
            } else {
                0
            }

            val stableId = idMal ?: fallbackId
            if (stableId <= 0) continue

            val titleObject = item.optJSONObject("title")
            val titleEnglish = titleObject?.optNullableString("english")
            val titleJapanese = titleObject?.optNullableString("native")
            val title = titleObject?.optNullableString("romaji")
                ?: titleEnglish
                ?: titleJapanese
                ?: "Başlıksız"

            val formatRaw = item.optNullableString("format")
            val format = if (formatRaw != null) {
                formatRaw
                    .replace("_", " ")
                    .lowercase()
                    .replaceFirstChar { char -> char.uppercase() }
            } else ""

            val year = item
                .optJSONObject("startDate")
                ?.optionalPositiveInt("year")

            val total = when (mediaType) {
                MediaType.Anime, MediaType.Movie, MediaType.TvShow -> item.optionalPositiveInt("episodes")
                MediaType.Manga -> item.optionalPositiveInt("chapters")
            }

            val averageScore = item.optionalPositiveInt("averageScore")
            val score = averageScore
                ?.let { (it / 10.0).toInt().coerceIn(0, 10) }

            val genres = item.namesFromStringArray("genres")

            val subtitleParts = buildList {
                if (format.isNotBlank()) add(format.toTurkishMediaTypeString())
                if (year != null && year > 0) add(year.toString())
                addAll(genres.take(3).toTurkishGenres())
            }

            val subtitle = if (subtitleParts.isEmpty()) {
                when (mediaType) {
                    MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "AniList ile eklenen anime"
                    MediaType.Manga -> "AniList ile eklenen manga"
                }
            } else {
                subtitleParts.joinToString(", ")
            }

            val imageUrl = item
                .optJSONObject("coverImage")
                ?.let { cover ->
                    cover.optNullableString("extraLarge")
                        ?: cover.optNullableString("large")
                }

            val bannerImage = item.optNullableString("bannerImage")
            val isAdult = item.optBoolean("isAdult", false)

            if (title.isNotBlank()) {
                results.add(
                    JikanSearchResult(
                        malId = stableId,
                        title = title,
                        subtitle = subtitle,
                        type = mediaType,
                        total = total,
                        score = score,
                        isAdult = isAdult,
                        imageUrl = imageUrl,
                        year = year,
                        source = "anilist",
                        realMalId = idMal,
                        titleEnglish = titleEnglish,
                        titleJapanese = titleJapanese,
                        backdropUrl = bannerImage
                    )
                )
            }
        }

        return results
    }

    private fun getCurrentSeasonAndYear(): Pair<String, Int> {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) // 0-indexed
        val season = when (month) {
            java.util.Calendar.DECEMBER, java.util.Calendar.JANUARY, java.util.Calendar.FEBRUARY -> "WINTER"
            java.util.Calendar.MARCH, java.util.Calendar.APRIL, java.util.Calendar.MAY -> "SPRING"
            java.util.Calendar.JUNE, java.util.Calendar.JULY, java.util.Calendar.AUGUST -> "SUMMER"
            else -> "FALL"
        }
        return Pair(season, year)
    }
}
