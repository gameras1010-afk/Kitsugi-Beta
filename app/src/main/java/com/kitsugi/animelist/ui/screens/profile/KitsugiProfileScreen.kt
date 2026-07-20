package com.kitsugi.animelist.ui.screens.profile

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import coil3.compose.AsyncImage
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.components.KitsugiMediaEntryCard
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.ui.app.KitsugiProfileViewModel
import com.kitsugi.animelist.ui.app.AniListProfileState
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
    modifier: Modifier = Modifier
) {
    val accentColor = LocalKitsugiAccent.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var activeSubTab by remember { mutableStateOf(0) } // 0: Local, 1: AniList, 2: MAL, 3: Simkl
    val subTabs = listOf("Yerel", "AniList", "MyAnimeList", "Simkl")

    val aniListState by viewModel.aniListState.collectAsState()
    val malState by viewModel.malState.collectAsState()
    val simklState by viewModel.simklState.collectAsState()

    // Initialize local statistics
    LaunchedEffect(mediaEntries) {
        viewModel.initLocalStats(mediaEntries)
    }

    // Trigger API fetches on tab switch if connected
    LaunchedEffect(activeSubTab, isAniListConnected, isMalConnected, isSimklConnected) {
        when (activeSubTab) {
            1 -> if (isAniListConnected && aniListState.userId == null) viewModel.fetchAniListProfile()
            2 -> if (isMalConnected && malState.name.isBlank()) viewModel.fetchMalProfile()
            3 -> if (isSimklConnected && simklState.name.isBlank()) viewModel.fetchSimklProfile()
        }
    }

    Scaffold(
        containerColor = KitsugiColors.Background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Profile Sub-tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isLandscape) 18.dp else 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(KitsugiColors.Surface),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                subTabs.forEachIndexed { index, label ->
                    val isSelected = activeSubTab == index
                    val isServiceConnected = when (index) {
                        1 -> isAniListConnected
                        2 -> isMalConnected
                        3 -> isSimklConnected
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
                            if (index > 0 && isServiceConnected) {
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

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeSubTab) {
                    0 -> LocalProfileContent(
                        profileName = profileName,
                        listTitle = listTitle,
                        profileImageUri = profileImageUri,
                        bannerImageUri = bannerImageUri,
                        localStats = viewModel.localStats,
                        mediaEntries = mediaEntries,
                        appSettings = appSettings,
                        onEntryClick = onEntryClick,
                        onOpenSettingsClick = onOpenSettingsClick,
                        isLandscape = isLandscape,
                        accentColor = accentColor
                    )
                    1 -> ExternalProfileWrapper(
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
                            isLandscape = isLandscape,
                            accentColor = accentColor
                        )
                    }
                    2 -> ExternalProfileWrapper(
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
                            isLandscape = isLandscape,
                            accentColor = accentColor
                        )
                    }
                    3 -> ExternalProfileWrapper(
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
                            isLandscape = isLandscape,
                            accentColor = accentColor
                        )
                    }
                }
            }
        }
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
fun LocalProfileContent(
    profileName: String,
    listTitle: String,
    profileImageUri: String,
    bannerImageUri: String,
    localStats: com.kitsugi.animelist.ui.app.LocalStats,
    mediaEntries: List<MediaEntry>,
    appSettings: AppSettings,
    onEntryClick: (MediaEntry) -> Unit,
    onOpenSettingsClick: () -> Unit,
    isLandscape: Boolean,
    accentColor: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isLandscape) 18.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            KitsugiProfileHeaderCard(
                profileName = profileName,
                listTitle = listTitle,
                anilistUsername = "",
                profileImageUri = profileImageUri,
                bannerImageUri = bannerImageUri,
                totalCount = localStats.totalAnime + localStats.totalManga,
                favoriteCount = mediaEntries.count { it.isFavorite && it.source == "manual" },
                averageScoreText = "%.1f".format(
                    if (localStats.avgAnimeScore > 0 && localStats.avgMangaScore > 0) {
                        (localStats.avgAnimeScore + localStats.avgMangaScore) / 2.0
                    } else if (localStats.avgAnimeScore > 0) {
                        localStats.avgAnimeScore
                    } else {
                        localStats.avgMangaScore
                    }
                ),
                platformName = "Kitsugi",
                onSettingsClick = onOpenSettingsClick
            )
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
                    text = "Anime İstatistikleri",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                StatItemRow(
                    label = "İzleniyor",
                    count = localStats.watchingAnime,
                    total = localStats.totalAnime,
                    color = KitsugiColors.AccentBlue
                )
                StatItemRow(
                    label = "Tamamlandı",
                    count = localStats.completedAnime,
                    total = localStats.totalAnime,
                    color = KitsugiColors.AccentGreen
                )
                StatItemRow(
                    label = "Planlanıyor",
                    count = localStats.plannedAnime,
                    total = localStats.totalAnime,
                    color = KitsugiColors.TextMuted
                )
                StatItemRow(
                    label = "Durduruldu",
                    count = localStats.pausedAnime,
                    total = localStats.totalAnime,
                    color = KitsugiColors.AccentOrange
                )
                StatItemRow(
                    label = "Bırakıldı",
                    count = localStats.droppedAnime,
                    total = localStats.totalAnime,
                    color = KitsugiColors.AccentPink
                )
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
                    text = "Manga İstatistikleri",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                StatItemRow(
                    label = "Okuyor",
                    count = localStats.readingManga,
                    total = localStats.totalManga,
                    color = KitsugiColors.AccentBlue
                )
                StatItemRow(
                    label = "Tamamlandı",
                    count = localStats.completedManga,
                    total = localStats.totalManga,
                    color = KitsugiColors.AccentGreen
                )
                StatItemRow(
                    label = "Planlanıyor",
                    count = localStats.plannedManga,
                    total = localStats.totalManga,
                    color = KitsugiColors.TextMuted
                )
                StatItemRow(
                    label = "Durduruldu",
                    count = localStats.pausedManga,
                    total = localStats.totalManga,
                    color = KitsugiColors.AccentOrange
                )
                StatItemRow(
                    label = "Bırakıldı",
                    count = localStats.droppedManga,
                    total = localStats.totalManga,
                    color = KitsugiColors.AccentPink
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
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
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        FavoritesHorizontalSection("Favori Animeler", state.favoriteAnime) { item ->
                            item.id.toIntOrNull()?.let { id ->
                                onFavoriteMediaClick(id, MediaType.Anime, "anilist")
                            }
                        }
                        FavoritesHorizontalSection("Favori Mangalar", state.favoriteManga) { item ->
                            item.id.toIntOrNull()?.let { id ->
                                onFavoriteMediaClick(id, MediaType.Manga, "anilist")
                            }
                        }
                        FavoritesHorizontalSection("Favori Karakterler", state.favoriteCharacters) { item ->
                            item.id.toIntOrNull()?.let { id ->
                                onFavoriteCharacterClick(id, "anilist", item.title, item.imageUrl)
                            }
                        }
                        FavoritesHorizontalSection("Favori Ekip (Staff)", state.favoriteStaff) { item ->
                            item.id.toIntOrNull()?.let { id ->
                                onFavoriteStaffClick(id, "anilist", item.title, item.imageUrl)
                            }
                        }
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
                        ActivityCard(activity = activity, accentColor = accentColor)
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
fun MalProfileContent(
    state: MalProfileState,
    mediaEntries: List<MediaEntry>,
    appSettings: AppSettings,
    onEntryClick: (MediaEntry) -> Unit,
    onFavoriteMediaClick: (mediaId: Int, mediaType: MediaType, source: String) -> Unit,
    onFavoriteCharacterClick: (charId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onFavoriteStaffClick: (staffId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    isLandscape: Boolean,
    accentColor: Color
) {
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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FavoritesHorizontalSection("Favori Animeler", state.favoriteAnime) { item ->
                    item.id.toIntOrNull()?.let { id ->
                        onFavoriteMediaClick(id, MediaType.Anime, "jikan")
                    }
                }
                FavoritesHorizontalSection("Favori Mangalar", state.favoriteManga) { item ->
                    item.id.toIntOrNull()?.let { id ->
                        onFavoriteMediaClick(id, MediaType.Manga, "jikan")
                    }
                }
                FavoritesHorizontalSection("Favori Karakterler", state.favoriteCharacters) { item ->
                    item.id.toIntOrNull()?.let { id ->
                        onFavoriteCharacterClick(id, "jikan", item.title, item.imageUrl)
                    }
                }
                FavoritesHorizontalSection("Favori Personeller", state.favoriteStaff) { item ->
                    item.id.toIntOrNull()?.let { id ->
                        onFavoriteStaffClick(id, "jikan", item.title, item.imageUrl)
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
    isLandscape: Boolean,
    accentColor: Color
) {
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
                        val details = buildList {
                            if (!state.location.isNullOrBlank()) add(state.location)
                            if (!state.joinedAt.isNullOrBlank()) {
                                val dateStr = state.joinedAt.substringBefore(" ")
                                add("Katılım: $dateStr")
                            }
                        }.joinToString(" • ")

                        Text(
                            text = if (details.isNotBlank()) details else "Simkl Üyesi",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // PROFIL
        item {
            // Önce ViewModel'den API istatistikleri kullan (Simkl profil fetch'i sırasında hesaplanmış)
            // Yoksa yerel listeden hesapla (fallback)
            val hasApiStats = state.totalAnime > 0 || state.totalShows > 0 || state.totalMovies > 0

            val simklEntries = if (!hasApiStats) remember(mediaEntries) { mediaEntries.filter { it.source == "simkl" || it.simklId != null } } else emptyList()

            val totalCount = if (hasApiStats) state.totalAnime + state.totalShows + state.totalMovies else simklEntries.size
            val animeCount = if (hasApiStats) state.totalAnime else simklEntries.count { it.type == MediaType.Anime }
            val tvCount = if (hasApiStats) state.totalShows else simklEntries.count { it.type == MediaType.TvShow }
            val movieCount = if (hasApiStats) state.totalMovies else simklEntries.count { it.type == MediaType.Movie }

            val watching = if (hasApiStats) state.watching else simklEntries.count { it.status == WatchStatus.Watching || it.status == WatchStatus.Repeating }
            val completed = if (hasApiStats) state.completed else simklEntries.count { it.status == WatchStatus.Completed }
            val planned = if (hasApiStats) state.planned else simklEntries.count { it.status == WatchStatus.Planned }
            val paused = if (hasApiStats) state.paused else simklEntries.count { it.status == WatchStatus.Paused }
            val dropped = if (hasApiStats) state.dropped else simklEntries.count { it.status == WatchStatus.Dropped }

            val avgScore = if (hasApiStats) state.avgScore else {
                val scored = simklEntries.mapNotNull { it.score }
                if (scored.isNotEmpty()) scored.average() else 0.0
            }

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
                    Text(
                        text = "Kütüphane İstatistikleri",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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

                    StatItemRow("İzleniyor", watching, totalCount, accentColor)
                    StatItemRow("Tamamlandı", completed, totalCount, KitsugiColors.AccentGreen)
                    StatItemRow("Planlandı", planned, totalCount, KitsugiColors.TextMuted)
                    StatItemRow("Durduruldu", paused, totalCount, KitsugiColors.AccentOrange)
                    StatItemRow("Bırakıldı", dropped, totalCount, KitsugiColors.AccentPink)
                }
            }
        }

        // Son İzlenenler (Simkl history)
        if (state.recentHistory.isNotEmpty()) {
            item {
                FavoritesHorizontalSection("Son İzlenenler", state.recentHistory) {
                    // Simkl öğelerine tıklama; simklId ile arama yapılabilir ama şimdilik no-op
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
        Text(
            text = title,
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
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

@Composable
fun ActivityCard(
    activity: ProfileActivityItem,
    accentColor: Color
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(KitsugiColors.Background)
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
