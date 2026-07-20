package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.theme.KitsugiColors

/**
 * FP-44 – Display chip for scheduling when the next episode airs.
 */
@Composable
fun NextAiringChip(
    episodeNumber: Int,
    airingInSeconds: Long,
    modifier: Modifier = Modifier
) {
    val days = airingInSeconds / (24 * 3600)
    val hours = (airingInSeconds % (24 * 3600)) / 3600
    
    val timeText = when {
        days > 0 -> "${days} gün ${hours}sa"
        hours > 0 -> "${hours} saat"
        else -> "Yakında"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(KitsugiColors.AccentPink.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarToday,
            contentDescription = null,
            tint = KitsugiColors.AccentPink,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = "Bölüm $episodeNumber: $timeText sonra",
            color = KitsugiColors.AccentPink,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
