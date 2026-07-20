package com.kitsugi.animelist.ui.components.addons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.local.CloudstreamRepoEntity
import com.kitsugi.animelist.data.local.CsPluginEntity
import com.kitsugi.animelist.data.local.ManagedAddonEntity
import com.kitsugi.animelist.data.remote.CsPlugin
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class CsTabSection { REPOS, INSTALLED }

@Composable
internal fun CloudstreamExtensionsTab(
    repos: List<CloudstreamRepoEntity>,
    addons: List<ManagedAddonEntity>,
    csPlugins: List<CsPluginEntity>,
    repoPlugins: Map<String, List<CsPlugin>?>,
    repoLoadingState: Map<String, Boolean>,
    accentColor: Color,
    onAddRepo: (String) -> Unit,
    onDeleteRepo: (CloudstreamRepoEntity) -> Unit,
    onFetchRepoPlugins: (String) -> Unit,
    onInstallPlugin: (CsPlugin, ((Boolean) -> Unit)?) -> Unit,
    onInstallAllPlugins: (repoUrl: String, repoName: String, plugins: List<CsPlugin>) -> Unit,
    onUpdateAllPlugins: (repoUrl: String, repoName: String, plugins: List<CsPlugin>) -> Unit,
    bulkInstallRepoUrl: String?,
    bulkInstallDone: Int,
    bulkInstallTotal: Int,
    bulkInstallCurrentName: String,
    onToggleCsPlugin: (CsPluginEntity, Boolean) -> Unit,
    onUninstallCsPlugin: (CsPluginEntity) -> Unit,
    onVerifyPlugin: (pluginId: String, pluginName: String) -> Unit
) {
    var activeSection by rememberSaveable { mutableStateOf(CsTabSection.REPOS) }
    var newRepoUrl by rememberSaveable { mutableStateOf("") }

    val pluginInstallStates = remember { mutableStateMapOf<String, PluginInstallState>() }
    val reinstallStates = remember { mutableStateMapOf<String, PluginInstallState>() }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val text = clipboardManager.getText()?.text?.trim() ?: ""
            if (text.isNotBlank()) {
                val isCode = com.kitsugi.animelist.utils.CloudstreamUrlHelper.isShortCode(text)
                val normalized = com.kitsugi.animelist.utils.CloudstreamUrlHelper.normalizeUrl(text)
                val isRepoUrl = normalized.startsWith("http://") || normalized.startsWith("https://") || isCode
                if (isRepoUrl && newRepoUrl.isBlank()) {
                    newRepoUrl = text
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("KitsugiAddonsSettingsDialog", "Failed to auto-read clipboard", e)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Bölüm Başlık Sekmesi
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(KitsugiColors.SurfaceSoft)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    CsTabSection.REPOS to "🌐 Repolar",
                    CsTabSection.INSTALLED to "✅ Kurulu (${csPlugins.size})"
                ).forEach { (section, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeSection == section) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (activeSection == section) accentColor else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .tvClickable(shape = RoundedCornerShape(10.dp)) { activeSection = section }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (activeSection == section) accentColor else KitsugiColors.TextSecondary,
                            fontWeight = if (activeSection == section) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (activeSection == CsTabSection.REPOS) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(KitsugiColors.SurfaceSoft)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Video Sağlayıcı Deposu (CS) Ekle",
                        color = KitsugiColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        "Kitsugi video motoru için eklenti deposu (repo) veya kısa kod (örn. kekikdevam) girin.",
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    // Predefined repos
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val predefinedRepos = listOf(
                            Triple("⚡ Kekik Devam", "Kekik Devam Türkçe eklenti deposu (maarrem)", "https://raw.githubusercontent.com/maarrem/cs-Kekik/master/repo.json"),
                            Triple("⚡ Kekik (feroxx)", "Aktif Kekik eklenti deposu derlemeleri (builds)", "https://raw.githubusercontent.com/feroxx/Kekik-cloudstream/refs/heads/builds/repo.json"),
                            Triple("⚡ Pitipitii (sarapcanagii)", "Pitipitii dizi/anime sağlayıcı eklentileri", "https://raw.githubusercontent.com/sarapcanagii/Pitipitii/master/repo.json"),
                            Triple("⚡ Kraptor (Kraptor123)", "Kraptor123 dizi/film kaynakları", "https://raw.githubusercontent.com/Kraptor123/cs-kraptor/refs/heads/master/repo.json")
                        )
                        predefinedRepos.forEach { (name, desc, url) ->
                            val isAdded = repos.any { it.repoUrl.trim().lowercase() == url.trim().lowercase() }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accentColor.copy(alpha = 0.08f))
                                    .tvClickable(shape = RoundedCornerShape(10.dp)) {
                                        if (!isAdded) onAddRepo(url)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, color = accentColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text(desc, color = KitsugiColors.TextMuted, fontSize = 10.sp)
                                }
                                Text(
                                    if (isAdded) "✓ Eklendi" else "+ Ekle",
                                    color = if (isAdded) KitsugiColors.TextMuted else accentColor,
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    TextField(
                        value = newRepoUrl,
                        onValueChange = { newRepoUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2,
                        maxLines = 6,
                        shape = RoundedCornerShape(18.dp),
                        label = { Text("Repo URL veya kısa kod (her satıra bir tane)") },
                        placeholder = { Text("https://raw.githubusercontent.com/...\nkekikdevam") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = KitsugiColors.TextPrimary, unfocusedTextColor = KitsugiColors.TextPrimary,
                            focusedContainerColor = KitsugiColors.SurfaceStrong, unfocusedContainerColor = KitsugiColors.SurfaceStrong,
                            focusedIndicatorColor = accentColor, unfocusedIndicatorColor = Color.Transparent, cursorColor = accentColor
                        )
                    )
                    Button(
                        onClick = {
                            if (newRepoUrl.isNotBlank()) {
                                newRepoUrl.lines()
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .forEach { url -> onAddRepo(url) }
                                newRepoUrl = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Ekle", tint = KitsugiColors.Surface)
                        Spacer(Modifier.width(6.dp))
                        Text("Ekle", color = KitsugiColors.Surface, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                HorizontalDivider(color = KitsugiColors.Border)
                Spacer(Modifier.height(4.dp))
                Text("Eklenti Depoları (${repos.size})", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (repos.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("Henüz eklenti deposu eklenmemiş.", color = KitsugiColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                items(repos) { repo ->
                    val plugins = repoPlugins[repo.repoUrl]
                    val hasAttemptedFetch = repoPlugins.containsKey(repo.repoUrl)
                    val isLoading = repoLoadingState[repo.repoUrl] == true
                    val isBulkInstalling = bulkInstallRepoUrl == repo.repoUrl

                    CsRepoCard(
                        repo = repo,
                        plugins = plugins,
                        hasAttemptedFetch = hasAttemptedFetch,
                        isLoading = isLoading,
                        addons = addons,
                        csPlugins = csPlugins,
                        accentColor = accentColor,
                        onDeleteRepo = { onDeleteRepo(repo) },
                        onFetchRepoPlugins = { onFetchRepoPlugins(repo.repoUrl) },
                        onInstallAllPlugins = { pluginsToInstall ->
                            onInstallAllPlugins(repo.repoUrl, repo.name, pluginsToInstall)
                        },
                        onUpdateAllPlugins = { pluginsToUpdate ->
                            onUpdateAllPlugins(repo.repoUrl, repo.name, pluginsToUpdate)
                        },
                        isBulkInstalling = isBulkInstalling,
                        bulkInstallDone = bulkInstallDone,
                        bulkInstallTotal = bulkInstallTotal,
                        bulkInstallCurrentName = bulkInstallCurrentName,
                        pluginInstallStates = pluginInstallStates,
                        onInstallPlugin = onInstallPlugin,
                        onStartInstall = { pluginId ->
                            pluginInstallStates[pluginId] = PluginInstallState.LOADING
                        },
                        onInstallResult = { pluginId, success ->
                            pluginInstallStates[pluginId] = if (success) PluginInstallState.SUCCESS else PluginInstallState.FAILURE
                            if (success) {
                                scope.launch {
                                    delay(1500)
                                    pluginInstallStates.remove(pluginId)
                                }
                            }
                        }
                    )
                }
            }
        } else {
            // Yüklü CS Eklentileri
            item {
                Text("Yüklü Video Sağlayıcıları", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Kitsugi video motoru tarafından kullanılan video sağlayıcı (.cs3) eklentileri.",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (csPlugins.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = KitsugiColors.TextMuted, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Henüz eklenti kurulmamış.", color = KitsugiColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Depolar sekmesinden eklenti kurabilirsiniz.", color = KitsugiColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                items(csPlugins) { plugin ->
                    val reinstallState = reinstallStates[plugin.id] ?: PluginInstallState.IDLE

                    CsInstalledPluginRow(
                        plugin = plugin,
                        accentColor = accentColor,
                        reinstallState = reinstallState,
                        onInstallPlugin = onInstallPlugin,
                        onToggleCsPlugin = onToggleCsPlugin,
                        onUninstallCsPlugin = onUninstallCsPlugin,
                        onVerifyPlugin = onVerifyPlugin,
                        onStartReinstall = {
                            reinstallStates[plugin.id] = PluginInstallState.LOADING
                        },
                        onReinstallResult = { success ->
                            reinstallStates[plugin.id] = if (success) PluginInstallState.SUCCESS else PluginInstallState.FAILURE
                            if (success) {
                                scope.launch {
                                    delay(1500)
                                    reinstallStates.remove(plugin.id)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
