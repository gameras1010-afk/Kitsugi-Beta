package com.kitsugi.animelist.ui.theme.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * V2-B01: SpacingTokens
 *
 * Tüm ekranlarda tutarlı boşluk değerleri için merkezi token sistemi.
 * NuvioTV SpacingTokens.kt referans alındı.
 */
object SpacingTokens {

    // ── Micro ───────────────────────────────────────────────────────────────
    val Micro: Dp = 2.dp
    val ExtraSmall: Dp = 4.dp
    val Small: Dp = 8.dp

    // ── Standard ────────────────────────────────────────────────────────────
    val Medium: Dp = 12.dp
    val Default: Dp = 16.dp
    val Large: Dp = 20.dp
    val ExtraLarge: Dp = 24.dp

    // ── Section ─────────────────────────────────────────────────────────────
    val Section: Dp = 32.dp
    val SectionLarge: Dp = 40.dp
    val Page: Dp = 48.dp

    // ── Card / Content ──────────────────────────────────────────────────────
    val CardPadding: Dp = 12.dp
    val CardPaddingLarge: Dp = 16.dp
    val CardGap: Dp = 8.dp
    val CardGapLarge: Dp = 12.dp

    // ── Grid ────────────────────────────────────────────────────────────────
    val GridHorizontalPadding: Dp = 16.dp
    val GridVerticalPadding: Dp = 12.dp
    val GridItemGap: Dp = 10.dp

    // ── TV-specific ─────────────────────────────────────────────────────────
    val TvSectionPadding: Dp = 48.dp
    val TvCardGap: Dp = 12.dp
    val TvFocusRingPadding: Dp = 4.dp

    // ── Bottom bar / Nav ────────────────────────────────────────────────────
    val BottomBarHeight: Dp = 60.dp
    val TopBarHeight: Dp = 56.dp
}
