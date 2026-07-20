package com.kitsugi.animelist.ui.screens.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.components.KitsugiEmptyState
import com.kitsugi.animelist.ui.components.stats.DonutSegment
import com.kitsugi.animelist.ui.components.stats.KitsugiDonutChart
import com.kitsugi.animelist.ui.components.stats.KitsugiHorizontalStatsBar
import com.kitsugi.animelist.ui.components.stats.StatsChartColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent

/**
 * T3.6 – İstatistikler Ekranı.
 * Kullanıcının anime/manga listesindeki istatistikleri DonutChart ve HorizontalStatsBar
 * ile görselleştirir.
 */
@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.kitsugi.animelist.ui.theme.KitsugiColors.Background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Geri",
                    tint = com.kitsugi.animelist.ui.theme.KitsugiColors.TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "İstatistikler",
                color = com.kitsugi.animelist.ui.theme.KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

        // ─── Özet Kartları ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = !uiState.isLoading,
            enter = fadeIn() + slideInVertically { it / 3 }
        ) {
            Column {
                // Özet Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatsSummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.PlayCircle,
                        iconColor = StatsChartColors.Watching,
                        label = "Anime",
                        value = uiState.totalAnime.toString()
                    )
                    StatsSummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.MenuBook,
                        iconColor = StatsChartColors.Completed,
                        label = "Manga",
                        value = uiState.totalManga.toString()
                    )
                    StatsSummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Favorite,
                        iconColor = Color(0xFFE91E63),
                        label = "Favori",
                        value = uiState.totalFavorites.toString()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatsSummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Slideshow,
                        iconColor = StatsChartColors.Paused,
                        label = "Bölüm",
                        value = uiState.totalEpisodesWatched.toString()
                    )
                    StatsSummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.AccessTime,
                        iconColor = Color(0xFF00BCD4),
                        label = "Gün",
                        value = "%.1f".format(uiState.daysWatched)
                    )
                    StatsSummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Star,
                        iconColor = Color(0xFFFFC107),
                        label = "Ort. Puan",
                        value = if (uiState.meanScore > 0) "%.1f".format(uiState.meanScore) else "-"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ─── Anime Durum Dağılımı ────────────────────────────────────────
                if (uiState.animeStatusStats.isNotEmpty()) {
                    StatsSection(title = "Anime Durum Dağılımı") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val totalAnime = uiState.animeStatusStats.sumOf { it.count }
                            KitsugiDonutChart(
                                segments = uiState.animeStatusStats.map { stat ->
                                    DonutSegment(
                                        label = stat.status.label,
                                        count = stat.count,
                                        color = statusColor(stat.status)
                                    )
                                },
                                centerText = totalAnime.toString(),
                                centerSubText = "toplam",
                                modifier = Modifier.size(160.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                uiState.animeStatusStats.forEach { stat ->
                                    StatsLegendRow(
                                        label = stat.status.label,
                                        count = stat.count,
                                        color = statusColor(stat.status)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ─── Anime Puan Dağılımı ─────────────────────────────────────────
                if (uiState.animeScoreStats.isNotEmpty()) {
                    StatsSection(title = "Anime Puan Dağılımı") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            val maxCount = uiState.animeScoreStats.maxOfOrNull { it.count } ?: 1
                            uiState.animeScoreStats.sortedByDescending { it.score }.forEach { stat ->
                                KitsugiHorizontalStatsBar(
                                    label = "${stat.score} Puan",
                                    count = stat.count,
                                    maxCount = maxCount,
                                    barColor = StatsChartColors.ScoreColors.getOrElse(stat.score - 1) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ─── Manga Durum Dağılımı ────────────────────────────────────────
                if (uiState.mangaStatusStats.isNotEmpty()) {
                    StatsSection(title = "Manga Durum Dağılımı") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val totalManga = uiState.mangaStatusStats.sumOf { it.count }
                            KitsugiDonutChart(
                                segments = uiState.mangaStatusStats.map { stat ->
                                    DonutSegment(
                                        label = stat.status.label,
                                        count = stat.count,
                                        color = statusColor(stat.status)
                                    )
                                },
                                centerText = totalManga.toString(),
                                centerSubText = "toplam",
                                modifier = Modifier.size(160.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                uiState.mangaStatusStats.forEach { stat ->
                                    StatsLegendRow(
                                        label = stat.status.label,
                                        count = stat.count,
                                        color = statusColor(stat.status)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // T2-05: Standart boş durum bileşeni
                if (uiState.totalAnime == 0 && uiState.totalManga == 0) {
                    KitsugiEmptyState(
                        title = "İstatistik için liste gerekli",
                        subtitle = "Anime veya manga ekledikten sonra istatistikler burada görünür.",
                        icon = Icons.Rounded.BarChart
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // T2-05: Yükleme durumu — accent renkli merkezi spinner
    if (uiState.isLoading) {
        val accentColor = LocalKitsugiAccent.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = accentColor,
                strokeWidth = 3.dp
            )
        }
    }
}
}

@Composable
private fun StatsSummaryCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun StatsLegendRow(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun statusColor(status: WatchStatus): Color = when (status) {
    WatchStatus.Watching   -> StatsChartColors.Watching
    WatchStatus.Repeating  -> StatsChartColors.Watching  // Rewatching = aktif izleme rengi
    WatchStatus.Completed  -> StatsChartColors.Completed
    WatchStatus.Planned    -> StatsChartColors.Planned
    WatchStatus.Dropped    -> StatsChartColors.Dropped
    WatchStatus.Paused     -> StatsChartColors.Paused
}
