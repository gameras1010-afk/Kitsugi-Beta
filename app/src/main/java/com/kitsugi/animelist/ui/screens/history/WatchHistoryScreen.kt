package com.kitsugi.animelist.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.local.WatchHistoryManager
import com.kitsugi.animelist.data.model.WatchHistoryEntry
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WatchHistoryScreen(
    onBack: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val history by WatchHistoryManager.history.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
            .statusBarsPadding()
    ) {
        // ── Top Bar ────────────────────────────────────────────────────────────
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
                text = "İzleme Geçmişi",
                color = KitsugiColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
            AnimatedVisibility(visible = history.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = "Tümünü Temizle",
                        tint = KitsugiColors.TextMuted
                    )
                }
            }
        }

        HorizontalDivider(color = KitsugiColors.Border)

        // ── Content ────────────────────────────────────────────────────────────
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = KitsugiColors.TextMuted,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "Henüz izleme geçmişin yok.",
                        color = KitsugiColors.TextMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Bir video izlemeye başladığında burada görünecek.",
                        color = KitsugiColors.TextMuted.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { "${it.animeId}_${it.episode}_${it.watchedAtMs}" }) { entry ->
                    WatchHistoryItem(
                        entry = entry,
                        accentColor = accentColor,
                        onDelete = {
                            WatchHistoryManager.remove(entry.animeId, entry.episode)
                        }
                    )
                }
            }
        }
    }

    // ── Tümünü temizle dialog'u ────────────────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = KitsugiColors.Surface,
            titleContentColor = KitsugiColors.TextPrimary,
            title = { Text("Geçmişi Temizle", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Tüm izleme geçmişin silinecek. Bu işlem geri alınamaz.",
                    color = KitsugiColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        WatchHistoryManager.clearAll()
                        showClearDialog = false
                    }
                ) {
                    Text("Temizle", color = KitsugiColors.AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("İptal", color = accentColor)
                }
            }
        )
    }
}

@Composable
private fun WatchHistoryItem(
    entry: WatchHistoryEntry,
    accentColor: androidx.compose.ui.graphics.Color,
    onDelete: () -> Unit
) {
    val dateStr = remember(entry.watchedAtMs) {
        SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault())
            .format(Date(entry.watchedAtMs))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(KitsugiColors.SurfaceSoft, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Poster
        Box(
            modifier = Modifier
                .size(width = 58.dp, height = 86.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KitsugiColors.SurfaceStrong)
        ) {
            if (!entry.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = entry.posterUrl,
                    contentDescription = entry.animeTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay alt kısımda
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    androidx.compose.ui.graphics.Color.Transparent,
                                    KitsugiColors.Background.copy(alpha = 0.5f)
                                ),
                                startY = 40f
                            )
                        )
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Movie, null, tint = KitsugiColors.TextMuted, modifier = Modifier.size(24.dp))
                }
            }
        }

        // Info
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = entry.animeTitle,
                color = KitsugiColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (entry.isMovie) "Film" else "Sezon ${entry.season} · Bölüm ${entry.episode}",
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!entry.source.isNullOrBlank() || !entry.quality.isNullOrBlank()) {
                Text(
                    text = listOfNotNull(entry.source, entry.quality).joinToString(" · "),
                    color = KitsugiColors.TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.AccessTime, null, tint = KitsugiColors.TextMuted, modifier = Modifier.size(11.dp))
                Text(text = dateStr, color = KitsugiColors.TextMuted, fontSize = 11.sp)
            }
        }

        // Sil butonu
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Sil",
                tint = KitsugiColors.TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
