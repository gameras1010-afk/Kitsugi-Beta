package com.kitsugi.animelist.ui.tv.detail

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.ui.platform.LocalContext
import com.kitsugi.animelist.ui.components.KitsugiShimmerAvatarRow
import com.kitsugi.animelist.ui.components.KitsugiShimmerMediaRow
import com.kitsugi.animelist.ui.components.KitsugiShimmerSearchResultList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.KitsugiCharacter
import com.kitsugi.animelist.data.remote.KitsugiRelation
import com.kitsugi.animelist.data.remote.KitsugiReview
import com.kitsugi.animelist.data.remote.KitsugiStaff
import com.kitsugi.animelist.data.remote.KitsugiStats
import com.kitsugi.animelist.data.remote.KitsugiStreamingEpisode
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.screens.detail.ApiResultDetailViewModel
import com.kitsugi.animelist.ui.screens.detail.DetailTabState
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import com.kitsugi.animelist.ui.tv.input.isSelectKey
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames
import com.kitsugi.animelist.ui.utils.tvClickable
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle
import com.kitsugi.animelist.ui.tv.focus.TvFocusRestoration.safeRequestFocus
import com.kitsugi.animelist.ui.tv.components.TvLoadingScreen

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TvDetailScreen(
    result: JikanSearchResult,
    existingEntry: MediaEntry?,
    onBackClick: () -> Unit,
    onPlayEpisodeClick: (episode: KitsugiStreamingEpisode, season: Int) -> Unit,
    onPlayMovieClick: () -> Unit,
    onToggleLibrary: () -> Unit,
    onNavigateToRelationDetail: (JikanSearchResult) -> Unit,
    onCharacterClick: (characterId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    onStaffClick: (staffId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    viewModel: ApiResultDetailViewModel = viewModel()
) {
    val accentColor = LocalKitsugiAccent.current

    LaunchedEffect(result.source, result.malId, result.type) {
        viewModel.loadResult(result, showAnimeLogos = true)
    }

    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val detailLoading by viewModel.detailLoading.collectAsStateWithLifecycle()
    val translatedSynopsis by viewModel.translatedSynopsis.collectAsStateWithLifecycle()
    val charactersState by viewModel.charactersState.collectAsStateWithLifecycle()
    val staffState by viewModel.staffState.collectAsStateWithLifecycle()
    val relationsState by viewModel.relationsState.collectAsStateWithLifecycle()
    val episodesState by viewModel.episodesState.collectAsStateWithLifecycle()
    val targetSeason by viewModel.targetSeason.collectAsStateWithLifecycle()
    val logoUrl by viewModel.logoUrl.collectAsStateWithLifecycle()
    val episodeRatings by viewModel.episodeRatings.collectAsStateWithLifecycle()
    val reviewsState by viewModel.reviewsState.collectAsStateWithLifecycle()
    val statsState by viewModel.statsState.collectAsStateWithLifecycle()

    LaunchedEffect(detailState) {
        if (detailState != null) {
            viewModel.loadTab(1, result, result.realMalId ?: detailState?.realMalId) // Characters
            viewModel.loadTab(2, result, result.realMalId ?: detailState?.realMalId) // Staff
            viewModel.loadTab(3, result, result.realMalId ?: detailState?.realMalId) // Relations
            viewModel.loadTab(4, result, result.realMalId ?: detailState?.realMalId) // Stats
            viewModel.loadTab(5, result, result.realMalId ?: detailState?.realMalId) // Reviews
            viewModel.loadTab(6, result, result.realMalId ?: detailState?.realMalId) // Episodes
        }
    }

    val displayResult = remember(result, detailState) {
        val detail = detailState
        if (detail != null) {
            result.copy(
                title = if (result.title == "Yükleniyor...") (detail.title ?: result.title) else result.title,
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

    val displayTitle = displayResult.getDisplayTitle("ROMAJI")
    val displaySynopsis = translatedSynopsis ?: detailState?.synopsis ?: ""
    val isMovie = displayResult.type == MediaType.Movie

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val trailerService = remember { com.kitsugi.animelist.data.trailer.TrailerServiceHolder.get(context) }
    val apiClient = remember { JikanApiClient() }

    var showTrailerOverlay by remember { mutableStateOf(false) }
    var trailerPlaybackSource by remember { mutableStateOf<com.kitsugi.animelist.data.trailer.TrailerPlaybackSource?>(null) }
    var trailerLoading by remember { mutableStateOf(false) }
    var trailerError by remember { mutableStateOf<String?>(null) }

    val loadAndShowTrailer = {
        val tmdbIdStr = displayResult.tmdbId?.toString() ?: detailState?.tmdbId?.toString()
        if (tmdbIdStr == null && displayResult.title.isBlank()) {
            trailerError = "Fragman bilgisi bulunamadı."
            showTrailerOverlay = true
        } else {
            showTrailerOverlay = true
            trailerLoading = true
            trailerError = null
            trailerPlaybackSource = null
            scope.launch {
                try {
                    val source = trailerService.getTrailerPlaybackSource(
                        title = displayResult.title,
                        year = displayResult.year?.toString(),
                        tmdbId = tmdbIdStr,
                        type = if (displayResult.type == MediaType.Movie) "movie" else "tv"
                    )
                    if (source != null && !source.videoUrl.isBlank()) {
                        trailerPlaybackSource = source
                    } else {
                        trailerError = "Fragman bulunamadı."
                    }
                } catch (e: Exception) {
                    trailerError = "Fragman yüklenirken hata oluştu: ${e.message}"
                } finally {
                    trailerLoading = false
                }
            }
        }
    }

    val playButtonFocusRequester = remember { FocusRequester() }
    val libraryButtonFocusRequester = remember { FocusRequester() }
    val trailerButtonFocusRequester = remember { FocusRequester() }
    val seasonTabsFocusRequester = remember { FocusRequester() }
    val episodesRowFocusRequester = remember { FocusRequester() }
    val charactersRowFocusRequester = remember { FocusRequester() }
    val staffRowFocusRequester = remember { FocusRequester() }
    val relationsRowFocusRequester = remember { FocusRequester() }
    val statsRowFocusRequester = remember { FocusRequester() }
    val reviewsRowFocusRequester = remember { FocusRequester() }

    // Track which focusable row sections are actually rendered/attached.
    // focusProperties must never reference a FocusRequester that has no
    // corresponding Modifier.focusRequester() node in the tree — doing so
    // causes an IllegalStateException crash on D-pad key events.
    val hasSeasonTabs = !isMovie && (detailState?.totalSeasons ?: 1) > 1
    val hasEpisodesRow = !isMovie &&
        episodesState is DetailTabState.Success &&
        ((episodesState as DetailTabState.Success<*>).data as? List<*>)?.isNotEmpty() == true
    val hasCharactersRow = charactersState is DetailTabState.Success &&
        ((charactersState as DetailTabState.Success<*>).data as? List<*>)?.isNotEmpty() == true
    val hasStaffRow = staffState is DetailTabState.Success &&
        ((staffState as DetailTabState.Success<*>).data as? List<*>)?.isNotEmpty() == true
    val hasRelationsRow = relationsState is DetailTabState.Success &&
        ((relationsState as DetailTabState.Success<*>).data as? List<*>)?.isNotEmpty() == true
    val hasStatsRow = statsState is DetailTabState.Success
    val hasReviewsRow = reviewsState is DetailTabState.Success &&
        ((reviewsState as DetailTabState.Success<*>).data as? List<*>)?.isNotEmpty() == true

    BackHandler {
        onBackClick()
    }

    val detailFocusRequester = remember { FocusRequester() }
    com.kitsugi.animelist.ui.tv.focus.TvGlobalFocusRecovery(fallbackFocusRequester = playButtonFocusRequester)

    LaunchedEffect(detailLoading) {
        if (!detailLoading) {
            playButtonFocusRequester.requestFocusAfterFrames(frames = 3)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
            .focusRequester(detailFocusRequester)
    ) {
        // 1. Fullscreen Backdrop with Gradients
        val backdropUrl = displayResult.backdropUrl ?: displayResult.imageUrl
        if (!backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.3f),
                contentScale = ContentScale.Crop
            )

            // Dynamic gradients to protect text legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                KitsugiColors.Background,
                                KitsugiColors.Background.copy(alpha = 0.9f),
                                KitsugiColors.Background.copy(alpha = 0.6f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = 1600f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                KitsugiColors.Background.copy(alpha = 0.4f),
                                KitsugiColors.Background
                            )
                        )
                    )
            )
        }

        if (detailLoading) {
            TvLoadingScreen()
        } else {
            val mainLazyListState = rememberLazyListState()

            CompositionLocalProvider(
                LocalBringIntoViewSpec provides KitsugiScrollDefaults.rememberTvCenteredSpec()
            ) {
                LazyColumn(
                    state = mainLazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .dpadVerticalFastScroll(scrollableState = mainLazyListState),
                    contentPadding = PaddingValues(
                        horizontal = KitsugiTvTokens.Spacing.screenHorizontal,
                        vertical = KitsugiTvTokens.Spacing.screenVertical
                    ),
                    verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.rowGap)
                ) {
                    // --- Hero Section ---
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .padding(top = KitsugiTvTokens.Spacing.contentPadding),
                            verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.md)
                        ) {
                            if (!logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = logoUrl,
                                    contentDescription = displayTitle,
                                    modifier = Modifier
                                        .height(90.dp)
                                        .fillMaxWidth(),
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.CenterStart
                                )
                            } else {
                                Text(
                                    text = displayTitle,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 36.sp
                                    ),
                                    color = KitsugiColors.TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Meta Badges Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BadgeText(
                                    text = when (displayResult.type) {
                                        MediaType.Anime -> "ANIME"
                                        MediaType.Movie -> "FİLM"
                                        MediaType.TvShow -> "DİZİ"
                                        else -> "MANGA"
                                    }
                                )
                                if (displayResult.year != null) {
                                    BadgeText(text = displayResult.year.toString())
                                }
                                if (displayResult.score != null && displayResult.score > 0) {
                                    BadgeText(text = "⭐ %.1f".format(displayResult.score.toDouble()))
                                }
                                if (displayResult.isAdult) {
                                    BadgeText(text = "18+", color = Color(0xFFD32F2F))
                                }
                            }

                            // Synopsis
                            Text(
                                text = displaySynopsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = KitsugiColors.TextSecondary,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 22.sp
                            )

                            // Genres + Studios
                            val genres = detailState?.genres.orEmpty()
                            val studios = detailState?.studios.orEmpty().filter { it.isMain }.map { it.name }
                            if (genres.isNotEmpty() || studios.isNotEmpty()) {
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    items(studios) { studio ->
                                        BadgeText(text = studio, color = accentColor)
                                    }
                                    items(genres.take(6)) { genre ->
                                        BadgeText(text = genre)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(KitsugiTvTokens.Spacing.sm))

                            // Action Buttons Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.contentPadding),
                                modifier = Modifier.focusGroup()
                            ) {
                                val totalSeasons = detailState?.totalSeasons ?: 1
                                val hasMultipleSeasons = totalSeasons > 1

                                TvActionButton(
                                    onClick = {
                                        if (isMovie) {
                                            com.kitsugi.animelist.core.player.TvTrailerPlayerPoolHolder.get(context).yield()
                                            onPlayMovieClick()
                                        } else {
                                            try {
                                                if (hasMultipleSeasons && hasSeasonTabs) {
                                                    seasonTabsFocusRequester.requestFocus()
                                                } else if (hasEpisodesRow) {
                                                    episodesRowFocusRequester.requestFocus()
                                                }
                                            } catch (_: IllegalStateException) {
                                                // FocusRequester henüz init edilmemiş — veri yüklenirken
                                                // güvenle görmezden gel
                                            }
                                        }
                                    },
                                    focusRequester = playButtonFocusRequester,
                                    text = if (isMovie) "Oynat" else "Bölüm Seçin",
                                    icon = Icons.Rounded.PlayArrow,
                                    backgroundColor = accentColor,
                                    textColor = Color.White,
                                    modifier = Modifier.focusProperties {
                                        right = libraryButtonFocusRequester
                                        // FocusRequester.Cancel: Compose focus sistemine
                                        // "bu yönde geçiş yapma" der ve uninit requester
                                        // crash'ini tamamen engeller.
                                        down = when {
                                            !isMovie && hasSeasonTabs  -> seasonTabsFocusRequester
                                            !isMovie && hasEpisodesRow -> episodesRowFocusRequester
                                            hasCharactersRow            -> charactersRowFocusRequester
                                            else                        -> FocusRequester.Cancel
                                        }
                                    }
                                )

                                TvActionButton(
                                    onClick = onToggleLibrary,
                                    focusRequester = libraryButtonFocusRequester,
                                    text = if (existingEntry != null) "Kütüphaneden Çıkar" else "Kütüphaneye Ekle",
                                    icon = Icons.Rounded.Bookmarks,
                                    backgroundColor = KitsugiColors.SurfaceStrong,
                                    textColor = KitsugiColors.TextPrimary,
                                    modifier = Modifier.focusProperties {
                                        left = playButtonFocusRequester
                                        right = trailerButtonFocusRequester
                                        down = when {
                                            !isMovie && hasSeasonTabs  -> seasonTabsFocusRequester
                                            !isMovie && hasEpisodesRow -> episodesRowFocusRequester
                                            hasCharactersRow            -> charactersRowFocusRequester
                                            else                        -> FocusRequester.Cancel
                                        }
                                    }
                                )

                                TvActionButton(
                                    onClick = { loadAndShowTrailer() },
                                    focusRequester = trailerButtonFocusRequester,
                                    text = "Fragman",
                                    icon = Icons.Rounded.Movie,
                                    backgroundColor = KitsugiColors.SurfaceStrong,
                                    textColor = KitsugiColors.TextPrimary,
                                    modifier = Modifier.focusProperties {
                                        left = libraryButtonFocusRequester
                                        down = when {
                                            !isMovie && hasSeasonTabs  -> seasonTabsFocusRequester
                                            !isMovie && hasEpisodesRow -> episodesRowFocusRequester
                                            hasCharactersRow            -> charactersRowFocusRequester
                                            else                        -> FocusRequester.Cancel
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // --- Season Selection Tabs (For multiple seasons) ---
                    val totalSeasons = detailState?.totalSeasons ?: 1
                    if (!isMovie && totalSeasons > 1) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm)) {
                                Text(
                                    text = "Sezonlar",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = KitsugiColors.TextPrimary
                                )

                                val seasonsList = remember(totalSeasons) { (1..totalSeasons).toList() }
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(seasonTabsFocusRequester)
                                        .focusRestorer()
                                        .focusGroup()
                                        .focusProperties {
                                            up = playButtonFocusRequester
                                            down = if (hasEpisodesRow) episodesRowFocusRequester else FocusRequester.Cancel
                                        },
                                    horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(seasonsList) { season ->
                                        val isSelected = season == targetSeason
                                        var isFocused by remember { mutableStateOf(false) }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (isSelected) accentColor else KitsugiColors.Surface)
                                                .border(
                                                    BorderStroke(
                                                        width = if (isFocused) KitsugiTvTokens.Cards.focusedBorderWidth else 0.dp,
                                                        color = if (isFocused) Color.White else Color.Transparent
                                                    ),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .onFocusChanged { isFocused = it.isFocused }
                                                .tvClickable(shape = RoundedCornerShape(20.dp)) {
                                                    viewModel.setTargetSeason(season, result, result.realMalId ?: detailState?.realMalId)
                                                }
                                                .padding(horizontal = 20.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = "${season}. Sezon",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSelected || isFocused) Color.White else KitsugiColors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- Episodes Section ---
                    if (!isMovie) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm)) {
                                Text(
                                    text = "Bölümler",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = KitsugiColors.TextPrimary
                                )

                                when (val epState = episodesState) {
                                    is DetailTabState.Loading -> {
                                        KitsugiShimmerMediaRow(cardCount = 4)
                                    }
                                    is DetailTabState.Success -> {
                                        val episodes = epState.data
                                        if (episodes.isEmpty()) {
                                            Text(
                                                text = "Bölüm bulunamadı.",
                                                color = KitsugiColors.TextMuted,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        } else {
                                            LazyRow(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .focusRequester(episodesRowFocusRequester)
                                                    .focusRestorer()
                                                    .focusGroup()
                                                    .focusProperties {
                                                        up = if (hasSeasonTabs) seasonTabsFocusRequester else playButtonFocusRequester
                                                        down = if (hasCharactersRow) charactersRowFocusRequester else FocusRequester.Cancel
                                                    },
                                                horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                itemsIndexed(episodes) { index, episode ->
                                                    val episodeNumber = episode.episodeNumber ?: (index + 1)
                                                    val isWatched = (existingEntry?.progress ?: 0) >= episodeNumber
                                                    val ratingKey = targetSeason to episodeNumber
                                                    val rating = episodeRatings[ratingKey]
                                                        ?: episodeRatings[1 to episodeNumber]
                                                    TvEpisodeCard(
                                                        episode = episode,
                                                        index = index,
                                                        accentColor = accentColor,
                                                        rating = rating,
                                                        isWatched = isWatched,
                                                        onClick = {
                                                            com.kitsugi.animelist.core.player.TvTrailerPlayerPoolHolder.get(context).yield()
                                                            onPlayEpisodeClick(episode, targetSeason)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }

                    // --- Characters Section ---
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm)) {
                            Text(
                                text = "Karakterler",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = KitsugiColors.TextPrimary
                            )

                            when (val charsState = charactersState) {
                                is DetailTabState.Loading -> {
                                    KitsugiShimmerAvatarRow(avatarCount = 6)
                                }
                                is DetailTabState.Success -> {
                                    val characters = charsState.data
                                    if (characters.isEmpty()) {
                                        Text(
                                            text = "Karakter bilgisi yok.",
                                            color = KitsugiColors.TextMuted,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        LazyRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(charactersRowFocusRequester)
                                                .focusRestorer()
                                                .focusGroup()
                                                .focusProperties {
                                                    up = if (!isMovie && hasEpisodesRow) episodesRowFocusRequester else playButtonFocusRequester
                                                    down = when {
                                                        hasStaffRow     -> staffRowFocusRequester
                                                        hasRelationsRow -> relationsRowFocusRequester
                                                        else            -> FocusRequester.Cancel
                                                    }
                                                },
                                            horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            items(characters) { char ->
                                                TvCharacterCard(
                                                    character = char,
                                                    onClick = {
                                                        onCharacterClick(char.id, displayResult.source, char.name, char.imageUrl)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {}
                             }
                        }
                    }

                    // --- Staff Section ---
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm)) {
                            Text(
                                text = "Yapım Ekibi",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = KitsugiColors.TextPrimary
                            )

                            when (val stfState = staffState) {
                                is DetailTabState.Loading -> {
                                    KitsugiShimmerAvatarRow(avatarCount = 6)
                                }
                                is DetailTabState.Success -> {
                                    val staffList = stfState.data
                                    if (staffList.isEmpty()) {
                                        Text(
                                            text = "Yapım ekibi bilgisi yok.",
                                            color = KitsugiColors.TextMuted,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        LazyRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(staffRowFocusRequester)
                                                .focusRestorer()
                                                .focusGroup()
                                                .focusProperties {
                                                    up = if (hasCharactersRow) charactersRowFocusRequester else playButtonFocusRequester
                                                    down = if (hasRelationsRow) relationsRowFocusRequester else FocusRequester.Cancel
                                                },
                                            horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            items(staffList) { staff ->
                                                TvStaffCard(
                                                    staff = staff,
                                                    onClick = {
                                                        onStaffClick(staff.id, displayResult.source, staff.name, staff.imageUrl)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }

                    // --- Relations Section ---
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm)) {
                            Text(
                                text = "İlişkili İçerikler",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = KitsugiColors.TextPrimary
                            )

                            when (val relsState = relationsState) {
                                is DetailTabState.Loading -> {
                                    KitsugiShimmerSearchResultList(itemCount = 3)
                                }
                                is DetailTabState.Success -> {
                                    val relations = relsState.data
                                    if (relations.isEmpty()) {
                                        Text(
                                            text = "İlişkili içerik bulunamadı.",
                                            color = KitsugiColors.TextMuted,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        LazyRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(relationsRowFocusRequester)
                                                .focusRestorer()
                                                .focusGroup()
                                                .focusProperties {
                                                    up = when {
                                                        hasStaffRow     -> staffRowFocusRequester
                                                        hasCharactersRow -> charactersRowFocusRequester
                                                        else            -> playButtonFocusRequester
                                                    }
                                                    down = when {
                                                        hasStatsRow    -> statsRowFocusRequester
                                                        hasReviewsRow  -> reviewsRowFocusRequester
                                                        else           -> FocusRequester.Cancel
                                                    }
                                                },
                                            horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.itemGap),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            items(relations) { rel ->
                                                TvRelationCard(
                                                    relation = rel,
                                                    onClick = {
                                                        val relResult = JikanSearchResult(
                                                            source = displayResult.source,
                                                            malId = rel.malId,
                                                            title = rel.title,
                                                            subtitle = "",
                                                            type = when (rel.mediaType) {
                                                                MediaType.Manga -> MediaType.Manga
                                                                MediaType.Movie -> MediaType.Movie
                                                                else -> MediaType.Anime
                                                            },
                                                            total = null,
                                                            score = null,
                                                            isAdult = false,
                                                            imageUrl = rel.imageUrl,
                                                            year = null
                                                        )
                                                        onNavigateToRelationDetail(relResult)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }

                    // --- Stats Section ---
                    if (statsState is DetailTabState.Success) {
                        val statsData = (statsState as DetailTabState.Success<KitsugiStats?>).data
                        if (statsData != null) {
                            item {
                                TvStatsSection(
                                    stats = statsData,
                                    source = displayResult.source,
                                    focusRequester = statsRowFocusRequester,
                                    focusUp = when {
                                        hasRelationsRow  -> relationsRowFocusRequester
                                        hasStaffRow      -> staffRowFocusRequester
                                        hasCharactersRow -> charactersRowFocusRequester
                                        else             -> playButtonFocusRequester
                                    },
                                    focusDown = if (hasReviewsRow) reviewsRowFocusRequester else FocusRequester.Cancel
                                )
                            }
                        }
                    }

                    // --- Reviews, Activities & Forum Topics Section ---
                    item {
                        TvReviewsTabContent(
                            state = reviewsState as? DetailTabState<List<KitsugiReview>>
                                ?: DetailTabState.Loading,
                            source = displayResult.source,
                            externalId = displayResult.malId ?: 0,
                            mediaType = displayResult.type,
                            apiClient = apiClient,
                            titleLanguage = "ROMAJI",
                            focusRequester = reviewsRowFocusRequester,
                            focusUp = when {
                                hasStatsRow     -> statsRowFocusRequester
                                hasRelationsRow -> relationsRowFocusRequester
                                hasStaffRow     -> staffRowFocusRequester
                                hasCharactersRow -> charactersRowFocusRequester
                                else            -> playButtonFocusRequester
                            },
                            focusDown = FocusRequester.Cancel
                        )
                    }
                }
            }


            // Auto focus play button on start
            LaunchedEffect(Unit) {
                playButtonFocusRequester.safeRequestFocus()
            }

            if (showTrailerOverlay) {
                com.kitsugi.animelist.ui.tv.components.TvSharedTrailerOverlay(
                    title = displayTitle,
                    playbackSource = trailerPlaybackSource,
                    isLoading = trailerLoading,
                    errorMessage = trailerError,
                    onDismiss = {
                        showTrailerOverlay = false
                    },
                    onRetry = {
                        loadAndShowTrailer()
                    }
                )
            }
        }
    }
}

@Composable
private fun BadgeText(text: String, color: Color = KitsugiColors.TextMuted) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(KitsugiColors.SurfaceStrong)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun TvActionButton(
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                BorderStroke(
                    width = if (isFocused) KitsugiTvTokens.Cards.focusedBorderWidth else 0.dp,
                    color = if (isFocused) Color.White else Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .tvClickable(shape = RoundedCornerShape(12.dp), onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}

@Composable
private fun TvEpisodeCard(
    episode: KitsugiStreamingEpisode,
    index: Int,
    accentColor: Color,
    rating: Double?,
    isWatched: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val blurRadius = if (!isWatched && index > 0 && !isFocused) 16.dp else 0.dp

    Column(
        modifier = Modifier
            .width(KitsugiTvTokens.Cards.episodeWidth)
            .clip(KitsugiTvTokens.Shapes.backdropCard)
            .background(KitsugiColors.Surface)
            .border(
                BorderStroke(
                    width = if (isFocused) KitsugiTvTokens.Cards.focusedBorderWidth else 0.dp,
                    color = if (isFocused) accentColor else Color.Transparent
                ),
                shape = KitsugiTvTokens.Shapes.backdropCard
            )
            .onFocusChanged { isFocused = it.isFocused }
            .tvClickable(shape = KitsugiTvTokens.Shapes.backdropCard, onClick = onClick)
    ) {
        // Thumbnail Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(KitsugiColors.SurfaceStrong),
            contentAlignment = Alignment.Center
        ) {
            if (!episode.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = episode.thumbnail,
                    contentDescription = episode.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier
                        ),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = accentColor.copy(alpha = 0.5f)
                )
            }

            // Blur Overlay text if blurred
            if (blurRadius > 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Spoiler",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Top Status Badges (Watched and IMDb)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isWatched) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2E7D32), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "İzlendi",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (rating != null && rating > 0.0) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFB300), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⭐ %.1f".format(rating),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Details Row
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Bölüm ${index + 1}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Text(
                text = episode.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = KitsugiColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!episode.site.isNullOrBlank()) {
                Text(
                    text = episode.site ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = KitsugiColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun TvCharacterCard(
    character: KitsugiCharacter,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(100.dp)
            .tvClickable(shape = RoundedCornerShape(10.dp), onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = character.imageUrl,
            contentDescription = character.name,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(KitsugiColors.SurfaceStrong)
                .border(
                    BorderStroke(
                        width = if (isFocused) KitsugiTvTokens.Cards.focusedBorderWidth else 0.dp,
                        color = if (isFocused) Color.White else Color.Transparent
                    ),
                    shape = RoundedCornerShape(36.dp)
                ),
            contentScale = ContentScale.Crop
        )

        Text(
            text = character.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = KitsugiColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TvStaffCard(
    staff: KitsugiStaff,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(100.dp)
            .tvClickable(shape = RoundedCornerShape(10.dp), onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = staff.imageUrl,
            contentDescription = staff.name,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(KitsugiColors.SurfaceStrong)
                .border(
                    BorderStroke(
                        width = if (isFocused) KitsugiTvTokens.Cards.focusedBorderWidth else 0.dp,
                        color = if (isFocused) Color.White else Color.Transparent
                    ),
                    shape = RoundedCornerShape(36.dp)
                ),
            contentScale = ContentScale.Crop
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = staff.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = KitsugiColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = staff.role,
                style = MaterialTheme.typography.labelSmall,
                color = KitsugiColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TvRelationCard(
    relation: KitsugiRelation,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(110.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .tvClickable(shape = RoundedCornerShape(10.dp), onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AsyncImage(
            model = relation.imageUrl,
            contentDescription = relation.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KitsugiColors.SurfaceStrong)
                .border(
                    BorderStroke(
                        width = if (isFocused) KitsugiTvTokens.Cards.focusedBorderWidth else 0.dp,
                        color = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentScale = ContentScale.Crop
        )

        Text(
            text = relation.title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = KitsugiColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = relation.relationType,
            style = MaterialTheme.typography.labelSmall,
            color = KitsugiColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------------------------------------------------------------------
//  TV Stats Section
// ---------------------------------------------------------------------------

@Composable
private fun TvStatsSection(
    stats: KitsugiStats,
    source: String,
    focusRequester: FocusRequester,
    focusUp: FocusRequester,
    focusDown: FocusRequester
) {
    val accentColor = LocalKitsugiAccent.current
    val isTmdb = source.equals("tmdb", ignoreCase = true) || source.equals("simkl", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .focusProperties {
                up   = focusUp
                down = focusDown
            },
        verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm)
    ) {
        Text(
            text = "İstatistikler",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = KitsugiColors.TextPrimary
        )

        val hasWatching = stats.watching != null && stats.watching > 0
        val hasCompleted = stats.completed != null && stats.completed > 0
        val hasPlanned = stats.planned != null && stats.planned > 0
        val hasDropped = stats.dropped != null && stats.dropped > 0
        val total = ((stats.watching ?: 0) + (stats.completed ?: 0) +
            (stats.planned ?: 0) + (stats.dropped ?: 0)).coerceAtLeast(1)

        if (hasWatching || hasCompleted || hasPlanned || hasDropped) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasWatching) {
                    TvStatChip(
                        label = if (isTmdb) "Popülerlik" else "İzleniyor",
                        count = stats.watching ?: 0,
                        total = total,
                        color = KitsugiColors.AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (hasCompleted) {
                    TvStatChip(
                        label = if (isTmdb) "Oylar" else "Tamamlandı",
                        count = stats.completed ?: 0,
                        total = total,
                        color = KitsugiColors.AccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (hasPlanned) {
                    TvStatChip(
                        label = "Planlandı",
                        count = stats.planned ?: 0,
                        total = total,
                        color = KitsugiColors.AccentOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (hasDropped) {
                    TvStatChip(
                        label = "Bırakıldı",
                        count = stats.dropped ?: 0,
                        total = total,
                        color = KitsugiColors.AccentRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (stats.scoreDistribution.isNotEmpty()) {
            val sorted = stats.scoreDistribution.sortedByDescending { it.score }
            val maxVotes = sorted.maxOfOrNull { it.amount }?.coerceAtLeast(1) ?: 1
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(KitsugiColors.Surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Puan Dağılımı",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = KitsugiColors.TextPrimary
                )
                sorted.forEach { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${s.score}★",
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.width(30.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(KitsugiColors.SurfaceStrong)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(s.amount.toFloat() / maxVotes)
                                    .background(accentColor)
                            )
                        }
                        Text(
                            text = s.amount.toString(),
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvStatChip(
    label: String,
    count: Int,
    total: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val pct = (count.toFloat() / total * 100).toInt()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KitsugiColors.Surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = KitsugiColors.TextMuted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = count.toString(),
            color = color,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(KitsugiColors.SurfaceStrong)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pct / 100f)
                    .background(color)
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  TV Review Card (D-pad optimize, BottomSheet'siz)
// ---------------------------------------------------------------------------

@Composable
private fun TvReviewCard(review: KitsugiReview) {
    var isFocused by remember { mutableStateOf(false) }
    val accentColor = LocalKitsugiAccent.current

    Column(
        modifier = Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .border(
                BorderStroke(
                    width = if (isFocused) KitsugiTvTokens.Cards.focusedBorderWidth else 0.dp,
                    color = if (isFocused) Color.White else Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = {})
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header: avatar + username + score
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(KitsugiColors.SurfaceStrong),
                contentAlignment = Alignment.Center
            ) {
                if (!review.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = review.avatarUrl,
                        contentDescription = review.username,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = review.username.take(1).uppercase(),
                        color = accentColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Text(
                text = review.username,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (review.score != null && review.score > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${review.score}/10",
                        color = accentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Summary
        Text(
            text = review.summary,
            color = KitsugiColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )

        // Date + helpful
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!review.dateText.isNullOrBlank()) {
                Text(
                    text = review.dateText,
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (review.helpfulCount != null && review.helpfulCount > 0) {
                Text(
                    text = "👍 ${review.helpfulCount}",
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
