package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

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
    onSave: (
        profileName: String,
        listTitle: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    var profileName by rememberSaveable(initialProfileName) {
        mutableStateOf(initialProfileName)
    }

    var listTitle by rememberSaveable(initialListTitle) {
        mutableStateOf(initialListTitle)
    }

    val canSave = profileName.isNotBlank() && listTitle.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KitsugiColors.Surface,
        titleContentColor = KitsugiColors.TextPrimary,
        textContentColor = KitsugiColors.TextSecondary,
        shape = RoundedCornerShape(26.dp),
        title = {
            Text(
                text = "Profili Düzenle",
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Profil bilgilerini düzenle ve görsellerini seç.",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(14.dp))

                ProfileTextField(
                    value = profileName,
                    onValueChange = {
                        profileName = it
                    },
                    label = "Profil adı",
                    placeholder = "Profilim"
                )

                Spacer(modifier = Modifier.height(12.dp))

                ProfileTextField(
                    value = listTitle,
                    onValueChange = {
                        listTitle = it
                    },
                    label = "Liste başlığı",
                    placeholder = "Anime & Manga Listem"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Profil Görselleri",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                AccountActionRow(
                    title = "Profil resmi seç",
                    description = if (hasProfileImage) "Profil resmi seçildi" else "Galeriden profil resmi seç",
                    enabled = true,
                    onClick = onPickProfileImageClick
                )

                if (hasProfileImage) {
                    Spacer(modifier = Modifier.height(8.dp))

                    AccountActionRow(
                        title = "Profil resmini kaldır",
                        description = "Varsayılan harf avatarına dön",
                        enabled = true,
                        danger = true,
                        onClick = onClearProfileImageClick
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                AccountActionRow(
                    title = "Banner resmi seç",
                    description = if (hasBannerImage) "Banner resmi seçildi" else "Galeriden kapak/banner resmi seç",
                    enabled = true,
                    onClick = onPickBannerImageClick
                )

                if (hasBannerImage) {
                    Spacer(modifier = Modifier.height(8.dp))

                    AccountActionRow(
                        title = "Banner resmini kaldır",
                        description = "Varsayılan renkli bannera dön",
                        enabled = true,
                        danger = true,
                        onClick = onClearBannerImageClick
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        profileName.trim(),
                        listTitle.trim()
                    )
                }
            ) {
                Text(
                    text = "Kaydet",
                    color = if (canSave) {
                        accentColor
                    } else {
                        KitsugiColors.TextMuted
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Kapat",
                    color = KitsugiColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun AccountConnectionRow(
    title: String,
    description: String,
    connected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(KitsugiColors.SurfaceSoft)
            .tvClickable(shape = RoundedCornerShape(18.dp), onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = description,
                color = if (connected) {
                    KitsugiColors.AccentGreen
                } else {
                    KitsugiColors.TextSecondary
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (connected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
            )
        }

        Text(
            text = if (connected) {
                "Bağlantıyı Kes"
            } else {
                "Bağlan"
            },
            color = if (connected) {
                KitsugiColors.AccentRed
            } else {
                accentColor
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
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