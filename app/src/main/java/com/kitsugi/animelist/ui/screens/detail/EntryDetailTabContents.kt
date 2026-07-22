package com.kitsugi.animelist.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.remote.MdbListRatings
import com.kitsugi.animelist.data.remote.KitsugiMediaDetail
import com.kitsugi.animelist.data.remote.KitsugiStreamingEpisode
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.screens.stream.KitsugiStreamActivity
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
    preferredTranslator: String = "DEFAULT"
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
            }
        )

        if (detail?.nextAiringEpisode != null) {
            AiringCountdownCard(nextAiring = detail.nextAiringEpisode)
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
