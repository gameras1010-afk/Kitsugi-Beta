package com.kitsugi.animelist.ui.screens.fullscreen.controls

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

// ─────────────────────────────────────────────────────────────────────────────
// LocalPlayerButtonsClickEvent — resets control auto-hide timer on each click
// ─────────────────────────────────────────────────────────────────────────────

@Suppress("CompositionLocalAllowlist")
val LocalPlayerButtonsClickEvent = staticCompositionLocalOf<() -> Unit> { {} }
