package com.kitsugi.animelist.data.cloudstream.extractors

/**
 * FP-31 – Mixdrop extractor wrapper.
 */
object MixdropWrapper {
    fun extract(url: String): String? {
        return if (url.contains("mixdrop")) url else null
    }
}
