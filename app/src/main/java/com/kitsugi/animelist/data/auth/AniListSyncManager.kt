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
        entry: MediaEntry,
        advancedScores: List<Double>? = null
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
                ${'$'}hiddenFromStatusLists: Boolean,
                ${'$'}advancedScores: [Float]
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
                    hiddenFromStatusLists: ${'$'}hiddenFromStatusLists,
                    advancedScores: ${'$'}advancedScores
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

        if (advancedScores != null) {
            val jsonArray = org.json.JSONArray()
            advancedScores.forEach { jsonArray.put(it) }
            variables.put("advancedScores", jsonArray)
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

    fun resolveAniListMediaId(
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

    fun fetchAniListUserId(
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

    data class ViewerOptions(
        val scoreFormat: String,
        val animeCategories: List<String>,
        val animeEnabled: Boolean,
        val mangaCategories: List<String>,
        val mangaEnabled: Boolean
    )

    fun getViewerOptions(token: String): ViewerOptions? {
        val query = """
            query {
                Viewer {
                    mediaListOptions {
                        scoreFormat
                        animeList {
                            advancedScoring
                            advancedScoringEnabled
                        }
                        mangaList {
                            advancedScoring
                            advancedScoringEnabled
                        }
                    }
                }
            }
        """.trimIndent()

        return try {
            val response = postAniList(token = token, query = query, variables = JSONObject())
            val viewer = JSONObject(response)
                .optJSONObject("data")
                ?.optJSONObject("Viewer")
            val mediaListOptions = viewer?.optJSONObject("mediaListOptions")
            val scoreFormat = mediaListOptions?.optString("scoreFormat", "POINT_10") ?: "POINT_10"
            
            val animeList = mediaListOptions?.optJSONObject("animeList")
            val animeEnabled = animeList?.optBoolean("advancedScoringEnabled", false) ?: false
            val animeCatsArray = animeList?.optJSONArray("advancedScoring")
            val animeCategories = mutableListOf<String>()
            if (animeCatsArray != null) {
                for (i in 0 until animeCatsArray.length()) {
                    animeCategories.add(animeCatsArray.optString(i))
                }
            }

            val mangaList = mediaListOptions?.optJSONObject("mangaList")
            val mangaEnabled = mangaList?.optBoolean("advancedScoringEnabled", false) ?: false
            val mangaCatsArray = mangaList?.optJSONArray("advancedScoring")
            val mangaCategories = mutableListOf<String>()
            if (mangaCatsArray != null) {
                for (i in 0 until mangaCatsArray.length()) {
                    mangaCategories.add(mangaCatsArray.optString(i))
                }
            }

            ViewerOptions(
                scoreFormat = scoreFormat,
                animeCategories = animeCategories,
                animeEnabled = animeEnabled,
                mangaCategories = mangaCategories,
                mangaEnabled = mangaEnabled
            )
        } catch (e: java.lang.Exception) {
            null
        }
    }

    data class MediaListMetadata(
        val customLists: Map<String, Boolean>?,
        val advancedScores: Map<String, Double>?
    )

    fun getMediaListMetadata(
        token: String,
        entryId: Int,
        userId: Int
    ): MediaListMetadata? {
        val query = """
            query (${'$'}id: Int, ${'$'}userId: Int) {
                MediaList(id: ${'$'}id, userId: ${'$'}userId) {
                    customLists
                    advancedScores
                }
            }
        """.trimIndent()

        val variables = JSONObject()
            .put("id", entryId)
            .put("userId", userId)

        return try {
            val response = postAniList(token = token, query = query, variables = variables)
            val mediaList = JSONObject(response)
                .optJSONObject("data")
                ?.optJSONObject("MediaList") ?: return null

            val jsonCustomLists = mediaList.optJSONObject("customLists")
            val customListsMap = mutableMapOf<String, Boolean>()
            if (jsonCustomLists != null) {
                val keys = jsonCustomLists.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    customListsMap[key] = jsonCustomLists.optBoolean(key, false)
                }
            }

            val jsonAdvancedScores = mediaList.optJSONObject("advancedScores")
            val advancedScoresMap = mutableMapOf<String, Double>()
            if (jsonAdvancedScores != null) {
                val keys = jsonAdvancedScores.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    advancedScoresMap[key] = jsonAdvancedScores.optDouble(key, 0.0)
                }
            }

            MediaListMetadata(customLists = customListsMap, advancedScores = advancedScoresMap)
        } catch (e: java.lang.Exception) {
            null
        }
    }

    fun getViewerCustomLists(
        token: String,
        isManga: Boolean
    ): List<String>? {
        val query = """
            query {
                Viewer {
                    mediaListOptions {
                        animeList {
                            customLists
                        }
                        mangaList {
                            customLists
                        }
                    }
                }
            }
        """.trimIndent()

        return try {
            val response = postAniList(token = token, query = query, variables = JSONObject())
            val viewer = JSONObject(response)
                .optJSONObject("data")
                ?.optJSONObject("Viewer")
            val listOptions = viewer?.optJSONObject("mediaListOptions")
            val list = if (isManga) {
                listOptions?.optJSONObject("mangaList")
            } else {
                listOptions?.optJSONObject("animeList")
            }
            val customListsArray = list?.optJSONArray("customLists")
            val result = mutableListOf<String>()
            if (customListsArray != null) {
                for (i in 0 until customListsArray.length()) {
                    val item = customListsArray.optString(i)
                    if (!item.isNullOrBlank()) {
                        result.add(item)
                    }
                }
            }
            result
        } catch (e: java.lang.Exception) {
            null
        }
    }

    fun updateEntryCustomLists(
        token: String,
        mediaId: Int?,
        entryId: Int?,
        customLists: List<String>
    ): Boolean {
        val query = """
            mutation (${'$'}id: Int, ${'$'}mediaId: Int, ${'$'}customLists: [String]) {
                SaveMediaListEntry(id: ${'$'}id, mediaId: ${'$'}mediaId, customLists: ${'$'}customLists) {
                    id
                }
            }
        """.trimIndent()

        val jsonArray = org.json.JSONArray()
        customLists.forEach { jsonArray.put(it) }

        val variables = JSONObject()
        if (entryId != null && entryId > 0) {
            variables.put("id", entryId)
        } else if (mediaId != null) {
            variables.put("mediaId", mediaId)
        } else {
            return false
        }
        variables.put("customLists", jsonArray)

        return try {
            postAniList(token = token, query = query, variables = variables)
            true
        } catch (e: java.lang.Exception) {
            false
        }
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
