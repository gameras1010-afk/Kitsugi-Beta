package com.kitsugi.animelist.ui.screens.detail

sealed class DetailTabState<out T> {
    data object Loading : DetailTabState<Nothing>()
    data object Error : DetailTabState<Nothing>()
    data class Success<out T>(val data: T) : DetailTabState<T>()
}
