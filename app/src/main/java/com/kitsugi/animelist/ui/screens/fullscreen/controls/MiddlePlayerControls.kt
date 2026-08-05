package com.kitsugi.animelist.ui.screens.fullscreen.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Spacer
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

private enum class MiddleControlState {
    Seek,
    Loading,
    PlayPause,
    Hidden
}

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
    val currentState = remember(gestureSeekAmount, showLoadingCircle, controlsShown, areControlsLocked) {
        when {
            gestureSeekAmount != null -> MiddleControlState.Seek
            showLoadingCircle -> MiddleControlState.Loading
            controlsShown && !areControlsLocked -> MiddleControlState.PlayPause
            else -> MiddleControlState.Hidden
        }
    }

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
        AnimatedContent(
            targetState = currentState,
            transitionSpec = {
                (fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) +
                 scaleIn(initialScale = 0.8f, animationSpec = androidx.compose.animation.core.tween(220)))
                    .togetherWith(
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(220)) +
                        scaleOut(targetScale = 0.8f, animationSpec = androidx.compose.animation.core.tween(220))
                    )
            },
            label = "MiddleControlsTransition"
        ) { state ->
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    MiddleControlState.Seek -> {
                        val current = gestureSeekAmount?.first ?: 0
                        val diff = gestureSeekAmount?.second ?: 0
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
                    MiddleControlState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp),
                            color = KitsugiColors.AccentBlue,
                            strokeWidth = 4.dp
                        )
                    }
                    MiddleControlState.PlayPause -> {
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
                    MiddleControlState.Hidden -> {
                        Spacer(modifier = Modifier.size(96.dp))
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
