package com.kitsugi.animelist.data.cloudstream.extractors

import android.util.Log

/**
 * FP-33 – Upstream (upstream.to) extractor wrapper.
 *
 * Upstream.to, stream URL'sini bir JWPlayer veya benzer player kurulum
 * scriptinde "file":"url" formatıyla HLS veya doğrudan MP4 olarak sağlar.
 *
 * Kaynak: https://github.com/recloudstream/cloudstream/blob/master/app/src/main/java/com/lagradost/cloudstream3/extractors/Upstream.kt
 */
object UpstreamWrapper {
    private const val TAG = "UpstreamWrapper"

    data class ExtractedVideo(
        val url: String,
        val label: String,
        val isM3u8: Boolean,
        val headers: Map<String, String> = emptyMap()
    )

    /**
     * Upstream embed sayfasından HLS/MP4 stream URL'lerini çıkarır.
     *
     * @param pageContent HTML sayfasının içeriği
     * @param embedUrl Embed URL'si (referer için)
     */
    fun extractFromHtml(pageContent: String, embedUrl: String): List<ExtractedVideo> {
        val videos = mutableListOf<ExtractedVideo>()
        try {
            val refererHost = runCatching {
                val uri = java.net.URI(embedUrl)
                "${uri.scheme}://${uri.host}"
            }.getOrDefault("https://upstream.to")

            // Yöntem 1: sources array içinde file/label çiftleri
            val sourcesRegex = Regex("""sources\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
            sourcesRegex.find(pageContent)?.groupValues?.get(1)?.let { block ->
                val fileRx  = Regex("""["']?file["']?\s*:\s*["']([^"']+)["']""")
                val labelRx = Regex("""["']?label["']?\s*:\s*["']([^"']+)["']""")
                val files  = fileRx.findAll(block).map { it.groupValues[1] }.toList()
                val labels = labelRx.findAll(block).map { it.groupValues[1] }.toList()
                files.forEachIndexed { i, url ->
                    if (url.startsWith("http") || url.startsWith("//")) {
                        val fixedUrl = if (url.startsWith("//")) "https:$url" else url
                        val label = labels.getOrNull(i) ?: "HD"
                        val isM3u8 = fixedUrl.contains(".m3u8")
                        videos.add(ExtractedVideo(fixedUrl, label, isM3u8,
                            mapOf("Referer" to refererHost, "Origin" to refererHost)))
                        Log.d(TAG, "Upstream source: $fixedUrl [$label]")
                    }
                }
            }

            // Yöntem 2: doğrudan m3u8 veya mp4 URL
            if (videos.isEmpty()) {
                val urlRegex = Regex("""["'](https?://[^"']+\.(?:m3u8|mp4)[^"']*)["']""")
                urlRegex.findAll(pageContent).forEach { match ->
                    val url = match.groupValues[1]
                    if (!url.contains("poster") && !url.contains(".jpg") && !url.contains(".png")) {
                        val isM3u8 = url.contains(".m3u8")
                        videos.add(ExtractedVideo(url, "Upstream", isM3u8,
                            mapOf("Referer" to refererHost)))
                    }
                }
            }

            Log.d(TAG, "Upstream extraction: ${videos.size} video")
        } catch (e: Exception) {
            Log.e(TAG, "Upstream extraction hatası: ${e.message}")
        }
        return videos
    }

    fun isUpstreamUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("upstream.to") || lower.contains("upns.to")
    }
}
