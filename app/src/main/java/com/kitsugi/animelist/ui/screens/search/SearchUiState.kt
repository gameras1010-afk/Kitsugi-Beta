package com.kitsugi.animelist.ui.screens.search

import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.model.MediaType

/**
 * Search ekranının UI durumu.
 * MoeList SearchUiState.kt'den ilham alınarak Kitsugi'nun JikanSearchResult modeline uyarlanmıştır.
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
    val activeFilters: SearchFilters = SearchFilters(),
    val isFilterSheetOpen: Boolean = false
)

data class SearchHistoryItem(
    val query: String,
    val platform: SearchPlatform,
    val mediaType: MediaType
)

enum class SearchPlatform(val label: String) {
    All("Tümü"),
    MAL("MAL"),
    AniList("AniList"),
    TMDB("TMDB")
}

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
