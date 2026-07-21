package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.remote.KitsugiRanking
import com.kitsugi.animelist.data.remote.KitsugiStats
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent

@Composable
fun StatsTabContent(
    state: DetailTabState<KitsugiStats?>,
    source: String = ""
) {
    val isTurkish = java.util.Locale.getDefault().language.equals("tr", ignoreCase = true)

    when (state) {
        is DetailTabState.Loading -> {
            Text(
                text = if (isTurkish) "İstatistikler yükleniyor..." else "Loading statistics...",
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        is DetailTabState.Error -> {
            Text(
                text = if (isTurkish) "İstatistikler yüklenirken hata oluştu." else "Failed to load statistics.",
                color = KitsugiColors.AccentRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        is DetailTabState.Success -> {
            val stats = state.data
            if (stats == null) {
                Text(
                    text = if (isTurkish) "İstatistik verisi bulunamadı." else "No statistics available.",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    // 1. SIRALAMA (RANKINGS)
                    if (stats.rankings.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = if (isTurkish) "Sıralama" else "Rankings",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            stats.rankings.forEach { ranking ->
                                RankingPillItem(ranking)
                            }
                        }
                    }

                    // 2. DURUM DAĞILIMI (STATUS DISTRIBUTION)
                    val watching = stats.watching ?: 0
                    val planned = stats.planned ?: 0
                    val completed = stats.completed ?: 0
                    val dropped = stats.dropped ?: 0
                    val paused = stats.paused ?: 0
                    val totalStatus = watching + planned + completed + dropped + paused
                    val safeTotal = if (totalStatus > 0) totalStatus else 1

                    if (totalStatus > 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = if (isTurkish) "Durum Dağılımı" else "Status Distribution",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            // Horizontal scrollable pill badges
                            val scrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (watching > 0) {
                                    StatusBadge(amount = watching, label = if (isTurkish) "Şimdiki" else "Current", color = Color(0xFF8CE637))
                                }
                                if (planned > 0) {
                                    StatusBadge(amount = planned, label = if (isTurkish) "Planlanan" else "Planning", color = Color(0xFFC08A6E))
                                }
                                if (completed > 0) {
                                    StatusBadge(amount = completed, label = if (isTurkish) "Tamamlanan" else "Completed", color = Color(0xFF5B8EFF))
                                }
                                if (dropped > 0) {
                                    StatusBadge(amount = dropped, label = if (isTurkish) "Bırakıldı" else "Dropped", color = Color(0xFFFF5252))
                                }
                                if (paused > 0) {
                                    StatusBadge(amount = paused, label = if (isTurkish) "Ara Verildi" else "Paused", color = Color(0xFF909399))
                                }
                            }

                            // Stacked horizontal progress bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(CircleShape)
                                    .background(KitsugiColors.SurfaceSoft)
                            ) {
                                if (watching > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(watching.toFloat() / safeTotal)
                                            .background(Color(0xFF8CE637))
                                    )
                                }
                                if (planned > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(planned.toFloat() / safeTotal)
                                            .background(Color(0xFFC08A6E))
                                    )
                                }
                                if (completed > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(completed.toFloat() / safeTotal)
                                            .background(Color(0xFF5B8EFF))
                                    )
                                }
                                if (dropped > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(dropped.toFloat() / safeTotal)
                                            .background(Color(0xFFFF5252))
                                    )
                                }
                                if (paused > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(paused.toFloat() / safeTotal)
                                            .background(Color(0xFF909399))
                                    )
                                }
                            }

                            Text(
                                text = "${if (isTurkish) "Toplam" else "Total"}: ${formatNumber(totalStatus)}",
                                color = KitsugiColors.TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 3. PUAN DAĞILIMI (SCORE DISTRIBUTION)
                    if (stats.scoreDistribution.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = if (isTurkish) "Puan Dağılımı" else "Score Distribution",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            val scoreMap = stats.scoreDistribution.associate { it.score to it.amount }
                            val maxVotes = (stats.scoreDistribution.maxOfOrNull { it.amount } ?: 1).coerceAtLeast(1)

                            val scoresToDisplay = if (scoreMap.keys.any { it > 10 }) {
                                (10..100 step 10).toList()
                            } else {
                                (1..10).toList()
                            }

                            val chartScrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(chartScrollState)
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                scoresToDisplay.forEach { score ->
                                    val amount = scoreMap[score] ?: 0
                                    ScoreColumnItem(score = score, amount = amount, maxVotes = maxVotes)
                                }
                            }
                        }
                    }

                    if (stats.rankings.isEmpty() && totalStatus <= 0 && stats.scoreDistribution.isEmpty()) {
                        Text(
                            text = if (isTurkish) "İstatistik verisi bulunamadı." else "No statistics available.",
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
private fun RankingPillItem(ranking: KitsugiRanking) {
    val isPopular = ranking.type.equals("POPULAR", ignoreCase = true) || ranking.context.lowercase().contains("popular")
    val formattedContext = formatRankingContext(ranking)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, KitsugiColors.Border.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isPopular) Icons.Rounded.FavoriteBorder else Icons.Rounded.StarBorder,
            contentDescription = null,
            tint = if (isPopular) KitsugiColors.AccentRed else KitsugiColors.AccentOrange,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "#${ranking.rank} $formattedContext",
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatusBadge(amount: Int, label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${formatNumber(amount)} ",
            color = Color.Black.copy(alpha = 0.85f),
            fontWeight = FontWeight.Black,
            fontSize = 13.sp
        )
        Text(
            text = label,
            color = Color.Black.copy(alpha = 0.85f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ScoreColumnItem(score: Int, amount: Int, maxVotes: Int) {
    val accentColor = LocalKitsugiAccent.current
    val barColor = when (score) {
        10, 1 -> Color(0xFFFB7185)
        20, 2 -> Color(0xFFFB923C)
        30, 3 -> Color(0xFFFBBF24)
        40, 4 -> Color(0xFFFACC15)
        50, 5 -> Color(0xFFA3E635)
        60, 6 -> Color(0xFF60A5FA)
        70, 7 -> Color(0xFF818CF8)
        else  -> accentColor
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(36.dp)
    ) {
        Text(
            text = if (amount > 0) formatCompactNumber(amount) else "",
            color = KitsugiColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .height(110.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val barRatio = (amount.toFloat() / maxVotes).coerceIn(0.04f, 1f)
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .fillMaxHeight(barRatio)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                    .background(barColor)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$score",
            color = KitsugiColors.TextMuted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatRankingContext(ranking: KitsugiRanking): String {
    val isTurkish = java.util.Locale.getDefault().language.equals("tr", ignoreCase = true)
    val ctxLower = ranking.context.lowercase().trim()
    val isPopular = ranking.type.equals("POPULAR", ignoreCase = true) || ctxLower.contains("popular")
    val isAllTime = ranking.allTime || ctxLower.contains("all time")

    if (isTurkish) {
        if (isAllTime) {
            return if (isPopular) "Tüm Zamanların En Popüleri" else "Tüm Zamanların En Yüksek Puanlısı"
        }

        val seasonTr = when (ranking.season?.uppercase() ?: run {
            when {
                ctxLower.contains("winter") -> "Kış"
                ctxLower.contains("spring") -> "İlkbahar"
                ctxLower.contains("summer") -> "Yaz"
                ctxLower.contains("fall") -> "Sonbahar"
                else -> null
            }
        }) {
            "WINTER", "KIŞ" -> "Kış"
            "SPRING", "İLKBAHAR" -> "İlkbahar"
            "SUMMER", "YAZ" -> "Yaz"
            "FALL", "SONBAHAR" -> "Sonbahar"
            else -> null
        }

        val year = ranking.year ?: Regex("\\b(19|20)\\d{2}\\b").find(ranking.context)?.value?.toIntOrNull()

        if (seasonTr != null && year != null) {
            return if (isPopular) "$year $seasonTr Sezonunun En Popüleri" else "$year $seasonTr Sezonunun En Yüksek Puanlısı"
        }

        if (year != null) {
            return if (isPopular) "$year Yılının En Popüleri" else "$year Yılının En Yüksek Puanlısı"
        }

        return if (isPopular) "En Popüler" else "En Yüksek Puanlı"
    } else {
        if (isAllTime) {
            return if (isPopular) "Most Popular All Time" else "Highest Rated All Time"
        }

        val seasonEn = when (ranking.season?.uppercase() ?: run {
            when {
                ctxLower.contains("winter") -> "Winter"
                ctxLower.contains("spring") -> "Spring"
                ctxLower.contains("summer") -> "Summer"
                ctxLower.contains("fall") -> "Fall"
                else -> null
            }
        }) {
            "WINTER", "KIŞ" -> "Winter"
            "SPRING", "İLKBAHAR" -> "Spring"
            "SUMMER", "YAZ" -> "Summer"
            "FALL", "SONBAHAR" -> "Fall"
            else -> null
        }

        val year = ranking.year ?: Regex("\\b(19|20)\\d{2}\\b").find(ranking.context)?.value?.toIntOrNull()

        if (seasonEn != null && year != null) {
            return if (isPopular) "Most Popular $seasonEn $year" else "Highest Rated $seasonEn $year"
        }

        if (year != null) {
            return if (isPopular) "Most Popular $year" else "Highest Rated $year"
        }

        return if (isPopular) "Most Popular" else "Highest Rated"
    }
}

private fun formatNumber(number: Int): String {
    val isTurkish = java.util.Locale.getDefault().language.equals("tr", ignoreCase = true)
    val locale = if (isTurkish) java.util.Locale("tr", "TR") else java.util.Locale.US
    return java.text.NumberFormat.getInstance(locale).format(number)
}

private fun formatCompactNumber(number: Int): String {
    return if (number >= 1_000_000) {
        String.format(java.util.Locale.US, "%.1fM", number / 1_000_000.0)
    } else if (number >= 10_000) {
        String.format(java.util.Locale.US, "%.1fK", number / 1_000.0)
    } else {
        formatNumber(number)
    }
}
