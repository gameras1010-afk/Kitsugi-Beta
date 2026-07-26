package com.kitsugi.animelist.ui.screens.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.R
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun TmdbCategoriesSection(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    accentColor: Color,
    filteredTopAnime: List<JikanSearchResult>,
    filteredAiringAnime: List<JikanSearchResult>,
    filteredMovieAnime: List<JikanSearchResult>,
    filteredTopManga: List<JikanSearchResult>,
    filteredUpcomingAnime: List<JikanSearchResult>,
    filteredSeasonalAnime: List<JikanSearchResult>,
    filteredPublishingManga: List<JikanSearchResult>,
    filteredTrendingAnime: List<JikanSearchResult>,
    filteredNewlyAddedAnime: List<JikanSearchResult>,
    filteredTrendingManga: List<JikanSearchResult>,
    onSeeAllSection: (title: String, categoryType: ExploreCategoryType, results: List<JikanSearchResult>) -> Unit,
    onOpenAiringCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KitsugiColors.Surface.copy(alpha = 0.4f))
                .clickable { onExpandedChange(!isExpanded) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.GridView,
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
                imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Genişlet/Daralt",
                tint = KitsugiColors.TextSecondary
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = "Dizi ve Film",
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
                            label = "Trend Her Şey",
                            onClick = { onSeeAllSection("Trend Her Şey", ExploreCategoryType.TOP_ANIME, filteredTopAnime) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = "Trend Diziler",
                            onClick = { onSeeAllSection("Trend Diziler", ExploreCategoryType.AIRING_ANIME, filteredAiringAnime) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = "Trend Filmler",
                            onClick = { onSeeAllSection("Trend Filmler", ExploreCategoryType.MOVIE_ANIME, filteredMovieAnime) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = "Popüler Diziler",
                            onClick = { onSeeAllSection("Popüler Diziler", ExploreCategoryType.TOP_MANGA, filteredTopManga) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = "Popüler Filmler",
                            onClick = { onSeeAllSection("Popüler Filmler", ExploreCategoryType.UPCOMING_ANIME, filteredUpcomingAnime) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = "Yakında Yayında",
                            onClick = onOpenAiringCalendar
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = "En Yüksek Puanlı Diziler",
                            onClick = { onSeeAllSection("En Yüksek Puanlı Diziler", ExploreCategoryType.SEASONAL_ANIME, filteredSeasonalAnime) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = "En Yüksek Puanlı Filmler",
                            onClick = { onSeeAllSection("En Yüksek Puanlı Filmler", ExploreCategoryType.PUBLISHING_MANGA, filteredPublishingManga) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                            label = "Trend Animeler",
                            onClick = { onSeeAllSection("Trend Animeler", ExploreCategoryType.TRENDING_ANIME, filteredTrendingAnime) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = "Popüler Animeler",
                            onClick = { onSeeAllSection("Popüler Animeler", ExploreCategoryType.NEWLY_ADDED_ANIME, filteredNewlyAddedAnime) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = "En Yüksek Puanlı Animeler",
                            onClick = { onSeeAllSection("En Yüksek Puanlı Animeler", ExploreCategoryType.TRENDING_MANGA, filteredTrendingManga) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = "Yakında Yayında",
                            onClick = onOpenAiringCalendar
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = "Yayın Takvimi",
                            onClick = onOpenAiringCalendar
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultCategoriesSection(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    accentColor: Color,
    filteredSeasonalAnime: List<JikanSearchResult>,
    filteredTopAnime: List<JikanSearchResult>,
    filteredTrendingAnime: List<JikanSearchResult>,
    filteredMovieAnime: List<JikanSearchResult>,
    filteredNewlyAddedAnime: List<JikanSearchResult>,
    filteredTopManga: List<JikanSearchResult>,
    filteredPublishingManga: List<JikanSearchResult>,
    filteredTrendingManga: List<JikanSearchResult>,
    filteredNewlyAddedManga: List<JikanSearchResult>,
    onSeeAllSection: (title: String, categoryType: ExploreCategoryType, results: List<JikanSearchResult>) -> Unit,
    onOpenAiringCalendar: () -> Unit,
    onOpenMangaReader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KitsugiColors.Surface.copy(alpha = 0.4f))
                .clickable { onExpandedChange(!isExpanded) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.GridView,
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
                imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Genişlet/Daralt",
                tint = KitsugiColors.TextSecondary
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 4.dp, end = 4.dp)
            ) {
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
                            label = stringResource(R.string.explore_seasonal_anime),
                            onClick = { onSeeAllSection(context.getString(R.string.explore_seasonal_anime), ExploreCategoryType.SEASONAL_ANIME, filteredSeasonalAnime) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = stringResource(R.string.explore_airing_soon),
                            onClick = onOpenAiringCalendar
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = stringResource(R.string.explore_top_anime),
                            onClick = { onSeeAllSection(context.getString(R.string.explore_top_anime), ExploreCategoryType.TOP_ANIME, filteredTopAnime) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = stringResource(R.string.explore_trending_anime),
                            onClick = { onSeeAllSection(context.getString(R.string.explore_trending_anime), ExploreCategoryType.TRENDING_ANIME, filteredTrendingAnime) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = stringResource(R.string.explore_movie_anime),
                            onClick = { onSeeAllSection(context.getString(R.string.explore_movie_anime), ExploreCategoryType.MOVIE_ANIME, filteredMovieAnime) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = stringResource(R.string.explore_newly_added_anime),
                            onClick = { onSeeAllSection(context.getString(R.string.explore_newly_added_anime), ExploreCategoryType.NEWLY_ADDED_ANIME, filteredNewlyAddedAnime) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                            label = stringResource(R.string.explore_top_manga),
                            onClick = { onSeeAllSection(context.getString(R.string.explore_top_manga), ExploreCategoryType.TOP_MANGA, filteredTopManga) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = stringResource(R.string.explore_publishing_manga),
                            onClick = { onSeeAllSection(context.getString(R.string.explore_publishing_manga), ExploreCategoryType.PUBLISHING_MANGA, filteredPublishingManga) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = stringResource(R.string.explore_trending_manga),
                            onClick = { onSeeAllSection(context.getString(R.string.explore_trending_manga), ExploreCategoryType.TRENDING_MANGA, filteredTrendingManga) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = stringResource(R.string.explore_newly_added_manga),
                            onClick = { onSeeAllSection(context.getString(R.string.explore_newly_added_manga), ExploreCategoryType.NEWLY_ADDED_MANGA, filteredNewlyAddedManga) }
                        )
                    }
                    item {
                        ExploreCategoryChip(
                            label = stringResource(R.string.explore_read_manga),
                            onClick = onOpenMangaReader
                        )
                    }
                }
            }
        }
    }
}
