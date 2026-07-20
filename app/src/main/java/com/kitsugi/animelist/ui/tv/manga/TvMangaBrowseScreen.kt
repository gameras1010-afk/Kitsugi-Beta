package com.kitsugi.animelist.ui.tv.manga

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.manga.MangaDetails
import com.kitsugi.animelist.data.manga.MangaSource
import com.kitsugi.animelist.data.manga.MangaSourceRepository
import com.kitsugi.animelist.ui.screens.manga.MangaBrowseViewModel
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import com.kitsugi.animelist.ui.utils.dpadRepeatThrottle
import com.kitsugi.animelist.ui.utils.tvClickable
import com.kitsugi.animelist.ui.components.rememberKitsugiShimmerBrush

/**
 * WP-13 — TV-optimize edilmiş Manga Browse ekranı.
 *
 * Mobilden farkları:
 * - KitsugiTvTokens spacing/overscan alanı
 * - D-pad odak yönetimi: kaynak chip row + içerik grid ayrı focus group'lar
 * - `focusRestorer` ile grid'e geri dönüşte son odaklanılan kart geri gelir
 * - Arama alanı yalnızca klavye odağına geçince expand olur
 * - Backdrop blur yerine hafif alpha overlay (TV GPU bütçesi için)
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TvMangaBrowseScreen(
    repository: MangaSourceRepository,
    vm: MangaBrowseViewModel = viewModel(factory = MangaBrowseViewModel.Factory(repository)),
    onMangaClick: (MangaSource, MangaDetails) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    val ui by vm.ui.collectAsState()
    val gridState = rememberLazyGridState()

    // Focus requesters
    val searchFocusRequester = remember { FocusRequester() }
    val chipRowFocusRequester = remember { FocusRequester() }
    val gridFocusRequester    = remember { FocusRequester() }

    var searchFocused by remember { mutableStateOf(false) }

    BackHandler(enabled = true) { onBack() }

    // Birleştirilmiş manga listesi (mobil mantığıyla aynı)
    val query     = ui.searchQuery
    val states    = ui.sourceStates
    val selFilter = ui.selectedSourceFilter
    val popular   = ui.popularMangas

    val mergedMangas = remember(states, selFilter, query, popular) {
        val raw = if (query.isNotBlank()) {
            val src = selFilter
            if (src != null) {
                (states.firstOrNull { it.source.name == src.name }?.mangas ?: emptyList())
                    .sortedByDescending {
                        com.kitsugi.animelist.data.manga.MangaTitleMatcher.getSimilarityScore(query, it.title)
                    }
            } else {
                val minScore = if (query.trim().length <= 3) 0.15 else 0.30
                states.flatMap { st ->
                    st.mangas.map { manga ->
                        val score = com.kitsugi.animelist.data.manga.MangaTitleMatcher.getSimilarityScore(query, manga.title)
                        Triple(manga, score, st.source)
                    }
                }
                .filter { (_, score, _) -> score >= minScore }
                .sortedByDescending { it.second }
                .map { it.first }
            }
        } else popular
        raw.distinctBy { "${it.source}_${it.url}" }
    }

    val backdropUrl = mergedMangas.firstOrNull()?.thumbnailUrl

    Box(Modifier.fillMaxSize().background(KitsugiColors.Background)) {

        // ── Hafif backdrop (TV: alpha overlay, blur yok) ──────────────────────
        if (!backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.12f }
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(KitsugiColors.Background.copy(0.7f), KitsugiColors.Background)
                )
            )
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    start  = KitsugiTvTokens.Spacing.screenHorizontal,
                    end    = KitsugiTvTokens.Spacing.screenHorizontal,
                    top    = KitsugiTvTokens.Spacing.screenVertical,
                    bottom = KitsugiTvTokens.Spacing.screenVertical
                )
        ) {

            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(KitsugiColors.SurfaceStrong.copy(0.5f))
                        .tvClickable(shape = RoundedCornerShape(8.dp)) { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Geri",
                        tint = KitsugiColors.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    "Manga Keşfet",
                    style = MaterialTheme.typography.titleLarge,
                    color = KitsugiColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Rounded.TravelExplore,
                    contentDescription = null,
                    tint = KitsugiColors.AccentBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Arama alanı ──────────────────────────────────────────────────
            val searchBorderAlpha by animateFloatAsState(
                targetValue = if (searchFocused) 1f else 0.4f,
                animationSpec = tween(200),
                label = "searchBorderAlpha"
            )
            OutlinedTextField(
                value = query,
                onValueChange = { vm.search(it) },
                placeholder = { Text("Manga ara...", color = KitsugiColors.TextMuted, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon  = { Icon(Icons.Rounded.Search, null, tint = KitsugiColors.TextMuted) },
                singleLine   = true,
                shape        = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor    = KitsugiColors.AccentBlue.copy(alpha = searchBorderAlpha),
                    unfocusedBorderColor  = KitsugiColors.Border.copy(alpha = searchBorderAlpha),
                    cursorColor           = KitsugiColors.AccentBlue,
                    focusedTextColor      = KitsugiColors.TextPrimary,
                    unfocusedTextColor    = KitsugiColors.TextPrimary,
                    focusedContainerColor    = KitsugiColors.Surface.copy(0.6f),
                    unfocusedContainerColor  = KitsugiColors.Surface.copy(0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester)
                    .onFocusChanged { searchFocused = it.isFocused }
            )

            Spacer(Modifier.height(12.dp))

            // ── Kaynak chip satırı ────────────────────────────────────────────
            if (ui.sources.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(chipRowFocusRequester)
                        .focusGroup()
                        .focusRestorer()
                        .dpadRepeatThrottle(horizontalGateMs = 100L, verticalGateMs = Long.MAX_VALUE),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    if (query.isNotBlank()) {
                        val totalCount = states.sumOf { it.mangas.size }
                        val anyLoading = states.any { it.isLoading }
                        item {
                            TvMangaSourceChip(
                                label     = "Tümü",
                                count     = totalCount,
                                isLoading = anyLoading,
                                isSelected = selFilter == null,
                                onClick = { vm.selectSourceFilter(null) }
                            )
                        }
                        items(states, key = { it.source.name }) { st ->
                            TvMangaSourceChip(
                                label      = st.source.name,
                                count      = st.mangas.size,
                                isLoading  = st.isLoading,
                                isSelected = selFilter?.name == st.source.name,
                                onClick    = {
                                    vm.selectSourceFilter(
                                        if (selFilter?.name == st.source.name) null else st.source
                                    )
                                }
                            )
                        }
                    } else {
                        items(ui.sources) { src ->
                            TvMangaSourceChip(
                                label      = "${src.name}  [${src.lang.uppercase()}]",
                                count      = if (selFilter?.name == src.name) popular.size else 0,
                                isLoading  = selFilter?.name == src.name && ui.isLoadingPopular,
                                isSelected = selFilter?.name == src.name,
                                onClick    = { vm.selectSourceFilter(src) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Manga grid ────────────────────────────────────────────────────
            val isLoading = if (query.isNotBlank()) states.any { it.isLoading } else ui.isLoadingPopular

            Box(Modifier.weight(1f)) {
                when {
                    isLoading && mergedMangas.isEmpty() -> {
                        // Skeleton grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            state = gridState,
                            modifier = Modifier.fillMaxSize()
                                .focusRequester(gridFocusRequester)
                                .focusGroup()
                                .focusRestorer(),
                            contentPadding = PaddingValues(4.dp),
                            verticalArrangement   = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(10) {
                                TvMangaCardSkeleton()
                            }
                        }
                    }
                    !isLoading && mergedMangas.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.Extension,
                                    contentDescription = null,
                                    tint = KitsugiColors.TextMuted,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Manga bulunamadı",
                                    color = KitsugiColors.TextSecondary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "Ayarlardan manga eklentisi ekle",
                                    color = KitsugiColors.TextMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            state = gridState,
                            modifier = Modifier.fillMaxSize()
                                .focusRequester(gridFocusRequester)
                                .focusGroup()
                                .focusRestorer(),
                            contentPadding = PaddingValues(4.dp),
                            verticalArrangement   = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(mergedMangas, key = { _, m -> "${m.source}_${m.url}" }) { _, manga ->
                                TvMangaCard(
                                    manga = manga,
                                    onClick = {
                                        val src = states.firstOrNull { it.source.name == manga.source }?.source
                                            ?: ui.sources.firstOrNull { it.name == manga.source }
                                            ?: selFilter
                                            ?: states.firstOrNull()?.source
                                            ?: ui.sources.firstOrNull()
                                        if (src != null) onMangaClick(src, manga)
                                    }
                                )
                            }

                            // Sonraki sayfa yükleme trigger
                            if (ui.hasNextPage && !isLoading) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(5) }) {
                                    LaunchedEffect(mergedMangas.size) { vm.loadNextPage() }
                                    Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            color = KitsugiColors.AccentBlue,
                                            modifier = Modifier.size(28.dp)
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
}

// ── TV Manga kart bileşeni ────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvMangaCard(
    manga: MangaDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .height(220.dp)
            .fillMaxWidth()
            .clip(cardShape)
            .tvClickable(
                shape       = cardShape,
                scaleFocused = 1.06f,
                borderWidth = 2.dp,
                onClick     = onClick
            )
            .background(KitsugiColors.Surface)
    ) {
        AsyncImage(
            model = manga.thumbnailUrl,
            contentDescription = manga.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(0.92f))
                    )
                )
                .padding(8.dp)
        ) {
            Column {
                Text(
                    manga.title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!manga.source.isNullOrBlank()) {
                    Text(
                        manga.source,
                        color = KitsugiColors.AccentBlue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Manga ikonu — poster yoksa merkeze
        if (manga.thumbnailUrl.isNullOrBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Book,
                    contentDescription = null,
                    tint = KitsugiColors.TextMuted,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

// ── TV Manga skeleton kart ────────────────────────────────────────────────────

@Composable
private fun TvMangaCardSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberKitsugiShimmerBrush()
    Box(
        modifier = modifier
            .height(220.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(brush)
    )
}

// ── TV Kaynak chip ────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvMangaSourceChip(
    label: String,
    count: Int,
    isLoading: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val chipShape = RoundedCornerShape(999.dp)
    val bg = when {
        isSelected -> KitsugiColors.AccentBlue.copy(0.35f)
        count > 0  -> KitsugiColors.AccentBlue.copy(0.12f)
        else       -> Color.White.copy(0.08f)
    }

    Row(
        modifier = Modifier
            .clip(chipShape)
            .background(bg)
            .tvClickable(shape = chipShape, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when {
            isLoading -> CircularProgressIndicator(
                Modifier.size(10.dp),
                color = KitsugiColors.AccentBlue,
                strokeWidth = 1.5.dp
            )
            count > 0 -> Box(
                Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(KitsugiColors.AccentBlue)
            )
            else -> Box(
                Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(KitsugiColors.TextMuted.copy(0.5f))
            )
        }
        Text(
            text = if (count > 0) "$label  $count" else label,
            color = if (isSelected || count > 0) KitsugiColors.TextPrimary else KitsugiColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
