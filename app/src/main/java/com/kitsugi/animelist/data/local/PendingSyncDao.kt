package com.kitsugi.animelist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingSyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingSyncEntity)

    @Query("SELECT * FROM pending_syncs ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingSyncEntity>

    @Query("DELETE FROM pending_syncs WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM pending_syncs WHERE retryCount >= :maxRetries")
    suspend fun deleteStale(maxRetries: Int = 5)

    @Query("UPDATE pending_syncs SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Int)

    @Query("SELECT COUNT(*) FROM pending_syncs")
    suspend fun count(): Int
}
