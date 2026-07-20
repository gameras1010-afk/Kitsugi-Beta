package com.kitsugi.animelist.core.diagnostics

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLoggingTree {
    private const val TAG = "KitsugiDebugLog"
    private const val LOG_DIR = "diagnostics"
    private const val LOG_FILE = "kitsugi_debug.log"
    private const val MAX_LOG_BYTES = 5 * 1024 * 1024L // 5 MB

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    @Synchronized
    fun log(priority: Int, tag: String?, message: String, throwable: Throwable? = null) {
        // Log to console logcat first
        val safeTag = tag ?: "Global"
        val exceptionStr = throwable?.let { "\n${Log.getStackTraceString(it)}" } ?: ""
        
        when (priority) {
            Log.VERBOSE -> Log.v(safeTag, message, throwable)
            Log.DEBUG -> Log.d(safeTag, message, throwable)
            Log.INFO -> Log.i(safeTag, message, throwable)
            Log.WARN -> Log.w(safeTag, message, throwable)
            Log.ERROR -> Log.e(safeTag, message, throwable)
            else -> Log.d(safeTag, message, throwable)
        }

        val context = appContext ?: return
        val level = when (priority) {
            Log.VERBOSE -> "VERBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> "UNKNOWN"
        }

        val line = "[${dateFmt.format(Date())}] [$level] [$safeTag] $message$exceptionStr"

        try {
            val file = getOrCreateLogFile(context)
            rotateIfNeeded(file)
            FileWriter(file, true).use { fw ->
                PrintWriter(fw).use { pw ->
                    pw.println(line)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log to file: ${e.message}")
        }
    }

    fun v(tag: String?, message: String, throwable: Throwable? = null) = log(Log.VERBOSE, tag, message, throwable)
    fun d(tag: String?, message: String, throwable: Throwable? = null) = log(Log.DEBUG, tag, message, throwable)
    fun i(tag: String?, message: String, throwable: Throwable? = null) = log(Log.INFO, tag, message, throwable)
    fun w(tag: String?, message: String, throwable: Throwable? = null) = log(Log.WARN, tag, message, throwable)
    fun e(tag: String?, message: String, throwable: Throwable? = null) = log(Log.ERROR, tag, message, throwable)

    fun getLogFile(context: Context): File = getOrCreateLogFile(context)

    fun getLogFilePath(context: Context): String = getOrCreateLogFile(context).absolutePath

    @Synchronized
    fun clearLogs(context: Context) {
        try {
            getOrCreateLogFile(context).writeText(
                "=== Kitsugi Debug Log Dosyası Temizlendi: ${dateFmt.format(Date())} ===\n"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Log temizlenemedi: ${e.message}")
        }
    }

    private fun getOrCreateLogFile(context: Context): File {
        val dir = File(context.filesDir, LOG_DIR).also { it.mkdirs() }
        return File(dir, LOG_FILE).also { if (!it.exists()) it.createNewFile() }
    }

    private fun rotateIfNeeded(file: File) {
        if (file.length() < MAX_LOG_BYTES) return
        try {
            val lines = file.readLines()
            val keepFrom = lines.size / 2
            val kept = lines.drop(keepFrom)
            file.writeText(
                "=== Eski loglar temizlendi (boyut limiti 5MB): ${dateFmt.format(Date())} ===\n" +
                kept.joinToString("\n") + "\n"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Log rotasyonu başarısız: ${e.message}")
        }
    }
}
