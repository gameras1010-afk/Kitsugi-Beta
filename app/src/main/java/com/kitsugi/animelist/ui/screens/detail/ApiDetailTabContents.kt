package com.kitsugi.animelist.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.remote.MdbListRatings
import com.kitsugi.animelist.data.remote.KitsugiMediaDetail
import com.kitsugi.animelist.data.remote.KitsugiStreamingEpisode
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.screens.stream.KitsugiStreamActivity
import com.kitsugi.animelist.utils.KitsugiTranslateUtils.openTranslator

import com.kitsugi.animelist.data.remote.JikanSearchResult

/**
 * Tab 0 — Overview (Bilgi) content for ApiResultDetailPage.
 */
@Composable
internal fun ApiDetailOverviewTab(
    result: JikanSearchResult,
    detail: KitsugiMediaDetail?,
    displaySynopsis: String?,
    isDetailLoading: Boolean,
    isTranslating: Boolean,
    onSearchQuery: (String) -> Unit,
    onStudioClick: (id: Int, source: String, name: String?, url: String?) -> Unit,
    onTranslateClick: () -> Unit,
    onCopyClick: () -> Unit,
    mdbListRatings: MdbListRatings? = null,
    mdbListLoading: Boolean = false,
    mdbListShowImdb: Boolean = true,
    mdbListShowTomatoes: Boolean = true,
    mdbListShowMetacritic: Boolean = true,
    mdbListShowAudience: Boolean = false,
    mdbListShowLetterboxd: Boolean = false,
    mdbListShowTmdb: Boolean = false,
    mdbListShowTrakt: Boolean = false,
    onSettingsClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        ApiSynopsisCard(
            synopsis = displaySynopsis,
            isLoading = isDetailLoading || (!isDetailLoading && detail?.synopsis != null && isTranslating),
            onTranslateClick = onTranslateClick,
            onCopyClick = onCopyClick
        )

        if (detail != null) {
            Spacer(modifier = Modifier.height(14.dp))
            DetailOverviewStatsCard(detail = detail)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // MDBList external ratings
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
            mediaTitle = result.title
        )

        if (!detail?.genres.isNullOrEmpty()) {
            ApiGenresChipRow(
                genres = detail!!.genres,
                onGenreClick = onSearchQuery
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (detail != null && (detail.studios.isNotEmpty() || detail.producers.isNotEmpty())) {
            KitsugiStudiosCard(
                studios = detail.studios,
                producers = detail.producers,
                onStudioClick = { studio ->
                    val resolvedSource = when {
                        result.source.lowercase() == "anilist" -> "anilist"
                        result.type == MediaType.Anime || result.type == MediaType.Manga -> "jikan"
                        else -> "tmdb"
                    }
                    onStudioClick(studio.id, resolvedSource, studio.name, null)
                },
                onProducerClick = { producer ->
                    val resolvedSource = when {
                        result.source.lowercase() == "anilist" -> "anilist"
                        result.type == MediaType.Anime || result.type == MediaType.Manga -> "jikan"
                        else -> "tmdb"
                    }
                    onStudioClick(producer.id, resolvedSource, producer.name, null)
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (!detail?.tags.isNullOrEmpty()) {
            KitsugiTagsCard(
                tags = detail!!.tags,
                onTagClick = onSearchQuery
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (!detail?.trailerUrl.isNullOrBlank()) {
            KitsugiTrailerCard(trailerUrl = detail!!.trailerUrl!!)
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (detail != null) {
            ApiInfoSection(
                detail = detail,
                mediaType = result.type,
                onSearchQuery = onSearchQuery
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (detail != null && (detail.streamingLinks.isNotEmpty() || detail.externalLinks.isNotEmpty())) {
            KitsugiLinksCard(
                streamingLinks = detail.streamingLinks,
                externalLinks = detail.externalLinks
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (!detail?.openings.isNullOrEmpty() || !detail?.endings.isNullOrEmpty()) {
            KitsugiThemesList(
                openings = detail?.openings.orEmpty(),
                endings = detail?.endings.orEmpty()
            )
        }
    }
}

/**
 * Tab 6 — Episodes content for ApiResultDetailPage.
 */
@Composable
internal fun ApiDetailEpisodesTab(
    state: DetailTabState<List<KitsugiStreamingEpisode>>,
    episodeRatings: Map<Pair<Int, Int>, Double>,
    targetSeason: Int?,
    totalSeasons: Int?,
    resolvedTmdbId: Int?,
    displayTitle: String,
    displaySource: String,
    displayMalId: Int,
    displayRealMalId: Int?,
    displayImageUrl: String?,
    displayTitleEnglish: String?,
    displayTitleRomaji: String?,
    displayTitleNative: String?,
    displayYear: Int?,
    isMovie: Boolean = false,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeOptionsRequested: (KitsugiStreamingEpisode) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    EpisodesTabContent(
        state = state,
        episodeRatings = episodeRatings,
        targetSeason = targetSeason,
        totalSeasons = totalSeasons,
        onSeasonSelected = onSeasonSelected,
        onEpisodeClick = { episode ->
            val epNum = episode.episodeNumber
            if (epNum != null && epNum > 0) {
                val streamMalId = if (displaySource.lowercase() == "anilist") {
                    displayRealMalId
                } else {
                    displayMalId
                }
                val rawStableId = if (displaySource.lowercase() == "anilist") displayMalId else null
                val streamAniListId = rawStableId?.let {
                    if (it >= 100_000_000) it - 100_000_000 else it
                }
                KitsugiStreamActivity.start(
                    context = context,
                    malId = streamMalId,
                    aniListId = streamAniListId,
                    tmdbId = resolvedTmdbId,
                    episode = epNum,
                    season = targetSeason ?: 1,
                    isMovie = isMovie,
                    title = displayTitle,
                    posterUrl = displayImageUrl,
                    titleEnglish = displayTitleEnglish,
                    titleRomaji = displayTitleRomaji,
                    titleNative = displayTitleNative,
                    startYear = displayYear
                )
            } else {
                onEpisodeOptionsRequested(episode)
            }
        },
        onRatingClick = { season, episode ->
            val tmdbId = resolvedTmdbId
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
