package com.kitsugi.animelist.core.player

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.DefaultAllocator

/**
 * DefaultLoadControl with custom back buffer duration override and memory budget support.
 * Derived from KitsugiTV-dev.
 */
@UnstableApi
class BitrateAwareLoadControl(
    minBufferMs: Int,
    maxBufferMs: Int,
    bufferForPlaybackMs: Int,
    bufferForPlaybackAfterRebufferMs: Int,
    prioritizeTimeOverSizeThresholds: Boolean,
    backBufferDurationMs: Int,
    retainBackBufferFromKeyframe: Boolean,
    /** Memory ceiling in bytes. Defaults to unset. */
    private val budgetBytes: Long = C.LENGTH_UNSET.toLong(),
    allocator: DefaultAllocator = DefaultAllocator(/* trimOnReset= */ true, C.DEFAULT_BUFFER_SEGMENT_SIZE, 64)
) : DefaultLoadControl(
    allocator,
    minBufferMs,
    maxBufferMs,
    bufferForPlaybackMs,
    bufferForPlaybackAfterRebufferMs,
    /* targetBufferBytes= */ C.LENGTH_UNSET,
    prioritizeTimeOverSizeThresholds,
    backBufferDurationMs,
    retainBackBufferFromKeyframe
) {

    @Volatile
    private var backBufferOverrideUs: Long = -1L

    /** Set the back buffer at runtime; negative restores the constructed value. */
    fun setBackBufferDurationOverrideMs(ms: Int) {
        backBufferOverrideUs = if (ms < 0) -1L else ms.toLong() * 1000L
    }

    @Volatile
    private var budgetBytesOverride: Long = -1L

    /** Set the byte budget at runtime; negative restores the constructed budget. */
    fun setBudgetBytesOverride(bytes: Long) {
        budgetBytesOverride = if (bytes < 0L) -1L else bytes
    }

    override fun getBackBufferDurationUs(playerId: PlayerId): Long {
        val override = backBufferOverrideUs
        return if (override >= 0L) override else super.getBackBufferDurationUs(playerId)
    }

    override fun calculateTargetBufferBytes(
        trackSelectionArray: Array<out ExoTrackSelection?>
    ): Int {
        val effectiveBudget = if (budgetBytesOverride >= 0L) budgetBytesOverride else budgetBytes
        if (effectiveBudget == C.LENGTH_UNSET.toLong()) {
            return super.calculateTargetBufferBytes(trackSelectionArray)
        }
        return effectiveBudget.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
