package com.kitsugi.animelist.utils

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import okhttp3.Request

object CloudstreamUrlHelper {

    private const val TAG = "CloudstreamUrlHelper"

    /** Kısa kod regex: sadece harf, rakam, tire, alt çizgi, ünlem — nokta/slash yok */
    private val SHORT_CODE_REGEX = Regex("^[a-zA-Z0-9!_-]+$")

    /**
     * Normalizes repository and plugin URLs.
     * Handles cloudstreamrepo:// and cs.repo/? URI schemes.
     * Both keyiflerolsun and feroxx repos are treated as independent, valid sources.
     */
    fun normalizeUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (url.contains("http-protocol-redirector?r=")) {
            val queryParam = url.substringAfter("http-protocol-redirector?r=")
            url = try {
                URLDecoder.decode(queryParam, "UTF-8")
            } catch (e: Exception) {
                queryParam
            }
        }
        if (url.startsWith("cloudstreamrepo://")) {
            url = "https://" + url.removePrefix("cloudstreamrepo://")
        } else if (url.startsWith("cloudstreamrepo:")) {
            val remaining = url.removePrefix("cloudstreamrepo:")
            url = if (remaining.startsWith("http://") || remaining.startsWith("https://")) {
                remaining
            } else {
                "https://$remaining"
            }
        } else if (url.startsWith("https://cs.repo/?") || url.startsWith("https://cs.repo?")) {
            // cs.repo kısa URL şeması: https://cs.repo/?https://raw.githubusercontent.com/...
            val realUrl = url.substringAfter("?")
            url = if (realUrl.startsWith("http")) realUrl else "https://$realUrl"
        }

        // Dead keyiflerolsun master repo is automatically redirected to the working maarrem repo
        if (url.equals("https://raw.githubusercontent.com/keyiflerolsun/Kekik-cloudstream/master/repo.json", ignoreCase = true)) {
            url = "https://raw.githubusercontent.com/maarrem/cs-Kekik/master/repo.json"
        }

        return url
    }

    /**
     * Verilen girdinin bir Cloudstream kısa kodu olup olmadığını kontrol eder.
     * Kısa kod: sadece harf/rakam/tire/alt çizgi/ünlem içeren ve nokta veya slash içermeyen string.
     * Örnek: "kekikdevam" → true, "https://..." → false
     */
    fun isShortCode(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.isNotBlank() &&
               !trimmed.contains('.') &&
               !trimmed.contains('/') &&
               !trimmed.startsWith("http") &&
               trimmed.matches(SHORT_CODE_REGEX)
    }

    /**
     * cutt.ly URL kısaltma servisi üzerinden kısa kodu gerçek repo URL'sine çözer.
     * Cloudstream topluluğu repo URL'lerini cutt.ly kısa kodlarıyla paylaşır.
     *
     * Örnek: "kekikdevam" → "https://raw.githubusercontent.com/Kekik.../repo.json"
     *
     * @return Gerçek URL veya null (geçersiz / bulunamayan kısa kod)
     */
    suspend fun resolveShortCode(shortCode: String): String? {
        return try {
            Log.d(TAG, "Kısa kod çözülüyor: cutt.ly/$shortCode")
            val request = Request.Builder()
                .url("https://cutt.ly/$shortCode")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val noRedirectClient = com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build()

            noRedirectClient.newCall(request).execute().use { response ->
                val location = response.header("Location")
                if (location == null) {
                    Log.w(TAG, "cutt.ly/$shortCode → Location header yok")
                    return null
                }
                if (location.startsWith("https://cutt.ly/404") || location.removeSuffix("/") == "https://cutt.ly") {
                    Log.w(TAG, "cutt.ly/$shortCode → Geçersiz kısa kod (404)")
                    return null
                }

                Log.d(TAG, "Kısa kod çözüldü: $shortCode → $location")
                location
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kısa kod çözme hatası: $shortCode", e)
            null
        }
    }
}
