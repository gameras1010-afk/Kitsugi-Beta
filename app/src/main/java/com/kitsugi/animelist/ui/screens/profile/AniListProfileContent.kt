@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kitsugi.animelist.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.app.AniListProfileState
import com.kitsugi.animelist.ui.app.KitsugiProfileViewModel
import com.kitsugi.animelist.ui.app.ProfileFavoriteItem
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.screens.profile.tabs.*
import kotlinx.coroutines.launch

@Composable
fun AniListProfileContent(
    viewModel: KitsugiProfileViewModel,
    state: AniListProfileState,
    mediaEntries: List<MediaEntry>,
    appSettings: AppSettings,
    onEntryClick: (MediaEntry) -> Unit,
    onFavoriteMediaClick: (mediaId: Int, mediaType: MediaType, source: String, title: String, imageUrl: String?) -> Unit,
    onFavoriteCharacterClick: (charId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStaffClick: (staffId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStudioClick: ((studioId: Int, source: String, name: String?, imageUrl: String?) -> Unit)? = null,
    onLoadMoreActivities: () -> Unit,
    onOpenFavoriteSheet: (title: String, items: List<ProfileFavoriteItem>, onClick: (ProfileFavoriteItem) -> Unit) -> Unit,
    onOpenStatsClick: (() -> Unit)? = null,
    onActivityClick: ((Int) -> Unit)? = null,
    onLikeClick: ((Int) -> Unit)? = null,
    onDeleteClick: ((Int) -> Unit)? = null,
    onGenreClick: (String) -> Unit = {},
    onTagClick: (String) -> Unit = {},
    onUserProfileClick: (userId: Int, username: String, avatarUrl: String?) -> Unit = { _, _, _ -> },
    isLandscape: Boolean,
    accentColor: Color,
    onImageClick: ((urls: List<String>, initialIndex: Int, title: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var activeTab by rememberSaveable { mutableIntStateOf(viewModel.aniListActiveTab) }
    LaunchedEffect(activeTab) { viewModel.aniListActiveTab = activeTab }

    var statsMediaType by rememberSaveable { mutableIntStateOf(viewModel.aniListStatsMediaType) }
    LaunchedEffect(statsMediaType) { viewModel.aniListStatsMediaType = statsMediaType }

    var statsSubTab by rememberSaveable { mutableIntStateOf(viewModel.aniListStatsSubTab) }
    LaunchedEffect(statsSubTab) { viewModel.aniListStatsSubTab = statsSubTab }

    var favoritesFilter by rememberSaveable { mutableIntStateOf(viewModel.aniListFavoritesFilter) }
    LaunchedEffect(favoritesFilter) { viewModel.aniListFavoritesFilter = favoritesFilter }

    var socialFilter by rememberSaveable { mutableIntStateOf(viewModel.aniListSocialFilter) }
    LaunchedEffect(socialFilter) { viewModel.aniListSocialFilter = socialFilter }

    var scoreDistType by rememberSaveable { mutableIntStateOf(viewModel.aniListScoreDistType) }
    LaunchedEffect(scoreDistType) { viewModel.aniListScoreDistType = scoreDistType }

    var lengthDistType by rememberSaveable { mutableIntStateOf(viewModel.aniListLengthDistType) }
    LaunchedEffect(lengthDistType) { viewModel.aniListLengthDistType = lengthDistType }

    var releaseYearDistType by rememberSaveable { mutableIntStateOf(viewModel.aniListReleaseYearDistType) }
    LaunchedEffect(releaseYearDistType) { viewModel.aniListReleaseYearDistType = releaseYearDistType }

    var startYearDistType by rememberSaveable { mutableIntStateOf(viewModel.aniListStartYearDistType) }
    LaunchedEffect(startYearDistType) { viewModel.aniListStartYearDistType = startYearDistType }

    var statSortType by rememberSaveable { mutableIntStateOf(viewModel.aniListStatsSortType) }
    LaunchedEffect(statSortType) { viewModel.aniListStatsSortType = statSortType }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.aniListScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.aniListScrollOffset
    )
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        viewModel.updateAniListScroll(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
    }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = viewModel.aniListActiveTab,
        pageCount = { 5 }
    )
    LaunchedEffect(pagerState.currentPage) {
        activeTab = pagerState.currentPage
        viewModel.aniListActiveTab = pagerState.currentPage
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isLandscape) 18.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Banner and Avatar ──
        item {
            val avatarUrl = state.avatarUrl?.takeIf { it.isNotBlank() }
            val bannerUrl = state.bannerUrl?.takeIf { it.isNotBlank() }
            val imageList = listOfNotNull(avatarUrl, bannerUrl)
            val username = state.name.ifBlank { "Kullanıcı" }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .then(
                        if (!bannerUrl.isNullOrBlank()) Modifier.clickable {
                            val idx = imageList.indexOf(bannerUrl).coerceAtLeast(0)
                            onImageClick?.invoke(imageList, idx, "$username Banner")
                        } else Modifier
                    )
            ) {
                if (!state.bannerUrl.isNullOrBlank()) {
                    AsyncImage(model = state.bannerUrl, contentDescription = "Banner", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(accentColor, KitsugiColors.AccentPink))))
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, KitsugiColors.Background.copy(alpha = 0.8f)))))

                // Share button
                if (state.name.isNotBlank()) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(36.dp).clip(CircleShape).background(KitsugiColors.Background.copy(alpha = 0.50f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = {
                            val url = com.kitsugi.animelist.utils.ShareUtils.buildProfileUrl("anilist", state.name)
                            com.kitsugi.animelist.utils.ShareUtils.shareText(context, state.name, url)
                        }) {
                            Icon(imageVector = Icons.Rounded.Share, contentDescription = "Profili Paylaş", tint = KitsugiColors.TextPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Avatar overlay
                Row(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = state.avatarUrl ?: "",
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(3.dp, KitsugiColors.Background, CircleShape)
                            .then(
                                if (!avatarUrl.isNullOrBlank()) Modifier.clickable {
                                    val idx = imageList.indexOf(avatarUrl).coerceAtLeast(0)
                                    onImageClick?.invoke(imageList, idx, "$username Profil Resmi")
                                } else Modifier
                            ),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = state.name, color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (!state.donatorBadge.isNullOrBlank() && state.donatorTier > 0) {
                            Text(text = "Donator Tier ${state.donatorTier}", color = KitsugiColors.AccentOrange, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Sticky Tab Bar + Sub-filters ──
        stickyHeader(key = "anilist_tabs_header") {
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
                        onTabSelected = { page -> coroutineScope.launch { pagerState.animateScrollToPage(page) } },
                        accentColor = accentColor
                    )

                    when (pagerState.currentPage) {
                        2 -> {
                            val statsSubTabState = androidx.compose.foundation.lazy.rememberLazyListState()
                            androidx.compose.foundation.lazy.LazyRow(
                                state = statsSubTabState,
                                flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                                    lazyListState = statsSubTabState,
                                    snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Start
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(6) { idx ->
                                    val label = listOf("Genel Bakış", "Türler", "Etiketler", "Ekip", "Seslendirenler", "Stüdyolar")[idx]
                                    ProfileFilterChip(text = label, isSelected = statsSubTab == idx, accentColor = accentColor, onClick = { statsSubTab = idx })
                                }
                            }
                        }
                        3 -> {
                            val favoritesFilterState = androidx.compose.foundation.lazy.rememberLazyListState()
                            androidx.compose.foundation.lazy.LazyRow(
                                state = favoritesFilterState,
                                flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                                    lazyListState = favoritesFilterState,
                                    snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Start
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(5) { idx ->
                                    val label = listOf("Anime", "Manga", "Karakterler", "Ekip", "Stüdyolar")[idx]
                                    ProfileFilterChip(text = label, isSelected = favoritesFilter == idx, accentColor = accentColor, onClick = { favoritesFilter = idx })
                                }
                            }
                        }
                        4 -> {
                            val socialFilterState = androidx.compose.foundation.lazy.rememberLazyListState()
                            androidx.compose.foundation.lazy.LazyRow(
                                state = socialFilterState,
                                flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                                    lazyListState = socialFilterState,
                                    snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Start
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item { ProfileFilterChip(text = "Takipçiler (${state.socialState.followers.size})", isSelected = socialFilter == 0, accentColor = accentColor, onClick = { socialFilter = 0 }) }
                                item { ProfileFilterChip(text = "Takip Edilen (${state.socialState.following.size})", isSelected = socialFilter == 1, accentColor = accentColor, onClick = { socialFilter = 1 }) }
                            }
                        }
                    }
                }
            }
        }

        // ── Pager Content ──
        item(key = "content") {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val pageHeights = remember { androidx.compose.runtime.mutableStateMapOf<Int, Int>() }
            val currentPage = pagerState.currentPage
            val currentPageOffset = pagerState.currentPageOffsetFraction
            val targetPage = if (currentPageOffset > 0f) currentPage + 1 else if (currentPageOffset < 0f) currentPage - 1 else currentPage

            val currentHeightPx = pageHeights[currentPage] ?: 0
            val targetHeightPx = pageHeights[targetPage] ?: currentHeightPx

            val interpolatedHeightDp = remember(currentHeightPx, targetHeightPx, currentPageOffset) {
                val heightPx = if (currentHeightPx > 0 && targetHeightPx > 0) {
                    currentHeightPx + (targetHeightPx - currentHeightPx) * kotlin.math.abs(currentPageOffset)
                } else if (currentHeightPx > 0) currentHeightPx.toFloat() else 0f
                if (heightPx > 0f) with(density) { heightPx.toDp() } else null
            }

            val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp

            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                beyondViewportPageCount = 1,
                pageSpacing = 12.dp,
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val minPagerHeightPx = with(density) { (screenHeightDp - 64).dp.roundToPx() }
                        val placeable = measurable.measure(constraints.copy(minHeight = minPagerHeightPx, maxHeight = androidx.compose.ui.unit.Constraints.Infinity))
                        val height = interpolatedHeightDp?.roundToPx()?.coerceAtLeast(minPagerHeightPx) ?: placeable.height
                        layout(placeable.width, height) { placeable.placeRelative(0, 0) }
                    }
                    .clipToBounds()
            ) { page ->
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 600.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().onGloballyPositioned { pageHeights[page] = it.size.height },
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (page) {
                            0 -> AniListInfoTab(state = state, accentColor = accentColor, onImageClick = onImageClick)
                            1 -> AniListActivityTab(
                                state = state, accentColor = accentColor, appSettings = appSettings,
                                onFavoriteMediaClick = onFavoriteMediaClick,
                                onActivityClick = onActivityClick, onLikeClick = onLikeClick, onDeleteClick = onDeleteClick
                            )
                            2 -> AniListStatsTab(
                                state = state, accentColor = accentColor,
                                statsMediaType = statsMediaType, statsSubTab = statsSubTab,
                                scoreDistType = scoreDistType, lengthDistType = lengthDistType,
                                releaseYearDistType = releaseYearDistType, startYearDistType = startYearDistType,
                                statSortType = statSortType,
                                onStatsMediaTypeChange = { statsMediaType = it },
                                onScoreDistTypeChange = { scoreDistType = it },
                                onLengthDistTypeChange = { lengthDistType = it },
                                onReleaseYearDistTypeChange = { releaseYearDistType = it },
                                onStartYearDistTypeChange = { startYearDistType = it },
                                onStatSortTypeChange = { statSortType = it },
                                onGenreClick = onGenreClick, onTagClick = onTagClick,
                                onFavoriteStaffClick = onFavoriteStaffClick,
                                onFavoriteStudioClick = onFavoriteStudioClick
                            )
                            3 -> AniListFavoritesTab(
                                state = state, accentColor = accentColor, isLandscape = isLandscape,
                                favoritesFilter = favoritesFilter, viewModel = viewModel,
                                onFavoriteMediaClick = onFavoriteMediaClick,
                                onFavoriteCharacterClick = onFavoriteCharacterClick,
                                onFavoriteStaffClick = onFavoriteStaffClick,
                                onFavoriteStudioClick = onFavoriteStudioClick,
                                onOpenFavoriteSheet = onOpenFavoriteSheet
                            )
                            4 -> AniListSocialTab(
                                state = state, accentColor = accentColor, isLandscape = isLandscape,
                                socialFilter = socialFilter, onUserProfileClick = onUserProfileClick
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
