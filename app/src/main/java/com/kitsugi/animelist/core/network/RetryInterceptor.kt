package com.kitsugi.animelist.core.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * OkHttp interceptor'ı: 5xx, 429 ve IOException durumlarında
 * üstel geri çekilme (exponential backoff) ile yeniden deneme.
 *
 * — 5xx hatası → 300ms, 600ms gecikme ile 2 retry
 * — 429 Too Many Requests → Retry-After header'a uyulur (max 30s)
 * — IOException → 400ms, 800ms gecikme ile 2 retry
 */
class RetryInterceptor(
    private val maxRetries: Int = 2
) : Interceptor {

    companion object {
        private const val TAG = "RetryInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null

        repeat(maxRetries + 1) { attempt ->
            if (attempt > 0) {
                Log.d(TAG, "Retry $attempt/${maxRetries} for ${request.url}")
            }

            try {
                val response = chain.proceed(request)

                // 429 Too Many Requests — Retry-After header'a uy
                if (response.code == 429 && attempt < maxRetries) {
                    val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull() ?: 5L
                    val delayMs = minOf(retryAfterSeconds * 1000L, 30_000L)
                    Log.w(TAG, "429 alındı. Retry-After: ${delayMs}ms — ${request.url}")
                    response.close()
                    Thread.sleep(delayMs)
                    return@repeat // devam et, retry
                }

                // 5xx Server Error — exponential backoff
                if (response.code in 500..599 && attempt < maxRetries) {
                    val delayMs = 300L * (1L shl attempt) // 300ms → 600ms
                    Log.w(TAG, "${response.code} server error. Retry after ${delayMs}ms — ${request.url}")
                    response.close()
                    Thread.sleep(delayMs)
                    return@repeat // devam et, retry
                }

                // Başarılı ya da son deneme — response'u döndür
                return response

            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries) {
                    val delayMs = 400L * (1L shl attempt) // 400ms → 800ms
                    Log.w(TAG, "IOException (attempt $attempt): ${e.message}. Retry after ${delayMs}ms — ${request.url}")
                    Thread.sleep(delayMs)
                }
            }
        }

        // Tüm denemeler bitti, son exception'ı fırlat
        throw lastException ?: IOException("Request failed after $maxRetries retries: ${request.url}")
    }
}
