package com.kitsugi.animelist.ui.utils

import java.util.Locale

/**
 * V2-B02: LanguageUtils
 *
 * Dil kodlarını (ISO 639-1/2) okunabilir Türkçe isimlere çevirir.
 * Altyazı / ses dili etiketleme için kullanılır.
 * NuvioTV LanguageUtils.kt referans alındı.
 */
object LanguageUtils {

    /**
     * ISO 639-1 dil kodunu Türkçe dil adına çevirir.
     * "tr" → "Türkçe", "en" → "İngilizce", "ja" → "Japonca"
     */
    fun displayName(langCode: String?): String {
        if (langCode.isNullOrBlank()) return "Bilinmiyor"
        val code = langCode.lowercase().trim()
        return languageMap[code]
            ?: runCatching { Locale(code).getDisplayLanguage(Locale("tr")) }
                .getOrNull()
                ?.takeIf { it != code && it.isNotBlank() }
            ?: langCode
    }

    /**
     * Dil kodunu bayrak emoji'ye çevirir.
     * "tr" → "🇹🇷", "en" → "🇬🇧", "ja" → "🇯🇵"
     */
    fun flagEmoji(langCode: String?): String {
        if (langCode.isNullOrBlank()) return "🌐"
        return flagMap[langCode.lowercase().trim()] ?: "🌐"
    }

    /**
     * Ses listesini dil adıyla etiketlenmiş string'e çevirir.
     * ["tr","en"] → "Türkçe, İngilizce"
     */
    fun formatList(langCodes: List<String>): String =
        langCodes.map { displayName(it) }.joinToString(", ")

    private val languageMap: Map<String, String> = mapOf(
        "tr"   to "Türkçe",
        "en"   to "İngilizce",
        "ja"   to "Japonca",
        "ko"   to "Korece",
        "zh"   to "Çince",
        "zh-cn" to "Çince (Basitleştirilmiş)",
        "zh-tw" to "Çince (Geleneksel)",
        "ar"   to "Arapça",
        "de"   to "Almanca",
        "fr"   to "Fransızca",
        "es"   to "İspanyolca",
        "it"   to "İtalyanca",
        "pt"   to "Portekizce",
        "pt-br" to "Portekizce (Brezilya)",
        "ru"   to "Rusça",
        "pl"   to "Lehçe",
        "nl"   to "Hollandaca",
        "sv"   to "İsveççe",
        "no"   to "Norveççe",
        "da"   to "Danimarkaca",
        "fi"   to "Fince",
        "cs"   to "Çekçe",
        "hu"   to "Macarca",
        "ro"   to "Rumence",
        "el"   to "Yunanca",
        "he"   to "İbranice",
        "th"   to "Tayca",
        "vi"   to "Vietnamca",
        "id"   to "Endonezce",
        "ms"   to "Malayca",
        "hi"   to "Hintçe",
        "bn"   to "Bengalce",
        "uk"   to "Ukraynaca",
        "bg"   to "Bulgarca",
        "hr"   to "Hırvatça",
        "sk"   to "Slovakça",
        "sr"   to "Sırpça",
        "lt"   to "Litvanca",
        "lv"   to "Letonca",
        "et"   to "Estonca",
        "ca"   to "Katalanca",
        "fa"   to "Farsça"
    )

    private val flagMap: Map<String, String> = mapOf(
        "tr" to "🇹🇷",
        "en" to "🇬🇧",
        "ja" to "🇯🇵",
        "ko" to "🇰🇷",
        "zh" to "🇨🇳",
        "ar" to "🇸🇦",
        "de" to "🇩🇪",
        "fr" to "🇫🇷",
        "es" to "🇪🇸",
        "it" to "🇮🇹",
        "pt" to "🇵🇹",
        "ru" to "🇷🇺",
        "pl" to "🇵🇱",
        "nl" to "🇳🇱",
        "sv" to "🇸🇪",
        "no" to "🇳🇴",
        "da" to "🇩🇰",
        "fi" to "🇫🇮",
        "hu" to "🇭🇺",
        "ro" to "🇷🇴",
        "el" to "🇬🇷",
        "he" to "🇮🇱",
        "th" to "🇹🇭",
        "vi" to "🇻🇳",
        "id" to "🇮🇩",
        "hi" to "🇮🇳",
        "uk" to "🇺🇦"
    )
}
