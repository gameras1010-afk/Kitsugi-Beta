package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent

@Composable
fun StreamInfoOverlay(
    visible: Boolean,
    onClose: () -> Unit,
    info: StreamInfoData,
    modifier: Modifier = Modifier,
    showPlayerResolution: Boolean = true
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier.fillMaxHeight().width(320.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF080814).copy(alpha = 0.88f),
                            Color(0xFF0D0D20).copy(alpha = 0.78f)
                        )
                    )
                )
                .leftBorder(1.dp, Color.White.copy(alpha = 0.12f))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val accentColor = LocalKitsugiAccent.current
                        Icon(
                            imageVector = Icons.Rounded.Analytics,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Akış Bilgileri",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, "Kapat", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    InfoRow("Eklenti / Sağlayıcı", info.addonName ?: "Bilinmiyor")
                    if (!info.streamName.isNullOrBlank()) InfoRow("Akış Başlığı", info.streamName)
                    if (!info.streamDescription.isNullOrBlank()) InfoRow("Açıklama", info.streamDescription)
                    if (!info.filename.isNullOrBlank()) InfoRow("Dosya Adı", info.filename)
                    if (info.fileSize != null && info.fileSize > 0) {
                        val sizeStr = android.text.format.Formatter.formatFileSize(LocalContext.current, info.fileSize)
                        InfoRow("Dosya Boyutu", sizeStr)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                    InfoRow("Oynatma Motoru", info.playerEngine ?: "ExoPlayer (Internal)")
                    
                    val videoDetails = buildString {
                        if (!info.videoCodec.isNullOrBlank()) append(info.videoCodec)
                        if (showPlayerResolution && info.videoWidth != null && info.videoHeight != null) {
                            if (isNotEmpty()) append(" ")
                            append("(${info.videoWidth}x${info.videoHeight})")
                        }
                        if (info.videoFrameRate != null && info.videoFrameRate > 0) {
                            if (isNotEmpty()) append(" @ ")
                            append("%.2f fps".format(info.videoFrameRate))
                        }
                    }
                    if (videoDetails.isNotEmpty()) InfoRow("Video Formatı", videoDetails)
                    
                    if (info.videoBitrate != null && info.videoBitrate > 0) {
                        val mbps = info.videoBitrate.toFloat() / 1_000_000f
                        InfoRow("Video Bit Hızı", "%.2f Mbps".format(mbps))
                    }
 
                    val audioDetails = buildString {
                        if (!info.audioCodec.isNullOrBlank()) append(info.audioCodec)
                        if (!info.audioChannels.isNullOrBlank()) {
                            if (isNotEmpty()) append(" ")
                            append("(${info.audioChannels} ch)")
                        }
                        if (!info.audioLanguage.isNullOrBlank()) {
                            if (isNotEmpty()) append(" - ")
                            append(info.audioLanguage)
                        }
                    }
                    if (audioDetails.isNotEmpty()) InfoRow("Ses Detayları", audioDetails)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = KitsugiColors.TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}
