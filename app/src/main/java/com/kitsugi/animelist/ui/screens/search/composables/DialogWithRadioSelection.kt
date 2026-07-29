package com.kitsugi.animelist.ui.screens.search.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Ported from AniHyou's CommonAlertDialog.kt.
 * Provides single-selection (radio) and multi-selection (checkbox) dialogs
 * for the AniHyou-style search filter chips.
 */

interface KitsugiLocalizable {
    @Composable
    fun localized(): String
}

@Composable
fun <T : KitsugiLocalizable> DialogWithRadioSelection(
    values: Array<T>,
    defaultValue: T?,
    title: String? = null,
    isDeselectable: Boolean = false,
    showAllCasesOption: Boolean = false,
    onConfirm: (T?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedValue by remember { mutableStateOf(defaultValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedValue) }) {
                Text(text = "Tamam")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "İptal")
            }
        },
        title = { if (title != null) Text(text = title) },
        text = {
            LazyColumn(
                modifier = Modifier.sizeIn(maxHeight = 400.dp)
            ) {
                if (showAllCasesOption) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedValue = null },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedValue == null, onClick = { selectedValue = null })
                            Text(text = "Tümü")
                        }
                    }
                }
                items(values) { value ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedValue = if (isDeselectable) {
                                    if (selectedValue != value) value else null
                                } else value
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedValue == value,
                            onClick = {
                                selectedValue = if (isDeselectable) {
                                    if (selectedValue != value) value else null
                                } else value
                            }
                        )
                        Text(text = value.localized())
                    }
                }
            }
        }
    )
}

@Composable
fun <T : KitsugiLocalizable> DialogWithCheckboxSelection(
    values: List<T>,
    defaultValues: List<T>,
    title: String? = null,
    onConfirm: (List<T>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedValues = remember { defaultValues.toMutableStateList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedValues.toList()) }) {
                Text(text = "Tamam")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "İptal")
            }
        },
        title = { if (title != null) Text(text = title) },
        text = {
            LazyColumn(
                modifier = Modifier.sizeIn(maxHeight = 400.dp)
            ) {
                items(values) { value ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedValues.contains(value)) selectedValues.remove(value)
                                else selectedValues.add(value)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedValues.contains(value),
                            onCheckedChange = { checked ->
                                if (checked) selectedValues.add(value)
                                else selectedValues.remove(value)
                            }
                        )
                        Text(text = value.localized())
                    }
                }
            }
        }
    )
}
