package com.kitsugi.animelist.data.remote

import android.content.Context
import android.util.Log
import com.kitsugi.animelist.data.local.MediaMetaCacheDao
import com.kitsugi.animelist.data.local.MediaMetaCacheEntity
import com.kitsugi.animelist.data.local.KitsugiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import kotlinx.coroutines.flow.first

/**
 * KitsugiEpisodeRatingsRepository
 *
 * Hem AniList hem MAL kaynaklı animeler için bölüm puanlarını çeker.
 *
 * AniList → tmdbId zaten externalLinks'ten gelir
 * MAL     → arm.haglund.dev ile mal_id → tmdb_id dönüşümü yapılır (ücretsiz, API key gerekmez)
 *
 * SeriesGraph API: /api/shows/{tmdbId}/season-ratings
 * Döndürülen tip: Map<Pair<seasonNumber, episodeNumber>, voteAverage>
 */
object KitsugiEpisodeRatingsRepository {

    private const val TAG = "KitsugiEpisodeRatings"
    private const val CACHE_TTL_MS = 30L * 60L * 1000L // 30 dakika



    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    // Room Veritabanı
    private var database: KitsugiDatabase? = null
    private val dao: MediaMetaCacheDao? get() = database?.mediaMetaCacheDao()
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        if (database == null) {
            database = KitsugiDatabase.getDatabase(context.applicationContext)
        }
    }

    private suspend fun isTmdbEnabled(): Boolean {
        val context = appContext ?: return true
        return try {
            com.kitsugi.animelist.data.settings.SettingsDataStore(context).settingsFlow.first().tmdbEnabled
        } catch (e: Exception) {
            true
        }
    }

    /** Fanart.tv ayarlarını döner (apiKey, enabled). Built-in API anahtarı her zaman fallback olarak kullanılır. */
    private suspend fun getFanartSettings(): Pair<Boolean, String> {
        val context = appContext ?: return Pair(true, FanartApiClient.getActiveApiKey())
        return try {
            val settings = com.kitsugi.animelist.data.settings.SettingsDataStore(context).settingsFlow.first()
            // Kullanıcı toggle'ı kapattıysa devre dışı; aksi hâlde built-in key ile devam et
            val enabled = settings.fanartTvEnabled
            val apiKey = FanartApiClient.getActiveApiKey(settings.fanartTvApiKey)
            Pair(enabled, apiKey)
        } catch (e: Exception) {
            Pair(true, FanartApiClient.getActiveApiKey())
        }
    }

    // ── All caches are consolidated in DetailCache (app-lifetime singleton) ──
    // References kept as local aliases for readability
    private val ratingsCache get() = DetailCache.episodeRatingsCache
    private val inFlight = mutableMapOf<Int, Deferred<Map<Pair<Int, Int>, Double>>>()

    // malId → tmdbId önbelleği (ARM API sonuçları)
    private val malToTmdbCache get() = DetailCache.malToTmdbCache

    // tmdbId → tvdbId önbelleği
    private val tmdbToTvdbCache get() = DetailCache.tmdbToTvdbCache

    // tmdbId → logo URL önbelleği
    private val logoCache get() = DetailCache.logoCache
    private val logoInFlight = mutableMapOf<Int, Deferred<String?>>()

    data class TmdbEpisodeDto(
        val episodeNumber: Int,
        val name: String?,
        val overview: String?,
        val stillPath: String?,
        val airDate: String?
    )

    private val tmdbEpisodesInFlight = mutableMapOf<Pair<Int, Int>, Deferred<List<TmdbEpisodeDto>>>()

    // Helper to convert between TmdbEpisodeDto and DetailCache.TmdbEpisodeDtoCached
    private fun TmdbEpisodeDto.toCached() = DetailCache.TmdbEpisodeDtoCached(
        episodeNumber = episodeNumber, name = name, overview = overview,
        stillPath = stillPath, airDate = airDate
    )
    private fun DetailCache.TmdbEpisodeDtoCached.toDto() = TmdbEpisodeDto(
        episodeNumber = episodeNumber, name = name, overview = overview,
        stillPath = stillPath, airDate = airDate
    )


    /**
     * AniList kaynağı için — tmdbId doğrudan biliniyorsa kullan
     */
    suspend fun getEpisodeRatings(tmdbId: Int): Map<Pair<Int, Int>, Double> = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext emptyMap()
        if (tmdbId <= 0) return@withContext emptyMap()
        fetchWithCache(tmdbId)
    }

    /**
     * MAL kaynağı için — malId'yi önce TMDB ID'ye çevir, sonra puan çek
     * ARM API: https://arm.haglund.dev/api/v2/ids?source=myanimelist&id={malId}
     */
    suspend fun getEpisodeRatingsByMalId(malId: Int): Map<Pair<Int, Int>, Double> = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext emptyMap()
        if (malId <= 0) return@withContext emptyMap()

        // MAL → TMDB ID çevrim (önbellekli)
        val tmdbId = resolveTmdbIdFromMal(malId) ?: return@withContext emptyMap()
        fetchWithCache(tmdbId)
    }

    /**
     * AniList kaynağı için — aniListId üzerinden ARM API'si ile TMDB ID bul, sonra puan çek.
     * TMDB ID zaten biliniyorsa (externalLinks'ten çıkarıldıysa) bunu kullanmak daha verimlidir.
     * ARM API: https://arm.haglund.dev/api/v2/ids?source=anilist&id={aniListId}
     */
    suspend fun resolveTmdbIdFromAniList(aniListId: Int): Int? = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext null
        if (aniListId <= 0) return@withContext null
        val cacheKey = -aniListId

        // Önbellek kontrolü — tamamen mutex içinde
        val isCached = mutex.withLock { malToTmdbCache.containsKey(cacheKey) }
        if (isCached) return@withContext mutex.withLock { malToTmdbCache[cacheKey] }

        // Room önbellek kontrolü
        val cachedEntity = runCatching { dao?.getByAniListId(aniListId) }.getOrNull()
        if (cachedEntity != null) {
            val id = cachedEntity.tmdbId
            mutex.withLock { malToTmdbCache[cacheKey] = id }
            if (cachedEntity.tvdbId != null && cachedEntity.tvdbId > 0) {
                mutex.withLock { tmdbToTvdbCache[id] = cachedEntity.tvdbId }
            }
            Log.d(TAG, "Room hit: aniListId=$aniListId → tmdbId=$id")
            return@withContext id
        }

        var tmdbId = runCatching {
            val url = URL("https://arm.haglund.dev/api/v2/ids?source=anilist&id=$aniListId")
            val response = KitsugiApiBase.executeGetRequest(url) ?: return@runCatching null
            val json = JSONObject(response)
            val tvdbVal = json.optInt("thetvdb", -1).takeIf { it > 0 }
            if (tvdbVal != null) {
                val tmdbVal = json.optInt("themoviedb", -1)
                if (tmdbVal > 0) {
                    mutex.withLock { tmdbToTvdbCache[tmdbVal] = tvdbVal }
                }
            }
            if (json.isNull("themoviedb")) {
                if (!json.isNull("myanimelist")) {
                    val malId = json.optInt("myanimelist", -1)
                    if (malId > 0) {
                        val resolved = resolveTmdbIdFromMal(malId)
                        if (resolved != null) {
                            val existing = dao?.getByTmdbId(resolved)
                            val updated = existing?.copy(aniListId = aniListId, malId = malId, tvdbId = tvdbVal ?: existing.tvdbId) ?: MediaMetaCacheEntity(
                                tmdbId = resolved,
                                malId = malId,
                                aniListId = aniListId,
                                logoUrl = null,
                                logoNotFound = false,
                                tvdbId = tvdbVal
                            )
                            dao?.insert(updated)
                            Log.d(TAG, "Room write (both MAL & AniList): tmdbId=$resolved")
                        }
                        resolved
                    } else null
                } else null
            } else {
                val value = json.optInt("themoviedb", -1)
                if (value > 0) {
                    val malId = if (!json.isNull("myanimelist")) json.optInt("myanimelist", -1) else null
                    val cleanMalId = if (malId != null && malId > 0) malId else null
                    val existing = dao?.getByTmdbId(value)
                    val updated = existing?.copy(aniListId = aniListId, malId = cleanMalId ?: existing.malId, tvdbId = tvdbVal ?: existing.tvdbId) ?: MediaMetaCacheEntity(
                        tmdbId = value,
                        malId = cleanMalId,
                        aniListId = aniListId,
                        logoUrl = null,
                        logoNotFound = false,
                        tvdbId = tvdbVal
                    )
                    dao?.insert(updated)
                    Log.d(TAG, "Room write (AniList): aniListId=$aniListId → tmdbId=$value")
                    value
                } else null
            }
        }.getOrElse {
            Log.w(TAG, "ARM API lookup failed for aniListId=$aniListId: ${it.message}")
            null
        }

        // Fallback to animeapi.my.id if ARM failed or returned null
        if (tmdbId == null) {
            tmdbId = runCatching {
                val url = URL("https://animeapi.my.id/anilist/$aniListId")
                val response = KitsugiApiBase.executeGetRequest(url) ?: return@runCatching null
                val json = JSONObject(response)
                val value = json.optInt("themoviedb", -1)
                val tvdbVal = json.optInt("thetvdb", -1).takeIf { it > 0 }
                if (value > 0) {
                    if (tvdbVal != null) {
                        mutex.withLock { tmdbToTvdbCache[value] = tvdbVal }
                    }
                    val malId = if (json.isNull("myanimelist")) null else json.optInt("myanimelist", -1).takeIf { it > 0 }
                    val existing = dao?.getByTmdbId(value)
                    val updated = existing?.copy(aniListId = aniListId, malId = malId ?: existing.malId, tvdbId = tvdbVal ?: existing.tvdbId) ?: MediaMetaCacheEntity(
                        tmdbId = value,
                        malId = malId,
                        aniListId = aniListId,
                        logoUrl = null,
                        logoNotFound = false,
                        tvdbId = tvdbVal
                    )
                    dao?.insert(updated)
                    Log.d(TAG, "Room write (AnimeAPI AniList): aniListId=$aniListId → tmdbId=$value")
                    value
                } else null
            }.getOrElse {
                Log.w(TAG, "AnimeAPI lookup failed for aniListId=$aniListId: ${it.message}")
                null
            }
        }

        mutex.withLock { malToTmdbCache[cacheKey] = tmdbId }
        Log.d(TAG, "ARM lookup (with fallback): aniListId=$aniListId → tmdbId=$tmdbId")
        tmdbId
    }

    suspend fun getEpisodeRatingsByAniListId(aniListId: Int): Map<Pair<Int, Int>, Double> = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext emptyMap()
        if (aniListId <= 0) return@withContext emptyMap()
        val tmdbId = resolveTmdbIdFromAniList(aniListId) ?: return@withContext emptyMap()
        fetchWithCache(tmdbId)
    }

    /** Tüm önbellekleri temizler — DetailCache.clear() ile yapılır */
    fun clearCache() {
        DetailCache.clear()
        inFlight.clear()
        logoInFlight.clear()
        tmdbEpisodesInFlight.clear()
    }

    /**
     * TMDB ID'ye göre anime logo URL'ini döner.
     * SeriesGraph /api/shows/{tmdbId} endpoint'inden logo_path çeker.
     * TMDB image CDN (image.tmdb.org) API key gerektirmez.
     */
    suspend fun getLogoUrl(tmdbId: Int): String? = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext null
        if (tmdbId <= 0) return@withContext null

        // Önbellekte varsa direkt dön — SADECE gerçek null (not found) için kabul et
        val isCached = mutex.withLock { logoCache.containsKey(tmdbId) }
        if (isCached) return@withContext mutex.withLock { logoCache[tmdbId] }

        // Room önbellek kontrolü
        val cachedEntity = runCatching { dao?.getByTmdbId(tmdbId) }.getOrNull()
        if (cachedEntity != null) {
            if (cachedEntity.logoNotFound) {
                mutex.withLock { logoCache[tmdbId] = null }
                Log.d(TAG, "Room logo hit (not found): tmdbId=$tmdbId")
                return@withContext null
            }
            if (!cachedEntity.logoUrl.isNullOrBlank()) {
                mutex.withLock { logoCache[tmdbId] = cachedEntity.logoUrl }
                Log.d(TAG, "Room logo hit (found): tmdbId=$tmdbId → ${cachedEntity.logoUrl}")
                return@withContext cachedEntity.logoUrl
            }
        }

        // In-flight dedup
        val deferred = mutex.withLock {
            logoInFlight[tmdbId] ?: scope.async {
                try {
                    // 1. Fanart.tv (en yüksek kalite) — TVDB ID gerektirir
                    val (fanartEnabled, fanartApiKey) = getFanartSettings()
                    val fanartLogoUrl = if (fanartEnabled) {
                        val tvdbId = resolveTvdbIdFromTmdb(tmdbId)
                        if (tvdbId != null && tvdbId > 0) {
                            runCatching {
                                FanartApiClient.fetchBestLogo(tvdbId, fanartApiKey)
                            }.getOrNull()
                        } else null
                    } else null

                    // 2. SeriesGraph fallback
                    val seriesGraphUrl = if (fanartLogoUrl == null) fetchLogoFromSeriesGraph(tmdbId) else null

                    // 3. TMDB direct fallback
                    val finalUrl = fanartLogoUrl
                        ?: seriesGraphUrl
                        ?: fetchLogoFromTmdbDirect(tmdbId)
                    
                    // Sadece gerçek sonucu önbelleğe yaz (null = bulunamadı anlamına gelir)
                    mutex.withLock { logoCache[tmdbId] = finalUrl }
                    
                    // Room önbelleğe kaydet
                    runCatching {
                        val existing = dao?.getByTmdbId(tmdbId)
                        val updated = existing?.copy(
                            logoUrl = finalUrl,
                            logoNotFound = finalUrl == null
                        ) ?: MediaMetaCacheEntity(
                            tmdbId = tmdbId,
                            malId = null,
                            aniListId = null,
                            logoUrl = finalUrl,
                            logoNotFound = finalUrl == null
                        )
                        dao?.insert(updated)
                        Log.d(TAG, "Room logo write: tmdbId=$tmdbId → logoUrl=$finalUrl (notFound=${finalUrl == null})")
                    }
                    
                    finalUrl
                } catch (e: Exception) {
                    Log.w(TAG, "Logo fetch failed for tmdbId=$tmdbId: ${e.message}")
                    // Hata durumunda null yazma — bir sonraki açılışta tekrar denensin
                    null
                } finally {
                    mutex.withLock { logoInFlight.remove(tmdbId) }
                }
            }.also { logoInFlight[tmdbId] = it }
        }
        deferred.await()
    }

    /**
     * MAL ID'ye göre anime logo URL'ini döner.
     * AniList kaynaklı animeler için fallbackAniListId verilebilir.
     */
    suspend fun getLogoUrlByMalId(malId: Int, fallbackAniListId: Int? = null): String? = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext null
        if (malId <= 0 && fallbackAniListId == null) return@withContext null
        var tmdbId: Int? = null
        if (malId > 0) tmdbId = resolveTmdbIdFromMal(malId)
        if (tmdbId == null && fallbackAniListId != null && fallbackAniListId > 0) {
            tmdbId = resolveTmdbIdFromAniList(fallbackAniListId)
        }
        val resolvedId = tmdbId ?: return@withContext null
        getLogoUrl(resolvedId)
    }

    /**
     * AniList ID'ye göre anime logo URL'ini döner.
     * ARM lookup başarısız olursa fallbackMalId ile tekrar dener.
     */
    suspend fun getLogoUrlByAniListId(aniListId: Int, fallbackMalId: Int? = null): String? = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext null
        var tmdbId: Int? = null
        if (aniListId > 0) tmdbId = resolveTmdbIdFromAniList(aniListId)
        // AniList ARM lookup boş döndüyse gerçek MAL ID ile tekrar dene
        if (tmdbId == null && fallbackMalId != null && fallbackMalId > 0) {
            Log.d(TAG, "AniList ARM miss for aniListId=$aniListId — retrying with malId=$fallbackMalId")
            tmdbId = resolveTmdbIdFromMal(fallbackMalId)
        }
        val resolvedId = tmdbId ?: return@withContext null
        getLogoUrl(resolvedId)
    }

    private fun fetchLogoFromSeriesGraph(tmdbId: Int): String? {
        val url = runCatching {
            URL("https://seriesgraph.com/api/shows/$tmdbId")
        }.getOrNull() ?: return null

        val responseText = KitsugiApiBase.executeGetRequest(url) ?: return null
        return runCatching {
            val json = JSONObject(responseText)
            val logoPath = json.optNullableString("logo_path") ?: return@runCatching null
            "https://image.tmdb.org/t/p/w500$logoPath"
        }.getOrElse {
            Log.w(TAG, "Logo parse failed for tmdbId=$tmdbId: ${it.message}")
            null
        }
    }

    private fun fetchLogoFromTmdbDirect(tmdbId: Int): String? {
        val apiKey = TmdbApiClient.getActiveApiKey()

        // Try TV show first
        var logoPath = queryTmdbImages(tmdbId, "tv", apiKey)
        // If not found, try Movie
        if (logoPath == null) {
            logoPath = queryTmdbImages(tmdbId, "movie", apiKey)
        }

        return logoPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    }

    private fun queryTmdbImages(tmdbId: Int, type: String, apiKey: String): String? {
        val url = runCatching {
            URL("https://api.themoviedb.org/3/$type/$tmdbId/images?api_key=$apiKey")
        }.getOrNull() ?: return null

        val responseText = KitsugiApiBase.executeGetRequest(url) ?: return null
        return runCatching {
            val json = JSONObject(responseText)
            val logos = json.optJSONArray("logos")
            if (logos == null || logos.length() == 0) return null

            // Prefer English logos first, then any logo
            var bestLogoPath: String? = null
            for (i in 0 until logos.length()) {
                val logoObj = logos.getJSONObject(i)
                val lang = logoObj.optNullableString("iso_639_1").orEmpty()
                val path = logoObj.optNullableString("file_path")
                if (!path.isNullOrBlank()) {
                    if (lang == "en") {
                        bestLogoPath = path
                        break
                    }
                    if (bestLogoPath == null) {
                        bestLogoPath = path
                    }
                }
            }
            bestLogoPath
        }.getOrNull()
    }

    /**
     * Verilen TMDB ID'nin TVDB ID'sini çözer.
     *
     * Çözümleme zinciri:
     *  1. Bellek önbelleği (tmdbToTvdbCache)
     *  2. Room önbelleğindeki tvdbId sütunu (önceki ARM/AnimeAPI sorgularından doldurulmuş)
     *  3. TMDB external_ids endpoint'i (doğrudan tvdb_id alanı)
     *  4. Room'daki malId üzerinden animeapi.my.id
     *  5. Room'daki aniListId üzerinden animeapi.my.id
     */
    private suspend fun resolveTvdbIdFromTmdb(
        tmdbId: Int,
        fallbackMalId: Int? = null,
        fallbackAniListId: Int? = null,
        fallbackKitsuId: Int? = null
    ): Int? = withContext(Dispatchers.IO) {
        // 1. Bellek önbelleği kontrolü
        if (tmdbToTvdbCache.containsKey(tmdbId)) {
            return@withContext mutex.withLock { tmdbToTvdbCache[tmdbId] }
        }

        // 2. Room'daki tvdbId sütununu doğrudan oku (ARM/AnimeAPI önceden kaydetmiş olabilir)
        val cachedEntity = runCatching { dao?.getByTmdbId(tmdbId) }.getOrNull()
        if (cachedEntity?.tvdbId != null && cachedEntity.tvdbId > 0) {
            val cached = cachedEntity.tvdbId
            mutex.withLock { tmdbToTvdbCache[tmdbId] = cached }
            Log.d(TAG, "Room tvdbId hit: tmdbId=$tmdbId → tvdbId=$cached")
            return@withContext cached
        }

        // 3. TMDB external_ids endpoint → tvdb_id
        val isTv = true // Fanart.tv TV endpoint için; movie de denenebilir
        val extIds = runCatching { KitsugiIdResolver.fetchExternalIds(tmdbId, isTv) }.getOrNull()
        val tvdbFromTmdb = extIds?.tvdbId?.takeIf { it > 0 }
            ?: runCatching { KitsugiIdResolver.fetchExternalIds(tmdbId, false) }.getOrNull()?.tvdbId?.takeIf { it > 0 }
        if (tvdbFromTmdb != null) {
            mutex.withLock { tmdbToTvdbCache[tmdbId] = tvdbFromTmdb }
            // Kalıcı olarak Room'a yaz
            runCatching {
                val existing = dao?.getByTmdbId(tmdbId)
                if (existing != null) dao?.insert(existing.copy(tvdbId = tvdbFromTmdb))
            }
            Log.d(TAG, "TVDB resolve (TMDB ext_ids): tmdbId=$tmdbId → tvdbId=$tvdbFromTmdb")
            return@withContext tvdbFromTmdb
        }

        // 4. malId üzerinden TVDB ID çöz (Room'dan veya caller fallback)
        var tvdbId: Int? = null
        val malId = cachedEntity?.malId ?: fallbackMalId
        if (malId != null && malId > 0) {
            tvdbId = resolveTvdbIdFromMal(malId)
        }

        // 5. aniListId üzerinden dene (Room'dan veya caller fallback)
        if (tvdbId == null) {
            val aniListId = cachedEntity?.aniListId ?: fallbackAniListId
            if (aniListId != null && aniListId > 0) {
                tvdbId = resolveTvdbIdFromAniList(aniListId)
            }
        }

        // 6. kitsuId üzerinden dene (Room'dan veya caller fallback)
        if (tvdbId == null) {
            val kitsuId = cachedEntity?.kitsuId?.toIntOrNull() ?: fallbackKitsuId
            if (kitsuId != null && kitsuId > 0) {
                tvdbId = resolveTvdbIdFromKitsu(kitsuId)
            }
        }

        mutex.withLock { tmdbToTvdbCache[tmdbId] = tvdbId }
        Log.d(TAG, "TVDB resolve (via MAL/AniList/Kitsu): tmdbId=$tmdbId → tvdbId=$tvdbId")
        tvdbId
    }

    /**
     * MAL ID üzerinden TVDB ID çözer (animeapi.my.id).
     */
    private suspend fun resolveTvdbIdFromMal(malId: Int): Int? {
        if (malId <= 0) return null
        return runCatching {
            val url = URL("https://animeapi.my.id/myanimelist/$malId")
            val response = KitsugiApiBase.executeGetRequest(url) ?: return@runCatching null
            val json = JSONObject(response)
            val value = json.optInt("thetvdb", -1)
            if (value > 0) {
                Log.d(TAG, "TVDB via AnimeAPI (MAL): malId=$malId → tvdbId=$value")
                value
            } else null
        }.getOrElse {
            Log.w(TAG, "resolveTvdbIdFromMal failed for malId=$malId: ${it.message}")
            null
        }
    }

    /**
     * AniList ID üzerinden TVDB ID çözer (animeapi.my.id).
     */
    private suspend fun resolveTvdbIdFromAniList(aniListId: Int): Int? {
        if (aniListId <= 0) return null
        return runCatching {
            val url = URL("https://animeapi.my.id/anilist/$aniListId")
            val response = KitsugiApiBase.executeGetRequest(url) ?: return@runCatching null
            val json = JSONObject(response)
            val value = json.optInt("thetvdb", -1)
            if (value > 0) {
                Log.d(TAG, "TVDB via AnimeAPI (AniList): aniListId=$aniListId → tvdbId=$value")
                value
            } else null
        }.getOrElse {
            Log.w(TAG, "resolveTvdbIdFromAniList failed for aniListId=$aniListId: ${it.message}")
            null
        }
    }

    /**
     * Fanart.tv'den zengin galeri öğeleri (logo, backdrop, poster, vb.) çeker.
     *
     * TV/Anime için: TMDB ID → Room cache → MAL/AniList ID → TVDB ID → Fanart.tv TV endpoint
     * Film için:    TMDB ID → Fanart.tv Film endpoint (doğrudan)
     *
     * @param tmdbId      TMDB ID (TV veya Film)
     * @param isMovie     true ise Film endpoint'i kullanılır; false ise TV
     * @param fallbackMalId    Opsiyonel MAL ID (tmdb→tvdb çevriminde yedek olarak kullanılır)
     * @param fallbackAniListId Opsiyonel AniList ID (tmdb→tvdb çevriminde yedek olarak kullanılır)
     * @return            Fanart.tv API'sinden elde edilen [GalleryItem] listesi.
     */
    suspend fun getFanartGalleryItems(
        tmdbId: Int,
        isMovie: Boolean = false,
        fallbackMalId: Int? = null,
        fallbackAniListId: Int? = null,
        fallbackKitsuId: Int? = null
    ): List<GalleryItem> = withContext(Dispatchers.IO) {
        val (fanartEnabled, fanartApiKey) = getFanartSettings()
        if (!fanartEnabled || fanartApiKey.isBlank()) return@withContext emptyList()

        // Bellek önbelleği: aynı sayfa tekrar açılırsa anında döner
        val cacheId = if (isMovie) tmdbId else tmdbId
        val cached = DetailCache.getFanartGallery(isMovie, cacheId)
        if (cached != null) {
            Log.d(TAG, "Fanart memory cache hit: isMovie=$isMovie id=$cacheId → ${cached.size} items")
            return@withContext cached
        }

        val result = if (isMovie) {
            if (tmdbId <= 0) return@withContext emptyList()
            // Film: TMDB ID doğrudan kullanılır
            runCatching {
                FanartApiClient.fetchMovieImages(tmdbId, fanartApiKey)
            }.getOrElse {
                Log.w(TAG, "getFanartGalleryItems (movie) failed: ${it.message}")
                emptyList()
            }
        } else {
            // TV/Anime: TVDB ID çözümleme zinciri (fallback ID'leri ileterek)
            var tvdbId = if (tmdbId > 0) resolveTvdbIdFromTmdb(tmdbId, fallbackMalId, fallbackAniListId, fallbackKitsuId) else null

            // Hâlâ bulunamazsa caller'dan gelen fallback id'leri doğrudan dene
            if (tvdbId == null || tvdbId <= 0) {
                if (fallbackMalId != null && fallbackMalId > 0) {
                    tvdbId = resolveTvdbIdFromMal(fallbackMalId)
                }
            }
            if (tvdbId == null || tvdbId <= 0) {
                if (fallbackAniListId != null && fallbackAniListId > 0) {
                    tvdbId = resolveTvdbIdFromAniList(fallbackAniListId)
                }
            }
            if (tvdbId == null || tvdbId <= 0) {
                if (fallbackKitsuId != null && fallbackKitsuId > 0) {
                    tvdbId = resolveTvdbIdFromKitsu(fallbackKitsuId)
                }
            }

            if (tvdbId == null || tvdbId <= 0) {
                Log.d(TAG, "No TVDB ID for tmdbId=$tmdbId — Fanart.tv TV skipped")
                return@withContext emptyList()
            }
            runCatching {
                FanartApiClient.fetchTvImages(tvdbId, fanartApiKey)
            }.getOrElse {
                Log.w(TAG, "getFanartGalleryItems (tv) failed: ${it.message}")
                emptyList()
            }
        }

        if (result.isNotEmpty()) {
            DetailCache.putFanartGallery(isMovie, cacheId, result)
            Log.d(TAG, "Fanart memory cache write: isMovie=$isMovie id=$cacheId → ${result.size} items")
        }
        result
    }

    /**
     * MAL ID için önbelleğe alınmış TMDB ID'yi döner.
     */
    suspend fun getResolvedTmdbIdForMal(malId: Int): Int? = mutex.withLock {
        malToTmdbCache[malId]
    }

    /**
     * AniList ID için önbelleğe alınmış TMDB ID'yi döner.
     */
    suspend fun getResolvedTmdbIdForAniList(aniListId: Int): Int? = mutex.withLock {
        malToTmdbCache[-aniListId]
    }

    // ─────────────────────────────────────────────────────────────
    // TMDB ID'ye göre önbellekli SeriesGraph fetch
    // ─────────────────────────────────────────────────────────────
    private suspend fun fetchWithCache(tmdbId: Int): Map<Pair<Int, Int>, Double> {
        val now = System.currentTimeMillis()

        // Önbellek kontrolü — withLock içinde return kullanmıyoruz
        val cached = mutex.withLock {
            val entry = ratingsCache[tmdbId]
            if (entry != null && entry.expiresAtMs > now) entry.ratings
            else {
                if (entry != null) ratingsCache.remove(tmdbId)
                null
            }
        }
        if (cached != null) return cached

        // In-flight dedup: aynı ID için çok sayıda istek gelirse hepsi aynı coroutine'i bekler
        val deferred = mutex.withLock {
            inFlight[tmdbId] ?: scope.async {
                try {
                    fetchFromSeriesGraph(tmdbId).also { ratings ->
                        mutex.withLock {
                            ratingsCache[tmdbId] = DetailCache.RatingCacheEntry(
                                ratings = ratings,
                                expiresAtMs = System.currentTimeMillis() + CACHE_TTL_MS
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "SeriesGraph fetch failed for tmdbId=$tmdbId: ${e.message}")
                    emptyMap()
                } finally {
                    mutex.withLock { inFlight.remove(tmdbId) }
                }
            }.also { inFlight[tmdbId] = it }
        }

        return deferred.await()
    }

    // ─────────────────────────────────────────────────────────────
    // ARM API: MAL ID → TMDB ID
    // Endpoint: https://arm.haglund.dev/api/v2/ids?source=myanimelist&id={malId}
    // Response: { "myanimelist": 21, "anilist": 21, "thetvdb": 81797,
    //             "themoviedb": 37854, "anidb": 69 }
    // ─────────────────────────────────────────────────────────────
    suspend fun resolveTmdbIdFromMal(malId: Int): Int? = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext null
        // Önbellek kontrolü — tamamen mutex içinde
        val isCached = mutex.withLock { malToTmdbCache.containsKey(malId) }
        if (isCached) return@withContext mutex.withLock { malToTmdbCache[malId] }

        // Room önbellek kontrolü
        val cachedEntity = runCatching { dao?.getByMalId(malId) }.getOrNull()
        if (cachedEntity != null) {
            val id = cachedEntity.tmdbId
            mutex.withLock { malToTmdbCache[malId] = id }
            if (cachedEntity.tvdbId != null && cachedEntity.tvdbId > 0) {
                mutex.withLock { tmdbToTvdbCache[id] = cachedEntity.tvdbId }
            }
            Log.d(TAG, "Room hit: malId=$malId → tmdbId=$id")
            return@withContext id
        }

        var tmdbId = runCatching {
            val url = URL("https://arm.haglund.dev/api/v2/ids?source=myanimelist&id=$malId")
            val response = KitsugiApiBase.executeGetRequest(url) ?: return@runCatching null
            val json = JSONObject(response)
            val tvdbVal = json.optInt("thetvdb", -1).takeIf { it > 0 }
            if (tvdbVal != null) {
                val tmdbVal = json.optInt("themoviedb", -1)
                if (tmdbVal > 0) {
                    mutex.withLock { tmdbToTvdbCache[tmdbVal] = tvdbVal }
                }
            }
            if (json.isNull("themoviedb")) null
            else {
                val value = json.optInt("themoviedb", -1)
                if (value > 0) {
                    val existing = dao?.getByTmdbId(value)
                    val updated = existing?.copy(malId = malId, tvdbId = tvdbVal ?: existing.tvdbId) ?: MediaMetaCacheEntity(
                        tmdbId = value,
                        malId = malId,
                        aniListId = null,
                        logoUrl = null,
                        logoNotFound = false,
                        tvdbId = tvdbVal
                    )
                    dao?.insert(updated)
                    Log.d(TAG, "Room write (MAL): malId=$malId → tmdbId=$value")
                    value
                } else null
            }
        }.getOrElse {
            Log.w(TAG, "ARM API lookup failed for malId=$malId: ${it.message}")
            null
        }

        // Fallback to animeapi.my.id if ARM failed or returned null
        if (tmdbId == null) {
            tmdbId = runCatching {
                val url = URL("https://animeapi.my.id/myanimelist/$malId")
                val response = KitsugiApiBase.executeGetRequest(url) ?: return@runCatching null
                val json = JSONObject(response)
                val value = json.optInt("themoviedb", -1)
                val tvdbVal = json.optInt("thetvdb", -1).takeIf { it > 0 }
                if (value > 0) {
                    if (tvdbVal != null) {
                        mutex.withLock { tmdbToTvdbCache[value] = tvdbVal }
                    }
                    val aniListId = if (json.isNull("anilist")) null else json.optInt("anilist", -1).takeIf { it > 0 }
                    val existing = dao?.getByTmdbId(value)
                    val updated = existing?.copy(malId = malId, aniListId = aniListId ?: existing.aniListId, tvdbId = tvdbVal ?: existing.tvdbId) ?: MediaMetaCacheEntity(
                        tmdbId = value,
                        malId = malId,
                        aniListId = aniListId,
                        logoUrl = null,
                        logoNotFound = false,
                        tvdbId = tvdbVal
                    )
                    dao?.insert(updated)
                    Log.d(TAG, "Room write (AnimeAPI MAL): malId=$malId → tmdbId=$value")
                    value
                } else null
            }.getOrElse {
                Log.w(TAG, "AnimeAPI lookup failed for malId=$malId: ${it.message}")
                null
            }
        }

        mutex.withLock { malToTmdbCache[malId] = tmdbId }
        Log.d(TAG, "ARM lookup (with fallback): malId=$malId → tmdbId=$tmdbId")
        tmdbId
    }

    // ─────────────────────────────────────────────────────────────
    // SeriesGraph API
    // Endpoint: https://seriesgraph.com/api/shows/{tmdbId}/season-ratings
    // ─────────────────────────────────────────────────────────────
    private fun fetchFromSeriesGraph(tmdbId: Int): Map<Pair<Int, Int>, Double> {
        val url = runCatching {
            URL("https://seriesgraph.com/api/shows/$tmdbId/season-ratings")
        }.getOrNull() ?: return emptyMap()

        val responseText = KitsugiApiBase.executeGetRequest(url) ?: return emptyMap()
        return parseSeriesGraphResponse(responseText)
    }

    private fun parseSeriesGraphResponse(json: String): Map<Pair<Int, Int>, Double> {
        return runCatching {
            val root = JSONArray(json)
            val result = mutableMapOf<Pair<Int, Int>, Double>()

            for (s in 0 until root.length()) {
                val seasonObj = root.optJSONObject(s) ?: continue
                val episodes = seasonObj.optJSONArray("episodes") ?: continue

                for (e in 0 until episodes.length()) {
                    val ep = episodes.optJSONObject(e) ?: continue
                    val seasonNum = ep.optInt("season_number", -1).takeIf { it >= 0 } ?: continue
                    val episodeNum = ep.optInt("episode_number", -1).takeIf { it > 0 } ?: continue
                    val voteAvg = ep.optDouble("vote_average", Double.NaN)
                        .takeIf { !it.isNaN() && it > 0.0 } ?: continue
                    result[seasonNum to episodeNum] = voteAvg
                }
            }
            result
        }.getOrElse {
            Log.w(TAG, "SeriesGraph parse failed: ${it.message}")
            emptyMap()
        }
    }

    /**
     * Verilen TMDB sezon numarası ve başlık/sinonimler yardımıyla hedef sezon numarasını tahmin eder.
     */
    fun determineTargetSeason(
        tmdbSeason: Int?,
        title: String?,
        titleEnglish: String?,
        synonyms: List<String>
    ): Int {
        if (tmdbSeason != null && tmdbSeason > 0) return tmdbSeason

        val seasonPatterns = listOf(
            Regex("""season\s+(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""s\s*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""sezon\s+(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)(?:st|nd|rd|th)\s+season""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\.\s+sezon""", RegexOption.IGNORE_CASE),
            Regex("""\b(\d+)\b(?:\s*\(.*?\))?\s*$""")
        )

        val romanNumerals = mapOf(
            " ii" to 2, " iii" to 3, " iv" to 4, " v" to 5, " vi" to 6, " vii" to 7, " viii" to 8, " ix" to 9, " x" to 10,
            " ii " to 2, " iii " to 3, " iv " to 4, " v " to 5, " vi " to 6, " vii " to 7, " viii " to 8, " ix " to 9, " x " to 10
        )

        fun parseFromText(text: String?): Int? {
            if (text.isNullOrBlank()) return null
            val lower = text.lowercase()

            for (pattern in seasonPatterns) {
                val match = pattern.find(lower)
                if (match != null) {
                    val num = match.groupValues[1].toIntOrNull()
                    if (num != null && num > 0) return num
                }
            }

            for ((roman, num) in romanNumerals) {
                if (lower.endsWith(roman) || lower.contains(roman)) {
                    return num
                }
            }

            return null
        }

        parseFromText(title)?.let { return it }
        parseFromText(titleEnglish)?.let { return it }
        for (syn in synonyms) {
            parseFromText(syn)?.let { return it }
        }

        return 1
    }

    /**
     * TMDB ID ve sezon numarasına göre bölüm listesini çeker.
     * Sonuçları önbelleğe alır.
     */
    suspend fun getTmdbEpisodes(tmdbId: Int, seasonNumber: Int): List<TmdbEpisodeDto> = withContext(Dispatchers.IO) {
        if (tmdbId <= 0 || seasonNumber <= 0) return@withContext emptyList()
        val cacheKey = tmdbId to seasonNumber

        val cached = mutex.withLock { DetailCache.tmdbEpisodesCache[cacheKey]?.map { it.toDto() } }
        if (cached != null) return@withContext cached

        val deferred = mutex.withLock {
            tmdbEpisodesInFlight[cacheKey] ?: scope.async {
                try {
                    val apiKey = TmdbApiClient.getActiveApiKey()

                    val langTag = TmdbApiClient.getActiveLanguage()

                    val url = runCatching {
                        URL("https://api.themoviedb.org/3/tv/$tmdbId/season/$seasonNumber?api_key=$apiKey&language=$langTag")
                    }.getOrNull() ?: return@async emptyList<TmdbEpisodeDto>()

                    val responseText = KitsugiApiBase.executeGetRequest(url) ?: return@async emptyList<TmdbEpisodeDto>()
                    val parsed = parseTmdbEpisodes(responseText)
                    if (parsed.isNotEmpty()) {
                        mutex.withLock {
                            DetailCache.tmdbEpisodesCache[cacheKey] = parsed.map { it.toCached() }
                        }
                    }
                    parsed
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch TMDB episodes for tmdbId=$tmdbId season=$seasonNumber: ${e.message}")
                    emptyList()
                } finally {
                    mutex.withLock { tmdbEpisodesInFlight.remove(cacheKey) }
                }
            }.also { tmdbEpisodesInFlight[cacheKey] = it }
        }
        deferred.await()
    }

    private fun parseTmdbEpisodes(json: String): List<TmdbEpisodeDto> {
        return runCatching {
            val root = JSONObject(json)
            val episodes = root.optJSONArray("episodes") ?: return emptyList()
            val list = mutableListOf<TmdbEpisodeDto>()
            for (i in 0 until episodes.length()) {
                val ep = episodes.optJSONObject(i) ?: continue
                val epNum = ep.optInt("episode_number", -1)
                if (epNum <= 0) continue
                val name = ep.optNullableString("name")
                val overview = ep.optNullableString("overview")
                val stillPath = ep.optNullableString("still_path")
                val airDate = ep.optNullableString("air_date")
                list.add(
                    TmdbEpisodeDto(
                        episodeNumber = epNum,
                        name = name,
                        overview = overview,
                        stillPath = stillPath,
                        airDate = airDate
                    )
                )
            }
            list
        }.getOrElse {
            Log.w(TAG, "TMDB episodes parse failed: ${it.message}")
            emptyList()
        }
    }

    suspend fun resolveTmdbIdFromKitsu(kitsuId: Int): Int? = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext null
        if (kitsuId <= 0) return@withContext null
        val cacheKey = kitsuId + 300_000_000

        // Önbellek kontrolü — tamamen mutex içinde
        val isCached = mutex.withLock { malToTmdbCache.containsKey(cacheKey) }
        if (isCached) return@withContext mutex.withLock { malToTmdbCache[cacheKey] }

        // Room önbellek kontrolü
        val cachedEntity = runCatching { dao?.getByKitsuId(kitsuId.toString()) }.getOrNull()
        if (cachedEntity != null) {
            val id = cachedEntity.tmdbId
            mutex.withLock { malToTmdbCache[cacheKey] = id }
            if (cachedEntity.tvdbId != null && cachedEntity.tvdbId > 0) {
                mutex.withLock { tmdbToTvdbCache[id] = cachedEntity.tvdbId }
            }
            Log.d(TAG, "Room hit: kitsuId=$kitsuId → tmdbId=$id")
            return@withContext id
        }

        val tmdbId = runCatching {
            val url = URL("https://arm.haglund.dev/api/v2/ids?source=kitsu&id=$kitsuId")
            val response = KitsugiApiBase.executeGetRequest(url) ?: return@runCatching null
            val json = JSONObject(response)
            val tvdbVal = json.optInt("thetvdb", -1).takeIf { it > 0 }
            if (tvdbVal != null) {
                val tmdbVal = json.optInt("themoviedb", -1)
                if (tmdbVal > 0) {
                    mutex.withLock { tmdbToTvdbCache[tmdbVal] = tvdbVal }
                }
            }
            if (json.isNull("themoviedb")) null
            else {
                val value = json.optInt("themoviedb", -1)
                if (value > 0) {
                    val malId = if (!json.isNull("myanimelist")) json.optInt("myanimelist", -1) else null
                    val cleanMalId = if (malId != null && malId > 0) malId else null
                    val aniListId = if (!json.isNull("anilist")) json.optInt("anilist", -1) else null
                    val cleanAniListId = if (aniListId != null && aniListId > 0) aniListId else null
                    val existing = dao?.getByTmdbId(value)
                    val updated = existing?.copy(
                        kitsuId = kitsuId.toString(),
                        malId = cleanMalId ?: existing.malId,
                        aniListId = cleanAniListId ?: existing.aniListId,
                        tvdbId = tvdbVal ?: existing.tvdbId
                    ) ?: MediaMetaCacheEntity(
                        tmdbId = value,
                        malId = cleanMalId,
                        aniListId = cleanAniListId,
                        logoUrl = null,
                        logoNotFound = false,
                        kitsuId = kitsuId.toString(),
                        tvdbId = tvdbVal
                    )
                    dao?.insert(updated)
                    Log.d(TAG, "Room write (Kitsu): kitsuId=$kitsuId → tmdbId=$value")
                    value
                } else null
            }
        }.getOrElse {
            Log.w(TAG, "ARM API lookup failed for kitsuId=$kitsuId: ${it.message}")
            null
        }

        mutex.withLock { malToTmdbCache[cacheKey] = tmdbId }
        Log.d(TAG, "ARM lookup kitsu: kitsuId=$kitsuId → tmdbId=$tmdbId")
        tmdbId
    }

    private suspend fun resolveTvdbIdFromKitsu(kitsuId: Int): Int? = withContext(Dispatchers.IO) {
        if (kitsuId <= 0) return@withContext null
        runCatching {
            val url = URL("https://arm.haglund.dev/api/v2/ids?source=kitsu&id=$kitsuId")
            val response = KitsugiApiBase.executeGetRequest(url) ?: return@runCatching null
            val json = JSONObject(response)
            val value = json.optInt("thetvdb", -1)
            if (value > 0) {
                Log.d(TAG, "TVDB via ARM (Kitsu): kitsuId=$kitsuId → tvdbId=$value")
                value
            } else null
        }.getOrElse {
            Log.w(TAG, "resolveTvdbIdFromKitsu failed for kitsuId=$kitsuId: ${it.message}")
            null
        }
    }

    suspend fun getEpisodeRatingsByKitsuId(kitsuId: Int): Map<Pair<Int, Int>, Double> = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext emptyMap()
        if (kitsuId <= 0) return@withContext emptyMap()
        val tmdbId = resolveTmdbIdFromKitsu(kitsuId) ?: return@withContext emptyMap()
        fetchWithCache(tmdbId)
    }

    suspend fun getLogoUrlByKitsuId(kitsuId: Int): String? = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext null
        if (kitsuId <= 0) return@withContext null
        val tmdbId = resolveTmdbIdFromKitsu(kitsuId) ?: return@withContext null
        getLogoUrl(tmdbId)
    }

    suspend fun getResolvedTmdbIdForKitsu(kitsuId: Int): Int? = mutex.withLock {
        malToTmdbCache[kitsuId + 300_000_000]
    }
}
