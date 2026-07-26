package com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets

import android.text.format.DateUtils
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kitsugi.animelist.ui.screens.fullscreen.Decoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSheet(
    selectedDecoder: Decoder,
    onSelectDecoder: (Decoder) -> Unit,
    remainingTime: Int,
    onStartTimer: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    onEnterFiltersPanel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlayerSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Daha Fazla",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    var isSleepTimerDialogShown by remember { mutableStateOf(false) }
                    TextButton(onClick = { isSleepTimerDialogShown = true }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(imageVector = Icons.Outlined.Timer, contentDescription = null)
                            Text(
                                text = if (remainingTime == 0) {
                                    "Zamanlayıcı"
                                } else {
                                    DateUtils.formatElapsedTime(remainingTime.toLong())
                                },
                            )
                            if (isSleepTimerDialogShown) {
                                TimePickerDialog(
                                    remainingTime = remainingTime,
                                    onDismissRequest = { isSleepTimerDialogShown = false },
                                    onTimeSelect = onStartTimer,
                                )
                            }
                        }
                    }
                    TextButton(onClick = onEnterFiltersPanel) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null)
                            Text(text = "Filtreler")
                        }
                    }
                }
            }

            Text("Kod Çözücü Modu (Decoder)", style = MaterialTheme.typography.titleMedium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Decoder.entries) { decoder ->
                    FilterChip(
                        selected = decoder == selectedDecoder,
                        onClick = { onSelectDecoder(decoder) },
                        label = { Text(text = decoder.title) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    remainingTime: Int = 0,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .width(IntrinsicSize.Max)
                    .animateContentSize()
                    .padding(16.dp),
            ) {
                var currentLayoutType by rememberSaveable { mutableIntStateOf(0) }
                Text(
                    text = if (currentLayoutType == 1) {
                        "Zamanlayıcı Seç"
                    } else {
                        "Zamanlayıcı Gir"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val state = rememberTimePickerState(
                    remainingTime / 3600,
                    (remainingTime % 3600) / 60,
                    is24Hour = true,
                )
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    if (currentLayoutType == 1) {
                        TimePicker(state = state)
                    } else {
                        TimeInput(state = state)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentLayoutType = if (currentLayoutType == 0) 1 else 0 }) {
                        Icon(
                            imageVector = if (currentLayoutType == 0) {
                                Icons.Outlined.Schedule
                            } else {
                                Icons.Default.KeyboardAlt
                            },
                            contentDescription = null,
                        )
                    }
                    Row {
                        if (remainingTime > 0) {
                            TextButton(onClick = {
                                onTimeSelect(0)
                                onDismissRequest()
                            }) {
                                Text("İptal Et")
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                onTimeSelect(state.hour * 3600 + state.minute * 60)
                                onDismissRequest()
                            },
                        ) {
                            Text("Tamam")
                        }
                    }
                }
            }
        }
    }
}
