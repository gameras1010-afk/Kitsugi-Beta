@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.kitsugi.animelist.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Alignment
import com.kitsugi.animelist.ui.theme.LocalIsTv
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.matches
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.ui.components.KitsugiEmptyState
import com.kitsugi.animelist.ui.components.KitsugiShimmerSearchResultList
import com.kitsugi.animelist.ui.theme.KitsugiColors
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll

/**
 * Yeni Arama Ekranı — bağımsız alt sekmesi.
 * AniHyou SearchView.kt ve MoeList SearchViewModel.kt'den ilham alınarak
 * Kitsugi'nun modüler bileşen mimarisinde yazılmıştır.
 *
 * Bileşenler:
 * - SearchBarSection     → Arama çubuğu (X temizle + Ara butonu)
 * - SearchFilterBar      → Anime/Manga ve MAL/AniList/Tümü filtre çipleri
 * - SearchHistorySection → Arama geçmişi (kalıcı, platform etiketli)
 * - SearchGenreChips     → Popüler tür çipleri (AniHyou'dan ilham)
 * - Sonuç listesi        → SearchResultRow bileşenleri (kaynak platform etiketli)
 */
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

    fun isAlreadyInList(result: JikanSearchResult): Boolean {
        return currentEntries.any { entry ->
            entry.matches(result)
        }
    }

    // Bir türe tıklandığında: query'yi set et → aramayı tetikle
    fun onGenreChipClick(genre: String) {
        viewModel.setQuery(genre)
        viewModel.search()
    }

    val showIdleContent = !uiState.hasSearched && !uiState.isLoading
    val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current
    val lazyListState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background),
        contentAlignment = Alignment.TopCenter
    ) {
        val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
        CompositionLocalProvider(
            LocalBringIntoViewSpec provides if (isTvDevice) tvSpec else LocalBringIntoViewSpec.current
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .then(if (isTvDevice) Modifier.dpadVerticalFastScroll(lazyListState) else Modifier)
            ) {
        // ── Başlık ──
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Arama",
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // ── Arama Çubuğu (X + Ara butonlu) ──
        item {
            SearchBarSection(
                query = uiState.query,
                onQueryChange = viewModel::setQuery,
                onSearch = viewModel::search,
                onClearQuery = viewModel::clearQuery,
                onFilterClick = { viewModel.setFilterSheetOpen(true) },
                hasActiveFilters = !uiState.activeFilters.isDefault()
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ── Filtre Çipleri ──
        item {
            SearchFilterBar(
                selectedMediaType = uiState.selectedMediaType,
                selectedPlatform = uiState.selectedPlatform,
                onMediaTypeChange = viewModel::setMediaType,
                onPlatformChange = viewModel::setPlatform
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

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
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }

        // ── Arama Geçmişi (boştayken, arama yapılmamışsa göster) ──
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

        // ── Popüler Türler (boştayken göster) ──
        if (showIdleContent) {
            item {
                SearchGenreChips(
                    onGenreClick = ::onGenreChipClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ── Yükleniyor ──
        if (uiState.isLoading) {
            item {
                KitsugiShimmerSearchResultList(itemCount = 4)
            }
        }

        // ── Hata / Boş sonuç mesajı ──
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

        // ── Arama Sonuçları ──
        val filteredResults = uiState.results.filter { showAdultContent || !it.isAdult }

        items(filteredResults) { result ->
            SearchResultRow(
                result = result,
                alreadyInList = isAlreadyInList(result),
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
        }

        if (uiState.isFilterSheetOpen) {
            SearchFilterSheet(
                mediaType = uiState.selectedMediaType,
                currentFilters = uiState.activeFilters,
                onDismiss = { viewModel.setFilterSheetOpen(false) },
                onApplyFilters = viewModel::updateFilters,
                onResetFilters = viewModel::resetFilters
            )
        }
    }
}

@Composable
fun ActiveFiltersChipsRow(
    filters: SearchFilters,
    onRemoveFilter: (filterType: String, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rowScrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rowScrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Format
        if (filters.format != null) {
            ActiveFilterChip(
                label = "Format: ${filters.format}",
                onCloseClick = { onRemoveFilter("format", "") }
            )
        }

        // Status
        if (filters.status != null) {
            val statusLabel = when (filters.status) {
                "AIRING" -> "Yayında"
                "FINISHED" -> "Tamamlandı"
                "UPCOMING" -> "Yakında"
                "PUBLISHING" -> "Yayınlanıyor"
                "HIATUS" -> "Ara Verildi"
                "DISCONTINUED" -> "Durduruldu"
                else -> filters.status
            }
            ActiveFilterChip(
                label = "Durum: $statusLabel",
                onCloseClick = { onRemoveFilter("status", "") }
            )
        }

        // Season
        if (filters.season != null) {
            val seasonLabel = when (filters.season) {
                "WINTER" -> "Kış"
                "SPRING" -> "İlkbahar"
                "SUMMER" -> "Yaz"
                "FALL" -> "Sonbahar"
                else -> filters.season
            }
            ActiveFilterChip(
                label = "Sezon: $seasonLabel",
                onCloseClick = { onRemoveFilter("season", "") }
            )
        }

        // Genres (Included)
        filters.genres.forEach { genre ->
            ActiveFilterChip(
                label = "+ $genre",
                onCloseClick = { onRemoveFilter("genre", genre) },
                borderColor = Color(0xFF10B981)
            )
        }

        // Excluded Genres
        filters.excludedGenres.forEach { genre ->
            ActiveFilterChip(
                label = "- $genre",
                onCloseClick = { onRemoveFilter("excludedGenre", genre) },
                borderColor = Color(0xFFEF4444)
            )
        }

        // Tags
        filters.tags.forEach { tag ->
            ActiveFilterChip(
                label = "# $tag",
                onCloseClick = { onRemoveFilter("tag", tag) }
            )
        }

        // Year Range
        if (filters.minYear != null || filters.maxYear != null) {
            val yearText = "${filters.minYear ?: 1970} - ${filters.maxYear ?: 2026}"
            ActiveFilterChip(
                label = "Yıl: $yearText",
                onCloseClick = { onRemoveFilter("year", "") }
            )
        }

        // Score Range
        if (filters.minScore != null || filters.maxScore != null) {
            val scoreText = "%${filters.minScore ?: 0} - %${filters.maxScore ?: 100}"
            ActiveFilterChip(
                label = "Puan: $scoreText",
                onCloseClick = { onRemoveFilter("score", "") }
            )
        }

        // Sort
        if (filters.sort != "POPULARITY_DESC" && filters.sort != null) {
            val sortLabel = when (filters.sort) {
                "SCORE_DESC" -> "Puan"
                "TITLE_ROMAJI_ASC" -> "İsim (A-Z)"
                "TITLE_ROMAJI_DESC" -> "İsim (Z-A)"
                else -> filters.sort
            }
            ActiveFilterChip(
                label = "Sıralama: $sortLabel",
                onCloseClick = { onRemoveFilter("sort", "") }
            )
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
        Text(
            text = label,
            color = KitsugiColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
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
