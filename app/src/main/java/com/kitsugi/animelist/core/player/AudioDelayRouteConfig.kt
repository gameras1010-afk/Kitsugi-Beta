package com.kitsugi.animelist.core.player

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * T1.3 — Ses Rotası Başına Gecikme Yapılandırması.
 *
 * Bluetooth kulaklık, kablolu kulaklık ve hoparlör için ayrı ayrı
 * ses gecikmesi (ms) tanımlanabilir. Ayarlar JSON olarak `AppSettings.audioDelayPerRouteJson`
 * alanında saklanır.
 *
 * ### Kullanım
 * ```kotlin
 * val config = AudioDelayRouteConfig.fromJson(appSettings.audioDelayPerRouteJson)
 * val delayMs = config.getDelayFor(activeAudioRoute)
 * playerEngine.setAudioDelay(manualDelayMs + delayMs)
 * ```
 */
@Serializable
data class AudioDelayRouteConfig(
    /** Hoparlör (telefon veya TV hoparlörü) için gecikme (ms) */
    val speakerDelayMs: Long = 0L,
    /** Bluetooth kulaklık / hoparlör için gecikme (ms) */
    val bluetoothDelayMs: Long = 0L,
    /** Kablolu kulaklık / AUX için gecikme (ms) */
    val wiredDelayMs: Long = 0L,
    /** HDMI / USB-C harici ekran çıkışı için gecikme (ms) */
    val hdmiDelayMs: Long = 0L
) {
    companion object {
        val DEFAULT = AudioDelayRouteConfig()

        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        /** JSON string'den deserialize eder. Hatalı JSON için DEFAULT döner. */
        fun fromJson(raw: String?): AudioDelayRouteConfig {
            if (raw.isNullOrBlank()) return DEFAULT
            return try {
                json.decodeFromString<AudioDelayRouteConfig>(raw)
            } catch (e: Exception) {
                android.util.Log.w("AudioDelayRouteConfig", "JSON parse hatası: ${e.message}")
                DEFAULT
            }
        }

        /** Veri sınıfını JSON string'e serialize eder. */
        fun toJson(config: AudioDelayRouteConfig): String =
            json.encodeToString(config)
    }

    /**
     * Aktif ses rotasına karşılık gelen gecikme değerini döner.
     *
     * @param route Aktif ses çıkış rotası (AudioOutputRouteDetector'dan gelir)
     * @return Gecikme değeri (ms)
     */
    fun getDelayFor(route: AudioRoute): Long = when (route) {
        AudioRoute.SPEAKER   -> speakerDelayMs
        AudioRoute.BLUETOOTH -> bluetoothDelayMs
        AudioRoute.WIRED     -> wiredDelayMs
        AudioRoute.HDMI      -> hdmiDelayMs
        AudioRoute.OTHER     -> 0L
    }
}
