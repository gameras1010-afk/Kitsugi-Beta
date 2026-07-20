package com.kitsugi.animelist.ui.screens.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.components.KitsugiEmptyState

/**
 * V2-D04 – CollectionManagementScreen
 *
 * Top-level screen listing all user collections/folders with
 * create, reorder and delete controls.
 */
data class CollectionFolder(
    val id: String,
    val name: String,
    val emoji: String,
    val itemCount: Int,
    val coverUrl: String? = null,
    val isPrivate: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionManagementScreen(
    collections: List<CollectionFolder>,
    onCreateCollection: () -> Unit,
    onOpenFolder: (CollectionFolder) -> Unit,
    onDeleteFolder: (CollectionFolder) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf<CollectionFolder?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Koleksiyonlarım", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateCollection) {
                        Icon(Icons.Rounded.Add, contentDescription = "Yeni Koleksiyon")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateCollection) {
                Icon(Icons.Rounded.Add, contentDescription = "Yeni Koleksiyon")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (collections.isEmpty()) {
            // T2-05: Standardized empty state using KitsugiEmptyState
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                KitsugiEmptyState(
                    title = "Henüz koleksiyon yok",
                    subtitle = "Favori içeriklerini düzenlemek için koleksiyon oluştur",
                    icon = Icons.Rounded.FolderOpen,
                    actionText = "Koleksiyon Oluştur",
                    onActionClick = onCreateCollection
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "${collections.size} koleksiyon",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(collections, key = { it.id }) { folder ->
                    CollectionFolderRow(
                        folder = folder,
                        onClick = { onOpenFolder(folder) },
                        onDelete = { showDeleteDialog = folder }
                    )
                }
            }
        }
    }

    showDeleteDialog?.let { folder ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Koleksiyonu Sil") },
            text = { Text("\"${folder.emoji} ${folder.name}\" koleksiyonu kalıcı olarak silinecek. İçindeki ${folder.itemCount} öğe listeden çıkarılacak.") },
            confirmButton = {
                Button(
                    onClick = { onDeleteFolder(folder); showDeleteDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sil") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = null }) { Text("İptal") }
            }
        )
    }
}

@Composable
private fun CollectionFolderRow(
    folder: CollectionFolder,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(folder.emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(folder.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        if (folder.isPrivate) {
                            Icon(Icons.Rounded.Lock, contentDescription = "Gizli", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("${folder.itemCount} öğe", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
