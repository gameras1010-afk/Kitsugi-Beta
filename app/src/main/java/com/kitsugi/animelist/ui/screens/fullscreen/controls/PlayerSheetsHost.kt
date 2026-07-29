package com.kitsugi.animelist.ui.screens.fullscreen.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.ui.screens.fullscreen.AudioChannels
import com.kitsugi.animelist.ui.screens.fullscreen.Decoder
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiPanels
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiSheets
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.IndexedSegment
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets.ChaptersSheet
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets.PlaybackSpeedSheet
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets.SubtitleTracksSheet
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets.SubtitleTrackInfo
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets.AudioTracksSheet
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets.AudioTrackInfo
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets.QualitySheet
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets.MoreSheet
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets.ScreenshotSheet
import com.kitsugi.animelist.ui.screens.fullscreen.ArtType
import com.kitsugi.animelist.data.local.CustomButton
import java.io.InputStream

/**
 * Tüm sheet'leri merkezi olarak yöneten koordinatör.
 * [sheetShown] değerine göre ilgili sheet gösterilir.
 */
@Composable
fun PlayerSheetsHost(
    sheetShown: KitsugiSheets,
    dismissSheet: Boolean,
    onDismissRequest: () -> Unit,
    onOpenPanel: (KitsugiPanels) -> Unit,

    // Playback Speed
    currentSpeed: Float = 1f,
    onSpeedChange: (Float) -> Unit = {},
    onSetSpeedAsDefault: (Float) -> Unit = {},

    // Chapters
    currentChapter: IndexedSegment? = null,
    chapters: List<IndexedSegment> = emptyList(),
    onSeekToChapter: (IndexedSegment) -> Unit = {},

    // Subtitle Tracks
    subtitleTracks: List<SubtitleTrackInfo> = emptyList(),
    selectedSubtitleIndex: Int = -1,
    onSelectSubtitle: (Int) -> Unit = {},
    onAddSubtitleFile: () -> Unit = {},

    // Audio Tracks
    audioTrackLabels: List<String> = emptyList(),
    selectedAudioIndex: Int = -1,
    onSelectAudio: (Int) -> Unit = {},
    onAddAudioFile: () -> Unit = {},

    // Quality / Stream Sources
    streamSources: List<StreamSource> = emptyList(),
    selectedSourceIndex: Int = -1,
    onSelectSource: (Int) -> Unit = {},
    isQualityLoading: Boolean = false,

    // Sleep Timer
    sleepTimerSecondsLeft: Int = 0,
    onStartSleepTimer: (Int) -> Unit = {},

    // Decoder selection
    selectedDecoder: Decoder = Decoder.Auto,
    onSelectDecoder: (Decoder) -> Unit = {},

    // Screenshot actions
    isLocalSource: Boolean = false,
    hasSubTracks: Boolean = false,
    showSubtitles: Boolean = true,
    onToggleShowSubtitles: (Boolean) -> Unit = {},
    cachePath: String = "",
    onSetAsArt: (ArtType, (() -> InputStream)) -> Unit = { _, _ -> },
    onSaveScreenshot: (() -> InputStream) -> Unit = {},
    onShareScreenshot: (() -> InputStream) -> Unit = {},
    takeScreenshot: (String, Boolean) -> InputStream? = { _, _ -> null },

    customButtons: List<CustomButton> = emptyList(),
    onClickCustomButton: (CustomButton) -> Unit = {},
    onLongClickCustomButton: (CustomButton) -> Unit = {},

    // Statistics Page
    statisticsPage: Int = 0,
    onSelectStatisticsPage: (Int) -> Unit = {},

    // Audio Channels
    audioChannels: AudioChannels = AudioChannels.Auto,
    onSelectAudioChannels: (AudioChannels) -> Unit = {},

    modifier: Modifier = Modifier,
) {
    val isVisible = sheetShown != KitsugiSheets.None

    Box(modifier = modifier) {
        // Dim background overlay
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    )
            )
        }

        // Slide-up sheet container
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            when (sheetShown) {
                KitsugiSheets.None -> Box(Modifier.size(0.dp))

                KitsugiSheets.PlaybackSpeed -> {
                    PlaybackSpeedSheet(
                        speed = currentSpeed,
                        onSpeedChange = onSpeedChange,
                        onSetAsDefault = onSetSpeedAsDefault,
                        onDismissRequest = onDismissRequest,
                    )
                }

                KitsugiSheets.Chapters -> {
                    val chapter = currentChapter ?: return@AnimatedVisibility
                    if (chapters.isEmpty()) return@AnimatedVisibility
                    ChaptersSheet(
                        chapters = chapters,
                        currentChapter = chapter,
                        onClick = onSeekToChapter,
                        onDismissRequest = onDismissRequest,
                        dismissSheet = dismissSheet,
                    )
                }

                KitsugiSheets.SubtitleTracks -> {
                    val selectedIndices = if (selectedSubtitleIndex >= 0) listOf(selectedSubtitleIndex) else emptyList()
                    SubtitleTracksSheet(
                        tracks = subtitleTracks,
                        selectedIndices = selectedIndices,
                        onSelect = onSelectSubtitle,
                        onAddSubtitleFile = onAddSubtitleFile,
                        onOpenSubtitleSettings = { onOpenPanel(KitsugiPanels.SubtitleSettings) },
                        onOpenSubtitleDelay = { onOpenPanel(KitsugiPanels.SubtitleDelay) },
                        onDismissRequest = onDismissRequest,
                    )
                }

                KitsugiSheets.AudioTracks -> {
                    val audioTrackInfos = audioTrackLabels.mapIndexed { index, label ->
                        AudioTrackInfo(
                            id = index,
                            label = label
                        )
                    }
                    AudioTracksSheet(
                        tracks = audioTrackInfos,
                        selectedId = selectedAudioIndex,
                        onSelect = onSelectAudio,
                        onAddAudioFile = onAddAudioFile,
                        onOpenDelayPanel = { onOpenPanel(KitsugiPanels.AudioDelay) },
                        onDismissRequest = onDismissRequest,
                    )
                }

                KitsugiSheets.QualityTracks -> {
                    QualitySheet(
                        streamSources = streamSources,
                        selectedIndex = selectedSourceIndex,
                        isLoading = isQualityLoading,
                        onSelect = onSelectSource,
                        onDismissRequest = onDismissRequest,
                        dismissSheet = dismissSheet,
                    )
                }

                KitsugiSheets.More -> {
                    MoreSheet(
                        selectedDecoder = selectedDecoder,
                        onSelectDecoder = onSelectDecoder,
                        remainingTime = sleepTimerSecondsLeft,
                        onStartTimer = onStartSleepTimer,
                        onDismissRequest = onDismissRequest,
                        onEnterFiltersPanel = { onOpenPanel(KitsugiPanels.VideoFilters) },
                        customButtons = customButtons,
                        onClickCustomButton = onClickCustomButton,
                        onLongClickCustomButton = onLongClickCustomButton,
                        statisticsPage = statisticsPage,
                        onSelectStatisticsPage = onSelectStatisticsPage,
                        audioChannels = audioChannels,
                        onSelectAudioChannels = onSelectAudioChannels,
                    )
                }

                KitsugiSheets.Screenshot -> {
                    ScreenshotSheet(
                        isLocalSource = isLocalSource,
                        hasSubTracks = hasSubTracks,
                        showSubtitles = showSubtitles,
                        onToggleShowSubtitles = onToggleShowSubtitles,
                        cachePath = cachePath,
                        onSetAsArt = onSetAsArt,
                        onSave = onSaveScreenshot,
                        onShare = onShareScreenshot,
                        takeScreenshot = takeScreenshot,
                        onDismissRequest = onDismissRequest,
                    )
                }
            }
        }
    }
}
