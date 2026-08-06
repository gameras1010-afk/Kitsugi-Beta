package com.kitsugi.animelist.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.local.CustomButton
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.launch
import java.io.File

enum class PlayerSettingsSubScreen {
    Main,
    DahiliOynatici,
    Hareketler,
    KodCozucu,
    Altyazilar,
    Ses,
    OzelButonlar,
    KodDuzenleyici,
    Gelismis
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KitsugiPlayerSettingsDialog(
    playerPreference: String,
    preferredExternalPlayerPackage: String,
    isAutoplayEnabled: Boolean,
    skipIntroDurationSec: Int,
    defaultSubtitleSize: Int,
    defaultSubtitleColor: Int,
    subtitleBold: Boolean,
    subtitleOutlineEnabled: Boolean,
    defaultAudioBoost: Float,
    defaultAudioDelayMs: Long,
    minBufferMs: Int,
    maxBufferMs: Int,
    bufferForPlaybackMs: Int,
    bufferForPlaybackAfterRebufferMs: Int,
    backBufferDurationMs: Int,
    dv7HandlingMode: com.kitsugi.animelist.data.settings.Dv7HandlingMode,
    stripHdr10PlusSei: Boolean,
    preferredSubtitleLanguages: String,
    addonSubtitleStartupMode: String,
    qualityProfileJson: String,
    // T1.3 – Rota bazlı gecikme
    speakerDelayMs: Long = 0L,
    bluetoothDelayMs: Long = 0L,
    wiredDelayMs: Long = 0L,
    hdmiDelayMs: Long = 0L,
    activeAudioRoute: com.kitsugi.animelist.core.player.AudioRoute = com.kitsugi.animelist.core.player.AudioRoute.SPEAKER,
    onRouteDelayChanged: (speaker: Long, bluetooth: Long, wired: Long, hdmi: Long) -> Unit = { _, _, _, _ -> },
    onPlayerPreferenceSelected: (String) -> Unit,
    onPreferredExternalPlayerPackageSelected: (String) -> Unit,
    onAutoplayEnabledChanged: (Boolean) -> Unit,
    onSkipIntroDurationSecSelected: (Int) -> Unit,
    onDefaultSubtitleSizeSelected: (Int) -> Unit,
    onDefaultSubtitleColorSelected: (Int) -> Unit,
    onSubtitleBoldChanged: (Boolean) -> Unit,
    onSubtitleOutlineEnabledChanged: (Boolean) -> Unit,
    onDefaultAudioBoostSelected: (Float) -> Unit,
    onDefaultAudioDelayMsSelected: (Long) -> Unit,
    onPreferredSubtitleLanguagesSelected: (String) -> Unit,
    onAddonSubtitleStartupModeSelected: (String) -> Unit,
    onBufferSettingsChanged: (min: Int, max: Int, playback: Int, rebuffer: Int, back: Int) -> Unit,
    onDv7HandlingModeSelected: (com.kitsugi.animelist.data.settings.Dv7HandlingMode) -> Unit,
    onStripHdr10PlusSeiChanged: (Boolean) -> Unit,
    onQualityProfileSelected: (String) -> Unit,
    // T1.9
    parallelRangeEnabled: Boolean = false,
    onParallelRangeEnabledChanged: (Boolean) -> Unit = {},
    // T1.4
    frameRateMatchingMode: com.kitsugi.animelist.data.settings.FrameRateMatchingMode = com.kitsugi.animelist.data.settings.FrameRateMatchingMode.OFF,
    resolutionMatchingEnabled: Boolean = false,
    onFrameRateMatchingModeSelected: (com.kitsugi.animelist.data.settings.FrameRateMatchingMode) -> Unit = {},
    onResolutionMatchingEnabledChanged: (Boolean) -> Unit = {},
    // T2.1 + T2.7 – Gesture Ayarları
    gestureVolumeEnabled: Boolean = true,
    gestureBrightnessEnabled: Boolean = true,
    gestureZoomEnabled: Boolean = true,
    doubleTapSeekSeconds: Int = 10,
    holdSpeedMultiplier: Float = 2.0f,
    gestureScrollSensitivity: Float = 1.0f,
    onGestureVolumeEnabledChanged: (Boolean) -> Unit = {},
    onGestureBrightnessEnabledChanged: (Boolean) -> Unit = {},
    onGestureZoomEnabledChanged: (Boolean) -> Unit = {},
    onDoubleTapSeekSecondsSelected: (Int) -> Unit = {},
    onHoldSpeedMultiplierSelected: (Float) -> Unit = {},
    onGestureScrollSensitivityChanged: (Float) -> Unit = {},
    // T2.2 – Önizleme Seekbar
    previewSeekbarEnabled: Boolean = true,
    onPreviewSeekbarEnabledChanged: (Boolean) -> Unit = {},
    // T1.1 – Görüntü Oranı
    aspectMode: com.kitsugi.animelist.core.player.PlayerAspectMode = com.kitsugi.animelist.core.player.PlayerAspectMode.ORIGINAL,
    onAspectModeSelected: (com.kitsugi.animelist.core.player.PlayerAspectMode) -> Unit = {},
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
    // ─── T1-01 – StillWatching + PostPlayMode + AutoplaySessionLimit ──────────
    stillWatchingEnabled: Boolean = true,
    onStillWatchingEnabledChanged: (Boolean) -> Unit = {},
    stillWatchingThresholdMinutes: Int = 90,
    onStillWatchingThresholdMinutesChanged: (Int) -> Unit = {},
    postPlayMode: String = "AUTO_PLAY_NEXT",
    onPostPlayModeChanged: (String) -> Unit = {},
    autoplaySessionLimit: Int = 0,
    onAutoplaySessionLimitChanged: (Int) -> Unit = {},
    // ─── T1-03 – Ses Gelişmiş ────────────────────────────────────────────────
    gainBoostDb: Float = 0f,
    onGainBoostDbChanged: (Float) -> Unit = {},
    subtitleDelayMs: Long = 0L,
    onSubtitleDelayMsChanged: (Long) -> Unit = {},
    // ─── T1-04 – Dekoder Önceliği (Telefon) ──────────────────────────────────
    decoderPriority: Int = 0,
    onDecoderPriorityChanged: (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // SharedPreferences for direct setting fields
    val settingsDataStore = remember { SettingsDataStore(context) }
    val appSettingsFlow = remember { settingsDataStore.settingsFlow }
    val appSettings by appSettingsFlow.collectAsState(initial = null)

    // Database flow for Custom Buttons
    val db = remember { KitsugiDatabase.getDatabase(context) }
    val customButtonsFlow = remember { db.customButtonDao().subscribeAll() }
    val customButtons by customButtonsFlow.collectAsState(initial = emptyList())

    // Sub-screen Navigation State
    var activeSubScreen by rememberSaveable { mutableStateOf(PlayerSettingsSubScreen.Main) }

    // Lazy List States for scrolling
    val mainMenuScrollState = rememberLazyListState()
    val subScreenScrollState = rememberLazyListState()
    val activeScrollState = if (activeSubScreen == PlayerSettingsSubScreen.Main) mainMenuScrollState else subScreenScrollState

    // File Editor dialog states
    var editingFile by remember { mutableStateOf<File?>(null) }
    var fileContentText by remember { mutableStateOf("") }
    
    // Custom Button dialog states
    var showButtonEditDialog by remember { mutableStateOf<CustomButton?>(null) }
    var showButtonAddDialog by remember { mutableStateOf(false) }

    // Common Dropdown state placeholders
    var playerDropdownExpanded by remember { mutableStateOf(false) }
    var extPackageDropdownExpanded by remember { mutableStateOf(false) }
    var introDropdownExpanded by remember { mutableStateOf(false) }
    var stillWatchingDropdownExpanded by remember { mutableStateOf(false) }
    var postPlayDropdownExpanded by remember { mutableStateOf(false) }
    var autoplayLimitDropdownExpanded by remember { mutableStateOf(false) }
    
    var doubleTapSeekDropdownExpanded by remember { mutableStateOf(false) }
    var holdSpeedDropdownExpanded by remember { mutableStateOf(false) }
    
    var dvDropdownExpanded by remember { mutableStateOf(false) }
    var decoderDropdownExpanded by remember { mutableStateOf(false) }
    var mpvHwdecDropdownExpanded by remember { mutableStateOf(false) }
    var mpvGpuDropdownExpanded by remember { mutableStateOf(false) }
    var mpvDebandDropdownExpanded by remember { mutableStateOf(false) }
    
    var subSizeDropdownExpanded by remember { mutableStateOf(false) }
    var subColorDropdownExpanded by remember { mutableStateOf(false) }
    var subJustificationDropdownExpanded by remember { mutableStateOf(false) }
    var subBgColorDropdownExpanded by remember { mutableStateOf(false) }
    var subBorderColorDropdownExpanded by remember { mutableStateOf(false) }
    var subStartupDropdownExpanded by remember { mutableStateOf(false) }
    
    var volBoostCapDropdownExpanded by remember { mutableStateOf(false) }
    var audioBoostDropdownExpanded by remember { mutableStateOf(false) }
    var audioDelayDropdownExpanded by remember { mutableStateOf(false) }
    
    var bufferMinExp by remember { mutableStateOf(false) }
    var bufferMaxExp by remember { mutableStateOf(false) }
    var bufferPlayExp by remember { mutableStateOf(false) }
    var bufferRebExp by remember { mutableStateOf(false) }
    var bufferBackExp by remember { mutableStateOf(false) }

    KitsugiSheetOrDialog(
        onDismiss = onDismiss,
        fullScreen = true,
        innerScrollState = activeScrollState
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KitsugiColors.Surface)
        ) {
            // Header Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeSubScreen != PlayerSettingsSubScreen.Main) {
                        IconButton(onClick = { activeSubScreen = PlayerSettingsSubScreen.Main }) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Geri",
                                tint = KitsugiColors.TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = when (activeSubScreen) {
                            PlayerSettingsSubScreen.Main -> "Oynatıcı Ayarları"
                            PlayerSettingsSubScreen.DahiliOynatici -> "Dahili oynatıcı"
                            PlayerSettingsSubScreen.Hareketler -> "Hareketler"
                            PlayerSettingsSubScreen.KodCozucu -> "Kod çözücü"
                            PlayerSettingsSubScreen.Altyazilar -> "Alt yazılar"
                            PlayerSettingsSubScreen.Ses -> "Ses"
                            PlayerSettingsSubScreen.OzelButonlar -> "Özel butonlar"
                            PlayerSettingsSubScreen.KodDuzenleyici -> "Kod düzenleyici"
                            PlayerSettingsSubScreen.Gelismis -> "Gelişmiş"
                        },
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Kapat",
                        tint = KitsugiColors.TextSecondary
                    )
                }
            }

            KitsugiSettingsDivider()

            // Sub-screen Selector / Body Content
            Box(modifier = Modifier.weight(1f)) {
                when (activeSubScreen) {
                    PlayerSettingsSubScreen.Main -> {
                        LazyColumn(
                            state = mainMenuScrollState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            item {
                                KitsugiSettingsItem(
                                    title = "Dahili oynatıcı",
                                    description = "Varsayılan oynatıcı, otomatik oynatma, intro atlama ve başlık görünümü",
                                    icon = Icons.Rounded.PlayCircle,
                                    iconColor = accentColor,
                                    onClick = { activeSubScreen = PlayerSettingsSubScreen.DahiliOynatici }
                                )
                                KitsugiSettingsDivider()
                            }
                            item {
                                KitsugiSettingsItem(
                                    title = "Hareketler",
                                    description = "Dikey ve yatay kaydırma, çift dokunma süresi, basılı tutma hızı ve arama jestleri",
                                    icon = Icons.Rounded.Swipe,
                                    iconColor = accentColor,
                                    onClick = { activeSubScreen = PlayerSettingsSubScreen.Hareketler }
                                )
                                KitsugiSettingsDivider()
                            }
                            item {
                                KitsugiSettingsItem(
                                    title = "Kod çözücü",
                                    description = "MPV GPU renderer, donanım kod çözücü (hwdec), debanding ve Dolby Vision",
                                    icon = Icons.Rounded.Memory,
                                    iconColor = accentColor,
                                    onClick = { activeSubScreen = PlayerSettingsSubScreen.KodCozucu }
                                )
                                KitsugiSettingsDivider()
                            }
                            item {
                                KitsugiSettingsItem(
                                    title = "Alt yazılar",
                                    description = "Altyazı boyutu, rengi, yazı kalınlığı, gölge, arka plan ve dil tercihleri",
                                    icon = Icons.Rounded.Subtitles,
                                    iconColor = accentColor,
                                    onClick = { activeSubScreen = PlayerSettingsSubScreen.Altyazilar }
                                )
                                KitsugiSettingsDivider()
                            }
                            item {
                                KitsugiSettingsItem(
                                    title = "Ses",
                                    description = "Ses güçlendirme sınırı, rota bazlı ses gecikmeleri ve gecikme ayarları",
                                    icon = Icons.Rounded.VolumeUp,
                                    iconColor = accentColor,
                                    onClick = { activeSubScreen = PlayerSettingsSubScreen.Ses }
                                )
                                KitsugiSettingsDivider()
                            }
                            item {
                                KitsugiSettingsItem(
                                    title = "Özel butonlar",
                                    description = "Oynatıcı içi kontrol paneline yeni özel lua/mpv tetikleyici butonları ekleyin ve düzenleyin",
                                    icon = Icons.Rounded.SmartButton,
                                    iconColor = accentColor,
                                    onClick = { activeSubScreen = PlayerSettingsSubScreen.OzelButonlar }
                                )
                                KitsugiSettingsDivider()
                            }
                            item {
                                KitsugiSettingsItem(
                                    title = "Kod düzenleyici",
                                    description = "Uygulama dizinindeki özel Lua script dosyalarını ve Conf seçeneklerini düzenleyin",
                                    icon = Icons.Rounded.Code,
                                    iconColor = accentColor,
                                    onClick = { activeSubScreen = PlayerSettingsSubScreen.KodDuzenleyici }
                                )
                                KitsugiSettingsDivider()
                            }
                            item {
                                KitsugiSettingsItem(
                                    title = "Gelişmiş",
                                    description = "ExoPlayer arabellek sınırları, paralel indirme, mpv.conf ve input.conf dosyaları",
                                    icon = Icons.Rounded.Settings,
                                    iconColor = accentColor,
                                    onClick = { activeSubScreen = PlayerSettingsSubScreen.Gelismis }
                                )
                            }
                        }
                    }

                    PlayerSettingsSubScreen.DahiliOynatici -> {
                        LazyColumn(
                            state = subScreenScrollState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                KitsugiSettingsSection(title = "Oynatma Motoru") {
                                    // Player Preference Selection
                                    Box {
                                        val playerOptions = listOf(
                                            "MPV"      to "Dahili Oynatıcı (MPV)",
                                            "INTERNAL" to "Dahili Oynatıcı (ExoPlayer)",
                                            "EXTERNAL" to "Harici Oynatıcı (MPV/VLC vb.)",
                                            "ASK"      to "Her Seferinde Sor"
                                        )
                                        val currentPlayerName = playerOptions.find { it.first == playerPreference }?.second ?: "Dahili Oynatıcı (MPV)"
                                        KitsugiSettingsListItem(
                                            title = "Varsayılan Video Oynatıcı",
                                            description = "Hangi oynatma motorunun kullanılacağını seçin",
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
                                    
                                    if (playerPreference == "EXTERNAL") {
                                        KitsugiSettingsDivider()
                                        Box {
                                            val extOptions = listOf(
                                                "" to "Seçilmedi (Varsayılan Sistem Oynatıcısı)",
                                                "com.mxtech.videoplayer.ad" to "MX Player",
                                                "com.mxtech.videoplayer.pro" to "MX Player Pro",
                                                "org.videolan.vlc" to "VLC Player",
                                                "is.xyz.mpv" to "MPV Player",
                                                "com.brouken.player" to "Just Player"
                                            )
                                            val currentExtName = extOptions.find { it.first == preferredExternalPlayerPackage }?.second ?: "Harici Sistem Oynatıcısı"
                                            KitsugiSettingsListItem(
                                                title = "Harici Oynatıcı Uygulaması",
                                                description = "Oynatma için hedeflenecek varsayılan harici paket",
                                                value = currentExtName,
                                                icon = Icons.Rounded.SettingsInputComponent,
                                                iconColor = accentColor,
                                                onClick = { extPackageDropdownExpanded = true }
                                            )
                                            KitsugiDropdownMenu(expanded = extPackageDropdownExpanded, onDismissRequest = { extPackageDropdownExpanded = false }) {
                                                extOptions.forEach { option ->
                                                    KitsugiDropdownItem(
                                                        text = option.second,
                                                        selected = option.first == preferredExternalPlayerPackage,
                                                        onClick = {
                                                            onPreferredExternalPlayerPackageSelected(option.first)
                                                            extPackageDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            item {
                                KitsugiSettingsSection(title = "Otomatik Oynatma & Geçişler") {
                                    KitsugiSettingsSwitchItem(
                                        title = "Otomatik Oynatma",
                                        description = "Sonraki bölüme otomatik olarak geçiş yap",
                                        checked = isAutoplayEnabled,
                                        icon = Icons.Rounded.FastForward,
                                        iconColor = accentColor,
                                        onCheckedChange = onAutoplayEnabledChanged
                                    )
                                    
                                    KitsugiSettingsDivider()
                                    
                                    Box {
                                        val introOptions = listOf(
                                            0 to "Devre Dışı",
                                            3 to "3 Saniye",
                                            5 to "5 Saniye",
                                            10 to "10 Saniye",
                                            15 to "15 Saniye"
                                        )
                                        val currentIntroName = introOptions.find { it.first == skipIntroDurationSec }?.second ?: "5 Saniye"
                                        KitsugiSettingsListItem(
                                            title = "İntro Atlama Süresi",
                                            description = "+85s atlama butonunun başlangıçta intro süresine göre ayarlanması",
                                            value = currentIntroName,
                                            icon = Icons.Rounded.SkipNext,
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
                                }
                            }

                            item {
                                KitsugiSettingsSection(title = "Oynatıcı Arayüzü Bilgileri") {
                                    KitsugiSettingsSwitchItem(
                                        title = "Bölüm Başlığını Göster",
                                        description = "Üst kontrolde oynatılan medyanın adını göster",
                                        checked = showPlayerTitle,
                                        icon = Icons.Rounded.Title,
                                        iconColor = accentColor,
                                        onCheckedChange = onShowPlayerTitleChanged
                                    )
                                    KitsugiSettingsDivider()
                                    KitsugiSettingsSwitchItem(
                                        title = "Çözünürlük Göster",
                                        description = "Üst kontrolde video çözünürlüğünü göster (örn: 1080p)",
                                        checked = showPlayerResolution,
                                        icon = Icons.Rounded.Hd,
                                        iconColor = accentColor,
                                        onCheckedChange = onShowPlayerResolutionChanged
                                    )
                                    KitsugiSettingsDivider()
                                    KitsugiSettingsSwitchItem(
                                        title = "Medya / Codec Bilgisi",
                                        description = "Video codec, fps ve bitrate bilgilerini göster",
                                        checked = showMediaInfo,
                                        icon = Icons.Rounded.Info,
                                        iconColor = accentColor,
                                        onCheckedChange = onShowMediaInfoChanged
                                    )
                                }
                            }

                            item {
                                KitsugiSettingsSection(title = "Hareketsizlik ve Oynatma Kuralları") {
                                    KitsugiSettingsSwitchItem(
                                        title = "Hâlâ İzliyor musun?",
                                        description = "Belirli bir süre hareketsizlik sonrası oynatmayı durdur ve sor",
                                        checked = stillWatchingEnabled,
                                        icon = Icons.Rounded.QuestionMark,
                                        iconColor = accentColor,
                                        onCheckedChange = onStillWatchingEnabledChanged
                                    )
                                    if (stillWatchingEnabled) {
                                        KitsugiSettingsDivider()
                                        Box {
                                            val thresholdOptions = listOf(
                                                45 to "45 Dakika",
                                                90 to "90 Dakika (Varsayılan)",
                                                120 to "120 Dakika",
                                                180 to "180 Dakika"
                                            )
                                            val currentThresholdName = thresholdOptions.find { it.first == stillWatchingThresholdMinutes }?.second ?: "${stillWatchingThresholdMinutes} Dakika"
                                            KitsugiSettingsListItem(
                                                title = "Hareketsizlik Eşiği",
                                                description = "Ne kadar süre sonra uyarı gösterileceğini seçin",
                                                value = currentThresholdName,
                                                icon = Icons.Rounded.HourglassEmpty,
                                                iconColor = accentColor,
                                                onClick = { stillWatchingDropdownExpanded = true }
                                            )
                                            KitsugiDropdownMenu(expanded = stillWatchingDropdownExpanded, onDismissRequest = { stillWatchingDropdownExpanded = false }) {
                                                thresholdOptions.forEach { option ->
                                                    KitsugiDropdownItem(
                                                        text = option.second,
                                                        selected = option.first == stillWatchingThresholdMinutes,
                                                        onClick = {
                                                            onStillWatchingThresholdMinutesChanged(option.first)
                                                            stillWatchingDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    KitsugiSettingsDivider()

                                    Box {
                                        val postOptions = listOf(
                                            "MANUAL" to "Bölüm bittiğinde manuel geçiş yap",
                                            "AUTO_PLAY_NEXT" to "Doğrudan sonraki bölümü oynat",
                                            "BINGE_PROMPT" to "Sonraki bölüm için sor"
                                        )
                                        val currentPostName = postOptions.find { it.first == postPlayMode }?.second ?: "Doğrudan sonraki bölümü oynat"
                                        KitsugiSettingsListItem(
                                            title = "Sonraki Bölüm Modu",
                                            description = "Bölüm bittiğinde yapılacak varsayılan eylem",
                                            value = currentPostName,
                                            icon = Icons.Rounded.QueuePlayNext,
                                            iconColor = accentColor,
                                            onClick = { postPlayDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = postPlayDropdownExpanded, onDismissRequest = { postPlayDropdownExpanded = false }) {
                                            postOptions.forEach { option ->
                                                KitsugiDropdownItem(
                                                    text = option.second,
                                                    selected = option.first == postPlayMode,
                                                    onClick = {
                                                        onPostPlayModeChanged(option.first)
                                                        postPlayDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    KitsugiSettingsDivider()

                                    Box {
                                        val limitOptions = listOf(
                                            0 to "Sınırsız (Otomatik Oynat)",
                                            1 to "1 Bölüm",
                                            2 to "2 Bölüm",
                                            3 to "3 Bölüm",
                                            5 to "5 Bölüm"
                                        )
                                        val currentLimitName = limitOptions.find { it.first == autoplaySessionLimit }?.second ?: "${autoplaySessionLimit} Bölüm"
                                        KitsugiSettingsListItem(
                                            title = "Otomatik Oynatma Limiti",
                                            description = "Kullanıcı etkileşimi olmadan art arda kaç bölüm oynatılabileceğini sınırlayın",
                                            value = currentLimitName,
                                            icon = Icons.Rounded.Timer,
                                            iconColor = accentColor,
                                            onClick = { autoplayLimitDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = autoplayLimitDropdownExpanded, onDismissRequest = { autoplayLimitDropdownExpanded = false }) {
                                            limitOptions.forEach { option ->
                                                KitsugiDropdownItem(
                                                    text = option.second,
                                                    selected = option.first == autoplaySessionLimit,
                                                    onClick = {
                                                        onAutoplaySessionLimitChanged(option.first)
                                                        autoplayLimitDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PlayerSettingsSubScreen.Hareketler -> {
                        LazyColumn(
                            state = subScreenScrollState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                KitsugiSettingsSection(title = "Kaydırma Jesti Kontrolleri") {
                                    KitsugiSettingsSwitchItem(
                                        title = "Ses Kaydırma Jesti",
                                        description = "Ekranın dikey kaydırılmasıyla ses seviyesini ayarla",
                                        checked = gestureVolumeEnabled,
                                        icon = Icons.Rounded.VolumeUp,
                                        iconColor = accentColor,
                                        onCheckedChange = onGestureVolumeEnabledChanged
                                    )
                                    KitsugiSettingsDivider()
                                    KitsugiSettingsSwitchItem(
                                        title = "Parlaklık Kaydırma Jesti",
                                        description = "Ekranın dikey kaydırılmasıyla ekran parlaklığını ayarla",
                                        checked = gestureBrightnessEnabled,
                                        icon = Icons.Rounded.BrightnessMedium,
                                        iconColor = accentColor,
                                        onCheckedChange = onGestureBrightnessEnabledChanged
                                    )
                                    KitsugiSettingsDivider()
                                    KitsugiSettingsSwitchItem(
                                        title = "Çimdikleme (Pinch-to-Zoom)",
                                        description = "İki parmakla yakınlaştırarak ekran modunu ayarla",
                                        checked = gestureZoomEnabled,
                                        icon = Icons.Rounded.ZoomIn,
                                        iconColor = accentColor,
                                        onCheckedChange = onGestureZoomEnabledChanged
                                    )
                                }
                            }

                            item {
                                KitsugiSettingsSection(title = "Jest Hassasiyetleri") {
                                    // Scroll Sensitivity Slider
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Text(
                                            text = "Kaydırma Hassasiyeti: ${"%.1f".format(gestureScrollSensitivity)}x",
                                            color = KitsugiColors.TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Slider(
                                            value = gestureScrollSensitivity,
                                            onValueChange = onGestureScrollSensitivityChanged,
                                            valueRange = 0.5f..2.5f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = accentColor,
                                                activeTrackColor = accentColor
                                            )
                                        )
                                    }

                                    KitsugiSettingsDivider()

                                    // Double Tap Seek Seconds selection
                                    Box {
                                        val seekSecOptions = listOf(5, 10, 15, 20, 30)
                                        KitsugiSettingsListItem(
                                            title = "Çift Dokunma Arama Süresi",
                                            description = "Ekranın sağına/soluna çift dokunulduğunda atlanacak saniye",
                                            value = "${doubleTapSeekSeconds} Saniye",
                                            icon = Icons.Rounded.DoubleArrow,
                                            iconColor = accentColor,
                                            onClick = { doubleTapSeekDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = doubleTapSeekDropdownExpanded, onDismissRequest = { doubleTapSeekDropdownExpanded = false }) {
                                            seekSecOptions.forEach { seconds ->
                                                KitsugiDropdownItem(
                                                    text = "$seconds Saniye",
                                                    selected = seconds == doubleTapSeekSeconds,
                                                    onClick = {
                                                        onDoubleTapSeekSecondsSelected(seconds)
                                                        doubleTapSeekDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    KitsugiSettingsDivider()

                                    // Hold Speed Multiplier
                                    Box {
                                        val holdOptions = listOf(1.5f, 2.0f, 2.5f, 3.0f)
                                        KitsugiSettingsListItem(
                                            title = "Basılı Tutma Hız Çarpanı",
                                            description = "Ekrana uzun basıldığında oynatılacak video hızı çarpanı",
                                            value = "${holdSpeedMultiplier}x",
                                            icon = Icons.Rounded.Speed,
                                            iconColor = accentColor,
                                            onClick = { holdSpeedDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = holdSpeedDropdownExpanded, onDismissRequest = { holdSpeedDropdownExpanded = false }) {
                                            holdOptions.forEach { multiplier ->
                                                KitsugiDropdownItem(
                                                    text = "${multiplier}x",
                                                    selected = multiplier == holdSpeedMultiplier,
                                                    onClick = {
                                                        onHoldSpeedMultiplierSelected(multiplier)
                                                        holdSpeedDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                KitsugiSettingsSection(title = "Gelişmiş Jestler") {
                                    val swipeSides = appSettings?.swipeVolumeBrightnessSides ?: true
                                    KitsugiSettingsSwitchItem(
                                        title = "Jest Yönlerini Ters Çevir",
                                        description = if (swipeSides) "Sol: Ses, Sağ: Parlaklık (Varsayılan)" else "Sol: Parlaklık, Sağ: Ses",
                                        checked = !swipeSides,
                                        icon = Icons.Rounded.CompareArrows,
                                        iconColor = accentColor,
                                        onCheckedChange = {
                                            scope.launch {
                                                settingsDataStore.setSwipeVolumeBrightnessSides(!it)
                                            }
                                        }
                                    )

                                    KitsugiSettingsDivider()

                                    val horizontalSeek = appSettings?.horizontalSeekGestureEnabled ?: true
                                    KitsugiSettingsSwitchItem(
                                        title = "Yatay Arama Jesti",
                                        description = "Ekranda yatay sürükleme ile ileri/geri sar",
                                        checked = horizontalSeek,
                                        icon = Icons.Rounded.SettingsEthernet,
                                        iconColor = accentColor,
                                        onCheckedChange = {
                                            scope.launch {
                                                settingsDataStore.setHorizontalSeekGestureEnabled(it)
                                            }
                                        }
                                    )

                                    KitsugiSettingsDivider()

                                    val preciseSeek = appSettings?.preciseSeeking ?: false
                                    KitsugiSettingsSwitchItem(
                                        title = "Hassas Arama Modu",
                                        description = "Yatay sürüklemede kare kare yavaş/hassas hareket et",
                                        checked = preciseSeek,
                                        icon = Icons.Rounded.CenterFocusWeak,
                                        iconColor = accentColor,
                                        onCheckedChange = {
                                            scope.launch {
                                                settingsDataStore.setPreciseSeeking(it)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    PlayerSettingsSubScreen.KodCozucu -> {
                        LazyColumn(
                            state = subScreenScrollState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {


                            item {
                                KitsugiSettingsSection(title = "MPV Motoru Konfigürasyonları") {
                                    Box {
                                        val hwdecOptions = listOf("auto", "auto-safe", "no")
                                        val currentHwdec = appSettings?.mpvHwdecMode ?: "auto-safe"
                                        KitsugiSettingsListItem(
                                            title = "MPV Donanım Hızlandırma (hwdec)",
                                            description = "MPV donanım video çözücü modu",
                                            value = currentHwdec,
                                            icon = Icons.Rounded.Memory,
                                            iconColor = accentColor,
                                            onClick = { mpvHwdecDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = mpvHwdecDropdownExpanded, onDismissRequest = { mpvHwdecDropdownExpanded = false }) {
                                            hwdecOptions.forEach { mode ->
                                                KitsugiDropdownItem(
                                                    text = mode,
                                                    selected = mode == currentHwdec,
                                                    onClick = {
                                                        scope.launch { settingsDataStore.setMpvHwdecMode(mode) }
                                                        mpvHwdecDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    KitsugiSettingsDivider()

                                    Box {
                                        val gpuOptions = listOf("gpu", "gpu-next")
                                        val currentGpu = appSettings?.mpvGpuRenderer ?: "gpu"
                                        KitsugiSettingsListItem(
                                            title = "MPV GPU Renderer Backend",
                                            description = "Yüksek kaliteli render motoru backend seçimi",
                                            value = currentGpu,
                                            icon = Icons.Rounded.SettingsApplications,
                                            iconColor = accentColor,
                                            onClick = { mpvGpuDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = mpvGpuDropdownExpanded, onDismissRequest = { mpvGpuDropdownExpanded = false }) {
                                            gpuOptions.forEach { renderer ->
                                                KitsugiDropdownItem(
                                                    text = renderer,
                                                    selected = renderer == currentGpu,
                                                    onClick = {
                                                        scope.launch { settingsDataStore.setMpvGpuRenderer(renderer) }
                                                        mpvGpuDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    KitsugiSettingsDivider()

                                    Box {
                                        val debandOptions = listOf("none", "cpu", "gpu")
                                        val currentDeband = appSettings?.mpvDebandMode ?: "none"
                                        KitsugiSettingsListItem(
                                            title = "Video Debanding (Bantlaşmayı Önleme)",
                                            description = "Renk geçişlerindeki çizgileri yumuşatır",
                                            value = currentDeband,
                                            icon = Icons.Rounded.BlurOn,
                                            iconColor = accentColor,
                                            onClick = { mpvDebandDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = mpvDebandDropdownExpanded, onDismissRequest = { mpvDebandDropdownExpanded = false }) {
                                            debandOptions.forEach { mode ->
                                                KitsugiDropdownItem(
                                                    text = mode,
                                                    selected = mode == currentDeband,
                                                    onClick = {
                                                        scope.launch { settingsDataStore.setMpvDebandMode(mode) }
                                                        mpvDebandDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    KitsugiSettingsDivider()

                                    val forceYuv = appSettings?.mpvForceYuv420p ?: false
                                    KitsugiSettingsSwitchItem(
                                        title = "YUV420P Renk Düzenini Zorla",
                                        description = "Eski cihazlarda yeşil ekran/çökme sorunlarını giderir",
                                        checked = forceYuv,
                                        icon = Icons.Rounded.ColorLens,
                                        iconColor = accentColor,
                                        onCheckedChange = {
                                            scope.launch { settingsDataStore.setMpvForceYuv420p(it) }
                                        }
                                    )

                                    KitsugiSettingsDivider()

                                    val demuxerCache = appSettings?.mpvDemuxerCacheMb ?: 64
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Text(
                                            text = "MPV Demuxer Önbelleği: ${demuxerCache} MB",
                                            color = KitsugiColors.TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Slider(
                                            value = demuxerCache.toFloat(),
                                            onValueChange = { scope.launch { settingsDataStore.setMpvDemuxerCacheMb(it.toInt()) } },
                                            valueRange = 8f..512f,
                                            steps = 63,
                                            colors = SliderDefaults.colors(
                                                thumbColor = accentColor,
                                                activeTrackColor = accentColor
                                            )
                                        )
                                    }
                                }
                            }


                        }
                    }

                    PlayerSettingsSubScreen.Altyazilar -> {
                        LazyColumn(
                            state = subScreenScrollState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                KitsugiSettingsSection(title = "Altyazı Stili") {
                                    Box {
                                        val sizeOptions = listOf(
                                            12 to "12sp (Çok Küçük)",
                                            14 to "14sp (Küçük)",
                                            16 to "16sp (Normal)",
                                            18 to "18sp (Büyük)",
                                            20 to "20sp (Çok Büyük)",
                                            24 to "24sp (Devasa)"
                                        )
                                        val currentSizeLabel = sizeOptions.find { it.first == defaultSubtitleSize }?.second ?: "${defaultSubtitleSize}sp"
                                        KitsugiSettingsListItem(
                                            title = "Yazı Boyutu",
                                            description = "Altyazı ekran heights oranlama boyutu",
                                            value = currentSizeLabel,
                                            icon = Icons.Rounded.TextFields,
                                            iconColor = accentColor,
                                            onClick = { subSizeDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = subSizeDropdownExpanded, onDismissRequest = { subSizeDropdownExpanded = false }) {
                                            sizeOptions.forEach { option ->
                                                KitsugiDropdownItem(
                                                    text = option.second,
                                                    selected = option.first == defaultSubtitleSize,
                                                    onClick = {
                                                        onDefaultSubtitleSizeSelected(option.first)
                                                        subSizeDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    KitsugiSettingsDivider()

                                    Box {
                                        val colorOptions = listOf(
                                            0xFFFFFFFF.toInt() to "Beyaz",
                                            0xFFFFFF00.toInt() to "Sarı",
                                            0xFF00FF00.toInt() to "Yeşil",
                                            0xFF00FFFF.toInt() to "Mavi",
                                            0xFFFF0000.toInt() to "Kırmızı"
                                        )
                                        val currentColorLabel = colorOptions.find { it.first == defaultSubtitleColor }?.second ?: "Özel"
                                        KitsugiSettingsListItem(
                                            title = "Yazı Rengi",
                                            description = "Altyazıların birincil yazı rengi",
                                            value = currentColorLabel,
                                            icon = Icons.Rounded.ColorLens,
                                            iconColor = accentColor,
                                            onClick = { subColorDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = subColorDropdownExpanded, onDismissRequest = { subColorDropdownExpanded = false }) {
                                            colorOptions.forEach { option ->
                                                KitsugiDropdownItem(
                                                    text = option.second,
                                                    selected = option.first == defaultSubtitleColor,
                                                    onClick = {
                                                        onDefaultSubtitleColorSelected(option.first)
                                                        subColorDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    KitsugiSettingsDivider()

                                    KitsugiSettingsSwitchItem(
                                        title = "Altyazıları Kalınlaştır (Bold)",
                                        description = "Yazı tipini kalınlaştırır",
                                        checked = subtitleBold,
                                        icon = Icons.Rounded.FormatBold,
                                        iconColor = accentColor,
                                        onCheckedChange = onSubtitleBoldChanged
                                    )

                                    KitsugiSettingsDivider()

                                    val italicSub = appSettings?.subtitleItalic ?: false
                                    KitsugiSettingsSwitchItem(
                                        title = "Altyazıları İtalik Yap",
                                        description = "Yazı tipini italik moduna geçirir",
                                        checked = italicSub,
                                        icon = Icons.Rounded.FormatItalic,
                                        iconColor = accentColor,
                                        onCheckedChange = {
                                            scope.launch { settingsDataStore.setSubtitleItalic(it) }
                                        }
                                    )

                                    KitsugiSettingsDivider()

                                    KitsugiSettingsSwitchItem(
                                        title = "Altyazı Metin Kenarlığı (Outline)",
                                        description = "Metnin okunabilirliğini artırmak için siyah kenarlık ekler",
                                        checked = subtitleOutlineEnabled,
                                        icon = Icons.Rounded.FormatPaint,
                                        iconColor = accentColor,
                                        onCheckedChange = onSubtitleOutlineEnabledChanged
                                    )

                                    KitsugiSettingsDivider()

                                    Box {
                                        val justificationOptions = listOf(
                                            "left" to "Sol",
                                            "center" to "Orta (Varsayılan)",
                                            "right" to "Sağ"
                                        )
                                        val currentJust = appSettings?.subtitleJustification ?: "center"
                                        val currentJustLabel = justificationOptions.find { it.first == currentJust }?.second ?: "Orta"
                                        KitsugiSettingsListItem(
                                            title = "Altyazı Hizalaması",
                                            description = "Altyazı satırlarının hizalanacağı yön",
                                            value = currentJustLabel,
                                            icon = Icons.Rounded.FormatAlignLeft,
                                            iconColor = accentColor,
                                            onClick = { subJustificationDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = subJustificationDropdownExpanded, onDismissRequest = { subJustificationDropdownExpanded = false }) {
                                            justificationOptions.forEach { option ->
                                                KitsugiDropdownItem(
                                                    text = option.second,
                                                    selected = option.first == currentJust,
                                                    onClick = {
                                                        scope.launch { settingsDataStore.setSubtitleJustification(option.first) }
                                                        subJustificationDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    KitsugiSettingsDivider()

                                    Box {
                                        val bgColors = listOf(
                                            0 to "Şeffaf (Yok)",
                                            0xFF000000.toInt() to "Siyah",
                                            0x80000000.toInt() to "Yarı Şeffaf Siyah"
                                        )
                                        val currentBg = appSettings?.subtitleBackgroundColor ?: 0
                                        val currentBgLabel = bgColors.find { it.first == currentBg }?.second ?: "Özel"
                                        KitsugiSettingsListItem(
                                            title = "Arka Plan Rengi",
                                            description = "Altyazı arkasındaki kutu veya şerit rengi",
                                            value = currentBgLabel,
                                            icon = Icons.Rounded.SelectAll,
                                            iconColor = accentColor,
                                            onClick = { subBgColorDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = subBgColorDropdownExpanded, onDismissRequest = { subBgColorDropdownExpanded = false }) {
                                            bgColors.forEach { option ->
                                                KitsugiDropdownItem(
                                                    text = option.second,
                                                    selected = option.first == currentBg,
                                                    onClick = {
                                                        scope.launch { settingsDataStore.setSubtitleBackgroundColor(option.first) }
                                                        subBgColorDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    KitsugiSettingsDivider()

                                    val shadowOffset = appSettings?.subtitleShadowOffset ?: 1.5f
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Text(
                                            text = "Gölge Uzaklığı: ${"%.1f".format(shadowOffset)} dp",
                                            color = KitsugiColors.TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Slider(
                                            value = shadowOffset,
                                            onValueChange = { scope.launch { settingsDataStore.setSubtitleShadowOffset(it) } },
                                            valueRange = 0f..8f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = accentColor,
                                                activeTrackColor = accentColor
                                            )
                                        )
                                    }

                                    KitsugiSettingsDivider()

                                    Box {
                                        val borderColors = listOf(
                                            0xFF000000.toInt() to "Siyah (Varsayılan)",
                                            0xFFFFFFFF.toInt() to "Beyaz"
                                        )
                                        val currentBorderColor = appSettings?.subtitleBorderColor ?: 0xFF000000.toInt()
                                        val currentBorderColorLabel = borderColors.find { it.first == currentBorderColor }?.second ?: "Özel"
                                        KitsugiSettingsListItem(
                                            title = "Kenarlık / Gölge Rengi",
                                            description = "Altyazı gölgesinin veya dış kenarlığının rengi",
                                            value = currentBorderColorLabel,
                                            icon = Icons.Rounded.BorderOuter,
                                            iconColor = accentColor,
                                            onClick = { subBorderColorDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = subBorderColorDropdownExpanded, onDismissRequest = { subBorderColorDropdownExpanded = false }) {
                                            borderColors.forEach { option ->
                                                KitsugiDropdownItem(
                                                    text = option.second,
                                                    selected = option.first == currentBorderColor,
                                                    onClick = {
                                                        scope.launch { settingsDataStore.setSubtitleBorderColor(option.first) }
                                                        subBorderColorDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    KitsugiSettingsDivider()

                                    val borderSize = appSettings?.subtitleBorderSize ?: 1.5f
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Text(
                                            text = "Kenarlık Kalınlığı: ${"%.1f".format(borderSize)} dp",
                                            color = KitsugiColors.TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Slider(
                                            value = borderSize,
                                            onValueChange = { scope.launch { settingsDataStore.setSubtitleBorderSize(it) } },
                                            valueRange = 0f..6f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = accentColor,
                                                activeTrackColor = accentColor
                                            )
                                        )
                                    }
                                }
                            }

                            item {
                                KitsugiSettingsSection(title = "Altyazı Dil Tercihleri") {
                                    // ── Chip tabanlı çok-seçimli dil seçici ──────────────────
                                    // Dil kodları ISO 639-1 standardında virgülle ayrılmış string
                                    // olarak kaydedilir (örn: "tr,en"). Varsayılan: "tr"
                                    val allSubtitleLanguages = remember {
                                        listOf(
                                            "tr" to "Türkçe",
                                            "en" to "İngilizce",
                                            "ja" to "Japonca",
                                            "ar" to "Arapça",
                                            "zh" to "Çince",
                                            "ko" to "Korece",
                                            "fr" to "Fransızca",
                                            "de" to "Almanca",
                                            "es" to "İspanyolca",
                                            "pt" to "Portekizce",
                                            "it" to "İtalyanca",
                                            "ru" to "Rusça",
                                            "nl" to "Hollandaca",
                                            "pl" to "Lehçe",
                                            "sv" to "İsveççe",
                                            "no" to "Norveççe",
                                            "da" to "Danca",
                                            "fi" to "Fince",
                                            "uk" to "Ukraynaca",
                                            "ro" to "Romence",
                                            "cs" to "Çekçe",
                                            "hu" to "Macarca",
                                            "he" to "İbranice",
                                            "id" to "Endonezyaca",
                                            "th" to "Tayca",
                                            "vi" to "Vietnamca"
                                        )
                                    }
                                    val selectedLangs = remember(preferredSubtitleLanguages) {
                                        mutableStateOf(
                                            preferredSubtitleLanguages
                                                .split(",")
                                                .map { it.trim().lowercase() }
                                                .filter { it.isNotBlank() }
                                                .toMutableSet()
                                        )
                                    }

                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.Language,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Tercih Edilen Altyazı Dilleri",
                                                    color = KitsugiColors.TextPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = if (selectedLangs.value.isEmpty())
                                                        "Dil seçilmedi — tüm diller indirilir"
                                                    else
                                                        "Seçili: ${selectedLangs.value.joinToString(", ").uppercase()} · Öncelik: ilk seçilen",
                                                    color = KitsugiColors.TextSecondary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        // Chip grid
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            allSubtitleLanguages.forEach { (code, label) ->
                                                val isSelected = code in selectedLangs.value
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        val updated = selectedLangs.value.toMutableSet()
                                                        if (isSelected) {
                                                            updated.remove(code)
                                                        } else {
                                                            updated.add(code)
                                                        }
                                                        selectedLangs.value = updated
                                                        val saved = updated.joinToString(",")
                                                        onPreferredSubtitleLanguagesSelected(saved)
                                                    },
                                                    label = {
                                                        Text(
                                                            text = "$code · $label",
                                                            fontSize = 12.sp,
                                                            color = if (isSelected) accentColor else KitsugiColors.TextSecondary
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = accentColor.copy(alpha = 0.18f),
                                                        selectedLabelColor = accentColor,
                                                        containerColor = KitsugiColors.Surface
                                                    ),
                                                    border = FilterChipDefaults.filterChipBorder(
                                                        enabled = true,
                                                        selected = isSelected,
                                                        selectedBorderColor = accentColor,
                                                        borderColor = KitsugiColors.TextSecondary.copy(alpha = 0.3f)
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    KitsugiSettingsDivider()

                                    Box {
                                        val startupModeOptions = listOf(
                                            "ALL_SUBTITLES" to "Tüm Altyazıları Yükle",
                                            "PREFERRED_ONLY" to "Yalnızca Tercih Edilen Dilleri Yükle"
                                        )
                                        val currentStartupModeName = startupModeOptions.find { it.first == addonSubtitleStartupMode }?.second ?: "Yalnızca Tercih Edilen Dilleri Yükle"
                                        KitsugiSettingsListItem(
                                            title = "Altyazı Yükleme Başlangıç Modu",
                                            description = "Eklentiden altyazıların nasıl çekileceği",
                                            value = currentStartupModeName,
                                            icon = Icons.Rounded.FilterList,
                                            iconColor = accentColor,
                                            onClick = { subStartupDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = subStartupDropdownExpanded, onDismissRequest = { subStartupDropdownExpanded = false }) {
                                            startupModeOptions.forEach { option ->
                                                KitsugiDropdownItem(
                                                    text = option.second,
                                                    selected = option.first == addonSubtitleStartupMode,
                                                    onClick = {
                                                        onAddonSubtitleStartupModeSelected(option.first)
                                                        subStartupDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PlayerSettingsSubScreen.Ses -> {
                        LazyColumn(
                            state = subScreenScrollState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                KitsugiSettingsSection(title = "Ses Güçlendirme (Boost)") {
                                    Box {
                                        val capOptions = listOf(100, 120, 150, 180, 200)
                                        val currentCap = appSettings?.volumeBoostCap ?: 200
                                        KitsugiSettingsListItem(
                                            title = "Maksimum Ses Limit Sınırı (Cap)",
                                            description = "Oynatıcının ses açma limit tavanı (%100 - %200)",
                                            value = "%$currentCap",
                                            icon = Icons.Rounded.Equalizer,
                                            iconColor = accentColor,
                                            onClick = { volBoostCapDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = volBoostCapDropdownExpanded, onDismissRequest = { volBoostCapDropdownExpanded = false }) {
                                            capOptions.forEach { cap ->
                                                KitsugiDropdownItem(
                                                    text = "%$cap",
                                                    selected = cap == currentCap,
                                                    onClick = {
                                                        scope.launch { settingsDataStore.setVolumeBoostCap(cap) }
                                                        volBoostCapDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    KitsugiSettingsDivider()

                                    Box {
                                        val boostOptions = listOf(
                                            0.0f to "Normal (%0)",
                                            0.25f to "Düşük (%25)",
                                            0.5f to "Orta (%50)",
                                            0.75f to "Yüksek (%75)",
                                            1.0f to "Maksimum (%100)"
                                        )
                                        val currentBoostName = boostOptions.find { it.first == defaultAudioBoost }?.second ?: "Normal (%0)"
                                        KitsugiSettingsListItem(
                                            title = "Varsayılan Ses Güçlendirme (Boost)",
                                            description = "Ses seviyesini ekstra yükseltme oranı",
                                            value = currentBoostName,
                                            icon = Icons.Rounded.VolumeUp,
                                            iconColor = accentColor,
                                            onClick = { audioBoostDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = audioBoostDropdownExpanded, onDismissRequest = { audioBoostDropdownExpanded = false }) {
                                            boostOptions.forEach { option ->
                                                KitsugiDropdownItem(
                                                    text = option.second,
                                                    selected = option.first == defaultAudioBoost,
                                                    onClick = {
                                                        onDefaultAudioBoostSelected(option.first)
                                                        audioBoostDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                KitsugiSettingsSection(title = "Ses Gecikmesi (Audio Delay)") {
                                    Box {
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
                                        val currentDelayName = delayOptions.find { it.first == defaultAudioDelayMs }?.second ?: "${defaultAudioDelayMs} ms"
                                        KitsugiSettingsListItem(
                                            title = "Varsayılan Ses Gecikmesi",
                                            description = "Altyazı ve video ile senkronize etmek için genel ses kaydırma",
                                            value = currentDelayName,
                                            icon = Icons.Rounded.AvTimer,
                                            iconColor = accentColor,
                                            onClick = { audioDelayDropdownExpanded = true }
                                        )
                                        KitsugiDropdownMenu(expanded = audioDelayDropdownExpanded, onDismissRequest = { audioDelayDropdownExpanded = false }) {
                                            delayOptions.forEach { option ->
                                                KitsugiDropdownItem(
                                                    text = option.second,
                                                    selected = option.first == defaultAudioDelayMs,
                                                    onClick = {
                                                        onDefaultAudioDelayMsSelected(option.first)
                                                        audioDelayDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                KitsugiSettingsSection(title = "Çıkış Rotalarına Göre Gecikme") {
                                    // Speaker Delay Slider
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Text(
                                            text = "Dahili Hoparlör Gecikmesi: ${speakerDelayMs} ms",
                                            color = KitsugiColors.TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Slider(
                                            value = speakerDelayMs.toFloat(),
                                            onValueChange = { onRouteDelayChanged(it.toLong(), bluetoothDelayMs, wiredDelayMs, hdmiDelayMs) },
                                            valueRange = -1000f..1000f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = accentColor,
                                                activeTrackColor = accentColor
                                            )
                                        )
                                    }

                                    KitsugiSettingsDivider()

                                    // Bluetooth Delay Slider
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Text(
                                            text = "Bluetooth Kulaklık Gecikmesi: ${bluetoothDelayMs} ms",
                                            color = KitsugiColors.TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Slider(
                                            value = bluetoothDelayMs.toFloat(),
                                            onValueChange = { onRouteDelayChanged(speakerDelayMs, it.toLong(), wiredDelayMs, hdmiDelayMs) },
                                            valueRange = -1000f..1000f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = accentColor,
                                                activeTrackColor = accentColor
                                            )
                                        )
                                    }

                                    KitsugiSettingsDivider()

                                    // Wired Headphones Delay Slider
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Text(
                                            text = "Kablolu Kulaklık Gecikmesi: ${wiredDelayMs} ms",
                                            color = KitsugiColors.TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Slider(
                                            value = wiredDelayMs.toFloat(),
                                            onValueChange = { onRouteDelayChanged(speakerDelayMs, bluetoothDelayMs, it.toLong(), hdmiDelayMs) },
                                            valueRange = -1000f..1000f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = accentColor,
                                                activeTrackColor = accentColor
                                            )
                                        )
                                    }

                                    KitsugiSettingsDivider()

                                    // HDMI Output Delay Slider
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Text(
                                            text = "HDMI / TV Gecikmesi: ${hdmiDelayMs} ms",
                                            color = KitsugiColors.TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Slider(
                                            value = hdmiDelayMs.toFloat(),
                                            onValueChange = { onRouteDelayChanged(speakerDelayMs, bluetoothDelayMs, wiredDelayMs, it.toLong()) },
                                            valueRange = -1000f..1000f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = accentColor,
                                                activeTrackColor = accentColor
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PlayerSettingsSubScreen.OzelButonlar -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Button(
                                onClick = { showButtonAddDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Yeni Özel Buton Ekle", color = Color.White)
                            }

                            LazyColumn(
                                state = subScreenScrollState,
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (customButtons.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Henüz özel bir buton eklenmedi.",
                                                color = KitsugiColors.TextSecondary
                                            )
                                        }
                                    }
                                } else {
                                    items(customButtons, key = { it.id }) { btn ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = KitsugiColors.SurfaceSoft
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = btn.name,
                                                        color = KitsugiColors.TextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Tıklama: ${btn.content}",
                                                        color = KitsugiColors.TextSecondary,
                                                        maxLines = 1,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(onClick = { showButtonEditDialog = btn }) {
                                                        Icon(
                                                            Icons.Rounded.Edit,
                                                            contentDescription = "Düzenle",
                                                            tint = KitsugiColors.TextSecondary
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            scope.launch {
                                                                db.customButtonDao().delete(btn.id)
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.Delete,
                                                            contentDescription = "Sil",
                                                            tint = accentColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PlayerSettingsSubScreen.KodDuzenleyici -> {
                        var scriptFolderState by remember { mutableStateOf("scripts") }
                        val scriptsDir = remember(context) { File(context.filesDir, "scripts").apply { mkdirs() } }
                        val scriptOptsDir = remember(context) { File(context.filesDir, "script-opts").apply { mkdirs() } }
                        val activeDir = if (scriptFolderState == "scripts") scriptsDir else scriptOptsDir
                        
                        var filesList by remember { mutableStateOf<List<File>>(emptyList()) }
                        var showCreateFileDialog by remember { mutableStateOf(false) }

                        LaunchedEffect(scriptFolderState) {
                            filesList = activeDir.listFiles()?.toList() ?: emptyList()
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Folder Tab Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { scriptFolderState = "scripts" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (scriptFolderState == "scripts") accentColor else KitsugiColors.SurfaceSoft
                                    )
                                ) {
                                    Text(
                                        "Scripts (Lua)",
                                        color = if (scriptFolderState == "scripts") Color.White else KitsugiColors.TextPrimary
                                    )
                                }
                                Button(
                                    onClick = { scriptFolderState = "script-opts" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (scriptFolderState == "script-opts") accentColor else KitsugiColors.SurfaceSoft
                                    )
                                ) {
                                    Text(
                                        "Script Options (Conf)",
                                        color = if (scriptFolderState == "script-opts") Color.White else KitsugiColors.TextPrimary
                                    )
                                }
                            }

                            Button(
                                onClick = { showCreateFileDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Yeni Dosya Oluştur", color = Color.White)
                            }

                            LazyColumn(
                                state = subScreenScrollState,
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (filesList.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Bu dizinde dosya bulunmuyor.",
                                                color = KitsugiColors.TextSecondary
                                            )
                                        }
                                    }
                                } else {
                                    items(filesList) { file ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = KitsugiColors.SurfaceSoft
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = file.name,
                                                    color = KitsugiColors.TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Row {
                                                    IconButton(
                                                        onClick = {
                                                            editingFile = file
                                                            fileContentText = file.readText()
                                                        }
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.Edit,
                                                            contentDescription = "Düzenle",
                                                            tint = KitsugiColors.TextSecondary
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            file.delete()
                                                            filesList = activeDir.listFiles()?.toList() ?: emptyList()
                                                        }
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.Delete,
                                                            contentDescription = "Sil",
                                                            tint = accentColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Create file dialog
                        if (showCreateFileDialog) {
                            var newFileName by remember { mutableStateOf("") }
                            AlertDialog(
                                onDismissRequest = { showCreateFileDialog = false },
                                containerColor = KitsugiColors.Surface,
                                title = { Text("Yeni Dosya Oluştur", color = KitsugiColors.TextPrimary) },
                                text = {
                                    OutlinedTextField(
                                        value = newFileName,
                                        onValueChange = { newFileName = it },
                                        label = { Text("Dosya Adı") },
                                        placeholder = { Text(if (scriptFolderState == "scripts") "script.lua" else "opts.conf") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = accentColor,
                                            focusedLabelColor = accentColor,
                                            focusedTextColor = KitsugiColors.TextPrimary,
                                            unfocusedTextColor = KitsugiColors.TextPrimary
                                        )
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            if (newFileName.isNotBlank()) {
                                                val suffix = if (scriptFolderState == "scripts") ".lua" else ".conf"
                                                val finalName = if (newFileName.endsWith(suffix)) newFileName else newFileName + suffix
                                                File(activeDir, finalName).writeText("")
                                                filesList = activeDir.listFiles()?.toList() ?: emptyList()
                                            }
                                            showCreateFileDialog = false
                                        }
                                    ) {
                                        Text("Oluştur", color = accentColor)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCreateFileDialog = false }) {
                                        Text("Vazgeç", color = KitsugiColors.TextSecondary)
                                    }
                                }
                            )
                        }
                    }

                    PlayerSettingsSubScreen.Gelismis -> {
                        LazyColumn(
                            state = subScreenScrollState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                KitsugiSettingsSection(title = "Kullanıcı MPV Yapılandırma Dosyaları") {
                                    val prefs = remember(context) { context.getSharedPreferences("kitsugi_prefs", Context.MODE_PRIVATE) }
                                    var userFilesEnabled by remember {
                                        mutableStateOf(prefs.getBoolean("mpv_user_files_enabled", true))
                                    }
                                    KitsugiSettingsSwitchItem(
                                        title = "Kullanıcı Dosyalarını Etkinleştir",
                                        description = "mpv.conf, input.conf ve lua scriptlerinin yüklenmesini kontrol eder",
                                        checked = userFilesEnabled,
                                        icon = Icons.Rounded.FolderOpen,
                                        iconColor = accentColor,
                                        onCheckedChange = {
                                            userFilesEnabled = it
                                            prefs.edit().putBoolean("mpv_user_files_enabled", it).apply()
                                        }
                                    )

                                    KitsugiSettingsDivider()

                                    KitsugiSettingsItem(
                                        title = "mpv.conf Düzenle",
                                        description = "Gelişmiş MPV render ve oynatma parametrelerini yapılandırın",
                                        icon = Icons.Rounded.EditNote,
                                        iconColor = accentColor,
                                        onClick = {
                                            val f = File(context.filesDir, "mpv.conf")
                                            if (!f.exists()) f.createNewFile()
                                            editingFile = f
                                            fileContentText = f.readText()
                                        }
                                    )

                                    KitsugiSettingsDivider()

                                    KitsugiSettingsItem(
                                        title = "input.conf Düzenle",
                                        description = "Oynatıcı içi buton ve jest kısayol eşlemelerini tanımlayın",
                                        icon = Icons.Rounded.Keyboard,
                                        iconColor = accentColor,
                                        onClick = {
                                            val f = File(context.filesDir, "input.conf")
                                            if (!f.exists()) f.createNewFile()
                                            editingFile = f
                                            fileContentText = f.readText()
                                        }
                                    )
                                }
                            }


                        }
                    }
                }
            }

            KitsugiSettingsDivider()

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

    // Text Editor Dialog (Fullscreen style Dialog)
    if (editingFile != null) {
        Dialog(
            onDismissRequest = { editingFile = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = KitsugiColors.Surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = editingFile?.name ?: "Kod Düzenleyici",
                            color = KitsugiColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row {
                            TextButton(onClick = { editingFile = null }) {
                                Text("İptal", color = KitsugiColors.TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    editingFile?.writeText(fileContentText)
                                    editingFile = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("Kaydet", color = Color.White)
                            }
                        }
                    }
                    KitsugiSettingsDivider()
                    OutlinedTextField(
                        value = fileContentText,
                        onValueChange = { fileContentText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            cursorColor = accentColor,
                            focusedTextColor = KitsugiColors.TextPrimary,
                            unfocusedTextColor = KitsugiColors.TextPrimary
                        )
                    )
                }
            }
        }
    }

    // Custom Button Edit Dialog
    if (showButtonEditDialog != null) {
        val btn = showButtonEditDialog!!
        var name by remember(btn) { mutableStateOf(btn.name) }
        var contentVal by remember(btn) { mutableStateOf(btn.content) }
        var longPressContentVal by remember(btn) { mutableStateOf(btn.longPressContent) }
        var onStartupVal by remember(btn) { mutableStateOf(btn.onStartup) }

        AlertDialog(
            onDismissRequest = { showButtonEditDialog = null },
            containerColor = KitsugiColors.Surface,
            title = { Text("Özel Butonu Düzenle", color = KitsugiColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Buton İsmi") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, cursorColor = accentColor, focusedLabelColor = accentColor, focusedTextColor = KitsugiColors.TextPrimary, unfocusedTextColor = KitsugiColors.TextPrimary)
                    )
                    OutlinedTextField(
                        value = contentVal,
                        onValueChange = { contentVal = it },
                        label = { Text("Basınca Çalışacak Kod (Lua / MPV)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, cursorColor = accentColor, focusedLabelColor = accentColor, focusedTextColor = KitsugiColors.TextPrimary, unfocusedTextColor = KitsugiColors.TextPrimary)
                    )
                    OutlinedTextField(
                        value = longPressContentVal,
                        onValueChange = { longPressContentVal = it },
                        label = { Text("Basılı Tutunca Çalışacak Kod") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, cursorColor = accentColor, focusedLabelColor = accentColor, focusedTextColor = KitsugiColors.TextPrimary, unfocusedTextColor = KitsugiColors.TextPrimary)
                    )
                    OutlinedTextField(
                        value = onStartupVal,
                        onValueChange = { onStartupVal = it },
                        label = { Text("Başlangıçta Çalışacak Kod") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, cursorColor = accentColor, focusedLabelColor = accentColor, focusedTextColor = KitsugiColors.TextPrimary, unfocusedTextColor = KitsugiColors.TextPrimary)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            db.customButtonDao().update(
                                btn.copy(
                                    name = name,
                                    content = contentVal,
                                    longPressContent = longPressContentVal,
                                    onStartup = onStartupVal
                                )
                            )
                        }
                        showButtonEditDialog = null
                    }
                ) {
                    Text("Kaydet", color = accentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showButtonEditDialog = null }) {
                    Text("İptal", color = KitsugiColors.TextSecondary)
                }
            }
        )
    }

    // Custom Button Add Dialog
    if (showButtonAddDialog) {
        var name by remember { mutableStateOf("") }
        var contentVal by remember { mutableStateOf("") }
        var longPressContentVal by remember { mutableStateOf("") }
        var onStartupVal by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showButtonAddDialog = false },
            containerColor = KitsugiColors.Surface,
            title = { Text("Yeni Özel Buton Ekle", color = KitsugiColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Buton İsmi") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, cursorColor = accentColor, focusedLabelColor = accentColor, focusedTextColor = KitsugiColors.TextPrimary, unfocusedTextColor = KitsugiColors.TextPrimary)
                    )
                    OutlinedTextField(
                        value = contentVal,
                        onValueChange = { contentVal = it },
                        label = { Text("Basınca Çalışacak Kod (Lua / MPV)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, cursorColor = accentColor, focusedLabelColor = accentColor, focusedTextColor = KitsugiColors.TextPrimary, unfocusedTextColor = KitsugiColors.TextPrimary)
                    )
                    OutlinedTextField(
                        value = longPressContentVal,
                        onValueChange = { longPressContentVal = it },
                        label = { Text("Basılı Tutunca Çalışacak Kod") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, cursorColor = accentColor, focusedLabelColor = accentColor, focusedTextColor = KitsugiColors.TextPrimary, unfocusedTextColor = KitsugiColors.TextPrimary)
                    )
                    OutlinedTextField(
                        value = onStartupVal,
                        onValueChange = { onStartupVal = it },
                        label = { Text("Başlangıçta Çalışacak Kod") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, cursorColor = accentColor, focusedLabelColor = accentColor, focusedTextColor = KitsugiColors.TextPrimary, unfocusedTextColor = KitsugiColors.TextPrimary)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val count = db.customButtonDao().getAll().size.toLong()
                            db.customButtonDao().insert(
                                CustomButton(
                                    name = name,
                                    content = contentVal,
                                    longPressContent = longPressContentVal,
                                    onStartup = onStartupVal,
                                    isFavorite = false,
                                    sortIndex = count
                                )
                            )
                        }
                        showButtonAddDialog = false
                    }
                ) {
                    Text("Ekle", color = accentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showButtonAddDialog = false }) {
                    Text("İptal", color = KitsugiColors.TextSecondary)
                }
            }
        )
    }
}
