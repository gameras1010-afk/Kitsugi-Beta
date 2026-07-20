package com.kitsugi.animelist.data.cloudstream.extractors

/**
 * FP-35 – XStreamCdn extractor wrapper.
 */
object XStreamCdnWrapper {
    fun extract(url: String): String? {
        return if (url.contains("xstreamcdn") || url.contains("fembed")) url else null
    }
}
