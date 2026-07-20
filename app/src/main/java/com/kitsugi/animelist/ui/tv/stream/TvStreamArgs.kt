package com.kitsugi.animelist.ui.tv.stream

import java.io.Serializable

data class TvStreamArgs(
    val malId: Int?,
    val aniListId: Int?,
    val tmdbId: Int? = null,
    val episode: Int,
    val season: Int,
    val isMovie: Boolean,
    val title: String,
    val posterUrl: String?,
    val titleEnglish: String?,
    val titleRomaji: String?,
    val titleNative: String?,
    val startYear: Int?,
    val description: String? = null
) : Serializable
