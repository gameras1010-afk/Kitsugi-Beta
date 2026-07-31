package com.kitsugi.animelist.ui.screens.offline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.core.player.OfflinePlaybackHelper
import com.kitsugi.animelist.data.model.AnimeDownload
import com.kitsugi.animelist.data.local.AnimeDownloadManager
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiFullscreenPlayerActivity
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val downloads by AnimeDownloadManager.downloads.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Geri",
                    tint = KitsugiColors.TextPrimary
                )
            }
            Text(
                text = "İndirmeler",
                color = KitsugiColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
        }

        HorizontalDivider(color = KitsugiColors.Border)

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.DownloadForOffline,
                        contentDescription = null,
                        tint = KitsugiColors.TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Henüz indirilmiş veya sıraya alınmış anime yok.",
                        color = KitsugiColors.TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(downloads, key = { "${it.animeId}_${it.episode}" }) { download ->
                    DownloadItemRow(
                        download = download,
                        accentColor = accentColor,
                        onPlay = {
                            val mediaId = "${download.animeId}_${download.episode}"
                            val localMedia = OfflinePlaybackHelper.getLocalMedia(context, mediaId)
                            val params = localMedia?.let { OfflinePlaybackHelper.buildPlayerParams(it) }
                            if (params != null) {
                                KitsugiFullscreenPlayerActivity.startWithStreamUrls(
                                    context = context,
                                    videoUrl = params.videoUri,
                                    title = params.title,
                                    headers = params.headers,
                                    subtitles = params.subtitles
                                )
                            } else {
                                download.localPath?.let { path ->
                                    KitsugiFullscreenPlayerActivity.startWithStreamUrls(
                                        context = context,
                                        videoUrl = "file://$path",
                                        title = "${download.animeTitle} - Bölüm ${download.episode}",
                                        headers = emptyMap(),
                                        subtitles = emptyList()
                                    )
                                }
                            }
                        },
                        onPause = {
                            AnimeDownloadManager.pauseDownload(download.animeId, download.episode)
                        },
                        onResume = {
                            AnimeDownloadManager.resumeDownload(context, download.animeId, download.episode)
                        },
                        onDelete = {
                            AnimeDownloadManager.deleteDownload(context, download.animeId, download.episode)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadItemRow(
    download: AnimeDownload,
    accentColor: Color,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(KitsugiColors.SurfaceSoft, shape = RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Anime Poster
        AsyncImage(
            model = download.posterUrl,
            contentDescription = null,
            modifier = Modifier
                .size(width = 60.dp, height = 90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KitsugiColors.SurfaceStrong),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Info & Progress
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = download.animeTitle,
                color = KitsugiColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Bölüm ${download.episode} • ${download.quality}",
                color = KitsugiColors.TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (download.status) {
                AnimeDownload.Status.DOWNLOADING -> {
                    LinearProgressIndicator(
                        progress = { download.progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = accentColor,
                        trackColor = KitsugiColors.SurfaceStrong
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val sizeText = if (download.totalBytes > 0) {
                        "${formatBytes(download.downloadedBytes)} / ${formatBytes(download.totalBytes)}"
                    } else {
                        formatBytes(download.downloadedBytes)
                    }
                    Text(
                        text = "İndiriliyor... $sizeText (%${download.progress})",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                AnimeDownload.Status.QUEUE -> {
                    Text(
                        text = "Sırada bekliyor...",
                        color = KitsugiColors.TextMuted,
                        fontSize = 12.sp
                    )
                }
                AnimeDownload.Status.PAUSED -> {
                    Text(
                        text = "Duraklatıldı",
                        color = KitsugiColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                AnimeDownload.Status.COMPLETED -> {
                    Text(
                        text = "Tamamlandı",
                        color = KitsugiColors.AccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                AnimeDownload.Status.ERROR -> {
                    Text(
                        text = "Hata oluştu!",
                        color = KitsugiColors.AccentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (download.status) {
                AnimeDownload.Status.DOWNLOADING -> {
                    IconButton(onClick = onPause) {
                        Icon(Icons.Rounded.Pause, contentDescription = "Duraklat", tint = KitsugiColors.TextPrimary)
                    }
                }
                AnimeDownload.Status.PAUSED, AnimeDownload.Status.ERROR, AnimeDownload.Status.QUEUE -> {
                    IconButton(onClick = onResume) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Başlat", tint = accentColor)
                    }
                }
                AnimeDownload.Status.COMPLETED -> {
                    IconButton(onClick = onPlay) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Oynat", tint = KitsugiColors.AccentGreen)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Sil", tint = KitsugiColors.AccentRed)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
