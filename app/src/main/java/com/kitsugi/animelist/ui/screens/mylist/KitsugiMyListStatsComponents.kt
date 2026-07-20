package com.kitsugi.animelist.ui.screens.mylist

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import java.util.Locale

internal data class ListStats(
    val totalCount: Int,
    val watchingCount: Int,
    val completedCount: Int,
    val plannedCount: Int,
    val droppedCount: Int,
    val favoriteCount: Int,
    val averageScoreText: String,
    val totalProgress: Int,
    val apiCount: Int
) {
    companion object {
        fun from(entries: List<MediaEntry>): ListStats {
            val scoredEntries = entries.mapNotNull { it.score }
            val averageScore = if (scoredEntries.isEmpty()) {
                "-"
            } else {
                String.format(
                    Locale.US,
                    "%.1f",
                    scoredEntries.average()
                )
            }

            return ListStats(
                totalCount = entries.size,
                watchingCount = entries.count { it.status == WatchStatus.Watching },
                completedCount = entries.count { it.status == WatchStatus.Completed },
                plannedCount = entries.count { it.status == WatchStatus.Planned },
                droppedCount = entries.count { it.status == WatchStatus.Dropped },
                favoriteCount = entries.count { it.isFavorite },
                averageScoreText = averageScore,
                totalProgress = entries.sumOf { it.progress },
                apiCount = entries.count { it.source != "manual" }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ListStatsHeader(
    stats: ListStats,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(KitsugiColors.Surface)
            .padding(14.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 3
        ) {
            StatCard("Toplam", stats.totalCount.toString(), Modifier.weight(1f))
            StatCard("İzleniyor", stats.watchingCount.toString(), Modifier.weight(1f))
            StatCard("Favori", stats.favoriteCount.toString(), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(KitsugiColors.SurfaceSoft)
                .tvClickable(shape = RoundedCornerShape(999.dp), onClick = onToggleExpanded)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (expanded) "Detayları Gizle" else "Detayları Göster",
                color = LocalKitsugiAccent.current,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 3
            ) {
                StatCard("Biten", stats.completedCount.toString(), Modifier.weight(1f))
                StatCard("Plan", stats.plannedCount.toString(), Modifier.weight(1f))
                StatCard("Bırakıldı", stats.droppedCount.toString(), Modifier.weight(1f))
                StatCard("Ort. Puan", stats.averageScoreText, Modifier.weight(1f))
                StatCard("İlerleme", stats.totalProgress.toString(), Modifier.weight(1f))
                StatCard("Kaynaklı", stats.apiCount.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = KitsugiColors.SurfaceSoft
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = LocalKitsugiAccent.current,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = title,
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
