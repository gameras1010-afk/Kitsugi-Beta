package com.kitsugi.animelist.core.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manga görsel ve API istekleri için optimize edilmiş OkHttpClient sağlayıcısı.
 *
 * imageClient → manga sayfası resimleri için (CloudflareInterceptor dahil)
 * apiClient   → manga API çağrıları için (daha hafif)
 *
 * Konfigürasyon (PERFORMANCE_R2_PATCH'e göre):
 * - connectTimeout: 8s, readTimeout: 15s, writeTimeout: 15s
 * - connectionPool: 10 idle bağlantı, 5 dakika keep-alive
 * - dispatcher: max 64 istek, host başına max 8
 * - retryOnConnectionFailure: true
 * - Header'lar: User-Agent, Accept, Accept-Language
 */
@Singleton
class NuvioOkHttpProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 Nuvio/1.4"

        private val SHARED_CONNECTION_POOL = ConnectionPool(
            maxIdleConnections = 10,
            keepAliveDuration = 5,
            timeUnit = TimeUnit.MINUTES
        )

        private val SHARED_DISPATCHER = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 8
        }
    }

    /** Manga sayfası resimleri için — CloudflareInterceptor + RetryInterceptor dahil */
    val imageClient: OkHttpClient by lazy {
        buildBaseClient()
            .addInterceptor(CloudflareInterceptor(context))  // EN BAŞA ekle
            .addInterceptor(headerInterceptor())
            .addInterceptor(RetryInterceptor(maxRetries = 2))
            .build()
    }

    /** Manga API çağrıları için — daha hafif (CloudflareInterceptor opsiyonel) */
    val apiClient: OkHttpClient by lazy {
        buildBaseClient()
            .addInterceptor(headerInterceptor())
            .addInterceptor(RetryInterceptor(maxRetries = 2))
            .build()
    }

    private fun buildBaseClient() = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .connectionPool(SHARED_CONNECTION_POOL)
        .dispatcher(SHARED_DISPATCHER)
        .dns { hostname -> DnsManager.getActiveDns().lookup(hostname) }
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)

    private fun headerInterceptor() = Interceptor { chain ->
        val request: Request = chain.request().newBuilder()
            .header("User-Agent", USER_AGENT)
            .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
            .header("Accept-Language", "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()
        chain.proceed(request)
    }
}
