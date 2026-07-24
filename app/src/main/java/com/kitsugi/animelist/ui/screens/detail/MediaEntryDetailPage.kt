@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.kitsugi.animelist.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.wrapContentHeight
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import com.kitsugi.animelist.utils.rememberScrollVisibilityState
import com.kitsugi.animelist.utils.rememberScrollConnection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.CircularProgressIndicator
import coil3.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.local.TranslationManager
import com.kitsugi.animelist.data.remote.DetailCache
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.KitsugiCharacter
import com.kitsugi.animelist.data.remote.KitsugiEpisodeRatingsRepository
import com.kitsugi.animelist.data.remote.KitsugiMediaDetail
import com.kitsugi.animelist.data.remote.KitsugiRelation
import com.kitsugi.animelist.data.remote.KitsugiReview
import com.kitsugi.animelist.data.remote.KitsugiStaff
import com.kitsugi.animelist.data.remote.KitsugiStreamingEpisode
import com.kitsugi.animelist.data.remote.KitsugiStats
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import com.kitsugi.animelist.ui.components.KitsugiPageEnter
import com.kitsugi.animelist.ui.components.KitsugiEpisodeOptionsDialog
import com.kitsugi.animelist.ui.components.KitsugiCinematicLoadingScreen
import com.kitsugi.animelist.ui.components.KitsugiImageGalleryDialog
import com.kitsugi.animelist.ui.components.KitsugiStreamSelectorBottomSheet
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiFullscreenPlayerActivity
import com.kitsugi.animelist.ui.screens.stream.KitsugiStreamActivity
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator
import com.kitsugi.animelist.data.local.MangaMappingEntity
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.ui.components.KitsugiIntegrationsSettingsDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import com.kitsugi.animelist.ui.theme.LocalIsTv
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.remember
import com.kitsugi.animelist.utils.parseToMediaType
import com.kitsugi.animelist.data.remote.GalleryItem
import com.kitsugi.animelist.data.remote.GalleryCategory

@Composable
fun MediaEntryDetailPage(
    entry: MediaEntry,
    onBackClick: () -> Unit,
    onIncrementProgressClick: () -> Unit,
    onToggleFavoriteClick: () -> Unit,
    onSynopsisLoaded: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRelationClick: (JikanSearchResult) -> Unit,
    onCharacterClick: (KitsugiCharacter) -> Unit,
    onStaffClick: (Int, String, String?, String?) -> Unit,
    onStudioClick: (Int, String, String?, String?) -> Unit,
    onUserProfileClick: (Int, String, String?) -> Unit = { _, _, _ -> },
    onSearchQuery: (String) -> Unit = {},
    onSearchByGenre: (String) -> Unit = {},
    onSearchByTag: (String) -> Unit = {},
    titleLanguage: String = "ROMAJI",
    scoreFormat: String = "POINT_10",
    hideScores: Boolean = false,
    showAnimeLogos: Boolean = false,
    blurAdultMedia: Boolean = false,
    onReadMangaClick: ((MangaMappingEntity?) -> Unit)? = null,
    preferredTranslator: String = "DEFAULT",
    mdbListShowImdb: Boolean = true,
    mdbListShowTomatoes: Boolean = true,
    mdbListShowMetacritic: Boolean = true,
    mdbListShowAudience: Boolean = false,
    mdbListShowLetterboxd: Boolean = false,
    mdbListShowTmdb: Boolean = false,
    mdbListShowTrakt: Boolean = false,
    settingsDataStore: SettingsDataStore? = null
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val accentColor = LocalKitsugiAccent.current
    val isTv = LocalIsTv.current
    val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current
    val externalUrl = buildExternalUrl(entry)
    val apiClient = remember { JikanApiClient() }

    val isSourceAniList = entry.source.lowercase() == "anilist"
    val isAniListConnected = remember { com.kitsugi.animelist.data.auth.ExternalAuthManager.getAniListToken(context) != null }
    val showFavouriteButton = isSourceAniList || isAniListConnected

    // Obtain ViewModel
    val viewModel: MediaEntryDetailViewModel = viewModel(key = "entry_${entry.source}_${entry.id}")

    val settingsState = settingsDataStore?.settingsFlow?.collectAsState(initial = AppSettings())?.value
    val mdbListRatings by viewModel.mdbListRatings.collectAsState()
    val mdbListLoading by viewModel.mdbListLoading.collectAsState()
    var showIntegrationsDialog by remember { mutableStateOf(false) }

    // Load entry in ViewModel
    LaunchedEffect(entry.id, entry.source, entry.malId, showAnimeLogos) {
        viewModel.loadEntry(entry, showAnimeLogos)
    }

    // Collect states from ViewModel
    val detailState by viewModel.detailState.collectAsState()
    val detailLoading by viewModel.detailLoading.collectAsState()
    val mangaMapping by viewModel.mangaMapping.collectAsState()
    val synopsisState by viewModel.synopsisState.collectAsState()
    val translatedSynopsis by viewModel.translatedSynopsis.collectAsState()
    val originalSynopsis by viewModel.originalSynopsis.collectAsState()
    val logoUrl by viewModel.logoUrl.collectAsState()
    val episodeRatings by viewModel.episodeRatings.collectAsState()
    val resolvedTmdbId by viewModel.resolvedTmdbId.collectAsState()
    val charactersState by viewModel.charactersState.collectAsState()
    val staffState by viewModel.staffState.collectAsState()
    val relationsState by viewModel.relationsState.collectAsState()
    val recommendationsState by viewModel.recommendationsState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val reviewsState by viewModel.reviewsState.collectAsState()
    val episodesState by viewModel.episodesState.collectAsState()
    val targetSeason by viewModel.targetSeason.collectAsState()
    val galleryItems by viewModel.galleryItems.collectAsState()

    val onMediaClick: (Int, String, String) -> Unit = { mediaId, mediaType, mediaSource ->
        val searchType = mediaType.parseToMediaType()
        val searchResult = JikanSearchResult(
            malId = mediaId,
            title = "Yükleniyor...",
            subtitle = "",
            type = searchType,
            total = null,
            score = null,
            isAdult = false,
            imageUrl = null,
            year = null,
            source = mediaSource
        )
        onRelationClick(searchResult)
    }

    val allImages = remember(entry.imageUrl, detailState?.pictures) {
        buildList {
            if (!entry.imageUrl.isNullOrBlank()) {
                add(entry.imageUrl)
            }
            detailState?.pictures?.let { addAll(it) }
        }.distinct()
    }
    var activeGalleryImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeGalleryIndex by remember { mutableStateOf(0) }
    // GalleryItem-based dialog state (Fanart.tv + multi-source)
    var activeGalleryItems by remember { mutableStateOf<List<GalleryItem>>(emptyList()) }
    var activeGalleryItemIndex by remember { mutableStateOf(0) }
    var activeEpisodeForOptions by remember { mutableStateOf<KitsugiStreamingEpisode?>(null) }
    var showWatchDialog by remember { mutableStateOf(false) }
    var watchEpisodeInput by remember { mutableStateOf("") }
    var showWatchStreamSelector by remember { mutableStateOf<Int?>(null) } // episode number

    val listState = rememberLazyListState()
    val tabListState = rememberLazyListState()
    val density = LocalDensity.current
    // TV odak highway — sol panel ↔ sağ panel tab bar
    val leftPanelFocusRequester = remember { FocusRequester() }
    val tabBarFocusRequester = remember { FocusRequester() }

    // State for tabs
    val isAnime = entry.type == MediaType.Anime
    val hasTvEpisodes = isAnime || entry.type == MediaType.TvShow
    val tabs = buildList {
        addAll(listOf("Bilgi", "Karakterler", "Ekip", "Öneriler", "İlişkiler", "Grafikler", "Yorumlar"))
        if (hasTvEpisodes) add("Bölümler")
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val selectedTab = pagerState.currentPage

    // Call loadTab when tab changes
    LaunchedEffect(entry.id, selectedTab, detailState?.realMalId, detailState?.tmdbId, resolvedTmdbId, detailState == null) {
        viewModel.loadTab(selectedTab, entry, detailState?.realMalId)
    }

    // Propagate original synopsis back to UI if requested
    LaunchedEffect(originalSynopsis) {
        val synopsis = originalSynopsis
        if (!synopsis.isNullOrBlank()) {
            onSynopsisLoaded(synopsis)
        }
    }

    KitsugiPageEnter {
        if (detailLoading) {
            KitsugiCinematicLoadingScreen(
                title = entry.title,
                imageUrl = entry.imageUrl,
                onBackClick = onBackClick,
                logoUrl = if (showAnimeLogos) logoUrl else null,
                isAdult = entry.isAdult,
                blurAdultMedia = blurAdultMedia
            )
        } else if (detailState == null) {
            DataUnavailableScreen(
                title = entry.title,
                onBackClick = onBackClick,
                onRetryClick = { viewModel.loadEntry(entry, showAnimeLogos, forceRefresh = true) }
            )
        } else {
            val pullRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = detailLoading,
                onRefresh = { viewModel.loadEntry(entry, showAnimeLogos, forceRefresh = true) },
                modifier = Modifier.fillMaxSize(),
                state = pullRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullRefreshState,
                        isRefreshing = detailLoading,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = KitsugiColors.Surface,
                        color = accentColor
                    )
                }
            ) {
                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (isLandscape) {
                    val configuration = LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp
                    val leftPanelWeight = when {
                        screenWidth >= 1200 -> 0.28f
                        screenWidth >= 840  -> 0.32f
                        else                -> 0.38f
                    }
                    val rightPanelWeight = 1f - leftPanelWeight
                // ── LANDSCAPE: Sol panel (Hero + Actions + Stats), Sağ panel (Tablar + İçerik) ──
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Sol Panel
                        val leftScrollState = rememberScrollState()
                        val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides if (isTvDevice) tvSpec else LocalBringIntoViewSpec.current
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(leftPanelWeight)
                                    .fillMaxSize()
                                    .then(if (isTvDevice) Modifier.dpadVerticalFastScroll(leftScrollState) else Modifier)
                                    .verticalScroll(leftScrollState)
                            ) {
                            DetailHero(
                                entry = entry,
                                logoUrl = if (showAnimeLogos) logoUrl else null,
                                onBackClick = onBackClick,
                                titleLanguage = titleLanguage,
                                blurAdultMedia = blurAdultMedia,
                                onPosterClick = { clickedUrl ->
                                    val index = allImages.indexOf(clickedUrl).coerceAtLeast(0)
                                    activeGalleryImages = allImages
                                    activeGalleryIndex = index
                                },
                                nextAiring = detailState?.nextAiringEpisode,
                                showFavoriteButton = showFavouriteButton,
                                onToggleFavoriteClick = onToggleFavoriteClick
                            )
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Spacer(modifier = Modifier.height(12.dp))
                                QuickActions(
                                    entry = entry,
                                    externalUrl = externalUrl,
                                    onIncrementProgressClick = onIncrementProgressClick,
                                    onToggleFavoriteClick = onToggleFavoriteClick,
                                    onEditClick = onEditClick,
                                    onDeleteClick = onDeleteClick,
                                    onOpenExternalClick = {
                                        if (externalUrl != null) {
                                            uriHandler.openUri(externalUrl)
                                        }
                                    },
                                    onWatchClick = if (entry.type == com.kitsugi.animelist.model.MediaType.Anime || entry.type == com.kitsugi.animelist.model.MediaType.TvShow || entry.type == com.kitsugi.animelist.model.MediaType.Movie) {
                                        {
                                            val streamMalId = if (entry.source.lowercase() == "anilist") {
                                                detailState?.realMalId ?: entry.malId
                                            } else {
                                                entry.id
                                            }
                                            val rawAniListId = if (entry.source.lowercase() == "anilist") entry.id else null
                                            val streamAniListId = rawAniListId?.let {
                                                if (it >= 100_000_000) it - 100_000_000 else it
                                            }
                                            KitsugiStreamActivity.start(
                                                context = context,
                                                malId = streamMalId,
                                                aniListId = streamAniListId,
                                                tmdbId = entry.tmdbId ?: detailState?.tmdbId ?: resolvedTmdbId,
                                                episode = 1,
                                                season = 1,
                                                isMovie = entry.type == com.kitsugi.animelist.model.MediaType.Movie,
                                                title = entry.title,
                                                posterUrl = entry.imageUrl,
                                                titleEnglish = detailState?.titleEnglish,
                                                titleRomaji = detailState?.titleRomaji,
                                                titleNative = detailState?.titleNative,
                                                startYear = entry.year
                                            )
                                        }
                                    } else null,
                                    onReadClick = if (entry.type == com.kitsugi.animelist.model.MediaType.Manga) {
                                        {
                                            if (onReadMangaClick != null) {
                                                onReadMangaClick(mangaMapping)
                                            }
                                        }
                                    } else null,
                                    mangaMapping = mangaMapping,
                                    onLinkMangaClick = if (entry.type == com.kitsugi.animelist.model.MediaType.Manga) {
                                        {
                                            if (onReadMangaClick != null) {
                                                onReadMangaClick(null)
                                            }
                                        }
                                    } else null,
                                    onUnlinkMangaClick = {
                                        viewModel.deleteMangaMapping(entry.id)
                                    },
                                    primaryFocusRequester = leftPanelFocusRequester,
                                    tabBarFocusRequester = tabBarFocusRequester
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                        }

                        // Sağ Panel
                        Column(
                            modifier = Modifier
                                .weight(rightPanelWeight)
                                .fillMaxSize()
                        ) {
                            // Tablar
                            LaunchedEffect(selectedTab) {
                                tabListState.animateScrollToItem(selectedTab)
                            }
                            LazyRow(
                                state = tabListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(tabBarFocusRequester)
                                    .focusProperties { left = leftPanelFocusRequester }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(tabs) { index, title ->
                                    val isSelected = selectedTab == index
                                    val bgColor = if (isSelected) accentColor else KitsugiColors.Surface
                                    val textColor = if (isSelected) KitsugiColors.Background else KitsugiColors.TextSecondary
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(bgColor)
                                            .tvClickable(shape = RoundedCornerShape(999.dp)) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title,
                                            color = textColor,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            // Tab İçeriği
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(KitsugiColors.Background)
                            ) {
                                HorizontalPager(
                                    state = pagerState,
                                    userScrollEnabled = !isTvDevice,
                                    beyondViewportPageCount = 1,
                                    pageSpacing = 12.dp,
                                    modifier = Modifier.fillMaxSize().clipToBounds()
                                ) { page ->
                                    val pageScrollState = rememberScrollState()
                                    val pageTvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
                                    CompositionLocalProvider(
                                        LocalBringIntoViewSpec provides if (isTvDevice) pageTvSpec else LocalBringIntoViewSpec.current
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(if (isTvDevice) Modifier.dpadVerticalFastScroll(pageScrollState) else Modifier)
                                                .verticalScroll(pageScrollState)
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                        when (page) {
                                            0 -> {
                                                EntryDetailOverviewTab(
                                                    entry = entry,
                                                    detail = detailState,
                                                    synopsisState = synopsisState,
                                                    originalSynopsis = originalSynopsis,
                                                    externalUrl = externalUrl,
                                                    onSearchQuery = onSearchQuery,
                                                    onStudioClick = onStudioClick,
                                                    onGenreClick = onSearchByGenre,
                                                    onTagClick = onSearchByTag,
                                                    preferredTranslator = preferredTranslator,
                                                    mdbListRatings = mdbListRatings,
                                                    mdbListLoading = mdbListLoading,
                                                    mdbListShowImdb = settingsState?.mdbListShowImdb ?: mdbListShowImdb,
                                                    mdbListShowTomatoes = settingsState?.mdbListShowTomatoes ?: mdbListShowTomatoes,
                                                    mdbListShowMetacritic = settingsState?.mdbListShowMetacritic ?: mdbListShowMetacritic,
                                                    mdbListShowAudience = settingsState?.mdbListShowAudience ?: mdbListShowAudience,
                                                    mdbListShowLetterboxd = settingsState?.mdbListShowLetterboxd ?: mdbListShowLetterboxd,
                                                    mdbListShowTmdb = settingsState?.mdbListShowTmdb ?: mdbListShowTmdb,
                                                    mdbListShowTrakt = settingsState?.mdbListShowTrakt ?: mdbListShowTrakt,
                                                    onSettingsClick = if (settingsDataStore != null) {
                                                        { showIntegrationsDialog = true }
                                                    } else null,
                                                    onImageGalleryRequest = { urls, index ->
                                                        activeGalleryImages = urls
                                                        activeGalleryIndex = index
                                                    },
                                                    galleryItems = galleryItems,
                                                    onGalleryItemRequest = { items, index ->
                                                        activeGalleryItems = items
                                                        activeGalleryItemIndex = index
                                                    }
                                                )
                                            }
                                            1 -> CharactersTabContent(state = charactersState, onCharacterClick = onCharacterClick, onStaffClick = onStaffClick, onMediaClick = onMediaClick)
                                            2 -> StaffTabContent(state = staffState, onStaffClick = onStaffClick)
                                            3 -> RecommendationsTabContent(state = recommendationsState, titleLanguage = titleLanguage, blurAdultMedia = blurAdultMedia, onRecommendationClick = { rel ->
                                                val typeLabel = when (rel.mediaType) {
                                                    MediaType.Anime -> "Anime"
                                                    MediaType.Movie -> "Film"
                                                    MediaType.TvShow -> "Dizi"
                                                    MediaType.Manga -> "Manga"
                                                }
                                                val result = JikanSearchResult(
                                                    malId = rel.malId,
                                                    title = rel.title,
                                                    subtitle = "${rel.relationType}, $typeLabel",
                                                    type = rel.mediaType,
                                                    total = null,
                                                    score = null,
                                                    isAdult = rel.isAdult,
                                                    imageUrl = rel.imageUrl,
                                                    year = null,
                                                    realMalId = rel.malId,
                                                    source = rel.source,
                                                    titleEnglish = rel.titleEnglish,
                                                    titleJapanese = rel.titleJapanese
                                                )
                                                onRelationClick(result)
                                            })
                                            4 -> RelationsTabContent(state = relationsState, titleLanguage = titleLanguage, blurAdultMedia = blurAdultMedia, onRelationClick = { rel ->
                                                val typeLabel = when (rel.mediaType) {
                                                    MediaType.Anime -> "Anime"
                                                    MediaType.Movie -> "Film"
                                                    MediaType.TvShow -> "Dizi"
                                                    MediaType.Manga -> "Manga"
                                                }
                                                val result = JikanSearchResult(
                                                    malId = rel.malId,
                                                    title = rel.title,
                                                    subtitle = "${rel.relationType}, $typeLabel",
                                                    type = rel.mediaType,
                                                    total = null,
                                                    score = null,
                                                    isAdult = rel.isAdult,
                                                    imageUrl = rel.imageUrl,
                                                    year = null,
                                                    realMalId = rel.malId,
                                                    source = rel.source,
                                                    titleEnglish = rel.titleEnglish,
                                                    titleJapanese = rel.titleJapanese
                                                )
                                                onRelationClick(result)
                                            })
                                            5 -> StatsTabContent(state = statsState)
                                            6 -> ReviewsTabContent(
                                                state = reviewsState,
                                                source = entry.source,
                                                externalId = entry.malId ?: 0,
                                                mediaType = entry.type,
                                                apiClient = apiClient,
                                                titleLanguage = titleLanguage,
                                                onUserProfileClick = onUserProfileClick,
                                                preferredTranslator = preferredTranslator
                                            )
                                            7 -> {
                                                EntryDetailEpisodesTab(
                                                    entry = entry,
                                                    detailState = detailState,
                                                    state = episodesState,
                                                    episodeRatings = episodeRatings,
                                                    targetSeason = targetSeason,
                                                    onSeasonSelected = { newSeason ->
                                                        viewModel.setTargetSeason(newSeason, entry)
                                                    },
                                                    onEpisodeOptionsRequested = { episode ->
                                                        activeEpisodeForOptions = episode
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                }
                            }
                        }
                    }
                }
            } else {
                // ── PORTRAIT ──
                Box(modifier = Modifier.fillMaxSize()) {
                    val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
                    CompositionLocalProvider(
                        LocalBringIntoViewSpec provides if (isTvDevice) tvSpec else LocalBringIntoViewSpec.current
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(KitsugiColors.Background)
                                .then(if (isTvDevice) Modifier.dpadVerticalFastScroll(listState) else Modifier)
                        ) {
                    // Hero
                    item(key = "hero") {
                        DetailHero(
                            entry = entry,
                            logoUrl = if (showAnimeLogos) logoUrl else null,
                            onBackClick = onBackClick,
                            titleLanguage = titleLanguage,
                            blurAdultMedia = blurAdultMedia,
                            onPosterClick = { clickedUrl ->
                                val index = allImages.indexOf(clickedUrl).coerceAtLeast(0)
                                activeGalleryImages = allImages
                                activeGalleryIndex = index
                            },
                            nextAiring = detailState?.nextAiringEpisode,
                            showFavoriteButton = showFavouriteButton,
                            onToggleFavoriteClick = onToggleFavoriteClick
                        )
                    }

                    // Butonlar + İstatistikler
                    item(key = "info") {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Spacer(modifier = Modifier.height(18.dp))

                            QuickActions(
                                entry = entry,
                                externalUrl = externalUrl,
                                onIncrementProgressClick = onIncrementProgressClick,
                                onToggleFavoriteClick = onToggleFavoriteClick,
                                onEditClick = onEditClick,
                                onDeleteClick = onDeleteClick,
                                onOpenExternalClick = {
                                    if (externalUrl != null) {
                                        uriHandler.openUri(externalUrl)
                                    }
                                },
                                onWatchClick = if (entry.type == com.kitsugi.animelist.model.MediaType.Anime || entry.type == com.kitsugi.animelist.model.MediaType.TvShow || entry.type == com.kitsugi.animelist.model.MediaType.Movie) {
                                    {
                                        val streamMalId = if (entry.source.lowercase() == "anilist") {
                                            detailState?.realMalId ?: entry.malId
                                        } else {
                                            entry.id
                                        }
                                        val rawAniListId = if (entry.source.lowercase() == "anilist") entry.id else null
                                        val streamAniListId = rawAniListId?.let {
                                            if (it >= 100_000_000) it - 100_000_000 else it
                                        }
                                        KitsugiStreamActivity.start(
                                            context = context,
                                            malId = streamMalId,
                                            aniListId = streamAniListId,
                                            tmdbId = entry.tmdbId ?: detailState?.tmdbId ?: resolvedTmdbId,
                                            episode = 1,
                                            season = 1,
                                            isMovie = entry.type == com.kitsugi.animelist.model.MediaType.Movie,
                                            title = entry.title,
                                            posterUrl = entry.imageUrl,
                                            titleEnglish = detailState?.titleEnglish,
                                            titleRomaji = detailState?.titleRomaji,
                                            titleNative = detailState?.titleNative,
                                            startYear = entry.year
                                        )
                                    }
                                } else null,
                                onReadClick = if (entry.type == com.kitsugi.animelist.model.MediaType.Manga) {
                                    {
                                        if (onReadMangaClick != null) {
                                            onReadMangaClick(mangaMapping)
                                        }
                                    }
                                } else null,
                                mangaMapping = mangaMapping,
                                onLinkMangaClick = if (entry.type == com.kitsugi.animelist.model.MediaType.Manga) {
                                    {
                                        if (onReadMangaClick != null) {
                                            onReadMangaClick(null)
                                        }
                                    }
                                } else null,
                                onUnlinkMangaClick = {
                                    viewModel.deleteMangaMapping(entry.id)
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Sekme Barı — NATIVE STICKY HEADER
                    stickyHeader(key = "tabs") {
                        val accentColor = LocalKitsugiAccent.current
                        LaunchedEffect(selectedTab) {
                            // Sekme değiştiğinde eğer aşağı kaydırılmışsa sekmeleri üste sabitleyecek şekilde yukarı kaydır
                            if (listState.firstVisibleItemIndex > 2) {
                                listState.scrollToItem(2)
                            }
                            val itemInfo = tabListState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.index == selectedTab }
                            if (itemInfo != null) {
                                val centerOffset = (tabListState.layoutInfo.viewportEndOffset - itemInfo.size) / 2
                                tabListState.animateScrollToItem(selectedTab, -centerOffset)
                            } else {
                                tabListState.animateScrollToItem(selectedTab)
                            }
                        }

                        val showFloatingHeader = listState.firstVisibleItemIndex >= 2

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(KitsugiColors.Surface.copy(alpha = 0.97f))
                        ) {
                            AnimatedVisibility(
                                visible = showFloatingHeader,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .background(KitsugiColors.Surface.copy(alpha = 0.92f))
                                        .padding(horizontal = 8.dp)
                                ) {
                                    IconButton(onClick = onBackClick) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = "Geri",
                                            tint = KitsugiColors.TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = entry.title,
                                        color = KitsugiColors.TextPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        val mediaId = entry.malId ?: entry.id
                                        val url = com.kitsugi.animelist.utils.ShareUtils.buildMediaUrl(entry.source, mediaId, entry.type)
                                        com.kitsugi.animelist.utils.ShareUtils.shareText(context, entry.title, url)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Share,
                                            contentDescription = "Paylaş",
                                            tint = KitsugiColors.TextSecondary
                                        )
                                    }
                                    if (showFavouriteButton) {
                                        IconButton(onClick = onToggleFavoriteClick) {
                                            Icon(
                                                imageVector = if (entry.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                contentDescription = if (entry.isFavorite) "Favoriden Çıkar" else "Favori Yap",
                                                tint = if (entry.isFavorite) accentColor else KitsugiColors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            LazyRow(
                                state = tabListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(tabs) { index, title ->
                                    val isSelected = selectedTab == index
                                    val bgColor = if (isSelected) accentColor else KitsugiColors.Background
                                    val textColor = if (isSelected) KitsugiColors.Background else KitsugiColors.TextSecondary
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(bgColor)
                                            .tvClickable(shape = RoundedCornerShape(999.dp)) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title,
                                            color = textColor,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(KitsugiColors.SurfaceSoft)
                            )
                        }
                    }
                    // Sekme İçerikleri
                    item(key = "content") {
                        val pageHeights = remember { androidx.compose.runtime.mutableStateMapOf<Int, Int>() }
                        val density = androidx.compose.ui.platform.LocalDensity.current
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

                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = !isTv,
                            beyondViewportPageCount = 1,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
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
                                        }
                                ) {
                                    when (page) {
                                    0 -> {
                                        EntryDetailOverviewTab(
                                            entry = entry,
                                            detail = detailState,
                                            synopsisState = synopsisState,
                                            originalSynopsis = originalSynopsis,
                                            externalUrl = externalUrl,
                                            onSearchQuery = onSearchQuery,
                                            onStudioClick = onStudioClick,
                                            onGenreClick = onSearchByGenre,
                                            onTagClick = onSearchByTag,
                                            preferredTranslator = preferredTranslator,
                                            mdbListRatings = mdbListRatings,
                                            mdbListLoading = mdbListLoading,
                                            mdbListShowImdb = settingsState?.mdbListShowImdb ?: mdbListShowImdb,
                                            mdbListShowTomatoes = settingsState?.mdbListShowTomatoes ?: mdbListShowTomatoes,
                                            mdbListShowMetacritic = settingsState?.mdbListShowMetacritic ?: mdbListShowMetacritic,
                                            mdbListShowAudience = settingsState?.mdbListShowAudience ?: mdbListShowAudience,
                                            mdbListShowLetterboxd = settingsState?.mdbListShowLetterboxd ?: mdbListShowLetterboxd,
                                            mdbListShowTmdb = settingsState?.mdbListShowTmdb ?: mdbListShowTmdb,
                                            mdbListShowTrakt = settingsState?.mdbListShowTrakt ?: mdbListShowTrakt,
                                            onSettingsClick = if (settingsDataStore != null) {
                                                { showIntegrationsDialog = true }
                                            } else null,
                                            onImageGalleryRequest = { urls, index ->
                                                activeGalleryImages = urls
                                                activeGalleryIndex = index
                                            },
                                            galleryItems = galleryItems,
                                            onGalleryItemRequest = { items, index ->
                                                activeGalleryItems = items
                                                activeGalleryItemIndex = index
                                            }
                                        )
                                    }
                                    1 -> CharactersTabContent(state = charactersState, onCharacterClick = onCharacterClick, onStaffClick = onStaffClick, onMediaClick = onMediaClick)
                                    2 -> StaffTabContent(state = staffState, onStaffClick = onStaffClick)
                                    3 -> RecommendationsTabContent(state = recommendationsState, titleLanguage = titleLanguage, blurAdultMedia = blurAdultMedia, onRecommendationClick = { rel ->
                                        val typeLabel = when (rel.mediaType) {
                                            MediaType.Anime -> "Anime"
                                            MediaType.Movie -> "Film"
                                            MediaType.TvShow -> "Dizi"
                                            MediaType.Manga -> "Manga"
                                        }
                                        val result = JikanSearchResult(
                                            malId = rel.malId,
                                            title = rel.title,
                                            subtitle = "${rel.relationType}, $typeLabel",
                                            type = rel.mediaType,
                                            total = null,
                                            score = null,
                                            isAdult = rel.isAdult,
                                            imageUrl = rel.imageUrl,
                                            year = null,
                                            realMalId = rel.malId,
                                            source = rel.source,
                                            titleEnglish = rel.titleEnglish,
                                            titleJapanese = rel.titleJapanese
                                        )
                                        onRelationClick(result)
                                    })
                                    4 -> RelationsTabContent(state = relationsState, titleLanguage = titleLanguage, blurAdultMedia = blurAdultMedia, onRelationClick = { rel ->
                                        val typeLabel = when (rel.mediaType) {
                                            MediaType.Anime -> "Anime"
                                            MediaType.Movie -> "Film"
                                            MediaType.TvShow -> "Dizi"
                                            MediaType.Manga -> "Manga"
                                        }
                                        val result = JikanSearchResult(
                                            malId = rel.malId,
                                            title = rel.title,
                                            subtitle = "${rel.relationType}, $typeLabel",
                                            type = rel.mediaType,
                                            total = null,
                                            score = null,
                                            isAdult = rel.isAdult,
                                            imageUrl = rel.imageUrl,
                                            year = null,
                                            realMalId = rel.malId,
                                            source = rel.source,
                                            titleEnglish = rel.titleEnglish,
                                            titleJapanese = rel.titleJapanese
                                        )
                                        onRelationClick(result)
                                    })
                                    5 -> StatsTabContent(state = statsState)
                                    6 -> ReviewsTabContent(
                                        state = reviewsState,
                                        source = entry.source,
                                        externalId = entry.malId ?: 0,
                                        mediaType = entry.type,
                                        apiClient = apiClient,
                                        titleLanguage = titleLanguage,
                                        onUserProfileClick = onUserProfileClick,
                                        preferredTranslator = preferredTranslator
                                    )
                                    7 -> {
                                        EntryDetailEpisodesTab(
                                            entry = entry,
                                            detailState = detailState,
                                            state = episodesState,
                                            episodeRatings = episodeRatings,
                                            targetSeason = targetSeason,
                                            onSeasonSelected = { newSeason ->
                                                viewModel.setTargetSeason(newSeason, entry)
                                            },
                                            onEpisodeOptionsRequested = { episode ->
                                                activeEpisodeForOptions = episode
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
                }

            } // end portrait Box
        } // end portrait else
            } // end PullToRefreshBox
    } // end else (non-loading)

        if (activeGalleryItems.isNotEmpty()) {
            KitsugiImageGalleryDialog(
                galleryItems = activeGalleryItems,
                initialIndex = activeGalleryItemIndex,
                title = entry.title,
                onDismiss = { activeGalleryItems = emptyList() }
            )
        } else if (activeGalleryImages.isNotEmpty()) {
            KitsugiImageGalleryDialog(
                imageUrls = activeGalleryImages,
                initialIndex = activeGalleryIndex,
                title = entry.title,
                onDismiss = { activeGalleryImages = emptyList() }
            )
        }

        activeEpisodeForOptions?.let { ep ->
            val entryMalId = if (entry.source.lowercase() == "anilist") entry.malId else entry.id
            val entryAniListId = if (entry.source.lowercase() == "anilist") entry.id else null
            KitsugiEpisodeOptionsDialog(
                animeTitle = entry.title,
                episodeNumber = ep.episodeNumber,
                episodeTitle = ep.title,
                originalUrl = ep.url,
                siteName = ep.site,
                malId = entryMalId,
                aniListId = entryAniListId,
                tmdbId = entry.tmdbId ?: detailState?.tmdbId ?: resolvedTmdbId,
                posterUrl = entry.imageUrl,
                titleEnglish = detailState?.titleEnglish,
                titleRomaji = detailState?.titleRomaji,
                titleNative = detailState?.titleNative,
                startYear = entry.year,
                isMovie = entry.type == com.kitsugi.animelist.model.MediaType.Movie,
                onDismiss = { activeEpisodeForOptions = null }
            )
        }

        // İzle butonu - bölüm numarası dialog
        if (showWatchDialog) {
            val accentColor = LocalKitsugiAccent.current
            AlertDialog(
                onDismissRequest = { showWatchDialog = false },
                containerColor = KitsugiColors.Surface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = accentColor
                        )
                        Text(
                            text = entry.title,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Hangi bölümü izlemek istiyorsunuz?",
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = watchEpisodeInput,
                            onValueChange = { watchEpisodeInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Bölüm Numarası") },
                            placeholder = { Text("Örn: 1") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = KitsugiColors.Border,
                                focusedLabelColor = accentColor,
                                cursorColor = accentColor,
                                focusedTextColor = KitsugiColors.TextPrimary,
                                unfocusedTextColor = KitsugiColors.TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    val epNum = watchEpisodeInput.toIntOrNull()
                    Button(
                        onClick = {
                            if (epNum != null && epNum > 0) {
                                showWatchDialog = false
                                // For MAL entries: malId = entry.id, aniListId = null
                                // For AniList entries: malId = detailState.realMalId, aniListId = entry.id - 100_000_000
                                val streamMalId = if (entry.source.lowercase() == "anilist") {
                                    detailState?.realMalId ?: entry.malId
                                } else {
                                    entry.id
                                }
                                val rawAniListId = if (entry.source.lowercase() == "anilist") entry.id else null
                                val streamAniListId = rawAniListId?.let {
                                    if (it >= 100_000_000) it - 100_000_000 else it
                                }
                                KitsugiStreamActivity.start(
                                    context = context,
                                    malId = streamMalId,
                                    aniListId = streamAniListId,
                                    tmdbId = entry.tmdbId ?: detailState?.tmdbId ?: resolvedTmdbId,
                                    episode = epNum,
                                    season = 1,
                                    isMovie = entry.type == com.kitsugi.animelist.model.MediaType.Movie,
                                    title = entry.title,
                                    posterUrl = entry.imageUrl,
                                    titleEnglish = detailState?.titleEnglish,
                                    titleRomaji = detailState?.titleRomaji,
                                    titleNative = detailState?.titleNative,
                                    startYear = entry.year
                                )
                            }
                        },
                        enabled = (epNum ?: 0) > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("İzle", color = KitsugiColors.Background, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWatchDialog = false }) {
                        Text("İptal", color = KitsugiColors.TextSecondary)
                    }
                }
            )
        }

        if (showIntegrationsDialog && settingsDataStore != null && settingsState != null) {
            KitsugiIntegrationsSettingsDialog(
                tmdbEnabled = settingsState.tmdbEnabled,
                onTmdbEnabledChanged = { coroutineScope.launch { settingsDataStore.setTmdbEnabled(it) } },
                tmdbApiKey = settingsState.tmdbUserApiKey,
                onTmdbApiKeyChanged = { coroutineScope.launch { settingsDataStore.setTmdbUserApiKey(it) } },
                tmdbModernHomeEnabled = settingsState.tmdbModernHomeEnabled,
                onTmdbModernHomeEnabledChanged = { coroutineScope.launch { settingsDataStore.setTmdbModernHomeEnabled(it) } },
                tmdbEnrichContinueWatching = settingsState.tmdbEnrichContinueWatching,
                onTmdbEnrichContinueWatchingChanged = { coroutineScope.launch { settingsDataStore.setTmdbEnrichContinueWatching(it) } },
                tmdbLanguage = settingsState.tmdbLanguage,
                onTmdbLanguageChanged = { coroutineScope.launch { settingsDataStore.setTmdbLanguage(it) } },
                tmdbUseArtwork = settingsState.tmdbUseArtwork,
                onTmdbUseArtworkChanged = { coroutineScope.launch { settingsDataStore.setTmdbUseArtwork(it) } },
                tmdbUseBasicInfo = settingsState.tmdbUseBasicInfo,
                onTmdbUseBasicInfoChanged = { coroutineScope.launch { settingsDataStore.setTmdbUseBasicInfo(it) } },
                tmdbUseDetails = settingsState.tmdbUseDetails,
                onTmdbUseDetailsChanged = { coroutineScope.launch { settingsDataStore.setTmdbUseDetails(it) } },
                tmdbUseReleaseDates = settingsState.tmdbUseReleaseDates,
                onTmdbUseReleaseDatesChanged = { coroutineScope.launch { settingsDataStore.setTmdbUseReleaseDates(it) } },
                tmdbUseCredits = settingsState.tmdbUseCredits,
                onTmdbUseCreditsChanged = { coroutineScope.launch { settingsDataStore.setTmdbUseCredits(it) } },
                tmdbUseProductions = settingsState.tmdbUseProductions,
                onTmdbUseProductionsChanged = { coroutineScope.launch { settingsDataStore.setTmdbUseProductions(it) } },
                tmdbUseNetworks = settingsState.tmdbUseNetworks,
                onTmdbUseNetworksChanged = { coroutineScope.launch { settingsDataStore.setTmdbUseNetworks(it) } },
                tmdbUseEpisodes = settingsState.tmdbUseEpisodes,
                onTmdbUseEpisodesChanged = { coroutineScope.launch { settingsDataStore.setTmdbUseEpisodes(it) } },
                tmdbUseTrailers = settingsState.tmdbUseTrailers,
                onTmdbUseTrailersChanged = { coroutineScope.launch { settingsDataStore.setTmdbUseTrailers(it) } },
                tmdbUseMoreLikeThis = settingsState.tmdbUseMoreLikeThis,
                onTmdbUseMoreLikeThisChanged = { coroutineScope.launch { settingsDataStore.setTmdbUseMoreLikeThis(it) } },
                tmdbUseCollections = settingsState.tmdbUseCollections,
                onTmdbUseCollectionsChanged = { coroutineScope.launch { settingsDataStore.setTmdbUseCollections(it) } },
                
                mdbListEnabled = settingsState.mdbListEnabled,
                onMdbListEnabledChanged = { coroutineScope.launch { settingsDataStore.setMdbListEnabled(it) } },
                mdbListApiKey = settingsState.mdbListApiKey,
                onMdbListApiKeyChanged = { coroutineScope.launch { settingsDataStore.setMdbListApiKey(it) } },
                mdbListShowImdb = settingsState.mdbListShowImdb,
                onMdbListShowImdbChanged = { coroutineScope.launch { settingsDataStore.setMdbListShowImdb(it) } },
                mdbListShowTomatoes = settingsState.mdbListShowTomatoes,
                onMdbListShowTomatoesChanged = { coroutineScope.launch { settingsDataStore.setMdbListShowTomatoes(it) } },
                mdbListShowMetacritic = settingsState.mdbListShowMetacritic,
                onMdbListShowMetacriticChanged = { coroutineScope.launch { settingsDataStore.setMdbListShowMetacritic(it) } },
                mdbListShowAudience = settingsState.mdbListShowAudience,
                onMdbListShowAudienceChanged = { coroutineScope.launch { settingsDataStore.setMdbListShowAudience(it) } },
                mdbListShowLetterboxd = settingsState.mdbListShowLetterboxd,
                onMdbListShowLetterboxdChanged = { coroutineScope.launch { settingsDataStore.setMdbListShowLetterboxd(it) } },
                mdbListShowTmdb = settingsState.mdbListShowTmdb,
                onMdbListShowTmdbChanged = { coroutineScope.launch { settingsDataStore.setMdbListShowTmdb(it) } },
                mdbListShowTrakt = settingsState.mdbListShowTrakt,
                onMdbListShowTraktChanged = { coroutineScope.launch { settingsDataStore.setMdbListShowTrakt(it) } },
                
                aniSkipEnabled = settingsState.aniSkipEnabled,
                onAniSkipEnabledChanged = { coroutineScope.launch { settingsDataStore.setAniSkipEnabled(it) } },
                aniSkipAutoSkip = settingsState.aniSkipAutoSkip,
                onAniSkipAutoSkipChanged = { coroutineScope.launch { settingsDataStore.setAniSkipAutoSkip(it) } },
                animeSkipClientId = settingsState.animeSkipClientId,
                onAnimeSkipClientIdChanged = { coroutineScope.launch { settingsDataStore.setAnimeSkipClientId(it) } },
                fanartTvEnabled = settingsState.fanartTvEnabled,
                onFanartTvEnabledChanged = { coroutineScope.launch { settingsDataStore.setFanartTvEnabled(it) } },
                fanartTvApiKey = settingsState.fanartTvApiKey,
                onFanartTvApiKeyChanged = { coroutineScope.launch { settingsDataStore.setFanartTvApiKey(it) } },
                onDismiss = { showIntegrationsDialog = false }
            )
        }

    } // KitsugiPageEnter
}

internal fun buildExternalUrl(entry: MediaEntry): String? {
    val id = entry.malId ?: return null
    return com.kitsugi.animelist.utils.ShareUtils.buildExternalMediaUrl(entry.source, id, type = entry.type)
}

internal fun progressUnit(entry: MediaEntry): String {
    return when (entry.type) {
        MediaType.Anime -> "bölüm"
        MediaType.Manga -> "chapter"
        else -> "bölüm"
    }
}

internal fun entryProgressText(entry: MediaEntry): String {
    val totalText = entry.total?.toString() ?: "?"
    return "${entry.progress}/$totalText ${progressUnit(entry)}"
}

internal fun scoreText(entry: MediaEntry): String {
    return if (entry.score == null) {
        "-"
    } else {
        "${entry.score}/10"
    }
}

sealed class SynopsisState {
    data object Loading : SynopsisState()
    data object Error : SynopsisState()
    data class Success(val text: String) : SynopsisState()
}