package com.lagradost.cloudstream3.network

import androidx.annotation.AnyThread
import com.lagradost.cloudstream3.app
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.cookies
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * @param alwaysBypass will pre-emptively fetch ddos guard cookies if true.
 * If false it will only try to get cookies when a request returns 403
 * */
@AnyThread
class DdosGuardKiller(private val alwaysBypass: Boolean) : Interceptor {
    val savedCookiesMap = ConcurrentHashMap<String, Map<String, String>>()
    private val bypassLock = ReentrantLock()

    @Volatile
    private var ddosBypassPath: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host

        // Prevent recursion on DDoS-Guard's own domains
        if (host.contains("ddos-guard", ignoreCase = true)) {
            return chain.proceed(request)
        }

        if (alwaysBypass) {
            val cookies = getOrFetchCookies(request)
            return proceedWithCookies(chain, request, cookies)
        }

        // If we already have cookies for this host, attach them and proceed
        val existingCookies = savedCookiesMap[host]
        if (existingCookies != null) {
            return proceedWithCookies(chain, request, existingCookies)
        }

        val response = chain.proceed(request)
        if (response.code == 403) {
            response.close()
            val cookies = getOrFetchCookies(request)
            return proceedWithCookies(chain, request, cookies)
        }
        return response
    }

    private fun getOrFetchCookies(request: Request): Map<String, String> {
        val host = request.url.host
        val existing = savedCookiesMap[host]
        if (existing != null) return existing

        return bypassLock.withLock {
            val afterLock = savedCookiesMap[host]
            if (afterLock != null) return@withLock afterLock

            runBlocking {
                try {
                    val path = ddosBypassPath ?: Regex("'(.*?)'").find(
                        app.get("https://check.ddos-guard.net/check.js").text
                    )?.groupValues?.get(1)
                    ddosBypassPath = path

                    val bypassUrl = request.url.scheme + "://" + request.url.host + (path ?: "")
                    val newCookies = Requests().get(bypassUrl).cookies
                    savedCookiesMap[host] = newCookies
                    newCookies
                } catch (e: Exception) {
                    emptyMap()
                }
            }
        }
    }

    private fun proceedWithCookies(chain: Interceptor.Chain, request: Request, cookies: Map<String, String>): Response {
        val headers = getHeaders(request.headers.toMap(), cookies + request.cookies)
        val newRequest = request.newBuilder()
            .headers(headers)
            .build()
        return chain.proceed(newRequest)
    }
}
