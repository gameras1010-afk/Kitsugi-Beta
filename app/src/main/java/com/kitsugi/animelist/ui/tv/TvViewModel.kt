package com.kitsugi.animelist.ui.tv

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.TmdbApiClient
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.tv.home.HomeScreenFocusState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class TvHomeState(
    val isLoading: Boolean = true,
    val heroItems: List<JikanSearchResult> = emptyList(),
    val trendingAnime: List<JikanSearchResult> = emptyList(),
    val airingAnime: List<JikanSearchResult> = emptyList(),
    val movieAnime: List<JikanSearchResult> = emptyList(),
    val topAnime: List<JikanSearchResult> = emptyList(),
    val errorMessage: String? = null
)

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val jikanClient = JikanApiClient(
        aniListToken = ExternalAuthManager.getAniListToken(application)
    )
    private val tmdbClient = TmdbApiClient()

    private val _homeState = MutableStateFlow(TvHomeState())
    val homeState: StateFlow<TvHomeState> = _homeState.asStateFlow()

    private val _focusState = MutableStateFlow(HomeScreenFocusState())
    val focusState: StateFlow<HomeScreenFocusState> = _focusState.asStateFlow()

    private val _enrichingItemId = MutableStateFlow<Int?>(null)
    val enrichingItemId: StateFlow<Int?> = _enrichingItemId.asStateFlow()

    private val _enrichedItems = MutableStateFlow<Map<Int, JikanSearchResult>>(emptyMap())
    val enrichedItems: StateFlow<Map<Int, JikanSearchResult>> = _enrichedItems.asStateFlow()

    private val _trailerUrls = MutableStateFlow<Map<Int, String>>(emptyMap())
    val trailerUrls: StateFlow<Map<Int, String>> = _trailerUrls.asStateFlow()

    private var loadJob: Job? = null
    private var enrichmentJob: Job? = null

    init {
        loadHomeData()
    }

    fun loadHomeData(forceRefresh: Boolean = false) {
        if (!forceRefresh && !_homeState.value.isLoading && _homeState.value.heroItems.isNotEmpty()) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _homeState.value = _homeState.value.copy(isLoading = true, errorMessage = null)
            try {
                val data = fetchTvHomeData()
                _homeState.value = data.copy(isLoading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _homeState.value = _homeState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Veriler yüklenemedi"
                )
            }
        }
    }

    private suspend fun fetchTvHomeData(): TvHomeState = supervisorScope {
        // Paralel olarak tüm kategorileri çek (Jikan)
        val trendingDeferred  = async { runCatching { jikanClient.trendingAnime() }.getOrDefault(emptyList()) }
        val airingDeferred    = async { runCatching { jikanClient.airingAnime() }.getOrDefault(emptyList()) }
        val moviesDeferred    = async { runCatching { jikanClient.movieAnime() }.getOrDefault(emptyList()) }
        val topDeferred       = async { runCatching { jikanClient.topAnime() }.getOrDefault(emptyList()) }

        var trending = trendingDeferred.await()
        var airing   = airingDeferred.await()
        var movies   = moviesDeferred.await()
        var top      = topDeferred.await()

        // Jikan API rate limit veya ağ engeli nedeniyle tamamen boş döndüyse AniList'i dene!
        if (trending.isEmpty() && airing.isEmpty() && movies.isEmpty() && top.isEmpty()) {
            val alTrendingDeferred = async { runCatching { jikanClient.aniListTrendingAnime() }.getOrDefault(emptyList()) }
            val alAiringDeferred   = async { runCatching { jikanClient.aniListAiringAnime() }.getOrDefault(emptyList()) }
            val alMoviesDeferred   = async { runCatching { jikanClient.aniListMovieAnime() }.getOrDefault(emptyList()) }
            val alTopDeferred      = async { runCatching { jikanClient.aniListTopAnime() }.getOrDefault(emptyList()) }

            trending = alTrendingDeferred.await()
            airing   = alAiringDeferred.await()
            movies   = alMoviesDeferred.await()
            top      = alTopDeferred.await()
        }

        // Eğer her iki API de başarısız olduysa siyah ekran kalmaması için hata fırlat
        if (trending.isEmpty() && airing.isEmpty() && movies.isEmpty() && top.isEmpty()) {
            throw IllegalStateException("Sunucu bağlantısı kurulamadı. Lütfen internet bağlantınızı kontrol edip tekrar deneyin.")
        }

        // Hero listesi için backdrop zenginleştirmesi (ilk 8 öğe)
        val heroRaw = (trending + top).distinctBy { it.malId }.take(8)
        val heroEnriched = heroRaw.map { item ->
            async {
                runCatching {
                    val backdrop = tmdbClient.fetchBackdropByTitle(item.title)
                    if (backdrop != null) item.copy(backdropUrl = backdrop) else item
                }.getOrDefault(item)
            }
        }.map { it.await() }

        TvHomeState(
            isLoading       = false,
            heroItems       = heroEnriched,
            trendingAnime   = trending,
            airingAnime     = airing,
            movieAnime      = movies,
            topAnime        = top
        )
    }

    fun saveFocusState(
        verticalScrollIndex: Int,
        verticalScrollOffset: Int,
        focusedRowKey: String?,
        focusedItemKeyByRow: Map<String, String>,
        catalogRowScrollStates: Map<String, Int>,
        focusedRowIndex: Int,
        focusedItemIndex: Int
    ) {
        _focusState.value = HomeScreenFocusState(
            verticalScrollIndex = verticalScrollIndex,
            verticalScrollOffset = verticalScrollOffset,
            focusedRowKey = focusedRowKey,
            focusedItemKeyByRow = focusedItemKeyByRow,
            focusedRowIndex = focusedRowIndex,
            focusedItemIndex = focusedItemIndex,
            catalogRowScrollStates = catalogRowScrollStates,
            hasSavedFocus = true
        )
    }

    fun onItemFocus(item: JikanSearchResult) {
        val malId = item.malId
        _enrichingItemId.value = malId

        enrichmentJob?.cancel()
        enrichmentJob = viewModelScope.launch {
            // 1. TMDB'den yüksek kaliteli backdropUrl çekip zenginleştir
            if (!_enrichedItems.value.containsKey(malId) && item.backdropUrl.isNullOrBlank()) {
                val backdrop = runCatching { tmdbClient.fetchBackdropByTitle(item.title) }.getOrNull()
                if (backdrop != null) {
                    _enrichedItems.value = _enrichedItems.value + (malId to item.copy(backdropUrl = backdrop))
                }
            }

            // 2. Fragman URL'sini detaylardan veya TMDB'den çek
            if (!_trailerUrls.value.containsKey(malId)) {
                val detail = runCatching { jikanClient.fetchDetail(item.source, malId, item.type) }.getOrNull()
                val trailerUrl = detail?.trailerUrl
                if (!trailerUrl.isNullOrBlank()) {
                    _trailerUrls.value = _trailerUrls.value + (malId to trailerUrl)
                } else {
                    // TMDB'den video ara
                    val (tmdbTrailer, _) = runCatching { tmdbClient.fetchVideos(malId, item.type == MediaType.Movie) }.getOrDefault(Pair(null, emptyList()))
                    if (tmdbTrailer != null) {
                        _trailerUrls.value = _trailerUrls.value + (malId to tmdbTrailer)
                    }
                }
            }

            if (_enrichingItemId.value == malId) {
                _enrichingItemId.value = null
            }
        }
    }
}
