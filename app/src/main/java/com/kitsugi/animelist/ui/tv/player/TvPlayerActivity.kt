package com.kitsugi.animelist.ui.tv.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kitsugi.animelist.core.player.SubtitleInput
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.ui.screens.fullscreen.components.MetaCastMember
import com.kitsugi.animelist.ui.theme.KitsugiAnimeListTheme

class TvPlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_VIDEO_URL = "extra_video_url"
        const val EXTRA_AUDIO_URL = "extra_audio_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_HEADERS = "extra_headers"
        const val EXTRA_CURRENT_INDEX = "extra_current_index"
        const val EXTRA_MAL_ID = "extra_mal_id"
        const val EXTRA_ANILIST_ID = "extra_anilist_id"
        const val EXTRA_SEASON = "extra_season"
        const val EXTRA_EPISODE = "extra_episode"
        const val EXTRA_ANIME_TITLE = "extra_anime_title"
        const val EXTRA_POSTER_URL = "extra_poster_url"
        const val EXTRA_TITLE_ENGLISH = "extra_title_english"
        const val EXTRA_TITLE_ROMAJI = "extra_title_romaji"
        const val EXTRA_TITLE_NATIVE = "extra_title_native"
        const val EXTRA_START_YEAR = "extra_start_year"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_STREAM_SOURCES = "extra_stream_sources"
        const val EXTRA_CAST = "extra_cast"
        const val EXTRA_SUBTITLES = "extra_subtitles"

        fun start(
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
            context.startActivity(
                Intent(context, TvPlayerActivity::class.java).apply {
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
                    putExtra(EXTRA_SEASON, season)
                    putExtra(EXTRA_EPISODE, episode)
                    putExtra(EXTRA_ANIME_TITLE, animeTitle)
                    putExtra(EXTRA_POSTER_URL, posterUrl)
                    putExtra(EXTRA_TITLE_ENGLISH, titleEnglish)
                    putExtra(EXTRA_TITLE_ROMAJI, titleRomaji)
                    putExtra(EXTRA_TITLE_NATIVE, titleNative)
                    startYear?.let { putExtra(EXTRA_START_YEAR, it) }
                    putExtra(EXTRA_DESCRIPTION, description)
                    putExtra(EXTRA_SUBTITLES, ArrayList(subtitles))
                    putExtra(EXTRA_STREAM_SOURCES, ArrayList(allSources))
                    putExtra(EXTRA_CAST, ArrayList(cast))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // T1.8: KeepAliveService start
        com.kitsugi.animelist.core.player.KeepAliveService.start(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: ""
        val audioUrl = intent.getStringExtra(EXTRA_AUDIO_URL)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val headersBundle = intent.getBundleExtra(EXTRA_HEADERS)
        val requestHeaders: Map<String, String> = if (headersBundle != null) {
            buildMap { headersBundle.keySet().forEach { k -> headersBundle.getString(k)?.let { v -> put(k, v) } } }
        } else emptyMap()

        @Suppress("UNCHECKED_CAST")
        val initialSubtitles = (intent.getSerializableExtra(EXTRA_SUBTITLES) as? ArrayList<SubtitleInput>) ?: emptyList()

        @Suppress("UNCHECKED_CAST")
        val streamSources = (intent.getSerializableExtra(EXTRA_STREAM_SOURCES) as? ArrayList<StreamSource>) ?: emptyList()

        val currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, -1)
        val malId = intent.getIntExtra(EXTRA_MAL_ID, -1).takeIf { it != -1 }
        val aniListId = intent.getIntExtra(EXTRA_ANILIST_ID, -1).takeIf { it != -1 }
        val season = intent.getIntExtra(EXTRA_SEASON, 1)
        val episode = intent.getIntExtra(EXTRA_EPISODE, 1)
        val animeTitle = intent.getStringExtra(EXTRA_ANIME_TITLE) ?: ""
        val posterUrl = intent.getStringExtra(EXTRA_POSTER_URL)
        val titleEnglish = intent.getStringExtra(EXTRA_TITLE_ENGLISH)
        val titleRomaji = intent.getStringExtra(EXTRA_TITLE_ROMAJI)
        val titleNative = intent.getStringExtra(EXTRA_TITLE_NATIVE)
        val startYear = intent.getIntExtra(EXTRA_START_YEAR, -1).takeIf { it != -1 }
        val description = intent.getStringExtra(EXTRA_DESCRIPTION)

        @Suppress("UNCHECKED_CAST")
        val castList = (intent.getSerializableExtra(EXTRA_CAST) as? ArrayList<MetaCastMember>) ?: emptyList()

        setContent {
            KitsugiAnimeListTheme {
                TvPlayerScreen(
                    videoUrl = videoUrl,
                    audioUrl = audioUrl,
                    title = title,
                    requestHeaders = requestHeaders,
                    initialSubtitles = initialSubtitles,
                    streamSources = streamSources,
                    initialIndex = currentIndex,
                    malId = malId,
                    aniListId = aniListId,
                    season = season,
                    episode = episode,
                    animeTitle = animeTitle,
                    posterUrl = posterUrl,
                    titleEnglish = titleEnglish,
                    titleRomaji = titleRomaji,
                    titleNative = titleNative,
                    startYear = startYear,
                    description = description,
                    castList = castList,
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // T1.8: KeepAliveService stop
        com.kitsugi.animelist.core.player.KeepAliveService.stop(this)
    }
}
