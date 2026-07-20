package com.kitsugi.animelist.ui.components.posteroptions

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kitsugi.animelist.ui.components.KitsugiSheetOrDialog

/**
 * V2-A07 – PosterOptionsDialog
 *
 * Poster uzun basma ile tetiklenen seçenek diyaloğu.
 * NuvioTV posteroptions/Dialog.kt referans alındı.
 */
@Composable
fun PosterOptionsDialog(
    state: PosterOptionsState,
    onOptionSelected: (PosterOption) -> Unit
) {
    if (!state.isVisible) return

    KitsugiSheetOrDialog(
        onDismiss = state::dismiss,
        heightFraction = 0.55f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Media header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!state.mediaCoverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = state.mediaCoverUrl,
                        contentDescription = state.mediaTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp, 80.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.mediaTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Options
            state.availableOptions.forEach { option ->
                PosterOptionRow(
                    option = option,
                    onClick = {
                        onOptionSelected(option)
                        state.dismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PosterOptionRow(
    option: PosterOption,
    onClick: () -> Unit
) {
    val (icon, label) = when (option) {
        PosterOption.ADD_TO_LIST -> Icons.Rounded.PlaylistAdd to "Listeye Ekle"
        PosterOption.SET_STATUS -> Icons.Rounded.Edit to "Durumu Değiştir"
        PosterOption.ADD_TO_COLLECTION -> Icons.Rounded.FolderCopy to "Koleksiyona Ekle"
        PosterOption.SHARE -> Icons.Rounded.Share to "Paylaş"
        PosterOption.OPEN_IN_BROWSER -> Icons.Rounded.OpenInNew to "Tarayıcıda Aç"
        PosterOption.EDIT_PROGRESS -> Icons.Rounded.Numbers to "İlerlemeyi Düzenle"
        PosterOption.REMOVE_FROM_LIST -> Icons.Rounded.RemoveCircle to "Listeden Kaldır"
    }

    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (option == PosterOption.REMOVE_FROM_LIST)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (option == PosterOption.REMOVE_FROM_LIST)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
