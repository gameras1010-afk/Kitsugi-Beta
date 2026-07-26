package com.kitsugi.animelist.ui.screens.profile.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.app.ProfileFavoriteItem
import com.kitsugi.animelist.ui.components.KitsugiNsfwImage
import com.kitsugi.animelist.ui.screens.profile.KitsugiUserProfileViewModel
import com.kitsugi.animelist.ui.screens.profile.OtherUserProfileState
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun UserProfileFavoritesTab(
    state: OtherUserProfileState,
    accentColor: Color,
    appSettings: AppSettings,
    viewModel: KitsugiUserProfileViewModel,
    isLandscape: Boolean,
    onFavoriteMediaClick: (Int, MediaType, String, String, String?) -> Unit,
    onFavoriteCharacterClick: (Int, String, String?, String?) -> Unit,
    onFavoriteStaffClick: (Int, String, String?, String?) -> Unit,
    onFavoriteStudioClick: ((Int, String, String?, String?) -> Unit)?,
    onOpenSheet: (String, List<ProfileFavoriteItem>) -> Unit
) {
    val favoritesFilter = viewModel.favoritesFilter
    val currentFavList = when (favoritesFilter) {
        0 -> state.favoriteAnime
        1 -> state.favoriteManga
        2 -> state.favoriteCharacters
        3 -> state.favoriteStaff
        4 -> state.favoriteStudios
        else -> emptyList()
    }

    val filterTitle = when (favoritesFilter) {
        0 -> "Favori Animeler"
        1 -> "Favori Mangalar"
        2 -> "Favori Karakterler"
        3 -> "Favori Ekip"
        4 -> "Favori Stüdyolar"
        else -> "Favoriler"
    }

    if (currentFavList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Favori öğe bulunamadı.", color = KitsugiColors.TextMuted)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$filterTitle (${currentFavList.size})",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(KitsugiColors.SurfaceStrong)
                        .clickable {
                            onOpenSheet(filterTitle, currentFavList)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Tümünü Gör",
                        color = accentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val gridColumns = if (isLandscape) 3 else 2
            currentFavList.chunked(gridColumns).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    item.id.toIntOrNull()?.let { id ->
                                        when (favoritesFilter) {
                                            0 -> onFavoriteMediaClick(id, MediaType.Anime, "anilist", item.title, item.imageUrl)
                                            1 -> onFavoriteMediaClick(id, MediaType.Manga, "anilist", item.title, item.imageUrl)
                                            2 -> onFavoriteCharacterClick(id, "anilist", item.title, item.imageUrl)
                                            3 -> onFavoriteStaffClick(id, "anilist", item.title, item.imageUrl)
                                            4 -> onFavoriteStudioClick?.invoke(id, "anilist", item.title, item.imageUrl)
                                        }
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.7f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(KitsugiColors.Surface)
                            ) {
                                if (item.imageUrl.isNotBlank()) {
                                    KitsugiNsfwImage(
                                        model = item.imageUrl,
                                        contentDescription = item.title,
                                        isAdult = item.isAdult,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(imageVector = Icons.Rounded.Favorite, contentDescription = null, tint = accentColor)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.title,
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (rowItems.size < gridColumns) {
                        repeat(gridColumns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            val currentHasNext = when (favoritesFilter) {
                0 -> state.favAnimeHasNext
                1 -> state.favMangaHasNext
                2 -> state.favCharHasNext
                3 -> state.favStaffHasNext
                4 -> state.favStudioHasNext
                else -> false
            }
            val currentFavCategory = when (favoritesFilter) {
                0 -> "anime"
                1 -> "manga"
                2 -> "characters"
                3 -> "staff"
                4 -> "studios"
                else -> "anime"
            }

            if (currentHasNext) {
                LaunchedEffect(currentFavList.size) {
                    viewModel.loadMoreFavorites(currentFavCategory)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = accentColor,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}
