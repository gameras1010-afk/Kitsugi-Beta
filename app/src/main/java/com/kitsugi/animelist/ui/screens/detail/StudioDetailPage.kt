package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.remote.GalleryItem
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import com.kitsugi.animelist.ui.components.KitsugiImageGalleryDialog
import com.kitsugi.animelist.ui.components.KitsugiPageEnter
import com.kitsugi.animelist.ui.components.KitsugiCinematicLoadingScreen
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel

sealed interface StudioDetailState {
    object Loading : StudioDetailState
    data class Error(val message: String) : StudioDetailState
    data class Success(val detail: com.kitsugi.animelist.data.remote.KitsugiStudioDetail) : StudioDetailState
}

@Composable
fun StudioDetailPage(
    studioId: Int,
    source: String,
    onBackClick: () -> Unit,
    onMediaClick: (mediaId: Int, mediaType: String, mediaSource: String) -> Unit,
    name: String? = null,
    imageUrl: String? = null,
    titleLanguage: String = "ROMAJI"
) {
    val accentColor = LocalKitsugiAccent.current

    // Obtain ViewModel
    val viewModel: StudioDetailViewModel = viewModel(key = "studio_${source}_${studioId}")

    // Load studio in ViewModel
    LaunchedEffect(studioId, source) {
        viewModel.loadStudio(studioId, source, name)
    }

    // Collect state from ViewModel
    val state by viewModel.state.collectAsState()
    val isFavourite by viewModel.isFavourite.collectAsState()
    val isAniListSource = source.lowercase() == "anilist"
    val context = LocalContext.current
    val isAniListConnected = remember { com.kitsugi.animelist.data.auth.ExternalAuthManager.getAniListToken(context) != null }
    val showFavouriteButton = isAniListSource || isAniListConnected

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
                var activeGalleryItems by remember { mutableStateOf<List<GalleryItem>>(emptyList()) }
                var activeGalleryIndex  by remember { mutableStateOf(0) }
                val galleryItems by viewModel.galleryItems.collectAsState()

                if (isLandscape) {
                    val configuration = LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp
                    val leftPanelWeight = when {
                        screenWidth >= 1200 -> 0.28f
                        screenWidth >= 840  -> 0.32f
                        else                -> 0.38f
                    }
                    val rightPanelWeight = 1f - leftPanelWeight
                    // ── LANDSCAPE: Sol meta paneli + Sağ media grid ──
                    KitsugiPageEnter {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Sol panel: Banner + stüdyo bilgileri
                            StudioDetailLeftPanel(
                                detail = detail,
                                source = source,
                                studioId = studioId,
                                accentColor = accentColor,
                                isFavourite = isFavourite,
                                showFavouriteButton = showFavouriteButton,
                                galleryItems = galleryItems,
                                onBackClick = onBackClick,
                                onToggleFavourite = { viewModel.toggleFavourite() },
                                onGalleryClick = { items, idx ->
                                    activeGalleryItems = items
                                    activeGalleryIndex = idx
                                },
                                modifier = Modifier.weight(leftPanelWeight)
                            )
                            // Sağ panel: Media grid
                            Column(
                                modifier = Modifier
                                    .weight(rightPanelWeight)
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
                                            StudioMediaGridItem(work = work, titleLanguage = titleLanguage, onMediaClick = onMediaClick)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Gallery overlay for landscape
                    if (activeGalleryItems.isNotEmpty()) {
                        KitsugiImageGalleryDialog(
                            galleryItems = activeGalleryItems,
                            initialIndex = activeGalleryIndex,
                            title = detail.name,
                            onDismiss = { activeGalleryItems = emptyList() }
                        )
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
                                        onBackClick = onBackClick,
                                        isFavourite = isFavourite,
                                        showFavouriteButton = showFavouriteButton,
                                        onToggleFavourite = { viewModel.toggleFavourite() },
                                        onGalleryClick = if (galleryItems.isNotEmpty()) {
                                            {
                                                activeGalleryItems = galleryItems
                                                activeGalleryIndex = 0
                                            }
                                        } else null
                                    )
                                }

                                // 2. About section (MAL/Jikan usually has this)
                                if (!detail.about.isNullOrBlank()) {
                                    item(span = { GridItemSpan(3) }) {
                                        StudioAboutSection(
                                            about = detail.about,
                                            onGalleryClick = { items, idx ->
                                                activeGalleryItems = items
                                                activeGalleryIndex = idx
                                            },
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
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
                                        titleLanguage = titleLanguage,
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
                                    IconButton(onClick = {
                                        val url = com.kitsugi.animelist.utils.ShareUtils.buildStudioUrl(source, studioId)
                                        com.kitsugi.animelist.utils.ShareUtils.shareText(context, detail.name, url)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Share,
                                            contentDescription = "Paylaş",
                                            tint = KitsugiColors.TextSecondary
                                        )
                                    }
                                    if (showFavouriteButton) {
                                        IconButton(onClick = { viewModel.toggleFavourite() }) {
                                            Icon(
                                                imageVector = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                contentDescription = if (isFavourite) "Favoriden Çıkar" else "Favori Yap",
                                                tint = if (isFavourite) accentColor else KitsugiColors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Gallery overlay for portrait
                    if (activeGalleryItems.isNotEmpty()) {
                        KitsugiImageGalleryDialog(
                            galleryItems = activeGalleryItems,
                            initialIndex = activeGalleryIndex,
                            title = detail.name,
                            onDismiss = { activeGalleryItems = emptyList() }
                        )
                    }
                } // end else (portrait)
            }
        }
    }
}
