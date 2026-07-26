package com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun QualitySheet(
    streamSources: List<StreamSource>,
    selectedIndex: Int,
    isLoading: Boolean,
    onSelect: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    dismissSheet: Boolean = false,
    modifier: Modifier = Modifier,
) {
    PlayerSheet(onDismissRequest = onDismissRequest, dismissEvent = dismissSheet) {
        Column(modifier = modifier) {
            TrackSheetTitle(title = "Kalite / Kaynak")

            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = KitsugiColors.AccentBlue)
                }
            } else {
                LazyColumn {
                    itemsIndexed(streamSources) { index, source ->
                        QualitySourceRow(
                            source = source,
                            isSelected = index == selectedIndex,
                            onClick = { onSelect(index) },
                        )
                        if (index < streamSources.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QualitySourceRow(
    source: StreamSource,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.addonName,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                fontStyle = if (isSelected) FontStyle.Italic else FontStyle.Normal,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!source.name.isNullOrBlank()) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
