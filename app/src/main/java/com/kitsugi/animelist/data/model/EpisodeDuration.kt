package com.kitsugi.animelist.data.model

/**
 * FP-43 – Model representing metadata for episode duration.
 */
data class EpisodeDuration(
    val mediaId: Int,
    val durationMin: Int,
    val totalEpisodes: Int
) {
    val durationText: String
        get() = "${durationMin}dk"
        
    val totalDurationMin: Int
        get() = durationMin * totalEpisodes
}
