package com.kitsugi.animelist.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * FP-45 – Interactive row for editing entry episode/chapter progress.
 */
@Composable
fun EditMediaProgressRow(
    currentProgress: Int,
    maxProgress: Int?,
    onProgressChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "İlerleme",
            style = MaterialTheme.typography.bodyLarge
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { if (currentProgress > 0) onProgressChanged(currentProgress - 1) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("-")
            }
            Text(
                text = "$currentProgress / ${maxProgress ?: "?"}",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = { if (maxProgress == null || currentProgress < maxProgress) onProgressChanged(currentProgress + 1) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("+")
            }
        }
    }
}
