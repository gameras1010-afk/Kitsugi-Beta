package com.kitsugi.animelist.data.cloudstream.extractors

/**
 * FP-30 – Mcloud extractor wrapper.
 */
object McloudWrapper {
    fun extract(url: String): String? {
        return if (url.contains("mcloud")) url else null
    }
}
