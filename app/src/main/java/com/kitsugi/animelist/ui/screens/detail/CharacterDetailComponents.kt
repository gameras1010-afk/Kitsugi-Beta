package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle
import com.kitsugi.animelist.utils.toTurkishLanguage
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.selection.SelectionContainer
import com.kitsugi.animelist.data.remote.KitsugiCharacterDetail
import com.kitsugi.animelist.data.remote.GalleryItem
import com.kitsugi.animelist.data.remote.GalleryCategory
import com.kitsugi.animelist.ui.components.KitsugiMarkdownText
import com.kitsugi.animelist.utils.copyOnDoubleTap
import com.kitsugi.animelist.utils.toFriendlySourceLabel
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator

@Composable
internal fun MediaAppearanceRow(
    appearance: com.kitsugi.animelist.data.remote.KitsugiCharacterMediaAppearance,
    titleLanguage: String = "ROMAJI",
    onMediaClick: (mediaId: Int, mediaType: String, mediaSource: String) -> Unit
) {
    val displayTitle = appearance.getDisplayTitle(titleLanguage)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = { onMediaClick(appearance.mediaId, appearance.mediaType, appearance.source) })
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KitsugiColors.SurfaceSoft)
        ) {
            if (!appearance.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = appearance.imageUrl,
                    contentDescription = displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = displayTitle.take(2).uppercase(),
                        color = KitsugiColors.TextMuted,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${appearance.mediaType.replaceFirstChar { it.uppercase() }} • Rol: ${appearance.characterRole}",
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
internal fun VoiceActorRow(
    actor: com.kitsugi.animelist.data.remote.KitsugiVoiceActor,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KitsugiColors.SurfaceSoft)
        ) {
            if (!actor.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = actor.imageUrl,
                    contentDescription = actor.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = actor.name.take(2).uppercase(),
                        color = KitsugiColors.TextMuted,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = actor.name,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            val labelText = actor.language.toTurkishLanguage()
                .replaceFirstChar { it.uppercase() }
                .ifBlank { "Bilinmeyen" }
            Text(
                text = labelText,
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
internal fun CharacterDetailLeftPanel(
    detail: KitsugiCharacterDetail,
    source: String,
    characterId: Int,
    accentColor: Color,
    isFavourite: Boolean,
    showFavouriteButton: Boolean,
    galleryItems: List<GalleryItem>,
    leftPanelFocusRequester: FocusRequester,
    tabBarFocusRequester: FocusRequester,
    onBackClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    onGalleryClick: (List<GalleryItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            if (!detail.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = detail.imageUrl,
                    contentDescription = detail.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(leftPanelFocusRequester)
                        .focusProperties { right = tabBarFocusRequester }
                        .tvClickable {
                            onGalleryClick(galleryItems, 0)
                        },
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = detail.name.take(2).uppercase(),
                        color = accentColor,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                KitsugiColors.Background.copy(alpha = 0.05f),
                                KitsugiColors.Background.copy(alpha = 0.30f),
                                KitsugiColors.Background.copy(alpha = 0.72f),
                                KitsugiColors.Background
                            )
                        )
                    )
            )
            // Top action bar: Back (left) + Share/Favourite (right)
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
                    if (!detail.imageUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.22f))
                                .then(Modifier.padding(1.dp)),
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
                            val url = com.kitsugi.animelist.utils.ShareUtils.buildCharacterUrl(source, characterId)
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
        // İsim + native name + pill
        Column(modifier = Modifier.padding(16.dp)) {
            DetailPill(text = source.toFriendlySourceLabel().uppercase(), color = accentColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = detail.name,
                modifier = Modifier.copyOnDoubleTap(context, detail.name),
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            if (!detail.nativeName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail.nativeName,
                    modifier = Modifier.copyOnDoubleTap(context, detail.nativeName),
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
internal fun CharacterAboutTabContent(
    detail: KitsugiCharacterDetail,
    translatedBio: String?,
    preferredTranslator: String,
    accentColor: Color,
    onGalleryClick: (List<GalleryItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    SelectionContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (detail.alternativeNames.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(KitsugiColors.Surface)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Diğer İsimler",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = detail.alternativeNames.joinToString(", "),
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            val hasDemographics = detail.gender != null || detail.age != null || detail.birthday != null || detail.bloodType != null
            if (hasDemographics) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(KitsugiColors.Surface)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Bilgiler",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (detail.gender != null) InfoRow(label = "Cinsiyet", value = detail.gender)
                        if (detail.age != null) InfoRow(label = "Yaş", value = detail.age)
                        if (detail.birthday != null) InfoRow(label = "Doğum Günü", value = detail.birthday)
                        if (detail.bloodType != null) InfoRow(label = "Kan Grubu", value = detail.bloodType)
                    }
                }
            }
            val displayBio = translatedBio ?: detail.biography
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(KitsugiColors.Surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Biyografi",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (!detail.biography.isNullOrBlank()) {
                        IconButton(
                            onClick = { context.openTranslator(detail.biography, preferredTranslator) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Rounded.Translate, contentDescription = "Çevir", tint = accentColor)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("biography", displayBio))
                                android.widget.Toast.makeText(context, "Panoya kopyalandı", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Kopyala", tint = KitsugiColors.TextSecondary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (displayBio.isNullOrBlank()) {
                    Text("Biyografi bulunmuyor.", color = KitsugiColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                } else {
                    KitsugiMarkdownText(
                        text = displayBio,
                        onImageGalleryRequest = { urls, idx ->
                            val items = urls.map { url -> GalleryItem(url = url, category = GalleryCategory.OTHER, source = "Biyografi") }
                            onGalleryClick(items, idx)
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun CharacterAppearancesTabContent(
    detail: KitsugiCharacterDetail,
    titleLanguage: String,
    onMediaClick: (mediaId: Int, mediaType: String, mediaSource: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (detail.mediaAppearances.isEmpty()) {
            Text(
                text = "Yapım bilgisi bulunmuyor.",
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            detail.mediaAppearances.forEach { appearance ->
                MediaAppearanceRow(
                    appearance = appearance,
                    titleLanguage = titleLanguage,
                    onMediaClick = onMediaClick
                )
            }
        }
    }
}

@Composable
internal fun CharacterVoiceActorsTabContent(
    detail: KitsugiCharacterDetail,
    onStaffClick: (staffId: Int, source: String, name: String?, imageUrl: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (detail.voiceActors.isEmpty()) {
            Text(
                text = "Seslendiren sanatçı bulunmuyor.",
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            detail.voiceActors.forEach { actor ->
                VoiceActorRow(
                    actor = actor,
                    onClick = { onStaffClick(actor.id, actor.source, actor.name, actor.imageUrl) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CharacterPortraitHeroSection — Portrait hero image + gradient + action bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
internal fun CharacterPortraitHeroSection(
    detail: KitsugiCharacterDetail,
    source: String,
    characterId: Int,
    accentColor: Color,
    isFavourite: Boolean,
    isAniListSource: Boolean,
    galleryItems: List<GalleryItem>,
    onBackClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    onGalleryOpen: (List<GalleryItem>, Int) -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .height(380.dp)
    ) {
        if (!detail.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = detail.imageUrl,
                contentDescription = detail.name,
                modifier = androidx.compose.ui.Modifier.fillMaxSize().tvClickable {
                    if (galleryItems.isNotEmpty()) onGalleryOpen(galleryItems, 0)
                    else onGalleryOpen(
                        listOfNotNull(detail.imageUrl).map { url ->
                            GalleryItem(url = url, category = GalleryCategory.POSTER, source = "Jikan")
                        }, 0
                    )
                },
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize().background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = detail.name.take(2).uppercase(),
                    color = accentColor,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }
        // Gradient overlay
        Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        KitsugiColors.Background.copy(alpha = 0.05f),
                        KitsugiColors.Background.copy(alpha = 0.30f),
                        KitsugiColors.Background.copy(alpha = 0.72f),
                        KitsugiColors.Background
                    )
                )
            )
        )
        // Top Action Bar
        Row(
            modifier = androidx.compose.ui.Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = androidx.compose.ui.Modifier.size(40.dp).clip(CircleShape).background(KitsugiColors.Background.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = KitsugiColors.TextPrimary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!detail.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = androidx.compose.ui.Modifier.size(40.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { onGalleryOpen(galleryItems, 0) }) {
                            Icon(Icons.Rounded.Image, contentDescription = "Galeri", tint = accentColor)
                        }
                    }
                }
                Box(
                    modifier = androidx.compose.ui.Modifier.size(40.dp).clip(CircleShape).background(KitsugiColors.Background.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = {
                        val url = com.kitsugi.animelist.utils.ShareUtils.buildCharacterUrl(source, characterId)
                        com.kitsugi.animelist.utils.ShareUtils.shareText(context, detail.name, url)
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Paylaş", tint = KitsugiColors.TextPrimary)
                    }
                }
                if (isAniListSource) {
                    Box(
                        modifier = androidx.compose.ui.Modifier.size(40.dp).clip(CircleShape).background(KitsugiColors.Background.copy(alpha = 0.45f)),
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
        // Name + native name + pill
        Column(modifier = androidx.compose.ui.Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            DetailPill(text = source.toFriendlySourceLabel().uppercase(), color = accentColor)
            Spacer(modifier = androidx.compose.ui.Modifier.height(10.dp))
            Text(
                text = detail.name,
                modifier = androidx.compose.ui.Modifier.copyOnDoubleTap(context, detail.name),
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!detail.nativeName.isNullOrBlank()) {
                Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                Text(
                    text = detail.nativeName,
                    modifier = androidx.compose.ui.Modifier.copyOnDoubleTap(context, detail.nativeName),
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CharacterDetailStickyHeader — Floating mini-header + tab chips (portrait)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CharacterDetailStickyHeader(
    detail: KitsugiCharacterDetail,
    source: String,
    characterId: Int,
    tabs: List<String>,
    selectedTab: Int,
    showFloatingHeader: Boolean,
    isFavourite: Boolean,
    showFavouriteButton: Boolean,
    galleryItems: List<GalleryItem>,
    tabListState: androidx.compose.foundation.lazy.LazyListState,
    accentColor: Color,
    onBackClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    onGalleryOpen: (List<GalleryItem>, Int) -> Unit,
    onTabSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(selectedTab) {
        tabListState.animateScrollToItem(selectedTab)
    }
    Column(
        modifier = androidx.compose.ui.Modifier
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
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(KitsugiColors.Surface.copy(alpha = 0.92f))
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = KitsugiColors.TextPrimary)
                }
                Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                Text(
                    text = detail.name,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                )
                if (!detail.imageUrl.isNullOrBlank()) {
                    IconButton(onClick = { onGalleryOpen(galleryItems, 0) }) {
                        Icon(Icons.Rounded.Image, contentDescription = "Galeri", tint = accentColor)
                    }
                }
                IconButton(onClick = {
                    val url = com.kitsugi.animelist.utils.ShareUtils.buildCharacterUrl(source, characterId)
                    com.kitsugi.animelist.utils.ShareUtils.shareText(context, detail.name, url)
                }) {
                    Icon(Icons.Rounded.Share, contentDescription = "Paylaş", tint = KitsugiColors.TextSecondary)
                }
                if (showFavouriteButton) {
                    IconButton(onClick = onToggleFavourite) {
                        Icon(
                            imageVector = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isFavourite) "Favoriden Çıkar" else "Favori Yap",
                            tint = if (isFavourite) accentColor else KitsugiColors.TextSecondary
                        )
                    }
                }
            }
        }
        LazyRow(
            state = tabListState,
            modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs.size) { index ->
                val isSelected = selectedTab == index
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) accentColor else KitsugiColors.Surface)
                        .tvClickable(shape = RoundedCornerShape(999.dp)) { onTabSelected(index) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabs[index],
                        color = if (isSelected) KitsugiColors.Background else KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                    )
                }
            }
        }
    }
}
