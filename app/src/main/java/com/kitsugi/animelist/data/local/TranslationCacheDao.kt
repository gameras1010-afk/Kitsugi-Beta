package com.kitsugi.animelist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TranslationCacheDao {
    @Query("SELECT translatedText FROM translation_cache WHERE originalHash = :hash LIMIT 1")
    suspend fun getTranslation(hash: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(cache: TranslationCacheEntity)
}
