package com.kitsugi.animelist.ui.screens.fullscreen.runtime

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.data.settings.FrameRateMatchingMode

/**
 * T1.6 – PlayerRuntimeOrchestrator
 *
 * KitsugiPlayerViewModel'in tüm runtime controller'larını bir arada tutan
 * üst düzey kapsayıcı. Her controller tek bir sorumluluğa sahiptir:
 *
 * - [stillWatching]  → StillWatching prompt + binge session kuralları
 * - [audio]          → Ses rotası tespiti + route-bazlı gecikme uygulama
 * - [skip]           → AniSkip + AnimeSkip zaman damgası yükleme
 * - [source]         → Kaynak listesi, manuel / otomatik kaynak değiştirme
 *
 * ViewModel bu orchestrator'ı init bloğunda oluşturur ve tüm
 * controller state'lerini doğrudan orchestrator üzerinden okur.
 */
class PlayerRuntimeOrchestrator(
    private val scope: CoroutineScope,
    private val context: Context,
    /** Yeni kaynak hazır olduğunda VM'e bildirim gönderilir. */
    onSourceReady: (url: String, audio: String?, headers: Map<String, String>, source: StreamSource, title: String) -> Unit,
    /** AFR preflight tetiklemesi için VM'e delege edilir. */
    onAfrRequired: (url: String, headers: Map<String, String>, mode: FrameRateMatchingMode, resolution: Boolean) -> Unit,
    getEngine: () -> com.kitsugi.animelist.core.player.engine.PlayerEngine? = { null },
    getAniListToken: () -> String? = { null },
    onRetry: (attempt: Int) -> Unit = {},
    onFallback: () -> Unit = {},
    onFatal: (errorCode: Int, errorMsg: String) -> Unit = { _, _ -> },
    onAutoPlayNext: () -> Unit = {},
    onLoop: () -> Unit = {},
    onShowStillWatching: () -> Unit = {},
    onShowEndPrompt: () -> Unit = {},
    onCountdownTick: (remaining: Int) -> Unit = {}
) {
    private val TAG = "PlayerOrchestrator"

    // ── Controller instances ───────────────────────────────────────────────────

    val stillWatching: PlayerStillWatchingController =
        PlayerStillWatchingController(scope)

    val audio: PlayerAudioController =
        PlayerAudioController(scope, context)

    val skip: PlayerSkipController =
        PlayerSkipController(scope, context)

    val source: PlayerSourceController =
        PlayerSourceController(scope, context, onSourceReady, onAfrRequired)

    val track: PlayerTrackController =
        PlayerTrackController(scope, getEngine)

    val errorRecovery: PlayerErrorRecoveryController =
        PlayerErrorRecoveryController(scope, onRetry, onFallback, onFatal)

    val scrobble: PlayerScrobbleController =
        PlayerScrobbleController(scope, getAniListToken)

    val autoplay: PlayerAutoplayController =
        PlayerAutoplayController(
            scope = scope,
            onAutoPlayNext = onAutoPlayNext,
            onLoop = onLoop,
            onShowStillWatching = onShowStillWatching,
            onShowEndPrompt = onShowEndPrompt,
            onCountdownTick = onCountdownTick
        )

    val live: com.kitsugi.animelist.core.player.live.LiveManager =
        com.kitsugi.animelist.core.player.live.LiveManager(scope)

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Orchestrator'ı başlatır.
     * VM.init { } bloğunda bir kez çağrılır.
     *
     * @param getActiveEngine Aktif PlayerEngine'e erişim lambda'sı (audio delay uygulaması için)
     * @param liveHelperEnabled Canlı yayın desteğinin aktif olup olmadığı
     */
    fun start(
        getActiveEngine: () -> com.kitsugi.animelist.core.player.engine.PlayerEngine?,
        liveHelperEnabled: Boolean = false
    ) {
        Log.d(TAG, "Starting all runtime controllers")
        skip.observeSettings()
        audio.startObserving(getActiveEngine)
        if (liveHelperEnabled) {
            live.start(
                getPositionMs = { getActiveEngine()?.currentPosition ?: 0L },
                getDurationMs = { getActiveEngine()?.duration ?: 0L }
            )
        }
    }

    /**
     * Orchestrator'ı durdurur.
     * VM.onCleared() içinde çağrılır.
     */
    fun stop() {
        Log.d(TAG, "Stopping all runtime controllers")
        audio.stopObserving()
        scrobble.clear()
        autoplay.cancelCountdown()
        live.stop()
    }
}
