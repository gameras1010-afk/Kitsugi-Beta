package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle
import java.text.NumberFormat
import java.util.Locale

@Composable
fun KitsugiRankingMediaCard(
    result: JikanSearchResult,
    rankIndex: Int,
    alreadyInList: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleLanguage: String = "ROMAJI",
    hideScores: Boolean = false,
    blurAdultMedia: Boolean = false
) {
    val accentColor = LocalKitsugiAccent.current
    val displayTitle = result.getDisplayTitle(titleLanguage)

    Card(
        modifier = modifier
            .fillMaxWidth()
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
            // Left Poster Box with Rank Badge
            Box(
                modifier = Modifier
                    .size(width = 86.dp, height = 120.dp)
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

                // Rank Badge (#1, #2, #3, etc.)
                val rankNumber = result.rank ?: rankIndex
                val isTopRank = rankNumber == 1
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isTopRank) Color(0xFFFFD700).copy(alpha = 0.9f)
                            else Color.Black.copy(alpha = 0.65f)
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "#$rankNumber",
                        color = if (isTopRank) Color.Black else Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }

                // Already in list checkmark
                if (alreadyInList) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(KitsugiColors.AccentGreen)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Listede",
                            tint = KitsugiColors.Background,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info Column
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

                // Subtitle (Format + Total Episodes/Chapters)
                val formatText = buildFormatText(result)
                if (formatText.isNotEmpty()) {
                    Text(
                        text = formatText,
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Score Line
                if (!hideScores) {
                    val scoreVal = result.rawScoreDouble
                        ?: result.score?.let { it.toDouble() }
                    if (scoreVal != null && scoreVal > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Puan",
                                tint = Color(0xFFFFB800),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "%.2f", scoreVal),
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Members / Popularity Line
                val membersVal = result.members ?: result.favorites
                if (membersVal != null && membersVal > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Group,
                            contentDescription = "Üyeler",
                            tint = KitsugiColors.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = formatNumber(membersVal),
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Yayın geri sayımı — sadece AniList kaynaklı, nextAiringEpisode dolu ise
                if (!result.nextAiringEpisode.isNullOrBlank()) {
                    NextAiringChip(nextAiringEpisode = result.nextAiringEpisode)
                }
            }
        }
    }
}

private fun buildFormatText(result: JikanSearchResult): String {
    val typeName = when (result.type) {
        MediaType.Anime -> "Anime"
        MediaType.Manga -> "Manga"
        MediaType.Movie -> "Film"
        MediaType.TvShow -> "Dizi"
    }

    return if (result.total != null && result.total > 0) {
        val countLabel = if (result.type == MediaType.Manga) "bölüm" else "bölüm"
        "$typeName (${result.total} $countLabel)"
    } else {
        result.subtitle.ifBlank { typeName }
    }
}

private val trLocale = Locale("tr", "TR")
private val trNumberFormat = NumberFormat.getInstance(trLocale)

private fun formatNumber(number: Int): String {
    return trNumberFormat.format(number)
}
