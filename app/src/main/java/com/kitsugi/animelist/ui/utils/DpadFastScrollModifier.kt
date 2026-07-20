package com.kitsugi.animelist.ui.utils

import android.view.KeyEvent
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Drag velocity for vertical takeover, expressed in dp per second.
 * Tuned to feel like a native TV launcher long-press scroll.
 */
private const val DEFAULT_VERTICAL_VELOCITY_DP_PER_SEC = 3200f

/**
 * Maximum idle gap between key events before the drag self-terminates.
 * Guards against missed ACTION_UP events (e.g. focus shift to system IME).
 */
private const val DEFAULT_END_TIMEOUT_MS = 160L

/**
 * Per-frame delta clamp so a long frame stall (GC, layout thrash) can't
 * teleport the list by hundreds of dp on the next frame (~3 frames at 60 Hz).
 */
private const val DEFAULT_MAX_FRAME_DT_SEC = 0.048f

private enum class FastScrollMode { None, Vertical }

/**
 * Adds frame-driven fast-scroll behaviour to any scrollable container.
 * Ported from KitsugiTV-dev DpadFastScrollModifier.
 *
 * Behaviour:
 * - DPAD_UP / DPAD_DOWN **repeats** take over [scrollableState] with a
 *   coroutine that drags the list at constant velocity while focus stays
 *   frozen on the originating card. On ACTION_UP (or list edge or idle
 *   timeout), the drag ends and [resolveVerticalLanding] is called.
 * - DPAD_LEFT / DPAD_RIGHT repeats tear down any in-flight vertical drag
 *   then fall through to the chained [dpadRepeatThrottle].
 * - First-press (non-repeat) D-pad events always fall through so Compose's
 *   default focus navigation handles single-step movement.
 */
fun Modifier.dpadVerticalFastScroll(
    scrollableState: ScrollableState,
    resolveVerticalLanding: (sign: Int) -> Unit = {},
    onFastScrollingChanged: (Boolean) -> Unit = {},
    shouldHaltForward: () -> Boolean = { false },
    horizontalGateMs: Long = 80L,
    verticalVelocityDpPerSec: Float = DEFAULT_VERTICAL_VELOCITY_DP_PER_SEC,
    endTimeoutMs: Long = DEFAULT_END_TIMEOUT_MS,
    maxFrameDtSec: Float = DEFAULT_MAX_FRAME_DT_SEC,
): Modifier = composed {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val jobRef      = remember { AtomicReference<Job?>(null) }
    val endTimerRef = remember { AtomicReference<Job?>(null) }
    val modeRef     = remember { AtomicReference(FastScrollMode.None) }
    val directionRef = remember { AtomicInteger(0) }
    val isActiveRef = remember { AtomicReference(false) }

    DisposableEffect(Unit) {
        onDispose {
            jobRef.getAndSet(null)?.cancel()
            endTimerRef.getAndSet(null)?.cancel()
        }
    }

    onPreviewKeyEvent { event ->
        val native = event.nativeKeyEvent
        val kc = native.keyCode
        val isHoriz = kc == KeyEvent.KEYCODE_DPAD_LEFT || kc == KeyEvent.KEYCODE_DPAD_RIGHT
        val isVert  = kc == KeyEvent.KEYCODE_DPAD_UP   || kc == KeyEvent.KEYCODE_DPAD_DOWN

        fun setActive(value: Boolean) {
            if (isActiveRef.getAndSet(value) != value) onFastScrollingChanged(value)
        }

        fun endFastScroll() {
            val mode = modeRef.getAndSet(FastScrollMode.None)
            val direction = directionRef.getAndSet(0)
            jobRef.getAndSet(null)?.cancel()
            endTimerRef.getAndSet(null)?.cancel()
            setActive(false)
            if (mode == FastScrollMode.Vertical) {
                resolveVerticalLanding(if (direction == 0) 1 else direction)
            }
        }

        if (!isHoriz && !isVert) return@onPreviewKeyEvent false

        if (native.action == KeyEvent.ACTION_UP) {
            if (modeRef.get() != FastScrollMode.None) endFastScroll()
            return@onPreviewKeyEvent false
        }

        if (native.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false

        // First press (not a repeat) — let Compose move focus by one cell.
        if (native.repeatCount == 0) return@onPreviewKeyEvent false

        if (isHoriz) {
            if (modeRef.get() != FastScrollMode.None) endFastScroll()
            return@onPreviewKeyEvent false
        }

        // Vertical repeat — enter or extend fast-scroll drag mode.
        val sign = if (kc == KeyEvent.KEYCODE_DPAD_UP) -1 else 1
        val needsStart = modeRef.get() != FastScrollMode.Vertical ||
            directionRef.get() != sign ||
            jobRef.get()?.isActive != true

        if (needsStart) {
            jobRef.getAndSet(null)?.cancel()

            val atScrollEdge = (sign > 0 && !scrollableState.canScrollForward) ||
                (sign < 0 && !scrollableState.canScrollBackward)
            val halted = sign > 0 && shouldHaltForward()
            if (atScrollEdge || halted) {
                if (modeRef.get() != FastScrollMode.None) endFastScroll()
                return@onPreviewKeyEvent true
            }

            modeRef.set(FastScrollMode.Vertical)
            directionRef.set(sign)
            setActive(true)

            val velocityPxPerSec = with(density) { verticalVelocityDpPerSec.dp.toPx() }

            jobRef.set(
                scope.launch {
                    try {
                        scrollableState.scroll {
                            var lastFrame = withFrameNanos { it }
                            while (true) {
                                val now = withFrameNanos { it }
                                val dtSec = ((now - lastFrame) / 1_000_000_000f)
                                    .coerceAtMost(maxFrameDtSec)
                                lastFrame = now

                                if (sign > 0 && shouldHaltForward()) break

                                val delta = sign * velocityPxPerSec * dtSec
                                val consumed = scrollBy(delta)
                                if (consumed == 0f && delta != 0f) break
                            }
                        }
                        endFastScroll()
                    } catch (_: CancellationException) {
                        // expected on release / axis change
                    }
                }
            )
        }

        // Re-arm safety timer against missed ACTION_UP.
        endTimerRef.getAndSet(null)?.cancel()
        endTimerRef.set(
            scope.launch {
                delay(endTimeoutMs)
                endFastScroll()
            }
        )

        true
    }.dpadRepeatThrottle(
        horizontalGateMs = horizontalGateMs,
        // Vertical repeats are fully consumed above; Long.MAX_VALUE keeps
        // the throttle as a pure horizontal gate.
        verticalGateMs = Long.MAX_VALUE,
    )
}
