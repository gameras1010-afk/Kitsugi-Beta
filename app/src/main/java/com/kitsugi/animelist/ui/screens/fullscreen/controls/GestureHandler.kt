package com.kitsugi.animelist.ui.screens.fullscreen.controls

// ─────────────────────────────────────────────────────────────────────────────
// GestureHandler — Aniyomi-derived, Kitsugi-adapted
// Original: eu.kanade.tachiyomi.ui.player.controls.GestureHandler
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiPlayerViewModel
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiSheets
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiDialogs
import com.kitsugi.animelist.ui.screens.fullscreen.components.LeftSideOvalShape
import com.kitsugi.animelist.ui.screens.fullscreen.components.RightSideOvalShape
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.DoubleTapSeekTriangles
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// GestureHandler
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GestureHandler(
    viewModel: KitsugiPlayerViewModel,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val settings         by viewModel.appSettings.collectAsState()
    val controlsShown    by viewModel.controlsShown.collectAsState()
    val areControlsLocked by viewModel.areControlsLocked.collectAsState()
    val sheetShown       by viewModel.sheetShown.collectAsState()
    val panelShown       by viewModel.panelShown.collectAsState()
    val dialogShown      by viewModel.dialogShown.collectAsState()
    val seekAmount       by viewModel.doubleTapSeekAmount.collectAsState()
    val isSeekingForwards by viewModel.isSeekingForwards.collectAsState()
    val posMs            by viewModel.pos.collectAsState()
    val durationMs       by viewModel.duration.collectAsState()

    val isSheetOrPanelOrDialogShown = sheetShown != KitsugiSheets.None ||
            panelShown != com.kitsugi.animelist.ui.screens.fullscreen.KitsugiPanels.None ||
            dialogShown != KitsugiDialogs.None

    // Convert ms → seconds for gesture calculations (matching Aniyomi's Float-based API)
    val positionSec = posMs / 1000f
    val durationSec = durationMs / 1000f

    var isDoubleTapSeeking by remember { mutableStateOf(false) }

    // Hide the double-tap seek indicator after 800 ms (Aniyomi pattern)
    LaunchedEffect(seekAmount) {
        delay(800)
        isDoubleTapSeeking = false
        viewModel.updateSeekAmount(0)
        viewModel.updateSeekText(null)
        delay(100)
        viewModel.hideSeekBar()
    }

    val swapVolumeBrightness  = settings.swipeVolumeBrightnessSides
    val seekGestureEnabled    = settings.horizontalSeekGestureEnabled
    val gestureBrightness     = settings.gestureBrightnessEnabled
    val gestureVolume         = settings.gestureVolumeEnabled
    val gestureVolumeBrightness = gestureBrightness || gestureVolume
    val preciseSeeking        = settings.preciseSeeking
    val volumeBoostingCap     = settings.volumeBoostCap - 100 // extra headroom above 100%

    val currentVolume    by viewModel.currentVolume.collectAsState()
    val currentMPVVolume by viewModel.currentMPVVolume.collectAsState()
    val currentBrightness by viewModel.currentBrightness.collectAsState()

    var isLongPressing by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            // ── Tap / Double-tap / Long-press ────────────────────────────────
            .pointerInput(areControlsLocked, isSheetOrPanelOrDialogShown) {
                detectTapGestures(
                    onTap = {
                        if (isSheetOrPanelOrDialogShown) {
                            viewModel.showSheet(KitsugiSheets.None)
                            viewModel.showPanel(com.kitsugi.animelist.ui.screens.fullscreen.KitsugiPanels.None)
                            viewModel.showDialog(KitsugiDialogs.None)
                        } else {
                            if (controlsShown) viewModel.hideControls() else viewModel.showControls()
                        }
                    },
                    onDoubleTap = {
                        if (areControlsLocked || isDoubleTapSeeking || isSheetOrPanelOrDialogShown) return@detectTapGestures
                        if (it.x > size.width * 3 / 5) {
                            if (!isSeekingForwards) viewModel.updateSeekAmount(0)
                            viewModel.handleRightDoubleTap()
                            isDoubleTapSeeking = true
                        } else if (it.x < size.width * 2 / 5) {
                            if (isSeekingForwards) viewModel.updateSeekAmount(0)
                            viewModel.handleLeftDoubleTap()
                            isDoubleTapSeeking = true
                        } else {
                            viewModel.handleCenterDoubleTap()
                        }
                    },
                    onPress = {
                        if (isSheetOrPanelOrDialogShown) return@detectTapGestures
                        if (!areControlsLocked && isDoubleTapSeeking && seekAmount != 0) {
                            val press = PressInteraction.Press(
                                it.copy(x = if (it.x > size.width * 3 / 5) it.x - size.width * 0.6f else it.x),
                            )
                            if (it.x > size.width * 3 / 5) {
                                if (!isSeekingForwards) viewModel.updateSeekAmount(0)
                                viewModel.handleRightDoubleTap()
                            } else if (it.x < size.width * 2 / 5) {
                                if (isSeekingForwards) viewModel.updateSeekAmount(0)
                                viewModel.handleLeftDoubleTap()
                            } else {
                                viewModel.handleCenterDoubleTap()
                            }
                            interactionSource.emit(press)
                            tryAwaitRelease()
                            if (isLongPressing) {
                                isLongPressing = false
                                viewModel.setPlaybackSpeed(1f)
                            }
                            interactionSource.emit(PressInteraction.Release(press))
                        } else {
                            isDoubleTapSeeking = false
                        }
                    },
                    onLongPress = {
                        if (areControlsLocked || isSheetOrPanelOrDialogShown) return@detectTapGestures
                        if (!isLongPressing) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isLongPressing = true
                            val holdSpeed = settings.holdSpeedMultiplier
                            viewModel.setPlaybackSpeed(holdSpeed)
                        }
                    },
                )
            }
            // ── Horizontal seek ──────────────────────────────────────────────
            .pointerInput(areControlsLocked, isSheetOrPanelOrDialogShown) {
                if (!seekGestureEnabled || areControlsLocked || isSheetOrPanelOrDialogShown) return@pointerInput
                var startingPositionSec = 0f
                var startingX = 0f
                var wasPlayerAlreadyPaused = false
                detectHorizontalDragGestures(
                    onDragStart = {
                        startingPositionSec = viewModel.pos.value / 1000f
                        startingX = it.x
                        wasPlayerAlreadyPaused = viewModel.paused.value
                        viewModel.pause()
                    },
                    onDragEnd = {
                        viewModel.gestureSeekAmount.value = null
                        viewModel.hideSeekBar()
                        if (!wasPlayerAlreadyPaused) viewModel.play()
                    },
                ) { change, _ ->
                    val currentPosSec = viewModel.pos.value / 1000f
                    val durSec = viewModel.duration.value / 1000f
                    if (currentPosSec <= 0f && change.position.x < startingX) return@detectHorizontalDragGestures
                    if (currentPosSec >= durSec && change.position.x > startingX) return@detectHorizontalDragGestures

                    val newPosSec = calculateNewHorizontalGestureValue(
                        startingPositionSec, startingX, change.position.x, 0.15f
                    ).coerceIn(0f, durSec)

                    viewModel.gestureSeekAmount.value = Pair(
                        startingPositionSec.toInt(),
                        (newPosSec - startingPositionSec).toInt()
                            .coerceIn(-startingPositionSec.toInt(), (durSec - startingPositionSec).toInt())
                    )
                    viewModel.seekTo((newPosSec * 1000f).toLong())
                    viewModel.showSeekBar()
                }
            }
            // ── Vertical volume / brightness ─────────────────────────────────
            .pointerInput(areControlsLocked, isSheetOrPanelOrDialogShown) {
                if (!gestureVolumeBrightness || areControlsLocked || isSheetOrPanelOrDialogShown) return@pointerInput
                var startingY = 0f
                var mpvVolumeStartingY = 0f
                var originalVolume = currentVolume
                var originalMPVVolume = currentMPVVolume
                var originalBrightness = currentBrightness
                val brightnessGestureSens = 0.001f
                val volumeGestureSens = 0.001f * viewModel.maxVolume
                val mpvVolumeGestureSens = if (volumeBoostingCap > 0) 0.001f * volumeBoostingCap else 0f

                val isIncreasingVolumeBoost: (Float) -> Boolean = {
                    volumeBoostingCap > 0 &&
                        currentVolume == viewModel.maxVolume &&
                        currentMPVVolume - 100 < volumeBoostingCap &&
                        it < 0
                }
                val isDecreasingVolumeBoost: (Float) -> Boolean = {
                    volumeBoostingCap > 0 &&
                        currentVolume == viewModel.maxVolume &&
                        currentMPVVolume - 100 in 1..volumeBoostingCap &&
                        it > 0
                }

                detectVerticalDragGestures(
                    onDragEnd = { startingY = 0f },
                    onDragStart = {
                        startingY = 0f
                        mpvVolumeStartingY = 0f
                        originalVolume = currentVolume
                        originalMPVVolume = currentMPVVolume
                        originalBrightness = currentBrightness
                    },
                ) { change, amount ->
                    val changeVolume: () -> Unit = {
                        if (gestureVolume) {
                            if (isIncreasingVolumeBoost(amount) || isDecreasingVolumeBoost(amount)) {
                                if (mpvVolumeStartingY == 0f) {
                                    startingY = 0f
                                    originalVolume = currentVolume
                                    mpvVolumeStartingY = change.position.y
                                }
                                viewModel.changeMPVVolumeTo(
                                    calculateNewVerticalGestureValue(
                                        originalMPVVolume,
                                        mpvVolumeStartingY,
                                        change.position.y,
                                        mpvVolumeGestureSens,
                                    ).coerceIn(100, 100 + volumeBoostingCap)
                                )
                            } else {
                                if (startingY == 0f) {
                                    mpvVolumeStartingY = 0f
                                    originalMPVVolume = currentMPVVolume
                                    startingY = change.position.y
                                }
                                viewModel.changeVolumeTo(
                                    calculateNewVerticalGestureValue(
                                        originalVolume,
                                        startingY,
                                        change.position.y,
                                        volumeGestureSens,
                                    )
                                )
                            }
                            viewModel.displayVolumeSlider()
                        }
                    }
                    val changeBrightness: () -> Unit = {
                        if (gestureBrightness) {
                            if (startingY == 0f) startingY = change.position.y
                            viewModel.changeBrightnessTo(
                                calculateNewVerticalGestureValue(
                                    originalBrightness,
                                    startingY,
                                    change.position.y,
                                    brightnessGestureSens,
                                )
                            )
                            viewModel.displayBrightnessSlider()
                        }
                    }

                    if (swapVolumeBrightness) {
                        if (change.position.x > size.width / 2) changeBrightness() else changeVolume()
                    } else {
                        if (change.position.x < size.width / 2) changeBrightness() else changeVolume()
                    }
                }
            },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// DoubleTapToSeekOvals
// Matches Aniyomi's DoubleTapToSeekOvals composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DoubleTapToSeekOvals(
    amount: Int,
    text: String?,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(
        targetValue = if (amount == 0) 0f else 0.2f,
        label = "double_tap_animation_alpha"
    )
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (amount > 0) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        if (amount != 0 || text != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.4f), // 2/5 of screen width
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(if (amount > 0) RightSideOvalShape else LeftSideOvalShape)
                        .background(Color.White.copy(alpha))
                        .indication(interactionSource, ripple()),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DoubleTapSeekTriangles(isForward = amount > 0)
                    Text(
                        text      = text ?: "${kotlin.math.abs(amount)}s",
                        fontSize  = 12.sp,
                        textAlign = TextAlign.Center,
                        color     = Color.White,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gesture Math Helpers — identical to Aniyomi's
// ─────────────────────────────────────────────────────────────────────────────

fun calculateNewVerticalGestureValue(originalValue: Int, startingY: Float, newY: Float, sensitivity: Float): Int =
    originalValue + ((startingY - newY) * sensitivity).toInt()

fun calculateNewVerticalGestureValue(originalValue: Float, startingY: Float, newY: Float, sensitivity: Float): Float =
    originalValue + ((startingY - newY) * sensitivity)

fun calculateNewHorizontalGestureValue(originalValue: Int, startingX: Float, newX: Float, sensitivity: Float): Int =
    originalValue + ((newX - startingX) * sensitivity).toInt()

fun calculateNewHorizontalGestureValue(originalValue: Float, startingX: Float, newX: Float, sensitivity: Float): Float =
    originalValue + ((newX - startingX) * sensitivity)
