package com.kitsugi.animelist.ui.screens.explore

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.R
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.ui.components.KitsugiHorizontalMediaSection

fun LazyListScope.defaultExploreSections(
    viewModel: ExploreViewModel,
    filteredTopAnime: List<JikanSearchResult>,
    filteredAiringAnime: List<JikanSearchResult>,
    filteredUpcomingAnime: List<JikanSearchResult>,
    filteredNewlyAddedAnime: List<JikanSearchResult>,
    filteredTopManga: List<JikanSearchResult>,
    filteredPublishingManga: List<JikanSearchResult>,
    filteredTrendingManga: List<JikanSearchResult>,
    filteredNewlyAddedManga: List<JikanSearchResult>,
    isAlreadyInList: (JikanSearchResult) -> Boolean,
    getMediaEntry: (JikanSearchResult) -> MediaEntry?,
    onItemClick: (JikanSearchResult) -> Unit,
    onLongClickItem: (JikanSearchResult) -> Unit,
    onSeeAllSection: (title: String, categoryType: ExploreCategoryType, results: List<JikanSearchResult>) -> Unit,
    titleLanguage: String,
    scoreFormat: String,
    hideScores: Boolean,
    blurAdultMedia: Boolean,
    context: android.content.Context
) {
    // ─── POPÜLER ANİME ───
    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_top_anime),
            results = filteredTopAnime,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_top_anime), ExploreCategoryType.TOP_ANIME, filteredTopAnime) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
        Spacer(modifier = Modifier.height(26.dp))
    }

    // ─── YAYINDAKİ ANİME ───
    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_airing_anime),
            results = filteredAiringAnime,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_airing_anime), ExploreCategoryType.AIRING_ANIME, filteredAiringAnime) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
        Spacer(modifier = Modifier.height(26.dp))
    }

    // ─── YAKLAŞAN ANİME ───
    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_upcoming_anime),
            results = filteredUpcomingAnime,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_upcoming_anime), ExploreCategoryType.UPCOMING_ANIME, filteredUpcomingAnime) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
        Spacer(modifier = Modifier.height(26.dp))
    }

    // ─── YENİ EKLENEN ANİME ───
    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_newly_added_anime),
            results = filteredNewlyAddedAnime,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_newly_added_anime), ExploreCategoryType.NEWLY_ADDED_ANIME, filteredNewlyAddedAnime) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
        Spacer(modifier = Modifier.height(26.dp))
    }

    // ─── POPÜLER MANGA ───
    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_top_manga),
            results = filteredTopManga,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_top_manga), ExploreCategoryType.TOP_MANGA, filteredTopManga) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
        Spacer(modifier = Modifier.height(26.dp))
    }

    // ─── YAYINDAKİ MANGA ───
    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_publishing_manga),
            results = filteredPublishingManga,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_publishing_manga), ExploreCategoryType.PUBLISHING_MANGA, filteredPublishingManga) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
        Spacer(modifier = Modifier.height(26.dp))
    }

    // ─── TREND MANGA ───
    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_trending_manga),
            results = filteredTrendingManga,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_trending_manga), ExploreCategoryType.TRENDING_MANGA, filteredTrendingManga) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
        Spacer(modifier = Modifier.height(26.dp))
    }

    // ─── YENİ EKLENEN MANGA ───
    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_newly_added_manga),
            results = filteredNewlyAddedManga,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_newly_added_manga), ExploreCategoryType.NEWLY_ADDED_MANGA, filteredNewlyAddedManga) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
    }
}
