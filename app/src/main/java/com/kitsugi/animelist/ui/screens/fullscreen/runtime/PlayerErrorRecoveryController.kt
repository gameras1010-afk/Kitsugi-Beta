package com.kitsugi.animelist.ui.screens.fullscreen.runtime

import android.content.Context
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.kitsugi.animelist.core.player.PlayerManagerListener
import com.kitsugi.animelist.core.player.engine.PlayerEngineType
import com.kitsugi.animelist.core.player.engine.PlayerFallbackCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * T1.6: PlayerErrorRecoveryController
 *
 * Oynatma hatalarını yakalar ve fallback / retry mantığını yönetir.
 * Hata → engine fallback (MEDIA3→MPV) → source fallback → retry (max 3x) → kullanıcıya bildir.
 *
 * Engine switching ([PlayerFallbackCoordinator]) artık bu sınıfın içinde yönetilmektedir;
 * UI katmanındaki ad-hoc fallback koordinatörüne gerek kalmamaktadır.
 */
class PlayerErrorRecoveryController(
    private val scope: CoroutineScope,
    private val onRetry: (attempt: Int) -> Unit,
    private val onFallback: () -> Unit,
    private val onFatal: (errorCode: Int, errorMsg: String) -> Unit,
    /** Called when a fallback engine switch is required (e.g. MEDIA3 → MPV). */
    private val onSwitchEngine: ((to: PlayerEngineType) -> Unit)? = null,
    /** Lambda that provides the currently active engine type. */
    private val getCurrentEngine: (() -> PlayerEngineType)? = null,
    /** Whether the user has MPV enabled in settings. */
    private val isMpvEnabled: (() -> Boolean)? = null,
    /** Application context for showing toasts. */
    private val context: Context? = null
) {
    private val TAG = "ErrorRecovery"

    companion object {
        private const val MAX_RETRY = 3
        private const val RETRY_DELAY_MS = 1500L
    }

    private var retryCount = 0
    private val _isRecovering = MutableStateFlow(false)
    val isRecovering: StateFlow<Boolean> = _isRecovering.asStateFlow()

    /** Internal engine-level fallback coordinator */
    private val engineFallback = PlayerFallbackCoordinator(
        maxAttempts = 3,
        listener = object : PlayerManagerListener {
            override fun onPlayerSwitched(from: PlayerEngineType, to: PlayerEngineType) {
                Log.d(TAG, "Engine fallback: $from → $to")
                showToastOnMain("⚠️ Oynatma hatası — Oynatıcı motoru değiştiriliyor...")
                onSwitchEngine?.invoke(to)
            }
            override fun onFatalError(errorCode: Int, errorMsg: String) {
                Log.e(TAG, "Engine fallback exhausted: $errorMsg")
                // All engines tried — now fall through to source-level fallback
                triggerSourceFallbackOrFatal(errorCode, errorMsg)
            }
        }
    )

    /**
     * Hata bildirimi alındığında çağrılır.
     * Engine switching önce denenir, sonra kaynak değiştirme, sonra retry, sonra fatal.
     */
    fun onPlaybackError(errorCode: Int, errorMsg: String) {
        Log.w(TAG, "Playback error: code=$errorCode, msg=$errorMsg, retry=$retryCount, hasFallback=$hasFallback")

        val isNonRecoverable = errorCode == 403 ||
            errorMsg.contains("403") ||
            errorMsg.contains("404") ||
            errorMsg.contains("401") ||
            errorMsg.contains("UnrecognizedInputFormatException") ||
            errorMsg.contains("None of the available extractors") ||
            errorMsg.contains("Response code: 4") ||
            errorMsg.contains("Response code: 5")

        val isCodecFailure = errorCode in 4000..4005

        // 1. Codec or non-recoverable failures: skip engine fallback, go straight to source fallback
        if (isNonRecoverable || isCodecFailure) {
            Log.e(TAG, "Non-recoverable/codec failure ($errorCode). Going to source fallback.")
            retryCount = 0
            _isRecovering.value = false
            triggerSourceFallbackOrFatal(errorCode, errorMsg)
            return
        }

        // 2. Try engine-level fallback first (MEDIA3 → MPV)
        val currentEngine = getCurrentEngine?.invoke() ?: PlayerEngineType.MEDIA3
        val mpvEnabled = isMpvEnabled?.invoke() ?: false
        val nextEngine = engineFallback.getFallbackEngine(
            currentEngine = currentEngine,
            errorCode = errorCode,
            mpvEnabled = mpvEnabled
        )

        if (nextEngine != null) {
            // Engine switch is being triggered — wait for the new engine to pick up
            Log.d(TAG, "Switching to engine $nextEngine — deferring source fallback")
            retryCount = 0
            _isRecovering.value = false
            return
        }
        // nextEngine == null means engineFallback.onFatalError was called → handled by listener
    }

    private fun triggerSourceFallbackOrFatal(errorCode: Int, errorMsg: String) {
        if (hasFallback) {
            onFallback()
        } else {
            onFatal(errorCode, "Yayın kaynağı okunamadı ($errorCode). Lütfen farklı bir kaynak seçin.")
        }
    }

    /** Oynatma başarılı olduğunda retry sayacını ve engine fallback durumunu sıfırla */
    fun onPlaybackReady() {
        if (retryCount > 0) {
            Log.d(TAG, "Recovery successful after $retryCount retries")
            retryCount = 0
        }
        _isRecovering.value = false
    }

    /** Fallback kaynak mevcut mu (PlayerSourceController ile iletişim için) */
    var hasFallback: Boolean = false

    /** Controller durumunu ve engine fallback sayacını sıfırla (yeni medya açıldığında) */
    fun reset() {
        retryCount = 0
        _isRecovering.value = false
        engineFallback.reset()
    }

    private fun showToastOnMain(msg: String) {
        val ctx = context ?: return
        val run = Runnable { Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
        if (Looper.myLooper() == Looper.getMainLooper()) run.run()
        else android.os.Handler(Looper.getMainLooper()).post(run)
    }
}
