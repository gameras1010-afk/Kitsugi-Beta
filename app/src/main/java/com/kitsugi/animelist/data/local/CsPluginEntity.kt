package com.kitsugi.animelist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cs_plugins")
data class CsPluginEntity(
    @PrimaryKey val id: String, // internalName (örn. "AnimeciX")
    val name: String,
    val downloadUrl: String,
    val tvTypes: String, // JSON array: ["Anime", "Movie"]
    val iconUrl: String?,
    val version: Int,
    val enabled: Boolean = true,
    val installedAt: Long = System.currentTimeMillis()
)
