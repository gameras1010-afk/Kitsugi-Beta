package com.kitsugi.animelist.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp

/**
 * Global CompositionLocal to detect if the app is running on an Android TV device.
 * Any composable can read this via: val isTv = LocalIsTv.current
 */
val LocalIsTv = staticCompositionLocalOf { false }
val LocalIsTvDevice = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
fun KitsugiAnimeListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledBlack: Boolean = false,
    selectedThemeId: String = "mint",
    customAccentColor: Int = 0,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    isTv: Boolean = false,
    content: @Composable () -> Unit
) {
    val KitsugiColors = when {
        isTv -> KitsugiDarkColors // TV holds dark theme stable
        !darkTheme -> KitsugiLightColors
        amoledBlack -> KitsugiAmoledColors
        else -> KitsugiDarkColors
    }

    val accentColor = if (customAccentColor != 0) {
        androidx.compose.ui.graphics.Color(customAccentColor)
    } else {
        KitsugiAccentForThemeId(selectedThemeId)
    }

    // Provide isTv as false for layout-adaptive components, but keep isTvDevice for D-pad enhancements
    CompositionLocalProvider(
        LocalIsTv provides false,
        LocalIsTvDevice provides isTv,
        LocalKitsugiColors provides KitsugiColors,
        LocalKitsugiAccent provides accentColor
    ) {
        val colorScheme = when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

private fun scaleTextStyle(style: androidx.compose.ui.text.TextStyle, factor: Float): androidx.compose.ui.text.TextStyle {
    val newSize = if (style.fontSize.type == TextUnitType.Sp) (style.fontSize.value * factor).sp else style.fontSize
    val newHeight = if (style.lineHeight.type == TextUnitType.Sp) (style.lineHeight.value * factor).sp else style.lineHeight
    return style.copy(fontSize = newSize, lineHeight = newHeight)
}