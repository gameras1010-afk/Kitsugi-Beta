package com.kitsugi.animelist.ui.tv.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import com.kitsugi.animelist.ui.utils.tvClickable
import com.kitsugi.animelist.ui.app.AddonViewModel
import com.kitsugi.animelist.ui.app.AppViewModel
import com.kitsugi.animelist.ui.app.AuthViewModel
import com.kitsugi.animelist.data.local.MediaEntryRepository
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.ui.components.KitsugiTvQrLoginDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TvSettingsScreen(
    addonViewModel: AddonViewModel = viewModel(),
    appViewModel: AppViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    settingsDataStore: SettingsDataStore,
    mediaRepository: MediaEntryRepository,
    mediaEntries: List<MediaEntry>,
    onNavigateToAddons: () -> Unit = {},
    onNavigateToMangaExtension: () -> Unit = {},
    onNavigateToMangaSourceHealth: () -> Unit = {},
    onNavigateToCompanion: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by settingsDataStore.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())

    // Auth durumunu LaunchedEffect ile yükle
    LaunchedEffect(Unit) { authViewModel.refreshAuthState() }
    val isAniListConnected = authViewModel.isAniListConnected
    val isMalConnected = authViewModel.isMalConnected
    val isSimklConnected = authViewModel.isSimklConnected
    val isAniListImportRunning = authViewModel.isAniListImportRunning
    val isMalImportRunning = authViewModel.isMalImportRunning
    val isSimklImportRunning = authViewModel.isSimklImportRunning
    val isCrossSyncRunning = authViewModel.isCrossSyncRunning

    var selectedTab by remember { mutableStateOf(0) }
    var showTvQrDialog by remember { mutableStateOf(false) }

    // FocusRequester: sol panel → sağ içerik paneli arası DPAD geçişi
    val contentPanelFocusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = KitsugiTvTokens.Spacing.screenHorizontal,
                vertical = KitsugiTvTokens.Spacing.screenVertical
            ),
        horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.screenHorizontal)
    ) {
        // Left Column: Categories
        Column(
            modifier = Modifier
                .width(KitsugiTvTokens.Layout.sidebarExpandedWidth - 40.dp)
                .fillMaxHeight()
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.gridRowGap)
        ) {
            Text(
                text = "Ayarlar",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = KitsugiTvTokens.Spacing.contentPadding)
            )

            val categories = listOf("Görünüm", "Akış & Eklentiler", "Oynatıcı", "Hesap")
            categories.forEachIndexed { index, title ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = selectedTab == index

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            RoundedCornerShape(KitsugiTvTokens.Spacing.sm)
                        )
                        .focusProperties {
                            right = contentPanelFocusRequester
                        }
                        .tvClickable(shape = RoundedCornerShape(KitsugiTvTokens.Spacing.sm)) { selectedTab = index }
                        .onFocusChanged {
                            isFocused = it.isFocused
                            if (it.isFocused) {
                                selectedTab = index
                            }
                        }
                        .padding(
                            horizontal = KitsugiTvTokens.Spacing.contentPadding,
                            vertical = KitsugiTvTokens.Spacing.md
                        )
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
        val settingsListState = rememberLazyListState()

        // Right Column: Content panel based on selectedTab
        CompositionLocalProvider(LocalBringIntoViewSpec provides tvSpec) {
            LazyColumn(
                state = settingsListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRequester(contentPanelFocusRequester)
                    .focusRestorer()
                    .focusGroup()
                    .background(Color.White.copy(alpha = 0.03f), KitsugiTvTokens.Shapes.dialog as RoundedCornerShape)
                    .padding(KitsugiTvTokens.Spacing.rowGap)
                    .dpadVerticalFastScroll(scrollableState = settingsListState),
                verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap)
            ) {
                when (selectedTab) {
                    0 -> {
                        item {
                            Text(
                                text = "Görünüm Ayarları",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        item {
                            TvSettingsToggleRow(
                                title = "+18 İçerikleri Göster",
                                description = "Arama ve listeleme sonuçlarında yetişkin içerikleri filtreler veya açar.",
                                checked = settings.showAdultContent,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setShowAdultContent(checked) }
                                }
                            )
                        }

                        item {
                            TvSettingsToggleRow(
                                title = "Puanları Gizle",
                                description = "Genel puanları ve derecelendirmeleri arayüzden gizler.",
                                checked = settings.hideScores,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setHideScores(checked) }
                                }
                            )
                        }

                        item {
                            TvSettingsToggleRow(
                                title = "Anime Logolarını Göster",
                                description = "Desteklenen başlıklarda görsel logoları etkinleştirir.",
                                checked = settings.showAnimeLogos,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setShowAnimeLogos(checked) }
                                }
                            )
                        }

                        item {
                            val layoutText = when (settings.selectedHomeLayoutId) {
                                "classic" -> "Klasik"
                                "modern" -> "Modern"
                                "grid" -> "Izgara"
                                else -> "Klasik"
                            }
                            TvSettingsActionRow(
                                title = "Ana Sayfa Yerleşimi",
                                description = "TV ana sayfa düzenini değiştirin. Şu anki: $layoutText",
                                actionText = "Değiştir",
                                onClick = {
                                    val nextLayout = when (settings.selectedHomeLayoutId) {
                                        "classic" -> "modern"
                                        "modern" -> "grid"
                                        else -> "classic"
                                    }
                                    scope.launch { settingsDataStore.setSelectedHomeLayoutId(nextLayout) }
                                }
                            )
                        }

                        item {
                            val langText = when (settings.titleLanguage) {
                                "ENGLISH" -> "İngilizce"
                                "NATIVE" -> "Japonca"
                                "JAPANESE_STAFF" -> "Japonca (Personel)"
                                else -> "Romaji"
                            }
                            TvSettingsActionRow(
                                title = "Tercih Edilen Başlık Dili",
                                description = "Medya başlıklarının hangi dilde gösterileceğini ayarlar. Şu anki: $langText",
                                actionText = "Değiştir",
                                onClick = {
                                    val nextLang = when (settings.titleLanguage) {
                                        "ROMAJI" -> "ENGLISH"
                                        "ENGLISH" -> "NATIVE"
                                        "NATIVE" -> "JAPANESE_STAFF"
                                        else -> "ROMAJI"
                                    }
                                    scope.launch { settingsDataStore.setTitleLanguage(nextLang) }
                                }
                            )
                        }

                        item {
                            val formatText = when (settings.scoreFormat) {
                                "POINT_100" -> "100 Puan"
                                "POINT_10_DECIMAL" -> "10 Puan Ondalık"
                                "POINT_5" -> "5 Yıldız"
                                "POINT_3" -> "3 Durum (Gülen Yüz)"
                                "STARS" -> "Yıldızlı (★)"
                                else -> "10 Puan"
                            }
                            TvSettingsActionRow(
                                title = "Puanlama Formatı",
                                description = "Listelerinizdeki puanlama sistemini seçin. Şu anki: $formatText",
                                actionText = "Değiştir",
                                onClick = {
                                    val nextFormat = when (settings.scoreFormat) {
                                        "POINT_10" -> "POINT_100"
                                        "POINT_100" -> "POINT_10_DECIMAL"
                                        "POINT_10_DECIMAL" -> "POINT_5"
                                        "POINT_5" -> "POINT_3"
                                        "POINT_3" -> "STARS"
                                        else -> "POINT_10"
                                    }
                                    scope.launch { settingsDataStore.setScoreFormat(nextFormat) }
                                }
                            )
                        }
                    }
                    1 -> {
                        item {
                            Text(
                                text = "Akış & Eklentiler",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        item {
                            TvSettingsToggleRow(
                                title = "TMDB Zenginleştirme",
                                description = "Afişler, açıklamalar ve fragmanlar için TMDB veritabanı entegrasyonu.",
                                checked = settings.tmdbEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setTmdbEnabled(checked) }
                                }
                            )
                        }

                        item {
                            TvSettingsToggleRow(
                                title = "AniSkip Entegrasyonu",
                                description = "Bölüm giriş ve çıkışlarını (Intro/Outro) atlama zaman damgaları.",
                                checked = settings.aniSkipEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setAniSkipEnabled(checked) }
                                }
                            )
                        }

                        item {
                            TvSettingsActionRow(
                                title = "Eklenti & Havuz Yönetimi",
                                description = "Stremio eklentilerini ve Cloudstream havuzlarını yönetin, RealDebrid hesabınızı bağlayın.",
                                actionText = "Yönet",
                                onClick = onNavigateToAddons
                            )
                        }

                        item {
                            TvSettingsActionRow(
                                title = "Manga Eklentileri",
                                description = "Manga eklentilerini kurun, güncelleyin ve kaynak depolarını (Keiyoushi vb.) yönetin.",
                                actionText = "Yönet",
                                onClick = onNavigateToMangaExtension
                            )
                        }

                        item {
                            TvSettingsActionRow(
                                title = "Manga Kaynak Sağlığı",
                                description = "Kurulu manga kaynaklarının tanı verilerini inceleyin ve anlık sağlık kontrolü yapın.",
                                actionText = "Görüntüle",
                                onClick = onNavigateToMangaSourceHealth
                            )
                        }

                        item {
                            TvSettingsActionRow(
                                title = "📱 Telefon ile Yönet",
                                description = "Aynı Wi-Fi ağındaki telefonunuzdan eklentileri, API anahtarlarını ve ayarları yönetin.",
                                actionText = "Başlat",
                                onClick = onNavigateToCompanion
                            )
                        }
                    }
                    2 -> {
                        item {
                            Text(
                                text = "Oynatıcı Ayarları",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        item {
                            val playerText = when (settings.playerPreference.uppercase()) {
                                "MPV" -> "MPV Oynatıcı"
                                "EXTERNAL" -> "Harici Oynatıcı"
                                else -> "ExoPlayer (Dahili)"
                            }
                            TvSettingsActionRow(
                                title = "Oynatıcı Tercihi",
                                description = "Varsayılan video oynatıcı motorunu seçin. Şu anki: $playerText",
                                actionText = "Değiştir",
                                onClick = {
                                    val nextPreference = when (settings.playerPreference.uppercase()) {
                                        "INTERNAL" -> "MPV"
                                        "MPV" -> "EXTERNAL"
                                        else -> "INTERNAL"
                                    }
                                    scope.launch { settingsDataStore.setPlayerPreference(nextPreference) }
                                }
                            )
                        }

                        if (settings.playerPreference.uppercase() == "EXTERNAL") {
                            val availablePlayers = com.kitsugi.animelist.core.player.ExternalPlayerPackages.players
                            val currentIndex = availablePlayers.indexOfFirst { it.packageName == settings.preferredExternalPlayerPackage }.coerceAtLeast(0)
                            val currentExternalPlayerName = availablePlayers[currentIndex].name
                            item {
                                TvSettingsActionRow(
                                    title = "Tercih Edilen Harici Oynatıcı",
                                    description = "Harici oynatıcı olarak kullanılacak paket. Şu anki: $currentExternalPlayerName",
                                    actionText = "Değiştir",
                                    onClick = {
                                        val nextIndex = (currentIndex + 1) % availablePlayers.size
                                        val nextPlayer = availablePlayers[nextIndex]
                                        scope.launch {
                                            settingsDataStore.setPreferredExternalPlayerPackage(nextPlayer.packageName)
                                        }
                                    }
                                )
                            }
                        }

                        item {
                            TvSettingsToggleRow(
                                title = "Otomatik Oynat",
                                description = "Bir bölüm bittiğinde sıradaki bölümü otomatik olarak başlatır.",
                                checked = settings.isAutoplayEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setAutoplayEnabled(checked) }
                                }
                            )
                        }

                        item {
                            TvSettingsToggleRow(
                                title = "Altyazı Kalın Yazı Tipi",
                                description = "Video oynatıcıda altyazı yazı tipini kalın (Bold) yapar.",
                                checked = settings.subtitleBold,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setSubtitleBold(checked) }
                                }
                            )
                        }

                        // Secure DNS Configuration (Cycle through options)
                        item {
                            val dnsText = when (settings.dnsChoice) {
                                1 -> "Cloudflare DoH"
                                2 -> "Google DoH"
                                3 -> "AdGuard DoH"
                                else -> "Sistem Varsayılanı"
                            }
                            TvSettingsActionRow(
                                title = "Güvenli DNS (DoH)",
                                description = "İSS engellemelerini aşmak için DNS ayarlarını değiştirin. Şu anki: $dnsText",
                                actionText = "Değiştir",
                                onClick = {
                                    val nextChoice = (settings.dnsChoice + 1) % 4
                                    appViewModel.updateDnsChoice(nextChoice, settingsDataStore)
                                }
                            )
                        }

                        item {
                            val skipDur = settings.skipIntroDurationSec
                            TvSettingsActionRow(
                                title = "Intro Atlama Süresi",
                                description = "Bölüm başlangıcında intro atlama butonunun geçeceği süre. Şu anki: ${skipDur}sn",
                                actionText = "Değiştir",
                                onClick = {
                                    val nextSkip = when (skipDur) {
                                        0 -> 5
                                        5 -> 10
                                        10 -> 15
                                        15 -> 30
                                        30 -> 60
                                        60 -> 90
                                        else -> 0
                                    }
                                    scope.launch { settingsDataStore.setSkipIntroDurationSec(nextSkip) }
                                }
                            )
                        }

                        item {
                            val subSize = settings.defaultSubtitleSize
                            TvSettingsActionRow(
                                title = "Varsayılan Altyazı Boyutu",
                                description = "Video oynatıcıda gösterilecek altyazıların yazı boyutu. Şu anki: ${subSize}sp",
                                actionText = "Değiştir",
                                onClick = {
                                    val nextSize = when (subSize) {
                                        12 -> 14
                                        14 -> 16
                                        16 -> 18
                                        18 -> 20
                                        20 -> 24
                                        24 -> 12
                                        else -> 16
                                    }
                                    scope.launch { settingsDataStore.setDefaultSubtitleSize(nextSize) }
                                }
                            )
                        }

                        item {
                            val prefLang = settings.preferredSubtitleLanguages
                            val prefLangText = when (prefLang) {
                                "tr,en" -> "Türkçe & İngilizce (tr,en)"
                                "tr" -> "Sadece Türkçe (tr)"
                                "en" -> "Sadece İngilizce (en)"
                                else -> prefLang
                            }
                            TvSettingsActionRow(
                                title = "Tercih Edilen Altyazı Dili",
                                description = "Eklentilerden altyazı çekilirken öncelik verilen dil. Şu anki: $prefLangText",
                                actionText = "Değiştir",
                                onClick = {
                                    val nextLang = when (prefLang) {
                                        "tr,en" -> "tr"
                                        "tr" -> "en"
                                        else -> "tr,en"
                                    }
                                    scope.launch { settingsDataStore.setPreferredSubtitleLanguages(nextLang) }
                                }
                            )
                        }

                        item {
                            val startupMode = settings.addonSubtitleStartupMode
                            val startupModeText = when (startupMode) {
                                "ALL_SUBTITLES" -> "Tüm Altyazıları Yükle"
                                "PREFERRED_ONLY" -> "Yalnızca Tercih Edilen Dilleri Yükle"
                                else -> startupMode
                            }
                            TvSettingsActionRow(
                                title = "Altyazı Yükleme Modu",
                                description = "Altyazıların ne kadarının çekileceğini belirler. Şu anki: $startupModeText",
                                actionText = "Değiştir",
                                onClick = {
                                    val nextMode = when (startupMode) {
                                        "ALL_SUBTITLES" -> "PREFERRED_ONLY"
                                        else -> "ALL_SUBTITLES"
                                    }
                                    scope.launch { settingsDataStore.setAddonSubtitleStartupMode(nextMode) }
                                }
                            )
                        }

                        item {
                            val afrText = when (settings.frameRateMatchingMode) {
                                com.kitsugi.animelist.data.settings.FrameRateMatchingMode.START -> "Sadece Başlangıçta"
                                com.kitsugi.animelist.data.settings.FrameRateMatchingMode.START_STOP -> "Başlangıç ve Bitişte"
                                else -> "Kapalı"
                            }
                            TvSettingsActionRow(
                                title = "Otomatik Kare Hızı (AFR)",
                                description = "Ekran yenileme hızını video kare hızı (FPS) ile eşler. Şu anki: $afrText",
                                actionText = "Değiştir",
                                onClick = {
                                    val nextMode = when (settings.frameRateMatchingMode) {
                                        com.kitsugi.animelist.data.settings.FrameRateMatchingMode.OFF -> com.kitsugi.animelist.data.settings.FrameRateMatchingMode.START
                                        com.kitsugi.animelist.data.settings.FrameRateMatchingMode.START -> com.kitsugi.animelist.data.settings.FrameRateMatchingMode.START_STOP
                                        com.kitsugi.animelist.data.settings.FrameRateMatchingMode.START_STOP -> com.kitsugi.animelist.data.settings.FrameRateMatchingMode.OFF
                                    }
                                    scope.launch { settingsDataStore.setFrameRateMatchingMode(nextMode) }
                                }
                            )
                        }

                        item {
                            TvSettingsToggleRow(
                                title = "Ekran Çözünürlüğü Eşleme",
                                description = "TV ekran çözünürlüğünü video çözünürlüğüne göre otomatik değiştirir.",
                                checked = settings.resolutionMatchingEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setResolutionMatchingEnabled(checked) }
                                }
                            )
                        }

                        item {
                            val priorityText = when (settings.decoderPriority) {
                                1 -> "Yazılım Fallback"
                                2 -> "Yazılım Öncelikli"
                                else -> "Donanım Öncelikli"
                            }
                            TvSettingsActionRow(
                                title = "Dekoder Önceliği",
                                description = "Yazılımsal/donanımsal kod çözücü önceliğini belirler. Şu anki: $priorityText",
                                actionText = "Değiştir",
                                onClick = {
                                    val nextPriority = (settings.decoderPriority + 1) % 3
                                    scope.launch { settingsDataStore.setDecoderPriority(nextPriority) }
                                }
                            )
                        }

                        item {
                            val dvText = when (settings.dv7HandlingMode) {
                                com.kitsugi.animelist.data.settings.Dv7HandlingMode.AUTO -> "Otomatik (Cihaz Desteğine Göre)"
                                com.kitsugi.animelist.data.settings.Dv7HandlingMode.OFF -> "Kapalı (Doğal DV7)"
                                com.kitsugi.animelist.data.settings.Dv7HandlingMode.DV81_LIBDOVI -> "DV8.1 Dönüştürme (libdovi)"
                                com.kitsugi.animelist.data.settings.Dv7HandlingMode.HDR10_BASE_LAYER -> "HDR10 Base Layer"
                                com.kitsugi.animelist.data.settings.Dv7HandlingMode.STRIP_DV -> "DV Metadatasını Ayıkla"
                            }
                            TvSettingsActionRow(
                                title = "Dolby Vision (DV7) İşleme Modu",
                                description = "Dolby Vision Profile 7 videoların işlenme şeklini belirler. Şu anki: $dvText",
                                actionText = "Değiştir",
                                onClick = {
                                    val nextMode = when (settings.dv7HandlingMode) {
                                        com.kitsugi.animelist.data.settings.Dv7HandlingMode.AUTO -> com.kitsugi.animelist.data.settings.Dv7HandlingMode.OFF
                                        com.kitsugi.animelist.data.settings.Dv7HandlingMode.OFF -> com.kitsugi.animelist.data.settings.Dv7HandlingMode.DV81_LIBDOVI
                                        com.kitsugi.animelist.data.settings.Dv7HandlingMode.DV81_LIBDOVI -> com.kitsugi.animelist.data.settings.Dv7HandlingMode.HDR10_BASE_LAYER
                                        com.kitsugi.animelist.data.settings.Dv7HandlingMode.HDR10_BASE_LAYER -> com.kitsugi.animelist.data.settings.Dv7HandlingMode.STRIP_DV
                                        com.kitsugi.animelist.data.settings.Dv7HandlingMode.STRIP_DV -> com.kitsugi.animelist.data.settings.Dv7HandlingMode.AUTO
                                    }
                                    scope.launch { settingsDataStore.setDv7HandlingMode(nextMode) }
                                }
                            )
                        }

                        item {
                            TvSettingsToggleRow(
                                title = "HDR10+ SEI Metadatasını Ayıkla",
                                description = "HDR10+ akışlardaki dinamik SEI meta verilerini temizler.",
                                checked = settings.stripHdr10PlusSei,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setStripHdr10PlusSei(checked) }
                                }
                            )
                        }

                        // ─── T1-10 · TV Parity – Ek Oynatıcı Ayarları ───────────────────────

                        item {
                            TvSettingsToggleRow(
                                title = "İzliyorum Uyarısı",
                                description = "Belirlenen süre boyunca etkileşim olmadığında \"Hâlâ izliyor musunuz?\" ekranı gösterir.",
                                checked = settings.stillWatchingEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setStillWatchingEnabled(checked) }
                                }
                            )
                        }

                        if (settings.stillWatchingEnabled) {
                            item {
                                val thresholdMin = settings.stillWatchingThresholdMinutes
                                TvSettingsActionRow(
                                    title = "Hareketsizlik Eşiği",
                                    description = "\"İzliyorum\" uyarısını tetiklemek için gereken hareketsizlik süresi. Şu anki: ${thresholdMin} dk",
                                    actionText = "Değiştir",
                                    onClick = {
                                        val nextMin = when (thresholdMin) {
                                            10 -> 15
                                            15 -> 20
                                            20 -> 30
                                            30 -> 45
                                            45 -> 60
                                            else -> 10
                                        }
                                        scope.launch { settingsDataStore.setStillWatchingThresholdMinutes(nextMin) }
                                    }
                                )
                            }
                        }

                        item {
                            val postPlayText = when (settings.postPlayMode) {
                                "AUTOPLAY" -> "Otomatik Oynat"
                                "PROMPT" -> "Sor"
                                "MANUAL" -> "Manuel"
                                else -> "Otomatik Oynat"
                            }
                            TvSettingsActionRow(
                                title = "Bölüm Sonu Davranışı",
                                description = "Bölüm bittiğinde sıradaki bölüm için yapılacak işlemi belirler. Şu anki: $postPlayText",
                                actionText = "Değiştir",
                                onClick = {
                                    val nextMode = when (settings.postPlayMode) {
                                        "AUTOPLAY" -> "PROMPT"
                                        "PROMPT" -> "MANUAL"
                                        else -> "AUTOPLAY"
                                    }
                                    scope.launch { settingsDataStore.setPostPlayMode(nextMode) }
                                }
                            )
                        }

                        item {
                            val sessionLimit = settings.autoplaySessionLimit
                            val sessionText = if (sessionLimit == 0) "Sınırsız" else "$sessionLimit bölüm"
                            TvSettingsActionRow(
                                title = "Oturum Bölüm Limiti",
                                description = "Tek oturumda otomatik olarak oynatılacak maksimum bölüm sayısı. Şu anki: $sessionText",
                                actionText = "Değiştir",
                                onClick = {
                                    val nextLimit = when (sessionLimit) {
                                        0 -> 1
                                        1 -> 2
                                        2 -> 3
                                        3 -> 5
                                        5 -> 10
                                        else -> 0
                                    }
                                    scope.launch { settingsDataStore.setAutoplaySessionLimit(nextLimit) }
                                }
                            )
                        }

                        item {
                            TvSettingsToggleRow(
                                title = "Önizleme Seek Çubuğu",
                                description = "İleri/geri sarma sırasında video karesini küçük bir önizleme olarak gösterir.",
                                checked = settings.previewSeekbarEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setPreviewSeekbarEnabled(checked) }
                                }
                            )
                        }

                        item {
                            TvSettingsToggleRow(
                                title = "Paralel Parça İndirme",
                                description = "Akış sırasında arka planda paralel HTTP aralığı istekleri yaparak tampon dolumunu hızlandırır.",
                                checked = settings.parallelRangeEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setParallelRangeEnabled(checked) }
                                }
                            )
                        }

                        item {
                            TvSettingsToggleRow(
                                title = "Açılış Animasyonu",
                                description = "Uygulama başlatılırken gösterilen açılış animasyonunu etkinleştirir.",
                                checked = settings.splashAnimationEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setSplashAnimationEnabled(checked) }
                                }
                            )
                        }

                        item {
                            TvSettingsToggleRow(
                                title = "Açılış Sesi",
                                description = "Animasyon etkinken uygulama başlangıç sesi çalar.",
                                checked = settings.splashSoundEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setSplashSoundEnabled(checked) }
                                }
                            )
                        }
                    }
                    3 -> {
                        item {
                            Text(
                                text = "Hesap & Profil",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // 1. AniList
                        item {
                            if (isAniListConnected) {
                                TvSettingsActionRow(
                                    title = "AniList Hesabı (${settings.anilistUsername.ifBlank { "Bağlı" }})",
                                    description = if (isAniListImportRunning) "Senkronize ediliyor..." else "Listenizi AniList'ten içe aktarmak için tıklayın.",
                                    actionText = if (isAniListImportRunning) "Aktarılıyor" else "Eşitle",
                                    onClick = {
                                        if (!isAniListImportRunning) {
                                            authViewModel.importAniListAnimeList(mediaEntries, mediaRepository)
                                        }
                                    }
                                )
                            } else {
                                TvSettingsActionRow(
                                    title = "AniList Hesabını Bağla",
                                    description = "Hesabınızı bağlamak için QR kodu kullanın.",
                                    actionText = "Bağla",
                                    onClick = {
                                        showTvQrDialog = true
                                    }
                                )
                            }
                        }

                        if (isAniListConnected) {
                            item {
                                TvSettingsActionRow(
                                    title = "AniList Bağlantısını Kes",
                                    description = "AniList hesabınızı uygulamadan kaldırır.",
                                    actionText = "Bağlantıyı Kes",
                                    onClick = {
                                        authViewModel.disconnectExternalAccount("anilist")
                                    }
                                )
                            }
                        }

                        // 2. MyAnimeList
                        item {
                            if (isMalConnected) {
                                TvSettingsActionRow(
                                    title = "MyAnimeList Hesabı (${settings.malUsername.ifBlank { "Bağlı" }})",
                                    description = if (isMalImportRunning) "Senkronize ediliyor..." else "Listenizi MyAnimeList'ten içe aktarmak için tıklayın.",
                                    actionText = if (isMalImportRunning) "Aktarılıyor" else "Eşitle",
                                    onClick = {
                                        if (!isMalImportRunning) {
                                            authViewModel.importMalAnimeList(mediaEntries, mediaRepository)
                                        }
                                    }
                                )
                            } else {
                                TvSettingsActionRow(
                                    title = "MyAnimeList Hesabını Bağla",
                                    description = "Hesabınızı bağlamak için QR kodu kullanın.",
                                    actionText = "Bağla",
                                    onClick = {
                                        showTvQrDialog = true
                                    }
                                )
                            }
                        }

                        if (isMalConnected) {
                            item {
                                TvSettingsActionRow(
                                    title = "MyAnimeList Bağlantısını Kes",
                                    description = "MyAnimeList hesabınızı uygulamadan kaldırır.",
                                    actionText = "Bağlantıyı Kes",
                                    onClick = {
                                        authViewModel.disconnectExternalAccount("mal")
                                    }
                                )
                            }
                        }

                        // 3. Simkl
                        item {
                            if (isSimklConnected) {
                                TvSettingsActionRow(
                                    title = "Simkl Hesabı (${settings.simklUsername.ifBlank { "Bağlı" }})",
                                    description = if (isSimklImportRunning) "Senkronize ediliyor..." else "Listenizi Simkl'dan içe aktarmak için tıklayın.",
                                    actionText = if (isSimklImportRunning) "Aktarılıyor" else "Eşitle",
                                    onClick = {
                                        if (!isSimklImportRunning) {
                                            authViewModel.importSimklList(mediaEntries, mediaRepository)
                                        }
                                    }
                                )
                            } else {
                                TvSettingsActionRow(
                                    title = "Simkl Hesabını Bağla",
                                    description = "Hesabınızı bağlamak için QR kodu kullanın.",
                                    actionText = "Bağla",
                                    onClick = {
                                        showTvQrDialog = true
                                    }
                                )
                            }
                        }

                        if (isSimklConnected) {
                            item {
                                TvSettingsActionRow(
                                    title = "Simkl Bağlantısını Kes",
                                    description = "Simkl hesabınızı uygulamadan kaldırır.",
                                    actionText = "Bağlantıyı Kes",
                                    onClick = {
                                        authViewModel.disconnectExternalAccount("simkl")
                                    }
                                )
                            }
                        }

                        // 4. Çift Yönlü Eşitleme
                        if (isAniListConnected && isMalConnected) {
                            item {
                                TvSettingsActionRow(
                                    title = "Hesapları Birbiriyle Eşitle",
                                    description = if (isCrossSyncRunning) "Eşitleme yapılıyor..." else "AniList ve MyAnimeList verilerini karşılıklı güncelleyin.",
                                    actionText = if (isCrossSyncRunning) "Eşitleniyor" else "Eşitle",
                                    onClick = {
                                        if (!isCrossSyncRunning) {
                                            authViewModel.syncPlatforms(mediaRepository)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }



    if (showTvQrDialog) {
        KitsugiTvQrLoginDialog(
            authViewModel = authViewModel,
            onDismiss = { showTvQrDialog = false }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSettingsActionRow(
    title: String,
    description: String,
    actionText: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isFocused) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                KitsugiTvTokens.Shapes.posterCard as RoundedCornerShape
            )
            .tvClickable(shape = KitsugiTvTokens.Shapes.posterCard as RoundedCornerShape) { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .padding(KitsugiTvTokens.Spacing.contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.width(KitsugiTvTokens.Spacing.contentPadding))

        Box(
            modifier = Modifier
                .width(100.dp)
                .height(36.dp)
                .clip(KitsugiTvTokens.Shapes.chip as RoundedCornerShape)
                .background(Color.White.copy(alpha = 0.1f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.2f),
                    KitsugiTvTokens.Shapes.chip as RoundedCornerShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isFocused) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                KitsugiTvTokens.Shapes.posterCard as RoundedCornerShape
            )
            .tvClickable(shape = KitsugiTvTokens.Shapes.posterCard as RoundedCornerShape) { onCheckedChange(!checked) }
            .onFocusChanged { isFocused = it.isFocused }
            .padding(KitsugiTvTokens.Spacing.contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.width(KitsugiTvTokens.Spacing.contentPadding))

        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

