package com.kitsugi.animelist.ui.screens.stream

import com.kitsugi.animelist.data.repository.StreamSource

/**
 * Tracks the loading / result state of a single addon in the stream selection UI.
 */
data class AddonFetchState(
    val addonName: String,
    val manifestUrl: String,
    val isLoading: Boolean = true,
    val streams: List<StreamSource> = emptyList(),
    val error: String? = null
)

/**
 * Holds a pending play action when the user has "ask" engine set and needs to choose.
 */
data class PendingPlayAction(
    val source: StreamSource,
    val resolvedUrl: String,
    val streamKey: String
)

/**
 * Represents the Debrid cache state of a given stream source.
 */
enum class DebridCacheState {
    CACHED,
    NOT_CACHED,
    P2P
}
