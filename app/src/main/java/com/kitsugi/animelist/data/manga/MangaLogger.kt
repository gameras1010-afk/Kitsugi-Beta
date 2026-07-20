package com.kitsugi.animelist.data.manga

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Kitsugi Manga — Kaynak Hata ve İşlem Kayıt Sistemi
 *
 * PlayerLogger ile aynı yapıda; manga arama, detay, bölüm listesi,
 * sayfa listesi ve görsel indirme olaylarını dosyaya kaydeder.
 *
 * Log dosyası: [context.filesDir]/manga_logs/Kitsugi_manga.log
 * Maksimum boyut: 3 MB — aşılırsa eski yarısı otomatik silinir.
 *
 * Logcat tag: KitsugiMangaLog
 *
 * Kullanım örneği:
 *   MangaLogger.logSearch(context, "Sadscans", "attack on titan", success=true, resultCount=12)
 *   MangaLogger.logPageFetch(context, "MangaTR", chapterName, error=e)
 */
object MangaLogger {

    private const val TAG      = "KitsugiMangaLog"
    private const val LOG_DIR  = "manga_logs"
    private const val LOG_FILE = "Kitsugi_manga.log"

    /** Maksimum log dosyası boyutu: 3 MB */
    private const val MAX_LOG_BYTES = 3 * 1024 * 1024L

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // ── Genel Yazıcı ─────────────────────────────────────────────────────────

    private fun write(context: Context, level: String, tag: String, message: String) {
        val line = "[${dateFmt.format(Date())}] [$level] [$tag] $message"
        Log.d(TAG, line)
        try {
            val logFile = getOrCreateLogFile(context)
            rotateIfNeeded(logFile)
            FileWriter(logFile, /* append= */ true).use { fw ->
                PrintWriter(fw).use { pw -> pw.println(line) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Manga log dosyasına yazılamadı: ${e.message}")
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Arama sonucunu kaydeder.
     *
     * @param sourceName   Manga kaynağının adı (ör. "Sadscans", "MangaTR")
     * @param query        Aranan metin
     * @param success      Başarılı mı?
     * @param resultCount  Kaç sonuç döndü?
     * @param elapsedMs    İstek süresi (ms)
     * @param error        Hata nesnesi (başarısızsa)
     */
    fun logSearch(
        context: Context,
        sourceName: String,
        query: String,
        success: Boolean,
        resultCount: Int = 0,
        elapsedMs: Long = 0,
        error: Throwable? = null,
    ) {
        if (success) {
            write(context, "INFO", "ARAMA",
                "✅ Kaynak=$sourceName | Sorgu=\"$query\" | Sonuç=$resultCount | Süre=${elapsedMs}ms")
        } else {
            val errStr = formatError(error)
            write(context, "ERROR", "ARAMA_HATA",
                "❌ Kaynak=$sourceName | Sorgu=\"$query\" | Süre=${elapsedMs}ms | $errStr")
        }
    }

    /**
     * Manga detayı çekme sonucunu kaydeder.
     *
     * @param sourceName  Manga kaynağının adı
     * @param mangaUrl    Manga URL'si
     * @param success     Başarılı mı?
     * @param error       Hata nesnesi (başarısızsa)
     */
    fun logMangaDetails(
        context: Context,
        sourceName: String,
        mangaUrl: String,
        success: Boolean,
        error: Throwable? = null,
    ) {
        val safeUrl = mangaUrl.take(100)
        if (success) {
            write(context, "INFO", "DETAY",
                "✅ Kaynak=$sourceName | URL=$safeUrl")
        } else {
            val errStr = formatError(error)
            write(context, "ERROR", "DETAY_HATA",
                "❌ Kaynak=$sourceName | URL=$safeUrl | $errStr")
        }
    }

    /**
     * Bölüm listesi çekme sonucunu kaydeder.
     *
     * @param sourceName    Manga kaynağının adı
     * @param mangaUrl      Manga URL'si
     * @param success       Başarılı mı?
     * @param chapterCount  Kaç bölüm döndü?
     * @param elapsedMs     İstek süresi (ms)
     * @param error         Hata nesnesi (başarısızsa)
     */
    fun logChapterList(
        context: Context,
        sourceName: String,
        mangaUrl: String,
        success: Boolean,
        chapterCount: Int = 0,
        elapsedMs: Long = 0,
        error: Throwable? = null,
    ) {
        val safeUrl = mangaUrl.take(100)
        if (success) {
            write(context, "INFO", "BÖLÜM_LİSTE",
                "✅ Kaynak=$sourceName | Bölüm=$chapterCount | Süre=${elapsedMs}ms | URL=$safeUrl")
        } else {
            val errStr = formatError(error)
            write(context, "ERROR", "BÖLÜM_HATA",
                "❌ Kaynak=$sourceName | Süre=${elapsedMs}ms | URL=$safeUrl | $errStr")
        }
    }

    /**
     * Sayfa listesi (pageList) yükleme sonucunu kaydeder.
     *
     * @param sourceName   Manga kaynağının adı
     * @param chapterName  Bölüm adı
     * @param success      Başarılı mı?
     * @param pageCount    Kaç sayfa döndü?
     * @param elapsedMs    İstek süresi (ms)
     * @param error        Hata nesnesi (başarısızsa)
     */
    fun logPageList(
        context: Context,
        sourceName: String,
        chapterName: String,
        success: Boolean,
        pageCount: Int = 0,
        elapsedMs: Long = 0,
        error: Throwable? = null,
    ) {
        if (success) {
            write(context, "INFO", "SAYFA_LİSTE",
                "✅ Kaynak=$sourceName | Bölüm=$chapterName | Sayfa=$pageCount | Süre=${elapsedMs}ms")
        } else {
            val errStr = formatError(error)
            write(context, "ERROR", "SAYFA_HATA",
                "❌ Kaynak=$sourceName | Bölüm=$chapterName | Süre=${elapsedMs}ms | $errStr")
        }
    }

    /**
     * Sayfa görseli indirme sonucunu kaydeder.
     *
     * @param sourceName  Manga kaynağının adı
     * @param pageIndex   Sayfa indeksi
     * @param imageUrl    Görsel URL'si
     * @param success     Başarılı mı?
     * @param elapsedMs   İndirme süresi (ms)
     * @param error       Hata nesnesi (başarısızsa)
     * @param isTimeout   Timeout mu oldu?
     */
    fun logImageFetch(
        context: Context,
        sourceName: String,
        pageIndex: Int,
        imageUrl: String?,
        success: Boolean,
        elapsedMs: Long = 0,
        error: Throwable? = null,
        isTimeout: Boolean = false,
    ) {
        val safeUrl = imageUrl?.take(100) ?: "null"
        if (success) {
            write(context, "INFO", "GÖRSEL",
                "✅ Kaynak=$sourceName | Sayfa=${pageIndex + 1} | Süre=${elapsedMs}ms | URL=$safeUrl")
        } else if (isTimeout) {
            write(context, "WARN", "GÖRSEL_TIMEOUT",
                "⏱ Kaynak=$sourceName | Sayfa=${pageIndex + 1} | Zaman aşımı | URL=$safeUrl")
        } else {
            val errStr = formatError(error)
            write(context, "ERROR", "GÖRSEL_HATA",
                "❌ Kaynak=$sourceName | Sayfa=${pageIndex + 1} | URL=$safeUrl | $errStr")
        }
    }

    /**
     * Popüler manga listesi çekme sonucunu kaydeder.
     */
    fun logPopular(
        context: Context,
        sourceName: String,
        success: Boolean,
        resultCount: Int = 0,
        elapsedMs: Long = 0,
        error: Throwable? = null,
    ) {
        if (success) {
            write(context, "INFO", "POPÜLER",
                "✅ Kaynak=$sourceName | Sonuç=$resultCount | Süre=${elapsedMs}ms")
        } else {
            val errStr = formatError(error)
            write(context, "ERROR", "POPÜLER_HATA",
                "❌ Kaynak=$sourceName | Süre=${elapsedMs}ms | $errStr")
        }
    }

    /**
     * Captcha / Cloudflare engeli tespitini kaydeder.
     */
    fun logCaptchaDetected(
        context: Context,
        sourceName: String,
        operation: String,
        detail: String? = null,
    ) {
        write(context, "WARN", "CAPTCHA",
            "🔒 Kaynak=$sourceName | İşlem=$operation | Detay=${detail ?: "Cloudflare/WAF engeli"}")
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

    /** Log dosyasını döndürür (yoksa oluşturur). */
    fun getLogFile(context: Context): File = getOrCreateLogFile(context)

    /** Log dosyasının tam yolunu string olarak döndürür. */
    fun getLogFilePath(context: Context): String = getOrCreateLogFile(context).absolutePath

    /**
     * Log dosyasını temizler ve başlangıç satırı yazar.
     */
    fun clearLogs(context: Context) {
        try {
            getOrCreateLogFile(context).writeText(
                "=== Kitsugi Manga Log Dosyası Temizlendi: ${dateFmt.format(Date())} ===\n"
            )
            Log.i(TAG, "Manga log dosyası temizlendi.")
        } catch (e: Exception) {
            Log.e(TAG, "Manga log temizlenemedi: ${e.message}")
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun getOrCreateLogFile(context: Context): File {
        val dir = File(context.filesDir, LOG_DIR).also { it.mkdirs() }
        return File(dir, LOG_FILE).also { if (!it.exists()) it.createNewFile() }
    }

    /**
     * Dosya MAX_LOG_BYTES'ı aşarsa son yarısını tutar, eskisini siler.
     */
    private fun rotateIfNeeded(file: File) {
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
            Log.e(TAG, "Manga log rotasyonu başarısız: ${e.message}")
        }
    }

    /**
     * Hata nesnesinden anlamlı bir string üretir.
     * HTTP kodu, timeout, captcha gibi bilinen hata türlerini öne çıkarır.
     */
    private fun formatError(error: Throwable?): String {
        if (error == null) return "Hata=bilinmiyor"
        val msg = error.message ?: ""
        val type = error.javaClass.simpleName

        // HTTP kodu varsa yakala (ör. "HTTP error 403 for https://...")
        val httpMatch = Regex("HTTP\\s+(?:error\\s+)?(\\d{3})").find(msg)
        if (httpMatch != null) {
            val code = httpMatch.groupValues[1]
            val hint = when (code) {
                "403" -> " (Erişim yasak — User-Agent veya Cloudflare engeli)"
                "429" -> " (Rate limit — çok fazla istek)"
                "503" -> " (Sunucu geçici kapalı)"
                "404" -> " (Sayfa bulunamadı — bozuk URL)"
                "401" -> " (Kimlik doğrulama gerekli)"
                else  -> ""
            }
            return "HTTP=$code$hint | Tip=$type"
        }

        // Timeout
        if (type.contains("Timeout") || msg.contains("timed out", ignoreCase = true)) {
            return "Hata=TIMEOUT | Mesaj=${msg.take(80)}"
        }

        return "Hata=$type | Mesaj=${msg.take(100)}"
    }
}
