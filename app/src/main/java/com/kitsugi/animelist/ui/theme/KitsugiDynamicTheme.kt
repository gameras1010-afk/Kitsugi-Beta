package com.kitsugi.animelist.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalKitsugiAccent = compositionLocalOf {
    Color(0xFFC8F4EF)
}

fun KitsugiAccentForThemeId(themeId: String): Color {
    return when (themeId) {
        "mint" -> Color(0xFFC8F4EF)
        "pink" -> KitsugiColors.AccentPink
        "purple" -> KitsugiColors.AccentPurple
        "blue" -> KitsugiColors.AccentBlue
        "green" -> KitsugiColors.AccentGreen
        "red" -> KitsugiColors.AccentRed
        "orange" -> KitsugiColors.AccentOrange
        "yellow" -> KitsugiColors.AccentYellow
        "teal" -> KitsugiColors.AccentTeal
        "indigo" -> KitsugiColors.AccentIndigo
        else -> Color(0xFFC8F4EF)
    }
}