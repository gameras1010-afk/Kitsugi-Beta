package com.kitsugi.animelist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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
    suspend fun insert(entry: MediaEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MediaEntryEntity>): List<Long>

    @Update
    suspend fun update(entry: MediaEntryEntity)

    @Update
    suspend fun updateAll(entities: List<MediaEntryEntity>)

    @Query("DELETE FROM media_entries WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM media_entries WHERE source = :source")
    suspend fun deleteBySource(source: String)

    @Query("DELETE FROM media_entries WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MediaEntryEntity>): List<Long>

    @Query("DELETE FROM media_entries")
    suspend fun deleteAll()

    /**
     * Tüm import işlemini tek atomik transaction olarak gerçekleştirir.
     * Bu sayede Room Flow yalnızca BİR KEZ tetiklenir ve tüm liste
     * yeniden render edilmez — sadece değişen kayıtlar güncellenir.
     */
    @Transaction
    suspend fun smartImportTransaction(
        toInsert: List<MediaEntryEntity>,
        toUpdate: List<MediaEntryEntity>,
        toDeleteIds: List<Int>
    ) {
        if (toInsert.isNotEmpty()) upsertAll(toInsert)
        if (toUpdate.isNotEmpty()) updateAll(toUpdate)
        if (toDeleteIds.isNotEmpty()) deleteByIds(toDeleteIds)
    }
}