package com.kitsugi.animelist.data.remote

import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.MediaEntry

data class JikanSearchResult(
    val malId: Int,
    val title: String,
    val subtitle: String,
    val type: MediaType,
    val total: Int?,
    val score: Int?,
    val isAdult: Boolean,
    val imageUrl: String?,
    val year: Int?,
    val source: String,
    // AniList kaynağından gelen sonuçlarda gerçek MAL ID'si (idMal != null ise)
    val realMalId: Int? = null,
    val titleEnglish: String? = null,
    val titleJapanese: String? = null,
    val tmdbId: Int? = null,
    val backdropUrl: String? = null,
    val rank: Int? = null,
    val members: Int? = null,
    val favorites: Int? = null,
    val rawScoreDouble: Double? = null,
    // AniList kaynaklı sonuçlar için: "episode|airingAtEpoch" formatında
    val nextAiringEpisode: String? = null
)

data class KitsugiTheme(
    val label: String,       // Görüntülenecek metin: "We Are!" by Hiroshi Kitadani (OP1)
    val videoUrl: String?    // animethemes.moe direkt .webm linki (yoksa null → YouTube araması)
)

data class KitsugiStudio(
    val id: Int,
    val name: String,
    val isMain: Boolean = true
)

data class KitsugiStudioDetail(
    val id: Int,
    val name: String,
    val isMain: Boolean = true,
    val imageUrl: String? = null,
    val favorites: Int? = null,
    val established: String? = null,
    val about: String? = null,
    val mediaWorks: List<KitsugiStaffMediaWork> = emptyList(),
    val isFavourite: Boolean = false
)

data class KitsugiMediaDetail(
    val synopsis: String?,
    val genres: List<String> = emptyList(),
    val status: String? = null,
    val season: String? = null,
    val sourceMaterial: String? = null,
    val studios: List<KitsugiStudio> = emptyList(),
    val producers: List<KitsugiStudio> = emptyList(),
    val rating: String? = null,
    val broadcast: String? = null,
    val episodeDuration: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val titleEnglish: String? = null,
    val titleJapanese: String? = null,
    val titleRomaji: String? = null,
    val titleNative: String? = null,
    val synonyms: List<String> = emptyList(),
    val openings: List<KitsugiTheme> = emptyList(),
    val endings: List<KitsugiTheme> = emptyList(),
    val trailerUrl: String? = null,
    val title: String? = null,
    val imageUrl: String? = null,
    val score: Int? = null,
    val year: Int? = null,
    val total: Int? = null,
    val isAdult: Boolean = false,
    val realMalId: Int? = null,
    val tags: List<KitsugiTag> = emptyList(),
    val externalLinks: List<KitsugiExternalLink> = emptyList(),
    val streamingLinks: List<KitsugiExternalLink> = emptyList(),
    val streamingEpisodes: List<KitsugiStreamingEpisode> = emptyList(),
    val tmdbId: Int? = null,   // SeriesGraph API için — AniList externalLinks'ten veya TVDB'den çıkarılır
    val tmdbSeason: Int? = null,
    val pictures: List<String> = emptyList(),  // Jikan /pictures endpoint'inden gelen ek resimler
    val totalSeasons: Int? = null,
    val nextAiringEpisode: String? = null,
    val meanScore: Int? = null,
    val averageScore: Int? = null,
    val popularity: Int? = null,
    val favorites: Int? = null,
    val rank: Int? = null,
    val popularityRank: Int? = null,
    val scoredBy: Int? = null,
    val members: Int? = null
)

data class KitsugiStreamingEpisode(
    val title: String,
    val thumbnail: String?,
    val url: String?,
    val site: String?,
    val seasonNumber: Int? = null,   // Bölüm puanı eşleştirmesi için
    val episodeNumber: Int? = null   // Bölüm puanı eşleştirmesi için
)

data class KitsugiTag(
    val name: String,
    val rank: Int?,       // % relevance, AniList'ten gelir
    val isSpoiler: Boolean
)

data class KitsugiExternalLink(
    val site: String,
    val url: String,
    val language: String? = null  // örn. "JP", "EN"
)

data class KitsugiVoiceActor(
    val id: Int,
    val name: String,
    val language: String,
    val imageUrl: String?,
    val source: String = "jikan"
)

data class KitsugiCharacter(
    val id: Int,
    val name: String,
    val role: String,
    val imageUrl: String?,
    val voiceActors: List<KitsugiVoiceActor> = emptyList(),
    val source: String = "jikan"
)

data class KitsugiCharacterMediaAppearance(
    val mediaId: Int,
    val title: String,
    val imageUrl: String?,
    val mediaType: String,
    val characterRole: String,
    val source: String = "jikan",
    val titleEnglish: String? = null,
    val titleJapanese: String? = null,
    val titleRomaji: String? = null
)

data class KitsugiCharacterDetail(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val alternativeNames: List<String>,
    val imageUrl: String?,
    val gender: String?,
    val age: String?,
    val birthday: String?,
    val bloodType: String?,
    val biography: String?,
    val voiceActors: List<KitsugiVoiceActor> = emptyList(),
    val mediaAppearances: List<KitsugiCharacterMediaAppearance> = emptyList(),
    val isFavourite: Boolean = false
)

data class KitsugiStaff(
    val id: Int,
    val name: String,
    val role: String,
    val imageUrl: String?,
    val source: String = "jikan"
)

data class KitsugiStaffCharacterRole(
    val characterId: Int,
    val characterName: String,
    val characterImageUrl: String?,
    val characterSource: String = "jikan",
    val mediaId: Int,
    val mediaTitle: String,
    val mediaImageUrl: String?,
    val mediaType: String,
    val characterRole: String,
    val mediaSource: String = "jikan"
)

data class KitsugiStaffMediaWork(
    val mediaId: Int,
    val mediaTitle: String,
    val mediaImageUrl: String?,
    val mediaType: String,
    val staffRole: String,
    val source: String = "jikan",
    val titleEnglish: String? = null,
    val titleJapanese: String? = null,
    val titleRomaji: String? = null
)

data class KitsugiStaffDetail(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val alternativeNames: List<String>,
    val imageUrl: String?,
    val biography: String?,
    val occupation: String?,
    val birthday: String?,
    val age: String?,
    val gender: String?,
    val homeTown: String?,
    val characterRoles: List<KitsugiStaffCharacterRole> = emptyList(),
    val mediaWorks: List<KitsugiStaffMediaWork> = emptyList(),
    val isFavourite: Boolean = false
)

data class KitsugiRelation(
    val malId: Int,
    val title: String,
    val relationType: String,
    val imageUrl: String?,
    val mediaType: MediaType,
    val source: String,
    val titleEnglish: String? = null,
    val titleJapanese: String? = null,
    val titleRomaji: String? = null,
    val isAdult: Boolean = false
)

data class KitsugiScoreStat(
    val score: Int,
    val amount: Int
)

data class KitsugiRanking(
    val rank: Int,
    val type: String,
    val context: String,
    val allTime: Boolean = false,
    val year: Int? = null,
    val season: String? = null
)

data class KitsugiStats(
    val watching: Int?,
    val completed: Int?,
    val planned: Int?,
    val dropped: Int?,
    val paused: Int? = null,
    val scoreDistribution: List<KitsugiScoreStat> = emptyList(),
    val rankings: List<KitsugiRanking> = emptyList()
)

data class KitsugiReview(
    val id: Int? = null,
    val username: String,
    val avatarUrl: String?,
    val score: Int?,
    val summary: String,
    val fullText: String = "",
    val dateText: String? = null,
    val helpfulCount: Int? = null,
    val ratingAmount: Int? = null,
    val userRating: String? = null
)

data class KitsugiForumTopic(
    val id: Int,
    val title: String,
    val commentCount: Int,
    val viewCount: Int,
    val username: String,
    val avatarUrl: String?,
    val dateText: String? = null,
    val likeCount: Int = 0,
    val isLiked: Boolean = false
)

data class KitsugiForumReply(
    val id: Int,
    val comment: String,
    val dateText: String?,
    val username: String,
    val avatarUrl: String?,
    val likeCount: Int,
    val isLiked: Boolean
)

data class KitsugiActivity(
    val id: Int,
    val text: String,
    val dateText: String?,
    val username: String,
    val avatarUrl: String?,
    val mediaTitle: String? = null,
    val mediaTitleRomaji: String? = null,
    val mediaTitleEnglish: String? = null,
    val mediaTitleNative: String? = null,
    val mediaCoverUrl: String? = null,
    val likeCount: Int,
    val isLiked: Boolean,
    val replies: List<KitsugiActivityReply> = emptyList(),
    val mediaId: Int? = null,
    val mediaType: String? = null,
    val isAdult: Boolean = false
)

data class KitsugiActivityReply(
    val id: Int,
    val text: String,
    val dateText: String?,
    val username: String,
    val avatarUrl: String?,
    val likeCount: Int,
    val isLiked: Boolean
)

fun MediaEntry.matches(result: JikanSearchResult): Boolean {
    // 1. Doğrudan kaynak + ID eşleşmesi
    if (this.source.equals(result.source, ignoreCase = true) && this.malId == result.malId) {
        return true
    }

    // 2. TMDB ID eşleşmesi (Çapraz eşleşme)
    val entryTmdb = this.tmdbId
    val resultTmdb = result.tmdbId ?: if (result.source.equals("tmdb", ignoreCase = true)) result.malId else null
    if (entryTmdb != null && resultTmdb != null && entryTmdb == resultTmdb) {
        return true
    }

    // 3. MAL ID / realMalId eşleşmesi
    val resultIsMal = result.source.equals("jikan", ignoreCase = true) || result.source.equals("mal", ignoreCase = true)
    val entryIsMal = this.source.equals("jikan", ignoreCase = true) || this.source.equals("mal", ignoreCase = true)
    val rMal = if (resultIsMal) result.malId else result.realMalId
    val eMal = if (entryIsMal) this.malId else null
    if (rMal != null && eMal != null && rMal == eMal) {
        return true
    }

    // 4. Göreceli başlık + yıl + tip eşleşmesi (Fuzzy fallback)
    if (this.type == result.type) {
        val entryTitleNorm = this.title.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }.trim()
        val resultTitleNorm = result.title.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }.trim()
        if (entryTitleNorm.isNotEmpty() && entryTitleNorm == resultTitleNorm) {
            val y1 = this.year
            val y2 = result.year
            if (y1 == null || y2 == null || java.lang.Math.abs(y1 - y2) <= 1) {
                return true
            }
        }
    }

    return false
}

fun MediaEntry.matches(mediaId: Int, mediaSource: String): Boolean {
    if (this.source.equals(mediaSource, ignoreCase = true) && this.malId == mediaId) {
        return true
    }

    // TMDB ID eşleşmesi
    if (mediaSource.equals("tmdb", ignoreCase = true) && this.tmdbId == mediaId) {
        return true
    }

    val resultIsMal = mediaSource.equals("jikan", ignoreCase = true) || mediaSource.equals("mal", ignoreCase = true)
    val entryIsMal = this.source.equals("jikan", ignoreCase = true) || this.source.equals("mal", ignoreCase = true)
    if (resultIsMal && entryIsMal && this.malId == mediaId) {
        return true
    }

    return false
}

fun List<MediaEntry>.firstMatching(result: JikanSearchResult): MediaEntry? {
    val src = result.source.lowercase()
    return this.firstOrNull { entry ->
        val entrySrc = entry.source.lowercase()
        (entrySrc == src || (entrySrc == "mal" && src == "jikan") || (entrySrc == "jikan" && src == "mal")) && entry.matches(result)
    } ?: this.firstOrNull { entry ->
        entry.matches(result)
    }
}

fun List<MediaEntry>.firstMatching(mediaId: Int, mediaSource: String): MediaEntry? {
    val src = mediaSource.lowercase()
    return this.firstOrNull { entry ->
        val entrySrc = entry.source.lowercase()
        (entrySrc == src || (entrySrc == "mal" && src == "jikan") || (entrySrc == "jikan" && src == "mal")) && entry.matches(mediaId, mediaSource)
    } ?: this.firstOrNull { entry ->
        entry.matches(mediaId, mediaSource)
    }
}


