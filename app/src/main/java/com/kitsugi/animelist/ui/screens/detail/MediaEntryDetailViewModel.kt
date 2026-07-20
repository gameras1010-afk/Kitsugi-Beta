package com.kitsugi.animelist.ui.screens.detail

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.local.TranslationManager
import com.kitsugi.animelist.data.remote.DetailCache
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.KitsugiCharacter
import com.kitsugi.animelist.data.remote.KitsugiEpisodeRatingsRepository
import com.kitsugi.animelist.data.remote.KitsugiMediaDetail
import com.kitsugi.animelist.data.remote.KitsugiRelation
import com.kitsugi.animelist.data.remote.KitsugiReview
import com.kitsugi.animelist.data.remote.KitsugiStaff
import com.kitsugi.animelist.data.remote.KitsugiStats
import com.kitsugi.animelist.data.remote.KitsugiStreamingEpisode
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.kitsugi.animelist.data.local.MangaMappingEntity
import com.kitsugi.animelist.data.manga.MangaSourceRepository
import com.kitsugi.animelist.data.remote.MdbListClient
import com.kitsugi.animelist.data.remote.MdbListRatings
import com.kitsugi.animelist.data.remote.KitsugiIdResolver
import com.kitsugi.animelist.data.settings.SettingsDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first

class MediaEntryDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val apiClient = JikanApiClient()
    private val translationManager = TranslationManager(context)
    private val mangaRepository = MangaSourceRepository(context)
    private val settingsDataStore = SettingsDataStore(context)
    private val TAG = "MediaEntryDetailVM"

    // --- StateFlows ---

    private val _mangaMapping = MutableStateFlow<MangaMappingEntity?>(null)
    val mangaMapping: StateFlow<MangaMappingEntity?> = _mangaMapping.asStateFlow()

    private val _detailState = MutableStateFlow<KitsugiMediaDetail?>(null)
    val detailState: StateFlow<KitsugiMediaDetail?> = _detailState.asStateFlow()

    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading.asStateFlow()

    private val _synopsisState = MutableStateFlow<SynopsisState>(SynopsisState.Loading)
    val synopsisState: StateFlow<SynopsisState> = _synopsisState.asStateFlow()

    private val _translatedSynopsis = MutableStateFlow<String?>(null)
    val translatedSynopsis: StateFlow<String?> = _translatedSynopsis.asStateFlow()

    private val _originalSynopsis = MutableStateFlow<String?>(null)
    val originalSynopsis: StateFlow<String?> = _originalSynopsis.asStateFlow()

    private val _logoUrl = MutableStateFlow<String?>(null)
    val logoUrl: StateFlow<String?> = _logoUrl.asStateFlow()

    private val _episodeRatings = MutableStateFlow<Map<Pair<Int, Int>, Double>>(emptyMap())
    val episodeRatings: StateFlow<Map<Pair<Int, Int>, Double>> = _episodeRatings.asStateFlow()

    private val _resolvedTmdbId = MutableStateFlow<Int?>(null)
    val resolvedTmdbId: StateFlow<Int?> = _resolvedTmdbId.asStateFlow()

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

    private val _mdbListRatings = MutableStateFlow<MdbListRatings?>(null)
    val mdbListRatings: StateFlow<MdbListRatings?> = _mdbListRatings.asStateFlow()

    private val _mdbListLoading = MutableStateFlow(false)
    val mdbListLoading: StateFlow<Boolean> = _mdbListLoading.asStateFlow()

    // --- Cache / Lock Key ---
    private var currentFetchKey: String? = null
    private var mangaMappingJob: Job? = null

    /**
     * Initializes state and starts background jobs to fetch all entry-specific details.
     * Prevents redundant scanning if [entry] hasn't changed.
     */
    fun loadEntry(entry: MediaEntry, showAnimeLogos: Boolean) {
        mangaMappingJob?.cancel()
        mangaMappingJob = viewModelScope.launch {
            mangaRepository.observeMangaMapping(entry.id).collect {
                _mangaMapping.value = it
            }
        }

        val newKey = "${entry.id}:${entry.source}:${entry.malId}"
        if (newKey == currentFetchKey) {
            Log.d(TAG, "loadEntry: Cache hit for key=$newKey — skipping")
            return
        }

        Log.d(TAG, "loadEntry: New key=$newKey (was $currentFetchKey)")
        currentFetchKey = newKey

        // Reset states
        val cachedDetail = DetailCache.getMediaDetail(entry.source, entry.malId ?: 0)
        val cachedSynopsisTranslation = DetailCache.getTranslation("synopsis", entry.source, entry.malId ?: 0)

        _detailState.value = cachedDetail
        _detailLoading.value = cachedDetail == null

        _synopsisState.value = when {
            cachedSynopsisTranslation != null -> SynopsisState.Success(cachedSynopsisTranslation)
            !entry.synopsis.isNullOrBlank() -> SynopsisState.Success(entry.synopsis)
            else -> SynopsisState.Loading
        }
        _translatedSynopsis.value = cachedSynopsisTranslation
        _originalSynopsis.value = entry.synopsis

        _logoUrl.value = null
        _episodeRatings.value = emptyMap()
        _resolvedTmdbId.value = null

        // Reset tab states to either cached values or Loading
        val malId = entry.malId ?: 0
        val cachedCharacters = DetailCache.getMediaCharacters(entry.source, malId)
        if (cachedCharacters != null && cachedCharacters.isEmpty()) {
            DetailCache.removeMediaCharacters(entry.source, malId)
            _charactersState.value = DetailTabState.Loading
        } else {
            _charactersState.value = if (cachedCharacters != null) DetailTabState.Success(cachedCharacters) else DetailTabState.Loading
        }

        val cachedStaff = DetailCache.getMediaStaff(entry.source, malId)
        if (cachedStaff != null && cachedStaff.isEmpty()) {
            DetailCache.removeMediaStaff(entry.source, malId)
            _staffState.value = DetailTabState.Loading
        } else {
            _staffState.value = if (cachedStaff != null) DetailTabState.Success(cachedStaff) else DetailTabState.Loading
        }

        val cachedRelations = DetailCache.getMediaRelations(entry.source, malId)
        if (cachedRelations != null && cachedRelations.isEmpty()) {
            DetailCache.removeMediaRelations(entry.source, malId)
            _relationsState.value = DetailTabState.Loading
        } else {
            _relationsState.value = if (cachedRelations != null) DetailTabState.Success(cachedRelations) else DetailTabState.Loading
        }

        val cachedRecommendations = DetailCache.getMediaRecommendations(entry.source, malId)
        if (cachedRecommendations != null && cachedRecommendations.isEmpty()) {
            DetailCache.removeMediaRecommendations(entry.source, malId)
            _recommendationsState.value = DetailTabState.Loading
        } else {
            _recommendationsState.value = if (cachedRecommendations != null) DetailTabState.Success(cachedRecommendations) else DetailTabState.Loading
        }

        val cachedStats = DetailCache.getMediaStats(entry.source, malId)
        _statsState.value = if (DetailCache.hasMediaStats(entry.source, malId)) DetailTabState.Success(cachedStats) else DetailTabState.Loading

        val cachedReviews = DetailCache.getMediaReviews(entry.source, malId)
        _reviewsState.value = if (cachedReviews != null) DetailTabState.Success(cachedReviews) else DetailTabState.Loading

        val cachedEpisodes = DetailCache.getMediaEpisodes(entry.source, malId)
        if (cachedEpisodes != null && cachedEpisodes.isEmpty()) {
            DetailCache.removeMediaEpisodes(entry.source, malId)
            _episodesState.value = DetailTabState.Loading
        } else {
            _episodesState.value = if (cachedEpisodes != null) DetailTabState.Success(cachedEpisodes) else DetailTabState.Loading
        }

        // Run fetches in view model scope
        viewModelScope.launch {
            fetchDetail(entry)
        }

        viewModelScope.launch {
            fetchLogo(entry, showAnimeLogos)
        }

        viewModelScope.launch {
            fetchSynopsis(entry)
        }

        viewModelScope.launch {
            fetchMdbListRatingsForEntry(entry)
        }
    }

    private suspend fun fetchDetail(entry: MediaEntry) {
        val stableId = entry.malId ?: 0
        val cached = DetailCache.getMediaDetail(entry.source, stableId)
        val detail = if (cached != null) {
            cached
        } else {
            _detailLoading.value = true
            val fetched = withContext(Dispatchers.IO) {
                apiClient.fetchDetail(
                    source = entry.source,
                    externalId = entry.malId,
                    mediaType = entry.type
                )
            }
            if (fetched != null) {
                DetailCache.putMediaDetail(entry.source, stableId, fetched)
            }
            fetched
        }

        _detailState.value = detail
        _detailLoading.value = false

        if (detail != null) {
            val trSynopsis = detail.synopsis
            if (!trSynopsis.isNullOrBlank() && trSynopsis != entry.synopsis) {
                _originalSynopsis.value = trSynopsis
                _translatedSynopsis.value = trSynopsis
                _synopsisState.value = SynopsisState.Success(trSynopsis)
            }

            val determinedSeason = KitsugiEpisodeRatingsRepository.determineTargetSeason(
                tmdbSeason = detail.tmdbSeason,
                title = entry.title,
                titleEnglish = detail.titleEnglish,
                synonyms = detail.synonyms.orEmpty()
            )
            _targetSeason.value = determinedSeason

            // Fetch episode ratings
            fetchEpisodeRatings(entry, detail)
        }
    }

    private suspend fun fetchEpisodeRatings(entry: MediaEntry, detail: KitsugiMediaDetail) {
        val tmdbId = detail.tmdbId
        var foundRatings = emptyMap<Pair<Int, Int>, Double>()
        var resolvedId: Int? = null

        withContext(Dispatchers.IO) {
            when {
                tmdbId != null && tmdbId > 0 -> {
                    resolvedId = tmdbId
                    foundRatings = KitsugiEpisodeRatingsRepository.getEpisodeRatings(tmdbId)
                }
                entry.source.equals("anilist", ignoreCase = true) -> {
                    val stableId = entry.malId ?: 0
                    if (stableId >= 100_000_000) {
                        val aniListId = stableId - 100_000_000
                        if (aniListId > 0) {
                            foundRatings = KitsugiEpisodeRatingsRepository.getEpisodeRatingsByAniListId(aniListId)
                            resolvedId = KitsugiEpisodeRatingsRepository.getResolvedTmdbIdForAniList(aniListId)
                        }
                    } else if (stableId > 0) {
                        foundRatings = KitsugiEpisodeRatingsRepository.getEpisodeRatingsByMalId(stableId)
                        resolvedId = KitsugiEpisodeRatingsRepository.getResolvedTmdbIdForMal(stableId)
                    }

                    if (foundRatings.isEmpty()) {
                        val malId = detail.realMalId
                        if (malId != null && malId > 0) {
                            foundRatings = KitsugiEpisodeRatingsRepository.getEpisodeRatingsByMalId(malId)
                            resolvedId = KitsugiEpisodeRatingsRepository.getResolvedTmdbIdForMal(malId)
                        }
                    }
                }
                else -> {
                    val malId = entry.malId
                    if (malId != null && malId > 0) {
                        foundRatings = KitsugiEpisodeRatingsRepository.getEpisodeRatingsByMalId(malId)
                        resolvedId = KitsugiEpisodeRatingsRepository.getResolvedTmdbIdForMal(malId)
                    }
                }
            }
        }

        _episodeRatings.value = foundRatings
        _resolvedTmdbId.value = resolvedId
    }

    private suspend fun fetchLogo(entry: MediaEntry, showAnimeLogos: Boolean) {
        if (!showAnimeLogos) {
            _logoUrl.value = null
            return
        }
        val stableId = entry.malId ?: 0
        val logo = withContext(Dispatchers.IO) {
            when {
                entry.source.equals("tmdb", ignoreCase = true) -> {
                    if (stableId > 0) KitsugiEpisodeRatingsRepository.getLogoUrl(stableId) else null
                }
                entry.source.equals("anilist", ignoreCase = true) -> {
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

    private suspend fun fetchSynopsis(entry: MediaEntry) {
        val stableId = entry.malId ?: 0
        val autoTranslate = runCatching { settingsDataStore.settingsFlow.first() }.getOrNull()?.autoTranslateEnabled ?: false
        _translatedSynopsis.value = null
        _originalSynopsis.value = entry.synopsis

        if (!entry.synopsis.isNullOrBlank()) {
            _synopsisState.value = SynopsisState.Success(entry.synopsis)
            _translatedSynopsis.value = entry.synopsis
            // Otomatik çeviri açıksa arka planda Google Translate uygula (zaten Türkçeyse TranslationManager atlar)
            if (autoTranslate) {
                viewModelScope.launch {
                    val cachedTr = DetailCache.getTranslation("synopsis", entry.source, stableId)
                    if (cachedTr != null) {
                        _synopsisState.value = SynopsisState.Success(cachedTr)
                        _translatedSynopsis.value = cachedTr
                    } else {
                        val tr = withContext(Dispatchers.IO) {
                            translationManager.translateToTurkish(entry.synopsis)
                        }
                        if (!tr.isNullOrBlank() && tr != entry.synopsis) {
                            DetailCache.putTranslation("synopsis", entry.source, stableId, tr)
                            _synopsisState.value = SynopsisState.Success(tr)
                            _translatedSynopsis.value = tr
                        }
                    }
                }
            }
            return
        }

        _synopsisState.value = SynopsisState.Loading

        val synopsis = withContext(Dispatchers.IO) {
            apiClient.fetchSynopsis(
                source = entry.source,
                externalId = entry.malId,
                mediaType = entry.type
            )
        }

        if (synopsis.isNullOrBlank()) {
            _synopsisState.value = SynopsisState.Error
        } else {
            _originalSynopsis.value = synopsis
            _synopsisState.value = SynopsisState.Success(synopsis)
            _translatedSynopsis.value = synopsis

            // Otomatik çeviri açıksa çevir (zaten Türkçeyse TranslationManager atlar)
            if (autoTranslate) {
                val cachedTr = DetailCache.getTranslation("synopsis", entry.source, stableId)
                if (cachedTr != null) {
                    _synopsisState.value = SynopsisState.Success(cachedTr)
                    _translatedSynopsis.value = cachedTr
                } else {
                    val tr = withContext(Dispatchers.IO) {
                        translationManager.translateToTurkish(synopsis)
                    }
                    if (!tr.isNullOrBlank() && tr != synopsis) {
                        DetailCache.putTranslation("synopsis", entry.source, stableId, tr)
                        _synopsisState.value = SynopsisState.Success(tr)
                        _translatedSynopsis.value = tr
                    }
                }
            }
        }
    }

    /**
     * Lazy-loads tabs data when a specific tab index is selected.
     */
    fun loadTab(tabIndex: Int, entry: MediaEntry, realMalId: Int?) {
        val malId = entry.malId ?: 0
        val tmdbId = entry.tmdbId ?: _detailState.value?.tmdbId ?: _resolvedTmdbId.value
        viewModelScope.launch {
            when (tabIndex) {
                1 -> {
                    val currentSuccess = _charactersState.value as? DetailTabState.Success
                    val needsRefetch = currentSuccess == null ||
                        (currentSuccess.data.isEmpty() && DetailCache.getMediaCharacters(entry.source, malId) == null)
                    if (needsRefetch) {
                        _charactersState.value = DetailTabState.Loading
                        val result = withContext(Dispatchers.IO) {
                            apiClient.fetchCharacters(
                                source = entry.source,
                                externalId = entry.malId,
                                mediaType = entry.type,
                                realMalId = realMalId,
                                tmdbId = tmdbId
                            )
                        }
                        if (result.isNotEmpty()) {
                            DetailCache.putMediaCharacters(entry.source, malId, result)
                        }
                        _charactersState.value = DetailTabState.Success(result)
                    }
                }
                2 -> {
                    val currentSuccess = _staffState.value as? DetailTabState.Success
                    val needsRefetch = currentSuccess == null ||
                        (currentSuccess.data.isEmpty() && DetailCache.getMediaStaff(entry.source, malId) == null)
                    if (needsRefetch) {
                        _staffState.value = DetailTabState.Loading
                        val result = withContext(Dispatchers.IO) {
                            apiClient.fetchStaff(entry.source, entry.malId, entry.type, tmdbId = tmdbId, realMalId = realMalId)
                        }
                        if (result.isNotEmpty()) {
                            DetailCache.putMediaStaff(entry.source, malId, result)
                        }
                        _staffState.value = DetailTabState.Success(result)
                    }
                }
                3 -> {
                    val currentSuccess = _recommendationsState.value as? DetailTabState.Success
                    val needsRefetch = currentSuccess == null ||
                        (currentSuccess.data.isEmpty() && DetailCache.getMediaRecommendations(entry.source, malId) == null)
                    if (needsRefetch) {
                        _recommendationsState.value = DetailTabState.Loading
                        val result = withContext(Dispatchers.IO) {
                            apiClient.fetchRecommendations(entry.source, entry.malId, entry.type, tmdbId = tmdbId, realMalId = realMalId)
                        }
                        if (result.isNotEmpty()) {
                            DetailCache.putMediaRecommendations(entry.source, malId, result)
                        }
                        _recommendationsState.value = DetailTabState.Success(result)
                    }
                }
                4 -> {
                    val currentSuccess = _relationsState.value as? DetailTabState.Success
                    val needsRefetch = currentSuccess == null ||
                        (currentSuccess.data.isEmpty() && DetailCache.getMediaRelations(entry.source, malId) == null)
                    if (needsRefetch) {
                        _relationsState.value = DetailTabState.Loading
                        val result = withContext(Dispatchers.IO) {
                            apiClient.fetchRelations(entry.source, entry.malId, entry.type, tmdbId = tmdbId, realMalId = realMalId)
                        }
                        if (result.isNotEmpty()) {
                            DetailCache.putMediaRelations(entry.source, malId, result)
                        }
                        _relationsState.value = DetailTabState.Success(result)
                    }
                }
                5 -> {
                    if (_statsState.value !is DetailTabState.Success) {
                        _statsState.value = DetailTabState.Loading
                        val result = withContext(Dispatchers.IO) {
                            apiClient.fetchStats(entry.source, entry.malId, entry.type, realMalId = realMalId)
                        }
                        if (result != null) {
                            DetailCache.putMediaStats(entry.source, malId, result)
                            _statsState.value = DetailTabState.Success(result)
                        } else {
                            _statsState.value = DetailTabState.Success(null)
                        }
                    }
                }
                6 -> {
                    val currentSuccess = _reviewsState.value as? DetailTabState.Success
                    val needsRefetch = currentSuccess == null ||
                        (currentSuccess.data.isEmpty() && DetailCache.getMediaReviews(entry.source, malId) == null)
                    if (needsRefetch) {
                        _reviewsState.value = DetailTabState.Loading
                        val result = withContext(Dispatchers.IO) {
                            apiClient.fetchReviews(entry.source, entry.malId, entry.type, tmdbId = tmdbId, realMalId = realMalId)
                        }
                        if (result.isNotEmpty()) {
                            DetailCache.putMediaReviews(entry.source, malId, result)
                        }
                        _reviewsState.value = DetailTabState.Success(result)
                    }
                }
                7 -> {
                    val currentEpisodes = _episodesState.value
                    val needsEpFetch = currentEpisodes !is DetailTabState.Success ||
                        (currentEpisodes is DetailTabState.Success && currentEpisodes.data.isEmpty())
                    if (needsEpFetch) {
                        _episodesState.value = DetailTabState.Loading
                        val result = withContext(Dispatchers.IO) {
                            apiClient.fetchEpisodes(
                                source = entry.source,
                                externalId = entry.malId,
                                mediaType = entry.type,
                                realMalId = realMalId,
                                totalEpisodes = maxOf(entry.total ?: _detailState.value?.total ?: 0, entry.progress),
                                context = context,
                                targetSeason = _targetSeason.value,
                                tmdbId = tmdbId
                            )
                        }
                        if (result.isNotEmpty()) {
                            DetailCache.putMediaEpisodes(entry.source, malId, result)
                        }
                        _episodesState.value = DetailTabState.Success(result)
                    }
                }
            }
        }
    }

    fun deleteMangaMapping(mediaId: Int) {
        viewModelScope.launch {
            mangaRepository.deleteMangaMapping(mediaId)
        }
    }

    fun setTargetSeason(season: Int, entry: MediaEntry) {
        _targetSeason.value = season
        _episodesState.value = DetailTabState.Loading
        DetailCache.removeMediaEpisodes(entry.source, entry.malId ?: 0)
        loadTab(7, entry, _detailState.value?.realMalId)
    }

    /**
     * Fetches MDBList external ratings for a library entry.
     * Resolves IMDb ID via KitsugiIdResolver then calls MdbListClient.
     * Works for AniList, MAL and Kitsu sources.
     */
    private suspend fun fetchMdbListRatingsForEntry(entry: MediaEntry) {
        val context = getApplication<android.app.Application>().applicationContext
        val settingsDataStore = SettingsDataStore(context)
        val settings = runCatching { settingsDataStore.settingsFlow.first() }.getOrNull()
        if (settings == null || !settings.mdbListEnabled || settings.mdbListApiKey.isBlank()) return

        _mdbListLoading.value = true
        try {
            val stableId = entry.malId ?: 0
            val isAniList = entry.source.equals("anilist", ignoreCase = true)

            val malId: Int? = when {
                isAniList && stableId >= 100_000_000 -> null // no real MAL ID from stableId
                isAniList -> stableId
                else -> entry.id
            }
            val aniListId: Int? = if (isAniList && stableId >= 100_000_000) stableId - 100_000_000 else null
            val tmdbId: Int? = entry.tmdbId ?: _detailState.value?.tmdbId ?: _resolvedTmdbId.value

            // Try to resolve IMDb ID from detail links first
            var imdbId: String? = _detailState.value?.externalLinks
                ?.firstOrNull { it.url.contains("imdb.com") }
                ?.url?.substringAfter("/title/")?.substringBefore("/")

            if (imdbId.isNullOrBlank()) {
                val resolved = withContext(Dispatchers.IO) {
                    KitsugiIdResolver.resolveIds(
                        malId = malId,
                        aniListId = aniListId,
                        tmdbId = tmdbId
                    )
                }
                imdbId = resolved.imdbId
            }

            if (!imdbId.isNullOrBlank()) {
                val ratings = withContext(Dispatchers.IO) {
                    MdbListClient.fetchRatings(imdbId, settings.mdbListApiKey)
                }
                _mdbListRatings.value = ratings
            }
        } catch (e: Exception) {
            Log.e(TAG, "MDBList entry rating fetch failed", e)
        } finally {
            _mdbListLoading.value = false
        }
    }
}
