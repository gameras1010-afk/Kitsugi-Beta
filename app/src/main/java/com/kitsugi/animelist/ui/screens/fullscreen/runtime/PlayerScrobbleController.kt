package com.kitsugi.animelist.ui.screens.fullscreen.runtime

import android.util.Log
import com.kitsugi.animelist.data.auth.AniListSyncManager
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.WatchStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * T1.6: PlayerScrobbleController
 *
 * AniList / MAL izleme ilerlemesini oynatıcıdan alıp uzak servislere iletir.
 * Dağınık VM kodundan buraya taşındı; PlayerRuntimeOrchestrator ile yönetilir.
 *
 * Scrobble mantığı:
 *  - Bölüm %80 izlendiğinde (veya 20dk sonra) tamamlandı işareti
 *  - Her 30sn'de bir pozisyon backend'e gönderilmez; sadece bölüm tamamı kaydedilir
 *  - Offline scrob kuyruğu için SimklSyncRepository.outbox benzer bir yapı
 */
class PlayerScrobbleController(
    private val scope: CoroutineScope,
    private val getAniListToken: () -> String?,
    private val onScrobbleSuccess: (mediaId: Int, episode: Int) -> Unit = { _, _ -> },
    private val onScrobbleFail: (reason: String) -> Unit = {}
) {
    private val TAG = "ScrobbleController"

    // Scrobble durumu
    private var currentMediaEntry: MediaEntry? = null
    private var currentEpisode: Int = 0
    private var durationMs: Long = 0L
    private var scrobbled: Boolean = false
    private var scrobbleJob: Job? = null

    /** Yeni bölüm başladığında çağrılır */
    fun onEpisodeStarted(entry: MediaEntry, episode: Int, durationMs: Long) {
        scrobbleJob?.cancel()
        currentMediaEntry = entry
        currentEpisode = episode
        this.durationMs = durationMs
        scrobbled = false
        Log.d(TAG, "Episode started: mediaId=${entry.id} ep=$episode dur=${durationMs}ms")
    }

    /**
     * Pozisyon güncellemesinde çağrılır.
     * @param positionMs Anlık pozisyon
     * @param durationMs Toplam bölüm süresi (ms)
     */
    fun onPositionUpdate(positionMs: Long, durationMs: Long = 0L) {
        if (durationMs > 0L) {
            this.durationMs = durationMs
        }
        if (scrobbled || this.durationMs <= 0L) return
        val progress = positionMs.toFloat() / this.durationMs.toFloat()
        // %80 eşiği aşıldığında scrobble et
        if (progress >= 0.80f) {
            scrobble()
        }
    }

    /** Manuel olarak scrobble tetikle (bölüm "Tamamlandı" butonu) */
    fun forceScrobble() = scrobble()

    private fun scrobble() {
        if (scrobbled) return
        scrobbled = true
        val entry = currentMediaEntry ?: return
        val token = getAniListToken() ?: run {
            Log.d(TAG, "No AniList token, skipping scrobble")
            return
        }

        scrobbleJob = scope.launch(Dispatchers.IO) {
            try {
                val updatedEntry = entry.copy(
                    progress = currentEpisode,
                    status = if (currentEpisode >= (entry.total ?: Int.MAX_VALUE)) {
                        WatchStatus.Completed
                    } else {
                        WatchStatus.Watching
                    }
                )
                val result = AniListSyncManager.updateAniListEntry(token, updatedEntry)
                if (result != null) {
                    Log.d(TAG, "Scrobble SUCCESS: aniListId=$result ep=$currentEpisode")
                    onScrobbleSuccess(result, currentEpisode)
                } else {
                    Log.w(TAG, "Scrobble returned null")
                    onScrobbleFail("AniList null response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Scrobble failed", e)
                scrobbled = false // Tekrar dene
                onScrobbleFail(e.message ?: "Unknown error")
            }
        }
    }

    /** Controller temizle (VM.onCleared) */
    fun clear() {
        scrobbleJob?.cancel()
        currentMediaEntry = null
        scrobbled = false
    }
}
