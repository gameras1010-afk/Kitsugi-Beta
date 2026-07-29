package com.kitsugi.animelist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_buttons")
data class CustomButton(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isFavorite: Boolean,
    val sortIndex: Long,
    val content: String,
    val longPressContent: String,
    val onStartup: String
) {
    fun getButtonContent(primaryId: Long): String {
        val isPrimary = if (primaryId == id) "true" else "false"
        return content.replace("\$id", id.toString()).replace("\$isPrimary", isPrimary)
    }

    fun getButtonLongPressContent(primaryId: Long): String {
        val isPrimary = if (primaryId == id) "true" else "false"
        return longPressContent.replace("\$id", id.toString()).replace("\$isPrimary", isPrimary)
    }

    fun getButtonOnStartup(primaryId: Long): String {
        val isPrimary = if (primaryId == id) "true" else "false"
        return onStartup.replace("\$id", id.toString()).replace("\$isPrimary", isPrimary)
    }
}
