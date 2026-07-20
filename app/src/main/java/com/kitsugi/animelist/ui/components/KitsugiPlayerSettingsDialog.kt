package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.components.player.PlayerGeneralTab
import com.kitsugi.animelist.ui.components.player.PlayerSubtitleAudioTab
import com.kitsugi.animelist.ui.components.player.PlayerBufferTab
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

@OptIn(ExperimentalMaterial3Api::class)
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
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val scope = rememberCoroutineScope()

    KitsugiSheetOrDialog(
        onDismiss = onDismiss,
        heightFraction = 0.93f
    ) {
            // Header content (Title, Close button, and TabRow)
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
                        text = "Oynatıcı Ayarları",
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
                                "Genel",
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
                                "Altyazı & Ses",
                                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        text = {
                            Text(
                                "Gelişmiş Arabellek",
                                fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Body content (HorizontalPager)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) { page ->
                when (page) {
                    0 -> PlayerGeneralTab(
                        playerPreference = playerPreference,
                        preferredExternalPlayerPackage = preferredExternalPlayerPackage,
                        isAutoplayEnabled = isAutoplayEnabled,
                        skipIntroDurationSec = skipIntroDurationSec,
                        dv7HandlingMode = dv7HandlingMode,
                        stripHdr10PlusSei = stripHdr10PlusSei,
                        qualityProfileJson = qualityProfileJson,
                        accentColor = accentColor,
                        frameRateMatchingMode = frameRateMatchingMode,
                        resolutionMatchingEnabled = resolutionMatchingEnabled,
                        gestureVolumeEnabled = gestureVolumeEnabled,
                        gestureBrightnessEnabled = gestureBrightnessEnabled,
                        gestureZoomEnabled = gestureZoomEnabled,
                        doubleTapSeekSeconds = doubleTapSeekSeconds,
                        holdSpeedMultiplier = holdSpeedMultiplier,
                        gestureScrollSensitivity = gestureScrollSensitivity,
                        onPlayerPreferenceSelected = onPlayerPreferenceSelected,
                        onPreferredExternalPlayerPackageSelected = onPreferredExternalPlayerPackageSelected,
                        onAutoplayEnabledChanged = onAutoplayEnabledChanged,
                        onSkipIntroDurationSecSelected = onSkipIntroDurationSecSelected,
                        onDv7HandlingModeSelected = onDv7HandlingModeSelected,
                        onStripHdr10PlusSeiChanged = onStripHdr10PlusSeiChanged,
                        onQualityProfileSelected = onQualityProfileSelected,
                        onFrameRateMatchingModeSelected = onFrameRateMatchingModeSelected,
                        onResolutionMatchingEnabledChanged = onResolutionMatchingEnabledChanged,
                        onGestureVolumeEnabledChanged = onGestureVolumeEnabledChanged,
                        onGestureBrightnessEnabledChanged = onGestureBrightnessEnabledChanged,
                        onGestureZoomEnabledChanged = onGestureZoomEnabledChanged,
                        onDoubleTapSeekSecondsSelected = onDoubleTapSeekSecondsSelected,
                        onHoldSpeedMultiplierSelected = onHoldSpeedMultiplierSelected,
                        onGestureScrollSensitivityChanged = onGestureScrollSensitivityChanged,
                        previewSeekbarEnabled = previewSeekbarEnabled,
                        onPreviewSeekbarEnabledChanged = onPreviewSeekbarEnabledChanged,
                        aspectMode = aspectMode,
                        onAspectModeSelected = onAspectModeSelected,
                        liveHelperEnabled = liveHelperEnabled,
                        onLiveHelperEnabledChanged = onLiveHelperEnabledChanged,
                        enableAssExtractor = enableAssExtractor,
                        onEnableAssExtractorChanged = onEnableAssExtractorChanged,
                        showPlayerTitle = showPlayerTitle,
                        onShowPlayerTitleChanged = onShowPlayerTitleChanged,
                        showPlayerResolution = showPlayerResolution,
                        onShowPlayerResolutionChanged = onShowPlayerResolutionChanged,
                        showMediaInfo = showMediaInfo,
                        onShowMediaInfoChanged = onShowMediaInfoChanged,
                        // ─── T1-01 – StillWatching + PostPlayMode + AutoplaySessionLimit ─────────
                        stillWatchingEnabled = stillWatchingEnabled,
                        onStillWatchingEnabledChanged = onStillWatchingEnabledChanged,
                        stillWatchingThresholdMinutes = stillWatchingThresholdMinutes,
                        onStillWatchingThresholdMinutesChanged = onStillWatchingThresholdMinutesChanged,
                        postPlayMode = postPlayMode,
                        onPostPlayModeChanged = onPostPlayModeChanged,
                        autoplaySessionLimit = autoplaySessionLimit,
                        onAutoplaySessionLimitChanged = onAutoplaySessionLimitChanged,
                        // ─── T1-03 – Ses Gelişmiş ────────────────────────────────────────────────
                        gainBoostDb = gainBoostDb,
                        onGainBoostDbChanged = onGainBoostDbChanged,
                        subtitleDelayMs = subtitleDelayMs,
                        onSubtitleDelayMsChanged = onSubtitleDelayMsChanged,
                        // ─── T1-04 – Dekoder Önceliği (Telefon) ──────────────────────────────────
                        decoderPriority = decoderPriority,
                        onDecoderPriorityChanged = onDecoderPriorityChanged
                    )
                    1 -> PlayerSubtitleAudioTab(
                        defaultSubtitleSize = defaultSubtitleSize,
                        defaultSubtitleColor = defaultSubtitleColor,
                        subtitleBold = subtitleBold,
                        subtitleOutlineEnabled = subtitleOutlineEnabled,
                        defaultAudioBoost = defaultAudioBoost,
                        defaultAudioDelayMs = defaultAudioDelayMs,
                        preferredSubtitleLanguages = preferredSubtitleLanguages,
                        addonSubtitleStartupMode = addonSubtitleStartupMode,
                        accentColor = accentColor,
                        speakerDelayMs = speakerDelayMs,
                        bluetoothDelayMs = bluetoothDelayMs,
                        wiredDelayMs = wiredDelayMs,
                        hdmiDelayMs = hdmiDelayMs,
                        activeAudioRoute = activeAudioRoute,
                        onRouteDelayChanged = onRouteDelayChanged,
                        onDefaultSubtitleSizeSelected = onDefaultSubtitleSizeSelected,
                        onDefaultSubtitleColorSelected = onDefaultSubtitleColorSelected,
                        onSubtitleBoldChanged = onSubtitleBoldChanged,
                        onSubtitleOutlineEnabledChanged = onSubtitleOutlineEnabledChanged,
                        onDefaultAudioBoostSelected = onDefaultAudioBoostSelected,
                        onDefaultAudioDelayMsSelected = onDefaultAudioDelayMsSelected,
                        onPreferredSubtitleLanguagesSelected = onPreferredSubtitleLanguagesSelected,
                        onAddonSubtitleStartupModeSelected = onAddonSubtitleStartupModeSelected
                    )
                    else -> PlayerBufferTab(
                        minBufferMs = minBufferMs,
                        maxBufferMs = maxBufferMs,
                        bufferForPlaybackMs = bufferForPlaybackMs,
                        bufferForPlaybackAfterRebufferMs = bufferForPlaybackAfterRebufferMs,
                        backBufferDurationMs = backBufferDurationMs,
                        parallelRangeEnabled = parallelRangeEnabled,
                        accentColor = accentColor,
                        onBufferSettingsChanged = onBufferSettingsChanged,
                        onParallelRangeEnabledChanged = onParallelRangeEnabledChanged
                    )
                }
            }

            // Footer content (Tamam button)
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

