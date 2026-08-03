package com.kitsugi.animelist.data.cloudstream.extractors

import android.util.Log

/**
 * FP-35 – XStreamCDN / Fembed extractor wrapper.
 *
 * XStreamCDN (eski adı Fembed), /api/source/<id> endpoint'i üzerinden
 * POST isteğiyle JSON formatında stream URL'lerini döndürür.
 *
 * API yanıtı: {"success":true,"data":[{"file":"url","label":"720p","type":"mp4"}]}
 *
 * Kaynak: https://github.com/recloudstream/cloudstream/blob/master/app/src/main/java/com/lagradost/cloudstream3/extractors/XStreamCDN.kt
 */
object XStreamCdnWrapper {
    private const val TAG = "XStreamCdnWrapper"

    data class ExtractedVideo(
        val url: String,
        val label: String,
        val isM3u8: Boolean,
        val headers: Map<String, String> = emptyMap()
    )

    /**
     * XStreamCDN/Fembed embed sayfasından veya API yanıtından stream URL'lerini çıkarır.
     *
     * @param pageContent HTML içeriği veya API JSON yanıtı
     * @param embedUrl Embed URL'si (video ID çıkarmak için)
     */
    fun extractFromHtml(pageContent: String, embedUrl: String): List<ExtractedVideo> {
        val videos = mutableListOf<ExtractedVideo>()
        try {
            val refererHost = runCatching {
                val uri = java.net.URI(embedUrl)
                "${uri.scheme}://${uri.host}"
            }.getOrDefault("https://fembed.com")

            // Yöntem 1: JSON API yanıtı ({"success":true,"data":[...]})
            if (pageContent.contains("\"success\"") && pageContent.contains("\"data\"")) {
                val fileRegex  = Regex(""""file"\s*:\s*"([^"]+)"""")
                val labelRegex = Regex(""""label"\s*:\s*"([^"]+)"""")
                val files  = fileRegex.findAll(pageContent).map { it.groupValues[1] }.toList()
                val labels = labelRegex.findAll(pageContent).map { it.groupValues[1] }.toList()
                files.forEachIndexed { i, url ->
                    if (url.isNotBlank() && url.startsWith("http")) {
                        val label = labels.getOrNull(i) ?: "HD"
                        val isM3u8 = url.contains(".m3u8")
                        videos.add(ExtractedVideo(url, label, isM3u8,
                            mapOf("Referer" to refererHost)))
                        Log.d(TAG, "XStreamCDN API source: $url [$label]")
                    }
                }
            }

            // Yöntem 2: HTML script içinde sources array
            if (videos.isEmpty()) {
                val sourcesRegex = Regex("""sources\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
                sourcesRegex.find(pageContent)?.groupValues?.get(1)?.let { block ->
                    val fileRx  = Regex("""["']?file["']?\s*:\s*["']([^"']+)["']""")
                    val labelRx = Regex("""["']?label["']?\s*:\s*["']([^"']+)["']""")
                    val files  = fileRx.findAll(block).map { it.groupValues[1] }.toList()
                    val labels = labelRx.findAll(block).map { it.groupValues[1] }.toList()
                    files.forEachIndexed { i, url ->
                        if (url.startsWith("http")) {
                            val label = labels.getOrNull(i) ?: "HD"
                            val isM3u8 = url.contains(".m3u8")
                            videos.add(ExtractedVideo(url, label, isM3u8,
                                mapOf("Referer" to refererHost)))
                        }
                    }
                }
            }

            Log.d(TAG, "XStreamCDN extraction: ${videos.size} video")
        } catch (e: Exception) {
            Log.e(TAG, "XStreamCDN extraction hatası: ${e.message}")
        }
        return videos
    }

    /**
     * Embed URL'sinden video ID'sini çıkarır.
     * Örnek: https://fembed.com/v/xxxx → xxxx
     */
    fun extractVideoId(embedUrl: String): String? {
        return Regex("""/(?:v|e|f)/([A-Za-z0-9_-]+)""").find(embedUrl)?.groupValues?.get(1)
    }

    fun isXStreamCdnUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("xstreamcdn.com") ||
               lower.contains("fembed.com") ||
               lower.contains("fembad.org") ||
               lower.contains("feurl.com") ||
               lower.contains("fembed9hd.com") ||
               lower.contains("diasfem.com")
    }
}
