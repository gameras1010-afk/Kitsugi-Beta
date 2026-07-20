package com.kitsugi.animelist.ui.theme.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/**
 * Motion and Focus interaction design tokens.
 */
object MotionFocusTokens {
    // Easing Curves
    val EasingStandard: Easing = FastOutSlowInEasing
    val EasingDecelerate: Easing = LinearOutSlowInEasing
    val EasingEmphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    
    // Durations in MS
    const val DurationInstant = 0
    const val DurationQuick = 125
    const val DurationFast = 180
    const val DurationMedium = 350
    const val DurationSlow = 450
    
    // Focus Constants
    const val FocusScale = 1.02f
    const val FocusSubtleScale = 1.01f
    const val FocusPressedScale = 0.98f
    const val FocusDisabledScale = 1.0f
}
