package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.remote.KitsugiStats
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun StatsTabContent(
    state: DetailTabState<KitsugiStats?>,
    source: String = ""
) {
    val isTmdb = source.equals("tmdb", ignoreCase = true) || source.equals("simkl", ignoreCase = true)
    when (state) {
        is DetailTabState.Loading -> {
            Text(
                text = "İstatistikler yükleniyor...",
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        is DetailTabState.Error -> {
            Text(
                text = "İstatistikler yüklenirken hata oluştu.",
                color = KitsugiColors.AccentRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        is DetailTabState.Success -> {
            val stats = state.data
            if (stats == null) {
                Text(
                    text = "İstatistik verisi bulunamadı.",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val hasWatching = stats.watching != null && stats.watching > 0
                    val hasCompleted = stats.completed != null && stats.completed > 0
                    val hasPlanned = stats.planned != null && stats.planned > 0
                    val hasDropped = stats.dropped != null && stats.dropped > 0
                    val hasAnyStatus = hasWatching || hasCompleted || hasPlanned || hasDropped

                    if (hasAnyStatus) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(KitsugiColors.Surface)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (isTmdb) "TMDB Verisi" else "İzleme/Okuma Durumları",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val totalStatus = (stats.watching ?: 0) + (stats.completed ?: 0) + (stats.planned ?: 0) + (stats.dropped ?: 0)
                            val safeTotal = if (totalStatus > 0) totalStatus else 1

                            if (hasWatching) {
                                StatusStatRow(
                                    label = if (isTmdb) "Popülerlik Skoru" else "İzleniyor / Okunuyor",
                                    amount = stats.watching ?: 0,
                                    color = KitsugiColors.AccentBlue,
                                    total = safeTotal
                                )
                            }
                            if (hasCompleted) {
                                StatusStatRow(
                                    label = if (isTmdb) "Toplam Oy" else "Tamamlandı",
                                    amount = stats.completed ?: 0,
                                    color = KitsugiColors.AccentGreen,
                                    total = safeTotal
                                )
                            }
                            if (hasPlanned) {
                                StatusStatRow("Planlandı", stats.planned ?: 0, KitsugiColors.AccentOrange, safeTotal)
                            }
                            if (hasDropped) {
                                StatusStatRow("Bırakıldı", stats.dropped ?: 0, KitsugiColors.AccentRed, safeTotal)
                            }
                        }
                    }

                    if (stats.scoreDistribution.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(KitsugiColors.Surface)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Puan Dağılımı",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val maxVotes = stats.scoreDistribution.maxOfOrNull { it.amount } ?: 1
                            val safeMaxVotes = if (maxVotes > 0) maxVotes else 1

                            stats.scoreDistribution.sortedByDescending { it.score }.forEach { scoreStat ->
                                ScoreStatRow(scoreStat.score, scoreStat.amount, safeMaxVotes)
                            }
                        }
                    }

                    if (!hasAnyStatus && stats.scoreDistribution.isEmpty()) {
                        Text(
                            text = "İstatistik verisi bulunamadı.",
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusStatRow(label: String, amount: Int, color: Color, total: Int) {
    val percentage = (amount.toFloat() / total * 100).toInt()
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text("$amount kişi (%$percentage)", color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(KitsugiColors.SurfaceSoft)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(amount.toFloat() / total)
                    .background(color)
            )
        }
    }
}

@Composable
private fun ScoreStatRow(score: Int, amount: Int, maxVotes: Int) {
    val accentColor = LocalKitsugiAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$score ★",
            color = KitsugiColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(KitsugiColors.SurfaceSoft)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(amount.toFloat() / maxVotes)
                    .background(accentColor)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = amount.toString(),
            color = KitsugiColors.TextMuted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp)
        )
    }
}
