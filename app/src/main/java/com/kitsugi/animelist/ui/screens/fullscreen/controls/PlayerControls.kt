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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.BrightnessSlider
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.VolumeSlider
import com.kitsugi.animelist.core.player.PlaybackState
import com.kitsugi.animelist.core.player.PlayerPipHelper
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiDialogs
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiPlayerViewModel
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiSheets
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.IndexedSegment
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.SeekbarWithTimers
import androidx.compose.ui.platform.LocalContext
import dev.vivvvek.seeker.Segment
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay

// How long (ms) controls stay visible when playing
private const val CONTROLS_HIDE_DELAY_MS = 5_000L

/**
 * Top-level OSD controls composable.
 *
 * Binds directly to [KitsugiPlayerViewModel] — no prop-drilling.
 * The hide-timer lives here via LaunchedEffect (Aniyomi pattern).
 *
 * @param onBackClick   Navigate back / exit player.
 * @param onRotateClick Cycle screen orientation.
 */
@Composable
fun PlayerControls(
    viewModel: KitsugiPlayerViewModel,
    onBackClick: () -> Unit,
    onRotateClick: () -> Unit,
    onAspectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ── Collect all state from ViewModel ──────────────────────────────────────
    val controlsShown     by viewModel.controlsShown.collectAsState()
    val areControlsLocked by viewModel.areControlsLocked.collectAsState()
    val sheetShown        by viewModel.sheetShown.collectAsState()
    val panelShown        by viewModel.panelShown.collectAsState()
    val dialogShown       by viewModel.dialogShown.collectAsState()

    val isAnyMenuShown = sheetShown != KitsugiSheets.None ||
            panelShown != com.kitsugi.animelist.ui.screens.fullscreen.KitsugiPanels.None ||
            dialogShown != KitsugiDialogs.None

    val areControlsVisible = controlsShown && !isAnyMenuShown
    val paused            by viewModel.paused.collectAsState()
    val isLoading         by viewModel.isLoading.collectAsState()
    val isLoadingEpisode  by viewModel.nextEpisodeLoading.collectAsState()
    val posMs             by viewModel.pos.collectAsState()
    val durationMs        by viewModel.duration.collectAsState()
    val hasPrevious       by viewModel.hasPreviousEpisode.collectAsState()
    val hasNext           by viewModel.hasNextEpisode.collectAsState()
    val chapters          by viewModel.chapters.collectAsState()
    val currentChapter    by viewModel.currentChapter.collectAsState()
    val animeTitle        by viewModel.animeTitleFlow.collectAsState()
    val currentTitle      by viewModel.currentTitle.collectAsState()
    val settings          by viewModel.appSettings.collectAsState()
    val skipIntervals     by viewModel.skipIntervals.collectAsState()
    val bingeCountdown    by viewModel.bingeCountdownSec.collectAsState()
    val showBingeCard     by viewModel.showBingeCardState.collectAsState()
    val playerState       by viewModel.playerState.collectAsState()
    val controlsResetTrigger by viewModel.controlsResetTrigger.collectAsState()
    val customButton      by viewModel.primaryButton.collectAsState()
    val customButtonTitle by viewModel.primaryButtonTitle.collectAsState()
    val playbackSpeed     by viewModel.playbackSpeed.collectAsState()
    val context           = LocalContext.current

    val showLoadingCircle = isLoading || isLoadingEpisode ||
            playerState is PlaybackState.Buffering

    // Formatted title respecting settings
    val mediaTitle = remember(currentTitle, settings.showPlayerTitle, settings.titleLimitType) {
        if (!settings.showPlayerTitle) {
            ""
        } else {
            when (settings.titleLimitType) {
                "LIMIT_20" -> if (currentTitle.length > 20) currentTitle.take(20) + "…" else currentTitle
                "LIMIT_40" -> if (currentTitle.length > 40) currentTitle.take(40) + "…" else currentTitle
                else       -> currentTitle
            }
        }
    }

    // Skip-intro button text (AniSkip aralığı varsa dinamik, yoksa null)
    val currentPosSec = posMs / 1000f
    val skipIntroText = remember(skipIntervals, currentPosSec) {
        skipIntervals.firstOrNull { interval ->
            currentPosSec >= interval.startTime && currentPosSec < interval.endTime
        }?.let { interval ->
            when (interval.type) {
                "intro" -> "Giriş'i Atla"
                "outro" -> "Bitişi Atla"
                "recap" -> "Özeti Atla"
                else    -> "Atla"
            }
        }
    }

    // Sabit skip-intro süresi (SharedPrefs'ten, varsayılan 85sn)
    val introSkipLength = remember { viewModel.getAnimeSkipIntroLength() }

    // Chapters as Segment list for seekbar
    val seekSegments = remember(chapters) {
        chapters.map { it.toSegment() }.toImmutableList()
    }

    // ── Hide-controls timer (Aniyomi LaunchedEffect pattern) ─────────────────
    var resetTimerTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(controlsShown, paused, resetTimerTrigger, controlsResetTrigger) {
        if (controlsShown && !paused) {
            delay(CONTROLS_HIDE_DELAY_MS)
            viewModel.hideControls()
        }
    }

    val isBrightnessSliderShown by viewModel.isBrightnessSliderShown.collectAsState()
    val isVolumeSliderShown by viewModel.isVolumeSliderShown.collectAsState()
    val brightness by viewModel.currentBrightness.collectAsState()
    val volume by viewModel.currentVolume.collectAsState()
    val mpvVolume by viewModel.currentMPVVolume.collectAsState()

    LaunchedEffect(volume, mpvVolume, isVolumeSliderShown) {
        if (isVolumeSliderShown) {
            delay(2000)
            viewModel.isVolumeSliderShown.value = false
        }
    }
    LaunchedEffect(brightness, isBrightnessSliderShown) {
        var ctx = context
        var activity: android.app.Activity? = null
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) {
                activity = ctx
                break
            }
            ctx = ctx.baseContext
        }
        if (activity != null) {
            val params = activity.window.attributes
            params.screenBrightness = brightness.coerceIn(0.01f, 1f)
            activity.window.attributes = params
        }
        if (isBrightnessSliderShown) {
            delay(2000)
            viewModel.isBrightnessSliderShown.value = false
        }
    }

    fun resetTimer() { resetTimerTrigger++ }

    val interactionSource = remember { MutableInteractionSource() }
    val seekAmount        by viewModel.doubleTapSeekAmount.collectAsState()
    val seekText          by viewModel.seekText.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {

        // ── Gesture layer (fullscreen, behind all controls) ───────────────────
        GestureHandler(
            viewModel         = viewModel,
            interactionSource = interactionSource,
            modifier          = Modifier.fillMaxSize()
        )

        // ── Double-tap seek ovals ─────────────────────────────────────────────
        DoubleTapToSeekOvals(
            amount            = seekAmount,
            text              = seekText,
            interactionSource = interactionSource,
            modifier          = Modifier.fillMaxSize()
        )

        // ── Brightness slider overlay ─────────────────────────────────────────
        val swapVolumeAndBrightness = settings.swipeVolumeBrightnessSides
        AnimatedVisibility(
            visible = isBrightnessSliderShown,
            enter = slideInHorizontally(androidx.compose.animation.core.tween(100)) { if (swapVolumeAndBrightness) -it else it } + fadeIn(),
            exit = slideOutHorizontally(androidx.compose.animation.core.tween(300)) { if (swapVolumeAndBrightness) -it else it } + fadeOut(),
            modifier = Modifier
                .align(if (swapVolumeAndBrightness) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 24.dp)
        ) {
            BrightnessSlider(
                brightness = brightness,
                positiveRange = 0f..1f,
                negativeRange = 0f..0.75f,
            )
        }

        // ── Volume slider overlay ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = isVolumeSliderShown,
            enter = slideInHorizontally(androidx.compose.animation.core.tween(100)) { if (swapVolumeAndBrightness) it else -it } + fadeIn(),
            exit = slideOutHorizontally(androidx.compose.animation.core.tween(300)) { if (swapVolumeAndBrightness) it else -it } + fadeOut(),
            modifier = Modifier
                .align(if (swapVolumeAndBrightness) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 24.dp)
        ) {
            VolumeSlider(
                volume = volume,
                mpvVolume = mpvVolume,
                range = 0..viewModel.maxVolume,
                boostRange = if (settings.volumeBoostCap - 100 > 0) 0..(settings.volumeBoostCap - 100) else null,
                displayAsPercentage = false,
            )
        }

        // ── Gradient scrim ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = areControlsVisible && !areControlsLocked,
            enter   = fadeIn(),
            exit    = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )
        }

        if (areControlsLocked) {
            // ── Lock icon (tap to unlock) ─────────────────────────────────────
            AnimatedVisibility(
                visible  = areControlsVisible,
                enter    = fadeIn(),
                exit     = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable {
                            viewModel.unlockControls()
                            resetTimer()
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Lock,
                        contentDescription = "Kilidi Aç",
                        tint               = Color.White,
                        modifier           = Modifier.size(36.dp)
                    )
                }
            }
        } else {
            // ── Top bar ───────────────────────────────────────────────────────
            AnimatedVisibility(
                visible  = areControlsVisible,
                enter    = slideInVertically { -it } + fadeIn(),
                exit     = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment   = Alignment.CenterVertically
                ) {
                    TopLeftPlayerControls(
                        animeTitle   = animeTitle,
                        mediaTitle   = mediaTitle,
                        onTitleClick = {
                            viewModel.showDialog(KitsugiDialogs.EpisodeList)
                            resetTimer()
                        },
                        onBackClick  = onBackClick,
                        modifier     = Modifier.weight(1f)
                    )
                    TopRightPlayerControls(
                        autoPlayEnabled      = settings.isAutoplayEnabled,
                        onToggleAutoPlay     = { viewModel.setAutoPlay(it) },
                        onSubtitlesClick     = {
                            viewModel.showSheet(KitsugiSheets.SubtitleTracks)
                            resetTimer()
                        },
                        onSubtitlesLongClick = {
                            viewModel.showPanel(com.kitsugi.animelist.ui.screens.fullscreen.KitsugiPanels.SubtitleSettings)
                            resetTimer()
                        },
                        onAudioClick         = {
                            viewModel.showSheet(KitsugiSheets.AudioTracks)
                            resetTimer()
                        },
                        onAudioLongClick     = {
                            viewModel.showPanel(com.kitsugi.animelist.ui.screens.fullscreen.KitsugiPanels.AudioDelay)
                            resetTimer()
                        },
                        onQualityClick       = {
                            viewModel.showSheet(KitsugiSheets.QualityTracks)
                            resetTimer()
                        },
                        isEpisodeOnline      = currentTitle.isNotBlank(),
                        onMoreClick          = {
                            viewModel.showSheet(KitsugiSheets.More)
                            resetTimer()
                        },
                        onMoreLongClick      = {
                            viewModel.showPanel(com.kitsugi.animelist.ui.screens.fullscreen.KitsugiPanels.VideoFilters)
                            resetTimer()
                        },
                    )
                }
            }

            // ── Middle playback controls ──────────────────────────────────────
            MiddlePlayerControls(
                hasPrevious       = hasPrevious,
                onSkipPrevious    = {
                    viewModel.playNextEpisode(
                        activity              = null,
                        onAlternativeRequired = {},
                        onResolutionFailed    = {}
                    )
                    resetTimer()
                },
                isLoading         = isLoading,
                isLoadingEpisode  = isLoadingEpisode,
                controlsShown     = areControlsVisible,
                areControlsLocked = areControlsLocked,
                showLoadingCircle = showLoadingCircle,
                paused            = paused,
                gestureSeekAmount = null,
                onPlayPauseClick  = {
                    viewModel.togglePlay()
                    resetTimer()
                },
                hasNext           = hasNext,
                onSkipNext        = {
                    viewModel.playNextEpisode(
                        activity              = null,
                        onAlternativeRequired = {},
                        onResolutionFailed    = {}
                    )
                    resetTimer()
                },
                enter             = fadeIn(),
                exit              = fadeOut(),
                modifier          = Modifier.align(Alignment.Center)
            )

            // ── Bottom bar ────────────────────────────────────────────────────
            AnimatedVisibility(
                visible  = areControlsVisible,
                enter    = slideInVertically { it } + fadeIn(),
                exit     = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SeekbarWithTimers(
                        position             = posMs / 1000f,
                        duration             = (durationMs / 1000f).coerceAtLeast(0f),
                        readAheadValue       = viewModel.readAhead.collectAsState().value,
                        onValueChange        = { viewModel.seekTo((it * 1000f).toLong()) },
                        onValueChangeFinished = { resetTimer() },
                        timersInverted       = Pair(false, false),
                        positionTimerOnClick  = {},
                        durationTimerOnCLick  = {},
                        chapters             = seekSegments,
                        modifier             = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        BottomLeftPlayerControls(
                            playbackSpeed       = playbackSpeed,
                            currentChapter      = currentChapter,
                            onLockControls      = {
                                viewModel.lockControls()
                                viewModel.showControls()
                            },
                            onCycleRotation     = {
                                onRotateClick()
                                resetTimer()
                            },
                            onPlaybackSpeedChange = { speed ->
                                viewModel.setPlaybackSpeed(speed)
                                resetTimer()
                            },
                            onOpenSheet         = { sheet ->
                                viewModel.showSheet(sheet)
                                resetTimer()
                            },
                            showChapters        = chapters.isNotEmpty()
                        )
                        BottomRightPlayerControls(
                            customButton         = customButton,
                            customButtonTitle    = customButtonTitle,
                            onPressCustomButton  = {
                                customButton?.let { viewModel.executeCustomButton(it) }
                                resetTimer()
                            },
                            onLongPressCustomButton = {
                                customButton?.let { viewModel.executeCustomButtonLongPress(it) }
                                resetTimer()
                            },
                            skipIntroButton      = skipIntroText,
                            onPressSkipIntroButton = {
                                skipIntervals.firstOrNull { interval ->
                                    currentPosSec >= interval.startTime && currentPosSec < interval.endTime
                                }?.let { interval ->
                                    viewModel.seekTo((interval.endTime * 1000f).toLong())
                                }
                                resetTimer()
                            },
                            fixedSkipSec         = introSkipLength,
                            onPressFixedSkip     = {
                                viewModel.seekTo(posMs + introSkipLength * 1000L)
                                resetTimer()
                            },
                            isPipAvailable       = settings.pipEnabled,
                            onAspectClick        = {
                                onAspectClick()
                                resetTimer()
                            },
                            onPipClick           = {
                                val activity = (context as? android.app.Activity)
                                if (activity != null) {
                                    PlayerPipHelper.enterPipSafe(
                                        activity     = activity,
                                        playerEngine = null,
                                        isPlaying    = !paused,
                                        hasNext      = hasNext
                                    )
                                }
                                resetTimer()
                            }
                        )
                    }
                }
            }
        }

        // ── Binge-card overlay ────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = showBingeCard,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 96.dp, end = 24.dp)
        ) {
            Card(
                shape  = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E38).copy(alpha = 0.9f),
                    contentColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier  = Modifier.width(280.dp)
            ) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = "Sonraki Bölüm",
                            fontWeight = FontWeight.Bold,
                            style      = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick  = { viewModel.userCancelledBinge = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Close,
                                contentDescription = "İptal",
                                modifier           = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text  = "Sonraki bölüm $bingeCountdown saniye içinde başlayacak…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.userCancelledBinge = true }) {
                            Text("İptal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            viewModel.playNextEpisode(
                                activity              = null,
                                onAlternativeRequired = {},
                                onResolutionFailed    = {}
                            )
                        }) {
                            Icon(
                                imageVector        = Icons.Default.SkipNext,
                                contentDescription = null,
                                modifier           = Modifier.size(16.dp)
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

/** Convert [IndexedSegment] to seekbar [Segment]. */
private fun IndexedSegment.toSegment(): Segment = Segment(
    name  = name,
    start = start,
    color = color
)
