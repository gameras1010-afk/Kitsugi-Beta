package com.kitsugi.animelist.ui.screens.search.composables

import androidx.compose.runtime.Composable

// ─── MediaFormat ─────────────────────────────────────────────────────────────

enum class KitsugiMediaFormat(val apiValue: String) : KitsugiLocalizable {
    TV("TV"),
    TV_SHORT("TV_SHORT"),
    MOVIE("MOVIE"),
    SPECIAL("SPECIAL"),
    OVA("OVA"),
    ONA("ONA"),
    MUSIC("MUSIC"),
    MANGA("MANGA"),
    NOVEL("NOVEL"),
    ONE_SHOT("ONE_SHOT");

    @Composable
    override fun localized(): String = when (this) {
        TV -> "TV"
        TV_SHORT -> "TV Kısa"
        MOVIE -> "Film"
        SPECIAL -> "Özel"
        OVA -> "OVA"
        ONA -> "ONA"
        MUSIC -> "Müzik"
        MANGA -> "Manga"
        NOVEL -> "Romanı"
        ONE_SHOT -> "Tek Bölüm"
    }

    companion object {
        val animeEntries = listOf(TV, TV_SHORT, MOVIE, SPECIAL, OVA, ONA, MUSIC)
        val mangaEntries = listOf(MANGA, NOVEL, ONE_SHOT)
    }
}

// ─── MediaStatus ─────────────────────────────────────────────────────────────

enum class KitsugiMediaStatus(val apiValue: String) : KitsugiLocalizable {
    FINISHED("FINISHED"),
    RELEASING("RELEASING"),
    NOT_YET_RELEASED("NOT_YET_RELEASED"),
    CANCELLED("CANCELLED"),
    HIATUS("HIATUS");

    @Composable
    override fun localized(): String = when (this) {
        FINISHED -> "Tamamlandı"
        RELEASING -> "Yayınlanıyor"
        NOT_YET_RELEASED -> "Henüz Yayınlanmadı"
        CANCELLED -> "İptal Edildi"
        HIATUS -> "Ara Verildi"
    }
}

// ─── CountryOfOrigin ─────────────────────────────────────────────────────────

enum class KitsugiCountryOfOrigin(val code: String) : KitsugiLocalizable {
    JAPAN("JP"),
    SOUTH_KOREA("KR"),
    CHINA("CN"),
    TAIWAN("TW");

    @Composable
    override fun localized(): String = when (this) {
        JAPAN -> "Japonya"
        SOUTH_KOREA -> "Güney Kore"
        CHINA -> "Çin"
        TAIWAN -> "Tayvan"
    }
}

// ─── MediaSource ─────────────────────────────────────────────────────────────

enum class KitsugiMediaSource(val apiValue: String) : KitsugiLocalizable {
    ORIGINAL("ORIGINAL"),
    MANGA("MANGA"),
    LIGHT_NOVEL("LIGHT_NOVEL"),
    VISUAL_NOVEL("VISUAL_NOVEL"),
    VIDEO_GAME("VIDEO_GAME"),
    OTHER("OTHER"),
    NOVEL("NOVEL"),
    DOUJINSHI("DOUJINSHI"),
    ANIME("ANIME"),
    WEB_NOVEL("WEB_NOVEL"),
    LIVE_ACTION("LIVE_ACTION"),
    GAME("GAME"),
    COMIC("COMIC"),
    MULTIMEDIA_PROJECT("MULTIMEDIA_PROJECT"),
    PICTURE_BOOK("PICTURE_BOOK");

    @Composable
    override fun localized(): String = when (this) {
        ORIGINAL -> "Orijinal"
        MANGA -> "Manga"
        LIGHT_NOVEL -> "Light Novel"
        VISUAL_NOVEL -> "Görsel Roman"
        VIDEO_GAME -> "Video Oyunu"
        OTHER -> "Diğer"
        NOVEL -> "Roman"
        DOUJINSHI -> "Doujinshi"
        ANIME -> "Anime"
        WEB_NOVEL -> "Web Roman"
        LIVE_ACTION -> "Canlı Aksiyon"
        GAME -> "Oyun"
        COMIC -> "Çizgi Roman"
        MULTIMEDIA_PROJECT -> "Multimedya Projesi"
        PICTURE_BOOK -> "Resimli Kitap"
    }
}

// ─── MediaSort ───────────────────────────────────────────────────────────────

enum class KitsugiMediaSortSearch(val ascApiValue: String, val descApiValue: String) : KitsugiLocalizable {
    SEARCH_MATCH("SEARCH_MATCH", "SEARCH_MATCH"),
    POPULARITY("POPULARITY", "POPULARITY_DESC"),
    SCORE("SCORE", "SCORE_DESC"),
    TRENDING("TRENDING", "TRENDING_DESC"),
    FAVOURITES("FAVOURITES", "FAVOURITES_DESC"),
    START_DATE("START_DATE", "START_DATE_DESC"),
    END_DATE("END_DATE", "END_DATE_DESC");

    @Composable
    override fun localized(): String = when (this) {
        SEARCH_MATCH -> "Varsayılan"
        POPULARITY -> "Popülerlik"
        SCORE -> "Puan"
        TRENDING -> "Trend"
        FAVOURITES -> "Favoriler"
        START_DATE -> "Başlangıç Tarihi"
        END_DATE -> "Bitiş Tarihi"
    }

    companion object {
        fun fromApiValue(apiValue: String): KitsugiMediaSortSearch? =
            entries.find { it.ascApiValue == apiValue || it.descApiValue == apiValue }
    }
}

// ─── MediaSeason ─────────────────────────────────────────────────────────────

enum class KitsugiMediaSeason(val apiValue: String) : KitsugiLocalizable {
    WINTER("WINTER"),
    SPRING("SPRING"),
    SUMMER("SUMMER"),
    FALL("FALL");

    @Composable
    override fun localized(): String = when (this) {
        WINTER -> "Kış"
        SPRING -> "İlkbahar"
        SUMMER -> "Yaz"
        FALL -> "Sonbahar"
    }
}
