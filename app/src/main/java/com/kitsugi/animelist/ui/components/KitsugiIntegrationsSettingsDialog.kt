package com.kitsugi.animelist.ui.components

import androidx.compose.ui.res.stringResource
import com.kitsugi.animelist.R
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.launch

data class TmdbLanguage(val code: String, val displayName: String)

val AVAILABLE_TMDB_LANGUAGES = listOf(
    TmdbLanguage("en", "English"),
    TmdbLanguage("tr", "Türkçe"),
    TmdbLanguage("ja", "日本語"),
    TmdbLanguage("fr", "Français"),
    TmdbLanguage("de", "Deutsch"),
    TmdbLanguage("es", "Español"),
    TmdbLanguage("it", "Italiano"),
    TmdbLanguage("ru", "Русский"),
    TmdbLanguage("zh", "中文")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiIntegrationsSettingsDialog(
    tmdbEnabled: Boolean,
    onTmdbEnabledChanged: (Boolean) -> Unit,
    tmdbApiKey: String,
    onTmdbApiKeyChanged: (String) -> Unit,
    tmdbModernHomeEnabled: Boolean,
    onTmdbModernHomeEnabledChanged: (Boolean) -> Unit,
    tmdbEnrichContinueWatching: Boolean,
    onTmdbEnrichContinueWatchingChanged: (Boolean) -> Unit,
    tmdbLanguage: String,
    onTmdbLanguageChanged: (String) -> Unit,
    tmdbUseArtwork: Boolean,
    onTmdbUseArtworkChanged: (Boolean) -> Unit,
    tmdbUseBasicInfo: Boolean,
    onTmdbUseBasicInfoChanged: (Boolean) -> Unit,
    tmdbUseDetails: Boolean,
    onTmdbUseDetailsChanged: (Boolean) -> Unit,
    tmdbUseReleaseDates: Boolean,
    onTmdbUseReleaseDatesChanged: (Boolean) -> Unit,
    tmdbUseCredits: Boolean,
    onTmdbUseCreditsChanged: (Boolean) -> Unit,
    tmdbUseProductions: Boolean,
    onTmdbUseProductionsChanged: (Boolean) -> Unit,
    tmdbUseNetworks: Boolean,
    onTmdbUseNetworksChanged: (Boolean) -> Unit,
    tmdbUseEpisodes: Boolean,
    onTmdbUseEpisodesChanged: (Boolean) -> Unit,
    tmdbUseTrailers: Boolean,
    onTmdbUseTrailersChanged: (Boolean) -> Unit,
    tmdbUseMoreLikeThis: Boolean,
    onTmdbUseMoreLikeThisChanged: (Boolean) -> Unit,
    tmdbUseCollections: Boolean,
    onTmdbUseCollectionsChanged: (Boolean) -> Unit,
    mdbListEnabled: Boolean,
    onMdbListEnabledChanged: (Boolean) -> Unit,
    mdbListApiKey: String,
    onMdbListApiKeyChanged: (String) -> Unit,
    mdbListShowImdb: Boolean,
    onMdbListShowImdbChanged: (Boolean) -> Unit,
    mdbListShowTomatoes: Boolean,
    onMdbListShowTomatoesChanged: (Boolean) -> Unit,
    mdbListShowMetacritic: Boolean,
    onMdbListShowMetacriticChanged: (Boolean) -> Unit,
    mdbListShowAudience: Boolean,
    onMdbListShowAudienceChanged: (Boolean) -> Unit,
    mdbListShowLetterboxd: Boolean,
    onMdbListShowLetterboxdChanged: (Boolean) -> Unit,
    mdbListShowTmdb: Boolean,
    onMdbListShowTmdbChanged: (Boolean) -> Unit,
    mdbListShowTrakt: Boolean,
    onMdbListShowTraktChanged: (Boolean) -> Unit,
    aniSkipEnabled: Boolean,
    onAniSkipEnabledChanged: (Boolean) -> Unit,
    aniSkipAutoSkip: Boolean,
    onAniSkipAutoSkipChanged: (Boolean) -> Unit,
    animeSkipClientId: String,
    onAnimeSkipClientIdChanged: (String) -> Unit,
    fanartTvEnabled: Boolean,
    onFanartTvEnabledChanged: (Boolean) -> Unit,
    fanartTvApiKey: String,
    onFanartTvApiKeyChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val scope = rememberCoroutineScope()

    KitsugiSheetOrDialog(
        onDismiss = onDismiss,
        heightFraction = 0.85f
    ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.integrations_title),
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close), tint = KitsugiColors.TextSecondary)
                    }
                }
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = KitsugiColors.Surface,
                    contentColor = accentColor
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        text = {
                            Text(
                                stringResource(R.string.integrations_tab_tmdb),
                                fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        text = {
                            Text(
                                stringResource(R.string.integrations_tab_mdblist),
                                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        text = {
                            Text(
                                stringResource(R.string.integrations_tab_skip),
                                fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 3,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(3) }
                        },
                        text = {
                            Text(
                                "Fanart.tv",
                                fontWeight = if (pagerState.currentPage == 3) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Body
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) { page ->
                when (page) {
                    0 -> TmdbSettingsTab(
                        enabled = tmdbEnabled,
                        onEnabledChanged = onTmdbEnabledChanged,
                        apiKey = tmdbApiKey,
                        onApiKeyChanged = onTmdbApiKeyChanged,
                        modernHomeEnabled = tmdbModernHomeEnabled,
                        onModernHomeEnabledChanged = onTmdbModernHomeEnabledChanged,
                        enrichContinueWatching = tmdbEnrichContinueWatching,
                        onEnrichContinueWatchingChanged = onTmdbEnrichContinueWatchingChanged,
                        language = tmdbLanguage,
                        onLanguageChanged = onTmdbLanguageChanged,
                        useArtwork = tmdbUseArtwork,
                        onUseArtworkChanged = onTmdbUseArtworkChanged,
                        useBasicInfo = tmdbUseBasicInfo,
                        onUseBasicInfoChanged = onTmdbUseBasicInfoChanged,
                        useDetails = tmdbUseDetails,
                        onUseDetailsChanged = onTmdbUseDetailsChanged,
                        useReleaseDates = tmdbUseReleaseDates,
                        onUseReleaseDatesChanged = onTmdbUseReleaseDatesChanged,
                        useCredits = tmdbUseCredits,
                        onUseCreditsChanged = onTmdbUseCreditsChanged,
                        useProductions = tmdbUseProductions,
                        onUseProductionsChanged = onTmdbUseProductionsChanged,
                        useNetworks = tmdbUseNetworks,
                        onUseNetworksChanged = onTmdbUseNetworksChanged,
                        useEpisodes = tmdbUseEpisodes,
                        onUseEpisodesChanged = onTmdbUseEpisodesChanged,
                        useTrailers = tmdbUseTrailers,
                        onUseTrailersChanged = onTmdbUseTrailersChanged,
                        useMoreLikeThis = tmdbUseMoreLikeThis,
                        onUseMoreLikeThisChanged = onTmdbUseMoreLikeThisChanged,
                        useCollections = tmdbUseCollections,
                        onUseCollectionsChanged = onTmdbUseCollectionsChanged,
                        accentColor = accentColor
                    )
                    1 -> MdbListSettingsTab(
                        enabled = mdbListEnabled,
                        onEnabledChanged = onMdbListEnabledChanged,
                        apiKey = mdbListApiKey,
                        onApiKeyChanged = onMdbListApiKeyChanged,
                        showImdb = mdbListShowImdb,
                        onShowImdbChanged = onMdbListShowImdbChanged,
                        showTomatoes = mdbListShowTomatoes,
                        onShowTomatoesChanged = onMdbListShowTomatoesChanged,
                        showMetacritic = mdbListShowMetacritic,
                        onShowMetacriticChanged = onMdbListShowMetacriticChanged,
                        showAudience = mdbListShowAudience,
                        onShowAudienceChanged = onMdbListShowAudienceChanged,
                        showLetterboxd = mdbListShowLetterboxd,
                        onShowLetterboxdChanged = onMdbListShowLetterboxdChanged,
                        showTmdb = mdbListShowTmdb,
                        onShowTmdbChanged = onMdbListShowTmdbChanged,
                        showTrakt = mdbListShowTrakt,
                        onShowTraktChanged = onMdbListShowTraktChanged,
                        accentColor = accentColor
                    )
                    2 -> AniSkipSettingsTab(
                        enabled = aniSkipEnabled,
                        onEnabledChanged = onAniSkipEnabledChanged,
                        autoSkip = aniSkipAutoSkip,
                        onAutoSkipChanged = onAniSkipAutoSkipChanged,
                        animeSkipClientId = animeSkipClientId,
                        onAnimeSkipClientIdChanged = onAnimeSkipClientIdChanged,
                        accentColor = accentColor
                    )
                    3 -> FanartTvSettingsTab(
                        enabled = fanartTvEnabled,
                        onEnabledChanged = onFanartTvEnabledChanged,
                        apiKey = fanartTvApiKey,
                        onApiKeyChanged = onFanartTvApiKeyChanged,
                        accentColor = accentColor
                    )
                }
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_ok), color = accentColor, fontWeight = FontWeight.SemiBold)
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TmdbSettingsTab(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    apiKey: String,
    onApiKeyChanged: (String) -> Unit,
    modernHomeEnabled: Boolean,
    onModernHomeEnabledChanged: (Boolean) -> Unit,
    enrichContinueWatching: Boolean,
    onEnrichContinueWatchingChanged: (Boolean) -> Unit,
    language: String,
    onLanguageChanged: (String) -> Unit,
    useArtwork: Boolean,
    onUseArtworkChanged: (Boolean) -> Unit,
    useBasicInfo: Boolean,
    onUseBasicInfoChanged: (Boolean) -> Unit,
    useDetails: Boolean,
    onUseDetailsChanged: (Boolean) -> Unit,
    useReleaseDates: Boolean,
    onUseReleaseDatesChanged: (Boolean) -> Unit,
    useCredits: Boolean,
    onUseCreditsChanged: (Boolean) -> Unit,
    useProductions: Boolean,
    onUseProductionsChanged: (Boolean) -> Unit,
    useNetworks: Boolean,
    onUseNetworksChanged: (Boolean) -> Unit,
    useEpisodes: Boolean,
    onUseEpisodesChanged: (Boolean) -> Unit,
    useTrailers: Boolean,
    onUseTrailersChanged: (Boolean) -> Unit,
    useMoreLikeThis: Boolean,
    onUseMoreLikeThisChanged: (Boolean) -> Unit,
    useCollections: Boolean,
    onUseCollectionsChanged: (Boolean) -> Unit,
    accentColor: Color
) {
    val scrollState = rememberScrollState()
    var tempKey by remember(apiKey) { mutableStateOf(apiKey) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        KitsugiSettingsSection(title = stringResource(R.string.tmdb_title)) {
            KitsugiSettingsSwitchItem(
                title = "Etkinleştir",
                description = stringResource(R.string.tmdb_desc),
                icon = Icons.Rounded.Movie,
                iconColor = accentColor,
                checked = enabled,
                onCheckedChange = onEnabledChanged
            )

            if (enabled) {
                KitsugiSettingsDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = {
                            tempKey = it
                            onApiKeyChanged(it)
                        },
                        label = { Text(stringResource(R.string.tmdb_api_key)) },
                        placeholder = { Text(stringResource(R.string.tmdb_api_key_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = KitsugiColors.Border,
                            cursorColor = accentColor,
                            focusedTextColor = KitsugiColors.TextPrimary,
                            unfocusedTextColor = KitsugiColors.TextPrimary,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = KitsugiColors.TextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.tmdb_modern_home),
                    description = stringResource(R.string.tmdb_modern_home_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = modernHomeEnabled,
                    onCheckedChange = onModernHomeEnabledChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.tmdb_enrich_continue_watching),
                    description = stringResource(R.string.tmdb_enrich_continue_watching_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = enrichContinueWatching,
                    onCheckedChange = onEnrichContinueWatchingChanged
                )

                KitsugiSettingsDivider()

                val currentLangName = AVAILABLE_TMDB_LANGUAGES.find { it.code == language }?.displayName ?: language.uppercase()
                KitsugiSettingsListItem(
                    title = stringResource(R.string.tmdb_language),
                    description = stringResource(R.string.tmdb_language_desc),
                    value = currentLangName,
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    onClick = { showLanguageDialog = true }
                )
            }
        }

        if (enabled) {
            KitsugiSettingsSection(title = stringResource(R.string.tmdb_detail_blocks)) {
                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.tmdb_artwork),
                    description = stringResource(R.string.tmdb_artwork_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = useArtwork,
                    onCheckedChange = onUseArtworkChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.tmdb_basic_info),
                    description = stringResource(R.string.tmdb_basic_info_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = useBasicInfo,
                    onCheckedChange = onUseBasicInfoChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.tmdb_details),
                    description = stringResource(R.string.tmdb_details_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = useDetails,
                    onCheckedChange = onUseDetailsChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.tmdb_release_dates),
                    description = stringResource(R.string.tmdb_release_dates_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = useReleaseDates,
                    onCheckedChange = onUseReleaseDatesChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.tmdb_credits),
                    description = stringResource(R.string.tmdb_credits_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = useCredits,
                    onCheckedChange = onUseCreditsChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.tmdb_productions),
                    description = stringResource(R.string.tmdb_productions_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = useProductions,
                    onCheckedChange = onUseProductionsChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.tmdb_networks),
                    description = stringResource(R.string.tmdb_networks_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = useNetworks,
                    onCheckedChange = onUseNetworksChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.tmdb_episodes),
                    description = stringResource(R.string.tmdb_episodes_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = useEpisodes,
                    onCheckedChange = onUseEpisodesChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.tmdb_trailers),
                    description = stringResource(R.string.tmdb_trailers_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = useTrailers,
                    onCheckedChange = onUseTrailersChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.tmdb_more_like_this),
                    description = stringResource(R.string.tmdb_more_like_this_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = useMoreLikeThis,
                    onCheckedChange = onUseMoreLikeThisChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.tmdb_collections),
                    description = stringResource(R.string.tmdb_collections_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = useCollections,
                    onCheckedChange = onUseCollectionsChanged
                )
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            title = stringResource(R.string.tmdb_language_dialog_title),
            selectedLanguage = language,
            onLanguageSelected = {
                onLanguageChanged(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun LanguageSelectionDialog(
    title: String,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = KitsugiColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AVAILABLE_TMDB_LANGUAGES.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvClickable(shape = RoundedCornerShape(8.dp)) {
                                onLanguageSelected(lang.code)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lang.displayName,
                            color = if (lang.code == selectedLanguage) LocalKitsugiAccent.current else KitsugiColors.TextPrimary,
                            fontWeight = if (lang.code == selectedLanguage) FontWeight.Bold else FontWeight.Normal
                        )
                        if (lang.code == selectedLanguage) {
                            Text("✓", color = LocalKitsugiAccent.current, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), color = LocalKitsugiAccent.current)
            }
        },
        containerColor = KitsugiColors.Surface,
        textContentColor = KitsugiColors.TextPrimary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MdbListSettingsTab(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    apiKey: String,
    onApiKeyChanged: (String) -> Unit,
    showImdb: Boolean,
    onShowImdbChanged: (Boolean) -> Unit,
    showTomatoes: Boolean,
    onShowTomatoesChanged: (Boolean) -> Unit,
    showMetacritic: Boolean,
    onShowMetacriticChanged: (Boolean) -> Unit,
    showAudience: Boolean,
    onShowAudienceChanged: (Boolean) -> Unit,
    showLetterboxd: Boolean,
    onShowLetterboxdChanged: (Boolean) -> Unit,
    showTmdb: Boolean,
    onShowTmdbChanged: (Boolean) -> Unit,
    showTrakt: Boolean,
    onShowTraktChanged: (Boolean) -> Unit,
    accentColor: Color
) {
    val scrollState = rememberScrollState()
    var tempKey by remember(apiKey) { mutableStateOf(apiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        KitsugiSettingsSection(title = stringResource(R.string.mdblist_title)) {
            KitsugiSettingsSwitchItem(
                title = "Etkinleştir",
                description = stringResource(R.string.mdblist_desc),
                icon = Icons.Rounded.Movie,
                iconColor = accentColor,
                checked = enabled,
                onCheckedChange = onEnabledChanged
            )

            if (enabled) {
                KitsugiSettingsDivider()

                var showApiKeyDialog by remember { mutableStateOf(false) }

                KitsugiSettingsListItem(
                    title = stringResource(R.string.mdblist_api_key),
                    description = stringResource(R.string.mdblist_api_key_desc),
                    value = maskApiKey(apiKey, stringResource(R.string.value_not_set)),
                    icon = Icons.Rounded.Key,
                    iconColor = accentColor,
                    onClick = { showApiKeyDialog = true }
                )

                if (showApiKeyDialog) {
                    MdbListApiKeyValidationDialog(
                        currentValue = apiKey,
                        onSave = { key ->
                            onApiKeyChanged(key)
                            showApiKeyDialog = false
                        },
                        onDismiss = { showApiKeyDialog = false },
                        accentColor = accentColor
                    )
                }
            }
        }

        if (enabled) {
            KitsugiSettingsSection(title = stringResource(R.string.mdblist_score_services)) {
                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.mdblist_imdb),
                    description = stringResource(R.string.mdblist_imdb_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = showImdb,
                    onCheckedChange = onShowImdbChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.mdblist_rotten_tomatoes),
                    description = stringResource(R.string.mdblist_rotten_tomatoes_desc),
                    icon = Icons.Rounded.Star,
                    iconColor = accentColor,
                    checked = showTomatoes,
                    onCheckedChange = onShowTomatoesChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.mdblist_audience),
                    description = stringResource(R.string.mdblist_audience_desc),
                    icon = Icons.Rounded.Star,
                    iconColor = accentColor,
                    checked = showAudience,
                    onCheckedChange = onShowAudienceChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.mdblist_metacritic),
                    description = stringResource(R.string.mdblist_metacritic_desc),
                    icon = Icons.Rounded.Star,
                    iconColor = accentColor,
                    checked = showMetacritic,
                    onCheckedChange = onShowMetacriticChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.mdblist_letterboxd),
                    description = stringResource(R.string.mdblist_letterboxd_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = showLetterboxd,
                    onCheckedChange = onShowLetterboxdChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.mdblist_tmdb_score),
                    description = stringResource(R.string.mdblist_tmdb_score_desc),
                    icon = Icons.Rounded.Movie,
                    iconColor = accentColor,
                    checked = showTmdb,
                    onCheckedChange = onShowTmdbChanged
                )

                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.mdblist_trakt),
                    description = stringResource(R.string.mdblist_trakt_desc),
                    icon = Icons.Rounded.Star,
                    iconColor = accentColor,
                    checked = showTrakt,
                    onCheckedChange = onShowTraktChanged
                )
            }
        }
    }
}

@Composable
private fun AniSkipSettingsTab(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    autoSkip: Boolean,
    onAutoSkipChanged: (Boolean) -> Unit,
    animeSkipClientId: String,
    onAnimeSkipClientIdChanged: (String) -> Unit,
    accentColor: Color
) {
    val scrollState = rememberScrollState()
    var tempClientId by remember(animeSkipClientId) { mutableStateOf(animeSkipClientId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        KitsugiSettingsSection(title = stringResource(R.string.aniskip_title)) {
            KitsugiSettingsSwitchItem(
                title = "Etkinleştir",
                description = stringResource(R.string.aniskip_desc),
                icon = Icons.Rounded.SkipNext,
                iconColor = accentColor,
                checked = enabled,
                onCheckedChange = onEnabledChanged
            )

            if (enabled) {
                KitsugiSettingsDivider()

                KitsugiSettingsSwitchItem(
                    title = stringResource(R.string.aniskip_auto_skip),
                    description = stringResource(R.string.aniskip_auto_skip_desc),
                    icon = Icons.Rounded.SkipNext,
                    iconColor = accentColor,
                    checked = autoSkip,
                    onCheckedChange = onAutoSkipChanged
                )
            }
        }

        if (enabled) {
            KitsugiSettingsSection(title = stringResource(R.string.animeskip_integration)) {
                var showClientIdDialog by remember { mutableStateOf(false) }

                KitsugiSettingsListItem(
                    title = stringResource(R.string.animeskip_client_id),
                    description = stringResource(R.string.animeskip_client_id_desc),
                    value = maskApiKey(animeSkipClientId, stringResource(R.string.animeskip_default)),
                    icon = Icons.Rounded.Key,
                    iconColor = accentColor,
                    onClick = { showClientIdDialog = true }
                )

                if (showClientIdDialog) {
                    AnimeSkipClientIdValidationDialog(
                        currentValue = animeSkipClientId,
                        onSave = { clientId ->
                            onAnimeSkipClientIdChanged(clientId)
                            showClientIdDialog = false
                        },
                        onDismiss = { showClientIdDialog = false },
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

private fun maskApiKey(key: String, placeholder: String): String {
    val trimmed = key.trim()
    if (trimmed.isBlank()) return placeholder
    return if (trimmed.length <= 4) "••••" else "••••••${trimmed.takeLast(4)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MdbListApiKeyValidationDialog(
    currentValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    var value by remember(currentValue) { mutableStateOf(currentValue) }
    var validating by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.mdblist_api_dialog_title),
                color = KitsugiColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.api_key_dialog_desc),
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.mdblist_api_key)) },
                    placeholder = { Text(stringResource(R.string.mdblist_api_key_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = KitsugiColors.Border,
                        cursorColor = accentColor,
                        focusedTextColor = KitsugiColors.TextPrimary,
                        unfocusedTextColor = KitsugiColors.TextPrimary,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = KitsugiColors.TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (value.isNotEmpty()) {
                            IconButton(onClick = { value = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.action_clear),
                                    tint = KitsugiColors.TextSecondary
                                )
                            }
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (value.isBlank()) {
                        onSave("")
                        return@Button
                    }
                    validating = true
                    scope.launch {
                        val valid = com.kitsugi.animelist.data.remote.MdbListClient.validateApiKey(value)
                        validating = false
                        if (valid) {
                            onSave(value)
                            android.widget.Toast.makeText(context, context.getString(R.string.api_key_validated), android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, context.getString(R.string.api_key_invalid), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                enabled = !validating
            ) {
                if (validating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text(stringResource(R.string.action_validating))
                    }
                } else {
                    Text(stringResource(R.string.action_validate_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !validating) {
                Text(stringResource(R.string.action_cancel), color = KitsugiColors.TextMuted)
            }
        },
        containerColor = KitsugiColors.Surface,
        textContentColor = KitsugiColors.TextPrimary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimeSkipClientIdValidationDialog(
    currentValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    var value by remember(currentValue) { mutableStateOf(currentValue) }
    var validating by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.animeskip_client_id_dialog_title),
                color = KitsugiColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.animeskip_client_id_dialog_desc),
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.animeskip_client_id_label)) },
                    placeholder = { Text(stringResource(R.string.animeskip_default)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = KitsugiColors.Border,
                        cursorColor = accentColor,
                        focusedTextColor = KitsugiColors.TextPrimary,
                        unfocusedTextColor = KitsugiColors.TextPrimary,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = KitsugiColors.TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (value.isNotEmpty()) {
                            IconButton(onClick = { value = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.action_clear),
                                    tint = KitsugiColors.TextSecondary
                                )
                            }
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (value.isBlank()) {
                        onSave("")
                        return@Button
                    }
                    validating = true
                    scope.launch {
                        val valid = com.kitsugi.animelist.data.remote.AnimeSkipClient.validateClientId(value)
                        validating = false
                        if (valid) {
                            onSave(value)
                            android.widget.Toast.makeText(context, context.getString(R.string.client_id_validated), android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, context.getString(R.string.client_id_invalid), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                enabled = !validating
            ) {
                if (validating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text(stringResource(R.string.action_validating))
                    }
                } else {
                    Text(stringResource(R.string.action_validate_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !validating) {
                Text(stringResource(R.string.action_cancel), color = KitsugiColors.TextMuted)
            }
        },
        containerColor = KitsugiColors.Surface,
        textContentColor = KitsugiColors.TextPrimary
    )
}

@Composable
private fun FanartTvSettingsTab(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    apiKey: String,
    onApiKeyChanged: (String) -> Unit,
    accentColor: Color
) {
    val scrollState = rememberScrollState()
    var tempKey by remember(apiKey) { mutableStateOf(apiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        KitsugiSettingsSection(title = "Fanart.tv Ayarları") {
            KitsugiSettingsSwitchItem(
                title = "Etkinleştir",
                description = "Fanart.tv entegrasyonunu etkinleştirerek yüksek kaliteli logo, karakter tasarımları ve arka plan görselleri çekin.",
                icon = Icons.Rounded.Movie,
                iconColor = accentColor,
                checked = enabled,
                onCheckedChange = onEnabledChanged
            )

            if (enabled) {
                KitsugiSettingsDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = {
                            tempKey = it
                            onApiKeyChanged(it)
                        },
                        label = { Text("Fanart.tv API Anahtarı") },
                        placeholder = { Text("Kişisel Fanart.tv API anahtarınızı girin...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = KitsugiColors.Border,
                            cursorColor = accentColor,
                            focusedTextColor = KitsugiColors.TextPrimary,
                            unfocusedTextColor = KitsugiColors.TextPrimary,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = KitsugiColors.TextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (tempKey.isNotEmpty()) {
                                IconButton(onClick = {
                                    tempKey = ""
                                    onApiKeyChanged("")
                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Temizle",
                                        tint = KitsugiColors.TextSecondary
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

