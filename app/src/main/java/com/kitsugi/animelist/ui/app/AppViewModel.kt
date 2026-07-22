package com.kitsugi.animelist.ui.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.local.MediaEntryBackup
import com.kitsugi.animelist.data.local.MediaEntryRepository
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.data.remote.matches
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.components.BackupImportMode
import com.kitsugi.animelist.ui.navigation.MainTab
import kotlinx.coroutines.launch
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppTvSyncEntryPoint {
    fun channelSyncService(): com.kitsugi.animelist.core.recommendations.TvChannelSyncService
}

/**
 * Koordinatör ViewModel:
 *  - Tab seçimi & global arama
 *  - Snackbar mesajları
 *  - MyList filtre / sıralama / scroll state
 *  - Yedekleme (backup/import) text state
 *  - Listeye kayıt ekleme
 *  - Tema / liste görünümü / genel görsel ayarlar
 *  - Tüm liste silme
 *
 *  Auth → AuthViewModel
 *  Profil bilgisi & görseller → ProfileViewModel
 *  Oynatıcı ayarları → PlayerSettingsViewModel
 */
class AppViewModel : ViewModel() {

    // ── Navigation ────────────────────────────────────────────────────────────

    var selectedTab by mutableStateOf(MainTab.Explore)
        private set

    var showGlobalSearch by mutableStateOf(false)
        private set

    // ── Backup / Import ───────────────────────────────────────────────────────

    var backupText by mutableStateOf("")
        private set

    var importText by mutableStateOf("")
        private set

    var importMode by mutableStateOf(BackupImportMode.Replace)
        private set

    // ── Snackbar ──────────────────────────────────────────────────────────────

    var snackbarMessage by mutableStateOf<String?>(null)
        private set

    var snackbarLong by mutableStateOf(false)
        private set

    // ── MyList filters / sort / scroll ────────────────────────────────────────

    var myListSearchQuery by mutableStateOf("")
        private set

    var myListStatusFilterId by mutableStateOf("all")
        private set

    var myListTypeFilterId by mutableStateOf("all")
        private set

    var myListFavoriteFilterId by mutableStateOf("all")
        private set

    var myListScoreFilterId by mutableStateOf("all")
        private set

    var myListYearFilterId by mutableStateOf("all")
        private set

    var myListExtraFilterId by mutableStateOf("all")
        private set

    var myListSortId by mutableStateOf("newest")
        private set

    var myListScrollIndex by mutableIntStateOf(0)
        private set

    var myListScrollOffset by mutableIntStateOf(0)
        private set

    var myListTabIndex by mutableIntStateOf(0)
        private set

    var exploreScrollIndex by mutableIntStateOf(0)
        private set

    var exploreScrollOffset by mutableIntStateOf(0)
        private set

    private val tabHistory = java.util.ArrayList<MainTab>()

    fun selectTab(tab: MainTab) {
        if (selectedTab != tab) {
            tabHistory.remove(tab)
            tabHistory.add(selectedTab)
            selectedTab = tab
        }
    }

    fun popTabHistory(): Boolean {
        if (tabHistory.isNotEmpty()) {
            val prev = tabHistory.removeAt(tabHistory.size - 1)
            selectedTab = prev
            return true
        }
        return false
    }

    fun openGlobalSearch() {
        showGlobalSearch = true
    }

    fun closeGlobalSearch() {
        showGlobalSearch = false
    }

    // ── Snackbar ──────────────────────────────────────────────────────────────

    fun showSnackbarMessage(message: String, long: Boolean = false) {
        snackbarLong = long
        snackbarMessage = message
    }

    fun clearSnackbarMessage() {
        snackbarMessage = null
        snackbarLong = false
    }

    // ── MyList state persistence ──────────────────────────────────────────────

    fun loadFilters(context: Context) {
        val prefs = context.getSharedPreferences("Kitsugi_list_filters", Context.MODE_PRIVATE)
        myListStatusFilterId = prefs.getString("status_filter", "all") ?: "all"
        myListTypeFilterId = prefs.getString("type_filter", "all") ?: "all"
        myListFavoriteFilterId = prefs.getString("favorite_filter", "all") ?: "all"
        myListScoreFilterId = prefs.getString("score_filter", "all") ?: "all"
        myListYearFilterId = prefs.getString("year_filter", "all") ?: "all"
        myListExtraFilterId = prefs.getString("extra_filter", "all") ?: "all"
        myListSortId = prefs.getString("sort", "newest") ?: "newest"
        myListTabIndex = prefs.getInt("tab_index", 0)
    }

    fun updateMyListTabIndex(context: Context, index: Int) {
        myListTabIndex = index
        resetMyListScroll()
        if (index == 2 && myListTypeFilterId == "manga") {
            updateMyListTypeFilter(context, "all")
        }
        context.getSharedPreferences("Kitsugi_list_filters", Context.MODE_PRIVATE)
            .edit().putInt("tab_index", index).apply()
    }

    fun updateMyListScrollPosition(index: Int, offset: Int) {
        myListScrollIndex = index
        myListScrollOffset = offset
    }

    fun updateExploreScrollPosition(index: Int, offset: Int) {
        exploreScrollIndex = index
        exploreScrollOffset = offset
    }

    fun updateMyListSearchQuery(value: String) {
        myListSearchQuery = value
        resetMyListScroll()
    }

    fun updateMyListStatusFilter(context: Context, value: String) {
        myListStatusFilterId = value
        resetMyListScroll()
        context.getSharedPreferences("Kitsugi_list_filters", Context.MODE_PRIVATE)
            .edit().putString("status_filter", value).apply()
    }

    fun updateMyListTypeFilter(context: Context, value: String) {
        myListTypeFilterId = value
        resetMyListScroll()
        context.getSharedPreferences("Kitsugi_list_filters", Context.MODE_PRIVATE)
            .edit().putString("type_filter", value).apply()
    }

    fun updateMyListFavoriteFilter(context: Context, value: String) {
        myListFavoriteFilterId = value
        resetMyListScroll()
        context.getSharedPreferences("Kitsugi_list_filters", Context.MODE_PRIVATE)
            .edit().putString("favorite_filter", value).apply()
    }

    fun updateMyListScoreFilter(context: Context, value: String) {
        myListScoreFilterId = value
        resetMyListScroll()
        context.getSharedPreferences("Kitsugi_list_filters", Context.MODE_PRIVATE)
            .edit().putString("score_filter", value).apply()
    }

    fun updateMyListYearFilter(context: Context, value: String) {
        myListYearFilterId = value
        resetMyListScroll()
        context.getSharedPreferences("Kitsugi_list_filters", Context.MODE_PRIVATE)
            .edit().putString("year_filter", value).apply()
    }

    fun updateMyListExtraFilter(context: Context, value: String) {
        myListExtraFilterId = value
        resetMyListScroll()
        context.getSharedPreferences("Kitsugi_list_filters", Context.MODE_PRIVATE)
            .edit().putString("extra_filter", value).apply()
    }

    fun updateMyListSort(context: Context, value: String) {
        myListSortId = value
        resetMyListScroll()
        context.getSharedPreferences("Kitsugi_list_filters", Context.MODE_PRIVATE)
            .edit().putString("sort", value).apply()
    }

    private fun resetMyListScroll() {
        myListScrollIndex = 0
        myListScrollOffset = 0
    }

    // ── Backup / Import text state ────────────────────────────────────────────

    fun clearBackupText() {
        backupText = ""
    }

    fun updateImportText(value: String) {
        importText = value
    }

    fun clearImportText() {
        importText = ""
    }

    fun updateImportMode(value: BackupImportMode) {
        importMode = value
    }

    fun createBackupText(currentEntries: List<MediaEntry>) {
        backupText = MediaEntryBackup.exportToJson(currentEntries)
    }

    fun importBackupText(currentEntries: List<MediaEntry>, repository: MediaEntryRepository) {
        importBackupJsonText(
            jsonText = importText,
            currentEntries = currentEntries,
            repository = repository
        )
        clearImportText()
    }

    fun importBackupJsonText(
        jsonText: String,
        currentEntries: List<MediaEntry>,
        repository: MediaEntryRepository
    ) {
        runCatching {
            MediaEntryBackup.importFromJson(jsonText)
        }.onSuccess { importedEntries ->
            val entriesToSave = when (importMode) {
                BackupImportMode.Replace -> importedEntries
                BackupImportMode.Append -> MediaEntryBackup.mergeWithoutApiDuplicates(
                    currentEntries = currentEntries,
                    importedEntries = importedEntries
                )
            }

            viewModelScope.launch {
                when (importMode) {
                    BackupImportMode.Replace -> repository.replaceAll(entriesToSave)
                    BackupImportMode.Append -> repository.insertAll(entriesToSave)
                }

                val message = when (importMode) {
                    BackupImportMode.Replace -> "${entriesToSave.size} kayıt dosyadan içe aktarıldı"
                    BackupImportMode.Append -> "${entriesToSave.size} yeni kayıt dosyadan eklendi"
                }
                showSnackbarMessage(message)
            }
        }.onFailure { error ->
            showSnackbarMessage(error.message ?: "Yedek dosyası içe aktarılamadı")
        }
    }

    // ── Add API selection to list ─────────────────────────────────────────────

    fun addApiSelectionToList(
        selection: ApiSearchSelection,
        currentEntries: List<MediaEntry>,
        repository: MediaEntryRepository,
        isAniListConnected: Boolean,
        isMalConnected: Boolean,
        isSimklConnected: Boolean
    ) {
        val result = selection.result

        val targetSource = when {
            result.type == MediaType.TvShow || result.type == MediaType.Movie -> "simkl"
            isAniListConnected -> "anilist"
            isMalConnected -> "mal"
            isSimklConnected -> "simkl"
            else -> if (result.source.equals("anilist", ignoreCase = true)) "anilist" else "mal"
        }

        val isConnected = when (targetSource) {
            "anilist" -> isAniListConnected
            "mal" -> isMalConnected
            "simkl" -> isSimklConnected
            else -> true
        }

        if (!isConnected && (isAniListConnected || isMalConnected || isSimklConnected)) {
            // If user has at least one account connected, allow adding to that connected account
            // e.g. targetSource defaulted to mal, but user is connected to AniList
        } else if (!isConnected) {
            val platformName = when (targetSource) {
                "anilist" -> "AniList"
                "simkl" -> "Simkl"
                else -> "MyAnimeList"
            }
            showSnackbarMessage("Listeye eklemek için önce $platformName hesabını bağlamalısın!")
            return
        }

        val finalSource = when {
            isAniListConnected && (targetSource == "mal" || targetSource == "anilist") -> "anilist"
            isMalConnected && targetSource == "mal" -> "mal"
            isSimklConnected && targetSource == "simkl" -> "simkl"
            else -> targetSource
        }

        val alreadyExists = currentEntries.any { entry -> entry.matches(result) }
        if (alreadyExists) {
            showSnackbarMessage("\"${result.title}\" zaten listende var.")
            return
        }

        val newEntry = MediaEntry(
            id = 0,
            title = result.title,
            subtitle = result.subtitle,
            type = result.type,
            status = WatchStatus.Planned,
            score = result.score,
            progress = 0,
            total = result.total,
            isFavorite = false,
            isAdult = result.isAdult,
            source = finalSource,
            malId = result.malId,
            imageUrl = result.imageUrl,
            year = result.year,
            synopsis = selection.synopsis
        )

        viewModelScope.launch {
            repository.insert(newEntry)
            showSnackbarMessage("Listeye eklendi: ${newEntry.title}")
        }
    }

    // ── Visual / general settings (delegates to SettingsDataStore) ────────────

    fun updateTheme(
        themeId: String,
        settingsDataStore: SettingsDataStore
    ) {
        viewModelScope.launch {
            settingsDataStore.setSelectedThemeId(themeId)
            showSnackbarMessage("Tema güncellendi")
        }
    }

    fun updateListLayout(
        layoutId: String,
        settingsDataStore: SettingsDataStore
    ) {
        viewModelScope.launch {
            settingsDataStore.setSelectedListLayoutId(layoutId)
            showSnackbarMessage("Liste görünümü güncellendi")
        }
    }

    fun updateTitleLanguage(
        titleLanguage: String,
        settingsDataStore: SettingsDataStore
    ) {
        viewModelScope.launch {
            settingsDataStore.setTitleLanguage(titleLanguage)
            showSnackbarMessage("Başlık dili güncellendi")
        }
    }

    fun updateScoreFormat(
        scoreFormat: String,
        settingsDataStore: SettingsDataStore
    ) {
        viewModelScope.launch {
            settingsDataStore.setScoreFormat(scoreFormat)
            showSnackbarMessage("Puan formatı güncellendi")
        }
    }

    fun updateHideScores(
        hide: Boolean,
        settingsDataStore: SettingsDataStore
    ) {
        viewModelScope.launch {
            settingsDataStore.setHideScores(hide)
            showSnackbarMessage(if (hide) "Puanlar gizlendi" else "Puanlar gösteriliyor")
        }
    }

    fun updateShowAnimeLogos(
        show: Boolean,
        settingsDataStore: SettingsDataStore
    ) {
        viewModelScope.launch {
            settingsDataStore.setShowAnimeLogos(show)
            showSnackbarMessage(
                if (show) "Anime logoları etkinleştirildi" else "Anime logoları devre dışı bırakıldı"
            )
        }
    }

    fun updateDnsChoice(
        choice: Int,
        settingsDataStore: SettingsDataStore
    ) {
        viewModelScope.launch {
            settingsDataStore.setDnsChoice(choice)
            com.kitsugi.animelist.core.network.DnsManager.dnsChoice = choice
            showSnackbarMessage("DNS Ayarı Güncellendi")
        }
    }

    // ── Delete all entries ────────────────────────────────────────────────────

    fun deleteAllEntries(repository: MediaEntryRepository) {
        viewModelScope.launch {
            repository.deleteAll()
            showSnackbarMessage("Liste temizlendi")

            // B1.12: Clear TV launcher channels/recommendations
            val ctx = repository.context
            if (ctx != null) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        ctx.applicationContext, AppTvSyncEntryPoint::class.java
                    )
                    entryPoint.channelSyncService().clearAll()
                } catch (e: Exception) {
                    android.util.Log.w("AppViewModel", "Failed to clear TV channel during deleteAllEntries", e)
                }
            }
        }
    }
}