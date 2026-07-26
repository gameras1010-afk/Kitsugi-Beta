package com.kitsugi.animelist.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlin.math.roundToInt

// ─── Generic Radio-Selection Dialog ──────────────────────────────────────────

@Composable
fun <T> DialogWithRadioSelection(
    title: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T?) -> Unit,
    onDismiss: () -> Unit,
    optionLabel: (T) -> String = { it.toString() }
) {
    val accentColor = LocalKitsugiAccent.current
    val isTv = LocalIsTv.current
    var tempSelection by remember { mutableStateOf(selectedOption) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = KitsugiColors.Surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, KitsugiColors.Border),
                modifier = Modifier
                    .width(if (isTv) 400.dp else 320.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = title,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(options) { option ->
                            val isSelected = option == tempSelection
                            val rowShape = RoundedCornerShape(12.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(rowShape)
                                    .background(
                                        if (isSelected) accentColor.copy(alpha = 0.08f)
                                        else Color.Transparent
                                    )
                                    .tvClickable(shape = rowShape) {
                                        tempSelection = if (isSelected) null else option
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { tempSelection = if (isSelected) null else option },
                                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = optionLabel(option),
                                    color = if (isSelected) accentColor else KitsugiColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(text = "İptal", color = KitsugiColors.TextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onOptionSelected(tempSelection)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Uygula", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ─── Year & Season Selection Dialog ──────────────────────────────────────────

@Composable
fun DialogWithYearSeasonSelection(
    title: String,
    selectedStartYear: Int?,
    selectedEndYear: Int?,
    selectedSeason: String?,
    onStartYearSelected: (Int?) -> Unit,
    onEndYearSelected: (Int?) -> Unit,
    onSeasonSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val isTv = LocalIsTv.current

    var tempStartYear by remember { mutableStateOf(selectedStartYear) }
    var tempEndYear by remember { mutableStateOf(selectedEndYear) }
    var tempSeason by remember { mutableStateOf(selectedSeason) }

    val years = remember { (1970..2026).toList().reversed() }
    val seasons = listOf(
        "WINTER" to "❄️ Kış",
        "SPRING" to "🌱 İlkbahar",
        "SUMMER" to "☀️ Yaz",
        "FALL" to "🍂 Sonbahar"
    )

    var startYearExpanded by remember { mutableStateOf(false) }
    var endYearExpanded by remember { mutableStateOf(false) }
    var seasonExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = KitsugiColors.Surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, KitsugiColors.Border),
                modifier = Modifier
                    .width(if (isTv) 420.dp else 340.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = title,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Start Year
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Başlangıç Yılı",
                            color = KitsugiColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Box {
                            OutlinedButton(
                                onClick = { startYearExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = KitsugiColors.TextPrimary),
                                border = BorderStroke(1.dp, KitsugiColors.Border)
                            ) {
                                Text(text = tempStartYear?.toString() ?: "Seçilmedi", fontSize = 14.sp)
                            }
                            DropdownMenu(
                                expanded = startYearExpanded,
                                onDismissRequest = { startYearExpanded = false },
                                modifier = Modifier.heightIn(max = 240.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Seçilmedi") },
                                    onClick = { tempStartYear = null; startYearExpanded = false }
                                )
                                years.forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text(y.toString()) },
                                        onClick = { tempStartYear = y; startYearExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    // End Year
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Bitiş Yılı",
                            color = KitsugiColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Box {
                            OutlinedButton(
                                onClick = { endYearExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = KitsugiColors.TextPrimary),
                                border = BorderStroke(1.dp, KitsugiColors.Border)
                            ) {
                                Text(text = tempEndYear?.toString() ?: "Seçilmedi", fontSize = 14.sp)
                            }
                            DropdownMenu(
                                expanded = endYearExpanded,
                                onDismissRequest = { endYearExpanded = false },
                                modifier = Modifier.heightIn(max = 240.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Seçilmedi") },
                                    onClick = { tempEndYear = null; endYearExpanded = false }
                                )
                                years.forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text(y.toString()) },
                                        onClick = { tempEndYear = y; endYearExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    // Season
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Sezon",
                            color = KitsugiColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Box {
                            OutlinedButton(
                                onClick = { seasonExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = KitsugiColors.TextPrimary),
                                border = BorderStroke(1.dp, KitsugiColors.Border)
                            ) {
                                val display = seasons.firstOrNull { it.first == tempSeason }?.second ?: "Seçilmedi"
                                Text(text = display, fontSize = 14.sp)
                            }
                            DropdownMenu(
                                expanded = seasonExpanded,
                                onDismissRequest = { seasonExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Seçilmedi") },
                                    onClick = { tempSeason = null; seasonExpanded = false }
                                )
                                seasons.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.second) },
                                        onClick = { tempSeason = s.first; seasonExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(text = "İptal", color = KitsugiColors.TextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onStartYearSelected(tempStartYear)
                                onEndYearSelected(tempEndYear)
                                onSeasonSelected(tempSeason)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Uygula", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ─── Score Range Selection Dialog ─────────────────────────────────────────────

@Composable
fun DialogWithScoreRangeSelection(
    title: String,
    selectedMinScore: Int?,
    selectedMaxScore: Int?,
    onScoreRangeSelected: (Int?, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val isTv = LocalIsTv.current

    var sliderStart by remember { mutableFloatStateOf(selectedMinScore?.toFloat() ?: 0f) }
    var sliderEnd by remember { mutableFloatStateOf(selectedMaxScore?.toFloat() ?: 100f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = KitsugiColors.Surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, KitsugiColors.Border),
                modifier = Modifier
                    .width(if (isTv) 400.dp else 320.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = title,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "%${sliderStart.roundToInt()} - %${sliderEnd.roundToInt()}",
                            color = accentColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        RangeSlider(
                            value = sliderStart..sliderEnd,
                            onValueChange = { range ->
                                sliderStart = range.start
                                sliderEnd = range.endInclusive
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = KitsugiColors.Border
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(text = "İptal", color = KitsugiColors.TextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val min = sliderStart.roundToInt()
                                val max = sliderEnd.roundToInt()
                                if (min == 0 && max == 100) {
                                    onScoreRangeSelected(null, null)
                                } else {
                                    onScoreRangeSelected(min, max)
                                }
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Uygula", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
