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

fun LazyListScope.tmdbExploreSections(
    viewModel: ExploreViewModel,
    filteredTrendingAnime: List<JikanSearchResult>,
    filteredNewlyAddedAnime: List<JikanSearchResult>,
    filteredTrendingManga: List<JikanSearchResult>,
    filteredTopAnime: List<JikanSearchResult>,
    filteredAiringAnime: List<JikanSearchResult>,
    filteredMovieAnime: List<JikanSearchResult>,
    filteredTopManga: List<JikanSearchResult>,
    filteredUpcomingAnime: List<JikanSearchResult>,
    filteredPublishingManga: List<JikanSearchResult>,
    filteredSeasonalAnime: List<JikanSearchResult>,
    isAlreadyInList: (JikanSearchResult) -> Boolean,
    getMediaEntry: (JikanSearchResult) -> MediaEntry?,
    onItemClick: (JikanSearchResult) -> Unit,
    onLongClickItem: (JikanSearchResult) -> Unit,
    onSeeAllSection: (title: String, categoryType: ExploreCategoryType, results: List<JikanSearchResult>) -> Unit,
    onNavigateToWatchHistory: () -> Unit,
    titleLanguage: String,
    scoreFormat: String,
    hideScores: Boolean,
    blurAdultMedia: Boolean,
    context: android.content.Context
) {
    // ─── TMDB TREND MEDYALAR ───
    if (filteredTrendingAnime.isNotEmpty()) {
        item {
            KitsugiHorizontalMediaSection(
                title = "Trend Animeler",
                results = filteredTrendingAnime,
                isLoading = viewModel.isLoading,
                alreadyInList = isAlreadyInList,
                getMediaEntry = getMediaEntry,
                onItemClick = onItemClick,
                onLongClickItem = onLongClickItem,
                onSeeAllClick = { onSeeAllSection("Trend Animeler", ExploreCategoryType.TRENDING_ANIME, filteredTrendingAnime) },
                titleLanguage = titleLanguage,
                scoreFormat = scoreFormat,
                hideScores = hideScores,
                blurAdultMedia = blurAdultMedia
            )
            Spacer(modifier = Modifier.height(26.dp))
        }
    }

    // ─── TMDB POPÜLER MEDYALAR ───
    if (filteredNewlyAddedAnime.isNotEmpty()) {
        item {
            KitsugiHorizontalMediaSection(
                title = "Popüler Animeler",
                results = filteredNewlyAddedAnime,
                isLoading = viewModel.isLoading,
                alreadyInList = isAlreadyInList,
                getMediaEntry = getMediaEntry,
                onItemClick = onItemClick,
                onLongClickItem = onLongClickItem,
                onSeeAllClick = { onSeeAllSection("Popüler Animeler", ExploreCategoryType.NEWLY_ADDED_ANIME, filteredNewlyAddedAnime) },
                titleLanguage = titleLanguage,
                scoreFormat = scoreFormat,
                hideScores = hideScores,
                blurAdultMedia = blurAdultMedia
            )
            Spacer(modifier = Modifier.height(26.dp))
        }
    }

    // ─── TMDB EN YÜKSEK PUANLI ANİMELER ───
    if (filteredTrendingManga.isNotEmpty()) {
        item {
            KitsugiHorizontalMediaSection(
                title = "En Yüksek Puanlı Animeler",
                results = filteredTrendingManga,
                isLoading = viewModel.isLoading,
                alreadyInList = isAlreadyInList,
                getMediaEntry = getMediaEntry,
                onItemClick = onItemClick,
                onLongClickItem = onLongClickItem,
                onSeeAllClick = { onSeeAllSection("En Yüksek Puanlı Animeler", ExploreCategoryType.TRENDING_MANGA, filteredTrendingManga) },
                titleLanguage = titleLanguage,
                scoreFormat = scoreFormat,
                hideScores = hideScores,
                blurAdultMedia = blurAdultMedia
            )
            Spacer(modifier = Modifier.height(26.dp))
        }
    }

    // ─── TMDB YATAY MEDYA LİSTELERİ ───
    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_tmdb_trending_all),
            results = filteredTopAnime,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_tmdb_trending_all), ExploreCategoryType.TOP_ANIME, filteredTopAnime) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
        Spacer(modifier = Modifier.height(26.dp))
    }

    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_tmdb_trending_shows),
            results = filteredAiringAnime,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_tmdb_trending_shows), ExploreCategoryType.AIRING_ANIME, filteredAiringAnime) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
        Spacer(modifier = Modifier.height(26.dp))
    }

    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_tmdb_trending_movies),
            results = filteredMovieAnime,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_tmdb_trending_movies), ExploreCategoryType.MOVIE_ANIME, filteredMovieAnime) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
        Spacer(modifier = Modifier.height(26.dp))
    }

    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_tmdb_popular_shows),
            results = filteredTopManga,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_tmdb_popular_shows), ExploreCategoryType.TOP_MANGA, filteredTopManga) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
        Spacer(modifier = Modifier.height(26.dp))
    }

    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_tmdb_popular_movies),
            results = filteredUpcomingAnime,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_tmdb_popular_movies), ExploreCategoryType.UPCOMING_ANIME, filteredUpcomingAnime) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
        Spacer(modifier = Modifier.height(26.dp))
    }

    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_tmdb_top_rated_movies),
            results = filteredPublishingManga,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_tmdb_top_rated_movies), ExploreCategoryType.PUBLISHING_MANGA, filteredPublishingManga) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
        Spacer(modifier = Modifier.height(26.dp))
    }

    item {
        KitsugiHorizontalMediaSection(
            title = stringResource(R.string.explore_tmdb_top_rated_shows),
            results = filteredSeasonalAnime,
            isLoading = viewModel.isLoading,
            alreadyInList = isAlreadyInList,
            getMediaEntry = getMediaEntry,
            onItemClick = onItemClick,
            onLongClickItem = onLongClickItem,
            onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_tmdb_top_rated_shows), ExploreCategoryType.SEASONAL_ANIME, filteredSeasonalAnime) },
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia
        )
    }

    // ─── SİMKL KULLANICI LİSTELERİ ───
    if (viewModel.simklContinueSeries.isNotEmpty()) {
        item {
            Spacer(modifier = Modifier.height(26.dp))
            KitsugiHorizontalMediaSection(
                title = stringResource(R.string.explore_simkl_continue_watching_series),
                results = viewModel.simklContinueSeries,
                isLoading = viewModel.isLoading,
                alreadyInList = isAlreadyInList,
                getMediaEntry = getMediaEntry,
                onItemClick = onItemClick,
                onLongClickItem = onLongClickItem,
                onSeeAllClick = onNavigateToWatchHistory,
                titleLanguage = titleLanguage,
                scoreFormat = scoreFormat,
                hideScores = hideScores,
                blurAdultMedia = blurAdultMedia
            )
        }
    }

    if (viewModel.simklContinueMovies.isNotEmpty()) {
        item {
            Spacer(modifier = Modifier.height(26.dp))
            KitsugiHorizontalMediaSection(
                title = stringResource(R.string.explore_simkl_continue_watching_movies),
                results = viewModel.simklContinueMovies,
                isLoading = viewModel.isLoading,
                alreadyInList = isAlreadyInList,
                getMediaEntry = getMediaEntry,
                onItemClick = onItemClick,
                onLongClickItem = onLongClickItem,
                onSeeAllClick = onNavigateToWatchHistory,
                titleLanguage = titleLanguage,
                scoreFormat = scoreFormat,
                hideScores = hideScores,
                blurAdultMedia = blurAdultMedia
            )
        }
    }

    if (viewModel.simklPlannedSeries.isNotEmpty()) {
        item {
            Spacer(modifier = Modifier.height(26.dp))
            KitsugiHorizontalMediaSection(
                title = stringResource(R.string.explore_simkl_plantowatch_series),
                results = viewModel.simklPlannedSeries,
                isLoading = viewModel.isLoading,
                alreadyInList = isAlreadyInList,
                getMediaEntry = getMediaEntry,
                onItemClick = onItemClick,
                onLongClickItem = onLongClickItem,
                onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_simkl_plantowatch_series), ExploreCategoryType.AIRING_ANIME, viewModel.simklPlannedSeries) },
                titleLanguage = titleLanguage,
                scoreFormat = scoreFormat,
                hideScores = hideScores,
                blurAdultMedia = blurAdultMedia
            )
        }
    }

    if (viewModel.simklPlannedMovies.isNotEmpty()) {
        item {
            Spacer(modifier = Modifier.height(26.dp))
            KitsugiHorizontalMediaSection(
                title = stringResource(R.string.explore_simkl_plantowatch_movies),
                results = viewModel.simklPlannedMovies,
                isLoading = viewModel.isLoading,
                alreadyInList = isAlreadyInList,
                getMediaEntry = getMediaEntry,
                onItemClick = onItemClick,
                onLongClickItem = onLongClickItem,
                onSeeAllClick = { onSeeAllSection(context.getString(R.string.explore_simkl_plantowatch_movies), ExploreCategoryType.MOVIE_ANIME, viewModel.simklPlannedMovies) },
                titleLanguage = titleLanguage,
                scoreFormat = scoreFormat,
                hideScores = hideScores,
                blurAdultMedia = blurAdultMedia
            )
        }
    }
}
