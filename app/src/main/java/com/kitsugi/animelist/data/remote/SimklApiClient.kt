package com.kitsugi.animelist.data.remote

import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.model.MediaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class SimklApiClient(
    private val clientMap: Map<String, String>? = null
) {
    private val client = com.kitsugi.animelist.core.network.KitsugiHttpClient.client
    // T3-01: BuildConfig'den alınır
    private val clientId get() = com.kitsugi.animelist.BuildConfig.SIMKL_CLIENT_ID

    private fun checkResponseAndThrow(response: okhttp3.Response) {
        if (response.code == 401) {
            val context = com.kitsugi.animelist.KitsugiApplication.getInstance()?.applicationContext
            if (context != null) {
                com.kitsugi.animelist.data.auth.ExternalAuthManager.handleSimkl401(context)
            }
            throw com.kitsugi.animelist.data.repository.SimklAuthException("Token revoked (401)")
        }
    }


    suspend fun getTrending(typePath: String): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        // NyanTV referansÄ±: api.simkl.com/trending yerine data.simkl.in CDN kullanÄ±yoruz
        // CDN: https://data.simkl.in/discover/trending/{tv|movies|anime}/today_100.json
        val cdnType = when (typePath) {
            "movies" -> "movies"
            "tv", "shows" -> "tv"
            "anime" -> "anime"
            else -> "tv"
        }
        val cdnUrl = "https://data.simkl.in/discover/trending/$cdnType/today_100.json"
        val request = Request.Builder()
            .url(cdnUrl)
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.e("SimklApiClient", "getTrending $cdnType HTTP ${response.code}")
                    return@withContext emptyList()
                }
                val responseText = response.body?.string().orEmpty()
                val jsonArray = JSONArray(responseText)
                val results = mutableListOf<JikanSearchResult>()

                for (i in 0 until minOf(jsonArray.length(), 30)) {
                    val obj = jsonArray.getJSONObject(i)
                    val title = obj.optString("title", "")
                    val ids = obj.optJSONObject("ids") ?: continue

                    // CDN response'unda ids iÃ§inde "simkl_id" veya "simkl" olabilir (NyanTV referans)
                    val simklId = ids.optInt("simkl_id", 0)
                        .takeIf { it > 0 }
                        ?: ids.optInt("simkl", 0)
                    if (simklId <= 0) continue

                    val tmdbId = ids.optInt("tmdb_id", 0)
                        .takeIf { it > 0 }
                        ?: ids.optInt("tmdb", 0)
                    val malId = ids.optInt("mal", 0)
                    val poster = obj.optString("poster", "")
                    val year = obj.optInt("year", 0)

                    // CDN ratingsObj: ratings â†’ simkl â†’ {rating, votes}
                    val ratingsObj = obj.optJSONObject("ratings")
                    val simklRating = ratingsObj?.optJSONObject("simkl")?.optDouble("rating", 0.0) ?: 0.0
                    val imdbRating = ratingsObj?.optJSONObject("imdb")?.optDouble("rating", 0.0) ?: 0.0
                    val score = if (simklRating > 0.0) (simklRating * 10).toInt()
                                else if (imdbRating > 0.0) (imdbRating * 10).toInt()
                                else null

                    val mediaType = when (cdnType) {
                        "movies" -> MediaType.Movie
                        "tv" -> MediaType.TvShow
                        "anime" -> MediaType.Anime
                        else -> MediaType.TvShow
                    }

                    results.add(
                        JikanSearchResult(
                            malId = simklId,
                            title = title,
                            subtitle = if (year > 0) year.toString() else "",
                            type = mediaType,
                            total = null,
                            score = score,
                            isAdult = false,
                            imageUrl = if (poster.isNotEmpty()) "https://simkl.in/posters/${poster}_m.jpg" else null,
                            year = if (year > 0) year else null,
                            source = "simkl",
                            realMalId = if (mediaType == MediaType.Anime && malId > 0) malId else null,
                            titleEnglish = title,
                            titleJapanese = null,
                            tmdbId = if (tmdbId > 0) tmdbId else null
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            android.util.Log.e("SimklApiClient", "getTrending $cdnType failed: ${e.message}", e)
            emptyList()
        }
    }


    suspend fun getDvdMovies(): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.simkl.com/discover/dvd/week.json?client_id=$clientId")
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val responseText = response.body?.string().orEmpty()
                val jsonArray = JSONArray(responseText)
                val results = mutableListOf<JikanSearchResult>()

                for (i in 0 until minOf(jsonArray.length(), 20)) {
                    val obj = jsonArray.getJSONObject(i)
                    val title = obj.optString("title", "")
                    val ids = obj.optJSONObject("ids") ?: continue
                    val simklId = ids.optInt("simkl", 0)
                    val tmdbId = ids.optInt("tmdb", 0)
                    val poster = obj.optString("poster", "")
                    val year = obj.optInt("year", 0)

                    results.add(
                        JikanSearchResult(
                            malId = simklId,
                            title = title,
                            subtitle = if (year > 0) year.toString() else "",
                            type = MediaType.Movie,
                            total = null,
                            score = null,
                            isAdult = false,
                            imageUrl = if (poster.isNotEmpty()) "https://simkl.in/posters/${poster}_m.jpg" else null,
                            year = if (year > 0) year else null,
                            source = "simkl",
                            realMalId = null,
                            titleEnglish = title,
                            titleJapanese = null,
                            tmdbId = if (tmdbId > 0) tmdbId else null
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            // T3-05: printStackTrace → Log.e
            android.util.Log.e("SimklApiClient", "getDvdMovies failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun search(query: String): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("https://api.simkl.com/search/mixed?q=$encodedQuery&client_id=$clientId")
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val responseText = response.body?.string().orEmpty()
                if (responseText.trim().startsWith("{")) {
                    android.util.Log.w("SimklApiClient", "Search returned JSON object instead of array: $responseText")
                    return@withContext emptyList()
                }
                val jsonArray = try {
                    JSONArray(responseText)
                } catch (e: Exception) {
                    android.util.Log.e("SimklApiClient", "Failed to parse Simkl search response: ${e.message}")
                    return@withContext emptyList()
                }
                val results = mutableListOf<JikanSearchResult>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val title = obj.optString("title", "")
                    val typeStr = obj.optString("type", "")
                    val ids = obj.optJSONObject("ids") ?: continue
                    val simklId = ids.optInt("simkl", 0)
                    val tmdbId = ids.optInt("tmdb", 0)
                    val poster = obj.optString("poster", "")
                    val year = obj.optInt("year", 0)

                    val mediaType = when (typeStr) {
                        "movie" -> MediaType.Movie
                        "show", "tv" -> MediaType.TvShow
                        "anime" -> MediaType.Anime
                        else -> MediaType.Anime
                    }

                    results.add(
                        JikanSearchResult(
                            malId = simklId,
                            title = title,
                            subtitle = if (year > 0) year.toString() else "",
                            type = mediaType,
                            total = null,
                            score = null,
                            isAdult = false,
                            imageUrl = if (poster.isNotEmpty()) "https://simkl.in/posters/${poster}_m.jpg" else null,
                            year = if (year > 0) year else null,
                            source = "simkl",
                            realMalId = if (mediaType == MediaType.Anime) ids.optInt("mal", 0) else null,
                            titleEnglish = title,
                            titleJapanese = null,
                            tmdbId = if (tmdbId > 0) tmdbId else null
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            // T3-05: printStackTrace → Log.e
            android.util.Log.e("SimklApiClient", "search failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun updateWatchlistStatus(
        token: String,
        simklId: Int,
        mediaType: MediaType,
        status: WatchStatus
    ): Boolean = withContext(Dispatchers.IO) {
        val simklStatus = when (status) {
            WatchStatus.Watching -> "watching"
            WatchStatus.Completed -> "completed"
            WatchStatus.Planned -> "plantowatch"
            WatchStatus.Dropped -> "dropped"
            WatchStatus.Paused -> "hold"
            WatchStatus.Repeating -> "watching" // Simkl has no rewatching; treat as watching
        }

        val typeKey = when (mediaType) {
            MediaType.Movie -> "movies"
            MediaType.TvShow -> "shows"
            else -> "anime"
        }

        val jsonPayload = JSONObject().apply {
            put(typeKey, JSONArray().apply {
                put(JSONObject().apply {
                    put("to", simklStatus)
                    put("ids", JSONObject().apply {
                        put("simkl", simklId)
                    })
                })
            })
        }.toString()

        val request = Request.Builder()
            .url("https://api.simkl.com/sync/add-to-list")
            .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("Authorization", "Bearer $token")
            .header("simkl-api-key", clientId)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                checkResponseAndThrow(response)
                response.isSuccessful
            }
        } catch (e: com.kitsugi.animelist.data.repository.SimklAuthException) {
            throw e
        } catch (e: Exception) {
            // T3-05: printStackTrace → Log.e
            android.util.Log.e("SimklApiClient", "updateWatchlistStatus failed: ${e.message}", e)
            false
        }
    }

    suspend fun updateEpisodeProgress(
        token: String,
        simklId: Int,
        season: Int,
        episode: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val jsonPayload = JSONObject().apply {
            put("shows", JSONArray().apply {
                put(JSONObject().apply {
                    put("ids", JSONObject().apply {
                        put("simkl", simklId)
                    })
                    put("seasons", JSONArray().apply {
                        put(JSONObject().apply {
                            put("number", season)
                            put("episodes", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("number", episode)
                                })
                            })
                        })
                    })
                })
            })
        }.toString()

        val request = Request.Builder()
            .url("https://api.simkl.com/sync/history")
            .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("Authorization", "Bearer $token")
            .header("simkl-api-key", clientId)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                checkResponseAndThrow(response)
                response.isSuccessful
            }
        } catch (e: com.kitsugi.animelist.data.repository.SimklAuthException) {
            throw e
        } catch (e: Exception) {
            // T3-05: printStackTrace → Log.e
            android.util.Log.e("SimklApiClient", "updateEpisodeProgress failed: ${e.message}", e)
            false
        }
    }

    // â”€â”€ KullanÄ±cÄ± Listesi YÃ¶netimi (NyanTV SimklService.kt referans) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * KullanÄ±cÄ±nÄ±n tÃ¼m izleme listesini Ã§eker.
     * type: "shows", "movies", "anime"
     * NyanTV referans: GET /sync/all-items/{type}
     */
    suspend fun getUserWatchlist(token: String, type: String): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.simkl.com/sync/all-items/$type?extended=full")
            .header("Authorization", "Bearer $token")
            .header("simkl-api-key", clientId)
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                checkResponseAndThrow(response)
                if (!response.isSuccessful) return@withContext emptyList()
                val root = JSONObject(response.body?.string().orEmpty())
                val itemsArray = root.optJSONArray(type) ?: return@withContext emptyList()
                val results = mutableListOf<JikanSearchResult>()

                for (i in 0 until itemsArray.length()) {
                    val entry = itemsArray.getJSONObject(i)
                    // shows -> entry.show; movies -> entry.movie; anime -> entry.anime
                    val mediaKey = when (type) {
                        "movies" -> "movie"
                        "shows" -> "show"
                        "anime" -> "anime"
                        else -> "show"
                    }
                    val mediaObj = entry.optJSONObject(mediaKey) ?: continue
                    val ids = mediaObj.optJSONObject("ids") ?: continue
                    val simklId = ids.optInt("simkl", 0).takeIf { it > 0 } ?: continue
                    val tmdbId = ids.optInt("tmdb", 0)
                    val malId = ids.optInt("mal", 0)
                    val title = mediaObj.optString("title", "")
                    val poster = mediaObj.optString("poster", "")
                    val year = mediaObj.optInt("year", 0)
                    val status = entry.optString("status", "")
                    val watchedEps = entry.optInt("watched_episodes_count", 0)
                    val totalEps = entry.optInt("total_episodes_count", 0)

                    val mediaType = when (type) {
                        "movies" -> MediaType.Movie
                        "shows" -> MediaType.TvShow
                        "anime" -> MediaType.Anime
                        else -> MediaType.TvShow
                    }

                    results.add(
                        JikanSearchResult(
                            malId = simklId,
                            title = title,
                            subtitle = simklStatusToDisplayString(status),
                            type = mediaType,
                            total = if (totalEps > 0) totalEps else null,
                            score = null,
                            isAdult = false,
                            imageUrl = if (poster.isNotEmpty()) "https://simkl.in/posters/${poster}_m.jpg" else null,
                            year = if (year > 0) year else null,
                            source = "simkl",
                            realMalId = if (malId > 0) malId else null,
                            titleEnglish = title,
                            titleJapanese = null,
                            tmdbId = if (tmdbId > 0) tmdbId else null
                        )
                    )
                }
                results
            }
        } catch (e: com.kitsugi.animelist.data.repository.SimklAuthException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SimklApiClient", "getUserWatchlist $type failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Listeye iÃ§erik ekler veya durumunu gÃ¼nceller.
     * type: "shows" veya "movies" veya "anime"
     * status: "watching", "plantowatch", "completed", "hold", "dropped"
     * NyanTV referans: POST /sync/add-to-list
     */
    suspend fun addToList(token: String, simklId: Int, type: String, status: String): Boolean = withContext(Dispatchers.IO) {
        val mediaKey = when (type) { "movies" -> "movies"; else -> "shows" }
        val idObj = JSONObject().put("simkl", simklId)
        val itemObj = JSONObject().put("to", status).put("ids", idObj)
        val payload = JSONObject().put(mediaKey, JSONArray().put(itemObj)).toString()

        val request = Request.Builder()
            .url("https://api.simkl.com/sync/add-to-list")
            .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("Authorization", "Bearer $token")
            .header("simkl-api-key", clientId)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                checkResponseAndThrow(response)
                response.isSuccessful
            }
        } catch (e: com.kitsugi.animelist.data.repository.SimklAuthException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Listeden iÃ§eriÄŸi siler.
     * NyanTV referans: POST /sync/history/remove
     */
    suspend fun removeFromList(token: String, simklId: Int, type: String): Boolean = withContext(Dispatchers.IO) {
        val mediaKey = when (type) { "movies" -> "movies"; else -> "shows" }
        val idObj = JSONObject().put("simkl", simklId)
        val itemObj = JSONObject().put("ids", idObj)
        val payload = JSONObject().put(mediaKey, JSONArray().put(itemObj)).toString()

        val request = Request.Builder()
            .url("https://api.simkl.com/sync/history/remove")
            .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("Authorization", "Bearer $token")
            .header("simkl-api-key", clientId)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                checkResponseAndThrow(response)
                response.isSuccessful
            }
        } catch (e: com.kitsugi.animelist.data.repository.SimklAuthException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /**
     * KullanÄ±cÄ± profilini Ã§eker (ad, avatar, id).
     * NyanTV referans: POST /users/settings
     */
    suspend fun getUserProfile(token: String): JSONObject? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.simkl.com/users/settings")
            .post("{}".toRequestBody("application/json".toMediaTypeOrNull()))
            .header("Authorization", "Bearer $token")
            .header("simkl-api-key", clientId)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                checkResponseAndThrow(response)
                if (!response.isSuccessful) null
                else JSONObject(response.body?.string().orEmpty())
            }
        } catch (e: com.kitsugi.animelist.data.repository.SimklAuthException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    // â”€â”€ Status yardÄ±mcÄ±larÄ± â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Simkl status string'ini WatchStatus'a Ã§evirir (NyanTV simklStatusToAL referans) */
    fun simklStatusToWatchStatusString(simklStatus: String?): String = when (simklStatus) {
        "watching"    -> "CURRENT"
        "completed"   -> "COMPLETED"
        "hold"        -> "PAUSED"
        "dropped"     -> "DROPPED"
        "plantowatch" -> "PLANNING"
        else          -> "PLANNING"
    }

    /** WatchStatus string'ini Simkl status'una Ã§evirir (NyanTV alStatusToSimkl referans) */
    fun watchStatusStringToSimkl(status: String?): String = when (status) {
        "CURRENT"   -> "watching"
        "COMPLETED" -> "completed"
        "PAUSED"    -> "hold"
        "DROPPED"   -> "dropped"
        "PLANNING"  -> "plantowatch"
        else        -> "plantowatch"
    }

    private fun simklStatusToDisplayString(status: String?) = when (status) {
        "watching"    -> "İzleniyor"
        "completed"   -> "Tamamlandı"
        "hold"        -> "Beklemede"
        "dropped"     -> "Bırakıldı"
        "plantowatch" -> "Planlandı"
        else          -> status.orEmpty()
    }

    // ── Delta Sync: /sync/activities ─────────────────────────────────────────

    /**
     * SIMKL sync/activities endpoint'ini çeker.
     * Delta sync gate: timestamp değişmemişse library yeniden çekilmez.
     * Bkz: https://api.simkl.org/guides/sync
     */
    suspend fun getActivities(token: String): JSONObject? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.simkl.com/sync/activities")
            .header("Authorization", "Bearer $token")
            .header("simkl-api-key", clientId)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                checkResponseAndThrow(response)
                when {
                    response.code == 429 -> throw com.kitsugi.animelist.data.repository.SimklRateLimitException("Rate limited (429)")
                    !response.isSuccessful -> null
                    else -> JSONObject(response.body?.string().orEmpty())
                }
            }
        } catch (e: com.kitsugi.animelist.data.repository.SimklAuthException) { throw e }
        catch (e: com.kitsugi.animelist.data.repository.SimklRateLimitException) { throw e }
        catch (e: Exception) {
            android.util.Log.e("SimklApiClient", "getActivities failed: ${e.message}")
            null
        }
    }

    // ── Rating ───────────────────────────────────────────────────────────────

    /**
     * 1–10 arası puan gönderir.
     * NyanTV referans: POST /sync/ratings
     */
    suspend fun setRating(
        token: String,
        simklId: Int,
        mediaType: MediaType,
        rating: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val typeKey = when (mediaType) {
            MediaType.Movie -> "movies"
            MediaType.Anime -> "anime"
            else            -> "shows"
        }
        val idObj   = JSONObject().put("simkl", simklId)
        val itemObj = JSONObject().put("ids", idObj).put("rating", rating)
        val payload = JSONObject().put(typeKey, JSONArray().put(itemObj)).toString()

        val request = Request.Builder()
            .url("https://api.simkl.com/sync/ratings")
            .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("Authorization", "Bearer $token")
            .header("simkl-api-key", clientId)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                checkResponseAndThrow(response)
                response.isSuccessful
            }
        } catch (e: com.kitsugi.animelist.data.repository.SimklAuthException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Puanı kaldırır.
     * NyanTV referans: POST /sync/ratings/remove
     */
    suspend fun removeRating(
        token: String,
        simklId: Int,
        mediaType: MediaType
    ): Boolean = withContext(Dispatchers.IO) {
        val typeKey = when (mediaType) {
            MediaType.Movie -> "movies"
            MediaType.Anime -> "anime"
            else            -> "shows"
        }
        val idObj   = JSONObject().put("simkl", simklId)
        val itemObj = JSONObject().put("ids", idObj)
        val payload = JSONObject().put(typeKey, JSONArray().put(itemObj)).toString()

        val request = Request.Builder()
            .url("https://api.simkl.com/sync/ratings/remove")
            .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("Authorization", "Bearer $token")
            .header("simkl-api-key", clientId)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                checkResponseAndThrow(response)
                response.isSuccessful
            }
        } catch (e: com.kitsugi.animelist.data.repository.SimklAuthException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    // ── Calendar (CDN) ───────────────────────────────────────────────────────

    /**
     * SIMKL CDN takvim JSON'ını çeker (auth gerekmez).
     * type: "tv", "anime", "movie_release"
     */
    suspend fun getCalendar(type: String): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val cdnType = when (type) {
            "movie", "movies", "movie_release" -> "movie_release"
            "anime"                            -> "anime"
            else                               -> "tv"
        }
        val url = "https://data.simkl.in/calendar/${cdnType}.json"
        val request = Request.Builder().url(url).header("Accept", "application/json").build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val jsonArray = JSONArray(response.body?.string().orEmpty())
                val results = mutableListOf<JikanSearchResult>()

                for (i in 0 until minOf(jsonArray.length(), 50)) {
                    val obj   = jsonArray.getJSONObject(i)
                    val ids   = obj.optJSONObject("ids") ?: continue
                    val simklId = ids.optInt("simkl_id", 0).takeIf { it > 0 }
                        ?: ids.optInt("simkl", 0).takeIf { it > 0 } ?: continue
                    val title = obj.optString("title", "")
                    val poster = obj.optString("poster", "")
                    val year  = obj.optInt("year", 0)
                    val tmdbId = ids.optInt("tmdb", 0)

                    val mediaType = when (cdnType) {
                        "movie_release" -> MediaType.Movie
                        "anime"         -> MediaType.Anime
                        else            -> MediaType.TvShow
                    }

                    results.add(
                        JikanSearchResult(
                            malId = simklId,
                            title = title,
                            subtitle = if (year > 0) year.toString() else "",
                            type = mediaType,
                            total = null,
                            score = null,
                            isAdult = false,
                            imageUrl = if (poster.isNotEmpty()) "https://simkl.in/posters/${poster}_m.jpg" else null,
                            year = if (year > 0) year else null,
                            source = "simkl",
                            realMalId = null,
                            titleEnglish = title,
                            titleJapanese = null,
                            tmdbId = if (tmdbId > 0) tmdbId else null
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            android.util.Log.e("SimklApiClient", "getCalendar $cdnType failed: ${e.message}")
            emptyList()
        }
    }
}
