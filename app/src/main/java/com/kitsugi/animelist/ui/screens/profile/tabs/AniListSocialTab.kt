package com.kitsugi.animelist.ui.screens.profile.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.ui.app.AniListProfileState
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun AniListSocialTab(
    state: AniListProfileState,
    accentColor: Color,
    isLandscape: Boolean,
    socialFilter: Int,
    onUserProfileClick: (userId: Int, username: String, avatarUrl: String?) -> Unit
) {
    val userList = if (socialFilter == 0) state.socialState.followers else state.socialState.following

    if (userList.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (socialFilter == 0) "Takipçi bulunamadı." else "Takip edilen kullanıcı bulunamadı.",
                    color = KitsugiColors.TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val gridColumns = if (isLandscape) 3 else 2
            userList.chunked(gridColumns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { u ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(KitsugiColors.Surface)
                                .clickable { onUserProfileClick(u.id, u.name, u.avatarUrl) }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = u.avatarUrl ?: "",
                                contentDescription = u.name,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = u.name,
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (rowItems.size < gridColumns) {
                        repeat(gridColumns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
