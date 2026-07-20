package com.kitsugi.animelist

import androidx.compose.runtime.Composable
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
import com.kitsugi.animelist.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Primary wrapper. Passes SettingsContext to SettingsScreenExtras to stay within JVM 64KB limit.
 */
@Composable
internal fun SettingsScreenContent(
    ctx: SettingsContext
) {
    SettingsScreenExtras(ctx = ctx)
}
