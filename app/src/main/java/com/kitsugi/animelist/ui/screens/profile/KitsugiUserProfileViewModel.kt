package com.kitsugi.animelist.ui.screens.profile

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import com.kitsugi.animelist.data.remote.optNullableString
import com.kitsugi.animelist.ui.app.AniListStats
import com.kitsugi.animelist.ui.app.DetailedUserOverviewStats
import com.kitsugi.animelist.ui.app.ProfileActivityItem
import com.kitsugi.animelist.ui.app.ProfileFavoriteItem
import com.kitsugi.animelist.ui.app.SocialState
import com.kitsugi.animelist.ui.app.UserFollowItem
import com.kitsugi.animelist.ui.app.parseDetailedOverviewStats
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
import org.json.JSONObject

data class OtherUserProfileState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userId: Int = 0,
    val name: String = "",
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val about: String = "",
    val isFollowing: Boolean = false,
    val isFollower: Boolean = false,
    val isFollowLoading: Boolean = false,
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
    val favAnimePage: Int = 1,
    val favAnimeHasNext: Boolean = false,
    val favMangaPage: Int = 1,
    val favMangaHasNext: Boolean = false,
    val favCharPage: Int = 1,
    val favCharHasNext: Boolean = false,
    val favStaffPage: Int = 1,
    val favStaffHasNext: Boolean = false,
    val favStudioPage: Int = 1,
    val favStudioHasNext: Boolean = false,
    val activities: List<ProfileActivityItem> = emptyList(),
    val activitiesHasNext: Boolean = false,
    val activitiesPage: Int = 1,
    val socialState: SocialState = SocialState()
)

class KitsugiUserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val client = com.kitsugi.animelist.core.network.KitsugiHttpClient.client

    private val _uiState = MutableStateFlow(OtherUserProfileState())
    val uiState: StateFlow<OtherUserProfileState> = _uiState.asStateFlow()

    // Sub-tab & filter persistence for other user screen
    var activeTab by mutableIntStateOf(0)
    var statsMediaType by mutableIntStateOf(0)
    var statsSubTab by mutableIntStateOf(0)
    var favoritesFilter by mutableIntStateOf(0)
    var socialFilter by mutableIntStateOf(0)
    var scoreDistType by mutableIntStateOf(0)
    var lengthDistType by mutableIntStateOf(0)
    var releaseYearDistType by mutableIntStateOf(0)
    var startYearDistType by mutableIntStateOf(0)
    var statsSortType by mutableIntStateOf(0)

    private var targetUserId: Int? = null

    fun loadUser(userId: Int, fallbackName: String? = null, fallbackAvatar: String? = null) {
        if (targetUserId == userId && _uiState.value.userId == userId && !_uiState.value.isLoading) {
            return
        }
        targetUserId = userId
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                userId = userId,
                name = fallbackName ?: it.name,
                avatarUrl = fallbackAvatar ?: it.avatarUrl
            )
        }
        fetchUserProfile(userId)
    }

    fun fetchUserProfile(userId: Int) {
        val token = ExternalAuthManager.getAniListToken(context)

        viewModelScope.launch {
            try {
                val userQuery = """
                    query (${'$'}userId: Int) {
                        User(id: ${'$'}userId) {
                            id
                            name
                            about(asHtml: false)
                            avatar { large }
                            bannerImage
                            isFollowing
                            isFollower
                            donatorBadge
                            donatorTier
                            statistics {
                                anime {
                                    count
                                    episodesWatched
                                    minutesWatched
                                    meanScore
                                    standardDeviation
                                    scores(sort: MEAN_SCORE) { count minutesWatched meanScore score }
                                    lengths { length count minutesWatched meanScore }
                                    formats { count minutesWatched meanScore format }
                                    statuses { count minutesWatched meanScore status }
                                    countries { count minutesWatched meanScore country }
                                    releaseYears { count minutesWatched meanScore releaseYear }
                                    startYears { count minutesWatched meanScore startYear }
                                    genres(sort: COUNT_DESC) { count minutesWatched meanScore genre }
                                    tags(sort: COUNT_DESC, limit: 30) { count minutesWatched meanScore tag { id name } }
                                    staff(sort: COUNT_DESC, limit: 30) { count minutesWatched meanScore staff { id name { full } image { large } } }
                                    voiceActors(sort: COUNT_DESC, limit: 30) { count minutesWatched meanScore voiceActor { id name { full } image { large } } }
                                    studios(sort: COUNT_DESC, limit: 30) { count minutesWatched meanScore studio { id name } }
                                }
                                manga {
                                    count
                                    chaptersRead
                                    volumesRead
                                    meanScore
                                    standardDeviation
                                    scores(sort: MEAN_SCORE) { count chaptersRead meanScore score }
                                    lengths { length count chaptersRead meanScore }
                                    formats { count chaptersRead meanScore format }
                                    statuses { count chaptersRead meanScore status }
                                    countries { count chaptersRead meanScore country }
                                    releaseYears { count chaptersRead meanScore releaseYear }
                                    startYears { count chaptersRead meanScore startYear }
                                    genres(sort: COUNT_DESC) { count chaptersRead meanScore genre }
                                    tags(sort: COUNT_DESC, limit: 30) { count chaptersRead meanScore tag { id name } }
                                    staff(sort: COUNT_DESC, limit: 30) { count chaptersRead meanScore staff { id name { full } image { large } } }
                                }
                            }
                        }
                    }
                """.trimIndent()

                val variables = JSONObject().put("userId", userId)
                val jsonResponse = postGraphQl(token, userQuery, variables)
                val userJson = JSONObject(jsonResponse)
                    .getJSONObject("data")
                    .getJSONObject("User")

                val id = userJson.getInt("id")
                val name = userJson.optNullableString("name") ?: "Kullanıcı"
                val avatar = userJson.optJSONObject("avatar")?.optNullableString("large")
                val banner = userJson.optNullableString("bannerImage")
                val about = userJson.optNullableString("about") ?: ""
                val isFollowing = userJson.optBoolean("isFollowing", false)
                val isFollower = userJson.optBoolean("isFollower", false)
                val donatorBadge = userJson.optNullableString("donatorBadge")
                val donatorTier = userJson.optInt("donatorTier", 0)

                val statisticsObj = userJson.optJSONObject("statistics")
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

                val (favAnimeList, favAnimeHasNext) = fetchFavoritesPaginated(token, id, "anime", 1)
                val (favMangaList, favMangaHasNext) = fetchFavoritesPaginated(token, id, "manga", 1)
                val (favCharList, favCharHasNext) = fetchFavoritesPaginated(token, id, "characters", 1)
                val (favStaffList, favStaffHasNext) = fetchFavoritesPaginated(token, id, "staff", 1)
                val (favStudioList, favStudioHasNext) = fetchFavoritesPaginated(token, id, "studios", 1)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userId = id,
                        name = name,
                        avatarUrl = avatar,
                        bannerUrl = banner,
                        about = about,
                        isFollowing = isFollowing,
                        isFollower = isFollower,
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
                        favoriteStudios = favStudioList,
                        favAnimePage = 1, favAnimeHasNext = favAnimeHasNext,
                        favMangaPage = 1, favMangaHasNext = favMangaHasNext,
                        favCharPage = 1, favCharHasNext = favCharHasNext,
                        favStaffPage = 1, favStaffHasNext = favStaffHasNext,
                        favStudioPage = 1, favStudioHasNext = favStudioHasNext
                    )
                }

                fetchActivities(token, id, 1)
                fetchSocial(token, id)

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Kullanıcı profili yüklenirken hata oluştu.") }
            }
        }
    }

    fun toggleFollow() {
        val token = ExternalAuthManager.getAniListToken(context)
        if (token.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Takip etmek için AniList ile giriş yapmalısınız.") }
            return
        }
        val current = _uiState.value
        val targetId = current.userId
        if (targetId == 0 || current.isFollowLoading) return

        _uiState.update { it.copy(isFollowLoading = true) }

        viewModelScope.launch {
            try {
                val mutation = """
                    mutation (${'$'}userId: Int!) {
                        ToggleFollow(userId: ${'$'}userId) {
                            id
                            isFollowing
                        }
                    }
                """.trimIndent()

                val variables = JSONObject().put("userId", targetId)
                val response = postGraphQl(token, mutation, variables)
                val toggleObj = JSONObject(response)
                    .getJSONObject("data")
                    .getJSONObject("ToggleFollow")

                val newFollowingState = toggleObj.getBoolean("isFollowing")
                _uiState.update {
                    it.copy(
                        isFollowing = newFollowingState,
                        isFollowLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFollowLoading = false,
                        error = "Takip durumu değiştirilemedi: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun fetchActivities(token: String?, userId: Int, page: Int) {
        withContext(Dispatchers.IO) {
            try {
                val activityQuery = """
                    query (${'$'}userId: Int, ${'$'}page: Int) {
                        Page(page: ${'$'}page, perPage: 25) {
                            pageInfo { hasNextPage }
                            activities(userId: ${'$'}userId, sort: ID_DESC) {
                                __typename
                                ... on TextActivity {
                                    id text createdAt likeCount replyCount isLiked
                                    user { name avatar { large } }
                                }
                                ... on ListActivity {
                                    id status progress createdAt likeCount replyCount isLiked
                                    user { name avatar { large } }
                                    media { id type isAdult title { romaji english } coverImage { large } }
                                }
                            }
                        }
                    }
                """.trimIndent()

                val variables = JSONObject().put("userId", userId).put("page", page)
                val jsonResponse = postGraphQl(token, activityQuery, variables)
                val pageObj = JSONObject(jsonResponse).getJSONObject("data").getJSONObject("Page")
                val hasNext = pageObj.getJSONObject("pageInfo").getBoolean("hasNextPage")
                val activityArray = pageObj.getJSONArray("activities")
                val list = mutableListOf<ProfileActivityItem>()

                for (i in 0 until activityArray.length()) {
                    val act = activityArray.getJSONObject(i)
                    val typename = act.optString("__typename")
                    val user = act.optJSONObject("user")
                    val userName = user?.optNullableString("name") ?: "Kullanıcı"
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
                        val isAdult = media?.optBoolean("isAdult", false) ?: false
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
                                mediaType = mediaType,
                                isAdult = isAdult
                            )
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        activities = if (page == 1) list else it.activities + list,
                        activitiesHasNext = hasNext,
                        activitiesPage = page
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("UserProfileViewModel", "Activities fetch failed: ${e.message}")
            }
        }
    }

    fun loadNextActivitiesPage() {
        val token = ExternalAuthManager.getAniListToken(context)
        val state = _uiState.value
        if (state.userId == 0 || !state.activitiesHasNext) return
        viewModelScope.launch {
            fetchActivities(token, state.userId, state.activitiesPage + 1)
        }
    }

    private suspend fun fetchSocial(token: String?, userId: Int) {
        withContext(Dispatchers.IO) {
            val followersQuery = """
                query (${'$'}userId: Int!) {
                    Page(page: 1, perPage: 100) {
                        followers(userId: ${'$'}userId) { id name avatar { large } }
                    }
                }
            """.trimIndent()

            val followingQuery = """
                query (${'$'}userId: Int!) {
                    Page(page: 1, perPage: 100) {
                        following(userId: ${'$'}userId) { id name avatar { large } }
                    }
                }
            """.trimIndent()

            val variables = JSONObject().put("userId", userId)

            val followersList = mutableListOf<UserFollowItem>()
            try {
                val fResponse = postGraphQl(token, followersQuery, variables)
                val fPage = JSONObject(fResponse).getJSONObject("data").getJSONObject("Page")
                fPage.optJSONArray("followers")?.let { arr ->
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
            } catch (e: Exception) {
                android.util.Log.e("UserProfileViewModel", "followers fetch failed: ${e.message}")
            }

            val followingList = mutableListOf<UserFollowItem>()
            try {
                val fgResponse = postGraphQl(token, followingQuery, variables)
                val fgPage = JSONObject(fgResponse).getJSONObject("data").getJSONObject("Page")
                fgPage.optJSONArray("following")?.let { arr ->
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
            } catch (e: Exception) {
                android.util.Log.e("UserProfileViewModel", "following fetch failed: ${e.message}")
            }

            _uiState.update {
                it.copy(socialState = SocialState(followers = followersList, following = followingList))
            }
        }
    }

    private suspend fun fetchFavoritesPaginated(token: String?, userId: Int, category: String, page: Int = 1): Pair<List<ProfileFavoriteItem>, Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val fieldBlock = when (category) {
                    "anime" -> """
                        anime(page: ${'$'}page, perPage: 25) {
                            nodes { id title { romaji english } coverImage { large } isAdult }
                            pageInfo { hasNextPage }
                        }"""
                    "manga" -> """
                        manga(page: ${'$'}page, perPage: 25) {
                            nodes { id title { romaji english } coverImage { large } isAdult }
                            pageInfo { hasNextPage }
                        }"""
                    "characters" -> """
                        characters(page: ${'$'}page, perPage: 25) {
                            nodes { id name { full } image { large } }
                            pageInfo { hasNextPage }
                        }"""
                    "staff" -> """
                        staff(page: ${'$'}page, perPage: 25) {
                            nodes { id name { full } image { large } }
                            pageInfo { hasNextPage }
                        }"""
                    "studios" -> """
                        studios(page: ${'$'}page, perPage: 25) {
                            nodes { id name }
                            pageInfo { hasNextPage }
                        }"""
                    else -> return@withContext Pair(emptyList(), false)
                }

                val query = """
                    query (${'$'}userId: Int, ${'$'}page: Int) {
                        User(id: ${'$'}userId) {
                            favourites { $fieldBlock }
                        }
                    }
                """.trimIndent()

                val variables = JSONObject().put("userId", userId).put("page", page)
                val response = postGraphQl(token, query, variables)
                val favObj = JSONObject(response).getJSONObject("data").getJSONObject("User").getJSONObject("favourites")
                val catObj = favObj.getJSONObject(category)
                val hasNext = catObj.optJSONObject("pageInfo")?.optBoolean("hasNextPage", false) ?: false
                val nodes = catObj.optJSONArray("nodes")
                val list = mutableListOf<ProfileFavoriteItem>()
                if (nodes != null) {
                    for (i in 0 until nodes.length()) {
                        val node = nodes.getJSONObject(i)
                        val item = when (category) {
                            "anime", "manga" -> ProfileFavoriteItem(
                                id = node.getInt("id").toString(),
                                title = node.optJSONObject("title")?.let {
                                    it.optNullableString("romaji") ?: it.optNullableString("english")
                                } ?: "İsimsiz",
                                imageUrl = node.optJSONObject("coverImage")?.optNullableString("large") ?: "",
                                isAdult = node.optBoolean("isAdult", false)
                            )
                            "characters", "staff" -> ProfileFavoriteItem(
                                id = node.getInt("id").toString(),
                                title = node.optJSONObject("name")?.optNullableString("full") ?: "İsimsiz",
                                imageUrl = node.optJSONObject("image")?.optNullableString("large") ?: ""
                            )
                            "studios" -> ProfileFavoriteItem(
                                id = node.getInt("id").toString(),
                                title = node.optString("name", "İsimsiz"),
                                imageUrl = ""
                            )
                            else -> null
                        }
                        if (item != null) list.add(item)
                    }
                }
                Pair(list, hasNext)
            } catch (e: Exception) {
                android.util.Log.e("UserProfileViewModel", "Favorites ($category) fetch failed: ${e.message}")
                Pair(emptyList(), false)
            }
        }
    }

    fun loadMoreFavorites(category: String) {
        val token = ExternalAuthManager.getAniListToken(context)
        val state = _uiState.value
        val userId = state.userId
        if (userId == 0) return

        viewModelScope.launch {
            val currentPage = when (category) {
                "anime" -> state.favAnimePage
                "manga" -> state.favMangaPage
                "characters" -> state.favCharPage
                "staff" -> state.favStaffPage
                "studios" -> state.favStudioPage
                else -> 1
            }
            val hasNext = when (category) {
                "anime" -> state.favAnimeHasNext
                "manga" -> state.favMangaHasNext
                "characters" -> state.favCharHasNext
                "staff" -> state.favStaffHasNext
                "studios" -> state.favStudioHasNext
                else -> false
            }
            if (!hasNext) return@launch

            val (newItems, nextHasNext) = fetchFavoritesPaginated(token, userId, category, currentPage + 1)
            _uiState.update {
                when (category) {
                    "anime" -> it.copy(favoriteAnime = it.favoriteAnime + newItems, favAnimePage = currentPage + 1, favAnimeHasNext = nextHasNext)
                    "manga" -> it.copy(favoriteManga = it.favoriteManga + newItems, favMangaPage = currentPage + 1, favMangaHasNext = nextHasNext)
                    "characters" -> it.copy(favoriteCharacters = it.favoriteCharacters + newItems, favCharPage = currentPage + 1, favCharHasNext = nextHasNext)
                    "staff" -> it.copy(favoriteStaff = it.favoriteStaff + newItems, favStaffPage = currentPage + 1, favStaffHasNext = nextHasNext)
                    "studios" -> it.copy(favoriteStudios = it.favoriteStudios + newItems, favStudioPage = currentPage + 1, favStudioHasNext = nextHasNext)
                    else -> it
                }
            }
        }
    }

    private suspend fun postGraphQl(token: String?, query: String, variables: JSONObject): String {
        return withContext(Dispatchers.IO) {
            val jsonBody = JSONObject().apply {
                put("query", query)
                put("variables", variables)
            }
            val requestBuilder = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))

            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            response.body?.string() ?: throw Exception("Boş yanıt alındı")
        }
    }
}
