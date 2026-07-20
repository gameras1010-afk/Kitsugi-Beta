package com.kitsugi.animelist.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
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
import com.kitsugi.animelist.data.local.MangaSourceStateEntity
import com.kitsugi.animelist.data.manga.MangaSource
import com.kitsugi.animelist.data.remote.CsPlugin
import com.kitsugi.animelist.ui.components.addons.CloudstreamExtensionsTab
import com.kitsugi.animelist.ui.components.addons.MangaExtensionsTab
import com.kitsugi.animelist.ui.components.addons.StremioTab
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiAddonsSettingsDialog(
    addons: List<ManagedAddonEntity>,
    initialDebridToken: String,
    repos: List<CloudstreamRepoEntity>,
    repoPlugins: Map<String, List<CsPlugin>?>,
    repoLoadingState: Map<String, Boolean>,
    csPlugins: List<CsPluginEntity> = emptyList(),
    onAddAddon: (String) -> Unit,
    onToggleAddon: (ManagedAddonEntity, Boolean) -> Unit,
    onDeleteAddon: (ManagedAddonEntity) -> Unit,
    onSaveDebridToken: (String) -> Unit,
    onAddRepo: (String) -> Unit,
    onDeleteRepo: (CloudstreamRepoEntity) -> Unit,
    onFetchRepoPlugins: (String) -> Unit,
    onInstallPlugin: (CsPlugin, ((Boolean) -> Unit)?) -> Unit,
    onInstallAllPlugins: (repoUrl: String, repoName: String, plugins: List<CsPlugin>) -> Unit = { _, _, _ -> },
    onUpdateAllPlugins: (repoUrl: String, repoName: String, plugins: List<CsPlugin>) -> Unit = { _, _, _ -> },
    bulkInstallRepoUrl: String? = null,
    bulkInstallRepoName: String? = null,
    bulkInstallDone: Int = 0,
    bulkInstallTotal: Int = 0,
    bulkInstallCurrentName: String = "",
    bulkInstallResultMessage: String? = null,
    onClearBulkInstallResult: () -> Unit = {},
    onToggleCsPlugin: (CsPluginEntity, Boolean) -> Unit = { _, _ -> },
    onUninstallCsPlugin: (CsPluginEntity) -> Unit = {},
    // Manga extensions
    mangaSources: List<MangaSource> = emptyList(),
    onInstallMangaExtension: (Uri) -> Unit = {},
    onDeleteMangaExtension: (MangaSource) -> Unit = {},
    // Manga repos (Keiyoushi / native)
    mangaRepos: List<String> = emptyList(),
    mangaRepoExtensions: Map<String, List<com.kitsugi.animelist.data.remote.MangaExtensionInfo>?> = emptyMap(),
    mangaRepoLoadingState: Map<String, Boolean> = emptyMap(),
    onAddMangaRepo: (String) -> Unit = {},
    onDeleteMangaRepo: (String) -> Unit = {},
    onFetchMangaRepo: (String) -> Unit = {},
    onInstallMangaApk: (com.kitsugi.animelist.data.remote.MangaExtensionInfo, ((Boolean) -> Unit)?) -> Unit = { _, _ -> },
    onInstallAllMangaExtensions: (repoUrl: String, extensions: List<com.kitsugi.animelist.data.remote.MangaExtensionInfo>) -> Unit = { _, _ -> },
    onUpdateAllMangaExtensions: (repoUrl: String, extensions: List<com.kitsugi.animelist.data.remote.MangaExtensionInfo>) -> Unit = { _, _ -> },
    mangaBulkInstallRepoUrl: String? = null,
    mangaBulkInstallDone: Int = 0,
    mangaBulkInstallTotal: Int = 0,
    mangaBulkInstallCurrentName: String = "",
    /** versionCode karşılaştırması için (Mihon uyumlu) */
    onGetInstalledMangaVersionCode: (String) -> Long = { -1L },
    /** Görüntü amacıyla versionName metni */
    onGetInstalledMangaVersion: (String) -> String? = { null },
    onGetMangaSourceHealthStatus: (MangaSource) -> com.kitsugi.animelist.data.manga.model.SourceHealthStatus = { com.kitsugi.animelist.data.manga.model.SourceHealthStatus.Unknown },
    onGetMangaSourceRuntimeStats: (MangaSource) -> com.kitsugi.animelist.data.manga.model.SourceRuntimeStats = { com.kitsugi.animelist.data.manga.model.SourceRuntimeStats() },
    onGetMangaConfiguredDomain: (MangaSource) -> String? = { null },
    onGetMangaConfiguredBaseUrl: (MangaSource) -> String = { it.baseUrl },
    onGetMangaSourceUserAgent: (MangaSource) -> String? = { null },
    onGetMangaSourceSlowdownEnabled: (MangaSource) -> Boolean = { false },
    onSetMangaSourceUserAgent: (MangaSource, String?) -> Unit = { _, _ -> },
    onSetMangaSourceSlowdownEnabled: (MangaSource, Boolean) -> Unit = { _, _ -> },
    onResetMangaSourceDiagnostics: (MangaSource) -> Unit = {},
    onClearAllMangaSourceDiagnostics: () -> Unit = {},
    onIsMangaSourceBusy: (MangaSource) -> Boolean = { false },
    onQuickCheckMangaSource: (MangaSource) -> Unit = {},
    onRefreshMangaSourceMirror: (MangaSource) -> Unit = {},
    onClearMangaSourceMirror: (MangaSource) -> Unit = {},
    onSetMangaSourceDomain: (MangaSource, String?) -> Unit = { _, _ -> },
    mangaSourceStateReport: List<MangaSourceStateEntity> = emptyList(),
    onOpenMangaSourceHealthScreen: () -> Unit = {},
    onForceCheckMangaUpdates: (onResult: (String) -> Unit) -> Unit = {},
    untrustedRepoToConfirm: String? = null,
    untrustedSignatureToConfirm: Pair<com.kitsugi.animelist.data.remote.MangaExtensionInfo, String>? = null,
    onConfirmUntrustedRepo: (String) -> Unit = {},
    onDismissUntrustedRepo: () -> Unit = {},
    onConfirmUntrustedSignature: (com.kitsugi.animelist.data.remote.MangaExtensionInfo, String) -> Unit = { _, _ -> },
    onDismissUntrustedSignature: () -> Unit = {},
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    var webViewDialogUrl by remember { mutableStateOf<String?>(null) }
    var webViewDialogTitle by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val pagerState = rememberPagerState(initialPage = initialTab, pageCount = { 3 })

    KitsugiSheetOrDialog(
        onDismiss = onDismiss,
        heightFraction = 0.93f
    ) {
            // Header content (Title + Close button)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Eklenti & Akış Ayarları",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = KitsugiColors.TextSecondary)
                    }
                }
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = KitsugiColors.Surface,
                    contentColor = accentColor
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        text = {
                            Text(
                                "Torrent & Akış",
                                fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        text = {
                            Text(
                                "Video Sağlayıcıları",
                                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        text = {
                            Text(
                                "Manga Kaynakları",
                                fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            // Body content (HorizontalPager)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) { page ->
                Column(modifier = Modifier.fillMaxSize()) {
                    // ── Bulk install result banner ──────────────────────────────
                    if (bulkInstallResultMessage != null) {
                        val isError = bulkInstallResultMessage.startsWith("❌") || bulkInstallResultMessage.startsWith("⚠️")
                        val isInfo = bulkInstallResultMessage.startsWith("ℹ️")
                        val bannerColor = when {
                            isError -> Color(0xFFB71C1C).copy(alpha = 0.15f)
                            isInfo  -> Color(0xFF1565C0).copy(alpha = 0.15f)
                            else    -> Color(0xFF1B5E20).copy(alpha = 0.15f)
                        }
                        val textColor = when {
                            isError -> Color(0xFFEF9A9A)
                            isInfo  -> Color(0xFF90CAF9)
                            else    -> Color(0xFFA5D6A7)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(bannerColor)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = bulkInstallResultMessage,
                                color = textColor,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onClearBulkInstallResult,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Kapat",
                                    tint = textColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    
                    // Tab Content based on page index
                    when (page) {
                        0 -> StremioTab(
                            addons = addons,
                            initialDebridToken = initialDebridToken,
                            accentColor = accentColor,
                            onAddAddon = onAddAddon,
                            onToggleAddon = onToggleAddon,
                            onDeleteAddon = onDeleteAddon,
                            onSaveDebridToken = onSaveDebridToken
                        )
                        1 -> CloudstreamExtensionsTab(
                            repos = repos,
                            addons = addons,
                            csPlugins = csPlugins,
                            repoPlugins = repoPlugins,
                            repoLoadingState = repoLoadingState,
                            accentColor = accentColor,
                            onAddRepo = onAddRepo,
                            onDeleteRepo = onDeleteRepo,
                            onFetchRepoPlugins = onFetchRepoPlugins,
                            onInstallPlugin = onInstallPlugin,
                            onInstallAllPlugins = onInstallAllPlugins,
                            onUpdateAllPlugins = onUpdateAllPlugins,
                            bulkInstallRepoUrl = bulkInstallRepoUrl,
                            bulkInstallDone = bulkInstallDone,
                            bulkInstallTotal = bulkInstallTotal,
                            bulkInstallCurrentName = bulkInstallCurrentName,
                            onToggleCsPlugin = onToggleCsPlugin,
                            onUninstallCsPlugin = onUninstallCsPlugin,
                            onVerifyPlugin = { pluginId, pluginName ->
                                scope.launch(Dispatchers.IO) {
                                    val cs3File = java.io.File(context.filesDir, "cs_extensions/${pluginId}.cs3")
                                    var loadedPlugin = com.kitsugi.animelist.data.cloudstream.CsPluginLoader.loadedPlugins[cs3File.absolutePath] as? com.lagradost.cloudstream3.plugins.Plugin
                                    if (loadedPlugin == null) {
                                        com.kitsugi.animelist.data.cloudstream.CsPluginLoader.loadExtension(context, pluginId)
                                    }
                                    val api = com.lagradost.cloudstream3.APIHolder.allProviders.firstOrNull { it.sourcePlugin == cs3File.absolutePath }
                                    val url = api?.mainUrl
                                    withContext(Dispatchers.Main) {
                                        if (!url.isNullOrBlank()) {
                                            webViewDialogUrl = url
                                            webViewDialogTitle = pluginName
                                        } else {
                                            android.widget.Toast.makeText(context, "Eklenti adresi bulunamadı.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                        2 -> MangaExtensionsTab(
                            sources = mangaSources,
                            onVerifySource = { url, name ->
                                webViewDialogUrl = url
                                webViewDialogTitle = name
                            },
                            accentColor = accentColor,
                            onInstallExtension = onInstallMangaExtension,
                            onDeleteExtension = onDeleteMangaExtension,
                            repos = mangaRepos,
                            repoExtensions = mangaRepoExtensions,
                            repoLoadingState = mangaRepoLoadingState,
                            onAddRepo = onAddMangaRepo,
                            onDeleteRepo = onDeleteMangaRepo,
                            onFetchRepo = onFetchMangaRepo,
                            onInstallApk = onInstallMangaApk,
                            onInstallAllExtensions = onInstallAllMangaExtensions,
                            onUpdateAllExtensions = onUpdateAllMangaExtensions,
                            bulkInstallRepoUrl = mangaBulkInstallRepoUrl,
                            bulkInstallDone = mangaBulkInstallDone,
                            bulkInstallTotal = mangaBulkInstallTotal,
                            bulkInstallCurrentName = mangaBulkInstallCurrentName,
                            onGetInstalledVersionCode = onGetInstalledMangaVersionCode,
                            onGetInstalledVersion = onGetInstalledMangaVersion,
                            sourceStateReport = mangaSourceStateReport,
                            onGetSourceHealthStatus = onGetMangaSourceHealthStatus,
                            onGetSourceRuntimeStats = onGetMangaSourceRuntimeStats,
                            onGetConfiguredDomain = onGetMangaConfiguredDomain,
                            onGetConfiguredBaseUrl = onGetMangaConfiguredBaseUrl,
                            onSetSourceDomain = onSetMangaSourceDomain,
                            onGetSourceUserAgent = onGetMangaSourceUserAgent,
                            onGetSourceSlowdownEnabled = onGetMangaSourceSlowdownEnabled,
                            onSetSourceUserAgent = onSetMangaSourceUserAgent,
                            onSetSourceSlowdownEnabled = onSetMangaSourceSlowdownEnabled,
                            onResetSourceDiagnostics = onResetMangaSourceDiagnostics,
                            onClearAllSourceDiagnostics = onClearAllMangaSourceDiagnostics,
                            onIsSourceBusy = onIsMangaSourceBusy,
                            onQuickCheckSource = onQuickCheckMangaSource,
                            onRefreshSourceMirror = onRefreshMangaSourceMirror,
                            onClearSourceMirror = onClearMangaSourceMirror,
                            onOpenSourceHealthScreen = onOpenMangaSourceHealthScreen,
                            onForceCheckUpdates = onForceCheckMangaUpdates
                        )
                    }
                }
            }

            // Footer (Tamam button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Tamam", color = accentColor, fontWeight = FontWeight.SemiBold)
                }
            }
    }

    if (webViewDialogUrl != null) {
        KitsugiWebViewDialog(
            title = webViewDialogTitle ?: "",
            url = webViewDialogUrl!!,
            onDismiss = {
                webViewDialogUrl = null
                webViewDialogTitle = null
            }
        )
    }

    if (untrustedRepoToConfirm != null) {
        AlertDialog(
            onDismissRequest = onDismissUntrustedRepo,
            title = { Text("Güvenli Olmayan Kaynak", color = KitsugiColors.TextPrimary) },
            text = {
                Text(
                    "Eklemek istediğiniz kaynak güvenilir eklenti havuzları listesinde yer almıyor:\n\n$untrustedRepoToConfirm\n\nBu kaynağı eklemek cihazınızda güvenlik riski oluşturabilir. Devam etmek istiyor musunuz?",
                    color = KitsugiColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { onConfirmUntrustedRepo(untrustedRepoToConfirm) }) {
                    Text("Evet, Ekle", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUntrustedRepo) {
                    Text("Vazgeç", color = KitsugiColors.TextSecondary)
                }
            },
            containerColor = KitsugiColors.SurfaceStrong
        )
    }

    if (untrustedSignatureToConfirm != null) {
        val (ext, hash) = untrustedSignatureToConfirm
        AlertDialog(
            onDismissRequest = onDismissUntrustedSignature,
            title = { Text("Güvenli Olmayan Eklenti İmzası", color = KitsugiColors.TextPrimary) },
            text = {
                Text(
                    "Yüklemek istediğiniz eklentinin imzası güvenilir imzalar listesinde yer almıyor:\n\nEklenti: ${ext.name}\nPaket: ${ext.pkg}\nİmza: $hash\n\nBu eklentiyi yüklemek cihazınızda güvenlik riski oluşturabilir. İmzayı güvenilir kabul edip yüklemek istiyor musunuz?",
                    color = KitsugiColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { onConfirmUntrustedSignature(ext, hash) }) {
                    Text("Güven ve Yükle", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUntrustedSignature) {
                    Text("İptal Et", color = KitsugiColors.TextSecondary)
                }
            },
            containerColor = KitsugiColors.SurfaceStrong
        )
    }
}
