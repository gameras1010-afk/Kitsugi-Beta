@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kitsugi.animelist.ui.screens.mylist

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.zIndex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kitsugi.animelist.ui.screens.mylist.components.KitsugiListStatusBottomSheet
import com.kitsugi.animelist.ui.screens.mylist.components.KitsugiMyListSortMenu
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.data.remote.matches
import com.kitsugi.animelist.ui.components.KitsugiApiSearchDialog
import com.kitsugi.animelist.ui.components.KitsugiConfirmDialog
import com.kitsugi.animelist.ui.components.KitsugiInfoDialog
import com.kitsugi.animelist.ui.components.KitsugiImagePreviewDialog
import com.kitsugi.animelist.ui.components.KitsugiMediaEntryEditorDialog
import com.kitsugi.animelist.ui.components.KitsugiProfileHeaderCard
import com.kitsugi.animelist.ui.components.KitsugiSearchField
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiColors
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import kotlinx.coroutines.launch

@Composable
fun MyListScreen(
    selectedListLayoutId: String,
    showAdultContent: Boolean,
    appSettings: com.kitsugi.animelist.data.settings.AppSettings,
    searchQuery: String,
    selectedStatusFilterId: String,
    selectedTypeFilterId: String,
    selectedFavoriteFilterId: String,
    selectedScoreFilterId: String,
    selectedYearFilterId: String,
    selectedExtraFilterId: String,
    selectedSortId: String,
    initialScrollIndex: Int,
    initialScrollOffset: Int,
    selectedTabIndex: Int,
    onTabIndexChange: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onTypeFilterChange: (String) -> Unit,
    onFavoriteFilterChange: (String) -> Unit,
    onScoreFilterChange: (String) -> Unit,
    onYearFilterChange: (String) -> Unit,
    onExtraFilterChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onScrollPositionChange: (index: Int, offset: Int) -> Unit,
    onExternalSyncMessage: (String) -> Unit,
    isAniListConnected: Boolean,
    isMalConnected: Boolean,
    isSimklConnected: Boolean,
    isSimklSessionExpired: Boolean,
    onLoginAniList: () -> Unit,
    onLoginMal: () -> Unit,
    onLoginSimkl: () -> Unit,
    onSyncAniList: () -> Unit,
    onSyncMal: () -> Unit,
    onSyncSimkl: () -> Unit,
    onEntryClick: (MediaEntry) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val KitsugiColors = LocalKitsugiColors.current

    val viewModel: MyListViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    // ViewModel artık init{} içinde hazır — sadece sync mesajlarını dinle
    LaunchedEffect(Unit) {
        viewModel.syncMessages.collect { message ->
            onExternalSyncMessage(message)
        }
    }

    val entries by viewModel.entriesFlow.collectAsState()

    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialScrollIndex,
        initialFirstVisibleItemScrollOffset = initialScrollOffset
    )

    var isScrollRestored by rememberSaveable(initialScrollIndex, initialScrollOffset) {
        mutableStateOf(initialScrollIndex == 0 && initialScrollOffset == 0)
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
        }.collect { position ->
            if (isScrollRestored && entries.isNotEmpty()) {
                onScrollPositionChange(
                    position.first,
                    position.second
                )
            }
        }
    }

    LaunchedEffect(entries) {
        if (!isScrollRestored) {
            if (entries.isNotEmpty()) {
                if (initialScrollIndex > 0 || initialScrollOffset > 0) {
                    try {
                        lazyListState.scrollToItem(initialScrollIndex, initialScrollOffset)
                    } catch (e: Exception) {
                        // ignore out of bounds
                    }
                }
                isScrollRestored = true
            } else {
                kotlinx.coroutines.delay(100)
                if (entries.isEmpty()) {
                    isScrollRestored = true
                }
            }
        }
    }

    var showAddDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var activeZoomImageUrl by remember { mutableStateOf<String?>(null) }
    var activeZoomTitle by remember { mutableStateOf("") }

    var showApiSearchDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showStatusBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by rememberSaveable { mutableStateOf(false) }
    var showSearchField by rememberSaveable { mutableStateOf(false) }

    var isFabVisible by remember { mutableStateOf(true) }
    var prevIndex by remember { mutableIntStateOf(0) }
    var prevOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index == 0 && offset < 40) {
                    isFabVisible = true
                } else if (index > prevIndex || (index == prevIndex && offset > prevOffset + 15)) {
                    isFabVisible = false
                } else if (index < prevIndex || (index == prevIndex && offset < prevOffset - 15)) {
                    isFabVisible = true
                }
                prevIndex = index
                prevOffset = offset
            }
    }

    var duplicateMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var editingEntry by remember {
        mutableStateOf<MediaEntry?>(null)
    }

    var deletingEntry by remember {
        mutableStateOf<MediaEntry?>(null)
    }

    var showDetailedStats by rememberSaveable {
        mutableStateOf(false)
    }

    val entriesAfterAdultFilter = entries.filter { entry ->
        showAdultContent || !entry.isAdult
    }

    val selectedTabEntries = remember(entriesAfterAdultFilter, selectedTabIndex) {
        entriesAfterAdultFilter.filter { entry ->
            when (selectedTabIndex) {
                0 -> entry.source == "anilist"
                1 -> entry.source == "mal" || entry.source == "jikan"
                else -> entry.source == "simkl"
            }
        }
    }

    val selectedStatus = statusFilters.firstOrNull {
        it.id == selectedStatusFilterId
    }?.status

    val selectedType = typeFilters.firstOrNull {
        it.id == selectedTypeFilterId
    }?.type

    val favoritesOnly = selectedFavoriteFilterId == "favorites"

    val filteredEntries = selectedTabEntries
        .filter { entry ->
            if (selectedStatus == null) true else entry.status == selectedStatus
        }
        .filter { entry ->
            if (selectedType == null) true else entry.type == selectedType
        }
        .filter { entry ->
            if (favoritesOnly) entry.isFavorite else true
        }
        .filter { entry ->
            when (selectedScoreFilterId) {
                "high" -> (entry.score ?: 0) >= 8
                "mid" -> (entry.score ?: 0) in 5..7
                "low" -> (entry.score ?: 0) in 1..4
                "unrated" -> entry.score == null || entry.score == 0
                else -> true
            }
        }
        .filter { entry ->
            when (selectedYearFilterId) {
                "new" -> (entry.year ?: 0) >= 2025
                "2020s" -> (entry.year ?: 0) in 2020..2024
                "2010s" -> (entry.year ?: 0) in 2010..2019
                "2000s" -> (entry.year ?: 0) in 2000..2009
                "classic" -> (entry.year ?: 0) in 1..1999
                else -> true
            }
        }
        .filter { entry ->
            when (selectedExtraFilterId) {
                "repeating" -> entry.isRepeating
                "private" -> entry.isPrivate
                "ongoing" -> entry.status != WatchStatus.Completed && entry.status != WatchStatus.Dropped
                else -> true
            }
        }
        .filter { entry ->
            if (searchQuery.isBlank()) {
                true
            } else {
                val query = searchQuery.trim().lowercase()
                entry.title.lowercase().contains(query) ||
                    entry.subtitle.lowercase().contains(query) ||
                    entry.type.name.lowercase().contains(query) ||
                    entry.status.label.lowercase().contains(query) ||
                    entry.source.lowercase().contains(query) ||
                    entry.year?.toString()?.contains(query) == true ||
                    entry.malId?.toString()?.contains(query) == true
            }
        }

    val visibleEntries = applySort(
        entries = filteredEntries,
        sortId = selectedSortId
    )

    val listStats = remember(selectedTabEntries) {
        ListStats.from(selectedTabEntries)
    }

    fun incrementEntryProgress(entry: MediaEntry) {
        viewModel.incrementEntryProgress(entry)
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val horizontalPadding = if (isLandscape) 12.dp else 20.dp

    val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current
    val accentColor = com.kitsugi.animelist.ui.theme.LocalKitsugiAccent.current

    var isListRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
        CompositionLocalProvider(
            LocalBringIntoViewSpec provides if (isTvDevice) tvSpec else LocalBringIntoViewSpec.current
        ) {
            PullToRefreshBox(
                isRefreshing = isListRefreshing,
                onRefresh = {
                    coroutineScope.launch {
                        isListRefreshing = true
                        when (selectedTabIndex) {
                            0 -> onSyncAniList()
                            1 -> onSyncMal()
                            else -> onSyncSimkl()
                        }
                        isListRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize(),
                state = pullRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullRefreshState,
                        isRefreshing = isListRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = KitsugiColors.surface,
                        color = accentColor
                    )
                }
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                        .then(if (isTvDevice) Modifier.dpadVerticalFastScroll(lazyListState) else Modifier),
                    verticalArrangement = Arrangement.Top
                ) {
        item {
            Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(onClick = { showSearchField = !showSearchField }) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Arama",
                            tint = if (showSearchField || searchQuery.isNotBlank()) accentColor else KitsugiColors.textSecondary
                        )
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Sort,
                                contentDescription = "Sıralama",
                                tint = accentColor
                            )
                        }

                        KitsugiMyListSortMenu(
                            expanded = showSortMenu,
                            selectedSortId = selectedSortId,
                            onSortSelected = onSortChange,
                            onDismissRequest = { showSortMenu = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        item {
            val currentProfileName = when (selectedTabIndex) {
                0 -> {
                    if (isAniListConnected && appSettings.anilistUsername.isNotBlank()) {
                        appSettings.anilistUsername
                    } else {
                        "AniList Profilim"
                    }
                }
                1 -> {
                    if (isMalConnected && appSettings.malUsername.isNotBlank()) {
                        appSettings.malUsername
                    } else {
                        "MAL Profilim"
                    }
                }
                else -> {
                    if (isSimklConnected && appSettings.simklUsername.isNotBlank()) {
                        appSettings.simklUsername
                    } else {
                        "Simkl Profilim"
                    }
                }
            }

            val currentProfileImage = when (selectedTabIndex) {
                0 -> appSettings.anilistProfileImageUri
                1 -> appSettings.malProfileImageUri
                else -> appSettings.simklProfileImageUri
            }

            val currentBannerImage = when (selectedTabIndex) {
                0 -> appSettings.anilistBannerImageUri
                1 -> appSettings.malBannerImageUri
                else -> appSettings.simklBannerImageUri
            }

            val currentListTitle = when (selectedTabIndex) {
                0 -> "AniList Kütüphanesi"
                1 -> "MyAnimeList Kütüphanesi"
                else -> "Simkl Kütüphanesi"
            }

            val currentPlatformName = when (selectedTabIndex) {
                0 -> "AniList"
                1 -> "MyAnimeList"
                else -> "Simkl"
            }

            val currentUsername = when (selectedTabIndex) {
                0 -> appSettings.anilistUsername
                1 -> appSettings.malUsername
                else -> appSettings.simklUsername
            }

            KitsugiProfileHeaderCard(
                profileName = currentProfileName,
                listTitle = currentListTitle,
                anilistUsername = currentUsername,
                profileImageUri = currentProfileImage,
                bannerImageUri = currentBannerImage,
                totalCount = listStats.totalCount,
                favoriteCount = listStats.favoriteCount,
                averageScoreText = listStats.averageScoreText,
                platformName = currentPlatformName,
                onSettingsClick = onSettingsClick
            )

            Spacer(modifier = Modifier.height(18.dp))
        }

        item {
            ListStatsHeader(
                stats = listStats,
                expanded = showDetailedStats,
                onToggleExpanded = {
                    showDetailedStats = !showDetailedStats
                }
            )

            Spacer(modifier = Modifier.height(18.dp))
        }

        stickyHeader(key = "platform_tabs") {
            val tabs = listOf("AniList", "MAL", "Simkl")

            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                color = KitsugiColors.background
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                    .background(
                                        if (isSelected) accentColor else KitsugiColors.surface
                                    )
                                    .tvClickable(shape = RoundedCornerShape(22.dp), onClick = { onTabIndexChange(index) })
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
                }
            }
        }

        val isConnected = when (selectedTabIndex) {
            0 -> isAniListConnected
            1 -> isMalConnected
            else -> isSimklConnected
        }

        if (!isConnected) {
            item {
                MyListNotConnectedState(
                    selectedTabIndex = selectedTabIndex,
                    isSimklSessionExpired = isSimklSessionExpired,
                    onLogin = {
                        when (selectedTabIndex) {
                            0 -> onLoginAniList()
                            1 -> onLoginMal()
                            else -> onLoginSimkl()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(90.dp))
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AddActionButton(
                        title = "API ile Ara",
                        primary = true,
                        modifier = Modifier.weight(1.2f),
                        onClick = { showApiSearchDialog = true }
                    )

                    AddActionButton(
                        title = "Senkronize Et",
                        primary = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (selectedTabIndex) {
                                0 -> onSyncAniList()
                                1 -> onSyncMal()
                                else -> onSyncSimkl()
                            }
                        }
                    )

                    AddActionButton(
                        title = "Manuel Ekle",
                        primary = false,
                        modifier = Modifier.weight(1f),
                        onClick = { showAddDialog = true }
                    )

                    AddActionButton(
                        title = "🎲",
                        primary = false,
                        modifier = Modifier.weight(0.5f),
                        onClick = {
                            if (visibleEntries.isNotEmpty()) {
                                val randomEntry = visibleEntries.random()
                                onEntryClick(randomEntry)
                            } else {
                                onExternalSyncMessage("Gösterilecek bir öğe yok")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            item {
                KitsugiSearchField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = "Listende ara..."
                )

                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                var isFiltersExpanded by rememberSaveable { mutableStateOf(false) }

                val activeStatusTitle = statusFilters.firstOrNull { it.id == selectedStatusFilterId }?.title ?: "Tümü"
                val activeTypeTitle = typeFilters.firstOrNull { it.id == selectedTypeFilterId }?.title ?: "Tümü"
                val activeFavoriteTitle = favoriteFilters.firstOrNull { it.id == selectedFavoriteFilterId }?.title ?: "Tümü"
                val activeScoreTitle = scoreFilters.firstOrNull { it.id == selectedScoreFilterId }?.title ?: "Tümü"
                val activeYearTitle = yearFilters.firstOrNull { it.id == selectedYearFilterId }?.title ?: "Tümü"
                val activeExtraTitle = extraFilters.firstOrNull { it.id == selectedExtraFilterId }?.title ?: "Tümü"
                val activeSortTitle = sortOptions.firstOrNull { it.id == selectedSortId }?.title ?: "Son eklenen"

                val summary = listOf(
                    if (activeStatusTitle != "Tümü") "Durum: $activeStatusTitle" else null,
                    if (activeTypeTitle != "Tümü") "Tür: $activeTypeTitle" else null,
                    if (activeFavoriteTitle != "Tümü") activeFavoriteTitle else null,
                    if (activeScoreTitle != "Tümü") "Puan: $activeScoreTitle" else null,
                    if (activeYearTitle != "Tümü") activeYearTitle else null,
                    if (activeExtraTitle != "Tümü") activeExtraTitle else null,
                    activeSortTitle
                ).filterNotNull().joinToString("  •  ")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(KitsugiColors.surface)
                        .tvClickable(shape = RoundedCornerShape(16.dp), onClick = { isFiltersExpanded = !isFiltersExpanded })
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Filtrele ve Sırala",
                                    color = KitsugiColors.textPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = summary,
                                    color = KitsugiColors.textMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isFiltersExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = KitsugiColors.textMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = isFiltersExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
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
                        },
                        onHideFilters = {
                            isFiltersExpanded = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            if (selectedTabEntries.isEmpty()) {
                item {
                    MyListSyncPromptState()
                }
            } else if (visibleEntries.isEmpty()) {
                item {
                    EmptyListResultCard(
                        searchQuery = searchQuery,
                        selectedStatusFilterId = selectedStatusFilterId,
                        selectedTypeFilterId = selectedTypeFilterId,
                        selectedFavoriteFilterId = selectedFavoriteFilterId,
                        selectedScoreFilterId = selectedScoreFilterId,
                        selectedYearFilterId = selectedYearFilterId,
                        selectedExtraFilterId = selectedExtraFilterId,
                        selectedSortId = selectedSortId
                    )
                    Spacer(modifier = Modifier.height(90.dp))
                }
            } else {
                item {
                    Text(
                        text = "${visibleEntries.size} sonuç",
                        color = KitsugiColors.textMuted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                    )
                }

                if (selectedStatusFilterId == "completed") {
                    MyListFlatContent(
                        visibleEntries = visibleEntries,
                        selectedListLayoutId = selectedListLayoutId,
                        titleLanguage = appSettings.titleLanguage,
                        scoreFormat = appSettings.scoreFormat,
                        hideScores = appSettings.hideScores,
                        blurAdultMedia = appSettings.blurAdultMedia,
                        onEntryClick = onEntryClick,
                        onIncrementProgress = { incrementEntryProgress(it) },
                        onPosterLongClick = { imageUrl ->
                            activeZoomImageUrl = imageUrl
                            activeZoomTitle = visibleEntries.find { it.imageUrl == imageUrl }?.title ?: ""
                        }
                    )
                } else {
                    MyListGroupedContent(
                        visibleEntries = visibleEntries,
                        selectedListLayoutId = selectedListLayoutId,
                        titleLanguage = appSettings.titleLanguage,
                        scoreFormat = appSettings.scoreFormat,
                        hideScores = appSettings.hideScores,
                        blurAdultMedia = appSettings.blurAdultMedia,
                        onEntryClick = onEntryClick,
                        onIncrementProgress = { incrementEntryProgress(it) },
                        onPosterLongClick = { imageUrl ->
                            activeZoomImageUrl = imageUrl
                            activeZoomTitle = visibleEntries.find { it.imageUrl == imageUrl }?.title ?: ""
                        }
                    )
                }
            }
        }
    } // end LazyColumn

        // ── Scroll-aware floating category button (inside Box so .align works) ──
        val isTv = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current
        if (!isTv) {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 76.dp, end = 20.dp)
                    .zIndex(10f)
            ) {
                val activeStatusLabel = statusFilters.firstOrNull { it.id == selectedStatusFilterId }?.title
                    ?: if (selectedStatusFilterId == "adult") "Yetişkin"
                    else if (selectedStatusFilterId == "favorites") "Favoriler"
                    else "Tümü"

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor)
                        .tvClickable(shape = RoundedCornerShape(999.dp), onClick = { showStatusBottomSheet = true })
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.FormatListBulleted,
                            contentDescription = "Kategori",
                            tint = KitsugiColors.background,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeStatusLabel,
                            color = KitsugiColors.background,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
}

    if (showStatusBottomSheet) {
        KitsugiListStatusBottomSheet(
            entries = selectedTabEntries,
            selectedStatusFilterId = selectedStatusFilterId,
            showAdultContent = showAdultContent,
            onStatusSelected = { newStatusId ->
                onStatusFilterChange(newStatusId)
                showStatusBottomSheet = false
            },
            onDismissRequest = {
                showStatusBottomSheet = false
            }
        )
    }


    if (showAddDialog) {
        KitsugiMediaEntryEditorDialog(
            source = when (selectedTabIndex) {
                0 -> "anilist"
                1 -> "mal"
                else -> "simkl"
            },
            onDismiss = {
                showAddDialog = false
            },
            onConfirm = { title, subtitle, type, status, isAdult, progress, total, score, isFavorite, startDate, endDate, notes, tags, priority, isRepeating, repeatCount, repeatValue, volumeProgress, isPrivate, isHiddenFromStatusLists ->
                val newEntry = MediaEntry(
                    id = 0,
                    title = title,
                    subtitle = if (subtitle.isBlank()) {
                        when (selectedTabIndex) {
                            0 -> "Manuel AniList Kaydı"
                            1 -> "Manuel MAL Kaydı"
                            else -> "Manuel Simkl Kaydı"
                        }
                    } else {
                        subtitle
                    },
                    type = type,
                    status = status,
                    score = score,
                    progress = progress,
                    total = total,
                    isFavorite = isFavorite,
                    isAdult = isAdult,
                    source = when (selectedTabIndex) {
                        0 -> "anilist"
                        1 -> "mal"
                        else -> "simkl"
                    },
                    malId = null,
                    imageUrl = null,
                    year = null,
                    synopsis = null,
                    startDate = startDate,
                    endDate = endDate,
                    notes = notes,
                    tags = tags,
                    priority = priority,
                    isRepeating = isRepeating,
                    repeatCount = repeatCount,
                    repeatValue = repeatValue,
                    volumeProgress = volumeProgress,
                    isPrivate = isPrivate,
                    isHiddenFromStatusLists = isHiddenFromStatusLists
                )

                viewModel.insertEntry(newEntry)

                showAddDialog = false
            }
        )
    }

    if (showApiSearchDialog) {
        KitsugiApiSearchDialog(
            onDismiss = {
                showApiSearchDialog = false
            },
            onResultSelected = { selection ->
                val result = selection.result
                val resolvedSource = when (selectedTabIndex) {
                    0 -> "anilist"
                    1 -> result.source
                    else -> "simkl"
                }

                val alreadyExists = entries.any { entry ->
                    entry.matches(result)
                }

                if (alreadyExists) {
                    duplicateMessage = "\"${result.title}\" zaten listende var."
                    showApiSearchDialog = false
                    return@KitsugiApiSearchDialog
                }

                val newEntry = MediaEntry(
                    id = 0,
                    title = result.title,
                    subtitle = result.subtitle,
                    type = result.type,
                    status = WatchStatus.Planned,
                    score = result.score,
                    progress = 0,
                    total = result.total,
                    isFavorite = false,
                    isAdult = result.isAdult,
                    source = resolvedSource,
                    malId = result.malId,
                    imageUrl = result.imageUrl,
                    year = result.year,
                    synopsis = selection.synopsis,
                    startDate = null,
                    endDate = null
                )

                viewModel.insertEntry(newEntry)

                showApiSearchDialog = false
            }
        )
    }

    duplicateMessage?.let { message ->
        KitsugiInfoDialog(
            title = "Zaten listede",
            message = message,
            onDismiss = {
                duplicateMessage = null
            }
        )
    }

    editingEntry?.let { entry ->
        KitsugiMediaEntryEditorDialog(
            initialEntry = entry,
            onDismiss = {
                editingEntry = null
            },
            onDeleteClick = {
                editingEntry = null
                viewModel.deleteEntryById(entry.id)
            },
            onConfirm = { title, subtitle, type, status, isAdult, progress, total, score, isFavorite, startDate, endDate, notes, tags, priority, isRepeating, repeatCount, repeatValue, volumeProgress, isPrivate, isHiddenFromStatusLists ->
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

                // Kullanıcı tarihleri boş bıraktıysa durum değişimine göre otomatik doldur
                val resolvedStartDate = if (startDate.isNullOrBlank() &&
                    (status == WatchStatus.Watching || status == WatchStatus.Completed) &&
                    entry.startDate.isNullOrBlank()
                ) today else startDate

                val resolvedEndDate = if (endDate.isNullOrBlank() &&
                    status == WatchStatus.Completed &&
                    entry.endDate.isNullOrBlank()
                ) today else endDate

                val updatedEntry = entry.copy(
                    title = title,
                    subtitle = if (subtitle.isBlank()) {
                        "Manuel eklenen içerik"
                    } else {
                        subtitle
                    },
                    type = type,
                    status = status,
                    isAdult = isAdult,
                    progress = progress,
                    total = total,
                    score = score,
                    isFavorite = isFavorite,
                    startDate = resolvedStartDate,
                    endDate = resolvedEndDate,
                    notes = notes,
                    tags = tags,
                    priority = priority ?: 0,
                    isRepeating = isRepeating,
                    repeatCount = repeatCount,
                    repeatValue = repeatValue,
                    volumeProgress = volumeProgress,
                    isPrivate = isPrivate,
                    isHiddenFromStatusLists = isHiddenFromStatusLists
                )

                viewModel.updateEntry(updatedEntry)

                editingEntry = null
            }
        )
    }

    deletingEntry?.let { entry ->
        KitsugiConfirmDialog(
            title = "Kaydı sil?",
            message = "\"${entry.title}\" listenizden kalıcı olarak kaldırılacak.",
            confirmText = "Sil",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteEntryById(entry.id)

                deletingEntry = null
            },
            onDismiss = {
                deletingEntry = null
            }
        )
    }

    if (activeZoomImageUrl != null) {
        KitsugiImagePreviewDialog(
            imageUrl = activeZoomImageUrl!!,
            title = activeZoomTitle,
            onDismiss = {
                activeZoomImageUrl = null
                activeZoomTitle = ""
            }
        )
    }
}
