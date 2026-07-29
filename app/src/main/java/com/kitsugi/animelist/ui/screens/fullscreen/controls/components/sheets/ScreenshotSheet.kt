package com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.screens.fullscreen.ArtType
import java.io.InputStream

@Composable
fun ScreenshotSheet(
    isLocalSource: Boolean,
    hasSubTracks: Boolean,
    showSubtitles: Boolean,
    onToggleShowSubtitles: (Boolean) -> Unit,
    cachePath: String,
    onSetAsArt: (ArtType, (() -> InputStream)) -> Unit,
    onShare: (() -> InputStream) -> Unit,
    onSave: (() -> InputStream) -> Unit,
    takeScreenshot: (String, Boolean) -> InputStream?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var setArtTypeAs: ArtType? by remember { mutableStateOf(null) }

    PlayerSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!isLocalSource) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = "Kapak Yap",
                        icon = Icons.Outlined.Photo,
                        onClick = { setArtTypeAs = ArtType.Cover },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = "Arka Plan",
                        icon = Icons.Outlined.Photo,
                        onClick = { setArtTypeAs = ArtType.Background },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = "Küçük Resim",
                        icon = Icons.Outlined.Photo,
                        onClick = { setArtTypeAs = ArtType.Thumbnail },
                    )
                }
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = "Paylaş",
                    icon = Icons.Outlined.Share,
                    onClick = {
                        val stream = takeScreenshot(cachePath, showSubtitles)
                        if (stream != null) {
                            onShare { stream }
                            onDismissRequest()
                        }
                    },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = "Kaydet",
                    icon = Icons.Outlined.Save,
                    onClick = {
                        val stream = takeScreenshot(cachePath, showSubtitles)
                        if (stream != null) {
                            onSave { stream }
                            onDismissRequest()
                        }
                    },
                )
            }

            if (hasSubTracks) {
                SwitchPreference(
                    value = showSubtitles,
                    onValueChange = onToggleShowSubtitles,
                    modifier = Modifier.padding(bottom = 16.dp),
                    content = {
                        Text(
                            text = "Altyazıları Dahil Et",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
            }
        }
    }

    if (setArtTypeAs != null) {
        val artName = when (setArtTypeAs!!) {
            ArtType.Cover -> "Kapak Resmi"
            ArtType.Background -> "Duvar Kağıdı"
            ArtType.Thumbnail -> "Küçük Resim"
        }
        AlertDialog(
            onDismissRequest = { setArtTypeAs = null },
            title = { Text("Resmi Ayarla") },
            text = { Text("Bu ekran görüntüsünü $artName olarak ayarlamak istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(onClick = {
                    val artType = setArtTypeAs!!
                    val stream = takeScreenshot(cachePath, showSubtitles)
                    if (stream != null) {
                        onSetAsArt(artType) { stream }
                    }
                    setArtTypeAs = null
                    onDismissRequest()
                }) {
                    Text("Evet")
                }
            },
            dismissButton = {
                TextButton(onClick = { setArtTypeAs = null }) {
                    Text("Hayır")
                }
            }
        )
    }
}
