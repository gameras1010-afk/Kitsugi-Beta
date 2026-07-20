package com.kitsugi.animelist.ui.tv.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.screens.explore.ExplorePlatform
import com.kitsugi.animelist.ui.screens.explore.ExploreViewModel
import com.kitsugi.animelist.ui.tv.TvViewModel
import com.kitsugi.animelist.ui.tv.components.TvErrorScreen
import com.kitsugi.animelist.ui.tv.components.TvLoadingScreen
import com.kitsugi.animelist.ui.utils.rememberPlaceholderShimmerOffsetState
import kotlinx.coroutines.delay

import com.kitsugi.animelist.ui.tv.focus.TvFocusRestoration.safeRequestFocus

private const val HOME_STABLE_GATE_TIMEOUT_MS = 5_000L

@OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TvHomeScreen(
    exploreViewModel: ExploreViewModel = viewModel(),
    currentEntries: List<com.kitsugi.animelist.model.MediaEntry> = emptyList(),
    showAdultContent: Boolean = false,
    selectedHomeLayoutId: String = "classic",
    onNavigateToDetail: (JikanSearchResult) -> Unit = {},
    onSeeAllClick: (String, List<JikanSearchResult>) -> Unit = { _, _ -> }
) {
    val selectedPlatform = exploreViewModel.selectedPlatform
    val isLoading = exploreViewModel.isLoading
    val errorMessage = exploreViewModel.errorMessage

    val tvViewModel: TvViewModel = viewModel()
    val focusState by tvViewModel.focusState.collectAsStateWithLifecycle()

    val filteredTopAnime = exploreViewModel.topAnime.filter { showAdultContent || !it.isAdult }
    val filteredAiringAnime = exploreViewModel.airingAnime.filter { showAdultContent || !it.isAdult }
    val filteredUpcomingAnime = exploreViewModel.upcomingAnime.filter { showAdultContent || !it.isAdult }
    val filteredTopManga = exploreViewModel.topManga.filter { showAdultContent || !it.isAdult }
    val filteredPublishingManga = exploreViewModel.publishingManga.filter { showAdultContent || !it.isAdult }
    val filteredMovieAnime = exploreViewModel.movieAnime.filter { showAdultContent || !it.isAdult }
    val filteredSeasonalAnime = exploreViewModel.seasonalAnime.filter { showAdultContent || !it.isAdult }

    val isEmpty = when (selectedPlatform) {
        ExplorePlatform.TMDB -> {
            filteredTopAnime.isEmpty() &&
            filteredAiringAnime.isEmpty() &&
            filteredMovieAnime.isEmpty() &&
            filteredTopManga.isEmpty() &&
            filteredUpcomingAnime.isEmpty() &&
            filteredPublishingManga.isEmpty() &&
            filteredSeasonalAnime.isEmpty() &&
            exploreViewModel.simklContinueMovies.isEmpty() &&
            exploreViewModel.simklPlannedMovies.isEmpty() &&
            exploreViewModel.simklContinueSeries.isEmpty() &&
            exploreViewModel.simklPlannedSeries.isEmpty()
        }
        else -> {
            filteredTopAnime.isEmpty() &&
            filteredAiringAnime.isEmpty() &&
            filteredUpcomingAnime.isEmpty() &&
            filteredTopManga.isEmpty() &&
            filteredPublishingManga.isEmpty()
        }
    }

    var homeStableGateReleased by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isLoading, isEmpty) {
        if (!homeStableGateReleased && !isLoading && !isEmpty) {
            homeStableGateReleased = true
        }
    }
    LaunchedEffect(Unit) {
        delay(HOME_STABLE_GATE_TIMEOUT_MS)
        if (!homeStableGateReleased) homeStableGateReleased = true
    }

    if (!homeStableGateReleased) {
        if (errorMessage != null && isEmpty) {
            TvErrorScreen(message = errorMessage, onRetry = { exploreViewModel.loadData(forceRefresh = true) })
        } else {
            TvLoadingScreen()
        }
        return
    }

    errorMessage?.let { error ->
        if (isEmpty) {
            TvErrorScreen(message = error, onRetry = { exploreViewModel.loadData(forceRefresh = true) })
            return
        }
    }

    val shimmerOffsetState = rememberPlaceholderShimmerOffsetState()

    // Shared row states and focus management
    val listState = rememberLazyListState()
    val focusedItemKeyByRow = remember { mutableStateMapOf<String, String>() }
    val catalogRowScrollStates = remember { mutableStateMapOf<String, Int>() }
    val rowStates = remember {
        val keys = listOf(
            "featured", "continue_series", "continue_movies", "planned_series", "planned_movies",
            "trend_all", "trend_series", "trend_movies", "pop_series", "pop_movies", "top_movies",
            "top_series", "pop_anime", "airing_anime", "upcoming_anime", "pop_manga", "publishing_manga"
        )
        mutableMapOf<String, LazyListState>().apply {
            keys.forEach { key ->
                put(
                    key,
                    LazyListState(
                        prefetchStrategy = androidx.compose.foundation.lazy.LazyListPrefetchStrategy(nestedPrefetchItemCount = 2)
                    )
                )
            }
        }
    }
    val rowEntryFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    // State Restoration
    LaunchedEffect(homeStableGateReleased, focusState.hasSavedFocus) {
        if (homeStableGateReleased) {
            if (focusState.hasSavedFocus) {
                listState.scrollToItem(focusState.verticalScrollIndex, focusState.verticalScrollOffset)
                focusState.focusedItemKeyByRow.forEach { (rowKey, itemKey) ->
                    focusedItemKeyByRow[rowKey] = itemKey
                }
                val rowKey = focusState.focusedRowKey
                if (!rowKey.isNullOrEmpty()) {
                    delay(120) // Give compose a small window to lay out elements
                    rowEntryFocusRequesters[rowKey]?.safeRequestFocus()
                }
            } else {
                delay(150)
                rowEntryFocusRequesters["featured"]?.safeRequestFocus()
                    ?: rowEntryFocusRequesters.values.firstOrNull()?.safeRequestFocus()
            }
        }
    }

    val contentFocusRequester = remember { FocusRequester() }
    com.kitsugi.animelist.ui.tv.focus.TvGlobalFocusRecovery(fallbackFocusRequester = contentFocusRequester)

    LaunchedEffect(homeStableGateReleased) {
        if (homeStableGateReleased) {
            contentFocusRequester.requestFocusAfterFrames(frames = 3)
        }
    }

    val onItemFocusedStable: (JikanSearchResult) -> Unit = remember {
        { _ -> }
    }

    androidx.compose.foundation.layout.Box(modifier = Modifier.focusRequester(contentFocusRequester)) {
        // Switch between layouts
        when (selectedHomeLayoutId) {
        "modern" -> {
            TvModernHomeContent(
                exploreViewModel = exploreViewModel,
                tvViewModel = tvViewModel,
                showAdultContent = showAdultContent,
                shimmerOffsetState = shimmerOffsetState,
                rowStates = rowStates,
                rowEntryFocusRequesters = rowEntryFocusRequesters,
                focusedItemKeyByRow = focusedItemKeyByRow,
                onItemFocusedStable = onItemFocusedStable,
                onNavigateToDetail = onNavigateToDetail,
                onSeeAllClick = onSeeAllClick,
                listState = listState,
                catalogRowScrollStates = catalogRowScrollStates
            )
        }
        "grid" -> {
            TvGridHomeContent(
                exploreViewModel = exploreViewModel,
                tvViewModel = tvViewModel,
                showAdultContent = showAdultContent,
                shimmerOffsetState = shimmerOffsetState,
                onItemFocusedStable = onItemFocusedStable,
                onNavigateToDetail = onNavigateToDetail,
                onSeeAllClick = onSeeAllClick
            )
        }
        else -> { // "classic"
            TvClassicHomeContent(
                exploreViewModel = exploreViewModel,
                tvViewModel = tvViewModel,
                showAdultContent = showAdultContent,
                shimmerOffsetState = shimmerOffsetState,
                rowStates = rowStates,
                rowEntryFocusRequesters = rowEntryFocusRequesters,
                focusedItemKeyByRow = focusedItemKeyByRow,
                onItemFocusedStable = onItemFocusedStable,
                onNavigateToDetail = onNavigateToDetail,
                onSeeAllClick = onSeeAllClick,
                listState = listState,
                catalogRowScrollStates = catalogRowScrollStates
            )
        }
    }
    }
}
