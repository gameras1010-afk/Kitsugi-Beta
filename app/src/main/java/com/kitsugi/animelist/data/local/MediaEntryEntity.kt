package com.kitsugi.animelist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus

@Entity(tableName = "media_entries")
data class MediaEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val subtitle: String,
    val type: String,
    val status: String,
    val score: Int?,
    val progress: Int,
    val total: Int?,
    val isFavorite: Boolean,
    val isAdult: Boolean,
    val source: String,
    val malId: Int?,
    val imageUrl: String?,
    val year: Int?,
    val synopsis: String?,
    val startDate: String?,
    val endDate: String?,
    val notes: String?,
    val tags: String?,
    val priority: Int?,
    val isRepeating: Boolean,
    val repeatCount: Int,
    val repeatValue: Int,
    val volumeProgress: Int,
    val isPrivate: Boolean,
    val isHiddenFromStatusLists: Boolean,
    val titleEnglish: String? = null,
    val titleJapanese: String? = null,
    val aniListEntryId: Int? = null,
    val malListId: Long? = null,
    val tmdbId: Int? = null,
    val simklId: Int? = null
)

fun MediaEntryEntity.toDomain(): MediaEntry {
    return MediaEntry(
        id = id,
        title = title,
        subtitle = subtitle,
        type = runCatching {
            MediaType.valueOf(type)
        }.getOrDefault(MediaType.Anime),
        status = runCatching {
            WatchStatus.valueOf(status)
        }.getOrDefault(WatchStatus.Planned),
        score = score,
        progress = progress,
        total = total,
        isFavorite = isFavorite,
        isAdult = isAdult,
        source = source,
        malId = malId,
        imageUrl = imageUrl,
        year = year,
        synopsis = synopsis,
        startDate = startDate,
        endDate = endDate,
        notes = notes,
        tags = tags,
        priority = priority,
        isRepeating = isRepeating,
        repeatCount = repeatCount,
        repeatValue = repeatValue,
        volumeProgress = volumeProgress,
        isPrivate = isPrivate,
        isHiddenFromStatusLists = isHiddenFromStatusLists,
        titleEnglish = titleEnglish,
        titleJapanese = titleJapanese,
        aniListEntryId = aniListEntryId,
        malListId = malListId,
        tmdbId = tmdbId,
        simklId = simklId
    )
}

fun MediaEntry.toEntity(): MediaEntryEntity {
    return MediaEntryEntity(
        id = id,
        title = title,
        subtitle = subtitle,
        type = type.name,
        status = status.name,
        score = score,
        progress = progress,
        total = total,
        isFavorite = isFavorite,
        isAdult = isAdult,
        source = source,
        malId = malId,
        imageUrl = imageUrl,
        year = year,
        synopsis = synopsis,
        startDate = startDate,
        endDate = endDate,
        notes = notes,
        tags = tags,
        priority = priority,
        isRepeating = isRepeating,
        repeatCount = repeatCount,
        repeatValue = repeatValue,
        volumeProgress = volumeProgress,
        isPrivate = isPrivate,
        isHiddenFromStatusLists = isHiddenFromStatusLists,
        titleEnglish = titleEnglish,
        titleJapanese = titleJapanese,
        aniListEntryId = aniListEntryId,
        malListId = malListId,
        tmdbId = tmdbId,
        simklId = simklId
    )
}