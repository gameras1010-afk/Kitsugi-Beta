package com.kitsugi.animelist.ui.tv.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.screens.explore.ExplorePlatform
import com.kitsugi.animelist.ui.screens.explore.ExplorePlatformToggle
import com.kitsugi.animelist.ui.screens.explore.ExploreViewModel
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.tv.TvViewModel
import com.kitsugi.animelist.ui.tv.components.TvCatalogRow
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.asStable
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TvModernHomeContent(
    exploreViewModel: ExploreViewModel,
    tvViewModel: TvViewModel,
    showAdultContent: Boolean,
    shimmerOffsetState: State<Float>,
    rowStates: MutableMap<String, LazyListState>,
    rowEntryFocusRequesters: MutableMap<String, FocusRequester>,
    focusedItemKeyByRow: MutableMap<String, String>,
    onItemFocusedStable: (JikanSearchResult) -> Unit,
    onNavigateToDetail: (JikanSearchResult) -> Unit,
    onSeeAllClick: (String, List<JikanSearchResult>) -> Unit,
    listState: LazyListState,
    catalogRowScrollStates: MutableMap<String, Int>
) {
    val selectedPlatform = exploreViewModel.selectedPlatform
    val enrichedItems by tvViewModel.enrichedItems.collectAsStateWithLifecycle()
    val trailerUrls by tvViewModel.trailerUrls.collectAsStateWithLifecycle()

    val filteredTopAnime = exploreViewModel.topAnime.filter { showAdultContent || !it.isAdult }
    val filteredAiringAnime = exploreViewModel.airingAnime.filter { showAdultContent || !it.isAdult }
    val filteredUpcomingAnime = exploreViewModel.upcomingAnime.filter { showAdultContent || !it.isAdult }
    val filteredTopManga = exploreViewModel.topManga.filter { showAdultContent || !it.isAdult }
    val filteredPublishingManga = exploreViewModel.publishingManga.filter { showAdultContent || !it.isAdult }
    val filteredMovieAnime = exploreViewModel.movieAnime.filter { showAdultContent || !it.isAdult }
    val filteredSeasonalAnime = exploreViewModel.seasonalAnime.filter { showAdultContent || !it.isAdult }
    val filteredTrendingAnime = exploreViewModel.trendingAnime.filter { showAdultContent || !it.isAdult }

    val heroItems = if (selectedPlatform == ExplorePlatform.TMDB) {
        val itemsMix = mutableListOf<JikanSearchResult>()
        val topIt = filteredTopAnime.iterator()
        val airIt = filteredAiringAnime.iterator()
        val movIt = filteredMovieAnime.iterator()
        val addedKeys = mutableSetOf<String>()
        fun addIfUnique(item: JikanSearchResult) {
            val key = "${item.source}_${item.malId}"
            if (key !in addedKeys) { addedKeys.add(key); itemsMix.add(item) }
        }
        while (itemsMix.size < 8 && (topIt.hasNext() || airIt.hasNext() || movIt.hasNext())) {
            if (topIt.hasNext() && itemsMix.size < 8) addIfUnique(topIt.next())
            if (airIt.hasNext() && itemsMix.size < 8) addIfUnique(airIt.next())
            if (movIt.hasNext() && itemsMix.size < 8) addIfUnique(movIt.next())
        }
        itemsMix
    } else {
        filteredTopAnime.take(8)
    }

    var focusedItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var focusedItem by remember { mutableStateOf<JikanSearchResult?>(null) }
    var activeHeroItem by remember { mutableStateOf<JikanSearchResult?>(null) }

    LaunchedEffect(heroItems, focusedItemKey) {
        if (focusedItem == null) {
            focusedItem = if (focusedItemKey != null) {
                heroItems.firstOrNull { "${it.source}_${it.malId}" == focusedItemKey } ?: heroItems.firstOrNull()
            } else {
                heroItems.firstOrNull()
            }
        }
    }

    val latestFocusedItem = rememberUpdatedState(focusedItem)
    LaunchedEffect(focusedItem) {
        focusedItem?.let { item ->
            delay(300)
            if (latestFocusedItem.value?.malId == item.malId) {
                activeHeroItem = item
                tvViewModel.onItemFocus(item)
            }
        }
    }

    val verticalTvSpec = KitsugiScrollDefaults.rememberTvVerticalInsetSpec(
        insetDp = 260.dp,
        canScrollBackwardProvider = { listState.canScrollBackward }
    )

    var showHomeContentWithAnimation by rememberSaveable { mutableStateOf(false) }
    var hasShownInitialHomeContent by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!showHomeContentWithAnimation) {
            kotlinx.coroutines.yield()
            showHomeContentWithAnimation = true
        }
    }
    LaunchedEffect(showHomeContentWithAnimation) {
        if (showHomeContentWithAnimation) {
            hasShownInitialHomeContent = true
        }
    }

    val navigateToDetailWithRestoration: (JikanSearchResult, String) -> Unit =
        remember(tvViewModel, onNavigateToDetail, listState) {
            { item, rowKey ->
                focusedItemKeyByRow[rowKey] = "${item.source}_${item.malId}"
                tvViewModel.saveFocusState(
                    verticalScrollIndex = listState.firstVisibleItemIndex,
                    verticalScrollOffset = listState.firstVisibleItemScrollOffset,
                    focusedRowKey = rowKey,
                    focusedItemKeyByRow = focusedItemKeyByRow.toMap(),
                    catalogRowScrollStates = catalogRowScrollStates.toMap(),
                    focusedRowIndex = listState.firstVisibleItemIndex,
                    focusedItemIndex = 0
                )
                onNavigateToDetail(item)
            }
        }

    val onContinueSeriesClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "continue_series") } }
    val onContinueMoviesClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "continue_movies") } }
    val onPlannedSeriesClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "planned_series") } }
    val onPlannedMoviesClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "planned_movies") } }
    val onTrendAllClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "trend_all") } }
    val onTrendSeriesClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "trend_series") } }
    val onTrendMoviesClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "trend_movies") } }
    val onPopSeriesClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "pop_series") } }
    val onPopMoviesClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "pop_movies") } }
    val onTopMoviesClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "top_movies") } }
    val onTopSeriesClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "top_series") } }
    val onPopAnimeClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "pop_anime") } }
    val onAiringAnimeClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "airing_anime") } }
    val onUpcomingAnimeClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "upcoming_anime") } }
    val onPopMangaClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "pop_manga") } }
    val onPublishingMangaClick = remember(navigateToDetailWithRestoration) { { item: JikanSearchResult -> navigateToDetailWithRestoration(item, "publishing_manga") } }

    val onSeeAllTopAnime = remember(filteredTopAnime) { { onSeeAllClick("Trend Her Şey", filteredTopAnime) } }
    val onSeeAllAiringAnime = remember(filteredAiringAnime) { { onSeeAllClick("Trend Diziler", filteredAiringAnime) } }
    val onSeeAllMovieAnime = remember(filteredMovieAnime) { { onSeeAllClick("Trend Filmler", filteredMovieAnime) } }
    val onSeeAllTopManga = remember(filteredTopManga) { { onSeeAllClick("Popüler Diziler", filteredTopManga) } }
    val onSeeAllUpcomingAnime = remember(filteredUpcomingAnime) { { onSeeAllClick("Popüler Filmler", filteredUpcomingAnime) } }
    val onSeeAllPublishingManga = remember(filteredPublishingManga) { { onSeeAllClick("En Yüksek Puanlı Filmler", filteredPublishingManga) } }
    val onSeeAllSeasonalAnime = remember(filteredSeasonalAnime) { { onSeeAllClick("En Yüksek Puanlı Diziler", filteredSeasonalAnime) } }
    val onSeeAllTrendingAnime = remember(filteredTrendingAnime) { { onSeeAllClick("Trend Anime", filteredTrendingAnime) } }
    val onSeeAllSimklContinueSeries = remember(exploreViewModel.simklContinueSeries) { { onSeeAllClick("İzlemeye Devam Et — Diziler", exploreViewModel.simklContinueSeries) } }
    val onSeeAllSimklContinueMovies = remember(exploreViewModel.simklContinueMovies) { { onSeeAllClick("İzlemeye Devam Et — Filmler", exploreViewModel.simklContinueMovies) } }
    val onSeeAllSimklPlannedSeries = remember(exploreViewModel.simklPlannedSeries) { { onSeeAllClick("Planladıklarım — Diziler", exploreViewModel.simklPlannedSeries) } }
    val onSeeAllSimklPlannedMovies = remember(exploreViewModel.simklPlannedMovies) { { onSeeAllClick("Planladıklarım — Filmler", exploreViewModel.simklPlannedMovies) } }

    val onSeeAllPopAnime = remember(filteredTopAnime) { { onSeeAllClick("Popüler Anime", filteredTopAnime) } }
    val onSeeAllAiringAnimeMAL = remember(filteredAiringAnime) { { onSeeAllClick("Yayındaki Anime", filteredAiringAnime) } }
    val onSeeAllUpcomingAnimeMAL = remember(filteredUpcomingAnime) { { onSeeAllClick("Yaklaşan Anime", filteredUpcomingAnime) } }
    val onSeeAllPopManga = remember(filteredTopManga) { { onSeeAllClick("Popüler Manga", filteredTopManga) } }
    val onSeeAllPublishingMangaMAL = remember(filteredPublishingManga) { { onSeeAllClick("Yayındaki Manga", filteredPublishingManga) } }

    val onItemFocusedCallback = remember {
        { item: JikanSearchResult ->
            focusedItem = item
            focusedItemKey = "${item.source}_${item.malId}"
            onItemFocusedStable(item)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val currentHero = activeHeroItem?.let { enrichedItems[it.malId] ?: it } ?: heroItems.firstOrNull()
        TvModernHero(
            item = currentHero,
            trailerUrl = currentHero?.let { trailerUrls[it.malId] }
        )

        AnimatedVisibility(
            visible = showHomeContentWithAnimation,
            enter = if (hasShownInitialHomeContent) {
                androidx.compose.animation.EnterTransition.None
            } else {
                fadeIn(animationSpec = tween(320)) +
                    slideInVertically(
                        initialOffsetY = { it / 24 },
                        animationSpec = tween(320)
                    )
            }
        ) {
            CompositionLocalProvider(LocalBringIntoViewSpec provides verticalTvSpec) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRestorer()
                        .dpadVerticalFastScroll(
                            scrollableState = listState,
                            resolveVerticalLanding = { sign ->
                                val visibleItems = listState.layoutInfo.visibleItemsInfo
                                val lastIdx = listState.layoutInfo.totalItemsCount - 1
                                val lastItemAtBottom = lastIdx >= 0 &&
                                    visibleItems.lastOrNull { it.index == lastIdx }?.let {
                                        it.offset + it.size <= listState.layoutInfo.viewportEndOffset
                                    } == true
                                val target = when {
                                    lastItemAtBottom -> visibleItems.lastOrNull { it.index == lastIdx }
                                    sign < 0 -> visibleItems.firstOrNull { it.offset > -it.size / 2 } ?: visibleItems.firstOrNull()
                                    else -> visibleItems.firstOrNull { it.offset >= 0 } ?: visibleItems.firstOrNull()
                                }
                                val key = target?.key as? String
                                val rowKey = key?.removePrefix("row_")
                                val requester = if (rowKey != null) rowEntryFocusRequesters[rowKey] else null
                                runCatching { requester?.requestFocus() }
                            }
                        ),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item(key = "hero_spacer", contentType = "spacer") {
                        Spacer(modifier = Modifier.height(340.dp))
                    }

                    item(key = "platform_header", contentType = "header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = KitsugiTvTokens.Spacing.screenHorizontal,
                                    end = KitsugiTvTokens.Spacing.screenHorizontal,
                                    top = KitsugiTvTokens.Spacing.sm,
                                    bottom = KitsugiTvTokens.Spacing.rowGap
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Keşfet",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            ExplorePlatformToggle(
                                selectedPlatform = selectedPlatform,
                                onPlatformSelected = { platform -> exploreViewModel.selectPlatform(platform) },
                                modifier = Modifier.width(320.dp)
                            )
                        }
                    }

                    if (selectedPlatform == ExplorePlatform.TMDB) {
                        if (exploreViewModel.simklContinueSeries.isNotEmpty()) {
                            item(key = "row_continue_series", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "İzlemeye Devam Et — Diziler",
                                    items = exploreViewModel.simklContinueSeries.asStable(),
                                    listState = rowStates.getOrPut("continue_series") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("continue_series") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["continue_series"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onContinueSeriesClick,
                                    onSeeAllClick = onSeeAllSimklContinueSeries,
                                    isContinueWatching = true,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                        if (exploreViewModel.simklContinueMovies.isNotEmpty()) {
                            item(key = "row_continue_movies", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "İzlemeye Devam Et — Filmler",
                                    items = exploreViewModel.simklContinueMovies.asStable(),
                                    listState = rowStates.getOrPut("continue_movies") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("continue_movies") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["continue_movies"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onContinueMoviesClick,
                                    onSeeAllClick = onSeeAllSimklContinueMovies,
                                    isContinueWatching = true,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                        if (exploreViewModel.simklPlannedSeries.isNotEmpty()) {
                            item(key = "row_planned_series", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "Planladıklarım — Diziler",
                                    items = exploreViewModel.simklPlannedSeries.asStable(),
                                    listState = rowStates.getOrPut("planned_series") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("planned_series") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["planned_series"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onPlannedSeriesClick,
                                    onSeeAllClick = onSeeAllSimklPlannedSeries,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                        if (exploreViewModel.simklPlannedMovies.isNotEmpty()) {
                            item(key = "row_planned_movies", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "Planladıklarım — Filmler",
                                    items = exploreViewModel.simklPlannedMovies.asStable(),
                                    listState = rowStates.getOrPut("planned_movies") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("planned_movies") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["planned_movies"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onPlannedMoviesClick,
                                    onSeeAllClick = onSeeAllSimklPlannedMovies,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                        if (filteredTopAnime.isNotEmpty()) {
                            item(key = "row_trend_all", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "Trend Her Şey",
                                    items = filteredTopAnime.asStable(),
                                    listState = rowStates.getOrPut("trend_all") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("trend_all") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["trend_all"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onTrendAllClick,
                                    onSeeAllClick = onSeeAllTopAnime,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                        if (filteredAiringAnime.isNotEmpty()) {
                            item(key = "row_trend_series", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "Trend Diziler",
                                    items = filteredAiringAnime.asStable(),
                                    listState = rowStates.getOrPut("trend_series") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("trend_series") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["trend_series"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onTrendSeriesClick,
                                    onSeeAllClick = onSeeAllAiringAnime,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                        if (filteredMovieAnime.isNotEmpty()) {
                            item(key = "row_trend_movies", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "Trend Filmler",
                                    items = filteredMovieAnime.asStable(),
                                    listState = rowStates.getOrPut("trend_movies") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("trend_movies") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["trend_movies"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onTrendMoviesClick,
                                    onSeeAllClick = onSeeAllMovieAnime,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                        if (filteredTopManga.isNotEmpty()) {
                            item(key = "row_pop_series", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "Popüler Diziler",
                                    items = filteredTopManga.asStable(),
                                    listState = rowStates.getOrPut("pop_series") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("pop_series") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["pop_series"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onPopSeriesClick,
                                    onSeeAllClick = onSeeAllTopManga,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                        if (filteredUpcomingAnime.isNotEmpty()) {
                            item(key = "row_pop_movies", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "Popüler Filmler",
                                    items = filteredUpcomingAnime.asStable(),
                                    listState = rowStates.getOrPut("pop_movies") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("pop_movies") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["pop_movies"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onPopMoviesClick,
                                    onSeeAllClick = onSeeAllUpcomingAnime,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                        if (filteredPublishingManga.isNotEmpty()) {
                            item(key = "row_top_movies", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "En Yüksek Puanlı Filmler",
                                    items = filteredPublishingManga.asStable(),
                                    listState = rowStates.getOrPut("top_movies") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("top_movies") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["top_movies"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onTopMoviesClick,
                                    onSeeAllClick = onSeeAllPublishingManga,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                        if (filteredSeasonalAnime.isNotEmpty()) {
                            item(key = "row_top_series", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "En Yüksek Puanlı Diziler",
                                    items = filteredSeasonalAnime.asStable(),
                                    listState = rowStates.getOrPut("top_series") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("top_series") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["top_series"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onTopSeriesClick,
                                    onSeeAllClick = onSeeAllSeasonalAnime,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                    } else {
                        if (filteredTopAnime.isNotEmpty()) {
                            item(key = "row_pop_anime", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "Popüler Anime",
                                    items = filteredTopAnime.asStable(),
                                    listState = rowStates.getOrPut("pop_anime") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("pop_anime") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["pop_anime"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onPopAnimeClick,
                                    onSeeAllClick = onSeeAllPopAnime,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                        if (filteredAiringAnime.isNotEmpty()) {
                            item(key = "row_airing_anime", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "Yayındaki Anime",
                                    items = filteredAiringAnime.asStable(),
                                    listState = rowStates.getOrPut("airing_anime") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("airing_anime") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["airing_anime"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onAiringAnimeClick,
                                    onSeeAllClick = onSeeAllAiringAnimeMAL,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                        if (filteredUpcomingAnime.isNotEmpty()) {
                            item(key = "row_upcoming_anime", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "Yaklaşan Anime",
                                    items = filteredUpcomingAnime.asStable(),
                                    listState = rowStates.getOrPut("upcoming_anime") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("upcoming_anime") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["upcoming_anime"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onUpcomingAnimeClick,
                                    onSeeAllClick = onSeeAllUpcomingAnimeMAL,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                        if (filteredTopManga.isNotEmpty()) {
                            item(key = "row_pop_manga", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "Popüler Manga",
                                    items = filteredTopManga.asStable(),
                                    listState = rowStates.getOrPut("pop_manga") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("pop_manga") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["pop_manga"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onPopMangaClick,
                                    onSeeAllClick = onSeeAllPopManga,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                        if (filteredPublishingManga.isNotEmpty()) {
                            item(key = "row_publishing_manga", contentType = "content_row") {
                                TvCatalogRow(
                                    title = "Yayındaki Manga",
                                    items = filteredPublishingManga.asStable(),
                                    listState = rowStates.getOrPut("publishing_manga") { LazyListState() },
                                    entryFocusRequester = rowEntryFocusRequesters.getOrPut("publishing_manga") { FocusRequester() },
                                    savedFocusedItemKey = focusedItemKeyByRow["publishing_manga"],
                                    onItemFocused = onItemFocusedCallback,
                                    onItemClick = onPublishingMangaClick,
                                    onSeeAllClick = onSeeAllPublishingMangaMAL,
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
