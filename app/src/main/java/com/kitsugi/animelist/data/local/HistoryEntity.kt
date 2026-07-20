package com.kitsugi.animelist.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_history",
    indices = [Index(value = ["mediaId", "episode"], unique = true)]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val mediaId: Int,
    val episode: Int,
    val lastPositionMs: Long,
    val durationMs: Long,
    val lastWatchedAt: Long,
    val addonName: String? = null
)
