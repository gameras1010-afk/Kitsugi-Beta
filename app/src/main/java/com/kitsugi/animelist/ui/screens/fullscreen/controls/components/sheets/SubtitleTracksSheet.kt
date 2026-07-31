package com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.core.player.SubtitleInput

data class SubtitleTrackInfo(
    val id: Int,
    val label: String,
    val language: String? = null,
)

@Composable
fun SubtitleTracksSheet(
    tracks: List<SubtitleTrackInfo>,
    selectedIndices: List<Int>,
    onSelect: (Int) -> Unit,
    onAddSubtitleFile: () -> Unit,
    onOpenSubtitleSettings: () -> Unit,
    onOpenSubtitleDelay: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allTracks = androidx.compose.runtime.remember(tracks) {
        listOf(SubtitleTrackInfo(id = -99, label = "Altyazıyı Kapat")) + tracks
    }
    val allSelected = androidx.compose.runtime.remember(selectedIndices) {
        if (selectedIndices.isEmpty()) listOf(-99) else selectedIndices
    }

    GenericTracksSheet(
        tracks = allTracks,
        onDismissRequest = onDismissRequest,
        header = {
            TrackSheetTitle(
                title = "Altyazılar",
                actions = {
                    TextButton(onClick = onOpenSubtitleSettings) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null)
                            Text("Stil")
                        }
                    }
                    TextButton(onClick = onOpenSubtitleDelay) {
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
            AddTrackRow(title = "Harici Altyazı Ekle", onClick = onAddSubtitleFile)
        },
        track = { track ->
            SubtitleTrackRow(
                title = if (track.id == -99) track.label else buildTrackTitle(track),
                selected = if (allSelected.contains(track.id)) 0 else -1,
                onClick = { onSelect(track.id) },
            )
        },
        footer = {
            Column(
                modifier = modifier.padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Icon(Icons.Outlined.Info, null)
                Text(
                    "İkincil altyazı için yalnızca ASS/SSA formatı desteklenmektedir.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
fun SubtitleTrackRow(
    title: String,
    selected: Int, // -1=seçilmedi, 0=1. seçim, 1=2. seçim
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected > -1,
            onCheckedChange = { onClick() },
        )
        Text(
            text = title,
            fontStyle = if (selected > -1) FontStyle.Italic else FontStyle.Normal,
            fontWeight = if (selected > -1) FontWeight.ExtraBold else FontWeight.Normal,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (selected != -1) {
            Text(
                text = "#${selected + 1}",
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

private fun buildTrackTitle(track: SubtitleTrackInfo): String {
    return when {
        track.id == -1 -> track.label
        track.language.isNullOrBlank() && track.label.isNotBlank() -> "${track.id}. ${track.label}"
        !track.language.isNullOrBlank() && track.label.isNotBlank() -> "${track.id}. ${track.label} [${track.language}]"
        !track.language.isNullOrBlank() -> "${track.id}. [${track.language}]"
        else -> "${track.id}. ${track.label}"
    }
}
