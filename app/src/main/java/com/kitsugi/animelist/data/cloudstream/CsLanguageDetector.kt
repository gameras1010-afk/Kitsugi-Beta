package com.kitsugi.animelist.data.cloudstream

import java.util.Locale

/**
 * Altyazı dil kodu tespiti.
 * [CsStreamRunner] içindeki `detectLanguageCode` mantığı buraya taşındı.
 */
internal object CsLanguageDetector {

    /**
     * Verilen dil adı string'inden BCP-47 uyumlu dil kodu döndürür.
     *
     * Örnek: "Turkish" → "tr", "English" → "en", "Türkçe" → "tr"
     *
     * @param lang Altyazı dosyasından gelen dil adı (herhangi bir formatta olabilir)
     * @return BCP-47 dil kodu ("tr", "en" vb.)
     */
    fun detectLanguageCode(lang: String): String {
        val lower = lang.lowercase(Locale.ROOT)
        return when {
            lower.contains("tr") || lower.contains("turk") || lower.contains("türk") -> "tr"
            lower.contains("en") || lower.contains("eng") -> "en"
            else -> "tr" // Turkish anime sites default to Turkish
        }
    }
}
