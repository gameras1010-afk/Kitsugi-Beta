# KitsugiAnimeList – Settings Keys Reference

> **Son güncelleme:** 2026-07-15  
> Bu tablo, `AppSettings` veri sınıfındaki tüm ayarları,  
> ilgili `SettingsDataStore` anahtarlarını ve hangi UI / engine  
> bileşenlerinde kullanıldığını gösterir.

---

## Okuma / Yazma Rehberi

```kotlin
// Okuma (her yerde, Flow ile)
settingsDataStore.settingsFlow.collect { settings ->
    val v = settings.someField
}

// Yazma (ViewModel'de, suspend fun içinde)
settingsDataStore.setSomeField(newValue)
```

---

## Ayar Tablosu

### 🎨 Görünüm ve Tema

| Alan | DataStore Key | Tür | Default | UI | Engine |
|---|---|---|---|---|---|
| `selectedThemeId` | `selected_theme_id` | String | `"mint"` | ThemeSettingsPage | – |
| `themeMode` | `theme_mode` | String | `"FOLLOW_SYSTEM"` | ThemeSettingsPage | – |
| `amoledBlack` | `amoled_black` | Boolean | `false` | ThemeSettingsPage | – |
| `customAccentColor` | `custom_accent_color` | Int | `0` | ThemeSettingsPage | – |
| `themeStyle` | `theme_style` | String (enum) | `SYSTEM` | ThemeSettingsPage | – |
| `selectedListLayoutId` | `selected_list_layout_id` | String | `"comfortable"` | ListSettingsPage | – |
| `listStyle` | `list_style` | String (enum) | `COMFORTABLE` | ListSettingsPage | – |
| `itemsPerRow` | `items_per_row` | String (enum) | `THREE` | ListSettingsPage | – |
| `tabletMode` | `tablet_mode` | Boolean | `false` | ListSettingsPage | – |
| `showAnimeLogos` | `show_anime_logos` | Boolean | `false` | GeneralSettingsPage | – |

### 👤 Profil ve Hesap

| Alan | DataStore Key | Tür | Default | UI |
|---|---|---|---|---|
| `profileName` | `profile_name` | String | `"Profilim"` | ProfilePage |
| `listTitle` | `list_title` | String | `"Anime & Manga Listem"` | ProfilePage |
| `anilistUsername` | `anilist_username` | String | `""` | AniList login |
| `anilistProfileImageUri` | `anilist_profile_image_uri` | String | `""` | ProfilePage |
| `malUsername` | `mal_username` | String | `""` | MAL login |
| `simklUsername` | `simkl_username` | String | `""` | Simkl login |
| `titleLanguage` | `title_language` | String | `"ROMAJI"` | GeneralSettingsPage |
| `scoreFormat` | `score_format` | String | `"POINT_10"` | GeneralSettingsPage |
| `hideScores` | `hide_scores` | Boolean | `false` | GeneralSettingsPage |
| `showAdultContent` | `show_adult_content` | Boolean | `false` | GeneralSettingsPage |

### 🔍 Arama ve Navigasyon

| Alan | DataStore Key | Tür | Default | UI |
|---|---|---|---|---|
| `searchHistoryEnabled` | `search_history_enabled` | Boolean | `true` | SearchPage |
| `defaultTab` | `default_tab` | String | `"LAST_USED"` | NavSettingsPage |
| `lastUsedTab` | `last_used_tab` | String | `"Explore"` | Otomatik |
| `startTab` | `start_tab` | String | `"Explore"` | NavSettingsPage |
| `fixedNavBar` | `fixed_nav_bar` | Boolean | `false` | NavSettingsPage |
| `selectedHomeLayoutId` | `selected_home_layout_id` | String | `"classic"` | HomeSettingsPage |
| `appLanguage` | `app_language` | String | `"system"` | GeneralSettingsPage |

### 🎬 Oynatıcı – Genel

| Alan | DataStore Key | Tür | Default | UI | Engine |
|---|---|---|---|---|---|
| `playerPreference` | `player_preference` | String | `"INTERNAL"` | PlayerGeneralTab | PlayerEngineSelector |
| `isAutoplayEnabled` | `is_autoplay_enabled` | Boolean | `true` | PlayerGeneralTab | StreamAutoPlaySelector |
| `aspectMode` | `aspect_mode` | String | `"ORIGINAL"` | PlayerGeneralTab | Media3/MPV resize |
| `decoderPriority` | `decoder_priority` | Int | `0` | PlayerGeneralTab | DefaultRenderersFactory |
| `previewSeekbarEnabled` | `preview_seekbar_enabled` | Boolean | `true` | PlayerGeneralTab | PreviewGenerator |
| `showPlayerTitle` | `show_player_title` | Boolean | `true` | PlayerGeneralTab | PlayerTopBar |
| `showPlayerResolution` | `show_player_resolution` | Boolean | `true` | PlayerGeneralTab | StreamInfoOverlay |
| `showMediaInfo` | `show_media_info` | Boolean | `true` | PlayerGeneralTab | PlayerBottomActions |
| `titleLimitType` | `title_limit_type` | String | `"NONE"` | PlayerGeneralTab | formattedTitle |
| `skipIntroDurationSec` | `skip_intro_duration_sec` | Int | `5` | PlayerGeneralTab | AniSkip handler |
| `stillWatchingEnabled` | `still_watching_enabled` | Boolean | `true` | PlayerGeneralTab | StillWatchingGating |
| `stillWatchingThresholdMinutes` | `still_watching_threshold_minutes` | Int | `90` | PlayerGeneralTab | StillWatchingGating |
| `postPlayMode` | `post_play_mode` | String | `"AUTO_PLAY_NEXT"` | PlayerGeneralTab | PlayerNextEpisodeRules |
| `autoplaySessionLimit` | `autoplay_session_limit` | Int | `0` | PlayerGeneralTab | PlayerAutoplaySessionRules |

### 🎞 Oynatıcı – Video / Display

| Alan | DataStore Key | Tür | Default | UI | Engine |
|---|---|---|---|---|---|
| `frameRateMatchingMode` | `frame_rate_matching_mode` | String (enum) | `OFF` | PlayerGeneralTab | PlayerAfrPreflight |
| `resolutionMatchingEnabled` | `resolution_matching_enabled` | Boolean | `false` | PlayerGeneralTab | DisplayCapabilities |
| `dv7HandlingMode` | `dv7_handling_mode` | String (enum) | `AUTO` | PlayerVideoTab | DolbyVisionBaseLayerPolicy |
| `stripHdr10PlusSei` | `strip_hdr10_plus_sei` | Boolean | `false` | PlayerVideoTab | DolbyVisionExtractorsFactory |
| `qualityProfileJson` | `quality_profile_json` | String (JSON) | `""` | PlayerVideoTab | QualityDataHelper |

### 🔊 Oynatıcı – Ses

| Alan | DataStore Key | Tür | Default | UI | Engine |
|---|---|---|---|---|---|
| `defaultAudioBoost` | `default_audio_boost` | Float | `0.0` | PlayerAudioTab | GainAudioProcessor |
| `defaultAudioDelayMs` | `default_audio_delay_ms` | Long | `0L` | PlayerAudioTab | playerEngine.setAudioDelay() |
| `gainBoostDb` | `gain_boost_db` | Float | `0f` | PlayerAudioTab | GainAudioProcessor |
| `audioDelayPerRouteJson` | `audio_delay_per_route_json` | String (JSON) | `"{}"` | PlayerAudioTab | AudioDelayRouteConfig |

### 📝 Oynatıcı – Altyazı

| Alan | DataStore Key | Tür | Default | UI | Engine |
|---|---|---|---|---|---|
| `defaultSubtitleSize` | `default_subtitle_size` | Int | `16` | PlayerSubtitleTab | CaptionStyleCompat |
| `defaultSubtitleColor` | `default_subtitle_color` | Int | `0xFFFFFFFF` | PlayerSubtitleTab | CaptionStyleCompat |
| `subtitleBold` | `subtitle_bold` | Boolean | `false` | PlayerSubtitleTab | CaptionStyleCompat |
| `subtitleOutlineEnabled` | `subtitle_outline_enabled` | Boolean | `true` | PlayerSubtitleTab | CaptionStyleCompat |
| `subtitleDelayMs` | `subtitle_delay_ms` | Long | `0L` | PlayerSubtitleTab | playerEngine.setSubtitleDelay() |
| `preferredSubtitleLanguages` | `preferred_subtitle_languages` | String | `"tr,en"` | PlayerSubtitleTab | SubtitlePrefetcher |
| `addonSubtitleStartupMode` | `addon_subtitle_startup_mode` | String | `"ALL_SUBTITLES"` | PlayerSubtitleTab | SubtitlePrefetcher |

### 📦 Oynatıcı – Buffer

| Alan | DataStore Key | Tür | Default | UI | Engine |
|---|---|---|---|---|---|
| `minBufferMs` | `min_buffer_ms` | Int | `15000` | PlayerBufferTab | BitrateAwareLoadControl |
| `maxBufferMs` | `max_buffer_ms` | Int | `45000` | PlayerBufferTab | BitrateAwareLoadControl |
| `bufferForPlaybackMs` | `buffer_for_playback_ms` | Int | `5000` | PlayerBufferTab | BitrateAwareLoadControl |
| `bufferForPlaybackAfterRebufferMs` | `buffer_for_playback_after_rebuffer_ms` | Int | `3000` | PlayerBufferTab | BitrateAwareLoadControl |
| `backBufferDurationMs` | `back_buffer_duration_ms` | Int | `0` | PlayerBufferTab | BitrateAwareLoadControl |
| `parallelRangeEnabled` | `parallel_range_enabled` | Boolean | `false` | PlayerBufferTab | ParallelRangeDataSource |

### 👆 Oynatıcı – Gesture

| Alan | DataStore Key | Tür | Default | UI | Engine |
|---|---|---|---|---|---|
| `gestureVolumeEnabled` | `gesture_volume_enabled` | Boolean | `true` | PlayerGestureTab | PlayerGestureHelper |
| `gestureBrightnessEnabled` | `gesture_brightness_enabled` | Boolean | `true` | PlayerGestureTab | PlayerGestureHelper |
| `gestureZoomEnabled` | `gesture_zoom_enabled` | Boolean | `true` | PlayerGestureTab | PlayerGestureHelper |
| `doubleTapSeekSeconds` | `double_tap_seek_seconds` | Int | `10` | PlayerGestureTab | PlayerGestureHelper |
| `holdSpeedMultiplier` | `hold_speed_multiplier` | Float | `2.0` | PlayerGestureTab | PlayerGestureHelper |

### 📺 Oynatıcı – Harici / PiP

| Alan | DataStore Key | Tür | Default | UI | Engine |
|---|---|---|---|---|---|
| `preferredExternalPlayerPackage` | `preferred_external_player_package` | String | `""` | PlayerGeneralTab | ExternalPlayerLauncher |
| `pipEnabled` | `pip_enabled` | Boolean | `true` | PlayerGeneralTab | PlayerPipHelper |

### 🌐 Ağ

| Alan | DataStore Key | Tür | Default | UI | Engine |
|---|---|---|---|---|---|
| `dnsChoice` | `dns_choice` | Int | `0` | NetworkSettingsPage | DnsManager |

> **DNS Choice Değerleri:** 0=System, 1=Google, 2=Cloudflare, 3=AdGuard, 4=DNS.Watch, 5=Quad9, 6=DNS.SB, 7=Canadian Shield

### 🔔 Bildirimler

| Alan | DataStore Key | Tür | Default | UI | Engine |
|---|---|---|---|---|---|
| `airingNotificationsEnabled` | `airing_notifications_enabled` | Boolean | `false` | NotificationsPage | AiringNotificationWorker |

### 🔌 API Entegrasyonları

| Alan | DataStore Key | Tür | Default |
|---|---|---|---|
| `tmdbEnabled` | `tmdb_enabled` | Boolean | `true` |
| `tmdbUserApiKey` | `tmdb_user_api_key` | String | `""` |
| `tmdbLanguage` | `tmdb_language` | String | `"en"` |
| `mdbListApiKey` | `mdblist_api_key` | String | `""` |
| `mdbListEnabled` | `mdblist_enabled` | Boolean | `false` |
| `aniSkipEnabled` | `aniskip_enabled` | Boolean | `true` |
| `aniSkipAutoSkip` | `aniskip_auto_skip` | Boolean | `false` |
| `animeSkipClientId` | `animeskip_client_id` | String | `""` |
| `autoTranslateEnabled` | `auto_translate_enabled` | Boolean | `false` |

### 📚 Manga

| Alan | DataStore Key | Tür | Default |
|---|---|---|---|
| `mangaReadingMode` | `manga_reading_mode` | String | `"RightToLeft"` |
| `mangaColorFilter` | `manga_color_filter` | String | `"Normal"` |
| `mangaFitMode` | `manga_fit_mode` | String | `"FitScreen"` |
| `mangaBrightness` | `manga_brightness` | Float | `1.0` |

---

## JSON Alanları

### `audioDelayPerRouteJson`
```json
{
  "speakerDelayMs": 0,
  "bluetoothDelayMs": 150,
  "wiredDelayMs": 20,
  "hdmiDelayMs": 0
}
```

### `qualityProfileJson`
```
"P1080|8000"  →  preference=P1080, maxBitrateKbps=8000
"AUTO|-1"     →  AUTO, sınırsız
```

---

## Enum Değerleri

| Enum | Değerler |
|---|---|
| `FrameRateMatchingMode` | `OFF`, `START`, `START_STOP` |
| `Dv7HandlingMode` | `AUTO`, `OFF`, `DV81_LIBDOVI`, `HDR10_BASE_LAYER`, `STRIP_DV` |
| `QualityPreference` | `AUTO`, `P1080`, `P720`, `P480`, `DATA_SAVER` |
| `ThemeStyle` | `SYSTEM`, `LIGHT`, `DARK` |
| `ListStyle` | `COMFORTABLE`, `COMPACT`, `GRID` |
| `ItemsPerRow` | `TWO`, `THREE`, `FOUR` |
| `AudioRoute` | `SPEAKER`, `BLUETOOTH`, `WIRED`, `HDMI`, `OTHER` |
