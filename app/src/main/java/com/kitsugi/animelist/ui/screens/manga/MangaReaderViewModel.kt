package com.kitsugi.animelist.ui.screens.manga

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.local.MangaChapterProgressEntity
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.manga.MangaChapter
import com.kitsugi.animelist.data.manga.MangaDetails
import com.kitsugi.animelist.data.manga.MangaPage
import com.kitsugi.animelist.data.manga.MangaSource
import com.kitsugi.animelist.data.manga.MangaSourceStateStore
import com.kitsugi.animelist.data.manga.ReadingMode
import com.kitsugi.animelist.data.manga.model.SourceHealthStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.kitsugi.animelist.data.manga.loader.MangaCache
import com.kitsugi.animelist.data.manga.loader.MangaPageLoaderV2
import com.kitsugi.animelist.data.manga.SourceFailureClassifier

import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.data.manga.ColorFilterType
import com.kitsugi.animelist.data.manga.MangaFitMode

/**
 * MangaReaderScreen için ViewModel.
 *
 * Sorumluluklar:
 * - Aktif bölümü ve sayfa listesini yönetmek
 * - Okuma modunu (LTR/RTL/Webtoon) kalıcı yapmak
 * - Her sayfa değişiminde Room'a ilerlemeyi kaydetmek
 * - Bir sonraki/önceki bölüme geçişi yönetmek
 */
class MangaReaderViewModel(
    private val context: Context,
    val source: MangaSource,
    val mangaDetails: MangaDetails,
    initialChapter: MangaChapter
) : ViewModel() {

    private val db  = KitsugiDatabase.getDatabase(context)
    private val dao = db.mangaChapterProgressDao()
    private val cache = MangaCache(context)
    private val settingsDataStore = SettingsDataStore(context)
    private val sourceStateStore = MangaSourceStateStore(context)
    val loader = MangaPageLoaderV2(context, source, cache)
    val pages: StateFlow<List<MangaPage>> = loader.pages

    // F5: Debounce için Job referansı — hızlı swipe'ta Room IO flood önler
    private var progressSaveJob: kotlinx.coroutines.Job? = null

    // ─── UI Durumu ────────────────────────────────────────────────────────────

    data class UiState(
        val currentChapter: MangaChapter,
        val chapterList: List<MangaChapter> = emptyList(),
        val currentPageIndex: Int = 0,
        val readingMode: ReadingMode = ReadingMode.RightToLeft,
        val colorFilterType: ColorFilterType = ColorFilterType.Normal,
        val fitMode: MangaFitMode = MangaFitMode.FitScreen,
        val customBrightness: Float = 1.0f,
        val isLoadingChapters: Boolean = false,
        val error: String? = null,
        val isLoadingPages: Boolean = false,
        val pagesError: String? = null
    )

    private val _uiState = MutableStateFlow(UiState(currentChapter = initialChapter))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadChapterList()
        restoreProgress(initialChapter)
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { settings ->
                val mode = when (settings.mangaReadingMode) {
                    "LeftToRight" -> ReadingMode.LeftToRight
                    "RightToLeft" -> ReadingMode.RightToLeft
                    "Vertical" -> ReadingMode.Vertical
                    "Webtoon" -> ReadingMode.Webtoon
                    else -> ReadingMode.RightToLeft
                }
                val filter = when (settings.mangaColorFilter) {
                    "Normal" -> ColorFilterType.Normal
                    "Grayscale" -> ColorFilterType.Grayscale
                    "Sepia" -> ColorFilterType.Sepia
                    "Invert" -> ColorFilterType.Invert
                    else -> ColorFilterType.Normal
                }
                val fit = when (settings.mangaFitMode) {
                    "FitScreen" -> MangaFitMode.FitScreen
                    "FitWidth" -> MangaFitMode.FitWidth
                    "FitHeight" -> MangaFitMode.FitHeight
                    else -> MangaFitMode.FitScreen
                }
                // R2: Webtoon modunda 3+1 keep, pager modunda 2+1 keep preload
                loader.preloadAhead = if (mode == ReadingMode.Webtoon) 3 else 2
                loader.keepBehind   = 1
                _uiState.update {
                    it.copy(
                        readingMode = mode,
                        colorFilterType = filter,
                        fitMode = fit,
                        customBrightness = settings.mangaBrightness
                    )
                }
            }
        }
    }

    // ─── Bölüm listesini yükle ────────────────────────────────────────────────

    private fun loadChapterList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChapters = true, error = null) }
            val startedAt = System.currentTimeMillis()
            try {
                val chapters = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    source.fetchChapterList(mangaDetails.url)
                }
                sourceStateStore.recordOperationSuccess(
                    source = source,
                    operation = "chapters",
                    elapsedMs = System.currentTimeMillis() - startedAt,
                )
                _uiState.update { it.copy(chapterList = chapters, isLoadingChapters = false) }
            } catch (e: Exception) {
                sourceStateStore.recordOperationFailure(
                    source = source,
                    operation = "chapters",
                    reason = e.message,
                    statusOverride = classifySourceStatus(e),
                    elapsedMs = System.currentTimeMillis() - startedAt,
                )
                _uiState.update { it.copy(isLoadingChapters = false, error = e.message) }
            }
        }
    }

    // ─── İlerleme kaydetme ───────────────────────────────────────────────────

    /**
     * Sayfa değiştikçe Room'a anlık olarak kaydeder.
     * Bölüm tamamlandıysa isCompleted = true olarak işaretler.
     */
    // F5: Progress debounce — 500ms bekle, hızlı swipe'ta tekrar tetiklenirse iptal et
    fun onPageChanged(chapterUrl: String, pageIndex: Int, totalPages: Int) {
        _uiState.update { it.copy(currentPageIndex = pageIndex) }
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500L)
            val isCompleted = totalPages > 0 && pageIndex >= totalPages - 1
            dao.upsert(
                MangaChapterProgressEntity(
                    chapterUrl    = chapterUrl,
                    mangaUrl      = mangaDetails.url,
                    chapterName   = _uiState.value.currentChapter.name,
                    lastPageIndex = pageIndex,
                    totalPages    = totalPages,
                    isCompleted   = isCompleted,
                    updatedAt     = System.currentTimeMillis()
                )
            )
        }
    }

    // ─── Okuma modu değiştirme ────────────────────────────────────────────────

    fun setReadingMode(mode: ReadingMode) {
        viewModelScope.launch {
            settingsDataStore.setMangaReadingMode(mode.name)
        }
    }

    fun setColorFilter(filter: ColorFilterType) {
        viewModelScope.launch {
            settingsDataStore.setMangaColorFilter(filter.name)
        }
    }

    fun setFitMode(fitMode: MangaFitMode) {
        viewModelScope.launch {
            settingsDataStore.setMangaFitMode(fitMode.name)
        }
    }

    fun setCustomBrightness(brightness: Float) {
        viewModelScope.launch {
            settingsDataStore.setMangaBrightness(brightness)
        }
    }

    // ─── Bölüm gezinme ───────────────────────────────────────────────────────

    // F6: Bölüm değişiminde eski queue'yu temizle — yanlış sayfa yükleme önler
    fun goToChapter(chapter: MangaChapter) {
        loader.resetQueue()
        _uiState.update { it.copy(currentChapter = chapter, currentPageIndex = 0) }
        restoreProgress(chapter)
    }

    fun goToNextChapter() {
        val list    = _uiState.value.chapterList
        val current = _uiState.value.currentChapter
        val idx     = list.indexOfFirst { it.url == current.url }
        if (idx > 0) goToChapter(list[idx - 1])   // Listede yeniden eskiye doğru
    }

    fun goToPreviousChapter() {
        val list    = _uiState.value.chapterList
        val current = _uiState.value.currentChapter
        val idx     = list.indexOfFirst { it.url == current.url }
        if (idx < list.size - 1) goToChapter(list[idx + 1])
    }

    val hasNextChapter: Boolean
        get() {
            val list = _uiState.value.chapterList
            val idx  = list.indexOfFirst { it.url == _uiState.value.currentChapter.url }
            return idx > 0
        }

    /** Sonraki bölümün adı (bölüm sonu geçiş kartında göstermek için); yoksa null. */
    val nextChapterName: String?
        get() {
            val list = _uiState.value.chapterList
            val idx  = list.indexOfFirst { it.url == _uiState.value.currentChapter.url }
            return if (idx > 0) list[idx - 1].name else null
        }

    val hasPreviousChapter: Boolean
        get() {
            val list = _uiState.value.chapterList
            val idx  = list.indexOfFirst { it.url == _uiState.value.currentChapter.url }
            return idx < list.size - 1
        }

    private fun restoreProgress(chapter: MangaChapter) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPages = true, pagesError = null) }
            val saved = dao.getProgress(chapter.url)
            val pageIndex = if (saved != null && !saved.isCompleted) saved.lastPageIndex else 0
            _uiState.update { it.copy(currentPageIndex = pageIndex) }
            try {
                loader.loadPageList(chapter)
                if (loader.pages.value.isEmpty()) {
                    _uiState.update { it.copy(isLoadingPages = false, pagesError = "Sayfalar yüklenemedi. İnternet bağlantınızı kontrol edip tekrar deneyin.") }
                } else {
                    _uiState.update { it.copy(isLoadingPages = false) }
                }
            } catch (e: com.kitsugi.animelist.data.manga.CaptchaRequiredException) {
                // F9: Captcha tespiti — kullanıcıya anlamlı mesaj göster
                _uiState.update {
                    it.copy(
                        isLoadingPages = false,
                        pagesError = "⚠️ ${e.sourceName}: Bu bölümü açmak için captcha doğrulaması gerekiyor. Tarayıcıda açmayı deneyin."
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingPages = false, pagesError = e.localizedMessage ?: e.message ?: "Sayfalar yüklenirken bir hata oluştu.") }
            }
        }
    }

    fun retryLoadPages() {
        restoreProgress(_uiState.value.currentChapter)
    }

    private fun classifySourceStatus(error: Throwable): SourceHealthStatus {
        return SourceFailureClassifier.classifyStatus(error)
    }

    override fun onCleared() {
        super.onCleared()
        loader.recycle()
    }

    // ─── Factory ─────────────────────────────────────────────────────────────

    class Factory(
        private val context: Context,
        private val source: MangaSource,
        private val mangaDetails: MangaDetails,
        private val initialChapter: MangaChapter
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MangaReaderViewModel(context, source, mangaDetails, initialChapter) as T
    }
}
