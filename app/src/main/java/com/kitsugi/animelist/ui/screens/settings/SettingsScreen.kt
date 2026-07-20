@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.kitsugi.animelist.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.BuildConfig
import com.kitsugi.animelist.ui.components.BackupImportMode
import com.kitsugi.animelist.ui.components.KitsugiChoiceOption
import com.kitsugi.animelist.ui.components.KitsugiConfirmDialog
import com.kitsugi.animelist.ui.components.KitsugiPage
import com.kitsugi.animelist.ui.components.KitsugiProfileEditDialog
import com.kitsugi.animelist.ui.components.KitsugiSettingsDivider
import com.kitsugi.animelist.ui.components.KitsugiSettingsItem
import com.kitsugi.animelist.ui.components.KitsugiSettingsSection
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalIsTv
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

import com.kitsugi.animelist.data.local.CloudstreamRepoEntity
import com.kitsugi.animelist.data.local.CsPluginEntity
import com.kitsugi.animelist.data.local.ManagedAddonEntity
import com.kitsugi.animelist.data.local.MangaSourceStateEntity
import com.kitsugi.animelist.data.remote.CsPlugin
import com.kitsugi.animelist.ui.components.KitsugiAddonsSettingsDialog
import com.kitsugi.animelist.ui.components.KitsugiPlayerSettingsDialog
import com.kitsugi.animelist.ui.components.KitsugiPreferencesSettingsDialog
import com.kitsugi.animelist.ui.components.KitsugiSystemSettingsDialog
import com.kitsugi.animelist.ui.components.KitsugiIntegrationsSettingsDialog

@Composable
fun SettingsScreen(
    params: SettingsScreenParameters
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val general = params.general
    val profile = params.profile
    val player = params.player
    val addon = params.addon
    val manga = params.manga
    val integrations = params.integrations

    var activeDialog by rememberSaveable {
        mutableStateOf<SettingsDialog?>(null)
    }

    var showDeleteAllConfirm by rememberSaveable {
        mutableStateOf(false)
    }

    val selectedTheme = themeOptions.firstOrNull { it.id == general.selectedThemeId }
        ?: themeOptions.first()

    val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current
    val scrollState = rememberScrollState()
    val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()

    CompositionLocalProvider(
        LocalBringIntoViewSpec provides if (isTvDevice) tvSpec else LocalBringIntoViewSpec.current
    ) {
        KitsugiPage(
            title = "Ayarlar",
            modifier = Modifier
                .then(if (isTvDevice) Modifier.dpadVerticalFastScroll(scrollState) else Modifier)
                .verticalScroll(scrollState)
        ) {
        Spacer(modifier = Modifier.height(22.dp))

        SettingsProfileSection(
            profileName = profile.profileName,
            onEditProfileClick = { activeDialog = SettingsDialog.Account },
            isAniListConnected = profile.isAniListConnected,
            anilistUsername = profile.anilistUsername,
            isAniListImportRunning = profile.isAniListImportRunning,
            onAniListImportClick = profile.onAniListImportClick,
            onAniListAuthClick = profile.onAniListAuthClick,
            isMalConnected = profile.isMalConnected,
            malUsername = profile.malUsername,
            isMalImportRunning = profile.isMalImportRunning,
            onMalImportClick = profile.onMalImportClick,
            onMalAuthClick = profile.onMalAuthClick,
            isSimklConnected = profile.isSimklConnected,
            simklUsername = profile.simklUsername,
            isSimklImportRunning = profile.isSimklImportRunning,
            isSimklSessionExpired = profile.isSimklSessionExpired,
            onSimklImportClick = profile.onSimklImportClick,
            onSimklAuthClick = profile.onSimklAuthClick,
            isCrossSyncRunning = profile.isCrossSyncRunning,
            onCrossSyncClick = profile.onCrossSyncClick
        )

        Spacer(modifier = Modifier.height(22.dp))

        KitsugiSettingsSection(
            title = "Uygulama Ayarları"
        ) {
            KitsugiSettingsItem(
                title = "Kütüphane İstatistikleri",
                description = "Kütüphanenizin detaylı durumu, puan dağılımları ve analizleri",
                icon = Icons.Rounded.PieChart,
                iconColor = KitsugiColors.AccentBlue,
                onClick = integrations.onOpenStats
            )

            KitsugiSettingsDivider()

            KitsugiSettingsItem(
                title = "Görünüm ve Tercihler",
                description = "Tema, liste görünümü, puanlama ve dil ayarları",
                icon = Icons.Rounded.Palette,
                iconColor = selectedTheme.color ?: KitsugiColors.Accent,
                onClick = { activeDialog = SettingsDialog.Preferences }
            )

            KitsugiSettingsDivider()

            KitsugiSettingsItem(
                title = "Eklenti & Akış Ayarları",
                description = "Torrent, video sağlayıcıları, debrid ve manga kaynakları",
                icon = Icons.Rounded.Extension,
                iconColor = KitsugiColors.AccentPurple,
                onClick = { activeDialog = SettingsDialog.Addons }
            )

            KitsugiSettingsDivider()

            KitsugiSettingsItem(
                title = "Oynatıcı Ayarları",
                description = "Gelişmiş video oynatıcı, altyazı, ses ve arabellek ayarları",
                icon = Icons.Rounded.PlayCircle,
                iconColor = KitsugiColors.AccentOrange,
                onClick = { activeDialog = SettingsDialog.PlayerSettings }
            )

            KitsugiSettingsDivider()

            KitsugiSettingsItem(
                title = "Harici Entegrasyonlar",
                description = "TMDB, MDBList ve AniSkip (Intro/Outro) ayarları",
                icon = Icons.Rounded.Hub,
                iconColor = KitsugiColors.AccentBlue,
                onClick = { activeDialog = SettingsDialog.Integrations }
            )

            KitsugiSettingsDivider()

            KitsugiSettingsItem(
                title = "Sistem & Veri Ayarları",
                description = "Veri yönetimi, yedekleme, DoH (DNS) ve hakkında bilgileri",
                icon = Icons.Rounded.Storage,
                iconColor = KitsugiColors.AccentGreen,
                onClick = { activeDialog = SettingsDialog.SystemSettings }
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        KitsugiSettingsSection(
            title = "Destek & Hakkında"
        ) {
            KitsugiSettingsItem(
                title = "Favorilerim",
                description = "Favorilerinize eklediğiniz anime ve mangaları görüntüleyin",
                icon = Icons.Rounded.Favorite,
                iconColor = KitsugiColors.AccentPink,
                onClick = integrations.onOpenFavourites
            )

            KitsugiSettingsDivider()

            KitsugiSettingsItem(
                title = "Geri Bildirim Gönder",
                description = "Uygulamayla ilgili hata bildirin veya önerilerinizi paylaşın",
                icon = Icons.Rounded.Feedback,
                iconColor = KitsugiColors.AccentBlue,
                onClick = { activeDialog = SettingsDialog.Feedback }
            )

            KitsugiSettingsDivider()

            KitsugiSettingsItem(
                title = "Hakkında",
                description = "Sürüm bilgileri, açık kaynak kütüphaneler ve katkıda bulunanlar",
                icon = Icons.Rounded.Info,
                iconColor = KitsugiColors.AccentPurple,
                onClick = integrations.onOpenAbout
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
    }

    when (activeDialog) {
        SettingsDialog.Account -> {
            KitsugiProfileEditDialog(
                initialProfileName = profile.profileName,
                initialListTitle = profile.listTitle,
                hasProfileImage = profile.profileImageUri.isNotBlank(),
                hasBannerImage = profile.bannerImageUri.isNotBlank(),
                onPickProfileImageClick = profile.onPickProfileImageClick,
                onPickBannerImageClick = profile.onPickBannerImageClick,
                onClearProfileImageClick = profile.onClearProfileImageClick,
                onClearBannerImageClick = profile.onClearBannerImageClick,
                onSave = { newProfileName, newListTitle ->
                    profile.onProfileInfoSave(
                        newProfileName,
                        newListTitle
                    )
                    activeDialog = null
                },
                onDismiss = {
                    activeDialog = null
                }
            )
        }

        SettingsDialog.Preferences -> {
            SettingsPreferencesDialogWrapper(
                general = params.general,
                integrations = params.integrations,
                onDismiss = { activeDialog = null }
            )
        }

        SettingsDialog.Addons -> {
            SettingsAddonsDialogWrapper(
                addon = params.addon,
                manga = params.manga,
                onDismiss = { activeDialog = null }
            )
        }

        SettingsDialog.PlayerSettings -> {
            SettingsPlayerSettingsDialogWrapper(
                player = params.player,
                onDismiss = { activeDialog = null }
            )
        }

        SettingsDialog.SystemSettings -> {
            KitsugiSystemSettingsDialog(
                totalEntryCount = params.profile.totalEntryCount,
                onExportFileClick = {
                    activeDialog = null
                    params.profile.onExportBackupFileClick()
                },
                onImportFileClick = {
                    activeDialog = null
                    params.profile.onImportBackupFileClick()
                },
                onDeleteAllClick = {
                    activeDialog = null
                    showDeleteAllConfirm = true
                },
                dnsChoice = params.integrations.dnsChoice,
                onDnsChoiceSelected = params.integrations.onDnsChoiceSelected,
                onDeveloperLogsClick = {
                    activeDialog = SettingsDialog.DeveloperLogs
                },
                onDismiss = {
                    activeDialog = null
                },
                autoUpdateCheckEnabled = params.general.autoUpdateCheckEnabled,
                onAutoUpdateCheckEnabledChanged = params.general.onAutoUpdateCheckEnabledChanged,
                onCheckForUpdatesClick = params.general.onCheckForUpdatesClick
            )
        }

        SettingsDialog.DeveloperLogs -> {
            DeveloperLogsDialog(
                onDismiss = {
                    activeDialog = SettingsDialog.SystemSettings
                }
            )
        }

        SettingsDialog.Integrations -> {
            SettingsIntegrationsDialogWrapper(
                integrations = params.integrations,
                onDismiss = { activeDialog = null }
            )
        }

        SettingsDialog.Feedback -> {
            com.kitsugi.animelist.ui.screens.more.FeedbackDialog(
                onDismiss = { activeDialog = null },
                onSubmit = { title, type, description ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:")
                        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("feedback@kitsugi.app"))
                        val subject = "[Kitsugi Beta Feedback] [$type] $title"
                        val body = """
                            Tür: $type
                            Konu: $title
                            
                            Açıklama:
                            $description
                            
                            -- Cihaz Bilgisi --
                            Uygulama Sürümü: ${com.kitsugi.animelist.BuildConfig.VERSION_NAME} (${com.kitsugi.animelist.BuildConfig.VERSION_CODE})
                            Cihaz: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                            Android Sürümü: API ${android.os.Build.VERSION.SDK_INT}
                        """.trimIndent()
                        putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
                        putExtra(android.content.Intent.EXTRA_TEXT, body)
                    }
                    runCatching {
                        context.startActivity(intent)
                    }.onFailure {
                        android.widget.Toast.makeText(
                            context,
                            "E-posta uygulaması bulunamadı. Lütfen feedback@kitsugi.app adresine yazın.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }

        null -> Unit
    }

    if (showDeleteAllConfirm) {
        KitsugiConfirmDialog(
            title = "Tüm liste silinsin mi?",
            message = "Room veritabanındaki tüm anime/manga kayıtları kalıcı olarak silinecek. Tema ve uygulama ayarları korunur.",
            confirmText = "Tümünü sil",
            isDestructive = true,
            onConfirm = {
                profile.onDeleteAllEntries()
                showDeleteAllConfirm = false
                activeDialog = SettingsDialog.SystemSettings
            },
            onDismiss = {
                showDeleteAllConfirm = false
                activeDialog = SettingsDialog.SystemSettings
            }
        )
    }
}

private enum class SettingsDialog {
    Account,
    Preferences,
    Addons,
    PlayerSettings,
    SystemSettings,
    DeveloperLogs,
    Integrations,
    Feedback
}

private val themeOptions = listOf(
    KitsugiChoiceOption(
        id = "mint",
        title = "Mint",
        description = "Mevcut Kitsugi açık mint teması",
        color = Color(0xFFC8F4EF)
    ),
    KitsugiChoiceOption(
        id = "pink",
        title = "Pembe",
        description = "Canlı pembe vurgu rengi",
        color = KitsugiColors.AccentPink
    ),
    KitsugiChoiceOption(
        id = "purple",
        title = "Mor",
        description = "Neon ve modern mor görünüm",
        color = KitsugiColors.AccentPurple
    ),
    KitsugiChoiceOption(
        id = "blue",
        title = "Mavi",
        description = "Sade ve temiz mavi vurgu",
        color = KitsugiColors.AccentBlue
    ),
    KitsugiChoiceOption(
        id = "green",
        title = "Yeşil",
        description = "Doğal ve dengeli yeşil vurgu",
        color = KitsugiColors.AccentGreen
    ),
    KitsugiChoiceOption(
        id = "red",
        title = "Kırmızı",
        description = "Tutkulu ve enerjik kırmızı vurgu",
        color = KitsugiColors.AccentRed
    ),
    KitsugiChoiceOption(
        id = "orange",
        title = "Turuncu",
        description = "Dinamik ve sıcak turuncu vurgu",
        color = KitsugiColors.AccentOrange
    ),
    KitsugiChoiceOption(
        id = "yellow",
        title = "Sarı",
        description = "Parlak ve neşeli sarı vurgu",
        color = KitsugiColors.AccentYellow
    ),
    KitsugiChoiceOption(
        id = "teal",
        title = "Turkuaz",
        description = "Ferah ve sakin turkuaz vurgu",
        color = KitsugiColors.AccentTeal
    ),
    KitsugiChoiceOption(
        id = "indigo",
        title = "İndigo",
        description = "Zengin ve derin indigo vurgu",
        color = KitsugiColors.AccentIndigo
    )
)

@Composable
private fun SettingsPreferencesDialogWrapper(
    general: com.kitsugi.animelist.ui.screens.settings.GeneralSettings,
    integrations: com.kitsugi.animelist.ui.screens.settings.IntegrationsSettings,
    onDismiss: () -> Unit
) {
    val localSettings = remember(
        general.selectedThemeId, general.themeMode, general.amoledBlack, general.customAccentColor, general.defaultTab,
        general.showAdultContent, general.showAnimeLogos, general.selectedListLayoutId, general.selectedHomeLayoutId,
        general.titleLanguage, general.scoreFormat, general.hideScores, integrations.autoTranslateEnabled, general.appLanguage, general.fixedNavBar,
        general.airingNotificationsEnabled, general.splashAnimationEnabled, general.splashSoundEnabled,
        general.mangaReadingMode, general.mangaColorFilter, general.mangaFitMode, general.mangaBrightness
    ) {
        com.kitsugi.animelist.data.settings.AppSettings(
            selectedThemeId = general.selectedThemeId,
            themeMode = general.themeMode,
            amoledBlack = general.amoledBlack,
            customAccentColor = general.customAccentColor,
            defaultTab = general.defaultTab,
            showAdultContent = general.showAdultContent,
            showAnimeLogos = general.showAnimeLogos,
            selectedListLayoutId = general.selectedListLayoutId,
            selectedHomeLayoutId = general.selectedHomeLayoutId,
            titleLanguage = general.titleLanguage,
            scoreFormat = general.scoreFormat,
            hideScores = general.hideScores,
            autoTranslateEnabled = integrations.autoTranslateEnabled,
            appLanguage = general.appLanguage,
            fixedNavBar = general.fixedNavBar,
            airingNotificationsEnabled = general.airingNotificationsEnabled,
            splashAnimationEnabled = general.splashAnimationEnabled,
            splashSoundEnabled = general.splashSoundEnabled,
            searchHistoryEnabled = general.searchHistoryEnabled,
            mangaReadingMode = general.mangaReadingMode,
            mangaColorFilter = general.mangaColorFilter,
            mangaFitMode = general.mangaFitMode,
            mangaBrightness = general.mangaBrightness
        )
    }
    KitsugiPreferencesSettingsDialog(
        appSettings = localSettings,
        onThemeSelected = general.onThemeSelected,
        onThemeModeSelected = general.onThemeModeSelected,
        onAmoledBlackChanged = general.onAmoledBlackChanged,
        onCustomAccentColorChanged = general.onCustomAccentColorChanged,
        onDefaultTabSelected = general.onDefaultTabSelected,
        onAdultContentChanged = general.onAdultContentChanged,
        onShowAnimeLogosChanged = general.onShowAnimeLogosChanged,
        onListLayoutSelected = general.onListLayoutSelected,
        onTitleLanguageSelected = general.onTitleLanguageSelected,
        onScoreFormatSelected = general.onScoreFormatSelected,
        onHideScoresChanged = general.onHideScoresChanged,
        onHomeLayoutSelected = general.onHomeLayoutSelected,
        onAutoTranslateEnabledChanged = integrations.onAutoTranslateEnabledChanged,
        onAppLanguageSelected = general.onAppLanguageSelected,
        onFixedNavBarChanged = general.onFixedNavBarChanged,
        onAiringNotificationsChanged = general.onAiringNotificationsChanged,
        onSplashAnimationEnabledChanged = general.onSplashAnimationEnabledChanged,
        onSplashSoundEnabledChanged = general.onSplashSoundEnabledChanged,
        onSearchHistoryEnabledChanged = general.onSearchHistoryEnabledChanged,
        // ─── T1-07 – Manga Okuyucu Varsayılan Ayarları ───────────────────────
        onMangaReadingModeSelected = general.onMangaReadingModeSelected,
        onMangaColorFilterSelected = general.onMangaColorFilterSelected,
        onMangaFitModeSelected = general.onMangaFitModeSelected,
        onMangaBrightnessChanged = general.onMangaBrightnessChanged,
        onDismiss = onDismiss
    )
}

@Composable
private fun SettingsPlayerSettingsDialogWrapper(
    player: PlayerSettings,
    onDismiss: () -> Unit
) {
    KitsugiPlayerSettingsDialog(
        playerPreference = player.playerPreference,
        preferredExternalPlayerPackage = player.preferredExternalPlayerPackage,
        isAutoplayEnabled = player.isAutoplayEnabled,
        skipIntroDurationSec = player.skipIntroDurationSec,
        defaultSubtitleSize = player.defaultSubtitleSize,
        defaultSubtitleColor = player.defaultSubtitleColor,
        subtitleBold = player.subtitleBold,
        subtitleOutlineEnabled = player.subtitleOutlineEnabled,
        defaultAudioBoost = player.defaultAudioBoost,
        defaultAudioDelayMs = player.defaultAudioDelayMs,
        minBufferMs = player.minBufferMs,
        maxBufferMs = player.maxBufferMs,
        bufferForPlaybackMs = player.bufferForPlaybackMs,
        bufferForPlaybackAfterRebufferMs = player.bufferForPlaybackAfterRebufferMs,
        backBufferDurationMs = player.backBufferDurationMs,
        dv7HandlingMode = player.dv7HandlingMode,
        stripHdr10PlusSei = player.stripHdr10PlusSei,
        preferredSubtitleLanguages = player.preferredSubtitleLanguages,
        addonSubtitleStartupMode = player.addonSubtitleStartupMode,
        qualityProfileJson = player.qualityProfileJson,
        parallelRangeEnabled = player.parallelRangeEnabled,
        frameRateMatchingMode = player.frameRateMatchingMode,
        resolutionMatchingEnabled = player.resolutionMatchingEnabled,
        gestureVolumeEnabled = player.gestureVolumeEnabled,
        gestureBrightnessEnabled = player.gestureBrightnessEnabled,
        gestureZoomEnabled = player.gestureZoomEnabled,
        doubleTapSeekSeconds = player.doubleTapSeekSeconds,
        holdSpeedMultiplier = player.holdSpeedMultiplier,
        gestureScrollSensitivity = player.gestureScrollSensitivity,
        onPlayerPreferenceSelected = player.onPlayerPreferenceSelected,
        onPreferredExternalPlayerPackageSelected = player.onPreferredExternalPlayerPackageSelected,
        onAutoplayEnabledChanged = player.onAutoplayEnabledChanged,
        onSkipIntroDurationSecSelected = player.onSkipIntroDurationSecSelected,
        onDefaultSubtitleSizeSelected = player.onDefaultSubtitleSizeSelected,
        onDefaultSubtitleColorSelected = player.onDefaultSubtitleColorSelected,
        onSubtitleBoldChanged = player.onSubtitleBoldChanged,
        onSubtitleOutlineEnabledChanged = player.onSubtitleOutlineEnabledChanged,
        onDefaultAudioBoostSelected = player.onDefaultAudioBoostSelected,
        onDefaultAudioDelayMsSelected = player.onDefaultAudioDelayMsSelected,
        onPreferredSubtitleLanguagesSelected = player.onPreferredSubtitleLanguagesSelected,
        onAddonSubtitleStartupModeSelected = player.onAddonSubtitleStartupModeSelected,
        onBufferSettingsChanged = player.onBufferSettingsChanged,
        onDv7HandlingModeSelected = player.onDv7HandlingModeSelected,
        onStripHdr10PlusSeiChanged = player.onStripHdr10PlusSeiChanged,
        onQualityProfileSelected = player.onQualityProfileSelected,
        onParallelRangeEnabledChanged = player.onParallelRangeEnabledChanged,
        onFrameRateMatchingModeSelected = player.onFrameRateMatchingModeSelected,
        onResolutionMatchingEnabledChanged = player.onResolutionMatchingEnabledChanged,
        onGestureVolumeEnabledChanged = player.onGestureVolumeEnabledChanged,
        onGestureBrightnessEnabledChanged = player.onGestureBrightnessEnabledChanged,
        onGestureZoomEnabledChanged = player.onGestureZoomEnabledChanged,
        onDoubleTapSeekSecondsSelected = player.onDoubleTapSeekSecondsSelected,
        onHoldSpeedMultiplierSelected = player.onHoldSpeedMultiplierSelected,
        onGestureScrollSensitivityChanged = player.onGestureScrollSensitivityChanged,
        previewSeekbarEnabled = player.previewSeekbarEnabled,
        onPreviewSeekbarEnabledChanged = player.onPreviewSeekbarEnabledChanged,
        aspectMode = player.aspectMode,
        onAspectModeSelected = player.onAspectModeSelected,
        liveHelperEnabled = player.liveHelperEnabled,
        onLiveHelperEnabledChanged = player.onLiveHelperEnabledChanged,
        enableAssExtractor = player.enableAssExtractor,
        onEnableAssExtractorChanged = player.onEnableAssExtractorChanged,
        showPlayerTitle = player.showPlayerTitle,
        onShowPlayerTitleChanged = player.onShowPlayerTitleChanged,
        showPlayerResolution = player.showPlayerResolution,
        onShowPlayerResolutionChanged = player.onShowPlayerResolutionChanged,
        showMediaInfo = player.showMediaInfo,
        onShowMediaInfoChanged = player.onShowMediaInfoChanged,
        stillWatchingEnabled = player.stillWatchingEnabled,
        onStillWatchingEnabledChanged = player.onStillWatchingEnabledChanged,
        stillWatchingThresholdMinutes = player.stillWatchingThresholdMinutes,
        onStillWatchingThresholdMinutesChanged = player.onStillWatchingThresholdMinutesChanged,
        postPlayMode = player.postPlayMode,
        onPostPlayModeChanged = player.onPostPlayModeChanged,
        autoplaySessionLimit = player.autoplaySessionLimit,
        onAutoplaySessionLimitChanged = player.onAutoplaySessionLimitChanged,
        gainBoostDb = player.gainBoostDb,
        onGainBoostDbChanged = player.onGainBoostDbChanged,
        subtitleDelayMs = player.subtitleDelayMs,
        onSubtitleDelayMsChanged = player.onSubtitleDelayMsChanged,
        decoderPriority = player.decoderPriority,
        onDecoderPriorityChanged = player.onDecoderPriorityChanged,
        onDismiss = onDismiss
    )
}

@Composable
private fun SettingsIntegrationsDialogWrapper(
    integrations: IntegrationsSettings,
    onDismiss: () -> Unit
) {
    KitsugiIntegrationsSettingsDialog(
        tmdbEnabled = integrations.tmdbEnabled,
        onTmdbEnabledChanged = integrations.onTmdbEnabledChanged,
        tmdbApiKey = integrations.tmdbApiKey,
        onTmdbApiKeyChanged = integrations.onTmdbApiKeyChanged,
        tmdbModernHomeEnabled = integrations.tmdbModernHomeEnabled,
        onTmdbModernHomeEnabledChanged = integrations.onTmdbModernHomeEnabledChanged,
        tmdbEnrichContinueWatching = integrations.tmdbEnrichContinueWatching,
        onTmdbEnrichContinueWatchingChanged = integrations.onTmdbEnrichContinueWatchingChanged,
        tmdbLanguage = integrations.tmdbLanguage,
        onTmdbLanguageChanged = integrations.onTmdbLanguageChanged,
        tmdbUseArtwork = integrations.tmdbUseArtwork,
        onTmdbUseArtworkChanged = integrations.onTmdbUseArtworkChanged,
        tmdbUseBasicInfo = integrations.tmdbUseBasicInfo,
        onTmdbUseBasicInfoChanged = integrations.onTmdbUseBasicInfoChanged,
        tmdbUseDetails = integrations.tmdbUseDetails,
        onTmdbUseDetailsChanged = integrations.onTmdbUseDetailsChanged,
        tmdbUseReleaseDates = integrations.tmdbUseReleaseDates,
        onTmdbUseReleaseDatesChanged = integrations.onTmdbUseReleaseDatesChanged,
        tmdbUseCredits = integrations.tmdbUseCredits,
        onTmdbUseCreditsChanged = integrations.onTmdbUseCreditsChanged,
        tmdbUseProductions = integrations.tmdbUseProductions,
        onTmdbUseProductionsChanged = integrations.onTmdbUseProductionsChanged,
        tmdbUseNetworks = integrations.tmdbUseNetworks,
        onTmdbUseNetworksChanged = integrations.onTmdbUseNetworksChanged,
        tmdbUseEpisodes = integrations.tmdbUseEpisodes,
        onTmdbUseEpisodesChanged = integrations.onTmdbUseEpisodesChanged,
        tmdbUseTrailers = integrations.tmdbUseTrailers,
        onTmdbUseTrailersChanged = integrations.onTmdbUseTrailersChanged,
        tmdbUseMoreLikeThis = integrations.tmdbUseMoreLikeThis,
        onTmdbUseMoreLikeThisChanged = integrations.onTmdbUseMoreLikeThisChanged,
        tmdbUseCollections = integrations.tmdbUseCollections,
        onTmdbUseCollectionsChanged = integrations.onTmdbUseCollectionsChanged,
        mdbListEnabled = integrations.mdbListEnabled,
        onMdbListEnabledChanged = integrations.onMdbListEnabledChanged,
        mdbListApiKey = integrations.mdbListApiKey,
        onMdbListApiKeyChanged = integrations.onMdbListApiKeyChanged,
        mdbListShowImdb = integrations.mdbListShowImdb,
        onMdbListShowImdbChanged = integrations.onMdbListShowImdbChanged,
        mdbListShowTomatoes = integrations.mdbListShowTomatoes,
        onMdbListShowTomatoesChanged = integrations.onMdbListShowTomatoesChanged,
        mdbListShowMetacritic = integrations.mdbListShowMetacritic,
        onMdbListShowMetacriticChanged = integrations.onMdbListShowMetacriticChanged,
        mdbListShowAudience = integrations.mdbListShowAudience,
        onMdbListShowAudienceChanged = integrations.onMdbListShowAudienceChanged,
        mdbListShowLetterboxd = integrations.mdbListShowLetterboxd,
        onMdbListShowLetterboxdChanged = integrations.onMdbListShowLetterboxdChanged,
        mdbListShowTmdb = integrations.mdbListShowTmdb,
        onMdbListShowTmdbChanged = integrations.onMdbListShowTmdbChanged,
        mdbListShowTrakt = integrations.mdbListShowTrakt,
        onMdbListShowTraktChanged = integrations.onMdbListShowTraktChanged,
        aniSkipEnabled = integrations.aniSkipEnabled,
        onAniSkipEnabledChanged = integrations.onAniSkipEnabledChanged,
        aniSkipAutoSkip = integrations.aniSkipAutoSkip,
        onAniSkipAutoSkipChanged = integrations.onAniSkipAutoSkipChanged,
        animeSkipClientId = integrations.animeSkipClientId,
        onAnimeSkipClientIdChanged = integrations.onAnimeSkipClientIdChanged,
        onDismiss = onDismiss
    )
}

@Composable
private fun SettingsAddonsDialogWrapper(
    addon: AddonSettings,
    manga: MangaSettings,
    onDismiss: () -> Unit
) {
    KitsugiAddonsSettingsDialog(
        addons = addon.addons,
        initialDebridToken = addon.debridToken,
        repos = addon.repos,
        repoPlugins = addon.repoPlugins,
        repoLoadingState = addon.repoLoadingState,
        csPlugins = addon.csPlugins,
        onAddAddon = addon.onAddAddon,
        onToggleAddon = addon.onToggleAddon,
        onDeleteAddon = addon.onDeleteAddon,
        onSaveDebridToken = addon.onSaveDebridToken,
        onAddRepo = addon.onAddRepo,
        onDeleteRepo = addon.onDeleteRepo,
        onFetchRepoPlugins = addon.onFetchRepoPlugins,
        onInstallPlugin = addon.onInstallPlugin,
        onInstallAllPlugins = addon.onInstallAllPlugins,
        onUpdateAllPlugins = addon.onUpdateAllPlugins,
        bulkInstallRepoUrl = addon.bulkInstallRepoUrl,
        bulkInstallRepoName = addon.bulkInstallRepoName,
        bulkInstallDone = addon.bulkInstallDone,
        bulkInstallTotal = addon.bulkInstallTotal,
        bulkInstallCurrentName = addon.bulkInstallCurrentName,
        bulkInstallResultMessage = addon.bulkInstallResultMessage,
        onClearBulkInstallResult = addon.onClearBulkInstallResult,
        onToggleCsPlugin = addon.onToggleCsPlugin,
        onUninstallCsPlugin = addon.onUninstallCsPlugin,
        mangaSources = manga.mangaSources,
        onInstallMangaExtension = manga.onInstallMangaExtension,
        onDeleteMangaExtension = manga.onDeleteMangaExtension,
        mangaRepos = manga.mangaRepos,
        mangaRepoExtensions = manga.mangaRepoExtensions,
        mangaRepoLoadingState = manga.mangaRepoLoadingState,
        onAddMangaRepo = manga.onAddMangaRepo,
        onDeleteMangaRepo = manga.onDeleteMangaRepo,
        onFetchMangaRepo = manga.onFetchMangaRepo,
        onInstallMangaApk = manga.onInstallMangaApk,
        onInstallAllMangaExtensions = manga.onInstallAllMangaExtensions,
        onUpdateAllMangaExtensions = manga.onUpdateAllMangaExtensions,
        mangaBulkInstallRepoUrl = manga.mangaBulkInstallRepoUrl,
        mangaBulkInstallDone = manga.mangaBulkInstallDone,
        mangaBulkInstallTotal = manga.mangaBulkInstallTotal,
        mangaBulkInstallCurrentName = manga.mangaBulkInstallCurrentName,
        onGetInstalledMangaVersionCode = manga.onGetInstalledMangaVersionCode,
        onGetInstalledMangaVersion = manga.onGetInstalledMangaVersion,
        mangaSourceStateReport = manga.mangaSourceStateReport,
        onGetMangaSourceHealthStatus = manga.onGetMangaSourceHealthStatus,
        onGetMangaSourceRuntimeStats = manga.onGetMangaSourceRuntimeStats,
        onGetMangaConfiguredDomain = manga.onGetMangaConfiguredDomain,
        onGetMangaConfiguredBaseUrl = manga.onGetMangaConfiguredBaseUrl,
        onGetMangaSourceUserAgent = manga.onGetMangaSourceUserAgent,
        onGetMangaSourceSlowdownEnabled = manga.onGetMangaSourceSlowdownEnabled,
        onSetMangaSourceUserAgent = manga.onSetMangaSourceUserAgent,
        onSetMangaSourceSlowdownEnabled = manga.onSetMangaSourceSlowdownEnabled,
        onSetMangaSourceDomain = manga.onSetMangaSourceDomain,
        onResetMangaSourceDiagnostics = manga.onResetMangaSourceDiagnostics,
        onClearAllMangaSourceDiagnostics = manga.onClearAllMangaSourceDiagnostics,
        onIsMangaSourceBusy = manga.onIsMangaSourceBusy,
        onQuickCheckMangaSource = manga.onQuickCheckMangaSource,
        onRefreshMangaSourceMirror = manga.onRefreshMangaSourceMirror,
        onClearMangaSourceMirror = manga.onClearMangaSourceMirror,
        onOpenMangaSourceHealthScreen = manga.onOpenMangaSourceHealthScreen,
        onForceCheckMangaUpdates = manga.onForceCheckMangaUpdates,
        untrustedRepoToConfirm = manga.untrustedRepoToConfirm,
        untrustedSignatureToConfirm = manga.untrustedSignatureToConfirm,
        onConfirmUntrustedRepo = manga.onConfirmUntrustedRepo,
        onDismissUntrustedRepo = manga.onDismissUntrustedRepo,
        onConfirmUntrustedSignature = manga.onConfirmUntrustedSignature,
        onDismissUntrustedSignature = manga.onDismissUntrustedSignature,
        onDismiss = onDismiss
    )
}