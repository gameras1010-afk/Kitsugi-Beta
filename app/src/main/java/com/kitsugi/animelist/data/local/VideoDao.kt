package com.kitsugi.animelist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VideoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Query("SELECT * FROM video_cache WHERE mediaId = :mediaId AND episode = :episode")
    suspend fun getVideosForEpisode(mediaId: Int, episode: Int): List<VideoEntity>

    @Query("DELETE FROM video_cache WHERE mediaId = :mediaId AND episode = :episode")
    suspend fun deleteVideosForEpisode(mediaId: Int, episode: Int)

    @Query("DELETE FROM video_cache WHERE resolvedAt < :expireTime")
    suspend fun clearExpiredVideos(expireTime: Long)

    @Query("DELETE FROM video_cache")
    suspend fun clearAll()
}
