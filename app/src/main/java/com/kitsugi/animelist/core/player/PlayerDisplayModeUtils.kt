package com.kitsugi.animelist.core.player

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.WindowManager

/**
 * T1.4: PlayerDisplayModeUtils
 *
 * NuvioTV PlayerDisplayModeUtils referansından adapte edildi.
 * Ekran yenileme hızını içerik FPS'ine göre ayarlamak için
 * WindowManager.LayoutParams.preferredDisplayModeId kullanır.
 *
 * Yalnızca Android 6.0+ (API 23) cihazlarda çalışır.
 */
object PlayerDisplayModeUtils {

    private const val TAG = "PlayerDisplayModeUtils"

    /**
     * En iyi ekran modunu seç ve uygula.
     *
     * @param activity Mevcut Activity
     * @param targetFps İçerik kare hızı (ör: 23.976, 24, 30, 60)
     * @param capabilities Ekran yetenekleri
     * @return Seçilen mod ID veya 0 (değiştirilmedi)
     */
    fun applyBestDisplayMode(
        activity: Activity,
        targetFps: Float,
        snapshot: DisplayCapabilities.Snapshot
    ): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.d(TAG, "API < 23, display mode değiştirilemez")
            return 0
        }

        return try {
            val normalized = normalizeContentFps(targetFps)
            val bestMode = snapshot.supportedModes
                .filter { mode -> isRefreshRateCompatible(mode.refreshRate, normalized) }
                .minByOrNull { mode ->
                    // En yakın yenileme hızını tercih et (en düşük uyumlu kat)
                    val ratio = mode.refreshRate / normalized
                    ratio
                }
            if (bestMode != null) {
                val params = activity.window.attributes
                params.preferredDisplayModeId = bestMode.modeId
                activity.window.attributes = params
                Log.d(TAG, "Display mode uygulandı: ${bestMode.modeId} (${bestMode.refreshRate}Hz) → targetFps=$targetFps")
                bestMode.modeId
            } else {
                Log.d(TAG, "Uygun display mode bulunamadı (targetFps=$targetFps)")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Display mode uygulanamadı", e)
            0
        }
    }

    /**
     * Ekran modunu varsayılana sıfırla (oyuncu kapatılınca çağırın).
     */
    fun resetDisplayMode(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val params = activity.window.attributes
            params.preferredDisplayModeId = 0
            activity.window.attributes = params
            Log.d(TAG, "Display mode varsayılana sıfırlandı")
        } catch (e: Exception) {
            Log.e(TAG, "Display mode sıfırlanamadı", e)
        }
    }

    /**
     * İçerik FPS'inden önerilen ekran yenileme hızını hesaplar.
     * Örn: 23.976 → 24Hz, 29.97 → 30Hz, 59.94 → 60Hz
     */
    fun normalizeContentFps(rawFps: Float): Float {
        return when {
            rawFps in 23.8f..24.1f  -> 24f
            rawFps in 25.0f..25.1f  -> 25f
            rawFps in 29.9f..30.1f  -> 30f
            rawFps in 47.9f..48.1f  -> 48f
            rawFps in 50.0f..50.1f  -> 50f
            rawFps in 59.9f..60.1f  -> 60f
            rawFps in 119.9f..120.1f -> 120f
            else                     -> rawFps
        }
    }

    /**
     * Verilen FPS için bir refresh rate'in uyumlu olup olmadığını kontrol eder.
     * Örn: 24fps → 24Hz, 48Hz, 72Hz, 96Hz, 120Hz uyumlu.
     */
    fun isRefreshRateCompatible(displayRefreshRate: Float, contentFps: Float): Boolean {
        val normalized = normalizeContentFps(contentFps)
        if (normalized <= 0) return false
        // displayRefreshRate, contentFps'in tam katı mı?
        val ratio = displayRefreshRate / normalized
        return ratio >= 1f && (ratio - ratio.toInt()) < 0.01f
    }
}
