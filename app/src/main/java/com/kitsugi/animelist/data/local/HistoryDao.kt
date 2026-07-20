package com.kitsugi.animelist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun getAllHistoryFlow(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE mediaId = :mediaId AND episode = :episode LIMIT 1")
    suspend fun getProgress(mediaId: Int, episode: Int): HistoryEntity?

    @Query("SELECT * FROM watch_history WHERE mediaId = :mediaId ORDER BY episode DESC")
    suspend fun getHistoryForMedia(mediaId: Int): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: HistoryEntity)

    @Query("DELETE FROM watch_history WHERE mediaId = :mediaId AND episode = :episode")
    suspend fun deleteProgress(mediaId: Int, episode: Int)

    @Query("DELETE FROM watch_history")
    suspend fun clearAllHistory()
}
