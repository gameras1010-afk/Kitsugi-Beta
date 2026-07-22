package com.kitsugi.animelist.ui.screens.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.AiringEntry
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.launch
import java.util.Calendar

// ─── Yardımcı sabitler ─────────────────────────────────────────────────────

/** Calendar.DAY_OF_WEEK sırasına göre kısa gün isimleri (TR) */
private val DAY_LABELS = mapOf(
    Calendar.MONDAY    to "Pazartesi",
    Calendar.TUESDAY   to "Salı",
    Calendar.WEDNESDAY to "Çarşamba",
    Calendar.THURSDAY  to "Perşembe",
    Calendar.FRIDAY    to "Cuma",
    Calendar.SATURDAY  to "Cumartesi",
    Calendar.SUNDAY    to "Pazar"
)

/** Pazartesi'den Pazar'a sıralı gün listesi */
private val DAYS_ORDERED = listOf(
    Calendar.MONDAY,
    Calendar.TUESDAY,
    Calendar.WEDNESDAY,
    Calendar.THURSDAY,
    Calendar.FRIDAY,
    Calendar.SATURDAY,
    Calendar.SUNDAY
)

// ─── Ana ekran ──────────────────────────────────────────────────────────────

/**
 * Haftalık yayın takvimi tam ekran görünümü.
 * Anime tabına "Bu Hafta Yayında" kart widget'ından açılır.
 *
 * @param currentEntries Kullanıcının listesindeki girişler (vurgu için).
 * @param titleLanguage  Başlık dil tercihi (ROMAJI / ENGLISH / NATIVE).
 * @param onOpenApiDetail Karta tıklandığında detay sayfasını aç.
 * @param onBackClick     Geri butonuna tıklandığında tetiklenir.
 * @param viewModel      Inject edilmiş veya varsayılan ViewModel.
 */
@Composable
fun KitsugiAiringCalendarScreen(
    currentEntries: List<MediaEntry> = emptyList(),
    titleLanguage: String = "ROMAJI",
    onOpenAiringEntry: (AiringEntry) -> Unit = {},
    onBackClick: () -> Unit = {},
    viewModel: KitsugiAiringCalendarViewModel = viewModel()
) {
    val accentColor = KitsugiColors.Accent
    var showOnlyMyList by rememberSaveable { mutableStateOf(false) }
    var isGridView by rememberSaveable { mutableStateOf(true) }

    val filteredScheduleMap = remember(viewModel.weekSchedule, showOnlyMyList, currentEntries) {
        if (showOnlyMyList) {
            viewModel.weekSchedule.mapValues { (_, entries) ->
                entries.filter { entry ->
                    currentEntries.any { me ->
                        (entry.malId != null && me.malId == entry.malId) ||
                                me.malId == entry.aniListId
                    }
                }
            }
        } else {
            viewModel.weekSchedule
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
    ) {
        // ── Başlık çubuğu ──────────────────────────────────────────────────
        AiringCalendarHeader(
            totalCount = filteredScheduleMap.values.sumOf { it.size },
            isLoading = viewModel.isLoading,
            showOnlyMyList = showOnlyMyList,
            onShowOnlyMyListChange = { showOnlyMyList = it },
            isGridView = isGridView,
            onGridViewChange = { isGridView = it },
            onRefresh = { viewModel.loadSchedule() },
            onBackClick = onBackClick
        )

        // ── Gün sekmeleri ──────────────────────────────────────────────────
        AiringDayTabRow(
            days = DAYS_ORDERED,
            selectedDay = viewModel.selectedDay,
            scheduleMap = filteredScheduleMap,
            accentColor = accentColor,
            onDaySelected = { viewModel.selectDay(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── İçerik listesi ─────────────────────────────────────────────────
        when {
            viewModel.isLoading && viewModel.weekSchedule.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            }
            viewModel.errorMessage != null && viewModel.weekSchedule.isEmpty() -> {
                AiringErrorState(
                    message = viewModel.errorMessage.orEmpty(),
                    onRetry = { viewModel.loadSchedule() }
                )
            }
            else -> {
                AnimatedContent(
                    targetState = viewModel.selectedDay,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        slideInHorizontally { it * dir } + fadeIn() togetherWith
                                slideOutHorizontally { -it * dir } + fadeOut()
                    },
                    label = "day_anim"
                ) { day ->
                    val entries = filteredScheduleMap[day] ?: emptyList()
                    if (isGridView) {
                        AiringEntryGridList(
                            entries = entries,
                            currentEntries = currentEntries,
                            titleLanguage = titleLanguage,
                            onEntryClick = onOpenAiringEntry
                        )
                    } else {
                        AiringEntryList(
                            entries = entries,
                            currentEntries = currentEntries,
                            titleLanguage = titleLanguage,
                            onEntryClick = onOpenAiringEntry
                        )
                    }
                }
            }
        }
    }
}

// ─── Header ────────────────────────────────────────────────────────────────

@Composable
private fun AiringCalendarHeader(
    totalCount: Int,
    isLoading: Boolean,
    showOnlyMyList: Boolean,
    onShowOnlyMyListChange: (Boolean) -> Unit,
    isGridView: Boolean,
    onGridViewChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Geri",
                tint = KitsugiColors.TextPrimary
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = KitsugiColors.Accent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Bu Hafta Yayında",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KitsugiColors.TextPrimary
            )
            if (totalCount > 0) {
                Text(
                    text = "$totalCount bölüm",
                    style = MaterialTheme.typography.labelSmall,
                    color = KitsugiColors.TextSecondary
                )
            }
        }

        // Listemde Toggle Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (showOnlyMyList) KitsugiColors.AccentGreen.copy(0.18f) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (showOnlyMyList) KitsugiColors.AccentGreen else KitsugiColors.TextSecondary.copy(0.3f),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onShowOnlyMyListChange(!showOnlyMyList) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (showOnlyMyList) Icons.Rounded.PlaylistAddCheck else Icons.Rounded.PlaylistPlay,
                    contentDescription = null,
                    tint = if (showOnlyMyList) KitsugiColors.AccentGreen else KitsugiColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Listemde",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (showOnlyMyList) KitsugiColors.AccentGreen else KitsugiColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Grid/List toggle button
        IconButton(onClick = { onGridViewChange(!isGridView) }) {
            Icon(
                imageVector = if (isGridView) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                contentDescription = "Görünüm Değiştir",
                tint = KitsugiColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(onClick = onRefresh, enabled = !isLoading) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = KitsugiColors.Accent,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Yenile",
                    tint = KitsugiColors.TextSecondary
                )
            }
        }
    }
}

// ─── Gün Sekme Satırı ──────────────────────────────────────────────────────

@Composable
private fun AiringDayTabRow(
    days: List<Int>,
    selectedDay: Int,
    scheduleMap: Map<Int, List<AiringEntry>>,
    accentColor: Color,
    onDaySelected: (Int) -> Unit
) {
    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days) { day ->
            val isSelected = day == selectedDay
            val isToday = day == today
            val count = scheduleMap[day]?.size ?: 0
            AiringDayTab(
                label = DAY_LABELS[day] ?: "?",
                count = count,
                isSelected = isSelected,
                isToday = isToday,
                accentColor = accentColor,
                onClick = { onDaySelected(day) }
            )
        }
    }
}

@Composable
private fun AiringDayTab(
    label: String,
    count: Int,
    isSelected: Boolean,
    isToday: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> accentColor
        else       -> KitsugiColors.Surface
    }
    val textColor = when {
        isSelected -> KitsugiColors.Background
        isToday    -> accentColor
        else       -> KitsugiColors.TextSecondary
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(
                if (!isSelected && isToday)
                    Modifier.border(1.dp, accentColor, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
        if (count > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) KitsugiColors.Background.copy(0.15f) else accentColor.copy(0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.coerceAtMost(99).toString(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) KitsugiColors.Background else accentColor
                )
            }
        }
    }
}

// ─── Bölüm Listesi ─────────────────────────────────────────────────────────

@Composable
private fun AiringEntryList(
    entries: List<AiringEntry>,
    currentEntries: List<MediaEntry>,
    titleLanguage: String,
    onEntryClick: (AiringEntry) -> Unit
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Bu gün için yayın bulunamadı.",
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 3
        }
    }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entries, key = { "${it.malId}_${it.aniListId}_${it.episode}" }) { entry ->
                val isInList = remember(currentEntries, entry) {
                    currentEntries.any { me ->
                        (entry.malId != null && me.malId == entry.malId) ||
                                me.malId == entry.aniListId
                    }
                }
                AiringEntryCard(
                    entry = entry,
                    isInWatchingList = isInList,
                    titleLanguage = titleLanguage,
                    onClick = { onEntryClick(entry) }
                )
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KitsugiColors.Accent)
                    .tvClickable(shape = RoundedCornerShape(16.dp)) {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Yukarı Git",
                    tint = KitsugiColors.Background,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun AiringEntryGridList(
    entries: List<AiringEntry>,
    currentEntries: List<MediaEntry>,
    titleLanguage: String,
    onEntryClick: (AiringEntry) -> Unit
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Bu gün için yayın bulunamadı.",
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val gridState = rememberLazyGridState()
    val showScrollToTop by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 3
        }
    }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 105.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries, key = { "${it.aniListId}_${it.episode}" }) { entry ->
                val isInList = remember(currentEntries, entry) {
                    currentEntries.any { me ->
                        (entry.malId != null && me.malId == entry.malId) ||
                                me.malId == entry.aniListId
                    }
                }
                AiringEntryGridCard(
                    entry = entry,
                    isInWatchingList = isInList,
                    titleLanguage = titleLanguage,
                    onClick = { onEntryClick(entry) }
                )
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KitsugiColors.Accent)
                    .tvClickable(shape = RoundedCornerShape(16.dp)) {
                        scope.launch {
                            gridState.animateScrollToItem(0)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Yukarı Git",
                    tint = KitsugiColors.Background,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun AiringEntryGridCard(
    entry: AiringEntry,
    isInWatchingList: Boolean,
    titleLanguage: String = "ROMAJI",
    onClick: () -> Unit = {}
) {
    val displayTitle = when (titleLanguage) {
        "ENGLISH" -> entry.titleEnglish ?: entry.title
        "NATIVE"  -> entry.titleNative ?: entry.title
        else      -> entry.title
    }

    val accentColor = if (isInWatchingList) KitsugiColors.AccentGreen else KitsugiColors.Accent
    val hasAired = entry.hasAired()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(KitsugiColors.Surface)
            .then(
                if (isInWatchingList)
                    Modifier.border(1.dp, KitsugiColors.AccentGreen.copy(0.45f), RoundedCornerShape(18.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Kapak görseli
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(KitsugiColors.SurfaceSoft)
        ) {
            AsyncImage(
                model = entry.coverUrl,
                contentDescription = displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Yayınlandı overlay
            if (hasAired) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(0.55f))
                            )
                        )
                )
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Yayınlandı",
                    tint = KitsugiColors.AccentGreen,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                )
            }
            // İzliyorum ikonu overlay
            if (isInWatchingList) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(KitsugiColors.AccentGreen)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = "İzliyorum",
                        tint = KitsugiColors.Background,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Başlık
        Text(
            text = displayTitle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = KitsugiColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(38.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Episode & Time info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = KitsugiColors.TextSecondary,
                modifier = Modifier.size(11.dp)
            )
            AiringTimeText(entry = entry, episode = entry.episode)
        }
    }
}

// ─── Bölüm Kartı ───────────────────────────────────────────────────────────

@Composable
fun AiringEntryCard(
    entry: AiringEntry,
    isInWatchingList: Boolean,
    titleLanguage: String = "ROMAJI",
    onClick: () -> Unit = {}
) {
    val displayTitle = when (titleLanguage) {
        "ENGLISH" -> entry.titleEnglish ?: entry.title
        "NATIVE"  -> entry.titleNative ?: entry.title
        else      -> entry.title
    }

    val accentColor = if (isInWatchingList) KitsugiColors.AccentGreen else KitsugiColors.Accent
    val hasAired = entry.hasAired()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KitsugiColors.Surface)
            .then(
                if (isInWatchingList)
                    Modifier.border(1.dp, KitsugiColors.AccentGreen.copy(0.45f), RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kapak görseli
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KitsugiColors.SurfaceSoft)
        ) {
            AsyncImage(
                model = entry.coverUrl,
                contentDescription = displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Yayınlandı overlay
            if (hasAired) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(0.55f))
                            )
                        )
                )
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Yayınlandı",
                    tint = KitsugiColors.AccentGreen,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Başlık + bilgi
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = KitsugiColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Bölüm rozeti
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(0.18f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Bölüm ${entry.episode}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = accentColor
                    )
                }
                // Saat rozeti
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = KitsugiColors.TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    AiringTimeText(entry = entry)
                }
            }
        }

        // İzliyorum ikonu
        if (isInWatchingList) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.PlayCircle,
                contentDescription = "İzliyorum",
                tint = KitsugiColors.AccentGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun Long.secondsToLegibleText(): String {
    val remaining = this
    val days = remaining / 86400
    val hours = (remaining % 86400) / 3600
    val minutes = (remaining % 3600) / 60
    return when {
        days > 30 -> {
            val months = days / 30
            "$months ay sonra"
        }
        days > 7 -> {
            val weeks = days / 7
            "$weeks hafta sonra"
        }
        days >= 1 -> {
            if (hours > 0) {
                "$days gün $hours saat sonra"
            } else {
                "$days gün sonra"
            }
        }
        hours >= 1 -> {
            if (minutes > 0) {
                "$hours saat $minutes dakika sonra"
            } else {
                "$hours saat sonra"
            }
        }
        else -> {
            if (minutes > 0) "$minutes dakika sonra" else "Az sonra"
        }
    }
}

@Composable
private fun AiringTimeText(entry: AiringEntry, episode: Int? = null) {
    var text by remember(entry) { mutableStateOf(entry.formattedTime()) }

    if (!entry.hasAired()) {
        LaunchedEffect(entry) {
            while (true) {
                val remaining = entry.airingAt - System.currentTimeMillis() / 1000L
                if (remaining <= 0) {
                    text = "${entry.formattedTime()} • Yayınlandı"
                    break
                }
                text = "${entry.formattedTime()} • ${remaining.secondsToLegibleText()}"
                val delayTime = if (remaining > 86400) 60000L else 10000L
                kotlinx.coroutines.delay(delayTime)
            }
        }
    } else {
        text = "${entry.formattedTime()} • Yayınlandı"
    }

    val displayText = if (episode != null) "$episode. Bölüm • $text" else text

    Text(
        text = displayText,
        style = MaterialTheme.typography.labelSmall,
        color = KitsugiColors.TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// ─── Hata ekranı ───────────────────────────────────────────────────────────

@Composable
private fun AiringErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = KitsugiColors.TextSecondary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Takvim yüklenemedi",
            style = MaterialTheme.typography.titleSmall,
            color = KitsugiColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = KitsugiColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(KitsugiColors.Accent)
                .clickable(onClick = onRetry)
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Tekrar Dene",
                color = KitsugiColors.Background,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

