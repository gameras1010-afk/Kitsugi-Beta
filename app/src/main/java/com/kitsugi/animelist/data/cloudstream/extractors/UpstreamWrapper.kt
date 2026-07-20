package com.kitsugi.animelist.data.cloudstream.extractors

/**
 * FP-33 – Upstream extractor wrapper.
 */
object UpstreamWrapper {
    fun extract(url: String): String? {
        return if (url.contains("upstream")) url else null
    }
}
