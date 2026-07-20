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

    suspend fun postReply(targetId: Int, isActivity: Boolean, text: String): Boolean {
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
                val query = """
                    mutation (${'$'}threadId: Int, ${'$'}comment: String) {
                        SaveThreadComment(threadId: ${'$'}threadId, comment: ${'$'}comment) { id }
                    }
                """.trimIndent()
                val variables = JSONObject().put("threadId", targetId).put("comment", text)
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
}
