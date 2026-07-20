package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun TorrentOverlay(
    visible: Boolean,
    downloadSpeedBytes: Long,
    uploadSpeedBytes: Long,
    seeders: Int,
    peers: Int,
    bufferPercent: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CloudDownload, "Torrent", tint = KitsugiColors.AccentGreen, modifier = Modifier.size(16.dp))
                    Text("Torrent Bağlantısı", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                val ds = android.text.format.Formatter.formatFileSize(LocalContext.current, downloadSpeedBytes) + "/s"
                val us = android.text.format.Formatter.formatFileSize(LocalContext.current, uploadSpeedBytes) + "/s"

                Text("İndirme: $ds", color = KitsugiColors.AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("Yükleme: $us", color = KitsugiColors.TextSecondary, fontSize = 11.sp)
                Text("Seeder: $seeders | Peer: $peers", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { bufferPercent / 100f },
                        color = KitsugiColors.AccentGreen,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.width(80.dp).height(4.dp).clip(CircleShape)
                    )
                    Text("Tampon: $bufferPercent%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
