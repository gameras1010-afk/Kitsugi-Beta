package com.kitsugi.animelist.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class KitsugiColorScheme(
    val background: Color,
    val backgroundElevated: Color,
    val surface: Color,
    val surfaceSoft: Color,
    val surfaceStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val scrim: Color
)

val KitsugiDarkColors = KitsugiColorScheme(
    background = Color(0xFF080A10),
    backgroundElevated = Color(0xFF0B0D14),
    surface = Color(0xFF10131D),
    surfaceSoft = Color(0xFF171B28),
    surfaceStrong = Color(0xFF202636),
    textPrimary = Color(0xFFF4F7FB),
    textSecondary = Color(0xFFA9B0C2),
    textMuted = Color(0xFF727A8F),
    border = Color(0xFF252B3A),
    scrim = Color(0x99000000)
)

val KitsugiLightColors = KitsugiColorScheme(
    background = Color(0xFFF3F4F6),
    backgroundElevated = Color(0xFFE5E7EB),
    surface = Color(0xFFFFFFFF),
    surfaceSoft = Color(0xFFF9FAFB),
    surfaceStrong = Color(0xFFF3F4F6),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF4B5563),
    textMuted = Color(0xFF9CA3AF),
    border = Color(0xFFE5E7EB),
    scrim = Color(0x66000000)
)

val KitsugiAmoledColors = KitsugiColorScheme(
    background = Color(0xFF000000),
    backgroundElevated = Color(0xFF000000),
    surface = Color(0xFF080808),
    surfaceSoft = Color(0xFF0C0C0C),
    surfaceStrong = Color(0xFF141414),
    textPrimary = Color(0xFFF4F7FB),
    textSecondary = Color(0xFFA9B0C2),
    textMuted = Color(0xFF727A8F),
    border = Color(0xFF1F1F1F),
    scrim = Color(0x99000000)
)

val LocalKitsugiColors = staticCompositionLocalOf {
    KitsugiDarkColors
}
