package com.kitsugi.animelist.ui.tv.stream

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil3.compose.AsyncImage
import com.kitsugi.animelist.core.player.ExternalPlayerInput
import com.kitsugi.animelist.core.player.ExternalPlayerLauncher
import com.kitsugi.animelist.data.cloudstream.CsPluginLoader
import com.kitsugi.animelist.data.cloudstream.CsStreamRunner
import com.kitsugi.animelist.data.cloudstream.CsPluginStatusTracker
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.remote.AddonStreamClient
import com.kitsugi.animelist.data.remote.DebridResolver
import com.kitsugi.animelist.data.repository.AddonStreamRepository
import com.kitsugi.animelist.data.repository.StreamSorter
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.ui.tv.player.TvPlayerActivity
import com.kitsugi.animelist.ui.screens.stream.AddonFetchState
import com.kitsugi.animelist.ui.screens.stream.StreamViewModel
import com.kitsugi.animelist.ui.screens.stream.DebridCacheState
import com.kitsugi.animelist.ui.screens.stream.getCacheState
import com.kitsugi.animelist.ui.screens.stream.parseStreamTitle
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import com.kitsugi.animelist.ui.tv.input.isSelectKey
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.kitsugi.animelist.core.player.QualityProfile
import com.kitsugi.animelist.core.player.QualityDataHelper

@OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun TvStreamScreen(
    args: TvStreamArgs,
    onBack: () -> Unit,
    onLaunchExternalPlayer: ((input: ExternalPlayerInput, streamKey: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val scope = rememberCoroutineScope()

    val viewModel: StreamViewModel = viewModel()
    val addonStates by viewModel.addonStates.collectAsStateWithLifecycle()
    val isResolvingId by viewModel.isResolvingId.collectAsStateWithLifecycle()
    val idResolveFailed by viewModel.idResolveFailed.collectAsStateWithLifecycle()
    val imdbId by viewModel.imdbId.collectAsStateWithLifecycle()

    val repository = remember { AddonStreamRepository(context) }
    val streamPrefs = remember { context.getSharedPreferences("KitsugiStreamPrefs", Context.MODE_PRIVATE) }
    val dataStore = remember { SettingsDataStore(context) }
    val appSettings by dataStore.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())

    var selectedAddonFilter by remember { mutableStateOf<String?>(null) }
    var resolvingSource by remember { mutableStateOf<StreamSource?>(null) }
    var resolvingError by remember { mutableStateOf<String?>(null) }

    val allStreams = remember(addonStates, appSettings.qualityProfileJson) {
        val rawList = addonStates.flatMap { it.streams }
        val profile = QualityProfile.deserialize(appSettings.qualityProfileJson)
        val filtered = QualityDataHelper.filterByBitrate(rawList, profile.maxBitrateKbps)
        val sorted = StreamSorter.sort(filtered)
        QualityDataHelper.sortByProfile(sorted, profile)
    }
    val filteredStreams = remember(allStreams, selectedAddonFilter) {
        if (selectedAddonFilter == null) allStreams else allStreams.filter { it.addonName == selectedAddonFilter }
    }

    val alternativeTitles = remember(args.titleEnglish, args.titleRomaji, args.titleNative, args.title) {
        buildList {
            args.titleEnglish?.takeIf { it.isNotBlank() && it != args.title }?.let { add(it) }
            args.titleRomaji?.takeIf { it.isNotBlank() && it != args.title }?.let { add(it) }
            args.titleNative?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }

    LaunchedEffect(args.malId, args.aniListId, args.tmdbId, args.episode, args.season) {
        viewModel.startFetch(
            malId = args.malId,
            aniListId = args.aniListId,
            tmdbId = args.tmdbId,
            episode = args.episode,
            season = args.season,
            title = args.title,
            alternativeTitles = alternativeTitles,
            startYear = args.startYear
        )
    }

    val launchPlayer = remember(context, allStreams, streamPrefs, onLaunchExternalPlayer) {
        { source: StreamSource, resolvedUrl: String, engine: String ->
            val streamKey = (source.infoHash ?: resolvedUrl).hashCode().toString()
            val resumePositionMs = streamPrefs.getLong("pos_$streamKey", 0L)
            if (engine == "EXTERNAL" && onLaunchExternalPlayer != null) {
                val input = ExternalPlayerLauncher.createInput(
                    url = resolvedUrl,
                    title = "${args.title} - Bölüm ${args.episode}",
                    headers = source.requestHeaders,
                    resumePositionMs = resumePositionMs,
                    subtitles = source.subtitles
                )
                onLaunchExternalPlayer(input, streamKey)
            } else {
                TvPlayerActivity.start(
                    context = context,
                    videoUrl = resolvedUrl,
                    title = "${args.title} - Bölüm ${args.episode}",
                    headers = source.requestHeaders,
                    subtitles = source.subtitles ?: emptyList(),
                    allSources = allStreams,
                    currentSourceIndex = allStreams.indexOf(source),
                    malId = args.malId,
                    aniListId = args.aniListId,
                    season = args.season,
                    episode = args.episode,
                    animeTitle = args.title,
                    posterUrl = args.posterUrl,
                    titleEnglish = args.titleEnglish,
                    titleRomaji = args.titleRomaji,
                    titleNative = args.titleNative,
                    startYear = args.startYear,
                    description = args.description
                )
            }
        }
    }

    val handlePlayStream = remember(appSettings, launchPlayer) {
        { source: StreamSource, resolvedUrl: String ->
            val engine = appSettings.playerPreference
            launchPlayer(source, resolvedUrl, engine)
        }
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
    ) {
        // Background art (Poster blurred)
        if (!args.posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = args.posterUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.15f),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                KitsugiColors.Background.copy(alpha = 0.5f),
                                KitsugiColors.Background
                            )
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = KitsugiTvTokens.Spacing.screenHorizontal,
                    vertical = KitsugiTvTokens.Spacing.screenVertical
                ),
            horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.contentPadding)
        ) {
            // Left Panel: Poster & Info
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm)
            ) {
                AsyncImage(
                    model = args.posterUrl,
                    contentDescription = args.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f)
                        .clip(KitsugiTvTokens.Shapes.posterCard as RoundedCornerShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), KitsugiTvTokens.Shapes.posterCard as RoundedCornerShape),
                    contentScale = ContentScale.Crop
                )

                Text(
                    text = args.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = KitsugiColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (args.isMovie) "Film" else "${args.season}. Sezon - ${args.episode}. Bölüm",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )

                if (!args.description.isNullOrBlank()) {
                    Text(
                        text = args.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = KitsugiColors.TextSecondary,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Panel: Filter Tabs & Stream List
            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm)
            ) {
                // Addon Filter Tabs Row
                val uniqueAddons = remember(addonStates) { addonStates.map { it.addonName } }
                if (uniqueAddons.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusGroup(),
                        horizontalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        item {
                            FilterChipTv(
                                text = "Hepsi",
                                isSelected = selectedAddonFilter == null,
                                onClick = { selectedAddonFilter = null }
                            )
                        }
                        items(uniqueAddons) { addonName ->
                            FilterChipTv(
                                text = addonName,
                                isSelected = selectedAddonFilter == addonName,
                                onClick = { selectedAddonFilter = addonName }
                            )
                        }
                    }
                }

                // Streams list container
                val listState = rememberLazyListState()
                val isAnyLoading = addonStates.any { it.isLoading }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (isResolvingId) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    } else if (filteredStreams.isEmpty() && !isAnyLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Akış bulunamadı.",
                                style = MaterialTheme.typography.titleMedium,
                                color = KitsugiColors.TextMuted
                            )
                            if (idResolveFailed) {
                                Text(
                                    text = "Kimlik çözümleme başarısız oldu.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Red.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides KitsugiScrollDefaults.rememberTvCenteredSpec()
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .dpadVerticalFastScroll(scrollableState = listState)
                                    .focusRestorer()
                                    .focusGroup(),
                                verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm)
                            ) {
                                itemsIndexed(filteredStreams) { index, stream ->
                                    TvStreamRowItem(
                                        stream = stream,
                                        index = index,
                                        accentColor = accentColor,
                                        onClick = {
                                            val isTorrent = !stream.infoHash.isNullOrBlank() || stream.url?.startsWith("magnet:") == true
                                            if (isTorrent && DebridResolver(context).getApiKey().isNullOrBlank()) {
                                                resolvingError = "Debrid API anahtarı gerekli."
                                                return@TvStreamRowItem
                                            }
                                            resolvingSource = stream
                                            resolvingError = null
                                            scope.launch {
                                                val resolvedUrl = repository.resolveStreamUrl(stream)
                                                resolvingSource = null
                                                if (resolvedUrl == null) {
                                                    resolvingError = "Akış linki çözümlenemedi."
                                                    return@launch
                                                }
                                                handlePlayStream(stream, resolvedUrl)
                                            }
                                        }
                                    )
                                }

                                if (isAnyLoading) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Resolving Overlay Dialog
        resolvingSource?.let { source ->
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(260.dp, 160.dp)
                        .clip(KitsugiTvTokens.Shapes.dialog as RoundedCornerShape)
                        .background(KitsugiColors.BackgroundElevated)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = accentColor)
                        Text(
                            text = "Link Çözümleniyor...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = source.addonName,
                            style = MaterialTheme.typography.labelSmall,
                            color = KitsugiColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Error Dialog
        resolvingError?.let { err ->
            com.kitsugi.animelist.ui.tv.components.TvDialog(
                onDismiss = { resolvingError = null },
                title = "Akış Hatası",
                width = 420.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = err,
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { resolvingError = null }) {
                            Text("Tamam", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipTv(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) LocalKitsugiAccent.current else KitsugiColors.Surface)
            .border(
                BorderStroke(
                    width = if (isFocused) KitsugiTvTokens.Cards.focusedBorderWidth else 0.dp,
                    color = if (isFocused) Color.White else Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected || isFocused) Color.White else KitsugiColors.TextSecondary
        )
    }
}

@Composable
private fun TvStreamRowItem(
    stream: StreamSource,
    index: Int,
    accentColor: Color,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val (quality, size) = remember(stream.title) { parseStreamTitle(stream.title ?: "") }
    val cacheState = remember(stream) { getCacheState(stream) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color.White.copy(alpha = 0.08f) else KitsugiColors.Surface)
            .border(
                BorderStroke(
                    width = if (isFocused) KitsugiTvTokens.Cards.focusedBorderWidth else 0.dp,
                    color = if (isFocused) accentColor else Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .tvClickable(shape = RoundedCornerShape(8.dp), onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stream Index / Quality Badge
        val indexText = if (quality.isNotBlank()) quality else "${index + 1}"
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KitsugiColors.SurfaceStrong),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = indexText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                color = accentColor
            )
        }

        // Title and Provider Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Rich Badges Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Addon Name Badge
                TvStreamBadge(
                    text = stream.addonName,
                    color = KitsugiColors.AccentPurple,
                    bgAlpha = 0.15f,
                    bgColor = KitsugiColors.AccentPurple
                )

                // Debrid Cache Badge
                val (cacheText, cacheColor) = when (cacheState) {
                    DebridCacheState.CACHED     -> "Önbellek" to KitsugiColors.AccentGreen
                    DebridCacheState.NOT_CACHED -> "İndir" to KitsugiColors.AccentOrange
                    DebridCacheState.P2P        -> "P2P" to KitsugiColors.AccentBlue
                }
                TvStreamBadge(text = cacheText, color = cacheColor, bgAlpha = 0.15f, bgColor = cacheColor)

                // Size Badge
                if (size.isNotBlank()) {
                    TvStreamBadge(
                        text = size,
                        color = KitsugiColors.TextSecondary,
                        bgAlpha = 0.1f,
                        bgColor = Color.White
                    )
                }
            }

            Text(
                text = stream.name ?: "İsimsiz Akış",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = KitsugiColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!stream.title.isNullOrBlank() && stream.title != stream.name) {
                Text(
                    text = stream.title.trim(),
                    style = MaterialTheme.typography.labelSmall,
                    color = KitsugiColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Action Indicator (Play Icon)
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = if (isFocused) accentColor else KitsugiColors.TextMuted,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TvStreamBadge(text: String, color: Color, bgAlpha: Float, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor.copy(alpha = bgAlpha))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
