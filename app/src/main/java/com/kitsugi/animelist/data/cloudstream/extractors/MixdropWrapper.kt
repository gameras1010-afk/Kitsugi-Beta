package com.kitsugi.animelist.data.cloudstream.extractors

import android.util.Log

/**
 * FP-31 – Mixdrop (mixdrop.co / mixdrop.to) extractor wrapper.
 *
 * Mixdrop, stream URL'sini MDCore.wurl veya MDCore.t değişkenlerinde
 * JavaScript ile obfuscate edilmiş şekilde saklar.
 *
 * Kaynak: https://github.com/recloudstream/cloudstream/blob/master/app/src/main/java/com/lagradost/cloudstream3/extractors/MixDrop.kt
 */
object MixdropWrapper {
    private const val TAG = "MixdropWrapper"

    data class ExtractedVideo(
        val url: String,
        val label: String,
        val isM3u8: Boolean,
        val headers: Map<String, String> = emptyMap()
    )

    /**
     * Mixdrop embed sayfası HTML içeriğinden stream URL'sini çıkarır.
     * MDCore.wurl = "//mixdrop.co/..." veya wurl: "..." formatındadır.
     *
     * @param pageContent HTML sayfasının içeriği
     * @param embedUrl Embed URL'si (referer için)
     */
    fun extractFromHtml(pageContent: String, embedUrl: String): List<ExtractedVideo> {
        val videos = mutableListOf<ExtractedVideo>()
        try {
            // Yöntem 1: MDCore.wurl (Mixdrop'un ana obfuscation paterni)
            val wurlRegex = Regex("""(?:MDCore\.wurl|wurl)\s*=\s*["']([^"']+)["']""")
            wurlRegex.find(pageContent)?.groupValues?.get(1)?.let { wurl ->
                val fullUrl = when {
                    wurl.startsWith("//") -> "https:$wurl"
                    wurl.startsWith("/") -> {
                        val host = runCatching { java.net.URI(embedUrl).let { "${it.scheme}://${it.host}" } }.getOrDefault("https://mixdrop.co")
                        "$host$wurl"
                    }
                    wurl.startsWith("http") -> wurl
                    else -> "https://$wurl"
                }
                val isM3u8 = fullUrl.contains(".m3u8")
                videos.add(ExtractedVideo(fullUrl, "Mixdrop", isM3u8,
                    mapOf("Referer" to embedUrl, "Origin" to "https://mixdrop.co")))
                Log.d(TAG, "Mixdrop MDCore.wurl bulundu: $fullUrl")
            }

            // Yöntem 2: doğrudan .mp4 / .m3u8 URL
            if (videos.isEmpty()) {
                val directRegex = Regex("""["'](https?://[^"']+\.(?:mp4|m3u8)[^"']*)["']""")
                directRegex.findAll(pageContent).forEach { match ->
                    val url = match.groupValues[1]
                    if (!url.contains("poster") && !url.contains("thumb") && !url.contains(".png") && !url.contains(".jpg")) {
                        val isM3u8 = url.contains(".m3u8")
                        videos.add(ExtractedVideo(url, "Mixdrop", isM3u8,
                            mapOf("Referer" to embedUrl)))
                    }
                }
            }

            Log.d(TAG, "Mixdrop extraction: ${videos.size} video")
        } catch (e: Exception) {
            Log.e(TAG, "Mixdrop extraction hatası: ${e.message}")
        }
        return videos
    }

    fun isMixdropUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("mixdrop.co") ||
               lower.contains("mixdrop.to") ||
               lower.contains("mixdrop.ch") ||
               lower.contains("mixdrop.bz") ||
               lower.contains("mixdrop.gl") ||
               lower.contains("mixdrop.club")
    }
}
