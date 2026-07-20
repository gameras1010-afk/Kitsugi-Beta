package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kitsugi.animelist.core.player.QualityDataHelper
import com.kitsugi.animelist.core.player.QualityPreference
import com.kitsugi.animelist.core.player.QualityProfile
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.utils.tvClickable

/**
 * T2.4 — Kalite Profili Seçim Diyaloğu.
 *
 * Kullanıcının tercih ettiği çözünürlük/kalite bandını seçmesini ve
 * isteğe bağlı olarak maksimum bit hızı belirlemesini sağlar.
 *
 * Seçilen profil [QualityProfile.serialize] ile string'e dönüştürülür
 * ve `AppSettings.qualityProfileJson` alanında saklanır.
 *
 * ### Kullanım
 * ```kotlin
 * QualityProfileDialog(
 *     currentProfile = QualityProfile.deserialize(appSettings.qualityProfileJson),
 *     onDismiss = { showQualityDialog = false },
 *     onProfileSelected = { profile ->
 *         scope.launch { dataStore.setQualityProfile(QualityProfile.serialize(profile)) }
 *     }
 * )
 * ```
 */
@Composable
fun QualityProfileDialog(
    currentProfile: QualityProfile,
    onDismiss: () -> Unit,
    onProfileSelected: (QualityProfile) -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    var selectedPreference by remember { mutableStateOf(currentProfile.preference) }
    var showBitrateSection by remember { mutableStateOf(currentProfile.maxBitrateKbps > 0) }
    var bitrateSliderValue by remember {
        mutableStateOf(
            if (currentProfile.maxBitrateKbps > 0) currentProfile.maxBitrateKbps.toFloat() else 10000f
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(KitsugiColors.Surface)
                .border(1.dp, KitsugiColors.Border, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ── Başlık ──────────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HighQuality,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Kalite Profili",
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tercih ettiğiniz çözünürlük bandını seçin",
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider(color = KitsugiColors.Border)

                // ── Kalite Seçenekleri ─────────────────────────────────────────
                Text(
                    text = "Çözünürlük Tercihi",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QualityPreference.entries.forEach { preference ->
                        QualityOptionRow(
                            preference    = preference,
                            isSelected    = selectedPreference == preference,
                            accentColor   = accentColor,
                            onClick       = { selectedPreference = preference }
                        )
                    }
                }

                HorizontalDivider(color = KitsugiColors.Border)

                // ── Bit Hızı Limiti ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Maksimum Bit Hızı",
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Hücresel ağda veri tasarrufu için sınır koy",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = showBitrateSection,
                        onCheckedChange = { showBitrateSection = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor
                        )
                    )
                }

                AnimatedVisibility(
                    visible = showBitrateSection,
                    enter = fadeIn(tween(200)) + expandVertically(),
                    exit = fadeOut(tween(200)) + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Limit",
                                color = KitsugiColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = formatBitrate(bitrateSliderValue.toInt()),
                                color = accentColor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = bitrateSliderValue,
                            onValueChange = { bitrateSliderValue = it },
                            valueRange = 500f..50000f,
                            steps = 49,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = KitsugiColors.Border
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("500 Kbps", color = KitsugiColors.TextMuted,
                                style = MaterialTheme.typography.bodySmall)
                            Text("50 Mbps", color = KitsugiColors.TextMuted,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // ── Eylem Butonları ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KitsugiColors.Border),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("İptal", color = KitsugiColors.TextSecondary)
                    }
                    Button(
                        onClick = {
                            val profile = QualityProfile(
                                preference     = selectedPreference,
                                maxBitrateKbps = if (showBitrateSection) bitrateSliderValue.toInt() else -1
                            )
                            onProfileSelected(profile)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Uygula", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityOptionRow(
    preference : QualityPreference,
    isSelected : Boolean,
    accentColor: Color,
    onClick    : () -> Unit
) {
    val containerColor = if (isSelected) accentColor.copy(alpha = 0.12f) else KitsugiColors.SurfaceSoft
    val borderColor    = if (isSelected) accentColor else KitsugiColors.Border

    val qualityIcon = when (preference) {
        QualityPreference.AUTO       -> Icons.Rounded.AutoAwesome
        QualityPreference.P1080      -> Icons.Rounded.Hd
        QualityPreference.P720       -> Icons.Rounded.HighQuality
        QualityPreference.P480       -> Icons.Rounded.SdCard
        QualityPreference.DATA_SAVER -> Icons.Rounded.SignalCellularAlt
    }

    val descriptionText = when (preference) {
        QualityPreference.AUTO       -> "Ağ koşullarına göre otomatik"
        QualityPreference.P1080      -> "Yüksek kalite, geniş bant gerektirir"
        QualityPreference.P720       -> "Dengeli kalite ve hız"
        QualityPreference.P480       -> "Orta kalite, düşük veri tüketimi"
        QualityPreference.DATA_SAVER -> "Minimum veri tüketimi"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .tvClickable(shape = RoundedCornerShape(12.dp)) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = qualityIcon,
            contentDescription = null,
            tint = if (isSelected) accentColor else KitsugiColors.TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = preference.label,
                color = if (isSelected) accentColor else KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text  = descriptionText,
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Seçili",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Bit hızını okunabilir formata çevirir: 1000 Kbps → 1 Mbps */
private fun formatBitrate(kbps: Int): String {
    return if (kbps >= 1000) {
        val mbps = kbps / 1000f
        if (mbps == mbps.toLong().toFloat()) "${mbps.toLong()} Mbps" else "%.1f Mbps".format(mbps)
    } else {
        "$kbps Kbps"
    }
}
