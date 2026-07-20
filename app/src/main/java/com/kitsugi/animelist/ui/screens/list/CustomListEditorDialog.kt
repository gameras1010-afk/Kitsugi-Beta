package com.kitsugi.animelist.ui.screens.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.components.KitsugiSheetOrDialog

/**
 * V2-F03 – CustomListEditorDialog
 *
 * AniList özel liste oluşturma/düzenleme/silme dialogu.
 * AniHyou CustomListEditorDialog referans alındı.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomListEditorDialog(
    customLists: List<String>,
    onCreateList: (String) -> Unit,
    onRenameList: (oldName: String, newName: String) -> Unit,
    onDeleteList: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newListName by remember { mutableStateOf("") }
    var editingList by remember { mutableStateOf<String?>(null) }
    var editedName by remember { mutableStateOf("") }

    KitsugiSheetOrDialog(onDismiss = onDismiss, heightFraction = 0.75f) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Özel Listeler",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Kapat")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // New list input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Yeni Liste Adı") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newListName.isNotBlank()) {
                                onCreateList(newListName.trim())
                                newListName = ""
                            }
                        }
                    )
                )
                FilledTonalIconButton(
                    onClick = {
                        if (newListName.isNotBlank()) {
                            onCreateList(newListName.trim())
                            newListName = ""
                        }
                    }
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Ekle")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (customLists.isEmpty()) {
                Text(
                    text = "Henüz özel liste yok. Yukarıdan bir tane oluşturun.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(customLists, key = { it }) { listName ->
                        CustomListRow(
                            name = listName,
                            isEditing = editingList == listName,
                            editedName = editedName,
                            onEditedNameChange = { editedName = it },
                            onEditClick = {
                                editingList = listName
                                editedName = listName
                            },
                            onSaveEdit = {
                                if (editedName.isNotBlank() && editedName != listName) {
                                    onRenameList(listName, editedName.trim())
                                }
                                editingList = null
                            },
                            onCancelEdit = { editingList = null },
                            onDeleteClick = { onDeleteList(listName) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Tamam", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CustomListRow(
    name: String,
    isEditing: Boolean,
    editedName: String,
    onEditedNameChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = onEditedNameChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSaveEdit() })
                )
                IconButton(onClick = onSaveEdit) {
                    Icon(Icons.Rounded.Add, contentDescription = "Kaydet")
                }
                IconButton(onClick = onCancelEdit) {
                    Icon(Icons.Rounded.Close, contentDescription = "İptal")
                }
            } else {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Düzenle",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
