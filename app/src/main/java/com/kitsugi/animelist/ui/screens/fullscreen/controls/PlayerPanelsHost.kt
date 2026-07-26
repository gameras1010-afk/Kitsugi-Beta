package com.kitsugi.animelist.ui.screens.fullscreen.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiPanels
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiVideoFilters
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.panels.AudioDelayPanel
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.panels.SubtitleDelayPanel
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.panels.SubtitleSettingsPanel
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.panels.VideoFiltersPanel
import com.kitsugi.animelist.ui.screens.fullscreen.components.SubtitleStyleSettings

@Composable
fun PlayerPanelsHost(
    panelShown: KitsugiPanels,
    onDismissRequest: () -> Unit,

    // AudioDelay
    currentAudioDelayMs: Int = 0,
    onAudioDelayChanged: (Long) -> Unit = {},

    // SubtitleDelay
    currentSubDelayMs: Int = 0,
    currentSecondaryDelayMs: Int = 0,
    currentSubSpeed: Float = 1f,
    onSubDelayChanged: (Int) -> Unit = {},
    onSecondarySubDelayChanged: (Int) -> Unit = {},
    onSubSpeedChanged: (Float) -> Unit = {},

    // VideoFilters
    filterValues: Map<KitsugiVideoFilters, Int> = emptyMap(),
    onFilterChange: (KitsugiVideoFilters, Int) -> Unit = { _, _ -> },
    onResetAllFilters: () -> Unit = {},

    // SubtitleSettings
    subtitleStyle: SubtitleStyleSettings = SubtitleStyleSettings(),
    onSubtitleStyleChange: (SubtitleStyleSettings) -> Unit = {},

    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = panelShown,
        label = "panels",
        contentAlignment = Alignment.CenterEnd,
        contentKey = { it.name },
        transitionSpec = {
            (fadeIn() + slideInHorizontally { it / 3 })
                .togetherWith(fadeOut() + slideOutHorizontally { it / 2 })
        },
        modifier = modifier,
    ) { currentPanel ->
        when (currentPanel) {
            KitsugiPanels.None -> Box(Modifier.fillMaxHeight())

            KitsugiPanels.AudioDelay -> AudioDelayPanel(
                onDismissRequest = onDismissRequest,
                onDelayChanged = onAudioDelayChanged,
                currentAudioDelayMs = currentAudioDelayMs,
            )

            KitsugiPanels.SubtitleDelay -> SubtitleDelayPanel(
                onDismissRequest = onDismissRequest,
                currentSubtitleDelayMs = currentSubDelayMs,
                currentSecondaryDelayMs = currentSecondaryDelayMs,
                currentSpeed = currentSubSpeed,
                onSubDelayChanged = onSubDelayChanged,
                onSecondaryDelayChanged = onSecondarySubDelayChanged,
                onSpeedChanged = onSubSpeedChanged,
            )

            KitsugiPanels.VideoFilters -> VideoFiltersPanel(
                filterValues = filterValues,
                onFilterChange = onFilterChange,
                onResetAll = onResetAllFilters,
                onDismissRequest = onDismissRequest,
            )

            KitsugiPanels.SubtitleSettings -> SubtitleSettingsPanel(
                subtitleStyle = subtitleStyle,
                onStyleChange = onSubtitleStyleChange,
                onDismissRequest = onDismissRequest,
            )
        }
    }
}
