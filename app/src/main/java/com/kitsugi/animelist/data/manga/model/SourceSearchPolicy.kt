package com.kitsugi.animelist.data.manga.model

data class SourceSearchPolicy(
    val sourceKey: String,
    val priority: Int,
    val isTurkishPreferred: Boolean,
    val isTrusted: Boolean,
    val isFallbackOnly: Boolean,
    val allowGlobalSearch: Boolean,
    val queryMode: SourceQueryMode,
    val reason: String? = null,
)
