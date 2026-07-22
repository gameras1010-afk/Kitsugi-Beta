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
    settingsDataStore: SettingsDataStore? = null
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

    val displayResult = remember(result, detailState) {
        val detail = detailState
        if (detail != null) {
            result.copy(
                title = if (result.title.isBlank() || result.title == "Yükleniyor...") (detail.title ?: result.title) else result.title,
                imageUrl = result.imageUrl ?: detail.imageUrl,
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
    val isConnected = if (isSourceAniList) isAniListConnected else isMalConnected

    // State for tabs
    val isAnime = result.type == MediaType.Anime
    val hasTvEpisodes = isAnime || result.type == MediaType.TvShow
    val tabs = buildList {
        addAll(listOf("Bilgi", "Karakterler", "Ekip", "Öneriler", "İlişkiler", "Grafikler", "Yorumlar"))
        if (hasTvEpisodes) add("Bölümler")
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val selectedTab = pagerState.currentPage

    var activeEpisodeForOptions by remember { mutableStateOf<KitsugiStreamingEpisode?>(null) }
    var showWatchDialog by remember { mutableStateOf(false) }
    var watchEpisodeInput by remember { mutableStateOf("") }
    var showWatchStreamSelector by remember { mutableStateOf<Int?>(null) }
    val allImages = remember(displayResult.imageUrl, detailState?.pictures) {
        buildList {
            if (!displayResult.imageUrl.isNullOrBlank()) {
                add(displayResult.imageUrl)
            }
            detailState?.pictures?.let { addAll(it) }
        }.distinct()
    }
    var activeGalleryImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeGalleryIndex by remember { mutableStateOf(0) }

    val listState = rememberLazyListState()
    val tabListState = rememberLazyListState()
    val density = LocalDensity.current
    // TV odak highway — sol panel ↔ sağ panel tab bar
    val leftPanelFocusRequester = remember { FocusRequester() }
    val tabBarFocusRequester = remember { FocusRequester() }

    // Call loadTab when tab changes
    LaunchedEffect(result.source, result.malId, result.type, selectedTab, displayResult.realMalId) {
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
        if (detailLoading) {
            KitsugiCinematicLoadingScreen(
                title = displayResult.title,
                imageUrl = displayResult.imageUrl,
                onBackClick = onBackClick,
                logoUrl = if (showAnimeLogos) logoUrl else null,
                isAdult = displayResult.isAdult,
                blurAdultMedia = blurAdultMedia
            )
        } else {
            val pullRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = detailLoading,
                onRefresh = { viewModel.loadResult(displayResult, showAnimeLogos, forceRefresh = true) },
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
                                    .weight(0.38f)
                                    .fillMaxSize()
                                    .then(if (isTvDevice) Modifier.dpadVerticalFastScroll(leftScrollState) else Modifier)
                                    .verticalScroll(leftScrollState)
                            ) {
                            KitsugiDetailHero(
                                title = displayResult.getDisplayTitle(titleLanguage),
                                subtitle = displayResult.subtitle,
                                imageUrl = displayResult.imageUrl,
                                logoUrl = if (showAnimeLogos) logoUrl else null,
                                source = displayResult.source,
                                typeLabel = when (displayResult.type) {
                                    MediaType.Anime -> "ANIME"
                                    MediaType.Movie -> "FİLM"
                                    MediaType.TvShow -> "DİZİ"
                                    else -> "MANGA"
                                },
                                year = displayResult.year?.toString(),
                                isAdult = displayResult.isAdult,
                                onBackClick = onBackClick,
                                blurAdultMedia = blurAdultMedia,
                                onPosterClick = {
                                    val posterUrl = displayResult.imageUrl
                                    val idx = if (posterUrl != null) allImages.indexOf(posterUrl).coerceAtLeast(0) else 0
                                     activeGalleryImages = allImages
                                     activeGalleryIndex = idx
                                },
                                scoreLabel = if (!hideScores) displayResult.getDisplayScore(scoreFormat, hideScores) else null,
                                alreadyInList = existingEntry != null,
                                totalEpisodes = displayResult.total,
                                nextAiring = detailState?.nextAiringEpisode
                            )
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Spacer(modifier = Modifier.height(12.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (displayResult.type == MediaType.Anime || displayResult.type == MediaType.TvShow || displayResult.type == MediaType.Movie) {
                                        val accentColor = LocalKitsugiAccent.current
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(accentColor)
                                                .focusRequester(leftPanelFocusRequester)
                                                .focusProperties { right = tabBarFocusRequester }
                                                .tvClickable(shape = RoundedCornerShape(18.dp)) {
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
                                                        episode = 1,
                                                        isMovie = displayResult.type == MediaType.Movie,
                                                        season = 1,
                                                        title = displayResult.title,
                                                        posterUrl = displayResult.imageUrl,
                                                        titleEnglish = displayResult.titleEnglish,
                                                        titleRomaji = detailState?.titleRomaji,
                                                        titleNative = detailState?.titleNative,
                                                        startYear = displayResult.year
                                                    )
                                                }
                                                .padding(vertical = 14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PlayArrow,
                                                    contentDescription = null,
                                                    tint = KitsugiColors.Background
                                                )
                                                androidx.compose.material3.Text(
                                                    text = "İzle",
                                                    color = KitsugiColors.Background,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    if (displayResult.type == MediaType.Manga && onReadMangaClick != null) {
                                        val accentColor = LocalKitsugiAccent.current
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(accentColor)
                                                .focusRequester(leftPanelFocusRequester)
                                                .focusProperties { right = tabBarFocusRequester }
                                                .tvClickable(shape = RoundedCornerShape(18.dp), onClick = onReadMangaClick)
                                                .padding(vertical = 14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoStories,
                                                    contentDescription = null,
                                                    tint = KitsugiColors.Background
                                                )
                                                androidx.compose.material3.Text(
                                                    text = "Oku",
                                                    color = KitsugiColors.Background,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    val fallbackFocusMod = if (displayResult.type != MediaType.Anime && displayResult.type != MediaType.TvShow && displayResult.type != MediaType.Movie && displayResult.type != MediaType.Manga) {
                                        Modifier.focusRequester(leftPanelFocusRequester)
                                    } else Modifier

                                    if (existingEntry != null) {
                                        ApiActionButton(
                                            text = "✎ Düzenle",
                                            primary = true,
                                            enabled = true,
                                            modifier = fallbackFocusMod.focusProperties { right = tabBarFocusRequester },
                                            onClick = { onEditClick(existingEntry) }
                                        )
                                    } else {
                                        ApiActionButton(
                                            text = "Listeye Ekle",
                                            primary = true,
                                            enabled = true,
                                            modifier = fallbackFocusMod.focusProperties { right = tabBarFocusRequester },
                                            onClick = {
                                                if (isConnected) {
                                                    onAddClick(
                                                        ApiSearchSelection(
                                                            result = displayResult,
                                                            synopsis = synopsisForSave
                                                        )
                                                    )
                                                } else {
                                                    showAuthWarningDialog = true
                                                }
                                            }
                                        )
                                    }

                                    if (externalUrl != null) {
                                        val sourceLinkLabel = when (displayResult.source.lowercase()) {
                                            "anilist" -> "AniList'te Aç"
                                            "jikan", "mal" -> "MAL'da Gör"
                                            "tmdb" -> "TMDB'de Aç"
                                            else -> "Kaynakta Aç"
                                        }
                                        ApiActionButton(
                                            text = sourceLinkLabel,
                                            primary = false,
                                            enabled = true,
                                            modifier = Modifier.focusProperties { right = tabBarFocusRequester },
                                            onClick = { uriHandler.openUri(externalUrl) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                        }

                        // Sağ Panel
                        Column(
                            modifier = Modifier
                                .weight(0.62f)
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
                                                    } else null
                                                )
                                            }
                                            1 -> CharactersTabContent(state = charactersState, onCharacterClick = onCharacterClick, onStaffClick = onStaffClick)
                                            2 -> StaffTabContent(state = staffState, onStaffClick = onStaffClick)
                                            3 -> RecommendationsTabContent(state = recommendationsState, titleLanguage = titleLanguage, blurAdultMedia = blurAdultMedia, onRecommendationClick = { rel ->
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
                                            4 -> RelationsTabContent(state = relationsState, titleLanguage = titleLanguage, blurAdultMedia = blurAdultMedia, onRelationClick = { rel ->
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
                                            5 -> StatsTabContent(state = statsState)
                                            6 -> ReviewsTabContent(
                                                state = reviewsState,
                                                source = result.source,
                                                externalId = result.malId,
                                                mediaType = result.type,
                                                apiClient = apiClient,
                                                titleLanguage = titleLanguage,
                                                 preferredTranslator = settingsState?.preferredTranslator ?: "DEFAULT"
                                            )
                                            7 -> {
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
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ── PORTRAIT ──
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                        .fillMaxSize()
                        .background(KitsugiColors.Background)
                ) {
                    // Hero
                    item(key = "hero") {
                        KitsugiDetailHero(
                            title = displayResult.getDisplayTitle(titleLanguage),
                            subtitle = displayResult.subtitle,
                            imageUrl = displayResult.imageUrl,
                            logoUrl = if (showAnimeLogos) logoUrl else null,
                            source = displayResult.source,
                            typeLabel = when (displayResult.type) {
                                    MediaType.Anime -> "ANIME"
                                    MediaType.Movie -> "FİLM"
                                    MediaType.TvShow -> "DİZİ"
                                    else -> "MANGA"
                                },
                            year = displayResult.year?.toString(),
                            isAdult = displayResult.isAdult,
                            onBackClick = onBackClick,
                            blurAdultMedia = blurAdultMedia,
                            onPosterClick = {
                                val posterUrl = displayResult.imageUrl
                                val idx = if (posterUrl != null) allImages.indexOf(posterUrl).coerceAtLeast(0) else 0
                                activeGalleryImages = allImages
                                activeGalleryIndex = idx
                            },
                            scoreLabel = if (!hideScores) displayResult.getDisplayScore(scoreFormat, hideScores) else null,
                            alreadyInList = existingEntry != null,
                            totalEpisodes = displayResult.total,
                            nextAiring = detailState?.nextAiringEpisode
                        )
                    }

                    // Butonlar + İstatistikler
                    item(key = "info") {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Spacer(modifier = Modifier.height(18.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (displayResult.type == MediaType.Anime || displayResult.type == MediaType.TvShow || displayResult.type == MediaType.Movie) {
                                    val accentColor = LocalKitsugiAccent.current
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(accentColor)
                                            .tvClickable(shape = RoundedCornerShape(18.dp)) {
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
                                                    episode = 1,
                                                    season = 1,
                                                    isMovie = displayResult.type == MediaType.Movie,
                                                    title = displayResult.title,
                                                    posterUrl = displayResult.imageUrl,
                                                    titleEnglish = displayResult.titleEnglish,
                                                    titleRomaji = detailState?.titleRomaji,
                                                    titleNative = detailState?.titleNative,
                                                    startYear = displayResult.year
                                                )
                                            }
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = null,
                                                tint = KitsugiColors.Background
                                            )
                                            androidx.compose.material3.Text(
                                                text = "İzle",
                                                color = KitsugiColors.Background,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                if (displayResult.type == MediaType.Manga && onReadMangaClick != null) {
                                    val accentColor = LocalKitsugiAccent.current
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(accentColor)
                                            .tvClickable(shape = RoundedCornerShape(18.dp), onClick = onReadMangaClick)
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoStories,
                                                contentDescription = null,
                                                tint = KitsugiColors.Background
                                            )
                                            androidx.compose.material3.Text(
                                                text = "Oku",
                                                color = KitsugiColors.Background,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                if (existingEntry != null) {
                                    // Listede var — "Düzenle" butonu
                                    ApiActionButton(
                                        text = "✎ Düzenle",
                                        primary = true,
                                        enabled = true,
                                        onClick = { onEditClick(existingEntry) }
                                    )
                                } else {
                                    // Listede yok — "Listeye Ekle" butonu
                                    ApiActionButton(
                                        text = "Listeye Ekle",
                                        primary = true,
                                        enabled = true,
                                        onClick = {
                                            if (isConnected) {
                                                onAddClick(
                                                    ApiSearchSelection(
                                                        result = displayResult,
                                                        synopsis = synopsisForSave
                                                     )
                                                 )
                                            } else {
                                                 showAuthWarningDialog = true
                                            }
                                        }
                                    )
                                }

                                if (externalUrl != null) {
                                    val sourceLinkLabel = when (displayResult.source.lowercase()) {
                                        "anilist" -> "AniList'te Aç"
                                        "jikan", "mal" -> "MAL'da Gör"
                                        "tmdb" -> "TMDB'de Aç"
                                        else -> "Kaynakta Aç"
                                    }
                                    ApiActionButton(
                                        text = sourceLinkLabel,
                                        primary = false,
                                        enabled = true,
                                        onClick = { uriHandler.openUri(externalUrl) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
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
                                        text = displayResult.title,
                                        color = KitsugiColors.TextPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
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
                                            } else null
                                        )
                                    }
                                    1 -> CharactersTabContent(state = charactersState, onCharacterClick = onCharacterClick, onStaffClick = onStaffClick)
                                    2 -> StaffTabContent(state = staffState, onStaffClick = onStaffClick)
                                    3 -> RecommendationsTabContent(state = recommendationsState, titleLanguage = titleLanguage, blurAdultMedia = blurAdultMedia, onRecommendationClick = { rel ->
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
                                    4 -> RelationsTabContent(state = relationsState, titleLanguage = titleLanguage, blurAdultMedia = blurAdultMedia, onRelationClick = { rel ->
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
                                    5 -> StatsTabContent(state = statsState)
                                    6 -> ReviewsTabContent(
                                        state = reviewsState,
                                        source = result.source,
                                        externalId = result.malId,
                                        mediaType = result.type,
                                        apiClient = apiClient,
                                        titleLanguage = titleLanguage,
                                                 preferredTranslator = settingsState?.preferredTranslator ?: "DEFAULT"
                                    )
                                    7 -> {
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
                        }
                    }
                        Spacer(modifier = Modifier.height(90.dp))
                    } // end item
                } // end LazyColumn
            } // end portrait Box
        } // end portrait else
            } // end PullToRefreshBox
    } // end else (non-loading)

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
                                season = 1,
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

    if (activeGalleryImages.isNotEmpty()) {
        KitsugiImageGalleryDialog(
            imageUrls = activeGalleryImages,
            initialIndex = activeGalleryIndex,
            title = displayResult.title,
            onDismiss = { activeGalleryImages = emptyList() }
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
            onDismiss = { showIntegrationsDialog = false }
        )
    }

    } // KitsugiPageEnter
} // ApiResultDetailPage

/**
 * Kaynak platforma göre doğru URL oluşturur.
 * - MAL/Jikan → myanimelist.net/anime|manga/{malId}
 * - AniList   → anilist.co/anime|manga/{aniListId}  (malId >= 100_000_000 ise offset'ten çıkar)
 * - TMDB      → themoviedb.org/movie|tv/{tmdbId}
 * - Diğer     → null
 */
private fun buildExternalUrl(result: JikanSearchResult): String? {
    return when (result.source.lowercase()) {
        "jikan", "mal" -> {
            val id = result.malId
            when (result.type) {
                MediaType.Anime -> "https://myanimelist.net/anime/$id"
                MediaType.Manga -> "https://myanimelist.net/manga/$id"
                else -> null
            }
        }

        "anilist" -> {
            // malId >= 100_000_000 → AniList ID offset'li gömülü
            val rawId = result.malId
            val aniListId = if (rawId >= 100_000_000) rawId - 100_000_000 else rawId
            if (aniListId > 0) {
                when (result.type) {
                    MediaType.Anime -> "https://anilist.co/anime/$aniListId"
                    MediaType.Manga -> "https://anilist.co/manga/$aniListId"
                    else -> null
                }
            } else null
        }

        "tmdb" -> {
            val tmdbId = result.tmdbId ?: result.malId
            when (result.type) {
                MediaType.Movie -> "https://www.themoviedb.org/movie/$tmdbId"
                MediaType.TvShow -> "https://www.themoviedb.org/tv/$tmdbId"
                else -> null
            }
        }

        else -> null
    }
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