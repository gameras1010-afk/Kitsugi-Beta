package com.kitsugi.animelist.data.remote

import android.util.Log
import org.json.JSONObject

/**
 * Fanart.tv REST API istemcisi.
 *
 * Fanart.tv, TVDB ID'ye göre (TV/Anime) ve TMDB ID'ye göre (Film) yüksek kaliteli
 * logo, backdrop, poster ve daha fazlasını sunar.
 *
 * Proje API Anahtarı: https://fanart.tv/get-an-api-key/
 *
 * TV  : https://webservice.fanart.tv/v3/tv/{tvdb_id}?api_key=KEY
 * Film: https://webservice.fanart.tv/v3/movies/{tmdb_id}?api_key=KEY
 *
 * Döndürülen varlıklar (kategori → API alanı):
 *   TV:    hdtvlogo / hdclearart → LOGO
 *          showbackground         → BACKDROP
 *          tvposter / seasonposter → POSTER
 *          tvthumb                → THUMBNAIL
 *          characterart           → CHARACTER
 *          tvbanner / seasonbanner → BANNER
 *   Film:  hdmovielogo / hdmovieclearart → LOGO
 *          moviebackground               → BACKDROP
 *          movieposter                   → POSTER
 *          moviethumb                    → THUMBNAIL
 *          moviebanner                   → BANNER
 */
object FanartApiClient {

    private const val TAG = "FanartApiClient"
    private const val BASE_URL = "https://webservice.fanart.tv/v3"
    private const val IMG_ORIGINAL = "" // Fanart.tv URL'leri zaten tam yol içerir

    /**
     * Dahili (yedek) Fanart.tv proje API anahtarı.
     * Kullanıcı kendi anahtarını girmezse bu anahtar kullanılır.
     * NOT: Fanart.tv project API keys expire or can become invalid.
     * Always prefer the user's own key via Settings → Integrations.
     *
     * Öncelik sırası:
     *  1. Kullanıcının ayarlardan girdiği kişisel API anahtarı  → api_key olarak gönderilir
     *  2. Bu built-in proje anahtarı (rate-limit paylaşımlı)    → api_key olarak gönderilir
     */
    private const val BUILT_IN_API_KEY = "7e8fce70b5cc0dc7c9b3b2b2741a9e92"

    /**
     * Etkin Fanart.tv API anahtarını döner.
     * Kullanıcı anahtarı varsa onu, yoksa dahili proje anahtarını kullanır.
     */
    fun getActiveApiKey(userKey: String = ""): String =
        userKey.trim().ifBlank { BUILT_IN_API_KEY }

    /**
     * Fanart.tv istek URL'sini oluşturur.
     *
     * - Kullanıcı kendi API anahtarını girmişse → api_key = userKey (en güvenilir yol)
     * - Kullanıcı anahtarı yoksa               → api_key = BUILT_IN_API_KEY
     *
     * [apiKey] parametresi [getActiveApiKey] sonucu olmalıdır (zaten doğru anahtarı içerir).
     */
    private fun buildUrl(endpoint: String, id: Int, apiKey: String): java.net.URL {
        val trimmedKey = apiKey.trim().ifBlank { BUILT_IN_API_KEY }
        val urlString = "$BASE_URL/$endpoint/$id?api_key=$trimmedKey"
        Log.d(TAG, "Fanart request URL: $urlString")
        return java.net.URL(urlString)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TV / Anime: TVDB ID bazlı
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * TVDB ID ile TV/Anime görsellerini çeker.
     * [language] → "tr", "en" vb. — Fanart.tv mevcut dilde görselleri önce sıralamaya çalışır.
     */
    fun fetchTvImages(tvdbId: Int, apiKey: String, language: String = "en"): List<GalleryItem> {
        if (tvdbId <= 0 || apiKey.isBlank()) return emptyList()
        return try {
            val url = buildUrl("tv", tvdbId, apiKey)
            val response = KitsugiApiBase.executeGetRequest(url)
            if (response == null) {
                Log.w(TAG, "fetchTvImages: HTTP request failed or returned empty for tvdbId=$tvdbId")
                return emptyList()
            }
            Log.d(TAG, "fetchTvImages: Response length=${response.length} for tvdbId=$tvdbId")
            parseTvImages(JSONObject(response), language)
        } catch (e: Exception) {
            Log.w(TAG, "fetchTvImages failed for tvdbId=$tvdbId: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * TVDB ID ile yalnızca en iyi logo URL'sini çeker (hero alanı için).
     */
    fun fetchBestLogo(tvdbId: Int, apiKey: String, language: String = "en"): String? {
        if (tvdbId <= 0 || apiKey.isBlank()) return null
        return try {
            val url = buildUrl("tv", tvdbId, apiKey)
            val response = KitsugiApiBase.executeGetRequest(url)
            if (response == null) {
                Log.w(TAG, "fetchBestLogo: HTTP request failed or returned empty for tvdbId=$tvdbId")
                return null
            }
            Log.d(TAG, "fetchBestLogo: Response length=${response.length} for tvdbId=$tvdbId")
            val root = JSONObject(response)
            extractBestUrl(root, listOf("hdtvlogo", "hdclearart"), language)
        } catch (e: Exception) {
            Log.w(TAG, "fetchBestLogo (TV) failed for tvdbId=$tvdbId: ${e.message}", e)
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Film: TMDB ID bazlı
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * TMDB ID ile film görsellerini çeker.
     */
    fun fetchMovieImages(tmdbId: Int, apiKey: String, language: String = "en"): List<GalleryItem> {
        if (tmdbId <= 0 || apiKey.isBlank()) return emptyList()
        return try {
            val url = buildUrl("movies", tmdbId, apiKey)
            val response = KitsugiApiBase.executeGetRequest(url)
            if (response == null) {
                Log.w(TAG, "fetchMovieImages: HTTP request failed or returned empty for tmdbId=$tmdbId")
                return emptyList()
            }
            Log.d(TAG, "fetchMovieImages: Response length=${response.length} for tmdbId=$tmdbId")
            parseMovieImages(JSONObject(response), language)
        } catch (e: Exception) {
            Log.w(TAG, "fetchMovieImages failed for tmdbId=$tmdbId: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * TMDB ID ile yalnızca en iyi film logo URL'sini çeker (hero alanı için).
     */
    fun fetchBestMovieLogo(tmdbId: Int, apiKey: String, language: String = "en"): String? {
        if (tmdbId <= 0 || apiKey.isBlank()) return null
        return try {
            val url = buildUrl("movies", tmdbId, apiKey)
            val response = KitsugiApiBase.executeGetRequest(url)
            if (response == null) {
                Log.w(TAG, "fetchBestMovieLogo: HTTP request failed or returned empty for tmdbId=$tmdbId")
                return null
            }
            Log.d(TAG, "fetchBestMovieLogo: Response length=${response.length} for tmdbId=$tmdbId")
            val root = JSONObject(response)
            extractBestUrl(root, listOf("hdmovielogo", "hdmovieclearart"), language)
        } catch (e: Exception) {
            Log.w(TAG, "fetchBestMovieLogo failed for tmdbId=$tmdbId: ${e.message}", e)
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON parse yardımcıları
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseTvImages(root: JSONObject, language: String): List<GalleryItem> {
        val items = mutableListOf<GalleryItem>()

        // Logolar
        appendImages(items, root, "hdtvlogo",      GalleryCategory.LOGO,     language, limit = 5)
        appendImages(items, root, "hdclearart",    GalleryCategory.LOGO,     language, limit = 5)
        // Arka planlar
        appendImages(items, root, "showbackground", GalleryCategory.BACKDROP, language, limit = 12)
        // Posterler
        appendImages(items, root, "tvposter",      GalleryCategory.POSTER,   language, limit = 10)
        appendImages(items, root, "seasonposter",  GalleryCategory.POSTER,   language, limit = 6)
        // Afiş
        appendImages(items, root, "tvbanner",      GalleryCategory.BANNER,   language, limit = 5)
        appendImages(items, root, "seasonbanner",  GalleryCategory.BANNER,   language, limit = 4)
        // Küçük resim
        appendImages(items, root, "tvthumb",       GalleryCategory.THUMBNAIL, language, limit = 6)
        // Karakter
        appendImages(items, root, "characterart",  GalleryCategory.CHARACTER, language, limit = 8)

        return items
    }

    private fun parseMovieImages(root: JSONObject, language: String): List<GalleryItem> {
        val items = mutableListOf<GalleryItem>()

        appendImages(items, root, "hdmovielogo",    GalleryCategory.LOGO,     language, limit = 5)
        appendImages(items, root, "hdmovieclearart", GalleryCategory.LOGO,    language, limit = 5)
        appendImages(items, root, "moviebackground", GalleryCategory.BACKDROP, language, limit = 12)
        appendImages(items, root, "movieposter",    GalleryCategory.POSTER,   language, limit = 10)
        appendImages(items, root, "moviethumb",     GalleryCategory.THUMBNAIL, language, limit = 6)
        appendImages(items, root, "moviebanner",    GalleryCategory.BANNER,   language, limit = 5)

        return items
    }

    /**
     * Belirtilen API alanındaki resimleri [limit] adet kadar listeye ekler.
     * Önce tercih edilen dil, sonra dil bağımsız, sonra İngilizce görüntüler sıralanır.
     */
    private fun appendImages(
        target: MutableList<GalleryItem>,
        root: JSONObject,
        key: String,
        category: GalleryCategory,
        preferredLanguage: String,
        limit: Int
    ) {
        val array = root.optJSONArray(key) ?: return
        val preferred = mutableListOf<GalleryItem>()
        val neutral   = mutableListOf<GalleryItem>()
        val english   = mutableListOf<GalleryItem>()
        val other     = mutableListOf<GalleryItem>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val urlStr = obj.optString("url", "").trim()
            if (urlStr.isBlank()) continue
            val lang = obj.optString("lang", "").trim()
            val item = GalleryItem(url = urlStr, source = "Fanart.tv", category = category)
            when {
                lang == preferredLanguage -> preferred.add(item)
                lang.isBlank() || lang == "00" -> neutral.add(item)
                lang == "en" -> english.add(item)
                else -> other.add(item)
            }
        }

        val sorted = preferred + neutral + english + other
        target.addAll(sorted.take(limit))
    }

    /**
     * Belirli alanlardan en iyi (tercih edilen dil → dil bağımsız → İngilizce) URL'yi döner.
     */
    private fun extractBestUrl(root: JSONObject, keys: List<String>, language: String): String? {
        for (key in keys) {
            val array = root.optJSONArray(key) ?: continue
            val preferred = mutableListOf<String>()
            val neutral   = mutableListOf<String>()
            val english   = mutableListOf<String>()

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val urlStr = obj.optString("url", "").trim()
                if (urlStr.isBlank()) continue
                val lang = obj.optString("lang", "").trim()
                when {
                    lang == language -> preferred.add(urlStr)
                    lang.isBlank() || lang == "00" -> neutral.add(urlStr)
                    lang == "en" -> english.add(urlStr)
                }
            }

            val best = preferred.firstOrNull() ?: neutral.firstOrNull() ?: english.firstOrNull()
            if (!best.isNullOrBlank()) return best
        }
        return null
    }
}
