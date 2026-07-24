package com.kitsugi.animelist.data.remote

import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kitsugi.animelist.utils.*

class KitsugiDetailClient {

    private suspend fun getTurkishMetadataFromTmdb(
        source: String,
        externalId: Int,
        mediaType: MediaType,
        providedTmdbId: Int? = null,
        providedRealMalId: Int? = null
    ): KitsugiMediaDetail? {
        val tmdbId = providedTmdbId ?: run {
            // Source'a göre doğru ID tipini belirle
            val malIdForResolve: Int? = when (source.lowercase()) {
                "simkl" -> providedRealMalId
                "jikan", "mal" -> externalId
                "anilist" -> {
                    // stableId < 100_000_000 → AniList arama sonucu MAL ID ile döndü
                    // Bu durumda MAL ID olarak çözümle, AniList ID olarak değil
                    if (externalId < 100_000_000) externalId else providedRealMalId
                }
                else -> null
            }
            val aniListIdForResolve: Int? = if (source.lowercase() == "anilist" && externalId >= 100_000_000) {
                // 100_000_000+ offset'li stableId → gerçek AniList ID'yi çıkar
                externalId - 100_000_000
            } else null
            KitsugiIdResolver.resolveIds(malId = malIdForResolve, aniListId = aniListIdForResolve, tmdbId = providedTmdbId, mediaType = mediaType).tmdbId
        }

        if (tmdbId != null && tmdbId > 0) {
            val isMovie = mediaType == MediaType.Movie
            val firstTry = TmdbApiClient().fetchMediaDetail(tmdbId, isMovie)
            if (firstTry != null) return firstTry

            // Anime veya TMDB kaynaklı içeriklerde film/dizi ayrımı yanlış yapılmış olabilir.
            // İlk deneme null dönerse, diğer formatta tekrar çekmeyi dene (TV -> Movie veya Movie -> TV).
            if (mediaType != MediaType.Manga) {
                return TmdbApiClient().fetchMediaDetail(tmdbId, !isMovie)
            }
        }
        return null
    }

    suspend fun fetchSynopsis(
        source: String,
        externalId: Int?,
        mediaType: MediaType
    ): String? {
        return withContext(Dispatchers.IO) {
            if (externalId == null || externalId <= 0) {
                return@withContext null
            }

            if (mediaType != MediaType.Manga) {
                val trMeta = getTurkishMetadataFromTmdb(source, externalId, mediaType)
                if (trMeta != null && !trMeta.synopsis.isNullOrBlank()) {
                    return@withContext trMeta.synopsis
                }
            }

            when (source.lowercase()) {
                "jikan", "mal" -> KitsugiMalDetailClient.fetchSynopsis(
                    malId = externalId,
                    mediaType = mediaType
                )

                "anilist" -> KitsugiAniListDetailClient.fetchSynopsis(
                    stableId = externalId,
                    mediaType = mediaType
                )

                "simkl" -> KitsugiSimklDetailClient.fetchSimklDetailDirect(externalId, mediaType)?.synopsis

                else -> null
            }
        }
    }

    suspend fun fetchDetail(
        source: String,
        externalId: Int?,
        mediaType: MediaType,
        tmdbId: Int? = null,
        realMalId: Int? = null,
        title: String? = null
    ): KitsugiMediaDetail? {
        return withContext(Dispatchers.IO) {
            if (externalId == null || externalId <= 0) return@withContext null

            val cacheKey = "${source.lowercase()}_$externalId"
            val context = com.kitsugi.animelist.KitsugiApplication.getInstance()?.applicationContext
            val db = context?.let { com.kitsugi.animelist.data.local.KitsugiDatabase.getDatabase(it) }
            val gson = com.google.gson.Gson()

            // 1. Fresh Cache Check (Room - 24 hours threshold)
            if (db != null) {
                try {
                    val cached = db.persistentDetailCacheDao().getDetail(cacheKey)
                    if (cached != null) {
                        val isFresh = (System.currentTimeMillis() - cached.cachedAtMs) < 24 * 60 * 60 * 1000L
                        if (isFresh) {
                            val detail = gson.fromJson(cached.detailJson, KitsugiMediaDetail::class.java)
                            if (detail != null) {
                                android.util.Log.d("KitsugiDetailClient", "Serving fresh detail from Room cache for $cacheKey")
                                return@withContext detail
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("KitsugiDetailClient", "Error reading detail cache: ${e.message}")
                }
            }

            // 2. Primary source fetch
            val detail = when (source.lowercase()) {
                "jikan", "mal" -> KitsugiMalDetailClient.fetchDetail(externalId, mediaType)
                "anilist" -> KitsugiAniListDetailClient.fetchDetail(externalId, mediaType)
                // TMDB discovery öğeleri: malId aslında tmdbId, doğrudan TMDB'den çek
                "tmdb" -> {
                    val effectiveTmdbId = tmdbId ?: externalId
                    if (effectiveTmdbId > 0) {
                        val isMovie = mediaType == MediaType.Movie
                        TmdbApiClient().fetchMediaDetail(effectiveTmdbId, isMovie)
                    } else null
                }
                "simkl" -> {
                    val resolvedTmdb = tmdbId ?: run {
                        val malIdForResolve = realMalId ?: DetailCache.getMediaDetail("simkl", externalId)?.realMalId
                        KitsugiIdResolver.resolveIds(malId = malIdForResolve, aniListId = null, tmdbId = tmdbId, mediaType = mediaType).tmdbId
                    }
                    // ── Öncelik zinciri (Simkl son çaredir) ────────────────────────────
                    // 1. Anime ise ve gerçek MAL ID varsa -> Jikan (rate-limit yok, önerilen)
                    if (mediaType == MediaType.Anime && realMalId != null && realMalId > 0) {
                        val malDetail = KitsugiMalDetailClient.fetchDetail(realMalId, mediaType)
                        if (malDetail != null) malDetail
                        else {
                            if (resolvedTmdb != null && resolvedTmdb > 0) {
                                val isMovie = mediaType == MediaType.Movie
                                val tmdbDetail = TmdbApiClient().fetchMediaDetail(resolvedTmdb, isMovie)
                                if (tmdbDetail != null) tmdbDetail
                                else KitsugiSimklDetailClient.fetchSimklDetailDirect(externalId, mediaType)
                            } else {
                                KitsugiSimklDetailClient.fetchSimklDetailDirect(externalId, mediaType)
                            }
                        }
                    } else if (resolvedTmdb != null && resolvedTmdb > 0) {
                        val isMovie = mediaType == MediaType.Movie
                        val tmdbDetail = TmdbApiClient().fetchMediaDetail(resolvedTmdb, isMovie)
                        if (tmdbDetail != null) tmdbDetail
                        else KitsugiSimklDetailClient.fetchSimklDetailDirect(externalId, mediaType)
                    } else {
                        KitsugiSimklDetailClient.fetchSimklDetailDirect(externalId, mediaType)
                    }
                }
                else -> null
            }

            var finalDetail = detail
            
            // 3. Fallback Client Chains (Live Backups)
            if (finalDetail == null) {
                if (mediaType == MediaType.Movie || mediaType == MediaType.TvShow) {
                    // TMDB Fallback: TVmaze for TV Shows
                    if (mediaType == MediaType.TvShow && !title.isNullOrBlank()) {
                        android.util.Log.d("KitsugiDetailClient", "TMDB returned null. Trying TVmaze fallback for TV show: $title")
                        finalDetail = TvMazeClient.fetchShowDetailByTitle(title)
                    }
                    // TMDB Fallback: Search fallback by title
                    if (finalDetail == null && !title.isNullOrBlank()) {
                        android.util.Log.d("KitsugiDetailClient", "Trying direct TMDB search fallback for: $title")
                        val searchResults = TmdbApiClient().search(title)
                        val matchedResult = searchResults.firstOrNull { it.type == mediaType }
                        if (matchedResult != null && matchedResult.tmdbId != null && matchedResult.tmdbId > 0) {
                            finalDetail = TmdbApiClient().fetchMediaDetail(matchedResult.tmdbId, mediaType == MediaType.Movie)
                        }
                    }
                } else if (mediaType == MediaType.Anime) {
                    // Anime Fallback: Kitsu
                    android.util.Log.d("KitsugiDetailClient", "AniList/MAL detail returned null. Trying Kitsu fallback.")
                    val kitsuId = if (db != null) {
                        val resolvedEntity = if (source.lowercase() == "anilist") {
                            db.mediaMetaCacheDao().getByAniListId(externalId)
                        } else {
                            db.mediaMetaCacheDao().getByMalId(externalId)
                        }
                        resolvedEntity?.kitsuId
                    } else null

                    if (!kitsuId.isNullOrBlank()) {
                        android.util.Log.d("KitsugiDetailClient", "Fetching Kitsu detail via resolved kitsuId: $kitsuId")
                        finalDetail = KitsuClient.fetchAnimeDetail(kitsuId)
                    }
                    if (finalDetail == null && !title.isNullOrBlank()) {
                        android.util.Log.d("KitsugiDetailClient", "Fetching Kitsu detail via title search: $title")
                        finalDetail = KitsuClient.fetchAnimeDetailByTitle(title)
                    }
                }
            }

            if (finalDetail != null && mediaType != MediaType.Manga) {
                // TMDB zenginleştirmesi için en iyi MAL ID'yi bul
                val effectiveRealMalId = realMalId
                    ?: finalDetail.realMalId
                    ?: if (source.lowercase() == "anilist" && externalId < 100_000_000) externalId else null
                
                var resolvedTmdbId = tmdbId ?: finalDetail.tmdbId
                
                // Fallback scenario 2: Primary detail is not null, but tmdbId is missing -> Try direct TMDB search fallback by title!
                if ((resolvedTmdbId == null || resolvedTmdbId <= 0) && (mediaType == MediaType.Movie || mediaType == MediaType.TvShow)) {
                    val searchTitle = title ?: finalDetail.title ?: finalDetail.titleEnglish
                    if (!searchTitle.isNullOrBlank()) {
                        android.util.Log.d("KitsugiDetailClient", "Primary resolution has no tmdbId. Triggering TMDB search for title: $searchTitle")
                        val searchResults = TmdbApiClient().search(searchTitle)
                        val matchedResult = searchResults.firstOrNull { it.type == mediaType }
                        if (matchedResult != null && matchedResult.tmdbId != null && matchedResult.tmdbId > 0) {
                            resolvedTmdbId = matchedResult.tmdbId
                            android.util.Log.d("KitsugiDetailClient", "Resolved tmdbId = $resolvedTmdbId via search for title: $searchTitle")
                        }
                    }
                }
                
                val trMeta = getTurkishMetadataFromTmdb(source, externalId, mediaType, resolvedTmdbId, effectiveRealMalId)
                if (trMeta != null) {
                    val updatedSynopsis = if (!trMeta.synopsis.isNullOrBlank()) trMeta.synopsis else finalDetail.synopsis
                    val updatedTitle = if (!trMeta.title.isNullOrBlank()) trMeta.title else finalDetail.title
                    val updatedTitleEnglish = if (!trMeta.titleEnglish.isNullOrBlank()) trMeta.titleEnglish else finalDetail.titleEnglish
                    val updatedGenres = if (trMeta.genres.isNotEmpty()) trMeta.genres else finalDetail.genres
                    val combinedPictures = (finalDetail.pictures.orEmpty() + trMeta.pictures.orEmpty()).distinct()
                    val mergedStudios = if (finalDetail.studios.isNotEmpty()) finalDetail.studios else trMeta.studios
                    val mergedProducers = if (finalDetail.producers.isNotEmpty()) finalDetail.producers else trMeta.producers
                    val mergedRating = if (!finalDetail.rating.isNullOrBlank()) finalDetail.rating else trMeta.rating
                    finalDetail = finalDetail.copy(
                        synopsis = updatedSynopsis,
                        title = updatedTitle,
                        titleEnglish = updatedTitleEnglish,
                        genres = updatedGenres,
                        tmdbId = resolvedTmdbId ?: finalDetail.tmdbId,
                        pictures = combinedPictures,
                        studios = mergedStudios,
                        producers = mergedProducers,
                        rating = mergedRating,
                        totalSeasons = if (source.lowercase() == "tmdb" || source.lowercase() == "simkl") {
                            trMeta.totalSeasons ?: finalDetail.totalSeasons
                        } else {
                            finalDetail.totalSeasons ?: 1
                        },
                        meanScore = finalDetail.meanScore ?: trMeta.meanScore,
                        averageScore = finalDetail.averageScore ?: trMeta.averageScore,
                        popularity = finalDetail.popularity ?: trMeta.popularity,
                        favorites = finalDetail.favorites ?: trMeta.favorites,
                        rank = finalDetail.rank ?: trMeta.rank,
                        popularityRank = finalDetail.popularityRank ?: trMeta.popularityRank,
                        scoredBy = finalDetail.scoredBy ?: trMeta.scoredBy,
                        members = finalDetail.members ?: trMeta.members,
                        nextAiringEpisode = finalDetail.nextAiringEpisode ?: trMeta.nextAiringEpisode
                    )
                }

                // Eğer nextAiringEpisode hâlâ null ise AniList üzerinden çöz ve çek
                if (finalDetail.nextAiringEpisode == null) {
                    val malIdForResolve = when (source.lowercase()) {
                        "simkl" -> realMalId ?: finalDetail.realMalId
                        "jikan", "mal" -> externalId
                        "anilist" -> if (externalId < 100_000_000) externalId else realMalId ?: finalDetail.realMalId
                        else -> null
                    }
                    val resolvedAniListId = runCatching {
                        KitsugiIdResolver.resolveIds(malId = malIdForResolve, aniListId = null, mediaType = mediaType).aniListId
                    }.getOrNull()
                    if (resolvedAniListId != null && resolvedAniListId > 0) {
                        val nextAiring = KitsugiAniListDetailClient.fetchNextAiringEpisodeOnly(resolvedAniListId)
                        if (nextAiring != null) {
                            finalDetail = finalDetail.copy(nextAiringEpisode = nextAiring)
                        }
                    }
                }
            }

            // 4. Stale Cache Fallback (If all network attempts returned null, check cache again even if expired)
            if (finalDetail == null && db != null) {
                try {
                    val cached = db.persistentDetailCacheDao().getDetail(cacheKey)
                    if (cached != null) {
                        finalDetail = gson.fromJson(cached.detailJson, KitsugiMediaDetail::class.java)
                        if (finalDetail != null) {
                            android.util.Log.d("KitsugiDetailClient", "Serving stale detail from Room cache for $cacheKey")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("KitsugiDetailClient", "Error reading stale cache: ${e.message}")
                }
            }

            // 5. Cache update on success
            if (finalDetail != null && db != null) {
                try {
                    val entity = com.kitsugi.animelist.data.local.PersistentDetailCacheEntity(
                        cacheKey = cacheKey,
                        detailJson = gson.toJson(finalDetail),
                        cachedAtMs = System.currentTimeMillis()
                    )
                    db.persistentDetailCacheDao().insertDetail(entity)
                } catch (e: Exception) {
                    android.util.Log.e("KitsugiDetailClient", "Error writing detail cache: ${e.message}")
                }
            }

            finalDetail
        }
    }
}
