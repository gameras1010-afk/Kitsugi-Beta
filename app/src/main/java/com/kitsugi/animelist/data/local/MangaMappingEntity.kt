package com.kitsugi.animelist.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "manga_mappings")
data class MangaMappingEntity(
    @PrimaryKey
    val mediaId: Int, // Maps to MediaEntry.id
    val mangaSource: String, // e.g. "Manga-TR"
    val mangaUrl: String, // e.g. "/manga/one-piece"
    val mangaTitle: String, // e.g. "One Piece"
    val mangaThumbnail: String? = null
)

@Dao
interface MangaMappingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: MangaMappingEntity)

    @Query("SELECT * FROM manga_mappings WHERE mediaId = :mediaId")
    suspend fun getMapping(mediaId: Int): MangaMappingEntity?

    @Query("SELECT * FROM manga_mappings WHERE mediaId = :mediaId")
    fun observeMapping(mediaId: Int): Flow<MangaMappingEntity?>

    @Query("DELETE FROM manga_mappings WHERE mediaId = :mediaId")
    suspend fun deleteMapping(mediaId: Int)
}
