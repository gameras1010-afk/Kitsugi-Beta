package eu.kanade.tachiyomi.network

import android.content.Context
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.kitsugi.animelist.core.network.IPv4FirstDns
import com.lagradost.nicehttp.ignoreAllSSLErrors

class NetworkHelper(context: Context) {
    val cookieJar = WebViewCookieJar(context)

    val client: OkHttpClient = OkHttpClient.Builder()
        .dns(IPv4FirstDns())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request()
            var url = request.url
            var host = url.host
            var urlString = url.toString()
            var modified = false

            var matchedSource: com.kitsugi.animelist.data.manga.MangaSource? = null
            try {
                val loadedSources = com.kitsugi.animelist.data.manga.MangaExtensionLoader.getLoadedSources()
                matchedSource = loadedSources.find { src ->
                    val origUri = android.net.Uri.parse(src.originalBaseUrl)
                    val origHost = origUri.host
                    origHost != null && origHost.equals(host, ignoreCase = true)
                }
            } catch (e: Exception) {
                android.util.Log.e("NetworkHelper", "Error scanning sources for domain redirect", e)
            }

            if (matchedSource != null) {
                val activeDomain = com.kitsugi.animelist.data.manga.SourceConfigStore(context).getActiveDomain(matchedSource)
                if (activeDomain != null && !activeDomain.equals(host, ignoreCase = true)) {
                    urlString = urlString.replace(host, activeDomain)
                    modified = true
                }
            }

            val newRequestBuilder = if (modified) {
                val reqBuilder = request.newBuilder().url(urlString)
                val referer = request.header("Referer")
                if (referer != null && matchedSource != null) {
                    val activeDomain = com.kitsugi.animelist.data.manga.SourceConfigStore(context).getActiveDomain(matchedSource)
                    if (activeDomain != null) {
                        reqBuilder.header("Referer", referer.replace(host, activeDomain))
                    }
                }
                reqBuilder
            } else {
                request.newBuilder()
            }

            val hasUserAgent = request.header("User-Agent")
            if (hasUserAgent.isNullOrBlank() || 
                hasUserAgent.startsWith("okhttp", ignoreCase = true) || 
                hasUserAgent.equals("Mozilla/5.0", ignoreCase = true)
            ) {
                newRequestBuilder.header("User-Agent", defaultUserAgentProvider())
            }
            chain.proceed(newRequestBuilder.build())
        }
        .ignoreAllSSLErrors()
        .addInterceptor(com.lagradost.cloudstream3.network.CloudflareKiller())
        .addInterceptor(com.lagradost.cloudstream3.network.DdosGuardKiller(alwaysBypass = false))
        .build()

    val cloudflareClient: OkHttpClient = client

    fun defaultUserAgentProvider(): String = com.lagradost.cloudstream3.network.CloudflareKiller.UNIFIED_USER_AGENT
}
