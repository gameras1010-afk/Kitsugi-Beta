package com.kitsugi.animelist.ui.tv.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalView
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames
import kotlinx.coroutines.delay

/**
 * Global focus recovery for Android TV.
 *
 * D-pad navigasyonunda focus boşluğa düştüğünde (örn: dialog kapanışı,
 * ekran geçişi, lazy list recyclama), otomatik olarak fallback focus requester'a
 * focus'u geri taşır.
 */
@Composable
fun TvGlobalFocusRecovery(
    fallbackFocusRequester: FocusRequester,
    checkIntervalMs: Long = 500L
) {
    val view = LocalView.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(checkIntervalMs)
            try {
                val current = view.findFocus()
                if (current == null) {
                    // Focus boşlukta — fallback'e geri dön
                    fallbackFocusRequester.requestFocusAfterFrames(frames = 1)
                }
            } catch (_: Exception) {
                // Sessizce devam et
            }
        }
    }
}
