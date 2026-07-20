package com.kitsugi.animelist.core.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject

enum class AudioRoute {
    SPEAKER,
    BLUETOOTH,
    WIRED,
    HDMI,
    OTHER;

    fun toJsonKey(): String = name.lowercase()
}

/**
 * AudioOutputRouteDetector — Cihazın ses çıkış rotasını izler.
 * Kulaklık takılması/çıkarılması veya bluetooth bağlantısına göre gecikmeyi ayarlar.
 */
class AudioOutputRouteDetector(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Anlık aktif ses rotasını döner.
     */
    fun getCurrentRoute(): AudioRoute {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var hasBluetooth = false
            var hasWired = false
            var hasSpeaker = false
            var hasHdmi = false

            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    22, // TYPE_BLE_HEADSET
                    26, // TYPE_BLE_SPEAKER
                    27 -> { // TYPE_BLE_BROADCAST
                        hasBluetooth = true
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_DEVICE -> {
                        hasWired = true
                    }
                    AudioDeviceInfo.TYPE_HDMI,
                    AudioDeviceInfo.TYPE_HDMI_ARC -> {
                        hasHdmi = true
                    }
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                        hasSpeaker = true
                    }
                }
            }

            return when {
                hasBluetooth -> AudioRoute.BLUETOOTH
                hasWired     -> AudioRoute.WIRED
                hasHdmi      -> AudioRoute.HDMI
                hasSpeaker   -> AudioRoute.SPEAKER
                else         -> AudioRoute.OTHER
            }
        } else {
            // API 23 altı için basit kontroller (Deprecations bypass)
            @Suppress("DEPRECATION")
            return when {
                audioManager.isBluetoothA2dpOn -> AudioRoute.BLUETOOTH
                audioManager.isWiredHeadsetOn -> AudioRoute.WIRED
                else -> AudioRoute.SPEAKER
            }
        }
    }

    /**
     * Ses çıkış rotası değişikliklerini flow olarak yayar.
     */
    fun observeRouteChanges(): Flow<AudioRoute> = callbackFlow {
        // İlk rotayı hemen gönder
        trySend(getCurrentRoute())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    trySend(getCurrentRoute())
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    trySend(getCurrentRoute())
                }
            }
            audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
            awaitClose {
                audioManager.unregisterAudioDeviceCallback(callback)
            }
        } else {
            // API 23 öncesi için receiver ile dinleyelim (büyük oranda yedek)
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: android.content.Intent?) {
                    trySend(getCurrentRoute())
                }
            }
            val filter = android.content.IntentFilter().apply {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                @Suppress("DEPRECATION")
                addAction(android.content.Intent.ACTION_HEADSET_PLUG)
            }
            context.registerReceiver(receiver, filter)
            awaitClose {
                context.unregisterReceiver(receiver)
            }
        }
    }

    companion object {
        /**
         * JSON içinden route bazlı gecikmeyi çeker.
         * Hem eski org.json formatını hem de AudioDelayRouteConfig'i destekler.
         */
        fun getDelayForRoute(jsonStr: String, route: AudioRoute): Long {
            // Önce AudioDelayRouteConfig ile dene (yeni format)
            val config = AudioDelayRouteConfig.fromJson(jsonStr)
            val configDelay = config.getDelayFor(route)
            if (configDelay != 0L) return configDelay

            // Eski JSON format fallback: {"speaker": 100, "bluetooth": 200}
            return try {
                val obj = org.json.JSONObject(jsonStr)
                obj.optLong(route.toJsonKey(), 0L)
            } catch (e: Exception) {
                0L
            }
        }
    }
}
