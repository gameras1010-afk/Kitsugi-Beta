package com.kitsugi.animelist.ui.app

import com.lagradost.cloudstream3.SearchResponse

/**
 * Navigation state for the addon-specific full-screen grid page.
 *
 * Unlike [FullScreenMediaGridState] which uses JikanSearchResult + Jikan/AniList/TMDB pagination,
 * this state holds Cloudstream [SearchResponse] items and the data needed to paginate via
 * [com.lagradost.cloudstream3.MainAPI.getMainPage].
 */
data class AddonFullScreenGridState(
    /** Category title shown in the header (e.g. "Yeni Bölümler") */
    val title: String,
    /** Exact name of the MainAPI provider that owns this category */
    val apiName: String,
    /** Items already loaded from the home feed row — shown immediately without a loading spinner */
    val initialItems: List<SearchResponse>,
    /** [com.lagradost.cloudstream3.MainPageData.data] — used to paginate via getMainPage() */
    val mainPageData: String,
    /** Whether the category uses horizontal (wide) poster images */
    val horizontalImages: Boolean = false
)
