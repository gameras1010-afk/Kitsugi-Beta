@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.rememberCoroutineScope
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.KitsugiCharacterDetail
import com.kitsugi.animelist.data.local.TranslationManager
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator
import com.kitsugi.animelist.ui.components.KitsugiMarkdownText
import com.kitsugi.animelist.data.remote.DetailCache
import com.kitsugi.animelist.ui.components.KitsugiCinematicLoadingScreen
import com.kitsugi.animelist.ui.components.KitsugiImageGalleryDialog
import com.kitsugi.animelist.ui.components.KitsugiPageEnter
import com.kitsugi.animelist.utils.copyOnDoubleTap
import com.kitsugi.animelist.utils.toFriendlySourceLabel
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import com.kitsugi.animelist.data.remote.GalleryItem
import com.kitsugi.animelist.data.remote.GalleryCategory
import androidx.compose.ui.focus.focusRequester

sealed interface CharacterDetailState {
    object Loading : CharacterDetailState
    data class Error(val message: String) : CharacterDetailState
    data class Success(val detail: KitsugiCharacterDetail) : CharacterDetailState
}

@Composable
fun CharacterDetailPage(
    characterId: Int,
    source: String,
    onBackClick: () -> Unit,
    onMediaClick: (mediaId: Int, mediaType: String, mediaSource: String) -> Unit,
    onStaffClick: (staffId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onCharacterClick: (characterId: Int, characterSource: String, name: String?, imageUrl: String?) -> Unit,
    name: String? = null,
    imageUrl: String? = null,
    titleLanguage: String = "ROMAJI",
    preferredTranslator: String = "DEFAULT"
) {
    val accentColor = LocalKitsugiAccent.current
    val context = LocalContext.current

    // Obtain ViewModel
    val viewModel: CharacterDetailViewModel = viewModel(key = "character_${source}_${characterId}")

    // Load character in ViewModel
    LaunchedEffect(characterId, source) {
        viewModel.loadCharacter(characterId, source, name)
    }

    // Collect states from ViewModel
    val state by viewModel.state.collectAsState()
    val translatedBio by viewModel.translatedBio.collectAsState()
    val isFavourite by viewModel.isFavourite.collectAsState()
    val isAniListSource = source.lowercase() == "anilist"
    val isAniListConnected = remember { com.kitsugi.animelist.data.auth.ExternalAuthManager.getAniListToken(context) != null }
    val showFavouriteButton = isAniListSource || isAniListConnected

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
    ) {
        when (val currentState = state) {
            is CharacterDetailState.Loading -> {
                KitsugiCinematicLoadingScreen(
                    title = name ?: "Karakter Yükleniyor...",
                    imageUrl = imageUrl,
                    onBackClick = onBackClick
                )
            }
            is CharacterDetailState.Error -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Geri",
                            tint = KitsugiColors.TextPrimary
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentState.message,
                            color = KitsugiColors.AccentRed,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = {
                                viewModel.retry()
                            }
                        ) {
                            Text("Yeniden Dene", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            is CharacterDetailState.Success -> {
                val detail = currentState.detail
                val isRefreshing by viewModel.isRefreshing.collectAsState()
                val pullRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.forceRefresh() },
                    modifier = Modifier.fillMaxSize(),
                    state = pullRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullRefreshState,
                            isRefreshing = isRefreshing,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = KitsugiColors.Surface,
                            color = accentColor
                        )
                    }
                ) {
                    val listState = rememberLazyListState()
                val showFloatingHeader = listState.firstVisibleItemIndex >= 1
                val tabs = if (source.lowercase() == "tmdb") {
                    listOf("Hakkında", "Yapımlar")
                } else {
                    listOf("Hakkında", "Yapımlar", "Seslendirenler")
                }
                @OptIn(ExperimentalFoundationApi::class)
                val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
                val selectedTab = pagerState.currentPage
                val coroutineScope = rememberCoroutineScope()
                val isTv = LocalIsTv.current
                val tabListState = rememberLazyListState()
                var activeGalleryItems by remember { mutableStateOf<List<GalleryItem>>(emptyList()) }
                var activeGalleryIndex by remember { mutableStateOf(0) }
                val galleryItems by viewModel.galleryItems.collectAsState()

                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                // TV odak highway
                val leftPanelFocusRequester = remember { FocusRequester() }
                val tabBarFocusRequester = remember { FocusRequester() }

                if (isLandscape) {
                    val configuration = LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp
                    val leftPanelWeight = when {
                        screenWidth >= 1200 -> 0.28f
                        screenWidth >= 840  -> 0.32f
                        else                -> 0.38f
                    }
                    val rightPanelWeight = 1f - leftPanelWeight
                    // ── LANDSCAPE: Sol hero paneli + Sağ tab paneli ──
                    KitsugiPageEnter {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Sol panel: CharacterDetailLeftPanel
                                CharacterDetailLeftPanel(
                                    detail = detail,
                                    source = source,
                                    characterId = characterId,
                                    accentColor = accentColor,
                                    isFavourite = isFavourite,
                                    showFavouriteButton = showFavouriteButton,
                                    galleryItems = galleryItems,
                                    leftPanelFocusRequester = leftPanelFocusRequester,
                                    tabBarFocusRequester = tabBarFocusRequester,
                                    onBackClick = onBackClick,
                                    onToggleFavourite = { viewModel.toggleFavourite() },
                                    onGalleryClick = { items, idx ->
                                        activeGalleryItems = items
                                        activeGalleryIndex = idx
                                    },
                                    modifier = Modifier.weight(leftPanelWeight)
                                )

                                // Sağ panel: Tab seçici + içerik
                                Column(
                                    modifier = Modifier
                                        .weight(rightPanelWeight)
                                        .fillMaxSize()
                                ) {
                                    // Tab row
                                    LaunchedEffect(selectedTab) {
                                        tabListState.animateScrollToItem(selectedTab)
                                    }
                                    LazyRow(
                                        state = tabListState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(tabBarFocusRequester)
                                            .focusProperties { left = leftPanelFocusRequester }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(tabs.size) { index ->
                                            val title = tabs[index]
                                            val isSelected = selectedTab == index
                                            val bgColor = if (isSelected) accentColor else KitsugiColors.Surface
                                            val textColor = if (isSelected) KitsugiColors.Background else KitsugiColors.TextSecondary
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(bgColor)
                                                    .tvClickable(shape = RoundedCornerShape(999.dp)) { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = title,
                                                    color = textColor,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    // Tab content - swipeable HorizontalPager
                                    @OptIn(ExperimentalFoundationApi::class)
                                    HorizontalPager(
                                        state = pagerState,
                                        userScrollEnabled = !isTv,
                                        modifier = Modifier.fillMaxSize()
                                    ) { page ->
                                    val currentTab = tabs.getOrNull(page)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        when (currentTab) {
                                            "Hakkında" -> {
                                                CharacterAboutTabContent(
                                                    detail = detail,
                                                    translatedBio = translatedBio,
                                                    preferredTranslator = preferredTranslator,
                                                    accentColor = accentColor,
                                                    onGalleryClick = { items, idx ->
                                                        activeGalleryItems = items
                                                        activeGalleryIndex = idx
                                                    }
                                                )
                                            }
                                            "Yapımlar" -> {
                                                CharacterAppearancesTabContent(
                                                    detail = detail,
                                                    titleLanguage = titleLanguage,
                                                    onMediaClick = onMediaClick
                                                )
                                            }
                                            "Seslendirenler" -> {
                                                CharacterVoiceActorsTabContent(
                                                    detail = detail,
                                                    onStaffClick = onStaffClick
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(32.dp))
                                    }
                                    } // end HorizontalPager
                                }
                            }

                            if (activeGalleryItems.isNotEmpty()) {
                                KitsugiImageGalleryDialog(
                                    galleryItems = activeGalleryItems,
                                    initialIndex = activeGalleryIndex,
                                    title = detail.name,
                                    onDismiss = { activeGalleryItems = emptyList() }
                                )
                            }
                        }
                    }
                } else {
                    // ── PORTRAIT: Mevcut LazyColumn düzeni ──
                KitsugiPageEnter {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Hero image section
                            item(key = "hero") {
                                CharacterPortraitHeroSection(
                                    detail = detail,
                                    source = source,
                                    characterId = characterId,
                                    accentColor = accentColor,
                                    isFavourite = isFavourite,
                                    isAniListSource = isAniListSource,
                                    galleryItems = galleryItems,
                                    onBackClick = onBackClick,
                                    onToggleFavourite = { viewModel.toggleFavourite() },
                                    onGalleryOpen = { items, idx ->
                                        activeGalleryItems = items
                                        activeGalleryIndex = idx
                                    }
                                )
                            }
                            // Tabs sticky header
                            stickyHeader(key = "tabs") {
                                CharacterDetailStickyHeader(
                                    detail = detail,
                                    source = source,
                                    characterId = characterId,
                                    tabs = tabs,
                                    selectedTab = selectedTab,
                                    showFloatingHeader = showFloatingHeader,
                                    isFavourite = isFavourite,
                                    showFavouriteButton = showFavouriteButton,
                                    galleryItems = galleryItems,
                                    tabListState = tabListState,
                                    accentColor = accentColor,
                                    onBackClick = onBackClick,
                                    onToggleFavourite = { viewModel.toggleFavourite() },
                                    onGalleryOpen = { items, idx ->
                                        activeGalleryItems = items
                                        activeGalleryIndex = idx
                                    },
                                    onTabSelected = { coroutineScope.launch { pagerState.animateScrollToPage(it) } }
                                )
                            }

                            // Tab contents - swipeable HorizontalPager
                            item(key = "content") {
                                val pageHeights = remember { androidx.compose.runtime.mutableStateMapOf<Int, Int>() }
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val currentPage = pagerState.currentPage
                                val currentPageOffset = pagerState.currentPageOffsetFraction
                                val targetPage = if (currentPageOffset > 0f) currentPage + 1 else if (currentPageOffset < 0f) currentPage - 1 else currentPage

                                val currentHeightPx = pageHeights[currentPage] ?: 0
                                val targetHeightPx = pageHeights[targetPage] ?: currentHeightPx

                                val interpolatedHeightDp = remember(currentHeightPx, targetHeightPx, currentPageOffset) {
                                    val heightPx = if (currentHeightPx > 0 && targetHeightPx > 0) {
                                        currentHeightPx + (targetHeightPx - currentHeightPx) * kotlin.math.abs(currentPageOffset)
                                    } else if (currentHeightPx > 0) {
                                        currentHeightPx.toFloat()
                                    } else {
                                        0f
                                    }
                                    if (heightPx > 0f) with(density) { heightPx.toDp() } else null
                                }

                                val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp

                                @OptIn(ExperimentalFoundationApi::class)
                                HorizontalPager(
                                    state = pagerState,
                                    userScrollEnabled = !isTv,
                                    beyondViewportPageCount = 1,
                                    pageSpacing = 12.dp,
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .layout { measurable, constraints ->
                                            val minPagerHeightPx = with(density) { (screenHeightDp - 64).dp.roundToPx() }
                                            val placeable = measurable.measure(
                                                constraints.copy(
                                                    minHeight = minPagerHeightPx,
                                                    maxHeight = androidx.compose.ui.unit.Constraints.Infinity
                                                )
                                            )
                                            val height = interpolatedHeightDp?.roundToPx()?.coerceAtLeast(minPagerHeightPx) ?: placeable.height
                                            layout(placeable.width, height) {
                                                placeable.placeRelative(0, 0)
                                            }
                                        }
                                        .clipToBounds()
                                ) { page ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 600.dp)
                                    ) {
                                        val currentTab = tabs.getOrNull(page)
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp)
                                                .onGloballyPositioned { coordinates ->
                                                    pageHeights[page] = coordinates.size.height
                                                }
                                        ) {
                                            when (currentTab) {
                                                "Hakkında" -> {
                                                    CharacterAboutTabContent(
                                                        detail = detail,
                                                        translatedBio = translatedBio,
                                                        preferredTranslator = preferredTranslator,
                                                        accentColor = accentColor,
                                                        onGalleryClick = { items, idx ->
                                                            activeGalleryItems = items
                                                            activeGalleryIndex = idx
                                                        }
                                                    )
                                                }
                                                "Yapımlar" -> {
                                                    CharacterAppearancesTabContent(
                                                        detail = detail,
                                                        titleLanguage = titleLanguage,
                                                        onMediaClick = onMediaClick
                                                    )
                                                }
                                                "Seslendirenler" -> {
                                                    CharacterVoiceActorsTabContent(
                                                        detail = detail,
                                                        onStaffClick = onStaffClick
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(50.dp))
                                        }
                                    }
                                } // end HorizontalPager
                            }
                        }

                        if (activeGalleryItems.isNotEmpty()) {
                            KitsugiImageGalleryDialog(
                                galleryItems = activeGalleryItems,
                                initialIndex = activeGalleryIndex,
                                title = detail.name,
                                onDismiss = { activeGalleryItems = emptyList() }
                            )
                        }




                    }
                }
                } // end else (portrait)
                } // end PullToRefreshBox
            }
        }
    }
}


