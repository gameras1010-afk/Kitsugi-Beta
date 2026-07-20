package com.kitsugi.animelist.ui.components.addons

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.manga.MangaSource
import com.kitsugi.animelist.data.manga.stableSourceKey
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.tv.components.TvDialog
import androidx.compose.ui.platform.LocalClipboardManager

private enum class MangaTabSection { REPOS, INSTALLED }
private enum class MangaInstallState { IDLE, LOADING, SUCCESS, FAILURE }

@Composable
internal fun MangaExtensionsTab(
    sources: List<MangaSource>,
    accentColor: Color,
    onInstallExtension: (Uri) -> Unit,
    onDeleteExtension: (MangaSource) -> Unit,
    repos: List<String>,
    repoExtensions: Map<String, List<com.kitsugi.animelist.data.remote.MangaExtensionInfo>?>,
    repoLoadingState: Map<String, Boolean>,
    onAddRepo: (String) -> Unit,
    onDeleteRepo: (String) -> Unit,
    onFetchRepo: (String) -> Unit,
    onInstallApk: (com.kitsugi.animelist.data.remote.MangaExtensionInfo, ((Boolean) -> Unit)?) -> Unit,
    onInstallAllExtensions: (String, List<com.kitsugi.animelist.data.remote.MangaExtensionInfo>) -> Unit = { _, _ -> },
    onUpdateAllExtensions: (String, List<com.kitsugi.animelist.data.remote.MangaExtensionInfo>) -> Unit = { _, _ -> },
    bulkInstallRepoUrl: String? = null,
    bulkInstallDone: Int = 0,
    bulkInstallTotal: Int = 0,
    bulkInstallCurrentName: String = "",
    /** Kurulu APK'nın versionCode'unu döndürür. Kurulu değilse -1L. */
    onGetInstalledVersionCode: (String) -> Long = { -1L },
    /** Kurulu APK'nın görüntü sürüm metnini döndürür (display için). */
    onGetInstalledVersion: (String) -> String? = { null },
    sourceStateReport: List<com.kitsugi.animelist.data.local.MangaSourceStateEntity> = emptyList(),
    onGetSourceHealthStatus: (MangaSource) -> com.kitsugi.animelist.data.manga.model.SourceHealthStatus = { com.kitsugi.animelist.data.manga.model.SourceHealthStatus.Unknown },
    onGetSourceRuntimeStats: (MangaSource) -> com.kitsugi.animelist.data.manga.model.SourceRuntimeStats = { com.kitsugi.animelist.data.manga.model.SourceRuntimeStats() },
    onGetConfiguredDomain: (MangaSource) -> String? = { null },
    onGetConfiguredBaseUrl: (MangaSource) -> String = { it.baseUrl },
    onSetSourceDomain: (MangaSource, String?) -> Unit = { _, _ -> },
    onGetSourceUserAgent: (MangaSource) -> String? = { null },
    onGetSourceSlowdownEnabled: (MangaSource) -> Boolean = { false },
    onSetSourceUserAgent: (MangaSource, String?) -> Unit = { _, _ -> },
    onSetSourceSlowdownEnabled: (MangaSource, Boolean) -> Unit = { _, _ -> },
    onResetSourceDiagnostics: (MangaSource) -> Unit = {},
    onClearAllSourceDiagnostics: () -> Unit = {},
    onIsSourceBusy: (MangaSource) -> Boolean = { false },
    onQuickCheckSource: (MangaSource) -> Unit = {},
    onRefreshSourceMirror: (MangaSource) -> Unit = {},
    onClearSourceMirror: (MangaSource) -> Unit = {},
    onOpenSourceHealthScreen: () -> Unit = {},
    onForceCheckUpdates: (onResult: (String) -> Unit) -> Unit = {},
    onVerifySource: ((String, String) -> Unit)? = null,
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { onInstallExtension(it) } }

    var activeSection by rememberSaveable { mutableStateOf(MangaTabSection.REPOS) }
    var deleteTarget by remember { mutableStateOf<MangaSource?>(null) }
    var manageTarget by remember { mutableStateOf<MangaSource?>(null) }
    var reportOpen by remember { mutableStateOf(false) }
    var reportFilter by rememberSaveable { mutableStateOf("ALL") }
    var reportSort by rememberSaveable { mutableStateOf("UPDATED") }
    var newRepoUrl by rememberSaveable { mutableStateOf("") }
    var expandedRepo by rememberSaveable { mutableStateOf<String?>(null) }
    var langFilter by rememberSaveable { mutableStateOf("Tümü") }
    val installStates = remember { mutableStateMapOf<String, MangaInstallState>() }
    val installedSourceByKey = remember(sources) { sources.associateBy { it.stableSourceKey() } }
    // Oto-güncelleme durumu
    var autoUpdateRunning by remember { mutableStateOf(false) }
    var autoUpdateResult by remember { mutableStateOf<String?>(null) }

    // Seçili dil filtresine göre repo'nun extensionlarını filtreler
    fun filteredExtensionsForBulk(plugins: List<com.kitsugi.animelist.data.remote.MangaExtensionInfo>): List<com.kitsugi.animelist.data.remote.MangaExtensionInfo> {
        return if (langFilter == "Tümü") plugins
        else plugins.filter { it.lang == langFilter }
    }

    val clipboardManager = LocalClipboardManager.current
    LaunchedEffect(Unit) {
        try {
            val text = clipboardManager.getText()?.text?.trim() ?: ""
            if (text.isNotBlank()) {
                val isRepoUrl = text.startsWith("http://") || text.startsWith("https://") || text.contains("raw.githubusercontent.com")
                if (isRepoUrl && newRepoUrl.isBlank()) {
                    newRepoUrl = text
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("MangaExtensionsTab", "Failed to auto-read clipboard", e)
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
                    MangaTabSection.REPOS to "🌐 Repolar",
                    MangaTabSection.INSTALLED to "✅ Kurulu (${sources.size})"
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

        if (activeSection == MangaTabSection.REPOS) {
            // Repo Ekle Kartı
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
                        "Manga Repo Ekle",
                        color = KitsugiColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    // Keiyoushi hızlı ekle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.08f))
                            .tvClickable(shape = RoundedCornerShape(10.dp)) {
                                val url = com.kitsugi.animelist.data.remote.MangaRepoClient.KEIYOUSHI_INDEX_URL
                                if (!repos.contains(url)) onAddRepo(url)
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
                            Text("⚡ Keiyoushi Resmi Reposu", color = accentColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("1354+ Mihon eklentisi · 78 Türkçe", color = KitsugiColors.TextMuted, fontSize = 10.sp)
                        }
                        Text(
                            if (repos.contains(com.kitsugi.animelist.data.remote.MangaRepoClient.KEIYOUSHI_INDEX_URL)) "✓ Eklendi" else "+ Ekle",
                            color = if (repos.contains(com.kitsugi.animelist.data.remote.MangaRepoClient.KEIYOUSHI_INDEX_URL)) KitsugiColors.TextMuted else accentColor,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                    }

                    // Kotatsu-Redo (Futon) built-in kaynakları kartı
                    val kotatsuPurple = Color(0xFF7B61FF)
                    var kotatsuActiveLangs by remember {
                        mutableStateOf(com.kitsugi.animelist.data.manga.KotatsuExtensionAdapter.getActiveLangs())
                    }
                    val kotatsuAllLangs = remember {
                        val fromCatalog = com.kitsugi.animelist.data.manga.KotatsuExtensionAdapter.getAllLangs()
                        // Katalog henüz yüklenmemişse yaygın diller göster (TR her zaman ilk)
                        if (fromCatalog.isEmpty())
                            listOf("tr", "en", "ko", "ja", "zh", "es", "pt", "fr", "de", "ru")
                        else fromCatalog.take(12)
                    }
                    val kotatsuTotal = remember {
                        val c = com.kitsugi.animelist.data.manga.KotatsuExtensionAdapter.getSourceCount()
                        if (c > 0) c else 1300
                    }
                    val kotatsuActiveCount = remember(kotatsuActiveLangs) {
                        com.kitsugi.animelist.data.manga.KotatsuExtensionAdapter.getActiveSourceCount()
                            .let { if (it > 0) it else kotatsuActiveLangs.size * 74 }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(kotatsuPurple.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Başlık satırı
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = kotatsuPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "🤖 Kotatsu-Redo (Futon)",
                                    color = kotatsuPurple,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "$kotatsuTotal+ kaynak · ${kotatsuActiveLangs.size} dil seçili · $kotatsuActiveCount aktif",
                                    color = KitsugiColors.TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            // Aktif sayacı badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(kotatsuPurple.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "Otomatik",
                                    color = kotatsuPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Dil seçici chip'ler
                        Text(
                            "Aktif diller — sadece seçilen dillerdeki kaynaklar çalışır:",
                            color = KitsugiColors.TextMuted,
                            fontSize = 9.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            kotatsuAllLangs.forEach { lang ->
                                val isActive = lang in kotatsuActiveLangs
                                val langEmoji = when (lang) {
                                    "tr" -> "🇹🇷"
                                    "en" -> "🇬🇧"
                                    "ko" -> "🇰🇷"
                                    "ja" -> "🇯🇵"
                                    "zh" -> "🇨🇳"
                                    "es" -> "🇪🇸"
                                    "pt" -> "🇧🇷"
                                    "fr" -> "🇫🇷"
                                    "de" -> "🇩🇪"
                                    "ru" -> "🇷🇺"
                                    "be" -> "🇧🇾"
                                    "bg" -> "🇧🇬"
                                    "bn" -> "🇧🇩"
                                    "ar" -> "🇸🇦"
                                    "id" -> "🇮🇩"
                                    "it" -> "🇮🇹"
                                    "pl" -> "🇵🇱"
                                    "uk" -> "🇺🇦"
                                    "vi" -> "🇻🇳"
                                    "th" -> "🇹🇭"
                                    "mn" -> "🇲🇳"
                                    "ms" -> "🇲🇾"
                                    "hi" -> "🇮🇳"
                                    "fa" -> "🇮🇷"
                                    "ro" -> "🇷🇴"
                                    "cs" -> "🇨🇿"
                                    "sk" -> "🇸🇰"
                                    "hu" -> "🇭🇺"
                                    "el" -> "🇬🇷"
                                    "nl" -> "🇳🇱"
                                    "sv" -> "🇸🇪"
                                    "da" -> "🇩🇰"
                                    "fi" -> "🇫🇮"
                                    "nb" -> "🇳🇴"
                                    "az" -> "🇦🇿"
                                    "kk" -> "🇰🇿"
                                    "uz" -> "🇺🇿"
                                    "ky" -> "🇰🇬"
                                    "ka" -> "🇬🇪"
                                    "hy" -> "🇦🇲"
                                    "my" -> "🇲🇲"
                                    "km" -> "🇰🇭"
                                    "lo" -> "🇱🇦"
                                    "si" -> "🇱🇰"
                                    "ne" -> "🇳🇵"
                                    "ur" -> "🇵🇰"
                                    "he" -> "🇮🇱"
                                    "sr" -> "🇷🇸"
                                    "hr" -> "🇭🇷"
                                    else -> lang.uppercase().map { c ->
                                        String(Character.toChars(0x1F1E6 + (c.code - 'A'.code)))
                                    }.joinToString("")
                                }
                                val chipBg = if (isActive)
                                    kotatsuPurple.copy(alpha = 0.22f)
                                else
                                    KitsugiColors.SurfaceStrong
                                val chipBorder = if (isActive) kotatsuPurple else KitsugiColors.Border.copy(alpha = 0.4f)
                                val chipTextColor = if (isActive) kotatsuPurple else KitsugiColors.TextSecondary

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(chipBg)
                                        .border(1.dp, chipBorder, RoundedCornerShape(50.dp))
                                        .tvClickable(shape = RoundedCornerShape(50.dp)) {
                                            com.kitsugi.animelist.data.manga.KotatsuExtensionAdapter.toggleLang(lang)
                                            kotatsuActiveLangs = com.kitsugi.animelist.data.manga.KotatsuExtensionAdapter.getActiveLangs()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = langEmoji,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = lang.uppercase(),
                                            color = chipTextColor,
                                            fontSize = 11.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Manuel URL girişi
                    OutlinedTextField(
                        value = newRepoUrl,
                        onValueChange = { newRepoUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://raw.githubusercontent.com/.../index.min.json", color = KitsugiColors.TextMuted, fontSize = 11.sp) },
                        label = { Text("Repo URL'si", color = KitsugiColors.TextSecondary, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = KitsugiColors.Border,
                            focusedTextColor = KitsugiColors.TextPrimary,
                            unfocusedTextColor = KitsugiColors.TextPrimary
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        trailingIcon = {
                            if (newRepoUrl.isNotBlank()) {
                                IconButton(onClick = {
                                    onAddRepo(newRepoUrl.trim())
                                    newRepoUrl = ""
                                }) {
                                    Icon(Icons.Rounded.Add, null, tint = accentColor, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    )
                }
            }

            // Repo Listesi
            if (repos.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.CloudOff, null, tint = KitsugiColors.TextMuted, modifier = Modifier.size(40.dp))
                        Text("Henüz repo eklenmedi", color = KitsugiColors.TextMuted, fontWeight = FontWeight.SemiBold)
                        Text("Keiyoushi'yi hızlıca eklemek için yukarıdaki butona tıklayın.", color = KitsugiColors.TextMuted.copy(alpha = 0.7f), fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }

            repos.distinct().forEachIndexed { repoIndex, repoUrl ->
                val repoPlugins = repoExtensions[repoUrl]
                val isLoading = repoLoadingState[repoUrl] == true
                val isExpanded = expandedRepo == repoUrl
                val repoName = if (repoUrl.contains("keiyoushi")) "Keiyoushi" else repoUrl.substringAfterLast("/").substringBefore(".")

                val totalCount = repoPlugins?.size ?: 0
                val installedCount = repoPlugins?.count { ext ->
                    sources.any { (it.pkgName.isNotEmpty() && it.pkgName == ext.pkg) || it.name == ext.name }
                } ?: 0

                // Repo'daki eklentilerin güncelleme durumu:
                // versionCode karşılaştırması — Mihon'daki hasUpdatedVer mantığı
                val hasUpdates = repoPlugins?.any { ext ->
                    val localCode = onGetInstalledVersionCode(ext.pkg)
                    localCode >= 0L && ext.versionCode.toLong() > localCode
                } ?: false

                item(key = "repo_idx_$repoIndex") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(KitsugiColors.SurfaceSoft)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvClickable(shape = RoundedCornerShape(14.dp)) {
                                    if (isExpanded) expandedRepo = null
                                    else {
                                        expandedRepo = repoUrl
                                        if (repoPlugins == null) onFetchRepo(repoUrl)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Rounded.Storage, null, tint = accentColor, modifier = Modifier.size(18.dp)) }
                            Column(modifier = Modifier.weight(1f)) {
                                FlatName(repoName)
                                Text(
                                    if (repoPlugins != null) {
                                        if (installedCount == totalCount && totalCount > 0) "✓ $installedCount/$totalCount Yüklü"
                                        else "$installedCount/$totalCount Yüklü"
                                    } else "Yüklenmedi",
                                    color = if (installedCount == totalCount && totalCount > 0) Color(0xFF4CAF50) else KitsugiColors.TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (installedCount == totalCount && totalCount > 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            if (isLoading) CircularProgressIndicator(color = accentColor, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            else if (repoPlugins == null) {
                                Icon(Icons.Rounded.Refresh, null, tint = accentColor, modifier = Modifier.size(18.dp).tvClickable(shape = RoundedCornerShape(999.dp)) { onFetchRepo(repoUrl) })
                            } else {
                                val isBulkInstalling = bulkInstallRepoUrl == repoUrl
                                if (hasUpdates && !isBulkInstalling) {
                                    IconButton(
                                        onClick = { onUpdateAllExtensions(repoUrl, repoPlugins) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Rounded.ArrowCircleUp, "Güncelle", tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                                    }
                                }
                                if (!isBulkInstalling && installedCount < totalCount) {
                                    // TR hızlı indirme butonu (Keiyoushi repo ise göster)
                                    val trCount = repoPlugins.count { it.lang == "tr" }
                                    val trInstalledCount = repoPlugins.count { ext ->
                                        ext.lang == "tr" && sources.any { it.name == ext.name }
                                    }
                                    if (trCount > 0 && trInstalledCount < trCount) {
                                        IconButton(
                                            onClick = {
                                                val trOnly = repoPlugins.filter { it.lang == "tr" }
                                                onInstallAllExtensions(repoUrl, trOnly)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFE53935).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("🇹🇷", fontSize = 14.sp)
                                            }
                                        }
                                    }
                                    // Aktif dil filtresine göre toplu indirme
                                    val bulkList = filteredExtensionsForBulk(repoPlugins)
                                    val bulkLabel = if (langFilter == "Tümü") "Hepsini İndir" else "${langFilter.uppercase()} İndir"
                                    IconButton(
                                        onClick = { onInstallAllExtensions(repoUrl, bulkList) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Rounded.DownloadForOffline, bulkLabel, tint = accentColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Icon(
                                if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                null, tint = KitsugiColors.TextMuted, modifier = Modifier.size(20.dp)
                            )
                            IconButton(onClick = { onDeleteRepo(repoUrl) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.DeleteForever, null, tint = KitsugiColors.AccentRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                            }
                        }

                        val isBulkInstalling = bulkInstallRepoUrl == repoUrl
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
                                    Text("⏳ Kuruluyor...", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("$bulkInstallDone / $bulkInstallTotal", color = KitsugiColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                if (bulkInstallCurrentName.isNotBlank()) {
                                    Spacer(Modifier.height(3.dp))
                                    Text(text = "📦 $bulkInstallCurrentName", color = KitsugiColors.TextSecondary, fontSize = 10.sp, maxLines = 1)
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

                        if (isExpanded && repoPlugins != null) {
                            HorizontalDivider(color = KitsugiColors.Border.copy(alpha = 0.3f))

                            val allLangs = remember(repoPlugins) {
                                val langs = repoPlugins.map { it.lang }.distinct().sorted()
                                // TR'yi her zaman öne al
                                val prioritized = mutableListOf("Tümü")
                                if (langs.contains("tr")) prioritized.add("tr")
                                prioritized.addAll(langs.filter { it != "tr" })
                                prioritized
                            }
                            // Dil filtreleri - yatay kaydırmalı Row (LazyRow LazyColumn içinde crash verir)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allLangs.forEach { l ->
                                    FilterChip(
                                        text = if (l == "all") "Global" else if (l == "Tümü") "Tümü" else l.uppercase(),
                                        selected = langFilter == l,
                                        onClick = { langFilter = l },
                                        accentColor = if (l == "tr") Color(0xFFE53935) else accentColor
                                    )
                                }
                            }

                            // Seçilen dile göre filtrele + kurulmamış olanları öne al
                            val filtered = remember(repoPlugins, langFilter) {
                                val base = if (langFilter == "Tümü") repoPlugins
                                else repoPlugins.filter { it.lang == langFilter }
                                // Kurulmamışları üste al
                                val notInstalled = base.filter { ext -> sources.none { (it.pkgName.isNotEmpty() && it.pkgName == ext.pkg) || it.name == ext.name } }
                                val installed = base.filter { ext -> sources.any { (it.pkgName.isNotEmpty() && it.pkgName == ext.pkg) || it.name == ext.name } }
                                notInstalled + installed
                            }

                            filtered.forEach { ext ->
                                val installState = installStates[ext.pkg] ?: MangaInstallState.IDLE
                                val isInstalled = sources.any { (it.pkgName.isNotEmpty() && it.pkgName == ext.pkg) || it.name == ext.name }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (!ext.iconUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ext.iconUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(accentColor.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                ext.lang.uppercase().take(2),
                                                color = accentColor, fontWeight = FontWeight.Bold, fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(ext.name, color = KitsugiColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (ext.isNsfw) Box(
                                                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(KitsugiColors.AccentRed.copy(alpha = 0.2f)).padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) { Text("18+", color = KitsugiColors.AccentRed, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                                        }
                                        Text("v${ext.version} · ${if (ext.lang == "all") "Global" else ext.lang.uppercase()}", color = KitsugiColors.TextMuted, fontSize = 10.sp)
                                    }
                                    when (installState) {
                                        MangaInstallState.LOADING -> CircularProgressIndicator(color = accentColor, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        MangaInstallState.SUCCESS -> Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                        MangaInstallState.FAILURE -> Icon(Icons.Rounded.Error, null, tint = KitsugiColors.AccentRed, modifier = Modifier.size(20.dp))
                                        MangaInstallState.IDLE -> {
                                            if (isInstalled) {
                                                // versionCode karşılaştırması (Mihon'daki hasUpdatedVer)
                                                val localCode = onGetInstalledVersionCode(ext.pkg)
                                                val hasUpdate = localCode >= 0L && ext.versionCode.toLong() > localCode
                                                if (hasUpdate) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color(0xFFFF9800).copy(alpha = 0.15f))
                                                            .tvClickable(shape = RoundedCornerShape(8.dp)) {
                                                                installStates[ext.pkg] = MangaInstallState.LOADING
                                                                onInstallApk(ext) { success ->
                                                                    installStates[ext.pkg] = if (success) MangaInstallState.SUCCESS else MangaInstallState.FAILURE
                                                                }
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) { Text("Güncelle", color = Color(0xFFFF9800), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                                } else {
                                                    Box(
                                                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(KitsugiColors.SurfaceStrong).padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) { Text("Kurulu", color = KitsugiColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(accentColor.copy(alpha = 0.15f))
                                                        .tvClickable(shape = RoundedCornerShape(8.dp)) {
                                                            installStates[ext.pkg] = MangaInstallState.LOADING
                                                            onInstallApk(ext) { success ->
                                                                installStates[ext.pkg] = if (success) MangaInstallState.SUCCESS else MangaInstallState.FAILURE
                                                            }
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) { Text("Kur", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Manuel APK yükleme
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(KitsugiColors.SurfaceSoft)
                        .tvClickable(shape = RoundedCornerShape(14.dp)) { filePickerLauncher.launch("*/*") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.FolderOpen, null, tint = accentColor, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dosyadan Yükle (.mex / .apk)", color = accentColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("Cihazınızdan bir eklenti dosyası seçin", color = KitsugiColors.TextMuted, fontSize = 10.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = KitsugiColors.TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        // KURULU KAYNAKLAR BÖLÜMÜ
        if (activeSection == MangaTabSection.INSTALLED) {
            // ── Otomatik Güncelleme Kartı ─────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(KitsugiColors.SurfaceSoft)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (autoUpdateRunning) {
                                CircularProgressIndicator(color = accentColor, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.CloudSync, null, tint = accentColor, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Keiyoushi'den Oto-Güncelle",
                                color = KitsugiColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                autoUpdateResult ?: "Günde 1 kez otomatik kontrol edilir",
                                color = if (autoUpdateResult?.startsWith("✅") == true) Color(0xFF4CAF50)
                                        else if (autoUpdateResult?.startsWith("❌") == true) androidx.compose.ui.graphics.Color(0xFFEF5350)
                                        else KitsugiColors.TextMuted,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = {
                                if (!autoUpdateRunning) {
                                    autoUpdateRunning = true
                                    autoUpdateResult = "🔄 Kontrol ediliyor..."
                                    onForceCheckUpdates { result ->
                                        autoUpdateRunning = false
                                        autoUpdateResult = result
                                    }
                                }
                            },
                            enabled = !autoUpdateRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (autoUpdateRunning) "Kontrol..." else "Şimdi Güncelle",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            // ── Sağlık Raporu Başlığı ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Kaynak sağlık raporu",
                        color = KitsugiColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { reportOpen = true }) {
                            Text("Rapor", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onOpenSourceHealthScreen) {
                            Text("Tam Ekran", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (sources.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.MenuBook, null, tint = KitsugiColors.TextMuted, modifier = Modifier.size(48.dp))
                        Text("Yüklü manga eklentisi yok", color = KitsugiColors.TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Repolar sekmesinden eklenti kurabilirsiniz.", color = KitsugiColors.TextMuted.copy(alpha = 0.6f), fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
            items(sources, key = { it.name + it.lang }) { source ->
                val health = onGetSourceHealthStatus(source)
                val isBusy = onIsSourceBusy(source)
                val domain = onGetConfiguredDomain(source)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(KitsugiColors.SurfaceSoft)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(source.lang.uppercase().take(2), color = accentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(source.name, color = KitsugiColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(onGetConfiguredBaseUrl(source).ifBlank { "Mihon APK" }, color = KitsugiColors.TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            HealthBadge(health = health, accentColor = accentColor)
                            if (!domain.isNullOrBlank()) {
                                TinyInfoChip(text = domain, tint = accentColor)
                            }
                        }
                    }
                    if (isBusy) {
                        CircularProgressIndicator(color = accentColor, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(KitsugiColors.SurfaceStrong).padding(horizontal = 6.dp, vertical = 3.dp)
                    ) { Text(source.lang, color = KitsugiColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                    IconButton(
                        onClick = {
                            val url = onGetConfiguredBaseUrl(source).ifBlank { source.baseUrl }
                            onVerifySource?.invoke(url, source.name)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.Language, "Doğrula", tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { manageTarget = source }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Tune, "Yönet", tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { deleteTarget = source }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.DeleteForever, "Sil", tint = KitsugiColors.AccentRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (reportOpen) {
        val filteredReport = remember(sourceStateReport, reportFilter, reportSort) {
            val filtered = sourceStateReport.filter {
                reportFilter == "ALL" || it.healthStatus.equals(reportFilter, ignoreCase = true)
            }
            when (reportSort) {
                "FAILURES" -> filtered.sortedWith(
                    compareByDescending<com.kitsugi.animelist.data.local.MangaSourceStateEntity> { it.failureCount }
                        .thenByDescending { it.updatedAt }
                )
                "LATENCY" -> filtered.sortedWith(
                    compareByDescending<com.kitsugi.animelist.data.local.MangaSourceStateEntity> { it.avgImageMs + it.avgSearchMs }
                        .thenByDescending { it.updatedAt }
                )
                else -> filtered.sortedByDescending { it.updatedAt }
            }
        }
        val healthyCount = remember(sourceStateReport) { sourceStateReport.count { it.healthStatus.equals("Healthy", ignoreCase = true) } }
        val degradedCount = remember(sourceStateReport) { sourceStateReport.count { it.healthStatus.equals("Degraded", ignoreCase = true) } }
        val brokenCount = remember(sourceStateReport) { sourceStateReport.count { it.healthStatus.equals("Broken", ignoreCase = true) } }
        val avgSuccessRate = remember(sourceStateReport) {
            if (sourceStateReport.isEmpty()) "—" else {
                val rates = sourceStateReport.map {
                    val total = it.successCount + it.failureCount
                    if (total <= 0) 0.0 else (it.successCount * 100.0 / total)
                }
                "${"%.0f".format(rates.average())}%"
            }
        }
        AlertDialog(
            onDismissRequest = { reportOpen = false },
            containerColor = KitsugiColors.Surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Manga Source Raporu", color = KitsugiColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(text = "Tümü", selected = reportFilter == "ALL", onClick = { reportFilter = "ALL" }, accentColor = accentColor)
                        FilterChip(text = "Healthy", selected = reportFilter == "Healthy", onClick = { reportFilter = "Healthy" }, accentColor = accentColor)
                        FilterChip(text = "Degraded", selected = reportFilter == "Degraded", onClick = { reportFilter = "Degraded" }, accentColor = accentColor)
                        FilterChip(text = "Broken", selected = reportFilter == "Broken", onClick = { reportFilter = "Broken" }, accentColor = accentColor)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(text = "Captcha", selected = reportFilter == "CaptchaRequired", onClick = { reportFilter = "CaptchaRequired" }, accentColor = accentColor)
                        FilterChip(text = "429", selected = reportFilter == "RateLimited", onClick = { reportFilter = "RateLimited" }, accentColor = accentColor)
                        FilterChip(text = "Unknown", selected = reportFilter == "Unknown", onClick = { reportFilter = "Unknown" }, accentColor = accentColor)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(text = "Güncel", selected = reportSort == "UPDATED", onClick = { reportSort = "UPDATED" }, accentColor = accentColor)
                        FilterChip(text = "Hata", selected = reportSort == "FAILURES", onClick = { reportSort = "FAILURES" }, accentColor = accentColor)
                        FilterChip(text = "Gecikme", selected = reportSort == "LATENCY", onClick = { reportSort = "LATENCY" }, accentColor = accentColor)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryStatCard(label = "Healthy", value = healthyCount.toString(), tint = Color(0xFF4CAF50), modifier = Modifier.weight(1f))
                        SummaryStatCard(label = "Degraded", value = degradedCount.toString(), tint = Color(0xFFFFB300), modifier = Modifier.weight(1f))
                        SummaryStatCard(label = "Broken", value = brokenCount.toString(), tint = KitsugiColors.AccentRed, modifier = Modifier.weight(1f))
                    }
                    SummaryStatCard(label = "Ortalama başarı", value = avgSuccessRate, tint = accentColor, modifier = Modifier.fillMaxWidth())

                    if (filteredReport.isEmpty()) {
                        Text("Bu filtre için source sağlık verisi bulunamadı.", color = KitsugiColors.TextSecondary)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.heightIn(max = 420.dp)
                        ) {
                            items(filteredReport, key = { it.sourceKey }) { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(KitsugiColors.SurfaceSoft)
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(item.sourceName, color = KitsugiColors.TextPrimary, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        TinyInfoChip(text = item.lang.uppercase(), tint = accentColor)
                                        TinyInfoChip(text = item.healthStatus, tint = accentColor)
                                    }
                                    SourceStatLine("Active domain", item.activeDomain ?: "—")
                                    SourceStatLine("Success / Failure", "${item.successCount} / ${item.failureCount}")
                                    SourceStatLine("Başarı oranı", formatSuccessRate(item.successCount, item.failureCount))
                                    TinyInfoChip(text = "Trend", tint = trendTint(item.successCount, item.failureCount))
                                    SourceStatLine("Son kontrol", formatTimestamp(item.lastCheckedAt))
                                    SourceStatLine("Search / Image", "${formatMs(item.avgSearchMs)} / ${formatMs(item.avgImageMs)}")
                                    if (!item.lastReason.isNullOrBlank()) {
                                        SourceStatLine("Son neden", item.lastReason)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onClearAllSourceDiagnostics) {
                        Text("Raporu temizle", color = KitsugiColors.AccentRed, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { reportOpen = false }) {
                        Text("Kapat", color = accentColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    manageTarget?.let { src ->
        val health = onGetSourceHealthStatus(src)
        val stats = onGetSourceRuntimeStats(src)
        val configuredDomain = onGetConfiguredDomain(src)
        val configuredBaseUrl = onGetConfiguredBaseUrl(src)
        val isBusy = onIsSourceBusy(src)
        var editedDomain by remember(src) { mutableStateOf(configuredDomain.orEmpty()) }
        var editedUserAgent by remember(src) { mutableStateOf(onGetSourceUserAgent(src).orEmpty()) }
        var slowdownEnabled by remember(src) { mutableStateOf(onGetSourceSlowdownEnabled(src)) }
        val domainError = editedDomain.isNotBlank() && !isLikelyValidDomain(editedDomain)
        val userAgentError = editedUserAgent.isNotBlank() && !isLikelyValidUserAgent(editedUserAgent)
        val canSave = !isBusy && !domainError && !userAgentError

        val isTv = LocalIsTv.current
        if (isTv) {
            TvDialog(
                onDismiss = { manageTarget = null },
                title = src.name,
                subtitle = "Motor: ${src.engineType.name} · Dil: ${src.lang.uppercase()}",
                width = 520.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SourceStatLine("Base URL", configuredBaseUrl)
                    SourceStatLine("Configured domain", configuredDomain ?: "—")
                    SourceStatLine("Search avg", formatMs(stats.avgSearchMs))
                    SourceStatLine("Popular avg", formatMs(stats.avgPopularMs))
                    SourceStatLine("Chapters avg", formatMs(stats.avgChapterMs))
                    SourceStatLine("Pages avg", formatMs(stats.avgPageMs))
                    SourceStatLine("Image avg", formatMs(stats.avgImageMs))
                    SourceStatLine("Başarı / Hata", "${stats.successCount} / ${stats.failureCount}")

                    OutlinedTextField(
                        value = editedDomain,
                        onValueChange = { editedDomain = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy,
                        isError = domainError,
                        label = { Text("Domain override", fontSize = 12.sp) },
                        placeholder = { Text("örn. manga-tr.com", fontSize = 11.sp) },
                        supportingText = {
                            if (domainError) {
                                Text("Geçerli bir alan adı girin. Örn: manga-tr.com", fontSize = 11.sp)
                            }
                        },
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = editedUserAgent,
                        onValueChange = { editedUserAgent = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy,
                        isError = userAgentError,
                        label = { Text("User-Agent override", fontSize = 12.sp) },
                        placeholder = { Text("Varsayılanı kullan", fontSize = 11.sp) },
                        supportingText = {
                            if (userAgentError) {
                                Text("User-Agent 4-512 karakter aralığında olmalı ve satır içermemeli.", fontSize = 11.sp)
                            }
                        },
                        singleLine = true,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Slowdown", color = KitsugiColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = slowdownEnabled,
                            enabled = !isBusy,
                            onCheckedChange = {
                                slowdownEnabled = it
                                onSetSourceSlowdownEnabled(src, it)
                            }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { onQuickCheckSource(src) }, enabled = !isBusy) { Text("Sağlık testi") }
                        FilledTonalButton(onClick = { onRefreshSourceMirror(src) }, enabled = !isBusy) { Text("Mirror bul") }
                        TextButton(onClick = {
                            editedDomain = ""
                            onClearSourceMirror(src)
                        }, enabled = !isBusy) { Text("Domain temizle") }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            editedUserAgent = ""
                            onSetSourceUserAgent(src, null)
                        }, enabled = !isBusy) {
                            Text("UA temizle", color = KitsugiColors.TextSecondary)
                        }
                        TextButton(onClick = { onResetSourceDiagnostics(src) }, enabled = !isBusy) {
                            Text("Sıfırla", color = KitsugiColors.AccentRed)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSetSourceDomain(src, editedDomain.ifBlank { null })
                                onSetSourceUserAgent(src, editedUserAgent.ifBlank { null })
                                manageTarget = null
                            },
                            enabled = canSave,
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Kaydet", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            AlertDialog(
                onDismissRequest = { manageTarget = null },
                containerColor = KitsugiColors.Surface,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(src.name, color = KitsugiColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            HealthBadge(health = health, accentColor = accentColor)
                            TinyInfoChip(text = src.lang.uppercase(), tint = accentColor)
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SourceStatLine("Base URL", configuredBaseUrl)
                        SourceStatLine("Configured domain", configuredDomain ?: "—")
                        SourceStatLine("Search avg", formatMs(stats.avgSearchMs))
                        SourceStatLine("Popular avg", formatMs(stats.avgPopularMs))
                        SourceStatLine("Chapters avg", formatMs(stats.avgChapterMs))
                        SourceStatLine("Pages avg", formatMs(stats.avgPageMs))
                        SourceStatLine("Image avg", formatMs(stats.avgImageMs))
                        SourceStatLine("Başarı / Hata", "${stats.successCount} / ${stats.failureCount}")

                        OutlinedTextField(
                            value = editedDomain,
                            onValueChange = { editedDomain = it },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isBusy,
                            isError = domainError,
                            label = { Text("Domain override", fontSize = 12.sp) },
                            placeholder = { Text("örn. manga-tr.com", fontSize = 11.sp) },
                            supportingText = {
                                if (domainError) {
                                    Text("Geçerli bir alan adı girin. Örn: manga-tr.com", fontSize = 11.sp)
                                }
                            },
                            singleLine = true,
                        )

                        OutlinedTextField(
                            value = editedUserAgent,
                            onValueChange = { editedUserAgent = it },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isBusy,
                            isError = userAgentError,
                            label = { Text("User-Agent override", fontSize = 12.sp) },
                            placeholder = { Text("Varsayılanı kullan", fontSize = 11.sp) },
                            supportingText = {
                                if (userAgentError) {
                                    Text("User-Agent 4-512 karakter aralığında olmalı ve satır içermemeli.", fontSize = 11.sp)
                                }
                            },
                            singleLine = true,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Slowdown", color = KitsugiColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = slowdownEnabled,
                                enabled = !isBusy,
                                onCheckedChange = {
                                    slowdownEnabled = it
                                    onSetSourceSlowdownEnabled(src, it)
                                }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { onQuickCheckSource(src) }, enabled = !isBusy) { Text("Sağlık testi") }
                            FilledTonalButton(onClick = { onRefreshSourceMirror(src) }, enabled = !isBusy) { Text("Mirror bul") }
                            TextButton(onClick = {
                                editedDomain = ""
                                onClearSourceMirror(src)
                            }, enabled = !isBusy) { Text("Domain temizle") }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onSetSourceDomain(src, editedDomain.ifBlank { null })
                        onSetSourceUserAgent(src, editedUserAgent.ifBlank { null })
                        manageTarget = null
                    }, enabled = canSave) {
                        Text("Kaydet", color = accentColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            editedUserAgent = ""
                            onSetSourceUserAgent(src, null)
                        }, enabled = !isBusy) {
                            Text("UA temizle", color = KitsugiColors.TextSecondary)
                        }
                        TextButton(onClick = { onResetSourceDiagnostics(src) }, enabled = !isBusy) {
                            Text("İstatistiği sıfırla", color = KitsugiColors.AccentRed)
                        }
                        TextButton(onClick = { manageTarget = null }) {
                            Text("Kapat", color = KitsugiColors.TextSecondary)
                        }
                    }
                }
            )
        }
    }

    deleteTarget?.let { src ->
        val isTv = LocalIsTv.current
        if (isTv) {
            TvDialog(
                onDismiss = { deleteTarget = null },
                title = "Eklenti Silinsin mi?",
                subtitle = "\"${src.name}\" eklentisi diskten silinecek ve kaldırılacak.",
                width = 440.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { deleteTarget = null }) {
                        Text("İptal", color = KitsugiColors.TextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onDeleteExtension(src); deleteTarget = null },
                        colors = ButtonDefaults.buttonColors(containerColor = KitsugiColors.AccentRed)
                    ) {
                        Text("Sil", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                containerColor = KitsugiColors.Surface,
                shape = RoundedCornerShape(20.dp),
                title = { Text("Eklenti Silinsin mi?", color = KitsugiColors.TextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("\"${src.name}\" eklentisi diskten silinecek ve kaldırılacak.", color = KitsugiColors.TextSecondary) },
                confirmButton = {
                    TextButton(onClick = { onDeleteExtension(src); deleteTarget = null }) {
                        Text("Sil", color = KitsugiColors.AccentRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) {
                        Text("İptal", color = KitsugiColors.TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun HealthBadge(
    health: com.kitsugi.animelist.data.manga.model.SourceHealthStatus,
    accentColor: Color,
) {
    val tint = when (health) {
        com.kitsugi.animelist.data.manga.model.SourceHealthStatus.Healthy -> Color(0xFF4CAF50)
        com.kitsugi.animelist.data.manga.model.SourceHealthStatus.Degraded -> Color(0xFFFFB300)
        com.kitsugi.animelist.data.manga.model.SourceHealthStatus.Broken -> KitsugiColors.AccentRed
        com.kitsugi.animelist.data.manga.model.SourceHealthStatus.CaptchaRequired -> Color(0xFFFF7043)
        com.kitsugi.animelist.data.manga.model.SourceHealthStatus.RateLimited -> Color(0xFF42A5F5)
        com.kitsugi.animelist.data.manga.model.SourceHealthStatus.Disabled -> KitsugiColors.TextMuted
        com.kitsugi.animelist.data.manga.model.SourceHealthStatus.Unknown -> accentColor.copy(alpha = 0.7f)
    }
    TinyInfoChip(text = health.name, tint = tint)
}

@Composable
private fun TinyInfoChip(text: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(text, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun SourceStatLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = KitsugiColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = KitsugiColors.TextPrimary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatMs(value: Long): String {
    return if (value <= 0L) "—" else "${value} ms"
}

private fun formatSuccessRate(success: Int, failure: Int): String {
    val total = success + failure
    if (total <= 0) return "—"
    val percent = (success * 100.0 / total)
    return "${"%.0f".format(percent)}%"
}

@Composable
private fun trendTint(success: Int, failure: Int): Color {
    val total = success + failure
    if (total <= 0) return KitsugiColors.TextMuted
    val rate = success.toDouble() / total.toDouble()
    return when {
        rate >= 0.85 -> Color(0xFF4CAF50)
        rate >= 0.55 -> Color(0xFFFFB300)
        else -> KitsugiColors.AccentRed
    }
}

private fun formatTimestamp(value: Long): String {
    if (value <= 0L) return "—"
    return try {
        java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(value))
    } catch (_: Exception) {
        value.toString()
    }
}

@Composable
private fun SummaryStatCard(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = KitsugiColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FlatName(name: String) {
    Text(name, color = KitsugiColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

private fun isLikelyValidDomain(domain: String): Boolean {
    if (domain.isBlank()) return false
    if (domain.contains("://")) return false
    if (domain.contains(" ")) return false
    val parts = domain.split('.')
    if (parts.size < 2) return false
    return parts.all { it.isNotEmpty() }
}

private fun isLikelyValidUserAgent(ua: String): Boolean {
    if (ua.length < 4 || ua.length > 512) return false
    if (ua.contains('\n') || ua.contains('\r')) return false
    return true
}
