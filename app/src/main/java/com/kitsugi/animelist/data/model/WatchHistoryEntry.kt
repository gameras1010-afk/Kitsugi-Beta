package com.kitsugi.animelist.data.model

data class WatchHistoryEntry(
    val animeId: String,
    val animeTitle: String,
    val posterUrl: String?,
    val episode: Int,
    val season: Int,
    val isMovie: Boolean = false,
    val quality: String? = null,
    val source: String? = null,          // Eklenti adı (örn. "Torrentio")
    val watchedAtMs: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,           // Toplam süre (ms), bilinmiyorsa 0
    val positionMs: Long = 0L,           // Bırakılan konum (ms)
    val malId: Int? = null,
    val aniListId: Int? = null,
    val tmdbId: Int? = null,
    // Direkt oynatma için saklanan son stream URL'si ve başlıkları.
    // Boş değilse geçmişten devam ederken KitsugiStreamActivity'yi atlar.
    val streamUrl: String? = null,
    val streamHeaders: Map<String, String>? = null,
    val streamTitle: String? = null,
    val streamName: String? = null,
    val cs3Url: String? = null,
    val cs3ApiName: String? = null
)
