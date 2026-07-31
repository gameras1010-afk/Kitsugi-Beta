package com.kitsugi.animelist.ui.screens.search.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.RangeSlider
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import com.kitsugi.animelist.ui.components.KitsugiSheetOrDialog
import kotlin.math.roundToInt

// ─── ChipWithMenu ────────────────────────────────────────────────────────────
/**
 * A FilterChip that opens a DropdownMenu for single-value selection.
 * Ported from AniHyou's ChipWithMenu.kt.
 */
@Composable
fun <T> KitsugiChipWithMenu(
    title: String,
    values: List<T>,
    selectedValue: T?,
    onValueSelected: (T?) -> Unit,
    modifier: Modifier = Modifier,
    valueString: @Composable (T) -> String = { it.toString() },
) {
    var menuOpened by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilterChip(
            selected = selectedValue != null,
            onClick = { menuOpened = true },
            label = { Text(text = selectedValue?.let { valueString(it) } ?: title) }
        )
        DropdownMenu(
            expanded = menuOpened,
            onDismissRequest = { menuOpened = false },
            modifier = Modifier
                .widthIn(min = 160.dp)
                .heightIn(max = 280.dp)
        ) {
            values.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedValue == item) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Text(text = valueString(item))
                        }
                    },
                    onClick = {
                        onValueSelected(item.takeIf { it != selectedValue })
                        menuOpened = false
                    }
                )
            }
        }
    }
}

// ─── ChipWithRange ───────────────────────────────────────────────────────────
/**
 * A FilterChip that opens a ModalBottomSheet with a RangeSlider.
 * Ported from AniHyou's ChipWithRange.kt.
 */
@Composable
fun KitsugiChipWithRange(
    title: String,
    startValue: Float?,
    endValue: Float?,
    modifier: Modifier = Modifier,
    minValue: Float = 0f,
    maxValue: Float,
    onValueChanged: (IntRange?) -> Unit,
) {
    val hasValue = startValue != null || endValue != null

    var rangeStart by remember { mutableIntStateOf((startValue ?: minValue).roundToInt()) }
    var rangeEnd by remember { mutableIntStateOf((endValue ?: maxValue).roundToInt()) }
    var sliderStart by remember { mutableStateOf(startValue ?: minValue) }
    var sliderEnd by remember { mutableStateOf(endValue ?: maxValue) }
    var sheetOpened by remember { mutableStateOf(false) }

    LaunchedEffect(startValue, endValue) {
        sliderStart = startValue ?: minValue
        sliderEnd = endValue ?: maxValue
        rangeStart = (startValue ?: minValue).roundToInt()
        rangeEnd = (endValue ?: maxValue).roundToInt()
    }

    FilterChip(
        selected = hasValue,
        onClick = { sheetOpened = true },
        modifier = modifier,
        label = {
            Text(text = if (hasValue) "$rangeStart – $rangeEnd" else title)
        }
    )

    if (sheetOpened) {
        KitsugiSheetOrDialog(onDismiss = { sheetOpened = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(text = title)
                Text(text = "$rangeStart – $rangeEnd")
                RangeSlider(
                    value = sliderStart..sliderEnd,
                    onValueChange = { range ->
                        sliderStart = range.start
                        sliderEnd = range.endInclusive
                        rangeStart = range.start.roundToInt()
                        rangeEnd = range.endInclusive.roundToInt()
                    },
                    valueRange = minValue..maxValue,
                    onValueChangeFinished = {
                        val start = sliderStart.roundToInt()
                        val end = sliderEnd.roundToInt()
                        if (start == minValue.roundToInt() && end == maxValue.roundToInt()) {
                            onValueChanged(null)
                        } else {
                            onValueChanged(start..end)
                        }
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

// ─── SortChip ────────────────────────────────────────────────────────────────
/**
 * A dual-chip Sort selector: one chip for the sort field, one for asc/desc.
 * Ported from AniHyou's MediaSearchSortChip.kt.
 */
@Composable
fun KitsugiSearchSortChip(
    sortSearch: KitsugiMediaSortSearch,
    isDescending: Boolean,
    onSortChanged: (KitsugiMediaSortSearch, Boolean) -> Unit,
) {
    var openDialog by remember { mutableStateOf(false) }

    if (openDialog) {
        DialogWithRadioSelection(
            values = KitsugiMediaSortSearch.entries.toTypedArray(),
            defaultValue = sortSearch,
            title = "Sıralama",
            isDeselectable = false,
            onConfirm = { selected ->
                openDialog = false
                if (selected != null) onSortChanged(selected, isDescending)
            },
            onDismiss = { openDialog = false }
        )
    }

    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = { openDialog = true },
            label = { Text(text = sortSearch.localized()) }
        )
        if (sortSearch != KitsugiMediaSortSearch.SEARCH_MATCH) {
            AssistChip(
                onClick = { onSortChanged(sortSearch, !isDescending) },
                label = { Text(text = if (isDescending) "Azalan" else "Artan") },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
