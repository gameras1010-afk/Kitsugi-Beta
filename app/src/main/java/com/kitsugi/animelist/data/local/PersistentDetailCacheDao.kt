package com.kitsugi.animelist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PersistentDetailCacheDao {
    @Query("SELECT * FROM persistent_details_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getDetail(key: String): PersistentDetailCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetail(cache: PersistentDetailCacheEntity)

    @Query("DELETE FROM persistent_details_cache WHERE cacheKey = :key")
    suspend fun deleteDetail(key: String)

    @Query("DELETE FROM persistent_details_cache")
    suspend fun clearAll()

    @Query("DELETE FROM persistent_details_cache WHERE cachedAtMs < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}
