package com.kitsugi.animelist.ui.screens.detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * FP-16 – Collection grouping section for detail screen.
 */
@Composable
fun CollectionSection(
    collections: List<String>,
    onCollectionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (collections.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Koleksiyonlar",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            collections.forEach { collectionName ->
                InputChip(
                    selected = false,
                    onClick = { onCollectionClick(collectionName) },
                    label = { Text(collectionName) }
                )
            }
        }
    }
}
