package com.kitsugi.animelist.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.theme.KitsugiColors

/**
 * V2-A02: ContinueWatchingProgressLabel
 *
 * "Devam Et" kartlarında episode bilgisi + ilerleme çubuğu gösteren bileşen.
 * NuvioTV ContinueWatchingProgressLabel.kt referans alındı.
 *
 * @param episodeLabel  "S2E05" veya "Bölüm 5" gibi etiket
 * @param progress      0f..1f arası ilerleme değeri
 * @param remainingText "23 dk kaldı" gibi kalan süre metni (null ise gösterilmez)
 * @param accentColor   İlerleme çubuğu rengi (varsayılan KitsugiColors.Accent)
 */
@Composable
fun ContinueWatchingProgressLabel(
    episodeLabel: String,
    progress: Float,
    modifier: Modifier = Modifier,
    remainingText: String? = null,
    accentColor: Color = KitsugiColors.Accent,
    trackColor: Color = Color.White.copy(alpha = 0.15f),
    barHeight: Dp = 3.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "progressAnim"
    )

    Column(modifier = modifier) {
        // Episode label row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = episodeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = KitsugiColors.TextPrimary,
                    fontSize = 11.sp
                )
            }

            if (remainingText != null) {
                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = KitsugiColors.TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(barHeight)),
            color = accentColor,
            trackColor = trackColor,
            strokeCap = StrokeCap.Round
        )
    }
}

/**
 * Compact variant — yalnızca çubuk, etiket yok. Hero kartları için.
 */
@Composable
fun CompactProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    accentColor: Color = KitsugiColors.Accent,
    trackColor: Color = Color.White.copy(alpha = 0.2f),
    barHeight: Dp = 2.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "compactProgressAnim"
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(barHeight)),
        color = accentColor,
        trackColor = trackColor,
        strokeCap = StrokeCap.Round
    )
}
