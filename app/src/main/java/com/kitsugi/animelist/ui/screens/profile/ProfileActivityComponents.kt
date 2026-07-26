@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kitsugi.animelist.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.app.ProfileActivityItem
import com.kitsugi.animelist.ui.app.ProfileFavoriteItem
import com.kitsugi.animelist.ui.components.KitsugiNsfwImage
import com.kitsugi.animelist.ui.components.KitsugiSheetOrDialog
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent

// ─────────────────────────────────────────────────────────────────────────────
// ProfileActivityRow — aktivite satır bileşeni
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ProfileActivityRow(
    title: String,
    imageUrl: String?,
    statusStr: String,
    progressStr: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = statusStr,
                    color = LocalKitsugiAccent.current,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                if (!progressStr.isNullOrBlank()) {
                    Text(
                        text = "• $progressStr",
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ActivityCard — AniList aktivite kartı
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ActivityCard(
    activity: ProfileActivityItem,
    accentColor: androidx.compose.ui.graphics.Color,
    blurAdultMedia: Boolean = false,
    onMediaClick: ((mediaId: Int, type: MediaType) -> Unit)? = null,
    onActivityClick: ((activityId: Int) -> Unit)? = null,
    onLikeClick: ((activityId: Int) -> Unit)? = null,
    onDeleteClick: ((activityId: Int) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                activity.id.toIntOrNull()?.let { id -> onActivityClick?.invoke(id) }
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = KitsugiColors.Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = activity.userAvatar ?: "",
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.userName,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatActivityTime(activity.createdAt),
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (onDeleteClick != null) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Seçenekler",
                                tint = KitsugiColors.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(KitsugiColors.SurfaceStrong)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sil", color = KitsugiColors.AccentRed, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = KitsugiColors.AccentRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    activity.id.toIntOrNull()?.let { id -> onDeleteClick.invoke(id) }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (activity) {
                is ProfileActivityItem.TextActivity -> {
                    Text(
                        text = activity.text,
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                is ProfileActivityItem.ListActivity -> {
                    val isClickable = activity.mediaId != null && onMediaClick != null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(KitsugiColors.Background)
                            .then(
                                if (isClickable) {
                                    Modifier.clickable {
                                        val mType = when (activity.mediaType?.uppercase()) {
                                            "MANGA" -> MediaType.Manga
                                            "MOVIE" -> MediaType.Movie
                                            "TV", "TV_SHOW" -> MediaType.TvShow
                                            else -> MediaType.Anime
                                        }
                                        onMediaClick?.invoke(activity.mediaId!!, mType)
                                    }
                                } else Modifier
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KitsugiNsfwImage(
                            model = activity.mediaImage ?: "",
                            contentDescription = "",
                            isAdult = (activity as? ProfileActivityItem.ListActivity)?.isAdult == true,
                            modifier = Modifier
                                .size(45.dp, 64.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val actionText = when (activity.status) {
                                "completed" -> "tamamladı"
                                "watching" -> "izliyor"
                                "reading" -> "okuyor"
                                "plan_to_watch" -> "izlemeyi planlıyor"
                                "plan_to_read" -> "okumayı planlıyor"
                                else -> activity.status
                            }
                            Text(
                                text = "${activity.userName} bunu $actionText:",
                                color = accentColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activity.mediaTitle,
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!activity.progress.isNullOrBlank()) {
                                Text(
                                    text = "Bölüm/Bölümler: ${activity.progress}",
                                    color = KitsugiColors.TextMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable {
                        activity.id.toIntOrNull()?.let { id -> onActivityClick?.invoke(id) }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = "Yanıtlar",
                        tint = KitsugiColors.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = activity.replyCount.toString(),
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                var isLikedState by remember(activity.isLiked) { mutableStateOf(activity.isLiked) }
                var likeCountState by remember(activity.likeCount) { mutableStateOf(activity.likeCount) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable {
                        isLikedState = !isLikedState
                        likeCountState = if (isLikedState) likeCountState + 1 else likeCountState - 1
                        activity.id.toIntOrNull()?.let { id -> onLikeClick?.invoke(id) }
                    }
                ) {
                    Icon(
                        imageVector = if (isLikedState) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Beğen",
                        tint = if (isLikedState) KitsugiColors.AccentPink else KitsugiColors.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = likeCountState.toString(),
                        color = if (isLikedState) KitsugiColors.AccentPink else KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FavoritesHorizontalSection — yatay kaydırmalı favoriler bölümü
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FavoritesHorizontalSection(
    title: String,
    items: List<ProfileFavoriteItem>,
    blurAdultMedia: Boolean = false,
    onSeeAllClick: (() -> Unit)? = null,
    onItemClick: (ProfileFavoriteItem) -> Unit
) {
    if (items.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(KitsugiColors.Surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (items.size > 1 && onSeeAllClick != null) {
                TextButton(
                    onClick = onSeeAllClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tümünü Gör (${items.size})",
                            color = LocalKitsugiAccent.current,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = LocalKitsugiAccent.current,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(
                items = items,
                key = { item -> item.id.ifBlank { item.title } }
            ) { item ->
                Column(
                    modifier = Modifier
                        .width(90.dp)
                        .clickable { onItemClick(item) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp, 128.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(KitsugiColors.Background)
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
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Rounded.Favorite, contentDescription = null, tint = KitsugiColors.TextMuted)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.title,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FavoritesExpandedBottomSheet — favorileri tam ekran sheet'te gösteren bileşen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesExpandedBottomSheet(
    title: String,
    items: List<ProfileFavoriteItem>,
    blurAdultMedia: Boolean = false,
    hasNextPage: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    onItemClick: (ProfileFavoriteItem) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val favGridState = rememberLazyGridState()

    KitsugiSheetOrDialog(
        onDismiss = onDismiss,
        innerGridScrollState = favGridState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$title (${items.size})",
                    color = KitsugiColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = KitsugiColors.SurfaceStrong
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Kapat",
                        tint = KitsugiColors.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                state = favGridState,
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = items,
                    key = { item -> item.id.ifBlank { item.title } }
                ) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onItemClick(item)
                                onDismiss()
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(KitsugiColors.Background)
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
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Rounded.Favorite, contentDescription = null, tint = KitsugiColors.TextMuted)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.title,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp
                        )
                    }
                }

                if (hasNextPage && onLoadMore != null) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        LaunchedEffect(items.size) { onLoadMore() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
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
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PositionalStatItemCard — sıralı istatistik öge kartı
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PositionalStatItemCard(
    rank: Int,
    title: String,
    count: Int,
    meanScore: Double,
    timeSpentMinutes: Int? = null,
    chaptersRead: Int? = null,
    imageUrl: String? = null,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KitsugiColors.Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(KitsugiColors.SurfaceStrong),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = accentColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = title,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        color = accentColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = count.toString(),
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Başlık sayısı",
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (meanScore > 0) "%.1f".format(meanScore) else "-",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ortalama Puan",
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (timeSpentMinutes != null && timeSpentMinutes > 0) {
                    val hours = timeSpentMinutes / 60
                    val days = hours / 24
                    val timeText = when {
                        days >= 30 -> "${days / 30} ay"
                        days >= 7 -> "${days / 7} hafta"
                        days >= 1 -> "$days gün"
                        hours >= 1 -> "$hours saat"
                        else -> "$timeSpentMinutes dak"
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeText,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Harcanan süre",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else if (chaptersRead != null && chaptersRead > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = chaptersRead.toString(),
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Okunan Cilt/Bölüm",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// formatActivityTime — aktivite zaman formatı
// ─────────────────────────────────────────────────────────────────────────────
fun formatActivityTime(timestamp: Long): String {
    val diff = (System.currentTimeMillis() / 1000) - timestamp
    return when {
        diff < 60 -> "Şimdi"
        diff < 3600 -> "${diff / 60} dakika önce"
        diff < 86400 -> "${diff / 3600} saat önce"
        else -> "${diff / 86400} gün önce"
    }
}
