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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import com.kitsugi.animelist.ui.components.KitsugiMarkdownText
import com.kitsugi.animelist.ui.components.KitsugiImageGalleryDialog
import com.kitsugi.animelist.ui.components.KitsugiPageEnter
import com.kitsugi.animelist.ui.components.KitsugiCinematicLoadingScreen
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle

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
                var activeGalleryImages by remember { mutableStateOf<List<String>>(emptyList()) }
                var activeGalleryIndex  by remember { mutableStateOf(0) }

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
                                    // Top Action Bar: Back (left) + Share & Favourite (right)
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 12.dp, top = 24.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(KitsugiColors.Background.copy(alpha = 0.45f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(onClick = onBackClick) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                    contentDescription = "Geri",
                                                    tint = KitsugiColors.TextPrimary
                                                )
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(KitsugiColors.Background.copy(alpha = 0.45f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(onClick = {
                                                    val url = com.kitsugi.animelist.utils.ShareUtils.buildStudioUrl(source, studioId)
                                                    com.kitsugi.animelist.utils.ShareUtils.shareText(context, detail.name, url)
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Share,
                                                        contentDescription = "Paylaş",
                                                        tint = KitsugiColors.TextPrimary
                                                    )
                                                }
                                            }

                                            if (showFavouriteButton) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(KitsugiColors.Background.copy(alpha = 0.45f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    IconButton(onClick = { viewModel.toggleFavourite() }) {
                                                        Icon(
                                                            imageVector = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                            contentDescription = if (isFavourite) "Favoriden Çıkar" else "Favori Yap",
                                                            tint = if (isFavourite) accentColor else KitsugiColors.TextPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
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
                                            KitsugiMarkdownText(
                                                text = detail.about,
                                                onImageGalleryRequest = { urls, idx ->
                                                    activeGalleryImages = urls
                                                    activeGalleryIndex = idx
                                                }
                                            )
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
                                            StudioMediaGridItem(work = work, titleLanguage = titleLanguage, onMediaClick = onMediaClick)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Gallery overlay for landscape
                    if (activeGalleryImages.isNotEmpty()) {
                        KitsugiImageGalleryDialog(
                            imageUrls = activeGalleryImages,
                            initialIndex = activeGalleryIndex,
                            title = detail.name,
                            onDismiss = { activeGalleryImages = emptyList() }
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
                                    isAniListSource = showFavouriteButton,
                                    onToggleFavourite = { viewModel.toggleFavourite() }
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
                                        KitsugiMarkdownText(
                                            text = detail.about,
                                            onImageGalleryRequest = { urls, idx ->
                                                activeGalleryImages = urls
                                                activeGalleryIndex = idx
                                            }
                                        )
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
                    if (activeGalleryImages.isNotEmpty()) {
                        KitsugiImageGalleryDialog(
                            imageUrls = activeGalleryImages,
                            initialIndex = activeGalleryIndex,
                            title = detail.name,
                            onDismiss = { activeGalleryImages = emptyList() }
                        )
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
    onBackClick: () -> Unit,
    isFavourite: Boolean = false,
    isAniListSource: Boolean = false,
    onToggleFavourite: () -> Unit = {}
) {
    val context = LocalContext.current

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

        // Top Action Bar: Back (left) + Share & Favourite (right)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(KitsugiColors.Background.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Geri",
                        tint = KitsugiColors.TextPrimary
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(KitsugiColors.Background.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = {
                        val url = com.kitsugi.animelist.utils.ShareUtils.buildStudioUrl(source, detail.id)
                        com.kitsugi.animelist.utils.ShareUtils.shareText(context, detail.name, url)
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Paylaş",
                            tint = KitsugiColors.TextPrimary
                        )
                    }
                }

                if (isAniListSource) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(KitsugiColors.Background.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onToggleFavourite) {
                            Icon(
                                imageVector = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (isFavourite) "Favoriden Çıkar" else "Favori Yap",
                                tint = if (isFavourite) accentColor else KitsugiColors.TextPrimary
                            )
                        }
                    }
                }
            }
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
    titleLanguage: String = "ROMAJI",
    onMediaClick: (mediaId: Int, mediaType: String, mediaSource: String) -> Unit
) {
    val displayTitle = work.getDisplayTitle(titleLanguage)

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
                    contentDescription = displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayTitle.take(2).uppercase(),
                        color = KitsugiColors.TextMuted,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = displayTitle,
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}


