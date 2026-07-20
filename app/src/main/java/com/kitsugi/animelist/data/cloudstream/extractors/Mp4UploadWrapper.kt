package com.kitsugi.animelist.data.cloudstream.extractors

/**
 * FP-32 – Mp4Upload extractor wrapper.
 */
object Mp4UploadWrapper {
    fun extract(url: String): String? {
        return if (url.contains("mp4upload")) url else null
    }
}
