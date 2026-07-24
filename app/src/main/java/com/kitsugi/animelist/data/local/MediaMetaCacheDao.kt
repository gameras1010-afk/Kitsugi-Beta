package com.kitsugi.animelist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaMetaCacheDao {
    @Query("SELECT * FROM media_meta_cache WHERE tmdbId = :tmdbId")
    suspend fun getByTmdbId(tmdbId: Int): MediaMetaCacheEntity?

    @Query("SELECT * FROM media_meta_cache WHERE malId = :malId")
    suspend fun getByMalId(malId: Int): MediaMetaCacheEntity?

    @Query("SELECT * FROM media_meta_cache WHERE aniListId = :aniListId")
    suspend fun getByAniListId(aniListId: Int): MediaMetaCacheEntity?

    @Query("SELECT * FROM media_meta_cache WHERE kitsuId = :kitsuId")
    suspend fun getByKitsuId(kitsuId: String): MediaMetaCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: MediaMetaCacheEntity)

    @Query("DELETE FROM media_meta_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM media_meta_cache")
    suspend fun getCount(): Int
}
