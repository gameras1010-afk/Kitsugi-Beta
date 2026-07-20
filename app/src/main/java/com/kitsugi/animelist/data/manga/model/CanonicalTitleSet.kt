package com.kitsugi.animelist.data.manga.model

data class CanonicalTitleSet(
    val raw: String,
    val cleaned: String,
    val ascii: String,
    val compact: String,
    val core: String,
    val aliases: Set<String>,
    val tokens: Set<String>,
)
