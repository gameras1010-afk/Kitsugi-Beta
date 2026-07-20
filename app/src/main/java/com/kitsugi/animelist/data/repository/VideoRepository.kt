package com.kitsugi.animelist.data.repository

import com.kitsugi.animelist.data.local.VideoEntity

interface VideoRepository {
    suspend fun cacheVideo(
        mediaId: Int,
        episode: Int,
        url: String,
        quality: String,
        headers: Map<String, String>? = null
    )

    suspend fun getCachedVideos(mediaId: Int, episode: Int): List<VideoEntity>

    suspend fun clearCachedVideos(mediaId: Int, episode: Int)

    suspend fun clearExpiredVideos(expireAgeMs: Long)

    suspend fun clearAll()
}
