package com.kitsugi.animelist.ui.screens.mylist

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.components.KitsugiMediaEntryCard
import com.kitsugi.animelist.ui.theme.KitsugiColors

/**
 * Status-grouped list content rendering for MyListScreen.
 * Renders entries grouped by watch status with section headers.
 * Supports all layout IDs including grid_2col (2-column poster grid).
 */
internal fun LazyListScope.MyListGroupedContent(
    visibleEntries: List<MediaEntry>,
    selectedListLayoutId: String,
    titleLanguage: String,
    scoreFormat: String,
    hideScores: Boolean,
    onEntryClick: (MediaEntry) -> Unit,
    onIncrementProgress: (MediaEntry) -> Unit,
    onPosterLongClick: (String) -> Unit
) {
    val isGrid = selectedListLayoutId == "grid_2col"
    val statusOrder = listOf(
        WatchStatus.Watching,
        WatchStatus.Repeating,
        WatchStatus.Planned,
        WatchStatus.Paused,
        WatchStatus.Dropped,
        WatchStatus.Completed
    )

    statusOrder.forEach { status ->
        val itemsForStatus = visibleEntries.filter { it.status == status }
        if (itemsForStatus.isNotEmpty()) {
            item(key = "header_${status.name}") {
                val headerTitle = when (status) {
                    WatchStatus.Watching   -> "İzleniyor"
                    WatchStatus.Repeating  -> "Yeniden İzleniyor"
                    WatchStatus.Planned    -> "Planlandı"
                    WatchStatus.Paused     -> "Durduruldu"
                    WatchStatus.Dropped    -> "Bırakıldı"
                    WatchStatus.Completed  -> "İzlendi"
                }
                Text(
                    text = headerTitle,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp)
                )
            }

            if (isGrid) {
                // ─── 2-sütun grid: ikişerli satırlar ──────────────────────
                val rows = itemsForStatus.chunked(2)
                items(
                    items = rows,
                    key = { row -> "grid_row_${status.name}_${row.firstOrNull()?.let { "${it.source}_${it.id}" }}" }
                ) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { entry ->
                            KitsugiMediaEntryCard(
                                entry = entry,
                                layoutId = selectedListLayoutId,
                                modifier = Modifier.weight(1f),
                                onClick = { onEntryClick(entry) },
                                titleLanguage = titleLanguage,
                                scoreFormat = scoreFormat,
                                hideScores = hideScores,
                                onPosterLongClick = { imageUrl -> onPosterLongClick(imageUrl) }
                            )
                        }
                        // Tek sayıda eleman varsa sağ hücreyi boş bırak
                        if (row.size == 1) {
                            androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                // ─── Normal tek sütun liste ───────────────────────────────
                items(
                    items = itemsForStatus,
                    key = { entry -> "${entry.source}_${entry.id}" }
                ) { entry ->
                    KitsugiMediaEntryCard(
                        entry = entry,
                        layoutId = selectedListLayoutId,
                        onClick = { onEntryClick(entry) },
                        onIncrementClick = { onIncrementProgress(entry) },
                        titleLanguage = titleLanguage,
                        scoreFormat = scoreFormat,
                        hideScores = hideScores,
                        onPosterLongClick = { imageUrl -> onPosterLongClick(imageUrl) }
                    )
                    Spacer(modifier = Modifier.height(cardSpacingForLayout(selectedListLayoutId)))
                }
            }
        }
    }
}

/**
 * Flat (non-grouped) list content rendering for MyListScreen.
 * Used when the Completed filter is active.
 * Supports all layout IDs including grid_2col.
 */
internal fun LazyListScope.MyListFlatContent(
    visibleEntries: List<MediaEntry>,
    selectedListLayoutId: String,
    titleLanguage: String,
    scoreFormat: String,
    hideScores: Boolean,
    onEntryClick: (MediaEntry) -> Unit,
    onIncrementProgress: (MediaEntry) -> Unit,
    onPosterLongClick: (String) -> Unit
) {
    val isGrid = selectedListLayoutId == "grid_2col"

    if (isGrid) {
        // ─── 2-sütun grid: ikişerli satırlar ──────────────────────────────
        val rows = visibleEntries.chunked(2)
        items(
            items = rows,
            key = { row -> "flat_grid_row_${row.firstOrNull()?.let { "${it.source}_${it.id}" }}" }
        ) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { entry ->
                    KitsugiMediaEntryCard(
                        entry = entry,
                        layoutId = selectedListLayoutId,
                        modifier = Modifier.weight(1f),
                        onClick = { onEntryClick(entry) },
                        titleLanguage = titleLanguage,
                        scoreFormat = scoreFormat,
                        hideScores = hideScores,
                        onPosterLongClick = { imageUrl -> onPosterLongClick(imageUrl) }
                    )
                }
                // Tek sayıda eleman varsa sağ hücreyi boş bırak
                if (row.size == 1) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    } else {
        // ─── Normal tek sütun liste ───────────────────────────────────────
        items(
            items = visibleEntries,
            key = { entry -> "${entry.source}_${entry.id}" }
        ) { entry ->
            KitsugiMediaEntryCard(
                entry = entry,
                layoutId = selectedListLayoutId,
                onClick = { onEntryClick(entry) },
                onIncrementClick = { onIncrementProgress(entry) },
                titleLanguage = titleLanguage,
                scoreFormat = scoreFormat,
                hideScores = hideScores,
                onPosterLongClick = { imageUrl -> onPosterLongClick(imageUrl) }
            )
            Spacer(modifier = Modifier.height(cardSpacingForLayout(selectedListLayoutId)))
        }
    }
}

