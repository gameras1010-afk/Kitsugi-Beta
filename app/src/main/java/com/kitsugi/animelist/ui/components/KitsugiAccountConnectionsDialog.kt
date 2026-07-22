package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalIsTv

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiAccountConnectionsDialog(
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
    isSimklConnected: Boolean = false,
    simklUsername: String = "",
    isSimklImportRunning: Boolean = false,
    isSimklSessionExpired: Boolean = false,
    onSimklImportClick: () -> Unit = {},
    onSimklAuthClick: () -> Unit = {},
    isCrossSyncRunning: Boolean = false,
    onCrossSyncClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    KitsugiSheetOrDialog(
        onDismiss = onDismiss,
        heightFraction = 0.85f
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hesap Bağlantıları",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = KitsugiColors.TextSecondary)
                }
            }
        }

        // Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            AccountConnectionsTab(
                isAniListConnected = isAniListConnected,
                anilistUsername = anilistUsername,
                isAniListImportRunning = isAniListImportRunning,
                onAniListImportClick = onAniListImportClick,
                onAniListAuthClick = onAniListAuthClick,
                isMalConnected = isMalConnected,
                malUsername = malUsername,
                isMalImportRunning = isMalImportRunning,
                onMalImportClick = onMalImportClick,
                onMalAuthClick = onMalAuthClick,
                isSimklConnected = isSimklConnected,
                simklUsername = simklUsername,
                isSimklImportRunning = isSimklImportRunning,
                isSimklSessionExpired = isSimklSessionExpired,
                onSimklImportClick = onSimklImportClick,
                onSimklAuthClick = onSimklAuthClick,
                isCrossSyncRunning = isCrossSyncRunning,
                onCrossSyncClick = onCrossSyncClick,
                accentColor = accentColor
            )
        }

        // Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("Tamam", color = accentColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AccountConnectionsTab(
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
    isSimklSessionExpired: Boolean,
    onSimklImportClick: () -> Unit,
    onSimklAuthClick: () -> Unit,
    isCrossSyncRunning: Boolean,
    onCrossSyncClick: () -> Unit,
    accentColor: Color
) {
    val scrollState = rememberScrollState()
    val isTv = LocalIsTv.current
    var showTvQrDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AniList
        KitsugiSettingsSection(title = "AniList Hesabı") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
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
            }
        }

        // MyAnimeList
        KitsugiSettingsSection(title = "MyAnimeList Hesabı") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
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
            }
        }

        // Simkl
        KitsugiSettingsSection(title = "Simkl Hesabı") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
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
            }
        }

        // Çift Yönlü Eşitleme
        if (isAniListConnected && isMalConnected) {
            KitsugiSettingsSection(title = "Eşitleme") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    KitsugiSettingsItem(
                        title = "Hesapları Birbiriyle Eşitle",
                        description = if (isCrossSyncRunning) "Eşitleme yapılıyor..." else "AniList ve MyAnimeList verilerini karşılıklı güncelleyin",
                        icon = Icons.Rounded.Cached,
                        iconColor = KitsugiColors.AccentOrange,
                        onClick = { if (!isCrossSyncRunning) onCrossSyncClick() }
                    )
                }
            }
        }
    }

    if (showTvQrDialog) {
        KitsugiTvQrLoginDialog(
            onDismiss = { showTvQrDialog = false }
        )
    }
}
