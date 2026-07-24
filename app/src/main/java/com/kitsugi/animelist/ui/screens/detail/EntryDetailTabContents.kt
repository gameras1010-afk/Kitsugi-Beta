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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                },
                onOpenGallery = { category ->
                    // İlk eşleşen görselden başla
                    val startIndex = if (category == null) {
                        0
                    } else {
                        galleryItems.indexOfFirst { it.category == category }.coerceAtLeast(0)
                    }
                    onGalleryItemRequest?.invoke(galleryItems, startIndex)
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
    onItemClick: (index: Int) -> Unit,
    onOpenGallery: ((category: GalleryCategory?) -> Unit)? = null
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
        var showCategoryMenu by remember { mutableStateOf(false) }

        // Mevcut kategorileri hesapla (sıralı)
        val availableCategories = remember(items) {
            val order = listOf(
                GalleryCategory.LOGO,
                GalleryCategory.BACKDROP,
                GalleryCategory.POSTER,
                GalleryCategory.CHARACTER,
                GalleryCategory.THUMBNAIL,
                GalleryCategory.BANNER,
                GalleryCategory.OTHER
            )
            order.filter { cat -> items.any { it.category == cat } }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Resimler",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${items.size} görsel · ${availableCategories.size} kategori",
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Kategori seçici buton
            Box {
                IconButton(
                    onClick = { showCategoryMenu = true }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = "Galeriyi Aç",
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false },
                    containerColor = KitsugiColors.Surface,
                ) {
                    // "Tümü" seçeneği
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "📂", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Tümü (${items.size})",
                                    color = KitsugiColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        },
                        onClick = {
                            showCategoryMenu = false
                            onOpenGallery?.invoke(null)
                        }
                    )

                    availableCategories.forEach { category ->
                        val count = items.count { it.category == category }
                        val emoji = when (category) {
                            GalleryCategory.LOGO      -> "🎨"
                            GalleryCategory.BACKDROP  -> "🖼"
                            GalleryCategory.POSTER    -> "📋"
                            GalleryCategory.CHARACTER -> "🎭"
                            GalleryCategory.THUMBNAIL -> "🌐"
                            GalleryCategory.BANNER    -> "🎫"
                            GalleryCategory.OTHER     -> "📁"
                        }
                        val label = when (category) {
                            GalleryCategory.LOGO      -> "Logo"
                            GalleryCategory.BACKDROP  -> "Arka Plan"
                            GalleryCategory.POSTER    -> "Poster"
                            GalleryCategory.CHARACTER -> "Karakter"
                            GalleryCategory.THUMBNAIL -> "Küçük Resim"
                            GalleryCategory.BANNER    -> "Afiş"
                            GalleryCategory.OTHER     -> "Diğer"
                        }
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "$label ($count)",
                                        color = KitsugiColors.TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            onClick = {
                                showCategoryMenu = false
                                onOpenGallery?.invoke(category)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val orderedCategories = listOf(
            GalleryCategory.LOGO,
            GalleryCategory.BACKDROP,
            GalleryCategory.POSTER,
            GalleryCategory.CHARACTER,
            GalleryCategory.THUMBNAIL,
            GalleryCategory.BANNER,
            GalleryCategory.OTHER
        )

        val grouped = androidx.compose.runtime.remember(items) {
            items.groupBy { it.category }
        }

        orderedCategories.forEach { category ->
            val categoryItems = grouped[category]
            if (!categoryItems.isNullOrEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    val emoji = when (category) {
                        GalleryCategory.LOGO -> "🎨"
                        GalleryCategory.BACKDROP -> "🖼"
                        GalleryCategory.POSTER -> "📋"
                        GalleryCategory.CHARACTER -> "🎭"
                        GalleryCategory.THUMBNAIL -> "🌐"
                        GalleryCategory.BANNER -> "🎫"
                        GalleryCategory.OTHER -> "📁"
                    }

                    val turkishLabel = when (category) {
                        GalleryCategory.LOGO -> "Logo"
                        GalleryCategory.BACKDROP -> "Arka Plan"
                        GalleryCategory.POSTER -> "Poster"
                        GalleryCategory.CHARACTER -> "Karakter"
                        GalleryCategory.THUMBNAIL -> "Küçük Resim"
                        GalleryCategory.BANNER -> "Afiş"
                        GalleryCategory.OTHER -> "Diğer"
                    }

                    Text(
                        text = "$emoji $turkishLabel",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(categoryItems) { _, item ->
                            val mainIndex = items.indexOf(item)

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
                                "anilist"   -> Color(0xFF3DB4F2)
                                "simkl"     -> Color(0xFFE50914)
                                "kitsu"     -> Color(0xFFFD5C63)
                                else        -> accentColor
                            }

                            Box(
                                modifier = Modifier
                                    .height(100.dp)
                                    .aspectRatio(aspectRatio)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, KitsugiColors.Border, RoundedCornerShape(12.dp))
                                    .tvClickable(shape = RoundedCornerShape(12.dp)) { onItemClick(mainIndex) }
                            ) {
                                AsyncImage(
                                    model = item.url,
                                    contentDescription = "${item.category.label} – ${item.source}",
                                    modifier = Modifier.matchParentSize(),
                                    contentScale = ContentScale.Crop
                                )

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

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
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
                            }
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
