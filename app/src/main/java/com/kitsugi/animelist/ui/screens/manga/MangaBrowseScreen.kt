package com.kitsugi.animelist.ui.screens.manga

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.manga.MangaDetails
import com.kitsugi.animelist.data.manga.MangaSource
import com.kitsugi.animelist.data.manga.MangaSourceRepository
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.kitsugi.animelist.ui.app.AddonViewModel
import com.kitsugi.animelist.ui.app.MangaViewModel
import com.kitsugi.animelist.ui.components.KitsugiAddonsSettingsDialog

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun MangaBrowseScreen(
    repository: MangaSourceRepository,
    initialQuery: String? = null,
    vm: MangaBrowseViewModel = viewModel(factory = MangaBrowseViewModel.Factory(repository)),
    onMangaClick: (MangaSource, MangaDetails) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    val ui by vm.ui.collectAsState()

    val addonViewModel: AddonViewModel = viewModel()
    val mangaViewModel: MangaViewModel = viewModel()

    val addonsList by addonViewModel.addonsList.collectAsState(initial = emptyList())
    val reposList by addonViewModel.reposList.collectAsState(initial = emptyList())
    val csPluginsList by addonViewModel.csPluginsList.collectAsState(initial = emptyList())

    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(reposList) {
        addonViewModel.syncRepos(reposList)
    }

    LaunchedEffect(addonViewModel) {
        addonViewModel.onShowMessage = { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(mangaViewModel) {
        mangaViewModel.onShowMessage = { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(initialQuery) {
        if (vm.lastInitialQuery != initialQuery) {
            vm.lastInitialQuery = initialQuery
            if (!initialQuery.isNullOrBlank()) {
                vm.search(initialQuery)
            } else {
                vm.search("")
            }
        }
    }
    val accentColor = LocalKitsugiAccent.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val cols = if (isLandscape) 4 else 2

    // Snapshot delegated props → allow smart cast
    val query          = ui.searchQuery
    val states         = ui.sourceStates
    val selFilter      = ui.selectedSourceFilter
    val popular        = ui.popularMangas
    val hasNext        = ui.hasNextPage

    val mergedMangas = remember(states, selFilter, query, popular) {
        val raw = if (query.isNotBlank()) {
            val src = selFilter
            if (src != null) {
                // Tek kaynak filtresi seçiliyse: o kaynağın sonuçlarını yine de
                // benzerlik skoruna göre sırala (en alakalı en üstte).
                (states.firstOrNull { it.source.name == src.name }?.mangas ?: emptyList())
                    .sortedByDescending {
                        com.kitsugi.animelist.data.manga.MangaTitleMatcher.getSimilarityScore(query, it.title)
                    }
            } else {
                // ── Mihon "global search" sıralama mantığı ──────────────────────
                // Tüm kaynaklardan gelen sonuçları tek bir havuzda birleştirip
                // benzerlik skoruna göre genel (global) sıralama yapıyoruz.
                val minScore = if (query.trim().length <= 3) 0.15 else 0.30

                states.flatMap { st ->
                    st.mangas.map { manga ->
                        val score = com.kitsugi.animelist.data.manga.MangaTitleMatcher.getSimilarityScore(query, manga.title)
                        Triple(manga, score, st.source)
                    }
                }
                .filter { (_, score, _) -> score >= minScore }
                .sortedWith(
                    compareByDescending<Triple<MangaDetails, Double, MangaSource>> { it.second } // Benzerlik skoru
                        .thenByDescending { it.third.let { src -> repository.getSourcePriority(src) } } // Kaynak önceliği
                        .thenBy { it.first.title.lowercase() } // Alfabetik
                )
                .map { it.first }
            }
        } else popular

        raw.distinctBy { "${it.source}_${it.url}" }
    }

    val backdropUrl = if (query.isNotBlank()) mergedMangas.firstOrNull()?.thumbnailUrl
                     else popular.firstOrNull()?.thumbnailUrl

    Box(Modifier.fillMaxSize().background(KitsugiColors.Background)) {

        // ── Backdrop ──
        if (!backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = backdropUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.18f }
            )
        }
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(KitsugiColors.Background.copy(alpha = 0.55f), KitsugiColors.Background))
        ))

        Column(Modifier.fillMaxSize().systemBarsPadding()) {

            // ── Top bar ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    colors  = IconButtonDefaults.iconButtonColors(containerColor = KitsugiColors.SurfaceStrong.copy(0.5f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri", tint = KitsugiColors.TextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Manga Keşfet", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.TravelExplore, null, tint = accentColor, modifier = Modifier.padding(end = 8.dp).size(22.dp))
            }

            // ── Info card (CS3 portrait style) ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(KitsugiColors.Surface.copy(0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // mini poster
                Box(
                    Modifier.size(width = 52.dp, height = 76.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(KitsugiColors.SurfaceStrong)
                ) {
                    if (!backdropUrl.isNullOrBlank()) {
                        AsyncImage(model = backdropUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Book, null, tint = KitsugiColors.TextMuted, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (query.isNotBlank()) "Arama: \"$query\"" else selFilter?.name ?: "Manga Keşfet",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (query.isNotBlank())
                            "${mergedMangas.size} sonuç · ${states.size} kaynak"
                        else
                            "${popular.size} manga · ${selFilter?.lang?.uppercase() ?: ""}",
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Search bar ──
            OutlinedTextField(
                value = query,
                onValueChange = { vm.search(it) },
                placeholder   = { Text("Manga ara...", color = KitsugiColors.TextMuted) },
                leadingIcon   = { Icon(Icons.Rounded.Search, null, tint = KitsugiColors.TextMuted) },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = accentColor,
                    unfocusedBorderColor = KitsugiColors.Border,
                    cursorColor          = accentColor,
                    focusedTextColor     = KitsugiColors.TextPrimary,
                    unfocusedTextColor   = KitsugiColors.TextPrimary,
                    focusedContainerColor   = KitsugiColors.Surface.copy(0.5f),
                    unfocusedContainerColor = KitsugiColors.Surface.copy(0.3f)
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(10.dp))

            // ── Source chips — ALWAYS premium capsule style ──
            // Sadece kaynaklar gerçekten boşsa VE arama yüklenip bitmişse göster.
            // Arama devam ediyorken (sourceStates doluyken) bu mesajı gösterme.
            val sourcesEmpty = ui.sources.isEmpty() && states.isEmpty() && !ui.isLoadingPopular
            if (sourcesEmpty) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    EmptySourcesHint(onOpenSettings = { showSettingsDialog = true })
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    if (query.isNotBlank()) {
                        // "Tümü" chip
                        val totalCount  = states.sumOf { it.mangas.size }
                        val anyLoading  = states.any { it.isLoading }
                        item {
                            MangaChip(
                                label     = "Tümü",
                                count     = totalCount,
                                isLoading = anyLoading,
                                error     = null,
                                isSelected = selFilter == null,
                                accent    = accentColor,
                                isAll     = true,
                                onClick   = { vm.selectSourceFilter(null) }
                            )
                        }
                        items(states, key = { it.source.name }) { st ->
                            MangaChip(
                                label     = st.source.name,
                                count     = st.mangas.size,
                                isLoading = st.isLoading,
                                error     = st.error,
                                isSelected = selFilter?.name == st.source.name,
                                accent    = accentColor,
                                isAll     = false,
                                onClick   = {
                                    vm.selectSourceFilter(if (selFilter?.name == st.source.name) null else st.source)
                                }
                            )
                        }
                    } else {
                        // Popular mode → still premium capsule chips
                        items(ui.sources) { src ->
                            MangaChip(
                                label      = "${src.name}  [${src.lang.uppercase()}]",
                                count      = if (selFilter?.name == src.name) popular.size else 0,
                                isLoading  = selFilter?.name == src.name && ui.isLoadingPopular,
                                error      = null,
                                isSelected = selFilter?.name == src.name,
                                accent     = accentColor,
                                isAll      = false,
                                onClick    = { vm.selectSourceFilter(src) }
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp), color = KitsugiColors.SurfaceStrong.copy(0.5f))

                // ── Grid ──
                Box(Modifier.weight(1f)) {
                    val loading = if (query.isNotBlank()) states.any { it.isLoading } else ui.isLoadingPopular
                    when {
                        loading && mergedMangas.isEmpty() -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(cols),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement   = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(8) {
                                    MangaCardShimmer()
                                }
                            }
                        }
                        !loading && mergedMangas.isEmpty() ->
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                ) {
                                    Icon(Icons.Rounded.SearchOff, null, tint = KitsugiColors.TextMuted, modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Sonuç bulunamadı", color = KitsugiColors.TextSecondary)
                                }
                            }
                        else -> LazyVerticalGrid(
                            columns = GridCells.Fixed(cols),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement   = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1) ÖNCE başarıyla veri gelen kaynakların mangaları (en üstte).
                            items(mergedMangas, key = { "${it.source}_${it.url}" }) { manga ->
                                MangaCard(manga, accentColor) {
                                    // Kaynağı önce sourceStates'ten, sonra ui.sources'tan ara.
                                    // Arama modunda ui.sources boş olabilir; states her zaman dolu.
                                    val src = states.firstOrNull { it.source.name == manga.source }?.source
                                        ?: ui.sources.firstOrNull { it.name == manga.source }
                                        ?: selFilter
                                        ?: states.firstOrNull()?.source
                                        ?: ui.sources.firstOrNull()
                                    if (src != null) onMangaClick(src, manga)
                                }
                            }
                            // 2) SONRA hâlâ yüklenen kaynakların iskelet (placeholder) kartları — EN ALTTA.
                            //    Böylece başarıyla gelen sonuçlar her zaman üstte kalır, yüklenenler onları aşağı itmez.
                            if (query.isNotBlank()) {
                                val loadingStates = states.filter { it.isLoading && (selFilter == null || selFilter.name == it.source.name) }
                                items(loadingStates, key = { "sk_${it.source.name}" }) {
                                    SkeletonCard(it.source.name, accentColor)
                                }
                            }
                            val hasNextPageToShow = if (query.isBlank()) hasNext
                                else (selFilter != null && (states.firstOrNull { it.source.name == selFilter.name }?.hasNextPage == true))

                            if (query.isBlank() || selFilter != null) {
                                item(span = { GridItemSpan(cols) }) {
                                    LaunchedEffect(mergedMangas.size) { vm.loadNextPage() }
                                    androidx.compose.animation.AnimatedVisibility(visible = hasNextPageToShow) {
                                        Box(Modifier.fillMaxWidth().height(64.dp), Alignment.Center) {
                                            CircularProgressIndicator(color = accentColor, modifier = Modifier.size(30.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        KitsugiAddonsSettingsDialog(
            addons = addonsList,
            initialDebridToken = addonViewModel.debridToken,
            repos = reposList,
            repoPlugins = addonViewModel.repoPluginsState,
            repoLoadingState = addonViewModel.repoLoadingState,
            csPlugins = csPluginsList,
            initialTab = 2,
            onAddAddon = { addonViewModel.addAddon(it) },
            onToggleAddon = { addon, enabled -> addonViewModel.toggleAddon(addon, enabled) },
            onDeleteAddon = { addonViewModel.deleteAddon(it) },
            onSaveDebridToken = { addonViewModel.saveDebridToken(it) },
            onAddRepo = { addonViewModel.addRepo(it) },
            onDeleteRepo = { addonViewModel.deleteRepo(it) },
            onFetchRepoPlugins = { addonViewModel.fetchRepoPlugins(it) },
            onInstallPlugin = { plugin, onResult -> addonViewModel.installPlugin(plugin, onResult) },
            onInstallAllPlugins = { repoUrl, repoName, plugins ->
                addonViewModel.installAllPlugins(repoUrl, repoName, plugins, addonsList, csPluginsList)
            },
            onUpdateAllPlugins = { repoUrl, repoName, plugins ->
                addonViewModel.updateAllPlugins(repoUrl, repoName, plugins, csPluginsList)
            },
            bulkInstallRepoUrl = addonViewModel.bulkInstallRepoUrl,
            bulkInstallRepoName = addonViewModel.bulkInstallRepoName,
            bulkInstallDone = addonViewModel.bulkInstallDone,
            bulkInstallTotal = addonViewModel.bulkInstallTotal,
            bulkInstallCurrentName = addonViewModel.bulkInstallCurrentName,
            bulkInstallResultMessage = addonViewModel.bulkInstallResultMessage,
            onClearBulkInstallResult = { addonViewModel.clearBulkInstallResult() },
            onToggleCsPlugin = { plugin, enabled -> addonViewModel.toggleCsPlugin(plugin, enabled) },
            onUninstallCsPlugin = { addonViewModel.uninstallCsPlugin(it) },
            mangaSources = mangaViewModel.mangaSources,
            onInstallMangaExtension = { mangaViewModel.installMangaExtension(it) },
            onDeleteMangaExtension = { mangaViewModel.deleteMangaExtension(it) },
            mangaRepos = mangaViewModel.mangaReposState,
            mangaRepoExtensions = mangaViewModel.mangaRepoExtensionsState,
            mangaRepoLoadingState = mangaViewModel.mangaRepoLoadingState,
            onAddMangaRepo = { mangaViewModel.addMangaRepo(it) },
            onDeleteMangaRepo = { mangaViewModel.deleteMangaRepo(it) },
            onFetchMangaRepo = { mangaViewModel.fetchMangaRepo(it) },
            onInstallMangaApk = { extension, onResult -> mangaViewModel.installMangaApk(extension, onResult) },
            onInstallAllMangaExtensions = { repoUrl, extensions -> mangaViewModel.installAllMangaExtensions(repoUrl, extensions) },
            onUpdateAllMangaExtensions = { repoUrl, extensions -> mangaViewModel.updateAllMangaExtensions(repoUrl, extensions) },
            mangaBulkInstallRepoUrl = mangaViewModel.mangaBulkInstallRepoUrl,
            mangaBulkInstallDone = mangaViewModel.mangaBulkInstallDone,
            mangaBulkInstallTotal = mangaViewModel.mangaBulkInstallTotal,
            mangaBulkInstallCurrentName = mangaViewModel.mangaBulkInstallCurrentName,
            onGetInstalledMangaVersionCode = { mangaViewModel.getInstalledVersionCode(it) },
            onGetInstalledMangaVersion = { mangaViewModel.getInstalledMangaExtensionVersion(it) },
            mangaSourceStateReport = mangaViewModel.mangaSourceStateReport,
            onGetMangaSourceHealthStatus = { mangaViewModel.getSourceHealthStatus(it) },
            onGetMangaSourceRuntimeStats = { mangaViewModel.getSourceRuntimeStats(it) },
            onGetMangaConfiguredDomain = { mangaViewModel.getConfiguredSourceDomain(it) },
            onGetMangaConfiguredBaseUrl = { mangaViewModel.getConfiguredSourceBaseUrl(it) },
            onGetMangaSourceUserAgent = { mangaViewModel.getSourceUserAgentOverride(it) },
            onGetMangaSourceSlowdownEnabled = { mangaViewModel.getSourceSlowdownEnabled(it) },
            onSetMangaSourceUserAgent = { source, ua -> mangaViewModel.setSourceUserAgentOverride(source, ua) },
            onSetMangaSourceSlowdownEnabled = { source, enabled -> mangaViewModel.setSourceSlowdownEnabled(source, enabled) },
            onSetMangaSourceDomain = { source, domain -> mangaViewModel.setSourceDomainOverride(source, domain) },
            onResetMangaSourceDiagnostics = { mangaViewModel.resetSourceDiagnostics(it) },
            onClearAllMangaSourceDiagnostics = { mangaViewModel.clearAllSourceDiagnostics() },
            onIsMangaSourceBusy = { mangaViewModel.isSourceBusy(it) },
            onQuickCheckMangaSource = { mangaViewModel.quickCheckSource(it) },
            onRefreshMangaSourceMirror = { mangaViewModel.refreshSourceMirror(it) },
            onClearMangaSourceMirror = { mangaViewModel.clearSourceMirror(it) },
            onForceCheckMangaUpdates = { mangaViewModel.forceCheckMangaUpdates(it) },
            untrustedRepoToConfirm = mangaViewModel.untrustedRepoToConfirm,
            untrustedSignatureToConfirm = mangaViewModel.untrustedSignatureToConfirm,
            onConfirmUntrustedRepo = { mangaViewModel.addMangaRepo(it, force = true) },
            onDismissUntrustedRepo = { mangaViewModel.clearUntrustedRepoConfirm() },
            onConfirmUntrustedSignature = { ext, hash -> mangaViewModel.installMangaApk(ext, force = true) },
            onDismissUntrustedSignature = { mangaViewModel.clearUntrustedSignatureConfirm() },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

// ─── Premium capsule chip ─────────────────────────────────────────────────────

@Composable
fun MangaChip(
    label: String, count: Int, isLoading: Boolean, error: String?,
    isSelected: Boolean, accent: Color, isAll: Boolean, onClick: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "p")
    val pulse by inf.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "a"
    )
    val bg = when {
        isSelected  -> accent.copy(alpha = 0.35f)
        isLoading   -> accent.copy(alpha = pulse * 0.22f)
        count > 0   -> accent.copy(alpha = 0.12f)
        error != null -> Color.Red.copy(alpha = 0.15f)
        else        -> Color.White.copy(alpha = 0.07f)
    }
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(bg).tvClickable(shape = RoundedCornerShape(999.dp)) { onClick() }.padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when {
            isLoading -> CircularProgressIndicator(Modifier.size(10.dp), color = accent, strokeWidth = 1.5.dp)
            isAll     -> Icon(Icons.Rounded.Extension, null, tint = if (isSelected) accent else KitsugiColors.TextSecondary, modifier = Modifier.size(13.dp))
            count > 0 -> Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
            error != null -> Box(Modifier.size(7.dp).clip(CircleShape).background(Color.Red.copy(0.7f)))
            else      -> Box(Modifier.size(7.dp).clip(CircleShape).background(KitsugiColors.TextMuted.copy(0.5f)))
        }
        Text(
            buildString {
                append(label)
                if (count > 0 && !isAll) append("  $count")
            },
            color = if (isSelected || count > 0) KitsugiColors.TextPrimary else KitsugiColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected || count > 0) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ─── Skeleton card ────────────────────────────────────────────────────────────

@Composable
private fun SkeletonCard(sourceName: String, accent: Color) {
    val inf = rememberInfiniteTransition(label = "sk")
    val sh by inf.animateFloat(0.25f, 0.07f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "sh")
    Card(
        Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = KitsugiColors.SurfaceSoft.copy(0.6f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(sh)))
            Box(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.85f)))).padding(8.dp)
            ) {
                Column {
                    Text("Aranıyor...", color = KitsugiColors.TextSecondary, fontSize = 11.sp)
                    Text(sourceName, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ─── Manga card ───────────────────────────────────────────────────────────────

@Composable
private fun MangaCard(manga: MangaDetails, accent: Color, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)).tvClickable(shape = RoundedCornerShape(12.dp)) { onClick() },
        colors = CardDefaults.cardColors(containerColor = KitsugiColors.SurfaceSoft),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(model = manga.thumbnailUrl, contentDescription = manga.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.93f)))).padding(8.dp)
            ) {
                Column {
                    Text(manga.title, color = KitsugiColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!manga.source.isNullOrBlank())
                        Text(manga.source, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Empty sources hint ───────────────────────────────────────────────────────

@Composable
private fun EmptySourcesHint(onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Extension, null, tint = KitsugiColors.TextMuted, modifier = Modifier.size(56.dp))
            Text("Henüz manga eklentisi yüklü değil.", color = KitsugiColors.TextSecondary, fontWeight = FontWeight.Medium)
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalKitsugiAccent.current,
                    contentColor = KitsugiColors.Background
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Eklentileri Düzenle", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Manga card shimmer ───────────────────────────────────────────────────────

@Composable
private fun MangaCardShimmer() {
    val inf = rememberInfiniteTransition(label = "shimmer")
    val alpha by inf.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = KitsugiColors.SurfaceSoft),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = alpha * 0.1f))
        ) {
            // Text placeholders
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f))))
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = alpha * 0.15f))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = alpha * 0.15f))
                )
            }
        }
    }
}
