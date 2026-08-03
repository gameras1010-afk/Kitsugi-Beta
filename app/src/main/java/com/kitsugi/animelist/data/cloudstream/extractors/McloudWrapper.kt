package com.kitsugi.animelist.data.cloudstream.extractors

import android.util.Log

/**
 * FP-30 – Mcloud (mcloud.to / mcloud.bz) extractor wrapper.
 *
 * Mcloud genellikle HiAnime ve benzeri anime sitelerinde kullanılan bir embed player'dır.
 * HTML sayfasında Base64 encode edilmiş JSON payload içinde stream URL'sini saklar.
 *
 * Kaynak: https://github.com/recloudstream/cloudstream/blob/master/app/src/main/java/com/lagradost/cloudstream3/extractors/MCloud.kt
 */
object McloudWrapper {
    private const val TAG = "McloudWrapper"

    data class ExtractedVideo(
        val url: String,
        val label: String,
        val isM3u8: Boolean,
        val headers: Map<String, String> = emptyMap()
    )

    /**
     * Mcloud embed sayfası HTML içeriğinden stream URL'lerini çıkarır.
     * Mcloud sayfası window.atob() ile encode edilmiş bir JSON blob içerir.
     *
     * @param pageContent HTML sayfasının içeriği
     * @param embedUrl Embed sayfasının URL'si (referer için)
     * @return Çıkarılan video listesi
     */
    fun extractFromHtml(pageContent: String, embedUrl: String): List<ExtractedVideo> {
        val videos = mutableListOf<ExtractedVideo>()
        try {
            // Yöntem 1: sources JSON array'i bul
            val sourcesRegex = Regex("""sources\s*:\s*\[\s*\{[^}]*file\s*:\s*["']([^"']+)["'][^}]*\}""")
            sourcesRegex.findAll(pageContent).forEach { match ->
                val url = match.groupValues[1]
                if (url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))) {
                    val isM3u8 = url.contains(".m3u8")
                    videos.add(ExtractedVideo(url, if (isM3u8) "HLS" else "MP4", isM3u8,
                        mapOf("Referer" to embedUrl)))
                }
            }

            // Yöntem 2: file: "url" formatı
            if (videos.isEmpty()) {
                val fileRegex = Regex("""file\s*:\s*["']([^"']+\.(?:m3u8|mp4)[^"']*)["']""")
                fileRegex.findAll(pageContent).forEach { match ->
                    val url = match.groupValues[1]
                    if (url.isNotBlank()) {
                        val isM3u8 = url.contains(".m3u8")
                        videos.add(ExtractedVideo(url, if (isM3u8) "HLS" else "MP4", isM3u8,
                            mapOf("Referer" to embedUrl)))
                    }
                }
            }

            // Yöntem 3: Base64 JSON blob parse (window.mcloudData veya benzeri)
            if (videos.isEmpty()) {
                val b64Regex = Regex("""(?:data|payload|sources)\s*=\s*['"]([A-Za-z0-9+/=]{40,})['"]""")
                b64Regex.findAll(pageContent).forEach { match ->
                    try {
                        val decoded = String(android.util.Base64.decode(match.groupValues[1], android.util.Base64.DEFAULT))
                        val urlInDecoded = Regex("""https?://[^\s"'\\]+\.(?:m3u8|mp4)[^\s"'\\]*""").find(decoded)?.value
                        if (!urlInDecoded.isNullOrBlank()) {
                            val isM3u8 = urlInDecoded.contains(".m3u8")
                            videos.add(ExtractedVideo(urlInDecoded, if (isM3u8) "HLS" else "MP4", isM3u8,
                                mapOf("Referer" to embedUrl)))
                        }
                    } catch (_: Exception) {}
                }
            }

            Log.d(TAG, "Mcloud extraction: ${videos.size} video bulundu")
        } catch (e: Exception) {
            Log.e(TAG, "Mcloud extraction hatası: ${e.message}")
        }
        return videos
    }

    /** URL'nin Mcloud'a ait olup olmadığını kontrol eder */
    fun isMcloudUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("mcloud.to") ||
               lower.contains("mcloud.bz") ||
               lower.contains("mcloud.uno") ||
               lower.contains("mcloud.ru")
    }
}
