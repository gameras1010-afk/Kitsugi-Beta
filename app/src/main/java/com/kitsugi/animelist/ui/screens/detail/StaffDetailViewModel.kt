package com.kitsugi.animelist.ui.screens.detail

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.local.TranslationManager
import com.kitsugi.animelist.data.remote.DetailCache
import com.kitsugi.animelist.data.remote.JikanApiClient
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
    private val translationManager = TranslationManager(context)
    private val settingsDataStore = SettingsDataStore(context)
    private val TAG = "StaffDetailVM"

    private val _state = MutableStateFlow<StaffDetailState>(StaffDetailState.Loading)
    val state: StateFlow<StaffDetailState> = _state.asStateFlow()

    private val _translatedBio = MutableStateFlow<String?>(null)
    val translatedBio: StateFlow<String?> = _translatedBio.asStateFlow()

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
}
