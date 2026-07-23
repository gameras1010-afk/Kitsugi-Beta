package com.kitsugi.animelist.ui.screens.notifications

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.R
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import com.kitsugi.animelist.data.remote.KitsugiAiringCalendarClient
import com.kitsugi.animelist.data.remote.KitsugiAniListNotificationClient
import com.kitsugi.animelist.data.remote.SimklApiClient
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.WatchStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Veri Modeli ─────────────────────────────────────────────────────────────

data class NotifItem(
    val id: String,
    val imageUrl: String?,
    val title: String,
    val body: String,
    val dateText: String?,
    val isUnread: Boolean = false,
    val mediaId: Int? = null,
    val activityId: Int? = null,
    val mediaType: String? = null
)

// ─── UI State ─────────────────────────────────────────────────────────────────

data class NotifUiState(
    val items: List<NotifItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val page: Int = 1
)

// ─── ViewModel ───────────────────────────────────────────────────────────────

class KitsugiNotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx: Context get() = getApplication<Application>().applicationContext

    // ── AniList ──
    private val _aniList = MutableStateFlow(NotifUiState())
    val aniList: StateFlow<NotifUiState> = _aniList.asStateFlow()

    private var currentAniListGroup = KitsugiAniListNotificationClient.NotificationGroup.ALL

    // ── MAL ──
    private val _mal = MutableStateFlow(NotifUiState())
    val mal: StateFlow<NotifUiState> = _mal.asStateFlow()

    // ── TMDB+Simkl ──
    private val _tmdbSimkl = MutableStateFlow(NotifUiState())
    val tmdbSimkl: StateFlow<NotifUiState> = _tmdbSimkl.asStateFlow()

    // ─── AniList Yükleyici ────────────────────────────────────────────────────

    fun loadAniList(
        group: KitsugiAniListNotificationClient.NotificationGroup,
        resetPage: Boolean = false,
        mediaEntries: List<MediaEntry> = emptyList()
    ) {
        // Filtre değiştiyse her zaman sıfırla
        val shouldReset = resetPage || group != currentAniListGroup
        currentAniListGroup = group

        val current = _aniList.value
        if (!shouldReset && !current.hasMore) return
        if (current.isLoading) return

        val page = if (shouldReset) 1 else current.page

        viewModelScope.launch {
            _aniList.value = if (shouldReset) {
                NotifUiState(isLoading = true)
            } else {
                current.copy(isLoading = true, error = null)
            }

            try {
                val token = ExternalAuthManager.getAniListToken(ctx)
                    ?: run {
                        _aniList.value = _aniList.value.copy(
                            isLoading = false,
                            error = ctx.getString(R.string.notif_login_required_anilist)
                        )
                        return@launch
                    }

                val client = KitsugiAniListNotificationClient()
                val result = client.fetchNotifications(
                    accessToken = token,
                    page = page,
                    perPage = 25,
                    group = group,
                    resetCount = page == 1
                )

                val mapped = result.notifications.map { n ->
                    val body = when (n.type) {
                        "AIRING" -> buildString {
                            n.airingContexts?.getOrNull(0)?.let { append(it) }
                            append(" ")
                            n.airingContexts?.getOrNull(1)?.let { append(it) }
                            append(" ${n.episode}")
                            n.airingContexts?.getOrNull(2)?.let { append(it) }
                        }.trim().ifBlank { ctx.getString(R.string.notif_body_airing, n.episode) }
                        "FOLLOWING"                 -> ctx.getString(R.string.notif_body_following, n.userName ?: "")
                        "ACTIVITY_MESSAGE"          -> ctx.getString(R.string.notif_body_activity_message, n.userName ?: "")
                        "ACTIVITY_REPLY"            -> ctx.getString(R.string.notif_body_activity_reply, n.userName ?: "", n.context ?: ctx.getString(R.string.notif_body_activity_reply_fallback))
                        "ACTIVITY_REPLY_SUBSCRIBED" -> ctx.getString(R.string.notif_body_activity_reply_subscribed, n.userName ?: "", n.context ?: ctx.getString(R.string.notif_body_activity_reply_subscribed_fallback))
                        "ACTIVITY_MENTION"          -> ctx.getString(R.string.notif_body_activity_mention, n.userName ?: "", n.context ?: ctx.getString(R.string.notif_body_activity_mention_fallback))
                        "ACTIVITY_LIKE"             -> ctx.getString(R.string.notif_body_activity_like, n.userName ?: "", n.context ?: ctx.getString(R.string.notif_body_activity_like_fallback))
                        "ACTIVITY_REPLY_LIKE"       -> ctx.getString(R.string.notif_body_activity_reply_like, n.userName ?: "", n.context ?: ctx.getString(R.string.notif_body_activity_reply_like_fallback))
                        "THREAD_COMMENT_MENTION"    -> ctx.getString(R.string.notif_body_thread_comment_mention, n.userName ?: "")
                        "THREAD_COMMENT_REPLY"      -> ctx.getString(R.string.notif_body_thread_comment_reply, n.userName ?: "")
                        "THREAD_COMMENT_SUBSCRIBED" -> ctx.getString(R.string.notif_body_thread_comment_subscribed, n.userName ?: "")
                        "THREAD_COMMENT_LIKE"       -> ctx.getString(R.string.notif_body_thread_comment_like, n.userName ?: "")
                        "THREAD_LIKE"               -> ctx.getString(R.string.notif_body_thread_like, n.userName ?: "", n.threadTitle ?: ctx.getString(R.string.notif_body_thread_like_fallback))
                        "RELATED_MEDIA_ADDITION"    -> ctx.getString(R.string.notif_body_related_media_addition, n.mediaTitle ?: "", n.context ?: ctx.getString(R.string.notif_body_related_media_addition_fallback))
                        "MEDIA_DATA_CHANGE"         -> ctx.getString(R.string.notif_body_media_data_change, n.mediaTitle ?: "", n.context ?: ctx.getString(R.string.notif_body_media_data_change_fallback))
                        "MEDIA_MERGE"               -> ctx.getString(R.string.notif_body_media_merge, n.mediaTitle ?: "", n.context ?: ctx.getString(R.string.notif_body_media_merge_fallback))
                        "MEDIA_DELETION"            -> ctx.getString(R.string.notif_body_media_deletion, n.deletedMediaTitle ?: ctx.getString(R.string.notif_body_media_deletion_fallback))
                        else                        -> n.context ?: ctx.getString(R.string.notif_body_new)
                    }
                    NotifItem(
                        id = "al_${n.id}",
                        imageUrl = n.mediaCoverUrl ?: n.userAvatarUrl,
                        title = n.mediaTitle ?: n.userName ?: n.threadTitle ?: "AniList",
                        body = body,
                        dateText = n.dateText,
                        mediaId = n.mediaId,
                        activityId = n.activityId,
                        mediaType = n.mediaType?.lowercase()
                    )
                }

                val existingItems = if (shouldReset) emptyList() else _aniList.value.items
                _aniList.value = NotifUiState(
                    items = existingItems + mapped,
                    isLoading = false,
                    error = null,
                    hasMore = result.hasNextPage,
                    page = if (result.hasNextPage) page + 1 else page
                )
            } catch (e: Exception) {
                _aniList.value = _aniList.value.copy(
                    isLoading = false,
                    error = ctx.getString(R.string.notif_error_load, e.message ?: "")
                )
            }
        }
    }

    // ─── MAL Yükleyici ───────────────────────────────────────────────────────

    fun loadMal(mediaEntries: List<MediaEntry>) {
        if (_mal.value.isLoading) return
        viewModelScope.launch {
            _mal.value = NotifUiState(isLoading = true)
            try {
                val calendarClient = KitsugiAiringCalendarClient()
                val schedule = calendarClient.fetchWeeklySchedule()
                val allEntries = schedule.values.flatten()
                val now = System.currentTimeMillis()
                val malEntries = mediaEntries.filter {
                    (it.source.equals("jikan", ignoreCase = true) || it.source.equals("mal", ignoreCase = true)) &&
                    (it.status == WatchStatus.Watching || it.status == WatchStatus.Repeating)
                }
                val matched = allEntries
                    .filter { entry -> malEntries.any { me -> me.malId == entry.malId } }
                    .filter { entry ->
                        val triggerMs = entry.airingAt * 1000L
                        triggerMs <= now && triggerMs > now - 7 * 24 * 60 * 60 * 1000L
                    }
                val items = matched.map { entry ->
                    val dateText = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
                        .format(Date(entry.airingAt * 1000L))
                    NotifItem(
                        id = "mal_${entry.malId}_${entry.episode}",
                        imageUrl = null,
                        title = entry.title,
                        body = ctx.getString(R.string.notif_mal_episode_released, entry.episode),
                        dateText = dateText,
                        mediaId = entry.malId,
                        mediaType = "anime"
                    )
                }.sortedByDescending { it.id }

                _mal.value = NotifUiState(items = items, isLoading = false, hasMore = false)
            } catch (e: Exception) {
                android.util.Log.e("KitsugiNotif", "MAL load failed: ${e.message}")
                _mal.value = NotifUiState(
                    isLoading = false,
                    error = ctx.getString(R.string.notif_error_load, e.message ?: "")
                )
            }
        }
    }

    // ─── TMDB+Simkl Yükleyici ────────────────────────────────────────────────

    fun loadTmdbSimkl(mediaEntries: List<MediaEntry>) {
        if (_tmdbSimkl.value.isLoading) return
        viewModelScope.launch {
            _tmdbSimkl.value = NotifUiState(isLoading = true)
            try {
                val simklClient = SimklApiClient()
                val tvCal    = simklClient.getCalendar("tv")
                val animeCal = simklClient.getCalendar("anime")
                val movieCal = simklClient.getCalendar("movies")
                val allCal   = (tvCal + animeCal + movieCal).distinctBy { it.malId }

                val relevant = mediaEntries.filter { me ->
                    (me.source.equals("tmdb", ignoreCase = true) || me.source.equals("simkl", ignoreCase = true)) &&
                    (me.status == WatchStatus.Watching || me.status == WatchStatus.Repeating)
                }
                val matched = allCal.filter { calItem ->
                    relevant.any { me ->
                        (me.simklId != null && me.simklId == calItem.malId) ||
                        (me.tmdbId != null && calItem.tmdbId != null && me.tmdbId == calItem.tmdbId)
                    }
                }
                val items = matched.map { item ->
                    val mType = when (item.type) {
                        com.kitsugi.animelist.model.MediaType.Movie -> "movie"
                        com.kitsugi.animelist.model.MediaType.TvShow -> "tv"
                        else -> "anime"
                    }
                    NotifItem(
                        id = "simkl_${item.malId}",
                        imageUrl = item.imageUrl,
                        title = item.title,
                        body = ctx.getString(R.string.notif_simkl_new_content),
                        dateText = null,
                        mediaId = item.malId,
                        mediaType = mType
                    )
                }
                _tmdbSimkl.value = NotifUiState(items = items, isLoading = false, hasMore = false)
            } catch (e: Exception) {
                android.util.Log.e("KitsugiNotif", "TMDB/Simkl load failed: ${e.message}")
                _tmdbSimkl.value = NotifUiState(
                    isLoading = false,
                    error = ctx.getString(R.string.notif_error_load, e.message ?: "")
                )
            }
        }
    }
}
