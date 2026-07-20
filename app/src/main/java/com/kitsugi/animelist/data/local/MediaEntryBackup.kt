package com.kitsugi.animelist.data.local

import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import org.json.JSONArray
import org.json.JSONObject

data class BackupPreview(
    val totalCount: Int,
    val animeCount: Int,
    val mangaCount: Int,
    val apiCount: Int,
    val favoriteCount: Int
)

object MediaEntryBackup {
    private const val ANILIST_SYNTHETIC_ID_OFFSET = 100_000_000

    fun exportToJson(entries: List<MediaEntry>): String {
        val array = JSONArray()

        entries.forEach { entry ->
            val item = JSONObject()
                .put("title", entry.title)
                .put("subtitle", entry.subtitle)
                .put("type", entry.type.name)
                .put("status", entry.status.name)
                .put("score", entry.score)
                .put("progress", entry.progress)
                .put("total", entry.total)
                .put("isFavorite", entry.isFavorite)
                .put("isAdult", entry.isAdult)
                .put("source", entry.source)
                .put("malId", entry.malId)
                .put("imageUrl", entry.imageUrl)
                .put("year", entry.year)
                .put("synopsis", entry.synopsis)
                .put("aniListEntryId", entry.aniListEntryId)

            array.put(item)
        }

        val root = JSONObject()
            .put("schemaVersion", 1)
            .put("app", "Kitsugi")
            .put("entries", array)

        return root.toString(2)
    }

    fun previewJson(jsonText: String): BackupPreview {
        val entries = importFromJson(jsonText)

        return BackupPreview(
            totalCount = entries.size,
            animeCount = entries.count { it.type == MediaType.Anime },
            mangaCount = entries.count { it.type == MediaType.Manga },
            apiCount = entries.count { it.source != "manual" },
            favoriteCount = entries.count { it.isFavorite }
        )
    }

    fun importFromJson(jsonText: String): List<MediaEntry> {
        val root = JSONObject(jsonText)
        val array = root.optJSONArray("entries")
            ?: throw IllegalArgumentException("Yedek içinde entries alanı bulunamadı.")

        val result = mutableListOf<MediaEntry>()

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue

            val title = item.optString("title").trim()
            if (title.isBlank()) continue

            val type = runCatching {
                MediaType.valueOf(item.optString("type"))
            }.getOrDefault(MediaType.Anime)

            val status = runCatching {
                WatchStatus.valueOf(item.optString("status"))
            }.getOrDefault(WatchStatus.Planned)

            val entry = MediaEntry(
                id = 0,
                title = title,
                subtitle = item.optString("subtitle").ifBlank {
                    "İçe aktarılan içerik"
                },
                type = type,
                status = status,
                score = item.optionalInt("score"),
                progress = item.optInt("progress", 0).coerceAtLeast(0),
                total = item.optionalInt("total"),
                isFavorite = item.optBoolean("isFavorite", false),
                isAdult = item.optBoolean("isAdult", false),
                source = item.optString("source").ifBlank {
                    "manual"
                },
                malId = item.optionalInt("malId"),
                imageUrl = item.optString("imageUrl").takeIf {
                    it.isNotBlank() && it != "null"
                },
                year = item.optionalInt("year"),
                synopsis = item.optString("synopsis").takeIf {
                    it.isNotBlank() && it != "null"
                },
                aniListEntryId = item.optionalInt("aniListEntryId")
            )

            result.add(entry)
        }

        if (result.isEmpty()) {
            throw IllegalArgumentException("Yedek içinde geçerli kayıt bulunamadı.")
        }

        return result
    }

    private fun isRealMalId(id: Int?): Boolean {
        return id != null && id > 0 && id < ANILIST_SYNTHETIC_ID_OFFSET
    }

    fun mergeWithoutApiDuplicates(
        currentEntries: List<MediaEntry>,
        importedEntries: List<MediaEntry>
    ): List<MediaEntry> {
        val existingRealMalIds = currentEntries
            .mapNotNull { entry ->
                val malId = entry.malId
                if (entry.source != "manual" && isRealMalId(malId)) malId else null
            }
            .toSet()

        val existingKeys = currentEntries
            .mapNotNull { entry ->
                val malId = entry.malId
                if (entry.source != "manual" && malId != null && !isRealMalId(malId)) {
                    "${canonicalSource(entry.source)}:$malId"
                } else {
                    null
                }
            }
            .toSet()

        val acceptedRealMalIds = mutableSetOf<Int>()
        val acceptedKeys = mutableSetOf<String>()

        return importedEntries.filter { entry ->
            val malId = entry.malId

            if (entry.source == "manual" || malId == null) {
                true
            } else if (isRealMalId(malId)) {
                val isDuplicate = malId in existingRealMalIds || malId in acceptedRealMalIds
                if (!isDuplicate) {
                    acceptedRealMalIds.add(malId)
                }
                !isDuplicate
            } else {
                val key = "${canonicalSource(entry.source)}:$malId"
                val isDuplicate = key in existingKeys || key in acceptedKeys
                if (!isDuplicate) {
                    acceptedKeys.add(key)
                }
                !isDuplicate
            }
        }
    }

    fun mergeAndSyncEntries(
        currentEntries: List<MediaEntry>,
        importedEntries: List<MediaEntry>
    ): MergeResult {
        val toInsert = mutableListOf<MediaEntry>()
        val toUpdate = mutableListOf<MediaEntry>()

        val existingByMalId = currentEntries
            .filter { it.malId != null }
            .associateBy { it.malId!! }

        val existingByTitle = currentEntries
            .filter { it.malId == null }
            .associateBy { it.title.lowercase().trim() }

        importedEntries.forEach { imported ->
            val malId = imported.malId
            val existing = if (malId != null) {
                existingByMalId[malId]
            } else {
                existingByTitle[imported.title.lowercase().trim()]
            }

            if (existing != null) {
                // Enrich existing local entry with remote sync status
                val updated = existing.copy(
                    title = if (imported.title.isNotBlank()) imported.title else existing.title,
                    subtitle = if (imported.subtitle.isNotBlank() && imported.subtitle != "MyAnimeList'ten içe aktarıldı" && imported.subtitle != "AniList'ten içe aktarıldı") imported.subtitle else existing.subtitle,
                    status = imported.status,
                    score = imported.score ?: existing.score,
                    progress = imported.progress,
                    total = imported.total ?: existing.total,
                    isFavorite = imported.isFavorite,
                    imageUrl = imported.imageUrl ?: existing.imageUrl,
                    year = imported.year ?: existing.year,
                    notes = imported.notes ?: existing.notes,
                    tags = imported.tags ?: existing.tags,
                    priority = imported.priority ?: existing.priority,
                    isRepeating = imported.isRepeating,
                    repeatCount = imported.repeatCount,
                    repeatValue = imported.repeatValue,
                    volumeProgress = imported.volumeProgress,
                    startDate = imported.startDate ?: existing.startDate,
                    endDate = imported.endDate ?: existing.endDate,
                    malId = malId ?: existing.malId
                )
                if (updated != existing) {
                    toUpdate.add(updated)
                }
            } else {
                toInsert.add(imported)
            }
        }

        return MergeResult(toInsert = toInsert, toUpdate = toUpdate)
    }

    data class MergeResult(
        val toInsert: List<MediaEntry>,
        val toUpdate: List<MediaEntry>
    )

    private fun canonicalSource(source: String): String {
        val lower = source.lowercase()
        if (lower == "mal" || lower == "jikan") return "mal"
        return lower
    }

    private fun JSONObject.optionalInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null

        val raw = opt(key)
        return when (raw) {
            is Int -> raw
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull()
            else -> null
        }
    }
}