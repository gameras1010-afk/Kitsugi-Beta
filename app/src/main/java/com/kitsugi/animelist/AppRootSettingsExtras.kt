package com.kitsugi.animelist

import androidx.compose.runtime.Composable
import com.kitsugi.animelist.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch

/**
 * Holds the full SettingsScreen(...) call. Split from SettingsScreenContent to keep
 * each JVM method under the 64 KB bytecode limit.
 */
@Composable
internal fun SettingsScreenExtras(ctx: SettingsContext) {
    SettingsScreen(params = ctx.buildSettingsParams())
}

// ─── Non-Composable Parameter Builder ─────────────────────────────────────────────

internal fun SettingsContext.buildSettingsParams() =
    com.kitsugi.animelist.ui.screens.settings.SettingsScreenParameters(
        general = com.kitsugi.animelist.ui.screens.settings.GeneralSettings(
            selectedThemeId = appSettings.selectedThemeId,
            showAdultContent = appSettings.showAdultContent,
            selectedListLayoutId = appSettings.selectedListLayoutId,
            selectedHomeLayoutId = appSettings.selectedHomeLayoutId,
            onHomeLayoutSelected = { onHomeLayoutSelected(it) },
            titleLanguage = appSettings.titleLanguage,
            scoreFormat = appSettings.scoreFormat,
            hideScores = appSettings.hideScores,
            showAnimeLogos = appSettings.showAnimeLogos,
            onThemeSelected = { onThemeSelected(it) },
            onAdultContentChanged = { onAdultContentChanged(it) },
            onListLayoutSelected = { onListLayoutSelected(it) },
            onTitleLanguageSelected = { onTitleLanguageSelected(it) },
            onScoreFormatSelected = { onScoreFormatSelected(it) },
            onHideScoresChanged = { onHideScoresChanged(it) },
            onShowAnimeLogosChanged = { onShowAnimeLogosChanged(it) },
            themeMode = appSettings.themeMode,
            onThemeModeSelected = { onThemeModeSelected(it) },
            amoledBlack = appSettings.amoledBlack,
            onAmoledBlackChanged = { onAmoledBlackChanged(it) },
            customAccentColor = appSettings.customAccentColor,
            onCustomAccentColorChanged = { onCustomAccentColorChanged(it) },
            defaultTab = appSettings.defaultTab,
            onDefaultTabSelected = { onDefaultTabSelected(it) },
            appLanguage = appSettings.appLanguage,
            onAppLanguageSelected = { onAppLanguageSelected(it) },
            fixedNavBar = appSettings.fixedNavBar,
            onFixedNavBarChanged = { onFixedNavBarChanged(it) },
            airingNotificationsEnabled = appSettings.airingNotificationsEnabled,
            onAiringNotificationsChanged = { onAiringNotificationsChanged(it) },
            splashAnimationEnabled = appSettings.splashAnimationEnabled,
            splashSoundEnabled = appSettings.splashSoundEnabled,
            onSplashAnimationEnabledChanged = { onSplashAnimationEnabledChanged(it) },
            onSplashSoundEnabledChanged = { onSplashSoundEnabledChanged(it) },
            searchHistoryEnabled = appSettings.searchHistoryEnabled,
            onSearchHistoryEnabledChanged = { onSearchHistoryEnabledChanged(it) },
            // ─── T1-07 – Manga Okuyucu Varsayılan Ayarları ───────────────────────
            mangaReadingMode = appSettings.mangaReadingMode,
            mangaColorFilter = appSettings.mangaColorFilter,
            mangaFitMode = appSettings.mangaFitMode,
            mangaBrightness = appSettings.mangaBrightness,
            onMangaReadingModeSelected = { onMangaReadingModeSelected(it) },
            onMangaColorFilterSelected = { onMangaColorFilterSelected(it) },
            onMangaFitModeSelected = { onMangaFitModeSelected(it) },
            onMangaBrightnessChanged = { onMangaBrightnessChanged(it) },
            autoUpdateCheckEnabled = appSettings.autoUpdateCheckEnabled,
            onAutoUpdateCheckEnabledChanged = { onAutoUpdateCheckEnabledChanged(it) },
            onCheckForUpdatesClick = { onCheckForUpdatesClick() },
            customImageDownloadUri = appSettings.customImageDownloadUri,
            onCustomImageDownloadUriChanged = { onCustomImageDownloadUriChanged(it) }
        ),
        profile = com.kitsugi.animelist.ui.screens.settings.ProfileSettings(
            profileName = appSettings.profileName,
            listTitle = appSettings.listTitle,
            anilistUsername = appSettings.anilistUsername,
            malUsername = appSettings.malUsername,
            profileImageUri = appSettings.profileImageUri,
            bannerImageUri = appSettings.bannerImageUri,
            totalEntryCount = mediaEntries.size,
            backupText = backupText,
            importText = importText,
            importMode = importMode,
            isAniListConnected = authViewModel.isAniListConnected,
            isMalConnected = authViewModel.isMalConnected,
            isAniListImportRunning = authViewModel.isAniListImportRunning,
            isMalImportRunning = authViewModel.isMalImportRunning,
            isSimklConnected = authViewModel.isSimklConnected,
            simklUsername = appSettings.simklUsername,
            isSimklImportRunning = authViewModel.isSimklImportRunning,
            isSimklSessionExpired = authViewModel.isSimklSessionExpired,
            onProfileInfoSave = { name, title -> onProfileInfoSave(name, title) },
            onPickProfileImageClick = onPickProfileImageClick,
            onPickBannerImageClick = onPickBannerImageClick,
            onClearProfileImageClick = { onClearProfileImageClick() },
            onClearBannerImageClick = { onClearBannerImageClick() },
            onAniListAuthClick = { onAniListAuthClick() },
            onMalAuthClick = { onMalAuthClick() },
            onAniListImportClick = { onAniListImportClick() },
            onMalImportClick = { onMalImportClick() },
            onSimklAuthClick = { onSimklAuthClick() },
            onSimklImportClick = { onSimklImportClick() },
            onDeleteAllEntries = { onDeleteAllEntries() },
            onCreateBackupText = { onCreateBackupText() },
            onImportBackupText = { onImportBackupText() },
            onClearBackupText = { appViewModel.clearBackupText() },
            onClearImportText = { appViewModel.clearImportText() },
            onExportBackupFileClick = onExportBackupFileClick,
            onImportBackupFileClick = onImportBackupFileClick,
            isCrossSyncRunning = authViewModel.isCrossSyncRunning,
            onCrossSyncClick = { onCrossSyncClick() },
            onImportModeChange = { appViewModel.updateImportMode(it) },
            onImportTextChange = { appViewModel.updateImportText(it) }
        ),
        player = com.kitsugi.animelist.ui.screens.settings.PlayerSettings(
            playerPreference = appSettings.playerPreference,
            isAutoplayEnabled = appSettings.isAutoplayEnabled,
            skipIntroDurationSec = appSettings.skipIntroDurationSec,
            defaultSubtitleSize = appSettings.defaultSubtitleSize,
            defaultSubtitleColor = appSettings.defaultSubtitleColor,
            subtitleBold = appSettings.subtitleBold,
            subtitleOutlineEnabled = appSettings.subtitleOutlineEnabled,
            defaultAudioBoost = appSettings.defaultAudioBoost,
            defaultAudioDelayMs = appSettings.defaultAudioDelayMs,
            minBufferMs = appSettings.minBufferMs,
            maxBufferMs = appSettings.maxBufferMs,
            bufferForPlaybackMs = appSettings.bufferForPlaybackMs,
            bufferForPlaybackAfterRebufferMs = appSettings.bufferForPlaybackAfterRebufferMs,
            backBufferDurationMs = appSettings.backBufferDurationMs,
            dv7HandlingMode = appSettings.dv7HandlingMode,
            stripHdr10PlusSei = appSettings.stripHdr10PlusSei,
            preferredSubtitleLanguages = appSettings.preferredSubtitleLanguages,
            addonSubtitleStartupMode = appSettings.addonSubtitleStartupMode,
            onPlayerPreferenceSelected = { playerSettingsViewModel.updatePlayerPreference(it) },
            onAutoplayEnabledChanged = { playerSettingsViewModel.updateAutoplayEnabled(it) },
            onSkipIntroDurationSecSelected = { playerSettingsViewModel.updateSkipIntroDurationSec(it) },
            onDefaultSubtitleSizeSelected = { playerSettingsViewModel.updateDefaultSubtitleSize(it) },
            onDefaultSubtitleColorSelected = { playerSettingsViewModel.updateDefaultSubtitleColor(it) },
            onSubtitleBoldChanged = { playerSettingsViewModel.updateSubtitleBold(it) },
            onSubtitleOutlineEnabledChanged = { playerSettingsViewModel.updateSubtitleOutline(it) },
            onDefaultAudioBoostSelected = { playerSettingsViewModel.updateDefaultAudioBoost(it) },
            onDefaultAudioDelayMsSelected = { playerSettingsViewModel.updateDefaultAudioDelayMs(it) },
            onPreferredSubtitleLanguagesSelected = { playerSettingsViewModel.updatePreferredSubtitleLanguages(it) },
            onAddonSubtitleStartupModeSelected = { playerSettingsViewModel.updateAddonSubtitleStartupMode(it) },
            onBufferSettingsChanged = { min, max, playback, rebuffer, back -> playerSettingsViewModel.updateBufferSettings(min, max, playback, rebuffer, back) },
            onDv7HandlingModeSelected = { playerSettingsViewModel.updateDv7HandlingMode(it) },
            onStripHdr10PlusSeiChanged = { playerSettingsViewModel.updateStripHdr10PlusSei(it) },
            qualityProfileJson = appSettings.qualityProfileJson,
            onQualityProfileSelected = { playerSettingsViewModel.updateQualityProfileJson(it) },
            parallelRangeEnabled = appSettings.parallelRangeEnabled,
            onParallelRangeEnabledChanged = { playerSettingsViewModel.updateParallelRangeEnabled(it) },
            frameRateMatchingMode = appSettings.frameRateMatchingMode,
            resolutionMatchingEnabled = appSettings.resolutionMatchingEnabled,
            onFrameRateMatchingModeSelected = { playerSettingsViewModel.updateFrameRateMatchingMode(it) },
            onResolutionMatchingEnabledChanged = { playerSettingsViewModel.updateResolutionMatchingEnabled(it) },
            gestureVolumeEnabled = appSettings.gestureVolumeEnabled,
            gestureBrightnessEnabled = appSettings.gestureBrightnessEnabled,
            gestureZoomEnabled = appSettings.gestureZoomEnabled,
            doubleTapSeekSeconds = appSettings.doubleTapSeekSeconds,
            holdSpeedMultiplier = appSettings.holdSpeedMultiplier,
            gestureScrollSensitivity = appSettings.gestureScrollSensitivity,
            onGestureVolumeEnabledChanged = { playerSettingsViewModel.updateGestureVolumeEnabled(it) },
            onGestureBrightnessEnabledChanged = { playerSettingsViewModel.updateGestureBrightnessEnabled(it) },
            onGestureZoomEnabledChanged = { playerSettingsViewModel.updateGestureZoomEnabled(it) },
            onDoubleTapSeekSecondsSelected = { playerSettingsViewModel.updateDoubleTapSeekSeconds(it) },
            onHoldSpeedMultiplierSelected = { playerSettingsViewModel.updateHoldSpeedMultiplier(it) },
            onGestureScrollSensitivityChanged = { playerSettingsViewModel.updateGestureScrollSensitivity(it) },
            previewSeekbarEnabled = appSettings.previewSeekbarEnabled,
            onPreviewSeekbarEnabledChanged = { playerSettingsViewModel.updatePreviewSeekbarEnabled(it) },
            preferredExternalPlayerPackage = appSettings.preferredExternalPlayerPackage,
            onPreferredExternalPlayerPackageSelected = { playerSettingsViewModel.updatePreferredExternalPlayerPackage(it) },
            // T1.1 – Görüntü Oranı
            aspectMode = runCatching { com.kitsugi.animelist.core.player.PlayerAspectMode.valueOf(appSettings.aspectMode) }.getOrDefault(com.kitsugi.animelist.core.player.PlayerAspectMode.ORIGINAL),
            onAspectModeSelected = { playerSettingsViewModel.updateAspectMode(it) },
            liveHelperEnabled = appSettings.liveHelperEnabled,
            onLiveHelperEnabledChanged = { playerSettingsViewModel.updateLiveHelperEnabled(it) },
            enableAssExtractor = appSettings.enableAssExtractor,
            onEnableAssExtractorChanged = { playerSettingsViewModel.updateEnableAssExtractor(it) },
            // ─── T2.6 – Oynatıcı Başlık / Medya Bilgisi Görünürlüğü ────────────────
            showPlayerTitle = appSettings.showPlayerTitle,
            onShowPlayerTitleChanged = { onShowPlayerTitleChanged(it) },
            showPlayerResolution = appSettings.showPlayerResolution,
            onShowPlayerResolutionChanged = { onShowPlayerResolutionChanged(it) },
            showMediaInfo = appSettings.showMediaInfo,
            onShowMediaInfoChanged = { onShowMediaInfoChanged(it) },
            // ─── T1-01 – StillWatching + PostPlayMode + AutoplaySessionLimit ──────────
            stillWatchingEnabled = appSettings.stillWatchingEnabled,
            onStillWatchingEnabledChanged = { playerSettingsViewModel.updateStillWatchingEnabled(it) },
            stillWatchingThresholdMinutes = appSettings.stillWatchingThresholdMinutes,
            onStillWatchingThresholdMinutesChanged = { playerSettingsViewModel.updateStillWatchingThresholdMinutes(it) },
            postPlayMode = appSettings.postPlayMode,
            onPostPlayModeChanged = { playerSettingsViewModel.updatePostPlayMode(it) },
            autoplaySessionLimit = appSettings.autoplaySessionLimit,
            onAutoplaySessionLimitChanged = { playerSettingsViewModel.updateAutoplaySessionLimit(it) },
            // ─── T1-03 – Ses Gelişmiş ────────────────────────────────────────────────
            gainBoostDb = appSettings.gainBoostDb,
            onGainBoostDbChanged = { playerSettingsViewModel.updateGainBoostDb(it) },
            subtitleDelayMs = appSettings.subtitleDelayMs,
            onSubtitleDelayMsChanged = { playerSettingsViewModel.updateSubtitleDelayMs(it) },
            // ─── T1-04 – Dekoder Önceliği (Telefon) ──────────────────────────────────
            decoderPriority = appSettings.decoderPriority,
            onDecoderPriorityChanged = { playerSettingsViewModel.updateDecoderPriority(it) }
        ),
        addon = com.kitsugi.animelist.ui.screens.settings.AddonSettings(
            addons = addonsList,
            debridToken = addonViewModel.debridToken,
            repos = reposList,
            repoPlugins = addonViewModel.repoPluginsState,
            repoLoadingState = addonViewModel.repoLoadingState,
            onAddAddon = { addonViewModel.addAddon(it) },
            onToggleAddon = { addon, enabled -> onToggleAddon(addon, enabled) },
            onDeleteAddon = { addonViewModel.deleteAddon(it) },
            onSaveDebridToken = { addonViewModel.saveDebridToken(it) },
            onAddRepo = { addonViewModel.addRepo(it) },
            onDeleteRepo = { addonViewModel.deleteRepo(it) },
            onFetchRepoPlugins = { addonViewModel.fetchRepoPlugins(it) },
            onInstallPlugin = { plugin, callback -> onInstallPlugin(plugin, callback) },
            onInstallAllPlugins = { repoUrl, repoName, plugins -> onInstallAllPlugins(repoUrl, repoName, plugins) },
            onUpdateAllPlugins = { repoUrl, repoName, plugins -> onUpdateAllPlugins(repoUrl, repoName, plugins) },
            bulkInstallRepoUrl = addonViewModel.bulkInstallRepoUrl,
            bulkInstallRepoName = addonViewModel.bulkInstallRepoName,
            bulkInstallDone = addonViewModel.bulkInstallDone,
            bulkInstallTotal = addonViewModel.bulkInstallTotal,
            bulkInstallCurrentName = addonViewModel.bulkInstallCurrentName,
            bulkInstallResultMessage = addonViewModel.bulkInstallResultMessage,
            onClearBulkInstallResult = { addonViewModel.clearBulkInstallResult() },
            csPlugins = csPluginsList,
            onToggleCsPlugin = { plugin, enabled -> onToggleCsPlugin(plugin, enabled) },
            onUninstallCsPlugin = { addonViewModel.uninstallCsPlugin(it) }
        ),
        manga = com.kitsugi.animelist.ui.screens.settings.MangaSettings(
            mangaSources = mangaViewModel.mangaSources,
            onInstallMangaExtension = { mangaViewModel.installMangaExtension(it) },
            onDeleteMangaExtension = { mangaViewModel.deleteMangaExtension(it) },
            mangaRepos = mangaViewModel.mangaReposState,
            mangaRepoExtensions = mangaViewModel.mangaRepoExtensionsState,
            mangaRepoLoadingState = mangaViewModel.mangaRepoLoadingState,
            onAddMangaRepo = { mangaViewModel.addMangaRepo(it) },
            onDeleteMangaRepo = { mangaViewModel.deleteMangaRepo(it) },
            onFetchMangaRepo = { mangaViewModel.fetchMangaRepo(it) },
            onInstallMangaApk = { ext, cb -> onInstallMangaApk(ext, cb) },
            onInstallAllMangaExtensions = { repoUrl, list -> onInstallAllMangaExtensions(repoUrl, list) },
            onUpdateAllMangaExtensions = { repoUrl, list -> onUpdateAllMangaExtensions(repoUrl, list) },
            mangaBulkInstallRepoUrl = mangaViewModel.mangaBulkInstallRepoUrl,
            mangaBulkInstallDone = mangaViewModel.mangaBulkInstallDone,
            mangaBulkInstallTotal = mangaViewModel.mangaBulkInstallTotal,
            mangaBulkInstallCurrentName = mangaViewModel.mangaBulkInstallCurrentName,
            onGetInstalledMangaVersionCode = { mangaViewModel.getInstalledVersionCode(it) },
            onGetInstalledMangaVersion = { mangaViewModel.getInstalledMangaExtensionVersion(it) },
            onGetMangaSourceHealthStatus = { mangaViewModel.getSourceHealthStatus(it) },
            onGetMangaSourceRuntimeStats = { mangaViewModel.getSourceRuntimeStats(it) },
            onGetMangaConfiguredDomain = { mangaViewModel.getConfiguredSourceDomain(it) },
            onGetMangaConfiguredBaseUrl = { mangaViewModel.getConfiguredSourceBaseUrl(it) },
            onSetMangaSourceDomain = { src, dom -> onSetMangaSourceDomain(src, dom) },
            onGetMangaSourceUserAgent = { mangaViewModel.getSourceUserAgentOverride(it) },
            onGetMangaSourceSlowdownEnabled = { mangaViewModel.getSourceSlowdownEnabled(it) },
            onSetMangaSourceUserAgent = { src, ua -> onSetMangaSourceUserAgent(src, ua) },
            onSetMangaSourceSlowdownEnabled = { src, env -> onSetMangaSourceSlowdownEnabled(src, env) },
            onResetMangaSourceDiagnostics = { mangaViewModel.resetSourceDiagnostics(it) },
            onClearAllMangaSourceDiagnostics = { mangaViewModel.clearAllSourceDiagnostics() },
            onIsMangaSourceBusy = { mangaViewModel.isSourceBusy(it) },
            onQuickCheckMangaSource = { mangaViewModel.quickCheckSource(it) },
            onRefreshMangaSourceMirror = { mangaViewModel.refreshSourceMirror(it) },
            onClearMangaSourceMirror = { mangaViewModel.clearSourceMirror(it) },
            mangaSourceStateReport = mangaViewModel.mangaSourceStateReport,
            onOpenMangaSourceHealthScreen = { onOpenMangaSourceHealthScreen() },
            onForceCheckMangaUpdates = { mangaViewModel.forceCheckMangaUpdates(it) },
            untrustedRepoToConfirm = mangaViewModel.untrustedRepoToConfirm,
            untrustedSignatureToConfirm = mangaViewModel.untrustedSignatureToConfirm,
            onConfirmUntrustedRepo = { mangaViewModel.addMangaRepo(it, force = true) },
            onDismissUntrustedRepo = { mangaViewModel.clearUntrustedRepoConfirm() },
            onConfirmUntrustedSignature = { ext, hash -> mangaViewModel.installMangaApk(ext, force = true) },
            onDismissUntrustedSignature = { mangaViewModel.clearUntrustedSignatureConfirm() }
        ),
        integrations = com.kitsugi.animelist.ui.screens.settings.IntegrationsSettings(
            dnsChoice = appSettings.dnsChoice,
            onDnsChoiceSelected = { onDnsChoiceSelected(it) },
            tmdbEnabled = appSettings.tmdbEnabled,
            onTmdbEnabledChanged = { onTmdbEnabledChanged(it) },
            tmdbApiKey = appSettings.tmdbUserApiKey,
            onTmdbApiKeyChanged = { onTmdbApiKeyChanged(it) },
            tmdbModernHomeEnabled = appSettings.tmdbModernHomeEnabled,
            onTmdbModernHomeEnabledChanged = { onTmdbModernHomeEnabledChanged(it) },
            tmdbEnrichContinueWatching = appSettings.tmdbEnrichContinueWatching,
            onTmdbEnrichContinueWatchingChanged = { onTmdbEnrichContinueWatchingChanged(it) },
            tmdbLanguage = appSettings.tmdbLanguage,
            onTmdbLanguageChanged = { onTmdbLanguageChanged(it) },
            tmdbUseArtwork = appSettings.tmdbUseArtwork,
            onTmdbUseArtworkChanged = { onTmdbUseArtworkChanged(it) },
            tmdbUseBasicInfo = appSettings.tmdbUseBasicInfo,
            onTmdbUseBasicInfoChanged = { onTmdbUseBasicInfoChanged(it) },
            tmdbUseDetails = appSettings.tmdbUseDetails,
            onTmdbUseDetailsChanged = { onTmdbUseDetailsChanged(it) },
            tmdbUseReleaseDates = appSettings.tmdbUseReleaseDates,
            onTmdbUseReleaseDatesChanged = { onTmdbUseReleaseDatesChanged(it) },
            tmdbUseCredits = appSettings.tmdbUseCredits,
            onTmdbUseCreditsChanged = { onTmdbUseCreditsChanged(it) },
            tmdbUseProductions = appSettings.tmdbUseProductions,
            onTmdbUseProductionsChanged = { onTmdbUseProductionsChanged(it) },
            tmdbUseNetworks = appSettings.tmdbUseNetworks,
            onTmdbUseNetworksChanged = { onTmdbUseNetworksChanged(it) },
            tmdbUseEpisodes = appSettings.tmdbUseEpisodes,
            onTmdbUseEpisodesChanged = { onTmdbUseEpisodesChanged(it) },
            tmdbUseTrailers = appSettings.tmdbUseTrailers,
            onTmdbUseTrailersChanged = { onTmdbUseTrailersChanged(it) },
            tmdbUseMoreLikeThis = appSettings.tmdbUseMoreLikeThis,
            onTmdbUseMoreLikeThisChanged = { onTmdbUseMoreLikeThisChanged(it) },
            tmdbUseCollections = appSettings.tmdbUseCollections,
            onTmdbUseCollectionsChanged = { onTmdbUseCollectionsChanged(it) },
            mdbListEnabled = appSettings.mdbListEnabled,
            onMdbListEnabledChanged = { onMdbListEnabledChanged(it) },
            mdbListApiKey = appSettings.mdbListApiKey,
            onMdbListApiKeyChanged = { onMdbListApiKeyChanged(it) },
            mdbListShowImdb = appSettings.mdbListShowImdb,
            onMdbListShowImdbChanged = { onMdbListShowImdbChanged(it) },
            mdbListShowTomatoes = appSettings.mdbListShowTomatoes,
            onMdbListShowTomatoesChanged = { onMdbListShowTomatoesChanged(it) },
            mdbListShowMetacritic = appSettings.mdbListShowMetacritic,
            onMdbListShowMetacriticChanged = { onMdbListShowMetacriticChanged(it) },
            mdbListShowAudience = appSettings.mdbListShowAudience,
            onMdbListShowAudienceChanged = { onMdbListShowAudienceChanged(it) },
            mdbListShowLetterboxd = appSettings.mdbListShowLetterboxd,
            onMdbListShowLetterboxdChanged = { onMdbListShowLetterboxdChanged(it) },
            mdbListShowTmdb = appSettings.mdbListShowTmdb,
            onMdbListShowTmdbChanged = { onMdbListShowTmdbChanged(it) },
            mdbListShowTrakt = appSettings.mdbListShowTrakt,
            onMdbListShowTraktChanged = { onMdbListShowTraktChanged(it) },
            aniSkipEnabled = appSettings.aniSkipEnabled,
            onAniSkipEnabledChanged = { onAniSkipEnabledChanged(it) },
            aniSkipAutoSkip = appSettings.aniSkipAutoSkip,
            onAniSkipAutoSkipChanged = { onAniSkipAutoSkipChanged(it) },
            animeSkipClientId = appSettings.animeSkipClientId,
            onAnimeSkipClientIdChanged = { onAnimeSkipClientIdChanged(it) },
            autoTranslateEnabled = appSettings.autoTranslateEnabled,
            onAutoTranslateEnabledChanged = { onAutoTranslateEnabledChanged(it) },
            onOpenStats = { onOpenStats() },
            onOpenFavourites = { onOpenFavourites() },
            onOpenAbout = { onOpenAbout() }
        )
    )

// ─── Non-Composable SettingsContext Extension Functions ────────────────────────────

internal fun SettingsContext.onHomeLayoutSelected(homeLayout: String) {
    coroutineScope.launch {
        settingsDataStore.setSelectedHomeLayoutId(homeLayout)
        appViewModel.showSnackbarMessage("Ana sayfa düzeni güncellendi")
    }
}

internal fun SettingsContext.onProfileInfoSave(name: String, title: String) {
    coroutineScope.launch {
        settingsDataStore.setProfileInfo(name, title, appSettings.anilistUsername)
        appViewModel.showSnackbarMessage("Profil güncellendi")
    }
}

internal fun SettingsContext.onClearProfileImageClick() {
    coroutineScope.launch {
        settingsDataStore.setProfileImageUri("")
        appViewModel.showSnackbarMessage("Profil resmi kaldırıldı")
    }
}

internal fun SettingsContext.onClearBannerImageClick() {
    coroutineScope.launch {
        settingsDataStore.setBannerImageUri("")
        appViewModel.showSnackbarMessage("Afiş resmi kaldırıldı")
    }
}

internal fun SettingsContext.onAniListAuthClick() {
    if (authViewModel.isAniListConnected) authViewModel.disconnectExternalAccount("anilist")
    else authViewModel.startExternalAuth("anilist")
}

internal fun SettingsContext.onMalAuthClick() {
    if (authViewModel.isMalConnected) authViewModel.disconnectExternalAccount("mal")
    else authViewModel.startExternalAuth("mal")
}

internal fun SettingsContext.onSimklAuthClick() {
    if (authViewModel.isSimklConnected) authViewModel.disconnectExternalAccount("simkl")
    else authViewModel.startExternalAuth("simkl")
}

internal fun SettingsContext.onAdultContentChanged(show: Boolean) {
    coroutineScope.launch {
        settingsDataStore.setShowAdultContent(show)
        appViewModel.showSnackbarMessage(if (show) "+18 içerikler gösteriliyor" else "+18 içerikler gizlendi")
    }
}

internal fun SettingsContext.onFixedNavBarChanged(enabled: Boolean) {
    coroutineScope.launch {
        settingsDataStore.setFixedNavBar(enabled)
        appViewModel.showSnackbarMessage(if (enabled) "Sabit alt bar etkin" else "Sabit alt bar devre dışı")
    }
}

internal fun SettingsContext.onAiringNotificationsChanged(enabled: Boolean) {
    coroutineScope.launch {
        settingsDataStore.setAiringNotificationsEnabled(enabled)
        appViewModel.showSnackbarMessage(if (enabled) "Yayın bildirimleri etkinleştirildi" else "Yayın bildirimleri devre dışı bırakıldı")
    }
}


internal fun SettingsContext.onSplashAnimationEnabledChanged(enabled: Boolean) {
    coroutineScope.launch {
        settingsDataStore.setSplashAnimationEnabled(enabled)
        appViewModel.showSnackbarMessage(if (enabled) "Açılış animasyonu etkin" else "Açılış animasyonu devre dışı")
    }
}

internal fun SettingsContext.onSplashSoundEnabledChanged(enabled: Boolean) {
    coroutineScope.launch {
        settingsDataStore.setSplashSoundEnabled(enabled)
        appViewModel.showSnackbarMessage(if (enabled) "Açılış sesi etkin" else "Açılış sesi devre dışı")
    }
}

internal fun SettingsContext.onToggleAddon(addon: com.kitsugi.animelist.data.local.ManagedAddonEntity, enabled: Boolean) {
    addonViewModel.toggleAddon(addon, enabled)
}

internal fun SettingsContext.onInstallPlugin(plugin: com.kitsugi.animelist.data.remote.CsPlugin, callback: ((Boolean) -> Unit)?) {
    addonViewModel.installPlugin(plugin, callback)
}

internal fun SettingsContext.onInstallAllPlugins(repoUrl: String, repoName: String, plugins: List<com.kitsugi.animelist.data.remote.CsPlugin>) {
    addonViewModel.installAllPlugins(repoUrl, repoName, plugins, addonsList, csPluginsList)
}

internal fun SettingsContext.onUpdateAllPlugins(repoUrl: String, repoName: String, plugins: List<com.kitsugi.animelist.data.remote.CsPlugin>) {
    addonViewModel.updateAllPlugins(repoUrl, repoName, plugins, csPluginsList)
}

internal fun SettingsContext.onToggleCsPlugin(plugin: com.kitsugi.animelist.data.local.CsPluginEntity, enabled: Boolean) {
    addonViewModel.toggleCsPlugin(plugin, enabled)
}

internal fun SettingsContext.onInstallMangaApk(ext: com.kitsugi.animelist.data.remote.MangaExtensionInfo, cb: ((Boolean) -> Unit)?) {
    mangaViewModel.installMangaApk(ext, cb)
}

internal fun SettingsContext.onInstallAllMangaExtensions(repoUrl: String, list: List<com.kitsugi.animelist.data.remote.MangaExtensionInfo>) {
    mangaViewModel.installAllMangaExtensions(repoUrl, list)
}

internal fun SettingsContext.onUpdateAllMangaExtensions(repoUrl: String, list: List<com.kitsugi.animelist.data.remote.MangaExtensionInfo>) {
    mangaViewModel.updateAllMangaExtensions(repoUrl, list)
}

internal fun SettingsContext.onSetMangaSourceDomain(src: com.kitsugi.animelist.data.manga.MangaSource, dom: String?) {
    mangaViewModel.setSourceDomainOverride(src, dom)
}

internal fun SettingsContext.onSetMangaSourceUserAgent(src: com.kitsugi.animelist.data.manga.MangaSource, ua: String?) {
    mangaViewModel.setSourceUserAgentOverride(src, ua)
}

internal fun SettingsContext.onSetMangaSourceSlowdownEnabled(src: com.kitsugi.animelist.data.manga.MangaSource, env: Boolean) {
    mangaViewModel.setSourceSlowdownEnabled(src, env)
}

internal fun SettingsContext.onOpenMangaSourceHealthScreen() {
    navState.openMangaSourceHealth()
}

internal fun SettingsContext.onAniListImportClick() {
    authViewModel.importAniListAnimeList(mediaEntries, mediaRepository)
}

internal fun SettingsContext.onMalImportClick() {
    authViewModel.importMalAnimeList(mediaEntries, mediaRepository)
}

internal fun SettingsContext.onSimklImportClick() {
    authViewModel.importSimklList(mediaEntries, mediaRepository)
}

internal fun SettingsContext.onThemeSelected(themeId: String) {
    appViewModel.updateTheme(themeId, settingsDataStore)
}

internal fun SettingsContext.onListLayoutSelected(layoutId: String) {
    appViewModel.updateListLayout(layoutId, settingsDataStore)
}

internal fun SettingsContext.onTitleLanguageSelected(lang: String) {
    appViewModel.updateTitleLanguage(lang, settingsDataStore)
}

internal fun SettingsContext.onScoreFormatSelected(format: String) {
    appViewModel.updateScoreFormat(format, settingsDataStore)
}

internal fun SettingsContext.onHideScoresChanged(hide: Boolean) {
    appViewModel.updateHideScores(hide, settingsDataStore)
}

internal fun SettingsContext.onShowAnimeLogosChanged(show: Boolean) {
    appViewModel.updateShowAnimeLogos(show, settingsDataStore)
}

internal fun SettingsContext.onDeleteAllEntries() {
    appViewModel.deleteAllEntries(mediaRepository)
}

internal fun SettingsContext.onCreateBackupText() {
    appViewModel.createBackupText(mediaEntries)
}

internal fun SettingsContext.onImportBackupText() {
    appViewModel.importBackupText(mediaEntries, mediaRepository)
}

internal fun SettingsContext.onDnsChoiceSelected(choice: Int) {
    appViewModel.updateDnsChoice(choice, settingsDataStore)
}

internal fun SettingsContext.onTmdbEnabledChanged(enabled: Boolean) {
    coroutineScope.launch { settingsDataStore.setTmdbEnabled(enabled) }
}

internal fun SettingsContext.onTmdbApiKeyChanged(key: String) {
    coroutineScope.launch { settingsDataStore.setTmdbUserApiKey(key) }
}

internal fun SettingsContext.onTmdbModernHomeEnabledChanged(enabled: Boolean) {
    coroutineScope.launch { settingsDataStore.setTmdbModernHomeEnabled(enabled) }
}

internal fun SettingsContext.onTmdbEnrichContinueWatchingChanged(enabled: Boolean) {
    coroutineScope.launch { settingsDataStore.setTmdbEnrichContinueWatching(enabled) }
}

internal fun SettingsContext.onTmdbLanguageChanged(lang: String) {
    coroutineScope.launch { settingsDataStore.setTmdbLanguage(lang) }
}

internal fun SettingsContext.onTmdbUseArtworkChanged(use: Boolean) {
    coroutineScope.launch { settingsDataStore.setTmdbUseArtwork(use) }
}

internal fun SettingsContext.onTmdbUseBasicInfoChanged(use: Boolean) {
    coroutineScope.launch { settingsDataStore.setTmdbUseBasicInfo(use) }
}

internal fun SettingsContext.onTmdbUseDetailsChanged(use: Boolean) {
    coroutineScope.launch { settingsDataStore.setTmdbUseDetails(use) }
}

internal fun SettingsContext.onTmdbUseReleaseDatesChanged(use: Boolean) {
    coroutineScope.launch { settingsDataStore.setTmdbUseReleaseDates(use) }
}

internal fun SettingsContext.onTmdbUseCreditsChanged(use: Boolean) {
    coroutineScope.launch { settingsDataStore.setTmdbUseCredits(use) }
}

internal fun SettingsContext.onTmdbUseProductionsChanged(use: Boolean) {
    coroutineScope.launch { settingsDataStore.setTmdbUseProductions(use) }
}

internal fun SettingsContext.onTmdbUseNetworksChanged(use: Boolean) {
    coroutineScope.launch { settingsDataStore.setTmdbUseNetworks(use) }
}

internal fun SettingsContext.onTmdbUseEpisodesChanged(use: Boolean) {
    coroutineScope.launch { settingsDataStore.setTmdbUseEpisodes(use) }
}

internal fun SettingsContext.onTmdbUseTrailersChanged(use: Boolean) {
    coroutineScope.launch { settingsDataStore.setTmdbUseTrailers(use) }
}

internal fun SettingsContext.onTmdbUseMoreLikeThisChanged(use: Boolean) {
    coroutineScope.launch { settingsDataStore.setTmdbUseMoreLikeThis(use) }
}

internal fun SettingsContext.onTmdbUseCollectionsChanged(use: Boolean) {
    coroutineScope.launch { settingsDataStore.setTmdbUseCollections(use) }
}

internal fun SettingsContext.onMdbListEnabledChanged(enabled: Boolean) {
    coroutineScope.launch { settingsDataStore.setMdbListEnabled(enabled) }
}

internal fun SettingsContext.onMdbListApiKeyChanged(key: String) {
    coroutineScope.launch { settingsDataStore.setMdbListApiKey(key) }
}

internal fun SettingsContext.onMdbListShowImdbChanged(show: Boolean) {
    coroutineScope.launch { settingsDataStore.setMdbListShowImdb(show) }
}

internal fun SettingsContext.onMdbListShowTomatoesChanged(show: Boolean) {
    coroutineScope.launch { settingsDataStore.setMdbListShowTomatoes(show) }
}

internal fun SettingsContext.onMdbListShowMetacriticChanged(show: Boolean) {
    coroutineScope.launch { settingsDataStore.setMdbListShowMetacritic(show) }
}

internal fun SettingsContext.onMdbListShowAudienceChanged(show: Boolean) {
    coroutineScope.launch { settingsDataStore.setMdbListShowAudience(show) }
}

internal fun SettingsContext.onMdbListShowLetterboxdChanged(show: Boolean) {
    coroutineScope.launch { settingsDataStore.setMdbListShowLetterboxd(show) }
}

internal fun SettingsContext.onMdbListShowTmdbChanged(show: Boolean) {
    coroutineScope.launch { settingsDataStore.setMdbListShowTmdb(show) }
}

internal fun SettingsContext.onMdbListShowTraktChanged(show: Boolean) {
    coroutineScope.launch { settingsDataStore.setMdbListShowTrakt(show) }
}

internal fun SettingsContext.onAniSkipEnabledChanged(enabled: Boolean) {
    coroutineScope.launch { settingsDataStore.setAniSkipEnabled(enabled) }
}

internal fun SettingsContext.onAniSkipAutoSkipChanged(autoSkip: Boolean) {
    coroutineScope.launch { settingsDataStore.setAniSkipAutoSkip(autoSkip) }
}

internal fun SettingsContext.onAnimeSkipClientIdChanged(clientId: String) {
    coroutineScope.launch { settingsDataStore.setAnimeSkipClientId(clientId) }
}

internal fun SettingsContext.onAutoTranslateEnabledChanged(enabled: Boolean) {
    coroutineScope.launch { settingsDataStore.setAutoTranslateEnabled(enabled) }
}

internal fun SettingsContext.onThemeModeSelected(mode: String) {
    coroutineScope.launch { settingsDataStore.setThemeMode(mode) }
}

internal fun SettingsContext.onAmoledBlackChanged(enabled: Boolean) {
    coroutineScope.launch { settingsDataStore.setAmoledBlack(enabled) }
}

internal fun SettingsContext.onCustomAccentColorChanged(color: Int) {
    coroutineScope.launch { settingsDataStore.setCustomAccentColor(color) }
}

internal fun SettingsContext.onDefaultTabSelected(tab: String) {
    coroutineScope.launch { settingsDataStore.setDefaultTab(tab) }
}

internal fun SettingsContext.onAppLanguageSelected(lang: String) {
    coroutineScope.launch { settingsDataStore.setAppLanguage(lang) }
    LocaleCache.updateLocale(context, lang)
}

internal fun SettingsContext.onOpenStats() {
    navState.navigateToDetail(com.kitsugi.animelist.DetailScreen.Stats)
}

internal fun SettingsContext.onOpenFavourites() {
    navState.navigateToDetail(com.kitsugi.animelist.DetailScreen.Favourites)
}

internal fun SettingsContext.onOpenAbout() {
    navState.navigateToDetail(com.kitsugi.animelist.DetailScreen.About)
}

internal fun SettingsContext.onCrossSyncClick() {
    authViewModel.syncPlatforms(mediaRepository)
}

internal fun SettingsContext.onSearchHistoryEnabledChanged(enabled: Boolean) {
    coroutineScope.launch {
        settingsDataStore.setSearchHistoryEnabled(enabled)
    }
}

// ─── T2.6 – Oynatıcı Başlık / Medya Bilgisi Görünürlüğü ────────────────
internal fun SettingsContext.onShowPlayerTitleChanged(show: Boolean) {
    coroutineScope.launch { settingsDataStore.setShowPlayerTitle(show) }
}

internal fun SettingsContext.onShowPlayerResolutionChanged(show: Boolean) {
    coroutineScope.launch { settingsDataStore.setShowPlayerResolution(show) }
}

internal fun SettingsContext.onShowMediaInfoChanged(show: Boolean) {
    coroutineScope.launch { settingsDataStore.setShowMediaInfo(show) }
}

// ─── T1-07 – Manga Okuyucu Varsayılan Ayarları ───────────────────────────────
internal fun SettingsContext.onMangaReadingModeSelected(mode: String) {
    coroutineScope.launch {
        settingsDataStore.setMangaReadingMode(mode)
        appViewModel.showSnackbarMessage("Manga okuma modu: $mode")
    }
}

internal fun SettingsContext.onMangaColorFilterSelected(filter: String) {
    coroutineScope.launch {
        settingsDataStore.setMangaColorFilter(filter)
        appViewModel.showSnackbarMessage("Renk filtresi: $filter")
    }
}

internal fun SettingsContext.onMangaFitModeSelected(fitMode: String) {
    coroutineScope.launch {
        settingsDataStore.setMangaFitMode(fitMode)
        appViewModel.showSnackbarMessage("Sığdırma modu: $fitMode")
    }
}

internal fun SettingsContext.onMangaBrightnessChanged(brightness: Float) {
    coroutineScope.launch {
        settingsDataStore.setMangaBrightness(brightness)
    }
}

internal fun SettingsContext.onAutoUpdateCheckEnabledChanged(enabled: Boolean) {
    coroutineScope.launch {
        settingsDataStore.setAutoUpdateCheckEnabled(enabled)
        appViewModel.showSnackbarMessage(if (enabled) "Otomatik güncelleme kontrolü açıldı" else "Otomatik güncelleme kontrolü kapatıldı")
    }
}

internal fun SettingsContext.onCheckForUpdatesClick() {
    updateViewModel.checkForUpdates(silent = false, force = true)
}

internal fun SettingsContext.onCustomImageDownloadUriChanged(uri: String) {
    coroutineScope.launch {
        settingsDataStore.setCustomImageDownloadUri(uri)
        appViewModel.showSnackbarMessage(if (uri.isNotBlank()) "İndirme klasörü güncellendi" else "İndirme klasörü varsayılana sıfırlandı")
    }
}



