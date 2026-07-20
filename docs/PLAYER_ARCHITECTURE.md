# KitsugiAnimeList Player Architecture

> **Son güncelleme:** 2026-07-15  
> Bu belge, KitsugiAnimeList oynatıcı altyapısını açıklar. Yeni geliştiricilerin  
> koda başlamadan önce bu dökümanı okuması önerilir.

---

## Genel Bakış

KitsugiAnimeList, iki ayrı oynatıcı motoru ve bir harici oynatıcı köprüsü üzerinden  
media content oynatmayı destekler:

```
                 ┌─────────────────────────────────┐
                 │       KitsugiPlayerViewModel       │
                 │  (State + SettingsDataStore)     │
                 └────────────┬────────────────────-┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
     Media3PlayerEngine   MpvPlayerEngine  ExternalPlayerLauncher
     (ExoPlayer + DV)     (libmpv JNI)     (Intent to 3rd party)
```

---

## Modüller

### `core/player/engine/`

| Dosya | Açıklama |
|---|---|
| `PlayerEngine.kt` | Ortak arayüz: `prepare()`, `play()`, `pause()`, `seekTo()`, `release()` |
| `PlayerEngineType.kt` | Enum: `MEDIA3`, `MPV` |
| `PlayerEngineSelector.kt` | `AppSettings.playerPreference` + codec availability'e göre engine seçer |
| `Media3PlayerEngine.kt` | ExoPlayer (Media3) implementasyonu; DV7/HDR10+ desteği, GainAudioProcessor |
| `MpvPlayerEngine.kt` | libmpv JNI köprüsü; ASS/SSA render, harici codec |
| `PlayerFallbackCoordinator.kt` | Bir engine başarısız olunca diğerine fallback |

### `core/player/` (Ana Yardımcılar)

| Dosya | T | Açıklama |
|---|---|---|
| `PlayerMediaSourceFactory.kt` | T1.13 | DataSource / ExtractorFactory / MergingMediaSource oluşturma |
| `PlayerLoadingDiagnostics.kt` | T1.13 | Buffering, first-frame, bitrate, error logu |
| `PlayerPlaybackAnalytics.kt` | T1.13 | İzleme süresi, seek, stall oranı metrikleri |
| `ParallelRangeDataSource.kt` | T1.9 | Multi-connection progressive download |
| `BitrateAwareLoadControl.kt` | – | ExoPlayer LoadControl; buffer threshold'ları |
| `AudioDelayRouteConfig.kt` | T1.3 | Ses rotasına göre delay (BT/HDMI/kablolu) |
| `AudioOutputRouteDetector.kt` | T1.3 | Aktif ses çıkış rotasını algılar |
| `FrameRateUtils.kt` | T2.1 (AFR) | HDMI refresh rate / resolution matching |
| `DisplayCapabilities.kt` | T2.1 | Cihaz HDR ve refresh rate desteği |
| `PlayerGestureHelper.kt` | T2.1 | Volume / Brightness / Zoom gesture tanıma (Compose) |
| `PlayerAfrPreflight.kt` | T2.1 | Oynatma öncesi AFR doğrulama ve display switching |
| `PreviewGenerator.kt` | T2.2 | Seekbar thumbnail extract + LRU cache |
| `PlayerPipHelper.kt` | T2.3 | PiP mode enter/exit + RemoteAction handler |
| `QualityProfile.kt` | T2.4 | Kalite bandı (AUTO/1080p/720p/480p) + bitrate limiti |
| `ExternalPlayerLauncher.kt` | T2.5 | VLC / JustPlayer / MX Player intent builder |
| `ExternalPlayerPackages.kt` | T2.5 | Cihazda kurulu harici oynatıcıları tarar |
| `StillWatchingGating.kt` | T1.7 | Hareketsizlik sayacı ("Hâlâ izliyor musun?") |
| `PlayerAutoplaySessionRules.kt` | T1.7 | Binge session limit kuralları |
| `StreamAutoPlaySelector.kt` | T1.7 | Sonraki bölümü otomatik seçer |
| `ExternalAutoNextPolicy.kt` | T1.7 | Harici oynatıcı sonrası auto-next politikası |
| `KeepAliveService.kt` | T2.5 | Harici oynatıcı geçişlerinde uygulama yaşatma |
| `OfflinePlaybackHelper.kt` | T1.14 | İndirilen dosyaları listeler ve PlayerParams üretir |
| `PlayerLogger.kt` | – | Oynatıcı event'lerini yapılandırılmış olarak loglar |
| `PlayerMediaSessionHelper.kt` | – | MediaSession / MediaController entegrasyonu |

### Dolby Vision Altyapısı

| Dosya | Açıklama |
|---|---|
| `DolbyVisionBaseLayerPolicy.kt` | AUTO modunda DV7 → DV8.1 / HDR10 / Native kararı |
| `DolbyVisionCodecFallback.kt` | Decoder fallback zinciri |
| `DolbyVisionExtractorsFactory.kt` | RPU NAL sıyırma / dönüştürme extractor factory |
| `DolbyVisionMatroskaTransformer.kt` | MKV container DV track işleme |
| `DoviBridge.kt` | libdovi JNI köprüsü |
| `HevcDvRpuStripper.kt` | HEVC bitstream DV RPU kaldırma |
| `HevcHdr10PlusStripper.kt` | HEVC HDR10+ SEI NAL kaldırma |
| `dvmkv/DolbyVisionCompatibility.kt` | HDR10 base-layer mod bayrağı |

---

## Veri Akışı — Oynatma Başlangıcı

```
KitsugiPlayerViewModel.loadStream(url, headers, subs)
      │
      ├── PlayerEngineSelector.select(settings)  ──► engine seçimi
      │
      ├── PlayerAfrPreflight.run()               ──► AFR + display switch
      │
      ├── PlayerMediaSourceFactory.create()
      │       ├── buildExtractorsFactory()        ──► DV7 modu kararı
      │       ├── buildDataSourceFactory()        ──► OkHttp + ParallelRange
      │       └── create()                        ──► MediaSource/MergingMediaSource
      │
      ├── PlayerEngine.prepare(videoUrl, audioUrl, headers, subs)
      │       └── PlayerLoadingDiagnostics.startSession()
      │
      └── PlayerPlaybackAnalytics.startSession()
```

---

## Ayar → Engine Bağlantı Tablosu

| AppSettings Alanı | Engine Etkisi |
|---|---|
| `playerPreference` | Media3 / MPV / External seçimi |
| `decoderPriority` | `DefaultRenderersFactory.setExtensionRendererMode()` |
| `dv7HandlingMode` | `DolbyVisionBaseLayerPolicy` kararı |
| `stripHdr10PlusSei` | `DolbyVisionExtractorsFactory` SEI sıyırma |
| `frameRateMatchingMode` | `PlayerAfrPreflight` display mode switch |
| `resolutionMatchingEnabled` | `DisplayCapabilities` resolution switch |
| `parallelRangeEnabled` | `ParallelRangeDataSource.Factory` wrap |
| `minBufferMs` / `maxBufferMs` | `BitrateAwareLoadControl` |
| `pipEnabled` | `PlayerPipHelper.enterPip()` guard |
| `previewSeekbarEnabled` | `PreviewGenerator` aktif / pasif |
| `gestureVolumeEnabled` | `PlayerGestureHelper` volume gesture |
| `gestureBrightnessEnabled` | `PlayerGestureHelper` brightness gesture |
| `gestureZoomEnabled` | `PlayerGestureHelper` pinch zoom |
| `audioDelayPerRouteJson` | `AudioDelayRouteConfig.getDelayFor(route)` |
| `qualityProfileJson` | `QualityDataHelper.sortByProfile()` |
| `preferredExternalPlayerPackage` | `ExternalPlayerLauncher.launch()` intent target |
| `showPlayerTitle` | `PlayerTopBar` görünürlük |
| `showPlayerResolution` | `StreamInfoOverlay` çözünürlük etiketi |
| `showMediaInfo` | `PlayerBottomActions` info paneli butonu |
| `titleLimitType` | `formattedTitle` karakter kırpma |

---

## Engine Listener Arayüzü

```kotlin
interface PlayerEngine.Listener {
    fun onStateChanged(state: State)         // IDLE/BUFFERING/READY/ENDED
    fun onPlaybackError(code: Int, msg: String, cause: Exception?)
    fun onVideoSizeChanged(w: Int, h: Int, rotation: Int, ratio: Float)
    fun onTracksChanged(audio: List<TrackOption>, subtitles: List<TrackOption>)
}
```

---

## Test Stratejisi

| Tip | Lokasyon | Kapsam |
|---|---|---|
| Unit | `app/src/test/` | Settings migration, QualityProfile, Diagnostics, Analytics |
| Instrumented | `app/src/androidTest/` | Player E2E matrix (T4.2) |
| Manual | `docs/PLAYER_E2E_CHECKLIST.md` | HDR, PiP, BT delay, AFR |
