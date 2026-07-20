package com.kitsugi.animelist.ui.tv.addons

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import com.kitsugi.animelist.data.local.CloudstreamRepoEntity
import com.kitsugi.animelist.data.local.ManagedAddonEntity
import com.kitsugi.animelist.ui.app.AddonViewModel
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.utils.tvClickable
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Sayfa 1/4: package, imports, state modeli ──────────────────────────────

private enum class TvAddonsTab { STREMIO, REPOS }

// ── Sayfa 2/4: Ana screen composable ───────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvAddonsScreen(
    addonViewModel: AddonViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    BackHandler { onBack() }

    val addons by addonViewModel.addonsList.collectAsStateWithLifecycle(initialValue = emptyList())
    val repos  by addonViewModel.reposList.collectAsStateWithLifecycle(initialValue = emptyList())
    val csPlugins by addonViewModel.csPluginsList.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTab by remember { mutableStateOf(TvAddonsTab.STREMIO) }
    var showAddAddonDialog by remember { mutableStateOf(false) }
    var showAddRepoDialog  by remember { mutableStateOf(false) }
    var showDebridDialog   by remember { mutableStateOf(false) }
    var toastMessage       by remember { mutableStateOf<String?>(null) }
    var toastIsError       by remember { mutableStateOf(false) }

    val contentFocusRequester = remember { FocusRequester() }
    val tabFocusRequesters = remember { List(2) { FocusRequester() } }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try { tabFocusRequesters[0].requestFocusAfterFrames(frames = 3) } catch (_: Exception) {}
    }

    // Bağla onShowMessage
    LaunchedEffect(addonViewModel) {
        addonViewModel.onShowMessage = { msg ->
            toastMessage = msg
            toastIsError = msg.startsWith("⚠️") || msg.startsWith("❌")
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3200)
            toastMessage = null
        }
    }

    // Repo pluginlerini arka planda çek
    LaunchedEffect(repos) {
        addonViewModel.syncRepos(repos)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = KitsugiTvTokens.Spacing.screenHorizontal,
                    vertical   = KitsugiTvTokens.Spacing.screenVertical
                ),
            horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.screenHorizontal)
        ) {
            // ── Sol Panel: Sekmeler ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .width(KitsugiTvTokens.Layout.sidebarExpandedWidth - 40.dp)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.gridRowGap)
            ) {
                Text(
                    text = "Eklentiler",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = KitsugiTvTokens.Spacing.contentPadding)
                )

                TvAddonsTabButton(
                    label     = "Stremio Eklentileri",
                    icon      = Icons.Default.Extension,
                    selected  = selectedTab == TvAddonsTab.STREMIO,
                    badge     = addons.size,
                    focusRequester = tabFocusRequesters[0],
                    rightTarget = contentFocusRequester,
                    onClick   = { selectedTab = TvAddonsTab.STREMIO }
                )
                TvAddonsTabButton(
                    label     = "Repo & Eklenti Havuzları",
                    icon      = Icons.Default.Folder,
                    selected  = selectedTab == TvAddonsTab.REPOS,
                    badge     = repos.size,
                    focusRequester = tabFocusRequesters[1],
                    rightTarget = contentFocusRequester,
                    onClick   = { selectedTab = TvAddonsTab.REPOS }
                )
            }

            // ── Sağ Panel: İçerik ───────────────────────────────────────────
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(contentFocusRequester),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap)
            ) {
                when (selectedTab) {
                    TvAddonsTab.STREMIO -> {
                        item {
                            TvAddonsPanelHeader(
                                title   = "Stremio Addon Yönetimi",
                                subtitle = "${addons.size} addon kurulu",
                                actionLabel = "Addon Ekle",
                                actionIcon  = Icons.Default.Add,
                                onAction    = { showAddAddonDialog = true }
                            )
                        }
                        item {
                            TvAddonsDebridRow(
                                debridToken = addonViewModel.debridToken,
                                onClick     = { showDebridDialog = true }
                            )
                        }
                        if (addons.isEmpty()) {
                            item {
                                TvAddonsEmptyState(
                                    message = "Henüz Stremio addon kurulmamış.\nEklenti eklemek için \"Addon Ekle\" butonuna basın."
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = addons,
                                key   = { _, a -> a.manifestUrl }
                            ) { _, addon ->
                                TvAddonCard(
                                    addon     = addon,
                                    onToggle  = { addonViewModel.toggleAddon(addon, it) },
                                    onDelete  = { addonViewModel.deleteAddon(addon) }
                                )
                            }
                        }
                    }
                    TvAddonsTab.REPOS -> {
                        item {
                            TvAddonsPanelHeader(
                                title   = "Repo & Eklenti Havuzları",
                                subtitle = "${repos.size} repo tanımlı",
                                actionLabel = "Repo Ekle",
                                actionIcon  = Icons.Default.Add,
                                onAction    = { showAddRepoDialog = true }
                            )
                        }
                        if (repos.isEmpty()) {
                            item {
                                TvAddonsEmptyState(
                                    message = "Henüz repo eklenmemiş.\n\"Repo Ekle\" ile bir eklenti havuzu tanımlayın."
                                )
                            }
                        } else {
                            items(
                                items = repos,
                                key   = { it.repoUrl }
                            ) { repo ->
                                val plugins = addonViewModel.repoPluginsState[repo.repoUrl]
                                val isLoading = addonViewModel.repoLoadingState[repo.repoUrl] == true
                                val isBulkInstalling = addonViewModel.bulkInstallRepoUrl == repo.repoUrl
                                TvRepoCard(
                                    repo           = repo,
                                    pluginCount    = plugins?.size,
                                    isLoading      = isLoading,
                                    isBulkInstalling = isBulkInstalling,
                                    bulkDone       = if (isBulkInstalling) addonViewModel.bulkInstallDone else 0,
                                    bulkTotal      = if (isBulkInstalling) addonViewModel.bulkInstallTotal else 0,
                                    onInstallAll   = {
                                        if (plugins != null && !isBulkInstalling) {
                                            addonViewModel.installAllPlugins(
                                                repo.repoUrl, repo.name, plugins, addons, csPlugins
                                            )
                                        }
                                    },
                                    onRefresh      = { scope.launch { addonViewModel.fetchRepoPlugins(repo.repoUrl) } },
                                    onDelete       = { addonViewModel.deleteRepo(repo) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Toast overlay ───────────────────────────────────────────────────
        TvAddonsToast(message = toastMessage, isError = toastIsError)
    }

    // ── Dialoglar ───────────────────────────────────────────────────────────
    if (showAddAddonDialog) {
        TvAddonsInputDialog(
            title       = "Addon URL Gir",
            placeholder = "https://... /manifest.json",
            confirmLabel = "Ekle",
            onConfirm   = { url ->
                addonViewModel.addAddon(url)
                showAddAddonDialog = false
            },
            onDismiss   = { showAddAddonDialog = false }
        )
    }

    if (showAddRepoDialog) {
        TvAddonsInputDialog(
            title       = "Repo URL Gir",
            placeholder = "https://repo.example.com",
            confirmLabel = "Ekle",
            onConfirm   = { url ->
                addonViewModel.addRepo(url)
                showAddRepoDialog = false
            },
            onDismiss   = { showAddRepoDialog = false }
        )
    }

    if (showDebridDialog) {
        TvAddonsInputDialog(
            title        = "Debrid Token Gir",
            placeholder  = "RealDebrid / Alldebrid token",
            initialValue = addonViewModel.debridToken,
            confirmLabel = "Kaydet",
            onConfirm    = { token ->
                addonViewModel.saveDebridToken(token)
                showDebridDialog = false
            },
            onDismiss    = { showDebridDialog = false }
        )
    }
}

// ── Sayfa 3/4: Yardımcı bileşenler ────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvAddonsTabButton(
    label:       String,
    icon:        ImageVector,
    selected:    Boolean,
    badge:       Int,
    focusRequester: FocusRequester,
    rightTarget: FocusRequester,
    onClick:     () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgAlpha by animateFloatAsState(
        if (selected) 0.18f else if (isFocused) 0.10f else 0f, label = "tab_bg"
    )
    val borderAlpha by animateFloatAsState(
        if (isFocused) 1f else 0f, label = "tab_border"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .clip(RoundedCornerShape(KitsugiTvTokens.Spacing.sm))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(KitsugiTvTokens.Spacing.sm)
            )
            .focusProperties { right = rightTarget }
            .tvClickable(shape = RoundedCornerShape(KitsugiTvTokens.Spacing.sm)) { onClick() }
            .onFocusChanged { isFocused = it.isFocused; if (it.isFocused) onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(0.7f), modifier = Modifier.size(20.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = if (selected) Color.White else Color.White.copy(0.7f), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
        if (badge > 0) {
            Box(
                modifier = Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = badge.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvAddonsPanelHeader(
    title:       String,
    subtitle:    String,
    actionLabel: String,
    actionIcon:  ImageVector,
    onAction:    () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
        }
        Button(
            onClick = onAction,
            colors  = ButtonDefaults.colors(
                containerColor        = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                contentColor          = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.primary,
                focusedContentColor   = Color.Black
            ),
            shape = ButtonDefaults.shape(RoundedCornerShape(KitsugiTvTokens.Spacing.sm))
        ) {
            Icon(imageVector = actionIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = actionLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvAddonsDebridRow(debridToken: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val isConnected = debridToken.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KitsugiTvTokens.Spacing.sm))
            .background(if (isFocused) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f))
            .border(1.dp, if (isFocused) MaterialTheme.colorScheme.primary.copy(0.7f) else Color.White.copy(0.08f), RoundedCornerShape(KitsugiTvTokens.Spacing.sm))
            .tvClickable(shape = RoundedCornerShape(KitsugiTvTokens.Spacing.sm)) { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape)
                .background(if (isConnected) Color(0xFF2E7D32).copy(0.25f) else Color.White.copy(0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.Check else Icons.Default.Settings,
                contentDescription = null,
                tint = if (isConnected) Color(0xFF4CAF50) else Color.White.copy(0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("RealDebrid / Alldebrid Token", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (isConnected) "Token bağlı: ${debridToken.take(10)}..." else "Token tanımlanmamış — tıklayın",
                style = MaterialTheme.typography.bodySmall,
                color = if (isConnected) Color(0xFF4CAF50) else Color.White.copy(0.4f)
            )
        }
        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color.White.copy(0.3f), modifier = Modifier.size(18.dp))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvAddonCard(
    addon:    ManagedAddonEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var isFocused    by remember { mutableStateOf(false) }
    var showConfirm  by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KitsugiTvTokens.Spacing.sm))
            .background(if (isFocused) Color.White.copy(0.08f) else Color.White.copy(0.03f))
            .border(1.dp, if (isFocused) MaterialTheme.colorScheme.primary.copy(0.5f) else Color.White.copy(0.06f), RoundedCornerShape(KitsugiTvTokens.Spacing.sm))
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = addon.name, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = addon.description ?: addon.manifestUrl, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.4f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Switch(
            checked = addon.isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = MaterialTheme.colorScheme.primary,
                checkedTrackColor  = MaterialTheme.colorScheme.primary.copy(0.3f),
                uncheckedThumbColor = Color.White.copy(0.5f),
                uncheckedTrackColor = Color.White.copy(0.1f)
            )
        )
        Surface(
            onClick = { showConfirm = true },
            colors  = ClickableSurfaceDefaults.colors(
                containerColor        = Color.Transparent,
                focusedContainerColor = Color(0xFFC62828).copy(0.2f)
            ),
            shape  = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFC62828)),
                    shape  = RoundedCornerShape(8.dp)
                )
            )
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFEF5350), modifier = Modifier.padding(8.dp).size(18.dp))
        }
    }

    if (showConfirm) {
        TvAddonsConfirmDialog(
            message    = "\"${addon.name}\" silinsin mi?",
            onConfirm  = { onDelete(); showConfirm = false },
            onDismiss  = { showConfirm = false }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvRepoCard(
    repo:             CloudstreamRepoEntity,
    pluginCount:      Int?,
    isLoading:        Boolean,
    isBulkInstalling: Boolean,
    bulkDone:         Int,
    bulkTotal:        Int,
    onInstallAll:     () -> Unit,
    onRefresh:        () -> Unit,
    onDelete:         () -> Unit
) {
    var isFocused   by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KitsugiTvTokens.Spacing.sm))
            .background(if (isFocused) Color.White.copy(0.08f) else Color.White.copy(0.03f))
            .border(1.dp, if (isFocused) MaterialTheme.colorScheme.primary.copy(0.5f) else Color.White.copy(0.06f), RoundedCornerShape(KitsugiTvTokens.Spacing.sm))
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1565C0).copy(0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF42A5F5), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF42A5F5), modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = repo.name, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val desc = when {
                isBulkInstalling -> "Kuruluyor: $bulkDone / $bulkTotal"
                isLoading        -> "Yükleniyor..."
                pluginCount != null -> "$pluginCount eklenti"
                else             -> repo.repoUrl
            }
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.4f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // Refresh
        Surface(
            onClick = onRefresh,
            colors  = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.White.copy(0.08f)),
            shape   = ClickableSurfaceDefaults.shape(CircleShape),
            border  = ClickableSurfaceDefaults.border(focusedBorder = Border(border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape = CircleShape))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = Color.White.copy(0.5f), modifier = Modifier.padding(8.dp).size(16.dp))
        }
        // Hepsini Kur
        Button(
            onClick  = onInstallAll,
            enabled  = pluginCount != null && !isBulkInstalling,
            colors   = ButtonDefaults.colors(
                containerColor        = MaterialTheme.colorScheme.primary.copy(0.15f),
                contentColor          = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.primary,
                focusedContentColor   = Color.Black,
                disabledContainerColor = Color.White.copy(0.05f),
                disabledContentColor   = Color.White.copy(0.3f)
            ),
            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
        ) {
            Text(if (isBulkInstalling) "Kuruluyor..." else "Hepsini Kur", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        // Sil
        Surface(
            onClick = { showConfirm = true },
            colors  = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color(0xFFC62828).copy(0.15f)),
            shape   = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
            border  = ClickableSurfaceDefaults.border(focusedBorder = Border(border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFC62828)), shape = RoundedCornerShape(8.dp)))
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFEF5350), modifier = Modifier.padding(8.dp).size(16.dp))
        }
    }

    if (showConfirm) {
        TvAddonsConfirmDialog(
            message   = "\"${repo.name}\" silinsin mi?",
            onConfirm = { onDelete(); showConfirm = false },
            onDismiss = { showConfirm = false }
        )
    }
}

@Composable
private fun TvAddonsEmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(KitsugiTvTokens.Spacing.sm))
            .background(Color.White.copy(0.03f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.4f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

// ── Sayfa 4/4: Toast, ConfirmDialog, InputDialog ───────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvAddonsToast(message: String?, isError: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(visible = message != null, enter = fadeIn(), exit = fadeOut()) {
            val visible = message ?: return@AnimatedVisibility
            Surface(
                onClick = {},
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (isError) Color(0xFFC62828).copy(0.92f) else Color(0xFF2E7D32).copy(0.92f)
                ),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.Close else Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(text = visible, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvAddonsConfirmDialog(
    message:   String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cancelFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try { cancelFocusRequester.requestFocusAfterFrames(frames = 3) } catch (_: Exception) {}
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(420.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A1A2E))
                    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(20.dp))
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(text = "Onay", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = message, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).focusRequester(cancelFocusRequester),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(0.08f),
                            contentColor   = Color.White.copy(0.8f),
                            focusedContainerColor = Color.White.copy(0.15f),
                            focusedContentColor   = Color.White
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(12.dp))
                    ) { Text("Vazgeç") }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xFFC62828).copy(0.8f),
                            contentColor   = Color.White,
                            focusedContainerColor = Color(0xFFC62828),
                            focusedContentColor   = Color.White
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(12.dp))
                    ) { Text("Sil", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvAddonsInputDialog(
    title:        String,
    placeholder:  String,
    initialValue: String = "",
    confirmLabel: String,
    onConfirm:    (String) -> Unit,
    onDismiss:    () -> Unit
) {
    var input by remember { mutableStateOf(initialValue) }
    val confirmFR = remember { FocusRequester() }
    val cancelFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try { cancelFocusRequester.requestFocusAfterFrames(frames = 3) } catch (_: Exception) {}
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(500.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A1A2E))
                    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(20.dp))
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                androidx.compose.foundation.text.BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.07f))
                        .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    decorationBox = { inner ->
                        Box {
                            if (input.isEmpty()) {
                                Text(text = placeholder, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.3f))
                            }
                            inner()
                        }
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).focusRequester(cancelFocusRequester),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(0.08f),
                            contentColor   = Color.White.copy(0.8f),
                            focusedContainerColor = Color.White.copy(0.15f),
                            focusedContentColor   = Color.White
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(12.dp))
                    ) { Text("Vazgeç") }
                    Button(
                        onClick  = { if (input.isNotBlank()) onConfirm(input.trim()) },
                        enabled  = input.isNotBlank(),
                        modifier = Modifier.weight(1f).focusRequester(confirmFR),
                        colors   = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(0.85f),
                            contentColor   = Color.Black,
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            focusedContentColor   = Color.Black,
                            disabledContainerColor = Color.White.copy(0.05f),
                            disabledContentColor   = Color.White.copy(0.3f)
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(12.dp))
                    ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
