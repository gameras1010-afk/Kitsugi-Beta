@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kitsugi.animelist.ui.screens.profile

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import com.kitsugi.animelist.ui.app.RankedStatItem
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kitsugi.animelist.ui.components.KitsugiSheetOrDialog
import com.kitsugi.animelist.ui.components.KitsugiActivityDetailBottomSheet
import com.kitsugi.animelist.ui.components.KitsugiImageGalleryDialog
import com.kitsugi.animelist.data.remote.JikanApiClient
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.components.KitsugiMediaEntryCard
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.utils.toEnglishGenreForSearch
import com.kitsugi.animelist.utils.toTurkishGenre
import com.kitsugi.animelist.ui.app.KitsugiProfileViewModel
import com.kitsugi.animelist.ui.app.AniListProfileState
import com.kitsugi.animelist.utils.rememberScrollConnection
import com.kitsugi.animelist.utils.rememberScrollVisibilityState
import com.kitsugi.animelist.ui.app.MalProfileState
import com.kitsugi.animelist.ui.app.SimklProfileState
import com.kitsugi.animelist.ui.app.ProfileFavoriteItem
import com.kitsugi.animelist.ui.app.ProfileActivityItem
import android.content.Context
import android.widget.Toast
import com.kitsugi.animelist.ui.components.KitsugiProfileHeaderCard
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiColors
import com.kitsugi.animelist.ui.utils.tvClickable
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator

@Composable
fun KitsugiProfileScreen(
    viewModel: KitsugiProfileViewModel,
    mediaEntries: List<MediaEntry>,
    isAniListConnected: Boolean,
    isMalConnected: Boolean,
    isSimklConnected: Boolean,
    profileName: String,
    listTitle: String,
    profileImageUri: String,
    bannerImageUri: String,
    appSettings: AppSettings,
    onEntryClick: (MediaEntry) -> Unit,
    onOpenSettingsClick: () -> Unit,
    onFavoriteMediaClick: (mediaId: Int, mediaType: MediaType, source: String) -> Unit,
    onFavoriteCharacterClick: (charId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStaffClick: (staffId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStudioClick: ((studioId: Int, source: String, name: String?, imageUrl: String?) -> Unit)? = null,
    onOpenStatsClick: (() -> Unit)? = null,
    onGenreClick: (String) -> Unit = {},
    onTagClick: (String) -> Unit = {},
    onLoginAniList: () -> Unit = {},
    onLoginMal: () -> Unit = {},
    onLoginSimkl: () -> Unit = {},
    onUserProfileClick: (userId: Int, username: String, avatarUrl: String?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val accentColor = LocalKitsugiAccent.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val activeSubTab = viewModel.activeSubTab // 0: AniList, 1: MAL, 2: Simkl
    val subTabs = listOf("AniList", "MyAnimeList", "Simkl")

    val aniListState by viewModel.aniListState.collectAsState()
    val malState by viewModel.malState.collectAsState()
    val simklState by viewModel.simklState.collectAsState()

    val scrollState = rememberScrollVisibilityState(initialVisible = true)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember { JikanApiClient() }

    var activeFavoriteSheet by remember { mutableStateOf<Pair<String, List<ProfileFavoriteItem>>?>(null) }
    var onSheetItemClick by remember { mutableStateOf<((ProfileFavoriteItem) -> Unit)?>(null) }
    var activeActivityIdForDetail by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<String?>(null) }
    var activeGalleryImages by remember { mutableStateOf<Triple<List<String>, Int, String>?>(null) }

    val openFavoriteSheet: (String, List<ProfileFavoriteItem>, (ProfileFavoriteItem) -> Unit) -> Unit = { title, items, onClick ->
        activeFavoriteSheet = title to items
        onSheetItemClick = onClick
    }

    LaunchedEffect(activeSubTab) {
        scrollState.show()
    }

    // Trigger API fetches on tab switch if connected
    LaunchedEffect(activeSubTab, isAniListConnected, isMalConnected, isSimklConnected) {
        when (activeSubTab) {
            0 -> if (isAniListConnected && aniListState.userId == null) viewModel.fetchAniListProfile()
            1 -> if (isMalConnected && malState.name.isBlank()) viewModel.fetchMalProfile()
            2 -> if (isSimklConnected && simklState.name.isBlank()) viewModel.fetchSimklProfile()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Profile Sub-tab selector (Fixed at top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isLandscape) 18.dp else 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(KitsugiColors.Surface),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    subTabs.forEachIndexed { index, label ->
                        val isSelected = activeSubTab == index
                        val isServiceConnected = when (index) {
                            0 -> isAniListConnected
                            1 -> isMalConnected
                            2 -> isSimklConnected
                            else -> true
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    if (isSelected) accentColor else KitsugiColors.Surface
                                )
                                .tvClickable(shape = RoundedCornerShape(22.dp), onClick = { viewModel.activeSubTab = index })
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) KitsugiColors.Background else if (isServiceConnected) KitsugiColors.TextPrimary else KitsugiColors.TextMuted,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                                )
                                if (isServiceConnected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) KitsugiColors.Background else KitsugiColors.AccentGreen)
                                    )
                                }
                            }
                        }
                    }
                }

                if (onOpenStatsClick != null) {
                    IconButton(
                        onClick = onOpenStatsClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = KitsugiColors.Surface
                        ),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = "Kütüphane İstatistikleri",
                            tint = accentColor
                        )
                    }
                }
            }

            // Main Content Area
            val isProfileLoading = when (activeSubTab) {
                0 -> aniListState.isLoading
                1 -> malState.isLoading
                else -> simklState.isLoading
            }
            val pullRefreshState = rememberPullToRefreshState()

            PullToRefreshBox(
                isRefreshing = isProfileLoading,
                onRefresh = { viewModel.refreshActiveProfile() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = pullRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullRefreshState,
                        isRefreshing = isProfileLoading,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = KitsugiColors.Surface,
                        color = accentColor
                    )
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (activeSubTab) {
                        0 -> ExternalProfileWrapper(
                            isConnected = isAniListConnected,
                            isLoading = aniListState.isLoading,
                            error = aniListState.error,
                            onConnectClick = onLoginAniList,
                            accentColor = accentColor,
                            platformName = "AniList"
                        ) {
                            AniListProfileContent(
                                viewModel = viewModel,
                                state = aniListState,
                                mediaEntries = mediaEntries,
                                appSettings = appSettings,
                                onEntryClick = onEntryClick,
                                onFavoriteMediaClick = onFavoriteMediaClick,
                                onFavoriteCharacterClick = onFavoriteCharacterClick,
                                onFavoriteStaffClick = onFavoriteStaffClick,
                                onLoadMoreActivities = { viewModel.loadNextAniListActivitiesPage() },
                                onOpenFavoriteSheet = openFavoriteSheet,
                                onOpenStatsClick = onOpenStatsClick,
                                onActivityClick = { actId -> activeActivityIdForDetail = actId },
                                onLikeClick = { actId ->
                                    coroutineScope.launch {
                                        apiClient.toggleLike(actId, "ACTIVITY")
                                    }
                                },
                                onDeleteClick = { actId -> showDeleteConfirmDialog = actId.toString() },
                                isLandscape = isLandscape,
                                onGenreClick = onGenreClick,
                                onTagClick = onTagClick,
                                onFavoriteStudioClick = onFavoriteStudioClick,
                                onUserProfileClick = onUserProfileClick,
                                accentColor = accentColor,
                                onImageClick = { urls, idx, title -> activeGalleryImages = Triple(urls, idx, title) }
                            )
                        }
                        1 -> ExternalProfileWrapper(
                            isConnected = isMalConnected,
                            isLoading = malState.isLoading,
                            error = malState.error,
                            onConnectClick = onLoginMal,
                            accentColor = accentColor,
                            platformName = "MyAnimeList"
                        ) {
                            MalProfileContent(
                                viewModel = viewModel,
                                state = malState,
                                mediaEntries = mediaEntries,
                                appSettings = appSettings,
                                onEntryClick = onEntryClick,
                                onFavoriteMediaClick = onFavoriteMediaClick,
                                onFavoriteCharacterClick = onFavoriteCharacterClick,
                                onFavoriteStaffClick = onFavoriteStaffClick,
                                onOpenFavoriteSheet = openFavoriteSheet,
                                onOpenStatsClick = onOpenStatsClick,
                                isLandscape = isLandscape,
                                accentColor = accentColor,
                                onImageClick = { urls, idx, title -> activeGalleryImages = Triple(urls, idx, title) }
                            )
                        }
                        2 -> ExternalProfileWrapper(
                            isConnected = isSimklConnected,
                            isLoading = simklState.isLoading,
                            error = simklState.error,
                            onConnectClick = onLoginSimkl,
                            accentColor = accentColor,
                            platformName = "Simkl"
                        ) {
                            SimklProfileContent(
                                viewModel = viewModel,
                                state = simklState,
                                mediaEntries = mediaEntries,
                                appSettings = appSettings,
                                onEntryClick = onEntryClick,
                                onFavoriteMediaClick = onFavoriteMediaClick,
                                onOpenFavoriteSheet = openFavoriteSheet,
                                onOpenStatsClick = onOpenStatsClick,
                                isLandscape = isLandscape,
                                accentColor = accentColor,
                                onImageClick = { urls, idx, title -> activeGalleryImages = Triple(urls, idx, title) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (activeFavoriteSheet != null) {
        val filterTitle = activeFavoriteSheet!!.first
        val currentFavCategory = when (viewModel.aniListFavoritesFilter) {
            0 -> "anime"
            1 -> "manga"
            2 -> "characters"
            3 -> "staff"
            4 -> "studios"
            else -> "anime"
        }
        val currentFavList = if (activeSubTab == 0) {
            when (viewModel.aniListFavoritesFilter) {
                0 -> aniListState.favoriteAnime
                1 -> aniListState.favoriteManga
                2 -> aniListState.favoriteCharacters
                3 -> aniListState.favoriteStaff
                4 -> aniListState.favoriteStudios
                else -> activeFavoriteSheet!!.second
            }
        } else {
            activeFavoriteSheet!!.second
        }
        val currentHasNext = if (activeSubTab == 0) {
            when (viewModel.aniListFavoritesFilter) {
                0 -> aniListState.favAnimeHasNext
                1 -> aniListState.favMangaHasNext
                2 -> aniListState.favCharHasNext
                3 -> aniListState.favStaffHasNext
                4 -> aniListState.favStudioHasNext
                else -> false
            }
        } else false

        FavoritesExpandedBottomSheet(
            title = filterTitle,
            items = currentFavList,
            blurAdultMedia = appSettings.blurAdultMedia,
            hasNextPage = currentHasNext,
            onLoadMore = { viewModel.loadMoreFavorites(currentFavCategory) },
            onItemClick = { item ->
                onSheetItemClick?.invoke(item)
            },
            onDismiss = {
                activeFavoriteSheet = null
                onSheetItemClick = null
            }
        )
    }

    if (activeActivityIdForDetail != null) {
        KitsugiActivityDetailBottomSheet(
            activityId = activeActivityIdForDetail!!,
            apiClient = apiClient,
            titleLanguage = appSettings.titleLanguage.toString(),
            blurAdultMedia = appSettings.blurAdultMedia,
            onMediaClick = { mediaId, mType, source ->
                onFavoriteMediaClick(mediaId, mType, source)
            },
            onDismiss = {
                activeActivityIdForDetail = null
                viewModel.fetchAniListProfile()
            }
        )
    }

    if (showDeleteConfirmDialog != null) {
        val actIdStr = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Aktiviteyi Sil", color = KitsugiColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Bu aktiviteyi silmek istediğinize emin misiniz?", color = KitsugiColors.TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val actId = actIdStr.toIntOrNull() ?: 0
                            val success = apiClient.deleteActivity(actId)
                            if (success) {
                                viewModel.fetchAniListProfile()
                            }
                        }
                        showDeleteConfirmDialog = null
                    }
                ) {
                    Text("Sil", color = KitsugiColors.AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("İptal", color = KitsugiColors.TextMuted)
                }
            },
            containerColor = KitsugiColors.Surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (activeGalleryImages != null) {
        val (urls, initialIdx, title) = activeGalleryImages!!
        KitsugiImageGalleryDialog(
            imageUrls = urls,
            initialIndex = initialIdx,
            title = title,
            onDismiss = { activeGalleryImages = null }
        )
    }
}

@Composable
fun ExternalProfileWrapper(
    isConnected: Boolean,
    isLoading: Boolean,
    error: String?,
    onConnectClick: () -> Unit,
    accentColor: Color,
    platformName: String,
    content: @Composable () -> Unit
) {
    if (!isConnected) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = KitsugiColors.Surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LinkOff,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "$platformName Hesabı Bağlı Değil",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Profil istatistiklerinizi, favorilerinizi ve sosyal aktivitelerinizi Kitsugi'de görmek için hesabınızı bağlayın.",
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onConnectClick,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text(text = "Hesabı Bağla", color = KitsugiColors.Background, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = accentColor)
        }
    } else if (error != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = error, color = KitsugiColors.TextPrimary)
            }
        }
    } else {
        content()
    }
}

@Composable
fun VerticalStatsBar(
    stats: List<Pair<String, Float>>,
    accentColor: Color = LocalKitsugiAccent.current,
    maxHeightDp: Int = 90,
    modifier: Modifier = Modifier,
    mapColorTo: ((String) -> Color)? = null
) {
    if (stats.isEmpty()) return
    val maxValue = remember(stats) { stats.maxOfOrNull { it.second } ?: 1f }.coerceAtLeast(1f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        stats.forEach { (label, value) ->
            val barHeight = ((value / maxValue) * maxHeightDp).coerceAtLeast(4f).dp
            val barColor = mapColorTo?.invoke(label) ?: accentColor

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value),
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(barColor)
                )
                Text(
                    text = label,
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun HorizontalStatsBar(
    stats: List<Triple<String, Int, Color>>,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) return
    val total = remember(stats) { stats.sumOf { it.second } }.coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stats.forEach { (label, count, color) ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(KitsugiColors.SurfaceStrong)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$count $label",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(KitsugiColors.SurfaceStrong)
        ) {
            stats.forEach { (_, count, color) ->
                val weight = count.toFloat() / total.toFloat()
                if (weight > 0) {
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileHeaderIconTabs(
    tabs: List<Pair<ImageVector, String>>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    accentColor: Color
) {
    val tabListState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(selectedTab) {
        tabListState.animateScrollToItem(selectedTab)
    }

    androidx.compose.foundation.lazy.LazyRow(
        state = tabListState,
        flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
            lazyListState = tabListState,
            snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Start
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(tabs.size) { index ->
            val (icon, label) = tabs[index]
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) accentColor else Color.Transparent)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) KitsugiColors.Background else KitsugiColors.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    if (isSelected) {
                        Text(
                            text = label,
                            color = KitsugiColors.Background,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AniListProfileContent(
    viewModel: KitsugiProfileViewModel,
    state: AniListProfileState,
    mediaEntries: List<MediaEntry>,
    appSettings: AppSettings,
    onEntryClick: (MediaEntry) -> Unit,
    onFavoriteMediaClick: (mediaId: Int, mediaType: MediaType, source: String) -> Unit,
    onFavoriteCharacterClick: (charId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStaffClick: (staffId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStudioClick: ((studioId: Int, source: String, name: String?, imageUrl: String?) -> Unit)? = null,
    onLoadMoreActivities: () -> Unit,
    onOpenFavoriteSheet: (title: String, items: List<ProfileFavoriteItem>, onClick: (ProfileFavoriteItem) -> Unit) -> Unit,
    onOpenStatsClick: (() -> Unit)? = null,
    onActivityClick: ((Int) -> Unit)? = null,
    onLikeClick: ((Int) -> Unit)? = null,
    onDeleteClick: ((Int) -> Unit)? = null,
    onGenreClick: (String) -> Unit = {},
    onTagClick: (String) -> Unit = {},
    onUserProfileClick: (userId: Int, username: String, avatarUrl: String?) -> Unit = { _, _, _ -> },
    isLandscape: Boolean,
    accentColor: Color,
    onImageClick: ((urls: List<String>, initialIndex: Int, title: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var activeTab by rememberSaveable { mutableIntStateOf(viewModel.aniListActiveTab) } // 0: Info, 1: Activity, 2: Stats, 3: Favorites, 4: Social
    LaunchedEffect(activeTab) { viewModel.aniListActiveTab = activeTab }

    var statsMediaType by rememberSaveable { mutableIntStateOf(viewModel.aniListStatsMediaType) } // 0: Anime, 1: Manga
    LaunchedEffect(statsMediaType) { viewModel.aniListStatsMediaType = statsMediaType }

    var statsSubTab by rememberSaveable { mutableIntStateOf(viewModel.aniListStatsSubTab) } // 0: Genel Bakış, 1: Türler, 2: Etiketler, 3: Ekip, 4: Seslendirenler, 5: Stüdyolar
    LaunchedEffect(statsSubTab) { viewModel.aniListStatsSubTab = statsSubTab }

    var favoritesFilter by rememberSaveable { mutableIntStateOf(viewModel.aniListFavoritesFilter) } // 0: Anime, 1: Manga, 2: Character, 3: Staff, 4: Studio
    LaunchedEffect(favoritesFilter) { viewModel.aniListFavoritesFilter = favoritesFilter }

    var socialFilter by rememberSaveable { mutableIntStateOf(viewModel.aniListSocialFilter) } // 0: Followers, 1: Following
    LaunchedEffect(socialFilter) { viewModel.aniListSocialFilter = socialFilter }

    var scoreDistType by rememberSaveable { mutableIntStateOf(viewModel.aniListScoreDistType) } // 0: Başlık sayısı, 1: Harcanan süre
    LaunchedEffect(scoreDistType) { viewModel.aniListScoreDistType = scoreDistType }

    var lengthDistType by rememberSaveable { mutableIntStateOf(viewModel.aniListLengthDistType) } // 0: Başlık sayısı, 1: Harcanan süre, 2: Ortalama Puan
    LaunchedEffect(lengthDistType) { viewModel.aniListLengthDistType = lengthDistType }

    var releaseYearDistType by rememberSaveable { mutableIntStateOf(viewModel.aniListReleaseYearDistType) } // 0: Başlık sayısı, 1: Harcanan süre, 2: Ortalama Puan
    LaunchedEffect(releaseYearDistType) { viewModel.aniListReleaseYearDistType = releaseYearDistType }

    var startYearDistType by rememberSaveable { mutableIntStateOf(viewModel.aniListStartYearDistType) } // 0: Başlık sayısı, 1: Harcanan süre, 2: Ortalama Puan
    LaunchedEffect(startYearDistType) { viewModel.aniListStartYearDistType = startYearDistType }

    var statSortType by rememberSaveable { mutableIntStateOf(viewModel.aniListStatsSortType) } // 0: Başlık sayısı, 1: Harcanan süre, 2: Ortalama Puan
    LaunchedEffect(statSortType) { viewModel.aniListStatsSortType = statSortType }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.aniListScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.aniListScrollOffset
    )

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        viewModel.updateAniListScroll(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = viewModel.aniListActiveTab,
        pageCount = { 5 }
    )

    LaunchedEffect(pagerState.currentPage) {
        activeTab = pagerState.currentPage
        viewModel.aniListActiveTab = pagerState.currentPage
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isLandscape) 18.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner and Avatar
        item {
            val context = LocalContext.current
            val avatarUrl = state.avatarUrl?.takeIf { it.isNotBlank() }
            val bannerUrl = state.bannerUrl?.takeIf { it.isNotBlank() }
            val imageList = listOfNotNull(avatarUrl, bannerUrl)
            val username = state.name.ifBlank { "Kullanıcı" }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .then(
                        if (!bannerUrl.isNullOrBlank()) {
                            Modifier.clickable {
                                val idx = imageList.indexOf(bannerUrl).coerceAtLeast(0)
                                onImageClick?.invoke(imageList, idx, "$username Banner")
                            }
                        } else Modifier
                    )
            ) {
                if (!state.bannerUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = state.bannerUrl,
                        contentDescription = "Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(accentColor, KitsugiColors.AccentPink)))
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, KitsugiColors.Background.copy(alpha = 0.8f))))
                )

                // Share butonu – sağ üst köşe
                if (state.name.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(KitsugiColors.Background.copy(alpha = 0.50f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = {
                            val url = com.kitsugi.animelist.utils.ShareUtils.buildProfileUrl("anilist", state.name)
                            com.kitsugi.animelist.utils.ShareUtils.shareText(context, state.name, url)
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Profili Paylaş",
                                tint = KitsugiColors.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Avatar overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = state.avatarUrl ?: "",
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(3.dp, KitsugiColors.Background, CircleShape)
                            .then(
                                if (!avatarUrl.isNullOrBlank()) {
                                    Modifier.clickable {
                                        val idx = imageList.indexOf(avatarUrl).coerceAtLeast(0)
                                        onImageClick?.invoke(imageList, idx, "$username Profil Resmi")
                                    }
                                } else Modifier
                            ),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = state.name,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (!state.donatorBadge.isNullOrBlank()) {
                            Text(
                                text = "Donator Tier ${state.donatorTier}",
                                color = KitsugiColors.AccentOrange,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Top 5 Icon Sub-Tabs + Sub-filters (Sticky at top)
        stickyHeader(key = "anilist_tabs_header") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = KitsugiColors.Background
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val tabs = listOf(
                        Icons.Rounded.Info to "Hakkında",
                        Icons.Rounded.ChatBubble to "Aktivite",
                        Icons.Rounded.BarChart to "İstatistikler",
                        Icons.Rounded.Star to "Favoriler",
                        Icons.Rounded.People to "Sosyal"
                    )
                    ProfileHeaderIconTabs(
                        tabs = tabs,
                        selectedTab = pagerState.currentPage,
                        onTabSelected = { page ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        },
                        accentColor = accentColor
                    )

                    if (pagerState.currentPage == 2) {
                        val statsSubTabState = rememberLazyListState()
                        androidx.compose.foundation.lazy.LazyRow(
                            state = statsSubTabState,
                            flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                                lazyListState = statsSubTabState,
                                snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Start
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(6) { idx ->
                                val label = listOf("Genel Bakış", "Türler", "Etiketler", "Ekip", "Seslendirenler", "Stüdyolar")[idx]
                                ProfileFilterChip(
                                    text = label,
                                    isSelected = statsSubTab == idx,
                                    accentColor = accentColor,
                                    onClick = { statsSubTab = idx }
                                )
                            }
                        }
                    } else if (pagerState.currentPage == 3) {
                        val favoritesFilterState = rememberLazyListState()
                        androidx.compose.foundation.lazy.LazyRow(
                            state = favoritesFilterState,
                            flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                                lazyListState = favoritesFilterState,
                                snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Start
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(5) { idx ->
                                val label = listOf("Anime", "Manga", "Karakterler", "Ekip", "Stüdyolar")[idx]
                                ProfileFilterChip(
                                    text = label,
                                    isSelected = favoritesFilter == idx,
                                    accentColor = accentColor,
                                    onClick = { favoritesFilter = idx }
                                )
                            }
                        }
                    } else if (pagerState.currentPage == 4) {
                        val socialFilterState = rememberLazyListState()
                        androidx.compose.foundation.lazy.LazyRow(
                            state = socialFilterState,
                            flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                                lazyListState = socialFilterState,
                                snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Start
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                ProfileFilterChip(
                                    text = "Takipçiler (${state.socialState.followers.size})",
                                    isSelected = socialFilter == 0,
                                    accentColor = accentColor,
                                    onClick = { socialFilter = 0 }
                                )
                            }
                            item {
                                ProfileFilterChip(
                                    text = "Takip Edilen (${state.socialState.following.size})",
                                    isSelected = socialFilter == 1,
                                    accentColor = accentColor,
                                    onClick = { socialFilter = 1 }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pager Content
        item(key = "content") {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val pageHeights = remember { androidx.compose.runtime.mutableStateMapOf<Int, Int>() }
            val currentPage = pagerState.currentPage
            val currentPageOffset = pagerState.currentPageOffsetFraction
            val targetPage = if (currentPageOffset > 0f) currentPage + 1 else if (currentPageOffset < 0f) currentPage - 1 else currentPage

            val currentHeightPx = pageHeights[currentPage] ?: 0
            val targetHeightPx = pageHeights[targetPage] ?: currentHeightPx

            val interpolatedHeightDp = remember(currentHeightPx, targetHeightPx, currentPageOffset) {
                val heightPx = if (currentHeightPx > 0 && targetHeightPx > 0) {
                    currentHeightPx + (targetHeightPx - currentHeightPx) * kotlin.math.abs(currentPageOffset)
                } else if (currentHeightPx > 0) {
                    currentHeightPx.toFloat()
                } else {
                    0f
                }
                if (heightPx > 0f) with(density) { heightPx.toDp() } else null
            }

            val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp

            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                beyondViewportPageCount = 1,
                pageSpacing = 12.dp,
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val minPagerHeightPx = with(density) { (screenHeightDp - 64).dp.roundToPx() }
                        val placeable = measurable.measure(
                            constraints.copy(
                                minHeight = minPagerHeightPx,
                                maxHeight = androidx.compose.ui.unit.Constraints.Infinity
                            )
                        )
                        val height = interpolatedHeightDp?.roundToPx()?.coerceAtLeast(minPagerHeightPx) ?: placeable.height
                        layout(placeable.width, height) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                    .clipToBounds()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 600.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                pageHeights[page] = coordinates.size.height
                            },
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (page) {
                            0 -> {
                                // Info tab contents
                                if (state.about.isNotBlank()) {
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
                                                text = "Hakkında",
                                                color = KitsugiColors.TextPrimary,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { context.openTranslator(state.about) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Translate,
                                                        contentDescription = "Çevir",
                                                        tint = accentColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("about", state.about))
                                                        Toast.makeText(context, "Panoya kopyalandı", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.ContentCopy,
                                                        contentDescription = "Kopyala",
                                                        tint = KitsugiColors.TextSecondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = state.about,
                                            color = KitsugiColors.TextSecondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(KitsugiColors.Surface)
                                        .padding(18.dp)
                                ) {
                                    Text(
                                        text = "Profil Özeti",
                                        color = KitsugiColors.TextPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        StatCard("Anime", state.animeStats?.count?.toString() ?: "0")
                                        StatCard("Manga", state.mangaStats?.count?.toString() ?: "0")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        StatCard("Takipçi", state.socialState.followers.size.toString())
                                        StatCard("Takip Edilen", state.socialState.following.size.toString())
                                    }
                                }
                            }
                            1 -> {
                                // Activity tab contents
                                if (state.activities.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "Henüz aktivite bulunmuyor.", color = KitsugiColors.TextMuted)
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        state.activities.forEach { act ->
                                            ActivityCard(
                                                activity = act,
                                                accentColor = accentColor,
                                                blurAdultMedia = appSettings.blurAdultMedia,
                                                onMediaClick = { mediaId, mType ->
                                                    onFavoriteMediaClick(mediaId, mType, "anilist")
                                                },
                                                onActivityClick = { actId -> onActivityClick?.invoke(actId) },
                                                onLikeClick = { actId -> onLikeClick?.invoke(actId) },
                                                onDeleteClick = { actId -> onDeleteClick?.invoke(actId) }
                                            )
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // Stats tab contents
                                val overview = if (statsMediaType == 0) state.animeOverviewStats else state.mangaOverviewStats
                                if (statsSubTab == 0) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(KitsugiColors.Surface)
                                            .padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Media Switcher
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(KitsugiColors.SurfaceStrong)
                                                .padding(4.dp)
                                        ) {
                                            listOf("Anime", "Manga").forEachIndexed { idx, label ->
                                                val isSel = statsMediaType == idx
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(if (isSel) accentColor else Color.Transparent)
                                                        .clickable { statsMediaType = idx }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = if (isSel) KitsugiColors.Background else KitsugiColors.TextMuted,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.labelLarge
                                                    )
                                                }
                                            }
                                        }

                                        if (overview != null) {
                                            // 1. Key Stats Grid (3x2)
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth()) {
                                                    StatCard("Toplam", overview.count.toString())
                                                    StatCard(if (statsMediaType == 0) "İzlenen bölüm" else "Okunan bölüm", overview.episodesWatched.toString())
                                                    StatCard(if (statsMediaType == 0) "İzlenen gün" else "Okunan cilt", "%.1f".format(overview.daysWatched))
                                                }
                                                Row(modifier = Modifier.fillMaxWidth()) {
                                                    StatCard(if (statsMediaType == 0) "Planlanan gün" else "Planlanan bölüm", "%.1f".format(overview.plannedDaysOrCount))
                                                    StatCard("Ortalama Puan", "%.2f".format(overview.meanScore))
                                                    StatCard("Standart sapma", "%.1f".format(overview.standardDeviation))
                                                }
                                            }

                                            HorizontalDivider(color = KitsugiColors.SurfaceStrong)

                                            // 2. Score Distribution
                                            if (overview.scoreList.isNotEmpty()) {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = "Puan",
                                                        color = KitsugiColors.TextPrimary,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                                    ) {
                                                        FilterChipItem(
                                                            selected = scoreDistType == 0,
                                                            text = "Başlık sayısı",
                                                            onClick = { scoreDistType = 0 },
                                                            accentColor = accentColor
                                                        )
                                                        FilterChipItem(
                                                            selected = scoreDistType == 1,
                                                            text = "Harcanan süre",
                                                            onClick = { scoreDistType = 1 },
                                                            accentColor = accentColor
                                                        )
                                                    }
                                                    val mappedStats = overview.scoreList.map { item ->
                                                        val valFloat = if (scoreDistType == 0) item.count.toFloat() else (item.minutesWatched / 60.0f)
                                                        item.score.toString() to valFloat
                                                    }
                                                    VerticalStatsBar(
                                                        stats = mappedStats,
                                                        accentColor = accentColor,
                                                        mapColorTo = { scoreStr ->
                                                            val scoreNum = scoreStr.toIntOrNull() ?: 0
                                                            when (scoreNum) {
                                                                in 1..3 -> Color(0xFFE57373)
                                                                in 4..5 -> Color(0xFFFFB74D)
                                                                in 6..7 -> Color(0xFFFFD54F)
                                                                in 8..9 -> Color(0xFF81C784)
                                                                10 -> Color(0xFF4FC3F7)
                                                                else -> accentColor
                                                            }
                                                        }
                                                    )
                                                }
                                            }

                                            // 3. Episode / Chapter Length Distribution
                                            if (overview.lengthList.isNotEmpty()) {
                                                HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = if (statsMediaType == 0) "Bölüm Sayısı" else "Cilt/Bölüm Sayısı",
                                                        color = KitsugiColors.TextPrimary,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                                    ) {
                                                        FilterChipItem(
                                                            selected = lengthDistType == 0,
                                                            text = "Başlık sayısı",
                                                            onClick = { lengthDistType = 0 },
                                                            accentColor = accentColor
                                                        )
                                                        FilterChipItem(
                                                            selected = lengthDistType == 1,
                                                            text = "Harcanan süre",
                                                            onClick = { lengthDistType = 1 },
                                                            accentColor = accentColor
                                                        )
                                                        FilterChipItem(
                                                            selected = lengthDistType == 2,
                                                            text = "Ortalama Puan",
                                                            onClick = { lengthDistType = 2 },
                                                            accentColor = accentColor
                                                        )
                                                    }
                                                    val mappedLength = overview.lengthList.map { item ->
                                                        val valFloat = when (lengthDistType) {
                                                            0 -> item.count.toFloat()
                                                            1 -> (item.minutesWatched / 60.0f)
                                                            else -> item.meanScore.toFloat()
                                                        }
                                                        item.length to valFloat
                                                    }
                                                    VerticalStatsBar(
                                                        stats = mappedLength,
                                                        accentColor = accentColor
                                                    )
                                                }
                                            }

                                            // 4. Status Distribution
                                            if (overview.statusList.isNotEmpty()) {
                                                HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = "Durum Dağılımı",
                                                        color = KitsugiColors.TextPrimary,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    val statusItems = overview.statusList.map { item ->
                                                        val (label, color) = when (item.status.uppercase()) {
                                                            "CURRENT" -> (if (statsMediaType == 0) "Şimdiki" else "Okunuyor") to Color(0xFF81C784)
                                                            "COMPLETED" -> "Tamamlandı" to Color(0xFF64B5F6)
                                                            "PLANNING" -> "Planlanan" to Color(0xFFA1887F)
                                                            "PAUSED" -> "Durduruldu" to Color(0xFFFFB74D)
                                                            "DROPPED" -> "Bırakıldı" to Color(0xFFE57373)
                                                            "REPEATING" -> (if (statsMediaType == 0) "Tekrar İzleniyor" else "Tekrar Okunuyor") to Color(0xFFBA68C8)
                                                            else -> item.status to accentColor
                                                        }
                                                        Triple(label, item.count, color)
                                                    }
                                                    HorizontalStatsBar(stats = statusItems)
                                                }
                                            }

                                            // 5. Format Distribution
                                            if (overview.formatList.isNotEmpty()) {
                                                HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = "Tür Dağılımı",
                                                        color = KitsugiColors.TextPrimary,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    val formatItems = overview.formatList.map { item ->
                                                        val (label, color) = when (item.format.uppercase()) {
                                                            "TV" -> "TV" to Color(0xFF5C6BC0)
                                                            "TV_SHORT" -> "TV Kısa" to Color(0xFF7E57C2)
                                                            "MOVIE" -> "Film" to Color(0xFF26A69A)
                                                            "SPECIAL" -> "Özel" to Color(0xFFFFA726)
                                                            "OVA" -> "OVA" to Color(0xFFFF7043)
                                                            "ONA" -> "ONA" to Color(0xFFEC407A)
                                                            "MUSIC" -> "Müzik Klip" to Color(0xFFAB47BC)
                                                            "MANGA" -> "Manga" to Color(0xFF42A5F5)
                                                            "NOVEL" -> "LN" to Color(0xFF8D6E63)
                                                            "ONE_SHOT" -> "One-Shot" to Color(0xFF78909C)
                                                            else -> item.format to accentColor
                                                        }
                                                        Triple(label, item.count, color)
                                                    }
                                                    HorizontalStatsBar(stats = formatItems)
                                                }
                                            }

                                            // 6. Country Distribution
                                            if (overview.countryList.isNotEmpty()) {
                                                HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = "Ülke Dağılımı",
                                                        color = KitsugiColors.TextPrimary,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    val countryItems = overview.countryList.map { item ->
                                                        val (label, color) = when (item.country.uppercase()) {
                                                            "JP" -> "Japonya" to Color(0xFF5C6BC0)
                                                            "KR" -> "Güney Kore" to Color(0xFF26A69A)
                                                            "CN" -> "Çin" to Color(0xFFFF7043)
                                                            "TW" -> "Tayvan" to Color(0xFFAB47BC)
                                                            else -> item.country to accentColor
                                                        }
                                                        Triple(label, item.count, color)
                                                    }
                                                    HorizontalStatsBar(stats = countryItems)
                                                }
                                            }

                                            // 7. Release Year Distribution
                                            if (overview.releaseYearList.isNotEmpty()) {
                                                HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = "Yayın Yılı",
                                                        color = KitsugiColors.TextPrimary,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                                    ) {
                                                        FilterChipItem(
                                                            selected = releaseYearDistType == 0,
                                                            text = "Başlık sayısı",
                                                            onClick = { releaseYearDistType = 0 },
                                                            accentColor = accentColor
                                                        )
                                                        FilterChipItem(
                                                            selected = releaseYearDistType == 1,
                                                            text = "Harcanan süre",
                                                            onClick = { releaseYearDistType = 1 },
                                                            accentColor = accentColor
                                                        )
                                                        FilterChipItem(
                                                            selected = releaseYearDistType == 2,
                                                            text = "Ortalama Puan",
                                                            onClick = { releaseYearDistType = 2 },
                                                            accentColor = accentColor
                                                        )
                                                    }
                                                    val mappedYears = overview.releaseYearList
                                                        .filter { it.releaseYear > 0 }
                                                        .sortedBy { it.releaseYear }
                                                        .map { item ->
                                                            val valFloat = when (releaseYearDistType) {
                                                                0 -> item.count.toFloat()
                                                                1 -> (item.minutesWatched / 60.0f)
                                                                else -> item.meanScore.toFloat()
                                                            }
                                                            item.releaseYear.toString() to valFloat
                                                        }
                                                    VerticalStatsBar(
                                                        stats = mappedYears,
                                                        accentColor = accentColor
                                                    )
                                                }
                                            }

                                            // 8. Watch / Read Year Distribution
                                            if (overview.startYearList.isNotEmpty()) {
                                                HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = if (statsMediaType == 0) "İzleme Yılı" else "Okuma Yılı",
                                                        color = KitsugiColors.TextPrimary,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                                    ) {
                                                        FilterChipItem(
                                                            selected = startYearDistType == 0,
                                                            text = "Başlık sayısı",
                                                            onClick = { startYearDistType = 0 },
                                                            accentColor = accentColor
                                                        )
                                                        FilterChipItem(
                                                            selected = startYearDistType == 1,
                                                            text = "Harcanan süre",
                                                            onClick = { startYearDistType = 1 },
                                                            accentColor = accentColor
                                                        )
                                                        FilterChipItem(
                                                            selected = startYearDistType == 2,
                                                            text = "Ortalama Puan",
                                                            onClick = { startYearDistType = 2 },
                                                            accentColor = accentColor
                                                        )
                                                    }
                                                    val mappedStartYears = overview.startYearList
                                                        .filter { it.startYear > 0 }
                                                        .sortedBy { it.startYear }
                                                        .map { item ->
                                                            val valFloat = when (startYearDistType) {
                                                                0 -> item.count.toFloat()
                                                                1 -> (item.minutesWatched / 60.0f)
                                                                else -> item.meanScore.toFloat()
                                                            }
                                                            item.startYear.toString() to valFloat
                                                        }
                                                    VerticalStatsBar(
                                                        stats = mappedStartYears,
                                                        accentColor = accentColor
                                                    )
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = "İstatistik verisi hazırlanamadı.", color = KitsugiColors.TextMuted)
                                            }
                                        }
                                    }
                                } else {
                                    val currentList: List<RankedStatItem> = when (statsSubTab) {
                                        1 -> overview?.genreList.orEmpty()
                                        2 -> overview?.tagList.orEmpty()
                                        3 -> overview?.staffList.orEmpty()
                                        4 -> overview?.voiceActorList.orEmpty()
                                        5 -> overview?.studioList.orEmpty()
                                        else -> emptyList()
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(KitsugiColors.Surface)
                                            .padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Media Switcher (Anime / Manga)
                                        if (statsSubTab in 1..3) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(KitsugiColors.SurfaceStrong)
                                                    .padding(4.dp)
                                            ) {
                                                listOf("Anime", "Manga").forEachIndexed { idx, label ->
                                                    val isSel = statsMediaType == idx
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(if (isSel) accentColor else Color.Transparent)
                                                            .clickable { statsMediaType = idx }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            color = if (isSel) KitsugiColors.Background else KitsugiColors.TextMuted,
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.labelLarge
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Sort Chips Row
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.horizontalScroll(rememberScrollState())
                                        ) {
                                            ProfileFilterChip(
                                                isSelected = statSortType == 0,
                                                text = "Başlık sayısı",
                                                onClick = { statSortType = 0 },
                                                accentColor = accentColor
                                            )
                                            ProfileFilterChip(
                                                isSelected = statSortType == 1,
                                                text = "Harcanan süre",
                                                onClick = { statSortType = 1 },
                                                accentColor = accentColor
                                            )
                                            ProfileFilterChip(
                                                isSelected = statSortType == 2,
                                                text = "Ortalama Puan",
                                                onClick = { statSortType = 2 },
                                                accentColor = accentColor
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    val sortedList = when (statSortType) {
                                        0 -> currentList.sortedByDescending { it.count }
                                        1 -> currentList.sortedByDescending { it.timeSpentMinutes ?: it.chaptersRead ?: 0 }
                                        2 -> currentList.sortedByDescending { it.meanScore }
                                        else -> currentList
                                    }

                                    if (sortedList.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "İstatistik verisi bulunamadı.", color = KitsugiColors.TextMuted)
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            sortedList.forEachIndexed { idx, item ->
                                                PositionalStatItemCard(
                                                    rank = idx + 1,
                                                    title = if (statsSubTab == 1) item.name.toTurkishGenre() else item.name,
                                                    count = item.count,
                                                    meanScore = item.meanScore,
                                                    timeSpentMinutes = item.timeSpentMinutes,
                                                    chaptersRead = item.chaptersRead,
                                                    imageUrl = item.imageUrl,
                                                    accentColor = accentColor,
                                                    onClick = {
                                                        when (statsSubTab) {
                                                            1 -> onGenreClick(item.name.toEnglishGenreForSearch())
                                                            2 -> onTagClick(item.name)
                                                            3, 4 -> if (item.id != null) onFavoriteStaffClick(item.id, "anilist", item.name, item.imageUrl)
                                                            5 -> if (item.id != null) onFavoriteStudioClick?.invoke(item.id, "anilist", item.name, item.imageUrl) else onTagClick(item.name)
                                                            else -> {}
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            3 -> {
                                // Favorites tab contents
                                val currentFavCategory = when (favoritesFilter) {
                                    0 -> "anime"
                                    1 -> "manga"
                                    2 -> "characters"
                                    3 -> "staff"
                                    4 -> "studios"
                                    else -> "anime"
                                }
                                val currentFavList = when (favoritesFilter) {
                                    0 -> state.favoriteAnime
                                    1 -> state.favoriteManga
                                    2 -> state.favoriteCharacters
                                    3 -> state.favoriteStaff
                                    4 -> state.favoriteStudios
                                    else -> emptyList()
                                }
                                val currentHasNext = when (favoritesFilter) {
                                    0 -> state.favAnimeHasNext
                                    1 -> state.favMangaHasNext
                                    2 -> state.favCharHasNext
                                    3 -> state.favStaffHasNext
                                    4 -> state.favStudioHasNext
                                    else -> false
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
                                        Text(text = "Favori öge bulunamadı.", color = KitsugiColors.TextMuted)
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
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
                                                                    0 -> onFavoriteMediaClick(id, MediaType.Anime, "anilist")
                                                                    1 -> onFavoriteMediaClick(id, MediaType.Manga, "anilist")
                                                                    2 -> onFavoriteCharacterClick(id, "anilist", item.title, item.imageUrl)
                                                                    3 -> onFavoriteStaffClick(id, "anilist", item.title, item.imageUrl)
                                                                    4 -> onFavoriteStudioClick?.invoke(id, "anilist", item.title, item.imageUrl)
                                                                }
                                                            }
                                                        }
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

                                        currentFavList.chunked(3).forEach { rowItems ->
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
                                                                        0 -> onFavoriteMediaClick(id, MediaType.Anime, "anilist")
                                                                        1 -> onFavoriteMediaClick(id, MediaType.Manga, "anilist")
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
                                                                AsyncImage(
                                                                    model = item.imageUrl,
                                                                    contentDescription = item.title,
                                                                    modifier = Modifier
                                                                        .fillMaxSize()
                                                                        .then(if (appSettings.blurAdultMedia && item.isAdult) Modifier.blur(24.dp) else Modifier),
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
                                                // Fill empty slots if last row has fewer than 3 items
                                                if (rowItems.size < 3) {
                                                    repeat(3 - rowItems.size) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }

                                        // "Daha Fazla" butonu
                                        if (currentHasNext) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(accentColor.copy(alpha = 0.15f))
                                                        .clickable { viewModel.loadMoreFavorites(currentFavCategory) }
                                                        .padding(horizontal = 24.dp, vertical = 10.dp)
                                                ) {
                                                    Text(
                                                        text = "Daha Fazla Yükle",
                                                        color = accentColor,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            4 -> {
                                // Social tab contents
                                val userList = if (socialFilter == 0) state.socialState.followers else state.socialState.following
                                if (userList.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
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
                                        userList.chunked(3).forEach { rowItems ->
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
                                                if (rowItems.size < 3) {
                                                    repeat(3 - rowItems.size) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun MalProfileContent(
    viewModel: KitsugiProfileViewModel,
    state: MalProfileState,
    mediaEntries: List<MediaEntry>,
    appSettings: AppSettings,
    onEntryClick: (MediaEntry) -> Unit,
    onFavoriteMediaClick: (mediaId: Int, mediaType: MediaType, source: String) -> Unit,
    onFavoriteCharacterClick: (charId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStaffClick: (staffId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onOpenFavoriteSheet: (title: String, items: List<ProfileFavoriteItem>, onClick: (ProfileFavoriteItem) -> Unit) -> Unit,
    onOpenStatsClick: (() -> Unit)? = null,
    isLandscape: Boolean,
    accentColor: Color,
    onImageClick: ((urls: List<String>, initialIndex: Int, title: String) -> Unit)? = null
) {
    var activeTab by rememberSaveable { mutableIntStateOf(viewModel.malActiveTab) }
    LaunchedEffect(activeTab) { viewModel.malActiveTab = activeTab }

    var statsMediaType by rememberSaveable { mutableIntStateOf(viewModel.malStatsMediaType) }
    LaunchedEffect(statsMediaType) { viewModel.malStatsMediaType = statsMediaType }

    var favoritesFilter by rememberSaveable { mutableIntStateOf(viewModel.malFavoritesFilter) }
    LaunchedEffect(favoritesFilter) { viewModel.malFavoritesFilter = favoritesFilter }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.malScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.malScrollOffset
    )

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        viewModel.updateMalScroll(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = viewModel.malActiveTab,
        pageCount = { 5 }
    )

    LaunchedEffect(pagerState.currentPage) {
        activeTab = pagerState.currentPage
        viewModel.malActiveTab = pagerState.currentPage
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isLandscape) 18.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner and Avatar
        item {
            val context = LocalContext.current
            val avatarUrl = state.avatarUrl?.takeIf { it.isNotBlank() }
            val username = state.name.ifBlank { "MyAnimeList Kullanıcısı" }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(accentColor, KitsugiColors.AccentBlue)))
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, KitsugiColors.Background.copy(alpha = 0.8f))))
                )

                // Share butonu – sağ üst köşe
                if (state.name.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(KitsugiColors.Background.copy(alpha = 0.50f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = {
                            val url = com.kitsugi.animelist.utils.ShareUtils.buildProfileUrl("myanimelist", state.name)
                            com.kitsugi.animelist.utils.ShareUtils.shareText(context, state.name, url)
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Profili Paylaş",
                                tint = KitsugiColors.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Avatar overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = state.avatarUrl ?: "",
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(3.dp, KitsugiColors.Background, CircleShape)
                            .then(
                                if (!avatarUrl.isNullOrBlank()) {
                                    Modifier.clickable {
                                        onImageClick?.invoke(listOf(avatarUrl), 0, "$username Profil Resmi")
                                    }
                                } else Modifier
                            ),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = state.name,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val details = buildList {
                            if (state.location.isNotBlank()) add(state.location)
                            if (state.joinedAt.isNotBlank()) {
                                val dateStr = state.joinedAt.substringBefore("T")
                                add("Katılım: $dateStr")
                            }
                        }.joinToString(" • ")

                        if (details.isNotBlank()) {
                            Text(
                                text = details,
                                color = KitsugiColors.TextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        // Top 5 Icon Sub-Tabs + Sub-filters (Sticky at top)
        stickyHeader(key = "mal_tabs_header") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = KitsugiColors.Background
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val tabs = listOf(
                        Icons.Rounded.Info to "Hakkında",
                        Icons.Rounded.ChatBubble to "Aktivite",
                        Icons.Rounded.BarChart to "İstatistikler",
                        Icons.Rounded.Star to "Favoriler",
                        Icons.Rounded.People to "Sosyal"
                    )
                    ProfileHeaderIconTabs(
                        tabs = tabs,
                        selectedTab = pagerState.currentPage,
                        onTabSelected = { page ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        },
                        accentColor = accentColor
                    )

                    if (pagerState.currentPage == 3) {
                        val favoritesFilterState = rememberLazyListState()
                        androidx.compose.foundation.lazy.LazyRow(
                            state = favoritesFilterState,
                            flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                                lazyListState = favoritesFilterState,
                                snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Start
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(4) { idx ->
                                val label = listOf("Anime", "Manga", "Karakterler", "Ekip")[idx]
                                ProfileFilterChip(
                                    text = label,
                                    isSelected = favoritesFilter == idx,
                                    accentColor = accentColor,
                                    onClick = { favoritesFilter = idx }
                                )
                            }
                        }
                    } else if (pagerState.currentPage == 4) {
                        val userList = state.socialState.followers
                        val socialFilterState = rememberLazyListState()
                        androidx.compose.foundation.lazy.LazyRow(
                            state = socialFilterState,
                            flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                                lazyListState = socialFilterState,
                                snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Start
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                ProfileFilterChip(
                                    text = "Arkadaşlar (${userList.size})",
                                    isSelected = true,
                                    accentColor = accentColor,
                                    onClick = {}
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pager Content
        item(key = "content") {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val pageHeights = remember { androidx.compose.runtime.mutableStateMapOf<Int, Int>() }
            val currentPage = pagerState.currentPage
            val currentPageOffset = pagerState.currentPageOffsetFraction
            val targetPage = if (currentPageOffset > 0f) currentPage + 1 else if (currentPageOffset < 0f) currentPage - 1 else currentPage

            val currentHeightPx = pageHeights[currentPage] ?: 0
            val targetHeightPx = pageHeights[targetPage] ?: currentHeightPx

            val interpolatedHeightDp = remember(currentHeightPx, targetHeightPx, currentPageOffset) {
                val heightPx = if (currentHeightPx > 0 && targetHeightPx > 0) {
                    currentHeightPx + (targetHeightPx - currentHeightPx) * kotlin.math.abs(currentPageOffset)
                } else if (currentHeightPx > 0) {
                    currentHeightPx.toFloat()
                } else {
                    0f
                }
                if (heightPx > 0f) with(density) { heightPx.toDp() } else null
            }

            val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp

            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                beyondViewportPageCount = 1,
                pageSpacing = 12.dp,
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val minPagerHeightPx = with(density) { (screenHeightDp - 64).dp.roundToPx() }
                        val placeable = measurable.measure(
                            constraints.copy(
                                minHeight = minPagerHeightPx,
                                maxHeight = androidx.compose.ui.unit.Constraints.Infinity
                            )
                        )
                        val height = interpolatedHeightDp?.roundToPx()?.coerceAtLeast(minPagerHeightPx) ?: placeable.height
                        layout(placeable.width, height) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                    .clipToBounds()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 600.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                pageHeights[page] = coordinates.size.height
                            },
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (page) {
                            0 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(KitsugiColors.Surface)
                                        .padding(18.dp)
                                ) {
                                    Text(
                                        text = "MyAnimeList Profil Özeti",
                                        color = KitsugiColors.TextPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        StatCard("Anime", state.animeStats?.count?.toString() ?: "0")
                                        StatCard("Manga", state.mangaStats?.count?.toString() ?: "0")
                                    }
                                }
                            }
                            1 -> {
                                val malEntries = remember(mediaEntries) { mediaEntries.filter { it.source == "myanimelist" } }
                                if (malEntries.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "Henüz MyAnimeList aktivitesi bulunmuyor.", color = KitsugiColors.TextMuted)
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        malEntries.take(15).forEach { entry ->
                                            ProfileActivityRow(
                                                title = entry.title,
                                                imageUrl = entry.imageUrl,
                                                statusStr = entry.status.label,
                                                progressStr = "Bölüm: ${entry.progress}",
                                                onClick = { onEntryClick(entry) }
                                            )
                                        }
                                    }
                                }
                            }
                            2 -> {
                                val stats = if (statsMediaType == 0) state.animeStats else state.mangaStats
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(KitsugiColors.Surface)
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(KitsugiColors.SurfaceStrong)
                                            .padding(4.dp)
                                    ) {
                                        listOf("Anime", "Manga").forEachIndexed { idx, label ->
                                            val isSel = statsMediaType == idx
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSel) accentColor else Color.Transparent)
                                                    .clickable { statsMediaType = idx }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSel) KitsugiColors.Background else KitsugiColors.TextMuted,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }

                                    stats?.let { s ->
                                        SegmentedDistributionBar(
                                            watching = s.watching,
                                            completed = s.completed,
                                            planned = s.planned,
                                            paused = s.paused,
                                            dropped = s.dropped,
                                            total = s.count,
                                            accentColor = accentColor
                                        )
                                        StatItemRow("İzliyor/Okuyor", s.watching, s.count, accentColor)
                                        StatItemRow("Tamamlandı", s.completed, s.count, KitsugiColors.AccentGreen)
                                        StatItemRow("Planlanıyor", s.planned, s.count, KitsugiColors.TextMuted)
                                        StatItemRow("Durduruldu", s.paused, s.count, KitsugiColors.AccentOrange)
                                        StatItemRow("Bırakıldı", s.dropped, s.count, KitsugiColors.AccentPink)

                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            StatCard("Toplam Kayıt", s.count.toString())
                                            StatCard("Ortalama Skor", "%.1f".format(s.meanScore))
                                        }
                                    }
                                }
                            }
                            3 -> {
                                val currentFavList = when (favoritesFilter) {
                                    0 -> state.favoriteAnime
                                    1 -> state.favoriteManga
                                    2 -> state.favoriteCharacters
                                    3 -> state.favoriteStaff
                                    else -> emptyList()
                                }

                                val filterTitle = when (favoritesFilter) {
                                    0 -> "Favori Animeler"
                                    1 -> "Favori Mangalar"
                                    2 -> "Favori Karakterler"
                                    3 -> "Favori Ekip"
                                    else -> "Favoriler"
                                }

                                if (currentFavList.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "Favori öge bulunamadı.", color = KitsugiColors.TextMuted)
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
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
                                                                    0 -> onFavoriteMediaClick(id, MediaType.Anime, "jikan")
                                                                    1 -> onFavoriteMediaClick(id, MediaType.Manga, "jikan")
                                                                    2 -> onFavoriteCharacterClick(id, "jikan", item.title, item.imageUrl)
                                                                    3 -> onFavoriteStaffClick(id, "jikan", item.title, item.imageUrl)
                                                                }
                                                            }
                                                        }
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

                                        currentFavList.chunked(3).forEach { rowItems ->
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
                                                                        0 -> onFavoriteMediaClick(id, MediaType.Anime, "jikan")
                                                                        1 -> onFavoriteMediaClick(id, MediaType.Manga, "jikan")
                                                                        2 -> onFavoriteCharacterClick(id, "jikan", item.title, item.imageUrl)
                                                                        3 -> onFavoriteStaffClick(id, "jikan", item.title, item.imageUrl)
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
                                                                AsyncImage(
                                                                    model = item.imageUrl,
                                                                    contentDescription = item.title,
                                                                    modifier = Modifier
                                                                        .fillMaxSize()
                                                                        .then(if (appSettings.blurAdultMedia && item.isAdult) Modifier.blur(24.dp) else Modifier),
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
                                                if (rowItems.size < 3) {
                                                    repeat(3 - rowItems.size) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            4 -> {
                                val userList = state.socialState.followers
                                if (userList.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "Arkadaş bulunamadı.", color = KitsugiColors.TextMuted)
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        userList.chunked(3).forEach { rowItems ->
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
                                                repeat(3 - rowItems.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

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

@Composable
fun SimklProfileContent(
    viewModel: KitsugiProfileViewModel,
    state: SimklProfileState,
    mediaEntries: List<MediaEntry>,
    appSettings: AppSettings,
    onEntryClick: (MediaEntry) -> Unit,
    onFavoriteMediaClick: (mediaId: Int, mediaType: MediaType, source: String) -> Unit,
    onOpenFavoriteSheet: (title: String, items: List<ProfileFavoriteItem>, onClick: (ProfileFavoriteItem) -> Unit) -> Unit,
    onOpenStatsClick: (() -> Unit)? = null,
    isLandscape: Boolean,
    accentColor: Color,
    onImageClick: ((urls: List<String>, initialIndex: Int, title: String) -> Unit)? = null
) {
    var activeTab by rememberSaveable { mutableIntStateOf(viewModel.simklActiveTab) }
    val coroutineScope = rememberCoroutineScope()
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = viewModel.simklActiveTab.coerceIn(0, 2),
        pageCount = { 3 }
    )
    LaunchedEffect(pagerState.currentPage) {
        activeTab = pagerState.currentPage
        viewModel.simklActiveTab = pagerState.currentPage
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.simklScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.simklScrollOffset
    )

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        viewModel.updateSimklScroll(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isLandscape) 18.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner and Avatar
        item {
            val avatarUrl = state.avatarUrl?.takeIf { it.isNotBlank() }
            val username = state.name.ifBlank { "Simkl Kullanıcısı" }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(accentColor, KitsugiColors.AccentOrange)))
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, KitsugiColors.Background.copy(alpha = 0.8f))))
                )

                // Share butonu – sağ üst köşe
                if (state.name.isNotBlank()) {
                    val context = LocalContext.current
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(KitsugiColors.Background.copy(alpha = 0.50f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = {
                            val url = com.kitsugi.animelist.utils.ShareUtils.buildProfileUrl("simkl", state.name)
                            com.kitsugi.animelist.utils.ShareUtils.shareText(context, state.name, url)
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Profili Paylaş",
                                tint = KitsugiColors.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Avatar overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = state.avatarUrl ?: "",
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(3.dp, KitsugiColors.Background, CircleShape)
                            .then(
                                if (!avatarUrl.isNullOrBlank()) {
                                    Modifier.clickable {
                                        onImageClick?.invoke(listOf(avatarUrl), 0, "$username Profil Resmi")
                                    }
                                } else Modifier
                            ),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = state.name,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (!state.accountType.isNullOrBlank()) {
                            Text(
                                text = "Hesap Türü: ${state.accountType}",
                                color = KitsugiColors.AccentOrange,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Top 3 Icon Sub-Tabs (Sticky at top)
        stickyHeader(key = "simkl_tabs_header") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = KitsugiColors.Background
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val tabs = listOf(
                        Icons.Rounded.Info to "Hakkında",
                        Icons.Rounded.ChatBubble to "Aktivite",
                        Icons.Rounded.BarChart to "İstatistikler"
                    )
                    ProfileHeaderIconTabs(
                        tabs = tabs,
                        selectedTab = pagerState.currentPage,
                        onTabSelected = { page ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        },
                        accentColor = accentColor
                    )
                }
            }
        }

        // Pager Content
        item(key = "content") {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val pageHeights = remember { androidx.compose.runtime.mutableStateMapOf<Int, Int>() }
            val currentPage = pagerState.currentPage
            val currentPageOffset = pagerState.currentPageOffsetFraction
            val targetPage = if (currentPageOffset > 0f) currentPage + 1 else if (currentPageOffset < 0f) currentPage - 1 else currentPage

            val currentHeightPx = pageHeights[currentPage] ?: 0
            val targetHeightPx = pageHeights[targetPage] ?: currentHeightPx

            val interpolatedHeightDp = remember(currentHeightPx, targetHeightPx, currentPageOffset) {
                val heightPx = if (currentHeightPx > 0 && targetHeightPx > 0) {
                    currentHeightPx + (targetHeightPx - currentHeightPx) * kotlin.math.abs(currentPageOffset)
                } else if (currentHeightPx > 0) {
                    currentHeightPx.toFloat()
                } else {
                    0f
                }
                if (heightPx > 0f) with(density) { heightPx.toDp() } else null
            }

            val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp

            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                beyondViewportPageCount = 1,
                pageSpacing = 12.dp,
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val minPagerHeightPx = with(density) { (screenHeightDp - 64).dp.roundToPx() }
                        val placeable = measurable.measure(
                            constraints.copy(
                                minHeight = minPagerHeightPx,
                                maxHeight = androidx.compose.ui.unit.Constraints.Infinity
                            )
                        )
                        val height = interpolatedHeightDp?.roundToPx()?.coerceAtLeast(minPagerHeightPx) ?: placeable.height
                        layout(placeable.width, height) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                    .clipToBounds()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 600.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                pageHeights[page] = coordinates.size.height
                            },
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (page) {
                            0 -> {
                                if (!state.bio.isNullOrBlank()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(KitsugiColors.Surface)
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Hakkında",
                                            color = KitsugiColors.TextPrimary,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = state.bio,
                                            color = KitsugiColors.TextSecondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(KitsugiColors.Surface)
                                        .padding(18.dp)
                                ) {
                                    Text(
                                        text = "Simkl Profil Özeti",
                                        color = KitsugiColors.TextPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        StatCard("Anime Sayısı", state.totalAnime.toString())
                                        StatCard("Dizi/TV Sayısı", state.totalShows.toString())
                                        StatCard("Film Sayısı", state.totalMovies.toString())
                                    }
                                }
                            }
                            1 -> {
                                if (state.recentHistory.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        state.recentHistory.forEach { item ->
                                            ProfileActivityRow(
                                                title = item.title,
                                                imageUrl = item.imageUrl,
                                                statusStr = "İzlendi",
                                                progressStr = null,
                                                onClick = {
                                                    item.id.toIntOrNull()?.let { id ->
                                                        onFavoriteMediaClick(id, MediaType.Anime, "simkl")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "Henüz Simkl aktivitesi bulunmuyor.", color = KitsugiColors.TextMuted)
                                    }
                                }
                            }
                            2 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(KitsugiColors.Surface)
                                        .padding(18.dp)
                                ) {
                                    Text(
                                        text = "İstatistikler",
                                        color = KitsugiColors.TextPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        StatCard("Toplam Anime", state.totalAnime.toString())
                                        StatCard("Diziler", state.totalShows.toString())
                                        StatCard("Filmler", state.totalMovies.toString())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ProfileFilterChip(
    text: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) accentColor else KitsugiColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) KitsugiColors.Background else KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun SegmentedDistributionBar(
    watching: Int,
    completed: Int,
    planned: Int,
    paused: Int,
    dropped: Int,
    total: Int,
    accentColor: Color
) {
    if (total <= 0) return

    val wPct = watching.toFloat() / total.toFloat()
    val cPct = completed.toFloat() / total.toFloat()
    val plPct = planned.toFloat() / total.toFloat()
    val paPct = paused.toFloat() / total.toFloat()
    val dPct = dropped.toFloat() / total.toFloat()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(KitsugiColors.Background)
    ) {
        if (wPct > 0) Box(modifier = Modifier.weight(wPct).fillMaxHeight().background(accentColor))
        if (cPct > 0) Box(modifier = Modifier.weight(cPct).fillMaxHeight().background(KitsugiColors.AccentGreen))
        if (plPct > 0) Box(modifier = Modifier.weight(plPct).fillMaxHeight().background(KitsugiColors.TextMuted))
        if (paPct > 0) Box(modifier = Modifier.weight(paPct).fillMaxHeight().background(KitsugiColors.AccentOrange))
        if (dPct > 0) Box(modifier = Modifier.weight(dPct).fillMaxHeight().background(KitsugiColors.AccentPink))
    }
}

@Composable
fun StatItemRow(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val pct = if (total > 0) count.toFloat() / total.toFloat() else 0f
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count (${"%.1f".format(pct * 100f)}%)",
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = KitsugiColors.Background
        )
    }
}

@Composable
fun FilterChipItem(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    accentColor: Color = LocalKitsugiAccent.current
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accentColor.copy(alpha = 0.22f) else KitsugiColors.SurfaceStrong)
            .border(
                width = 1.dp,
                color = if (selected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            color = if (selected) KitsugiColors.TextPrimary else KitsugiColors.TextMuted,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun RowScope.StatCard(
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.weight(1f).padding(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KitsugiColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
        }
    }
}

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
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (blurAdultMedia && item.isAdult) Modifier.blur(24.dp) else Modifier),
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

    KitsugiSheetOrDialog(
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
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
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = item.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(if (blurAdultMedia && item.isAdult) Modifier.blur(24.dp) else Modifier),
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .clickable { onLoadMore() }
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Daha Fazla Yükle",
                                    color = accentColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityCard(
    activity: ProfileActivityItem,
    accentColor: Color,
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        AsyncImage(
                            model = activity.mediaImage ?: "",
                            contentDescription = null,
                            modifier = Modifier
                                .size(45.dp, 64.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .then(if (blurAdultMedia && (activity as? ProfileActivityItem.ListActivity)?.isAdult == true) Modifier.blur(24.dp) else Modifier),
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

private fun formatActivityTime(timestamp: Long): String {
    val diff = (System.currentTimeMillis() / 1000) - timestamp
    return when {
        diff < 60 -> "Şimdi"
        diff < 3600 -> "${diff / 60} dakika önce"
        diff < 86400 -> "${diff / 3600} saat önce"
        else -> "${diff / 86400} gün önce"
    }
}

@Composable
fun PositionalStatItemCard(
    rank: Int,
    title: String,
    count: Int,
    meanScore: Double,
    timeSpentMinutes: Int? = null,
    chaptersRead: Int? = null,
    imageUrl: String? = null,
    accentColor: Color,
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        days >= 1 -> "${days} gün"
                        hours >= 1 -> "${hours} saat"
                        else -> "${timeSpentMinutes} dak"
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
