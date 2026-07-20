package com.kitsugi.animelist.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kitsugi.animelist.ui.components.KitsugiSettingsDivider
import com.kitsugi.animelist.ui.components.KitsugiSettingsItem
import com.kitsugi.animelist.ui.components.KitsugiSettingsSection
import com.kitsugi.animelist.ui.components.KitsugiTvQrLoginDialog
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun SettingsProfileSection(
    profileName: String,
    onEditProfileClick: () -> Unit,
    isAniListConnected: Boolean,
    anilistUsername: String,
    isAniListImportRunning: Boolean,
    onAniListImportClick: () -> Unit,
    onAniListAuthClick: () -> Unit,
    isMalConnected: Boolean,
    malUsername: String,
    isMalImportRunning: Boolean,
    onMalImportClick: () -> Unit,
    onMalAuthClick: () -> Unit,
    isSimklConnected: Boolean,
    simklUsername: String,
    isSimklImportRunning: Boolean,
    isSimklSessionExpired: Boolean = false,
    onSimklImportClick: () -> Unit,
    onSimklAuthClick: () -> Unit,
    isCrossSyncRunning: Boolean,
    onCrossSyncClick: () -> Unit
) {
    val isTv = LocalIsTv.current
    // TV: single QR dialog that handles all three services
    var showTvQrDialog by remember { mutableStateOf(false) }

    KitsugiSettingsSection(
        title = "Hesap & Profil"
    ) {
        // 1. Profil Bilgileri
        KitsugiSettingsItem(
            title = "Profil Bilgileri",
            description = "$profileName • Yerel profil ayarlarını düzenle",
            icon = Icons.Rounded.Person,
            iconColor = KitsugiColors.AccentBlue,
            onClick = onEditProfileClick
        )

        KitsugiSettingsDivider()

        // 2. AniList
        if (isAniListConnected) {
            KitsugiSettingsItem(
                title = "AniList Hesabı ($anilistUsername)",
                description = if (isAniListImportRunning) "Senkronize ediliyor..." else "Listenizi AniList'ten içe aktarın",
                icon = Icons.Rounded.Sync,
                iconColor = KitsugiColors.AccentGreen,
                onClick = { if (!isAniListImportRunning) onAniListImportClick() }
            )
            KitsugiSettingsDivider()
            KitsugiSettingsItem(
                title = "AniList Bağlantısını Kes",
                description = "Hesabınızı uygulamadan kaldırır",
                icon = Icons.Rounded.LinkOff,
                iconColor = KitsugiColors.AccentRed,
                onClick = onAniListAuthClick
            )
        } else {
            KitsugiSettingsItem(
                title = "AniList Hesabını Bağla",
                description = if (isTv) "TV'de QR kod ile giriş yap" else "Giriş yap ve listenizi eşitle",
                icon = Icons.Rounded.Link,
                iconColor = KitsugiColors.AccentBlue,
                onClick = {
                    if (isTv) showTvQrDialog = true
                    else onAniListAuthClick()
                }
            )
        }

        KitsugiSettingsDivider()

        // 3. MyAnimeList
        if (isMalConnected) {
            KitsugiSettingsItem(
                title = "MyAnimeList Hesabı ($malUsername)",
                description = if (isMalImportRunning) "Senkronize ediliyor..." else "Listenizi MyAnimeList'ten içe aktarın",
                icon = Icons.Rounded.Sync,
                iconColor = KitsugiColors.AccentGreen,
                onClick = { if (!isMalImportRunning) onMalImportClick() }
            )
            KitsugiSettingsDivider()
            KitsugiSettingsItem(
                title = "MyAnimeList Bağlantısını Kes",
                description = "Hesabınızı uygulamadan kaldırır",
                icon = Icons.Rounded.LinkOff,
                iconColor = KitsugiColors.AccentRed,
                onClick = onMalAuthClick
            )
        } else {
            KitsugiSettingsItem(
                title = "MyAnimeList Hesabını Bağla",
                description = if (isTv) "TV'de QR kod ile giriş yap" else "Giriş yap ve listenizi eşitle",
                icon = Icons.Rounded.Link,
                iconColor = KitsugiColors.AccentBlue,
                onClick = {
                    if (isTv) showTvQrDialog = true
                    else onMalAuthClick()
                }
            )
        }

        KitsugiSettingsDivider()

        // 4. Simkl
        if (isSimklConnected) {
            KitsugiSettingsItem(
                title = "Simkl Hesabı ($simklUsername)",
                description = if (isSimklImportRunning) "Senkronize ediliyor..." else "Listenizi Simkl'dan içe aktarın",
                icon = Icons.Rounded.Sync,
                iconColor = KitsugiColors.AccentGreen,
                onClick = { if (!isSimklImportRunning) onSimklImportClick() }
            )
            KitsugiSettingsDivider()
            KitsugiSettingsItem(
                title = "Simkl Bağlantısını Kes",
                description = "Hesabınızı uygulamadan kaldırır",
                icon = Icons.Rounded.LinkOff,
                iconColor = KitsugiColors.AccentRed,
                onClick = onSimklAuthClick
            )
        } else if (isSimklSessionExpired) {
            KitsugiSettingsItem(
                title = "Simkl Bağlantısını Yenile (Süresi Doldu)",
                description = "Oturum süresi doldu. Tekrar giriş yapın.",
                icon = Icons.Rounded.Warning,
                iconColor = KitsugiColors.AccentRed,
                onClick = {
                    if (isTv) showTvQrDialog = true
                    else onSimklAuthClick()
                }
            )
        } else {
            KitsugiSettingsItem(
                title = "Simkl Hesabını Bağla",
                description = if (isTv) "TV'de QR kod ile giriş yap" else "Giriş yap, film, dizi ve animelerini eşitle",
                icon = Icons.Rounded.Link,
                iconColor = KitsugiColors.AccentBlue,
                onClick = {
                    if (isTv) showTvQrDialog = true
                    else onSimklAuthClick()
                }
            )
        }

        // 5. Çift Yönlü Eşitleme
        if (isAniListConnected && isMalConnected) {
            KitsugiSettingsDivider()
            KitsugiSettingsItem(
                title = "Hesapları Birbiriyle Eşitle",
                description = if (isCrossSyncRunning) "Eşitleme yapılıyor..." else "AniList ve MyAnimeList verilerini karşılıklı güncelleyin",
                icon = Icons.Rounded.Cached,
                iconColor = KitsugiColors.AccentOrange,
                onClick = { if (!isCrossSyncRunning) onCrossSyncClick() }
            )
        }
    }

    // TV: QR dialog for all three auth services
    if (showTvQrDialog) {
        KitsugiTvQrLoginDialog(
            onDismiss = { showTvQrDialog = false }
        )
    }
}
