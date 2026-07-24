package com.kitsugi.animelist.data.remote

import com.kitsugi.animelist.KitsugiApplication
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class RateLimitException(message: String) : java.io.IOException(message)
class ResourceNotFoundException(message: String) : java.io.IOException(message)

object KitsugiApiBase {
    private const val MIN_REQUEST_INTERVAL_MS = 450L
    private val requestMutex = Mutex()
    private var lastRequestTime: Long = 0L

    // ─── AniList'e özel hız sınırlama (Jikan'dan bağımsız) ──────────────────────
    // AniList resmi limiti dakikada ~90 istek; güvenli tarafta kalmak için
    // istekler arası minimum 700ms ve aynı anda tek istek (Mutex) uygulanır.
    private const val ANILIST_MIN_INTERVAL_MS = 700L
    private const val ANILIST_MAX_RETRIES = 3
    private val aniListMutex = Mutex()
    private var lastAniListRequestTime: Long = 0L

    suspend fun <T> runWithRateLimit(block: suspend () -> T): T {
        return requestMutex.withLock {
            waitForRateLimitWindow()
            block()
        }
    }

    private suspend fun waitForRateLimitWindow() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime
        val waitMs = MIN_REQUEST_INTERVAL_MS - elapsed

        if (waitMs > 0) {
            delay(waitMs)
        }

        lastRequestTime = System.currentTimeMillis()
    }

    private suspend fun waitForAniListWindow() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastAniListRequestTime
        val waitMs = ANILIST_MIN_INTERVAL_MS - elapsed

        if (waitMs > 0) {
            delay(waitMs)
        }

        lastAniListRequestTime = System.currentTimeMillis()
    }

    fun executeGetRequest(url: URL): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "KitsugiAnimeList/1.0")
            .build()

        return try {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    if (response.code == 429) {
                        android.util.Log.w("KitsugiApiBase", "HTTP 429 Too Many Requests: Rate limit hit for URL: $url")
                    } else {
                        android.util.Log.w("KitsugiApiBase", "HTTP Error: ${response.code} ${response.message} for URL: $url")
                    }
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("KitsugiApiBase", "executeGetRequest Exception: ${e.message} for URL: $url", e)
            null
        }
    }

    fun executeGetRequestOrThrow(url: URL): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "KitsugiAnimeList/1.0")
            .build()

        com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return response.body?.string().orEmpty()
            } else {
                if (response.code == 429) {
                    throw RateLimitException("HTTP 429 Too Many Requests: Rate limit hit for URL: $url")
                } else if (response.code == 404) {
                    throw ResourceNotFoundException("HTTP 404 Not Found for URL: $url")
                } else {
                    throw java.io.IOException("HTTP Error ${response.code}: ${response.message} for URL: $url")
                }
            }
        }
    }

    /**
     * AniList GraphQL sorgusu çalıştırır.
     *
     * Hız sınırlama (rate limiting) ve 429 dayanıklılığı BURADA yönetilir:
     *  - [aniListMutex] ile aynı anda tek istek gider (paralel patlamayı önler).
     *  - Her istekten önce [waitForAniListWindow] ile ~700ms bekleme uygulanır.
     *  - 429 (Too Many Requests) veya 5xx gelirse, "Retry-After" başlığına saygı
     *    göstererek üstel bekleme ile [ANILIST_MAX_RETRIES] kez tekrar denenir.
     *
     * NOT: Fonksiyon artık `suspend`. Tüm çağıranlar zaten suspend/withContext(IO)
     * bağlamında olduğu için imza değişikliği çağrı yerlerini bozmaz.
     */
    suspend fun executeAniListQuery(query: String, variables: JSONObject, accessToken: String? = null): String? {
        val requestBody = JSONObject()
            .put("query", query)
            .put("variables", variables)
            .toString()

        val token = if (!accessToken.isNullOrBlank()) {
            accessToken
        } else {
            KitsugiApplication.getInstance()?.let { ctx ->
                ExternalAuthManager.getAniListToken(ctx)
            }
        }

        var attempt = 0
        var tokenFailed = false
        while (true) {
            val currentToken = if (tokenFailed) null else token
            // Sırayla + throttle uygulayarak tek istek gönder
            val result = aniListMutex.withLock {
                waitForAniListWindow()
                performAniListRequest(requestBody, currentToken)
            }

            when (result) {
                is AniListResult.Success -> {
                    // JSON içindeki authorization veya invalid token hatalarını kontrol et
                    val hasAuthError = try {
                        val root = JSONObject(result.body)
                        val errors = root.optJSONArray("errors")
                        if (errors != null && errors.length() > 0) {
                            val msg = errors.optJSONObject(0)?.optString("message").orEmpty().lowercase()
                            msg.contains("invalid token") || msg.contains("unauthorized") || msg.contains("forbidden") || msg.contains("invalid credentials")
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    }
                    if (hasAuthError && !currentToken.isNullOrBlank()) {
                        tokenFailed = true
                        continue
                    }
                    return result.body
                }
                is AniListResult.Failure -> {
                    if (!currentToken.isNullOrBlank()) {
                        // Token geçersiz veya yetkisiz, tokensız tekrar dene
                        tokenFailed = true
                        continue
                    }
                    return null   // Kalıcı hata (4xx, retry anlamsız)
                }
                is AniListResult.Retryable -> {
                    if (attempt >= ANILIST_MAX_RETRIES) return null
                    // Retry-After (saniye) verilmişse ona uy; yoksa üstel bekleme
                    val backoffMs = result.retryAfterMs
                        ?: (1_000L * (1 shl attempt))   // 1s, 2s, 4s...
                    delay(backoffMs)
                    attempt++
                }
            }
        }
    }

    private sealed class AniListResult {
        data class Success(val body: String) : AniListResult()
        data object Failure : AniListResult()
        data class Retryable(val retryAfterMs: Long?) : AniListResult()
    }

    private fun performAniListRequest(requestBody: String, token: String?): AniListResult {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = requestBody.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://graphql.anilist.co")
            .post(body)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "KitsugiAnimeList/1.0")
            .apply {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }
            .build()

        return try {
            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    AniListResult.Success(responseBody)
                } else if (response.code == 429 || response.code in 500..599) {
                    android.util.Log.w("KitsugiApiBase", "AniList query retryable failure with code: ${response.code} ${response.message}")
                    val retryAfterSec = response.header("Retry-After")?.toLongOrNull()
                    val resetEpoch = response.header("X-RateLimit-Reset")?.toLongOrNull()
                    val retryAfterMs = when {
                        retryAfterSec != null -> retryAfterSec * 1_000L
                        resetEpoch != null -> (resetEpoch * 1_000L - System.currentTimeMillis()).coerceAtLeast(0L)
                        else -> null
                    }
                    AniListResult.Retryable(retryAfterMs)
                } else {
                    android.util.Log.e("KitsugiApiBase", "AniList query permanent failure with code: ${response.code} ${response.message}")
                    AniListResult.Failure
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("KitsugiApiBase", "AniList performRequest Exception: ${e.message}", e)
            AniListResult.Retryable(null)
        }
    }

    fun classifyNetworkError(error: Throwable): NetworkErrorCategory {
        val message = error.message.orEmpty()

        val isRateLimited = message.contains("429") ||
                message.contains("rate", ignoreCase = true) ||
                message.contains("limit", ignoreCase = true)

        if (isRateLimited) {
            return NetworkErrorCategory.RateLimited
        }

        val isGatewayProblem = message.contains("504") ||
                message.contains("502") ||
                message.contains("503") ||
                message.contains("gateway", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true)

        if (isGatewayProblem) {
            return NetworkErrorCategory.GatewayProblem
        }

        return NetworkErrorCategory.Other
    }

    enum class NetworkErrorCategory {
        RateLimited,
        GatewayProblem,
        Other
    }
}

// JSON and String helper extensions
fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val value = optString(key)
    return if (value == "null" || value.isBlank()) null else value
}

fun JSONObject.optionalPositiveInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    val value = optInt(key, 0)
    return if (value > 0) value else null
}

fun JSONObject.namesFromObjectArray(key: String): List<String> {
    val array = optJSONArray(key) ?: return emptyList()
    val names = mutableListOf<String>()

    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        val name = item.optString("name")
        if (name.isNotBlank() && name != "null") {
            names.add(name)
        }
    }

    return names
}

fun JSONObject.namesFromStringArray(key: String): List<String> {
    val array = optJSONArray(key) ?: return emptyList()
    val names = mutableListOf<String>()

    for (index in 0 until array.length()) {
        val name = array.optString(index)
        if (name.isNotBlank() && name != "null") {
            names.add(name)
        }
    }

    return names
}

fun String.cleanApiText(): String {
    return this
        .replace("<br>", "\n")
        .replace("<br />", "\n")
        .replace("<br/>", "\n")
        .replace("<i>", "")
        .replace("</i>", "")
        .replace("<b>", "")
        .replace("</b>", "")
        .replace(Regex("<.*?>"), "")
        // HTML entity decode
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&apos;", "'")
        .replace("&#039;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace("&ldquo;", "\u201C")
        .replace("&rdquo;", "\u201D")
        .replace("&lsquo;", "\u2018")
        .replace("&rsquo;", "\u2019")
        .replace("&mdash;", "\u2014")
        .replace("&ndash;", "\u2013")
        .replace("&hellip;", "\u2026")
        // Remove excessive blank lines (3+ newlines → 2 newlines)
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}
