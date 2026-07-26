package com.kitsugi.animelist.core.player

/**
 * T1.8: StreamAutoPlayPolicy
 *
 * CloudStream StreamAutoPlayPolicy referansından adapte edildi.
 * Bir stream listesi geldiğinde otomatik oynatma için politikayı belirler.
 */
enum class StreamAutoPlayPolicy(val displayName: String) {
    /**
     * Otomatik oynatma yok — kullanıcı kaynağı seçer.
     */
    DISABLED("Manuel Seçim"),

    /**
     * Kalite profiline göre en iyi stream'i otomatik seç ve oynat.
     */
    BEST_QUALITY("En İyi Kalite"),

    /**
     * İlk gelen stream'i doğrudan oynat (en hızlı yükleme).
     */
    FIRST_AVAILABLE("İlk Kaynak"),

    /**
     * Son izlenen kaynağı hatırla ve aynı provider/quality'yi tercih et.
     */
    REMEMBER_LAST("Son Kullanılan");

    companion object {
        fun fromOrdinal(ordinal: Int): StreamAutoPlayPolicy =
            entries.getOrElse(ordinal) { DISABLED }
    }
}
