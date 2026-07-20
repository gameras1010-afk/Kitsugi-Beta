package com.kitsugi.animelist.core.player

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Kitsugi Player — Oynatma Kayıt Sistemi
 *
 * Tüm oynatma olaylarını (başarılı oynatma, hata, kaynak değiştirme)
 * dosyaya yazar. Log dosyası: [context.filesDir]/player_logs/Kitsugi_player.log
 *
 * Kullanım:
 *   PlayerLogger.logPlaybackStart(context, url, addonName, title)
 *   PlayerLogger.logPlaybackError(context, url, addonName, error)
 *   PlayerLogger.logSourceChange(context, ...)
 *   PlayerLogger.getLogFile(context)  ← log dosyasını al
 */
object PlayerLogger {

    private const val TAG = "KitsugiPlayerLog"
    private const val LOG_DIR  = "player_logs"
    private const val LOG_FILE = "Kitsugi_player.log"

    /** Maksimum log dosyası boyutu: 2 MB — aşılırsa eski yarısı silinir. */
    private const val MAX_LOG_BYTES = 2 * 1024 * 1024L

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // ── Genel Amaçlı Yazıcı ──────────────────────────────────────────────────

    private fun write(context: Context, level: String, tag: String, message: String) {
        val line = "[${dateFmt.format(Date())}] [$level] [$tag] $message"
        Log.d(TAG, line)
        val priority = when (level) {
            "INFO" -> Log.INFO
            "WARN" -> Log.WARN
            "ERROR" -> Log.ERROR
            else -> Log.DEBUG
        }
        com.kitsugi.animelist.core.diagnostics.FileLoggingTree.log(priority, tag, message)
        try {
            val logFile = getOrCreateLogFile(context)
            rotatIfNeeded(logFile)
            FileWriter(logFile, /* append= */ true).use { fw ->
                PrintWriter(fw).use { pw -> pw.println(line) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Log dosyasına yazılamadı: ${e.message}")
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Video oynatma başlangıcını kayıt altına alır.
     *
     * @param url       Oynatılan video URL'si (ilk 120 karakter)
     * @param addonName Kaynağın addon/eklenti adı (örn. "TurkAnime", "TorBox")
     * @param title     Anime başlığı ve bölüm bilgisi
     * @param isCS      CloudStream eklentisinden mi geliyor?
     */
    fun logPlaybackStart(
        context: Context,
        url: String?,
        addonName: String?,
        title: String?,
        isCS: Boolean = false
    ) {
        val safeUrl  = url?.take(120) ?: "null"
        val src      = addonName ?: "bilinmeyen"
        val srcType  = if (isCS) "CS3" else "Stremio"
        write(context, "INFO", "OYNAT",
            "Başladı | Kaynak=[$srcType] $src | Başlık=$title | URL=$safeUrl")
    }

    /**
     * ExoPlayer oynatma hatasını kayıt altına alır.
     *
     * @param url         Hata veren URL
     * @param addonName   Kaynağın eklenti adı
     * @param errorCode   ExoPlayer hata kodu (PlaybackException.errorCode)
     * @param errorMsg    Hata mesajı
     * @param cause       Root cause (opsiyonel)
     */
    fun logPlaybackError(
        context: Context,
        url: String?,
        addonName: String?,
        title: String?,
        errorCode: Int,
        errorMsg: String?,
        cause: Throwable? = null
    ) {
        val safeUrl = url?.take(120) ?: "null"
        val src     = addonName ?: "bilinmeyen"
        val causeStr = cause?.let { " | Neden=${it.javaClass.simpleName}: ${it.message?.take(80)}" } ?: ""
        write(context, "ERROR", "OYNAT_HATA",
            "HATA | Kaynak=$src | Başlık=$title | Kod=$errorCode | Mesaj=$errorMsg$causeStr | URL=$safeUrl")
    }

    /**
     * Kaynak (stream source) değişikliğini kayıt altına alır.
     */
    fun logSourceChange(
        context: Context,
        fromAddon: String?,
        toAddon: String?,
        newUrl: String?,
        title: String?
    ) {
        val safeUrl = newUrl?.take(120) ?: "null"
        write(context, "INFO", "KAYNAK_DEĞİŞ",
            "$fromAddon → $toAddon | Başlık=$title | URL=$safeUrl")
    }

    /**
     * Harici oynatıcıya manuel geçişi kayıt altına alır.
     */
    fun logExternalPlayerLaunch(
        context: Context,
        url: String?,
        addonName: String?,
        title: String?,
        manual: Boolean
    ) {
        val safeUrl = url?.take(120) ?: "null"
        val typ     = if (manual) "MANUEL" else "OTOMATİK"
        write(context, "WARN", "HARİCİ_OYNATICI",
            "$typ açıldı | Kaynak=$addonName | Başlık=$title | URL=$safeUrl")
    }

    /**
     * Genel bilgi mesajı yazar.
     */
    fun logInfo(context: Context, tag: String, message: String) {
        write(context, "INFO", tag, message)
    }

    /**
     * Genel uyarı mesajı yazar.
     */
    fun logWarn(context: Context, tag: String, message: String) {
        write(context, "WARN", tag, message)
    }

    // ── Dosya Yardımcıları ────────────────────────────────────────────────────

    /** Log dosyasını döndürür (henüz yoksa oluşturur). */
    fun getLogFile(context: Context): File = getOrCreateLogFile(context)

    /** Log dosyasının tam yolunu string olarak döndürür. */
    fun getLogFilePath(context: Context): String = getOrCreateLogFile(context).absolutePath

    /** Log dosyasını tamamen siler ve yeniden oluşturur. */
    fun clearLogs(context: Context) {
        try {
            getOrCreateLogFile(context).writeText(
                "=== Kitsugi Player Log Dosyası Temizlendi: ${dateFmt.format(Date())} ===\n"
            )
            Log.i(TAG, "Log dosyası temizlendi.")
        } catch (e: Exception) {
            Log.e(TAG, "Log temizlenemedi: ${e.message}")
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun getOrCreateLogFile(context: Context): File {
        val dir = File(context.filesDir, LOG_DIR).also { it.mkdirs() }
        return File(dir, LOG_FILE).also { if (!it.exists()) it.createNewFile() }
    }

    /**
     * Dosya boyutu MAX_LOG_BYTES'ı aşarsa son yarısını tutar, eskisini siler.
     * Bu sayede dosya süresiz büyümez.
     */
    private fun rotatIfNeeded(file: File) {
        if (file.length() < MAX_LOG_BYTES) return
        try {
            val lines = file.readLines()
            val keepFrom = lines.size / 2
            val kept = lines.drop(keepFrom)
            file.writeText(
                "=== Eski loglar temizlendi (boyut limiti): ${dateFmt.format(Date())} ===\n" +
                kept.joinToString("\n") + "\n"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Log rotasyonu başarısız: ${e.message}")
        }
    }
}
