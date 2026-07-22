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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.ContentCopy
import android.widget.Toast
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
import androidx.compose.runtime.DisposableEffect
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
import com.kitsugi.animelist.ui.components.KitsugiMarkdownText
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import android.content.Context
import com.kitsugi.animelist.utils.KitsugiMarkdownUtils.cleanUserAboutText
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator
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
    accentColor: Color? = null,
    customViewModel: KitsugiUserProfileViewModel? = null
) {
    val accentColor = accentColor ?: LocalKitsugiAccent.current
    val viewModel: KitsugiUserProfileViewModel = customViewModel ?: viewModel(key = "user_profile_${userId}")

    LaunchedEffect(userId) {
        viewModel.loadUser(userId, fallbackUsername, fallbackAvatar)
    }

    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember { JikanApiClient() }

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = viewModel.activeTab.coerceIn(0, 4),
        pageCount = { 5 }
    )
    LaunchedEffect(pagerState.currentPage) { viewModel.activeTab = pagerState.currentPage }
    val activeTab = pagerState.currentPage
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

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.scrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.scrollOffset
    )

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        viewModel.updateScroll(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset
        )
    }

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
                        text = state.name.ifBlank { fallbackUsername ?: "Kullan─▒c─▒ Profili" },
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
                            contentDescription = "Profili Payla┼ş",
                            tint = KitsugiColors.TextPrimary
                        )
                    }
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading && state.name.isNotBlank(),
            onRefresh = { viewModel.loadUser(userId, fallbackUsername, fallbackAvatar, forceRefresh = true) },
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
                    state = listState,
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
                        val username = state.name.ifBlank { fallbackUsername ?: "Kullan─▒c─▒" }

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
                                            text = state.name.ifBlank { fallbackUsername ?: "AniList Kullan─▒c─▒s─▒" },
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
                                    selectedTab = pagerState.currentPage,
                                    onTabSelected = { coroutineScope.launch { pagerState.animateScrollToPage(it) } },
                                    accentColor = accentColor
                                )

                                // Sub-filter chips per tab
                                if (activeTab == 2) {
                                    val subTabs = listOf("Genel", "Türler", "Etiketler", "Ekip", "Seslendirenler", "Stüdyolar")
                                    val statsSubTabState = rememberLazyListState()
                                    LazyRow(
                                        state = statsSubTabState,
                                        flingBehavior = rememberSnapFlingBehavior(
                                            lazyListState = statsSubTabState,
                                            snapPosition = SnapPosition.Start
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(subTabs.size) { idx ->
                                            ProfileFilterChip(text = subTabs[idx], isSelected = statsSubTab == idx, accentColor = accentColor, onClick = { viewModel.statsSubTab = idx })
                                        }
                                    }
                                } else if (activeTab == 3) {
                                    val favoritesFilterState = rememberLazyListState()
                                    LazyRow(
                                        state = favoritesFilterState,
                                        flingBehavior = rememberSnapFlingBehavior(
                                            lazyListState = favoritesFilterState,
                                            snapPosition = SnapPosition.Start
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(5) { idx ->
                                            val labels = listOf("Anime", "Manga", "Karakterler", "Ekip", "Stüdyolar")
                                            ProfileFilterChip(text = labels[idx], isSelected = favoritesFilter == idx, accentColor = accentColor, onClick = { viewModel.favoritesFilter = idx })
                                        }
                                    }
                                } else if (activeTab == 4) {
                                    val socialFilterState = rememberLazyListState()
                                    LazyRow(
                                        state = socialFilterState,
                                        flingBehavior = rememberSnapFlingBehavior(
                                            lazyListState = socialFilterState,
                                            snapPosition = SnapPosition.Start
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item { ProfileFilterChip(text = "Takipçiler (${state.socialState.followers.size})", isSelected = socialFilter == 0, accentColor = accentColor, onClick = { viewModel.socialFilter = 0 }) }
                                        item { ProfileFilterChip(text = "Takip Edilen (${state.socialState.following.size})", isSelected = socialFilter == 1, accentColor = accentColor, onClick = { viewModel.socialFilter = 1 }) }
                                    }
                                }
                            }
                        }
                    }

                    // Pager Content
                    item(key = "user_content") {
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

                        val screenHeightDp = LocalConfiguration.current.screenHeightDp

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
                                                    .padding(18.dp),
                                                verticalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                val displayAbout = remember(state.about) { state.about.cleanUserAboutText() }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Kullanıcı Biyografisi",
                                                        color = KitsugiColors.TextPrimary,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (displayAbout.isNotBlank()) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            IconButton(
                                                                onClick = { context.openTranslator(displayAbout) },
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
                                                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("about", displayAbout))
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
                                                }
                                                if (displayAbout.isNotBlank()) {
                                                    KitsugiMarkdownText(
                                                        text = displayAbout,
                                                        fontSize = 14.sp,
                                                        lineHeight = 20.sp
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
                                        1 -> {
                                            if (state.activities.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(32.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = "Son aktivite bulunamadı.", color = KitsugiColors.TextMuted)
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

                                                    if (state.activitiesHasNext) {
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
                                                                    .clickable { viewModel.loadNextActivitiesPage() }
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
                                        2 -> {
                                            val overview = if (statsMediaType == 0) state.animeOverviewStats else state.mangaOverviewStats
                                            if (statsSubTab == 0) {
                                                if (overview != null) {
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

                                                        overview.let { ov ->
                                                            // 1. Key Stats Grid (3x2)
                                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                Row(modifier = Modifier.fillMaxWidth()) {
                                                                    StatCard("Toplam", ov.count.toString())
                                                                    StatCard(if (statsMediaType == 0) "İzlenen bölüm" else "Okunan bölüm", ov.episodesWatched.toString())
                                                                    StatCard(if (statsMediaType == 0) "İzlenen gün" else "Okunan cilt", "%.1f".format(ov.daysWatched))
                                                                }
                                                                Row(modifier = Modifier.fillMaxWidth()) {
                                                                    StatCard(if (statsMediaType == 0) "Planlanan gün" else "Planlanan bölüm", "%.1f".format(ov.plannedDaysOrCount))
                                                                    StatCard("Ortalama Puan", "%.2f".format(ov.meanScore))
                                                                    StatCard("Standart sapma", "%.1f".format(ov.standardDeviation))
                                                                }
                                                            }

                                                            HorizontalDivider(color = KitsugiColors.SurfaceStrong)

                                                            // 2. Score Distribution
                                                            if (ov.scoreList.isNotEmpty()) {
                                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                    Text(
                                                                        text = "Puan",
                                                                        color = KitsugiColors.TextPrimary,
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    val scoreDistListState = rememberLazyListState()
                                                                    LazyRow(
                                                                        state = scoreDistListState,
                                                                        flingBehavior = rememberSnapFlingBehavior(
                                                                            lazyListState = scoreDistListState,
                                                                            snapPosition = SnapPosition.Start
                                                                        ),
                                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                        modifier = Modifier.fillMaxWidth()
                                                                    ) {
                                                                        item {
                                                                            FilterChipItem(
                                                                                selected = scoreDistType == 0,
                                                                                text = "Başlık sayısı",
                                                                                onClick = { scoreDistType = 0 },
                                                                                accentColor = accentColor
                                                                            )
                                                                        }
                                                                        item {
                                                                            FilterChipItem(
                                                                                selected = scoreDistType == 1,
                                                                                text = "Harcanan süre",
                                                                                onClick = { scoreDistType = 1 },
                                                                                accentColor = accentColor
                                                                            )
                                                                        }
                                                                    }
                                                                    val mappedStats = ov.scoreList.map { item ->
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
                                                            if (ov.lengthList.isNotEmpty()) {
                                                                HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                    Text(
                                                                        text = if (statsMediaType == 0) "Bölüm Sayısı" else "Cilt/Bölüm Sayısı",
                                                                        color = KitsugiColors.TextPrimary,
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    val lengthDistListState = rememberLazyListState()
                                                                    LazyRow(
                                                                        state = lengthDistListState,
                                                                        flingBehavior = rememberSnapFlingBehavior(
                                                                            lazyListState = lengthDistListState,
                                                                            snapPosition = SnapPosition.Start
                                                                        ),
                                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                        modifier = Modifier.fillMaxWidth()
                                                                    ) {
                                                                        item {
                                                                            FilterChipItem(
                                                                                selected = lengthDistType == 0,
                                                                                text = "Başlık sayısı",
                                                                                onClick = { lengthDistType = 0 },
                                                                                accentColor = accentColor
                                                                            )
                                                                        }
                                                                        item {
                                                                            FilterChipItem(
                                                                                selected = lengthDistType == 1,
                                                                                text = "Harcanan süre",
                                                                                onClick = { lengthDistType = 1 },
                                                                                accentColor = accentColor
                                                                            )
                                                                        }
                                                                        item {
                                                                            FilterChipItem(
                                                                                selected = lengthDistType == 2,
                                                                                text = "Ortalama Puan",
                                                                                onClick = { lengthDistType = 2 },
                                                                                accentColor = accentColor
                                                                            )
                                                                        }
                                                                    }
                                                                    val mappedLength = ov.lengthList.map { item ->
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
                                                            if (ov.statusList.isNotEmpty()) {
                                                                HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                    Text(
                                                                        text = "Durum Dağılımı",
                                                                        color = KitsugiColors.TextPrimary,
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    val statusItems = ov.statusList.map { item ->
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
                                                            if (ov.formatList.isNotEmpty()) {
                                                                HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                    Text(
                                                                        text = "Tür Dağılımı",
                                                                        color = KitsugiColors.TextPrimary,
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    val formatItems = ov.formatList.map { item ->
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
                                                            if (ov.countryList.isNotEmpty()) {
                                                                HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                    Text(
                                                                        text = "Ülke Dağılımı",
                                                                        color = KitsugiColors.TextPrimary,
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    val countryItems = ov.countryList.map { item ->
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
                                                            if (ov.releaseYearList.isNotEmpty()) {
                                                                HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                    Text(
                                                                        text = "Yayın Yılı",
                                                                        color = KitsugiColors.TextPrimary,
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    val releaseYearDistListState = rememberLazyListState()
                                                                    LazyRow(
                                                                        state = releaseYearDistListState,
                                                                        flingBehavior = rememberSnapFlingBehavior(
                                                                            lazyListState = releaseYearDistListState,
                                                                            snapPosition = SnapPosition.Start
                                                                        ),
                                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                        modifier = Modifier.fillMaxWidth()
                                                                    ) {
                                                                        item {
                                                                            FilterChipItem(
                                                                                selected = releaseYearDistType == 0,
                                                                                text = "Başlık sayısı",
                                                                                onClick = { releaseYearDistType = 0 },
                                                                                accentColor = accentColor
                                                                            )
                                                                        }
                                                                        item {
                                                                            FilterChipItem(
                                                                                selected = releaseYearDistType == 1,
                                                                                text = "Harcanan süre",
                                                                                onClick = { releaseYearDistType = 1 },
                                                                                accentColor = accentColor
                                                                            )
                                                                        }
                                                                        item {
                                                                            FilterChipItem(
                                                                                selected = releaseYearDistType == 2,
                                                                                text = "Ortalama Puan",
                                                                                onClick = { releaseYearDistType = 2 },
                                                                                accentColor = accentColor
                                                                            )
                                                                        }
                                                                    }
                                                                    val mappedYears = ov.releaseYearList
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
                                                            if (ov.startYearList.isNotEmpty()) {
                                                                HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                    Text(
                                                                        text = if (statsMediaType == 0) "İzleme Yılı" else "Okuma Yılı",
                                                                        color = KitsugiColors.TextPrimary,
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    val startYearDistListState = rememberLazyListState()
                                                                    LazyRow(
                                                                        state = startYearDistListState,
                                                                        flingBehavior = rememberSnapFlingBehavior(
                                                                            lazyListState = startYearDistListState,
                                                                            snapPosition = SnapPosition.Start
                                                                        ),
                                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                        modifier = Modifier.fillMaxWidth()
                                                                    ) {
                                                                        item {
                                                                            FilterChipItem(
                                                                                selected = startYearDistType == 0,
                                                                                text = "Başlık sayısı",
                                                                                onClick = { startYearDistType = 0 },
                                                                                accentColor = accentColor
                                                                            )
                                                                        }
                                                                        item {
                                                                            FilterChipItem(
                                                                                selected = startYearDistType == 1,
                                                                                text = "Harcanan süre",
                                                                                onClick = { startYearDistType = 1 },
                                                                                accentColor = accentColor
                                                                            )
                                                                        }
                                                                        item {
                                                                            FilterChipItem(
                                                                                selected = startYearDistType == 2,
                                                                                text = "Ortalama Puan",
                                                                                onClick = { startYearDistType = 2 },
                                                                                accentColor = accentColor
                                                                            )
                                                                        }
                                                                    }
                                                                    val mappedStartYears = ov.startYearList
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
                                                        }
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(16.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(text = "İstatistik yüklenemedi.", color = KitsugiColors.TextMuted)
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

                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(24.dp))
                                                            .background(KitsugiColors.Surface)
                                                            .padding(18.dp),
                                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                                    ) {
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
                                                        }

                                                        val sortTypeState = rememberLazyListState()
                                                        LazyRow(
                                                            state = sortTypeState,
                                                            flingBehavior = rememberSnapFlingBehavior(
                                                                lazyListState = sortTypeState,
                                                                snapPosition = SnapPosition.Start
                                                            ),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            item {
                                                                ProfileFilterChip(
                                                                    isSelected = viewModel.statsSortType == 0,
                                                                    text = "Başlık sayısı",
                                                                    onClick = { viewModel.statsSortType = 0 },
                                                                    accentColor = accentColor
                                                                )
                                                            }
                                                            item {
                                                                ProfileFilterChip(
                                                                    isSelected = viewModel.statsSortType == 1,
                                                                    text = "Harcanan süre",
                                                                    onClick = { viewModel.statsSortType = 1 },
                                                                    accentColor = accentColor
                                                                )
                                                            }
                                                            item {
                                                                ProfileFilterChip(
                                                                    isSelected = viewModel.statsSortType == 2,
                                                                    text = "Ortalama Puan",
                                                                    onClick = { viewModel.statsSortType = 2 },
                                                                    accentColor = accentColor
                                                                )
                                                            }
                                                        }
                                                    }

                                                    val sortedList = when (viewModel.statsSortType) {
                                                        0 -> currentList.sortedByDescending { it.count }
                                                        1 -> currentList.sortedByDescending { it.timeSpentMinutes }
                                                        else -> currentList.sortedByDescending { it.meanScore }
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
                                                                        }
                                                                    }
                                                                )
                                                            }
                                                        }
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
                                            val userList = if (socialFilter == 0) state.socialState.followers else state.socialState.following
                                            if (userList.isEmpty()) {
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
                                            } else {
                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    userList.chunked(3).forEach { rowItems ->
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
                        }
                    }
                }
            }
        }

    if (activeFavoriteSheet != null) {
        val currentHasNext = when (favoritesFilter) {
            0 -> state.favAnimeHasNext
            1 -> state.favMangaHasNext
            2 -> state.favCharHasNext
            3 -> state.favStaffHasNext
            4 -> state.favStudioHasNext
            else -> false
        }
        val currentCategory = when (favoritesFilter) {
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
            else -> activeFavoriteSheet!!.second
        }

        FavoritesExpandedBottomSheet(
            title = activeFavoriteSheet!!.first,
            items = currentFavList,
            blurAdultMedia = appSettings.blurAdultMedia,
            hasNextPage = currentHasNext,
            onLoadMore = { viewModel.loadMoreFavorites(currentCategory) },
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
}



