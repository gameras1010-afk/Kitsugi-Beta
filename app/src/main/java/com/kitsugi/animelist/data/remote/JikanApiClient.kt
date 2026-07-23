package com.kitsugi.animelist.data.remote

import com.kitsugi.animelist.model.MediaType

class JikanApiClient(
    private val aniListToken: String? = null
) {
    private val jikanSearchClient = JikanSearchClient()
    private val aniListSearchClient = AniListSearchClient(accessToken = aniListToken)
    private val detailClient = KitsugiDetailClient()
    private val characterClient = KitsugiCharacterClient()
    private val staffClient = KitsugiStaffClient()
    private val mediaTabsClient = KitsugiMediaTabsClient()
    private val studioClient = KitsugiStudioClient()

    // Bölünmüş yeni istemciler
    private val mediaRelationsClient = KitsugiMediaRelationsClient()
    private val mediaSocialClient = KitsugiMediaSocialClient()
    private val mediaMutationsClient = KitsugiMediaMutationsClient()

    // Search Methods
    suspend fun search(
        query: String,
        mediaType: MediaType,
        showAdultContent: Boolean = false,
        status: String? = null,
        format: String? = null,
        genreId: Int? = null,
        sort: String? = null,
        orderBy: String? = null
    ) = jikanSearchClient.search(query, mediaType, showAdultContent, status, format, genreId, sort, orderBy)

    suspend fun searchMALOnly(
        query: String,
        mediaType: MediaType,
        showAdultContent: Boolean = false,
        status: String? = null,
        format: String? = null,
        genreId: Int? = null,
        sort: String? = null,
        orderBy: String? = null
    ) = jikanSearchClient.searchMALOnly(query, mediaType, showAdultContent, status, format, genreId, sort, orderBy)

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
    ) = aniListSearchClient.searchAniList(
        query = query,
        mediaType = mediaType,
        showAdultContent = showAdultContent,
        status = status,
        format = format,
        season = season,
        genres = genres,
        excludedGenres = excludedGenres,
        tags = tags,
        minYear = minYear,
        maxYear = maxYear,
        minScore = minScore,
        maxScore = maxScore,
        sort = sort
    )

    // Top & Filters
    suspend fun topAnime(page: Int = 1, showAdultContent: Boolean = false) = jikanSearchClient.topAnime(page, showAdultContent)
    suspend fun airingAnime(page: Int = 1, showAdultContent: Boolean = false) = jikanSearchClient.airingAnime(page, showAdultContent)
    suspend fun upcomingAnime(page: Int = 1, showAdultContent: Boolean = false) = jikanSearchClient.upcomingAnime(page, showAdultContent)
    suspend fun topManga(page: Int = 1, showAdultContent: Boolean = false) = jikanSearchClient.topManga(page, showAdultContent)
    suspend fun publishingManga(page: Int = 1, showAdultContent: Boolean = false) = jikanSearchClient.publishingManga(page, showAdultContent)
    suspend fun completedManga(page: Int = 1, showAdultContent: Boolean = false) = jikanSearchClient.completedManga(page, showAdultContent)
    suspend fun trendingAnime(page: Int = 1, showAdultContent: Boolean = false) = jikanSearchClient.trendingAnime(page, showAdultContent)
    suspend fun movieAnime(page: Int = 1, showAdultContent: Boolean = false) = jikanSearchClient.movieAnime(page, showAdultContent)
    suspend fun trendingManga(page: Int = 1, showAdultContent: Boolean = false) = jikanSearchClient.trendingManga(page, showAdultContent)
    suspend fun newlyAddedAnime(page: Int = 1, showAdultContent: Boolean = false) = jikanSearchClient.newlyAddedAnime(page, showAdultContent)
    suspend fun newlyAddedManga(page: Int = 1, showAdultContent: Boolean = false) = jikanSearchClient.newlyAddedManga(page, showAdultContent)
    suspend fun seasonalAnime(
        page: Int = 1,
        showAdultContent: Boolean = false,
        year: Int? = null,
        season: String? = null,
        sort: String? = null
    ) = jikanSearchClient.seasonalAnime(page, showAdultContent, year, season, sort)

    // AniList Specific Search Methods
    suspend fun aniListTopAnime(page: Int = 1, showAdultContent: Boolean = false) = aniListSearchClient.aniListTopAnime(page, showAdultContent)
    suspend fun aniListAiringAnime(page: Int = 1, showAdultContent: Boolean = false) = aniListSearchClient.aniListAiringAnime(page, showAdultContent)
    suspend fun aniListUpcomingAnime(page: Int = 1, showAdultContent: Boolean = false) = aniListSearchClient.aniListUpcomingAnime(page, showAdultContent)
    suspend fun aniListTopManga(page: Int = 1, showAdultContent: Boolean = false) = aniListSearchClient.aniListTopManga(page, showAdultContent)
    suspend fun aniListPublishingManga(page: Int = 1, showAdultContent: Boolean = false) = aniListSearchClient.aniListPublishingManga(page, showAdultContent)
    suspend fun aniListTrendingAnime(page: Int = 1, showAdultContent: Boolean = false) = aniListSearchClient.aniListTrendingAnime(page, showAdultContent)
    suspend fun aniListMovieAnime(page: Int = 1, showAdultContent: Boolean = false) = aniListSearchClient.aniListMovieAnime(page, showAdultContent)
    suspend fun aniListTrendingManga(page: Int = 1, showAdultContent: Boolean = false) = aniListSearchClient.aniListTrendingManga(page, showAdultContent)
    suspend fun aniListNewlyAddedAnime(page: Int = 1, showAdultContent: Boolean = false) = aniListSearchClient.aniListNewlyAddedAnime(page, showAdultContent)
    suspend fun aniListNewlyAddedManga(page: Int = 1, showAdultContent: Boolean = false) = aniListSearchClient.aniListNewlyAddedManga(page, showAdultContent)
    suspend fun aniListSeasonalAnime(
        page: Int = 1,
        showAdultContent: Boolean = false,
        year: Int? = null,
        season: String? = null,
        sort: String? = null
    ) = aniListSearchClient.aniListSeasonalAnime(
        page = page,
        showAdultContent = showAdultContent,
        year = year,
        season = season,
        sort = if (sort != null) listOf(sort) else listOf("POPULARITY_DESC")
    )

    // Detail & Synopsis
    suspend fun fetchSynopsis(source: String, externalId: Int?, mediaType: MediaType) = detailClient.fetchSynopsis(source, externalId, mediaType)
    suspend fun fetchDetail(source: String, externalId: Int?, mediaType: MediaType, tmdbId: Int? = null, realMalId: Int? = null, title: String? = null) = detailClient.fetchDetail(source, externalId, mediaType, tmdbId, realMalId, title)

    // Characters & Staff
    suspend fun fetchCharacters(source: String, externalId: Int?, mediaType: MediaType, realMalId: Int? = null, tmdbId: Int? = null) = characterClient.fetchCharacters(source, externalId, mediaType, realMalId, tmdbId)
    suspend fun fetchCharacterDetail(source: String, characterId: Int, name: String? = null) = characterClient.fetchCharacterDetail(source, characterId, name)
    suspend fun fetchStaff(source: String, externalId: Int?, mediaType: MediaType, tmdbId: Int? = null, realMalId: Int? = null) = staffClient.fetchStaff(source, externalId, mediaType, tmdbId, realMalId)
    suspend fun fetchStaffDetail(source: String, staffId: Int, name: String? = null) = staffClient.fetchStaffDetail(source, staffId, name)
    suspend fun fetchStudioDetail(source: String, studioId: Int, name: String? = null) = studioClient.fetchStudioDetail(source, studioId, name)

    // Relations, Stats, Reviews, Episodes
    suspend fun fetchRelations(source: String, externalId: Int?, mediaType: MediaType, tmdbId: Int? = null, realMalId: Int? = null) = mediaRelationsClient.fetchRelations(source, externalId, mediaType, tmdbId, realMalId)
    suspend fun fetchRecommendations(source: String, externalId: Int?, mediaType: MediaType, tmdbId: Int? = null, realMalId: Int? = null) = mediaRelationsClient.fetchRecommendations(source, externalId, mediaType, tmdbId, realMalId)
    suspend fun fetchStats(source: String, externalId: Int?, mediaType: MediaType, realMalId: Int? = null) = mediaSocialClient.fetchStats(source, externalId, mediaType, realMalId)
    suspend fun fetchReviews(source: String, externalId: Int?, mediaType: MediaType, page: Int = 1, tmdbId: Int? = null, realMalId: Int? = null) = mediaSocialClient.fetchReviews(source, externalId, mediaType, page, tmdbId, realMalId)
    suspend fun fetchEpisodes(
        source: String,
        externalId: Int?,
        mediaType: MediaType,
        realMalId: Int? = null,
        totalEpisodes: Int? = null,
        context: android.content.Context? = null,
        targetSeason: Int? = null,
        tmdbId: Int? = null
    ) = mediaTabsClient.fetchEpisodes(source, externalId, mediaType, realMalId, totalEpisodes, context, targetSeason, tmdbId)

    suspend fun fetchForumTopics(source: String, externalId: Int, mediaType: MediaType, page: Int = 1) =
        mediaSocialClient.fetchForumTopics(source, externalId, mediaType, page)

    suspend fun fetchActivities(source: String, externalId: Int, page: Int = 1, mediaType: MediaType = MediaType.Anime) =
        mediaSocialClient.fetchActivities(source, externalId, page, mediaType)

    suspend fun fetchActivityReplies(activityId: Int) =
        mediaSocialClient.fetchActivityReplies(activityId)

    suspend fun fetchForumTopicReplies(topicId: Int, page: Int = 1) =
        mediaSocialClient.fetchForumTopicReplies(topicId, page)

    suspend fun toggleLike(id: Int, type: String) =
        mediaMutationsClient.toggleLike(id, type)

    suspend fun postReply(targetId: Int, isActivity: Boolean, text: String, parentCommentId: Int? = null) =
        mediaMutationsClient.postReply(targetId, isActivity, text, parentCommentId)

    suspend fun toggleThreadSubscription(threadId: Int) =
        mediaMutationsClient.toggleThreadSubscription(threadId)

    suspend fun rateReview(reviewId: Int, rating: String) =
        mediaMutationsClient.rateReview(reviewId, rating)

    suspend fun deleteActivity(activityId: Int) =
        mediaMutationsClient.deleteActivity(activityId)
}