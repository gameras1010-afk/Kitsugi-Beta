@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kitsugi.animelist.ui.screens.fullscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FilterList
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
import com.kitsugi.animelist.ui.components.KitsugiSeasonalFilterBottomSheet
import com.kitsugi.animelist.ui.screens.explore.ExploreCategoryType
import com.kitsugi.animelist.ui.screens.explore.ExplorePlatform
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
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
    blurAdultMedia: Boolean = false
) {
    val accentColor = LocalKitsugiAccent.current
    val scope = rememberCoroutineScope()
    val apiClient = remember { JikanApiClient() }
    val tmdbApiClient = remember { TmdbApiClient() }

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
            val seasonName = when (seasonalSeason.uppercase()) {
                "WINTER" -> "Kış"
                "SPRING" -> "İlkbahar"
                "SUMMER" -> "Yaz"
                else -> "Sonbahar"
            }
            "$seasonName $seasonalYear"
        } else {
            title
        }
    }

    var loadedResults by remember {
        mutableStateOf(initialResults)
    }

    var currentPage by remember {
        mutableStateOf(1)
    }

    var isLoadingMore by remember {
        mutableStateOf(false)
    }

    var hasMorePages by remember {
        mutableStateOf(true)
    }

    var loadError by remember {
        mutableStateOf<String?>(null)
    }

    fun applySeasonalFilter(season: String, year: Int, sort: String) {
        seasonalSeason = season
        seasonalYear = year
        seasonalSort = sort
        currentPage = 1
        hasMorePages = true
        loadedResults = emptyList()
        isLoadingMore = true
        loadError = null
        scope.launch {
            try {
                val newItems = if (platform == ExplorePlatform.AniList) {
                    apiClient.aniListSeasonalAnime(
                        page = 1,
                        showAdultContent = showAdultContent,
                        year = year,
                        season = season,
                        sort = sort
                    )
                } else {
                    apiClient.seasonalAnime(
                        page = 1,
                        showAdultContent = showAdultContent,
                        year = year,
                        season = season,
                        sort = sort
                    )
                }
                loadedResults = newItems
            } catch (e: Exception) {
                loadError = e.message ?: "Mevsimlik veriler yüklenirken hata oluştu"
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun loadNextPage() {
        if (isLoadingMore || !hasMorePages) return
        isLoadingMore = true
        loadError = null
        scope.launch {
            try {
                val nextPage = currentPage + 1
                val newItems = when (platform) {
                    ExplorePlatform.MAL -> {
                        when (categoryType) {
                            ExploreCategoryType.TOP_ANIME -> apiClient.topAnime(nextPage)
                            ExploreCategoryType.TRENDING_ANIME -> apiClient.trendingAnime(nextPage)
                            ExploreCategoryType.AIRING_ANIME -> apiClient.airingAnime(nextPage)
                            ExploreCategoryType.UPCOMING_ANIME -> apiClient.upcomingAnime(nextPage)
                            ExploreCategoryType.MOVIE_ANIME -> apiClient.movieAnime(nextPage)
                            ExploreCategoryType.SEASONAL_ANIME -> apiClient.seasonalAnime(
                                page = nextPage,
                                showAdultContent = showAdultContent,
                                year = seasonalYear,
                                season = seasonalSeason,
                                sort = seasonalSort
                            )
                            ExploreCategoryType.TOP_MANGA -> apiClient.topManga(nextPage)
                            ExploreCategoryType.PUBLISHING_MANGA -> apiClient.publishingManga(nextPage)
                        }
                    }
                    ExplorePlatform.AniList -> {
                        when (categoryType) {
                            ExploreCategoryType.TOP_ANIME -> apiClient.aniListTopAnime(nextPage)
                            ExploreCategoryType.TRENDING_ANIME -> apiClient.aniListTrendingAnime(nextPage)
                            ExploreCategoryType.AIRING_ANIME -> apiClient.aniListAiringAnime(nextPage)
                            ExploreCategoryType.UPCOMING_ANIME -> apiClient.aniListUpcomingAnime(nextPage)
                            ExploreCategoryType.MOVIE_ANIME -> apiClient.aniListMovieAnime(nextPage)
                            ExploreCategoryType.SEASONAL_ANIME -> apiClient.aniListSeasonalAnime(
                                page = nextPage,
                                showAdultContent = showAdultContent,
                                year = seasonalYear,
                                season = seasonalSeason,
                                sort = seasonalSort
                            )
                            ExploreCategoryType.TOP_MANGA -> apiClient.aniListTopManga(nextPage)
                            ExploreCategoryType.PUBLISHING_MANGA -> apiClient.aniListPublishingManga(nextPage)
                        }
                    }
                    ExplorePlatform.TMDB -> {
                        if (title.startsWith("İzlemeye Devam") || title.startsWith("Planladıklarım")) {
                            emptyList()
                        } else {
                            when (categoryType) {
                                ExploreCategoryType.TOP_ANIME -> tmdbApiClient.getTrendingAll(nextPage)
                                ExploreCategoryType.TRENDING_ANIME -> tmdbApiClient.getTrendingAll(nextPage)
                                ExploreCategoryType.AIRING_ANIME -> tmdbApiClient.getTrendingShows(nextPage)
                                ExploreCategoryType.MOVIE_ANIME -> tmdbApiClient.getTrendingMovies(nextPage)
                                ExploreCategoryType.UPCOMING_ANIME -> tmdbApiClient.getPopularMovies(nextPage)
                                ExploreCategoryType.TOP_MANGA -> tmdbApiClient.getTopRatedMovies(nextPage)
                                ExploreCategoryType.PUBLISHING_MANGA -> tmdbApiClient.getTopRatedMovies(nextPage)
                                ExploreCategoryType.SEASONAL_ANIME -> tmdbApiClient.getTopRatedShows(nextPage)
                            }
                        }
                    }
                }

                if (newItems.isNotEmpty()) {
                    loadedResults = loadedResults + newItems
                    currentPage = nextPage
                } else {
                    hasMorePages = false
                }
            } catch (e: Exception) {
                loadError = e.message ?: "Daha fazla yüklenirken hata oluştu"
            } finally {
                isLoadingMore = false
            }
        }
    }

    val displayedResults = remember(loadedResults, showAdultContent) {
        loadedResults.filter { result ->
            showAdultContent || !result.isAdult
        }
    }

    val chunkedResults = remember(displayedResults) {
        displayedResults.chunked(2)
    }

    val listState = rememberLazyListState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 4
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            loadNextPage()
        }
    }

    val showFloatingHeader = listState.firstVisibleItemIndex >= 1
    val isTv = LocalIsTv.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background),
        contentAlignment = Alignment.TopCenter
    ) {
        // SCROLLABLE LIST SECTION
        LazyColumn(
            state = listState,
            modifier = if (isTv) {
                Modifier.width(960.dp)
            } else {
                Modifier.fillMaxSize()
            },
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Geri Butonu
            item(key = "back_button") {
                Column {
                    Spacer(modifier = Modifier.height(28.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = onBackClick
                        ) {
                            Text(
                                text = "Geri",
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Başlık + Filtre Butonu
            item(key = "title") {
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

                    if (categoryType == ExploreCategoryType.SEASONAL_ANIME) {
                        IconButton(
                            onClick = { showFilterBottomSheet = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(KitsugiColors.Surface)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = "Mevsim Filtresi",
                                tint = accentColor
                            )
                        }
                    }
                }
            }

            // 3. İçerik Sayısı
            item(key = "count") {
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

            // 4. Medya Listesi (Sıralama sayfalarında dikey sıralama kartları, diğerlerinde 2'li grid)
            if (displayedResults.isEmpty() && !isLoadingMore) {
                item(key = "empty_state") {
                    KitsugiEmptyState(
                        title = "Henüz içerik yok",
                        subtitle = "Bu kategoride gösterilecek içerik bulunamadı.",
                        icon = Icons.Rounded.SearchOff
                    )
                }
            } else if (categoryType == ExploreCategoryType.TOP_ANIME ||
                       categoryType == ExploreCategoryType.TOP_MANGA ||
                       categoryType == ExploreCategoryType.TRENDING_ANIME ||
                       categoryType == ExploreCategoryType.AIRING_ANIME ||
                       categoryType == ExploreCategoryType.UPCOMING_ANIME ||
                       categoryType == ExploreCategoryType.PUBLISHING_MANGA) {
                itemsIndexed(displayedResults, key = { index, item -> item.source + "_" + item.malId + "_" + index }) { index, result ->
                    com.kitsugi.animelist.ui.components.KitsugiRankingMediaCard(
                        result = result,
                        rankIndex = index + 1,
                        alreadyInList = alreadyInList(result),
                        onClick = { onItemClick(result) },
                        titleLanguage = titleLanguage,
                        hideScores = hideScores,
                        blurAdultMedia = blurAdultMedia,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            } else {
                items(chunkedResults, key = { pair -> "${pair[0].source}_${pair[0].malId}_${pair.getOrNull(1)?.malId ?: 0}" }) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KitsugiExploreMediaCard(
                            result = pair[0],
                            alreadyInList = alreadyInList(pair[0]),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onItemClick(pair[0])
                            },
                            titleLanguage = titleLanguage,
                            scoreFormat = scoreFormat,
                            hideScores = hideScores,
                            blurAdultMedia = blurAdultMedia
                        )
                        if (pair.size > 1) {
                            KitsugiExploreMediaCard(
                                result = pair[1],
                                alreadyInList = alreadyInList(pair[1]),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onItemClick(pair[1])
                                },
                                titleLanguage = titleLanguage,
                                scoreFormat = scoreFormat,
                                hideScores = hideScores,
                                blurAdultMedia = blurAdultMedia
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (isLoadingMore) {
                item(key = "loading_more") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }
            }

            if (loadError != null) {
                item(key = "error") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = loadError ?: "Bir hata oluştu.",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { loadNextPage() }) {
                            Text("Tekrar Dene", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Floating overlay header
        AnimatedVisibility(
            visible = showFloatingHeader,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (isTv) {
                    Modifier
                        .width(960.dp)
                        .height(64.dp)
                        .background(KitsugiColors.Surface.copy(alpha = 0.92f))
                        .padding(horizontal = 8.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(KitsugiColors.Surface.copy(alpha = 0.92f))
                        .padding(horizontal = 8.dp)
                }
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
                    text = dynamicTitle,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (categoryType == ExploreCategoryType.SEASONAL_ANIME) {
                    IconButton(onClick = { showFilterBottomSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = "Mevsim Filtresi",
                            tint = accentColor
                        )
                    }
                }
            }
        }

        if (showFilterBottomSheet) {
            KitsugiSeasonalFilterBottomSheet(
                initialSeason = seasonalSeason,
                initialYear = seasonalYear,
                initialSort = seasonalSort,
                onDismissRequest = { showFilterBottomSheet = false },
                onApply = { newSeason, newYear, newSort ->
                    applySeasonalFilter(newSeason, newYear, newSort)
                }
            )
        }
    }
}