package com.kitsugi.animelist.data.model

/**
 * FP-47 – Detailed list status matching full MyAnimeList entry criteria.
 */
data class FullMyListStatus(
    val mediaId: Int,
    val isRewatching: Boolean,
    val numTimesRewatched: Int,
    val tags: List<String>,
    val comments: String,
    val priority: Int
)
