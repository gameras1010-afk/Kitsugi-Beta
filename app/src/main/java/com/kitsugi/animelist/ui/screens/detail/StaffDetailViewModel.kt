package com.kitsugi.animelist.ui.screens.detail

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import com.kitsugi.animelist.data.local.TranslationManager
import com.kitsugi.animelist.data.remote.DetailCache
import com.kitsugi.animelist.data.remote.GalleryCategory
import com.kitsugi.animelist.data.remote.GalleryItem
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
import org.json.JSONObject
import java.net.URL

class StaffDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val apiClient = JikanApiClient()
    private val mutationsClient = KitsugiMediaMutationsClient()
    private val translationManager = TranslationManager(context)
    private val settingsDataStore = SettingsDataStore(context)
    private val TAG = "StaffDetailVM"

    private val _state = MutableStateFlow<StaffDetailState>(StaffDetailState.Loading)
    val state: StateFlow<StaffDetailState> = _state.asStateFlow()

    private val _galleryItems = MutableStateFlow<List<GalleryItem>>(emptyList())
    val galleryItems: StateFlow<List<GalleryItem>> = _galleryItems.asStateFlow()

    private val _translatedBio = MutableStateFlow<String?>(null)
    val translatedBio: StateFlow<String?> = _translatedBio.asStateFlow()

    private val _isFavourite = MutableStateFlow(false)
    val isFavourite: StateFlow<Boolean> = _isFavourite.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

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

    fun forceRefresh() {
        val staffId = lastStaffId
        val source = lastSource
        val name = lastStaffName
        if (staffId > 0 && source.isNotEmpty()) {
            _isRefreshing.value = true
            viewModelScope.launch {
                try {
                    fetchStaffDetail(staffId, source, name, force = true)
                } finally {
                    _isRefreshing.value = false
                }
            }
        }
    }

    private suspend fun fetchStaffDetail(staffId: Int, source: String, name: String? = null, force: Boolean = false) {
        if (force) {
            DetailCache.removeStaffDetail(source, staffId)
        }
        val cached = DetailCache.getStaffDetail(source, staffId)
        val detail = if (cached != null && !force) {
            cached
        } else {
            runCatching {
                withContext(Dispatchers.IO) {
                    apiClient.fetchStaffDetail(source, staffId, name)
                }
            }.onFailure { err ->
                val msg = when (err) {
                    is RateLimitException -> "Hız limitine takıldık. Lütfen birkaç saniye bekleyip tekrar deneyin."
                    is ResourceNotFoundException -> "Bu ekip üyesi bulunamadı."
                    is java.io.IOException -> "Ağ bağlantısı hatası. Lütfen bağlantınızı kontrol edin."
                    else -> "Ekip üyesi detayları yüklenemedi."
                }
                Log.e(TAG, "fetchStaffDetail error: ${err.message}", err)
                if (_state.value !is StaffDetailState.Success) {
                    _state.value = StaffDetailState.Error(msg)
                }
            }.getOrNull()
        }

        if (detail != null) {
            DetailCache.putStaffDetail(source, staffId, detail)
            _state.value = StaffDetailState.Success(detail)
            _isFavourite.value = detail.isFavourite

            // Build gallery from imageUrl + Jikan /people pictures
            buildStaffGallery(staffId, source, detail.imageUrl)

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
        } else if (_state.value !is StaffDetailState.Success) {
            _state.value = StaffDetailState.Error("Ekip üyesi detayları yüklenemedi.")
        }
    }

    /**
     * Ekip üyesi görsellerini Jikan /people/{id}/pictures endpoint'inden alır.
     * Ana imageUrl dahil tüm görseller CHARACTER kategorisinde etiketlenir.
     */
    private suspend fun buildStaffGallery(staffId: Int, source: String, mainImageUrl: String?) {
        val jikanId = if (source.lowercase() == "anilist" && staffId >= 100_000_000) {
            null
        } else {
            staffId.takeIf { it > 0 }
        }

        val pictureUrls = if (jikanId != null && source.lowercase() != "anilist") {
            withContext(Dispatchers.IO) {
                fetchJikanPictures(jikanId, "people")
            }
        } else emptyList()

        val items = buildList {
            if (!mainImageUrl.isNullOrBlank()) {
                add(GalleryItem(url = mainImageUrl, source = "Jikan", category = GalleryCategory.CHARACTER))
            }
            for (url in pictureUrls) {
                if (url != mainImageUrl && url.isNotBlank()) {
                    add(GalleryItem(url = url, source = "Jikan", category = GalleryCategory.CHARACTER))
                }
            }
        }.distinctBy { it.url }

        _galleryItems.value = items
    }

    private fun fetchJikanPictures(id: Int, endpoint: String): List<String> {
        val url = URL("https://api.jikan.moe/v4/$endpoint/$id/pictures")
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "KitsugiAnimeList/1.0")
            .build()
        return runCatching {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching emptyList()
                val text = response.body?.string() ?: return@runCatching emptyList()
                val dataArr = JSONObject(text).optJSONArray("data") ?: return@runCatching emptyList()
                val urls = mutableListOf<String>()
                for (i in 0 until dataArr.length()) {
                    val obj = dataArr.getJSONObject(i)
                    val webp = obj.optJSONObject("webp")
                    val jpg = obj.optJSONObject("jpg")
                    val picUrl = webp?.optString("large_image_url")?.takeIf { it.isNotBlank() }
                        ?: webp?.optString("image_url")?.takeIf { it.isNotBlank() }
                        ?: jpg?.optString("large_image_url")?.takeIf { it.isNotBlank() }
                        ?: jpg?.optString("image_url")?.takeIf { it.isNotBlank() }
                    if (!picUrl.isNullOrBlank()) urls.add(picUrl)
                }
                urls
            }
        }.getOrElse { emptyList() }
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
