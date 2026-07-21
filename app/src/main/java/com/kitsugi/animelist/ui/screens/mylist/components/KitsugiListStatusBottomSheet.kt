package com.kitsugi.animelist.ui.screens.mylist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable

data class StatusItemData(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val count: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiListStatusBottomSheet(
    entries: List<MediaEntry>,
    selectedStatusFilterId: String,
    showAdultContent: Boolean = false,
    onStatusSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val accentColor = LocalKitsugiAccent.current

    val totalCount = entries.size
    val watchingCount = entries.count { it.status == WatchStatus.Watching }
    val completedCount = entries.count { it.status == WatchStatus.Completed }
    val plannedCount = entries.count { it.status == WatchStatus.Planned }
    val pausedCount = entries.count { it.status == WatchStatus.Paused }
    val droppedCount = entries.count { it.status == WatchStatus.Dropped }
    val adultCount = entries.count { it.isAdult }
    val favoritesCount = entries.count { it.isFavorite }

    val statusItems = mutableListOf(
        StatusItemData("all", "Tümü", Icons.Rounded.FormatListBulleted, totalCount),
        StatusItemData("watching", "İzleniyor", Icons.Rounded.PlayCircle, watchingCount),
        StatusItemData("completed", "Tamamlandı", Icons.Rounded.CheckCircle, completedCount),
        StatusItemData("planned", "Planlanan", Icons.Rounded.Schedule, plannedCount),
        StatusItemData("paused", "Durduruldu", Icons.Rounded.PauseCircle, pausedCount),
        StatusItemData("dropped", "Bırakıldı", Icons.Rounded.StopCircle, droppedCount)
    )

    if (showAdultContent && adultCount > 0) {
        statusItems.add(StatusItemData("adult", "Yetişkin (+18)", Icons.Rounded.Star, adultCount))
    }

    if (favoritesCount > 0) {
        statusItems.add(StatusItemData("favorites", "Favoriler", Icons.Rounded.Star, favoritesCount))
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = KitsugiColors.Surface,
        scrimColor = KitsugiColors.Background.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(KitsugiColors.TextMuted.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statusItems.forEach { item ->
                val isSelected = selectedStatusFilterId == item.id

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.15f) else KitsugiColors.SurfaceSoft.copy(alpha = 0.4f)
                        )
                        .tvClickable(shape = RoundedCornerShape(16.dp), onClick = {
                            onStatusSelected(item.id)
                            onDismissRequest()
                        })
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (isSelected) accentColor else KitsugiColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = item.title,
                            color = if (isSelected) accentColor else KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = item.count.toString(),
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
