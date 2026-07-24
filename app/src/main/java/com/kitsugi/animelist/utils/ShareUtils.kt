package com.kitsugi.animelist.utils

import android.content.Context
import android.content.Intent
import com.kitsugi.animelist.model.MediaType

object ShareUtils {

    fun shareText(context: Context, title: String, url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
        }
        val chooser = Intent.createChooser(intent, "Paylaş: $title")
        context.startActivity(chooser)
    }

    fun buildMediaUrl(source: String, mediaId: Int, mediaType: MediaType?): String {
        val s = source.lowercase()
        return when {
            s == "anilist" -> {
                val realId = if (mediaId >= 100_000_000) mediaId - 100_000_000 else mediaId
                when (mediaType) {
                    MediaType.Manga -> "https://anilist.co/manga/$realId"
                    else -> "https://anilist.co/anime/$realId"
                }
            }
            s == "tmdb" -> {
                when (mediaType) {
                    MediaType.Movie -> "https://themoviedb.org/movie/$mediaId"
                    else -> "https://themoviedb.org/tv/$mediaId"
                }
            }
            s == "simkl" -> {
                when (mediaType) {
                    MediaType.Manga -> "https://simkl.com/manga/$mediaId"
                    MediaType.Movie -> "https://simkl.com/movies/$mediaId"
                    MediaType.TvShow -> "https://simkl.com/shows/$mediaId"
                    else -> "https://simkl.com/anime/$mediaId"
                }
            }
            else -> { // mal / jikan
                when (mediaType) {
                    MediaType.Manga -> "https://myanimelist.net/manga/$mediaId"
                    else -> "https://myanimelist.net/anime/$mediaId"
                }
            }
        }
    }

    fun buildExternalMediaUrl(source: String, id: Int, tmdbId: Int? = null, type: MediaType): String? {
        val s = source.lowercase().trim()
        return when (s) {
            "jikan", "mal" -> {
                when (type) {
                    MediaType.Anime -> "https://myanimelist.net/anime/$id"
                    MediaType.Manga -> "https://myanimelist.net/manga/$id"
                    else -> null
                }
            }
            "anilist" -> {
                val aniListId = if (id >= 100_000_000) id - 100_000_000 else id
                if (aniListId > 0) {
                    when (type) {
                        MediaType.Anime -> "https://anilist.co/anime/$aniListId"
                        MediaType.Manga -> "https://anilist.co/manga/$aniListId"
                        else -> null
                    }
                } else {
                    // Fallback to MAL link
                    when (type) {
                        MediaType.Anime -> "https://myanimelist.net/anime/$id"
                        MediaType.Manga -> "https://myanimelist.net/manga/$id"
                        else -> null
                    }
                }
            }
            "tmdb" -> {
                val realTmdbId = tmdbId ?: id
                when (type) {
                    MediaType.Movie -> "https://www.themoviedb.org/movie/$realTmdbId"
                    MediaType.TvShow -> "https://www.themoviedb.org/tv/$realTmdbId"
                    else -> null
                }
            }
            "simkl" -> {
                when (type) {
                    MediaType.Manga -> "https://simkl.com/manga/$id"
                    MediaType.Movie -> "https://simkl.com/movies/$id"
                    MediaType.TvShow -> "https://simkl.com/shows/$id"
                    else -> "https://simkl.com/anime/$id"
                }
            }
            else -> null
        }
    }

    fun buildProfileUrl(source: String, username: String): String {
        return when (source.lowercase()) {
            "anilist" -> "https://anilist.co/user/$username"
            "simkl" -> "https://simkl.com/user/$username"
            else -> "https://myanimelist.net/profile/$username"
        }
    }

    fun buildCharacterUrl(source: String, characterId: Int): String {
        return when (source.lowercase()) {
            "anilist" -> "https://anilist.co/character/$characterId"
            else -> "https://myanimelist.net/character/$characterId"
        }
    }

    fun buildStaffUrl(source: String, staffId: Int): String {
        return when (source.lowercase()) {
            "anilist" -> "https://anilist.co/staff/$staffId"
            else -> "https://myanimelist.net/people/$staffId"
        }
    }

    fun buildStudioUrl(source: String, studioId: Int): String {
        return when (source.lowercase()) {
            "anilist" -> "https://anilist.co/studio/$studioId"
            else -> "https://anilist.co/studio/$studioId"
        }
    }
}
