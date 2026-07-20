package com.kitsugi.animelist.ui.utils

import com.kitsugi.animelist.model.MediaType

/**
 * V2-B02: GenreLabelFormatter
 *
 * Anime/manga/film türlerini Türkçe gösterimle formatlar.
 * NuvioTV GenreLabelFormatter.kt referans alındı.
 */
object GenreLabelFormatter {

    /** Türleri Türkçeye çevirir ve ikon eşler. */
    fun format(genre: String): String = genreMap[genre.lowercase().trim()] ?: genre

    /** Birden fazla türü virgülle birleştirerek döner. Max N adet. */
    fun formatList(genres: List<String>, maxCount: Int = 3): String =
        genres.take(maxCount).joinToString(" · ") { format(it) }

    /** MediaType için varsayılan tür etiketlerini döner. */
    fun defaultGenresFor(type: MediaType): List<String> = when (type) {
        MediaType.Anime  -> listOf("Anime", "Animasyon")
        MediaType.Movie  -> listOf("Film")
        MediaType.TvShow -> listOf("Dizi")
        else             -> emptyList()
    }

    private val genreMap: Map<String, String> = mapOf(
        // Anime/general
        "action"          to "Aksiyon",
        "adventure"       to "Macera",
        "comedy"          to "Komedi",
        "drama"           to "Dram",
        "fantasy"         to "Fantezi",
        "horror"          to "Korku",
        "mystery"         to "Gizem",
        "romance"         to "Romantik",
        "sci-fi"          to "Bilim Kurgu",
        "science fiction" to "Bilim Kurgu",
        "thriller"        to "Gerilim",
        "slice of life"   to "Günlük Yaşam",
        "supernatural"    to "Doğaüstü",
        "psychological"   to "Psikolojik",
        "sports"          to "Spor",
        "school"          to "Okul",
        "mecha"           to "Mecha",
        "military"        to "Askeri",
        "music"           to "Müzik",
        "historical"      to "Tarihi",
        "history"         to "Tarih",
        "shounen"         to "Shōnen",
        "seinen"          to "Seinen",
        "josei"           to "Josei",
        "shoujo"          to "Shōjo",
        "ecchi"           to "Ecchi",
        "harem"           to "Harem",
        "isekai"          to "İsekai",
        "space"           to "Uzay",
        "vampire"         to "Vampir",
        "magic"           to "Büyü",
        "demons"          to "İblis",
        "martial arts"    to "Dövüş Sanatları",
        "super power"     to "Süper Güç",
        "game"            to "Oyun",
        "parody"          to "Parodi",
        "kids"            to "Çocuk",
        "animation"       to "Animasyon",
        "anime"           to "Anime",
        // TV/Film
        "western"         to "Western",
        "crime"           to "Suç",
        "family"          to "Aile",
        "documentary"     to "Belgesel",
        "biography"       to "Biyografi",
        "war"             to "Savaş",
        "news"            to "Haber",
        "reality"         to "Reality",
        "talk"            to "Talk Show",
        "soap"            to "Pembe Dizi",
        "tv movie"        to "TV Filmi"
    )
}
