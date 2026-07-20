package com.kitsugi.animelist.data.remote

import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.kitsugi.animelist.utils.toTurkishRelationType

/**
 * İki medya kimliği çözümleme ve ilişki çekme işlemlerini yürüten istemci.
 * [KitsugiMediaTabsClient]'dan bölünmüştür.
 */
class KitsugiMediaRelationsClient {

    /**
     * Verilen MAL id'sini AniList id'sine çevirir.
     * Önce ARM API, ardından GraphQL fallback kullanır.
     */
    internal suspend fun resolveAniListId(malId: Int, mediaType: MediaType): Int? {
        if (malId <= 0) return null
        return withContext(Dispatchers.IO) {
            val armResolved = runCatching {
                KitsugiIdResolver.resolveIds(malId, null).aniListId
            }.getOrNull()
            if (armResolved != null && armResolved > 0) return@withContext armResolved

            val query = """
                query (${'$'}idMal: Int, ${'$'}type: MediaType) {
                    Media(idMal: ${'$'}idMal, type: ${'$'}type) {
                        id
                    }
                }
            """.trimIndent()
            val variables = JSONObject()
                .put("idMal", malId)
                .put("type", if (mediaType == MediaType.Anime) "ANIME" else "MANGA")
            runCatching {
                val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching null
                val root = JSONObject(response)
                root.optJSONObject("data")?.optJSONObject("Media")?.optInt("id")
            }.getOrNull()
        }
    }

    suspend fun fetchRelations(
        source: String,
        externalId: Int?,
        mediaType: MediaType,
        tmdbId: Int? = null,
        realMalId: Int? = null
    ): List<KitsugiRelation> {
        return withContext(Dispatchers.IO) {
            if (externalId == null || externalId <= 0) return@withContext emptyList()

            when (source.lowercase()) {
                "simkl" -> {
                    if (mediaType == MediaType.Anime) {
                        val malId = realMalId ?: DetailCache.getMediaDetail("simkl", externalId)?.realMalId
                        if (malId != null && malId > 0) {
                            val malList = fetchRelations("jikan", malId, mediaType, null, null)
                            if (malList.isNotEmpty()) return@withContext malList
                        }
                    }
                    val resolvedTmdb = tmdbId ?: run {
                        val malIdForResolve = realMalId ?: DetailCache.getMediaDetail("simkl", externalId)?.realMalId
                        KitsugiIdResolver.resolveIds(malId = malIdForResolve, aniListId = null, tmdbId = tmdbId).tmdbId
                    }
                    if (resolvedTmdb != null && resolvedTmdb > 0) {
                        val isMovie = mediaType == MediaType.Movie
                        val tmdbRelations = TmdbApiClient().fetchRecommendations(resolvedTmdb, isMovie)
                        if (tmdbRelations.isNotEmpty()) return@withContext tmdbRelations
                    }
                    emptyList()
                }
                "tmdb" -> {
                    val effectiveTmdbId = tmdbId ?: externalId
                    if (effectiveTmdbId > 0) {
                        val isMovie = mediaType == MediaType.Movie
                        TmdbApiClient().fetchRecommendations(effectiveTmdbId, isMovie)
                    } else emptyList()
                }
                "jikan", "mal" -> fetchRelationsFromJikan(externalId, mediaType)
                "anilist"      -> fetchRelationsFromAniList(externalId, mediaType)
                else           -> emptyList()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun fetchRelationsFromJikan(
        externalId: Int,
        mediaType: MediaType
    ): List<KitsugiRelation> {
        val endpoint = if (mediaType == MediaType.Anime) "anime" else "manga"
        val url = java.net.URL("https://api.jikan.moe/v4/$endpoint/$externalId/relations")
        return runCatching {
            KitsugiApiBase.runWithRateLimit {
                val response = KitsugiApiBase.executeGetRequest(url) ?: return@runWithRateLimit emptyList()
                val root = JSONObject(response)
                val data = root.optJSONArray("data") ?: return@runWithRateLimit emptyList()
                val list = mutableListOf<KitsugiRelation>()

                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    val relType = item.optNullableString("relation") ?: "Relation"
                    val entries = item.optJSONArray("entry") ?: continue
                    for (j in 0 until entries.length()) {
                        val entry = entries.optJSONObject(j) ?: continue
                        val relMalId = entry.optInt("mal_id")
                        val title = entry.optNullableString("name") ?: "Bilinmeyen"
                        val mTypeStr = entry.optNullableString("type").orEmpty()
                        val mType = if (mTypeStr.equals("manga", ignoreCase = true)) MediaType.Manga else MediaType.Anime
                        list.add(KitsugiRelation(
                            malId = relMalId,
                            title = title,
                            relationType = relType.toTurkishRelationType(),
                            imageUrl = null,
                            mediaType = mType,
                            source = "jikan"
                        ))
                    }
                }

                // AniList bulk sorgusuyla kapak resimlerini ekle
                if (list.isNotEmpty()) {
                    val malIds = list.map { it.malId }
                    val idsArray = JSONArray(malIds)
                    val coverQuery = """
                        query (${'$'}ids: [Int]) {
                            Page(perPage: 50) {
                                media(idMal_in: ${'$'}ids) {
                                    idMal
                                    coverImage { large }
                                }
                            }
                        }
                    """.trimIndent()
                    val coverVars = JSONObject().put("ids", idsArray)
                    val coverMap = mutableMapOf<Int, String>()
                    runCatching {
                        val coverResp = KitsugiApiBase.executeAniListQuery(coverQuery, coverVars)
                        if (coverResp != null) {
                            val coverRoot = JSONObject(coverResp)
                            val mediaArr = coverRoot.optJSONObject("data")
                                ?.optJSONObject("Page")
                                ?.optJSONArray("media")
                            if (mediaArr != null) {
                                for (k in 0 until mediaArr.length()) {
                                    val mi = mediaArr.optJSONObject(k) ?: continue
                                    val mId = mi.optInt("idMal")
                                    val img = mi.optJSONObject("coverImage")?.optNullableString("large")
                                    if (mId > 0 && img != null) coverMap[mId] = img
                                }
                            }
                        }
                    }
                    list.replaceAll { rel ->
                        coverMap[rel.malId]?.let { rel.copy(imageUrl = it) } ?: rel
                    }
                }
                list
            }
        }.getOrElse { emptyList() }
    }

    private suspend fun fetchRelationsFromAniList(
        externalId: Int,
        mediaType: MediaType
    ): List<KitsugiRelation> {
        val aniListId = if (externalId >= 100_000_000) externalId - 100_000_000 else null
        val idParam  = if (aniListId != null) "\$id: Int" else "\$idMal: Int"
        val idFilter = if (aniListId != null) "id: \$id"  else "idMal: \$idMal"
        val query = """
            query (${'$'}type: MediaType, $idParam) {
                Media($idFilter, type: ${'$'}type) {
                    relations {
                        edges {
                            relationType(version: 2)
                            node {
                                id
                                idMal
                                title { romaji english native }
                                type
                                coverImage { large }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        val variables = JSONObject().put("type", if (mediaType == MediaType.Anime) "ANIME" else "MANGA")
        if (aniListId != null) variables.put("id", aniListId) else variables.put("idMal", externalId)

        return runCatching {
            val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return emptyList()
            val root  = JSONObject(response)
            val edges = root.optJSONObject("data")
                ?.optJSONObject("Media")
                ?.optJSONObject("relations")
                ?.optJSONArray("edges") ?: return emptyList()
            val list = mutableListOf<KitsugiRelation>()
            for (i in 0 until edges.length()) {
                val edge = edges.optJSONObject(i) ?: continue
                val relType  = edge.optNullableString("relationType") ?: "Relation"
                val node     = edge.optJSONObject("node") ?: continue
                val nodeId   = node.optInt("id")
                val nodeIdMal = node.optionalPositiveInt("idMal")
                val stableId = nodeIdMal ?: (100_000_000 + nodeId)
                val titleObj = node.optJSONObject("title")
                val titleRomaji = titleObj?.optNullableString("romaji")
                val titleEnglish = titleObj?.optNullableString("english")
                val titleNative = titleObj?.optNullableString("native")
                val title = titleRomaji
                    ?: titleEnglish
                    ?: titleNative ?: "Bilinmeyen"
                val nodeTypeStr = node.optNullableString("type").orEmpty()
                val nodeType = if (nodeTypeStr.equals("manga", ignoreCase = true)) MediaType.Manga else MediaType.Anime
                val imageUrl = node.optJSONObject("coverImage")?.optNullableString("large")
                list.add(KitsugiRelation(
                    malId = stableId,
                    title = title,
                    relationType = relType.toTurkishRelationType(),
                    imageUrl = imageUrl,
                    mediaType = nodeType,
                    source = "anilist",
                    titleEnglish = titleEnglish,
                    titleJapanese = titleNative,
                    titleRomaji = titleRomaji
                ))
            }
            list
        }.getOrElse { emptyList() }
    }

    suspend fun fetchRecommendations(
        source: String,
        externalId: Int?,
        mediaType: MediaType,
        tmdbId: Int? = null,
        realMalId: Int? = null
    ): List<KitsugiRelation> {
        return withContext(Dispatchers.IO) {
            if (externalId == null || externalId <= 0) return@withContext emptyList()

            when (source.lowercase()) {
                "simkl" -> {
                    if (mediaType == MediaType.Anime) {
                        val malId = realMalId ?: DetailCache.getMediaDetail("simkl", externalId)?.realMalId
                        if (malId != null && malId > 0) {
                            val malList = fetchRecommendations("jikan", malId, mediaType, null, null)
                            if (malList.isNotEmpty()) return@withContext malList
                        }
                    }
                    val resolvedTmdb = tmdbId ?: run {
                        val malIdForResolve = realMalId ?: DetailCache.getMediaDetail("simkl", externalId)?.realMalId
                        KitsugiIdResolver.resolveIds(malId = malIdForResolve, aniListId = null, tmdbId = tmdbId).tmdbId
                    }
                    if (resolvedTmdb != null && resolvedTmdb > 0) {
                        val isMovie = mediaType == MediaType.Movie
                        val tmdbRecommendations = TmdbApiClient().fetchRecommendations(resolvedTmdb, isMovie)
                        if (tmdbRecommendations.isNotEmpty()) return@withContext tmdbRecommendations
                    }
                    emptyList()
                }
                "tmdb" -> {
                    val effectiveTmdbId = tmdbId ?: externalId
                    if (effectiveTmdbId > 0) {
                        val isMovie = mediaType == MediaType.Movie
                        TmdbApiClient().fetchRecommendations(effectiveTmdbId, isMovie)
                    } else emptyList()
                }
                "jikan", "mal" -> fetchRecommendationsFromJikan(externalId, mediaType)
                "anilist"      -> fetchRecommendationsFromAniList(externalId, mediaType)
                else           -> emptyList()
            }
        }
    }

    private suspend fun fetchRecommendationsFromJikan(
        externalId: Int,
        mediaType: MediaType
    ): List<KitsugiRelation> {
        val endpoint = if (mediaType == MediaType.Anime) "anime" else "manga"
        val url = java.net.URL("https://api.jikan.moe/v4/$endpoint/$externalId/recommendations")
        return runCatching {
            KitsugiApiBase.runWithRateLimit {
                val response = KitsugiApiBase.executeGetRequest(url) ?: return@runWithRateLimit emptyList()
                val root = JSONObject(response)
                val data = root.optJSONArray("data") ?: return@runWithRateLimit emptyList()
                val list = mutableListOf<KitsugiRelation>()

                for (i in 0 until minOf(data.length(), 20)) {
                    val item = data.optJSONObject(i) ?: continue
                    val entry = item.optJSONObject("entry") ?: continue
                    val relMalId = entry.optInt("mal_id")
                    val title = entry.optNullableString("title") ?: "Bilinmeyen"
                    val imagesObj = entry.optJSONObject("images")
                    val imageUrl = imagesObj?.optJSONObject("jpg")?.optNullableString("image_url")
                        ?: imagesObj?.optJSONObject("webp")?.optNullableString("image_url")
                    
                    list.add(KitsugiRelation(
                        malId = relMalId,
                        title = title,
                        relationType = "Öneri",
                        imageUrl = imageUrl,
                        mediaType = mediaType,
                        source = "jikan"
                    ))
                }
                list
            }
        }.getOrElse { emptyList() }
    }

    private suspend fun fetchRecommendationsFromAniList(
        externalId: Int,
        mediaType: MediaType
    ): List<KitsugiRelation> {
        val aniListId = if (externalId >= 100_000_000) externalId - 100_000_000 else null
        val idParam  = if (aniListId != null) "\$id: Int" else "\$idMal: Int"
        val idFilter = if (aniListId != null) "id: \$id"  else "idMal: \$idMal"
        val query = """
            query (${'$'}type: MediaType, $idParam) {
                Media($idFilter, type: ${'$'}type) {
                    recommendations(page: 1, perPage: 20, sort: [RATING_DESC, ID]) {
                        edges {
                            node {
                                mediaRecommendation {
                                    id
                                    idMal
                                    title { romaji english native }
                                    type
                                    coverImage { large }
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        val variables = JSONObject().put("type", if (mediaType == MediaType.Anime) "ANIME" else "MANGA")
        if (aniListId != null) variables.put("id", aniListId) else variables.put("idMal", externalId)

        return runCatching {
            val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return emptyList()
            val root  = JSONObject(response)
            val edges = root.optJSONObject("data")
                ?.optJSONObject("Media")
                ?.optJSONObject("recommendations")
                ?.optJSONArray("edges") ?: return emptyList()
            val list = mutableListOf<KitsugiRelation>()
            for (i in 0 until edges.length()) {
                val edge = edges.optJSONObject(i) ?: continue
                val node = edge.optJSONObject("node") ?: continue
                val mediaRec = node.optJSONObject("mediaRecommendation") ?: continue
                val nodeId   = mediaRec.optInt("id")
                val nodeIdMal = mediaRec.optionalPositiveInt("idMal")
                val stableId = nodeIdMal ?: (100_000_000 + nodeId)
                val titleObj = mediaRec.optJSONObject("title")
                val titleRomaji = titleObj?.optNullableString("romaji")
                val titleEnglish = titleObj?.optNullableString("english")
                val titleNative = titleObj?.optNullableString("native")
                val title = titleRomaji
                    ?: titleEnglish
                    ?: titleNative ?: "Bilinmeyen"
                val nodeTypeStr = mediaRec.optNullableString("type").orEmpty()
                val nodeType = if (nodeTypeStr.equals("manga", ignoreCase = true)) MediaType.Manga else MediaType.Anime
                val imageUrl = mediaRec.optJSONObject("coverImage")?.optNullableString("large")
                list.add(KitsugiRelation(
                    malId = stableId,
                    title = title,
                    relationType = "Öneri",
                    imageUrl = imageUrl,
                    mediaType = nodeType,
                    source = "anilist",
                    titleEnglish = titleEnglish,
                    titleJapanese = titleNative,
                    titleRomaji = titleRomaji
                ))
            }
            list
        }.getOrElse { emptyList() }
    }
}
