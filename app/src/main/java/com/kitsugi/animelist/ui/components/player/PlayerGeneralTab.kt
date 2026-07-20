package com.kitsugi.animelist.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.core.player.PlayerAspectMode
import com.kitsugi.animelist.core.player.QualityProfile
import com.kitsugi.animelist.core.player.QualityPreference
import com.kitsugi.animelist.ui.screens.fullscreen.components.QualityProfileDialog
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.core.player.ExternalPlayerPackages
import com.kitsugi.animelist.ui.components.KitsugiSettingsSection
import com.kitsugi.animelist.ui.components.KitsugiSettingsListItem
import com.kitsugi.animelist.ui.components.KitsugiSettingsSwitchItem
import com.kitsugi.animelist.ui.components.KitsugiSettingsDivider
import com.kitsugi.animelist.ui.components.KitsugiDropdownMenu
import com.kitsugi.animelist.ui.components.KitsugiDropdownItem

/**
 * Oynatıcı Ayarları — Genel sekmesi.
 * Oynatıcı tercihi, otomatik oynatma ve intro atlama süresini içerir.
 */
@Composable
internal fun PlayerGeneralTab(
    playerPreference: String,
    preferredExternalPlayerPackage: String,
    isAutoplayEnabled: Boolean,
    skipIntroDurationSec: Int,
    dv7HandlingMode: com.kitsugi.animelist.data.settings.Dv7HandlingMode,
    stripHdr10PlusSei: Boolean,
    qualityProfileJson: String,
    accentColor: Color,
    frameRateMatchingMode: com.kitsugi.animelist.data.settings.FrameRateMatchingMode,
    resolutionMatchingEnabled: Boolean,
    aspectMode: PlayerAspectMode = PlayerAspectMode.ORIGINAL,
    onAspectModeSelected: (PlayerAspectMode) -> Unit = {},
    gestureVolumeEnabled: Boolean = true,
    gestureBrightnessEnabled: Boolean = true,
    gestureZoomEnabled: Boolean = true,
    doubleTapSeekSeconds: Int = 10,
    holdSpeedMultiplier: Float = 2.0f,
    gestureScrollSensitivity: Float = 1.0f,
    previewSeekbarEnabled: Boolean = true,
    onPlayerPreferenceSelected: (String) -> Unit,
    onPreferredExternalPlayerPackageSelected: (String) -> Unit,
    onAutoplayEnabledChanged: (Boolean) -> Unit,
    onSkipIntroDurationSecSelected: (Int) -> Unit,
    onDv7HandlingModeSelected: (com.kitsugi.animelist.data.settings.Dv7HandlingMode) -> Unit,
    onStripHdr10PlusSeiChanged: (Boolean) -> Unit,
    onQualityProfileSelected: (String) -> Unit,
    onFrameRateMatchingModeSelected: (com.kitsugi.animelist.data.settings.FrameRateMatchingMode) -> Unit,
    onResolutionMatchingEnabledChanged: (Boolean) -> Unit,
    onGestureVolumeEnabledChanged: (Boolean) -> Unit = {},
    onGestureBrightnessEnabledChanged: (Boolean) -> Unit = {},
    onGestureZoomEnabledChanged: (Boolean) -> Unit = {},
    onDoubleTapSeekSecondsSelected: (Int) -> Unit = {},
    onHoldSpeedMultiplierSelected: (Float) -> Unit = {},
    onGestureScrollSensitivityChanged: (Float) -> Unit = {},
    onPreviewSeekbarEnabledChanged: (Boolean) -> Unit = {},
    liveHelperEnabled: Boolean = true,
    onLiveHelperEnabledChanged: (Boolean) -> Unit = {},
    enableAssExtractor: Boolean = true,
    onEnableAssExtractorChanged: (Boolean) -> Unit = {},
    showPlayerTitle: Boolean = true,
    onShowPlayerTitleChanged: (Boolean) -> Unit = {},
    showPlayerResolution: Boolean = true,
    onShowPlayerResolutionChanged: (Boolean) -> Unit = {},
    showMediaInfo: Boolean = true,
    onShowMediaInfoChanged: (Boolean) -> Unit = {},
    stillWatchingEnabled: Boolean = true,
    onStillWatchingEnabledChanged: (Boolean) -> Unit = {},
    stillWatchingThresholdMinutes: Int = 90,
    onStillWatchingThresholdMinutesChanged: (Int) -> Unit = {},
    postPlayMode: String = "AUTO_PLAY_NEXT",
    onPostPlayModeChanged: (String) -> Unit = {},
    autoplaySessionLimit: Int = 0,
    onAutoplaySessionLimitChanged: (Int) -> Unit = {},
    gainBoostDb: Float = 0f,
    onGainBoostDbChanged: (Float) -> Unit = {},
    subtitleDelayMs: Long = 0L,
    onSubtitleDelayMsChanged: (Long) -> Unit = {},
    decoderPriority: Int = 0,
    onDecoderPriorityChanged: (Int) -> Unit = {}
) {
    var showQualityProfileDialog by remember { mutableStateOf(false) }
    var playerDropdownExpanded by remember { mutableStateOf(false) }
    var introDropdownExpanded by remember { mutableStateOf(false) }
    var dvDropdownExpanded by remember { mutableStateOf(false) }
    var afrDropdownExpanded by remember { mutableStateOf(false) }
    var seekDropdownExpanded by remember { mutableStateOf(false) }
    var holdDropdownExpanded by remember { mutableStateOf(false) }
    var aspectDropdownExpanded by remember { mutableStateOf(false) }
    var thresholdDropdownExpanded by remember { mutableStateOf(false) }
    var postPlayDropdownExpanded by remember { mutableStateOf(false) }
    var sessionLimitDropdownExpanded by remember { mutableStateOf(false) }
    var decoderDropdownExpanded by remember { mutableStateOf(false) }

    val playerOptions = listOf(
        "INTERNAL" to "Dahili Oynatıcı (ExoPlayer)",
        "MPV" to "Dahili Oynatıcı (MPV)",
        "EXTERNAL" to "Harici Oynatıcı (MPV/VLC vb.)",
        "ASK" to "Her Seferinde Sor"
    )

    val introOptions = listOf(
        0 to "Devre Dışı",
        3 to "3 Saniye",
        5 to "5 Saniye",
        10 to "10 Saniye",
        15 to "15 Saniye"
    )

    val dvOptions = listOf(
        com.kitsugi.animelist.data.settings.Dv7HandlingMode.AUTO to "Otomatik (Cihaz Desteğine Göre)",
        com.kitsugi.animelist.data.settings.Dv7HandlingMode.OFF to "Kapalı (Doğal DV7 Oynat)",
        com.kitsugi.animelist.data.settings.Dv7HandlingMode.DV81_LIBDOVI to "DV8.1 Dönüştürme (libdovi)",
        com.kitsugi.animelist.data.settings.Dv7HandlingMode.HDR10_BASE_LAYER to "HDR10 Base Layer (RPU Yoksay)",
        com.kitsugi.animelist.data.settings.Dv7HandlingMode.STRIP_DV to "DV RPU Metadatasını Ayıkla"
    )

    val currentPlayerName = playerOptions.find { it.first == playerPreference }?.second
        ?: "Dahili Oynatıcı (ExoPlayer)"
    val currentIntroName = introOptions.find { it.first == skipIntroDurationSec }?.second
        ?: "5 Saniye"
    val currentDvName = dvOptions.find { it.first == dv7HandlingMode }?.second
        ?: "Otomatik (Cihaz Desteğine Göre)"

    LazyColumn(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Oynatıcı Tercihleri
        item {
            KitsugiSettingsSection(
                title = "Oynatıcı Tercihleri",
                subtitle = "Varsayılan video oynatıcı motorunu ve oynatma davranışlarını yapılandırın."
            ) {
                // Tercih Edilen Oynatıcı
                Box {
                    KitsugiSettingsListItem(
                        title = "Tercih Edilen Oynatıcı",
                        description = "Uygulama içi veya harici video oynatıcı seçimi",
                        value = currentPlayerName,
                        icon = Icons.Rounded.PlayArrow,
                        iconColor = accentColor,
                        onClick = { playerDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = playerDropdownExpanded, onDismissRequest = { playerDropdownExpanded = false }) {
                        playerOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == playerPreference,
                                onClick = {
                                    onPlayerPreferenceSelected(option.first)
                                    playerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Tercih Edilen Harici Oynatıcı (if EXTERNAL)
                if (playerPreference == "EXTERNAL") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    var externalPlayerDropdownExpanded by remember { mutableStateOf(false) }
                    val currentExternalPlayerName = ExternalPlayerPackages.players.find { it.packageName == preferredExternalPlayerPackage }?.name ?: "Sistem Varsayılanı"

                    KitsugiSettingsDivider()

                    Box {
                        KitsugiSettingsListItem(
                            title = "Tercih Edilen Harici Oynatıcı",
                            description = "Sisteminizde yüklü olan harici oynatıcı paketini seçin",
                            value = currentExternalPlayerName,
                            icon = Icons.Rounded.Launch,
                            iconColor = accentColor,
                            onClick = { externalPlayerDropdownExpanded = true }
                        )
                        KitsugiDropdownMenu(expanded = externalPlayerDropdownExpanded, onDismissRequest = { externalPlayerDropdownExpanded = false }) {
                            ExternalPlayerPackages.players.forEach { playerDef ->
                                val isInstalled = playerDef.packageName.isEmpty() || playerDef.isInstalled(context)
                                KitsugiDropdownItem(
                                    text = if (isInstalled) playerDef.name else "${playerDef.name} (Yüklü Değil)",
                                    selected = playerDef.packageName == preferredExternalPlayerPackage,
                                    onClick = {
                                        if (isInstalled) {
                                            onPreferredExternalPlayerPackageSelected(playerDef.packageName)
                                        } else {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${playerDef.packageName}"))
                                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(playerDef.storeUrl))
                                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            }
                                        }
                                        externalPlayerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Otomatik Sonraki Bölüm (Switch)
                KitsugiSettingsSwitchItem(
                    title = "Otomatik Sonraki Bölüm",
                    description = "Sonraki bölümü otomatik olarak başlatır.",
                    icon = Icons.Rounded.SkipNext,
                    iconColor = accentColor,
                    checked = isAutoplayEnabled,
                    onCheckedChange = onAutoplayEnabledChanged
                )

                KitsugiSettingsDivider()

                // İntro Atlama Buton Süresi
                Box {
                    KitsugiSettingsListItem(
                        title = "İntro Atlama Buton Süresi",
                        description = "Hızlı geçiş için ekrana gelen introyu atla butonunun süresi",
                        value = currentIntroName,
                        icon = Icons.Rounded.Forward10,
                        iconColor = accentColor,
                        onClick = { introDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = introDropdownExpanded, onDismissRequest = { introDropdownExpanded = false }) {
                        introOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == skipIntroDurationSec,
                                onClick = {
                                    onSkipIntroDurationSecSelected(option.first)
                                    introDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Kalite Profili
                val profile = remember(qualityProfileJson) { QualityProfile.deserialize(qualityProfileJson) }
                val preferenceName = when (profile.preference) {
                    QualityPreference.AUTO       -> "Otomatik"
                    QualityPreference.P1080      -> "1080p Tercih Et"
                    QualityPreference.P720       -> "720p Tercih Et"
                    QualityPreference.P480       -> "480p Tercih Et"
                    QualityPreference.DATA_SAVER -> "Veri Tasarrufu"
                }
                KitsugiSettingsListItem(
                    title = "Kalite Profili",
                    description = "Tercih edilen video kalitesi ve çözünürlük limitleri",
                    value = preferenceName,
                    icon = Icons.Rounded.Hd,
                    iconColor = accentColor,
                    onClick = { showQualityProfileDialog = true }
                )
            }
        }

        // Video İşleme & Uyum
        item {
            KitsugiSettingsSection(
                title = "Video İşleme & Uyum",
                subtitle = "HDR, Dolby Vision ve ekran uyumluluk modlarını yapılandırın."
            ) {
                // Dolby Vision (DV7) İşleme Modu
                Box {
                    KitsugiSettingsListItem(
                        title = "Dolby Vision (DV7) İşleme Modu",
                        description = "Profil 7 Dolby Vision videolar için renk dönüştürme ve işleme ayarı",
                        value = currentDvName,
                        icon = Icons.Rounded.Settings,
                        iconColor = accentColor,
                        onClick = { dvDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = dvDropdownExpanded, onDismissRequest = { dvDropdownExpanded = false }) {
                        dvOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == dv7HandlingMode,
                                onClick = {
                                    onDv7HandlingModeSelected(option.first)
                                    dvDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // HDR10+ SEI Metadatasını Ayıkla
                KitsugiSettingsSwitchItem(
                    title = "HDR10+ SEI Metadatasını Ayıkla",
                    description = "HDR10+ akışlarındaki SEI dinamik meta verilerini temizler.",
                    icon = Icons.Rounded.FilterCenterFocus,
                    iconColor = accentColor,
                    checked = stripHdr10PlusSei,
                    onCheckedChange = onStripHdr10PlusSeiChanged
                )

                KitsugiSettingsDivider()

                // Görüntü Oranı
                val aspectOptions = listOf(
                    PlayerAspectMode.ORIGINAL  to "📺 Orijinal (Varsayılan)",
                    PlayerAspectMode.FIT       to "⇔ Sığdır (Boşluk bırak)",
                    PlayerAspectMode.FILL      to "⛶ Doldur (Esnet)",
                    PlayerAspectMode.ZOOM      to "🔍 Yakınlaştır (Kırp)",
                    PlayerAspectMode.CROP_16_9 to "▭ 16:9 Kırp",
                    PlayerAspectMode.CROP_4_3  to "□ 4:3 Kırp"
                )
                val currentAspectName = aspectOptions.find { it.first == aspectMode }?.second
                    ?: "📺 Orijinal (Varsayılan)"
                Box {
                    KitsugiSettingsListItem(
                        title = "Görüntü Oranı",
                        description = "Video görüntü boyutunu ve ekranı kaplama şeklini belirler",
                        value = currentAspectName,
                        icon = Icons.Rounded.AspectRatio,
                        iconColor = accentColor,
                        onClick = { aspectDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = aspectDropdownExpanded, onDismissRequest = { aspectDropdownExpanded = false }) {
                        aspectOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == aspectMode,
                                onClick = {
                                    onAspectModeSelected(option.first)
                                    aspectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Kare Hızı Eşleme (AFR)
                val afrOptions = listOf(
                    com.kitsugi.animelist.data.settings.FrameRateMatchingMode.OFF to "Kapalı",
                    com.kitsugi.animelist.data.settings.FrameRateMatchingMode.START to "Yalnızca Başlangıçta",
                    com.kitsugi.animelist.data.settings.FrameRateMatchingMode.START_STOP to "Başlangıç ve Bitişte"
                )
                val currentAfrName = afrOptions.find { it.first == frameRateMatchingMode }?.second ?: "Kapalı"
                Box {
                    KitsugiSettingsListItem(
                        title = "Kare Hızı Eşleme (AFR)",
                        description = "TV ekran yenileme hızını video FPS'ine göre senkronize eder",
                        value = currentAfrName,
                        icon = Icons.Rounded.Sync,
                        iconColor = accentColor,
                        onClick = { afrDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = afrDropdownExpanded, onDismissRequest = { afrDropdownExpanded = false }) {
                        afrOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == frameRateMatchingMode,
                                onClick = {
                                    onFrameRateMatchingModeSelected(option.first)
                                    afrDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Çözünürlük Eşleme
                KitsugiSettingsSwitchItem(
                    title = "Çözünürlük Eşleme",
                    description = "Ekran çözünürlüğünü video çözünürlüğüne göre eşler.",
                    icon = Icons.Rounded.SettingsOverscan,
                    iconColor = accentColor,
                    checked = resolutionMatchingEnabled,
                    onCheckedChange = onResolutionMatchingEnabledChanged
                )
            }
        }

        // Arayüz & Yardımcılar
        item {
            KitsugiSettingsSection(
                title = "Arayüz & Yardımcılar",
                subtitle = "Oynatıcı içi yardımcı asistan ve arayüz elemanlarını özelleştirin."
            ) {
                // Canlı Yardımcı
                KitsugiSettingsSwitchItem(
                    title = "Canlı Yardımcı",
                    description = "Oynatıcı için canlı asistan ve yardımcı özellikleri etkinleştirir.",
                    icon = Icons.Rounded.Help,
                    iconColor = accentColor,
                    checked = liveHelperEnabled,
                    onCheckedChange = onLiveHelperEnabledChanged
                )

                KitsugiSettingsDivider()

                // ASS Altyazı Ayıklayıcı
                KitsugiSettingsSwitchItem(
                    title = "ASS Altyazı Ayıklayıcı",
                    description = "ASS biçimindeki gelişmiş altyazıları ayıklar ve daha iyi biçimlendirir.",
                    icon = Icons.Rounded.Subtitles,
                    iconColor = accentColor,
                    checked = enableAssExtractor,
                    onCheckedChange = onEnableAssExtractorChanged
                )

                KitsugiSettingsDivider()

                // Oynatıcı Başlığını Göster
                KitsugiSettingsSwitchItem(
                    title = "Oynatıcı Başlığını Göster",
                    description = "Video oynatılırken sol üstte anime/bölüm başlığını gösterir.",
                    icon = Icons.Rounded.Title,
                    iconColor = accentColor,
                    checked = showPlayerTitle,
                    onCheckedChange = onShowPlayerTitleChanged
                )

                KitsugiSettingsDivider()

                // Oynatıcı Çözünürlüğünü Göster
                KitsugiSettingsSwitchItem(
                    title = "Oynatıcı Çözünürlüğünü Göster",
                    description = "Video oynatılırken sol üstte çözünürlük bilgisini gösterir.",
                    icon = Icons.Rounded.SettingsOverscan,
                    iconColor = accentColor,
                    checked = showPlayerResolution,
                    onCheckedChange = onShowPlayerResolutionChanged
                )

                KitsugiSettingsDivider()

                // Medya Bilgisini Göster
                KitsugiSettingsSwitchItem(
                    title = "Medya Bilgisini Göster",
                    description = "Video oynatılırken fps, codec vb. detaylı medya bilgilerini gösterir.",
                    icon = Icons.Rounded.Info,
                    iconColor = accentColor,
                    checked = showMediaInfo,
                    onCheckedChange = onShowMediaInfoChanged
                )

                KitsugiSettingsDivider()

                // Önizleme Seekbarı
                KitsugiSettingsSwitchItem(
                    title = "Önizleme Seekbarı",
                    description = "Sarma (scrubbing) sırasında küçük video önizleme görseli gösterir.",
                    icon = Icons.Rounded.Preview,
                    iconColor = accentColor,
                    checked = previewSeekbarEnabled,
                    onCheckedChange = onPreviewSeekbarEnabledChanged
                )
            }
        }

        // Otomatik Oynatma & Seans
        item {
            KitsugiSettingsSection(
                title = "Otomatik Oynatma & Seans Ayarları",
                subtitle = "Çoklu izleme oturumları için limitler ve uyarılar ayarlayın."
            ) {
                // Hâlâ İzliyor Musun?
                KitsugiSettingsSwitchItem(
                    title = "Hâlâ İzliyor Musun?",
                    description = "Uzun süre hareketsiz kalınırsa ekrana ‘Hâlâ izliyor musun?’ sorusu gelir.",
                    icon = Icons.Rounded.Tv,
                    iconColor = accentColor,
                    checked = stillWatchingEnabled,
                    onCheckedChange = onStillWatchingEnabledChanged
                )

                // Hareketsizlik Eşiği
                if (stillWatchingEnabled) {
                    KitsugiSettingsDivider()
                    val thresholdOptions = listOf(30 to "30 dakika", 60 to "60 dakika", 90 to "90 dakika (Varsayılan)", 120 to "120 dakika")
                    val currentThresholdName = thresholdOptions.find { it.first == stillWatchingThresholdMinutes }?.second ?: "90 dakika (Varsayılan)"
                    Box {
                        KitsugiSettingsListItem(
                            title = "Hareketsizlik Eşiği",
                            description = "Ne kadar süre sonra uyarı gösterileceğini belirler",
                            value = currentThresholdName,
                            icon = Icons.Rounded.HourglassEmpty,
                            iconColor = accentColor,
                            onClick = { thresholdDropdownExpanded = true }
                        )
                        KitsugiDropdownMenu(expanded = thresholdDropdownExpanded, onDismissRequest = { thresholdDropdownExpanded = false }) {
                            thresholdOptions.forEach { opt ->
                                KitsugiDropdownItem(
                                    text = opt.second,
                                    selected = opt.first == stillWatchingThresholdMinutes,
                                    onClick = {
                                        onStillWatchingThresholdMinutesChanged(opt.first)
                                        thresholdDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Sonraki Bölüm Modu
                val postPlayOptions = listOf(
                    "AUTO_PLAY_NEXT" to "▶ Otomatik Oynat",
                    "BINGE_PROMPT" to "📺 Devam İster Misin? Sor",
                    "MANUAL" to "✋ Manuel (Otomatik Oynatma Yok)"
                )
                val currentPostPlayName = postPlayOptions.find { it.first == postPlayMode }?.second ?: "▶ Otomatik Oynat"
                Box {
                    KitsugiSettingsListItem(
                        title = "Sonraki Bölüm Modu",
                        description = "Bölüm bittiğinde yapılacak varsayılan eylem",
                        value = currentPostPlayName,
                        icon = Icons.Rounded.Forward,
                        iconColor = accentColor,
                        onClick = { postPlayDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = postPlayDropdownExpanded, onDismissRequest = { postPlayDropdownExpanded = false }) {
                        postPlayOptions.forEach { opt ->
                            KitsugiDropdownItem(
                                text = opt.second,
                                selected = opt.first == postPlayMode,
                                onClick = {
                                    onPostPlayModeChanged(opt.first)
                                    postPlayDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Oturum Limiti
                val limitOptions = listOf(0 to "Sınırsız", 3 to "3 Bölüm", 5 to "5 Bölüm", 10 to "10 Bölüm", 20 to "20 Bölüm")
                val currentLimitName = limitOptions.find { it.first == autoplaySessionLimit }?.second ?: "Sınırsız"
                Box {
                    KitsugiSettingsListItem(
                        title = "Seans Başına Otomatik Oynatma Limiti",
                        description = "Oturum başına otomatik oynatılacak maksimum bölüm sayısı",
                        value = currentLimitName,
                        icon = Icons.Rounded.SlowMotionVideo,
                        iconColor = accentColor,
                        onClick = { sessionLimitDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = sessionLimitDropdownExpanded, onDismissRequest = { sessionLimitDropdownExpanded = false }) {
                        limitOptions.forEach { opt ->
                            KitsugiDropdownItem(
                                text = opt.second,
                                selected = opt.first == autoplaySessionLimit,
                                onClick = {
                                    onAutoplaySessionLimitChanged(opt.first)
                                    sessionLimitDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Ses & Altyazı Gelişmiş
        item {
            KitsugiSettingsSection(
                title = "Ses & Altyazı Gelişmiş",
                subtitle = "Kod çözücü önceliğini, gelişmiş ses amplifikasyonunu ve altyazı gecikmelerini ayarlayın."
            ) {
                // Dekoder Önceliği
                val decoderOptions = listOf(
                    0 to "📺 Donanım Öncelikli (HW only)",
                    1 to "💻 Yazılım Fallback (HW → SW)",
                    2 to "📁 Yazılım Öncelikli (SW preferred)"
                )
                val currentDecoderName = decoderOptions.find { it.first == decoderPriority }?.second
                    ?: "📺 Donanım Öncelikli (HW only)"
                Box {
                    KitsugiSettingsListItem(
                        title = "Dekoder Önceliği",
                        description = "Donanımsal veya yazılımsal kod çözücü öncelik seçimi",
                        value = currentDecoderName,
                        icon = Icons.Rounded.Memory,
                        iconColor = accentColor,
                        onClick = { decoderDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = decoderDropdownExpanded, onDismissRequest = { decoderDropdownExpanded = false }) {
                        decoderOptions.forEach { option ->
                            KitsugiDropdownItem(
                                text = option.second,
                                selected = option.first == decoderPriority,
                                onClick = {
                                    onDecoderPriorityChanged(option.first)
                                    decoderDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Slider values styled cleanly
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Ses Güçlendirme (Gain Boost): ${String.format("%.1f", gainBoostDb)} dB",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "0 dB = normal; pozitif = amplifikasyon; negatif = azaltma",
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = gainBoostDb,
                        onValueChange = onGainBoostDbChanged,
                        valueRange = -10f..20f,
                        steps = 59,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                KitsugiSettingsDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val subtitleDelaySeconds = subtitleDelayMs / 1000f
                    val clampedDelaySeconds = subtitleDelaySeconds.coerceIn(-10f, 10f)
                    Text(
                        "Altyazı Gecikmesi: ${String.format("%.1f", subtitleDelaySeconds)} s",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Negatif = altyazı erken; pozitif = altyazı geç",
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = clampedDelaySeconds,
                        onValueChange = { onSubtitleDelayMsChanged((it * 1000).toLong()) },
                        valueRange = -10f..10f,
                        steps = 199,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Jestler
        item {
            KitsugiSettingsSection(
                title = "Dokunmatik Jest Ayarları",
                subtitle = "Oynatma esnasında ekran kaydırma ve çift tıklama eylemlerini yapılandırın."
            ) {
                // Ses Jesti
                KitsugiSettingsSwitchItem(
                    title = "Ses Jesti",
                    description = "Sağ dikey kaydırma ses seviyesini değiştirir.",
                    icon = Icons.Rounded.VolumeUp,
                    iconColor = accentColor,
                    checked = gestureVolumeEnabled,
                    onCheckedChange = onGestureVolumeEnabledChanged
                )

                KitsugiSettingsDivider()

                // Parlaklık Jesti
                KitsugiSettingsSwitchItem(
                    title = "Parlaklık Jesti",
                    description = "Sol dikey kaydırma ekran parlaklığını değiştirir.",
                    icon = Icons.Rounded.BrightnessMedium,
                    iconColor = accentColor,
                    checked = gestureBrightnessEnabled,
                    onCheckedChange = onGestureBrightnessEnabledChanged
                )

                KitsugiSettingsDivider()

                // Zoom Jesti
                KitsugiSettingsSwitchItem(
                    title = "Zoom Jesti",
                    description = "İki parmak baskısı ile ekran zoom'u.",
                    icon = Icons.Rounded.ZoomIn,
                    iconColor = accentColor,
                    checked = gestureZoomEnabled,
                    onCheckedChange = onGestureZoomEnabledChanged
                )

                KitsugiSettingsDivider()

                // Çift Dokunuş İleri/Geri Süresi
                val seekOptions = listOf(5 to "5s", 10 to "10s", 15 to "15s", 20 to "20s", 30 to "30s")
                val currentSeekName = seekOptions.find { it.first == doubleTapSeekSeconds }?.second ?: "10s"
                Box {
                    KitsugiSettingsListItem(
                        title = "Çift Dokunuş İleri/Geri Süresi",
                        description = "Ekranın soluna/sağına çift dokunulduğunda atlanacak süre",
                        value = currentSeekName,
                        icon = Icons.Rounded.Forward10,
                        iconColor = accentColor,
                        onClick = { seekDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = seekDropdownExpanded, onDismissRequest = { seekDropdownExpanded = false }) {
                        seekOptions.forEach { opt ->
                            KitsugiDropdownItem(
                                text = opt.second,
                                selected = opt.first == doubleTapSeekSeconds,
                                onClick = {
                                    onDoubleTapSeekSecondsSelected(opt.first)
                                    seekDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Basılı Tutma Hız Çarpanı
                val holdOptions = listOf(1.5f to "1.5x", 2.0f to "2x", 2.5f to "2.5x", 3.0f to "3x")
                val currentHoldName = holdOptions.find { it.first == holdSpeedMultiplier }?.second ?: "2x"
                Box {
                    KitsugiSettingsListItem(
                        title = "Basılı Tutma Hız Çarpanı",
                        description = "Ekrana basılı tutulduğundaki oynatma hızı",
                        value = currentHoldName,
                        icon = Icons.Rounded.Speed,
                        iconColor = accentColor,
                        onClick = { holdDropdownExpanded = true }
                    )
                    KitsugiDropdownMenu(expanded = holdDropdownExpanded, onDismissRequest = { holdDropdownExpanded = false }) {
                        holdOptions.forEach { opt ->
                            KitsugiDropdownItem(
                                text = opt.second,
                                selected = opt.first == holdSpeedMultiplier,
                                onClick = {
                                    onHoldSpeedMultiplierSelected(opt.first)
                                    holdDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                KitsugiSettingsDivider()

                // Kaydırma Hassasiyeti
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val scrollLabel = when {
                        gestureScrollSensitivity <= 0.6f -> "Yavaş (%.1fx)".format(gestureScrollSensitivity)
                        gestureScrollSensitivity >= 1.6f -> "Hızlı (%.1fx)".format(gestureScrollSensitivity)
                        else -> "Normal (%.1fx)".format(gestureScrollSensitivity)
                    }
                    Text(
                        "Kaydırma Hassasiyeti: $scrollLabel",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = gestureScrollSensitivity,
                        onValueChange = onGestureScrollSensitivityChanged,
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showQualityProfileDialog) {
        val currentProfile = remember(qualityProfileJson) { QualityProfile.deserialize(qualityProfileJson) }
        QualityProfileDialog(
            currentProfile = currentProfile,
            onDismiss = { showQualityProfileDialog = false },
            onProfileSelected = { newProfile ->
                onQualityProfileSelected(QualityProfile.serialize(newProfile))
                showQualityProfileDialog = false
            }
        )
    }
}
