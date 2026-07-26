package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent

/**
 * Dialog shown when a previously saved playback position is detected.
 * Pauses playback and asks the user whether to resume from [pendingResumePos] or start fresh.
 */
@Composable
fun PlayerResumeDialog(
    visible: Boolean,
    pendingResumePos: Long,
    onResume: () -> Unit,
    onStartOver: () -> Unit
) {
    if (!visible) return
    val accent = LocalKitsugiAccent.current
    AlertDialog(
        onDismissRequest = onStartOver,
        containerColor = KitsugiColors.Surface,
        titleContentColor = KitsugiColors.TextPrimary,
        textContentColor = KitsugiColors.TextSecondary,
        shape = RoundedCornerShape(26.dp),
        title = {
            Text(
                text = "Kaldığınız Yerden Devam Edilsin mi?",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "Video en son ${formatMs(pendingResumePos)} konumunda kalmış. Kaldığınız yerden devam etmek ister misiniz?",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onResume) {
                Text("Evet", color = accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onStartOver) {
                Text("Hayır", color = KitsugiColors.TextSecondary)
            }
        }
    )
}

/**
 * Small skip button that appears during an active AniSkip interval (intro/outro/recap).
 * Shown only when auto-skip is disabled and the player is NOT in PiP mode.
 */
@Composable
fun AniSkipButton(
    label: String,
    isInPipMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isInPipMode) return
    androidx.compose.material3.Button(
        onClick = onClick,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f),
            contentColor = androidx.compose.ui.graphics.Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}
