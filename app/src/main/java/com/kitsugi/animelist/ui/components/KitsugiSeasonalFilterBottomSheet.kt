@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import java.util.Calendar

enum class KitsugiSeason(val apiValue: String, val turkishName: String, val icon: ImageVector) {
    WINTER("WINTER", "Kış", Icons.Rounded.AcUnit),
    SPRING("SPRING", "İlkbahar", Icons.Rounded.LocalFlorist),
    SUMMER("SUMMER", "Yaz", Icons.Rounded.WbSunny),
    FALL("FALL", "Sonbahar", Icons.Rounded.Forest);

    companion object {
        fun fromApiValue(value: String): KitsugiSeason {
            return entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: SUMMER
        }

        fun current(): KitsugiSeason {
            val calendar = Calendar.getInstance()
            return when (calendar.get(Calendar.MONTH)) {
                Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> WINTER
                Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> SPRING
                Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> SUMMER
                else -> FALL
            }
        }
    }
}

enum class KitsugiSeasonalSort(val apiValue: String, val turkishName: String) {
    POPULARITY("POPULARITY_DESC", "Popülerlik"),
    SCORE("SCORE_DESC", "Puan"),
    MEMBERS("MEMBERS_DESC", "Üyeler"),
    START_DATE("START_DATE_DESC", "Yeni");

    companion object {
        fun fromApiValue(value: String): KitsugiSeasonalSort {
            return entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: POPULARITY
        }
    }
}

@Composable
fun KitsugiSeasonalFilterBottomSheet(
    initialSeason: String,
    initialYear: Int,
    initialSort: String,
    onDismissRequest: () -> Unit,
    onApply: (season: String, year: Int, sort: String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val accentColor = LocalKitsugiAccent.current

    var selectedSeason by remember { mutableStateOf(KitsugiSeason.fromApiValue(initialSeason)) }
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedSort by remember { mutableStateOf(KitsugiSeasonalSort.fromApiValue(initialSort)) }

    val currentCalendarYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val currentCalendarSeason = remember { KitsugiSeason.current() }

    // Generates list of years from currentYear + 1 down to 1970
    val yearList = remember { (currentCalendarYear + 1 downTo 1970).toList() }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(selectedYear) {
        val index = yearList.indexOf(selectedYear)
        if (index >= 0) {
            lazyListState.animateScrollToItem(index)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = KitsugiColors.Surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(KitsugiColors.TextMuted.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── 1. Header Action Row (İptal - Uygula) ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = "İptal",
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = {
                        onApply(selectedSeason.apiValue, selectedYear, selectedSort.apiValue)
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Uygula",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ─── 2. Quick Relative Season Selector (Önceki - Şimdiki - Sonraki) ───
            val isCurrentActive = selectedYear == currentCalendarYear && selectedSeason == currentCalendarSeason
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(KitsugiColors.SurfaceSoft)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Önceki
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            val currentIndex = KitsugiSeason.entries.indexOf(selectedSeason)
                            if (currentIndex == 0) {
                                selectedSeason = KitsugiSeason.FALL
                                selectedYear -= 1
                            } else {
                                selectedSeason = KitsugiSeason.entries[currentIndex - 1]
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Önceki",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Şimdiki
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isCurrentActive) accentColor.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable {
                            selectedYear = currentCalendarYear
                            selectedSeason = currentCalendarSeason
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCurrentActive) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "Şimdiki",
                            color = if (isCurrentActive) accentColor else KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Sonraki
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            val currentIndex = KitsugiSeason.entries.indexOf(selectedSeason)
                            if (currentIndex == KitsugiSeason.entries.lastIndex) {
                                selectedSeason = KitsugiSeason.WINTER
                                selectedYear += 1
                            } else {
                                selectedSeason = KitsugiSeason.entries[currentIndex + 1]
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sonraki",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ─── 3. 4 Season Icons Row ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                KitsugiSeason.entries.forEach { season ->
                    val isSelected = selectedSeason == season
                    val bgShape = RoundedCornerShape(18.dp)

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(bgShape)
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.25f)
                                else KitsugiColors.SurfaceSoft
                            )
                            .clickable { selectedSeason = season },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = season.icon,
                            contentDescription = season.turkishName,
                            tint = if (isSelected) accentColor else KitsugiColors.TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ─── 4. Horizontal Scrollable Year Chips ───
            LazyRow(
                state = lazyListState,
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(yearList) { year ->
                    val isSelected = year == selectedYear
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedYear = year },
                        label = {
                            Text(
                                text = year.toString(),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = KitsugiColors.SurfaceSoft,
                            labelColor = KitsugiColors.TextPrimary,
                            selectedContainerColor = accentColor.copy(alpha = 0.35f),
                            selectedLabelColor = accentColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ─── 5. Sort Chip Dropdown Menu ───
            var sortMenuExpanded by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(KitsugiColors.SurfaceSoft)
                        .clickable { sortMenuExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "Sıralama",
                        tint = KitsugiColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedSort.turkishName,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "▾",
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier.background(KitsugiColors.Surface)
                ) {
                    KitsugiSeasonalSort.entries.forEach { sortOption ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = sortOption.turkishName,
                                    color = if (selectedSort == sortOption) accentColor else KitsugiColors.TextPrimary,
                                    fontWeight = if (selectedSort == sortOption) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedSort = sortOption
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
