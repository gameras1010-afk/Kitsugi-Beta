package com.kitsugi.animelist.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.TmdbApiClient
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.screens.search.composables.KitsugiCountryOfOrigin
import com.kitsugi.animelist.ui.screens.search.composables.KitsugiMediaFormat
import com.kitsugi.animelist.ui.screens.search.composables.KitsugiMediaSeason
import com.kitsugi.animelist.ui.screens.search.composables.KitsugiMediaSortSearch
import com.kitsugi.animelist.ui.screens.search.composables.KitsugiMediaSource
import com.kitsugi.animelist.ui.screens.search.composables.KitsugiMediaStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
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

    fun setPlatformAndMediaType(platform: SearchPlatform, mediaType: MediaType) {
        _uiState.update {
            it.copy(
                selectedPlatform = platform,
                selectedMediaType = mediaType
            )
        }
        search()
    }

    fun setFilterSheetOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isFilterSheetOpen = isOpen) }
    }

    // ── Legacy updateFilters (used by GenresTagsSheet & SearchFilterSheet) ──
    fun updateFilters(filters: SearchFilters) {
        _uiState.update {
            it.copy(
                genres = filters.genres,
                excludedGenres = filters.excludedGenres,
                tags = filters.tags,
                startYear = filters.minYear,
                endYear = filters.maxYear,
                season = filters.season?.let { s -> KitsugiMediaSeason.entries.find { e -> e.apiValue == s } },
                minScore = filters.minScore,
                maxScore = filters.maxScore
            )
        }
        search()
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                genres = emptyList(),
                excludedGenres = emptyList(),
                tags = emptyList(),
                selectedFormats = emptyList(),
                selectedStatuses = emptyList(),
                country = null,
                selectedSources = emptyList(),
                startYear = null,
                endYear = null,
                season = null,
                minScore = null,
                maxScore = null,
                minEpCh = null,
                maxEpCh = null,
                minDuration = null,
                maxDuration = null,
                sortSearch = KitsugiMediaSortSearch.SEARCH_MATCH,
                isSortDescending = true
            )
        }
        search()
    }

    // ── AniHyou-style reactive filter events ──────────────────────────────

    fun setFormats(values: List<KitsugiMediaFormat>) {
        _uiState.update { it.copy(selectedFormats = values) }
        search()
    }

    fun setStatuses(values: List<KitsugiMediaStatus>) {
        _uiState.update { it.copy(selectedStatuses = values) }
        search()
    }

    fun setCountry(value: KitsugiCountryOfOrigin?) {
        _uiState.update { it.copy(country = value) }
        search()
    }

    fun setSources(values: List<KitsugiMediaSource>) {
        _uiState.update { it.copy(selectedSources = values) }
        search()
    }

    fun setStartYear(value: Int?) {
        _uiState.update { it.copy(startYear = value) }
        search()
    }

    fun setEndYear(value: Int?) {
        _uiState.update { it.copy(endYear = value) }
        search()
    }

    fun setSeason(value: KitsugiMediaSeason?) {
        _uiState.update { it.copy(season = value) }
        search()
    }

    fun setMinScore(value: Int?) {
        _uiState.update { it.copy(minScore = value) }
        search()
    }

    fun setMaxScore(value: Int?) {
        _uiState.update { it.copy(maxScore = value) }
        search()
    }

    fun setEpCh(range: IntRange?) {
        _uiState.update { it.copy(minEpCh = range?.first, maxEpCh = range?.last) }
        search()
    }

    fun setDuration(range: IntRange?) {
        _uiState.update { it.copy(minDuration = range?.first, maxDuration = range?.last) }
        search()
    }

    fun setSort(sortSearch: KitsugiMediaSortSearch, isDescending: Boolean) {
        _uiState.update { it.copy(sortSearch = sortSearch, isSortDescending = isDescending) }
        search()
    }

    /**
     * Profil / detay sayfasından bir tür (genre) adına tıklandığında çağrılır.
     * [genreEnglish] her zaman İngilizce API adı olmalı (Kitsugi translation map zaten dönüştürür).
     */
    fun setGenreFilter(genreEnglish: String) {
        val cleanGenre = translateToEnglishForSearch(genreEnglish)
        val isAniListGenre = isOfficialAniListGenre(cleanGenre)
        _uiState.update {
            it.copy(
                query = "",
                genres = if (isAniListGenre) listOf(cleanGenre) else emptyList(),
                excludedGenres = emptyList(),
                tags = if (!isAniListGenre) listOf(cleanGenre) else emptyList(),
                selectedFormats = emptyList(),
                selectedStatuses = emptyList(),
                country = null,
                selectedSources = emptyList(),
                startYear = null,
                endYear = null,
                season = null,
                minScore = null,
                maxScore = null,
                minEpCh = null,
                maxEpCh = null,
                minDuration = null,
                maxDuration = null,
                sortSearch = KitsugiMediaSortSearch.SEARCH_MATCH,
                isSortDescending = true,
                hasSearched = false
            )
        }
        search()
    }

    /**
     * Profil / detay sayfasından bir etikete (tag) tıklandığında çağrılır.
     */
    fun setTagFilter(tag: String) {
        val cleanTag = translateToEnglishForSearch(tag)
        _uiState.update {
            it.copy(
                query = "",
                genres = emptyList(),
                excludedGenres = emptyList(),
                tags = listOf(cleanTag),
                selectedFormats = emptyList(),
                selectedStatuses = emptyList(),
                country = null,
                selectedSources = emptyList(),
                startYear = null,
                endYear = null,
                season = null,
                minScore = null,
                maxScore = null,
                minEpCh = null,
                maxEpCh = null,
                minDuration = null,
                maxDuration = null,
                sortSearch = KitsugiMediaSortSearch.SEARCH_MATCH,
                isSortDescending = true,
                hasSearched = false
            )
        }
        search()
    }

    private fun isOfficialAniListGenre(genre: String): Boolean {
        val officialGenres = listOf(
            "Action", "Adventure", "Comedy", "Drama", "Ecchi", "Fantasy",
            "Hentai", "Horror", "Mahou Shoujo", "Mecha", "Music", "Mystery",
            "Psychological", "Romance", "Sci-Fi", "Slice of Life", "Sports",
            "Supernatural", "Thriller"
        )
        return officialGenres.any { it.equals(genre.trim(), ignoreCase = true) }
    }


    private fun translateToEnglishForSearch(label: String): String =
        SearchTranslation.translateToEnglishForSearch(label)


    private fun getJikanGenreId(genreOrTag: String?): Int? {
        // Önce Türkçe ise İngilizce'ye çevir, sonra ID'yi bul
        val eng = if (genreOrTag != null) SearchTranslation.translateToEnglishForSearch(genreOrTag) else return null
        return when (eng.lowercase().trim()) {
            "action"           -> 1
            "adventure"        -> 2
            "racing"           -> 3
            "comedy"           -> 4
            "avant garde"      -> 5
            "mythology"        -> 6
            "mystery"          -> 7
            "drama"            -> 8
            "ecchi"            -> 9
            "fantasy"          -> 10
            "magic"            -> 10
            "strategy game"    -> 11
            "hentai"           -> 12
            "historical"       -> 13
            "horror"           -> 14
            "kids"             -> 15
            "martial arts"     -> 17
            "mecha"            -> 18
            "music"            -> 19
            "parody"           -> 20
            "samurai"          -> 21
            "romance"          -> 22
            "school"           -> 23
            "sci-fi"           -> 24
            "cyberpunk"        -> 24
            "shoujo ai"        -> 25
            "shounen ai"       -> 26
            "space"            -> 27
            "space opera"      -> 27
            "sports"           -> 30
            "super power"      -> 31
            "superhero"        -> 31
            "vampire"          -> 32
            "harem"            -> 33
            "slice of life"    -> 36
            "iyashikei"        -> 36
            "supernatural"     -> 37
            "youkai"           -> 37
            "military"         -> 38
            "detective"        -> 39
            "psychological"    -> 40
            "suspense","thriller" -> 41
            "seinen"           -> 42
            "josei"            -> 43
            "gourmet"          -> 47
            "workplace", "work" -> 48
            "adult cast"       -> 50
            "cgdct"            -> 52
            "cute girls doing cute things" -> 52
            "childcare"        -> 53
            "combat sports"    -> 54
            "delinquents"      -> 56
            "educational"      -> 57
            "gag humor"        -> 58
            "surreal comedy"   -> 58
            "gore"             -> 59
            "body horror"      -> 59
            "high stakes game", "death game" -> 60
            "idols"            -> 61
            "isekai"           -> 62
            "love polygon", "love triangle" -> 64
            "medicine", "medical" -> 66
            "organized crime", "mafia", "yakuza", "criminal organization" -> 67
            "otaku culture"    -> 68
            "performing arts", "showbiz" -> 69
            "pets", "animals"  -> 70
            "reincarnation"    -> 71
            "reverse harem"    -> 72
            "survival"         -> 75
            "post-apocalyptic" -> 75
            "time travel", "time loop", "time manipulation" -> 77
            "video games", "video game", "e-sports" -> 79
            "visual arts", "photography", "drawing" -> 80
            "boys' love", "boys love" -> 26
            "yuri"             -> 25
            "shounen"          -> 27
            "shoujo"           -> 25
            "seinen"           -> 42
            "josei"            -> 43
            else               -> null
        }
    }

    private fun getTmdbGenreId(genreOrTag: String?, isMovie: Boolean): Int? {
        val eng = if (genreOrTag != null) SearchTranslation.translateToEnglishForSearch(genreOrTag) else return null
        return when (eng.lowercase().trim()) {
            "action", "battle royale", "martial arts", "superhero" -> if (isMovie) 28 else 10759
            "adventure", "isekai", "survival", "post-apocalyptic" -> if (isMovie) 12 else 10759
            "comedy", "parody", "gag humor", "surreal comedy", "slapstick" -> 35
            "drama", "tragedy", "coming of age", "romance" -> 18
            "fantasy", "magic", "supernatural", "alchemy", "youkai", "mythology" -> if (isMovie) 14 else 10765
            "horror", "gore", "body horror", "cosmic horror" -> 27
            "mystery", "detective", "conspiracy", "noir" -> 9648
            "sci-fi", "cyberpunk", "space opera", "time travel", "time loop" -> if (isMovie) 878 else 10765
            "thriller", "suspense", "psychological", "espionage", "terrorism" -> 53
            "music", "band", "dancing", "musical theater" -> 10402
            "historical", "medieval", "ancient china", "samurai", "vikings" -> 36
            "crime", "mafia", "yakuza", "organized crime", "gangs" -> 80
            "family", "childcare", "parenthood" -> 10751
            "military", "war", "guns" -> if (isMovie) 10752 else 10768
            "animation", "anime" -> 16
            "documentary", "educational", "biographical" -> 99
            "western" -> if (isMovie) 37 else null
            "kids", "cgdct" -> 10762
            else -> null
        }
    }

    // AniList'e gönderilecek tür adını normalize eder (Türkçe → İngilizce)
    private fun getAniListGenreName(genre: String?): String? {
        if (genre == null) return null
        return SearchTranslation.translateToEnglishForSearch(genre)
    }

    private fun getAniListGenreNames(genres: List<String>): List<String> =
        genres.mapNotNull { getAniListGenreName(it) }

    private fun cleanSearchQuery(query: String): String {
        return query
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace(Regex("([a-zA-Z])([0-9])"), "$1 $2")
            .replace(Regex("([0-9])([a-zA-Z])"), "$1 $2")
            .replace("-", " ")
            .replace("_", " ")
            .replace(".", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun generateFallbackQueries(query: String): List<String> {
        val list = mutableListOf<String>()
        val raw = query.trim()
        if (raw.isBlank()) return list

        val cleaned = cleanSearchQuery(raw)
        if (cleaned.isNotBlank() && cleaned != raw) {
            list.add(cleaned)
        }

        // Vowel typo correction (e.g. shorlock -> sherlock)
        val lower = raw.lowercase()
        if (lower.contains("shorl")) list.add(raw.replace(Regex("shorl", RegexOption.IGNORE_CASE), "sherl"))
        if (lower.contains("attak")) list.add(raw.replace(Regex("attak", RegexOption.IGNORE_CASE), "attack"))
        if (lower.contains("demn")) list.add(raw.replace(Regex("demn", RegexOption.IGNORE_CASE), "demon"))

        return list.distinct()
    }

    fun search() {
        val state = _uiState.value
        if (state.query.isBlank() && !state.hasFiltersApplied) {
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
            try {
                val rawQuery = state.query.trim()
                val fallbackCandidates = generateFallbackQueries(rawQuery)

                var results = executeSearchForQuery(rawQuery)
                if (results.isEmpty()) {
                    for (fallback in fallbackCandidates) {
                        if (fallback != rawQuery) {
                            results = executeSearchForQuery(fallback)
                            if (results.isNotEmpty()) break
                        }
                    }
                }

                ensureActive()

                if (newHistoryItem != null && searchHistoryEnabledState && results.isNotEmpty()) {
                    searchHistoryRepository.insertSearchQuery(newHistoryItem)
                }

                ensureActive()

                _uiState.update {
                    it.copy(
                        results = results,
                        isLoading = false,
                        hasSearched = true,
                        errorMessage = if (results.isEmpty()) "Sonuç bulunamadı." else null
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Arama sırasında bir hata oluştu."
                    )
                }
            }
        }
    }

    private suspend fun executeSearchForQuery(queryText: String): List<JikanSearchResult> {
        val state = _uiState.value
        val showAdult = showAdultContentState

        // ── Derive filter values directly from state ──────────────────────
        val firstStatus = state.selectedStatuses.firstOrNull()?.apiValue
        val firstFormat = state.selectedFormats.firstOrNull()?.apiValue

        // Jikan filters
        val jikanStatus = when (firstStatus) {
            "RELEASING" -> "airing"
            "FINISHED" -> "complete"
            "NOT_YET_RELEASED" -> "upcoming"
            "HIATUS" -> "hiatus"
            "CANCELLED" -> "discontinued"
            else -> null
        }
        val jikanFormat = firstFormat?.lowercase()
        val jikanGenreId = getJikanGenreId(state.genres.firstOrNull() ?: state.tags.firstOrNull())
        val effectiveSort = state.effectiveSortApiValue
        val jikanSort = if (effectiveSort.endsWith("_DESC") || effectiveSort == "POPULARITY_DESC") "desc" else "asc"
        val jikanOrderBy = when {
            effectiveSort.startsWith("SCORE") -> "score"
            effectiveSort.startsWith("TITLE") -> "title"
            else -> "popularity"
        }

        // AniList filters
        val aniListStatus = firstStatus
        val aniListFormat = firstFormat
        val aniListGenres = getAniListGenreNames(state.genres.filter { isOfficialAniListGenre(it) })
        val aniListExcludedGenres = getAniListGenreNames(state.excludedGenres.filter { isOfficialAniListGenre(it) })
        val aniListTags = state.tags.toMutableList().apply {
            addAll(state.genres.filter { !isOfficialAniListGenre(it) })
        }.distinct()
        val aniListSort = when {
            effectiveSort == "SEARCH_MATCH" && queryText.isNotBlank() -> emptyList()
            effectiveSort == "SEARCH_MATCH" -> listOf("POPULARITY_DESC")
            else -> listOf(effectiveSort)
        }
        val aniListCountry = state.country?.code
        val aniListSources = state.selectedSources.map { it.apiValue }

        val fallbacks = generateFallbackQueries(queryText)

        return runCatching {
            when (state.selectedPlatform) {
                SearchPlatform.MAL -> {
                    var res = apiClient.searchMALOnly(
                        query = queryText,
                        mediaType = state.selectedMediaType,
                        showAdultContent = showAdult,
                        status = jikanStatus,
                        format = jikanFormat,
                        genreId = jikanGenreId,
                        sort = jikanSort,
                        orderBy = jikanOrderBy
                    )
                    if (res.isEmpty()) {
                        for (fb in fallbacks) {
                            res = apiClient.searchMALOnly(
                                query = fb, mediaType = state.selectedMediaType,
                                showAdultContent = showAdult, status = jikanStatus,
                                format = jikanFormat, genreId = jikanGenreId,
                                sort = jikanSort, orderBy = jikanOrderBy
                            )
                            if (res.isNotEmpty()) break
                        }
                    }
                    res
                }
                SearchPlatform.AniList -> {
                    var res = apiClient.searchAniList(
                        query = queryText,
                        mediaType = state.selectedMediaType,
                        showAdultContent = showAdult,
                        status = aniListStatus,
                        format = aniListFormat,
                        season = state.season?.apiValue,
                        genres = aniListGenres,
                        excludedGenres = aniListExcludedGenres,
                        tags = aniListTags,
                        minYear = state.startYear,
                        maxYear = state.endYear,
                        minScore = state.minScore,
                        maxScore = state.maxScore,
                        sort = aniListSort,
                        country = aniListCountry,
                        sources = aniListSources
                    )
                    if (res.isEmpty()) {
                        for (fb in fallbacks) {
                            res = apiClient.searchAniList(
                                query = fb, mediaType = state.selectedMediaType,
                                showAdultContent = showAdult, status = aniListStatus,
                                format = aniListFormat, season = state.season?.apiValue,
                                genres = aniListGenres, excludedGenres = aniListExcludedGenres,
                                tags = aniListTags, minYear = state.startYear,
                                maxYear = state.endYear, minScore = state.minScore,
                                maxScore = state.maxScore, sort = aniListSort,
                                country = aniListCountry, sources = aniListSources
                            )
                            if (res.isNotEmpty()) break
                        }
                    }
                    res
                }
                SearchPlatform.TMDB -> {
                    val tmdbGenreId = getTmdbGenreId(state.genres.firstOrNull() ?: state.tags.firstOrNull(), state.selectedMediaType == MediaType.Movie)
                    if (queryText.isBlank() && tmdbGenreId != null) {
                        TmdbApiClient().discoverByGenre(tmdbGenreId, state.selectedMediaType == MediaType.Movie)
                    } else if (queryText.isNotBlank()) {
                        var res = TmdbApiClient().search(queryText)
                        if (res.isEmpty()) {
                            for (fb in fallbacks) {
                                res = TmdbApiClient().search(fb)
                                if (res.isNotEmpty()) break
                            }
                        }
                        res
                    } else {
                        emptyList()
                    }
                }
                SearchPlatform.All -> {
                    coroutineScope {
                        val malDeferred = async {
                            runCatching {
                                var res = apiClient.searchMALOnly(
                                    query = queryText,
                                    mediaType = state.selectedMediaType,
                                    showAdultContent = showAdult,
                                    status = jikanStatus,
                                    format = jikanFormat,
                                    genreId = jikanGenreId,
                                    sort = jikanSort,
                                    orderBy = jikanOrderBy
                                )
                                if (res.isEmpty()) {
                                    for (fb in fallbacks) {
                                        res = apiClient.searchMALOnly(
                                            query = fb, mediaType = state.selectedMediaType,
                                            showAdultContent = showAdult, status = jikanStatus,
                                            format = jikanFormat, genreId = jikanGenreId,
                                            sort = jikanSort, orderBy = jikanOrderBy
                                        )
                                        if (res.isNotEmpty()) break
                                    }
                                }
                                res
                            }.getOrElse { e ->
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                emptyList()
                            }
                        }
                        val aniListDeferred = async {
                            runCatching {
                                var res = apiClient.searchAniList(
                                    query = queryText,
                                    mediaType = state.selectedMediaType,
                                    showAdultContent = showAdult,
                                    status = aniListStatus,
                                    format = aniListFormat,
                                    season = state.season?.apiValue,
                                    genres = aniListGenres,
                                    excludedGenres = aniListExcludedGenres,
                                    tags = aniListTags,
                                    minYear = state.startYear,
                                    maxYear = state.endYear,
                                    minScore = state.minScore,
                                    maxScore = state.maxScore,
                                    sort = aniListSort,
                                    country = aniListCountry,
                                    sources = aniListSources
                                )
                                if (res.isEmpty()) {
                                    for (fb in fallbacks) {
                                        res = apiClient.searchAniList(
                                            query = fb, mediaType = state.selectedMediaType,
                                            showAdultContent = showAdult, status = aniListStatus,
                                            format = aniListFormat, season = state.season?.apiValue,
                                            genres = aniListGenres, excludedGenres = aniListExcludedGenres,
                                            tags = aniListTags, minYear = state.startYear,
                                            maxYear = state.endYear, minScore = state.minScore,
                                            maxScore = state.maxScore, sort = aniListSort,
                                            country = aniListCountry, sources = aniListSources
                                        )
                                        if (res.isNotEmpty()) break
                                    }
                                }
                                res
                            }.getOrElse { e ->
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                emptyList()
                            }
                        }
                        val tmdbDeferred = async {
                            if (state.selectedMediaType == MediaType.Manga || state.selectedMediaType == MediaType.Anime) {
                                emptyList()
                            } else {
                                val tmdbGenreId = getTmdbGenreId(state.genres.firstOrNull() ?: state.tags.firstOrNull(), state.selectedMediaType == MediaType.Movie)
                                if (queryText.isBlank() && tmdbGenreId != null) {
                                    runCatching {
                                        TmdbApiClient().discoverByGenre(tmdbGenreId, state.selectedMediaType == MediaType.Movie)
                                    }.getOrElse { e ->
                                        if (e is kotlinx.coroutines.CancellationException) throw e
                                        emptyList()
                                    }
                                } else if (queryText.isNotBlank()) {
                                    runCatching {
                                        var res = TmdbApiClient().search(queryText)
                                        if (res.isEmpty()) {
                                            for (fb in fallbacks) {
                                                res = TmdbApiClient().search(fb)
                                                if (res.isNotEmpty()) break
                                            }
                                        }
                                        res
                                    }.getOrElse { e ->
                                        if (e is kotlinx.coroutines.CancellationException) throw e
                                        emptyList()
                                    }
                                } else {
                                    emptyList()
                                }
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

                        val seenKeys = mutableSetOf<String>()
                        val uniqueResults = mutableListOf<JikanSearchResult>()

                        for (result in combined) {
                            val itemKey = "${result.source.lowercase()}:${result.tmdbId ?: result.malId}"
                            if (!seenKeys.contains(itemKey)) {
                                seenKeys.add(itemKey)
                                uniqueResults.add(result)
                            }
                        }
                        uniqueResults
                    }
                }
                SearchPlatform.CS3 -> {
                    val targetApiName = state.selectedPluginApiName
                    val rawResults = if (targetApiName != null) {
                        // Seçili tek eklentide ara
                        val api = com.lagradost.cloudstream3.APIHolder.allProviders
                            .firstOrNull { it.name == targetApiName }
                        if (api != null) {
                            com.kitsugi.animelist.data.cloudstream.CsStreamRunner.safeSearch(api, queryText)
                                .map { api to it }
                        } else {
                            com.kitsugi.animelist.data.cloudstream.CsStreamRunner.searchAllAddons(getApplication(), queryText)
                        }
                    } else {
                        // Tüm aktif eklentilerde ara
                        com.kitsugi.animelist.data.cloudstream.CsStreamRunner.searchAllAddons(getApplication(), queryText)
                    }
                    rawResults.map { (api, response) ->
                        JikanSearchResult(
                            malId = response.url.hashCode(),
                            title = response.name,
                            subtitle = api.name,
                            type = MediaType.Anime,
                            total = null,
                            score = null,
                            isAdult = false,
                            imageUrl = response.posterUrl,
                            year = null,
                            source = "cs3",
                            cs3Url = response.url,
                            cs3ApiName = api.name
                        )
                    }
                }
            }
        }.getOrElse { e ->
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
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

    /**
     * Bir eklenti seçildiğinde çağrılır.
     * [apiName] null ise ve [keepPlatformCs3] true ise, platform CS3'te (Eklentiler) kalır ama tüm eklentiler aranır.
     * [apiName] null ise ve [keepPlatformCs3] false ise, platform All (Tümü) olur.
     */
    fun setSelectedPlugin(apiName: String?, keepPlatformCs3: Boolean = false) {
        _uiState.update {
            it.copy(
                selectedPluginApiName = apiName,
                selectedPlatform = if (keepPlatformCs3 || apiName != null) SearchPlatform.CS3 else SearchPlatform.All,
                // Arama geçmişini temizle, boş sayfa göster
                query = "",
                results = emptyList(),
                hasSearched = false,
                errorMessage = null
            )
        }
    }
}
