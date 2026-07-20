package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun KitsugiSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalKitsugiAccent.current

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(22.dp),
        leadingIcon = {
            Text(
                text = "⌕",
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium
            )
        },
        trailingIcon = {
            if (value.isNotBlank()) {
                TextButton(
                    onClick = {
                        onValueChange("")
                    }
                ) {
                    Text(
                        text = "Sil",
                        color = accentColor,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        },
        placeholder = {
            Text(
                text = placeholder,
                color = KitsugiColors.TextMuted
            )
        },
        colors = TextFieldDefaults.colors(
            focusedTextColor = KitsugiColors.TextPrimary,
            unfocusedTextColor = KitsugiColors.TextPrimary,
            focusedContainerColor = KitsugiColors.Surface,
            unfocusedContainerColor = KitsugiColors.Surface,
            focusedIndicatorColor = accentColor,
            unfocusedIndicatorColor = KitsugiColors.Border,
            cursorColor = accentColor
        )
    )
}