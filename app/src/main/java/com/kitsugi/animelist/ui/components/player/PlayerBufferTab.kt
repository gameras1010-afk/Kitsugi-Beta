package com.kitsugi.animelist.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
 * Oynatıcı Ayarları — Gelişmiş Arabellek (Buffer) sekmesi.
 * ExoPlayer arabellek parametrelerini özelleştirmeyi sağlar.
 */
@Composable
internal fun PlayerBufferTab(
    minBufferMs: Int,
    maxBufferMs: Int,
    bufferForPlaybackMs: Int,
    bufferForPlaybackAfterRebufferMs: Int,
    backBufferDurationMs: Int,
    parallelRangeEnabled: Boolean,
    accentColor: Color,
    onBufferSettingsChanged: (min: Int, max: Int, playback: Int, rebuffer: Int, back: Int) -> Unit,
    onParallelRangeEnabledChanged: (Boolean) -> Unit
) {
    var minExp by remember { mutableStateOf(false) }
    var maxExp by remember { mutableStateOf(false) }
    var playExp by remember { mutableStateOf(false) }
    var rebExp by remember { mutableStateOf(false) }
    var backExp by remember { mutableStateOf(false) }

    val minOptions = listOf(
        10000 to "10 Saniye",
        15000 to "15 Saniye (Varsayılan)",
        30000 to "30 Saniye",
        45000 to "45 Saniye",
        60000 to "60 Saniye",
        90000 to "90 Saniye"
    )

    val maxOptions = listOf(
        30000 to "30 Saniye",
        45000 to "45 Saniye (Varsayılan)",
        60000 to "60 Saniye",
        90000 to "90 Saniye",
        120000 to "120 Saniye",
        240000 to "240 Saniye"
    )

    val playOptions = listOf(
        1000 to "1 Saniye",
        2000 to "2 Saniye",
        3000 to "3 Saniye",
        5000 to "5 Saniye (Varsayılan)",
        8000 to "8 Saniye",
        10000 to "10 Saniye"
    )

    val rebOptions = listOf(
        1000 to "1 Saniye",
        2000 to "2 Saniye",
        3000 to "3 Saniye (Varsayılan)",
        5000 to "5 Saniye",
        8000 to "8 Saniye"
    )

    val backOptions = listOf(
        0 to "Devre Dışı",
        5000 to "5 Saniye",
        10000 to "10 Saniye",
        15000 to "15 Saniye",
        30000 to "30 Saniye",
        60000 to "60 Saniye"
    )

    val currentMinStr = minOptions.find { it.first == minBufferMs }?.second ?: "${minBufferMs / 1000} Saniye"
    val currentMaxStr = maxOptions.find { it.first == maxBufferMs }?.second ?: "${maxBufferMs / 1000} Saniye"
    val currentPlayStr = playOptions.find { it.first == bufferForPlaybackMs }?.second ?: "${bufferForPlaybackMs / 1000} Saniye"
    val currentRebStr = rebOptions.find { it.first == bufferForPlaybackAfterRebufferMs }?.second ?: "${bufferForPlaybackAfterRebufferMs / 1000} Saniye"
    val currentBackStr = backOptions.find { it.first == backBufferDurationMs }?.second ?: "${backBufferDurationMs / 1000} Saniye"

    LazyColumn(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            KitsugiSettingsSection(
                title = "Gelişmiş Arabellek Yönetimi",
                subtitle = "Yavaş bağlantılarda takılmaları önlemek veya bellek kullanımını azaltmak için ExoPlayer arabelleğini özelleştirin."
            ) {
                // Minimum Arabellek
                Box {
                    KitsugiSettingsListItem(
                        title = "Minimum Arabellek (Min Buffer)",
                        description = "Gerekli minimum arabellek süresi",
                        value = currentMinStr,
                        icon = Icons.Rounded.NetworkCheck,
                        iconColor = accentColor,
                        onClick = { minExp = true }
                    )
                    KitsugiDropdownMenu(expanded = minExp, onDismissRequest = { minExp = false }) {
                        minOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == minBufferMs,
                                onClick = {
                                    onBufferSettingsChanged(option.first, maxBufferMs, bufferForPlaybackMs, bufferForPlaybackAfterRebufferMs, backBufferDurationMs)
                                    minExp = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Maksimum Arabellek
                Box {
                    KitsugiSettingsListItem(
                        title = "Maksimum Arabellek (Max Buffer)",
                        description = "İzin verilen maksimum arabellek süresi",
                        value = currentMaxStr,
                        icon = Icons.Rounded.NetworkCheck,
                        iconColor = accentColor,
                        onClick = { maxExp = true }
                    )
                    KitsugiDropdownMenu(expanded = maxExp, onDismissRequest = { maxExp = false }) {
                        maxOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == maxBufferMs,
                                onClick = {
                                    onBufferSettingsChanged(minBufferMs, option.first, bufferForPlaybackMs, bufferForPlaybackAfterRebufferMs, backBufferDurationMs)
                                    maxExp = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Oynatmayı Başlatma Süresi
                Box {
                    KitsugiSettingsListItem(
                        title = "Oynatmayı Başlatma Süresi",
                        description = "Oynatmayı başlatmadan önce gereken arabellek süresi",
                        value = currentPlayStr,
                        icon = Icons.Rounded.NetworkCheck,
                        iconColor = accentColor,
                        onClick = { playExp = true }
                    )
                    KitsugiDropdownMenu(expanded = playExp, onDismissRequest = { playExp = false }) {
                        playOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == bufferForPlaybackMs,
                                onClick = {
                                    onBufferSettingsChanged(minBufferMs, maxBufferMs, option.first, bufferForPlaybackAfterRebufferMs, backBufferDurationMs)
                                    playExp = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Takılma Sonrası Başlatma Süresi
                Box {
                    KitsugiSettingsListItem(
                        title = "Takılma Sonrası Başlatma Süresi",
                        description = "Arabellek boşaldığında oynatmanın devam etmesi için gereken süre",
                        value = currentRebStr,
                        icon = Icons.Rounded.NetworkCheck,
                        iconColor = accentColor,
                        onClick = { rebExp = true }
                    )
                    KitsugiDropdownMenu(expanded = rebExp, onDismissRequest = { rebExp = false }) {
                        rebOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == bufferForPlaybackAfterRebufferMs,
                                onClick = {
                                    onBufferSettingsChanged(minBufferMs, maxBufferMs, bufferForPlaybackMs, option.first, backBufferDurationMs)
                                    rebExp = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Geriye Dönük Arabellek
                Box {
                    KitsugiSettingsListItem(
                        title = "Geriye Dönük Arabellek (Back Buffer)",
                        description = "Geri sarıldığında anında oynatma için saklanan arabellek süresi",
                        value = currentBackStr,
                        icon = Icons.Rounded.NetworkCheck,
                        iconColor = accentColor,
                        onClick = { backExp = true }
                    )
                    KitsugiDropdownMenu(expanded = backExp, onDismissRequest = { backExp = false }) {
                        backOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == backBufferDurationMs,
                                onClick = {
                                    onBufferSettingsChanged(minBufferMs, maxBufferMs, bufferForPlaybackMs, bufferForPlaybackAfterRebufferMs, option.first)
                                    backExp = false
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            KitsugiSettingsSection(
                title = "Paralel İndirme",
                subtitle = "MKV ve MP4 gibi progressive içerikler için çok bağlantılı chunk indirme. HLS/DASH çökenez; uyumlu olmayan sunucularda otomatik olarak tek bağlantı moduna geçer."
            ) {
                KitsugiSettingsSwitchItem(
                    title = "Paralel İndirme",
                    description = if (parallelRangeEnabled) "Etkin — 3 eşzamanlı bağlantı, 512 KB chunk" else "Devre Dışı — Tek bağlantı modu",
                    icon = Icons.Rounded.SystemUpdateAlt,
                    iconColor = accentColor,
                    checked = parallelRangeEnabled,
                    onCheckedChange = onParallelRangeEnabledChanged
                )
            }
        }
    }
}
