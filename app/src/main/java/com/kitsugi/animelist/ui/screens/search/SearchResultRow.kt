package com.kitsugi.animelist.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayScore

import com.kitsugi.animelist.ui.utils.tvClickable

/**
 * Arama sonucu satır bileşeni.
 * MoeList'in arama sonucu liste öğesinden ve AniHyou SearchView.kt'nin
 * sonuç gösterim yaklaşımından ilham alınarak Kitsugi'ya uyarlanmıştır.
 */
@Composable
fun SearchResultRow(
    result: JikanSearchResult,
    alreadyInList: Boolean,
    onItemClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleLanguage: String = "ROMAJI",
    scoreFormat: String = "POINT_10",
    hideScores: Boolean = false
) {
    val accentColor = LocalKitsugiAccent.current
    val displayTitle = result.getDisplayTitle(titleLanguage)
    val isTv = LocalIsTv.current

    val rowShape = if (isTv) KitsugiTvTokens.Shapes.posterCard else RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(KitsugiColors.Surface)
            .tvClickable(shape = rowShape, onClick = onItemClick)
            .padding(if (isTv) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kapak görseli
        val thumbWidth = if (isTv) KitsugiTvTokens.Cards.searchThumbWidth else 56.dp
        val thumbHeight = if (isTv) KitsugiTvTokens.Cards.searchThumbHeight else 80.dp
        Box(
            modifier = Modifier
                .size(width = thumbWidth, height = thumbHeight)
                .clip(if (isTv) KitsugiTvTokens.Shapes.posterCard else RoundedCornerShape(10.dp))
                .background(KitsugiColors.SurfaceStrong)
        ) {
            if (result.imageUrl != null) {
                AsyncImage(
                    model = result.imageUrl,
                    contentDescription = displayTitle,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Bilgiler
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = displayTitle,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (result.subtitle.isNotBlank()) {
                Text(
                    text = result.subtitle,
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!hideScores) {
                    val displayScore = result.getDisplayScore(scoreFormat, hideScores)
                    val scoreText = if (displayScore == "unrated") "unrated" else "★ $displayScore"
                    Text(
                        text = scoreText,
                        color = KitsugiColors.AccentOrange,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (result.year != null) {
                    Text(
                        text = "${result.year}",
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Text(
                    text = result.type.name,
                    color = accentColor.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                val sourceLabel = when (result.source.lowercase()) {
                    "anilist" -> "AniList"
                    "mal", "jikan" -> "MAL"
                    else -> result.source.uppercase()
                }
                val sourceColor = when (result.source.lowercase()) {
                    "anilist" -> KitsugiColors.AccentBlue
                    else -> accentColor
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(KitsugiColors.SurfaceStrong)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = sourceLabel,
                        color = sourceColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Ekle / Eklendi butonu (TV'de odaklanabilir Box + tvClickable)
        val buttonShape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(buttonShape)
                .background(
                    if (alreadyInList) KitsugiColors.AccentGreen.copy(alpha = 0.20f)
                    else accentColor.copy(alpha = 0.15f)
                )
                .tvClickable(
                    shape = buttonShape,
                    enabled = !alreadyInList,
                    onClick = onAddClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (alreadyInList) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = if (alreadyInList) "Listede" else "Listeye ekle",
                tint = if (alreadyInList) KitsugiColors.AccentGreen else accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
