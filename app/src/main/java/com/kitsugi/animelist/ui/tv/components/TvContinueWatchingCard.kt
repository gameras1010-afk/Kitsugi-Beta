package com.kitsugi.animelist.ui.tv.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.tv.design.TvTheme
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvContinueWatchingCard(
    item: JikanSearchResult,
    isInitialFocused: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shimmerOffsetState: State<Float>? = null
) {
    val focusRequester = remember { FocusRequester() }
    val focusState = remember { mutableStateOf(false) }
    val isFocused by remember { derivedStateOf { focusState.value } }

    val cwCardTokens = TvTheme.components.continueWatchingCard
    val cardShape = RoundedCornerShape(cwCardTokens.cornerRadius)
    val accentColor = LocalKitsugiAccent.current

    // Calculate a stable progress percentage and remaining minutes based on malId
    val progressFraction = remember(item.malId) { (((item.malId % 6) + 3) / 10f).coerceIn(0.3f, 0.9f) }
    val remainingMinutes = remember(item.malId) { (item.malId % 25) + 5 }
    val badgeText = remember(progressFraction, remainingMinutes) {
        "-%d dk (%d%%)".format(remainingMinutes, (progressFraction * 100).toInt())
    }

    LaunchedEffect(isInitialFocused) {
        if (isInitialFocused) {
            focusRequester.requestFocusAfterFrames(frames = 2)
        }
    }

    Column(
        modifier = modifier.width(cwCardTokens.width)
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .width(cwCardTokens.width)
                .height(cwCardTokens.height)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    focusState.value = state.isFocused
                    if (state.isFocused) {
                        onFocused()
                    }
                },
            shape = CardDefaults.shape(cardShape),
            scale = CardDefaults.scale(focusedScale = cwCardTokens.focusedScale),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(cwCardTokens.focusedBorderWidth, accentColor),
                    shape = cardShape
                )
            ),
            colors = CardDefaults.colors(
                containerColor = KitsugiColors.Surface
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // landscape model is backdropUrl or imageUrl
                val imageModel = item.backdropUrl ?: item.imageUrl
                TvImage(
                    model = imageModel,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = KitsugiColors.Surface
                )

                // Progress/Remaining time badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                // Progress bar at the bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(1.5.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .height(3.dp)
                            .background(TvTheme.colorScheme.primary, RoundedCornerShape(1.5.dp))
                    )
                }
            }
        }

        TvFocusMarqueeText(
            text = item.title,
            focused = isFocused,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 2.dp, end = 2.dp)
        )
    }
}
