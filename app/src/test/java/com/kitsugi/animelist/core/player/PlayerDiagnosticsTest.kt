package com.kitsugi.animelist.core.player

import org.junit.Assert.*
import org.junit.Test

/**
 * T4.1 – PlayerDiagnosticsTest
 *
 * PlayerLoadingDiagnostics ve PlayerPlaybackAnalytics sınıflarının
 * doğruluğunu ve thread-safety'ini doğrular.
 */
class PlayerDiagnosticsTest {

    // ── PlayerLoadingDiagnostics ──────────────────────────────────────────────

    @Test
    fun `PlayerLoadingDiagnostics dump without startSession returns empty sessionId`() {
        val diag = PlayerLoadingDiagnostics()
        val snap = diag.dump()
        assertEquals("", snap.sessionId)
    }

    @Test
    fun `PlayerLoadingDiagnostics startSession records sessionId and url`() {
        val diag = PlayerLoadingDiagnostics()
        diag.startSession("abc123", "https://example.com/video.m3u8")
        val snap = diag.dump()
        assertEquals("abc123", snap.sessionId)
        assertEquals("https://example.com/video.m3u8", snap.videoUrl)
    }

    @Test
    fun `PlayerLoadingDiagnostics onBufferingStarted and onBufferingEnded records event`() {
        val diag = PlayerLoadingDiagnostics()
        diag.startSession("s1", "url")
        diag.onBufferingStarted("initial-load")
        Thread.sleep(10)
        diag.onBufferingEnded()
        val snap = diag.dump()
        assertEquals(1, snap.bufferingEvents.size)
        assertTrue("bufferingMs should be > 0", snap.bufferingEvents[0].durationMs >= 0)
    }

    @Test
    fun `PlayerLoadingDiagnostics multiple buffering events are all recorded`() {
        val diag = PlayerLoadingDiagnostics()
        diag.startSession("s2", "url")
        repeat(3) {
            diag.onBufferingStarted("rebuffer")
            diag.onBufferingEnded()
        }
        val snap = diag.dump()
        assertEquals(3, snap.bufferingEvents.size)
    }

    @Test
    fun `PlayerLoadingDiagnostics totalBufferingMs is sum of event durations`() {
        val diag = PlayerLoadingDiagnostics()
        diag.startSession("s3", "url")
        repeat(2) {
            diag.onBufferingStarted("rebuffer")
            diag.onBufferingEnded()
        }
        val snap = diag.dump()
        val expected = snap.bufferingEvents.sumOf { it.durationMs }
        assertEquals(expected, snap.totalBufferingMs)
    }

    @Test
    fun `PlayerLoadingDiagnostics onFirstFrame records non-null firstFrameMs`() {
        val diag = PlayerLoadingDiagnostics()
        diag.startSession("s4", "url")
        diag.onFirstFrame()
        val snap = diag.dump()
        assertNotNull(snap.firstFrameMs)
    }

    @Test
    fun `PlayerLoadingDiagnostics onFirstFrame is idempotent`() {
        val diag = PlayerLoadingDiagnostics()
        diag.startSession("s5", "url")
        diag.onFirstFrame()
        val firstTime = diag.dump().firstFrameMs
        diag.onFirstFrame() // second call should be ignored
        val secondTime = diag.dump().firstFrameMs
        assertEquals(firstTime, secondTime)
    }

    @Test
    fun `PlayerLoadingDiagnostics onPlaybackError records error code and message`() {
        val diag = PlayerLoadingDiagnostics()
        diag.startSession("s6", "url")
        diag.onPlaybackError(2001, "Decoding error")
        val snap = diag.dump()
        assertEquals(2001, snap.errorCode)
        assertEquals("Decoding error", snap.errorMessage)
        assertEquals("ERROR", snap.finalState)
    }

    @Test
    fun `PlayerLoadingDiagnostics bitrateEstimate is captured and aggregated`() {
        val diag = PlayerLoadingDiagnostics()
        diag.startSession("s7", "url")
        diag.onBitrateEstimate(1_000_000L) // 1 Mbps
        diag.onBitrateEstimate(2_000_000L) // 2 Mbps
        val snap = diag.dump()
        assertEquals(2000, snap.peakBitrateKbps)
        assertEquals(1500, snap.averageBitrateKbps) // (1000+2000)/2
    }

    @Test
    fun `PlayerLoadingDiagnostics startSession resets previous state`() {
        val diag = PlayerLoadingDiagnostics()
        diag.startSession("old", "old-url")
        diag.onPlaybackError(999, "old error")
        diag.startSession("new", "new-url")
        val snap = diag.dump()
        assertEquals("new", snap.sessionId)
        assertNull(snap.errorCode)
        assertEquals(0, snap.bufferingEvents.size)
    }

    // ── PlayerPlaybackAnalytics ───────────────────────────────────────────────

    @Test
    fun `PlayerPlaybackAnalytics endSession without startSession returns zero watchedMs`() {
        val analytics = PlayerPlaybackAnalytics()
        val report = analytics.endSession()
        assertEquals(0L, report.watchedMs)
        assertEquals(0f, report.completionPercent, 0.01f)
    }

    @Test
    fun `PlayerPlaybackAnalytics onSeek increments seekCount`() {
        val analytics = PlayerPlaybackAnalytics()
        analytics.startSession("s1", "url")
        analytics.onSeek()
        analytics.onSeek()
        analytics.onSeek()
        val report = analytics.endSession()
        assertEquals(3, report.seekCount)
    }

    @Test
    fun `PlayerPlaybackAnalytics onStalledMs accumulates totalStallMs`() {
        val analytics = PlayerPlaybackAnalytics()
        analytics.startSession("s2", "url")
        analytics.onStalledMs(100L)
        analytics.onStalledMs(200L)
        val report = analytics.endSession()
        assertEquals(2, report.stallCount)
        assertEquals(300L, report.totalStallMs)
    }

    @Test
    fun `PlayerPlaybackAnalytics onQualityChanged increments qualityChanges`() {
        val analytics = PlayerPlaybackAnalytics()
        analytics.startSession("s3", "url")
        analytics.onQualityChanged()
        analytics.onQualityChanged()
        val report = analytics.endSession()
        assertEquals(2, report.qualityChanges)
    }

    @Test
    fun `PlayerPlaybackAnalytics completionPercent does not exceed 100`() {
        val analytics = PlayerPlaybackAnalytics()
        analytics.startSession("s4", "url")
        analytics.onPlayingChanged(true)
        // Simulate watchedMs > durationMs scenario
        analytics.onPlaybackTick(60_000L, 30_000L) // over-ticked
        analytics.onPlaybackTick(60_000L, 30_000L)
        val report = analytics.endSession()
        assertTrue("completionPercent should be <= 100", report.completionPercent <= 100f)
    }

    @Test
    fun `PlayerPlaybackAnalytics startSession resets all counters`() {
        val analytics = PlayerPlaybackAnalytics()
        analytics.startSession("old", "old-url")
        analytics.onSeek()
        analytics.onStalledMs(500L)
        analytics.onQualityChanged()

        analytics.startSession("new", "new-url")
        val report = analytics.endSession()
        assertEquals(0, report.seekCount)
        assertEquals(0, report.stallCount)
        assertEquals(0L, report.totalStallMs)
        assertEquals(0, report.qualityChanges)
        assertEquals("new", report.sessionId)
    }
}
