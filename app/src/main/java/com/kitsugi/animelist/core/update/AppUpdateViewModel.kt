package com.kitsugi.animelist.core.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class UpdateAvailable(val release: AppRelease) : UpdateUiState()
    data class Downloading(
        val release: AppRelease,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateUiState()
    data class ReadyToInstall(val release: AppRelease, val apkFile: File) : UpdateUiState()
    data class Failed(val message: String) : UpdateUiState()
}

class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KitsugiUpdateRepository()
    private val installerHelper = ApkInstallerHelper(application)

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var currentRelease: AppRelease? = null

    fun checkForUpdates(silent: Boolean = true) {
        if (_uiState.value is UpdateUiState.Downloading || _uiState.value is UpdateUiState.ReadyToInstall) {
            return
        }

        viewModelScope.launch {
            if (!silent) _uiState.value = UpdateUiState.Checking

            val result = repository.checkForUpdate()
            result.onSuccess { release ->
                if (release != null) {
                    currentRelease = release
                    _uiState.value = UpdateUiState.UpdateAvailable(release)
                } else {
                    if (!silent) _uiState.value = UpdateUiState.Idle
                }
            }.onFailure { error ->
                if (!silent) {
                    _uiState.value = UpdateUiState.Failed(error.localizedMessage ?: "Güncelleme kontrolü başarısız.")
                }
            }
        }
    }

    fun startDownloadAndInstall() {
        val release = currentRelease ?: return

        if (!installerHelper.canInstallPackages()) {
            installerHelper.openInstallPermissionSettings()
        }

        viewModelScope.launch {
            installerHelper.downloadApk(release.downloadUrl).collect { downloadState ->
                when (downloadState) {
                    is DownloadState.Idle -> {}
                    is DownloadState.Progress -> {
                        _uiState.value = UpdateUiState.Downloading(
                            release = release,
                            progress = downloadState.percentage,
                            downloadedBytes = downloadState.downloadedBytes,
                            totalBytes = downloadState.totalBytes
                        )
                    }
                    is DownloadState.Finished -> {
                        _uiState.value = UpdateUiState.ReadyToInstall(release, downloadState.file)
                        installerHelper.installApk(downloadState.file)
                    }
                    is DownloadState.Error -> {
                        _uiState.value = UpdateUiState.Failed(downloadState.message)
                    }
                }
            }
        }
    }

    fun retryInstall() {
        val state = _uiState.value
        if (state is UpdateUiState.ReadyToInstall) {
            if (!installerHelper.canInstallPackages()) {
                installerHelper.openInstallPermissionSettings()
            }
            installerHelper.installApk(state.apkFile)
        }
    }

    fun dismissUpdate() {
        _uiState.value = UpdateUiState.Idle
    }
}
