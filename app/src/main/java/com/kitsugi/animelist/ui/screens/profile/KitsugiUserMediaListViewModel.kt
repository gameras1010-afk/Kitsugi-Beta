package com.kitsugi.animelist.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.remote.KitsugiApiBase
import com.kitsugi.animelist.data.remote.cleanApiText
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class UserMediaListItem(
    val mediaId: Int,
    val malId: Int?,
    val title: String,
    val imageUrl: String?,
    val mediaType: MediaType,
    val status: WatchStatus,
    val score: Double?,
    val progress: Int,
    val total: Int?,
    val isAdult: Boolean,
    val format: String?,
    val year: Int?
)

data class UserMediaListUiState(
    val isLoading: Boolean = true,
    val items: List<UserMediaListItem> = emptyList(),
    val error: String? = null
)

class KitsugiUserMediaListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UserMediaListUiState())
    val uiState: StateFlow<UserMediaListUiState> = _uiState.asStateFlow()

    private var loadedUserId: Int? = null
    private var loadedMediaType: MediaType? = null

    fun loadUserMediaList(userId: Int, mediaType: MediaType, forceRefresh: Boolean = false) {
        if (!forceRefresh && loadedUserId == userId && loadedMediaType == mediaType && _uiState.value.items.isNotEmpty() && !_uiState.value.isLoading) {
            return
        }
        loadedUserId = userId
        loadedMediaType = mediaType
        viewModelScope.launch {
            _uiState.value = UserMediaListUiState(isLoading = true, items = emptyList(), error = null)
            val fetched = fetchUserMediaListFromAniList(userId, mediaType)
            if (fetched != null) {
                _uiState.value = UserMediaListUiState(isLoading = false, items = fetched, error = null)
            } else {
                _uiState.value = UserMediaListUiState(isLoading = false, items = emptyList(), error = "Liste yüklenemedi")
            }
        }
    }

    fun resetState() {
        loadedUserId = null
        loadedMediaType = null
        _uiState.value = UserMediaListUiState()
    }

    private suspend fun fetchUserMediaListFromAniList(userId: Int, mediaType: MediaType): List<UserMediaListItem>? {
        return withContext(Dispatchers.IO) {
            val typeStr = if (mediaType == MediaType.Anime) "ANIME" else "MANGA"
            val query = """
                query (${'$'}userId: Int, ${'$'}type: MediaType) {
                    MediaListCollection(userId: ${'$'}userId, type: ${'$'}type) {
                        lists {
                            name
                            status
                            entries {
                                status
                                score(format: POINT_10_DECIMAL)
                                progress
                                media {
                                    id
                                    idMal
                                    title { romaji english native userPreferred }
                                    coverImage { extraLarge large medium }
                                    episodes
                                    chapters
                                    format
                                    isAdult
                                    startDate { year }
                                }
                            }
                        }
                    }
                }
            """.trimIndent()
            val variables = JSONObject().put("userId", userId).put("type", typeStr)
            runCatching {
                val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching null
                val root = JSONObject(response)
                val listsArr = root.optJSONObject("data")?.optJSONObject("MediaListCollection")?.optJSONArray("lists") ?: return@runCatching null
                val resultList = mutableListOf<UserMediaListItem>()
                for (i in 0 until listsArr.length()) {
                    val listObj = listsArr.optJSONObject(i) ?: continue
                    val entriesArr = listObj.optJSONArray("entries") ?: continue
                    for (j in 0 until entriesArr.length()) {
                        val entryObj = entriesArr.optJSONObject(j) ?: continue
                        val mediaObj = entryObj.optJSONObject("media") ?: continue
                        val mediaId = mediaObj.optInt("id")
                        val idMal = if (mediaObj.has("idMal") && !mediaObj.isNull("idMal")) mediaObj.optInt("idMal") else null
                        val titleObj = mediaObj.optJSONObject("title")
                        val title = titleObj?.optString("userPreferred")?.takeIf { it.isNotBlank() }
                            ?: titleObj?.optString("romaji")?.takeIf { it.isNotBlank() }
                            ?: titleObj?.optString("english")?.takeIf { it.isNotBlank() }
                            ?: titleObj?.optString("native") ?: "Medya #$mediaId"
                        val coverObj = mediaObj.optJSONObject("coverImage")
                        val imageUrl = coverObj?.optString("extraLarge")?.takeIf { it.isNotBlank() }
                            ?: coverObj?.optString("large")?.takeIf { it.isNotBlank() }
                            ?: coverObj?.optString("medium")
                        val statusStr = entryObj.optString("status").uppercase()
                        val watchStatus = when (statusStr) {
                            "CURRENT" -> WatchStatus.Watching
                            "COMPLETED" -> WatchStatus.Completed
                            "PLANNING" -> WatchStatus.Planned
                            "PAUSED" -> WatchStatus.Paused
                            "DROPPED" -> WatchStatus.Dropped
                            else -> WatchStatus.Watching
                        }
                        val scoreVal = if (entryObj.has("score") && !entryObj.isNull("score")) entryObj.optDouble("score") else null
                        val progressVal = entryObj.optInt("progress", 0)
                        val totalVal = if (mediaType == MediaType.Anime) {
                            if (mediaObj.has("episodes") && !mediaObj.isNull("episodes")) mediaObj.optInt("episodes") else null
                        } else {
                            if (mediaObj.has("chapters") && !mediaObj.isNull("chapters")) mediaObj.optInt("chapters") else null
                        }
                        val isAdult = mediaObj.optBoolean("isAdult", false)
                        val format = mediaObj.optString("format", "")
                        val year = mediaObj.optJSONObject("startDate")?.let {
                            if (it.has("year") && !it.isNull("year")) it.optInt("year") else null
                        }

                        resultList.add(
                            UserMediaListItem(
                                mediaId = mediaId,
                                malId = idMal,
                                title = title.cleanApiText(),
                                imageUrl = imageUrl,
                                mediaType = mediaType,
                                status = watchStatus,
                                score = if (scoreVal != null && scoreVal > 0) scoreVal else null,
                                progress = progressVal,
                                total = if (totalVal != null && totalVal > 0) totalVal else null,
                                isAdult = isAdult,
                                format = format,
                                year = year
                            )
                        )
                    }
                }
                resultList.distinctBy { it.mediaId }
            }.getOrNull()
        }
    }
}
