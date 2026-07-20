package com.kitsugi.animelist

/**
 * T4.2 – Player E2E Test Matrix
 *
 * Bu dosya Gherkin tarzı bir E2E test checklist'i içerir.
 * Her senaryo "Given / When / Then" formatında tanımlanmıştır.
 *
 * Otomasyonu olmayan (cihaz-bağımlı) senaryolar manuel test edilir.
 * Çalıştırma komutu (instrumented): ./gradlew connectedDebugAndroidTest
 *
 * Hedef kapsam:
 * ─────────────────────────────────────────────────────────────────
 * [Engine]     Media3 vs MPV vs External (VLC, JustPlayer, MX)
 * [Format]     HLS / DASH / MP4 / MKV / Torrent-like progressive
 * [Codec]      AVC / HEVC / AV1 / DV7 / HDR10+
 * [Subtitles]  SRT / ASS / VTT / external fetch
 * [Audio]      BT delay / HDMI / Wired / Boosted
 * [Gesture]    Volume swipe / Brightness / Double-tap / Hold-speed
 * [PIP]        Enter PIP / Remote actions / Return
 * [Quality]    Profile sort / Auto-switch on error
 * [Live]       DVR window / Go-To-Live (feature flag OFF skip)
 * ─────────────────────────────────────────────────────────────────
 */

/*
 * ═══════════════════════════════════════════════════════════════════
 *  SENARYO GRUBU 1 — Temel Oynatma ve Engine Seçimi
 * ═══════════════════════════════════════════════════════════════════
 *
 * S1.1 – Media3 + HLS + Türkçe Altyazı
 *   Given  playerPreference = INTERNAL, engine = Media3
 *   And    stream type = HLS (.m3u8)
 *   And    Turkish SRT subtitle available via addon
 *   When   video yüklenir
 *   Then   video 5 saniye içinde oynar
 *   And    TR altyazı otomatik seçilir (AddonSubtitleStartupMode=ALL_SUBTITLES)
 *
 * S1.2 – MPV + MKV + Embedded ASS Subtitle
 *   Given  playerPreference = MPV
 *   And    stream type = MKV (progressive download)
 *   And    embedded ASS track var
 *   When   video yüklenir
 *   Then   MPV styled ASS subtitle görünür
 *
 * S1.3 – External VLC + Progress Return
 *   Given  preferredExternalPlayerPackage = org.videolan.vlc
 *   And    VLC yüklü
 *   When   video VLC'ye gönderilir (23:00 izlenir)
 *   Then   VLC'den geri dönünce position 23:00 olarak kaydedilir
 *   And    "Devam Et" sorusu gelir
 *
 * ═══════════════════════════════════════════════════════════════════
 *  SENARYO GRUBU 2 — Kalite Profili ve Otomatik Kaynak Değiştirme
 * ═══════════════════════════════════════════════════════════════════
 *
 * S2.1 – Quality Profile Sort: 720p TR Tercihli
 *   Given  qualityProfile = {preferredResolution: 720, preferredLanguage: ["tr"]}
 *   When   stream listesi yüklenir (1080p EN, 720p TR, 480p EN)
 *   Then   list 720p TR en üstte, 1080p EN 2., 480p EN sonda
 *
 * S2.2 – Auto-Switch on Playback Error
 *   Given  currentStreamSources = [Source1, Source2, Source3]
 *   And    Source1 playback error (403)
 *   When   tryNextSource() çağrılır
 *   Then   Source2'ye geçilir, isAutoSwitching = false olur
 *   And   maxAutoSwitchAttempts (5) aşılınca hasError = true
 *
 * S2.3 – Parallel Range DataSource (enabled)
 *   Given  parallelRangeEnabled = true
 *   And    stream = progressive MP4 (large file)
 *   When   video yüklenir
 *   Then   network log'da birden fazla Range: header isteği görülür
 *   And    buffering normal playback'ten kısa sürer
 *
 * ═══════════════════════════════════════════════════════════════════
 *  SENARYO GRUBU 3 — Ses Rotası ve Gecikme
 * ═══════════════════════════════════════════════════════════════════
 *
 * S3.1 – Bluetooth Delay 150ms
 *   Given  audioDelayPerRouteJson = {"bt": 150}
 *   And    BT kulaklık bağlı (AudioRoute.BLUETOOTH)
 *   When   video oynarken BT kulaklık takılır
 *   Then   setAudioDelay(150ms) çağrılır
 *   And   speaker'a dönünce 0ms
 *
 * S3.2 – Audio Boost +10dB
 *   Given  gainBoostDb = 10.0f
 *   When   Media3 player başlatılır
 *   Then   GainAudioProcessor gain = 10.0f uygulanmış
 *   And    audio output clipping yok (overload test)
 *
 * ═══════════════════════════════════════════════════════════════════
 *  SENARYO GRUBU 4 — Altyazı Sistemi
 * ═══════════════════════════════════════════════════════════════════
 *
 * S4.1 – Startup Subtitle Prefetch (20s Timeout)
 *   Given  malId = 20 (Naruto), episode = 1
 *   And    opensubtitles-v3 addon enabled
 *   When   video yüklenir
 *   Then   20 saniye içinde TR/EN altyazılar listede görünür
 *   And    addonSubtitles state boş değil
 *
 * S4.2 – ASS Subtitle Fallback (enableAssExtractor = false)
 *   Given  enableAssExtractor = false (default)
 *   And    external ASS file eklendi
 *   When   video oynarken ASS altyazı seçilir
 *   Then   Media3 ASS'i text olarak render eder (styled değil)
 *   And    crash yok
 *
 * S4.3 – SRT Auto-Cache Hit
 *   Given  SRT altyazı URL'si daha önce indirilmiş (cache var)
 *   When   aynı URL'li video tekrar açılır
 *   Then   SubtitleFileCache.cacheSubtitle() → network isteği YOK
 *   And    local cache dosyası ile altyazı yüklenir
 *
 * ═══════════════════════════════════════════════════════════════════
 *  SENARYO GRUBU 5 — Gesture ve Kontroller
 * ═══════════════════════════════════════════════════════════════════
 *
 * S5.1 – Volume Swipe (Right Side)
 *   Given  gestureVolumeEnabled = true
 *   When   ekranın sağ yarısında aşağı doğru swipe (100px)
 *   Then   AudioManager.STREAM_MUSIC volume azalır
 *   And    volume overlay görünür
 *
 * S5.2 – Brightness Swipe (Left Side)
 *   Given  gestureBrightnessEnabled = true
 *   When   ekranın sol yarısında yukarı doğru swipe
 *   Then   WindowManager.LayoutParams.screenBrightness artar
 *
 * S5.3 – Double Tap Seek (10s)
 *   Given  doubleTapSeekSeconds = 10
 *   When   sağ tarafa çift dokunulur
 *   Then   position +10s ilerler, "+10s" toast görünür
 *
 * S5.4 – Hold to Speed Up (2x)
 *   Given  holdSpeedMultiplier = 2.0f
 *   When   ekran merkezi uzun basılır (long press)
 *   Then   playback speed 2.0f olur, "2x Hız" overlay görünür
 *   And    bırakınca hız eski değerine döner
 *
 * ═══════════════════════════════════════════════════════════════════
 *  SENARYO GRUBU 6 — PIP (Picture-in-Picture)
 * ═══════════════════════════════════════════════════════════════════
 *
 * S6.1 – PIP Enter on Home Press
 *   Given  pipEnabled = true
 *   And    feature FEATURE_PICTURE_IN_PICTURE destekleniyor
 *   When   Home tuşuna basılır (onUserLeaveHint)
 *   Then   PIP küçük pencere açılır
 *   And    play/pause/next remote actions görünür
 *
 * S6.2 – PIP Remote Pause Action
 *   Given  PIP penceresi açık
 *   When   pause remote action tetiklenir
 *   Then   playback durur, icon ▶ olur
 *
 * S6.3 – PIP Return to Full Screen
 *   Given  PIP penceresi açık
 *   When   PIP penceresine çift tıklanır / maximize edilir
 *   Then   tam ekran moda geri dönülür, playback devam eder
 *
 * ═══════════════════════════════════════════════════════════════════
 *  SENARYO GRUBU 7 — HDR / Dolby Vision
 * ═══════════════════════════════════════════════════════════════════
 *
 * S7.1 – HDR10+ SEI Strip
 *   Given  stripHdr10PlusSei = true
 *   And    video = HDR10+ HEVC stream
 *   When   video yüklenir
 *   Then   PlayerMediaSourceFactory ExtractorsFactory → SEI strip aktif
 *   And    playback crash yok, HDR10 base layer oynar
 *
 * S7.2 – DV7 AUTO Mode
 *   Given  dv7HandlingMode = AUTO
 *   And    cihaz DV desteklemiyor (displaySnapshot.supportsDolyVision = false)
 *   When   DV7 stream açılır
 *   Then   DolbyVisionBaseLayerPolicy → HDR10_BASE_LAYER seçilir
 *   And    crash yok
 *
 * ═══════════════════════════════════════════════════════════════════
 *  SENARYO GRUBU 8 — StillWatching + Binge Session
 * ═══════════════════════════════════════════════════════════════════
 *
 * S8.1 – StillWatching Prompt After Session Limit
 *   Given  autoplaySessionLimit = 3, stillWatchingEnabled = true
 *   When   3 bölüm art arda izlenir (onEpisodeCompleted x3)
 *   Then   4. bölüm başlamadan önce "Hâlâ izliyor musun?" prompt gelir
 *
 * S8.2 – StillWatching Confirmed
 *   Given  StillWatching prompt görünür
 *   When   kullanıcı "Evet" der
 *   Then   prompt kapanır, session counter sıfırlanır, 4. bölüm oynar
 *
 * S8.3 – StillWatching Timeout (countdown expires)
 *   Given  StillWatching prompt görünür, countdown = 30s
 *   When   30 saniye geçer (kullanıcı hiç dokunmaz)
 *   Then   showStillWatchingPrompt = true devam eder (player pause sinyal)
 *
 * ═══════════════════════════════════════════════════════════════════
 *  SENARYO GRUBU 9 — LiveHelper (Feature Flag OFF → Skip)
 * ═══════════════════════════════════════════════════════════════════
 *
 * S9.1 – LiveHelper Disabled (Normal VOD Behavior)
 *   Given  liveHelperEnabled = false
 *   And    stream URL contains "/live/"
 *   When   video yüklenir
 *   Then   LiveManager başlatılmaz
 *   And    video normal VOD gibi oynar (seekbar linear)
 *
 * S9.2 – LiveHelper Enabled + DVR Window (Manual Test)
 *   Given  liveHelperEnabled = true
 *   And    dvrWindowSec = 3600 (1 saat)
 *   When   kullanıcı baştan beri izliyor (position = 0)
 *   Then   "3600s GERİDE" badge görünür
 *   And    "Canlıya Geri Dön" butonu görünür
 *
 * ═══════════════════════════════════════════════════════════════════
 *  SENARYO GRUBU 10 — AFR (Auto Frame Rate Matching)
 * ═══════════════════════════════════════════════════════════════════
 *
 * S10.1 – 24fps Video → Display 24Hz Mode
 *   Given  frameRateMatchingMode = START
 *   And    display supports 24Hz mode
 *   When   24.0 fps video yüklenir
 *   Then   WindowManager.preferredDisplayModeId = 24Hz mod ID
 *
 * S10.2 – Display Does Not Support AFR → No Crash
 *   Given  frameRateMatchingMode = START
 *   And    cihaz 24Hz desteklemiyor
 *   When   24fps video yüklenir
 *   Then   Log.i "display does not support frame rate switching"
 *   And    crash yok, 60Hz kalır
 */

// Gerçek instrumented testler için PlayerE2ETest.kt dosyası aşağıya eklenir.
// Şimdilik bu dosya dokumentasyon + manuel test checklist olarak kullanılır.
class PlayerE2ETestMatrix
