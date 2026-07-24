package com.kitsugi.animelist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "explore_cache")
data class ExploreCacheEntity(
    @PrimaryKey
    val categoryKey: String, // e.g. "tmdb_trending_movies"
    val payloadJson: String,  // List<JikanSearchResult> serialized
    val cachedAtMs: Long
)
