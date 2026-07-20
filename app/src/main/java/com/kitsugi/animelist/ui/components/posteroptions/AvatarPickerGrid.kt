package com.kitsugi.animelist.ui.components.posteroptions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent

/**
 * V2-A07 – AvatarPickerGrid
 *
 * Profil avatarı seçim ızgarası.
 * NuvioTV AvatarPickerGrid.kt referans alındı.
 */

data class AvatarOption(
    val id: String,
    val imageUrl: String,
    val label: String? = null
)

@Composable
fun AvatarPickerGrid(
    avatarOptions: List<AvatarOption>,
    selectedAvatarId: String?,
    onAvatarSelected: (AvatarOption) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 4
) {
    val accentColor = LocalKitsugiAccent.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(avatarOptions, key = { it.id }) { avatar ->
            val isSelected = avatar.id == selectedAvatarId
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) accentColor else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onAvatarSelected(avatar) },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = avatar.imageUrl,
                    contentDescription = avatar.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(accentColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Seçili",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
