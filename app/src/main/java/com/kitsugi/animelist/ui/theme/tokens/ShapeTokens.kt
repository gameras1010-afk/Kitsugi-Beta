package com.kitsugi.animelist.ui.theme.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * V2-B01: ShapeTokens
 *
 * Tüm bileşenlerde tutarlı köşe yarıçapları için merkezi token sistemi.
 * NuvioTV ShapeTokens.kt referans alındı.
 */
object ShapeTokens {

    // ── Rounded Corners ─────────────────────────────────────────────────────
    val ExtraSmall: Shape = RoundedCornerShape(4.dp)
    val Small: Shape = RoundedCornerShape(8.dp)
    val Medium: Shape = RoundedCornerShape(12.dp)
    val Large: Shape = RoundedCornerShape(16.dp)
    val ExtraLarge: Shape = RoundedCornerShape(20.dp)
    val Huge: Shape = RoundedCornerShape(24.dp)

    // ── Circle ──────────────────────────────────────────────────────────────
    val Circle: Shape = RoundedCornerShape(50)

    // ── Component-specific ──────────────────────────────────────────────────
    val Card: Shape = RoundedCornerShape(12.dp)
    val CardLarge: Shape = RoundedCornerShape(16.dp)
    val CardHero: Shape = RoundedCornerShape(0.dp)

    val Button: Shape = RoundedCornerShape(10.dp)
    val ButtonRound: Shape = RoundedCornerShape(50)

    val Dialog: Shape = RoundedCornerShape(24.dp)
    val BottomSheet: Shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

    val Badge: Shape = RoundedCornerShape(4.dp)
    val Chip: Shape = RoundedCornerShape(8.dp)

    val SearchBar: Shape = RoundedCornerShape(12.dp)
    val TextField: Shape = RoundedCornerShape(10.dp)

    // ── TV-specific ─────────────────────────────────────────────────────────
    val TvCard: Shape = RoundedCornerShape(8.dp)
    val TvCardFocused: Shape = RoundedCornerShape(10.dp)
    val TvFocusRing: Shape = RoundedCornerShape(12.dp)

    // ── Poster / Image ──────────────────────────────────────────────────────
    val PosterSmall: Shape = RoundedCornerShape(6.dp)
    val PosterMedium: Shape = RoundedCornerShape(8.dp)
    val PosterLarge: Shape = RoundedCornerShape(10.dp)
}
