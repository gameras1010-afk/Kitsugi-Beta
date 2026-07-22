@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kitsugi.animelist.ui.screens.fullscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.TmdbApiClient
import com.kitsugi.animelist.ui.components.KitsugiEmptyState
import com.kitsugi.animelist.ui.components.KitsugiExploreMediaCard
import com.kitsugi.animelist.ui.components.KitsugiRankingMediaCard
import com.kitsugi.animelist.ui.components.DetailedSeasonalMediaCard
import com.kitsugi.animelist.ui.components.KitsugiSeasonalFilterBottomSheet
import com.kitsugi.animelist.ui.screens.explore.ExploreCategoryType
import com.kitsugi.animelist.ui.screens.explore.ExplorePlatform
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun FullScreenMediaGridPage(
    title: String,
    categoryType: ExploreCategoryType,
    platform: ExplorePlatform,
    initialResults: List<JikanSearchResult>,
    alreadyInList: (JikanSearchResult) -> Boolean,
    onItemClick: (JikanSearchResult) -> Unit,
    onBackClick: () -> Unit,
    titleLanguage: String = "ROMAJI",
    scoreFormat: String = "POINT_10",
    hideScores: Boolean = false,
    showAdultContent: Boolean = false,
    blurAdultMedia: Boolean = false,
    getMediaEntry: (JikanSearchResult) -> com.kitsugi.animelist.model.MediaEntry? = { null }
) {
    val accentColor = LocalKitsugiAccent.current
    val isTv = LocalIsTv.current
    val scope = rememberCoroutineScope()
    val apiClient = remember { JikanApiClient() }
    val tmdbApiClient = remember { TmdbApiClient() }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val columnCount = if (isLandscape) 3 else 2

    // false = Liste görünümü (varsayılan), true = Grid görünümü
    var isGridView by rememberSaveable { mutableStateOf(false) }

    val currentCalendar = remember { Calendar.getInstance() }
    val currentYear = remember { currentCalendar.get(Calendar.YEAR) }
    val currentSeasonStr = remember {
        val month = currentCalendar.get(Calendar.MONTH)
        when (month) {
            Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> "WINTER"
            Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> "SPRING"
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> "SUMMER"
            else -> "FALL"
        }
    }

    var seasonalYear by remember { mutableIntStateOf(currentYear) }
    var seasonalSeason by remember { mutableStateOf(currentSeasonStr) }
    var seasonalSort by remember { mutableStateOf("POPULARITY_DESC") }
    var showFilterBottomSheet by remember { mutableStateOf(false) }

    val dynamicTitle = remember(categoryType, title, seasonalSeason, seasonalYear) {
        if (categoryType == ExploreCategoryType.SEASONAL_ANIME) {
            val sn = when (seasonalSeason.uppercase()) {
                "WINTER" -> "Kış"; "SPRING" -> "İlkbahar"; "SUMMER" -> "Yaz"; else -> "Sonbahar"
            }
            "$sn $seasonalYear"
        } else title
    }

    var loadedResults by remember { mutableStateOf(initialResults) }
    var currentPage by remember { mutableStateOf(if (initialResults.isEmpty()) 0 else 1) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMorePages by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    fun loadNextPage() {
        if (isLoadingMore || !hasMorePages) return
        isLoadingMore = true; loadError = null
        scope.launch {
            try {
                val np = currentPage + 1
                val newItems = when (platform) {
                    ExplorePlatform.MAL -> when (categoryType) {
                        ExploreCategoryType.TOP_ANIME -> apiClient.topAnime(np)
                        ExploreCategoryType.TRENDING_ANIME -> apiClient.trendingAnime(np)
                        ExploreCategoryType.AIRING_ANIME -> apiClient.airingAnime(np)
                        ExploreCategoryType.UPCOMING_ANIME -> apiClient.upcomingAnime(np)
                        ExploreCategoryType.MOVIE_ANIME -> apiClient.movieAnime(np)
                        ExploreCategoryType.SEASONAL_ANIME -> apiClient.seasonalAnime(np, showAdultContent, seasonalYear, seasonalSeason, seasonalSort)
                        ExploreCategoryType.TOP_MANGA -> apiClient.topManga(np)
                        ExploreCategoryType.PUBLISHING_MANGA -> apiClient.publishingManga(np)
                        ExploreCategoryType.TRENDING_MANGA -> apiClient.trendingManga(np)
                        ExploreCategoryType.NEWLY_ADDED_ANIME -> apiClient.newlyAddedAnime(np)
                        ExploreCategoryType.NEWLY_ADDED_MANGA -> apiClient.newlyAddedManga(np)
                    }
                    ExplorePlatform.AniList -> when (categoryType) {
                        ExploreCategoryType.TOP_ANIME -> apiClient.aniListTopAnime(np)
                        ExploreCategoryType.TRENDING_ANIME -> apiClient.aniListTrendingAnime(np)
                        ExploreCategoryType.AIRING_ANIME -> apiClient.aniListAiringAnime(np)
                        ExploreCategoryType.UPCOMING_ANIME -> apiClient.aniListUpcomingAnime(np)
                        ExploreCategoryType.MOVIE_ANIME -> apiClient.aniListMovieAnime(np)
                        ExploreCategoryType.SEASONAL_ANIME -> apiClient.aniListSeasonalAnime(np, showAdultContent, seasonalYear, seasonalSeason, seasonalSort)
                        ExploreCategoryType.TOP_MANGA -> apiClient.aniListTopManga(np)
                        ExploreCategoryType.PUBLISHING_MANGA -> apiClient.aniListPublishingManga(np)
                        ExploreCategoryType.TRENDING_MANGA -> apiClient.aniListTrendingManga(np)
                        ExploreCategoryType.NEWLY_ADDED_ANIME -> apiClient.aniListNewlyAddedAnime(np)
                        ExploreCategoryType.NEWLY_ADDED_MANGA -> apiClient.aniListNewlyAddedManga(np)
                    }
                    ExplorePlatform.TMDB -> {
                        if (title.startsWith("İzlemeye Devam") || title.startsWith("Planladıklarım")) emptyList()
                        else when (categoryType) {
                            ExploreCategoryType.TOP_ANIME -> tmdbApiClient.getTrendingAll(np)
                            ExploreCategoryType.TRENDING_ANIME -> tmdbApiClient.getTrendingAll(np)
                            ExploreCategoryType.AIRING_ANIME -> tmdbApiClient.getTrendingShows(np)
                            ExploreCategoryType.MOVIE_ANIME -> tmdbApiClient.getTrendingMovies(np)
                            ExploreCategoryType.UPCOMING_ANIME -> tmdbApiClient.getPopularMovies(np)
                            ExploreCategoryType.TOP_MANGA -> tmdbApiClient.getTopRatedMovies(np)
                            ExploreCategoryType.PUBLISHING_MANGA -> tmdbApiClient.getTopRatedMovies(np)
                            ExploreCategoryType.SEASONAL_ANIME -> tmdbApiClient.getTopRatedShows(np)
                            ExploreCategoryType.TRENDING_MANGA -> emptyList()
                            ExploreCategoryType.NEWLY_ADDED_ANIME -> emptyList()
                            ExploreCategoryType.NEWLY_ADDED_MANGA -> emptyList()
                        }
                    }
                }
                if (newItems.isNotEmpty()) { loadedResults = loadedResults + newItems; currentPage = np }
                else hasMorePages = false
            } catch (e: Exception) {
                loadError = e.message ?: "Yükleme hatası"
            } finally { isLoadingMore = false }
        }
    }

    LaunchedEffect(Unit) {
        if (loadedResults.isEmpty()) {
            loadNextPage()
        }
    }

    fun applySeasonalFilter(season: String, year: Int, sort: String) {
        seasonalSeason = season; seasonalYear = year; seasonalSort = sort
        currentPage = 1; hasMorePages = true; loadedResults = emptyList()
        isLoadingMore = true; loadError = null
        scope.launch {
            try {
                loadedResults = if (platform == ExplorePlatform.AniList)
                    apiClient.aniListSeasonalAnime(1, showAdultContent, year, season, sort)
                else apiClient.seasonalAnime(1, showAdultContent, year, season, sort)
            } catch (e: Exception) {
                loadError = e.message ?: "Hata"
            } finally { isLoadingMore = false }
        }
    }

    val displayedResults = remember(loadedResults, showAdultContent) {
        loadedResults.filter { showAdultContent || !it.isAdult }
    }

    // Liste scroll state + auto-load trigger
    val listState = rememberLazyListState()
    val shouldLoadMoreList by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            last.index >= listState.layoutInfo.totalItemsCount - 4
        }
    }
    LaunchedEffect(shouldLoadMoreList) { if (shouldLoadMoreList) loadNextPage() }

    // Grid scroll state + auto-load trigger
    val gridState = rememberLazyGridState()
    val shouldLoadMoreGrid by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            last.index >= gridState.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(shouldLoadMoreGrid) { if (shouldLoadMoreGrid) loadNextPage() }

    val showFloatingHeader = if (isGridView) gridState.firstVisibleItemIndex >= 1
    else listState.firstVisibleItemIndex >= 1

    val showScrollToTop by remember {
        derivedStateOf {
            if (isGridView) {
                gridState.firstVisibleItemIndex > 3
            } else {
                listState.firstVisibleItemIndex > 3
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(KitsugiColors.Background),
        contentAlignment = Alignment.TopCenter
    ) {

        if (isGridView) {
            // ── GRID MODU ────────────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                state = gridState,
                modifier = if (isTv) Modifier.width(960.dp) else Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Başlık - tam genişlik span
                item(key = "grid_header", span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
                        // Geri butonu
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = onBackClick) {
                                Text("Geri", color = accentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                        // Başlık + toggle + filtre
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dynamicTitle,
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.weight(1f)
                            )
                            // Grid→Liste toggle
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val gridIndex = gridState.firstVisibleItemIndex
                                        val gridOffset = gridState.firstVisibleItemScrollOffset
                                        val listIndex = if (gridIndex == 0) 0 else gridIndex + 1
                                        isGridView = false
                                        kotlinx.coroutines.delay(10)
                                        listState.scrollToItem(listIndex, gridOffset)
                                    }
                                },
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(KitsugiColors.Surface)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ListAlt, contentDescription = "Liste Görünümü", tint = accentColor)
                            }
                            if (categoryType == ExploreCategoryType.SEASONAL_ANIME) {
                                IconButton(
                                    onClick = { showFilterBottomSheet = true },
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(KitsugiColors.Surface)
                                ) {
                                    Icon(Icons.Rounded.FilterList, contentDescription = "Filtre", tint = accentColor)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${displayedResults.size} içerik",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (displayedResults.isEmpty() && !isLoadingMore) {
                    item(key = "grid_empty", span = { GridItemSpan(maxLineSpan) }) {
                        KitsugiEmptyState(
                            title = "Henüz içerik yok",
                            subtitle = "Bu kategoride gösterilecek içerik bulunamadı.",
                            icon = Icons.Rounded.SearchOff
                        )
                    }
                } else {
                    itemsIndexed(
                        displayedResults,
                        key = { idx, item -> "${item.source}_${item.malId}_g$idx" }
                    ) { _, result ->
                        KitsugiExploreMediaCard(
                            result = result,
                            alreadyInList = alreadyInList(result),
                            mediaEntry = getMediaEntry(result),
                            onClick = { onItemClick(result) },
                            titleLanguage = titleLanguage,
                            scoreFormat = scoreFormat,
                            hideScores = hideScores,
                            blurAdultMedia = blurAdultMedia,
                            forceVertical = true
                        )
                    }
                }

                if (isLoadingMore) {
                    item(key = "grid_loading", span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = accentColor) }
                    }
                }

                if (loadError != null) {
                    item(key = "grid_error", span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(loadError ?: "Bir hata oluştu.", color = KitsugiColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { loadNextPage() }) {
                                Text("Tekrar Dene", color = accentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

        } else {
            // ── LİSTE MODU ───────────────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = if (isTv) Modifier.width(960.dp) else Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "list_header") {
                    Column {
                        Spacer(modifier = Modifier.height(28.dp))
                        // Geri butonu
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = onBackClick) {
                                Text("Geri", color = accentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                        // Başlık + toggle + filtre
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dynamicTitle,
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.weight(1f)
                            )
                            // Liste→Grid toggle
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val listIndex = listState.firstVisibleItemIndex
                                        val listOffset = listState.firstVisibleItemScrollOffset
                                        val gridIndex = if (listIndex <= 1) 0 else listIndex - 1
                                        isGridView = true
                                        kotlinx.coroutines.delay(10)
                                        gridState.scrollToItem(gridIndex, listOffset)
                                    }
                                },
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(KitsugiColors.Surface)
                            ) {
                                Icon(Icons.Rounded.GridView, contentDescription = "Grid Görünümü", tint = accentColor)
                            }
                            if (categoryType == ExploreCategoryType.SEASONAL_ANIME) {
                                IconButton(
                                    onClick = { showFilterBottomSheet = true },
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(KitsugiColors.Surface)
                                ) {
                                    Icon(Icons.Rounded.FilterList, contentDescription = "Mevsim Filtresi", tint = accentColor)
                                }
                            }
                        }
                    }
                }

                item(key = "list_count") {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "${displayedResults.size} içerik",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                if (displayedResults.isEmpty() && !isLoadingMore) {
                    item(key = "list_empty") {
                        KitsugiEmptyState(
                            title = "Henüz içerik yok",
                            subtitle = "Bu kategoride gösterilecek içerik bulunamadı.",
                            icon = Icons.Rounded.SearchOff
                        )
                    }
                } else {
                    itemsIndexed(
                        displayedResults,
                        key = { idx, item -> "${item.source}_${item.malId}_l$idx" }
                    ) { index, result ->
                        if (categoryType == ExploreCategoryType.SEASONAL_ANIME) {
                            DetailedSeasonalMediaCard(
                                result = result,
                                alreadyInList = alreadyInList(result),
                                onClick = { onItemClick(result) },
                                titleLanguage = titleLanguage
                            )
                        } else {
                            KitsugiRankingMediaCard(
                                result = result,
                                rankIndex = index + 1,
                                alreadyInList = alreadyInList(result),
                                mediaEntry = getMediaEntry(result),
                                onClick = { onItemClick(result) },
                                titleLanguage = titleLanguage,
                                hideScores = hideScores,
                                blurAdultMedia = blurAdultMedia
                            )
                        }
                    }
                }

                if (isLoadingMore) {
                    item(key = "list_loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = accentColor) }
                    }
                }

                if (loadError != null) {
                    item(key = "list_error") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(loadError ?: "Bir hata oluştu.", color = KitsugiColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { loadNextPage() }) {
                                Text("Tekrar Dene", color = accentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ── Floating Overlay Header ───────────────────────────────────────────
        AnimatedVisibility(
            visible = showFloatingHeader,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = (if (isTv) Modifier.width(960.dp) else Modifier.fillMaxWidth())
                    .height(64.dp)
                    .background(KitsugiColors.Surface.copy(alpha = 0.92f))
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = KitsugiColors.TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dynamicTitle,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Toggle – floating header'da da göster
                IconButton(onClick = {
                    scope.launch {
                        if (isGridView) {
                            val gridIndex = gridState.firstVisibleItemIndex
                            val gridOffset = gridState.firstVisibleItemScrollOffset
                            val listIndex = if (gridIndex == 0) 0 else gridIndex + 1
                            isGridView = false
                            kotlinx.coroutines.delay(10)
                            listState.scrollToItem(listIndex, gridOffset)
                        } else {
                            val listIndex = listState.firstVisibleItemIndex
                            val listOffset = listState.firstVisibleItemScrollOffset
                            val gridIndex = if (listIndex <= 1) 0 else listIndex - 1
                            isGridView = true
                            kotlinx.coroutines.delay(10)
                            gridState.scrollToItem(gridIndex, listOffset)
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Rounded.ListAlt else Icons.Rounded.GridView,
                        contentDescription = if (isGridView) "Liste" else "Grid",
                        tint = accentColor
                    )
                }
                if (categoryType == ExploreCategoryType.SEASONAL_ANIME) {
                    IconButton(onClick = { showFilterBottomSheet = true }) {
                        Icon(Icons.Rounded.FilterList, contentDescription = "Filtre", tint = accentColor)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor)
                    .tvClickable(shape = RoundedCornerShape(16.dp)) {
                        scope.launch {
                            if (isGridView) {
                                gridState.animateScrollToItem(0)
                            } else {
                                listState.animateScrollToItem(0)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Yukarı Git",
                    tint = KitsugiColors.Background,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (showFilterBottomSheet) {
            KitsugiSeasonalFilterBottomSheet(
                initialSeason = seasonalSeason,
                initialYear = seasonalYear,
                initialSort = seasonalSort,
                onDismissRequest = { showFilterBottomSheet = false },
                onApply = { s, y, sort -> applySeasonalFilter(s, y, sort) }
            )
        }
    }
}