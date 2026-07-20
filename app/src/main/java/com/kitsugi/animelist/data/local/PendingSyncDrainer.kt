package com.kitsugi.animelist.data.local

import android.content.Context
import com.kitsugi.animelist.data.auth.ExternalListSyncManager
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Bekleyen (başarısız) sync işlemlerini kuyruğa yazar ve boşaltır.
 *
 * Akış:
 *  1. Sync başarısız → [enqueue] ile kuyruğa yaz
 *  2. Uygulama açılışı / her başarılı işlem sonrası → [drain] çağır
 *  3. [drain]: kuyruktaki her işlemi dene, başarılıysa sil, değilse retry++ yap
 *  4. retryCount >= MAX_RETRIES olanlara → stale temizle
 */
object PendingSyncDrainer {

    private const val MAX_RETRIES = 5

    // ────────────────────────────────────────────────
    // Kuyruğa Ekleme
    // ────────────────────────────────────────────────

    suspend fun enqueue(
        dao: PendingSyncDao,
        operation: String,
        entry: MediaEntry
    ) = withContext(Dispatchers.IO) {
        runCatching {
            dao.insert(
                PendingSyncEntity(
                    operation = operation,
                    entryJson = entry.toJson()
                )
            )
        }
    }

    // ────────────────────────────────────────────────
    // Kuyruğu Boşalt
    // ────────────────────────────────────────────────

    /**
     * Kuyruktaki tüm bekleyen işlemleri dener.
     * Başarılı olanları siler, başarısız olanların retryCount'unu artırır.
     * MAX_RETRIES'a ulaşan stale kayıtları temizler.
     *
     * @return Başarıyla gönderilen işlem sayısı
     */
    suspend fun drain(
        context: Context,
        dao: PendingSyncDao
    ): Int = withContext(Dispatchers.IO) {
        // Önce stale olanları temizle
        dao.deleteStale(MAX_RETRIES)

        val pending = dao.getAll()
        if (pending.isEmpty()) return@withContext 0

        var successCount = 0

        for (item in pending) {
            val entry = runCatching { item.entryJson.toMediaEntry() }.getOrNull()
            if (entry == null) {
                // Parse edilemeyen bozuk kayıt → sil
                dao.deleteById(item.id)
                continue
            }

            val result = runCatching {
                when (item.operation) {
                    "UPDATE" -> ExternalListSyncManager.syncEntry(context, entry)
                    "DELETE" -> ExternalListSyncManager.deleteEntry(context, entry)
                    else -> null
                }
            }

            val syncResult = result.getOrNull()
            val success = result.isSuccess && syncResult != null && syncResult.errors.isEmpty()

            if (success) {
                dao.deleteById(item.id)
                successCount++
            } else {
                val allErrors = syncResult?.errors.orEmpty().joinToString(" | ")
                val isAuthError = allErrors.contains("401") || allErrors.contains("token geçersiz") || allErrors.contains("Unauthorized")
                if (!isAuthError) {
                    dao.incrementRetry(item.id)
                }
            }
        }

        successCount
    }

    // ────────────────────────────────────────────────
    // JSON Serializasyon / Deserializasyon
    // ────────────────────────────────────────────────

    private fun MediaEntry.toJson(): String {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("subtitle", subtitle)
            put("type", type.name)
            put("status", status.name)
            put("score", score ?: JSONObject.NULL)
            put("progress", progress)
            put("total", total ?: JSONObject.NULL)
            put("isFavorite", isFavorite)
            put("isAdult", isAdult)
            put("source", source)
            put("malId", malId ?: JSONObject.NULL)
            put("imageUrl", imageUrl ?: JSONObject.NULL)
            put("year", year ?: JSONObject.NULL)
            put("synopsis", synopsis ?: JSONObject.NULL)
            put("startDate", startDate ?: JSONObject.NULL)
            put("endDate", endDate ?: JSONObject.NULL)
            put("notes", notes ?: JSONObject.NULL)
            put("tags", tags ?: JSONObject.NULL)
            put("priority", priority ?: JSONObject.NULL)
            put("isRepeating", isRepeating)
            put("repeatCount", repeatCount)
            put("repeatValue", repeatValue)
            put("volumeProgress", volumeProgress)
            put("isPrivate", isPrivate)
            put("isHiddenFromStatusLists", isHiddenFromStatusLists)
            put("aniListEntryId", aniListEntryId ?: JSONObject.NULL)
            put("tmdbId", tmdbId ?: JSONObject.NULL)
            put("simklId", simklId ?: JSONObject.NULL)
        }.toString()
    }

    private fun String.toMediaEntry(): MediaEntry {
        val j = JSONObject(this)
        return MediaEntry(
            id = j.optInt("id", 0),
            title = j.optString("title", ""),
            subtitle = j.optString("subtitle", ""),
            type = runCatching { MediaType.valueOf(j.optString("type")) }.getOrDefault(MediaType.Anime),
            status = runCatching { WatchStatus.valueOf(j.optString("status")) }.getOrDefault(WatchStatus.Planned),
            score = j.opt("score")?.takeIf { it != JSONObject.NULL } as? Int,
            progress = j.optInt("progress", 0),
            total = j.opt("total")?.takeIf { it != JSONObject.NULL } as? Int,
            isFavorite = j.optBoolean("isFavorite", false),
            isAdult = j.optBoolean("isAdult", false),
            source = j.optString("source", "manual"),
            malId = j.opt("malId")?.takeIf { it != JSONObject.NULL } as? Int,
            imageUrl = j.optString("imageUrl").takeIf { it.isNotBlank() && it != "null" },
            year = j.opt("year")?.takeIf { it != JSONObject.NULL } as? Int,
            synopsis = j.optString("synopsis").takeIf { it.isNotBlank() && it != "null" },
            startDate = j.optString("startDate").takeIf { it.isNotBlank() && it != "null" },
            endDate = j.optString("endDate").takeIf { it.isNotBlank() && it != "null" },
            notes = j.optString("notes").takeIf { it.isNotBlank() && it != "null" },
            tags = j.optString("tags").takeIf { it.isNotBlank() && it != "null" },
            priority = j.opt("priority")?.takeIf { it != JSONObject.NULL } as? Int,
            isRepeating = j.optBoolean("isRepeating", false),
            repeatCount = j.optInt("repeatCount", 0),
            repeatValue = j.optInt("repeatValue", 0),
            volumeProgress = j.optInt("volumeProgress", 0),
            isPrivate = j.optBoolean("isPrivate", false),
            isHiddenFromStatusLists = j.optBoolean("isHiddenFromStatusLists", false),
            aniListEntryId = j.opt("aniListEntryId")?.takeIf { it != JSONObject.NULL } as? Int,
            tmdbId = j.opt("tmdbId")?.takeIf { it != JSONObject.NULL } as? Int,
            simklId = j.opt("simklId")?.takeIf { it != JSONObject.NULL } as? Int
        )
    }
}
