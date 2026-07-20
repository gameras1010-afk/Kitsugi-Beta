package com.kitsugi.animelist.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * V2-A02: FocusMarqueeText
 *
 * Odaklanıldığında (TV d-pad focus) metin kayan marquee animasyonuna geçer.
 * Odak olmadığında tek satırda ellipsis ile gösterilir.
 *
 * NuvioTV FocusMarqueeText.kt referans alındı.
 */
@Composable
fun FocusMarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    focusColor: Color = Color.Unspecified,
    gradientColor: Color = Color.Black,
    scrollDurationMs: Int = 4000,
    delayMs: Int = 1000
) {
    var isFocused by remember { mutableStateOf(false) }
    val transition = rememberInfiniteTransition(label = "marquee")

    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = scrollDurationMs,
                delayMillis = delayMs,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "marqueeOffset"
    )

    SubcomposeLayout(
        modifier = modifier
            .clipToBounds()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) { constraints ->
        val containerWidth = constraints.maxWidth

        // Measure the full text width
        val textPlaceable = subcompose("text") {
            Text(
                text = text,
                style = style,
                color = if (isFocused && focusColor != Color.Unspecified) focusColor else color,
                fontWeight = fontWeight,
                softWrap = false,
                maxLines = 1
            )
        }.first().measure(constraints.copy(maxWidth = Int.MAX_VALUE))

        val textWidth = textPlaceable.width
        val overflowAmount = textWidth - containerWidth

        // Only animate if text overflows and focused
        val scrollX = if (isFocused && overflowAmount > 0) {
            -(offset * (overflowAmount + 40.dp.toPx())).roundToInt()
        } else {
            0
        }

        layout(containerWidth, textPlaceable.height) {
            if (!isFocused || overflowAmount <= 0) {
                // Static ellipsis text when not focused
                val staticPlaceable = subcompose("static") {
                    Text(
                        text = text,
                        style = style,
                        color = if (isFocused && focusColor != Color.Unspecified) focusColor else color,
                        fontWeight = fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }.first().measure(constraints)
                staticPlaceable.place(0, 0)
            } else {
                // Scrolling text when focused
                textPlaceable.place(scrollX, 0)

                // Gradient fade edges
                subcompose("fade") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        gradientColor,
                                        Color.Transparent,
                                        Color.Transparent,
                                        gradientColor
                                    )
                                )
                            )
                    )
                }.first().measure(constraints).place(0, 0)
            }
        }
    }
}
