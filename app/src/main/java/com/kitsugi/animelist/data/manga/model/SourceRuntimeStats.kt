package com.kitsugi.animelist.data.manga.model

data class SourceRuntimeStats(
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val avgSearchMs: Long = 0L,
    val avgPopularMs: Long = 0L,
    val avgDetailsMs: Long = 0L,
    val avgChapterMs: Long = 0L,
    val avgPageMs: Long = 0L,
    val avgImageMs: Long = 0L,
)
