@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kitsugi.animelist.ui.screens.detail

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.LocalIsTvDevice
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlinx.coroutines.launch

/**
 * Shared scaffold for both [ApiResultDetailPage] and [MediaEntryDetailPage].
 *
 * Handles:
 * - Loading / error guard
 * - PullToRefresh wrapper
 * - Portrait ↔ Landscape layout switch
 * - Left panel + right panel composition in landscape
 * - Sticky tab bar + HorizontalPager in portrait
 * - TV D-pad focus routing
 */
@Composable
fun DetailPageScaffold(
    title: String,
    isLoading: Boolean,
    isError: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    tabs: List<String>,
    pagerState: PagerState,
    listState: LazyListState,
    tabListState: LazyListState,
    loadingScreen: @Composable () -> Unit,
    errorScreen: @Composable () -> Unit,
    leftPanel: @Composable (
        leftPanelFocusRequester: FocusRequester,
        tabBarFocusRequester: FocusRequester
    ) -> Unit,
    floatingHeaderActions: @Composable RowScope.() -> Unit = {},
    portraitTopItems: (LazyListScope.(
        listState: LazyListState,
        leftPanelFocusRequester: FocusRequester,
        tabBarFocusRequester: FocusRequester
    ) -> Unit)? = null,
    pageContent: @Composable (page: Int) -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val isTv = LocalIsTv.current
    val isTvDevice = LocalIsTvDevice.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val leftPanelFocusRequester = remember { FocusRequester() }
    val tabBarFocusRequester = remember { FocusRequester() }
    val selectedTab = pagerState.currentPage

    when {
        isLoading -> loadingScreen()
        isError   -> errorScreen()
        else -> {
            val pullRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
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
                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (isLandscape) {
                    val configuration = LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp
                    val leftPanelWeight = when {
                        screenWidth >= 1200 -> 0.28f
                        screenWidth >= 840  -> 0.32f
                        else                -> 0.38f
                    }
                    val rightPanelWeight = 1f - leftPanelWeight

                    Box(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // ── Sol Panel ──
                            val leftScrollState = rememberScrollState()
                            val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
                            CompositionLocalProvider(
                                LocalBringIntoViewSpec provides if (isTvDevice) tvSpec else LocalBringIntoViewSpec.current
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(leftPanelWeight)
                                        .fillMaxSize()
                                        .then(if (isTvDevice) Modifier.dpadVerticalFastScroll(leftScrollState) else Modifier)
                                        .verticalScroll(leftScrollState)
                                ) {
                                    leftPanel(leftPanelFocusRequester, tabBarFocusRequester)
                                }
                            }

                            // ── Sağ Panel: Tab bar + Pager ──
                            Column(
                                modifier = Modifier
                                    .weight(rightPanelWeight)
                                    .fillMaxSize()
                            ) {
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
                                    itemsIndexed(tabs) { index, tabTitle ->
                                        DetailTabChip(
                                            title = tabTitle,
                                            isSelected = selectedTab == index,
                                            accentColor = accentColor,
                                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(KitsugiColors.Background)
                                ) {
                                    HorizontalPager(
                                        state = pagerState,
                                        userScrollEnabled = !isTvDevice,
                                        beyondViewportPageCount = 1,
                                        pageSpacing = 12.dp,
                                        modifier = Modifier.fillMaxSize().clipToBounds()
                                    ) { page ->
                                        val pageScrollState = rememberScrollState()
                                        val pageTvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
                                        CompositionLocalProvider(
                                            LocalBringIntoViewSpec provides if (isTvDevice) pageTvSpec else LocalBringIntoViewSpec.current
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .then(if (isTvDevice) Modifier.dpadVerticalFastScroll(pageScrollState) else Modifier)
                                                    .verticalScroll(pageScrollState)
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                            ) {
                                                pageContent(page)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ── PORTRAIT ──
                    Box(modifier = Modifier.fillMaxSize()) {
                        val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides if (isTvDevice) tvSpec else LocalBringIntoViewSpec.current
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(KitsugiColors.Background)
                                    .then(if (isTvDevice) Modifier.dpadVerticalFastScroll(listState) else Modifier)
                            ) {
                                // Caller-supplied top items (hero, actions, etc.)
                                portraitTopItems?.invoke(this, listState, leftPanelFocusRequester, tabBarFocusRequester)

                                // ── Sekme Barı — STICKY HEADER ──
                                stickyHeader(key = "tabs") {
                                    val stickyAccent = LocalKitsugiAccent.current
                                    LaunchedEffect(selectedTab) {
                                        if (listState.firstVisibleItemIndex > 2) listState.scrollToItem(2)
                                        val itemInfo = tabListState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.index == selectedTab }
                                        if (itemInfo != null) {
                                            val centerOffset = (tabListState.layoutInfo.viewportEndOffset - itemInfo.size) / 2
                                            tabListState.animateScrollToItem(selectedTab, -centerOffset)
                                        } else {
                                            tabListState.animateScrollToItem(selectedTab)
                                        }
                                    }

                                    val showFloatingHeader = listState.firstVisibleItemIndex >= 2
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(KitsugiColors.Surface.copy(alpha = 0.97f))
                                    ) {
                                        AnimatedVisibility(
                                            visible = showFloatingHeader,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(64.dp)
                                                    .background(KitsugiColors.Surface.copy(alpha = 0.92f))
                                                    .padding(horizontal = 8.dp)
                                            ) {
                                                IconButton(onClick = onBackClick) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                        contentDescription = "Geri",
                                                        tint = KitsugiColors.TextPrimary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = title,
                                                    color = KitsugiColors.TextPrimary,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Black,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                floatingHeaderActions()
                                            }
                                        }

                                        LazyRow(
                                            state = tabListState,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            itemsIndexed(tabs) { index, tabTitle ->
                                                DetailTabChip(
                                                    title = tabTitle,
                                                    isSelected = selectedTab == index,
                                                    accentColor = stickyAccent,
                                                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(KitsugiColors.SurfaceSoft)
                                        )
                                    }
                                }

                                // ── Sekme İçerikleri ──
                                item(key = "content") {
                                    val pageHeights = remember { mutableStateMapOf<Int, Int>() }
                                    val currentPage = pagerState.currentPage
                                    val currentPageOffset = pagerState.currentPageOffsetFraction
                                    val targetPage = when {
                                        currentPageOffset > 0f -> currentPage + 1
                                        currentPageOffset < 0f -> currentPage - 1
                                        else -> currentPage
                                    }
                                    val currentHeightPx = pageHeights[currentPage] ?: 0
                                    val targetHeightPx = pageHeights[targetPage] ?: currentHeightPx
                                    val interpolatedHeightDp = remember(currentHeightPx, targetHeightPx, currentPageOffset) {
                                        val heightPx = when {
                                            currentHeightPx > 0 && targetHeightPx > 0 ->
                                                currentHeightPx + (targetHeightPx - currentHeightPx) * kotlin.math.abs(currentPageOffset)
                                            currentHeightPx > 0 -> currentHeightPx.toFloat()
                                            else -> 0f
                                        }
                                        if (heightPx > 0f) with(density) { heightPx.toDp() } else null
                                    }
                                    val screenHeightDp = LocalConfiguration.current.screenHeightDp

                                    HorizontalPager(
                                        state = pagerState,
                                        userScrollEnabled = !isTv,
                                        beyondViewportPageCount = 1,
                                        contentPadding = PaddingValues(horizontal = 16.dp),
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
                                                val height = interpolatedHeightDp?.roundToPx()
                                                    ?.coerceAtLeast(minPagerHeightPx) ?: placeable.height
                                                layout(placeable.width, height) { placeable.placeRelative(0, 0) }
                                            }
                                            .clipToBounds()
                                    ) { page ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 600.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .onGloballyPositioned { pageHeights[page] = it.size.height }
                                            ) {
                                                pageContent(page)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(90.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Reusable tab chip used in both landscape and portrait tab bars. */
@Composable
private fun DetailTabChip(
    title: String,
    isSelected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) accentColor else KitsugiColors.Background
    val textColor = if (isSelected) KitsugiColors.Background else KitsugiColors.TextSecondary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .tvClickable(shape = RoundedCornerShape(999.dp), onClick = onClick)
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
