package com.kitsugi.animelist.ui.screens.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun UserProfileHeaderCard(
    state: OtherUserProfileState,
    fallbackUsername: String?,
    fallbackAvatar: String?,
    accentColor: Color,
    onImageClick: (urls: List<String>, idx: Int, title: String) -> Unit
) {
    val avatarUrl = (state.avatarUrl ?: fallbackAvatar)?.takeIf { it.isNotBlank() }
    val bannerUrl = state.bannerUrl?.takeIf { it.isNotBlank() }
    val imageList = listOfNotNull(avatarUrl, bannerUrl)
    val username = state.name.ifBlank { fallbackUsername ?: "Kullanıcı" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .then(if (!bannerUrl.isNullOrBlank()) Modifier.clickable {
                onImageClick(imageList, imageList.indexOf(bannerUrl).coerceAtLeast(0), "$username Banner")
            } else Modifier)
    ) {
        if (!bannerUrl.isNullOrBlank()) {
            AsyncImage(model = bannerUrl, contentDescription = "Banner",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(modifier = Modifier.fillMaxSize()
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(accentColor, KitsugiColors.AccentBlue))))
        }
        Box(modifier = Modifier.fillMaxSize()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(Color.Transparent, KitsugiColors.Background.copy(alpha = 0.85f)))))

        Row(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = avatarUrl ?: "",
                contentDescription = "Avatar",
                modifier = Modifier.size(68.dp).clip(CircleShape)
                    .border(3.dp, KitsugiColors.Background, CircleShape)
                    .then(if (!avatarUrl.isNullOrBlank()) Modifier.clickable {
                        onImageClick(imageList, imageList.indexOf(avatarUrl).coerceAtLeast(0), "$username Profil Resmi")
                    } else Modifier),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = state.name.ifBlank { fallbackUsername ?: "AniList Kullanıcısı" },
                        color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val badge = state.donatorBadge
                    if (!badge.isNullOrBlank() && state.donatorTier > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = badge, color = accentColor, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                if (state.isFollower) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Sizi takip ediyor", color = KitsugiColors.AccentGreen,
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun UserProfileActionButtons(
    state: OtherUserProfileState,
    accentColor: Color,
    userId: Int,
    viewModel: KitsugiUserProfileViewModel,
    onOpenUserMediaList: (Int, MediaType) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {

        Box(modifier = Modifier.weight(1.2f).clip(RoundedCornerShape(14.dp))
            .background(if (state.isFollowing) KitsugiColors.SurfaceStrong else accentColor)
            .clickable(enabled = !state.isFollowLoading) { viewModel.toggleFollow() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center) {
            if (state.isFollowLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp),
                    color = if (state.isFollowing) KitsugiColors.TextPrimary else KitsugiColors.Background,
                    strokeWidth = 2.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = if (state.isFollowing) Icons.Rounded.Check else Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        tint = if (state.isFollowing) KitsugiColors.TextPrimary else KitsugiColors.Background,
                        modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (state.isFollowing) "Takip Ediliyorsun" else "Takip Et",
                        color = if (state.isFollowing) KitsugiColors.TextPrimary else KitsugiColors.Background,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        listOf("Anime Listesi" to MediaType.Anime, "Manga Listesi" to MediaType.Manga).forEach { (label, type) ->
            val tint = if (type == MediaType.Anime) accentColor else KitsugiColors.AccentOrange
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                .background(KitsugiColors.Surface)
                .clickable { onOpenUserMediaList(userId, type) }
                .padding(vertical = 10.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ListAlt, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = label, color = KitsugiColors.TextPrimary, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
