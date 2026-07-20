package com.kitsugi.animelist.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * T1.11: WebViewResolver
 *
 * Cloudflare Turnstile / IUAM challenge'ı WebView ile çözer ve çerezleri döndürür.
 * CloudStream WebViewResolver.kt referansından adapte edildi.
 *
 * Kullanım:
 *   val cookies = WebViewResolver.resolve(context, "https://site.com/manga/page")
 *   // Ardından bu cookie'leri OkHttp isteğine ekle
 *
 * NOT: WebView UI thread gerektirdiğinden, bu fonksiyon ana thread'de çağrılmalıdır.
 * Coroutine içinden kullanırken Dispatchers.Main kullanın.
 */
object WebViewResolver {

    private const val TAG = "WebViewResolver"
    private const val DEFAULT_TIMEOUT_MS = 15_000L
    private const val CF_SUCCESS_INDICATOR = "cf-mitigated"
    private const val CF_CHALLENGE_INDICATOR = "cf_clearance"

    /**
     * Verilen URL'i WebView ile açar ve Cloudflare challenge'ı çözer.
     *
     * @param context Activity veya Application context
     * @param url Challenge içeren URL
     * @param timeoutMs Maksimum bekleme süresi (ms)
     * @return Cookie map (boş olabilir — challenge başarısız olunca)
     */
    suspend fun resolve(
        context: Context,
        url: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): Map<String, String> = withContext(Dispatchers.Main) {
        val result = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val webView = createWebView(context)
                var resumed = false

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (url == null) return

                        // Challenge çözüldü mü kontrol et
                        val cookieString = CookieManager.getInstance().getCookie(url) ?: ""
                        if (cookieString.contains(CF_CHALLENGE_INDICATOR) ||
                            !url.contains("challenge", ignoreCase = true)) {
                            if (!resumed) {
                                resumed = true
                                val cookies = parseCookieString(cookieString)
                                Log.d(TAG, "Challenge resolved for $url → ${cookies.size} cookies")
                                webView.destroy()
                                continuation.resume(cookies)
                            }
                        } else {
                            Log.d(TAG, "Challenge page still showing: $url")
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        // Cloudflare challenge sayfaları arasındaki yönlendirmelere izin ver
                        return false
                    }
                }

                continuation.invokeOnCancellation {
                    webView.stopLoading()
                    webView.destroy()
                    Log.d(TAG, "WebView cancelled for $url")
                }

                webView.loadUrl(url)
            }
        }

        result ?: run {
            Log.w(TAG, "WebView resolver timed out for $url")
            // Timeout oldu: mevcut cookie'leri dön (kısmi başarı)
            val cookieString = CookieManager.getInstance().getCookie(url) ?: ""
            parseCookieString(cookieString)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(context: Context): WebView {
        return WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                userAgentString = KitsugiUserAgent.DEFAULT
                // Cloudflare için JS gerekli
                loadWithOverviewMode = true
                useWideViewPort = true
            }
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        }
    }

    /**
     * `name1=value1; name2=value2; ...` formatındaki cookie string'ini Map'e çevirir.
     */
    private fun parseCookieString(cookieString: String): Map<String, String> {
        return cookieString
            .split(";")
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx == -1) null
                else {
                    val key = part.substring(0, idx).trim()
                    val value = part.substring(idx + 1).trim()
                    if (key.isNotBlank()) key to value else null
                }
            }
            .toMap()
    }

    /**
     * Belirli bir URL için mevcut çerezleri çeker (WebView açmadan).
     * Önceki resolve() çağrısından kalan çerezler için kullanılır.
     */
    fun getCachedCookies(url: String): Map<String, String> {
        val cookieString = CookieManager.getInstance().getCookie(url) ?: return emptyMap()
        return parseCookieString(cookieString)
    }

    /** Tüm çerezleri temizle (hesap çıkışı vb.) */
    fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }
}

/** Kitsugi kullanıcı ajanı sabitleri */
object KitsugiUserAgent {
    const val DEFAULT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
}
