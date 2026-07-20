package com.kitsugi.animelist.data.remote

import android.util.Log
import org.json.JSONObject
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Holds all resolved external IDs for a given anime entry.
 */
data class ResolvedIds(
    val imdbId: String? = null,
    val kitsuId: Int? = null,
    val tmdbId: Int? = null,
    val aniListId: Int? = null,
    val malId: Int? = null
)

object KitsugiIdResolver {
    private const val TAG = "KitsugiIdResolver"

    /**
     * Resolves both IMDb ID and Kitsu ID in a single ARM API call.
     * Falls back to TMDB for the IMDb ID if ARM doesn't have one.
     */
    suspend fun resolveIds(malId: Int?, aniListId: Int?, tmdbId: Int? = null): ResolvedIds = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting ID resolution: malParam=$malId, aniListParam=$aniListId, tmdbParam=$tmdbId")
        // Try ARM with MAL first, then AniList, then TMDB
        var armJson = fetchArmJson("myanimelist", malId)
        if (armJson != null) {
            Log.d(TAG, "Successfully fetched ARM JSON for myanimelist: $malId -> $armJson")
        } else {
            armJson = fetchArmJson("anilist", aniListId)
            if (armJson != null) {
                Log.d(TAG, "Successfully fetched ARM JSON for anilist: $aniListId -> $armJson")
            } else {
                armJson = fetchArmJson("themoviedb", tmdbId)
                if (armJson != null) {
                    Log.d(TAG, "Successfully fetched ARM JSON for themoviedb: $tmdbId -> $armJson")
                } else {
                    Log.w(TAG, "ARM lookup failed for all sources (malId=$malId, aniListId=$aniListId, tmdbId=$tmdbId)")
                }
            }
        }

        val imdbFromArm = armJson?.optNullableString("imdb")
        val kitsuFromArm = armJson?.let {
            val v = it.optInt("kitsu", -1)
            if (v > 0) v else null
        }
        val tmdbFromArm = armJson?.let {
            val v = it.optInt("themoviedb", -1)
            if (v > 0) v else null
        }
        val aniListFromArm = armJson?.let {
            val v = it.optInt("anilist", -1)
            if (v > 0) v else null
        }
        val malFromArm = armJson?.let {
            val v = it.optInt("myanimelist", -1)
            if (v > 0) v else null
        }
        // Use provided tmdbId directly if ARM didn't find one
        val finalTmdbId = tmdbFromArm ?: tmdbId ?: resolveTmdbId(malId, aniListId)

        // If ARM didn't give us an IMDb ID, fall back through TMDB
        val finalImdb = if (!imdbFromArm.isNullOrBlank()) {
            imdbFromArm
        } else {
            finalTmdbId?.let { fetchImdbFromTmdb(it) }
        }

        val finalAniList = aniListFromArm ?: aniListId
        val finalMal = malFromArm ?: malId

        Log.d(TAG, "resolveIds complete → imdb=$finalImdb kitsu=$kitsuFromArm tmdb=$finalTmdbId anilist=$finalAniList mal=$finalMal")
        ResolvedIds(imdbId = finalImdb, kitsuId = kitsuFromArm, tmdbId = finalTmdbId, aniListId = finalAniList, malId = finalMal)
    }

    /**
     * Resolves the IMDb ID for a media entry, using either MAL ID or AniList ID.
     */
    suspend fun getImdbId(malId: Int?, aniListId: Int?): String? =
        resolveIds(malId, aniListId).imdbId

    /** Fetches the raw ARM JSON for a given source+id pair. Returns null on failure or missing id. */
    private fun fetchArmJson(source: String, id: Int?): JSONObject? {
        if (id == null || id <= 0) return null
        val url = runCatching {
            URL("https://arm.haglund.dev/api/v2/ids?source=$source&id=$id")
        }.getOrNull() ?: return null
        return try {
            val response = KitsugiApiBase.executeGetRequest(url) ?: return null
            val trimmed = response.trim()
            // ARM returns "null" string or "[]" empty array when ID is not in anime DB
            // (e.g. Turkish TV shows, movies not indexed by ARM). Silently return null.
            if (trimmed == "null" || trimmed == "[]" || trimmed.isEmpty()) return null
            // ARM may return an array with one element — unwrap it
            if (trimmed.startsWith("[")) {
                val arr = org.json.JSONArray(trimmed)
                if (arr.length() == 0) return null
                arr.optJSONObject(0)
            } else {
                JSONObject(trimmed)
            }
        } catch (e: Exception) {
            Log.w(TAG, "ARM fetch failed for $source ID $id: ${e.message}")
            null
        }
    }

    private suspend fun resolveTmdbId(malId: Int?, aniListId: Int?): Int? = withContext(Dispatchers.IO) {
        val malJson = if (malId != null && malId > 0) fetchArmJson("myanimelist", malId) else null
        val malTmdbVal = malJson?.optInt("themoviedb", -1)
        if (malTmdbVal != null && malTmdbVal > 0) {
            return@withContext malTmdbVal
        }

        val aniJson = if (aniListId != null && aniListId > 0) fetchArmJson("anilist", aniListId) else null
        val aniTmdbVal = aniJson?.optInt("themoviedb", -1)
        if (aniTmdbVal != null && aniTmdbVal > 0) {
            return@withContext aniTmdbVal
        }
        null
    }

    private fun fetchImdbFromTmdb(tmdbId: Int): String? {
        val apiKey = TmdbApiClient.getActiveApiKey()

        // Probe TV external IDs first
        try {
            val tvUrl = URL("https://api.themoviedb.org/3/tv/$tmdbId/external_ids?api_key=$apiKey")
            val tvResponse = KitsugiApiBase.executeGetRequest(tvUrl)
            if (tvResponse != null) {
                val json = JSONObject(tvResponse)
                val imdbId = json.optNullableString("imdb_id")
                if (!imdbId.isNullOrBlank()) return imdbId
            }
        } catch (e: Exception) {
            // Ignore and try movie
        }

        // Try movie details
        try {
            val movieUrl = URL("https://api.themoviedb.org/3/movie/$tmdbId?api_key=$apiKey")
            val movieResponse = KitsugiApiBase.executeGetRequest(movieUrl)
            if (movieResponse != null) {
                val json = JSONObject(movieResponse)
                val imdbId = json.optNullableString("imdb_id")
                if (!imdbId.isNullOrBlank()) return imdbId
            }
        } catch (e: Exception) {
            // Ignore
        }

        return null
    }

    // JSON Helper
    private fun JSONObject.optNullableString(key: String): String? {
        if (isNull(key)) return null
        return optString(key)
    }
}
