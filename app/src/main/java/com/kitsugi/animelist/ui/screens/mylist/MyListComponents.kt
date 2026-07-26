@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kitsugi.animelist.ui.screens.mylist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DensityMedium
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.components.KitsugiSearchField
import com.kitsugi.animelist.ui.screens.mylist.components.KitsugiMyListSortMenu
import com.kitsugi.animelist.ui.theme.LocalKitsugiColors
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlinx.coroutines.launch

@Composable
fun MyListHeaderSection(
    selectedListLayoutId: String,
    onListLayoutChange: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showSearchField: Boolean,
    onSearchFieldToggle: () -> Unit,
    showFilterPanel: Boolean,
    onFilterPanelToggle: () -> Unit,
    onHideFilters: () -> Unit,
    selectedStatusFilterId: String,
    selectedTypeFilterId: String,
    selectedFavoriteFilterId: String,
    selectedScoreFilterId: String,
    selectedYearFilterId: String,
    selectedExtraFilterId: String,
    selectedSortId: String,
    onStatusFilterChange: (String) -> Unit,
    onTypeFilterChange: (String) -> Unit,
    onFavoriteFilterChange: (String) -> Unit,
    onScoreFilterChange: (String) -> Unit,
    onYearFilterChange: (String) -> Unit,
    onExtraFilterChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    activeTabScrollState: LazyListState,
    accentColor: Color,
    horizontalPadding: Dp,
    hasActiveFilters: Boolean
) {
    val KitsugiColors = LocalKitsugiColors.current
    var showSortMenu by rememberSaveable { mutableStateOf(false) }

    val rawOffset = activeTabScrollState.firstVisibleItemScrollOffset
    val firstVisibleIndex = activeTabScrollState.firstVisibleItemIndex
    val collapseProgress = remember(firstVisibleIndex, rawOffset) {
        if (firstVisibleIndex > 0) 1f
        else (rawOffset.toFloat() / 100f).coerceIn(0f, 1f)
    }
    val headerHeight = 60.dp * (1f - collapseProgress)

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .graphicsLayer {
                    alpha = 1f - collapseProgress
                    translationY = -20.dp.toPx() * collapseProgress
                }
                .clipToBounds()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(start = horizontalPadding, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Listem",
                    color = KitsugiColors.textPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    IconButton(
                        onClick = {
                            val nextLayoutId = when (selectedListLayoutId) {
                                "compact" -> "comfortable"
                                "comfortable" -> "large"
                                "large" -> "grid_2col"
                                "grid_2col" -> "compact"
                                else -> "comfortable"
                            }
                            onListLayoutChange(nextLayoutId)
                        }
                    ) {
                        val layoutIcon = when (selectedListLayoutId) {
                            "compact" -> Icons.Rounded.DensityMedium
                            "comfortable" -> Icons.Rounded.FormatListBulleted
                            "large" -> Icons.Rounded.ViewStream
                            "grid_2col" -> Icons.Rounded.GridView
                            else -> Icons.Rounded.FormatListBulleted
                        }
                        Icon(
                            imageVector = layoutIcon,
                            contentDescription = "Görünüm Değiştir",
                            tint = KitsugiColors.textSecondary
                        )
                    }

                    IconButton(onClick = onSearchFieldToggle) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Arama",
                            tint = if (showSearchField || searchQuery.isNotBlank()) accentColor else KitsugiColors.textSecondary
                        )
                    }

                    Box {
                        IconButton(onClick = onFilterPanelToggle) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = "Filtrele ve Sırala",
                                tint = if (showFilterPanel || hasActiveFilters) accentColor else KitsugiColors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showFilterPanel,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 6.dp)
            ) {
                RichMyListFilterPanel(
                    selectedStatusFilterId = selectedStatusFilterId,
                    selectedTypeFilterId = selectedTypeFilterId,
                    selectedFavoriteFilterId = selectedFavoriteFilterId,
                    selectedScoreFilterId = selectedScoreFilterId,
                    selectedYearFilterId = selectedYearFilterId,
                    selectedExtraFilterId = selectedExtraFilterId,
                    selectedSortId = selectedSortId,
                    onStatusSelected = onStatusFilterChange,
                    onTypeSelected = onTypeFilterChange,
                    onFavoriteSelected = onFavoriteFilterChange,
                    onScoreSelected = onScoreFilterChange,
                    onYearSelected = onYearFilterChange,
                    onExtraSelected = onExtraFilterChange,
                    onSortSelected = onSortChange,
                    onResetFilters = {
                        onStatusFilterChange("all")
                        onTypeFilterChange("all")
                        onFavoriteFilterChange("all")
                        onScoreFilterChange("all")
                        onYearFilterChange("all")
                        onExtraFilterChange("all")
                        onSortChange("newest")
                    },
                    onHideFilters = onHideFilters
                )
            }
        }

        AnimatedVisibility(
            visible = showSearchField || searchQuery.isNotBlank(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KitsugiSearchField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = "Listende ara...",
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val typeItems = listOf(
                        "all" to "Tümü",
                        "anime" to "Anime",
                        "manga" to "Manga",
                        "movie" to "Film",
                        "tvshow" to "Dizi"
                    )
                    typeItems.forEach { (id, label) ->
                        val isSelected = selectedTypeFilterId == id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) accentColor else KitsugiColors.surface)
                                .tvClickable(shape = RoundedCornerShape(12.dp)) {
                                    onTypeFilterChange(id)
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = if (isSelected) "✓ $label" else label,
                                color = if (isSelected) KitsugiColors.background else KitsugiColors.textPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val activeSortTitle = sortOptions.firstOrNull { it.id == selectedSortId }?.title ?: "Son eklenen"
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(KitsugiColors.surface)
                                .tvClickable(shape = RoundedCornerShape(12.dp)) {
                                    showSortMenu = true
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "≡ $activeSortTitle ▾",
                                color = accentColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        KitsugiMyListSortMenu(
                            expanded = showSortMenu,
                            selectedSortId = selectedSortId,
                            onSortSelected = onSortChange,
                            onDismissRequest = { showSortMenu = false }
                        )
                    }

                    if (searchQuery.isNotBlank() || hasActiveFilters) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .tvClickable(shape = RoundedCornerShape(12.dp)) {
                                    onSearchQueryChange("")
                                    onStatusFilterChange("all")
                                    onTypeFilterChange("all")
                                    onFavoriteFilterChange("all")
                                    onScoreFilterChange("all")
                                    onYearFilterChange("all")
                                    onExtraFilterChange("all")
                                    onSortChange("newest")
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Temizle",
                                color = accentColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            color = KitsugiColors.border.copy(alpha = 0.18f),
            thickness = 0.5.dp
        )
    }
}

@Composable
fun MyListTabBar(
    selectedTabIndex: Int,
    onTabIndexChange: (Int) -> Unit,
    visibleEntries: List<MediaEntry>,
    onEntryClick: (MediaEntry) -> Unit,
    onExternalSyncMessage: (String) -> Unit,
    accentColor: Color,
    horizontalPadding: Dp
) {
    val KitsugiColors = LocalKitsugiColors.current
    val tabs = listOf("AniList", "MAL", "Simkl")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(KitsugiColors.surface),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val isSelected = selectedTabIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isSelected) accentColor else KitsugiColors.surface)
                        .tvClickable(shape = RoundedCornerShape(22.dp), onClick = {
                            onTabIndexChange(index)
                        })
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) KitsugiColors.background else KitsugiColors.textMuted,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(KitsugiColors.surface)
                .tvClickable(shape = RoundedCornerShape(14.dp)) {
                    if (visibleEntries.isNotEmpty()) {
                        onEntryClick(visibleEntries.random())
                    } else {
                        onExternalSyncMessage("Gösterilecek bir öğe yok")
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🎲", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun MyListContentPage(
    pageTabIndex: Int,
    selectedTabIndex: Int,
    isConnected: Boolean,
    isSimklSessionExpired: Boolean,
    pageEntries: List<MediaEntry>,
    visibleEntries: List<MediaEntry>,
    groupedVisibleEntries: List<Pair<WatchStatus, List<MediaEntry>>>,
    searchQuery: String,
    selectedStatusFilterId: String,
    selectedListLayoutId: String,
    appSettings: com.kitsugi.animelist.data.settings.AppSettings,
    pageScrollState: LazyListState,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
    onEntryClick: (MediaEntry) -> Unit,
    onIncrementProgress: (MediaEntry) -> Unit,
    onPosterLongClick: (String) -> Unit,
    accentColor: Color,
    horizontalPadding: Dp
) {
    val KitsugiColors = LocalKitsugiColors.current
    val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current
    val coroutineScope = rememberCoroutineScope()
    val pageRefreshState = rememberPullToRefreshState()
    var pageIsRefreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = pageIsRefreshing,
        onRefresh = {
            coroutineScope.launch {
                pageIsRefreshing = true
                onRefresh()
                pageIsRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize(),
        state = pageRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pageRefreshState,
                isRefreshing = pageIsRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = KitsugiColors.surface,
                color = accentColor
            )
        }
    ) {
        if (!isConnected) {
            LazyColumn(
                state = pageScrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
            ) {
                item {
                    MyListNotConnectedState(
                        selectedTabIndex = pageTabIndex,
                        isSimklSessionExpired = isSimklSessionExpired,
                        onLogin = onLogin
                    )
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        } else if (pageEntries.isEmpty()) {
            LazyColumn(
                state = pageScrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
            ) {
                item { MyListSyncPromptState() }
            }
        } else {
            val displayEntries = if (pageTabIndex == selectedTabIndex) visibleEntries else pageEntries
            val displayGrouped = if (pageTabIndex == selectedTabIndex) {
                groupedVisibleEntries
            } else {
                val statusOrder = listOf(
                    WatchStatus.Watching, WatchStatus.Repeating, WatchStatus.Planned,
                    WatchStatus.Paused, WatchStatus.Dropped, WatchStatus.Completed
                )
                statusOrder.map { status ->
                    status to pageEntries.filter { it.status == status }
                }.filter { it.second.isNotEmpty() }
            }

            LazyColumn(
                state = pageScrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
                    .then(if (isTvDevice) Modifier.dpadVerticalFastScroll(pageScrollState) else Modifier),
                verticalArrangement = Arrangement.Top
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${displayEntries.size} sonuç",
                        color = KitsugiColors.textMuted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                    )
                }
                if (displayEntries.isEmpty()) {
                    item {
                        EmptyListResultCard(
                            searchQuery = if (pageTabIndex == selectedTabIndex) searchQuery else "",
                            selectedStatusFilterId = if (pageTabIndex == selectedTabIndex) selectedStatusFilterId else "all",
                            selectedTypeFilterId = "all",
                            selectedFavoriteFilterId = "all",
                            selectedScoreFilterId = "all",
                            selectedYearFilterId = "all",
                            selectedExtraFilterId = "all",
                            selectedSortId = "newest"
                        )
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                } else if (selectedStatusFilterId == "completed" && pageTabIndex == selectedTabIndex) {
                    MyListFlatContent(
                        visibleEntries = displayEntries,
                        selectedListLayoutId = selectedListLayoutId,
                        titleLanguage = appSettings.titleLanguage,
                        scoreFormat = appSettings.scoreFormat,
                        hideScores = appSettings.hideScores,
                        blurAdultMedia = appSettings.blurAdultMedia,
                        onEntryClick = onEntryClick,
                        onIncrementProgress = onIncrementProgress,
                        onPosterLongClick = onPosterLongClick
                    )
                } else {
                    MyListGroupedContent(
                        groupedEntries = displayGrouped,
                        selectedListLayoutId = selectedListLayoutId,
                        titleLanguage = appSettings.titleLanguage,
                        scoreFormat = appSettings.scoreFormat,
                        hideScores = appSettings.hideScores,
                        blurAdultMedia = appSettings.blurAdultMedia,
                        onEntryClick = onEntryClick,
                        onIncrementProgress = onIncrementProgress,
                        onPosterLongClick = onPosterLongClick
                    )
                }
                item { Spacer(modifier = Modifier.height(90.dp)) }
            }
        }
    }
}
