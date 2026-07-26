package com.kitsugi.animelist.ui.screens.mylist

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.model.MediaEntry

internal fun applySort(
    entries: List<MediaEntry>,
    sortId: String
): List<MediaEntry> {
    return when (sortId) {
        "newest" -> entries.sortedByDescending { it.id }
        "oldest" -> entries.sortedBy { it.id }
        "title" -> entries.sortedBy { it.title.lowercase() }
        "title_desc" -> entries.sortedByDescending { it.title.lowercase() }
        "score" -> entries.sortedWith(
            compareByDescending<MediaEntry> { it.score ?: -1 }.thenBy { it.title.lowercase() }
        )
        "score_asc" -> entries.sortedWith(
            compareBy<MediaEntry> { it.score ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() }
        )
        "progress" -> entries.sortedWith(
            compareByDescending<MediaEntry> { it.progress }.thenBy { it.title.lowercase() }
        )
        "progress_asc" -> entries.sortedWith(
            compareBy<MediaEntry> { it.progress }.thenBy { it.title.lowercase() }
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
        "updated_asc" -> entries.sortedWith(
            compareBy<MediaEntry> { it.updatedAt }.thenBy { it.title.lowercase() }
        )
        "repeat_desc" -> entries.sortedWith(
            compareByDescending<MediaEntry> { it.repeatCount }.thenBy { it.title.lowercase() }
        )
        "repeat_asc" -> entries.sortedWith(
            compareBy<MediaEntry> { it.repeatCount }.thenBy { it.title.lowercase() }
        )
        "priority_desc" -> entries.sortedWith(
            compareByDescending<MediaEntry> { it.priority ?: 0 }.thenBy { it.title.lowercase() }
        )
        "priority_asc" -> entries.sortedWith(
            compareBy<MediaEntry> { it.priority ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() }
        )
        else -> entries.sortedByDescending { it.id }
    }
}

internal fun getSortTitle(sortId: String): String {
    return when (sortId) {
        "newest" -> "Son eklenen"
        "oldest" -> "İlk eklenen"
        "title" -> "Başlık (A-Z)"
        "title_desc" -> "Başlık (Z-A)"
        "score" -> "Puan (En yüksek)"
        "score_asc" -> "Puan (En düşük)"
        "progress" -> "İlerleme (En çok)"
        "progress_asc" -> "İlerleme (En az)"
        "favorites" -> "Favoriler"
        "start_date_desc" -> "Başlangıç (Yeni)"
        "start_date_asc" -> "Başlangıç (Eski)"
        "end_date_desc" -> "Bitiş (Yeni)"
        "end_date_asc" -> "Bitiş (Eski)"
        "year_desc" -> "Yayın Yılı (Yeni)"
        "year_asc" -> "Yayın Yılı (Eski)"
        "updated_desc" -> "Son Güncelleme"
        "updated_asc" -> "Güncelleme (Eski)"
        "repeat_desc" -> "Tekrar Sayısı (Çok)"
        "repeat_asc" -> "Tekrar Sayısı (Az)"
        "priority_desc" -> "Öncelik (Yüksek)"
        "priority_asc" -> "Öncelik (Düşük)"
        else -> "Son eklenen"
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
