package com.kitsugi.animelist.ui.screens.fullscreen.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.ControlsButton
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlin.math.abs

@Composable
fun MiddlePlayerControls(
    hasPrevious: Boolean,
    onSkipPrevious: () -> Unit,
    isLoading: Boolean,
    isLoadingEpisode: Boolean,
    controlsShown: Boolean,
    areControlsLocked: Boolean,
    showLoadingCircle: Boolean,
    paused: Boolean,
    gestureSeekAmount: Pair<Int, Int>?,
    onPlayPauseClick: () -> Unit,
    hasNext: Boolean,
    onSkipNext: () -> Unit,
    enter: EnterTransition,
    exit: ExitTransition,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        AnimatedVisibility(
            visible = controlsShown && !areControlsLocked,
            enter = enter,
            exit = exit,
        ) {
            if (gestureSeekAmount == null) {
                ControlsButton(
                    icon = Icons.Filled.SkipPrevious,
                    onClick = onSkipPrevious,
                    iconSize = 48.dp,
                    enabled = hasPrevious,
                )
            }
        }

        val interaction = remember { MutableInteractionSource() }
        when {
            gestureSeekAmount != null -> {
                val current = gestureSeekAmount.first
                val diff = gestureSeekAmount.second
                Text(
                    text = "${if (diff >= 0) "+" else "-"}${prettyTime(abs(diff))} [${prettyTime(current + diff)}]",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        shadow = Shadow(Color.Black, blurRadius = 5f),
                    ),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            (isLoading || isLoadingEpisode) && showLoadingCircle -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(96.dp),
                    color = KitsugiColors.AccentBlue,
                    strokeWidth = 6.dp
                )
            }

            else -> {
                AnimatedVisibility(
                    visible = controlsShown && !areControlsLocked,
                    enter = enter,
                    exit = exit,
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interaction,
                                indication = ripple(),
                                onClick = onPlayPauseClick,
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = controlsShown && !areControlsLocked,
            enter = enter,
            exit = exit,
        ) {
            if (gestureSeekAmount == null) {
                ControlsButton(
                    icon = Icons.Filled.SkipNext,
                    onClick = onSkipNext,
                    iconSize = 48.dp,
                    enabled = hasNext,
                )
            }
        }
    }
}

private fun prettyTime(seconds: Int): String {
    val absVal = abs(seconds)
    val h = absVal / 3600
    val m = (absVal % 3600) / 60
    val s = absVal % 60
    return if (h > 0) {
        "${h}:%02d:%02d".format(m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}
