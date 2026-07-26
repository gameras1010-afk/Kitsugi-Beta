package com.kitsugi.animelist.ui.screens.fullscreen.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.ControlsButton
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.FilledControlsButton

@Composable
fun BottomRightPlayerControls(
    skipIntroButton: String?,
    onPressSkipIntroButton: () -> Unit,
    isPipAvailable: Boolean,
    onAspectClick: () -> Unit,
    onPipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (skipIntroButton != null) {
            FilledControlsButton(
                text = skipIntroButton,
                onClick = onPressSkipIntroButton,
                onLongClick = {},
            )
        }

        if (isPipAvailable) {
            ControlsButton(
                icon = Icons.Default.PictureInPictureAlt,
                onClick = onPipClick,
            )
        }

        ControlsButton(
            icon = Icons.Default.AspectRatio,
            onClick = onAspectClick,
        )
    }
}
