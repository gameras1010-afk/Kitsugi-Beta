package com.kitsugi.animelist.ui.utils

import android.view.KeyEvent
import android.view.ViewConfiguration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

// ── FocusMarqueeText ─────────────────────────────────────────────────────────
// 45.dp/s ≈ 5.3 chars/sec — comfortable and fast enough to read long titles
// ported from KitsugiTV-dev FocusMarqueeText (research: 95% comprehension ≤8.5 cps)
private val MarqueeVelocity = 45.dp

/**
 * Single-line TV text that marquees horizontally while [focused] when content
 * overflows, and otherwise ellipsizes. Long anime titles become fully readable
 * when the card is focused.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FocusMarqueeText(
    text: String,
    focused: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = if (focused) {
            modifier.basicMarquee(iterations = Int.MAX_VALUE, velocity = MarqueeVelocity)
        } else {
            modifier
        },
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = if (focused) TextOverflow.Clip else TextOverflow.Ellipsis,
        textAlign = textAlign,
    )
}

// ── PlaceholderShimmer ────────────────────────────────────────────────────────

private const val SHIMMER_DISTANCE_PX = 1000f
private const val SHIMMER_WIDTH_FRACTION = 0.6f
private val SHIMMER_COLOR_STOPS = arrayOf(
    0.0f to Color.Transparent,
    0.4f to Color.White.copy(alpha = 0.07f),
    0.5f to Color.White.copy(alpha = 0.13f),
    0.6f to Color.White.copy(alpha = 0.07f),
    1.0f to Color.Transparent
)

/**
 * Returns an animated [State<Float>] that drives a shimmer sweep.
 * Share one instance across all placeholder items in the same LazyGrid to keep
 * shimmers in-sync and minimize animation overhead.
 */
@Composable
fun rememberPlaceholderShimmerOffsetState(label: String = "shimmer"): State<Float> {
    val transition = rememberInfiniteTransition(label = label)
    return transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
}

/**
 * Draws a smooth shimmer sweep on top of this composable.
 * Apply to skeleton/placeholder items while content is loading.
 */
fun Modifier.placeholderCardShimmer(
    shimmerOffsetState: State<Float>,
    backgroundColor: Color? = null
): Modifier = drawWithCache {
    onDrawBehind {
        backgroundColor?.let { drawRect(color = it) }
        val off = shimmerOffsetState.value
        drawRect(
            brush = Brush.linearGradient(
                colorStops = SHIMMER_COLOR_STOPS,
                start = Offset(off * SHIMMER_DISTANCE_PX, 0f),
                end = Offset(
                    (off + SHIMMER_WIDTH_FRACTION) * SHIMMER_DISTANCE_PX,
                    0f
                )
            )
        )
    }
}


/**
 * Requests focus after waiting for recomposition frames, handling rendering delays.
 */
suspend fun androidx.compose.ui.focus.FocusRequester.requestFocusAfterFrames(frames: Int = 2) {
    repeat(frames.coerceAtLeast(0)) {
        androidx.compose.runtime.withFrameNanos { }
    }
    repeat(4) { attempt ->
        val requested = runCatching {
            requestFocus()
            true
        }.getOrDefault(false)
        if (requested) return
        if (attempt < 3) {
            androidx.compose.runtime.withFrameNanos { }
        }
    }
}
