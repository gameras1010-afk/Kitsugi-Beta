package com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class AudioTrackInfo(
    val id: Int,
    val label: String,
    val language: String? = null,
)

@Composable
fun AudioTracksSheet(
    tracks: List<AudioTrackInfo>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    onAddAudioFile: () -> Unit,
    onOpenDelayPanel: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GenericTracksSheet(
        tracks = tracks,
        onDismissRequest = onDismissRequest,
        header = {
            TrackSheetTitle(
                title = "Ses Parçaları",
                actions = {
                    TextButton(onClick = onOpenDelayPanel) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(imageVector = Icons.Default.MoreTime, contentDescription = null)
                            Text("Gecikme")
                        }
                    }
                },
            )
            AddTrackRow(title = "Harici Ses Dosyası Ekle", onClick = onAddAudioFile)
        },
        track = { track ->
            AudioTrackRow(
                title = buildAudioTitle(track),
                isSelected = selectedId == track.id,
                onClick = { onSelect(track.id) },
            )
        },
        modifier = modifier,
    )
}

@Composable
fun AudioTrackRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(
            text = title,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
            fontStyle = if (isSelected) FontStyle.Italic else FontStyle.Normal,
        )
    }
}

private fun buildAudioTitle(track: AudioTrackInfo): String {
    return when {
        track.id == -1 -> track.label
        track.language.isNullOrBlank() -> "${track.id}. ${track.label}"
        track.label.isNotBlank() -> "${track.id}. ${track.label} [${track.language}]"
        else -> "${track.id}. [${track.language}]"
    }
}
