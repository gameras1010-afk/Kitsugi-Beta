package com.kitsugi.animelist.ui.tv.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import com.kitsugi.animelist.ui.theme.KitsugiColors

val LocalTvComponentTokens = staticCompositionLocalOf { KitsugiComponents.tokens }
val LocalTvLayoutTokens = staticCompositionLocalOf { KitsugiLayout.tokens }
val LocalTvMediaTokens = staticCompositionLocalOf { KitsugiMedia.tokens }
val LocalTvMotionTokens = staticCompositionLocalOf { KitsugiMotion.tokens }
val LocalTvFocusTokens = staticCompositionLocalOf { KitsugiFocus.tokens }
val LocalTvShapeTokens = staticCompositionLocalOf { KitsugiShapes.tokens }
val LocalTvSizeTokens = staticCompositionLocalOf { KitsugiSizes.tokens }

@OptIn(ExperimentalTvMaterial3Api::class)
object TvTheme {
    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val components: KitsugiComponentTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTvComponentTokens.current

    val layout: KitsugiLayoutTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTvLayoutTokens.current

    val media: KitsugiMediaTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTvMediaTokens.current

    val motion: KitsugiMotionTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTvMotionTokens.current

    val focus: KitsugiFocusTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTvFocusTokens.current

    val shapes: KitsugiShapeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTvShapeTokens.current

    val sizes: KitsugiSizeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTvSizeTokens.current
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun KitsugiTvTheme(
    content: @Composable () -> Unit
) {
    val TvDarkColorScheme = darkColorScheme(
        primary = KitsugiColors.Accent,
        onPrimary = KitsugiColors.Background,
        primaryContainer = KitsugiColors.SurfaceSoft,
        onPrimaryContainer = KitsugiColors.TextPrimary,
        secondary = KitsugiColors.AccentPurple,
        onSecondary = KitsugiColors.Background,
        secondaryContainer = KitsugiColors.Surface,
        onSecondaryContainer = KitsugiColors.TextSecondary,
        tertiary = KitsugiColors.AccentPink,
        onTertiary = KitsugiColors.Background,
        background = KitsugiColors.Background,
        onBackground = KitsugiColors.TextPrimary,
        surface = KitsugiColors.Surface,
        onSurface = KitsugiColors.TextPrimary,
        surfaceVariant = KitsugiColors.SurfaceSoft,
        onSurfaceVariant = KitsugiColors.TextSecondary,
        border = KitsugiColors.Border,
        error = KitsugiPrimitives.error
    )

    CompositionLocalProvider(
        LocalTvComponentTokens provides KitsugiComponents.tokens,
        LocalTvLayoutTokens provides KitsugiLayout.tokens,
        LocalTvMediaTokens provides KitsugiMedia.tokens,
        LocalTvMotionTokens provides KitsugiMotion.tokens,
        LocalTvFocusTokens provides KitsugiFocus.tokens,
        LocalTvShapeTokens provides KitsugiShapes.tokens,
        LocalTvSizeTokens provides KitsugiSizes.tokens
    ) {
        MaterialTheme(
            colorScheme = TvDarkColorScheme,
            typography = Typography(),
            content = content
        )
    }
}
