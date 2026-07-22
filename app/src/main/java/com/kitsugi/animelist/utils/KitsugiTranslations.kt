package com.kitsugi.animelist.utils

import com.kitsugi.animelist.model.MediaType

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
)

fun String.toTurkishLanguage(): String = if (!isTurkish()) this else languageMap[this] ?: this

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
)

fun String.toTurkishStaffRole(): String = if (!isTurkish()) this else staffRoleMap[this] ?: this

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
