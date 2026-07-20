package com.kitsugi.animelist.ui.screens.home.components

/**
 * FP-24 – Actions available for media items in the local library lists.
 */
sealed interface LibraryActions {
    data class AddToList(val mediaId: Int, val status: String) : LibraryActions
    data class RemoveFromList(val mediaId: Int) : LibraryActions
    data class ToggleFavorite(val mediaId: Int) : LibraryActions
    data class UpdateProgress(val mediaId: Int, val progress: Int) : LibraryActions
}
