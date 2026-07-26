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
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │                    TV / ANİME KATEGORİLERİ                          │
 * ├──────────────────────┬───────────────────────┬──────────────────────┤
 * │ Fanart.tv Kategorisi │ API Alanı              │ GalleryCategory      │
 * ├──────────────────────┼───────────────────────┼──────────────────────┤
 * │ HD ClearLOGO         │ hdtvlogo / clearlogo   │ LOGO                 │
 * │ HD ClearART          │ hdclearart / clearart  │ CLEARART             │
 * │ Background           │ showbackground         │ BACKDROP             │
 * │ 4K Background        │ showbackground (4k)    │ BACKDROP             │
 * │ Poster               │ tvposter / seasonposter│ POSTER               │
 * │ Banner               │ tvbanner / seasonbanner│ BANNER               │
 * │ TV Thumbs            │ tvthumb / seasonthumb  │ THUMBNAIL            │
 * │ CharacterART         │ characterart           │ CHARACTER            │
 * │ Square               │ squareposter           │ SQUARE               │
 * ├──────────────────────┴───────────────────────┴──────────────────────┤
 * │                    FİLM KATEGORİLERİ                                │
 * ├──────────────────────┬───────────────────────┬──────────────────────┤
 * │ HD ClearLOGO         │ hdmovielogo / movielogo│ LOGO                 │
 * │ HD ClearART          │ hdmovieclearart        │ CLEARART             │
 * │ Background           │ moviebackground        │ BACKDROP             │
 * │ Poster               │ movieposter            │ POSTER               │
 * │ Banner               │ moviebanner            │ BANNER               │
 * │ Thumbnail            │ moviethumb             │ THUMBNAIL            │
 * │ Disc Art             │ moviedisc              │ OTHER                │
 * └──────────────────────┴───────────────────────┴──────────────────────┘
 *
 * NOT: Tüm kategoriler LIMIT OLMADAN çekilir — Fanart.tv API'si tek sorguda
 * tüm kategori verilerini döndürür (tek HTTP isteği), dolayısıyla limit
 * kaldırmak ağ maliyetini artırmaz, sadece UI'a daha fazla resim iletir.
 */
object FanartApiClient {

    private const val TAG = "FanartApiClient"
    private const val BASE_URL = "https://webservice.fanart.tv/v3"

    /**
     * Dahili (yedek) Fanart.tv proje API anahtarı.
     * Kullanıcı kendi anahtarını girmezse bu anahtar kullanılır.
     *
     * Öncelik sırası:
     *  1. Kullanıcının ayarlardan girdiği kişisel API anahtarı
     *  2. Bu built-in proje anahtarı (rate-limit paylaşımlı)
     */
    private const val BUILT_IN_API_KEY = "7e8fce70b5cc0dc7c9b3b2b2741a9e92"

    /**
     * Etkin Fanart.tv API anahtarını döner.
     */
    fun getActiveApiKey(userKey: String = ""): String =
        userKey.trim().ifBlank { BUILT_IN_API_KEY }

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
     * Tek HTTP isteği ile TÜM kategoriler alınır — limit yok.
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
            val root = JSONObject(response)
            extractBestUrl(root, listOf("hdtvlogo", "clearlogo", "hdclearart"), language)
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
     * Tek HTTP isteği ile TÜM kategoriler alınır — limit yok.
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
            val root = JSONObject(response)
            extractBestUrl(root, listOf("hdmovielogo", "movielogo", "hdmovieclearart"), language)
        } catch (e: Exception) {
            Log.w(TAG, "fetchBestMovieLogo failed for tmdbId=$tmdbId: ${e.message}", e)
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON parse yardımcıları — LİMİTSİZ, tüm kategoriler
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * TV / Anime: Fanart.tv sayfasındaki TÜM kategorileri çeker.
     *
     * Fanart.tv'de görebileceğin kategoriler (resimdeki gibi):
     *   HD ClearLOGO, Poster, HD ClearART, CharacterART,
     *   TV Thumbs, Background, Banner, 4K Background, Square
     */
    private fun parseTvImages(root: JSONObject, language: String): List<GalleryItem> {
        val items = mutableListOf<GalleryItem>()

        // ── LOGO ──────────────────────────────────────────────────────────────
        appendImages(items, root, "hdtvlogo",      GalleryCategory.LOGO,      language)
        appendImages(items, root, "clearlogo",     GalleryCategory.LOGO,      language)

        // ── CLEARART (HD ClearART) ────────────────────────────────────────────
        appendImages(items, root, "hdclearart",    GalleryCategory.CLEARART,  language)
        appendImages(items, root, "clearart",      GalleryCategory.CLEARART,  language)

        // ── BACKDROP (Background + 4K Background) ─────────────────────────────
        appendImages(items, root, "showbackground", GalleryCategory.BACKDROP,  language)

        // ── POSTER ────────────────────────────────────────────────────────────
        appendImages(items, root, "tvposter",      GalleryCategory.POSTER,    language)
        appendImages(items, root, "seasonposter",  GalleryCategory.POSTER,    language)

        // ── BANNER ────────────────────────────────────────────────────────────
        appendImages(items, root, "tvbanner",      GalleryCategory.BANNER,    language)
        appendImages(items, root, "seasonbanner",  GalleryCategory.BANNER,    language)

        // ── THUMBNAIL (TV Thumbs) ─────────────────────────────────────────────
        appendImages(items, root, "tvthumb",       GalleryCategory.THUMBNAIL, language)
        appendImages(items, root, "seasonthumb",   GalleryCategory.THUMBNAIL, language)

        // ── CHARACTER (CharacterART) ───────────────────────────────────────────
        appendImages(items, root, "characterart",  GalleryCategory.CHARACTER, language)

        // ── SQUARE ────────────────────────────────────────────────────────────
        appendImages(items, root, "squareposter",  GalleryCategory.SQUARE,    language)

        Log.d(TAG, "parseTvImages: ${items.size} total items parsed")
        return items
    }

    /**
     * Film: Fanart.tv'deki tüm film kategorileri.
     */
    private fun parseMovieImages(root: JSONObject, language: String): List<GalleryItem> {
        val items = mutableListOf<GalleryItem>()

        // ── LOGO ──────────────────────────────────────────────────────────────
        appendImages(items, root, "hdmovielogo",     GalleryCategory.LOGO,      language)
        appendImages(items, root, "movielogo",       GalleryCategory.LOGO,      language)

        // ── CLEARART ──────────────────────────────────────────────────────────
        appendImages(items, root, "hdmovieclearart", GalleryCategory.CLEARART,  language)
        appendImages(items, root, "movieart",        GalleryCategory.CLEARART,  language)

        // ── BACKDROP ──────────────────────────────────────────────────────────
        appendImages(items, root, "moviebackground", GalleryCategory.BACKDROP,  language)

        // ── POSTER ────────────────────────────────────────────────────────────
        appendImages(items, root, "movieposter",     GalleryCategory.POSTER,    language)

        // ── BANNER ────────────────────────────────────────────────────────────
        appendImages(items, root, "moviebanner",     GalleryCategory.BANNER,    language)

        // ── THUMBNAIL ─────────────────────────────────────────────────────────
        appendImages(items, root, "moviethumb",      GalleryCategory.THUMBNAIL, language)

        // ── DISC ART ──────────────────────────────────────────────────────────
        appendImages(items, root, "moviedisc",       GalleryCategory.OTHER,     language)

        Log.d(TAG, "parseMovieImages: ${items.size} total items parsed")
        return items
    }

    /**
     * Belirtilen API alanındaki TÜM resimleri listeye ekler (limit yok).
     *
     * Sıralama: tercih edilen dil → dil bağımsız ("00") → İngilizce → diğerleri
     *
     * NOT: Fanart.tv API tek bir HTTP isteğiyle tüm kategori verilerini döndürür.
     * Limit kaldırmak ek ağ isteği gerektirmez — sadece JSON'dan daha fazla
     * öğe okunur ve UI'a iletilir.
     */
    private fun appendImages(
        target: MutableList<GalleryItem>,
        root: JSONObject,
        key: String,
        category: GalleryCategory,
        preferredLanguage: String
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
                lang == preferredLanguage      -> preferred.add(item)
                lang.isBlank() || lang == "00" -> neutral.add(item)
                lang == "en"                   -> english.add(item)
                else                           -> other.add(item)
            }
        }

        // Tüm öğeleri ekle — limit yok
        target.addAll(preferred + neutral + english + other)
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
                    lang == language               -> preferred.add(urlStr)
                    lang.isBlank() || lang == "00" -> neutral.add(urlStr)
                    lang == "en"                   -> english.add(urlStr)
                }
            }

            val best = preferred.firstOrNull() ?: neutral.firstOrNull() ?: english.firstOrNull()
            if (!best.isNullOrBlank()) return best
        }
        return null
    }
}
