package com.lagradost.cloudstream3.network

import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import androidx.annotation.AnyThread
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mvvm.debugWarning
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.nicehttp.cookies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@AnyThread
class CloudflareKiller : Interceptor {
    companion object {
        const val TAG = "CloudflareKiller"
        private val ERROR_CODES = listOf(403, 503)
        private val CLOUDFLARE_SERVERS = listOf("cloudflare-nginx", "cloudflare")
        
        // Per-host locks: each domain gets its own lock so multiple hosts can bypass in parallel.
        // e.g. tranimaci.com and yts.gg won't block each other.
        private val hostLocks: MutableMap<String, ReentrantLock> = ConcurrentHashMap()
        private fun getLockForHost(host: String): ReentrantLock =
            hostLocks.getOrPut(host) { ReentrantLock() }

        // Recursion guard: prevents intercept() from calling itself when the WebView resolver
        // makes HTTP requests through the same OkHttp client, causing stack overflows.
        private val isIntercepting = ThreadLocal.withInitial { false }

        const val UNIFIED_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        // Global thread-safe map for cookies shared across all CloudflareKiller instances
        val savedCookies: MutableMap<String, Map<String, String>> = ConcurrentHashMap()

        // Per-host bypass start time so the 4.5s cool-down is tracked independently per domain.
        private val hostBypassStartTime: MutableMap<String, Long> = ConcurrentHashMap()

        // Per-host failure cooldown map to prevent continuous WebView solve attempts for broken/dead/blocked sites.
        val hostFailureCooldown: MutableMap<String, Long> = ConcurrentHashMap()

        fun parseCookieMap(cookie: String): Map<String, String> {
            return cookie.split(";").associate {
                val split = it.split("=")
                (split.getOrNull(0)?.trim() ?: "") to (split.getOrNull(1)?.trim() ?: "")
            }.filter { it.key.isNotBlank() && it.value.isNotBlank() }
        }
    }

    init {
        // Persist cookies across sessions and align the webview user agent
        WebViewResolver.Companion.webViewUserAgent = UNIFIED_USER_AGENT
    }

    /**
     * Gets the headers with cookies, webview user agent included!
     * */
    fun getCookieHeaders(url: String): Headers {
        val userAgentHeaders = mapOf("user-agent" to UNIFIED_USER_AGENT)
        return getHeaders(userAgentHeaders, savedCookies[URI(url).host] ?: emptyMap())
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host

        // Guard against recursive calls: when CloudflareKiller's WebView bypass makes HTTP
        // requests through the same client (e.g. to fetch cookies), we skip Cloudflare logic
        // to prevent infinite recursion and stack overflow crashes.
        if (isIntercepting.get() == true) {
            return chain.proceed(request)
        }

        // 1. If we already have cookies for this host, attach them to the initial request
        val existingCookies = savedCookies[host]
        val initialRequest = if (existingCookies != null) {
            val userAgentMap = mapOf(
                "user-agent" to UNIFIED_USER_AGENT,
                "sec-ch-ua" to "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\"",
                "sec-ch-ua-mobile" to "?1",
                "sec-ch-ua-platform" to "\"Android\"",
                "sec-fetch-dest" to "document",
                "sec-fetch-mode" to "navigate",
                "sec-fetch-site" to "none",
                "sec-fetch-user" to "?1",
                "upgrade-insecure-requests" to "1"
            )
            val headers = getHeaders(request.headers.toMap() + userAgentMap, existingCookies + request.cookies)
            request.newBuilder().headers(headers).build()
        } else {
            request
        }

        // 2. Proceed with the request
        isIntercepting.set(true)
        val response = try {
            chain.proceed(initialRequest)
        } finally {
            isIntercepting.set(false)
        }

        // 3. Detect if it's a Cloudflare challenge (403/503) or a 200 OK custom WAF challenge page
        var isWafChallenge = false
        if (response.code == 200) {
            val contentType = response.body?.contentType()?.toString()?.lowercase()
            if (contentType != null && (contentType.contains("text/html") || contentType.contains("html"))) {
                try {
                    // Peek the response body to avoid consuming the stream (10 KB max)
                    val peekedBody = response.peekBody(10240L)
                    val bodyString = peekedBody.string()
                    if (bodyString.contains("Security Verification") || 
                        (bodyString.contains("__waf_challenge") && bodyString.contains("challenge"))) {
                        isWafChallenge = true
                        Log.d(TAG, "Custom WAF Security Verification challenge detected for host: $host")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to peek response body for WAF check: ${e.message}")
                }
            }
        }

        val isCfChallenge = response.header("Server") in CLOUDFLARE_SERVERS && response.code in ERROR_CODES

        if (isCfChallenge || isWafChallenge) {
            val cooldownUntil = hostFailureCooldown.getOrDefault(host, 0L)
            if (cooldownUntil > System.currentTimeMillis()) {
                Log.d(TAG, "Skipping WAF/Cloudflare WebView solve for $host due to active failure cooldown")
                return response
            }

            response.close()

            // Since the request failed even with existing cookies, those cookies must be invalid.
            // Remove them from saved cache and WebView CookieManager so a fresh solve is triggered.
            savedCookies.remove(host)
            runBlocking(Dispatchers.Main) {
                try {
                    val cookieManager = CookieManager.getInstance()
                    val urlStr = request.url.toString()
                    cookieManager.setCookie(urlStr, "cf_clearance=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/")
                    cookieManager.setCookie(urlStr, "__waf_session=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to clear expired cookies from CookieManager: ${e.message}")
                }
            }

            val solvedCookies = getLockForHost(host).withLock {
                // Double check if another thread resolved it while we were waiting for the host lock
                val cookiesAfterLock = savedCookies[host]
                if (cookiesAfterLock != null) {
                    cookiesAfterLock
                } else {
                    Log.d(TAG, "Entering serialized WAF/Cloudflare bypass for host: $host")
                    hostBypassStartTime[host] = System.currentTimeMillis()
                    runBlocking(Dispatchers.Main) {
                        try {
                            if (!trySolveWithSavedCookies(request, host)) {
                                Log.d(TAG, "Loading webview to solve WAF/Cloudflare for ${request.url}")
                                WebViewResolver(
                                    Regex(".^"),
                                    userAgent = UNIFIED_USER_AGENT,
                                    useOkhttp = false,
                                    additionalUrls = listOf(Regex("."))
                                ).resolveUsingWebView(request.url.toString()) {
                                    trySolveWithSavedCookies(request, host)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during WAF/Cloudflare WebView resolution", e)
                        }
                    }
                    savedCookies[host]
                }
            }

            if (solvedCookies != null) {
                Log.d(TAG, "Succeeded bypassing WAF/Cloudflare for: ${request.url}")
                val responseWithCookies = proceedWithCookies(chain, request, solvedCookies)
                
                var stillFailed = responseWithCookies.header("Server") in CLOUDFLARE_SERVERS && responseWithCookies.code in ERROR_CODES
                var stillWaf = false
                if (responseWithCookies.code == 200) {
                    val contentType = responseWithCookies.body?.contentType()?.toString()?.lowercase()
                    if (contentType != null && (contentType.contains("text/html") || contentType.contains("html"))) {
                        try {
                            val peekedBody = responseWithCookies.peekBody(10240L)
                            val bodyString = peekedBody.string()
                            if (bodyString.contains("Security Verification") || 
                                (bodyString.contains("__waf_challenge") && bodyString.contains("challenge"))) {
                                stillWaf = true
                            }
                        } catch (_: Exception) {}
                    }
                }

                if (stillFailed || stillWaf) {
                    Log.w(TAG, "Bypass seemed to succeed but request still failed. Putting $host on failure cooldown.")
                    hostFailureCooldown[host] = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutes
                } else {
                    hostFailureCooldown.remove(host)
                }

                // Peek and log the HTML response body for tranimaci search request to inspect Next.js data
                if (request.url.toString().contains("tranimaci", ignoreCase = true)) {
                    try {
                        val peeked = responseWithCookies.peekBody(120 * 1024L) // Peek 120KB
                        val bodyStr = peeked.string()
                        Log.d("TR_HTML", "BODY (first 3KB): " + bodyStr.take(3000))
                        // Extract and log __NEXT_DATA__ JSON for easy inspection
                        val nextDataMatch = Regex("""<script id="__NEXT_DATA__" type="application/json">([\s\S]*?)</script>""")
                            .find(bodyStr)
                        if (nextDataMatch != null) {
                            val nextJson = nextDataMatch.groups[1]?.value ?: ""
                            Log.d("TR_HTML", "__NEXT_DATA__ (first 5KB): " + nextJson.take(5000))
                            // Log pageProps keys for debugging
                            try {
                                val pageProps = org.json.JSONObject(nextJson)
                                    .optJSONObject("props")?.optJSONObject("pageProps")
                                Log.d("TR_HTML", "__NEXT_DATA__ pageProps keys: ${pageProps?.keys()?.asSequence()?.toList()}")
                            } catch (_: Exception) {}
                        } else {
                            Log.w("TR_HTML", "__NEXT_DATA__ script NOT found in response — WAF challenge or different layout")
                        }
                    } catch (e: Exception) {
                        Log.e("TR_HTML", "Failed to peek response body", e)
                    }
                }
                
                return responseWithCookies
            } else {
                Log.w(TAG, "Failed WAF/Cloudflare bypass at: ${request.url}. Putting $host on failure cooldown.")
                hostFailureCooldown[host] = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutes
                debugWarning({ true }) { "Failed WAF/Cloudflare bypass at: ${request.url}" }
            }
        }

        return response
    }

    private fun proceedWithCookies(chain: Interceptor.Chain, request: Request, cookies: Map<String, String>): Response {
        val userAgentMap = mapOf(
            "user-agent" to UNIFIED_USER_AGENT,
            "sec-ch-ua" to "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\"",
            "sec-ch-ua-mobile" to "?1",
            "sec-ch-ua-platform" to "\"Android\"",
            "sec-fetch-dest" to "document",
            "sec-fetch-mode" to "navigate",
            "sec-fetch-site" to "none",
            "sec-fetch-user" to "?1",
            "upgrade-insecure-requests" to "1"
        )
        val headers = getHeaders(request.headers.toMap() + userAgentMap, cookies + request.cookies)
        
        val newRequest = request.newBuilder()
            .headers(headers)
            .build()
        return chain.proceed(newRequest)
    }

    private fun getWebViewCookie(url: String): String? {
        return safe {
            var cookie: String? = null
            if (Looper.myLooper() == Looper.getMainLooper()) {
                cookie = CookieManager.getInstance()?.getCookie(url)
            } else {
                runBlocking(Dispatchers.Main) {
                    cookie = CookieManager.getInstance()?.getCookie(url)
                }
            }
            cookie
        }
    }

    private fun trySolveWithSavedCookies(request: Request, host: String = request.url.host): Boolean {
        val startTime = hostBypassStartTime[host] ?: 0L
        val timeDiff = System.currentTimeMillis() - startTime
        
        val rawCookie = getWebViewCookie(request.url.toString()) ?: return false
        val cookieMap = parseCookieMap(rawCookie)
        if (cookieMap.isEmpty()) return false
        
        val hasCfClearance = cookieMap["cf_clearance"]?.isNotBlank() == true
        val hasDdg = cookieMap["__ddg"]?.isNotBlank() == true || cookieMap["ddg"]?.isNotBlank() == true
        val hasDdgId = cookieMap["__ddgid"]?.isNotBlank() == true || cookieMap["__ddg2"]?.isNotBlank() == true
        
        val isTrAnimeci = host.contains("tranimaci", ignoreCase = true)
        val hasWafSession = if (isTrAnimeci) {
            // tranimaci.com uses a SHA-256 Proof of Work challenge.
            // We MUST wait for at least 5.5 seconds and ensure both WAF session cookies are present
            // before considering the challenge solved, otherwise the WebView is closed prematurely.
            cookieMap.containsKey("vDDoS-YG") && cookieMap.containsKey("__waf_session") && timeDiff >= 5500L
        } else {
            cookieMap.keys.any { k ->
                k.contains("waf", ignoreCase = true) || k.contains("session", ignoreCase = true) ||
                k.contains("challenge", ignoreCase = true) || k.contains("clearance", ignoreCase = true)
            } && cookieMap.values.any { it.isNotBlank() }
        }
        
        val solvedImmediately = hasCfClearance || hasDdg || hasDdgId || hasWafSession
        if (solvedImmediately) {
            savedCookies[host] = cookieMap
            Log.d(TAG, "Cookies accepted immediately for $host after ${timeDiff}ms: keys=${cookieMap.keys}")
            return true
        }
        
        if (timeDiff >= 6000L) { // Increased fallback timeout to 6s to allow slow devices to finish PoW
            savedCookies[host] = cookieMap
            Log.d(TAG, "Cookies accepted via fallback after ${timeDiff}ms for $host: keys=${cookieMap.keys}")
            return true
        }
        
        return false
    }
}

