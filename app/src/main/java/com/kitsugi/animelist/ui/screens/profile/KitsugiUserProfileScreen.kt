@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.kitsugi.animelist.ui.screens.profile

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.app.ProfileFavoriteItem
import com.kitsugi.animelist.ui.components.KitsugiActivityDetailBottomSheet
import com.kitsugi.animelist.ui.components.KitsugiImageGalleryDialog
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.utils.ShareUtils
import kotlinx.coroutines.launch
import com.kitsugi.animelist.ui.screens.profile.tabs.UserProfileInfoTab
import com.kitsugi.animelist.ui.screens.profile.tabs.UserProfileActivityTab
import com.kitsugi.animelist.ui.screens.profile.tabs.UserProfileStatsTab
import com.kitsugi.animelist.ui.screens.profile.tabs.UserProfileFavoritesTab
import com.kitsugi.animelist.ui.screens.profile.tabs.UserProfileSocialTab

@Composable
fun KitsugiUserProfileScreen(
    userId: Int,
    fallbackUsername: String? = null,
    fallbackAvatar: String? = null,
    appSettings: AppSettings,
    mediaEntries: List<MediaEntry>,
    onBackClick: () -> Unit,
    onFavoriteMediaClick: (mediaId: Int, mediaType: MediaType, source: String, title: String, imageUrl: String?) -> Unit,
    onFavoriteCharacterClick: (charId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStaffClick: (staffId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStudioClick: ((studioId: Int, source: String, name: String?, imageUrl: String?) -> Unit)? = null,
    onUserProfileClick: (userId: Int, username: String, avatarUrl: String?) -> Unit,
    onGenreClick: (genre: String) -> Unit = {},
    onTagClick: (tag: String) -> Unit = {},
    onOpenUserMediaList: (userId: Int, mediaType: MediaType) -> Unit = { _, _ -> },
    accentColor: Color? = null,
    customViewModel: KitsugiUserProfileViewModel? = null,
    onScrollReset: () -> Unit = {}
) {
    val resolvedAccent = accentColor ?: LocalKitsugiAccent.current
    val viewModel: KitsugiUserProfileViewModel = customViewModel ?: viewModel(key = "user_profile_$userId")

    LaunchedEffect(userId) { viewModel.loadUser(userId, fallbackUsername, fallbackAvatar) }

    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember { JikanApiClient() }

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = viewModel.activeTab.coerceIn(0, 4),
        pageCount = { 5 }
    )
    LaunchedEffect(pagerState.currentPage) { viewModel.activeTab = pagerState.currentPage }
    val activeTab = pagerState.currentPage

    var activeFavoriteSheet by remember { mutableStateOf<Pair<String, List<ProfileFavoriteItem>>?>(null) }
    var activeActivityIdForDetail by remember { mutableStateOf<Int?>(null) }
    var activeGalleryImages by remember { mutableStateOf<Triple<List<String>, Int, String>?>(null) }

    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.scrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.scrollOffset
    )

    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            viewModel.updateScroll(index, offset)
            if (index == 0 && offset == 0) onScrollReset()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(KitsugiColors.Background)) {

        // ── Top bar ──────────────────────────────────────────────────────────
        Surface(modifier = Modifier.fillMaxWidth(), color = KitsugiColors.Surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = KitsugiColors.TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = state.name.ifBlank { fallbackUsername ?: "Kullanıcı Profili" },
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (state.name.isNotBlank()) {
                    IconButton(onClick = {
                        ShareUtils.shareText(context, state.name, ShareUtils.buildProfileUrl("anilist", state.name))
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Profili Paylaş", tint = KitsugiColors.TextPrimary)
                    }
                }
            }
        }

        // ── Content ──────────────────────────────────────────────────────────
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.name.isNotBlank(),
            onRefresh = { viewModel.loadUser(userId, fallbackUsername, fallbackAvatar, forceRefresh = true) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = pullRefreshState
        ) {
            if (state.isLoading && state.name.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = resolvedAccent)
                }
            } else {
                val username = state.name.ifBlank { fallbackUsername ?: "Kullanıcı" }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                        .padding(horizontal = if (isLandscape) 18.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Banner + avatar header ────────────────────────────
                    item {
                        UserProfileHeaderCard(
                            state = state,
                            fallbackUsername = fallbackUsername,
                            fallbackAvatar = fallbackAvatar,
                            accentColor = resolvedAccent,
                            onImageClick = { urls, idx, title -> activeGalleryImages = Triple(urls, idx, title) }
                        )
                    }

                    // ── Action buttons ────────────────────────────────────
                    item {
                        UserProfileActionButtons(
                            state = state,
                            accentColor = resolvedAccent,
                            userId = state.userId,
                            viewModel = viewModel,
                            onOpenUserMediaList = onOpenUserMediaList
                        )
                    }

                    // ── Sticky tab bar ────────────────────────────────────
                    stickyHeader(key = "user_tabs") {
                        Surface(modifier = Modifier.fillMaxWidth(), color = KitsugiColors.Background) {
                            Column(modifier = Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                val tabs = listOf(
                                    Icons.Rounded.Info to "Hakkında",
                                    Icons.Rounded.ChatBubble to "Aktivite",
                                    Icons.Rounded.BarChart to "İstatistikler",
                                    Icons.Rounded.Star to "Favoriler",
                                    Icons.Rounded.People to "Sosyal"
                                )
                                ProfileHeaderIconTabs(
                                    tabs = tabs,
                                    selectedTab = pagerState.currentPage,
                                    onTabSelected = { coroutineScope.launch { pagerState.animateScrollToPage(it) } },
                                    accentColor = resolvedAccent
                                )
                                // Sub-filter chips
                                when (activeTab) {
                                    2 -> {
                                        val subTabs = listOf("Genel", "Türler", "Etiketler", "Ekip", "Seslendirenler", "Stüdyolar")
                                        val s = rememberLazyListState()
                                        LazyRow(state = s, flingBehavior = rememberSnapFlingBehavior(s, SnapPosition.Start),
                                            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(subTabs.size) { idx ->
                                                ProfileFilterChip(subTabs[idx], viewModel.statsSubTab == idx, resolvedAccent) { viewModel.statsSubTab = idx }
                                            }
                                        }
                                    }
                                    3 -> {
                                        val s = rememberLazyListState()
                                        val labels = listOf("Anime", "Manga", "Karakterler", "Ekip", "Stüdyolar")
                                        LazyRow(state = s, flingBehavior = rememberSnapFlingBehavior(s, SnapPosition.Start),
                                            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(5) { idx ->
                                                ProfileFilterChip(labels[idx], viewModel.favoritesFilter == idx, resolvedAccent) { viewModel.favoritesFilter = idx }
                                            }
                                        }
                                    }
                                    4 -> {
                                        val s = rememberLazyListState()
                                        LazyRow(state = s, flingBehavior = rememberSnapFlingBehavior(s, SnapPosition.Start),
                                            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            item { ProfileFilterChip("Takipçiler (${state.socialState.followers.size})", viewModel.socialFilter == 0, resolvedAccent) { viewModel.socialFilter = 0 } }
                                            item { ProfileFilterChip("Takip Edilen (${state.socialState.following.size})", viewModel.socialFilter == 1, resolvedAccent) { viewModel.socialFilter = 1 } }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Pager ─────────────────────────────────────────────
                    item(key = "user_content") {
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        val pageHeights = remember { androidx.compose.runtime.mutableStateMapOf<Int, Int>() }
                        val currentPage = pagerState.currentPage
                        val currentPageOffset = pagerState.currentPageOffsetFraction
                        val targetPage = if (currentPageOffset > 0f) currentPage + 1 else if (currentPageOffset < 0f) currentPage - 1 else currentPage
                        val currentH = pageHeights[currentPage] ?: 0
                        val targetH = pageHeights[targetPage] ?: currentH
                        val interpolatedH = remember(currentH, targetH, currentPageOffset) {
                            val px = if (currentH > 0 && targetH > 0) currentH + (targetH - currentH) * kotlin.math.abs(currentPageOffset)
                            else if (currentH > 0) currentH.toFloat() else 0f
                            if (px > 0f) with(density) { px.toDp() } else null
                        }
                        val screenH = LocalConfiguration.current.screenHeightDp

                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 1,
                            pageSpacing = 12.dp,
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                                .layout { measurable, constraints ->
                                    val minH = with(density) { (screenH - 64).dp.roundToPx() }
                                    val placeable = measurable.measure(constraints.copy(minHeight = minH, maxHeight = androidx.compose.ui.unit.Constraints.Infinity))
                                    val h = interpolatedH?.roundToPx()?.coerceAtLeast(minH) ?: placeable.height
                                    layout(placeable.width, h) { placeable.placeRelative(0, 0) }
                                }.clipToBounds()
                        ) { page ->
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .onGloballyPositioned { pageHeights[page] = it.size.height },
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                when (page) {
                                    0 -> UserProfileInfoTab(state, resolvedAccent, username) { urls, idx, title -> activeGalleryImages = Triple(urls, idx, title) }
                                    1 -> UserProfileActivityTab(state, resolvedAccent, appSettings, userId, fallbackUsername, fallbackAvatar, viewModel, apiClient, onFavoriteMediaClick) { activeActivityIdForDetail = it }
                                    2 -> UserProfileStatsTab(state, resolvedAccent, viewModel, onGenreClick, onTagClick, onFavoriteStaffClick, onFavoriteStudioClick)
                                    3 -> UserProfileFavoritesTab(state, resolvedAccent, appSettings, viewModel, isLandscape, onFavoriteMediaClick, onFavoriteCharacterClick, onFavoriteStaffClick, onFavoriteStudioClick) { title, items -> activeFavoriteSheet = title to items }
                                    4 -> UserProfileSocialTab(state, resolvedAccent, viewModel, isLandscape, onUserProfileClick)
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ── Overlays ─────────────────────────────────────────────────────────────
    if (activeFavoriteSheet != null) {
        val favoritesFilter = viewModel.favoritesFilter
        val currentHasNext = when (favoritesFilter) {
            0 -> state.favAnimeHasNext; 1 -> state.favMangaHasNext
            2 -> state.favCharHasNext; 3 -> state.favStaffHasNext
            4 -> state.favStudioHasNext; else -> false
        }
        val currentCategory = when (favoritesFilter) {
            0 -> "anime"; 1 -> "manga"; 2 -> "characters"; 3 -> "staff"; 4 -> "studios"; else -> "anime"
        }
        val currentFavList = when (favoritesFilter) {
            0 -> state.favoriteAnime; 1 -> state.favoriteManga
            2 -> state.favoriteCharacters; 3 -> state.favoriteStaff
            4 -> state.favoriteStudios; else -> activeFavoriteSheet!!.second
        }
        FavoritesExpandedBottomSheet(
            title = activeFavoriteSheet!!.first,
            items = currentFavList,
            blurAdultMedia = appSettings.blurAdultMedia,
            hasNextPage = currentHasNext,
            onLoadMore = { viewModel.loadMoreFavorites(currentCategory) },
            onItemClick = { item ->
                item.id.toIntOrNull()?.let { id ->
                    when (favoritesFilter) {
                        0 -> onFavoriteMediaClick(id, MediaType.Anime, "anilist", item.title, item.imageUrl)
                        1 -> onFavoriteMediaClick(id, MediaType.Manga, "anilist", item.title, item.imageUrl)
                        2 -> onFavoriteCharacterClick(id, "anilist", item.title, item.imageUrl)
                        3 -> onFavoriteStaffClick(id, "anilist", item.title, item.imageUrl)
                        4 -> onFavoriteStudioClick?.invoke(id, "anilist", item.title, item.imageUrl)
                    }
                }
                activeFavoriteSheet = null
            },
            onDismiss = { activeFavoriteSheet = null }
        )
    }

    if (activeActivityIdForDetail != null) {
        KitsugiActivityDetailBottomSheet(
            activityId = activeActivityIdForDetail!!,
            apiClient = apiClient,
            titleLanguage = appSettings.titleLanguage.toString(),
            blurAdultMedia = appSettings.blurAdultMedia,
            onMediaClick = { mediaId, mType, source -> onFavoriteMediaClick(mediaId, mType, source, "", null) },
            onDismiss = { activeActivityIdForDetail = null; viewModel.loadUser(userId, fallbackUsername, fallbackAvatar) }
        )
    }

    if (activeGalleryImages != null) {
        val (urls, idx, title) = activeGalleryImages!!
        KitsugiImageGalleryDialog(imageUrls = urls, initialIndex = idx, title = title, onDismiss = { activeGalleryImages = null })
    }
}
