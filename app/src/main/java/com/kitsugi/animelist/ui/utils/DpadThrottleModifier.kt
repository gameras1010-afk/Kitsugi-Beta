package com.kitsugi.animelist.ui.utils

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager

/**
 * When true (provided via CompositionLocalProvider at the LazyRow level),
 * horizontal D-pad repeats are throttled at 48 ms instead of [horizontalGateMs].
 * This lets fast-scrolling rows feel snappier while slower rows keep the default gate.
 *
 * Ported from KitsugiTV-dev DpadThrottleModifier.
 */
val LocalFastHorizontalNavigationEnabled = compositionLocalOf { false }

/**
 * Throttles D-pad key repeats to prevent HWUI overload and focus jank
 * when a directional key is held down. Consumes rapid repeats and
 * manually moves focus at a controlled rate.
 *
 * KEY FIX vs previous version:
 * - Two SEPARATE timestamps for horizontal and vertical axes.
 *   The old single-timestamp bug caused horizontal repeats to be swallowed
 *   after a vertical scroll (the 115 ms vertical gate applied to right/left too).
 * - LocalFastHorizontalNavigationEnabled support (48 ms fast lane).
 *
 * @param horizontalGateMs minimum interval between horizontal repeats (default 80 ms)
 * @param verticalGateMs   minimum interval between vertical repeats (default 112 ms)
 */
fun Modifier.dpadRepeatThrottle(
    horizontalGateMs: Long = 80L,
    verticalGateMs: Long = 112L
): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val fastHorizontalNavigationEnabled = LocalFastHorizontalNavigationEnabled.current

    // CRITICAL FIX: separate timestamps per axis so a recent vertical event
    // does NOT gate the next horizontal event and vice-versa.
    val lastHorizontalRepeatTime = remember { longArrayOf(0L) }
    val lastVerticalRepeatTime   = remember { longArrayOf(0L) }

    onPreviewKeyEvent { event ->
        val native = event.nativeKeyEvent
        if (native.action == KeyEvent.ACTION_DOWN &&
            native.repeatCount > 0 &&
            (native.keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                native.keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                native.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                native.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
        ) {
            val isVertical = native.keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                native.keyCode == KeyEvent.KEYCODE_DPAD_UP

            val gateMs = when {
                isVertical                       -> verticalGateMs
                fastHorizontalNavigationEnabled  -> 48L   // fast lane for quick-scroll rows
                else                             -> horizontalGateMs
            }

            val now = SystemClock.uptimeMillis()
            val lastRepeat = if (isVertical) lastVerticalRepeatTime else lastHorizontalRepeatTime

            if (now - lastRepeat[0] < gateMs) {
                return@onPreviewKeyEvent true  // swallow the repeat
            }
            lastRepeat[0] = now

            val direction = when (native.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN  -> FocusDirection.Down
                KeyEvent.KEYCODE_DPAD_UP    -> FocusDirection.Up
                KeyEvent.KEYCODE_DPAD_LEFT  -> FocusDirection.Left
                KeyEvent.KEYCODE_DPAD_RIGHT -> FocusDirection.Right
                else                        -> null
            }
            if (direction != null) focusManager.moveFocus(direction)
            return@onPreviewKeyEvent true
        }
        false
    }
}
