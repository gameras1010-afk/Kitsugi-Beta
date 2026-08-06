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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.wrapContentHeight
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.kitsugi.animelist.utils.parseToMediaType
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.remote.DetailCache
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayScore
import com.kitsugi.animelist.data.remote.KitsugiCharacter
import com.kitsugi.animelist.data.remote.KitsugiEpisodeRatingsRepository
import com.kitsugi.animelist.data.remote.KitsugiMediaDetail
import com.kitsugi.animelist.data.remote.KitsugiRelation
import com.kitsugi.animelist.data.remote.KitsugiReview
import com.kitsugi.animelist.data.remote.KitsugiScoreStat
import com.kitsugi.animelist.data.remote.KitsugiStaff
import com.kitsugi.animelist.data.remote.KitsugiStats
import com.kitsugi.animelist.data.remote.KitsugiStudio
import com.kitsugi.animelist.data.remote.KitsugiStreamingEpisode
import com.kitsugi.animelist.data.local.TranslationManager
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.ui.components.KitsugiPageEnter
import com.kitsugi.animelist.ui.components.KitsugiEpisodeOptionsDialog
import com.kitsugi.animelist.ui.components.KitsugiCinematicLoadingScreen
import com.kitsugi.animelist.ui.components.KitsugiImageGalleryDialog
import com.kitsugi.animelist.data.remote.GalleryItem
import com.kitsugi.animelist.data.remote.GalleryCategory
import com.kitsugi.animelist.ui.components.KitsugiStreamSelectorBottomSheet
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiFullscreenPlayerActivity
import com.kitsugi.animelist.ui.screens.stream.KitsugiStreamActivity
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.ui.components.KitsugiIntegrationsSettingsDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApiResultDetailPage(
    result: JikanSearchResult,
    existingEntry: MediaEntry?,
    onBackClick: () -> Unit,
    onAddClick: (ApiSearchSelection) -> Unit,
    onEditClick: (MediaEntry) -> Unit = {},
    onRelationClick: (JikanSearchResult) -> Unit,
    onCharacterClick: (KitsugiCharacter) -> Unit,
    onStaffClick: (Int, String, String?, String?) -> Unit,
    onStudioClick: (Int, String, String?, String?) -> Unit,
    onSearchQuery: (String) -> Unit = {},
    onSearchByGenre: (String) -> Unit = {},
    onSearchByTag: (String) -> Unit = {},
    titleLanguage: String = "ROMAJI",
    scoreFormat: String = "POINT_10",
    hideScores: Boolean = false,
    showAnimeLogos: Boolean = false,
    isAniListConnected: Boolean = false,
    isMalConnected: Boolean = false,
    isSimklConnected: Boolean = false,
    onLoginAniList: () -> Unit = {},
    onLoginMal: () -> Unit = {},
    onLoginSimkl: () -> Unit = {},
    onReadMangaClick: (() -> Unit)? = null,
    mdbListShowImdb: Boolean = true,
    mdbListShowTomatoes: Boolean = true,
    mdbListShowMetacritic: Boolean = true,
    mdbListShowAudience: Boolean = false,
    mdbListShowLetterboxd: Boolean = false,
    mdbListShowTmdb: Boolean = false,
    mdbListShowTrakt: Boolean = false,
    settingsDataStore: SettingsDataStore? = null,
    onUserProfileClick: (userId: Int, username: String, avatarUrl: String?) -> Unit = { _, _, _ -> },
    onToggleFavoriteClick: ((ApiSearchSelection) -> Unit)? = null
) {
    val accentColor = LocalKitsugiAccent.current
    val isTv = LocalIsTv.current
    val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember { JikanApiClient() }

    val settingsState = settingsDataStore?.settingsFlow?.collectAsState(initial = AppSettings())?.value
    val blurAdultMedia = settingsState?.blurAdultMedia ?: false
    var showIntegrationsDialog by remember { mutableStateOf(false) }
    var showAuthWarningDialog by remember { mutableStateOf(false) }

    // Obtain ViewModel
    val viewModel: ApiResultDetailViewModel = viewModel(key = "api_${result.source}_${result.malId}_${result.type.name}")

    // Load result in ViewModel
    LaunchedEffect(result.source, result.malId, result.type, showAnimeLogos) {
        viewModel.loadResult(result, showAnimeLogos)
    }

    // Collect states from ViewModel
    val detailState by viewModel.detailState.collectAsState()
    val detailLoading by viewModel.detailLoading.collectAsState()
    val translatedSynopsis by viewModel.translatedSynopsis.collectAsState()
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
    val mdbListRatings by viewModel.mdbListRatings.collectAsState()
    val mdbListLoading by viewModel.mdbListLoading.collectAsState()
    val galleryLoading by viewModel.galleryLoading.collectAsState()
    val pageResetTrigger by viewModel.pageResetTrigger.collectAsState()

    // Sadece detay verisi hazır olana kadar yükleme ekranı göster.
    // Galeri (fanart.tv) arka planda yüklenirken sayfa zaten açık kalır.
    val isLoading = detailLoading

    val displayResult = remember(result, detailState) {
        val detail = detailState
        if (detail != null) {
            result.copy(
                title = if (result.title.isBlank() || result.title == "Yükleniyor...") (detail.title ?: result.title) else result.title,
                imageUrl = if (!detail.imageUrl.isNullOrBlank()) detail.imageUrl else result.imageUrl,
                score = result.score ?: detail.score,
                year = result.year ?: detail.year,
                total = result.total ?: detail.total,
                isAdult = result.isAdult || detail.isAdult,
                realMalId = result.realMalId ?: detail.realMalId
            )
        } else {
            result
        }
    }

    val externalUrl = buildExternalUrl(displayResult)
    val malCrossUrl = buildMalCrossUrl(displayResult)

    val isSourceAniList = remember(displayResult) {
        displayResult.source.equals("anilist", ignoreCase = true)
    }
    val isConnected = remember(displayResult, isAniListConnected, isMalConnected, isSimklConnected) {
        val src = displayResult.source.lowercase()
        val hasRealMalId = displayResult.realMalId != null || (displayResult.malId != null && displayResult.malId > 0 && displayResult.malId < 100_000_000)
        when {
            displayResult.type == MediaType.TvShow || displayResult.type == MediaType.Movie -> isSimklConnected
            src == "anilist" -> {
                isAniListConnected || (isMalConnected && hasRealMalId)
            }
            src == "mal" || src == "jikan" -> {
                isMalConnected || isAniListConnected
            }
            src == "simkl" -> {
                isSimklConnected
            }
            else -> true
        }
    }

    val showFavouriteButton = isSourceAniList || isAniListConnected
    val isFavorite = existingEntry?.isFavorite ?: (detailState?.isFavourite ?: false)

    // State for tabs
    val isAnime = result.type == MediaType.Anime
    val hasTvEpisodes = isAnime || result.type == MediaType.TvShow
    val tabs = buildList {
        addAll(listOf("Bilgi", "Resimler", "Karakterler", "Ekip", "Öneriler", "İlişkiler", "Grafikler", "Yorumlar"))
        if (hasTvEpisodes) add("Bölümler")
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val selectedTab = pagerState.currentPage

    var activeEpisodeForOptions by remember { mutableStateOf<KitsugiStreamingEpisode?>(null) }
    var showWatchDialog by remember { mutableStateOf(false) }
    var watchEpisodeInput by remember { mutableStateOf("") }
    var showWatchStreamSelector by remember { mutableStateOf<Int?>(null) }

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

    val allImages = remember(displayResult.imageUrl, detailState?.pictures) {
        buildList {
            if (!displayResult.imageUrl.isNullOrBlank()) {
                add(displayResult.imageUrl)
            }
            detailState?.pictures?.let { addAll(it) }
        }.distinct()
    }
    var activeGalleryItems by remember { mutableStateOf<List<GalleryItem>>(emptyList()) }
    var activeGalleryIndex by remember { mutableStateOf(0) }
    val galleryItems by viewModel.galleryItems.collectAsState()

    val listState = rememberLazyListState()
    val tabListState = rememberLazyListState()
    val density = LocalDensity.current
    // TV odak highway — sol panel ↔ sağ panel tab bar
    val leftPanelFocusRequester = remember { FocusRequester() }
    val tabBarFocusRequester = remember { FocusRequester() }

    // Yeni bir sonuç'a geçildiğinde tab'ı sıfırla — geri tuşuyla dönüşlerde SIFIRLAMAZ.
    var lastProcessedTrigger by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(pageResetTrigger) {
        if (pageResetTrigger > 0 && pageResetTrigger != lastProcessedTrigger) {
            lastProcessedTrigger = pageResetTrigger
            pagerState.scrollToPage(0)
        }
    }

    // Call loadTab when tab changes
    LaunchedEffect(
        result.source,
        result.malId,
        result.type,
        selectedTab,
        displayResult.realMalId,
        detailState?.tmdbId,
        resolvedTmdbId,
        detailState == null
    ) {
        viewModel.loadTab(selectedTab, result, displayResult.realMalId)
    }

    LaunchedEffect(detailLoading) {
        if (!detailLoading) {
            try {
                leftPanelFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus request failures if views are not yet attached
            }
        }
    }

    val synopsisForSave = detailState?.synopsis
    // Use translated synopsis if available, fallback to raw
    val displaySynopsis = translatedSynopsis ?: detailState?.synopsis

    KitsugiPageEnter {
        DetailPageScaffold(
            title = displayResult.title,
            isLoading = isLoading,
            isError = !detailLoading && detailState == null,
            isRefreshing = detailLoading,
            onRefresh = { viewModel.loadResult(displayResult, showAnimeLogos, forceRefresh = true) },
            onBackClick = onBackClick,
            tabs = tabs,
            pagerState = pagerState,
            listState = listState,
            tabListState = tabListState,
            loadingScreen = {
                KitsugiCinematicLoadingScreen(
                    title = displayResult.title,
                    imageUrl = displayResult.imageUrl,
                    onBackClick = onBackClick,
                    logoUrl = if (showAnimeLogos) logoUrl else null,
                    isAdult = displayResult.isAdult,
                    blurAdultMedia = blurAdultMedia
                )
            },
            errorScreen = {
                DataUnavailableScreen(
                    title = displayResult.title,
                    onBackClick = onBackClick,
                    onRetryClick = { viewModel.loadResult(result, showAnimeLogos) }
                )
            },
            leftPanel = { lpFocus, tbFocus ->
                ApiDetailLeftPanel(
                    displayResult = displayResult,
                    detailState = detailState,
                    resolvedTmdbId = resolvedTmdbId,
                    galleryItems = galleryItems,
                    existingEntry = existingEntry,
                    isConnected = isConnected,
                    isFavorite = isFavorite,
                    showFavouriteButton = showFavouriteButton,
                    synopsisForSave = synopsisForSave,
                    titleLanguage = titleLanguage,
                    scoreFormat = scoreFormat,
                    hideScores = hideScores,
                    showAnimeLogos = showAnimeLogos,
                    logoUrl = logoUrl,
                    blurAdultMedia = blurAdultMedia,
                    onBackClick = onBackClick,
                    onAddClick = onAddClick,
                    onEditClick = onEditClick,
                    onToggleFavoriteClick = onToggleFavoriteClick,
                    onReadMangaClick = onReadMangaClick,
                    onGalleryOpen = { items, idx ->
                        activeGalleryItems = items
                        activeGalleryIndex = idx
                    },
                    onShowAuthWarning = { showAuthWarningDialog = true },
                    leftPanelFocusRequester = lpFocus,
                    tabBarFocusRequester = tbFocus,
                    targetSeason = targetSeason
                )
            },
            floatingHeaderActions = {
                IconButton(onClick = {
                    val url = buildExternalUrl(displayResult)
                    if (!url.isNullOrBlank()) {
                        com.kitsugi.animelist.utils.ShareUtils.shareText(context, displayResult.title, url)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Paylaş",
                        tint = KitsugiColors.TextSecondary
                    )
                }
                if (showFavouriteButton && onToggleFavoriteClick != null) {
                    IconButton(onClick = {
                        onToggleFavoriteClick(ApiSearchSelection(result = displayResult, synopsis = synopsisForSave))
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isFavorite) "Favoriden Çıkar" else "Favori Yap",
                            tint = if (isFavorite) accentColor else KitsugiColors.TextSecondary
                        )
                    }
                }
            },
            portraitTopItems = { _, lpFocus, tbFocus ->
                item(key = "hero") {
                    ApiDetailLeftPanel(
                        displayResult = displayResult,
                        detailState = detailState,
                        resolvedTmdbId = resolvedTmdbId,
                        galleryItems = galleryItems,
                        existingEntry = existingEntry,
                        isConnected = isConnected,
                        isFavorite = isFavorite,
                        showFavouriteButton = showFavouriteButton,
                        synopsisForSave = synopsisForSave,
                        titleLanguage = titleLanguage,
                        scoreFormat = scoreFormat,
                        hideScores = hideScores,
                        showAnimeLogos = showAnimeLogos,
                        logoUrl = logoUrl,
                        blurAdultMedia = blurAdultMedia,
                        onBackClick = onBackClick,
                        onAddClick = onAddClick,
                        onEditClick = onEditClick,
                        onToggleFavoriteClick = onToggleFavoriteClick,
                        onReadMangaClick = onReadMangaClick,
                        onGalleryOpen = { items, idx ->
                            activeGalleryItems = items
                            activeGalleryIndex = idx
                        },
                        onShowAuthWarning = { showAuthWarningDialog = true },
                        leftPanelFocusRequester = lpFocus,
                        tabBarFocusRequester = tbFocus,
                        targetSeason = targetSeason
                    )
                }
            },
            pageContent = { page ->
                when (page) {
                    0 -> {
                        ApiDetailOverviewTab(
                            result = result,
                            detail = detailState,
                            displaySynopsis = displaySynopsis,
                            isDetailLoading = detailLoading,
                            isTranslating = (translatedSynopsis == null && detailState?.synopsis != null),
                            onSearchQuery = onSearchQuery,
                            onStudioClick = onStudioClick,
                            onGenreClick = onSearchByGenre,
                            onTagClick = onSearchByTag,
                            onTranslateClick = {
                                val raw = detailState?.synopsis
                                if (!raw.isNullOrBlank()) context.openTranslator(raw, settingsState?.preferredTranslator ?: "DEFAULT")
                            },
                            onCopyClick = {
                                val text = displaySynopsis ?: detailState?.synopsis
                                if (!text.isNullOrBlank()) {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("synopsis", text))
                                    android.widget.Toast.makeText(context, "Panoya kopyalandı", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
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
                                    GalleryItem(
                                        url = url,
                                        category = GalleryCategory.POSTER,
                                        source = displayResult.source
                                    )
                                }
                                activeGalleryIndex = index
                            },
                            galleryItems = galleryItems,
                            galleryLoading = galleryLoading,
                            onGalleryItemRequest = { items, idx ->
                                activeGalleryItems = items
                                activeGalleryIndex = idx
                            }
                        )
                    }
                    1 -> {
                        // Resimler sekmesi — tüm galeri öğeleri (Fanart.tv, TMDB, AniList posterler)
                        Column(
                            modifier = androidx.compose.ui.Modifier.padding(horizontal = 0.dp, vertical = 14.dp)
                        ) {
                            if (galleryLoading && galleryItems.isEmpty()) {
                                DetailGalleryLoadingCard()
                            } else if (galleryItems.isNotEmpty()) {
                                DetailGalleryCard(
                                    items = galleryItems,
                                    onItemClick = { index ->
                                        activeGalleryItems = galleryItems
                                        activeGalleryIndex = index
                                    },
                                    onOpenGallery = { category ->
                                        val startIndex = if (category == null) 0
                                        else galleryItems.indexOfFirst { it.category == category }.coerceAtLeast(0)
                                        activeGalleryItems = galleryItems
                                        activeGalleryIndex = startIndex
                                    }
                                )
                            } else {
                                // Galeri yüklenirken veya boşken bilgi mesajı
                                androidx.compose.foundation.layout.Box(
                                    modifier = androidx.compose.ui.Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (galleryLoading) "Resimler yükleniyor..." else "Görsel bulunamadı",
                                        color = KitsugiColors.TextMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    2 -> CharactersTabContent(state = charactersState, onCharacterClick = onCharacterClick, onStaffClick = onStaffClick, onMediaClick = onMediaClick)
                    3 -> StaffTabContent(state = staffState, onStaffClick = onStaffClick)
                    4 -> RecommendationsTabContent(state = recommendationsState, titleLanguage = titleLanguage, blurAdultMedia = blurAdultMedia, onRecommendationClick = { rel ->
                        val typeLabel = when (rel.mediaType) {
                            MediaType.Anime -> "Anime"
                            MediaType.Movie -> "Film"
                            MediaType.TvShow -> "Dizi"
                            MediaType.Manga -> "Manga"
                        }
                        val relResult = JikanSearchResult(
                            malId = rel.malId,
                            title = rel.title,
                            subtitle = "${rel.relationType}, $typeLabel",
                            type = rel.mediaType,
                            total = null,
                            score = null,
                            isAdult = rel.isAdult,
                            imageUrl = rel.imageUrl,
                            year = null,
                            source = rel.source,
                            titleEnglish = rel.titleEnglish,
                            titleJapanese = rel.titleJapanese
                        )
                        onRelationClick(relResult)
                    })
                    5 -> RelationsTabContent(state = relationsState, titleLanguage = titleLanguage, blurAdultMedia = blurAdultMedia, onRelationClick = { rel ->
                        val typeLabel = when (rel.mediaType) {
                            MediaType.Anime -> "Anime"
                            MediaType.Movie -> "Film"
                            MediaType.TvShow -> "Dizi"
                            MediaType.Manga -> "Manga"
                        }
                        val relResult = JikanSearchResult(
                            malId = rel.malId,
                            title = rel.title,
                            subtitle = "${rel.relationType}, $typeLabel",
                            type = rel.mediaType,
                            total = null,
                            score = null,
                            isAdult = rel.isAdult,
                            imageUrl = rel.imageUrl,
                            year = null,
                            source = rel.source,
                            titleEnglish = rel.titleEnglish,
                            titleJapanese = rel.titleJapanese
                        )
                        onRelationClick(relResult)
                    })
                    6 -> StatsTabContent(state = statsState)
                    7 -> ReviewsTabContent(
                        state = reviewsState,
                        source = result.source,
                        externalId = result.malId,
                        mediaType = result.type,
                        apiClient = apiClient,
                        titleLanguage = titleLanguage,
                        onUserProfileClick = onUserProfileClick,
                        preferredTranslator = settingsState?.preferredTranslator ?: "DEFAULT"
                    )
                    8 -> {
                        ApiDetailEpisodesTab(
                            state = episodesState,
                            episodeRatings = episodeRatings,
                            targetSeason = targetSeason,
                            totalSeasons = detailState?.totalSeasons,
                            resolvedTmdbId = detailState?.tmdbId,
                            displayTitle = displayResult.title,
                            displaySource = displayResult.source,
                            displayMalId = displayResult.malId,
                            displayRealMalId = displayResult.realMalId,
                            displayImageUrl = displayResult.imageUrl,
                            displayTitleEnglish = displayResult.titleEnglish,
                            displayTitleRomaji = detailState?.titleRomaji,
                            displayTitleNative = detailState?.titleNative,
                            displayYear = displayResult.year,
                            isMovie = displayResult.type == MediaType.Movie,
                            onSeasonSelected = { newSeason ->
                                viewModel.setTargetSeason(newSeason, result, displayResult.realMalId)
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

    activeEpisodeForOptions?.let { ep ->
        val resultMalId = if (displayResult.source.lowercase() == "anilist") displayResult.realMalId else displayResult.malId
        val resultAniListId = if (displayResult.source.lowercase() == "anilist") displayResult.malId else null
        val resultTmdbId = detailState?.tmdbId ?: resolvedTmdbId
        KitsugiEpisodeOptionsDialog(
            animeTitle = displayResult.title,
            episodeNumber = ep.episodeNumber,
            episodeTitle = ep.title,
            originalUrl = ep.url,
            siteName = ep.site,
            malId = resultMalId,
            aniListId = resultAniListId,
            tmdbId = resultTmdbId,
            posterUrl = displayResult.imageUrl,
            titleEnglish = displayResult.titleEnglish,
            titleRomaji = detailState?.titleRomaji,
            titleNative = detailState?.titleNative,
            startYear = displayResult.year,
            isMovie = displayResult.type == MediaType.Movie,
            seasonNumber = ep.seasonNumber ?: targetSeason,
            onDismiss = { activeEpisodeForOptions = null }
        )
    }

    // İzle - bölüm numarası dialog
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
                    androidx.compose.material3.Text(
                        text = displayResult.title,
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
                    androidx.compose.material3.Text(
                        text = "Hangi bölümü izlemek istiyorsunuz?",
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = watchEpisodeInput,
                        onValueChange = { watchEpisodeInput = it.filter { c -> c.isDigit() } },
                        label = { androidx.compose.material3.Text("Bölüm Numarası") },
                        placeholder = { androidx.compose.material3.Text("Örn: 1") },
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
                            // AniList: malId = realMalId (from detailState), aniListId = decoded from stableId
                            val streamMalId = if (displayResult.source.lowercase() == "anilist") {
                                displayResult.realMalId
                            } else {
                                displayResult.malId
                            }
                            val rawStableId = if (displayResult.source.lowercase() == "anilist") displayResult.malId else null
                            val streamAniListId = rawStableId?.let {
                                if (it >= 100_000_000) it - 100_000_000 else it
                            }
                            KitsugiStreamActivity.start(
                                context = context,
                                malId = streamMalId,
                                aniListId = streamAniListId,
                                tmdbId = detailState?.tmdbId ?: resolvedTmdbId,
                                episode = epNum,
                                season = targetSeason,
                                isMovie = displayResult.type == MediaType.Movie,
                                title = displayResult.title,
                                posterUrl = displayResult.imageUrl,
                                titleEnglish = displayResult.titleEnglish,
                                titleRomaji = detailState?.titleRomaji,
                                titleNative = detailState?.titleNative,
                                startYear = displayResult.year
                            )
                        }
                    },
                    enabled = (epNum ?: 0) > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    androidx.compose.material3.Text("İzle", color = KitsugiColors.Background, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWatchDialog = false }) {
                    androidx.compose.material3.Text("İptal", color = KitsugiColors.TextSecondary)
                }
            }
        )
    }

    if (showAuthWarningDialog) {
        val platformName = if (isSourceAniList) "AniList" else "MyAnimeList"
        AlertDialog(
            onDismissRequest = { showAuthWarningDialog = false },
            title = {
                Text(
                    text = "Hesap Bağlantısı Gerekli",
                    color = KitsugiColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Bu içeriği listenize eklemek için aktif bir $platformName hesabınızın bağlı olması gerekmektedir. Yerel (çevrimdışı) liste kaydı oluşturma devre dışı bırakılmıştır. Şimdi hesabınızı bağlamak ister misiniz?",
                    color = KitsugiColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAuthWarningDialog = false
                        if (isSourceAniList) onLoginAniList() else onLoginMal()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = KitsugiColors.Background
                    )
                ) {
                    Text("Bağlan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAuthWarningDialog = false }
                ) {
                    Text("İptal", color = KitsugiColors.TextMuted)
                }
            },
            containerColor = KitsugiColors.Surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (activeGalleryItems.isNotEmpty()) {
        KitsugiImageGalleryDialog(
            galleryItems = activeGalleryItems,
            initialIndex = activeGalleryIndex,
            title = displayResult.title,
            onDismiss = { activeGalleryItems = emptyList() }
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
} // ApiResultDetailPage

/**
 * Kaynak platforma göre doğru URL oluşturur.
 * - MAL/Jikan → myanimelist.net/anime|manga/{malId}
 * - AniList   → anilist.co/anime|manga/{aniListId}  (malId >= 100_000_000 ise offset'ten çıkar)
 * - TMDB      → themoviedb.org/movie|tv/{tmdbId}
 * - Diğer     → null
 */
private fun buildExternalUrl(result: JikanSearchResult): String? {
    return com.kitsugi.animelist.utils.ShareUtils.buildExternalMediaUrl(result.source, result.malId, result.tmdbId, result.type)
}

// buildMalCrossUrl: artık kullanılmıyor (platform-specific buton mantığı buildExternalUrl'e taşındı)
@Suppress("unused")
private fun buildMalCrossUrl(result: JikanSearchResult): String? {
    if (result.source.lowercase() != "anilist") return null
    val realMalId = result.realMalId ?: return null
    return when (result.type) {
        MediaType.Anime -> "https://myanimelist.net/anime/$realMalId"
        MediaType.Manga -> "https://myanimelist.net/manga/$realMalId"
        else -> null
    }
}