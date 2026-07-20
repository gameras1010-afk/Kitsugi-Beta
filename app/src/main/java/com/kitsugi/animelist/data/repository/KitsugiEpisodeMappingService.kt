package com.kitsugi.animelist.data.repository

import java.util.Locale

object KitsugiEpisodeMappingService {

    fun normalizeTitle(title: String): String {
        return title.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Tries to find an episode match by matching normalized titles.
     * If titles are too generic (e.g. "Episode 1", "Bölüm 2"), it won't map by title.
     */
    fun isGenericTitle(title: String): Boolean {
        val normalized = normalizeTitle(title)
        val genericPatterns = listOf(
            Regex("^episode \\d+\$"),
            Regex("^bölüm \\d+\$"),
            Regex("^ep \\d+\$"),
            Regex("^ep\\d+\$"),
            Regex("^\\d+\$"),
            Regex("^part \\d+\$")
        )
        return genericPatterns.any { it.matches(normalized) }
    }

    fun parseEpisodeNumber(title: String): Int? {
        val patterns = listOf(
            Regex("[Ss]\\d+[Ee](\\d+)"),
            Regex("[Ee]p(?:isode)?\\.?\\s*(\\d+)"),
            Regex("#(\\d+)"),
            Regex("B[öo]l[üu]m\\s+(\\d+)"),
            Regex("^(\\d+)\\s*[-–—.]"),
            Regex("\\b(\\d+)\\b")
        )
        for (pattern in patterns) {
            val match = pattern.find(title)
            if (match != null) {
                val num = match.groupValues.getOrNull(1)?.toIntOrNull()
                if (num != null) return num
            }
        }
        return null
    }

    /**
     * Fallback to map by index. If the 5th episode of the source list is requested,
     * it will return the 5th episode of the target list.
     */
    fun <T, R> alignEpisodesByAbsoluteIndex(
        trackerEpisodes: List<T>,
        addonEpisodes: List<R>,
        trackerIndex: Int
    ): R? {
        if (trackerIndex < 0 || trackerIndex >= trackerEpisodes.size) return null
        return addonEpisodes.getOrNull(trackerIndex)
    }
}
