package com.kitsugi.animelist.data.manga.model

import com.kitsugi.animelist.data.manga.MangaDetails
import com.kitsugi.animelist.data.manga.MangaSource

data class ScoredSearchResult(
    val source: MangaSource,
    val manga: MangaDetails,
    val score: Double,
)
