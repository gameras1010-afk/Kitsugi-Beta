package com.kitsugi.animelist.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.annotation.StringRes
import com.kitsugi.animelist.R

enum class MainTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    Explore(R.string.tab_explore, Icons.Rounded.Explore),
    MyList(R.string.tab_mylist, Icons.AutoMirrored.Rounded.ListAlt),
    Search(R.string.tab_search, Icons.Rounded.Search),
    Settings(R.string.tab_settings, Icons.Rounded.Settings)
}