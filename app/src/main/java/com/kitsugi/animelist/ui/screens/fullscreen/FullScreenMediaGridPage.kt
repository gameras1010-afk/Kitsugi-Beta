package com.kitsugi.animelist.ui.screens.fullscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.TmdbApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.components.KitsugiExploreMediaCard
import com.kitsugi.animelist.ui.components.KitsugiEmptyState
import com.kitsugi.animelist.ui.screens.explore.ExploreCategoryType
import com.kitsugi.animelist.ui.screens.explore.ExplorePlatform
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.launch

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
    showAdultContent: Boolean = false
) {
    val accentColor = LocalKitsugiAccent.current
    val scope = rememberCoroutineScope()
    val apiClient = remember { JikanApiClient() }
    val tmdbApiClient = remember { TmdbApiClient() }

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
                            ExploreCategoryType.SEASONAL_ANIME -> apiClient.seasonalAnime(nextPage)
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
                            ExploreCategoryType.SEASONAL_ANIME -> apiClient.aniListSeasonalAnime(nextPage)
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

    val displayedResults = loadedResults.filter { result ->
        showAdultContent || !result.isAdult
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
                Modifier
                    .width(960.dp)
            } else {
                Modifier
                    .fillMaxSize()
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

            // 2. Başlık
            item(key = "title") {
                Text(
                    text = title,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
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

            // 4. Medya Listesi
            if (displayedResults.isEmpty()) {
                item(key = "empty_state") {
                    KitsugiEmptyState(
                        title = "Henüz içerik yok",
                        subtitle = "Bu bölümde gösterilecek içerik bulunamadı.",
                        icon = Icons.Rounded.SearchOff
                    )
                }
            } else {
                items(displayedResults.chunked(2), key = { it[0].source + "_" + it[0].malId }) { pair ->
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
                            hideScores = hideScores
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
                                hideScores = hideScores
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
                    text = title,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}