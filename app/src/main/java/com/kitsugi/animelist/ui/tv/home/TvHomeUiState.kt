package com.kitsugi.animelist.ui.tv.home

import com.kitsugi.animelist.data.remote.JikanSearchResult

sealed interface TvHomeUiState {
    object Loading : TvHomeUiState
    data class Error(val message: String) : TvHomeUiState
    data class Success(
        val selectedPlatform: String,
        val heroItems: List<JikanSearchResult>,
        val activeHeroItem: JikanSearchResult?
    ) : TvHomeUiState
}
