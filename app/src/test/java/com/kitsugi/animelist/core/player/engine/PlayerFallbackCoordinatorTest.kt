package com.kitsugi.animelist.core.player.engine

import com.kitsugi.animelist.core.player.PlayerManagerListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

class PlayerFallbackCoordinatorTest {

    @Test
    fun testFallbackChainMpvEnabled() {
        val listener = mock<PlayerManagerListener>()
        val coordinator = PlayerFallbackCoordinator(maxAttempts = 3, listener = listener)

        // 1. step: MEDIA3 -> MPV
        val next1 = coordinator.getFallbackEngine(
            currentEngine = PlayerEngineType.MEDIA3,
            errorCode = 1001,
            mpvEnabled = true
        )
        assertEquals(PlayerEngineType.MPV, next1)
        verify(listener).onPlayerSwitched(PlayerEngineType.MEDIA3, PlayerEngineType.MPV)

        // 2. step: MPV -> EXTERNAL
        val next2 = coordinator.getFallbackEngine(
            currentEngine = PlayerEngineType.MPV,
            errorCode = 1002,
            mpvEnabled = true
        )
        assertEquals(PlayerEngineType.EXTERNAL, next2)
        verify(listener).onPlayerSwitched(PlayerEngineType.MPV, PlayerEngineType.EXTERNAL)

        // 3. step: EXTERNAL -> null (chain exhausted)
        val next3 = coordinator.getFallbackEngine(
            currentEngine = PlayerEngineType.EXTERNAL,
            errorCode = 1003,
            mpvEnabled = true
        )
        assertNull(next3)
        verify(listener).onFatalError(
            errorCode = 1003,
            errorMsg = "Fallback zinciri tükendi (son motor: EXTERNAL). Hata kodu: 1003"
        )
    }

    @Test
    fun testFallbackChainMpvDisabled() {
        val listener = mock<PlayerManagerListener>()
        val coordinator = PlayerFallbackCoordinator(maxAttempts = 3, listener = listener)

        // 1. step: MEDIA3 -> EXTERNAL (MPV is disabled)
        val next1 = coordinator.getFallbackEngine(
            currentEngine = PlayerEngineType.MEDIA3,
            errorCode = 1001,
            mpvEnabled = false
        )
        assertEquals(PlayerEngineType.EXTERNAL, next1)
        verify(listener).onPlayerSwitched(PlayerEngineType.MEDIA3, PlayerEngineType.EXTERNAL)

        // 2. step: EXTERNAL -> null
        val next2 = coordinator.getFallbackEngine(
            currentEngine = PlayerEngineType.EXTERNAL,
            errorCode = 1002,
            mpvEnabled = false
        )
        assertNull(next2)
        verify(listener).onFatalError(
            errorCode = 1002,
            errorMsg = "Fallback zinciri tükendi (son motor: EXTERNAL). Hata kodu: 1002"
        )
    }

    @Test
    fun testMaxAttemptsLimit() {
        val listener = mock<PlayerManagerListener>()
        val coordinator = PlayerFallbackCoordinator(maxAttempts = 2, listener = listener)

        // 1. attempt
        val next1 = coordinator.getFallbackEngine(PlayerEngineType.MEDIA3, 1001, mpvEnabled = true)
        assertEquals(PlayerEngineType.MPV, next1)
        assertEquals(1, coordinator.attemptCount)

        // 2. attempt
        val next2 = coordinator.getFallbackEngine(PlayerEngineType.MPV, 1002, mpvEnabled = true)
        assertEquals(PlayerEngineType.EXTERNAL, next2)
        assertEquals(2, coordinator.attemptCount)

        // 3. attempt -> should fail immediately due to maxAttempts limit
        val next3 = coordinator.getFallbackEngine(PlayerEngineType.EXTERNAL, 1003, mpvEnabled = true)
        assertNull(next3)
        assertEquals(2, coordinator.attemptCount) // remains at 2
        verify(listener).onFatalError(
            errorCode = 1003,
            errorMsg = "Tüm dahili motorlar (2 deneme) başarısız oldu. Hata kodu: 1003"
        )
    }

    @Test
    fun testReset() {
        val coordinator = PlayerFallbackCoordinator(maxAttempts = 3)
        coordinator.getFallbackEngine(PlayerEngineType.MEDIA3, 1001, mpvEnabled = true)
        assertEquals(1, coordinator.attemptCount)

        coordinator.reset()
        assertEquals(0, coordinator.attemptCount)
    }

    @Test
    fun testStaticCompanionFallback() {
        val nextWithMpv = PlayerFallbackCoordinator.nextEngine(PlayerEngineType.MEDIA3, mpvEnabled = true)
        assertEquals(PlayerEngineType.MPV, nextWithMpv)

        val nextNoMpv = PlayerFallbackCoordinator.nextEngine(PlayerEngineType.MEDIA3, mpvEnabled = false)
        assertEquals(PlayerEngineType.EXTERNAL, nextNoMpv)

        val nextMpv = PlayerFallbackCoordinator.nextEngine(PlayerEngineType.MPV, mpvEnabled = true)
        assertEquals(PlayerEngineType.EXTERNAL, nextMpv)

        val nextExternal = PlayerFallbackCoordinator.nextEngine(PlayerEngineType.EXTERNAL, mpvEnabled = true)
        assertNull(nextExternal)
    }
}
