package com.kitsugi.animelist.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenSubtitlesHasherTest {

    @Test
    fun testComputeWithInvalidUrlScheme() {
        val result = OpenSubtitlesHasher.compute("ftp://example.com/video.mp4")
        assertNull(result)
    }

    @Test
    fun testComputeWithEmptyUrl() {
        val result = OpenSubtitlesHasher.compute("")
        assertNull(result)
    }

    @Test
    fun testHashResultDataClass() {
        val result = OpenSubtitlesHasher.HashResult("abc123xyz", 987654321L)
        assertEquals("abc123xyz", result.hash)
        assertEquals(987654321L, result.size)
    }
}
