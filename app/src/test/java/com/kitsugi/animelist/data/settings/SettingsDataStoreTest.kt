package com.kitsugi.animelist.data.settings

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.io.File

class SettingsDataStoreTest {

    private lateinit var mockContext: Context
    private lateinit var tempDir: File
    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        tempDir = File("temp_settings_test_dir").apply { mkdirs() }
        whenever(mockContext.filesDir).thenReturn(tempDir)
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        settingsDataStore = SettingsDataStore(mockContext)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testDefaultValues() = runBlocking {
        val settings = settingsDataStore.settingsFlow.first()
        assertFalse(settings.liveHelperEnabled)
        assertFalse(settings.enableAssExtractor)
    }

    @Test
    fun testLiveHelperEnabledRoundTrip() = runBlocking {
        // Initially false
        var settings = settingsDataStore.settingsFlow.first()
        assertFalse(settings.liveHelperEnabled)

        // Set to true
        settingsDataStore.setLiveHelperEnabled(true)
        settings = settingsDataStore.settingsFlow.first()
        assertTrue(settings.liveHelperEnabled)

        // Set to false
        settingsDataStore.setLiveHelperEnabled(false)
        settings = settingsDataStore.settingsFlow.first()
        assertFalse(settings.liveHelperEnabled)
    }

    @Test
    fun testEnableAssExtractorRoundTrip() = runBlocking {
        // Initially false
        var settings = settingsDataStore.settingsFlow.first()
        assertFalse(settings.enableAssExtractor)

        // Set to true
        settingsDataStore.setEnableAssExtractor(true)
        settings = settingsDataStore.settingsFlow.first()
        assertTrue(settings.enableAssExtractor)

        // Set to false
        settingsDataStore.setEnableAssExtractor(false)
        settings = settingsDataStore.settingsFlow.first()
        assertFalse(settings.enableAssExtractor)
    }
}
