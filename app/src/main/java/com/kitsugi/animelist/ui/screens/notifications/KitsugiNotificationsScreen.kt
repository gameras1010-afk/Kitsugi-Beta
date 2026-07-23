package com.kitsugi.animelist.ui.screens.notifications

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.res.stringResource
import com.kitsugi.animelist.R
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.KitsugiAniListNotificationClient
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlinx.coroutines.launch

// ─── Platform seçimi ──────────────────────────────────────────────────────────

private enum class NotifPlatform(val label: String) {
    ANILIST("AniList"),
    MAL("MAL"),
    TMDB_SIMKL("TMDB & Simkl")
}

// ─── AniList alt filtre ───────────────────────────────────────────────────────

private enum class AniListFilter(
    val labelResId: Int,
    val group: KitsugiAniListNotificationClient.NotificationGroup
) {
    ALL(R.string.notif_filter_all, KitsugiAniListNotificationClient.NotificationGroup.ALL),
    AIRING(R.string.notif_filter_airing, KitsugiAniListNotificationClient.NotificationGroup.AIRING),
    ACTIVITY(R.string.notif_filter_activity, KitsugiAniListNotificationClient.NotificationGroup.ACTIVITY),
    FORUM(R.string.notif_filter_forum, KitsugiAniListNotificationClient.NotificationGroup.FORUM),
    FOLLOWS(R.string.notif_filter_follows, KitsugiAniListNotificationClient.NotificationGroup.FOLLOWS),
    MEDIA(R.string.notif_filter_media, KitsugiAniListNotificationClient.NotificationGroup.MEDIA)
}

// ─── Ana Ekran ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KitsugiNotificationsScreen(
    mediaEntries: List<MediaEntry>,
    isAniListConnected: Boolean,
    isMalConnected: Boolean,
    isSimklConnected: Boolean,
    onBack: () -> Unit,
    onOpenApiDetail: ((mediaId: Int, source: String, mediaType: String?) -> Unit)? = null,
    viewModel: KitsugiNotificationsViewModel = viewModel()
) {
    val accentColor = LocalKitsugiAccent.current
    val scope = rememberCoroutineScope()

    // ── ViewModel state'lerini topla ──
    val aniListState by viewModel.aniList.collectAsState()
    val malState    by viewModel.mal.collectAsState()
    val simklState  by viewModel.tmdbSimkl.collectAsState()

    // ── Sayfa + filtre state ──
    val pagerState = rememberPagerState(
        initialPage = when {
            isAniListConnected -> 0
            isMalConnected     -> 1
            isSimklConnected   -> 2
            else               -> 0
        },
        pageCount = { 3 }
    )
    var aniListFilter by remember { mutableStateOf(AniListFilter.ALL) }

    // ── İlk yükleme — sayfa veya filtre değiştiğinde ──
    LaunchedEffect(pagerState.currentPage, aniListFilter) {
        when (pagerState.currentPage) {
            0 -> if (isAniListConnected) viewModel.loadAniList(
                group = aniListFilter.group,
                resetPage = true,
                mediaEntries = mediaEntries
            )
            1 -> if (isMalConnected) viewModel.loadMal(mediaEntries)
            2 -> if (isSimklConnected) viewModel.loadTmdbSimkl(mediaEntries)
        }
    }

    val aniListListState  = rememberLazyListState()
    val malListState      = rememberLazyListState()
    val tmdbSimklListState = rememberLazyListState()

    // ── Infinite scroll (AniList) — sadece scroll sonunda tetikle ──
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = aniListListState.layoutInfo.totalItemsCount
            val last  = aniListListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= total - 5 && total > 0
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && pagerState.currentPage == 0 && !aniListState.isLoading && aniListState.hasMore) {
            viewModel.loadAniList(
                group = aniListFilter.group,
                resetPage = false,
                mediaEntries = mediaEntries
            )
        }
    }

    // ── Filtre değişince liste başa dönsün ──
    LaunchedEffect(aniListFilter) {
        aniListListState.scrollToItem(0)
    }

    val configuration = LocalConfiguration.current
    val isLandscape   = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen  = isLandscape || configuration.screenWidthDp > 600

    // ─────────────────────────────────────────────────────────────────────────
    //  UI
    // ─────────────────────────────────────────────────────────────────────────

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
                    contentDescription = stringResource(R.string.notif_action_back),
                    tint = KitsugiColors.TextPrimary
                )
            }
            Text(
                text = stringResource(R.string.notif_title),
                color = KitsugiColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
            IconButton(onClick = {
                when (pagerState.currentPage) {
                    0 -> viewModel.loadAniList(aniListFilter.group, resetPage = true, mediaEntries)
                    1 -> viewModel.loadMal(mediaEntries)
                    2 -> viewModel.loadTmdbSimkl(mediaEntries)
                }
            }) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.notif_action_refresh),
                    tint = accentColor
                )
            }
        }

        // ── Platform Sekmeleri ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isLandscape) 18.dp else 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(KitsugiColors.Surface),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                NotifPlatform.entries.forEachIndexed { index, platform ->
                    val active  = pagerState.currentPage == index
                    val enabled = when (platform) {
                        NotifPlatform.ANILIST    -> isAniListConnected
                        NotifPlatform.MAL        -> isMalConnected
                        NotifPlatform.TMDB_SIMKL -> isSimklConnected || true
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (active) accentColor else KitsugiColors.Surface)
                            .tvClickable(shape = RoundedCornerShape(22.dp), enabled = enabled) {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = platform.label,
                                color = if (active) KitsugiColors.Background
                                        else if (enabled) KitsugiColors.TextPrimary
                                        else KitsugiColors.TextMuted,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.Black else FontWeight.Medium
                            )
                            if (enabled) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (active) KitsugiColors.Background
                                            else KitsugiColors.AccentGreen
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            color = KitsugiColors.SurfaceStrong.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Pager ──
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                // ──────────────── Page 0: AniList ────────────────
                0 -> {
                    if (!isAniListConnected) {
                        CenteredEmptyState(stringResource(R.string.notif_login_required_anilist))
                    } else {
                        LazyColumn(
                            state = aniListListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            // Alt filtreler
                            item(key = "al_filters") {
                                AniListFilterHeader(
                                    activeFilter = aniListFilter,
                                    onFilterSelected = { aniListFilter = it },
                                    isWideScreen = isWideScreen,
                                    accentColor = accentColor
                                )
                            }

                            when {
                                aniListState.isLoading && aniListState.items.isEmpty() -> {
                                    item { LoadingState(accentColor) }
                                }
                                aniListState.error != null && aniListState.items.isEmpty() -> {
                                    item { CenteredEmptyState(aniListState.error!!) }
                                }
                                aniListState.items.isEmpty() -> {
                                    item { CenteredEmptyState(stringResource(R.string.notif_empty_no_notifications)) }
                                }
                                else -> {
                                    items(aniListState.items, key = { it.id }) { notif ->
                                        NotifItemRow(
                                            notif = notif,
                                            accentColor = accentColor,
                                            onClick = {
                                                notif.mediaId?.let { id -> onOpenApiDetail?.invoke(id, "anilist", notif.mediaType) }
                                            }
                                        )
                                    }
                                    // Sayfa sonu yükleyici
                                    if (aniListState.isLoading) {
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

                // ──────────────── Page 1: MAL ────────────────
                1 -> {
                    if (!isMalConnected) {
                        CenteredEmptyState(stringResource(R.string.notif_login_required_mal))
                    } else {
                        LazyColumn(
                            state = malListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            when {
                                malState.isLoading && malState.items.isEmpty() -> {
                                    item { LoadingState(accentColor) }
                                }
                                malState.error != null && malState.items.isEmpty() -> {
                                    item { CenteredEmptyState(malState.error!!) }
                                }
                                malState.items.isEmpty() -> {
                                    item { CenteredEmptyState(stringResource(R.string.notif_empty_list_mal)) }
                                }
                                else -> {
                                    items(malState.items, key = { it.id }) { notif ->
                                        NotifItemRow(
                                            notif = notif,
                                            accentColor = accentColor,
                                            onClick = {
                                                notif.mediaId?.let { id -> onOpenApiDetail?.invoke(id, "jikan", notif.mediaType) }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ──────────────── Page 2: TMDB & Simkl ────────────────
                2 -> {
                    if (!isSimklConnected) {
                        CenteredEmptyState(stringResource(R.string.notif_login_required_simkl))
                    } else {
                        LazyColumn(
                            state = tmdbSimklListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            when {
                                simklState.isLoading && simklState.items.isEmpty() -> {
                                    item { LoadingState(accentColor) }
                                }
                                simklState.error != null && simklState.items.isEmpty() -> {
                                    item { CenteredEmptyState(simklState.error!!) }
                                }
                                simklState.items.isEmpty() -> {
                                    item { CenteredEmptyState(stringResource(R.string.notif_empty_list_simkl)) }
                                }
                                else -> {
                                    items(simklState.items, key = { it.id }) { notif ->
                                        NotifItemRow(
                                            notif = notif,
                                            accentColor = accentColor,
                                            onClick = {
                                                notif.mediaId?.let { id -> onOpenApiDetail?.invoke(id, "simkl", notif.mediaType) }
                                            }
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
}

// ─── AniList Alt Filtre Header ────────────────────────────────────────────────

@Composable
private fun AniListFilterHeader(
    activeFilter: AniListFilter,
    onFilterSelected: (AniListFilter) -> Unit,
    isWideScreen: Boolean,
    accentColor: Color
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KitsugiColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rowMod = if (isWideScreen) {
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(KitsugiColors.Surface)
                    .padding(4.dp)
            } else {
                Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(KitsugiColors.Surface)
                    .horizontalScroll(scrollState)
                    .padding(4.dp)
            }
            Row(
                modifier = rowMod,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isWideScreen) Arrangement.spacedBy(0.dp) else Arrangement.spacedBy(4.dp)
            ) {
                AniListFilter.entries.forEach { filter ->
                    val active    = activeFilter == filter
                    val itemMod   = if (isWideScreen) Modifier.weight(1f) else Modifier
                    Box(
                        modifier = itemMod
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (active) accentColor else Color.Transparent)
                            .tvClickable(shape = RoundedCornerShape(22.dp)) { onFilterSelected(filter) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(filter.labelResId),
                            color = if (active) KitsugiColors.Background else KitsugiColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            color = KitsugiColors.SurfaceStrong.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Bildirim Satır Kartı ─────────────────────────────────────────────────────

@Composable
private fun NotifItemRow(
    notif: NotifItem,
    accentColor: Color,
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
                Text(text = notif.dateText, color = KitsugiColors.TextMuted, fontSize = 11.sp)
            }
        }
    }
    HorizontalDivider(
        color = KitsugiColors.SurfaceStrong.copy(alpha = 0.35f),
        modifier = Modifier.padding(start = 80.dp, end = 16.dp)
    )
}

// ─── Yardımcı Composable'lar ──────────────────────────────────────────────────

@Composable
private fun LoadingState(accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = accentColor)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.loading), color = KitsugiColors.TextMuted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun CenteredEmptyState(message: String) {
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
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
