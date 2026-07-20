package com.kitsugi.animelist.utils

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs

/**
 * A state holder to track vertical scroll changes and manage UI element visibility.
 * Implements an accumulator threshold to avoid micro-flickering during minor finger jitters.
 */
@Stable
class ScrollVisibilityState(
    initialVisible: Boolean = true
) {
    var isVisible by mutableStateOf(initialVisible)
        internal set

    private var accumulatedDelta = 0f
    private val threshold = 25f // Minimum scroll delta (in pixels) before toggling visibility

    fun onScroll(deltaY: Float) {
        if (deltaY > 0f) {
            // Scrolling up (dragging finger down, content moves down)
            if (accumulatedDelta < 0f) accumulatedDelta = 0f
            accumulatedDelta += deltaY
            if (accumulatedDelta > threshold) {
                isVisible = true
            }
        } else if (deltaY < 0f) {
            // Scrolling down (dragging finger up, content moves up)
            if (accumulatedDelta > 0f) accumulatedDelta = 0f
            accumulatedDelta += deltaY
            if (abs(accumulatedDelta) > threshold) {
                isVisible = false
            }
        }
    }

    fun show() {
        isVisible = true
        accumulatedDelta = 0f
    }

    fun hide() {
        isVisible = false
        accumulatedDelta = 0f
    }
}

/**
 * Creates and remembers a [ScrollVisibilityState].
 */
@Composable
fun rememberScrollVisibilityState(initialVisible: Boolean = true): ScrollVisibilityState {
    return remember { ScrollVisibilityState(initialVisible) }
}

/**
 * Remembers a [NestedScrollConnection] linked to a [ScrollVisibilityState].
 */
@Composable
fun rememberScrollConnection(
    state: ScrollVisibilityState
): NestedScrollConnection {
    return remember(state) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (consumed.y != 0f) {
                    state.onScroll(consumed.y)
                }
                return Offset.Zero
            }
        }
    }
}
