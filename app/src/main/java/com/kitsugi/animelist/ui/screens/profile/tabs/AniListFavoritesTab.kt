package com.kitsugi.animelist.ui.screens.profile.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
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
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.app.AniListProfileState
import com.kitsugi.animelist.ui.app.KitsugiProfileViewModel
import com.kitsugi.animelist.ui.app.ProfileFavoriteItem
import com.kitsugi.animelist.ui.components.KitsugiNsfwImage
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun AniListFavoritesTab(
    state: AniListProfileState,
    accentColor: Color,
    isLandscape: Boolean,
    favoritesFilter: Int,
    viewModel: KitsugiProfileViewModel,
    onFavoriteMediaClick: (mediaId: Int, mediaType: MediaType, source: String, title: String, imageUrl: String?) -> Unit,
    onFavoriteCharacterClick: (charId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStaffClick: (staffId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStudioClick: ((studioId: Int, source: String, name: String?, imageUrl: String?) -> Unit)? = null,
    onOpenFavoriteSheet: (title: String, items: List<ProfileFavoriteItem>, onClick: (ProfileFavoriteItem) -> Unit) -> Unit
) {
    val currentFavCategory = when (favoritesFilter) {
        0 -> "anime"; 1 -> "manga"; 2 -> "characters"; 3 -> "staff"; 4 -> "studios"; else -> "anime"
    }
    val currentFavList = when (favoritesFilter) {
        0 -> state.favoriteAnime; 1 -> state.favoriteManga; 2 -> state.favoriteCharacters
        3 -> state.favoriteStaff; 4 -> state.favoriteStudios; else -> emptyList()
    }
    val currentHasNext = when (favoritesFilter) {
        0 -> state.favAnimeHasNext; 1 -> state.favMangaHasNext; 2 -> state.favCharHasNext
        3 -> state.favStaffHasNext; 4 -> state.favStudioHasNext; else -> false
    }
    val filterTitle = when (favoritesFilter) {
        0 -> "Favori Animeler"; 1 -> "Favori Mangalar"; 2 -> "Favori Karakterler"
        3 -> "Favori Ekip"; 4 -> "Favori Stüdyolar"; else -> "Favoriler"
    }

    if (currentFavList.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(text = "Favori öge bulunamadı.", color = KitsugiColors.TextMuted)
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
                            onOpenFavoriteSheet(filterTitle, currentFavList) { item ->
                                item.id.toIntOrNull()?.let { id ->
                                    when (favoritesFilter) {
                                        0 -> onFavoriteMediaClick(id, MediaType.Anime, "anilist", item.title, item.imageUrl)
                                        1 -> onFavoriteMediaClick(id, MediaType.Manga, "anilist", item.title, item.imageUrl)
                                        2 -> onFavoriteCharacterClick(id, "anilist", item.title, item.imageUrl)
                                        3 -> onFavoriteStaffClick(id, "anilist", item.title, item.imageUrl)
                                        4 -> onFavoriteStudioClick?.invoke(id, "anilist", item.title, item.imageUrl)
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(text = "Tümünü Gör", color = accentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            val gridColumns = if (isLandscape) 3 else 2
            currentFavList.chunked(gridColumns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
                    // Fill empty slots in last row
                    if (rowItems.size < gridColumns) {
                        repeat(gridColumns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            // "Daha Fazla" button
            if (currentHasNext) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .clickable { viewModel.loadMoreFavorites(currentFavCategory) }
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text(text = "Daha Fazla Yükle", color = accentColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
