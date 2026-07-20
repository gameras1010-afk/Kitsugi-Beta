package com.kitsugi.animelist.data.settings

import org.junit.Assert.*
import org.junit.Test

/**
 * T4.1 – SettingsMigrationTest
 *
 * AppSettings default değerlerinin ve JSON serialization yardımcı sınıflarının
 * doğruluğunu doğrular. Gerçek bir DataStore'a ihtiyaç duymaz – saf Kotlin
 * unit testleridir, dolayısıyla JVM'de hızlıca çalışır.
 *
 * Kapsam:
 *  - AppSettings default değerleri (migration edge-case'ler)
 *  - AudioDelayRouteConfig JSON round-trip
 *  - QualityProfile serialize/deserialize
 *  - FrameRateMatchingMode ve Dv7HandlingMode enum valueOf safety
 */
class SettingsMigrationTest {

    // ── AppSettings default değerleri ─────────────────────────────────────────

    @Test
    fun `AppSettings default minBufferMs is 15000`() {
        val settings = AppSettings()
        assertEquals(15_000, settings.minBufferMs)
    }

    @Test
    fun `AppSettings default maxBufferMs is 45000`() {
        val settings = AppSettings()
        assertEquals(45_000, settings.maxBufferMs)
    }

    @Test
    fun `AppSettings default parallelRangeEnabled is false`() {
        assertFalse(AppSettings().parallelRangeEnabled)
    }

    @Test
    fun `AppSettings default airingNotificationsEnabled is false`() {
        assertFalse(AppSettings().airingNotificationsEnabled)
    }

    @Test
    fun `AppSettings default previewSeekbarEnabled is true`() {
        assertTrue(AppSettings().previewSeekbarEnabled)
    }

    @Test
    fun `AppSettings default showPlayerTitle is true`() {
        assertTrue(AppSettings().showPlayerTitle)
    }

    @Test
    fun `AppSettings default showPlayerResolution is true`() {
        assertTrue(AppSettings().showPlayerResolution)
    }

    @Test
    fun `AppSettings default titleLimitType is NONE`() {
        assertEquals("NONE", AppSettings().titleLimitType)
    }

    @Test
    fun `AppSettings default dnsChoice is 0 (System)`() {
        assertEquals(0, AppSettings().dnsChoice)
    }

    @Test
    fun `AppSettings default postPlayMode is AUTO_PLAY_NEXT`() {
        assertEquals("AUTO_PLAY_NEXT", AppSettings().postPlayMode)
    }

    @Test
    fun `AppSettings default stillWatchingEnabled is true`() {
        assertTrue(AppSettings().stillWatchingEnabled)
    }

    @Test
    fun `AppSettings default stillWatchingThresholdMinutes is 90`() {
        assertEquals(90, AppSettings().stillWatchingThresholdMinutes)
    }

    @Test
    fun `AppSettings default frameRateMatchingMode is OFF`() {
        assertEquals(FrameRateMatchingMode.OFF, AppSettings().frameRateMatchingMode)
    }

    @Test
    fun `AppSettings default dv7HandlingMode is AUTO`() {
        assertEquals(Dv7HandlingMode.AUTO, AppSettings().dv7HandlingMode)
    }

    @Test
    fun `AppSettings default liveHelperEnabled is false`() {
        assertFalse(AppSettings().liveHelperEnabled)
    }

    @Test
    fun `AppSettings default enableAssExtractor is false`() {
        assertFalse(AppSettings().enableAssExtractor)
    }

    @Test
    fun `AppSettings default mangaReadingMode is RightToLeft`() {
        assertEquals("RightToLeft", AppSettings().mangaReadingMode)
    }

    @Test
    fun `AppSettings default mangaColorFilter is Normal`() {
        assertEquals("Normal", AppSettings().mangaColorFilter)
    }

    @Test
    fun `AppSettings default mangaFitMode is FitScreen`() {
        assertEquals("FitScreen", AppSettings().mangaFitMode)
    }

    @Test
    fun `AppSettings default mangaBrightness is 1_0f`() {
        assertEquals(1.0f, AppSettings().mangaBrightness, 0.001f)
    }

    @Test
    fun `AppSettings default searchHistoryEnabled is true`() {
        assertTrue(AppSettings().searchHistoryEnabled)
    }

    // ── FrameRateMatchingMode enum safety ─────────────────────────────────────

    @Test
    fun `FrameRateMatchingMode valueOf known names succeeds`() {
        assertEquals(FrameRateMatchingMode.OFF, FrameRateMatchingMode.valueOf("OFF"))
        assertEquals(FrameRateMatchingMode.START, FrameRateMatchingMode.valueOf("START"))
        assertEquals(FrameRateMatchingMode.START_STOP, FrameRateMatchingMode.valueOf("START_STOP"))
    }

    @Test
    fun `FrameRateMatchingMode runCatching on unknown name returns OFF`() {
        val result = runCatching { FrameRateMatchingMode.valueOf("INVALID_KEY") }
            .getOrDefault(FrameRateMatchingMode.OFF)
        assertEquals(FrameRateMatchingMode.OFF, result)
    }

    // ── Dv7HandlingMode enum safety ───────────────────────────────────────────

    @Test
    fun `Dv7HandlingMode valueOf all known values succeeds`() {
        Dv7HandlingMode.values().forEach { mode ->
            assertEquals(mode, runCatching { Dv7HandlingMode.valueOf(mode.name) }.getOrNull())
        }
    }

    @Test
    fun `Dv7HandlingMode runCatching on unknown name returns AUTO`() {
        val result = runCatching { Dv7HandlingMode.valueOf("LEGACY_MODE") }
            .getOrDefault(Dv7HandlingMode.AUTO)
        assertEquals(Dv7HandlingMode.AUTO, result)
    }

    // ── QualityProfile serialize / deserialize ────────────────────────────────

    @Test
    fun `QualityProfile serialize then deserialize round-trip`() {
        val profile = com.kitsugi.animelist.core.player.QualityProfile(
            preference = com.kitsugi.animelist.core.player.QualityPreference.P1080,
            maxBitrateKbps = 8000
        )
        val serialized = com.kitsugi.animelist.core.player.QualityProfile.serialize(profile)
        val restored = com.kitsugi.animelist.core.player.QualityProfile.deserialize(serialized)
        assertEquals(profile.preference, restored.preference)
        assertEquals(profile.maxBitrateKbps, restored.maxBitrateKbps)
    }

    @Test
    fun `QualityProfile deserialize null returns DEFAULT`() {
        val result = com.kitsugi.animelist.core.player.QualityProfile.deserialize(null)
        assertEquals(com.kitsugi.animelist.core.player.QualityProfile.DEFAULT, result)
    }

    @Test
    fun `QualityProfile deserialize blank returns DEFAULT`() {
        val result = com.kitsugi.animelist.core.player.QualityProfile.deserialize("")
        assertEquals(com.kitsugi.animelist.core.player.QualityProfile.DEFAULT, result)
    }

    @Test
    fun `QualityProfile deserialize corrupt string returns AUTO`() {
        val result = com.kitsugi.animelist.core.player.QualityProfile.deserialize("garbage|NaN")
        assertEquals(com.kitsugi.animelist.core.player.QualityPreference.AUTO, result.preference)
        assertEquals(-1, result.maxBitrateKbps)
    }

    @Test
    fun `QualityProfile all QualityPreference enum values have valid ranges`() {
        com.kitsugi.animelist.core.player.QualityPreference.values().forEach { pref ->
            assertTrue("${pref.name} minQualityValue must be >= -1", pref.minQualityValue >= -1)
            assertTrue("${pref.name} maxQualityValue must be >= minQualityValue",
                pref.maxQualityValue >= pref.minQualityValue)
        }
    }

    // ── AudioDelayRouteConfig JSON round-trip ─────────────────────────────────

    @Test
    fun `AudioDelayRouteConfig fromJson with null returns DEFAULT`() {
        val config = com.kitsugi.animelist.core.player.AudioDelayRouteConfig.fromJson(null)
        assertEquals(com.kitsugi.animelist.core.player.AudioDelayRouteConfig.DEFAULT, config)
    }

    @Test
    fun `AudioDelayRouteConfig fromJson with empty string returns DEFAULT`() {
        val config = com.kitsugi.animelist.core.player.AudioDelayRouteConfig.fromJson("")
        assertEquals(com.kitsugi.animelist.core.player.AudioDelayRouteConfig.DEFAULT, config)
    }

    @Test
    fun `AudioDelayRouteConfig fromJson with corrupt JSON returns DEFAULT`() {
        // Should not throw; graceful fallback expected
        val config = com.kitsugi.animelist.core.player.AudioDelayRouteConfig.fromJson("{not valid json")
        assertEquals(com.kitsugi.animelist.core.player.AudioDelayRouteConfig.DEFAULT, config)
    }

    @Test
    fun `AudioDelayRouteConfig toJson then fromJson round-trip`() {
        val original = com.kitsugi.animelist.core.player.AudioDelayRouteConfig(
            speakerDelayMs   = 0L,
            bluetoothDelayMs = 150L,
            wiredDelayMs     = 20L,
            hdmiDelayMs      = 60L
        )
        val json = com.kitsugi.animelist.core.player.AudioDelayRouteConfig.toJson(original)
        val restored = com.kitsugi.animelist.core.player.AudioDelayRouteConfig.fromJson(json)
        assertEquals(original, restored)
    }

    @Test
    fun `AudioDelayRouteConfig getDelayFor BLUETOOTH returns bluetoothDelayMs`() {
        val config = com.kitsugi.animelist.core.player.AudioDelayRouteConfig(
            bluetoothDelayMs = 200L
        )
        assertEquals(200L, config.getDelayFor(com.kitsugi.animelist.core.player.AudioRoute.BLUETOOTH))
    }

    @Test
    fun `AudioDelayRouteConfig getDelayFor OTHER returns 0`() {
        val config = com.kitsugi.animelist.core.player.AudioDelayRouteConfig(
            speakerDelayMs = 999L, bluetoothDelayMs = 999L
        )
        assertEquals(0L, config.getDelayFor(com.kitsugi.animelist.core.player.AudioRoute.OTHER))
    }

    // ── DnsChoice boundary test ───────────────────────────────────────────────

    @Test
    fun `AppSettings dnsChoice 0 is system default`() {
        // 0 = System DNS – değer aralığı 0-7
        val settings = AppSettings(dnsChoice = 0)
        assertEquals(0, settings.dnsChoice)
    }

    @Test
    fun `AppSettings dnsChoice 7 is valid (Canadian Shield)`() {
        val settings = AppSettings(dnsChoice = 7)
        assertEquals(7, settings.dnsChoice)
    }
}
