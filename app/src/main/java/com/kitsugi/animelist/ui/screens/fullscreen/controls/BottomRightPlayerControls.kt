package com.kitsugi.animelist.ui.screens.fullscreen.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.ControlsButton
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.FilledControlsButton
import com.kitsugi.animelist.data.local.CustomButton

@Composable
fun BottomRightPlayerControls(
    customButton: CustomButton?,
    customButtonTitle: String,
    onPressCustomButton: () -> Unit,
    onLongPressCustomButton: () -> Unit,
    skipIntroButton: String?,
    onPressSkipIntroButton: () -> Unit,
    /** AniSkip interval aktif değilken gösterilecek sabit atlama süresi (saniye). */
    fixedSkipSec: Int = 85,
    onPressFixedSkip: () -> Unit = {},
    isPipAvailable: Boolean,
    onAspectClick: () -> Unit,
    onPipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AniSkip/AnimeSkip interval aktifse dinamik butonu göster,
        // yoksa sabit "+Xs Geç" butonunu göster
        if (skipIntroButton != null) {
            FilledControlsButton(
                text = skipIntroButton,
                onClick = onPressSkipIntroButton,
                onLongClick = {},
            )
        } else {
            // Her zaman görünen sabit intro-atlama butonu
            FilledControlsButton(
                text = "+${fixedSkipSec}s",
                onClick = onPressFixedSkip,
                onLongClick = {},
            )
        }

        if (customButton != null) {
            Spacer(modifier = Modifier.width(8.dp))
            FilledControlsButton(
                text = customButtonTitle,
                onClick = onPressCustomButton,
                onLongClick = onLongPressCustomButton,
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
