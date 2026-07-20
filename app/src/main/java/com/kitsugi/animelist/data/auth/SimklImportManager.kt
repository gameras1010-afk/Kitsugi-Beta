package com.kitsugi.animelist.data.auth

import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import okhttp3.Request
import com.kitsugi.animelist.data.remote.optNullableString

object SimklImportManager {

    private val client = com.kitsugi.animelist.core.network.KitsugiHttpClient.client
    // T3-01: BuildConfig'den alınır
    private val CLIENT_ID get() = com.kitsugi.animelist.BuildConfig.SIMKL_CLIENT_ID

    data class SimklUserProfile(
        val name: String,
        val avatarUrl: String?,
        val bannerUrl: String?,
        val joinedAt: String? = null,
        val location: String? = null,
        val bio: String? = null,
        val accountType: String? = null
    )

    suspend fun fetchUserProfile(token: String): SimklUserProfile {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.simkl.com/users/settings")
                .header("Authorization", "Bearer $token")
                .header("simkl-api-key", CLIENT_ID)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 401) {
                    val context = com.kitsugi.animelist.KitsugiApplication.getInstance()?.applicationContext
                    if (context != null) {
                        com.kitsugi.animelist.data.auth.ExternalAuthManager.handleSimkl401(context)
                    }
                    throw com.kitsugi.animelist.data.repository.SimklAuthException("Simkl yetkilendirme hatası: 401")
                }
                if (!response.isSuccessful) {
                    throw IllegalStateException("Simkl profil hatası: ${response.code}")
                }
                val responseText = response.body?.string().orEmpty()
                val root = JSONObject(responseText)
                val user = root.optJSONObject("user")
                val name = user?.optNullableString("name") ?: "Simkl Kullanıcısı"
                val avatar = user?.optNullableString("avatar")
                val avatarUrl = if (!avatar.isNullOrBlank()) "https://simkl.in/avatars/${avatar}_m.jpg" else null

                val joinedAt = user?.optNullableString("joined_at")
                val location = user?.optNullableString("location")
                val bio = user?.optNullableString("bio")
                val account = root.optJSONObject("account")
                val accountType = account?.optNullableString("type")

                SimklUserProfile(
                    name = name,
                    avatarUrl = avatarUrl,
                    bannerUrl = null,
                    joinedAt = joinedAt,
                    location = location,
                    bio = bio,
                    accountType = accountType
                )
            }
        }
    }

    suspend fun fetchAllLists(token: String): List<MediaEntry> {
        return withContext(Dispatchers.IO) {
            val movies = fetchList(token, "movies")
            val shows = fetchList(token, "shows")
            val anime = fetchList(token, "anime")
            movies + shows + anime
        }
    }

    private suspend fun fetchList(token: String, type: String): List<MediaEntry> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.simkl.com/sync/all-items/$type")
                .header("Authorization", "Bearer $token")
                .header("simkl-api-key", CLIENT_ID)
                .build()

            val entries = mutableListOf<MediaEntry>()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.code == 401) {
                        val context = com.kitsugi.animelist.KitsugiApplication.getInstance()?.applicationContext
                        if (context != null) {
                            com.kitsugi.animelist.data.auth.ExternalAuthManager.handleSimkl401(context)
                        }
                        throw com.kitsugi.animelist.data.repository.SimklAuthException("Simkl yetkilendirme hatası: 401")
                    }
                    if (!response.isSuccessful) {
                        return@withContext emptyList()
                    }
                    val responseText = response.body?.string().orEmpty()
                    val root = JSONObject(responseText)
                    val arrayKey = when (type) {
                        "movies" -> "movies"
                        "shows" -> "shows"
                        else -> "anime"
                    }
                    val jsonArray = root.optJSONArray(arrayKey) ?: return@withContext emptyList()

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val statusStr = item.optNullableString("status")
                        val watchStatus = mapSimklStatus(statusStr)

                        val mediaObjKey = when (type) {
                            "movies" -> "movie"
                            "shows" -> "show"
                            else -> "anime"
                        }
                        val mediaObj = item.optJSONObject(mediaObjKey) ?: continue
                        val title = mediaObj.optNullableString("title") ?: "BaÅŸlÄ±ksÄ±z"
                        val year = mediaObj.optInt("year", 0).takeIf { it > 0 }
                        val poster = mediaObj.optNullableString("poster")
                        val imageUrl = if (!poster.isNullOrBlank()) "https://simkl.in/posters/${poster}_m.jpg" else null

                        val ids = mediaObj.optJSONObject("ids")
                        val simklId = ids?.optInt("simkl", 0)?.takeIf { it > 0 }
                        val tmdbId = ids?.optInt("tmdb", 0)?.takeIf { it > 0 }
                        val malId = ids?.optInt("mal", 0)?.takeIf { it > 0 }

                        val mediaType = when (type) {
                            "movies" -> MediaType.Movie
                            "shows" -> MediaType.TvShow
                            else -> MediaType.Anime
                        }

                        val progress = when (mediaType) {
                            MediaType.Movie -> if (watchStatus == WatchStatus.Completed) 1 else 0
                            else -> item.optInt("watched_episodes_count", 0)
                        }

                        val total = when (mediaType) {
                            MediaType.Movie -> 1
                            else -> item.optInt("total_episodes_count", 0).takeIf { it > 0 }
                        }

                        // Parse rating if present
                        val score = item.optInt("user_rating", 0).takeIf { it > 0 }

                        entries.add(
                            MediaEntry(
                                id = 0,
                                title = title,
                                subtitle = when (mediaType) {
                                    MediaType.Movie -> "Film"
                                    MediaType.TvShow -> "Dizi"
                                    MediaType.Anime -> "Anime"
                                    else -> "Manga"
                                }.let { label ->
                                    if (year != null) "$label • $year" else label
                                },
                                type = mediaType,
                                status = watchStatus,
                                score = score,
                                progress = progress,
                                total = total,
                                isFavorite = false,
                                isAdult = false,
                                source = "simkl",
                                malId = malId ?: simklId ?: (150_000_000 + i), // fallback stable id
                                imageUrl = imageUrl,
                                year = year,
                                synopsis = null,
                                tmdbId = tmdbId,
                                simklId = simklId
                            )
                        )
                    }
                }
            } catch (e: com.kitsugi.animelist.data.repository.SimklAuthException) {
                throw e
            } catch (e: Exception) {
                // T3-05: printStackTrace → Log.e
                android.util.Log.e("SimklImportManager", "fetchList [$type] failed: ${e.message}", e)
            }
            entries
        }
    }

    private fun mapSimklStatus(status: String?): WatchStatus {
        return when (status) {
            "watching" -> WatchStatus.Watching
            "completed" -> WatchStatus.Completed
            "hold" -> WatchStatus.Paused
            "dropped" -> WatchStatus.Dropped
            "plantowatch" -> WatchStatus.Planned
            else -> WatchStatus.Planned
        }
    }
}
