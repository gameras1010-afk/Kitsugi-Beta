package com.kitsugi.animelist.data.cloudstream.extractors

/**
 * FP-29 – Gdriveplayer extractor wrapper.
 */
object GdriveplayerWrapper {
    fun extract(url: String): String? {
        return if (url.contains("gdrive")) url else null
    }
}
