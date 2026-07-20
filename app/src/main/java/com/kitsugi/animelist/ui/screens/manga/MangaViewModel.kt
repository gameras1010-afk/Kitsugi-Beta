package com.kitsugi.animelist.ui.screens.manga

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MangaViewModel : ViewModel() {

    private val apiClient = JikanApiClient()

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var topManga by mutableStateOf<List<JikanSearchResult>>(emptyList())
        private set

    var publishingManga by mutableStateOf<List<JikanSearchResult>>(emptyList())
        private set

    var completedManga by mutableStateOf<List<JikanSearchResult>>(emptyList())
        private set

    val isDataLoaded: Boolean
        get() = topManga.isNotEmpty() || publishingManga.isNotEmpty()

    init {
        loadData()
    }

    fun loadData(forceRefresh: Boolean = false) {
        if (isLoading) return
        if (isDataLoaded && !forceRefresh) return

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            runCatching {
                val topDeferred = async { apiClient.topManga() }
                val publishingDeferred = async { apiClient.publishingManga() }
                val completedDeferred = async { apiClient.completedManga() }

                Triple(
                    topDeferred.await(),
                    publishingDeferred.await(),
                    completedDeferred.await()
                )
            }.onSuccess { result ->
                topManga = result.first
                publishingManga = result.second
                completedManga = result.third
            }.onFailure { error ->
                errorMessage = error.message ?: "Manga listeleri alınamadı."
            }

            isLoading = false
        }
    }
}
