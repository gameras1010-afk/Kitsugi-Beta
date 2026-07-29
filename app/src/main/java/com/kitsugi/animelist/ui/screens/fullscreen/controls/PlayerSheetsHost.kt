package com.kitsugi.animelist.ui.screens.fullscreen.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    when (sheetShown) {
        KitsugiSheets.None -> Unit

        KitsugiSheets.PlaybackSpeed -> {
            PlaybackSpeedSheet(
                speed = currentSpeed,
                onSpeedChange = onSpeedChange,
                onSetAsDefault = onSetSpeedAsDefault,
                onDismissRequest = onDismissRequest,
            )
        }

        KitsugiSheets.Chapters -> {
            val chapter = currentChapter ?: return
            if (chapters.isEmpty()) return
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
                modifier = modifier,
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
                modifier = modifier,
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
                modifier = modifier,
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
                modifier = modifier,
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
                modifier = modifier,
            )
        }
    }
}
