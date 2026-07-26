package com.kitsugi.animelist.ui.screens.fullscreen.runtime

import android.util.Log
import com.kitsugi.animelist.core.player.engine.PlayerEngine
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
 * Hata → retry (max 3x) → fallback source → kullanıcıya bildir zinciri.
 *
 * NuvioTV PlayerErrorRecoveryController referansından adapte edildi.
 */
class PlayerErrorRecoveryController(
    private val scope: CoroutineScope,
    private val onRetry: (attempt: Int) -> Unit,
    private val onFallback: () -> Unit,
    private val onFatal: (errorCode: Int, errorMsg: String) -> Unit
) {
    private val TAG = "ErrorRecovery"

    companion object {
        private const val MAX_RETRY = 3
        private const val RETRY_DELAY_MS = 1500L
    }

    private var retryCount = 0
    private val _isRecovering = MutableStateFlow(false)
    val isRecovering: StateFlow<Boolean> = _isRecovering.asStateFlow()

    /**
     * Hata bildirimi alındığında çağrılır.
     * @param errorCode PlayerEngine hata kodu
     * @param errorMsg Hata mesajı
     */
    fun onPlaybackError(errorCode: Int, errorMsg: String) {
        Log.w(TAG, "Playback error: code=$errorCode, msg=$errorMsg, retry=$retryCount, hasFallback=$hasFallback")

        val isNonRecoverableFormatOrHttp = errorCode == 403 ||
            errorMsg.contains("403") ||
            errorMsg.contains("404") ||
            errorMsg.contains("401") ||
            errorMsg.contains("UnrecognizedInputFormatException") ||
            errorMsg.contains("None of the available extractors") ||
            errorMsg.contains("Response code: 4") ||
            errorMsg.contains("Response code: 5")

        // 1. Non-recoverable failures (403, 404, 5xx, Format errors): Trigger immediate fallback source
        if (isNonRecoverableFormatOrHttp) {
            Log.e(TAG, "Non-recoverable failure ($errorCode / $errorMsg). hasFallback=$hasFallback — ${if (hasFallback) "trying fallback source" else "triggering fatal"}")
            retryCount = 0
            _isRecovering.value = false
            if (hasFallback) {
                onFallback()
            } else {
                onFatal(errorCode, "Yayın kaynağı okunamadı ($errorCode). Lütfen farklı bir kaynak seçin.")
            }
            return
        }

        // 2. Codec/Decoding failures (4000..4005): Trigger immediate fallback without retries
        if (errorCode in 4000..4005) {
            Log.w(TAG, "Codec/Decoding failure ($errorCode). Triggering immediate fallback.")
            retryCount = 0
            _isRecovering.value = false
            if (hasFallback) {
                onFallback()
            } else {
                onFatal(errorCode, "Codec/decoder hatası ($errorCode). Lütfen farklı bir kaynak deneyin.")
            }
            return
        }

        // 3. Network/Other failures: Trigger backoff retries up to MAX_RETRY times, then fallback
        if (retryCount < MAX_RETRY) {
            _isRecovering.value = true
            scope.launch {
                retryCount++
                delay(RETRY_DELAY_MS * retryCount) // Exponential-ish backoff
                Log.d(TAG, "Retrying... attempt $retryCount/$MAX_RETRY")
                onRetry(retryCount)
                _isRecovering.value = false
            }
        } else {
            // Max retry aşıldı → fallback source dene
            Log.w(TAG, "Max retries ($MAX_RETRY) reached. hasFallback=$hasFallback")
            _isRecovering.value = false
            retryCount = 0
            if (hasFallback) {
                onFallback()
            } else {
                onFatal(errorCode, "Bağlantı hatası. Tüm yeniden denemeler başarısız oldu.")
            }
        }
    }

    /** Oynatma başarılı olduğunda retry sayacını sıfırla */
    fun onPlaybackReady() {
        if (retryCount > 0) {
            Log.d(TAG, "Recovery successful after $retryCount retries")
            retryCount = 0
        }
        _isRecovering.value = false
    }

    /** Fallback kaynak mevcut mu (PlayerSourceController ile iletişim için) */
    var hasFallback: Boolean = false

    /** Controller durumunu sıfırla (yeni medya açıldığında) */
    fun reset() {
        retryCount = 0
        _isRecovering.value = false
    }
}
