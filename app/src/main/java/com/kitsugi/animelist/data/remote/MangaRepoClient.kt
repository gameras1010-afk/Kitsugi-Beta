package com.kitsugi.animelist.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Bir Manga Eklenti Reposundan çekilen eklentiyi temsil eder.
 *
 * Mihon/Keiyoushi repo JSON formatıyla uyumludur:
 * https://raw.githubusercontent.com/keiyoushi/extensions/main/index.json
 */
data class MangaExtensionInfo(
    /** Eklentinin görünen adı. Örn: "Manga-TR" */
    val name: String,
    /** Paketin tam adı. Örn: "eu.kanade.tachiyomi.extension.tr.mangatr" */
    val pkg: String,
    /** İndirilebilir APK dosyasının tam URL'si */
    val apkUrl: String,
    /** Eklentinin dil kodu. Örn: "tr", "en" */
    val lang: String,
    /** Semver versiyonu. Örn: "1.4.3" */
    val version: String,
    /** Mihon versionCode (tamsayı) */
    val versionCode: Int = 0,
    /** Repo içindeki simge görseli URL'si (varsa) */
    val iconUrl: String? = null,
    /** Açıklama metni (varsa) */
    val description: String? = null,
    /** Eklentinin NSFW içerik barındırıp barındırmadığı */
    val isNsfw: Boolean = false,
    /** Kaynak sitenin kategorisi (varsa). Örn: "manga", "manhwa" */
    val categories: List<String> = emptyList()
)

/**
 * MangaRepoClient
 *
 * Keiyoushi/Mihon uyumlu repo JSON'larından eklenti listesi çekmek için
 * kullanılan network istemcisi.
 *
 * Desteklenen formatlar:
 *
 * 1) Keiyoushi formatı (index.json):
 *    [
 *      {
 *        "name": "Manga-TR",
 *        "pkg": "eu.kanade.tachiyomi.extension.tr.mangatr",
 *        "apk": "eu.kanade.tachiyomi.extension.tr.mangatr.apk",
 *        "lang": "tr",
 *        "code": 14,
 *        "version": "1.4.14",
 *        "nsfw": 0,
 *        "hasReadme": 0,
 *        "hasChangelog": 0,
 *        "sources": [...]
 *      }
 *    ]
 *
 * 2) Basit düz APK liste formatı (plugins.json - Kitsugi native):
 *    [{ "name": "...", "lang": "tr", "url": "https://.../plugin.apk", "version": "1.0" }]
 */
class MangaRepoClient(context: android.content.Context) {

    companion object {
        private const val TAG = "MangaRepoClient"

        // Keiyoushi resmi repo URL'leri
        const val KEIYOUSHI_INDEX_URL =
            "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json"

        // APK'ları barındıran Keiyoushi CDN base URL'si
        const val KEIYOUSHI_APK_BASE_URL =
            "https://github.com/keiyoushi/extensions/releases/download/extensions-1.x/"

        // Keiyoushi GitHub repo base URL (apk dosyaları için)
        const val KEIYOUSHI_RELEASE_BASE =
            "https://github.com/keiyoushi/extensions/releases/latest/download/"

        // ── Kotatsu-Redo Repo Sabitleri ────────────────────────────────────────

        /**
         * manga_scanner.py tarafından üretilen ve Keiyoushi repo'sunda saklanan
         * birleşik Kotatsu kaynak kataloğu.
         * Futon/kotatsu-parsers-redo güncellendiğinde bu JSON da güncellenir.
         */
        const val KOTATSU_CATALOG_URL =
            "https://raw.githubusercontent.com/keiyoushi/extensions/repo/kotatsu_catalog.json"

        /**
         * kotatsu-parsers-redo reposunun en son commit SHA'sını döndüren GitHub API endpoint'i.
         * Header: Accept: application/vnd.github.sha → Sadece SHA string döner.
         */
        const val KOTATSU_COMMIT_API =
            "https://api.github.com/repos/Kotatsu-Redo/kotatsu-parsers-redo/commits/master"

        /**
         * Kotatsu-Redo'nun GitHub Actions üzerinden üretilen APK release page'i.
         * Referans amaçlı, runtime'da kullanılmaz.
         */
        const val KOTATSU_RELEASES_URL =
            "https://github.com/Kotatsu-Redo/kotatsu-parsers-redo/releases"

        /**
         * raw.githubusercontent.com adresini raw.github.com ile değiştirir.
         * Türkiye'deki ISP engellerini (Turk Telekom vb.) aşmak için kullanılır.
         */
        fun applyMirror(url: String): String {
            if (url.startsWith("https://raw.githubusercontent.com/")) {
                return url.replace("raw.githubusercontent.com", "raw.github.com")
            }
            return url
        }
    }

    private val client = uy.kohesive.injekt.Injekt.get(eu.kanade.tachiyomi.network.NetworkHelper::class.java).client

    /**
     * Verilen [indexUrl] adresinden eklenti listesini çeker.
     *
     * @param indexUrl index.json URL'si (Keiyoushi formatı)
     * @param apkBaseUrl APK dosyalarının indirildiği base URL (Keiyoushi'de CDN)
     * @return Başarıyla ayrıştırılan eklenti listesi, hata durumunda null
     */
    suspend fun fetchExtensions(
        indexUrl: String,
        apkBaseUrl: String = KEIYOUSHI_RELEASE_BASE
    ): List<MangaExtensionInfo>? = withContext(Dispatchers.IO) {
        try {
            val mirroredUrl = applyMirror(indexUrl)
            val mirroredApkBase = applyMirror(apkBaseUrl)
            Log.d(TAG, "Manga repo çekiliyor: $mirroredUrl (original: $indexUrl)")
            val request = Request.Builder().url(mirroredUrl).build()
            val responseBody = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "fetchExtensions başarısız (${response.code}): $mirroredUrl")
                    return@withContext null
                }
                response.body?.string()
            } ?: return@withContext null

            parseKeiyoushiIndex(responseBody, mirroredApkBase)
        } catch (e: Exception) {
            Log.e(TAG, "fetchExtensions hata: ${e.message}", e)
            null
        }
    }

    /**
     * Keiyoushi index.json formatını ayrıştırır.
     */
    private fun parseKeiyoushiIndex(
        json: String,
        apkBaseUrl: String
    ): List<MangaExtensionInfo> {
        val result = mutableListOf<MangaExtensionInfo>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim()
                val pkg = obj.optString("pkg", "").trim()
                val lang = obj.optString("lang", "all").trim()
                val version = obj.optString("version", "1.0").trim()
                val versionCode = obj.optInt("code", 0)
                val isNsfw = obj.optInt("nsfw", 0) == 1

                if (name.isBlank() || pkg.isBlank()) continue

                // APK dosya adını belirle
                val apkFileName = obj.optString("apk", "").trim().ifBlank { "$pkg.apk" }
                val apkUrl = if (apkFileName.startsWith("http")) {
                    apkFileName
                } else {
                    "${apkBaseUrl.trimEnd('/')}/$apkFileName"
                }

                // İkon URL'si
                val iconUrl = obj.optString("iconUrl", "").takeIf { it.isNotBlank() }

                // Kategoriler (sources array'inden çek)
                val categories = mutableListOf<String>()
                val sourcesArr = obj.optJSONArray("sources")
                if (sourcesArr != null) {
                    val seen = mutableSetOf<String>()
                    for (j in 0 until sourcesArr.length()) {
                        val src = sourcesArr.optJSONObject(j) ?: continue
                        val type = src.optString("type", "").trim()
                        if (type.isNotBlank() && seen.add(type)) categories.add(type)
                    }
                }

                result.add(
                    MangaExtensionInfo(
                        name = name,
                        pkg = pkg,
                        apkUrl = apkUrl,
                        lang = lang,
                        version = version,
                        versionCode = versionCode,
                        iconUrl = iconUrl,
                        isNsfw = isNsfw,
                        categories = categories
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseKeiyoushiIndex ayrıştırma hatası: ${e.message}", e)
        }
        Log.i(TAG, "Toplam ${result.size} eklenti ayrıştırıldı")
        return result
    }

    /**
     * Basit Kitsugi native JSON listesini ayrıştırır.
     * Format: [{ "name": "...", "lang": "tr", "url": "https://...", "version": "1.0" }]
     */
    private fun parseNativePluginList(json: String): List<MangaExtensionInfo> {
        val result = mutableListOf<MangaExtensionInfo>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim()
                val url = obj.optString("url", "").trim()
                if (name.isBlank() || url.isBlank()) continue
                result.add(
                    MangaExtensionInfo(
                        name = name,
                        pkg = obj.optString("pkg", name).trim(),
                        apkUrl = url,
                        lang = obj.optString("lang", "all").trim(),
                        version = obj.optString("version", "1.0").trim(),
                        iconUrl = obj.optString("iconUrl", "").takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseNativePluginList ayrıştırma hatası: ${e.message}", e)
        }
        return result
    }

    /**
     * Belirli bir URL'den akıllıca format tespiti yaparak eklenti listesi çeker.
     * Keiyoushi index.json veya native plugins.json farkını otomatik algılar.
     */
    suspend fun fetchExtensionsAutoDetect(repoUrl: String): List<MangaExtensionInfo>? =
        withContext(Dispatchers.IO) {
            try {
                val mirroredUrl = applyMirror(repoUrl)
                Log.d(TAG, "fetchExtensionsAutoDetect çekiliyor: $mirroredUrl (original: $repoUrl)")
                val request = Request.Builder().url(mirroredUrl).build()
                val responseBody = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "fetchExtensionsAutoDetect başarısız (${response.code}): $mirroredUrl")
                        return@withContext null
                    }
                    response.body?.string()
                } ?: return@withContext null

                // İlk elemanı kontrol ederek format belirle
                val trimmed = responseBody.trim()
                if (!trimmed.startsWith("[")) {
                    Log.e(TAG, "Desteklenmeyen format (JSON array bekleniyor): $mirroredUrl")
                    return@withContext null
                }

                val arr = JSONArray(trimmed)
                if (arr.length() == 0) return@withContext emptyList()

                val firstObj = arr.optJSONObject(0)
                val isKeiyoushiFormat = firstObj?.has("pkg") == true

                if (isKeiyoushiFormat) {
                    // Keiyoushi APK base URL'sini repo URL'sinden türet
                    val baseUrl = deriveApkBaseUrl(mirroredUrl)
                    parseKeiyoushiIndex(trimmed, baseUrl)
                } else {
                    parseNativePluginList(trimmed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchExtensionsAutoDetect hata: ${e.message}", e)
                null
            }
        }

    /**
     * Keiyoushi tarzı bir repo URL'sinden APK base URL'sini türetir.
     * Örn: "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json"
     *   -> "https://github.com/keiyoushi/extensions/repo/apk/"
     */
    private fun deriveApkBaseUrl(repoUrl: String): String {
        // Eğer URL index.min.json veya index.json ile bitiyorsa, o kısmı temizleyip sonuna "apk/" ekleyelim
        val baseDir = repoUrl.substringBeforeLast("/")
        
        // Keiyoushi veya diğer GitHub repoları için raw linkler üzerinden apk dizinini hedefle
        if (repoUrl.contains("githubusercontent.com") || repoUrl.contains("github.com")) {
            return "$baseDir/apk/"
        }
        
        // Standart Mihon repo yapısı gereği eklentiler /apk/ altındadır
        return "$baseDir/apk/"
    }
}
