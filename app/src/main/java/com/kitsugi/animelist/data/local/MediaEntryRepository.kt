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
        val newId = dao.insert(entry.copy(id = 0).toEntity())
        // DB'de oluşan gerçek id ile sync yap (AniList entryId geri yazımı doğru entity'yi yakalar)
        val persistedEntry = dao.getById(newId.toInt())?.toDomain() ?: entry.copy(id = newId.toInt())
        syncEntryIfPossible(persistedEntry)
    }

    suspend fun insertAll(entries: List<MediaEntry>) {
        if (entries.isEmpty()) return
        dao.insertAll(entries.map { it.copy(id = 0).toEntity() })
    }

    /**
     * Granüler import: mevcut kayıtlarla karşılaştırır, sadece
     * gerçekten değişen kayıtları günceller. Tüm işlem tek bir
     * @Transaction içinde çalışır → Room Flow yalnızca 1 kez
     * tetiklenir ve Compose yalnızca değişen kartları yeniden çizer.
     *
     * @param source  "anilist", "mal", "simkl" vb.
     * @param importedEntries  Uzak API'dan gelen güncel liste
     */
    suspend fun smartImport(source: String, importedEntries: List<MediaEntry>) {
        if (importedEntries.isEmpty()) {
            dao.deleteBySource(source)
            return
        }

        // Yerel DB'deki mevcut kayıtları al (sadece ilgili source)
        val existing = dao.getAll()
            .filter { it.source.equals(source, ignoreCase = true) }
            .associateBy { it.malId }

        val importedByMalId = importedEntries
            .filter { it.malId != null }
            .associateBy { it.malId!! }

        val toInsert = mutableListOf<MediaEntryEntity>()
        val toUpdate = mutableListOf<MediaEntryEntity>()

        for (imported in importedEntries) {
            val existingEntity = existing[imported.malId]
            if (existingEntity == null) {
                // Yeni kayıt → insert
                toInsert.add(imported.copy(id = 0).toEntity())
            } else if (hasChanged(existingEntity.toDomain(), imported)) {
                // Değişmiş kayıt → id'yi koruyarak güncelle
                toUpdate.add(imported.copy(id = existingEntity.id).toEntity())
            }
            // Değişmemiş kayıt → atla (Flow tetiklememe)
        }

        // Uzak listede artık olmayan kayıtları sil
        val importedMalIds = importedByMalId.keys
        val toDeleteIds = existing.values
            .filter { it.malId == null || it.malId !in importedMalIds }
            .map { it.id }

        // Tek atomik transaction → Flow 1 kez tetiklenir
        dao.smartImportTransaction(toInsert, toUpdate, toDeleteIds)
    }

    /**
     * İki entry arasında anlamlı bir fark var mı kontrol eder.
     * Aynıysa import sırasında DB'ye dokunmayız.
     */
    private fun hasChanged(local: MediaEntry, remote: MediaEntry): Boolean {
        return local.status != remote.status ||
            local.progress != remote.progress ||
            local.score != remote.score ||
            local.total != remote.total ||
            local.isFavorite != remote.isFavorite ||
            local.repeatCount != remote.repeatCount ||
            local.notes != remote.notes ||
            local.startDate != remote.startDate ||
            local.endDate != remote.endDate
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
        // Senkronizasyon için geçerli koşullar:
        // 1) malId var (MAL veya gerçek bir MAL ID'si olan AniList kaydı)
        // 2) aniListEntryId var (kaynak ne olursa olsun AniList liste kaydı)
        // 3) kaynak anilist (mediaId henüz bilinmese bile AniList'e push edilebilir)
        // 4) simklId var (Simkl kaydı)
        val hasAniListLink = entry.aniListEntryId != null || entry.source == "anilist"
        val hasMalLink = entry.malId != null
        val hasSimklLink = entry.simklId != null && entry.simklId > 0
        if (!hasAniListLink && !hasMalLink && !hasSimklLink) return

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
        // Silme için geçerli koşullar (syncEntryIfPossible ile aynı mantık)
        val hasAniListLink = entry.aniListEntryId != null || entry.source == "anilist"
        val hasMalLink = entry.malId != null
        val hasSimklLink = entry.simklId != null && entry.simklId > 0
        if (!hasAniListLink && !hasMalLink && !hasSimklLink) return

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