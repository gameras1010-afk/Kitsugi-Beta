package com.kitsugi.animelist.ui.screens.notifications

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import com.kitsugi.animelist.data.remote.KitsugiAiringCalendarClient
import com.kitsugi.animelist.data.remote.KitsugiAniListNotificationClient
import com.kitsugi.animelist.data.remote.SimklApiClient
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Platform seçimi ──────────────────────────────────────────────────────────

private enum class NotifPlatform(val label: String) {
    ANILIST("AniList"),
    MAL("MAL"),
    TMDB_SIMKL("TMDB & Simkl")
}

// ─── AniList alt filtre ───────────────────────────────────────────────────────

private enum class AniListFilter(
    val label: String,
    val group: KitsugiAniListNotificationClient.NotificationGroup
) {
    ALL("Tümü", KitsugiAniListNotificationClient.NotificationGroup.ALL),
    AIRING("Yayın", KitsugiAniListNotificationClient.NotificationGroup.AIRING),
    ACTIVITY("Aktivite", KitsugiAniListNotificationClient.NotificationGroup.ACTIVITY),
    FORUM("Forum", KitsugiAniListNotificationClient.NotificationGroup.FORUM),
    FOLLOWS("Takip", KitsugiAniListNotificationClient.NotificationGroup.FOLLOWS),
    MEDIA("Medya", KitsugiAniListNotificationClient.NotificationGroup.MEDIA)
}

// ─── Basit bildirim veri modeli (tüm platformlar ortak) ───────────────────────

private data class SimpleNotif(
    val id: String,
    val imageUrl: String?,
    val title: String,
    val body: String,
    val dateText: String?,
    val isUnread: Boolean = false,
    val mediaId: Int? = null,
    val activityId: Int? = null
)

// ─── Ana Ekran ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiNotificationsScreen(
    mediaEntries: List<MediaEntry>,
    isAniListConnected: Boolean,
    isMalConnected: Boolean,
    isSimklConnected: Boolean,
    onBack: () -> Unit,
    onOpenApiDetail: ((mediaId: Int, source: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val scope = rememberCoroutineScope()

    // Platform seçimi — varsayılan: bağlı olan ilk platform
    var selectedPlatform by remember {
        mutableStateOf(
            when {
                isAniListConnected -> NotifPlatform.ANILIST
                isMalConnected     -> NotifPlatform.MAL
                isSimklConnected   -> NotifPlatform.TMDB_SIMKL
                else               -> NotifPlatform.ANILIST
            }
        )
    }

    // ── AniList state ──
    var aniListFilter by remember { mutableStateOf(AniListFilter.ALL) }
    var aniListNotifs by remember { mutableStateOf<List<SimpleNotif>>(emptyList()) }
    var aniListLoading by remember { mutableStateOf(false) }
    var aniListError by remember { mutableStateOf<String?>(null) }
    var aniListPage by remember { mutableStateOf(1) }
    var aniListHasMore by remember { mutableStateOf(true) }

    // ── MAL state ──
    var malNotifs by remember { mutableStateOf<List<SimpleNotif>>(emptyList()) }
    var malLoading by remember { mutableStateOf(false) }

    // ── TMDB+Simkl state ──
    var tmdbSimklNotifs by remember { mutableStateOf<List<SimpleNotif>>(emptyList()) }
    var tmdbSimklLoading by remember { mutableStateOf(false) }

    // ── AniList yükleyici ──
    suspend fun loadAniList(resetPage: Boolean = false) {
        val token = ExternalAuthManager.getAniListToken(context) ?: return
        if (resetPage) { aniListPage = 1; aniListHasMore = true }
        if (!aniListHasMore) return
        aniListLoading = true
        aniListError = null
        try {
            val client = KitsugiAniListNotificationClient()
            val result = client.fetchNotifications(
                accessToken = token,
                page = aniListPage,
                perPage = 25,
                group = aniListFilter.group,
                resetCount = aniListPage == 1
            )
            val mapped = result.notifications.map { n ->
                val body = when (n.type) {
                    "AIRING" -> buildString {
                        n.airingContexts?.getOrNull(0)?.let { append(it) }
                        append(" ")
                        n.airingContexts?.getOrNull(1)?.let { append(it) }
                        append(" ${n.episode}")
                        n.airingContexts?.getOrNull(2)?.let { append(it) }
                    }.trim().ifBlank { "Bölüm ${n.episode} yayında" }
                    "FOLLOWING"                  -> "${n.userName} sizi takip etti"
                    "ACTIVITY_MESSAGE"           -> "${n.userName} size mesaj gönderdi"
                    "ACTIVITY_REPLY"             -> "${n.userName} ${n.context ?: "aktivitenize yanıt verdi"}"
                    "ACTIVITY_REPLY_SUBSCRIBED"  -> "${n.userName} ${n.context ?: "abone olduğunuz aktiviteye yanıt verdi"}"
                    "ACTIVITY_MENTION"           -> "${n.userName} ${n.context ?: "sizi bir aktivitede bahsetti"}"
                    "ACTIVITY_LIKE"              -> "${n.userName} ${n.context ?: "aktivitenizi beğendi"}"
                    "ACTIVITY_REPLY_LIKE"        -> "${n.userName} ${n.context ?: "yanıtınızı beğendi"}"
                    "THREAD_COMMENT_MENTION"     -> "${n.userName} sizi bir forum yorumunda etiketledi"
                    "THREAD_COMMENT_REPLY"       -> "${n.userName} forum yorumunuza yanıt verdi"
                    "THREAD_COMMENT_SUBSCRIBED"  -> "${n.userName} abone olduğunuz konuya yorum yaptı"
                    "THREAD_COMMENT_LIKE"        -> "${n.userName} forum yorumunuzu beğendi"
                    "THREAD_LIKE"                -> "${n.userName} ${n.threadTitle ?: "forumunuzu"} beğendi"
                    "RELATED_MEDIA_ADDITION"     -> "${n.mediaTitle} ${n.context ?: "ilgili medya olarak eklendi"}"
                    "MEDIA_DATA_CHANGE"          -> "${n.mediaTitle} ${n.context ?: "verisi güncellendi"}"
                    "MEDIA_MERGE"                -> "${n.mediaTitle} ${n.context ?: "başka bir medyayla birleştirildi"}"
                    "MEDIA_DELETION"             -> "${n.deletedMediaTitle ?: "Bir medya"} silindi"
                    else -> n.context ?: "Yeni bildirim"
                }
                val imageUrl = n.mediaCoverUrl ?: n.userAvatarUrl
                SimpleNotif(
                    id = "al_${n.id}",
                    imageUrl = imageUrl,
                    title = n.mediaTitle ?: n.userName ?: n.threadTitle ?: "AniList",
                    body = body,
                    dateText = n.dateText,
                    mediaId = n.mediaId,
                    activityId = n.activityId
                )
            }
            aniListNotifs = if (resetPage) mapped else (aniListNotifs + mapped)
            aniListHasMore = result.hasNextPage
            if (result.hasNextPage) aniListPage++
        } catch (e: Exception) {
            aniListError = "Bildirimler yüklenemedi: ${e.message}"
        } finally {
            aniListLoading = false
        }
    }

    // ── MAL yükleyici (Airing Calendar + watchlist eşleşme) ──
    suspend fun loadMal() {
        malLoading = true
        try {
            val calendarClient = KitsugiAiringCalendarClient()
            val schedule = calendarClient.fetchWeeklySchedule()
            val allEntries = schedule.values.flatten()
            val now = System.currentTimeMillis()
            val malEntries = mediaEntries.filter {
                (it.source.equals("jikan", ignoreCase = true) || it.source.equals("mal", ignoreCase = true)) &&
                (it.status == WatchStatus.Watching || it.status == WatchStatus.Repeating)
            }
            val matched = allEntries.filter { entry ->
                malEntries.any { me -> me.malId == entry.malId }
            }.filter { entry ->
                val triggerMs = entry.airingAt * 1000L
                triggerMs <= now && triggerMs > now - 7 * 24 * 60 * 60 * 1000L
            }
            malNotifs = matched.map { entry ->
                val dateText = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
                    .format(Date(entry.airingAt * 1000L))
                SimpleNotif(
                    id = "mal_${entry.malId}_${entry.episode}",
                    imageUrl = null,
                    title = entry.title,
                    body = "Bölüm ${entry.episode} yayınlandı 🎬",
                    dateText = dateText,
                    mediaId = entry.malId
                )
            }.sortedByDescending { it.id }
        } catch (e: Exception) {
            android.util.Log.e("KitsugiNotif", "MAL load failed: ${e.message}")
        } finally {
            malLoading = false
        }
    }

    // ── TMDB+Simkl yükleyici ──
    suspend fun loadTmdbSimkl() {
        tmdbSimklLoading = true
        try {
            val simklClient = SimklApiClient()
            val tvCal = simklClient.getCalendar("tv")
            val animeCal = simklClient.getCalendar("anime")
            val movieCal = simklClient.getCalendar("movies")
            val allCal = (tvCal + animeCal + movieCal).distinctBy { it.malId }

            val tmdbSimklEntries = mediaEntries.filter { me ->
                (me.source.equals("tmdb", ignoreCase = true) || me.source.equals("simkl", ignoreCase = true)) &&
                (me.status == WatchStatus.Watching || me.status == WatchStatus.Repeating)
            }

            val matched = allCal.filter { calItem ->
                tmdbSimklEntries.any { me ->
                    (me.simklId != null && me.simklId == calItem.malId) ||
                    (me.tmdbId != null && calItem.tmdbId != null && me.tmdbId == calItem.tmdbId)
                }
            }

            val now = System.currentTimeMillis()
            tmdbSimklNotifs = matched.map { item ->
                SimpleNotif(
                    id = "simkl_${item.malId}",
                    imageUrl = item.imageUrl,
                    title = item.title,
                    body = "Yeni içerik mevcut 🎬",
                    dateText = null,
                    mediaId = item.malId
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("KitsugiNotif", "TMDB/Simkl load failed: ${e.message}")
        } finally {
            tmdbSimklLoading = false
        }
    }

    // İlk yükleme
    LaunchedEffect(selectedPlatform, aniListFilter) {
        when (selectedPlatform) {
            NotifPlatform.ANILIST    -> if (isAniListConnected) loadAniList(resetPage = true)
            NotifPlatform.MAL        -> loadMal()
            NotifPlatform.TMDB_SIMKL -> loadTmdbSimkl()
        }
    }

    val listState = rememberLazyListState()

    // Infinite scroll (AniList)
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val last  = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= total - 5 && total > 0
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && selectedPlatform == NotifPlatform.ANILIST && !aniListLoading && aniListHasMore) {
            loadAniList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
            .statusBarsPadding()
    ) {
        // ── Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Geri",
                    tint = KitsugiColors.TextPrimary
                )
            }
            Text(
                text = "Bildirimler",
                color = KitsugiColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            IconButton(onClick = {
                scope.launch {
                    when (selectedPlatform) {
                        NotifPlatform.ANILIST    -> loadAniList(resetPage = true)
                        NotifPlatform.MAL        -> loadMal()
                        NotifPlatform.TMDB_SIMKL -> loadTmdbSimkl()
                    }
                }
            }) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Yenile",
                    tint = accentColor
                )
            }
        }

        // ── Platform Filtreleri ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            items(NotifPlatform.entries) { platform ->
                val active = selectedPlatform == platform
                val enabled = when (platform) {
                    NotifPlatform.ANILIST    -> isAniListConnected
                    NotifPlatform.MAL        -> isMalConnected
                    NotifPlatform.TMDB_SIMKL -> isSimklConnected || true // TMDB her zaman açık
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            when {
                                active  -> accentColor
                                !enabled -> KitsugiColors.SurfaceSoft.copy(alpha = 0.4f)
                                else    -> KitsugiColors.SurfaceStrong
                            }
                        )
                        .tvClickable(shape = RoundedCornerShape(20.dp), enabled = enabled) {
                            selectedPlatform = platform
                        }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = platform.label,
                        color = when {
                            active   -> KitsugiColors.Background
                            !enabled -> KitsugiColors.TextMuted
                            else     -> KitsugiColors.TextPrimary
                        },
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // ── AniList Alt Filtreleri ──
        if (selectedPlatform == NotifPlatform.ANILIST && isAniListConnected) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                items(AniListFilter.entries) { filter ->
                    val active = aniListFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (active) accentColor.copy(alpha = 0.18f)
                                else KitsugiColors.Surface
                            )
                            .tvClickable(shape = RoundedCornerShape(14.dp)) {
                                aniListFilter = filter
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            color = if (active) accentColor else KitsugiColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            color = KitsugiColors.SurfaceStrong.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        )

        // ── İçerik ──
        AnimatedContent(
            targetState = selectedPlatform,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "notif_content"
        ) { platform ->

            val (notifs, isLoading, errorMsg) = when (platform) {
                NotifPlatform.ANILIST    -> Triple(aniListNotifs, aniListLoading, aniListError)
                NotifPlatform.MAL        -> Triple(malNotifs, malLoading, null)
                NotifPlatform.TMDB_SIMKL -> Triple(tmdbSimklNotifs, tmdbSimklLoading, null)
            }

            when {
                isLoading && notifs.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = accentColor)
                            Spacer(Modifier.height(12.dp))
                            Text("Yükleniyor...", color = KitsugiColors.TextMuted, fontSize = 14.sp)
                        }
                    }
                }

                platform == NotifPlatform.ANILIST && !isAniListConnected -> {
                    NotifEmptyState("AniList'e giriş yapmanız gerekiyor")
                }

                platform == NotifPlatform.MAL && !isMalConnected -> {
                    NotifEmptyState("MAL'a giriş yapmanız gerekiyor")
                }

                errorMsg != null -> {
                    NotifEmptyState(errorMsg)
                }

                notifs.isEmpty() -> {
                    NotifEmptyState(
                        when (platform) {
                            NotifPlatform.ANILIST    -> "Yeni bildirim yok"
                            NotifPlatform.MAL        -> "İzleme listenizdeki animeler için yeni bölüm bulunamadı"
                            NotifPlatform.TMDB_SIMKL -> "İzleme listenizdeki içerikler için yeni bildirim yok"
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                    ) {
                        items(notifs, key = { it.id }) { notif ->
                            NotifItem(
                                notif = notif,
                                accentColor = accentColor,
                                onClick = {
                                    notif.mediaId?.let { id ->
                                        val source = when (platform) {
                                            NotifPlatform.ANILIST    -> "anilist"
                                            NotifPlatform.MAL        -> "jikan"
                                            NotifPlatform.TMDB_SIMKL -> "simkl"
                                        }
                                        onOpenApiDetail?.invoke(id, source)
                                    }
                                }
                            )
                        }

                        if (isLoading) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = accentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Bildirim Satır Kartı ─────────────────────────────────────────────────────

@Composable
private fun NotifItem(
    notif: SimpleNotif,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvClickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Görsel / ikon
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KitsugiColors.SurfaceStrong),
            contentAlignment = Alignment.Center
        ) {
            if (!notif.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = notif.imageUrl,
                    contentDescription = notif.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.NotificationsNone,
                    contentDescription = null,
                    tint = KitsugiColors.TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Metin
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notif.title,
                color = KitsugiColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = notif.body,
                color = KitsugiColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!notif.dateText.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notif.dateText,
                    color = KitsugiColors.TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }

    HorizontalDivider(
        color = KitsugiColors.SurfaceStrong.copy(alpha = 0.35f),
        modifier = Modifier.padding(start = 80.dp, end = 16.dp)
    )
}

// ─── Boş durum ────────────────────────────────────────────────────────────────

@Composable
private fun NotifEmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = null,
                tint = KitsugiColors.TextMuted,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                color = KitsugiColors.TextMuted,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
