package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

// Glassmorphism renk sabitleri — tüm player kontrolleri için
private val GlassWhite   = Color.White.copy(alpha = 0.08f)
private val GlassBorder  = Color.White.copy(alpha = 0.18f)
private val GlassBlack   = Color(0xFF0A0A0F).copy(alpha = 0.55f)

// ─────────────────────────────────────────────────────────────
// Top bar — title, back, external player
// ─────────────────────────────────────────────────────────────
@Composable
fun PlayerTopBar(
    title: String,
    onBack: () -> Unit,
    onLaunchExternal: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0.0f to Color(0xFF000000).copy(alpha = 0.72f),
                    0.6f to Color(0xFF0A0A1A).copy(alpha = 0.38f),
                    1.0f to Color.Transparent
                )
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Geri butonu — glass pill
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(GlassBlack)
                .border(0.8.dp, GlassBorder, CircleShape)
                .tvClickable(shape = CircleShape, onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Geri",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        if (onLaunchExternal != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GlassBlack)
                    .border(0.8.dp, GlassBorder, CircleShape)
                    .tvClickable(shape = CircleShape) { onLaunchExternal() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Launch,
                    contentDescription = "Harici Oynatıcı",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Center play/pause button
// ─────────────────────────────────────────────────────────────
@Composable
fun PlayerCenterControl(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color(0xFF0A0A1A).copy(alpha = 0.70f)
                    )
                )
            )
            .border(1.dp, GlassBorder, CircleShape)
            .tvClickable(shape = CircleShape, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Duraklat" else "Oynat",
            tint = Color.White,
            modifier = Modifier.size(38.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Seek bar + time labels
// ─────────────────────────────────────────────────────────────
@Composable
fun PlayerSeekBar(
    currentPosition: Long,
    duration: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    previewBitmap: android.graphics.Bitmap? = null,
    onScrubPosition: ((Float) -> Unit)? = null
) {
    var isDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(0f) }

    val finalSlider = if (isDragging) sliderValue
    else if (duration > 0L) currentPosition.toFloat() / duration else 0f

    Box(modifier = modifier.fillMaxWidth()) {
        // ── Floating preview popup (shown while scrubbing) ──────────────────
        if (isDragging) {
            val thumbFraction = finalSlider.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.TopStart
            ) {
                // Calculate approximate horizontal position of the thumb
                // We use BoxWithConstraints-like approach via fraction offset
                Box(
                    modifier = Modifier
                        .fillMaxWidth(thumbFraction.coerceIn(0f, 1f))
                        .wrapContentWidth(Alignment.End)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.16f),
                                        Color(0xFF050515).copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .border(0.8.dp, GlassBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        if (previewBitmap != null && !previewBitmap.isRecycled) {
                            // Show thumbnail + timestamp
                            androidx.compose.foundation.layout.Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                androidx.compose.foundation.Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = "Preview",
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(68.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Text(
                                    text = formatMs((sliderValue * duration).toLong()),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Fallback: text-only timestamp
                            Text(
                                text = formatMs((sliderValue * duration).toLong()),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ── Seek row ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMs(if (isDragging) (sliderValue * duration).toLong() else currentPosition),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Slider(
                value = finalSlider.coerceIn(0f, 1f),
                onValueChange = {
                    isDragging = true
                    sliderValue = it
                    onScrubPosition?.invoke(it)
                },
                onValueChangeFinished = {
                    isDragging = false
                    onSeekTo((sliderValue * duration).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = LocalKitsugiAccent.current,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            )

            Text(
                text = formatMs(duration),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Bottom action buttons row
// ─────────────────────────────────────────────────────────────
@Composable
fun PlayerBottomActions(
    hasTextTracks: Boolean,
    hasMultiAudio: Boolean,
    hasSources: Boolean,
    hasEpisodes: Boolean,
    currentResizeModeLabel: String,
    currentSpeedLabel: String,
    onSubtitleClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSourcesClick: () -> Unit,
    onEpisodesClick: () -> Unit,
    onAspectClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onStreamInfoClick: () -> Unit,
    onRotateClick: () -> Unit,
    onSkipSettingsClick: (() -> Unit)? = null,
    showMediaInfo: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasEpisodes) {
            PlayerActionButton(
                icon = Icons.Rounded.ViewList,
                label = "Bölümler",
                onClick = onEpisodesClick
            )
        }
        if (hasTextTracks) {
            PlayerActionButton(
                icon = Icons.Rounded.Subtitles,
                label = "Altyazı",
                onClick = onSubtitleClick
            )
        }
        if (hasMultiAudio) {
            PlayerActionButton(
                icon = Icons.Rounded.Audiotrack,
                label = "Ses",
                onClick = onAudioClick
            )
        }
        if (hasSources) {
            PlayerActionButton(
                icon = Icons.Rounded.SwapHoriz,
                label = "Kaynak",
                onClick = onSourcesClick
            )
        }
        if (showMediaInfo) {
            PlayerActionButton(
                icon = Icons.Rounded.Analytics,
                label = "Bilgi",
                onClick = onStreamInfoClick
            )
        }
        if (onSkipSettingsClick != null) {
            PlayerActionButton(
                icon = Icons.Rounded.SkipNext,
                label = "Atlama",
                onClick = onSkipSettingsClick
            )
        }
        PlayerActionButton(
            icon = Icons.Rounded.ScreenRotation,
            label = "Döndür",
            onClick = onRotateClick
        )
        PlayerActionButton(
            icon = Icons.Rounded.AspectRatio,
            label = currentResizeModeLabel,
            onClick = onAspectClick
        )
        PlayerActionButton(
            icon = Icons.Rounded.Speed,
            label = currentSpeedLabel,
            onClick = onSpeedClick
        )
    }
}

@Composable
fun PlayerActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color(0xFF0A0A1A).copy(alpha = 0.52f)
                    )
                )
            )
            .border(0.7.dp, GlassBorder, RoundedCornerShape(10.dp))
            .tvClickable(shape = RoundedCornerShape(10.dp), onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Speed picker dialog
// ─────────────────────────────────────────────────────────────


// ─────────────────────────────────────────────────────────────
// Seek / aspect feedback overlay
// ─────────────────────────────────────────────────────────────
@Composable
fun FeedbackBubble(
    text: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = text != null,
        enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.8f),
        exit = fadeOut(tween(150)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color(0xFF050510).copy(alpha = 0.80f)
                        )
                    )
                )
                .border(0.8.dp, GlassBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text(text ?: "", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Helper: format milliseconds → HH:mm:ss or mm:ss
// ─────────────────────────────────────────────────────────────
fun formatMs(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec)
    else "%02d:%02d".format(m, sec)
}
