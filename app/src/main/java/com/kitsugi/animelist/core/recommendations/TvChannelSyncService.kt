package com.kitsugi.animelist.core.recommendations

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TvChannelSync"
private const val DEBOUNCE_MS = 2_000L
private const val MAX_CHANNEL_ITEMS = 20

/**
 * B1.11 - TvChannelSyncService: MediaEntryRepository'deki "Watching" durumundaki
 * girisleri Android TV launcher "Continue Watching" kanaline esler.
 *
 * Mimari: Kitsugi'da REF'in ContinueWatchingEnrichmentCache'i gibi bir kac bircik
 * pipeline bulunmuyor; bunun yerine dogrudan MediaEntryDao'dan "Watching" durumdaki
 * girisleri okuyup WatchProgress'e donusturuyoruz. Bu, gelecekte episodic
 * ilerleme tracking eklendigi zaman kolayca glusturulabilir.
 *
 * Debounce: Liste degisimlerini 2sn debounce ederek ContentResolver thrash'ini onler.
 * Foreground/Background: onForegroundChanged() ile oynatma sirasinda sync'i erteler.
 */
@Singleton
class TvChannelSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val channelManager: AndroidTvChannelManager,
    private val recommendationManager: TvRecommendationManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var appInForeground = false

    /**
     * MainActivity.onStart/onStop'tan cagrilir.
     * Arka plana geciste bir kez reconcile yapar — launcher o anda gorundugunde
     * kanal en guncel hali gostermis olur.
     */
    fun onForegroundChanged(foreground: Boolean) {
        val wasForeground = appInForeground
        appInForeground = foreground
        if (wasForeground && !foreground) {
            scope.launch { reconcileFromDatabase() }
        }
    }

    /**
     * Servisi baslatir: Non-leanback cihaziarda hemen cikis yapar.
     * Baslangicat bir kez reconcile yapar; sonra MediaEntry degisimlerini izler.
     */
    @OptIn(FlowPreview::class)
    fun start() {
        if (!channelManager.isSupported()) {
            Log.d(TAG, "Non-leanback device; TV channel sync skipped")
            return
        }
        TvChannelRefreshJobService.schedulePeriodic(context)

        // Baslangic reconcile
        scope.launch { reconcileFromDatabase() }

        // MediaEntry degisimlerini izle ve debounce ile reconcile yap
        scope.launch {
            val db = KitsugiDatabase.getDatabase(context)
            db.mediaEntryDao().observeAll()
                .debounce(DEBOUNCE_MS)
                .collect { _ ->
                    // Uygulama on plandayken launcher kanali gorunmez;
                    // reconcile'i arka plana gecise ertele (onForegroundChanged)
                    if (appInForeground) return@collect
                    reconcileFromDatabase()
                }
        }
    }

    /**
     * Database'den guncel "Watching" girisleri okur ve kanalda reconcile eder.
     * TvChannelRefreshJobService tarafindan da cagrilir.
     */
    suspend fun reconcileFromDatabase() {
        try {
            val db = KitsugiDatabase.getDatabase(context)
            val allEntries = db.mediaEntryDao().getAll()
            val watchingEntries = allEntries
                .filter { entity ->
                    runCatching { WatchStatus.valueOf(entity.status) }
                        .getOrDefault(WatchStatus.Planned) == WatchStatus.Watching
                }
                .sortedByDescending { it.id } // Yakin zamanda guncellenen once
                .take(MAX_CHANNEL_ITEMS)

            val watchProgressList = watchingEntries.mapNotNull { entity ->
                buildWatchProgress(entity)
            }

            Log.d(TAG, "Reconciling ${watchProgressList.size} items from ${watchingEntries.size} watching entries")
            channelManager.reconcile(watchProgressList)
            recommendationManager.updateWatchNext(watchProgressList)
        } catch (e: Exception) {
            Log.w(TAG, "reconcileFromDatabase failed", e)
        }
    }

    /**
     * Cikis veya profil degisimi: Kanalin tum programlarini temizler.
     */
    suspend fun clearAll() {
        channelManager.clearAll()
        recommendationManager.clearAll()
    }

    // --- Private ---

    /**
     * MediaEntryEntity'i WatchProgress'e donusturur.
     * Not: Simdilik episodic pozisyon (ms) bilgisi yok; bu alan
     * KitsugiPlayerViewModel entegrasyonu tamamlandiginda doldurulacak.
     * Progress alanini bolum sayisi olarak okuruz; launcher progress bar
     * bos olur ama content tile gozukur.
     */
    private fun buildWatchProgress(entity: com.kitsugi.animelist.data.local.MediaEntryEntity): WatchProgress? {
        return try {
            val malId = entity.malId ?: return null
            val contentType = when (runCatching { MediaType.valueOf(entity.type) }.getOrDefault(MediaType.Anime)) {
                MediaType.Movie -> "movie"
                MediaType.Manga -> "manga"
                else -> "anime"
            }
            val contentId = WatchProgress.buildContentId(malId)
            WatchProgress(
                contentId = contentId,
                contentType = contentType,
                name = entity.title,
                poster = entity.imageUrl,
                backdrop = null,
                logo = null,
                season = null,
                // Bolum ilerlemesini sezon/bolum olarak goster
                episode = if (entity.progress > 0) entity.progress else null,
                episodeTitle = null,
                position = 0L,
                duration = 0L,
                lastWatched = System.currentTimeMillis(),
                progressPercent = if (entity.total != null && entity.total > 0) {
                    ((entity.progress.toFloat() / entity.total) * 100).toInt()
                } else null
            )
        } catch (e: Exception) {
            Log.w(TAG, "buildWatchProgress failed for entity ${entity.malId}", e)
            null
        }
    }
}
