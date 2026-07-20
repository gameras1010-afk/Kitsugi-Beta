package com.kitsugi.animelist.core.player

import java.util.Locale

/**
 * FP-28 – PlayerSubtitleHelper
 *
 * Altyazı dil kodlamalarını normalize etme, URL temizleme, IETF tag oluşturma
 * ve altyazı durum takibi işlevlerini sunan yardımcı sınıf.
 */
object PlayerSubtitleHelper {

    enum class SubtitleStatus {
        WAIT_UPLOADING,
        UPLOADED,
        FAILED,
        CACHED,
        LOADING,
        LOADED
    }

    /**
     * Altyazı URL'sinden benzersiz bir ID üretir.
     */
    fun getId(url: String): String {
        return url.hashCode().toString()
    }

    /**
     * Verilen ham dil string'inin hedef dil kodu ile eşleşip eşleşmediğini kontrol eder.
     */
    fun matchesLanguageCode(rawLang: String, targetCode: String): Boolean {
        return PlayerSubtitleUtils.matchesLanguageCode(rawLang, targetCode)
    }

    /**
     * Dil isminden veya kodundan standart IETF BCP-47 / IETF dil etiketini döner (örn: "tr", "en", "ja").
     */
    fun getIETF_tag(lang: String): String {
        val lower = lang.lowercase(Locale.ROOT).trim()
        return when {
            lower.contains("turk") || lower == "tr" || lower == "tur" -> "tr"
            lower.contains("eng") || lower == "en" -> "en"
            lower.contains("jap") || lower == "ja" || lower == "jpn" -> "ja"
            lower.contains("ger") || lower == "de" || lower == "deu" -> "de"
            lower.contains("fra") || lower == "fr" || lower == "fre" -> "fr"
            lower.contains("spa") || lower == "es" -> "es"
            lower.contains("rus") || lower == "ru" -> "ru"
            lower.contains("ara") || lower == "ar" -> "ar"
            lower.contains("kor") || lower == "ko" -> "ko"
            lower.contains("chi") || lower == "zh" || lower.contains("zho") -> "zh"
            lower.length == 2 -> lower
            lower.length == 3 -> lower.substring(0, 2)
            else -> "en"
        }
    }

    /**
     * Altyazı indirme URL'sini temizler ve normalize eder.
     */
    fun getFixedUrl(url: String): String {
        return url.trim()
    }
}
