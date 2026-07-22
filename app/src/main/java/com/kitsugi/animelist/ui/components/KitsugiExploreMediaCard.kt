package com.kitsugi.animelist.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens

import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayScore

@Composable
fun KitsugiExploreMediaCard(
    result: JikanSearchResult,
    alreadyInList: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleLanguage: String = "ROMAJI",
    scoreFormat: String = "POINT_10",
    hideScores: Boolean = false,
    blurAdultMedia: Boolean = false,
    forceVertical: Boolean = false
) {
    val accentColor = LocalKitsugiAccent.current
    val displayTitle = result.getDisplayTitle(titleLanguage)
    val isTv = LocalIsTv.current
    // TV'de her zaman dikey poster, isLandscape yoksayılır
    // Mobilde orijinal landscape/portrait mantığı korunur
    val isLandscape = !isTv && !forceVertical &&
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // TV: KitsugiTvTokens referanslı kompakt shape
    val cardShape = if (isTv) KitsugiTvTokens.Shapes.posterCard else RoundedCornerShape(24.dp)
    val focusScale = if (isTv) KitsugiTvTokens.Cards.focusedScale else 1.08f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .tvClickable(
                shape = cardShape,
                scaleFocused = focusScale,
                onClick = onClick
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = KitsugiColors.Surface
        )
    ) {
        // ── TV: Her zaman kompakt dikey poster (2:3) ─────────────────────────
        if (isTv) {
            Column {
                // Poster görseli — tam genişlik, sabit yükseklik
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KitsugiTvTokens.Cards.posterHeight)
                        .clip(KitsugiTvTokens.Shapes.posterCard)
                        .background(KitsugiColors.SurfaceSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (!result.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = result.imageUrl,
                            contentDescription = displayTitle,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (blurAdultMedia && result.isAdult) Modifier.blur(24.dp)
                                    else Modifier
                                ),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = displayTitle.take(2).uppercase(),
                            color = accentColor,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (alreadyInList) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(KitsugiColors.AccentGreen)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "✓",
                                color = KitsugiColors.Background,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Başlık + meta bilgi
                Column(
                    modifier = Modifier.padding(
                        horizontal = KitsugiTvTokens.Spacing.sm,
                        vertical = KitsugiTvTokens.Spacing.sm
                    )
                ) {
                    Text(
                        text = displayTitle,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildMetaText(result, scoreFormat, hideScores),
                        color = if (alreadyInList) KitsugiColors.AccentGreen else accentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 9.sp
                    )
                }
            }
        // ── MOBİL LANDSCAPE ──────────────────────────────────────────────────
        } else if (isLandscape) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 160.dp, height = 100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(KitsugiColors.SurfaceSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (!result.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = result.imageUrl,
                            contentDescription = displayTitle,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (blurAdultMedia && result.isAdult) Modifier.blur(24.dp)
                                    else Modifier
                                ),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = displayTitle.take(2).uppercase(),
                            color = accentColor,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (alreadyInList) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(KitsugiColors.AccentGreen)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Listede",
                                color = KitsugiColors.Background,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = displayTitle,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = result.subtitle,
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = buildMetaText(result, scoreFormat, hideScores),
                        color = if (alreadyInList) {
                            KitsugiColors.AccentGreen
                        } else {
                            accentColor
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Yayın geri sayımı — sadece AniList kaynaklı, nextAiringEpisode dolu ise
                    if (!result.nextAiringEpisode.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(5.dp))
                        NextAiringChip(
                            nextAiringEpisode = result.nextAiringEpisode
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(KitsugiColors.SurfaceSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (!result.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = result.imageUrl,
                            contentDescription = displayTitle,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (blurAdultMedia && result.isAdult) Modifier.blur(24.dp)
                                    else Modifier
                                ),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = displayTitle.take(2).uppercase(),
                            color = accentColor,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (alreadyInList) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(KitsugiColors.AccentGreen)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Listede",
                                color = KitsugiColors.Background,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = displayTitle,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = result.subtitle,
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = buildMetaText(result, scoreFormat, hideScores),
                    color = if (alreadyInList) {
                        KitsugiColors.AccentGreen
                    } else {
                        accentColor
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                // Yayın geri sayımı — sadece AniList kaynaklı, nextAiringEpisode dolu ise
                if (!result.nextAiringEpisode.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    NextAiringChip(
                        nextAiringEpisode = result.nextAiringEpisode
                    )
                }
            }
        }
    }
}

private fun buildMetaText(
    result: JikanSearchResult,
    scoreFormat: String,
    hideScores: Boolean
): String {
    val parts = buildList {
        val typeLabel = when (result.type) {
            MediaType.Anime -> "ANIME"
            MediaType.Manga -> "MANGA"
            MediaType.Movie -> "FİLM"
            MediaType.TvShow -> "DİZİ"
        }
        add(typeLabel)
        if (result.year != null) add(result.year.toString())
        if (!hideScores) {
            val scoreStr = result.getDisplayScore(scoreFormat, hideScores)
            if (scoreStr.isNotEmpty() && scoreStr != "N/A" && scoreStr != "0") {
                add(scoreStr)
            }
        }

        val sourceLabel = when (result.source.lowercase()) {
            "anilist" -> "AniList"
            "mal", "jikan" -> "MAL"
            "simkl" -> "Simkl"
            else -> result.source.uppercase()
        }
        add("$sourceLabel #${result.malId}")
        if (result.isAdult) add("+18")
    }

    return parts.joinToString(" • ")
}