package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle
import com.kitsugi.animelist.utils.copyOnDoubleTap
import com.kitsugi.animelist.utils.ShareUtils
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailHero(
    entry: MediaEntry,
    logoUrl: String?,
    onBackClick: () -> Unit,
    titleLanguage: String,
    blurAdultMedia: Boolean = false,
    onPosterClick: (String) -> Unit = {},
    onShareClick: (() -> Unit)? = null
) {
    val accentColor = LocalKitsugiAccent.current
    val statusColor = statusColor(entry.status)
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(470.dp)
    ) {
        if (!entry.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = entry.imageUrl,
                contentDescription = entry.getDisplayTitle(titleLanguage),
                modifier = Modifier
                    .fillMaxSize()
                    .tvClickable { onPosterClick(entry.imageUrl) }
                    .then(
                        if (blurAdultMedia && entry.isAdult) Modifier.blur(24.dp)
                        else Modifier
                    ),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(statusColor.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.getDisplayTitle(titleLanguage).take(2).uppercase(),
                    color = statusColor,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            KitsugiColors.Background.copy(alpha = 0.05f),
                            KitsugiColors.Background.copy(alpha = 0.30f),
                            KitsugiColors.Background.copy(alpha = 0.72f),
                            KitsugiColors.Background
                        )
                    )
                )
        )

        // ── Top action bar: Back (left) + Share (right) ───────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Geri butonu
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(KitsugiColors.Background.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Geri",
                        tint = KitsugiColors.TextPrimary
                    )
                }
            }

            // Paylaş butonu
            val shareAction: () -> Unit = onShareClick ?: {
                val mediaId = entry.malId ?: entry.id
                val url = ShareUtils.buildMediaUrl(entry.source, mediaId, entry.type)
                ShareUtils.shareText(context, entry.title, url)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(KitsugiColors.Background.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = shareAction) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Paylaş",
                        tint = KitsugiColors.TextPrimary
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            val context = LocalContext.current
            val titleText = entry.getDisplayTitle(titleLanguage)
            val copyTitleGesture = Modifier.copyOnDoubleTap(context, titleText)

            if (!logoUrl.isNullOrBlank()) {
                var logoFailed by remember(logoUrl) { mutableStateOf(false) }
                if (!logoFailed) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = entry.getDisplayTitle(titleLanguage),
                        modifier = Modifier
                            .height(75.dp)
                            .fillMaxWidth(0.85f)
                            .then(copyTitleGesture),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        onError = {
                            logoFailed = true
                        }
                    )
                } else {
                    Text(
                        text = entry.getDisplayTitle(titleLanguage),
                        modifier = copyTitleGesture,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = entry.getDisplayTitle(titleLanguage),
                    modifier = copyTitleGesture,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailPill(
                    text = mediaTypeLabel(entry),
                    color = accentColor
                )

                DetailPill(
                    text = entry.status.label,
                    color = statusColor
                )

                DetailPill(
                    text = entry.source.uppercase(),
                    color = accentColor
                )

                if (entry.year != null) {
                    DetailPill(
                        text = entry.year.toString(),
                        color = KitsugiColors.TextSecondary
                    )
                }

                if (entry.isFavorite) {
                    DetailPill(
                        text = "★ Favori",
                        color = accentColor
                    )
                }

                if (entry.isAdult) {
                    DetailPill(
                        text = "+18",
                        color = KitsugiColors.AccentRed
                    )
                }
            }

            val isRedundantSubtitle = entry.subtitle.isBlank() ||
                entry.subtitle.contains(" • ") ||
                entry.subtitle.equals("Manuel eklenen içerik", ignoreCase = true)

            if (!isRedundantSubtitle) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = entry.subtitle,
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}



private fun statusColor(status: WatchStatus): Color {
    return when (status) {
        WatchStatus.Watching   -> KitsugiColors.AccentBlue
        WatchStatus.Completed  -> KitsugiColors.AccentGreen
        WatchStatus.Planned    -> KitsugiColors.AccentOrange
        WatchStatus.Dropped    -> KitsugiColors.AccentRed
        WatchStatus.Paused     -> KitsugiColors.AccentPurple
        WatchStatus.Repeating  -> KitsugiColors.AccentBlue
    }
}

private fun mediaTypeLabel(entry: MediaEntry): String {
    return when (entry.type) {
        MediaType.Anime -> "ANIME"
        MediaType.Manga -> "MANGA"
        MediaType.Movie -> "FİLM"
        MediaType.TvShow -> "DİZİ"
    }
}
