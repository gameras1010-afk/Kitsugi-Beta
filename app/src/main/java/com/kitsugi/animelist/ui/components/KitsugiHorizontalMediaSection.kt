package com.kitsugi.animelist.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.ui.theme.LocalIsTv
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults

@OptIn(ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun KitsugiHorizontalMediaSection(
    title: String,
    results: List<JikanSearchResult>,
    isLoading: Boolean,
    alreadyInList: (JikanSearchResult) -> Boolean,
    onItemClick: (JikanSearchResult) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    titleLanguage: String = "ROMAJI",
    scoreFormat: String = "POINT_10",
    hideScores: Boolean = false,
    blurAdultMedia: Boolean = false
) {
    val accentColor = LocalKitsugiAccent.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (onSeeAllClick != null && results.isNotEmpty()) {
                TextButton(
                    onClick = onSeeAllClick
                ) {
                    Text(
                        text = "Tümünü Gör",
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when {
            isLoading && results.isEmpty() -> {
                // KitsugiShimmer.kt'deki animasyonlu shimmer satırını kullan
                KitsugiShimmerMediaRow(cardCount = 5)
            }

            results.isEmpty() -> {
                Text(
                    text = "Gösterilecek içerik yok.",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> {
                val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current
                if (isTvDevice) {
                    val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
                    val lastFocusedIndex = remember(results) { mutableStateOf(0) }
                    val focusRequesters = remember(results) { mutableMapOf<Int, FocusRequester>() }
                    val rowFocusRequester = remember { FocusRequester() }
                    CompositionLocalProvider(LocalBringIntoViewSpec provides tvSpec) {
                        LazyRow(
                            state = androidx.compose.foundation.lazy.rememberLazyListState(),
                            modifier = Modifier
                                .focusRequester(rowFocusRequester)
                                .focusRestorer {
                                    focusRequesters[lastFocusedIndex.value] ?: FocusRequester.Default
                                }
                                .focusGroup(),
                            contentPadding = PaddingValues(
                                horizontal = 20.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(results) { index, result ->
                                val requester = focusRequesters.getOrPut(index) { FocusRequester() }
                                val cardWidth = if (isLandscape) 320.dp else 180.dp
                                KitsugiExploreMediaCard(
                                    result = result,
                                    alreadyInList = alreadyInList(result),
                                    modifier = Modifier
                                        .width(cardWidth)
                                        .focusRequester(requester)
                                        .onFocusChanged { state ->
                                            if (state.isFocused) {
                                                lastFocusedIndex.value = index
                                            }
                                        },
                                    onClick = { onItemClick(result) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia,
                                    forceVertical = false
                                )
                            }
                        }
                    }
                } else {
                    LazyRow(
                        state = androidx.compose.foundation.lazy.rememberLazyListState(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = results,
                            key = { index, result -> "${result.source}_${result.malId ?: result.tmdbId ?: 0}_$index" }
                        ) { _, result ->
                            val cardWidth = if (isLandscape) 320.dp else 180.dp
                            KitsugiExploreMediaCard(
                                result = result,
                                alreadyInList = alreadyInList(result),
                                modifier = Modifier.width(cardWidth),
                                onClick = {
                                    onItemClick(result)
                                },
                                titleLanguage = titleLanguage,
                                scoreFormat = scoreFormat,
                                hideScores = hideScores,
                                blurAdultMedia = blurAdultMedia,
                                forceVertical = false
                            )
                        }
                    }
                }
            }
        }
    }
}

// Eski statik LoadingSkeletonRow ve SkeletonMediaCard kaldırıldı.
// Animasyonlu versiyonları KitsugiShimmer.kt'de tanımlanmıştır:
//   - KitsugiShimmerMediaRow()  → yatay satır
//   - KitsugiShimmerMediaCard() → tek kart