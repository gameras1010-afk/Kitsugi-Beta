package com.kitsugi.animelist.data.cloudstream.extractors

/**
 * FP-36 – StreamTape extractor wrapper.
 */
object StreamTapeWrapper {
    fun extract(url: String): String? {
        return if (url.contains("streamtape")) url else null
    }
}
