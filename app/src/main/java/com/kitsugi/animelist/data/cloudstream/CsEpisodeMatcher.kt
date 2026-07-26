package com.kitsugi.animelist.data.cloudstream

import android.util.Log
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse

/**
 * Bölüm verisi çıkarma yardımcıları.
 * [CsStreamRunner] içindeki episode matching mantığı buraya taşındı.
 */
internal object CsEpisodeMatcher {

    private const val TAG = "CsEpisodeMatcher"

    /**
     * Verilen [LoadResponse] içinde belirtilen sezon ve bölüme karşılık gelen
     * bölüm verisini (episode data string) döndürür.
     * Bulunamazsa `null` döner.
     */
    fun findEpisodeData(response: LoadResponse, season: Int, episode: Int): String? {
        return try {
            when (response) {
                is AnimeLoadResponse -> findInAnimeResponse(response, season, episode)
                is TvSeriesLoadResponse -> findInTvSeriesResponse(response, season, episode)
                is MovieLoadResponse -> {
                    Log.d(TAG, "MovieLoadResponse: dataUrl=${response.dataUrl}")
                    response.dataUrl
                }
                else -> {
                    Log.w(TAG, "Bilinmeyen LoadResponse tipi: ${response.javaClass.name}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "findEpisodeData HATA", e)
            null
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private fun findInAnimeResponse(response: AnimeLoadResponse, season: Int, episode: Int): String? {
        // Log all available episodes for debugging
        val allEntries = response.episodes.entries
        Log.d(TAG, "AnimeLoadResponse: ${allEntries.size} sezon bucket(ı)")
        allEntries.forEach { (dubStatus, eps) ->
            Log.d(TAG, "  DubStatus=$dubStatus bölüm sayısı=${eps.size}")
        }

        // Separate Dub and Sub, prioritize Subbed, then Dubbed, then None
        val subEpisodes = response.episodes[com.lagradost.cloudstream3.DubStatus.Subbed] ?: emptyList()
        val dubEpisodes = response.episodes[com.lagradost.cloudstream3.DubStatus.Dubbed] ?: emptyList()
        val noneEpisodes = response.episodes[com.lagradost.cloudstream3.DubStatus.None] ?: emptyList()

        val preferredEpisodes = when {
            subEpisodes.isNotEmpty() -> subEpisodes
            dubEpisodes.isNotEmpty() -> dubEpisodes
            else -> noneEpisodes
        }

        // Flatten all episodes once
        val allEpisodes = allEntries.flatMap { it.value }

        // Try: season+episode exact match on preferred bucket
        var match = preferredEpisodes.find { ep ->
            val epNum = getEpisodeNumber(ep) ?: return@find false
            val epSeason = getEpisodeSeason(ep) ?: 1
            epSeason == season && epNum == episode
        }

        // Fallback 1: search across ALL episode buckets
        if (match == null) {
            match = allEpisodes.find { ep ->
                val epNum = getEpisodeNumber(ep) ?: return@find false
                val epSeason = getEpisodeSeason(ep) ?: 1
                epSeason == season && epNum == episode
            }
        }

        // Fallback 2: match by episode number only (season-agnostic) within preferred bucket
        if (match == null) {
            match = preferredEpisodes.find { ep ->
                (getEpisodeNumber(ep) ?: -1) == episode
            }
            if (match != null) Log.d(TAG, "Preferred bucket sezon-bağımsız eşleşme: ep=$episode")
        }

        // Fallback 3: match by episode number across all buckets
        if (match == null) {
            match = allEpisodes.find { ep ->
                (getEpisodeNumber(ep) ?: -1) == episode
            }
            if (match != null) Log.d(TAG, "Sezon bağımsız bölüm eşleşmesi kullanıldı: ep=$episode")
        }

        // Fallback 4: none of the episodes have season/episode fields set at all
        // (common in Turkish plugins) — use 1-based index within preferred bucket.
        // This avoids S2 requests falling back to S1 episodes when episodes are
        // stored as a flat list without metadata.
        if (match == null && preferredEpisodes.isNotEmpty()) {
            val allHaveNoMeta = preferredEpisodes.all { ep ->
                getEpisodeNumber(ep) == null && getEpisodeSeason(ep) == null
            }
            if (allHaveNoMeta) {
                val idx = episode - 1  // episode is 1-based
                if (idx in preferredEpisodes.indices) {
                    match = preferredEpisodes[idx]
                    Log.d(TAG, "İndeks-bazlı fallback kullanıldı (meta yok): bucket[${idx}] → ep=$episode")
                }
            }
        }

        // Fallback 5: if there's exactly 1 episode and we want ep 1
        if (match == null && episode == 1 && allEpisodes.size == 1) {
            match = allEpisodes.first()
            Log.d(TAG, "Tek bölüm fallback kullanıldı")
        }

        return match?.let { getEpisodeData(it) }
    }

    private fun findInTvSeriesResponse(response: TvSeriesLoadResponse, season: Int, episode: Int): String? {
        Log.d(TAG, "TvSeriesLoadResponse: ${response.episodes.size} bölüm")

        var match = response.episodes.find { ep ->
            val epNum = getEpisodeNumber(ep) ?: return@find false
            val epSeason = getEpisodeSeason(ep) ?: 1
            epSeason == season && epNum == episode
        }

        // Fallback: episode number only
        if (match == null) {
            match = response.episodes.find { ep ->
                (getEpisodeNumber(ep) ?: -1) == episode
            }
            if (match != null) Log.d(TAG, "TvSeries: sezon bağımsız fallback ep=$episode")
        }

        return match?.let { getEpisodeData(it) }
    }

    // ─── Reflection helpers ──────────────────────────────────────────────────

    fun getEpisodeNumber(ep: Any): Int? {
        return try { getField(ep, "episode") }
        catch (_: Exception) { null }
    }

    fun getEpisodeSeason(ep: Any): Int? {
        return try { getField(ep, "season") }
        catch (_: Exception) { null }
    }

    fun getEpisodeData(ep: Any): String? {
        return try {
            val field = ep.javaClass.getDeclaredField("data")
            field.isAccessible = true
            field.get(ep) as? String
        } catch (_: Exception) {
            try {
                ep.javaClass.getMethod("getData").invoke(ep) as? String
            } catch (_: Exception) { null }
        }
    }

    private fun getField(obj: Any, name: String): Int? {
        // Walk the class hierarchy to find the field
        var clazz: Class<*>? = obj.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(name)
                field.isAccessible = true
                val value = field.get(obj)
                return when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull()
                    else -> value?.toString()?.toIntOrNull()
                }
            } catch (_: NoSuchFieldException) {
                clazz = clazz.superclass
            } catch (_: Exception) {
                break
            }
        }
        // Fallback: try getter method
        return try {
            val getter = obj.javaClass.getMethod("get${name.replaceFirstChar { it.uppercase() }}")
            val value = getter.invoke(obj)
            when (value) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull()
                else -> value?.toString()?.toIntOrNull()
            }
        } catch (_: Exception) { null }
    }
}
