package com.kitsugi.animelist.ui.screens.detail

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.local.TranslationManager
import com.kitsugi.animelist.data.remote.DetailCache
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.MdbListClient
import com.kitsugi.animelist.data.remote.MdbListRatings
import com.kitsugi.animelist.data.remote.KitsugiCharacter
import com.kitsugi.animelist.data.remote.KitsugiEpisodeRatingsRepository
import com.kitsugi.animelist.data.remote.KitsugiMediaDetail
import com.kitsugi.animelist.data.remote.KitsugiRelation
import com.kitsugi.animelist.data.remote.KitsugiReview
import com.kitsugi.animelist.data.remote.KitsugiStaff
import com.kitsugi.animelist.data.remote.KitsugiStats
import com.kitsugi.animelist.data.remote.KitsugiStreamingEpisode
import com.kitsugi.animelist.data.remote.GalleryItem
import com.kitsugi.animelist.data.remote.GalleryCategory
import com.kitsugi.animelist.data.remote.KitsugiIdResolver
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApiResultDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val apiClient = JikanApiClient()
    private val translationManager = TranslationManager(context)
    private val settingsDataStore = SettingsDataStore(context)
    private val TAG = "ApiResultDetailVM"

    // --- StateFlows ---

    private val _detailState = MutableStateFlow<KitsugiMediaDetail?>(null)
    val detailState: StateFlow<KitsugiMediaDetail?> = _detailState.asStateFlow()

    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading.asStateFlow()

    private val _translatedSynopsis = MutableStateFlow<String?>(null)
    val translatedSynopsis: StateFlow<String?> = _translatedSynopsis.asStateFlow()

    private val _logoUrl = MutableStateFlow<String?>(null)
    val logoUrl: StateFlow<String?> = _logoUrl.asStateFlow()

    private val _episodeRatings = MutableStateFlow<Map<Pair<Int, Int>, Double>>(emptyMap())
    val episodeRatings: StateFlow<Map<Pair<Int, Int>, Double>> = _episodeRatings.asStateFlow()

    private val _resolvedTmdbId = MutableStateFlow<Int?>(null)
    val resolvedTmdbId: StateFlow<Int?> = _resolvedTmdbId.asStateFlow()

    // --- MDBList Puanları ---
    private val _mdbListRatings = MutableStateFlow<MdbListRatings?>(null)
    val mdbListRatings: StateFlow<MdbListRatings?> = _mdbListRatings.asStateFlow()

    private val _mdbListLoading = MutableStateFlow(false)
    val mdbListLoading: StateFlow<Boolean> = _mdbListLoading.asStateFlow()

    private val _charactersState = MutableStateFlow<DetailTabState<List<KitsugiCharacter>>>(DetailTabState.Loading)
    val charactersState: StateFlow<DetailTabState<List<KitsugiCharacter>>> = _charactersState.asStateFlow()

    private val _staffState = MutableStateFlow<DetailTabState<List<KitsugiStaff>>>(DetailTabState.Loading)
    val staffState: StateFlow<DetailTabState<List<KitsugiStaff>>> = _staffState.asStateFlow()

    private val _relationsState = MutableStateFlow<DetailTabState<List<KitsugiRelation>>>(DetailTabState.Loading)
    val relationsState: StateFlow<DetailTabState<List<KitsugiRelation>>> = _relationsState.asStateFlow()

    private val _recommendationsState = MutableStateFlow<DetailTabState<List<KitsugiRelation>>>(DetailTabState.Loading)
    val recommendationsState: StateFlow<DetailTabState<List<KitsugiRelation>>> = _recommendationsState.asStateFlow()

    private val _statsState = MutableStateFlow<DetailTabState<KitsugiStats?>>(DetailTabState.Loading)
    val statsState: StateFlow<DetailTabState<KitsugiStats?>> = _statsState.asStateFlow()

    private val _reviewsState = MutableStateFlow<DetailTabState<List<KitsugiReview>>>(DetailTabState.Loading)
    val reviewsState: StateFlow<DetailTabState<List<KitsugiReview>>> = _reviewsState.asStateFlow()

    private val _episodesState = MutableStateFlow<DetailTabState<List<KitsugiStreamingEpisode>>>(DetailTabState.Loading)
    val episodesState: StateFlow<DetailTabState<List<KitsugiStreamingEpisode>>> = _episodesState.asStateFlow()

    private val _targetSeason = MutableStateFlow<Int>(1)
    val targetSeason: StateFlow<Int> = _targetSeason.asStateFlow()

    private val _galleryItems = MutableStateFlow<List<GalleryItem>>(emptyList())
    val galleryItems: StateFlow<List<GalleryItem>> = _galleryItems.asStateFlow()

    // --- Cache / Lock Key ---
    private var currentFetchKey: String? = null

    fun loadResult(result: JikanSearchResult, showAnimeLogos: Boolean, forceRefresh: Boolean = false) {
        if (forceRefresh) {
            currentFetchKey = null
            DetailCache.removeMediaDetail(result.source, result.malId)
            DetailCache.removeMediaCharacters(result.source, result.malId)
            DetailCache.removeMediaStaff(result.source, result.malId)
            DetailCache.removeMediaRelations(result.source, result.malId)
            DetailCache.removeMediaRecommendations(result.source, result.malId)
            DetailCache.removeMediaReviews(result.source, result.malId)
            DetailCache.removeMediaEpisodes(result.source, result.malId)
        }

        val newKey = "${result.source}:${result.malId}:${result.type.name}"
        if (newKey == currentFetchKey) {
            Log.d(TAG, "loadResult: Cache hit for key=$newKey — skipping")
            return
        }

        Log.d(TAG, "loadResult: New key=$newKey (was $currentFetchKey)")
        currentFetchKey = newKey

        val cachedDetail = DetailCache.getMediaDetail(result.source, result.malId)
        val cachedSynopsisTranslation = DetailCache.getTranslation("synopsis", result.source, result.malId)

        _detailState.value = cachedDetail
        _detailLoading.value = cachedDetail == null
        _translatedSynopsis.value = cachedSynopsisTranslation
        _logoUrl.value = null
        _episodeRatings.value = emptyMap()
        _resolvedTmdbId.value = null
        _galleryItems.value = emptyList()

        val cachedCharacters = DetailCache.getMediaCharacters(result.source, result.malId)
        if (cachedCharacters != null && cachedCharacters.isEmpty()) {
            DetailCache.removeMediaCharacters(result.source, result.malId)
            _charactersState.value = DetailTabState.Loading
        } else {
            _charactersState.value = if (cachedCharacters != null) DetailTabState.Success(cachedCharacters) else DetailTabState.Loading
        }

        val cachedStaff = DetailCache.getMediaStaff(result.source, result.malId)
        if (cachedStaff != null && cachedStaff.isEmpty()) {
            DetailCache.removeMediaStaff(result.source, result.malId)
            _staffState.value = DetailTabState.Loading
        } else {
            _staffState.value = if (cachedStaff != null) DetailTabState.Success(cachedStaff) else DetailTabState.Loading
        }

        val cachedRelations = DetailCache.getMediaRelations(result.source, result.malId)
        if (cachedRelations != null && cachedRelations.isEmpty()) {
            DetailCache.removeMediaRelations(result.source, result.malId)
            _relationsState.value = DetailTabState.Loading
        } else {
            _relationsState.value = if (cachedRelations != null) DetailTabState.Success(cachedRelations) else DetailTabState.Loading
        }

        val cachedRecommendations = DetailCache.getMediaRecommendations(result.source, result.malId)
        if (cachedRecommendations != null && cachedRecommendations.isEmpty()) {
            DetailCache.removeMediaRecommendations(result.source, result.malId)
            _recommendationsState.value = DetailTabState.Loading
        } else {
            _recommendationsState.value = if (cachedRecommendations != null) DetailTabState.Success(cachedRecommendations) else DetailTabState.Loading
        }

        val cachedStats = DetailCache.getMediaStats(result.source, result.malId)
        _statsState.value = if (DetailCache.hasMediaStats(result.source, result.malId)) DetailTabState.Success(cachedStats) else DetailTabState.Loading

        val cachedReviews = DetailCache.getMediaReviews(result.source, result.malId)
        _reviewsState.value = if (cachedReviews != null) DetailTabState.Success(cachedReviews) else DetailTabState.Loading

        val cachedEpisodes = DetailCache.getMediaEpisodes(result.source, result.malId)
        // Boş liste cache'deyse yeniden yükleme yapılabilmesi için sil
        if (cachedEpisodes != null && cachedEpisodes.isEmpty()) {
            DetailCache.removeMediaEpisodes(result.source, result.malId)
            _episodesState.value = DetailTabState.Loading
        } else {
            _episodesState.value = if (cachedEpisodes != null) DetailTabState.Success(cachedEpisodes) else DetailTabState.Loading
        }

        viewModelScope.launch {
            try {
                fetchDetail(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching detail: ${e.message}", e)
                _detailLoading.value = false
            }
            try {
                fetchFanartGallery(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Fanart gallery: ${e.message}", e)
            }
            try {
                fetchMdbListRatings(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching mdbList ratings: ${e.message}", e)
            }
        }

        viewModelScope.launch {
            try {
                fetchLogo(result, showAnimeLogos)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching logo: ${e.message}", e)
            }
        }
    }

    fun setTargetSeason(season: Int, result: JikanSearchResult, realMalId: Int?) {
        _targetSeason.value = season
        _episodesState.value = DetailTabState.Loading
        DetailCache.removeMediaEpisodes(result.source, result.malId)
        loadTab(7, result, realMalId)
    }

    private suspend fun fetchDetail(result: JikanSearchResult) {
        // TMDB devre dışıysa TMDB zenginleştirmesini atla
        val settings = runCatching { settingsDataStore.settingsFlow.first() }.getOrNull()
        val tmdbEnabled = settings?.tmdbEnabled ?: true

        val cached = DetailCache.getMediaDetail(result.source, result.malId)
        val detail = if (cached != null) {
            cached
        } else {
            _detailLoading.value = true
            val fetched = try {
                withContext(Dispatchers.IO) {
                    apiClient.fetchDetail(
                        source = result.source,
                        externalId = result.malId,
                        mediaType = result.type,
                        // TMDB devre dışıysa tmdbId gönderme
                        tmdbId = if (tmdbEnabled) result.tmdbId else null,
                        realMalId = result.realMalId,
                        title = result.title
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during apiClient.fetchDetail: ${e.message}", e)
                null
            }
            if (fetched != null) {
                DetailCache.putMediaDetail(result.source, result.malId, fetched)
            }
            fetched
        }

        _detailState.value = detail
        _detailLoading.value = false

        if (detail != null) {
            val determinedSeason = KitsugiEpisodeRatingsRepository.determineTargetSeason(
                tmdbSeason = detail.tmdbSeason,
                title = result.title,
                titleEnglish = detail.titleEnglish,
                synonyms = detail.synonyms.orEmpty()
            )
            _targetSeason.value = determinedSeason

            // Önce ham synopsis'i göster
            val rawSynopsis = detail.synopsis
            if (!rawSynopsis.isNullOrBlank()) {
                _translatedSynopsis.value = rawSynopsis
                // Google Translate yalnızca kullanıcı otomatik çeviri ayarını açtıysa çalışır.
                // Jikan/MAL için Türkçe synopsis artık TMDB üzerinden (ARM ID resolve) doğrudan geliyor.
                val autoTranslate = settings?.autoTranslateEnabled ?: false
                if (autoTranslate) {
                    val cachedTr = DetailCache.getTranslation("synopsis", result.source, result.malId)
                    if (cachedTr != null) {
                        _translatedSynopsis.value = cachedTr
                    } else {
                        val tr = withContext(Dispatchers.IO) {
                            translationManager.translateToTurkish(rawSynopsis)
                        }
                        if (!tr.isNullOrBlank() && tr != rawSynopsis) {
                            DetailCache.putTranslation("synopsis", result.source, result.malId, tr)
                            _translatedSynopsis.value = tr
                        }
                    }
                }
            }

            // Fetch episode ratings
            fetchEpisodeRatings(result, detail)

            // Detail yüklendi → eğer TvShow/Anime ise ve episode state'i boş/loading ise,
            // artık tmdbId biliniyor — episode'ları otomatik olarak yeniden çek.
            val hasTvEps = result.type == com.kitsugi.animelist.model.MediaType.Anime ||
                result.type == com.kitsugi.animelist.model.MediaType.TvShow
            if (hasTvEps) {
                val epState = _episodesState.value
                val needsEpLoad = epState !is DetailTabState.Success ||
                    (epState is DetailTabState.Success && epState.data.isEmpty())
                if (needsEpLoad) {
                    android.util.Log.d(TAG, "fetchDetail: detail loaded, auto-triggering episode load for ${result.source}/${result.malId}")
                    loadTab(7, result, result.realMalId ?: detail.realMalId)
                }
            }
        }
    }

    private suspend fun fetchEpisodeRatings(result: JikanSearchResult, detail: KitsugiMediaDetail) {
        val settings = runCatching { settingsDataStore.settingsFlow.first() }.getOrNull()
        val tmdbEnabled = settings?.tmdbEnabled ?: true
        if (!tmdbEnabled) {
            _episodeRatings.value = emptyMap()
            _resolvedTmdbId.value = null
            return
        }

        val tmdbId = detail.tmdbId
        var foundRatings = emptyMap<Pair<Int, Int>, Double>()
        var resolvedId: Int? = null

        withContext(Dispatchers.IO) {
            when {
                tmdbId != null && tmdbId > 0 -> {
                    resolvedId = tmdbId
                    foundRatings = KitsugiEpisodeRatingsRepository.getEpisodeRatings(tmdbId)
                }
                result.source.equals("anilist", ignoreCase = true) -> {
                    val stableId = result.malId
                    if (stableId >= 100_000_000) {
                        // Saf AniList ID (offset'li): 100M’ı çıkarıp AniList ID'yi al
                        val aniListId = stableId - 100_000_000
                        if (aniListId > 0) {
                            foundRatings = KitsugiEpisodeRatingsRepository.getEpisodeRatingsByAniListId(aniListId)
                            resolvedId = KitsugiEpisodeRatingsRepository.getResolvedTmdbIdForAniList(aniListId)
                        }
                    } else {
                        // stableId < 100M → MAL ID ile döndü
                        foundRatings = KitsugiEpisodeRatingsRepository.getEpisodeRatingsByMalId(stableId)
                        resolvedId = KitsugiEpisodeRatingsRepository.getResolvedTmdbIdForMal(stableId)
                    }

                    if (foundRatings.isEmpty()) {
                        val malId = detail.realMalId
                            ?: if (stableId < 100_000_000) stableId else null
                        if (malId != null && malId > 0) {
                            foundRatings = KitsugiEpisodeRatingsRepository.getEpisodeRatingsByMalId(malId)
                            resolvedId = KitsugiEpisodeRatingsRepository.getResolvedTmdbIdForMal(malId)
                        }
                    }
                }
                else -> {
                    val malId = result.malId
                    if (malId > 0) {
                        foundRatings = KitsugiEpisodeRatingsRepository.getEpisodeRatingsByMalId(malId)
                        resolvedId = KitsugiEpisodeRatingsRepository.getResolvedTmdbIdForMal(malId)
                    }
                }
            }
        }

        _episodeRatings.value = foundRatings
        _resolvedTmdbId.value = resolvedId
    }

    private suspend fun fetchLogo(result: JikanSearchResult, showAnimeLogos: Boolean) {
        if (!showAnimeLogos) {
            _logoUrl.value = null
            return
        }
        val stableId = result.malId
        val logo = withContext(Dispatchers.IO) {
            when {
                result.source.equals("tmdb", ignoreCase = true) -> {
                    if (stableId > 0) KitsugiEpisodeRatingsRepository.getLogoUrl(stableId) else null
                }
                result.source.equals("anilist", ignoreCase = true) -> {
                    if (stableId >= 100_000_000) {
                        val aniListId = stableId - 100_000_000
                        KitsugiEpisodeRatingsRepository.getLogoUrlByAniListId(aniListId)
                    } else {
                        KitsugiEpisodeRatingsRepository.getLogoUrlByMalId(stableId)
                    }
                }
                stableId > 0 -> KitsugiEpisodeRatingsRepository.getLogoUrlByMalId(stableId)
                else -> null
            }
        }
        _logoUrl.value = logo
    }

    fun loadTab(tabIndex: Int, result: JikanSearchResult, realMalId: Int?) {
        val malId = result.malId
        // TMDB kaynaklı içerikte malId zaten tmdbId'dir
        val tmdbId = _detailState.value?.tmdbId
            ?: result.tmdbId
            ?: if (result.source.equals("tmdb", ignoreCase = true)) result.malId else null
        viewModelScope.launch {
            val settings = runCatching { settingsDataStore.settingsFlow.first() }.getOrNull()
            val tmdbEnabled    = settings?.tmdbEnabled    ?: true
            val useCredits     = settings?.tmdbUseCredits  ?: true
            val useEpisodes    = settings?.tmdbUseEpisodes ?: true
            val useMoreLikeThis = settings?.tmdbUseMoreLikeThis ?: true
            try {
                when (tabIndex) {
                    1 -> {
                        val currentSuccess = _charactersState.value as? DetailTabState.Success
                        val needsRefetch = currentSuccess == null ||
                            (currentSuccess.data.isEmpty() && DetailCache.getMediaCharacters(result.source, malId) == null)
                        if (needsRefetch) {
                            _charactersState.value = DetailTabState.Loading
                            val data = withContext(Dispatchers.IO) {
                                apiClient.fetchCharacters(
                                    source = result.source,
                                    externalId = result.malId,
                                    mediaType = result.type,
                                    realMalId = realMalId,
                                    // Credits devre dışıysa ya da TMDB tamamen kapalıysa tmdbId gönderme
                                    tmdbId = if (tmdbEnabled && useCredits) tmdbId else null
                                )
                            }
                            if (data.isNotEmpty()) {
                                DetailCache.putMediaCharacters(result.source, malId, data)
                            }
                            _charactersState.value = DetailTabState.Success(data)
                        }
                    }
                    2 -> {
                        val currentSuccess = _staffState.value as? DetailTabState.Success
                        val needsRefetch = currentSuccess == null ||
                            (currentSuccess.data.isEmpty() && DetailCache.getMediaStaff(result.source, malId) == null)
                        if (needsRefetch) {
                            _staffState.value = DetailTabState.Loading
                            val data = withContext(Dispatchers.IO) {
                                apiClient.fetchStaff(
                                    result.source, result.malId, result.type,
                                    tmdbId = if (tmdbEnabled && useCredits) tmdbId else null,
                                    realMalId = realMalId
                                )
                            }
                            if (data.isNotEmpty()) {
                                DetailCache.putMediaStaff(result.source, malId, data)
                            }
                            _staffState.value = DetailTabState.Success(data)
                        }
                    }
                    3 -> {
                        val currentSuccess = _recommendationsState.value as? DetailTabState.Success
                        val needsRefetch = currentSuccess == null ||
                            (currentSuccess.data.isEmpty() && DetailCache.getMediaRecommendations(result.source, malId) == null)
                        if (needsRefetch) {
                            _recommendationsState.value = DetailTabState.Loading
                            val data = withContext(Dispatchers.IO) {
                                apiClient.fetchRecommendations(
                                    result.source, result.malId, result.type,
                                    tmdbId = if (tmdbEnabled && useMoreLikeThis) tmdbId else null,
                                    realMalId = realMalId
                                )
                            }
                            if (data.isNotEmpty()) {
                                DetailCache.putMediaRecommendations(result.source, malId, data)
                            }
                            _recommendationsState.value = DetailTabState.Success(data)
                        }
                    }
                    4 -> {
                        val currentSuccess = _relationsState.value as? DetailTabState.Success
                        val needsRefetch = currentSuccess == null ||
                            (currentSuccess.data.isEmpty() && DetailCache.getMediaRelations(result.source, malId) == null)
                        if (needsRefetch) {
                            _relationsState.value = DetailTabState.Loading
                            val data = withContext(Dispatchers.IO) {
                                apiClient.fetchRelations(
                                    result.source, result.malId, result.type,
                                    tmdbId = if (tmdbEnabled && useMoreLikeThis) tmdbId else null,
                                    realMalId = realMalId
                                )
                            }
                            if (data.isNotEmpty()) {
                                DetailCache.putMediaRelations(result.source, malId, data)
                            }
                            _relationsState.value = DetailTabState.Success(data)
                        }
                    }
                    5 -> {
                        if (_statsState.value !is DetailTabState.Success) {
                            _statsState.value = DetailTabState.Loading
                            val data = withContext(Dispatchers.IO) {
                                apiClient.fetchStats(
                                    result.source, result.malId, result.type,
                                    realMalId = realMalId
                                    // Note: fetchStats uses externalId (result.malId) as tmdbId when source="tmdb"
                                )
                            }
                            if (data != null) {
                                DetailCache.putMediaStats(result.source, malId, data)
                                _statsState.value = DetailTabState.Success(data)
                            } else {
                                _statsState.value = DetailTabState.Success(null)
                            }
                        }
                    }
                    6 -> {
                        val currentSuccess = _reviewsState.value as? DetailTabState.Success
                        val needsRefetch = currentSuccess == null ||
                            (currentSuccess.data.isEmpty() && DetailCache.getMediaReviews(result.source, malId) == null)
                        if (needsRefetch) {
                            _reviewsState.value = DetailTabState.Loading
                            val data = withContext(Dispatchers.IO) {
                                apiClient.fetchReviews(
                                    result.source, result.malId, result.type,
                                    tmdbId = if (tmdbEnabled) tmdbId else null,
                                    realMalId = realMalId
                                )
                            }
                            if (data.isNotEmpty()) {
                                DetailCache.putMediaReviews(result.source, malId, data)
                            }
                            _reviewsState.value = DetailTabState.Success(data)
                        }
                    }
                    7 -> {
                        val currentEpisodes = _episodesState.value
                        // Boş liste veya Loading ise yeniden yükle
                        val needsEpFetch = currentEpisodes !is DetailTabState.Success ||
                            (currentEpisodes is DetailTabState.Success && currentEpisodes.data.isEmpty())
                        if (needsEpFetch) {
                            _episodesState.value = DetailTabState.Loading
                            val data = withContext(Dispatchers.IO) {
                                apiClient.fetchEpisodes(
                                    source = result.source,
                                    externalId = result.malId,
                                    mediaType = result.type,
                                    realMalId = realMalId,
                                    totalEpisodes = result.total ?: _detailState.value?.total,
                                    context = context,
                                    targetSeason = _targetSeason.value,
                                    // Episodes (TMDB bölüm başlıkları/thumbnail'ları) devre dışıysa tmdbId gönderme
                                    tmdbId = if (tmdbEnabled && useEpisodes) tmdbId else null
                                )
                            }
                            if (data.isNotEmpty()) {
                                DetailCache.putMediaEpisodes(result.source, malId, data)
                            }
                            _episodesState.value = DetailTabState.Success(data)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading tab data for index $tabIndex: ${e.message}", e)
                when (tabIndex) {
                    1 -> _charactersState.value = DetailTabState.Error
                    2 -> _staffState.value = DetailTabState.Error
                    3 -> _recommendationsState.value = DetailTabState.Error
                    4 -> _relationsState.value = DetailTabState.Error
                    5 -> _statsState.value = DetailTabState.Error
                    6 -> _reviewsState.value = DetailTabState.Error
                    7 -> _episodesState.value = DetailTabState.Error
                }
            }
        }
    }

    private suspend fun fetchMdbListRatings(result: JikanSearchResult) {
        val settings = settingsDataStore.settingsFlow.first()
        if (!settings.mdbListEnabled || settings.mdbListApiKey.isBlank()) {
            _mdbListRatings.value = null
            return
        }

        _mdbListLoading.value = true

        try {
            val malId = result.malId
            val aniListId = if (result.source.equals("anilist", ignoreCase = true) && malId >= 100_000_000) {
                // Saf AniList ID (100M+ offset)
                malId - 100_000_000
            } else {
                null
            }
            // stableId < 100M → AniList kaynaklı olsa bile MAL ID ile dönmüş
            val realMalId = if (aniListId != null) null else if (!result.source.equals("anilist", ignoreCase = true)) malId else null

            val detail = _detailState.value
            val tmdbId = detail?.tmdbId ?: result.tmdbId

            var imdbId: String? = detail?.externalLinks?.firstOrNull { it.url.contains("imdb.com") }
                ?.url?.substringAfter("/title/")?.substringBefore("/")

            if (imdbId.isNullOrBlank()) {
                val resolved = KitsugiIdResolver.resolveIds(
                    malId = realMalId,
                    aniListId = aniListId,
                    tmdbId = tmdbId,
                    mediaType = result.type
                )
                imdbId = resolved.imdbId
            }

            if (!imdbId.isNullOrBlank()) {
                val ratings = MdbListClient.fetchRatings(imdbId, settings.mdbListApiKey)
                _mdbListRatings.value = ratings
            } else {
                Log.w(TAG, "MDBList: IMDb ID resolved to null")
                _mdbListRatings.value = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "MDBList rating fetch failed", e)
            _mdbListRatings.value = null
        } finally {
            _mdbListLoading.value = false
        }
    }

    private suspend fun fetchFanartGallery(result: JikanSearchResult) {
        val tmdbId = withContext(Dispatchers.IO) {
            val stableId = result.malId
            when {
                result.tmdbId != null && result.tmdbId > 0 -> result.tmdbId
                result.source.equals("tmdb", ignoreCase = true) -> if (stableId > 0) stableId else null
                result.source.equals("anilist", ignoreCase = true) -> {
                    if (stableId >= 100_000_000) {
                        val aniListId = stableId - 100_000_000
                        KitsugiEpisodeRatingsRepository.resolveTmdbIdFromAniList(aniListId)
                    } else {
                        KitsugiEpisodeRatingsRepository.resolveTmdbIdFromMal(stableId)
                    }
                }
                stableId > 0 -> KitsugiEpisodeRatingsRepository.resolveTmdbIdFromMal(stableId)
                else -> null
            }
        }

        val isMovie = result.type == MediaType.Movie

        val fanartItems = if (tmdbId != null && tmdbId > 0) {
            withContext(Dispatchers.IO) {
                KitsugiEpisodeRatingsRepository.getFanartGalleryItems(tmdbId, isMovie)
            }
        } else {
            emptyList()
        }

        val currentDetail = _detailState.value
        val existingItems = buildList {
            val imageUrl = result.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                val src = determineGallerySource(imageUrl, result.source)
                val cat = determineGalleryCategory(imageUrl, GalleryCategory.POSTER)
                add(GalleryItem(url = imageUrl, source = src, category = cat))
            }
            currentDetail?.pictures?.forEach { url ->
                if (url.isNotBlank()) {
                    val src = determineGallerySource(url, result.source)
                    val cat = determineGalleryCategory(url, GalleryCategory.POSTER)
                    add(GalleryItem(url = url, source = src, category = cat))
                }
            }
        }

        val merged = (fanartItems + existingItems).distinctBy { it.url }
        _galleryItems.value = merged
    }

    private fun determineGallerySource(url: String, fallbackSource: String): String {
        val lowerUrl = url.lowercase()
        return when {
            lowerUrl.contains("fanart.tv") -> "Fanart.tv"
            lowerUrl.contains("image.tmdb.org") || lowerUrl.contains("tmdb.org") -> "TMDB"
            lowerUrl.contains("anilist.co") -> "AniList"
            lowerUrl.contains("simkl.in") || lowerUrl.contains("simkl.com") -> "Simkl"
            lowerUrl.contains("myanimelist.net") || lowerUrl.contains("jikan.moe") -> "Jikan"
            lowerUrl.contains("kitsu.io") -> "Kitsu"
            else -> when (fallbackSource.lowercase()) {
                "anilist" -> "AniList"
                "tmdb" -> "TMDB"
                "simkl" -> "Simkl"
                "jikan", "mal" -> "Jikan"
                "kitsu" -> "Kitsu"
                else -> fallbackSource.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
    }

    private fun determineGalleryCategory(url: String, defaultCategory: GalleryCategory): GalleryCategory {
        val lowerUrl = url.lowercase()
        return when {
            lowerUrl.contains("logo") || lowerUrl.contains("clearart") -> GalleryCategory.LOGO
            lowerUrl.contains("backdrop") || lowerUrl.contains("background") || lowerUrl.contains("/w1280") || lowerUrl.contains("showbackground") -> GalleryCategory.BACKDROP
            lowerUrl.contains("poster") || lowerUrl.contains("/w780") || lowerUrl.contains("/w500") || lowerUrl.contains("/w342") || lowerUrl.contains("coverimage") || lowerUrl.contains("large_image_url") -> GalleryCategory.POSTER
            lowerUrl.contains("character") || lowerUrl.contains("actor") -> GalleryCategory.CHARACTER
            lowerUrl.contains("thumb") || lowerUrl.contains("still") || lowerUrl.contains("/w300") || lowerUrl.contains("/w185") -> GalleryCategory.THUMBNAIL
            lowerUrl.contains("banner") -> GalleryCategory.BANNER
            else -> defaultCategory
        }
    }
}

