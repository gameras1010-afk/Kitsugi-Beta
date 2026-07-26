@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kitsugi.animelist.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.app.KitsugiProfileViewModel
import com.kitsugi.animelist.ui.app.MalProfileState
import com.kitsugi.animelist.ui.app.ProfileFavoriteItem
import com.kitsugi.animelist.ui.components.KitsugiNsfwImage
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.launch

@Composable
fun MalProfileContent(
    viewModel: KitsugiProfileViewModel,
    state: MalProfileState,
    mediaEntries: List<MediaEntry>,
    appSettings: AppSettings,
    onEntryClick: (MediaEntry) -> Unit,
    onFavoriteMediaClick: (mediaId: Int, mediaType: MediaType, source: String, title: String, imageUrl: String?) -> Unit,
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
                                                                    0 -> onFavoriteMediaClick(id, MediaType.Anime, "jikan", item.title, item.imageUrl)
                                                                    1 -> onFavoriteMediaClick(id, MediaType.Manga, "jikan", item.title, item.imageUrl)
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
                                                                        0 -> onFavoriteMediaClick(id, MediaType.Anime, "jikan", item.title, item.imageUrl)
                                                                        1 -> onFavoriteMediaClick(id, MediaType.Manga, "jikan", item.title, item.imageUrl)
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
                                                    repeat(gridColumns - rowItems.size) {
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
                                                repeat(gridColumns - rowItems.size) {
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

