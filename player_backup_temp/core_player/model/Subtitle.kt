package com.kitsugi.animelist.core.player.model

/**
 * S01 – Domain modeli: Stremio altyazı addon'larından gelen her altyazı kaynağı.
 * KitsugiTV-dev SubtitleRepositoryImpl.Subtitle ile birebir eşdeğer.
 */
data class Subtitle(
    /** Addon tarafından dönen benzersiz ID (URL de olabilir) */
    val id: String,
    /** Altyazı dosyasının indirme URL'si */
    val url: String,
    /** Dil kodu: "tr", "en", "Turkish", "Türkçe" vb. – normalize edilmemiş ham değer */
    val lang: String,
    /** Addon adı (gösterim için) */
    val addonName: String,
    /** Addon logo URL'si (gösterim için, nullable) */
    val addonLogo: String? = null,
    /** Format tahmini: "srt", "ass", "vtt" vb. – URL'den tahmin edilir */
    val format: String? = null
)
