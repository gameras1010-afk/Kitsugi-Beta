package com.kitsugi.animelist.data.remote

import android.util.Log
import com.kitsugi.animelist.core.network.KitsugiHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Anime-Skip (anime-skip.com) GraphQL API istemcisi.
 *
 * anime-skip.com, anime bölümlerinin intro/outro/recap gibi zaman damgalarını
 * MAL ID bazlı bir GraphQL API ile sunar.
 *
 * Endpoint: POST https://api.anime-skip.com/graphql
 * Auth: X-Client-Id header (Kitsugi için: BuildConfig.ANIME_SKIP_CLIENT_ID)
 * Rate Limit: 60 istek/dakika
 *
 * Fallback: AniSkipClient ile zincir oluşturulur — AnimeSkip önce denenir,
 * bulunamazsa AniSkip sonuçları kullanılır.
 */
object AnimeSkipClient {

    private const val TAG = "AnimeSkipClient"
    private const val BASE_URL = "https://api.anime-skip.com/graphql"
    /** Kitsugi uygulaması için kayıtlı Client ID — T3-01: BuildConfig'den gelir */
    private val DEFAULT_CLIENT_ID get() = com.kitsugi.animelist.BuildConfig.ANIME_SKIP_CLIENT_ID
    private const val CACHE_TTL_MS = 60L * 60L * 1_000L // 60 dakika

    /**
     * Client ID'nin geçerliliğini test etmek için basit bir sorgu atar.
     * X-Client-Id başlığı geçersiz ise HTTP 401/403 veya isSuccessful=false döner.
     */
    suspend fun validateClientId(clientId: String): Boolean =
        withContext(Dispatchers.IO) {
            if (clientId.isBlank()) return@withContext false
            val query = """
                query {
                  __schema {
                    types {
                      name
                    }
                  }
                }
            """.trimIndent()

            val body = JSONObject().apply {
                put("query", query)
            }.toString()

            try {
                val request = Request.Builder()
                    .url(BASE_URL)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .header("X-Client-Id", clientId)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", "KitsugiAnimeList/1.0")
                    .build()

                KitsugiHttpClient.client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                Log.w(TAG, "AnimeSkip validation error: ${e.message}")
                false
            }
        }

    private data class CacheEntry(val intervals: List<SkipInterval>, val expiresAt: Long)

    // Anahtar: "$malId:$episode:$clientId"
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    /**
     * MAL ID ve bölüm numarasına göre anime-skip.com'dan skip aralıklarını getirir.
     *
     * @param malId     MyAnimeList ID'si
     * @param episode   Bölüm numarası (1'den başlar)
     * @param clientId  Kullanıcının özel Client ID'si; boşsa DEFAULT_CLIENT_ID kullanılır
     * @return Sıfır veya daha fazla [SkipInterval] listesi; provider = "animeskip"
     */
    suspend fun getSkipTimes(
        malId: Int,
        episode: Int,
        clientId: String = ""
    ): List<SkipInterval> = withContext(Dispatchers.IO) {
        val effectiveClientId = clientId.takeIf { it.isNotBlank() } ?: DEFAULT_CLIENT_ID
        val cacheKey = "$malId:$episode:$effectiveClientId"
        val now = System.currentTimeMillis()

        cache[cacheKey]?.let { entry ->
            if (entry.expiresAt > now) return@withContext entry.intervals
            cache.remove(cacheKey)
        }

        // Adım 1: MAL ID → Anime-Skip Internal Show ID bul
        val showId = resolveShowIdByMalId(malId, effectiveClientId) ?: run {
            Log.d(TAG, "AnimeSkip: MAL=$malId için show bulunamadı")
            cache[cacheKey] = CacheEntry(emptyList(), now + CACHE_TTL_MS)
            return@withContext emptyList()
        }

        // Adım 2: Show ID + bölüm → timestamp'leri çek
        val intervals = fetchTimestamps(showId, episode, effectiveClientId)

        cache[cacheKey] = CacheEntry(intervals, now + CACHE_TTL_MS)
        intervals
    }

    /**
     * MAL ID'yi anime-skip.com'un iç show ID'sine çevirir.
     * findShowsByExternalId GraphQL sorgusunu kullanır.
     */
    private suspend fun resolveShowIdByMalId(malId: Int, clientId: String): String? {
        val query = """
            query FindShowByMalId(${'$'}malId: Int!) {
              findShowsByExternalId(malId: ${'$'}malId) {
                id
                name
              }
            }
        """.trimIndent()

        val body = JSONObject().apply {
            put("query", query)
            put("variables", JSONObject().put("malId", malId))
        }.toString()

        return try {
            val request = Request.Builder()
                .url(BASE_URL)
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("X-Client-Id", clientId)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "KitsugiAnimeList/1.0")
                .build()

            KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "AnimeSkip resolveShowId: HTTP ${response.code} MAL=$malId")
                    return null
                }
                val root = JSONObject(response.body?.string() ?: return null)
                val showsArr = root.optJSONObject("data")
                    ?.optJSONArray("findShowsByExternalId")
                    ?: return null
                if (showsArr.length() == 0) return null
                showsArr.optJSONObject(0)?.optString("id")?.takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "AnimeSkip resolveShowId error MAL=$malId: ${e.message}")
            null
        }
    }

    /**
     * Show ID ve bölüm numarasına göre timestamp'leri getirir.
     * findEpisodesByShowId + timestamps sorgusunu kullanır.
     */
    private suspend fun fetchTimestamps(
        showId: String,
        episode: Int,
        clientId: String
    ): List<SkipInterval> {
        val query = """
            query FindTimestamps(${'$'}showId: ID!, ${'$'}episode: Float!) {
              findEpisodesByShowId(showId: ${'$'}showId, episodeFilter: { number: ${'$'}episode }) {
                id
                number
                timestamps {
                  id
                  at
                  typeId
                  type {
                    name
                    description
                  }
                }
              }
            }
        """.trimIndent()

        val body = JSONObject().apply {
            put("query", query)
            put("variables", JSONObject().apply {
                put("showId", showId)
                put("episode", episode.toDouble())
            })
        }.toString()

        return try {
            val request = Request.Builder()
                .url(BASE_URL)
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("X-Client-Id", clientId)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "KitsugiAnimeList/1.0")
                .build()

            KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "AnimeSkip fetchTimestamps: HTTP ${response.code} show=$showId ep=$episode")
                    return emptyList()
                }
                val body2 = response.body?.string() ?: return emptyList()
                parseTimestamps(body2)
            }
        } catch (e: Exception) {
            Log.w(TAG, "AnimeSkip fetchTimestamps error show=$showId ep=$episode: ${e.message}")
            emptyList()
        }
    }

    private fun parseTimestamps(json: String): List<SkipInterval> {
        return try {
            val root = JSONObject(json)
            val episodes = root.optJSONObject("data")
                ?.optJSONArray("findEpisodesByShowId")
                ?: return emptyList()

            if (episodes.length() == 0) return emptyList()

            val firstEp = episodes.optJSONObject(0) ?: return emptyList()
            val timestamps = firstEp.optJSONArray("timestamps") ?: return emptyList()

            val list = mutableListOf<SkipInterval>()
            // timestamps dizisi sıralı AT değerlerine sahip, pairs olarak başlangıç/bitiş çıkar
            // AnimeSkip'in formatı: her timestamp bir "at" (saniye) + type içerir
            // Intro: tip "op" veya "Opening", Outro: "ed" veya "Ending"
            // AnimeSkip timestamp'leri çiftler halinde gelir: intro start → intro end
            // Basit yaklaşım: type bazında min/max al
            data class TsEntry(val at: Double, val typeName: String)
            val entries = mutableListOf<TsEntry>()

            for (i in 0 until timestamps.length()) {
                val ts = timestamps.optJSONObject(i) ?: continue
                val at = ts.optDouble("at", Double.NaN)
                if (at.isNaN()) continue
                val typeName = ts.optJSONObject("type")?.optString("name", "")?.lowercase() ?: continue
                entries.add(TsEntry(at, typeName))
            }

            // Tip bazında grupla ve min/max'ı skip interval olarak ver
            val grouped = entries.groupBy { it.typeName }
            grouped.forEach { (typeName, tsEntries) ->
                if (tsEntries.size < 2) return@forEach
                val sortedTimes = tsEntries.map { it.at }.sorted()
                val start = sortedTimes.first()
                val end = sortedTimes.last()
                if (end <= start) return@forEach

                val skipType = when {
                    typeName.contains("op") || typeName.contains("opening") || typeName.contains("intro") -> "op"
                    typeName.contains("ed") || typeName.contains("ending") || typeName.contains("outro") -> "ed"
                    typeName.contains("recap") -> "recap"
                    typeName.contains("mixed") && (typeName.contains("op") || typeName.contains("opening")) -> "mixed-op"
                    typeName.contains("mixed") && (typeName.contains("ed") || typeName.contains("ending")) -> "mixed-ed"
                    else -> typeName
                }

                list.add(SkipInterval(startTime = start, endTime = end, type = skipType, provider = "animeskip"))
            }

            Log.d(TAG, "AnimeSkip parsed ${list.size} intervals")
            list
        } catch (e: Exception) {
            Log.e(TAG, "AnimeSkip JSON parse error: ${e.message}")
            emptyList()
        }
    }

    /** Önbelleği temizle */
    fun clearCache() = cache.clear()
}
