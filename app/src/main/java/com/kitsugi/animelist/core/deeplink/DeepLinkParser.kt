package com.kitsugi.animelist.core.deeplink

import android.net.Uri
import com.kitsugi.animelist.model.MediaType

/**
 * B1.1 - Versioned Typed Deep-Link Model
 *
 * Desteklenen schemalar (v1 & Web):
 *   Kitsugianimelist://v1/detail/{source}/{mediaId}
 *   Kitsugianimelist://v1/play/{source}/{mediaId}?season=1&episode=7
 *   Kitsugianimelist://v1/manga/{sourceKey}/{mangaId}?chapter={chapterId}
 *   Kitsugianimelist://tv-login
 *   https://anilist.co/... (anime, manga, character, staff, studio, user)
 *   https://myanimelist.net/... (anime, manga, character, people, profile)
 *   https://themoviedb.org / tmdb.org/... (tv, movie, person)
 *   https://simkl.com/... (anime, shows, movies, manga)
 */
sealed interface TvDeepLink {

    object None : TvDeepLink

    object Auth : TvDeepLink

    data class Detail(
        val source: String,
        val mediaId: String,
        val mediaType: MediaType? = null
    ) : TvDeepLink

    data class Character(
        val characterId: Int,
        val source: String
    ) : TvDeepLink

    data class Staff(
        val staffId: Int,
        val source: String
    ) : TvDeepLink

    data class Studio(
        val studioId: Int,
        val source: String
    ) : TvDeepLink

    data class UserProfile(
        val username: String,
        val source: String
    ) : TvDeepLink

    data class Play(
        val source: String,
        val mediaId: String,
        val season: Int?,
        val episode: Int?
    ) : TvDeepLink

    data class Manga(
        val sourceKey: String,
        val mangaId: String,
        val chapterId: String?
    ) : TvDeepLink
}

private const val SCHEME = "Kitsugianimelist"
private const val VERSION_V1 = "v1"

object DeepLinkParser {

    fun parse(uri: Uri?): TvDeepLink {
        if (uri == null) return TvDeepLink.None

        val scheme = uri.scheme?.lowercase() ?: return TvDeepLink.None
        val host = uri.host?.lowercase() ?: ""

        // Auth callback'ler
        if (scheme == "malapp" && host == "auth") return TvDeepLink.Auth
        if (scheme == SCHEME && host == "tv-login") return TvDeepLink.Auth

        // Custom app scheme: Kitsugianimelist://v1/...
        if (scheme == SCHEME) {
            val segments = uri.pathSegments.filter { it.isNotBlank() }
            if (segments.size < 2) return TvDeepLink.None
            val version = segments[0]
            if (version != VERSION_V1) return TvDeepLink.None

            val action = segments[1]
            return when (action) {
                "detail" -> parseDetail(segments, uri)
                "play"   -> parsePlay(segments, uri)
                "manga"  -> parseManga(segments, uri)
                else     -> TvDeepLink.None
            }
        }

        // Web App Links (http / https)
        if (scheme == "http" || scheme == "https") {
            val segments = uri.pathSegments.filter { it.isNotBlank() }
            return parseWebUrl(host, segments)
        }

        return TvDeepLink.None
    }

    // -- Web URL Parsers -------------------------------------------------------

    private fun parseWebUrl(host: String, segments: List<String>): TvDeepLink {
        if (segments.isEmpty()) return TvDeepLink.None
        val cleanHost = host.removePrefix("www.")

        return when (cleanHost) {
            "anilist.co" -> parseAniListUrl(segments)
            "myanimelist.net" -> parseMalUrl(segments)
            "themoviedb.org", "tmdb.org" -> parseTmdbUrl(segments)
            "simkl.com" -> parseSimklUrl(segments)
            else -> TvDeepLink.None
        }
    }

    private fun parseAniListUrl(segments: List<String>): TvDeepLink {
        if (segments.size < 2) return TvDeepLink.None
        val category = segments[0].lowercase()
        val idStr = segments[1]
        val idInt = idStr.toIntOrNull()

        return when (category) {
            "anime" -> TvDeepLink.Detail(source = "anilist", mediaId = idStr, mediaType = MediaType.Anime)
            "manga" -> TvDeepLink.Detail(source = "anilist", mediaId = idStr, mediaType = MediaType.Manga)
            "character" -> if (idInt != null) TvDeepLink.Character(characterId = idInt, source = "anilist") else TvDeepLink.None
            "staff" -> if (idInt != null) TvDeepLink.Staff(staffId = idInt, source = "anilist") else TvDeepLink.None
            "studio" -> if (idInt != null) TvDeepLink.Studio(studioId = idInt, source = "anilist") else TvDeepLink.None
            "user" -> TvDeepLink.UserProfile(username = idStr, source = "anilist")
            else -> TvDeepLink.None
        }
    }

    private fun parseMalUrl(segments: List<String>): TvDeepLink {
        if (segments.size < 2) return TvDeepLink.None
        val category = segments[0].lowercase()
        val idStr = segments[1]
        val idInt = idStr.toIntOrNull()

        return when (category) {
            "anime" -> TvDeepLink.Detail(source = "mal", mediaId = idStr, mediaType = MediaType.Anime)
            "manga" -> TvDeepLink.Detail(source = "mal", mediaId = idStr, mediaType = MediaType.Manga)
            "character" -> if (idInt != null) TvDeepLink.Character(characterId = idInt, source = "mal") else TvDeepLink.None
            "people" -> if (idInt != null) TvDeepLink.Staff(staffId = idInt, source = "mal") else TvDeepLink.None
            "profile" -> TvDeepLink.UserProfile(username = idStr, source = "mal")
            else -> TvDeepLink.None
        }
    }

    private fun parseTmdbUrl(segments: List<String>): TvDeepLink {
        if (segments.size < 2) return TvDeepLink.None
        val category = segments[0].lowercase()
        val rawId = segments[1].substringBefore("-")
        val idInt = rawId.toIntOrNull() ?: return TvDeepLink.None

        return when (category) {
            "tv" -> TvDeepLink.Detail(source = "tmdb", mediaId = rawId, mediaType = MediaType.TvShow)
            "movie" -> TvDeepLink.Detail(source = "tmdb", mediaId = rawId, mediaType = MediaType.Movie)
            "person" -> TvDeepLink.Staff(staffId = idInt, source = "tmdb")
            else -> TvDeepLink.None
        }
    }

    private fun parseSimklUrl(segments: List<String>): TvDeepLink {
        if (segments.size < 2) return TvDeepLink.None
        val category = segments[0].lowercase()
        val idStr = segments[1]

        return when (category) {
            "anime" -> TvDeepLink.Detail(source = "simkl", mediaId = idStr, mediaType = MediaType.Anime)
            "shows" -> TvDeepLink.Detail(source = "simkl", mediaId = idStr, mediaType = MediaType.TvShow)
            "movies" -> TvDeepLink.Detail(source = "simkl", mediaId = idStr, mediaType = MediaType.Movie)
            "manga" -> TvDeepLink.Detail(source = "simkl", mediaId = idStr, mediaType = MediaType.Manga)
            else -> TvDeepLink.None
        }
    }

    // -- Private parsers ------------------------------------------------------

    private fun parseDetail(segments: List<String>, uri: Uri): TvDeepLink {
        if (segments.size < 4) return TvDeepLink.None
        val source  = segments[2].decodeSafe() ?: return TvDeepLink.None
        val mediaId = segments[3].decodeSafe() ?: return TvDeepLink.None
        if (source.isBlank() || mediaId.isBlank()) return TvDeepLink.None
        return TvDeepLink.Detail(source = source, mediaId = mediaId)
    }

    private fun parsePlay(segments: List<String>, uri: Uri): TvDeepLink {
        if (segments.size < 4) return TvDeepLink.None
        val source  = segments[2].decodeSafe() ?: return TvDeepLink.None
        val mediaId = segments[3].decodeSafe() ?: return TvDeepLink.None
        if (source.isBlank() || mediaId.isBlank()) return TvDeepLink.None

        val season  = uri.getQueryParameter("season")?.toIntOrNull()
        val episode = uri.getQueryParameter("episode")?.toIntOrNull()

        return TvDeepLink.Play(
            source  = source,
            mediaId = mediaId,
            season  = season,
            episode = episode
        )
    }

    private fun parseManga(segments: List<String>, uri: Uri): TvDeepLink {
        if (segments.size < 4) return TvDeepLink.None
        val sourceKey = segments[2].decodeSafe() ?: return TvDeepLink.None
        val mangaId   = segments[3].decodeSafe() ?: return TvDeepLink.None
        if (sourceKey.isBlank() || mangaId.isBlank()) return TvDeepLink.None

        val chapterId = uri.getQueryParameter("chapter")?.trim()?.takeIf { it.isNotBlank() }

        return TvDeepLink.Manga(
            sourceKey = sourceKey,
            mangaId   = mangaId,
            chapterId = chapterId
        )
    }

    private fun String.decodeSafe(): String? = try {
        java.net.URLDecoder.decode(this, "UTF-8").takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}
