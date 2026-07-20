package com.kitsugi.animelist.ui.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.LocalIsTvDevice
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens

import androidx.compose.foundation.combinedClickable

/**
 * TV-aware clickable modifier for D-pad navigation support.
 *
 * On TV:
 *  - Listens to focus via [onFocusChanged] FIRST (before drawing) so that
 *    border & scale react to the exact moment focus lands.
 *  - Draws an accent-colored border when focused.
 *  - Optionally scales the element up slightly (1.02x) on focus.
 *  - Uses a single [clickable] which already makes the node focusable
 *    — no extra .focusable() to avoid double-focus traversal stops.
 *
 * On Mobile: falls back to a plain [clickable].
 *
 * ⚠️ Modifier order matters:
 *   onFocusChanged → border → scale → clickable
 *   (onFocusChanged must wrap clickable so it sees the focus coming in)
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.tvClickable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    scaleOnFocus: Boolean = true,
    scaleFocused: Float = KitsugiTvTokens.Focus.scale,
    borderWidth: Dp = KitsugiTvTokens.Cards.focusedBorderWidth,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val isTv = LocalIsTvDevice.current
    if (isTv) {
        var isFocused by remember { mutableStateOf(false) }
        val accentColor = LocalKitsugiAccent.current
        val scale by animateFloatAsState(
            targetValue = if (isFocused) scaleFocused else 1.0f,
            animationSpec = KitsugiTvTokens.Motion.focusSpring(),
            label = "tvClickable_scale"
        )

        this
            .onFocusChanged { state -> isFocused = state.isFocused }
            .border(
                width = borderWidth,
                color = if (isFocused) accentColor else Color.Transparent,
                shape = shape
            )
            .then(if (scaleOnFocus) Modifier.scale(scale) else Modifier)
            .combinedClickable(
                enabled = enabled,
                onLongClick = onLongClick,
                onClick = onClick
            )
    } else {
        this.combinedClickable(
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = onClick
        )
    }
}

