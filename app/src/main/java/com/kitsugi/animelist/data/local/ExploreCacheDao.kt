package com.kitsugi.animelist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExploreCacheDao {
    @Query("SELECT * FROM explore_cache WHERE categoryKey = :key LIMIT 1")
    suspend fun getCategory(key: String): ExploreCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(cache: ExploreCacheEntity)

    @Query("DELETE FROM explore_cache WHERE categoryKey = :key")
    suspend fun deleteCategory(key: String)

    @Query("DELETE FROM explore_cache")
    suspend fun clearAll()
}
