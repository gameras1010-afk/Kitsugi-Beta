package com.kitsugi.animelist.ui.screens.fullscreen.runtime

import android.util.Log
import com.kitsugi.animelist.data.remote.AniSkipClient
import com.kitsugi.animelist.data.remote.AnimeSkipClient
import com.kitsugi.animelist.data.remote.SkipInterval
import com.kitsugi.animelist.data.settings.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * T1.6 – PlayerSkipController
 *
 * AniSkip + AnimeSkip intro/outro zaman damgalarını yükler ve
 * otomatik atlama ayarını yönetir.
 *
 * KitsugiPlayerViewModel'deki loadSkipIntervals / updateSkipSettings
 * metodlarından ayrıştırılmıştır.
 */
class PlayerSkipController(
    private val scope: CoroutineScope,
    private val context: android.content.Context
) {
    private val TAG = "PlayerSkipCtrl"

    private val _skipIntervals = MutableStateFlow<List<SkipInterval>>(emptyList())
    val skipIntervals: StateFlow<List<SkipInterval>> = _skipIntervals.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _autoSkipEnabled = MutableStateFlow(false)
    val autoSkipEnabled: StateFlow<Boolean> = _autoSkipEnabled.asStateFlow()

    private val _aniSkipEnabled = MutableStateFlow(false)
    val aniSkipEnabled: StateFlow<Boolean> = _aniSkipEnabled.asStateFlow()

    private val _animeSkipClientId = MutableStateFlow("")
    val animeSkipClientId: StateFlow<String> = _animeSkipClientId.asStateFlow()

    /**
     * Settings'i dinlemeye başlar. VM.init { } içinde bir kez çağrılır.
     */
    fun observeSettings() {
        scope.launch {
            SettingsDataStore(context).settingsFlow.collect { settings ->
                _aniSkipEnabled.value = settings.aniSkipEnabled
                _autoSkipEnabled.value = settings.aniSkipAutoSkip
                _animeSkipClientId.value = settings.animeSkipClientId
            }
        }
    }

    /**
     * Belirtilen bölüm için AniSkip + AnimeSkip zaman damgalarını yükler.
     */
    fun loadIntervals(malId: Int, episode: Int) {
        scope.launch {
            _isLoading.value = true
            try {
                val settings = SettingsDataStore(context).settingsFlow.first()
                if (!settings.aniSkipEnabled) {
                    _skipIntervals.value = emptyList()
                    _autoSkipEnabled.value = false
                    return@launch
                }
                _autoSkipEnabled.value = settings.aniSkipAutoSkip

                val clientId = settings.animeSkipClientId
                val aniSkipDeferred = async { AniSkipClient.getSkipTimes(malId, episode) }
                val animeSkipDeferred = async { AnimeSkipClient.getSkipTimes(malId, episode, clientId) }

                val aniList   = runCatching { aniSkipDeferred.await() }.getOrElse { emptyList() }
                val animeList = runCatching { animeSkipDeferred.await() }.getOrElse { emptyList() }

                // AnimeSkip öncelikli, AniSkip aynı type'da zaten varsa atlanır
                val merged = mutableListOf<SkipInterval>()
                merged.addAll(animeList)
                val covered = animeList.map { it.type }.toSet()
                aniList.filterNot { it.type in covered }.forEach { merged.add(it) }

                Log.d(TAG, "Loaded: AniSkip=${aniList.size}, AnimeSkip=${animeList.size}, merged=${merged.size}")
                _skipIntervals.value = merged
            } catch (e: Exception) {
                Log.e(TAG, "Error loading skip intervals", e)
                _skipIntervals.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Kullanıcı skip ayarlarını değiştirdi.
     * DataStore'a yazar ve sonra intervals'ı yeniden yükler.
     */
    fun updateSettings(
        enabled: Boolean,
        autoSkip: Boolean,
        clientId: String,
        currentMalId: Int?,
        currentEpisode: Int
    ) {
        scope.launch {
            val store = SettingsDataStore(context)
            store.setAniSkipEnabled(enabled)
            store.setAniSkipAutoSkip(autoSkip)
            store.setAnimeSkipClientId(clientId)
            currentMalId?.let { loadIntervals(it, currentEpisode) }
        }
    }
}
