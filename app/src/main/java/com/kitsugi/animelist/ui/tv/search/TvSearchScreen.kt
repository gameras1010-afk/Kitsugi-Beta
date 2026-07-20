package com.kitsugi.animelist.ui.tv.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.screens.search.SearchHistoryItem
import com.kitsugi.animelist.ui.screens.search.SearchPlatform
import com.kitsugi.animelist.ui.screens.search.SearchViewModel
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.utils.FocusMarqueeText
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private val TvChipShape = RoundedCornerShape(20.dp)

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun TvSearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    onNavigateToDetail: (JikanSearchResult) -> Unit = {}
) {
    val state by searchViewModel.uiState.collectAsStateWithLifecycle()
    val searchBarFocusRequester = remember { FocusRequester() }

    // ── Debounce: 500ms sonra otomatik ara ────────────────────────────────────
    LaunchedEffect(Unit) {
        snapshotFlow { state.query }
            .debounce(500L)
            .distinctUntilChanged()
            .filter { it.isNotBlank() }
            .collect { searchViewModel.search() }
    }

    // ── Ekran ilk açıldığında arama kutusuna focus ver ────────────────────────
    // requestFocusAfterFrames: layout attach olduktan sonra focus istiyor,
    // böylece "FocusRequester is not initialized" hatası tamamen engellenmiş oluyor.
    LaunchedEffect(Unit) {
        searchBarFocusRequester.requestFocusAfterFrames(frames = 2)
    }

    com.kitsugi.animelist.ui.tv.focus.TvGlobalFocusRecovery(fallbackFocusRequester = searchBarFocusRequester)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        // ── Başlık ────────────────────────────────────────────────────────────
        Text(
            text = "Arama",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ── Arama Kutusu ──────────────────────────────────────────────────────
        TvSearchBar(
            query = state.query,
            onQueryChange = { searchViewModel.setQuery(it) },
            onSearch = { searchViewModel.search() },
            onClear = { searchViewModel.clearQuery() },
            focusRequester = searchBarFocusRequester
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Filtre satırı: MediaType + Platform ───────────────────────────────
        TvSearchFilters(
            selectedMediaType = state.selectedMediaType,
            selectedPlatform = state.selectedPlatform,
            onMediaTypeSelected = { searchViewModel.setMediaType(it) },
            onPlatformSelected = { searchViewModel.setPlatform(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── İçerik Alanı ──────────────────────────────────────────────────────
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Aranıyor...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            state.errorMessage != null && state.hasSearched && state.results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "\"${state.query}\" için sonuç bulunamadı",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Farklı kelimeler veya filtreler deneyin",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            state.results.isNotEmpty() -> {
                val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
                val gridState = rememberLazyGridState()
                CompositionLocalProvider(LocalBringIntoViewSpec provides tvSpec) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = KitsugiTvTokens.Cards.posterWidth),
                        contentPadding = PaddingValues(bottom = KitsugiTvTokens.Spacing.overscanHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                        verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                        modifier = Modifier.dpadVerticalFastScroll(scrollableState = gridState)
                    ) {
                        items(state.results, key = { "${it.source}_${it.malId}" }) { result ->
                            TvSearchResultCard(
                                item = result,
                                onClick = { onNavigateToDetail(result) }
                            )
                        }
                    }
                }
            }

            !state.hasSearched && state.searchHistory.isNotEmpty() -> {
                TvSearchHistory(
                    history = state.searchHistory.take(10),
                    onHistoryClick = { searchViewModel.applyHistoryItem(it) },
                    onRemoveItem = { searchViewModel.removeHistoryItem(it) },
                    onClearAll = { searchViewModel.clearHistory() }
                )
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Anime, dizi veya film arayın",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ── Arama Kutusu ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f)
    val bgAlpha = if (isFocused) 0.12f else 0.08f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = bgAlpha))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .size(20.dp)
                .padding(end = 0.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = "Anime, film veya dizi ara...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
                innerTextField()
            },
            singleLine = true
        )

        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .tvClickable(shape = RoundedCornerShape(12.dp)) { onClear() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Temizle",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ── Filtre Satırı ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSearchFilters(
    selectedMediaType: MediaType,
    selectedPlatform: SearchPlatform,
    onMediaTypeSelected: (MediaType) -> Unit,
    onPlatformSelected: (SearchPlatform) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // MediaType chips
        item {
            TvFilterChip(
                label = "Anime",
                selected = selectedMediaType == MediaType.Anime,
                accent = accent,
                onClick = { onMediaTypeSelected(MediaType.Anime) }
            )
        }
        item {
            TvFilterChip(
                label = "Manga",
                selected = selectedMediaType == MediaType.Manga,
                accent = accent,
                onClick = { onMediaTypeSelected(MediaType.Manga) }
            )
        }

        item { Spacer(modifier = Modifier.width(8.dp)) }

        // Platform chips — Manga platformlarını filtrele
        val availablePlatforms = if (selectedMediaType == MediaType.Manga) {
            listOf(SearchPlatform.All, SearchPlatform.MAL, SearchPlatform.AniList)
        } else {
            SearchPlatform.values().toList()
        }

        items(availablePlatforms) { platform ->
            TvFilterChip(
                label = platform.label,
                selected = selectedPlatform == platform,
                accent = accent,
                onClick = { onPlatformSelected(platform) }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvFilterChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        selected -> accent.copy(alpha = 0.85f)
        isFocused -> Color.White.copy(alpha = 0.15f)
        else -> Color.White.copy(alpha = 0.07f)
    }
    val textColor = when {
        selected -> Color.White
        isFocused -> Color.White
        else -> Color.White.copy(alpha = 0.65f)
    }
    val borderColor = when {
        selected -> accent
        isFocused -> Color.White.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .wrapContentWidth()
            .clip(TvChipShape)
            .background(bgColor)
            .border(1.dp, borderColor, TvChipShape)
            .tvClickable(shape = TvChipShape) { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ── Sonuç Kartı ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSearchResultCard(item: JikanSearchResult, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(KitsugiTvTokens.Cards.posterWidth)
            .tvClickable(
                shape = KitsugiTvTokens.Shapes.posterCard as RoundedCornerShape,
                onClick = onClick
            )
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(KitsugiTvTokens.Cards.posterHeight)
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Puan badge
            item.score?.let { score ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "★ $score",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFD700)
                    )
                }
            }

            // Platform/kaynak badge
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.source.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Focus border overlay
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            2.dp,
                            Color.White,
                            KitsugiTvTokens.Shapes.posterCard
                        )
                )
            }
        }

        // Marquee başlık
        FocusMarqueeText(
            text = item.title,
            focused = isFocused,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E))
                .padding(6.dp)
        )
    }
}

// ── Arama Geçmişi ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSearchHistory(
    history: List<SearchHistoryItem>,
    onHistoryClick: (SearchHistoryItem) -> Unit,
    onRemoveItem: (SearchHistoryItem) -> Unit,
    onClearAll: () -> Unit
) {
    Column {
        // Başlık + Tümünü Temizle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Son Aramalar",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.6f)
            )

            var clearFocused by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (clearFocused) Color.White.copy(alpha = 0.12f)
                        else Color.Transparent
                    )
                    .tvClickable(shape = RoundedCornerShape(6.dp)) { onClearAll() }
                    .onFocusChanged { clearFocused = it.isFocused }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Tümünü Temizle",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Temizle",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // Geçmiş öğeleri
        history.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Arama metni alanı (Focusable)
                var isTextFocused by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isTextFocused) Color.White.copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .tvClickable(shape = RoundedCornerShape(6.dp)) { onHistoryClick(item) }
                        .onFocusChanged { isTextFocused = it.isFocused }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.query,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Platform badge
                    Text(
                        text = item.platform.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Ayrı Fokuslanabilir Silme Butonu (D-pad ile sağa geçilerek silinebilir)
                var isDeleteFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isDeleteFocused) Color(0xFFE63946).copy(alpha = 0.8f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .tvClickable(shape = RoundedCornerShape(6.dp)) { onRemoveItem(item) }
                        .onFocusChanged { isDeleteFocused = it.isFocused },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Sil",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
