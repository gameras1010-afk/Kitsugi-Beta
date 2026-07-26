package com.kitsugi.animelist.core.player

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.Display
import kotlin.math.roundToInt

/**
 * T1.4 – Auto Frame Rate (AFR) Matching Infrastructure
 *
 * Ekranın yenileme hızı ve çözünürlük değiştirmeyi destekleyip
 * desteklemediğini Display.getSupportedModes() aracılığıyla tespit eder.
 *
 * EDID tabanlı algılama best-effort'tur: bazı TV'ler modları yanlış
 * raporlayabilir. Sonuç, kullanıcı arayüzünü bilgilendirmek içindir
 * (kullanıcı, donanımında etkisiz görünen bir ayarı kapatabilir);
 * FrameRateUtils zaten uygun mod bulunamazsa sessizce no-op yapar.
 */
object DisplayCapabilities {

    private const val TAG = "DisplayCapabilities"

    data class Snapshot(
        /** Mevcut çözünürlükte birden fazla yenileme hızı var mı? */
        val supportsFrameRateSwitching: Boolean,
        /** Birden fazla fiziksel çözünürlük modu var mı? */
        val supportsResolutionSwitching: Boolean,
        val supportedModes: List<Display.Mode>,
        val currentModeId: Int,
        val apiSupported: Boolean,
    ) {
        companion object {
            val Unknown = Snapshot(
                supportsFrameRateSwitching = false,
                supportsResolutionSwitching = false,
                supportedModes = emptyList(),
                currentModeId = -1,
                apiSupported = false,
            )
        }
    }

    /**
     * Verilen activity üzerinden ekran yeteneklerini tespit eder.
     * API 23 (M) altında her zaman [Snapshot.Unknown] döner.
     */
    fun detect(activity: Activity): Snapshot {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return Snapshot.Unknown
        }
        val display = activity.window?.decorView?.display ?: return Snapshot.Unknown
        val modes = display.supportedModes.toList()
        val current = display.mode
        val (afr, res) = deriveSupport(modes, current.modeId)
        return Snapshot(
            supportsFrameRateSwitching = afr,
            supportsResolutionSwitching = res,
            supportedModes = modes,
            currentModeId = current.modeId,
            apiSupported = true,
        )
    }

    /**
     * Ekran yeteneklerini logcat'e yazar; debug ve kullanıcı
     * destek amacıyla kullanılabilir.
     */
    fun logSummary(snapshot: Snapshot) {
        if (!snapshot.apiSupported) {
            Log.i(TAG, "api=${Build.VERSION.SDK_INT} apiSupported=false (no display introspection)")
            return
        }
        val current = snapshot.supportedModes.firstOrNull { it.modeId == snapshot.currentModeId }
        val rates = snapshot.supportedModes
            .map { roundedMilliHz(it.refreshRate) / 1000f }
            .distinct()
            .sorted()
            .joinToString(",")
        val resolutions = snapshot.supportedModes
            .map { "${it.physicalWidth}x${it.physicalHeight}" }
            .distinct()
            .joinToString(",")
        val currentDesc = current?.let {
            "${it.physicalWidth}x${it.physicalHeight}@${"%.3f".format(it.refreshRate)}Hz"
        } ?: "unknown"
        Log.i(
            TAG,
            "api=${Build.VERSION.SDK_INT} current=$currentDesc modeCount=${snapshot.supportedModes.size} " +
                "rates=[$rates] resolutions=[$resolutions] " +
                "afrSupported=${snapshot.supportsFrameRateSwitching} " +
                "resSupported=${snapshot.supportsResolutionSwitching}"
        )
    }

    /**
     * Verilen mod listesinden AFR ve çözünürlük değiştirme desteğini türetir.
     *
     * Millihertz hassasiyetinde yenileme hızları karşılaştırılır; bu sayede
     * 59.94f != 59.940002f gibi float gürültüsünden kaynaklanan yanlış
     * pozitifler önlenir.
     *
     * Birim testler için internal erişimli bırakılmıştır.
     */
    internal fun deriveSupport(modes: List<Display.Mode>, currentModeId: Int): Pair<Boolean, Boolean> {
        if (modes.isEmpty()) return false to false
        val current = modes.firstOrNull { it.modeId == currentModeId } ?: modes.first()
        val sameResModes = modes.filter {
            it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight
        }
        val distinctRates = sameResModes.map { roundedMilliHz(it.refreshRate) }.toSet()
        val distinctResolutions = modes.map { it.physicalWidth to it.physicalHeight }.toSet()
        return (distinctRates.size >= 2) to (distinctResolutions.size >= 2)
    }

    private fun roundedMilliHz(refreshRate: Float): Int = (refreshRate * 1000f).roundToInt()
}
