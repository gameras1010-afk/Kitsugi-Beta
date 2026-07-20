package com.kitsugi.animelist.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FrameRateUtilsTest {

    @Test
    fun testSnapToStandardRate() {
        // NTSC Film (23.976)
        assertEquals(24000f / 1001f, FrameRateUtils.snapToStandardRate(23.976f), 0.001f)
        assertEquals(24000f / 1001f, FrameRateUtils.snapToStandardRate(23.95f), 0.001f)

        // Cinema 24
        assertEquals(24f, FrameRateUtils.snapToStandardRate(24.0f), 0.001f)
        assertEquals(24f, FrameRateUtils.snapToStandardRate(24.05f), 0.001f)

        // PAL 25
        assertEquals(25f, FrameRateUtils.snapToStandardRate(25.0f), 0.001f)
        assertEquals(25f, FrameRateUtils.snapToStandardRate(24.95f), 0.001f)

        // NTSC 30 (29.97)
        assertEquals(30000f / 1001f, FrameRateUtils.snapToStandardRate(29.97f), 0.001f)

        // 30 FPS
        assertEquals(30f, FrameRateUtils.snapToStandardRate(30.0f), 0.001f)

        // 50 FPS
        assertEquals(50f, FrameRateUtils.snapToStandardRate(50.0f), 0.001f)

        // 59.94 FPS
        assertEquals(60000f / 1001f, FrameRateUtils.snapToStandardRate(59.94f), 0.001f)

        // 60 FPS
        assertEquals(60f, FrameRateUtils.snapToStandardRate(60.0f), 0.001f)

        // Unsnapped rates should remain unchanged
        assertEquals(15f, FrameRateUtils.snapToStandardRate(15f), 0.001f)
        assertEquals(45f, FrameRateUtils.snapToStandardRate(45f), 0.001f)
    }

    @Test
    fun testFrameRateCacheAndSanitization() {
        val url = "https://example.com/video.mkv"
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0",
            "Authorization" to "Bearer token123",
            "Range" to "bytes=0-" // Range should be stripped in sanitizeHeaders
        )

        val detection = FrameRateUtils.FrameRateDetection(
            raw = 23.976f,
            snapped = 23.976f,
            videoWidth = 1920,
            videoHeight = 1080
        )

        // Initially not cached
        assertNull(FrameRateUtils.getCachedFrameRate(url, headers))

        // Cache it
        FrameRateUtils.cacheFrameRate(url, headers, detection)

        // Verify cache hit
        val cached = FrameRateUtils.getCachedFrameRate(url, headers)
        assertNotNull(cached)
        assertEquals(23.976f, cached!!.raw, 0.001f)
        assertEquals(1920, cached.videoWidth)

        // Verify headers with different order/case get a cache hit (after normalization)
        val headersPermuted = mapOf(
            "authorization" to "Bearer token123",
            "user-agent" to "Mozilla/5.0"
        )
        val cachedPermuted = FrameRateUtils.getCachedFrameRate(url, headersPermuted)
        assertNotNull(cachedPermuted)

        // Verify different authorization token gets a cache miss
        val headersDifferent = mapOf(
            "User-Agent" to "Mozilla/5.0",
            "Authorization" to "Bearer token999"
        )
        assertNull(FrameRateUtils.getCachedFrameRate(url, headersDifferent))
    }
}
