package com.kitsugi.animelist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.kitsugi.animelist.ui.app.AppNavigationState
import com.kitsugi.animelist.ui.app.AppViewModel
import com.kitsugi.animelist.ui.app.AuthViewModel
import com.kitsugi.animelist.ui.app.ProfileViewModel
import com.kitsugi.animelist.ui.app.PlayerSettingsViewModel
import com.kitsugi.animelist.ui.app.AddonViewModel
import com.kitsugi.animelist.ui.app.MangaViewModel
import com.kitsugi.animelist.ui.screens.explore.ExploreViewModel
import com.kitsugi.animelist.ui.screens.search.SearchViewModel
import com.kitsugi.animelist.ui.screens.explore.ExploreCategoryType
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.data.local.MediaEntryRepository
import com.kitsugi.animelist.data.local.ManagedAddonEntity
import com.kitsugi.animelist.data.local.CloudstreamRepoEntity
import com.kitsugi.animelist.data.local.CsPluginEntity
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.ui.navigation.MainTab
import com.kitsugi.animelist.ui.screens.explore.ExploreScreen
import com.kitsugi.animelist.ui.screens.mylist.MyListScreen
import com.kitsugi.animelist.ui.screens.search.SearchScreen
import com.kitsugi.animelist.ui.components.BackupImportMode
import kotlinx.coroutines.CoroutineScope

import com.kitsugi.animelist.ui.app.KitsugiProfileViewModel

data class TabPagesContext(
    val mediaEntries: List<MediaEntry>,
    val appSettings: AppSettings,
    val settingsDataStore: SettingsDataStore,
    val appViewModel: AppViewModel,
    val exploreViewModel: ExploreViewModel,
    val searchViewModel: SearchViewModel,
    val authViewModel: AuthViewModel,
    val profileViewModel: ProfileViewModel,
    val kitsugiProfileViewModel: KitsugiProfileViewModel,
    val playerSettingsViewModel: PlayerSettingsViewModel,
    val updateViewModel: com.kitsugi.animelist.core.update.AppUpdateViewModel,
    val addonViewModel: AddonViewModel,
    val mangaViewModel: MangaViewModel,
    val mediaRepository: MediaEntryRepository,
    val coroutineScope: CoroutineScope,
    val navState: AppNavigationState,
    val backupText: String,
    val importText: String,
    val importMode: BackupImportMode,
    val onPickProfileImageClick: () -> Unit,
    val onPickBannerImageClick: () -> Unit,
    val onExportBackupFileClick: () -> Unit,
    val onImportBackupFileClick: () -> Unit,
    val onOpenApiDetail: (JikanSearchResult) -> Unit,
    val onAddApiSelectionToList: (ApiSearchSelection) -> Unit,
    val onSeeAllSection: (String, ExploreCategoryType, List<JikanSearchResult>) -> Unit,
    val onOpenMangaReader: () -> Unit
)

@Composable
fun AppRootTabPages(
    key: AppStateKey.Tab,
    ctx: TabPagesContext
) {
    val context = LocalContext.current

    val addonsList by ctx.addonViewModel.addonsList.collectAsState(initial = emptyList())
    val reposList by ctx.addonViewModel.reposList.collectAsState(initial = emptyList())
    val csPluginsList by ctx.addonViewModel.csPluginsList.collectAsState(initial = emptyList())

    when (key.tab) {
        MainTab.Explore -> {
            ExploreTabPage(ctx)
        }

        MainTab.MyList -> {
            MyListTabPageWrapper(
                appSettings = ctx.appSettings,
                mediaEntries = ctx.mediaEntries,
                mediaRepository = ctx.mediaRepository,
                appViewModel = ctx.appViewModel,
                authViewModel = ctx.authViewModel,
                navState = ctx.navState,
                context = context
            )
        }

        MainTab.Search -> {
            SearchTabPage(ctx)
        }

        MainTab.Profile -> {
            com.kitsugi.animelist.ui.screens.profile.KitsugiProfileScreen(
                viewModel = ctx.kitsugiProfileViewModel,
                mediaEntries = ctx.mediaEntries,
                isAniListConnected = ctx.authViewModel.isAniListConnected,
                isMalConnected = ctx.authViewModel.isMalConnected,
                isSimklConnected = ctx.authViewModel.isSimklConnected,
                profileName = ctx.appSettings.profileName,
                listTitle = ctx.appSettings.listTitle,
                profileImageUri = ctx.appSettings.profileImageUri,
                bannerImageUri = ctx.appSettings.bannerImageUri,
                appSettings = ctx.appSettings,
                onEntryClick = { entry ->
                    ctx.navState.navigateToDetail(DetailScreen.MediaDetail(entry.id))
                },
                onOpenSettingsClick = {
                    ctx.appViewModel.selectTab(MainTab.Settings)
                },
                onFavoriteMediaClick = { mediaId, mediaType, source ->
                    // Navigate to ApiResultDetail by building a minimal JikanSearchResult
                    val result = com.kitsugi.animelist.data.remote.JikanSearchResult(
                        malId = mediaId,
                        title = "",
                        subtitle = "",
                        type = mediaType,
                        total = null,
                        score = null,
                        isAdult = false,
                        imageUrl = null,
                        year = null,
                        source = source
                    )
                    ctx.navState.navigateToDetail(DetailScreen.ApiResultDetail(result))
                },
                onFavoriteCharacterClick = { charId, source, name, imageUrl ->
                    ctx.navState.navigateToDetail(DetailScreen.CharacterDetail(charId, source, name, imageUrl))
                },
                onFavoriteStaffClick = { staffId, source, name, imageUrl ->
                    ctx.navState.navigateToDetail(DetailScreen.StaffDetail(staffId, source, name, imageUrl))
                }
            )
        }

        MainTab.Settings -> {
            SettingsTabPage(
                addonsList = addonsList,
                reposList = reposList,
                csPluginsList = csPluginsList,
                ctx = ctx,
                context = context
            )
        }
    }
}

@Composable
private fun ExploreTabPage(ctx: TabPagesContext) {
    ExploreScreen(
        currentEntries = ctx.mediaEntries,
        showAdultContent = ctx.appSettings.showAdultContent,
        onAddSelectionToList = ctx.onAddApiSelectionToList,
        onSeeAllSection = ctx.onSeeAllSection,
        onOpenApiDetail = ctx.onOpenApiDetail,
        onOpenMangaReader = ctx.onOpenMangaReader,
        onOpenAiringCalendar = {
            ctx.navState.navigateToDetail(DetailScreen.AiringCalendar)
        },
        initialScrollOffset = ctx.appViewModel.exploreScrollOffset,
        onScrollOffsetChange = { offset ->
            ctx.appViewModel.updateExploreScrollPosition(0, offset)
        },
        viewModel = ctx.exploreViewModel,
        titleLanguage = ctx.appSettings.titleLanguage,
        scoreFormat = ctx.appSettings.scoreFormat,
        hideScores = ctx.appSettings.hideScores,
        showAnimeLogos = ctx.appSettings.showAnimeLogos,
        isSimklConnected = ctx.authViewModel.isSimklConnected
    )
}

@Composable
private fun SearchTabPage(ctx: TabPagesContext) {
    SearchScreen(
        currentEntries = ctx.mediaEntries,
        showAdultContent = ctx.appSettings.showAdultContent,
        onOpenApiDetail = ctx.onOpenApiDetail,
        onAddSelectionToList = ctx.onAddApiSelectionToList,
        viewModel = ctx.searchViewModel,
        titleLanguage = ctx.appSettings.titleLanguage,
        scoreFormat = ctx.appSettings.scoreFormat,
        hideScores = ctx.appSettings.hideScores
    )
}

@Composable
private fun SettingsTabPage(
    addonsList: List<ManagedAddonEntity>,
    reposList: List<CloudstreamRepoEntity>,
    csPluginsList: List<CsPluginEntity>,
    ctx: TabPagesContext,
    context: android.content.Context
) {
    SettingsScreenContent(
        ctx = SettingsContext(
            addonsList = addonsList,
            addonViewModel = ctx.addonViewModel,
            reposList = reposList,
            csPluginsList = csPluginsList,
            appSettings = ctx.appSettings,
            settingsDataStore = ctx.settingsDataStore,
            coroutineScope = ctx.coroutineScope,
            appViewModel = ctx.appViewModel,
            mediaEntries = ctx.mediaEntries,
            backupText = ctx.backupText,
            importText = ctx.importText,
            importMode = ctx.importMode,
            authViewModel = ctx.authViewModel,
            mediaRepository = ctx.mediaRepository,
            playerSettingsViewModel = ctx.playerSettingsViewModel,
            updateViewModel = ctx.updateViewModel,
            mangaViewModel = ctx.mangaViewModel,
            onPickProfileImageClick = ctx.onPickProfileImageClick,
            onPickBannerImageClick = ctx.onPickBannerImageClick,
            onExportBackupFileClick = ctx.onExportBackupFileClick,
            onImportBackupFileClick = ctx.onImportBackupFileClick,
            context = context,
            navState = ctx.navState
        )
    )
}

@Composable
private fun MyListTabPageWrapper(
    appSettings: AppSettings,
    mediaEntries: List<MediaEntry>,
    mediaRepository: MediaEntryRepository,
    appViewModel: AppViewModel,
    authViewModel: AuthViewModel,
    navState: AppNavigationState,
    context: android.content.Context
) {
    MyListScreen(
        selectedListLayoutId = appSettings.selectedListLayoutId,
        showAdultContent = appSettings.showAdultContent,
        appSettings = appSettings,
        searchQuery = appViewModel.myListSearchQuery,
        selectedStatusFilterId = appViewModel.myListStatusFilterId,
        selectedTypeFilterId = appViewModel.myListTypeFilterId,
        selectedFavoriteFilterId = appViewModel.myListFavoriteFilterId,
        selectedScoreFilterId = appViewModel.myListScoreFilterId,
        selectedYearFilterId = appViewModel.myListYearFilterId,
        selectedExtraFilterId = appViewModel.myListExtraFilterId,
        selectedSortId = appViewModel.myListSortId,
        initialScrollIndex = appViewModel.myListScrollIndex,
        initialScrollOffset = appViewModel.myListScrollOffset,
        selectedTabIndex = appViewModel.myListTabIndex,
        onTabIndexChange = { appViewModel.updateMyListTabIndex(context, it) },
        onSearchQueryChange = { appViewModel.updateMyListSearchQuery(it) },
        onStatusFilterChange = { appViewModel.updateMyListStatusFilter(context, it) },
        onTypeFilterChange = { appViewModel.updateMyListTypeFilter(context, it) },
        onFavoriteFilterChange = { appViewModel.updateMyListFavoriteFilter(context, it) },
        onScoreFilterChange = { appViewModel.updateMyListScoreFilter(context, it) },
        onYearFilterChange = { appViewModel.updateMyListYearFilter(context, it) },
        onExtraFilterChange = { appViewModel.updateMyListExtraFilter(context, it) },
        onSortChange = { appViewModel.updateMyListSort(context, it) },
        onScrollPositionChange = { index, offset ->
            appViewModel.updateMyListScrollPosition(index, offset)
        },
        onExternalSyncMessage = { appViewModel.showSnackbarMessage(it) },
        isAniListConnected = authViewModel.isAniListConnected,
        isMalConnected = authViewModel.isMalConnected,
        isSimklConnected = authViewModel.isSimklConnected,
        isSimklSessionExpired = authViewModel.isSimklSessionExpired,
        onLoginAniList = { authViewModel.startExternalAuth("anilist") },
        onLoginMal = { authViewModel.startExternalAuth("mal") },
        onLoginSimkl = { authViewModel.startExternalAuth("simkl") },
        onSyncAniList = { authViewModel.importAniListAnimeList(mediaEntries, mediaRepository) },
        onSyncMal = { authViewModel.importMalAnimeList(mediaEntries, mediaRepository) },
        onSyncSimkl = { authViewModel.importSimklList(mediaEntries, mediaRepository) },
        onEntryClick = { entry ->
            navState.navigateToDetail(DetailScreen.MediaDetail(entry.id))
        },
        onSettingsClick = {
            appViewModel.selectTab(MainTab.Settings)
        }
    )
}
