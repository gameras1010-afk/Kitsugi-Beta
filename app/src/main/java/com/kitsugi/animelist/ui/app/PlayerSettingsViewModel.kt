package com.kitsugi.animelist.ui.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.settings.SettingsDataStore
import kotlinx.coroutines.launch

class PlayerSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val settingsDataStore = SettingsDataStore(context)

    // Callback to show messages on UI
    var onShowMessage: ((String) -> Unit)? = null

    fun updatePlayerPreference(pref: String) {
        viewModelScope.launch {
            settingsDataStore.setPlayerPreference(pref)
            onShowMessage?.invoke("Varsayılan oynatıcı güncellendi")
        }
    }

    fun updateAutoplayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAutoplayEnabled(enabled)
            onShowMessage?.invoke(if (enabled) "Otomatik oynatma etkin" else "Otomatik oynatma devre dışı")
        }
    }

    fun updateSkipIntroDurationSec(sec: Int) {
        viewModelScope.launch {
            settingsDataStore.setSkipIntroDurationSec(sec)
            onShowMessage?.invoke("İntro atlama süresi güncellendi")
        }
    }

    fun updateDefaultSubtitleSize(size: Int) {
        viewModelScope.launch {
            settingsDataStore.setDefaultSubtitleSize(size)
            onShowMessage?.invoke("Varsayılan altyazı boyutu güncellendi")
        }
    }

    fun updateDefaultSubtitleColor(color: Int) {
        viewModelScope.launch {
            settingsDataStore.setDefaultSubtitleColor(color)
            onShowMessage?.invoke("Varsayılan altyazı rengi güncellendi")
        }
    }

    fun updateSubtitleBold(bold: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setSubtitleBold(bold)
            onShowMessage?.invoke(if (bold) "Kalın altyazı etkin" else "Kalın altyazı devre dışı")
        }
    }

    fun updateSubtitleOutline(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setSubtitleOutlineEnabled(enabled)
            onShowMessage?.invoke(if (enabled) "Altyazı kenarlığı etkin" else "Altyazı kenarlığı devre dışı")
        }
    }

    fun updateDefaultAudioBoost(boost: Float) {
        viewModelScope.launch {
            settingsDataStore.setDefaultAudioBoost(boost)
            onShowMessage?.invoke("Varsayılan ses güçlendirmesi güncellendi")
        }
    }

    fun updateDefaultAudioDelayMs(delayMs: Long) {
        viewModelScope.launch {
            settingsDataStore.setDefaultAudioDelayMs(delayMs)
            onShowMessage?.invoke("Varsayılan ses gecikmesi güncellendi")
        }
    }

    fun updateBufferSettings(
        min: Int,
        max: Int,
        playback: Int,
        rebuffer: Int,
        back: Int
    ) {
        // T1-15: min≤max ve playback≤max kuralı; geçersiz değer gelirse clamp uygula
        val clampedMax = max.coerceAtLeast(1000)
        val clampedMin = min.coerceIn(0, clampedMax)
        val clampedPlayback = playback.coerceIn(0, clampedMax)
        val clampedRebuffer = rebuffer.coerceIn(0, clampedMax)
        val clampedBack = back.coerceAtLeast(0)
        viewModelScope.launch {
            settingsDataStore.setBufferSettings(clampedMin, clampedMax, clampedPlayback, clampedRebuffer, clampedBack)
            onShowMessage?.invoke("Arabellek (Buffer) ayarları güncellendi")
        }
    }

    fun updateDv7HandlingMode(mode: com.kitsugi.animelist.data.settings.Dv7HandlingMode) {
        viewModelScope.launch {
            settingsDataStore.setDv7HandlingMode(mode)
            onShowMessage?.invoke("Dolby Vision işleme modu güncellendi")
        }
    }

    fun updateStripHdr10PlusSei(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setStripHdr10PlusSei(enabled)
            onShowMessage?.invoke(if (enabled) "HDR10+ SEI çıkarma etkin" else "HDR10+ SEI çıkarma devre dışı")
        }
    }

    fun updatePreferredSubtitleLanguages(langs: String) {
        viewModelScope.launch {
            settingsDataStore.setPreferredSubtitleLanguages(langs)
            onShowMessage?.invoke("Tercih edilen altyazı dilleri güncellendi")
        }
    }

    fun updateAddonSubtitleStartupMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setAddonSubtitleStartupMode(mode)
            onShowMessage?.invoke("Altyazı yükleme modu güncellendi")
        }
    }

    fun updateQualityProfileJson(json: String) {
        viewModelScope.launch {
            settingsDataStore.setQualityProfileJson(json)
            onShowMessage?.invoke("Kalite profili güncellendi")
        }
    }

    // ─── T1.9 – Paralel Aralık İndirme ──────────────────────────────────────
    fun updateParallelRangeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setParallelRangeEnabled(enabled)
            onShowMessage?.invoke(
                if (enabled) "Paralel indirme etkin (MKV/MP4)" else "Paralel indirme devre dışı"
            )
        }
    }

    // ─── T1.4 – Frame Rate & Resolution Matching ────────────────────────────
    fun updateFrameRateMatchingMode(mode: com.kitsugi.animelist.data.settings.FrameRateMatchingMode) {
        viewModelScope.launch {
            settingsDataStore.setFrameRateMatchingMode(mode)
            onShowMessage?.invoke("Kare hızı eşleme modu güncellendi: ${mode.name}")
        }
    }

    fun updateResolutionMatchingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setResolutionMatchingEnabled(enabled)
            onShowMessage?.invoke(
                if (enabled) "Çözünürlük eşleme etkin" else "Çözünürlük eşleme devre dışı"
            )
        }
    }

    // ─── T2.1 + T2.7 – Gesture Ayarları ────────────────────────────────────
    fun updateGestureVolumeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setGestureVolumeEnabled(enabled)
            onShowMessage?.invoke(if (enabled) "Ses jesti etkin" else "Ses jesti devre dışı")
        }
    }

    fun updateGestureBrightnessEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setGestureBrightnessEnabled(enabled)
            onShowMessage?.invoke(if (enabled) "Parlaklık jesti etkin" else "Parlaklık jesti devre dışı")
        }
    }

    fun updateGestureZoomEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setGestureZoomEnabled(enabled)
            onShowMessage?.invoke(if (enabled) "Zoom jesti etkin" else "Zoom jesti devre dışı")
        }
    }

    fun updateDoubleTapSeekSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsDataStore.setDoubleTapSeekSeconds(seconds)
            onShowMessage?.invoke("Çift dokunuş ileri/geri süresi: ${seconds}s")
        }
    }

    fun updateHoldSpeedMultiplier(multiplier: Float) {
        viewModelScope.launch {
            settingsDataStore.setHoldSpeedMultiplier(multiplier)
            onShowMessage?.invoke("Basılı tutma hız çarpanı: ${multiplier}x")
        }
    }

    // TASK_050 — Swipe hassasiyeti
    fun updateGestureScrollSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            settingsDataStore.setGestureScrollSensitivity(sensitivity)
            val label = when {
                sensitivity <= 0.6f -> "Yavaş (%.1fx)".format(sensitivity)
                sensitivity >= 1.6f -> "Hızlı (%.1fx)".format(sensitivity)
                else                -> "Normal (%.1fx)".format(sensitivity)
            }
            onShowMessage?.invoke("Kaydırma hassasiyeti: $label")
        }
    }

    // ─── T2.2 – Önizleme Seekbar ───────────────────────────────────────────
    fun updatePreviewSeekbarEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setPreviewSeekbarEnabled(enabled)
            onShowMessage?.invoke(
                if (enabled) "Önizleme seekbarı etkin" else "Önizleme seekbarı devre dışı"
            )
        }
    }

    // ─── T2.5 – Harici Oynatıcı Tercihi ────────────────────────────────────
    fun updatePreferredExternalPlayerPackage(packageName: String) {
        viewModelScope.launch {
            settingsDataStore.setPreferredExternalPlayerPackage(packageName)
            onShowMessage?.invoke("Tercih edilen harici oynatıcı güncellendi")
        }
    }

    // ─── T2.6 – Oynatıcı Başlık / Medya Bilgisi Görünürlüğü ───────────────
    fun updateShowPlayerTitle(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setShowPlayerTitle(show)
        }
    }

    fun updateShowPlayerResolution(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setShowPlayerResolution(show)
        }
    }

    fun updateShowMediaInfo(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setShowMediaInfo(show)
        }
    }

    fun updateTitleLimitType(limitType: String) {
        viewModelScope.launch {
            settingsDataStore.setTitleLimitType(limitType)
        }
    }

    // ─── T1.1 – Görüntü Oranı ───────────────────────────────────────────────
    fun updateAspectMode(mode: com.kitsugi.animelist.core.player.PlayerAspectMode) {
        viewModelScope.launch {
            settingsDataStore.setAspectMode(mode.name)
            onShowMessage?.invoke("Görüntü oranı güncellendi: ${mode.name}")
        }
    }

    fun updateLiveHelperEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setLiveHelperEnabled(enabled)
            onShowMessage?.invoke(if (enabled) "Canlı Yardımcı etkin" else "Canlı Yardımcı devre dışı")
        }
    }

    fun updateEnableAssExtractor(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setEnableAssExtractor(enabled)
            onShowMessage?.invoke(if (enabled) "ASS Altyazı Ayıklayıcı etkin" else "ASS Altyazı Ayıklayıcı devre dışı")
        }
    }

    // ─── T1-01 – StillWatching + PostPlayMode + AutoplaySessionLimit ──────────
    fun updateStillWatchingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setStillWatchingEnabled(enabled)
            onShowMessage?.invoke(if (enabled) "Hâlâ izliyor musun? sorusu etkin" else "Hâlâ izliyor musun? sorusu devre dışı")
        }
    }

    fun updateStillWatchingThresholdMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsDataStore.setStillWatchingThresholdMinutes(minutes)
            onShowMessage?.invoke("Hareketsizlik eşiği: ${minutes} dakika")
        }
    }

    fun updatePostPlayMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setPostPlayMode(mode)
            onShowMessage?.invoke("Sonraki bölüm modu güncellendi")
        }
    }

    fun updateAutoplaySessionLimit(limit: Int) {
        viewModelScope.launch {
            settingsDataStore.setAutoplaySessionLimit(limit)
            onShowMessage?.invoke(if (limit == 0) "Otomatik oynatma limiti: Sınırsız" else "Otomatik oynatma limiti: $limit bölüm")
        }
    }

    // ─── T1-03 – Ses Gelişmiş: GainBoost + SubtitleDelay ──────────────────
    fun updateGainBoostDb(db: Float) {
        viewModelScope.launch {
            settingsDataStore.setGainBoostDb(db)
            onShowMessage?.invoke("Ses güçlendirme: ${db}dB")
        }
    }

    fun updateSubtitleDelayMs(delayMs: Long) {
        viewModelScope.launch {
            settingsDataStore.setSubtitleDelayMs(delayMs)
            onShowMessage?.invoke("Altyazı gecikmesi: ${delayMs}ms")
        }
    }

    // ─── T1-04 – Dekoder Önceliği (Telefon) ──────────────────────────────
    fun updateDecoderPriority(priority: Int) {
        viewModelScope.launch {
            settingsDataStore.setDecoderPriority(priority)
            val label = when (priority) {
                1 -> "Yazılım Fallback"
                2 -> "Yazılım Öncelikli"
                else -> "Donanım Öncelikli"
            }
            onShowMessage?.invoke("Dekoder önceliği: $label")
        }
    }
}
