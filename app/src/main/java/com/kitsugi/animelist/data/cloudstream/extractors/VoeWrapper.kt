package com.kitsugi.animelist.data.cloudstream.extractors

/**
 * FP-34 – Voe extractor wrapper.
 */
object VoeWrapper {
    fun extract(url: String): String? {
        return if (url.contains("voe")) url else null
    }
}
