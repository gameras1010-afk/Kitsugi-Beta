package com.kitsugi.animelist.data.cloudstream

import android.content.Context
import com.lagradost.cloudstream3.Prerelease
import com.lagradost.cloudstream3.UnsafeSSL
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.insecureApp
import com.lagradost.nicehttp.ignoreAllSSLErrors
import com.lagradost.cloudstream3.utils.extractorApis
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object CsRuntimeInit {
    private var wrappedContext: Context? = null

    @OptIn(Prerelease::class, UnsafeSSL::class)
    fun init(context: Context) {
        try {
            val wrapped = com.kitsugi.animelist.KitsugiApplication.getDynamicContext(context)
            com.lagradost.api.setContext(java.lang.ref.WeakReference(wrapped))

            // Build a standard OkHttp client with cache, timeouts and redirect handling.
            // Android 10+ already includes Conscrypt as the default TLS provider natively;
            // no separate registration needed.
            val httpCache = Cache(
                directory = File(context.cacheDir, "cs_http_cache"),
                maxSize = 50L * 1024L * 1024L // 50 MiB
            )
            val okHttpClient = OkHttpClient.Builder()
                .dns(com.kitsugi.animelist.core.network.IPv4FirstDns())
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val hasUserAgent = original.header("User-Agent") != null
                    val requestBuilder = original.newBuilder()
                    if (!hasUserAgent) {
                        requestBuilder.header("User-Agent", com.lagradost.cloudstream3.network.CloudflareKiller.UNIFIED_USER_AGENT)
                    }
                    chain.proceed(requestBuilder.build())
                }
                .addInterceptor(com.lagradost.cloudstream3.network.CloudflareKiller())
                .addInterceptor(com.lagradost.cloudstream3.network.DdosGuardKiller(alwaysBypass = false))
                .cache(httpCache)
                .build()

            // Build insecure OkHttp client to ignore SSL errors for insecureApp.
            // Some Turkish anime providers have expired/self-signed certificates.
            val insecureOkHttpClient = okHttpClient.newBuilder()
                .ignoreAllSSLErrors()
                .build()

            // Set both default app.baseClient and insecureApp.baseClient to use
            // the SSL-ignoring client. This is crucial for Turkish plugins that fetch
            // API endpoints from self-signed or expired SSL domains using default app calls.
            app.baseClient = insecureOkHttpClient
            insecureApp.baseClient = insecureOkHttpClient

            val extractorsCount = extractorApis.size
            android.util.Log.d(
                "CsRuntimeInit",
                "Cloudstream runtime ready. Clients initialized. $extractorsCount built-in extractors registered."
            )
        } catch (e: Exception) {
            android.util.Log.e("CsRuntimeInit", "Failed to initialize Cloudstream runtime", e)
        }
    }
}

