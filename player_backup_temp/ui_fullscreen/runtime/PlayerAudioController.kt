package com.kitsugi.animelist.ui.screens.fullscreen.runtime

import android.util.Log
import com.kitsugi.animelist.core.player.AudioDelayRouteConfig
import com.kitsugi.animelist.core.player.AudioOutputRouteDetector
import com.kitsugi.animelist.core.player.AudioRoute
import com.kitsugi.animelist.core.player.engine.PlayerEngine
import com.kitsugi.animelist.data.settings.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * T1.6 – PlayerAudioController
 *
 * Ses rotası algılama (Bluetooth / HDMI / Kablolu / Hoparlör) ve
 * route-config'e göre otomatik ses gecikmesi uygulama mantığını
 * KitsugiPlayerViewModel'den ayrıştırır.
 *
 * @param scope   ViewModel'in viewModelScope'u
 * @param context Application context (AudioManager + SettingsDataStore)
 */
class PlayerAudioController(
    private val scope: CoroutineScope,
    private val context: android.content.Context
) {
    private val TAG = "PlayerAudioCtrl"

    private val _activeAudioRoute = MutableStateFlow(AudioRoute.SPEAKER)
    val activeAudioRoute: StateFlow<AudioRoute> = _activeAudioRoute.asStateFlow()

    private val routeDetector by lazy { AudioOutputRouteDetector(context) }
    private var observerJob: Job? = null

    /**
     * Ses rotası değişikliklerini dinlemeye başlar.
     * Her değişimde aktif engine'e uygun gecikme değerini uygular.
     *
     * @param getEngine Çağrı anında aktif engine'i döner (null-safe).
     */
    fun startObserving(getEngine: () -> PlayerEngine?) {
        observerJob?.cancel()
        observerJob = scope.launch {
            routeDetector.observeRouteChanges().collect { route ->
                _activeAudioRoute.value = route
                val settings = SettingsDataStore(context).settingsFlow.first()
                val config = AudioDelayRouteConfig.fromJson(settings.audioDelayPerRouteJson)
                val routeDelay = config.getDelayFor(route)
                val manualDelay = settings.defaultAudioDelayMs
                val total = manualDelay + routeDelay
                getEngine()?.setAudioDelay(total)
                Log.d(TAG, "Route: $route → delay=${total}ms (route=${routeDelay}, manual=${manualDelay})")
            }
        }
    }

    /**
     * Gözlemi durdurur (onCleared / ekrandan çıkışta çağrılır).
     */
    fun stopObserving() {
        observerJob?.cancel()
        observerJob = null
    }
}
