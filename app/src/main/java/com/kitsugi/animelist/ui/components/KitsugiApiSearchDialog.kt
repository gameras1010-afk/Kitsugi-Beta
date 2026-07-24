@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.kitsugi.animelist.ui.components

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalIsTv
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import com.kitsugi.animelist.ui.utils.KitsugiScrollDefaults
import com.kitsugi.animelist.ui.utils.dpadVerticalFastScroll
import com.kitsugi.animelist.utils.toFriendlySourceLabel
import kotlinx.coroutines.launch

@Composable
fun KitsugiApiSearchDialog(
    onDismiss: () -> Unit,
    onResultSelected: (ApiSearchSelection) -> Unit
) {
    val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current
    val accentColor = LocalKitsugiAccent.current
    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember {
        JikanApiClient()
    }

    var query by rememberSaveable {
        mutableStateOf("")
    }

    var selectedType by rememberSaveable {
        mutableStateOf(MediaType.Anime)
    }

    var isLoading by rememberSaveable {
        mutableStateOf(false)
    }

    var errorMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var results by remember {
        mutableStateOf<List<JikanSearchResult>>(emptyList())
    }

    var previewResult by remember {
        mutableStateOf<JikanSearchResult?>(null)
    }

    fun runSearch() {
        if (query.isBlank() || isLoading) return

        coroutineScope.launch {
            isLoading = true
            errorMessage = null

            runCatching {
                apiClient.search(
                    query = query,
                    mediaType = selectedType
                )
            }.onSuccess { searchResults ->
                results = searchResults
                if (searchResults.isEmpty()) {
                    errorMessage = "Sonuç bulunamadı."
                }
            }.onFailure { error ->
                results = emptyList()
                errorMessage = error.message ?: "API isteği başarısız oldu."
            }

            isLoading = false
        }
    }

    if (isTvDevice) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KitsugiColors.Background)
                    .padding(horizontal = 48.dp, vertical = 32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "API ile Ara",
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = onDismiss
                        ) {
                            Text(
                                text = "Kapat",
                                color = KitsugiColors.TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Sol Sütun: Arama Girişleri
                        val leftScrollState = rememberScrollState()
                        val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides tvSpec
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(250.dp)
                                    .dpadVerticalFastScroll(leftScrollState)
                                    .verticalScroll(leftScrollState),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Jikan veya AniList üzerinden anime/manga ara. Sonuca basınca önizleme açılır.",
                                    color = KitsugiColors.TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )

                                TextField(
                                    value = query,
                                    onValueChange = {
                                        query = it
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(18.dp),
                                    label = {
                                        Text(
                                            text = "Arama",
                                            color = KitsugiColors.TextSecondary
                                        )
                                    },
                                    placeholder = {
                                        Text(
                                            text = "Örn: Naruto, Berserk, Frieren",
                                            color = KitsugiColors.TextMuted
                                        )
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = KitsugiColors.TextPrimary,
                                        unfocusedTextColor = KitsugiColors.TextPrimary,
                                        focusedContainerColor = KitsugiColors.SurfaceSoft,
                                        unfocusedContainerColor = KitsugiColors.SurfaceSoft,
                                        focusedIndicatorColor = accentColor,
                                        unfocusedIndicatorColor = KitsugiColors.Border,
                                        cursorColor = accentColor
                                    )
                                )

                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ApiSearchChip(
                                        title = "Anime",
                                        selected = selectedType == MediaType.Anime,
                                        onClick = {
                                            selectedType = MediaType.Anime
                                        }
                                    )

                                    ApiSearchChip(
                                        title = "Manga",
                                        selected = selectedType == MediaType.Manga,
                                        onClick = {
                                            selectedType = MediaType.Manga
                                        }
                                    )
                                }

                                TextButton(
                                    enabled = query.isNotBlank() && !isLoading,
                                    onClick = {
                                        runSearch()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isLoading) "Aranıyor..." else "Ara",
                                        color = if (query.isNotBlank() && !isLoading) {
                                            accentColor
                                        } else {
                                            KitsugiColors.TextMuted
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (errorMessage != null) {
                                    Text(
                                        text = errorMessage.orEmpty(),
                                        color = KitsugiColors.AccentRed,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Sağ Sütun: Arama Sonuçları
                        val rightScrollState = rememberScrollState()
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides tvSpec
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .dpadVerticalFastScroll(rightScrollState)
                                    .verticalScroll(rightScrollState),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (results.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isLoading) "Aranıyor..." else "Sonuç bulunamadı.",
                                            color = KitsugiColors.TextMuted,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "${results.size} sonuç",
                                        color = KitsugiColors.TextMuted,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    results.forEach { result ->
                                        ApiSearchResultCard(
                                            result = result,
                                            onClick = {
                                                previewResult = result
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
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = KitsugiColors.Surface,
            titleContentColor = KitsugiColors.TextPrimary,
            textContentColor = KitsugiColors.TextSecondary,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "API ile Ara",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Sol Sütun: Arama Girişleri
                        val leftScrollState = rememberScrollState()
                        val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides if (isTvDevice) tvSpec else LocalBringIntoViewSpec.current
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(250.dp)
                                    .then(if (isTvDevice) Modifier.dpadVerticalFastScroll(leftScrollState) else Modifier)
                                    .verticalScroll(leftScrollState),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                            Text(
                                text = "Jikan veya AniList üzerinden anime/manga ara. Sonuca basınca önizleme açılır.",
                                color = KitsugiColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )

                            TextField(
                                value = query,
                                onValueChange = {
                                    query = it
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                label = {
                                    Text(
                                        text = "Arama",
                                        color = KitsugiColors.TextSecondary
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = "Örn: Naruto, Berserk, Frieren",
                                        color = KitsugiColors.TextMuted
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = KitsugiColors.TextPrimary,
                                    unfocusedTextColor = KitsugiColors.TextPrimary,
                                    focusedContainerColor = KitsugiColors.SurfaceSoft,
                                    unfocusedContainerColor = KitsugiColors.SurfaceSoft,
                                    focusedIndicatorColor = accentColor,
                                    unfocusedIndicatorColor = KitsugiColors.Border,
                                    cursorColor = accentColor
                                )
                            )

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ApiSearchChip(
                                    title = "Anime",
                                    selected = selectedType == MediaType.Anime,
                                    onClick = {
                                        selectedType = MediaType.Anime
                                    }
                                )

                                ApiSearchChip(
                                    title = "Manga",
                                    selected = selectedType == MediaType.Manga,
                                    onClick = {
                                        selectedType = MediaType.Manga
                                    }
                                )
                            }

                            TextButton(
                                enabled = query.isNotBlank() && !isLoading,
                                onClick = {
                                    runSearch()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isLoading) "Aranıyor..." else "Ara",
                                    color = if (query.isNotBlank() && !isLoading) {
                                        accentColor
                                    } else {
                                        KitsugiColors.TextMuted
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = KitsugiColors.AccentRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        }

                        // Sağ Sütun: Arama Sonuçları
                        val rightScrollState = rememberScrollState()
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides if (isTvDevice) tvSpec else LocalBringIntoViewSpec.current
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(if (isTvDevice) Modifier.dpadVerticalFastScroll(rightScrollState) else Modifier)
                                    .verticalScroll(rightScrollState),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            if (results.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isLoading) "Aranıyor..." else "Sonuç bulunamadı.",
                                        color = KitsugiColors.TextMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                Text(
                                    text = "${results.size} sonuç",
                                    color = KitsugiColors.TextMuted,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                results.forEach { result ->
                                    ApiSearchResultCard(
                                        result = result,
                                        onClick = {
                                            previewResult = result
                                        }
                                    )
                                }
                            }
                        }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Jikan veya AniList üzerinden anime/manga ara. Sonuca basınca önce önizleme açılır.",
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        TextField(
                            value = query,
                            onValueChange = {
                                query = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            label = {
                                Text(
                                    text = "Arama",
                                    color = KitsugiColors.TextSecondary
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "Örn: Naruto, Berserk, Frieren",
                                    color = KitsugiColors.TextMuted
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = KitsugiColors.TextPrimary,
                                unfocusedTextColor = KitsugiColors.TextPrimary,
                                focusedContainerColor = KitsugiColors.SurfaceSoft,
                                unfocusedContainerColor = KitsugiColors.SurfaceSoft,
                                focusedIndicatorColor = accentColor,
                                unfocusedIndicatorColor = KitsugiColors.Border,
                                cursorColor = accentColor
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ApiSearchChip(
                                title = "Anime",
                                selected = selectedType == MediaType.Anime,
                                onClick = {
                                    selectedType = MediaType.Anime
                                }
                            )

                            ApiSearchChip(
                                title = "Manga",
                                selected = selectedType == MediaType.Manga,
                                onClick = {
                                    selectedType = MediaType.Manga
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        TextButton(
                            enabled = query.isNotBlank() && !isLoading,
                            onClick = {
                                runSearch()
                            }
                        ) {
                            Text(
                                text = if (isLoading) "Aranıyor..." else "Ara",
                                color = if (query.isNotBlank() && !isLoading) {
                                    accentColor
                                } else {
                                    KitsugiColors.TextMuted
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = errorMessage.orEmpty(),
                                color = KitsugiColors.AccentRed,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (results.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "${results.size} sonuç",
                                color = KitsugiColors.TextMuted,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            results.forEach { result ->
                                ApiSearchResultCard(
                                    result = result,
                                    onClick = {
                                        previewResult = result
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = "Kapat",
                        color = KitsugiColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    previewResult?.let { result ->
        ApiResultPreviewDialog(
            result = result,
            apiClient = apiClient,
            onAddClick = { synopsis ->
                onResultSelected(
                    ApiSearchSelection(
                        result = result,
                        synopsis = synopsis
                    )
                )
                previewResult = null
            },
            onDismiss = {
                previewResult = null
            }
        )
    }
}

@Composable
private fun ApiResultPreviewDialog(
    result: JikanSearchResult,
    apiClient: JikanApiClient,
    onAddClick: (synopsis: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current
    val accentColor = LocalKitsugiAccent.current

    var synopsisState by remember(result.source, result.malId, result.type) {
        mutableStateOf<PreviewSynopsisState>(PreviewSynopsisState.Loading)
    }

    LaunchedEffect(result.source, result.malId, result.type) {
        synopsisState = PreviewSynopsisState.Loading

        val synopsis = apiClient.fetchSynopsis(
            source = result.source,
            externalId = result.malId,
            mediaType = result.type
        )

        synopsisState = if (synopsis.isNullOrBlank()) {
            PreviewSynopsisState.Error
        } else {
            PreviewSynopsisState.Success(synopsis)
        }
    }

    val synopsisForSave = when (val state = synopsisState) {
        is PreviewSynopsisState.Success -> state.text
        else -> null
    }

    if (isTvDevice) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KitsugiColors.Background)
                    .padding(horizontal = 48.dp, vertical = 32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Önizleme",
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        androidx.compose.foundation.layout.Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    onAddClick(synopsisForSave)
                                }
                            ) {
                                Text(
                                    text = "Listeye Ekle",
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            TextButton(
                                onClick = onDismiss
                            ) {
                                Text(
                                    text = "İptal",
                                    color = KitsugiColors.TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Sol Sütun: Poster Resmi
                        Column(
                            modifier = Modifier
                                .width(180.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PreviewPoster(
                                result = result,
                                accentColor = accentColor,
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(250.dp)
                            )
                        }

                        // Sağ Sütun: Detaylar (Scrollable)
                        val rightScrollState = rememberScrollState()
                        val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides tvSpec
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .dpadVerticalFastScroll(rightScrollState)
                                    .verticalScroll(rightScrollState),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = result.title,
                                    color = KitsugiColors.TextPrimary,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (result.subtitle.isNotBlank()) {
                                    Text(
                                        text = result.subtitle,
                                        color = KitsugiColors.TextSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                PreviewInfoRow(
                                    label = "Tür",
                                    value = if (result.type == MediaType.Anime) "Anime" else "Manga"
                                )

                                val sourceLabel = result.source.toFriendlySourceLabel()
                                PreviewInfoRow(
                                    label = "Kaynak",
                                    value = sourceLabel
                                )

                                val sourceIdLabel = "${result.source.toFriendlySourceLabel()} ID"
                                PreviewInfoRow(
                                    label = sourceIdLabel,
                                    value = result.malId.toString()
                                )

                                PreviewInfoRow(
                                    label = "Yıl",
                                    value = result.year?.toString() ?: "-"
                                )

                                PreviewInfoRow(
                                    label = "Toplam",
                                    value = result.total?.toString() ?: "-"
                                )

                                PreviewInfoRow(
                                    label = "Skor",
                                    value = result.score?.let { "$it/10" } ?: "-"
                                )

                                PreviewInfoRow(
                                    label = "+18",
                                    value = if (result.isAdult) "Evet" else "Hayır"
                                )

                                PreviewSynopsisBox(
                                    synopsisState = synopsisState
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = KitsugiColors.Surface,
            titleContentColor = KitsugiColors.TextPrimary,
            textContentColor = KitsugiColors.TextSecondary,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Önizleme",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Sol Sütun: Poster Resmi
                        Column(
                            modifier = Modifier
                                .width(180.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PreviewPoster(
                                result = result,
                                accentColor = accentColor,
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(250.dp)
                            )
                        }

                        // Sağ Sütun: Detaylar (Scrollable)
                        val rightScrollState = rememberScrollState()
                        val tvSpec = KitsugiScrollDefaults.rememberTvCenteredSpec()
                        CompositionLocalProvider(
                            LocalBringIntoViewSpec provides if (isTvDevice) tvSpec else LocalBringIntoViewSpec.current
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(if (isTvDevice) Modifier.dpadVerticalFastScroll(rightScrollState) else Modifier)
                                    .verticalScroll(rightScrollState),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                            Text(
                                text = result.title,
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (result.subtitle.isNotBlank()) {
                                Text(
                                    text = result.subtitle,
                                    color = KitsugiColors.TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            PreviewInfoRow(
                                label = "Tür",
                                value = if (result.type == MediaType.Anime) "Anime" else "Manga"
                            )

                            val sourceLabel = result.source.toFriendlySourceLabel()
                            PreviewInfoRow(
                                label = "Kaynak",
                                value = sourceLabel
                            )

                            val sourceIdLabel = "${result.source.toFriendlySourceLabel()} ID"
                            PreviewInfoRow(
                                label = sourceIdLabel,
                                value = result.malId.toString()
                            )

                            PreviewInfoRow(
                                label = "Yıl",
                                value = result.year?.toString() ?: "-"
                            )

                            PreviewInfoRow(
                                label = "Toplam",
                                value = result.total?.toString() ?: "-"
                            )

                            PreviewInfoRow(
                                label = "Skor",
                                value = result.score?.let { "$it/10" } ?: "-"
                            )

                            PreviewInfoRow(
                                label = "+18",
                                value = if (result.isAdult) "Evet" else "Hayır"
                            )

                            PreviewSynopsisBox(
                                synopsisState = synopsisState
                            )
                        }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        PreviewPoster(
                            result = result,
                            accentColor = accentColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = result.title,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = result.subtitle,
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        PreviewInfoRow(
                            label = "Tür",
                            value = if (result.type == MediaType.Anime) "Anime" else "Manga"
                        )

                        val sourceLabel = result.source.toFriendlySourceLabel()
                        PreviewInfoRow(
                            label = "Kaynak",
                            value = sourceLabel
                        )

                        val sourceIdLabel = "${result.source.toFriendlySourceLabel()} ID"
                        PreviewInfoRow(
                            label = sourceIdLabel,
                            value = result.malId.toString()
                        )

                        PreviewInfoRow(
                            label = "Yıl",
                            value = result.year?.toString() ?: "-"
                        )

                        PreviewInfoRow(
                            label = "Toplam",
                            value = result.total?.toString() ?: "-"
                        )

                        PreviewInfoRow(
                            label = "Skor",
                            value = result.score?.let { "$it/10" } ?: "-"
                        )

                        PreviewInfoRow(
                            label = "+18",
                            value = if (result.isAdult) "Evet" else "Hayır"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PreviewSynopsisBox(
                            synopsisState = synopsisState
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddClick(synopsisForSave)
                    }
                ) {
                    Text(
                        text = "Listeye Ekle",
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = "İptal",
                        color = KitsugiColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}

@Composable
private fun PreviewPoster(
    result: JikanSearchResult,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(KitsugiColors.SurfaceSoft),
        contentAlignment = Alignment.Center
    ) {
        if (!result.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = result.imageUrl,
                contentDescription = result.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = result.title.take(2).uppercase(),
                color = accentColor,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun PreviewSynopsisBox(
    synopsisState: PreviewSynopsisState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(KitsugiColors.SurfaceSoft)
            .padding(14.dp)
    ) {
        Text(
            text = "Açıklama",
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        val text = when (synopsisState) {
            PreviewSynopsisState.Loading -> "Açıklama yükleniyor..."
            PreviewSynopsisState.Error -> "Açıklama alınamadı."
            is PreviewSynopsisState.Success -> synopsisState.text
        }

        Text(
            text = text,
            color = when (synopsisState) {
                PreviewSynopsisState.Error -> KitsugiColors.AccentRed
                else -> KitsugiColors.TextSecondary
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PreviewInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.SurfaceSoft)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = KitsugiColors.TextMuted,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun ApiSearchChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    Text(
        text = title,
        color = if (selected) {
            KitsugiColors.Background
        } else {
            KitsugiColors.TextSecondary
        },
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                color = if (selected) {
                    accentColor
                } else {
                    KitsugiColors.SurfaceSoft
                }
            )
            .tvClickable(shape = RoundedCornerShape(999.dp), onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    )
}

@Composable
private fun ApiSearchResultCard(
    result: JikanSearchResult,
    onClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(18.dp), onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = KitsugiColors.SurfaceSoft
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = result.title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = result.subtitle,
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (result.type == MediaType.Anime) "ANIME" else "MANGA",
                    color = accentColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                val sourceLabel = result.source.toFriendlySourceLabel()
                Text(
                    text = sourceLabel,
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "#${result.malId}",
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                if (result.year != null) {
                    Text(
                        text = result.year.toString(),
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (result.total != null) {
                    Text(
                        text = "Toplam: ${result.total}",
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (result.isAdult) {
                    Text(
                        text = "+18",
                        color = KitsugiColors.AccentRed,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private sealed class PreviewSynopsisState {
    data object Loading : PreviewSynopsisState()

    data object Error : PreviewSynopsisState()

    data class Success(
        val text: String
    ) : PreviewSynopsisState()
}