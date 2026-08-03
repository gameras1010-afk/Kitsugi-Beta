package com.kitsugi.animelist.data.cloudstream.extractors

import android.util.Log

/**
 * FP-32 – Mp4Upload (mp4upload.com) extractor wrapper.
 *
 * Mp4Upload, JWPlayer ile stream sağlar. HTML içinde player
 * kurulum scripti içinde "file":"url" formatıyla saklar.
 *
 * Kaynak: https://github.com/recloudstream/cloudstream/blob/master/app/src/main/java/com/lagradost/cloudstream3/extractors/Mp4Upload.kt
 */
object Mp4UploadWrapper {
    private const val TAG = "Mp4UploadWrapper"

    data class ExtractedVideo(
        val url: String,
        val label: String,
        val isM3u8: Boolean,
        val headers: Map<String, String> = emptyMap()
    )

    /**
     * Mp4Upload embed sayfasından stream URL'sini çıkarır.
     * player kurulum scripti: player.setup({ sources: [{ file: "url", label: "720p" }] })
     *
     * @param pageContent HTML sayfasının içeriği
     * @param embedUrl Embed URL'si (referer için)
     */
    fun extractFromHtml(pageContent: String, embedUrl: String): List<ExtractedVideo> {
        val videos = mutableListOf<ExtractedVideo>()
        try {
            // Yöntem 1: sources array içinde file + label
            val sourcesBlockRegex = Regex("""sources\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
            sourcesBlockRegex.find(pageContent)?.groupValues?.get(1)?.let { sourcesBlock ->
                val fileRegex  = Regex("""file\s*:\s*["']([^"']+)["']""")
                val labelRegex = Regex("""label\s*:\s*["']([^"']+)["']""")
                val files  = fileRegex.findAll(sourcesBlock).map { it.groupValues[1] }.toList()
                val labels = labelRegex.findAll(sourcesBlock).map { it.groupValues[1] }.toList()
                files.forEachIndexed { i, url ->
                    if (url.isNotBlank() && (url.startsWith("http") || url.startsWith("//"))) {
                        val fixedUrl = if (url.startsWith("//")) "https:$url" else url
                        val label = labels.getOrNull(i) ?: "HD"
                        val isM3u8 = fixedUrl.contains(".m3u8")
                        videos.add(ExtractedVideo(fixedUrl, label, isM3u8,
                            mapOf("Referer" to "https://www.mp4upload.com/")))
                        Log.d(TAG, "Mp4Upload source: $fixedUrl [$label]")
                    }
                }
            }

            // Yöntem 2: doğrudan src veya file regex
            if (videos.isEmpty()) {
                val directRegex = Regex(""""file"\s*:\s*"(https?://[^"]+\.(?:mp4|m3u8)[^"]*)"""")
                directRegex.findAll(pageContent).forEach { match ->
                    val url = match.groupValues[1]
                    if (url.isNotBlank()) {
                        val isM3u8 = url.contains(".m3u8")
                        videos.add(ExtractedVideo(url, "HD", isM3u8,
                            mapOf("Referer" to "https://www.mp4upload.com/")))
                    }
                }
            }

            Log.d(TAG, "Mp4Upload extraction: ${videos.size} video")
        } catch (e: Exception) {
            Log.e(TAG, "Mp4Upload extraction hatası: ${e.message}")
        }
        return videos
    }

    fun isMp4UploadUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("mp4upload.com") || lower.contains("www.mp4upload.com")
    }
}
