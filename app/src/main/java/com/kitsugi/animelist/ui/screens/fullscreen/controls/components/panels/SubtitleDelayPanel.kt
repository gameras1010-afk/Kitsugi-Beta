package com.kitsugi.animelist.ui.screens.fullscreen.controls.components.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.OutlinedNumericChooser
import kotlin.math.round
import kotlin.math.roundToInt

enum class SubtitleDelayType(val label: String) {
    Primary("Birincil"),
    Secondary("İkincil"),
    Both("İkisi Birden"),
}

@Composable
fun SubtitleDelayPanel(
    onDismissRequest: () -> Unit,
    currentSubtitleDelayMs: Int,
    currentSecondaryDelayMs: Int,
    currentSpeed: Float,
    onSubDelayChanged: (Int) -> Unit,
    onSecondaryDelayChanged: (Int) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        val delayControlCard = createRef()

        var affectedSubtitle by remember { mutableStateOf(SubtitleDelayType.Primary) }
        var delay by remember { mutableIntStateOf(currentSubtitleDelayMs) }
        var secondaryDelay by remember { mutableIntStateOf(currentSecondaryDelayMs) }
        var speed by remember { mutableFloatStateOf(currentSpeed) }

        LaunchedEffect(speed) {
            if (speed in 0.1f..10f) onSpeedChanged(speed)
        }

        LaunchedEffect(delay, secondaryDelay) {
            when (affectedSubtitle) {
                SubtitleDelayType.Primary -> onSubDelayChanged(delay)
                SubtitleDelayType.Secondary -> onSecondaryDelayChanged(secondaryDelay)
                SubtitleDelayType.Both -> {
                    onSubDelayChanged(delay)
                    onSecondaryDelayChanged(delay)
                }
            }
        }

        DelayCard(
            delay = if (affectedSubtitle == SubtitleDelayType.Secondary) secondaryDelay else delay,
            onDelayChange = {
                if (affectedSubtitle == SubtitleDelayType.Secondary) secondaryDelay = it else delay = it
            },
            onApply = { /* saved externally */ },
            onReset = {
                delay = 0
                secondaryDelay = 0
                speed = 1f
            },
            title = {
                SubtitleDelayTitle(
                    affectedSubtitle = affectedSubtitle,
                    onClose = onDismissRequest,
                    onTypeChange = { affectedSubtitle = it },
                )
            },
            extraSettings = {
                if (affectedSubtitle == SubtitleDelayType.Primary) {
                    OutlinedNumericChooser(
                        label = { Text("Hız", color = Color.White) },
                        value = speed,
                        onChange = { speed = round(it * 1000) / 1000f },
                        max = 10f,
                        step = .05f,
                        min = .1f,
                    )
                }
            },
            delayType = DelayType.Subtitle,
            modifier = Modifier.constrainAs(delayControlCard) {
                linkTo(parent.top, parent.bottom, bias = 0.8f)
                end.linkTo(parent.end)
            },
        )
    }
}

@Composable
private fun SubtitleDelayTitle(
    affectedSubtitle: SubtitleDelayType,
    onClose: () -> Unit,
    onTypeChange: (SubtitleDelayType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text("Alt yazı gecikmesi", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        var showDropDown by remember { mutableStateOf(false) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { showDropDown = true }
        ) {
            Text(
                affectedSubtitle.label,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp)
            )
            Icon(Icons.Default.ArrowDropDown, null, tint = Color.White)
            DropdownMenu(expanded = showDropDown, onDismissRequest = { showDropDown = false }) {
                SubtitleDelayType.entries.forEach {
                    DropdownMenuItem(
                        text = { Text(it.label) },
                        onClick = { onTypeChange(it); showDropDown = false },
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClose) {
            Icon(Icons.Default.Close, null, modifier = Modifier.size(32.dp), tint = Color.White)
        }
    }
}
