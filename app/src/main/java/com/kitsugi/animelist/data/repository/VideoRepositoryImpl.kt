package com.kitsugi.animelist.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kitsugi.animelist.data.local.VideoDao
import com.kitsugi.animelist.data.local.VideoEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val videoDao: VideoDao,
    private val gson: Gson
) : VideoRepository {

    override suspend fun cacheVideo(
        mediaId: Int,
        episode: Int,
        url: String,
        quality: String,
        headers: Map<String, String>?
    ) {
        val headersJson = headers?.let { gson.toJson(it) }
        val video = VideoEntity(
            url = url,
            mediaId = mediaId,
            episode = episode,
            quality = quality,
            headersJson = headersJson
        )
        videoDao.insertVideo(video)
    }

    override suspend fun getCachedVideos(mediaId: Int, episode: Int): List<VideoEntity> {
        return videoDao.getVideosForEpisode(mediaId, episode)
    }

    override suspend fun clearCachedVideos(mediaId: Int, episode: Int) {
        videoDao.deleteVideosForEpisode(mediaId, episode)
    }

    override suspend fun clearExpiredVideos(expireAgeMs: Long) {
        val cutoffTime = System.currentTimeMillis() - expireAgeMs
        videoDao.clearExpiredVideos(cutoffTime)
    }

    override suspend fun clearAll() {
        videoDao.clearAll()
    }

    // Utility function to parse headers back
    fun parseHeaders(headersJson: String?): Map<String, String>? {
        if (headersJson.isNullOrEmpty()) return null
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(headersJson, type)
        } catch (e: Exception) {
            null
        }
    }
}
