@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kitsugi.animelist.ui.screens.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import com.kitsugi.animelist.model.MediaType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.matches
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.ui.components.KitsugiHeroSection
import com.kitsugi.animelist.ui.components.KitsugiHorizontalMediaSection
import com.kitsugi.animelist.ui.components.KitsugiErrorState
import com.kitsugi.animelist.ui.components.KitsugiEmptyState
import com.kitsugi.animelist.ui.components.KitsugiShimmerHeroSection
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.delay
import com.kitsugi.animelist.ui.components.KitsugiShimmerProvider

@Composable
fun ExploreScreen(
    currentEntries: List<MediaEntry>,
    showAdultContent: Boolean,
    onAddSelectionToList: (ApiSearchSelection) -> Unit,
    onSeeAllSection: (title: String, categoryType: ExploreCategoryType, results: List<JikanSearchResult>) -> Unit,
    onOpenApiDetail: (JikanSearchResult) -> Unit,
    onOpenMangaReader: () -> Unit = {},
    onOpenAiringCalendar: () -> Unit = {},
    initialScrollIndex: Int = 0,
    initialScrollOffset: Int = 0,
    onScrollPositionChange: (index: Int, offset: Int) -> Unit = { _, _ -> },
    viewModel: ExploreViewModel = viewModel(),
    titleLanguage: String = "ROMAJI",
    scoreFormat: String = "POINT_10",
    hideScores: Boolean = false,
    showAnimeLogos: Boolean = false,
    isSimklConnected: Boolean = false,
    blurAdultMedia: Boolean = false
) {
    val accentColor = LocalKitsugiAccent.current

    val filteredTopAnime = remember(viewModel.topAnime, showAdultContent) { viewModel.topAnime.filter { showAdultContent || !it.isAdult } }
    val filteredAiringAnime = remember(viewModel.airingAnime, showAdultContent) { viewModel.airingAnime.filter { showAdultContent || !it.isAdult } }
    val filteredUpcomingAnime = remember(viewModel.upcomingAnime, showAdultContent) { viewModel.upcomingAnime.filter { showAdultContent || !it.isAdult } }
    val filteredTopManga = remember(viewModel.topManga, showAdultContent) { viewModel.topManga.filter { showAdultContent || !it.isAdult } }
    val filteredPublishingManga = remember(viewModel.publishingManga, showAdultContent) { viewModel.publishingManga.filter { showAdultContent || !it.isAdult } }
    val filteredTrendingAnime = remember(viewModel.trendingAnime, showAdultContent) { viewModel.trendingAnime.filter { showAdultContent || !it.isAdult } }
    val filteredMovieAnime = remember(viewModel.movieAnime, showAdultContent) { viewModel.movieAnime.filter { showAdultContent || !it.isAdult } }
    val filteredSeasonalAnime = remember(viewModel.seasonalAnime, showAdultContent) { viewModel.seasonalAnime.filter { showAdultContent || !it.isAdult } }

    val heroItems = remember(viewModel.selectedPlatform, filteredTopAnime, filteredAiringAnime, filteredMovieAnime) {
        if (viewModel.selectedPlatform == ExplorePlatform.TMDB) {
            val itemsMix = mutableListOf<JikanSearchResult>()
            val topIt = filteredTopAnime.iterator()
            val airIt = filteredAiringAnime.iterator()
            val movIt = filteredMovieAnime.iterator()

            val addedKeys = mutableSetOf<String>()
            fun addIfUnique(item: JikanSearchResult) {
                val key = "${item.source}_${item.malId}"
                if (key !in addedKeys) {
                    addedKeys.add(key)
                    itemsMix.add(item)
                }
            }

            while (itemsMix.size < 5 && (topIt.hasNext() || airIt.hasNext() || movIt.hasNext())) {
                if (topIt.hasNext() && itemsMix.size < 5) addIfUnique(topIt.next())
                if (airIt.hasNext() && itemsMix.size < 5) addIfUnique(airIt.next())
                if (movIt.hasNext() && itemsMix.size < 5) addIfUnique(movIt.next())
            }
            itemsMix
        } else {
            filteredTopAnime.take(5)
        }
    }

    val entryMap = remember(currentEntries) {
        val mapping = mutableMapOf<String, MediaEntry>()
        currentEntries.forEach { entry ->
            mapping["${entry.source.lowercase()}_${entry.malId}"] = entry
            if (entry.tmdbId != null) {
                mapping["tmdb_${entry.tmdbId}"] = entry
            }
            if (entry.simklId != null) {
                mapping["simkl_${entry.simklId}"] = entry
            }
            if (entry.source.equals("anilist", ignoreCase = true) && entry.malId != null && entry.malId >= 100_000_000) {
                mapping["anilist_${entry.malId - 100_000_000}"] = entry
            }
            if (entry.source.equals("jikan", ignoreCase = true) || entry.source.equals("mal", ignoreCase = true)) {
                mapping["mal_${entry.malId}"] = entry
                mapping["jikan_${entry.malId}"] = entry
            }
            val normTitle = entry.title.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }.trim()
            if (normTitle.isNotEmpty()) {
                mapping["${entry.type.name.lowercase()}_$normTitle"] = entry
            }
        }
        mapping
    }

    val getMediaEntry = remember(entryMap) {
        { result: JikanSearchResult ->
            val directKey = "${result.source.lowercase()}_${result.malId}"
            var found = entryMap[directKey]

            if (found == null) {
                val tmdbId = result.tmdbId ?: if (result.source.equals("tmdb", ignoreCase = true)) result.malId else null
                if (tmdbId != null) {
                    found = entryMap["tmdb_$tmdbId"]
                }
            }

            if (found == null) {
                val rMal = if (result.source.equals("jikan", ignoreCase = true) || result.source.equals("mal", ignoreCase = true)) {
                    result.malId
                } else {
                    result.realMalId
                }
                if (rMal != null) {
                    found = entryMap["${result.source.lowercase()}_$rMal"]
                        ?: entryMap["mal_$rMal"]
                        ?: entryMap["jikan_$rMal"]
                        ?: entryMap["anilist_$rMal"]
                        ?: entryMap["simkl_$rMal"]
                }
            }

            if (found == null) {
                val normTitle = buildString {
                    for (c in result.title.lowercase()) {
                        if (c in 'a'..'z' || c in '0'..'9') append(c)
                    }
                }.trim()
                if (normTitle.isNotEmpty()) {
                    found = entryMap["${result.type.name.lowercase()}_$normTitle"]
                }
            }

            found
        }
    }

    val isAlreadyInList = remember(getMediaEntry) {
        { result: JikanSearchResult ->
            getMediaEntry(result) != null
        }
    }

    fun handleAdd(result: JikanSearchResult, synopsis: String?) {
        if (!isAlreadyInList(result)) {
            onAddSelectionToList(ApiSearchSelection(result = result, synopsis = synopsis))
        }
    }

    // Kategori kartları için gradient renk tanımları
    val animeGradients = listOf(
        listOf(KitsugiColors.AccentPurple.copy(0.85f), KitsugiColors.AccentBlue.copy(0.70f)),   // Top 100
        listOf(KitsugiColors.AccentPink.copy(0.85f), KitsugiColors.AccentPurple.copy(0.70f)),   // Trend
        listOf(KitsugiColors.AccentGreen.copy(0.80f), KitsugiColors.AccentBlue.copy(0.65f)),    // Yayında
        listOf(KitsugiColors.AccentOrange.copy(0.85f), KitsugiColors.AccentRed.copy(0.70f)),    // Yaklaşan
        listOf(KitsugiColors.AccentBlue.copy(0.85f), KitsugiColors.AccentPurple.copy(0.65f)),   // Filmler
        listOf(KitsugiColors.AccentRed.copy(0.80f), KitsugiColors.AccentPink.copy(0.65f)),      // Mevsimlik
    )

    val mangaGradients = listOf(
        listOf(KitsugiColors.AccentOrange.copy(0.85f), KitsugiColors.AccentPink.copy(0.70f)),   // Top Manga
        listOf(KitsugiColors.AccentGreen.copy(0.85f), KitsugiColors.AccentBlue.copy(0.70f)),    // Yayında
    )

    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialScrollIndex,
        initialFirstVisibleItemScrollOffset = initialScrollOffset
    )

    var activeRankingSheetData by remember { mutableStateOf<Triple<String, MediaType, List<JikanSearchResult>>?>(null) }
    var isCategoriesExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }

    val isHeroGone by remember {
        androidx.compose.runtime.derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 300
        }
    }

    LaunchedEffect(lazyListState) {
        androidx.compose.runtime.snapshotFlow {
            lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            onScrollPositionChange(index, offset)
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current

    val isCatalogEmpty = !viewModel.isLoading && viewModel.errorMessage == null &&
        filteredTopAnime.isEmpty() && filteredAiringAnime.isEmpty() &&
        filteredUpcomingAnime.isEmpty() && filteredTopManga.isEmpty() &&
        filteredPublishingManga.isEmpty() && filteredTrendingAnime.isEmpty() &&
        filteredMovieAnime.isEmpty() && filteredSeasonalAnime.isEmpty() &&
        viewModel.simklContinueSeries.isEmpty() && viewModel.simklContinueMovies.isEmpty() &&
        viewModel.simklPlannedSeries.isEmpty() && viewModel.simklPlannedMovies.isEmpty()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
            val pullRefreshState = rememberPullToRefreshState()
            CompositionLocalProvider(
                LocalBringIntoViewSpec provides if (isTvDevice) tvSpec else LocalBringIntoViewSpec.current
            ) {
                PullToRefreshBox(
                    isRefreshing = viewModel.isLoading,
                    onRefresh = { viewModel.loadData(forceRefresh = true) },
                    modifier = Modifier.fillMaxSize(),
                    state = pullRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullRefreshState,
                            isRefreshing = viewModel.isLoading,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = KitsugiColors.Surface,
                            color = accentColor
                        )
                    }
                ) {
                    KitsugiShimmerProvider {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                        if (heroItems.isNotEmpty()) {
                            item {
                                KitsugiHeroSection(
                                    items = heroItems,
                                    alreadyInList = isAlreadyInList,
                                    onInfoClick = onOpenApiDetail,
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    showAnimeLogos = showAnimeLogos,
                                    blurAdultMedia = blurAdultMedia,
                                    isVisible = lazyListState.firstVisibleItemIndex == 0
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }
                        } else if (viewModel.isLoading) {
                            item {
                                KitsugiShimmerProvider {
                                    KitsugiShimmerHeroSection()
                                }
                                Spacer(modifier = Modifier.height(26.dp))
                            }
                        } else {
                            item {
                                Spacer(modifier = Modifier.height(28.dp))
                            }
                        }

                        // Header / Title and Toggle as STICKY HEADER
                        stickyHeader(key = "explore_platform_toggle") {
                            if (!isLandscape || isTvDevice) {
                                androidx.compose.material3.Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = KitsugiColors.Background.copy(alpha = 0.95f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 8.dp)
                                    ) {
                                        if (isTvDevice) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Keşfet",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Black,
                                                    color = KitsugiColors.TextPrimary
                                                )
                                                ExplorePlatformToggle(
                                                    selectedPlatform = viewModel.selectedPlatform,
                                                    onPlatformSelected = { platform -> viewModel.selectPlatform(platform) },
                                                    modifier = Modifier.width(300.dp)
                                                )
                                            }
                                        } else {
                                            ExplorePlatformToggle(
                                                selectedPlatform = viewModel.selectedPlatform,
                                                onPlatformSelected = { platform -> viewModel.selectPlatform(platform) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Hata mesajı
                        if (viewModel.errorMessage != null) {
                            item(key = "error_message") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    KitsugiErrorState(
                                        message = viewModel.errorMessage.orEmpty(),
                                        onRetryClick = { viewModel.loadData(forceRefresh = true) }
                                    )
                                }
                            }
                        }

                        if (isCatalogEmpty) {
                            item {
                                KitsugiEmptyState(
                                    title = "Gösterilecek İçerik Yok",
                                    subtitle = "Seçilen platformda görüntülenebilecek medya bulunamadı."
                                )
                            }
                        } else if (viewModel.selectedPlatform == ExplorePlatform.TMDB) {
                            // ─── TMDB YATAY MEDYA LİSTELERİ ───
                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Trend Her Şey",
                                    results = filteredTopAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Trend Her Şey", ExploreCategoryType.TOP_ANIME, filteredTopAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Trend Diziler",
                                    results = filteredAiringAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Trend Diziler", ExploreCategoryType.AIRING_ANIME, filteredAiringAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Trend Filmler",
                                    results = filteredMovieAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Trend Filmler", ExploreCategoryType.MOVIE_ANIME, filteredMovieAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Popüler Diziler",
                                    results = filteredTopManga,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Popüler Diziler", ExploreCategoryType.AIRING_ANIME, filteredTopManga) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Popüler Filmler",
                                    results = filteredUpcomingAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Popüler Filmler", ExploreCategoryType.UPCOMING_ANIME, filteredUpcomingAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "En Yüksek Puanlı Filmler",
                                    results = filteredPublishingManga,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("En Yüksek Puanlı Filmler", ExploreCategoryType.TOP_MANGA, filteredPublishingManga) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "En Yüksek Puanlı Diziler",
                                    results = filteredSeasonalAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("En Yüksek Puanlı Diziler", ExploreCategoryType.SEASONAL_ANIME, filteredSeasonalAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                            }

                            // ─── SİMKL KULLANICI LİSTELERİ ───
                            if (viewModel.simklContinueSeries.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(26.dp))
                                    KitsugiHorizontalMediaSection(
                                        title = "İzlemeye Devam Et — Diziler",
                                        results = viewModel.simklContinueSeries,
                                        isLoading = viewModel.isLoading,
                                        alreadyInList = isAlreadyInList,
                                        getMediaEntry = getMediaEntry,
                                        onItemClick = onOpenApiDetail,
                                        onSeeAllClick = { onSeeAllSection("İzlemeye Devam Et — Diziler", ExploreCategoryType.AIRING_ANIME, viewModel.simklContinueSeries) },
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores,
                                        blurAdultMedia = blurAdultMedia
                                    )
                                }
                            }

                            if (viewModel.simklContinueMovies.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(26.dp))
                                    KitsugiHorizontalMediaSection(
                                        title = "İzlemeye Devam Et — Filmler",
                                        results = viewModel.simklContinueMovies,
                                        isLoading = viewModel.isLoading,
                                        alreadyInList = isAlreadyInList,
                                        getMediaEntry = getMediaEntry,
                                        onItemClick = onOpenApiDetail,
                                        onSeeAllClick = { onSeeAllSection("İzlemeye Devam Et — Filmler", ExploreCategoryType.MOVIE_ANIME, viewModel.simklContinueMovies) },
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores,
                                        blurAdultMedia = blurAdultMedia
                                    )
                                }
                            }

                            if (viewModel.simklPlannedSeries.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(26.dp))
                                    KitsugiHorizontalMediaSection(
                                        title = "Planladıklarım — Diziler",
                                        results = viewModel.simklPlannedSeries,
                                        isLoading = viewModel.isLoading,
                                        alreadyInList = isAlreadyInList,
                                        getMediaEntry = getMediaEntry,
                                        onItemClick = onOpenApiDetail,
                                        onSeeAllClick = { onSeeAllSection("Planladıklarım — Diziler", ExploreCategoryType.AIRING_ANIME, viewModel.simklPlannedSeries) },
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores,
                                        blurAdultMedia = blurAdultMedia
                                    )
                                }
                            }

                            if (viewModel.simklPlannedMovies.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(26.dp))
                                    KitsugiHorizontalMediaSection(
                                        title = "Planladıklarım — Filmler",
                                        results = viewModel.simklPlannedMovies,
                                        isLoading = viewModel.isLoading,
                                        alreadyInList = isAlreadyInList,
                                        getMediaEntry = getMediaEntry,
                                        onItemClick = onOpenApiDetail,
                                        onSeeAllClick = { onSeeAllSection("Planladıklarım — Filmler", ExploreCategoryType.MOVIE_ANIME, viewModel.simklPlannedMovies) },
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores,
                                        blurAdultMedia = blurAdultMedia
                                    )
                                }
                            }
                        } else {
                            // ─── KATEGORİLER (Collapsible Section) ───
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(KitsugiColors.Surface.copy(alpha = 0.4f))
                                            .clickable { isCategoriesExpanded = !isCategoriesExpanded }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Rounded.GridView,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Kategoriler",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = KitsugiColors.TextPrimary
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isCategoriesExpanded) androidx.compose.material.icons.Icons.Rounded.KeyboardArrowUp else androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "Genişlet/Daralt",
                                            tint = KitsugiColors.TextSecondary
                                        )
                                    }
                                    
                                    AnimatedVisibility(
                                        visible = isCategoriesExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp, start = 4.dp, end = 4.dp)
                                        ) {
                                            // ── Anime Alt Kategorisi ──
                                            Text(
                                                text = "Anime",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = KitsugiColors.TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Sezon",
                                                        onClick = { onSeeAllSection("Mevsimlik Anime", ExploreCategoryType.SEASONAL_ANIME, filteredSeasonalAnime) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Yayın Takvimi",
                                                        onClick = onOpenAiringCalendar
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "En İyi 100",
                                                        onClick = { onSeeAllSection("En İyi Anime", ExploreCategoryType.TOP_ANIME, filteredTopAnime) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Trend",
                                                        onClick = { onSeeAllSection("Trend Anime", ExploreCategoryType.TRENDING_ANIME, filteredTrendingAnime) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Filmler",
                                                        onClick = { onSeeAllSection("Anime Filmleri", ExploreCategoryType.MOVIE_ANIME, filteredMovieAnime) }
                                                    )
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(20.dp))
                                            
                                            // ── Manga Alt Kategorisi ──
                                            Text(
                                                text = "Manga",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = KitsugiColors.TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "En İyi 100",
                                                        onClick = { onSeeAllSection("En Popüler Manga", ExploreCategoryType.TOP_MANGA, filteredTopManga) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Yakında",
                                                        onClick = { onSeeAllSection("Yayındaki Manga", ExploreCategoryType.PUBLISHING_MANGA, filteredPublishingManga) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Manga Oku",
                                                        onClick = onOpenMangaReader
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }

                            // ─── YAKINDA YAYINDA (Airing Soon) ───
                            if (viewModel.airingSoonAnime.isNotEmpty()) {
                                item {
                                    KitsugiHorizontalMediaSection(
                                        title = "Yakında Yayında",
                                        results = viewModel.airingSoonAnime,
                                        isLoading = viewModel.isLoading,
                                        alreadyInList = isAlreadyInList,
                                        getMediaEntry = getMediaEntry,
                                        onItemClick = onOpenApiDetail,
                                        onSeeAllClick = onOpenAiringCalendar,
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores,
                                        blurAdultMedia = blurAdultMedia
                                    )
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }

                            // ─── YATAY MEDYA LİSTELERİ ───
                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Popüler Anime",
                                    results = filteredTopAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Popüler Anime", ExploreCategoryType.TOP_ANIME, filteredTopAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Yayındaki Anime",
                                    results = filteredAiringAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Yayındaki Anime", ExploreCategoryType.AIRING_ANIME, filteredAiringAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Yaklaşan Anime",
                                    results = filteredUpcomingAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Yaklaşan Anime", ExploreCategoryType.UPCOMING_ANIME, filteredUpcomingAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Popüler Manga",
                                    results = filteredTopManga,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Popüler Manga", ExploreCategoryType.TOP_MANGA, filteredTopManga) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Yayındaki Manga",
                                    results = filteredPublishingManga,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Yayındaki Manga", ExploreCategoryType.PUBLISHING_MANGA, filteredPublishingManga) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                            }
                        }
                    }
                }
                }
            }
        }

        if (isLandscape && !isTvDevice) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(72.dp)
                    .background(KitsugiColors.Background)
                    .padding(vertical = 24.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                ExplorePlatformToggle(
                    selectedPlatform = viewModel.selectedPlatform,
                    onPlatformSelected = { platform -> viewModel.selectPlatform(platform) },
                    isVertical = true,
                    modifier = Modifier.fillMaxHeight(0.6f)
                )
            }
        }
    }

    val rankingSheet = activeRankingSheetData
    if (rankingSheet != null) {
        com.kitsugi.animelist.ui.components.KitsugiRankingBottomSheet(
            title = rankingSheet.first,
            mediaType = rankingSheet.second,
            platform = viewModel.selectedPlatform,
            initialResults = rankingSheet.third,
            alreadyInList = isAlreadyInList,
            onItemClick = onOpenApiDetail,
            onDismissRequest = { activeRankingSheetData = null },
            titleLanguage = titleLanguage,
            hideScores = hideScores,
            showAdultContent = showAdultContent,
            blurAdultMedia = blurAdultMedia,
            getMediaEntry = getMediaEntry
        )
    }
}

@Composable
fun ExploreCategoryChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KitsugiColors.Accent.copy(alpha = 0.08f))
            .border(1.dp, KitsugiColors.Accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = KitsugiColors.Accent,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}