package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle

@Composable
fun DetailedSeasonalMediaCard(
    result: JikanSearchResult,
    alreadyInList: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleLanguage: String = "ROMAJI"
) {
    val accentColor = LocalKitsugiAccent.current
    val displayTitle = result.getDisplayTitle(titleLanguage)

    // Airing countdown logic
    val nextAiringEpisode = result.nextAiringEpisode
    var airingText by remember(nextAiringEpisode) { mutableStateOf("") }

    if (!nextAiringEpisode.isNullOrBlank()) {
        val parts = remember(nextAiringEpisode) { nextAiringEpisode.split("|") }
        val episode = remember(parts) { parts.getOrNull(0)?.toIntOrNull() }
        val targetEpoch = remember(parts) { parts.getOrNull(1)?.toLongOrNull() }

        if (episode != null && targetEpoch != null) {
            LaunchedEffect(episode, targetEpoch) {
                while (true) {
                    val now = System.currentTimeMillis() / 1000L
                    val remaining = targetEpoch - now
                    val timeText = when {
                        remaining <= 0L -> "yayınlandı"
                        remaining < 3600L -> {
                            val mins = (remaining / 60).toInt()
                            "${mins} dk sonra"
                        }
                        remaining < 86400L -> {
                            val hours = (remaining / 3600).toInt()
                            val mins = ((remaining % 3600) / 60).toInt()
                            "%02d:%02d sonra".format(hours, mins)
                        }
                        else -> {
                            val days = (remaining / 86400).toInt()
                            "$days gün sonra"
                        }
                    }
                    airingText = "Bölüm $episode, $timeText yayında"
                    if (remaining <= 0L) break
                    val delayMs = if (remaining < 3600L) 1_000L
                                  else if (remaining < 86400L) 10_000L
                                  else 60_000L
                    kotlinx.coroutines.delay(delayMs)
                }
            }
        }
    }

    val fallbackAiringText = remember(result) {
        val typeName = when (result.type) {
            com.kitsugi.animelist.model.MediaType.Anime -> "Anime"
            com.kitsugi.animelist.model.MediaType.Manga -> "Manga"
            com.kitsugi.animelist.model.MediaType.Movie -> "Film"
            com.kitsugi.animelist.model.MediaType.TvShow -> "Dizi"
        }
        if (result.total != null && result.total > 0) {
            "$typeName (${result.total} bölüm)"
        } else {
            result.subtitle.split(", ").firstOrNull() ?: typeName
        }
    }

    // Score format like 86%
    val displayScore = remember(result.rawScoreDouble, result.score) {
        val scoreVal = result.rawScoreDouble ?: result.score?.toDouble()
        if (scoreVal != null && scoreVal > 0) {
            if (scoreVal <= 10.0) {
                val percentage = (scoreVal * 10).toInt()
                "$percentage%"
            } else {
                "${scoreVal.toInt()}%"
            }
        } else {
            null
        }
    }

    // Extract genres from subtitle
    val genresText = remember(result.subtitle) {
        val parts = result.subtitle.split(", ")
        val filtered = parts.filter { part ->
            part != "TV" && part != "Manga" && part != "Anime" && part != "Film" && part != "Dizi" &&
            part != "Special" && part != "OVA" && part != "ONA" && part != "Movie" &&
            part.toIntOrNull() == null
        }
        if (filtered.isNotEmpty()) filtered.joinToString(", ") else result.subtitle
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (alreadyInList) {
                    Modifier.border(1.dp, KitsugiColors.AccentGreen.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                } else Modifier
            )
            .tvClickable(
                shape = RoundedCornerShape(20.dp),
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = KitsugiColors.Surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(KitsugiColors.SurfaceSoft)
            ) {
                KitsugiNsfwImage(
                        model = result.imageUrl,
                        contentDescription = displayTitle,
                        isAdult = result.isAdult,
                        modifier = Modifier.fillMaxSize(),
                        initials = displayTitle
                    )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Metadata Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text = displayTitle,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Airing Countdown or Fallback Text
                Text(
                    text = if (airingText.isNotBlank()) airingText else fallbackAiringText,
                    color = if (airingText.isNotBlank()) KitsugiColors.AccentOrange else KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Score Percentage
                if (displayScore != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Puan",
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = displayScore,
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Genres Text
                if (genresText.isNotBlank()) {
                    Text(
                        text = genresText,
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
