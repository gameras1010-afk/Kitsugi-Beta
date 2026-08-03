package com.kitsugi.animelist.core.player

/**
 * S04 – Dil eşleşme yardımcısı.
 *
 * KitsugiTV-dev PlayerSubtitleUtils.matchesLanguageCode() portu.
 * Stremio addon'larından gelen ham dil kodlarını normalize ederek
 * kullanıcının tercih ettiği dil listesiyle karşılaştırır.
 *
 * Örnekler:
 *  - "tr", "tur", "Türkçe", "Turkish", "Tur" → "tr" ile eşleşir
 *  - "en", "eng", "English", "İngilizce"     → "en" ile eşleşir
 *  - "jp", "jpn", "Japanese", "ja"            → "ja" ile eşleşir
 */
object PlayerSubtitleUtils {

    /** BCP-47 2-harf kodu → eşleşen ham string setleri (küçük harf) */
    private val LANG_ALIASES: Map<String, Set<String>> = mapOf(
        "tr" to setOf("tr", "tur", "türkçe", "turkce", "turkish", "turk", "turkish language"),
        "en" to setOf("en", "eng", "english", "ingilizce", "english language"),
        "ja" to setOf("ja", "jp", "jpn", "japanese", "japonca", "日本語"),
        "ar" to setOf("ar", "ara", "arabic", "arapça"),
        "de" to setOf("de", "deu", "ger", "german", "almanca"),
        "fr" to setOf("fr", "fra", "fre", "french", "fransızca"),
        "es" to setOf("es", "spa", "spanish", "ispanyolca"),
        "it" to setOf("it", "ita", "italian", "italyanca"),
        "pt" to setOf("pt", "por", "portuguese", "portekizce"),
        "ru" to setOf("ru", "rus", "russian", "rusça"),
        "ko" to setOf("ko", "kor", "korean", "korece"),
        "zh" to setOf("zh", "chi", "zho", "chinese", "çince", "中文"),
        "nl" to setOf("nl", "nld", "dutch", "flemish"),
        "pl" to setOf("pl", "pol", "polish"),
        "sv" to setOf("sv", "swe", "swedish"),
        "no" to setOf("no", "nor", "norwegian"),
        "da" to setOf("da", "dan", "danish"),
        "fi" to setOf("fi", "fin", "finnish"),
        "cs" to setOf("cs", "cze", "ces", "czech"),
        "hu" to setOf("hu", "hun", "hungarian"),
        "ro" to setOf("ro", "ron", "rum", "romanian"),
        "el" to setOf("el", "gre", "ell", "greek"),
        "he" to setOf("he", "heb", "hebrew"),
        "fa" to setOf("fa", "per", "fas", "persian"),
        "id" to setOf("id", "ind", "indonesian"),
        "ms" to setOf("ms", "msa", "malay"),
        "th" to setOf("th", "tha", "thai"),
        "vi" to setOf("vi", "vie", "vietnamese")
    )

    /**
     * Verilen ham dil string'inin [targetLangCode] ile eşleşip eşleşmediğini döner.
     *
     * @param rawLang    Addon'dan gelen ham dil string'i (örn: "Türkçe", "tr", "tur")
     * @param targetCode Karşılaştırılacak BCP-47 kodu (örn: "tr", "en")
     */
    fun matchesLanguageCode(rawLang: String, targetCode: String): Boolean {
        val normalizedRaw = rawLang.trim().lowercase()
        val normalizedTarget = targetCode.trim().lowercase()

        // Doğrudan eşleşme
        if (normalizedRaw == normalizedTarget) return true

        // Alias tablosundan karşılaştırma
        val targetAliases = LANG_ALIASES[normalizedTarget] ?: setOf(normalizedTarget)
        if (normalizedRaw in targetAliases) return true

        // Hedef kodun hangi alias grubunda olduğunu da kontrol et
        // (örn: rawLang = "tr", targetCode = "tur" → tur grubuna bak)
        val rawCode = LANG_ALIASES.entries.firstOrNull { (_, aliases) ->
            normalizedRaw in aliases
        }?.key

        if (rawCode != null) {
            val targetNormalized = LANG_ALIASES.entries.firstOrNull { (_, aliases) ->
                normalizedTarget in aliases
            }?.key ?: normalizedTarget
            return rawCode == targetNormalized
        }

        return false
    }

    /**
     * Verilen altyazı listesini tercih listesine göre sıralar.
     * Önce tercih edilen diller (sırasıyla), sonra diğerleri.
     *
     * @param subtitles      Sıralanacak altyazı listesi (Subtitle domain modeli)
     * @param preferredLangs Tercih sırası (BCP-47): ["tr", "en"] gibi
     */
    fun <T> sortByPreference(
        subtitles: List<T>,
        preferredLangs: List<String>,
        getLang: (T) -> String
    ): List<T> {
        if (preferredLangs.isEmpty()) return subtitles
        return subtitles.sortedWith(Comparator { a, b ->
            val aIdx = preferredLangs.indexOfFirst { matchesLanguageCode(getLang(a), it) }
                .let { if (it == -1) Int.MAX_VALUE else it }
            val bIdx = preferredLangs.indexOfFirst { matchesLanguageCode(getLang(b), it) }
                .let { if (it == -1) Int.MAX_VALUE else it }
            aIdx.compareTo(bIdx)
        })
    }

    /**
     * KitsugiPlayerViewModel'ın aradığı özel tipteki (SubtitleInput) sıralama metodu.
     */
    fun sortSubtitlesByPreference(
        subtitles: List<SubtitleInput>,
        preferredLangs: List<String>
    ): List<SubtitleInput> {
        return sortByPreference(subtitles, preferredLangs) { it.lang ?: "" }
    }

    /**
     * Listeden ilk eşleşen tercih edilen altyazıyı seç.
     * Önce "tr", bulamazsa "en", hiçbiri yoksa ilk altyazı.
     */
    fun <T> autoSelect(
        subtitles: List<T>,
        preferredLangs: List<String>,
        getLang: (T) -> String
    ): T? {
        if (subtitles.isEmpty()) return null
        for (lang in preferredLangs) {
            val match = subtitles.firstOrNull { matchesLanguageCode(getLang(it), lang) }
            if (match != null) return match
        }
        return subtitles.firstOrNull()
    }

    /**
     * Dil kodunu veya ham dil adını kullanıcı dostu Türkçe dil adına çevirir.
     */
    fun getFriendlyLanguageName(lang: String): String {
        val normalized = lang.trim().lowercase()
        return when {
            matchesLanguageCode(normalized, "tr") -> "Türkçe"
            matchesLanguageCode(normalized, "en") -> "İngilizce"
            matchesLanguageCode(normalized, "ja") -> "Japonca"
            matchesLanguageCode(normalized, "ar") -> "Arapça"
            matchesLanguageCode(normalized, "de") -> "Almanca"
            matchesLanguageCode(normalized, "fr") -> "Fransızca"
            matchesLanguageCode(normalized, "es") -> "İspanyolca"
            matchesLanguageCode(normalized, "it") -> "İtalyanca"
            matchesLanguageCode(normalized, "pt") -> "Portekizce"
            matchesLanguageCode(normalized, "ru") -> "Rusça"
            matchesLanguageCode(normalized, "ko") -> "Korece"
            matchesLanguageCode(normalized, "zh") -> "Çince"
            else -> lang.uppercase()
        }
    }

    /** Verilen dil string'inin Türkçe olup olmadığını döner. */
    fun isTurkishLang(lang: String?): Boolean {
        if (lang.isNullOrBlank()) return false
        return matchesLanguageCode(lang.trim(), "tr")
    }

    /**
     * SubtitleInput listesinden en iyi altyazıyı seçer.
     *
     * Öncelik sırası:
     *  1. Dahili (isExternal=false) Türkçe altyazı
     *  2. Harici/addon (isExternal=true) Türkçe altyazı
     *  3. Kullanıcı tercih listesindeki ilk eşleşme (sırasıyla)
     *  4. Listedeki ilk altyazı (fallback)
     *
     * @param subtitles      Aranacak SubtitleInput listesi
     * @param preferredLangs Kullanıcı tercih dil listesi (BCP-47, örn: ["tr", "en"])
     */
    fun findBestSubtitleInput(
        subtitles: List<SubtitleInput>,
        preferredLangs: List<String>
    ): SubtitleInput? {
        if (subtitles.isEmpty()) return null

        // 1. Dahili (gömülü) Türkçe
        subtitles.firstOrNull { !it.isExternal && isTurkishLang(it.lang) }?.let { return it }

        // 2. Harici/addon Türkçe
        subtitles.firstOrNull { it.isExternal && isTurkishLang(it.lang) }?.let { return it }

        // 3. Kullanıcı tercih listesi sırasıyla
        for (lang in preferredLangs) {
            subtitles.firstOrNull { matchesLanguageCode(it.lang, lang) }?.let { return it }
        }

        // 4. Fallback: ilk altyazı
        return subtitles.firstOrNull()
    }

    /**
     * MPV track snapshot altyazılarından en iyi parçanın ID'sini seçer.
     *
     * Öncelik sırası:
     *  1. Dahili (isExternal=false) Türkçe altyazı parçası
     *  2. Harici (isExternal=true) Türkçe altyazı parçası
     *  3. Kullanıcı tercih listesindeki ilk eşleşme
     *  4. İlk mevcut parça (fallback)
     *
     * @param subtitleTracks MPV track snapshot altyazı listesi (MpvTrack)
     * @param preferredLangs Kullanıcı tercih dil listesi
     */
    fun findBestMpvSubtitleTrack(
        subtitleTracks: List<com.kitsugi.animelist.core.player.engine.MpvTrack>,
        preferredLangs: List<String>
    ): com.kitsugi.animelist.core.player.engine.MpvTrack? {
        if (subtitleTracks.isEmpty()) return null

        // 1. Dahili Türkçe
        subtitleTracks.firstOrNull { !it.isExternal && isTurkishLang(it.language) }?.let { return it }

        // 2. Harici Türkçe
        subtitleTracks.firstOrNull { it.isExternal && isTurkishLang(it.language) }?.let { return it }

        // 3. Kullanıcı tercih listesi
        for (lang in preferredLangs) {
            subtitleTracks.firstOrNull { matchesLanguageCode(it.language ?: "", lang) }?.let { return it }
        }

        // 4. Fallback
        return subtitleTracks.firstOrNull()
    }

    /**
     * MPV track snapshot ses parçalarından en iyi parçanın ID'sini seçer.
     *
     * Öncelik sırası:
     *  1. Türkçe ses parçası
     *  2. İlk mevcut ses parçası (fallback)
     */
    fun findBestMpvAudioTrack(
        audioTracks: List<com.kitsugi.animelist.core.player.engine.MpvTrack>,
        preferredLangs: List<String>
    ): com.kitsugi.animelist.core.player.engine.MpvTrack? {
        if (audioTracks.isEmpty()) return null

        // 1. Türkçe ses
        audioTracks.firstOrNull { isTurkishLang(it.language) }?.let { return it }

        // 2. Kullanıcı tercih listesi
        for (lang in preferredLangs) {
            audioTracks.firstOrNull { matchesLanguageCode(it.language ?: "", lang) }?.let { return it }
        }

        // 3. Fallback
        return audioTracks.firstOrNull()
    }
}
