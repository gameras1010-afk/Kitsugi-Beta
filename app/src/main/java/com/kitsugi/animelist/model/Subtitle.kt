package com.kitsugi.animelist.model

data class Subtitle(
    val id: String,
    val url: String,
    val lang: String,
    val addonName: String,
    val addonLogo: String? = null,
    val mimeType: String? = null,
    val extension: String? = null
)
