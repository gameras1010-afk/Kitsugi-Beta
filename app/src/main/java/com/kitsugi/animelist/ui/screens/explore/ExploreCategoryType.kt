package com.kitsugi.animelist.ui.screens.explore

enum class ExploreCategoryType {
    TOP_ANIME,
    TRENDING_ANIME,
    AIRING_ANIME,
    UPCOMING_ANIME,
    MOVIE_ANIME,
    SEASONAL_ANIME,
    TOP_MANGA,
    PUBLISHING_MANGA,
    TRENDING_MANGA,
    NEWLY_ADDED_ANIME,
    NEWLY_ADDED_MANGA,
    /** TMDB'ye özgü "Yakında Yayında" animeleri — getUpcomingAnime() ile sayfalanır */
    UPCOMING_ANIME_TMDB
}
