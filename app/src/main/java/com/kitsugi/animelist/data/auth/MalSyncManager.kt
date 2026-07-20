package com.kitsugi.animelist.data.auth

import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull

object MalSyncManager {
    fun updateMalEntry(
        token: String,
        entry: MediaEntry
    ) {
        val malId = entry.malId ?: return
        if (!malId.isRealMalId()) return

        val endpointType = when (entry.type) {
            MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "anime"
            MediaType.Manga -> "manga"
        }

        val progressKey = when (entry.type) {
            MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "num_watched_episodes"
            MediaType.Manga -> "num_chapters_read"
        }

        val url = URL("https://api.myanimelist.net/v2/$endpointType/$malId/my_list_status")

        val postData = buildString {
            append("status=${URLEncoder.encode(entry.status.toMalStatus(entry.type), "UTF-8")}")
            append("&score=${entry.score ?: 0}")
            append("&$progressKey=${entry.progress}")

            val startDate = entry.startDate?.takeIf { it.isNotBlank() }
            if (startDate != null) {
                append("&start_date=${URLEncoder.encode(startDate, "UTF-8")}")
            }

            val endDate = entry.endDate?.takeIf { it.isNotBlank() }
            if (endDate != null) {
                append("&finish_date=${URLEncoder.encode(endDate, "UTF-8")}")
            }

            append("&priority=${entry.priority ?: 0}")

            if (entry.type == MediaType.Manga) {
                append("&is_rereading=${entry.isRepeating || entry.status == WatchStatus.Repeating}")
                append("&num_volumes_read=${entry.volumeProgress}")
                append("&num_times_reread=${entry.repeatCount}")
                append("&reread_value=${entry.repeatValue}")
            } else {
                append("&is_rewatching=${entry.isRepeating || entry.status == WatchStatus.Repeating}")
                append("&num_times_rewatched=${entry.repeatCount}")
                append("&rewatch_value=${entry.repeatValue}")
            }

            entry.tags?.takeIf { it.isNotBlank() }?.let {
                append("&tags=${URLEncoder.encode(it, "UTF-8")}")
            }
            entry.notes?.takeIf { it.isNotBlank() }?.let {
                append("&comments=${URLEncoder.encode(it, "UTF-8")}")
            }
        }

        val request = Request.Builder()
            .url(url)
            .patch(postData.toRequestBody("application/x-www-form-urlencoded; charset=utf-8".toMediaTypeOrNull()))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Authorization", "Bearer $token")
            .build()

        try {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorText = response.body?.string().orEmpty()
                    throw IllegalStateException("MAL API hatası: ${response.code} $errorText")
                }
            }
        } catch (e: Exception) {
            if (e is IllegalStateException) throw e
            throw IllegalStateException("MAL bağlantı hatası: ${e.message}", e)
        }
    }

    fun deleteMalEntry(
        token: String,
        entry: MediaEntry
    ) {
        val malId = entry.malId ?: return
        if (!malId.isRealMalId()) return

        val endpointType = when (entry.type) {
            MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "anime"
            MediaType.Manga -> "manga"
        }

        val url = URL("https://api.myanimelist.net/v2/$endpointType/$malId/my_list_status")

        val request = Request.Builder()
            .url(url)
            .delete()
            .header("Authorization", "Bearer $token")
            .build()

        try {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return
                }
                if (!response.isSuccessful) {
                    val errorText = response.body?.string().orEmpty()
                    throw IllegalStateException("MAL silme hatası: ${response.code} $errorText")
                }
            }
        } catch (e: Exception) {
            if (e is IllegalStateException) throw e
            throw IllegalStateException("MAL bağlantı hatası: ${e.message}", e)
        }
    }

    private fun WatchStatus.toMalStatus(mediaType: MediaType): String {
        return when (this) {
            WatchStatus.Watching -> if (mediaType == MediaType.Manga) "reading" else "watching"
            WatchStatus.Completed -> "completed"
            WatchStatus.Planned -> if (mediaType == MediaType.Manga) "plan_to_read" else "plan_to_watch"
            WatchStatus.Dropped -> "dropped"
            WatchStatus.Paused -> "on_hold"
            WatchStatus.Repeating -> if (mediaType == MediaType.Manga) "reading" else "watching"
        }
    }

    private fun Int.isRealMalId(): Boolean {
        return this > 0 && this < 100_000_000
    }
}
