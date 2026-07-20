package com.kitsugi.animelist.ui.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.auth.AniListImportManager
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import com.kitsugi.animelist.data.auth.ExternalListSyncManager
import com.kitsugi.animelist.data.auth.AniListSyncManager
import com.kitsugi.animelist.data.auth.MalSyncManager
import com.kitsugi.animelist.data.auth.MalImportManager
import com.kitsugi.animelist.data.auth.SimklImportManager
import com.kitsugi.animelist.data.local.MediaEntryRepository
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.model.MediaEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TvSyncEntryPoint {
    fun channelSyncService(): com.kitsugi.animelist.core.recommendations.TvChannelSyncService
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    var isAniListConnected by mutableStateOf(false)
        private set

    var isMalConnected by mutableStateOf(false)
        private set

    var isSimklConnected by mutableStateOf(false)
        private set

    var isSimklSessionExpired by mutableStateOf(false)
        private set

    var isAniListImportRunning by mutableStateOf(false)
        private set

    var isMalImportRunning by mutableStateOf(false)
        private set

    var isSimklImportRunning by mutableStateOf(false)
        private set

    var isCrossSyncRunning by mutableStateOf(false)
        private set

    var onShowMessage: ((String) -> Unit)? = null

    init {
        refreshAuthState()
    }

    fun refreshAuthState() {
        val state = ExternalAuthManager.getAuthState(context)
        isAniListConnected = state.isAniListConnected
        isMalConnected = state.isMalConnected
        isSimklConnected = state.isSimklConnected

        isSimklSessionExpired = context.getSharedPreferences("MyWebViewPrefs", Context.MODE_PRIVATE)
            .getBoolean("simkl_session_expired", false)
    }

    fun startExternalAuth(serviceName: String) {
        ExternalAuthManager.startAuthentication(
            context = context,
            serviceName = serviceName,
            onError = { message ->
                onShowMessage?.invoke(message)
            }
        )
    }

    fun disconnectExternalAccount(serviceName: String) {
        ExternalAuthManager.disconnectAccount(
            context = context,
            serviceName = serviceName
        )

        viewModelScope.launch {
            val settings = SettingsDataStore(context)
            when (serviceName) {
                "anilist" -> settings.clearAniListProfileInfo()
                "mal" -> settings.clearMalProfileInfo()
                "simkl" -> {
                    settings.clearSimklProfileInfo()
                }
            }
            refreshAuthState()

            // B1.12: Clear the TV launcher channel and watch-next recommendation when disconnecting
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context, TvSyncEntryPoint::class.java
                )
                entryPoint.channelSyncService().clearAll()
                // Reconcile again from DB to show remaining active sources if any
                entryPoint.channelSyncService().reconcileFromDatabase()
            } catch (e: Exception) {
                android.util.Log.w("AuthViewModel", "Failed to clear TV channel during disconnect", e)
            }
        }

        onShowMessage?.invoke(
            when (serviceName) {
                "anilist" -> "AniList bağlantısı kesildi"
                "simkl" -> "Simkl bağlantısı kesildi"
                else -> "MyAnimeList bağlantısı kesildi"
            }
        )
    }

    fun importAniListAnimeList(
        currentEntries: List<MediaEntry>,
        repository: MediaEntryRepository
    ) {
        val token = ExternalAuthManager.getAniListToken(context)

        if (token.isNullOrBlank()) {
            onShowMessage?.invoke("AniList token bulunamadı")
            return
        }

        if (isAniListImportRunning) {
            onShowMessage?.invoke("AniList import zaten çalışıyor")
            return
        }

        isAniListImportRunning = true
        onShowMessage?.invoke("AniList listesi getiriliyor...")

        viewModelScope.launch {
            val settingsDataStore = SettingsDataStore(context)

            runCatching {
                runCatching {
                    val profile = AniListImportManager.fetchUserProfile(token)
                    settingsDataStore.setAniListProfileInfo(
                        username = profile.name,
                        profileImageUri = profile.avatarUrl.orEmpty(),
                        bannerImageUri = profile.bannerUrl.orEmpty()
                    )
                }
                AniListImportManager.fetchAllLists(token)
            }.onSuccess { importedEntries ->
                repository.deleteBySource("anilist")
                repository.insertAll(importedEntries)

                onShowMessage?.invoke(
                    "${importedEntries.size} AniList kaydı başarıyla aktarıldı"
                )
            }.onFailure { error ->
                onShowMessage?.invoke(
                    error.message ?: "AniList içe aktarma başarısız"
                )
            }

            isAniListImportRunning = false
        }
    }

    fun importMalAnimeList(
        currentEntries: List<MediaEntry>,
        repository: MediaEntryRepository
    ) {
        if (isMalImportRunning) {
            onShowMessage?.invoke("MyAnimeList import zaten çalışıyor")
            return
        }

        isMalImportRunning = true
        onShowMessage?.invoke("MyAnimeList listesi getiriliyor...")

        viewModelScope.launch {
            val token = ExternalAuthManager.getOrRefreshMalToken(context)

            if (token.isNullOrBlank()) {
                onShowMessage?.invoke("MyAnimeList token bulunamadı")
                isMalImportRunning = false
                return@launch
            }

            val settingsDataStore = SettingsDataStore(context)

            runCatching {
                runCatching {
                    val profile = MalImportManager.fetchUserProfile(token)
                    settingsDataStore.setMalProfileInfo(
                        username = profile.name,
                        profileImageUri = profile.pictureUrl.orEmpty(),
                        bannerImageUri = ""
                    )
                }
                val showAdult = settingsDataStore.settingsFlow.first().showAdultContent
                MalImportManager.fetchAllLists(token, showAdult)
            }.onSuccess { importedEntries ->
                repository.deleteBySource("mal")
                repository.insertAll(importedEntries)

                onShowMessage?.invoke(
                    "${importedEntries.size} MyAnimeList kaydı başarıyla aktarıldı"
                )
            }.onFailure { error ->
                onShowMessage?.invoke(
                    error.message ?: "MyAnimeList içe aktarma başarısız"
                )
            }

            isMalImportRunning = false
        }
    }

    fun syncPlatforms(
        repository: MediaEntryRepository
    ) {
        if (isCrossSyncRunning) {
            onShowMessage?.invoke("Eşitleme zaten devam ediyor...")
            return
        }

        isCrossSyncRunning = true
        onShowMessage?.invoke("Çift yönlü eşitleme başlatıldı...")

        viewModelScope.launch {
            val aniListToken = ExternalAuthManager.getAniListToken(context)
            val malToken = ExternalAuthManager.getOrRefreshMalToken(context)

            if (aniListToken.isNullOrBlank() || malToken.isNullOrBlank()) {
                onShowMessage?.invoke("Bu işlemi gerçekleştirmek için hem AniList hem de MyAnimeList hesabınızın bağlı olması gerekir.")
                isCrossSyncRunning = false
                return@launch
            }

            val settingsDataStore = SettingsDataStore(context)
            runCatching {
                val showAdult = settingsDataStore.settingsFlow.first().showAdultContent
                val aniListEntries = AniListImportManager.fetchAllLists(aniListToken)
                val malEntries = MalImportManager.fetchAllLists(malToken, showAdult)

                val aniListByMalId = aniListEntries
                    .filter { it.malId != null && it.malId > 0 && it.malId < 100_000_000 }
                    .associateBy { it.malId!! }

                val malByMalId = malEntries
                    .filter { it.malId != null && it.malId > 0 && it.malId < 100_000_000 }
                    .associateBy { it.malId!! }

                val allMalIds = aniListByMalId.keys + malByMalId.keys
                var syncCount = 0

                val finalEntries = mutableListOf<MediaEntry>()

                allMalIds.forEach { malId ->
                    val aniListEntry = aniListByMalId[malId]
                    val malEntry = malByMalId[malId]

                    if (aniListEntry != null && malEntry != null) {
                        if (aniListEntry.status != malEntry.status ||
                            aniListEntry.progress != malEntry.progress ||
                            aniListEntry.score != malEntry.score) {
                            
                            if (aniListEntry.updatedAt >= malEntry.updatedAt) {
                                MalSyncManager.updateMalEntry(malToken, aniListEntry)
                                finalEntries.add(aniListEntry)
                            } else {
                                AniListSyncManager.updateAniListEntry(aniListToken, malEntry)
                                finalEntries.add(malEntry)
                            }
                            syncCount++
                        } else {
                            finalEntries.add(aniListEntry)
                        }
                    } else if (aniListEntry != null) {
                        MalSyncManager.updateMalEntry(malToken, aniListEntry)
                        finalEntries.add(aniListEntry)
                        syncCount++
                    } else if (malEntry != null) {
                        AniListSyncManager.updateAniListEntry(aniListToken, malEntry)
                        finalEntries.add(malEntry)
                        syncCount++
                    }
                }

                repository.deleteBySource("anilist")
                repository.deleteBySource("mal")
                repository.insertAll(finalEntries)

                onShowMessage?.invoke("$syncCount anime başarıyla eşitlendi!")
            }.onFailure { error ->
                onShowMessage?.invoke("Eşitleme sırasında hata oluştu: ${error.message}")
            }
            isCrossSyncRunning = false
        }
    }

    fun importSimklList(
        currentEntries: List<MediaEntry>,
        repository: MediaEntryRepository
    ) {
        val token = ExternalAuthManager.getSimklToken(context)

        if (token.isNullOrBlank()) {
            onShowMessage?.invoke("Simkl token bulunamadı")
            return
        }

        if (isSimklImportRunning) {
            onShowMessage?.invoke("Simkl import zaten çalışıyor")
            return
        }

        isSimklImportRunning = true
        onShowMessage?.invoke("Simkl listesi getiriliyor...")

        viewModelScope.launch {
            val settingsDataStore = SettingsDataStore(context)

            runCatching {
                runCatching {
                    val profile = SimklImportManager.fetchUserProfile(token)
                    settingsDataStore.setSimklProfileInfo(
                        username = profile.name,
                        profileImageUri = profile.avatarUrl.orEmpty(),
                        bannerImageUri = ""
                    )
                }
                SimklImportManager.fetchAllLists(token)
            }.onSuccess { importedEntries ->
                repository.deleteBySource("simkl")
                repository.insertAll(importedEntries)

                onShowMessage?.invoke(
                    "${importedEntries.size} Simkl kaydı başarıyla aktarıldı"
                )
            }.onFailure { error ->
                onShowMessage?.invoke(
                    error.message ?: "Simkl içe aktarma başarısız"
                )
            }

            isSimklImportRunning = false
        }
    }
}
