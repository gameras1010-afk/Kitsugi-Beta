package com.kitsugi.animelist.data.remote

import android.util.Log
import com.kitsugi.animelist.core.network.KitsugiHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

object KitsuClient {
    private const val TAG = "KitsuClient"
    private const val BASE = "https://kitsu.io/api/edge"

    suspend fun fetchAnimeDetail(kitsuIdOrSlug: String): KitsugiMediaDetail? = withContext(Dispatchers.IO) {
        val url = "$BASE/anime/$kitsuIdOrSlug"
        executeGet(url)
    }

    suspend fun fetchAnimeDetailByTitle(title: String): KitsugiMediaDetail? = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(title, "UTF-8")
        val url = "$BASE/anime?filter[text]=$encoded&page[limit]=1"
        executeGet(url, isArrayResponse = true)
    }

    // ─── Character Fetching ───────────────────────────────────────────────────

    /**
     * Kitsu anime-characters endpoint'inden karakter listesi çeker (paginated).
     * JSON:API: data[] = anime-character join records (role attr), included[] = character objects.
     * @param kitsuNumericId Kitsu'nun kendi numeric ID'si (300_000_000 offset olmadan)
     */
    suspend fun fetchKitsuCharacters(kitsuNumericId: Int): List<KitsugiCharacter> = withContext(Dispatchers.IO) {
        val allCharacters = mutableListOf<KitsugiCharacter>()
        var offset = 0
        val limit = 20
        val maxPages = 5 // max 100 characters

        repeat(maxPages) {
            val pageResult = fetchCharacterPage(kitsuNumericId, limit, offset)
            allCharacters.addAll(pageResult.first)
            if (pageResult.second < limit) return@withContext allCharacters
            offset += limit
        }
        allCharacters
    }

    private fun fetchCharacterPage(
        kitsuNumericId: Int,
        limit: Int,
        offset: Int
    ): Pair<List<KitsugiCharacter>, Int> {
        val url = "$BASE/anime-characters" +
            "?filter[animeId]=$kitsuNumericId" +
            "&include=character" +
            "&fields[characters]=name,image" +
            "&page[limit]=$limit&page[offset]=$offset"
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.api+json")
                .header("User-Agent", "Kitsugi/1.0 (Android)")
                .build()

            KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code} for anime-characters")
                    return Pair(emptyList(), 0)
                }
                val body = response.body?.string() ?: return Pair(emptyList(), 0)
                val root = JSONObject(body)

                val dataArr = root.optJSONArray("data") ?: return Pair(emptyList(), 0)
                val includedArr = root.optJSONArray("included") ?: JSONArray()

                // Build map: character id -> character JSON object from "included"
                val characterMap = mutableMapOf<String, JSONObject>()
                for (i in 0 until includedArr.length()) {
                    val inc = includedArr.optJSONObject(i) ?: continue
                    if (inc.optString("type") == "characters") {
                        val cid = inc.optString("id", "")
                        if (cid.isNotBlank()) characterMap[cid] = inc
                    }
                }

                val results = mutableListOf<KitsugiCharacter>()
                for (i in 0 until dataArr.length()) {
                    val item = dataArr.optJSONObject(i) ?: continue
                    val attrs = item.optJSONObject("attributes") ?: continue
                    val role = when (attrs.optString("role", "supporting").lowercase()) {
                        "main"       -> "Ana Karakter"
                        "supporting" -> "Yardımcı Karakter"
                        else         -> "Yardımcı Karakter"
                    }

                    val charRelId = item.optJSONObject("relationships")
                        ?.optJSONObject("character")
                        ?.optJSONObject("data")
                        ?.optString("id", "") ?: ""

                    val charObj = characterMap[charRelId] ?: continue
                    val charAttrs = charObj.optJSONObject("attributes") ?: continue
                    val charIdInt = charObj.optString("id", "0").toIntOrNull() ?: continue

                    val name = charAttrs.optString("name", "").takeIf { it.isNotBlank() } ?: "Bilinmeyen"
                    val imageObj = charAttrs.optJSONObject("image")
                    val imageUrl = imageObj?.optString("original")?.takeIf { it.isNotBlank() }
                        ?: imageObj?.optString("large")?.takeIf { it.isNotBlank() }
                        ?: imageObj?.optString("medium")?.takeIf { it.isNotBlank() }

                    results.add(
                        KitsugiCharacter(
                            id = charIdInt,
                            name = name,
                            role = role,
                            imageUrl = imageUrl,
                            voiceActors = emptyList(),
                            source = "kitsu"
                        )
                    )
                }
                Log.d(TAG, "Kitsu characters page (offset=$offset): ${results.size} chars")
                Pair(results, dataArr.length())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Kitsu character page: ${e.message}", e)
            Pair(emptyList(), 0)
        }
    }

    // ─── Episode Fetching ─────────────────────────────────────────────────────

    /**
     * Kitsu episodes endpoint'inden bölüm listesi çeker (paginated, max 200 episode).
     * @param kitsuNumericId Kitsu numeric ID'si (300_000_000 offset olmadan)
     */
    suspend fun fetchKitsuEpisodes(kitsuNumericId: Int): List<KitsugiStreamingEpisode> = withContext(Dispatchers.IO) {
        val allEpisodes = mutableListOf<KitsugiStreamingEpisode>()
        var offset = 0
        val limit = 20
        val maxPages = 10 // max 200 episodes

        repeat(maxPages) {
            val pageResult = fetchEpisodePage(kitsuNumericId, limit, offset)
            allEpisodes.addAll(pageResult.first)
            if (pageResult.second < limit) return@withContext allEpisodes
            offset += limit
        }
        allEpisodes
    }

    private fun fetchEpisodePage(
        kitsuNumericId: Int,
        limit: Int,
        offset: Int
    ): Pair<List<KitsugiStreamingEpisode>, Int> {
        val url = "$BASE/episodes" +
            "?filter[mediaId]=$kitsuNumericId" +
            "&page[limit]=$limit&page[offset]=$offset" +
            "&sort=number"
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.api+json")
                .header("User-Agent", "Kitsugi/1.0 (Android)")
                .build()

            KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code} for episodes (kitsuId=$kitsuNumericId)")
                    return Pair(emptyList(), 0)
                }
                val body = response.body?.string() ?: return Pair(emptyList(), 0)
                val root = JSONObject(body)
                val dataArr = root.optJSONArray("data") ?: return Pair(emptyList(), 0)

                val results = mutableListOf<KitsugiStreamingEpisode>()
                for (i in 0 until dataArr.length()) {
                    val ep = dataArr.optJSONObject(i) ?: continue
                    val attrs = ep.optJSONObject("attributes") ?: continue
                    val epNum = attrs.optInt("number", i + 1)
                    val titlesObj = attrs.optJSONObject("titles")
                    val titleEn = titlesObj?.optString("en_us", "")?.takeIf { it.isNotBlank() }
                        ?: titlesObj?.optString("en_jp", "")?.takeIf { it.isNotBlank() }
                        ?: attrs.optString("canonicalTitle", "").takeIf { it.isNotBlank() }
                    val thumbnail = attrs.optJSONObject("thumbnail")
                        ?.optString("original")?.takeIf { it.isNotBlank() }
                    val title = if (!titleEn.isNullOrBlank()) "#$epNum – $titleEn" else "Bölüm $epNum"

                    results.add(
                        KitsugiStreamingEpisode(
                            title = title,
                            thumbnail = thumbnail,
                            url = null,
                            site = "Kitsu",
                            seasonNumber = 1,
                            episodeNumber = epNum
                        )
                    )
                }
                Log.d(TAG, "Kitsu episodes page (offset=$offset): ${results.size} eps")
                Pair(results, dataArr.length())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Kitsu episode page: ${e.message}", e)
            Pair(emptyList(), 0)
        }
    }

    private suspend fun executeGet(urlStr: String, isArrayResponse: Boolean = false): KitsugiMediaDetail? {
        try {
            val request = Request.Builder()
                .url(urlStr)
                .header("Accept", "application/vnd.api+json")
                .header("Content-Type", "application/vnd.api+json")
                .header("User-Agent", "Kitsugi/1.0 (Android)")
                .build()
            KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP ${response.code} for $urlStr")
                    return null
                }
                val body = response.body?.string() ?: return null
                val root = JSONObject(body)
                val dataObj = if (isArrayResponse) {
                    val arr = root.optJSONArray("data")
                    if (arr != null && arr.length() > 0) arr.getJSONObject(0) else null
                } else {
                    root.optJSONObject("data")
                }
                return dataObj?.let { parseKitsuAnime(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Kitsu: ${e.message}", e)
            return null
        }
    }

    private fun parseKitsuAnime(data: JSONObject): KitsugiMediaDetail {
        val id = data.optString("id", "")
        val attributes = data.getJSONObject("attributes")

        val canonicalTitle = attributes.optString("canonicalTitle", "")
        val titlesObj = attributes.optJSONObject("titles")
        val titleEnglish = titlesObj?.optString("en", "").takeIf { !it.isNullOrBlank() } ?: canonicalTitle
        val titleRomaji = titlesObj?.optString("en_jp", "").takeIf { !it.isNullOrBlank() } ?: canonicalTitle
        val titleNative = titlesObj?.optString("ja_jp", "").takeIf { !it.isNullOrBlank() }
        val synonyms = attributes.optJSONArray("abbreviatedTitles")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        } ?: emptyList()

        val synopsis = attributes.optString("synopsis", "").takeIf { it.isNotBlank() }
        val statusVal = attributes.optString("status", "").toKitsuTurkishStatus()
        val startDate = attributes.optString("startDate", "").takeIf { it.isNotBlank() }
        val endDate = attributes.optString("endDate", "").takeIf { it.isNotBlank() }
        val yearVal = startDate?.take(4)?.toIntOrNull()

        val episodeCount = attributes.optInt("episodeCount", 0)
        val episodeLength = attributes.optInt("episodeLength", 0)
        val duration = if (episodeLength > 0) "$episodeLength dk" else null

        val averageRating = attributes.optString("averageRating", "0")
        val ratingDouble = averageRating.toDoubleOrNull() ?: 0.0
        val scoreVal = if (ratingDouble > 0) (ratingDouble / 10.0).toInt().coerceIn(1, 10) else null
        val meanScore = if (ratingDouble > 0) ratingDouble.toInt() else null

        val userCount = attributes.optInt("userCount", 0).takeIf { it > 0 }
        val favCount = attributes.optInt("favoritesCount", 0).takeIf { it > 0 }
        val ratingRank = attributes.optInt("ratingRank", 0).takeIf { it > 0 }
        val popularityRank = attributes.optInt("popularityRank", 0).takeIf { it > 0 }

        val ageRating = attributes.optString("ageRating", "")
        val ageRatingGuide = attributes.optString("ageRatingGuide", "")
        val ratingStr = when (ageRating.uppercase()) {
            "G"   -> "G - Her Yaştan"
            "PG"  -> "PG - Çocuklar"
            "R"   -> "R - 17+ (Şiddet & Küfür)"
            "R18" -> "Rx - Hentai"
            else  -> ageRatingGuide.takeIf { it.isNotBlank() }
        }
        val isAdult = ageRating.equals("R18", ignoreCase = true)

        val posterObj = attributes.optJSONObject("posterImage")
        val imageUrl = posterObj?.optString("medium")?.takeIf { it.isNotBlank() }
            ?: posterObj?.optString("original")?.takeIf { it.isNotBlank() }

        val youtubeVideoId = attributes.optString("youtubeVideoId", "")
        val trailerUrl = if (youtubeVideoId.isNotEmpty()) "https://www.youtube.com/watch?v=$youtubeVideoId" else null

        val links = mutableListOf<KitsugiExternalLink>()
        links.add(KitsugiExternalLink("Kitsu", "https://kitsu.io/anime/$id", "EN"))

        return KitsugiMediaDetail(
            synopsis = synopsis,
            status = statusVal,
            titleEnglish = titleEnglish,
            titleRomaji = titleRomaji,
            titleNative = titleNative,
            title = canonicalTitle,
            imageUrl = imageUrl,
            score = scoreVal,
            year = yearVal,
            total = if (episodeCount > 0) episodeCount else null,
            episodeDuration = duration,
            startDate = startDate,
            endDate = endDate,
            trailerUrl = trailerUrl,
            externalLinks = links,
            synonyms = synonyms,
            rating = ratingStr,
            isAdult = isAdult,
            meanScore = meanScore,
            averageScore = meanScore,
            members = userCount,
            favorites = favCount,
            rank = ratingRank,
            popularityRank = popularityRank
        )
    }

    private fun String.toKitsuTurkishStatus(): String {
        return when (this.lowercase()) {
            "current"    -> "Devam Ediyor"
            "finished"   -> "Tamamlandı"
            "tba"        -> "Bilinmiyor"
            "unreleased" -> "Yayınlanmadı"
            "upcoming"   -> "Yakında"
            else         -> this
        }
    }
}
