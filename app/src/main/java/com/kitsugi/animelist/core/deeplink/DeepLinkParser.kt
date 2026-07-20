package com.kitsugi.animelist.core.deeplink

import android.net.Uri

/**
 * B1.1 - Versioned Typed Deep-Link Model
 *
 * Desteklenen schemalar (v1):
 *   Kitsugianimelist://v1/detail/{source}/{mediaId}
 *   Kitsugianimelist://v1/play/{source}/{mediaId}?season=1&episode=7
 *   Kitsugianimelist://v1/manga/{sourceKey}/{mangaId}?chapter={chapterId}
 *   Kitsugianimelist://tv-login  (mevcut auth flow; ayrica parse edilir)
 *
 * Gecersiz veya eksik link = TvDeepLink.None
 * Auth link = TvDeepLink.Auth
 */
sealed interface TvDeepLink {

    /** Gecersiz, bos veya desteklenmeyen deep link. UI Home'a fallback verir. */
    object None : TvDeepLink

    /** Mevcut AniList/MAL/Simkl auth callback; asiri handler yonlendirir. */
    object Auth : TvDeepLink

    /**
     * Detail ekrani: Kitsugianimelist://v1/detail/{source}/{mediaId}
     * @param source  "anilist" | "mal" | "jikan" vb.
     * @param mediaId Kaynak icindeki sayisal veya string ID.
     */
    data class Detail(
        val source: String,
        val mediaId: String
    ) : TvDeepLink

    /**
     * Direkt oynatma: Kitsugianimelist://v1/play/{source}/{mediaId}
     * @param season  Query param "season" (1-indexed, null = ilk sezon)
     * @param episode Query param "episode" (1-indexed, null = devam et)
     */
    data class Play(
        val source: String,
        val mediaId: String,
        val season: Int?,
        val episode: Int?
    ) : TvDeepLink

    /**
     * Manga okuyucu: Kitsugianimelist://v1/manga/{sourceKey}/{mangaId}
     * @param chapterId Query param "chapter" (null = en son veya devam)
     */
    data class Manga(
        val sourceKey: String,
        val mangaId: String,
        val chapterId: String?
    ) : TvDeepLink
}

/** Uygulamanin genel deep-link semasi. */
private const val SCHEME = "Kitsugianimelist"

/** Versioned path onek. Gelecekte v2 eklenirse uyumlu okuma mumkun. */
private const val VERSION_V1 = "v1"

/**
 * B1.1 - Deep-link URI'sini TvDeepLink modeline donusturur.
 *
 * - Null veya sema uyumsuz URI   = TvDeepLink.None
 * - malapp://auth or tv-login    = TvDeepLink.Auth
 * - Tanimli v1 path'ler         = ilgili sealed alt turu
 * - Bilinmeyen path/eksik/hata  = TvDeepLink.None
 */
object DeepLinkParser {

    fun parse(uri: Uri?): TvDeepLink {
        if (uri == null) return TvDeepLink.None

        // Auth callbackler
        if (uri.scheme == "malapp" && uri.host == "auth") return TvDeepLink.Auth
        if (uri.scheme == SCHEME && uri.host == "tv-login") return TvDeepLink.Auth

        if (uri.scheme != SCHEME) return TvDeepLink.None

        // Segment listesini temizle (bos parcalari at)
        val segments = uri.pathSegments.filter { it.isNotBlank() }

        // Minimum: [version, action, ...payload]
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

    // -- Private parsers ------------------------------------------------------

    private fun parseDetail(segments: List<String>, uri: Uri): TvDeepLink {
        // [v1, detail, source, mediaId]
        if (segments.size < 4) return TvDeepLink.None
        val source  = segments[2].decodeSafe() ?: return TvDeepLink.None
        val mediaId = segments[3].decodeSafe() ?: return TvDeepLink.None
        if (source.isBlank() || mediaId.isBlank()) return TvDeepLink.None
        return TvDeepLink.Detail(source = source, mediaId = mediaId)
    }

    private fun parsePlay(segments: List<String>, uri: Uri): TvDeepLink {
        // [v1, play, source, mediaId]
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
        // [v1, manga, sourceKey, mangaId]
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

    // -- Helpers ---------------------------------------------------------------

    // URL decode wrapper - null doner; bu durumda parse caller None'a gecer.
    private fun String.decodeSafe(): String? = try {
        java.net.URLDecoder.decode(this, "UTF-8").takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}
