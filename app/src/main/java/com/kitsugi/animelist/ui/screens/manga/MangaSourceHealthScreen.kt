package com.kitsugi.animelist.ui.screens.manga

import android.content.Intent
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.local.MangaSourceStateEntity
import com.kitsugi.animelist.data.manga.MangaSource
import com.kitsugi.animelist.data.manga.stableSourceKey
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MangaSourceHealthScreen(
    report: List<MangaSourceStateEntity>,
    installedSources: List<MangaSource>,
    onBack: () -> Unit,
    onExportReportFile: (String) -> Unit = {},
    onQuickCheckSource: (MangaSource) -> Unit = {},
    onRefreshSourceMirror: (MangaSource) -> Unit = {},
    onResetSourceDiagnostics: (MangaSource) -> Unit = {},
    onClearSourceMirror: (MangaSource) -> Unit = {},
    onClearAllSourceDiagnostics: () -> Unit = {},
    onClearAllSourceConfigs: () -> Unit = {},
    onIsSourceBusy: (MangaSource) -> Boolean = { false },
    onGetSourceFailureStreak: (MangaSource) -> Int = { 0 },
    onGetSourceCooldownUntil: (MangaSource) -> Long = { 0L },
    onGetMangaConfiguredDomain: (MangaSource) -> String = { "" },
    onGetMangaConfiguredBaseUrl: (MangaSource) -> String = { "" },
    onSetMangaSourceDomain: (MangaSource, String) -> Unit = { _, _ -> },
    repoExtensions: List<com.kitsugi.animelist.data.remote.MangaExtensionInfo> = emptyList(),
    onInstallExtension: (com.kitsugi.animelist.data.remote.MangaExtensionInfo) -> Unit = {},
    onInstallAllTr: () -> Unit = {},
    onAddCustomRepo: (String) -> Unit = {},
    onRefreshRepo: () -> Unit = {},
    kotatsuCommitSha: String = "",
    kotatsuSourceCount: Int = 0,
    keiyoushiTotal: Int = 0,
) {
    val accent = LocalKitsugiAccent.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var statusFilter by remember { mutableStateOf("ALL") }
    var sortMode by remember { mutableStateOf("UPDATED") }
    var rangeMode by remember { mutableStateOf("ALL") }
    var editingDomainSource by remember { mutableStateOf<MangaSource?>(null) }
    var domainInputText by remember { mutableStateOf("") }

    val sourceMap = remember(installedSources) { installedSources.associateBy { it.stableSourceKey() } }
    val now = System.currentTimeMillis()
    val filtered = remember(report, statusFilter, sortMode, rangeMode, now) {
        val rangeFiltered = report.filter { item ->
            val age = now - item.updatedAt
            when (rangeMode) {
                "24H" -> item.updatedAt > 0L && age <= 24L * 60L * 60L * 1000L
                "7D" -> item.updatedAt > 0L && age <= 7L * 24L * 60L * 60L * 1000L
                else -> true
            }
        }.filter { item ->
            statusFilter == "ALL" || item.healthStatus.equals(statusFilter, ignoreCase = true)
        }

        when (sortMode) {
            "FAILURES" -> rangeFiltered.sortedWith(compareByDescending<MangaSourceStateEntity> { it.failureCount }.thenByDescending { it.updatedAt })
            "LATENCY" -> rangeFiltered.sortedWith(compareByDescending<MangaSourceStateEntity> { it.avgImageMs + it.avgSearchMs }.thenByDescending { it.updatedAt })
            "SUCCESS" -> rangeFiltered.sortedWith(compareByDescending<MangaSourceStateEntity> { successRate(it) }.thenByDescending { it.updatedAt })
            else -> rangeFiltered.sortedByDescending { it.updatedAt }
        }
    }

    val healthyCount = filtered.count { it.healthStatus.equals("Healthy", ignoreCase = true) }
    val degradedCount = filtered.count { it.healthStatus.equals("Degraded", ignoreCase = true) }
    val brokenCount = filtered.count { it.healthStatus.equals("Broken", ignoreCase = true) }
    val avgSuccessRate = if (filtered.isEmpty()) "—" else "${"%.0f".format(filtered.map { successRate(it) }.average())}%"

    var selectedTab by remember { mutableIntStateOf(0) }
    var trFilterActive by remember { mutableStateOf(false) }
    var customRepoUrl by remember { mutableStateOf("") }
    var showRepoDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Repolar", "Kurulu (${installedSources.size})")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KitsugiColors.Background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = KitsugiColors.TextPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kaynakları yönet", color = KitsugiColors.TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("${installedSources.size} öge", color = KitsugiColors.TextSecondary, fontSize = 12.sp)
                }
                if (selectedTab == 0) {
                    // TR filtresi butonu
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (trFilterActive) accent.copy(0.25f) else KitsugiColors.Surface.copy(0.5f))
                            .tvClickable(shape = RoundedCornerShape(8.dp)) { trFilterActive = !trFilterActive }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (trFilterActive) "🇹🇷 Türkçe" else "🌐 Tümü",
                            color = if (trFilterActive) accent else KitsugiColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Tab Row ──────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = KitsugiColors.Surface.copy(0.3f),
                contentColor = accent,
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // ── Tab Content ───────────────────────────────────────────────────
            when (selectedTab) {
                0 -> ReposTab(
                    accent = accent,
                    repoExtensions = repoExtensions,
                    onInstallExtension = onInstallExtension,
                    onInstallAllTr = onInstallAllTr,
                    onAddCustomRepo = onAddCustomRepo,
                    onRefreshRepo = onRefreshRepo,
                    kotatsuCommitSha = kotatsuCommitSha,
                    kotatsuSourceCount = kotatsuSourceCount,
                    keiyoushiTotal = keiyoushiTotal,
                    trFilterActive = trFilterActive,
                )
                else -> HealthReportTab(
                    accent = accent,
                    context = context,
                    clipboardManager = clipboardManager,
                    filtered = filtered,
                    healthyCount = healthyCount,
                    degradedCount = degradedCount,
                    brokenCount = brokenCount,
                    avgSuccessRate = avgSuccessRate,
                    statusFilter = statusFilter,
                    sortMode = sortMode,
                    rangeMode = rangeMode,
                    onStatusFilter = { statusFilter = it },
                    onSortMode = { sortMode = it },
                    onRangeMode = { rangeMode = it },
                    sourceMap = sourceMap,
                    onExportReportFile = onExportReportFile,
                    onClearAllSourceDiagnostics = onClearAllSourceDiagnostics,
                    onClearAllSourceConfigs = onClearAllSourceConfigs,
                    onIsSourceBusy = onIsSourceBusy,
                    onGetSourceFailureStreak = onGetSourceFailureStreak,
                    onGetSourceCooldownUntil = onGetSourceCooldownUntil,
                    onQuickCheckSource = onQuickCheckSource,
                    onRefreshSourceMirror = onRefreshSourceMirror,
                    onClearSourceMirror = onClearSourceMirror,
                    onResetSourceDiagnostics = onResetSourceDiagnostics,
                    onGetMangaConfiguredDomain = onGetMangaConfiguredDomain,
                    onGetMangaConfiguredBaseUrl = onGetMangaConfiguredBaseUrl,
                    onSetMangaSourceDomain = onSetMangaSourceDomain,
                    editingDomainSource = editingDomainSource,
                    domainInputText = domainInputText,
                    onEditDomainSource = { editingDomainSource = it; domainInputText = onGetMangaConfiguredDomain(it) },
                    onDomainTextChange = { domainInputText = it },
                    onDomainDialogDismiss = { editingDomainSource = null },
                    onDomainSave = { onSetMangaSourceDomain(editingDomainSource!!, domainInputText.trim()); editingDomainSource = null },
                )
            }
        }
    }
}

// ── Repolar Sekmesi ─────────────────────────────────────────────────────────

@Composable
private fun ReposTab(
    accent: Color,
    repoExtensions: List<com.kitsugi.animelist.data.remote.MangaExtensionInfo>,
    onInstallExtension: (com.kitsugi.animelist.data.remote.MangaExtensionInfo) -> Unit,
    onInstallAllTr: () -> Unit,
    onAddCustomRepo: (String) -> Unit,
    onRefreshRepo: () -> Unit,
    kotatsuCommitSha: String,
    kotatsuSourceCount: Int,
    keiyoushiTotal: Int,
    trFilterActive: Boolean,
) {
    var customRepoUrl by remember { mutableStateOf("") }
    var showRepoDialog by remember { mutableStateOf(false) }
    val trExts = remember(repoExtensions, trFilterActive) {
        if (trFilterActive) repoExtensions.filter { it.lang == "tr" } else repoExtensions
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Manga Repo Ekle header ──
        item {
            Text("Manga Repo Ekle", color = KitsugiColors.TextMuted, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
        }

        // ── Keiyoushi Resmi Reposu kartı ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = KitsugiColors.SurfaceSoft),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Extension, null, tint = accent, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("⚡ Keiyoushi Resmi Reposu", color = KitsugiColors.TextPrimary,
                            fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${if (keiyoushiTotal > 0) keiyoushiTotal else "5000"}+ Mihon eklentisi (Türkçe dahil)",
                            color = KitsugiColors.TextSecondary, fontSize = 11.sp)
                    }
                    Text("✓ Eklendi", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Kotatsu-Redo Reposu kartı ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = KitsugiColors.SurfaceSoft),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF7B61FF).copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFF7B61FF), modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🤖 Kotatsu-Redo (Futon)", color = KitsugiColors.TextPrimary,
                            fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            buildString {
                                append("1300+ built-in kaynak")
                                if (kotatsuSourceCount > 0) append(" · $kotatsuSourceCount yüklendi")
                                if (kotatsuCommitSha.isNotBlank()) append(" · SHA: $kotatsuCommitSha")
                            },
                            color = KitsugiColors.TextSecondary, fontSize = 11.sp
                        )
                    }
                    if (kotatsuSourceCount > 0)
                        Text("✓ Aktif", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    else
                        Text("Otomatik", color = KitsugiColors.TextMuted, fontSize = 11.sp)
                }
            }
        }

        // ── Custom repo URL ──
        item {
            OutlinedTextField(
                value = customRepoUrl,
                onValueChange = { customRepoUrl = it },
                placeholder = { Text("Repo URL'si", color = KitsugiColors.TextMuted) },
                singleLine = true,
                trailingIcon = {
                    if (customRepoUrl.isNotBlank())
                        IconButton(onClick = { onAddCustomRepo(customRepoUrl); customRepoUrl = "" }) {
                            Icon(Icons.Rounded.Add, null, tint = accent)
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
        }

        // ── Dosyadan yükleme butonu ──
        item {
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Dosyadan Yükleme (.mex / .apk)")
            }
        }

        // ── Mevcut repolar header ──
        if (repoExtensions.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Keiyoushi  ·  ${trExts.size}/${repoExtensions.size} gösteriliyor",
                        color = KitsugiColors.TextMuted, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
                    )
                    // ── Toplu indirme butonu (TR filtresi aktifken) ──
                    if (trFilterActive && trExts.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent.copy(0.2f))
                                .tvClickable(shape = RoundedCornerShape(8.dp)) { onInstallAllTr() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.Download, null, tint = accent, modifier = Modifier.size(14.dp))
                                Text("Tümünü İndir (${trExts.size})", color = accent,
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onRefreshRepo, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.Refresh, null, tint = KitsugiColors.TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // ── Keiyoushi repo satırı ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = KitsugiColors.Surface.copy(0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                .background(KitsugiColors.SurfaceStrong),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.List, null, tint = KitsugiColors.TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Keiyoushi", color = KitsugiColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("0/${repoExtensions.size} Yüklendi",
                                color = KitsugiColors.TextMuted, fontSize = 11.sp)
                        }
                        Text("🇹🇷", fontSize = 16.sp)
                        Icon(Icons.Rounded.Download, null, tint = KitsugiColors.TextSecondary, modifier = Modifier.size(18.dp))
                        Icon(Icons.Rounded.ExpandMore, null, tint = KitsugiColors.TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ── Extension listesi ──
            items(trExts, key = { it.pkg }) { ext ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = KitsugiColors.SurfaceSoft.copy(0.7f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
                                .background(KitsugiColors.Surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(ext.name.take(1), color = accent, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ext.name, color = KitsugiColors.TextPrimary,
                                fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Manga,${if (ext.lang == "tr") "Türkçe" else ext.lang.uppercase()}",
                                color = KitsugiColors.TextMuted, fontSize = 10.sp)
                        }
                        IconButton(
                            onClick = { onInstallExtension(ext) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Download, null, tint = KitsugiColors.TextSecondary,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Sağlık Raporu Sekmesi ──────────────────────────────────────────────────────

@Composable
private fun HealthReportTab(
    accent: Color,
    context: android.content.Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    filtered: List<MangaSourceStateEntity>,
    healthyCount: Int, degradedCount: Int, brokenCount: Int, avgSuccessRate: String,
    statusFilter: String, sortMode: String, rangeMode: String,
    onStatusFilter: (String) -> Unit, onSortMode: (String) -> Unit, onRangeMode: (String) -> Unit,
    sourceMap: Map<String, MangaSource>,
    onExportReportFile: (String) -> Unit,
    onClearAllSourceDiagnostics: () -> Unit, onClearAllSourceConfigs: () -> Unit,
    onIsSourceBusy: (MangaSource) -> Boolean,
    onGetSourceFailureStreak: (MangaSource) -> Int,
    onGetSourceCooldownUntil: (MangaSource) -> Long,
    onQuickCheckSource: (MangaSource) -> Unit,
    onRefreshSourceMirror: (MangaSource) -> Unit,
    onClearSourceMirror: (MangaSource) -> Unit,
    onResetSourceDiagnostics: (MangaSource) -> Unit,
    onGetMangaConfiguredDomain: (MangaSource) -> String,
    onGetMangaConfiguredBaseUrl: (MangaSource) -> String,
    onSetMangaSourceDomain: (MangaSource, String) -> Unit,
    editingDomainSource: MangaSource?,
    domainInputText: String,
    onEditDomainSource: (MangaSource) -> Unit,
    onDomainTextChange: (String) -> Unit,
    onDomainDialogDismiss: () -> Unit,
    onDomainSave: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { FilterChip(selected = false, onClick = { clipboardManager.setText(AnnotatedString(buildReportExport(filtered))) }, label = { Text("Raporu kopyala") }) }
        item { FilterChip(selected = false, onClick = { onExportReportFile(buildReportExport(filtered)) }, label = { Text("Dosyaya kaydet") }) }
        item { FilterChip(selected = false, onClick = {
            val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, buildReportExport(filtered)) }
            context.startActivity(Intent.createChooser(i, "Paylaş"))
        }, label = { Text("Paylaş") }) }
        item { FilterChip(selected = false, onClick = onClearAllSourceDiagnostics, label = { Text("Raporu temizle") }) }
        item { FilterChip(selected = false, onClick = onClearAllSourceConfigs, label = { Text("Config temizle") }) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Healthy", healthyCount.toString(), Color(0xFF4CAF50), Modifier.weight(1f))
                    SummaryCard("Degraded", degradedCount.toString(), Color(0xFFFFB300), Modifier.weight(1f))
                    SummaryCard("Broken", brokenCount.toString(), KitsugiColors.AccentRed, Modifier.weight(1f))
                }
                SummaryCard("Ortalama başarı", avgSuccessRate, accent, Modifier.fillMaxWidth())
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterRow(listOf("ALL" to "Tümü","Healthy" to "Healthy","Degraded" to "Degraded","Broken" to "Broken","CaptchaRequired" to "Captcha","RateLimited" to "429"), statusFilter, accent, onStatusFilter)
                FilterRow(listOf("UPDATED" to "Güncel","FAILURES" to "Hata","LATENCY" to "Gecikme","SUCCESS" to "Başarı"), sortMode, accent, onSortMode)
                FilterRow(listOf("ALL" to "Tüm zaman","24H" to "24s","7D" to "7g"), rangeMode, accent, onRangeMode)
            }
        }
        if (filtered.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), Alignment.Center) { Text("Bu filtre için rapor verisi yok.", color = KitsugiColors.TextSecondary) } }
        } else {
            items(filtered, key = { it.sourceKey }) { item ->
                val source = sourceMap[item.sourceKey]
                val isBusy = source?.let(onIsSourceBusy) == true
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = KitsugiColors.SurfaceSoft), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.sourceName, color = KitsugiColors.TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            StatusChip(item.healthStatus, trendTint(successRate(item)))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            SmallChip(item.lang.uppercase(), accent)
                            if (!item.activeDomain.isNullOrBlank()) SmallChip(item.activeDomain, accent.copy(0.85f))
                        }
                        StatLine("Success / Failure", "${item.successCount} / ${item.failureCount}")
                        StatLine("Başarı oranı", "${"%.0f".format(successRate(item))}%")
                        StatLine("Son başarı / Son hata", "${formatTimestamp(item.lastSuccessAt)} / ${formatTimestamp(item.lastFailureAt)}")
                        StatLine("Son kontrol", formatTimestamp(item.lastCheckedAt))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniMetricCard("Search", formatMs(item.avgSearchMs), Modifier.weight(1f))
                            MiniMetricCard("Popular", formatMs(item.avgPopularMs), Modifier.weight(1f))
                            MiniMetricCard("Image", formatMs(item.avgImageMs), Modifier.weight(1f))
                        }
                        if (!item.lastReason.isNullOrBlank()) StatLine("Son hata", item.lastReason)
                        if (source != null) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                item { FilterChip(selected = false, enabled = !isBusy, onClick = { onQuickCheckSource(source) }, label = { Text(if (isBusy) "Çalışıyor" else "Sağlık testi") }) }
                                item { FilterChip(selected = false, enabled = !isBusy, onClick = { onRefreshSourceMirror(source) }, label = { Text("Mirror bul") }) }
                                item { FilterChip(selected = false, enabled = !isBusy, onClick = { onEditDomainSource(source) }, label = { Text("Domain Ayarla") }) }
                                item { FilterChip(selected = false, enabled = !isBusy, onClick = { onClearSourceMirror(source) }, label = { Text("Domain temizle") }) }
                                item { FilterChip(selected = false, enabled = !isBusy, onClick = { onResetSourceDiagnostics(source) }, label = { Text("İstatistik sıfırla") }) }
                            }
                        }
                    }
                }
            }
        }
    }

    editingDomainSource?.let { source ->
        AlertDialog(
            onDismissRequest = onDomainDialogDismiss,
            title = { Text("Domain Ayarla: ${source.name}", color = KitsugiColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currentBaseUrl = onGetMangaConfiguredBaseUrl(source)
                    if (currentBaseUrl.isNotEmpty()) Text("Varsayılan: $currentBaseUrl", color = KitsugiColors.TextSecondary, fontSize = 12.sp)
                    OutlinedTextField(value = domainInputText, onValueChange = onDomainTextChange,
                        placeholder = { Text("örn. mangagezgini.online") }, label = { Text("Yeni Domain") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = onDomainSave) { Text("Kaydet", color = accent) } },
            dismissButton = { TextButton(onClick = onDomainDialogDismiss) { Text("İptal", color = KitsugiColors.TextSecondary) } },
            containerColor = Color(0xFF141424)
        )
    }
}


@Composable
private fun FilterRow(
    options: List<Pair<String, String>>,
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = selected == key,
                onClick = { onSelect(key) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = KitsugiColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = tint, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusChip(text: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallChip(text: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = tint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = KitsugiColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = KitsugiColors.TextPrimary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MiniMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(KitsugiColors.Surface.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = KitsugiColors.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = KitsugiColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun successRate(item: MangaSourceStateEntity): Double {
    val total = item.successCount + item.failureCount
    if (total <= 0) return 0.0
    return item.successCount * 100.0 / total.toDouble()
}

private fun trendTint(rate: Double): Color = when {
    rate >= 85.0 -> Color(0xFF4CAF50)
    rate >= 55.0 -> Color(0xFFFFB300)
    else -> KitsugiColors.AccentRed
}

private fun formatMs(value: Long): String = if (value <= 0L) "—" else "${value} ms"

private fun formatTimestamp(value: Long): String {
    if (value <= 0L) return "—"
    return try {
        SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(value))
    } catch (_: Exception) {
        value.toString()
    }
}

private fun formatRelativeCooldown(value: Long): String {
    if (value <= 0L) return "bitti"
    val minutes = value / 60000L
    if (minutes < 60) return "${minutes} dk"
    val hours = minutes / 60
    val rem = minutes % 60
    return if (rem == 0L) "${hours} sa" else "${hours} sa ${rem} dk"
}

private fun buildReportExport(items: List<MangaSourceStateEntity>): String {
    return buildString {
        appendLine("# Manga Source Health Report")
        appendLine("Count: ${items.size}")
        appendLine()
        items.forEach { item ->
            appendLine("- ${item.sourceName} [${item.lang}] :: ${item.healthStatus}")
            appendLine("  success/failure: ${item.successCount}/${item.failureCount}")
            appendLine("  last checked: ${formatTimestamp(item.lastCheckedAt)}")
            appendLine("  search/image: ${formatMs(item.avgSearchMs)} / ${formatMs(item.avgImageMs)}")
            if (!item.activeDomain.isNullOrBlank()) appendLine("  domain: ${item.activeDomain}")
            if (!item.lastErrorType.isNullOrBlank()) appendLine("  error type: ${item.lastErrorType}")
            if (!item.lastReason.isNullOrBlank()) appendLine("  last reason: ${item.lastReason}")
            appendLine()
        }
    }
}
