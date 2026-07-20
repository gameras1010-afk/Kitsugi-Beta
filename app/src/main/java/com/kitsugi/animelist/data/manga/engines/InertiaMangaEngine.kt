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
import org.json.JSONObject

/**
 * InertiaMangaEngine
 *
 * Laravel + Inertia.js tabanlı manga siteleri için fallback scraper.
 *
 * Şu an desteklenen tek Inertia sitesi: MangaDenizi (mangadenizi.net)
 *
 * Inertia.js nasıl çalışır?
 *  - Normal sayfa isteği → tam HTML döner
 *  - Header "X-Inertia: true" ile istek → JSON props döner
 *  - "X-Inertia-Version" header'ı server ile senkronize olmalı (mismatch → 409)
 *  - Response: { "component": "Manga/Index", "props": { "mangas": {...} } }
 */
class InertiaMangaEngine(
    private val baseUrl: String,
    private val client: OkHttpClient
) {
    private val TAG = "InertiaEngine[$baseUrl]"
    private var cachedVersion: String = ""

    // ── Arama ─────────────────────────────────────────────────────────────────

    suspend fun searchManga(query: String, page: Int): MangaSourceResult = withContext(Dispatchers.IO) {
        val url = "$baseUrl/manga?search=${query.encodeUrl()}&page=$page"
        Log.d(TAG, "Arama: $url")
        val json = fetchInertia(url)
        parseMangaListFromProps(json)
    }

    // ── Popüler ───────────────────────────────────────────────────────────────

    suspend fun fetchPopularManga(page: Int): MangaSourceResult = withContext(Dispatchers.IO) {
        val url = "$baseUrl/manga?page=$page&sort=views"
        Log.d(TAG, "Popüler: $url")
        val json = fetchInertia(url)
        parseMangaListFromProps(json)
    }

    // ── Manga Detay ───────────────────────────────────────────────────────────

    suspend fun fetchMangaDetails(mangaUrl: String): MangaDetails = withContext(Dispatchers.IO) {
        Log.d(TAG, "Detay: $mangaUrl")
        val json = fetchInertia(mangaUrl)
        parseMangaDetailsFromProps(json, mangaUrl)
    }

    // ── Bölüm Listesi ─────────────────────────────────────────────────────────

    suspend fun fetchChapterList(mangaUrl: String): List<MangaChapter> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Bölümler: $mangaUrl")
        val json = fetchInertia(mangaUrl)
        parseChapterListFromProps(json, mangaUrl)
    }

    // ── Sayfa Listesi ─────────────────────────────────────────────────────────

    suspend fun fetchPageList(chapterUrl: String): List<MangaPage> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Sayfalar: $chapterUrl")
        val json = fetchInertia(chapterUrl)
        parsePageListFromProps(json)
    }

    // ── Inertia HTTP ─────────────────────────────────────────────────────────

    private fun fetchInertia(url: String): JSONObject {
        val version = getOrFetchVersion()
        val request = Request.Builder()
            .url(url)
            .header("X-Inertia", "true")
            .header("X-Inertia-Version", version)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Accept", "text/html, application/xhtml+xml")
            .header("Referer", baseUrl)
            .header("User-Agent", NUVIO_UA)
            .build()

        val response = client.newCall(request).execute()

        return when {
            response.code == 409 -> {
                // Version mismatch → yeni version'ı al ve tekrar dene
                val newVersion = response.headers["X-Inertia-Location"]
                    ?: response.headers["X-Inertia-Version"]
                    ?: ""
                Log.w(TAG, "Inertia version mismatch (409), yeni version: $newVersion")
                cachedVersion = newVersion
                response.close()
                fetchInertia(url) // rekursif retry
            }
            !response.isSuccessful -> {
                val code = response.code
                response.close()
                throw Exception("HTTP $code for $url")
            }
            else -> {
                val body = response.body?.string() ?: throw Exception("Boş response: $url")
                // Inertia JSON veya HTML wrap olabilir
                try {
                    JSONObject(body)
                } catch (_: Exception) {
                    // HTML içinden Inertia data'sını çıkar
                    extractInertiaFromHtml(body)
                }
            }
        }
    }

    /**
     * HTML sayfası döndüyse içindeki Inertia JSON'ını çıkarır.
     * <div id="app" data-page="...JSON..."></div>
     */
    private fun extractInertiaFromHtml(html: String): JSONObject {
        val pattern = Regex("""data-page="([^"]+)"""")
        val match = pattern.find(html)?.groupValues?.getOrNull(1)
            ?: throw Exception("Inertia data-page attribute bulunamadı")
        val decoded = match
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
        return JSONObject(decoded)
    }

    /** Inertia version'ı önce cache'den, yoksa bir GET ile alır. */
    private fun getOrFetchVersion(): String {
        if (cachedVersion.isNotEmpty()) return cachedVersion
        return try {
            val request = Request.Builder()
                .url("$baseUrl/manga")
                .header("User-Agent", NUVIO_UA)
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            response.close()
            // version meta tag veya asset URL hash'inden çıkar
            val versionMatch = Regex("""["']version["']\s*:\s*["']([^"']+)["']""").find(body)
            cachedVersion = versionMatch?.groupValues?.getOrNull(1) ?: ""
            Log.d(TAG, "Inertia version: $cachedVersion")
            cachedVersion
        } catch (e: Exception) {
            Log.w(TAG, "Version alınamadı: ${e.message}")
            ""
        }
    }

    // ── Props Parse ───────────────────────────────────────────────────────────

    private fun parseMangaListFromProps(root: JSONObject): MangaSourceResult {
        val props = root.optJSONObject("props") ?: return MangaSourceResult(emptyList(), false)

        // mangas veya data key'ini dene
        val mangaData = props.optJSONObject("mangas")
            ?: props.optJSONObject("manga_list")
            ?: return MangaSourceResult(emptyList(), false)

        val dataArr = mangaData.optJSONArray("data") ?: return MangaSourceResult(emptyList(), false)

        val mangas = (0 until dataArr.length()).mapNotNull { i ->
            parseMangaItem(dataArr.optJSONObject(i))
        }

        // Sayfalama
        val meta = mangaData.optJSONObject("meta")
        val links = mangaData.optJSONObject("links")
        val hasNext = meta?.let {
            it.optInt("current_page") < it.optInt("last_page")
        } ?: (links?.optString("next")?.isNotEmpty() == true)

        Log.d(TAG, "Manga listesi: ${mangas.size} sonuç, hasNext=$hasNext")
        return MangaSourceResult(mangas, hasNext)
    }

    private fun parseMangaItem(obj: JSONObject?): MangaDetails? {
        if (obj == null) return null
        return try {
            val slug   = obj.optString("slug").ifEmpty { return null }
            val title  = obj.optString("title").ifEmpty { obj.optString("name").ifEmpty { slug } }
            val cover  = obj.optString("cover").ifEmpty { obj.optString("thumbnail") }.ifEmpty { null }
            val status = parseStatus(obj.optString("status"))

            // Categories → genres
            val genres = mutableListOf<String>()
            val cats = obj.optJSONArray("categories")
            if (cats != null) {
                for (i in 0 until cats.length()) {
                    cats.optJSONObject(i)?.optString("name")?.let { genres.add(it) }
                }
            }

            MangaDetails(
                url          = "$baseUrl/manga/$slug",
                title        = title,
                thumbnailUrl = cover,
                status       = status,
                genre        = genres,
                source       = "MangaDenizi"
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMangaDetailsFromProps(root: JSONObject, mangaUrl: String): MangaDetails {
        val props  = root.optJSONObject("props") ?: return MangaDetails(url = mangaUrl, title = mangaUrl)
        val manga  = props.optJSONObject("manga") ?: return MangaDetails(url = mangaUrl, title = mangaUrl)

        val slug    = manga.optString("slug")
        val title   = manga.optString("title").ifEmpty { manga.optString("name").ifEmpty { slug } }
        val desc    = manga.optString("description").ifEmpty { null }
        val cover   = manga.optString("cover").ifEmpty { null }
        val status  = parseStatus(manga.optString("status"))

        val genres = mutableListOf<String>()
        val cats   = manga.optJSONArray("categories")
        if (cats != null) {
            for (i in 0 until cats.length()) {
                cats.optJSONObject(i)?.optString("name")?.let { genres.add(it) }
            }
        }

        val authorObj = manga.optJSONArray("authors")?.optJSONObject(0)
        val author = authorObj?.optString("name")?.ifEmpty { null }

        return MangaDetails(
            url          = mangaUrl,
            title        = title,
            description  = desc,
            author       = author,
            thumbnailUrl = cover,
            status       = status,
            genre        = genres,
            source       = "MangaDenizi"
        )
    }

    private fun parseChapterListFromProps(root: JSONObject, mangaUrl: String): List<MangaChapter> {
        val props      = root.optJSONObject("props") ?: return emptyList()
        val manga      = props.optJSONObject("manga") ?: return emptyList()
        val mangaSlug  = manga.optString("slug").ifEmpty { mangaUrl.substringAfterLast('/') }
        val chapterArr = manga.optJSONArray("chapters") ?: return emptyList()

        val chapters = (0 until chapterArr.length()).mapNotNull { i ->
            val ch    = chapterArr.optJSONObject(i) ?: return@mapNotNull null
            val slug  = ch.optString("slug").ifEmpty { return@mapNotNull null }
            val title = ch.optString("title").ifEmpty { ch.optString("name").ifEmpty { "Bölüm $i" } }
            val num   = ch.optString("chapter_number").toFloatOrNull()
                ?: extractChapterNumber(title)
            val date  = ch.optString("created_at").parseTimestamp()

            MangaChapter(
                url           = "$baseUrl/read/$mangaSlug/$slug",
                name          = title,
                chapterNumber = num,
                uploadDate    = date,
                mangaUrl      = mangaUrl
            )
        }
        Log.d(TAG, "Bölüm listesi: ${chapters.size} bölüm")
        return chapters.sortedByDescending { it.chapterNumber }
    }

    private fun parsePageListFromProps(root: JSONObject): List<MangaPage> {
        val props = root.optJSONObject("props") ?: return emptyList()

        // pages veya reader_pages
        val pageArr: JSONArray? = props.optJSONArray("pages")
            ?: props.optJSONObject("reader")?.optJSONArray("pages")
            ?: props.optJSONArray("images")

        if (pageArr == null) return emptyList()

        val pages = (0 until pageArr.length()).mapNotNull { i ->
            val page = pageArr.opt(i)
            val imgUrl = when (page) {
                is String     -> page
                is JSONObject -> page.optString("image_url").ifEmpty {
                    page.optString("url").ifEmpty { null }
                }
                else -> null
            }
            if (imgUrl != null) MangaPage(index = i, url = imgUrl, imageUrl = imgUrl)
            else null
        }
        Log.d(TAG, "Sayfa listesi: ${pages.size} sayfa")
        return pages
    }

    // ── Yardımcılar ───────────────────────────────────────────────────────────

    private fun parseStatus(raw: String): MangaStatus {
        return when (raw.lowercase()) {
            "ongoing", "devam ediyor", "devam"     -> MangaStatus.Ongoing
            "completed", "tamamlandı", "bitti"     -> MangaStatus.Completed
            "hiatus", "ara", "beklemede"           -> MangaStatus.OnHiatus
            "cancelled", "dropped", "bırakıldı"   -> MangaStatus.Cancelled
            else                                    -> MangaStatus.Unknown
        }
    }

    private fun extractChapterNumber(name: String): Float {
        return Regex("(\\d+(?:\\.\\d+)?)").find(name)?.value?.toFloatOrNull() ?: -1f
    }

    private fun String.parseTimestamp(): Long {
        return try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
            fmt.parse(this.take(19))?.time ?: 0L
        } catch (_: Exception) { 0L }
    }

    private fun String.encodeUrl(): String =
        java.net.URLEncoder.encode(this, "UTF-8")

    companion object {
        private const val NUVIO_UA = "Mozilla/5.0 (Android 12; Mobile) AppleWebKit/537.36 Chrome/120.0"
    }
}
