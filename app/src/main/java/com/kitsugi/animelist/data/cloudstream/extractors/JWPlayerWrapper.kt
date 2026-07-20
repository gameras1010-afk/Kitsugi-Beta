package com.kitsugi.animelist.data.cloudstream.extractors

import android.util.Log

/**
 * FP-35 & FP-36 – JWPlayer and GdrivePlayer extractor wrapper classes.
 * Wraps extraction logic for resolving stream URLs from JWPlayer embed hosts
 * and Google Drive stream players.
 */
object JWPlayerWrapper {
    private const val TAG = "JWPlayerWrapper"

    data class ExtractedVideo(
        val url: String,
        val label: String,
        val isM3u8: Boolean,
        val headers: Map<String, String> = emptyMap()
    )

    /**
     * Extracts direct video streams from a JWPlayer embed page content.
     */
    fun extractJWPlayer(pageContent: String): List<ExtractedVideo> {
        val videos = mutableListOf<ExtractedVideo>()
        try {
            // Match source files in JSON format: "file": "..." or sources: [...]
            val fileRegex = """"file"\s*:\s*"([^"]+)"""".toRegex()
            val labelRegex = """"label"\s*:\s*"([^"]+)"""".toRegex()
            
            val files = fileRegex.findAll(pageContent).map { it.groupValues[1] }.toList()
            val labels = labelRegex.findAll(pageContent).map { it.groupValues[1] }.toList()
            
            for (i in files.indices) {
                val url = files[i]
                val label = labels.getOrNull(i) ?: "HD"
                val isM3u8 = url.contains(".m3u8")
                videos.add(ExtractedVideo(url, label, isM3u8))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting JWPlayer", e)
        }
        return videos
    }

    /**
     * Extracts video streams from a Gdriveplayer helper/proxy stream URL.
     */
    fun extractGdrivePlayer(embedUrl: String): List<ExtractedVideo> {
        val videos = mutableListOf<ExtractedVideo>()
        try {
            if (embedUrl.contains("gdriveplayer") || embedUrl.contains("drive.google.com")) {
                // Return fallback stream formats or direct link placeholder
                videos.add(
                    ExtractedVideo(
                        url = embedUrl,
                        label = "GDrive Source",
                        isM3u8 = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting GdrivePlayer", e)
        }
        return videos
    }
}
