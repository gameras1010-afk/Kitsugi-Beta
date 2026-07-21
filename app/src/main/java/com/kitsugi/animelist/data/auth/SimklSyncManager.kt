package com.kitsugi.animelist.data.auth

import android.content.Context
import android.util.Log
import com.kitsugi.animelist.data.remote.SimklApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simkl ↔ Kitsugi uygulaması arasında çift yönlü senkronizasyon yöneticisi.
 *
 * Referans mimariler:
 * - NyanTV/SimklService.kt → updateEntry, fetchUserMovies/Shows, status mapping
 * - scrob → history/sync geri yazma (POST /sync/history)
 * - SyncMeta → dry-run, diagnostics, concurrency kontrolü
 * - Showly → Trakt = Simkl katmanı, TMDB = metadata katmanı ayrımı
 */
object SimklSyncManager {

    private const val TAG = "SimklSyncManager"
    private val simklApiClient = SimklApiClient()

    // ── Durum Dönüşüm Yardımcıları (NyanTV simklStatusToAL referans) ────────────

    /** Simkl status string'ini uygulama WatchStatus'una çevirir */
    fun simklStatusToWatchStatus(simklStatus: String?): WatchStatus = when (simklStatus) {
        "watching"    -> WatchStatus.Watching
        "completed"   -> WatchStatus.Completed
        "hold"        -> WatchStatus.Paused
        "dropped"     -> WatchStatus.Dropped
        "plantowatch" -> WatchStatus.Planned
        else          -> WatchStatus.Planned
    }

    /** Uygulama WatchStatus'unu Simkl status string'ine çevirir (NyanTV alStatusToSimkl referans) */
    fun watchStatusToSimkl(status: WatchStatus?): String = when (status) {
        WatchStatus.Watching   -> "watching"
        WatchStatus.Completed  -> "completed"
        WatchStatus.Paused     -> "hold"
        WatchStatus.Dropped    -> "dropped"
        WatchStatus.Planned    -> "plantowatch"
        else                   -> "plantowatch"
    }

    /** MediaType'a göre Simkl API tipini döner */
    private fun mediaTypeToSimklType(mediaType: MediaType): String = when (mediaType) {
        MediaType.Movie  -> "movies"
        MediaType.TvShow -> "shows"
        MediaType.Anime  -> "shows" // Simkl animeleri de shows olarak yönetir
        else             -> "shows"
    }

    // ── Tek Entry Senkronizasyonu ─────────────────────────────────────────────────

    /**
     * Uygulama listesindeki bir MediaEntry'yi Simkl'e yazar.
     * Simkl bağlı değilse işlem yapılmaz.
     * Referans: NyanTV/SimklService.kt updateEntry()
     */
    suspend fun syncEntryToSimkl(
        context: Context,
        entry: MediaEntry
    ): SyncResult = withContext(Dispatchers.IO) {
        val token = ExternalAuthManager.getSimklToken(context)
        if (token.isNullOrBlank()) {
            return@withContext SyncResult(messages = emptyList(), errors = listOf("Simkl hesabı bağlı değil"))
        }

        val simklId = entry.simklId
        if (simklId == null || simklId <= 0) {
            return@withContext SyncResult(messages = emptyList(), errors = listOf("Simkl ID bulunamadı: ${entry.title}"))
        }

        val type = mediaTypeToSimklType(entry.type)
        val simklStatus = watchStatusToSimkl(entry.status)
        val messages = mutableListOf<String>()
        val errors = mutableListOf<String>()

        // 1. Listeye ekle / durumu güncelle (NyanTV: POST /sync/add-to-list)
        runCatching {
            simklApiClient.addToList(token, simklId, type, simklStatus)
        }.onSuccess { success ->
            if (success) messages.add("Simkl listesi güncellendi (${entry.title})")
            else errors.add("Simkl listesi güncellenemedi (${entry.title})")
        }.onFailure { e ->
            errors.add("Simkl addToList hatası: ${e.message}")
            Log.e(TAG, "syncEntryToSimkl addToList hatası", e)
        }

        // 2. Bölüm ilerlemesini güncelle — sadece dizi/anime için (Scrob referans)
        val progress = entry.progress ?: 0
        if (progress > 0 && entry.type != MediaType.Movie) {
            runCatching {
                simklApiClient.updateEpisodeProgress(token, simklId, season = 1, episode = progress)
            }.onSuccess { success ->
                if (success) messages.add("Simkl bölüm ilerlemesi güncellendi: Bölüm $progress")
                else errors.add("Bölüm ilerlemesi güncellenemedi")
            }.onFailure { e ->
                errors.add("Simkl bölüm ilerlemesi hatası: ${e.message}")
                Log.e(TAG, "syncEntryToSimkl updateEpisodeProgress hatası", e)
            }
        }

        // 3. Puanı Simkl'e yaz (POST /sync/ratings)
        val score = entry.score
        if (score != null && score > 0) {
            runCatching {
                simklApiClient.setRating(token, simklId, entry.type, score)
            }.onSuccess { success ->
                if (success) messages.add("Simkl puanı güncellendi: $score/10")
                else errors.add("Simkl puanı güncellenemedi")
            }.onFailure { e ->
                errors.add("Simkl puan hatası: ${e.message}")
                Log.e(TAG, "syncEntryToSimkl setRating hatası", e)
            }
        } else if (score == null || score == 0) {
            // Puan silinmişse kaldır
            runCatching {
                simklApiClient.removeRating(token, simklId, entry.type)
            }.onFailure { e ->
                Log.w(TAG, "Simkl removeRating uyarısı: ${e.message}")
            }
        }

        SyncResult(messages = messages, errors = errors)
    }

    /**
     * Entry'yi Simkl'den siler.
     * Referans: NyanTV/SimklService.kt deleteEntry() → POST /sync/history/remove
     */
    suspend fun deleteEntryFromSimkl(
        context: Context,
        entry: MediaEntry
    ): SyncResult = withContext(Dispatchers.IO) {
        val token = ExternalAuthManager.getSimklToken(context)
        if (token.isNullOrBlank()) {
            return@withContext SyncResult(messages = emptyList(), errors = listOf("Simkl hesabı bağlı değil"))
        }

        val simklId = entry.simklId
        if (simklId == null || simklId <= 0) {
            return@withContext SyncResult(messages = emptyList(), errors = listOf("Simkl ID bulunamadı"))
        }

        val type = mediaTypeToSimklType(entry.type)
        val messages = mutableListOf<String>()
        val errors = mutableListOf<String>()

        runCatching {
            simklApiClient.removeFromList(token, simklId, type)
        }.onSuccess { success ->
            if (success) messages.add("Simkl listesinden silindi (${entry.title})")
            else errors.add("Simkl listesinden silinemedi")
        }.onFailure { e ->
            errors.add("Simkl removeFromList hatası: ${e.message}")
            Log.e(TAG, "deleteEntryFromSimkl hatası", e)
        }

        SyncResult(messages = messages, errors = errors)
    }

    // ── Simkl → Uygulama İçe Aktarma ─────────────────────────────────────────────

    /**
     * Simkl kullanıcı listesini çekip JikanSearchResult listesi olarak döner.
     * type: "shows", "movies", "anime"
     * Referans: NyanTV/SimklService.kt fetchUserMovies() + fetchUserShows()
     */
    suspend fun fetchSimklWatchlist(
        context: Context,
        type: String = "shows"
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val token = ExternalAuthManager.getSimklToken(context)
        if (token.isNullOrBlank()) {
            Log.w(TAG, "fetchSimklWatchlist: Simkl token bulunamadı")
            return@withContext emptyList()
        }

        runCatching {
            simklApiClient.getUserWatchlist(token, type)
        }.getOrElse { e ->
            Log.e(TAG, "fetchSimklWatchlist $type hatası: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Simkl kullanıcı profil bilgilerini çeker (kullanıcı adı, avatar vb.).
     * Referans: NyanTV/SimklService.kt fetchUserProfile()
     */
    suspend fun fetchSimklUserProfile(context: Context): SimklUserProfile? = withContext(Dispatchers.IO) {
        val token = ExternalAuthManager.getSimklToken(context)
        if (token.isNullOrBlank()) return@withContext null

        runCatching {
            val json = simklApiClient.getUserProfile(token) ?: return@withContext null
            val account = json.optJSONObject("account")
            val user = json.optJSONObject("user")
            SimklUserProfile(
                id = account?.optString("id"),
                name = user?.optString("name"),
                avatarUrl = user?.optString("avatar")
            )
        }.getOrElse { e ->
            Log.e(TAG, "fetchSimklUserProfile hatası: ${e.message}", e)
            null
        }
    }

    // ── Veri Modelleri ────────────────────────────────────────────────────────────

    data class SyncResult(
        val messages: List<String>,
        val errors: List<String> = emptyList()
    ) {
        val isSuccess: Boolean get() = errors.isEmpty()
        val hasWarnings: Boolean get() = messages.isNotEmpty() && errors.isNotEmpty()
    }

    data class SimklUserProfile(
        val id: String?,
        val name: String?,
        val avatarUrl: String?
    )
}
