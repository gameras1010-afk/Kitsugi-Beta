package com.kitsugi.animelist

import com.kitsugi.animelist.utils.parseToMediaType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kitsugi.animelist.ui.app.AppNavigationState
import com.kitsugi.animelist.ui.app.AuthViewModel
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.data.remote.matches
import com.kitsugi.animelist.data.remote.firstMatching
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.data.local.MediaEntryRepository
import com.kitsugi.animelist.data.local.MangaMappingEntity
import com.kitsugi.animelist.ui.app.AppViewModel
import com.kitsugi.animelist.ui.screens.detail.StudioDetailPage
import com.kitsugi.animelist.ui.screens.detail.StaffDetailPage
import com.kitsugi.animelist.ui.screens.detail.CharacterDetailPage
import com.kitsugi.animelist.ui.screens.detail.ApiResultDetailPage
import com.kitsugi.animelist.ui.screens.detail.MediaEntryDetailPage
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.data.local.KitsugiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Composable
fun AppRootDetailPages(
    key: AppStateKey,
    navState: AppNavigationState,
    mediaEntries: List<MediaEntry>,
    appSettings: AppSettings,
    settingsDataStore: SettingsDataStore,
    appViewModel: AppViewModel,
    authViewModel: AuthViewModel,
    mediaRepository: MediaEntryRepository,
    coroutineScope: CoroutineScope,
    updateViewModel: com.kitsugi.animelist.core.update.AppUpdateViewModel,
    onEditEntry: (MediaEntry) -> Unit,
    onDeleteEntry: (MediaEntry) -> Unit,
    onIncrementEntryProgress: (MediaEntry) -> Unit,
    onAddApiSelectionToList: (ApiSearchSelection) -> Unit,
    onReadMangaClick: (MediaEntry, MangaMappingEntity?) -> Unit,
    triggerSearch: (String) -> Unit,
    triggerSearchByGenre: (String) -> Unit = {},
    triggerSearchByTag: (String) -> Unit = {}
) {
    when (key) {
        is AppStateKey.StudioDetail -> {
            navState.stateHolder.SaveableStateProvider(key = "studio_${key.depth}_${key.source}_${key.studioId}") {
                StudioDetailPage(
                    studioId = key.studioId,
                    source = key.source,
                    onBackClick = { navState.popDetailStack() },
                    onMediaClick = { mediaId, mediaType, mediaSource ->
                        val existingEntry = mediaEntries.firstMatching(mediaId, mediaSource)
                        if (existingEntry != null) {
                            navState.navigateToDetail(DetailScreen.MediaDetail(existingEntry.id))
                        } else {
                            val searchType = mediaType.parseToMediaType()
                            val searchResult = JikanSearchResult(
                                malId = mediaId,
                                title = "Yükleniyor...",
                                subtitle = "",
                                type = searchType,
                                total = null,
                                score = null,
                                isAdult = false,
                                imageUrl = null,
                                year = null,
                                source = mediaSource
                            )
                            navState.navigateToDetail(DetailScreen.ApiResultDetail(searchResult))
                        }
                    },
                    name = key.name,
                    imageUrl = key.imageUrl,
                    titleLanguage = appSettings.titleLanguage
                )
            }
        }

        is AppStateKey.StaffDetail -> {
            navState.stateHolder.SaveableStateProvider(key = "staff_${key.depth}_${key.source}_${key.staffId}") {
                StaffDetailPage(
                    staffId = key.staffId,
                    source = key.source,
                    onBackClick = { navState.popDetailStack() },
                    onCharacterClick = { charId, charSource, charName, charImageUrl ->
                        navState.navigateToDetail(DetailScreen.CharacterDetail(charId, charSource, charName, charImageUrl))
                    },
                    onMediaClick = { mediaId, mediaType, mediaSource ->
                        val existingEntry = mediaEntries.firstMatching(mediaId, mediaSource)
                        if (existingEntry != null) {
                            navState.navigateToDetail(DetailScreen.MediaDetail(existingEntry.id))
                        } else {
                            val searchType = mediaType.parseToMediaType()
                            val searchResult = JikanSearchResult(
                                malId = mediaId,
                                title = "Yükleniyor...",
                                subtitle = "",
                                type = searchType,
                                total = null,
                                score = null,
                                isAdult = false,
                                imageUrl = null,
                                year = null,
                                source = mediaSource
                            )
                            navState.navigateToDetail(DetailScreen.ApiResultDetail(searchResult))
                        }
                    },
                    name = key.name,
                    imageUrl = key.imageUrl,
                    titleLanguage = appSettings.titleLanguage,
                    preferredTranslator = appSettings.preferredTranslator
                )
            }
        }

        is AppStateKey.CharacterDetail -> {
            navState.stateHolder.SaveableStateProvider(key = "char_${key.depth}_${key.source}_${key.characterId}") {
                CharacterDetailPage(
                    characterId = key.characterId,
                    source = key.source,
                    onBackClick = { navState.popDetailStack() },
                    onStaffClick = { staffId, staffSource, staffName, staffImageUrl ->
                        navState.navigateToDetail(DetailScreen.StaffDetail(staffId, staffSource, staffName, staffImageUrl))
                    },
                    onCharacterClick = { charId, charSource, charName, charImageUrl ->
                        navState.navigateToDetail(DetailScreen.CharacterDetail(charId, charSource, charName, charImageUrl))
                    },
                    onMediaClick = { mediaId, mediaType, mediaSource ->
                        val existingEntry = mediaEntries.firstMatching(mediaId, mediaSource)
                        if (existingEntry != null) {
                            navState.navigateToDetail(DetailScreen.MediaDetail(existingEntry.id))
                        } else {
                            val searchType = mediaType.parseToMediaType()
                            val searchResult = JikanSearchResult(
                                malId = mediaId,
                                title = "Yükleniyor...",
                                subtitle = "",
                                type = searchType,
                                total = null,
                                score = null,
                                isAdult = false,
                                imageUrl = null,
                                year = null,
                                source = mediaSource
                            )
                            navState.navigateToDetail(DetailScreen.ApiResultDetail(searchResult))
                        }
                    },
                    name = key.name,
                    imageUrl = key.imageUrl,
                    titleLanguage = appSettings.titleLanguage,
                    preferredTranslator = appSettings.preferredTranslator
                )
            }
        }

        is AppStateKey.ApiResultDetail -> {
            navState.stateHolder.SaveableStateProvider(key = "api_${key.depth}_${key.result.source}_${key.result.malId}") {
                val existingApiEntry = mediaEntries.firstMatching(key.result)
                ApiResultDetailPage(
                    result = key.result,
                    existingEntry = existingApiEntry,
                    onBackClick = { navState.popDetailStack() },
                    onAddClick = { selection ->
                        onAddApiSelectionToList(selection)
                    },
                    onEditClick = { entry ->
                        onEditEntry(entry)
                    },
                    onRelationClick = { result ->
                        val existingEntry = mediaEntries.firstMatching(result)
                        if (existingEntry != null) {
                            navState.navigateToDetail(DetailScreen.MediaDetail(existingEntry.id))
                        } else {
                            navState.navigateToDetail(DetailScreen.ApiResultDetail(result))
                        }
                    },
                    onCharacterClick = { char ->
                        navState.navigateToDetail(DetailScreen.CharacterDetail(char.id, char.source, char.name, char.imageUrl))
                    },
                    onStaffClick = { staffId, staffSource, staffName, staffImageUrl ->
                        navState.navigateToDetail(DetailScreen.StaffDetail(staffId, staffSource, staffName, staffImageUrl))
                    },
                    onStudioClick = { studioId, studioSource, studioName, studioImageUrl ->
                        navState.navigateToDetail(DetailScreen.StudioDetail(studioId, studioSource, studioName, studioImageUrl))
                    },
                    onUserProfileClick = { userId, username, avatarUrl ->
                        navState.navigateToDetail(DetailScreen.UserProfile(userId, username, avatarUrl))
                    },
                    onSearchQuery = triggerSearch,
                    onSearchByGenre = triggerSearchByGenre,
                    onSearchByTag = triggerSearchByTag,
                    titleLanguage = appSettings.titleLanguage,
                    scoreFormat = appSettings.scoreFormat,
                    hideScores = appSettings.hideScores,
                    showAnimeLogos = appSettings.showAnimeLogos,
                    isAniListConnected = authViewModel.isAniListConnected,
                    isMalConnected = authViewModel.isMalConnected,
                    isSimklConnected = authViewModel.isSimklConnected,
                    onLoginAniList = {
                        authViewModel.startExternalAuth("anilist")
                    },
                    onLoginMal = {
                        authViewModel.startExternalAuth("mal")
                    },
                    onLoginSimkl = {
                        authViewModel.startExternalAuth("simkl")
                    },
                    onReadMangaClick = {
                        navState.detailBackStack = emptyList<DetailScreen>()
                        navState.openMangaBrowse(key.result.title)
                    },
                    mdbListShowImdb = appSettings.mdbListShowImdb,
                    mdbListShowTomatoes = appSettings.mdbListShowTomatoes,
                    mdbListShowMetacritic = appSettings.mdbListShowMetacritic,
                    mdbListShowAudience = appSettings.mdbListShowAudience,
                    mdbListShowLetterboxd = appSettings.mdbListShowLetterboxd,
                    mdbListShowTmdb = appSettings.mdbListShowTmdb,
                    mdbListShowTrakt = appSettings.mdbListShowTrakt,
                    settingsDataStore = settingsDataStore,
                    onToggleFavoriteClick = {
                        existingApiEntry?.let { entry ->
                            val updatedEntry = entry.copy(
                                isFavorite = !entry.isFavorite
                            )
                            coroutineScope.launch {
                                mediaRepository.update(updatedEntry)
                            }
                        }
                    }
                )
            }
        }

        is AppStateKey.MediaDetail -> {
            val entry = mediaEntries.firstOrNull { it.id == key.entryId }
            if (entry != null) {
                navState.stateHolder.SaveableStateProvider(key = "media_${key.depth}_${key.entryId}") {
                    MediaEntryDetailPage(
                        entry = entry,
                        settingsDataStore = settingsDataStore,
                        onBackClick = { navState.popDetailStack() },
                        onIncrementProgressClick = {
                            onIncrementEntryProgress(entry)
                        },
                        onToggleFavoriteClick = {
                            val updatedEntry = entry.copy(
                                isFavorite = !entry.isFavorite
                            )
                            coroutineScope.launch {
                                mediaRepository.update(updatedEntry)
                            }
                        },
                        onSynopsisLoaded = { synopsis ->
                            if (entry.synopsis != synopsis && synopsis.isNotBlank()) {
                                val updatedEntry = entry.copy(
                                    synopsis = synopsis
                                )
                                coroutineScope.launch {
                                    mediaRepository.update(updatedEntry, syncExternal = false)
                                }
                            }
                        },
                        onEditClick = {
                            onEditEntry(entry)
                        },
                        onDeleteClick = {
                            onDeleteEntry(entry)
                        },
                        onRelationClick = { result ->
                            val existingEntry = mediaEntries.firstMatching(result)
                            if (existingEntry != null) {
                                navState.navigateToDetail(DetailScreen.MediaDetail(existingEntry.id))
                            } else {
                                navState.navigateToDetail(DetailScreen.ApiResultDetail(result))
                            }
                        },
                        onCharacterClick = { char ->
                            navState.navigateToDetail(DetailScreen.CharacterDetail(char.id, char.source, char.name, char.imageUrl))
                        },
                        onStaffClick = { staffId, staffSource, staffName, staffImageUrl ->
                            navState.navigateToDetail(DetailScreen.StaffDetail(staffId, staffSource, staffName, staffImageUrl))
                        },
                        onStudioClick = { studioId, studioSource, studioName, studioImageUrl ->
                            navState.navigateToDetail(DetailScreen.StudioDetail(studioId, studioSource, studioName, studioImageUrl))
                        },
                        onUserProfileClick = { userId, username, avatarUrl ->
                            navState.navigateToDetail(DetailScreen.UserProfile(userId, username, avatarUrl))
                        },
                        onSearchQuery = triggerSearch,
                        onSearchByGenre = triggerSearchByGenre,
                        onSearchByTag = triggerSearchByTag,
                        titleLanguage = appSettings.titleLanguage,
                        scoreFormat = appSettings.scoreFormat,
                        hideScores = appSettings.hideScores,
                        showAnimeLogos = appSettings.showAnimeLogos,
                        blurAdultMedia = appSettings.blurAdultMedia,
                        onReadMangaClick = { mapping ->
                            onReadMangaClick(entry, mapping)
                        },
                        preferredTranslator = appSettings.preferredTranslator
                    )
                }
            }
        }
        is AppStateKey.AiringCalendar -> {
            navState.stateHolder.SaveableStateProvider(key = "airing_calendar_${key.depth}") {
                com.kitsugi.animelist.ui.screens.explore.KitsugiAiringCalendarScreen(
                    currentEntries = mediaEntries,
                    titleLanguage = appSettings.titleLanguage,
                    onOpenAiringEntry = { airingEntry ->
                        val result = airingEntry.toJikanSearchResult(preferredSource = key.preferredSource)
                        val existingEntry = mediaEntries.firstMatching(result)
                        if (existingEntry != null) {
                            navState.navigateToDetail(DetailScreen.MediaDetail(existingEntry.id))
                        } else {
                            navState.navigateToDetail(DetailScreen.ApiResultDetail(result))
                        }
                    },
                    onBackClick = { navState.popDetailStack() }
                )
            }
        }

        is AppStateKey.Stats -> {
            navState.stateHolder.SaveableStateProvider(key = "stats_${key.depth}") {
                val context = androidx.compose.ui.platform.LocalContext.current
                val db = KitsugiDatabase.getDatabase(context)
                val statsVm: com.kitsugi.animelist.ui.screens.stats.StatsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return com.kitsugi.animelist.ui.screens.stats.StatsViewModel(db.mediaEntryDao()) as T
                        }
                    }
                )
                val uiState by statsVm.uiState.collectAsState()
                com.kitsugi.animelist.ui.screens.stats.StatsScreen(
                    uiState = uiState,
                    onBack = { navState.popDetailStack() }
                )
            }
        }

        is AppStateKey.Favourites -> {
            navState.stateHolder.SaveableStateProvider(key = "favourites_${key.depth}") {
                com.kitsugi.animelist.ui.screens.profile.tabs.FavouritesScreen(
                    mediaEntries = mediaEntries,
                    titleLanguage = appSettings.titleLanguage,
                    scoreFormat = appSettings.scoreFormat,
                    hideScores = appSettings.hideScores,
                    blurAdultMedia = appSettings.blurAdultMedia,
                    onBackClick = { navState.popDetailStack() },
                    onEntryClick = { entry ->
                        navState.navigateToDetail(DetailScreen.MediaDetail(entry.id))
                    }
                )
            }
        }

        is AppStateKey.About -> {
            navState.stateHolder.SaveableStateProvider(key = "about_${key.depth}") {
                com.kitsugi.animelist.ui.screens.more.AboutScreen(
                    autoUpdateCheckEnabled = appSettings.autoUpdateCheckEnabled,
                    onAutoUpdateCheckEnabledChanged = { enabled ->
                        coroutineScope.launch {
                            settingsDataStore.setAutoUpdateCheckEnabled(enabled)
                        }
                    },
                    onCheckForUpdatesClick = {
                        updateViewModel.checkForUpdates(silent = false, force = true)
                    },
                    onBackClick = { navState.popDetailStack() }
                )
            }
        }

        is AppStateKey.UserProfile -> {
            navState.stateHolder.SaveableStateProvider(key = "user_profile_${key.depth}_${key.userId}") {
                com.kitsugi.animelist.ui.screens.profile.KitsugiUserProfileScreen(
                    userId = key.userId,
                    fallbackUsername = key.username,
                    fallbackAvatar = key.avatarUrl,
                    appSettings = appSettings,
                    mediaEntries = mediaEntries,
                    onBackClick = { navState.popDetailStack() },
                    onFavoriteMediaClick = { mediaId, mediaType, source, title, imageUrl ->
                        val stableId = if (source == "anilist") mediaId + 100_000_000 else mediaId
                        val searchResult = com.kitsugi.animelist.data.remote.JikanSearchResult(
                            malId = stableId,
                            title = title.ifBlank { "Yükleniyor..." },
                            subtitle = "",
                            type = mediaType,
                            total = null,
                            score = null,
                            isAdult = false,
                            imageUrl = imageUrl,
                            year = null,
                            source = source
                        )
                        val existingEntry = mediaEntries.firstMatching(searchResult)
                        if (existingEntry != null) {
                            navState.navigateToDetail(DetailScreen.MediaDetail(existingEntry.id))
                        } else {
                            navState.navigateToDetail(DetailScreen.ApiResultDetail(searchResult))
                        }
                    },
                    onFavoriteCharacterClick = { charId, source, name, imageUrl ->
                        navState.navigateToDetail(DetailScreen.CharacterDetail(charId, source, name, imageUrl))
                    },
                    onFavoriteStaffClick = { staffId, source, name, imageUrl ->
                        navState.navigateToDetail(DetailScreen.StaffDetail(staffId, source, name, imageUrl))
                    },
                    onFavoriteStudioClick = { studioId, source, name, imageUrl ->
                        navState.navigateToDetail(DetailScreen.StudioDetail(studioId, source, name, imageUrl))
                    },
                    onUserProfileClick = { newUserId, newUsername, newAvatar ->
                        navState.navigateToDetail(DetailScreen.UserProfile(newUserId, newUsername, newAvatar))
                    },
                    onGenreClick = { genre -> triggerSearch(genre) },
                    onTagClick = { tag -> triggerSearch(tag) },
                    onOpenUserMediaList = { uId, mType ->
                        navState.navigateToDetail(DetailScreen.UserMediaList(uId, key.username ?: "Kullanıcı", mType))
                    }
                )
            }
        }

        is AppStateKey.UserMediaList -> {
            navState.stateHolder.SaveableStateProvider(key = "user_media_list_${key.depth}_${key.userId}_${key.initialMediaType.name}") {
                com.kitsugi.animelist.ui.screens.profile.KitsugiUserMediaListScreen(
                    userId = key.userId,
                    username = key.username,
                    initialMediaType = key.initialMediaType,
                    appSettings = appSettings,
                    mediaEntries = mediaEntries,
                    onBackClick = { navState.popDetailStack() },
                    onMediaClick = { result ->
                        navState.navigateToDetail(DetailScreen.ApiResultDetail(result))
                    },
                    onLocalEntryClick = { entry ->
                        navState.navigateToDetail(DetailScreen.MediaDetail(entry.id))
                    }
                )
            }
        }

        is AppStateKey.Notifications -> {
            navState.stateHolder.SaveableStateProvider(key = "notifications_${key.depth}") {
                com.kitsugi.animelist.ui.screens.notifications.KitsugiNotificationsScreen(
                    mediaEntries = mediaEntries,
                    isAniListConnected = authViewModel.isAniListConnected,
                    isMalConnected = authViewModel.isMalConnected,
                    isSimklConnected = authViewModel.isSimklConnected,
                    onBack = { navState.popDetailStack() },
                    onOpenApiDetail = { mediaId, source, mediaTypeStr ->
                        val stableId = if (source.equals("anilist", ignoreCase = true) && mediaId < 100_000_000) {
                            mediaId + 100_000_000
                        } else {
                            mediaId
                        }
                        val existingEntry = mediaEntries.firstMatching(stableId, source)
                        if (existingEntry != null) {
                            navState.navigateToDetail(DetailScreen.MediaDetail(existingEntry.id))
                        } else {
                            val mediaType = when (mediaTypeStr?.lowercase()) {
                                "manga" -> MediaType.Manga
                                "movie" -> MediaType.Movie
                                "tv" -> MediaType.TvShow
                                else -> MediaType.Anime
                            }
                            val searchResult = JikanSearchResult(
                                malId = stableId,
                                title = "Yükleniyor...",
                                subtitle = "",
                                type = mediaType,
                                total = null,
                                score = null,
                                isAdult = false,
                                imageUrl = null,
                                year = null,
                                source = source
                            )
                            navState.navigateToDetail(DetailScreen.ApiResultDetail(searchResult))
                        }
                    }
                )
            }
        }

        else -> {
            // No-op or throw for unhandled states
        }
    }
}
