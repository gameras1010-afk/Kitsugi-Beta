package com.kitsugi.animelist.ui.screens.fullscreen.controls.components

// ─────────────────────────────────────────────────────────────────────────────
// SeekBar — Aniyomi-derived, Kitsugi-themed
// Original: eu.kanade.tachiyomi.ui.player.controls.components.SeekBar
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.screens.fullscreen.controls.LocalPlayerButtonsClickEvent
import dev.vivvvek.seeker.Seeker
import dev.vivvvek.seeker.SeekerDefaults
import dev.vivvvek.seeker.Segment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class IndexedSegment(
    val name: String,
    val start: Float,
    val color: Color = Color.Unspecified,
    val index: Int = 0,
) {
    companion object {
        val Unspecified = IndexedSegment(name = "", start = 0f)
    }

    fun toSegment(): Segment = Segment(name, start, color)
}

@Composable
fun SeekbarWithTimers(
    position: Float,
    duration: Float,
    readAheadValue: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    timersInverted: Pair<Boolean, Boolean>,
    positionTimerOnClick: () -> Unit,
    durationTimerOnCLick: () -> Unit,
    chapters: ImmutableList<Segment>,
    modifier: Modifier = Modifier,
) {
    val clickEvent = LocalPlayerButtonsClickEvent.current
    Row(
        modifier = modifier.height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        VideoTimer(
            value = position,
            isInverted = timersInverted.first,
            onClick = {
                clickEvent()
                positionTimerOnClick()
            },
            modifier = Modifier.width(80.dp),
        )
        Seeker(
            value = position.coerceIn(0f, duration),
            range = 0f..duration,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            readAheadValue = readAheadValue,
            segments = chapters
                .filter { it.start in 0f..duration }
                .let {
                    if (it.isNotEmpty() && it[0].start != 0f) {
                        persistentListOf(Segment("", 0f)) + it
                    } else {
                        it
                    } + it
                },
            modifier = Modifier.weight(1f),
            colors = SeekerDefaults.seekerColors(
                progressColor = MaterialTheme.colorScheme.primary,
                thumbColor = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.3f),
                readAheadColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            ),
        )
        VideoTimer(
            value = if (timersInverted.second) position - duration else duration,
            isInverted = timersInverted.second,
            onClick = {
                clickEvent()
                durationTimerOnCLick()
            },
            modifier = Modifier.width(80.dp),
        )
    }
}

@Composable
fun VideoTimer(
    value: Float,
    isInverted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .wrapContentHeight(Alignment.CenterVertically),
        text = prettyTime(value.toInt(), isInverted),
        color = Color.White,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun prettyTime(seconds: Int, isInverted: Boolean = false): String {
    val abs = kotlin.math.abs(seconds)
    val h = abs / 3600
    val m = (abs % 3600) / 60
    val s = abs % 60
    val prefix = if (isInverted) "-" else ""
    return if (h > 0) {
        "$prefix${h}:%02d:%02d".format(m, s)
    } else {
        "$prefix%d:%02d".format(m, s)
    }
}
