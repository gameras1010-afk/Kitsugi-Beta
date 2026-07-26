package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * GestureHandler — Tüm pointer gesture mantığını barındıran composable.
 *
 * Bu bileşen:
 * - Dikey swipe → ses / parlaklık
 * - Yatay swipe → seek
 * - Double tap → ileri / geri sar
 * - Long press → hız artırma
 * - Pinch → aspect ratio
 *
 * @param config           Gesture ayarları (PlayerGestureConfig)
 * @param gestureController Hazır PlayerGestureController (ses/parlaklık kontrol eder)
 * @param onSeek           Seek delta (ms) callback
 * @param onToggleControls Tek tıklamada kontrolları aç/kapat
 * @param onDoubleTapLeft  Sol double tap
 * @param onDoubleTapRight Sağ double tap
 * @param onZoom           Pinch-to-zoom oranı
 * @param content          Oynatıcı Surface veya başka içerik
 */
@Composable
fun GestureHandler(
    config: PlayerGestureConfig,
    gestureController: PlayerGestureController,
    onSeek: (deltaMs: Long) -> Unit,
    onToggleControls: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onZoom: ((scale: Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Track gesture type lock to avoid mixing vertical/horizontal drags
    var gestureLock by remember { mutableStateOf(GestureLock.NONE) }
    var gestureStartX by remember { mutableStateOf(0f) }
    var gestureStartY by remember { mutableStateOf(0f) }

    // Long press tracking
    var longPressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapX by remember { mutableStateOf(0f) }
    var screenWidth by remember { mutableStateOf(1) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (onZoom != null) {
                    Modifier.pointerInput(config) {
                        detectTransformGestures { _, _, zoom, _ ->
                            onZoom(zoom)
                        }
                    }
                } else Modifier
            )
            .pointerInput(config) {
                screenWidth = size.width.coerceAtLeast(1)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downX = down.position.x
                    val downY = down.position.y
                    gestureStartX = downX
                    gestureStartY = downY
                    gestureLock = GestureLock.NONE

                    // Long-press detection — start job on finger down
                    longPressJob = scope.launch {
                        delay(600L)
                        gestureController.startHoldSpeed()
                    }

                    var totalDx = 0f
                    var totalDy = 0f
                    val isLeftSide = downX < screenWidth / 2f

                    // Consume move events
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        if (change.isConsumed) {
                            longPressJob?.cancel(); longPressJob = null
                            gestureController.stopHoldSpeed()
                            break
                        }

                        val dx = change.position.x - change.previousPosition.x
                        val dy = change.position.y - change.previousPosition.y
                        totalDx += dx
                        totalDy += dy

                        // Lock gesture direction after threshold
                        if (gestureLock == GestureLock.NONE && (abs(totalDx) > 10f || abs(totalDy) > 10f)) {
                            gestureLock = if (abs(totalDy) > abs(totalDx)) GestureLock.VERTICAL else GestureLock.HORIZONTAL
                            longPressJob?.cancel(); longPressJob = null
                        }

                        when (gestureLock) {
                            GestureLock.VERTICAL -> {
                                val normalizedDelta = -dy / size.height.toFloat() * config.scrollSensitivity
                                val volumeSide = config.swipeVolumeBrightnessSides
                                when {
                                    config.volumeGestureEnabled && ((volumeSide && !isLeftSide) || (!volumeSide && isLeftSide)) -> {
                                        gestureController.adjustVolume(normalizedDelta)
                                        change.consume()
                                    }
                                    config.brightnessGestureEnabled && ((volumeSide && isLeftSide) || (!volumeSide && !isLeftSide)) -> {
                                        gestureController.adjustBrightness(context, normalizedDelta)
                                        change.consume()
                                    }
                                }
                            }
                            GestureLock.HORIZONTAL -> {
                                if (config.horizontalSeekGestureEnabled) {
                                    // 1px = 300ms seek delta (scaled by screen width)
                                    val seekDeltaMs = (dx / screenWidth * 60_000L).toLong()
                                    onSeek(seekDeltaMs)
                                    change.consume()
                                }
                            }
                            GestureLock.NONE -> { /* waiting for lock */ }
                        }

                    } while (event.changes.any { it.pressed })

                    // Release — stop long press / speed
                    longPressJob?.cancel(); longPressJob = null
                    gestureController.stopHoldSpeed()

                    // Tap detection (only if no significant gesture happened)
                    if (gestureLock == GestureLock.NONE) {
                        val now = System.currentTimeMillis()
                        val isDoubleTap = now - lastTapTime < 300L && abs(downX - lastTapX) < 80f
                        if (isDoubleTap) {
                            val isLeft = downX < screenWidth / 2f
                            if (isLeft) onDoubleTapLeft() else onDoubleTapRight()
                            lastTapTime = 0L
                        } else {
                            lastTapTime = now
                            lastTapX = downX
                            scope.launch {
                                delay(310L)
                                if (lastTapTime == now) {
                                    onToggleControls()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        content()
    }
}

/**
 * Gesture yönü kilidi — NONE: belirsiz, VERTICAL: dikey, HORIZONTAL: yatay
 */
private enum class GestureLock { NONE, VERTICAL, HORIZONTAL }
