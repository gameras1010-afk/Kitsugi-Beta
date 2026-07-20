package com.kitsugi.animelist.ui.utils

import com.kitsugi.animelist.model.MediaType

/**
 * V2-B02: TypeLabelFormatter
 *
 * MediaType ve alt tür string'lerini kullanıcı dostu Türkçe etiketlere dönüştürür.
 * NuvioTV TypeLabelFormatter.kt referans alındı.
 */
object TypeLabelFormatter {

    /**
     * MediaType'ı kısa Türkçe label'a çevirir.
     * Movie → "Film", Anime → "Anime", TvShow → "Dizi"
     */
    fun format(type: MediaType?): String = when (type) {
        MediaType.Movie  -> "Film"
        MediaType.Anime  -> "Anime"
        MediaType.TvShow -> "Dizi"
        MediaType.Manga  -> "Manga"
        else             -> "Medya"
    }

    /**
     * String tabanlı tür (AniList/MAL formatından) Türkçeye çevirir.
     * "TV" → "Dizi", "MOVIE" → "Film", "ONA" → "ONA" vs.
     */
    fun formatRaw(typeStr: String?): String = when (typeStr?.uppercase()?.trim()) {
        "TV", "TV_SHORT"        -> "Dizi"
        "MOVIE"                 -> "Film"
        "ANIME"                 -> "Anime"
        "MANGA"                 -> "Manga"
        "ONA"                   -> "ONA"
        "OVA"                   -> "OVA"
        "SPECIAL"               -> "Özel"
        "MUSIC"                 -> "Müzik"
        "ONE_SHOT"              -> "Tek Bölüm"
        "NOVEL", "LIGHT_NOVEL"  -> "Roman"
        "ONE_SHOT"              -> "Tek Bölüm"
        "MANHWA"                -> "Manhwa"
        "MANHUA"                -> "Manhua"
        "DOUJINSHI"             -> "Doujinshi"
        else                    -> typeStr?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Bilinmiyor"
    }

    /**
     * Bölüm/sezon formatı.
     * episodes = 12, isAnime = true  → "12 Bölüm"
     * episodes = null, isAnime = false → "?"
     */
    fun formatEpisodeCount(episodes: Int?, isAnime: Boolean = true): String {
        if (episodes == null || episodes <= 0) return "?"
        return "$episodes ${if (isAnime) "Bölüm" else "Ep."}"
    }

    /**
     * Süreyi dakikadan okunabilir formata çevirir.
     * 145 → "2s 25dk"
     * 45  → "45 dk"
     */
    fun formatDuration(minutes: Int?): String {
        if (minutes == null || minutes <= 0) return "?"
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours}s ${mins}dk" else "$mins dk"
    }

    /**
     * MediaType'ı API tipi string'ine çevirir (SIMKL endpoint için).
     * Movie → "movies", Anime → "anime", TvShow → "shows"
     */
    fun toApiType(type: MediaType?): String = when (type) {
        MediaType.Movie  -> "movies"
        MediaType.Anime  -> "anime"
        else             -> "shows"
    }

    /**
     * MediaType'ı Simkl payload key'ine çevirir.
     * Movie → "movie", Anime → "anime", TvShow → "show"
     */
    fun toSimklKey(type: MediaType?): String = when (type) {
        MediaType.Movie  -> "movie"
        MediaType.Anime  -> "anime"
        else             -> "show"
    }
}
