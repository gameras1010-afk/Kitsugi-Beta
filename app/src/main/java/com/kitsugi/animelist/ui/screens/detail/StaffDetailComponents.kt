package com.kitsugi.animelist.ui.screens.detail

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import com.kitsugi.animelist.data.remote.GalleryItem
import com.kitsugi.animelist.data.remote.GalleryCategory
import com.kitsugi.animelist.data.remote.KitsugiStaffDetail
import com.kitsugi.animelist.utils.copyOnDoubleTap
import com.kitsugi.animelist.utils.toFriendlySourceLabel
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator
import androidx.compose.ui.res.stringResource
import com.kitsugi.animelist.R

// Icon extension imports
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.ContentCopy

@Composable
internal fun StaffCharacterRoleCard(
    role: com.kitsugi.animelist.data.remote.KitsugiStaffCharacterRole,
    titleLanguage: String = "ROMAJI",
    onCharacterClick: (characterId: Int, characterSource: String, name: String?, imageUrl: String?) -> Unit,
    onMediaClick: (mediaId: Int, mediaType: String, mediaSource: String) -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val displayMediaTitle = role.mediaTitle
    // AniHyou PersonItemHorizontal-style: single compact row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .tvClickable(shape = RoundedCornerShape(16.dp)) {
                onCharacterClick(role.characterId, role.characterSource, role.characterName, role.characterImageUrl)
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Character image
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KitsugiColors.SurfaceSoft)
        ) {
            if (!role.characterImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = role.characterImageUrl,
                    contentDescription = role.characterName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = role.characterName.take(2).uppercase(),
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
                text = role.characterName,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${role.mediaTitle} • ${role.characterRole}",
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun StaffMediaWorkRow(
    work: com.kitsugi.animelist.data.remote.KitsugiStaffMediaWork,
    titleLanguage: String = "ROMAJI",
    onMediaClick: (mediaId: Int, mediaType: String, mediaSource: String) -> Unit
) {
    val displayTitle = work.getDisplayTitle(titleLanguage)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = { onMediaClick(work.mediaId, work.mediaType, work.source) })
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KitsugiColors.SurfaceSoft)
        ) {
            if (!work.mediaImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = work.mediaImageUrl,
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
                text = "${work.mediaType.replaceFirstChar { it.uppercase() }} \u2022 ${stringResource(R.string.detail_role_prefix, work.staffRole)}",
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
internal fun StaffDetailLeftPanel(
    detail: KitsugiStaffDetail,
    source: String,
    staffId: Int,
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
    val context = androidx.compose.ui.platform.LocalContext.current
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
                            listOf(
                                KitsugiColors.Background.copy(alpha = 0.05f),
                                KitsugiColors.Background.copy(alpha = 0.30f),
                                KitsugiColors.Background.copy(alpha = 0.72f),
                                KitsugiColors.Background
                            )
                        )
                    )
            )
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
                            contentDescription = stringResource(R.string.action_back),
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
                                    contentDescription = stringResource(R.string.action_gallery),
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
                            val url = com.kitsugi.animelist.utils.ShareUtils.buildStaffUrl(source, staffId)
                            com.kitsugi.animelist.utils.ShareUtils.shareText(context, detail.name, url)
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.action_share),
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
                                    contentDescription = if (isFavourite) stringResource(R.string.action_favourite_remove) else stringResource(R.string.action_favourite_add),
                                    tint = if (isFavourite) accentColor else KitsugiColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
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
internal fun StaffAboutTabContent(
    detail: KitsugiStaffDetail,
    translatedBio: String?,
    preferredTranslator: String,
    accentColor: Color,
    onGalleryClick: (List<GalleryItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.foundation.text.selection.SelectionContainer(modifier = modifier) {
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
                        text = stringResource(R.string.section_alternative_names),
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

            val hasDemographics = detail.gender != null || detail.age != null || detail.birthday != null || detail.homeTown != null || detail.occupation != null
            if (hasDemographics) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(KitsugiColors.Surface)
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_info),
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (detail.occupation != null) InfoRow(label = stringResource(R.string.info_label_occupation), value = detail.occupation)
                        if (detail.gender != null) InfoRow(label = stringResource(R.string.info_label_gender), value = detail.gender)
                        if (detail.age != null) InfoRow(label = stringResource(R.string.info_label_age), value = detail.age)
                        if (detail.birthday != null) InfoRow(label = stringResource(R.string.info_label_birthday), value = detail.birthday)
                        if (detail.homeTown != null) InfoRow(label = stringResource(R.string.info_label_hometown), value = detail.homeTown)
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
                        text = stringResource(R.string.section_biography),
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
                            Icon(
                                imageVector = Icons.Rounded.Translate,
                                contentDescription = stringResource(R.string.action_translate),
                                tint = accentColor
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("biography", displayBio))
                                android.widget.Toast.makeText(context, context.getString(R.string.toast_copied_clipboard), android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(R.string.action_copy),
                                tint = KitsugiColors.TextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (displayBio.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.empty_biography),
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    com.kitsugi.animelist.ui.components.KitsugiMarkdownText(
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
internal fun StaffCharacterRolesTabContent(
    detail: KitsugiStaffDetail,
    titleLanguage: String,
    onCharacterClick: (characterId: Int, characterSource: String, name: String?, imageUrl: String?) -> Unit,
    onMediaClick: (mediaId: Int, mediaType: String, mediaSource: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (detail.characterRoles.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_character_roles),
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            detail.characterRoles.forEach { role ->
                StaffCharacterRoleCard(
                    role = role,
                    titleLanguage = titleLanguage,
                    onCharacterClick = onCharacterClick,
                    onMediaClick = onMediaClick
                )
            }
        }
    }
}

@Composable
internal fun StaffMediaWorksTabContent(
    detail: KitsugiStaffDetail,
    titleLanguage: String,
    onMediaClick: (mediaId: Int, mediaType: String, mediaSource: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (detail.mediaWorks.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_media_works),
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            detail.mediaWorks.forEach { work ->
                StaffMediaWorkRow(
                    work = work,
                    titleLanguage = titleLanguage,
                    onMediaClick = onMediaClick
                )
            }
        }
    }
}

@Composable
internal fun StaffPortraitHeroSection(
    detail: KitsugiStaffDetail,
    source: String,
    staffId: Int,
    accentColor: Color,
    isFavourite: Boolean,
    showFavouriteButton: Boolean,
    galleryItems: List<GalleryItem>,
    onBackClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    onGalleryOpen: (List<GalleryItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp)
    ) {
        if (!detail.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = detail.imageUrl,
                contentDescription = detail.name,
                modifier = Modifier
                    .fillMaxSize()
                    .tvClickable {
                        onGalleryOpen(galleryItems, 0)
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

        // Gradient overlay
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
                        contentDescription = stringResource(R.string.action_back),
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
                            onGalleryOpen(galleryItems, 0)
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = stringResource(R.string.action_gallery),
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
                        val url = com.kitsugi.animelist.utils.ShareUtils.buildStaffUrl(source, staffId)
                        com.kitsugi.animelist.utils.ShareUtils.shareText(context, detail.name, url)
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.action_share),
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
                                contentDescription = if (isFavourite) stringResource(R.string.action_favourite_remove) else stringResource(R.string.action_favourite_add),
                                tint = if (isFavourite) accentColor else KitsugiColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Staff Name Info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            DetailPill(
                text = source.toFriendlySourceLabel().uppercase(),
                color = accentColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = detail.name,
                modifier = Modifier.copyOnDoubleTap(context, detail.name),
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!detail.nativeName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail.nativeName,
                    modifier = Modifier.copyOnDoubleTap(context, detail.nativeName),
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
internal fun StaffDetailStickyHeader(
    detail: KitsugiStaffDetail,
    source: String,
    staffId: Int,
    tabs: List<String>,
    selectedTab: Int,
    showFloatingHeader: Boolean,
    isFavourite: Boolean,
    showFavouriteButton: Boolean,
    galleryItems: List<GalleryItem>,
    accentColor: Color,
    onBackClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    onGalleryOpen: (List<GalleryItem>, Int) -> Unit,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(KitsugiColors.Surface.copy(alpha = 0.92f))
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
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
                        onGalleryOpen(galleryItems, 0)
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = stringResource(R.string.action_gallery),
                            tint = accentColor
                        )
                    }
                }
                IconButton(onClick = {
                    val url = com.kitsugi.animelist.utils.ShareUtils.buildStaffUrl(source, staffId)
                    com.kitsugi.animelist.utils.ShareUtils.shareText(context, detail.name, url)
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.action_share),
                        tint = KitsugiColors.TextSecondary
                    )
                }
                if (showFavouriteButton) {
                    IconButton(onClick = onToggleFavourite) {
                        Icon(
                            imageVector = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isFavourite) stringResource(R.string.action_favourite_remove) else stringResource(R.string.action_favourite_add),
                            tint = if (isFavourite) accentColor else KitsugiColors.TextSecondary
                        )
                    }
                }
            }
        }

        // Full-width tab row — each tab fills equally (AniHyou style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                val bgColor = if (isSelected) accentColor else KitsugiColors.SurfaceSoft
                val textColor = if (isSelected) KitsugiColors.Background else KitsugiColors.TextSecondary

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(bgColor)
                        .tvClickable(shape = RoundedCornerShape(999.dp)) { onTabSelected(index) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = textColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

