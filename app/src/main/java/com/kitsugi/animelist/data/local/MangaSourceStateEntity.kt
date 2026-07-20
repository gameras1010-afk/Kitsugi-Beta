package com.kitsugi.animelist.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "manga_source_state",
    indices = [
        Index(value = ["healthStatus"], name = "idx_manga_source_state_healthStatus"),
        Index(value = ["updatedAt"], name = "idx_manga_source_state_updatedAt")
    ]
)
data class MangaSourceStateEntity(
    @PrimaryKey val sourceKey: String,
    val sourceName: String,
    val pkgName: String,
    val lang: String,
    val baseUrl: String,
    val activeDomain: String? = null,
    val healthStatus: String,
    val lastReason: String? = null,
    val lastErrorType: String? = null,
    @ColumnInfo(defaultValue = "0") val lastCheckedAt: Long = 0L,
    @ColumnInfo(defaultValue = "0") val lastSuccessAt: Long = 0L,
    @ColumnInfo(defaultValue = "0") val lastFailureAt: Long = 0L,
    @ColumnInfo(defaultValue = "0") val successCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val failureCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val avgSearchMs: Long = 0L,
    @ColumnInfo(defaultValue = "0") val avgPopularMs: Long = 0L,
    @ColumnInfo(defaultValue = "0") val avgDetailsMs: Long = 0L,
    @ColumnInfo(defaultValue = "0") val avgChapterMs: Long = 0L,
    @ColumnInfo(defaultValue = "0") val avgPageMs: Long = 0L,
    @ColumnInfo(defaultValue = "0") val avgImageMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface MangaSourceStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MangaSourceStateEntity)

    @Query("SELECT * FROM manga_source_state WHERE sourceKey = :sourceKey LIMIT 1")
    suspend fun getByKey(sourceKey: String): MangaSourceStateEntity?

    @Query("SELECT * FROM manga_source_state ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MangaSourceStateEntity>>

    @Query("DELETE FROM manga_source_state WHERE sourceKey = :sourceKey")
    suspend fun deleteByKey(sourceKey: String)

    @Query("DELETE FROM manga_source_state")
    suspend fun clearAll()
}
