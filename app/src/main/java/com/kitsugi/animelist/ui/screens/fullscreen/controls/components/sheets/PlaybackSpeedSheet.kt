package com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.roundToInt

private val DEFAULT_SPEED_PRESETS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

@Composable
fun PlaybackSpeedSheet(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onSetAsDefault: (Float) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var speedPresets by remember { mutableStateOf(DEFAULT_SPEED_PRESETS) }

    PlayerSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
            // Speed label + slider
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Oynatma Hızı", style = MaterialTheme.typography.titleMedium)
                    Text("%.2fx".format(speed), style = MaterialTheme.typography.titleMedium)
                }
                Slider(
                    value = speed,
                    onValueChange = onSpeedChange,
                    valueRange = 0.1f..6f,
                    steps = 0,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Preset chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalIconButton(onClick = { speedPresets = DEFAULT_SPEED_PRESETS }) {
                    Icon(Icons.Default.RestartAlt, null)
                }
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(speedPresets.sorted(), key = { it }) { preset ->
                        InputChip(
                            selected = speed.toFixed() == preset.toFixed(),
                            onClick = { onSpeedChange(preset) },
                            label = { Text("${preset}x") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    modifier = Modifier.clickable {
                                        speedPresets = speedPresets - preset
                                    },
                                )
                            },
                        )
                    }
                }
                FilledTonalIconButton(onClick = {
                    if (speed !in speedPresets) speedPresets = speedPresets + speed.toFixed()
                }) {
                    Icon(Icons.Default.Add, null)
                }
            }

            // Actions
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onSetAsDefault(speed) },
                ) { Text("Varsayılan Yap") }
                FilledIconButton(onClick = { onSpeedChange(1f) }) {
                    Icon(Icons.Default.RestartAlt, null)
                }
            }
        }
    }
}

fun Float.toFixed(precision: Int = 2): Float {
    val factor = 10.0f.pow(precision)
    return (this * factor).roundToInt() / factor
}
