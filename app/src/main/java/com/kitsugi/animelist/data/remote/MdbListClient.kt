package com.kitsugi.animelist.data.remote

import android.util.Log
import com.kitsugi.animelist.core.network.KitsugiHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * MDBList API istemcisi.
 *
 * MDBList, tek bir GET isteği ile IMDb, Rotten Tomatoes, Metacritic, Letterboxd, TMDB
 * ve Trakt gibi platformların puanlarını döndürür.
 *
 * Endpoint: GET https://mdblist.com/api/?apikey=KEY&i=IMDB_ID
 * API Key: https://mdblist.com/api/ adresinden ücretsiz alınabilir.
 *
 * Rate Limit: API anahtarı başına günlük 1000 istek (ücretsiz plan).
 * Cache: 30 dakika TTL ile in-memory ConcurrentHashMap.
 */
data class MdbListRatings(
    val imdb: Double? = null,
    val tomatoes: Int? = null,           // Rotten Tomatoes Tomatometer %
    val tomatoesAudience: Int? = null,   // Rotten Tomatoes Audience Score %
    val metacritic: Int? = null,
    val letterboxd: Double? = null,
    val tmdb: Double? = null,
    val trakt: Double? = null,
    val imdbId: String? = null
) {
    /** Gösterilecek en az bir puan mevcut mu? */
    val isEmpty: Boolean get() = imdb == null && tomatoes == null && tomatoesAudience == null
        && metacritic == null && letterboxd == null && tmdb == null && trakt == null
}

object MdbListClient {

    private const val TAG = "MdbListClient"
    private const val BASE_URL = "https://mdblist.com/api/"
    private const val CACHE_TTL_MS = 30L * 60L * 1_000L // 30 dakika

    /**
     * API anahtarının geçerliliğini mdblist.com/api/?apikey=KEY isteği atarak test eder.
     * Geçerli ise true, değilse veya ağ hatası olursa false döner.
     */
    suspend fun validateApiKey(apiKey: String): Boolean =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext false
            // Use a stable IMDb ID (tt0133093 for The Matrix) to test the API key with a valid request
            val url = "$BASE_URL?apikey=$apiKey&i=tt0133093"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "KitsugiAnimeList/1.0")
                .build()

            try {
                KitsugiHttpClient.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext false
                    val body = response.body?.string() ?: return@withContext false
                    val root = JSONObject(body)
                    val responseVal = root.opt("response")
                    when (responseVal) {
                        is Boolean -> responseVal
                        is String -> responseVal.trim().equals("true", ignoreCase = true)
                        else -> false
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "MDBList validation error: ${e.message}")
                false
            }
        }

    private data class CacheEntry(val ratings: MdbListRatings?, val expiresAt: Long)

    // Anahtar: "${apiKeyHash}:${imdbId}"
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    /**
     * Verilen IMDb ID için tüm platform puanlarını çeker.
     *
     * @param imdbId  "tt1234567" formatında IMDb ID
     * @param apiKey  Kullanıcının MDBList API anahtarı
     * @return [MdbListRatings] nesnesi; başarısız olursa null
     */
    suspend fun fetchRatings(imdbId: String, apiKey: String): MdbListRatings? =
        withContext(Dispatchers.IO) {
            if (imdbId.isBlank() || apiKey.isBlank()) return@withContext null

            val cacheKey = "${apiKey.hashCode()}:$imdbId"
            val now = System.currentTimeMillis()
            cache[cacheKey]?.let { entry ->
                if (entry.expiresAt > now) return@withContext entry.ratings
                cache.remove(cacheKey)
            }

            val url = "$BASE_URL?apikey=$apiKey&i=$imdbId"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "KitsugiAnimeList/1.0")
                .build()

            val ratings = try {
                KitsugiHttpClient.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "MDBList isteği başarısız: HTTP ${response.code} — $imdbId")
                        return@withContext null
                    }
                    val body = response.body?.string() ?: return@withContext null
                    parseRatings(body, imdbId)
                }
            } catch (e: Exception) {
                Log.w(TAG, "MDBList ağ hatası — $imdbId: ${e.message}")
                null
            }

            cache[cacheKey] = CacheEntry(ratings, now + CACHE_TTL_MS)
            ratings
        }

    private fun parseRatings(json: String, imdbId: String?): MdbListRatings? {
        return try {
            val root = JSONObject(json)

            // API hata durumunda {"response":false} döndürür
            if (!root.optBoolean("response", true)) {
                Log.w(TAG, "MDBList: response=false — muhtemelen geçersiz API anahtarı")
                return null
            }

            // scores dizisi → her eleman { "source": "imdb", "value": 8.1 } formatında
            val scoresArr = root.optJSONArray("ratings") ?: root.optJSONArray("scores")
            if (scoresArr != null && scoresArr.length() > 0) {
                var imdb: Double? = null
                var tomatoes: Int? = null
                var tomatoesAudience: Int? = null
                var metacritic: Int? = null
                var letterboxd: Double? = null
                var tmdb: Double? = null
                var trakt: Double? = null

                for (i in 0 until scoresArr.length()) {
                    val item = scoresArr.optJSONObject(i) ?: continue
                    val source = item.optString("source").lowercase()
                    val value = item.optDouble("value", Double.NaN)
                    val votes = item.optInt("votes", 0)
                    if (value.isNaN() || votes == 0) continue

                    when (source) {
                        "imdb" -> imdb = value
                        "tomatoes" -> tomatoes = value.toInt()
                        "tomatoesaudience" -> tomatoesAudience = value.toInt()
                        "metacritic" -> metacritic = value.toInt()
                        "letterboxd" -> letterboxd = value
                        "tmdb" -> tmdb = value
                        "trakt" -> trakt = value
                    }
                }
                return MdbListRatings(imdb, tomatoes, tomatoesAudience, metacritic, letterboxd, tmdb, trakt, imdbId)
            }

            // Alternatif düz-alan formatı (v1 API): { "imdbrating": 8.1, "tomatoesrating": 91, ... }
            MdbListRatings(
                imdb = root.optDouble("imdbrating", Double.NaN).takeIf { !it.isNaN() },
                tomatoes = root.optInt("tomatoesrating", -1).takeIf { it >= 0 },
                tomatoesAudience = root.optInt("tomatoesaudiencerating", -1).takeIf { it >= 0 },
                metacritic = root.optInt("metacriticrating", -1).takeIf { it >= 0 },
                letterboxd = root.optDouble("letterboxdrating", Double.NaN).takeIf { !it.isNaN() },
                tmdb = root.optDouble("tmdbrating", Double.NaN).takeIf { !it.isNaN() },
                trakt = root.optDouble("traktrating", Double.NaN).takeIf { !it.isNaN() },
                imdbId = imdbId
            ).takeIf { !it.isEmpty }
        } catch (e: Exception) {
            Log.e(TAG, "MDBList JSON parse hatası: ${e.message}")
            null
        }
    }
}
