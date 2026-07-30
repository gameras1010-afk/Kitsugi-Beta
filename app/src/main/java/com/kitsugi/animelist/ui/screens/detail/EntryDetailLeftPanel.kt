package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.local.MangaMappingEntity
import com.kitsugi.animelist.data.remote.GalleryCategory
import com.kitsugi.animelist.data.remote.GalleryItem
import com.kitsugi.animelist.data.remote.KitsugiMediaDetail
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.ui.screens.stream.KitsugiStreamActivity

/**
 * Left panel for [MediaEntryDetailPage] in landscape mode.
 *
 * Contains [DetailHero] and [QuickActions] buttons.
 */
@Composable
fun EntryDetailLeftPanel(
    entry: MediaEntry,
    detailState: KitsugiMediaDetail?,
    logoUrl: String?,
    galleryItems: List<GalleryItem>,
    mangaMapping: MangaMappingEntity?,
    resolvedTmdbId: Int?,
    showAnimeLogos: Boolean,
    showFavouriteButton: Boolean,
    titleLanguage: String,
    blurAdultMedia: Boolean,
    onBackClick: () -> Unit,
    onIncrementProgressClick: () -> Unit,
    onToggleFavoriteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReadMangaClick: ((MangaMappingEntity?) -> Unit)?,
    onUnlinkMangaClick: () -> Unit,
    onGalleryOpen: (List<GalleryItem>, Int) -> Unit,
    leftPanelFocusRequester: FocusRequester,
    tabBarFocusRequester: FocusRequester,
    targetSeason: Int = 1
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val externalUrl = buildExternalUrl(entry)

    DetailHero(
        entry = entry,
        logoUrl = if (showAnimeLogos) logoUrl else null,
        onBackClick = onBackClick,
        titleLanguage = titleLanguage,
        blurAdultMedia = blurAdultMedia,
        onPosterClick = { clickedUrl ->
            if (galleryItems.isNotEmpty()) {
                val index = galleryItems.indexOfFirst { item ->
                    item.url == clickedUrl || 
                    (!clickedUrl.isNullOrBlank() && item.url.substringAfterLast("/") == clickedUrl.substringAfterLast("/"))
                }.coerceAtLeast(0)
                onGalleryOpen(galleryItems, index)
            } else if (!clickedUrl.isNullOrBlank()) {
                onGalleryOpen(
                    listOf(
                        GalleryItem(
                            url = clickedUrl,
                            category = GalleryCategory.POSTER,
                            source = entry.source
                        )
                    ), 0
                )
            }
        },
        onGalleryClick = if (galleryItems.isNotEmpty()) {
            { onGalleryOpen(galleryItems, 0) }
        } else null,
        nextAiring = detailState?.nextAiringEpisode,
        showFavoriteButton = showFavouriteButton,
        onToggleFavoriteClick = onToggleFavoriteClick
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(12.dp))
        QuickActions(
            entry = entry,
            externalUrl = externalUrl,
            onIncrementProgressClick = onIncrementProgressClick,
            onToggleFavoriteClick = onToggleFavoriteClick,
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick,
            onOpenExternalClick = {
                if (externalUrl != null) {
                    uriHandler.openUri(externalUrl)
                }
            },
            onWatchClick = if (entry.type == com.kitsugi.animelist.model.MediaType.Anime || entry.type == com.kitsugi.animelist.model.MediaType.TvShow || entry.type == com.kitsugi.animelist.model.MediaType.Movie) {
                {
                    val streamMalId = if (entry.source.lowercase() == "anilist") {
                        detailState?.realMalId ?: entry.malId
                    } else {
                        entry.id
                    }
                    val rawAniListId = if (entry.source.lowercase() == "anilist") entry.id else null
                    val streamAniListId = rawAniListId?.let {
                        if (it >= 100_000_000) it - 100_000_000 else it
                    }
                    KitsugiStreamActivity.start(
                        context = context,
                        malId = streamMalId,
                        aniListId = streamAniListId,
                        tmdbId = entry.tmdbId ?: detailState?.tmdbId ?: resolvedTmdbId,
                        episode = 1,
                        season = targetSeason,
                        isMovie = entry.type == com.kitsugi.animelist.model.MediaType.Movie,
                        title = entry.title,
                        posterUrl = entry.imageUrl,
                        titleEnglish = detailState?.titleEnglish,
                        titleRomaji = detailState?.titleRomaji,
                        titleNative = detailState?.titleNative,
                        startYear = entry.year
                    )
                }
            } else null,
            onReadClick = if (entry.type == com.kitsugi.animelist.model.MediaType.Manga) {
                {
                    if (onReadMangaClick != null) {
                        onReadMangaClick(mangaMapping)
                    }
                }
            } else null,
            mangaMapping = mangaMapping,
            onLinkMangaClick = if (entry.type == com.kitsugi.animelist.model.MediaType.Manga) {
                {
                    if (onReadMangaClick != null) {
                        onReadMangaClick(null)
                    }
                }
            } else null,
            onUnlinkMangaClick = onUnlinkMangaClick,
            primaryFocusRequester = leftPanelFocusRequester,
            tabBarFocusRequester = tabBarFocusRequester
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}
