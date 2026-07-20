package com.kitsugi.animelist.ui.screens.fullscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kitsugi.animelist.core.player.ExternalPlayerLauncher
import com.kitsugi.animelist.core.player.PlayerMediaSessionHelper
import com.kitsugi.animelist.core.player.PlayerPipHelper
import com.kitsugi.animelist.core.player.SubtitleInput
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.ui.screens.fullscreen.components.MetaCastMember
import com.kitsugi.animelist.ui.theme.KitsugiAnimeListTheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KitsugiFullscreenPlayerActivity : ComponentActivity() {

    private var isPipEnabled = true

    // ── T2.3: MediaSession + PiP BroadcastReceiver ────────────────────────────
    private var mediaSessionHelper: PlayerMediaSessionHelper? = null

    /** ViewModel referansı — PiP broadcast'leri ViewModel'a iletilir */
    private var pipPlayerCallback: PipPlayerCallback? = null

    interface PipPlayerCallback {
        fun onPipPlay()
        fun onPipPause()
        fun onPipSkipNext()
    }

    private val pipBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PlayerPipHelper.ACTION_PLAY -> pipPlayerCallback?.onPipPlay()
                PlayerPipHelper.ACTION_PAUSE -> pipPlayerCallback?.onPipPause()
                PlayerPipHelper.ACTION_SKIP_NEXT -> pipPlayerCallback?.onPipSkipNext()
            }
        }
    }

    companion object {
        const val EXTRA_VIDEO_ID  = "extra_video_id"
        const val EXTRA_VIDEO_URL = "extra_video_url"
        const val EXTRA_AUDIO_URL = "extra_audio_url"
        const val EXTRA_TITLE     = "extra_title"
        const val EXTRA_HEADERS   = "extra_headers"

        const val EXTRA_SUBTITLES_JSON = "extra_subtitles_json"
        const val EXTRA_STREAM_LIST_JSON = "extra_stream_list_json"
        const val EXTRA_CURRENT_INDEX = "extra_current_index"
        const val EXTRA_MAL_ID = "extra_mal_id"
        const val EXTRA_ANILIST_ID = "extra_anilist_id"
        const val EXTRA_TMDB_ID = "extra_tmdb_id"
        const val EXTRA_EPISODE = "extra_episode"
        const val EXTRA_SEASON = "extra_season"
        const val EXTRA_ANIME_TITLE = "extra_anime_title"
        const val EXTRA_POSTER_URL = "extra_poster_url"
        const val EXTRA_TITLE_ENGLISH = "extra_title_english"
        const val EXTRA_TITLE_ROMAJI = "extra_title_romaji"
        const val EXTRA_TITLE_NATIVE = "extra_title_native"
        const val EXTRA_START_YEAR = "extra_start_year"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_CAST_JSON   = "extra_cast_json"

        @Volatile
        var tempStreamSources: List<StreamSource>? = null

        @Volatile
        var tempCast: List<MetaCastMember>? = null

        @Volatile
        var tempSubtitles: List<SubtitleInput>? = null

        fun startWithYouTubeId(context: Context, videoId: String, title: String = "") {
            context.startActivity(
                Intent(context, KitsugiFullscreenPlayerActivity::class.java).apply {
                    putExtra(EXTRA_VIDEO_ID, videoId)
                    putExtra(EXTRA_TITLE, title)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }

        fun startWithStreamUrls(
            context: Context,
            videoUrl: String,
            audioUrl: String? = null,
            title: String = "",
            headers: Map<String, String>? = null,
            subtitles: List<SubtitleInput> = emptyList(),
            allSources: List<StreamSource> = emptyList(),
            currentSourceIndex: Int = -1,
            malId: Int? = null,
            aniListId: Int? = null,
            tmdbId: Int? = null,
            season: Int = 1,
            episode: Int = 1,
            animeTitle: String = "",
            posterUrl: String? = null,
            titleEnglish: String? = null,
            titleRomaji: String? = null,
            titleNative: String? = null,
            startYear: Int? = null,
            description: String? = null,
            cast: List<MetaCastMember> = emptyList()
        ) {
            tempSubtitles = subtitles
            tempStreamSources = allSources
            tempCast = cast

            context.startActivity(
                Intent(context, KitsugiFullscreenPlayerActivity::class.java).apply {
                    putExtra(EXTRA_VIDEO_URL, videoUrl)
                    putExtra(EXTRA_AUDIO_URL, audioUrl)
                    putExtra(EXTRA_TITLE, title)
                    if (!headers.isNullOrEmpty()) {
                        val bundle = android.os.Bundle()
                        headers.forEach { (k, v) -> bundle.putString(k, v) }
                        putExtra(EXTRA_HEADERS, bundle)
                    }
                    putExtra(EXTRA_CURRENT_INDEX, currentSourceIndex)
                    malId?.let { putExtra(EXTRA_MAL_ID, it) }
                    aniListId?.let { putExtra(EXTRA_ANILIST_ID, it) }
                    tmdbId?.let { putExtra(EXTRA_TMDB_ID, it) }
                    putExtra(EXTRA_SEASON, season)
                    putExtra(EXTRA_EPISODE, episode)
                    putExtra(EXTRA_ANIME_TITLE, animeTitle)
                    putExtra(EXTRA_POSTER_URL, posterUrl)
                    putExtra(EXTRA_TITLE_ENGLISH, titleEnglish)
                    putExtra(EXTRA_TITLE_ROMAJI, titleRomaji)
                    putExtra(EXTRA_TITLE_NATIVE, titleNative)
                    startYear?.let { putExtra(EXTRA_START_YEAR, it) }
                    description?.let { putExtra(EXTRA_DESCRIPTION, it) }
                    
                    val gson = com.google.gson.Gson()
                    if (subtitles.isNotEmpty()) {
                        putExtra(EXTRA_SUBTITLES_JSON, gson.toJson(subtitles))
                    }
                    if (allSources.isNotEmpty()) {
                        putExtra(EXTRA_STREAM_LIST_JSON, gson.toJson(allSources))
                    }
                    if (cast.isNotEmpty()) {
                        putExtra(EXTRA_CAST_JSON, gson.toJson(cast))
                    }

                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }

        fun launchExternalPlayer(
            context: Context,
            videoUrl: String,
            title: String,
            positionMs: Long,
            headers: Map<String, String>? = null,
            subtitles: List<SubtitleInput>? = null
        ) {
            val launched = ExternalPlayerLauncher.launch(
                context          = context,
                url              = videoUrl,
                title            = title,
                headers          = headers,
                resumePositionMs = positionMs,
                subtitles        = subtitles
            )
            if (!launched) {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (_: Exception) {
                    Toast.makeText(context, "Harici oynatıcı başlatılamadı.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // T1.8: KeepAliveService start
        com.kitsugi.animelist.core.player.KeepAliveService.start(this)

        // T2.3: PiP BroadcastReceiver kaydı
        val filter = IntentFilter().apply {
            addAction(PlayerPipHelper.ACTION_PLAY)
            addAction(PlayerPipHelper.ACTION_PAUSE)
            addAction(PlayerPipHelper.ACTION_SKIP_NEXT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipBroadcastReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(pipBroadcastReceiver, filter)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val videoId  = intent.getStringExtra(EXTRA_VIDEO_ID)
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
        val audioUrl = intent.getStringExtra(EXTRA_AUDIO_URL)
        val title    = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val headersBundle = intent.getBundleExtra(EXTRA_HEADERS)
        val requestHeaders: Map<String, String> = if (headersBundle != null) {
            buildMap { headersBundle.keySet().forEach { k -> headersBundle.getString(k)?.let { v -> put(k, v) } } }
        } else emptyMap()

        val initialSubtitles: List<SubtitleInput> = tempSubtitles ?: run {
            val subtitlesJson = intent.getStringExtra(EXTRA_SUBTITLES_JSON)
            if (!subtitlesJson.isNullOrEmpty()) {
                try {
                    val type = object : com.google.gson.reflect.TypeToken<List<SubtitleInput>>() {}.type
                    com.google.gson.Gson().fromJson(subtitlesJson, type)
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()
        }
        tempSubtitles = null

        val rawStreamSources: List<StreamSource> = tempStreamSources ?: run {
            val streamsJson = intent.getStringExtra(EXTRA_STREAM_LIST_JSON)
            if (!streamsJson.isNullOrEmpty()) {
                try {
                    val type = object : com.google.gson.reflect.TypeToken<List<StreamSource>>() {}.type
                    com.google.gson.Gson().fromJson(streamsJson, type)
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()
        }
        tempStreamSources = null
        val streamSources = rawStreamSources.map {
            it.copy(
                subtitles = it.subtitles ?: emptyList(),
                addonName = it.addonName ?: "Bilinmeyen Eklenti",
                name = it.name ?: "",
                title = it.title ?: ""
            )
        }

        val currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, -1)
        val malId = intent.getIntExtra(EXTRA_MAL_ID, -1).takeIf { it != -1 }
        val aniListId = intent.getIntExtra(EXTRA_ANILIST_ID, -1).takeIf { it != -1 }
        val tmdbId = intent.getIntExtra(EXTRA_TMDB_ID, -1).takeIf { it != -1 }
        val season = intent.getIntExtra(EXTRA_SEASON, 1)
        val episode = intent.getIntExtra(EXTRA_EPISODE, 1)
        val animeTitle = intent.getStringExtra(EXTRA_ANIME_TITLE) ?: ""
        val posterUrl = intent.getStringExtra(EXTRA_POSTER_URL)
        val titleEnglish = intent.getStringExtra(EXTRA_TITLE_ENGLISH)
        val titleRomaji = intent.getStringExtra(EXTRA_TITLE_ROMAJI)
        val titleNative = intent.getStringExtra(EXTRA_TITLE_NATIVE)
        val startYear = intent.getIntExtra(EXTRA_START_YEAR, -1).takeIf { it != -1 }
        val description = intent.getStringExtra(EXTRA_DESCRIPTION)

        val castList: List<MetaCastMember> = tempCast ?: run {
            val castJson = intent.getStringExtra(EXTRA_CAST_JSON)
            if (!castJson.isNullOrEmpty()) {
                try {
                    val type = object : com.google.gson.reflect.TypeToken<List<MetaCastMember>>() {}.type
                    com.google.gson.Gson().fromJson(castJson, type)
                } catch (_: Exception) { emptyList() }
            } else emptyList()
        }
        tempCast = null

        lifecycleScope.launch {
            SettingsDataStore(applicationContext).settingsFlow.collectLatest { settings ->
                isPipEnabled = settings.pipEnabled
            }
        }

        // T2.3: MediaSession başlat
        mediaSessionHelper = PlayerMediaSessionHelper(
            context = this,
            title = animeTitle.ifBlank { title },
            onPlay = { pipPlayerCallback?.onPipPlay() },
            onPause = { pipPlayerCallback?.onPipPause() },
            onSkipNext = { pipPlayerCallback?.onPipSkipNext() }
        ).also { helper ->
            helper.setMetadata(
                title = animeTitle.ifBlank { title },
                subtitle = if (episode > 0) "Bölüm $episode" else "",
                durationMs = 0L
            )
            helper.updatePlaybackState(isPlaying = true, positionMs = 0L, hasNext = false)
        }

        setContent {
            KitsugiAnimeListTheme {
                KitsugiFullscreenPlayerScreen(
                    videoId          = videoId,
                    videoUrl         = videoUrl,
                    audioUrl         = audioUrl,
                    title            = title,
                    requestHeaders   = requestHeaders,
                    initialSubtitles = initialSubtitles,
                    streamSources    = streamSources,
                    initialIndex     = currentIndex,
                    malId            = malId,
                    aniListId        = aniListId,
                    tmdbId           = tmdbId,
                    season           = season,
                    episode          = episode,
                    animeTitle       = animeTitle,
                    posterUrl        = posterUrl,
                    titleEnglish     = titleEnglish,
                    titleRomaji      = titleRomaji,
                    titleNative      = titleNative,
                    startYear        = startYear,
                    description      = description,
                    castList         = castList,
                    onBack           = { finish() }
                )
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isPipEnabled) {
            PlayerPipHelper.enterPipSafe(this, null, isPlaying = true, hasNext = false)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PlayerPipHelper.onPipModeChanged(isInPictureInPictureMode) { /* Screen observes via ViewModel */ }
    }

    /**
     * T2.3: MediaSession metadata/state güncellemesi — ViewModel veya Screen tarafından çağrılır.
     */
    fun updateMediaSession(
        title: String,
        episode: Int,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
        hasNext: Boolean = false
    ) {
        val helper = mediaSessionHelper ?: return
        helper.setMetadata(title = title, subtitle = "Bölüm $episode", durationMs = durationMs)
        helper.updatePlaybackState(isPlaying = isPlaying, positionMs = positionMs, hasNext = hasNext)
        if (isPipEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PlayerPipHelper.updatePipActions(this, null, isPlaying, hasNext)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tempStreamSources = null
        tempCast = null
        tempSubtitles = null
        // T2.3: MediaSession temizliği
        mediaSessionHelper?.release()
        mediaSessionHelper = null
        unregisterReceiver(pipBroadcastReceiver)
        // T1.8: KeepAliveService stop
        com.kitsugi.animelist.core.player.KeepAliveService.stop(this)
    }
}
