package com.kitsugi.animelist.ui.tv.home

import android.net.Uri
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.trailer.InAppYouTubeExtractor
import com.kitsugi.animelist.data.trailer.TrailerPlaybackSource
import com.kitsugi.animelist.data.trailer.YoutubeChunkedDataSourceFactory
import com.kitsugi.animelist.model.MediaType

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvModernHero(
    item: JikanSearchResult?,
    trailerUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backdropUrl = item?.backdropUrl ?: item?.imageUrl
    
    // Manage state via the state machine
    val stateHolder = rememberTvHeroState(backdropUrl = backdropUrl, trailerUrl = trailerUrl)
    val state = stateHolder.state

    val playerPool = remember { com.kitsugi.animelist.core.player.TvTrailerPlayerPoolHolder.get(context) }
    
    DisposableEffect(Unit) {
        onDispose {
            playerPool.yield()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(430.dp)
            .background(Color.Black)
    ) {
        // 1. Static Backdrop Base
        if (backdropUrl != null) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = item?.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Video Player Layer (Visible only when PlayingTrailer)
        if (state is TvHeroState.PlayingTrailer) {
            var playbackSource by remember(state.videoId) { mutableStateOf<TrailerPlaybackSource?>(null) }
            var isVideoReady by remember { mutableStateOf(false) }

            LaunchedEffect(state.videoId) {
                try {
                    val extractor = InAppYouTubeExtractor()
                    val source = extractor.extractPlaybackSource("https://www.youtube.com/watch?v=${state.videoId}")
                    if (source != null) {
                        playbackSource = source
                    }
                } catch (e: Exception) {
                    Log.e("TvModernHero", "Extraction error for ${state.videoId}", e)
                }
            }

            playbackSource?.let { source ->
                val exoPlayer = remember(playerPool, state.videoId) {
                    playerPool.reclaim()
                    playerPool.acquire()?.apply {
                        volume = 0f // Muted playback for ambient trailer
                        repeatMode = Player.REPEAT_MODE_ONE
                        playWhenReady = true
                    }
                }

                if (exoPlayer != null) {
                    DisposableEffect(exoPlayer) {
                        val listener = object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_READY) {
                                    isVideoReady = true
                                }
                            }
                        }
                        exoPlayer.addListener(listener)
                        onDispose {
                            exoPlayer.removeListener(listener)
                            playerPool.yield()
                        }
                    }

                    LaunchedEffect(source) {
                        val videoUri = Uri.parse(source.videoUrl)
                        if (!source.audioUrl.isNullOrBlank()) {
                            val mediaSourceFactory = DefaultMediaSourceFactory(YoutubeChunkedDataSourceFactory())
                            val videoSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUri))
                            val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(Uri.parse(source.audioUrl)))
                            exoPlayer.setMediaSource(MergingMediaSource(videoSource, audioSource))
                        } else {
                            exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
                        }
                        exoPlayer.prepare()
                    }

                    Crossfade(targetState = isVideoReady, animationSpec = tween(800), label = "video_crossfade") { ready ->
                        if (ready) {
                            AndroidView(
                                factory = { ctx ->
                                    android.view.LayoutInflater.from(ctx).inflate(
                                        com.kitsugi.animelist.R.layout.trailer_player_view,
                                        null
                                    ).apply {
                                        val playerView = findViewById<androidx.media3.ui.PlayerView>(
                                            com.kitsugi.animelist.R.id.trailer_player_view
                                        )
                                        playerView.player = exoPlayer
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Trigger play once the machine hits LoadingTrailer
        if (state is TvHeroState.LoadingTrailer) {
            LaunchedEffect(state.videoId) {
                // Instantly advance state to PlayingTrailer (the actual video view takes care of loading)
                stateHolder.setPlaying(state.videoId, backdropUrl)
            }
        }

        // 3. Ambient Shadows & Gradient Masks (consistent with original layout)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 1100f
                    )
                )
        )

        // 4. Hero Metadata Block
        if (item != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 58.dp, bottom = 48.dp)
                    .width(480.dp)
            ) {
                // Score, Type & Year badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item.score?.let { score ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "★ $score",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.type.name.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (item.year != null && item.year > 0) {
                        Text(
                            text = item.year.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                val description = item.subtitle
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
