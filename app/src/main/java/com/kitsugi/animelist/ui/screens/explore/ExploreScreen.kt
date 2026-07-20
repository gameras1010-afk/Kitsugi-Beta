@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.foundation.lazy.rememberLazyListState
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

@Composable
fun ExploreScreen(
    currentEntries: List<MediaEntry>,
    showAdultContent: Boolean,
    onAddSelectionToList: (ApiSearchSelection) -> Unit,
    onSeeAllSection: (title: String, categoryType: ExploreCategoryType, results: List<JikanSearchResult>) -> Unit,
    onOpenApiDetail: (JikanSearchResult) -> Unit,
    onOpenMangaReader: () -> Unit = {},
    onOpenAiringCalendar: () -> Unit = {},
    initialScrollOffset: Int = 0,
    onScrollOffsetChange: (Int) -> Unit = {},
    viewModel: ExploreViewModel = viewModel(),
    titleLanguage: String = "ROMAJI",
    scoreFormat: String = "POINT_10",
    hideScores: Boolean = false,
    showAnimeLogos: Boolean = false,
    isSimklConnected: Boolean = false
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

    val entryKeysSet = remember(currentEntries) {
        val keys = mutableSetOf<String>()
        currentEntries.forEach { entry ->
            keys.add("${entry.source.lowercase()}_${entry.malId}")
            if (entry.tmdbId != null) {
                keys.add("tmdb_${entry.tmdbId}")
            }
            if (entry.source.equals("jikan", ignoreCase = true) || entry.source.equals("mal", ignoreCase = true)) {
                keys.add("mal_${entry.malId}")
                keys.add("jikan_${entry.malId}")
            }
            val normTitle = entry.title.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }.trim()
            if (normTitle.isNotEmpty()) {
                keys.add("${entry.type}_$normTitle")
            }
        }
        keys
    }

    val isAlreadyInList = remember(entryKeysSet) {
        { result: JikanSearchResult ->
            val directKey = "${result.source.lowercase()}_${result.malId}"
            val tmdbId = result.tmdbId ?: if (result.source.equals("tmdb", ignoreCase = true)) result.malId else null
            val rMal = if (result.source.equals("jikan", ignoreCase = true) || result.source.equals("mal", ignoreCase = true)) result.malId else result.realMalId
            val normTitle = result.title.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }.trim()

            entryKeysSet.contains(directKey) ||
                    (tmdbId != null && entryKeysSet.contains("tmdb_$tmdbId")) ||
                    (rMal != null && (entryKeysSet.contains("mal_$rMal") || entryKeysSet.contains("jikan_$rMal"))) ||
                    (normTitle.isNotEmpty() && entryKeysSet.contains("${result.type}_$normTitle"))
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

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollOffset)

    val isHeroGone by remember {
        androidx.compose.runtime.derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 300
        }
    }

    LaunchedEffect(lazyListState.firstVisibleItemScrollOffset) {
        onScrollOffsetChange(lazyListState.firstVisibleItemScrollOffset)
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
            CompositionLocalProvider(
                LocalBringIntoViewSpec provides if (isTvDevice) tvSpec else LocalBringIntoViewSpec.current
            ) {
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
                                    scrollValue = lazyListState.firstVisibleItemScrollOffset.toFloat(),
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    showAnimeLogos = showAnimeLogos,
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

                        // Header / Title and Toggle
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                if (!isLandscape || isTvDevice) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        horizontalArrangement = if (isTvDevice) Arrangement.SpaceBetween else Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isTvDevice) {
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
                                        } else {
                                            ExplorePlatformToggle(
                                                selectedPlatform = viewModel.selectedPlatform,
                                                onPlatformSelected = { platform -> viewModel.selectPlatform(platform) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Hata mesajı
                                if (viewModel.errorMessage != null) {
                                    KitsugiErrorState(
                                        message = viewModel.errorMessage.orEmpty(),
                                        onRetryClick = { viewModel.loadData(forceRefresh = true) }
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
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
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Trend Her Şey", ExploreCategoryType.TOP_ANIME, filteredTopAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Trend Diziler",
                                    results = filteredAiringAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Trend Diziler", ExploreCategoryType.AIRING_ANIME, filteredAiringAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Trend Filmler",
                                    results = filteredMovieAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Trend Filmler", ExploreCategoryType.MOVIE_ANIME, filteredMovieAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Popüler Diziler",
                                    results = filteredTopManga,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Popüler Diziler", ExploreCategoryType.AIRING_ANIME, filteredTopManga) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Popüler Filmler",
                                    results = filteredUpcomingAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Popüler Filmler", ExploreCategoryType.UPCOMING_ANIME, filteredUpcomingAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "En Yüksek Puanlı Filmler",
                                    results = filteredPublishingManga,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("En Yüksek Puanlı Filmler", ExploreCategoryType.TOP_MANGA, filteredPublishingManga) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "En Yüksek Puanlı Diziler",
                                    results = filteredSeasonalAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("En Yüksek Puanlı Diziler", ExploreCategoryType.SEASONAL_ANIME, filteredSeasonalAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores
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
                                        onItemClick = onOpenApiDetail,
                                        onSeeAllClick = { onSeeAllSection("İzlemeye Devam Et — Diziler", ExploreCategoryType.AIRING_ANIME, viewModel.simklContinueSeries) },
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores
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
                                        onItemClick = onOpenApiDetail,
                                        onSeeAllClick = { onSeeAllSection("İzlemeye Devam Et — Filmler", ExploreCategoryType.MOVIE_ANIME, viewModel.simklContinueMovies) },
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores
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
                                        onItemClick = onOpenApiDetail,
                                        onSeeAllClick = { onSeeAllSection("Planladıklarım — Diziler", ExploreCategoryType.AIRING_ANIME, viewModel.simklPlannedSeries) },
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores
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
                                        onItemClick = onOpenApiDetail,
                                        onSeeAllClick = { onSeeAllSection("Planladıklarım — Filmler", ExploreCategoryType.MOVIE_ANIME, viewModel.simklPlannedMovies) },
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores
                                    )
                                }
                            }
                        } else {
                            // ─── ANİME KATEGORİLERİ ───
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                ) {
                                    ExploreCategoryGrid(
                                        title = "Keşfet",
                                        items = listOf(
                                            ExploreCategoryItem(
                                                title = "Trend",
                                                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                                                gradientColors = animeGradients[1],
                                                onClick = { onSeeAllSection("Trend Anime", ExploreCategoryType.TRENDING_ANIME, filteredTrendingAnime) }
                                            ),
                                            ExploreCategoryItem(
                                                title = "Filmler",
                                                icon = Icons.Rounded.Movie,
                                                gradientColors = animeGradients[4],
                                                onClick = { onSeeAllSection("Anime Filmleri", ExploreCategoryType.MOVIE_ANIME, filteredMovieAnime) }
                                            ),
                                            ExploreCategoryItem(
                                                title = "Mevsimlik",
                                                icon = Icons.Rounded.LocalFlorist,
                                                gradientColors = animeGradients[5],
                                                onClick = { onSeeAllSection("Mevsimlik Anime", ExploreCategoryType.SEASONAL_ANIME, filteredSeasonalAnime) }
                                            ),
                                            ExploreCategoryItem(
                                                title = "Yayın Takvimi",
                                                icon = Icons.Rounded.CalendarMonth,
                                                gradientColors = listOf(
                                                    Color(0xFF0D47A1),
                                                    Color(0xFF1976D2)
                                                ),
                                                onClick = onOpenAiringCalendar
                                            ),
                                            ExploreCategoryItem(
                                                title = "Manga Oku",
                                                icon = Icons.Default.AutoStories,
                                                gradientColors = listOf(
                                                    Color(0xFF6A1B9A),
                                                    Color(0xFFAD1457)
                                                ),
                                                onClick = onOpenMangaReader
                                            ),
                                        )
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
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Popüler Anime", ExploreCategoryType.TOP_ANIME, filteredTopAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Yayındaki Anime",
                                    results = filteredAiringAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Yayındaki Anime", ExploreCategoryType.AIRING_ANIME, filteredAiringAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Yaklaşan Anime",
                                    results = filteredUpcomingAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Yaklaşan Anime", ExploreCategoryType.UPCOMING_ANIME, filteredUpcomingAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Popüler Manga",
                                    results = filteredTopManga,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Popüler Manga", ExploreCategoryType.TOP_MANGA, filteredTopManga) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = "Yayındaki Manga",
                                    results = filteredPublishingManga,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    onItemClick = onOpenApiDetail,
                                    onSeeAllClick = { onSeeAllSection("Yayındaki Manga", ExploreCategoryType.PUBLISHING_MANGA, filteredPublishingManga) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores
                                )
                            }
                        }
                    }
            }

            // Sticky Top Bar — vitrin tamamen geçince yumuşakça detay sayfasındaki gibi üstten expand ederek gelir
            androidx.compose.animation.AnimatedVisibility(
                visible = isHeroGone && !isLandscape && !isTvDevice,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KitsugiColors.Background.copy(alpha = 0.82f))
                        .padding(top = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ExplorePlatformToggle(
                            selectedPlatform = viewModel.selectedPlatform,
                            onPlatformSelected = { platform -> viewModel.selectPlatform(platform) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    androidx.compose.material3.HorizontalDivider(
                        color = KitsugiColors.Border.copy(alpha = 0.25f),
                        thickness = 0.5.dp
                    )
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
}
