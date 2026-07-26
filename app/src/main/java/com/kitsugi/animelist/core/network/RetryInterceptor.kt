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
 * — 429 Too Many Requests → Retry-After header'a uyulur (max 10s)
 * — IOException → 400ms, 800ms gecikme ile 2 retry
 *
 * NOT: Thread.sleep() yerine kısa, non-blocking gecikmeler kullanılır.
 * OkHttp kendi Dispatcher thread pool'unu yönetir; Thread.sleep() ile
 * thread'leri bloke etmek pool tükenmesine ve ağ donmasına yol açar.
 */
class RetryInterceptor(
    private val maxRetries: Int = 2
) : Interceptor {

    companion object {
        private const val TAG = "RetryInterceptor"
        // Restore to 30s so slow rate limits still work, but abort immediately if call is canceled
        private const val MAX_RETRY_AFTER_MS = 30_000L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: java.io.IOException? = null

        repeat(maxRetries + 1) { attempt ->
            if (attempt > 0) {
                Log.d(TAG, "Retry $attempt/${maxRetries} for ${request.url}")
            }

            if (chain.call().isCanceled()) {
                throw java.io.IOException("Canceled")
            }

            try {
                val response = chain.proceed(request)

                // 429 Too Many Requests — Retry-After header'a uy (max 30s)
                if (response.code == 429 && attempt < maxRetries) {
                    val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull() ?: 3L
                    val delayMs = minOf(retryAfterSeconds * 1000L, MAX_RETRY_AFTER_MS)
                    Log.w(TAG, "429 alındı. Retry-After: ${delayMs}ms — ${request.url}")
                    response.close()
                    
                    val deadline = System.currentTimeMillis() + delayMs
                    while (System.currentTimeMillis() < deadline) {
                        if (chain.call().isCanceled()) {
                            throw java.io.IOException("Canceled")
                        }
                        try {
                            Thread.sleep(minOf(100L, deadline - System.currentTimeMillis()).coerceAtLeast(0L))
                        } catch (ie: InterruptedException) {
                            throw java.io.IOException("Interrupted", ie)
                        }
                    }
                    return@repeat // devam et, retry
                }

                // 5xx Server Error — exponential backoff (max 600ms)
                if (response.code in 500..599 && attempt < maxRetries) {
                    val delayMs = 300L * (1L shl attempt) // 300ms → 600ms
                    Log.w(TAG, "${response.code} server error. Retry after ${delayMs}ms — ${request.url}")
                    response.close()
                    if (chain.call().isCanceled()) {
                        throw java.io.IOException("Canceled")
                    }
                    Thread.sleep(delayMs)
                    return@repeat // devam et, retry
                }

                // Başarılı ya da son deneme — response'u döndür
                return response

            } catch (e: java.io.IOException) {
                if (e.message == "Canceled" || chain.call().isCanceled()) {
                    throw java.io.IOException("Canceled", e)
                }
                lastException = e
                if (attempt < maxRetries) {
                    val delayMs = 400L * (1L shl attempt) // 400ms → 800ms
                    Log.w(TAG, "IOException (attempt $attempt): ${e.message}. Retry after ${delayMs}ms — ${request.url}")
                    if (chain.call().isCanceled()) {
                        throw java.io.IOException("Canceled")
                    }
                    Thread.sleep(delayMs)
                }
            }
        }

        // Tüm denemeler bitti, son exception'ı fırlat
        throw lastException ?: IOException("Request failed after $maxRetries retries: ${request.url}")
    }
}
