package com.kitsugi.animelist.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import com.kitsugi.animelist.ui.app.MangaViewModel
import com.kitsugi.animelist.data.manga.stableSourceKey
import com.kitsugi.animelist.ui.tv.manga.TvMangaBrowseScreen
import com.kitsugi.animelist.ui.tv.manga.TvMangaDetailScreen
import com.kitsugi.animelist.ui.tv.manga.TvMangaExtensionScreen
import com.kitsugi.animelist.ui.tv.manga.TvMangaSourceHealthScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.utils.parseToMediaType
import com.kitsugi.animelist.ui.tv.detail.TvDetailScreen
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.ui.app.AuthViewModel
import com.kitsugi.animelist.ui.screens.detail.ApiResultDetailPage
import com.kitsugi.animelist.ui.screens.search.SearchViewModel
import com.kitsugi.animelist.ui.screens.explore.ExploreViewModel
import com.kitsugi.animelist.ui.screens.explore.ExplorePlatform
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.tv.home.TvHomeScreen
import com.kitsugi.animelist.ui.tv.library.TvLibraryScreen
import com.kitsugi.animelist.ui.tv.qrlogin.TvQrLoginScreen
import com.kitsugi.animelist.ui.tv.search.TvSearchScreen
import com.kitsugi.animelist.ui.tv.stream.TvStreamScreen
import com.kitsugi.animelist.ui.tv.stream.TvStreamArgs
import com.kitsugi.animelist.ui.tv.settings.TvSettingsScreen
import com.kitsugi.animelist.ui.tv.addons.TvAddonsScreen
import com.kitsugi.animelist.ui.tv.manga.TvMangaReaderScreen
import dev.chrisbanes.haze.HazeState

import com.kitsugi.animelist.ui.tv.navigation.TvDestination
import com.kitsugi.animelist.ui.tv.navigation.TvNavItem
import com.kitsugi.animelist.ui.tv.navigation.rememberTvNavigationState
import com.kitsugi.animelist.ui.tv.navigation.TvDetailTarget
import com.kitsugi.animelist.ui.tv.navigation.TvSidebarState
import com.kitsugi.animelist.ui.tv.navigation.rememberTvSidebarState
import com.kitsugi.animelist.ui.tv.navigation.animatedWidth
import com.kitsugi.animelist.ui.tv.navigation.animatedLabelAlpha
import com.kitsugi.animelist.ui.tv.navigation.animatedExpandProgress
import com.kitsugi.animelist.ui.tv.navigation.TvBackCoordinator
import com.kitsugi.animelist.ui.tv.navigation.rememberTvBackCoordinator
import com.kitsugi.animelist.ui.tv.navigation.TvDrawerFocusRequesters
import com.kitsugi.animelist.ui.tv.navigation.rememberTvDrawerFocusRequesters
import com.kitsugi.animelist.ui.tv.navigation.drainDeepLink
import com.kitsugi.animelist.ui.screens.detail.CharacterDetailPage
import com.kitsugi.animelist.ui.screens.detail.StaffDetailPage
import com.kitsugi.animelist.ui.screens.detail.StudioDetailPage
import android.app.Activity

import com.kitsugi.animelist.core.companion.TvCompanionServer
import com.kitsugi.animelist.core.companion.TvCompanionSessionManager
import com.kitsugi.animelist.ui.tv.companion.TvCompanionApprovalDialog
import com.kitsugi.animelist.ui.tv.companion.TvCompanionQrScreen

import com.kitsugi.animelist.data.local.MediaEntryRepository
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.data.remote.matches
import com.kitsugi.animelist.ui.app.AppDialogHost
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val navItems = listOf(
    TvNavItem(TvDestination.HOME,     Icons.Filled.Home,          "Ana Sayfa"),
    TvNavItem(TvDestination.SEARCH,   Icons.Filled.Search,        "Ara"),
    TvNavItem(TvDestination.LIBRARY,  Icons.Filled.Bookmarks,     "Kütüphane"),
    TvNavItem(TvDestination.MANGA,    Icons.Filled.MenuBook,      "Manga"),
    TvNavItem(TvDestination.SETTINGS, Icons.Filled.Settings,      "Ayarlar"),
    TvNavItem(TvDestination.ACCOUNT,  Icons.Filled.AccountCircle, "Hesap / QR"),
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvRootScreen(
    tvViewModel: TvViewModel        = viewModel(),
    searchViewModel: SearchViewModel = viewModel(),
    authViewModel: AuthViewModel    = viewModel(),
    appViewModel: com.kitsugi.animelist.ui.app.AppViewModel = viewModel(),
    playerSettingsViewModel: com.kitsugi.animelist.ui.app.PlayerSettingsViewModel = viewModel(),
    addonViewModel: com.kitsugi.animelist.ui.app.AddonViewModel = viewModel(),
    profileViewModel: com.kitsugi.animelist.ui.app.ProfileViewModel = viewModel(),
    exploreViewModel: ExploreViewModel = viewModel(),
    mangaViewModel: MangaViewModel = viewModel()
) {
    val context = LocalContext.current

    // Settings ve coroutineScope (companion server'a veri sağlar — server'dan ÖNCE tanımlanmalı)
    val settingsDataStore = remember { SettingsDataStore(context.applicationContext) }
    val settingsState = settingsDataStore.settingsFlow.collectAsState(initial = AppSettings()).value
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val mediaRepository = remember {
        MediaEntryRepository(
            dao = KitsugiDatabase.getDatabase(context.applicationContext).mediaEntryDao(),
            context = context.applicationContext,
            onExternalSyncMessage = { message ->
                appViewModel.showSnackbarMessage(message)
            }
        )
    }
    val mediaEntries by mediaRepository.entriesFlow.collectAsState(initial = emptyList())

    // Yüklü Stremio eklentileri ve Cloudstream repoları (companion server'a veri sağlar)
    val installedAddons by addonViewModel.addonsList.collectAsState(initial = emptyList())
    val installedRepos  by addonViewModel.reposList.collectAsState(initial = emptyList())
    val installedCsPlugins by addonViewModel.csPluginsList.collectAsState(initial = emptyList())

    // TV Companion Server ve Session Manager (Uygulama başladığında tek instance)
    val companionSession = remember { TvCompanionSessionManager() }
    val companionServer = remember {
        TvCompanionServer(
            context = context.applicationContext,
            sessionManager = companionSession,
            addonProvider = {
                installedAddons.map { addon ->
                    mapOf(
                        "id"          to addon.manifestUrl,
                        "name"        to addon.name,
                        "manifestUrl" to addon.manifestUrl,
                        "isEnabled"   to addon.isEnabled
                    )
                }
            },
            repoProvider = {
                installedRepos.map { repo ->
                    mapOf(
                        "id"      to repo.repoUrl,
                        "name"    to repo.name,
                        "repoUrl" to repo.repoUrl
                    )
                }
            },
            csPluginsProvider = {
                installedCsPlugins.map { plugin ->
                    mapOf(
                        "id"          to plugin.id,
                        "name"        to plugin.name,
                        "downloadUrl" to plugin.downloadUrl,
                        "version"     to plugin.version,
                        "isEnabled"   to plugin.enabled
                    )
                }
            },
            mangaReposProvider = {
                mangaViewModel.mangaReposState
            },
            mangaSourcesProvider = {
                com.kitsugi.animelist.data.manga.MangaExtensionLoader.getLoadedSources().map { source ->
                    val health = mangaViewModel.getSourceHealthStatus(source)
                    mapOf(
                        "id"        to source.stableSourceKey(),
                        "name"      to source.name,
                        "pkgName"   to source.pkgName,
                        "baseUrl"   to source.baseUrl,
                        "lang"      to source.lang,
                        "isEnabled" to (health != com.kitsugi.animelist.data.manga.model.SourceHealthStatus.Disabled)
                    )
                }
            },
            onDeleteAddon  = { manifestUrl -> installedAddons.find { it.manifestUrl == manifestUrl }?.let { addonViewModel.deleteAddon(it) } },
            onToggleAddon  = { manifestUrl, enabled -> installedAddons.find { it.manifestUrl == manifestUrl }?.let { addonViewModel.toggleAddon(it, enabled) } },
            onDeleteRepo   = { repoUrl -> installedRepos.find { it.repoUrl == repoUrl }?.let { addonViewModel.deleteRepo(it) } },
            onToggleCsPlugin = { id, enabled -> installedCsPlugins.find { it.id == id }?.let { addonViewModel.toggleCsPlugin(it, enabled) } },
            onUninstallCsPlugin = { id -> installedCsPlugins.find { it.id == id }?.let { addonViewModel.uninstallCsPlugin(it) } },
            onDeleteMangaRepo = { url -> mangaViewModel.deleteMangaRepo(url) },
            onToggleMangaSource = { key, enabled ->
                com.kitsugi.animelist.data.manga.MangaExtensionLoader.getLoadedSources().find { it.stableSourceKey() == key }?.let { source ->
                    mangaViewModel.toggleMangaSource(source, enabled)
                }
            },
            onDeleteMangaSource = { key ->
                com.kitsugi.animelist.data.manga.MangaExtensionLoader.getLoadedSources().find { it.stableSourceKey() == key }?.let { source ->
                    mangaViewModel.deleteMangaExtension(source)
                }
            },
            // D4: API Keys — write-only masked state; actual values NEVER returned over the wire
            apiKeysProvider = {
                val s = settingsState
                mapOf(
                    "tmdb"    to mapOf("hasValue" to s.tmdbUserApiKey.isNotBlank()),
                    "mdblist" to mapOf("hasValue" to s.mdbListApiKey.isNotBlank()),
                    "aniskip" to mapOf("hasValue" to s.animeSkipClientId.isNotBlank()),
                    "debrid"  to mapOf("hasValue" to addonViewModel.debridToken.isNotBlank())
                )
            },
            onSaveApiKey = { key, value ->
                coroutineScope.launch {
                    when (key) {
                        "tmdb"    -> settingsDataStore.setTmdbUserApiKey(value)
                        "mdblist" -> settingsDataStore.setMdbListApiKey(value)
                        "aniskip" -> settingsDataStore.setAnimeSkipClientId(value)
                        "debrid"  -> addonViewModel.saveDebridToken(value)
                    }
                }
            },
            // D6: Player Settings — expose key numeric/boolean settings
            playerSettingsProvider = {
                val s = settingsState
                mapOf(
                    "autoplay"         to s.isAutoplayEnabled,
                    "skipIntroDurSec"  to s.skipIntroDurationSec,
                    "subtitleSize"     to s.defaultSubtitleSize,
                    "subtitleBold"     to s.subtitleBold,
                    "audioBoostDb"     to s.defaultAudioBoost,
                    "audioDelayMs"     to s.defaultAudioDelayMs,
                    "minBufferMs"      to s.minBufferMs,
                    "maxBufferMs"      to s.maxBufferMs,
                    "playbackBufferMs" to s.bufferForPlaybackMs
                )
            },
            onSavePlayerSetting = { key, value ->
                coroutineScope.launch {
                    when (key) {
                        "autoplay"         -> settingsDataStore.setAutoplayEnabled(value.toBooleanStrictOrNull() ?: return@launch)
                        "skipIntroDurSec"  -> settingsDataStore.setSkipIntroDurationSec(value.toIntOrNull() ?: return@launch)
                        "subtitleSize"     -> settingsDataStore.setDefaultSubtitleSize(value.toIntOrNull() ?: return@launch)
                        "subtitleBold"     -> settingsDataStore.setSubtitleBold(value.toBooleanStrictOrNull() ?: return@launch)
                        "audioBoostDb"     -> settingsDataStore.setDefaultAudioBoost(value.toFloatOrNull() ?: return@launch)
                        "audioDelayMs"     -> settingsDataStore.setDefaultAudioDelayMs(value.toLongOrNull() ?: return@launch)
                        "minBufferMs"      -> settingsDataStore.setBufferSettings(
                            value.toIntOrNull() ?: settingsState.minBufferMs,
                            settingsState.maxBufferMs,
                            settingsState.bufferForPlaybackMs,
                            settingsState.bufferForPlaybackAfterRebufferMs,
                            settingsState.backBufferDurationMs
                        )
                        "maxBufferMs"      -> settingsDataStore.setBufferSettings(
                            settingsState.minBufferMs,
                            value.toIntOrNull() ?: settingsState.maxBufferMs,
                            settingsState.bufferForPlaybackMs,
                            settingsState.bufferForPlaybackAfterRebufferMs,
                            settingsState.backBufferDurationMs
                        )
                        "playbackBufferMs" -> settingsDataStore.setBufferSettings(
                            settingsState.minBufferMs,
                            settingsState.maxBufferMs,
                            value.toIntOrNull() ?: settingsState.bufferForPlaybackMs,
                            settingsState.bufferForPlaybackAfterRebufferMs,
                            settingsState.backBufferDurationMs
                        )
                    }
                }
            },
            backupProvider = {
                com.kitsugi.animelist.data.local.MediaEntryBackup.exportToJson(mediaEntries)
            },
            onImportBackup = { payload ->
                try {
                    val importedEntries = com.kitsugi.animelist.data.local.MediaEntryBackup.importFromJson(payload)
                    val merged = com.kitsugi.animelist.data.local.MediaEntryBackup.mergeWithoutApiDuplicates(
                        currentEntries = mediaEntries,
                        importedEntries = importedEntries
                    )
                    val mergeResult = com.kitsugi.animelist.data.local.MediaEntryBackup.mergeAndSyncEntries(
                        currentEntries = mediaEntries,
                        importedEntries = merged
                    )
                    coroutineScope.launch {
                        mediaRepository.insertAll(mergeResult.toInsert)
                        mergeResult.toUpdate.forEach { mediaRepository.update(it) }
                    }
                    true
                } catch (e: Exception) {
                    android.util.Log.e("TvRootScreen", "Failed to import companion backup", e)
                    false
                }
            },
            onApprovalRequested = { /* UI zaten StateFlow'dan izliyor */ }
        )
    }

    // Uygulama açık olduğu sürece arka planda çalıştır
    DisposableEffect(Unit) {
        try {
            companionServer.start()
        } catch (_: Exception) { /* port mesgul vb. */ }
        onDispose {
            try { companionServer.stop() } catch (_: Exception) {}
        }
    }


    // authViewModel mesajlarını TV snackbar'a yönlendir
    LaunchedEffect(Unit) {
        authViewModel.onShowMessage = { message ->
            appViewModel.showSnackbarMessage(message)
        }
    }

    // appViewModel snackbar mesajlarını izle ve göster
    val snackbarMessage = appViewModel.snackbarMessage
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            appViewModel.clearSnackbarMessage()
        }
    }

    var editingEntry by remember { mutableStateOf<com.kitsugi.animelist.model.MediaEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<com.kitsugi.animelist.model.MediaEntry?>(null) }
    var showMediaGridDialog by remember { mutableStateOf(false) }
    var mediaGridDialogTitle by remember { mutableStateOf("") }
    var mediaGridDialogResults by remember { mutableStateOf<List<JikanSearchResult>>(emptyList()) }

    val navigationState = rememberTvNavigationState()
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    // B1.1: Cold-start / warm-start deep link'leri drain et.
    // DeepLinkHandler.drainPending() tek seferlik tuketim yapar.
    // Compose ilk frame'ini cizince cagrilir; navState hazir.
    LaunchedEffect(Unit) {
        navigationState.drainDeepLink()
    }

    // B1.1: pendingDeepLinkDetail degisince detail sayfasina git.
    // malId parse edilir; JikanSearchResult minimal olarak olusturulur,
    // TvDetailScreen ViewModel'i eksik alanlari async yukler.
    val pendingDetail = navigationState.pendingDeepLinkDetail
    LaunchedEffect(pendingDetail) {
        val link = pendingDetail ?: return@LaunchedEffect
        navigationState.pendingDeepLinkDetail = null // tek seferlik tukettik
        val malId = link.mediaId.toIntOrNull() ?: return@LaunchedEffect
        val syntheticResult = JikanSearchResult(
            malId = malId,
            title = "Yukluyor...",
            subtitle = "",
            type = com.kitsugi.animelist.model.MediaType.Anime,
            total = null,
            score = null,
            isAdult = false,
            imageUrl = null,
            year = null,
            source = link.source.ifBlank { "mal" }
        )
        navigationState.navigateToDetail(syntheticResult)
    }

    // B1.1: pendingDeepLinkManga degisince Manga tabina gec.
    val pendingManga = navigationState.pendingDeepLinkManga
    LaunchedEffect(pendingManga) {
        if (pendingManga != null) {
            navigationState.pendingDeepLinkManga = null
            navigationState.currentTab = TvDestination.MANGA
        }
    }

    // WP-03: Modüler sidebar ve back yönetimi
    val sidebarState = rememberTvSidebarState()
    val drawerFocusRequesters = rememberTvDrawerFocusRequesters()
    val backCoordinator = rememberTvBackCoordinator(
        navigationState = navigationState,
        sidebarState = sidebarState,
        onRequestExit = { showExitConfirmDialog = true }
    )
    val contentFocusRequester = remember { FocusRequester() }

    // Global Focus Recovery: automatically restore focus to contentFocusRequester if lost
    com.kitsugi.animelist.ui.tv.focus.TvGlobalFocusRecovery(fallbackFocusRequester = contentFocusRequester)

    // Sidebar animasyon değerleri (TvSidebarState extension'larından)
    val sidebarWidth = sidebarState.animatedWidth()
    val sidebarLabelAlpha = sidebarState.animatedLabelAlpha()
    val sidebarExpandProgress = sidebarState.animatedExpandProgress()

    val sidebarHazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize()) {
        // TV Snackbar Host — sol alt köşe (overscan-safe)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = KitsugiTvTokens.Spacing.screenHorizontal,
                    bottom = KitsugiTvTokens.Spacing.screenVertical
                )
                .zIndex(100f)
        )
        AnimatedContent(
            targetState = navigationState.detailBackStack.lastOrNull(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tv_detail_transition"
        ) { detail ->
            if (detail != null) {
                // ── Detay Sayfası Overlay ─────────────────────────────────────────
                BackHandler(enabled = true) {
                    navigationState.pop()
                }
                when (detail) {
                    is TvDetailTarget.Media -> {
                        val mediaResult = detail.result
                        val existingApiEntry = mediaEntries.firstOrNull { it.matches(mediaResult) }

                        TvDetailScreen(
                            result = mediaResult,
                            existingEntry = existingApiEntry,
                            onBackClick = { navigationState.pop() },
                            onPlayEpisodeClick = { episode, season ->
                                val streamMalId = if (mediaResult.source.lowercase() == "anilist") {
                                    mediaResult.realMalId
                                } else {
                                    mediaResult.malId
                                }
                                val rawStableId = if (mediaResult.source.lowercase() == "anilist") mediaResult.malId else null
                                val streamAniListId = rawStableId?.let {
                                    if (it >= 100_000_000) it - 100_000_000 else it
                                }
                                navigationState.navigateToStream(
                                    TvStreamArgs(
                                        malId = streamMalId,
                                        aniListId = streamAniListId,
                                        tmdbId = mediaResult.tmdbId,
                                        episode = episode.episodeNumber ?: 1,
                                        season = season,
                                        isMovie = false,
                                        title = mediaResult.title,
                                        posterUrl = mediaResult.imageUrl,
                                        titleEnglish = mediaResult.titleEnglish,
                                        titleRomaji = mediaResult.title,
                                        titleNative = mediaResult.titleJapanese,
                                        startYear = mediaResult.year,
                                        description = mediaResult.subtitle
                                    )
                                )
                            },
                            onPlayMovieClick = {
                                val streamMalId = if (mediaResult.source.lowercase() == "anilist") {
                                    mediaResult.realMalId
                                } else {
                                    mediaResult.malId
                                }
                                val rawStableId = if (mediaResult.source.lowercase() == "anilist") mediaResult.malId else null
                                val streamAniListId = rawStableId?.let {
                                    if (it >= 100_000_000) it - 100_000_000 else it
                                }
                                navigationState.navigateToStream(
                                    TvStreamArgs(
                                        malId = streamMalId,
                                        aniListId = streamAniListId,
                                        tmdbId = mediaResult.tmdbId,
                                        episode = 1,
                                        season = 1,
                                        isMovie = true,
                                        title = mediaResult.title,
                                        posterUrl = mediaResult.imageUrl,
                                        titleEnglish = mediaResult.titleEnglish,
                                        titleRomaji = mediaResult.title,
                                        titleNative = mediaResult.titleJapanese,
                                        startYear = mediaResult.year,
                                        description = mediaResult.subtitle
                                    )
                                )
                            },
                            onToggleLibrary = {
                                appViewModel.addApiSelectionToList(
                                    selection = ApiSearchSelection(
                                        result = mediaResult,
                                        synopsis = mediaResult.subtitle
                                    ),
                                    currentEntries = mediaEntries,
                                    repository = mediaRepository,
                                    isAniListConnected = authViewModel.isAniListConnected,
                                    isMalConnected = authViewModel.isMalConnected,
                                    isSimklConnected = authViewModel.isSimklConnected
                                )
                            },
                            onNavigateToRelationDetail = { rel -> navigationState.navigateToDetail(rel) },
                            onCharacterClick = { charId, src, charName, imgUrl ->
                                navigationState.navigateToCharacterDetail(charId, src, charName, imgUrl)
                            },
                            onStaffClick = { staffId, src, staffName, imgUrl ->
                                navigationState.navigateToStaffDetail(staffId, src, staffName, imgUrl)
                            }
                        )
                    }
                    is TvDetailTarget.Character -> {
                        CharacterDetailPage(
                            characterId = detail.characterId,
                            source = detail.source,
                            name = detail.name,
                            imageUrl = detail.imageUrl,
                            onBackClick = { navigationState.pop() },
                            onMediaClick = { mediaId, mediaType, mediaSource ->
                                val targetResult = JikanSearchResult(
                                    malId = mediaId,
                                    title = "Yükleniyor...",
                                    subtitle = "",
                                    type = mediaType.parseToMediaType(),
                                    total = null,
                                    score = null,
                                    isAdult = false,
                                    imageUrl = null,
                                    year = null,
                                    source = mediaSource
                                )
                                navigationState.navigateToDetail(targetResult)
                            },
                            onStaffClick = { staffId, src, staffName, imgUrl ->
                                navigationState.navigateToStaffDetail(staffId, src, staffName, imgUrl)
                            },
                            titleLanguage = settingsState.titleLanguage
                        )
                    }
                    is TvDetailTarget.Staff -> {
                        StaffDetailPage(
                            staffId = detail.staffId,
                            source = detail.source,
                            name = detail.name,
                            imageUrl = detail.imageUrl,
                            onBackClick = { navigationState.pop() },
                            onCharacterClick = { charId, charSource, charName, imgUrl ->
                                navigationState.navigateToCharacterDetail(charId, charSource, charName, imgUrl)
                            },
                            onMediaClick = { mediaId, mediaType, mediaSource ->
                                val targetResult = JikanSearchResult(
                                    malId = mediaId,
                                    title = "Yükleniyor...",
                                    subtitle = "",
                                    type = mediaType.parseToMediaType(),
                                    total = null,
                                    score = null,
                                    isAdult = false,
                                    imageUrl = null,
                                    year = null,
                                    source = mediaSource
                                )
                                navigationState.navigateToDetail(targetResult)
                            }
                        )
                    }
                    is TvDetailTarget.Studio -> {
                        StudioDetailPage(
                            studioId = detail.studioId,
                            source = detail.source,
                            name = detail.name,
                            imageUrl = detail.imageUrl,
                            onBackClick = { navigationState.pop() },
                            onMediaClick = { mediaId, mediaType, mediaSource ->
                                val targetResult = JikanSearchResult(
                                    malId = mediaId,
                                    title = "Yükleniyor...",
                                    subtitle = "",
                                    type = mediaType.parseToMediaType(),
                                    total = null,
                                    score = null,
                                    isAdult = false,
                                    imageUrl = null,
                                    year = null,
                                    source = mediaSource
                                )
                                navigationState.navigateToDetail(targetResult)
                            }
                        )
                    }
                }
            } else {
            // Root back button flow:
            // 1. If sidebar has focus, show exit confirm dialog.
            BackHandler(enabled = sidebarState.hasFocus && !showExitConfirmDialog) {
                showExitConfirmDialog = true
            }

            // 2. If sidebar does not have focus, request focus on the active tab item of the sidebar.
            BackHandler(enabled = !sidebarState.hasFocus && !showExitConfirmDialog) {
                val selectedRequester = drawerFocusRequesters[navigationState.currentTab]
                if (selectedRequester != null) {
                    try {
                        selectedRequester.requestFocus()
                    } catch (e: Exception) {}
                }
            }

            // Set initial focus to main content on screen load
            LaunchedEffect(Unit) {
                try {
                    contentFocusRequester.requestFocus()
                } catch (e: Exception) {}
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KitsugiColors.Background)
            ) {
                // 1. Content Area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = KitsugiTvTokens.Layout.sidebarCompactWidth)
                        .focusRequester(contentFocusRequester)
                ) {
                    when (navigationState.currentTab) {
                        TvDestination.HOME     -> TvHomeScreen(
                            exploreViewModel = exploreViewModel,
                            currentEntries = mediaEntries,
                            showAdultContent = settingsState.showAdultContent,
                            selectedHomeLayoutId = settingsState.selectedHomeLayoutId,
                            onNavigateToDetail = { navigationState.navigateToDetail(it) },
                            onSeeAllClick = { title, results ->
                                mediaGridDialogTitle = title
                                mediaGridDialogResults = results
                                showMediaGridDialog = true
                            }
                        )
                        TvDestination.SEARCH   -> TvSearchScreen(
                            searchViewModel = searchViewModel,
                            onNavigateToDetail = { navigationState.navigateToDetail(it) }
                        )
                        TvDestination.LIBRARY  -> TvLibraryScreen(
                            onNavigateToDetail = { navigationState.navigateToDetail(it) }
                        )
                        TvDestination.MANGA    -> TvMangaBrowseScreen(
                            repository = mangaViewModel.mangaRepository,
                            onMangaClick = { source, manga ->
                                navigationState.navigateToMangaDetail(source, manga)
                            },
                            onBack = { navigationState.currentTab = TvDestination.HOME }
                        )
                        TvDestination.SETTINGS -> TvSettingsScreen(
                            addonViewModel = addonViewModel,
                            appViewModel = appViewModel,
                            authViewModel = authViewModel,
                            settingsDataStore = settingsDataStore,
                            mediaRepository = mediaRepository,
                            mediaEntries = mediaEntries,
                            onNavigateToAddons = { navigationState.navigateToAddons() },
                            onNavigateToMangaExtension = { navigationState.navigateToMangaExtension() },
                            onNavigateToMangaSourceHealth = { navigationState.navigateToMangaSourceHealth() },
                            onNavigateToCompanion = { navigationState.navigateToCompanion() }
                        )
                        TvDestination.ACCOUNT  -> TvQrLoginScreen(authViewModel = authViewModel)
                    }
                }

                // 2. Floating Collapsible Sidebar Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxHeight()
                        .width(sidebarWidth)
                        .onFocusChanged {
                            sidebarState.onFocusChange(it.hasFocus)
                        }
                ) {
                    ModernSidebarBlurPanel(
                        drawerItems = navItems,
                        selectedDrawerRoute = navigationState.currentTab,
                        keepSidebarFocusDuringCollapse = true,
                        sidebarLabelAlpha = sidebarLabelAlpha,
                        sidebarIconScale = 1f,
                        sidebarExpandProgress = sidebarExpandProgress,
                        isSidebarExpanded = sidebarState.isExpanded,
                        sidebarCollapsePending = false,
                        blurEnabled = false,
                        sidebarHazeState = sidebarHazeState,
                        panelShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                        drawerItemFocusRequesters = drawerFocusRequesters.toMap(),
                        contentFocusRequester = contentFocusRequester,
                        onDrawerItemFocused = {},
                        onDrawerItemClick = { dest ->
                            navigationState.currentTab = dest
                            // Focus back to main content when selected
                            try {
                                contentFocusRequester.requestFocus()
                            } catch (e: Exception) {}
                        },
                        activeProfileName = settingsState.profileName,
                        activeProfileAvatarImageUrl = settingsState.profileImageUri.ifBlank { null },
                        showProfileSelector = true,
                        onSwitchProfile = {
                            navigationState.currentTab = TvDestination.ACCOUNT
                            try {
                                contentFocusRequester.requestFocus()
                            } catch (e: Exception) {}
                        }
                    )
                }
            }
            } // end else (no selectedDetail)
        } // end AnimatedContent

        navigationState.streamTarget?.let { target ->
            TvStreamScreen(
                args = target,
                onBack = { navigationState.pop() }
            )
        }

        // ── Manga Detay Overlay ────────────────────────────────────────────────
        navigationState.mangaDetailTarget?.let { mangaArgs ->
            BackHandler(enabled = true) { navigationState.pop() }
            TvMangaDetailScreen(
                source = mangaArgs.source,
                mangaDetails = mangaArgs.manga,
                onOpenChapter = { chapter ->
                    navigationState.navigateToMangaReader(chapter, mangaArgs.manga, mangaArgs.source)
                },
                onBack = { navigationState.pop() }
            )
        }

        // ── Manga Okuyucu Overlay ───────────────────────────────────────────────
        navigationState.mangaReaderTarget?.let { readerArgs ->
            BackHandler(enabled = true) { navigationState.pop() }
            TvMangaReaderScreen(
                chapter = readerArgs.chapter,
                mangaDetails = readerArgs.manga,
                source = readerArgs.source,
                onBack = { navigationState.pop() }
            )
        }

        // ── Eklenti/Addons Yönetim Overlay ──────────────────────────────────────
        if (navigationState.isAddonsActive) {
            BackHandler(enabled = true) { navigationState.pop() }
            TvAddonsScreen(
                addonViewModel = addonViewModel,
                onBack = { navigationState.pop() }
            )
        }

        // ── Manga Eklenti Yönetim Overlay ──────────────────────────────────────
        if (navigationState.isMangaExtensionActive) {
            BackHandler(enabled = true) { navigationState.pop() }
            TvMangaExtensionScreen(
                sources = mangaViewModel.mangaSources,
                repos = mangaViewModel.mangaReposState,
                repoExtensions = mangaViewModel.mangaRepoExtensionsState,
                repoLoadingState = mangaViewModel.mangaRepoLoadingState,
                bulkInstallRepoUrl = mangaViewModel.mangaBulkInstallRepoUrl,
                bulkInstallDone = mangaViewModel.mangaBulkInstallDone,
                bulkInstallTotal = mangaViewModel.mangaBulkInstallTotal,
                bulkInstallCurrentName = mangaViewModel.mangaBulkInstallCurrentName,
                onAddRepo = { mangaViewModel.addMangaRepo(it) },
                onDeleteRepo = { mangaViewModel.deleteMangaRepo(it) },
                onFetchRepo = { mangaViewModel.fetchMangaRepo(it) },
                onInstallApk = { ext, cb -> mangaViewModel.installMangaApk(ext, cb) },
                onDeleteExtension = { mangaViewModel.deleteMangaExtension(it) },
                onGetSourceHealth = { mangaViewModel.getSourceHealthStatus(it) },
                onGetRuntimeStats = { mangaViewModel.getSourceRuntimeStats(it) },
                onQuickCheckSource = { mangaViewModel.quickCheckSource(it) },
                onNavigateToHealth = { navigationState.navigateToMangaSourceHealth() },
                onBack = { navigationState.pop() }
            )
        }

        // ── Manga Kaynak Sağlığı Tanı Overlay ──────────────────────────────────
        if (navigationState.isMangaSourceHealthActive) {
            BackHandler(enabled = true) { navigationState.pop() }
            TvMangaSourceHealthScreen(
                sources = mangaViewModel.mangaSources,
                checkingSource = mangaViewModel.mangaSources.firstOrNull { mangaViewModel.isSourceBusy(it) },
                onGetSourceHealth = { mangaViewModel.getSourceHealthStatus(it) },
                onGetRuntimeStats = { mangaViewModel.getSourceRuntimeStats(it) },
                onQuickCheckSource = { mangaViewModel.quickCheckSource(it) },
                onRefreshMirror = { mangaViewModel.refreshSourceMirror(it) },
                onClearDiagnostics = { mangaViewModel.resetSourceDiagnostics(it) },
                onClearAllDiagnostics = { mangaViewModel.clearAllSourceDiagnostics() },
                onBack = { navigationState.pop() }
            )
        }

        // ── TV Companion Overlay ───────────────────────────────────────────────
        if (navigationState.isCompanionActive) {
            BackHandler(enabled = true) { navigationState.pop() }

            // Ana QR ekranı (sunucuyu ve oturumu paylaşır)
            TvCompanionQrScreen(
                sessionManager = companionSession,
                companionServer = companionServer,
                onRefreshToken = { companionSession.rotateToken() },
                onBack = { navigationState.pop() }
            )
        }
    }

    val companionSessionState by companionSession.sessionState.collectAsState()

    // Onay dialogı (herhangi bir ekrandayken pending request gelirse görünür)
    TvCompanionApprovalDialog(
        pendingRequest = companionSessionState.pendingRequest,
        onApprove = {
            val approved = companionSession.approvePending()
            if (approved != null) {
                when (approved.action) {
                    "INSTALL_ADDON" -> {
                        addonViewModel.addAddon(approved.payload)
                        appViewModel.showSnackbarMessage("Eklenti kuruluyor: ${approved.payload}")
                    }
                    "ADD_REPO" -> {
                        if (approved.payload.contains("keiyoushi") || approved.payload.contains("manga")) {
                            mangaViewModel.addMangaRepo(approved.payload)
                            appViewModel.showSnackbarMessage("Manga deposu eklendi: ${approved.payload}")
                        } else {
                            addonViewModel.addRepo(approved.payload)
                            appViewModel.showSnackbarMessage("Eklenti deposu eklendi: ${approved.payload}")
                        }
                    }
                }
            }
        },
        onReject = { companionSession.rejectPending() }
    )

    AppDialogHost(
        showGlobalSearch = false,
        onDismissGlobalSearch = {},
        onGlobalSearchResultSelected = {},

        editingEntry = editingEntry,
        scoreFormat = settingsState.scoreFormat,
        onDismissEditing = { editingEntry = null },
        onDeleteEditingEntry = { entry ->
            deletingEntry = entry
        },
        onConfirmEdit = { title, subtitle, type, status, isAdult, progress, total, score, isFavorite, startDate, endDate, notes, tags, priority, isRepeating, repeatCount, repeatValue, volumeProgress, isPrivate, isHiddenFromStatusLists ->
            val entry = editingEntry ?: return@AppDialogHost
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val resolvedStartDate = if (startDate.isNullOrBlank() &&
                (status == WatchStatus.Watching || status == WatchStatus.Completed) &&
                entry.startDate.isNullOrBlank()) today else startDate
            val resolvedEndDate = if (endDate.isNullOrBlank() &&
                status == WatchStatus.Completed &&
                entry.endDate.isNullOrBlank()) today else endDate
            val updatedEntry = entry.copy(
                title = title,
                subtitle = if (subtitle.isBlank()) "Manuel eklenen içerik" else subtitle,
                type = type,
                status = status,
                isAdult = isAdult,
                progress = progress,
                total = total,
                score = score,
                isFavorite = isFavorite,
                startDate = resolvedStartDate,
                endDate = resolvedEndDate,
                notes = notes,
                tags = tags,
                priority = priority ?: 0,
                isRepeating = isRepeating,
                repeatCount = repeatCount,
                repeatValue = repeatValue,
                volumeProgress = volumeProgress,
                isPrivate = isPrivate,
                isHiddenFromStatusLists = isHiddenFromStatusLists
            )
            coroutineScope.launch { mediaRepository.update(updatedEntry) }
            editingEntry = null
        },

        deletingEntry = deletingEntry,
        onConfirmDelete = { entry ->
            coroutineScope.launch { mediaRepository.deleteById(entry.id) }
            navigationState.clearDetails()
            deletingEntry = null
        },
        onDismissDelete = { deletingEntry = null },

        showExitConfirmDialog = showExitConfirmDialog,
        onConfirmExit = {
            (context as? Activity)?.finish()
        },
        onDismissExit = { showExitConfirmDialog = false },

        showMediaGridDialog = showMediaGridDialog,
        mediaGridDialogTitle = mediaGridDialogTitle,
        mediaGridDialogResults = mediaGridDialogResults,
        isAlreadyInList = { result -> mediaEntries.any { it.matches(result) } },
        onMediaGridItemClick = { result ->
            navigationState.navigateToDetail(result)
        },
        onDismissMediaGrid = { showMediaGridDialog = false }
    )
}
