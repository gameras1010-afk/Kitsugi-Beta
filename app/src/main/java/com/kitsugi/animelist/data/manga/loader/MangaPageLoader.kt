package com.kitsugi.animelist.data.manga.loader

import android.content.Context
import android.util.Log
import com.kitsugi.animelist.data.manga.MangaPage
import com.kitsugi.animelist.data.manga.MangaPageStatus
import com.kitsugi.animelist.data.manga.MangaSource
import com.kitsugi.animelist.data.manga.MangaSourceStateStore
import com.kitsugi.animelist.data.manga.model.SourceHealthStatus
import com.kitsugi.animelist.data.manga.MangaLogger
import com.kitsugi.animelist.data.manga.SourceFailureClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import kotlin.math.min

/**
 * Manga sayfalarını öncelik kuyruğu (PriorityBlockingQueue) ile
 * yükleyen ve önbellekleyen sınıf.
 *
 * — Aktif sayfa yüksek öncelikle yüklenir.
 * — Sonraki sayfalar arka planda önceden indirilir.
 * — Kaynak başarısı / başarısızlığı source state store'a işlenir.
 *
 * Mihon/Aniyomi HttpPageLoader mantığından esinlenilmiş, ancak
 * Kitsugi için çoklu worker + source health kaydı eklenmiştir.
 */
class MangaPageLoader(
    private val context: Context,
    private val source: MangaSource,
    val cache: MangaCache,
    // F16: Okuma moduna göre dinamik preload miktarı — Webtoon=8, Pager=3
    var preloadCount: Int = 4,
) {
    private val tag = "MangaPageLoader"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = PriorityBlockingQueue<PriorityPage>()
    private val scheduledPageIds = ConcurrentHashMap.newKeySet<Int>()
    private val imageMutexes = ConcurrentHashMap<String, Mutex>()
    private val sourceStateStore = MangaSourceStateStore(context)

    companion object {
        private const val WORKER_COUNT = 3     // F8-R2: 4 → 3 (Cloudflare'li TR siteler için daha kararlı)
        private const val MAX_QUEUE_SIZE = 30
        private const val IMAGE_TIMEOUT_MS = 30_000L
    }

    init {
        repeat(WORKER_COUNT) { workerIndex ->
            scope.launch {
                while (true) {
                    val priorityPage = runInterruptible { queue.take() }
                    if (priorityPage.page.status == MangaPageStatus.Queue) {
                        internalLoad(priorityPage.page, workerIndex)
                    }
                }
            }
        }
    }

    private val _pages = MutableStateFlow<List<MangaPage>>(emptyList())
    val pages: StateFlow<List<MangaPage>> = _pages.asStateFlow()

    /**
     * Compose'un yeniden çizim yapması için liste referansını değiştir.
     * ÖNEMLI: it.copy() YAPMA — yeni nesneler oluşturur ve worker'ların
     * elindeki eski referansların status/imageUrl güncellemelerini görmesini engeller.
     * toList() ile aynı MangaPage nesnelerini tutan yeni bir liste oluşturulur.
     */
    private fun notifyPagesChanged() {
        _pages.value = _pages.value.toList()
    }

    suspend fun loadPageList(chapter: com.kitsugi.animelist.data.manga.MangaChapter) =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            val startedAt = System.currentTimeMillis()
            try {
                val fetched = source.fetchPageList(chapter)
                _pages.value = fetched
                val elapsed = System.currentTimeMillis() - startedAt
                sourceStateStore.recordOperationSuccess(
                    source = source,
                    operation = "pages",
                    elapsedMs = elapsed,
                )
                MangaLogger.logPageList(context, source.name, chapter.name, success = true,
                    pageCount = fetched.size, elapsedMs = elapsed)
                Log.d(tag, "${chapter.name}: ${fetched.size} sayfa alındı")
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - startedAt
                sourceStateStore.recordOperationFailure(
                    source = source,
                    operation = "pages",
                    reason = e.message,
                    statusOverride = classifySourceStatus(e),
                    elapsedMs = elapsed,
                )
                MangaLogger.logPageList(context, source.name, chapter.name, success = false,
                    elapsedMs = elapsed, error = e)
                Log.e(tag, "${chapter.name}: sayfa listesi alınamadı -> ${e.message}", e)
                _pages.value = emptyList()
            }
        }

    fun loadPage(page: MangaPage) {
        val cacheKey = page.imageUrl?.takeIf { it.isNotBlank() } ?: page.url.takeIf { it.isNotBlank() }
        if (page.status == MangaPageStatus.Ready && cacheKey != null && cache.isImageInCache(cacheKey)) return

        var changed = false
        if (page.status == MangaPageStatus.Error) {
            page.status = MangaPageStatus.Queue
            changed = true
        }

        if (page.status == MangaPageStatus.Queue && schedule(page, priority = 2)) {
            changed = true
        }

        if (changed) notifyPagesChanged()
        // F16: preloadCount instance parametresini kullan
        preloadNext(page, preloadCount)
    }

    fun retryPage(page: MangaPage) {
        page.status = MangaPageStatus.Queue
        notifyPagesChanged()
        schedule(page, priority = 2)
    }

    /**
     * F6: Bölüm değişiminde çağrılır.
     * Kuyruğu ve scheduledPageIds setini temizler.
     * Worker'lar durdurmaz — kuyruğu boşaltmak yeterli.
     * _pages state'ini sıfırlamaz — restoreProgress() bunu halleder.
     */
    fun resetQueue() {
        queue.clear()
        scheduledPageIds.clear()
        Log.d(tag, "Queue sıfırlandı (bölüm değişimi)")
    }

    fun recycle() {
        scope.cancel()
        queue.clear()
        scheduledPageIds.clear()
        imageMutexes.clear()
        Log.d(tag, "PageLoader kapatıldı")
    }

    private suspend fun internalLoad(page: MangaPage, workerIndex: Int) {
        val startedAt = System.currentTimeMillis()
        try {
            kotlinx.coroutines.withTimeout(IMAGE_TIMEOUT_MS) {
                if (page.imageUrl.isNullOrEmpty()) {
                    page.status = MangaPageStatus.LoadPage
                    notifyPagesChanged()
                    page.imageUrl = source.fetchImageUrl(page)
                }

                val imageUrl = page.imageUrl!!
                val imageMutex = imageMutexes.getOrPut(imageUrl) { Mutex() }
                imageMutex.withLock {
                    if (!cache.isImageInCache(imageUrl)) {
                        page.status = MangaPageStatus.DownloadImage
                        notifyPagesChanged()
                        source.getImage(page).use { stream ->
                            cache.putImageToCache(imageUrl, stream)
                        }
                    }
                }

                page.stream = { cache.getImageFile(imageUrl).inputStream() }
                page.status = MangaPageStatus.Ready
                notifyPagesChanged()
                sourceStateStore.recordOperationSuccess(
                    source = source,
                    operation = "image",
                    elapsedMs = System.currentTimeMillis() - startedAt,
                )
                Log.v(tag, "Sayfa hazır [${page.index}] (worker=$workerIndex): $imageUrl")
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            page.status = MangaPageStatus.Error
            notifyPagesChanged()
            MangaLogger.logImageFetch(context, source.name, page.index,
                page.imageUrl, success = false,
                elapsedMs = IMAGE_TIMEOUT_MS, isTimeout = true)
            Log.e(tag, "Sayfa timeout [${page.index}] (worker=$workerIndex) — ${IMAGE_TIMEOUT_MS}ms aşıldı")
        } catch (e: Exception) {
            page.status = MangaPageStatus.Error
            notifyPagesChanged()
            val elapsed = System.currentTimeMillis() - startedAt
            sourceStateStore.recordOperationFailure(
                source = source,
                operation = "image",
                reason = e.message,
                statusOverride = classifySourceStatus(e),
                elapsedMs = elapsed,
            )
            MangaLogger.logImageFetch(context, source.name, page.index,
                page.imageUrl, success = false, elapsedMs = elapsed, error = e)
            Log.e(tag, "Sayfa yükleme hatası [${page.index}] (worker=$workerIndex): ${e.message}")
        } finally {
            scheduledPageIds.remove(pageKey(page))
        }
    }

    private fun preloadNext(currentPage: MangaPage, amount: Int) {
        val allPages = _pages.value
        val from = currentPage.index + 1
        val to = min(from + amount, allPages.size)
        allPages.subList(from, to).forEach { p ->
            if (p.status == MangaPageStatus.Queue) {
                schedule(p, priority = 0)
            }
        }
    }

    private fun schedule(page: MangaPage, priority: Int): Boolean {
        if (priority <= 0 && queue.size >= MAX_QUEUE_SIZE) return false
        val key = pageKey(page)
        if (!scheduledPageIds.add(key)) return false
        queue.offer(PriorityPage(page, priority = priority))
        return true
    }

    private fun pageKey(page: MangaPage): Int = System.identityHashCode(page)

    private fun classifySourceStatus(error: Throwable): SourceHealthStatus {
        return SourceFailureClassifier.classifyStatus(error)
    }

    private class PriorityPage(
        val page: MangaPage,
        val priority: Int,
    ) : Comparable<PriorityPage> {

        /**
         * Sıralama:
         *  1. Yüksek öncelik önce gelir (priority büyük → küçük).
         *  2. Öncelikler eşitse, sayfa indeksi küçük olan önce gelir (sıralı yükleme).
         */
        override fun compareTo(other: PriorityPage): Int {
            val p = other.priority.compareTo(priority)   // yüksek öncelik önce
            return if (p != 0) p else page.index.compareTo(other.page.index)  // küçük indeks önce
        }
    }
}
