package com.kitsugi.animelist.ui.screens.stream

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.ui.theme.KitsugiColors
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable

/**
 * A single stream card showing quality/cache/addon badges and triggering playback on tap.
 *
 * Overhauled layout:
 *   [Thumbnail 85×120dp, click to zoom]  ->  [Text column (vertical stack)]  ->  [Action buttons]
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreamCard(
    source: StreamSource,
    accentColor: Color,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val (quality, size) = remember(source) { parseStreamQuality(source) }
    val langType = remember(source) { detectStreamLang(source) }
    val cacheState = remember(source) { getCacheState(source) }

    var showImageDialog by remember { mutableStateOf(false) }

    // Fullscreen thumbnail preview dialog
    if (showImageDialog && !source.thumbnailUrl.isNullOrBlank()) {
        Dialog(
            onDismissRequest = { showImageDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { showImageDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = KitsugiColors.SurfaceStrong),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.8f)
                        .clickable(enabled = false) {} // Prevent click-through dismissal
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = source.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { showImageDialog = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Kapat",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = KitsugiColors.SurfaceStrong.copy(alpha = 0.5f)
        )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 400.dp
            val imgWidth = if (isCompact) 95.dp else 115.dp
            val imgHeight = if (isCompact) 135.dp else 162.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Thumbnail (optional, poster style) ───────────────────
                if (!source.thumbnailUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .width(imgWidth)
                            .height(imgHeight)
                            .clip(RoundedCornerShape(10.dp))
                            .background(KitsugiColors.Surface)
                            .clickable { showImageDialog = true }
                    ) {
                        AsyncImage(
                            model = source.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Subtle bottom gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                    )
                                )
                        )
                        // Zoom indicator icon overlay
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomIn,
                                contentDescription = "Büyüt",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // ── Text column (weight=1f, stacked vertically for responsiveness) ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.Top)
                ) {
                    // Badge row
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        val qualityColor = when {
                            quality.contains("4K", ignoreCase = true) || quality.contains("2160", ignoreCase = true) -> KitsugiColors.AccentRed
                            quality.contains("1080", ignoreCase = true) -> KitsugiColors.AccentBlue
                            quality.contains("720", ignoreCase = true)  -> KitsugiColors.AccentGreen
                            else -> KitsugiColors.AccentOrange
                        }
                        StreamBadge(text = quality, color = qualityColor, bgAlpha = 0.15f, bgColor = qualityColor)

                        val (langText, langColor) = when (langType) {
                            StreamLangType.DUB     -> "🎙️ Dublaj"     to KitsugiColors.AccentOrange
                            StreamLangType.SUB     -> "💬 Altyazılı"  to KitsugiColors.AccentBlue
                            StreamLangType.DUAL    -> "🌐 Dual"       to KitsugiColors.AccentPurple
                            StreamLangType.UNKNOWN -> "🎬 Standart"   to KitsugiColors.TextSecondary
                        }
                        StreamBadge(text = langText, color = langColor, bgAlpha = 0.15f, bgColor = langColor)

                        StreamBadge(
                            text = source.addonName,
                            color = KitsugiColors.AccentPurple,
                            bgAlpha = 0.10f,
                            bgColor = KitsugiColors.AccentPurple
                        )

                        // Only show Cache State badge if it is a torrent/debrid stream (saves space!)
                        val isTorrent = !source.infoHash.isNullOrBlank() || source.url?.startsWith("magnet:") == true
                        if (isTorrent) {
                            val (cacheText, cacheColor) = when (cacheState) {
                                DebridCacheState.CACHED     -> "Önbellekte"    to KitsugiColors.AccentGreen
                                DebridCacheState.NOT_CACHED -> "İndirilecek"   to KitsugiColors.AccentOrange
                                DebridCacheState.P2P        -> "Torrent (P2P)" to KitsugiColors.AccentBlue
                            }
                            StreamBadge(text = cacheText, color = cacheColor, bgAlpha = 0.15f, bgColor = cacheColor)
                        }

                        if (size.isNotBlank()) {
                            Text(
                                text = size,
                                color = KitsugiColors.TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.CenterVertically),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Source name
                    Text(
                        text = source.name,
                        color = KitsugiColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Subtitle / detail text
                    if (source.title.isNotBlank() && source.title != source.name) {
                        Text(
                            text = source.title.trim(),
                            color = KitsugiColors.TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }

                    // Action buttons inside column for compact screens
                    if (isCompact) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onDownloadClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = "İndir",
                                    tint = KitsugiColors.TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(accentColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Oynat",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Action buttons on the right for wide screens
                if (!isCompact) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        IconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "İndir",
                                tint = KitsugiColors.TextSecondary,
                                modifier = Modifier.size(22.dp)
                                )
                            }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(accentColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Oynat",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamBadge(text: String, color: Color, bgAlpha: Float, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor.copy(alpha = bgAlpha))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
        )
    }
}


