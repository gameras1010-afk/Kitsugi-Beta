package com.kitsugi.animelist.data.local

import android.content.Context
import com.kitsugi.animelist.data.auth.ExternalListSyncManager
import com.kitsugi.animelist.model.MediaEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MediaEntryRepository(
    private val dao: MediaEntryDao,
    private val pendingSyncDao: PendingSyncDao? = null,
    val context: Context? = null,
    private val onExternalSyncMessage: ((String) -> Unit)? = null
) {
    val entriesFlow: Flow<List<MediaEntry>> = kotlinx.coroutines.flow.flow {
        dao.observeAll().collect { entities ->
            emit(entities.map { it.toDomain() })
        }
    }

    suspend fun insert(entry: MediaEntry) {
        dao.insert(
            entry.copy(id = 0).toEntity()
        )

        syncEntryIfPossible(entry)
    }

    suspend fun insertAll(entries: List<MediaEntry>) {
        if (entries.isEmpty()) return
        dao.insertAll(entries.map { it.copy(id = 0).toEntity() })
    }

    suspend fun updateAllDirect(entries: List<MediaEntry>) {
        if (entries.isEmpty()) return
        dao.updateAll(entries.map { it.toEntity() })
    }

    suspend fun update(entry: MediaEntry, syncExternal: Boolean = true, advancedScores: List<Double>? = null) {
        dao.update(
            entry.toEntity()
        )

        if (syncExternal) {
            syncEntryIfPossible(entry, advancedScores)
        }
    }

    suspend fun deleteById(id: Int) {
        val entry = dao.getById(id)?.toDomain()

        dao.deleteById(id)

        if (entry != null) {
            syncDeleteIfPossible(entry)
        }
    }

    suspend fun deleteBySource(source: String) {
        dao.deleteBySource(source)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    suspend fun replaceAll(entries: List<MediaEntry>) {
        dao.deleteAll()
        insertAll(entries)
    }

    /**
     * Bekleyen kuyruğu boşaltır. AppViewModel.init'ten çağrılır.
     * @return Başarıyla gönderilen işlem sayısı
     */
    suspend fun drainPendingQueue(): Int {
        val ctx = context ?: return 0
        val queueDao = pendingSyncDao ?: return 0
        return PendingSyncDrainer.drain(ctx, queueDao)
    }

    // ────────────────────────────────────────────────
    // Private Sync Helpers
    // ────────────────────────────────────────────────

    private suspend fun syncEntryIfPossible(entry: MediaEntry, advancedScores: List<Double>? = null) {
        val appContext = context ?: return
        // Simkl kaydı (simklId var) veya MAL/AniList kaydı (malId var) ise sync yap
        if (entry.malId == null && (entry.simklId == null || entry.simklId <= 0)) return

        val result = runCatching {
            ExternalListSyncManager.syncEntry(
                context = appContext,
                entry = entry,
                advancedScores = advancedScores
            )
        }

        val syncResult = result.getOrNull()
        val success = result.isSuccess && syncResult != null && syncResult.errors.isEmpty()

        if (success) {
            val messages = syncResult?.messages.orEmpty()
            removeFromQueueIfPresent(entry, "UPDATE")
            notifySyncResult(messages)

            // Dual-write: AniList'ten dönen list entry ID'yi yerel DB'ye kaydet
            val newAniListEntryId = syncResult?.aniListEntryId
            if (newAniListEntryId != null && newAniListEntryId != entry.aniListEntryId) {
                val updatedEntry = entry.copy(aniListEntryId = newAniListEntryId)
                dao.update(updatedEntry.toEntity())
            }

            drainPendingQueue()
        } else {
            enqueuePending("UPDATE", entry)
            val allErrors = syncResult?.errors.orEmpty().joinToString(" | ")
            val isAuthError = allErrors.contains("401") || allErrors.contains("token geçersiz") || allErrors.contains("Unauthorized")
            val displayMessage = if (isAuthError) {
                "🔑 Oturum süresi doldu, lütfen ayarlardan tekrar bağlanın"
            } else {
                "📵 Çevrimdışı kaydedildi, bağlantı gelince gönderilecek"
            }
            notifySyncResult(syncResult?.messages.orEmpty() + listOf(displayMessage))
        }
    }

    private suspend fun syncDeleteIfPossible(entry: MediaEntry) {
        val appContext = context ?: return
        // Simkl kaydı (simklId var) veya MAL/AniList kaydı (malId var) ise sil
        if (entry.malId == null && (entry.simklId == null || entry.simklId <= 0)) return

        val result = runCatching {
            ExternalListSyncManager.deleteEntry(
                context = appContext,
                entry = entry
            )
        }

        val syncResult = result.getOrNull()
        val success = result.isSuccess && syncResult != null && syncResult.errors.isEmpty()

        if (success) {
            val messages = syncResult?.messages.orEmpty()
            removeFromQueueIfPresent(entry, "DELETE")
            notifySyncResult(messages)
            drainPendingQueue()
        } else {
            enqueuePending("DELETE", entry)
            val allErrors = syncResult?.errors.orEmpty().joinToString(" | ")
            val isAuthError = allErrors.contains("401") || allErrors.contains("token geçersiz") || allErrors.contains("Unauthorized")
            val displayMessage = if (isAuthError) {
                "🔑 Oturum süresi doldu, lütfen ayarlardan tekrar bağlanın"
            } else {
                "📵 Çevrimdışı kaydedildi, bağlantı gelince gönderilecek"
            }
            notifySyncResult(syncResult?.messages.orEmpty() + listOf(displayMessage))
        }
    }

    private suspend fun enqueuePending(operation: String, entry: MediaEntry) {
        val queueDao = pendingSyncDao ?: return
        PendingSyncDrainer.enqueue(queueDao, operation, entry)
    }

    /**
     * Aynı malId + operation için eski bekleyen kayıt varsa sil
     * (örn. çevrimiçiyken yeniden güncelleme yapınca eski UPDATE'i temizle)
     */
    private suspend fun removeFromQueueIfPresent(entry: MediaEntry, operation: String) {
        val queueDao = pendingSyncDao ?: return
        val all = queueDao.getAll()
        all.filter { it.operation == operation }.forEach { pending ->
            // JSON'dan malId'yi okuyup karşılaştır
            runCatching {
                val json = org.json.JSONObject(pending.entryJson)
                val pendingMalId = json.opt("malId")
                if (pendingMalId != null && pendingMalId != org.json.JSONObject.NULL &&
                    (pendingMalId as? Int) == entry.malId
                ) {
                    queueDao.deleteById(pending.id)
                }
            }
        }
    }

    private fun notifySyncResult(messages: List<String>) {
        val message = messages
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        if (message.isNotBlank()) {
            onExternalSyncMessage?.invoke(message)
        }
    }
}