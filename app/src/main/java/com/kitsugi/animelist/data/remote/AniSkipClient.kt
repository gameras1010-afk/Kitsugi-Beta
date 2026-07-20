package com.kitsugi.animelist.data.remote

import android.util.Log
import com.kitsugi.animelist.core.network.KitsugiHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * AniSkip API istemcisi.
 *
 * AniSkip (api.aniskip.com) bir anime bölümünün intro (OP), outro (ED) ve özet (recap)
 * zaman aralıklarını MAL ID + bölüm numarasına göre döndürür.
 *
 * Endpoint: GET https://api.aniskip.com/v2/skip-times/{malId}/{episode}?types[]=op&...
 * Kimlik doğrulama gerektirmez — tamamen açık bir API.
 *
 * Rate Limit: Belgelenmiş bir limit yok; yine de 60 dk TTL cache ile optimize edilmiştir.
 */
data class SkipInterval(
    /** Saniye cinsinden başlangıç zamanı */
    val startTime: Double,
    /** Saniye cinsinden bitiş zamanı */
    val endTime: Double,
    /**
     * Segment türü:
     *  - "op"       → Açılış jeneriği
     *  - "ed"       → Kapanış jeneriği
     *  - "recap"    → Bölüm özeti
     *  - "mixed-op" → İçiçe geçmiş açılış
     *  - "mixed-ed" → İçiçe geçmiş kapanış
     */
    val type: String,
    /** Her zaman "aniskip" */
    val provider: String = "aniskip"
) {
    /** Kullanıcıya gösterilecek buton etiketi */
    val skipLabel: String get() = when (type.lowercase()) {
        "op", "mixed-op" -> "İntroyu Atla"
        "ed", "mixed-ed" -> "Bitişi Atla"
        "recap"          -> "Özeti Atla"
        else             -> "Atla"
    }
}

object AniSkipClient {

    private const val TAG = "AniSkipClient"
    private const val BASE_URL = "https://api.aniskip.com/v2/skip-times"
    private const val CACHE_TTL_MS = 60L * 60L * 1_000L // 60 dakika

    private data class CacheEntry(val intervals: List<SkipInterval>, val expiresAt: Long)

    // Anahtar: "$malId:$episode"
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    /** İstenen segment türleri — gereksinime göre genişletilebilir */
    private val TYPES = listOf("op", "ed", "recap", "mixed-op", "mixed-ed")

    /**
     * Belirtilen bölüm için skip aralıklarını getirir.
     *
     * @param malId   MyAnimeList ID'si
     * @param episode Bölüm numarası (1'den başlar)
     * @return Sıfır veya daha fazla [SkipInterval] listesi
     */
    suspend fun getSkipTimes(malId: Int, episode: Int): List<SkipInterval> =
        withContext(Dispatchers.IO) {
            val cacheKey = "$malId:$episode"
            val now = System.currentTimeMillis()
            cache[cacheKey]?.let { entry ->
                if (entry.expiresAt > now) return@withContext entry.intervals
                cache.remove(cacheKey)
            }

            val typesParam = TYPES.joinToString("&") { "types[]=$it" }
            val url = "$BASE_URL/$malId/$episode?$typesParam&episodeLength=0"

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "KitsugiAnimeList/1.0")
                .build()

            val intervals = try {
                KitsugiHttpClient.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.d(TAG, "AniSkip: veri bulunamadı — MAL=$malId E$episode (HTTP ${response.code})")
                        return@withContext emptyList()
                    }
                    val body = response.body?.string() ?: return@withContext emptyList()
                    parseIntervals(body)
                }
            } catch (e: Exception) {
                Log.w(TAG, "AniSkip ağ hatası — MAL=$malId E$episode: ${e.message}")
                emptyList()
            }

            cache[cacheKey] = CacheEntry(intervals, now + CACHE_TTL_MS)
            intervals
        }

    private fun parseIntervals(json: String): List<SkipInterval> {
        return try {
            val root = JSONObject(json)
            if (!root.optBoolean("found", false)) return emptyList()

            val resultsArr = root.optJSONArray("results") ?: return emptyList()
            val list = mutableListOf<SkipInterval>()

            for (i in 0 until resultsArr.length()) {
                val item = resultsArr.optJSONObject(i) ?: continue
                val skipType = item.optString("skipType").takeIf { it.isNotBlank() } ?: continue
                val interval = item.optJSONObject("interval") ?: continue
                val start = interval.optDouble("startTime", Double.NaN)
                val end = interval.optDouble("endTime", Double.NaN)
                if (start.isNaN() || end.isNaN() || end <= start) continue
                list.add(SkipInterval(startTime = start, endTime = end, type = skipType))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "AniSkip JSON parse hatası: ${e.message}")
            emptyList()
        }
    }

    /** Önbelleği temizle (test veya çıkış senaryoları için) */
    fun clearCache() = cache.clear()
}
