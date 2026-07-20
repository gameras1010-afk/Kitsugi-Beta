package eu.kanade.tachiyomi.network

import android.content.Context
import android.webkit.CookieManager
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

class WebViewCookieJar(private val context: Context) : CookieJar {

    private val TAG = "WebViewCookieJar"

    /**
     * WebView CookieManager'daki cookie'leri OkHttp CookieJar'a senkronize eder.
     * Cloudflare bypass için kritik: WebView cf_clearance cookie'si OkHttp'e aktarılmalı.
     *
     * @param url Senkronize edilecek domain URL'si (örn. "https://mangagezgini.online")
     */
    fun syncFromWebView(url: String) {
        try {
            val httpUrl = url.toHttpUrlOrNull() ?: return
            val webViewCookieManager = CookieManager.getInstance()
            val rawCookies = webViewCookieManager.getCookie(url) ?: return

            if (rawCookies.isBlank()) return

            val now = System.currentTimeMillis()
            val parsed = mutableListOf<Cookie>()

            rawCookies.split(";").forEach { part ->
                val trimmed = part.trim()
                if (trimmed.isEmpty()) return@forEach
                val eqIdx = trimmed.indexOf('=')
                val name = if (eqIdx >= 0) trimmed.substring(0, eqIdx).trim() else trimmed
                val value = if (eqIdx >= 0) trimmed.substring(eqIdx + 1).trim() else ""

                try {
                    val cookie = Cookie.Builder()
                        .name(name)
                        .value(value)
                        .domain(httpUrl.host)
                        .path("/")
                        .expiresAt(now + 24L * 60 * 60 * 1000) // 24 saat
                        .build()
                    parsed.add(cookie)
                } catch (_: Exception) {}
            }

            if (parsed.isNotEmpty()) {
                saveFromResponse(httpUrl, parsed)
                Log.d(TAG, "WebView->OkHttp sync: ${parsed.size} cookie aktarıldı (${httpUrl.host})")
            }
        } catch (e: Exception) {
            Log.w(TAG, "WebView cookie sync hatası: ${e.message}")
        }
    }

    private val cookieFile = File(context.cacheDir, "manga_cookies.bin")
    private var cookieStore = mutableMapOf<String, MutableList<SerializableCookie>>()

    init {
        loadFromDisk()
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val hostCookies = cookieStore.getOrPut(host) { mutableListOf() }
        cookies.forEach { newCookie ->
            hostCookies.removeAll { it.name == newCookie.name }
            if (newCookie.expiresAt > System.currentTimeMillis()) {
                hostCookies.add(SerializableCookie(newCookie))
            }
        }
        saveToDisk()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val now = System.currentTimeMillis()
        val matchedCookies = mutableListOf<Cookie>()
        
        cookieStore.forEach { (domain, cookies) ->
            if (host == domain || host.endsWith(".$domain")) {
                val iterator = cookies.iterator()
                while (iterator.hasNext()) {
                    val sCookie = iterator.next()
                    val cookie = sCookie.toCookie()
                    if (cookie == null || cookie.expiresAt <= now) {
                        iterator.remove()
                    } else if (cookie.matches(url)) {
                        matchedCookies.add(cookie)
                    }
                }
            }
        }
        return matchedCookies.distinctBy { it.name }
    }

    private fun loadFromDisk() {
        if (!cookieFile.exists()) return
        try {
            ObjectInputStream(cookieFile.inputStream()).use { ois ->
                @Suppress("UNCHECKED_CAST")
                cookieStore = ois.readObject() as MutableMap<String, MutableList<SerializableCookie>>
            }
        } catch (e: Exception) {
            try { cookieFile.delete() } catch (_: Exception) {}
        }
    }

    private fun saveToDisk() {
        try {
            ObjectOutputStream(cookieFile.outputStream()).use { oos ->
                oos.writeObject(cookieStore)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private class SerializableCookie(
        val name: String,
        val value: String,
        val expiresAt: Long,
        val domain: String,
        val path: String,
        val secure: Boolean,
        val httpOnly: Boolean,
        val hostOnly: Boolean
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }

        constructor(cookie: Cookie) : this(
            cookie.name,
            cookie.value,
            cookie.expiresAt,
            cookie.domain,
            cookie.path,
            cookie.secure,
            cookie.httpOnly,
            cookie.hostOnly
        )

        fun toCookie(): Cookie? {
            return try {
                var builder = Cookie.Builder()
                    .name(name)
                    .value(value)
                    .expiresAt(expiresAt)
                    .path(path)
                
                if (hostOnly) {
                    builder = builder.hostOnlyDomain(domain)
                } else {
                    builder = builder.domain(domain)
                }
                if (secure) builder = builder.secure()
                if (httpOnly) builder = builder.httpOnly()
                
                builder.build()
            } catch (e: Exception) {
                null
            }
        }
    }
}
