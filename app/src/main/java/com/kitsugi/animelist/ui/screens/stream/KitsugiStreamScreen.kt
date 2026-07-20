package com.kitsugi.animelist.ui.screens.stream

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitsugi.animelist.core.player.ExternalPlayerInput
import com.kitsugi.animelist.core.player.ExternalPlayerLauncher
import com.kitsugi.animelist.data.repository.AddonStreamRepository
import com.kitsugi.animelist.data.repository.StreamSorter
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiFullscreenPlayerActivity
import com.kitsugi.animelist.ui.screens.fullscreen.components.MetaCastMember
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import android.webkit.CookieManager
import com.kitsugi.animelist.ui.components.KitsugiWebViewDialog
import kotlinx.coroutines.launch
import com.kitsugi.animelist.ui.app.AddonViewModel
import com.kitsugi.animelist.ui.components.KitsugiAddonsSettingsDialog
import com.kitsugi.animelist.data.remote.DebridResolver
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import android.util.Log
import com.kitsugi.animelist.core.player.SubtitleInput


import com.kitsugi.animelist.ui.theme.LocalIsTvDevice
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.onFocusChanged

import com.kitsugi.animelist.core.player.QualityProfile
import com.kitsugi.animelist.core.player.QualityDataHelper

/**
 * Stream picker screen.
 *
 * All fetch work is delegated to [StreamViewModel], which survives:
 *  - backgrounding the app
 *  - screen rotation
 *  - navigating away and back within the same Activity
 *
 * The Composable only reads [StateFlow]s from the ViewModel, making it
 * purely presentational with no lifecycle-coupled data loading.
 */
@Composable
fun KitsugiStreamScreen(
    malId: Int?,
    aniListId: Int?,
    tmdbId: Int? = null,
    episode: Int,
    season: Int,
    isMovie: Boolean = false,
    title: String,
    posterUrl: String?,
    titleEnglish: String? = null,
    titleRomaji: String? = null,
    titleNative: String? = null,
    startYear: Int? = null,
    description: String? = null,
    castList: List<MetaCastMember> = emptyList(),
    isAutoplay: Boolean = false,
    onBack: () -> Unit,
    onLaunchExternalPlayer: ((input: ExternalPlayerInput, streamKey: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val scope = rememberCoroutineScope()

    // ── ViewModel (survives backgrounding & rotation) ──────────────────────────
    val viewModel: StreamViewModel = viewModel()
    val addonViewModel: AddonViewModel = viewModel()

    // ── Collect ViewModel state ───────────────────────────────────────────────
    val addonStates    by viewModel.addonStates.collectAsState()
    val isResolvingId  by viewModel.isResolvingId.collectAsState()
    val idResolveFailed by viewModel.idResolveFailed.collectAsState()
    val imdbId         by viewModel.imdbId.collectAsState()
    val webViewState   by viewModel.webViewDialogState.collectAsState()

    val addonsList by addonViewModel.addonsList.collectAsState(initial = emptyList())
    val reposList by addonViewModel.reposList.collectAsState(initial = emptyList())
    val csPluginsList by addonViewModel.csPluginsList.collectAsState(initial = emptyList())

    // ── Local UI-only state (ephemeral, fine to live in Composable) ───────────
    val repository     = remember { AddonStreamRepository(context) }
    val streamPrefs    = remember { context.getSharedPreferences(KitsugiStreamActivity.PREFS_NAME, Context.MODE_PRIVATE) }
    val dataStore      = remember { SettingsDataStore(context) }
    val playerPrefs    = remember { context.getSharedPreferences("MyWebViewPrefs", Context.MODE_PRIVATE) }
    val appSettings    by dataStore.settingsFlow.collectAsState(initial = AppSettings())

    var selectedAddonFilter by remember { mutableStateOf<String?>(null) }
    var resolvingSource     by remember { mutableStateOf<StreamSource?>(null) }
    var resolvingError      by remember { mutableStateOf<String?>(null) }
    var pendingPlayAction   by remember { mutableStateOf<PendingPlayAction?>(null) }
    var showSettingsDialog  by remember { mutableStateOf(false) }

    val allStreams = remember(addonStates, appSettings.qualityProfileJson) {
        val rawList = addonStates.flatMap { it.streams }
        val profile = QualityProfile.deserialize(appSettings.qualityProfileJson)
        val filtered = QualityDataHelper.filterByBitrate(rawList, profile.maxBitrateKbps)
        val sorted = StreamSorter.sort(filtered)
        QualityDataHelper.sortByProfile(sorted, profile)
    }
    val isAnyLoading = addonStates.any { it.isLoading }

    var isAutoplayAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(reposList) {
        addonViewModel.syncRepos(reposList)
    }

    LaunchedEffect(addonViewModel) {
        addonViewModel.onShowMessage = { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // ── Build alternative titles list once ────────────────────────────────────
    val alternativeTitles = remember(titleEnglish, titleRomaji, titleNative, title) {
        buildList {
            titleEnglish?.takeIf { it.isNotBlank() && it != title }?.let { add(it) }
            titleRomaji?.takeIf { it.isNotBlank() && it != title }?.let { add(it) }
            titleNative?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }

    // ── Trigger fetch — ViewModel guards against duplicate/redundant calls ────
    LaunchedEffect(malId, aniListId, tmdbId, episode, season) {
        viewModel.startFetch(
            malId             = malId,
            aniListId         = aniListId,
            tmdbId            = tmdbId,
            episode           = episode,
            season            = season,
            title             = title,
            alternativeTitles = alternativeTitles,
            startYear         = startYear
        )
    }

    // ── Player launcher ───────────────────────────────────────────────────────
    val launchPlayer = remember(context, allStreams, streamPrefs, onLaunchExternalPlayer, description, castList) {
        { source: StreamSource, resolvedUrl: String, engine: String ->
            val streamKey = (source.infoHash ?: resolvedUrl).hashCode().toString()
            val resumePositionMs = streamPrefs.getLong(KitsugiStreamActivity.KEY_POS_PFX + streamKey, 0L)
            if (engine == "exoplayer") {
                KitsugiFullscreenPlayerActivity.startWithStreamUrls(
                    context = context, videoUrl = resolvedUrl,
                    title = "$title - Bölüm $episode", headers = source.requestHeaders,
                    subtitles = source.subtitles, allSources = allStreams,
                    currentSourceIndex = allStreams.indexOf(source),
                    malId = malId, aniListId = aniListId, tmdbId = tmdbId, season = season, episode = episode,
                    animeTitle = title, posterUrl = posterUrl,
                    titleEnglish = titleEnglish, titleRomaji = titleRomaji, titleNative = titleNative,
                    startYear = startYear, description = description, cast = castList
                )
            } else {
                if (onLaunchExternalPlayer != null) {
                    val input = ExternalPlayerLauncher.createInput(
                        url = resolvedUrl, title = "$title - Bölüm $episode",
                        headers = source.requestHeaders, resumePositionMs = resumePositionMs,
                        subtitles = source.subtitles
                    )
                    onLaunchExternalPlayer(input, streamKey)
                } else {
                    KitsugiFullscreenPlayerActivity.startWithStreamUrls(
                        context = context, videoUrl = resolvedUrl,
                        title = "$title - Bölüm $episode", headers = source.requestHeaders,
                        subtitles = source.subtitles, allSources = allStreams,
                        currentSourceIndex = allStreams.indexOf(source),
                        malId = malId, aniListId = aniListId, tmdbId = tmdbId, season = season, episode = episode,
                        animeTitle = title, posterUrl = posterUrl,
                        titleEnglish = titleEnglish, titleRomaji = titleRomaji, titleNative = titleNative,
                        startYear = startYear, description = description, cast = castList
                    )
                }
            }
        }
    }

    val handlePlayStream = remember(appSettings, launchPlayer) {
        { source: StreamSource, resolvedUrl: String ->
            val engine = when (appSettings.playerPreference) {
                "EXTERNAL" -> "mpv"
                "ASK"      -> "ask"
                else       -> "exoplayer"
            }
            val streamKey = (source.infoHash ?: resolvedUrl).hashCode().toString()
            if (engine == "ask") {
                pendingPlayAction = PendingPlayAction(source, resolvedUrl, streamKey)
            } else {
                launchPlayer(source, resolvedUrl, engine)
            }
            Unit
        }
    }

    // T1.8 – Autoplay Selection Logic
    LaunchedEffect(allStreams, isAnyLoading) {
        if (isAutoplay && !isAnyLoading && allStreams.isNotEmpty() && !isAutoplayAttempted) {
            isAutoplayAttempted = true
            val lastAddonName = streamPrefs.getString("last_addon_name", null)
            val bestSource = com.kitsugi.animelist.core.player.StreamAutoPlaySelector.selectBestStream(
                currentAddonName = lastAddonName,
                currentStreamSource = null,
                nextEpisodeStreams = allStreams
            )
            if (bestSource != null) {
                resolvingSource = bestSource
                val resolvedUrl = repository.resolveStreamUrl(bestSource)
                resolvingSource = null
                if (resolvedUrl != null) {
                    handlePlayStream(bestSource, resolvedUrl)
                }
            }
        }
    }

    BackHandler { onBack() }

    // ── Main content ──────────────────────────────────────────────────────────
    StreamScreenContent(
        title = title, posterUrl = posterUrl, episode = episode, season = season, isMovie = isMovie,
        imdbId = imdbId, accentColor = accentColor,
        addonStates = addonStates, allStreams = allStreams,
        isResolvingId = isResolvingId, idResolveFailed = idResolveFailed,
        isAnyLoading = isAnyLoading, selectedAddonFilter = selectedAddonFilter,
        onAddonFilterChange = { selectedAddonFilter = it },
        onStreamSelected = { source ->
            streamPrefs.edit().putString("last_addon_name", source.addonName).apply()
            val isTorrent = !source.infoHash.isNullOrBlank() || source.url?.startsWith("magnet:") == true
            if (isTorrent && DebridResolver(context).getApiKey().isNullOrBlank()) {
                resolvingError = "Debrid API anahtarı gerekli."
                return@StreamScreenContent
            }
            resolvingSource = source; resolvingError = null
            scope.launch {
                val resolvedUrlDeferred = async(Dispatchers.IO) {
                    repository.resolveStreamUrl(source)
                }

                val subtitlesDeferred = async(Dispatchers.IO) {
                    try {
                        val currentMalId = malId
                        val currentAniList = aniListId
                        val currentEp = episode
                        val currentS = season
                        val isMovieType = isMovie || (currentS == 0 && currentEp <= 1)

                        val resolvedIds = com.kitsugi.animelist.data.remote.KitsugiIdResolver.resolveIds(currentMalId, currentAniList, tmdbId)
                        val imdbId = resolvedIds.imdbId
                        val kitsuId = resolvedIds.kitsuId

                        val type = if (isMovieType) "movie" else "series"
                        val queryId = when {
                            !imdbId.isNullOrBlank() -> if (isMovieType) imdbId else "$imdbId:$currentS:$currentEp"
                            kitsuId != null -> if (isMovieType) "kitsu:$kitsuId" else "kitsu:$kitsuId:$currentEp"
                            else -> null
                        }

                        if (queryId == null) {
                            Log.w("KitsugiStreamScreen", "Altyazı atlandı: ID çözümlenemedi")
                            return@async emptyList<SubtitleInput>()
                        }

                        val resolvedUrl = resolvedUrlDeferred.await() ?: return@async emptyList<SubtitleInput>()

                        val guessedFilename = source.title?.takeIf { it.isNotBlank() }
                            ?: resolvedUrl.let { url ->
                                try {
                                    android.net.Uri.parse(url).lastPathSegment?.takeIf { it.contains(".") }
                                } catch (_: Exception) { null }
                            }
                        val cleanedFilename = guessedFilename?.substringBefore("\n")?.substringBefore("\r")?.trim()

                        val subRepo = com.kitsugi.animelist.data.repository.SubtitleRepositoryImpl(context)
                        val remoteSubs = subRepo.getSubtitles(
                            type = type,
                            id = queryId,
                            videoUrl = resolvedUrl,
                            videoHeaders = source.requestHeaders,
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

                        coroutineScope {
                            filteredSubs.map { sub ->
                                async(Dispatchers.IO) {
                                    val localFile = com.kitsugi.animelist.core.player.SubtitleFileCache.cacheSubtitle(context, sub.url)
                                    if (localFile != null) {
                                        val friendlyLangName = com.kitsugi.animelist.core.player.PlayerSubtitleUtils.getFriendlyLanguageName(sub.lang)
                                        SubtitleInput(
                                            url = localFile.absolutePath,
                                            name = "$friendlyLangName (${sub.addonName})",
                                            lang = sub.lang
                                        )
                                    } else {
                                        null
                                    }
                                }
                            }.awaitAll().filterNotNull()
                        }
                    } catch (e: Exception) {
                        Log.e("KitsugiStreamScreen", "Altyazı pre-fetch hatası", e)
                        emptyList<SubtitleInput>()
                    }
                }

                val resolvedUrl = resolvedUrlDeferred.await()
                val fetchedSubtitles = subtitlesDeferred.await()

                resolvingSource = null
                if (resolvedUrl == null) {
                    resolvingError = "Akış linki çözümlenemedi."
                    return@launch
                }

                val updatedSource = source.copy(
                    subtitles = (source.subtitles ?: emptyList()) + fetchedSubtitles
                )

                handlePlayStream(updatedSource, resolvedUrl)
            }
        },
        onBack = onBack,
        resolvingSource = resolvingSource, resolvingError = resolvingError,
        onResolvingErrorDismiss = { resolvingError = null },
        pendingPlayAction = pendingPlayAction,
        onPendingDismiss = { pendingPlayAction = null },
        playerPrefs = playerPrefs, launchPlayer = launchPlayer,
        onPendingDone = { pendingPlayAction = null },
        onRememberChoice = { engine ->
            scope.launch {
                val pref = if (engine == "mpv") "EXTERNAL" else "INTERNAL"
                dataStore.setPlayerPreference(pref)
            }
            playerPrefs.edit().putString("default_player_engine", engine).apply()
        },
        onVerifyPlugin = { addonDisplayName ->
            viewModel.onVerifyPlugin(addonDisplayName)
        },
        onOpenSettings = { showSettingsDialog = true }
    )

    if (showSettingsDialog) {
        val isTv = LocalIsTvDevice.current
        if (isTv) {
            TvStreamSettingsDialog(
                addonViewModel = addonViewModel,
                onDismiss = { showSettingsDialog = false }
            )
        } else {
            KitsugiAddonsSettingsDialog(
                addons = addonsList,
                initialDebridToken = addonViewModel.debridToken,
                repos = reposList,
                repoPlugins = addonViewModel.repoPluginsState,
                repoLoadingState = addonViewModel.repoLoadingState,
                csPlugins = csPluginsList,
                initialTab = 1, // Focuses directly on Video Sağlayıcıları
                onAddAddon = { addonViewModel.addAddon(it) },
                onToggleAddon = { addon, enabled -> addonViewModel.toggleAddon(addon, enabled) },
                onDeleteAddon = { addonViewModel.deleteAddon(it) },
                onSaveDebridToken = { addonViewModel.saveDebridToken(it) },
                onAddRepo = { addonViewModel.addRepo(it) },
                onDeleteRepo = { addonViewModel.deleteRepo(it) },
                onFetchRepoPlugins = { addonViewModel.fetchRepoPlugins(it) },
                onInstallPlugin = { plugin, onResult -> addonViewModel.installPlugin(plugin, onResult) },
                onInstallAllPlugins = { repoUrl, repoName, plugins ->
                    addonViewModel.installAllPlugins(repoUrl, repoName, plugins, addonsList, csPluginsList)
                },
                onUpdateAllPlugins = { repoUrl, repoName, plugins ->
                    addonViewModel.updateAllPlugins(repoUrl, repoName, plugins, csPluginsList)
                },
                bulkInstallRepoUrl = addonViewModel.bulkInstallRepoUrl,
                bulkInstallRepoName = addonViewModel.bulkInstallRepoName,
                bulkInstallDone = addonViewModel.bulkInstallDone,
                bulkInstallTotal = addonViewModel.bulkInstallTotal,
                bulkInstallCurrentName = addonViewModel.bulkInstallCurrentName,
                bulkInstallResultMessage = addonViewModel.bulkInstallResultMessage,
                onClearBulkInstallResult = { addonViewModel.clearBulkInstallResult() },
                onToggleCsPlugin = { plugin, enabled -> addonViewModel.toggleCsPlugin(plugin, enabled) },
                onUninstallCsPlugin = { addonViewModel.uninstallCsPlugin(it) },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }

    // ── WebView captcha dialog — driven by ViewModel state ────────────────────
    val state = webViewState
    if (state != null) {
        KitsugiWebViewDialog(
            title = state.displayName,
            url   = state.url,
            onDismiss = {
                viewModel.onWebViewDismissed(
                    dismissedPluginId   = state.pluginId,
                    dismissedDisplayName = state.displayName,
                    title               = title,
                    alternativeTitles   = alternativeTitles,
                    startYear           = startYear,
                    season              = season,
                    episode             = episode,
                    malId               = malId,
                    aniListId           = aniListId,
                    tmdbId              = tmdbId
                )
            }
        )
    }
}

@OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TvStreamSettingsDialog(
    addonViewModel: AddonViewModel,
    onDismiss: () -> Unit
) {
    val addons by addonViewModel.addonsList.collectAsState(initial = emptyList())
    val repos by addonViewModel.reposList.collectAsState(initial = emptyList())
    val csPlugins by addonViewModel.csPluginsList.collectAsState(initial = emptyList())

    var showDebridDialog by remember { mutableStateOf(false) }
    var showAddRepoDialog by remember { mutableStateOf(false) }

    // Fetch repo plugins in background when repos load
    LaunchedEffect(repos) {
        repos.forEach { repo ->
            addonViewModel.fetchRepoPlugins(repo.repoUrl)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
                .heightIn(max = screenHeight * 0.9f)
                .clip(KitsugiTvTokens.Shapes.dialog as RoundedCornerShape)
                .background(KitsugiColors.Surface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.tv.material3.Text(
                    text = "Akış & Eklenti Ayarları",
                    style = androidx.tv.material3.MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Kapat",
                        tint = KitsugiColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
            val listState = rememberLazyListState()

            CompositionLocalProvider(LocalBringIntoViewSpec provides tvSpec) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .dpadVerticalFastScroll(scrollableState = listState),
                    verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap)
                ) {
                    // Debrid Token
                    item {
                        val isConnected = addonViewModel.debridToken.isNotBlank()
                        TvSettingsActionRow(
                            title = "RealDebrid / Alldebrid Token",
                            description = if (isConnected) "Token tanımlı: ${addonViewModel.debridToken.take(8)}..." else "Hesap bağlı değil. Debrid tokeninizi girmek için tıklayın.",
                            actionText = if (isConnected) "Güncelle" else "Bağla",
                            onClick = { showDebridDialog = true }
                        )
                    }

                    // Repository Headers
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.tv.material3.Text(
                            text = "Eklenti Havuzları (Repos)",
                            style = androidx.tv.material3.MaterialTheme.typography.titleMedium,
                            color = androidx.tv.material3.MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Repos List
                    itemsIndexed(repos) { _, repo ->
                        val plugins = addonViewModel.repoPluginsState[repo.repoUrl]
                        val isCurrentlyInstalling = addonViewModel.bulkInstallRepoUrl == repo.repoUrl
                        val repoDesc = when {
                            isCurrentlyInstalling -> "Eklentiler kuruluyor: ${addonViewModel.bulkInstallDone}/${addonViewModel.bulkInstallTotal}"
                            plugins != null -> "${plugins.size} eklenti mevcut. Kurmak veya güncellemek için dokunun."
                            else -> "Eklentiler yükleniyor..."
                        }

                        TvSettingsActionRow(
                            title = repo.name,
                            description = repoDesc,
                            actionText = if (plugins != null && !isCurrentlyInstalling) "Hepsini Kur" else "Bekleyin",
                            onClick = {
                                if (plugins != null && !isCurrentlyInstalling) {
                                    addonViewModel.installAllPlugins(repo.repoUrl, repo.name, plugins, addons, csPlugins)
                                }
                            }
                        )
                    }

                    item {
                        TvSettingsActionRow(
                            title = "+ Yeni Repo Ekle",
                            description = "Yeni bir eklenti havuzu URL'si ekleyin.",
                            actionText = "Ekle",
                            onClick = { showAddRepoDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Dialogs for TV Inputs
    if (showDebridDialog) {
        var tokenInput by remember { mutableStateOf(addonViewModel.debridToken) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDebridDialog = false },
            title = { androidx.compose.material3.Text("Debrid Tokeni Gir", color = Color.White) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { androidx.compose.material3.Text("Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = androidx.tv.material3.MaterialTheme.colorScheme.primary,
                        focusedBorderColor = androidx.tv.material3.MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        addonViewModel.saveDebridToken(tokenInput)
                        showDebridDialog = false
                    }
                ) {
                    androidx.compose.material3.Text("Kaydet", color = androidx.tv.material3.MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDebridDialog = false }) {
                    androidx.compose.material3.Text("Vazgeç", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = KitsugiColors.BackgroundElevated,
            textContentColor = Color.White
        )
    }

    if (showAddRepoDialog) {
        var repoUrlInput by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddRepoDialog = false },
            title = { androidx.compose.material3.Text("Yeni Repo Ekle", color = Color.White) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = repoUrlInput,
                    onValueChange = { repoUrlInput = it },
                    label = { androidx.compose.material3.Text("Repo URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = androidx.tv.material3.MaterialTheme.colorScheme.primary,
                        focusedBorderColor = androidx.tv.material3.MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (repoUrlInput.isNotBlank()) {
                            addonViewModel.addRepo(repoUrlInput)
                        }
                        showAddRepoDialog = false
                    }
                ) {
                    androidx.compose.material3.Text("Ekle", color = androidx.tv.material3.MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showAddRepoDialog = false }) {
                    androidx.compose.material3.Text("Vazgeç", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = KitsugiColors.BackgroundElevated,
            textContentColor = Color.White
        )
    }
}

@OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSettingsActionRow(
    title: String,
    description: String,
    actionText: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isFocused) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                KitsugiTvTokens.Shapes.posterCard as RoundedCornerShape
            )
            .tvClickable(shape = KitsugiTvTokens.Shapes.posterCard as RoundedCornerShape) { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .padding(KitsugiTvTokens.Spacing.contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            androidx.tv.material3.Text(
                text = title,
                style = androidx.tv.material3.MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            androidx.tv.material3.Text(
                text = description,
                style = androidx.tv.material3.MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.width(KitsugiTvTokens.Spacing.contentPadding))

        Box(
            modifier = Modifier
                .width(100.dp)
                .height(36.dp)
                .clip(KitsugiTvTokens.Shapes.chip as RoundedCornerShape)
                .background(Color.White.copy(alpha = 0.1f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.2f),
                    KitsugiTvTokens.Shapes.chip as RoundedCornerShape
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.tv.material3.Text(
                text = actionText,
                style = androidx.tv.material3.MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
