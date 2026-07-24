package com.kitsugi.animelist.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.utils.*
import java.util.concurrent.ConcurrentHashMap

/**
 * TMDB keşfet (trending/popular/top-rated) ve backdrop arama işlevleri.
 * 60 dakikalık in-memory TTL cache ile rate limit baskısını azaltır.
 *
 * [TmdbApiClient] tarafından delegate olarak kullanılır; doğrudan çağrılmamalıdır.
 */
internal object TmdbDiscoverClient {

    private const val TAG = "TmdbDiscoverClient"

    // ── In-memory TTL cache ─────────────────────────────────────────────────────
    private const val TTL_MS = 60 * 60 * 1_000L
    private data class CacheEntry(val data: List<JikanSearchResult>, val fetchedAt: Long)
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    fun get(key: String): List<JikanSearchResult>? {
        val entry = cache[key] ?: return null
        return if (System.currentTimeMillis() - entry.fetchedAt < TTL_MS) entry.data else null
    }

    fun put(key: String, data: List<JikanSearchResult>) {
        cache[key] = CacheEntry(data, System.currentTimeMillis())
    }

    // ── Trending (fallback: /discover?sort_by=popularity.desc) ─────────────────

    suspend fun getTrendingMovies(
        page: Int = 1,
        apiKey: String,
        language: String,
        executeGet: suspend (String) -> String?
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "trending_movies_$page"
        get(cacheKey)?.let { return@withContext it }

        // Önce trending endpoint'i dene
        val trendUrl = "https://api.themoviedb.org/3/trending/movie/week?api_key=$apiKey&language=$language&page=$page"
        var result = parseTmdbDiscoverList(trendUrl, MediaType.Movie, executeGet)

        // Trending boş döndüyse discover fallback kullan
        if (result.isEmpty()) {
            Log.w(TAG, "getTrendingMovies: trending empty, falling back to discover/popularity")
            val fallbackUrl = "https://api.themoviedb.org/3/discover/movie?api_key=$apiKey&language=$language&sort_by=popularity.desc&page=$page"
            result = parseTmdbDiscoverList(fallbackUrl, MediaType.Movie, executeGet)
        }

        if (result.isNotEmpty()) put(cacheKey, result)
        result
    }

    suspend fun getTrendingShows(
        page: Int = 1,
        apiKey: String,
        language: String,
        executeGet: suspend (String) -> String?
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "trending_shows_$page"
        get(cacheKey)?.let { return@withContext it }

        val trendUrl = "https://api.themoviedb.org/3/trending/tv/week?api_key=$apiKey&language=$language&page=$page"
        var result = parseTmdbDiscoverList(trendUrl, MediaType.TvShow, executeGet)

        if (result.isEmpty()) {
            Log.w(TAG, "getTrendingShows: trending empty, falling back to discover/popularity")
            val fallbackUrl = "https://api.themoviedb.org/3/discover/tv?api_key=$apiKey&language=$language&sort_by=popularity.desc&page=$page"
            result = parseTmdbDiscoverList(fallbackUrl, MediaType.TvShow, executeGet)
        }

        if (result.isNotEmpty()) put(cacheKey, result)
        result
    }

    /**
     * TMDB'den bu hafta trend olan tüm medyayı (film + dizi) çeker.
     * Fallback: film ve dizi popular listelerini birleştirip karıştırır.
     */
    suspend fun getTrendingAll(
        page: Int = 1,
        apiKey: String,
        language: String,
        executeGet: suspend (String) -> String?
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "trending_all_$page"
        get(cacheKey)?.let { return@withContext it }

        val trendUrl = "https://api.themoviedb.org/3/trending/all/week?api_key=$apiKey&language=$language&page=$page"
        var result = parseTmdbDiscoverListAll(trendUrl, executeGet)

        if (result.isEmpty()) {
            Log.w(TAG, "getTrendingAll: trending empty, merging movie+tv popular fallback")
            val moviesUrl = "https://api.themoviedb.org/3/discover/movie?api_key=$apiKey&language=$language&sort_by=popularity.desc&page=$page"
            val showsUrl  = "https://api.themoviedb.org/3/discover/tv?api_key=$apiKey&language=$language&sort_by=popularity.desc&page=$page"
            val movies = parseTmdbDiscoverList(moviesUrl, MediaType.Movie, executeGet)
            val shows  = parseTmdbDiscoverList(showsUrl, MediaType.TvShow, executeGet)
            // Film ve dizileri interleave et: 1 film, 1 dizi sırayla
            val merged = mutableListOf<JikanSearchResult>()
            val maxSize = maxOf(movies.size, shows.size)
            for (i in 0 until maxSize) {
                if (i < movies.size) merged.add(movies[i])
                if (i < shows.size) merged.add(shows[i])
            }
            result = merged.take(20)
        }

        if (result.isNotEmpty()) put(cacheKey, result)
        result
    }

    // ── Popular ─────────────────────────────────────────────────────────────────

    /**
     * En popüler aksiyon/macera filmlerini çeker; "Ev Sineması" şeridi için kullanılır.
     */
    suspend fun getPopularMovies(
        page: Int = 1,
        apiKey: String,
        language: String,
        executeGet: suspend (String) -> String?
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "popular_movies_$page"
        get(cacheKey)?.let { return@withContext it }
        val url = "https://api.themoviedb.org/3/movie/popular?api_key=$apiKey&language=$language&page=$page"
        val result = parseTmdbDiscoverList(url, MediaType.Movie, executeGet)
        if (result.isNotEmpty()) put(cacheKey, result)
        result
    }

    /** En popüler dizileri çeker — Keşfet TMDB sekmesi için */
    suspend fun getPopularShows(
        page: Int = 1,
        apiKey: String,
        language: String,
        executeGet: suspend (String) -> String?
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "popular_shows_$page"
        get(cacheKey)?.let { return@withContext it }
        val url = "https://api.themoviedb.org/3/tv/popular?api_key=$apiKey&language=$language&page=$page"
        val result = parseTmdbDiscoverList(url, MediaType.TvShow, executeGet)
        if (result.isNotEmpty()) put(cacheKey, result)
        result
    }

    // ── Top Rated ───────────────────────────────────────────────────────────────

    /** En yüksek puanlı filmleri çeker — Keşfet TMDB sekmesi için */
    suspend fun getTopRatedMovies(
        page: Int = 1,
        apiKey: String,
        language: String,
        executeGet: suspend (String) -> String?
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "top_rated_movies_$page"
        get(cacheKey)?.let { return@withContext it }
        val url = "https://api.themoviedb.org/3/movie/top_rated?api_key=$apiKey&language=$language&page=$page"
        val result = parseTmdbDiscoverList(url, MediaType.Movie, executeGet)
        if (result.isNotEmpty()) put(cacheKey, result)
        result
    }

    /** En yüksek puanlı dizileri çeker — Keşfet TMDB sekmesi için */
    suspend fun getTopRatedShows(
        page: Int = 1,
        apiKey: String,
        language: String,
        executeGet: suspend (String) -> String?
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "top_rated_shows_$page"
        get(cacheKey)?.let { return@withContext it }
        val url = "https://api.themoviedb.org/3/tv/top_rated?api_key=$apiKey&language=$language&page=$page"
        val result = parseTmdbDiscoverList(url, MediaType.TvShow, executeGet)
        if (result.isNotEmpty()) put(cacheKey, result)
        result
    }

    /**
     * TMDB keşfet endpoint'i üzerinden belirli bir tür ID'sine göre içerik çeker.
     */
    suspend fun discoverByGenre(
        genreId: Int,
        isMovie: Boolean,
        apiKey: String,
        language: String,
        executeGet: suspend (String) -> String?
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "discover_genre_${genreId}_${if (isMovie) "movie" else "tv"}"
        get(cacheKey)?.let { return@withContext it }

        val endpoint = if (isMovie) "movie" else "tv"
        val mediaType = if (isMovie) MediaType.Movie else MediaType.TvShow
        val url = "https://api.themoviedb.org/3/discover/$endpoint?api_key=$apiKey&language=$language&with_genres=$genreId&sort_by=popularity.desc&page=1"
        val result = parseTmdbDiscoverList(url, mediaType, executeGet)

        if (result.isNotEmpty()) put(cacheKey, result)
        result
    }

    suspend fun getTrendingMedia(
        page: Int = 1,
        apiKey: String,
        language: String,
        executeGet: suspend (String) -> String?
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "trending_media_$page"
        get(cacheKey)?.let { return@withContext it }

        val tvUrl = "https://api.themoviedb.org/3/discover/tv?api_key=$apiKey&language=$language&with_genres=16&with_original_language=ja&sort_by=popularity.desc&page=$page"
        val tvResult = parseTmdbDiscoverList(tvUrl, MediaType.Anime, executeGet)

        val movieUrl = "https://api.themoviedb.org/3/discover/movie?api_key=$apiKey&language=$language&with_genres=16&with_original_language=ja&sort_by=popularity.desc&page=$page"
        val movieResult = parseTmdbDiscoverList(movieUrl, MediaType.Anime, executeGet)

        val merged = mutableListOf<JikanSearchResult>()
        val maxSize = maxOf(tvResult.size, movieResult.size)
        for (i in 0 until maxSize) {
            if (i < tvResult.size) merged.add(tvResult[i])
            if (i < movieResult.size) merged.add(movieResult[i])
        }
        val result = merged.take(20)

        if (result.isNotEmpty()) put(cacheKey, result)
        result
    }

    suspend fun getPopularMedia(
        page: Int = 1,
        apiKey: String,
        language: String,
        executeGet: suspend (String) -> String?
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "popular_media_$page"
        get(cacheKey)?.let { return@withContext it }

        val tvUrl = "https://api.themoviedb.org/3/discover/tv?api_key=$apiKey&language=$language&with_genres=16&with_original_language=ja&sort_by=vote_count.desc&page=$page"
        val tvResult = parseTmdbDiscoverList(tvUrl, MediaType.Anime, executeGet)

        val movieUrl = "https://api.themoviedb.org/3/discover/movie?api_key=$apiKey&language=$language&with_genres=16&with_original_language=ja&sort_by=vote_count.desc&page=$page"
        val movieResult = parseTmdbDiscoverList(movieUrl, MediaType.Anime, executeGet)

        val merged = mutableListOf<JikanSearchResult>()
        val maxSize = maxOf(tvResult.size, movieResult.size)
        for (i in 0 until maxSize) {
            if (i < tvResult.size) merged.add(tvResult[i])
            if (i < movieResult.size) merged.add(movieResult[i])
        }
        val result = merged.take(20)

        if (result.isNotEmpty()) put(cacheKey, result)
        result
    }

    suspend fun getTopRatedAnime(
        page: Int = 1,
        apiKey: String,
        language: String,
        executeGet: suspend (String) -> String?
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "top_rated_anime_$page"
        get(cacheKey)?.let { return@withContext it }

        val tvUrl = "https://api.themoviedb.org/3/discover/tv?api_key=$apiKey&language=$language&with_genres=16&with_original_language=ja&sort_by=vote_average.desc&vote_count.gte=200&page=$page"
        val tvResult = parseTmdbDiscoverList(tvUrl, MediaType.Anime, executeGet)

        val movieUrl = "https://api.themoviedb.org/3/discover/movie?api_key=$apiKey&language=$language&with_genres=16&with_original_language=ja&sort_by=vote_average.desc&vote_count.gte=100&page=$page"
        val movieResult = parseTmdbDiscoverList(movieUrl, MediaType.Anime, executeGet)

        val merged = mutableListOf<JikanSearchResult>()
        val maxSize = maxOf(tvResult.size, movieResult.size)
        for (i in 0 until maxSize) {
            if (i < tvResult.size) merged.add(tvResult[i])
            if (i < movieResult.size) merged.add(movieResult[i])
        }
        val result = merged.take(20)

        if (result.isNotEmpty()) put(cacheKey, result)
        result
    }

    suspend fun getUpcomingMedia(
        page: Int = 1,
        apiKey: String,
        language: String,
        executeGet: suspend (String) -> String?
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "upcoming_media_$page"
        get(cacheKey)?.let { return@withContext it }

        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

        val tvUrl = "https://api.themoviedb.org/3/discover/tv?api_key=$apiKey&language=$language&first_air_date.gte=$today&sort_by=first_air_date.asc&page=$page"
        val tvResult = parseTmdbDiscoverList(tvUrl, MediaType.TvShow, executeGet)

        val movieUrl = "https://api.themoviedb.org/3/discover/movie?api_key=$apiKey&language=$language&primary_release_date.gte=$today&sort_by=primary_release_date.asc&page=$page"
        val movieResult = parseTmdbDiscoverList(movieUrl, MediaType.Movie, executeGet)

        val merged = mutableListOf<JikanSearchResult>()
        merged.addAll(tvResult)
        merged.addAll(movieResult)

        val result = merged.sortedBy { item ->
            item.nextAiringEpisode?.split("|")?.getOrNull(1)?.toLongOrNull() ?: Long.MAX_VALUE
        }.take(20)

        if (result.isNotEmpty()) put(cacheKey, result)
        result
    }


    // ── Backdrop by Title ───────────────────────────────────────────────────────

    /**
     * Anime/dizi başlığına göre TMDB'de arama yapıp ilk sonucun backdrop URL'sini döner.
     * MAL (Jikan) platformunda vitrin öğeleri için kullanılır; Jikan API'si backdrop
     * sağlamadığından en iyi eşleşme üzerinden TMDB'den yatay kapak çekilir.
     */
    suspend fun fetchBackdropByTitle(
        title: String,
        apiKey: String,
        language: String,
        executeGet: suspend (String) -> String?
    ): String? = withContext(Dispatchers.IO) {
        val cacheKey = "backdrop_${title.lowercase().trim()}"
        val cached = get(cacheKey)
        if (cached != null) return@withContext cached.firstOrNull()?.backdropUrl

        return@withContext try {
            val encodedTitle = java.net.URLEncoder.encode(title.take(60), "UTF-8")
            val url = "https://api.themoviedb.org/3/search/multi?api_key=$apiKey&language=$language&query=$encodedTitle&page=1"
            val responseText = executeGet(url) ?: return@withContext null
            val root = JSONObject(responseText)
            val results = root.optJSONArray("results") ?: return@withContext null
            for (i in 0 until minOf(results.length(), 5)) {
                val item = results.getJSONObject(i)
                val mediaType = item.optString("media_type", "")
                if (mediaType == "person") continue
                val backdropPath = item.optNullableString("backdrop_path") ?: ""
                if (backdropPath.isNotEmpty()) {
                    val backdropUrl = "https://image.tmdb.org/t/p/w1280$backdropPath"
                    put(cacheKey, listOf(JikanSearchResult(
                        malId = 0, title = title, subtitle = "", type = MediaType.Anime,
                        total = null, score = null, isAdult = false, imageUrl = null, year = null,
                        source = "tmdb", backdropUrl = backdropUrl
                    )))
                    return@withContext backdropUrl
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "fetchBackdropByTitle error: ${e.message}")
            null
        }
    }

    // ── Private Parsers ─────────────────────────────────────────────────────────

    private suspend fun parseTmdbDiscoverList(
        url: String,
        mediaType: MediaType,
        executeGet: suspend (String) -> String?
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        try {
            val responseText = executeGet(url) ?: return@withContext emptyList()
            val root = JSONObject(responseText)
            val results = root.optJSONArray("results") ?: return@withContext emptyList()
            val list = mutableListOf<JikanSearchResult>()
            for (i in 0 until minOf(results.length(), 20)) {
                val item = results.getJSONObject(i)
                val tmdbId = item.optInt("id", 0).takeIf { it > 0 } ?: continue
                val isMovie = url.contains("/movie") || mediaType == MediaType.Movie
                val title = if (isMovie) item.optString("title", "") else item.optString("name", "")
                if (title.isBlank()) continue
                val posterPath = item.optNullableString("poster_path") ?: ""
                val backdropPath = item.optNullableString("backdrop_path") ?: ""
                val backdropUrl = if (backdropPath.isNotEmpty()) "https://image.tmdb.org/t/p/w1280$backdropPath" else null
                val releaseDate = if (isMovie) item.optString("release_date", "") else item.optString("first_air_date", "")
                val year = releaseDate.take(4).toIntOrNull()
                val rating = item.optDouble("vote_average", 0.0)
                val score = if (rating > 0.0) (rating * 10).toInt().coerceIn(0, 100) / 10 else null
                val imageUrl = if (posterPath.isNotEmpty()) "https://image.tmdb.org/t/p/w500$posterPath" else null
                val actualType = if (mediaType == MediaType.Anime) MediaType.Anime else if (isMovie) MediaType.Movie else MediaType.TvShow
                val subtitleParts = buildList {
                    if (mediaType == MediaType.Anime) {
                        add("Anime")
                        add(if (isMovie) "Film" else "Dizi")
                    } else {
                        add(if (isMovie) "Film" else "Dizi")
                    }
                    if (year != null && year > 0) add(year.toString())
                }
                val nextAiringEpisode = try {
                    if (releaseDate.isNotEmpty()) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        val date = sdf.parse(releaseDate)
                        val epoch = date?.time?.div(1000L)
                        val nowSeconds = System.currentTimeMillis() / 1000L
                        if (epoch != null && epoch > nowSeconds) {
                            "-1|$epoch"
                        } else null
                    } else null
                } catch (e: Exception) {
                    null
                }
                list.add(
                    JikanSearchResult(
                        malId = tmdbId, title = title,
                        subtitle = subtitleParts.joinToString(", "),
                        type = actualType, total = null, score = score,
                        isAdult = item.optBoolean("adult", false),
                        imageUrl = imageUrl, year = year, source = "tmdb",
                        realMalId = null, titleEnglish = title, titleJapanese = null,
                        tmdbId = tmdbId, backdropUrl = backdropUrl,
                        nextAiringEpisode = nextAiringEpisode
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "parseTmdbDiscoverList error: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun parseTmdbDiscoverListAll(
        url: String,
        executeGet: suspend (String) -> String?
    ): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        try {
            val responseText = executeGet(url) ?: return@withContext emptyList()
            val root = JSONObject(responseText)
            val results = root.optJSONArray("results") ?: return@withContext emptyList()
            val list = mutableListOf<JikanSearchResult>()
            for (i in 0 until minOf(results.length(), 20)) {
                val item = results.getJSONObject(i)
                val tmdbId = item.optInt("id", 0).takeIf { it > 0 } ?: continue
                val mediaTypeStr = item.optString("media_type", "movie")
                val isMovie = mediaTypeStr == "movie"
                val mediaType = if (isMovie) MediaType.Movie else MediaType.TvShow
                val title = if (isMovie) item.optString("title", "") else item.optString("name", "")
                if (title.isBlank()) continue
                val posterPath = item.optNullableString("poster_path") ?: ""
                val backdropPath = item.optNullableString("backdrop_path") ?: ""
                val backdropUrl = if (backdropPath.isNotEmpty()) "https://image.tmdb.org/t/p/w1280$backdropPath" else null
                val releaseDate = if (isMovie) item.optString("release_date", "") else item.optString("first_air_date", "")
                val year = releaseDate.take(4).toIntOrNull()
                val rating = item.optDouble("vote_average", 0.0)
                val score = if (rating > 0.0) (rating * 10).toInt().coerceIn(0, 100) / 10 else null
                val imageUrl = if (posterPath.isNotEmpty()) "https://image.tmdb.org/t/p/w500$posterPath" else null
                val subtitleParts = buildList {
                    add(if (isMovie) "Film" else "Dizi")
                    if (year != null && year > 0) add(year.toString())
                }
                val nextAiringEpisode = try {
                    if (releaseDate.isNotEmpty()) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        val date = sdf.parse(releaseDate)
                        val epoch = date?.time?.div(1000L)
                        val nowSeconds = System.currentTimeMillis() / 1000L
                        if (epoch != null && epoch > nowSeconds) {
                            "-1|$epoch"
                        } else null
                    } else null
                } catch (e: Exception) {
                    null
                }
                list.add(
                    JikanSearchResult(
                        malId = tmdbId, title = title,
                        subtitle = subtitleParts.joinToString(", "),
                        type = mediaType, total = null, score = score,
                        isAdult = item.optBoolean("adult", false),
                        imageUrl = imageUrl, year = year, source = "tmdb",
                        realMalId = null, titleEnglish = title, titleJapanese = null,
                        tmdbId = tmdbId, backdropUrl = backdropUrl,
                        nextAiringEpisode = nextAiringEpisode
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "parseTmdbDiscoverListAll error: ${e.message}", e)
            emptyList()
        }
    }
}
