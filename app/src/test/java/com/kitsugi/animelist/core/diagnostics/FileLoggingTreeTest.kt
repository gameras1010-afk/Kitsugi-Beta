package com.kitsugi.animelist.core.diagnostics

import android.content.Context
import android.util.Log
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.io.File

class FileLoggingTreeTest {

    private lateinit var mockContext: Context
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        tempDir = File("temp_file_logging_test_dir").apply { mkdirs() }
        whenever(mockContext.filesDir).thenReturn(tempDir)
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        FileLoggingTree.init(mockContext)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `test file log writing`() {
        FileLoggingTree.d("TestTag", "Hello logging world!")
        val logFile = FileLoggingTree.getLogFile(mockContext)
        assertTrue(logFile.exists())
        val text = logFile.readText()
        assertTrue(text.contains("[DEBUG] [TestTag] Hello logging world!"))
    }

    @Test
    fun `test clear logs`() {
        FileLoggingTree.i("TestTag", "Initial line")
        FileLoggingTree.clearLogs(mockContext)
        val text = FileLoggingTree.getLogFile(mockContext).readText()
        assertTrue(text.contains("Kitsugi Debug Log Dosyası Temizlendi"))
        assertFalse(text.contains("Initial line"))
    }

    @Test
    fun `test log rotation occurs when limit reached`() {
        // Create a large file to trigger rotation
        val logFile = FileLoggingTree.getLogFile(mockContext)
        // 5MB limit. We write a line, and trigger rotation
        // To force rotation quickly, we can fill the log file manually
        // We write 6MB of data
        val dummyData = "A".repeat(1024) // 1KB
        logFile.bufferedWriter().use { writer ->
            repeat(6000) { // 6MB
                writer.write(dummyData)
                writer.newLine()
            }
        }
        
        // Log one more message, which should trigger rotation
        FileLoggingTree.w("TestTag", "Rotated warning message")
        
        // The file size should now be reduced (approx half or less)
        assertTrue(logFile.length() < 5 * 1024 * 1024L)
        val text = logFile.readText()
        assertTrue(text.contains("Eski loglar temizlendi (boyut limiti 5MB)"))
        assertTrue(text.contains("[WARN] [TestTag] Rotated warning message"))
    }
}
