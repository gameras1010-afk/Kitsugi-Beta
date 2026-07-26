@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.kitsugi.animelist.ui.screens.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.components.KitsugiEmptyState
import com.kitsugi.animelist.ui.components.KitsugiShimmerSearchResultList
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    currentEntries: List<MediaEntry>,
    showAdultContent: Boolean,
    onOpenApiDetail: (JikanSearchResult) -> Unit,
    onAddSelectionToList: (ApiSearchSelection) -> Unit,
    viewModel: SearchViewModel = viewModel(),
    titleLanguage: String = "ROMAJI",
    scoreFormat: String = "POINT_10",
    hideScores: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val accentColor = LocalKitsugiAccent.current
    val isTv = LocalIsTv.current
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Active Dialog States
    var openPlatformDialog by remember { mutableStateOf(false) }
    var openFormatDialog by remember { mutableStateOf(false) }
    var openStatusDialog by remember { mutableStateOf(false) }
    var openDateDialog by remember { mutableStateOf(false) }
    var openScoreDialog by remember { mutableStateOf(false) }
    var openSortDialog by remember { mutableStateOf(false) }
    var openTmdbFormatDialog by remember { mutableStateOf(false) }
    var openTmdbGenreDialog by remember { mutableStateOf(false) }

    val entryMap = remember(currentEntries) {
        val mapping = mutableMapOf<String, MediaEntry>()
        currentEntries.forEach { entry ->
            mapping["${entry.source.lowercase()}_${entry.malId}"] = entry
            if (entry.tmdbId != null) {
                mapping["tmdb_${entry.tmdbId}"] = entry
            }
            if (entry.simklId != null) {
                mapping["simkl_${entry.simklId}"] = entry
            }
            if (entry.source.equals("anilist", ignoreCase = true) && entry.malId != null && entry.malId >= 100_000_000) {
                mapping["anilist_${entry.malId - 100_000_000}"] = entry
            }
            if (entry.source.equals("jikan", ignoreCase = true) || entry.source.equals("mal", ignoreCase = true)) {
                mapping["mal_${entry.malId}"] = entry
                mapping["jikan_${entry.malId}"] = entry
            }
            val normTitle = entry.title.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }.trim()
            if (normTitle.isNotEmpty()) {
                mapping["${entry.type.name.lowercase()}_$normTitle"] = entry
            }
        }
        mapping
    }

    val getMediaEntry = remember(entryMap) {
        { result: JikanSearchResult ->
            val directKey = "${result.source.lowercase()}_${result.malId}"
            var found = entryMap[directKey]

            if (found == null) {
                val tmdbId = result.tmdbId ?: if (result.source.equals("tmdb", ignoreCase = true)) result.malId else null
                if (tmdbId != null) {
                    found = entryMap["tmdb_$tmdbId"]
                }
            }

            if (found == null) {
                val rMal = if (result.source.equals("jikan", ignoreCase = true) || result.source.equals("mal", ignoreCase = true)) {
                    result.malId
                } else {
                    result.realMalId
                }
                if (rMal != null) {
                    found = entryMap["${result.source.lowercase()}_$rMal"]
                        ?: entryMap["mal_$rMal"]
                        ?: entryMap["jikan_$rMal"]
                        ?: entryMap["anilist_$rMal"]
                        ?: entryMap["simkl_$rMal"]
                }
            }

            if (found == null) {
                val normTitle = buildString {
                    for (c in result.title.lowercase()) {
                        if (c in 'a'..'z' || c in '0'..'9') append(c)
                    }
                }.trim()
                if (normTitle.isNotEmpty()) {
                    found = entryMap["${result.type.name.lowercase()}_$normTitle"]
                }
            }

            found
        }
    }

    val isAlreadyInList = remember(getMediaEntry) {
        { result: JikanSearchResult ->
            getMediaEntry(result) != null
        }
    }

    val showIdleContent = !uiState.hasSearched && !uiState.isLoading
    val showFab by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 1 } }

    // Constants/Options mapping
    val isTmdbPlatform = uiState.selectedPlatform == SearchPlatform.TMDB
    val currentMediaType = uiState.selectedMediaType

    val animeFormats = listOf("TV", "MOVIE", "SPECIAL", "OVA", "ONA", "MUSIC")
    val mangaFormats = listOf("MANGA", "NOVEL", "ONE_SHOT", "DOUJIN", "MANHWA", "MANHUA")
    val formats = if (currentMediaType == MediaType.Manga) mangaFormats else animeFormats

    val animeStatuses = listOf("AIRING" to "Yayında", "FINISHED" to "Tamamlandı", "UPCOMING" to "Yakında")
    val mangaStatuses = listOf("PUBLISHING" to "Yayınlanıyor", "FINISHED" to "Tamamlandı", "HIATUS" to "Ara Verildi", "DISCONTINUED" to "Durduruldu")
    val statuses = if (currentMediaType == MediaType.Manga) mangaStatuses else animeStatuses

    val sortOptions = listOf(
        "POPULARITY_DESC" to "🔥 Popülerlik",
        "SCORE_DESC" to "⭐ Puan",
        "TITLE_ROMAJI_ASC" to "🔤 İsim (A-Z)",
        "TITLE_ROMAJI_DESC" to "🔤 İsim (Z-A)"
    )

    val tmdbGenres = listOf(
        "Tümü", "Aksiyon", "Macera", "Komedi", "Dram", "Fantastik",
        "Korku", "Gizem", "Romantizm", "Sci-Fi", "Gerilim", "Müzik", "Tarihi", "Animasyon"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .then(if (isTv) Modifier.dpadVerticalFastScroll(lazyListState) else Modifier)
        ) {
            // Title & Search Input Section
            item {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Arama",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(18.dp))

                // Premium Search Bar (inspired by AniHyou)
                var isFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .onFocusChanged { isFocused = it.hasFocus }
                        .border(
                            width = if (isFocused) 1.5.dp else 1.dp,
                            color = if (isFocused) accentColor else KitsugiColors.Border,
                            shape = RoundedCornerShape(22.dp)
                        )
                        .background(KitsugiColors.Surface)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Ara",
                            tint = KitsugiColors.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )

                        BasicTextField(
                            value = uiState.query,
                            onValueChange = viewModel::setQuery,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            cursorBrush = SolidColor(accentColor),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = KitsugiColors.TextPrimary,
                                fontWeight = FontWeight.Normal
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    viewModel.search()
                                    keyboardController?.hide()
                                }
                            ),
                            decorationBox = { innerTextField ->
                                if (uiState.query.isEmpty()) {
                                    Text(
                                        text = "Anime, dizi veya film ara...",
                                        color = KitsugiColors.TextMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                innerTextField()
                            }
                        )

                        AnimatedVisibility(
                            visible = uiState.query.isNotEmpty(),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.clearQuery()
                                    keyboardController?.hide()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Temizle",
                                    tint = KitsugiColors.TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Search Type Selection (Anime, Manga, TMDB)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isAnimeSelected = !isTmdbPlatform && currentMediaType == MediaType.Anime
                    val isMangaSelected = !isTmdbPlatform && currentMediaType == MediaType.Manga
                    val isTmdbSelected = isTmdbPlatform

                    // Anime Chip
                    SearchTypeChip(
                        label = "Anime",
                        selected = isAnimeSelected,
                        onClick = {
                            viewModel.setPlatform(SearchPlatform.All)
                            viewModel.setMediaType(MediaType.Anime)
                        }
                    )

                    // Manga Chip
                    SearchTypeChip(
                        label = "Manga",
                        selected = isMangaSelected,
                        onClick = {
                            viewModel.setPlatform(SearchPlatform.All)
                            viewModel.setMediaType(MediaType.Manga)
                        }
                    )

                    // TMDB Chip
                    SearchTypeChip(
                        label = "Film & Dizi (TMDB)",
                        selected = isTmdbSelected,
                        onClick = {
                            viewModel.setPlatform(SearchPlatform.TMDB)
                            viewModel.setMediaType(MediaType.Movie)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Filters Scroll Row (Format, Status, Year, etc.)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isTmdbPlatform) {
                        // Source (Platform)
                        val sourceLabel = when (uiState.selectedPlatform) {
                            SearchPlatform.MAL -> "Kaynak: MAL"
                            SearchPlatform.AniList -> "Kaynak: AniList"
                            else -> "Kaynak: Tümü"
                        }
                        SearchFilterChip(
                            label = sourceLabel,
                            selected = uiState.selectedPlatform != SearchPlatform.All,
                            onClick = { openPlatformDialog = true }
                        )

                        // Format
                        val formatLabel = uiState.activeFilters.format?.let { "Format: $it" } ?: "Format"
                        SearchFilterChip(
                            label = formatLabel,
                            selected = uiState.activeFilters.format != null,
                            onClick = { openFormatDialog = true }
                        )

                        // Status
                        val statusLabel = uiState.activeFilters.status?.let { s ->
                            statuses.firstOrNull { it.first == s }?.second?.let { "Durum: $it" } ?: "Durum: $s"
                        } ?: "Durum"
                        SearchFilterChip(
                            label = statusLabel,
                            selected = uiState.activeFilters.status != null,
                            onClick = { openStatusDialog = true }
                        )

                        // Year & Season
                        val dateLabel = if (uiState.activeFilters.minYear != null || uiState.activeFilters.season != null) {
                            val yearPart = uiState.activeFilters.minYear?.toString() ?: ""
                            val seasonPart = when (uiState.activeFilters.season) {
                                "WINTER" -> "Kış"
                                "SPRING" -> "İlkbahar"
                                "SUMMER" -> "Yaz"
                                "FALL" -> "Sonbahar"
                                else -> ""
                            }
                            if (yearPart.isNotEmpty() && seasonPart.isNotEmpty()) "$yearPart - $seasonPart"
                            else yearPart.ifEmpty { seasonPart }
                        } else "Yıl / Sezon"

                        SearchFilterChip(
                            label = dateLabel,
                            selected = uiState.activeFilters.minYear != null || uiState.activeFilters.season != null,
                            onClick = { openDateDialog = true }
                        )

                        // Score
                        val scoreLabel = if (uiState.activeFilters.minScore != null || uiState.activeFilters.maxScore != null) {
                            "Puan: %${uiState.activeFilters.minScore ?: 0} - %${uiState.activeFilters.maxScore ?: 100}"
                        } else "Puan Aralığı"
                        SearchFilterChip(
                            label = scoreLabel,
                            selected = uiState.activeFilters.minScore != null || uiState.activeFilters.maxScore != null,
                            onClick = { openScoreDialog = true }
                        )

                        // Sort
                        val sortLabel = sortOptions.firstOrNull { it.first == uiState.activeFilters.sort }?.second?.let { "Sıralama: $it" } ?: "Sıralama"
                        SearchFilterChip(
                            label = sortLabel,
                            selected = uiState.activeFilters.sort != "POPULARITY_DESC",
                            onClick = { openSortDialog = true }
                        )

                        // Genres/Tags Bottom Sheet Trigger
                        SearchFilterChip(
                            label = "Türler / Etiketler",
                            selected = uiState.activeFilters.genres.isNotEmpty() || uiState.activeFilters.excludedGenres.isNotEmpty() || uiState.activeFilters.tags.isNotEmpty(),
                            onClick = { viewModel.setFilterSheetOpen(true) }
                        )

                    } else {
                        // TMDB Platform Filters
                        val tmdbFormatLabel = if (currentMediaType == MediaType.Movie) "Film" else "Dizi"
                        SearchFilterChip(
                            label = "Tip: $tmdbFormatLabel",
                            selected = true,
                            onClick = { openTmdbFormatDialog = true }
                        )

                        val tmdbGenreLabel = uiState.activeFilters.genres.firstOrNull()?.let { "Tür: $it" }
                            ?: uiState.activeFilters.tags.firstOrNull()?.let { "Etiket: $it" }
                            ?: "Tür Seç"
                        SearchFilterChip(
                            label = tmdbGenreLabel,
                            selected = tmdbGenreLabel != "Tür Seç",
                            onClick = { openTmdbGenreDialog = true }
                        )
                    }

                    // Reset Filters Button
                    if (!uiState.activeFilters.isDefault() || uiState.selectedPlatform != SearchPlatform.All) {
                        TextButton(
                            onClick = {
                                viewModel.resetFilters()
                                viewModel.setPlatform(SearchPlatform.All)
                            }
                        ) {
                            Text("Sıfırla", color = KitsugiColors.AccentRed)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Active Filters Inline Dismissible Chips Row
            if (!uiState.activeFilters.isDefault()) {
                item {
                    ActiveFiltersChipsRow(
                        filters = uiState.activeFilters,
                        onRemoveFilter = { filterType, value ->
                            val current = uiState.activeFilters
                            val updated = when (filterType) {
                                "format" -> current.copy(format = null)
                                "status" -> current.copy(status = null)
                                "season" -> current.copy(season = null)
                                "genre" -> current.copy(genres = current.genres - value)
                                "excludedGenre" -> current.copy(excludedGenres = current.excludedGenres - value)
                                "tag" -> current.copy(tags = current.tags - value)
                                "year" -> current.copy(minYear = null, maxYear = null)
                                "score" -> current.copy(minScore = null, maxScore = null)
                                "sort" -> current.copy(sort = "POPULARITY_DESC")
                                else -> current
                            }
                            viewModel.updateFilters(updated)
                        },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // Search History Section
            if (showIdleContent && uiState.searchHistory.isNotEmpty()) {
                item {
                    SearchHistorySection(
                        history = uiState.searchHistory,
                        onHistoryItemClick = viewModel::applyHistoryItem,
                        onRemoveItem = viewModel::removeHistoryItem,
                        onClearAll = viewModel::clearHistory
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Shimmer Loading State
            if (uiState.isLoading) {
                item {
                    KitsugiShimmerSearchResultList(itemCount = 4)
                }
            }

            // Empty / Error State
            if (!uiState.isLoading && uiState.hasSearched && uiState.errorMessage != null) {
                item {
                    KitsugiEmptyState(
                        title = "Sonuç bulunamadı",
                        subtitle = uiState.errorMessage,
                        icon = Icons.Rounded.SearchOff
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Search Results List
            val filteredResults = uiState.results.filter { showAdultContent || !it.isAdult }

            items(filteredResults) { result ->
                SearchResultRow(
                    result = result,
                    alreadyInList = isAlreadyInList(result),
                    mediaEntry = getMediaEntry(result),
                    onItemClick = { onOpenApiDetail(result) },
                    onAddClick = {
                        onAddSelectionToList(ApiSearchSelection(result = result, synopsis = null))
                    },
                    titleLanguage = titleLanguage,
                    scoreFormat = scoreFormat,
                    hideScores = hideScores
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }

        // Scroll to Top FAB
        AnimatedVisibility(
            visible = showFab,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                },
                containerColor = accentColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(54.dp)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Yukarı Çık")
            }
        }
    }

    // Genres Bottom Sheet Dialog
    if (uiState.isFilterSheetOpen) {
        GenresTagsSheet(
            currentFilters = uiState.activeFilters,
            onApplyFilters = {
                viewModel.updateFilters(it)
                viewModel.setFilterSheetOpen(false)
            },
            onDismiss = { viewModel.setFilterSheetOpen(false) }
        )
    }

    // Dialog: Platform selection
    if (openPlatformDialog) {
        val platforms = listOf(SearchPlatform.All, SearchPlatform.MAL, SearchPlatform.AniList)
        DialogWithRadioSelection(
            title = "Kaynak Platform Seç",
            options = platforms,
            selectedOption = uiState.selectedPlatform,
            onOptionSelected = { plat ->
                if (plat != null) viewModel.setPlatform(plat)
            },
            onDismiss = { openPlatformDialog = false },
            optionLabel = { it.label }
        )
    }

    // Dialog: Format selection
    if (openFormatDialog) {
        DialogWithRadioSelection(
            title = "Format Seç",
            options = formats,
            selectedOption = uiState.activeFilters.format,
            onOptionSelected = { f ->
                viewModel.updateFilters(uiState.activeFilters.copy(format = f))
            },
            onDismiss = { openFormatDialog = false }
        )
    }

    // Dialog: Status selection
    if (openStatusDialog) {
        DialogWithRadioSelection(
            title = "Yayın Durumu Seç",
            options = statuses,
            selectedOption = statuses.firstOrNull { it.first == uiState.activeFilters.status },
            onOptionSelected = { s ->
                viewModel.updateFilters(uiState.activeFilters.copy(status = s?.first))
            },
            onDismiss = { openStatusDialog = false },
            optionLabel = { it.second }
        )
    }

    // Dialog: Year & Season selection
    if (openDateDialog) {
        DialogWithYearSeasonSelection(
            title = "Tarih ve Sezon Seç",
            selectedStartYear = uiState.activeFilters.minYear,
            selectedEndYear = uiState.activeFilters.maxYear,
            selectedSeason = uiState.activeFilters.season,
            onStartYearSelected = { y ->
                viewModel.updateFilters(uiState.activeFilters.copy(minYear = y))
            },
            onEndYearSelected = { y ->
                viewModel.updateFilters(uiState.activeFilters.copy(maxYear = y))
            },
            onSeasonSelected = { s ->
                viewModel.updateFilters(uiState.activeFilters.copy(season = s))
            },
            onDismiss = { openDateDialog = false }
        )
    }

    // Dialog: Score range selection
    if (openScoreDialog) {
        DialogWithScoreRangeSelection(
            title = "Puan Aralığı Seç",
            selectedMinScore = uiState.activeFilters.minScore,
            selectedMaxScore = uiState.activeFilters.maxScore,
            onScoreRangeSelected = { min, max ->
                viewModel.updateFilters(uiState.activeFilters.copy(minScore = min, maxScore = max))
            },
            onDismiss = { openScoreDialog = false }
        )
    }

    // Dialog: Sort selection
    if (openSortDialog) {
        DialogWithRadioSelection(
            title = "Sıralama Seç",
            options = sortOptions,
            selectedOption = sortOptions.firstOrNull { it.first == uiState.activeFilters.sort },
            onOptionSelected = { s ->
                viewModel.updateFilters(uiState.activeFilters.copy(sort = s?.first))
            },
            onDismiss = { openSortDialog = false },
            optionLabel = { it.second }
        )
    }

    // Dialog: TMDB Format Selection
    if (openTmdbFormatDialog) {
        val tmdbTypes = listOf(MediaType.Movie, MediaType.TvShow)
        DialogWithRadioSelection(
            title = "Tip Seç",
            options = tmdbTypes,
            selectedOption = currentMediaType,
            onOptionSelected = { mt ->
                if (mt != null) viewModel.setMediaType(mt)
            },
            onDismiss = { openTmdbFormatDialog = false },
            optionLabel = { if (it == MediaType.Movie) "Film" else "Dizi" }
        )
    }

    // Dialog: TMDB Genre Selection
    if (openTmdbGenreDialog) {
        val currentGenre = uiState.activeFilters.genres.firstOrNull() ?: "Tümü"
        DialogWithRadioSelection(
            title = "Tür Seç",
            options = tmdbGenres,
            selectedOption = currentGenre,
            onOptionSelected = { g ->
                val updated = if (g == null || g == "Tümü") {
                    uiState.activeFilters.copy(genres = emptyList())
                } else {
                    uiState.activeFilters.copy(genres = listOf(g))
                }
                viewModel.updateFilters(updated)
            },
            onDismiss = { openTmdbGenreDialog = false }
        )
    }
}

@Composable
fun SearchTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) accentColor else KitsugiColors.Surface)
            .border(1.dp, if (selected) Color.Transparent else KitsugiColors.Border, shape)
            .tvClickable(shape = shape, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else KitsugiColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun SearchFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) accentColor.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, if (selected) accentColor.copy(alpha = 0.5f) else KitsugiColors.Border, shape)
            .tvClickable(shape = shape, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) accentColor else KitsugiColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ─── Active Filters Chips Row ─────────────────────────────────────────────────

@Composable
fun ActiveFiltersChipsRow(
    filters: SearchFilters,
    onRemoveFilter: (filterType: String, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (filters.format != null) {
            ActiveFilterChip(label = "Format: ${filters.format}", onCloseClick = { onRemoveFilter("format", "") })
        }
        if (filters.status != null) {
            val lbl = when (filters.status) {
                "AIRING" -> "Yayında"; "FINISHED" -> "Tamamlandı"; "UPCOMING" -> "Yakında"
                "PUBLISHING" -> "Yayınlanıyor"; "HIATUS" -> "Ara"; "DISCONTINUED" -> "Durduruldu"
                else -> filters.status
            }
            ActiveFilterChip(label = "Durum: $lbl", onCloseClick = { onRemoveFilter("status", "") })
        }
        if (filters.season != null) {
            val lbl = when (filters.season) {
                "WINTER" -> "Kış"; "SPRING" -> "İlkbahar"; "SUMMER" -> "Yaz"; "FALL" -> "Sonbahar"
                else -> filters.season
            }
            ActiveFilterChip(label = "Sezon: $lbl", onCloseClick = { onRemoveFilter("season", "") })
        }
        filters.genres.forEach { g ->
            ActiveFilterChip(label = "+ $g", onCloseClick = { onRemoveFilter("genre", g) }, borderColor = Color(0xFF10B981))
        }
        filters.excludedGenres.forEach { g ->
            ActiveFilterChip(label = "- $g", onCloseClick = { onRemoveFilter("excludedGenre", g) }, borderColor = Color(0xFFEF4444))
        }
        filters.tags.forEach { t ->
            ActiveFilterChip(label = "# $t", onCloseClick = { onRemoveFilter("tag", t) })
        }
        if (filters.minYear != null || filters.maxYear != null) {
            ActiveFilterChip(label = "Yıl: ${filters.minYear ?: 1970}-${filters.maxYear ?: 2026}", onCloseClick = { onRemoveFilter("year", "") })
        }
        if (filters.minScore != null || filters.maxScore != null) {
            ActiveFilterChip(label = "Puan: %${filters.minScore ?: 0}-%${filters.maxScore ?: 100}", onCloseClick = { onRemoveFilter("score", "") })
        }
        if (filters.sort != null && filters.sort != "POPULARITY_DESC") {
            val lbl = when (filters.sort) {
                "SCORE_DESC" -> "Puan"; "TITLE_ROMAJI_ASC" -> "A-Z"; "TITLE_ROMAJI_DESC" -> "Z-A"; else -> filters.sort
            }
            ActiveFilterChip(label = "Sıra: $lbl", onCloseClick = { onRemoveFilter("sort", "") })
        }
    }
}

@Composable
fun ActiveFilterChip(
    label: String,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = LocalKitsugiAccent.current
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(KitsugiColors.Surface.copy(alpha = 0.6f))
            .border(1.dp, borderColor.copy(alpha = 0.5f), shape)
            .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, color = KitsugiColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .tvClickable(shape = RoundedCornerShape(8.dp), onClick = onCloseClick)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Temizle",
                tint = KitsugiColors.TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
