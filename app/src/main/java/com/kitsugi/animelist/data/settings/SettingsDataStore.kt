package com.kitsugi.animelist.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.kitsugi.animelist.ui.screens.search.SearchHistoryItem
import com.kitsugi.animelist.ui.screens.search.SearchPlatform
import com.kitsugi.animelist.model.MediaType
import org.json.JSONArray
import org.json.JSONObject

private val Context.settingsDataStore by preferencesDataStore(
    name = "Kitsugi_settings"
)

class SettingsDataStore(
    private val context: Context
) {
    private object Keys {
        val SelectedThemeId = stringPreferencesKey("selected_theme_id")
        val ShowAdultContent = booleanPreferencesKey("show_adult_content")
        val SelectedListLayoutId = stringPreferencesKey("selected_list_layout_id")
        val ProfileName = stringPreferencesKey("profile_name")
        val ListTitle = stringPreferencesKey("list_title")
        val AniListUsername = stringPreferencesKey("anilist_username")
        val AniListProfileImageUri = stringPreferencesKey("anilist_profile_image_uri")
        val AniListBannerImageUri = stringPreferencesKey("anilist_banner_image_uri")
        val MalUsername = stringPreferencesKey("mal_username")
        val MalProfileImageUri = stringPreferencesKey("mal_profile_image_uri")
        val MalBannerImageUri = stringPreferencesKey("mal_banner_image_uri")
        val SimklUsername = stringPreferencesKey("simkl_username")
        val SimklProfileImageUri = stringPreferencesKey("simkl_profile_image_uri")
        val SimklBannerImageUri = stringPreferencesKey("simkl_banner_image_uri")
        val ProfileImageUri = stringPreferencesKey("profile_image_uri")
        val BannerImageUri = stringPreferencesKey("banner_image_uri")
        val SearchHistory = stringPreferencesKey("search_history_json")
        // AniHyou'dan uyarlama: başlık dili (Romaji / İngilizce / Japonca)
        val TitleLanguage = stringPreferencesKey("title_language")
        // AniHyou'dan uyarlama: puanlama formatı (POINT_10 / POINT_100 / POINT_5 / POINT_3)
        val ScoreFormat = stringPreferencesKey("score_format")
        // MoeList'ten uyarlama: puanları gizle
        val HideScores = booleanPreferencesKey("hide_scores")
        val ShowAnimeLogos = booleanPreferencesKey("show_anime_logos")
        val PlayerPreference = stringPreferencesKey("player_preference")
        val IsAutoplayEnabled = booleanPreferencesKey("is_autoplay_enabled")
        val SkipIntroDurationSec = intPreferencesKey("skip_intro_duration_sec")
        val DefaultSubtitleSize = intPreferencesKey("default_subtitle_size")
        val DefaultSubtitleColor = intPreferencesKey("default_subtitle_color")
        val SubtitleBold = booleanPreferencesKey("subtitle_bold")
        val SubtitleOutlineEnabled = booleanPreferencesKey("subtitle_outline_enabled")
        val DefaultAudioBoost = floatPreferencesKey("default_audio_boost")
        val DefaultAudioDelayMs = longPreferencesKey("default_audio_delay_ms")
        val MinBufferMs = intPreferencesKey("min_buffer_ms")
        val MaxBufferMs = intPreferencesKey("max_buffer_ms")
        val BufferForPlaybackMs = intPreferencesKey("buffer_for_playback_ms")
        val BufferForPlaybackAfterRebufferMs = intPreferencesKey("buffer_for_playback_after_rebuffer_ms")
        val BackBufferDurationMs = intPreferencesKey("back_buffer_duration_ms")
        val MangaReadingMode = stringPreferencesKey("manga_reading_mode")
        val MangaColorFilter = stringPreferencesKey("manga_color_filter")
        val MangaFitMode = stringPreferencesKey("manga_fit_mode")
        val MangaBrightness = floatPreferencesKey("manga_brightness")
        val DnsChoice = intPreferencesKey("dns_choice")
        // ─── Harici API Entegrasyonları ─────────────────────────────────────────
        // TMDB kullanıcı API anahtarı (boşsa dahili anahtar devreye girer)
        val TmdbEnabled = booleanPreferencesKey("tmdb_enabled")
        val TmdbUserApiKey = stringPreferencesKey("tmdb_user_api_key")
        val TmdbModernHomeEnabled = booleanPreferencesKey("tmdb_modern_home_enabled")
        val TmdbEnrichContinueWatching = booleanPreferencesKey("tmdb_enrich_continue_watching")
        val TmdbLanguage = stringPreferencesKey("tmdb_language")
        val TmdbUseArtwork = booleanPreferencesKey("tmdb_use_artwork")
        val TmdbUseBasicInfo = booleanPreferencesKey("tmdb_use_basic_info")
        val TmdbUseDetails = booleanPreferencesKey("tmdb_use_details")
        val TmdbUseReleaseDates = booleanPreferencesKey("tmdb_use_release_dates")
        val TmdbUseCredits = booleanPreferencesKey("tmdb_use_credits")
        val TmdbUseProductions = booleanPreferencesKey("tmdb_use_productions")
        val TmdbUseNetworks = booleanPreferencesKey("tmdb_use_networks")
        val TmdbUseEpisodes = booleanPreferencesKey("tmdb_use_episodes")
        val TmdbUseTrailers = booleanPreferencesKey("tmdb_use_trailers")
        val TmdbUseMoreLikeThis = booleanPreferencesKey("tmdb_use_more_like_this")
        val TmdbUseCollections = booleanPreferencesKey("tmdb_use_collections")
        // MDBList çoklu puan zenginleştirme
        val MdbListApiKey = stringPreferencesKey("mdblist_api_key")
        val MdbListEnabled = booleanPreferencesKey("mdblist_enabled")
        val MdbListShowImdb = booleanPreferencesKey("mdblist_show_imdb")
        val MdbListShowTomatoes = booleanPreferencesKey("mdblist_show_tomatoes")
        val MdbListShowAudience = booleanPreferencesKey("mdblist_show_audience")
        val MdbListShowMetacritic = booleanPreferencesKey("mdblist_show_metacritic")
        val MdbListShowLetterboxd = booleanPreferencesKey("mdblist_show_letterboxd")
        val MdbListShowTmdb = booleanPreferencesKey("mdblist_show_tmdb")
        val MdbListShowTrakt = booleanPreferencesKey("mdblist_show_trakt")
        // AniSkip intro/outro atlama
        val AniSkipEnabled = booleanPreferencesKey("aniskip_enabled")
        val AniSkipAutoSkip = booleanPreferencesKey("aniskip_auto_skip")
        val AnimeSkipClientId = stringPreferencesKey("animeskip_client_id")
        // Açıklama Otomatik Çevirisi
        val AutoTranslateEnabled = booleanPreferencesKey("auto_translate_enabled")
        // TV ana sayfa yerleşim düzeni (classic / modern / grid)
        val SelectedHomeLayoutId = stringPreferencesKey("selected_home_layout_id")
        val FrameRateMatchingMode = stringPreferencesKey("frame_rate_matching_mode")
        val ResolutionMatchingEnabled = booleanPreferencesKey("resolution_matching_enabled")
        val DecoderPriority = intPreferencesKey("decoder_priority")
        val Dv7HandlingMode = stringPreferencesKey("dv7_handling_mode")
        val StripHdr10PlusSei = booleanPreferencesKey("strip_hdr10_plus_sei")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val AmoledBlack = booleanPreferencesKey("amoled_black")
        val CustomAccentColor = intPreferencesKey("custom_accent_color")
        val DefaultTab = stringPreferencesKey("default_tab")
        val LastUsedTab = stringPreferencesKey("last_used_tab")
        val AppLanguage = stringPreferencesKey("app_language")
        val FixedNavBar = booleanPreferencesKey("fixed_nav_bar")
        val AspectMode = stringPreferencesKey("aspect_mode")
        val GainBoostDb = floatPreferencesKey("gain_boost_db")
        val SubtitleDelayMs = longPreferencesKey("subtitle_delay_ms")
        val PreferredSubtitleLanguages = stringPreferencesKey("preferred_subtitle_languages")
        val AddonSubtitleStartupMode = stringPreferencesKey("addon_subtitle_startup_mode")
        // ─── Gesture (T2.1 + T2.7 + TASK_050) ──────────────────────────────────
        val GestureVolumeEnabled = booleanPreferencesKey("gesture_volume_enabled")
        val GestureBrightnessEnabled = booleanPreferencesKey("gesture_brightness_enabled")
        val GestureZoomEnabled = booleanPreferencesKey("gesture_zoom_enabled")
        /** TASK_050 — Dikey swipe hassasiyeti (0.5…2.0) */
        val GestureScrollSensitivity = floatPreferencesKey("gesture_scroll_sensitivity")
        val DoubleTapSeekSeconds = intPreferencesKey("double_tap_seek_seconds")
        val HoldSpeedMultiplier = floatPreferencesKey("hold_speed_multiplier")
        // ─── PIP (T2.3) ──────────────────────────────────────────────────────
        val PipEnabled = booleanPreferencesKey("pip_enabled")
        // ─── Audio Route Delay (T1.3) ─────────────────────────────────────────
        val AudioDelayPerRouteJson = stringPreferencesKey("audio_delay_per_route_json")
        // ─── Quality Profile (T2.4) ────────────────────────────────────────
        val QualityProfileJson = stringPreferencesKey("quality_profile_json")
        // ─── Airing Notifications (T3.3) ─────────────────────────────────────
        val AiringNotificationsEnabled = booleanPreferencesKey("airing_notifications_enabled")
        val SearchHistoryEnabled = booleanPreferencesKey("search_history_enabled")
        // ─── T1.7 – StillWatching + PostPlayMode + AutoplaySessionRules ──────
        val StillWatchingEnabled = booleanPreferencesKey("still_watching_enabled")
        val StillWatchingThresholdMinutes = intPreferencesKey("still_watching_threshold_minutes")
        val PostPlayMode = stringPreferencesKey("post_play_mode")
        val AutoplaySessionLimit = intPreferencesKey("autoplay_session_limit")
        // ─── T1.9 – Paralel Aralık İndirme ─────────────────────
        val ParallelRangeEnabled = booleanPreferencesKey("parallel_range_enabled")
        // ─── T2.2 – Önizleme Seekbar ───────────────────────────
        val PreviewSeekbarEnabled = booleanPreferencesKey("preview_seekbar_enabled")
        // ─── T2.5 – Harici Oynatıcı Tercihi ──────────────────────────────────
        val PreferredExternalPlayerPackage = stringPreferencesKey("preferred_external_player_package")
        //  T2.6  Oynatc Balk / Medya Bilgisi Grnrl 
        val ShowPlayerTitle = booleanPreferencesKey("show_player_title")
        val ShowPlayerResolution = booleanPreferencesKey("show_player_resolution")
        val ShowMediaInfo = booleanPreferencesKey("show_media_info")
        val TitleLimitType = stringPreferencesKey("title_limit_type")
        val SplashAnimationEnabled = booleanPreferencesKey("splash_animation_enabled")
        val SplashSoundEnabled = booleanPreferencesKey("splash_sound_enabled")
        val LiveHelperEnabled = booleanPreferencesKey("live_helper_enabled")
        val EnableAssExtractor = booleanPreferencesKey("enable_ass_extractor")
        val AutoUpdateCheckEnabled = booleanPreferencesKey("auto_update_check_enabled")
        val CustomImageDownloadUri = stringPreferencesKey("custom_image_download_uri")
    }

    val settingsFlow: Flow<AppSettings> = kotlinx.coroutines.flow.flow {
        context.settingsDataStore.data.collect { preferences ->
            emit(
                AppSettings(
                    selectedThemeId = preferences[Keys.SelectedThemeId] ?: "mint",
                    showAdultContent = preferences[Keys.ShowAdultContent] ?: false,
                    selectedListLayoutId = preferences[Keys.SelectedListLayoutId] ?: "comfortable",
                    profileName = preferences[Keys.ProfileName] ?: "Profilim",
                    listTitle = preferences[Keys.ListTitle] ?: "Anime & Manga Listem",
                    anilistUsername = preferences[Keys.AniListUsername] ?: "",
                    anilistProfileImageUri = preferences[Keys.AniListProfileImageUri] ?: "",
                    anilistBannerImageUri = preferences[Keys.AniListBannerImageUri] ?: "",
                    malUsername = preferences[Keys.MalUsername] ?: "",
                    malProfileImageUri = preferences[Keys.MalProfileImageUri] ?: "",
                    malBannerImageUri = preferences[Keys.MalBannerImageUri] ?: "",
                    simklUsername = preferences[Keys.SimklUsername] ?: "",
                    simklProfileImageUri = preferences[Keys.SimklProfileImageUri] ?: "",
                    simklBannerImageUri = preferences[Keys.SimklBannerImageUri] ?: "",
                    profileImageUri = preferences[Keys.ProfileImageUri] ?: "",
                    bannerImageUri = preferences[Keys.BannerImageUri] ?: "",
                    titleLanguage = preferences[Keys.TitleLanguage] ?: "ROMAJI",
                    scoreFormat = preferences[Keys.ScoreFormat] ?: "POINT_10",
                    hideScores = preferences[Keys.HideScores] ?: false,
                    showAnimeLogos = preferences[Keys.ShowAnimeLogos] ?: false,
                    playerPreference = preferences[Keys.PlayerPreference] ?: "INTERNAL",
                    isAutoplayEnabled = preferences[Keys.IsAutoplayEnabled] ?: true,
                    skipIntroDurationSec = preferences[Keys.SkipIntroDurationSec] ?: 5,
                    defaultSubtitleSize = preferences[Keys.DefaultSubtitleSize] ?: 16,
                    defaultSubtitleColor = preferences[Keys.DefaultSubtitleColor] ?: 0xFFFFFFFF.toInt(),
                    subtitleBold = preferences[Keys.SubtitleBold] ?: false,
                    subtitleOutlineEnabled = preferences[Keys.SubtitleOutlineEnabled] ?: true,
                    defaultAudioBoost = preferences[Keys.DefaultAudioBoost] ?: 0.0f,
                    defaultAudioDelayMs = preferences[Keys.DefaultAudioDelayMs] ?: 0L,
                    minBufferMs = preferences[Keys.MinBufferMs] ?: 15_000,
                    maxBufferMs = preferences[Keys.MaxBufferMs] ?: 50_000,
                    bufferForPlaybackMs = preferences[Keys.BufferForPlaybackMs] ?: 2_500,
                    bufferForPlaybackAfterRebufferMs = preferences[Keys.BufferForPlaybackAfterRebufferMs] ?: 3_000,
                    backBufferDurationMs = preferences[Keys.BackBufferDurationMs] ?: 0,
                    mangaReadingMode = preferences[Keys.MangaReadingMode] ?: "RightToLeft",
                    mangaColorFilter = preferences[Keys.MangaColorFilter] ?: "Normal",
                    mangaFitMode = preferences[Keys.MangaFitMode] ?: "FitScreen",
                    mangaBrightness = preferences[Keys.MangaBrightness] ?: 1.0f,
                    dnsChoice = preferences[Keys.DnsChoice] ?: 0,
                    // ─── Harici API Entegrasyonları
                    tmdbEnabled = preferences[Keys.TmdbEnabled] ?: true,
                    tmdbUserApiKey = preferences[Keys.TmdbUserApiKey] ?: "",
                    tmdbModernHomeEnabled = preferences[Keys.TmdbModernHomeEnabled] ?: true,
                    tmdbEnrichContinueWatching = preferences[Keys.TmdbEnrichContinueWatching] ?: true,
                    tmdbLanguage = preferences[Keys.TmdbLanguage] ?: "en",
                    tmdbUseArtwork = preferences[Keys.TmdbUseArtwork] ?: true,
                    tmdbUseBasicInfo = preferences[Keys.TmdbUseBasicInfo] ?: true,
                    tmdbUseDetails = preferences[Keys.TmdbUseDetails] ?: true,
                    tmdbUseReleaseDates = preferences[Keys.TmdbUseReleaseDates] ?: true,
                    tmdbUseCredits = preferences[Keys.TmdbUseCredits] ?: true,
                    tmdbUseProductions = preferences[Keys.TmdbUseProductions] ?: true,
                    tmdbUseNetworks = preferences[Keys.TmdbUseNetworks] ?: true,
                    tmdbUseEpisodes = preferences[Keys.TmdbUseEpisodes] ?: true,
                    tmdbUseTrailers = preferences[Keys.TmdbUseTrailers] ?: true,
                    tmdbUseMoreLikeThis = preferences[Keys.TmdbUseMoreLikeThis] ?: true,
                    tmdbUseCollections = preferences[Keys.TmdbUseCollections] ?: true,
                    mdbListApiKey = preferences[Keys.MdbListApiKey] ?: "",
                    mdbListEnabled = preferences[Keys.MdbListEnabled] ?: false,
                    mdbListShowImdb = preferences[Keys.MdbListShowImdb] ?: true,
                    mdbListShowTomatoes = preferences[Keys.MdbListShowTomatoes] ?: true,
                    mdbListShowAudience = preferences[Keys.MdbListShowAudience] ?: false,
                    mdbListShowMetacritic = preferences[Keys.MdbListShowMetacritic] ?: true,
                    mdbListShowLetterboxd = preferences[Keys.MdbListShowLetterboxd] ?: false,
                    mdbListShowTmdb = preferences[Keys.MdbListShowTmdb] ?: false,
                    mdbListShowTrakt = preferences[Keys.MdbListShowTrakt] ?: false,
                    aniSkipEnabled = preferences[Keys.AniSkipEnabled] ?: true,
                    aniSkipAutoSkip = preferences[Keys.AniSkipAutoSkip] ?: false,
                    animeSkipClientId = preferences[Keys.AnimeSkipClientId] ?: "",
                    autoTranslateEnabled = preferences[Keys.AutoTranslateEnabled] ?: false,
                    selectedHomeLayoutId = preferences[Keys.SelectedHomeLayoutId] ?: "classic",
                    frameRateMatchingMode = runCatching {
                        com.kitsugi.animelist.data.settings.FrameRateMatchingMode.valueOf(
                            preferences[Keys.FrameRateMatchingMode] ?: "OFF"
                        )
                    }.getOrDefault(com.kitsugi.animelist.data.settings.FrameRateMatchingMode.OFF),
                    resolutionMatchingEnabled = preferences[Keys.ResolutionMatchingEnabled] ?: false,
                    decoderPriority = preferences[Keys.DecoderPriority] ?: 0,
                    dv7HandlingMode = runCatching {
                        com.kitsugi.animelist.data.settings.Dv7HandlingMode.valueOf(
                            preferences[Keys.Dv7HandlingMode] ?: "AUTO"
                        )
                    }.getOrDefault(com.kitsugi.animelist.data.settings.Dv7HandlingMode.AUTO),
                    stripHdr10PlusSei = preferences[Keys.StripHdr10PlusSei] ?: false,
                    themeMode = preferences[Keys.ThemeMode] ?: "FOLLOW_SYSTEM",
                    amoledBlack = preferences[Keys.AmoledBlack] ?: false,
                    customAccentColor = preferences[Keys.CustomAccentColor] ?: 0,
                    defaultTab = preferences[Keys.DefaultTab] ?: "LAST_USED",
                    lastUsedTab = preferences[Keys.LastUsedTab] ?: "Explore",
                    appLanguage = preferences[Keys.AppLanguage] ?: "system",
                    fixedNavBar = preferences[Keys.FixedNavBar] ?: false,
                    aspectMode = preferences[Keys.AspectMode] ?: "ORIGINAL",
                    gainBoostDb = preferences[Keys.GainBoostDb] ?: 0f,
                    subtitleDelayMs = preferences[Keys.SubtitleDelayMs] ?: 0L,
                    preferredSubtitleLanguages = preferences[Keys.PreferredSubtitleLanguages] ?: "tr",
                    addonSubtitleStartupMode = preferences[Keys.AddonSubtitleStartupMode] ?: "PREFERRED_ONLY",
                    gestureVolumeEnabled = preferences[Keys.GestureVolumeEnabled] ?: true,
                    gestureBrightnessEnabled = preferences[Keys.GestureBrightnessEnabled] ?: true,
                    gestureZoomEnabled = preferences[Keys.GestureZoomEnabled] ?: true,
                    gestureScrollSensitivity = preferences[Keys.GestureScrollSensitivity] ?: 1.0f,
                    doubleTapSeekSeconds = preferences[Keys.DoubleTapSeekSeconds] ?: 10,
                    holdSpeedMultiplier = preferences[Keys.HoldSpeedMultiplier] ?: 2.0f,
                    pipEnabled = preferences[Keys.PipEnabled] ?: true,
                    audioDelayPerRouteJson = preferences[Keys.AudioDelayPerRouteJson] ?: "{}",
                    qualityProfileJson = preferences[Keys.QualityProfileJson] ?: "",
                    airingNotificationsEnabled = preferences[Keys.AiringNotificationsEnabled] ?: false,
                    searchHistoryEnabled = preferences[Keys.SearchHistoryEnabled] ?: true,
                    // ─── T1.7 ─────────────────────────────────────────────────
                    stillWatchingEnabled = preferences[Keys.StillWatchingEnabled] ?: true,
                    stillWatchingThresholdMinutes = preferences[Keys.StillWatchingThresholdMinutes] ?: 90,
                    postPlayMode = preferences[Keys.PostPlayMode] ?: "AUTO_PLAY_NEXT",
                    autoplaySessionLimit = preferences[Keys.AutoplaySessionLimit] ?: 0,
                    // ─── T1.9 ─────────────────────────────────────────────────
                    parallelRangeEnabled = preferences[Keys.ParallelRangeEnabled] ?: false,
                    // ─── T2.2 ─────────────────────────────────────────────────
                    previewSeekbarEnabled = preferences[Keys.PreviewSeekbarEnabled] ?: true,
                    // ─── T2.5 ─────────────────────────────────────────────────
                    preferredExternalPlayerPackage = preferences[Keys.PreferredExternalPlayerPackage] ?: "",
                    //  T2.6 
                    showPlayerTitle = preferences[Keys.ShowPlayerTitle] ?: true,
                    showPlayerResolution = preferences[Keys.ShowPlayerResolution] ?: true,
                    showMediaInfo = preferences[Keys.ShowMediaInfo] ?: true,
                    titleLimitType = preferences[Keys.TitleLimitType] ?: "NONE",
                    splashAnimationEnabled = preferences[Keys.SplashAnimationEnabled] ?: true,
                    splashSoundEnabled = preferences[Keys.SplashSoundEnabled] ?: true,
                    liveHelperEnabled = preferences[Keys.LiveHelperEnabled] ?: false,
                    enableAssExtractor = preferences[Keys.EnableAssExtractor] ?: false,
                    autoUpdateCheckEnabled = preferences[Keys.AutoUpdateCheckEnabled] ?: true,
                    customImageDownloadUri = preferences[Keys.CustomImageDownloadUri] ?: ""
                )
            )
        }
    }

    suspend fun setCustomImageDownloadUri(uri: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.CustomImageDownloadUri] = uri
        }
    }

    suspend fun setAutoUpdateCheckEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.AutoUpdateCheckEnabled] = enabled
        }
    }

    suspend fun setSearchHistoryEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.SearchHistoryEnabled] = enabled
        }
    }

    suspend fun setSelectedThemeId(themeId: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.SelectedThemeId] = themeId
        }
    }

    suspend fun setShowAdultContent(show: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.ShowAdultContent] = show
        }
    }

    suspend fun setSelectedListLayoutId(layoutId: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.SelectedListLayoutId] = layoutId
        }
    }

    suspend fun setProfileInfo(
        profileName: String,
        listTitle: String,
        anilistUsername: String
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.ProfileName] = profileName
            preferences[Keys.ListTitle] = listTitle
            preferences[Keys.AniListUsername] = anilistUsername
        }
    }

    suspend fun setAniListProfileInfo(
        username: String,
        profileImageUri: String,
        bannerImageUri: String
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.AniListUsername] = username
            preferences[Keys.AniListProfileImageUri] = profileImageUri
            preferences[Keys.AniListBannerImageUri] = bannerImageUri
        }
    }

    suspend fun setMalProfileInfo(
        username: String,
        profileImageUri: String,
        bannerImageUri: String
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.MalUsername] = username
            preferences[Keys.MalProfileImageUri] = profileImageUri
            preferences[Keys.MalBannerImageUri] = bannerImageUri
        }
    }

    suspend fun setSimklProfileInfo(
        username: String,
        profileImageUri: String,
        bannerImageUri: String
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.SimklUsername] = username
            preferences[Keys.SimklProfileImageUri] = profileImageUri
            preferences[Keys.SimklBannerImageUri] = bannerImageUri
        }
    }

    suspend fun clearAniListProfileInfo() {
        context.settingsDataStore.edit { preferences ->
            preferences.remove(Keys.AniListUsername)
            preferences.remove(Keys.AniListProfileImageUri)
            preferences.remove(Keys.AniListBannerImageUri)
        }
    }

    suspend fun clearMalProfileInfo() {
        context.settingsDataStore.edit { preferences ->
            preferences.remove(Keys.MalUsername)
            preferences.remove(Keys.MalProfileImageUri)
            preferences.remove(Keys.MalBannerImageUri)
        }
    }

    suspend fun clearSimklProfileInfo() {
        context.settingsDataStore.edit { preferences ->
            preferences.remove(Keys.SimklUsername)
            preferences.remove(Keys.SimklProfileImageUri)
            preferences.remove(Keys.SimklBannerImageUri)
        }
    }

    suspend fun setProfileImageUri(uri: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.ProfileImageUri] = uri
        }
    }

    suspend fun setBannerImageUri(uri: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.BannerImageUri] = uri
        }
    }

    suspend fun clearProfileImageUri() {
        context.settingsDataStore.edit { preferences ->
            preferences.remove(Keys.ProfileImageUri)
        }
    }


    suspend fun clearBannerImageUri() {
        context.settingsDataStore.edit { preferences ->
            preferences.remove(Keys.BannerImageUri)
        }
    }

    // AniHyou'dan uyarlama: Başlık dili tercihi (Romaji / İngilizce / Japonca)
    suspend fun setTitleLanguage(titleLanguage: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.TitleLanguage] = titleLanguage
        }
    }

    // AniHyou'dan uyarlama: Puanlama formatı (POINT_10 / POINT_100 / POINT_5 / POINT_3)
    suspend fun setScoreFormat(scoreFormat: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.ScoreFormat] = scoreFormat
        }
    }

    // MoeList'ten uyarlama: Puanları gizle
    suspend fun setHideScores(hide: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.HideScores] = hide
        }
    }

    suspend fun setShowAnimeLogos(show: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.ShowAnimeLogos] = show
        }
    }

    suspend fun setDnsChoice(choice: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.DnsChoice] = choice
        }
    }

    val searchHistoryFlow: Flow<List<SearchHistoryItem>> = kotlinx.coroutines.flow.flow {
        context.settingsDataStore.data.collect { preferences ->
            val jsonStr = preferences[Keys.SearchHistory] ?: ""
            emit(deserializeHistory(jsonStr))
        }
    }

    suspend fun saveSearchHistory(history: List<SearchHistoryItem>) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.SearchHistory] = serializeHistory(history)
        }
    }

    private fun serializeHistory(history: List<SearchHistoryItem>): String {
        val array = JSONArray()
        history.forEach { item ->
            val obj = JSONObject()
                .put("query", item.query)
                .put("platform", item.platform.name)
                .put("mediaType", item.mediaType.name)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeHistory(jsonStr: String): List<SearchHistoryItem> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<SearchHistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val query = obj.optString("query")
                val platformStr = obj.optString("platform")
                val mediaTypeStr = obj.optString("mediaType")

                val platform = runCatching { SearchPlatform.valueOf(platformStr) }.getOrDefault(SearchPlatform.All)
                val mediaType = runCatching { MediaType.valueOf(mediaTypeStr) }.getOrDefault(MediaType.Anime)

                if (query.isNotBlank()) {
                    list.add(SearchHistoryItem(query, platform, mediaType))
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun setPlayerPreference(pref: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.PlayerPreference] = pref
        }
    }

    suspend fun setAutoplayEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.IsAutoplayEnabled] = enabled
        }
    }

    suspend fun setSkipIntroDurationSec(sec: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.SkipIntroDurationSec] = sec
        }
    }

    suspend fun setDefaultSubtitleSize(size: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.DefaultSubtitleSize] = size
        }
    }

    suspend fun setDefaultSubtitleColor(color: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.DefaultSubtitleColor] = color
        }
    }

    suspend fun setSubtitleBold(bold: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.SubtitleBold] = bold
        }
    }

    suspend fun setSubtitleOutlineEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.SubtitleOutlineEnabled] = enabled
        }
    }

    suspend fun setDefaultAudioBoost(boost: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.DefaultAudioBoost] = boost
        }
    }

    suspend fun setDefaultAudioDelayMs(delayMs: Long) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.DefaultAudioDelayMs] = delayMs
        }
    }

    suspend fun setBufferSettings(min: Int, max: Int, playback: Int, rebuffer: Int, back: Int) {
        val validatedMax = max.coerceAtLeast(1000)
        val validatedMin = min.coerceIn(0, validatedMax)
        val validatedPlayback = playback.coerceIn(0, validatedMax)
        val validatedRebuffer = rebuffer.coerceIn(0, validatedMax)
        val validatedBack = back.coerceAtLeast(0)

        context.settingsDataStore.edit { preferences ->
            preferences[Keys.MinBufferMs] = validatedMin
            preferences[Keys.MaxBufferMs] = validatedMax
            preferences[Keys.BufferForPlaybackMs] = validatedPlayback
            preferences[Keys.BufferForPlaybackAfterRebufferMs] = validatedRebuffer
            preferences[Keys.BackBufferDurationMs] = validatedBack
        }
    }

    suspend fun setMangaReadingMode(mode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.MangaReadingMode] = mode
        }
    }

    suspend fun setMangaColorFilter(filter: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.MangaColorFilter] = filter
        }
    }

    suspend fun setMangaFitMode(fitMode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.MangaFitMode] = fitMode
        }
    }

    suspend fun setMangaBrightness(brightness: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.MangaBrightness] = brightness
        }
    }

    // ─── Harici API Entegrasyonları set fonksiyonları ─────────────────────────────────────

    suspend fun setTmdbUserApiKey(key: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.TmdbUserApiKey] = key
        }
    }

    suspend fun setMdbListApiKey(key: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.MdbListApiKey] = key
        }
    }

    suspend fun setMdbListEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.MdbListEnabled] = enabled
        }
    }

    suspend fun setMdbListShowImdb(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.MdbListShowImdb] = show }
    }

    suspend fun setMdbListShowTomatoes(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.MdbListShowTomatoes] = show }
    }

    suspend fun setMdbListShowAudience(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.MdbListShowAudience] = show }
    }

    suspend fun setMdbListShowMetacritic(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.MdbListShowMetacritic] = show }
    }

    suspend fun setMdbListShowLetterboxd(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.MdbListShowLetterboxd] = show }
    }

    suspend fun setMdbListShowTmdb(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.MdbListShowTmdb] = show }
    }

    suspend fun setMdbListShowTrakt(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.MdbListShowTrakt] = show }
    }

    suspend fun setAniSkipEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.AniSkipEnabled] = enabled
        }
    }

    suspend fun setAniSkipAutoSkip(autoSkip: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.AniSkipAutoSkip] = autoSkip
        }
    }

    suspend fun setTmdbEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.TmdbEnabled] = enabled }
    }

    suspend fun setTmdbModernHomeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.TmdbModernHomeEnabled] = enabled }
    }

    suspend fun setTmdbEnrichContinueWatching(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.TmdbEnrichContinueWatching] = enabled }
    }

    suspend fun setTmdbLanguage(lang: String) {
        context.settingsDataStore.edit { it[Keys.TmdbLanguage] = lang }
    }

    suspend fun setTmdbUseArtwork(use: Boolean) {
        context.settingsDataStore.edit { it[Keys.TmdbUseArtwork] = use }
    }

    suspend fun setTmdbUseBasicInfo(use: Boolean) {
        context.settingsDataStore.edit { it[Keys.TmdbUseBasicInfo] = use }
    }

    suspend fun setTmdbUseDetails(use: Boolean) {
        context.settingsDataStore.edit { it[Keys.TmdbUseDetails] = use }
    }

    suspend fun setTmdbUseReleaseDates(use: Boolean) {
        context.settingsDataStore.edit { it[Keys.TmdbUseReleaseDates] = use }
    }

    suspend fun setTmdbUseCredits(use: Boolean) {
        context.settingsDataStore.edit { it[Keys.TmdbUseCredits] = use }
    }

    suspend fun setTmdbUseProductions(use: Boolean) {
        context.settingsDataStore.edit { it[Keys.TmdbUseProductions] = use }
    }

    suspend fun setTmdbUseNetworks(use: Boolean) {
        context.settingsDataStore.edit { it[Keys.TmdbUseNetworks] = use }
    }

    suspend fun setTmdbUseEpisodes(use: Boolean) {
        context.settingsDataStore.edit { it[Keys.TmdbUseEpisodes] = use }
    }

    suspend fun setTmdbUseTrailers(use: Boolean) {
        context.settingsDataStore.edit { it[Keys.TmdbUseTrailers] = use }
    }

    suspend fun setTmdbUseMoreLikeThis(use: Boolean) {
        context.settingsDataStore.edit { it[Keys.TmdbUseMoreLikeThis] = use }
    }

    suspend fun setTmdbUseCollections(use: Boolean) {
        context.settingsDataStore.edit { it[Keys.TmdbUseCollections] = use }
    }

    suspend fun setAnimeSkipClientId(clientId: String) {
        context.settingsDataStore.edit { it[Keys.AnimeSkipClientId] = clientId }
    }

    suspend fun setAutoTranslateEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AutoTranslateEnabled] = enabled }
    }

    suspend fun setSelectedHomeLayoutId(layoutId: String) {
        context.settingsDataStore.edit { it[Keys.SelectedHomeLayoutId] = layoutId }
    }

    suspend fun setFrameRateMatchingMode(mode: FrameRateMatchingMode) {
        context.settingsDataStore.edit { it[Keys.FrameRateMatchingMode] = mode.name }
    }

    suspend fun setResolutionMatchingEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ResolutionMatchingEnabled] = enabled }
    }

    suspend fun setDecoderPriority(priority: Int) {
        context.settingsDataStore.edit { it[Keys.DecoderPriority] = priority }
    }

    suspend fun setDv7HandlingMode(mode: Dv7HandlingMode) {
        context.settingsDataStore.edit { it[Keys.Dv7HandlingMode] = mode.name }
    }

    suspend fun setStripHdr10PlusSei(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.StripHdr10PlusSei] = enabled }
    }

    suspend fun setThemeMode(mode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.ThemeMode] = mode
        }
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.AmoledBlack] = enabled
        }
    }

    suspend fun setCustomAccentColor(color: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.CustomAccentColor] = color
        }
    }

    suspend fun setDefaultTab(tab: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.DefaultTab] = tab
        }
    }

    suspend fun setLastUsedTab(tab: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.LastUsedTab] = tab
        }
    }

    suspend fun setAppLanguage(language: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.AppLanguage] = language
        }
    }

    suspend fun setFixedNavBar(fixed: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.FixedNavBar] = fixed
        }
    }

    suspend fun setAspectMode(mode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.AspectMode] = mode
        }
    }

    suspend fun setGainBoostDb(gain: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.GainBoostDb] = gain
        }
    }

    suspend fun setSubtitleDelayMs(delayMs: Long) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.SubtitleDelayMs] = delayMs
        }
    }

    suspend fun setPreferredSubtitleLanguages(langs: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.PreferredSubtitleLanguages] = langs
        }
    }

    suspend fun setAddonSubtitleStartupMode(mode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.AddonSubtitleStartupMode] = mode
        }
    }

    suspend fun setGestureVolumeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.GestureVolumeEnabled] = enabled }
    }

    suspend fun setGestureBrightnessEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.GestureBrightnessEnabled] = enabled }
    }

    suspend fun setGestureZoomEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.GestureZoomEnabled] = enabled }
    }

    /** TASK_050 — Dikey swipe hassasiyetini kaydet (0.5…2.0) */
    suspend fun setGestureScrollSensitivity(sensitivity: Float) {
        context.settingsDataStore.edit { it[Keys.GestureScrollSensitivity] = sensitivity.coerceIn(0.25f, 3.0f) }
    }

    suspend fun setDoubleTapSeekSeconds(seconds: Int) {
        context.settingsDataStore.edit { it[Keys.DoubleTapSeekSeconds] = seconds }
    }

    suspend fun setHoldSpeedMultiplier(multiplier: Float) {
        context.settingsDataStore.edit { it[Keys.HoldSpeedMultiplier] = multiplier }
    }

    suspend fun setPipEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.PipEnabled] = enabled }
    }

    suspend fun setAudioDelayPerRouteJson(json: String) {
        context.settingsDataStore.edit { it[Keys.AudioDelayPerRouteJson] = json }
    }

    // ─── Quality Profile (T2.4) ───────────────────────────────────────────
    suspend fun setQualityProfileJson(json: String) {
        context.settingsDataStore.edit { it[Keys.QualityProfileJson] = json }
    }

    // ─── Airing Notifications (T3.3) ─────────────────────────────────────
    suspend fun setAiringNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AiringNotificationsEnabled] = enabled }
    }

    // ─── T1.7 – StillWatching + PostPlayMode + AutoplaySessionRules ──────────
    suspend fun setStillWatchingEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.StillWatchingEnabled] = enabled }
    }

    suspend fun setStillWatchingThresholdMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[Keys.StillWatchingThresholdMinutes] = minutes }
    }

    suspend fun setPostPlayMode(mode: String) {
        context.settingsDataStore.edit { it[Keys.PostPlayMode] = mode }
    }

    suspend fun setAutoplaySessionLimit(limit: Int) {
        context.settingsDataStore.edit { it[Keys.AutoplaySessionLimit] = limit }
    }

    // ─── T1.9 – Paralel Aralık İndirme ───────────────────────────────
    suspend fun setParallelRangeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ParallelRangeEnabled] = enabled }
    }

    // ─── T2.2 – Önizleme Seekbar ───────────────────────────────
    suspend fun setPreviewSeekbarEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.PreviewSeekbarEnabled] = enabled }
    }

    // ─── T2.5 – Harici Oynatıcı Tercihi ────────────────────────
    suspend fun setPreferredExternalPlayerPackage(packageName: String) {
        context.settingsDataStore.edit { it[Keys.PreferredExternalPlayerPackage] = packageName }
    }

    // --- T2.6 - Player Title / Media Info Visibility ---
    suspend fun setShowPlayerTitle(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.ShowPlayerTitle] = show }
    }

    suspend fun setShowPlayerResolution(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.ShowPlayerResolution] = show }
    }

    suspend fun setShowMediaInfo(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.ShowMediaInfo] = show }
    }

    suspend fun setTitleLimitType(limitType: String) {
        context.settingsDataStore.edit { it[Keys.TitleLimitType] = limitType }
    }

    suspend fun setSplashAnimationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SplashAnimationEnabled] = enabled }
    }

    suspend fun setSplashSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SplashSoundEnabled] = enabled }
    }

    suspend fun setLiveHelperEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.LiveHelperEnabled] = enabled }
    }

    suspend fun setEnableAssExtractor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.EnableAssExtractor] = enabled }
    }
}


