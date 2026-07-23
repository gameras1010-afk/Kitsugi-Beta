package com.kitsugi.animelist.data.auth

import android.content.Context
import com.kitsugi.animelist.model.MediaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExternalListSyncManager {
    private const val ANILIST_SYNTHETIC_ID_OFFSET = 100_000_000

    suspend fun syncEntry(
        context: Context,
        entry: MediaEntry,
        advancedScores: List<Double>? = null
    ): SyncResult {
        return withContext(Dispatchers.IO) {
            val messages = mutableListOf<String>()
            val errors = mutableListOf<String>()
            val syncedSources = mutableListOf<String>()
            var resolvedAniListEntryId: Int? = null

            val aniListToken = ExternalAuthManager.getAniListToken(context)
            val malToken = ExternalAuthManager.getOrRefreshMalToken(context)
            val simklToken = ExternalAuthManager.getSimklToken(context)

            // Her girdiyi yalnızca kendi kaynağına (source) göre senkronize et
            val shouldSyncAniList = !aniListToken.isNullOrBlank() && entry.source == "anilist"
            val shouldSyncMal = !malToken.isNullOrBlank() && (entry.source == "mal" || entry.source == "jikan")
            val shouldSyncSimkl = !simklToken.isNullOrBlank() && entry.source == "simkl" && entry.simklId != null && entry.simklId > 0

            if (shouldSyncAniList) {
                runCatching {
                    AniListSyncManager.updateAniListEntry(
                        token = aniListToken!!,
                        entry = entry,
                        advancedScores = advancedScores
                    )
                }.onSuccess { remoteId ->
                    if (remoteId != null) resolvedAniListEntryId = remoteId
                    syncedSources.add("AniList")
                }.onFailure { error ->
                    errors.add("AniList: ${error.message}")
                }
            }

            // AniList bağlıysa favori durumunu senkronize et (Toggle-flip'i önlemek için güncel durumu kontrol ederiz)
            if (!aniListToken.isNullOrBlank()) {
                val malIdVal = entry.malId
                if (entry.source == "anilist" || (malIdVal != null && malIdVal.isRealMalId())) {
                    runCatching {
                        val remoteFav = AniListSyncManager.getAniListMediaFavoriteStatus(aniListToken, entry)
                        if (remoteFav != null && remoteFav != entry.isFavorite) {
                            AniListSyncManager.toggleAniListFavourite(aniListToken, entry)
                            if (entry.source != "anilist" && !syncedSources.contains("AniList")) {
                                syncedSources.add("AniList")
                            }
                        }
                    }
                }
            }

            val realMalId = entry.malId?.takeIf { it.isRealMalId() }

            if (shouldSyncMal && realMalId != null) {
                runCatching {
                    MalSyncManager.updateMalEntry(
                        token = malToken!!,
                        entry = entry
                    )
                }.onSuccess {
                    syncedSources.add("MyAnimeList")
                }.onFailure { error ->
                    errors.add("MAL: ${error.message}")
                }
            }

            if (shouldSyncSimkl) {
                runCatching {
                    SimklSyncManager.syncEntryToSimkl(context, entry)
                }.onSuccess { simklResult ->
                    if (simklResult.errors.isEmpty()) {
                        syncedSources.add("Simkl")
                    } else {
                        errors.addAll(simklResult.errors)
                    }
                }.onFailure { error ->
                    errors.add("Simkl: ${error.message}")
                }
            }

            val formatted = formatSyncSources(syncedSources, isDelete = false)
            if (formatted.isNotEmpty()) {
                messages.add(formatted)
            }

            SyncResult(
                messages = messages,
                errors = errors,
                aniListEntryId = resolvedAniListEntryId
            )
        }
    }

    suspend fun deleteEntry(
        context: Context,
        entry: MediaEntry
    ): SyncResult {
        return withContext(Dispatchers.IO) {
            val messages = mutableListOf<String>()
            val errors = mutableListOf<String>()
            val deletedSources = mutableListOf<String>()

            val aniListToken = ExternalAuthManager.getAniListToken(context)
            val malToken = ExternalAuthManager.getOrRefreshMalToken(context)
            val simklToken = ExternalAuthManager.getSimklToken(context)

            // Her girdiyi yalnızca kendi kaynağına (source) göre sil
            val shouldDeleteAniList = !aniListToken.isNullOrBlank() && entry.source == "anilist"
            val shouldDeleteMal = !malToken.isNullOrBlank() && (entry.source == "mal" || entry.source == "jikan")
            val shouldDeleteSimkl = !simklToken.isNullOrBlank() && entry.source == "simkl" && entry.simklId != null && entry.simklId > 0

            if (shouldDeleteAniList) {
                runCatching {
                    AniListSyncManager.deleteAniListEntry(
                        token = aniListToken!!,
                        entry = entry
                    )
                }.onSuccess {
                    deletedSources.add("AniList")
                }.onFailure { error ->
                    errors.add("AniList silme: ${error.message}")
                }
            }

            val realMalId = entry.malId?.takeIf { it.isRealMalId() }

            if (shouldDeleteMal && realMalId != null) {
                runCatching {
                    MalSyncManager.deleteMalEntry(
                        token = malToken!!,
                        entry = entry
                    )
                }.onSuccess {
                    deletedSources.add("MyAnimeList")
                }.onFailure { error ->
                    errors.add("MAL silme: ${error.message}")
                }
            }

            if (shouldDeleteSimkl) {
                runCatching {
                    SimklSyncManager.deleteEntryFromSimkl(context, entry)
                }.onSuccess { simklResult ->
                    if (simklResult.errors.isEmpty()) {
                        deletedSources.add("Simkl")
                    } else {
                        errors.addAll(simklResult.errors)
                    }
                }.onFailure { error ->
                    errors.add("Simkl silme: ${error.message}")
                }
            }

            val formatted = formatSyncSources(deletedSources, isDelete = true)
            if (formatted.isNotEmpty()) {
                messages.add(formatted)
            }

            SyncResult(
                messages = messages,
                errors = errors
            )
        }
    }

    private fun formatSyncSources(sources: List<String>, isDelete: Boolean): String {
        if (sources.isEmpty()) return ""
        val joined = when (sources.size) {
            1 -> sources[0]
            2 -> "${sources[0]} ve ${sources[1]}"
            else -> {
                val last = sources.last()
                val remaining = sources.dropLast(1).joinToString(", ")
                "$remaining ve $last"
            }
        }
        return if (isDelete) {
            "$joined kütüphanesinden silindi"
        } else {
            "$joined başarıyla eşitlendi"
        }
    }

    private fun Int.isRealMalId(): Boolean {
        return this > 0 && this < ANILIST_SYNTHETIC_ID_OFFSET
    }

    data class SyncResult(
        val messages: List<String>,
        val errors: List<String> = emptyList(),
        val aniListEntryId: Int? = null
    )
}