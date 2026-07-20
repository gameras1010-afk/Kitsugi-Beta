package com.kitsugi.animelist.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.SettingsInputHdmi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.components.KitsugiSettingsSection
import com.kitsugi.animelist.ui.components.KitsugiSettingsListItem
import com.kitsugi.animelist.ui.components.KitsugiSettingsSwitchItem
import com.kitsugi.animelist.ui.components.KitsugiSettingsDivider
import com.kitsugi.animelist.ui.components.KitsugiDropdownMenu
import com.kitsugi.animelist.ui.components.KitsugiDropdownItem
import com.kitsugi.animelist.ui.theme.KitsugiColors

/**
 * Oynatıcı Ayarları — Altyazı & Ses sekmesi.
 * Altyazı boyutu, rengi, kalınlık, kenarlık, ses güçlendirme ve gecikmeyi içerir.
 */
@Composable
internal fun PlayerSubtitleAudioTab(
    defaultSubtitleSize: Int,
    defaultSubtitleColor: Int,
    subtitleBold: Boolean,
    subtitleOutlineEnabled: Boolean,
    defaultAudioBoost: Float,
    defaultAudioDelayMs: Long,
    preferredSubtitleLanguages: String,
    addonSubtitleStartupMode: String,
    accentColor: Color,
    // T1.3 – Rota başına gecikme
    speakerDelayMs: Long = 0L,
    bluetoothDelayMs: Long = 0L,
    wiredDelayMs: Long = 0L,
    hdmiDelayMs: Long = 0L,
    activeAudioRoute: com.kitsugi.animelist.core.player.AudioRoute = com.kitsugi.animelist.core.player.AudioRoute.SPEAKER,
    onRouteDelayChanged: (speaker: Long, bluetooth: Long, wired: Long, hdmi: Long) -> Unit = { _, _, _, _ -> },
    onDefaultSubtitleSizeSelected: (Int) -> Unit,
    onDefaultSubtitleColorSelected: (Int) -> Unit,
    onSubtitleBoldChanged: (Boolean) -> Unit,
    onSubtitleOutlineEnabledChanged: (Boolean) -> Unit,
    onDefaultAudioBoostSelected: (Float) -> Unit,
    onDefaultAudioDelayMsSelected: (Long) -> Unit,
    onPreferredSubtitleLanguagesSelected: (String) -> Unit,
    onAddonSubtitleStartupModeSelected: (String) -> Unit
) {
    var sizeDropdownExpanded by remember { mutableStateOf(false) }
    var colorDropdownExpanded by remember { mutableStateOf(false) }
    var boostDropdownExpanded by remember { mutableStateOf(false) }
    var delayDropdownExpanded by remember { mutableStateOf(false) }
    var startupModeDropdownExpanded by remember { mutableStateOf(false) }

    val sizeOptions = listOf(
        12 to "12sp (Çok Küçük)",
        14 to "14sp (Küçük)",
        16 to "16sp (Normal)",
        18 to "18sp (Büyük)",
        20 to "20sp (Çok Büyük)",
        24 to "24sp (Devasa)"
    )

    val colorOptions = listOf(
        0xFFFFFFFF.toInt() to "Beyaz",
        0xFFFFFF00.toInt() to "Sarı",
        0xFF00FF00.toInt() to "Yeşil",
        0xFF00FFFF.toInt() to "Mavi",
        0xFFFF0000.toInt() to "Kırmızı"
    )

    val boostOptions = listOf(
        0.0f to "Normal (%0)",
        0.25f to "Düşük (%25)",
        0.5f to "Orta (%50)",
        0.75f to "Yüksek (%75)",
        1.0f to "Maksimum (%100)"
    )

    val delayOptions = listOf(
        0L to "Zamanında (0ms)",
        -100L to "-100 ms",
        -200L to "-200 ms",
        -300L to "-300 ms",
        -500L to "-500 ms",
        100L to "+100 ms",
        200L to "+200 ms",
        300L to "+300 ms",
        500L to "+500 ms"
    )

    // (Dil seçimi artık checkbox paneli ile yapılıyor — prefLangOptions kaldırıldı)

    val startupModeOptions = listOf(
        "ALL_SUBTITLES" to "Tüm Altyazıları Yükle",
        "PREFERRED_ONLY" to "Yalnızca Tercih Edilen Dilleri Yükle"
    )

    val currentSizeName = sizeOptions.find { it.first == defaultSubtitleSize }?.second ?: "16sp (Normal)"
    val currentColorName = colorOptions.find { it.first == defaultSubtitleColor }?.second ?: "Beyaz"
    val currentBoostName = boostOptions.find { it.first == defaultAudioBoost }?.second ?: "Normal (%0)"
    val currentDelayName = delayOptions.find { it.first == defaultAudioDelayMs }?.second ?: "Zamanında (0ms)"
    val currentStartupModeName = startupModeOptions.find { it.first == addonSubtitleStartupMode }?.second ?: "Yalnızca Tercih Edilen Dilleri Yükle"

    LazyColumn(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Altyazı Görünümü
        item {
            KitsugiSettingsSection(
                title = "Altyazı Görünümü",
                subtitle = "Altyazı boyutunu, rengini ve kalınlık/kenarlık stillerini yapılandırın."
            ) {
                // Size
                Box {
                    KitsugiSettingsListItem(
                        title = "Varsayılan Altyazı Boyutu",
                        description = "Altyazı metninin boyutu",
                        value = currentSizeName,
                        icon = Icons.Rounded.Subtitles,
                        iconColor = accentColor,
                        onClick = { sizeDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = sizeDropdownExpanded, onDismissRequest = { sizeDropdownExpanded = false }) {
                        sizeOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == defaultSubtitleSize,
                                onClick = {
                                    onDefaultSubtitleSizeSelected(option.first)
                                    sizeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Color
                Box {
                    KitsugiSettingsListItem(
                        title = "Varsayılan Altyazı Rengi",
                        description = "Altyazı metninin ana rengi",
                        value = currentColorName,
                        icon = Icons.Rounded.Subtitles,
                        iconColor = accentColor,
                        onClick = { colorDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = colorDropdownExpanded, onDismissRequest = { colorDropdownExpanded = false }) {
                        colorOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == defaultSubtitleColor,
                                onClick = {
                                    onDefaultSubtitleColorSelected(option.first)
                                    colorDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Bold
                KitsugiSettingsSwitchItem(
                    title = "Altyazı Kalın Yazı",
                    description = "Altyazı metnini kalınlaştırır.",
                    icon = Icons.Rounded.Subtitles,
                    iconColor = accentColor,
                    checked = subtitleBold,
                    onCheckedChange = onSubtitleBoldChanged
                )

                KitsugiSettingsDivider()

                // Outline
                KitsugiSettingsSwitchItem(
                    title = "Altyazı Kenarlığı (Outline)",
                    description = "Okunabilirliği artırmak için siyah kenarlık ekler.",
                    icon = Icons.Rounded.Subtitles,
                    iconColor = accentColor,
                    checked = subtitleOutlineEnabled,
                    onCheckedChange = onSubtitleOutlineEnabledChanged
                )
            }
        }

        // Ses & Altyazı Tercihleri
        item {
            KitsugiSettingsSection(
                title = "Ses & Altyazı Tercihleri",
                subtitle = "Ses güçlendirme, gecikme ve varsayılan dil tercihlerini özelleştirin."
            ) {
                // Audio Boost
                Box {
                    KitsugiSettingsListItem(
                        title = "Varsayılan Ses Güçlendirme (Audio Boost)",
                        description = "Düşük sesli videoların ses seviyesini artırır",
                        value = currentBoostName,
                        icon = Icons.Rounded.VolumeUp,
                        iconColor = accentColor,
                        onClick = { boostDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = boostDropdownExpanded, onDismissRequest = { boostDropdownExpanded = false }) {
                        boostOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == defaultAudioBoost,
                                onClick = {
                                    onDefaultAudioBoostSelected(option.first)
                                    boostDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Audio Delay
                Box {
                    KitsugiSettingsListItem(
                        title = "Varsayılan Ses Gecikmesi",
                        description = "Ses ve görüntü senkronizasyonu için genel gecikme süresi",
                        value = currentDelayName,
                        icon = Icons.Rounded.VolumeUp,
                        iconColor = accentColor,
                        onClick = { delayDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = delayDropdownExpanded, onDismissRequest = { delayDropdownExpanded = false }) {
                        delayOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == defaultAudioDelayMs,
                                onClick = {
                                    onDefaultAudioDelayMsSelected(option.first)
                                    delayDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // ─── Altyazı Dil Tercihi — çoklu seçim ────────────────
                // Seçili dilleri virgülle ayrılmış Set olarak yönet
                val selectedLangs = remember(preferredSubtitleLanguages) {
                    preferredSubtitleLanguages.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
                }

                val availableLangs = listOf(
                    "tr" to "🇹🇷 Türkçe",
                    "en" to "🇬🇧 İngilizce",
                    "ja" to "🇯🇵 Japonca",
                    "fr" to "🇫🇷 Fransızca",
                    "de" to "🇩🇪 Almanca",
                    "es" to "🇪🇸 İspanyolca",
                    "pt" to "🇵🇹 Portekizce",
                    "ar" to "🇸🇦 Arapça",
                    "ko" to "🇰🇷 Korece",
                    "zh" to "🇨🇳 Çince"
                )

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Subtitles,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tercih Edilen Altyazı Dilleri",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = KitsugiColors.TextPrimary
                            )
                            Text(
                                text = "Sadece işaretlenen dillerdeki altyazılar yüklenir",
                                style = MaterialTheme.typography.bodySmall,
                                color = KitsugiColors.TextSecondary
                            )
                        }
                    }

                    availableLangs.forEach { (code, label) ->
                        val isChecked = selectedLangs.contains(code)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 34.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val updated = selectedLangs.toMutableSet()
                                    if (checked) updated.add(code) else updated.remove(code)
                                    // En az bir dil seçili olmalı
                                    if (updated.isNotEmpty()) {
                                        onPreferredSubtitleLanguagesSelected(updated.joinToString(","))
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentColor,
                                    uncheckedColor = KitsugiColors.TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isChecked) KitsugiColors.TextPrimary else KitsugiColors.TextSecondary
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // ─── Altyazı Yükleme Modu ──────────────────────────────
                Box {
                    KitsugiSettingsListItem(
                        title = "Altyazı Yükleme Modu",
                        description = "Videoyu açarken hangi altyazıların yükleneceği",
                        value = currentStartupModeName,
                        icon = Icons.Rounded.Subtitles,
                        iconColor = accentColor,
                        onClick = { startupModeDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = startupModeDropdownExpanded, onDismissRequest = { startupModeDropdownExpanded = false }) {
                        startupModeOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == addonSubtitleStartupMode,
                                onClick = {
                                    onAddonSubtitleStartupModeSelected(option.first)
                                    startupModeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Rota Bazlı Ses Gecikmesi
        item {
            val routeLabel = when (activeAudioRoute) {
                com.kitsugi.animelist.core.player.AudioRoute.BLUETOOTH -> "🎧 Bluetooth"
                com.kitsugi.animelist.core.player.AudioRoute.WIRED     -> "🔌 Kablolu"
                com.kitsugi.animelist.core.player.AudioRoute.HDMI      -> "📺 HDMI"
                com.kitsugi.animelist.core.player.AudioRoute.SPEAKER   -> "🔊 Hoparlör"
                com.kitsugi.animelist.core.player.AudioRoute.OTHER     -> "🔈 Diğer"
            }
            KitsugiSettingsSection(
                title = "Rota Bazlı Ses Gecikmesi",
                subtitle = "Her ses çıkışı için farklı gecikme tanımlayabilirsiniz (ör. BT kulaklık için +150 ms). Aktif çıkış: $routeLabel"
            ) {
                val routeDelayOptions = listOf(
                    -500L to "-500 ms",
                    -300L to "-300 ms",
                    -200L to "-200 ms",
                    -150L to "-150 ms",
                    -100L to "-100 ms",
                    0L to "Zamanında (0 ms)",
                    100L to "+100 ms",
                    150L to "+150 ms",
                    200L to "+200 ms",
                    300L to "+300 ms",
                    500L to "+500 ms"
                )

                // Speaker
                var routeSpeakerExpanded by remember { mutableStateOf(false) }
                val isSpeakerActive = activeAudioRoute == com.kitsugi.animelist.core.player.AudioRoute.SPEAKER
                Box {
                    KitsugiSettingsListItem(
                        title = "Hoparlör Gecikmesi",
                        description = "Cihaz hoparlörleri için ses kaydırma",
                        value = routeDelayOptions.find { it.first == speakerDelayMs }?.second ?: "${speakerDelayMs} ms",
                        icon = Icons.Rounded.VolumeUp,
                        iconColor = if (isSpeakerActive) accentColor else KitsugiColors.TextSecondary,
                        onClick = { routeSpeakerExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = routeSpeakerExpanded, onDismissRequest = { routeSpeakerExpanded = false }) {
                        routeDelayOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == speakerDelayMs,
                                onClick = {
                                    onRouteDelayChanged(option.first, bluetoothDelayMs, wiredDelayMs, hdmiDelayMs)
                                    routeSpeakerExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Bluetooth
                var routeBtExpanded by remember { mutableStateOf(false) }
                val isBtActive = activeAudioRoute == com.kitsugi.animelist.core.player.AudioRoute.BLUETOOTH
                Box {
                    KitsugiSettingsListItem(
                        title = "Bluetooth Gecikmesi",
                        description = "Kablosuz kulaklık/hoparlör için ses kaydırma",
                        value = routeDelayOptions.find { it.first == bluetoothDelayMs }?.second ?: "${bluetoothDelayMs} ms",
                        icon = Icons.Rounded.VolumeUp,
                        iconColor = if (isBtActive) accentColor else KitsugiColors.TextSecondary,
                        onClick = { routeBtExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = routeBtExpanded, onDismissRequest = { routeBtExpanded = false }) {
                        routeDelayOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == bluetoothDelayMs,
                                onClick = {
                                    onRouteDelayChanged(speakerDelayMs, option.first, wiredDelayMs, hdmiDelayMs)
                                    routeBtExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Wired
                var routeWiredExpanded by remember { mutableStateOf(false) }
                val isWiredActive = activeAudioRoute == com.kitsugi.animelist.core.player.AudioRoute.WIRED
                Box {
                    KitsugiSettingsListItem(
                        title = "Kablolu Kulaklık Gecikmesi",
                        description = "3.5mm jak girişi için ses kaydırma",
                        value = routeDelayOptions.find { it.first == wiredDelayMs }?.second ?: "${wiredDelayMs} ms",
                        icon = Icons.Rounded.VolumeUp,
                        iconColor = if (isWiredActive) accentColor else KitsugiColors.TextSecondary,
                        onClick = { routeWiredExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = routeWiredExpanded, onDismissRequest = { routeWiredExpanded = false }) {
                        routeDelayOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == wiredDelayMs,
                                onClick = {
                                    onRouteDelayChanged(speakerDelayMs, bluetoothDelayMs, option.first, hdmiDelayMs)
                                    routeWiredExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // HDMI
                var routeHdmiExpanded by remember { mutableStateOf(false) }
                val isHdmiActive = activeAudioRoute == com.kitsugi.animelist.core.player.AudioRoute.HDMI
                Box {
                    KitsugiSettingsListItem(
                        title = "HDMI / ARC Gecikmesi",
                        description = "HDMI ses çıkışları veya TV ARC bağlantısı için ses kaydırma",
                        value = routeDelayOptions.find { it.first == hdmiDelayMs }?.second ?: "${hdmiDelayMs} ms",
                        icon = Icons.Rounded.SettingsInputHdmi,
                        iconColor = if (isHdmiActive) accentColor else KitsugiColors.TextSecondary,
                        onClick = { routeHdmiExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = routeHdmiExpanded, onDismissRequest = { routeHdmiExpanded = false }) {
                        routeDelayOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == hdmiDelayMs,
                                onClick = {
                                    onRouteDelayChanged(speakerDelayMs, bluetoothDelayMs, wiredDelayMs, option.first)
                                    routeHdmiExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
