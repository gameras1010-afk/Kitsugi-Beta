package com.kitsugi.animelist.ui.screens.detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * FP-18 – Component representing the episodes list section of detail pages.
 */
@Composable
fun EpisodesSection(
    episodeCount: Int,
    onEpisodeClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Bölümler ($episodeCount)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (episodeCount == 0) {
            Text(
                text = "Bölüm bilgisi mevcut değil.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..minOf(episodeCount, 4)) {
                    Button(onClick = { onEpisodeClick(i) }) {
                        Text("Bölüm $i")
                    }
                }
            }
        }
    }
}
