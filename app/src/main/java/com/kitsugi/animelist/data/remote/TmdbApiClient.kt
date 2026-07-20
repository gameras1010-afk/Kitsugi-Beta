package com.kitsugi.animelist.data.remote

import android.util.Log
import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.kitsugi.animelist.utils.*
import java.util.concurrent.TimeUnit

/**
 * TMDB API ana giriş noktası (Orkestrasyon katmanı).
 *
 * İç iş mantığı aşağıdaki modüllere delege edilir:
 *  - [TmdbMediaDetailClient]  → medya detayı, görseller, izleme sağlayıcıları, videolar
 *  - [TmdbCreditsClient]      → oyuncu/ekip, yorumlar, öneriler, istatistikler
 *  - [TmdbDiscoverClient]     → trending/popular/top-rated keşfet + backdrop arama
 *
 * Kullanıcı API anahtarı boşsa dahili yedek anahtar devreye girer.
 */
class TmdbApiClient(
    /** Kullanıcı API anahtarı — boşsa dahili anahtar [BUILT_IN_API_KEY] devreye girer */
    private val userApiKey: String = ""
) {
    /**
     * TMDB'ye özgü OkHttpClient — kısa timeout ile.
     * Global KitsugiHttpClient 15s kullanırken TMDB trending endpoint'leri
     * timeout'a girebilir. 8s ile hızlı fail + /discover fallback devreye girer.
     */
    private val client = OkHttpClient.Builder()
        .dns(com.kitsugi.animelist.core.network.IPv4FirstDns())
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    private val apiKey = resolveApiKey(userApiKey)
    private val TAG = "TmdbApiClient"

    companion object {
        /**
         * Dahili (yedek) TMDB API anahtarı.
         * Öncelik sırası:
         *  1. local.properties'teki `tmdb_api_key=` (BuildConfig üzerinden)
         *  2. Uygulama ayarlarındaki kullanıcı anahtarı (TmdbApiClient'e userApiKey olarak geçilir)
         *  3. Bu fallback (açık kaynak — rate-limit paylaşımlı)
         */
        private const val FALLBACK_KEY = "8265bd1679663a7ea12ac168da84d2e8"
        val BUILT_IN_API_KEY: String get() {
            val fromBuild = com.kitsugi.animelist.BuildConfig.TMDB_API_KEY
            return if (fromBuild.isBlank() || fromBuild == "YOUR_TMDB_API_KEY_HERE") {
                FALLBACK_KEY
            } else {
                fromBuild
            }
        }

        fun resolveApiKey(userKey: String): String = userKey.trim().ifBlank { BUILT_IN_API_KEY }

        fun getActiveApiKey(): String {
            val context = com.kitsugi.animelist.KitsugiApplication.getInstance()?.applicationContext ?: return BUILT_IN_API_KEY
            val userKey = runCatching {
                kotlinx.coroutines.runBlocking {
                    com.kitsugi.animelist.data.settings.SettingsDataStore(context).settingsFlow.first().tmdbUserApiKey
                }
            }.getOrDefault("")
            return resolveApiKey(userKey)
        }
    }

    private suspend fun isTmdbEnabled(): Boolean {
        val context = com.kitsugi.animelist.KitsugiApplication.getInstance()?.applicationContext ?: return true
        return try {
            kotlinx.coroutines.withTimeoutOrNull(800L) {
                com.kitsugi.animelist.data.settings.SettingsDataStore(context).settingsFlow.first().tmdbEnabled
            } ?: true
        } catch (e: Exception) {
            true
        }
    }

    // ── Medya Detayı ────────────────────────────────────────────────────────────

    suspend fun fetchMediaDetail(tmdbId: Int, isMovie: Boolean): KitsugiMediaDetail? {
        if (!isTmdbEnabled()) return null
        return TmdbMediaDetailClient.fetchMediaDetail(tmdbId, isMovie, apiKey, ::executeGet)
    }

    suspend fun fetchMediaImages(tmdbId: Int, isMovie: Boolean): List<String> = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext emptyList()
        TmdbMediaDetailClient.fetchMediaImages(tmdbId, isMovie, apiKey, ::executeGet)
    }

    suspend fun fetchWatchProviders(tmdbId: Int, isMovie: Boolean): List<KitsugiExternalLink> = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext emptyList()
        TmdbMediaDetailClient.fetchWatchProviders(tmdbId, isMovie, apiKey, ::executeGet)
    }

    suspend fun fetchVideos(tmdbId: Int, isMovie: Boolean): Pair<String?, List<KitsugiTheme>> = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext Pair(null, emptyList())
        TmdbMediaDetailClient.fetchVideos(tmdbId, isMovie, apiKey, ::executeGet)
    }

    // ── Oyuncu / Ekip / İçerik ─────────────────────────────────────────────────

    suspend fun fetchCredits(tmdbId: Int, isMovie: Boolean): Pair<List<KitsugiCharacter>, List<KitsugiStaff>> = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext Pair(emptyList(), emptyList())
        TmdbCreditsClient.fetchCredits(tmdbId, isMovie, apiKey, ::executeGet)
    }

    suspend fun fetchRecommendations(tmdbId: Int, isMovie: Boolean): List<KitsugiRelation> = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext emptyList()
        TmdbCreditsClient.fetchRecommendations(tmdbId, isMovie, apiKey, ::executeGet)
    }

    suspend fun fetchReviews(tmdbId: Int, isMovie: Boolean): List<KitsugiReview> = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext emptyList()
        TmdbCreditsClient.fetchReviews(tmdbId, isMovie, apiKey, ::executeGet)
    }

    suspend fun fetchPersonCharacterDetail(personId: Int): KitsugiCharacterDetail? = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext null
        TmdbCreditsClient.fetchPersonCharacterDetail(personId, apiKey, ::executeGet)
    }

    suspend fun fetchPersonStaffDetail(personId: Int): KitsugiStaffDetail? = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext null
        TmdbCreditsClient.fetchPersonStaffDetail(personId, apiKey, ::executeGet)
    }

    suspend fun fetchStats(tmdbId: Int, isMovie: Boolean): KitsugiStats? = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext null
        TmdbCreditsClient.fetchStats(tmdbId, isMovie, apiKey, ::executeGet)
    }

    // ── Keşfet (Trending / Popular / Top Rated) ─────────────────────────────────

    suspend fun getTrendingMovies(page: Int = 1): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        // isTmdbEnabled() burada kontrol edilmiyor — keşfet/discovery akışı
        // zaten ViewModel seviyesinde guard'landı; client başına DataStore okuma
        // race condition'a ve yavaşlamaya yol açıyordu.
        TmdbDiscoverClient.getTrendingMovies(page, apiKey, ::executeGet)
    }

    suspend fun getTrendingShows(page: Int = 1): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        TmdbDiscoverClient.getTrendingShows(page, apiKey, ::executeGet)
    }

    suspend fun getTrendingAll(page: Int = 1): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        TmdbDiscoverClient.getTrendingAll(page, apiKey, ::executeGet)
    }

    suspend fun getPopularMovies(page: Int = 1): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        TmdbDiscoverClient.getPopularMovies(page, apiKey, ::executeGet)
    }

    suspend fun getPopularShows(page: Int = 1): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        TmdbDiscoverClient.getPopularShows(page, apiKey, ::executeGet)
    }

    suspend fun getTopRatedMovies(page: Int = 1): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        TmdbDiscoverClient.getTopRatedMovies(page, apiKey, ::executeGet)
    }

    suspend fun getTopRatedShows(page: Int = 1): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        TmdbDiscoverClient.getTopRatedShows(page, apiKey, ::executeGet)
    }

    suspend fun fetchBackdropByTitle(title: String): String? = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext null
        TmdbDiscoverClient.fetchBackdropByTitle(title, apiKey, ::executeGet)
    }

    // ── Arama ───────────────────────────────────────────────────────────────────

    suspend fun search(query: String): List<JikanSearchResult> = withContext(Dispatchers.IO) {
        if (!isTmdbEnabled()) return@withContext emptyList()
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://api.themoviedb.org/3/search/multi?api_key=$apiKey&language=tr-TR&query=$encodedQuery&page=1"
        try {
            val responseText = executeGet(url) ?: return@withContext emptyList()
            val root = JSONObject(responseText)
            val results = root.optJSONArray("results") ?: return@withContext emptyList()
            val list = mutableListOf<JikanSearchResult>()
            for (i in 0 until minOf(results.length(), 20)) {
                val item = results.getJSONObject(i)
                val tmdbId = item.optInt("id", 0).takeIf { it > 0 } ?: continue
                val mediaTypeStr = item.optString("media_type", "movie")
                if (mediaTypeStr == "person") continue
                val isMovie = mediaTypeStr == "movie"
                val mediaType = if (isMovie) MediaType.Movie else MediaType.TvShow
                val title = if (isMovie) item.optString("title", "") else item.optString("name", "")
                if (title.isBlank()) continue
                val posterPath = item.optNullableString("poster_path") ?: ""
                val releaseDate = if (isMovie) item.optString("release_date", "") else item.optString("first_air_date", "")
                val year = releaseDate.take(4).toIntOrNull()
                val rating = item.optDouble("vote_average", 0.0)
                val score = if (rating > 0.0) (rating * 10).toInt().coerceIn(0, 100) / 10 else null
                val imageUrl = if (posterPath.isNotEmpty()) "https://image.tmdb.org/t/p/w500$posterPath" else null
                val subtitleParts = buildList {
                    add(if (isMovie) "Film" else "Dizi")
                    if (year != null && year > 0) add(year.toString())
                }
                list.add(
                    JikanSearchResult(
                        malId = tmdbId, title = title,
                        subtitle = subtitleParts.joinToString(", "),
                        type = mediaType, total = null, score = score,
                        isAdult = item.optBoolean("adult", false),
                        imageUrl = imageUrl, year = year, source = "tmdb",
                        realMalId = null, titleEnglish = title, titleJapanese = null,
                        tmdbId = tmdbId
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "search error: ${e.message}", e)
            emptyList()
        }
    }

    // ── Ağ ─────────────────────────────────────────────────────────────────────

    private suspend fun executeGet(urlStr: String): String? = withContext(Dispatchers.IO) {
        // withTimeoutOrNull: coroutine düzeyinde güvenlik ağı (OkHttp timeout'una ek olarak)
        withTimeoutOrNull(9_000L) {
            try {
                val request = Request.Builder()
                    .url(urlStr)
                    .header("Accept", "application/json")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()
                    } else {
                        Log.e(TAG, "HTTP ${response.code} for $urlStr")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "executeGet exception: ${e.message} — $urlStr")
                null
            }
        }
    }
}
