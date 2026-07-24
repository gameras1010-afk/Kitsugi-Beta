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
import com.kitsugi.animelist.data.remote.RateLimitException
import com.kitsugi.animelist.data.remote.ResourceNotFoundException
import com.kitsugi.animelist.data.settings.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CharacterDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val apiClient = JikanApiClient()
    private val mutationsClient = KitsugiMediaMutationsClient()
    private val translationManager = TranslationManager(context)
    private val settingsDataStore = SettingsDataStore(context)
    private val TAG = "CharacterDetailVM"

    private val _state = MutableStateFlow<CharacterDetailState>(CharacterDetailState.Loading)
    val state: StateFlow<CharacterDetailState> = _state.asStateFlow()

    private val _translatedBio = MutableStateFlow<String?>(null)
    val translatedBio: StateFlow<String?> = _translatedBio.asStateFlow()

    private val _isFavourite = MutableStateFlow(false)
    val isFavourite: StateFlow<Boolean> = _isFavourite.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentFetchKey: String? = null
    private var lastCharacterId: Int = 0
    private var lastSource: String = ""
    private var lastCharacterName: String? = null

    fun loadCharacter(characterId: Int, source: String, name: String? = null) {
        val newKey = "$source:$characterId"
        if (newKey == currentFetchKey) {
            Log.d(TAG, "loadCharacter: Cache hit for key=$newKey — skipping")
            return
        }

        Log.d(TAG, "loadCharacter: New key=$newKey (was $currentFetchKey)")
        currentFetchKey = newKey
        lastCharacterId = characterId
        lastSource = source
        lastCharacterName = name

        val cachedCharacterDetail = DetailCache.getCharacterDetail(source, characterId)
        val cachedBioTranslation = DetailCache.getTranslation("bio_char", source, characterId)

        _state.value = if (cachedCharacterDetail != null) CharacterDetailState.Success(cachedCharacterDetail) else CharacterDetailState.Loading
        _translatedBio.value = cachedBioTranslation

        viewModelScope.launch {
            fetchCharacterDetail(characterId, source, name)
        }
    }

    fun retry() {
        val characterId = lastCharacterId
        val source = lastSource
        val name = lastCharacterName
        if (characterId > 0 && source.isNotEmpty()) {
            _state.value = CharacterDetailState.Loading
            _translatedBio.value = null
            viewModelScope.launch {
                fetchCharacterDetail(characterId, source, name, force = true)
            }
        }
    }

    fun forceRefresh() {
        val characterId = lastCharacterId
        val source = lastSource
        val name = lastCharacterName
        if (characterId > 0 && source.isNotEmpty()) {
            _isRefreshing.value = true
            viewModelScope.launch {
                try {
                    fetchCharacterDetail(characterId, source, name, force = true)
                } finally {
                    _isRefreshing.value = false
                }
            }
        }
    }

    private suspend fun fetchCharacterDetail(characterId: Int, source: String, name: String? = null, force: Boolean = false) {
        if (force) {
            DetailCache.removeCharacterDetail(source, characterId)
        }
        val cached = DetailCache.getCharacterDetail(source, characterId)
        val detail = if (cached != null && !force) {
            cached
        } else {
            runCatching {
                withContext(Dispatchers.IO) {
                    apiClient.fetchCharacterDetail(source, characterId, name)
                }
            }.onFailure { err ->
                val msg = when (err) {
                    is RateLimitException -> "Hız limitine takıldık. Lütfen birkaç saniye bekleyip tekrar deneyin."
                    is ResourceNotFoundException -> "Bu karakter bulunamadı."
                    is java.io.IOException -> "Ağ bağlantısı hatası. Lütfen bağlantınızı kontrol edin."
                    else -> "Karakter detayları yüklenemedi."
                }
                Log.e(TAG, "fetchCharacterDetail error: ${err.message}", err)
                if (_state.value !is CharacterDetailState.Success) {
                    _state.value = CharacterDetailState.Error(msg)
                }
            }.getOrNull()
        }

        if (detail != null) {
            DetailCache.putCharacterDetail(source, characterId, detail)
            _state.value = CharacterDetailState.Success(detail)
            _isFavourite.value = detail.isFavourite

            // Otomatik çeviri açıksa biyografiyi çevir (zaten Türkçeyse TranslationManager atlar)
            val bio = detail.biography
            val autoTranslate = runCatching { settingsDataStore.settingsFlow.first() }.getOrNull()?.autoTranslateEnabled ?: false
            val cachedTranslation = DetailCache.getTranslation("bio_char", source, characterId)
            if (cachedTranslation != null && !force) {
                _translatedBio.value = cachedTranslation
            } else if (!bio.isNullOrBlank() && autoTranslate) {
                val tr = withContext(Dispatchers.IO) {
                    translationManager.translateToTurkish(bio)
                }
                if (tr != bio) {
                    DetailCache.putTranslation("bio_char", source, characterId, tr)
                    _translatedBio.value = tr
                }
            }
        } else if (_state.value !is CharacterDetailState.Success) {
            _state.value = CharacterDetailState.Error("Karakter detayları yüklenemedi.")
        }
    }

    /**
     * AniList karakter favori toggle — giriş yapılmış ve aniListId biliniyorsa çalışır.
     * Anında UI güncellemesi yapar, arka planda mutasyon çalışır.
     */
    fun toggleFavourite() {
        ExternalAuthManager.getAniListToken(context) ?: return
        val currentState = _state.value as? CharacterDetailState.Success ?: return
        val detail = currentState.detail
        val targetId = detail.aniListId ?: if (lastSource.lowercase() == "anilist") detail.id else null
        if (targetId == null || targetId <= 0) return

        val newFav = !_isFavourite.value
        _isFavourite.value = newFav
        val updated = detail.copy(isFavourite = newFav)
        _state.value = CharacterDetailState.Success(updated)
        DetailCache.putCharacterDetail(lastSource, lastCharacterId, updated)

        viewModelScope.launch(Dispatchers.IO) {
            val ok = mutationsClient.toggleFavourite("character", targetId)
            if (!ok) {
                // rollback
                _isFavourite.value = !newFav
                val rolled = detail.copy(isFavourite = !newFav)
                _state.value = CharacterDetailState.Success(rolled)
                DetailCache.putCharacterDetail(lastSource, lastCharacterId, rolled)
            }
        }
    }
}
