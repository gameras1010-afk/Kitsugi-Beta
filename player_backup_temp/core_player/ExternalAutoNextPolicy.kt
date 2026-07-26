package com.kitsugi.animelist.core.player

/**
 * T1.8 – ExternalAutoNextPolicy
 *
 * Evaluates whether we should automatically proceed to the next episode
 * based on the callback metrics received from the external video player.
 */
object ExternalAutoNextPolicy {

    /**
     * Determines if the next episode should play automatically.
     *
     * @param positionMs The last playback position returned by the player.
     * @param durationMs The total duration of the media file, if available.
     * @param endedByUser True if the user manually backed out/cancelled.
     * @param autoPlayEnabled True if global autoplay setting is enabled.
     * @return True if we should transition to the next episode.
     */
    fun shouldAutoNext(
        positionMs: Long,
        durationMs: Long?,
        endedByUser: Boolean,
        autoPlayEnabled: Boolean
    ): Boolean {
        if (!autoPlayEnabled) return false

        // If the video played to at least 90% completion
        if (durationMs != null && durationMs > 0) {
            val progress = positionMs.toFloat() / durationMs
            if (progress >= 0.90f) {
                return true
            }
        }

        // If the player ended playback naturally (endedByUser = false)
        return !endedByUser
    }
}
