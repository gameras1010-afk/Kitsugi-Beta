package com.kitsugi.animelist.ui.screens.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * V2-D04 – FolderDetailScreen
 *
 * Shows the content of a single collection folder with a poster grid,
 * reorder mode and item removal controls.
 */
data class CollectionItem(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val mediaType: String,
    val year: Int?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folder: CollectionFolder,
    items: List<CollectionItem>,
    onNavigateBack: () -> Unit,
    onRemoveItem: (CollectionItem) -> Unit,
    onEditFolder: () -> Unit,
    onItemClick: (CollectionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var reorderMode by remember { mutableStateOf(false) }
    var itemToRemove by remember { mutableStateOf<CollectionItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(folder.emoji, style = MaterialTheme.typography.titleLarge)
                        Column {
                            Text(folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${items.size} öğe", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (reorderMode) {
                        TextButton(onClick = { reorderMode = false }) { Text("Bitti") }
                    } else {
                        IconButton(onClick = { reorderMode = true }) {
                            Icon(Icons.Rounded.DragHandle, contentDescription = "Sırayı Düzenle")
                        }
                        IconButton(onClick = onEditFolder) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Klasörü Düzenle")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Bu koleksiyon boş", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Detay sayfasından içerikleri ekleyebilirsin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    CollectionItemCard(
                        item = item,
                        reorderMode = reorderMode,
                        onClick = { if (!reorderMode) onItemClick(item) },
                        onRemove = { itemToRemove = item }
                    )
                }
            }
        }
    }

    itemToRemove?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToRemove = null },
            title = { Text("Koleksiyondan Çıkar") },
            text = { Text("\"${item.title}\" bu koleksiyondan kaldırılsın mı?") },
            confirmButton = {
                Button(onClick = { onRemoveItem(item); itemToRemove = null }) { Text("Çıkar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToRemove = null }) { Text("İptal") }
            }
        )
    }
}

@Composable
private fun CollectionItemCard(
    item: CollectionItem,
    reorderMode: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(2f / 3f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Poster placeholder — real impl uses AsyncImage(Coil)
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Title overlay
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(4.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
            }

            if (reorderMode) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
                ) {
                    Icon(Icons.Rounded.RemoveCircle, contentDescription = "Kaldır", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
