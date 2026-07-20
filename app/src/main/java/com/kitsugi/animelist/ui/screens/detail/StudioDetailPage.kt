package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanApiClient
import com.kitsugi.animelist.data.remote.KitsugiStudioDetail
import com.kitsugi.animelist.data.remote.KitsugiStaffMediaWork
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.data.remote.DetailCache
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.kitsugi.animelist.ui.components.KitsugiMarkdownText
import com.kitsugi.animelist.ui.components.KitsugiPageEnter
import com.kitsugi.animelist.ui.components.KitsugiCinematicLoadingScreen

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel

sealed interface StudioDetailState {
    object Loading : StudioDetailState
    data class Error(val message: String) : StudioDetailState
    data class Success(val detail: KitsugiStudioDetail) : StudioDetailState
}

@Composable
fun StudioDetailPage(
    studioId: Int,
    source: String,
    onBackClick: () -> Unit,
    onMediaClick: (mediaId: Int, mediaType: String, mediaSource: String) -> Unit,
    name: String? = null,
    imageUrl: String? = null
) {
    val accentColor = LocalKitsugiAccent.current

    // Obtain ViewModel
    val viewModel: StudioDetailViewModel = viewModel()

    // Load studio in ViewModel
    LaunchedEffect(studioId, source) {
        viewModel.loadStudio(studioId, source, name)
    }

    // Collect state from ViewModel
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
    ) {
        when (val currentState = state) {
            is StudioDetailState.Loading -> {
                KitsugiCinematicLoadingScreen(
                    title = name ?: "Stüdyo Yükleniyor...",
                    imageUrl = imageUrl,
                    onBackClick = onBackClick
                )
            }
            is StudioDetailState.Error -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Geri",
                            tint = KitsugiColors.TextPrimary
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentState.message,
                            color = KitsugiColors.AccentRed,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = {
                                viewModel.retry()
                            }
                        ) {
                            Text("Yeniden Dene", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            is StudioDetailState.Success -> {
                val detail = currentState.detail
                val gridState = rememberLazyGridState()
                val showFloatingHeader = gridState.firstVisibleItemIndex >= 1
                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

                if (isLandscape) {
                    // ── LANDSCAPE: Sol meta paneli + Sağ media grid ──
                    KitsugiPageEnter {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Sol panel: Banner + stüdyo bilgileri
                            Column(
                                modifier = Modifier
                                    .weight(0.38f)
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .background(KitsugiColors.Background)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                ) {
                                    if (!detail.imageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = detail.imageUrl,
                                            contentDescription = detail.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(
                                                Brush.verticalGradient(listOf(KitsugiColors.Background.copy(alpha = 0.4f), KitsugiColors.Background))
                                            )
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(
                                                Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.2f), KitsugiColors.Background))
                                            ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(detail.name.take(2).uppercase(), color = accentColor, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    TextButton(
                                        onClick = onBackClick,
                                        modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 8.dp)
                                    ) {
                                        Text("Geri", color = KitsugiColors.TextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(detail.name, color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    DetailPill(text = if (detail.isMain) "Ana Stüdyo" else "Yapımcı", color = accentColor)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (detail.established != null) {
                                        DetailPill(text = "Kuruluş: ${detail.established}", color = KitsugiColors.TextSecondary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    if (detail.favorites != null && detail.favorites > 0) {
                                        DetailPill(text = "★ ${detail.favorites} Favori", color = accentColor)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    if (!detail.about.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Column(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(KitsugiColors.Surface).padding(16.dp)
                                        ) {
                                            Text("Hakkında", color = KitsugiColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            KitsugiMarkdownText(text = detail.about)
                                        }
                                    }
                                }
                            }
                            // Sağ panel: Media grid
                            Column(
                                modifier = Modifier
                                    .weight(0.62f)
                                    .fillMaxSize()
                                    .background(KitsugiColors.Background)
                            ) {
                                Text(
                                    text = "Yapımlar (${detail.mediaWorks.size})",
                                    color = KitsugiColors.TextPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                                )
                                if (detail.mediaWorks.isEmpty()) {
                                    Text("Yapım bulunamadı.", color = KitsugiColors.TextMuted, modifier = Modifier.padding(16.dp))
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(detail.mediaWorks) { work ->
                                            StudioMediaGridItem(work = work, onMediaClick = onMediaClick)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ── PORTRAIT: Mevcut LazyVerticalGrid düzeni ──
                KitsugiPageEnter {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 90.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 1. Banner & Detail Header Section
                            item(span = { GridItemSpan(3) }) {
                                StudioHeroHeader(
                                    detail = detail,
                                    source = source,
                                    accentColor = accentColor,
                                    onBackClick = onBackClick
                                )
                            }

                            // 2. About section (MAL/Jikan usually has this)
                            if (!detail.about.isNullOrBlank()) {
                                item(span = { GridItemSpan(3) }) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(KitsugiColors.Surface)
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Hakkında",
                                            color = KitsugiColors.TextPrimary,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        KitsugiMarkdownText(text = detail.about)
                                    }
                                }
                            }

                            // 3. Grid Title
                            item(span = { GridItemSpan(3) }) {
                                Text(
                                    text = "Yapımlar (${detail.mediaWorks.size})",
                                    color = KitsugiColors.TextPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                                )
                            }

                            // 4. Fallback if empty
                            if (detail.mediaWorks.isEmpty()) {
                                item(span = { GridItemSpan(3) }) {
                                    Text(
                                        text = "Yapım bulunamadı.",
                                        color = KitsugiColors.TextMuted,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            // 5. Grid of anime items
                            items(detail.mediaWorks) { work ->
                                StudioMediaGridItem(
                                    work = work,
                                    onMediaClick = onMediaClick
                                )
                            }
                        }

                        // Floating header overlay
                        AnimatedVisibility(
                            visible = showFloatingHeader,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                            modifier = Modifier.align(Alignment.TopCenter)
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
                                    text = detail.name,
                                    color = KitsugiColors.TextPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                } // end else (portrait)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudioHeroHeader(
    detail: KitsugiStudioDetail,
    source: String,
    accentColor: Color,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        // Logo / Fallback box
        if (!detail.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = detail.imageUrl,
                contentDescription = detail.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            // Blur or dark layer over image for visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                KitsugiColors.Background.copy(alpha = 0.4f),
                                KitsugiColors.Background
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.2f),
                                KitsugiColors.Background
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = detail.name.take(2).uppercase(),
                    color = accentColor,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 40.dp)
                )
            }
        }

        // Back Button
        TextButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 24.dp)
        ) {
            Text(
                text = "Geri",
                color = KitsugiColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        // Details Column (Name & Info Row)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                text = detail.name,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // AniHyou / MoeList gibi: Ana Stüdyo mu Yapımcı mı?
                DetailPill(
                    text = if (detail.isMain) "Ana Stüdyo" else "Yapımcı",
                    color = accentColor
                )

                DetailPill(
                    text = source.uppercase(),
                    color = KitsugiColors.TextSecondary
                )

                if (detail.established != null) {
                    DetailPill(
                        text = "Kuruluş: ${detail.established}",
                        color = KitsugiColors.TextSecondary
                    )
                }

                if (detail.favorites != null && detail.favorites > 0) {
                    DetailPill(
                        text = "★ ${detail.favorites} Favori",
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioMediaGridItem(
    work: KitsugiStaffMediaWork,
    onMediaClick: (mediaId: Int, mediaType: String, mediaSource: String) -> Unit
) {
    val leftPadding = 16.dp
    val rightPadding = 16.dp

    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .width(105.dp)
            .tvClickable { onMediaClick(work.mediaId, work.mediaType, work.source) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(11f / 16f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(KitsugiColors.Surface)
        ) {
            if (!work.mediaImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = work.mediaImageUrl,
                    contentDescription = work.mediaTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = work.mediaTitle.take(2).uppercase(),
                        color = KitsugiColors.TextMuted,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = work.mediaTitle,
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}


