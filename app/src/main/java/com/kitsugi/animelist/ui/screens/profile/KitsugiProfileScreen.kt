@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kitsugi.animelist.ui.screens.profile

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.app.KitsugiProfileViewModel
import com.kitsugi.animelist.ui.components.KitsugiActivityDetailBottomSheet
import com.kitsugi.animelist.ui.components.KitsugiImageGalleryDialog
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlinx.coroutines.launch

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
    onFavoriteMediaClick: (mediaId: Int, mediaType: MediaType, source: String, title: String, imageUrl: String?) -> Unit,
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

    val activeSubTab = viewModel.activeSubTab
    val subTabs = listOf("AniList", "MyAnimeList", "Simkl")

    val aniListState by viewModel.aniListState.collectAsState()
    val malState by viewModel.malState.collectAsState()
    val simklState by viewModel.simklState.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember { JikanApiClient() }

    var activeFavoriteSheet by remember { mutableStateOf<Pair<String, List<com.kitsugi.animelist.ui.app.ProfileFavoriteItem>>?>(null) }
    var onSheetItemClick by remember { mutableStateOf<((com.kitsugi.animelist.ui.app.ProfileFavoriteItem) -> Unit)?>(null) }
    var activeActivityIdForDetail by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<String?>(null) }
    var activeGalleryImages by remember { mutableStateOf<Triple<List<String>, Int, String>?>(null) }

    val openFavoriteSheet: (String, List<com.kitsugi.animelist.ui.app.ProfileFavoriteItem>, (com.kitsugi.animelist.ui.app.ProfileFavoriteItem) -> Unit) -> Unit = { title, items, onClick ->
        activeFavoriteSheet = title to items
        onSheetItemClick = onClick
    }

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
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Platform sub-tab selector ──────────────────────────────────
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
                                .background(if (isSelected) accentColor else KitsugiColors.Surface)
                                .tvClickable(shape = RoundedCornerShape(22.dp), onClick = { viewModel.activeSubTab = index })
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.layout.Row(
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
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
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
                        colors = IconButtonDefaults.iconButtonColors(containerColor = KitsugiColors.Surface),
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(18.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = "Kütüphane İstatistikleri",
                            tint = accentColor
                        )
                    }
                }
            }

            // ── Main content with pull-to-refresh ─────────────────────────
            val isProfileLoading = when (activeSubTab) {
                0 -> aniListState.isLoading
                1 -> malState.isLoading
                else -> simklState.isLoading
            }
            val pullRefreshState = rememberPullToRefreshState()

            PullToRefreshBox(
                isRefreshing = isProfileLoading,
                onRefresh = { viewModel.refreshActiveProfile() },
                modifier = Modifier.fillMaxWidth().weight(1f),
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
                Box(modifier = Modifier.fillMaxSize()) {
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
                                    coroutineScope.launch { apiClient.toggleLike(actId, "ACTIVITY") }
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

    // ── Overlays / Dialogs ─────────────────────────────────────────────────
    if (activeFavoriteSheet != null) {
        val filterTitle = activeFavoriteSheet!!.first
        val currentFavCategory = when (viewModel.aniListFavoritesFilter) {
            0 -> "anime"; 1 -> "manga"; 2 -> "characters"; 3 -> "staff"; 4 -> "studios"; else -> "anime"
        }
        val currentFavList = if (activeSubTab == 0) {
            when (viewModel.aniListFavoritesFilter) {
                0 -> aniListState.favoriteAnime; 1 -> aniListState.favoriteManga
                2 -> aniListState.favoriteCharacters; 3 -> aniListState.favoriteStaff
                4 -> aniListState.favoriteStudios; else -> activeFavoriteSheet!!.second
            }
        } else { activeFavoriteSheet!!.second }
        val currentHasNext = if (activeSubTab == 0) {
            when (viewModel.aniListFavoritesFilter) {
                0 -> aniListState.favAnimeHasNext; 1 -> aniListState.favMangaHasNext
                2 -> aniListState.favCharHasNext; 3 -> aniListState.favStaffHasNext
                4 -> aniListState.favStudioHasNext; else -> false
            }
        } else false

        FavoritesExpandedBottomSheet(
            title = filterTitle,
            items = currentFavList,
            blurAdultMedia = appSettings.blurAdultMedia,
            hasNextPage = currentHasNext,
            onLoadMore = { viewModel.loadMoreFavorites(currentFavCategory) },
            onItemClick = { item -> onSheetItemClick?.invoke(item) },
            onDismiss = { activeFavoriteSheet = null; onSheetItemClick = null }
        )
    }

    if (activeActivityIdForDetail != null) {
        KitsugiActivityDetailBottomSheet(
            activityId = activeActivityIdForDetail!!,
            apiClient = apiClient,
            titleLanguage = appSettings.titleLanguage.toString(),
            blurAdultMedia = appSettings.blurAdultMedia,
            onMediaClick = { mediaId, mType, source -> onFavoriteMediaClick(mediaId, mType, source, "", null) },
            onDismiss = { activeActivityIdForDetail = null; viewModel.fetchAniListProfile() }
        )
    }

    if (showDeleteConfirmDialog != null) {
        val actIdStr = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Aktiviteyi Sil", color = KitsugiColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Bu aktiviteyi silmek istediğinize emin misiniz?", color = KitsugiColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        val actId = actIdStr.toIntOrNull() ?: 0
                        val success = apiClient.deleteActivity(actId)
                        if (success) viewModel.fetchAniListProfile()
                    }
                    showDeleteConfirmDialog = null
                }) {
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
