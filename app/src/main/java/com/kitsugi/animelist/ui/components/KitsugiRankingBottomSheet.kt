@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.TmdbApiClient
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.screens.explore.ExplorePlatform
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import kotlinx.coroutines.launch

@Composable
fun KitsugiRankingBottomSheet(
    title: String,
    mediaType: MediaType,
    platform: ExplorePlatform,
    initialResults: List<JikanSearchResult>,
    alreadyInList: (JikanSearchResult) -> Boolean,
    onItemClick: (JikanSearchResult) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    titleLanguage: String = "ROMAJI",
    hideScores: Boolean = false,
    showAdultContent: Boolean = false,
    blurAdultMedia: Boolean = false
) {
    val accentColor = LocalKitsugiAccent.current
    val scope = rememberCoroutineScope()
    val jikanClient = remember { JikanApiClient() }
    val tmdbClient = remember { TmdbApiClient() }

    val isManga = mediaType == MediaType.Manga
    var selectedTab by remember { mutableStateOf("SCORE") } // SCORE, POPULARITY, FAVORITE, UPCOMING
    var resultsList by remember { mutableStateOf(initialResults) }
    var isLoading by remember { mutableStateOf(false) }

    fun loadTabResults(tab: String) {
        selectedTab = tab
        isLoading = true
        scope.launch {
            try {
                val newItems = when (platform) {
                    ExplorePlatform.AniList -> {
                        when (tab) {
                            "SCORE" -> jikanClient.aniListTopAnime(page = 1, showAdultContent = showAdultContent)
                            "POPULARITY" -> jikanClient.aniListTrendingAnime(page = 1, showAdultContent = showAdultContent)
                            "FAVORITE" -> jikanClient.searchAniList(
                                query = "",
                                mediaType = mediaType,
                                showAdultContent = showAdultContent,
                                sort = listOf("FAVOURITES_DESC")
                            )
                            "UPCOMING" -> jikanClient.aniListUpcomingAnime(page = 1, showAdultContent = showAdultContent)
                            else -> initialResults
                        }
                    }
                    ExplorePlatform.MAL -> {
                        when (tab) {
                            "SCORE" -> if (isManga) jikanClient.topManga(1, showAdultContent) else jikanClient.topAnime(1, showAdultContent)
                            "POPULARITY" -> if (isManga) jikanClient.topManga(1, showAdultContent) else jikanClient.trendingAnime(1, showAdultContent)
                            "FAVORITE" -> jikanClient.searchMALOnly(
                                query = "",
                                mediaType = mediaType,
                                showAdultContent = showAdultContent,
                                orderBy = "favorites",
                                sort = "desc"
                            )
                            "UPCOMING" -> jikanClient.upcomingAnime(1, showAdultContent)
                            else -> initialResults
                        }
                    }
                    ExplorePlatform.TMDB -> {
                        when (tab) {
                            "SCORE" -> tmdbClient.getTopRatedShows(1)
                            "POPULARITY" -> tmdbClient.getTrendingShows(1)
                            "FAVORITE" -> tmdbClient.getPopularMovies(1)
                            "UPCOMING" -> tmdbClient.getTrendingAll(1)
                            else -> initialResults
                        }
                    }
                }
                if (newItems.isNotEmpty()) {
                    resultsList = newItems
                }
            } catch (e: Exception) {
                // Keep previous results on error
            } finally {
                isLoading = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = KitsugiColors.Background,
        contentColor = KitsugiColors.TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
        ) {
            // Header bar (Title + Close Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Kapat",
                        tint = KitsugiColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-Tabs Bar (Puan | Popülerlik | Favori | Yakında)
            KitsugiRankingTabs(
                selectedTab = selectedTab,
                isManga = isManga,
                onTabSelected = { tab -> loadTabResults(tab) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Content List
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (resultsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    KitsugiEmptyState(
                        title = "Sonuç Bulunamadı",
                        subtitle = "Seçilen kategori için sıralama verisi mevcut değil.",
                        icon = Icons.Rounded.SearchOff
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    itemsIndexed(
                        items = resultsList,
                        key = { index, item -> "${item.source}_${item.malId}_$index" }
                    ) { index, item ->
                        KitsugiRankingMediaCard(
                            result = item,
                            rankIndex = index + 1,
                            alreadyInList = alreadyInList(item),
                            onClick = {
                                onItemClick(item)
                                onDismissRequest()
                            },
                            titleLanguage = titleLanguage,
                            hideScores = hideScores,
                            blurAdultMedia = blurAdultMedia
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KitsugiRankingTabs(
    selectedTab: String,
    isManga: Boolean,
    onTabSelected: (String) -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val tabs = remember(isManga) {
        if (isManga) {
            listOf("SCORE" to "Puan", "POPULARITY" to "Popülerlik", "FAVORITE" to "Favori")
        } else {
            listOf("SCORE" to "Puan", "POPULARITY" to "Popülerlik", "FAVORITE" to "Favori", "UPCOMING" to "Yakında")
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (tabKey, tabTitle) ->
            val isSelected = selectedTab == tabKey
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTabSelected(tabKey) }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = tabTitle,
                    color = if (isSelected) accentColor else KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(if (isSelected) 32.dp else 0.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor)
                )
            }
        }
    }
}
