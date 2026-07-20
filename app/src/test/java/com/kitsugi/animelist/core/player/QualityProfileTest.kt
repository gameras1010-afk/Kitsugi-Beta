package com.kitsugi.animelist.core.player

import org.junit.Assert.*
import org.junit.Test

/**
 * T4.1 – QualityProfileTest
 *
 * QualityProfile + QualityDataHelper mantığını ayrıntılı test eder.
 */
class QualityProfileTest {

    // ── Serialize / Deserialize ───────────────────────────────────────────────

    @Test
    fun `QualityProfile serialize produces pipe-separated string`() {
        val profile = QualityProfile(QualityPreference.P720, 4000)
        val result = QualityProfile.serialize(profile)
        assertEquals("P720|4000", result)
    }

    @Test
    fun `QualityProfile deserialize P1080 with maxBitrate`() {
        val result = QualityProfile.deserialize("P1080|8000")
        assertEquals(QualityPreference.P1080, result.preference)
        assertEquals(8000, result.maxBitrateKbps)
    }

    @Test
    fun `QualityProfile deserialize DATA_SAVER`() {
        val result = QualityProfile.deserialize("DATA_SAVER|-1")
        assertEquals(QualityPreference.DATA_SAVER, result.preference)
        assertEquals(-1, result.maxBitrateKbps)
    }

    @Test
    fun `QualityProfile deserialize unknown preference falls back to AUTO`() {
        val result = QualityProfile.deserialize("UNKNOWN|2000")
        assertEquals(QualityPreference.AUTO, result.preference)
    }

    @Test
    fun `QualityProfile deserialize no bitrate part uses -1`() {
        val result = QualityProfile.deserialize("P480")
        assertEquals(QualityPreference.P480, result.preference)
        assertEquals(-1, result.maxBitrateKbps)
    }

    // ── QualityPreference range invariants ────────────────────────────────────

    @Test
    fun `QualityPreference AUTO covers full range`() {
        val auto = QualityPreference.AUTO
        assertEquals(-1, auto.minQualityValue)
        assertEquals(Int.MAX_VALUE, auto.maxQualityValue)
    }

    @Test
    fun `QualityPreference P1080 lower bound is 1080`() {
        assertEquals(1080, QualityPreference.P1080.minQualityValue)
    }

    @Test
    fun `QualityPreference bands do not overlap`() {
        // P480 max < P720 min
        assertTrue(QualityPreference.P480.maxQualityValue < QualityPreference.P720.minQualityValue)
        // P720 max < P1080 min
        assertTrue(QualityPreference.P720.maxQualityValue < QualityPreference.P1080.minQualityValue)
    }

    // ── QualityDataHelper.filterByBitrate ─────────────────────────────────────

    @Test
    fun `filterByBitrate with -1 maxBitrate returns all sources`() {
        val sources = makeSources(listOf(480, 720, 1080))
        val result = QualityDataHelper.filterByBitrate(sources, -1)
        assertEquals(3, result.size)
    }

    @Test
    fun `filterByBitrate excludes sources above limit`() {
        // qualityValue is in "p" units; filterByBitrate multiplies by 8 to get kbps
        // 720p * 8 = 5760 kbps; limit = 5000 → 720p excluded
        val sources = makeSources(listOf(480, 720, 1080))
        // 480 * 8 = 3840 ≤ 5000 → keep; 720 * 8 = 5760 > 5000 → drop; 1080 * 8 > 5000 → drop
        val result = QualityDataHelper.filterByBitrate(sources, 5000)
        assertEquals(1, result.size)
        assertEquals(480, result.first().qualityValue)
    }

    @Test
    fun `filterByBitrate with 0 qualityValue source always passes`() {
        // Unknown-bitrate sources should not be filtered out
        val sources = makeSources(listOf(0, 1080))
        val result = QualityDataHelper.filterByBitrate(sources, 1000)
        assertTrue(result.any { it.qualityValue == 0 })
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun makeSources(qualities: List<Int>): List<com.kitsugi.animelist.data.repository.StreamSource> {
        return qualities.map { q ->
            com.kitsugi.animelist.data.repository.StreamSource(
                addonName    = "Test",
                name         = "${q}p",
                title        = "${q}p Test Stream",
                url          = "https://cdn.example.com/${q}p.m3u8",
                infoHash     = null,
                fileIndex    = null,
                qualityValue = q
            )
        }
    }

}
