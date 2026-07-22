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
            KitsugiIdResolver.resolveIds(malId = malIdForResolve, aniListId = aniListIdForResolve, tmdbId = providedTmdbId).tmdbId
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
                "jikan", "mal" -> KitsugiJikanDetailClient.fetchSynopsis(
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
        realMalId: Int? = null
    ): KitsugiMediaDetail? {
        return withContext(Dispatchers.IO) {
            if (externalId == null || externalId <= 0) return@withContext null

            val detail = when (source.lowercase()) {
                "jikan", "mal" -> KitsugiJikanDetailClient.fetchDetail(externalId, mediaType)
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
                        KitsugiIdResolver.resolveIds(malId = malIdForResolve, aniListId = null, tmdbId = tmdbId).tmdbId
                    }
                    // ── Öncelik zinciri (Simkl son çaredir) ────────────────────────────
                    // 1. Anime ise ve gerçek MAL ID varsa -> Jikan (rate-limit yok, önerilen)
                    if (mediaType == MediaType.Anime && realMalId != null && realMalId > 0) {
                        val malDetail = KitsugiJikanDetailClient.fetchDetail(realMalId, mediaType)
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
            if (finalDetail != null && mediaType != MediaType.Manga) {
                // TMDB zenginleştirmesi için en iyi MAL ID'yi bul
                // AniList source'da realMalId, detail.realMalId veya stableId < 100M ise stableId kendisi
                val effectiveRealMalId = realMalId
                    ?: finalDetail.realMalId
                    ?: if (source.lowercase() == "anilist" && externalId < 100_000_000) externalId else null
                val trMeta = getTurkishMetadataFromTmdb(source, externalId, mediaType, tmdbId ?: finalDetail.tmdbId, effectiveRealMalId)
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
                        tmdbId = tmdbId ?: finalDetail.tmdbId,
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
                        KitsugiIdResolver.resolveIds(malId = malIdForResolve, aniListId = null).aniListId
                    }.getOrNull()
                    if (resolvedAniListId != null && resolvedAniListId > 0) {
                        val nextAiring = KitsugiAniListDetailClient.fetchNextAiringEpisodeOnly(resolvedAniListId)
                        if (nextAiring != null) {
                            finalDetail = finalDetail.copy(nextAiringEpisode = nextAiring)
                        }
                    }
                }
            }
            finalDetail
        }
    }
}
