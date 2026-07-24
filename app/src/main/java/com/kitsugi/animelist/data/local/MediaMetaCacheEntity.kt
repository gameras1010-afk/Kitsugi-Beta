package com.kitsugi.animelist.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Anime metadata önbelleği — uygulama kapansa bile korunur.
 *
 * Saklananlar:
 *  - malId / aniListId → tmdbId dönüşüm sonuçları
 *  - tmdbId → logo URL
 *
 * Strateji:
 *  - Her satır tek bir anime'yi temsil eder (tmdbId PRIMARY KEY)
 *  - malId ve aniListId index'lenir (hızlı arama)
 *  - logoUrl: null ise "bulunamadı" anlamına gelir (tekrar deneme yapılmaz)
 *  - logoNotFound: true ise API'ye tekrar gitme
 *  - cachedAtMs: ne zaman kaydedildi
 */
@Entity(
    tableName = "media_meta_cache",
    indices = [
        Index(value = ["malId"]),
        Index(value = ["aniListId"]),
        Index(value = ["kitsuId"])
    ]
)
data class MediaMetaCacheEntity(
    @PrimaryKey
    val tmdbId: Int,
    val malId: Int?,         // Bu tmdbId'ye karşılık gelen MAL ID
    val aniListId: Int?,     // Bu tmdbId'ye karşılık gelen AniList ID
    val logoUrl: String?,    // null = logo yok veya henüz bilinmiyor
    val logoNotFound: Boolean = false, // true = API sorgulandı, logo yok
    val cachedAtMs: Long = System.currentTimeMillis(),
    val kitsuId: String? = null // Bu tmdbId'ye karşılık gelen Kitsu ID veya Slug
)
