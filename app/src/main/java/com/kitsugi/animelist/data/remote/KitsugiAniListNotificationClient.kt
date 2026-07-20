package com.kitsugi.animelist.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * AniList bildirim (notification) API'sini yöneten istemci.
 *
 * TASK-108: Bildirim listeleme ve okunmamış sayımı için GraphQL query'leri.
 *
 * AniList API endpoint: https://graphql.anilist.co
 * Kimlik doğrulama: Bearer token (yalnızca oturum açık kullanıcılar için bildirim alınabilir)
 */
class KitsugiAniListNotificationClient {

    // ─────────────────────────────────────────────────────────────────────────
    // Veri modelleri
    // ─────────────────────────────────────────────────────────────────────────

    enum class NotificationGroup(val apiTypes: List<String>) {
        ALL(emptyList()),
        AIRING(listOf("AIRING")),
        ACTIVITY(listOf(
            "ACTIVITY_MESSAGE", "ACTIVITY_REPLY", "ACTIVITY_REPLY_SUBSCRIBED",
            "ACTIVITY_MENTION", "ACTIVITY_LIKE", "ACTIVITY_REPLY_LIKE"
        )),
        FORUM(listOf(
            "THREAD_COMMENT_MENTION", "THREAD_COMMENT_REPLY", "THREAD_COMMENT_SUBSCRIBED",
            "THREAD_COMMENT_LIKE", "THREAD_LIKE"
        )),
        FOLLOWS(listOf("FOLLOWING")),
        MEDIA(listOf("RELATED_MEDIA_ADDITION", "MEDIA_DATA_CHANGE", "MEDIA_MERGE", "MEDIA_DELETION"))
    }

    data class KitsugiNotification(
        val id: Int,
        val type: String,
        val context: String?,            // Açıklama metni (ör. "beğendi yorumunuzu")
        val createdAt: Long?,
        val dateText: String?,
        // Kullanıcı bağlantılı bildirimler
        val userId: Int?,
        val userName: String?,
        val userAvatarUrl: String?,
        // Medya bağlantılı bildirimler
        val mediaId: Int?,
        val mediaTitle: String?,
        val mediaCoverUrl: String?,
        val episode: Int?,               // Yayın bildirimleri için bölüm numarası
        val airingContexts: List<String>?, // AiringNotification.contexts listesi
        // Forum / aktivite bağlantılı bildirimler
        val activityId: Int?,
        val commentId: Int?,
        val threadId: Int?,
        val threadTitle: String?,
        // Medya silme
        val deletedMediaTitle: String?,
        val reason: String?
    )

    data class KitsugiNotificationPage(
        val notifications: List<KitsugiNotification>,
        val hasNextPage: Boolean,
        val currentPage: Int
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Okunmamış bildirim sayısı
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Kullanıcının okunmamış bildirim sayısını döndürür.
     * Token olmadan null döner.
     */
    suspend fun fetchUnreadCount(accessToken: String): Int? {
        return withContext(Dispatchers.IO) {
            val query = """
                query UnreadNotificationCount {
                    Viewer {
                        unreadNotificationCount
                    }
                }
            """.trimIndent()
            runCatching {
                val response = KitsugiApiBase.executeAniListQuery(query, JSONObject(), accessToken)
                    ?: return@runCatching null
                JSONObject(response)
                    .optJSONObject("data")
                    ?.optJSONObject("Viewer")
                    ?.optInt("unreadNotificationCount")
            }.getOrNull()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bildirim listesi
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sayfalandırılmış AniList bildirimlerini çeker.
     *
     * @param accessToken Kullanıcının AniList Bearer token'ı
     * @param page Sayfa numarası (1'den başlar)
     * @param perPage Sayfa başına bildirim sayısı (maks. 25 önerilir)
     * @param group Bildirim grubu filtresi; [NotificationGroup.ALL] tüm bildirimleri getirir
     * @param resetCount true ise sunucu okunmamış sayacını sıfırlar
     */
    suspend fun fetchNotifications(
        accessToken: String,
        page: Int = 1,
        perPage: Int = 25,
        group: NotificationGroup = NotificationGroup.ALL,
        resetCount: Boolean = false
    ): KitsugiNotificationPage {
        return withContext(Dispatchers.IO) {
            val typeInParam = if (group.apiTypes.isEmpty()) "" else ", \$typeIn: [NotificationType]"
            val typeInArg   = if (group.apiTypes.isEmpty()) "" else ", type_in: \$typeIn"

            // TASK-108: Tüm bildirim alt tiplerini kapsayan kapsamlı query
            val query = """
                query Notifications(${'$'}page: Int, ${'$'}perPage: Int$typeInParam, ${'$'}resetCount: Boolean) {
                    Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                        notifications(resetNotificationCount: ${'$'}resetCount$typeInArg) {
                            ... on AiringNotification {
                                id contexts animeId episode
                                media { title { userPreferred } coverImage { medium large } }
                                type createdAt
                            }
                            ... on FollowingNotification {
                                id context userId
                                user { name avatar { medium } }
                                type createdAt
                            }
                            ... on ActivityMessageNotification {
                                id context activityId userId
                                user { name avatar { medium } }
                                type createdAt
                            }
                            ... on ActivityMentionNotification {
                                id context activityId userId
                                user { name avatar { medium } }
                                type createdAt
                            }
                            ... on ActivityReplyNotification {
                                id context activityId userId
                                user { name avatar { medium } }
                                type createdAt
                            }
                            ... on ActivityReplySubscribedNotification {
                                id context activityId userId
                                user { name avatar { medium } }
                                type createdAt
                            }
                            ... on ActivityLikeNotification {
                                id context activityId userId
                                user { name avatar { medium } }
                                type createdAt
                            }
                            ... on ActivityReplyLikeNotification {
                                id context activityId userId
                                user { name avatar { medium } }
                                type createdAt
                            }
                            ... on ThreadCommentMentionNotification {
                                id context commentId
                                thread { id title }
                                userId
                                user { name avatar { medium } }
                                type createdAt
                            }
                            ... on ThreadCommentReplyNotification {
                                id context commentId
                                thread { id title }
                                userId
                                user { name avatar { medium } }
                                type createdAt
                            }
                            ... on ThreadCommentSubscribedNotification {
                                id context commentId
                                thread { id title }
                                userId
                                user { name avatar { medium } }
                                type createdAt
                            }
                            ... on ThreadCommentLikeNotification {
                                id context commentId
                                thread { id title }
                                userId
                                user { name avatar { medium } }
                                type createdAt
                            }
                            ... on ThreadLikeNotification {
                                id context threadId userId
                                user { name avatar { medium } }
                                type createdAt
                            }
                            ... on RelatedMediaAdditionNotification {
                                id context mediaId
                                media { title { userPreferred } coverImage { medium large } }
                                type createdAt
                            }
                            ... on MediaDataChangeNotification {
                                id context mediaId reason
                                media { title { userPreferred } coverImage { medium large } }
                                type createdAt
                            }
                            ... on MediaMergeNotification {
                                id context reason mediaId
                                media { title { userPreferred } coverImage { medium large } }
                                type createdAt
                            }
                            ... on MediaDeletionNotification {
                                id context reason deletedMediaTitle
                                type createdAt
                            }
                        }
                        pageInfo { currentPage hasNextPage }
                    }
                }
            """.trimIndent()

            val variables = JSONObject()
                .put("page", page)
                .put("perPage", perPage)
                .put("resetCount", resetCount)
            if (group.apiTypes.isNotEmpty()) {
                val arr = org.json.JSONArray()
                group.apiTypes.forEach { arr.put(it) }
                variables.put("typeIn", arr)
            }

            runCatching {
                val response = KitsugiApiBase.executeAniListQuery(query, variables, accessToken)
                    ?: return@runCatching KitsugiNotificationPage(emptyList(), false, page)

                val pageObj = JSONObject(response)
                    .optJSONObject("data")
                    ?.optJSONObject("Page")
                    ?: return@runCatching KitsugiNotificationPage(emptyList(), false, page)

                val notificationsArr = pageObj.optJSONArray("notifications")
                val pageInfo = pageObj.optJSONObject("pageInfo")
                val hasNext = pageInfo?.optBoolean("hasNextPage", false) ?: false
                val currentP = pageInfo?.optInt("currentPage", page) ?: page

                val list = mutableListOf<KitsugiNotification>()
                if (notificationsArr != null) {
                    for (i in 0 until notificationsArr.length()) {
                        val item = notificationsArr.optJSONObject(i) ?: continue
                        list.add(parseNotification(item))
                    }
                }
                KitsugiNotificationPage(list, hasNext, currentP)
            }.getOrElse { KitsugiNotificationPage(emptyList(), false, page) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — JSON parser
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseNotification(item: JSONObject): KitsugiNotification {
        val type = item.optString("type", "UNKNOWN")
        val createdAtSec = item.optLong("createdAt").takeIf { it > 0 }
        val dateText = createdAtSec?.let {
            try {
                java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(it * 1000L))
            } catch (_: Exception) { null }
        }

        // Kullanıcı alanları
        val userObj = item.optJSONObject("user")
        val userName = userObj?.optNullableString("name")
        val userAvatarUrl = userObj?.optJSONObject("avatar")?.optNullableString("medium")

        // Medya alanları
        val mediaObj = item.optJSONObject("media")
        val mediaTitle = mediaObj?.optJSONObject("title")?.optNullableString("userPreferred")
        val mediaCoverUrl = mediaObj?.optJSONObject("coverImage")?.let {
            it.optNullableString("large") ?: it.optNullableString("medium")
        }

        // Thread alanları
        val threadObj = item.optJSONObject("thread")
        val threadTitle = threadObj?.optNullableString("title")

        // Contexts (AiringNotification) — JSONArray → List<String>
        val contextsArr = item.optJSONArray("contexts")
        val airingContexts = if (contextsArr != null) {
            (0 until contextsArr.length()).mapNotNull { contextsArr.optString(it).takeIf { s -> s.isNotBlank() } }
        } else null

        return KitsugiNotification(
            id                = item.optInt("id"),
            type              = type,
            context           = item.optNullableString("context"),
            createdAt         = createdAtSec,
            dateText          = dateText,
            userId            = item.optInt("userId").takeIf { it > 0 },
            userName          = userName,
            userAvatarUrl     = userAvatarUrl,
            mediaId           = (item.optInt("mediaId").takeIf { it > 0 }) ?: (item.optInt("animeId").takeIf { it > 0 }),
            mediaTitle        = mediaTitle,
            mediaCoverUrl     = mediaCoverUrl,
            episode           = item.optInt("episode").takeIf { it > 0 },
            airingContexts    = airingContexts,
            activityId        = item.optInt("activityId").takeIf { it > 0 },
            commentId         = item.optInt("commentId").takeIf { it > 0 },
            threadId          = (item.optInt("threadId").takeIf { it > 0 }) ?: threadObj?.optInt("id")?.takeIf { it > 0 },
            threadTitle       = threadTitle,
            deletedMediaTitle = item.optNullableString("deletedMediaTitle"),
            reason            = item.optNullableString("reason")
        )
    }
}
