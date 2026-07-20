package com.kitsugi.animelist.ui.screens.stream

import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.utils.tvClickable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreamScreenContent(
    title: String,
    posterUrl: String?,
    episode: Int,
    season: Int,
    isMovie: Boolean = false,
    imdbId: String?,
    accentColor: Color,
    addonStates: List<AddonFetchState>,
    allStreams: List<StreamSource>,
    isResolvingId: Boolean,
    idResolveFailed: Boolean,
    isAnyLoading: Boolean,
    selectedAddonFilter: String?,
    onAddonFilterChange: (String?) -> Unit,
    onStreamSelected: (StreamSource) -> Unit,
    onBack: () -> Unit,
    resolvingSource: StreamSource?,
    resolvingError: String?,
    onResolvingErrorDismiss: () -> Unit,
    pendingPlayAction: PendingPlayAction?,
    onPendingDismiss: () -> Unit,
    playerPrefs: SharedPreferences,
    launchPlayer: (StreamSource, String, String) -> Unit,
    onPendingDone: () -> Unit,
    onRememberChoice: (String) -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
    onVerifyPlugin: ((addonName: String) -> Unit)? = null
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // ── Local Search & Tag Filtering State ─────────────────────────────────────
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }

    val sortedAddonStates = remember(addonStates) {
        addonStates.sortedWith(
            compareByDescending<AddonFetchState> { it.streams.isNotEmpty() }
                .thenByDescending { it.isLoading }
        )
    }

    // ── Real-time Synchronized Stream & Addon Filtering ─────────────────────────
    val filteredAddonStates = remember(sortedAddonStates, selectedAddonFilter, searchQuery, selectedTagFilter) {
        val query = searchQuery.trim().lowercase()
        val tag = selectedTagFilter?.lowercase()

        sortedAddonStates.mapNotNull { addonState ->
            if (selectedAddonFilter != null && addonState.addonName != selectedAddonFilter) {
                return@mapNotNull null
            }

            val matchingStreams = addonState.streams.filter { stream ->
                val (qual, _) = parseStreamQuality(stream)
                val langType = detectStreamLang(stream)

                val matchesQuery = query.isEmpty() ||
                    stream.name.lowercase().contains(query) ||
                    stream.title.lowercase().contains(query) ||
                    stream.addonName.lowercase().contains(query) ||
                    (stream.url != null && stream.url.lowercase().contains(query)) ||
                    qual.lowercase().contains(query) ||
                    langType.label.lowercase().contains(query) ||
                    ((query == "tr" || query == "türkçe" || query == "turkce") && (langType == StreamLangType.SUB || langType == StreamLangType.DUB || langType == StreamLangType.DUAL))

                val matchesTag = when (tag) {
                    "altyazı" -> langType == StreamLangType.SUB || langType == StreamLangType.DUAL
                    "dublaj" -> langType == StreamLangType.DUB || langType == StreamLangType.DUAL
                    "1080p" -> qual.contains("1080") || stream.qualityValue == 1080
                    "720p" -> qual.contains("720") || stream.qualityValue == 720
                    "debrid" -> stream.infoHash != null || stream.url?.contains("magnet") == true
                    else -> true
                }

                matchesQuery && matchesTag
            }

            if (matchingStreams.isEmpty() && !addonState.isLoading && addonState.error == null) {
                null
            } else {
                addonState.copy(streams = matchingStreams)
            }
        }
    }

    val totalFilteredStreamsCount = remember(filteredAddonStates) {
        filteredAddonStates.sumOf { it.streams.size }
    }
    val hasActiveFilter = searchQuery.isNotBlank() || selectedTagFilter != null

    Box(modifier = Modifier.fillMaxSize().background(KitsugiColors.Background)) {
        if (!posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = posterUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.12f)
            )
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(KitsugiColors.Background.copy(alpha = 0.65f), KitsugiColors.Background))))

        if (isLandscape) {
            LandscapeLayout(
                title = title, posterUrl = posterUrl, episode = episode, season = season, isMovie = isMovie,
                imdbId = imdbId, accentColor = accentColor, addonStates = sortedAddonStates,
                allStreams = allStreams, selectedAddonFilter = selectedAddonFilter,
                searchQuery = searchQuery, onQueryChange = { searchQuery = it },
                selectedTagFilter = selectedTagFilter, onTagSelect = { selectedTagFilter = it },
                onAddonFilterChange = onAddonFilterChange, onBack = onBack,
                isResolvingId = isResolvingId, idResolveFailed = idResolveFailed, isAnyLoading = isAnyLoading,
                filteredAddonStates = filteredAddonStates, totalFilteredStreamsCount = totalFilteredStreamsCount,
                hasActiveFilter = hasActiveFilter, onStreamSelected = onStreamSelected,
                onVerifyPlugin = onVerifyPlugin, onOpenSettings = onOpenSettings
            )
        } else {
            PortraitLayout(
                title = title, posterUrl = posterUrl, episode = episode, season = season, isMovie = isMovie,
                imdbId = imdbId, accentColor = accentColor, addonStates = sortedAddonStates,
                allStreams = allStreams, selectedAddonFilter = selectedAddonFilter,
                searchQuery = searchQuery, onQueryChange = { searchQuery = it },
                selectedTagFilter = selectedTagFilter, onTagSelect = { selectedTagFilter = it },
                onAddonFilterChange = onAddonFilterChange, onBack = onBack,
                isResolvingId = isResolvingId, idResolveFailed = idResolveFailed, isAnyLoading = isAnyLoading,
                filteredAddonStates = filteredAddonStates, totalFilteredStreamsCount = totalFilteredStreamsCount,
                hasActiveFilter = hasActiveFilter, onStreamSelected = onStreamSelected,
                onVerifyPlugin = onVerifyPlugin, onOpenSettings = onOpenSettings
            )
        }

        // Resolving overlay
        if (resolvingSource != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)).tvClickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KitsugiColors.Surface), modifier = Modifier.padding(32.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(color = accentColor, strokeWidth = 3.dp)
                        val isDebrid = resolvingSource.infoHash != null || resolvingSource.url?.contains("magnet") == true
                        val message = if (isDebrid) "Debrid Üzerinden Çözümleniyor..." else "Akış Bağlantısı Çözülüyor..."
                        Text(message, color = KitsugiColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(resolvingSource.title, color = KitsugiColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Error dialog
        if (resolvingError != null) {
            AlertDialog(
                onDismissRequest = onResolvingErrorDismiss,
                title = { Text("Hata", color = KitsugiColors.TextPrimary) },
                text = { Text(resolvingError, color = KitsugiColors.TextSecondary) },
                confirmButton = { TextButton(onClick = onResolvingErrorDismiss) { Text("Tamam", color = accentColor) } },
                containerColor = KitsugiColors.Surface
            )
        }

        // Player picker dialog
        if (pendingPlayAction != null) {
            val playAction = pendingPlayAction
            var rememberChoice by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = onPendingDismiss,
                containerColor = KitsugiColors.Surface, titleContentColor = KitsugiColors.TextPrimary,
                textContentColor = KitsugiColors.TextSecondary, shape = RoundedCornerShape(26.dp),
                title = { Text("Oynatıcı Seçin", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Videoyu hangi oynatıcı ile açmak istersiniz?", color = KitsugiColors.TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        PlayerPickerCard(
                            icon = { Icon(Icons.Rounded.PlayCircle, null, tint = accentColor) },
                            label = "Dahili Oynatıcı (ExoPlayer)", sub = "Uygulama içi kesintisiz oynatma",
                            accentColor = accentColor,
                            onClick = {
                                if (rememberChoice) onRememberChoice("exoplayer")
                                onPendingDone()
                                launchPlayer(playAction.source, playAction.resolvedUrl, "exoplayer")
                            }
                        )
                        PlayerPickerCard(
                            icon = { Icon(Icons.AutoMirrored.Rounded.Launch, null, tint = accentColor) },
                            label = "Harici Oynatıcı (Chooser)", sub = "MX Player, Mi Video veya diğerleri",
                            accentColor = accentColor,
                            onClick = {
                                if (rememberChoice) onRememberChoice("mpv")
                                onPendingDone()
                                launchPlayer(playAction.source, playAction.resolvedUrl, "mpv")
                            }
                        )
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).tvClickable(shape = RoundedCornerShape(12.dp)) { rememberChoice = !rememberChoice }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(checked = rememberChoice, onCheckedChange = { rememberChoice = it }, colors = CheckboxDefaults.colors(checkedColor = accentColor, uncheckedColor = KitsugiColors.TextMuted, checkmarkColor = KitsugiColors.Surface))
                            Text("Seçimimi hatırla", color = KitsugiColors.TextPrimary)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = onPendingDismiss) { Text("İptal", color = accentColor) } }
            )
        }
    }
}

@Composable
private fun PlayerPickerCard(icon: @Composable () -> Unit, label: String, sub: String, accentColor: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().tvClickable(shape = RoundedCornerShape(16.dp)) { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = KitsugiColors.SurfaceStrong.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(accentColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { icon() }
            Column { Text(label, color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold); Text(sub, color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun StreamSearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedTagFilter: String?,
    onTagSelect: (String?) -> Unit,
    accentColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Kaynak, eklenti veya video ara... (Dizilla, Sibnet, Dublaj)",
                    color = KitsugiColors.TextMuted,
                    fontSize = 12.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = if (searchQuery.isNotBlank()) accentColor else KitsugiColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Temizle",
                            tint = KitsugiColors.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = KitsugiColors.SurfaceStrong.copy(alpha = 0.5f),
                unfocusedContainerColor = KitsugiColors.Surface.copy(alpha = 0.3f),
                focusedBorderColor = accentColor,
                unfocusedBorderColor = KitsugiColors.SurfaceStrong,
                focusedTextColor = KitsugiColors.TextPrimary,
                unfocusedTextColor = KitsugiColors.TextPrimary
            ),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            val tags = listOf(
                "Altyazı" to "💬 Altyazı",
                "Dublaj" to "🎙️ Dublaj",
                "1080p" to "✨ 1080p",
                "720p" to "🎬 720p",
                "debrid" to "⚡ Debrid"
            )
            items(tags) { (tagKey, tagLabel) ->
                val isSelected = selectedTagFilter == tagKey
                FilterChip(
                    selected = isSelected,
                    onClick = { onTagSelect(if (isSelected) null else tagKey) },
                    label = { Text(tagLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.25f),
                        selectedLabelColor = accentColor,
                        containerColor = KitsugiColors.SurfaceStrong.copy(alpha = 0.3f),
                        labelColor = KitsugiColors.TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = KitsugiColors.SurfaceStrong,
                        selectedBorderColor = accentColor.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}

@Composable
private fun FilteredEmptyState(
    searchQuery: String,
    tagFilter: String?,
    accentColor: Color,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KitsugiColors.SurfaceStrong.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SearchOff,
                contentDescription = null,
                tint = KitsugiColors.TextMuted,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "Aramanızla Eşleşen Kaynak Bulunamadı",
                color = KitsugiColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            val desc = buildString {
                if (searchQuery.isNotBlank()) append("\"$searchQuery\" ")
                if (tagFilter != null) append("[$tagFilter] ")
                append("kriterlerine uygun video akışı veya eklenti mevcut değil.")
            }
            Text(
                text = desc,
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onClearFilters,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.2f), contentColor = accentColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.FilterAltOff, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Filtreleri Temizle", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LandscapeLayout(
    title: String, posterUrl: String?, episode: Int, season: Int, isMovie: Boolean = false, imdbId: String?,
    accentColor: Color, addonStates: List<AddonFetchState>, allStreams: List<StreamSource>,
    selectedAddonFilter: String?, searchQuery: String, onQueryChange: (String) -> Unit,
    selectedTagFilter: String?, onTagSelect: (String?) -> Unit,
    onAddonFilterChange: (String?) -> Unit, onBack: () -> Unit,
    isResolvingId: Boolean, idResolveFailed: Boolean, isAnyLoading: Boolean,
    filteredAddonStates: List<AddonFetchState>, totalFilteredStreamsCount: Int, hasActiveFilter: Boolean,
    onStreamSelected: (StreamSource) -> Unit, onVerifyPlugin: ((String) -> Unit)?, onOpenSettings: (() -> Unit)?
) {
    val listState = rememberLazyListState()

    Row(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        // Left Column: Fixed compact detail sidebar & back navigation
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.32f)
                .background(KitsugiColors.Surface.copy(alpha = 0.2f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = KitsugiColors.SurfaceStrong.copy(alpha = 0.5f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri", tint = KitsugiColors.TextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Geri",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KitsugiColors.SurfaceStrong)
            ) {
                if (!posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = posterUrl, contentDescription = title,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Movie, null, tint = KitsugiColors.TextMuted, modifier = Modifier.size(36.dp))
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    title,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (isMovie) "Film" else "Sezon $season · Bölüm $episode",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                imdbId?.let { ImdbBadge(it) }
            }
        }

        // Right Column: Scrollable LazyColumn with Search, Sticky Addon Chips, and Streams
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.68f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // Item 1: Search Bar & Quick Tags
            item(key = "land_search_bar") {
                StreamSearchBar(
                    searchQuery = searchQuery,
                    onQueryChange = onQueryChange,
                    selectedTagFilter = selectedTagFilter,
                    onTagSelect = onTagSelect,
                    accentColor = accentColor
                )
            }

            // Sticky Header: Addon Chips Row
            stickyHeader(key = "land_addon_chips_sticky") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KitsugiColors.Background.copy(alpha = 0.95f))
                        .padding(vertical = 4.dp)
                ) {
                    AddonChipsRow(
                        addonStates = addonStates,
                        allStreams = allStreams,
                        selectedAddonFilter = selectedAddonFilter,
                        searchQuery = searchQuery,
                        accentColor = accentColor,
                        onFilterChange = onAddonFilterChange
                    )
                    HorizontalDivider(color = KitsugiColors.SurfaceStrong.copy(alpha = 0.5f))
                }
            }

            // Content Items (Streams / Skeleton / Empty state / Error state)
            when {
                isResolvingId -> {
                    item(key = "land_resolving_id") { ResolvingIdState() }
                }
                idResolveFailed -> {
                    item(key = "land_id_failed") { ErrorState("Bu içerik için IMDb ID'si bulunamadı.\nLütfen farklı bir kaynak deneyin.", null) }
                }
                addonStates.isEmpty() && !isResolvingId -> {
                    item(key = "land_no_addons") { NoAddonsState(onOpenSettings = onOpenSettings) }
                }
                !isAnyLoading && allStreams.isEmpty() -> {
                    item(key = "land_empty_streams") { EmptyStreamsState(onOpenSettings = onOpenSettings) }
                }
                !isAnyLoading && totalFilteredStreamsCount == 0 && hasActiveFilter -> {
                    item(key = "land_filtered_empty") {
                        FilteredEmptyState(
                            searchQuery = searchQuery,
                            tagFilter = selectedTagFilter,
                            accentColor = accentColor,
                            onClearFilters = {
                                onQueryChange("")
                                onTagSelect(null)
                                onAddonFilterChange(null)
                            }
                        )
                    }
                }
                else -> {
                    renderStreamResultsItems(
                        addonStates = filteredAddonStates,
                        accentColor = accentColor,
                        onStreamSelected = onStreamSelected,
                        onVerifyPlugin = onVerifyPlugin,
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PortraitLayout(
    title: String, posterUrl: String?, episode: Int, season: Int, isMovie: Boolean = false, imdbId: String?,
    accentColor: Color, addonStates: List<AddonFetchState>, allStreams: List<StreamSource>,
    selectedAddonFilter: String?, searchQuery: String, onQueryChange: (String) -> Unit,
    selectedTagFilter: String?, onTagSelect: (String?) -> Unit,
    onAddonFilterChange: (String?) -> Unit, onBack: () -> Unit,
    isResolvingId: Boolean, idResolveFailed: Boolean, isAnyLoading: Boolean,
    filteredAddonStates: List<AddonFetchState>, totalFilteredStreamsCount: Int, hasActiveFilter: Boolean,
    onStreamSelected: (StreamSource) -> Unit, onVerifyPlugin: ((String) -> Unit)?, onOpenSettings: (() -> Unit)?
) {
    val listState = rememberLazyListState()
    val showFloatingHeader by remember {
        derivedStateOf { listState.firstVisibleItemIndex >= 1 }
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Item 1: Top Navigation Header (Initial view)
            item(key = "top_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = KitsugiColors.SurfaceStrong.copy(alpha = 0.5f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri", tint = KitsugiColors.TextPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (isMovie) "Film" else "Sezon $season · Bölüm $episode",
                            color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.bodySmall
                        )
                    }
                    imdbId?.let { ImdbBadge(it) }
                }
            }

            // Item 2: Poster Card
            item(key = "poster_card") {
                Row(
                    modifier = Modifier.fillMaxWidth().background(KitsugiColors.Surface.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(width = 60.dp, height = 90.dp).clip(RoundedCornerShape(8.dp)).background(KitsugiColors.SurfaceStrong)) {
                        if (!posterUrl.isNullOrBlank()) AsyncImage(model = posterUrl, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Movie, null, tint = KitsugiColors.TextMuted, modifier = Modifier.size(24.dp)) }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (isMovie) "Film" else "Sezon $season · Bölüm $episode",
                            color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Item 3: Stream Search Bar & Quick Tags
            item(key = "search_bar") {
                StreamSearchBar(
                    searchQuery = searchQuery,
                    onQueryChange = onQueryChange,
                    selectedTagFilter = selectedTagFilter,
                    onTagSelect = onTagSelect,
                    accentColor = accentColor
                )
            }

            // Sticky Header: Floating Bar (when scrolled) + Addon Chips Row
            stickyHeader(key = "addon_chips_sticky") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KitsugiColors.Background.copy(alpha = 0.95f))
                        .padding(vertical = 4.dp)
                ) {
                    AnimatedVisibility(
                        visible = showFloatingHeader,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(bottom = 6.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                colors = IconButtonDefaults.iconButtonColors(containerColor = KitsugiColors.SurfaceStrong.copy(alpha = 0.6f)),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri", tint = KitsugiColors.TextPrimary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    color = KitsugiColors.TextPrimary,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isMovie) "Film" else "Sezon $season · Bölüm $episode",
                                    color = KitsugiColors.TextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            imdbId?.let { ImdbBadge(it) }
                        }
                    }

                    AddonChipsRow(
                        addonStates = addonStates,
                        allStreams = allStreams,
                        selectedAddonFilter = selectedAddonFilter,
                        searchQuery = searchQuery,
                        accentColor = accentColor,
                        onFilterChange = onAddonFilterChange
                    )
                    HorizontalDivider(color = KitsugiColors.SurfaceStrong.copy(alpha = 0.5f))
                }
            }

            // Content Items (Streams / Skeleton / Empty state / Error state)
            when {
                isResolvingId -> {
                    item(key = "resolving_id") { ResolvingIdState() }
                }
                idResolveFailed -> {
                    item(key = "id_failed") { ErrorState("Bu içerik için IMDb ID'si bulunamadı.\nLütfen farklı bir kaynak deneyin.", null) }
                }
                addonStates.isEmpty() && !isResolvingId -> {
                    item(key = "no_addons") { NoAddonsState(onOpenSettings = onOpenSettings) }
                }
                !isAnyLoading && allStreams.isEmpty() -> {
                    item(key = "empty_streams") { EmptyStreamsState(onOpenSettings = onOpenSettings) }
                }
                !isAnyLoading && totalFilteredStreamsCount == 0 && hasActiveFilter -> {
                    item(key = "filtered_empty") {
                        FilteredEmptyState(
                            searchQuery = searchQuery,
                            tagFilter = selectedTagFilter,
                            accentColor = accentColor,
                            onClearFilters = {
                                onQueryChange("")
                                onTagSelect(null)
                                onAddonFilterChange(null)
                            }
                        )
                    }
                }
                else -> {
                    renderStreamResultsItems(
                        addonStates = filteredAddonStates,
                        accentColor = accentColor,
                        onStreamSelected = onStreamSelected,
                        onVerifyPlugin = onVerifyPlugin,
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun AddonChipsRow(
    addonStates: List<AddonFetchState>,
    allStreams: List<StreamSource>,
    selectedAddonFilter: String?,
    searchQuery: String,
    accentColor: Color,
    onFilterChange: (String?) -> Unit
) {
    val query = searchQuery.trim().lowercase()
    val visibleAddonStates = remember(addonStates, query) {
        if (query.isBlank()) addonStates
        else addonStates.filter { state ->
            state.addonName.lowercase().contains(query) ||
            state.streams.any { stream ->
                stream.name.lowercase().contains(query) ||
                stream.title.lowercase().contains(query) ||
                detectStreamLang(stream).label.lowercase().contains(query)
            }
        }
    }

    if (visibleAddonStates.isNotEmpty()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            item {
                AddonStatusChip(
                    state = AddonFetchState(
                        addonName = if (query.isBlank()) "Tümü" else "Arama (${visibleAddonStates.sumOf { it.streams.size }})",
                        manifestUrl = "",
                        isLoading = false,
                        streams = allStreams
                    ),
                    accentColor = accentColor,
                    isSelected = selectedAddonFilter == null,
                    onClick = { onFilterChange(null) }
                )
            }
            items(visibleAddonStates.size) { idx ->
                val addonState = visibleAddonStates[idx]
                AddonStatusChip(
                    state = addonState,
                    accentColor = accentColor,
                    isSelected = selectedAddonFilter == addonState.addonName,
                    onClick = { onFilterChange(if (selectedAddonFilter == addonState.addonName) null else addonState.addonName) }
                )
            }
        }
    }
}

@Composable
private fun ImdbBadge(imdbId: String) {
    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFF5C518)).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(imdbId, color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
    }
}
