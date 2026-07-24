package com.kitsugi.animelist.ui.screens.detail

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import com.kitsugi.animelist.data.remote.DetailCache
import com.kitsugi.animelist.data.remote.GalleryCategory
import com.kitsugi.animelist.data.remote.GalleryItem
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.KitsugiMediaMutationsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudioDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val apiClient = JikanApiClient()
    private val mutationsClient = KitsugiMediaMutationsClient()
    private val TAG = "StudioDetailVM"

    private val _state = MutableStateFlow<StudioDetailState>(StudioDetailState.Loading)
    val state: StateFlow<StudioDetailState> = _state.asStateFlow()

    private val _galleryItems = MutableStateFlow<List<GalleryItem>>(emptyList())
    val galleryItems: StateFlow<List<GalleryItem>> = _galleryItems.asStateFlow()

    private val _isFavourite = MutableStateFlow(false)
    val isFavourite: StateFlow<Boolean> = _isFavourite.asStateFlow()

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
        if (cachedStudioDetail != null) _isFavourite.value = cachedStudioDetail.isFavourite

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
            _isFavourite.value = detail.isFavourite

            // Build gallery from studio imageUrl (logo)
            val imageUrl = detail.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                val category = if (imageUrl.contains("logo", ignoreCase = true) ||
                    imageUrl.contains("image.tmdb.org", ignoreCase = true)) {
                    GalleryCategory.LOGO
                } else {
                    GalleryCategory.POSTER
                }
                val src = if (imageUrl.contains("image.tmdb.org") || imageUrl.contains("tmdb.org")) "TMDB" else "Jikan"
                _galleryItems.value = listOf(GalleryItem(url = imageUrl, source = src, category = category))
            } else {
                _galleryItems.value = emptyList()
            }
        } else {
            _state.value = StudioDetailState.Error("Stüdyo detayları yüklenemedi.")
        }
    }

    /**
     * AniList stüdyo favori toggle — giriş yapılmış ve aniListId biliniyorsa çalışır.
     */
    fun toggleFavourite() {
        ExternalAuthManager.getAniListToken(context) ?: return
        val currentState = _state.value as? StudioDetailState.Success ?: return
        val detail = currentState.detail
        val targetId = detail.aniListId ?: if (lastSource.lowercase() == "anilist") detail.id else null
        if (targetId == null || targetId <= 0) return

        val newFav = !_isFavourite.value
        _isFavourite.value = newFav
        val updated = detail.copy(isFavourite = newFav)
        _state.value = StudioDetailState.Success(updated)
        DetailCache.putStudioDetail(lastSource, lastStudioId, updated)

        viewModelScope.launch(Dispatchers.IO) {
            val ok = mutationsClient.toggleFavourite("studio", targetId)
            if (!ok) {
                _isFavourite.value = !newFav
                val rolled = detail.copy(isFavourite = !newFav)
                _state.value = StudioDetailState.Success(rolled)
                DetailCache.putStudioDetail(lastSource, lastStudioId, rolled)
            }
        }
    }
}
