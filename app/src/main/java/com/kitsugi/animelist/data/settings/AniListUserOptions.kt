package com.kitsugi.animelist.data.settings

/**
 * V2-F02 – AniListUserOptions
 *
 * AniList kullanıcısının hesap genelindeki tercihlerini tutar.
 * AniHyou UserOptions referans alındı.
 *
 * Alanlar:
 *  - adult:              Yetişkin içerikleri göster/gizle
 *  - airingNotifications: Takip edilen animelerde yayın bildirimi
 *  - notificationOptions: Hangi bildirim tiplerinin aktif olduğu
 *  - timezone:           IANA timezone string (örn. "Europe/Istanbul")
 *  - activityMergeTime:  Aynı medya aktivitelerini kaç dakika içinde birleştir
 *  - staffNameLanguage:  Seiyuu/yönetmen isim dili (ROMAJI / NATIVE)
 *  - restrictMessagesToFollowing: Sadece takip edilenlerden mesaj al
 */
data class AniListUserOptions(
    val adult: Boolean = false,
    val airingNotifications: Boolean = true,
    val notificationOptions: List<AniListNotificationOption> = AniListNotificationOption.defaults(),
    val timezone: String = "Europe/Istanbul",
    val activityMergeTime: Int = 30,           // dakika; 0 = hiç birleştirme
    val staffNameLanguage: StaffNameLanguage = StaffNameLanguage.ROMAJI,
    val restrictMessagesToFollowing: Boolean = false
)

/**
 * Hangi AniList bildirim kategorisinin aktif olduğunu belirtir.
 */
data class AniListNotificationOption(
    val type: AniListNotificationCategory,
    val enabled: Boolean
) {
    companion object {
        fun defaults(): List<AniListNotificationOption> = AniListNotificationCategory.entries.map {
            AniListNotificationOption(type = it, enabled = it.defaultEnabled)
        }
    }
}

/**
 * AniList bildirim kategorileri.
 *
 * API'deki `notificationTypeSettings` alanına karşılık gelir.
 */
enum class AniListNotificationCategory(
    val apiValue: String,
    val label: String,
    val defaultEnabled: Boolean
) {
    AIRING(
        apiValue = "AIRING",
        label = "Yayın Bildirimleri",
        defaultEnabled = true
    ),
    ACTIVITY(
        apiValue = "ACTIVITY_MESSAGE",
        label = "Aktivite Mesajları",
        defaultEnabled = true
    ),
    ACTIVITY_REPLY(
        apiValue = "ACTIVITY_REPLY",
        label = "Aktivite Yanıtları",
        defaultEnabled = true
    ),
    ACTIVITY_MENTION(
        apiValue = "ACTIVITY_MENTION",
        label = "Etiketlemeler",
        defaultEnabled = true
    ),
    FOLLOWING(
        apiValue = "FOLLOWING",
        label = "Yeni Takipçiler",
        defaultEnabled = true
    ),
    FORUM_COMMENT(
        apiValue = "THREAD_COMMENT_MENTION",
        label = "Forum Yorumları",
        defaultEnabled = false
    ),
    FORUM_SUBSCRIBED(
        apiValue = "THREAD_SUBSCRIBED",
        label = "Abone Forum Güncellemeleri",
        defaultEnabled = false
    ),
    MEDIA_CHANGE(
        apiValue = "MEDIA_DATA_CHANGE",
        label = "Medya Verisi Değişiklikleri",
        defaultEnabled = false
    );

    companion object {
        fun fromApiValue(value: String): AniListNotificationCategory? =
            entries.find { it.apiValue == value }
    }
}

/**
 * Seiyuu ve yönetmen isimlerinin hangi dilde gösterileceğini belirler.
 */
enum class StaffNameLanguage(val apiValue: String, val label: String) {
    ROMAJI("ROMAJI", "Romaji"),
    NATIVE("NATIVE", "Yerel (Japonca vb.)"),
    ROMAJI_WESTERN("ROMAJI_WESTERN", "Romaji (Batı sırası)");

    companion object {
        fun fromApiValue(value: String) =
            entries.find { it.apiValue == value } ?: ROMAJI
    }
}
