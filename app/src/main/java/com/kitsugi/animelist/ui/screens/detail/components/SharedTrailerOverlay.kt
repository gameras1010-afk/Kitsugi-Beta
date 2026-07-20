package com.kitsugi.animelist.ui.screens.detail.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.PlayerView
import com.kitsugi.animelist.data.trailer.InAppYouTubeExtractor
import com.kitsugi.animelist.data.trailer.TrailerPlaybackSource
import com.kitsugi.animelist.data.trailer.YoutubeChunkedDataSourceFactory
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.delay

/**
 * FP-20 – Full-screen fragman (trailer) overlay for Mobile (TASK-405).
 *
 * Features:
 * - Full-screen Dialog with black background.
 * - Resolves YouTube URL via [InAppYouTubeExtractor] in background.
 * - Loading spinner while URL is being resolved.
 * - Error state with "YouTube'da Aç" fallback.
 * - Tap → play/pause toggle; double-tap left/right → seek ±10s.
 * - Controls auto-hide after 3 seconds of inactivity.
 * - Animated seek progress bar at the bottom.
 * - On dismiss: player is stopped and released.
 *
 * @param title        Title shown in the top-left corner.
 * @param trailerUrl   YouTube URL (or video ID) for the trailer.
 * @param onDismiss    Called when the user closes the overlay.
 */
@OptIn(UnstableApi::class)
@Composable
fun SharedTrailerOverlay(
    title: String = "",
    trailerUrl: String?,
    onDismiss: () -> Unit
) {
    if (trailerUrl.isNullOrBlank()) return

    val context = LocalContext.current

    // ── Extraction state ──────────────────────────────────────────────────────
    var playbackSource by remember(trailerUrl) { mutableStateOf<TrailerPlaybackSource?>(null) }
    var isLoading by remember(trailerUrl) { mutableStateOf(true) }
    var errorMessage by remember(trailerUrl) { mutableStateOf<String?>(null) }
    var retryToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(trailerUrl, retryToken) {
        isLoading = true
        errorMessage = null
        playbackSource = null
        try {
            val extractor = InAppYouTubeExtractor()
            val source = extractor.extractPlaybackSource(trailerUrl)
            if (source != null) {
                playbackSource = source
            } else {
                errorMessage = "Fragman yüklenemedi"
            }
        } catch (e: Exception) {
            errorMessage = "Bağlantı hatası"
        } finally {
            isLoading = false
        }
    }

    // ── Player lifecycle ──────────────────────────────────────────────────────
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    // Load source into player
    LaunchedEffect(playbackSource) {
        val source = playbackSource ?: return@LaunchedEffect
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        val factory = YoutubeChunkedDataSourceFactory()
        val mediaSourceFactory = DefaultMediaSourceFactory(factory)

        if (!source.audioUrl.isNullOrBlank() && source.audioUrl != source.videoUrl) {
            val videoSrc = mediaSourceFactory.createMediaSource(MediaItem.fromUri(Uri.parse(source.videoUrl)))
            val audioSrc = mediaSourceFactory.createMediaSource(MediaItem.fromUri(Uri.parse(source.audioUrl)))
            exoPlayer.setMediaSource(MergingMediaSource(videoSrc, audioSrc))
        } else {
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(source.videoUrl)))
        }
        exoPlayer.prepare()
    }

    // ── Playback UI state ─────────────────────────────────────────────────────
    var isPaused by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var seekFeedback by remember { mutableStateOf<String?>(null) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var controlsToken by remember { mutableIntStateOf(0) }

    // Progress polling
    LaunchedEffect(playbackSource) {
        while (true) {
            positionMs = exoPlayer.currentPosition
            durationMs = exoPlayer.duration.takeIf { it > 0L } ?: 0L
            delay(250L)
        }
    }

    // Apply pause/resume
    LaunchedEffect(isPaused) {
        exoPlayer.playWhenReady = !isPaused
    }

    // Auto-hide controls after 3s
    LaunchedEffect(controlsVisible, controlsToken) {
        if (controlsVisible && playbackSource != null) {
            delay(3_000L)
            controlsVisible = false
        }
    }

    // Seek feedback auto-clear
    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            delay(700L)
            seekFeedback = null
        }
    }

    // Player ended → dismiss
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) onDismiss()
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // ── Dialog ────────────────────────────────────────────────────────────────
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // ── Player surface ─────────────────────────────────────────────
            if (playbackSource != null && errorMessage == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(exoPlayer) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    val isRight = offset.x > size.width / 2f
                                    if (isRight) {
                                        val target = (exoPlayer.currentPosition + 10_000L)
                                            .coerceAtMost(exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
                                        exoPlayer.seekTo(target)
                                        seekFeedback = "+10s"
                                    } else {
                                        val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                        exoPlayer.seekTo(target)
                                        seekFeedback = "-10s"
                                    }
                                    controlsVisible = true
                                    controlsToken++
                                },
                                onTap = {
                                    if (controlsVisible) {
                                        isPaused = !isPaused
                                    }
                                    controlsVisible = true
                                    controlsToken++
                                }
                            )
                        }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                player = exoPlayer
                            }
                        },
                        update = { view -> view.player = exoPlayer },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // ── Loading spinner ────────────────────────────────────────────
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            // ── Error state ────────────────────────────────────────────────
            if (!errorMessage.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = errorMessage ?: "Hata",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { retryToken++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.20f)
                            )
                        ) {
                            Text(text = "Tekrar Dene", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Top bar (title + close) — shown on tap ─────────────────────
            AnimatedVisibility(
                visible = controlsVisible || isLoading,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Kapat",
                            tint = Color.White
                        )
                    }
                }
            }

            // ── Centre play/pause indicator ────────────────────────────────
            AnimatedVisibility(
                visible = controlsVisible && playbackSource != null && errorMessage == null,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // ── Seek feedback (+10s / -10s) ────────────────────────────────
            AnimatedVisibility(
                visible = seekFeedback != null,
                enter = fadeIn(tween(100)),
                exit = fadeOut(tween(150)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (seekFeedback?.startsWith("+") == true)
                                Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = seekFeedback ?: "",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Bottom progress bar ────────────────────────────────────────
            AnimatedVisibility(
                visible = (controlsVisible || !isLoading) && playbackSource != null && durationMs > 0L,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                MobileTrailerSeekBar(
                    positionMs = positionMs,
                    durationMs = durationMs
                )
            }
        }
    }
}

// ─── Bottom seek bar ──────────────────────────────────────────────────────────

@Composable
private fun MobileTrailerSeekBar(
    positionMs: Long,
    durationMs: Long
) {
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(100),
        label = "mobileTrailerProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.30f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(2.dp))
                    .background(KitsugiColors.AccentPurple)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "${formatMobileTrailerTime(positionMs)} / ${formatMobileTrailerTime(durationMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.80f)
            )
        }
    }
}

private fun formatMobileTrailerTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSec / 60L
    val seconds = totalSec % 60L
    return String.format("%02d:%02d", minutes, seconds)
}
