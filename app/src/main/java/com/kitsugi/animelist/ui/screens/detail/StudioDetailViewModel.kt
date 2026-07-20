package com.kitsugi.animelist.ui.screens.detail

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.remote.DetailCache
import com.kitsugi.animelist.data.remote.JikanApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudioDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val apiClient = JikanApiClient()
    private val TAG = "StudioDetailVM"

    private val _state = MutableStateFlow<StudioDetailState>(StudioDetailState.Loading)
    val state: StateFlow<StudioDetailState> = _state.asStateFlow()

    private var currentFetchKey: String? = null
    private var lastStudioId: Int = 0
    private var lastSource: String = ""
    private var lastStudioName: String? = null

    fun loadStudio(studioId: Int, source: String, name: String? = null) {
        val newKey = "$source:$studioId"
        if (newKey == currentFetchKey) {
            Log.d(TAG, "loadStudio: Cache hit for key=$newKey — skipping")
            return
        }

        Log.d(TAG, "loadStudio: New key=$newKey (was $currentFetchKey)")
        currentFetchKey = newKey
        lastStudioId = studioId
        lastSource = source
        lastStudioName = name

        val cachedStudioDetail = DetailCache.getStudioDetail(source, studioId)
        _state.value = if (cachedStudioDetail != null) StudioDetailState.Success(cachedStudioDetail) else StudioDetailState.Loading

        viewModelScope.launch {
            fetchStudioDetail(studioId, source, name)
        }
    }

    fun retry() {
        val studioId = lastStudioId
        val source = lastSource
        val name = lastStudioName
        if (studioId > 0 && source.isNotEmpty()) {
            _state.value = StudioDetailState.Loading
            viewModelScope.launch {
                fetchStudioDetail(studioId, source, name, force = true)
            }
        }
    }

    private suspend fun fetchStudioDetail(studioId: Int, source: String, name: String? = null, force: Boolean = false) {
        val cached = DetailCache.getStudioDetail(source, studioId)
        val detail = if (cached != null && !force) {
            cached
        } else {
            val fetched = withContext(Dispatchers.IO) {
                apiClient.fetchStudioDetail(source, studioId, name)
            }
            if (fetched != null) {
                DetailCache.putStudioDetail(source, studioId, fetched)
            }
            fetched
        }

        if (detail != null) {
            _state.value = StudioDetailState.Success(detail)
        } else {
            _state.value = StudioDetailState.Error("Stüdyo detayları yüklenemedi.")
        }
    }
}
