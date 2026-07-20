package com.kitsugi.animelist.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object KitsugiColors {
    val Background: Color
        @Composable
        get() = LocalKitsugiColors.current.background

    val BackgroundElevated: Color
        @Composable
        get() = LocalKitsugiColors.current.backgroundElevated

    val Surface: Color
        @Composable
        get() = LocalKitsugiColors.current.surface

    val SurfaceSoft: Color
        @Composable
        get() = LocalKitsugiColors.current.surfaceSoft

    val SurfaceStrong: Color
        @Composable
        get() = LocalKitsugiColors.current.surfaceStrong

    val Accent: Color
        @Composable
        get() = LocalKitsugiAccent.current

    val AccentMuted: Color
        @Composable
        get() = LocalKitsugiAccent.current.copy(alpha = 0.15f)

    val AccentPink = Color(0xFFFF66CC)
    val AccentPurple = Color(0xFF9D7CFF)
    val AccentBlue = Color(0xFF60A5FA)
    val AccentGreen = Color(0xFF34D399)
    val AccentRed = Color(0xFFFB7185)
    val AccentOrange = Color(0xFFFB923C)
    val AccentYellow = Color(0xFFFBBF24)
    val AccentTeal = Color(0xFF2DD4BF)
    val AccentIndigo = Color(0xFF818CF8)

    val TextPrimary: Color
        @Composable
        get() = LocalKitsugiColors.current.textPrimary

    val TextSecondary: Color
        @Composable
        get() = LocalKitsugiColors.current.textSecondary

    val TextMuted: Color
        @Composable
        get() = LocalKitsugiColors.current.textMuted

    val Border: Color
        @Composable
        get() = LocalKitsugiColors.current.border

    val Scrim: Color
        @Composable
        get() = LocalKitsugiColors.current.scrim
}