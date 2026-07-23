package com.kitsugi.animelist.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * AniList mutasyonlarını (like, rate, reply, subscribe) yürüten istemci.
 * [KitsugiMediaTabsClient]'dan bölünmüştür.
 */
class KitsugiMediaMutationsClient {

    suspend fun toggleLike(id: Int, type: String): Boolean {
        return withContext(Dispatchers.IO) {
            val query = """
                mutation (${'$'}id: Int, ${'$'}type: LikeableType) {
                    ToggleLikeV2(id: ${'$'}id, type: ${'$'}type) { __typename }
                }
            """.trimIndent()
            val variables = JSONObject().put("id", id).put("type", type)
            runCatching {
                val response = KitsugiApiBase.executeAniListQuery(query, variables)
                response != null && !response.contains("errors")
            }.getOrElse { false }
        }
    }

    suspend fun rateReview(reviewId: Int, rating: String): Boolean {
        return withContext(Dispatchers.IO) {
            val query = """
                mutation (${'$'}reviewId: Int, ${'$'}rating: ReviewRating) {
                    RateReview(reviewId: ${'$'}reviewId, rating: ${'$'}rating) {
                        id rating ratingAmount userRating
                    }
                }
            """.trimIndent()
            val variables = JSONObject().put("reviewId", reviewId).put("rating", rating)
            runCatching {
                val response = KitsugiApiBase.executeAniListQuery(query, variables)
                response != null && !response.contains("errors")
            }.getOrElse { false }
        }
    }

    suspend fun postReply(targetId: Int, isActivity: Boolean, text: String, parentCommentId: Int? = null): Boolean {
        return withContext(Dispatchers.IO) {
            if (isActivity) {
                val query = """
                    mutation (${'$'}activityId: Int, ${'$'}text: String) {
                        SaveActivityReply(activityId: ${'$'}activityId, text: ${'$'}text) { id }
                    }
                """.trimIndent()
                val variables = JSONObject().put("activityId", targetId).put("text", text)
                runCatching {
                    val response = KitsugiApiBase.executeAniListQuery(query, variables)
                    response != null && !response.contains("errors")
                }.getOrElse { false }
            } else {
                val query = if (parentCommentId != null) {
                    """
                        mutation (${'$'}threadId: Int, ${'$'}comment: String, ${'$'}parentCommentId: Int) {
                            SaveThreadComment(threadId: ${'$'}threadId, comment: ${'$'}comment, parentCommentId: ${'$'}parentCommentId) { id }
                        }
                    """.trimIndent()
                } else {
                    """
                        mutation (${'$'}threadId: Int, ${'$'}comment: String) {
                            SaveThreadComment(threadId: ${'$'}threadId, comment: ${'$'}comment) { id }
                        }
                    """.trimIndent()
                }
                val variables = JSONObject().put("threadId", targetId).put("comment", text)
                if (parentCommentId != null) {
                    variables.put("parentCommentId", parentCommentId)
                }
                runCatching {
                    val response = KitsugiApiBase.executeAniListQuery(query, variables)
                    response != null && !response.contains("errors")
                }.getOrElse { false }
            }
        }
    }

    suspend fun toggleThreadSubscription(threadId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val query = """
                mutation (${'$'}threadId: Int) {
                    ToggleThreadSubscription(threadId: ${'$'}threadId) { id isSubscribed }
                }
            """.trimIndent()
            val variables = JSONObject().put("threadId", threadId)
            runCatching {
                val response = KitsugiApiBase.executeAniListQuery(query, variables)
                response != null && !response.contains("errors")
            }.getOrElse { false }
        }
    }

    /**
     * AniList ToggleFavourite mutasyonu — karakter, ekip üyesi ve stüdyo için.
     * @param entityType "character", "staff" veya "studio"
     * @param entityId AniList entity ID'si
     * @return true = başarılı toggle, false = hata
     */
    suspend fun toggleFavourite(entityType: String, entityId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val (argDecl, argParam, varKey) = when (entityType.lowercase()) {
                "character" -> Triple("\$characterId: Int", "characterId: \$characterId", "characterId")
                "staff"     -> Triple("\$staffId: Int",     "staffId: \$staffId",         "staffId")
                "studio"    -> Triple("\$studioId: Int",    "studioId: \$studioId",        "studioId")
                else        -> return@withContext false
            }
            val query = """
                mutation ($argDecl) {
                    ToggleFavourite($argParam) {
                        characters { nodes { id } }
                        staff { nodes { id } }
                        studios { nodes { id } }
                    }
                }
            """.trimIndent()
            val variables = JSONObject().put(varKey, entityId)
            runCatching {
                val response = KitsugiApiBase.executeAniListQuery(query, variables)
                response != null && !response.contains("errors")
            }.getOrElse { false }
        }
    }

    suspend fun deleteActivity(activityId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val query = """
                mutation (${'$'}id: Int) {
                    DeleteActivity(id: ${'$'}id) {
                        deleted
                    }
                }
            """.trimIndent()
            val variables = JSONObject().put("id", activityId)
            runCatching {
                val response = KitsugiApiBase.executeAniListQuery(query, variables)
                response != null && !response.contains("errors")
            }.getOrElse { false }
        }
    }
}
