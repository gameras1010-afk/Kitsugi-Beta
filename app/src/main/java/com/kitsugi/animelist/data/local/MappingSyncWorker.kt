package com.kitsugi.animelist.data.local

import android.content.Context
import android.util.JsonReader
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kitsugi.animelist.core.network.KitsugiHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.InputStreamReader

class MappingSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("MappingSyncWorker", "Starting offline database mapping sync...")
        val db = KitsugiDatabase.getDatabase(applicationContext)
        val dao = db.mediaMetaCacheDao()

        // 30 günden eski detay önbelleğini otomatik temizle (camış gibi şişmemesi için)
        runCatching {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
            db.persistentDetailCacheDao().deleteOlderThan(thirtyDaysAgo)
            Log.d("MappingSyncWorker", "Cleaned up persistent detail cache entries older than 30 days.")
        }

        try {
            val request = Request.Builder()
                .url("https://raw.githubusercontent.com/manami-project/anime-offline-database/master/anime-offline-database.min.json")
                .build()

            KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("MappingSyncWorker", "Failed to download database: ${response.code}")
                    return@withContext Result.retry()
                }

                val bodyStream = response.body?.byteStream() ?: return@withContext Result.failure()
                JsonReader(InputStreamReader(bodyStream, "UTF-8")).use { reader ->
                    reader.beginObject()
                    while (reader.hasNext()) {
                        val name = reader.nextName()
                        if (name == "data") {
                            reader.beginArray()
                            val batch = mutableListOf<MediaMetaCacheEntity>()
                            while (reader.hasNext()) {
                                val entity = parseEntry(reader)
                                if (entity != null) {
                                    batch.add(entity)
                                    if (batch.size >= 200) {
                                        insertBatch(dao, batch)
                                        batch.clear()
                                    }
                                }
                            }
                            if (batch.isNotEmpty()) {
                                insertBatch(dao, batch)
                            }
                            reader.endArray()
                        } else {
                            reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
            }
            Log.d("MappingSyncWorker", "Offline database mapping sync completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e("MappingSyncWorker", "Error during mapping sync: ${e.message}", e)
            Result.retry()
        }
    }

    private fun parseEntry(reader: JsonReader): MediaMetaCacheEntity? {
        reader.beginObject()
        var sources: List<String>? = null
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (name == "sources") {
                sources = mutableListOf<String>().apply {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        add(reader.nextString())
                    }
                    reader.endArray()
                }
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()

        if (sources == null) return null

        var malId: Int? = null
        var aniListId: Int? = null
        var tmdbId: Int? = null
        var kitsuId: String? = null

        for (source in sources) {
            if (source.contains("myanimelist.net/anime/")) {
                malId = source.substringAfter("myanimelist.net/anime/").substringBefore("/").toIntOrNull()
            } else if (source.contains("anilist.co/anime/")) {
                aniListId = source.substringAfter("anilist.co/anime/").substringBefore("/").toIntOrNull()
            } else if (source.contains("themoviedb.org/tv/") || source.contains("themoviedb.org/movie/")) {
                val segment = if (source.contains("themoviedb.org/tv/")) "themoviedb.org/tv/" else "themoviedb.org/movie/"
                tmdbId = source.substringAfter(segment).substringBefore("/").toIntOrNull()
            } else if (source.contains("kitsu.io/anime/")) {
                kitsuId = source.substringAfter("kitsu.io/anime/").substringBefore("/").trim().takeIf { it.isNotEmpty() }
            }
        }

        if (tmdbId != null && tmdbId > 0) {
            return MediaMetaCacheEntity(
                tmdbId = tmdbId,
                malId = malId,
                aniListId = aniListId,
                logoUrl = null,
                logoNotFound = false,
                cachedAtMs = System.currentTimeMillis(),
                kitsuId = kitsuId
            )
        }
        return null
    }

    private suspend fun insertBatch(dao: MediaMetaCacheDao, batch: List<MediaMetaCacheEntity>) {
        for (item in batch) {
            val existing = dao.getByTmdbId(item.tmdbId)
            if (existing != null) {
                val updated = item.copy(
                    logoUrl = existing.logoUrl,
                    logoNotFound = existing.logoNotFound
                )
                dao.insert(updated)
            } else {
                dao.insert(item)
            }
        }
    }
}
