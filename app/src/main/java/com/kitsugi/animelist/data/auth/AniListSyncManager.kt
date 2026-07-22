package com.kitsugi.animelist.data.auth

import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull

object AniListSyncManager {
    private const val ANILIST_SYNTHETIC_ID_OFFSET = 100_000_000

    @Volatile private var cachedAniListUserId: Int? = null
    @Volatile private var cachedAniListToken: String? = null

    fun updateAniListEntry(
        token: String,
        entry: MediaEntry
    ): Int? {
        val existingEntryId: Int? = entry.aniListEntryId

        val mediaId: Int? = if (existingEntryId == null) {
            resolveAniListMediaId(token = token, entry = entry)
        } else {
            null
        }

        if (existingEntryId == null && mediaId == null) return null

        val query = """
            mutation (
                ${'$'}id: Int,
                ${'$'}mediaId: Int,
                ${'$'}status: MediaListStatus,
                ${'$'}score: Float,
                ${'$'}progress: Int,
                ${'$'}progressVolumes: Int,
                ${'$'}startedAt: FuzzyDateInput,
                ${'$'}completedAt: FuzzyDateInput,
                ${'$'}private: Boolean,
                ${'$'}notes: String,
                ${'$'}repeat: Int,
                ${'$'}hiddenFromStatusLists: Boolean
            ) {
                SaveMediaListEntry(
                    id: ${'$'}id,
                    mediaId: ${'$'}mediaId,
                    status: ${'$'}status,
                    score: ${'$'}score,
                    progress: ${'$'}progress,
                    progressVolumes: ${'$'}progressVolumes,
                    startedAt: ${'$'}startedAt,
                    completedAt: ${'$'}completedAt,
                    private: ${'$'}private,
                    notes: ${'$'}notes,
                    repeat: ${'$'}repeat,
                    hiddenFromStatusLists: ${'$'}hiddenFromStatusLists
                ) {
                    id
                    status
                    score
                    progress
                }
            }
        """.trimIndent()

        val variables = JSONObject()
        if (existingEntryId != null) {
            variables.put("id", existingEntryId)
        } else {
            variables.put("mediaId", mediaId)
        }
        variables
            .put("status", entry.status.toAniListStatus())
            .put("score", entry.score ?: 0)
            .put("progress", entry.progress)
            .put("progressVolumes", entry.volumeProgress)
            .put("private", entry.isPrivate)
            .put("notes", entry.notes ?: "")
            .put("repeat", entry.repeatCount)
            .put("hiddenFromStatusLists", entry.isHiddenFromStatusLists)

        val startedAt = entry.startDate.toAniListFuzzyDate()
        if (startedAt != null) {
            variables.put("startedAt", startedAt)
        }

        val completedAt = entry.endDate.toAniListFuzzyDate()
        if (completedAt != null) {
            variables.put("completedAt", completedAt)
        }

        val response = postAniList(
            token = token,
            query = query,
            variables = variables
        )

        return JSONObject(response)
            .optJSONObject("data")
            ?.optJSONObject("SaveMediaListEntry")
            ?.optInt("id", 0)
            ?.takeIf { it > 0 }
    }

    /**
     * AniList'teki güncel favori durumunu sorgular.
     */
    fun getAniListMediaFavoriteStatus(
        token: String,
        entry: MediaEntry
    ): Boolean? {
        val aniListMediaId: Int = resolveAniListMediaId(token = token, entry = entry) ?: return null

        val query = """
            query (${'$'}id: Int) {
                Media(id: ${'$'}id) {
                    isFavourite
                }
            }
        """.trimIndent()

        val variables = JSONObject().put("id", aniListMediaId)

        return runCatching {
            val response = postAniList(token = token, query = query, variables = variables)
            JSONObject(response)
                .optJSONObject("data")
                ?.optJSONObject("Media")
                ?.optBoolean("isFavourite", false)
        }.getOrNull()
    }

    /**
     * AniList'te favori toggle yapar.
     * ToggleFavourite mutasyonu ile anime/manga ID'sine göre toggle eder.
     * Bu fonksiyon sadece isFavorite değeri değiştiğinde çağrılmalı.
     * @return true = başarılı, false = API hatası veya mediaId bulunamadı
     */
    fun toggleAniListFavourite(
        token: String,
        entry: MediaEntry
    ): Boolean {
        val aniListMediaId: Int? = resolveAniListMediaId(token = token, entry = entry)

        if (aniListMediaId == null) return false

        val isAnime = entry.type != MediaType.Manga
        val mutationArg = if (isAnime) "\$animeId: Int" else "\$mangaId: Int"
        val mutationParam = if (isAnime) "animeId: \$animeId" else "mangaId: \$mangaId"
        val varKey = if (isAnime) "animeId" else "mangaId"

        val query = """
            mutation ($mutationArg) {
                ToggleFavourite($mutationParam) {
                    anime { nodes { id } }
                    manga { nodes { id } }
                }
            }
        """.trimIndent()

        val variables = JSONObject().put(varKey, aniListMediaId)

        return runCatching {
            postAniList(token = token, query = query, variables = variables)
            true
        }.getOrDefault(false)
    }

    fun deleteAniListEntry(
        token: String,
        entry: MediaEntry
    ) {
        val entryId: Int? = entry.aniListEntryId ?: run {
            val mediaId = resolveAniListMediaId(token = token, entry = entry) ?: return
            val userId = fetchAniListUserId(token)

            val findQuery = """
                query (
                    ${'$'}mediaId: Int,
                    ${'$'}userId: Int
                ) {
                    MediaList(
                        mediaId: ${'$'}mediaId,
                        userId: ${'$'}userId
                    ) {
                        id
                    }
                }
            """.trimIndent()

            val findVariables = JSONObject()
                .put("mediaId", mediaId)
                .put("userId", userId)

            val findResponse = postAniList(
                token = token,
                query = findQuery,
                variables = findVariables
            )

            JSONObject(findResponse)
                .optJSONObject("data")
                ?.optJSONObject("MediaList")
                ?.optInt("id", 0)
                ?.takeIf { it > 0 }
        }

        entryId ?: return

        val deleteQuery = """
            mutation (
                ${'$'}entryId: Int
            ) {
                DeleteMediaListEntry(
                    id: ${'$'}entryId
                ) {
                    deleted
                }
            }
        """.trimIndent()

        val deleteVariables = JSONObject()
            .put("entryId", entryId)

        postAniList(
            token = token,
            query = deleteQuery,
            variables = deleteVariables
        )
    }

    private fun resolveAniListMediaId(
        token: String,
        entry: MediaEntry
    ): Int? {
        val externalId = entry.malId ?: return null

        if (entry.source == "anilist" && externalId >= ANILIST_SYNTHETIC_ID_OFFSET) {
            return externalId - ANILIST_SYNTHETIC_ID_OFFSET
        }

        if (!externalId.isRealMalId()) {
            return null
        }

        val query = """
            query (
                ${'$'}idMal: Int,
                ${'$'}type: MediaType
            ) {
                Media(
                    idMal: ${'$'}idMal,
                    type: ${'$'}type
                ) {
                    id
                }
            }
        """.trimIndent()

        val variables = JSONObject()
            .put("idMal", externalId)
            .put(
                "type",
                when (entry.type) {
                    MediaType.Anime, MediaType.Movie, MediaType.TvShow -> "ANIME"
                    MediaType.Manga -> "MANGA"
                }
            )

        val response = postAniList(
            token = token,
            query = query,
            variables = variables
        )

        return JSONObject(response)
            .optJSONObject("data")
            ?.optJSONObject("Media")
            ?.optInt("id", 0)
            ?.takeIf { it > 0 }
    }

    private fun fetchAniListUserId(
        token: String
    ): Int {
        if (token == cachedAniListToken && cachedAniListUserId != null) {
            return cachedAniListUserId!!
        }

        val query = """
            query {
                Viewer {
                    id
                }
            }
        """.trimIndent()

        val response = postAniList(
            token = token,
            query = query,
            variables = JSONObject()
        )

        val userId = JSONObject(response)
            .optJSONObject("data")
            ?.optJSONObject("Viewer")
            ?.optInt("id", 0)
            ?.takeIf { it > 0 }
            ?: throw IllegalStateException("AniList kullanıcı kimliği alınamadı (token geçersiz olabilir)")

        cachedAniListToken = token
        cachedAniListUserId = userId
        return userId
    }

    private fun postAniList(
        token: String,
        query: String,
        variables: JSONObject
    ): String {
        val payload = JSONObject()
            .put("query", query)
            .put("variables", variables)
            .toString()

        val request = Request.Builder()
            .url("https://graphql.anilist.co")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $token")
            .build()

        try {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorText = response.body?.string().orEmpty()
                    throw IllegalStateException("AniList API hatası: ${response.code} $errorText")
                }

                val responseText = response.body?.string() ?: ""
                val errors = JSONObject(responseText).optJSONArray("errors")
                if (errors != null && errors.length() > 0) {
                    throw IllegalStateException(errors.toString())
                }

                return responseText
            }
        } catch (e: Exception) {
            if (e is IllegalStateException) throw e
            throw IllegalStateException("AniList bağlantı hatası: ${e.message}", e)
        }
    }

    private fun WatchStatus.toAniListStatus(): String {
        return when (this) {
            WatchStatus.Watching -> "CURRENT"
            WatchStatus.Completed -> "COMPLETED"
            WatchStatus.Planned -> "PLANNING"
            WatchStatus.Dropped -> "DROPPED"
            WatchStatus.Paused -> "PAUSED"
            WatchStatus.Repeating -> "REPEATING"
        }
    }

    private fun String?.toAniListFuzzyDate(): JSONObject? {
        if (this.isNullOrBlank()) return null

        val parts = split("-")
        if (parts.size != 3) return null

        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null

        return JSONObject()
            .put("year", year)
            .put("month", month)
            .put("day", day)
    }

    private fun Int.isRealMalId(): Boolean {
        return this > 0 && this < ANILIST_SYNTHETIC_ID_OFFSET
    }
}
