package com.kitsugi.animelist.ui.components.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * T3.6 – MoeList DonutChart portu.
 * Durum dağılımını (WatchStatus) animasyonlu donut grafiği olarak gösterir.
 */
@Composable
fun KitsugiDonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 22.dp,
    centerText: String = "",
    centerSubText: String = ""
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(segments) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasSize = this.size.minDimension
            val strokePx = strokeWidth.toPx()
            val radius = (canvasSize - strokePx) / 2
            val topLeft = Offset(strokePx / 2, strokePx / 2)
            val arcSize = Size(radius * 2, radius * 2)

            var startAngle = -90f
            val totalCount = segments.sumOf { it.count }.coerceAtLeast(1)

            segments.forEach { segment ->
                val sweepAngle = (segment.count.toFloat() / totalCount) * 360f * animatedProgress.value
                drawArc(
                    color = segment.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle - 1.5f, // küçük boşluk
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
            }
        }

        if (centerText.isNotBlank()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (centerSubText.isNotBlank()) {
                    Text(
                        text = centerSubText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class DonutSegment(
    val label: String,
    val count: Int,
    val color: Color
)

/**
 * T3.6 – MoeList HorizontalStatsBar portu.
 * Puan dağılımını animasyonlu yatay bar olarak gösterir.
 */
@Composable
fun KitsugiHorizontalStatsBar(
    label: String,
    count: Int,
    maxCount: Int,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedWidth = remember(count, maxCount) { Animatable(0f) }
    LaunchedEffect(count, maxCount) {
        animatedWidth.animateTo(
            targetValue = if (maxCount > 0) count.toFloat() / maxCount else 0f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            // Background track
            drawRoundRect(
                color = barColor.copy(alpha = 0.2f),
                size = this.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            // Filled bar
            drawRoundRect(
                color = barColor,
                size = this.size.copy(width = this.size.width * animatedWidth.value),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
        }
    }
}

/**
 * T3.6 – Renk paleti: Durum dağılımı için standart renkler.
 */
object StatsChartColors {
    val Watching = Color(0xFF4CAF50)
    val Completed = Color(0xFF2196F3)
    val Planned = Color(0xFF9C27B0)
    val Dropped = Color(0xFFF44336)
    val Paused = Color(0xFFFF9800)

    val ScoreColors = listOf(
        Color(0xFFF44336),
        Color(0xFFFF5722),
        Color(0xFFFF9800),
        Color(0xFFFFC107),
        Color(0xFFFFEB3B),
        Color(0xFFCDDC39),
        Color(0xFF8BC34A),
        Color(0xFF4CAF50),
        Color(0xFF009688),
        Color(0xFF2196F3)
    )
}
