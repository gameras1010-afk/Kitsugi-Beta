@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.kitsugi.animelist.ui.screens.profile

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.app.ProfileFavoriteItem
import com.kitsugi.animelist.ui.app.RankedStatItem
import com.kitsugi.animelist.ui.components.KitsugiActivityDetailBottomSheet
import com.kitsugi.animelist.ui.components.KitsugiImageGalleryDialog
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.utils.ShareUtils
import com.kitsugi.animelist.utils.toEnglishGenreForSearch
import com.kitsugi.animelist.utils.toTurkishGenre
import kotlinx.coroutines.launch

@Composable
fun KitsugiUserProfileScreen(
    userId: Int,
    fallbackUsername: String? = null,
    fallbackAvatar: String? = null,
    appSettings: AppSettings,
    mediaEntries: List<MediaEntry>,
    onBackClick: () -> Unit,
    onFavoriteMediaClick: (mediaId: Int, mediaType: MediaType, source: String) -> Unit,
    onFavoriteCharacterClick: (charId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStaffClick: (staffId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStudioClick: ((studioId: Int, source: String, name: String?, imageUrl: String?) -> Unit)? = null,
    onUserProfileClick: (userId: Int, username: String, avatarUrl: String?) -> Unit,
    onGenreClick: (genre: String) -> Unit = {},
    onTagClick: (tag: String) -> Unit = {},
    onOpenUserMediaList: (userId: Int, mediaType: MediaType) -> Unit = { _, _ -> },
    accentColor: Color = LocalKitsugiAccent.current,
    viewModel: KitsugiUserProfileViewModel = viewModel()
) {
    LaunchedEffect(userId) {
        viewModel.loadUser(userId, fallbackUsername, fallbackAvatar)
    }

    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember { JikanApiClient() }

    val activeTab = viewModel.activeTab
    val statsMediaType = viewModel.statsMediaType
    val statsSubTab = viewModel.statsSubTab
    val favoritesFilter = viewModel.favoritesFilter
    val socialFilter = viewModel.socialFilter

    var scoreDistType by remember { mutableStateOf(0) }
    var lengthDistType by remember { mutableStateOf(0) }
    var releaseYearDistType by remember { mutableStateOf(0) }
    var startYearDistType by remember { mutableStateOf(0) }

    var activeFavoriteSheet by remember { mutableStateOf<Pair<String, List<ProfileFavoriteItem>>?>(null) }
    var activeActivityIdForDetail by remember { mutableStateOf<Int?>(null) }
    var activeGalleryImages by remember { mutableStateOf<Triple<List<String>, Int, String>?>(null) }

    val pullRefreshState = rememberPullToRefreshState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
    ) {
        // Sticky Header / Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = KitsugiColors.Surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Geri",
                            tint = KitsugiColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = state.name.ifBlank { fallbackUsername ?: "Kullanıcı Profili" },
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (state.name.isNotBlank()) {
                    IconButton(onClick = {
                        val url = ShareUtils.buildProfileUrl("anilist", state.name)
                        ShareUtils.shareText(context, state.name, url)
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Profili Paylaş",
                            tint = KitsugiColors.TextPrimary
                        )
                    }
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading && state.name.isNotBlank(),
            onRefresh = { viewModel.loadUser(userId, fallbackUsername, fallbackAvatar) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = pullRefreshState
        ) {
            if (state.isLoading && state.name.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (isLandscape) 18.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Banner and Avatar Header Card
                    item {
                        val avatarUrl = (state.avatarUrl ?: fallbackAvatar)?.takeIf { it.isNotBlank() }
                        val bannerUrl = state.bannerUrl?.takeIf { it.isNotBlank() }
                        val imageList = listOfNotNull(avatarUrl, bannerUrl)
                        val username = state.name.ifBlank { fallbackUsername ?: "Kullanıcı" }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .then(
                                    if (!bannerUrl.isNullOrBlank()) {
                                        Modifier.clickable {
                                            val idx = imageList.indexOf(bannerUrl).coerceAtLeast(0)
                                            activeGalleryImages = Triple(imageList, idx, "$username Banner")
                                        }
                                    } else Modifier
                                )
                        ) {
                            if (!bannerUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = bannerUrl,
                                    contentDescription = "Banner",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.horizontalGradient(listOf(accentColor, KitsugiColors.AccentBlue)))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, KitsugiColors.Background.copy(alpha = 0.85f))))
                            )

                            // Avatar & User Info Row
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = avatarUrl ?: "",
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .border(3.dp, KitsugiColors.Background, CircleShape)
                                        .then(
                                            if (!avatarUrl.isNullOrBlank()) {
                                                Modifier.clickable {
                                                    val idx = imageList.indexOf(avatarUrl).coerceAtLeast(0)
                                                    activeGalleryImages = Triple(imageList, idx, "$username Profil Resmi")
                                                }
                                            } else Modifier
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = state.name.ifBlank { fallbackUsername ?: "AniList Kullanıcısı" },
                                            color = KitsugiColors.TextPrimary,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val badge = state.donatorBadge
                                        if (!badge.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = badge,
                                                color = accentColor,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(accentColor.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (state.isFollower) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Sizi takip ediyor",
                                            color = KitsugiColors.AccentGreen,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Action Buttons: Follow / Unfollow & Media List Buttons
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Follow / Unfollow Button
                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (state.isFollowing) KitsugiColors.SurfaceStrong else accentColor)
                                    .clickable(enabled = !state.isFollowLoading) { viewModel.toggleFollow() }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isFollowLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = if (state.isFollowing) KitsugiColors.TextPrimary else KitsugiColors.Background,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (state.isFollowing) Icons.Rounded.Check else Icons.Rounded.PersonAdd,
                                            contentDescription = null,
                                            tint = if (state.isFollowing) KitsugiColors.TextPrimary else KitsugiColors.Background,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (state.isFollowing) "Takip Ediliyorsun" else "Takip Et",
                                            color = if (state.isFollowing) KitsugiColors.TextPrimary else KitsugiColors.Background,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }

                            // View Anime List Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(KitsugiColors.Surface)
                                    .clickable { onOpenUserMediaList(state.userId, MediaType.Anime) }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.ListAlt,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Anime Listesi",
                                        color = KitsugiColors.TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            // View Manga List Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(KitsugiColors.Surface)
                                    .clickable { onOpenUserMediaList(state.userId, MediaType.Manga) }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.ListAlt,
                                        contentDescription = null,
                                        tint = KitsugiColors.AccentOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Manga Listesi",
                                        color = KitsugiColors.TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    // 3. Top Icon Sub-Tabs (Sticky Header)
                    stickyHeader(key = "other_user_tabs_header") {
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
                                    selectedTab = activeTab,
                                    onTabSelected = { viewModel.activeTab = it },
                                    accentColor = accentColor
                                )

                                // Sub-filter chips per tab
                                if (activeTab == 2) {
                                    val subTabs = listOf("Genel", "Türler", "Etiketler", "Ekip", "Seslendirmen", "Stüdyo")
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        subTabs.forEachIndexed { idx, label ->
                                            ProfileFilterChip(
                                                text = label,
                                                isSelected = statsSubTab == idx,
                                                accentColor = accentColor,
                                                onClick = { viewModel.statsSubTab = idx }
                                            )
                                        }
                                    }
                                } else if (activeTab == 3) {
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
                                                onClick = { viewModel.favoritesFilter = idx }
                                            )
                                        }
                                    }
                                } else if (activeTab == 4) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ProfileFilterChip(
                                            text = "Takipçiler (${state.socialState.followers.size})",
                                            isSelected = socialFilter == 0,
                                            accentColor = accentColor,
                                            onClick = { viewModel.socialFilter = 0 }
                                        )
                                        ProfileFilterChip(
                                            text = "Takip Edilen (${state.socialState.following.size})",
                                            isSelected = socialFilter == 1,
                                            accentColor = accentColor,
                                            onClick = { viewModel.socialFilter = 1 }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // TAB 0: INFO / ABOUT
                    if (activeTab == 0) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(KitsugiColors.Surface)
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Kullanıcı Hakkında",
                                    color = KitsugiColors.TextPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                if (state.about.isNotBlank()) {
                                    Text(
                                        text = state.about,
                                        color = KitsugiColors.TextSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Text(
                                        text = "Bu kullanıcı henüz bir biyografi eklemedi.",
                                        color = KitsugiColors.TextMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                HorizontalDivider(color = KitsugiColors.SurfaceStrong)

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    StatCard("Anime Kayıt", state.animeStats?.count?.toString() ?: "0")
                                    StatCard("Manga Kayıt", state.mangaStats?.count?.toString() ?: "0")
                                }
                            }
                        }
                    }

                    // TAB 1: ACTIVITIES
                    if (activeTab == 1) {
                        if (state.activities.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "Son aktivite bulunamadı.", color = KitsugiColors.TextMuted)
                                }
                            }
                        } else {
                            items(state.activities) { act ->
                                ActivityCard(
                                    activity = act,
                                    accentColor = accentColor,
                                    blurAdultMedia = appSettings.blurAdultMedia,
                                    onMediaClick = { mediaId, mType ->
                                        onFavoriteMediaClick(mediaId, mType, "anilist")
                                    },
                                    onActivityClick = { actId ->
                                        activeActivityIdForDetail = actId
                                    },
                                    onLikeClick = { actId ->
                                        coroutineScope.launch {
                                            apiClient.toggleLike(actId, "ACTIVITY")
                                            viewModel.loadUser(userId, fallbackUsername, fallbackAvatar)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // TAB 2: STATISTICS
                    if (activeTab == 2) {
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
                                    // Media Switcher (Anime / Manga)
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
                                                    .clickable { viewModel.statsMediaType = idx }
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

                                    overview?.let { ov ->
                                        Text(
                                            text = if (statsMediaType == 0) "Anime Genel İstatistikleri" else "Manga Genel İstatistikleri",
                                            color = KitsugiColors.TextPrimary,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        val stats = if (statsMediaType == 0) state.animeStats else state.mangaStats
                                        val total = stats?.count ?: ov.count

                                        SegmentedDistributionBar(
                                            watching = stats?.watching ?: 0,
                                            completed = stats?.completed ?: 0,
                                            planned = stats?.planned ?: 0,
                                            paused = stats?.paused ?: 0,
                                            dropped = stats?.dropped ?: 0,
                                            total = total,
                                            accentColor = accentColor
                                        )

                                        StatItemRow(if (statsMediaType == 0) "İzliyor" else "Okuyor", stats?.watching ?: 0, total, accentColor)
                                        StatItemRow("Tamamlandı", stats?.completed ?: 0, total, KitsugiColors.AccentGreen)
                                        StatItemRow("Planlanıyor", stats?.planned ?: 0, total, KitsugiColors.TextMuted)
                                        StatItemRow("Durduruldu", stats?.paused ?: 0, total, KitsugiColors.AccentOrange)
                                        StatItemRow("Bırakıldı", stats?.dropped ?: 0, total, KitsugiColors.AccentPink)

                                        HorizontalDivider(color = KitsugiColors.SurfaceStrong)

                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            StatCard("Ortalama Skor", "%.1f".format(ov.meanScore))
                                            StatCard("Standart Sapma", "%.1f".format(ov.standardDeviation))
                                        }
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            StatCard(if (statsMediaType == 0) "İzlenen Bölüm" else "Okunan Cilt", ov.episodesWatched.toString())
                                            StatCard(if (statsMediaType == 0) "İzlenen Gün" else "Okunan Gün", "%.1f".format(ov.daysWatched))
                                        }
                                    } ?: Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "İstatistik yüklenemedi.", color = KitsugiColors.TextMuted)
                                    }
                                }
                            }
                        } else {
                            val subTabTitle = when (statsSubTab) {
                                1 -> "Türler"
                                2 -> "Etiketler"
                                3 -> "Ekip"
                                4 -> "Seslendirenler"
                                5 -> "Stüdyolar"
                                else -> ""
                            }
                            val currentList: List<RankedStatItem> = when (statsSubTab) {
                                1 -> overview?.genreList.orEmpty()
                                2 -> overview?.tagList.orEmpty()
                                3 -> overview?.staffList.orEmpty()
                                4 -> overview?.voiceActorList.orEmpty()
                                5 -> overview?.studioList.orEmpty()
                                else -> emptyList()
                            }

                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(KitsugiColors.Surface)
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = subTabTitle,
                                        color = KitsugiColors.TextPrimary,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
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
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    // TAB 3: FAVORITES
                    if (activeTab == 3) {
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
                                                activeFavoriteSheet = filterTitle to currentFavList
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
                            }

                            items(currentFavList.chunked(3)) { rowItems ->
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
                                        repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 4: SOCIAL (Followers / Following)
                    if (activeTab == 4) {
                        val userList = if (socialFilter == 0) state.socialState.followers else state.socialState.following
                        if (userList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (socialFilter == 0) "Takipçi bulunamadı." else "Takip edilen kullanıcı bulunamadı.",
                                        color = KitsugiColors.TextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(userList.chunked(3)) { rowItems ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
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
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                    if (rowItems.size < 3) {
                                        repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                }
                            }
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
            blurAdultMedia = appSettings.blurAdultMedia,
            onItemClick = { item ->
                item.id.toIntOrNull()?.let { id ->
                    when (favoritesFilter) {
                        0 -> onFavoriteMediaClick(id, MediaType.Anime, "anilist")
                        1 -> onFavoriteMediaClick(id, MediaType.Manga, "anilist")
                        2 -> onFavoriteCharacterClick(id, "anilist", item.title, item.imageUrl)
                        3 -> onFavoriteStaffClick(id, "anilist", item.title, item.imageUrl)
                        4 -> onFavoriteStudioClick?.invoke(id, "anilist", item.title, item.imageUrl)
                    }
                }
                activeFavoriteSheet = null
            },
            onDismiss = { activeFavoriteSheet = null }
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
                viewModel.loadUser(userId, fallbackUsername, fallbackAvatar)
            }
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
