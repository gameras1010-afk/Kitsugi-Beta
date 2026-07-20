package com.kitsugi.animelist.data.manga.engines

import android.util.Log
import com.kitsugi.animelist.data.manga.MangaChapter
import com.kitsugi.animelist.data.manga.MangaDetails
import com.kitsugi.animelist.data.manga.MangaPage
import com.kitsugi.animelist.data.manga.MangaSourceResult
import com.kitsugi.animelist.data.manga.MangaStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * SvelteMangaEngine
 *
 * SvelteKit tabanlı TR manga siteleri için yerel fallback scraper.
 *
 * Desteklenen siteler (hepsi efsaneler2.can.re CDN'ini kullanıyor):
 *  - uzaymanga.com       → cdn-u.efsaneler2.can.re
 *  - afroditscans.com    → kendi CDN
 *  - limonmanga.com      → cdn-l.efsaneler2.can.re
 *  - eldermanga.com      → cdn-el.efsaneler2.can.re
 *  - eskimangalar.com    → cdn-es.efsaneler2.can.re
 *  - tenshimanga.com     → cdn-t.efsaneler2.can.re
 *
 * SvelteKit'in veri formatı:
 *  GET /some-route/__data.json
 *  Response: { "type": "data", "nodes": [...] }
 *  nodes içindeki "data" array'i sayısal index-referans sistemi kullanır.
 */
class SvelteMangaEngine(
    private val baseUrl: String,
    private val cdnUrl: String,
    private val client: OkHttpClient
) {
    private val TAG = "SvelteEngine[$baseUrl]"

    // ── Arama ─────────────────────────────────────────────────────────────────

    suspend fun searchManga(query: String, page: Int): MangaSourceResult = withContext(Dispatchers.IO) {
        val url = buildString {
            append("$baseUrl/manga/__data.json")
            append("?search=").append(query.encodeUrl())
            append("&page=").append(page)
        }
        Log.d(TAG, "Arama: $url")
        val json = fetchJson(url)
        parseMangaListResponse(json)
    }

    // ── Popüler ───────────────────────────────────────────────────────────────

    suspend fun fetchPopularManga(page: Int): MangaSourceResult = withContext(Dispatchers.IO) {
        val url = "$baseUrl/manga/__data.json?page=$page"
        Log.d(TAG, "Popüler: $url")
        val json = fetchJson(url)
        parseMangaListResponse(json)
    }

    // ── Manga Detay ───────────────────────────────────────────────────────────

    suspend fun fetchMangaDetails(mangaUrl: String): MangaDetails = withContext(Dispatchers.IO) {
        val slug = mangaUrl.trimEnd('/').substringAfterLast('/')
        val url = "$baseUrl/manga/$slug/__data.json"
        Log.d(TAG, "Detay: $url")
        val json = fetchJson(url)
        parseMangaDetailsResponse(json, mangaUrl)
    }

    // ── Bölüm Listesi ─────────────────────────────────────────────────────────

    suspend fun fetchChapterList(mangaUrl: String): List<MangaChapter> = withContext(Dispatchers.IO) {
        val slug = mangaUrl.trimEnd('/').substringAfterLast('/')
        val url = "$baseUrl/manga/$slug/__data.json"
        Log.d(TAG, "Bölümler: $url")
        val json = fetchJson(url)
        parseChapterListResponse(json, mangaUrl)
    }

    // ── Sayfa Listesi ─────────────────────────────────────────────────────────

    suspend fun fetchPageList(chapterUrl: String): List<MangaPage> = withContext(Dispatchers.IO) {
        val url = "$chapterUrl/__data.json"
        Log.d(TAG, "Sayfalar: $url")
        val json = fetchJson(url)
        parsePageListResponse(json)
    }

    // ── JSON Fetch ────────────────────────────────────────────────────────────

    private fun fetchJson(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Referer", baseUrl)
            .header("User-Agent", NUVIO_UA)
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code} for $url")
        }
        return response.body?.string() ?: throw Exception("Boş response: $url")
    }

    // ── SvelteKit Node Çözümleyici ────────────────────────────────────────────

    /**
     * SvelteKit'in node referans sistemini çözer.
     *
     * SvelteKit __data.json içindeki "data" array'inde bazı elemanlar
     * integer olabilir — bu integer, aynı array içindeki başka bir
     * indekse referans verir (deduplicated string pool).
     *
     * Örnek:
     *   data = [null, {"title": 2, "slug": 3}, "One Piece", "one-piece"]
     *   Burada title=2 demek data[2] = "One Piece" demektir.
     */
    private fun resolveNodes(data: JSONArray): ResolvedData {
        // Önce tüm string değerlerini cache'e al
        val resolved = mutableMapOf<Int, Any?>()
        for (i in 0 until data.length()) {
            val v = data.opt(i)
            if (v is String || v is Boolean || v is Double || v is Int || v is Long) {
                resolved[i] = v
            }
        }

        // Object ve array'leri çöz
        fun resolveValue(v: Any?): Any? {
            return when (v) {
                is Int -> resolved[v] ?: data.opt(v)
                is JSONObject -> resolveObject(v, data)
                is JSONArray -> resolveArray(v, data)
                else -> v
            }
        }

        // Ana data içindeki ilk object'i bul (manga listesi veya detay)
        for (i in 0 until data.length()) {
            val item = data.opt(i)
            if (item is JSONObject) {
                return ResolvedData(resolveObject(item, data), data)
            }
        }
        return ResolvedData(null, data)
    }

    private fun resolveObject(obj: JSONObject, pool: JSONArray): JSONObject {
        val result = JSONObject()
        obj.keys().forEach { key ->
            val v = obj.opt(key)
            result.put(key, when (v) {
                is Int -> pool.opt(v)     // index reference
                is JSONObject -> resolveObject(v, pool)
                is JSONArray  -> resolveArray(v, pool)
                else -> v
            })
        }
        return result
    }

    private fun resolveArray(arr: JSONArray, pool: JSONArray): JSONArray {
        val result = JSONArray()
        for (i in 0 until arr.length()) {
            val v = arr.opt(i)
            result.put(when (v) {
                is Int -> pool.opt(v)
                is JSONObject -> resolveObject(v, pool)
                is JSONArray  -> resolveArray(v, pool)
                else -> v
            })
        }
        return result
    }

    // ── Parse Fonksiyonları ───────────────────────────────────────────────────

    private fun parseMangaListResponse(json: String): MangaSourceResult {
        return try {
            val root = JSONObject(json)
            val nodes = root.optJSONArray("nodes") ?: return MangaSourceResult(emptyList(), false)

            // Veri node'unu bul (type="data" olan)
            val dataNode = findDataNode(nodes) ?: return MangaSourceResult(emptyList(), false)
            val data = dataNode.optJSONArray("data") ?: return MangaSourceResult(emptyList(), false)

            val resolved = resolveNodes(data)
            val mainObj = resolved.mainObject ?: return MangaSourceResult(emptyList(), false)

            // mangas veya manga_list key'ini dene
            val mangaArray = mainObj.optJSONArray("mangas")
                ?: mainObj.optJSONArray("manga_list")
                ?: mainObj.optJSONArray("data")
                ?: return MangaSourceResult(emptyList(), false)

            val mangas = (0 until mangaArray.length()).mapNotNull { i ->
                parseMangaItem(mangaArray.optJSONObject(i), data)
            }

            val hasNext = mainObj.optJSONObject("meta")?.let {
                it.optInt("current_page") < it.optInt("last_page")
            } ?: (mangas.size >= 20)

            Log.d(TAG, "Manga listesi: ${mangas.size} sonuç, hasNext=$hasNext")
            MangaSourceResult(mangas, hasNext)
        } catch (e: JSONException) {
            Log.e(TAG, "Manga listesi parse hatası: ${e.message}")
            MangaSourceResult(emptyList(), false)
        }
    }

    private fun parseMangaItem(obj: JSONObject?, pool: JSONArray): MangaDetails? {
        if (obj == null) return null
        return try {
            val slug  = obj.resolveStr(pool, "slug") ?: return null
            val title = obj.resolveStr(pool, "title") ?: obj.resolveStr(pool, "name") ?: slug
            val cover = obj.resolveStr(pool, "cover") ?: obj.resolveStr(pool, "thumbnail")
                ?: obj.resolveStr(pool, "cover_url") ?: obj.resolveStr(pool, "image")
            val coverUrl = cover?.let { resolveImageUrl(it) }

            MangaDetails(
                url          = "$baseUrl/manga/$slug",
                title        = title,
                thumbnailUrl = coverUrl,
                source       = baseUrl.removePrefix("https://").removePrefix("http://").split(".")[0]
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMangaDetailsResponse(json: String, mangaUrl: String): MangaDetails {
        return try {
            val root  = JSONObject(json)
            val nodes = root.optJSONArray("nodes") ?: return MangaDetails(url = mangaUrl, title = mangaUrl)
            val dataNode = findDataNode(nodes) ?: return MangaDetails(url = mangaUrl, title = mangaUrl)
            val data  = dataNode.optJSONArray("data") ?: return MangaDetails(url = mangaUrl, title = mangaUrl)
            val resolved = resolveNodes(data)
            val obj  = resolved.mainObject ?: return MangaDetails(url = mangaUrl, title = mangaUrl)

            val manga = obj.optJSONObject("manga") ?: obj

            val title   = manga.resolveStr(data, "title") ?: manga.resolveStr(data, "name") ?: mangaUrl
            val desc    = manga.resolveStr(data, "description") ?: manga.resolveStr(data, "summary")
            val author  = manga.resolveStr(data, "author")
            val cover   = manga.resolveStr(data, "cover") ?: manga.resolveStr(data, "thumbnail")
            val coverUrl = cover?.let { resolveImageUrl(it) }

            val statusStr = manga.resolveStr(data, "status")?.lowercase() ?: ""
            val status = when {
                "devam" in statusStr || "ongoing" in statusStr -> MangaStatus.Ongoing
                "tamamland" in statusStr || "completed" in statusStr -> MangaStatus.Completed
                "bırakıld" in statusStr || "dropped" in statusStr || "cancelled" in statusStr -> MangaStatus.Cancelled
                "ara" in statusStr || "hiatus" in statusStr -> MangaStatus.OnHiatus
                else -> MangaStatus.Unknown
            }

            // Genres
            val genreArray = manga.optJSONArray("genres") ?: manga.optJSONArray("tags") ?: manga.optJSONArray("categories")
            val genres = mutableListOf<String>()
            if (genreArray != null) {
                for (i in 0 until genreArray.length()) {
                    val g = genreArray.opt(i)
                    when (g) {
                        is JSONObject -> g.resolveStr(data, "name")?.let { genres.add(it) }
                        is String -> genres.add(g)
                        else -> {}
                    }
                }
            }

            MangaDetails(
                url          = mangaUrl,
                title        = title,
                description  = desc,
                author       = author,
                thumbnailUrl = coverUrl,
                status       = status,
                genre        = genres,
                source       = baseUrl.removePrefix("https://").split(".")[0]
            )
        } catch (e: Exception) {
            Log.e(TAG, "Detay parse hatası: ${e.message}")
            MangaDetails(url = mangaUrl, title = mangaUrl)
        }
    }

    private fun parseChapterListResponse(json: String, mangaUrl: String): List<MangaChapter> {
        return try {
            val root  = JSONObject(json)
            val nodes = root.optJSONArray("nodes") ?: return emptyList()
            val dataNode = findDataNode(nodes) ?: return emptyList()
            val data  = dataNode.optJSONArray("data") ?: return emptyList()
            val resolved = resolveNodes(data)
            val obj  = resolved.mainObject ?: return emptyList()

            val manga      = obj.optJSONObject("manga") ?: obj
            val chapterArr = manga.optJSONArray("chapters")
                ?: obj.optJSONArray("chapters")
                ?: return emptyList()

            val chapters = (0 until chapterArr.length()).mapNotNull { i ->
                val ch = chapterArr.optJSONObject(i) ?: return@mapNotNull null
                val slug   = ch.resolveStr(data, "slug") ?: ch.resolveStr(data, "chapter_slug") ?: return@mapNotNull null
                val name   = ch.resolveStr(data, "title") ?: ch.resolveStr(data, "name") ?: "Bölüm $i"
                val mangaSlug = mangaUrl.trimEnd('/').substringAfterLast('/')
                val chUrl  = "$baseUrl/manga/$mangaSlug/$slug"
                val numStr = ch.resolveStr(data, "chapter_number") ?: ch.resolveStr(data, "number") ?: name
                val num    = numStr.toFloatOrNull() ?: extractChapterNumber(name)
                val date   = ch.resolveStr(data, "created_at")?.parseTimestamp() ?: 0L

                MangaChapter(
                    url           = chUrl,
                    name          = name,
                    chapterNumber = num,
                    uploadDate    = date,
                    mangaUrl      = mangaUrl
                )
            }

            Log.d(TAG, "Bölüm listesi: ${chapters.size} bölüm")
            chapters.sortedByDescending { it.chapterNumber }
        } catch (e: Exception) {
            Log.e(TAG, "Bölüm parse hatası: ${e.message}")
            emptyList()
        }
    }

    private fun parsePageListResponse(json: String): List<MangaPage> {
        return try {
            val root  = JSONObject(json)
            val nodes = root.optJSONArray("nodes") ?: return emptyList()
            val dataNode = findDataNode(nodes) ?: return emptyList()
            val data  = dataNode.optJSONArray("data") ?: return emptyList()
            val resolved = resolveNodes(data)
            val obj  = resolved.mainObject ?: return emptyList()

            // pages, images veya reader_pages key'ini dene
            val pageArr = obj.optJSONArray("pages")
                ?: obj.optJSONArray("images")
                ?: obj.optJSONArray("reader_pages")
                ?: return emptyList()

            val pages = (0 until pageArr.length()).mapNotNull { i ->
                val page = pageArr.opt(i)
                val imgUrl = when (page) {
                    is String     -> resolveImageUrl(page)
                    is JSONObject -> page.resolveStr(data, "url")
                        ?: page.resolveStr(data, "image_url")
                        ?: page.resolveStr(data, "src")
                    else -> null
                }
                if (imgUrl != null) MangaPage(index = i, url = imgUrl, imageUrl = imgUrl)
                else null
            }

            Log.d(TAG, "Sayfa listesi: ${pages.size} sayfa")
            pages
        } catch (e: Exception) {
            Log.e(TAG, "Sayfa parse hatası: ${e.message}")
            emptyList()
        }
    }

    // ── Yardımcılar ───────────────────────────────────────────────────────────

    private fun findDataNode(nodes: JSONArray): JSONObject? {
        for (i in 0 until nodes.length()) {
            val node = nodes.optJSONObject(i) ?: continue
            if (node.optString("type") == "data" && node.has("data")) {
                return node
            }
        }
        return null
    }

    /** Relative resim URL'lerini CDN URL ile birleştirir. */
    private fun resolveImageUrl(url: String): String {
        if (url.startsWith("http")) return url
        val cdn = cdnUrl.ifBlank { baseUrl }
        return "${cdn.trimEnd('/')}/${url.trimStart('/')}"
    }

    /** Bölüm adından numara çıkarmaya çalışır. */
    private fun extractChapterNumber(name: String): Float {
        val match = Regex("(\\d+(?:\\.\\d+)?)").find(name)
        return match?.value?.toFloatOrNull() ?: -1f
    }

    /** "2024-01-15T12:00:00" gibi ISO string'i epoch ms'e çevirir. */
    private fun String.parseTimestamp(): Long {
        return try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
            fmt.parse(this.take(19))?.time ?: 0L
        } catch (_: Exception) { 0L }
    }

    /** URL encode (space → %20, vb.) */
    private fun String.encodeUrl(): String =
        java.net.URLEncoder.encode(this, "UTF-8")

    /** JSON pool'dan string değeri çöz (index veya direkt string olabilir). */
    private fun JSONObject.resolveStr(pool: JSONArray, key: String): String? {
        val v = this.opt(key) ?: return null
        return when (v) {
            is String -> v.ifEmpty { null }
            is Int    -> pool.optString(v).ifEmpty { null }
            else      -> null
        }
    }

    data class ResolvedData(val mainObject: JSONObject?, val pool: JSONArray)

    companion object {
        private const val NUVIO_UA = "Mozilla/5.0 (Android 12; Mobile) AppleWebKit/537.36 Chrome/120.0"

        /**
         * Uzaymanga grubu sitelerinin CDN URL'sini paket adından üretir.
         * eu.kanade.tachiyomi.extension.tr.uzaymanga → cdn-u.efsaneler2.can.re
         */
        fun cdnUrlFromPkg(pkgName: String): String {
            val siteName = pkgName.substringAfterLast(".")
            return when (siteName) {
                "uzaymanga"    -> "https://cdn-u.efsaneler2.can.re"
                "eldermanga"   -> "https://cdn-el.efsaneler2.can.re"
                "eskimangalar" -> "https://cdn-es.efsaneler2.can.re"
                "limonmanga"   -> "https://cdn-l.efsaneler2.can.re"
                "tenshimanga"  -> "https://cdn-t.efsaneler2.can.re"
                "afroditscans" -> "https://afroditscans.com"
                else           -> ""
            }
        }
    }
}
