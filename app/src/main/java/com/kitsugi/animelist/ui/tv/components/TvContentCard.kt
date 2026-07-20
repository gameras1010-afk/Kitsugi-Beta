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
fun TvContentCard(
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

    val posterCardTokens = TvTheme.components.posterCard
    val cardShape = RoundedCornerShape(posterCardTokens.cornerRadius)
    val accentColor = LocalKitsugiAccent.current

    LaunchedEffect(isInitialFocused) {
        if (isInitialFocused) {
            focusRequester.requestFocusAfterFrames(frames = 2)
        }
    }

    Column(
        modifier = modifier.width(posterCardTokens.width)
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .width(posterCardTokens.width)
                .height(posterCardTokens.height)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    focusState.value = state.isFocused
                    if (state.isFocused) {
                        onFocused()
                    }
                },
            shape = CardDefaults.shape(cardShape),
            scale = CardDefaults.scale(focusedScale = posterCardTokens.focusedScale),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(posterCardTokens.focusedBorderWidth, accentColor),
                    shape = cardShape
                )
            ),
            colors = CardDefaults.colors(
                containerColor = KitsugiColors.Surface
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                TvImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = KitsugiColors.Surface
                )

                // Score badge
                item.score?.let { score ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "★ $score",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFD700)
                        )
                    }
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
