package com.kitsugi.animelist.data.remote

import android.content.Context
import android.util.Log
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Bölüm listesi çekme işlevlerini barındıran istemci.
 * Diğer metotlar (social, relations, mutations client'larına) bölünmüştür.
 */
class KitsugiMediaTabsClient {

    suspend fun fetchEpisodes(
        source: String,
        externalId: Int?,
        mediaType: MediaType,
        realMalId: Int? = null,
        totalEpisodes: Int? = null,
        context: Context? = null,
        targetSeason: Int? = null,
        tmdbId: Int? = null
    ): List<KitsugiStreamingEpisode> {
        return withContext(Dispatchers.IO) {
            if (externalId == null || externalId <= 0) return@withContext emptyList()
            if (mediaType == MediaType.Movie) return@withContext emptyList()

            val effectiveSource = source.lowercase()
            val effectiveId = externalId

            if (effectiveSource == "jikan" || effectiveSource == "mal" || effectiveSource == "anilist") {
                val list = when (effectiveSource) {
                    "jikan", "mal" -> {
                        fetchEpisodesFromJikan(effectiveId)
                    }
                    "anilist" -> {
                        fetchEpisodesFromAniList(effectiveId)
                    }
                    else -> emptyList()
                }

                var finalTotalEpisodes = totalEpisodes
                if (finalTotalEpisodes == null && context != null) {
                    try {
                        val db = KitsugiDatabase.getDatabase(context.applicationContext)
                        val entry = db.mediaEntryDao().getByMalId(effectiveId)
                            ?: if (realMalId != null) db.mediaEntryDao().getByMalId(realMalId) else null
                        if (entry != null) {
                            finalTotalEpisodes = entry.total
                        }
                    } catch (e: Exception) {
                        Log.e("KitsugiMediaTabsClient", "Error querying total episodes from DB", e)
                    }
                }

                // Check if the media is currently airing
                val cachedDetail = DetailCache.getMediaDetail(source, effectiveId)
                    ?: if (realMalId != null) DetailCache.getMediaDetail("mal", realMalId) else null
                    ?: if (realMalId != null) DetailCache.getMediaDetail("anilist", realMalId) else null
                val isAiring = cachedDetail?.status?.let { s ->
                    s.contains("airing", ignoreCase = true) ||
                    s.contains("releasing", ignoreCase = true) ||
                    s.contains("yayında", ignoreCase = true) ||
                    s.contains("devam eden", ignoreCase = true) ||
                    s.contains("yapımda", ignoreCase = true) ||
                    s.contains("yayımlanıyor", ignoreCase = true)
                } == true

                val maxFetchedEp = list.maxOfOrNull { it.episodeNumber ?: 0 } ?: 0
                val limit = if (isAiring && maxFetchedEp > 0) {
                    maxFetchedEp
                } else {
                    maxOf(finalTotalEpisodes ?: 0, maxFetchedEp)
                }
                val finalLimit = if (limit > 0) limit else 12

                if (list.size < finalLimit) {
                    val existingMap = list.associateBy { it.episodeNumber ?: -1 }
                    val paddedList = mutableListOf<KitsugiStreamingEpisode>()
                    for (epNum in 1..finalLimit) {
                        val existing = existingMap[epNum]
                        if (existing != null) {
                            paddedList.add(existing)
                        } else {
                            paddedList.add(
                                KitsugiStreamingEpisode(
                                    title = "Bölüm $epNum",
                                    thumbnail = null,
                                    url = null,
                                    site = if (effectiveSource == "anilist") "AniList" else "MyAnimeList",
                                    seasonNumber = targetSeason ?: 1,
                                    episodeNumber = epNum
                                )
                            )
                        }
                    }
                    return@withContext paddedList
                }
                return@withContext list
            }

            val list: List<KitsugiStreamingEpisode> = when (effectiveSource) {
                "simkl" -> {
                    // Simkl için önce realMalId varsa Jikan'dan çek (anime)
                    if (realMalId != null && realMalId > 0) {
                        val jList = fetchEpisodesFromJikan(realMalId)
                        if (jList.isNotEmpty()) return@withContext jList
                    }
                    emptyList()
                }
                else -> emptyList()
            }

            var finalTotalEpisodes = totalEpisodes
            if (finalTotalEpisodes == null && context != null) {
                try {
                    val db = KitsugiDatabase.getDatabase(context.applicationContext)
                    val entry = db.mediaEntryDao().getByMalId(externalId)
                        ?: if (realMalId != null) db.mediaEntryDao().getByMalId(realMalId) else null
                    if (entry != null) {
                        finalTotalEpisodes = entry.total
                    }
                } catch (e: Exception) {
                    Log.e("KitsugiMediaTabsClient", "Error querying total episodes from DB", e)
                }
            }

            if (mediaType == MediaType.Anime || mediaType == MediaType.TvShow) {
                // Fetch TMDB episodes if available
                var tmdbEpisodes: List<KitsugiEpisodeRatingsRepository.TmdbEpisodeDto> = emptyList()
                try {
                    val tmdbIdVal = when (effectiveSource) {
                        "simkl" -> {
                            if (tmdbId != null && tmdbId > 0) {
                                tmdbId
                            } else if (realMalId != null && realMalId > 0) {
                                KitsugiEpisodeRatingsRepository.getResolvedTmdbIdForMal(realMalId)
                            } else null
                        }
                        "tmdb" -> effectiveId
                        else -> KitsugiEpisodeRatingsRepository.getResolvedTmdbIdForMal(effectiveId)
                    }

                    val resolvedId = tmdbIdVal ?: run {
                        when (effectiveSource) {
                            "simkl" -> {
                                if (tmdbId != null && tmdbId > 0) {
                                    tmdbId
                                } else if (realMalId != null && realMalId > 0) {
                                    KitsugiEpisodeRatingsRepository.resolveTmdbIdFromMal(realMalId)
                                } else null
                            }
                            "tmdb" -> effectiveId
                            else -> KitsugiEpisodeRatingsRepository.resolveTmdbIdFromMal(effectiveId)
                        }
                    }

                    if (resolvedId != null && resolvedId > 0) {
                        val sNum = targetSeason ?: 1
                        tmdbEpisodes = KitsugiEpisodeRatingsRepository.getTmdbEpisodes(resolvedId, sNum)
                        Log.d("KitsugiMediaTabsClient", "Fetched ${tmdbEpisodes.size} episodes from TMDB for tmdbId=$resolvedId, season=$sNum")
                    }
                } catch (e: Exception) {
                    Log.e("KitsugiMediaTabsClient", "Error resolving TMDB ID or fetching TMDB episodes", e)
                }

                // Filter out future (not yet aired) episodes from TMDB episodes
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                val filteredTmdbEpisodes = tmdbEpisodes.filter { ep ->
                    val date = ep.airDate
                    date.isNullOrBlank() || date <= todayStr
                }

                val maxFetchedEp = maxOf(
                    list.maxOfOrNull { it.episodeNumber ?: 0 } ?: 0,
                    filteredTmdbEpisodes.maxOfOrNull { it.episodeNumber } ?: 0
                )

                // Check if the media is currently airing
                val cachedDetail = DetailCache.getMediaDetail(source, effectiveId)
                    ?: if (realMalId != null) DetailCache.getMediaDetail("mal", realMalId) else null
                    ?: if (realMalId != null) DetailCache.getMediaDetail("anilist", realMalId) else null
                val isAiring = cachedDetail?.status?.let { s ->
                    s.contains("airing", ignoreCase = true) ||
                    s.contains("releasing", ignoreCase = true) ||
                    s.contains("yayında", ignoreCase = true) ||
                    s.contains("devam eden", ignoreCase = true) ||
                    s.contains("yapımda", ignoreCase = true) ||
                    s.contains("yayımlanıyor", ignoreCase = true)
                } == true

                val limit = if (filteredTmdbEpisodes.isNotEmpty() || (isAiring && maxFetchedEp > 0)) {
                    // TMDB season episodes are fetched or currently airing:
                    // They define the season's episode count. We don't pad to the entire show's total episodes.
                    maxFetchedEp
                } else {
                    // Fallback to Jikan/AniList list and/or total episodes
                    maxOf(finalTotalEpisodes ?: 0, maxFetchedEp)
                }
                val finalLimit = if (limit > 0) limit else 12

                val existingMap = list.associateBy { it.episodeNumber ?: -1 }
                val tmdbMap = filteredTmdbEpisodes.associateBy { it.episodeNumber }
                val resultList = mutableListOf<KitsugiStreamingEpisode>()

                for (epNum in 1..finalLimit) {
                    val existing = existingMap[epNum]
                    val tmdbEp = tmdbMap[epNum]

                    val tmdbThumb = tmdbEp?.stillPath?.let { "https://image.tmdb.org/t/p/w300$it" }
                    val tmdbCleanName = tmdbEp?.name?.trim()?.takeIf { name ->
                        name.isNotBlank() &&
                        !name.equals("Episode $epNum", ignoreCase = true) &&
                        !name.equals("Bölüm $epNum", ignoreCase = true) &&
                        !name.matches(Regex("""^\d+(\.\s*)?(Bölüm|Episode)?$""", RegexOption.IGNORE_CASE))
                    }

                    val finalTitle = when {
                        existing != null -> {
                            val isPlaceholder = existing.title.matches(Regex("""^(#\d+\s*–\s*)?(Bölüm|Episode)\s*\d+$""", RegexOption.IGNORE_CASE)) ||
                                                existing.title.matches(Regex("""^(Bölüm|Episode)\s*\d+$""", RegexOption.IGNORE_CASE))
                            if (isPlaceholder && tmdbCleanName != null) {
                                "#$epNum – $tmdbCleanName"
                            } else {
                                existing.title
                            }
                        }
                        tmdbCleanName != null -> {
                            "#$epNum – $tmdbCleanName"
                        }
                        else -> {
                            "Bölüm $epNum"
                        }
                    }

                    if (existing != null) {
                        resultList.add(
                            existing.copy(
                                title = finalTitle,
                                thumbnail = existing.thumbnail ?: tmdbThumb,
                                seasonNumber = targetSeason ?: existing.seasonNumber ?: 1
                            )
                        )
                    } else {
                        resultList.add(
                            KitsugiStreamingEpisode(
                                title = finalTitle,
                                thumbnail = tmdbThumb,
                                url = null,
                                site = "TMDB",
                                seasonNumber = targetSeason ?: 1,
                                episodeNumber = epNum
                            )
                        )
                    }
                }
                resultList
            } else {
                list
            }
        }
    }

    private suspend fun fetchEpisodesFromJikan(malId: Int): List<KitsugiStreamingEpisode> {
        val list = mutableListOf<KitsugiStreamingEpisode>()
        for (page in 1..3) {
            val url = URL("https://api.jikan.moe/v4/anime/$malId/episodes?page=$page")
            val pageList = runCatching {
                KitsugiApiBase.runWithRateLimit {
                    val response = KitsugiApiBase.executeGetRequest(url) ?: return@runWithRateLimit emptyList<KitsugiStreamingEpisode>()
                    val root = JSONObject(response)
                    val data = root.optJSONArray("data") ?: return@runWithRateLimit emptyList<KitsugiStreamingEpisode>()
                    val pageItems = mutableListOf<KitsugiStreamingEpisode>()
                    for (i in 0 until data.length()) {
                        val ep = data.optJSONObject(i) ?: continue
                        val epNum = ep.optInt("mal_id", i + 1)
                        val titleRomaji = ep.optNullableString("title")
                        val titleEn = ep.optNullableString("title_english")
                        val epTitle = titleRomaji ?: titleEn ?: "Bölüm $epNum"
                        pageItems.add(
                            KitsugiStreamingEpisode(
                                title = "#$epNum – $epTitle",
                                thumbnail = null,
                                url = ep.optNullableString("url"),
                                site = "MyAnimeList",
                                seasonNumber = 1,
                                episodeNumber = epNum
                            )
                        )
                    }
                    pageItems
                }
            }.getOrElse { emptyList() }

            list.addAll(pageList)
            if (pageList.size < 100) break
        }
        return list
    }

    private suspend fun fetchEpisodesFromAniList(externalId: Int): List<KitsugiStreamingEpisode> {
        val aniListId = if (externalId >= 100_000_000) externalId - 100_000_000 else null
        val idParam = if (aniListId != null) "\$id: Int" else "\$idMal: Int"
        val idFilter = if (aniListId != null) "id: \$id" else "idMal: \$idMal"
        val query = """
            query (${'$'}type: MediaType, $idParam) {
                Media($idFilter, type: ${'$'}type) {
                    streamingEpisodes { title thumbnail url site }
                }
            }
        """.trimIndent()
        val variables = JSONObject().put("type", "ANIME")
        if (aniListId != null) variables.put("id", aniListId) else variables.put("idMal", externalId)

        return runCatching {
            val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return emptyList()
            val root = JSONObject(response)
            val eps = root.optJSONObject("data")?.optJSONObject("Media")?.optJSONArray("streamingEpisodes")
                ?: return emptyList()
            val list = mutableListOf<KitsugiStreamingEpisode>()
            for (i in 0 until eps.length()) {
                val ep = eps.optJSONObject(i) ?: continue
                val title = ep.optNullableString("title") ?: "Bölüm ${i + 1}"
                val thumb = ep.optNullableString("thumbnail")
                val url = ep.optNullableString("url")
                val site = ep.optNullableString("site")
                val epNum = parseEpisodeNumberFromTitle(title) ?: (i + 1)
                list.add(
                    KitsugiStreamingEpisode(
                        title = title,
                        thumbnail = thumb,
                        url = url,
                        site = site,
                        seasonNumber = 1,
                        episodeNumber = epNum
                    )
                )
            }
            list
        }.getOrElse { emptyList() }
    }

    private fun parseEpisodeNumberFromTitle(title: String): Int? {
        val patterns = listOf(
            Regex("""[Ss]\d+[Ee](\d+)"""),
            Regex("""[Ee]p(?:isode)?\.?\s*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""#(\d+)"""),
            Regex("""B[öo]l[üu]m\s+(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""^(\d+)\s*[-–—.]"""),
            Regex("""\b(\d+)\b""")
        )
        for (pattern in patterns) {
            val num = pattern.find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (num != null && num > 0 && num < 5000) return num
        }
        return null
    }
}
