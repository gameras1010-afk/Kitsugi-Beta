package com.kitsugi.animelist.ui.theme.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * V2-B01: ElevationTokens
 *
 * Tüm yüzeyler için merkezi gölge/yükseklik (tonal elevation) token sistemi.
 * NuvioTV ElevationTokens.kt referans alındı.
 */
object ElevationTokens {

    // ── Surface Levels ───────────────────────────────────────────────────────
    val Level0: Dp = 0.dp   // Flat surface, no shadow
    val Level1: Dp = 1.dp   // Navigation rail, card resting
    val Level2: Dp = 3.dp   // Floating action button, menu
    val Level3: Dp = 6.dp   // Navigation drawer, modal side sheet
    val Level4: Dp = 8.dp   // Bottom sheet, dialog
    val Level5: Dp = 12.dp  // Full-screen dialog

    // ── Card Elevations ──────────────────────────────────────────────────────
    val CardResting: Dp = 2.dp
    val CardHovered: Dp = 4.dp
    val CardFocused: Dp = 8.dp
    val CardPressed: Dp = 0.dp

    // ── TV Focus Glow ────────────────────────────────────────────────────────
    val TvCardResting: Dp = 0.dp
    val TvCardFocused: Dp = 16.dp

    // ── Modal / Overlay ──────────────────────────────────────────────────────
    val BottomSheet: Dp = 8.dp
    val Dialog: Dp = 24.dp
    val Tooltip: Dp = 6.dp
    val DropdownMenu: Dp = 8.dp

    // ── Navigation ───────────────────────────────────────────────────────────
    val NavigationBar: Dp = 3.dp
    val TopAppBar: Dp = 0.dp    // Flat by default in Material3
    val TopAppBarScrolled: Dp = 3.dp
}
