package com.kitsugi.animelist.core.player

/**
 * T1.7 – PlayerNextEpisodeRules
 *
 * KitsugiTV-dev port: PlayerNextEpisodeRules benzeri.
 * Bölüm biterken sonraki bölümün nasıl başlatılacağına dair karar mantığı:
 * - Kredi/outro süresi kısaysa → otomatik oynat
 * - Skip-intro verisi mevcutsa → bölüm geçişini farklı işle
 * - Binge prompt gerekiyorsa → countdown göster
 *
 * Bu sınıf **pure logic** içerir, UI state tutmaz.
 */
object PlayerNextEpisodeRules {

    /**
     * Bölüm sona yaklaşırken (son [thresholdSec] saniye) ne yapılacağını belirler.
     *
     * @param durationMs          Toplam bölüm süresi (ms)
     * @param positionMs          Anlık oynatma pozisyonu (ms)
     * @param thresholdSec        Kaç saniye kala tetiklensin (varsayılan 30)
     * @param hasOutroSkip        Outro/Credits skip bilgisi var mı?
     * @param outroStartSec       Outro başlangıcı (saniye), null = bilinmiyor
     * @param isAutoPlayEnabled   Kullanıcı ayarında autoplay açık mı?
     * @param sessionLimitReached Bu oturum limiti aşıldı mı?
     * @return [NextEpisodeAction] — ne yapılacağına dair karar
     */
    fun evaluate(
        durationMs: Long,
        positionMs: Long,
        thresholdSec: Int = 30,
        hasOutroSkip: Boolean = false,
        outroStartSec: Long? = null,
        isAutoPlayEnabled: Boolean = true,
        sessionLimitReached: Boolean = false
    ): NextEpisodeAction {
        if (durationMs <= 0L) return NextEpisodeAction.None

        val remainingSec = ((durationMs - positionMs) / 1000L).coerceAtLeast(0L)

        // Henüz eşik gelmedi
        if (remainingSec > thresholdSec) return NextEpisodeAction.None

        // Autoplay kapalı → sadece manuel "Sonraki" butonu göster
        if (!isAutoPlayEnabled) return NextEpisodeAction.ShowNextButton

        // Oturum limiti aşıldı → StillWatching prompt'u göster
        if (sessionLimitReached) return NextEpisodeAction.ShowStillWatchingPrompt

        // Outro varsa ve pozisyon outro başlangıcını geçtiyse → hemen autoplay
        if (hasOutroSkip && outroStartSec != null) {
            val outroStartMs = outroStartSec * 1000L
            if (positionMs >= outroStartMs) {
                return NextEpisodeAction.AutoPlayImmediate
            }
        }

        // Krediler çok kısaysa (< 30 sn) → countdown ile autoplay
        val creditsSec = remainingSec
        return if (creditsSec <= 30L) {
            NextEpisodeAction.AutoPlayWithCountdown(countdownSec = creditsSec.toInt().coerceAtLeast(5))
        } else {
            NextEpisodeAction.ShowBingeCard(countdownSec = 10)
        }
    }
}

/**
 * PlayerNextEpisodeRules.evaluate() sonucu.
 */
sealed interface NextEpisodeAction {
    /** Henüz bir şey yapma */
    object None : NextEpisodeAction

    /** Sadece "Sonraki Bölüm" butonunu göster, otomatik geçiş yok */
    object ShowNextButton : NextEpisodeAction

    /** Hemen otomatik oynat (outro atlanmış) */
    object AutoPlayImmediate : NextEpisodeAction

    /** Geri sayım ile otomatik oynat */
    data class AutoPlayWithCountdown(val countdownSec: Int) : NextEpisodeAction

    /** Binge kart göster (10 sn countdown, iptal edilebilir) */
    data class ShowBingeCard(val countdownSec: Int = 10) : NextEpisodeAction

    /** Oturum limiti aşıldı — StillWatching dialog göster */
    object ShowStillWatchingPrompt : NextEpisodeAction
}
