package com.kitsugi.animelist.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Cloudflare 403/503 challenge'larını WebView üzerinden otomatik çözen OkHttp interceptor'ı.
 *
 * Akış:
 *  request → chain.proceed()
 *    → 403/503 + "Server: cloudflare" header?
 *      EVET: WebViewResolver(url) → 15s içinde cf_clearance cookie bekle
 *            → cookie bulundu: request'e Cookie header ekle → retry
 *      HAYIR: normal yanıt döndür
 *
 * Kullanım: OkHttpClient.Builder().addInterceptor(CloudflareInterceptor(context))
 * NOT: WebView main thread'de çalışmalıdır — Handler(Looper.getMainLooper()) ile tetiklenir.
 */
class CloudflareInterceptor(private val context: Context) : Interceptor {

    companion object {
        private const val TAG = "CloudflareInterceptor"
        private const val TIMEOUT_MS = 15_000L
        private const val CF_CLEARANCE_COOKIE = "cf_clearance"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Sadece 403/503 + Cloudflare server header'ı varsa müdahale et
        if (!isCloudflareChallenge(response)) {
            return response
        }

        Log.d(TAG, "Cloudflare challenge tespit edildi: ${request.url}")
        response.close()

        // WebView ile cookie'yi çöz
        val cookie = resolveWithWebView(request.url.toString())
        if (cookie == null) {
            Log.w(TAG, "Cloudflare cookie alınamadı (timeout). Orijinal isteği tekrarlıyoruz.")
            // Cookie olmadan tekrar dene (en azından bazı kaynaklarda çalışır)
            return chain.proceed(request)
        }

        val existingCookie = request.header("Cookie")
        val mergedCookie = if (!existingCookie.isNullOrBlank()) {
            if (existingCookie.contains(CF_CLEARANCE_COOKIE)) existingCookie else "$existingCookie; $CF_CLEARANCE_COOKIE=$cookie"
        } else {
            "$CF_CLEARANCE_COOKIE=$cookie"
        }
        val retryRequest = request.newBuilder()
            .header("Cookie", mergedCookie)
            .header("User-Agent", NuvioOkHttpProvider.USER_AGENT)
            .build()
        return chain.proceed(retryRequest)
    }

    private fun isCloudflareChallenge(response: Response): Boolean {
        if (response.code != 403 && response.code != 503) return false
        val server = response.header("Server") ?: response.header("server") ?: ""
        val cfRay = response.header("CF-RAY") ?: response.header("cf-ray") ?: ""
        return server.contains("cloudflare", ignoreCase = true) || cfRay.isNotEmpty()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(url: String): String? {
        val latch = CountDownLatch(1)
        var cfClearance: String? = null
        val handler = Handler(Looper.getMainLooper())

        handler.post {
            try {
                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = NuvioOkHttpProvider.USER_AGENT

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            // Cloudflare redirect'lerini takip et
                            view.loadUrl(request.url.toString())
                            return true
                        }

                        override fun onPageFinished(view: WebView, pageUrl: String) {
                            // Her sayfa yüklenişinde cf_clearance cookie'yi kontrol et
                            val httpUrl = pageUrl.toHttpUrlOrNull() ?: return
                            val cookieString = CookieManager.getInstance()
                                .getCookie(httpUrl.toString()) ?: return

                            val headers = okhttp3.Headers.Builder().add("Set-Cookie", cookieString).build()
                            val parsed = Cookie.parseAll(httpUrl, headers)
                            val found = parsed.find { it.name == CF_CLEARANCE_COOKIE }?.value
                                ?: cookieString.split(";")
                                    .map { it.trim() }
                                    .firstOrNull { it.startsWith("$CF_CLEARANCE_COOKIE=") }
                                    ?.removePrefix("$CF_CLEARANCE_COOKIE=")

                            if (found != null) {
                                cfClearance = found
                                Log.d(TAG, "cf_clearance cookie bulundu!")
                                view.stopLoading()
                                latch.countDown()
                            }
                        }
                    }
                }
                webView.loadUrl(url)

                // Timeout: latch 15s içinde indirilmezse iptal
                handler.postDelayed({
                    if (latch.count > 0) {
                        Log.w(TAG, "WebView timeout: cf_clearance 15s içinde alınamadı — $url")
                        webView.stopLoading()
                        webView.destroy()
                        latch.countDown()
                    }
                }, TIMEOUT_MS)

            } catch (e: Exception) {
                Log.e(TAG, "WebView başlatma hatası: ${e.message}", e)
                latch.countDown()
            }
        }

        latch.await(TIMEOUT_MS + 1000L, TimeUnit.MILLISECONDS)
        return cfClearance
    }
}
