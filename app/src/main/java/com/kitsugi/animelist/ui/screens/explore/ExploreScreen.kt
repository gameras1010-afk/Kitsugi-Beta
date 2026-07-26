@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kitsugi.animelist.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitsugi.animelist.R
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.components.KitsugiHeroSection
import com.kitsugi.animelist.ui.components.KitsugiShimmerHeroSection
import com.kitsugi.animelist.ui.components.KitsugiErrorState
import com.kitsugi.animelist.ui.components.KitsugiEmptyState
import com.kitsugi.animelist.ui.components.KitsugiShimmerProvider
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalIsTvDevice
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec

@Composable
fun ExploreScreen(
    currentEntries: List<MediaEntry>,
    showAdultContent: Boolean,
    onAddSelectionToList: (ApiSearchSelection) -> Unit,
    onSeeAllSection: (title: String, categoryType: ExploreCategoryType, results: List<JikanSearchResult>) -> Unit,
    onOpenApiDetail: (JikanSearchResult) -> Unit,
    onEditEntry: (MediaEntry) -> Unit,
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
    blurAdultMedia: Boolean = false,
    onOpenNotifications: () -> Unit = {},
    isNotificationsVisible: Boolean = false
) {
    val accentColor = LocalKitsugiAccent.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val filteredTopAnime = remember(viewModel.topAnime, showAdultContent) { viewModel.topAnime.filter { showAdultContent || !it.isAdult } }
    val filteredAiringAnime = remember(viewModel.airingAnime, showAdultContent) { viewModel.airingAnime.filter { showAdultContent || !it.isAdult } }
    val filteredUpcomingAnime = remember(viewModel.upcomingAnime, showAdultContent) { viewModel.upcomingAnime.filter { showAdultContent || !it.isAdult } }
    val filteredTopManga = remember(viewModel.topManga, showAdultContent) { viewModel.topManga.filter { showAdultContent || !it.isAdult } }
    val filteredPublishingManga = remember(viewModel.publishingManga, showAdultContent) { viewModel.publishingManga.filter { showAdultContent || !it.isAdult } }
    val filteredTrendingAnime = remember(viewModel.trendingAnime, showAdultContent) { viewModel.trendingAnime.filter { showAdultContent || !it.isAdult } }
    val filteredMovieAnime = remember(viewModel.movieAnime, showAdultContent) { viewModel.movieAnime.filter { showAdultContent || !it.isAdult } }
    val filteredSeasonalAnime = remember(viewModel.seasonalAnime, showAdultContent) { viewModel.seasonalAnime.filter { showAdultContent || !it.isAdult } }
    val filteredTrendingManga = remember(viewModel.trendingManga, showAdultContent) { viewModel.trendingManga.filter { showAdultContent || !it.isAdult } }
    val filteredNewlyAddedAnime = remember(viewModel.newlyAddedAnime, showAdultContent) { viewModel.newlyAddedAnime.filter { showAdultContent || !it.isAdult } }
    val filteredNewlyAddedManga = remember(viewModel.newlyAddedManga, showAdultContent) { viewModel.newlyAddedManga.filter { showAdultContent || !it.isAdult } }
    val filteredAiringSoonAnime = remember(viewModel.airingSoonAnime, showAdultContent) { viewModel.airingSoonAnime.filter { showAdultContent || !it.isAdult } }

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
        generateExploreEntryMap(currentEntries)
    }

    val getMediaEntry = remember(entryMap) {
        { result: JikanSearchResult ->
            getMediaEntryFromMap(result, entryMap)
        }
    }

    val isAlreadyInList = remember(getMediaEntry) {
        { result: JikanSearchResult ->
            getMediaEntry(result) != null
        }
    }

    val onLongClickItem = remember(getMediaEntry) {
        { result: JikanSearchResult ->
            val entry = getMediaEntry(result)
            if (entry != null) {
                onEditEntry(entry)
            } else {
                onAddSelectionToList(ApiSearchSelection(result = result, synopsis = null))
            }
        }
    }

    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialScrollIndex,
        initialFirstVisibleItemScrollOffset = initialScrollOffset
    )

    var activeRankingSheetData by remember { mutableStateOf<Triple<String, MediaType, List<JikanSearchResult>>?>(null) }
    var isCategoriesExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            onScrollPositionChange(index, offset)
        }
    }

    val isTvDevice = LocalIsTvDevice.current

    val isCatalogEmpty = !viewModel.isLoading && viewModel.errorMessage == null &&
        filteredTopAnime.isEmpty() && filteredAiringAnime.isEmpty() &&
        filteredUpcomingAnime.isEmpty() && filteredTopManga.isEmpty() &&
        filteredPublishingManga.isEmpty() && filteredTrendingAnime.isEmpty() &&
        filteredMovieAnime.isEmpty() && filteredSeasonalAnime.isEmpty() &&
        viewModel.simklContinueSeries.isEmpty() && viewModel.simklContinueMovies.isEmpty() &&
        viewModel.simklPlannedSeries.isEmpty() && viewModel.simklPlannedMovies.isEmpty() &&
        filteredNewlyAddedAnime.isEmpty() && filteredNewlyAddedManga.isEmpty() &&
        filteredAiringSoonAnime.isEmpty()

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
                                    KitsugiShimmerHeroSection()
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            } else {
                                item {
                                    Spacer(modifier = Modifier.height(28.dp))
                                }
                            }

                            // Header / Title and Toggle as STICKY HEADER
                            stickyHeader(key = "explore_platform_toggle") {
                                Surface(
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
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                ExplorePlatformToggle(
                                                    selectedPlatform = viewModel.selectedPlatform,
                                                    onPlatformSelected = { platform -> viewModel.selectPlatform(platform) },
                                                    modifier = Modifier.weight(1f)
                                                )

                                                if (isNotificationsVisible) {
                                                    // 🔔 Bildirim butonu
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(KitsugiColors.Surface)
                                                            .tvClickable(shape = RoundedCornerShape(12.dp)) {
                                                                onOpenNotifications()
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Notifications,
                                                            contentDescription = "Bildirimler",
                                                            tint = KitsugiColors.TextPrimary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                } else {
                                                    // 🎲 Rastgele keşfet butonu
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(KitsugiColors.Surface)
                                                            .tvClickable(shape = RoundedCornerShape(12.dp)) {
                                                                val randomPool = mutableListOf<JikanSearchResult>()
                                                                randomPool.addAll(filteredTopAnime)
                                                                randomPool.addAll(filteredAiringAnime)
                                                                randomPool.addAll(filteredUpcomingAnime)
                                                                randomPool.addAll(filteredTopManga)
                                                                randomPool.addAll(filteredPublishingManga)
                                                                randomPool.addAll(filteredTrendingAnime)
                                                                randomPool.addAll(filteredMovieAnime)
                                                                randomPool.addAll(filteredSeasonalAnime)
                                                                randomPool.addAll(viewModel.simklContinueMovies)
                                                                randomPool.addAll(viewModel.simklPlannedMovies)
                                                                randomPool.addAll(viewModel.simklContinueSeries)
                                                                randomPool.addAll(viewModel.simklPlannedSeries)
                                                                if (randomPool.isNotEmpty()) {
                                                                    val randomResult = randomPool.random()
                                                                    onOpenApiDetail(randomResult)
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "🎲",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Önbellek Bildirim Banner'ı
                            if (viewModel.isShowingCachedData) {
                                item(key = "cached_data_banner") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 6.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(KitsugiColors.Surface.copy(alpha = 0.5f))
                                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "📡",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Column {
                                                Text(
                                                    text = "Çevrimdışı / Önbellek Modu",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = KitsugiColors.TextPrimary
                                                )
                                                Text(
                                                    text = "İnternet bağlantısı kesildi veya sunucu yanıt vermiyor. Son başarılı önbelleğe alınan veriler gösteriliyor.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = KitsugiColors.TextSecondary
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
                                item {
                                    TmdbCategoriesSection(
                                        isExpanded = isCategoriesExpanded,
                                        onExpandedChange = { isCategoriesExpanded = it },
                                        accentColor = accentColor,
                                        filteredTopAnime = filteredTopAnime,
                                        filteredAiringAnime = filteredAiringAnime,
                                        filteredMovieAnime = filteredMovieAnime,
                                        filteredTopManga = filteredTopManga,
                                        filteredUpcomingAnime = filteredUpcomingAnime,
                                        filteredSeasonalAnime = filteredSeasonalAnime,
                                        filteredPublishingManga = filteredPublishingManga,
                                        filteredTrendingAnime = filteredTrendingAnime,
                                        filteredNewlyAddedAnime = filteredNewlyAddedAnime,
                                        filteredTrendingManga = filteredTrendingManga,
                                        onSeeAllSection = onSeeAllSection,
                                        onOpenAiringCalendar = onOpenAiringCalendar
                                    )
                                }
                                if (filteredAiringSoonAnime.isNotEmpty() || viewModel.isLoading) {
                                    item {
                                        ExploreAiringSoonSection(
                                            title = stringResource(R.string.explore_airing_soon),
                                            airingSoonAnime = filteredAiringSoonAnime,
                                            isLoading = viewModel.isLoading,
                                            alreadyInList = isAlreadyInList,
                                            onItemClick = onOpenApiDetail,
                                            onLongClickItem = onLongClickItem,
                                            onOpenAiringCalendar = onOpenAiringCalendar,
                                            accentColor = accentColor,
                                            titleLanguage = titleLanguage,
                                            blurAdultMedia = blurAdultMedia
                                        )
                                        Spacer(modifier = Modifier.height(26.dp))
                                    }
                                }
                                tmdbExploreSections(
                                    viewModel = viewModel,
                                    filteredTrendingAnime = filteredTrendingAnime,
                                    filteredNewlyAddedAnime = filteredNewlyAddedAnime,
                                    filteredTrendingManga = filteredTrendingManga,
                                    filteredTopAnime = filteredTopAnime,
                                    filteredAiringAnime = filteredAiringAnime,
                                    filteredMovieAnime = filteredMovieAnime,
                                    filteredTopManga = filteredTopManga,
                                    filteredUpcomingAnime = filteredUpcomingAnime,
                                    filteredPublishingManga = filteredPublishingManga,
                                    filteredSeasonalAnime = filteredSeasonalAnime,
                                    isAlreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllSection = onSeeAllSection,
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia,
                                    context = context
                                )
                            } else {
                                item {
                                    DefaultCategoriesSection(
                                        isExpanded = isCategoriesExpanded,
                                        onExpandedChange = { isCategoriesExpanded = it },
                                        accentColor = accentColor,
                                        filteredSeasonalAnime = filteredSeasonalAnime,
                                        filteredTopAnime = filteredTopAnime,
                                        filteredTrendingAnime = filteredTrendingAnime,
                                        filteredMovieAnime = filteredMovieAnime,
                                        filteredNewlyAddedAnime = filteredNewlyAddedAnime,
                                        filteredTopManga = filteredTopManga,
                                        filteredPublishingManga = filteredPublishingManga,
                                        filteredTrendingManga = filteredTrendingManga,
                                        filteredNewlyAddedManga = filteredNewlyAddedManga,
                                        onSeeAllSection = onSeeAllSection,
                                        onOpenAiringCalendar = onOpenAiringCalendar,
                                        onOpenMangaReader = onOpenMangaReader
                                    )
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                                if (filteredAiringSoonAnime.isNotEmpty()) {
                                    item {
                                        ExploreAiringSoonSection(
                                            title = stringResource(R.string.explore_airing_soon),
                                            airingSoonAnime = filteredAiringSoonAnime,
                                            isLoading = viewModel.isLoading,
                                            alreadyInList = isAlreadyInList,
                                            onItemClick = onOpenApiDetail,
                                            onLongClickItem = onLongClickItem,
                                            onOpenAiringCalendar = onOpenAiringCalendar,
                                            accentColor = accentColor,
                                            titleLanguage = titleLanguage,
                                            blurAdultMedia = blurAdultMedia
                                        )
                                        Spacer(modifier = Modifier.height(26.dp))
                                    }
                                }
                                defaultExploreSections(
                                    viewModel = viewModel,
                                    filteredTopAnime = filteredTopAnime,
                                    filteredAiringAnime = filteredAiringAnime,
                                    filteredUpcomingAnime = filteredUpcomingAnime,
                                    filteredNewlyAddedAnime = filteredNewlyAddedAnime,
                                    filteredTopManga = filteredTopManga,
                                    filteredPublishingManga = filteredPublishingManga,
                                    filteredTrendingManga = filteredTrendingManga,
                                    filteredNewlyAddedManga = filteredNewlyAddedManga,
                                    isAlreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllSection = onSeeAllSection,
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia,
                                    context = context
                                )
                            }
                        }
                    }
                }
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
