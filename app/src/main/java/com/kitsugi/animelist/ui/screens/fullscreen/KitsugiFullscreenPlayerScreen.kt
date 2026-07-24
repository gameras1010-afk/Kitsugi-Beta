package com.kitsugi.animelist.ui.screens.fullscreen

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.kitsugi.animelist.data.cloudstream.CsVideoInterceptorFactory
import com.kitsugi.animelist.core.player.SubtitleInput
import com.kitsugi.animelist.data.repository.AddonStreamRepository
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.data.trailer.YoutubeChunkedDataSourceFactory
import com.kitsugi.animelist.ui.screens.fullscreen.components.EpisodesSidePanel
import com.kitsugi.animelist.ui.screens.fullscreen.components.SubtitleSelectionOverlay
import com.kitsugi.animelist.ui.screens.fullscreen.components.AudioSelectionOverlay
import com.kitsugi.animelist.ui.screens.fullscreen.components.StreamInfoOverlay
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerSkipSettingsOverlay
import com.kitsugi.animelist.ui.screens.fullscreen.components.PauseOverlay
import com.kitsugi.animelist.ui.screens.fullscreen.components.PostPlayBingeCard
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerTopBar
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerCenterControl
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerSeekBar
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerBottomActions
import com.kitsugi.animelist.ui.screens.fullscreen.components.SpeedSelectionOverlay
import com.kitsugi.animelist.ui.screens.fullscreen.components.FeedbackBubble
import com.kitsugi.animelist.ui.screens.fullscreen.components.SourcesSelectionOverlay
import com.kitsugi.animelist.ui.screens.fullscreen.components.QualitySelectionOverlay
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerInlineLoadingOverlay
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerLoadingView
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerErrorView
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerBufferingView
import com.kitsugi.animelist.ui.screens.fullscreen.components.SubtitleStyleSettings
import com.kitsugi.animelist.ui.screens.fullscreen.components.StreamInfoData
import com.kitsugi.animelist.ui.screens.fullscreen.components.MetaCastMember
import com.kitsugi.animelist.ui.screens.fullscreen.components.TorrentOverlay
import com.kitsugi.animelist.ui.screens.fullscreen.components.TrackOption
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerPanel
import com.kitsugi.animelist.ui.screens.fullscreen.components.GestureSwipeSide
import android.util.Log
import com.kitsugi.animelist.core.player.PlayerLogger
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kitsugi.animelist.core.player.engine.PlayerEngine
import com.kitsugi.animelist.core.player.AudioOutputRouteDetector
import com.kitsugi.animelist.core.player.AudioRoute
import com.kitsugi.animelist.core.player.engine.PlayerEngineType
import com.kitsugi.animelist.core.player.engine.PlayerEngineSelector
import com.kitsugi.animelist.core.player.engine.Media3PlayerEngine
import com.kitsugi.animelist.core.player.engine.MpvPlayerEngine
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerGestureConfig
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerGestureOverlay
import com.kitsugi.animelist.ui.screens.fullscreen.components.VolumeProgressBar
import com.kitsugi.animelist.ui.screens.fullscreen.components.BrightnessProgressBar
import com.kitsugi.animelist.ui.screens.fullscreen.components.rememberPlayerGestureController
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import com.kitsugi.animelist.ui.screens.fullscreen.components.PreviewGenerator
import com.kitsugi.animelist.ui.screens.fullscreen.components.formatMs

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun KitsugiFullscreenPlayerScreen(
    videoId: String?,
    videoUrl: String?,
    audioUrl: String?,
    title: String,
    requestHeaders: Map<String, String> = emptyMap(),
    initialSubtitles: List<SubtitleInput> = emptyList(),
    streamSources: List<StreamSource> = emptyList(),
    initialIndex: Int = -1,
    malId: Int? = null,
    aniListId: Int? = null,
    tmdbId: Int? = null,
    season: Int = 1,
    episode: Int = 1,
    animeTitle: String = "",
    posterUrl: String? = null,
    titleEnglish: String? = null,
    titleRomaji: String? = null,
    titleNative: String? = null,
    startYear: Int? = null,
    description: String? = null,
    castList: List<MetaCastMember> = emptyList(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) break
            ctx = ctx.baseContext
        }
        ctx as? android.app.Activity
    }
    val scope   = rememberCoroutineScope()
    
    var isInPipMode by remember { mutableStateOf(activity?.isInPictureInPictureMode ?: false) }
    DisposableEffect(activity) {
        val compActivity = activity as? androidx.activity.ComponentActivity
        if (compActivity == null) return@DisposableEffect onDispose {}
        val listener = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            isInPipMode = info.isInPictureInPictureMode
        }
        compActivity.addOnPictureInPictureModeChangedListener(listener)
        onDispose {
            compActivity.removeOnPictureInPictureModeChangedListener(listener)
        }
    }
    
    val mainFocusRequester = remember { FocusRequester() }
    // Tracks whether the focusable root Box is fully attached to the layout tree.
    // We must NOT call requestFocus() before this is true — doing so throws IllegalStateException.
    var isFocusTargetAttached by remember { mutableStateOf(false) }

    LaunchedEffect(isFocusTargetAttached) {
        if (isFocusTargetAttached) {
            try {
                mainFocusRequester.requestFocus()
            } catch (e: Exception) {
                Log.e("KitsugiPlayer", "Failed to request initial focus", e)
            }
        }
    }
    
    val sharedPrefs = remember {
        context.getSharedPreferences("MyWebViewPrefs", Context.MODE_PRIVATE)
    }

    val dataStore = remember { SettingsDataStore(context.applicationContext) }
    val appSettingsState = dataStore.settingsFlow.collectAsState(initial = null)
    val appSettings = appSettingsState.value
    var isSettingsLoaded by remember { mutableStateOf(false) }

    // ViewModel Integration
    val viewModel: KitsugiPlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    LaunchedEffect(videoId, videoUrl, audioUrl, title, initialIndex, episode, tmdbId) {
        viewModel.initialize(
            videoId = videoId,
            videoUrl = videoUrl,
            audioUrl = audioUrl,
            title = title,
            requestHeaders = requestHeaders,
            initialSubtitles = initialSubtitles,
            streamSources = streamSources,
            initialIndex = initialIndex,
            malId = malId,
            aniListId = aniListId,
            tmdbId = tmdbId,
            season = season,
            episode = episode,
            animeTitle = animeTitle,
            titleEnglish = titleEnglish,
            titleRomaji = titleRomaji,
            titleNative = titleNative,
            startYear = startYear,
            activity = activity
        )
    }

    val currentVideoUrl = viewModel.currentVideoUrl
    val currentAudioUrl = viewModel.currentAudioUrl
    val currentHeaders = viewModel.currentHeaders
    val currentSubtitles = viewModel.currentSubtitles
    val currentTitle = viewModel.currentTitle
    val currentSourceIndex = viewModel.currentSourceIndex
    val currentStreamSources = viewModel.currentStreamSources
    val currentAddonName = viewModel.currentAddonName
    val currentEpisode = viewModel.currentEpisode
    val episodesList = viewModel.episodesList
    val userCancelledBinge = viewModel.userCancelledBinge
    val isResolvingStream = viewModel.isResolvingStream
    val nextEpisodeLoading = viewModel.nextEpisodeLoading
    val playbackSource = viewModel.playbackSource
    val isLoading = viewModel.isLoading
    val hasError = viewModel.hasError
    val errorDetails = viewModel.errorDetails
    val isAutoSwitching = viewModel.isAutoSwitching

    // AniSkip state
    val skipIntervals by viewModel.skipIntervals.collectAsState()
    val aniSkipAutoSkip by viewModel.aniSkipAutoSkip.collectAsState()
    val aniSkipEnabled by viewModel.aniSkipEnabled.collectAsState()
    val animeSkipClientId by viewModel.animeSkipClientId.collectAsState()


    // Sliding Panel options & control
    var activePanel by remember { mutableStateOf(PlayerPanel.NONE) }
    
    LaunchedEffect(activePanel, isFocusTargetAttached) {
        if (activePanel == PlayerPanel.NONE && isFocusTargetAttached) {
            try {
                mainFocusRequester.requestFocus()
            } catch (e: Exception) {
                Log.e("KitsugiPlayer", "Failed to restore focus after panel close", e)
            }
        }
    }
    
    // Autoplay / binge card (replaces full-screen countdown)
    val isAutoplayEnabled = appSettings?.isAutoplayEnabled ?: true
    val showBingeCard = viewModel.showBingeCardState
    var bingeCountdownSec by remember { mutableStateOf(10) }

    // Track selections
    var textTrackOptions by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var audioTrackOptions by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var isSubtitleDisabled by remember { mutableStateOf(false) }

    // Subtitle style & delay
    var subtitleStyle by remember { mutableStateOf(SubtitleStyleSettings()) }
    var subtitleDelayMs by remember { mutableStateOf(0L) }

    // Audio delay & boost
    var audioDelayMs by remember { mutableStateOf(0L) }
    var audioBoostLevel by remember { mutableStateOf(0f) }

    var topBarHeightState by remember { mutableStateOf(0f) }
    var bottomControlsHeightState by remember { mutableStateOf(0f) }

    var currentAspectMode by remember { mutableStateOf(com.kitsugi.animelist.core.player.PlayerAspectMode.ORIGINAL) }
    var screenOrientationState by remember { mutableStateOf(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR) }

    LaunchedEffect(appSettings) {
        if (appSettings != null && !isSettingsLoaded) {
            subtitleStyle = SubtitleStyleSettings(
                size = appSettings.defaultSubtitleSize,
                textColor = appSettings.defaultSubtitleColor,
                bold = appSettings.subtitleBold,
                outlineEnabled = appSettings.subtitleOutlineEnabled
            )
            audioDelayMs = appSettings.defaultAudioDelayMs
            audioBoostLevel = appSettings.defaultAudioBoost
            currentAspectMode = runCatching { com.kitsugi.animelist.core.player.PlayerAspectMode.valueOf(appSettings.aspectMode) }.getOrDefault(com.kitsugi.animelist.core.player.PlayerAspectMode.ORIGINAL)
            isSettingsLoaded = true
        }
    }

    // Stream diagnostics
    var streamInfoData by remember { mutableStateOf(StreamInfoData(playerEngine = "ExoPlayer")) }

    val mediaIdForHistory = remember(malId, aniListId, animeTitle) {
        malId ?: aniListId ?: animeTitle.hashCode()
    }
    var savedPos by remember(mediaIdForHistory, currentEpisode) { mutableStateOf<Long?>(null) }
    var showResumeDialog by remember(mediaIdForHistory, currentEpisode) { mutableStateOf(false) }
    var pendingResumePos by remember { mutableStateOf(0L) }
    var hasCheckedResume by remember(mediaIdForHistory, currentEpisode) { mutableStateOf(false) }

    LaunchedEffect(mediaIdForHistory, currentEpisode) {
        savedPos = viewModel.getSavedPosition(mediaIdForHistory, currentEpisode)
    }

    fun playEpisode(targetEp: Int) {
        viewModel.resetAutoSwitch()
        viewModel.playEpisode(
            targetEp = targetEp,
            activity = activity,
            onAlternativeRequired = {
                activePanel = PlayerPanel.SOURCES
                Toast.makeText(context, "Aynı kaynak bulunamadı. Diğer kaynaklar listeleniyor.", Toast.LENGTH_LONG).show()
            },
            onResolutionFailed = {
                activePanel = PlayerPanel.SOURCES
                Toast.makeText(context, "Seçilen kaynak çözümlenemedi. Lütfen listeden başka bir kaynak seçin.", Toast.LENGTH_LONG).show()
            }
        )
    }

    fun playNextEpisode() {
        viewModel.playNextEpisode(
            activity = activity,
            onAlternativeRequired = {
                activePanel = PlayerPanel.SOURCES
                Toast.makeText(context, "Aynı kaynak bulunamadı. Diğer kaynaklar listeleniyor.", Toast.LENGTH_LONG).show()
            },
            onResolutionFailed = {
                activePanel = PlayerPanel.SOURCES
                Toast.makeText(context, "Seçilen kaynak çözümlenemedi. Lütfen listeden başka bir kaynak seçin.", Toast.LENGTH_LONG).show()
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { isFocusTargetAttached = true },
        contentAlignment = Alignment.Center
    ) {
        when {
            isAutoSwitching -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text("⚡ Yedek kaynak deneniyor...", color = Color.White.copy(alpha = 0.85f))
                    Text(
                        text = "Kaynak: ${currentAddonName.orEmpty()} → sonraki",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Sadece playbackSource yokken yüklenme göster; settings asla video oynatmayı ENGELLEMEMELI
            isLoading && playbackSource == null -> {
                PlayerLoadingView()
            }

            hasError || playbackSource == null -> {
                PlayerErrorView(
                    message = errorDetails,
                    canOpenExternal = !currentVideoUrl.isNullOrBlank(),
                    onBack = onBack,
                    onOpenExternal = {
                        if (!currentVideoUrl.isNullOrBlank()) {
                            PlayerLogger.logExternalPlayerLaunch(
                                context   = context,
                                url       = currentVideoUrl,
                                addonName = currentAddonName,
                                title     = currentTitle,
                                manual    = true
                            )
                            KitsugiFullscreenPlayerActivity.launchExternalPlayer(
                                context = context,
                                videoUrl = currentVideoUrl,
                                title = currentTitle,
                                positionMs = savedPos ?: 0L,
                                headers = currentHeaders,
                                subtitles = currentSubtitles
                            )
                            onBack()
                        }
                    },
                    onRetry = { playEpisode(currentEpisode) },
                    onSwitchSource = { activePanel = PlayerPanel.SOURCES }
                )
            }

            else -> {
                val source = playbackSource

                // settings null ise varsayılan kullan — settings hiçbir zaman oynatmayı engellemez
                val safeSettings = appSettings ?: com.kitsugi.animelist.data.settings.AppSettings()

                val formattedTitle = remember(currentTitle, safeSettings.showPlayerTitle, safeSettings.titleLimitType) {
                    if (!safeSettings.showPlayerTitle) {
                        ""
                    } else {
                        when (safeSettings.titleLimitType) {
                            "LIMIT_20" -> if (currentTitle.length > 20) currentTitle.take(20) + "..." else currentTitle
                            "LIMIT_40" -> if (currentTitle.length > 40) currentTitle.take(40) + "..." else currentTitle
                            else -> currentTitle
                        }
                    }
                }

                // Determine which engine type to use based on the CURRENT source URL.
                // This is computed once per source URL — engine only rebuilds if the type changes
                // (e.g. user switches MPV setting), NOT on every playbackSource reference change.
                // Previously, remember(context, source) caused rebuild whenever loadPlaybackSource()
                // set a new TrailerPlaybackSource() object (even with identical URLs), which triggered
                // DisposableEffect onDispose and prematurely released ExoPlayer mid-playback.
                var activeEngineType by remember(source?.videoUrl) {
                    mutableStateOf(
                        PlayerEngineSelector.selectEngine(
                            settings = safeSettings,
                            videoUrl = source!!.videoUrl,
                            isCS = currentStreamSources.getOrNull(currentSourceIndex)?.isCS == true
                        )
                    )
                }

                val fallbackCoordinator = remember(source?.videoUrl) {
                    com.kitsugi.animelist.core.player.engine.PlayerFallbackCoordinator(
                        maxAttempts = 3,
                        listener = object : com.kitsugi.animelist.core.player.PlayerManagerListener {
                            override fun onPlayerSwitched(
                                from: com.kitsugi.animelist.core.player.engine.PlayerEngineType,
                                to: com.kitsugi.animelist.core.player.engine.PlayerEngineType
                            ) {
                                Log.d("KitsugiPlayerDebug", "FallbackCoordinator: Switched engine from $from to $to")
                                activeEngineType = to
                                Toast.makeText(
                                    context,
                                    "⚠️ Oynatma hatası — Oynatıcı motoru değiştiriliyor...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            override fun onFatalError(errorCode: Int, errorMsg: String) {
                                Log.e("KitsugiPlayerDebug", "FallbackCoordinator: Fatal fallback error: $errorMsg")
                                viewModel.orchestrator.errorRecovery.onPlaybackError(errorCode, errorMsg)
                            }
                        }
                    )
                }

                val playerEngine = remember(context, activeEngineType) {
                    when (activeEngineType) {
                        PlayerEngineType.MPV -> {
                            MpvPlayerEngine(context, safeSettings)
                        }
                        else -> {
                            Media3PlayerEngine(context, safeSettings)
                        }
                    }
                }

                // ─── Audio Output Route Detector (T1.3) ─────────────────────────────
                val routeDetector = remember(context) { AudioOutputRouteDetector(context) }
                val activeAudioRoute by routeDetector.observeRouteChanges().collectAsState(initial = AudioRoute.SPEAKER)

                LaunchedEffect(playerEngine, activeAudioRoute, audioDelayMs, safeSettings.audioDelayPerRouteJson) {
                    val json = safeSettings.audioDelayPerRouteJson
                    val routeDelay = AudioOutputRouteDetector.getDelayForRoute(json, activeAudioRoute)
                    playerEngine.setAudioDelay(audioDelayMs + routeDelay)
                    Log.d("KitsugiPlayer", "Audio delay updated: manual=$audioDelayMs, route=$activeAudioRoute, routeDelay=$routeDelay")
                }

                var currentPosition by remember { mutableStateOf(0L) }
                var duration by remember { mutableStateOf(0L) }
                var isPlayingState by remember { mutableStateOf(true) }
                var isBufferingState by remember { mutableStateOf(false) }
                var isPlaybackEnded by remember { mutableStateOf(false) }
                
                var currentSpeed by remember { mutableStateOf(1.0f) }
                var aspectFeedback by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(playerEngine, currentAspectMode) {
                    playerEngine.setAspectMode(currentAspectMode)
                }

                // ─── Gesture Controller (T2.1 + T2.7) ─────────────────────────────────
                val gestureConfig = remember(safeSettings) {
                    PlayerGestureConfig(
                        volumeGestureEnabled    = safeSettings.gestureVolumeEnabled,
                        brightnessGestureEnabled = safeSettings.gestureBrightnessEnabled,
                        zoomGestureEnabled      = safeSettings.gestureZoomEnabled,
                        doubleTapSeekSeconds    = safeSettings.doubleTapSeekSeconds,
                        holdSpeedMultiplier     = safeSettings.holdSpeedMultiplier,
                        // TASK_050 — NuvioTV PlayerGestureDetector.scrollSensitivity karşılığı
                        scrollSensitivity       = safeSettings.gestureScrollSensitivity
                    )
                }
                val gestureController = rememberPlayerGestureController(
                    config           = gestureConfig,
                    onVolumeChange   = { /* AudioManager already updated inside controller */ },
                    onBrightnessChange = { /* WindowManager already updated inside controller */ },
                    onSeek           = { deltaMs ->
                        val pos = playerEngine.currentPosition
                        val dur = playerEngine.duration
                        playerEngine.seekTo((pos + deltaMs).coerceIn(0L, dur))
                    },
                    onSpeedChange    = { speed ->
                        currentSpeed = speed
                        playerEngine.setPlaybackSpeed(speed)
                    }
                )
                val gestureState by gestureController.gestureState

                // ─── PiP Panel and Controls Reset (T2.3) ──────────────────────────────
                LaunchedEffect(isInPipMode) {
                    if (isInPipMode) {
                        activePanel = PlayerPanel.NONE
                        // showTopBar is declared inside inner else block — reset via activePanel only
                    }
                }

                // ─── Gesture drag state tracking ──────────────────────────────────────
                var isGestureSwipeActive by remember { mutableStateOf(false) }
                var gestureSwipeSide by remember { mutableStateOf<GestureSwipeSide>(GestureSwipeSide.NONE) }
                var swipeStartY by remember { mutableStateOf(0f) }
                var swipeTotalDy by remember { mutableStateOf(0f) }

                LaunchedEffect(aspectFeedback) {
                    if (aspectFeedback != null) {
                        delay(1000)
                        aspectFeedback = null
                    }
                }

                // Periodic Progress and Room DB Position Saver
                LaunchedEffect(playerEngine, isPlayingState, mediaIdForHistory, currentEpisode, currentAddonName) {
                    if (isPlayingState) {
                        while (true) {
                            currentPosition = playerEngine.currentPosition
                            duration = playerEngine.duration.coerceAtLeast(0L)
                            if (currentPosition > 0L) {
                                viewModel.saveProgress(
                                    mediaId = mediaIdForHistory,
                                    episode = currentEpisode,
                                    lastPositionMs = currentPosition,
                                    durationMs = duration,
                                    addonName = currentAddonName
                                )
                            }
                            viewModel.onPositionChanged(currentPosition, duration, playerEngine.isPlaying)
                            delay(1000)
                        }
                    }
                }

                val nextEpNum = currentEpisode + 1
                val nextEpTitle = episodesList.find { it.episodeNumber == nextEpNum }?.title ?: "Bölüm $nextEpNum"
                val nextEpThumbnail = episodesList.find { it.episodeNumber == nextEpNum }?.thumbnail

                val shouldShowBingeCard = isAutoplayEnabled && duration > 0L && currentPosition >= duration * 0.95f && !userCancelledBinge && (episodesList.isEmpty() || currentEpisode < (episodesList.lastOrNull()?.episodeNumber ?: Int.MAX_VALUE))

                LaunchedEffect(shouldShowBingeCard, userCancelledBinge) {
                    if (shouldShowBingeCard && !userCancelledBinge) {
                        bingeCountdownSec = 10
                        while (bingeCountdownSec > 0) {
                            delay(1000)
                            bingeCountdownSec -= 1
                        }
                        playNextEpisode()
                    }
                }

                val isTorrentStream = remember(currentSourceIndex, currentStreamSources) {
                    val currentSource = currentStreamSources.getOrNull(currentSourceIndex)
                    currentSource?.infoHash != null || currentSource?.url?.contains("magnet") == true
                }

                LaunchedEffect(playerEngine, audioBoostLevel) {
                    playerEngine.setVolume(1.0f + audioBoostLevel)
                }

                LaunchedEffect(playbackSource, isLoading, hasError, isAutoSwitching) {
                    Log.d("KitsugiPlayerDebug", "UI State: playbackSource=${playbackSource != null}, isLoading=$isLoading, hasError=$hasError, isAutoSwitching=$isAutoSwitching")
                }

                // ─── Buffering Watchdog (12s Timeout) ──────────────────────────────────
                var bufferingWatchdogJob by remember { mutableStateOf<Job?>(null) }
                LaunchedEffect(isBufferingState, activeEngineType, currentVideoUrl) {
                    if (isBufferingState) {
                        bufferingWatchdogJob?.cancel()
                        bufferingWatchdogJob = scope.launch {
                            delay(12_000L)
                            if (isBufferingState) {
                                Log.w("KitsugiPlayerDebug", "Buffering watchdog: Stream stuck buffering for 12s. Forcing fallback!")
                                Toast.makeText(context, "⚠️ Yayın sunucusu yanıt vermiyor — Alternatif kaynağa geçiliyor...", Toast.LENGTH_SHORT).show()
                                val mpvEnabled = safeSettings.playerPreference.equals("MPV", ignoreCase = true)
                                val nextEngine = fallbackCoordinator.getFallbackEngine(
                                    currentEngine = activeEngineType,
                                    errorCode = 5004,
                                    mpvEnabled = mpvEnabled
                                )
                                if (nextEngine == null) {
                                    viewModel.orchestrator.errorRecovery.onPlaybackError(5004, "Arabelleğe alma zaman aşımına uğradı (12sn)")
                                }
                            }
                        }
                    } else {
                        bufferingWatchdogJob?.cancel()
                        bufferingWatchdogJob = null
                    }
                }

                DisposableEffect(playerEngine) {
                    Log.d("KitsugiPlayerDebug", "playerEngine DisposableEffect initialized")
                    viewModel.setActiveEngine(playerEngine)
                    val listener = object : PlayerEngine.Listener {
                        override fun onStateChanged(state: PlayerEngine.State) {
                            Log.d("KitsugiPlayerDebug", "onStateChanged: state=$state, isPlaying=${playerEngine.isPlaying}")
                            isPlayingState = playerEngine.isPlaying
                            isBufferingState = state == PlayerEngine.State.BUFFERING
                            // TASK_042 — PlaybackState StateFlow güncelle
                            viewModel.updatePlayerState(state, playerEngine.isPlaying)
                            if (state == PlayerEngine.State.READY) {
                                isPlaybackEnded = false
                                duration = playerEngine.duration.coerceAtLeast(0L)
                                Log.d("KitsugiPlayerDebug", "Player READY: duration=$duration, savedPos=$savedPos")
                                // Only show resume dialog if saved position is >= 10s AND not near end
                                // savedPos < 10000ms (10s) → treat as "not started yet", skip dialog
                                if ((savedPos ?: 0L) >= 10_000L && !hasCheckedResume) {
                                    hasCheckedResume = true
                                    if ((savedPos ?: 0L) < duration - 10_000L) {
                                        playerEngine.pause()
                                        pendingResumePos = savedPos ?: 0L
                                        showResumeDialog = true
                                    }
                                } else if (!hasCheckedResume) {
                                    // savedPos < 10s — mark as checked without pause/dialog
                                    hasCheckedResume = true
                                }
                                playerEngine.setPlaybackSpeed(currentSpeed)
                                viewModel.orchestrator.errorRecovery.onPlaybackReady()
                            } else if (state == PlayerEngine.State.ENDED) {
                                Log.d("KitsugiPlayerDebug", "Player ENDED: calling onEpisodeEnded")
                                isPlaybackEnded = true
                                viewModel.onEpisodeEnded(duration, currentPosition)
                            }
                        }

                        override fun onPlaybackError(errorCode: Int, errorMsg: String, cause: Throwable?) {
                            Log.e("KitsugiPlayerDebug", "onPlaybackError: code=$errorCode, message=$errorMsg", cause)
                            // TASK_042 — PlayerState.Error'a geç
                            viewModel.setPlayerError(errorCode, errorMsg)
                            PlayerLogger.logPlaybackError(
                                context   = context,
                                url       = currentVideoUrl,
                                addonName = currentAddonName,
                                title     = currentTitle,
                                errorCode = errorCode,
                                errorMsg  = errorMsg,
                                cause     = cause
                            )

                            // Try falling back engine (MEDIA3 -> MPV -> EXTERNAL)
                            val mpvEnabled = safeSettings.playerPreference.equals("MPV", ignoreCase = true)
                            val nextEngine = fallbackCoordinator.getFallbackEngine(
                                currentEngine = activeEngineType,
                                errorCode = errorCode,
                                mpvEnabled = mpvEnabled
                            )

                            if (nextEngine == null) {
                                // Engine fallback exhausted for this source link — trigger source fallback to next link
                                viewModel.orchestrator.errorRecovery.onPlaybackError(errorCode, errorMsg)
                            }
                        }

                        override fun onTracksChanged(
                            audioTracks: List<TrackOption>,
                            subtitleTracks: List<TrackOption>
                        ) {
                            Log.d("KitsugiPlayerDebug", "onTracksChanged: audioTracks=${audioTracks.size}, subTracks=${subtitleTracks.size}")
                            audioTrackOptions = audioTracks
                            textTrackOptions = subtitleTracks
                            isSubtitleDisabled = playerEngine.isSubtitleDisabled
                            streamInfoData = playerEngine.activeStreamInfo
                        }
                    }
                    playerEngine.addListener(listener)
                    onDispose {
                        Log.d("KitsugiPlayerDebug", "playerEngine DisposableEffect onDispose called")
                        viewModel.setActiveEngine(null)
                        val finalPos = playerEngine.currentPosition
                        if (finalPos > 0L) {
                            viewModel.saveProgress(
                                mediaId = mediaIdForHistory,
                                episode = currentEpisode,
                                lastPositionMs = finalPos,
                                durationMs = duration,
                                addonName = currentAddonName
                            )
                        }
                        playerEngine.release()

                        if (activity != null && appSettings?.frameRateMatchingMode == com.kitsugi.animelist.data.settings.FrameRateMatchingMode.START_STOP) {
                            com.kitsugi.animelist.core.player.FrameRateUtils.restoreOriginalDisplayMode(activity)
                        } else {
                            com.kitsugi.animelist.core.player.FrameRateUtils.clearOriginalDisplayMode()
                        }
                    }
                }

                // KEY FIX: Use URL string + headers hash instead of source object reference.
                // PlaybackSource objects are frequently recreated by ViewModel (e.g. on recomposition)
                // even when the URL hasn't changed. Using the object reference as key caused
                // unnecessary player restarts every ~10s, resetting position to 0 each time.
                var lastPreparedUrl by remember { mutableStateOf<String?>(null) }
                val prepareKey = remember(source?.videoUrl, currentHeaders, currentSubtitles) {
                    Triple(source?.videoUrl, currentHeaders.hashCode(), currentSubtitles.map { it.url }.hashCode())
                }
                LaunchedEffect(playerEngine, prepareKey) {
                    val safeVideoUrl = source?.videoUrl ?: return@LaunchedEffect
                    isPlaybackEnded = false
                    Log.d("KitsugiPlayerDebug", "LaunchedEffect(source) prepare() starting. url=$safeVideoUrl")
                    PlayerLogger.logPlaybackStart(
                        context   = context,
                        url       = safeVideoUrl,
                        addonName = currentAddonName,
                        title     = currentTitle,
                        isCS      = currentStreamSources.getOrNull(currentSourceIndex)?.isCS == true
                    )
                    
                    playerEngine.setPlaybackSpeed(currentSpeed)

                    val activeSource = currentStreamSources.getOrNull(currentSourceIndex)
                    val startPos = if (safeVideoUrl == lastPreparedUrl) playerEngine.currentPosition else 0L
                    lastPreparedUrl = safeVideoUrl

                    playerEngine.prepare(
                        videoUrl = safeVideoUrl,
                        audioUrl = source?.audioUrl,
                        headers = currentHeaders,
                        subtitles = currentSubtitles,
                        startPositionMs = startPos,
                        addonName = currentAddonName,
                        isCS = activeSource?.isCS == true,
                        streamTitle = activeSource?.title ?: activeSource?.name,
                        qualityValue = activeSource?.qualityValue
                    )
                    Log.d("KitsugiPlayer", "prepare() ÇAĞRILDI: url=$safeVideoUrl addon=${currentAddonName} startPos=$startPos")
                }

                var seekFeedback  by remember { mutableStateOf<String?>(null) }
                // true = kullanıcı sağ tarafa tıkladı → gösterge sola (TopStart) gider
                var seekFeedbackOnRightSide by remember { mutableStateOf(true) }
                var playPauseIcon by remember { mutableStateOf<ImageVector?>(null) }
                var showTopBar     by remember { mutableStateOf(false) }
                var resetHideTimerTrigger by remember { mutableStateOf(0) }

                fun resetHideTimer() {
                    resetHideTimerTrigger++
                }

                LaunchedEffect(showTopBar, isPlayingState, activePanel, resetHideTimerTrigger) {
                    if (showTopBar && isPlayingState && activePanel == PlayerPanel.NONE) {
                        delay(5000)
                        showTopBar = false
                    }
                }

                LaunchedEffect(showTopBar) {
                    if (!showTopBar && isFocusTargetAttached) {
                        try {
                            mainFocusRequester.requestFocus()
                        } catch (e: Exception) {
                            Log.e("KitsugiPlayer", "Failed to request focus on controls hide", e)
                        }
                    }
                }

                LaunchedEffect(isPlayingState) {
                    if (!isPlayingState) {
                        showTopBar = true
                    } else {
                        showTopBar = true
                        resetHideTimer()
                    }
                }

                LaunchedEffect(seekFeedback) {
                    if (seekFeedback != null) { delay(650); seekFeedback = null }
                }
                LaunchedEffect(playPauseIcon) {
                    if (playPauseIcon != null) { delay(500); playPauseIcon = null }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(mainFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            
                            if (event.key == Key.Back || event.key == Key.Escape) {
                                return@onPreviewKeyEvent when {
                                    activePanel != PlayerPanel.NONE -> {
                                        activePanel = PlayerPanel.NONE
                                        mainFocusRequester.requestFocus()
                                        true
                                    }
                                    showTopBar -> {
                                        showTopBar = false
                                        mainFocusRequester.requestFocus()
                                        true
                                    }
                                    else -> {
                                        onBack()
                                        true
                                    }
                                }
                            }
                            
                            if (activePanel != PlayerPanel.NONE) return@onPreviewKeyEvent false
                            
                            if (showTopBar) {
                                resetHideTimer()
                                return@onPreviewKeyEvent false
                            }
                            
                            when (event.key) {
                                Key.DirectionCenter, Key.Enter -> {
                                    if (playerEngine.isPlaying) {
                                        playerEngine.pause()
                                        playPauseIcon = Icons.Rounded.Pause
                                    } else {
                                        playerEngine.play()
                                        playPauseIcon = Icons.Rounded.PlayArrow
                                    }
                                    showTopBar = true
                                    resetHideTimer()
                                    true
                                }
                                Key.DirectionLeft -> {
                                    val pos = playerEngine.currentPosition
                                    playerEngine.seekTo((pos - 10000).coerceAtLeast(0))
                                    seekFeedback = "-10s"
                                    seekFeedbackOnRightSide = false // sol tuş → geri → gösterge sağda (TopEnd)
                                    resetHideTimer()
                                    true
                                }
                                Key.DirectionRight -> {
                                    val pos = playerEngine.currentPosition
                                    val dur = playerEngine.duration
                                    playerEngine.seekTo((pos + 10000).coerceAtMost(dur))
                                    seekFeedback = "+10s"
                                    seekFeedbackOnRightSide = true // sağ tuş → ileri → gösterge solda (TopStart)
                                    resetHideTimer()
                                    true
                                }
                                Key.DirectionUp, Key.DirectionDown -> {
                                    showTopBar = true
                                    resetHideTimer()
                                    true
                                }
                                else -> false
                            }
                        }
                        .pointerInput(playerEngine, gestureConfig, isInPipMode, activePanel, showTopBar, topBarHeightState, bottomControlsHeightState) {
                            if (isInPipMode || activePanel != PlayerPanel.NONE) return@pointerInput
                            if (isInPipMode) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startX: Float = down.position.x
                                val startY: Float = down.position.y
                                val isInsideTopBar = showTopBar && startY < topBarHeightState
                                val isInsideBottomControls = showTopBar && startY > (size.height - bottomControlsHeightState)
                                if (isInsideTopBar || isInsideBottomControls) {
                                    return@awaitEachGesture
                                }
                                val isLeftSide = startX < size.width / 2f
                                var totalDy = 0f
                                var isDragging = false
                                 var dragVolume = gestureController.currentVolumeNormalized()
                                 var dragBrightness = gestureController.currentBrightnessNormalized(context)
                                var isPointerDown = true
                                var holdJob: kotlinx.coroutines.Job? = null

                                // Long press detection (hold-to-speedup T2.7)
                                holdJob = scope.launch {
                                    kotlinx.coroutines.delay(600)
                                    if (isPointerDown && !isDragging) { gestureController.startHoldSpeed() }
                                }

                                var upOrCancel: PointerInputChange? = null
                                var lastChange: PointerInputChange? = null
                                try {
                                    loop@ while (true) {
                                        val event = awaitPointerEvent()
                                        val change: androidx.compose.ui.input.pointer.PointerInputChange = event.changes.firstOrNull() ?: break@loop
                                        lastChange = change

                                        val dy: Float = change.position.y - startY
                                        val dx: Float = change.position.x - startX
                                        totalDy = dy

                                        val dxAbs: Float = if (dx < 0f) 0f - dx else dx
                                        val dyAbs: Float = if (dy < 0f) 0f - dy else dy

                                        if (!isDragging && (dyAbs > 12f || dxAbs > 12f)) {
                                            isPointerDown = false
                                            holdJob?.cancel() // Not a long press if user moved
                                            isDragging = true
                                        }

                                        if (isDragging) {
                                            resetHideTimer()
                                            if (dyAbs > dxAbs * 1.5f) {
                                                // Vertical swipe — volume or brightness
                                                val deltaNorm: Float = (change.position.y - change.previousPosition.y) * -1.5f / size.height
                                                if (isLeftSide) {
                                                    dragBrightness = (dragBrightness + deltaNorm).coerceIn(0.01f, 1f)
                                                    gestureController.setBrightnessAbsolute(context, dragBrightness)
                                                } else {
                                                    dragVolume = (dragVolume + deltaNorm).coerceIn(0f, 1f)
                                                    gestureController.setVolumeAbsolute(dragVolume)
                                                }
                                                change.consume()
                                            } else if (dxAbs > dyAbs * 1.5f) {
                                                // Horizontal swipe — seek
                                                val deltaX: Float = change.position.x - change.previousPosition.x
                                                val percentage: Float = (deltaX / size.width) * gestureConfig.scrollSensitivity
                                                val seekDeltaMs: Long = (percentage * duration * 0.15f).toLong()
                                                if (seekDeltaMs != 0L) {
                                                    val newPos = (playerEngine.currentPosition + seekDeltaMs).coerceIn(0L, duration)
                                                    playerEngine.seekTo(newPos)
                                                    
                                                    val seekSec = (seekDeltaMs / 1000).toInt()
                                                    if (seekSec != 0) {
                                                        seekFeedback = if (seekSec > 0) "+${seekSec}s" else "${seekSec}s"
                                                        // Swipe yönüne göre gösterge konumu: sağa kaydırma (ileri) → solda, sola kaydırma (geri) → sağda
                                                        seekFeedbackOnRightSide = deltaX > 0
                                                    }
                                                }
                                                change.consume()
                                            }
                                        }

                                        if (change.changedToUp() || !change.pressed || event.changes.none { it.pressed }) {
                                            upOrCancel = change
                                            break@loop
                                        }
                                    }
                                } finally {
                                    isPointerDown = false
                                    holdJob?.cancel()
                                    gestureController.stopHoldSpeed()
                                }
                            }
                        }
                        // Tap and double-tap gestures
                        .pointerInput(playerEngine, gestureConfig, isInPipMode, activePanel, showTopBar, topBarHeightState, bottomControlsHeightState) {
                            if (isInPipMode || activePanel != PlayerPanel.NONE) return@pointerInput
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    val startY = offset.y
                                    val isInsideTopBar = showTopBar && startY < topBarHeightState
                                    val isInsideBottomControls = showTopBar && startY > (size.height - bottomControlsHeightState)
                                    if (isInsideTopBar || isInsideBottomControls) return@detectTapGestures

                                    val isRight = offset.x > size.width / 2f
                                    val pos = playerEngine.currentPosition
                                    val dur = playerEngine.duration
                                    val seekMs = (safeSettings.doubleTapSeekSeconds * 1000).toLong()
                                    if (isRight) {
                                        playerEngine.seekTo((pos + seekMs).coerceAtMost(dur))
                                        seekFeedback = "+${safeSettings.doubleTapSeekSeconds}s"
                                        seekFeedbackOnRightSide = true // sağa tıklandı → gösterge solda (TopStart)
                                    } else {
                                        playerEngine.seekTo((pos - seekMs).coerceAtLeast(0))
                                        seekFeedback = "-${safeSettings.doubleTapSeekSeconds}s"
                                        seekFeedbackOnRightSide = false // sola tıklandı → gösterge sağda (TopEnd)
                                    }
                                    resetHideTimer()
                                },
                                onTap = { offset ->
                                    val startY = offset.y
                                    val isInsideTopBar = showTopBar && startY < topBarHeightState
                                    val isInsideBottomControls = showTopBar && startY > (size.height - bottomControlsHeightState)
                                    if (!isInsideTopBar && !isInsideBottomControls) {
                                        showTopBar = !showTopBar
                                        if (showTopBar) {
                                            resetHideTimer()
                                        }
                                    }
                                }
                            )
                        }
                        .pointerInput(playerEngine, gestureConfig, isInPipMode) {
                            if (isInPipMode || !gestureConfig.zoomGestureEnabled) return@pointerInput
                            detectTransformGestures { _, _, zoom, _ ->
                                if (zoom > 1.10f && currentAspectMode != com.kitsugi.animelist.core.player.PlayerAspectMode.ZOOM) {
                                    currentAspectMode = com.kitsugi.animelist.core.player.PlayerAspectMode.ZOOM
                                    aspectFeedback = "Yakınlaştır (Kırp)"
                                    resetHideTimer()
                                } else if (zoom < 0.90f && currentAspectMode != com.kitsugi.animelist.core.player.PlayerAspectMode.FIT) {
                                    currentAspectMode = com.kitsugi.animelist.core.player.PlayerAspectMode.FIT
                                    aspectFeedback = "Sığdır"
                                    resetHideTimer()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Video View
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            playerEngine.createVideoView(ctx)
                        },
                        update = { view ->
                            playerEngine.setAspectMode(currentAspectMode)
                            playerEngine.setSubtitleStyle(subtitleStyle)
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // PauseOverlay
                    PauseOverlay(
                        visible = !isPlayingState && activePanel == PlayerPanel.NONE && !isInPipMode,
                        onClose = { playerEngine.play() },
                        title = animeTitle.ifBlank { currentTitle },
                        posterUrl = posterUrl,
                        episodeTitle = currentTitle,
                        season = season,
                        episode = currentEpisode,
                        year = startYear?.toString(),
                        description = description,
                        cast = castList
                    )

                    // ─── Gesture overlays (T2.1) ────────────────────────────────────────
                    if (!gestureState.isHoldSpeeding && !isInPipMode) {
                        // Volume bar (left side - reversed UX)
                        if (gestureState.gestureOverlayIcon?.name?.contains("Volume") == true) {
                            VolumeProgressBar(
                                volume   = gestureState.volumeLevel,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 24.dp)
                            )
                        }
                        // Brightness bar (right side - reversed UX)
                        if (gestureState.gestureOverlayIcon?.name?.contains("Brightness") == true) {
                            BrightnessProgressBar(
                                brightness = gestureState.brightnessLevel.coerceAtLeast(0f),
                                modifier   = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 24.dp)
                            )
                        }
                    } else if (gestureState.isHoldSpeeding && !isInPipMode) {
                        // Hold-to-speedup overlay — merkez üst (play butonunun üstünde)
                        PlayerGestureOverlay(
                            text = gestureState.gestureOverlayText,
                            icon = gestureState.gestureOverlayIcon,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 80.dp)
                        )
                    }

                    // Controls overlay (Top and Bottom bars)
                    AnimatedVisibility(
                        visible = showTopBar && !isInPipMode,
                        enter = fadeIn(tween(250)),
                        exit  = fadeOut(tween(250)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f))) {
                            // Top Bar
                            PlayerTopBar(
                                title = formattedTitle,
                                onBack = onBack,
                                onLaunchExternal = if (!currentVideoUrl.isNullOrBlank()) {
                                    {
                                        PlayerLogger.logExternalPlayerLaunch(
                                            context   = context,
                                            url       = currentVideoUrl,
                                            addonName = currentAddonName,
                                            title     = currentTitle,
                                            manual    = true
                                        )
                                        KitsugiFullscreenPlayerActivity.launchExternalPlayer(
                                            context = context,
                                            videoUrl = currentVideoUrl,
                                            title = currentTitle,
                                            positionMs = playerEngine.currentPosition,
                                            headers = currentHeaders,
                                            subtitles = currentSubtitles
                                        )
                                        onBack()
                                    }
                                } else null,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .onGloballyPositioned { coords ->
                                        topBarHeightState = coords.size.height.toFloat()
                                    }
                            )

                            // Center Play/Pause button
                            PlayerCenterControl(
                                isPlaying = isPlayingState,
                                onClick = {
                                    if (playerEngine.isPlaying) {
                                        playerEngine.pause()
                                        playPauseIcon = Icons.Rounded.Pause
                                    } else {
                                        playerEngine.play()
                                        playPauseIcon = Icons.Rounded.PlayArrow
                                    }
                                    resetHideTimer()
                                },
                                modifier = Modifier.align(Alignment.Center)
                            )

                            // Bottom Seek Bar Controls
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                                        )
                                    )
                                    .onGloballyPositioned { coords ->
                                        bottomControlsHeightState = coords.size.height.toFloat()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 20.dp)
                            ) {
                                // ─── T2.2 – Preview Seekbar Generator Lifecycle ────────────────────────
                val previewBitmap = remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                val previewGenerator = remember(currentVideoUrl) {
                    if (!currentVideoUrl.isNullOrBlank() && safeSettings.previewSeekbarEnabled) {
                        PreviewGenerator(context, currentVideoUrl, currentHeaders)
                    } else null
                }

                LaunchedEffect(previewGenerator, duration) {
                    if (previewGenerator != null && duration > 0 && safeSettings.previewSeekbarEnabled) {
                        previewGenerator.start(duration)
                    }
                }

                DisposableEffect(previewGenerator) {
                    onDispose {
                        previewGenerator?.release()
                        previewBitmap.value = null
                    }
                }

                PlayerSeekBar(
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeekTo = {
                        playerEngine.seekTo(it)
                        resetHideTimer()
                    },
                    previewBitmap = if (safeSettings.previewSeekbarEnabled) previewBitmap.value else null,
                    onScrubPosition = if (safeSettings.previewSeekbarEnabled) { fraction ->
                        resetHideTimer()
                        scope.launch {
                            val bmp = previewGenerator?.getPreviewImage(fraction)
                            previewBitmap.value = bmp
                        }
                    } else null
                )

                                Spacer(modifier = Modifier.height(10.dp))

                                val hasQualityOptions = remember(currentStreamSources) {
                                    currentStreamSources
                                        .map { it.qualityValue ?: 0 }
                                        .filter { it > 0 }
                                        .distinct()
                                        .size > 1
                                }

                                PlayerBottomActions(
                                    hasTextTracks = textTrackOptions.isNotEmpty(),
                                    hasMultiAudio = audioTrackOptions.size > 1,
                                    hasSources = streamSources.isNotEmpty(),
                                    hasEpisodes = episodesList.isNotEmpty(),
                                    hasQualityOptions = hasQualityOptions,
                                    currentResizeModeLabel = when (currentAspectMode) {
                                        com.kitsugi.animelist.core.player.PlayerAspectMode.ORIGINAL -> "Orijinal"
                                        com.kitsugi.animelist.core.player.PlayerAspectMode.FIT -> "Sığdır"
                                        com.kitsugi.animelist.core.player.PlayerAspectMode.FILL -> "Uzat"
                                        com.kitsugi.animelist.core.player.PlayerAspectMode.ZOOM -> "Yakınlaştır"
                                        com.kitsugi.animelist.core.player.PlayerAspectMode.CROP_16_9 -> "Kırp 16:9"
                                        com.kitsugi.animelist.core.player.PlayerAspectMode.CROP_4_3 -> "Kırp 4:3"
                                    },
                                    currentSpeedLabel = if (currentSpeed == 1.0f) "Normal" else "${currentSpeed}x",
                                    onSubtitleClick = { activePanel = PlayerPanel.SUBTITLES },
                                    onAudioClick = { activePanel = PlayerPanel.AUDIO },
                                    onSourcesClick = { activePanel = PlayerPanel.SOURCES },
                                    onEpisodesClick = { activePanel = PlayerPanel.EPISODES },
                                    onAspectClick = {
                                        currentAspectMode = when (currentAspectMode) {
                                            com.kitsugi.animelist.core.player.PlayerAspectMode.ORIGINAL -> com.kitsugi.animelist.core.player.PlayerAspectMode.FIT
                                            com.kitsugi.animelist.core.player.PlayerAspectMode.FIT -> com.kitsugi.animelist.core.player.PlayerAspectMode.FILL
                                            com.kitsugi.animelist.core.player.PlayerAspectMode.FILL -> com.kitsugi.animelist.core.player.PlayerAspectMode.ZOOM
                                            com.kitsugi.animelist.core.player.PlayerAspectMode.ZOOM -> com.kitsugi.animelist.core.player.PlayerAspectMode.CROP_16_9
                                            com.kitsugi.animelist.core.player.PlayerAspectMode.CROP_16_9 -> com.kitsugi.animelist.core.player.PlayerAspectMode.CROP_4_3
                                            com.kitsugi.animelist.core.player.PlayerAspectMode.CROP_4_3 -> com.kitsugi.animelist.core.player.PlayerAspectMode.ORIGINAL
                                        }
                                        val newModeText = when (currentAspectMode) {
                                            com.kitsugi.animelist.core.player.PlayerAspectMode.ORIGINAL -> "Orijinal"
                                            com.kitsugi.animelist.core.player.PlayerAspectMode.FIT -> "Sığdır"
                                            com.kitsugi.animelist.core.player.PlayerAspectMode.FILL -> "Uzat"
                                            com.kitsugi.animelist.core.player.PlayerAspectMode.ZOOM -> "Yakınlaştır"
                                            com.kitsugi.animelist.core.player.PlayerAspectMode.CROP_16_9 -> "Kırp 16:9"
                                            com.kitsugi.animelist.core.player.PlayerAspectMode.CROP_4_3 -> "Kırp 4:3"
                                        }
                                        aspectFeedback = "Ekran Modu: $newModeText"
                                        resetHideTimer()
                                    },
                                    onRotateClick = {
                                        val nextOrientation = when (screenOrientationState) {
                                            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                            else -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                                        }
                                        screenOrientationState = nextOrientation
                                        activity?.requestedOrientation = nextOrientation
                                        val text = when (nextOrientation) {
                                            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR -> "Yönlendirme: Otomatik"
                                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE -> "Yönlendirme: Yatay (Kilitli)"
                                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT -> "Yönlendirme: Dikey (Kilitli)"
                                            else -> ""
                                        }
                                        aspectFeedback = text
                                        resetHideTimer()
                                    },
                                    onSpeedClick = { activePanel = PlayerPanel.SPEED },
                                    onStreamInfoClick = { activePanel = PlayerPanel.STREAM_INFO },
                                    onSkipSettingsClick = { activePanel = PlayerPanel.SKIP_SETTINGS },
                                    onQualityClick = { activePanel = PlayerPanel.QUALITY },
                                    showMediaInfo = safeSettings.showMediaInfo
                                )
                            }
                        }
                    }

                    // AniSkip Overlay
                    run {
                        val positionSec = currentPosition / 1000L
                        val activeSkip = skipIntervals.firstOrNull { interval ->
                            positionSec >= interval.startTime.toLong() &&
                            positionSec < interval.endTime.toLong()
                        }
                        if (activeSkip != null) {
                            val skipLabel = when (activeSkip.type) {
                                "op"       -> "⏭️ Intro Atla"
                                "ed"       -> "⏭️ Outro Atla"
                                "recap"    -> "⏭️ Özet Atla"
                                "mixed-op" -> "⏭️ Intro Atla"
                                "mixed-ed" -> "⏭️ Outro Atla"
                                else       -> "⏭️ Atla"
                            }
                            LaunchedEffect(activeSkip) {
                                if (aniSkipAutoSkip) {
                                    playerEngine.seekTo((activeSkip.endTime * 1000).toLong())
                                }
                            }
                            if (!aniSkipAutoSkip && !isInPipMode) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(bottom = 130.dp, end = 24.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            playerEngine.seekTo((activeSkip.endTime * 1000).toLong())
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Black.copy(alpha = 0.75f),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            skipLabel,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Double Tap Seek feedback
                    // Tıklanan tarafın KARŞISINDAki köşede göster, play butonunun üstünde
                    FeedbackBubble(
                        text = seekFeedback,
                        icon = if (seekFeedback?.startsWith("+") == true) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                        modifier = Modifier
                            .align(
                                if (seekFeedbackOnRightSide) Alignment.TopStart   // sağa tıklandı → gösterge sol üstte
                                else                         Alignment.TopEnd     // sola tıklandı → gösterge sağ üstte
                            )
                            .padding(
                                top   = 80.dp,
                                start = if (seekFeedbackOnRightSide) 32.dp else 0.dp,
                                end   = if (seekFeedbackOnRightSide) 0.dp  else 32.dp
                            )
                    )

                    // Aspect ratio feedback overlay — merkez üst (play butonunun üstünde)
                    FeedbackBubble(
                        text = aspectFeedback,
                        icon = Icons.Rounded.AspectRatio,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 80.dp)
                    )

                    // Playback speed selection overlay (Side Panel)
                    SpeedSelectionOverlay(
                        visible = activePanel == PlayerPanel.SPEED,
                        onClose = { activePanel = PlayerPanel.NONE },
                        currentSpeed = currentSpeed,
                        onSpeedSelected = { speed ->
                            currentSpeed = speed
                            playerEngine.setPlaybackSpeed(speed)
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                    // PostPlayBingeCard
                    PostPlayBingeCard(
                        visible = shouldShowBingeCard && !isInPipMode,
                        nextEpisodeTitle = nextEpTitle,
                        nextEpisodeNumber = nextEpNum,
                        thumbnailUrl = nextEpThumbnail,
                        onPlayNext = { playNextEpisode() },
                        onCancel = { viewModel.userCancelledBinge = true },
                        countdownSeconds = bingeCountdownSec,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 90.dp, end = 24.dp)
                    )

                    val hasNextEpisode = episodesList.any { it.episodeNumber == nextEpNum } || currentEpisode < (episodesList.lastOrNull()?.episodeNumber ?: Int.MAX_VALUE)
                    PlaybackEndedOverlay(
                        visible = isPlaybackEnded && !isAutoplayEnabled && !isInPipMode,
                        hasNextEpisode = hasNextEpisode,
                        onReplay = {
                            isPlaybackEnded = false
                            playerEngine.seekTo(0L)
                            playerEngine.play()
                        },
                        onPlayNext = {
                            isPlaybackEnded = false
                            playNextEpisode()
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // TorrentOverlay
                    TorrentOverlay(
                        visible = isTorrentStream && !isInPipMode,
                        downloadSpeedBytes = 2_450_000L,
                        uploadSpeedBytes = 120_000L,
                        seeders = 42,
                        peers = 128,
                        bufferPercent = 100,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 70.dp, start = 24.dp)
                    )

                    // StreamInfoOverlay (Side Panel)
                    StreamInfoOverlay(
                        visible = activePanel == PlayerPanel.STREAM_INFO,
                        onClose = { activePanel = PlayerPanel.NONE },
                        info = streamInfoData,
                        showPlayerResolution = safeSettings.showPlayerResolution,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )

                    // Search/Fetch loading overlay for next episode
                    if (nextEpisodeLoading && !isInPipMode) {
                        PlayerInlineLoadingOverlay(message = "Sonraki Bölüm Kaynakları Aranıyor...")
                    }

                    // Stream URL resolution loading overlay
                    if (isResolvingStream && !isInPipMode) {
                        PlayerInlineLoadingOverlay(message = "Kaynak Bağlantısı Çözümleniyor...")
                    }

                    // Buffering overlay
                    if (isBufferingState && !isInPipMode) {
                        PlayerBufferingView()
                    }

                    // Slide-out Settings Panels
                    // Episodes Side Panel
                    EpisodesSidePanel(
                        visible = activePanel == PlayerPanel.EPISODES,
                        onClose = { activePanel = PlayerPanel.NONE },
                        episodes = episodesList,
                        currentEpisode = currentEpisode,
                        onEpisodeClick = { ep ->
                            activePanel = PlayerPanel.NONE
                            val targetEp = ep.episodeNumber ?: 1
                            playEpisode(targetEp)
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )

                    // Subtitle Selection Overlay
                    SubtitleSelectionOverlay(
                        visible = activePanel == PlayerPanel.SUBTITLES,
                        onClose = { activePanel = PlayerPanel.NONE },
                        trackOptions = textTrackOptions,
                        isSubtitleDisabled = isSubtitleDisabled,
                        onDisableSubtitles = {
                            playerEngine.disableSubtitles()
                            isSubtitleDisabled = true
                        },
                        onSelectTrack = { opt ->
                            playerEngine.selectTrack(opt)
                            isSubtitleDisabled = false
                        },
                        styleSettings = subtitleStyle,
                        onStyleChange = { newStyle ->
                            subtitleStyle = newStyle
                            playerEngine.setSubtitleStyle(newStyle)
                            scope.launch {
                                dataStore.setDefaultSubtitleSize(newStyle.size)
                                dataStore.setDefaultSubtitleColor(newStyle.textColor)
                                dataStore.setSubtitleBold(newStyle.bold)
                                dataStore.setSubtitleOutlineEnabled(newStyle.outlineEnabled)
                            }
                        },
                        subtitleDelayMs = subtitleDelayMs,
                        onSubtitleDelayChange = { 
                            subtitleDelayMs = it
                            playerEngine.setSubtitleDelay(it)
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )

                    // Audio Selection Overlay
                    AudioSelectionOverlay(
                        visible = activePanel == PlayerPanel.AUDIO,
                        onClose = { activePanel = PlayerPanel.NONE },
                        trackOptions = audioTrackOptions,
                        onSelectTrack = { opt ->
                            playerEngine.selectTrack(opt)
                        },
                        audioDelayMs = audioDelayMs,
                        onAudioDelayChange = { delay ->
                            audioDelayMs = delay
                            playerEngine.setAudioDelay(delay)
                            scope.launch {
                                dataStore.setDefaultAudioDelayMs(delay)
                            }
                        },
                        audioBoostLevel = audioBoostLevel,
                        onAudioBoostChange = { boost ->
                            audioBoostLevel = boost
                            playerEngine.setVolume(1.0f + boost)
                            scope.launch {
                                dataStore.setDefaultAudioBoost(boost)
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )

                    // Sources Selection Panel
                    SourcesSelectionOverlay(
                        visible = activePanel == PlayerPanel.SOURCES,
                        onClose = { activePanel = PlayerPanel.NONE },
                        sources = currentStreamSources,
                        currentIndex = currentSourceIndex,
                        onSelectSource = { i, stream ->
                            activePanel = PlayerPanel.NONE
                            viewModel.isResolvingStream = true
                            scope.launch {
                                val repo = AddonStreamRepository(context)
                                val resolvedUrl = repo.resolveStreamUrl(stream)
                                viewModel.isResolvingStream = false
                                if (resolvedUrl != null) {
                                    val currentPos = playerEngine.currentPosition
                                    viewModel.changeStreamSource(i, stream, resolvedUrl)
                                    
                                    val nextResumeKey = "play_pos_" + resolvedUrl.hashCode()
                                    sharedPrefs.edit().putLong(nextResumeKey, currentPos).apply()
                                } else {
                                    Toast.makeText(context, "Kaynak çözümlenemedi.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )

                    // Quality Selection Overlay (Side Panel)
                    QualitySelectionOverlay(
                        visible = activePanel == PlayerPanel.QUALITY,
                        onClose = { activePanel = PlayerPanel.NONE },
                        sources = currentStreamSources,
                        currentSourceIndex = currentSourceIndex,
                        onQualitySelected = { source, index ->
                            activePanel = PlayerPanel.NONE
                            viewModel.isResolvingStream = true
                            scope.launch {
                                val repo = AddonStreamRepository(context)
                                val resolvedUrl = repo.resolveStreamUrl(source)
                                viewModel.isResolvingStream = false
                                if (resolvedUrl != null) {
                                    val currentPos = playerEngine.currentPosition
                                    viewModel.changeStreamSource(index, source, resolvedUrl)
                                    val nextResumeKey = "play_pos_" + resolvedUrl.hashCode()
                                    sharedPrefs.edit().putLong(nextResumeKey, currentPos).apply()
                                } else {
                                    android.widget.Toast.makeText(context, "Kaynak çözümlenemedi.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )

                    // PlayerSkipSettingsOverlay (Side Panel)
                    PlayerSkipSettingsOverlay(
                        visible = activePanel == PlayerPanel.SKIP_SETTINGS,
                        onClose = { activePanel = PlayerPanel.NONE },
                        enabled = aniSkipEnabled,
                        onEnabledChange = { enabled ->
                            viewModel.updateSkipSettings(enabled, aniSkipAutoSkip, animeSkipClientId)
                        },
                        autoSkip = aniSkipAutoSkip,
                        onAutoSkipChange = { autoSkip ->
                            viewModel.updateSkipSettings(aniSkipEnabled, autoSkip, animeSkipClientId)
                        },
                        clientId = animeSkipClientId,
                        onClientIdChange = { clientId ->
                            viewModel.updateSkipSettings(aniSkipEnabled, aniSkipAutoSkip, clientId)
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )

                    if (showResumeDialog) {
                    AlertDialog(
                    onDismissRequest = {
                    showResumeDialog = false
                    playerEngine.play()
                    },
                    containerColor = KitsugiColors.Surface,
                    titleContentColor = KitsugiColors.TextPrimary,
                    textContentColor = KitsugiColors.TextSecondary,
                    shape = RoundedCornerShape(26.dp),
                    title = {
                    Text(
                    text = "Kaldığınız Yerden Devam Edilsin mi?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                    )
                    },
                    text = {
                    Text(
                    text = "Video en son ${formatMs(pendingResumePos)} konumunda kalmış. Kaldığınız yerden devam etmek ister misiniz?",
                    style = MaterialTheme.typography.bodyMedium
                    )
                    },
                    confirmButton = {
                    TextButton(
                    onClick = {
                    showResumeDialog = false
                    playerEngine.seekTo(pendingResumePos)
                    playerEngine.play()
                    }
                    ) {
                    Text("Evet", color = LocalKitsugiAccent.current, fontWeight = FontWeight.Bold)
                    }
                    },
                    dismissButton = {
                    TextButton(
                    onClick = {
                    showResumeDialog = false
                    playerEngine.seekTo(0L)
                    playerEngine.play()
                    }
                    ) {
                    Text("Hayır", color = KitsugiColors.TextSecondary)
                    }
                    }
                    )
                    
                    }

                }
            }
        }
    }

}

@Composable
fun PlaybackEndedOverlay(
    visible: Boolean,
    hasNextEpisode: Boolean,
    onReplay: () -> Unit,
    onPlayNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bölüm Bitti",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Replay button
                Button(
                    onClick = onReplay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay,
                        contentDescription = "Yeniden Oynat",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Yeniden Oynat", fontWeight = FontWeight.SemiBold)
                }

                // Next Episode button
                if (hasNextEpisode) {
                    Button(
                        onClick = onPlayNext,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KitsugiColors.Accent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Sonraki Bölüm",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Sonraki Bölüm", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
