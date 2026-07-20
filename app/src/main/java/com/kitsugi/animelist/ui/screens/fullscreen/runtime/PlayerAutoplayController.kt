package com.kitsugi.animelist.ui.screens.fullscreen.runtime

import android.util.Log
import com.kitsugi.animelist.core.player.PlayerAutoplaySessionRules
import com.kitsugi.animelist.core.player.PlayerNextEpisodeRules
import com.kitsugi.animelist.core.player.PostPlayMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * T1.6: PlayerAutoplayController
 *
 * Bölüm tamamlandığında ne yapılacağını belirler:
 *  - PostPlayMode'a göre geri sayım + otomatik geçiş (AUTO_PLAY_NEXT)
 *  - BINGE_PROMPT: X bölüm sonunda stillWatching trigger
 *  - MANUAL: kullanıcı elle atlar
 *  - LOOP: aynı bölümü tekrar başlat
 *
 * PlayerStillWatchingController ile koordineli çalışır.
 */
class PlayerAutoplayController(
    private val scope: CoroutineScope,
    private val sessionRules: PlayerAutoplaySessionRules = PlayerAutoplaySessionRules(),
    private val onAutoPlayNext: () -> Unit,
    private val onLoop: () -> Unit,
    private val onShowStillWatching: () -> Unit,
    private val onShowEndPrompt: () -> Unit,
    private val onCountdownTick: (remaining: Int) -> Unit
) {
    private val TAG = "AutoplayController"

    private var countdownJob: Job? = null

    private val _isCountingDown = MutableStateFlow(false)
    val isCountingDown: StateFlow<Boolean> = _isCountingDown.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(0)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    /**
     * Bölüm sona erdiğinde çağrılır.
     * @param postPlayMode Ayardaki mod
     * @param hasNextEpisode Sonraki bölüm var mı?
     * @param durationMs Toplam bölüm süresi (ms)
     * @param positionMs Anlık oynatma pozisyonu (ms)
     * @param hasOutroSkip Outro/Credits skip bilgisi var mı?
     * @param outroStartSec Outro başlangıcı (saniye), null = bilinmiyor
     */
    fun onEpisodeEnded(
        postPlayMode: PostPlayMode,
        isAutoplaySettingEnabled: Boolean,
        hasNextEpisode: Boolean,
        durationMs: Long,
        positionMs: Long,
        hasOutroSkip: Boolean = false,
        outroStartSec: Long? = null
    ) {
        Log.d(TAG, "Episode ended. mode=$postPlayMode autoplaySetting=$isAutoplaySettingEnabled hasNext=$hasNextEpisode")

        if (!hasNextEpisode) {
            Log.d(TAG, "No next episode, stopping autoplay")
            return
        }

        if (postPlayMode == PostPlayMode.LOOP) {
            onLoop()
            return
        }

        val sessionLimitReached = sessionRules.onEpisodeCompleted()
        val isAutoPlayEnabled = isAutoplaySettingEnabled && 
            (postPlayMode == PostPlayMode.AUTO_PLAY_NEXT || postPlayMode == PostPlayMode.BINGE_PROMPT)

        val action = PlayerNextEpisodeRules.evaluate(
            durationMs = durationMs,
            positionMs = positionMs,
            thresholdSec = 30,
            hasOutroSkip = hasOutroSkip,
            outroStartSec = outroStartSec,
            isAutoPlayEnabled = isAutoPlayEnabled,
            sessionLimitReached = sessionLimitReached
        )

        Log.d(TAG, "Evaluated NextEpisodeAction: $action")

        when (action) {
            is com.kitsugi.animelist.core.player.NextEpisodeAction.None -> {
                // Do nothing
            }
            is com.kitsugi.animelist.core.player.NextEpisodeAction.ShowNextButton -> {
                onShowEndPrompt()
            }
            is com.kitsugi.animelist.core.player.NextEpisodeAction.ShowStillWatchingPrompt -> {
                sessionRules.resetSession()
                onShowStillWatching()
            }
            is com.kitsugi.animelist.core.player.NextEpisodeAction.AutoPlayImmediate -> {
                onAutoPlayNext()
            }
            is com.kitsugi.animelist.core.player.NextEpisodeAction.AutoPlayWithCountdown -> {
                startCountdown(action.countdownSec)
            }
            is com.kitsugi.animelist.core.player.NextEpisodeAction.ShowBingeCard -> {
                startCountdown(action.countdownSec)
            }
        }
    }

    /** Geri sayım başlat, sonunda auto-next */
    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        _isCountingDown.value = true
        _countdownSeconds.value = seconds
        countdownJob = scope.launch {
            for (remaining in seconds downTo 0) {
                _countdownSeconds.value = remaining
                onCountdownTick(remaining)
                if (remaining == 0) break
                delay(1000L)
            }
            _isCountingDown.value = false
            Log.d(TAG, "Countdown finished, auto-playing next")
            onAutoPlayNext()
        }
    }

    /** Kullanıcı geri sayımı iptal etti */
    fun cancelCountdown() {
        countdownJob?.cancel()
        _isCountingDown.value = false
        Log.d(TAG, "Countdown cancelled by user")
    }

    /** Kullanıcı stillWatching'i onayladı, session sıfırla */
    fun onStillWatchingConfirmed() {
        sessionRules.resetSession()
    }

    /** Yeni medya açıldığında session sıfırla */
    fun resetSession() {
        sessionRules.resetSession()
        cancelCountdown()
    }
}
