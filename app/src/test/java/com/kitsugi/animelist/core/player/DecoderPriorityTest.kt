package com.kitsugi.animelist.core.player

import com.kitsugi.animelist.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class DecoderPriorityTest {

    @Test
    fun testDecoderPriorityDefaults() {
        val settings = AppSettings()
        // Default decoder priority should be 0 (Hardware preferred)
        assertEquals(0, settings.decoderPriority)
    }

    @Test
    fun testDecoderPriorityCustomValue() {
        val settings = AppSettings(decoderPriority = 2)
        // Verify custom decoder priority is set correctly
        assertEquals(2, settings.decoderPriority)
    }
}
