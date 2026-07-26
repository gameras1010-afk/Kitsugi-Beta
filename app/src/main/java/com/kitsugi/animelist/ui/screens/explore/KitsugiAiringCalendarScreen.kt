package com.kitsugi.animelist.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitsugi.animelist.data.remote.AiringEntry
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.ui.theme.KitsugiColors

/**
 * Haftalık yayın takvimi tam ekran görünümü.
 * Anime tabına "Bu Hafta Yayında" kart widget'ından açılır.
 *
 * @param currentEntries Kullanıcının listesindeki girişler (vurgu için).
 * @param titleLanguage  Başlık dil tercihi (ROMAJI / ENGLISH / NATIVE).
 * @param onOpenAiringEntry Karta tıklandığında detay sayfasını aç.
 * @param onBackClick     Geri butonuna tıklandığında tetiklenir.
 * @param preferredSource Veri kaynağı (anilist / mal / simkl vb.)
 * @param viewModel      Inject edilmiş veya varsayılan ViewModel.
 */
@Composable
fun KitsugiAiringCalendarScreen(
    currentEntries: List<MediaEntry> = emptyList(),
    titleLanguage: String = "ROMAJI",
    onOpenAiringEntry: (AiringEntry) -> Unit = {},
    onBackClick: () -> Unit = {},
    preferredSource: String = "anilist",
    viewModel: KitsugiAiringCalendarViewModel = viewModel(key = "calendar_$preferredSource")
) {
    LaunchedEffect(preferredSource) {
        viewModel.loadSchedule(preferredSource = preferredSource)
    }

    val accentColor = KitsugiColors.Accent
    var showOnlyMyList by rememberSaveable { mutableStateOf(false) }
    var isGridView by rememberSaveable { mutableStateOf(true) }

    val filteredScheduleMap = remember(viewModel.weekSchedule, showOnlyMyList, currentEntries) {
        if (showOnlyMyList) {
            viewModel.weekSchedule.mapValues { (_, entries) ->
                entries.filter { entry ->
                    currentEntries.any { me ->
                        (entry.malId != null && me.malId == entry.malId) ||
                                me.malId == entry.aniListId ||
                                me.tmdbId == entry.aniListId
                    }
                }
            }
        } else {
            viewModel.weekSchedule
        }
    }

    val initialPage = remember { DAYS_ORDERED.indexOf(viewModel.selectedDay).coerceAtLeast(0) }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { DAYS_ORDERED.size }
    )

    // Pager swipe -> ViewModel update
    LaunchedEffect(pagerState.currentPage) {
        val targetDay = DAYS_ORDERED[pagerState.currentPage]
        if (viewModel.selectedDay != targetDay) {
            viewModel.selectDay(targetDay)
        }
    }

    // ViewModel update -> Pager animate
    LaunchedEffect(viewModel.selectedDay) {
        val targetPage = DAYS_ORDERED.indexOf(viewModel.selectedDay).coerceAtLeast(0)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
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
            onRefresh = { viewModel.loadSchedule(preferredSource = preferredSource, force = true) },
            onBackClick = onBackClick,
            isTmdb = preferredSource == "tmdb"
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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    val day = DAYS_ORDERED[page]
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
