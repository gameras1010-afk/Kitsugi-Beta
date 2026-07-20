package com.kitsugi.animelist.data.settings

import java.util.Locale

/**
 * V2-F05: AppLanguage
 *
 * Uygulamanın desteklediği arayüz dillerini tanımlar.
 * MoeList AppLanguage.kt referans alındı.
 *
 * Kullanım:
 *   SettingsDataStore.setAppLanguage(AppLanguage.Turkish.code)
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String
) {
    System(
        code = "system",
        displayName = "Sistem Varsayılanı",
        nativeName = "System Default",
        flagEmoji = "🌐"
    ),
    Turkish(
        code = "tr",
        displayName = "Türkçe",
        nativeName = "Türkçe",
        flagEmoji = "🇹🇷"
    ),
    English(
        code = "en",
        displayName = "İngilizce",
        nativeName = "English",
        flagEmoji = "🇬🇧"
    ),
    Japanese(
        code = "ja",
        displayName = "Japonca",
        nativeName = "日本語",
        flagEmoji = "🇯🇵"
    ),
    Korean(
        code = "ko",
        displayName = "Korece",
        nativeName = "한국어",
        flagEmoji = "🇰🇷"
    ),
    ChineseSimplified(
        code = "zh-cn",
        displayName = "Çince (Basit)",
        nativeName = "中文（简体）",
        flagEmoji = "🇨🇳"
    ),
    German(
        code = "de",
        displayName = "Almanca",
        nativeName = "Deutsch",
        flagEmoji = "🇩🇪"
    ),
    French(
        code = "fr",
        displayName = "Fransızca",
        nativeName = "Français",
        flagEmoji = "🇫🇷"
    ),
    Spanish(
        code = "es",
        displayName = "İspanyolca",
        nativeName = "Español",
        flagEmoji = "🇪🇸"
    ),
    Portuguese(
        code = "pt-br",
        displayName = "Portekizce (Brezilya)",
        nativeName = "Português (Brasil)",
        flagEmoji = "🇧🇷"
    ),
    Russian(
        code = "ru",
        displayName = "Rusça",
        nativeName = "Русский",
        flagEmoji = "🇷🇺"
    ),
    Arabic(
        code = "ar",
        displayName = "Arapça",
        nativeName = "العربية",
        flagEmoji = "🇸🇦"
    );

    companion object {
        /** Dil kodundan AppLanguage döner. Bulunamazsa System döner. */
        fun fromCode(code: String): AppLanguage =
            entries.find { it.code == code } ?: System

        /** Sistemin mevcut Locale'ine en yakın AppLanguage'i döner. */
        fun fromSystemLocale(): AppLanguage {
            val systemCode = Locale.getDefault().language
            return entries.find { it.code == systemCode || it.code.startsWith(systemCode) }
                ?: English
        }

        /** Tüm dilleri dropdown için liste olarak döner. */
        fun all(): List<AppLanguage> = entries.toList()

        /** Sistem dışı dilleri döner (manuel seçimler için). */
        fun selectableLanguages(): List<AppLanguage> =
            entries.filter { it != System }
    }

    /** Bu dil için Locale nesnesi döner. */
    fun toLocale(): Locale? = when (this) {
        System          -> null
        ChineseSimplified -> Locale.SIMPLIFIED_CHINESE
        else            -> Locale(code.take(2))
    }
}
