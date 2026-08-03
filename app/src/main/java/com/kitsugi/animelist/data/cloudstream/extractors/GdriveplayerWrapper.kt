package com.kitsugi.animelist.data.cloudstream.extractors

import android.util.Log

/**
 * FP-29 – Gdriveplayer extractor wrapper.
 *
 * Gdriveplayer, Google Drive videolarını bir proxy embed player üzerinden sunar.
 * Doğrudan Drive API kullanarak video kalite URL'lerini döndürür.
 *
 * Desteklenen formatlar:
 *   - gdriveplayer.me/player?id=<driveId>
 *   - drive.google.com/file/d/<driveId>/view
 *   - drive.google.com/uc?id=<driveId>
 */
object GdriveplayerWrapper {
    private const val TAG = "GdriveplayerWrapper"

    data class ExtractedVideo(
        val url: String,
        val label: String,
        val isM3u8: Boolean,
        val headers: Map<String, String> = emptyMap()
    )

    /**
     * URL'den Google Drive dosya ID'sini çıkarır.
     * Örnek: https://drive.google.com/file/d/ABC123/view → ABC123
     */
    fun extractDriveId(url: String): String? {
        return Regex("""/file/d/([a-zA-Z0-9_-]+)""").find(url)?.groupValues?.get(1)
            ?: Regex("""[?&]id=([a-zA-Z0-9_-]+)""").find(url)?.groupValues?.get(1)
    }

    /**
     * Google Drive video için doğrudan streaming URL'i oluşturur.
     * Küçük dosyalar için confirm token gerekmeden çalışır.
     *
     * @param driveId Google Drive dosya ID'si
     */
    fun buildDriveStreamUrl(driveId: String): String {
        return "https://drive.google.com/uc?export=download&id=$driveId"
    }

    /**
     * Gdriveplayer veya Drive embed sayfasından stream URL'lerini çıkarır.
     *
     * @param pageContent HTML sayfasının içeriği
     * @param embedUrl Embed URL'si
     */
    fun extractFromHtml(pageContent: String, embedUrl: String): List<ExtractedVideo> {
        val videos = mutableListOf<ExtractedVideo>()
        try {
            val driveHeaders = mapOf(
                "Referer"    to "https://drive.google.com/",
                "User-Agent" to "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36"
            )

            // Yöntem 1: URL'den drive ID çıkar ve stream URL oluştur
            val driveId = extractDriveId(embedUrl)
            if (!driveId.isNullOrBlank()) {
                val streamUrl = buildDriveStreamUrl(driveId)
                videos.add(ExtractedVideo(streamUrl, "Google Drive", false, driveHeaders))
                Log.d(TAG, "Gdriveplayer drive ID bulundu: $driveId → $streamUrl")
            }

            // Yöntem 2: HTML içinde doğrudan drive URL veya video URL
            if (videos.isEmpty()) {
                val driveUrlRegex = Regex("""["'](https://[^"']*(?:googleusercontent|googlevideo|drive\.google)[^"']+)["']""")
                driveUrlRegex.findAll(pageContent).forEach { match ->
                    val url = match.groupValues[1]
                    if (!url.contains(".png") && !url.contains(".jpg") && !url.contains("thumbnail")) {
                        val isM3u8 = url.contains(".m3u8")
                        videos.add(ExtractedVideo(url, "Google Drive", isM3u8, driveHeaders))
                        Log.d(TAG, "Gdriveplayer HTML URL: $url")
                    }
                }
            }

            // Yöntem 3: sources array içinde
            if (videos.isEmpty()) {
                val fileRegex = Regex(""""file"\s*:\s*"([^"]+(?:\.m3u8|\.mp4)[^"]*)"""")
                fileRegex.findAll(pageContent).forEach { match ->
                    val url = match.groupValues[1]
                    if (url.startsWith("http")) {
                        val isM3u8 = url.contains(".m3u8")
                        videos.add(ExtractedVideo(url, "GDrive", isM3u8, driveHeaders))
                    }
                }
            }

            Log.d(TAG, "Gdriveplayer extraction: ${videos.size} video")
        } catch (e: Exception) {
            Log.e(TAG, "Gdriveplayer extraction hatası: ${e.message}")
        }
        return videos
    }

    fun isGdriveplayerUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("gdriveplayer") ||
               lower.contains("drive.google.com") ||
               lower.contains("googleusercontent.com") ||
               lower.contains("googlevideo.com")
    }
}
