package com.kitsugi.animelist.core.player.engine

import android.content.Context
import com.kitsugi.animelist.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.any

class MpvPlayerEngineTest {

    private lateinit var mockContext: Context
    private lateinit var mockSettings: AppSettings
    private lateinit var engine: MpvPlayerEngine

    @Before
    fun setUp() {
        mockContext = mock()
        mockSettings = mock()
        engine = MpvPlayerEngine(mockContext, mockSettings)
    }

    @Test
    fun testInitialProperties() {
        assertEquals(PlayerEngineType.MPV, engine.engineType)
        assertEquals(PlayerEngine.State.IDLE, engine.currentState)
        assertEquals(0L, engine.currentPosition)
        assertEquals(0L, engine.duration)
        assertFalse(engine.isPlaying)
        assertEquals(1.0f, engine.currentSpeed, 0.01f)
        assertEquals(1.0f, engine.currentVolume, 0.01f)
    }

    @Test
    fun testEventPropertyPause() {
        val listener = mock<PlayerEngine.Listener>()
        engine.addListener(listener)

        // pause = true -> isPlaying should become false
        engine.eventProperty("pause", true)
        assertFalse(engine.isPlaying)
        assertEquals(PlayerEngine.State.READY, engine.currentState)
        verify(listener).onStateChanged(PlayerEngine.State.READY)

        // pause = false -> isPlaying should become true
        engine.eventProperty("pause", false)
        assertTrue(engine.isPlaying)
    }

    @Test
    fun testEventPropertyTimePosAndDuration() {
        val listener = mock<PlayerEngine.Listener>()
        engine.addListener(listener)

        // time-pos = 45.2 seconds -> 45200 ms
        engine.eventProperty("time-pos", 45.2)
        assertEquals(45200L, engine.currentPosition)
        verify(listener).onPositionChanged(45200L, 0L)

        // duration = 600.0 seconds -> 600000 ms
        engine.eventProperty("duration", 600.0)
        assertEquals(600000L, engine.duration)
        verify(listener).onPositionChanged(45200L, 600000L)
    }

    @Test
    fun testEventPropertySpeedAndVolume() {
        // speed = 1.5
        engine.eventProperty("speed", 1.5)
        assertEquals(1.5f, engine.currentSpeed, 0.01f)

        // volume = 50.0 -> 0.5f
        engine.eventProperty("volume", 50.0)
        assertEquals(0.5f, engine.currentVolume, 0.01f)
    }

    @Test
    fun testEventPropertySubVisibility() {
        // sub-visibility = false -> isSubtitleDisabled should become true
        engine.eventProperty("sub-visibility", false)
        assertTrue(engine.isSubtitleDisabled)

        // sub-visibility = true -> isSubtitleDisabled should become false
        engine.eventProperty("sub-visibility", true)
        assertFalse(engine.isSubtitleDisabled)
    }
}
