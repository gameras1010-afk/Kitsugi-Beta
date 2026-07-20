package com.kitsugi.animelist.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * FP-46 – Date input field for list start and finish times.
 */
@Composable
fun EditMediaDateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onPickDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            Icon(
                imageVector = Icons.Rounded.DateRange,
                contentDescription = null,
                modifier = Modifier.clickable { onPickDateClick() }
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}
