package com.kitsugi.animelist.data.manga

import android.content.Context
import com.kitsugi.animelist.data.manga.engines.InertiaMangaEngine
import com.kitsugi.animelist.data.manga.engines.SvelteMangaEngine
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import android.util.Log
import tachiyomi.core.common.util.lang.awaitSingle

/**
 * MihonSourceWrapper
 *
 * Bir Mihon/Tachiyomi APK'sından yüklenen eu.kanade.tachiyomi.source.Source nesnesini,
 * Kitsugi'nun yerel MangaSource interface'ine adapte eden köprü sınıfı.
 *
 * Bu sayede Keiyoushi gibi repolardan indirilen binlerce eklenti,
 * hiçbir değişiklik yapılmadan Kitsugi içinde çalışabilir.
 *
 * @param mihonSource Mihon APK'dan PathClassLoader ile yüklenmiş Source nesnesi
 * @param context Android Context (gerekli olduğu durumlarda)
 * @param pkgName Kaynak paket adı
 */
class MihonSourceWrapper(
    private val mihonSource: Source,
    private val context: Context,
    override val pkgName: String,
    override val engineType: ExtensionEngine = ExtensionEngine.UNKNOWN,
) : MangaSource {

    private val TAG = "MihonSourceWrapper"
    private val httpSource: HttpSource? get() = mihonSource as? HttpSource
    private val sourceConfigStore = SourceConfigStore(context)
    private val mirrorResolver = SourceMirrorResolver(context, sourceConfigStore)
    private val okHttpProvider by lazy { com.kitsugi.animelist.core.network.NuvioOkHttpProvider(context) }

    // ── Lazy Fallback Engine'ler ───────────────────────────────────────────────

    private val networkClient by lazy {
        try {
            uy.kohesive.injekt.Injekt.get(eu.kanade.tachiyomi.network.NetworkHelper::class.java).client
        } catch (_: Exception) {
            okhttp3.OkHttpClient.Builder().build()
        }
    }

    private val svelteEngine: SvelteMangaEngine? by lazy {
        if (engineType == ExtensionEngine.SVELTE) {
            val cdn = SvelteMangaEngine.cdnUrlFromPkg(pkgName)
            SvelteMangaEngine(rawBaseUrl(), cdn, networkClient)
        } else null
    }

    private val inertiaEngine: InertiaMangaEngine? by lazy {
        if (engineType == ExtensionEngine.INERTIA) {
            InertiaMangaEngine(rawBaseUrl(), networkClient)
        } else null
    }

    override val name: String get() = mihonSource.name
    override val lang: String get() = mihonSource.lang

    override val originalBaseUrl: String get() = rawBaseUrl()

    /**
     * HttpSource'un baseUrl'sini reflection ile okumaya çalışır.
     * Eklenti HttpSource'u implemente etmiyorsa boş string döner.
     */
    override val baseUrl: String
        get() = sourceConfigStore.getPreferredBaseUrl(this, rawBaseUrl()) ?: rawBaseUrl()

    private fun rawBaseUrl(): String = try {
        mihonSource.javaClass.getDeclaredField("baseUrl").let {
            it.isAccessible = true
            it.get(mihonSource) as? String ?: ""
        }
    } catch (_: Exception) {
        ""
    }

    private fun applyRuntimeSourceConfig() {
        runCatching {
            mirrorResolver.applyStoredDomainPreference(mihonSource.id, this)
        }
    }

    private suspend fun maybeSlowdown() {
        if (sourceConfigStore.getSlowdownEnabled(this)) {
            kotlinx.coroutines.delay(350L)
        }
    }

    private suspend fun <T> withRecovery(block: suspend () -> T): T {
        applyRuntimeSourceConfig()
        maybeSlowdown()
        return try {
            block()
        } catch (e: Exception) {
            val switched = mirrorResolver.tryResolveAndActivateMirror(this, e)
            if (!switched) throw e
            applyRuntimeSourceConfig()
            maybeSlowdown()
            block()
        }
    }

    // ── Popüler Manga ────────────────────────────────────────────────────────

    override suspend fun fetchPopularManga(page: Int): MangaSourceResult = withRecovery {
        val t0 = System.currentTimeMillis()
        try {
            val pageResult = mihonSource.getPopularManga(page)
            val result = MangaSourceResult(
                mangas = pageResult.mangas.map { it.toMangaDetails() },
                hasNextPage = pageResult.hasNextPage
            )
            MangaLogger.logPopular(context, mihonSource.name, success = true,
                resultCount = result.mangas.size, elapsedMs = System.currentTimeMillis() - t0)
            result
        } catch (e: Exception) {
            Log.w(TAG, "${mihonSource.name} APK popular başarısız (engine=$engineType): ${e.message}")
            val fallback = when (engineType) {
                ExtensionEngine.SVELTE  -> svelteEngine?.fetchPopularManga(page)
                ExtensionEngine.INERTIA -> inertiaEngine?.fetchPopularManga(page)
                else -> null
            }
            if (fallback != null) {
                MangaLogger.logPopular(context, mihonSource.name, success = true,
                    resultCount = fallback.mangas.size, elapsedMs = System.currentTimeMillis() - t0)
                fallback
            } else {
                MangaLogger.logPopular(context, mihonSource.name, success = false,
                    elapsedMs = System.currentTimeMillis() - t0, error = e)
                throw e
            }
        }
    }

    // ── Arama ────────────────────────────────────────────────────────────────

    override suspend fun fetchSearchManga(page: Int, query: String): MangaSourceResult = withRecovery {
        val t0 = System.currentTimeMillis()
        try {
            val raw = mihonSource.getSearchManga(page, query, FilterList())
            val result = MangaSourceResult(
                mangas = raw.mangas.map { it.toMangaDetails() },
                hasNextPage = raw.hasNextPage
            )
            MangaLogger.logSearch(context, mihonSource.name, query, success = true,
                resultCount = result.mangas.size, elapsedMs = System.currentTimeMillis() - t0)
            result
        } catch (e: Exception) {
            Log.w(TAG, "${mihonSource.name} APK search başarısız (engine=$engineType): ${e.message}")
            val fallback = when (engineType) {
                ExtensionEngine.SVELTE  -> svelteEngine?.searchManga(query, page)
                ExtensionEngine.INERTIA -> inertiaEngine?.searchManga(query, page)
                else -> null
            }
            if (fallback != null) {
                MangaLogger.logSearch(context, mihonSource.name, query, success = true,
                    resultCount = fallback.mangas.size, elapsedMs = System.currentTimeMillis() - t0)
                fallback
            } else {
                MangaLogger.logSearch(context, mihonSource.name, query, success = false,
                    elapsedMs = System.currentTimeMillis() - t0, error = e)
                throw e
            }
        }
    }

    // ── Manga Detayları ───────────────────────────────────────────────────────

    override suspend fun fetchMangaDetails(mangaUrl: String): MangaDetails {
        val stub = SManga.create().apply { url = mangaUrl; title = "" }
        val t0 = System.currentTimeMillis()
        return try {
            withRecovery {
                val src = httpSource ?: run {
                    Log.w(TAG, "${mihonSource.name}: HttpSource değil, detay çekilemiyor")
                    return@withRecovery MangaDetails(url = mangaUrl, title = mangaUrl)
                }
                val details = kotlinx.coroutines.withTimeout(20_000L) {
                    src.fetchMangaDetails(stub).awaitSingle().toMangaDetails()
                }
                MangaLogger.logMangaDetails(context, mihonSource.name, mangaUrl, success = true)
                details
            }
        } catch (e: Exception) {
            Log.w(TAG, "${mihonSource.name} APK detay başarısız (engine=$engineType): ${e.message}")
            try {
                val fallback = when (engineType) {
                    ExtensionEngine.SVELTE  -> svelteEngine?.fetchMangaDetails(mangaUrl)
                    ExtensionEngine.INERTIA -> inertiaEngine?.fetchMangaDetails(mangaUrl)
                    else -> null
                }
                if (fallback != null) {
                    MangaLogger.logMangaDetails(context, mihonSource.name, mangaUrl, success = true)
                    fallback
                } else {
                    MangaLogger.logMangaDetails(context, mihonSource.name, mangaUrl, success = false, error = e)
                    MangaDetails(url = mangaUrl, title = mangaUrl)
                }
            } catch (fe: Exception) {
                Log.e(TAG, "${mihonSource.name} fallback detay da başarısız: ${fe.message}")
                MangaLogger.logMangaDetails(context, mihonSource.name, mangaUrl, success = false, error = fe)
                MangaDetails(url = mangaUrl, title = mangaUrl)
            }
        }
    }

    // ── Bölüm Listesi ─────────────────────────────────────────────────────────

    override suspend fun fetchChapterList(mangaUrl: String): List<MangaChapter> {
        val t0 = System.currentTimeMillis()
        return try {
            withRecovery {
                val src = httpSource ?: run {
                    Log.w(TAG, "${mihonSource.name}: HttpSource değil, bölüm listesi çekilemiyor")
                    return@withRecovery emptyList()
                }
                val chapterStub = SManga.create().apply { url = mangaUrl; title = "" }

                // Manga başlığını ChapterRecognition için çekmeye çalış.
                val mangaTitle = try {
                    val detailStub = SManga.create().apply { url = mangaUrl; title = "" }
                    kotlinx.coroutines.withTimeout(10_000L) {
                        src.fetchMangaDetails(detailStub).awaitSingle().title
                    }.takeIf { it.isNotBlank() && !it.startsWith("/") } ?: ""
                } catch (_: Exception) { "" }

                val rawChapters = kotlinx.coroutines.withTimeout(45_000L) {
                    src.fetchChapterList(chapterStub).awaitSingle()
                }
                Log.d(TAG, "${mihonSource.name}: ${rawChapters.size} bölüm alındı — $mangaUrl")
                val mapped = rawChapters.map { it.toMangaChapter(mangaUrl, mangaTitle) }
                    .sortedWith(
                        compareByDescending<MangaChapter> {
                            if (it.chapterNumber >= 0f) it.chapterNumber else Float.NEGATIVE_INFINITY
                        }
                    )
                MangaLogger.logChapterList(context, mihonSource.name, mangaUrl, success = true,
                    chapterCount = mapped.size, elapsedMs = System.currentTimeMillis() - t0)
                mapped
            }
        } catch (e: Exception) {
            Log.e(TAG, "${mihonSource.name} fetchChapterList hata: ${e.message}")
            MangaLogger.logChapterList(context, mihonSource.name, mangaUrl, success = false,
                elapsedMs = System.currentTimeMillis() - t0, error = e)
            emptyList()
        }
    }

    // ── Sayfa Listesi ─────────────────────────────────────────────────────────

    // getPageList() — suspend API üzerinden çağırır.
    // CatalogueSource.getPageList() → fetchPageList().awaitSingle() köprüsü devreye girer,
    // bu da HttpSource.fetchPageList() → HTTP isteği + pageListParse() zincirini tetikler.
    override suspend fun fetchPageList(chapter: MangaChapter): List<MangaPage> {
        val sChapter = SChapter.create().apply {
            url = chapter.url
            name = chapter.name
            chapter_number = chapter.chapterNumber
        }
        val t0 = System.currentTimeMillis()
        return try {
            withRecovery {
                val pages = mihonSource.getPageList(sChapter).map { page ->
                    MangaPage(
                        index = page.index,
                        url = page.url,
                        imageUrl = page.imageUrl
                    )
                }
                MangaLogger.logPageList(context, mihonSource.name, chapter.name, success = true,
                    pageCount = pages.size, elapsedMs = System.currentTimeMillis() - t0)
                pages
            }
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            val isCaptcha = msg.contains("captcha") || msg.contains("webview") ||
                msg.contains("doğrula") || msg.contains("verify") || msg.contains("challenge")
            if (isCaptcha) {
                Log.w(TAG, "${mihonSource.name}: Captcha/WebView engeli tespit edildi")
                MangaLogger.logCaptchaDetected(context, mihonSource.name, "fetchPageList", chapter.name)
                throw CaptchaRequiredException(mihonSource.name, e.message ?: "Captcha gerekli")
            }
            Log.e(TAG, "${mihonSource.name} fetchPageList hata: ${e.message}")
            MangaLogger.logPageList(context, mihonSource.name, chapter.name, success = false,
                elapsedMs = System.currentTimeMillis() - t0, error = e)
            throw e
        }
    }

    // ── Resim URL Çözümleme ───────────────────────────────────────────────────

    override suspend fun fetchImageUrl(page: MangaPage): String {
        if (!page.imageUrl.isNullOrBlank()) return page.imageUrl!!

        // HttpSource.getImageUrl bir `suspend` fonksiyondur; reflection (getMethod)
        // ile bulunamaz çünkü derlenmiş imzası getImageUrl(Page, Continuation) olur.
        // Bu yüzden kaynağı HttpSource'a cast edip DOĞRUDAN çağırıyoruz.
        val mihonPage = eu.kanade.tachiyomi.source.model.Page(
            index = page.index,
            url = page.url,
            imageUrl = page.imageUrl
        )
        return try {
            withRecovery {
                val httpSource = mihonSource as? eu.kanade.tachiyomi.source.online.HttpSource
                httpSource?.getImageUrl(mihonPage) ?: page.url
            }
        } catch (_: Exception) {
            page.url
        }
    }

    override suspend fun getImage(page: MangaPage): java.io.InputStream {
        val imageUrl = page.imageUrl ?: throw IllegalArgumentException("Image URL is null")
        applyRuntimeSourceConfig()

        var client: okhttp3.OkHttpClient? = null
        var headers: okhttp3.Headers? = null

        try {
            val clientMethod = mihonSource.javaClass.getMethod("getClient")
            clientMethod.isAccessible = true
            client = clientMethod.invoke(mihonSource) as? okhttp3.OkHttpClient
        } catch (_: Exception) {}

        try {
            val headersMethod = mihonSource.javaClass.getMethod("getHeaders")
            headersMethod.isAccessible = true
            headers = headersMethod.invoke(mihonSource) as? okhttp3.Headers
        } catch (_: Exception) {}

        val finalClient = if (client != null) {
            okHttpProvider.imageClient.newBuilder()
                .apply {
                    client.interceptors.forEach { addInterceptor(it) }
                }
                .build()
        } else {
            okHttpProvider.imageClient
        }
        val requestBuilder = okhttp3.Request.Builder().url(imageUrl)
        val userAgentOverride = sourceConfigStore.getUserAgentOverride(this)

        if (headers != null) {
            requestBuilder.headers(headers)
            if (!userAgentOverride.isNullOrBlank()) {
                requestBuilder.header("User-Agent", userAgentOverride)
            }
        } else {
            requestBuilder.header("Referer", baseUrl)
            requestBuilder.header("User-Agent", userAgentOverride ?: "Mozilla/5.0")
        }

        val request = requestBuilder.build()
        maybeSlowdown()

        val imgT0 = System.currentTimeMillis()
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            var lastError: Exception? = null
            repeat(2) { attempt ->
                try {
                    val response = finalClient.newCall(request).execute()
                    if (!response.isSuccessful) {
                        response.close()
                        throw java.io.IOException("HTTP error ${response.code} for $imageUrl")
                    }
                    val body = response.body ?: throw java.io.IOException("Response body is null")
                    return@withContext object : java.io.FilterInputStream(body.byteStream()) {
                        override fun close() {
                            super.close()
                            response.close()
                        }
                    }
                } catch (e: Exception) {
                    lastError = e
                    if (attempt == 0) {
                        kotlinx.coroutines.delay(250L)
                    }
                }
            }
            val finalErr = lastError ?: java.io.IOException("Unknown image fetch error for $imageUrl")
            MangaLogger.logImageFetch(context, mihonSource.name, -1, imageUrl,
                success = false, elapsedMs = System.currentTimeMillis() - imgT0, error = finalErr)
            throw finalErr
        }
    }

    // ── Dönüşüm Yardımcıları ──────────────────────────────────────────────────

    private fun SManga.toMangaDetails(): MangaDetails = MangaDetails(
        url = url,
        title = title,
        author = author,
        artist = artist,
        description = description,
        genre = getGenres() ?: emptyList(),
        thumbnailUrl = thumbnail_url,
        status = when (status) {
            SManga.ONGOING -> MangaStatus.Ongoing
            SManga.COMPLETED -> MangaStatus.Completed
            SManga.LICENSED -> MangaStatus.Licensed
            SManga.PUBLISHING_FINISHED -> MangaStatus.PublicationComplete
            SManga.CANCELLED -> MangaStatus.Cancelled
            SManga.ON_HIATUS -> MangaStatus.OnHiatus
            else -> MangaStatus.Unknown
        },
        source = name
    )

    private fun SChapter.toMangaChapter(mangaUrl: String, mangaTitle: String = ""): MangaChapter {
        // Kaynak geçerli bir chapter_number verdiyse onu kullan; aksi halde
        // (çoğu HTML kaynağı -1 döndürür) bölüm adından ChapterRecognition ile ayıkla.
        val resolvedNumber: Float = if (chapter_number >= 0f) {
            chapter_number
        } else {
            ChapterRecognition.parseChapterNumber(
                mangaTitle = mangaTitle,
                chapterName = name,
                chapterNumber = chapter_number.toDouble()
            ).toFloat()
        }
        return MangaChapter(
            url = url,
            name = name,
            chapterNumber = resolvedNumber,
            scanlator = scanlator,
            uploadDate = date_upload,
            mangaUrl = mangaUrl
        )
    }
}
