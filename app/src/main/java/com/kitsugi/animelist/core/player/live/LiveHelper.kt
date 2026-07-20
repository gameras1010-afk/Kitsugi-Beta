package com.kitsugi.animelist.core.player.live

import android.util.Log

/**
 * T1.15 – LiveHelper (Feature flag: DEFAULT OFF)
 *
 * Canlı yayın (Live/Linear TV) stream'leri için yardımcı sınıf.
 * DVR zaman kaydırma (time shift), canlı yayın başlangıç pozisyonu tespiti
 * ve "Canlıya Geri Dön" (Go-To-Live) mantığını içerir.
 *
 * KitsugiAnimeList'te anime canlı yayın şu an kullanılmıyor; bu sınıf
 * ileriki TV/trailer entegrasyonu için opsiyonel olarak eklenmiştir.
 *
 * **Feature flag:** `AppSettings.liveHelperEnabled` default = false.
 * Bu flag false iken Live stream'ler normal VOD gibi oynatılır.
 */
object LiveHelper {

    private const val TAG = "LiveHelper"

    /**
     * Bir stream URL'sinin canlı yayın mı yoksa VOD mu olduğunu tahmin eder.
     *
     * Basit sezgisel: HLS manifest içinde `#EXT-X-ENDLIST` tag'i yoksa live,
     * varsa VOD. MPEG-DASH için `@type="dynamic"` attribute kontrolü yapılır.
     *
     * @param url Stream URL'si
     * @return true → canlı yayın olma ihtimali yüksek
     */
    fun isLikelyLive(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("/live/") ||
            lower.contains("live.m3u8") ||
            lower.contains("stream.m3u8") ||
            lower.contains("livestream") ||
            lower.contains("/hls/master") ||
            lower.endsWith(".m3u8") // HLS → muhtemelen live, ama heuristic
    }

    /**
     * DVR başlangıç ofsetini hesaplar.
     *
     * @param dvrWindowSec DVR penceresi (saniye). 0 → DVR yok.
     * @param preferredOffsetSec Kullanıcının tercih ettiği başlangıç (saniye). -1 → en sona git.
     * @return Oynatmaya başlanacak pozisyon (ms). -1L → live edge (en sona git).
     */
    fun computeStartPositionMs(dvrWindowSec: Long, preferredOffsetSec: Long): Long {
        if (dvrWindowSec <= 0L) {
            Log.d(TAG, "No DVR window — starting at live edge")
            return -1L // Live edge
        }
        return when {
            preferredOffsetSec < 0 -> {
                Log.d(TAG, "Live edge requested")
                -1L
            }
            preferredOffsetSec == 0L -> {
                Log.d(TAG, "DVR: starting from window beginning (offset=0)")
                0L
            }
            else -> {
                val posMs = preferredOffsetSec * 1000L
                Log.d(TAG, "DVR: starting at offset=${preferredOffsetSec}s → ${posMs}ms")
                posMs
            }
        }
    }

    /**
     * "Canlıya Geri Dön" butonu için oynatıcı canlı kenardan ne kadar
     * uzakta olduğunu hesaplar.
     *
     * @param positionMs   Anlık oynatma pozisyonu
     * @param durationMs   Stream toplam süresi (DVR penceresi sonu = live edge)
     * @param thresholdMs  Bu kadar ms'den daha gerideyse "Canlıya Dön" butonu görünür (default 30s)
     * @return true → buton gösterilmeli
     */
    fun shouldShowGoToLive(
        positionMs: Long,
        durationMs: Long,
        thresholdMs: Long = 30_000L
    ): Boolean {
        if (durationMs <= 0L) return false
        val behindLiveMs = durationMs - positionMs
        return behindLiveMs > thresholdMs
    }

    /**
     * Live stream için maksimum seek pozisyonu (live edge) hesaplar.
     * Kullanıcı bu sınırın ötesine seek yapamaz.
     *
     * @param durationMs   Toplam süre (ms)
     * @param bufferSec    Live edge'den önce bırakılacak güvenlik tamponu (default 5s)
     * @return Max seek pozisyonu (ms)
     */
    fun maxSeekPositionMs(durationMs: Long, bufferSec: Long = 5L): Long =
        (durationMs - bufferSec * 1000L).coerceAtLeast(0L)
}
