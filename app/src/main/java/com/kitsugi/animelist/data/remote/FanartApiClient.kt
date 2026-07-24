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
            val url = java.net.URL("$BASE_URL/tv/$tvdbId?api_key=$apiKey")
            val response = KitsugiApiBase.executeGetRequest(url) ?: return emptyList()
            parseTvImages(JSONObject(response), language)
        } catch (e: Exception) {
            Log.w(TAG, "fetchTvImages failed for tvdbId=$tvdbId: ${e.message}")
            emptyList()
        }
    }

    /**
     * TVDB ID ile yalnızca en iyi logo URL'sini çeker (hero alanı için).
     */
    fun fetchBestLogo(tvdbId: Int, apiKey: String, language: String = "en"): String? {
        if (tvdbId <= 0 || apiKey.isBlank()) return null
        return try {
            val url = java.net.URL("$BASE_URL/tv/$tvdbId?api_key=$apiKey")
            val response = KitsugiApiBase.executeGetRequest(url) ?: return null
            val root = JSONObject(response)
            extractBestUrl(root, listOf("hdtvlogo", "hdclearart"), language)
        } catch (e: Exception) {
            Log.w(TAG, "fetchBestLogo (TV) failed for tvdbId=$tvdbId: ${e.message}")
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
            val url = java.net.URL("$BASE_URL/movies/$tmdbId?api_key=$apiKey")
            val response = KitsugiApiBase.executeGetRequest(url) ?: return emptyList()
            parseMovieImages(JSONObject(response), language)
        } catch (e: Exception) {
            Log.w(TAG, "fetchMovieImages failed for tmdbId=$tmdbId: ${e.message}")
            emptyList()
        }
    }

    /**
     * TMDB ID ile yalnızca en iyi film logo URL'sini çeker (hero alanı için).
     */
    fun fetchBestMovieLogo(tmdbId: Int, apiKey: String, language: String = "en"): String? {
        if (tmdbId <= 0 || apiKey.isBlank()) return null
        return try {
            val url = java.net.URL("$BASE_URL/movies/$tmdbId?api_key=$apiKey")
            val response = KitsugiApiBase.executeGetRequest(url) ?: return null
            val root = JSONObject(response)
            extractBestUrl(root, listOf("hdmovielogo", "hdmovieclearart"), language)
        } catch (e: Exception) {
            Log.w(TAG, "fetchBestMovieLogo failed for tmdbId=$tmdbId: ${e.message}")
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
