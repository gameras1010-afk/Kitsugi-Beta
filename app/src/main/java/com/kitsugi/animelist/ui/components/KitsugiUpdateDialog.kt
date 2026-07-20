package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kitsugi.animelist.core.update.AppRelease
import com.kitsugi.animelist.core.update.UpdateUiState
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent

@Composable
fun KitsugiUpdateDialog(
    state: UpdateUiState,
    onUpdateClick: () -> Unit,
    onRetryInstallClick: () -> Unit,
    onDismiss: () -> Unit
) {
    if (state is UpdateUiState.Idle || state is UpdateUiState.Checking) return

    val isDownloading = state is UpdateUiState.Downloading

    Dialog(
        onDismissRequest = {
            if (!isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = false
        )
    ) {
        KitsugiUpdateDialogBody(
            state = state,
            onUpdateClick = onUpdateClick,
            onRetryInstallClick = onRetryInstallClick,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun KitsugiUpdateDialogBody(
    state: UpdateUiState,
    onUpdateClick: () -> Unit,
    onRetryInstallClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(KitsugiColors.BackgroundElevated)
            .border(1.dp, KitsugiColors.Border, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderIcon(isFailed = state is UpdateUiState.Failed, accentColor = accentColor)

            Spacer(modifier = Modifier.height(16.dp))

            RenderStateSection(
                state = state,
                accentColor = accentColor,
                onUpdateClick = onUpdateClick,
                onRetryInstallClick = onRetryInstallClick,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun RenderStateSection(
    state: UpdateUiState,
    accentColor: Color,
    onUpdateClick: () -> Unit,
    onRetryInstallClick: () -> Unit,
    onDismiss: () -> Unit
) {
    when (state) {
        is UpdateUiState.UpdateAvailable -> {
            UpdateAvailableSection(
                release = state.release,
                accentColor = accentColor,
                onUpdateClick = onUpdateClick,
                onDismiss = onDismiss
            )
        }
        is UpdateUiState.Downloading -> {
            DownloadingSection(
                state = state,
                accentColor = accentColor
            )
        }
        is UpdateUiState.ReadyToInstall -> {
            ReadyToInstallSection(
                accentColor = accentColor,
                onRetryInstallClick = onRetryInstallClick
            )
        }
        is UpdateUiState.Failed -> {
            FailedSection(
                message = state.message,
                accentColor = accentColor,
                onUpdateClick = onUpdateClick,
                onDismiss = onDismiss
            )
        }
        else -> {}
    }
}

@Composable
private fun HeaderIcon(
    isFailed: Boolean,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFailed) Icons.Rounded.Warning else Icons.Rounded.SystemUpdate,
            contentDescription = "Güncelleme",
            tint = accentColor,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun UpdateAvailableSection(
    release: AppRelease,
    accentColor: Color,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Text(
        text = "Yeni Sürüm Mevcut! 🎉",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = KitsugiColors.TextPrimary
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Kitsugi ${release.versionName} (${release.releaseTitle})",
        style = MaterialTheme.typography.bodySmall,
        color = accentColor,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(16.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(KitsugiColors.SurfaceSoft)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column {
            Text(
                text = "Değişiklikler ve Yenilikler:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = KitsugiColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = release.releaseNotes,
                style = MaterialTheme.typography.bodySmall,
                color = KitsugiColors.TextPrimary,
                lineHeight = 18.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = KitsugiColors.TextSecondary)
        ) {
            Text("Daha Sonra")
        }

        Button(
            onClick = onUpdateClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = KitsugiColors.Background
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Güncelle", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DownloadingSection(
    state: UpdateUiState.Downloading,
    accentColor: Color
) {
    val percentageInt = (state.progress * 100).toInt()
    val downloadedMb = String.format("%.1f", state.downloadedBytes.toDouble() / (1024 * 1024))
    val totalMb = if (state.totalBytes > 0) String.format("%.1f", state.totalBytes.toDouble() / (1024 * 1024)) else "?"

    Text(
        text = "Güncelleme İndiriliyor...",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = KitsugiColors.TextPrimary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "%$percentageInt ($downloadedMb MB / $totalMb MB)",
        style = MaterialTheme.typography.bodyMedium,
        color = accentColor,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(16.dp))

    LinearProgressIndicator(
        progress = { state.progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = accentColor,
        trackColor = KitsugiColors.SurfaceSoft
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Lütfen indirme tamamlanana kadar bekleyin.",
        style = MaterialTheme.typography.bodySmall,
        color = KitsugiColors.TextMuted
    )
}

@Composable
private fun ReadyToInstallSection(
    accentColor: Color,
    onRetryInstallClick: () -> Unit
) {
    Text(
        text = "İndirme Tamamlandı!",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = KitsugiColors.TextPrimary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Kurulum başlatıldı. Kurulum ekranı açılmazsa aşağıdaki butona tıklayabilirsiniz.",
        style = MaterialTheme.typography.bodySmall,
        color = KitsugiColors.TextSecondary
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onRetryInstallClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = KitsugiColors.Background
        )
    ) {
        Text("Kurulumu Yeniden Başlat", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FailedSection(
    message: String,
    accentColor: Color,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Text(
        text = "Güncelleme Hatası",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = KitsugiColors.TextPrimary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFFF5252)
    )

    Spacer(modifier = Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Kapat")
        }

        Button(
            onClick = onUpdateClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = KitsugiColors.Background
            )
        ) {
            Text("Tekrar Dene", fontWeight = FontWeight.Bold)
        }
    }
}
