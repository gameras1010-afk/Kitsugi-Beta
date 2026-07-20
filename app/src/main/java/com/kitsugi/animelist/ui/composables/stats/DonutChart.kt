package com.kitsugi.animelist.ui.composables.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.KitsugiColors

/**
 * T3.6: DonutChart
 *
 * MoeList DonutChart.kt referansından adapte edildi.
 * Animasyonlu halka grafik — durum dağılımını göstermek için kullanılır
 * (İzleniyor, Tamamlandı, Beklemede, Bırakıldı, Planlanıyor).
 *
 * @param slices Dilim listesi: (renk, oran 0f..1f, etiket)
 * @param centerLabel Ortadaki büyük metin (toplam sayı vb.)
 * @param centerSubLabel Ortadaki küçük metin
 * @param size Grafik boyutu
 * @param strokeWidth Halka kalınlığı
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    centerLabel: String = "",
    centerSubLabel: String = "",
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = 20.dp,
    animDuration: Int = 900
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animDuration, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
            val diameter = this.size.minDimension - strokeWidth.toPx()
            val topLeft = Offset(
                x = (this.size.width - diameter) / 2f,
                y = (this.size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            var startAngle = -90f
            val total = slices.sumOf { it.fraction.toDouble() }.toFloat().coerceAtLeast(0.001f)

            slices.forEach { slice ->
                val sweep = (slice.fraction / total) * 360f * animProgress.value
                if (sweep > 0f) {
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                    startAngle += sweep
                }
            }

            // Arka plan halkası (boş)
            if (slices.isEmpty()) {
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }
        }

        // Merkez metin
        if (centerLabel.isNotBlank() || centerSubLabel.isNotBlank()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (centerLabel.isNotBlank()) {
                    Text(
                        text = centerLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = KitsugiColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
                if (centerSubLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = centerSubLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = KitsugiColors.TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Tek bir donut dilimi.
 * @param color Dilim rengi
 * @param fraction Oran (0f..1f) — toplam içindeki pay (normalize edilir)
 * @param label Etiket metni
 */
data class DonutSlice(
    val color: Color,
    val fraction: Float,
    val label: String = ""
)
