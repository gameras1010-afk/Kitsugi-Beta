package com.kitsugi.animelist.core.player.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kitsugi.animelist.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Media3PlayerEngineTest {

    private lateinit var context: Context
    private lateinit var settings: AppSettings
    private lateinit var engine: Media3PlayerEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        settings = AppSettings()
        
        // Media3PlayerEngine can only be instantiated on the main/looper thread
        // since it internally creates a Handler and instantiates ExoPlayer.
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            engine = Media3PlayerEngine(context, settings)
        }
    }

    @Test
    fun testInitialState() {
        assertEquals(PlayerEngineType.MEDIA3, engine.engineType)
        assertEquals(PlayerEngine.State.IDLE, engine.currentState)
    }

    @Test
    fun testEngineProperties() {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertEquals(0L, engine.currentPosition)
            assertEquals(0L, engine.duration)
            assertEquals(1.0f, engine.currentSpeed, 0.01f)
            assertEquals(1.0f, engine.currentVolume, 0.01f)
        }
    }

    @Test
    fun testCreateVideoView() {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = engine.createVideoView(context)
            assertNotNull(view)
        }
    }
}
