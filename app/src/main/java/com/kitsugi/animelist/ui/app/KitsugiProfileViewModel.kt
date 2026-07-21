package com.kitsugi.animelist.ui.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import com.kitsugi.animelist.data.local.MediaEntryRepository
import com.kitsugi.animelist.data.remote.optNullableString
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class KitsugiProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val client = com.kitsugi.animelist.core.network.KitsugiHttpClient.client

    // Local library stats state
    var localStats by mutableStateOf(LocalStats())
        private set

    // Selected sub-tab persistence (0: AniList, 1: MAL, 2: Simkl)
    var activeSubTab by mutableStateOf(0)

    // AniList profile UI state
    private val _aniListState = MutableStateFlow(AniListProfileState())
    val aniListState: StateFlow<AniListProfileState> = _aniListState.asStateFlow()

    // MyAnimeList profile UI state
    private val _malState = MutableStateFlow(MalProfileState())
    val malState: StateFlow<MalProfileState> = _malState.asStateFlow()

    // Simkl profile UI state
    private val _simklState = MutableStateFlow(SimklProfileState())
    val simklState: StateFlow<SimklProfileState> = _simklState.asStateFlow()

    fun initLocalStats(entries: List<MediaEntry>) {
        viewModelScope.launch {
            val anime = entries.filter { it.type == MediaType.Anime || it.type == MediaType.Movie || it.type == MediaType.TvShow }
            val manga = entries.filter { it.type == MediaType.Manga }

            val animeScores = anime.mapNotNull { it.score }
            val avgAnimeScore = if (animeScores.isNotEmpty()) animeScores.average() else 0.0

            val mangaScores = manga.mapNotNull { it.score }
            val avgMangaScore = if (mangaScores.isNotEmpty()) mangaScores.average() else 0.0

            localStats = LocalStats(
                totalAnime = anime.size,
                watchingAnime = anime.count { it.status == WatchStatus.Watching },
                completedAnime = anime.count { it.status == WatchStatus.Completed },
                plannedAnime = anime.count { it.status == WatchStatus.Planned },
                pausedAnime = anime.count { it.status == WatchStatus.Paused },
                droppedAnime = anime.count { it.status == WatchStatus.Dropped },
                avgAnimeScore = avgAnimeScore,

                totalManga = manga.size,
                readingManga = manga.count { it.status == WatchStatus.Watching },
                completedManga = manga.count { it.status == WatchStatus.Completed },
                plannedManga = manga.count { it.status == WatchStatus.Planned },
                pausedManga = manga.count { it.status == WatchStatus.Paused },
                droppedManga = manga.count { it.status == WatchStatus.Dropped },
                avgMangaScore = avgMangaScore
            )
        }
    }

    fun refreshActiveProfile() {
        when (activeSubTab) {
            0 -> fetchAniListProfile()
            1 -> fetchMalProfile()
            2 -> fetchSimklProfile()
        }
    }

    // --- ANILIST API FETCHING ---
    fun fetchAniListProfile() {
        val token = ExternalAuthManager.getAniListToken(context)
        if (token.isNullOrBlank()) {
            _aniListState.update { it.copy(isConnected = false, isLoading = false, error = "Bağlantı bulunamadı") }
            return
        }

        _aniListState.update { it.copy(isConnected = true, isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val profileQuery = """
                    query {
                        Viewer {
                            id
                            name
                            about(asHtml: false)
                            avatar {
                                large
                            }
                            bannerImage
                            donatorBadge
                            donatorTier
                            statistics {
                                anime {
                                    count
                                    episodesWatched
                                    minutesWatched
                                    meanScore
                                    standardDeviation
                                    scores(sort: MEAN_SCORE) {
                                        count
                                        minutesWatched
                                        meanScore
                                        score
                                    }
                                    lengths {
                                        length
                                        count
                                        minutesWatched
                                        meanScore
                                    }
                                    formats {
                                        count
                                        minutesWatched
                                        meanScore
                                        format
                                    }
                                    statuses {
                                        count
                                        minutesWatched
                                        meanScore
                                        status
                                    }
                                    countries {
                                        count
                                        minutesWatched
                                        meanScore
                                        country
                                    }
                                    releaseYears {
                                        count
                                        minutesWatched
                                        meanScore
                                        releaseYear
                                    }
                                    startYears {
                                        count
                                        minutesWatched
                                        meanScore
                                        startYear
                                    }
                                    genres(sort: COUNT_DESC) {
                                        count
                                        minutesWatched
                                        meanScore
                                        genre
                                    }
                                    tags(sort: COUNT_DESC, limit: 30) {
                                        count
                                        minutesWatched
                                        meanScore
                                        tag {
                                            id
                                            name
                                        }
                                    }
                                    staff(sort: COUNT_DESC, limit: 30) {
                                        count
                                        minutesWatched
                                        meanScore
                                        staff {
                                            id
                                            name {
                                                full
                                            }
                                            image {
                                                large
                                            }
                                        }
                                    }
                                    voiceActors(sort: COUNT_DESC, limit: 30) {
                                        count
                                        minutesWatched
                                        meanScore
                                        voiceActor {
                                            id
                                            name {
                                                full
                                            }
                                            image {
                                                large
                                            }
                                        }
                                    }
                                    studios(sort: COUNT_DESC, limit: 30) {
                                        count
                                        minutesWatched
                                        meanScore
                                        studio {
                                            id
                                            name
                                        }
                                    }
                                }
                                manga {
                                    count
                                    chaptersRead
                                    volumesRead
                                    meanScore
                                    standardDeviation
                                    scores(sort: MEAN_SCORE) {
                                        count
                                        chaptersRead
                                        meanScore
                                        score
                                    }
                                    lengths {
                                        length
                                        count
                                        chaptersRead
                                        meanScore
                                    }
                                    formats {
                                        count
                                        chaptersRead
                                        meanScore
                                        format
                                    }
                                    statuses {
                                        count
                                        chaptersRead
                                        meanScore
                                        status
                                    }
                                    countries {
                                        count
                                        chaptersRead
                                        meanScore
                                        country
                                    }
                                    releaseYears {
                                        count
                                        chaptersRead
                                        meanScore
                                        releaseYear
                                    }
                                    startYears {
                                        count
                                        chaptersRead
                                        meanScore
                                        startYear
                                    }
                                    genres(sort: COUNT_DESC) {
                                        count
                                        chaptersRead
                                        meanScore
                                        genre
                                    }
                                    tags(sort: COUNT_DESC, limit: 30) {
                                        count
                                        chaptersRead
                                        meanScore
                                        tag {
                                            id
                                            name
                                        }
                                    }
                                    staff(sort: COUNT_DESC, limit: 30) {
                                        count
                                        chaptersRead
                                        meanScore
                                        staff {
                                            id
                                            name {
                                                full
                                            }
                                            image {
                                                large
                                            }
                                        }
                                    }
                                }
                            }
                            favourites {
                                anime(page: 1, perPage: 24) {
                                    nodes {
                                        id
                                        type
                                        title {
                                            romaji
                                            english
                                        }
                                        coverImage {
                                            large
                                        }
                                    }
                                }
                                manga(page: 1, perPage: 24) {
                                    nodes {
                                        id
                                        type
                                        title {
                                            romaji
                                            english
                                        }
                                        coverImage {
                                            large
                                        }
                                    }
                                }
                                characters(page: 1, perPage: 24) {
                                    nodes {
                                        id
                                        name {
                                            full
                                        }
                                        image {
                                            large
                                        }
                                    }
                                }
                                staff(page: 1, perPage: 24) {
                                    nodes {
                                        id
                                        name {
                                            full
                                        }
                                        image {
                                            large
                                        }
                                    }
                                }
                                studios(page: 1, perPage: 24) {
                                    nodes {
                                        id
                                        name
                                    }
                                }
                            }
                        }
                    }
                """.trimIndent()

                val jsonResponse = postGraphQl(token, profileQuery, JSONObject())
                val viewerJson = JSONObject(jsonResponse)
                    .getJSONObject("data")
                    .getJSONObject("Viewer")

                val id = viewerJson.getInt("id")
                val name = viewerJson.optNullableString("name") ?: "AniList Kullanıcısı"
                val avatar = viewerJson.optJSONObject("avatar")?.optNullableString("large")
                val banner = viewerJson.optNullableString("bannerImage")
                val about = viewerJson.optNullableString("about") ?: ""
                val donatorBadge = viewerJson.optNullableString("donatorBadge")
                val donatorTier = viewerJson.optInt("donatorTier", 0)

                // Parse stats
                val statisticsObj = viewerJson.optJSONObject("statistics")
                val aniAnimeStats = statisticsObj?.optJSONObject("anime")
                val aniMangaStats = statisticsObj?.optJSONObject("manga")

                val animeOverviewStats = parseDetailedOverviewStats(aniAnimeStats, isAnime = true)
                val mangaOverviewStats = parseDetailedOverviewStats(aniMangaStats, isAnime = false)

                val animeStats = if (aniAnimeStats != null) {
                    val statusesArray = aniAnimeStats.optJSONArray("statuses")
                    val statusesMap = mutableMapOf<String, Int>()
                    if (statusesArray != null) {
                        for (i in 0 until statusesArray.length()) {
                            val obj = statusesArray.getJSONObject(i)
                            statusesMap[obj.getString("status")] = obj.getInt("count")
                        }
                    }
                    AniListStats(
                        count = aniAnimeStats.optInt("count", 0),
                        episodesWatched = aniAnimeStats.optInt("episodesWatched", 0),
                        minutesWatched = aniAnimeStats.optInt("minutesWatched", 0),
                        meanScore = aniAnimeStats.optDouble("meanScore", 0.0),
                        watching = statusesMap["CURRENT"] ?: 0,
                        completed = statusesMap["COMPLETED"] ?: 0,
                        planned = statusesMap["PLANNING"] ?: 0,
                        paused = statusesMap["PAUSED"] ?: 0,
                        dropped = statusesMap["DROPPED"] ?: 0
                    )
                } else null

                val mangaStats = if (aniMangaStats != null) {
                    val statusesArray = aniMangaStats.optJSONArray("statuses")
                    val statusesMap = mutableMapOf<String, Int>()
                    if (statusesArray != null) {
                        for (i in 0 until statusesArray.length()) {
                            val obj = statusesArray.getJSONObject(i)
                            statusesMap[obj.getString("status")] = obj.getInt("count")
                        }
                    }
                    AniListStats(
                        count = aniMangaStats.optInt("count", 0),
                        episodesWatched = aniMangaStats.optInt("chaptersRead", 0),
                        minutesWatched = aniMangaStats.optInt("volumesRead", 0),
                        meanScore = aniMangaStats.optDouble("meanScore", 0.0),
                        watching = statusesMap["CURRENT"] ?: 0,
                        completed = statusesMap["COMPLETED"] ?: 0,
                        planned = statusesMap["PLANNING"] ?: 0,
                        paused = statusesMap["PAUSED"] ?: 0,
                        dropped = statusesMap["DROPPED"] ?: 0
                    )
                } else null

                // Parse favorites
                val favouritesObj = viewerJson.optJSONObject("favourites")
                val favAnimeList = mutableListOf<ProfileFavoriteItem>()
                val favMangaList = mutableListOf<ProfileFavoriteItem>()
                val favCharList = mutableListOf<ProfileFavoriteItem>()
                val favStaffList = mutableListOf<ProfileFavoriteItem>()
                val favStudioList = mutableListOf<ProfileFavoriteItem>()

                favouritesObj?.optJSONObject("anime")?.optJSONArray("nodes")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val node = arr.getJSONObject(i)
                        favAnimeList.add(
                            ProfileFavoriteItem(
                                id = node.getInt("id").toString(),
                                title = node.getJSONObject("title").optNullableString("romaji") ?: node.getJSONObject("title").optNullableString("english") ?: "İsimsiz",
                                imageUrl = node.getJSONObject("coverImage").optNullableString("large") ?: ""
                            )
                        )
                    }
                }

                favouritesObj?.optJSONObject("manga")?.optJSONArray("nodes")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val node = arr.getJSONObject(i)
                        favMangaList.add(
                            ProfileFavoriteItem(
                                id = node.getInt("id").toString(),
                                title = node.getJSONObject("title").optNullableString("romaji") ?: node.getJSONObject("title").optNullableString("english") ?: "İsimsiz",
                                imageUrl = node.getJSONObject("coverImage").optNullableString("large") ?: ""
                            )
                        )
                    }
                }

                favouritesObj?.optJSONObject("characters")?.optJSONArray("nodes")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val node = arr.getJSONObject(i)
                        favCharList.add(
                            ProfileFavoriteItem(
                                id = node.getInt("id").toString(),
                                title = node.getJSONObject("name").optNullableString("full") ?: "İsimsiz",
                                imageUrl = node.getJSONObject("image").optNullableString("large") ?: ""
                            )
                        )
                    }
                }

                favouritesObj?.optJSONObject("staff")?.optJSONArray("nodes")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val node = arr.getJSONObject(i)
                        favStaffList.add(
                            ProfileFavoriteItem(
                                id = node.getInt("id").toString(),
                                title = node.getJSONObject("name").optNullableString("full") ?: "İsimsiz",
                                imageUrl = node.getJSONObject("image").optNullableString("large") ?: ""
                            )
                        )
                    }
                }

                favouritesObj?.optJSONObject("studios")?.optJSONArray("nodes")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val node = arr.getJSONObject(i)
                        favStudioList.add(
                            ProfileFavoriteItem(
                                id = node.getInt("id").toString(),
                                title = node.optString("name", "İsimsiz"),
                                imageUrl = ""
                            )
                        )
                    }
                }

                _aniListState.update {
                    it.copy(
                        isLoading = false,
                        userId = id,
                        name = name,
                        avatarUrl = avatar,
                        bannerUrl = banner,
                        about = about,
                        donatorBadge = donatorBadge,
                        donatorTier = donatorTier,
                        animeStats = animeStats,
                        mangaStats = mangaStats,
                        animeOverviewStats = animeOverviewStats,
                        mangaOverviewStats = mangaOverviewStats,
                        favoriteAnime = favAnimeList,
                        favoriteManga = favMangaList,
                        favoriteCharacters = favCharList,
                        favoriteStaff = favStaffList,
                        favoriteStudios = favStudioList
                    )
                }

                // Fetch activities & social
                fetchAniListActivities(token, id, 1)
                fetchAniListSocial(token, id)

            } catch (e: Exception) {
                _aniListState.update { it.copy(isLoading = false, error = e.message ?: "AniList verisi yüklenirken hata oluştu.") }
            }
        }
    }

    private suspend fun fetchAniListActivities(token: String, userId: Int, page: Int) {
        withContext(Dispatchers.IO) {
            try {
                val activityQuery = """
                    query (${'$'}userId: Int, ${'$'}page: Int) {
                        Page(page: ${'$'}page, perPage: 25) {
                            pageInfo {
                                hasNextPage
                            }
                            activities(userId: ${'$'}userId, sort: ID_DESC) {
                                __typename
                                ... on TextActivity {
                                    id
                                    text
                                    createdAt
                                    likeCount
                                    replyCount
                                    isLiked
                                    user {
                                        name
                                        avatar {
                                            large
                                        }
                                    }
                                }
                                ... on ListActivity {
                                    id
                                    status
                                    progress
                                    createdAt
                                    likeCount
                                    replyCount
                                    isLiked
                                    user {
                                        name
                                        avatar {
                                            large
                                        }
                                    }
                                    media {
                                        id
                                        type
                                        title {
                                            romaji
                                            english
                                        }
                                        coverImage {
                                            large
                                        }
                                    }
                                }
                            }
                        }
                    }
                """.trimIndent()

                val variables = JSONObject()
                    .put("userId", userId)
                    .put("page", page)

                val jsonResponse = postGraphQl(token, activityQuery, variables)
                val pageObj = JSONObject(jsonResponse)
                    .getJSONObject("data")
                    .getJSONObject("Page")

                val hasNext = pageObj.getJSONObject("pageInfo").getBoolean("hasNextPage")
                val activityArray = pageObj.getJSONArray("activities")
                val list = mutableListOf<ProfileActivityItem>()

                for (i in 0 until activityArray.length()) {
                    val act = activityArray.getJSONObject(i)
                    val typename = act.optString("__typename")
                    val user = act.optJSONObject("user")
                    val userName = user?.optNullableString("name") ?: "Bilinmeyen"
                    val userAvatar = user?.optJSONObject("avatar")?.optNullableString("large")
                    val createdAt = act.optLong("createdAt", 0L)
                    val likeCount = act.optInt("likeCount", 0)
                    val replyCount = act.optInt("replyCount", 0)
                    val isLiked = act.optBoolean("isLiked", false)

                    if (typename == "TextActivity") {
                        list.add(
                            ProfileActivityItem.TextActivity(
                                id = act.getInt("id").toString(),
                                userName = userName,
                                userAvatar = userAvatar,
                                text = act.optNullableString("text") ?: "",
                                createdAt = createdAt,
                                likeCount = likeCount,
                                isLiked = isLiked,
                                replyCount = replyCount
                            )
                        )
                    } else if (typename == "ListActivity") {
                        val media = act.optJSONObject("media")
                        val mediaId = media?.optInt("id")
                        val mediaType = media?.optNullableString("type")
                        val mediaTitle = media?.optJSONObject("title")?.optNullableString("romaji") ?: media?.optJSONObject("title")?.optNullableString("english") ?: "Medya"
                        val mediaImage = media?.optJSONObject("coverImage")?.optNullableString("large")
                        val status = act.optNullableString("status") ?: "güncelledi"
                        val progress = act.optNullableString("progress")

                        list.add(
                            ProfileActivityItem.ListActivity(
                                id = act.getInt("id").toString(),
                                userName = userName,
                                userAvatar = userAvatar,
                                mediaTitle = mediaTitle,
                                mediaImage = mediaImage,
                                status = status,
                                progress = progress,
                                createdAt = createdAt,
                                likeCount = likeCount,
                                isLiked = isLiked,
                                replyCount = replyCount,
                                mediaId = mediaId,
                                mediaType = mediaType
                            )
                        )
                    }
                }

                _aniListState.update {
                    it.copy(
                        activities = if (page == 1) list else it.activities + list,
                        activitiesHasNext = hasNext,
                        activitiesPage = page
                    )
                }
            } catch (e: Exception) {
                // Fail silently for activity feed, but log
                android.util.Log.e("ProfileViewModel", "AniList activity fetch failed: ${e.message}")
            }
        }
    }

    fun loadNextAniListActivitiesPage() {
        val token = ExternalAuthManager.getAniListToken(context)
        val state = _aniListState.value
        if (token.isNullOrBlank() || state.userId == null || !state.activitiesHasNext) return

        viewModelScope.launch {
            fetchAniListActivities(token, state.userId, state.activitiesPage + 1)
        }
    }

    private suspend fun fetchAniListSocial(token: String, userId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val socialQuery = """
                    query (${'$'}userId: Int) {
                        User(id: ${'$'}userId) {
                            followers(page: 1, perPage: 50) {
                                nodes {
                                    id
                                    name
                                    avatar { large }
                                }
                            }
                            following(page: 1, perPage: 50) {
                                nodes {
                                    id
                                    name
                                    avatar { large }
                                }
                            }
                        }
                    }
                """.trimIndent()
                val variables = JSONObject().put("userId", userId)
                val jsonResponse = postGraphQl(token, socialQuery, variables)
                val userObj = JSONObject(jsonResponse).getJSONObject("data").getJSONObject("User")

                val followersList = mutableListOf<UserFollowItem>()
                userObj.optJSONObject("followers")?.optJSONArray("nodes")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        followersList.add(
                            UserFollowItem(
                                id = obj.getInt("id"),
                                name = obj.optNullableString("name") ?: "Kullanıcı",
                                avatarUrl = obj.optJSONObject("avatar")?.optNullableString("large")
                            )
                        )
                    }
                }

                val followingList = mutableListOf<UserFollowItem>()
                userObj.optJSONObject("following")?.optJSONArray("nodes")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        followingList.add(
                            UserFollowItem(
                                id = obj.getInt("id"),
                                name = obj.optNullableString("name") ?: "Kullanıcı",
                                avatarUrl = obj.optJSONObject("avatar")?.optNullableString("large")
                            )
                        )
                    }
                }

                _aniListState.update {
                    it.copy(socialState = SocialState(followers = followersList, following = followingList))
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "AniList social fetch failed: ${e.message}")
            }
        }
    }

    private fun parseDetailedOverviewStats(json: JSONObject?, isAnime: Boolean): DetailedUserOverviewStats {
        if (json == null) return DetailedUserOverviewStats()
        val count = json.optInt("count", 0)
        val epOrChap = if (isAnime) json.optInt("episodesWatched", 0) else json.optInt("chaptersRead", 0)
        val minutesOrVol = if (isAnime) json.optInt("minutesWatched", 0) else json.optInt("volumesRead", 0)
        val daysWatched = if (isAnime) (minutesOrVol / 60.0 / 24.0) else minutesOrVol.toDouble()
        val meanScore = json.optDouble("meanScore", 0.0)
        val stdDev = json.optDouble("standardDeviation", 0.0)

        val scoreList = mutableListOf<ScoreStatItem>()
        json.optJSONArray("scores")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                scoreList.add(
                    ScoreStatItem(
                        score = item.optInt("score", 0),
                        count = item.optInt("count", 0),
                        minutesWatched = item.optInt(if (isAnime) "minutesWatched" else "chaptersRead", 0),
                        meanScore = item.optDouble("meanScore", 0.0)
                    )
                )
            }
        }

        val lengthList = mutableListOf<LengthStatItem>()
        json.optJSONArray("lengths")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                lengthList.add(
                    LengthStatItem(
                        length = item.optString("length", "Bilinmiyor"),
                        count = item.optInt("count", 0),
                        minutesWatched = item.optInt(if (isAnime) "minutesWatched" else "chaptersRead", 0),
                        meanScore = item.optDouble("meanScore", 0.0)
                    )
                )
            }
        }

        val formatList = mutableListOf<FormatStatItem>()
        json.optJSONArray("formats")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                formatList.add(
                    FormatStatItem(
                        format = item.optString("format", "Diğer"),
                        count = item.optInt("count", 0),
                        minutesWatched = item.optInt(if (isAnime) "minutesWatched" else "chaptersRead", 0),
                        meanScore = item.optDouble("meanScore", 0.0)
                    )
                )
            }
        }

        val statusList = mutableListOf<StatusStatItem>()
        json.optJSONArray("statuses")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                statusList.add(
                    StatusStatItem(
                        status = item.optString("status", "Bilinmeyen"),
                        count = item.optInt("count", 0),
                        minutesWatched = item.optInt(if (isAnime) "minutesWatched" else "chaptersRead", 0),
                        meanScore = item.optDouble("meanScore", 0.0)
                    )
                )
            }
        }

        val countryList = mutableListOf<CountryStatItem>()
        json.optJSONArray("countries")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                countryList.add(
                    CountryStatItem(
                        country = item.optString("country", "Diğer"),
                        count = item.optInt("count", 0),
                        minutesWatched = item.optInt(if (isAnime) "minutesWatched" else "chaptersRead", 0),
                        meanScore = item.optDouble("meanScore", 0.0)
                    )
                )
            }
        }

        val releaseYearList = mutableListOf<ReleaseYearStatItem>()
        json.optJSONArray("releaseYears")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                releaseYearList.add(
                    ReleaseYearStatItem(
                        releaseYear = item.optInt("releaseYear", 0),
                        count = item.optInt("count", 0),
                        minutesWatched = item.optInt(if (isAnime) "minutesWatched" else "chaptersRead", 0),
                        meanScore = item.optDouble("meanScore", 0.0)
                    )
                )
            }
        }

        val startYearList = mutableListOf<StartYearStatItem>()
        json.optJSONArray("startYears")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                startYearList.add(
                    StartYearStatItem(
                        startYear = item.optInt("startYear", 0),
                        count = item.optInt("count", 0),
                        minutesWatched = item.optInt(if (isAnime) "minutesWatched" else "chaptersRead", 0),
                        meanScore = item.optDouble("meanScore", 0.0)
                    )
                )
            }
        }

        val genreList = mutableListOf<RankedStatItem>()
        json.optJSONArray("genres")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                genreList.add(
                    RankedStatItem(
                        name = item.optString("genre", "Bilinmiyor"),
                        count = item.optInt("count", 0),
                        meanScore = item.optDouble("meanScore", 0.0),
                        timeSpentMinutes = if (isAnime) item.optInt("minutesWatched", 0) else null,
                        chaptersRead = if (!isAnime) item.optInt("chaptersRead", 0) else null
                    )
                )
            }
        }

        val tagList = mutableListOf<RankedStatItem>()
        json.optJSONArray("tags")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val tagObj = item.optJSONObject("tag")
                tagList.add(
                    RankedStatItem(
                        name = tagObj?.optString("name") ?: item.optString("tag", "Bilinmiyor"),
                        count = item.optInt("count", 0),
                        meanScore = item.optDouble("meanScore", 0.0),
                        timeSpentMinutes = if (isAnime) item.optInt("minutesWatched", 0) else null,
                        chaptersRead = if (!isAnime) item.optInt("chaptersRead", 0) else null
                    )
                )
            }
        }

        val staffList = mutableListOf<RankedStatItem>()
        json.optJSONArray("staff")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val staffObj = item.optJSONObject("staff")
                staffList.add(
                    RankedStatItem(
                        id = staffObj?.optInt("id"),
                        name = staffObj?.optJSONObject("name")?.optString("full") ?: "Bilinmeyen Ekip",
                        count = item.optInt("count", 0),
                        meanScore = item.optDouble("meanScore", 0.0),
                        timeSpentMinutes = if (isAnime) item.optInt("minutesWatched", 0) else null,
                        chaptersRead = if (!isAnime) item.optInt("chaptersRead", 0) else null,
                        imageUrl = staffObj?.optJSONObject("image")?.optString("large")
                    )
                )
            }
        }

        val voiceActorList = mutableListOf<RankedStatItem>()
        json.optJSONArray("voiceActors")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val vaObj = item.optJSONObject("voiceActor")
                voiceActorList.add(
                    RankedStatItem(
                        id = vaObj?.optInt("id"),
                        name = vaObj?.optJSONObject("name")?.optString("full") ?: "Bilinmeyen Seslendirici",
                        count = item.optInt("count", 0),
                        meanScore = item.optDouble("meanScore", 0.0),
                        timeSpentMinutes = if (isAnime) item.optInt("minutesWatched", 0) else null,
                        chaptersRead = if (!isAnime) item.optInt("chaptersRead", 0) else null,
                        imageUrl = vaObj?.optJSONObject("image")?.optString("large")
                    )
                )
            }
        }

        val studioList = mutableListOf<RankedStatItem>()
        json.optJSONArray("studios")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val studioObj = item.optJSONObject("studio")
                studioList.add(
                    RankedStatItem(
                        id = studioObj?.optInt("id"),
                        name = studioObj?.optString("name") ?: item.optString("studio", "Bilinmeyen Stüdyo"),
                        count = item.optInt("count", 0),
                        meanScore = item.optDouble("meanScore", 0.0),
                        timeSpentMinutes = if (isAnime) item.optInt("minutesWatched", 0) else null,
                        chaptersRead = if (!isAnime) item.optInt("chaptersRead", 0) else null
                    )
                )
            }
        }

        var plannedDays = 0.0
        statusList.find { it.status == "PLANNING" || it.status == "Planlandı" }?.let {
            plannedDays = if (isAnime) (it.count * 12.0 * 24.0 / 60.0 / 24.0) else it.count.toDouble()
        }

        return DetailedUserOverviewStats(
            count = count,
            episodesWatched = epOrChap,
            daysWatched = daysWatched,
            plannedDaysOrCount = plannedDays,
            meanScore = meanScore,
            standardDeviation = stdDev,
            scoreList = scoreList,
            lengthList = lengthList,
            formatList = formatList,
            statusList = statusList,
            countryList = countryList,
            releaseYearList = releaseYearList,
            startYearList = startYearList,
            genreList = genreList,
            tagList = tagList,
            staffList = staffList,
            voiceActorList = voiceActorList,
            studioList = studioList
        )
    }

    fun computeOverviewStatsFromEntries(entries: List<MediaEntry>, isAnime: Boolean): DetailedUserOverviewStats {
        if (entries.isEmpty()) return DetailedUserOverviewStats()
        val count = entries.size
        val totalProgress = entries.sumOf { it.progress }
        val daysWatched = if (isAnime) (totalProgress * 24.0 / 60.0 / 24.0) else 0.0
        val scores = entries.mapNotNull { it.score }
        val meanScore = if (scores.isNotEmpty()) scores.average() else 0.0
        val variance = if (scores.size > 1) scores.sumOf { Math.pow(it - meanScore, 2.0) } / scores.size else 0.0
        val stdDev = Math.sqrt(variance)

        // Scores 1..10
        val scoreCounts = (1..10).map { s ->
            val matching = entries.filter { (it.score ?: 0) == s }
            ScoreStatItem(score = s, count = matching.size, meanScore = s.toDouble())
        }

        // Statuses
        val statusGroup = entries.groupBy { it.status }.map { (st, list) ->
            val label = when (st) {
                WatchStatus.Watching -> "CURRENT"
                WatchStatus.Completed -> "COMPLETED"
                WatchStatus.Planned -> "PLANNING"
                WatchStatus.Paused -> "PAUSED"
                WatchStatus.Dropped -> "DROPPED"
                else -> "PLANNING"
            }
            StatusStatItem(status = label, count = list.size)
        }

        // Formats
        val formatGroup = entries.groupBy { it.type }.map { (tp, list) ->
            val label = when (tp) {
                MediaType.Anime -> "TV"
                MediaType.Movie -> "MOVIE"
                MediaType.TvShow -> "TV"
                MediaType.Manga -> "MANGA"
            }
            FormatStatItem(format = label, count = list.size)
        }

        val plannedCount = entries.count { it.status == WatchStatus.Planned }.toDouble()

        val tagList = entries.flatMap { entry ->
            val tagString = entry.tags
            if (!tagString.isNullOrBlank()) {
                tagString.split(",").map { t -> t.trim() to entry }
            } else {
                emptyList()
            }
        }.filter { it.first.isNotBlank() }
            .groupBy { it.first }
            .map { (tag, pairs) ->
                val itemEntries = pairs.map { it.second }
                val sc = itemEntries.mapNotNull { it.score }
                val avg = if (sc.isNotEmpty()) sc.average() else 0.0
                val mins = itemEntries.sumOf { e -> e.progress * 24 }
                val chaps = itemEntries.sumOf { e -> e.progress }
                RankedStatItem(
                    name = tag,
                    count = itemEntries.size,
                    meanScore = avg,
                    timeSpentMinutes = if (isAnime) mins else null,
                    chaptersRead = if (!isAnime) chaps else null
                )
            }.sortedByDescending { it.count }

        val genreList = emptyList<RankedStatItem>()
        val studioList = emptyList<RankedStatItem>()

        return DetailedUserOverviewStats(
            count = count,
            episodesWatched = totalProgress,
            daysWatched = daysWatched,
            plannedDaysOrCount = plannedCount,
            meanScore = meanScore,
            standardDeviation = stdDev,
            scoreList = scoreCounts,
            statusList = statusGroup,
            formatList = formatGroup,
            genreList = genreList,
            tagList = tagList,
            studioList = studioList
        )
    }

    private suspend fun postGraphQl(token: String, query: String, variables: JSONObject): String {
        return withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("query", query)
                .put("variables", variables)
                .toString()

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $token")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorText = response.body?.string().orEmpty()
                    throw IllegalStateException("AniList hatası: ${response.code} $errorText")
                }
                response.body?.string() ?: ""
            }
        }
    }

    // --- MYANIMELIST API FETCHING (via Official API & Jikan Fallback) ---
    fun fetchMalProfile() {
        viewModelScope.launch {
            val token = ExternalAuthManager.getOrRefreshMalToken(context)
            if (token.isNullOrBlank()) {
                _malState.update { it.copy(isConnected = false, isLoading = false, error = "Bağlantı bulunamadı") }
                return@launch
            }

            _malState.update { it.copy(isConnected = true, isLoading = true, error = null) }

            try {
                // Fetch profile metadata using MAL OAuth API with picture, anime_statistics, and manga_statistics fields
                val responseJson = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://api.myanimelist.net/v2/users/@me?fields=id,name,picture,gender,location,joined_at,anime_statistics,manga_statistics")
                        .header("Authorization", "Bearer $token")
                        .header("Accept", "application/json")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IllegalStateException("MAL API error: ${response.code}")
                        }
                        response.body?.string() ?: ""
                    }
                }

                val userObj = JSONObject(responseJson)
                val username = userObj.optNullableString("name") ?: "MAL Kullanıcısı"
                val avatar = userObj.optNullableString("picture")
                val location = userObj.optNullableString("location") ?: ""
                val joinedAt = userObj.optNullableString("joined_at") ?: ""

                val animeStatsObj = userObj.optJSONObject("anime_statistics")
                val officialAnimeStats = if (animeStatsObj != null) {
                    val watching = animeStatsObj.optInt("num_items_watching", 0)
                    val completed = animeStatsObj.optInt("num_items_completed", 0)
                    val planned = animeStatsObj.optInt("num_items_plan_to_watch", 0)
                    val paused = animeStatsObj.optInt("num_items_on_hold", 0)
                    val dropped = animeStatsObj.optInt("num_items_dropped", 0)
                    val total = animeStatsObj.optInt("num_items", 0)
                    val meanScore = animeStatsObj.optDouble("mean_score", 0.0)
                    val episodes = animeStatsObj.optInt("num_episodes", 0)
                    val minutes = (animeStatsObj.optDouble("num_days", 0.0) * 24.0 * 60.0).toInt()

                    AniListStats(
                        count = total,
                        episodesWatched = episodes,
                        minutesWatched = minutes,
                        meanScore = meanScore,
                        watching = watching,
                        completed = completed,
                        planned = planned,
                        paused = paused,
                        dropped = dropped
                    )
                } else null

                val mangaStatsObj = userObj.optJSONObject("manga_statistics")
                val officialMangaStats = if (mangaStatsObj != null) {
                    val watching = mangaStatsObj.optInt("num_items_reading", 0)
                    val completed = mangaStatsObj.optInt("num_items_completed", 0)
                    val planned = mangaStatsObj.optInt("num_items_plan_to_read", 0)
                    val paused = mangaStatsObj.optInt("num_items_on_hold", 0)
                    val dropped = mangaStatsObj.optInt("num_items_dropped", 0)
                    val total = mangaStatsObj.optInt("num_items", 0)
                    val meanScore = mangaStatsObj.optDouble("mean_score", 0.0)
                    val chapters = mangaStatsObj.optInt("num_chapters", 0)
                    val volumes = mangaStatsObj.optInt("num_volumes", 0)

                    AniListStats(
                        count = total,
                        episodesWatched = chapters,
                        minutesWatched = volumes,
                        meanScore = meanScore,
                        watching = watching,
                        completed = completed,
                        planned = planned,
                        paused = paused,
                        dropped = dropped
                    )
                } else null

                _malState.update {
                    it.copy(
                        name = username,
                        avatarUrl = avatar,
                        location = location,
                        joinedAt = joinedAt,
                        animeStats = officialAnimeStats,
                        mangaStats = officialMangaStats
                    )
                }

                // Fetch Stats & Favorites via public Jikan API (safe call)
                try {
                    fetchMalJikanData(username)
                } catch (je: Exception) {
                    android.util.Log.e("ProfileViewModel", "Safe Jikan fetch failed: ${je.message}")
                    _malState.update { it.copy(isLoading = false) }
                }

            } catch (e: Exception) {
                _malState.update { it.copy(isLoading = false, error = e.message ?: "MAL verisi yüklenirken hata oluştu.") }
            }
        }
    }

    private suspend fun fetchMalJikanData(username: String) {
        withContext(Dispatchers.IO) {
            var animeStats: AniListStats? = null
            var mangaStats: AniListStats? = null
            val favAnime = mutableListOf<ProfileFavoriteItem>()
            val favManga = mutableListOf<ProfileFavoriteItem>()
            val favChar = mutableListOf<ProfileFavoriteItem>()
            val favStaff = mutableListOf<ProfileFavoriteItem>()

            // 1. Fetch Jikan User Stats (independent try-catch — failure won't block favorites)
            try {
                val statsRequest = Request.Builder()
                    .url("https://api.jikan.moe/v4/users/$username/statistics")
                    .build()

                client.newCall(statsRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val dataObj = JSONObject(response.body?.string().orEmpty()).optJSONObject("data")

                        dataObj?.optJSONObject("anime")?.let { animeObj ->
                            animeStats = AniListStats(
                                count = animeObj.optInt("total_entries", 0),
                                episodesWatched = animeObj.optInt("episodes_watched", 0),
                                minutesWatched = (animeObj.optDouble("days_watched", 0.0) * 24.0 * 60.0).toInt(),
                                meanScore = animeObj.optDouble("mean_score", 0.0),
                                watching = animeObj.optInt("watching", 0),
                                completed = animeObj.optInt("completed", 0),
                                planned = animeObj.optInt("plan_to_watch", 0),
                                paused = animeObj.optInt("on_hold", 0),
                                dropped = animeObj.optInt("dropped", 0)
                            )
                        }

                        dataObj?.optJSONObject("manga")?.let { mangaObj ->
                            mangaStats = AniListStats(
                                count = mangaObj.optInt("total_entries", 0),
                                episodesWatched = mangaObj.optInt("chapters_read", 0),
                                minutesWatched = mangaObj.optInt("volumes_read", 0),
                                meanScore = mangaObj.optDouble("mean_score", 0.0),
                                watching = mangaObj.optInt("reading", 0),
                                completed = mangaObj.optInt("completed", 0),
                                planned = mangaObj.optInt("plan_to_read", 0),
                                paused = mangaObj.optInt("on_hold", 0),
                                dropped = mangaObj.optInt("dropped", 0)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ProfileViewModel", "Jikan stats fetch failed (non-fatal): ${e.message}")
            }

            // Jikan rate limit: wait 1s between requests
            kotlinx.coroutines.delay(1000)

            // 2. Fetch Jikan Favorites (independent try-catch — runs even if stats failed)
            try {
                val favRequest = Request.Builder()
                    .url("https://api.jikan.moe/v4/users/$username/favorites")
                    .build()

                client.newCall(favRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val dataObj = JSONObject(response.body?.string().orEmpty()).optJSONObject("data")

                        // Helper to extract image URL from Jikan item
                        fun extractImageUrl(item: JSONObject): String =
                            item.optJSONObject("images")?.optJSONObject("webp")?.optString("image_url", "")?.takeIf { it.isNotBlank() }
                                ?: item.optJSONObject("images")?.optJSONObject("jpg")?.optString("image_url", "") ?: ""

                        dataObj?.optJSONArray("anime")?.let { arr ->
                            for (i in 0 until minOf(arr.length(), 12)) {
                                val item = arr.getJSONObject(i)
                                favAnime.add(ProfileFavoriteItem(
                                    id = item.optInt("mal_id").toString(),
                                    title = item.optString("title", "İsimsiz"),
                                    imageUrl = extractImageUrl(item)
                                ))
                            }
                        }

                        dataObj?.optJSONArray("manga")?.let { arr ->
                            for (i in 0 until minOf(arr.length(), 12)) {
                                val item = arr.getJSONObject(i)
                                favManga.add(ProfileFavoriteItem(
                                    id = item.optInt("mal_id").toString(),
                                    title = item.optString("title", "İsimsiz"),
                                    imageUrl = extractImageUrl(item)
                                ))
                            }
                        }

                        dataObj?.optJSONArray("characters")?.let { arr ->
                            for (i in 0 until minOf(arr.length(), 12)) {
                                val item = arr.getJSONObject(i)
                                favChar.add(ProfileFavoriteItem(
                                    id = item.optInt("mal_id").toString(),
                                    title = item.optString("name", "İsimsiz"),
                                    imageUrl = extractImageUrl(item)
                                ))
                            }
                        }

                        // "people" = voice actors / staff
                        dataObj?.optJSONArray("people")?.let { arr ->
                            for (i in 0 until minOf(arr.length(), 12)) {
                                val item = arr.getJSONObject(i)
                                favStaff.add(ProfileFavoriteItem(
                                    id = item.optInt("mal_id").toString(),
                                    title = item.optString("name", "İsimsiz"),
                                    imageUrl = extractImageUrl(item)
                                ))
                            }
                        }
                    } else {
                        android.util.Log.w("ProfileViewModel", "Jikan favorites HTTP ${response.code} for user: $username")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Jikan favorites fetch failed: ${e.message}")
            }

            val malFriends = mutableListOf<UserFollowItem>()
            try {
                kotlinx.coroutines.delay(1000)
                val friendsRequest = Request.Builder()
                    .url("https://api.jikan.moe/v4/users/$username/friends")
                    .build()

                client.newCall(friendsRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseText = response.body?.string().orEmpty()
                        val dataArr = JSONObject(responseText).optJSONArray("data")
                        dataArr?.let { arr ->
                            for (i in 0 until arr.length()) {
                                val friendObj = arr.getJSONObject(i)
                                val userObj = friendObj.optJSONObject("user")
                                if (userObj != null) {
                                    val friendName = userObj.optNullableString("username") ?: "Kullanıcı"
                                    val friendAvatar = userObj.optNullableString("image_url")
                                    malFriends.add(
                                        UserFollowItem(
                                            id = friendName.hashCode(),
                                            name = friendName,
                                            avatarUrl = friendAvatar
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        android.util.Log.w("ProfileViewModel", "Jikan friends HTTP ${response.code} for user: $username")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Jikan friends fetch failed: ${e.message}")
            }

            // Always update state — partial data is better than no data
            _malState.update {
                it.copy(
                    isLoading = false,
                    animeStats = animeStats ?: it.animeStats,
                    mangaStats = mangaStats ?: it.mangaStats,
                    favoriteAnime = favAnime,
                    favoriteManga = favManga,
                    favoriteCharacters = favChar,
                    favoriteStaff = favStaff,
                    socialState = SocialState(followers = malFriends, following = malFriends)
                )
            }
        }
    }

    // --- SIMKL API FETCHING ---
    fun fetchSimklProfile() {
        val token = ExternalAuthManager.getSimklToken(context)
        if (token.isNullOrBlank()) {
            _simklState.update { it.copy(isConnected = false, isLoading = false, error = "Bağlantı bulunamadı") }
            return
        }

        _simklState.update { it.copy(isConnected = true, isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val profile = com.kitsugi.animelist.data.auth.SimklImportManager.fetchUserProfile(token)
                val watchlist = com.kitsugi.animelist.data.auth.SimklImportManager.fetchAllLists(token)

                var totalAnime = 0
                var totalShows = 0
                var totalMovies = 0
                var watching = 0
                var completed = 0
                var planned = 0
                var paused = 0
                var dropped = 0
                val scores = mutableListOf<Int>()

                watchlist.forEach { entry ->
                    when (entry.type) {
                        com.kitsugi.animelist.model.MediaType.Anime -> totalAnime++
                        com.kitsugi.animelist.model.MediaType.TvShow -> totalShows++
                        com.kitsugi.animelist.model.MediaType.Movie -> totalMovies++
                        else -> {}
                    }

                    when (entry.status) {
                        com.kitsugi.animelist.model.WatchStatus.Watching -> watching++
                        com.kitsugi.animelist.model.WatchStatus.Completed -> completed++
                        com.kitsugi.animelist.model.WatchStatus.Planned -> planned++
                        com.kitsugi.animelist.model.WatchStatus.Paused -> paused++
                        com.kitsugi.animelist.model.WatchStatus.Dropped -> dropped++
                        else -> planned++
                    }

                    entry.score?.let { scores.add(it) }
                }

                val avgScore = if (scores.isNotEmpty()) scores.average() else 0.0

                // Fetch recent watch history from Simkl /sync/history
                val recentHistoryList = mutableListOf<ProfileFavoriteItem>()
                try {
                    val historyUrl = "https://api.simkl.com/sync/history?limit=20"
                    val historyRequest = Request.Builder()
                        .url(historyUrl)
                        .header("Authorization", "Bearer $token")
                        .header("simkl-api-key", com.kitsugi.animelist.BuildConfig.SIMKL_CLIENT_ID)
                        .build()

                    withContext(Dispatchers.IO) {
                        client.newCall(historyRequest).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string().orEmpty()
                                val root = JSONObject(body)
                                // History returns arrays: anime, shows, movies
                                listOf("anime", "shows", "movies").forEach { key ->
                                    root.optJSONArray(key)?.let { arr ->
                                        for (i in 0 until minOf(arr.length(), 20)) {
                                            val entry = arr.getJSONObject(i)
                                            // Item is nested under "show", "movie", or "anime" key
                                            val mediaObj = entry.optJSONObject("show")
                                                ?: entry.optJSONObject("movie")
                                                ?: entry.optJSONObject("anime")
                                                ?: continue
                                            val title = mediaObj.optNullableString("title") ?: continue
                                            val ids = mediaObj.optJSONObject("ids")
                                            val simklId = ids?.optInt("simkl", 0) ?: 0
                                            val posterSlug = mediaObj.optNullableString("poster")
                                            val imageUrl = if (!posterSlug.isNullOrBlank()) {
                                                "https://simkl.in/posters/${posterSlug}_m.jpg"
                                            } else ""
                                            if (simklId > 0) {
                                                recentHistoryList.add(
                                                    ProfileFavoriteItem(
                                                        id = simklId.toString(),
                                                        title = title,
                                                        imageUrl = imageUrl
                                                    )
                                                )
                                            }
                                            if (recentHistoryList.size >= 20) return@use
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (he: Exception) {
                    android.util.Log.w("ProfileViewModel", "Simkl history fetch failed: ${he.message}")
                }

                _simklState.update {
                    it.copy(
                        isLoading = false,
                        name = profile.name,
                        avatarUrl = profile.avatarUrl,
                        joinedAt = profile.joinedAt,
                        location = profile.location,
                        bio = profile.bio,
                        accountType = profile.accountType,
                        totalAnime = totalAnime,
                        totalShows = totalShows,
                        totalMovies = totalMovies,
                        watching = watching,
                        completed = completed,
                        planned = planned,
                        paused = paused,
                        dropped = dropped,
                        avgScore = avgScore,
                        recentHistory = recentHistoryList
                    )
                }
            } catch (e: Exception) {
                _simklState.update { it.copy(isLoading = false, error = e.message ?: "Simkl verisi yüklenirken hata oluştu.") }
            }
        }
    }
}

// --- DATA STRUCTURES ---

data class LocalStats(
    val totalAnime: Int = 0,
    val watchingAnime: Int = 0,
    val completedAnime: Int = 0,
    val plannedAnime: Int = 0,
    val pausedAnime: Int = 0,
    val droppedAnime: Int = 0,
    val avgAnimeScore: Double = 0.0,

    val totalManga: Int = 0,
    val readingManga: Int = 0,
    val completedManga: Int = 0,
    val plannedManga: Int = 0,
    val pausedManga: Int = 0,
    val droppedManga: Int = 0,
    val avgMangaScore: Double = 0.0
)

data class AniListStats(
    val count: Int,
    val episodesWatched: Int,
    val minutesWatched: Int,
    val meanScore: Double,
    val watching: Int,
    val completed: Int,
    val planned: Int,
    val paused: Int,
    val dropped: Int
)

data class ProfileFavoriteItem(
    val id: String,
    val title: String,
    val imageUrl: String
)

sealed class ProfileActivityItem {
    abstract val id: String
    abstract val userName: String
    abstract val userAvatar: String?
    abstract val createdAt: Long
    abstract val likeCount: Int
    abstract val isLiked: Boolean
    abstract val replyCount: Int

    data class TextActivity(
        override val id: String,
        override val userName: String,
        override val userAvatar: String?,
        val text: String,
        override val createdAt: Long,
        override val likeCount: Int,
        override val isLiked: Boolean,
        override val replyCount: Int = 0
    ) : ProfileActivityItem()

    data class ListActivity(
        override val id: String,
        override val userName: String,
        override val userAvatar: String?,
        val mediaTitle: String,
        val mediaImage: String?,
        val status: String,
        val progress: String?,
        override val createdAt: Long,
        override val likeCount: Int,
        override val isLiked: Boolean,
        override val replyCount: Int = 0,
        val mediaId: Int? = null,
        val mediaType: String? = null
    ) : ProfileActivityItem()
}


data class ScoreStatItem(
    val score: Int,
    val count: Int,
    val minutesWatched: Int = 0,
    val meanScore: Double = 0.0
)

data class LengthStatItem(
    val length: String,
    val count: Int,
    val minutesWatched: Int = 0,
    val meanScore: Double = 0.0
)

data class FormatStatItem(
    val format: String,
    val count: Int,
    val minutesWatched: Int = 0,
    val meanScore: Double = 0.0
)

data class StatusStatItem(
    val status: String,
    val count: Int,
    val minutesWatched: Int = 0,
    val meanScore: Double = 0.0
)

data class CountryStatItem(
    val country: String,
    val count: Int,
    val minutesWatched: Int = 0,
    val meanScore: Double = 0.0
)

data class ReleaseYearStatItem(
    val releaseYear: Int,
    val count: Int,
    val minutesWatched: Int = 0,
    val meanScore: Double = 0.0
)

data class StartYearStatItem(
    val startYear: Int,
    val count: Int,
    val minutesWatched: Int = 0,
    val meanScore: Double = 0.0
)

data class RankedStatItem(
    val id: Int? = null,
    val name: String,
    val count: Int,
    val meanScore: Double = 0.0,
    val timeSpentMinutes: Int? = null,
    val chaptersRead: Int? = null,
    val imageUrl: String? = null
)

data class DetailedUserOverviewStats(
    val count: Int = 0,
    val episodesWatched: Int = 0,
    val daysWatched: Double = 0.0,
    val plannedDaysOrCount: Double = 0.0,
    val meanScore: Double = 0.0,
    val standardDeviation: Double = 0.0,
    val scoreList: List<ScoreStatItem> = emptyList(),
    val lengthList: List<LengthStatItem> = emptyList(),
    val formatList: List<FormatStatItem> = emptyList(),
    val statusList: List<StatusStatItem> = emptyList(),
    val countryList: List<CountryStatItem> = emptyList(),
    val releaseYearList: List<ReleaseYearStatItem> = emptyList(),
    val startYearList: List<StartYearStatItem> = emptyList(),
    val genreList: List<RankedStatItem> = emptyList(),
    val tagList: List<RankedStatItem> = emptyList(),
    val staffList: List<RankedStatItem> = emptyList(),
    val voiceActorList: List<RankedStatItem> = emptyList(),
    val studioList: List<RankedStatItem> = emptyList()
)

data class UserFollowItem(
    val id: Int,
    val name: String,
    val avatarUrl: String?
)

data class SocialState(
    val followers: List<UserFollowItem> = emptyList(),
    val following: List<UserFollowItem> = emptyList()
)

data class AniListProfileState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val userId: Int? = null,
    val name: String = "",
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val about: String = "",
    val donatorBadge: String? = null,
    val donatorTier: Int = 0,
    val animeStats: AniListStats? = null,
    val mangaStats: AniListStats? = null,
    val animeOverviewStats: DetailedUserOverviewStats? = null,
    val mangaOverviewStats: DetailedUserOverviewStats? = null,
    val favoriteAnime: List<ProfileFavoriteItem> = emptyList(),
    val favoriteManga: List<ProfileFavoriteItem> = emptyList(),
    val favoriteCharacters: List<ProfileFavoriteItem> = emptyList(),
    val favoriteStaff: List<ProfileFavoriteItem> = emptyList(),
    val favoriteStudios: List<ProfileFavoriteItem> = emptyList(),
    val activities: List<ProfileActivityItem> = emptyList(),
    val activitiesHasNext: Boolean = false,
    val activitiesPage: Int = 1,
    val socialState: SocialState = SocialState()
)

data class MalProfileState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val avatarUrl: String? = null,
    val location: String = "",
    val joinedAt: String = "",
    val animeStats: AniListStats? = null,
    val mangaStats: AniListStats? = null,
    val animeOverviewStats: DetailedUserOverviewStats? = null,
    val mangaOverviewStats: DetailedUserOverviewStats? = null,
    val favoriteAnime: List<ProfileFavoriteItem> = emptyList(),
    val favoriteManga: List<ProfileFavoriteItem> = emptyList(),
    val favoriteCharacters: List<ProfileFavoriteItem> = emptyList(),
    val favoriteStaff: List<ProfileFavoriteItem> = emptyList(),
    val favoriteStudios: List<ProfileFavoriteItem> = emptyList(),
    val socialState: SocialState = SocialState()
)

data class SimklProfileState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val avatarUrl: String? = null,
    val joinedAt: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val accountType: String? = null,
    val totalAnime: Int = 0,
    val totalShows: Int = 0,
    val totalMovies: Int = 0,
    val watching: Int = 0,
    val completed: Int = 0,
    val planned: Int = 0,
    val paused: Int = 0,
    val dropped: Int = 0,
    val avgScore: Double = 0.0,
    val animeOverviewStats: DetailedUserOverviewStats? = null,
    val mangaOverviewStats: DetailedUserOverviewStats? = null,
    val recentHistory: List<ProfileFavoriteItem> = emptyList(),
    val socialState: SocialState = SocialState()
)
