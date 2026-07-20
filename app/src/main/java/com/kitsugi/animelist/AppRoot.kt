package com.kitsugi.animelist

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import com.kitsugi.animelist.ui.components.KitsugiMotion
import com.kitsugi.animelist.ui.components.detailEnterContentTransform
import com.kitsugi.animelist.ui.components.detailExitContentTransform
import com.kitsugi.animelist.ui.components.tabContentTransform
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.platform.LocalDensity
import com.kitsugi.animelist.utils.rememberScrollVisibilityState
import com.kitsugi.animelist.utils.rememberScrollConnection
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import com.kitsugi.animelist.data.local.MediaEntryBackup
import com.kitsugi.animelist.data.local.MediaEntryRepository
import com.kitsugi.animelist.data.local.NetworkMonitor
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.local.CloudstreamRepoEntity
import com.kitsugi.animelist.data.local.CsPluginEntity
import com.kitsugi.animelist.data.local.ManagedAddonEntity
import com.kitsugi.animelist.data.remote.CsPlugin
import com.kitsugi.animelist.data.repository.CloudstreamRepoRepository
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.matches
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.app.AppBulkInstallProgress
import com.kitsugi.animelist.ui.app.AppDialogHost
import com.kitsugi.animelist.ui.app.AppViewModel
import com.kitsugi.animelist.ui.app.AuthViewModel
import com.kitsugi.animelist.ui.app.PlayerSettingsViewModel
import com.kitsugi.animelist.ui.app.ProfileViewModel
import com.kitsugi.animelist.ui.app.AddonViewModel
import com.kitsugi.animelist.ui.app.MangaViewModel
import com.kitsugi.animelist.ui.app.rememberAppNavigationState
import com.kitsugi.animelist.ui.app.AppNavigationState
import com.kitsugi.animelist.DetailScreen
import com.kitsugi.animelist.ui.app.FullScreenMediaGridState
import com.kitsugi.animelist.ui.components.KitsugiApiSearchDialog
import com.kitsugi.animelist.ui.components.KitsugiConfirmDialog
import com.kitsugi.animelist.ui.components.KitsugiMediaEntryEditorDialog
import com.kitsugi.animelist.ui.components.KitsugiMediaGridDialog
import com.kitsugi.animelist.ui.components.KitsugiUpdateDialog
import com.kitsugi.animelist.core.update.AppUpdateViewModel
import com.kitsugi.animelist.ui.navigation.AppBottomBar
import com.kitsugi.animelist.ui.navigation.AppNavigationRail
import com.kitsugi.animelist.ui.navigation.MainTab
import com.kitsugi.animelist.ui.screens.anime.AnimeScreen
import com.kitsugi.animelist.ui.screens.detail.ApiResultDetailPage
import com.kitsugi.animelist.ui.screens.detail.MediaEntryDetailPage
import com.kitsugi.animelist.ui.screens.detail.CharacterDetailPage
import com.kitsugi.animelist.ui.screens.detail.StaffDetailPage
import com.kitsugi.animelist.ui.screens.detail.StudioDetailPage
import com.kitsugi.animelist.ui.screens.explore.ExploreScreen
import com.kitsugi.animelist.ui.screens.explore.ExploreViewModel
import com.kitsugi.animelist.ui.screens.explore.ExploreCategoryType
import com.kitsugi.animelist.ui.screens.fullscreen.FullScreenMediaGridPage
import com.kitsugi.animelist.ui.screens.mylist.MyListScreen
import com.kitsugi.animelist.ui.screens.search.SearchScreen
import com.kitsugi.animelist.ui.screens.search.SearchViewModel
import com.kitsugi.animelist.ui.screens.settings.SettingsScreen
import com.kitsugi.animelist.data.manga.MangaExtensionLoader
import com.kitsugi.animelist.data.manga.MangaSourceRepository
import com.kitsugi.animelist.data.remote.MangaExtensionInfo
import com.kitsugi.animelist.data.remote.MangaRepoClient
import com.kitsugi.animelist.ui.screens.manga.MangaBrowseScreen
import com.kitsugi.animelist.ui.screens.manga.MangaBrowseViewModel
import com.kitsugi.animelist.ui.screens.manga.MangaDetailScreen
import com.kitsugi.animelist.ui.screens.manga.MangaReaderScreen
import com.kitsugi.animelist.ui.screens.manga.MangaScreen
import com.kitsugi.animelist.ui.screens.manga.MangaSourceHealthScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiAccentForThemeId
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


@Composable
fun AppRoot(
    appViewModel: AppViewModel = viewModel()
) {
    val context = LocalContext.current
    val exploreViewModel: ExploreViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val playerSettingsViewModel: PlayerSettingsViewModel = viewModel()
    val addonViewModel: AddonViewModel = viewModel()
    val mangaViewModel: MangaViewModel = viewModel()
    val mangaBrowseViewModel: MangaBrowseViewModel = viewModel(
        factory = MangaBrowseViewModel.Factory(mangaViewModel.mangaRepository)
    )
    val updateViewModel: AppUpdateViewModel = viewModel()
    val updateUiState by updateViewModel.uiState.collectAsState()
    val navState = rememberAppNavigationState()

    val settingsDataStore = remember {
        SettingsDataStore(context.applicationContext)
    }

    val mediaRepository = remember {
        MediaEntryRepository(
            dao = KitsugiDatabase
                .getDatabase(context.applicationContext)
                .mediaEntryDao(),
            context = context.applicationContext,
            onExternalSyncMessage = { message ->
                appViewModel.showSnackbarMessage(message)
            }
        )
    }

    val networkMonitor = remember {
        NetworkMonitor(context.applicationContext)
    }

    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val appSettings by settingsDataStore.settingsFlow.collectAsState(
        initial = AppSettings()
    )

    val mediaEntries by mediaRepository.entriesFlow.collectAsState(
        initial = emptyList()
    )

    val latestMediaEntries by rememberUpdatedState(mediaEntries)

    val addonsList by addonViewModel.addonsList.collectAsState(
        initial = emptyList()
    )

    val reposList by addonViewModel.reposList.collectAsState(
        initial = emptyList()
    )

    val csPluginsList by addonViewModel.csPluginsList.collectAsState(
        initial = emptyList()
    )

    LaunchedEffect(reposList) {
        addonViewModel.syncRepos(reposList)
    }

    // Set callback to route ViewModel messages to the global app snackbar
    addonViewModel.onShowMessage = { message -> appViewModel.showSnackbarMessage(message) }
    mangaViewModel.onShowMessage = { message -> appViewModel.showSnackbarMessage(message) }

    val selectedTab = appViewModel.selectedTab
    val showGlobalSearch = appViewModel.showGlobalSearch
    val backupText = appViewModel.backupText
    val importText = appViewModel.importText
    val importMode = appViewModel.importMode
    val snackbarMessage = appViewModel.snackbarMessage

    val activeAccentColor = KitsugiAccentForThemeId(appSettings.selectedThemeId)

    var pendingExportText by remember {
        mutableStateOf("")
    }
    var pendingMangaSourceReportText by remember {
        mutableStateOf("")
    }

    LaunchedEffect(navState.detailBackStack) {
        navState.clearPreviousScreens()
    }

    var hasSetInitialTab by remember { mutableStateOf(false) }
    LaunchedEffect(appSettings) {
        if (!hasSetInitialTab && appSettings.defaultTab.isNotEmpty()) {
            val targetTab = when (appSettings.defaultTab) {
                "LAST_USED" -> {
                    val lastUsed = appSettings.lastUsedTab
                    MainTab.entries.firstOrNull { it.name == lastUsed } ?: MainTab.Explore
                }
                else -> {
                    MainTab.entries.firstOrNull { it.name == appSettings.defaultTab } ?: MainTab.Explore
                }
            }
            appViewModel.selectTab(targetTab)
            hasSetInitialTab = true
        }
    }

    LaunchedEffect(Unit) {
        appViewModel.loadFilters(context)
    }

    val triggerSearch: (String) -> Unit = { query ->
        navState.detailBackStack = emptyList<DetailScreen>()
        appViewModel.selectTab(MainTab.Search)
        searchViewModel.setQuery(query)
        searchViewModel.search()
    }

    val activeScreen = navState.detailBackStack.lastOrNull()

    val selectedDetailEntry = if (activeScreen is DetailScreen.MediaDetail) {
        mediaEntries.firstOrNull { it.id == activeScreen.entryId }
    } else null

    val selectedApiResult = if (activeScreen is DetailScreen.ApiResultDetail) {
        activeScreen.result
    } else null

    val selectedCharacterIdAndSource = if (activeScreen is DetailScreen.CharacterDetail) {
        Pair(activeScreen.characterId, activeScreen.source)
    } else null

    val selectedStaffIdAndSource = if (activeScreen is DetailScreen.StaffDetail) {
        Pair(activeScreen.staffId, activeScreen.source)
    } else null

    val selectedStudioIdAndSource = if (activeScreen is DetailScreen.StudioDetail) {
        Pair(activeScreen.studioId, activeScreen.source)
    } else null

    var editingEntry by remember {
        mutableStateOf<MediaEntry?>(null)
    }

    var deletingEntry by remember {
        mutableStateOf<MediaEntry?>(null)
    }

    var showMediaGridDialog by remember {
        mutableStateOf(false)
    }

    var mediaGridDialogTitle by remember {
        mutableStateOf("")
    }

    var mediaGridDialogResults by remember {
        mutableStateOf<List<JikanSearchResult>>(emptyList())
    }

    val bottomBarScrollState = rememberScrollVisibilityState(initialVisible = true)
    val bottomBarScrollConnection = rememberScrollConnection(bottomBarScrollState)

    LaunchedEffect(selectedTab, navState.detailBackStack, navState.fullScreenGridState) {
        bottomBarScrollState.show()
        coroutineScope.launch {
            settingsDataStore.setLastUsedTab(selectedTab.name)
        }
    }

    val profileImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            profileViewModel.updateProfileImageUri(uri = uri.toString())
        }
    }

    val bannerImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            profileViewModel.updateBannerImageUri(uri = uri.toString())
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) {
            appViewModel.showSnackbarMessage("Yedek dışa aktarma iptal edildi")
            return@rememberLauncherForActivityResult
        }

        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(pendingExportText)
                }
            } ?: error("Dosya yazılamadı")
        }.onSuccess {
            appViewModel.showSnackbarMessage("Yedek dosyası oluşturuldu")
            pendingExportText = ""
        }.onFailure { error ->
            appViewModel.showSnackbarMessage(
                error.message ?: "Yedek dosyası oluşturulamadı"
            )
            pendingExportText = ""
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            appViewModel.showSnackbarMessage("Yedek içe aktarma iptal edildi")
            return@rememberLauncherForActivityResult
        }

        runCatching {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            } ?: error("Dosya okunamadı")
        }.onSuccess { jsonText ->
            appViewModel.importBackupJsonText(
                jsonText = jsonText,
                currentEntries = latestMediaEntries,
                repository = mediaRepository
            )
        }.onFailure { error ->
            appViewModel.showSnackbarMessage(
                error.message ?: "Yedek dosyası okunamadı"
            )
        }
    }

    val exportMangaSourceReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) {
            appViewModel.showSnackbarMessage("Source raporu dışa aktarma iptal edildi")
            return@rememberLauncherForActivityResult
        }

        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(pendingMangaSourceReportText)
                }
            } ?: error("Dosya yazılamadı")
        }.onSuccess {
            appViewModel.showSnackbarMessage("Manga source raporu dosyaya kaydedildi")
            pendingMangaSourceReportText = ""
        }.onFailure { error ->
            appViewModel.showSnackbarMessage(
                error.message ?: "Source raporu dosyaya kaydedilemedi"
            )
            pendingMangaSourceReportText = ""
        }
    }

    // Auth mesajları appViewModel snackbar'ına yönlendiriliyor
    authViewModel.onShowMessage = { message -> appViewModel.showSnackbarMessage(message) }
    profileViewModel.onShowMessage = { message -> appViewModel.showSnackbarMessage(message) }
    playerSettingsViewModel.onShowMessage = { message -> appViewModel.showSnackbarMessage(message) }

    LaunchedEffect(Unit) {
        authViewModel.refreshAuthState()
        if (appSettings.autoUpdateCheckEnabled) {
            updateViewModel.checkForUpdates(silent = true)
        }

        launch {
            networkMonitor.isOnlineFlow.collect { isOnline ->
                if (isOnline) {
                    val count = mediaRepository.drainPendingQueue()
                    if (count > 0) {
                        appViewModel.showSnackbarMessage("🔄 Çevrimdışı kaydedilen $count işlem senkronize edildi!")
                    }
                }
            }
        }

        ExternalAuthManager.authEvents.collect { event ->
            when (event) {
                is ExternalAuthManager.AuthEvent.Success -> {
                    authViewModel.refreshAuthState()

                    appViewModel.showSnackbarMessage(
                        when (event.serviceName) {
                            "anilist" -> "AniList bağlantısı başarılı, liste aktarılıyor..."
                            "simkl"  -> "Simkl bağlantısı başarılı, liste aktarılıyor..."
                            else     -> "MyAnimeList bağlantısı başarılı, liste aktarılıyor..."
                        }
                    )

                    when (event.serviceName) {
                        "anilist" -> authViewModel.importAniListAnimeList(latestMediaEntries, mediaRepository)
                        "simkl"  -> authViewModel.importSimklList(latestMediaEntries, mediaRepository)
                        else     -> authViewModel.importMalAnimeList(latestMediaEntries, mediaRepository)
                    }

                    launch {
                        val count = mediaRepository.drainPendingQueue()
                        if (count > 0) {
                            appViewModel.showSnackbarMessage("🔄 Çevrimdışı kaydedilen $count işlem senkronize edildi!")
                        }
                    }
                }

                is ExternalAuthManager.AuthEvent.Error -> {
                    appViewModel.showSnackbarMessage(event.message)
                }

                is ExternalAuthManager.AuthEvent.SessionExpired -> {
                    authViewModel.refreshAuthState()
                    appViewModel.showSnackbarMessage(
                        when (event.serviceName) {
                            "simkl" -> "Simkl oturum süresi doldu. Lütfen tekrar bağlanın."
                            else -> "${event.serviceName} oturum süresi doldu."
                        }
                    )
                }
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        val message = snackbarMessage

        if (message != null) {
            snackbarHostState.showSnackbar(message)
            appViewModel.clearSnackbarMessage()
        }
    }

    fun addApiSelectionToList(selection: ApiSearchSelection) {
        appViewModel.addApiSelectionToList(
            selection = selection,
            currentEntries = mediaEntries,
            repository = mediaRepository,
            isAniListConnected = authViewModel.isAniListConnected,
            isMalConnected = authViewModel.isMalConnected,
            isSimklConnected = authViewModel.isSimklConnected
        )
    }

    fun createBackupFile() {
        val timestamp = SimpleDateFormat(
            "yyyyMMdd-HHmm",
            Locale.US
        ).format(Date())

        val filename = "Kitsugi-backup-$timestamp.json"

        pendingExportText = MediaEntryBackup.exportToJson(mediaEntries)

        exportBackupLauncher.launch(filename)
    }

    fun exportMangaSourceReportFile(text: String) {
        val timestamp = SimpleDateFormat(
            "yyyyMMdd-HHmm",
            Locale.US
        ).format(Date())

        val filename = "Kitsugi-manga-sources-$timestamp.txt"

        pendingMangaSourceReportText = text

        exportMangaSourceReportLauncher.launch(filename)
    }

    fun openBackupFile() {
        importBackupLauncher.launch(
            arrayOf(
                "application/json",
                "text/*",
                "*/*"
            )
        )
    }

    fun isAlreadyInList(result: JikanSearchResult): Boolean {
        return mediaEntries.any { entry ->
            entry.matches(result)
        }
    }

    fun openFullScreenSection(
        title: String,
        categoryType: ExploreCategoryType,
        results: List<JikanSearchResult>
    ) {
        navState.fullScreenGridState = FullScreenMediaGridState(
            title = title,
            categoryType = categoryType,
            platform = exploreViewModel.selectedPlatform,
            initialResults = results
        )
    }

    fun openApiDetail(result: JikanSearchResult) {
        navState.navigateToDetail(DetailScreen.ApiResultDetail(result))
    }

    fun incrementEntryProgress(entry: MediaEntry) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val nextProgress = if (entry.total != null) {
            (entry.progress + 1).coerceAtMost(entry.total)
        } else {
            entry.progress + 1
        }

        val nextStatus = if (entry.total != null && nextProgress >= entry.total) {
            WatchStatus.Completed
        } else if (entry.status == WatchStatus.Planned) {
            WatchStatus.Watching
        } else {
            entry.status
        }

        val autoStartDate = if (entry.status == WatchStatus.Planned && entry.startDate.isNullOrBlank()) {
            today
        } else {
            entry.startDate
        }

        val autoEndDate = if (nextStatus == WatchStatus.Completed && entry.endDate.isNullOrBlank()) {
            today
        } else {
            entry.endDate
        }

        val updatedEntry = entry.copy(
            progress = nextProgress,
            status = nextStatus,
            startDate = autoStartDate,
            endDate = autoEndDate
        )

        coroutineScope.launch {
            mediaRepository.update(updatedEntry)
        }
    }

    var showExitConfirmDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        when {
            navState.mangaReaderNavState != null  -> navState.mangaReaderNavState = null
            navState.mangaDetailNavState != null  -> navState.mangaDetailNavState = null
            navState.mangaSourceHealthOpen        -> navState.closeMangaSourceHealth()
            navState.mangaBrowseOpen              -> {
                mangaBrowseViewModel.reset()
                navState.closeMangaBrowse()
            }
            navState.detailBackStack.isNotEmpty() -> navState.popDetailStack()
            navState.fullScreenGridState != null  -> navState.fullScreenGridState = null
            appViewModel.popTabHistory()          -> { /* Tab history popped */ }
            else                                  -> showExitConfirmDialog = true
        }
    }

    val isInFullScreenMode = navState.fullScreenGridState != null || navState.detailBackStack.isNotEmpty() || navState.mangaBrowseOpen || navState.mangaDetailNavState != null || navState.mangaReaderNavState != null || navState.mangaSourceHealthOpen

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    CompositionLocalProvider(
        LocalKitsugiAccent provides activeAccentColor
    ) {
        Scaffold(
            containerColor = KitsugiColors.Background,
            modifier = Modifier.nestedScroll(bottomBarScrollConnection),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    snackbar = { snackbarData ->
                        Snackbar(
                            snackbarData = snackbarData,
                            containerColor = KitsugiColors.SurfaceStrong,
                            contentColor = KitsugiColors.TextPrimary,
                            actionColor = activeAccentColor,
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                )
            },
            bottomBar = {
                // Yatay modda NavigationRail kullanıldığı için BottomBar gizleniyor
                if (!isInFullScreenMode && !isLandscape) {
                    val density = LocalDensity.current
                    val bottomBarOffset by animateFloatAsState(
                        targetValue = if (appSettings.fixedNavBar || bottomBarScrollState.isVisible) 0f else with(density) { 100.dp.toPx() },
                        animationSpec = tween(durationMillis = KitsugiMotion.fastMillis + 50),
                        label = "bottom_bar_offset"
                    )
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationY = bottomBarOffset
                            }
                    ) {
                        AppBottomBar(
                            selectedTab = selectedTab,
                            onTabSelected = { tab ->
                                appViewModel.selectTab(tab)
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            // Yatay modda NavigationRail + içerik yan yana; dikey modda normal tam ekran içerik
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                        end = innerPadding.calculateEndPadding(LayoutDirection.Ltr)
                    )
            ) {
                if (isLandscape && !isInFullScreenMode) {
                    AppNavigationRail(
                        selectedTab = selectedTab,
                        onTabSelected = { tab -> appViewModel.selectTab(tab) }
                    )
                }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(KitsugiColors.Background)
            ) {
                val activeApiResult = selectedApiResult
                val activeFullScreenGrid = navState.fullScreenGridState
                val activeDetailEntry = selectedDetailEntry

                val activeCharacter = selectedCharacterIdAndSource
                val activeStaff = selectedStaffIdAndSource
                val activeStudio = selectedStudioIdAndSource

                val currentDepth = (if (activeFullScreenGrid != null) 1 else 0) +
                                   (if (navState.mangaBrowseOpen) 1 else 0) +
                                   (if (navState.mangaDetailNavState != null) 1 else 0) +
                                   (if (navState.mangaReaderNavState != null) 1 else 0) +
                                   (if (navState.mangaSourceHealthOpen) 1 else 0) +
                                   navState.detailBackStack.size
                val currentAppStateKey = when {
                    navState.mangaReaderNavState != null  -> AppStateKey.MangaReader(depth = currentDepth)
                    navState.mangaDetailNavState != null  -> AppStateKey.MangaDetail(depth = currentDepth)
                    navState.mangaSourceHealthOpen        -> AppStateKey.MangaSourceHealth(depth = currentDepth)
                    activeStudio != null         -> AppStateKey.StudioDetail(activeStudio.first, activeStudio.second, depth = currentDepth, name = (activeScreen as? DetailScreen.StudioDetail)?.name, imageUrl = (activeScreen as? DetailScreen.StudioDetail)?.imageUrl)
                    activeStaff != null          -> AppStateKey.StaffDetail(activeStaff.first, activeStaff.second, depth = currentDepth, name = (activeScreen as? DetailScreen.StaffDetail)?.name, imageUrl = (activeScreen as? DetailScreen.StaffDetail)?.imageUrl)
                    activeCharacter != null      -> AppStateKey.CharacterDetail(activeCharacter.first, activeCharacter.second, depth = currentDepth, name = (activeScreen as? DetailScreen.CharacterDetail)?.name, imageUrl = (activeScreen as? DetailScreen.CharacterDetail)?.imageUrl)
                    activeApiResult != null      -> AppStateKey.ApiResultDetail(activeApiResult, depth = currentDepth)
                    activeDetailEntry != null    -> AppStateKey.MediaDetail(activeDetailEntry.id, depth = currentDepth)
                    activeFullScreenGrid != null -> AppStateKey.FullScreenGrid(activeFullScreenGrid, depth = currentDepth)
                    navState.mangaBrowseOpen              -> AppStateKey.MangaBrowse(depth = currentDepth)
                    activeScreen is DetailScreen.AiringCalendar -> AppStateKey.AiringCalendar(depth = currentDepth)
                    activeScreen is DetailScreen.Stats -> AppStateKey.Stats(depth = currentDepth)
                    activeScreen is DetailScreen.Favourites -> AppStateKey.Favourites(depth = currentDepth)
                    activeScreen is DetailScreen.About -> AppStateKey.About(depth = currentDepth)
                    else                         -> AppStateKey.Tab(selectedTab)
                }

                AppNavigationContent(
                    currentAppStateKey = currentAppStateKey,
                    navState = navState,
                    mediaEntries = mediaEntries,
                    appSettings = appSettings,
                    settingsDataStore = settingsDataStore,
                    appViewModel = appViewModel,
                    exploreViewModel = exploreViewModel,
                    searchViewModel = searchViewModel,
                    authViewModel = authViewModel,
                    profileViewModel = profileViewModel,
                    playerSettingsViewModel = playerSettingsViewModel,
                    updateViewModel = updateViewModel,
                    addonViewModel = addonViewModel,
                    mangaViewModel = mangaViewModel,
                    mangaBrowseViewModel = mangaBrowseViewModel,
                    mediaRepository = mediaRepository,
                    coroutineScope = coroutineScope,
                    backupText = backupText,
                    importText = importText,
                    importMode = importMode,
                    profileImagePickerLauncher = profileImagePickerLauncher,
                    bannerImagePickerLauncher = bannerImagePickerLauncher,
                    onEditEntry = { editingEntry = it },
                    onDeleteEntry = { editingEntry = null; deletingEntry = it },
                    onIncrementEntryProgress = ::incrementEntryProgress,
                    onAddApiSelectionToList = ::addApiSelectionToList,
                    onExportBackupFileClick = ::createBackupFile,
                    onImportBackupFileClick = ::openBackupFile,
                    onOpenApiDetail = ::openApiDetail,
                    onSeeAllSection = ::openFullScreenSection,
                    onOpenMangaReader = { navState.openMangaBrowse() },
                    exportMangaSourceReportFile = ::exportMangaSourceReportFile,
                    triggerSearch = triggerSearch,
                    isAlreadyInList = ::isAlreadyInList
                )

                AppBulkInstallProgress(
                    accentColor = activeAccentColor,
                    bulkInstallRepoUrl = addonViewModel.bulkInstallRepoUrl,
                    bulkInstallRepoName = addonViewModel.bulkInstallRepoName,
                    bulkInstallDone = addonViewModel.bulkInstallDone,
                    bulkInstallTotal = addonViewModel.bulkInstallTotal,
                    bulkInstallCurrentName = addonViewModel.bulkInstallCurrentName,
                    isInFullScreenMode = isInFullScreenMode,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } // Box (içerik alanı)
            } // Row (landscape layout)
        }

        AppDialogHost(
            showGlobalSearch = showGlobalSearch,
            onDismissGlobalSearch = { appViewModel.closeGlobalSearch() },
            onGlobalSearchResultSelected = { selection ->
                addApiSelectionToList(selection)
            },

            editingEntry = editingEntry,
            scoreFormat = appSettings.scoreFormat,
            onDismissEditing = { editingEntry = null },
            onDeleteEditingEntry = { entry ->
                coroutineScope.launch { mediaRepository.deleteById(entry.id) }
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
                navState.popDetailStack()
                deletingEntry = null
            },
            onDismissDelete = { deletingEntry = null },

            showExitConfirmDialog = showExitConfirmDialog,
            onConfirmExit = { showExitConfirmDialog = false },
            onDismissExit = { showExitConfirmDialog = false },

            showMediaGridDialog = showMediaGridDialog,
            mediaGridDialogTitle = mediaGridDialogTitle,
            mediaGridDialogResults = mediaGridDialogResults,
            isAlreadyInList = ::isAlreadyInList,
            onMediaGridItemClick = { result ->
                navState.navigateToDetail(DetailScreen.ApiResultDetail(result))
            },
            onDismissMediaGrid = { showMediaGridDialog = false }
        )

        KitsugiUpdateDialog(
            state = updateUiState,
            onUpdateClick = { updateViewModel.startDownloadAndInstall() },
            onRetryInstallClick = { updateViewModel.retryInstall() },
            onDismiss = { updateViewModel.dismissUpdate() }
        )
    }
}

@Composable
private fun AppNavigationContent(
    currentAppStateKey: AppStateKey,
    navState: AppNavigationState,
    mediaEntries: List<MediaEntry>,
    appSettings: AppSettings,
    settingsDataStore: SettingsDataStore,
    appViewModel: AppViewModel,
    exploreViewModel: ExploreViewModel,
    searchViewModel: SearchViewModel,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    playerSettingsViewModel: PlayerSettingsViewModel,
    updateViewModel: AppUpdateViewModel,
    addonViewModel: AddonViewModel,
    mangaViewModel: MangaViewModel,
    mangaBrowseViewModel: MangaBrowseViewModel,
    mediaRepository: MediaEntryRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    backupText: String,
    importText: String,
    importMode: com.kitsugi.animelist.ui.components.BackupImportMode,
    profileImagePickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    bannerImagePickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onEditEntry: (MediaEntry) -> Unit,
    onDeleteEntry: (MediaEntry) -> Unit,
    onIncrementEntryProgress: (MediaEntry) -> Unit,
    onAddApiSelectionToList: (ApiSearchSelection) -> Unit,
    onExportBackupFileClick: () -> Unit,
    onImportBackupFileClick: () -> Unit,
    onOpenApiDetail: (JikanSearchResult) -> Unit,
    onSeeAllSection: (String, ExploreCategoryType, List<JikanSearchResult>) -> Unit,
    onOpenMangaReader: () -> Unit,
    exportMangaSourceReportFile: (String) -> Unit,
    triggerSearch: (String) -> Unit,
    isAlreadyInList: (JikanSearchResult) -> Boolean
) {

    AnimatedContent(
        targetState = currentAppStateKey,
        label = "page_transition",
        transitionSpec = {
            val fromTab = initialState is AppStateKey.Tab
            val toTab = targetState is AppStateKey.Tab
            when {
                fromTab && toTab -> tabContentTransform()
                initialState.depth > targetState.depth -> detailExitContentTransform()
                else -> detailEnterContentTransform()
            }
        }
    ) { key ->
        when (key) {
            is AppStateKey.StudioDetail,
            is AppStateKey.StaffDetail,
            is AppStateKey.CharacterDetail,
            is AppStateKey.ApiResultDetail,
            is AppStateKey.AiringCalendar,
            is AppStateKey.Stats,
            is AppStateKey.Favourites,
            is AppStateKey.About,
            is AppStateKey.MediaDetail -> {
                AppRootDetailPages(
                    key = key,
                    navState = navState,
                    mediaEntries = mediaEntries,
                    appSettings = appSettings,
                    settingsDataStore = settingsDataStore,
                    appViewModel = appViewModel,
                    authViewModel = authViewModel,
                    mediaRepository = mediaRepository,
                    coroutineScope = coroutineScope,
                    updateViewModel = updateViewModel,
                    onEditEntry = onEditEntry,
                    onDeleteEntry = onDeleteEntry,
                    onIncrementEntryProgress = onIncrementEntryProgress,
                    onAddApiSelectionToList = onAddApiSelectionToList,
                    onReadMangaClick = { entry, mapping ->
                        if (mapping != null) {
                            val source = mangaViewModel.mangaRepository.getSourceByName(mapping.mangaSource)
                            if (source != null) {
                                navState.mangaDetailNavState = MangaDetailNavState(
                                    source = source,
                                    mangaDetails = com.kitsugi.animelist.data.manga.MangaDetails(
                                        url = mapping.mangaUrl,
                                        title = mapping.mangaTitle,
                                        thumbnailUrl = mapping.mangaThumbnail,
                                        source = mapping.mangaSource
                                    )
                                )
                            } else {
                                navState.detailBackStack = emptyList<DetailScreen>()
                                navState.openMangaBrowse(entry.title, entry.id)
                            }
                        } else {
                            navState.detailBackStack = emptyList<DetailScreen>()
                            navState.openMangaBrowse(entry.title, entry.id)
                        }
                    },
                    triggerSearch = triggerSearch
                )
            }

            is AppStateKey.FullScreenGrid -> {
                FullScreenMediaGridPage(
                    title = key.state.title,
                    categoryType = key.state.categoryType,
                    platform = key.state.platform,
                    initialResults = key.state.initialResults,
                    alreadyInList = isAlreadyInList,
                    onItemClick = onOpenApiDetail,
                    onBackClick = {
                        navState.fullScreenGridState = null
                    },
                    titleLanguage = appSettings.titleLanguage,
                    scoreFormat = appSettings.scoreFormat,
                    hideScores = appSettings.hideScores,
                    showAdultContent = appSettings.showAdultContent
                )
            }

            is AppStateKey.MangaReader,
            is AppStateKey.MangaDetail,
            is AppStateKey.MangaSourceHealth,
            is AppStateKey.MangaBrowse -> {
                AppRootMangaPages(
                    key = key,
                    navState = navState,
                    mangaViewModel = mangaViewModel,
                    mangaBrowseViewModel = mangaBrowseViewModel,
                    coroutineScope = coroutineScope,
                    onExportMangaSourceReportFile = exportMangaSourceReportFile
                )
            }

            is AppStateKey.Tab -> {
                AppRootTabPages(
                    key = key,
                    ctx = TabPagesContext(
                        mediaEntries = mediaEntries,
                        appSettings = appSettings,
                        settingsDataStore = settingsDataStore,
                        appViewModel = appViewModel,
                        exploreViewModel = exploreViewModel,
                        searchViewModel = searchViewModel,
                        authViewModel = authViewModel,
                        profileViewModel = profileViewModel,
                        playerSettingsViewModel = playerSettingsViewModel,
                        updateViewModel = updateViewModel,
                        addonViewModel = addonViewModel,
                        mangaViewModel = mangaViewModel,
                        mediaRepository = mediaRepository,
                        coroutineScope = coroutineScope,
                        navState = navState,
                        backupText = backupText,
                        importText = importText,
                        importMode = importMode,
                        onPickProfileImageClick = { profileImagePickerLauncher.launch("image/*") },
                        onPickBannerImageClick = { bannerImagePickerLauncher.launch("image/*") },
                        onExportBackupFileClick = onExportBackupFileClick,
                        onImportBackupFileClick = onImportBackupFileClick,
                        onOpenApiDetail = onOpenApiDetail,
                        onAddApiSelectionToList = onAddApiSelectionToList,
                        onSeeAllSection = onSeeAllSection,
                        onOpenMangaReader = onOpenMangaReader
                    )
                )
            }
        }
    }
}