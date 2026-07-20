package com.kitsugi.animelist.core.recommendations

/**
 * B1.3 - WatchProgress: Android TV Launcher channel ve WatchNext provider icin
 * kullanilan hafif veri modeli.
 *
 * MediaEntryEntity'den (episode sayisi) veya KitsugiPlayerViewModel'den (ms pozisyon)
 * uretilir. Launcher channel reconcile ve WatchNextProgram upsert islemlerinde kullanilir.
 *
 * contentId: Unique kimlik - "mal_<malId>_s<season>e<episode>" veya "mal_<malId>"
 * contentType: "anime" | "movie" | "manga"
 * name: Gosterilecek baslik
 * poster: 2:3 poster URL (nullable)
 * backdrop: 16:9 backdrop URL (nullable)
 * logo: Kucuk logo/badge URL (nullable)
 * season: Bolum sezonu (anime icin null olabilir)
 * episode: Bolum numarasi (anime icin null olabilir)
 * episodeTitle: Bolum basligi (nullable)
 * position: Oynatma pozisyonu (ms) - 0 ise ilerlemesi bilinmiyor
 * duration: Toplam sure (ms) - 0 ise bilinmiyor
 * lastWatched: Son izleme zamani (epoch ms)
 * progressPercent: Yuzdeli ilerleme 0-100 (nullable - pozisyon/duration'dan hesaplanabilir)
 */
data class WatchProgress(
    val contentId: String,
    val contentType: String,
    val name: String,
    val poster: String? = null,
    val backdrop: String? = null,
    val logo: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
    val position: Long = 0L,
    val duration: Long = 0L,
    val lastWatched: Long = System.currentTimeMillis(),
    val progressPercent: Int? = null
) {
    // Helper: Launcher icin guvenli progress yuzdesi (0-100)
    val safeProgressPercent: Int
        get() = when {
            progressPercent != null -> progressPercent.coerceIn(0, 100)
            duration > 0 -> ((position.toFloat() / duration) * 100).toInt().coerceIn(0, 100)
            else -> 0
        }

    companion object {
        // contentId olusturma yardimcisi
        fun buildContentId(malId: Int, season: Int? = null, episode: Int? = null): String =
            if (season != null && episode != null) "mal_${malId}_s${season}e${episode}"
            else "mal_$malId"

        fun buildContentId(sourceKey: String, mediaId: String): String =
            "${sourceKey}_$mediaId"
    }
}
