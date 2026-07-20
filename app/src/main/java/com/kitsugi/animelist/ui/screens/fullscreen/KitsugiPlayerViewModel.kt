package com.kitsugi.animelist.ui.screens.fullscreen

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.core.player.SubtitleInput
import com.kitsugi.animelist.core.player.PlaybackState
import com.kitsugi.animelist.data.cloudstream.CsPluginLoader
import com.kitsugi.animelist.data.cloudstream.CsStreamRunner
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.local.HistoryRepository
import com.kitsugi.animelist.data.remote.KitsugiMediaTabsClient
import com.kitsugi.animelist.data.remote.KitsugiStreamingEpisode
import com.kitsugi.animelist.data.repository.AddonStreamRepository
import com.kitsugi.animelist.data.repository.StreamSorter
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.data.trailer.InAppYouTubeExtractor
import com.kitsugi.animelist.data.trailer.TrailerPlaybackSource
import com.kitsugi.animelist.ui.screens.fullscreen.components.MetaCastMember
import com.kitsugi.animelist.core.player.PlayerLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import com.kitsugi.animelist.core.player.AudioDelayRouteConfig
import com.kitsugi.animelist.core.player.AudioOutputRouteDetector
import com.kitsugi.animelist.core.player.AudioRoute
import com.kitsugi.animelist.data.remote.AniSkipClient
import com.kitsugi.animelist.data.remote.AnimeSkipClient
import com.kitsugi.animelist.data.remote.SkipInterval
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.data.local.toDomain
import com.kitsugi.animelist.core.player.PostPlayMode
import kotlinx.coroutines.async as asyncSkip
import kotlinx.coroutines.awaitAll


class KitsugiPlayerViewModel(application: Application) : AndroidViewModel(application) {


    private val context = application.applicationContext

    private val historyRepository by lazy {
        HistoryRepository(KitsugiDatabase.getDatabase(context).historyDao())
    }

    // State Variables
    var currentVideoUrl by mutableStateOf<String?>(null)
        private set
    var currentAudioUrl by mutableStateOf<String?>(null)
        private set
    var currentHeaders by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var currentSubtitles by mutableStateOf<List<SubtitleInput>>(emptyList())
        private set
    var currentTitle by mutableStateOf("")
        private set
    var currentSourceIndex by mutableStateOf(-1)
        private set
    var currentStreamSources by mutableStateOf<List<StreamSource>>(emptyList())
        private set
    var currentAddonName by mutableStateOf<String?>(null)
        private set
    var currentEpisode by mutableStateOf(1)
        private set
    var isMovie by mutableStateOf(false)
        private set
    var episodesList by mutableStateOf<List<KitsugiStreamingEpisode>>(emptyList())
        private set
    var userCancelledBinge by mutableStateOf(false)
    var isResolvingStream by mutableStateOf(false)
    var nextEpisodeLoading by mutableStateOf(false)
    var playbackSource by mutableStateOf<TrailerPlaybackSource?>(null)
        private set

    var isLoading by mutableStateOf(true)
    var hasError by mutableStateOf(false)
    var errorDetails by mutableStateOf<String?>(null)

    /** Set to true while the player is auto-switching to the next source after a playback error. */
    var isAutoSwitching by mutableStateOf(false)
        private set

    // ── TASK_042: PlaybackState StateFlow ────────────────────────────────────
    // NuvioTV PlayerRuntimeController.playerState pattern referans alındı.
    // PlayerEngine.Listener.onStateChanged olaylarını üst seviye PlaybackState'e dönüştürür.
    private val _playerState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playerState: StateFlow<PlaybackState> = _playerState.asStateFlow()

    /** PlayerEngine durumunu PlaybackState'e çevirir ve StateFlow'u günceller. */
    fun updatePlayerState(engineState: com.kitsugi.animelist.core.player.engine.PlayerEngine.State, isPlaying: Boolean) {
        _playerState.value = when (engineState) {
            com.kitsugi.animelist.core.player.engine.PlayerEngine.State.IDLE      -> PlaybackState.Idle
            com.kitsugi.animelist.core.player.engine.PlayerEngine.State.BUFFERING -> PlaybackState.Buffering
            com.kitsugi.animelist.core.player.engine.PlayerEngine.State.READY     -> if (isPlaying) PlaybackState.Playing else PlaybackState.Paused
            com.kitsugi.animelist.core.player.engine.PlayerEngine.State.ENDED     -> PlaybackState.Ended
        }
    }

    /** Hata oluştuğunda PlayerState.Error'a geç */
    fun setPlayerError(errorCode: Int, errorMsg: String) {
        _playerState.value = PlaybackState.Error(errorMessage = errorMsg, errorCode = errorCode)
    }

    var detectedFrameRateRaw by mutableStateOf(0f)
        private set
    var detectedFrameRate by mutableStateOf(0f)
        private set
    var afrProbeRunning by mutableStateOf(false)
        private set

    /** How many consecutive auto-switch attempts have been made for the current episode. */
    private var autoSwitchAttempts = 0
    private val maxAutoSwitchAttempts = 5

    // Meta parameters passed from Activity
    private var malId: Int? = null
    private var aniListId: Int? = null
    private var tmdbId: Int? = null
    private var seasonNum: Int = 1
    private var animeTitle: String = ""
    private var titleEnglish: String? = null
    private var titleRomaji: String? = null
    private var titleNative: String? = null
    private var startYear: Int? = null

    var isInitialized by mutableStateOf(false)
        private set

    private var lastInitializedKey: String? = null

    fun initialize(
        videoId: String?,
        videoUrl: String?,
        audioUrl: String?,
        title: String,
        requestHeaders: Map<String, String>,
        initialSubtitles: List<SubtitleInput>,
        streamSources: List<StreamSource>,
        initialIndex: Int,
        malId: Int?,
        aniListId: Int?,
        tmdbId: Int? = null,
        season: Int,
        episode: Int,
        animeTitle: String,
        titleEnglish: String?,
        titleRomaji: String?,
        titleNative: String?,
        startYear: Int?,
        isMovie: Boolean = false,
        activity: android.app.Activity? = null
    ) {
        val initKey = "${videoId ?: ""}_${videoUrl ?: ""}_${episode}_${aniListId ?: 0}_${malId ?: 0}_${tmdbId ?: 0}"
        if (isInitialized && lastInitializedKey == initKey) return
        isInitialized = true
        lastInitializedKey = initKey

        this.malId = malId
        this.aniListId = aniListId
        this.tmdbId = tmdbId
        this.seasonNum = season
        this.isMovie = isMovie || (season == 0 && episode <= 1)
        this.animeTitle = animeTitle
        this.titleEnglish = titleEnglish
        this.titleRomaji = titleRomaji
        this.titleNative = titleNative
        this.startYear = startYear

        this.currentVideoUrl = videoUrl
        this.currentAudioUrl = audioUrl
        this.currentHeaders = requestHeaders
        this.currentSubtitles = initialSubtitles
        this.currentStreamSources = streamSources
        this.currentSourceIndex = initialIndex
        this.currentTitle = title
        this.currentEpisode = episode
        if (initialIndex in streamSources.indices) {
            this.currentAddonName = streamSources[initialIndex].addonName
        }
        // Update hasFallback: true if there are multiple sources to switch between
        orchestrator.errorRecovery.hasFallback = streamSources.size > 1

        viewModelScope.launch {
            loadEpisodes()
        }

        // AniSkip: bölüm/seri değiştiğinde intro/outro aralıklarını yükle
        if (malId != null) {
            loadSkipIntervals(malId, episode)
        }

        loadPlaybackSource(videoId, activity)
        if (videoUrl != null) {
            fetchAutoSubtitles()
        }
    }

    private fun loadPlaybackSource(videoId: String?, activity: android.app.Activity?) {
        viewModelScope.launch {
            isLoading = true
            hasError = false
            when {
                videoId != null -> {
                    try {
                        val src = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            InAppYouTubeExtractor()
                                .extractPlaybackSource("https://www.youtube.com/watch?v=$videoId")
                        }
                        if (src != null) {
                            playbackSource = src
                        } else {
                            hasError = true
                        }
                    } catch (e: Exception) {
                        hasError = true
                        errorDetails = e.message
                    } finally {
                        isLoading = false
                    }
                }
                currentVideoUrl != null -> {
                    val settings = SettingsDataStore(context).settingsFlow.first()
                    viewModelScope.launch {
                        runAfrPreflightIfEnabled(
                            context = context,
                            activity = activity,
                            url = currentVideoUrl!!,
                            headers = currentHeaders,
                            frameRateMatchingMode = settings.frameRateMatchingMode,
                            resolutionMatchingEnabled = settings.resolutionMatchingEnabled
                        )
                    }
                    playbackSource = TrailerPlaybackSource(videoUrl = currentVideoUrl!!, audioUrl = currentAudioUrl)
                    isLoading = false
                }
                else -> {
                    hasError = true
                    isLoading = false
                }
            }
        }
    }

    suspend fun runAfrPreflightIfEnabled(
        context: Context,
        activity: android.app.Activity?,
        url: String,
        headers: Map<String, String>,
        frameRateMatchingMode: com.kitsugi.animelist.data.settings.FrameRateMatchingMode,
        resolutionMatchingEnabled: Boolean
    ) {
        if (frameRateMatchingMode == com.kitsugi.animelist.data.settings.FrameRateMatchingMode.OFF) {
            detectedFrameRateRaw = 0f
            detectedFrameRate = 0f
            afrProbeRunning = false
            return
        }

        if (activity == null) {
            Log.w("KitsugiPlayerViewModel", "AFR preflight skipped: host activity unavailable")
            return
        }

        if (afrProbeRunning) {
            Log.d("KitsugiPlayerViewModel", "AFR preflight: already running, skipping duplicate execution")
            return
        }

        afrProbeRunning = true
        detectedFrameRateRaw = 0f
        detectedFrameRate = 0f

        val streamHeaders = headers.filterKeys { !it.equals("Range", ignoreCase = true) }
        val probeHeaders = streamHeaders.toMutableMap().apply {
            put("Connection", "close")
        }

        try {
            // ─── T1.4: DisplayCapabilities — ekran yetenek tespiti ve loglama ────
            val displaySnapshot = com.kitsugi.animelist.core.player.DisplayCapabilities.detect(activity)
            com.kitsugi.animelist.core.player.DisplayCapabilities.logSummary(displaySnapshot)

            // Ekran hem AFR hem çözünürlük değiştirmeyi desteklemiyorsa probe'u atla
            if (!displaySnapshot.supportsFrameRateSwitching && !resolutionMatchingEnabled) {
                Log.i("KitsugiPlayerViewModel", "AFR preflight: display does not support frame rate switching, skipping probe")
                return
            }
            // ─────────────────────────────────────────────────────────────────────

            val cached = com.kitsugi.animelist.core.player.FrameRateUtils.getCachedFrameRate(url, headers)
            if (cached != null) {
                Log.d("KitsugiPlayerViewModel", "AFR preflight: cache hit! Using cached FPS=${cached.snapped}")
                detectedFrameRateRaw = cached.raw
                detectedFrameRate = cached.snapped

                val prefer23976ProbeBias = cached.raw in 23.95f..23.999f
                val targetFrameRate = com.kitsugi.animelist.core.player.FrameRateUtils.refineFrameRateForDisplay(
                    activity = activity,
                    detectedFps = cached.snapped,
                    prefer23976Near24 = prefer23976ProbeBias
                )

                com.kitsugi.animelist.core.player.FrameRateUtils.matchFrameRateAndWait(
                    activity = activity,
                    frameRate = targetFrameRate,
                    videoWidth = cached.videoWidth,
                    videoHeight = cached.videoHeight,
                    resolutionMatchingEnabled = resolutionMatchingEnabled
                )
                return
            }

            val nextLibDetection = kotlinx.coroutines.withTimeoutOrNull(6000L) {
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.kitsugi.animelist.core.player.FrameRateUtils.detectFrameRateFromNextLib(
                        context = context,
                        sourceUrl = url,
                        headers = streamHeaders
                    )
                }
            }
            val detection = if (nextLibDetection != null) {
                nextLibDetection
            } else {
                Log.w(
                    "KitsugiPlayerViewModel",
                    "AFR preflight NextLib probe failed/timed out; trying extractor fallback"
                )
                kotlinx.coroutines.withTimeoutOrNull(4000L) {
                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.kitsugi.animelist.core.player.FrameRateUtils.detectFrameRateFromExtractor(
                            context = context,
                            sourceUrl = url,
                            headers = probeHeaders
                        )
                    }
                }
            }

            if (detection == null) {
                Log.w("KitsugiPlayerViewModel", "AFR preflight probe timed out/failed (NextLib + extractor fallback)")
                return
            }

            com.kitsugi.animelist.core.player.FrameRateUtils.cacheFrameRate(url, headers, detection)
            detectedFrameRateRaw = detection.raw
            detectedFrameRate = detection.snapped

            val prefer23976ProbeBias = detection.raw in 23.95f..23.999f
            val targetFrameRate = com.kitsugi.animelist.core.player.FrameRateUtils.refineFrameRateForDisplay(
                activity = activity,
                detectedFps = detection.snapped,
                prefer23976Near24 = prefer23976ProbeBias
            )

            com.kitsugi.animelist.core.player.FrameRateUtils.matchFrameRateAndWait(
                activity = activity,
                frameRate = targetFrameRate,
                videoWidth = detection.videoWidth,
                videoHeight = detection.videoHeight,
                resolutionMatchingEnabled = resolutionMatchingEnabled
            )
        } catch (e: Exception) {
            Log.e("KitsugiPlayerViewModel", "AFR preflight error: ${e.message}", e)
        } finally {
            afrProbeRunning = false
        }
    }

    private suspend fun loadEpisodes() {
        if (malId != null || aniListId != null) {
            val client = KitsugiMediaTabsClient()
            val source = if (malId != null) "mal" else "anilist"
            val id = malId ?: aniListId
            try {
                val list = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.fetchEpisodes(
                        source = source,
                        externalId = id,
                        mediaType = com.kitsugi.animelist.model.MediaType.Anime,
                        realMalId = malId,
                        context = context
                    )
                }
                episodesList = list
            } catch (e: Exception) {
                Log.e("KitsugiPlayerViewModel", "Error fetching episodes", e)
            }
        }
    }

    suspend fun fetchStreamsForEpisode(nextEp: Int): List<StreamSource> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        val repository = AddonStreamRepository(context)

        // 1. Stremio stream sources
        val stremioJob = async {
            try {
                repository.getStreamsForEpisode(malId, aniListId, seasonNum, nextEp, tmdbId)
            } catch (e: Exception) {
                Log.e("KitsugiPlayerViewModel", "Error fetching Stremio streams", e)
                emptyList<StreamSource>()
            }
        }

        // 2. CS3 plugin stream sources
        val db = KitsugiDatabase.getDatabase(context)
        val enabledCsPlugins = try {
            db.csPluginDao().getEnabledPlugins()
        } catch (_: Exception) {
            emptyList()
        }

        val alternativeTitles = listOfNotNull(titleEnglish, titleRomaji, titleNative)
        val csJobs = enabledCsPlugins.map { plugin ->
            async {
                val csStreams = mutableListOf<StreamSource>()
                try {
                    val apis = CsPluginLoader.loadExtension(context, plugin.id)
                    for (api in apis) {
                        val streams = CsStreamRunner.getStreams(
                            api = api,
                            title = animeTitle,
                            alternativeTitles = alternativeTitles,
                            year = startYear,
                            season = seasonNum,
                            episode = nextEp,
                            malId = malId,
                            aniListId = aniListId,
                            tmdbId = tmdbId
                        )
                        csStreams.addAll(streams)
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (e: Throwable) {
                    Log.e("KitsugiPlayerViewModel", "Error fetching CS plugin ${plugin.name}", e)
                }
                csStreams
            }
        }

        val stremioStreams = stremioJob.await()
        val csStreams = csJobs.map { it.await() }.flatten()

        val combined = stremioStreams + csStreams
        StreamSorter.sort(combined)
    }

    fun playEpisode(targetEp: Int, activity: android.app.Activity?, onAlternativeRequired: () -> Unit, onResolutionFailed: () -> Unit) {
        userCancelledBinge = false
        showBingeCardState = false
        nextEpisodeLoading = true
        isLoading = true
        viewModelScope.launch {
            Log.d("KitsugiPlayerViewModel", "Fetching streams for episode: S${seasonNum}E${targetEp}")
            val streams = fetchStreamsForEpisode(targetEp)
            if (streams.isNotEmpty()) {
                currentStreamSources = streams
                orchestrator.errorRecovery.hasFallback = streams.size > 1
                val sameProviderStream = streams.firstOrNull {
                    it.addonName.equals(currentAddonName, ignoreCase = true)
                }

                val targetStream = sameProviderStream ?: streams.firstOrNull { !it.url.isNullOrBlank() } ?: streams.first()
                val repository = AddonStreamRepository(context)
                
                isResolvingStream = true
                val resolvedUrl = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repository.resolveStreamUrl(targetStream)
                }
                isResolvingStream = false

                if (resolvedUrl != null) {
                    currentEpisode = targetEp
                    currentVideoUrl = resolvedUrl
                    currentAudioUrl = null
                    currentHeaders = targetStream.requestHeaders ?: emptyMap()
                    currentSubtitles = targetStream.subtitles ?: emptyList()
                    currentTitle = "${animeTitle} - Bölüm ${targetEp}"
                    currentSourceIndex = streams.indexOf(targetStream)
                    currentAddonName = targetStream.addonName
                    // AniSkip: yeni bölüm için zaman damgalarını yeniden yükle
                    if (malId != null) {
                        loadSkipIntervals(malId!!, targetEp)
                    }
                    
                    fetchAutoSubtitles()
                    
                    // Reset position in prefs for the new url
                    val sharedPrefs = context.getSharedPreferences("MyWebViewPrefs", Context.MODE_PRIVATE)
                    sharedPrefs.edit().putLong("play_pos_" + resolvedUrl.hashCode(), 0L).apply()

                    val settings = SettingsDataStore(context).settingsFlow.first()
                    viewModelScope.launch {
                        runAfrPreflightIfEnabled(
                            context = context,
                            activity = activity,
                            url = resolvedUrl,
                            headers = currentHeaders,
                            frameRateMatchingMode = settings.frameRateMatchingMode,
                            resolutionMatchingEnabled = settings.resolutionMatchingEnabled
                        )
                    }

                    playbackSource = TrailerPlaybackSource(videoUrl = resolvedUrl, audioUrl = null)

                    if (sameProviderStream == null) {
                        onAlternativeRequired()
                    }
                } else {
                    onResolutionFailed()
                }
            } else {
                nextEpisodeLoading = false
                // No streams found
            }
            nextEpisodeLoading = false
            isLoading = false
        }
    }

    fun playNextEpisode(activity: android.app.Activity?, onAlternativeRequired: () -> Unit, onResolutionFailed: () -> Unit) {
        playEpisode(currentEpisode + 1, activity, onAlternativeRequired, onResolutionFailed)
    }

    fun changeStreamSource(index: Int, stream: StreamSource, resolvedUrl: String) {
        PlayerLogger.logSourceChange(
            context   = context,
            fromAddon = currentAddonName,
            toAddon   = stream.addonName,
            newUrl    = resolvedUrl,
            title     = currentTitle
        )
        currentEpisode = currentEpisode // Keep same
        currentVideoUrl = resolvedUrl
        currentAudioUrl = null
        currentHeaders = stream.requestHeaders ?: emptyMap()
        currentSubtitles = stream.subtitles ?: emptyList()
        currentSourceIndex = index
        currentAddonName = stream.addonName
        playbackSource = TrailerPlaybackSource(videoUrl = resolvedUrl, audioUrl = null)
        fetchAutoSubtitles()
    }

    /**
     * Oynatma hatası (403, SSL, timeout vb.) sonrasında listedeki bir sonraki
     * kaynağa otomatik olarak geçer.
     *
     * Maksimum [maxAutoSwitchAttempts] deneme sonrası hasError = true yapılır.
     * Yeni bölüme geçildiğinde [resetAutoSwitch] ile sıfırlanmalıdır.
     *
     * @return true → kaynak değiştirildi, false → deneme limiti aşıldı / kaynak yok
     */
    fun tryNextSource(activity: android.app.Activity?, onSwitched: (newUrl: String, newSource: StreamSource) -> Unit): Boolean {
        val sources = currentStreamSources
        if (sources.isEmpty() || autoSwitchAttempts >= maxAutoSwitchAttempts) {
            Log.w("KitsugiPlayerViewModel", "tryNextSource: limit aşıldı veya kaynak yok ($autoSwitchAttempts/$maxAutoSwitchAttempts)")
            return false
        }
        autoSwitchAttempts++
        val nextIndex = (currentSourceIndex + 1) % sources.size
        if (nextIndex == currentSourceIndex % sources.size && sources.size == 1) {
            Log.w("KitsugiPlayerViewModel", "tryNextSource: tek kaynak var, geçiş yapılamıyor")
            return false
        }
        Log.d("KitsugiPlayerViewModel", "tryNextSource: $currentSourceIndex → $nextIndex (deneme $autoSwitchAttempts/$maxAutoSwitchAttempts)")
        isAutoSwitching = true
        isLoading = true
        viewModelScope.launch {
            try {
                val target = sources[nextIndex]
                val repository = AddonStreamRepository(context)
                val resolvedUrl = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repository.resolveStreamUrl(target)
                }
                if (resolvedUrl != null) {
                    currentVideoUrl = resolvedUrl
                    currentAudioUrl = null
                    currentHeaders = target.requestHeaders ?: emptyMap()
                    currentSubtitles = target.subtitles ?: emptyList()
                    currentSourceIndex = nextIndex
                    currentAddonName = target.addonName
                    hasError = false
                    errorDetails = null
                    // Keep hasFallback updated: still > 1 source means we can keep trying
                    orchestrator.errorRecovery.hasFallback = sources.size > 1

                    val settings = SettingsDataStore(context).settingsFlow.first()
                    viewModelScope.launch {
                        runAfrPreflightIfEnabled(
                            context = context,
                            activity = activity,
                            url = resolvedUrl,
                            headers = currentHeaders,
                            frameRateMatchingMode = settings.frameRateMatchingMode,
                            resolutionMatchingEnabled = settings.resolutionMatchingEnabled
                        )
                    }

                    playbackSource = TrailerPlaybackSource(videoUrl = resolvedUrl, audioUrl = null)
                    onSwitched(resolvedUrl, target)
                    Log.d("KitsugiPlayerViewModel", "tryNextSource: '${target.addonName}' kaynağına geçildi")
                } else {
                    Log.w("KitsugiPlayerViewModel", "tryNextSource: kaynak çözümlenemedi, tekrar deneniyor...")
                    // Recursively try the one after
                    currentSourceIndex = nextIndex
                    tryNextSource(activity, onSwitched)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("KitsugiPlayerViewModel", "tryNextSource hata: ${e.message}")
            } finally {
                isAutoSwitching = false
                isLoading = false
            }
        }
        return true
    }

    /** Yeni bölüm oynatılmaya başlandığında otomatik geçiş sayacını sıfırla. */
    fun resetAutoSwitch() {
        autoSwitchAttempts = 0
        isAutoSwitching = false
    }

    // Keep track of binge state UI
    var showBingeCardState by mutableStateOf(false)

    private var activeEngine: com.kitsugi.animelist.core.player.engine.PlayerEngine? = null

    fun setActiveEngine(engine: com.kitsugi.animelist.core.player.engine.PlayerEngine?) {
        this.activeEngine = engine
        if (engine != null) {
            // Trigger scrobble onEpisodeStarted
            viewModelScope.launch {
                val db = KitsugiDatabase.getDatabase(context)
                val entryEntity = malId?.let { db.mediaEntryDao().getByMalId(it) }
                    ?: aniListId?.let { db.mediaEntryDao().getById(it) }
                val mediaEntry = entryEntity?.toDomain() ?: com.kitsugi.animelist.model.MediaEntry(
                    id = aniListId ?: 0,
                    title = animeTitle,
                    subtitle = "",
                    type = com.kitsugi.animelist.model.MediaType.Anime,
                    status = com.kitsugi.animelist.model.WatchStatus.Watching,
                    score = null,
                    progress = currentEpisode,
                    total = null,
                    malId = malId,
                    titleEnglish = titleEnglish
                )
                orchestrator.scrobble.onEpisodeStarted(mediaEntry, currentEpisode, engine.duration)
            }
        }
    }

    var bingeCountdownSec by mutableStateOf(10)
        private set

    // --- Player Runtime Orchestrator ---
    val orchestrator: com.kitsugi.animelist.ui.screens.fullscreen.runtime.PlayerRuntimeOrchestrator = com.kitsugi.animelist.ui.screens.fullscreen.runtime.PlayerRuntimeOrchestrator(
        scope = viewModelScope,
        context = context,
        onSourceReady = { url, audio, headers, source, title ->
            currentVideoUrl = url
            currentAudioUrl = audio
            currentHeaders = headers
            currentAddonName = source.addonName
            currentTitle = title
            playbackSource = TrailerPlaybackSource(videoUrl = url, audioUrl = audio)
            fetchAutoSubtitles()
            // Re-trigger scrobble onEpisodeStarted when new source is ready
            activeEngine?.let { engine ->
                viewModelScope.launch {
                    val db = KitsugiDatabase.getDatabase(context)
                    val entryEntity = malId?.let { db.mediaEntryDao().getByMalId(it) }
                        ?: aniListId?.let { db.mediaEntryDao().getById(it) }
                    val mediaEntry = entryEntity?.toDomain() ?: com.kitsugi.animelist.model.MediaEntry(
                        id = aniListId ?: 0,
                        title = animeTitle,
                        subtitle = "",
                        type = com.kitsugi.animelist.model.MediaType.Anime,
                        status = com.kitsugi.animelist.model.WatchStatus.Watching,
                        score = null,
                        progress = currentEpisode,
                        total = null,
                        malId = malId,
                        titleEnglish = titleEnglish
                    )
                    orchestrator.scrobble.onEpisodeStarted(mediaEntry, currentEpisode, engine.duration)
                }
            }
        },
        onAfrRequired = { url, headers, mode, resolution ->
            viewModelScope.launch {
                runAfrPreflightIfEnabled(
                    context = context,
                    activity = null,
                    url = url,
                    headers = headers,
                    frameRateMatchingMode = mode,
                    resolutionMatchingEnabled = resolution
                )
            }
        },
        getEngine = { activeEngine },
        getAniListToken = { com.kitsugi.animelist.data.auth.ExternalAuthManager.getAniListToken(context) },
        onRetry = { attempt ->
            viewModelScope.launch {
                activeEngine?.let { engine ->
                    currentVideoUrl?.let { url ->
                        engine.prepare(
                            videoUrl = url,
                            audioUrl = currentAudioUrl,
                            headers = currentHeaders,
                            subtitles = currentSubtitles,
                            startPositionMs = engine.currentPosition,
                            addonName = currentAddonName
                        )
                    }
                }
            }
        },
        onFallback = {
            tryNextSource(activity = null) { _, _ -> }
        },
        onFatal = { errorCode, errorMsg ->
            hasError = true
            errorDetails = buildString {
                append("⚠️ Oynatma hatası")
                append(": $errorMsg")
                append("\n\nKaynak: ${currentAddonName ?: "bilinmeyen"}")
                append("\nHata kodu: $errorCode")
                append("\n\nTüm kaynaklar ve oynatıcılar denendi. Farklı bir kaynak seçebilir veya harici oynatıcıda açabilirsiniz.")
            }
        },
        onAutoPlayNext = {
            playNextEpisode(activity = null, onAlternativeRequired = {}, onResolutionFailed = {})
        },
        onLoop = {
            activeEngine?.let {
                it.seekTo(0L)
                it.play()
            }
        },
        onShowStillWatching = {
            // Managed by StillWatchingController state
        },
        onShowEndPrompt = {
            activeEngine?.pause()
            showBingeCardState = true
        },
        onCountdownTick = { remaining ->
            bingeCountdownSec = remaining
        }
    )

    // --- Player Skip Settings (Intro/Outro Atlama) ---
    val skipIntervals: StateFlow<List<SkipInterval>> get() = orchestrator.skip.skipIntervals
    val aniSkipLoading: StateFlow<Boolean> get() = orchestrator.skip.isLoading
    val aniSkipAutoSkip: StateFlow<Boolean> get() = orchestrator.skip.autoSkipEnabled
    val aniSkipEnabled: StateFlow<Boolean> get() = orchestrator.skip.aniSkipEnabled
    val animeSkipClientId: StateFlow<String> get() = orchestrator.skip.animeSkipClientId

    // ─── T1.3 – AudioOutputRouteDetector ────────────────────────────────────
    val activeAudioRoute: StateFlow<AudioRoute> get() = orchestrator.audio.activeAudioRoute

    fun startRouteObserver(getActiveEngine: () -> com.kitsugi.animelist.core.player.engine.PlayerEngine?) {
        orchestrator.audio.startObserving(getActiveEngine)
    }

    fun stopRouteObserver() {
        orchestrator.audio.stopObserving()
    }

    fun onPositionChanged(positionMs: Long, durationMs: Long, isPlaying: Boolean) {
        orchestrator.scrobble.onPositionUpdate(positionMs, durationMs)
        orchestrator.stillWatching.onPlaybackTick(positionMs, isPlaying)
    }

    fun onEpisodeEnded(durationMs: Long, positionMs: Long) {
        viewModelScope.launch {
            try {
                historyRepository.deleteProgress(getResolveMediaId(), currentEpisode)
            } catch (e: Exception) {
                Log.e("KitsugiPlayerViewModel", "Error deleting progress on episode ended", e)
            }

            val settings = SettingsDataStore(context).settingsFlow.first()
            val hasNext = episodesList.any { it.episodeNumber == currentEpisode + 1 } || currentEpisode < (episodesList.lastOrNull()?.episodeNumber ?: Int.MAX_VALUE)
            val hasOutro = skipIntervals.value.any { it.type == "outro" }
            val outroStart = skipIntervals.value.find { it.type == "outro" }?.startTime?.toLong()

            orchestrator.autoplay.onEpisodeEnded(
                postPlayMode = PostPlayMode.fromString(settings.postPlayMode),
                isAutoplaySettingEnabled = settings.isAutoplayEnabled,
                hasNextEpisode = hasNext,
                durationMs = durationMs,
                positionMs = positionMs,
                hasOutroSkip = hasOutro,
                outroStartSec = outroStart
            )
        }
    }

    init {
        viewModelScope.launch {
            val settings = SettingsDataStore(context).settingsFlow.first()
            orchestrator.start(
                getActiveEngine = { activeEngine },
                liveHelperEnabled = settings.liveHelperEnabled
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        orchestrator.stop()
    }

    fun updateSkipSettings(enabled: Boolean, autoSkip: Boolean, clientId: String) {
        orchestrator.skip.updateSettings(enabled, autoSkip, clientId, malId, currentEpisode)
    }

    fun loadSkipIntervals(targetMalId: Int, targetEpisode: Int) {
        orchestrator.skip.loadIntervals(targetMalId, targetEpisode)
    }

    fun fetchAutoSubtitles() {
        val currentMalId = malId
        val currentAniList = aniListId
        val currentEp = currentEpisode
        val currentS = seasonNum
        val isMovieType = isMovie || (currentS == 0 && currentEp <= 1)

        viewModelScope.launch {
            try {
                val resolvedIds = com.kitsugi.animelist.data.remote.KitsugiIdResolver.resolveIds(currentMalId, currentAniList, tmdbId)
                val imdbId = resolvedIds.imdbId
                val kitsuId = resolvedIds.kitsuId

                val type = if (isMovieType) "movie" else "series"
                var queryId = when {
                    !imdbId.isNullOrBlank() -> if (isMovieType) imdbId else "$imdbId:$currentS:$currentEp"
                    kitsuId != null -> if (isMovieType) "kitsu:$kitsuId" else "kitsu:$kitsuId:$currentEp"
                    else -> null
                }

                if (queryId == null) {
                    Log.w("KitsugiPlayerViewModel", "Altyaz\u0131 atland\u0131: ID \u00e7\u00f6z\u00fcmlenemedi (malId=$currentMalId, aniListId=$currentAniList). ARM veri taban\u0131nda bulunmuyor olabilir.")
                    return@launch
                }

                val selectedSource = currentStreamSources.getOrNull(currentSourceIndex)
                    val guessedFilename = selectedSource?.title?.takeIf { it.isNotBlank() }
                        ?: currentVideoUrl?.let { url ->
                            try {
                                val lastSeg = android.net.Uri.parse(url).lastPathSegment
                                if (!lastSeg.isNullOrBlank() && lastSeg.contains(".")) lastSeg else null
                            } catch (_: Exception) { null }
                        }
                    val cleanedFilename = guessedFilename?.substringBefore("\n")?.substringBefore("\r")?.trim()

                    Log.d("KitsugiPlayerViewModel", "Fetching subtitles for queryId=$queryId, type=$type, filename=$cleanedFilename")

                    val subRepo = com.kitsugi.animelist.data.repository.SubtitleRepositoryImpl(context)
                    val remoteSubs = subRepo.getSubtitles(
                        type = type,
                        id = queryId,
                        videoUrl = currentVideoUrl,
                        videoHeaders = currentHeaders,
                        filename = cleanedFilename
                    )

                    val settings = SettingsDataStore(context).settingsFlow.first()
                    val preferredLangs = settings.preferredSubtitleLanguages.split(",").map { it.trim().lowercase() }
                    val startupMode = settings.addonSubtitleStartupMode

                    val filteredSubs = if (startupMode == "PREFERRED_ONLY") {
                        remoteSubs.filter { sub ->
                            preferredLangs.any { pref -> com.kitsugi.animelist.core.player.PlayerSubtitleUtils.matchesLanguageCode(sub.lang, pref) }
                        }
                    } else {
                        remoteSubs
                    }


                    val processedSubs = kotlinx.coroutines.coroutineScope {
                        filteredSubs.map { sub ->
                            async(kotlinx.coroutines.Dispatchers.IO) {
                                val localFile = com.kitsugi.animelist.core.player.SubtitleFileCache.cacheSubtitle(context, sub.url)
                                if (localFile != null) {
                                    val friendlyLangName = com.kitsugi.animelist.core.player.PlayerSubtitleUtils.getFriendlyLanguageName(sub.lang)
                                    SubtitleInput(
                                        url = localFile.absolutePath,
                                        name = "$friendlyLangName (${sub.addonName})",
                                        lang = sub.lang
                                    )
                                } else {
                                    Log.w("KitsugiPlayerViewModel", "Altyaz\u0131 indirilemedi: ${sub.url}")
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }

                    if (processedSubs.isNotEmpty()) {
                        val merged = (currentSubtitles + processedSubs).distinctBy { it.url }
                        val sorted = com.kitsugi.animelist.core.player.PlayerSubtitleUtils.sortSubtitlesByPreference(
                            merged,
                            preferredLangs
                        )
                        currentSubtitles = sorted
                        Log.d("KitsugiPlayerViewModel", "Altyaz\u0131lar y\u00fcklendi: toplam=${sorted.size} (${processedSubs.size} yeni)")
                    }
            } catch (e: Exception) {
                Log.e("KitsugiPlayerViewModel", "Failed to fetch auto subtitles", e)
            }
        }
    }

    // ─── T1.7 – StillWatching + PostPlayMode + AutoplaySessionRules ──────────

    /** StillWatching prompt overlay'inin görünür olup olmadığı */
    val showStillWatchingPrompt: Boolean get() = orchestrator.stillWatching.showPrompt

    /** StillWatching geri sayımda kalan saniye */
    val stillWatchingCountdownSec: Int? get() = orchestrator.stillWatching.countdownSec

    /**
     * Oynatma tick'i — her ~ 1 saniyede bir screen tarafından çağrılır.
     * StillWatching threshold dolmuşsa prompt gösterir.
     */
    fun onPlaybackTick(positionMs: Long, isPlaying: Boolean) {
        orchestrator.stillWatching.onPlaybackTick(positionMs, isPlaying)
    }

    /**
     * Kullanıcı "Evet, hâlâ izliyorum" dedi.
     * Prompt'u kapat, session sayacını sıfırla, oynatmaya devam et.
     */
    fun onStillWatchingConfirmed() {
        orchestrator.stillWatching.onConfirmed()
    }

    /**
     * Kullanıcı "Hayır, duraksın" dedi.
     * Prompt'u kapat, oynatmayı duraklat signal'i için state bırak.
     */
    fun onStillWatchingDismissed() {
        orchestrator.stillWatching.onDismissed()
    }

    /**
     * Bir bölüm tamamlandığında çağrılır.
     * AutoplaySessionRules kontrol edilir.
     */
    fun onEpisodeCompleted(settings: com.kitsugi.animelist.data.settings.AppSettings): Boolean {
        return orchestrator.stillWatching.onEpisodeCompleted(settings)
    }

    /**
     * Kullanıcı herhangi bir girişimde bulundu (dokunma, seek, vb.)
     * StillWatching sayacını sıfırlar.
     */
    fun onUserInteraction() {
        orchestrator.stillWatching.onUserInteraction()
    }

    /**
     * Settings değiştiğinde StillWatching config'ini yenile.
     */
    fun applyStillWatchingSettings(settings: com.kitsugi.animelist.data.settings.AppSettings) {
        orchestrator.stillWatching.applySettings(settings)
    }

    private fun getResolveMediaId(): Int {
        return malId ?: aniListId ?: animeTitle.hashCode()
    }

    suspend fun getSavedPosition(mediaId: Int, episode: Int): Long {
        return try {
            val progress = historyRepository.getProgress(mediaId, episode)
            progress?.lastPositionMs ?: 0L
        } catch (e: Exception) {
            Log.e("KitsugiPlayerViewModel", "Error getting saved position from DB", e)
            0L
        }
    }

    fun saveProgress(
        mediaId: Int,
        episode: Int,
        lastPositionMs: Long,
        durationMs: Long,
        addonName: String? = null
    ) {
        if (lastPositionMs <= 0L || durationMs <= 0L) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                historyRepository.saveProgress(
                    mediaId = mediaId,
                    episode = episode,
                    lastPositionMs = lastPositionMs,
                    durationMs = durationMs,
                    addonName = addonName
                )
            } catch (e: Exception) {
                Log.e("KitsugiPlayerViewModel", "Error saving progress to DB", e)
            }
        }
    }
}
