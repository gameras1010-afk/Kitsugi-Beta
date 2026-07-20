package com.kitsugi.animelist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaEntryDao {
    @Query("SELECT * FROM media_entries ORDER BY id DESC")
    fun observeAll(): Flow<List<MediaEntryEntity>>

    @Query("SELECT * FROM media_entries ORDER BY id DESC")
    suspend fun getAll(): List<MediaEntryEntity>

    @Query("SELECT * FROM media_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): MediaEntryEntity?

    @Query("SELECT * FROM media_entries WHERE malId = :malId LIMIT 1")
    suspend fun getByMalId(malId: Int): MediaEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MediaEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MediaEntryEntity>)

    @Update
    suspend fun update(entry: MediaEntryEntity)

    @Update
    suspend fun updateAll(entities: List<MediaEntryEntity>)

    @Query("DELETE FROM media_entries WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM media_entries WHERE source = :source")
    suspend fun deleteBySource(source: String)

    @Query("DELETE FROM media_entries")
    suspend fun deleteAll()
}