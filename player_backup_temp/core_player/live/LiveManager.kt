package com.kitsugi.animelist.core.player.live

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * T1.15 – LiveManager (Feature flag: DEFAULT OFF)
 *
 * Canlı yayın oturumunu yönetir:
 * - DVR penceresi konumunu takip eder
 * - "Canlıya Geri Dön" butonunun görünürlüğünü yönetir
 * - Live edge ile mevcut pozisyon arasındaki gecikmeyi hesaplar
 *
 * **Kullanım:** `AppSettings.liveHelperEnabled == true` ise KitsugiPlayerViewModel
 * bu sınıfı başlatır; false ise no-op (hiç kullanılmaz).
 */
class LiveManager(private val scope: CoroutineScope) {

    private val TAG = "LiveManager"

    // ── State ──────────────────────────────────────────────────────────────────

    /** Anlık oynatma pozisyonu (ms) */
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    /** Stream toplam süresi / DVR penceresi uzunluğu (ms) */
    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    /** "Canlıya Geri Dön" butonu görünür mü? */
    private val _showGoToLive = MutableStateFlow(false)
    val showGoToLive: StateFlow<Boolean> = _showGoToLive.asStateFlow()

    /** Live edge'den geri kalma süresi (ms) */
    private val _behindLiveMs = MutableStateFlow(0L)
    val behindLiveMs: StateFlow<Long> = _behindLiveMs.asStateFlow()

    private var monitorJob: Job? = null

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Live session'ı başlatır ve her saniye pozisyon günceller.
     *
     * @param getPositionMs  Anlık oynatma pozisyonunu dönen lambda
     * @param getDurationMs  Stream toplam süresini dönen lambda
     * @param goToLiveThresholdMs Bu sınırın gerisindeyse "Canlıya Dön" gösterilir (default 30s)
     */
    fun start(
        getPositionMs: () -> Long,
        getDurationMs: () -> Long,
        goToLiveThresholdMs: Long = 30_000L
    ) {
        Log.d(TAG, "LiveManager started")
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (true) {
                val pos = getPositionMs()
                val dur = getDurationMs()
                _positionMs.value = pos
                _durationMs.value = dur

                val behind = if (dur > 0L) (dur - pos).coerceAtLeast(0L) else 0L
                _behindLiveMs.value = behind
                _showGoToLive.value = LiveHelper.shouldShowGoToLive(pos, dur, goToLiveThresholdMs)

                delay(1_000L)
            }
        }
    }

    /**
     * Live session'ı durdurur.
     */
    fun stop() {
        Log.d(TAG, "LiveManager stopped")
        monitorJob?.cancel()
        monitorJob = null
    }

    /**
     * Kullanıcı "Canlıya Geri Dön" butonuna bastı.
     * Dönen değer = hedef pozisyon (ms) → engine.seekTo() ile kullanılır.
     */
    fun onGoToLiveClicked(): Long {
        val dur = _durationMs.value
        val maxPos = LiveHelper.maxSeekPositionMs(dur)
        Log.d(TAG, "GoToLive: seeking to ${maxPos}ms (duration=${dur}ms)")
        return maxPos
    }
}
