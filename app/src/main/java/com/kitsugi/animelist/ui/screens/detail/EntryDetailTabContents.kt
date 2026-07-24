package com.kitsugi.animelist.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.GalleryCategory
import com.kitsugi.animelist.data.remote.GalleryItem
import com.kitsugi.animelist.data.remote.MdbListRatings
import com.kitsugi.animelist.data.remote.KitsugiMediaDetail
import com.kitsugi.animelist.data.remote.KitsugiStreamingEpisode
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.screens.stream.KitsugiStreamActivity
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator

/**
 * Tab 0 — Overview (Bilgi) content for MediaEntryDetailPage.
 */
@Composable
internal fun EntryDetailOverviewTab(
    entry: MediaEntry,
    detail: KitsugiMediaDetail?,
    synopsisState: SynopsisState,
    originalSynopsis: String?,
    externalUrl: String?,
    onSearchQuery: (String) -> Unit,
    onStudioClick: (id: Int, source: String, name: String?, url: String?) -> Unit,
    onGenreClick: (String) -> Unit = {},
    onTagClick: (String) -> Unit = {},
    mdbListRatings: MdbListRatings? = null,
    mdbListLoading: Boolean = false,
    mdbListShowImdb: Boolean = true,
    mdbListShowTomatoes: Boolean = true,
    mdbListShowMetacritic: Boolean = true,
    mdbListShowAudience: Boolean = false,
    mdbListShowLetterboxd: Boolean = false,
    mdbListShowTmdb: Boolean = false,
    mdbListShowTrakt: Boolean = false,
    onSettingsClick: (() -> Unit)? = null,
    preferredTranslator: String = "DEFAULT",
    onImageGalleryRequest: ((urls: List<String>, index: Int) -> Unit)? = null,
    galleryItems: List<GalleryItem> = emptyList(),
    onGalleryItemRequest: ((items: List<GalleryItem>, index: Int) -> Unit)? = null
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.padding(horizontal = 0.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DetailSynopsisCard(
            synopsisState = synopsisState,
            originalText = originalSynopsis,
            onTranslateClick = { textToTranslate ->
                context.openTranslator(textToTranslate, preferredTranslator)
            },
            onCopyClick = { textToCopy ->
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("synopsis", textToCopy))
                android.widget.Toast.makeText(context, "Panonya kopyalandı", android.widget.Toast.LENGTH_SHORT).show()
            },
            onImageGalleryRequest = onImageGalleryRequest
        )

        // Fanart.tv + çok kaynaklı galeri
        if (galleryItems.isNotEmpty()) {
            DetailGalleryCard(
                items = galleryItems,
                onItemClick = { index ->
                    onGalleryItemRequest?.invoke(galleryItems, index)
                }
            )
        }

        // İstatistikler Kartı (Puan Sırası, Oy Sayısı, Üyeler, Popülerlik)
        if (detail != null) {
            DetailOverviewStatsCard(detail = detail)
        }

        // MDBList harici puanlar
        ApiMdbListRatingCard(
            ratings = mdbListRatings,
            isLoading = mdbListLoading,
            showImdb = mdbListShowImdb,
            showTomatoes = mdbListShowTomatoes,
            showMetacritic = mdbListShowMetacritic,
            showAudience = mdbListShowAudience,
            showLetterboxd = mdbListShowLetterboxd,
            showTmdb = mdbListShowTmdb,
            showTrakt = mdbListShowTrakt,
            onSettingsClick = onSettingsClick,
            mediaTitle = entry.title
        )

        // Türler
        if (!detail?.genres.isNullOrEmpty()) {
            EntryGenresChipRow(
                genres = detail!!.genres,
                onGenreClick = onGenreClick
            )
        }

        // Stüdyolar + Yapımcılar
        if (detail != null &&
            (detail.studios.isNotEmpty() || detail.producers.isNotEmpty())) {
            KitsugiStudiosCard(
                studios = detail.studios,
                producers = detail.producers,
                onStudioClick = { studio ->
                    val resolvedSource = when {
                        entry.source.lowercase() == "anilist" -> "anilist"
                        entry.type == MediaType.Anime || entry.type == MediaType.Manga -> "jikan"
                        else -> "tmdb"
                    }
                    onStudioClick(studio.id, resolvedSource, studio.name, null)
                },
                onProducerClick = { producer ->
                    val resolvedSource = when {
                        entry.source.lowercase() == "anilist" -> "anilist"
                        entry.type == MediaType.Anime || entry.type == MediaType.Manga -> "jikan"
                        else -> "tmdb"
                    }
                    onStudioClick(producer.id, resolvedSource, producer.name, null)
                }
            )
        }

        // Etiketler (AniList)
        if (!detail?.tags.isNullOrEmpty()) {
            KitsugiTagsCard(
                tags = detail!!.tags,
                onTagClick = onTagClick
            )
        }

        // Fragman (inline YouTube player)
        if (!detail?.trailerUrl.isNullOrBlank()) {
            KitsugiTrailerCard(trailerUrl = detail!!.trailerUrl!!)
        }

        // Bilgi satırları
        if (detail != null) {
            EntryInfoSection(detail = detail, mediaType = entry.type, onSearchQuery = onSearchQuery)
        }

        // Yayıncı + Harici Linkler
        if (detail != null &&
            (detail.streamingLinks.isNotEmpty() || detail.externalLinks.isNotEmpty())) {
            KitsugiLinksCard(
                streamingLinks = detail.streamingLinks,
                externalLinks = detail.externalLinks
            )
        }

        // Açılış/Kapanış müzikleri
        if (!detail?.openings.isNullOrEmpty() || !detail?.endings.isNullOrEmpty()) {
            KitsugiThemesList(
                openings = detail?.openings.orEmpty(),
                endings = detail?.endings.orEmpty()
            )
        }

        // Tarihler
        DateInfoCard(entry = entry)

        if (externalUrl != null) {
            DetailExternalLinkCard(
                externalUrl = externalUrl
            )
        }
    }
}

/**
 * Fanart.tv + çok kaynaklı galeri önizleme kartı.
 * Yatay kaydırılabilir küçük resim listesi; her görselin üzerinde
 * kaynak (Fanart.tv / TMDB / Jikan) ve kategori (Logo / Arka Plan…) pill badge'leri gösterilir.
 */
@Composable
private fun DetailGalleryCard(
    items: List<GalleryItem>,
    onItemClick: (index: Int) -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(KitsugiColors.SurfaceSoft)
            .border(1.dp, KitsugiColors.Border, RoundedCornerShape(18.dp))
            .padding(vertical = 14.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Galeri",
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${items.size} görsel",
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(items) { index, item ->
                // Aspect ratio depends on category
                val aspectRatio = when (item.category) {
                    GalleryCategory.BACKDROP -> 16f / 9f
                    GalleryCategory.BANNER   -> 5.4f / 1f
                    GalleryCategory.POSTER   -> 2f / 3f
                    GalleryCategory.LOGO     -> 3f / 1.4f
                    else                     -> 1f
                }

                val badgeBgColor = when (item.source.lowercase()) {
                    "fanart.tv" -> Color(0xFF9C27B0)
                    "tmdb"      -> Color(0xFF00C853)
                    "jikan"     -> Color(0xFF00B0FF)
                    else        -> accentColor
                }

                Box(
                    modifier = Modifier
                        .height(100.dp)
                        .aspectRatio(aspectRatio)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, KitsugiColors.Border, RoundedCornerShape(12.dp))
                        .tvClickable(shape = RoundedCornerShape(12.dp)) { onItemClick(index) }
                ) {
                    AsyncImage(
                        model = item.url,
                        contentDescription = "${item.category.label} – ${item.source}",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Dark gradient at the bottom for readability
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.55f)
                                    )
                                )
                            )
                    )

                    // Badge row at the bottom-left
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Source badge (coloured)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(badgeBgColor)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.source,
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Category badge (neutral)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.category.label,
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 6 — Episodes content for MediaEntryDetailPage.
 */
@Composable
internal fun EntryDetailEpisodesTab(
    entry: MediaEntry,
    detailState: KitsugiMediaDetail?,
    state: DetailTabState<List<KitsugiStreamingEpisode>>,
    episodeRatings: Map<Pair<Int, Int>, Double>,
    targetSeason: Int?,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeOptionsRequested: (KitsugiStreamingEpisode) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    EpisodesTabContent(
        state = state,
        episodeRatings = episodeRatings,
        targetSeason = targetSeason,
        totalSeasons = detailState?.totalSeasons,
        onSeasonSelected = onSeasonSelected,
        onEpisodeClick = { episode ->
            val epNum = episode.episodeNumber
            if (epNum != null && epNum > 0) {
                // Decode IDs correctly before launching stream
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
                    tmdbId = entry.tmdbId ?: detailState?.tmdbId,
                    episode = epNum,
                    season = targetSeason ?: 1,
                    isMovie = entry.type == com.kitsugi.animelist.model.MediaType.Movie,
                    title = entry.title,
                    posterUrl = entry.imageUrl,
                    titleEnglish = detailState?.titleEnglish,
                    titleRomaji = detailState?.titleRomaji,
                    titleNative = detailState?.titleNative,
                    startYear = entry.year
                )
            } else {
                onEpisodeOptionsRequested(episode)
            }
        },
        onRatingClick = { season, episode ->
            val tmdbId = detailState?.tmdbId
            if (tmdbId != null && tmdbId > 0) {
                val url = "https://www.themoviedb.org/tv/$tmdbId/season/$season/episode/$episode"
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }.onFailure {
                    runCatching { uriHandler.openUri(url) }
                }
            }
        }
    )
}
