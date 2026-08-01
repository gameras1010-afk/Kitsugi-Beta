package com.kitsugi.animelist.ui.screens.search

import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.screens.search.composables.KitsugiCountryOfOrigin
import com.kitsugi.animelist.ui.screens.search.composables.KitsugiMediaFormat
import com.kitsugi.animelist.ui.screens.search.composables.KitsugiMediaSeason
import com.kitsugi.animelist.ui.screens.search.composables.KitsugiMediaSortSearch
import com.kitsugi.animelist.ui.screens.search.composables.KitsugiMediaSource
import com.kitsugi.animelist.ui.screens.search.composables.KitsugiMediaStatus

/**
 * Search ekranının UI durumu.
 * AniHyou SearchUiState.kt mimarisine uyarlanmıştır.
 * Tüm AniHyou filtre alanları (format, status, tarih, sezon, ülke, kaynak, puan, bölüm/süre)
 * doğrudan bu state'e taşınmıştır.
 */
data class SearchUiState(
    val query: String = "",
    val selectedMediaType: MediaType = MediaType.Anime,
    val selectedPlatform: SearchPlatform = SearchPlatform.All,
    val results: List<JikanSearchResult> = emptyList(),
    val searchHistory: List<SearchHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null,
    val isFilterSheetOpen: Boolean = false,

    // ── Genre / Tag filters (used by GenresTagsSheet) ──────────────────────
    val genres: List<String> = emptyList(),
    val excludedGenres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),

    // ── AniHyou-style chip filters (AniList-only) ──────────────────────────
    val selectedFormats: List<KitsugiMediaFormat> = emptyList(),
    val selectedStatuses: List<KitsugiMediaStatus> = emptyList(),
    val country: KitsugiCountryOfOrigin? = null,
    val selectedSources: List<KitsugiMediaSource> = emptyList(),

    // ── Date / Season ──────────────────────────────────────────────────────
    val startYear: Int? = null,
    val endYear: Int? = null,
    val season: KitsugiMediaSeason? = null,

    // ── Score / Episode / Duration ranges ──────────────────────────────────
    val minScore: Int? = null,
    val maxScore: Int? = null,
    val minEpCh: Int? = null,
    val maxEpCh: Int? = null,
    val minDuration: Int? = null,
    val maxDuration: Int? = null,

    // ── Sort ───────────────────────────────────────────────────────────────
    val sortSearch: KitsugiMediaSortSearch = KitsugiMediaSortSearch.SEARCH_MATCH,
    val isSortDescending: Boolean = true,
) {
    /** Effective AniList sort string (e.g. "POPULARITY_DESC") */
    val effectiveSortApiValue: String get() =
        if (isSortDescending) sortSearch.descApiValue else sortSearch.ascApiValue

    /** True if any filter besides the default sort is active */
    val hasFiltersApplied: Boolean get() =
        genres.isNotEmpty() ||
        excludedGenres.isNotEmpty() ||
        tags.isNotEmpty() ||
        selectedFormats.isNotEmpty() ||
        selectedStatuses.isNotEmpty() ||
        country != null ||
        selectedSources.isNotEmpty() ||
        startYear != null ||
        endYear != null ||
        season != null ||
        minScore != null ||
        maxScore != null ||
        minEpCh != null ||
        maxEpCh != null ||
        minDuration != null ||
        maxDuration != null

    /** Returns the legacy SearchFilters object for backward-compatible ViewModel code. */
    fun toLegacyFilters(): SearchFilters = SearchFilters(
        format = selectedFormats.firstOrNull()?.apiValue,
        status = selectedStatuses.firstOrNull()?.apiValue,
        genres = genres,
        excludedGenres = excludedGenres,
        tags = tags,
        minYear = startYear,
        maxYear = endYear,
        season = season?.apiValue,
        minScore = minScore,
        maxScore = maxScore,
        sort = effectiveSortApiValue
    )
}

data class SearchHistoryItem(
    val query: String,
    val platform: SearchPlatform,
    val mediaType: MediaType
)

enum class SearchPlatform(val label: String) {
    All("Tümü"),
    MAL("MAL"),
    AniList("AniList"),
    TMDB("TMDB"),
    CS3("Eklentiler")
}

/**
 * Legacy filter bag kept for backward compatibility with JikanApiClient
 * and the existing executeSearchForQuery logic.
 */
data class SearchFilters(
    val format: String? = null,
    val status: String? = null,
    val genres: List<String> = emptyList(),
    val excludedGenres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val season: String? = null,
    val minScore: Int? = null,
    val maxScore: Int? = null,
    val sort: String? = "POPULARITY_DESC"
) {
    fun isDefault(): Boolean =
        format == null &&
        status == null &&
        genres.isEmpty() &&
        excludedGenres.isEmpty() &&
        tags.isEmpty() &&
        minYear == null &&
        maxYear == null &&
        season == null &&
        minScore == null &&
        maxScore == null &&
        sort == "POPULARITY_DESC"
}
