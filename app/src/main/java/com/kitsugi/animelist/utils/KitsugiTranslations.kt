package com.kitsugi.animelist.utils

import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.data.remote.KitsugiVoiceActor

// ─── Dil Yardımcısı ────────────────────────────────────────────────────────────

/**
 * Cihazın aktif dili Türkçe mi? Değilse tüm çeviri fonksiyonları orijinal metni döner.
 */
fun isTurkish(): Boolean = java.util.Locale.getDefault().language.startsWith("tr")

/**
 * Kitsugi Türkçe Çeviri Eşleme Yardımcıları
 *
 * Bu dosya, API kaynaklarından (Jikan/MAL, AniList, TMDB) gelen
 * sabit İngilizce değerleri Türkçeye çeviren saf (pure) eşleme fonksiyonlarını içerir.
 * Google Translate KULLANILMAZ — tüm eşlemeler statik tablolardır.
 */

// ─── Tür / Genre Çevirisi ──────────────────────────────────────────────────────

private val genreMap = mapOf(
    // Temel Türler
    "Action" to "Aksiyon",
    "Adventure" to "Macera",
    "Comedy" to "Komedi",
    "Drama" to "Drama",
    "Fantasy" to "Fantezi",
    "Horror" to "Korku",
    "Mystery" to "Gizem",
    "Romance" to "Romantik",
    "Sci-Fi" to "Bilim Kurgu",
    "Science Fiction" to "Bilim Kurgu",
    "Slice of Life" to "Günlük Yaşam",
    "Sports" to "Spor",
    "Supernatural" to "Doğaüstü",
    "Thriller" to "Gerilim",
    "Psychological" to "Psikolojik",
    "Ecchi" to "Ecchi",
    "Hentai" to "Hentai",
    "Erotica" to "Erotik",
    "Mecha" to "Meka",
    "Music" to "Müzik",
    "Historical" to "Tarihi",
    "Military" to "Askeri",
    "Space" to "Uzay",
    "Magic" to "Büyü",
    "Martial Arts" to "Dövüş Sanatları",
    "School" to "Okul",
    "Game" to "Oyun",
    "Racing" to "Yarış",
    "Parody" to "Parodi",
    "Samurai" to "Samuray",
    "Vampire" to "Vampir",
    "Demons" to "İblisler",
    "Josei" to "Josei",
    "Seinen" to "Seinen",
    "Shounen" to "Shōnen",
    "Shoujo" to "Shōjo",
    "Kids" to "Çocuklar",
    "Avant Garde" to "Avant-Garde",
    "Award Winning" to "Ödüllü",
    "Boys Love" to "Boys Love",
    "Girls Love" to "Girls Love",
    "Suspense" to "Gerilim",
    "Gourmet" to "Gurme",
    "Work Life" to "İş Hayatı",
    "Adult Cast" to "Yetişkin Kadro",
    "Anthropomorphic" to "Antropomorfik",
    "CGDCT" to "CGDCT",
    "Childcare" to "Çocuk Bakımı",
    "Combat Sports" to "Dövüş Sporları",
    "Crossdressing" to "Karşı Cins Kıyafeti",
    "Delinquents" to "Asi Gençler",
    "Detective" to "Dedektif",
    "Educational" to "Eğitici",
    "Erotica" to "Erotik",
    "Food" to "Yemek",
    "Gore" to "Kan ve Şiddet",
    "Harem" to "Harem",
    "High Stakes Game" to "Yüksek Riskli Oyun",
    "Isekai" to "İsekai",
    "Iyashikei" to "İyaşikei",
    "Love Polygon" to "Aşk Çokgeni",
    "Magical Sex Shift" to "Büyülü Cinsiyet Değişimi",
    "Mahou Shoujo" to "Sihirli Kız",
    "Medical" to "Tıp",
    "Mythology" to "Mitoloji",
    "Organized Crime" to "Organize Suç",
    "Otaku Culture" to "Otaku Kültürü",
    "Performing Arts" to "Sahne Sanatları",
    "Pets" to "Evcil Hayvanlar",
    "Reincarnation" to "Reenkarnasyon",
    "Reverse Harem" to "Ters Harem",
    "Romantic Subtext" to "Romantik Alt Metin",
    "Showbiz" to "Şov Dünyası",
    "Strategy Game" to "Strateji Oyunu",
    "Super Power" to "Süper Güç",
    "Survival" to "Hayatta Kalma",
    "Team Sports" to "Takım Sporu",
    "Time Travel" to "Zaman Yolculuğu",
    "Video Game" to "Video Oyunu",
    "Visual Arts" to "Görsel Sanatlar",
    "Workplace" to "İş Yeri",
    "Mythology" to "Mitoloji",
)

/** API'dan gelen tür adını Türkçeye çevirir. Eşleme yoksa orijinali döner. */
fun String.toTurkishGenre(): String = if (!isTurkish()) this else genreMap[this] ?: this

/** Tür listesini Türkçeye çevirir. */
fun List<String>.toTurkishGenres(): List<String> = map { it.toTurkishGenre() }

// Tersine dönüşüm: Türkçe → İngilizce (arama API'ları için)
private val reverseGenreMap: Map<String, String> by lazy {
    genreMap.entries.associate { (en, tr) -> tr to en }
}

/**
 * Türkçe veya İngilizce tür adını İngilizce API adına dönüştürür.
 * "Aksiyon" → "Action", "Action" → "Action"
 */
fun String.toEnglishGenreForSearch(): String = reverseGenreMap[this] ?: this

// ─── Yayın Durumu / Status ─────────────────────────────────────────────────────

private val statusMap = mapOf(
    // Jikan / MAL
    "Finished Airing" to "Yayın Tamamlandı",
    "Currently Airing" to "Yayında",
    "Not yet aired" to "Henüz Yayınlanmadı",
    "Finished" to "Tamamlandı",
    "Publishing" to "Yayımlanıyor",
    "Discontinued" to "Yayın Durduruldu",
    "On Hiatus" to "Ara Verildi",
    // AniList
    "FINISHED" to "Yayın Tamamlandı",
    "RELEASING" to "Yayında",
    "NOT_YET_RELEASED" to "Henüz Yayınlanmadı",
    "CANCELLED" to "İptal Edildi",
    "HIATUS" to "Ara Verildi",
    // TMDB
    "Ended" to "Yayın Tamamlandı",
    "Returning Series" to "Devam Eden Dizi",
    "In Production" to "Yapımda",
    "Planned" to "Planlandı",
    "Canceled" to "İptal Edildi",
    "Pilot" to "Pilot",
    "Released" to "Yayınlandı",
)

fun String.toTurkishStatus(): String = if (!isTurkish()) this else statusMap[this] ?: this

// ─── Mevsim / Season ──────────────────────────────────────────────────────────

private val seasonNameMap = mapOf(
    "Winter" to "Kış",
    "WINTER" to "Kış",
    "Spring" to "İlkbahar",
    "SPRING" to "İlkbahar",
    "Summer" to "Yaz",
    "SUMMER" to "Yaz",
    "Fall" to "Sonbahar",
    "FALL" to "Sonbahar",
)

/**
 * "Winter 2024" veya "SPRING" gibi sezon metinlerini Türkçeye çevirir.
 * Yıl varsa korunur: "Kış 2024"
 */
fun String.toTurkishSeason(): String {
    if (!isTurkish()) return this
    val parts = this.trim().split(" ")
    val seasonPart = parts.getOrNull(0) ?: return this
    val yearPart = parts.getOrNull(1)
    val translated = seasonNameMap[seasonPart] ?: seasonPart
    return if (yearPart != null) "$translated $yearPart" else translated
}

// ─── Kaynak Materyal / Source Material ────────────────────────────────────────

private val sourceMaterialMap = mapOf(
    // Jikan / MAL
    "4-koma manga" to "4-Koma Manga",
    "Book" to "Kitap",
    "Card game" to "Kart Oyunu",
    "Game" to "Oyun",
    "Light novel" to "Hafif Roman",
    "Manga" to "Manga",
    "Mixed media" to "Karma Medya",
    "Music" to "Müzik",
    "Novel" to "Roman",
    "Original" to "Orijinal",
    "Other" to "Diğer",
    "Picture book" to "Resimli Kitap",
    "Radio" to "Radyo",
    "Unknown" to "Bilinmiyor",
    "Visual novel" to "Görsel Roman",
    "Web manga" to "Web Manga",
    "Web novel" to "Web Roman",
    // AniList
    "ORIGINAL" to "Orijinal",
    "MANGA" to "Manga",
    "LIGHT_NOVEL" to "Hafif Roman",
    "VISUAL_NOVEL" to "Görsel Roman",
    "VIDEO_GAME" to "Video Oyunu",
    "OTHER" to "Diğer",
    "NOVEL" to "Roman",
    "DOUJINSHI" to "Doujinshi",
    "ANIME" to "Anime",
    "ONE_SHOT" to "Tek Bölüm",
    "WEB_NOVEL" to "Web Roman",
    "LIVE_ACTION" to "Canlı Yayın",
    "GAME" to "Oyun",
    "COMIC" to "Çizgi Roman",
    "MULTIMEDIA_PROJECT" to "Çoklu Medya Projesi",
    "PICTURE_BOOK" to "Resimli Kitap",
    "MUSIC" to "Müzik",
)

fun String.toTurkishSourceMaterial(): String = if (!isTurkish()) this else sourceMaterialMap[this] ?: this

// ─── Yaş Sınırı / Rating ──────────────────────────────────────────────────────

private val ratingMap = mapOf(
    "G - All Ages" to "G - Her Yaştan",
    "PG - Children" to "PG - Çocuklar",
    "PG-13 - Teens 13 or older" to "PG-13 - 13+ Gençler",
    "R - 17+ (violence & profanity)" to "R - 17+ (Şiddet & Küfür)",
    "R+ - Mild Nudity" to "R+ - Hafif Müstehcenlik",
    "Rx - Hentai" to "Rx - Hentai",
)

fun String.toTurkishRating(): String = if (!isTurkish()) this else ratingMap[this] ?: this

// ─── Karakter Rolü / Character Role ───────────────────────────────────────────

private val characterRoleMap = mapOf(
    // Jikan
    "Main" to "Ana Karakter",
    "Supporting" to "Yardımcı Karakter",
    "Background" to "Arka Plan",
    // AniList
    "MAIN" to "Ana Karakter",
    "SUPPORTING" to "Yardımcı Karakter",
    "BACKGROUND" to "Arka Plan",
    // Lowercase
    "main" to "Ana Karakter",
    "supporting" to "Yardımcı Karakter",
    "background" to "Arka Plan",
)

fun String.toTurkishCharacterRole(): String = if (!isTurkish()) this else characterRoleMap[this] ?: this

// ─── Dil Adı / Voice Actor Language ──────────────────────────────────────────

private val languageMap = mapOf(
    "Japanese" to "Japonca",
    "JAPANESE" to "Japonca",
    "English" to "İngilizce",
    "ENGLISH" to "İngilizce",
    "Korean" to "Korece",
    "KOREAN" to "Korece",
    "Chinese" to "Çince",
    "CHINESE" to "Çince",
    "Italian" to "İtalyanca",
    "ITALIAN" to "İtalyanca",
    "Spanish" to "İspanyolca",
    "SPANISH" to "İspanyolca",
    "French" to "Fransızca",
    "FRENCH" to "Fransızca",
    "German" to "Almanca",
    "GERMAN" to "Almanca",
    "Portuguese" to "Portekizce",
    "PORTUGUESE" to "Portekizce",
    "Hungarian" to "Macarca",
    "HUNGARIAN" to "Macarca",
    "Hebrew" to "İbranice",
    "HEBREW" to "İbranice",
    "Arabic" to "Arapça",
    "ARABIC" to "Arapça",
    "Turkish" to "Türkçe",
    "TURKISH" to "Türkçe",
    "Russian" to "Rusça",
    "RUSSIAN" to "Rusça",
    "Dutch" to "Hollandaca",
    "DUTCH" to "Hollandaca",
    "Swedish" to "İsveçce",
    "SWEDISH" to "İsveçce",
    "Polish" to "Lehçe",
    "POLISH" to "Lehçe",
    "Catalan" to "Katalanca",
    "CATALAN" to "Katalanca",
    "Finnish" to "Fince",
    "FINNISH" to "Fince",
    "Czech" to "Çekçe",
    "CZECH" to "Çekçe",
    "Thai" to "Tayca",
    "THAI" to "Tayca",
    "Romanian" to "Rumence",
    "ROMANIAN" to "Rumence",
    "Norwegian" to "Norveçce",
    "NORWEGIAN" to "Norveçce",
    "Danish" to "Danca",
    "DANISH" to "Danca",
    "Indonesian" to "Endonezyaca",
    "INDONESIAN" to "Endonezyaca",
    "Malay" to "Malayca",
    "MALAY" to "Malayca",
    "Vietnamese" to "Vietnamca",
    "VIETNAMESE" to "Vietnamca",
    "Filipino" to "Filipince",
    "FILIPINO" to "Filipince",
    "Greek" to "Yunanca",
    "GREEK" to "Yunanca",
    "Bulgarian" to "Bulgarca",
    "BULGARIAN" to "Bulgarca",
    "Ukrainian" to "Ukraynaca",
    "UKRAINIAN" to "Ukraynaca",
    "Croatian" to "Hırvatça",
    "CROATIAN" to "Hırvatça",
    "Spain" to "İspanya",
    "SPAIN" to "İspanya",
    "Brazil" to "Brezilya",
    "BRAZIL" to "Brezilya",
    "Latin America" to "Latin Amerika",
    "LATIN AMERICA" to "Latin Amerika",
)

fun String.toTurkishLanguage(): String {
    if (!isTurkish()) return this
    val mapped = languageMap[this]
    if (mapped != null) return mapped

    if (this.contains("(")) {
        val baseLang = this.substringBefore("(").trim()
        val suffix = this.substringAfter("(").substringBefore(")").trim()
        val baseMapped = languageMap[baseLang]
        if (baseMapped != null) {
            val suffixMapped = languageMap[suffix] ?: suffix
            return "$baseMapped ($suffixMapped)"
        }
    }
    return this
}

/**
 * Reorders names in "LastName, FirstName" format to "FirstName LastName".
 */
fun String.toFriendlyName(): String {
    if (this.contains(",")) {
        val parts = this.split(",").map { it.trim() }
        if (parts.size == 2) {
            return "${parts[1]} ${parts[0]}"
        }
    }
    return this
}

/**
 * Sorts voice actors: Japanese first, English second, Turkish third, then others alphabetically by language.
 */
fun List<KitsugiVoiceActor>.sortedByLanguagePreference(): List<KitsugiVoiceActor> {
    return this.sortedWith(
        compareByDescending<KitsugiVoiceActor> { it.language.equals("Japonca", ignoreCase = true) }
            .thenByDescending { it.language.equals("İngilizce", ignoreCase = true) }
            .thenByDescending { it.language.equals("Türkçe", ignoreCase = true) }
            .thenBy { it.language }
    )
}

// ─── İlişki Tipi / Relation Type ──────────────────────────────────────────────

private val relationTypeMap = mapOf(
    // Jikan / MAL
    "Sequel" to "Devam",
    "Prequel" to "Öncül",
    "Alternative setting" to "Alternatif Ortam",
    "Alternative version" to "Alternatif Versiyon",
    "Side story" to "Yan Hikaye",
    "Summary" to "Özet",
    "Full story" to "Tam Hikaye",
    "Spin-off" to "Spin-Off",
    "Adaptation" to "Uyarlama",
    "Character" to "Karakter",
    "Other" to "Diğer",
    "Relation" to "İlişkili",
    "Parent story" to "Ana Hikaye",
    // AniList
    "SEQUEL" to "Devam",
    "PREQUEL" to "Öncül",
    "ALTERNATIVE" to "Alternatif",
    "SIDE_STORY" to "Yan Hikaye",
    "CHARACTER" to "Karakter",
    "SUMMARY" to "Özet",
    "PARENT" to "Ana",
    "ADAPTATION" to "Uyarlama",
    "COMPILATION" to "Derleme",
    "CONTAINS" to "İçeriyor",
    "SPIN_OFF" to "Spin-Off",
    "SOURCE" to "Kaynak",
    "OTHER" to "Diğer",
)

fun String.toTurkishRelationType(): String = if (!isTurkish()) this else relationTypeMap[this] ?: this

// ─── Medya Tipi / Media Type (API string'den) ─────────────────────────────────

private val mediaTypeStringMap = mapOf(
    "TV" to "TV Dizisi",
    "Movie" to "Film",
    "MOVIE" to "Film",
    "OVA" to "OVA",
    "ONA" to "ONA",
    "Special" to "Özel",
    "SPECIAL" to "Özel",
    "Music" to "Müzik",
    "MUSIC" to "Müzik",
    "Manga" to "Manga",
    "MANGA" to "Manga",
    "One-shot" to "Tek Bölüm",
    "ONE_SHOT" to "Tek Bölüm",
    "Manhua" to "Manhua",
    "Manhwa" to "Manhwa",
    "Doujinshi" to "Doujinshi",
    "DOUJINSHI" to "Doujinshi",
    "Novel" to "Roman",
    "NOVEL" to "Roman",
    "Light Novel" to "Hafif Roman",
    "LIGHT_NOVEL" to "Hafif Roman",
    "Unknown" to "Bilinmiyor",
    "ANIME" to "Anime",
    "TV_SHORT" to "Kısa TV Dizisi",
    "GRAVEL" to "Gravel",
    // Lowercase & extra
    "movie" to "Film",
    "tv" to "Dizi",
    "anime" to "Anime",
    "manga" to "Manga",
)

fun String.toTurkishMediaTypeString(): String = if (!isTurkish()) this else mediaTypeStringMap[this] ?: this

// ─── Yayın Günü / Broadcast Day ───────────────────────────────────────────────

private val broadcastDayMap = mapOf(
    "Mondays" to "Pazartesi",
    "Tuesdays" to "Salı",
    "Wednesdays" to "Çarşamba",
    "Thursdays" to "Perşembe",
    "Fridays" to "Cuma",
    "Saturdays" to "Cumartesi",
    "Sundays" to "Pazar",
    "Monday" to "Pazartesi",
    "Tuesday" to "Salı",
    "Wednesday" to "Çarşamba",
    "Thursday" to "Perşembe",
    "Friday" to "Cuma",
    "Saturday" to "Cumartesi",
    "Sunday" to "Pazar",
)

/**
 * Yayın bilgisini Türkçeleştirir.
 * Örnek: "Saturdays at 17:00" → "Cumartesi 17:00"
 */
fun String.toTurkishBroadcast(): String {
    if (!isTurkish()) return this
    var result = this
    broadcastDayMap.forEach { (en, tr) ->
        result = result.replace(en, tr)
    }
    // "at" kelimesini kaldır
    result = result.replace(" at ", " ").trim()
    return result
}

// ─── Cinsiyet / Gender ────────────────────────────────────────────────────────

private val genderMap = mapOf(
    "Male" to "Erkek",
    "Female" to "Kadın",
    "Non-binary" to "Non-binary",
    "Unknown" to "Bilinmiyor",
)

fun String.toTurkishGender(): String = if (!isTurkish()) this else genderMap[this] ?: this

// ─── Kan Grubu / Blood Type ───────────────────────────────────────────────────
// (Zaten kısaltmalar: A, B, AB, O — Türkçe için değişmez)

// ─── Bölüm Süresi / Episode Duration ─────────────────────────────────────────

/**
 * "24 min" → "24 dk" gibi kısaltmaları Türkçeleştirir.
 */
fun String.toTurkishDuration(): String {
    if (!isTurkish()) return this
    return this.replace(" min per ep", " dk/bölüm")
        .replace(" min", " dk")
        .replace("hr", "sa")
        .replace("Unknown", "Bilinmiyor")
}

// ─── Staff Rolü / Staff Role (kısmi Türkçeleştirme) ─────────────────────────

private val staffRoleMap = mapOf(
    "Director" to "Yönetmen",
    "Script" to "Senarist",
    "Screenplay" to "Senarist",
    "Character Design" to "Karakter Tasarımı",
    "Animation Director" to "Animasyon Yönetmeni",
    "Chief Animation Director" to "Baş Animasyon Yönetmeni",
    "Art Director" to "Sanat Yönetmeni",
    "Sound Director" to "Ses Yönetmeni",
    "Music" to "Müzik",
    "Producer" to "Yapımcı",
    "Series Composition" to "Seri Düzenlemesi",
    "Color Design" to "Renk Tasarımı",
    "Photography Director" to "Görüntü Yönetmeni",
    "3D Director" to "3D Yönetmeni",
    "Prop Design" to "Sahne Eşyası Tasarımı",
    "Background Art" to "Arka Plan Sanatı",
    "Key Animation" to "Anahtar Animasyon",
    "In-Between Animation" to "Ara Animasyon",
    "Editing" to "Kurgu",
    "Opening Theme Song Arrangement" to "Açılış Teması Düzeni",
    "Ending Theme Song Arrangement" to "Kapanış Teması Düzeni",
    "Original Character Design" to "Orijinal Karakter Tasarımı",
    "Mechanical Design" to "Mekanik Tasarım",
    "Storyboard" to "Storyboard",
    "Episode Director" to "Bölüm Yönetmeni",
    "Unit Director" to "Birim Yönetmeni",
    "Special Effects" to "Özel Efektler",
    "Voice Actor" to "Seslendirme Sanatçısı",
    "Theme Song Performance" to "Tema Şarkısı Performansı",
    "Adaptation" to "Uyarlama",
    "Original Creator" to "Orijinal Yaratıcı",
    "Supervisor" to "Süpervizör",
    "Composition" to "Kompozisyon",
    "Art Design" to "Sanat Tasarımı",
    "CG Director" to "CG Yönetmeni",
    "Conceptual Design" to "Kavramsal Tasarım",
    "Planning" to "Planlama",
    "Action Animation Director" to "Aksiyon Animasyon Yönetmeni",
    "Original Music Composer" to "Orijinal Müzik Bestecisi",
    "Costume Design" to "Kostüm Tasarımı",
    "Director of Photography" to "Görüntü Yönetmeni",
    "Stunt Coordinator" to "Dublör Koordinatörü",
    "First Assistant Director" to "Birinci Yönetmen Yardımcısı",
    "Casting" to "Oyuncu Seçimi",
    "Conceptual Illustrator" to "Kavramsal İllüstratör",
    "Construction Coordinator" to "İnşaat Koordinatörü",
    "Transportation Coordinator" to "Ulaşım Koordinatörü",
    "Production Design" to "Yapım Tasarımı",
    "ADR Director" to "ADR Yönetmeni",
    "ADR Script" to "ADR Senaristi",
    "Executive Producer" to "Yönetici Yapımcı",
    "Associate Producer" to "Ortak Yapımcı",
    "Co-Producer" to "Eş Yapımcı",
    "Line Producer" to "Hat Yapımcısı",
    "Assistant Director" to "Yönetmen Yardımcısı",
    "Second Assistant Director" to "İkinci Yönetmen Yardımcısı",
    "Third Assistant Director" to "Üçüncü Yönetmen Yardımcısı",
    "Writer" to "Yazar",
    "Novel" to "Roman",
    "Original Story" to "Orijinal Hikaye",
    "Creator" to "Yaratıcı",
    "Set Decoration" to "Set Dekorasyonu",
    "Set Designer" to "Set Tasarımcısı",
    "Supervising Art Director" to "Baş Sanat Yönetmeni",
    "Production Director" to "Yapım Yönetmeni",
    "Sound Editor" to "Ses Editörü",
    "Sound Designer" to "Ses Tasarımcısı",
    "Sound Effects Editor" to "Ses Efektleri Editörü",
    "Supervising Sound Editor" to "Baş Ses Editörü",
    "Sound Re-Recording Mixer" to "Ses Miksajı",
    "Sound Mixer" to "Ses Mikseri",
    "Production Sound Mixer" to "Yapım Ses Mikseri",
    "Foley" to "Efekt Sanatçısı (Foley)",
    "Foley Artist" to "Foley Sanatçısı",
    "Foley Editor" to "Foley Editörü",
    "Foley Mixer" to "Foley Mikseri",
    "Dialogue Editor" to "Diyalog Editörü",
    "Visual Effects" to "Görsel Efektler",
    "Visual Effects Supervisor" to "Görsel Efektler Süpervizörü",
    "Visual Effects Producer" to "Görsel Efektler Yapımcısı",
    "Visual Effects Coordinator" to "Görsel Efektler Koordinatörü",
    "Special Effects Coordinator" to "Özel Efektler Koordinatörü",
    "Stunt Double" to "Dublör",
    "Camera Operator" to "Kamera Operatörü",
    "Steadicam Operator" to "Steadicam Operatörü",
    "Still Photographer" to "Set Fotoğrafçısı",
    "Gaffer" to "Işık Şefi",
    "Best Boy Grip" to "Set Amiri Yardımcısı",
    "Best Boy Electric" to "Işık Şefi Yardımcısı",
    "Key Grip" to "Set Amiri",
    "Grip" to "Set İşçisi",
    "Rigging Grip" to "Rigging Set İşçisi",
    "Rigging Gaffer" to "Rigging Işık Şefi",
    "Lighting Technician" to "Işık Teknisyeni",
    "Costume Supervisor" to "Kostüm Süpervizörü",
    "Key Costumer" to "Baş Kostümcü",
    "Costumer" to "Kostümcü",
    "Makeup Artist" to "Makyaj Sanatçısı",
    "Key Makeup Artist" to "Baş Makyaj Sanatçısı",
    "Hair Designer" to "Saç Tasarımcısı",
    "Key Hair Stylist" to "Baş Saç Stilisti",
    "Makeup Department Head" to "Makyaj Bölümü Başkanı",
    "Hair Department Head" to "Saç Bölümü Başkanı",
    "Makeup Designer" to "Makyaj Tasarımcısı",
    "Prosthetic Makeup Artist" to "Protez Makyaj Sanatçısı",
    "Script Supervisor" to "Senaryo Süpervizörü",
    "Publicist" to "Basın Danışmanı",
    "Location Manager" to "Mekan Sorumlusu",
    "Location Assistant" to "Mekan Sorumlusu Yardımcısı",
    "Production Coordinator" to "Yapım Koordinatörü",
    "Production Manager" to "Yapım Müdürü",
    "Production Supervisor" to "Yapım Süpervizörü",
    "Post Production Supervisor" to "Post-Prodüksiyon Süpervizörü",
    "Colorist" to "Renk Uzmanı (Colorist)",
    "Editorial Production Assistant" to "Kurgu Yapım Yardımcısı",
    "First Assistant Editor" to "Birinci Kurgu Yardımcısı",
    "Assistant Editor" to "Kurgu Yardımcısı",
    "Co-Editor" to "Kurgu Ortağı",
    "Music Editor" to "Müzik Editörü",
    "Music Supervisor" to "Müzik Süpervizörü",
    "Conductor" to "Orkestra Şefi",
    "Choreographer" to "Koreograf",
    "Boom Operator" to "Boom Operatörü",
    "Compositing Supervisor" to "Kompozisyon Süpervizörü",
    "Compositor" to "Kompozitör",
    "Lead Animator" to "Baş Animatör",
    "Animator" to "Animatör",
    "Sequence Lead" to "Sekans Şefi",
    "CG Artist" to "CG Sanatçısı",
    "3D Animator" to "3D Animatörü",
    "Character Modeling" to "Karakter Modelleme",
    "Modeler" to "Modelci",
    "Texture Artist" to "Doku Sanatçısı",
    "Concept Artist" to "Konsept Sanatçısı",
    "Storyboard Artist" to "Storyboard Sanatçısı",
    "Layout Artist" to "Layout Tasarımcısı",
    "Background Designer" to "Arka Plan Tasarımcısı",
    "Key Background Artist" to "Baş Arka Plan Sanatçısı",
    "Background Artist" to "Arka Plan Sanatçısı",
    "Inbetween Artist" to "Ara Çizim Sanatçısı",
    "Assistant Animation Director" to "Animasyon Yönetmeni Yardımcısı",
    "Co-Animation Director" to "Animasyon Yönetmeni Ortağı",
    "Co-Director" to "Yardımcı Yönetmen",
    "Theme Song Lyrics" to "Tema Şarkısı Sözleri",
    "Theme Song Arrangement" to "Tema Şarkısı Düzenlemesi",
    "Theme Song Composition" to "Tema Şarkısı Bestesi",
    "Theme Song Music" to "Tema Şarkısı Müziği",
)

private val commonPhrasesMap = mapOf(
    "Original Character Design" to "Orijinal Karakter Tasarımı",
    "Original Music Composer" to "Orijinal Müzik Bestecisi",
    "First Assistant Director" to "Birinci Yönetmen Yardımcısı",
    "Second Assistant Director" to "İkinci Yönetmen Yardımcısı",
    "Third Assistant Director" to "Üçüncü Yönetmen Yardımcısı",
    "Supervising Art Director" to "Baş Sanat Yönetmeni",
    "Assistant Animation Director" to "Animasyon Yönetmeni Yardımcısı",
    "Co-Animation Director" to "Animasyon Yönetmeni Ortağı",
    "Action Animation Director" to "Aksiyon Animasyon Yönetmeni",
    "Chief Animation Director" to "Baş Animasyon Yönetmeni",
    "Animation Director" to "Animasyon Yönetmeni",
    "Assistant Director" to "Yönetmen Yardımcısı",
    "Technical Director" to "Teknik Yönetmen",
    "Casting Director" to "Oyuncu Seçimi Yönetmeni",
    "Episode Director" to "Bölüm Yönetmeni",
    "Series Director" to "Seri Yönetmeni",
    "Action Director" to "Aksiyon Yönetmeni",
    "Chief Director" to "Baş Yönetmen",
    "Co-Director" to "Yardımcı Yönetmen",
    "Art Director" to "Sanat Yönetmeni",
    "Sound Director" to "Ses Yönetmeni",
    "Music Director" to "Müzik Yönetmeni",
    "Unit Director" to "Birim Yönetmeni",
    "Executive Producer" to "Yönetici Yapımcı",
    "Associate Producer" to "Ortak Yapımcı",
    "Line Producer" to "Hat Yapımcısı",
    "Co-Producer" to "Eş Yapımcı",
    "Music Producer" to "Müzik Yapımcısı",
    "Sound Producer" to "Ses Yapımcısı",
    "Animation Producer" to "Animasyon Yapımcısı",
    "Character Designer" to "Karakter Tasarımcısı",
    "Mechanical Designer" to "Mekanik Tasarımcı",
    "Prop Designer" to "Sahne Eşyası Tasarımcısı",
    "Set Designer" to "Set Tasarımcısı",
    "Costume Designer" to "Kostüm Tasarımcısı",
    "Makeup Designer" to "Makyaj Tasarımcısı",
    "Sound Designer" to "Ses Tasarımcısı",
    "Visual Designer" to "Görsel Tasarımcı",
    "Graphic Designer" to "Grafik Tasarımcı",
    "Background Designer" to "Arka Plan Tasarımcısı",
    "Original Creator" to "Orijinal Yaratıcı",
    "Original Story" to "Orijinal Hikaye",
    "Original Work" to "Orijinal Eser",
    "Theme Song Performance" to "Tema Şarkısı Performansı",
    "Theme Song Lyrics" to "Tema Şarkısı Sözleri",
    "Theme Song Arrangement" to "Tema Şarkısı Düzenlemesi",
    "Theme Song Composition" to "Tema Şarkısı Bestesi",
    "Theme Song Music" to "Tema Şarkısı Müziği",
    "Theme Song" to "Tema Şarkısı",
    "Opening Theme" to "Açılış Teması",
    "Ending Theme" to "Kapanış Teması",
    "Insert Song" to "Bölüm İçi Şarkı",
    "Song Lyrics" to "Şarkı Sözleri",
    "Song Performance" to "Şarkı Performansı",
    "Song Arrangement" to "Şarkı Düzenlemesi",
    "Song Composition" to "Şarkı Bestesi",
    "Sound Effects" to "Ses Efektleri",
    "Special Effects" to "Özel Efektler",
    "Visual Effects" to "Görsel Efektler",
    "VFX Supervisor" to "VFX Süpervizörü",
    "VFX Producer" to "VFX Yapımcısı",
    "VFX Coordinator" to "VFX Koordinatörü",
    "SFX Coordinator" to "SFX Koordinatörü",
    "Stunt Coordinator" to "Dublör Koordinatörü",
    "Stunt Double" to "Dublör",
    "Key Animator" to "Ana Animatör",
    "Lead Animator" to "Baş Animatör",
    "Assistant Animator" to "Yardımcı Animatör",
    "In-between Animator" to "Ara Çizim Animatörü",
    "Production Assistant" to "Yapım Asistanı",
    "Production Manager" to "Yapım Müdürü",
    "Production Supervisor" to "Yapım Süpervizörü",
    "Production Coordinator" to "Yapım Koordinatörü",
    "Location Manager" to "Mekan Sorumlusu",
    "Location Assistant" to "Mekan Sorumlusu Yardımcısı",
    "Public Relations" to "Halkla İlişkiler",
    "Script Supervisor" to "Senaryo Süpervizörü",
    "Dialogue Editor" to "Diyalog Editörü",
    "Sound Editor" to "Ses Editörü",
    "Foley Artist" to "Foley Sanatçısı",
    "Camera Operator" to "Kamera Operatörü",
    "Steadicam Operator" to "Steadicam Operatörü",
    "Still Photographer" to "Set Fotoğrafçısı",
    "Director of Photography" to "Görüntü Yönetmeni",
    "Photography Director" to "Görüntü Yönetmeni",
    "Costume Design" to "Kostüm Tasarımı",
    "Production Design" to "Yapım Tasarımı",
    "Character Design" to "Karakter Tasarımı",
    "Color Design" to "Renk Tasarımı",
    "Prop Design" to "Sahne Eşyası Tasarımı",
    "Mechanical Design" to "Mekanik Tasarım",
    "Conceptual Design" to "Kavramsal Tasarım",
    "Art Design" to "Sanat Tasarımı",
    "Background Art" to "Arka Plan Sanatı",
    "Series Composition" to "Seri Düzenlemesi",
    "Voice Actor" to "Seslendirme Sanatçısı",
)

private val commonWordsMap = mapOf(
    "Director" to "Yönetmen",
    "Producer" to "Yapımcı",
    "Writer" to "Yazar",
    "Creator" to "Yaratıcı",
    "Supervisor" to "Süpervizör",
    "Coordinator" to "Koordinatör",
    "Manager" to "Müdür",
    "Assistant" to "Yardımcı",
    "Associate" to "Ortak",
    "Executive" to "Yönetici",
    "Chief" to "Baş",
    "Editor" to "Editör",
    "Editing" to "Kurgu",
    "Artist" to "Sanatçı",
    "Animator" to "Animatör",
    "Designer" to "Tasarımcı",
    "Design" to "Tasarım",
    "Illustrator" to "İllüstratör",
    "Choreographer" to "Koreograf",
    "Conductor" to "Orkestra Şefi",
    "Casting" to "Oyuncu Seçimi",
    "Composer" to "Besteci",
    "Composition" to "Kompozisyon",
    "Singer" to "Şarkıcı",
    "Vocalist" to "Vokalist",
    "Actor" to "Oyuncu",
    "Actress" to "Aktris",
    "Host" to "Sunucu",
    "Staff" to "Ekip Üyesi",
    "Crew" to "Ekip",
    "Art" to "Sanat",
    "Music" to "Müzik",
    "Sound" to "Ses",
    "Audio" to "Ses",
    "Voice" to "Ses",
    "Camera" to "Kamera",
    "Photography" to "Fotoğraf",
    "Costume" to "Kostüm",
    "Makeup" to "Makyaj",
    "Hair" to "Saç",
    "Effects" to "Efektler",
    "Effect" to "Efekt",
    "Special" to "Özel",
    "Visual" to "Görsel",
    "Production" to "Yapım",
    "Development" to "Geliştirme",
    "Technical" to "Teknik",
    "Animation" to "Animasyon",
    "Original" to "Orijinal",
    "Character" to "Karakter",
    "Mechanical" to "Mekanik",
    "Concept" to "Konsept",
    "Planning" to "Planlama",
    "Theme" to "Tema",
    "Song" to "Şarkı",
    "Lyrics" to "Sözler",
    "Performance" to "Performans",
    "Arrangement" to "Düzenleme",
    "Background" to "Arka Plan",
    "Location" to "Mekan",
    "Set" to "Set",
    "Foley" to "Foley",
    "Stunt" to "Dublör",
    "Double" to "Dublör",
    "Modeler" to "Modelleyici",
    "Modeling" to "Modelleme",
    "Texture" to "Doku",
    "3D" to "3D",
    "2D" to "2D",
    "CG" to "CG",
    "VFX" to "VFX",
)

private fun translateSuffix(suffix: String): String {
    var result = suffix
    // Translate languages in the suffix
    languageMap.forEach { (en, tr) ->
        if (result.equals(en, ignoreCase = true)) {
            return tr
        }
        val pattern = "\\b${en}\\b".toRegex(RegexOption.IGNORE_CASE)
        result = result.replace(pattern, tr)
    }
    // Translate common abbreviation patterns
    result = result.replace("eps", "böl.", ignoreCase = true)
    result = result.replace("ep ", "böl. ", ignoreCase = true)
    result = result.replace("episode", "bölüm", ignoreCase = true)
    result = result.replace("episodes", "bölümler", ignoreCase = true)
    result = result.replace("Song Lyrics", "Şarkı Sözleri", ignoreCase = true)
    result = result.replace("lyrics", "şarkı sözleri", ignoreCase = true)
    return result
}

fun String.toTurkishStaffRole(): String {
    if (!isTurkish()) return this
    val trimmed = this.trim()
    if (trimmed.isEmpty()) return trimmed

    // Check for parenthesis details, e.g. "ADR Director (English)" or "Storyboard (OP, eps 1-3)"
    if (trimmed.contains("(")) {
        val base = trimmed.substringBefore("(").trim()
        val suffix = trimmed.substringAfter("(").substringBefore(")").trim()
        val baseTranslated = base.toTurkishStaffRole()
        val suffixTranslated = translateSuffix(suffix)
        return if (suffixTranslated.isNotEmpty()) {
            "$baseTranslated ($suffixTranslated)"
        } else {
            baseTranslated
        }
    }

    // Check for comma-separated roles, e.g. "Director, Writer"
    if (trimmed.contains(",")) {
        return trimmed.split(",")
            .map { it.trim().toTurkishStaffRole() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")
    }

    val exactMatch = staffRoleMap[trimmed]
        ?: staffRoleMap[trimmed.lowercase()]
        ?: staffRoleMap[trimmed.replaceFirstChar { it.uppercase() }]
    if (exactMatch != null) return exactMatch

    // Fallback: sub-phrase and word-by-word translation
    var fallbackTranslated = trimmed
    commonPhrasesMap.forEach { (en, tr) ->
        val pattern = "\\b${en}\\b".toRegex(RegexOption.IGNORE_CASE)
        fallbackTranslated = fallbackTranslated.replace(pattern, tr)
    }
    commonWordsMap.forEach { (en, tr) ->
        val pattern = "\\b${en}\\b".toRegex(RegexOption.IGNORE_CASE)
        fallbackTranslated = fallbackTranslated.replace(pattern, tr)
    }
    return fallbackTranslated
}

fun String.parseToMediaType(): MediaType {
    val lower = this.trim().lowercase()
    return when {
        // Manga / Novel / Webtoon
        lower.contains("manga") ||
        lower.contains("novel") ||
        lower.contains("roman") ||
        lower.contains("one-shot") ||
        lower.contains("one shot") ||
        lower.contains("tek bölüm") ||
        lower.contains("tek bolum") ||
        lower.contains("doujinshi") ||
        lower.contains("manhua") ||
        lower.contains("manhwa") -> MediaType.Manga

        // Movie / Film
        lower.contains("movie") ||
        lower.contains("film") -> MediaType.Movie

        // TV / TV Show / Dizi
        lower.contains("tvshow") ||
        lower.contains("tv show") ||
        lower.contains("tv dizisi") ||
        lower.contains("dizi") ||
        lower == "tv" -> MediaType.TvShow

        // Anime / Default (special, ova, ona, etc.)
        else -> MediaType.Anime
    }
}

fun String.toFriendlySourceLabel(): String {
    return when (this.lowercase().trim()) {
        "jikan", "mal" -> "MyAnimeList"
        "anilist" -> "AniList"
        "tmdb" -> "TMDB"
        "shikimori" -> "Shikimori"
        "simkl" -> "Simkl"
        "manual" -> "Manual"
        else -> this
    }
}

