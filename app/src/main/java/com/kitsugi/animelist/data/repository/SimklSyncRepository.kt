package com.kitsugi.animelist.data.repository

import android.content.Context
import android.util.Log
import com.kitsugi.animelist.data.remote.SimklApiClient
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SIMKL Entegrasyon – P0: SimklSyncRepository
 *
 * SIMKL Sync Guide (https://api.simkl.org/guides/sync) referans alındı.
 * NyanTV SimklService.kt + SIMKL-ENTEGRASYON-PLANI (3).md esas alındı.
 *
 * Sorumluluklar:
 *  1. İlk sync (shows → movies → anime) sıralı, paralel değil
 *  2. /sync/activities gate ile delta kontrolü
 *  3. Per-user write queue (max 1 write/sn) — outbox tabanlı retry
 *  4. 401 → revoked, 429 → exponential backoff
 */
class SimklSyncRepository(
    private val context: Context,
    private val apiClient: SimklApiClient = SimklApiClient()
) {

    companion object {
        private const val TAG = "SimklSyncRepository"
        private const val WRITE_INTERVAL_MS = 1100L   // >1s between writes
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    // ── Outbox ──────────────────────────────────────────────────────────────

    data class OutboxItem(
        val id: String = java.util.UUID.randomUUID().toString(),
        val token: String,
        val operation: Operation,
        val simklId: Int,
        val mediaType: MediaType,
        val status: WatchStatus? = null,
        val season: Int? = null,
        val episode: Int? = null,
        val rating: Int? = null,
        var attempts: Int = 0,
        var lastError: String? = null
    )

    enum class Operation {
        SET_STATUS,
        MARK_WATCHED_EPISODE,
        SET_RATING,
        REMOVE_RATING
    }

    private val outbox = ConcurrentLinkedQueue<OutboxItem>()
    private val isWorkerRunning = AtomicBoolean(false)

    // ── Sync State (in-memory; persist via DataStore if needed) ─────────────

    private var lastActivitiesJson: String = ""
    private var lastDeltaAt: String = ""
    private var initialSyncCompleted = false

    // ── Initial Sync ─────────────────────────────────────────────────────────

    /**
     * İlk bağlantıda tüm kütüphaneyi sırayla çeker.
     * SIMKL Sync Guide: "Never parallel-call initial sync."
     * Shows → Movies → Anime sırası zorunlu.
     *
     * @param token SIMKL OAuth access token
     * @param onPartialReady İlk tip hazır olunca UI'yı göstermek için çağrılır
     */
    suspend fun performInitialSync(
        token: String,
        onPartialReady: (() -> Unit)? = null
    ): InitialSyncResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "performInitialSync: starting...")

        try {
            val shows  = apiClient.getUserWatchlist(token, "shows")
            Log.d(TAG, "  shows: ${shows.size}")
            onPartialReady?.invoke()   // İlk liste hazır → UI'yı aç

            val movies = apiClient.getUserWatchlist(token, "movies")
            Log.d(TAG, "  movies: ${movies.size}")

            val anime  = apiClient.getUserWatchlist(token, "anime")
            Log.d(TAG, "  anime: ${anime.size}")

            // Sync cursor'u kaydet
            val activities = fetchActivities(token)
            lastActivitiesJson = activities
            lastDeltaAt = getCurrentIso()
            initialSyncCompleted = true

            Log.i(TAG, "performInitialSync: done — shows=${shows.size}, movies=${movies.size}, anime=${anime.size}")
            InitialSyncResult.Success(shows + movies + anime)
        } catch (e: Exception) {
            Log.e(TAG, "performInitialSync failed", e)
            InitialSyncResult.Error(e.message ?: "Sync hatası")
        }
    }

    // ── Delta Sync ───────────────────────────────────────────────────────────

    /**
     * /sync/activities kontrol eder; değişiklik yoksa API'ye dokunmaz.
     * Değişiklik varsa delta (date_from) ile sadece güncellenenleri alır.
     */
    suspend fun performDeltaSync(token: String): DeltaSyncResult = withContext(Dispatchers.IO) {
        if (!initialSyncCompleted) {
            Log.w(TAG, "performDeltaSync: skipping — initial sync not done")
            return@withContext DeltaSyncResult.Skipped("İlk sync henüz tamamlanmadı")
        }

        try {
            val currentActivities = fetchActivities(token)

            // Gate: activities değişmediyse hiçbir şey çekme
            if (currentActivities == lastActivitiesJson && lastActivitiesJson.isNotBlank()) {
                Log.d(TAG, "performDeltaSync: no changes detected")
                return@withContext DeltaSyncResult.NoChange
            }

            Log.i(TAG, "performDeltaSync: changes detected, fetching delta since $lastDeltaAt")

            // Delta fetch (date_from ile)
            // Not: SimklApiClient.getUserWatchlist date_from parametresi eklenmeli
            // Şimdilik full re-fetch (V1 basit uygulama)
            val shows  = apiClient.getUserWatchlist(token, "shows")
            val movies = apiClient.getUserWatchlist(token, "movies")
            val anime  = apiClient.getUserWatchlist(token, "anime")

            lastActivitiesJson = currentActivities
            lastDeltaAt = getCurrentIso()

            Log.i(TAG, "performDeltaSync: done")
            DeltaSyncResult.Updated(shows + movies + anime)
        } catch (e: SimklAuthException) {
            Log.e(TAG, "performDeltaSync: token revoked")
            DeltaSyncResult.TokenRevoked
        } catch (e: Exception) {
            Log.e(TAG, "performDeltaSync failed", e)
            DeltaSyncResult.Error(e.message ?: "Delta sync hatası")
        }
    }

    // ── Write Queue ──────────────────────────────────────────────────────────

    /**
     * Durum güncelleme isteğini outbox'a ekler (optimistic UI için).
     * Worker çalışmıyorsa başlatır.
     */
    fun enqueueSetStatus(
        token: String,
        simklId: Int,
        mediaType: MediaType,
        status: WatchStatus
    ) {
        outbox.add(
            OutboxItem(
                token = token,
                operation = Operation.SET_STATUS,
                simklId = simklId,
                mediaType = mediaType,
                status = status
            )
        )
        startWorkerIfNeeded()
    }

    /**
     * Bölüm izlendi isteğini outbox'a ekler.
     */
    fun enqueueMarkEpisode(
        token: String,
        simklId: Int,
        season: Int,
        episode: Int
    ) {
        outbox.add(
            OutboxItem(
                token = token,
                operation = Operation.MARK_WATCHED_EPISODE,
                simklId = simklId,
                mediaType = MediaType.TvShow,
                season = season,
                episode = episode
            )
        )
        startWorkerIfNeeded()
    }

    /**
     * Puan güncelleme isteğini outbox'a ekler (1-10).
     */
    fun enqueueSetRating(
        token: String,
        simklId: Int,
        mediaType: MediaType,
        rating: Int
    ) {
        require(rating in 1..10) { "SIMKL rating must be 1-10, got $rating" }
        outbox.add(
            OutboxItem(
                token = token,
                operation = Operation.SET_RATING,
                simklId = simklId,
                mediaType = mediaType,
                rating = rating
            )
        )
        startWorkerIfNeeded()
    }

    // ── Worker ───────────────────────────────────────────────────────────────

    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun startWorkerIfNeeded() {
        if (!isWorkerRunning.compareAndSet(false, true)) return

        workerScope.launch {
            try {
                processOutbox()
            } finally {
                isWorkerRunning.set(false)
                // Outbox'ta hâlâ item varsa tekrar başlat
                if (outbox.isNotEmpty()) startWorkerIfNeeded()
            }
        }
    }

    private suspend fun processOutbox() {
        while (outbox.isNotEmpty()) {
            val item = outbox.peek() ?: break

            val success = try {
                executeItem(item)
            } catch (e: SimklAuthException) {
                // 401 → token revoked, retry olmaz
                outbox.poll()
                Log.e(TAG, "worker: 401 revoked — clearing outbox for user")
                // TODO: notify UI to show reconnect prompt
                break
            } catch (e: SimklRateLimitException) {
                // 429 → exponential backoff
                val waitMs = (1000L * Math.pow(2.0, item.attempts.toDouble())).toLong()
                    .coerceAtMost(30_000L)
                Log.w(TAG, "worker: 429 rate limit — waiting ${waitMs}ms")
                delay(waitMs)
                false
            } catch (e: Exception) {
                Log.e(TAG, "worker: error executing ${item.operation}", e)
                item.lastError = e.message
                false
            }

            if (success) {
                outbox.poll()  // başarılıysa çıkar
                Log.d(TAG, "worker: ${item.operation} ok (simklId=${item.simklId})")
            } else {
                item.attempts++
                if (item.attempts >= MAX_RETRY_ATTEMPTS) {
                    outbox.poll()  // max retry aşıldı → drop
                    Log.e(TAG, "worker: ${item.operation} dropped after $MAX_RETRY_ATTEMPTS attempts")
                }
            }

            delay(WRITE_INTERVAL_MS)  // max 1 write/sn
        }
    }

    private suspend fun executeItem(item: OutboxItem): Boolean {
        return when (item.operation) {
            Operation.SET_STATUS -> {
                val statusStr = item.status?.let { watchStatusToSimkl(it) } ?: return false
                apiClient.updateWatchlistStatus(item.token, item.simklId, item.mediaType, item.status!!)
            }
            Operation.MARK_WATCHED_EPISODE -> {
                val season = item.season ?: return false
                val episode = item.episode ?: return false
                apiClient.updateEpisodeProgress(item.token, item.simklId, season, episode)
            }
            Operation.SET_RATING -> {
                val rating = item.rating ?: return false
                apiClient.setRating(item.token, item.simklId, item.mediaType, rating)
            }
            Operation.REMOVE_RATING -> {
                apiClient.removeRating(item.token, item.simklId, item.mediaType)
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private suspend fun fetchActivities(token: String): String = withContext(Dispatchers.IO) {
        try {
            val activities = apiClient.getActivities(token)
            activities?.toString() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun watchStatusToSimkl(status: WatchStatus): String = when (status) {
        WatchStatus.Watching   -> "watching"
        WatchStatus.Completed  -> "completed"
        WatchStatus.Planned    -> "plantowatch"
        WatchStatus.Dropped    -> "dropped"
        WatchStatus.Paused     -> "hold"
        WatchStatus.Repeating  -> "watching"
    }

    private fun getCurrentIso(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }

    // ── Pending count (UI badge için) ────────────────────────────────────────

    val pendingWriteCount: Int get() = outbox.size

    // ── Sealed Results ───────────────────────────────────────────────────────

    sealed class InitialSyncResult {
        data class Success(val items: List<com.kitsugi.animelist.data.remote.JikanSearchResult>) : InitialSyncResult()
        data class Error(val message: String) : InitialSyncResult()
    }

    sealed class DeltaSyncResult {
        data object NoChange : DeltaSyncResult()
        data object TokenRevoked : DeltaSyncResult()
        data class Updated(val items: List<com.kitsugi.animelist.data.remote.JikanSearchResult>) : DeltaSyncResult()
        data class Skipped(val reason: String) : DeltaSyncResult()
        data class Error(val message: String) : DeltaSyncResult()
    }
}

// ── Custom Exceptions ────────────────────────────────────────────────────────

class SimklAuthException(message: String) : Exception(message)
class SimklRateLimitException(message: String) : Exception(message)
