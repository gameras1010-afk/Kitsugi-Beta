package com.kitsugi.animelist.data.cloudstream.extractors

import android.util.Log

/**
 * FP-36 – StreamTape (streamtape.com) extractor wrapper.
 *
 * StreamTape, stream URL'sini iki parçada JS ile gizler:
 *   1. id= kısmı: robotlink div'inin textContent'i
 *   2. token= kısmı: onclick içindeki token string'in son kısmı
 * İkisi birleştirilerek "https://streamtape.com/get_video?id=...&token=..." oluşturulur.
 *
 * Kaynak: https://github.com/recloudstream/cloudstream/blob/master/app/src/main/java/com/lagradost/cloudstream3/extractors/StreamTape.kt
 */
object StreamTapeWrapper {
    private const val TAG = "StreamTapeWrapper"

    data class ExtractedVideo(
        val url: String,
        val label: String,
        val isM3u8: Boolean,
        val headers: Map<String, String> = emptyMap()
    )

    /**
     * StreamTape embed sayfasından stream URL'sini çıkarır.
     * HTML içindeki robotlink id'si + token parçasını birleştirir.
     *
     * @param pageContent HTML sayfasının içeriği
     * @param embedUrl Embed URL'si (referer için)
     */
    fun extractFromHtml(pageContent: String, embedUrl: String): List<ExtractedVideo> {
        val videos = mutableListOf<ExtractedVideo>()
        try {
            // Yöntem 1: id ve token ayrı ayrı çıkar ve birleştir
            val idRegex    = Regex("""id=([a-zA-Z0-9_-]+)""")
            val tokenRegex = Regex("""token=([a-zA-Z0-9_-]+)""")

            // StreamTape'in get_video URL'sini doğrudan içerdiği satırı bul
            val getVideoRegex = Regex("""(?:robotlink|get_video)\??[^"']*["']([^"']*get_video[^"']*)["']""")
            getVideoRegex.find(pageContent)?.groupValues?.get(1)?.let { partialUrl ->
                val fullUrl = when {
                    partialUrl.startsWith("//") -> "https:$partialUrl"
                    partialUrl.startsWith("/")  -> "https://streamtape.com$partialUrl"
                    partialUrl.startsWith("http") -> partialUrl
                    else -> "https://streamtape.com/$partialUrl"
                }.replace(" ", "")
                videos.add(ExtractedVideo(fullUrl, "StreamTape", false,
                    mapOf("Referer" to "https://streamtape.com/")))
                Log.d(TAG, "StreamTape get_video URL: $fullUrl")
            }

            // Yöntem 2: robotlink span içeriği + token birleştirme
            if (videos.isEmpty()) {
                val robotlinkRegex = Regex("""id="robotlink"[^>]*>([^<]+)<""")
                val tokenMatchRegex = Regex("""document\.getElementById\('robotlink'\)\.innerHTML\s*=\s*[^+]+\+\s*["']([^"']+)["']""")
                val robotPart = robotlinkRegex.find(pageContent)?.groupValues?.get(1)?.trim()
                val tokenPart = tokenMatchRegex.find(pageContent)?.groupValues?.get(1)?.trim()
                if (!robotPart.isNullOrBlank() && !tokenPart.isNullOrBlank()) {
                    val combined = "https:${robotPart}${tokenPart}".replace(" ", "")
                    videos.add(ExtractedVideo(combined, "StreamTape", false,
                        mapOf("Referer" to "https://streamtape.com/")))
                    Log.d(TAG, "StreamTape robot+token URL: $combined")
                }
            }

            // Yöntem 3: direkt .mp4 URL'i içeren satır
            if (videos.isEmpty()) {
                val directMp4 = Regex("""(https?://[^"' ]+\.mp4[^"' ]*)""").find(pageContent)?.groupValues?.get(1)
                if (!directMp4.isNullOrBlank()) {
                    videos.add(ExtractedVideo(directMp4, "StreamTape", false,
                        mapOf("Referer" to "https://streamtape.com/")))
                }
            }

            Log.d(TAG, "StreamTape extraction: ${videos.size} video")
        } catch (e: Exception) {
            Log.e(TAG, "StreamTape extraction hatası: ${e.message}")
        }
        return videos
    }

    fun isStreamTapeUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("streamtape.com") ||
               lower.contains("streamtape.net") ||
               lower.contains("shavetape.cash") ||
               lower.contains("streamtape.xyz")
    }
}
