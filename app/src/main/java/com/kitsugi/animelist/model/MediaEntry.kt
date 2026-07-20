package com.kitsugi.animelist.model

enum class MediaType {
    Anime,
    Manga,
    Movie,
    TvShow
}

enum class WatchStatus(
    val label: String
) {
    Watching("İzleniyor"),
    Completed("Tamamlandı"),
    Planned("Planlandı"),
    Dropped("Bırakıldı"),
    Paused("Durduruldu"),
    Repeating("Yeniden İzleniyor")
}

data class MediaEntry(
    val id: Int,
    val title: String,
    val subtitle: String,
    val type: MediaType,
    val status: WatchStatus,
    val score: Int?,
    val progress: Int,
    val total: Int?,
    val isFavorite: Boolean = false,
    val isAdult: Boolean = false,
    val source: String = "manual",
    val malId: Int? = null,
    val imageUrl: String? = null,
    val year: Int? = null,
    val synopsis: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val notes: String? = null,
    val tags: String? = null,
    val priority: Int? = null,
    val isRepeating: Boolean = false,
    val repeatCount: Int = 0,
    val repeatValue: Int = 0,
    val volumeProgress: Int = 0,
    val isPrivate: Boolean = false,
    val isHiddenFromStatusLists: Boolean = false,
    val updatedAt: Long = 0L,
    val titleEnglish: String? = null,
    val titleJapanese: String? = null,
    val aniListEntryId: Int? = null,
    val malListId: Long? = null,
    val tmdbId: Int? = null,
    val simklId: Int? = null
)