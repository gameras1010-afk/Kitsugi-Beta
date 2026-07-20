package com.kitsugi.animelist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Çevrimdışıyken veya token expire olduğunda başarısız olan
 * API sync işlemlerini saklar. Uygulama tekrar açılınca veya
 * bağlantı gelince otomatik olarak yeniden denenir.
 */
@Entity(tableName = "pending_syncs")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** "UPDATE" veya "DELETE" */
    val operation: String,

    /** MediaEntry'nin JSON hali — org.json.JSONObject ile serileştirilir */
    val entryJson: String,

    /** İşlemin ilk oluşturulma zamanı (ms) */
    val createdAt: Long = System.currentTimeMillis(),

    /** Kaç kez denendi — 5'e ulaşınca kayıt temizlenir */
    val retryCount: Int = 0
)
