package com.kitsugi.animelist.core.player

import kotlin.math.abs

/**
 * T1.4 – PlayerFrameRateHeuristics
 *
 * Video frame rate tespitinde kenar durumlarını ele alan sezgisel mantık.
 *
 * 23.976 fps ile 24.000 fps arasındaki belirsizliği çözmek için
 * probe (NextLib/extractor) sonucunu kontrol eder ve track metadata'sıyla
 * karşılaştırır. Eğer track metadata "ambiguous cinema 24" aralığındaysa ve
 * probe tam olarak 23.976 (NTSC film) döndürüyorsa, probe sonucuna inanır.
 *
 * KitsugiTV'deki [PlayerRuntimeControllerAfrPreflight] mantığından adapte edilmiştir;
 * PlayerUiState bağımlılığı kaldırılarak bağımsız utility hâline getirilmiştir.
 */
internal object PlayerFrameRateHeuristics {

    // 24.000 fps ile yakınlık belirlemek için geniş pencere
    private const val AMBIGUOUS_CINEMA_TRACK_MIN = 23.95f
    private const val AMBIGUOUS_CINEMA_TRACK_MAX = 24.05f

    // Probe ile track arasında bu kadar fark varsa override gerekebilir
    private const val FRAME_RATE_CORRECTION_EPSILON = 0.015f

    // NTSC film: 24000/1001 ≈ 23.9760
    private const val NTSC_FILM_FPS = 24000f / 1001f

    /**
     * Verilen fps değeri "ambiguous cinema 24" aralığında mı?
     * Yani yaklaşık 23.976 veya 24.000 olabilir mi?
     */
    fun isAmbiguousCinema24(value: Float): Boolean {
        return value in AMBIGUOUS_CINEMA_TRACK_MIN..AMBIGUOUS_CINEMA_TRACK_MAX
    }

    /**
     * Track metadata belirsiz (≈24 fps) iken probe'un NTSC film (≈23.976)
     * olduğunu bildirdiği durumda override gerekip gerekmediğini döner.
     *
     * @param trackFrameRateRaw   Track'tan gelen ham fps (0 ise [trackFrameRate] kullanılır)
     * @param trackFrameRate      Track'tan gelen snapped/reported fps
     * @param probeDetection      NextLib/extractor probe sonucu
     */
    fun shouldProbeOverrideTrack(
        trackFrameRateRaw: Float,
        trackFrameRate: Float,
        probeDetection: FrameRateUtils.FrameRateDetection
    ): Boolean {
        // Track'tan sağlıklı bir kaynak yoksa override gerekmez
        if (trackFrameRate <= 0f) return false

        val trackRaw = if (trackFrameRateRaw > 0f) trackFrameRateRaw else trackFrameRate
        val trackIsAmbiguous = isAmbiguousCinema24(trackRaw) || isAmbiguousCinema24(trackFrameRate)
        if (!trackIsAmbiguous) return false

        val probeIsNtscFilm = abs(probeDetection.snapped - NTSC_FILM_FPS) < 0.01f
        val differsEnough = abs(probeDetection.snapped - trackFrameRate) > FRAME_RATE_CORRECTION_EPSILON
        return probeIsNtscFilm && differsEnough
    }
}
