package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalBlurAdultMedia
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent

/**
 * Centralized NSFW-aware image composable.
 *
 * Reads [LocalBlurAdultMedia] from the composition. If the setting is enabled AND
 * [isAdult] is true the entire image box — including its shimmer/placeholder state —
 * is blurred with [blurRadius], ensuring no single frame of the raw image leaks through.
 *
 * Usage: replace every manual `AsyncImage + Modifier.blur(...)` pair in media cards
 * with this composable to get consistent, zero-per-component NSFW protection.
 *
 * @param model         Coil image model URL (nullable – shows placeholder initials when null/blank)
 * @param contentDescription Accessibility description for the image
 * @param isAdult       Whether this media item is tagged as adult/NSFW content
 * @param modifier      Modifier applied to the outer container Box
 * @param contentScale  How the image is scaled inside its bounds (default: Crop)
 * @param blurRadius    Amount of blur applied when NSFW blur is active (default: 24.dp)
 * @param initials      Fallback text shown when there is no image (default: empty — shows nothing)
 */
@Composable
fun KitsugiNsfwImage(
    model: String?,
    contentDescription: String,
    isAdult: Boolean,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    blurRadius: Dp = 24.dp,
    initials: String = "",
    initialsColor: Color? = null,
    initialsStyle: androidx.compose.ui.text.TextStyle? = null
) {
    val blurAdultMedia = LocalBlurAdultMedia.current
    val shouldBlur = blurAdultMedia && isAdult
    val accentColor = LocalKitsugiAccent.current
    val finalColor = initialsColor ?: accentColor
    val finalStyle = initialsStyle ?: MaterialTheme.typography.titleMedium

    // Apply the blur at the Box level so that BOTH the placeholder background/shimmer
    // AND the loaded image are blurred — nothing leaks through during loading.
    Box(
        modifier = modifier.then(
            if (shouldBlur) Modifier.blur(blurRadius) else Modifier
        ),
        contentAlignment = Alignment.Center
    ) {
        if (!model.isNullOrBlank()) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else if (initials.isNotEmpty()) {
            Text(
                text = initials.take(2).uppercase(),
                color = finalColor,
                style = finalStyle,
                fontWeight = FontWeight.Black
            )
        }
    }
}
