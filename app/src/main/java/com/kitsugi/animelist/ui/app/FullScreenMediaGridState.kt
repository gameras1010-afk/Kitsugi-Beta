package com.kitsugi.animelist.ui.app

import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.screens.explore.ExploreCategoryType
import com.kitsugi.animelist.ui.screens.explore.ExplorePlatform

data class FullScreenMediaGridState(
    val title: String,
    val categoryType: ExploreCategoryType,
    val platform: ExplorePlatform,
    val initialResults: List<JikanSearchResult>
)