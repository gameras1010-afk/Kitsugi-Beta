package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.delay

/**
 * FP-44 – "episode|airingAtEpoch" formatındaki string'i parse eden,
 * canlı geri sayım gösteren chip bileşeni.
 *
 * @param nextAiringEpisode "episode|airingAtEpoch" formatında string. null ise gösterilmez.
 * @param accentColor Chip rengi (varsayılan: AccentOrange)
 */
@Composable
fun NextAiringChip(
    nextAiringEpisode: String?,
    modifier: Modifier = Modifier,
    accentColor: Color = KitsugiColors.AccentOrange
) {
    if (nextAiringEpisode.isNullOrBlank()) return

    val parts = remember(nextAiringEpisode) { nextAiringEpisode.split("|") }
    val episode = remember(parts) { parts.getOrNull(0)?.toIntOrNull() } ?: return
    val targetEpoch = remember(parts) { parts.getOrNull(1)?.toLongOrNull() } ?: return

    var countdownText by remember(episode, targetEpoch) { mutableStateOf("") }

    LaunchedEffect(episode, targetEpoch) {
        while (true) {
            val now = System.currentTimeMillis() / 1000L
            val remaining = targetEpoch - now
            countdownText = when {
                remaining <= 0L -> "Bölüm $episode yayınlandı"
                remaining < 3600L -> {
                    val mins = (remaining / 60).toInt()
                    val secs = (remaining % 60).toInt()
                    "Bölüm $episode · %02d:%02d".format(mins, secs)
                }
                remaining < 86400L -> {
                    val hours = (remaining / 3600).toInt()
                    val mins = ((remaining % 3600) / 60).toInt()
                    "Bölüm $episode · %02d:%02d sonra yayında".format(hours, mins)
                }
                else -> {
                    val days = (remaining / 86400).toInt()
                    "Bölüm $episode · $days gün sonra yayında"
                }
            }
            if (remaining <= 0L) break
            val delayMs = if (remaining < 3600L) 1_000L
                          else if (remaining < 86400L) 10_000L
                          else 60_000L
            delay(delayMs)
        }
    }

    if (countdownText.isBlank()) return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = countdownText,
            color = accentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Geriye dönük uyumluluk için static overload.
 * Tercih edilen: [NextAiringChip(nextAiringEpisode: String?)]
 */
@Composable
fun NextAiringChip(
    episodeNumber: Int,
    airingInSeconds: Long,
    modifier: Modifier = Modifier,
    accentColor: Color = KitsugiColors.AccentOrange
) {
    val days = airingInSeconds / 86400
    val hours = (airingInSeconds % 86400) / 3600
    val timeText = when {
        days > 0 -> "${days}g ${hours}sa"
        hours > 0 -> "${hours} saat"
        else -> "Yakında"
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = "Bölüm $episodeNumber · $timeText sonra yayında",
            color = accentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
