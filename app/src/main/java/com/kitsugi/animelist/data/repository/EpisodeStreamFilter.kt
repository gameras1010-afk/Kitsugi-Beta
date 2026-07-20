package com.kitsugi.animelist.data.repository

import android.util.Log

/**
 * Filters stream results to only include streams relevant to the requested episode.
 *
 * Ported from KitsugiTV-dev's stream filtering logic.
 *
 * Torrent-based addons (e.g. Torrentio) return streams for ALL episodes in a season when
 * queried with an IMDB ID. The torrent file name / title contains episode identifiers like
 * "S01E02" that we can use to keep only the relevant streams.
 *
 * Rules (in order):
 *  1. CS plugin streams (isCS=true) are already episode-specific → pass through
 *  2. Direct HTTP streams with no infoHash are likely episode-specific → pass through
 *  3. If title/name contains a SPECIFIC episode tag that does NOT match → exclude
 *  4. If title/name contains a season pack tag (S01 without E__) → include (whole-season pack,
 *     user can seek to the right file)
 *  5. If no episode info at all → include (can't tell, be permissive)
 */
object EpisodeStreamFilter {

    private const val TAG = "EpisodeStreamFilter"

    fun filterForEpisode(
        streams: List<StreamSource>,
        season: Int,
        episode: Int
    ): List<StreamSource> {
        val result = streams.filter { stream ->
            shouldInclude(stream, season, episode)
        }
        Log.d(TAG, "Episode S${season}E${episode}: ${streams.size} → ${result.size} after filter")
        return result
    }

    private fun shouldInclude(stream: StreamSource, season: Int, episode: Int): Boolean {
        // CS plugin streams are already episode-specific
        if (stream.isCS) return true

        // Direct HTTP streams without a torrent hash are likely already episode-specific
        if (!stream.url.isNullOrBlank() && stream.infoHash.isNullOrBlank()) return true

        val searchText = buildSearchText(stream)
        if (searchText.isBlank()) return true // No info → be permissive

        val episodeTag = detectEpisodeTag(searchText)

        return when (episodeTag) {
            is EpisodeTag.Specific -> {
                // Has a specific S__E__ tag — only include if it matches our episode
                val matches = episodeTag.season == season && episodeTag.episode == episode
                if (!matches) {
                    Log.d(TAG, "Excluding: '${stream.title.take(60)}' → S${episodeTag.season}E${episodeTag.episode} ≠ S${season}E${episode}")
                }
                matches
            }
            is EpisodeTag.SeasonPack -> {
                // Season pack (e.g. "S01 Complete") — include if season matches
                episodeTag.season == season
            }
            EpisodeTag.None -> true // No episode info → include
        }
    }

    private fun buildSearchText(stream: StreamSource): String {
        return listOf(stream.title, stream.name)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .lowercase()
    }

    private sealed class EpisodeTag {
        data class Specific(val season: Int, val episode: Int) : EpisodeTag()
        data class SeasonPack(val season: Int) : EpisodeTag()
        object None : EpisodeTag()
    }

    private fun detectEpisodeTag(text: String): EpisodeTag {
        // Pattern: S01E02, S1E2, s01e02
        val sXeX = Regex("""s(\d{1,2})e(\d{1,2})(?!\d)""").find(text)
        if (sXeX != null) {
            return EpisodeTag.Specific(
                season = sXeX.groupValues[1].toInt(),
                episode = sXeX.groupValues[2].toInt()
            )
        }

        // Pattern: 1x02, 01x02
        val nxn = Regex("""(\d{1,2})x(\d{2})(?!\d)""").find(text)
        if (nxn != null) {
            return EpisodeTag.Specific(
                season = nxn.groupValues[1].toInt(),
                episode = nxn.groupValues[2].toInt()
            )
        }

        // Pattern: Episode 02, Ep02, E02 (standalone, not part of S__E__)
        val epOnly = Regex("""(?<![a-z])(?:episode[\s._-]?|ep[\s._-]?|e)(\d{2,3})(?!\d)""").find(text)
        if (epOnly != null) {
            // Episode-only tag without season: assume season 1
            return EpisodeTag.Specific(
                season = 1,
                episode = epOnly.groupValues[1].toInt()
            )
        }

        // Pattern: Season pack — S01 without E__ (e.g. "Attack.on.Titan.S01.Complete")
        val seasonPack = Regex("""s(\d{1,2})(?![\s._-]*e\d)""").find(text)
        if (seasonPack != null) {
            return EpisodeTag.SeasonPack(season = seasonPack.groupValues[1].toInt())
        }

        return EpisodeTag.None
    }
}
