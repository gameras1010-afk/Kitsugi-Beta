package com.kitsugi.animelist.ui.screens.explore

import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.data.remote.JikanSearchResult

fun generateExploreEntryMap(currentEntries: List<MediaEntry>): Map<String, MediaEntry> {
    val mapping = mutableMapOf<String, MediaEntry>()
    currentEntries.forEach { entry ->
        mapping["${entry.source.lowercase()}_${entry.malId}"] = entry
        if (entry.tmdbId != null) {
            mapping["tmdb_${entry.tmdbId}"] = entry
        }
        if (entry.simklId != null) {
            mapping["simkl_${entry.simklId}"] = entry
        }
        if (entry.source.equals("anilist", ignoreCase = true) && entry.malId != null && entry.malId >= 100_000_000) {
            mapping["anilist_${entry.malId - 100_000_000}"] = entry
        }
        if (entry.source.equals("jikan", ignoreCase = true) || entry.source.equals("mal", ignoreCase = true)) {
            mapping["mal_${entry.malId}"] = entry
            mapping["jikan_${entry.malId}"] = entry
        }
        val normTitle = entry.title.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }.trim()
        if (normTitle.isNotEmpty()) {
            mapping["${entry.type.name.lowercase()}_$normTitle"] = entry
        }
    }
    return mapping
}

fun getMediaEntryFromMap(
    result: JikanSearchResult,
    entryMap: Map<String, MediaEntry>
): MediaEntry? {
    val directKey = "${result.source.lowercase()}_${result.malId}"
    var found = entryMap[directKey]

    if (found == null) {
        val tmdbId = result.tmdbId ?: if (result.source.equals("tmdb", ignoreCase = true)) result.malId else null
        if (tmdbId != null) {
            found = entryMap["tmdb_$tmdbId"]
        }
    }

    if (found == null) {
        val rMal = if (result.source.equals("jikan", ignoreCase = true) || result.source.equals("mal", ignoreCase = true)) {
            result.malId
        } else {
            result.realMalId
        }
        if (rMal != null) {
            found = entryMap["${result.source.lowercase()}_$rMal"]
                ?: entryMap["mal_$rMal"]
                ?: entryMap["jikan_$rMal"]
                ?: entryMap["anilist_$rMal"]
                ?: entryMap["simkl_$rMal"]
        }
    }

    if (found == null) {
        val normTitle = buildString {
            for (c in result.title.lowercase()) {
                if (c in 'a'..'z' || c in '0'..'9') append(c)
            }
        }.trim()
        if (normTitle.isNotEmpty()) {
            found = entryMap["${result.type.name.lowercase()}_$normTitle"]
        }
    }

    return found
}
