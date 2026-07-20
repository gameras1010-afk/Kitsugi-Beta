package com.kitsugi.animelist.ui.screens.detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * FP-19 – Metadata info panel for anime/manga detail screens.
 */
@Composable
fun MetaDetails(
    format: String,
    status: String,
    season: String?,
    year: Int?,
    studio: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Detaylar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "Format: $format", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Durum: $status", style = MaterialTheme.typography.bodyMedium)
                if (season != null && year != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Sezon: $season $year", style = MaterialTheme.typography.bodyMedium)
                }
                if (studio != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Stüdyo: $studio", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
