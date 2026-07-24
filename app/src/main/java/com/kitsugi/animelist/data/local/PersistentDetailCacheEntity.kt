package com.kitsugi.animelist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persistent_details_cache")
data class PersistentDetailCacheEntity(
    @PrimaryKey
    val cacheKey: String, // e.g. "anilist_123" or "tmdb_456"
    val detailJson: String, // KitsugiMediaDetail serialized
    val cachedAtMs: Long
)
