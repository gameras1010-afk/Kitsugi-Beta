package com.kitsugi.animelist.ui.screens.detail

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kitsugi.animelist.data.trailer.InAppYouTubeExtractor
import com.kitsugi.animelist.data.trailer.TrailerPlaybackSource
import com.kitsugi.animelist.data.trailer.YoutubeChunkedDataSourceFactory
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiFullscreenPlayerActivity
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

// ---------------------------------------------------------------------------
//  YouTube ID Extractor
// ---------------------------------------------------------------------------
internal fun extractYouTubeId(url: String): String? {
    if (url.isBlank()) return null
    val patterns = listOf(
        Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([A-Za-z0-9_-]{11})"),
        Regex("(?:youtube\\.com|youtube-nocookie\\.com)/embed/([A-Za-z0-9_-]{11})")
    )
    for (pattern in patterns) {
        val match = pattern.find(url)
        if (match != null) return match.groupValues[1]
    }
    return null
}

// ---------------------------------------------------------------------------
//  Inline YouTube Player (Native ExoPlayer tabanlı, InAppYouTubeExtractor ile)
// ---------------------------------------------------------------------------
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun KitsugiYouTubePlayer(
    videoId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var playbackSource by remember(videoId) { mutableStateOf<TrailerPlaybackSource?>(null) }
    var isLoading by remember(videoId) { mutableStateOf(true) }
    var hasError by remember(videoId) { mutableStateOf(false) }

    LaunchedEffect(videoId) {
        isLoading = true
        hasError = false
        try {
            val extractor = InAppYouTubeExtractor()
            val source = extractor.extractPlaybackSource("https://www.youtube.com/watch?v=$videoId")
            if (source != null) {
                playbackSource = source
            } else {
                hasError = true
            }
        } catch (e: Exception) {
            Log.e("KitsugiYouTubePlayer", "Extraction error for $videoId", e)
            hasError = true
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = LocalKitsugiAccent.current)
        }
    } else if (hasError || playbackSource == null) {
        Box(
            modifier = modifier
                .background(Color.Black)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Fragman yüklenemedi",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        runCatching {
                            uriHandler.openUri("https://www.youtube.com/watch?v=$videoId")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LocalKitsugiAccent.current)
                ) {
                    Text("Tarayıcıda Aç", color = KitsugiColors.Background)
                }
            }
        }
    } else {
        val source = playbackSource!!
        val exoPlayer = remember(context, videoId) {
            androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            }
        }

        DisposableEffect(exoPlayer) {
            onDispose {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.release()
            }
        }

        LaunchedEffect(source) {
            val videoUri = Uri.parse(source.videoUrl)
            if (!source.audioUrl.isNullOrBlank()) {
                val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(YoutubeChunkedDataSourceFactory())
                val videoSource = mediaSourceFactory.createMediaSource(androidx.media3.common.MediaItem.fromUri(videoUri))
                val audioSource = mediaSourceFactory.createMediaSource(androidx.media3.common.MediaItem.fromUri(Uri.parse(source.audioUrl)))
                exoPlayer.setMediaSource(androidx.media3.exoplayer.source.MergingMediaSource(videoSource, audioSource))
            } else {
                exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUri))
            }
            exoPlayer.prepare()
        }

        KitsugiPlayerGestureWrapper(
            exoPlayer = exoPlayer,
            modifier = modifier,
            onFullscreen = {
                exoPlayer.pause()
                KitsugiFullscreenPlayerActivity.startWithStreamUrls(
                    context = context,
                    videoUrl = source.videoUrl,
                    audioUrl = source.audioUrl
                )
            }
        ) {
            AndroidView(
                factory = { ctx ->
                    android.view.LayoutInflater.from(ctx).inflate(com.kitsugi.animelist.R.layout.trailer_player_view, null).apply {
                        val playerView = findViewById<androidx.media3.ui.PlayerView>(com.kitsugi.animelist.R.id.trailer_player_view)
                        playerView.player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  Oynatıcı Hareket Sarmalayıcısı (Gesture Wrapper)
// ---------------------------------------------------------------------------
@Composable
fun KitsugiPlayerGestureWrapper(
    exoPlayer: androidx.media3.exoplayer.ExoPlayer,
    modifier: Modifier = Modifier,
    onFullscreen: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }
    var playPauseFeedbackIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }

    LaunchedEffect(seekFeedbackText) {
        if (seekFeedbackText != null) {
            kotlinx.coroutines.delay(650)
            seekFeedbackText = null
        }
    }

    LaunchedEffect(playPauseFeedbackIcon) {
        if (playPauseFeedbackIcon != null) {
            kotlinx.coroutines.delay(500)
            playPauseFeedbackIcon = null
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Video Player
        content()

        // Gesture Overlay Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(exoPlayer) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val width = size.width
                            val isRightSide = offset.x > width / 2f
                            val currentPos = exoPlayer.currentPosition
                            val duration = exoPlayer.duration
                            
                            if (isRightSide) {
                                val target = (currentPos + 5000).coerceAtMost(duration)
                                exoPlayer.seekTo(target)
                                seekFeedbackText = "+5s"
                            } else {
                                val target = (currentPos - 5000).coerceAtLeast(0)
                                exoPlayer.seekTo(target)
                                seekFeedbackText = "-5s"
                            }
                        },
                        onTap = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                                playPauseFeedbackIcon = Icons.Rounded.Pause
                            } else {
                                exoPlayer.play()
                                playPauseFeedbackIcon = Icons.Rounded.PlayArrow
                            }
                        }
                    )
                }
        )

        // Seek (+/- 5s) Overlay
        AnimatedVisibility(
            visible = seekFeedbackText != null,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (seekFeedbackText?.startsWith("+") == true) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = seekFeedbackText ?: "",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Play/Pause Overlay
        AnimatedVisibility(
            visible = playPauseFeedbackIcon != null,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = playPauseFeedbackIcon ?: Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Tam ekran butonu (sağ alt köşe)
        if (onFullscreen != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .tvClickable(shape = CircleShape) { onFullscreen() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Fullscreen,
                    contentDescription = "Tam Ekran",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
