package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.copyOnDoubleTap

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KitsugiDetailHero(
    title: String,
    subtitle: String,
    imageUrl: String?,
    logoUrl: String?,
    source: String,
    typeLabel: String, // "ANIME" or "MANGA"
    year: String?,
    isAdult: Boolean,
    onBackClick: () -> Unit,
    onPosterClick: (() -> Unit)? = null,
    statusLabel: String? = null,
    statusColor: Color? = null,
    scoreLabel: String? = null,
    isFavorite: Boolean = false,
    alreadyInList: Boolean = false,
) {
    val accentColor = LocalKitsugiAccent.current
    val fallbackPlaceholderColor = statusColor ?: accentColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(470.dp)
    ) {
        if (!imageUrl.isNullOrBlank()) {
            val baseModifier = Modifier.fillMaxSize()
            val imageModifier = if (onPosterClick != null) {
                baseModifier.tvClickable { onPosterClick() }
            } else {
                baseModifier
            }
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = imageModifier,
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fallbackPlaceholderColor.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.take(2).uppercase(),
                    color = fallbackPlaceholderColor,
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

        TextButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 24.dp)
        ) {
            Text(
                text = "Geri",
                color = KitsugiColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            val context = LocalContext.current
            val copyTitleGesture = Modifier.copyOnDoubleTap(context, title)

            if (!logoUrl.isNullOrBlank()) {
                var logoFailed by remember(logoUrl) { mutableStateOf(false) }
                if (!logoFailed) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = title,
                        modifier = Modifier
                            .height(75.dp)
                            .fillMaxWidth(0.85f)
                            .then(copyTitleGesture),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        onError = { logoFailed = true }
                    )
                } else {
                    Text(
                        text = title,
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
                    text = title,
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
                HeroDetailPill(
                    text = typeLabel,
                    color = accentColor
                )

                if (statusLabel != null) {
                    HeroDetailPill(
                        text = statusLabel,
                        color = statusColor ?: accentColor
                    )
                }

                HeroDetailPill(
                    text = source.uppercase(),
                    color = accentColor
                )

                if (year != null) {
                    HeroDetailPill(
                        text = year,
                        color = KitsugiColors.TextSecondary
                    )
                }

                if (scoreLabel != null) {
                    HeroDetailPill(
                        text = scoreLabel,
                        color = accentColor
                    )
                }

                if (isFavorite) {
                    HeroDetailPill(
                        text = "★ Favori",
                        color = accentColor
                    )
                }

                if (alreadyInList) {
                    HeroDetailPill(
                        text = "Listede",
                        color = KitsugiColors.AccentGreen
                    )
                }

                if (isAdult) {
                    HeroDetailPill(
                        text = "+18",
                        color = KitsugiColors.AccentRed
                    )
                }
            }

            val isRedundantSubtitle = subtitle.isBlank() ||
                subtitle.contains(" • ") ||
                subtitle.equals("Manuel eklenen içerik", ignoreCase = true)

            if (!isRedundantSubtitle) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = subtitle,
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HeroDetailPill(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
