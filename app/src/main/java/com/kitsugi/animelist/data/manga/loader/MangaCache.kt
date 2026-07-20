package com.kitsugi.animelist.data.manga.loader

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Manga sayfası resimlerini diske önbellekleyen (cache) sınıf.
 *
 * Her sayfa resmi URL'nin SHA-1 hash'i ile adlandırılarak
 * `filesDir/manga_cache/` altına kaydedilir.
 * Önbellek boyutu aşıldığında en eski dosyalar silinir (LRU yaklaşımı).
 */
class MangaCache(context: Context) {

    private val cacheDir = File(context.filesDir, "manga_cache").also {
        if (!it.exists()) it.mkdirs()
    }

    companion object {
        private const val TAG          = "MangaCache"
        private const val MAX_SIZE_MB  = 150L               // Maksimum 150 MB önbellek
        private const val MAX_BYTES    = MAX_SIZE_MB * 1024 * 1024
    }

    // ─── Kontrol ─────────────────────────────────────────────────────────────

    /** Verilen [imageUrl] için önbellekte resim dosyası var mı? */
    fun isImageInCache(imageUrl: String): Boolean =
        cacheFile(imageUrl).exists()

    // ─── Okuma ───────────────────────────────────────────────────────────────

    /**
     * Önbellekteki resim dosyasını döndürür.
     * Bulunamazsa null döner.
     */
    fun getImageFile(imageUrl: String): File {
        val file = cacheFile(imageUrl)
        // Son erişim zamanını güncelle (LRU için)
        file.setLastModified(System.currentTimeMillis())
        return file
    }

    // ─── Yazma ───────────────────────────────────────────────────────────────

    /**
     * [stream] içinden okunan veriyi önbelleğe yazar.
     * Yazma işlemi atomik olarak gerçekleşir (önce geçici dosyaya yazılır).
     */
    fun putImageToCache(imageUrl: String, stream: InputStream) {
        val target = cacheFile(imageUrl)
        val temp   = File(cacheDir, "${target.name}.tmp")
        try {
            FileOutputStream(temp).use { out -> stream.copyTo(out) }
            if (target.exists()) target.delete()
            temp.renameTo(target)
            Log.v(TAG, "Önbelleğe kaydedildi: ${target.name} (${target.length()} bytes)")
            trimIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Önbellek yazma hatası [$imageUrl]: ${e.message}")
            if (temp.exists()) temp.delete()
        }
    }

    // ─── Temizleme ────────────────────────────────────────────────────────────

    /** Önbellek boyutu MAX_BYTES'ı aşarsa en eski dosyaları siler. */
    private fun trimIfNeeded() {
        val files = cacheDir.listFiles()?.toMutableList() ?: return
        var totalSize = files.sumOf { it.length() }
        if (totalSize <= MAX_BYTES) return

        // En eskiden en yeniye sırala
        files.sortBy { it.lastModified() }
        for (file in files) {
            if (totalSize <= MAX_BYTES * 0.85) break   // %85'e düş
            val size = file.length()
            if (file.delete()) {
                totalSize -= size
                Log.v(TAG, "Önbellek kırpıldı: ${file.name}")
            }
        }
    }

    /** Tüm önbelleği temizler. */
    fun clearAll() {
        cacheDir.listFiles()?.forEach { it.delete() }
        Log.i(TAG, "Önbellek tamamen temizlendi")
    }

    /** Önbelleğin o anki boyutunu MB cinsinden döndürür. */
    fun currentSizeMb(): Float =
        (cacheDir.listFiles()?.sumOf { it.length() } ?: 0L)
            .toFloat() / (1024f * 1024f)

    // ─── Yardımcılar ─────────────────────────────────────────────────────────

    // F8: Dosya uzantısı ekle — Android baz\u0131 JPEG dosyalar\u0131n\u0131 uzants\u0131z okuyamayabilir
    private fun cacheFile(imageUrl: String): File {
        val hash = sha1(imageUrl)
        val ext = imageUrl.substringBefore("?").substringAfterLast(".").lowercase()
            .takeIf { it.length in 2..4 && it in setOf("jpg", "jpeg", "png", "webp", "gif", "avif") }
            ?: "jpg"
        return File(cacheDir, "$hash.$ext")
    }

    private fun sha1(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-1")
        return digest.digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
