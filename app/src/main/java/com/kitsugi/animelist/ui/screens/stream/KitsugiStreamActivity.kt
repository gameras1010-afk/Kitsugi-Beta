package com.kitsugi.animelist.ui.screens.stream

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.google.gson.Gson
import com.kitsugi.animelist.core.player.ExternalPlayerResultContract
import com.kitsugi.animelist.ui.screens.fullscreen.components.MetaCastMember
import com.kitsugi.animelist.ui.theme.KitsugiAnimeListTheme
import android.util.Log
import com.kitsugi.animelist.DeviceProfile
import com.kitsugi.animelist.DeviceFormFactor

class KitsugiStreamActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_MAL_ID         = "extra_mal_id"
        private const val EXTRA_ANILIST_ID     = "extra_anilist_id"
        private const val EXTRA_TMDB_ID        = "extra_tmdb_id"
        private const val EXTRA_EPISODE        = "extra_episode"
        private const val EXTRA_SEASON         = "extra_season"
        private const val EXTRA_IS_MOVIE       = "extra_is_movie"
        private const val EXTRA_TITLE          = "extra_title"
        private const val EXTRA_POSTER_URL     = "extra_poster_url"
        private const val EXTRA_TITLE_ENGLISH  = "extra_title_english"
        private const val EXTRA_TITLE_ROMAJI   = "extra_title_romaji"
        private const val EXTRA_TITLE_NATIVE   = "extra_title_native"
        private const val EXTRA_START_YEAR     = "extra_start_year"
        private const val EXTRA_DESCRIPTION    = "extra_description"
        private const val EXTRA_CAST_JSON      = "extra_cast_json"

        private const val EXTRA_IS_AUTOPLAY    = "extra_is_autoplay"

        const val PREFS_NAME  = "KitsugiStreamPrefs"
        const val KEY_POS_PFX = "pos_"

        @Volatile
        var tempCast: List<MetaCastMember>? = null

        fun start(
            context: Context,
            malId: Int?,
            aniListId: Int?,
            tmdbId: Int? = null,
            episode: Int,
            season: Int = 1,
            isMovie: Boolean = false,
            title: String,
            posterUrl: String? = null,
            titleEnglish: String? = null,
            titleRomaji: String? = null,
            titleNative: String? = null,
            startYear: Int? = null,
            description: String? = null,
            cast: List<MetaCastMember> = emptyList(),
            isAutoplay: Boolean = false
        ) {
            tempCast = cast
            context.startActivity(
                Intent(context, KitsugiStreamActivity::class.java).apply {
                    malId?.let          { putExtra(EXTRA_MAL_ID, it) }
                    aniListId?.let      { putExtra(EXTRA_ANILIST_ID, it) }
                    tmdbId?.let         { putExtra(EXTRA_TMDB_ID, it) }
                    putExtra(EXTRA_EPISODE, episode)
                    putExtra(EXTRA_SEASON, season)
                    putExtra(EXTRA_IS_MOVIE, isMovie)
                    putExtra(EXTRA_TITLE, title)
                    posterUrl?.let      { putExtra(EXTRA_POSTER_URL, it) }
                    titleEnglish?.let   { putExtra(EXTRA_TITLE_ENGLISH, it) }
                    titleRomaji?.let    { putExtra(EXTRA_TITLE_ROMAJI, it) }
                    titleNative?.let    { putExtra(EXTRA_TITLE_NATIVE, it) }
                    startYear?.let      { putExtra(EXTRA_START_YEAR, it) }
                    description?.let    { putExtra(EXTRA_DESCRIPTION, it) }
                    putExtra(EXTRA_IS_AUTOPLAY, isAutoplay)
                }
            )
        }
    }

    private val externalPlayerLauncher = registerForActivityResult(
        ExternalPlayerResultContract()
    ) { result ->
        // Servisi durdur
        com.kitsugi.animelist.core.player.KeepAliveService.stop(this)

        if (result == null) { Log.d("KitsugiStream", "External player returned no result"); return@registerForActivityResult }
        Log.d("KitsugiStream", "External player result → pos=${result.positionMs}ms dur=${result.durationMs}ms")
        val key = currentStreamKey
        if (key != null && result.positionMs > 0L) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putLong(KEY_POS_PFX + key, result.positionMs)
                .also { editor -> result.durationMs?.let { editor.putLong("${KEY_POS_PFX}dur_$key", it) } }
                .apply()
        }

        // T1.8: Otomatik sonraki bölümü başlatma
        val autoPlay = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean("autoplay", true)
        if (com.kitsugi.animelist.core.player.ExternalAutoNextPolicy.shouldAutoNext(
                positionMs = result.positionMs,
                durationMs = result.durationMs,
                endedByUser = result.endedByUser,
                autoPlayEnabled = autoPlay
            )
        ) {
            val nextEp = currentEpisode + 1
            Log.d("KitsugiStream", "AutoPlay Next Episode: S${currentSeason}E${nextEp}")
            // KitsugiStreamActivity'yi bir sonraki bölüm için başlat ve eskisini kapat
            start(
                context = this,
                malId = currentMalId,
                aniListId = currentAniList,
                tmdbId = currentTmdbId,
                episode = nextEp,
                season = currentSeason,
                isMovie = currentIsMovie,
                title = currentTitle,
                posterUrl = currentPosterUrl,
                titleEnglish = currentTitleEnglish,
                titleRomaji = currentTitleRomaji,
                titleNative = currentTitleNative,
                startYear = currentStartYear,
                description = currentDescription,
                isAutoplay = true
            )
            finish()
        }
    }

    var currentStreamKey: String? = null

    // Fields for auto-next transition
    private var currentMalId: Int? = null
    private var currentAniList: Int? = null
    private var currentTmdbId: Int? = null
    private var currentEpisode: Int = 1
    private var currentSeason: Int = 1
    private var currentIsMovie: Boolean = false
    private var currentTitle: String = ""
    private var currentPosterUrl: String? = null
    private var currentTitleEnglish: String? = null
    private var currentTitleRomaji: String? = null
    private var currentTitleNative: String? = null
    private var currentStartYear: Int? = null
    private var currentDescription: String? = null
    private var isAutoplayMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        currentMalId        = if (intent.hasExtra(EXTRA_MAL_ID)) intent.getIntExtra(EXTRA_MAL_ID, 0).takeIf { it > 0 } else null
        currentAniList    = if (intent.hasExtra(EXTRA_ANILIST_ID)) intent.getIntExtra(EXTRA_ANILIST_ID, 0).takeIf { it > 0 } else null
        currentTmdbId       = if (intent.hasExtra(EXTRA_TMDB_ID)) intent.getIntExtra(EXTRA_TMDB_ID, 0).takeIf { it > 0 } else null
        currentEpisode      = intent.getIntExtra(EXTRA_EPISODE, 1)
        currentSeason       = intent.getIntExtra(EXTRA_SEASON, 1)
        currentIsMovie      = intent.getBooleanExtra(EXTRA_IS_MOVIE, false)
        currentTitle        = intent.getStringExtra(EXTRA_TITLE) ?: ""
        currentPosterUrl    = intent.getStringExtra(EXTRA_POSTER_URL)
        currentTitleEnglish = intent.getStringExtra(EXTRA_TITLE_ENGLISH)
        currentTitleRomaji  = intent.getStringExtra(EXTRA_TITLE_ROMAJI)
        currentTitleNative  = intent.getStringExtra(EXTRA_TITLE_NATIVE)
        currentStartYear    = if (intent.hasExtra(EXTRA_START_YEAR)) intent.getIntExtra(EXTRA_START_YEAR, 0).takeIf { it > 0 } else null
        currentDescription  = intent.getStringExtra(EXTRA_DESCRIPTION)
        isAutoplayMode      = intent.getBooleanExtra(EXTRA_IS_AUTOPLAY, false)

        val castList: List<MetaCastMember> = tempCast ?: run {
            val castJson = intent.getStringExtra(EXTRA_CAST_JSON)
            if (!castJson.isNullOrEmpty()) {
                try { val type = object : com.google.gson.reflect.TypeToken<List<MetaCastMember>>() {}.type; Gson().fromJson(castJson, type) }
                catch (_: Exception) { emptyList() }
            } else emptyList()
        }
        tempCast = null

        val isTv = DeviceProfile.detect(this) == DeviceFormFactor.TV
        setContent {
            KitsugiAnimeListTheme(isTv = isTv) {
                KitsugiStreamScreen(
                    malId = currentMalId, aniListId = currentAniList, tmdbId = currentTmdbId,
                    episode = currentEpisode, season = currentSeason, isMovie = currentIsMovie,
                    title = currentTitle, posterUrl = currentPosterUrl, titleEnglish = currentTitleEnglish,
                    titleRomaji = currentTitleRomaji, titleNative = currentTitleNative, startYear = currentStartYear,
                    description = currentDescription, castList = castList,
                    isAutoplay = isAutoplayMode,
                    onBack = { finish() },
                    onLaunchExternalPlayer = { input, streamKey ->
                        currentStreamKey = streamKey
                        com.kitsugi.animelist.core.player.KeepAliveService.start(this)
                        externalPlayerLauncher.launch(input)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tempCast = null
    }
}
