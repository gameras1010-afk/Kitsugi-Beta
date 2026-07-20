package com.kitsugi.animelist.ui.tv.library

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.screens.mylist.MyListViewModel
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.utils.FocusMarqueeText
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames

private val ChipShape = RoundedCornerShape(20.dp)

// ── İçerik tipi seçimi (Anime / Manga) ──────────────────────────────────────
private enum class TvLibraryContentType(val label: String) {
    Anime("Anime"),
    Manga("Manga")
}

// ── İzleme/okuma durum sekmeleri ─────────────────────────────────────────────
private enum class TvWatchTab(
    val status: WatchStatus,
    val displayName: String
) {
    Watching(WatchStatus.Watching, "İzlenenler"),
    Planned(WatchStatus.Planned, "Planlananlar"),
    Completed(WatchStatus.Completed, "Tamamlananlar"),
    OnHold(WatchStatus.Paused, "Beklenenler"),
    Dropped(WatchStatus.Dropped, "Bırakılanlar")
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun TvLibraryScreen(
    myListViewModel: MyListViewModel = viewModel(),
    onNavigateToDetail: (com.kitsugi.animelist.data.remote.JikanSearchResult) -> Unit = {}
) {
    // ViewModel init{} içinde hazır — doğrudan dinle
    val entries by myListViewModel.entriesFlow.collectAsStateWithLifecycle()

    var selectedSort by remember { mutableStateOf("updated_desc") }

    // İçerik tipi ve durum sekmeleri
    var selectedContentType by remember { mutableStateOf(TvLibraryContentType.Anime) }
    var selectedTab by remember { mutableStateOf(TvWatchTab.Watching) }

    // İçerik tipine göre filtrele; Anime = Anime + Movie + TvShow, Manga = Manga
    val typeFilteredEntries = remember(entries, selectedContentType) {
        when (selectedContentType) {
            TvLibraryContentType.Anime -> entries.filter {
                it.type != MediaType.Manga
            }
            TvLibraryContentType.Manga -> entries.filter {
                it.type == MediaType.Manga
            }
        }
    }

    val filteredEntries = remember(typeFilteredEntries, selectedTab, selectedSort) {
        com.kitsugi.animelist.ui.screens.mylist.applySort(
            typeFilteredEntries.filter { it.status == selectedTab.status },
            selectedSort
        )
    }

    // İstatistik hesapla
    val totalCount = typeFilteredEntries.size
    val watchingCount = typeFilteredEntries.count { it.status == WatchStatus.Watching }
    val completedCount = typeFilteredEntries.count { it.status == WatchStatus.Completed }

    val libraryFocusRequester = remember { FocusRequester() }
    com.kitsugi.animelist.ui.tv.focus.TvGlobalFocusRecovery(fallbackFocusRequester = libraryFocusRequester)

    LaunchedEffect(Unit) {
        libraryFocusRequester.requestFocusAfterFrames(frames = 3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .focusRequester(libraryFocusRequester)
    ) {
        // ── Başlık ve İstatistikler ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Kütüphane",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            // İstatistik rozetleri
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvStatBadge(label = "Toplam", value = totalCount)
                TvStatBadge(label = "Aktif", value = watchingCount)
                TvStatBadge(label = "Tamamlandı", value = completedCount)
            }
        }

        // ── İçerik tipi seçimi (Anime / Manga) ──────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvLibraryContentType.entries.forEach { ct ->
                TvLibraryChip(
                    label = ct.label,
                    selected = selectedContentType == ct,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = { selectedContentType = ct }
                )
            }
        }

        // ── Durum sekmeleri ──────────────────────────────────────────────────
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            items(TvWatchTab.entries) { tab ->
                val tabEntryCount = typeFilteredEntries.count { it.status == tab.status }
                TvLibraryTabChip(
                    label = tab.displayName,
                    count = tabEntryCount,
                    selected = selectedTab == tab,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = { selectedTab = tab }
                )
            }
        }

        // ── Sıralama Satırı ─────────────────────────────────────────────────
        val sortOptions = remember {
            listOf(
                "updated_desc" to "Son Güncellenen",
                "title" to "Başlık",
                "score" to "Puan",
                "progress" to "İlerleme",
                "year_desc" to "Yıl"
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            items(sortOptions) { (id, label) ->
                TvLibraryChip(
                    label = label,
                    selected = selectedSort == id,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = { selectedSort = id }
                )
            }
        }

        // ── Continue Watching Satırı ─────────────────────────────────────────
        val continueWatching = remember(typeFilteredEntries) {
            typeFilteredEntries.filter {
                it.status == com.kitsugi.animelist.model.WatchStatus.Watching && it.progress > 0
            }.sortedByDescending { it.updatedAt }.take(12)
        }
        if (selectedTab == TvWatchTab.Watching && continueWatching.isNotEmpty()) {
            androidx.compose.material3.Text(
                text = "Devam Et",
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                items(continueWatching, key = { "cw_${it.id}" }) { entry ->
                    TvContinueWatchingCard(
                        entry = entry,
                        onClick = {
                            val result = com.kitsugi.animelist.data.remote.JikanSearchResult(
                                malId = entry.malId ?: 0,
                                title = entry.title,
                                subtitle = entry.subtitle ?: "",
                                type = entry.type,
                                total = entry.total,
                                score = entry.score,
                                isAdult = entry.isAdult,
                                imageUrl = entry.imageUrl,
                                year = entry.year,
                                source = entry.source,
                                realMalId = entry.malId
                            )
                            onNavigateToDetail(result)
                        }
                    )
                }
            }
        }

        // ── İçerik Alanı ──────────────────────────────────────────────────────
        if (filteredEntries.isNotEmpty()) {
            val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
            val gridState = rememberLazyGridState()
            var optionsEntry by remember { mutableStateOf<com.kitsugi.animelist.model.MediaEntry?>(null) }

            CompositionLocalProvider(LocalBringIntoViewSpec provides tvSpec) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = KitsugiTvTokens.Cards.posterWidth),
                    contentPadding = PaddingValues(bottom = KitsugiTvTokens.Spacing.overscanHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                    verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                    modifier = Modifier
                        .dpadVerticalFastScroll(scrollableState = gridState)
                        .focusRestorer()
                ) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        TvLibraryCard(
                            entry = entry,
                            onClick = {
                                val result = com.kitsugi.animelist.data.remote.JikanSearchResult(
                                    malId = entry.malId ?: 0,
                                    title = entry.title,
                                    subtitle = entry.subtitle ?: "",
                                    type = entry.type,
                                    total = entry.total,
                                    score = entry.score,
                                    isAdult = entry.isAdult,
                                    imageUrl = entry.imageUrl,
                                    year = entry.year,
                                    source = entry.source,
                                    realMalId = entry.malId
                                )
                                onNavigateToDetail(result)
                            },
                            onLongClick = { optionsEntry = entry }
                        )
                    }
                }
            }

            // Long-press options dialog
            optionsEntry?.let { e ->
                TvPosterOptionsDialog(
                    entry = e,
                    onDismiss = { optionsEntry = null },
                    onDetail = {
                        optionsEntry = null
                        val result = com.kitsugi.animelist.data.remote.JikanSearchResult(
                            malId = e.malId ?: 0,
                            title = e.title,
                            subtitle = e.subtitle ?: "",
                            type = e.type,
                            total = e.total,
                            score = e.score,
                            isAdult = e.isAdult,
                            imageUrl = e.imageUrl,
                            year = e.year,
                            source = e.source,
                            realMalId = e.malId
                        )
                        onNavigateToDetail(result)
                    },
                    onProgressIncrement = {
                        myListViewModel.incrementEntryProgress(e)
                        optionsEntry = null
                    },
                    onDelete = {
                        myListViewModel.deleteEntryById(e.id)
                        optionsEntry = null
                    }
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (selectedContentType == TvLibraryContentType.Manga) "📚" else "📭",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${selectedTab.displayName} listesinde içerik bulunmuyor",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Arama yaparak içerik ekleyebilirsiniz",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.25f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ── Yardımcı bileşenler ───────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvStatBadge(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.45f)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvLibraryChip(
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
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .clip(ChipShape)
            .background(bgColor)
            .border(1.dp, if (selected) accent else Color.Transparent, ChipShape)
            .tvClickable(shape = ChipShape) { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.65f),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvLibraryTabChip(
    label: String,
    count: Int,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        selected -> accent.copy(alpha = 0.25f)
        isFocused -> Color.White.copy(alpha = 0.12f)
        else -> Color.White.copy(alpha = 0.05f)
    }
    val borderColor = when {
        selected -> accent
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .tvClickable(shape = RoundedCornerShape(8.dp)) { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) accent else Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Kütüphane Kartı ──────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvLibraryCard(
    entry: MediaEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(KitsugiTvTokens.Cards.posterWidth)
            .tvClickable(
                shape = KitsugiTvTokens.Shapes.posterCard as RoundedCornerShape,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(KitsugiTvTokens.Cards.posterHeight)
        ) {
            AsyncImage(
                model = entry.imageUrl,
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // İlerleme rozeti
            val progressText = if (entry.total != null && entry.total > 0) {
                "${entry.progress}/${entry.total}"
            } else {
                "${entry.progress}"
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }

            // Favori rozeti
            if (entry.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFF6B35).copy(alpha = 0.85f))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "❤",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Focus border
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, Color.White, KitsugiTvTokens.Shapes.posterCard)
                )
            }
        }

        // Marquee başlık
        FocusMarqueeText(
            text = entry.title,
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvContinueWatchingCard(
    entry: com.kitsugi.animelist.model.MediaEntry,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .width(220.dp)
            .tvClickable(
                shape = KitsugiTvTokens.Shapes.posterCard as RoundedCornerShape,
                onClick = onClick
            )
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp)
        ) {
            AsyncImage(
                model = entry.imageUrl,
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Progress bar
            val progress = entry.progress.toFloat()
            val total = (entry.total ?: 1).toFloat().coerceAtLeast(1f)
            val progressPercent = (progress / total).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressPercent)
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = entry.title,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvPosterOptionsDialog(
    entry: com.kitsugi.animelist.model.MediaEntry,
    onDismiss: () -> Unit,
    onDetail: () -> Unit,
    onProgressIncrement: () -> Unit,
    onDelete: () -> Unit
) {
    com.kitsugi.animelist.ui.tv.components.TvDialog(
        onDismiss = onDismiss,
        title = entry.title,
        width = 360.dp
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TvDialogButton(label = "Detayları Göster", onClick = onDetail)
            TvDialogButton(
                label = "İlerlemeyi Artır (${entry.progress + 1})",
                onClick = onProgressIncrement
            )
            TvDialogButton(
                label = "Listeden Kaldır",
                onClick = onDelete,
                isDestructive = true
            )
            TvDialogButton(label = "Kapat", onClick = onDismiss)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvDialogButton(
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isFocused -> if (isDestructive) Color(0xFFE63946) else Color.White.copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    val contentColor = when {
        isFocused -> Color.White
        isDestructive -> Color(0xFFE63946)
        else -> Color.White.copy(alpha = 0.8f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, if (isFocused) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .tvClickable(shape = RoundedCornerShape(8.dp)) { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal
        )
    }
}


