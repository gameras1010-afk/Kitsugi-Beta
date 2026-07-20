package com.kitsugi.animelist.core.player

import com.kitsugi.animelist.core.player.engine.PlayerEngineSelector
import com.kitsugi.animelist.core.player.engine.PlayerEngineType
import com.kitsugi.animelist.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerEngineSelectorTest {

    @Test
    fun testSelectEngineMpv() {
        val settings = AppSettings(playerPreference = "MPV")
        val engineType = PlayerEngineSelector.selectEngine(settings, "https://example.com/video.mp4")
        assertEquals(PlayerEngineType.MPV, engineType)
    }

    @Test
    fun testSelectEngineMedia3() {
        val settings = AppSettings(playerPreference = "MEDIA3")
        val engineType = PlayerEngineSelector.selectEngine(settings, "https://example.com/video.mp4")
        assertEquals(PlayerEngineType.MEDIA3, engineType)
    }

    @Test
    fun testSelectEngineExternal() {
        val settings = AppSettings(playerPreference = "EXTERNAL")
        val engineType = PlayerEngineSelector.selectEngine(settings, "https://example.com/video.mp4")
        assertEquals(PlayerEngineType.EXTERNAL, engineType)
    }
}
