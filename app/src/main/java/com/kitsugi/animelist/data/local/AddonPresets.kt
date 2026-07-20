package com.kitsugi.animelist.data.local

object AddonPresets {
    val presets = emptyList<ManagedAddonEntity>()

    /**
     * Varsayılan altyazı addon'ları – ilk kurulumda otomatik olarak
     * isSystem=true / canDisable=false şeklinde eklenir.
     *
     * URL listesi manifest endpoint'leri (Stremio protokolü):
     *  - opensubtitles-v3: 5M+ altyazı, TR dahil, hash tabanlı eşleştirme
     *  - opensubtitles (v2): fallback / geniş format desteği
     *  - yts-subtitles: film altyazıları için özel kaynak
     *  - turkcealtyaziorg: TurkceAltyazi.org içeriği, TR altyazılar için birincil Türkçe kaynak
     *    → manifest doğrulandı: v1.1.1, types=[movie,series], resources=[subtitles] ✅
     */
    val DEFAULT_SUBTITLE_ADDONS = listOf(
        "https://opensubtitles-v3.strem.io",
        "https://yts-subtitles.strem.io",
        // TurkceAltyazi.org – Türkçe altyazı birincil kaynağı (v1.1.1 canlı doğrulandı 2026-07-16)
        "https://5a0d1888fa64-turkcealtyaziorg.baby-beamup.club"
    )

    /**
     * Sadece TR altyazı için özel addon'lar (DEFAULT listesine dahil değil,
     * ayrıca kullanılan grubu temsil eder – UI filtreleme vb. için referans).
     */
    val TR_SUBTITLE_ADDONS = listOf(
        "https://5a0d1888fa64-turkcealtyaziorg.baby-beamup.club"
    )
}

