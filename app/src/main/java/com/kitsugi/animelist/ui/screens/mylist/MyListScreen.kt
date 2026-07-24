@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kitsugi.animelist.ui.screens.mylist

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.DensityMedium
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import com.kitsugi.animelist.ui.components.KitsugiMotion
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
    onListLayoutChange: (String) -> Unit,
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
    onSettingsClick: () -> Unit,
    isBottomBarVisible: Boolean = true,
    onScrollReset: () -> Unit = {}
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

    var isScrollRestored by rememberSaveable(initialScrollIndex, initialScrollOffset) {
        mutableStateOf(initialScrollIndex == 0 && initialScrollOffset == 0)
    }

    var isFabVisible by rememberSaveable { mutableStateOf(true) }
    // showHeader and showScrollToTop are driven by active tab scroll — updated in LaunchedEffect below
    var showHeader by remember { mutableStateOf(true) }
    var showScrollToTopState by remember { mutableStateOf(false) }
    var prevIndex by remember { mutableIntStateOf(0) }
    var prevOffset by remember { mutableIntStateOf(0) }
    var showSearchField by rememberSaveable { mutableStateOf(false) }
    var showFilterPanel by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by rememberSaveable { mutableStateOf(false) }
    var showStatusBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showApiSearchDialog by rememberSaveable { mutableStateOf(false) }
    var activeZoomImageUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var activeZoomTitle by rememberSaveable { mutableStateOf("") }

    // Pager state for AniList / MAL / Simkl tabs (3 tabs)
    val tabPagerState = rememberPagerState(
        initialPage = selectedTabIndex.coerceIn(0, 2),
        pageCount = { 3 }
    )

    // Pager page change -> notify parent
    LaunchedEffect(tabPagerState.currentPage) {
        if (tabPagerState.currentPage != selectedTabIndex) {
            onTabIndexChange(tabPagerState.currentPage)
        }
    }

    // Parent tab change (chip click) -> animate pager
    LaunchedEffect(selectedTabIndex) {
        if (tabPagerState.currentPage != selectedTabIndex) {
            tabPagerState.animateScrollToPage(selectedTabIndex.coerceIn(0, 2))
        }
    }

    // Scroll tracking is handled per-tab inside the HorizontalPager LaunchedEffect below.

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

    val entriesAfterAdultFilter = remember(entries, showAdultContent) {
        entries.filter { entry ->
            showAdultContent || !entry.isAdult
        }
    }

    val selectedTabEntries = remember(entriesAfterAdultFilter, selectedTabIndex) {
        entriesAfterAdultFilter.filter { entry ->
            val src = entry.source.lowercase()
            when (selectedTabIndex) {
                0 -> src == "anilist"
                1 -> src == "mal" || src == "jikan" || src == "myanimelist"
                else -> src == "simkl"
            }
        }
    }

    val selectedStatus = remember(selectedStatusFilterId) {
        statusFilters.firstOrNull {
            it.id == selectedStatusFilterId
        }?.status
    }

    val selectedType = remember(selectedTypeFilterId) {
        typeFilters.firstOrNull {
            it.id == selectedTypeFilterId
        }?.type
    }

    val favoritesOnly = remember(selectedFavoriteFilterId, selectedStatusFilterId) {
        selectedFavoriteFilterId == "favorites" || selectedStatusFilterId == "favorites"
    }
    val adultOnly = remember(selectedStatusFilterId) {
        selectedStatusFilterId == "adult"
    }

    val filteredEntries = remember(
        selectedTabEntries,
        selectedStatus,
        selectedType,
        favoritesOnly,
        adultOnly,
        selectedScoreFilterId,
        selectedYearFilterId,
        selectedExtraFilterId,
        searchQuery
    ) {
        selectedTabEntries
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
                if (adultOnly) entry.isAdult else true
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
    }

    val visibleEntries = remember(filteredEntries, selectedSortId) {
        applySort(
            entries = filteredEntries,
            sortId = selectedSortId
        )
    }

    val groupedVisibleEntries = remember(visibleEntries) {
        val statusOrder = listOf(
            WatchStatus.Watching,
            WatchStatus.Repeating,
            WatchStatus.Planned,
            WatchStatus.Paused,
            WatchStatus.Dropped,
            WatchStatus.Completed
        )
        statusOrder.map { status ->
            status to visibleEntries.filter { it.status == status }
        }.filter { it.second.isNotEmpty() }
    }

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


    // Per-tab scroll states — declared at top level so FAB can reference them
    val tabScrollStates = remember { List(3) { androidx.compose.foundation.lazy.LazyListState() } }
    val activeTabScrollState = tabScrollStates[selectedTabIndex.coerceIn(0, 2)]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.background)
    ) {
        val hasActiveFilters = selectedStatusFilterId != "all" ||
                selectedTypeFilterId != "all" ||
                selectedFavoriteFilterId != "all" ||
                selectedScoreFilterId != "all" ||
                selectedYearFilterId != "all" ||
                selectedExtraFilterId != "all" ||
                (selectedSortId.isNotBlank() && selectedSortId != "newest" && selectedSortId != "added")

        // ── Sabit üst bar (başlık + arama + filtreleme) ──
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth(),
            color = KitsugiColors.background,
            shadowElevation = 0.dp
        ) {
            Column {
                // Başlık satırı — scroll aşağı gidince yumuşak şekilde daralır ve kaybolur
                val rawOffset = activeTabScrollState.firstVisibleItemScrollOffset
                val firstVisibleIndex = activeTabScrollState.firstVisibleItemIndex
                val collapseProgress = remember(firstVisibleIndex, rawOffset) {
                    if (firstVisibleIndex > 0) 1f
                    else (rawOffset.toFloat() / 100f).coerceIn(0f, 1f)
                }
                val headerHeight = 60.dp * (1f - collapseProgress)

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
                            // Görünüm değiştirme butonu
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

                            IconButton(onClick = { showSearchField = !showSearchField }) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Arama",
                                    tint = if (showSearchField || searchQuery.isNotBlank()) accentColor else KitsugiColors.textSecondary
                                )
                            }

                            // Filtrele ve Sırala Üst Bar Butonu (Sağ Üst Buton)
                            Box {
                                IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
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

                // Açılır Filtre ve Sırala Paneli (Sağ üstteki filtre butonuna basınca açılır)
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
                            onHideFilters = {
                                showFilterPanel = false
                            }
                        )
                    }
                }

                // AniHyou / MoeList Tarzı Arama ve Hızlı Filtre Paneli (3. Resimdeki Mantık)
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

                            // Sıralama Seçim Çipi
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

        // ── Sabit platform tab bar (AniList / MAL / Simkl) ──────────────────────
        val tabs = listOf("AniList", "MAL", "Simkl")
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 0.dp),
            color = KitsugiColors.background
        ) {
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
                                    coroutineScope.launch {
                                        tabPagerState.animateScrollToPage(index)
                                    }
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
                // 🎲 Rastgele buton
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

        // ── HorizontalPager: her sekme kendi scroll alanı ──────────────────────
        // tabScrollStates declared at composable top level

        // Aktif tab'ın scroll state'ini lazyListState ile senkronize et
        // (başlık gizleme ve FAB için)
        val activeTabScrollState = tabScrollStates[selectedTabIndex.coerceIn(0, 2)]

        LaunchedEffect(activeTabScrollState) {
            snapshotFlow {
                activeTabScrollState.firstVisibleItemIndex to activeTabScrollState.firstVisibleItemScrollOffset
            }.collect { (index, offset) ->
                if (isScrollRestored && entries.isNotEmpty()) {
                    onScrollPositionChange(index, offset)
                }
                val scrollingDown = index > prevIndex || (index == prevIndex && offset > prevOffset + 15)
                val scrollingUp   = index < prevIndex || (index == prevIndex && offset < prevOffset - 15)
                // Header collapse
                showHeader = (index == 0 && offset < 100)
                // Reset scroll/show bottom bar at top
                if (index == 0 && offset == 0) {
                    onScrollReset()
                }
                // Scroll-to-top FAB
                showScrollToTopState = index > 3
                // FAB (Tümü) görünürlüğü
                if (index == 0 && offset < 40) {
                    isFabVisible = true
                } else if (scrollingDown) {
                    isFabVisible = false
                } else if (scrollingUp) {
                    isFabVisible = true
                }
                // Hızlı aşağı scroll'da panel/arama otomatik kapat
                if (scrollingDown && index >= 1) {
                    if (showFilterPanel) showFilterPanel = false
                    if (showSearchField) showSearchField = false
                }
                prevIndex = index
                prevOffset = offset
            }
        }

        val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
        CompositionLocalProvider(
            LocalBringIntoViewSpec provides if (isTvDevice) tvSpec else LocalBringIntoViewSpec.current
        ) {
            HorizontalPager(
                state = tabPagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = !isTvDevice
            ) { page ->
                val pageTabIndex = page
                val pageIsConnected = when (pageTabIndex) {
                    0 -> isAniListConnected
                    1 -> isMalConnected
                    else -> isSimklConnected
                }
                val pageScrollState = tabScrollStates[pageTabIndex]
                val pageRefreshState = rememberPullToRefreshState()
                var pageIsRefreshing by remember { mutableStateOf(false) }

                PullToRefreshBox(
                    isRefreshing = pageIsRefreshing,
                    onRefresh = {
                        coroutineScope.launch {
                            pageIsRefreshing = true
                            when (pageTabIndex) {
                                0 -> onSyncAniList()
                                1 -> onSyncMal()
                                else -> onSyncSimkl()
                            }
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
                    // Sayfa içeriği: selectedTabIndex == page olduğunda asıl filtrelenmiş veriyi göster
                    // Diğer sayfalar için kendi içeriklerini göster (basit filtered view)
                    val pageEntries = remember(entriesAfterAdultFilter, pageTabIndex) {
                        entriesAfterAdultFilter.filter { entry ->
                            val src = entry.source.lowercase()
                            when (pageTabIndex) {
                                0 -> src == "anilist"
                                1 -> src == "mal" || src == "jikan" || src == "myanimelist"
                                else -> src == "simkl"
                            }
                        }
                    }

                    if (!pageIsConnected) {
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
                                    onLogin = {
                                        when (pageTabIndex) {
                                            0 -> onLoginAniList()
                                            1 -> onLoginMal()
                                            else -> onLoginSimkl()
                                        }
                                    }
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
                        // Aktif sayfaysa filtreli listeyi, değilse ham listeyi göster
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
                                    onIncrementProgress = { incrementEntryProgress(it) },
                                    onPosterLongClick = { imageUrl ->
                                        activeZoomImageUrl = imageUrl
                                        activeZoomTitle = displayEntries.find { it.imageUrl == imageUrl }?.title ?: ""
                                    }
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
                                    onIncrementProgress = { incrementEntryProgress(it) },
                                    onPosterLongClick = { imageUrl ->
                                        activeZoomImageUrl = imageUrl
                                        activeZoomTitle = displayEntries.find { it.imageUrl == imageUrl }?.title ?: ""
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(90.dp)) }
                        }
                    }
                }
            }
        }

    } // end Column

    // ── Scroll-aware floating category button ──
    val isTv = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current
    if (!isTv) {
        val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomPadding by animateDpAsState(
            targetValue = if (isLandscape) {
                16.dp
            } else if (isBottomBarVisible) {
                64.dp + navigationBarsPadding
            } else {
                16.dp + navigationBarsPadding
            },
            animationSpec = tween(durationMillis = KitsugiMotion.fastMillis + 50),
            label = "fab_bottom_padding"
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Tümü (Kategori) button on the Bottom-Start (Bottom-Left)
            AnimatedVisibility(
                visible = isFabVisible && isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        bottom = bottomPadding,
                        start = 20.dp
                    )
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

            // Scroll to Top button on the Bottom-End (Bottom-Right)
            AnimatedVisibility(
                visible = showScrollToTopState,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        bottom = bottomPadding,
                        end = 20.dp
                    )
                    .zIndex(10f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor)
                        .tvClickable(shape = RoundedCornerShape(16.dp)) {
                            coroutineScope.launch {
                                val activeState = tabScrollStates[selectedTabIndex.coerceIn(0, 2)]
                                activeState.animateScrollToItem(0)
                                onScrollReset()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Yukarı Git",
                        tint = KitsugiColors.background,
                        modifier = Modifier.size(24.dp)
                    )
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
            onConfirm = { title, subtitle, type, status, isAdult, progress, total, score, isFavorite, startDate, endDate, notes, tags, priority, isRepeating, repeatCount, repeatValue, volumeProgress, isPrivate, isHiddenFromStatusLists, _ ->
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
            onConfirm = { title, subtitle, type, status, isAdult, progress, total, score, isFavorite, startDate, endDate, notes, tags, priority, isRepeating, repeatCount, repeatValue, volumeProgress, isPrivate, isHiddenFromStatusLists, advancedScores ->
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

                viewModel.updateEntry(updatedEntry, advancedScores = advancedScores)

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
