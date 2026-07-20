package com.kitsugi.animelist.data.notifications

/**
 * V2-F04 – NotificationTypes
 *
 * AniList bildirim tipleri + Android notification channel tanımları.
 * AniHyou NotificationsViewModel referans alındı.
 */

/**
 * AniList API'den gelen bildirim tipi sabitleri.
 */
enum class AniListNotificationType(val apiValue: String) {
    ACTIVITY_MESSAGE("ACTIVITY_MESSAGE"),
    ACTIVITY_REPLY("ACTIVITY_REPLY"),
    ACTIVITY_REPLY_SUBSCRIBED("ACTIVITY_REPLY_SUBSCRIBED"),
    ACTIVITY_MENTION("ACTIVITY_MENTION"),
    ACTIVITY_LIKE("ACTIVITY_LIKE"),
    ACTIVITY_REPLY_LIKE("ACTIVITY_REPLY_LIKE"),
    FOLLOWING("FOLLOWING"),
    AIRING("AIRING"),
    RELATED_MEDIA_ADDITION("RELATED_MEDIA_ADDITION"),
    MEDIA_DATA_CHANGE("MEDIA_DATA_CHANGE"),
    MEDIA_MERGE("MEDIA_MERGE"),
    MEDIA_DELETION("MEDIA_DELETION"),
    THREAD_COMMENT_MENTION("THREAD_COMMENT_MENTION"),
    THREAD_SUBSCRIBED("THREAD_SUBSCRIBED"),
    THREAD_COMMENT_REPLY("THREAD_COMMENT_REPLY"),
    THREAD_LIKE("THREAD_LIKE"),
    THREAD_COMMENT_LIKE("THREAD_COMMENT_LIKE");

    companion object {
        fun fromApiValue(value: String): AniListNotificationType? =
            entries.find { it.apiValue == value }
    }
}

/**
 * Android notification channel ID'leri.
 */
object NotificationChannels {
    const val AIRING = "kitsugi_airing"
    const val ACTIVITY = "kitsugi_activity"
    const val SOCIAL = "kitsugi_social"
    const val SYSTEM = "kitsugi_system"
    const val UPDATES = "kitsugi_updates"
    const val DOWNLOADS = "kitsugi_downloads"
}

/**
 * Bildirim verisi modeli.
 */
data class KitsugiNotification(
    val id: Int,
    val type: AniListNotificationType,
    val title: String,
    val body: String,
    val mediaId: Int? = null,
    val activityId: Int? = null,
    val userId: Int? = null,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

/**
 * Kullanıcının bildirim tercih ayarları.
 */
data class NotificationPreferences(
    val airingEnabled: Boolean = true,
    val activityEnabled: Boolean = true,
    val socialEnabled: Boolean = true,
    val forumEnabled: Boolean = false,
    val followEnabled: Boolean = true,
    val mediaChangesEnabled: Boolean = false
)

/**
 * Yardımcı extension — bildirim tipinin hangi kanala ait olduğunu döner.
 */
fun AniListNotificationType.toChannelId(): String = when (this) {
    AniListNotificationType.AIRING -> NotificationChannels.AIRING
    AniListNotificationType.FOLLOWING -> NotificationChannels.SOCIAL
    AniListNotificationType.THREAD_COMMENT_MENTION,
    AniListNotificationType.THREAD_SUBSCRIBED,
    AniListNotificationType.THREAD_COMMENT_REPLY,
    AniListNotificationType.THREAD_LIKE,
    AniListNotificationType.THREAD_COMMENT_LIKE -> NotificationChannels.SOCIAL
    AniListNotificationType.MEDIA_DATA_CHANGE,
    AniListNotificationType.MEDIA_MERGE,
    AniListNotificationType.MEDIA_DELETION,
    AniListNotificationType.RELATED_MEDIA_ADDITION -> NotificationChannels.UPDATES
    else -> NotificationChannels.ACTIVITY
}
