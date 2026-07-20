package com.kitsugi.animelist.ui.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.settings.SettingsDataStore
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val settingsDataStore = SettingsDataStore(context)

    // Callback to show messages on UI
    var onShowMessage: ((String) -> Unit)? = null

    fun updateProfileInfo(
        profileName: String,
        listTitle: String,
        anilistUsername: String
    ) {
        viewModelScope.launch {
            settingsDataStore.setProfileInfo(
                profileName = profileName,
                listTitle = listTitle,
                anilistUsername = anilistUsername
            )
            onShowMessage?.invoke("Profil bilgileri güncellendi")
        }
    }

    fun updateProfileImageUri(uri: String) {
        viewModelScope.launch {
            settingsDataStore.setProfileImageUri(uri)
            onShowMessage?.invoke("Profil resmi güncellendi")
        }
    }

    fun updateBannerImageUri(uri: String) {
        viewModelScope.launch {
            settingsDataStore.setBannerImageUri(uri)
            onShowMessage?.invoke("Banner resmi güncellendi")
        }
    }

    fun clearProfileImage() {
        viewModelScope.launch {
            settingsDataStore.clearProfileImageUri()
            onShowMessage?.invoke("Profil resmi kaldırıldı")
        }
    }

    fun clearBannerImage() {
        viewModelScope.launch {
            settingsDataStore.clearBannerImageUri()
            onShowMessage?.invoke("Banner resmi kaldırıldı")
        }
    }
}
