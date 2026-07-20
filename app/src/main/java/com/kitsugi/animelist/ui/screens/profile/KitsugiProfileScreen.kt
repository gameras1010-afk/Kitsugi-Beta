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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kitsugi.animelist.ui.components.KitsugiSheetOrDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier
) {
    val accentColor = LocalKitsugiAccent.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var activeSubTab by remember { mutableStateOf(0) } // 0: AniList, 1: MAL, 2: Simkl
    val subTabs = listOf("AniList", "MyAnimeList", "Simkl")

    val aniListState by viewModel.aniListState.collectAsState()
    val malState by viewModel.malState.collectAsState()
    val simklState by viewModel.simklState.collectAsState()

    val scrollState = rememberScrollVisibilityState(initialVisible = true)

    var activeFavoriteSheet by remember { mutableStateOf<Pair<String, List<ProfileFavoriteItem>>?>(null) }
    var onSheetItemClick by remember { mutableStateOf<((ProfileFavoriteItem) -> Unit)?>(null) }

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
                                .tvClickable(shape = RoundedCornerShape(22.dp), onClick = { activeSubTab = index })
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeSubTab) {
                    0 -> ExternalProfileWrapper(
                        isConnected = isAniListConnected,
                        isLoading = aniListState.isLoading,
                        error = aniListState.error,
                        onOpenSettingsClick = onOpenSettingsClick,
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
                            isLandscape = isLandscape,
                            accentColor = accentColor
                        )
                    }
                    1 -> ExternalProfileWrapper(
                        isConnected = isMalConnected,
                        isLoading = malState.isLoading,
                        error = malState.error,
                        onOpenSettingsClick = onOpenSettingsClick,
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
                        onOpenSettingsClick = onOpenSettingsClick,
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
}

@Composable
fun ExternalProfileWrapper(
    isConnected: Boolean,
    isLoading: Boolean,
    error: String?,
    onOpenSettingsClick: () -> Unit,
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
                        onClick = onOpenSettingsClick,
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
    isLandscape: Boolean,
    accentColor: Color
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Profil, 1: Aktivite
    val tabs = listOf("Profil", "Aktivite")

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

        // About / Bio snippet
        if (state.about.isNotBlank() && activeTab == 0) {
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

        // Sub-tabs row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(KitsugiColors.Surface)
            ) {
                tabs.forEachIndexed { index, label ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = index }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) accentColor else KitsugiColors.TextMuted,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Sub-tab contents
        when (activeTab) {
            0 -> {
                // PROFIL
                state.animeStats?.let { stats ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(KitsugiColors.Surface)
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Anime İstatistikleri",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            SegmentedDistributionBar(
                                watching = stats.watching,
                                completed = stats.completed,
                                planned = stats.planned,
                                paused = stats.paused,
                                dropped = stats.dropped,
                                total = stats.count,
                                accentColor = KitsugiColors.AccentBlue
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            StatItemRow("İzleniyor", stats.watching, stats.count, KitsugiColors.AccentBlue)
                            StatItemRow("Tamamlandı", stats.completed, stats.count, KitsugiColors.AccentGreen)
                            StatItemRow("Planlanıyor", stats.planned, stats.count, KitsugiColors.TextMuted)
                            StatItemRow("Durduruldu", stats.paused, stats.count, KitsugiColors.AccentOrange)
                            StatItemRow("Bırakıldı", stats.dropped, stats.count, KitsugiColors.AccentPink)

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatCard("Toplam Anime", stats.count.toString())
                                StatCard("Ortalama Skor", "%.1f".format(stats.meanScore))
                                StatCard("Süre", "${stats.minutesWatched / 60 / 24} Gün")
                            }
                        }
                    }
                }

                state.mangaStats?.let { stats ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(KitsugiColors.Surface)
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Manga İstatistikleri",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            SegmentedDistributionBar(
                                watching = stats.watching,
                                completed = stats.completed,
                                planned = stats.planned,
                                paused = stats.paused,
                                dropped = stats.dropped,
                                total = stats.count,
                                accentColor = KitsugiColors.AccentBlue
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            StatItemRow("Okuyor", stats.watching, stats.count, KitsugiColors.AccentBlue)
                            StatItemRow("Tamamlandı", stats.completed, stats.count, KitsugiColors.AccentGreen)
                            StatItemRow("Planlanıyor", stats.planned, stats.count, KitsugiColors.TextMuted)
                            StatItemRow("Durduruldu", stats.paused, stats.count, KitsugiColors.AccentOrange)
                            StatItemRow("Bırakıldı", stats.dropped, stats.count, KitsugiColors.AccentPink)

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatCard("Toplam Manga", stats.count.toString())
                                StatCard("Ortalama Skor", "%.1f".format(stats.meanScore))
                                StatCard("Bölümler", stats.episodesWatched.toString())
                            }
                        }
                    }
                }

                if (state.animeStats == null && state.mangaStats == null && mediaEntries.isNotEmpty()) {
                    item {
                        val animeEntries = remember(mediaEntries) { mediaEntries.filter { it.type == MediaType.Anime || it.type == MediaType.Movie || it.type == MediaType.TvShow } }
                        val mangaEntries = remember(mediaEntries) { mediaEntries.filter { it.type == MediaType.Manga } }
                        val totalCount = mediaEntries.size
                        val avgScore = remember(mediaEntries) {
                            val scored = mediaEntries.mapNotNull { it.score }
                            if (scored.isNotEmpty()) scored.average() else 0.0
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(KitsugiColors.Surface)
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Kütüphane İstatistikleri",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                StatCard("Toplam Kayıt", totalCount.toString())
                                StatCard("Ort. Skor", if (avgScore > 0) "%.1f".format(avgScore) else "-")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                StatCard("Animeler", animeEntries.size.toString())
                                StatCard("Mangalar", mangaEntries.size.toString())
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                            Spacer(modifier = Modifier.height(12.dp))

                            StatItemRow("İzleniyor / Okunuyor", mediaEntries.count { it.status == WatchStatus.Watching || it.status == WatchStatus.Repeating }, totalCount, accentColor)
                            StatItemRow("Tamamlandı", mediaEntries.count { it.status == WatchStatus.Completed }, totalCount, KitsugiColors.AccentGreen)
                            StatItemRow("Planlandı", mediaEntries.count { it.status == WatchStatus.Planned }, totalCount, KitsugiColors.TextMuted)
                            StatItemRow("Durduruldu", mediaEntries.count { it.status == WatchStatus.Paused }, totalCount, KitsugiColors.AccentOrange)
                            StatItemRow("Bırakıldı", mediaEntries.count { it.status == WatchStatus.Dropped }, totalCount, KitsugiColors.AccentPink)
                        }
                    }
                }

                item {
                    val localFavs = remember(mediaEntries) {
                        mediaEntries.filter { it.isFavorite }.map { entry ->
                            ProfileFavoriteItem(
                                id = entry.id.toString(),
                                title = entry.title,
                                imageUrl = entry.imageUrl ?: ""
                            ) to entry
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (localFavs.isNotEmpty()) {
                            val localFavItems = localFavs.map { it.first }
                            FavoritesHorizontalSection(
                                title = "Yerel Favoriler",
                                items = localFavItems,
                                onSeeAllClick = {
                                    onOpenFavoriteSheet("Yerel Favoriler", localFavItems) { item ->
                                        localFavs.find { it.first.id == item.id }?.second?.let { onEntryClick(it) }
                                    }
                                },
                                onItemClick = { item ->
                                    localFavs.find { it.first.id == item.id }?.second?.let { onEntryClick(it) }
                                }
                            )
                        }
                        FavoritesHorizontalSection(
                            title = "Favori Animeler",
                            items = state.favoriteAnime,
                            onSeeAllClick = {
                                onOpenFavoriteSheet("Favori Animeler", state.favoriteAnime) { item ->
                                    item.id.toIntOrNull()?.let { id ->
                                        onFavoriteMediaClick(id + 100_000_000, MediaType.Anime, "anilist")
                                    }
                                }
                            },
                            onItemClick = { item ->
                                item.id.toIntOrNull()?.let { id ->
                                    onFavoriteMediaClick(id + 100_000_000, MediaType.Anime, "anilist")
                                }
                            }
                        )
                        FavoritesHorizontalSection(
                            title = "Favori Mangalar",
                            items = state.favoriteManga,
                            onSeeAllClick = {
                                onOpenFavoriteSheet("Favori Mangalar", state.favoriteManga) { item ->
                                    item.id.toIntOrNull()?.let { id ->
                                        onFavoriteMediaClick(id + 100_000_000, MediaType.Manga, "anilist")
                                    }
                                }
                            },
                            onItemClick = { item ->
                                item.id.toIntOrNull()?.let { id ->
                                    onFavoriteMediaClick(id + 100_000_000, MediaType.Manga, "anilist")
                                }
                            }
                        )
                        FavoritesHorizontalSection(
                            title = "Favori Karakterler",
                            items = state.favoriteCharacters,
                            onSeeAllClick = {
                                onOpenFavoriteSheet("Favori Karakterler", state.favoriteCharacters) { item ->
                                    item.id.toIntOrNull()?.let { id ->
                                        onFavoriteCharacterClick(id, "anilist", item.title, item.imageUrl)
                                    }
                                }
                            },
                            onItemClick = { item ->
                                item.id.toIntOrNull()?.let { id ->
                                    onFavoriteCharacterClick(id, "anilist", item.title, item.imageUrl)
                                }
                            }
                        )
                        FavoritesHorizontalSection(
                            title = "Favori Ekip (Staff)",
                            items = state.favoriteStaff,
                            onSeeAllClick = {
                                onOpenFavoriteSheet("Favori Ekip (Staff)", state.favoriteStaff) { item ->
                                    item.id.toIntOrNull()?.let { id ->
                                        onFavoriteStaffClick(id, "anilist", item.title, item.imageUrl)
                                    }
                                }
                            },
                            onItemClick = { item ->
                                item.id.toIntOrNull()?.let { id ->
                                    onFavoriteStaffClick(id, "anilist", item.title, item.imageUrl)
                                }
                            }
                        )
                    }
                }
            }
            1 -> {
                // ACTIVITY
                if (state.activities.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Son aktivite bulunamadı", color = KitsugiColors.TextMuted)
                        }
                    }
                } else {
                    items(state.activities) { activity ->
                        ActivityCard(
                            activity = activity,
                            accentColor = accentColor,
                            onMediaClick = { mediaId, mediaType ->
                                onFavoriteMediaClick(mediaId + 100_000_000, mediaType, "anilist")
                            }
                        )
                    }
                    item {
                        if (state.activitiesHasNext) {
                            Button(
                                onClick = onLoadMoreActivities,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = KitsugiColors.Surface)
                            ) {
                                Text(text = "Daha Fazla Yükle", color = KitsugiColors.TextPrimary)
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
    var activeTab by remember { mutableStateOf(0) } // 0: Profil, 1: Aktivite
    val tabs = listOf("Profil", "Aktivite")

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
                        .background(Brush.horizontalGradient(listOf(accentColor, KitsugiColors.AccentBlue)))
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, KitsugiColors.Background.copy(alpha = 0.8f))))
                )

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

        // Sub-tabs row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(KitsugiColors.Surface)
            ) {
                tabs.forEachIndexed { index, label ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = index }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) accentColor else KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (activeTab == 0) {
            // PROFIL
            state.animeStats?.let { stats ->
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(KitsugiColors.Surface)
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Anime İstatistikleri",
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SegmentedDistributionBar(
                            watching = stats.watching,
                            completed = stats.completed,
                            planned = stats.planned,
                            paused = stats.paused,
                            dropped = stats.dropped,
                            total = stats.count,
                            accentColor = KitsugiColors.AccentBlue
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        StatItemRow("İzleniyor", stats.watching, stats.count, KitsugiColors.AccentBlue)
                        StatItemRow("Tamamlandı", stats.completed, stats.count, KitsugiColors.AccentGreen)
                        StatItemRow("Planlanıyor", stats.planned, stats.count, KitsugiColors.TextMuted)
                        StatItemRow("Durduruldu", stats.paused, stats.count, KitsugiColors.AccentOrange)
                        StatItemRow("Bırakıldı", stats.dropped, stats.count, KitsugiColors.AccentPink)

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatCard("Toplam Anime", stats.count.toString())
                            StatCard("Ortalama Skor", "%.1f".format(stats.meanScore))
                            StatCard("Süre", "${stats.minutesWatched / 60 / 24} Gün")
                        }
                    }
                }
            }

            state.mangaStats?.let { stats ->
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(KitsugiColors.Surface)
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Manga İstatistikleri",
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SegmentedDistributionBar(
                            watching = stats.watching,
                            completed = stats.completed,
                            planned = stats.planned,
                            paused = stats.paused,
                            dropped = stats.dropped,
                            total = stats.count,
                            accentColor = KitsugiColors.AccentBlue
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        StatItemRow("Okuyor", stats.watching, stats.count, KitsugiColors.AccentBlue)
                        StatItemRow("Tamamlandı", stats.completed, stats.count, KitsugiColors.AccentGreen)
                        StatItemRow("Planlanıyor", stats.planned, stats.count, KitsugiColors.TextMuted)
                        StatItemRow("Durduruldu", stats.paused, stats.count, KitsugiColors.AccentOrange)
                        StatItemRow("Bırakıldı", stats.dropped, stats.count, KitsugiColors.AccentPink)

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = KitsugiColors.SurfaceStrong)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatCard("Toplam Manga", stats.count.toString())
                            StatCard("Ortalama Skor", "%.1f".format(stats.meanScore))
                            StatCard("Bölümler", stats.episodesWatched.toString())
                        }
                    }
                }
            }

            item {
                val localFavs = remember(mediaEntries) {
                    mediaEntries.filter { it.isFavorite }.map { entry ->
                        ProfileFavoriteItem(
                            id = entry.id.toString(),
                            title = entry.title,
                            imageUrl = entry.imageUrl ?: ""
                        ) to entry
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (localFavs.isNotEmpty()) {
                        val localFavItems = localFavs.map { it.first }
                        FavoritesHorizontalSection(
                            title = "Yerel Favoriler",
                            items = localFavItems,
                            onSeeAllClick = {
                                onOpenFavoriteSheet("Yerel Favoriler", localFavItems) { item ->
                                    localFavs.find { it.first.id == item.id }?.second?.let { onEntryClick(it) }
                                }
                            },
                            onItemClick = { item ->
                                localFavs.find { it.first.id == item.id }?.second?.let { onEntryClick(it) }
                            }
                        )
                    }

                    FavoritesHorizontalSection(
                        title = "Favori Animeler",
                        items = state.favoriteAnime,
                        onSeeAllClick = {
                            onOpenFavoriteSheet("Favori Animeler", state.favoriteAnime) { item ->
                                item.id.toIntOrNull()?.let { id ->
                                    onFavoriteMediaClick(id, MediaType.Anime, "jikan")
                                }
                            }
                        },
                        onItemClick = { item ->
                            item.id.toIntOrNull()?.let { id ->
                                onFavoriteMediaClick(id, MediaType.Anime, "jikan")
                            }
                        }
                    )
                    FavoritesHorizontalSection(
                        title = "Favori Mangalar",
                        items = state.favoriteManga,
                        onSeeAllClick = {
                            onOpenFavoriteSheet("Favori Mangalar", state.favoriteManga) { item ->
                                item.id.toIntOrNull()?.let { id ->
                                    onFavoriteMediaClick(id, MediaType.Manga, "jikan")
                                }
                            }
                        },
                        onItemClick = { item ->
                            item.id.toIntOrNull()?.let { id ->
                                onFavoriteMediaClick(id, MediaType.Manga, "jikan")
                            }
                        }
                    )
                    FavoritesHorizontalSection(
                        title = "Favori Karakterler",
                        items = state.favoriteCharacters,
                        onSeeAllClick = {
                            onOpenFavoriteSheet("Favori Karakterler", state.favoriteCharacters) { item ->
                                item.id.toIntOrNull()?.let { id ->
                                    onFavoriteCharacterClick(id, "jikan", item.title, item.imageUrl)
                                }
                            }
                        },
                        onItemClick = { item ->
                            item.id.toIntOrNull()?.let { id ->
                                onFavoriteCharacterClick(id, "jikan", item.title, item.imageUrl)
                            }
                        }
                    )
                    FavoritesHorizontalSection(
                        title = "Favori Personeller",
                        items = state.favoriteStaff,
                        onSeeAllClick = {
                            onOpenFavoriteSheet("Favori Personeller", state.favoriteStaff) { item ->
                                item.id.toIntOrNull()?.let { id ->
                                    onFavoriteStaffClick(id, "jikan", item.title, item.imageUrl)
                                }
                            }
                        },
                        onItemClick = { item ->
                            item.id.toIntOrNull()?.let { id ->
                                onFavoriteStaffClick(id, "jikan", item.title, item.imageUrl)
                            }
                        }
                    )
                }
            }
        } else {
            // AKTİVİTE
            item {
                val malEntries = remember(mediaEntries) {
                    mediaEntries.filter { it.source == "myanimelist" || it.malId != null }
                }
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
                        Text(
                            text = "Son Güncellenen Kitaplık İçerikleri",
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
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

        item {
            Spacer(modifier = Modifier.height(80.dp))
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
    // Memoize stats calculations so scrolling is 100% smooth without re-computing on every frame
    val hasApiStats = remember(state.totalAnime, state.totalShows, state.totalMovies) {
        state.totalAnime > 0 || state.totalShows > 0 || state.totalMovies > 0
    }

    val simklEntries = remember(hasApiStats, mediaEntries) {
        if (!hasApiStats) mediaEntries.filter { it.source == "simkl" || it.simklId != null } else emptyList()
    }

    val totalCount = remember(hasApiStats, state.totalAnime, state.totalShows, state.totalMovies, simklEntries) {
        if (hasApiStats) state.totalAnime + state.totalShows + state.totalMovies else simklEntries.size
    }
    val animeCount = remember(hasApiStats, state.totalAnime, simklEntries) {
        if (hasApiStats) state.totalAnime else simklEntries.count { it.type == MediaType.Anime }
    }
    val tvCount = remember(hasApiStats, state.totalShows, simklEntries) {
        if (hasApiStats) state.totalShows else simklEntries.count { it.type == MediaType.TvShow }
    }
    val movieCount = remember(hasApiStats, state.totalMovies, simklEntries) {
        if (hasApiStats) state.totalMovies else simklEntries.count { it.type == MediaType.Movie }
    }

    val watching = remember(hasApiStats, state.watching, simklEntries) {
        if (hasApiStats) state.watching else simklEntries.count { it.status == WatchStatus.Watching || it.status == WatchStatus.Repeating }
    }
    val completed = remember(hasApiStats, state.completed, simklEntries) {
        if (hasApiStats) state.completed else simklEntries.count { it.status == WatchStatus.Completed }
    }
    val planned = remember(hasApiStats, state.planned, simklEntries) {
        if (hasApiStats) state.planned else simklEntries.count { it.status == WatchStatus.Planned }
    }
    val paused = remember(hasApiStats, state.paused, simklEntries) {
        if (hasApiStats) state.paused else simklEntries.count { it.status == WatchStatus.Paused }
    }
    val dropped = remember(hasApiStats, state.dropped, simklEntries) {
        if (hasApiStats) state.dropped else simklEntries.count { it.status == WatchStatus.Dropped }
    }

    val avgScore = remember(hasApiStats, state.avgScore, simklEntries) {
        if (hasApiStats) state.avgScore else {
            val scored = simklEntries.mapNotNull { it.score }
            if (scored.isNotEmpty()) scored.average() else 0.0
        }
    }

    var activeTab by remember { mutableStateOf(0) } // 0: Profil, 1: Aktivite
    val tabs = listOf("Profil", "Aktivite")

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.name,
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (!state.accountType.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(accentColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = state.accountType.uppercase(),
                                        color = accentColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        val details = remember(state.location, state.joinedAt) {
                            buildList {
                                if (!state.location.isNullOrBlank()) add(state.location)
                                if (!state.joinedAt.isNullOrBlank()) {
                                    val dateStr = state.joinedAt.substringBefore(" ")
                                    add("Katılım: $dateStr")
                                }
                            }.joinToString(" • ")
                        }

                        Text(
                            text = if (details.isNotBlank()) details else "Simkl Üyesi",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // Sub-tabs row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(KitsugiColors.Surface)
            ) {
                tabs.forEachIndexed { index, label ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = index }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) accentColor else KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (activeTab == 0) {
            // PROFIL
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Connection Status Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = KitsugiColors.Surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = KitsugiColors.AccentGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Simkl Bağlantısı Aktif",
                                    color = KitsugiColors.TextPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (!state.bio.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = state.bio,
                                    color = KitsugiColors.TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Statistics Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(KitsugiColors.Surface)
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kütüphane İstatistikleri",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (onOpenStatsClick != null) {
                                Text(
                                    text = "Detaylar →",
                                    color = accentColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onOpenStatsClick() }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Cards Grid
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatCard("Toplam", totalCount.toString())
                            StatCard("Ort. Skor", if (avgScore > 0) "%.2f".format(avgScore) else "-")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatCard("Animeler", animeCount.toString())
                            StatCard("Diziler", tvCount.toString())
                            StatCard("Filmler", movieCount.toString())
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "İzleme Durumu Dağılımı",
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SegmentedDistributionBar(
                            watching = watching,
                            completed = completed,
                            planned = planned,
                            paused = paused,
                            dropped = dropped,
                            total = totalCount,
                            accentColor = accentColor
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        StatItemRow("İzleniyor", watching, totalCount, accentColor)
                        StatItemRow("Tamamlandı", completed, totalCount, KitsugiColors.AccentGreen)
                        StatItemRow("Planlandı", planned, totalCount, KitsugiColors.TextMuted)
                        StatItemRow("Durduruldu", paused, totalCount, KitsugiColors.AccentOrange)
                        StatItemRow("Bırakıldı", dropped, totalCount, KitsugiColors.AccentPink)
                    }
                }
            }

            // Yerel Favoriler
            item {
                val localFavs = remember(mediaEntries) {
                    mediaEntries.filter { it.isFavorite }.map { entry ->
                        ProfileFavoriteItem(
                            id = entry.id.toString(),
                            title = entry.title,
                            imageUrl = entry.imageUrl ?: ""
                        ) to entry
                    }
                }

                if (localFavs.isNotEmpty()) {
                    val localFavItems = localFavs.map { it.first }
                    FavoritesHorizontalSection(
                        title = "Yerel Favoriler",
                        items = localFavItems,
                        onSeeAllClick = {
                            onOpenFavoriteSheet("Yerel Favoriler", localFavItems) { item ->
                                localFavs.find { it.first.id == item.id }?.second?.let { onEntryClick(it) }
                            }
                        },
                        onItemClick = { item ->
                            localFavs.find { it.first.id == item.id }?.second?.let { onEntryClick(it) }
                        }
                    )
                }
            }

            // Son İzlenenler (Simkl history)
            if (state.recentHistory.isNotEmpty()) {
                item {
                    FavoritesHorizontalSection(
                        title = "Son İzlenenler",
                        items = state.recentHistory,
                        onSeeAllClick = {
                            onOpenFavoriteSheet("Son İzlenenler", state.recentHistory) { item ->
                                item.id.toIntOrNull()?.let { id ->
                                    onFavoriteMediaClick(id, MediaType.Anime, "simkl")
                                }
                            }
                        },
                        onItemClick = { item ->
                            item.id.toIntOrNull()?.let { id ->
                                onFavoriteMediaClick(id, MediaType.Anime, "simkl")
                            }
                        }
                    )
                }
            }
        } else {
            // AKTİVİTE
            item {
                if (state.recentHistory.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Son İzleme Geçmişi",
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
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
                    val simklEntries = remember(mediaEntries) {
                        mediaEntries.filter { it.source == "simkl" || it.simklId != null }
                    }
                    if (simklEntries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Henüz Simkl aktivitesi bulunmuyor.", color = KitsugiColors.TextMuted)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Son Güncellenen Kitaplık İçerikleri",
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            simklEntries.take(15).forEach { entry ->
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
    onMediaClick: ((mediaId: Int, type: MediaType) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Icon(
                    imageVector = if (activity.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = if (activity.isLiked) KitsugiColors.AccentPink else KitsugiColors.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = activity.likeCount.toString(),
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
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
