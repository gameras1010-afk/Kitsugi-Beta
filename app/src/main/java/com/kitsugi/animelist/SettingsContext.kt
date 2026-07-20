package com.kitsugi.animelist

import com.kitsugi.animelist.ui.app.AppNavigationState
import com.kitsugi.animelist.ui.app.AppViewModel
import com.kitsugi.animelist.ui.app.AuthViewModel
import com.kitsugi.animelist.ui.app.PlayerSettingsViewModel
import com.kitsugi.animelist.ui.app.AddonViewModel
import com.kitsugi.animelist.ui.app.MangaViewModel
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.data.local.MediaEntryRepository
import com.kitsugi.animelist.data.local.ManagedAddonEntity
import com.kitsugi.animelist.data.local.CloudstreamRepoEntity
import com.kitsugi.animelist.data.local.CsPluginEntity
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.ui.components.BackupImportMode
import kotlinx.coroutines.CoroutineScope

data class SettingsContext(
    val addonsList: List<ManagedAddonEntity>,
    val addonViewModel: AddonViewModel,
    val reposList: List<CloudstreamRepoEntity>,
    val csPluginsList: List<CsPluginEntity>,
    val appSettings: AppSettings,
    val settingsDataStore: SettingsDataStore,
    val coroutineScope: CoroutineScope,
    val appViewModel: AppViewModel,
    val mediaEntries: List<MediaEntry>,
    val backupText: String,
    val importText: String,
    val importMode: BackupImportMode,
    val authViewModel: AuthViewModel,
    val mediaRepository: MediaEntryRepository,
    val playerSettingsViewModel: PlayerSettingsViewModel,
    val mangaViewModel: MangaViewModel,
    val onPickProfileImageClick: () -> Unit,
    val onPickBannerImageClick: () -> Unit,
    val onExportBackupFileClick: () -> Unit,
    val onImportBackupFileClick: () -> Unit,
    val context: android.content.Context,
    val navState: AppNavigationState
)
