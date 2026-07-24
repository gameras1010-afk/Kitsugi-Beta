@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.kitsugi.animelist.ui.screens.profile

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.StopCircle
import com.kitsugi.animelist.ui.components.KitsugiSheetOrDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.KitsugiApiBase
import com.kitsugi.animelist.data.remote.cleanApiText
import com.kitsugi.animelist.data.remote.matches
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.components.KitsugiSearchField
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.LocalIsTvDevice
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class UserMediaListItem(
    val mediaId: Int,
    val malId: Int?,
    val title: String,
    val imageUrl: String?,
    val mediaType: MediaType,
    val status: WatchStatus,
    val score: Double?,
    val progress: Int,
    val total: Int?,
    val isAdult: Boolean,
    val format: String?,
    val year: Int?
)

data class UserMediaListUiState(
    val isLoading: Boolean = true,
    val items: List<UserMediaListItem> = emptyList(),
    val error: String? = null
)

class KitsugiUserMediaListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UserMediaListUiState())
    val uiState: StateFlow<UserMediaListUiState> = _uiState.asStateFlow()

    private var loadedUserId: Int? = null
    private var loadedMediaType: MediaType? = null

    fun loadUserMediaList(userId: Int, mediaType: MediaType, forceRefresh: Boolean = false) {
        if (!forceRefresh && loadedUserId == userId && loadedMediaType == mediaType && _uiState.value.items.isNotEmpty() && !_uiState.value.isLoading) {
            return
        }
        loadedUserId = userId
        loadedMediaType = mediaType
        viewModelScope.launch {
            _uiState.value = UserMediaListUiState(isLoading = true, items = emptyList(), error = null)
            val fetched = fetchUserMediaListFromAniList(userId, mediaType)
            if (fetched != null) {
                _uiState.value = UserMediaListUiState(isLoading = false, items = fetched, error = null)
            } else {
                _uiState.value = UserMediaListUiState(isLoading = false, items = emptyList(), error = "Liste yüklenemedi")
            }
        }
    }

    fun resetState() {
        loadedUserId = null
        loadedMediaType = null
        _uiState.value = UserMediaListUiState()
    }

    private suspend fun fetchUserMediaListFromAniList(userId: Int, mediaType: MediaType): List<UserMediaListItem>? {
        return withContext(Dispatchers.IO) {
            val typeStr = if (mediaType == MediaType.Anime) "ANIME" else "MANGA"
            val query = """
                query (${'$'}userId: Int, ${'$'}type: MediaType) {
                    MediaListCollection(userId: ${'$'}userId, type: ${'$'}type) {
                        lists {
                            name
                            status
                            entries {
                                status
                                score(format: POINT_10_DECIMAL)
                                progress
                                media {
                                    id
                                    idMal
                                    title { romaji english native userPreferred }
                                    coverImage { extraLarge large medium }
                                    episodes
                                    chapters
                                    format
                                    isAdult
                                    startDate { year }
                                }
                            }
                        }
                    }
                }
            """.trimIndent()
            val variables = JSONObject().put("userId", userId).put("type", typeStr)
            runCatching {
                val response = KitsugiApiBase.executeAniListQuery(query, variables) ?: return@runCatching null
                val root = JSONObject(response)
                val listsArr = root.optJSONObject("data")?.optJSONObject("MediaListCollection")?.optJSONArray("lists") ?: return@runCatching null
                val resultList = mutableListOf<UserMediaListItem>()
                for (i in 0 until listsArr.length()) {
                    val listObj = listsArr.optJSONObject(i) ?: continue
                    val entriesArr = listObj.optJSONArray("entries") ?: continue
                    for (j in 0 until entriesArr.length()) {
                        val entryObj = entriesArr.optJSONObject(j) ?: continue
                        val mediaObj = entryObj.optJSONObject("media") ?: continue
                        val mediaId = mediaObj.optInt("id")
                        val idMal = if (mediaObj.has("idMal") && !mediaObj.isNull("idMal")) mediaObj.optInt("idMal") else null
                        val titleObj = mediaObj.optJSONObject("title")
                        val title = titleObj?.optString("userPreferred")?.takeIf { it.isNotBlank() }
                            ?: titleObj?.optString("romaji")?.takeIf { it.isNotBlank() }
                            ?: titleObj?.optString("english")?.takeIf { it.isNotBlank() }
                            ?: titleObj?.optString("native") ?: "Medya #$mediaId"
                        val coverObj = mediaObj.optJSONObject("coverImage")
                        val imageUrl = coverObj?.optString("extraLarge")?.takeIf { it.isNotBlank() }
                            ?: coverObj?.optString("large")?.takeIf { it.isNotBlank() }
                            ?: coverObj?.optString("medium")
                        val statusStr = entryObj.optString("status").uppercase()
                        val watchStatus = when (statusStr) {
                            "CURRENT" -> WatchStatus.Watching
                            "COMPLETED" -> WatchStatus.Completed
                            "PLANNING" -> WatchStatus.Planned
                            "PAUSED" -> WatchStatus.Paused
                            "DROPPED" -> WatchStatus.Dropped
                            else -> WatchStatus.Watching
                        }
                        val scoreVal = if (entryObj.has("score") && !entryObj.isNull("score")) entryObj.optDouble("score") else null
                        val progressVal = entryObj.optInt("progress", 0)
                        val totalVal = if (mediaType == MediaType.Anime) {
                            if (mediaObj.has("episodes") && !mediaObj.isNull("episodes")) mediaObj.optInt("episodes") else null
                        } else {
                            if (mediaObj.has("chapters") && !mediaObj.isNull("chapters")) mediaObj.optInt("chapters") else null
                        }
                        val isAdult = mediaObj.optBoolean("isAdult", false)
                        val format = mediaObj.optString("format", "")
                        val year = mediaObj.optJSONObject("startDate")?.let {
                            if (it.has("year") && !it.isNull("year")) it.optInt("year") else null
                        }

                        resultList.add(
                            UserMediaListItem(
                                mediaId = mediaId,
                                malId = idMal,
                                title = title.cleanApiText(),
                                imageUrl = imageUrl,
                                mediaType = mediaType,
                                status = watchStatus,
                                score = if (scoreVal != null && scoreVal > 0) scoreVal else null,
                                progress = progressVal,
                                total = if (totalVal != null && totalVal > 0) totalVal else null,
                                isAdult = isAdult,
                                format = format,
                                year = year
                            )
                        )
                    }
                }
                resultList.distinctBy { it.mediaId }
            }.getOrNull()
        }
    }
}

@Composable
fun KitsugiUserMediaListScreen(
    userId: Int,
    username: String,
    initialMediaType: MediaType,
    appSettings: AppSettings,
    mediaEntries: List<MediaEntry>,
    onBackClick: () -> Unit,
    onMediaClick: (JikanSearchResult) -> Unit,
    onLocalEntryClick: (MediaEntry) -> Unit,
    accentColor: Color = LocalKitsugiAccent.current,
    customViewModel: KitsugiUserMediaListViewModel? = null
) {
    val viewModel: KitsugiUserMediaListViewModel = customViewModel ?: androidx.lifecycle.viewmodel.compose.viewModel(key = "user_media_list_${userId}_${initialMediaType.name}")

    var selectedType by rememberSaveable { mutableStateOf(initialMediaType) }

    LaunchedEffect(userId, selectedType) {
        viewModel.loadUserMediaList(userId, selectedType)
    }

    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatusFilter by rememberSaveable { mutableStateOf<WatchStatus?>(null) }
    var isGridView by rememberSaveable { mutableStateOf(true) }

    var isFabVisible by rememberSaveable { mutableStateOf(true) }
    var prevIndex by rememberSaveable { mutableStateOf(0) }
    var prevOffset by rememberSaveable { mutableStateOf(0) }

    val lazyGridState = rememberLazyGridState()
    val lazyListState = rememberLazyListState()

    val showScrollToTop by remember {
        derivedStateOf {
            if (isGridView) {
                lazyGridState.firstVisibleItemIndex > 3
            } else {
                lazyListState.firstVisibleItemIndex > 3
            }
        }
    }

    LaunchedEffect(isGridView) {
        isFabVisible = true
        prevIndex = 0
        prevOffset = 0
    }

    LaunchedEffect(isGridView, lazyGridState, lazyListState) {
        if (isGridView) {
            snapshotFlow { lazyGridState.firstVisibleItemIndex to lazyGridState.firstVisibleItemScrollOffset }
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
        } else {
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
    }

    // Sort: 0=Varsayılan, 1=A-Z, 2=Puan, 3=İlerleme
    var sortId by rememberSaveable { mutableStateOf(0) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showStatusBottomSheet by remember { mutableStateOf(false) }

    val sortLabels = listOf("Varsayılan", "A-Z", "Puana Göre ↓", "İlerleyeye Göre ↓")

    val filteredItems = remember(state.items, searchQuery, selectedStatusFilter, sortId) {
        state.items
            .filter { item ->
                if (selectedStatusFilter == null) true else item.status == selectedStatusFilter
            }
            .filter { item ->
                if (searchQuery.isBlank()) true
                else item.title.lowercase().contains(searchQuery.trim().lowercase())
            }
            .let { list ->
                when (sortId) {
                    1 -> list.sortedBy { it.title.lowercase() }
                    2 -> list.sortedByDescending { it.score ?: -1.0 }
                    3 -> list.sortedByDescending { it.progress }
                    else -> list
                }
            }
    }

    val statusOrder = listOf(
        WatchStatus.Watching,
        WatchStatus.Paused,
        WatchStatus.Planned,
        WatchStatus.Dropped,
        WatchStatus.Completed
    )

    val pullRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
    ) {
        // Sticky Header / Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = KitsugiColors.Surface,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Geri",
                                tint = KitsugiColors.TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = username,
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (selectedType == MediaType.Anime) "Anime Listesi" else "Manga Listesi",
                                color = KitsugiColors.TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Sort chip
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(KitsugiColors.SurfaceStrong)
                                    .tvClickable(shape = RoundedCornerShape(12.dp)) { showSortMenu = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "↕ ${sortLabels[sortId]}",
                                    color = if (sortId != 0) accentColor else KitsugiColors.TextMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                sortLabels.forEachIndexed { idx, label ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(text = label, color = if (sortId == idx) accentColor else KitsugiColors.TextPrimary) },
                                        onClick = { sortId = idx; showSortMenu = false }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                imageVector = if (isGridView) Icons.Rounded.List else Icons.Rounded.GridView,
                                contentDescription = "Görünüm Değiştir",
                                tint = accentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Type Switcher Row (Anime / Manga)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(KitsugiColors.SurfaceStrong)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedType == MediaType.Anime) accentColor else Color.Transparent)
                            .tvClickable(shape = RoundedCornerShape(12.dp)) { selectedType = MediaType.Anime }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Anime Listesi",
                            color = if (selectedType == MediaType.Anime) KitsugiColors.Background else KitsugiColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedType == MediaType.Manga) accentColor else Color.Transparent)
                            .tvClickable(shape = RoundedCornerShape(12.dp)) { selectedType = MediaType.Manga }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Manga Listesi",
                            color = if (selectedType == MediaType.Manga) KitsugiColors.Background else KitsugiColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Arama Kutusu
                KitsugiSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Kullanıcının listesinde ara...",
                    modifier = Modifier.fillMaxWidth()
                )


            }
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = {
                viewModel.loadUserMediaList(userId, selectedType, forceRefresh = true)
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = pullRefreshState
        ) {
            if (state.isLoading && state.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (state.error != null && state.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.error!!, color = KitsugiColors.TextMuted)
                }
            } else if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Gösterilecek öğe bulunamadı",
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                val handleItemClick: (UserMediaListItem) -> Unit = { item ->
                    val stableId = if (item.malId != null && item.malId > 0) item.malId else (item.mediaId + 100_000_000)
                    val searchResult = JikanSearchResult(
                        malId = stableId,
                        title = item.title,
                        subtitle = item.format ?: "",
                        type = item.mediaType,
                        total = item.total,
                        score = item.score?.toInt(),
                        isAdult = item.isAdult,
                        imageUrl = item.imageUrl,
                        year = item.year,
                        source = "anilist",
                        realMalId = item.malId,
                        rawScoreDouble = item.score
                    )
                    val existing = mediaEntries.firstOrNull { it.matches(searchResult) }
                    if (existing != null) {
                        onLocalEntryClick(existing)
                    } else {
                        onMediaClick(searchResult)
                    }
                }

                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (isLandscape) 5 else 3),
                        state = lazyGridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(listOf("__header__"), span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "${filteredItems.size} sonuç",
                                color = KitsugiColors.TextMuted,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                        if (selectedStatusFilter == null) {
                            // Grouped by status
                            statusOrder.forEach { status ->
                                val groupItems = filteredItems.filter { it.status == status }
                                if (groupItems.isNotEmpty()) {
                                    items(
                                        listOf("gh_${status.name}"),
                                        key = { it },
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        val headerLabel = when (status) {
                                            WatchStatus.Watching -> if (selectedType == MediaType.Anime) "İzleniyor" else "Okunuyor"
                                            WatchStatus.Completed -> "Tamamlandı"
                                            WatchStatus.Planned -> "Planlandı"
                                            WatchStatus.Paused -> "Durduruldu"
                                            WatchStatus.Dropped -> "Bırakıldı"
                                            else -> status.name
                                        }
                                        Text(
                                            text = "$headerLabel (${groupItems.size})",
                                            color = KitsugiColors.TextPrimary,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp)
                                        )
                                    }
                                    items(groupItems, key = { "g_${it.mediaId}" }) { item ->
                                        UserMediaGridCard(
                                            item = item,
                                            blurAdultMedia = appSettings.blurAdultMedia,
                                            accentColor = accentColor,
                                            onClick = { handleItemClick(item) }
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredItems, key = { it.mediaId }) { item ->
                                UserMediaGridCard(
                                    item = item,
                                    blurAdultMedia = appSettings.blurAdultMedia,
                                    accentColor = accentColor,
                                    onClick = { handleItemClick(item) }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "${filteredItems.size} sonuç",
                                color = KitsugiColors.TextMuted,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                        if (selectedStatusFilter == null) {
                            // Grouped by status
                            statusOrder.forEach { status ->
                                val groupItems = filteredItems.filter { it.status == status }
                                if (groupItems.isNotEmpty()) {
                                    item(key = "rh_${status.name}") {
                                        val headerLabel = when (status) {
                                            WatchStatus.Watching -> if (selectedType == MediaType.Anime) "İzleniyor" else "Okunuyor"
                                            WatchStatus.Completed -> "Tamamlandı"
                                            WatchStatus.Planned -> "Planlandı"
                                            WatchStatus.Paused -> "Durduruldu"
                                            WatchStatus.Dropped -> "Bırakıldı"
                                            else -> status.name
                                        }
                                        Text(
                                            text = "$headerLabel (${groupItems.size})",
                                            color = KitsugiColors.TextPrimary,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp)
                                        )
                                    }
                                    items(groupItems, key = { "r_${it.mediaId}" }) { item ->
                                        UserMediaRowCard(
                                            item = item,
                                            blurAdultMedia = appSettings.blurAdultMedia,
                                            accentColor = accentColor,
                                            onClick = { handleItemClick(item) }
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredItems, key = { it.mediaId }) { item ->
                                UserMediaRowCard(
                                    item = item,
                                    blurAdultMedia = appSettings.blurAdultMedia,
                                    accentColor = accentColor,
                                    onClick = { handleItemClick(item) }
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // ── Floating Status FAB (sağ alt köşe) ──
    val isTv = LocalIsTvDevice.current
    if (!isTv && !state.isLoading && state.items.isNotEmpty()) {
        Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize()
        ) {
            // Tümü (Kategori) button on the Bottom-Start (Bottom-Left)
            AnimatedVisibility(
                visible = isFabVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 20.dp, start = 20.dp)
                    .zIndex(10f)
            ) {
                val fabLabel = when (selectedStatusFilter) {
                    WatchStatus.Watching -> if (selectedType == MediaType.Anime) "İzleniyor" else "Okunuyor"
                    WatchStatus.Completed -> "Tamamlandı"
                    WatchStatus.Planned -> "Planlandı"
                    WatchStatus.Paused -> "Durduruldu"
                    WatchStatus.Dropped -> "Bırakıldı"
                    else -> "Tümü"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor)
                        .tvClickable(shape = RoundedCornerShape(999.dp)) {
                            showStatusBottomSheet = true
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.List,
                            contentDescription = "Kategori",
                            tint = KitsugiColors.Background,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = fabLabel,
                            color = KitsugiColors.Background,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Scroll to Top button on the Bottom-End (Bottom-Right)
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 20.dp, end = 20.dp)
                    .zIndex(10f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor)
                        .tvClickable(shape = RoundedCornerShape(16.dp)) {
                            coroutineScope.launch {
                                if (isGridView) {
                                    lazyGridState.animateScrollToItem(0)
                                } else {
                                    lazyListState.animateScrollToItem(0)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Yukarı Git",
                        tint = KitsugiColors.Background,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    if (showStatusBottomSheet) {
        UserMediaListStatusBottomSheet(
            items = state.items,
            selectedStatus = selectedStatusFilter,
            mediaType = selectedType,
            onStatusSelected = { selectedStatusFilter = it },
            onDismissRequest = { showStatusBottomSheet = false }
        )
    }
}

@Composable
private fun UserMediaGridCard(
    item: UserMediaListItem,
    blurAdultMedia: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(KitsugiColors.SurfaceStrong)
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurAdultMedia && item.isAdult) Modifier.blur(16.dp) else Modifier),
                contentScale = ContentScale.Crop
            )

            // Status Badge Top Left
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(KitsugiColors.Background.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = item.status.label,
                    color = accentColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Score Badge Top Right
            if (item.score != null) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(KitsugiColors.Background.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = KitsugiColors.AccentYellow,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "%.1f".format(item.score),
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(
                text = item.title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${item.progress} / ${item.total ?: "?"}",
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun UserMediaRowCard(
    item: UserMediaListItem,
    blurAdultMedia: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp, 84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KitsugiColors.SurfaceStrong)
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurAdultMedia && item.isAdult) Modifier.blur(16.dp) else Modifier),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(KitsugiColors.SurfaceStrong)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.status.label,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "İlerleme: ${item.progress} / ${item.total ?: "?"}",
                    color = KitsugiColors.TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        if (item.score != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(KitsugiColors.SurfaceStrong)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = KitsugiColors.AccentYellow,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "%.1f".format(item.score),
                    color = KitsugiColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun UserMediaListStatusBottomSheet(
    items: List<UserMediaListItem>,
    selectedStatus: WatchStatus?,
    mediaType: MediaType,
    onStatusSelected: (WatchStatus?) -> Unit,
    onDismissRequest: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    val totalCount = items.size
    val watchingCount = items.count { it.status == WatchStatus.Watching }
    val completedCount = items.count { it.status == WatchStatus.Completed }
    val plannedCount = items.count { it.status == WatchStatus.Planned }
    val pausedCount = items.count { it.status == WatchStatus.Paused }
    val droppedCount = items.count { it.status == WatchStatus.Dropped }

    val statusItems = listOf(
        Triple(null, "Tümü", Icons.Rounded.FormatListBulleted to totalCount),
        Triple(WatchStatus.Watching, if (mediaType == MediaType.Anime) "İzliyor" else "Okuyor", Icons.Rounded.PlayCircle to watchingCount),
        Triple(WatchStatus.Completed, "Tamamlandı", Icons.Rounded.CheckCircle to completedCount),
        Triple(WatchStatus.Planned, "Planlanan", Icons.Rounded.Schedule to plannedCount),
        Triple(WatchStatus.Paused, "Durduruldu", Icons.Rounded.PauseCircle to pausedCount),
        Triple(WatchStatus.Dropped, "Bırakıldı", Icons.Rounded.StopCircle to droppedCount)
    )

    KitsugiSheetOrDialog(onDismiss = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statusItems.forEach { (status, title, iconAndCount) ->
                val (icon, count) = iconAndCount
                val isSelected = selectedStatus == status

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.15f) else KitsugiColors.SurfaceStrong.copy(alpha = 0.4f)
                        )
                        .tvClickable(shape = RoundedCornerShape(16.dp), onClick = {
                            onStatusSelected(status)
                            onDismissRequest()
                        })
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) accentColor else KitsugiColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = title,
                            color = if (isSelected) accentColor else KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = count.toString(),
                        color = if (isSelected) accentColor else KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
