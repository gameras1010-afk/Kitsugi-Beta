package com.kitsugi.animelist.core.player

import android.util.Log

/**
 * T1.13 – PlayerPlaybackAnalytics
 *
 * Tracks per-session playback quality metrics such as total watched time,
 * seek counts, completion percentage, and stall ratio.
 *
 * Designed to be held as a field on [Media3PlayerEngine] and updated via
 * ExoPlayer [Player.Listener] callbacks.  All mutations are thread-safe.
 *
 * Usage:
 * ```kotlin
 * analytics.startSession(url)
 * // on Player.Listener callbacks:
 * analytics.onPlaybackTick(currentPositionMs, durationMs)
 * analytics.onSeek()
 * analytics.onStalledMs(500)
 * // on end / release:
 * analytics.endSession().let { Log.i(TAG, it.toString()) }
 * ```
 */
class PlayerPlaybackAnalytics {

    private val TAG = "PlayerPlaybackAnalytics"

    data class SessionReport(
        val sessionId: String,
        val videoUrl: String,
        val durationMs: Long,
        val watchedMs: Long,
        val completionPercent: Float,
        val seekCount: Int,
        val stallCount: Int,
        val totalStallMs: Long,
        val stallRatioPercent: Float,
        val qualityChanges: Int,
    ) {
        override fun toString(): String = buildString {
            appendLine("── PlayerPlaybackAnalytics [$sessionId] ──")
            appendLine("  url            : $videoUrl")
            appendLine("  duration       : ${durationMs / 1000}s")
            appendLine("  watched        : ${watchedMs / 1000}s (${completionPercent.toInt()}%)")
            appendLine("  seeks          : $seekCount")
            appendLine("  stalls         : $stallCount events / ${totalStallMs} ms total (${stallRatioPercent.toInt()}% stall ratio)")
            appendLine("  quality changes: $qualityChanges")
        }
    }

    // ── Internal state ────────────────────────────────────────────────────────

    @Volatile private var sessionId = ""
    @Volatile private var videoUrl = ""

    @Volatile private var lastTickMs: Long = 0L
    @Volatile private var lastPositionMs: Long = 0L
    @Volatile private var watchedMs: Long = 0L
    @Volatile private var durationMs: Long = 0L

    @Volatile private var seekCount = 0
    @Volatile private var stallCount = 0
    @Volatile private var totalStallMs = 0L
    @Volatile private var qualityChanges = 0

    @Volatile private var isPlaying = false

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Synchronized
    fun startSession(sessionId: String, videoUrl: String) {
        this.sessionId = sessionId
        this.videoUrl = videoUrl
        lastTickMs = System.currentTimeMillis()
        lastPositionMs = 0L
        watchedMs = 0L
        durationMs = 0L
        seekCount = 0
        stallCount = 0
        totalStallMs = 0L
        qualityChanges = 0
        isPlaying = false
        Log.d(TAG, "Analytics session started [$sessionId]")
    }

    @Synchronized
    fun onPlayingChanged(playing: Boolean) {
        isPlaying = playing
    }

    /**
     * Should be called periodically (e.g., every 1-second tick) while content is playing.
     */
    @Synchronized
    fun onPlaybackTick(positionMs: Long, durationMs: Long) {
        val now = System.currentTimeMillis()
        if (isPlaying && positionMs > lastPositionMs) {
            watchedMs += (now - lastTickMs).coerceAtMost(2_000L) // cap per-tick at 2s to handle pauses
        }
        lastPositionMs = positionMs
        lastTickMs = now
        if (durationMs > 0) this.durationMs = durationMs
    }

    @Synchronized
    fun onSeek() {
        seekCount++
    }

    @Synchronized
    fun onStalledMs(stallDurationMs: Long) {
        stallCount++
        totalStallMs += stallDurationMs
    }

    @Synchronized
    fun onQualityChanged() {
        qualityChanges++
    }

    @Synchronized
    fun endSession(): SessionReport {
        val effectiveDuration = durationMs.takeIf { it > 0 } ?: 1L
        val completion = (watchedMs.toFloat() / effectiveDuration * 100f).coerceIn(0f, 100f)
        val stallRatio = if (watchedMs > 0) totalStallMs.toFloat() / watchedMs * 100f else 0f

        return SessionReport(
            sessionId          = sessionId,
            videoUrl           = videoUrl,
            durationMs         = durationMs,
            watchedMs          = watchedMs,
            completionPercent  = completion,
            seekCount          = seekCount,
            stallCount         = stallCount,
            totalStallMs       = totalStallMs,
            stallRatioPercent  = stallRatio,
            qualityChanges     = qualityChanges,
        ).also { report ->
            Log.i(TAG, report.toString())
        }
    }
}
