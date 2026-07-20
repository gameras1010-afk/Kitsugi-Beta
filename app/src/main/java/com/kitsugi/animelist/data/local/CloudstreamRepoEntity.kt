package com.kitsugi.animelist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloudstream_repos")
data class CloudstreamRepoEntity(
    @PrimaryKey val repoUrl: String,
    val name: String,
    val description: String?,
    val addedAt: Long = System.currentTimeMillis()
)
