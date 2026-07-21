package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle

@Composable
internal fun StaffCharacterRoleCard(
    role: com.kitsugi.animelist.data.remote.KitsugiStaffCharacterRole,
    titleLanguage: String = "ROMAJI",
    onCharacterClick: (characterId: Int, characterSource: String, name: String?, imageUrl: String?) -> Unit,
    onMediaClick: (mediaId: Int, mediaType: String, mediaSource: String) -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val displayMediaTitle = role.mediaTitle
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tvClickable(shape = RoundedCornerShape(16.dp), onClick = { onCharacterClick(role.characterId, role.characterSource, role.characterName, role.characterImageUrl) }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KitsugiColors.SurfaceSoft)
            ) {
                if (!role.characterImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = role.characterImageUrl,
                        contentDescription = role.characterName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = role.characterName.take(2).uppercase(),
                            color = KitsugiColors.TextMuted,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = role.characterName,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Seslendirdiği Karakter (${role.characterRole})",
                    color = accentColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(start = 21.dp, top = 2.dp, bottom = 2.dp)
                .width(2.dp)
                .height(10.dp)
                .background(KitsugiColors.SurfaceSoft)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tvClickable(shape = RoundedCornerShape(16.dp), onClick = { onMediaClick(role.mediaId, role.mediaType, role.mediaSource) }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KitsugiColors.SurfaceSoft)
            ) {
                if (!role.mediaImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = role.mediaImageUrl,
                        contentDescription = displayMediaTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = displayMediaTitle.take(2).uppercase(),
                            color = KitsugiColors.TextMuted,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = displayMediaTitle,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Yapım (${role.mediaType.replaceFirstChar { it.uppercase() }})",
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
internal fun StaffMediaWorkRow(
    work: com.kitsugi.animelist.data.remote.KitsugiStaffMediaWork,
    titleLanguage: String = "ROMAJI",
    onMediaClick: (mediaId: Int, mediaType: String, mediaSource: String) -> Unit
) {
    val displayTitle = work.getDisplayTitle(titleLanguage)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = { onMediaClick(work.mediaId, work.mediaType, work.source) })
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KitsugiColors.SurfaceSoft)
        ) {
            if (!work.mediaImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = work.mediaImageUrl,
                    contentDescription = displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = displayTitle.take(2).uppercase(),
                        color = KitsugiColors.TextMuted,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${work.mediaType.replaceFirstChar { it.uppercase() }} • Rol: ${work.staffRole}",
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


