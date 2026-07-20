package com.kitsugi.animelist.data.local

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: HistoryDao) {
    val allHistoryFlow: Flow<List<HistoryEntity>> = dao.getAllHistoryFlow()

    suspend fun getProgress(mediaId: Int, episode: Int): HistoryEntity? {
        return dao.getProgress(mediaId, episode)
    }

    suspend fun getHistoryForMedia(mediaId: Int): List<HistoryEntity> {
        return dao.getHistoryForMedia(mediaId)
    }

    suspend fun saveProgress(
        mediaId: Int,
        episode: Int,
        lastPositionMs: Long,
        durationMs: Long,
        addonName: String? = null
    ) {
        val existing = dao.getProgress(mediaId, episode)
        val entity = HistoryEntity(
            id = existing?.id ?: 0,
            mediaId = mediaId,
            episode = episode,
            lastPositionMs = lastPositionMs,
            durationMs = durationMs,
            lastWatchedAt = System.currentTimeMillis(),
            addonName = addonName
        )
        dao.insertOrUpdate(entity)
    }

    suspend fun deleteProgress(mediaId: Int, episode: Int) {
        dao.deleteProgress(mediaId, episode)
    }

    suspend fun clearAllHistory() {
        dao.clearAllHistory()
    }
}
