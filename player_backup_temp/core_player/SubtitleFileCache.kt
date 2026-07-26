package com.kitsugi.animelist.core.player

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

object SubtitleFileCache {
    private const val MAX_CACHE_SIZE_BYTES = 50 * 1024 * 1024 // 50 MB

    fun getCacheDir(context: Context): File {
        val dir = File(context.filesDir, "subtitles")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getCachedFile(context: Context, url: String): File? {
        val hash = md5(url) ?: return null
        val dir = getCacheDir(context)
        val file = dir.listFiles()?.find { it.name.startsWith(hash) && it.isFile }
        if (file != null) {
            file.setLastModified(System.currentTimeMillis())
        }
        return file
    }

    suspend fun cacheSubtitle(context: Context, url: String, headers: Map<String, String>? = null): File? = withContext(Dispatchers.IO) {
        try {
            val existing = getCachedFile(context, url)
            if (existing != null && existing.length() > 0) {
                Log.d("SubtitleFileCache", "Cache hit: ${existing.name}")
                return@withContext existing
            }

            Log.d("SubtitleFileCache", "Cache miss, downloading subtitle: $url")
            val client = OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .apply {
                    headers?.forEach { (k, v) -> header(k, v) }
                }
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SubtitleFileCache", "Failed to download subtitle, HTTP ${response.code}")
                    return@withContext null
                }

                val bodyBytes = response.body?.bytes() ?: return@withContext null
                val ext = guessExtension(url, response.header("Content-Type"))
                val hash = md5(url) ?: return@withContext null
                val tempFile = File(getCacheDir(context), "${hash}_temp.$ext")
                tempFile.writeBytes(bodyBytes)

                val finalFile = File(getCacheDir(context), "$hash.$ext")
                if (tempFile.renameTo(finalFile)) {
                    Log.d("SubtitleFileCache", "Successfully cached subtitle to ${finalFile.name}")
                    runLruCleanup(context)
                    return@withContext finalFile
                } else {
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("SubtitleFileCache", "Error caching subtitle: $url", e)
        }
        null
    }

    private fun guessExtension(url: String, contentType: String?): String {
        val lowerUrl = url.lowercase()
        return when {
            lowerUrl.endsWith(".ass") || lowerUrl.endsWith(".ssa") -> "ass"
            lowerUrl.endsWith(".vtt") -> "vtt"
            lowerUrl.endsWith(".ttml") -> "ttml"
            lowerUrl.endsWith(".dfxp") -> "dfxp"
            contentType?.contains("ass", ignoreCase = true) == true -> "ass"
            contentType?.contains("vtt", ignoreCase = true) == true -> "vtt"
            contentType?.contains("ttml", ignoreCase = true) == true -> "ttml"
            else -> "srt"
        }
    }

    private fun md5(input: String): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun runLruCleanup(context: Context) {
        val dir = getCacheDir(context)
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        var totalSize = files.sumOf { it.length() }
        if (totalSize <= MAX_CACHE_SIZE_BYTES) return

        val sortedFiles = files.sortedBy { it.lastModified() }
        for (file in sortedFiles) {
            val size = file.length()
            if (file.delete()) {
                totalSize -= size
                Log.d("SubtitleFileCache", "Cleaned up cached subtitle: ${file.name}")
                if (totalSize <= MAX_CACHE_SIZE_BYTES) break
            }
        }
    }
}
