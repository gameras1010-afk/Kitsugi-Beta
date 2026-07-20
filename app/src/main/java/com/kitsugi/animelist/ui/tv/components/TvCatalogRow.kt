package com.kitsugi.animelist.ui.tv.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kitsugi.animelist.R
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.tv.design.TvTheme
import com.kitsugi.animelist.ui.utils.StableList
import com.kitsugi.animelist.ui.utils.dpadRepeatThrottle
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlin.math.abs

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TvCatalogRow(
    title: String,
    items: StableList<JikanSearchResult>,
    listState: LazyListState,
    entryFocusRequester: FocusRequester,
    savedFocusedItemKey: String?,
    onItemFocused: (JikanSearchResult) -> Unit,
    onItemClick: (JikanSearchResult) -> Unit,
    modifier: Modifier = Modifier,
    isContinueWatching: Boolean = false,
    onSeeAllClick: (() -> Unit)? = null,
    shimmerOffsetState: State<Float>? = null
) {
    val density = LocalDensity.current
    val rowTokens = TvTheme.components.row
    val posterCardTokens = TvTheme.components.posterCard
    val cwCardTokens = TvTheme.components.continueWatchingCard

    val bringIntoViewSpec = remember(density) {
        val parentStartOffsetPx = with(density) { 52.dp.roundToPx() }
        object : BringIntoViewSpec {
            override val scrollAnimationSpec: AnimationSpec<Float> = tween(300)

            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float {
                val childSize = abs(size)
                val childSmallerThanParent = childSize <= containerSize
                val initialTarget = parentStartOffsetPx.toFloat()
                val spaceAvailable = containerSize - initialTarget

                val targetForLeadingEdge =
                    if (childSmallerThanParent && spaceAvailable < childSize) {
                        containerSize - childSize
                    } else {
                        initialTarget
                    }

                return offset - targetForLeadingEdge
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                start = rowTokens.horizontalPadding,
                bottom = rowTokens.titleBottomSpacing,
                top = rowTokens.verticalPadding
            )
        )

        CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoViewSpec) {
            LaunchedEffect(items, savedFocusedItemKey) {
                if (savedFocusedItemKey != null) {
                    val index = items.list.indexOfFirst { "${it.source}_${it.malId}" == savedFocusedItemKey }
                    if (index >= 0) {
                        listState.scrollToItem(index)
                    }
                }
            }

            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(entryFocusRequester)
                    .focusGroup()
                    .focusRestorer()
                    .dpadRepeatThrottle(horizontalGateMs = 90L, verticalGateMs = Long.MAX_VALUE),
                contentPadding = PaddingValues(
                    horizontal = rowTokens.horizontalPadding,
                    vertical = 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(rowTokens.itemSpacing)
            ) {
                itemsIndexed(items.list) { index, item ->
                    val isInitialFocused = savedFocusedItemKey != null && "${item.source}_${item.malId}" == savedFocusedItemKey
                    if (isContinueWatching) {
                        TvContinueWatchingCard(
                            item = item,
                            isInitialFocused = isInitialFocused,
                            onFocused = { onItemFocused(item) },
                            onClick = { onItemClick(item) },
                            shimmerOffsetState = shimmerOffsetState
                        )
                    } else {
                        TvContentCard(
                            item = item,
                            isInitialFocused = isInitialFocused,
                            onFocused = { onItemFocused(item) },
                            onClick = { onItemClick(item) },
                            shimmerOffsetState = shimmerOffsetState
                        )
                    }
                }

                if (onSeeAllClick != null && items.list.isNotEmpty()) {
                    item {
                        val cardWidth = if (isContinueWatching) cwCardTokens.width else posterCardTokens.width
                        val cardHeight = if (isContinueWatching) cwCardTokens.height else posterCardTokens.height
                        val cardRadius = if (isContinueWatching) cwCardTokens.cornerRadius else posterCardTokens.cornerRadius
                        val scaleFocused = if (isContinueWatching) cwCardTokens.focusedScale else posterCardTokens.focusedScale
                        val borderWidth = if (isContinueWatching) cwCardTokens.focusedBorderWidth else posterCardTokens.focusedBorderWidth

                        val seeAllCardShape = RoundedCornerShape(cardRadius)

                        Box(
                            modifier = Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                                .tvClickable(
                                    shape = seeAllCardShape,
                                    scaleFocused = scaleFocused,
                                    borderWidth = borderWidth,
                                    onClick = onSeeAllClick
                                )
                                .clip(seeAllCardShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = stringResource(R.string.action_see_all),
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.action_see_all),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
