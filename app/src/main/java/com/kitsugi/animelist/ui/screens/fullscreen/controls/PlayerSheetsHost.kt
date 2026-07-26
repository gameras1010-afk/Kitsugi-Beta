package com.kitsugi.animelist.ui.screens.fullscreen.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kitsugi.animelist.core.player.SubtitleInput
import com.kitsugi.animelist.data.repository.StreamSource
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
    subtitleTracks: List<SubtitleInput> = emptyList(),
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
    onSaveScreenshot: () -> Unit = {},
    onShareScreenshot: () -> Unit = {},

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
            val subtitleTrackInfos = subtitleTracks.mapIndexed { index, input ->
                SubtitleTrackInfo(
                    id = index,
                    label = input.name ?: "Altyazı $index",
                    language = input.lang
                )
            }
            val selectedIndices = if (selectedSubtitleIndex >= 0) listOf(selectedSubtitleIndex) else emptyList()
            SubtitleTracksSheet(
                tracks = subtitleTrackInfos,
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
                modifier = modifier,
            )
        }

        KitsugiSheets.Screenshot -> {
            ScreenshotSheet(
                onSave = onSaveScreenshot,
                onShare = onShareScreenshot,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
            )
        }
    }
}
