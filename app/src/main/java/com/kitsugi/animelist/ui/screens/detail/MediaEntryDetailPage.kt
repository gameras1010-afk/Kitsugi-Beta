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
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Text
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
    val galleryLoading by viewModel.galleryLoading.collectAsState()
    val pageResetTrigger by viewModel.pageResetTrigger.collectAsState()

    // Sadece detay verisi hazır olana kadar yükleme ekranı göster.
    // Galeri (fanart.tv) arka planda yüklenirken sayfa zaten açık kalır.
    val isLoading = detailLoading

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

    // GalleryItem-based dialog state (Fanart.tv + multi-source)
    var activeGalleryItems by remember { mutableStateOf<List<GalleryItem>>(emptyList()) }
    var activeGalleryIndex by remember { mutableStateOf(0) }
    var activeEpisodeForOptions by remember { mutableStateOf<KitsugiStreamingEpisode?>(null) }

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

    // Yeni bir entry'ye geçildiğinde (geri tuşuyla aynı sayfaya dönüldüğünde dahil) tab’ı sıfırla
    LaunchedEffect(pageResetTrigger) {
        if (pageResetTrigger > 0) {
            pagerState.scrollToPage(0)
        }
    }

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
        DetailPageScaffold(
            title = entry.title,
            isLoading = isLoading,
            isError = !detailLoading && detailState == null,
            isRefreshing = detailLoading,
            onRefresh = { viewModel.loadEntry(entry, showAnimeLogos, forceRefresh = true) },
            onBackClick = onBackClick,
            tabs = tabs,
            pagerState = pagerState,
            listState = listState,
            tabListState = tabListState,
            loadingScreen = {
                KitsugiCinematicLoadingScreen(
                    title = entry.title,
                    imageUrl = entry.imageUrl,
                    onBackClick = onBackClick,
                    logoUrl = if (showAnimeLogos) logoUrl else null,
                    isAdult = entry.isAdult,
                    blurAdultMedia = blurAdultMedia
                )
            },
            errorScreen = {
                DataUnavailableScreen(
                    title = entry.title,
                    onBackClick = onBackClick,
                    onRetryClick = { viewModel.loadEntry(entry, showAnimeLogos, forceRefresh = true) }
                )
            },
            leftPanel = { lpFocus, tbFocus ->
                EntryDetailLeftPanel(
                    entry = entry,
                    detailState = detailState,
                    logoUrl = logoUrl,
                    galleryItems = galleryItems,
                    mangaMapping = mangaMapping,
                    resolvedTmdbId = resolvedTmdbId,
                    showAnimeLogos = showAnimeLogos,
                    showFavouriteButton = showFavouriteButton,
                    titleLanguage = titleLanguage,
                    blurAdultMedia = blurAdultMedia,
                    onBackClick = onBackClick,
                    onIncrementProgressClick = onIncrementProgressClick,
                    onToggleFavoriteClick = onToggleFavoriteClick,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                    onReadMangaClick = onReadMangaClick,
                    onUnlinkMangaClick = { viewModel.deleteMangaMapping(entry.id) },
                    onGalleryOpen = { items, idx ->
                        activeGalleryItems = items
                        activeGalleryIndex = idx
                    },
                    leftPanelFocusRequester = lpFocus,
                    tabBarFocusRequester = tbFocus
                )
            },
            floatingHeaderActions = {
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
                if (galleryItems.isNotEmpty()) {
                    IconButton(onClick = {
                        activeGalleryItems = galleryItems
                        activeGalleryIndex = 0
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = "Galeri",
                            tint = accentColor
                        )
                    }
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
            },
            portraitTopItems = { _, lpFocus, tbFocus ->
                item(key = "hero") {
                    EntryDetailLeftPanel(
                        entry = entry,
                        detailState = detailState,
                        logoUrl = logoUrl,
                        galleryItems = galleryItems,
                        mangaMapping = mangaMapping,
                        resolvedTmdbId = resolvedTmdbId,
                        showAnimeLogos = showAnimeLogos,
                        showFavouriteButton = showFavouriteButton,
                        titleLanguage = titleLanguage,
                        blurAdultMedia = blurAdultMedia,
                        onBackClick = onBackClick,
                        onIncrementProgressClick = onIncrementProgressClick,
                        onToggleFavoriteClick = onToggleFavoriteClick,
                        onEditClick = onEditClick,
                        onDeleteClick = onDeleteClick,
                        onReadMangaClick = onReadMangaClick,
                        onUnlinkMangaClick = { viewModel.deleteMangaMapping(entry.id) },
                        onGalleryOpen = { items, idx ->
                            activeGalleryItems = items
                            activeGalleryIndex = idx
                        },
                        leftPanelFocusRequester = lpFocus,
                        tabBarFocusRequester = tbFocus
                    )
                }
            },
            pageContent = { page ->
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
                                activeGalleryItems = urls.map { url ->
                                    GalleryItem(url = url, category = GalleryCategory.OTHER, source = "Açıklama")
                                }
                                activeGalleryIndex = index
                            },
                            galleryItems = galleryItems,
                            galleryLoading = galleryLoading,
                            onGalleryItemRequest = { items, index ->
                                activeGalleryItems = items
                                activeGalleryIndex = index
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
                        onRelationClick(JikanSearchResult(
                            malId = rel.malId,
                            title = rel.title,
                            subtitle = "${rel.relationType}, $typeLabel",
                            type = rel.mediaType,
                            total = null, score = null,
                            isAdult = rel.isAdult,
                            imageUrl = rel.imageUrl,
                            year = null, realMalId = rel.malId,
                            source = rel.source,
                            titleEnglish = rel.titleEnglish,
                            titleJapanese = rel.titleJapanese
                        ))
                    })
                    4 -> RelationsTabContent(state = relationsState, titleLanguage = titleLanguage, blurAdultMedia = blurAdultMedia, onRelationClick = { rel ->
                        val typeLabel = when (rel.mediaType) {
                            MediaType.Anime -> "Anime"
                            MediaType.Movie -> "Film"
                            MediaType.TvShow -> "Dizi"
                            MediaType.Manga -> "Manga"
                        }
                        onRelationClick(JikanSearchResult(
                            malId = rel.malId,
                            title = rel.title,
                            subtitle = "${rel.relationType}, $typeLabel",
                            type = rel.mediaType,
                            total = null, score = null,
                            isAdult = rel.isAdult,
                            imageUrl = rel.imageUrl,
                            year = null, realMalId = rel.malId,
                            source = rel.source,
                            titleEnglish = rel.titleEnglish,
                            titleJapanese = rel.titleJapanese
                        ))
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
        )
    }


        if (activeGalleryItems.isNotEmpty()) {
            KitsugiImageGalleryDialog(
                galleryItems = activeGalleryItems,
                initialIndex = activeGalleryIndex,
                title = entry.title,
                onDismiss = { activeGalleryItems = emptyList() }
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
                seasonNumber = ep.seasonNumber ?: targetSeason,
                onDismiss = { activeEpisodeForOptions = null }
            )
        }

        if (showIntegrationsDialog && settingsDataStore != null && settingsState != null) {
            DetailIntegrationsSettingsDialog(
                settingsDataStore = settingsDataStore,
                settingsState = settingsState,
                coroutineScope = coroutineScope,
                onDismiss = { showIntegrationsDialog = false }
            )
        }
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


