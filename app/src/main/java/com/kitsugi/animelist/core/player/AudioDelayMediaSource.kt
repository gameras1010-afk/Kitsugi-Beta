package com.kitsugi.animelist.core.player

import android.util.Log

/**
 * T1.3: AudioDelayMediaSource
 *
 * NuvioTV AudioDelayMediaSource referansından adapte edildi.
 * Media3'te audio delay, MediaSource wrapping yerine AudioProcessor
 * pipeline'ı üzerinden uygulanır. Bu sınıf, gecikme konfigürasyonunu
 * tutan ve route değişiminde gecikmeyi güncelleyen yönetici nesnedir.
 *
 * Gerçek ses gecikmesi: Media3PlayerEngine.setAudioDelayMs() →
 * GainAudioProcessor veya AudioSink offset aracılığıyla uygulanır.
 *
 * Kullanım:
 *   val delayConfig = AudioDelayMediaSource()
 *   delayConfig.setDelayForRoute(AudioRoute.BLUETOOTH, 150L)
 *   engine.setAudioDelayMs(delayConfig.getCurrentDelay())
 */
class AudioDelayMediaSource {

    companion object {
        private const val TAG = "AudioDelayMediaSource"
    }

    // Route bazlı gecikme haritası (ms)
    private val routeDelayMap = mutableMapOf<AudioRoute, Long>()

    // Aktif route
    private var activeRoute: AudioRoute = AudioRoute.SPEAKER

    /**
     * Belirli bir route için gecikme değerini ayarla (ms).
     */
    fun setDelayForRoute(route: AudioRoute, delayMs: Long) {
        routeDelayMap[route] = delayMs
        Log.d(TAG, "Delay ayarlandı: route=$route delayMs=$delayMs")
    }

    /**
     * Aktif route'u güncelle.
     * @return Yeni route için gecikme değeri (ms)
     */
    fun setActiveRoute(route: AudioRoute): Long {
        activeRoute = route
        val delay = getCurrentDelay()
        Log.d(TAG, "Aktif route değişti: $route → delay=${delay}ms")
        return delay
    }

    /**
     * Mevcut aktif route için gecikme değerini al (ms).
     */
    fun getCurrentDelay(): Long = routeDelayMap[activeRoute] ?: 0L

    /**
     * Tüm route gecikme haritasını al (kopyası).
     */
    fun getAllDelays(): Map<AudioRoute, Long> = routeDelayMap.toMap()

    /**
     * Route haritasını JSON string'den yükle.
     * Format: "SPEAKER:0,BLUETOOTH:150,WIRED:20"
     */
    fun loadFromString(encoded: String?) {
        if (encoded.isNullOrBlank()) return
        encoded.split(",").forEach { pair ->
            val parts = pair.trim().split(":")
            if (parts.size == 2) {
                val route = AudioRoute.entries.firstOrNull { it.name == parts[0].trim() }
                val delay = parts[1].trim().toLongOrNull()
                if (route != null && delay != null) {
                    routeDelayMap[route] = delay
                }
            }
        }
        Log.d(TAG, "Route delay haritası yüklendi: $routeDelayMap")
    }

    /**
     * Route haritasını JSON string'e kaydet.
     */
    fun saveToString(): String =
        routeDelayMap.entries.joinToString(",") { "${it.key.name}:${it.value}" }
}
