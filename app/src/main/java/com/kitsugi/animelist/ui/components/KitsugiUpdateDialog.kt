package com.kitsugi.animelist.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.kitsugi.animelist.BuildConfig
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
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
            ) {
                KitsugiUpdateBottomSheetBody(
                    state = state,
                    onUpdateClick = onUpdateClick,
                    onRetryInstallClick = onRetryInstallClick,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun KitsugiUpdateBottomSheetBody(
    state: UpdateUiState,
    onUpdateClick: () -> Unit,
    onRetryInstallClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val isAvailable = state is UpdateUiState.UpdateAvailable

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isAvailable) {
                    Modifier.fillMaxHeight(0.85f)
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .border(
                width = 1.dp,
                color = KitsugiColors.Border.copy(alpha = 0.5f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ),
        color = KitsugiColors.BackgroundElevated,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isAvailable) {
                        Modifier.fillMaxHeight()
                    } else {
                        Modifier
                    }
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Handle Bar
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(KitsugiColors.TextMuted.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is UpdateUiState.UpdateAvailable -> {
                    UpdateAvailableBottomSheetContent(
                        release = state.release,
                        accentColor = accentColor,
                        onUpdateClick = onUpdateClick,
                        onDismiss = onDismiss
                    )
                }
                is UpdateUiState.Downloading -> {
                    DownloadingBottomSheetContent(
                        state = state,
                        accentColor = accentColor
                    )
                }
                is UpdateUiState.ReadyToInstall -> {
                    ReadyToInstallBottomSheetContent(
                        accentColor = accentColor,
                        onRetryInstallClick = onRetryInstallClick
                    )
                }
                is UpdateUiState.Failed -> {
                    FailedBottomSheetContent(
                        message = state.message,
                        accentColor = accentColor,
                        onUpdateClick = onUpdateClick,
                        onDismiss = onDismiss
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ColumnScope.UpdateAvailableBottomSheetContent(
    release: AppRelease,
    accentColor: Color,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.NewReleases,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(30.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Yeni Sürüm Mevcut! 🚀",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = KitsugiColors.TextPrimary
            )
            Text(
                text = "v${BuildConfig.VERSION_NAME} < v${release.versionName}",
                style = MaterialTheme.typography.bodyMedium,
                color = KitsugiColors.TextSecondary
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Badges / Tags Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VersionBadge(text = "v${release.versionName}", accentColor = accentColor)
        VersionBadge(text = BuildConfig.FLAVOR.uppercase(), accentColor = KitsugiColors.TextMuted)
        if (release.apkSizeBytes > 0) {
            val sizeMb = String.format("%.1f MB", release.apkSizeBytes.toDouble() / (1024 * 1024))
            VersionBadge(text = sizeMb, accentColor = KitsugiColors.TextMuted)
        }
    }

    if (release.publishedAt.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Rounded.AccessTime,
                contentDescription = null,
                tint = KitsugiColors.TextMuted,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "Yayınlanma: ${release.publishedAt}",
                style = MaterialTheme.typography.labelSmall,
                color = KitsugiColors.TextMuted
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Release Notes Box with Clean Formatting
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))

            .background(KitsugiColors.SurfaceSoft)
            .border(1.dp, KitsugiColors.Border.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column {
            Text(
                text = "Değişiklikler ve Yenilikler",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            CleanReleaseNotesText(notes = release.releaseNotes)
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Action Buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = KitsugiColors.TextSecondary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, KitsugiColors.Border)
        ) {
            Text("Daha Sonra", fontWeight = FontWeight.SemiBold)
        }

        Button(
            onClick = onUpdateClick,
            modifier = Modifier
                .weight(1.4f)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = KitsugiColors.Background
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text("Şimdi Güncelle", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DownloadingBottomSheetContent(
    state: UpdateUiState.Downloading,
    accentColor: Color
) {
    val percentageInt = (state.progress * 100).toInt()
    val downloadedMb = String.format("%.1f", state.downloadedBytes.toDouble() / (1024 * 1024))
    val totalMb = if (state.totalBytes > 0) String.format("%.1f", state.totalBytes.toDouble() / (1024 * 1024)) else "?"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Güncelleme İndiriliyor...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = KitsugiColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "%$percentageInt  •  $downloadedMb MB / $totalMb MB",
            style = MaterialTheme.typography.bodyMedium,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
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
}

@Composable
private fun ReadyToInstallBottomSheetContent(
    accentColor: Color,
    onRetryInstallClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "İndirme Tamamlandı! 🎉",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = KitsugiColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Kurulum başlatıldı. Kurulum ekranı otomatik açılmazsa aşağıdaki butona tıklayın.",
            style = MaterialTheme.typography.bodyMedium,
            color = KitsugiColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetryInstallClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = KitsugiColors.Background
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("Kurulumu Başlat", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FailedBottomSheetContent(
    message: String,
    accentColor: Color,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF5252).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Güncelleme İndirilemedi",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = KitsugiColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFF5252)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Kapat")
            }

            Button(
                onClick = onUpdateClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = KitsugiColors.Background
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Tekrar Dene", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun VersionBadge(
    text: String,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Composable
private fun CleanReleaseNotesText(notes: String) {
    val lines = notes.lines()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#") -> {
                    val cleanText = trimmed.replace("#", "").trim()
                    if (cleanText.isNotEmpty()) {
                        Text(
                            text = cleanText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LocalKitsugiAccent.current,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
                }
                trimmed.startsWith("---") -> {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(KitsugiColors.Border.copy(alpha = 0.4f))
                            .padding(vertical = 4.dp)
                    )
                }
                trimmed.startsWith("-") || trimmed.startsWith("*") -> {
                    val cleanText = trimmed.removePrefix("-").removePrefix("*").replace("**", "").replace("`", "").trim()
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = LocalKitsugiAccent.current,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = cleanText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = KitsugiColors.TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
                trimmed.isNotEmpty() -> {
                    val cleanText = trimmed.replace("**", "").replace("`", "").trim()
                    Text(
                        text = cleanText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KitsugiColors.TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
