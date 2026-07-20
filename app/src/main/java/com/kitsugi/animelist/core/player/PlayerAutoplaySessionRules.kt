package com.kitsugi.animelist.core.player

/**
 * T1.7 – PlayerAutoplaySessionRules
 *
 * KitsugiTV-dev port: PlayerAutoplaySessionRules benzeri.
 * Bir oturumda kaç bölüm art arda izlenince StillWatching / AutoPlay
 * prompt'unun tetikleneceğini yönetir.
 *
 * @param sessionLimit 0 = sınırsız; N = N bölüm sonra prompt ver
 */
class PlayerAutoplaySessionRules(
    private val sessionLimit: Int = 0
) {

    /** Bu oturumda kaç bölüm başarıyla oynatıldı */
    private var episodesWatched: Int = 0

    /**
     * Bir bölüm tamamlandığında çağrılır.
     * @return true → oturum limiti aşıldı, stillWatching/prompt göster
     */
    fun onEpisodeCompleted(): Boolean {
        episodesWatched++
        if (sessionLimit <= 0) return false // sınırsız
        return episodesWatched >= sessionLimit
    }

    /**
     * Kullanıcı "devam et" dedi → sayacı sıfırla.
     * StillWatching yanıtlandıktan sonra çağrılmalı.
     */
    fun resetSession() {
        episodesWatched = 0
    }

    /** Şu anda kaç bölüm izlendi */
    fun watchedCount(): Int = episodesWatched

    /** Oturum limiti aktif mi? */
    fun isLimited(): Boolean = sessionLimit > 0

    /** Kaç bölüm kaldı — sınırsızsa null */
    fun episodesUntilPrompt(): Int? {
        if (sessionLimit <= 0) return null
        return (sessionLimit - episodesWatched).coerceAtLeast(0)
    }
}
