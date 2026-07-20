package com.kitsugi.animelist.ui.components.shimmer

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * V2-A04 – PlaceholderShimmer
 *
 * Yükleme sırasında içerik yerine gösterilen animasyonlu iskelet/shimmer sistemi.
 * NuvioTV PlaceholderShimmer.kt referans alındı.
 */

private val ShimmerColors = listOf(
    Color(0xFF1C1C1E),
    Color(0xFF2C2C2E),
    Color(0xFF3C3C3E),
    Color(0xFF2C2C2E),
    Color(0xFF1C1C1E)
)

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    return Brush.linearGradient(
        colors = ShimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )
}

/**
 * Tek bir shimmer kutu placeholder.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    brush: Brush = rememberShimmerBrush()
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

/**
 * Poster kart iskelet - dikey poster boyutu.
 */
@Composable
fun PosterSkeleton(
    modifier: Modifier = Modifier
) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.width(100.dp)) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            cornerRadius = 10.dp,
            brush = brush
        )
        Spacer(modifier = Modifier.height(6.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(10.dp),
            cornerRadius = 4.dp,
            brush = brush
        )
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(8.dp),
            cornerRadius = 4.dp,
            brush = brush
        )
    }
}

/**
 * Liste satırı iskelet - yatay liste öğesi.
 */
@Composable
fun ListRowSkeleton(
    modifier: Modifier = Modifier
) {
    val brush = rememberShimmerBrush()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(
            modifier = Modifier.size(64.dp),
            cornerRadius = 8.dp,
            brush = brush
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp),
                cornerRadius = 4.dp,
                brush = brush
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(10.dp),
                cornerRadius = 4.dp,
                brush = brush
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(10.dp),
                cornerRadius = 4.dp,
                brush = brush
            )
        }
    }
}

/**
 * Hero banner iskelet - detay ekranı üst bölümü.
 */
@Composable
fun HeroBannerSkeleton(
    modifier: Modifier = Modifier
) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth()) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            cornerRadius = 0.dp,
            brush = brush
        )
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp),
                cornerRadius = 6.dp,
                brush = brush
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp),
                cornerRadius = 4.dp,
                brush = brush
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(80.dp)
                            .height(32.dp),
                        cornerRadius = 16.dp,
                        brush = brush
                    )
                }
            }
        }
    }
}

/**
 * Genel içerik yükleme durumu - N adet liste satırı iskeleti.
 */
@Composable
fun ContentLoadingSkeleton(
    itemCount: Int = 6,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(itemCount) {
            ListRowSkeleton()
        }
    }
}
