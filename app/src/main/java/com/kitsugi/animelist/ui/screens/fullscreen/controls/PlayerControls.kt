package com.kitsugi.animelist.ui.screens.fullscreen.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiSheets
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.IndexedSegment
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.SeekbarWithTimers
import dev.vivvvek.seeker.Segment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun PlayerControls(
    controlsShown: Boolean,
    areControlsLocked: Boolean,
    onUnlockControls: () -> Unit,
    onLockControls: () -> Unit,

    // Titles
    animeTitle: String,
    mediaTitle: String,
    onTitleClick: () -> Unit,
    onBackClick: () -> Unit,

    // Top Right actions
    autoPlayEnabled: Boolean,
    onToggleAutoPlay: (Boolean) -> Unit,
    onSubtitlesClick: () -> Unit,
    onSubtitlesLongClick: () -> Unit,
    onAudioClick: () -> Unit,
    onAudioLongClick: () -> Unit,
    onQualityClick: () -> Unit,
    isEpisodeOnline: Boolean?,
    onMoreClick: () -> Unit,
    onMoreLongClick: () -> Unit,

    // Playback State
    positionSec: Float,
    durationSec: Float,
    readAheadSec: Float,
    onSeekValueChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    paused: Boolean,
    isLoading: Boolean,
    isLoadingEpisode: Boolean,
    showLoadingCircle: Boolean,
    gestureSeekAmount: Pair<Int, Int>?,
    onPlayPauseClick: () -> Unit,
    hasPrevious: Boolean,
    onSkipPrevious: () -> Unit,
    hasNext: Boolean,
    onSkipNext: () -> Unit,

    // Bottom Left actions
    playbackSpeed: Float,
    currentChapter: IndexedSegment?,
    onCycleRotation: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onOpenSheet: (KitsugiSheets) -> Unit,
    chapters: List<IndexedSegment>,

    // Bottom Right actions
    skipIntroButtonText: String?,
    onPressSkipIntroButton: () -> Unit,
    isPipAvailable: Boolean,
    onAspectClick: () -> Unit,
    onPipClick: () -> Unit,

    // Binge Playback / Countdown
    showBingeCard: Boolean,
    bingeCountdownSec: Int,
    onCancelBinge: () -> Unit,
    onPlayNextBinge: () -> Unit,

    // Timers
    timersInverted: Pair<Boolean, Boolean>,
    onPositionTimerClick: () -> Unit,
    onDurationTimerClick: () -> Unit,

    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // Subtle top & bottom shadows for readability
        AnimatedVisibility(
            visible = controlsShown && !areControlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
        }

        // Locked Screen Overlay
        if (areControlsLocked) {
            AnimatedVisibility(
                visible = controlsShown,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(onClick = onUnlockControls)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock Controls",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        } else {
            // Top Bar
            AnimatedVisibility(
                visible = controlsShown,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopLeftPlayerControls(
                        animeTitle = animeTitle,
                        mediaTitle = mediaTitle,
                        onTitleClick = onTitleClick,
                        onBackClick = onBackClick,
                        modifier = Modifier.weight(1f)
                    )
                    TopRightPlayerControls(
                        autoPlayEnabled = autoPlayEnabled,
                        onToggleAutoPlay = onToggleAutoPlay,
                        onSubtitlesClick = onSubtitlesClick,
                        onSubtitlesLongClick = onSubtitlesLongClick,
                        onAudioClick = onAudioClick,
                        onAudioLongClick = onAudioLongClick,
                        onQualityClick = onQualityClick,
                        isEpisodeOnline = isEpisodeOnline,
                        onMoreClick = onMoreClick,
                        onMoreLongClick = onMoreLongClick
                    )
                }
            }

            // Middle Playback Controls
            MiddlePlayerControls(
                hasPrevious = hasPrevious,
                onSkipPrevious = onSkipPrevious,
                isLoading = isLoading,
                isLoadingEpisode = isLoadingEpisode,
                controlsShown = controlsShown,
                areControlsLocked = areControlsLocked,
                showLoadingCircle = showLoadingCircle,
                paused = paused,
                gestureSeekAmount = gestureSeekAmount,
                onPlayPauseClick = onPlayPauseClick,
                hasNext = hasNext,
                onSkipNext = onSkipNext,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            )

            // Bottom Bar Controls
            AnimatedVisibility(
                visible = controlsShown,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val segments: ImmutableList<Segment> = chapters.map { it.toSegment() }.toImmutableList()
                    SeekbarWithTimers(
                        position = positionSec,
                        duration = durationSec,
                        readAheadValue = readAheadSec,
                        onValueChange = onSeekValueChange,
                        onValueChangeFinished = onSeekFinished,
                        timersInverted = timersInverted,
                        positionTimerOnClick = onPositionTimerClick,
                        durationTimerOnCLick = onDurationTimerClick,
                        chapters = segments,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomLeftPlayerControls(
                            playbackSpeed = playbackSpeed,
                            currentChapter = currentChapter,
                            onLockControls = onLockControls,
                            onCycleRotation = onCycleRotation,
                            onPlaybackSpeedChange = onPlaybackSpeedChange,
                            onOpenSheet = onOpenSheet,
                            showChapters = chapters.isNotEmpty()
                        )

                        BottomRightPlayerControls(
                            skipIntroButton = skipIntroButtonText,
                            onPressSkipIntroButton = onPressSkipIntroButton,
                            isPipAvailable = isPipAvailable,
                            onAspectClick = onAspectClick,
                            onPipClick = onPipClick
                        )
                    }
                }
            }
        }

        // Binge Card (Next Episode Countdown) Overlay
        AnimatedVisibility(
            visible = showBingeCard,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 96.dp, end = 24.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E38).copy(alpha = 0.9f),
                    contentColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sonraki Bölüm",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = onCancelBinge,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Countdown",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "Sonraki bölüm ${bingeCountdownSec} saniye içinde başlayacak...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onCancelBinge) {
                            Text("İptal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onPlayNextBinge) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Şimdi Oynat")
                        }
                    }
                }
            }
        }
    }
}
