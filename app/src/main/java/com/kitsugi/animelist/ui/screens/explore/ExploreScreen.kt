@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kitsugi.animelist.ui.screens.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import com.kitsugi.animelist.model.MediaType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.matches
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.ui.components.KitsugiHeroSection
import com.kitsugi.animelist.ui.components.KitsugiHorizontalMediaSection
import com.kitsugi.animelist.ui.components.KitsugiErrorState
import com.kitsugi.animelist.ui.components.KitsugiEmptyState
import com.kitsugi.animelist.ui.components.KitsugiShimmerHeroSection
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.delay
import com.kitsugi.animelist.ui.components.KitsugiShimmerProvider
import coil3.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.kitsugi.animelist.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun ExploreScreen(
    currentEntries: List<MediaEntry>,
    showAdultContent: Boolean,
    onAddSelectionToList: (ApiSearchSelection) -> Unit,
    onSeeAllSection: (title: String, categoryType: ExploreCategoryType, results: List<JikanSearchResult>) -> Unit,
    onOpenApiDetail: (JikanSearchResult) -> Unit,
    onEditEntry: (MediaEntry) -> Unit,
    onOpenMangaReader: () -> Unit = {},
    onOpenAiringCalendar: () -> Unit = {},
    initialScrollIndex: Int = 0,
    initialScrollOffset: Int = 0,
    onScrollPositionChange: (index: Int, offset: Int) -> Unit = { _, _ -> },
    viewModel: ExploreViewModel = viewModel(),
    titleLanguage: String = "ROMAJI",
    scoreFormat: String = "POINT_10",
    hideScores: Boolean = false,
    showAnimeLogos: Boolean = false,
    isSimklConnected: Boolean = false,
    blurAdultMedia: Boolean = false,
    onOpenNotifications: () -> Unit = {},
    isNotificationsVisible: Boolean = false
) {
    val accentColor = LocalKitsugiAccent.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val filteredTopAnime = remember(viewModel.topAnime, showAdultContent) { viewModel.topAnime.filter { showAdultContent || !it.isAdult } }
    val filteredAiringAnime = remember(viewModel.airingAnime, showAdultContent) { viewModel.airingAnime.filter { showAdultContent || !it.isAdult } }
    val filteredUpcomingAnime = remember(viewModel.upcomingAnime, showAdultContent) { viewModel.upcomingAnime.filter { showAdultContent || !it.isAdult } }
    val filteredTopManga = remember(viewModel.topManga, showAdultContent) { viewModel.topManga.filter { showAdultContent || !it.isAdult } }
    val filteredPublishingManga = remember(viewModel.publishingManga, showAdultContent) { viewModel.publishingManga.filter { showAdultContent || !it.isAdult } }
    val filteredTrendingAnime = remember(viewModel.trendingAnime, showAdultContent) { viewModel.trendingAnime.filter { showAdultContent || !it.isAdult } }
    val filteredMovieAnime = remember(viewModel.movieAnime, showAdultContent) { viewModel.movieAnime.filter { showAdultContent || !it.isAdult } }
    val filteredSeasonalAnime = remember(viewModel.seasonalAnime, showAdultContent) { viewModel.seasonalAnime.filter { showAdultContent || !it.isAdult } }
    val filteredTrendingManga = remember(viewModel.trendingManga, showAdultContent) { viewModel.trendingManga.filter { showAdultContent || !it.isAdult } }
    val filteredNewlyAddedAnime = remember(viewModel.newlyAddedAnime, showAdultContent) { viewModel.newlyAddedAnime.filter { showAdultContent || !it.isAdult } }
    val filteredNewlyAddedManga = remember(viewModel.newlyAddedManga, showAdultContent) { viewModel.newlyAddedManga.filter { showAdultContent || !it.isAdult } }
    val filteredUpcomingMediaTmdb = remember(viewModel.upcomingMediaTmdb, showAdultContent) { viewModel.upcomingMediaTmdb.filter { showAdultContent || !it.isAdult } }


    val heroItems = remember(viewModel.selectedPlatform, filteredTopAnime, filteredAiringAnime, filteredMovieAnime) {
        if (viewModel.selectedPlatform == ExplorePlatform.TMDB) {
            val itemsMix = mutableListOf<JikanSearchResult>()
            val topIt = filteredTopAnime.iterator()
            val airIt = filteredAiringAnime.iterator()
            val movIt = filteredMovieAnime.iterator()

            val addedKeys = mutableSetOf<String>()
            fun addIfUnique(item: JikanSearchResult) {
                val key = "${item.source}_${item.malId}"
                if (key !in addedKeys) {
                    addedKeys.add(key)
                    itemsMix.add(item)
                }
            }

            while (itemsMix.size < 5 && (topIt.hasNext() || airIt.hasNext() || movIt.hasNext())) {
                if (topIt.hasNext() && itemsMix.size < 5) addIfUnique(topIt.next())
                if (airIt.hasNext() && itemsMix.size < 5) addIfUnique(airIt.next())
                if (movIt.hasNext() && itemsMix.size < 5) addIfUnique(movIt.next())
            }
            itemsMix
        } else {
            filteredTopAnime.take(5)
        }
    }

    val entryMap = remember(currentEntries) {
        val mapping = mutableMapOf<String, MediaEntry>()
        currentEntries.forEach { entry ->
            mapping["${entry.source.lowercase()}_${entry.malId}"] = entry
            if (entry.tmdbId != null) {
                mapping["tmdb_${entry.tmdbId}"] = entry
            }
            if (entry.simklId != null) {
                mapping["simkl_${entry.simklId}"] = entry
            }
            if (entry.source.equals("anilist", ignoreCase = true) && entry.malId != null && entry.malId >= 100_000_000) {
                mapping["anilist_${entry.malId - 100_000_000}"] = entry
            }
            if (entry.source.equals("jikan", ignoreCase = true) || entry.source.equals("mal", ignoreCase = true)) {
                mapping["mal_${entry.malId}"] = entry
                mapping["jikan_${entry.malId}"] = entry
            }
            val normTitle = entry.title.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }.trim()
            if (normTitle.isNotEmpty()) {
                mapping["${entry.type.name.lowercase()}_$normTitle"] = entry
            }
        }
        mapping
    }

    val getMediaEntry = remember(entryMap) {
        { result: JikanSearchResult ->
            val directKey = "${result.source.lowercase()}_${result.malId}"
            var found = entryMap[directKey]

            if (found == null) {
                val tmdbId = result.tmdbId ?: if (result.source.equals("tmdb", ignoreCase = true)) result.malId else null
                if (tmdbId != null) {
                    found = entryMap["tmdb_$tmdbId"]
                }
            }

            if (found == null) {
                val rMal = if (result.source.equals("jikan", ignoreCase = true) || result.source.equals("mal", ignoreCase = true)) {
                    result.malId
                } else {
                    result.realMalId
                }
                if (rMal != null) {
                    found = entryMap["${result.source.lowercase()}_$rMal"]
                        ?: entryMap["mal_$rMal"]
                        ?: entryMap["jikan_$rMal"]
                        ?: entryMap["anilist_$rMal"]
                        ?: entryMap["simkl_$rMal"]
                }
            }

            if (found == null) {
                val normTitle = buildString {
                    for (c in result.title.lowercase()) {
                        if (c in 'a'..'z' || c in '0'..'9') append(c)
                    }
                }.trim()
                if (normTitle.isNotEmpty()) {
                    found = entryMap["${result.type.name.lowercase()}_$normTitle"]
                }
            }

            found
        }
    }

    val isAlreadyInList = remember(getMediaEntry) {
        { result: JikanSearchResult ->
            getMediaEntry(result) != null
        }
    }

    fun handleAdd(result: JikanSearchResult, synopsis: String?) {
        if (!isAlreadyInList(result)) {
            onAddSelectionToList(ApiSearchSelection(result = result, synopsis = synopsis))
        }
    }

    val onLongClickItem = remember(getMediaEntry) {
        { result: JikanSearchResult ->
            val entry = getMediaEntry(result)
            if (entry != null) {
                onEditEntry(entry)
            } else {
                onAddSelectionToList(ApiSearchSelection(result = result, synopsis = null))
            }
        }
    }

    // Kategori kartları için gradient renk tanımları
    val animeGradients = listOf(
        listOf(KitsugiColors.AccentPurple.copy(0.85f), KitsugiColors.AccentBlue.copy(0.70f)),   // Top 100
        listOf(KitsugiColors.AccentPink.copy(0.85f), KitsugiColors.AccentPurple.copy(0.70f)),   // Trend
        listOf(KitsugiColors.AccentGreen.copy(0.80f), KitsugiColors.AccentBlue.copy(0.65f)),    // Yayında
        listOf(KitsugiColors.AccentOrange.copy(0.85f), KitsugiColors.AccentRed.copy(0.70f)),    // Yaklaşan
        listOf(KitsugiColors.AccentBlue.copy(0.85f), KitsugiColors.AccentPurple.copy(0.65f)),   // Filmler
        listOf(KitsugiColors.AccentRed.copy(0.80f), KitsugiColors.AccentPink.copy(0.65f)),      // Mevsimlik
    )

    val mangaGradients = listOf(
        listOf(KitsugiColors.AccentOrange.copy(0.85f), KitsugiColors.AccentPink.copy(0.70f)),   // Top Manga
        listOf(KitsugiColors.AccentGreen.copy(0.85f), KitsugiColors.AccentBlue.copy(0.70f)),    // Yayında
    )

    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialScrollIndex,
        initialFirstVisibleItemScrollOffset = initialScrollOffset
    )

    var activeRankingSheetData by remember { mutableStateOf<Triple<String, MediaType, List<JikanSearchResult>>?>(null) }
    var isCategoriesExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }

    val isHeroGone by remember {
        androidx.compose.runtime.derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 300
        }
    }

    LaunchedEffect(lazyListState) {
        androidx.compose.runtime.snapshotFlow {
            lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            onScrollPositionChange(index, offset)
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current

    val isCatalogEmpty = !viewModel.isLoading && viewModel.errorMessage == null &&
        filteredTopAnime.isEmpty() && filteredAiringAnime.isEmpty() &&
        filteredUpcomingAnime.isEmpty() && filteredTopManga.isEmpty() &&
        filteredPublishingManga.isEmpty() && filteredTrendingAnime.isEmpty() &&
        filteredMovieAnime.isEmpty() && filteredSeasonalAnime.isEmpty() &&
        viewModel.simklContinueSeries.isEmpty() && viewModel.simklContinueMovies.isEmpty() &&
        viewModel.simklPlannedSeries.isEmpty() && viewModel.simklPlannedMovies.isEmpty() &&
        filteredNewlyAddedAnime.isEmpty() && filteredNewlyAddedManga.isEmpty() &&
        viewModel.airingSoonAnime.isEmpty() && filteredUpcomingMediaTmdb.isEmpty()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
            val pullRefreshState = rememberPullToRefreshState()
            CompositionLocalProvider(
                LocalBringIntoViewSpec provides if (isTvDevice) tvSpec else LocalBringIntoViewSpec.current
            ) {
                PullToRefreshBox(
                    isRefreshing = viewModel.isLoading,
                    onRefresh = { viewModel.loadData(forceRefresh = true) },
                    modifier = Modifier.fillMaxSize(),
                    state = pullRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullRefreshState,
                            isRefreshing = viewModel.isLoading,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = KitsugiColors.Surface,
                            color = accentColor
                        )
                    }
                ) {
                    KitsugiShimmerProvider {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                        if (heroItems.isNotEmpty()) {
                            item {
                                KitsugiHeroSection(
                                    items = heroItems,
                                    alreadyInList = isAlreadyInList,
                                    onInfoClick = onOpenApiDetail,
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    showAnimeLogos = showAnimeLogos,
                                    blurAdultMedia = blurAdultMedia,
                                    isVisible = lazyListState.firstVisibleItemIndex == 0
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }
                        } else if (viewModel.isLoading) {
                            item {
                                KitsugiShimmerProvider {
                                    KitsugiShimmerHeroSection()
                                }
                                Spacer(modifier = Modifier.height(26.dp))
                            }
                        } else {
                            item {
                                Spacer(modifier = Modifier.height(28.dp))
                            }
                        }

                        // Header / Title and Toggle as STICKY HEADER
                        stickyHeader(key = "explore_platform_toggle") {
                            androidx.compose.material3.Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = KitsugiColors.Background.copy(alpha = 0.95f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    if (isTvDevice) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Keşfet",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Black,
                                                color = KitsugiColors.TextPrimary
                                            )
                                            ExplorePlatformToggle(
                                                selectedPlatform = viewModel.selectedPlatform,
                                                onPlatformSelected = { platform -> viewModel.selectPlatform(platform) },
                                                modifier = Modifier.width(300.dp)
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            ExplorePlatformToggle(
                                                selectedPlatform = viewModel.selectedPlatform,
                                                onPlatformSelected = { platform -> viewModel.selectPlatform(platform) },
                                                modifier = Modifier.weight(1f)
                                            )

                                            if (isNotificationsVisible) {
                                                // 🔔 Bildirim butonu
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(KitsugiColors.Surface)
                                                        .tvClickable(shape = RoundedCornerShape(12.dp)) {
                                                            onOpenNotifications()
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Notifications,
                                                        contentDescription = "Bildirimler",
                                                        tint = KitsugiColors.TextPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            } else {
                                                // 🎲 Rastgele keşfet butonu
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(KitsugiColors.Surface)
                                                        .tvClickable(shape = RoundedCornerShape(12.dp)) {
                                                            val randomPool = mutableListOf<JikanSearchResult>()
                                                            randomPool.addAll(filteredTopAnime)
                                                            randomPool.addAll(filteredAiringAnime)
                                                            randomPool.addAll(filteredUpcomingAnime)
                                                            randomPool.addAll(filteredTopManga)
                                                            randomPool.addAll(filteredPublishingManga)
                                                            randomPool.addAll(filteredTrendingAnime)
                                                            randomPool.addAll(filteredMovieAnime)
                                                            randomPool.addAll(filteredSeasonalAnime)
                                                            randomPool.addAll(viewModel.simklContinueMovies)
                                                            randomPool.addAll(viewModel.simklPlannedMovies)
                                                            randomPool.addAll(viewModel.simklContinueSeries)
                                                            randomPool.addAll(viewModel.simklPlannedSeries)
                                                            if (randomPool.isNotEmpty()) {
                                                                val randomResult = randomPool.random()
                                                                onOpenApiDetail(randomResult)
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "🎲",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Önbellek Bildirim Banner'ı
                        if (viewModel.isShowingCachedData) {
                            item(key = "cached_data_banner") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(KitsugiColors.Surface.copy(alpha = 0.5f))
                                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "📡",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Column {
                                            Text(
                                                text = "Çevrimdışı / Önbellek Modu",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = KitsugiColors.TextPrimary
                                            )
                                            Text(
                                                text = "İnternet bağlantısı kesildi veya sunucu yanıt vermiyor. Son başarılı önbelleğe alınan veriler gösteriliyor.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = KitsugiColors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Hata mesajı
                        if (viewModel.errorMessage != null) {
                            item(key = "error_message") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    KitsugiErrorState(
                                        message = viewModel.errorMessage.orEmpty(),
                                        onRetryClick = { viewModel.loadData(forceRefresh = true) }
                                    )
                                }
                            }
                        }

                        if (isCatalogEmpty) {
                            item {
                                KitsugiEmptyState(
                                    title = "Gösterilecek İçerik Yok",
                                    subtitle = "Seçilen platformda görüntülenebilecek medya bulunamadı."
                                )
                            }
                        } else if (viewModel.selectedPlatform == ExplorePlatform.TMDB) {
                            // ─── TMDB KATEGORİLER (Collapsible Section) ───
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(KitsugiColors.Surface.copy(alpha = 0.4f))
                                            .clickable { isCategoriesExpanded = !isCategoriesExpanded }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Rounded.GridView,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Kategoriler",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = KitsugiColors.TextPrimary
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isCategoriesExpanded) androidx.compose.material.icons.Icons.Rounded.KeyboardArrowUp else androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "Genişlet/Daralt",
                                            tint = KitsugiColors.TextSecondary
                                        )
                                    }
                                    
                                    AnimatedVisibility(
                                        visible = isCategoriesExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp, start = 4.dp, end = 4.dp)
                                        ) {
                                            // ── Dizi ve Film Alt Kategorisi ──
                                            Text(
                                                text = "Dizi ve Film",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = KitsugiColors.TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Trend Her Şey",
                                                        onClick = { onSeeAllSection("Trend Her Şey", ExploreCategoryType.TOP_ANIME, filteredTopAnime) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Trend Diziler",
                                                        onClick = { onSeeAllSection("Trend Diziler", ExploreCategoryType.AIRING_ANIME, filteredAiringAnime) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Trend Filmler",
                                                        onClick = { onSeeAllSection("Trend Filmler", ExploreCategoryType.MOVIE_ANIME, filteredMovieAnime) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Popüler Diziler",
                                                        onClick = { onSeeAllSection("Popüler Diziler", ExploreCategoryType.TOP_MANGA, filteredTopManga) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Popüler Filmler",
                                                        onClick = { onSeeAllSection("Popüler Filmler", ExploreCategoryType.UPCOMING_ANIME, filteredUpcomingAnime) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Yakında Yayında",
                                                        onClick = onOpenAiringCalendar
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "En Yüksek Puanlı Diziler",
                                                        onClick = { onSeeAllSection("En Yüksek Puanlı Diziler", ExploreCategoryType.SEASONAL_ANIME, filteredSeasonalAnime) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "En Yüksek Puanlı Filmler",
                                                        onClick = { onSeeAllSection("En Yüksek Puanlı Filmler", ExploreCategoryType.PUBLISHING_MANGA, filteredPublishingManga) }
                                                    )
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(20.dp))
                                            
                                            // ── Anime Alt Kategorisi ──
                                            Text(
                                                text = "Anime",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = KitsugiColors.TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Trend Animeler",
                                                        onClick = { onSeeAllSection("Trend Animeler", ExploreCategoryType.TRENDING_ANIME, filteredTrendingAnime) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Popüler Animeler",
                                                        onClick = { onSeeAllSection("Popüler Animeler", ExploreCategoryType.NEWLY_ADDED_ANIME, filteredNewlyAddedAnime) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = "Yayın Takvimi",
                                                        onClick = onOpenAiringCalendar
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }

                            // ─── TMDB YAKINDA YAYINDA (Upcoming TV/Movies/Anime) ───
                            if (filteredUpcomingMediaTmdb.isNotEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Yakında Yayında",
                                                color = KitsugiColors.TextPrimary,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(onClick = onOpenAiringCalendar) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                                    contentDescription = "Tümünü Gör",
                                                    tint = accentColor
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        val lazyListState = remember { androidx.compose.foundation.lazy.LazyListState() }
                                        LazyRow(
                                            state = lazyListState,
                                            contentPadding = PaddingValues(horizontal = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                                                lazyListState = lazyListState,
                                                snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Start
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(filteredUpcomingMediaTmdb.size) { index ->
                                                val result = filteredUpcomingMediaTmdb[index]
                                                AiringSoonHorizontalCard(
                                                    result = result,
                                                    alreadyInList = isAlreadyInList(result),
                                                    onItemClick = { onOpenApiDetail(result) },
                                                    onLongClick = { onLongClickItem(result) },
                                                    titleLanguage = titleLanguage,
                                                    blurAdultMedia = blurAdultMedia
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }

                            // ─── TMDB TREND MEDYALAR ───
                            if (filteredTrendingAnime.isNotEmpty()) {
                                item {
                                    KitsugiHorizontalMediaSection(
                                        title = "Trend Medyalar",
                                        results = filteredTrendingAnime,
                                        isLoading = viewModel.isLoading,
                                        alreadyInList = isAlreadyInList,
                                        getMediaEntry = getMediaEntry,
                                        onItemClick = onOpenApiDetail,
                                        onLongClickItem = onLongClickItem,
                                        onSeeAllClick = { onSeeAllSection("Trend Medyalar", ExploreCategoryType.TRENDING_ANIME, filteredTrendingAnime) },
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores,
                                        blurAdultMedia = blurAdultMedia
                                    )
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }

                            // ─── TMDB POPÜLER MEDYALAR ───
                            if (filteredNewlyAddedAnime.isNotEmpty()) {
                                item {
                                    KitsugiHorizontalMediaSection(
                                        title = "Popüler Medyalar",
                                        results = filteredNewlyAddedAnime,
                                        isLoading = viewModel.isLoading,
                                        alreadyInList = isAlreadyInList,
                                        getMediaEntry = getMediaEntry,
                                        onItemClick = onOpenApiDetail,
                                        onLongClickItem = onLongClickItem,
                                        onSeeAllClick = { onSeeAllSection("Popüler Medyalar", ExploreCategoryType.NEWLY_ADDED_ANIME, filteredNewlyAddedAnime) },
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores,
                                        blurAdultMedia = blurAdultMedia
                                    )
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }

                            // ─── TMDB YATAY MEDYA LİSTELERİ ───
                            item {

                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_tmdb_trending_all),
                                    results = filteredTopAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_tmdb_trending_all), ExploreCategoryType.TOP_ANIME, filteredTopAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_tmdb_trending_shows),
                                    results = filteredAiringAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_tmdb_trending_shows), ExploreCategoryType.AIRING_ANIME, filteredAiringAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_tmdb_trending_movies),
                                    results = filteredMovieAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_tmdb_trending_movies), ExploreCategoryType.MOVIE_ANIME, filteredMovieAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_tmdb_popular_shows),
                                    results = filteredTopManga,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_tmdb_popular_shows), ExploreCategoryType.AIRING_ANIME, filteredTopManga) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_tmdb_popular_movies),
                                    results = filteredUpcomingAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_tmdb_popular_movies), ExploreCategoryType.UPCOMING_ANIME, filteredUpcomingAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_tmdb_top_rated_movies),
                                    results = filteredPublishingManga,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_tmdb_top_rated_movies), ExploreCategoryType.TOP_MANGA, filteredPublishingManga) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            item {
                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_tmdb_top_rated_shows),
                                    results = filteredSeasonalAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_tmdb_top_rated_shows), ExploreCategoryType.SEASONAL_ANIME, filteredSeasonalAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                            }

                            // ─── SİMKL KULLANICI LİSTELERİ ───
                            if (viewModel.simklContinueSeries.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(26.dp))
                                    KitsugiHorizontalMediaSection(
                                        title = stringResource(R.string.explore_simkl_continue_watching_series),
                                        results = viewModel.simklContinueSeries,
                                        isLoading = viewModel.isLoading,
                                        alreadyInList = isAlreadyInList,
                                        getMediaEntry = getMediaEntry,
                                        onItemClick = onOpenApiDetail,
                                        onLongClickItem = onLongClickItem,
                                        onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_simkl_continue_watching_series), ExploreCategoryType.AIRING_ANIME, viewModel.simklContinueSeries) },
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores,
                                        blurAdultMedia = blurAdultMedia
                                    )
                                }
                            }

                            if (viewModel.simklContinueMovies.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(26.dp))
                                    KitsugiHorizontalMediaSection(
                                        title = stringResource(R.string.explore_simkl_continue_watching_movies),
                                        results = viewModel.simklContinueMovies,
                                        isLoading = viewModel.isLoading,
                                        alreadyInList = isAlreadyInList,
                                        getMediaEntry = getMediaEntry,
                                        onItemClick = onOpenApiDetail,
                                        onLongClickItem = onLongClickItem,
                                        onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_simkl_continue_watching_movies), ExploreCategoryType.MOVIE_ANIME, viewModel.simklContinueMovies) },
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores,
                                        blurAdultMedia = blurAdultMedia
                                    )
                                }
                            }

                            if (viewModel.simklPlannedSeries.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(26.dp))
                                    KitsugiHorizontalMediaSection(
                                        title = stringResource(R.string.explore_simkl_plantowatch_series),
                                        results = viewModel.simklPlannedSeries,
                                        isLoading = viewModel.isLoading,
                                        alreadyInList = isAlreadyInList,
                                        getMediaEntry = getMediaEntry,
                                        onItemClick = onOpenApiDetail,
                                        onLongClickItem = onLongClickItem,
                                        onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_simkl_plantowatch_series), ExploreCategoryType.AIRING_ANIME, viewModel.simklPlannedSeries) },
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores,
                                        blurAdultMedia = blurAdultMedia
                                    )
                                }
                            }

                            if (viewModel.simklPlannedMovies.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(26.dp))
                                    KitsugiHorizontalMediaSection(
                                        title = stringResource(R.string.explore_simkl_plantowatch_movies),
                                        results = viewModel.simklPlannedMovies,
                                        isLoading = viewModel.isLoading,
                                        alreadyInList = isAlreadyInList,
                                        getMediaEntry = getMediaEntry,
                                        onItemClick = onOpenApiDetail,
                                        onLongClickItem = onLongClickItem,
                                        onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_simkl_plantowatch_movies), ExploreCategoryType.MOVIE_ANIME, viewModel.simklPlannedMovies) },
                                        titleLanguage = titleLanguage,
                                        scoreFormat = scoreFormat,
                                        hideScores = hideScores,
                                        blurAdultMedia = blurAdultMedia
                                    )
                                }
                            }
                        } else {
                            // ─── KATEGORİLER (Collapsible Section) ───
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(KitsugiColors.Surface.copy(alpha = 0.4f))
                                            .clickable { isCategoriesExpanded = !isCategoriesExpanded }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Rounded.GridView,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Kategoriler",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = KitsugiColors.TextPrimary
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isCategoriesExpanded) androidx.compose.material.icons.Icons.Rounded.KeyboardArrowUp else androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "Genişlet/Daralt",
                                            tint = KitsugiColors.TextSecondary
                                        )
                                    }
                                    
                                    AnimatedVisibility(
                                        visible = isCategoriesExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp, start = 4.dp, end = 4.dp)
                                        ) {
                                            // ── Anime Alt Kategorisi ──
                                            Text(
                                                text = "Anime",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = KitsugiColors.TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                item {
                                                    ExploreCategoryChip(
                                                        label = stringResource(R.string.explore_seasonal_anime),
                                                        onClick = { onSeeAllSection(context.getString(R.string.explore_seasonal_anime), ExploreCategoryType.SEASONAL_ANIME, filteredSeasonalAnime) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = stringResource(R.string.explore_airing_soon),
                                                        onClick = onOpenAiringCalendar
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = stringResource(R.string.explore_top_anime),
                                                        onClick = { onSeeAllSection(context.getString(R.string.explore_top_anime), ExploreCategoryType.TOP_ANIME, filteredTopAnime) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = stringResource(R.string.explore_trending_anime),
                                                        onClick = { onSeeAllSection(context.getString(R.string.explore_trending_anime), ExploreCategoryType.TRENDING_ANIME, filteredTrendingAnime) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = stringResource(R.string.explore_movie_anime),
                                                        onClick = { onSeeAllSection(context.getString(R.string.explore_movie_anime), ExploreCategoryType.MOVIE_ANIME, filteredMovieAnime) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = stringResource(R.string.explore_newly_added_anime),
                                                        onClick = { onSeeAllSection(context.getString(R.string.explore_newly_added_anime), ExploreCategoryType.NEWLY_ADDED_ANIME, filteredNewlyAddedAnime) }
                                                    )
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(20.dp))
                                            
                                            // ── Manga Alt Kategorisi ──
                                            Text(
                                                text = "Manga",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = KitsugiColors.TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                item {
                                                    ExploreCategoryChip(
                                                        label = stringResource(R.string.explore_top_manga),
                                                        onClick = { onSeeAllSection(context.getString(R.string.explore_top_manga), ExploreCategoryType.TOP_MANGA, filteredTopManga) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = stringResource(R.string.explore_publishing_manga),
                                                        onClick = { onSeeAllSection(context.getString(R.string.explore_publishing_manga), ExploreCategoryType.PUBLISHING_MANGA, filteredPublishingManga) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = stringResource(R.string.explore_trending_manga),
                                                        onClick = { onSeeAllSection(context.getString(R.string.explore_trending_manga), ExploreCategoryType.TRENDING_MANGA, filteredTrendingManga) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = stringResource(R.string.explore_newly_added_manga),
                                                        onClick = { onSeeAllSection(context.getString(R.string.explore_newly_added_manga), ExploreCategoryType.NEWLY_ADDED_MANGA, filteredNewlyAddedManga) }
                                                    )
                                                }
                                                item {
                                                    ExploreCategoryChip(
                                                        label = stringResource(R.string.explore_read_manga),
                                                        onClick = onOpenMangaReader
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }

                            // ─── YAKINDA YAYINDA (Airing Soon) ───
                            if (viewModel.airingSoonAnime.isNotEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = stringResource(R.string.explore_airing_soon),
                                                color = KitsugiColors.TextPrimary,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(onClick = onOpenAiringCalendar) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                                    contentDescription = "Yayın Takvimi",
                                                    tint = accentColor
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        val lazyListState = remember { androidx.compose.foundation.lazy.LazyListState() }
                                        LazyRow(
                                            state = lazyListState,
                                            contentPadding = PaddingValues(horizontal = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                                                lazyListState = lazyListState,
                                                snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Start
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(viewModel.airingSoonAnime.size) { index ->
                                                val result = viewModel.airingSoonAnime[index]
                                                AiringSoonHorizontalCard(
                                                    result = result,
                                                    alreadyInList = isAlreadyInList(result),
                                                    onItemClick = { onOpenApiDetail(result) },
                                                    onLongClick = { onLongClickItem(result) },
                                                    titleLanguage = titleLanguage,
                                                    blurAdultMedia = blurAdultMedia
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }

                            // ─── POPÜLER ANİME ───
                            item {
                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_top_anime),
                                    results = filteredTopAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_top_anime), ExploreCategoryType.TOP_ANIME, filteredTopAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            // ─── YAYINDAKİ ANİME ───
                            item {
                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_airing_anime),
                                    results = filteredAiringAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_airing_anime), ExploreCategoryType.AIRING_ANIME, filteredAiringAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            // ─── YAKLAŞAN ANİME ───
                            item {
                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_upcoming_anime),
                                    results = filteredUpcomingAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_upcoming_anime), ExploreCategoryType.UPCOMING_ANIME, filteredUpcomingAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            // ─── YENİ EKLENEN ANİME ───
                            item {
                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_newly_added_anime),
                                    results = filteredNewlyAddedAnime,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_newly_added_anime), ExploreCategoryType.NEWLY_ADDED_ANIME, filteredNewlyAddedAnime) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            // ─── POPÜLER MANGA ───
                            item {
                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_top_manga),
                                    results = filteredTopManga,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_top_manga), ExploreCategoryType.TOP_MANGA, filteredTopManga) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            // ─── YAYINDAKİ MANGA ───
                            item {
                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_publishing_manga),
                                    results = filteredPublishingManga,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_publishing_manga), ExploreCategoryType.PUBLISHING_MANGA, filteredPublishingManga) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            // ─── TREND MANGA ───
                            item {
                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_trending_manga),
                                    results = filteredTrendingManga,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_trending_manga), ExploreCategoryType.TRENDING_MANGA, filteredTrendingManga) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }

                            // ─── YENİ EKLENEN MANGA ───
                            item {
                                KitsugiHorizontalMediaSection(
                                    title = stringResource(R.string.explore_newly_added_manga),
                                    results = filteredNewlyAddedManga,
                                    isLoading = viewModel.isLoading,
                                    alreadyInList = isAlreadyInList,
                                    getMediaEntry = getMediaEntry,
                                    onItemClick = onOpenApiDetail,
                                    onLongClickItem = onLongClickItem,
                                    onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_newly_added_manga), ExploreCategoryType.NEWLY_ADDED_MANGA, filteredNewlyAddedManga) },
                                    titleLanguage = titleLanguage,
                                    scoreFormat = scoreFormat,
                                    hideScores = hideScores,
                                    blurAdultMedia = blurAdultMedia
                                )
                            }
                        }
                    }
                }
                }
            }
        }


    }

    val rankingSheet = activeRankingSheetData
    if (rankingSheet != null) {
        com.kitsugi.animelist.ui.components.KitsugiRankingBottomSheet(
            title = rankingSheet.first,
            mediaType = rankingSheet.second,
            platform = viewModel.selectedPlatform,
            initialResults = rankingSheet.third,
            alreadyInList = isAlreadyInList,
            onItemClick = onOpenApiDetail,
            onDismissRequest = { activeRankingSheetData = null },
            titleLanguage = titleLanguage,
            hideScores = hideScores,
            showAdultContent = showAdultContent,
            blurAdultMedia = blurAdultMedia,
            getMediaEntry = getMediaEntry
        )
    }
}

@Composable
fun ExploreCategoryChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KitsugiColors.Accent.copy(alpha = 0.08f))
            .border(1.dp, KitsugiColors.Accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = KitsugiColors.Accent,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
fun AiringSoonCountdownText(
    nextAiringEpisode: String?,
    modifier: Modifier = Modifier
) {
    if (nextAiringEpisode.isNullOrBlank()) return

    val parts = remember(nextAiringEpisode) { nextAiringEpisode.split("|") }
    val episode = remember(parts) { parts.getOrNull(0)?.toIntOrNull() } ?: return
    val targetEpoch = remember(parts) { parts.getOrNull(1)?.toLongOrNull() } ?: return

    var countdownText by remember(episode, targetEpoch) { mutableStateOf("") }

    LaunchedEffect(episode, targetEpoch) {
        while (true) {
            val now = System.currentTimeMillis() / 1000L
            val remaining = targetEpoch - now
            countdownText = when {
                remaining <= 0L -> if (episode > 0) "Bölüm $episode yayınlandı" else "Yayınlandı"
                remaining < 3600L -> {
                    val mins = (remaining / 60).toInt()
                    "${mins} dk sonra yayınlanacak"
                }
                remaining < 86400L -> {
                    val hours = (remaining / 3600).toInt()
                    "${hours} saat sonra yayınlanacak"
                }
                else -> {
                    val days = (remaining / 86400).toInt()
                    if (days > 6) {
                        val weeks = days / 7
                        if (weeks > 4) {
                            val months = days / 30
                            "$months ay sonra yayınlanacak"
                        } else {
                            "$weeks hafta sonra yayınlanacak"
                        }
                    } else {
                        "$days gün sonra yayınlanacak"
                    }
                }
            }
            if (remaining <= 0L) break
            val delayMs = if (remaining < 3600L) 1_000L
                          else if (remaining < 86400L) 10_000L
                          else 60_000L
            delay(delayMs)
        }
    }

    if (countdownText.isNotBlank()) {
        Text(
            text = countdownText,
            color = KitsugiColors.AccentOrange,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }
}

@Composable
fun AiringSoonHorizontalCard(
    result: JikanSearchResult,
    alreadyInList: Boolean,
    onItemClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    titleLanguage: String = "ROMAJI",
    blurAdultMedia: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayTitle = when (titleLanguage) {
        "ENGLISH" -> result.titleEnglish ?: result.title
        else      -> result.title
    }

    val displayScore = when {
        result.rawScoreDouble != null -> "★ ${result.rawScoreDouble}"
        result.score != null -> {
            if (result.score > 10) "★ ${result.score}%" else "★ ${result.score}"
        }
        else -> null
    }

    Row(
        modifier = modifier
            .width(280.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(KitsugiColors.Surface)
            .then(
                if (alreadyInList)
                    Modifier.border(1.dp, KitsugiColors.AccentGreen.copy(0.45f), RoundedCornerShape(12.dp))
                else Modifier
            )
            .tvClickable(
                shape = RoundedCornerShape(12.dp),
                onLongClick = onLongClick,
                onClick = onItemClick
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sol Taraf: Poster
        Box(
            modifier = Modifier
                .width(60.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(KitsugiColors.SurfaceSoft)
        ) {
            AsyncImage(
                model = result.imageUrl,
                contentDescription = displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (blurAdultMedia && result.isAdult) Modifier.blur(24.dp)
                        else Modifier
                    )
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Sağ Taraf: Detaylar
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = KitsugiColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            AiringSoonCountdownText(nextAiringEpisode = result.nextAiringEpisode)

            if (displayScore != null) {
                Text(
                    text = displayScore,
                    style = MaterialTheme.typography.labelSmall,
                    color = KitsugiColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }

        // İzliyorum ikonu overlay
        if (alreadyInList) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Rounded.PlayCircle,
                contentDescription = "İzliyorum",
                tint = KitsugiColors.AccentGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}