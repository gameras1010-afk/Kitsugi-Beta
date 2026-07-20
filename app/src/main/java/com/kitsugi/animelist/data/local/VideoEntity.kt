package com.kitsugi.animelist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_cache")
data class VideoEntity(
    @PrimaryKey val url: String,
    val mediaId: Int,
    val episode: Int,
    val quality: String,
    val headersJson: String? = null, // JSON string representing request headers
    val resolvedAt: Long = System.currentTimeMillis()
)
