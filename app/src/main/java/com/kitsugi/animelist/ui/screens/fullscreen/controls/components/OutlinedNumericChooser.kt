package com.kitsugi.animelist.ui.screens.fullscreen.controls.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun OutlinedNumericChooser(
    value: Int,
    onChange: (Int) -> Unit,
    max: Int,
    step: Int,
    modifier: Modifier = Modifier,
    min: Int = 0,
    suffix: (@Composable () -> Unit)? = null,
    label: (@Composable () -> Unit)? = null,
) {
    assert(max > min) { "min can't be larger than max ($min > $max)" }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RepeatingIconButton(onClick = { onChange(value - step) }) {
            Icon(
                imageVector = Icons.Filled.RemoveCircle,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }
        var valueString by remember { mutableStateOf("$value") }
        LaunchedEffect(value) {
            if (valueString.isBlank() && value == 0) return@LaunchedEffect
            valueString = value.toString()
        }
        OutlinedTextField(
            label = label,
            value = valueString,
            onValueChange = { newValue ->
                if (newValue.isBlank()) {
                    valueString = newValue
                    onChange(0)
                }
                runCatching {
                    val intValue = if (newValue.trimStart() == "-") 0 else newValue.toInt()
                    onChange(intValue)
                    valueString = newValue
                }
            },
            isError = value > max || value < min,
            supportingText = {
                if (value > max) Text("Değer çok büyük", color = Color.Red)
                if (value < min) Text("Değer çok küçük", color = Color.Red)
            },
            suffix = suffix,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = KitsugiColors.Accent,
                unfocusedBorderColor = KitsugiColors.Accent.copy(alpha = 0.5f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedSuffixColor = Color.White,
                unfocusedSuffixColor = Color.White.copy(alpha = 0.7f),
                cursorColor = KitsugiColors.Accent,
            ),
            textStyle = TextStyle(color = Color.White)
        )
        RepeatingIconButton(onClick = { onChange(value + step) }) {
            Icon(
                imageVector = Icons.Filled.AddCircle,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
fun OutlinedNumericChooser(
    value: Float,
    onChange: (Float) -> Unit,
    max: Float,
    step: Float,
    modifier: Modifier = Modifier,
    min: Float = 0f,
    suffix: (@Composable () -> Unit)? = null,
    label: (@Composable () -> Unit)? = null,
) {
    assert(max > min) { "min can't be larger than max ($min > $max)" }
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RepeatingIconButton(onClick = { onChange(value - step) }) {
            Icon(
                imageVector = Icons.Filled.RemoveCircle,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }
        var valueString by remember { mutableStateOf("$value") }
        LaunchedEffect(value) {
            if (valueString.isBlank() && value == 0f) return@LaunchedEffect
            valueString = value.toString().dropLastWhile { it == '0' }.dropLastWhile { it == '.' }
        }
        OutlinedTextField(
            value = valueString,
            label = label,
            onValueChange = { newValue ->
                if (newValue.isBlank()) {
                    valueString = newValue
                    onChange(0f)
                }
                runCatching {
                    if (newValue.startsWith('.')) return@runCatching
                    val floatValue = if (newValue.trimStart() == "-") -0f else newValue.toFloat()
                    onChange(floatValue)
                    valueString = newValue
                }
            },
            isError = value > max || value < min,
            supportingText = {
                if (value > max) Text("Değer çok büyük", color = Color.Red)
                if (value < min) Text("Değer çok küçük", color = Color.Red)
            },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            suffix = suffix,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = KitsugiColors.Accent,
                unfocusedBorderColor = KitsugiColors.Accent.copy(alpha = 0.5f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedSuffixColor = Color.White,
                unfocusedSuffixColor = Color.White.copy(alpha = 0.7f),
                cursorColor = KitsugiColors.Accent,
            ),
            textStyle = TextStyle(color = Color.White)
        )
        RepeatingIconButton(onClick = { onChange(value + step) }) {
            Icon(
                imageVector = Icons.Filled.AddCircle,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }
    }
}
