package com.kitsugi.animelist.ui.screens.fullscreen.controls.components

// ─────────────────────────────────────────────────────────────────────────────
// BrightnessOverlay — Aniyomi-derived
// Original: eu.kanade.tachiyomi.ui.player.controls.components.BrightnessOverlay
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun BrightnessOverlay(
    brightness: Float,
    modifier: Modifier = Modifier,
) {
    if (brightness >= 0f || brightness == -2f) return
    val alpha = (-brightness).coerceIn(0f, 0.75f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha)),
    )
}
