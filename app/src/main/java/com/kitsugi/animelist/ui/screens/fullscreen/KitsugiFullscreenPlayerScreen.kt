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
import com.kitsugi.animelist.ui.screens.fullscreen.components.PauseOverlay
import com.kitsugi.animelist.ui.screens.fullscreen.components.FeedbackBubble
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerInlineLoadingOverlay
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerLoadingView
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerErrorView
import com.kitsugi.animelist.ui.screens.fullscreen.components.PlayerBufferingView
import com.kitsugi.animelist.ui.screens.fullscreen.components.SubtitleStyleSettings
import com.kitsugi.animelist.ui.screens.fullscreen.components.StreamInfoData
import com.kitsugi.animelist.ui.screens.fullscreen.components.MetaCastMember
import com.kitsugi.animelist.ui.screens.fullscreen.components.TorrentOverlay
import com.kitsugi.animelist.ui.screens.fullscreen.components.TrackOption
import com.kitsugi.animelist.ui.screens.fullscreen.controls.PlayerControls
import com.kitsugi.animelist.ui.screens.fullscreen.controls.PlayerSheetsHost
import com.kitsugi.animelist.ui.screens.fullscreen.controls.PlayerPanelsHost
import com.kitsugi.animelist.ui.screens.fullscreen.controls.PlayerDialogsHost
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.BrightnessOverlay
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
// Legacy gesture imports removed
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import com.kitsugi.animelist.ui.screens.fullscreen.components.PreviewGenerator
import com.kitsugi.animelist.ui.screens.fullscreen.components.formatMs
import com.kitsugi.animelist.ui.screens.stream.KitsugiStreamActivity

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
    isMovie: Boolean = false,
    cs3Url: String? = null,
    cs3ApiName: String? = null,
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

    val subtitlesPicker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.addSubtitle(uri)
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.addAudio(uri)
        }
    }
    
    // ── İzleme geçmişi için animeId türet ────────────────────────────────────
    val watchHistoryAnimeId = remember(aniListId, malId, cs3Url, animeTitle) {
        when {
            aniListId != null -> aniListId.toString()
            malId != null     -> malId.toString()
            !cs3Url.isNullOrBlank() -> cs3Url.hashCode().toString()
            else              -> animeTitle.hashCode().toString()
        }
    }

    // ── Periyodik izleme konumu kaydı (her 10 sn) ───────────────────────────
    // Not: currentEpisode aşağıda (satır ~263) collectAsState() ile tanımlanıyor,
    // burada duplicate oluşmasın diye viewModel StateFlow'larına doğrudan erişiyoruz.
    LaunchedEffect(watchHistoryAnimeId) {
        while (true) {
            kotlinx.coroutines.delay(10_000L)
            val currentPos = viewModel.pos.value
            val currentDur = viewModel.duration.value
            val ep = viewModel.currentEpisode.value
            if (currentPos > 0L && currentDur > 0L && watchHistoryAnimeId.isNotBlank()) {
                com.kitsugi.animelist.data.local.WatchHistoryManager.updateProgress(
                    animeId    = watchHistoryAnimeId,
                    episode    = ep,
                    positionMs = currentPos,
                    durationMs = currentDur
                )
            }
        }
    }

    LaunchedEffect(videoId, videoUrl, audioUrl, title, initialIndex, episode, tmdbId, isMovie) {
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
            isMovie = isMovie,
            activity = activity,
            cs3Url = cs3Url,
            cs3ApiName = cs3ApiName
        )
    }

    val currentVideoUrl by viewModel.currentVideoUrl.collectAsState()
    val currentAudioUrl by viewModel.currentAudioUrl.collectAsState()
    val currentHeaders by viewModel.currentHeaders.collectAsState()
    val currentSubtitles by viewModel.currentSubtitles.collectAsState()
    val currentTitle by viewModel.currentTitle.collectAsState()
    val currentSourceIndex by viewModel.currentSourceIndex.collectAsState()
    val currentStreamSources by viewModel.currentStreamSources.collectAsState()
    val currentAddonName by viewModel.currentAddonName.collectAsState()
    val currentEpisode by viewModel.currentEpisode.collectAsState()
    val episodesList by viewModel.episodesList.collectAsState()
    val userCancelledBinge by viewModel.userCancelledBingeFlow.collectAsState()
    val isResolvingStream by viewModel.isResolvingStreamFlow.collectAsState()
    val nextEpisodeLoading by viewModel.nextEpisodeLoading.collectAsState()
    val playbackSource by viewModel.playbackSource.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasError by viewModel.hasError.collectAsState()
    val errorDetails by viewModel.errorDetails.collectAsState()
    val isAutoSwitching by viewModel.isAutoSwitching.collectAsState()

    // AniSkip state
    val skipIntervals by viewModel.skipIntervals.collectAsState()
    val aniSkipAutoSkip by viewModel.aniSkipAutoSkip.collectAsState()
    val aniSkipEnabled by viewModel.aniSkipEnabled.collectAsState()
    val animeSkipClientId by viewModel.animeSkipClientId.collectAsState()


    val sleepTimerSecondsLeft by viewModel.sleepTimerSecondsLeft.collectAsState()
    val currentBrightness by viewModel.currentBrightness.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()

    // ── New Aniyomi-style reactive UI state ──────────────────────────────────
    val sheetShown by viewModel.sheetShown.collectAsState()
    val dismissSheet by viewModel.dismissSheet.collectAsState()
    val panelShown by viewModel.panelShown.collectAsState()
    val dialogShown by viewModel.dialogShown.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val areControlsLocked by viewModel.areControlsLocked.collectAsState()

    LaunchedEffect(panelShown, sheetShown, isFocusTargetAttached) {
        if (panelShown == KitsugiPanels.None && sheetShown == KitsugiSheets.None && isFocusTargetAttached) {
            try {
                mainFocusRequester.requestFocus()
            } catch (e: Exception) {
                Log.e("KitsugiPlayer", "Failed to restore focus after panel/sheet close", e)
            }
        }
    }
    
    // Autoplay / binge card (replaces full-screen countdown)
    val isAutoplayEnabled = appSettings?.isAutoplayEnabled ?: true
    val showBingeCard by viewModel.showBingeCardState.collectAsState()
    val customButtons by viewModel.customButtons.collectAsState()
    val statisticsPage by viewModel.statisticsPage.collectAsState()
    val audioChannels by viewModel.audioChannels.collectAsState()

    val bingeCountdownSec by viewModel.bingeCountdownSec.collectAsState()

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

    LaunchedEffect(currentAspectMode) {
        if (isSettingsLoaded) {
            viewModel.setAspectMode(currentAspectMode)
        }
    }

    // Stream diagnostics
    var streamInfoData by remember { mutableStateOf(StreamInfoData(playerEngine = "ExoPlayer")) }

    val mediaIdForHistory = remember(malId, aniListId, cs3Url, animeTitle) {
        malId ?: aniListId
            ?: if (!cs3Url.isNullOrBlank()) cs3Url.hashCode()
               else animeTitle.hashCode()
    }
    var savedPos by remember(mediaIdForHistory, currentEpisode, currentAddonName) { mutableStateOf<Long?>(null) }
    var showResumeDialog by remember(mediaIdForHistory, currentEpisode, currentAddonName) { mutableStateOf(false) }
    var pendingResumePos by remember { mutableStateOf(0L) }
    var hasCheckedResume by remember(mediaIdForHistory, currentEpisode, currentAddonName) { mutableStateOf(false) }

    LaunchedEffect(mediaIdForHistory, currentEpisode, currentAddonName) {
        savedPos = viewModel.getSavedPosition(mediaIdForHistory, currentEpisode, currentAddonName)
    }

    fun playEpisode(targetEp: Int) {
        viewModel.resetAutoSwitch()
        viewModel.playEpisode(
            targetEp = targetEp,
            activity = activity,
            onAlternativeRequired = {
                viewModel.showSheet(KitsugiSheets.QualityTracks)
                Toast.makeText(context, "Aynı kaynak bulunamadı. Diğer kaynaklar listeleniyor.", Toast.LENGTH_LONG).show()
            },
            onResolutionFailed = {
                viewModel.showSheet(KitsugiSheets.QualityTracks)
                Toast.makeText(context, "Seçilen kaynak çözümlenemedi. Lütfen listeden başka bir kaynak seçin.", Toast.LENGTH_LONG).show()
            }
        )
    }

    fun playNextEpisode() {
        viewModel.playNextEpisode(
            activity = activity,
            onAlternativeRequired = {
                viewModel.showSheet(KitsugiSheets.QualityTracks)
                Toast.makeText(context, "Aynı kaynak bulunamadı. Diğer kaynaklar listeleniyor.", Toast.LENGTH_LONG).show()
            },
            onResolutionFailed = {
                viewModel.showSheet(KitsugiSheets.QualityTracks)
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
                        val urlVal = currentVideoUrl
                        if (!urlVal.isNullOrBlank()) {
                            PlayerLogger.logExternalPlayerLaunch(
                                context   = context,
                                url       = urlVal,
                                addonName = currentAddonName,
                                title     = currentTitle,
                                manual    = true
                            )
                            KitsugiFullscreenPlayerActivity.launchExternalPlayer(
                                context = context,
                                videoUrl = urlVal,
                                title = currentTitle,
                                positionMs = savedPos ?: 0L,
                                headers = currentHeaders,
                                subtitles = currentSubtitles
                            )
                            onBack()
                        }
                    },
                    onRetry = { playEpisode(currentEpisode) },
                    onSwitchSource = {
                        if (currentStreamSources.isNotEmpty()) {
                            viewModel.showSheet(KitsugiSheets.QualityTracks)
                        } else {
                            KitsugiStreamActivity.start(
                                context = context,
                                malId = malId,
                                aniListId = aniListId,
                                tmdbId = tmdbId,
                                episode = currentEpisode,
                                season = season,
                                isMovie = isMovie,
                                title = animeTitle.ifBlank { title },
                                posterUrl = posterUrl,
                                titleEnglish = titleEnglish,
                                titleRomaji = titleRomaji,
                                titleNative = titleNative,
                                startYear = startYear,
                                description = description,
                                cast = castList,
                                isAutoplay = true
                            )
                            activity?.finish()
                        }
                    },
                    switchSourceText = if (currentStreamSources.isNotEmpty()) null else "Kaynakları Yeniden Ara"
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
                // Collect active engine type from ViewModel (set by settings + updated on fallback).
                // This drives which engine is instantiated below without local ad-hoc state.
                val activeEngineType by viewModel.activeEngineType.collectAsState()

                LaunchedEffect(activeEngineType, currentVideoUrl) {
                    if (activeEngineType == PlayerEngineType.EXTERNAL) {
                        val urlVal = currentVideoUrl
                        if (!urlVal.isNullOrBlank()) {
                            PlayerLogger.logExternalPlayerLaunch(
                                context   = context,
                                url       = urlVal,
                                addonName = currentAddonName,
                                title     = currentTitle,
                                manual    = false
                            )
                            KitsugiFullscreenPlayerActivity.launchExternalPlayer(
                                context = context,
                                videoUrl = urlVal,
                                title = currentTitle,
                                positionMs = savedPos ?: 0L,
                                headers = currentHeaders,
                                subtitles = currentSubtitles
                            )
                            onBack()
                        }
                    }
                }

                val playerEngine = remember(context, activeEngineType) {
                    when (activeEngineType) {
                        PlayerEngineType.MPV -> MpvPlayerEngine(context, safeSettings)
                        else -> Media3PlayerEngine(context, safeSettings)
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
                
                var aspectFeedback by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(playerEngine, currentAspectMode) {
                    playerEngine.setAspectMode(currentAspectMode)
                }

                LaunchedEffect(playerEngine, playbackSpeed) {
                    playerEngine.setPlaybackSpeed(playbackSpeed)
                }

                // ─── Gesture Controller (T2.1 + T2.7) ─────────────────────────────────
                // Legacy rememberPlayerGestureController removed (Centralized GestureHandler used in PlayerControls)

                // ─── PiP Panel and Controls Reset (T2.3) ──────────────────────────────
                LaunchedEffect(isInPipMode) {
                    if (isInPipMode) {
                        viewModel.showPanel(KitsugiPanels.None)
                        viewModel.showSheet(KitsugiSheets.None)
                    }
                }



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
                                com.kitsugi.animelist.data.local.WatchHistoryManager.updateProgress(
                                    animeId = mediaIdForHistory.toString(),
                                    episode = currentEpisode,
                                    positionMs = currentPosition,
                                    durationMs = duration
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

                val shouldShowBingeCard = showBingeCard && !isInPipMode

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
                                Log.w("KitsugiPlayerDebug", "Buffering watchdog: Stream stuck buffering for 12s. Delegating to errorRecovery.")
                                viewModel.orchestrator.errorRecovery.onPlaybackError(5004, "Arabelleğe alma zaman aşımına uğradı (12sn)")
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
                                playerEngine.setPlaybackSpeed(playbackSpeed)
                                val mpvEngine = playerEngine as? com.kitsugi.animelist.core.player.engine.MpvPlayerEngine
                                mpvEngine?.mpvView?.mpv?.setPropertyInt("user-data/current-anime/intro-length", viewModel.getAnimeSkipIntroLength())
                                viewModel.orchestrator.errorRecovery.onPlaybackReady()
                            } else if (state == PlayerEngine.State.ENDED) {
                                Log.d("KitsugiPlayerDebug", "Player ENDED: calling onEpisodeEnded")
                                isPlaybackEnded = true
                                viewModel.onEpisodeEnded(duration, currentPosition)
                            }
                        }

                        override fun onPlaybackError(errorCode: Int, errorMsg: String, cause: Throwable?) {
                            Log.e("KitsugiPlayerDebug", "onPlaybackError: code=$errorCode, message=$errorMsg", cause)
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
                            // Delegate entirely to the orchestrator's unified error recovery pipeline
                            viewModel.orchestrator.errorRecovery.onPlaybackError(errorCode, errorMsg)
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

                        override fun onEngineEvent(property: String, value: String) {
                            if (property == "user-data/current-anime/intro-length") {
                                viewModel.setAnimeSkipIntroLength(value.toIntOrNull() ?: 85)
                            } else {
                                viewModel.handleLuaInvocation(property, value)
                            }
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
                            com.kitsugi.animelist.data.local.WatchHistoryManager.updateProgress(
                                animeId = mediaIdForHistory.toString(),
                                episode = currentEpisode,
                                positionMs = finalPos,
                                durationMs = duration
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
                    
                    playerEngine.setPlaybackSpeed(playbackSpeed)

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

                val controlsShown by viewModel.controlsShown.collectAsState()

                LaunchedEffect(controlsShown) {
                    if (!controlsShown && isFocusTargetAttached) {
                        try {
                            mainFocusRequester.requestFocus()
                        } catch (e: Exception) {
                            Log.e("KitsugiPlayer", "Failed to request focus on controls hide", e)
                        }
                    }
                }

                LaunchedEffect(isPlayingState) {
                    if (!isPlayingState) {
                        viewModel.showControls()
                    } else {
                        viewModel.showControls()
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
                                    panelShown != KitsugiPanels.None || sheetShown != KitsugiSheets.None || dialogShown != KitsugiDialogs.None -> {
                                        viewModel.showPanel(KitsugiPanels.None)
                                        viewModel.showSheet(KitsugiSheets.None)
                                        viewModel.showDialog(KitsugiDialogs.None)
                                        mainFocusRequester.requestFocus()
                                        true
                                    }
                                    controlsShown -> {
                                        viewModel.hideControls()
                                        mainFocusRequester.requestFocus()
                                        true
                                    }
                                    else -> {
                                        onBack()
                                        true
                                    }
                                }
                            }
                            
                            if (panelShown != KitsugiPanels.None || sheetShown != KitsugiSheets.None || dialogShown != KitsugiDialogs.None) return@onPreviewKeyEvent false
                            
                            if (controlsShown) {
                                viewModel.showControls() // Reset timer
                                return@onPreviewKeyEvent false
                            }
                            
                            when (event.key) {
                                Key.DirectionCenter, Key.Enter -> {
                                    viewModel.togglePlay()
                                    viewModel.showControls()
                                    true
                                }
                                Key.DirectionLeft -> {
                                    val pos = playerEngine.currentPosition
                                    playerEngine.seekTo((pos - 10000).coerceAtLeast(0))
                                    seekFeedback = "-10s"
                                    seekFeedbackOnRightSide = false
                                    viewModel.showControls()
                                    true
                                }
                                Key.DirectionRight -> {
                                    val pos = playerEngine.currentPosition
                                    val dur = playerEngine.duration
                                    playerEngine.seekTo((pos + 10000).coerceAtMost(dur))
                                    seekFeedback = "+10s"
                                    seekFeedbackOnRightSide = true
                                    viewModel.showControls()
                                    true
                                }
                                Key.DirectionUp, Key.DirectionDown -> {
                                    viewModel.showControls()
                                    true
                                }
                                else -> false
                            }
                        }
                        .pointerInput(playerEngine, safeSettings, isInPipMode) {
                            if (isInPipMode || !safeSettings.gestureZoomEnabled) return@pointerInput
                            detectTransformGestures { _, _, zoom, _ ->
                                if (zoom > 1.10f && currentAspectMode != com.kitsugi.animelist.core.player.PlayerAspectMode.ZOOM) {
                                    currentAspectMode = com.kitsugi.animelist.core.player.PlayerAspectMode.ZOOM
                                    aspectFeedback = "Yakınlaştır (Kırp)"
                                    viewModel.showControls()
                                } else if (zoom < 0.90f && currentAspectMode != com.kitsugi.animelist.core.player.PlayerAspectMode.FIT) {
                                    currentAspectMode = com.kitsugi.animelist.core.player.PlayerAspectMode.FIT
                                    aspectFeedback = "Sığdır"
                                    viewModel.showControls()
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

                    BrightnessOverlay(
                        brightness = currentBrightness,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Legacy gesture progress bars and overlays removed (GestureHandler & PlayerControls manage overlays)

                    // Centralized OSD controls (Aniyomi style)
                    if (!isInPipMode) {
                        PlayerControls(
                            viewModel = viewModel,
                            onBackClick = onBack,
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
                            },
                            onAspectClick = {
                                val modes = com.kitsugi.animelist.core.player.PlayerAspectMode.values()
                                val nextMode = modes[(currentAspectMode.ordinal + 1) % modes.size]
                                currentAspectMode = nextMode
                                val text = when (nextMode) {
                                    com.kitsugi.animelist.core.player.PlayerAspectMode.ORIGINAL -> "Orijinal"
                                    com.kitsugi.animelist.core.player.PlayerAspectMode.FIT -> "Sığdır"
                                    com.kitsugi.animelist.core.player.PlayerAspectMode.FILL -> "Doldur"
                                    com.kitsugi.animelist.core.player.PlayerAspectMode.CROP_16_9 -> "16:9"
                                    com.kitsugi.animelist.core.player.PlayerAspectMode.CROP_4_3 -> "4:3"
                                    com.kitsugi.animelist.core.player.PlayerAspectMode.ZOOM -> "Yakınlaştır (Kırp)"
                                }
                                aspectFeedback = text
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // AniSkip Auto-Skip logic
                    run {
                        val positionSec = currentPosition / 1000L
                        val activeSkip = skipIntervals.firstOrNull { interval ->
                            positionSec >= interval.startTime.toLong() &&
                            positionSec < interval.endTime.toLong()
                        }
                        if (activeSkip != null) {
                            LaunchedEffect(activeSkip) {
                                if (aniSkipAutoSkip) {
                                    playerEngine.seekTo((activeSkip.endTime * 1000).toLong())
                                }
                            }
                        }
                    }

                    // Double Tap Seek feedback
                    FeedbackBubble(
                        text = seekFeedback,
                        icon = if (seekFeedback?.startsWith("+") == true) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                        modifier = Modifier
                            .align(
                                if (seekFeedbackOnRightSide) Alignment.TopStart
                                else                         Alignment.TopEnd
                            )
                            .padding(
                                top   = 80.dp,
                                start = if (seekFeedbackOnRightSide) 32.dp else 0.dp,
                                end   = if (seekFeedbackOnRightSide) 0.dp  else 32.dp
                            )
                    )

                    // Aspect ratio feedback overlay
                    FeedbackBubble(
                        text = aspectFeedback,
                        icon = Icons.Rounded.AspectRatio,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 80.dp)
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

                    // Search/Fetch loading overlay for next episode
                    if (nextEpisodeLoading && !isInPipMode) {
                        PlayerInlineLoadingOverlay(message = "Sonraki Bölüm Kaynakları Aranıyor...")
                    }

                    // Stream URL resolution loading overlay
                    if (isResolvingStream && !isInPipMode) {
                        PlayerInlineLoadingOverlay(message = "Kaynak Bağlantısı Çözümleniyor...")
                    }

                    // Buffering overlay
                    if (isBufferingState && isPlayingState && !isInPipMode) {
                        PlayerBufferingView(
                            isPlaying = isPlayingState,
                            onPlayPauseClick = {
                                if (playerEngine.isPlaying) {
                                    playerEngine.pause()
                                } else {
                                    playerEngine.play()
                                }
                            }
                        )
                    }

                    // ── New Reactive Sheet Host (Aniyomi-style) ───────────────────────
                    PlayerSheetsHost(
                        sheetShown = sheetShown,
                        dismissSheet = dismissSheet,
                        onDismissRequest = {
                            viewModel.showSheet(KitsugiSheets.None)
                            viewModel.resetDismissSheet()
                        },
                        customButtons = customButtons,
                        onClickCustomButton = viewModel::executeCustomButton,
                        onLongClickCustomButton = viewModel::executeCustomButtonLongPress,
                        onOpenPanel = { panel -> viewModel.showPanel(panel) },
                        currentSpeed = playbackSpeed,
                        onSpeedChange = { speed ->
                            viewModel.setPlaybackSpeed(speed)
                        },
                        onSetSpeedAsDefault = { speed ->
                            viewModel.setPlaybackSpeed(speed)
                        },
                        currentChapter = currentChapter,
                        chapters = chapters,
                        onSeekToChapter = { seg ->
                            playerEngine.seekTo((seg.start * 1000f).toLong())
                        },
                        subtitleTracks = textTrackOptions.mapIndexed { index, option ->
                            com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets.SubtitleTrackInfo(
                                id = index,
                                label = option.label,
                                language = null
                            )
                        },
                        selectedSubtitleIndex = if (isSubtitleDisabled) -1 else textTrackOptions.indexOfFirst { it.isSelected },
                        onSelectSubtitle = { idx ->
                            if (idx == -99) {
                                playerEngine.disableSubtitles()
                            } else {
                                textTrackOptions.getOrNull(idx)?.let { playerEngine.selectTrack(it) }
                            }
                        },
                        onAddSubtitleFile = { subtitlesPicker.launch(arrayOf("*/*")) },
                        audioTrackLabels = audioTrackOptions.map { it.label },
                        selectedAudioIndex = audioTrackOptions.indexOfFirst { it.isSelected },
                        onSelectAudio = { idx -> audioTrackOptions.getOrNull(idx)?.let { playerEngine.selectTrack(it) } },
                        onAddAudioFile = { audioPicker.launch(arrayOf("*/*")) },
                        streamSources = currentStreamSources,
                        selectedSourceIndex = currentSourceIndex,
                        onSelectSource = { idx ->
                            val src = currentStreamSources.getOrNull(idx) ?: return@PlayerSheetsHost
                            viewModel.showSheet(KitsugiSheets.None)
                            viewModel.isResolvingStream = true
                            scope.launch {
                                val repo = AddonStreamRepository(context)
                                val resolvedUrl = kotlinx.coroutines.withTimeoutOrNull(30_000L) {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        repo.resolveStreamUrl(src)
                                    }
                                }
                                viewModel.isResolvingStream = false
                                if (resolvedUrl != null) {
                                    viewModel.changeStreamSource(idx, src, resolvedUrl)
                                } else {
                                    android.widget.Toast.makeText(context, "Kaynak çözümlenemedi.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        sleepTimerSecondsLeft = sleepTimerSecondsLeft,
                        onStartSleepTimer = { secs -> viewModel.startTimer(secs) },
                        selectedDecoder = viewModel.currentDecoder.collectAsState().value,
                        onSelectDecoder = { dec -> viewModel.updateDecoder(dec) },
                        statisticsPage = statisticsPage,
                        onSelectStatisticsPage = { page -> viewModel.updateStatisticsPage(page) },
                        audioChannels = audioChannels,
                        onSelectAudioChannels = { ch -> viewModel.updateAudioChannels(ch) },
                        isLocalSource = viewModel.isLocalSource,
                        hasSubTracks = viewModel.hasSubTracks.collectAsState().value,
                        showSubtitles = viewModel.screenshotShowSubtitles.collectAsState().value,
                        onToggleShowSubtitles = { viewModel.toggleScreenshotShowSubtitles(it) },
                        cachePath = context.cacheDir.absolutePath,
                        onSetAsArt = { artType, stream -> viewModel.setAsArt(artType, stream) },
                        onSaveScreenshot = { stream -> viewModel.saveImage(stream, playerEngine.currentPosition.toInt()) },
                        onShareScreenshot = { stream -> viewModel.shareImage(stream, playerEngine.currentPosition.toInt()) },
                        takeScreenshot = { path, showSubs -> viewModel.takeScreenshot(path, showSubs) },
                        modifier = Modifier.fillMaxSize()
                    )

                    // ── New Reactive Panel Host (Aniyomi-style) ──────────────────────
                    PlayerPanelsHost(
                        panelShown = panelShown,
                        onDismissRequest = { viewModel.showPanel(KitsugiPanels.None) },
                        currentAudioDelayMs = audioDelayMs.toInt(),
                        onAudioDelayChanged = { delay ->
                            audioDelayMs = delay
                            playerEngine.setAudioDelay(delay)
                            scope.launch { dataStore.setDefaultAudioDelayMs(delay) }
                        },
                        currentSubDelayMs = subtitleDelayMs.toInt(),
                        onSubDelayChanged = { delay ->
                            subtitleDelayMs = delay.toLong()
                            playerEngine.setSubtitleDelay(delay.toLong())
                        },
                        subtitleStyle = subtitleStyle,
                        onSubtitleStyleChange = { style ->
                            subtitleStyle = style
                            playerEngine.setSubtitleStyle(style)
                            scope.launch {
                                dataStore.setDefaultSubtitleSize(style.size)
                                dataStore.setDefaultSubtitleColor(style.textColor)
                                dataStore.setSubtitleBold(style.bold)
                                dataStore.setSubtitleOutlineEnabled(style.outlineEnabled)
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )

                    // ── New Reactive Dialog Host (Aniyomi-style) ─────────────────────
                    PlayerDialogsHost(
                        dialogShown = dialogShown,
                        episodes = episodesList,
                        currentEpisode = currentEpisode,
                        onPlayEpisode = { ep ->
                            viewModel.showDialog(KitsugiDialogs.None)
                            playEpisode(ep)
                        },
                        onDismissRequest = { viewModel.showDialog(KitsugiDialogs.None) },
                        modifier = Modifier.align(Alignment.Center)
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
