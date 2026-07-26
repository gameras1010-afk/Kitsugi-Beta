package com.kitsugi.animelist.ui.screens.fullscreen.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.kitsugi.animelist.data.remote.KitsugiStreamingEpisode
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiDialogs

@Composable
fun PlayerDialogsHost(
    dialogShown: KitsugiDialogs,
    episodes: List<KitsugiStreamingEpisode>,
    currentEpisode: Int,
    onPlayEpisode: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (dialogShown) {
        is KitsugiDialogs.None -> Unit
        is KitsugiDialogs.EpisodeList -> {
            EpisodeListDialog(
                episodes = episodes,
                currentEpisode = currentEpisode,
                onPlayEpisode = onPlayEpisode,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
            )
        }
        is KitsugiDialogs.IntegerPicker -> {
            IntegerPickerDialog(
                picker = dialogShown,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun EpisodeListDialog(
    episodes: List<KitsugiStreamingEpisode>,
    currentEpisode: Int,
    onPlayEpisode: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF16162A).copy(alpha = 0.95f),
                contentColor = Color.White
            ),
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Bölüm Listesi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(episodes) { episode ->
                        val isCurrent = episode.episodeNumber == currentEpisode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) Color.White.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    episode.episodeNumber?.let { onPlayEpisode(it) }
                                    onDismissRequest()
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!episode.thumbnail.isNullOrBlank()) {
                                AsyncImage(
                                    model = episode.thumbnail,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 80.dp, height = 45.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Bölüm ${episode.episodeNumber ?: "?"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White
                                )
                                if (!episode.title.isNullOrBlank()) {
                                    Text(
                                        text = episode.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = "Kapat")
                    }
                }
            }
        }
    }
}

@Composable
fun IntegerPickerDialog(
    picker: KitsugiDialogs.IntegerPicker,
    modifier: Modifier = Modifier,
) {
    var currentValue by remember { mutableFloatStateOf(picker.defaultValue.toFloat()) }

    AlertDialog(
        onDismissRequest = picker.onDismissRequest,
        title = {
            Text(text = picker.title)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val formattedText = picker.nameFormat.format(currentValue.toInt())
                Text(
                    text = formattedText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = currentValue,
                    onValueChange = {
                        val snapped = (Math.round(it / picker.step) * picker.step).toFloat()
                        currentValue = snapped.coerceIn(picker.minValue.toFloat(), picker.maxValue.toFloat())
                    },
                    valueRange = picker.minValue.toFloat()..picker.maxValue.toFloat(),
                    steps = ((picker.maxValue - picker.minValue) / picker.step) - 1
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    picker.onChange(currentValue.toInt())
                    picker.onDismissRequest()
                }
            ) {
                Text(text = "Uygula")
            }
        },
        dismissButton = {
            TextButton(onClick = picker.onDismissRequest) {
                Text(text = "İptal")
            }
        },
        modifier = modifier
    )
}
