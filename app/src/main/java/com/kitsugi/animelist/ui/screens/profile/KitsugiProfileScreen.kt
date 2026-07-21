@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import com.kitsugi.animelist.data.remote.JikanApiClient
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
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
import com.kitsugi.animelist.ui.app.KitsugiProfileViewModel
import com.kitsugi.animelist.ui.app.AniListProfileState
import com.kitsugi.animelist.utils.rememberScrollConnection
import com.kitsugi.animelist.utils.rememberScrollVisibilityState
import com.kitsugi.animelist.ui.app.MalProfileState
import com.kitsugi.animelist.ui.app.SimklProfileState
import com.kitsugi.animelist.ui.app.ProfileFavoriteItem
import com.kitsugi.animelist.ui.app.ProfileActivityItem
import com.kitsugi.animelist.ui.components.KitsugiProfileHeaderCard
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiColors
import com.kitsugi.animelist.ui.utils.tvClickable

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
    onOpenStatsClick: (() -> Unit)? = null,
    onLoginAniList: () -> Unit = {},
    onLoginMal: () -> Unit = {},
    onLoginSimkl: () -> Unit = {},
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
                                accentColor = accentColor
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
                                accentColor = accentColor
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
                                state = simklState,
                                mediaEntries = mediaEntries,
                                appSettings = appSettings,
                                onEntryClick = onEntryClick,
                                onFavoriteMediaClick = onFavoriteMediaClick,
                                onOpenFavoriteSheet = openFavoriteSheet,
                                onOpenStatsClick = onOpenStatsClick,
                                isLandscape = isLandscape,
                                accentColor = accentColor
                            )
                        }
                    }
                }
            }
        }
    }

    if (activeFavoriteSheet != null) {
        FavoritesExpandedBottomSheet(
            title = activeFavoriteSheet!!.first,
            items = activeFavoriteSheet!!.second,
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
    modifier: Modifier = Modifier
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

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = value.toInt().toString(),
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
                        .background(accentColor)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, (icon, label) ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) accentColor else Color.Transparent)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) KitsugiColors.Background else KitsugiColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AniListProfileContent(
    state: AniListProfileState,
    mediaEntries: List<MediaEntry>,
    appSettings: AppSettings,
    onEntryClick: (MediaEntry) -> Unit,
    onFavoriteMediaClick: (mediaId: Int, mediaType: MediaType, source: String) -> Unit,
    onFavoriteCharacterClick: (charId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStaffClick: (staffId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onLoadMoreActivities: () -> Unit,
    onOpenFavoriteSheet: (title: String, items: List<ProfileFavoriteItem>, onClick: (ProfileFavoriteItem) -> Unit) -> Unit,
    onOpenStatsClick: (() -> Unit)? = null,
    onActivityClick: ((Int) -> Unit)? = null,
    onLikeClick: ((Int) -> Unit)? = null,
    onDeleteClick: ((Int) -> Unit)? = null,
    isLandscape: Boolean,
    accentColor: Color
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Info, 1: Activity, 2: Stats, 3: Favorites, 4: Social
    var statsMediaType by remember { mutableStateOf(0) } // 0: Anime, 1: Manga
    var statsSubTab by remember { mutableStateOf(0) } // 0: Genel Bakış, 1: Türler, 2: Etiketler, 3: Ekip, 4: Seslendirenler, 5: Stüdyolar
    var favoritesFilter by remember { mutableStateOf(0) } // 0: Anime, 1: Manga, 2: Character, 3: Staff, 4: Studio
    var socialFilter by remember { mutableStateOf(0) } // 0: Followers, 1: Following

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isLandscape) 18.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner and Avatar
        item {
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
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
                            .border(3.dp, KitsugiColors.Background, CircleShape),
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

        // Top 5 Icon Sub-Tabs
        item {
            val tabs = listOf(
                Icons.Rounded.Info to "Hakkında",
                Icons.Rounded.ChatBubble to "Aktivite",
                Icons.Rounded.BarChart to "İstatistikler",
                Icons.Rounded.Star to "Favoriler",
                Icons.Rounded.People to "Sosyal"
            )
            ProfileHeaderIconTabs(
                tabs = tabs,
                selectedTab = activeTab,
                onTabSelected = { activeTab = it },
                accentColor = accentColor
            )
        }

        // TAB 0: INFO
        if (activeTab == 0) {
            if (state.about.isNotBlank()) {
                item {
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
                            text = state.about,
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
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
        }

        // TAB 1: ACTIVITY
        if (activeTab == 1) {
            item {
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
        }

        // TAB 2: STATS
        if (activeTab == 2) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Genel Bakış", "Türler", "Etiketler", "Ekip", "Seslendirenler", "Stüdyolar").forEachIndexed { idx, label ->
                        ProfileFilterChip(
                            text = label,
                            isSelected = statsSubTab == idx,
                            accentColor = accentColor,
                            onClick = { statsSubTab = idx }
                        )
                    }
                }
            }

            val overview = if (statsMediaType == 0) state.animeOverviewStats else state.mangaOverviewStats

            if (statsSubTab == 0) {
                item {
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
                            // Key Stats Grid
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    StatCard("Toplam", overview.count.toString())
                                    StatCard(if (statsMediaType == 0) "İzlenen Bölüm" else "Okunan Bölüm", overview.episodesWatched.toString())
                                    StatCard(if (statsMediaType == 0) "İzlenen Gün" else "Okunan Cilt", "%.1f".format(overview.daysWatched))
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    StatCard("Ortalama Skor", "%.1f".format(overview.meanScore))
                                    StatCard("Standart Sapma", "%.1f".format(overview.standardDeviation))
                                }
                            }

                            HorizontalDivider(color = KitsugiColors.SurfaceStrong)

                            // Score Distribution
                            if (overview.scoreList.isNotEmpty()) {
                                Column {
                                    Text(
                                        text = "Puan Dağılımı",
                                        color = KitsugiColors.TextPrimary,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    VerticalStatsBar(
                                        stats = overview.scoreList.map { it.score.toString() to it.count.toFloat() },
                                        accentColor = accentColor
                                    )
                                }
                            }

                            // Format Distribution
                            if (overview.formatList.isNotEmpty()) {
                                Column {
                                    Text(
                                        text = "Format Dağılımı",
                                        color = KitsugiColors.TextPrimary,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalStatsBar(
                                        stats = overview.formatList.map {
                                            Triple(it.format, it.count, accentColor)
                                        }
                                    )
                                }
                            }

                            // Status Distribution
                            if (overview.statusList.isNotEmpty()) {
                                Column {
                                    Text(
                                        text = "Durum Dağılımı",
                                        color = KitsugiColors.TextPrimary,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalStatsBar(
                                        stats = overview.statusList.map {
                                            val color = when (it.status) {
                                                "CURRENT" -> KitsugiColors.AccentBlue
                                                "COMPLETED" -> KitsugiColors.AccentGreen
                                                "PLANNING" -> KitsugiColors.TextMuted
                                                "PAUSED" -> KitsugiColors.AccentOrange
                                                "DROPPED" -> KitsugiColors.AccentPink
                                                else -> accentColor
                                            }
                                            Triple(it.status, it.count, color)
                                        }
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

                if (currentList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "İstatistik verisi bulunamadı.", color = KitsugiColors.TextMuted)
                        }
                    }
                } else {
                    itemsIndexed(currentList) { idx, item ->
                        Column {
                            PositionalStatItemCard(
                                rank = idx + 1,
                                title = item.name,
                                count = item.count,
                                meanScore = item.meanScore,
                                timeSpentMinutes = item.timeSpentMinutes,
                                chaptersRead = item.chaptersRead,
                                imageUrl = item.imageUrl,
                                accentColor = accentColor,
                                onClick = {
                                    if ((statsSubTab == 3 || statsSubTab == 4) && item.id != null) {
                                        onFavoriteStaffClick(item.id, "anilist", item.name, item.imageUrl)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // TAB 3: FAVORITES
        if (activeTab == 3) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Anime", "Manga", "Karakterler", "Ekip", "Stüdyolar").forEachIndexed { idx, label ->
                        ProfileFilterChip(
                            text = label,
                            isSelected = favoritesFilter == idx,
                            accentColor = accentColor,
                            onClick = { favoritesFilter = idx }
                        )
                    }
                }
            }

            val currentFavList = when (favoritesFilter) {
                0 -> state.favoriteAnime
                1 -> state.favoriteManga
                2 -> state.favoriteCharacters
                3 -> state.favoriteStaff
                4 -> state.favoriteStudios
                else -> emptyList()
            }

            if (currentFavList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Favori öge bulunamadı.", color = KitsugiColors.TextMuted)
                    }
                }
            } else {
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = false
                    ) {
                        items(currentFavList, key = { it.id }) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                    }
                }
            }
        }

        // TAB 4: SOCIAL
        if (activeTab == 4) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileFilterChip(
                        text = "Takipçiler (${state.socialState.followers.size})",
                        isSelected = socialFilter == 0,
                        accentColor = accentColor,
                        onClick = { socialFilter = 0 }
                    )
                    ProfileFilterChip(
                        text = "Takip Edilen (${state.socialState.following.size})",
                        isSelected = socialFilter == 1,
                        accentColor = accentColor,
                        onClick = { socialFilter = 1 }
                    )
                }
            }

            val userList = if (socialFilter == 0) state.socialState.followers else state.socialState.following
            if (userList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Kullanıcı bulunamadı.", color = KitsugiColors.TextMuted)
                    }
                }
            } else {
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = false
                    ) {
                        items(userList, key = { it.id }) { u ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
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
    accentColor: Color
) {
    var activeTab by remember { mutableStateOf(0) }
    var statsMediaType by remember { mutableStateOf(0) }
    var favoritesFilter by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isLandscape) 18.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner and Avatar
        item {
            val context = LocalContext.current
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
                            val url = com.kitsugi.animelist.utils.ShareUtils.buildProfileUrl("mal", state.name)
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
                            .border(3.dp, KitsugiColors.Background, CircleShape),
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

        // Top 5 Icon Sub-Tabs
        item {
            val tabs = listOf(
                Icons.Rounded.Info to "Hakkında",
                Icons.Rounded.ChatBubble to "Aktivite",
                Icons.Rounded.BarChart to "İstatistikler",
                Icons.Rounded.Star to "Favoriler",
                Icons.Rounded.People to "Sosyal"
            )
            ProfileHeaderIconTabs(
                tabs = tabs,
                selectedTab = activeTab,
                onTabSelected = { activeTab = it },
                accentColor = accentColor
            )
        }

        if (activeTab == 0) {
            item {
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
        }

        if (activeTab == 1) {
            item {
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
        }

        if (activeTab == 2) {
            val stats = if (statsMediaType == 0) state.animeStats else state.mangaStats
            item {
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
        }

        if (activeTab == 3) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Anime", "Manga", "Karakterler", "Ekip").forEachIndexed { idx, label ->
                        ProfileFilterChip(
                            text = label,
                            isSelected = favoritesFilter == idx,
                            accentColor = accentColor,
                            onClick = { favoritesFilter = idx }
                        )
                    }
                }
            }

            val currentFavList = when (favoritesFilter) {
                0 -> state.favoriteAnime
                1 -> state.favoriteManga
                2 -> state.favoriteCharacters
                3 -> state.favoriteStaff
                else -> emptyList()
            }

            if (currentFavList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Favori öge bulunamadı.", color = KitsugiColors.TextMuted)
                    }
                }
            } else {
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = false
                    ) {
                        items(currentFavList, key = { it.id }) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                    }
                }
            }
        }

        if (activeTab == 4) {
            val userList = state.socialState.followers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileFilterChip(
                        text = "Arkadaşlar (${userList.size})",
                        isSelected = true,
                        accentColor = accentColor,
                        onClick = {}
                    )
                }
            }

            if (userList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Arkadaş bulunamadı.", color = KitsugiColors.TextMuted)
                    }
                }
            } else {
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = false
                    ) {
                        items(userList, key = { it.id }) { u ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
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
    state: SimklProfileState,
    mediaEntries: List<MediaEntry>,
    appSettings: AppSettings,
    onEntryClick: (MediaEntry) -> Unit,
    onFavoriteMediaClick: (mediaId: Int, mediaType: MediaType, source: String) -> Unit,
    onOpenFavoriteSheet: (title: String, items: List<ProfileFavoriteItem>, onClick: (ProfileFavoriteItem) -> Unit) -> Unit,
    onOpenStatsClick: (() -> Unit)? = null,
    isLandscape: Boolean,
    accentColor: Color
) {
    var activeTab by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isLandscape) 18.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner and Avatar
        item {
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
                            .border(3.dp, KitsugiColors.Background, CircleShape),
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

        // Top 5 Icon Sub-Tabs
        item {
            val tabs = listOf(
                Icons.Rounded.Info to "Hakkında",
                Icons.Rounded.ChatBubble to "Aktivite",
                Icons.Rounded.BarChart to "İstatistikler"
            )
            ProfileHeaderIconTabs(
                tabs = tabs,
                selectedTab = activeTab,
                onTabSelected = { activeTab = it },
                accentColor = accentColor
            )
        }

        if (activeTab == 0) {
            if (!state.bio.isNullOrBlank()) {
                item {
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
            }

            item {
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
        }

        if (activeTab == 1) {
            item {
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
        }

        if (activeTab == 2) {
            item {
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

        if (activeTab == 3) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Simkl favori listesi boş.", color = KitsugiColors.TextMuted)
                }
            }
        }

        if (activeTab == 4) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Simkl sosyal verileri mevcut değil.", color = KitsugiColors.TextMuted)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesExpandedBottomSheet(
    title: String,
    items: List<ProfileFavoriteItem>,
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
}

@Composable
fun ActivityCard(
    activity: ProfileActivityItem,
    accentColor: Color,
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
                                        val mType = if (activity.mediaType == "MANGA") MediaType.Manga else MediaType.Anime
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
                        text = "Başlık",
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
                    val days = timeSpentMinutes / (60 * 24)
                    val hours = (timeSpentMinutes % (60 * 24)) / 60
                    val text = if (days > 0) "${days}g ${hours}s" else "${hours}s"
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = text,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Geçirilen Süre",
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
