package com.kitsugi.animelist.ui.components.addons

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.local.CloudstreamRepoEntity
import com.kitsugi.animelist.data.local.CsPluginEntity
import com.kitsugi.animelist.data.local.ManagedAddonEntity
import com.kitsugi.animelist.data.remote.CsPlugin
import com.kitsugi.animelist.ui.theme.KitsugiColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CsRepoCard(
    repo: CloudstreamRepoEntity,
    plugins: List<CsPlugin>?,
    hasAttemptedFetch: Boolean,
    isLoading: Boolean,
    addons: List<ManagedAddonEntity>,
    csPlugins: List<CsPluginEntity>,
    accentColor: Color,
    onDeleteRepo: () -> Unit,
    onFetchRepoPlugins: () -> Unit,
    onInstallAllPlugins: (plugins: List<CsPlugin>) -> Unit,
    onUpdateAllPlugins: (plugins: List<CsPlugin>) -> Unit,
    isBulkInstalling: Boolean,
    bulkInstallDone: Int,
    bulkInstallTotal: Int,
    bulkInstallCurrentName: String,
    pluginInstallStates: Map<String, PluginInstallState>,
    onInstallPlugin: (CsPlugin, ((Boolean) -> Unit)?) -> Unit,
    onStartInstall: (String) -> Unit,
    onInstallResult: (String, Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedLanguages by remember { mutableStateOf(setOf<String>()) }
    var selectedTvTypes by remember { mutableStateOf(setOf<String>()) }

    val totalCount = plugins?.size ?: 0
    val installedCount = plugins?.count { plugin ->
        val isStremio = plugin.url.trim().endsWith("/manifest.json", ignoreCase = true)
        if (isStremio) {
            addons.any { it.manifestUrl.trim().equals(plugin.url.trim(), ignoreCase = true) }
        } else {
            csPlugins.any { it.id == plugin.internalName }
        }
    } ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(KitsugiColors.SurfaceSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tvClickable(shape = RoundedCornerShape(18.dp)) {
                    if (isExpanded) {
                        isExpanded = false
                    } else {
                        isExpanded = true
                        if (plugins == null) onFetchRepoPlugins()
                    }
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = accentColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    repo.name,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!repo.description.isNullOrBlank()) {
                    Text(
                        repo.description,
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accentColor, strokeWidth = 2.dp)
            } else {
                Icon(
                    if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = KitsugiColors.TextSecondary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(52.dp))

            if (plugins == null) {
                Text(
                    text = if (isLoading) "Eklentiler alınıyor..." else "Eklentiler yüklenemedi",
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(KitsugiColors.AccentRed.copy(alpha = 0.15f))
                        .tvClickable(shape = RoundedCornerShape(8.dp)) { onDeleteRepo() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Sil", tint = KitsugiColors.AccentRed, modifier = Modifier.size(16.dp))
                }
            } else {
                val allInstalled = totalCount > 0 && installedCount == totalCount
                val hasUpdates = plugins.any { plugin ->
                    val isStremio = plugin.url.trim().endsWith("/manifest.json", ignoreCase = true)
                    if (isStremio) {
                        false
                    } else {
                        val installedPlugin = csPlugins.find { it.id == plugin.internalName }
                        installedPlugin != null && installedPlugin.version < plugin.version
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                allInstalled -> KitsugiColors.AccentGreen.copy(alpha = 0.12f)
                                hasUpdates -> KitsugiColors.AccentOrange.copy(alpha = 0.12f)
                                else -> KitsugiColors.Border.copy(alpha = 0.3f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (allInstalled) "✓ $installedCount/$totalCount" else "$installedCount/$totalCount Yüklü",
                        color = when {
                            allInstalled -> KitsugiColors.AccentGreen
                            hasUpdates -> KitsugiColors.AccentOrange
                            else -> KitsugiColors.TextSecondary
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(8.dp))

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasUpdates && !isBulkInstalling) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KitsugiColors.AccentOrange.copy(alpha = 0.15f))
                                .tvClickable(shape = RoundedCornerShape(8.dp)) {
                                    onUpdateAllPlugins(plugins)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.ArrowCircleUp, contentDescription = "Tümünü Güncelle", tint = KitsugiColors.AccentOrange, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    if (isBulkInstalling) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accentColor, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    } else if (!allInstalled) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .tvClickable(shape = RoundedCornerShape(8.dp)) {
                                    onInstallAllPlugins(plugins)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.DownloadForOffline, contentDescription = "Tümünü İndir", tint = accentColor, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(KitsugiColors.AccentRed.copy(alpha = 0.15f))
                            .tvClickable(shape = RoundedCornerShape(8.dp)) { onDeleteRepo() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Sil", tint = KitsugiColors.AccentRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (isBulkInstalling && bulkInstallTotal > 0) {
            val fraction = bulkInstallDone.toFloat() / bulkInstallTotal.toFloat()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentColor.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⏳ Kuruluyor...", color = accentColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("$bulkInstallDone / $bulkInstallTotal", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
                if (bulkInstallCurrentName.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(text = "📦 $bulkInstallCurrentName", color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.15f)
                )
            }
        }

        if (isExpanded) {
            HorizontalDivider(color = KitsugiColors.Border, modifier = Modifier.padding(horizontal = 12.dp))
            if (!hasAttemptedFetch || isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (plugins == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Eklentiler yüklenemedi. (Bağlantı veya URL hatası)", color = KitsugiColors.AccentRed, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Yeniden Dene",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .tvClickable(shape = RoundedCornerShape(8.dp)) { onFetchRepoPlugins() }
                                .background(accentColor.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            } else if (plugins.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Bu repoda eklenti bulunamadı.", color = KitsugiColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                val languages = plugins.mapNotNull { it.language?.uppercase() }.distinct().sorted()
                val tvTypes = plugins.flatMap { it.tvTypes ?: emptyList() }.distinct().sorted()

                val filteredPlugins = plugins.filter { plugin ->
                    val langMatch = selectedLanguages.isEmpty() || (plugin.language?.uppercase() in selectedLanguages)
                    val tvTypeMatch = selectedTvTypes.isEmpty() || (plugin.tvTypes?.any { it in selectedTvTypes } == true)
                    langMatch && tvTypeMatch
                }

                if (languages.size > 1 || tvTypes.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (languages.size > 1) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                languages.forEach { lang ->
                                    val isSel = lang in selectedLanguages
                                    FilterChip(
                                        text = lang,
                                        selected = isSel,
                                        onClick = {
                                            selectedLanguages = if (isSel) selectedLanguages - lang else selectedLanguages + lang
                                        },
                                        accentColor = accentColor
                                    )
                                }
                            }
                        }
                        if (tvTypes.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                tvTypes.forEach { type ->
                                    val isSel = type in selectedTvTypes
                                    FilterChip(
                                        text = type,
                                        selected = isSel,
                                        onClick = {
                                            selectedTvTypes = if (isSel) selectedTvTypes - type else selectedTvTypes + type
                                        },
                                        accentColor = accentColor
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = KitsugiColors.Border.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 12.dp))
                }

                if (filteredPlugins.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Filtrelere uygun eklenti bulunamadı.", color = KitsugiColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredPlugins.forEach { plugin ->
                            val isStremio = plugin.url.trim().endsWith("/manifest.json", ignoreCase = true)
                            val isInstalled = if (isStremio) {
                                addons.any { it.manifestUrl.trim().equals(plugin.url.trim(), ignoreCase = true) }
                            } else {
                                csPlugins.any { it.id == plugin.internalName }
                            }
                            val installState = pluginInstallStates[plugin.internalName] ?: PluginInstallState.IDLE
                            val installedPluginItem = csPlugins.find { it.id == plugin.internalName }
                            val installedVersion = installedPluginItem?.version

                            CsPluginRow(
                                plugin = plugin,
                                installState = installState,
                                isInstalled = isInstalled,
                                installedVersion = installedVersion,
                                installedPlugin = installedPluginItem,
                                accentColor = accentColor,
                                onInstallPlugin = onInstallPlugin,
                                onStartInstall = { onStartInstall(plugin.internalName) },
                                onInstallResult = { success -> onInstallResult(plugin.internalName, success) }
                            )
                        }
                    }
                }
            }
        }
    }
}
