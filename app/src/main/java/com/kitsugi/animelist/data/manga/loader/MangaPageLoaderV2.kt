package com.kitsugi.animelist.data.manga.loader

import android.content.Context
import android.util.Log
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.kitsugi.animelist.data.manga.MangaPage
import com.kitsugi.animelist.data.manga.MangaPageStatus
import com.kitsugi.animelist.data.manga.MangaSource
import com.kitsugi.animelist.data.manga.MangaSourceStateStore
import com.kitsugi.animelist.data.manga.MangaLogger
import com.kitsugi.animelist.data.manga.SourceFailureClassifier
import com.kitsugi.animelist.data.manga.model.SourceHealthStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

/**
 * MangaPageLoaderV2 — 3 ileri prefetch + paralel 4 worker + Coil disk cache entegrasyonu.
 *
 * Eski MangaPageLoader'a göre farklar:
 * — `onPageChanged(index)` çağrıldığında: +1, +2, +3 ilerideki sayfalar önceden yüklenir
 * — Aynı anda max 4 paralel Coil request (Semaphore(4))
 * — Coil memory + disk cache'e yazıyor → chapter yeniden açılınca anında hazır
 * — Keep-alive penceresi: -1 .. +3 (pencere dışı işler iptal edilir)
 * — Bölüm değişiminde tüm devam eden prefetch'ler iptal edilir
 *
 * Kullanım (MangaReaderScreen'de):
 * ```kotlin
 * val pageLoader = remember { MangaPageLoaderV2(context, source, imageLoader) }
 * LaunchedEffect(currentPage) { pageLoader.onPageChanged(currentPage) }
 * ```
 */
class MangaPageLoaderV2(
    private val context: Context,
    private val source: MangaSource,
    val cache: MangaCache,
    private val imageLoader: ImageLoader = coil3.SingletonImageLoader.get(context),
    var preloadAhead: Int = 3,
    var keepBehind: Int = 1,
) {
    companion object {
        private const val TAG = "MangaPageLoaderV2"
        private const val PARALLEL_LIMIT = 4
        private const val IMAGE_TIMEOUT_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val semaphore = Semaphore(PARALLEL_LIMIT)
    private val sourceStateStore = MangaSourceStateStore(context)

    // pageIndex → aktif prefetch job
    private val prefetchJobs = ConcurrentHashMap<Int, Job>()

    private val _pages = MutableStateFlow<List<MangaPage>>(emptyList())
    val pages: StateFlow<List<MangaPage>> = _pages.asStateFlow()

    // ─── Sayfa listesi yükleme ────────────────────────────────────────────────

    suspend fun loadPageList(chapter: com.kitsugi.animelist.data.manga.MangaChapter) =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            val t0 = System.currentTimeMillis()
            try {
                val fetched = source.fetchPageList(chapter)
                _pages.value = fetched
                val elapsed = System.currentTimeMillis() - t0
                sourceStateStore.recordOperationSuccess(source, "pages", elapsed)
                MangaLogger.logPageList(context, source.name, chapter.name,
                    success = true, pageCount = fetched.size, elapsedMs = elapsed)
                Log.d(TAG, "${chapter.name}: ${fetched.size} sayfa alındı (${elapsed}ms)")
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - t0
                sourceStateStore.recordOperationFailure(source, "pages",
                    reason = e.message, statusOverride = classifyStatus(e), elapsedMs = elapsed)
                MangaLogger.logPageList(context, source.name, chapter.name,
                    success = false, elapsedMs = elapsed, error = e)
                Log.e(TAG, "${chapter.name}: sayfa listesi alınamadı → ${e.message}", e)
                _pages.value = emptyList()
            }
        }

    // ─── Ana giriş noktası: sayfa değişince çağrılır ─────────────────────────

    /**
     * Okuyucu mevcut sayfayı değiştirince bu fonksiyon çağrılmalıdır.
     * Keep-alive penceresi: [currentIndex - keepBehind .. currentIndex + preloadAhead]
     * Bu pencere dışındaki tüm devam eden prefetch'ler iptal edilir.
     */
    fun onPageChanged(currentIndex: Int) {
        val allPages = _pages.value
        if (allPages.isEmpty()) return

        val windowStart = (currentIndex - keepBehind).coerceAtLeast(0)
        val windowEnd   = (currentIndex + preloadAhead).coerceAtMost(allPages.size - 1)

        // Pencere dışı işleri iptal et
        val outsideKeys = prefetchJobs.keys.filter { it < windowStart || it > windowEnd }
        outsideKeys.forEach { prefetchJobs.remove(it)?.cancel() }

        // Pencere içindeki sayfaları öncelikli sırayla prefetch et
        // Önce currentIndex, sonra +1, +2, +3, sonra -1
        val priority = buildList {
            add(currentIndex)
            for (i in 1..preloadAhead) {
                val next = currentIndex + i
                if (next <= windowEnd) add(next)
            }
            val prev = currentIndex - keepBehind
            if (prev >= windowStart) add(prev)
        }

        priority.forEach { idx ->
            val page = allPages.getOrNull(idx) ?: return@forEach
            if (page.status != MangaPageStatus.Ready && !prefetchJobs.containsKey(idx)) {
                prefetchJobs[idx] = scope.launch {
                    try {
                        loadPageInternal(page)
                    } finally {
                        prefetchJobs.remove(idx)
                    }
                }
            }
        }
    }

    /** Tek sayfa yükleme — retry ve manuel tetikleme için */
    fun loadPage(page: MangaPage) {
        if (page.status == MangaPageStatus.Ready) return
        if (prefetchJobs.containsKey(page.index)) return

        prefetchJobs[page.index] = scope.launch {
            try {
                loadPageInternal(page)
            } finally {
                prefetchJobs.remove(page.index)
            }
        }
    }

    fun retryPage(page: MangaPage) {
        prefetchJobs.remove(page.index)?.cancel()
        page.status = MangaPageStatus.Queue
        notifyChanged()
        loadPage(page)
    }

    // ─── İç yükleme mantığı ───────────────────────────────────────────────────

    private suspend fun loadPageInternal(page: MangaPage) {
        if (page.status == MangaPageStatus.Ready) return

        semaphore.withPermit {
            val t0 = System.currentTimeMillis()
            try {
                kotlinx.coroutines.withTimeout(IMAGE_TIMEOUT_MS) {
                    // 1. Sayfa URL'ini çöz (gerekiyorsa)
                    if (page.imageUrl.isNullOrEmpty()) {
                        page.status = MangaPageStatus.LoadPage
                        notifyChanged()
                        page.imageUrl = source.fetchImageUrl(page)
                    }

                    val imageUrl = page.imageUrl ?: return@withTimeout

                    // 2. Disk cache'te var mı kontrol et
                    if (cache.isImageInCache(imageUrl)) {
                        page.stream = { cache.getImageFile(imageUrl).inputStream() }
                        page.status = MangaPageStatus.Ready
                        notifyChanged()
                        Log.v(TAG, "Cache hit [${page.index}]: $imageUrl")
                        return@withTimeout
                    }

                    // 3. Coil ile indir (memory + disk cache'e yazar)
                    page.status = MangaPageStatus.DownloadImage
                    notifyChanged()

                    val coilRequest = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .build()

                    val result = imageLoader.execute(coilRequest)
                    if (result is SuccessResult) {
                        // Coil kendi cache'ine yazdı; ayrıca MangaCache'e de yaz
                        try {
                            source.getImage(page).use { stream ->
                                cache.putImageToCache(imageUrl, stream)
                            }
                        } catch (_: Exception) {
                            // getImage opsiyonel — Coil cache yeterli
                        }
                        page.stream = {
                            val file = cache.getImageFile(imageUrl)
                            if (file.exists()) {
                                file.inputStream()
                            } else {
                                throw java.io.IOException("Cache file not found for $imageUrl")
                            }
                        }
                        page.status = MangaPageStatus.Ready
                        notifyChanged()

                        val elapsed = System.currentTimeMillis() - t0
                        sourceStateStore.recordOperationSuccess(source, "image", elapsed)
                        Log.v(TAG, "Sayfa hazır [${page.index}] (${elapsed}ms): $imageUrl")
                    } else {
                        throw Exception("Coil yükleme başarısız: ${result.javaClass.simpleName}")
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                page.status = MangaPageStatus.Error
                notifyChanged()
                MangaLogger.logImageFetch(context, source.name, page.index,
                    page.imageUrl, success = false, elapsedMs = IMAGE_TIMEOUT_MS, isTimeout = true)
                Log.e(TAG, "Timeout [${page.index}] — ${IMAGE_TIMEOUT_MS}ms aşıldı")
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Prefetch penceresi daraldı → job iptal edildi → normal
                Log.v(TAG, "Prefetch iptal edildi [${page.index}]")
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - t0
                page.status = MangaPageStatus.Error
                notifyChanged()
                sourceStateStore.recordOperationFailure(source, "image",
                    reason = e.message, statusOverride = classifyStatus(e), elapsedMs = elapsed)
                MangaLogger.logImageFetch(context, source.name, page.index,
                    page.imageUrl, success = false, elapsedMs = elapsed, error = e)
                Log.e(TAG, "Sayfa hatası [${page.index}]: ${e.message}")
            }
        }
    }

    // ─── Yardımcı fonksiyonlar ────────────────────────────────────────────────

    private fun notifyChanged() {
        _pages.value = _pages.value.toList()
    }

    fun resetQueue() {
        prefetchJobs.values.forEach { it.cancel() }
        prefetchJobs.clear()
        Log.d(TAG, "Queue sıfırlandı (bölüm değişimi)")
    }

    fun recycle() {
        scope.cancel()
        prefetchJobs.clear()
        Log.d(TAG, "MangaPageLoaderV2 kapatıldı")
    }

    private fun classifyStatus(error: Throwable): SourceHealthStatus =
        SourceFailureClassifier.classifyStatus(error)
}
