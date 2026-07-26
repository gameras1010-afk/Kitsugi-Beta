package com.kitsugi.animelist.core.player

import android.util.Log

/**
 * T1.13 – PlayerLoadingDiagnostics
 *
 * Lightweight diagnostics collector that records the timeline of loading events
 * during a single playback session.  Once the session ends (or an error occurs),
 * call [dump] to obtain a structured snapshot suitable for Logcat output or a
 * crash-report payload.
 *
 * Thread-safe: all mutations are synchronised on [this].
 */
class PlayerLoadingDiagnostics {

    private val TAG = "PlayerLoadingDiag"

    /** Immutable snapshot emitted by [dump]. */
    data class Snapshot(
        val sessionId: String,
        val videoUrl: String,
        val startedAtMs: Long,
        val firstFrameMs: Long?,           // null if not yet reached
        val bufferingEvents: List<BufferingEvent>,
        val errorCode: Int?,
        val errorMessage: String?,
        val finalState: String,
        val totalBufferingMs: Long,
        val peakBitrateKbps: Int,
        val averageBitrateKbps: Int,
    ) {
        fun toLogString(): String = buildString {
            appendLine("── PlayerLoadingDiagnostics [$sessionId] ──")
            appendLine("  url        : $videoUrl")
            appendLine("  startedAt  : $startedAtMs ms")
            appendLine("  firstFrame : ${firstFrameMs?.let { "${it - startedAtMs} ms to first frame" } ?: "n/a"}")
            appendLine("  state      : $finalState")
            appendLine("  buffering  : ${bufferingEvents.size} event(s), total ${totalBufferingMs} ms")
            appendLine("  bitrate    : peak ${peakBitrateKbps} kbps / avg ${averageBitrateKbps} kbps")
            if (errorCode != null) appendLine("  ERROR      : code=$errorCode msg=$errorMessage")
        }
    }

    data class BufferingEvent(
        val startMs: Long,
        val durationMs: Long,
        val reason: String,
    )

    // ── Session state ─────────────────────────────────────────────────────────

    private var sessionId: String = ""
    private var videoUrl: String = ""
    private var startedAtMs: Long = 0L
    private var firstFrameMs: Long? = null
    private val bufferingEvents = mutableListOf<BufferingEvent>()
    private var currentBufferingStart: Long? = null
    private var errorCode: Int? = null
    private var errorMessage: String? = null
    private var finalState: String = "IDLE"
    private val bitrateReadings = mutableListOf<Int>()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Synchronized
    fun startSession(sessionId: String, videoUrl: String) {
        this.sessionId = sessionId
        this.videoUrl = videoUrl
        this.startedAtMs = System.currentTimeMillis()
        this.firstFrameMs = null
        this.bufferingEvents.clear()
        this.currentBufferingStart = null
        this.errorCode = null
        this.errorMessage = null
        this.finalState = "LOADING"
        this.bitrateReadings.clear()
        com.kitsugi.animelist.core.diagnostics.FileLoggingTree.d(TAG, "Session started [$sessionId] url=$videoUrl")
    }

    @Synchronized
    fun onFirstFrame() {
        if (firstFrameMs == null) {
            firstFrameMs = System.currentTimeMillis()
            com.kitsugi.animelist.core.diagnostics.FileLoggingTree.d(TAG, "First frame rendered in ${firstFrameMs!! - startedAtMs} ms")
        }
    }

    @Synchronized
    fun onBufferingStarted(reason: String = "unknown") {
        currentBufferingStart = System.currentTimeMillis()
        com.kitsugi.animelist.core.diagnostics.FileLoggingTree.d(TAG, "Buffering started: $reason")
    }

    @Synchronized
    fun onBufferingEnded() {
        val start = currentBufferingStart ?: return
        val durationMs = System.currentTimeMillis() - start
        bufferingEvents.add(BufferingEvent(start, durationMs, "rebuffer"))
        currentBufferingStart = null
        com.kitsugi.animelist.core.diagnostics.FileLoggingTree.d(TAG, "Buffering ended after ${durationMs} ms")
    }

    @Synchronized
    fun onBitrateEstimate(bitsPerSecond: Long) {
        val kbps = (bitsPerSecond / 1000L).toInt().coerceAtLeast(0)
        if (kbps > 0) bitrateReadings.add(kbps)
    }

    @Synchronized
    fun onPlaybackError(code: Int, message: String?) {
        errorCode = code
        errorMessage = message
        finalState = "ERROR"
        com.kitsugi.animelist.core.diagnostics.FileLoggingTree.e(TAG, "Playback error code=$code msg=$message")
    }

    @Synchronized
    fun onPlaybackEnded() {
        finalState = "ENDED"
    }

    @Synchronized
    fun onStateChanged(state: String) {
        finalState = state
    }

    // ── Output ────────────────────────────────────────────────────────────────

    @Synchronized
    fun dump(): Snapshot {
        val totalBuf = bufferingEvents.sumOf { it.durationMs }
        val peak = bitrateReadings.maxOrNull() ?: 0
        val avg = if (bitrateReadings.isNotEmpty()) bitrateReadings.average().toInt() else 0

        return Snapshot(
            sessionId         = sessionId,
            videoUrl          = videoUrl,
            startedAtMs       = startedAtMs,
            firstFrameMs      = firstFrameMs,
            bufferingEvents   = bufferingEvents.toList(),
            errorCode         = errorCode,
            errorMessage      = errorMessage,
            finalState        = finalState,
            totalBufferingMs  = totalBuf,
            peakBitrateKbps  = peak,
            averageBitrateKbps = avg,
        ).also { snapshot ->
            com.kitsugi.animelist.core.diagnostics.FileLoggingTree.i(TAG, snapshot.toLogString())
        }
    }
}
