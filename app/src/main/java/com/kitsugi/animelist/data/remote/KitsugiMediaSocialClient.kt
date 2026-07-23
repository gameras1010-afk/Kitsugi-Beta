package com.kitsugi.animelist.data.remote

import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * İstatistik, inceleme, forum konuları ve aktivite verilerini çeken istemci.
 * [KitsugiMediaTabsClient]'dan bölünmüştür.
 */
class KitsugiMediaSocialClient {

    private val relationsClient = KitsugiMediaRelationsClient()

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun fetchStats(
        source: String,
        externalId: Int?,
        mediaType: MediaType,
        realMalId: Int? = null
    ): KitsugiStats? {
        return withContext(Dispatchers.IO) {
            if (externalId == null || externalId <= 0) return@withContext null
            when (source.lowercase()) {
                "tmdb" -> {
                    val effectiveTmdbId = externalId
                    val isMovie = mediaType == MediaType.Movie
                    if (mediaType == MediaType.Anime) {
                        // Anime için önce MAL ID'si üzerinden Jikan'a git
                        val malId = realMalId ?: DetailCache.getMediaDetail("tmdb", externalId)?.realMalId
                        if (malId != null && malId > 0) {
                            fetchStats("jikan", malId, MediaType.Anime, null)
                        } else {
                            val resolved = KitsugiIdResolver.resolveIds(malId = null, aniListId = null, tmdbId = effectiveTmdbId)
                            val resolvedMalId = resolved.malId
                            if (resolvedMalId != null && resolvedMalId > 0) {
                                fetchStats("jikan", resolvedMalId, MediaType.Anime, null)
                            } else {
                                // Fallback: TMDB kendi istatistiğini döndür
                                TmdbApiClient().fetchStats(effectiveTmdbId, isMovie)
                            }
                        }
                    } else {
                        // Film / Dizi → doğrudan TMDB istatistiği
                        TmdbApiClient().fetchStats(effectiveTmdbId, isMovie)
                    }
                }
                "simkl" -> {
                    if (mediaType == MediaType.Anime) {
                        val malId = realMalId ?: DetailCache.getMediaDetail("simkl", externalId)?.realMalId
                        if (malId != null && malId > 0) {
                            fetchStats("jikan", malId, mediaType, null)
                        } else {
                            val resolved = KitsugiIdResolver.resolveIds(malId = null, aniListId = null, tmdbId = null)
                            val resolvedMalId = resolved.malId
                            if (resolvedMalId != null && resolvedMalId > 0) {
                                fetchStats("jikan", resolvedMalId, mediaType, null)
                            } else null
                        }
                    } else {
                        // Simkl Film/Dizi → TMDB ID'sini bul ve TMDB istatistiğini çek
                        val detail = DetailCache.getMediaDetail("simkl", externalId)
                        val resolvedTmdb = detail?.tmdbId ?: run {
                            val malIdForResolve = realMalId ?: detail?.realMalId
                            KitsugiIdResolver.resolveIds(malId = malIdForResolve, aniListId = null, tmdbId = null).tmdbId
                        }
                        if (resolvedTmdb != null && resolvedTmdb > 0) {
                            val isMovie = mediaType == MediaType.Movie
                            TmdbApiClient().fetchStats(resolvedTmdb, isMovie)
                        } else null
                    }
                }
                "jikan", "mal" -> {
                    val aniStats = fetchStatsFromAniList(externalId, mediaType)
                    if (aniStats != null && (aniStats.rankings.isNotEmpty() || aniStats.scoreDistribution.isNotEmpty())) {
                        aniStats
                    } else {
                        fetchStatsFromJikan(externalId, mediaType)
                    }
                }
                "anilist"      -> fetchStatsFromAniList(externalId, mediaType)
                else           -> null
            }
        }
    }

    private suspend fun fetchStatsFromJikan(externalId: Int, mediaType: MediaType): KitsugiStats? {
        val endpoint = if (mediaType == MediaType.Anime) "anime" else "manga"
        val url = java.net.URL("https://api.jikan.moe/v4/$endpoint/$externalId/statistics")
        return runCatching {
            KitsugiApiBase.runWithRateLimit {
                val response = KitsugiApiBase.executeGetRequest(url) ?: return@runWithRateLimit null
                val root = JSONObject(response)
                val data = root.optJSONObject("data") ?: return@runWithRateLimit null
                val watching   = data.optionalPositiveInt("watching")
                val completed  = data.optionalPositiveInt("completed")
                val onHold     = data.optionalPositiveInt("on_hold")
                val dropped    = data.optionalPositiveInt("dropped")
                val planToWatch = data.optionalPositiveInt("plan_to_watch")
                val scores = data.optJSONArray("scores")
                val scoreList = mutableListOf<KitsugiScoreStat>()
                if (scores != null) {
                    for (i in 0 until scores.length()) {
                        val s = scores.optJSONObject(i) ?: continue
                        scoreList.add(KitsugiScoreStat(s.optInt("score"), s.optInt("votes")))
                    }
                }
                KitsugiStats(
                    watching = watching,
                    completed = completed,
                    planned = planToWatch,
                    dropped = dropped,
                    paused = onHold,
                    scoreDistribution = scoreList
                )
            }
        }.getOrNull()
    }

    private suspend fun fetchStatsFromAniList(externalId: Int, mediaType: MediaType): KitsugiStats? {
        val aniListId = if (externalId >= 100_000_000) externalId - 100_000_000 else null
        val idParam  = if (aniListId != null) "\$id: Int" else "\$idMal: Int"
        val idFilter = if (aniListId != null) "id: \$id"  else "idMal: \$idMal"
        val query = """
            query (${'$'}type: MediaType, $idParam) {
                Media($idFilter, type: ${'$'}type) {
                    rankings {
                        id rank type format year season allTime context
                    }
                    stats {
                        statusDistribution { status amount }
                        scoreDistribution  { score amount }
                    }
                }
            }
        """.trimIndent()
        val variables = JSONObject().put("type", if (mediaType == MediaType.Anime) "ANIME" else "MANGA")
        if (aniListId != null) variables.put("id", aniListId) else variables.put("idMal", externalId)

        return runCatching {
            val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching null
            val root = JSONObject(response)
            val mediaObj = root.optJSONObject("data")?.optJSONObject("Media") ?: return@runCatching null
            
            val rankingsArr = mediaObj.optJSONArray("rankings")
            val rankingsList = mutableListOf<KitsugiRanking>()
            if (rankingsArr != null) {
                for (i in 0 until rankingsArr.length()) {
                    val r = rankingsArr.optJSONObject(i) ?: continue
                    val rank = r.optInt("rank", 0)
                    if (rank <= 0) continue
                    val type = r.optNullableString("type").orEmpty()
                    val context = r.optNullableString("context").orEmpty()
                    val allTime = r.optBoolean("allTime", false)
                    val year = r.optionalPositiveInt("year")
                    val season = r.optNullableString("season")
                    rankingsList.add(KitsugiRanking(rank, type, context, allTime, year, season))
                }
            }

            val statsObj = mediaObj.optJSONObject("stats") ?: return@runCatching null
            val statusDist = statsObj.optJSONArray("statusDistribution")
            val scoreDist  = statsObj.optJSONArray("scoreDistribution")
            var watching: Int? = null; var completed: Int? = null
            var planned: Int? = null;  var dropped: Int? = null; var paused: Int? = null
            if (statusDist != null) {
                for (i in 0 until statusDist.length()) {
                    val item = statusDist.optJSONObject(i) ?: continue
                    when (item.optNullableString("status").orEmpty()) {
                        "CURRENT"   -> watching  = item.optInt("amount")
                        "COMPLETED" -> completed = item.optInt("amount")
                        "PLANNING"  -> planned   = item.optInt("amount")
                        "DROPPED"   -> dropped   = item.optInt("amount")
                        "PAUSED"    -> paused    = item.optInt("amount")
                    }
                }
            }
            val scoreList = mutableListOf<KitsugiScoreStat>()
            if (scoreDist != null) {
                for (i in 0 until scoreDist.length()) {
                    val item = scoreDist.optJSONObject(i) ?: continue
                    scoreList.add(KitsugiScoreStat(item.optInt("score"), item.optInt("amount")))
                }
            }
            KitsugiStats(
                watching = watching,
                completed = completed,
                planned = planned,
                dropped = dropped,
                paused = paused,
                scoreDistribution = scoreList,
                rankings = rankingsList
            )
        }.getOrNull()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reviews
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun fetchReviews(
        source: String,
        externalId: Int?,
        mediaType: MediaType,
        page: Int = 1,
        tmdbId: Int? = null,
        realMalId: Int? = null
    ): List<KitsugiReview> {
        return withContext(Dispatchers.IO) {
            if (externalId == null || externalId <= 0) return@withContext emptyList()
            when (source.lowercase()) {
                "simkl" -> {
                    val list = mutableListOf<KitsugiReview>()
                    if (mediaType == MediaType.Anime) {
                        val malId = realMalId ?: DetailCache.getMediaDetail("simkl", externalId)?.realMalId
                        if (malId != null && malId > 0) {
                            val malList = fetchReviews("jikan", malId, mediaType, page, null, null)
                            if (malList.isNotEmpty()) return@withContext malList
                        }
                    }

                    // Film/Dizi ise veya anime için fallback olarak TMDB reviews çek
                    val resolvedTmdb = tmdbId ?: run {
                        val malIdForResolve = realMalId ?: DetailCache.getMediaDetail("simkl", externalId)?.realMalId
                        KitsugiIdResolver.resolveIds(malId = malIdForResolve, aniListId = null, tmdbId = tmdbId).tmdbId
                    }

                    if (resolvedTmdb != null && resolvedTmdb > 0) {
                        val isMovie = mediaType == MediaType.Movie
                        val tmdbReviews = TmdbApiClient().fetchReviews(resolvedTmdb, isMovie)
                        list.addAll(tmdbReviews)
                    }

                    list.distinctBy { it.username + it.fullText.take(20) }
                }
                "tmdb" -> {
                    val list = mutableListOf<KitsugiReview>()
                    val effectiveTmdbId = tmdbId ?: externalId
                    val isMovie = mediaType == MediaType.Movie

                    if (effectiveTmdbId > 0) {
                        val tmdbReviews = TmdbApiClient().fetchReviews(effectiveTmdbId, isMovie)
                        list.addAll(tmdbReviews)
                    }

                    if (list.isEmpty() && mediaType == MediaType.Anime) {
                        val malId = realMalId ?: DetailCache.getMediaDetail("tmdb", externalId)?.realMalId
                        val finalMalId = if (malId != null && malId > 0) {
                            malId
                        } else {
                            val resolved = KitsugiIdResolver.resolveIds(malId = null, aniListId = null, tmdbId = effectiveTmdbId)
                            resolved.malId
                        }
                        if (finalMalId != null && finalMalId > 0) {
                            return@withContext fetchReviews("jikan", finalMalId, mediaType, page, null, null)
                        }
                    }

                    list.distinctBy { it.username + it.fullText.take(20) }
                }
                "jikan", "mal" -> fetchReviewsFromJikan(externalId, mediaType, page)
                "anilist"      -> fetchReviewsFromAniList(externalId, mediaType, page)
                else           -> emptyList()
            }
        }
    }

    private suspend fun fetchReviewsFromJikan(externalId: Int, mediaType: MediaType, page: Int): List<KitsugiReview> {
        val endpoint = if (mediaType == MediaType.Anime) "anime" else "manga"
        val url = java.net.URL("https://api.jikan.moe/v4/$endpoint/$externalId/reviews?page=$page")
        return runCatching {
            KitsugiApiBase.runWithRateLimit {
                val response = KitsugiApiBase.executeGetRequest(url) ?: return@runWithRateLimit emptyList()
                val root = JSONObject(response)
                val data = root.optJSONArray("data") ?: return@runWithRateLimit emptyList()
                val list = mutableListOf<KitsugiReview>()
                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    val score = item.optionalPositiveInt("score")
                    val reviewText = item.optNullableString("review")?.cleanApiText().orEmpty()
                    val summary = if (reviewText.length > 280) reviewText.take(280) + "..." else reviewText
                    val rawDate = item.optNullableString("date")
                    val dateText = rawDate?.let {
                        try {
                            val sdfIn = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                            val sdfOut = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                            sdfOut.format(sdfIn.parse(it)!!)
                        } catch (_: Exception) { it.take(10) }
                    }
                    val reactions = item.optJSONObject("reactions")
                    val helpfulCount = reactions?.optInt("overall") ?: item.optInt("votes")
                    val userObj  = item.optJSONObject("user")
                    val username = userObj?.optNullableString("username") ?: "Kullanıcı"
                    val avatarUrl = userObj?.optJSONObject("images")?.optJSONObject("jpg")?.optNullableString("image_url")
                    list.add(KitsugiReview(
                        id = null, username = username, avatarUrl = avatarUrl,
                        score = score, summary = summary, fullText = reviewText,
                        dateText = dateText, helpfulCount = if (helpfulCount > 0) helpfulCount else null,
                        ratingAmount = null, userRating = null
                    ))
                }
                list
            }
        }.getOrElse { emptyList() }
    }

    private suspend fun fetchReviewsFromAniList(externalId: Int, mediaType: MediaType, page: Int): List<KitsugiReview> {
        val aniListId = if (externalId >= 100_000_000) {
            externalId - 100_000_000
        } else {
            relationsClient.resolveAniListId(externalId, mediaType)
        }
        val idParam  = if (aniListId != null) "\$id: Int" else "\$idMal: Int"
        val idFilter = if (aniListId != null) "id: \$id"  else "idMal: \$idMal"
        val query = """
            query (${'$'}type: MediaType, $idParam, ${'$'}page: Int) {
                Media($idFilter, type: ${'$'}type) {
                    reviews(page: ${'$'}page, perPage: 10, sort: RATING_DESC) {
                        nodes {
                            id summary body score rating ratingAmount userRating createdAt
                            user { id name avatar { medium } }
                        }
                    }
                }
            }
        """.trimIndent()
        val variables = JSONObject()
            .put("type", if (mediaType == MediaType.Anime) "ANIME" else "MANGA")
            .put("page", page)
        if (aniListId != null) variables.put("id", aniListId) else variables.put("idMal", externalId)

        return runCatching {
            val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching emptyList()
            val nodes = JSONObject(response)
                .optJSONObject("data")?.optJSONObject("Media")
                ?.optJSONObject("reviews")?.optJSONArray("nodes") ?: return@runCatching emptyList()
            val list = mutableListOf<KitsugiReview>()
            for (i in 0 until nodes.length()) {
                val node = nodes.optJSONObject(i) ?: continue
                val createdAt = node.optInt("createdAt")
                val dateText = if (createdAt > 0) {
                    try {
                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(createdAt * 1000L))
                    } catch (_: Exception) { null }
                } else null
                val summary  = node.optNullableString("summary")?.cleanApiText().orEmpty()
                val body     = node.optNullableString("body")?.cleanApiText().orEmpty()
                val userObj  = node.optJSONObject("user")
                list.add(KitsugiReview(
                    id = node.optInt("id"),
                    userId = userObj?.optInt("id"),
                    username = userObj?.optNullableString("name") ?: "Kullanıcı",
                    avatarUrl = userObj?.optJSONObject("avatar")?.optNullableString("medium"),
                    score = node.optionalPositiveInt("score"),
                    summary = summary,
                    fullText = if (body.isNotBlank()) body else summary,
                    dateText = dateText,
                    helpfulCount = node.optionalPositiveInt("rating"),
                    ratingAmount = node.optionalPositiveInt("ratingAmount"),
                    userRating   = node.optNullableString("userRating")
                ))
            }
            list
        }.getOrElse { emptyList() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Forum Topics
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun fetchForumTopics(
        source: String,
        externalId: Int,
        mediaType: MediaType,
        page: Int = 1
    ): List<KitsugiForumTopic> {
        return withContext(Dispatchers.IO) {
            when (source.lowercase()) {
                "jikan", "mal" -> {
                    val aniListId = relationsClient.resolveAniListId(externalId, mediaType)
                    if (aniListId != null) {
                        return@withContext fetchForumTopics("anilist", aniListId, mediaType, page)
                    }
                    if (page > 1) return@withContext emptyList()
                    fetchForumTopicsFromJikan(externalId, mediaType)
                }
                "anilist" -> {
                    val aniListId = if (externalId >= 100_000_000) {
                        externalId - 100_000_000
                    } else {
                        relationsClient.resolveAniListId(externalId, mediaType) ?: externalId
                    }
                    fetchForumTopicsFromAniList(aniListId, page)
                }
                else -> emptyList()
            }
        }
    }

    private suspend fun fetchForumTopicsFromJikan(externalId: Int, mediaType: MediaType): List<KitsugiForumTopic> {
        val pathType = if (mediaType == MediaType.Anime) "anime" else "manga"
        val url = java.net.URL("https://api.jikan.moe/v4/$pathType/$externalId/forum")
        return KitsugiApiBase.runWithRateLimit {
            val response = KitsugiApiBase.executeGetRequest(url) ?: return@runWithRateLimit emptyList()
            runCatching {
                val root = JSONObject(response)
                val data = root.optJSONArray("data") ?: return@runCatching emptyList()
                val list = mutableListOf<KitsugiForumTopic>()
                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    list.add(KitsugiForumTopic(
                        id = item.optInt("mal_id"),
                        title = item.optString("title"),
                        commentCount = item.optInt("comments"),
                        viewCount = 0,
                        username = item.optString("author_username"),
                        avatarUrl = null,
                        dateText = item.optNullableString("date")?.take(10)
                    ))
                }
                list
            }.getOrElse { emptyList() }
        }
    }

    private suspend fun fetchForumTopicsFromAniList(externalId: Int, page: Int): List<KitsugiForumTopic> {
        val aniListId = if (externalId >= 100_000_000) externalId - 100_000_000 else externalId
        val query = """
            query (${'$'}mediaId: Int, ${'$'}page: Int) {
                Page(page: ${'$'}page, perPage: 15) {
                    threads(mediaCategoryId: ${'$'}mediaId) {
                        id title replyCount viewCount likeCount isLiked createdAt
                        user { id name avatar { medium } }
                    }
                }
            }
        """.trimIndent()
        val variables = JSONObject().put("mediaId", aniListId).put("page", page)
        return runCatching {
            val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching emptyList()
            val threads = JSONObject(response).optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("threads") ?: return@runCatching emptyList()
            val list = mutableListOf<KitsugiForumTopic>()
            for (i in 0 until threads.length()) {
                val item = threads.optJSONObject(i) ?: continue
                val createdAt = item.optInt("createdAt")
                val dateText = if (createdAt > 0) {
                    try { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(createdAt * 1000L)) } catch (_: Exception) { null }
                } else null
                val userObj = item.optJSONObject("user")
                list.add(KitsugiForumTopic(
                    id = item.optInt("id"), title = item.optString("title"),
                    commentCount = item.optInt("replyCount"), viewCount = item.optInt("viewCount"),
                    username = userObj?.optNullableString("name") ?: "Kullanıcı",
                    avatarUrl = userObj?.optJSONObject("avatar")?.optNullableString("medium"),
                    dateText = dateText, likeCount = item.optInt("likeCount", 0),
                    isLiked = item.optBoolean("isLiked", false),
                    userId = userObj?.optInt("id")
                ))
            }
            list
        }.getOrElse { emptyList() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Activities
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun fetchActivities(
        source: String,
        externalId: Int,
        page: Int = 1,
        mediaType: MediaType = MediaType.Anime
    ): List<KitsugiActivity> {
        return withContext(Dispatchers.IO) {
            val aniListId = if (source.lowercase() == "anilist") {
                if (externalId >= 100_000_000) {
                    externalId - 100_000_000
                } else {
                    relationsClient.resolveAniListId(externalId, mediaType) ?: externalId
                }
            } else {
                relationsClient.resolveAniListId(externalId, mediaType) ?: return@withContext emptyList()
            }

            val query = """
                query (${'$'}mediaId: Int, ${'$'}page: Int) {
                    Page(page: ${'$'}page, perPage: 15) {
                        activities(mediaId: ${'$'}mediaId, sort: [ID_DESC]) {
                            ... on ListActivity {
                                id status progress createdAt likeCount isLiked
                                user { id name avatar { medium } }
                                media { id type isAdult title { romaji english native } coverImage { large } }
                            }
                            ... on TextActivity {
                                id text createdAt likeCount isLiked
                                user { id name avatar { medium } }
                            }
                        }
                    }
                }
            """.trimIndent()
            val variables = JSONObject().put("mediaId", aniListId).put("page", page)
            runCatching {
                val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching emptyList()
                val activities = JSONObject(response).optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("activities") ?: return@runCatching emptyList()
                val list = mutableListOf<KitsugiActivity>()
                for (i in 0 until activities.length()) {
                    val item = activities.optJSONObject(i) ?: continue
                    list.add(parseActivity(item))
                }
                list
            }.getOrElse { emptyList() }
        }
    }

    suspend fun fetchActivityReplies(activityId: Int): KitsugiActivity? {
        return withContext(Dispatchers.IO) {
            val query = """
                query (${'$'}activityId: Int) {
                    Activity(id: ${'$'}activityId) {
                        ... on ListActivity {
                            id status progress createdAt likeCount isLiked
                            user { id name avatar { medium } }
                            media { id type isAdult title { romaji english native } coverImage { large } }
                            replies { id text createdAt likeCount isLiked user { id name avatar { medium } } }
                        }
                        ... on TextActivity {
                            id text createdAt likeCount isLiked
                            user { id name avatar { medium } }
                            replies { id text createdAt likeCount isLiked user { id name avatar { medium } } }
                        }
                    }
                }
            """.trimIndent()
            val variables = JSONObject().put("activityId", activityId)
            runCatching {
                val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching null
                val actObj = JSONObject(response).optJSONObject("data")?.optJSONObject("Activity") ?: return@runCatching null
                val base = parseActivity(actObj)
                val repliesArr = actObj.optJSONArray("replies")
                val repliesList = mutableListOf<KitsugiActivityReply>()
                if (repliesArr != null) {
                    for (i in 0 until repliesArr.length()) {
                        val rep = repliesArr.optJSONObject(i) ?: continue
                        val rCreatedAt = rep.optInt("createdAt")
                        val rDateText = if (rCreatedAt > 0) {
                            try { java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(rCreatedAt * 1000L)) } catch (_: Exception) { null }
                        } else null
                        val rUser = rep.optJSONObject("user")
                        repliesList.add(KitsugiActivityReply(
                            id = rep.optInt("id"), text = rep.optString("text").cleanApiText(),
                            dateText = rDateText,
                            username = rUser?.optNullableString("name") ?: "Kullanıcı",
                            avatarUrl = rUser?.optJSONObject("avatar")?.optNullableString("medium"),
                            likeCount = rep.optInt("likeCount", 0), isLiked = rep.optBoolean("isLiked", false),
                            userId = rUser?.optInt("id")
                        ))
                    }
                }
                base.copy(replies = repliesList)
            }.getOrNull()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Forum Replies
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun fetchForumTopicReplies(topicId: Int, page: Int = 1): List<KitsugiForumReply> {
        return withContext(Dispatchers.IO) {
            val query = """
                query (${'$'}threadId: Int, ${'$'}page: Int) {
                    Page(page: ${'$'}page, perPage: 30) {
                        threadComments(threadId: ${'$'}threadId) {
                            id comment createdAt likeCount isLiked
                            user { id name avatar { medium } }
                        }
                    }
                }
            """.trimIndent()
            val variables = JSONObject().put("threadId", topicId).put("page", page)
            runCatching {
                val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching emptyList()
                val comments = JSONObject(response).optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("threadComments") ?: return@runCatching emptyList()
                val list = mutableListOf<KitsugiForumReply>()
                for (i in 0 until comments.length()) {
                    val item = comments.optJSONObject(i) ?: continue
                    val createdAt = item.optInt("createdAt")
                    val dateText = if (createdAt > 0) {
                        try { java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(createdAt * 1000L)) } catch (_: Exception) { null }
                    } else null
                    val userObj = item.optJSONObject("user")
                    list.add(KitsugiForumReply(
                        id = item.optInt("id"), comment = item.optString("comment").cleanApiText(),
                        dateText = dateText,
                        username = userObj?.optNullableString("name") ?: "Kullanıcı",
                        avatarUrl = userObj?.optJSONObject("avatar")?.optNullableString("medium"),
                        likeCount = item.optInt("likeCount", 0), isLiked = item.optBoolean("isLiked", false),
                        userId = userObj?.optInt("id")
                    ))
                }
                list
            }.getOrElse { emptyList() }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseActivity(item: JSONObject): KitsugiActivity {
        val createdAt = item.optInt("createdAt")
        val dateText = if (createdAt > 0) {
            try { java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(createdAt * 1000L)) } catch (_: Exception) { null }
        } else null
        val userObj = item.optJSONObject("user")
        var mediaTitleRomaji: String? = null; var mediaTitleEnglish: String? = null
        var mediaTitleNative: String? = null;  var mediaCoverUrl: String? = null
        var mediaId: Int? = null; var mediaType: String? = null; var isAdult = false
        if (item.has("media")) {
            val mediaObj = item.optJSONObject("media")
            if (mediaObj != null) {
                mediaId           = mediaObj.optInt("id")
                mediaType         = mediaObj.optNullableString("type")
                isAdult           = mediaObj.optBoolean("isAdult", false)
                val titleObj      = mediaObj.optJSONObject("title")
                mediaTitleRomaji  = titleObj?.optNullableString("romaji")
                mediaTitleEnglish = titleObj?.optNullableString("english")
                mediaTitleNative  = titleObj?.optNullableString("native")
                mediaCoverUrl     = mediaObj.optJSONObject("coverImage")?.optNullableString("large")
            }
        }
        val mediaTitle = mediaTitleRomaji ?: mediaTitleEnglish ?: mediaTitleNative
        val text = if (item.has("text")) item.optString("text")
                   else formatActivityText(item.optNullableString("status"), item.optNullableString("progress"), mediaTitle)
        return KitsugiActivity(
            id = item.optInt("id"), text = text.cleanApiText(), dateText = dateText,
            username = userObj?.optNullableString("name") ?: "Kullanıcı",
            avatarUrl = userObj?.optJSONObject("avatar")?.optNullableString("medium"),
            mediaTitle = mediaTitle, mediaTitleRomaji = mediaTitleRomaji,
            mediaTitleEnglish = mediaTitleEnglish, mediaTitleNative = mediaTitleNative,
            mediaCoverUrl = mediaCoverUrl,
            likeCount = item.optInt("likeCount", 0), isLiked = item.optBoolean("isLiked", false),
            mediaId = mediaId, mediaType = mediaType, isAdult = isAdult,
            userId = userObj?.optInt("id")
        )
    }

    private fun formatActivityText(status: String?, progress: String?, mediaTitle: String? = null): String {
        val cleanStatus   = status?.lowercase()?.trim().orEmpty()
        val cleanProgress = progress?.trim()?.takeIf { it.isNotBlank() && it != "null" }
        val prefix = if (!mediaTitle.isNullOrBlank()) "**$mediaTitle** " else ""
        return when {
            cleanStatus.contains("completed")    -> if (cleanProgress != null) "${prefix}serisini tamamladı ($cleanProgress)" else "${prefix}serisini tamamladı"
            cleanStatus.contains("watched")      -> if (cleanProgress != null) "${prefix}$cleanProgress. bölümü izledi" else "${prefix}izledi"
            cleanStatus.contains("watching")     -> if (cleanProgress != null) "${prefix}$cleanProgress. bölümü izliyor" else "${prefix}izliyor"
            cleanStatus.contains("plan")         -> if (cleanProgress != null) "${prefix}izlemeyi planlıyor ($cleanProgress)" else "${prefix}izlemeyi planlıyor"
            cleanStatus.contains("dropped")      -> if (cleanProgress != null) "${prefix}bıraktı ($cleanProgress)" else "${prefix}bıraktı"
            cleanStatus.contains("paused")       -> if (cleanProgress != null) "${prefix}ara verdi ($cleanProgress)" else "${prefix}ara verdi"
            cleanStatus.contains("rewatc")       -> if (cleanProgress != null) "${prefix}tekrar izledi ($cleanProgress)" else "${prefix}tekrar izledi"
            else -> if (cleanProgress != null) "${prefix}${status.orEmpty()} ($cleanProgress)" else "${prefix}${status.orEmpty()}"
        }
    }
}
