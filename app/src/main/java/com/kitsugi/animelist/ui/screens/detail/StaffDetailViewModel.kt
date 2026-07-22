package com.kitsugi.animelist.ui.screens.detail

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import com.kitsugi.animelist.data.local.TranslationManager
import com.kitsugi.animelist.data.remote.DetailCache
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.KitsugiMediaMutationsClient
import com.kitsugi.animelist.data.settings.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StaffDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val apiClient = JikanApiClient()
    private val mutationsClient = KitsugiMediaMutationsClient()
    private val translationManager = TranslationManager(context)
    private val settingsDataStore = SettingsDataStore(context)
    private val TAG = "StaffDetailVM"

    private val _state = MutableStateFlow<StaffDetailState>(StaffDetailState.Loading)
    val state: StateFlow<StaffDetailState> = _state.asStateFlow()

    private val _translatedBio = MutableStateFlow<String?>(null)
    val translatedBio: StateFlow<String?> = _translatedBio.asStateFlow()

    private val _isFavourite = MutableStateFlow(false)
    val isFavourite: StateFlow<Boolean> = _isFavourite.asStateFlow()

    private var currentFetchKey: String? = null
    private var lastStaffId: Int = 0
    private var lastSource: String = ""
    private var lastStaffName: String? = null

    fun loadStaff(staffId: Int, source: String, name: String? = null) {
        val newKey = "$source:$staffId"
        if (newKey == currentFetchKey) {
            Log.d(TAG, "loadStaff: Cache hit for key=$newKey — skipping")
            return
        }

        Log.d(TAG, "loadStaff: New key=$newKey (was $currentFetchKey)")
        currentFetchKey = newKey
        lastStaffId = staffId
        lastSource = source
        lastStaffName = name

        val cachedStaffDetail = DetailCache.getStaffDetail(source, staffId)
        val cachedBioTranslation = DetailCache.getTranslation("bio_staff", source, staffId)

        _state.value = if (cachedStaffDetail != null) StaffDetailState.Success(cachedStaffDetail) else StaffDetailState.Loading
        _translatedBio.value = cachedBioTranslation

        viewModelScope.launch {
            fetchStaffDetail(staffId, source, name)
        }
    }

    fun retry() {
        val staffId = lastStaffId
        val source = lastSource
        val name = lastStaffName
        if (staffId > 0 && source.isNotEmpty()) {
            _state.value = StaffDetailState.Loading
            _translatedBio.value = null
            viewModelScope.launch {
                fetchStaffDetail(staffId, source, name, force = true)
            }
        }
    }

    private suspend fun fetchStaffDetail(staffId: Int, source: String, name: String? = null, force: Boolean = false) {
        val cached = DetailCache.getStaffDetail(source, staffId)
        val detail = if (cached != null && !force) {
            cached
        } else {
            val fetched = withContext(Dispatchers.IO) {
                apiClient.fetchStaffDetail(source, staffId, name)
            }
            if (fetched != null) {
                DetailCache.putStaffDetail(source, staffId, fetched)
            }
            fetched
        }

        if (detail != null) {
            _state.value = StaffDetailState.Success(detail)
            _isFavourite.value = detail.isFavourite

            // Otomatik çeviri açıksa biyografiyi çevir (zaten Türkçeyse TranslationManager atlar)
            val bio = detail.biography
            val autoTranslate = runCatching { settingsDataStore.settingsFlow.first() }.getOrNull()?.autoTranslateEnabled ?: false
            val cachedTranslation = DetailCache.getTranslation("bio_staff", source, staffId)
            if (cachedTranslation != null && !force) {
                _translatedBio.value = cachedTranslation
            } else if (!bio.isNullOrBlank() && autoTranslate) {
                val tr = withContext(Dispatchers.IO) {
                    translationManager.translateToTurkish(bio)
                }
                if (tr != bio) {
                    DetailCache.putTranslation("bio_staff", source, staffId, tr)
                    _translatedBio.value = tr
                }
            }
        } else {
            _state.value = StaffDetailState.Error("Ekip üyesi detayları yüklenemedi.")
        }
    }

    /**
     * AniList ekip üyesi favori toggle — giriş yapılmış ve aniListId biliniyorsa çalışır.
     */
    fun toggleFavourite() {
        ExternalAuthManager.getAniListToken(context) ?: return
        val currentState = _state.value as? StaffDetailState.Success ?: return
        val detail = currentState.detail
        val targetId = detail.aniListId ?: if (lastSource.lowercase() == "anilist") detail.id else null
        if (targetId == null || targetId <= 0) return

        val newFav = !_isFavourite.value
        _isFavourite.value = newFav
        val updated = detail.copy(isFavourite = newFav)
        _state.value = StaffDetailState.Success(updated)
        DetailCache.putStaffDetail(lastSource, lastStaffId, updated)

        viewModelScope.launch(Dispatchers.IO) {
            val ok = mutationsClient.toggleFavourite("staff", targetId)
            if (!ok) {
                _isFavourite.value = !newFav
                val rolled = detail.copy(isFavourite = !newFav)
                _state.value = StaffDetailState.Success(rolled)
                DetailCache.putStaffDetail(lastSource, lastStaffId, rolled)
            }
        }
    }
}
