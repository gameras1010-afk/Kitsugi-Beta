package com.kitsugi.animelist.ui.tv.components

import android.content.Context
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.kitsugi.animelist.core.player.TvTrailerPlayerPoolHolder
import com.kitsugi.animelist.data.trailer.TrailerPlaybackSource
import kotlinx.coroutines.delay

// ─── State ────────────────────────────────────────────────────────────────────

@Stable
class TvTrailerSeekOverlayState {
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
}

// ─── Public overlay composable ────────────────────────────────────────────────

/**
 * Full-screen fragman (trailer) overlay for Android TV.
 *
 * Adapted from KitsugiTV-dev SharedTrailerOverlay + TrailerPlayer.
 *
 * Features:
 * - Full-screen Dialog with black background.
 * - DPAD key handling: Back/Escape → dismiss; OK/Enter/Play-Pause → toggle pause;
 *   Left/Right → seek (with repeat acceleration); Up → show seek bar; Down → hide seek bar.
 * - Seek overlay with animated progress bar and time display.
 * - Loading spinner while [playbackSource] is null and no error.
 * - Error state with retry action.
 * - Uses [TvTrailerPlayerPool] for lifecycle-safe Media3 ExoPlayer reuse.
 * - On dismiss: pool is stopped but NOT released (reused for next trailer).
 *
 * @param title           Title shown in the top-left corner.
 * @param playbackSource  Resolved [TrailerPlaybackSource] (video+optional audio URL). Null = loading.
 * @param isLoading       True while the URL is being resolved.
 * @param errorMessage    Non-null when resolution has failed.
 * @param onDismiss       Called when the user presses Back or the trailer ends.
 * @param onRetry         Called when the user presses the Retry button in error state.
 */
@OptIn(ExperimentalComposeUiApi::class, UnstableApi::class)
@Composable
fun TvSharedTrailerOverlay(
    title: String,
    playbackSource: TrailerPlaybackSource?,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    var isPaused by remember { mutableStateOf(false) }
    var seekOverlayVisible by remember { mutableStateOf(false) }
    val seekOverlayState = remember { TvTrailerSeekOverlayState() }
    var seekToken by remember { mutableIntStateOf(0) }
    var seekDeltaMs by remember { mutableLongStateOf(0L) }

    val canControl = playbackSource != null && !isLoading && errorMessage == null

    // Reset pause/seek state when source changes
    LaunchedEffect(playbackSource, isLoading, errorMessage) {
        isPaused = false
        seekOverlayVisible = false
    }

    // Auto-hide seek overlay after 3 seconds
    LaunchedEffect(seekOverlayVisible, canControl, seekToken) {
        if (seekOverlayVisible && canControl) {
            delay(3_000L)
            seekOverlayVisible = false
        }
    }

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
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                    handleTrailerKeyEvent(
                        keyCode = keyEvent.nativeKeyEvent.keyCode,
                        repeatCount = keyEvent.nativeKeyEvent.repeatCount,
                        canControl = canControl,
                        isPaused = isPaused,
                        onDismiss = onDismiss,
                        onTogglePause = {
                            isPaused = !isPaused
                            seekOverlayVisible = true
                        },
                        onPause = {
                            isPaused = true
                        },
                        onPlay = {
                            isPaused = false
                        },
                        onSeek = { deltaMs ->
                            seekDeltaMs = deltaMs
                            seekToken += 1
                            seekOverlayVisible = true
                        },
                        onShowSeekBar = { seekOverlayVisible = true },
                        onHideSeekBar = { seekOverlayVisible = false }
                    )
                }
        ) {
            // Player surface
            if (playbackSource != null && errorMessage == null) {
                TvTrailerPlayerSurface(
                    context = context,
                    playbackSource = playbackSource,
                    isPaused = isPaused,
                    seekToken = seekToken,
                    seekDeltaMs = seekDeltaMs,
                    onProgressChanged = { pos, dur ->
                        seekOverlayState.positionMs = pos
                        seekOverlayState.durationMs = dur
                    },
                    onEnded = onDismiss,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Title + back hint
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 48.dp, vertical = 36.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Geri dönmek için Geri tuşuna basın",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.80f)
                )
            }

            // Loading spinner
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            // Error state
            if (!errorMessage.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.20f)
                        )
                    ) {
                        Text(text = "Tekrar Dene", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Seek progress bar
            TvTrailerSeekOverlayHost(
                visible = canControl && seekOverlayVisible,
                overlayState = seekOverlayState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ─── Player surface ───────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
@Composable
private fun TvTrailerPlayerSurface(
    context: Context,
    playbackSource: TrailerPlaybackSource,
    isPaused: Boolean,
    seekToken: Int,
    seekDeltaMs: Long,
    onProgressChanged: (positionMs: Long, durationMs: Long) -> Unit,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onEndedState = rememberUpdatedState(onEnded)
    val onProgressState = rememberUpdatedState(onProgressChanged)
    val pool = remember { TvTrailerPlayerPoolHolder.get(context) }

    // Apply pause/resume
    LaunchedEffect(isPaused) {
        pool.acquire()?.playWhenReady = !isPaused
    }

    // Apply seek
    LaunchedEffect(seekToken) {
        if (seekToken <= 0) return@LaunchedEffect
        val player = pool.acquire() ?: return@LaunchedEffect
        val newPos = (player.currentPosition + seekDeltaMs).coerceIn(0L, player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE)
        player.seekTo(newPos)
    }

    // Progress polling
    LaunchedEffect(Unit) {
        while (true) {
            val player = pool.acquire()
            if (player != null) {
                onProgressState.value(player.currentPosition, player.duration.takeIf { it > 0L } ?: 0L)
            }
            delay(250L)
        }
    }

    // Load and play media
    LaunchedEffect(playbackSource) {
        val player = pool.acquire() ?: return@LaunchedEffect
        player.stop()
        player.clearMediaItems()

        val mediaSource = buildMediaSource(context, playbackSource)
        player.setMediaSource(mediaSource)
        player.prepare()
        player.volume = 1f
        player.playWhenReady = true
    }

    // Player ended listener
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onEndedState.value()
                }
            }
        }
        pool.acquire()?.addListener(listener)
        onDispose {
            pool.acquire()?.removeListener(listener)
            pool.stop()
        }
    }

    // PlayerView surface
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                this.player = pool.acquire()
            }
        },
        update = { view ->
            view.player = pool.acquire()
        },
        modifier = modifier
    )
}

// ─── Media source builder ─────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
private fun buildMediaSource(
    context: Context,
    source: TrailerPlaybackSource
): androidx.media3.exoplayer.source.MediaSource {
    val dataSourceFactory = DefaultDataSource.Factory(context)
    val factory = DefaultMediaSourceFactory(dataSourceFactory)

    // If separate audio track is available, use MergingMediaSource
    if (!source.audioUrl.isNullOrBlank() && source.audioUrl != source.videoUrl) {
        val videoSource = factory.createMediaSource(MediaItem.fromUri(source.videoUrl))
        val audioSource = factory.createMediaSource(MediaItem.fromUri(source.audioUrl))
        return androidx.media3.exoplayer.source.MergingMediaSource(videoSource, audioSource)
    }

    return factory.createMediaSource(MediaItem.fromUri(source.videoUrl))
}

// ─── Key event handler ────────────────────────────────────────────────────────

private fun handleTrailerKeyEvent(
    keyCode: Int,
    repeatCount: Int,
    canControl: Boolean,
    isPaused: Boolean,
    onDismiss: () -> Unit,
    onTogglePause: () -> Unit,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    onSeek: (deltaMs: Long) -> Unit,
    onShowSeekBar: () -> Unit,
    onHideSeekBar: () -> Unit
): Boolean = when (keyCode) {
    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
        onDismiss()
        true
    }
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_NUMPAD_ENTER,
    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
        if (!canControl) false
        else { onTogglePause(); true }
    }
    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
        if (!canControl) false
        else { onPause(); true }
    }
    KeyEvent.KEYCODE_MEDIA_PLAY -> {
        if (!canControl) false
        else { onPlay(); true }
    }
    KeyEvent.KEYCODE_DPAD_LEFT -> {
        if (!canControl) false
        else {
            val delta = when {
                repeatCount >= 12 -> -12_000L
                repeatCount >= 6 -> -8_000L
                repeatCount >= 2 -> -5_000L
                else -> -3_000L
            }
            onSeek(delta)
            true
        }
    }
    KeyEvent.KEYCODE_DPAD_RIGHT -> {
        if (!canControl) false
        else {
            val delta = when {
                repeatCount >= 12 -> 12_000L
                repeatCount >= 6 -> 8_000L
                repeatCount >= 2 -> 5_000L
                else -> 3_000L
            }
            onSeek(delta)
            true
        }
    }
    KeyEvent.KEYCODE_DPAD_UP -> {
        if (!canControl) false
        else { onShowSeekBar(); true }
    }
    KeyEvent.KEYCODE_DPAD_DOWN -> {
        if (!canControl) false
        else { onHideSeekBar(); true }
    }
    else -> false
}

// ─── Seek overlay ─────────────────────────────────────────────────────────────

@Composable
fun TvTrailerSeekOverlayHost(
    visible: Boolean,
    overlayState: TvTrailerSeekOverlayState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        TvTrailerSeekOverlay(
            currentPosition = overlayState.positionMs,
            duration = overlayState.durationMs
        )
    }
}

@Composable
private fun TvTrailerSeekOverlay(
    currentPosition: Long,
    duration: Long
) {
    val progress = if (duration > 0L) {
        (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(100),
        label = "tvTrailerSeekProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            )
            .padding(horizontal = 48.dp, vertical = 24.dp)
    ) {
        // Progress bar track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.30f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White)
            )
            // Thumb dot
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(animatedProgress)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "${formatTrailerTime(currentPosition)} / ${formatTrailerTime(duration)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.90f)
            )
        }
    }
}

// ─── Time formatter ───────────────────────────────────────────────────────────

fun formatTrailerTime(milliseconds: Long): String {
    val totalSec = (milliseconds / 1000L).coerceAtLeast(0L)
    val hours = totalSec / 3600L
    val minutes = (totalSec % 3600L) / 60L
    val seconds = totalSec % 60L
    return if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
