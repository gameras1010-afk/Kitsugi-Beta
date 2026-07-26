package com.kitsugi.animelist.ui.screens.fullscreen.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiSheets
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.ControlsButton
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.CurrentChapter
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.IndexedSegment

@Composable
fun BottomLeftPlayerControls(
    playbackSpeed: Float,
    currentChapter: IndexedSegment?,
    onLockControls: () -> Unit,
    onCycleRotation: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onOpenSheet: (KitsugiSheets) -> Unit,
    showChapters: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlsButton(
            icon = Icons.Default.LockOpen,
            onClick = onLockControls,
        )
        ControlsButton(
            icon = Icons.Default.ScreenRotation,
            onClick = onCycleRotation,
        )
        ControlsButton(
            text = "%.2fx".format(playbackSpeed),
            onClick = {
                val newSpeed = if (playbackSpeed >= 2f) 0.25f else playbackSpeed + 0.25f
                onPlaybackSpeedChange(newSpeed)
            },
            onLongClick = { onOpenSheet(KitsugiSheets.PlaybackSpeed) },
        )
        AnimatedVisibility(
            visible = currentChapter != null && showChapters,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CurrentChapter(
                chapter = currentChapter!!,
                onClick = { onOpenSheet(KitsugiSheets.Chapters) },
            )
        }
    }
}
