package com.kitsugi.animelist.ui.tv.focus

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.withFrameNanos

/**
 * Helper utilities for requesting and restoring focus in TV screens safely.
 */
object TvFocusRestoration {

    /**
     * Safely requests focus by waiting for recomposition frames, handling rendering delays.
     * Retries up to [maxAttempts] times in case focus cannot be requested immediately.
     */
    suspend fun FocusRequester.safeRequestFocus(maxAttempts: Int = 5) {
        repeat(2) {
            withFrameNanos { }
        }
        repeat(maxAttempts) { attempt ->
            val requested = runCatching {
                requestFocus()
                true
            }.getOrDefault(false)
            if (requested) return
            if (attempt < maxAttempts - 1) {
                withFrameNanos { }
            }
        }
    }
}
