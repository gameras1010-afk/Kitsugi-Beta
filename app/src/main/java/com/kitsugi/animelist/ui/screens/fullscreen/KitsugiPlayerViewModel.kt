package com.kitsugi.animelist.ui.screens.fullscreen

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.kitsugi.animelist.core.player.AudioDelayRouteConfig
import com.kitsugi.animelist.core.player.AudioOutputRouteDetector
import com.kitsugi.animelist.core.player.AudioRoute
import com.kitsugi.animelist.data.remote.AniSkipClient
import com.kitsugi.animelist.data.remote.AnimeSkipClient
import com.kitsugi.animelist.data.remote.SkipInterval
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.data.local.toDomain
import com.kitsugi.animelist.data.local.CustomButton
import kotlinx.coroutines.flow.update
import com.kitsugi.animelist.core.player.PostPlayMode
import kotlinx.coroutines.async as asyncSkip
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitAll
import com.kitsugi.animelist.ui.screens.fullscreen.AudioChannels


class KitsugiPlayerViewModel(application: Application) : AndroidViewModel(application) {


    private val context = application.applicationContext
    private val dataStore = SettingsDataStore(context)

    private val historyRepository by lazy {
        HistoryRepository(KitsugiDatabase.getDatabase(context).historyDao())
    }

    // ── App Settings reactive StateFlow ──────────────────────────────────────
    val appSettings: StateFlow<AppSettings> = dataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    val customButtons: StateFlow<List<CustomButton>> =
        KitsugiDatabase.getDatabase(context).customButtonDao().subscribeAll()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _primaryButton = MutableStateFlow<CustomButton?>(null)
    val primaryButton: StateFlow<CustomButton?> = _primaryButton.asStateFlow()

    private val _primaryButtonTitle = MutableStateFlow("")
    val primaryButtonTitle: StateFlow<String> = _primaryButtonTitle.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    init {
        viewModelScope.launch {
            customButtons.collect { buttons ->
                buttons.firstOrNull { it.isFavorite }?.let { fav ->
                    if (_primaryButton.value == null) {
                        _primaryButton.value = fav
                    }
                    if (_primaryButtonTitle.value.isEmpty()) {
                        setPrimaryCustomButtonTitle(fav)
                    }
                }
            }
        }
        viewModelScope.launch {
            appSettings.collect { settings ->
                _playbackSpeed.value = settings.playerSpeed
            }
        }
    }

    // State Variables - transitioned to reactive StateFlow system
    private val _currentVideoUrl = MutableStateFlow<String?>(null)
    val currentVideoUrl: StateFlow<String?> = _currentVideoUrl.asStateFlow()

    private val _currentAudioUrl = MutableStateFlow<String?>(null)
    val currentAudioUrl: StateFlow<String?> = _currentAudioUrl.asStateFlow()

    private val _currentHeaders = MutableStateFlow<Map<String, String>>(emptyMap())
    val currentHeaders: StateFlow<Map<String, String>> = _currentHeaders.asStateFlow()

    private val _currentSubtitles = MutableStateFlow<List<SubtitleInput>>(emptyList())
    val currentSubtitles: StateFlow<List<SubtitleInput>> = _currentSubtitles.asStateFlow()

    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    private val _currentSourceIndex = MutableStateFlow(-1)
    val currentSourceIndex: StateFlow<Int> = _currentSourceIndex.asStateFlow()

    private val _currentStreamSources = MutableStateFlow<List<StreamSource>>(emptyList())
    val currentStreamSources: StateFlow<List<StreamSource>> = _currentStreamSources.asStateFlow()

    private val _currentAddonName = MutableStateFlow<String?>(null)
    val currentAddonName: StateFlow<String?> = _currentAddonName.asStateFlow()

    private val _currentEpisode = MutableStateFlow(1)
    val currentEpisode: StateFlow<Int> = _currentEpisode.asStateFlow()

    private val _isMovie = MutableStateFlow(false)
    val isMovie: StateFlow<Boolean> = _isMovie.asStateFlow()

    private val _episodesList = MutableStateFlow<List<KitsugiStreamingEpisode>>(emptyList())
    val episodesList: StateFlow<List<KitsugiStreamingEpisode>> = _episodesList.asStateFlow()

    private val _userCancelledBinge = MutableStateFlow(false)
    var userCancelledBinge: Boolean
        get() = _userCancelledBinge.value
        set(value) { _userCancelledBinge.value = value }
    val userCancelledBingeFlow = _userCancelledBinge.asStateFlow()

    private val _isResolvingStream = MutableStateFlow(false)
    var isResolvingStream: Boolean
        get() = _isResolvingStream.value
        set(value) { _isResolvingStream.value = value }
    val isResolvingStreamFlow = _isResolvingStream.asStateFlow()

    private val _nextEpisodeLoading = MutableStateFlow(false)
    val nextEpisodeLoading: StateFlow<Boolean> = _nextEpisodeLoading.asStateFlow()

    private val _playbackSource = MutableStateFlow<TrailerPlaybackSource?>(null)
    val playbackSource: StateFlow<TrailerPlaybackSource?> = _playbackSource.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private val _errorDetails = MutableStateFlow<String?>(null)
    val errorDetails: StateFlow<String?> = _errorDetails.asStateFlow()

    private val _isAutoSwitching = MutableStateFlow(false)
    val isAutoSwitching: StateFlow<Boolean> = _isAutoSwitching.asStateFlow()

    // ── Controls visibility (Aniyomi-style OSD) ──────────────────────────────
    private val _controlsShown = MutableStateFlow(false)
    val controlsShown: StateFlow<Boolean> = _controlsShown.asStateFlow()

    private val _controlsResetTrigger = MutableStateFlow(0)
    val controlsResetTrigger: StateFlow<Int> = _controlsResetTrigger.asStateFlow()

    fun showControls() {
        _controlsShown.value = true
        _controlsResetTrigger.value += 1
    }

    fun hideControls() {
        _controlsShown.value = false
    }

    fun toggleControls() {
        _controlsShown.value = !_controlsShown.value
        _controlsResetTrigger.value += 1
    }

    // ── Playback position and duration reactive StateFlows ────────────────────
    private val _pos = MutableStateFlow(0L)
    val pos: StateFlow<Long> = _pos.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // ── Paused state ─────────────────────────────────────────────────────────
    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    // ── Anime title reactive StateFlow ────────────────────────────────────────
    private val _animeTitleFlow = MutableStateFlow("")
    val animeTitleFlow: StateFlow<String> = _animeTitleFlow.asStateFlow()

    // Sleep Timer (Legacy compatibility)
    private val _sleepTimerSecondsLeft = MutableStateFlow(0)
    val sleepTimerSecondsLeft: StateFlow<Int> = _sleepTimerSecondsLeft.asStateFlow()

    private var sleepTimerJob: Job? = null

    fun startSleepTimer(minutes: Int) {
        stopSleepTimer()
        if (minutes <= 0) return
        _sleepTimerSecondsLeft.value = minutes * 60
        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerSecondsLeft.value > 0) {
                delay(1000)
                _sleepTimerSecondsLeft.value -= 1
            }
            activeEngine?.pause()
        }
    }

    fun stopSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerSecondsLeft.value = 0
    }

    // --- New Aniyomi-style UI StateFlows & Coordinates ---
    private val _sheetShown = MutableStateFlow<KitsugiSheets>(KitsugiSheets.None)
    val sheetShown: StateFlow<KitsugiSheets> = _sheetShown.asStateFlow()

    private val _dismissSheet = MutableStateFlow(false)
    val dismissSheet: StateFlow<Boolean> = _dismissSheet.asStateFlow()

    fun showSheet(sheet: KitsugiSheets) {
        _sheetShown.value = sheet
        if (sheet == KitsugiSheets.None) {
            _dismissSheet.value = false
            showSeekBar()
        } else {
            hideSeekBar()
            _panelShown.value = KitsugiPanels.None
            _dialogShown.value = KitsugiDialogs.None
        }
    }

    fun dismissSheet() {
        _dismissSheet.value = true
    }

    fun resetDismissSheet() {
        _dismissSheet.value = false
    }

    private val _panelShown = MutableStateFlow<KitsugiPanels>(KitsugiPanels.None)
    val panelShown: StateFlow<KitsugiPanels> = _panelShown.asStateFlow()

    fun showPanel(panel: KitsugiPanels) {
        _panelShown.value = panel
        if (panel == KitsugiPanels.None) {
            showSeekBar()
        } else {
            hideSeekBar()
            _sheetShown.value = KitsugiSheets.None
            _dialogShown.value = KitsugiDialogs.None
        }
    }

    private val _dialogShown = MutableStateFlow<KitsugiDialogs>(KitsugiDialogs.None)
    val dialogShown: StateFlow<KitsugiDialogs> = _dialogShown.asStateFlow()

    fun showDialog(dialog: KitsugiDialogs) {
        _dialogShown.value = dialog
        if (dialog == KitsugiDialogs.None) {
            showSeekBar()
        } else {
            hideSeekBar()
            _sheetShown.value = KitsugiSheets.None
            _panelShown.value = KitsugiPanels.None
        }
    }

    private val _seekBarShown = MutableStateFlow(true)
    val seekBarShown: StateFlow<Boolean> = _seekBarShown.asStateFlow()

    fun showSeekBar() {
        if (_sheetShown.value != KitsugiSheets.None) return
        _seekBarShown.value = true
    }

    fun hideSeekBar() {
        _seekBarShown.value = false
    }

    private val _areControlsLocked = MutableStateFlow(false)
    val areControlsLocked: StateFlow<Boolean> = _areControlsLocked.asStateFlow()

    fun lockControls() {
        _areControlsLocked.value = true
    }

    fun unlockControls() {
        _areControlsLocked.value = false
    }

    private val _readAhead = MutableStateFlow(0f)
    val readAhead: StateFlow<Float> = _readAhead.asStateFlow()

    fun updateReadAhead(value: Long) {
        _readAhead.value = value.toFloat()
    }

    private val _currentDecoder = MutableStateFlow<Decoder>(Decoder.Auto)
    val currentDecoder: StateFlow<Decoder> = _currentDecoder.asStateFlow()

    fun updateDecoder(decoder: Decoder) {
        _currentDecoder.value = decoder
        // Apply to MPV engine immediately
        val mpvEngine = activeEngine as? com.kitsugi.animelist.core.player.engine.MpvPlayerEngine
        mpvEngine?.mpvView?.mpv?.setPropertyString("hwdec", decoder.value)
        viewModelScope.launch { dataStore.setMpvHwdecMode(decoder.value) }
    }

    private val _statisticsPage = MutableStateFlow(0)
    val statisticsPage: StateFlow<Int> = _statisticsPage.asStateFlow()

    fun updateStatisticsPage(page: Int) {
        val previousPage = _statisticsPage.value
        _statisticsPage.value = page
        // Aniyomi parity: toggle stats overlay when switching between off (0) and on states
        if ((page == 0) xor (previousPage == 0)) {
            activeEngine?.executeCommand(arrayOf("script-binding", "stats/display-stats-toggle"))
        }
        if (page != 0) {
            activeEngine?.executeCommand(arrayOf("script-binding", "stats/display-page-$page"))
        }
        viewModelScope.launch { dataStore.setPlayerStatisticsPage(page) }
    }

    private val _audioChannels = MutableStateFlow(AudioChannels.Auto)
    val audioChannels: StateFlow<AudioChannels> = _audioChannels.asStateFlow()

    fun updateAudioChannels(channels: AudioChannels) {
        _audioChannels.value = channels
        // Apply to MPV engine: Aniyomi pattern — ReverseStereo uses af filter, others use audio-channels
        val mpvEngine = activeEngine as? com.kitsugi.animelist.core.player.engine.MpvPlayerEngine
        runCatching {
            if (channels == AudioChannels.ReverseStereo) {
                // Clear audio-channels first
                mpvEngine?.mpvView?.mpv?.setPropertyString(AudioChannels.Auto.property, AudioChannels.Auto.value)
            } else {
                // Clear ReverseStereo af filter
                mpvEngine?.mpvView?.mpv?.setPropertyString(AudioChannels.ReverseStereo.property, "")
            }
            mpvEngine?.mpvView?.mpv?.setPropertyString(channels.property, channels.value)
        }
        viewModelScope.launch { dataStore.setAudioChannels(channels) }
    }

    private val _remainingTime = MutableStateFlow(0)
    val remainingTime: StateFlow<Int> = _remainingTime.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer(seconds: Int) {
        timerJob?.cancel()
        _remainingTime.value = seconds
        _sleepTimerSecondsLeft.value = seconds
        if (seconds < 1) return
        timerJob = viewModelScope.launch {
            for (time in seconds downTo 0) {
                _remainingTime.value = time
                _sleepTimerSecondsLeft.value = time
                delay(1000)
            }
            activeEngine?.pause()
        }
    }

    private val _chapters = MutableStateFlow<List<com.kitsugi.animelist.ui.screens.fullscreen.controls.components.IndexedSegment>>(emptyList())
    val chapters: StateFlow<List<com.kitsugi.animelist.ui.screens.fullscreen.controls.components.IndexedSegment>> = _chapters.asStateFlow()

    fun updateChapters(chapterList: List<com.kitsugi.animelist.ui.screens.fullscreen.controls.components.IndexedSegment>) {
        _chapters.value = chapterList
    }

    private val _currentChapter = MutableStateFlow<com.kitsugi.animelist.ui.screens.fullscreen.controls.components.IndexedSegment?>(null)
    val currentChapter: StateFlow<com.kitsugi.animelist.ui.screens.fullscreen.controls.components.IndexedSegment?> = _currentChapter.asStateFlow()

    fun updateChapter(chapter: com.kitsugi.animelist.ui.screens.fullscreen.controls.components.IndexedSegment?) {
        _currentChapter.value = chapter
    }

    val doubleTapSeekAmount = MutableStateFlow(0)
    val gestureSeekAmount = MutableStateFlow<Pair<Int, Int>?>(null)
    val seekText = MutableStateFlow<String?>(null)
    val isSeekingForwards = MutableStateFlow(false)

    val isVolumeSliderShown = MutableStateFlow(false)
    val isBrightnessSliderShown = MutableStateFlow(false)
    val currentBrightness = MutableStateFlow(-2.0f)
    val currentMPVVolume = MutableStateFlow(100)

    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    val maxVolume by lazy { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) }
    val currentVolume by lazy { MutableStateFlow(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)) }
    val volumeBoostCap: Int get() = appSettings.value.volumeBoostCap

    fun updateSeekAmount(amount: Int) {
        doubleTapSeekAmount.value = amount
    }

    fun updateSeekText(text: String?) {
        seekText.value = text
    }

    fun handleLeftDoubleTap() {
        isSeekingForwards.value = false
        val step = appSettings.value.doubleTapSeekSeconds
        val newAmount = doubleTapSeekAmount.value - step
        doubleTapSeekAmount.value = newAmount
        seekBy(-step * 1000L)
    }

    fun handleRightDoubleTap() {
        isSeekingForwards.value = true
        val step = appSettings.value.doubleTapSeekSeconds
        val newAmount = doubleTapSeekAmount.value + step
        doubleTapSeekAmount.value = newAmount
        seekBy(step * 1000L)
    }

    fun handleCenterDoubleTap() {
        togglePlay()
    }

    fun seekBy(offsetMs: Long) {
        val current = _pos.value
        val dur = _duration.value
        val target = (current + offsetMs).coerceIn(0L, dur)
        activeEngine?.seekTo(target)
        _pos.value = target
    }

    fun changeVolumeTo(volume: Int) {
        val newVolume = volume.coerceIn(0..maxVolume)
        audioManager.setStreamVolume(
            android.media.AudioManager.STREAM_MUSIC,
            newVolume,
            0,
        )
        currentVolume.value = newVolume
    }

    fun changeMPVVolumeTo(volume: Int) {
        currentMPVVolume.value = volume
        activeEngine?.setVolume(volume / 100f)
    }

    fun changeBrightnessTo(brightness: Float) {
        currentBrightness.value = brightness.coerceIn(-0.75f, 1f)
        val activity = context.findActivity() ?: return
        val params = activity.window.attributes
        params.screenBrightness = brightness.coerceIn(0f, 1f)
        activity.window.attributes = params
    }

    fun getSystemBrightness(): Float {
        return try {
            android.provider.Settings.System.getInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS
            ) / 255f
        } catch (e: Exception) {
            0.5f
        }
    }

    private fun Context.findActivity(): android.app.Activity? {
        var ctx = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun displayVolumeSlider() {
        isVolumeSliderShown.value = true
    }

    fun displayBrightnessSlider() {
        isBrightnessSliderShown.value = true
    }

    val playerUpdate = MutableStateFlow<KitsugiPlayerUpdates>(KitsugiPlayerUpdates.None)

    // ── TASK_042: PlaybackState StateFlow ────────────────────────────────────
    // NuvioTV PlayerRuntimeController.playerState pattern referans alındı.
    // PlayerEngine.Listener.onStateChanged olaylarını üst seviye PlaybackState'e dönüştürür.
    private val _playerState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playerState: StateFlow<PlaybackState> = _playerState.asStateFlow()

    /** PlayerEngine durumunu PlaybackState'e çevirir ve StateFlow'u günceller. */
    fun updatePlayerState(engineState: com.kitsugi.animelist.core.player.engine.PlayerEngine.State, isPlaying: Boolean) {
        _paused.value = !isPlaying
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

    private val _detectedFrameRateRaw = MutableStateFlow(0f)
    val detectedFrameRateRaw: StateFlow<Float> = _detectedFrameRateRaw.asStateFlow()

    private val _detectedFrameRate = MutableStateFlow(0f)
    val detectedFrameRate: StateFlow<Float> = _detectedFrameRate.asStateFlow()

    private val _afrProbeRunning = MutableStateFlow(false)
    val afrProbeRunning: StateFlow<Boolean> = _afrProbeRunning.asStateFlow()

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

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

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
        if (_isInitialized.value && lastInitializedKey == initKey) return
        _isInitialized.value = true
        lastInitializedKey = initKey

        this.malId = malId
        this.aniListId = aniListId
        this.tmdbId = tmdbId
        this.seasonNum = season
        _isMovie.value = isMovie || (season == 0 && episode <= 1)
        this.animeTitle = animeTitle
        this.titleEnglish = titleEnglish
        this.titleRomaji = titleRomaji
        this.titleNative = titleNative
        this.startYear = startYear
        _animeTitleFlow.value = animeTitle

        _currentVideoUrl.value = videoUrl
        _currentAudioUrl.value = audioUrl
        _currentHeaders.value = requestHeaders
        _currentSubtitles.value = initialSubtitles
        _currentStreamSources.value = streamSources
        _currentSourceIndex.value = initialIndex
        _currentTitle.value = title
        _currentEpisode.value = episode
        if (initialIndex in streamSources.indices) {
            _currentAddonName.value = streamSources[initialIndex].addonName
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
            _isLoading.value = true
            _hasError.value = false
            when {
                videoId != null -> {
                    try {
                        val src = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            InAppYouTubeExtractor()
                                .extractPlaybackSource("https://www.youtube.com/watch?v=$videoId")
                        }
                        if (src != null) {
                            _playbackSource.value = src
                        } else {
                            _hasError.value = true
                        }
                    } catch (e: Exception) {
                        _hasError.value = true
                        _errorDetails.value = e.message
                    } finally {
                        _isLoading.value = false
                    }
                }
                _currentVideoUrl.value != null -> {
                    val settings = SettingsDataStore(context).settingsFlow.first()
                    viewModelScope.launch {
                        runAfrPreflightIfEnabled(
                            context = context,
                            activity = activity,
                            url = _currentVideoUrl.value!!,
                            headers = _currentHeaders.value,
                            frameRateMatchingMode = settings.frameRateMatchingMode,
                            resolutionMatchingEnabled = settings.resolutionMatchingEnabled
                        )
                    }
                    _playbackSource.value = TrailerPlaybackSource(videoUrl = _currentVideoUrl.value!!, audioUrl = _currentAudioUrl.value)
                    _isLoading.value = false
                }
                else -> {
                    _hasError.value = true
                    _isLoading.value = false
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
            _detectedFrameRateRaw.value = 0f
            _detectedFrameRate.value = 0f
            _afrProbeRunning.value = false
            return
        }

        if (activity == null) {
            Log.w("KitsugiPlayerViewModel", "AFR preflight skipped: host activity unavailable")
            return
        }

        if (_afrProbeRunning.value) {
            Log.d("KitsugiPlayerViewModel", "AFR preflight: already running, skipping duplicate execution")
            return
        }

        _afrProbeRunning.value = true
        _detectedFrameRateRaw.value = 0f
        _detectedFrameRate.value = 0f

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
                _detectedFrameRateRaw.value = cached.raw
                _detectedFrameRate.value = cached.snapped

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
            _detectedFrameRateRaw.value = detection.raw
            _detectedFrameRate.value = detection.snapped

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
            _afrProbeRunning.value = false
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
                _episodesList.value = list
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
        _showBingeCardState.value = false
        _nextEpisodeLoading.value = true
        _isLoading.value = true
        viewModelScope.launch {
            Log.d("KitsugiPlayerViewModel", "Fetching streams for episode: S${seasonNum}E${targetEp}")
            val streams = fetchStreamsForEpisode(targetEp)
            if (streams.isNotEmpty()) {
                _currentStreamSources.value = streams
                orchestrator.errorRecovery.hasFallback = streams.size > 1
                val sameProviderStream = streams.firstOrNull {
                    it.addonName.equals(_currentAddonName.value, ignoreCase = true)
                }

                val targetStream = sameProviderStream ?: streams.firstOrNull { !it.url.isNullOrBlank() } ?: streams.first()
                val repository = AddonStreamRepository(context)

                _isResolvingStream.value = true
                val resolvedUrl = try {
                    kotlinx.coroutines.withTimeoutOrNull(30000L) {
                        withContext(kotlinx.coroutines.Dispatchers.IO) {
                            repository.resolveStreamUrl(targetStream)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("KitsugiPlayerViewModel", "playEpisode: akış çözümleme hatası", e)
                    null
                } finally {
                    isResolvingStream = false
                }

                if (resolvedUrl != null) {
                    _currentEpisode.value = targetEp
                    _currentVideoUrl.value = resolvedUrl
                    _currentAudioUrl.value = null
                    _currentHeaders.value = targetStream.requestHeaders ?: emptyMap()
                    _currentSubtitles.value = targetStream.subtitles ?: emptyList()
                    _currentTitle.value = "${animeTitle} - Bölüm ${targetEp}"
                    _currentSourceIndex.value = streams.indexOf(targetStream)
                    _currentAddonName.value = targetStream.addonName
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
                            headers = _currentHeaders.value,
                            frameRateMatchingMode = settings.frameRateMatchingMode,
                            resolutionMatchingEnabled = settings.resolutionMatchingEnabled
                        )
                    }

                    _playbackSource.value = TrailerPlaybackSource(videoUrl = resolvedUrl, audioUrl = null)

                    if (sameProviderStream == null) {
                        onAlternativeRequired()
                    }
                } else {
                    onResolutionFailed()
                }
            } else {
                _nextEpisodeLoading.value = false
                // No streams found
            }
            _nextEpisodeLoading.value = false
            _isLoading.value = false
        }
    }

    fun playNextEpisode(activity: android.app.Activity?, onAlternativeRequired: () -> Unit, onResolutionFailed: () -> Unit) {
        playEpisode(_currentEpisode.value + 1, activity, onAlternativeRequired, onResolutionFailed)
    }

    fun changeStreamSource(index: Int, stream: StreamSource, resolvedUrl: String) {
        PlayerLogger.logSourceChange(
            context   = context,
            fromAddon = _currentAddonName.value,
            toAddon   = stream.addonName,
            newUrl    = resolvedUrl,
            title     = _currentTitle.value
        )
        _currentVideoUrl.value = resolvedUrl
        _currentAudioUrl.value = null
        _currentHeaders.value = stream.requestHeaders ?: emptyMap()
        _currentSubtitles.value = stream.subtitles ?: emptyList()
        _currentSourceIndex.value = index
        _currentAddonName.value = stream.addonName
        _playbackSource.value = TrailerPlaybackSource(videoUrl = resolvedUrl, audioUrl = null)
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
        val sources = _currentStreamSources.value
        if (sources.isEmpty() || autoSwitchAttempts >= maxAutoSwitchAttempts) {
            Log.w("KitsugiPlayerViewModel", "tryNextSource: limit aşıldı veya kaynak yok ($autoSwitchAttempts/$maxAutoSwitchAttempts)")
            return false
        }
        autoSwitchAttempts++
        val nextIndex = (_currentSourceIndex.value + 1) % sources.size
        if (nextIndex == _currentSourceIndex.value % sources.size && sources.size == 1) {
            Log.w("KitsugiPlayerViewModel", "tryNextSource: tek kaynak var, geçiş yapılamıyor")
            return false
        }
        Log.d("KitsugiPlayerViewModel", "tryNextSource: ${_currentSourceIndex.value} → $nextIndex (deneme $autoSwitchAttempts/$maxAutoSwitchAttempts)")
        _isAutoSwitching.value = true
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val target = sources[nextIndex]
                val repository = AddonStreamRepository(context)
                val resolvedUrl = try {
                    kotlinx.coroutines.withTimeoutOrNull(30000L) {
                        withContext(kotlinx.coroutines.Dispatchers.IO) {
                            repository.resolveStreamUrl(target)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("KitsugiPlayerViewModel", "tryNextSource: çözümleme hatası: ${e.message}")
                    null
                }
                if (resolvedUrl != null) {
                    _currentVideoUrl.value = resolvedUrl
                    _currentAudioUrl.value = null
                    _currentHeaders.value = target.requestHeaders ?: emptyMap()
                    _currentSubtitles.value = target.subtitles ?: emptyList()
                    _currentSourceIndex.value = nextIndex
                    _currentAddonName.value = target.addonName
                    _hasError.value = false
                    _errorDetails.value = null
                    // Keep hasFallback updated: still > 1 source means we can keep trying
                    orchestrator.errorRecovery.hasFallback = sources.size > 1

                    val settings = SettingsDataStore(context).settingsFlow.first()
                    viewModelScope.launch {
                        runAfrPreflightIfEnabled(
                            context = context,
                            activity = activity,
                            url = resolvedUrl,
                            headers = _currentHeaders.value,
                            frameRateMatchingMode = settings.frameRateMatchingMode,
                            resolutionMatchingEnabled = settings.resolutionMatchingEnabled
                        )
                    }

                    _playbackSource.value = TrailerPlaybackSource(videoUrl = resolvedUrl, audioUrl = null)
                    onSwitched(resolvedUrl, target)
                    Log.d("KitsugiPlayerViewModel", "tryNextSource: '${target.addonName}' kaynağına geçildi")
                } else {
                    Log.w("KitsugiPlayerViewModel", "tryNextSource: kaynak çözümlenemedi, tekrar deneniyor...")
                    // Recursively try the one after
                    _currentSourceIndex.value = nextIndex
                    tryNextSource(activity, onSwitched)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("KitsugiPlayerViewModel", "tryNextSource hata: ${e.message}")
            } finally {
                _isAutoSwitching.value = false
                _isLoading.value = false
            }
        }
        return true
    }

    /** Yeni bölüm oynatılmaya başlandığında otomatik geçiş sayacını sıfırla. */
    fun resetAutoSwitch() {
        autoSwitchAttempts = 0
        _isAutoSwitching.value = false
    }

    // Keep track of binge state UI
    private val _showBingeCardState = MutableStateFlow(false)
    val showBingeCardState: StateFlow<Boolean> = _showBingeCardState.asStateFlow()

    private val _bingeCountdownSec = MutableStateFlow(10)
    val bingeCountdownSec: StateFlow<Int> = _bingeCountdownSec.asStateFlow()

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
                    progress = _currentEpisode.value,
                    total = null,
                    malId = malId,
                    titleEnglish = titleEnglish
                )
                orchestrator.scrobble.onEpisodeStarted(mediaEntry, _currentEpisode.value, engine.duration)
            }
        }
    }

    private var activeEngine: com.kitsugi.animelist.core.player.engine.PlayerEngine? = null

    // --- Player Runtime Orchestrator ---
    val orchestrator: com.kitsugi.animelist.ui.screens.fullscreen.runtime.PlayerRuntimeOrchestrator = com.kitsugi.animelist.ui.screens.fullscreen.runtime.PlayerRuntimeOrchestrator(
        scope = viewModelScope,
        context = context,
        onSourceReady = { url, audio, headers, source, title ->
            _currentVideoUrl.value = url
            _currentAudioUrl.value = audio
            _currentHeaders.value = headers
            _currentAddonName.value = source.addonName
            _currentTitle.value = title
            _playbackSource.value = TrailerPlaybackSource(videoUrl = url, audioUrl = audio)
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
                        progress = _currentEpisode.value,
                        total = null,
                        malId = malId,
                        titleEnglish = titleEnglish
                    )
                    orchestrator.scrobble.onEpisodeStarted(mediaEntry, _currentEpisode.value, engine.duration)
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
                    _currentVideoUrl.value?.let { url ->
                        engine.prepare(
                            videoUrl = url,
                            audioUrl = _currentAudioUrl.value,
                            headers = _currentHeaders.value,
                            subtitles = _currentSubtitles.value,
                            startPositionMs = engine.currentPosition,
                            addonName = _currentAddonName.value
                        )
                    }
                }
            }
        },
        onFallback = {
            tryNextSource(activity = null) { _, _ -> }
        },
        onFatal = { errorCode, errorMsg ->
            _hasError.value = true
            _errorDetails.value = buildString {
                append("⚠️ Oynatma hatası")
                append(": $errorMsg")
                append("\n\nKaynak: ${_currentAddonName.value ?: "bilinmeyen"}")
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
            _showBingeCardState.value = true
        },
        onCountdownTick = { remaining ->
            _bingeCountdownSec.value = remaining
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
        _pos.value = positionMs
        _duration.value = durationMs
        orchestrator.scrobble.onPositionUpdate(positionMs, durationMs)
        orchestrator.stillWatching.onPlaybackTick(positionMs, isPlaying)
    }

    // ── Engine-delegate playback controls ─────────────────────────────────────
    fun play() {
        activeEngine?.play()
        _paused.value = false
    }

    fun pause() {
        activeEngine?.pause()
        _paused.value = true
    }

    fun togglePlay() {
        if (activeEngine?.isPlaying == true) pause() else play()
    }

    fun seekTo(positionMs: Long) {
        activeEngine?.seekTo(positionMs)
        _pos.value = positionMs
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        activeEngine?.setPlaybackSpeed(speed)
        viewModelScope.launch { dataStore.setPlayerSpeed(speed) }
    }

    fun setAutoPlay(enabled: Boolean) {
        viewModelScope.launch { dataStore.setAutoplayEnabled(enabled) }
    }

    fun setAspectMode(mode: com.kitsugi.animelist.core.player.PlayerAspectMode) {
        viewModelScope.launch { dataStore.setAspectMode(mode.name) }
    }

    // ── Episode navigation helpers ─────────────────────────────────────────────
    val hasPreviousEpisode: StateFlow<Boolean> = _currentEpisode
        .map { it > 1 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val hasNextEpisode: StateFlow<Boolean> = combine(_episodesList, _currentEpisode) { episodes, current ->
        episodes.any { it.episodeNumber == current + 1 } ||
        current < (episodes.lastOrNull()?.episodeNumber ?: Int.MAX_VALUE)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun onEpisodeEnded(durationMs: Long, positionMs: Long) {
        viewModelScope.launch {
            try {
                historyRepository.deleteProgress(getResolveMediaId(), _currentEpisode.value)
            } catch (e: Exception) {
                Log.e("KitsugiPlayerViewModel", "Error deleting progress on episode ended", e)
            }

            val settings = SettingsDataStore(context).settingsFlow.first()
            val hasNext = _episodesList.value.any { it.episodeNumber == _currentEpisode.value + 1 } || _currentEpisode.value < (_episodesList.value.lastOrNull()?.episodeNumber ?: Int.MAX_VALUE)
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
        orchestrator.skip.updateSettings(enabled, autoSkip, clientId, malId, _currentEpisode.value)
    }

    fun loadSkipIntervals(targetMalId: Int, targetEpisode: Int) {
        orchestrator.skip.loadIntervals(targetMalId, targetEpisode)
    }

    fun fetchAutoSubtitles() {
        val currentMalId = malId
        val currentAniList = aniListId
        val currentEp = _currentEpisode.value
        val currentS = seasonNum
        val isMovieType = _isMovie.value || (currentS == 0 && currentEp <= 1)

        viewModelScope.launch {
            try {
                val resolvedIds = com.kitsugi.animelist.data.remote.KitsugiIdResolver.resolveIds(currentMalId, currentAniList, tmdbId)
                val imdbId = resolvedIds.imdbId
                val kitsuId = resolvedIds.kitsuId

                val type = if (isMovieType) "movie" else "series"
                val queryIds = mutableListOf<String>()

                // 1. IMDB ID varsa ekle (en yaygın format, çoğu addon destekler)
                if (!imdbId.isNullOrBlank()) {
                    queryIds.add(if (isMovieType) imdbId else "$imdbId:$currentS:$currentEp")
                }
                // 2. Kitsu ID varsa ekle (anime-specific eklentiler için)
                if (kitsuId != null) {
                    queryIds.add(if (isMovieType) "kitsu:$kitsuId" else "kitsu:$kitsuId:$currentEp")
                }
                // 3. Anime başlığı ile PARALEL arama — her zaman eklenir.
                // Bazı altyazı eklentileri (türkçealtyazi.org gibi) isim aramasını da
                // destekleyebilir. ID'ler varsa ekstra coverage sağlar, yoksa tek seçenektir.
                // 0 maliyetle çalışır çünkü diğer sorgularla zaten paralel atılıyor.
                val titleQuery = animeTitle.trim().takeIf { it.isNotBlank() }
                if (titleQuery != null) {
                    queryIds.add(if (isMovieType) titleQuery else "$titleQuery:$currentS:$currentEp")
                }

                if (queryIds.isEmpty()) {
                    Log.w("KitsugiPlayerViewModel", "Altyazı atlandı: ID ve başlık çözümlenemedi (malId=$currentMalId, aniListId=$currentAniList).")
                    return@launch
                }

                Log.d("KitsugiPlayerViewModel", "Altyazı sorgulama: queryIds=$queryIds, type=$type (imdb=$imdbId, kitsu=$kitsuId, title=$titleQuery)")

                val selectedSource = _currentStreamSources.value.getOrNull(_currentSourceIndex.value)
                val guessedFilename = selectedSource?.title?.takeIf { it.isNotBlank() }
                    ?: _currentVideoUrl.value?.let { url ->
                        try {
                            val lastSeg = android.net.Uri.parse(url).lastPathSegment
                            if (!lastSeg.isNullOrBlank() && lastSeg.contains(".")) lastSeg else null
                        } catch (_: Exception) { null }
                    }
                val cleanedFilename = guessedFilename?.substringBefore("\n")?.substringBefore("\r")?.trim()

                Log.d("KitsugiPlayerViewModel", "Fetching subtitles: queryIds=$queryIds, type=$type, filename=$cleanedFilename")

                val subRepo = com.kitsugi.animelist.data.repository.SubtitleRepositoryImpl(context)
                // Tüm queryId'ler için paralel sorgu — IMDB + kitsu + title hepsi aynı anda
                val remoteSubs = kotlinx.coroutines.coroutineScope {
                    queryIds.map { queryId ->
                        async(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                subRepo.getSubtitles(
                                    type = type,
                                    id = queryId,
                                    videoUrl = _currentVideoUrl.value,
                                    videoHeaders = _currentHeaders.value,
                                    filename = cleanedFilename
                                )
                            } catch (e: Exception) {
                                Log.e("KitsugiPlayerViewModel", "Failed to fetch subtitles for queryId=$queryId", e)
                                emptyList()
                            }
                        }
                    }.awaitAll().flatten().distinctBy { it.url }
                }.toMutableList()

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
                        val merged = (_currentSubtitles.value + processedSubs).distinctBy { it.url }
                        val sorted = com.kitsugi.animelist.core.player.PlayerSubtitleUtils.sortSubtitlesByPreference(
                            merged,
                            preferredLangs
                        )
                        _currentSubtitles.value = sorted
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

    suspend fun getSavedPosition(mediaId: Int, episode: Int, addonName: String? = null): Long {
        return try {
            val progress = historyRepository.getProgress(mediaId, episode)
            if (progress != null && (addonName == null || progress.addonName == addonName)) {
                progress.lastPositionMs
            } else {
                0L
            }
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

    // ─── Screenshot and Art Management ──────────────────────────────────────────

    private val _screenshotShowSubtitles = MutableStateFlow(true)
    val screenshotShowSubtitles: StateFlow<Boolean> = _screenshotShowSubtitles.asStateFlow()

    fun toggleScreenshotShowSubtitles(show: Boolean) {
        _screenshotShowSubtitles.value = show
    }

    val hasSubTracks: StateFlow<Boolean> = _currentSubtitles.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isLocalSource: Boolean
        get() = _currentVideoUrl.value?.let { !it.startsWith("http://") && !it.startsWith("https://") } ?: true

    fun takeScreenshot(cachePath: String, showSubtitles: Boolean): java.io.InputStream? {
        return activeEngine?.takeScreenshot(cachePath, showSubtitles)
    }

    fun saveImage(imageStream: () -> java.io.InputStream, timePos: Int?) {
        viewModelScope.launch {
            try {
                val timeStr = timePos?.let { formatMs(it.toLong()) } ?: System.currentTimeMillis().toString()
                val filename = "Kitsugi_${animeTitle}_${timeStr.replace(":", "-")}.png"
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Kitsugi")
                    }
                }
                val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        imageStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    Toast.makeText(context, "Ekran görüntüsü kaydedildi: $filename", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Ekran görüntüsü kaydedilemedi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "saveImage failed", e)
                Toast.makeText(context, "Kaydetme başarısız: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareImage(imageStream: () -> java.io.InputStream, timePos: Int?) {
        viewModelScope.launch {
            try {
                val cacheDir = java.io.File(context.cacheDir, "shared_images").apply { mkdirs() }
                val tempFile = java.io.File(cacheDir, "mpv_screenshot_share.png")
                tempFile.outputStream().use { out ->
                    imageStream().use { input ->
                        input.copyTo(out)
                    }
                }

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.kitsugi.animelist.fileprovider",
                    tempFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = Intent.createChooser(intent, "Ekran Görüntüsünü Paylaş").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "shareImage failed", e)
                Toast.makeText(context, "Paylaşım başarısız: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setAsArt(artType: ArtType, imageStream: () -> java.io.InputStream) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val db = KitsugiDatabase.getDatabase(context)
            val entryEntity = malId?.let { db.mediaEntryDao().getByMalId(it) }
                ?: aniListId?.let { db.mediaEntryDao().getById(it) }
                ?: db.mediaEntryDao().getAll().firstOrNull { it.title.equals(animeTitle, ignoreCase = true) }

            if (entryEntity == null) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "Resmi ayarlamak için önce bu animeyi kütüphanenize eklemelisiniz.", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            try {
                when (artType) {
                    ArtType.Cover -> {
                        val dir = java.io.File(context.filesDir, "covers").apply { mkdirs() }
                        val file = java.io.File(dir, "${entryEntity.id}.png")
                        file.outputStream().use { out ->
                            imageStream().use { input -> input.copyTo(out) }
                        }
                        // Update in DB
                        val updated = entryEntity.copy(imageUrl = "file://${file.absolutePath}")
                        db.mediaEntryDao().update(updated)
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, "Kapak resmi güncellendi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    ArtType.Background -> {
                        // Set wallpaper
                        val wallpaperManager = android.app.WallpaperManager.getInstance(context)
                        imageStream().use { input ->
                            wallpaperManager.setStream(input)
                        }
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, "Duvar kağıdı güncellendi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    ArtType.Thumbnail -> {
                        val dir = java.io.File(context.filesDir, "thumbnails").apply { mkdirs() }
                        val file = java.io.File(dir, "${entryEntity.id}_${_currentEpisode.value}.png")
                        file.outputStream().use { out ->
                            imageStream().use { input -> input.copyTo(out) }
                        }
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, "Küçük resim güncellendi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("KitsugiPlayerViewModel", "setAsArt failed", e)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "Resim ayarlanırken hata oluştu: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatMs(ms: Long): String {
        val totalSecs = ms / 1000
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (hours > 0) {
            String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun setPrimaryCustomButtonTitle(button: CustomButton) {
        val title = if (button.name.length <= 8) button.name else button.name.take(7) + "…"
        _primaryButtonTitle.value = title
    }

    fun executeCustomButton(button: CustomButton) {
        activeEngine?.executeCommand(arrayOf("script-message", "call_button_${button.id}"))
    }

    fun executeCustomButtonLongPress(button: CustomButton) {
        activeEngine?.executeCommand(arrayOf("script-message", "call_button_${button.id}_long"))
    }

    fun seekToWithText(positionSec: Int, text: String?) {
        val targetMs = positionSec * 1000L
        activeEngine?.seekTo(targetMs)
        _pos.value = targetMs
        if (!text.isNullOrBlank()) {
            playerUpdate.value = KitsugiPlayerUpdates.ShowText(text)
        }
    }

    fun seekByWithText(deltaSec: Int, text: String?) {
        val current = _pos.value
        val dur = _duration.value
        val target = (current + deltaSec * 1000L).coerceIn(0L, dur)
        activeEngine?.seekTo(target)
        _pos.value = target
        if (!text.isNullOrBlank()) {
            playerUpdate.value = KitsugiPlayerUpdates.ShowText(text)
        }
    }

    fun changeEpisode(previous: Boolean) {
        val target = if (previous) _currentEpisode.value - 1 else _currentEpisode.value + 1
        val activity = context.findActivity()
        playEpisode(target, activity, {}, {})
    }

    fun handleLuaInvocation(property: String, value: String) {
        val data = value
            .removePrefix("\"")
            .removeSuffix("\"")
            .ifEmpty { return }

        val mpvEngine = activeEngine as? com.kitsugi.animelist.core.player.engine.MpvPlayerEngine

        when (property.substringAfterLast("/")) {
            "show_text" -> playerUpdate.value = KitsugiPlayerUpdates.ShowText(data)
            "toggle_ui" -> {
                when (data) {
                    "show" -> showControls()
                    "toggle" -> toggleControls()
                    "hide" -> {
                        _sheetShown.value = KitsugiSheets.None
                        _panelShown.value = KitsugiPanels.None
                        _dialogShown.value = KitsugiDialogs.None
                        hideControls()
                    }
                }
            }
            "show_panel" -> {
                when (data) {
                    "subtitle_settings" -> showPanel(KitsugiPanels.SubtitleSettings)
                    "subtitle_delay" -> showPanel(KitsugiPanels.SubtitleDelay)
                    "audio_delay" -> showPanel(KitsugiPanels.AudioDelay)
                    "video_filters" -> showPanel(KitsugiPanels.VideoFilters)
                }
            }
            "set_button_title" -> {
                _primaryButtonTitle.value = data
            }
            "reset_button_title" -> {
                customButtons.value.firstOrNull { it.isFavorite }?.let {
                    setPrimaryCustomButtonTitle(it)
                }
            }
            "switch_episode" -> {
                when (data) {
                    "n" -> changeEpisode(false)
                    "p" -> changeEpisode(true)
                }
            }
            "launch_int_picker" -> {
                val parts = data.split("|")
                if (parts.size >= 6) {
                    val title = parts[0]
                    val nameFormat = parts[1]
                    val start = parts[2]
                    val stop = parts[3]
                    val step = parts[4]
                    val pickerProperty = parts[5]

                    val defaultValue = mpvEngine?.mpvView?.mpv?.getPropertyInt(pickerProperty) ?: 0
                    showDialog(
                        KitsugiDialogs.IntegerPicker(
                            defaultValue = defaultValue,
                            minValue = start.toIntOrNull() ?: 0,
                            maxValue = stop.toIntOrNull() ?: 100,
                            step = step.toIntOrNull() ?: 1,
                            nameFormat = nameFormat,
                            title = title,
                            onChange = { mpvEngine?.mpvView?.mpv?.setPropertyInt(pickerProperty, it) },
                            onDismissRequest = { showDialog(KitsugiDialogs.None) }
                        )
                    )
                }
            }
            "pause" -> {
                when (data) {
                    "pause" -> pause()
                    "unpause" -> play()
                    "pauseunpause" -> if (activeEngine?.isPlaying == true) pause() else play()
                }
            }
            "toggle_button" -> {
                fun showButton() {
                    if (_primaryButton.value == null) {
                        _primaryButton.update {
                            customButtons.value.firstOrNull { it.isFavorite }
                        }
                    }
                }

                when (data) {
                    "show" -> showButton()
                    "hide" -> _primaryButton.update { null }
                    "toggle" -> if (_primaryButton.value == null) showButton() else _primaryButton.update { null }
                }
            }
            "software_keyboard" -> {
                val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                when (data) {
                    "show" -> {
                        val activity = context.findActivity()
                        val view = activity?.currentFocus ?: activity?.window?.decorView
                        if (view != null) {
                            inputMethodManager.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
                        }
                    }
                    "hide" -> {
                        val activity = context.findActivity()
                        val view = activity?.currentFocus
                        if (view != null) {
                            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                        }
                    }
                    "toggle" -> {
                        inputMethodManager.toggleSoftInput(android.view.inputmethod.InputMethodManager.SHOW_FORCED, 0)
                    }
                }
            }
        }

        mpvEngine?.mpvView?.mpv?.setPropertyString(property, "")
    }

    fun getAnimeSkipIntroLength(): Int {
        val sharedPrefs = context.getSharedPreferences("kitsugi_player_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getInt("intro_length_${getResolveMediaId()}", 85)
    }

    fun setAnimeSkipIntroLength(length: Int) {
        val sharedPrefs = context.getSharedPreferences("kitsugi_player_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("intro_length_${getResolveMediaId()}", length).apply()
        val mpvEngine = activeEngine as? com.kitsugi.animelist.core.player.engine.MpvPlayerEngine
        mpvEngine?.mpvView?.mpv?.let { mpv ->
            val currentVal = mpv.getPropertyInt("user-data/current-anime/intro-length") ?: -1
            if (currentVal != length) {
                mpv.setPropertyInt("user-data/current-anime/intro-length", length)
            }
        }
    }

    fun addAudio(uri: android.net.Uri) {
        val url = uri.toString()
        val isContentUri = url.startsWith("content://")
        val path = if (isContentUri) uri.openContentFd(context) else url
        if (path == null) return
        val name = if (isContentUri) uri.getFileName(context) else null
        if (name == null) {
            activeEngine?.executeCommand(arrayOf("audio-add", path, "cached"))
        } else {
            activeEngine?.executeCommand(arrayOf("audio-add", path, "cached", name))
        }
    }

    fun addSubtitle(uri: android.net.Uri) {
        val url = uri.toString()
        val isContentUri = url.startsWith("content://")
        val path = if (isContentUri) uri.openContentFd(context) else url
        if (path == null) return
        val name = if (isContentUri) uri.getFileName(context) else null
        if (name == null) {
            activeEngine?.executeCommand(arrayOf("sub-add", path, "cached"))
        } else {
            activeEngine?.executeCommand(arrayOf("sub-add", path, "cached", name))
        }
    }

    private fun android.net.Uri.openContentFd(context: Context): String? {
        return context.contentResolver.openFileDescriptor(this, "r")?.detachFd()?.let {
            `is`.xyz.mpv.Utils.findRealPath(it)?.also { _ ->
                android.os.ParcelFileDescriptor.adoptFd(it).close()
            } ?: "fd://$it"
        }
    }

    private fun android.net.Uri.getFileName(context: Context): String? {
        return context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else null
        }
    }
}

