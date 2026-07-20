package com.kitsugi.animelist.ui.screens.fullscreen.runtime

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kitsugi.animelist.core.player.PlayerLogger
import com.kitsugi.animelist.data.repository.AddonStreamRepository
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.data.settings.FrameRateMatchingMode
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.data.trailer.TrailerPlaybackSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * T1.6 – PlayerSourceController
 *
 * Stream kaynağı seçimi, kaynak değişimi ve hata-sonrası otomatik geçiş
 * mantığını yönetir.
 *
 * Oynatma durumu (videoUrl, headers, subtitles) ve kaynak listesi bu
 * controller'da tutulur; ViewModel bunları gözlemler.
 */
class PlayerSourceController(
    private val scope: CoroutineScope,
    private val context: Context,
    /** Yeni kaynak hazır olduğunda çağrılır → ViewModel playbackSource'u günceller. */
    private val onSourceReady: (url: String, audio: String?, headers: Map<String, String>, source: StreamSource, title: String) -> Unit,
    /** AFR preflight'ı tetiklemek için VM'e delege edilir. */
    private val onAfrRequired: (url: String, headers: Map<String, String>, mode: FrameRateMatchingMode, resolution: Boolean) -> Unit,
) {
    private val TAG = "PlayerSourceCtrl"
    private val maxAutoSwitchAttempts = 5

    // ── Compose state ─────────────────────────────────────────────────────────
    var isAutoSwitching by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    // ── Internal state ────────────────────────────────────────────────────────
    var currentStreamSources: List<StreamSource> = emptyList()
        private set

    var currentSourceIndex: Int = -1
        private set

    var currentAddonName: String? = null
        private set

    private var autoSwitchAttempts = 0

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Kaynak listesini günceller (yeni bölüm veya ilk yükleme).
     */
    fun updateSources(sources: List<StreamSource>, index: Int) {
        currentStreamSources = sources
        currentSourceIndex = index
        currentAddonName = sources.getOrNull(index)?.addonName
    }

    /**
     * Manuel kaynak değişimi (kullanıcı source picker'dan seçti).
     */
    fun changeSource(index: Int, stream: StreamSource, resolvedUrl: String, currentTitle: String) {
        PlayerLogger.logSourceChange(
            context   = context,
            fromAddon = currentAddonName,
            toAddon   = stream.addonName,
            newUrl    = resolvedUrl,
            title     = currentTitle
        )
        currentSourceIndex = index
        currentAddonName = stream.addonName
        onSourceReady(resolvedUrl, null, stream.requestHeaders ?: emptyMap(), stream, currentTitle)
        Log.d(TAG, "Manual source change → ${stream.addonName}")
    }

    /**
     * Oynatma hatası sonrası listedeki bir sonraki kaynağa otomatik geçer.
     *
     * @return true → geçiş başlatıldı, false → limit aşıldı / tek kaynak
     */
    fun tryNextSource(
        activity: android.app.Activity?,
        currentTitle: String,
        onSwitched: (newUrl: String, newSource: StreamSource) -> Unit
    ): Boolean {
        val sources = currentStreamSources
        if (sources.isEmpty() || autoSwitchAttempts >= maxAutoSwitchAttempts) {
            Log.w(TAG, "tryNextSource: limit aşıldı ($autoSwitchAttempts/$maxAutoSwitchAttempts)")
            return false
        }
        autoSwitchAttempts++
        val nextIndex = (currentSourceIndex + 1) % sources.size
        if (nextIndex == currentSourceIndex % sources.size && sources.size == 1) {
            Log.w(TAG, "tryNextSource: tek kaynak, geçiş yapılamıyor")
            return false
        }
        Log.d(TAG, "tryNextSource: $currentSourceIndex → $nextIndex ($autoSwitchAttempts/$maxAutoSwitchAttempts)")
        isAutoSwitching = true
        isLoading = true

        scope.launch {
            try {
                val target = sources[nextIndex]
                val repository = AddonStreamRepository(context)
                val resolvedUrl = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repository.resolveStreamUrl(target)
                }
                if (resolvedUrl != null) {
                    currentSourceIndex = nextIndex
                    currentAddonName = target.addonName

                    val settings = SettingsDataStore(context).settingsFlow.first()
                    onAfrRequired(resolvedUrl, target.requestHeaders ?: emptyMap(), settings.frameRateMatchingMode, settings.resolutionMatchingEnabled)
                    onSourceReady(resolvedUrl, null, target.requestHeaders ?: emptyMap(), target, currentTitle)
                    onSwitched(resolvedUrl, target)
                    Log.d(TAG, "Auto-switched to '${target.addonName}'")
                } else {
                    Log.w(TAG, "tryNextSource: kaynak çözümlenemedi, recursion...")
                    currentSourceIndex = nextIndex
                    tryNextSource(activity, currentTitle, onSwitched)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "tryNextSource hata: ${e.message}")
            } finally {
                isAutoSwitching = false
                isLoading = false
            }
        }
        return true
    }

    /**
     * Yeni bölüme geçildiğinde otomatik geçiş sayacını sıfırla.
     */
    fun resetAutoSwitch() {
        autoSwitchAttempts = 0
        isAutoSwitching = false
    }
}
