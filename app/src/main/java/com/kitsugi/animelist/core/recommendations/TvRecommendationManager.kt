package com.kitsugi.animelist.core.recommendations

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * B1.5 - TvRecommendationManager: WatchNext (izlemeye devam) programlarini yonetir.
 *
 * Capability-gated: Sadece FEATURE_LEANBACK destekli cihazlarda calisir.
 * Thread-safe: Mutex ile eş zamanli content resolver yazimlarini onler.
 *
 * Kullanim: TvHomeViewModel veya AndroidTvChannelSyncService tarafindan
 * "Continue Watching" listesi degistiginde cagrilir.
 */
@Singleton
class TvRecommendationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val programBuilder: TvProgramBuilder
) {
    private val mutex = Mutex()

    companion object {
        const val TAG = "TvRecommendation"

        // Oynatma aktifken launcher channel sync'i duraksatmak icin paylasiilan durum.
        // TvPlayerActivity tarafindan set edilir; AndroidTvChannelSyncService bu
        // flag'i okuyarak oynatma sirasinda provider'i thrash etmez.
        val isPlaybackActive = MutableStateFlow(false)
    }

    /**
     * Verilen WatchProgress listesinden WatchNextProgram kayitlarini gunceller.
     * Oncelikle tum Kitsugi "wn_" kayitlarini temizler, sonra yeniden yazar —
     * bu yaklasim stale tile'lari onler ve launcher'in tutarli goruntulenmesini saglar.
     */
    suspend fun updateWatchNext(items: List<WatchProgress>) {
        if (!isTvDevice()) return
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    programBuilder.clearAllWatchNextPrograms()
                    items.forEach { progress ->
                        val program = programBuilder.buildWatchNextProgram(progress)
                        programBuilder.upsertWatchNextProgram(
                            program,
                            programBuilder.watchNextId(progress)
                        )
                    }
                    Log.d(TAG, "Updated ${items.size} WatchNext programs")
                } catch (e: Exception) {
                    Log.w(TAG, "updateWatchNext failed", e)
                }
            }
        }
    }

    /**
     * Belirli bir icerik silindiginde WatchNext'ten kaldirir.
     * Ornegin kullanici listeden bir seri sildiginde tetiklenir.
     */
    suspend fun onProgressRemoved(contentId: String) {
        if (!isTvDevice()) return
        withContext(Dispatchers.IO) {
            try {
                programBuilder.removeWatchNextByContentId(contentId)
                Log.d(TAG, "Removed WatchNext for contentId=$contentId")
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Cikis/profil degisimi sirasinda tum WatchNext kayitlarini temizler.
     */
    suspend fun clearAll() {
        if (!isTvDevice()) return
        withContext(Dispatchers.IO) {
            try {
                programBuilder.clearAllWatchNextPrograms()
                Log.d(TAG, "Cleared all WatchNext programs")
            } catch (_: Exception) {
            }
        }
    }

    // Capability gate — non-leanback cihazlarda hic calisma
    fun isTvDevice(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}
