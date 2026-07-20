package com.kitsugi.animelist.core.player.engine

import android.util.Log
import com.kitsugi.animelist.core.player.PlaybackState
import com.kitsugi.animelist.core.player.PlayerManagerListener

/**
 * TASK_032 — PlayerFallbackCoordinator (Geliştirilmiş)
 *
 * NuvioTV PlayerRuntimeControllerEngineFailover ve PlayerManagerImpl
 * referans alınarak genişletildi.
 *
 * Özellikler:
 * - Deneme limiti ([maxAttempts]) aşıldığında fallback durdurulur
 * - Fallback zinciri: MEDIA3 → MPV → EXTERNAL → null (bitti)
 * - [PlayerManagerListener] üzerinden geçiş ve fatal hata bildirimi
 * - [reset] ile yeni bölüm/kaynak geçişinde sayaç sıfırlanır
 */
class PlayerFallbackCoordinator(
    private val maxAttempts: Int = 3,
    private val listener: PlayerManagerListener? = null
) {
    private val TAG = "PlayerFallbackCoord"
    private var attempts = 0

    /**
     * Mevcut motora göre bir sonraki fallback motorunu döndürür.
     *
     * @param currentEngine Şu an oynatmaya çalışan motor tipi
     * @param errorCode     [PlayerEngine.Listener.onPlaybackError] hata kodu
     * @param mpvEnabled    Kullanıcı ayarında MPV seçeneği açık mı?
     *
     * @return Denedecek sonraki motor; fallback bitti ise null
     */
    fun getFallbackEngine(
        currentEngine: PlayerEngineType,
        errorCode: Int,
        mpvEnabled: Boolean = false
    ): PlayerEngineType? {
        if (attempts >= maxAttempts) {
            Log.w(TAG, "Fallback limit aşıldı ($attempts/$maxAttempts) — fatal error bildiriliyor")
            listener?.onFatalError(
                errorCode = errorCode,
                errorMsg  = "Tüm dahili motorlar ($maxAttempts deneme) başarısız oldu. Hata kodu: $errorCode"
            )
            return null
        }

        attempts++
        val next = when (currentEngine) {
            PlayerEngineType.MEDIA3   -> PlayerEngineType.MPV
            PlayerEngineType.MPV      -> PlayerEngineType.EXTERNAL
            PlayerEngineType.EXTERNAL -> null
        }

        Log.d(TAG, "Fallback #$attempts: $currentEngine → $next (hata kodu: $errorCode)")

        if (next != null) {
            listener?.onPlayerSwitched(from = currentEngine, to = next)
        } else {
            listener?.onFatalError(
                errorCode = errorCode,
                errorMsg  = "Fallback zinciri tükendi (son motor: $currentEngine). Hata kodu: $errorCode"
            )
        }

        return next
    }

    /**
     * Yeni bölüm veya kaynak değişiminde fallback sayacını sıfırlar.
     * KitsugiPlayerViewModel.resetAutoSwitch() ile birlikte çağrılmalıdır.
     */
    fun reset() {
        if (attempts > 0) Log.d(TAG, "Fallback sayacı sıfırlandı ($attempts deneme vardı)")
        attempts = 0
    }

    /** Kaç fallback denemesi yapıldığını döndürür */
    val attemptCount: Int get() = attempts

    // ── Statik yardımcı (geriye dönük uyumluluk) ──────────────────────────────

    companion object {
        /**
         * Statik kullanım için kolaylaştırıcı — listener veya limit yok.
         * Mevcut inline kodlar için geriye dönük uyumlu.
         */
        fun nextEngine(
            currentEngine: PlayerEngineType,
            mpvEnabled: Boolean = false
        ): PlayerEngineType? = when (currentEngine) {
            PlayerEngineType.MEDIA3   -> if (mpvEnabled) PlayerEngineType.MPV else PlayerEngineType.EXTERNAL
            PlayerEngineType.MPV      -> PlayerEngineType.EXTERNAL
            PlayerEngineType.EXTERNAL -> null
        }
    }
}
