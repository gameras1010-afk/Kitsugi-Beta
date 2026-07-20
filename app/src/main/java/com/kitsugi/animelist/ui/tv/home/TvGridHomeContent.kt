package com.kitsugi.animelist.ui.tv.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.screens.explore.ExplorePlatform
import com.kitsugi.animelist.ui.screens.explore.ExplorePlatformToggle
import com.kitsugi.animelist.ui.screens.explore.ExploreViewModel
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.tv.TvViewModel
import com.kitsugi.animelist.ui.tv.components.TvContentCard
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlinx.coroutines.delay

private enum class TmdbGridCategory(val displayName: String) {
    Continue("İzlemeye Devam Et"),
    Trending("Trendler"),
    Series("Diziler"),
    Movies("Filmler"),
    Planned("Planladıklarım")
}

private enum class MalGridCategory(val displayName: String) {
    Popular("Popülerler"),
    Airing("Yayındakiler"),
    Upcoming("Yaklaşanlar"),
    Manga("Mangalar")
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TvGridHomeContent(
    exploreViewModel: ExploreViewModel,
    tvViewModel: TvViewModel,
    showAdultContent: Boolean,
    shimmerOffsetState: State<Float>,
    onItemFocusedStable: (JikanSearchResult) -> Unit,
    onNavigateToDetail: (JikanSearchResult) -> Unit,
    onSeeAllClick: (String, List<JikanSearchResult>) -> Unit
) {
    val selectedPlatform = exploreViewModel.selectedPlatform

    val filteredTopAnime = exploreViewModel.topAnime.filter { showAdultContent || !it.isAdult }
    val filteredAiringAnime = exploreViewModel.airingAnime.filter { showAdultContent || !it.isAdult }
    val filteredUpcomingAnime = exploreViewModel.upcomingAnime.filter { showAdultContent || !it.isAdult }
    val filteredTopManga = exploreViewModel.topManga.filter { showAdultContent || !it.isAdult }
    val filteredPublishingManga = exploreViewModel.publishingManga.filter { showAdultContent || !it.isAdult }
    val filteredMovieAnime = exploreViewModel.movieAnime.filter { showAdultContent || !it.isAdult }
    val filteredSeasonalAnime = exploreViewModel.seasonalAnime.filter { showAdultContent || !it.isAdult }
    val filteredTrendingAnime = exploreViewModel.trendingAnime.filter { showAdultContent || !it.isAdult }

    // Categories state
    var selectedTmdbCategory by rememberSaveable { mutableStateOf(TmdbGridCategory.Trending) }
    var selectedMalCategory by rememberSaveable { mutableStateOf(MalGridCategory.Popular) }

    val currentItems = remember(
        selectedPlatform,
        selectedTmdbCategory,
        selectedMalCategory,
        filteredTopAnime,
        filteredAiringAnime,
        filteredUpcomingAnime,
        filteredTopManga,
        filteredPublishingManga,
        filteredMovieAnime,
        exploreViewModel.simklContinueSeries,
        exploreViewModel.simklContinueMovies,
        exploreViewModel.simklPlannedSeries,
        exploreViewModel.simklPlannedMovies
    ) {
        if (selectedPlatform == ExplorePlatform.TMDB) {
            when (selectedTmdbCategory) {
                TmdbGridCategory.Continue -> (exploreViewModel.simklContinueSeries + exploreViewModel.simklContinueMovies).distinctBy { it.malId }
                TmdbGridCategory.Trending -> filteredTopAnime
                TmdbGridCategory.Series -> filteredAiringAnime
                TmdbGridCategory.Movies -> filteredMovieAnime
                TmdbGridCategory.Planned -> (exploreViewModel.simklPlannedSeries + exploreViewModel.simklPlannedMovies).distinctBy { it.malId }
            }
        } else {
            when (selectedMalCategory) {
                MalGridCategory.Popular -> filteredTopAnime
                MalGridCategory.Airing -> filteredAiringAnime
                MalGridCategory.Upcoming -> filteredUpcomingAnime
                MalGridCategory.Manga -> filteredTopManga
            }
        }
    }

    val gridState = rememberLazyGridState()
    val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E)) // Solid premium TV background color
    ) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                // Header (Keşfet & Platform Switcher)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
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

                // Grid Sub-Categories Navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedPlatform == ExplorePlatform.TMDB) {
                        TmdbGridCategory.entries.forEach { cat ->
                            var isFocused by remember { mutableStateOf(false) }
                            val isSelected = selectedTmdbCategory == cat

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .tvClickable(shape = RoundedCornerShape(8.dp)) { selectedTmdbCategory = cat }
                                    .onFocusChanged {
                                        isFocused = it.isFocused
                                        if (it.isFocused) {
                                            selectedTmdbCategory = cat
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = cat.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    } else {
                        MalGridCategory.entries.forEach { cat ->
                            var isFocused by remember { mutableStateOf(false) }
                            val isSelected = selectedMalCategory == cat

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .tvClickable(shape = RoundedCornerShape(8.dp)) { selectedMalCategory = cat }
                                    .onFocusChanged {
                                        isFocused = it.isFocused
                                        if (it.isFocused) {
                                            selectedMalCategory = cat
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = cat.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(KitsugiTvTokens.Spacing.sm))

                // The Visual Grid of Content Cards
                if (currentItems.isNotEmpty()) {
                    CompositionLocalProvider(LocalBringIntoViewSpec provides tvSpec) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = KitsugiTvTokens.Cards.posterWidth),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                            verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                            modifier = Modifier.dpadVerticalFastScroll(scrollableState = gridState)
                        ) {
                            items(currentItems, key = { "${it.source}_${it.malId}" }) { item ->
                                TvContentCard(
                                    item = item,
                                    isInitialFocused = false,
                                    onFocused = { onItemFocusedStable(item) },
                                    onClick = { onNavigateToDetail(item) },
                                    shimmerOffsetState = shimmerOffsetState
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bu kategoride içerik bulunamadı",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
