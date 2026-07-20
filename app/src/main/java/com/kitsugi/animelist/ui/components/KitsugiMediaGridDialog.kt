@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.remote.JikanSearchResult
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.components.KitsugiEmptyState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KitsugiMediaGridDialog(
    title: String,
    results: List<JikanSearchResult>,
    alreadyInList: (JikanSearchResult) -> Boolean,
    onItemClick: (JikanSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    val filteredResults = results.filter { result ->
        if (searchQuery.isBlank()) {
            true
        } else {
            val query = searchQuery.trim().lowercase()

            result.title.lowercase().contains(query) ||
                    result.subtitle.lowercase().contains(query) ||
                    result.source.lowercase().contains(query) ||
                    result.year?.toString()?.contains(query) == true ||
                    result.malId.toString().contains(query)
        }
    }

    val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current

    if (isTvDevice) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KitsugiColors.Background)
                    .padding(horizontal = 48.dp, vertical = 32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = onDismiss
                        ) {
                            Text(
                                text = "Kapat",
                                color = KitsugiColors.TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    KitsugiSearchField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                        },
                        placeholder = "Bu rafta ara..."
                    )

                    Text(
                        text = "${filteredResults.size} / ${results.size} içerik",
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (filteredResults.isEmpty()) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                    KitsugiEmptyState(
                                title = "Sonuç bulunamadı",
                                subtitle = if (searchQuery.isBlank()) {
                                    "Bu rafta gösterilecek içerik yok."
                                } else {
                                    "\"$searchQuery\" araması için sonuç bulunamadı."
                                },
                                icon = Icons.Rounded.SearchOff
                            )
                        }
                    } else {
                        val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                        val tvSpec = com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults.rememberTvCenteredSpec()

                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides tvSpec
                        ) {
                            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                                state = gridState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .dpadVerticalFastScroll(gridState),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(
                                    count = filteredResults.size,
                                    key = { index -> filteredResults[index].malId }
                                ) { index ->
                                    val result = filteredResults[index]
                                    KitsugiExploreMediaCard(
                                        result = result,
                                        alreadyInList = alreadyInList(result),
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            onItemClick(result)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = KitsugiColors.Surface,
            titleContentColor = KitsugiColors.TextPrimary,
            textContentColor = KitsugiColors.TextSecondary,
            title = {
                Text(
                    text = title,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 640.dp)
                        .verticalScroll(scrollState)
                ) {
                    KitsugiSearchField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                        },
                        placeholder = "Bu rafta ara..."
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${filteredResults.size} / ${results.size} içerik",
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredResults.isEmpty()) {
                        KitsugiEmptyState(
                            title = "Sonuç bulunamadı",
                            subtitle = if (searchQuery.isBlank()) {
                                "Bu rafta gösterilecek içerik yok."
                            } else {
                                "\"$searchQuery\" araması için sonuç bulunamadı."
                            },
                            icon = Icons.Rounded.SearchOff
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            maxItemsInEachRow = 2
                        ) {
                            filteredResults.forEach { result ->
                                KitsugiExploreMediaCard(
                                    result = result,
                                    alreadyInList = alreadyInList(result),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        onItemClick(result)
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = "Kapat",
                        color = KitsugiColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}