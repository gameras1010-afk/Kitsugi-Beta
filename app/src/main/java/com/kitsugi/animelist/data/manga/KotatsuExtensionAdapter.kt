package com.kitsugi.animelist.data.manga

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.webview.InterceptedRequest
import org.koitharu.kotatsu.parsers.webview.InterceptionConfig as ParsersInterceptionConfig
import org.koitharu.kotatsu.parsers.model.Manga as KotatsuManga
import org.koitharu.kotatsu.parsers.model.MangaChapter as KotatsuChapter
import org.koitharu.kotatsu.parsers.model.MangaPage as KotatsuPage

/**
 * KotatsuExtensionAdapter
 *
 * kotatsu-parsers-redo kütüphanesindeki 1300+ kaynağı Kitsugi'nun MangaSource
 * arayüzüne adapte eder.
 *
 * Futon ile aynı mantık:
 *   - MangaParserSource enum'undan tüm kaynaklar alınır (compile-time)
 *   - MangaLoaderContext üzerinden gerçek HTTP istekleri yapılır
 *   - Dil filtresi ile sadece seçili dildeki kaynaklar aktif olur
 */
object KotatsuExtensionAdapter {

    private const val TAG = "KotatsuExtAdapter"

    // T4-11: Bounded scope — SupervisorJob so one child failure doesn't cancel siblings
    private val adapterScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, t ->
            Log.e(TAG, "Uncaught coroutine error: ${t.message}", t)
        }
    )

    // Tüm yüklü kaynaklar (ALL langs) — başlatma sonrası dolu olur
    private val allSources = mutableListOf<KotatsuMangaSource>()
    private var loaderContext: MangaLoaderContext? = null
    private var isInitialized = false

    /**
     * Aktif dil seti — sadece bu dillerdeki kaynaklar MangaExtensionLoader'a döner.
     * Varsayılan: {"tr"} — Futon'un davranışıyla aynı.
     */
    private val activeLangs = mutableSetOf("tr")

    /** Aktif dilleri döner (UI chip gösterimi için) */
    fun getActiveLangs(): Set<String> = activeLangs.toSet()

    /** Tek bir dili toggle eder */
    fun toggleLang(lang: String) {
        if (lang in activeLangs) {
            if (activeLangs.size > 1) activeLangs.remove(lang)
        } else {
            activeLangs.add(lang)
        }
        Log.i(TAG, "Kotatsu dil toggle: $lang → aktif: $activeLangs")
    }

    /** Aktif dil setini toptan günceller */
    fun setActiveLangs(langs: Set<String>) {
        activeLangs.clear()
        activeLangs.addAll(langs)
        Log.i(TAG, "Kotatsu aktif diller güncellendi: $langs")
    }

    /**
     * Tüm Kotatsu parser kaynaklarını MangaParserSource enum'undan yükler.
     * Bu metod KitsugiApplication.onCreate() → syncKotatsuSources() tarafından çağrılır.
     *
     * Futon'daki MangaSourcesRepository.allMangaSources ile birebir aynı mantık.
     */
    private var isInitializing = false

    /**
     * Tüm Kotatsu parser kaynaklarını MangaParserSource enum'undan yükler.
     * Bu metod KitsugiApplication.onCreate() → syncKotatsuSources() tarafından çağrılır.
     *
     * Futon'daki MangaSourcesRepository.allMangaSources ile birebir aynı mantık.
     */
    fun initialize(context: Context) {
        initializeLazy(context)
    }

    fun initializeLazy(context: Context) {
        if (isInitialized || isInitializing) return
        isInitializing = true
        // T4-11: Use bounded adapterScope instead of GlobalScope
        adapterScope.launch {
            try {
                loaderContext = KitsugiKotatsuContext(context)
                val ctx = loaderContext ?: run { isInitializing = false; return@launch }

                val result = mutableListOf<KotatsuMangaSource>()

                // MangaParserSource.entries → tüm 1300+ kaynak (bozuk olanlar hariç)
                for (parserSource in MangaParserSource.entries) {
                    if (parserSource.isBroken()) continue
                    try {
                        val parser = ctx.newParserInstance(parserSource)
                        result.add(
                            KotatsuMangaSource(
                                parserSource = parserSource,
                                parser = parser,
                                context = ctx as KitsugiKotatsuContext,
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Parser oluşturulamadı: ${parserSource.name} → ${e.message}")
                    }
                }

                synchronized(allSources) {
                    allSources.clear()
                    allSources.addAll(result)
                }
                isInitialized = true
                isInitializing = false

                val langCounts = result.groupBy { it.lang }.mapValues { it.value.size }
                Log.i(TAG, "Kotatsu init: ${result.size} kaynak | Dil: $langCounts | Aktif: $activeLangs")
            } catch (e: Exception) {
                isInitializing = false
                Log.e(TAG, "Kotatsu initialize hatası: ${e.message}", e)
            }
        }
    }

    /** Sadece aktif dillerdeki kaynakları döner */
    fun getActiveSources(): List<KotatsuMangaSource> = synchronized(allSources) {
        allSources.filter { it.lang in activeLangs }
    }

    /** Tüm kaynaklar (istatistik/UI için) */
    fun getLoadedSources(): List<KotatsuMangaSource> = synchronized(allSources) {
        allSources.toList()
    }

    /**
     * Güncel JSON kataloğunu alır. Kotatsu parser'ları derleme zamanında (compile-time)
     * eklendiği için dinamik kod yüklenemez; ancak catalog SHA eşitlemesi için
     * bu metod çağrılır. Mevcut yüklü kaynak listesini döner.
     */
    fun loadSources(jsonStr: String): List<KotatsuMangaSource> {
        Log.i(TAG, "loadSources çağrıldı, JSON katalog boyutu: ${jsonStr.length} karakter")
        return getLoadedSources()
    }

    /** Dile göre filtrele */
    fun getSourcesByLang(lang: String): List<KotatsuMangaSource> = synchronized(allSources) {
        allSources.filter { it.lang.equals(lang, ignoreCase = true) }
    }

    fun getSourceCount(): Int = synchronized(allSources) { allSources.size }
    fun getActiveSourceCount(): Int = getActiveSources().size

    /** Kataloğun içerdiği tüm dil kodlarını döner (TR her zaman ilk) */
    fun getAllLangs(): List<String> = synchronized(allSources) {
        val langs = allSources.map { it.lang }.distinct().sorted().toMutableList()
        if (langs.remove("tr")) langs.add(0, "tr")
        return langs
    }

    // MangaParserSource.isBroken() yok diye Futon'dan alınan yardımcı
    private fun MangaParserSource.isBroken(): Boolean = try {
        // Bazı kaynakların contentType veya locale bilgisi eksik olabilir
        this.locale // null dönerse exception fırlayabilir
        false
    } catch (_: Exception) {
        true
    }
}

// ─── KitsugiSourceConfig — MangaSourceConfig implementasyonu ───────────────────
class KitsugiSourceConfig(
    context: Context,
    source: org.koitharu.kotatsu.parsers.model.MangaSource
) : org.koitharu.kotatsu.parsers.config.MangaSourceConfig {
    private val prefs = context.getSharedPreferences("kotatsu_src_${source.name.replace('/', '$')}", Context.MODE_PRIVATE)

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: org.koitharu.kotatsu.parsers.config.ConfigKey<T>): T {
        val value = when (key) {
            is org.koitharu.kotatsu.parsers.config.ConfigKey.UserAgent -> prefs.getString(key.key, key.defaultValue) ?: key.defaultValue
            is org.koitharu.kotatsu.parsers.config.ConfigKey.Domain -> prefs.getString(key.key, key.defaultValue) ?: key.defaultValue
            is org.koitharu.kotatsu.parsers.config.ConfigKey.ShowSuspiciousContent -> prefs.getBoolean(key.key, key.defaultValue)
            is org.koitharu.kotatsu.parsers.config.ConfigKey.SplitByTranslations -> prefs.getBoolean(key.key, key.defaultValue)
            is org.koitharu.kotatsu.parsers.config.ConfigKey.PreferredImageServer -> prefs.getString(key.key, key.defaultValue) ?: key.defaultValue
            is org.koitharu.kotatsu.parsers.config.ConfigKey.DisableUpdateChecking -> prefs.getBoolean(key.key, key.defaultValue)
            is org.koitharu.kotatsu.parsers.config.ConfigKey.InterceptCloudflare -> prefs.getBoolean(key.key, key.defaultValue)
            else -> key.defaultValue
        }
        return value as T
    }
}

// ─── BitmapWrapper — Kotatsu Bitmap implementasyonu ──────────────────────────
class BitmapWrapper private constructor(
    private val androidBitmap: android.graphics.Bitmap,
) : org.koitharu.kotatsu.parsers.bitmap.Bitmap, AutoCloseable {

    private val canvas by lazy { android.graphics.Canvas(androidBitmap) }

    override val height: Int
        get() = androidBitmap.height

    override val width: Int
        get() = androidBitmap.width

    override fun drawBitmap(sourceBitmap: org.koitharu.kotatsu.parsers.bitmap.Bitmap, src: org.koitharu.kotatsu.parsers.bitmap.Rect, dst: org.koitharu.kotatsu.parsers.bitmap.Rect) {
        val androidSourceBitmap = (sourceBitmap as BitmapWrapper).androidBitmap
        canvas.drawBitmap(androidSourceBitmap, src.toAndroidRect(), dst.toAndroidRect(), null)
    }

    override fun close() {
        androidBitmap.recycle()
    }

    companion object {
        fun create(width: Int, height: Int) = BitmapWrapper(
            android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888),
        )

        private fun org.koitharu.kotatsu.parsers.bitmap.Rect.toAndroidRect() = android.graphics.Rect(left, top, right, bottom)
    }
}

// ─── KitsugiKotatsuContext — MangaLoaderContext implementasyonu ─────────────────
class KitsugiKotatsuContext(private val context: Context) : MangaLoaderContext() {

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override val httpClient: okhttp3.OkHttpClient
        get() = client

    override val cookieJar: okhttp3.CookieJar
        get() = okhttp3.CookieJar.NO_COOKIES

    override fun getConfig(source: org.koitharu.kotatsu.parsers.model.MangaSource): org.koitharu.kotatsu.parsers.config.MangaSourceConfig {
        return KitsugiSourceConfig(context, source)
    }

    override suspend fun evaluateJs(script: String): String? = null

    override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = null

    override fun getDefaultUserAgent(): String = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    override fun encodeBase64(data: ByteArray): String {
        return android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
    }

    override fun decodeBase64(data: String): ByteArray {
        return android.util.Base64.decode(data, android.util.Base64.DEFAULT)
    }

    override fun getPreferredLocales(): List<java.util.Locale> {
        return listOf(java.util.Locale.getDefault())
    }

    override fun requestBrowserAction(parser: org.koitharu.kotatsu.parsers.MangaParser, url: String): Nothing {
        throw UnsupportedOperationException("Browser action required for ${parser.source.name} at $url")
    }

    override fun redrawImageResponse(response: okhttp3.Response, redraw: (image: org.koitharu.kotatsu.parsers.bitmap.Bitmap) -> org.koitharu.kotatsu.parsers.bitmap.Bitmap): okhttp3.Response = response

    override fun createBitmap(width: Int, height: Int): org.koitharu.kotatsu.parsers.bitmap.Bitmap {
        return BitmapWrapper.create(width, height)
    }

    override suspend fun interceptWebViewRequests(
        url: String,
        interceptorScript: String,
        timeout: Long
    ): List<org.koitharu.kotatsu.parsers.webview.InterceptedRequest> = emptyList()

    override suspend fun interceptWebViewRequests(
        url: String,
        config: org.koitharu.kotatsu.parsers.webview.InterceptionConfig
    ): List<org.koitharu.kotatsu.parsers.webview.InterceptedRequest> = emptyList()

    override suspend fun captureWebViewUrls(
        pageUrl: String,
        urlPattern: Regex,
        timeout: Long
    ): List<String> = emptyList()
}

// ─── KotatsuMangaSource — Gerçek Parser Wrapper ──────────────────────────────
class KotatsuMangaSource(
    val parserSource: MangaParserSource,
    private val parser: MangaParser,
    private val context: KitsugiKotatsuContext,
) : MangaSource {

    override val name: String get() = parserSource.title
    override val lang: String get() = parserSource.locale ?: "unknown"
    override val pkgName: String get() = "kotatsu.${parserSource.name.lowercase()}"
    override val baseUrl: String get() = try { parser.domain } catch (_: Exception) { "" }

    override suspend fun fetchPopularManga(page: Int): MangaSourceResult = withContext(Dispatchers.IO) {
        try {
            val list = parser.getList(
                offset = (page - 1) * 20,
                filter = MangaListFilter(query = ""),
                order = SortOrder.POPULARITY,
            )
            MangaSourceResult(list.map { it.toMangaDetails() }, list.size >= 20)
        } catch (e: Exception) {
            android.util.Log.w("KotatsuMangaSrc", "fetchPopularManga hata [${parserSource.name}]: ${e.message}")
            MangaSourceResult(emptyList(), false)
        }
    }

    override suspend fun fetchSearchManga(page: Int, query: String): MangaSourceResult = withContext(Dispatchers.IO) {
        try {
            val list = parser.getList(
                offset = (page - 1) * 20,
                filter = MangaListFilter(query = query),
                order = SortOrder.RELEVANCE,
            )
            MangaSourceResult(list.map { it.toMangaDetails() }, list.size >= 20)
        } catch (e: Exception) {
            android.util.Log.w("KotatsuMangaSrc", "fetchSearchManga hata [${parserSource.name}]: ${e.message}")
            MangaSourceResult(emptyList(), false)
        }
    }

    override suspend fun fetchMangaDetails(mangaUrl: String): MangaDetails = withContext(Dispatchers.IO) {
        try {
            val stub = KotatsuManga(
                id = mangaUrl.hashCode().toLong(),
                title = name,
                altTitles = emptySet(),
                url = mangaUrl,
                publicUrl = mangaUrl,
                rating = 0f,
                contentRating = ContentRating.SAFE,
                coverUrl = "",
                tags = emptySet(),
                state = null,
                authors = emptySet(),
                largeCoverUrl = null,
                description = null,
                chapters = null,
                source = parserSource,
            )
            val detail = parser.getDetails(stub)
            detail.toMangaDetails()
        } catch (e: Exception) {
            android.util.Log.w("KotatsuMangaSrc", "fetchMangaDetails hata [${parserSource.name}]: ${e.message}")
            MangaDetails(url = mangaUrl, title = name, source = pkgName)
        }
    }

    override suspend fun fetchChapterList(mangaUrl: String): List<MangaChapter> = withContext(Dispatchers.IO) {
        try {
            val stub = KotatsuManga(
                id = mangaUrl.hashCode().toLong(),
                title = name,
                altTitles = emptySet(),
                url = mangaUrl,
                publicUrl = mangaUrl,
                rating = 0f,
                contentRating = ContentRating.SAFE,
                coverUrl = "",
                tags = emptySet(),
                state = null,
                authors = emptySet(),
                largeCoverUrl = null,
                description = null,
                chapters = null,
                source = parserSource,
            )
            val detail = parser.getDetails(stub)
            detail.chapters?.map { chapter ->
                MangaChapter(
                    url = chapter.url,
                    name = chapter.title ?: "",
                    chapterNumber = chapter.number,
                    uploadDate = chapter.uploadDate,
                    mangaUrl = mangaUrl,
                )
            } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.w("KotatsuMangaSrc", "fetchChapterList hata [${parserSource.name}]: ${e.message}")
            emptyList()
        }
    }

    override suspend fun fetchPageList(chapter: MangaChapter): List<MangaPage> = withContext(Dispatchers.IO) {
        try {
            val kotatsuChapter = KotatsuChapter(
                id = chapter.url.hashCode().toLong(),
                title = chapter.name,
                number = chapter.chapterNumber,
                volume = 0,
                url = chapter.url,
                scanlator = chapter.scanlator,
                uploadDate = chapter.uploadDate,
                branch = null,
                source = parserSource,
            )
            val pages = parser.getPages(kotatsuChapter)
            pages.mapIndexed { index, page ->
                MangaPage(
                    index = index,
                    url = page.url,
                    imageUrl = page.preview,
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("KotatsuMangaSrc", "fetchPageList hata [${parserSource.name}]: ${e.message}")
            emptyList()
        }
    }

    override suspend fun fetchImageUrl(page: MangaPage): String = withContext(Dispatchers.IO) {
        try {
            val kotatsuPage = KotatsuPage(
                id = page.url.hashCode().toLong(),
                url = page.url,
                preview = page.imageUrl,
                source = parserSource,
            )
            parser.getPageUrl(kotatsuPage)
        } catch (e: Exception) {
            page.imageUrl ?: page.url
        }
    }

    override suspend fun getImage(page: MangaPage): java.io.InputStream {
        val imageUrl = page.imageUrl ?: throw IllegalArgumentException("Image URL is null")
        val headersBuilder = okhttp3.Headers.Builder()
        try {
            headersBuilder.addAll(parser.getRequestHeaders())
        } catch (_: Exception) {}

        if (headersBuilder["User-Agent"] == null) {
            headersBuilder["User-Agent"] = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }
        if (headersBuilder["Referer"] == null) {
            headersBuilder["Referer"] = baseUrl
        }

        val request = okhttp3.Request.Builder()
            .url(imageUrl)
            .headers(headersBuilder.build())
            .build()

        return withContext(Dispatchers.IO) {
            val response = context.httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                throw java.io.IOException("HTTP error ${response.code} for $imageUrl")
            }
            val body = response.body ?: throw java.io.IOException("Response body is null")
            object : java.io.FilterInputStream(body.byteStream()) {
                override fun close() {
                    super.close()
                    response.close()
                }
            }
        }
    }

    override fun toString(): String = "KotatsuMangaSource[${parserSource.name}/$lang]"

    // ── Yardımcı dönüşüm ──────────────────────────────────────────────────────

    private fun KotatsuManga.toMangaDetails() = MangaDetails(
        url = url,
        title = title,
        author = authors.joinToString(", ").takeIf { it.isNotBlank() },
        description = description,
        thumbnailUrl = coverUrl?.takeIf { it.isNotBlank() },
        source = pkgName,
    )
}

// ─── Eski KotatsuSourceType (geriye dönük uyumluluk) ─────────────────────────

enum class KotatsuSourceType {
    DIRECT_PARSER,
    KEIYOUSHI_WITH_DOMAIN,
}
