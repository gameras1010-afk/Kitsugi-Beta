@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.remote.ApiSearchSelection
import com.kitsugi.animelist.data.remote.GalleryCategory
import com.kitsugi.animelist.data.remote.GalleryItem
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.KitsugiMediaDetail
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.screens.stream.KitsugiStreamActivity
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayScore
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle

/**
 * Left panel for [ApiResultDetailPage] in landscape mode.
 *
 * Contains the [KitsugiDetailHero] and all action buttons (Watch, Read, Add/Edit, Source link).
 */
@Composable
fun ApiDetailLeftPanel(
    displayResult: JikanSearchResult,
    detailState: KitsugiMediaDetail?,
    resolvedTmdbId: Int?,
    galleryItems: List<GalleryItem>,
    existingEntry: MediaEntry?,
    isConnected: Boolean,
    isFavorite: Boolean,
    showFavouriteButton: Boolean,
    synopsisForSave: String?,
    titleLanguage: String,
    scoreFormat: String,
    hideScores: Boolean,
    showAnimeLogos: Boolean,
    logoUrl: String?,
    blurAdultMedia: Boolean,
    onBackClick: () -> Unit,
    onAddClick: (ApiSearchSelection) -> Unit,
    onEditClick: (MediaEntry) -> Unit,
    onToggleFavoriteClick: ((ApiSearchSelection) -> Unit)?,
    onReadMangaClick: (() -> Unit)?,
    onGalleryOpen: (items: List<GalleryItem>, index: Int) -> Unit,
    onShowAuthWarning: () -> Unit,
    leftPanelFocusRequester: FocusRequester,
    tabBarFocusRequester: FocusRequester,
    targetSeason: Int = 1
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val accentColor = LocalKitsugiAccent.current
    val externalUrl = com.kitsugi.animelist.utils.ShareUtils.buildExternalMediaUrl(
        displayResult.source,
        displayResult.malId,
        displayResult.tmdbId,
        displayResult.type
    )

    KitsugiDetailHero(
        title = displayResult.getDisplayTitle(titleLanguage),
        subtitle = displayResult.subtitle,
        imageUrl = displayResult.imageUrl,
        logoUrl = if (showAnimeLogos) logoUrl else null,
        source = displayResult.source,
        typeLabel = when (displayResult.type) {
            MediaType.Anime  -> "ANIME"
            MediaType.Movie  -> "FİLM"
            MediaType.TvShow -> "DİZİ"
            else             -> "MANGA"
        },
        year = displayResult.year?.toString(),
        isAdult = displayResult.isAdult,
        onBackClick = onBackClick,
        blurAdultMedia = blurAdultMedia,
        onPosterClick = {
            val clickedUrl = displayResult.imageUrl
            if (galleryItems.isNotEmpty()) {
                val index = galleryItems.indexOfFirst { item ->
                    item.url == clickedUrl || 
                    (!clickedUrl.isNullOrBlank() && item.url.substringAfterLast("/") == clickedUrl.substringAfterLast("/"))
                }.coerceAtLeast(0)
                onGalleryOpen(galleryItems, index)
            } else {
                if (!clickedUrl.isNullOrBlank()) {
                    onGalleryOpen(listOf(GalleryItem(url = clickedUrl, category = GalleryCategory.POSTER, source = displayResult.source)), 0)
                }
            }
        },
        onGalleryClick = if (galleryItems.isNotEmpty()) {{ onGalleryOpen(galleryItems, 0) }} else null,
        scoreLabel = if (!hideScores) displayResult.getDisplayScore(scoreFormat, hideScores) else null,
        alreadyInList = existingEntry != null,
        isFavorite = isFavorite,
        showFavoriteButton = showFavouriteButton,
        onToggleFavoriteClick = if (onToggleFavoriteClick != null) {
            { onToggleFavoriteClick(ApiSearchSelection(result = displayResult, synopsis = synopsisForSave)) }
        } else null,
        totalEpisodes = displayResult.total,
        nextAiring = detailState?.nextAiringEpisode
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── İzle butonu ──
            if (displayResult.type == MediaType.Anime || displayResult.type == MediaType.TvShow || displayResult.type == MediaType.Movie) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(accentColor)
                        .focusRequester(leftPanelFocusRequester)
                        .focusProperties { right = tabBarFocusRequester }
                        .tvClickable(shape = RoundedCornerShape(18.dp)) {
                            val streamMalId = if (displayResult.source.lowercase() == "anilist") displayResult.realMalId else displayResult.malId
                            val rawStableId = if (displayResult.source.lowercase() == "anilist") displayResult.malId else null
                            val streamAniListId = rawStableId?.let { if (it >= 100_000_000) it - 100_000_000 else it }
                            KitsugiStreamActivity.start(
                                context = context,
                                malId = streamMalId,
                                aniListId = streamAniListId,
                                tmdbId = detailState?.tmdbId ?: resolvedTmdbId,
                                episode = 1,
                                isMovie = displayResult.type == MediaType.Movie,
                                season = targetSeason,
                                title = displayResult.title,
                                posterUrl = displayResult.imageUrl,
                                titleEnglish = displayResult.titleEnglish,
                                titleRomaji = detailState?.titleRomaji,
                                titleNative = detailState?.titleNative,
                                startYear = displayResult.year
                            )
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, tint = KitsugiColors.Background)
                        Text(text = "İzle", color = KitsugiColors.Background, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ── Oku butonu (Manga) ──
            if (displayResult.type == MediaType.Manga && onReadMangaClick != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(accentColor)
                        .focusRequester(leftPanelFocusRequester)
                        .focusProperties { right = tabBarFocusRequester }
                        .tvClickable(shape = RoundedCornerShape(18.dp), onClick = onReadMangaClick)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.AutoStories, contentDescription = null, tint = KitsugiColors.Background)
                        Text(text = "Oku", color = KitsugiColors.Background, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            val fallbackFocusMod = if (
                displayResult.type != MediaType.Anime &&
                displayResult.type != MediaType.TvShow &&
                displayResult.type != MediaType.Movie &&
                displayResult.type != MediaType.Manga
            ) Modifier.focusRequester(leftPanelFocusRequester) else Modifier

            // ── Düzenle / Listeye Ekle ──
            if (existingEntry != null) {
                ApiActionButton(
                    text = "✎ Düzenle",
                    primary = true,
                    enabled = true,
                    modifier = fallbackFocusMod.focusProperties { right = tabBarFocusRequester },
                    onClick = { onEditClick(existingEntry) }
                )
            } else {
                ApiActionButton(
                    text = "Listeye Ekle",
                    primary = true,
                    enabled = true,
                    modifier = fallbackFocusMod.focusProperties { right = tabBarFocusRequester },
                    onClick = {
                        if (isConnected) {
                            onAddClick(ApiSearchSelection(result = displayResult, synopsis = synopsisForSave))
                        } else {
                            onShowAuthWarning()
                        }
                    }
                )
            }

            // ── Kaynakta Aç ──
            if (externalUrl != null) {
                val sourceLinkLabel = when (displayResult.source.lowercase()) {
                    "anilist"       -> "AniList'te Aç"
                    "jikan", "mal"  -> "MAL'da Gör"
                    "tmdb"          -> "TMDB'de Aç"
                    else            -> "Kaynakta Aç"
                }
                ApiActionButton(
                    text = sourceLinkLabel,
                    primary = false,
                    enabled = true,
                    modifier = Modifier.focusProperties { right = tabBarFocusRequester },
                    onClick = { uriHandler.openUri(externalUrl) }
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
