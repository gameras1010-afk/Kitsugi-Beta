package com.kitsugi.animelist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_cache")
data class TranslationCacheEntity(
    @PrimaryKey
    val originalHash: String, // MD5 or SHA-256 hash of original text
    val translatedText: String
)
