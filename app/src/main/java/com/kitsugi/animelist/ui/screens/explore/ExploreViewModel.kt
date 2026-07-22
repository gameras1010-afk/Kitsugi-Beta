package com.kitsugi.animelist.ui.screens.explore

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import com.kitsugi.animelist.data.auth.SimklSyncManager
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
// SimklApiClient: discovery için artık kullanılmıyor; sadece SimklSyncManager üzerinden watchlist sync'te kullanılır
import com.kitsugi.animelist.data.remote.TmdbApiClient
import com.kitsugi.animelist.data.settings.SettingsDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import com.kitsugi.animelist.model.MediaType

// Keşfet sayfasında seçili platform: MAL (Jikan), AniList veya TMDB
enum class ExplorePlatform(val label: String) {
    MAL("MAL"),
    AniList("AniList"),
    TMDB("TMDB")
}

class ExploreViewModel(application: Application) : AndroidViewModel(application) {

    private val apiClient = JikanApiClient(
        aniListToken = ExternalAuthManager.getAniListToken(application)
    )
    // simklApiClient: discovery/trending için KALDIRILDI — sadece SimklSyncManager üzerinden watchlist sync
    // tmdbApiClient: settings'ten tmdbUserApiKey yüklenince rebuild edilir
    private var tmdbApiClient = TmdbApiClient()
    private val settingsDataStore = SettingsDataStore(application)
    private var tmdbUserApiKeyState = ""

    // Her platform için ayrı cache — platform geçişinde yeniden fetch yapılmaz
    private val platformCache = Companion.platformCache
    // Başarıyla yüklenen platformların seti — boş veri dönsün, takılmaması için
    private val loadedPlatforms = Companion.loadedPlatforms

    private var isFallbackInProgress = false
    private var isFirstLoad = true
    private var showAdultContentState = false
    private var loadJob: Job? = null

    var selectedPlatform by mutableStateOf(ExplorePlatform.TMDB)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // TMDB entegrasyon durumu — settingsFlow'dan reaktif olarak güncellenir
    private var tmdbEnabledState = true
    private var tmdbModernHomeEnabledState = false
    private var tmdbEnrichContinueWatchingState = true

    private val initialPayload = platformCache[ExplorePlatform.TMDB]

    var topAnime by mutableStateOf<List<JikanSearchResult>>(initialPayload?.topAnime ?: emptyList())
        private set

    var airingAnime by mutableStateOf<List<JikanSearchResult>>(initialPayload?.airingAnime ?: emptyList())
        private set

    var upcomingAnime by mutableStateOf<List<JikanSearchResult>>(initialPayload?.upcomingAnime ?: emptyList())
        private set

    var topManga by mutableStateOf<List<JikanSearchResult>>(initialPayload?.topManga ?: emptyList())
        private set

    var publishingManga by mutableStateOf<List<JikanSearchResult>>(initialPayload?.publishingManga ?: emptyList())
        private set

    var trendingAnime by mutableStateOf<List<JikanSearchResult>>(initialPayload?.trendingAnime ?: emptyList())
        private set

    var movieAnime by mutableStateOf<List<JikanSearchResult>>(initialPayload?.movieAnime ?: emptyList())
        private set

    var seasonalAnime by mutableStateOf<List<JikanSearchResult>>(initialPayload?.seasonalAnime ?: emptyList())
        private set

    var airingSoonAnime by mutableStateOf<List<JikanSearchResult>>(initialPayload?.airingSoonAnime ?: emptyList())
        private set

    var heroIndex by mutableIntStateOf(0)

    // ── Simkl Kullanıcı Listeleri (NyanTV HomeSections.kt referans) ──────────────
    /** İzlemeye devam et — filmler (Simkl status=watching, isMovie=true) */
    var simklContinueMovies by mutableStateOf<List<JikanSearchResult>>(initialPayload?.simklContinueMovies ?: emptyList())
        private set

    /** Planladıklarım — filmler (Simkl status=plantowatch, isMovie=true) */
    var simklPlannedMovies by mutableStateOf<List<JikanSearchResult>>(initialPayload?.simklPlannedMovies ?: emptyList())
        private set

    /** İzlemeye devam et — diziler/anime (Simkl status=watching, isMovie=false) */
    var simklContinueSeries by mutableStateOf<List<JikanSearchResult>>(initialPayload?.simklContinueSeries ?: emptyList())
        private set

    /** Planladıklarım — diziler/anime (Simkl status=plantowatch, isMovie=false) */
    var simklPlannedSeries by mutableStateOf<List<JikanSearchResult>>(initialPayload?.simklPlannedSeries ?: emptyList())
        private set

    val isDataLoaded: Boolean
        get() = selectedPlatform in loadedPlatforms ||
                topAnime.isNotEmpty() || airingAnime.isNotEmpty() || trendingAnime.isNotEmpty()

    init {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { settings ->
                val adultChanged = showAdultContentState != settings.showAdultContent
                showAdultContentState = settings.showAdultContent

                // TMDB toggle değişiklikleri
                val userKey = settings.tmdbUserApiKey
                val tmdbChanged = tmdbEnabledState != settings.tmdbEnabled ||
                    tmdbModernHomeEnabledState != settings.tmdbModernHomeEnabled ||
                    tmdbEnrichContinueWatchingState != settings.tmdbEnrichContinueWatching ||
                    userKey != tmdbUserApiKeyState
                tmdbEnabledState = settings.tmdbEnabled
                tmdbModernHomeEnabledState = settings.tmdbModernHomeEnabled
                tmdbEnrichContinueWatchingState = settings.tmdbEnrichContinueWatching

                // tmdbUserApiKey değişince TmdbApiClient'i yeniden oluştur
                if (userKey != tmdbUserApiKeyState) {
                    tmdbUserApiKeyState = userKey
                    tmdbApiClient = TmdbApiClient(userApiKey = userKey)
                }

                if (adultChanged || tmdbChanged || isFirstLoad) {
                    val wasFirstLoad = isFirstLoad
                    isFirstLoad = false
                    
                    if (wasFirstLoad) {
                        // Prefetch işlemi arka planda devam ediyorsa bitmesini bekle
                        Companion.prefetchJob?.join()

                        val cached = platformCache[selectedPlatform]
                        if (cached != null) {
                            // Cache hit — hemen uygula, loading state'i kısa tut
                            isLoading = true
                            applyPayload(cached)
                            loadedPlatforms.add(selectedPlatform)
                            isLoading = false
                        } else {
                            // Cache miss — loadData() kendi finally bloğuyla isLoading'i yönetir
                            errorMessage = null
                            loadData(forceRefresh = true)
                        }
                    } else {
                        platformCache.clear()
                        loadedPlatforms.clear()
                        loadData(forceRefresh = true)
                    }
                }
            }
        }
    }

    fun selectPlatform(platform: ExplorePlatform, isFallback: Boolean = false) {
        if (selectedPlatform == platform) return
        if (!isFallback) {
            isFallbackInProgress = false
        }
        selectedPlatform = platform

        // Stale veriyi hemen temizle — eski platformun verisi yeni platformda gözükmesin
        clearPayload()

        // Cache'de varsa anında yükle, yoksa fetch et
        val cached = platformCache[platform]
        if (cached != null) {
            applyPayload(cached)
            // Cache'den yüklenen platformu da "loaded" say
            loadedPlatforms.add(platform)
        } else {
            loadData(forceRefresh = true)
        }
    }

    private fun applyPayload(payload: ExplorePayload) {
        topAnime = payload.topAnime
        airingAnime = payload.airingAnime
        upcomingAnime = payload.upcomingAnime
        topManga = payload.topManga
        publishingManga = payload.publishingManga
        trendingAnime = payload.trendingAnime
        movieAnime = payload.movieAnime
        seasonalAnime = payload.seasonalAnime
        airingSoonAnime = payload.airingSoonAnime
        heroIndex = 0

        simklContinueMovies = payload.simklContinueMovies
        simklPlannedMovies = payload.simklPlannedMovies
        simklContinueSeries = payload.simklContinueSeries
        simklPlannedSeries = payload.simklPlannedSeries
    }

    /** Platform değişiminde UI'daki eski veriyi siler, loading skeleton gösterilir. */
    private fun clearPayload() {
        topAnime = emptyList()
        airingAnime = emptyList()
        upcomingAnime = emptyList()
        topManga = emptyList()
        publishingManga = emptyList()
        trendingAnime = emptyList()
        movieAnime = emptyList()
        seasonalAnime = emptyList()
        airingSoonAnime = emptyList()
        // Simkl kullanıcı şeritlerini temizleme — platform değişiminde de gözüksün
        heroIndex = 0
        errorMessage = null
    }

    fun loadData(forceRefresh: Boolean = false) {
        // Eğer forceRefresh değilse ve zaten data yüklüyse veya yükleniyorsa bir şey yapma
        if (!forceRefresh && (isDataLoaded || isLoading)) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val platformSnapshot = selectedPlatform

            try {
                val payload = when (platformSnapshot) {
                    ExplorePlatform.MAL -> loadMalData()
                    ExplorePlatform.AniList -> loadAniListData()
                    ExplorePlatform.TMDB -> loadTmdbData()
                }

                if (selectedPlatform == platformSnapshot) {
                    platformCache[platformSnapshot] = payload
                    loadedPlatforms.add(platformSnapshot)
                    applyPayload(payload)
                    isFallbackInProgress = false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (selectedPlatform == platformSnapshot) {
                    errorMessage = e.message ?: "Keşfet verileri alınamadı."
                    if (!isFallbackInProgress) {
                        isFallbackInProgress = true
                        val nextPlatform = if (platformSnapshot == ExplorePlatform.AniList) {
                            ExplorePlatform.MAL
                        } else if (platformSnapshot == ExplorePlatform.TMDB) {
                            ExplorePlatform.AniList
                        } else {
                            ExplorePlatform.AniList
                        }
                        selectPlatform(nextPlatform, isFallback = true)
                    } else {
                        isFallbackInProgress = false
                    }
                }
            } finally {
                if (coroutineContext[Job] == loadJob) {
                    isLoading = false
                }
            }
        }
    }

    /**
     * enrichWithTmdb: Sadece source="simkl" olan öğeler için (kullanıcı watchlist'i)
     * TMDB'den zenginleştirilmiş poster/puan çeker.
     * TMDB trending öğeleri zaten doğru veriye sahip olduğu için burada işleme alınmaz.
     */
    private suspend fun enrichWithTmdb(item: JikanSearchResult): JikanSearchResult {
        // TMDB kaynaklı öğeler zaten tam veriye sahip — tekrar TMDB'ye gitme
        if (item.source == "tmdb") return item
        val tmdbId = item.tmdbId ?: return item
        val isMovie = item.type == com.kitsugi.animelist.model.MediaType.Movie
        val details = tmdbApiClient.fetchMediaDetail(tmdbId, isMovie) ?: return item

        val typeStr = when (item.type) {
            com.kitsugi.animelist.model.MediaType.Movie -> "Film"
            com.kitsugi.animelist.model.MediaType.TvShow -> "Dizi"
            com.kitsugi.animelist.model.MediaType.Anime -> "Anime"
            else -> "Anime"
        }

        val subtitleParts = buildList {
            add(typeStr)
            val yearVal = item.year ?: details.year
            if (yearVal != null && yearVal > 0) add(yearVal.toString())
            addAll(details.genres.take(3))
        }
        val richSubtitle = subtitleParts.joinToString(", ")

        val ratingInt = (details.score ?: 0) / 10
        val finalScore = if (ratingInt > 0) ratingInt else null

        val backdropUrl = details.pictures.firstOrNull { it.contains("/w1280/") } ?: item.backdropUrl

        return item.copy(
            title = details.title?.takeIf { it.isNotBlank() } ?: item.title,
            subtitle = richSubtitle,
            score = finalScore ?: item.score,
            year = details.year ?: item.year,
            imageUrl = details.imageUrl ?: item.imageUrl,
            backdropUrl = backdropUrl
        )
    }

    /**
     * TMDB tab için veri kaynağı — TMDB trending/popular içerikleri çeker.
     * Simkl API'si yalnızca giriş yapmış kullanıcının watchlist'ini çekmek için
     * (SimklSyncManager) çağrılır. Unauthenticated discovery tamamen TMDB'ye taşındı.
     */
    private suspend fun loadTmdbData(): ExplorePayload = supervisorScope {
        val context = getApplication<Application>().applicationContext
        val simklToken = ExternalAuthManager.getSimklToken(context)

        // ── TMDB Discovery: her zaman aktif ──────────────────────────────────────
        val trendingMoviesDeferred = async { runCatching { tmdbApiClient.getTrendingMovies() }.getOrDefault(emptyList()) }
        val trendingShowsDeferred  = async { runCatching { tmdbApiClient.getTrendingShows() }.getOrDefault(emptyList()) }
        val popularMoviesDeferred  = async { runCatching { tmdbApiClient.getPopularMovies() }.getOrDefault(emptyList()) }
        val trendingAllDeferred    = async { runCatching { tmdbApiClient.getTrendingAll() }.getOrDefault(emptyList()) }
        val popularShowsDeferred   = async { runCatching { tmdbApiClient.getPopularShows() }.getOrDefault(emptyList()) }
        val topRatedMoviesDeferred = async { runCatching { tmdbApiClient.getTopRatedMovies() }.getOrDefault(emptyList()) }
        val topRatedShowsDeferred  = async { runCatching { tmdbApiClient.getTopRatedShows() }.getOrDefault(emptyList()) }

        val moviesList    = trendingMoviesDeferred.await()
        val showsList     = trendingShowsDeferred.await()
        val popularMovies = popularMoviesDeferred.await()
        val allTrending   = trendingAllDeferred.await()
        val popularShows  = popularShowsDeferred.await()
        val topRatedMovies = topRatedMoviesDeferred.await()
        val topRatedShows  = topRatedShowsDeferred.await()

        // ── Authenticated: Simkl watchlist (sadece giriş yapılmışsa) ────────────────
        val userMoviesDeferred = if (!simklToken.isNullOrBlank()) {
            async { SimklSyncManager.fetchSimklWatchlist(context, "movies") }
        } else null
        val userShowsDeferred = if (!simklToken.isNullOrBlank()) {
            async { SimklSyncManager.fetchSimklWatchlist(context, "shows") }
        } else null

        // Kullanıcı listelerini filtrele
        val userMovies = userMoviesDeferred?.let { runCatching { it.await() }.getOrDefault(emptyList()) } ?: emptyList()
        val userShows  = userShowsDeferred?.let { runCatching { it.await() }.getOrDefault(emptyList()) } ?: emptyList()

        var continueMovies = userMovies.filter { it.subtitle.contains("İzleniyor") }
        var plannedMovies  = userMovies.filter { it.subtitle.contains("Planlandı") }
        var continueSeries = userShows.filter { it.subtitle.contains("İzleniyor") }
        var plannedSeries  = userShows.filter { it.subtitle.contains("Planlandı") }

        if (tmdbEnabledState && tmdbEnrichContinueWatchingState) {
            // Sadece "İzlemeye Devam Et" listelerini paralel olarak zenginleştir (maksimum ilk 8 öğe)
            val moviesToEnrich = continueMovies.take(8)
            val seriesToEnrich = continueSeries.take(8)
            val movieJobs = moviesToEnrich.map { item ->
                async { enrichWithTmdb(item) }
            }
            val seriesJobs = seriesToEnrich.map { item ->
                async { enrichWithTmdb(item) }
            }
            val enrichedMovies = movieJobs.mapIndexed { idx, job ->
                runCatching { job.await() }.getOrDefault(moviesToEnrich[idx])
            }
            val enrichedSeries = seriesJobs.mapIndexed { idx, job ->
                runCatching { job.await() }.getOrDefault(seriesToEnrich[idx])
            }
            continueMovies = enrichedMovies + continueMovies.drop(8)
            continueSeries = enrichedSeries + continueSeries.drop(8)
        }

        simklContinueMovies  = continueMovies
        simklPlannedMovies   = plannedMovies
        simklContinueSeries  = continueSeries
        simklPlannedSeries   = plannedSeries

        ExplorePayload(
            topAnime      = allTrending,    // Trend Her Şey (film + dizi karışık)
            airingAnime   = showsList,      // Trend Diziler
            upcomingAnime = popularMovies,  // Popüler Filmler
            topManga      = popularShows,   // Popüler Diziler
            publishingManga = topRatedMovies, // En Yüksek Puanlı Filmler
            trendingAnime = allTrending,
            movieAnime    = moviesList,     // Trend Filmler
            seasonalAnime = topRatedShows,  // En Yüksek Puanlı Diziler
            simklContinueMovies = continueMovies,
            simklPlannedMovies = plannedMovies,
            simklContinueSeries = continueSeries,
            simklPlannedSeries = plannedSeries,
            airingSoonAnime = emptyList()
        )
    }

    private suspend fun loadMalData(): ExplorePayload = supervisorScope {
        val showAdult = showAdultContentState
        val topAnimeDeferred = async { apiClient.topAnime(showAdultContent = showAdult) }
        val airingAnimeDeferred = async { apiClient.airingAnime(showAdultContent = showAdult) }
        val upcomingAnimeDeferred = async { apiClient.upcomingAnime(showAdultContent = showAdult) }
        val topMangaDeferred = async { apiClient.topManga(showAdultContent = showAdult) }
        val publishingMangaDeferred = async { apiClient.publishingManga(showAdultContent = showAdult) }

        val rawTopAnime = runCatching { topAnimeDeferred.await() }.getOrDefault(emptyList())

        // İlk 5 vitrin öğesini paralel olarak TMDB'den yatay backdrop resmi ile zenginleştir
        val enrichedTopAnime = if (rawTopAnime.isNotEmpty() && tmdbEnabledState) {
            val heroCount = minOf(rawTopAnime.size, 5)
            val backdropJobs = (0 until heroCount).map { index ->
                val item = rawTopAnime[index]
                async {
                    val backdrop = tmdbApiClient.fetchBackdropByTitle(item.title)
                    if (backdrop != null) item.copy(backdropUrl = backdrop) else item
                }
            }
            val enrichedHeroes = backdropJobs.mapIndexed { index, job ->
                runCatching { job.await() }.getOrDefault(rawTopAnime[index])
            }
            enrichedHeroes + rawTopAnime.drop(heroCount)
        } else {
            rawTopAnime
        }

        val airingSoonDeferred = async {
            val calendarClient = com.kitsugi.animelist.data.remote.KitsugiAiringCalendarClient()
            val weekly = runCatching { calendarClient.fetchWeeklySchedule() }.getOrNull() ?: emptyMap()
            val nowSeconds = System.currentTimeMillis() / 1000L
            weekly.values.flatten()
                .filter { it.airingAt > nowSeconds }
                .sortedBy { it.airingAt }
                .take(15)
                .map { entry ->
                    JikanSearchResult(
                        malId = entry.malId ?: entry.aniListId,
                        title = entry.title,
                        subtitle = "${entry.episode}. Bölüm",
                        type = MediaType.Anime,
                        total = null,
                        score = null,
                        isAdult = false,
                        imageUrl = entry.coverUrl,
                        year = null,
                        source = "anilist",
                        realMalId = entry.malId,
                        titleEnglish = entry.titleEnglish,
                        titleJapanese = entry.titleNative,
                        nextAiringEpisode = "${entry.episode}|${entry.airingAt}"
                    )
                }
        }

        ExplorePayload(
            topAnime = enrichedTopAnime,
            airingAnime = runCatching { airingAnimeDeferred.await() }.getOrDefault(emptyList()),
            upcomingAnime = runCatching { upcomingAnimeDeferred.await() }.getOrDefault(emptyList()),
            topManga = runCatching { topMangaDeferred.await() }.getOrDefault(emptyList()),
            publishingManga = runCatching { publishingMangaDeferred.await() }.getOrDefault(emptyList()),
            trendingAnime = emptyList(),
            movieAnime = emptyList(),
            seasonalAnime = emptyList(),
            airingSoonAnime = runCatching { airingSoonDeferred.await() }.getOrDefault(emptyList())
        )
    }

    private suspend fun loadAniListData(): ExplorePayload = supervisorScope {
        val showAdult = showAdultContentState
        val topAnimeDeferred = async { apiClient.aniListTopAnime(showAdultContent = showAdult) }
        val airingAnimeDeferred = async { apiClient.aniListAiringAnime(showAdultContent = showAdult) }
        val upcomingAnimeDeferred = async { apiClient.aniListUpcomingAnime(showAdultContent = showAdult) }
        val topMangaDeferred = async { apiClient.aniListTopManga(showAdultContent = showAdult) }
        val publishingMangaDeferred = async { apiClient.aniListPublishingManga(showAdultContent = showAdult) }

        val airingSoonDeferred = async {
            val calendarClient = com.kitsugi.animelist.data.remote.KitsugiAiringCalendarClient()
            val weekly = runCatching { calendarClient.fetchWeeklySchedule() }.getOrNull() ?: emptyMap()
            val nowSeconds = System.currentTimeMillis() / 1000L
            weekly.values.flatten()
                .filter { it.airingAt > nowSeconds }
                .sortedBy { it.airingAt }
                .take(15)
                .map { entry ->
                    JikanSearchResult(
                        malId = entry.malId ?: entry.aniListId,
                        title = entry.title,
                        subtitle = "${entry.episode}. Bölüm",
                        type = MediaType.Anime,
                        total = null,
                        score = null,
                        isAdult = false,
                        imageUrl = entry.coverUrl,
                        year = null,
                        source = "anilist",
                        realMalId = entry.malId,
                        titleEnglish = entry.titleEnglish,
                        titleJapanese = entry.titleNative,
                        nextAiringEpisode = "${entry.episode}|${entry.airingAt}"
                    )
                }
        }

        ExplorePayload(
            topAnime = runCatching { topAnimeDeferred.await() }.getOrDefault(emptyList()),
            airingAnime = runCatching { airingAnimeDeferred.await() }.getOrDefault(emptyList()),
            upcomingAnime = runCatching { upcomingAnimeDeferred.await() }.getOrDefault(emptyList()),
            topManga = runCatching { topMangaDeferred.await() }.getOrDefault(emptyList()),
            publishingManga = runCatching { publishingMangaDeferred.await() }.getOrDefault(emptyList()),
            trendingAnime = emptyList(),
            movieAnime = emptyList(),
            seasonalAnime = emptyList(),
            airingSoonAnime = runCatching { airingSoonDeferred.await() }.getOrDefault(emptyList())
        )
    }

    fun nextHero(heroCount: Int) {
        if (heroCount == 0) return
        heroIndex = if (heroIndex >= heroCount - 1) 0 else heroIndex + 1
    }

    fun previousHero(heroCount: Int) {
        if (heroCount == 0) return
        heroIndex = if (heroIndex <= 0) heroCount - 1 else heroIndex - 1
    }

    companion object {
        val platformCache = java.util.concurrent.ConcurrentHashMap<ExplorePlatform, ExplorePayload>()
        val loadedPlatforms = java.util.Collections.synchronizedSet(mutableSetOf<ExplorePlatform>())
        
        private val isPrefetchStarted = java.util.concurrent.atomic.AtomicBoolean(false)
        @Volatile
        var prefetchJob: kotlinx.coroutines.Job? = null

        fun prefetch(context: android.content.Context) {
            if (!isPrefetchStarted.compareAndSet(false, true)) return
            
            val app = context.applicationContext as android.app.Application
            
            prefetchJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val settingsDataStore = SettingsDataStore(app)
                    val settings = settingsDataStore.settingsFlow.first()
                    
                    val showAdult = settings.showAdultContent
                    val tmdbEnabled = settings.tmdbEnabled
                    val tmdbEnrich = settings.tmdbEnrichContinueWatching
                    val simklToken = ExternalAuthManager.getSimklToken(app)
                    
                    val tmdbApiClient = TmdbApiClient(userApiKey = settings.tmdbUserApiKey)
                    
                    supervisorScope {
                        // Startup prefetch only fetches TMDB (default platform) to minimize latency and bandwidth
                        val tmdbPayload = runCatching {
                            val trendingMoviesDeferred = async { runCatching { tmdbApiClient.getTrendingMovies() }.getOrDefault(emptyList()) }
                            val trendingShowsDeferred  = async { runCatching { tmdbApiClient.getTrendingShows() }.getOrDefault(emptyList()) }
                            val popularMoviesDeferred  = async { runCatching { tmdbApiClient.getPopularMovies() }.getOrDefault(emptyList()) }
                            val trendingAllDeferred    = async { runCatching { tmdbApiClient.getTrendingAll() }.getOrDefault(emptyList()) }
                            val popularShowsDeferred   = async { runCatching { tmdbApiClient.getPopularShows() }.getOrDefault(emptyList()) }
                            val topRatedMoviesDeferred = async { runCatching { tmdbApiClient.getTopRatedMovies() }.getOrDefault(emptyList()) }
                            val topRatedShowsDeferred  = async { runCatching { tmdbApiClient.getTopRatedShows() }.getOrDefault(emptyList()) }

                            val moviesList    = trendingMoviesDeferred.await()
                            val showsList     = trendingShowsDeferred.await()
                            val popularMovies = popularMoviesDeferred.await()
                            val allTrending   = trendingAllDeferred.await()
                            val popularShows  = popularShowsDeferred.await()
                            val topRatedMovies = topRatedMoviesDeferred.await()
                            val topRatedShows  = topRatedShowsDeferred.await()

                            val userMoviesDeferred = if (!simklToken.isNullOrBlank()) {
                                async { SimklSyncManager.fetchSimklWatchlist(app, "movies") }
                            } else null
                            val userShowsDeferred = if (!simklToken.isNullOrBlank()) {
                                async { SimklSyncManager.fetchSimklWatchlist(app, "shows") }
                            } else null

                            val userMovies = userMoviesDeferred?.let { runCatching { it.await() }.getOrDefault(emptyList()) } ?: emptyList()
                            val userShows  = userShowsDeferred?.let { runCatching { it.await() }.getOrDefault(emptyList()) } ?: emptyList()

                            var continueMovies = userMovies.filter { it.subtitle.contains("İzleniyor") }
                            val plannedMovies  = userMovies.filter { it.subtitle.contains("Planlandı") }
                            var continueSeries = userShows.filter { it.subtitle.contains("İzleniyor") }
                            val plannedSeries  = userShows.filter { it.subtitle.contains("Planlandı") }

                            if (tmdbEnabled && tmdbEnrich) {
                                val moviesToEnrich = continueMovies.take(8)
                                val seriesToEnrich = continueSeries.take(8)
                                val movieJobs = moviesToEnrich.map { item ->
                                    async {
                                        if (item.source != "tmdb") {
                                            val tmdbId = item.tmdbId
                                            if (tmdbId != null) {
                                                val isMovie = item.type == MediaType.Movie
                                                val details = tmdbApiClient.fetchMediaDetail(tmdbId, isMovie)
                                                if (details != null) {
                                                    val typeStr = when (item.type) {
                                                        MediaType.Movie -> "Film"
                                                        MediaType.TvShow -> "Dizi"
                                                        MediaType.Anime -> "Anime"
                                                        else -> "Anime"
                                                    }
                                                    val subtitleParts = buildList {
                                                        add(typeStr)
                                                        val yearVal = item.year ?: details.year
                                                        if (yearVal != null && yearVal > 0) add(yearVal.toString())
                                                        addAll(details.genres.take(3))
                                                    }
                                                    val ratingInt = (details.score ?: 0) / 10
                                                    val finalScore = if (ratingInt > 0) ratingInt else null
                                                    val backdropUrl = details.pictures.firstOrNull { it.contains("/w1280/") } ?: item.backdropUrl
                                                    item.copy(
                                                        title = details.title?.takeIf { it.isNotBlank() } ?: item.title,
                                                        subtitle = subtitleParts.joinToString(", "),
                                                        score = finalScore ?: item.score,
                                                        year = details.year ?: item.year,
                                                        imageUrl = details.imageUrl ?: item.imageUrl,
                                                        backdropUrl = backdropUrl
                                                    )
                                                } else item
                                            } else item
                                        } else item
                                    }
                                }
                                val seriesJobs = seriesToEnrich.map { item ->
                                    async {
                                        if (item.source != "tmdb") {
                                            val tmdbId = item.tmdbId
                                            if (tmdbId != null) {
                                                val isMovie = item.type == MediaType.Movie
                                                val details = tmdbApiClient.fetchMediaDetail(tmdbId, isMovie)
                                                if (details != null) {
                                                    val typeStr = when (item.type) {
                                                        MediaType.Movie -> "Film"
                                                        MediaType.TvShow -> "Dizi"
                                                        MediaType.Anime -> "Anime"
                                                        else -> "Anime"
                                                    }
                                                    val subtitleParts = buildList {
                                                        add(typeStr)
                                                        val yearVal = item.year ?: details.year
                                                        if (yearVal != null && yearVal > 0) add(yearVal.toString())
                                                        addAll(details.genres.take(3))
                                                    }
                                                    val ratingInt = (details.score ?: 0) / 10
                                                    val finalScore = if (ratingInt > 0) ratingInt else null
                                                    val backdropUrl = details.pictures.firstOrNull { it.contains("/w1280/") } ?: item.backdropUrl
                                                    item.copy(
                                                        title = details.title?.takeIf { it.isNotBlank() } ?: item.title,
                                                        subtitle = subtitleParts.joinToString(", "),
                                                        score = finalScore ?: item.score,
                                                        year = details.year ?: item.year,
                                                        imageUrl = details.imageUrl ?: item.imageUrl,
                                                        backdropUrl = backdropUrl
                                                    )
                                                } else item
                                            } else item
                                        } else item
                                    }
                                }
                                continueMovies = movieJobs.mapIndexed { idx, job ->
                                    runCatching { job.await() }.getOrDefault(moviesToEnrich[idx])
                                }
                                continueSeries = seriesJobs.mapIndexed { idx, job ->
                                    runCatching { job.await() }.getOrDefault(seriesToEnrich[idx])
                                }
                                continueMovies = continueMovies + userMovies.filter { it.subtitle.contains("İzleniyor") }.drop(8)
                                continueSeries = continueSeries + userShows.filter { it.subtitle.contains("İzleniyor") }.drop(8)
                            }

                            ExplorePayload(
                                topAnime      = allTrending,
                                airingAnime   = showsList,
                                upcomingAnime = popularMovies,
                                topManga      = popularShows,
                                publishingManga = topRatedMovies,
                                trendingAnime = allTrending,
                                movieAnime    = moviesList,
                                seasonalAnime = topRatedShows,
                                simklContinueMovies = continueMovies,
                                simklPlannedMovies = plannedMovies,
                                simklContinueSeries = continueSeries,
                                simklPlannedSeries = plannedSeries,
                                airingSoonAnime = emptyList()
                            )
                        }.getOrNull()

                        if (tmdbPayload != null) {
                            platformCache[ExplorePlatform.TMDB] = tmdbPayload
                            loadedPlatforms.add(ExplorePlatform.TMDB)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ExplorePrefetch", "Prefetch failed", e)
                }
            }
        }
    }
}

data class ExplorePayload(
    val topAnime: List<JikanSearchResult>,
    val airingAnime: List<JikanSearchResult>,
    val upcomingAnime: List<JikanSearchResult>,
    val topManga: List<JikanSearchResult>,
    val publishingManga: List<JikanSearchResult>,
    val trendingAnime: List<JikanSearchResult>,
    val movieAnime: List<JikanSearchResult>,
    val seasonalAnime: List<JikanSearchResult>,
    val simklContinueMovies: List<JikanSearchResult> = emptyList(),
    val simklPlannedMovies: List<JikanSearchResult> = emptyList(),
    val simklContinueSeries: List<JikanSearchResult> = emptyList(),
    val simklPlannedSeries: List<JikanSearchResult> = emptyList(),
    val airingSoonAnime: List<JikanSearchResult> = emptyList()
)
