package com.kitsugi.animelist.ui.composables.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.KitsugiColors

/**
 * T3.6: HorizontalStatsBar
 *
 * MoeList HorizontalStatsBar.kt referansından adapte edildi.
 * Yatay segmentli bar — her segment bir duruma karşılık gelir.
 * Donut Chart'ı tamamlar; legend olarak da kullanılabilir.
 *
 * @param segments Bar segmentleri (renk + oran + etiket + değer)
 * @param height Bar yüksekliği
 * @param showLegend Altta açıklama listesi göster
 */
@Composable
fun HorizontalStatsBar(
    segments: List<StatBarSegment>,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    cornerRadius: Dp = 6.dp,
    showLegend: Boolean = true,
    animDuration: Int = 800
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(segments) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animDuration, easing = FastOutSlowInEasing)
        )
    }

    val total = segments.sumOf { it.fraction.toDouble() }.toFloat().coerceAtLeast(0.001f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(cornerRadius)),
            horizontalArrangement = Arrangement.Start
        ) {
            segments.forEachIndexed { index, segment ->
                val normalizedFraction = (segment.fraction / total) * animProgress.value
                if (normalizedFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(normalizedFraction)
                            .height(height)
                            .background(segment.color)
                    )
                }
            }
        }

        // Legend
        if (showLegend && segments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            // Satır başına 2 item
            val chunked = segments.chunked(2)
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { seg ->
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(seg.color)
                            )
                            Text(
                                text = seg.label,
                                color = KitsugiColors.TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                            if (seg.count > 0) {
                                Text(
                                    text = seg.count.toString(),
                                    color = KitsugiColors.TextMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    // Boş hücre doldurucu (tek sayı ise)
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * HorizontalStatsBar segmenti.
 * @param color Segment rengi
 * @param fraction Toplam içindeki oran (normalize edilir)
 * @param label Etiket
 * @param count Gösterilecek sayısal değer (legend'da)
 */
data class StatBarSegment(
    val color: Color,
    val fraction: Float,
    val label: String,
    val count: Int = 0
)

/**
 * StatChip — tek istatistik değeri için küçük kutucuk.
 * MoeList StatChip.kt referansından adapte edildi.
 */
@Composable
fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = KitsugiColors.TextPrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KitsugiColors.Surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = KitsugiColors.TextMuted
        )
    }
}
