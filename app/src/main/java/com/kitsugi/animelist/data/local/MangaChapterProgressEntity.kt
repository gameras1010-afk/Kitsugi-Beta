package com.kitsugi.animelist.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Kullanıcının her manga bölümündeki okuma ilerlemesini (son okunan sayfa)
 * ve bölümün tamamlanıp tamamlanmadığını saklar.
 *
 * primaryKey: chapterUrl — her bölüm URL'si benzersizdir.
 */
@Entity(
    tableName = "manga_chapter_progress",
    indices = [Index(value = ["mangaUrl"], name = "idx_manga_progress_mangaUrl")]
)
data class MangaChapterProgressEntity(
    @PrimaryKey
    val chapterUrl: String,
    val mangaUrl: String,
    val chapterName: String,
    @ColumnInfo(defaultValue = "0")
    val lastPageIndex: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val totalPages: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface MangaChapterProgressDao {

    /** Son okunan ilerlemeyi kaydeder veya günceller. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: MangaChapterProgressEntity)

    /** Belirli bir bölümün ilerlemesini döndürür; yoksa null. */
    @Query("SELECT * FROM manga_chapter_progress WHERE chapterUrl = :chapterUrl")
    suspend fun getProgress(chapterUrl: String): MangaChapterProgressEntity?

    /** Belirli bir manga'nın tüm bölüm ilerlemelerini döndürür. */
    @Query("SELECT * FROM manga_chapter_progress WHERE mangaUrl = :mangaUrl ORDER BY updatedAt DESC")
    suspend fun getAllForManga(mangaUrl: String): List<MangaChapterProgressEntity>

    /** Tamamlanan bölümleri döndürür. */
    @Query("SELECT * FROM manga_chapter_progress WHERE mangaUrl = :mangaUrl AND isCompleted = 1")
    suspend fun getCompletedChapters(mangaUrl: String): List<MangaChapterProgressEntity>

    /** Bir manga'nın tüm okuma geçmişini siler. */
    @Query("DELETE FROM manga_chapter_progress WHERE mangaUrl = :mangaUrl")
    suspend fun clearForManga(mangaUrl: String)
}
