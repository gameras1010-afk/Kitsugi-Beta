package com.kitsugi.animelist.ui.screens.mylist

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.model.MediaEntry

internal fun applySort(
    entries: List<MediaEntry>,
    sortId: String
): List<MediaEntry> {
    return when (sortId) {
        "oldest" -> entries.sortedBy { it.id }
        "title" -> entries.sortedBy { it.title.lowercase() }
        "score" -> entries.sortedWith(
            compareByDescending<MediaEntry> { it.score ?: -1 }.thenBy { it.title.lowercase() }
        )
        "progress" -> entries.sortedWith(
            compareByDescending<MediaEntry> { it.progress }.thenBy { it.title.lowercase() }
        )
        "favorites" -> entries.sortedWith(
            compareByDescending<MediaEntry> { it.isFavorite }.thenBy { it.title.lowercase() }
        )
        "start_date_desc" -> entries.sortedWith(
            compareByDescending<MediaEntry> { it.startDate ?: "" }.thenBy { it.title.lowercase() }
        )
        "start_date_asc" -> entries.sortedWith(
            compareBy<MediaEntry> { if (it.startDate.isNullOrBlank()) "9999" else it.startDate }
                .thenBy { it.title.lowercase() }
        )
        "end_date_desc" -> entries.sortedWith(
            compareByDescending<MediaEntry> { it.endDate ?: "" }.thenBy { it.title.lowercase() }
        )
        "end_date_asc" -> entries.sortedWith(
            compareBy<MediaEntry> { if (it.endDate.isNullOrBlank()) "9999" else it.endDate }
                .thenBy { it.title.lowercase() }
        )
        "year_desc" -> entries.sortedWith(
            compareByDescending<MediaEntry> { it.year ?: 0 }.thenBy { it.title.lowercase() }
        )
        "year_asc" -> entries.sortedWith(
            compareBy<MediaEntry> { it.year ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() }
        )
        "updated_desc" -> entries.sortedWith(
            compareByDescending<MediaEntry> { it.updatedAt }.thenBy { it.title.lowercase() }
        )
        "priority_desc" -> entries.sortedWith(
            compareByDescending<MediaEntry> { it.priority ?: 0 }.thenBy { it.title.lowercase() }
        )
        else -> entries.sortedByDescending { it.id }
    }
}

internal fun cardSpacingForLayout(layoutId: String): Dp {
    return when (layoutId) {
        "compact"    -> 8.dp
        "large"      -> 18.dp
        "grid_2col"  -> 8.dp
        else         -> 12.dp  // comfortable (varsayılan)
    }
}
