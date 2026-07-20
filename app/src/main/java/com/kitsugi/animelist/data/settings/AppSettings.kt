package com.kitsugi.animelist.data.settings

data class AppSettings(
    val selectedThemeId: String = "mint",
    val showAdultContent: Boolean = false,
    val selectedListLayoutId: String = "comfortable",
    val profileName: String = "Profilim",
    val listTitle: String = "Anime & Manga Listem",
    val anilistUsername: String = "",
    val anilistProfileImageUri: String = "",
    val anilistBannerImageUri: String = "",
    val malUsername: String = "",
    val malProfileImageUri: String = "",
    val malBannerImageUri: String = "",
    val simklUsername: String = "",
    val simklProfileImageUri: String = "",
    val simklBannerImageUri: String = "",
    val profileImageUri: String = "",
    val bannerImageUri: String = "",
    // AniHyou'dan: Başlık dili tercihi (Romaji / İngilizce / Japonca)
    val titleLanguage: String = "ROMAJI",
    // AniHyou'dan: Puanlama formatı (POINT_10 / POINT_100 / POINT_5 / POINT_3)
    val scoreFormat: String = "POINT_10",
    // MoeList'ten: Puanları gizle
    val hideScores: Boolean = false,
    val showAnimeLogos: Boolean = false,
    val playerPreference: String = "INTERNAL",
    val isAutoplayEnabled: Boolean = true,
    val skipIntroDurationSec: Int = 5,
    val defaultSubtitleSize: Int = 16,
    val defaultSubtitleColor: Int = 0xFFFFFFFF.toInt(),
    val subtitleBold: Boolean = false,
    val subtitleOutlineEnabled: Boolean = true,
    val defaultAudioBoost: Float = 0.0f,
    val defaultAudioDelayMs: Long = 0L,
    val minBufferMs: Int = 15_000,
    val maxBufferMs: Int = 50_000,
    val bufferForPlaybackMs: Int = 2_500,
    val bufferForPlaybackAfterRebufferMs: Int = 3_000,
    val backBufferDurationMs: Int = 0,
    val mangaReadingMode: String = "RightToLeft",
    val mangaColorFilter: String = "Normal",
    val mangaFitMode: String = "FitScreen",
    val mangaBrightness: Float = 1.0f,
    val dnsChoice: Int = 0,
    // ─── Harici API Entegrasyonları ──────────────────────────────────────────
    // TMDB: Kullanıcı kendi API anahtarını girebilir; boşsa dahili anahtar kullanılır
    val tmdbEnabled: Boolean = true,
    val tmdbUserApiKey: String = "",
    val tmdbModernHomeEnabled: Boolean = true,
    val tmdbEnrichContinueWatching: Boolean = true,
    val tmdbLanguage: String = "en",
    val tmdbUseArtwork: Boolean = true,
    val tmdbUseBasicInfo: Boolean = true,
    val tmdbUseDetails: Boolean = true,
    val tmdbUseReleaseDates: Boolean = true,
    val tmdbUseCredits: Boolean = true,
    val tmdbUseProductions: Boolean = true,
    val tmdbUseNetworks: Boolean = true,
    val tmdbUseEpisodes: Boolean = true,
    val tmdbUseTrailers: Boolean = true,
    val tmdbUseMoreLikeThis: Boolean = true,
    val tmdbUseCollections: Boolean = true,
    // MDBList: Çoklu platform puan zenginleştirme (IMDb, RT, Metacritic, vb.)
    val mdbListApiKey: String = "",
    val mdbListEnabled: Boolean = false,
    val mdbListShowImdb: Boolean = true,
    val mdbListShowTomatoes: Boolean = true,
    val mdbListShowAudience: Boolean = false,
    val mdbListShowMetacritic: Boolean = true,
    val mdbListShowLetterboxd: Boolean = false,
    val mdbListShowTmdb: Boolean = false,
    val mdbListShowTrakt: Boolean = false,
    // AniSkip: Anime intro/outro zaman damgalarını otomatik çekme
    val aniSkipEnabled: Boolean = true,
    val aniSkipAutoSkip: Boolean = false,
    // AnimeSkip: anime-skip.com için geliştirici anahtarı ve client ID
    val animeSkipClientId: String = "",
    // Açıklama Otomatik Çevirisi: Türkçe veri bulunamazsa Google Translate devreye girer
    val autoTranslateEnabled: Boolean = false,
    // TV ana sayfa yerleşim düzeni (classic / modern / grid)
    val selectedHomeLayoutId: String = "classic",
    // AFR (Auto Frame Rate) ayarları
    val frameRateMatchingMode: FrameRateMatchingMode = FrameRateMatchingMode.OFF,
    val resolutionMatchingEnabled: Boolean = false,
    val decoderPriority: Int = 0, // 0 = Hardware only, 1 = Software fallback, 2 = Software preferred
    // ─── Dolby Vision / HDR ─────────────────────────────────────────────────
    // DV7 → DV8.1 dönüşüm modu (AUTO = cihaz capability'sine göre karar verilir)
    val dv7HandlingMode: Dv7HandlingMode = Dv7HandlingMode.AUTO,
    // HDR10+ SEI NAL mesajlarını HEVC bitstream'inden sıyırır
    val stripHdr10PlusSei: Boolean = false,
    val themeMode: String = "FOLLOW_SYSTEM",
    val amoledBlack: Boolean = false,
    val customAccentColor: Int = 0,
    val defaultTab: String = "LAST_USED",
    val lastUsedTab: String = "Explore",
    val appLanguage: String = "system",
    val fixedNavBar: Boolean = false,
    val aspectMode: String = "ORIGINAL",
    val gainBoostDb: Float = 0f,
    val subtitleDelayMs: Long = 0L,
    val preferredSubtitleLanguages: String = "tr",
    val addonSubtitleStartupMode: String = "PREFERRED_ONLY",
    // ─── Gesture Ayarları (T2.1 + T2.7) ────────────────────────────────────
    val gestureVolumeEnabled: Boolean = true,
    val gestureBrightnessEnabled: Boolean = true,
    val gestureZoomEnabled: Boolean = true,
    /** Dikey swipe hassasiyeti — 0.5 (yavaş) … 2.0 (hızlı). Task_050 TASK_DOSYASI referans. */
    val gestureScrollSensitivity: Float = 1.0f,
    val doubleTapSeekSeconds: Int = 10,
    val holdSpeedMultiplier: Float = 2.0f,
    // ─── PIP (T2.3) ─────────────────────────────────────────────────────────
    val pipEnabled: Boolean = true,
    // ─── Ses Rotası Gecikmesi (T1.3) ─────────────────────────────────────────
    val audioDelayPerRouteJson: String = "{}",
    // ─── Kalite Profili (T2.4) ───────────────────────────────────────────────
    val qualityProfileJson: String = "",
    // ─── Yayın Takvimi Bildirimleri (T3.3) ──────────────────────────────────
    // true = "İzliyorum" listesindeki animeler yayınlandığında bildirim gönder
    val airingNotificationsEnabled: Boolean = false,
    val searchHistoryEnabled: Boolean = true,
    // ─── T1.7 – StillWatching + PostPlayMode + AutoplaySessionRules ──────────
    /** true = N dakika hareketsizlik sonrası "Hâlâ izliyor musun?" sorusu gelir */
    val stillWatchingEnabled: Boolean = true,
    /** Kaç dakika hareketsizlik sonrası StillWatching prompt tetiklenir */
    val stillWatchingThresholdMinutes: Int = 90,
    /** Sonraki bölüm oynatma modu: MANUAL, AUTO_PLAY_NEXT, BINGE_PROMPT */
    val postPlayMode: String = "AUTO_PLAY_NEXT",
    /** 0 = sınırsız; N = N bölüm art arda izlenince StillWatching prompt */
    val autoplaySessionLimit: Int = 0,
    // ─── T1.9 – Paralel Aralık İndirme (ParallelRangeDataSource) ─────────────
    /** true = MKV/MP4 progressive içerik için çok bağlantılı chunk indirme aktif */
    val parallelRangeEnabled: Boolean = false,
    // ─── T2.2 – Önizleme Seekbar (Preview Seekbar) ───────────────────────────
    val previewSeekbarEnabled: Boolean = true,
    // ─── T2.5 – Harici Oynatıcı Tercihi ──────────────────────────────────────
    val preferredExternalPlayerPackage: String = "",
    // ─── T2.6 – Oynatıcı Başlık / Medya Bilgisi Görünürlüğü (CloudStream) ───
    /** true = oynatma sırasında üst başlık overlay görünür */
    val showPlayerTitle: Boolean = true,
    /** true = video çözünürlüğü (örn: 1080p) overlay'de gösterilir */
    val showPlayerResolution: Boolean = true,
    /** true = codec / bitrate / fps detay paneli açılabilir */
    val showMediaInfo: Boolean = true,
    /** NONE = tam başlık, LIMIT_20 = 20 karakter, LIMIT_40 = 40 karakter */
    val titleLimitType: String = "NONE",
    // ─── T1.15 – Canlı Yayın (LiveHelper) ───────────────────────────────────
    /** true = LiveHelper / LiveManager aktif; false = canlı yayınlar normal VOD gibi oynatılır */
    val liveHelperEnabled: Boolean = false,
    // ─── T1.10 – Libass / ASS Extractor (Opsiyonel, riskli) ─────────────────
    /** true = KitsugiAssMatroskaExtractor DefaultExtractorsFactory'e eklenir; false = Media3 fallback */
    val enableAssExtractor: Boolean = false,
    val splashAnimationEnabled: Boolean = true,
    val splashSoundEnabled: Boolean = true
)

enum class FrameRateMatchingMode {
    OFF,
    START,
    START_STOP
}

/**
 * Dolby Vision Profile 7 stream'lerinin nasıl işleneceğini belirler.
 *
 * AUTO: [DolbyVisionBaseLayerPolicy] cihaz HDR + codec kabiliyetine göre karar verir.
 * OFF: Native DV7 pass-through (destekleyen ekran/deşifreleyici gerekir).
 * DV81_LIBDOVI: libdovi JNI köprüsü üzerinden DV7 → DV8.1'e runtime dönüşüm.
 * HDR10_BASE_LAYER: RPU NAL'larını HEVC bitstream'inden sıyır; HDR10 base layer kalır.
 * STRIP_DV: Tüm DV RPU/EL NAL'larını sıyır; düz SDR/HDR10 çıktısı.
 */
enum class Dv7HandlingMode {
    AUTO,
    OFF,
    DV81_LIBDOVI,
    HDR10_BASE_LAYER,
    STRIP_DV
}