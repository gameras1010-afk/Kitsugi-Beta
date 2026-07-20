package com.kitsugi.animelist.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.TmdbApiClient
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.repository.SearchHistoryRepository

/**
 * Search ViewModel.
 * MoeList SearchViewModel.kt ve AniHyou SearchViewModel.kt'den ilham alınarak
 * Kitsugi'nun JikanApiClient (Jikan + AniList fallback) altyapısına uyarlanmıştır.
 */
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val apiClient = JikanApiClient()
    private val settingsDataStore = SettingsDataStore(application)
    private val database = KitsugiDatabase.getDatabase(application)
    private val searchHistoryRepository = SearchHistoryRepository(database.searchHistoryDao())

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var showAdultContentState = false
    private var searchHistoryEnabledState = true
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { settings ->
                showAdultContentState = settings.showAdultContent
                searchHistoryEnabledState = settings.searchHistoryEnabled
            }
        }
        viewModelScope.launch {
            searchHistoryRepository.getRecentSearchHistory().collect { history ->
                _uiState.update { it.copy(searchHistory = history) }
            }
        }
    }

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun setMediaType(value: MediaType) {
        val currentPlatform = _uiState.value.selectedPlatform
        val targetPlatform = if (value == MediaType.Manga && currentPlatform == SearchPlatform.TMDB) {
            SearchPlatform.All
        } else {
            currentPlatform
        }
        _uiState.update { 
            it.copy(
                selectedMediaType = value,
                selectedPlatform = targetPlatform
            )
        }
        search()
    }

    fun setPlatform(value: SearchPlatform) {
        _uiState.update { it.copy(selectedPlatform = value) }
        search()
    }

    fun setFilterSheetOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isFilterSheetOpen = isOpen) }
    }

    fun updateFilters(filters: SearchFilters) {
        _uiState.update { it.copy(activeFilters = filters) }
        search()
    }

    fun resetFilters() {
        _uiState.update { it.copy(activeFilters = SearchFilters()) }
        search()
    }

    private fun getJikanGenreId(genre: String?): Int? = when (genre?.lowercase()?.trim()) {
        "action", "aksiyon" -> 1
        "adventure", "macera" -> 2
        "comedy", "komedi" -> 4
        "drama", "dram" -> 8
        "fantasy", "fantastik" -> 10
        "horror", "korku" -> 14
        "mystery", "gizem" -> 7
        "romance", "romantizm" -> 22
        "sci-fi" -> 24
        "sports", "spor" -> 30
        "supernatural", "doğaüstü" -> 37
        "suspense", "gerilim" -> 41
        "psychology", "psikoloji" -> 40
        "music", "müzik" -> 19
        "school", "okul" -> 23
        "historical", "tarihi" -> 13
        "mecha" -> 18
        else -> null
    }

    private fun getAniListGenreName(genre: String?): String? = when (genre?.lowercase()?.trim()) {
        "aksiyon" -> "Action"
        "macera" -> "Adventure"
        "komedi" -> "Comedy"
        "dram" -> "Drama"
        "fantastik" -> "Fantasy"
        "korku" -> "Horror"
        "gizem" -> "Mystery"
        "romantizm" -> "Romance"
        "sci-fi" -> "Sci-Fi"
        "spor" -> "Sports"
        "doğaüstü" -> "Supernatural"
        "gerilim" -> "Thriller"
        "psikoloji" -> "Psychological"
        "müzik" -> "Music"
        "okul" -> "School"
        "tarihi" -> "Historical"
        "mecha" -> "Mecha"
        else -> genre
    }

    private fun getAniListGenreNames(genres: List<String>): List<String> =
        genres.mapNotNull { getAniListGenreName(it) }

    fun search() {
        val state = _uiState.value
        if (state.query.isBlank() && state.activeFilters.isDefault()) {
            clearResults()
            return
        }

        searchJob?.cancel()

        val queryNotBlank = state.query.isNotBlank()
        val newHistoryItem = if (queryNotBlank) {
            SearchHistoryItem(
                query = state.query.trim(),
                platform = state.selectedPlatform,
                mediaType = state.selectedMediaType
            )
        } else null

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        searchJob = viewModelScope.launch {
            runCatching {
                if (newHistoryItem != null && searchHistoryEnabledState) {
                    searchHistoryRepository.insertSearchQuery(newHistoryItem)
                }

                val showAdult = showAdultContentState
                val filters = state.activeFilters

                // Jikan filters
                val jikanStatus = when (filters.status) {
                    "AIRING" -> "airing"
                    "PUBLISHING" -> "publishing"
                    "FINISHED" -> "complete"
                    "UPCOMING" -> "upcoming"
                    "HIATUS" -> "hiatus"
                    "DISCONTINUED" -> "discontinued"
                    else -> null
                }
                val jikanFormat = filters.format?.lowercase()
                val jikanGenreId = getJikanGenreId(filters.genres.firstOrNull())
                val jikanSort = when (filters.sort) {
                    "SCORE_DESC" -> "desc"
                    "POPULARITY_DESC" -> "desc"
                    "TITLE_ROMAJI_DESC" -> "desc"
                    "TITLE_ROMAJI_ASC" -> "asc"
                    else -> "desc"
                }
                val jikanOrderBy = when (filters.sort) {
                    "SCORE_DESC" -> "score"
                    "POPULARITY_DESC" -> "popularity"
                    "TITLE_ROMAJI_DESC", "TITLE_ROMAJI_ASC" -> "title"
                    else -> "popularity"
                }

                // AniList filters
                val aniListStatus = when (filters.status) {
                    "AIRING" -> "RELEASING"
                    "PUBLISHING" -> "RELEASING"
                    "FINISHED" -> "FINISHED"
                    "UPCOMING" -> "NOT_YET_RELEASED"
                    "HIATUS" -> "HIATUS"
                    "DISCONTINUED" -> "CANCELLED"
                    else -> null
                }
                val aniListFormat = filters.format
                val aniListGenres = getAniListGenreNames(filters.genres)
                val aniListExcludedGenres = getAniListGenreNames(filters.excludedGenres)
                val aniListTags = filters.tags
                val aniListSort = when (filters.sort) {
                    "SCORE_DESC" -> listOf("SCORE_DESC")
                    "POPULARITY_DESC" -> listOf("POPULARITY_DESC")
                    "TITLE_ROMAJI_DESC" -> listOf("TITLE_DESC")
                    "TITLE_ROMAJI_ASC" -> listOf("TITLE_ASC")
                    else -> listOf("POPULARITY_DESC")
                }

                when (state.selectedPlatform) {
                    SearchPlatform.MAL -> {
                        apiClient.searchMALOnly(
                            query = state.query.trim(),
                            mediaType = state.selectedMediaType,
                            showAdultContent = showAdult,
                            status = jikanStatus,
                            format = jikanFormat,
                            genreId = jikanGenreId,
                            sort = jikanSort,
                            orderBy = jikanOrderBy
                        )
                    }
                    SearchPlatform.AniList -> {
                        apiClient.searchAniList(
                            query = state.query.trim(),
                            mediaType = state.selectedMediaType,
                            showAdultContent = showAdult,
                            status = aniListStatus,
                            format = aniListFormat,
                            season = filters.season,
                            genres = aniListGenres,
                            excludedGenres = aniListExcludedGenres,
                            tags = aniListTags,
                            minYear = filters.minYear,
                            maxYear = filters.maxYear,
                            minScore = filters.minScore,
                            maxScore = filters.maxScore,
                            sort = aniListSort
                        )
                    }
                    SearchPlatform.TMDB -> {
                        if (state.query.isBlank()) emptyList() else TmdbApiClient().search(state.query.trim())
                    }
                    SearchPlatform.All -> {
                        coroutineScope {
                            val malDeferred = async {
                                runCatching {
                                    apiClient.searchMALOnly(
                                        query = state.query.trim(),
                                        mediaType = state.selectedMediaType,
                                        showAdultContent = showAdult,
                                        status = jikanStatus,
                                        format = jikanFormat,
                                        genreId = jikanGenreId,
                                        sort = jikanSort,
                                        orderBy = jikanOrderBy
                                    )
                                }.getOrDefault(emptyList())
                            }
                            val aniListDeferred = async {
                                runCatching {
                                    apiClient.searchAniList(
                                        query = state.query.trim(),
                                        mediaType = state.selectedMediaType,
                                        showAdultContent = showAdult,
                                        status = aniListStatus,
                                        format = aniListFormat,
                                        season = filters.season,
                                        genres = aniListGenres,
                                        excludedGenres = aniListExcludedGenres,
                                        tags = aniListTags,
                                        minYear = filters.minYear,
                                        maxYear = filters.maxYear,
                                        minScore = filters.minScore,
                                        maxScore = filters.maxScore,
                                        sort = aniListSort
                                    )
                                }.getOrDefault(emptyList())
                            }
                            val tmdbDeferred = async {
                                if (state.selectedMediaType == MediaType.Manga || state.query.isBlank()) {
                                    emptyList()
                                } else {
                                    runCatching {
                                        TmdbApiClient().search(state.query.trim())
                                    }.getOrDefault(emptyList())
                                }
                            }
                            val mal = malDeferred.await()
                            val aniList = aniListDeferred.await()
                            val tmdb = tmdbDeferred.await()

                            val combined = mutableListOf<JikanSearchResult>()
                            val maxLen = maxOf(mal.size, aniList.size, tmdb.size)
                            for (i in 0 until maxLen) {
                                if (i < mal.size) combined.add(mal[i])
                                if (i < aniList.size) combined.add(aniList[i])
                                if (i < tmdb.size) combined.add(tmdb[i])
                            }

                            // Akıllı Göreceli Arama & Tekilleştirme (Deduplication)
                            val seenKeys = mutableSetOf<String>()
                            val uniqueResults = mutableListOf<JikanSearchResult>()

                            for (result in combined) {
                                val keys = mutableListOf<String>()

                                // 1. ID tabanlı anahtarlar
                                val malIdKey = if (result.source == "jikan" || result.source == "mal") result.malId else result.realMalId
                                if (malIdKey != null && malIdKey > 0) {
                                    keys.add("mal:$malIdKey")
                                } else if (result.source == "tmdb" && result.tmdbId != null) {
                                    keys.add("tmdb:${result.tmdbId}")
                                } else {
                                    keys.add("${result.source}:${result.malId}")
                                }

                                // 2. Göreceli İsim/Yıl/Tip tabanlı anahtarlar (Alias ve dil toleranslı)
                                val typeKey = when (result.type) {
                                    MediaType.Anime, MediaType.TvShow -> "show"
                                    MediaType.Movie -> "movie"
                                    MediaType.Manga -> "manga"
                                }
                                val yearVal = result.year
                                val titles = listOfNotNull(result.title, result.titleEnglish, result.titleJapanese)
                                for (t in titles) {
                                    val norm = t.lowercase().replace(Regex("[^a-z0-9]"), "").trim()
                                    if (norm.isNotEmpty()) {
                                        if (yearVal != null) {
                                            keys.add("title:$norm:$typeKey:$yearVal")
                                            keys.add("title:$norm:$typeKey:${yearVal - 1}")
                                            keys.add("title:$norm:$typeKey:${yearVal + 1}")
                                        } else {
                                            keys.add("title:$norm:$typeKey:any")
                                        }
                                    }
                                }

                                // Eğer bu anahtarlardan herhangi biri daha önce görüldüyse mükerrer say ve atla
                                val isDuplicate = keys.any { seenKeys.contains(it) }
                                if (!isDuplicate) {
                                    uniqueResults.add(result)
                                    seenKeys.addAll(keys)
                                }
                            }
                            uniqueResults
                        }
                    }
                }
            }.onSuccess { results ->
                _uiState.update {
                    it.copy(
                        results = results,
                        isLoading = false,
                        hasSearched = true,
                        errorMessage = if (results.isEmpty()) "Sonuç bulunamadı." else null
                    )
                }
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) {
                    return@onFailure
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasSearched = true,
                        errorMessage = error.message ?: "Arama sırasında bir hata oluştu."
                    )
                }
            }
        }
    }


    fun clearHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearSearchHistory()
        }
    }

    fun removeHistoryItem(item: SearchHistoryItem) {
        viewModelScope.launch {
            searchHistoryRepository.deleteSearchQuery(item.query)
        }
    }

    fun applyHistoryItem(item: SearchHistoryItem) {
        _uiState.update {
            it.copy(
                query = item.query
            )
        }
        search()
    }

    fun clearResults() {
        _uiState.update { it.copy(results = emptyList(), hasSearched = false, errorMessage = null) }
    }

    /**
     * Arama çubuğundaki X butonuna basıldığında çağrılır.
     * Hem sorgu metnini hem arama sonuçlarını sıfırlar → geçmiş görünümüne dönüş.
     */
    fun clearQuery() {
        _uiState.update {
            it.copy(
                query = "",
                results = emptyList(),
                hasSearched = false,
                errorMessage = null
            )
        }
    }
}
