package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiProfileEditDialog(
    initialProfileName: String,
    initialListTitle: String,
    hasProfileImage: Boolean,
    hasBannerImage: Boolean,
    onPickProfileImageClick: () -> Unit,
    onPickBannerImageClick: () -> Unit,
    onClearProfileImageClick: () -> Unit,
    onClearBannerImageClick: () -> Unit,
    onSave: (profileName: String, listTitle: String) -> Unit,
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
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()

    var profileName by rememberSaveable(initialProfileName) {
        mutableStateOf(initialProfileName)
    }

    var listTitle by rememberSaveable(initialListTitle) {
        mutableStateOf(initialListTitle)
    }

    val canSave = profileName.isNotBlank() && listTitle.isNotBlank()

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
                    text = "Hesap & Profil Ayarları",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = KitsugiColors.TextSecondary)
                }
            }
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = KitsugiColors.Surface,
                contentColor = accentColor
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    },
                    text = {
                        Text(
                            "Profil Düzenle",
                            fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                    text = {
                        Text(
                            "Hesap Bağlantıları",
                            fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Body
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) { page ->
            when (page) {
                0 -> EditProfileTab(
                    profileName = profileName,
                    onProfileNameChange = { profileName = it },
                    listTitle = listTitle,
                    onListTitleChange = { listTitle = it },
                    hasProfileImage = hasProfileImage,
                    hasBannerImage = hasBannerImage,
                    onPickProfileImageClick = onPickProfileImageClick,
                    onPickBannerImageClick = onPickBannerImageClick,
                    onClearProfileImageClick = onClearProfileImageClick,
                    onClearBannerImageClick = onClearBannerImageClick,
                    onSave = { onSave(profileName.trim(), listTitle.trim()) },
                    canSave = canSave,
                    accentColor = accentColor
                )
                1 -> AccountConnectionsTab(
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
private fun EditProfileTab(
    profileName: String,
    onProfileNameChange: (String) -> Unit,
    listTitle: String,
    onListTitleChange: (String) -> Unit,
    hasProfileImage: Boolean,
    hasBannerImage: Boolean,
    onPickProfileImageClick: () -> Unit,
    onPickBannerImageClick: () -> Unit,
    onClearProfileImageClick: () -> Unit,
    onClearBannerImageClick: () -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    accentColor: Color
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        KitsugiSettingsSection(title = "Profil Bilgileri") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileTextField(
                    value = profileName,
                    onValueChange = onProfileNameChange,
                    label = "Profil adı",
                    placeholder = "Profilim"
                )

                ProfileTextField(
                    value = listTitle,
                    onValueChange = onListTitleChange,
                    label = "Liste başlığı",
                    placeholder = "Anime & Manga Listem"
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    enabled = canSave,
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Değişiklikleri Kaydet", color = KitsugiColors.Background, fontWeight = FontWeight.Bold)
                }
            }
        }

        KitsugiSettingsSection(title = "Profil Görselleri") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccountActionRow(
                    title = "Profil resmi seç",
                    description = if (hasProfileImage) "Profil resmi seçildi" else "Galeriden profil resmi seç",
                    enabled = true,
                    onClick = onPickProfileImageClick
                )

                if (hasProfileImage) {
                    AccountActionRow(
                        title = "Profil resmini kaldır",
                        description = "Varsayılan harf avatarına dön",
                        enabled = true,
                        danger = true,
                        onClick = onClearProfileImageClick
                    )
                }

                AccountActionRow(
                    title = "Banner resmi seç",
                    description = if (hasBannerImage) "Banner resmi seçildi" else "Galeriden kapak/banner resmi seç",
                    enabled = true,
                    onClick = onPickBannerImageClick
                )

                if (hasBannerImage) {
                    AccountActionRow(
                        title = "Banner resmini kaldır",
                        description = "Varsayılan renkli bannera dön",
                        enabled = true,
                        danger = true,
                        onClick = onClearBannerImageClick
                    )
                }
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

@Composable
private fun AccountActionRow(
    title: String,
    description: String,
    enabled: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(KitsugiColors.SurfaceSoft)
            .tvClickable(
                shape = RoundedCornerShape(18.dp),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = if (enabled) {
                    KitsugiColors.TextPrimary
                } else {
                    KitsugiColors.TextMuted
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = description,
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = if (danger) {
                "Kaldır"
            } else if (enabled) {
                "Seç"
            } else {
                "Bekle"
            },
            color = if (danger) {
                KitsugiColors.AccentRed
            } else if (enabled) {
                accentColor
            } else {
                KitsugiColors.TextMuted
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    val accentColor = LocalKitsugiAccent.current

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        label = {
            Text(
                text = label,
                color = KitsugiColors.TextSecondary
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                color = KitsugiColors.TextMuted
            )
        },
        colors = TextFieldDefaults.colors(
            focusedTextColor = KitsugiColors.TextPrimary,
            unfocusedTextColor = KitsugiColors.TextPrimary,
            focusedContainerColor = KitsugiColors.SurfaceSoft,
            unfocusedContainerColor = KitsugiColors.SurfaceSoft,
            focusedIndicatorColor = accentColor,
            unfocusedIndicatorColor = KitsugiColors.Border,
            cursorColor = accentColor
        )
    )
}