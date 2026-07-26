package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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
import com.kitsugi.animelist.data.remote.KitsugiStudioDetail
import com.kitsugi.animelist.data.remote.KitsugiStaffMediaWork
import com.kitsugi.animelist.data.remote.GalleryItem
import com.kitsugi.animelist.data.remote.GalleryCategory
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle
import com.kitsugi.animelist.utils.toFriendlySourceLabel
import com.kitsugi.animelist.ui.components.KitsugiMarkdownText

// Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Share

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StudioHeroHeader(
    detail: KitsugiStudioDetail,
    source: String,
    accentColor: Color,
    onBackClick: () -> Unit,
    isFavourite: Boolean = false,
    showFavouriteButton: Boolean = false,
    onToggleFavourite: () -> Unit = {},
    onGalleryClick: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current

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
                if (onGalleryClick != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onGalleryClick) {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = "Galeri",
                                tint = accentColor
                            )
                        }
                    }
                }
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

                if (showFavouriteButton) {
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
                DetailPill(
                    text = if (detail.isMain) "Ana Stüdyo" else "Yapımcı",
                    color = accentColor
                )

                DetailPill(
                    text = source.toFriendlySourceLabel().uppercase(),
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
internal fun StudioMediaGridItem(
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

@Composable
internal fun StudioAboutSection(
    about: String,
    onGalleryClick: (List<GalleryItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
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
            text = about,
            onImageGalleryRequest = { urls, idx ->
                val items = urls.map { url -> GalleryItem(url = url, category = GalleryCategory.OTHER, source = "Hakkında") }
                onGalleryClick(items, idx)
            }
        )
    }
}

@Composable
internal fun StudioDetailLeftPanel(
    detail: KitsugiStudioDetail,
    source: String,
    studioId: Int,
    accentColor: Color,
    isFavourite: Boolean,
    showFavouriteButton: Boolean,
    galleryItems: List<GalleryItem>,
    onBackClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    onGalleryClick: (List<GalleryItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
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
                    if (galleryItems.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = {
                                onGalleryClick(galleryItems, 0)
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Image,
                                    contentDescription = "Galeri",
                                    tint = accentColor
                                )
                            }
                        }
                    }
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
                StudioAboutSection(
                    about = detail.about,
                    onGalleryClick = onGalleryClick
                )
            }
        }
    }
}
