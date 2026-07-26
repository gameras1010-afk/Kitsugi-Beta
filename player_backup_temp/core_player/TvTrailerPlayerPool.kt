package com.kitsugi.animelist.core.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Application-scoped singleton that holds a single ExoPlayer instance dedicated to
 * trailer/preview playback on the TV home screen and TV detail hero.
 *
 * Adapted from KitsugiTV-dev [TrailerPlayerPool] with:
 * - No Hilt/Dagger injection: use [TvTrailerPlayerPoolHolder] to access the singleton.
 * - No PlayerSettingsDataStore dependency: uses sensible buffer defaults for TV.
 * - yield()/reclaim() pattern preserved so the full-screen player can claim hardware
 *   decoders when needed.
 *
 * Thread safety: acquire/stop/yield/reclaim/release are safe to call from any thread.
 */
@OptIn(UnstableApi::class)
class TvTrailerPlayerPool(private val context: Context) {

    companion object {
        private const val TAG = "TvTrailerPlayerPool"
    }

    private var _player: ExoPlayer? = null
    private val yielded = AtomicBoolean(false)
    private val released = AtomicBoolean(false)

    /**
     * Returns the shared trailer [ExoPlayer], creating it lazily if needed.
     * Returns null only if [release] was called.
     */
    fun acquire(): ExoPlayer? {
        if (released.get()) return null
        if (yielded.get()) reclaim()
        return _player ?: buildPlayer().also { _player = it }
    }

    /**
     * Stops playback and clears media items while keeping the instance alive for reuse.
     * Call when the trailer is no longer visible (focus lost, screen change).
     */
    fun stop() {
        _player?.let { p ->
            runCatching {
                p.playWhenReady = false
                p.stop()
                p.clearMediaItems()
            }.onFailure { Log.w(TAG, "stop error: ${it.message}") }
        }
    }

    /**
     * Releases codec resources so the detail-screen player can claim hardware decoders.
     * A fresh player will be created on the next [acquire] after [reclaim] is called.
     */
    fun yield() {
        if (yielded.compareAndSet(false, true)) {
            Log.d(TAG, "Yielding TV trailer player for detail playback")
            _player?.let { p ->
                runCatching { p.stop() }
                runCatching { p.clearMediaItems() }
                runCatching { p.release() }
            }
            _player = null
        }
    }

    /**
     * Marks the pool as reclaimed so the next [acquire] will rebuild the player lazily.
     * Safe to call multiple times.
     */
    fun reclaim() {
        if (released.get()) return
        if (yielded.compareAndSet(true, false)) {
            Log.d(TAG, "Reclaiming TV trailer player")
        }
    }

    /**
     * Permanently releases the player. Call from Application.onTerminate or a lifecycle observer.
     */
    fun release() {
        if (released.compareAndSet(false, true)) {
            Log.d(TAG, "Releasing TV trailer player permanently")
            _player?.let { p ->
                runCatching { p.stop() }
                runCatching { p.clearMediaItems() }
                runCatching { p.release() }
            }
            _player = null
        }
    }

    /**
     * True if the pool holds an active (non-yielded, non-released) player instance.
     */
    val isActive: Boolean get() = _player != null && !yielded.get() && !released.get()

    // ─── Player creation ──────────────────────────────────────────────────────

    private fun buildPlayer(): ExoPlayer {
        Log.d(TAG, "Creating shared TV trailer ExoPlayer instance")

        // TV-optimised buffer settings: larger buffers for smooth trailer autoplay
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs             = */ 30_000,
                /* maxBufferMs             = */ 120_000,
                /* bufferForPlaybackMs     = */ 5_000,
                /* bufferForPlaybackAfterRebufferMs = */ 10_000
            )
            .build()

        // Force highest bitrate from start — TV displays are large; quality matters more than start-up time
        val bandwidthMeter = DefaultBandwidthMeter.Builder(context)
            .setInitialBitrateEstimate(50_000_000L) // 50 Mbps: forces highest HLS variant immediately
            .build()

        // Track selector: allow all resolutions; adaptive quality on TV is preferable
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .clearVideoSizeConstraints()
                    .setForceHighestSupportedBitrate(true)
                    .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            )
        }

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setBandwidthMeter(bandwidthMeter)
            .setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS)
            .build()
            .apply {
                volume = 0f          // Mute by default; caller enables audio when needed
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }
}

// ─── Application-level singleton holder ───────────────────────────────────────

object TvTrailerPlayerPoolHolder {
    @Volatile
    private var instance: TvTrailerPlayerPool? = null

    fun get(context: Context): TvTrailerPlayerPool =
        instance ?: synchronized(this) {
            instance ?: TvTrailerPlayerPool(context.applicationContext).also { instance = it }
        }

    /** Call from Application.onTerminate (or a ProcessLifecycleOwner observer). */
    fun release() {
        instance?.release()
        instance = null
    }
}
