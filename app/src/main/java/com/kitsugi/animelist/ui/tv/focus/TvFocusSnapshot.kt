package com.kitsugi.animelist.ui.tv.focus

import androidx.compose.runtime.Immutable

/**
 * Stores generic focus and scroll state for a TV screen to enable proper state restoration.
 */
@Immutable
data class TvFocusSnapshot(
    val focusedItemKey: String? = null,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
    val metadata: Map<String, String> = emptyMap()
)
