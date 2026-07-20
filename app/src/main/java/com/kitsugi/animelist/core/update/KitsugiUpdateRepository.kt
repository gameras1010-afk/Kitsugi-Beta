package com.kitsugi.animelist.core.update

import android.util.Log
import com.kitsugi.animelist.BuildConfig
import com.kitsugi.animelist.core.network.KitsugiHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class KitsugiUpdateRepository(
    private val repoOwner: String = "gameras1010-afk",
    private val repoName: String = "Kitsugi-Beta"
) {

    companion object {
        private const val TAG = "KitsugiUpdateRepo"
        private const val GITHUB_RELEASES_API = "https://api.github.com/repos/%s/%s/releases/latest"
    }

    suspend fun checkForUpdate(): Result<AppRelease?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = String.format(GITHUB_RELEASES_API, repoOwner, repoName)
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "KitsugiApp/${BuildConfig.VERSION_NAME}")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = KitsugiHttpClient.client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Update check failed HTTP code: ${response.code}")
                return@runCatching null
            }

            val bodyString = response.body?.string() ?: return@runCatching null
            val json = JSONObject(bodyString)

            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", tagName).ifEmpty { "Yeni Güncelleme" }
            val releaseNotes = json.optString("body", "Detaylı değişiklik notu bulunmuyor.")
            val publishedAt = formatReleaseDate(json.optString("published_at", ""))

            val fetchedVersionCode = parseVersionCodeFromTag(tagName, json)
            val currentVersionCode = BuildConfig.VERSION_CODE
            val currentVersionName = BuildConfig.VERSION_NAME

            val isNewer = isNewerRelease(
                currentVersionName = currentVersionName,
                fetchedTag = tagName,
                currentVersionCode = currentVersionCode,
                fetchedVersionCode = fetchedVersionCode
            )

            Log.d(TAG, "Current: $currentVersionName (code: $currentVersionCode), Fetched: $tagName (code: $fetchedVersionCode) -> isNewer: $isNewer")

            if (!isNewer) {
                return@runCatching null // No newer update
            }

            // Find APK asset matching the installed flavor (foss vs gms)
            val assets = json.optJSONArray("assets") ?: JSONArray()
            val currentFlavor = BuildConfig.FLAVOR.lowercase()
            var downloadUrl = ""
            var apkSize = 0L
            var fallbackUrl = ""
            var fallbackSize = 0L

            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "").lowercase()
                val url = asset.optString("browser_download_url", "")
                val size = asset.optLong("size", 0L)

                if (name.endsWith(".apk") || asset.optString("content_type") == "application/vnd.android.package-archive") {
                    if (fallbackUrl.isEmpty()) {
                        fallbackUrl = url
                        fallbackSize = size
                    }
                    if (currentFlavor.isNotEmpty() && name.contains(currentFlavor)) {
                        downloadUrl = url
                        apkSize = size
                        break
                    }
                }
            }

            if (downloadUrl.isEmpty()) {
                downloadUrl = fallbackUrl
                apkSize = fallbackSize
            }

            if (downloadUrl.isEmpty()) {
                Log.w(TAG, "New release found ($tagName), but no valid APK asset attached.")
                return@runCatching null
            }

            AppRelease(
                versionCode = fetchedVersionCode,
                versionName = tagName.removePrefix("v"),
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                apkSizeBytes = apkSize,
                publishedAt = publishedAt
            )
        }
    }

    private fun formatReleaseDate(isoDate: String): String {
        if (isoDate.isBlank()) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(isoDate) ?: return ""
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr", "TR"))
            outputFormat.timeZone = TimeZone.getTimeZone("Europe/Istanbul")
            outputFormat.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    private fun parseVersionCodeFromTag(tag: String, json: JSONObject): Int {
        // 1. Try parsing pure digits from tag or version string if formatted like 29762145
        val cleanTag = tag.removePrefix("v").trim()

        // If tag is pure numbers
        cleanTag.toIntOrNull()?.let { return it }

        // If tag is like 2.4.0-beta.29762145, extract the last numeric segment
        val lastSegment = cleanTag.substringAfterLast(".").toIntOrNull()
        if (lastSegment != null && lastSegment > 100) {
            return lastSegment
        }

        // Try extracting numeric digits from tag
        val digitsOnly = cleanTag.filter { it.isDigit() }
        if (digitsOnly.isNotEmpty()) {
            val parsed = digitsOnly.toIntOrNull()
            if (parsed != null) return parsed
        }

        // Fallback: check assets for versionCode in name (e.g., Kitsugi-29762145.apk)
        val assets = json.optJSONArray("assets") ?: JSONArray()
        for (i in 0 until assets.length()) {
            val assetName = assets.optJSONObject(i)?.optString("name", "") ?: continue
            val apkDigits = assetName.filter { it.isDigit() }.toIntOrNull()
            if (apkDigits != null && apkDigits > 100) {
                return apkDigits
            }
        }

        return 0
    }

    private fun isNewerRelease(
        currentVersionName: String,
        fetchedTag: String,
        currentVersionCode: Int,
        fetchedVersionCode: Int
    ): Boolean {
        // 1. If timestamp-based versionCode is fetched and strictly greater
        if (fetchedVersionCode > 1000000 && fetchedVersionCode > currentVersionCode) {
            return true
        }

        // 2. Semantic version comparison (e.g., v2.4.2 vs 2.4.0)
        val cleanCurrent = currentVersionName.removePrefix("v").substringBefore("-").trim()
        val cleanFetched = fetchedTag.removePrefix("v").substringBefore("-").trim()

        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val fetchedParts = cleanFetched.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(currentParts.size, fetchedParts.size)) {
            val curr = currentParts.getOrElse(i) { 0 }
            val fetch = fetchedParts.getOrElse(i) { 0 }
            if (fetch > curr) return true
            if (fetch < curr) return false
        }

        return false
    }
}

