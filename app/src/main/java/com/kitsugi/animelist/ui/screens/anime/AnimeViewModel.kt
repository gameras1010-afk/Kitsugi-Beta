package com.kitsugi.animelist.ui.screens.anime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AnimeViewModel : ViewModel() {

    private val apiClient = JikanApiClient()

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var topAnime by mutableStateOf<List<JikanSearchResult>>(emptyList())
        private set

    var airingAnime by mutableStateOf<List<JikanSearchResult>>(emptyList())
        private set

    var upcomingAnime by mutableStateOf<List<JikanSearchResult>>(emptyList())
        private set

    val isDataLoaded: Boolean
        get() = topAnime.isNotEmpty() || airingAnime.isNotEmpty()

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
                val topDeferred = async { apiClient.topAnime() }
                val airingDeferred = async { apiClient.airingAnime() }
                val upcomingDeferred = async { apiClient.upcomingAnime() }

                Triple(
                    topDeferred.await(),
                    airingDeferred.await(),
                    upcomingDeferred.await()
                )
            }.onSuccess { result ->
                topAnime = result.first
                airingAnime = result.second
                upcomingAnime = result.third
            }.onFailure { error ->
                errorMessage = error.message ?: "Anime listeleri alınamadı."
            }

            isLoading = false
        }
    }
}
