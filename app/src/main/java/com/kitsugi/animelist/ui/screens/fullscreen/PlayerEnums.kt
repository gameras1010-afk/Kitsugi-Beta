package com.kitsugi.animelist.ui.screens.fullscreen

// ─────────────────────────────────────────────────────────────────────────────
// Aniyomi-derived Player Enums — adapted for Kitsugi
// Original: eu.kanade.tachiyomi.ui.player.PlayerEnums
// ─────────────────────────────────────────────────────────────────────────────

enum class PlayerOrientation {
    Free,
    Video,
    Portrait,
    ReversePortrait,
    SensorPortrait,
    Landscape,
    ReverseLandscape,
    SensorLandscape,
}

enum class VideoAspect(val label: String) {
    Crop("Kırp"),
    Fit("Sığdır"),
    Stretch("Gerer"),
}

enum class Decoder(val title: String, val value: String) {
    AutoCopy("Auto", "auto-copy"),
    Auto("Auto", "auto"),
    SW("SW", "no"),
    HW("HW", "mediacodec-copy"),
    HWPlus("HW+", "mediacodec"),
}

fun getDecoderFromValue(value: String): Decoder {
    return Decoder.entries.firstOrNull { it.value == value } ?: Decoder.Auto
}

enum class Debanding {
    None,
    CPU,
    GPU,
}

// ── Sheets: which bottom sheet is shown ─────────────────────────────────────
enum class KitsugiSheets {
    None,
    PlaybackSpeed,
    SubtitleTracks,
    AudioTracks,
    QualityTracks,
    Chapters,
    More,
    Screenshot,
}

// ── Panels: which side-panel is shown ────────────────────────────────────────
enum class KitsugiPanels {
    None,
    SubtitleSettings,
    SubtitleDelay,
    AudioDelay,
    VideoFilters,
}

// ── Dialogs: which dialog is shown ───────────────────────────────────────────
sealed class KitsugiDialogs {
    data object None : KitsugiDialogs()
    data object EpisodeList : KitsugiDialogs()
    data class IntegerPicker(
        val defaultValue: Int,
        val minValue: Int,
        val maxValue: Int,
        val step: Int,
        val nameFormat: String,
        val title: String,
        val onChange: (Int) -> Unit,
        val onDismissRequest: () -> Unit,
    ) : KitsugiDialogs()
}

// ── Player UI state updates (OSD notifications) ───────────────────────────────
sealed class KitsugiPlayerUpdates {
    data object None : KitsugiPlayerUpdates()
    data object DoubleSpeed : KitsugiPlayerUpdates()
    data object AspectRatio : KitsugiPlayerUpdates()
    data class ShowText(val value: String) : KitsugiPlayerUpdates()
}

// ── Video filters applied via MPV properties ─────────────────────────────────
enum class KitsugiVideoFilters(
    val label: String,
    val mpvProperty: String,
) {
    BRIGHTNESS("Parlaklık", "brightness"),
    SATURATION("Doygunluk", "saturation"),
    CONTRAST("Kontrast", "contrast"),
    GAMMA("Gamma", "gamma"),
    HUE("Ton", "hue"),
}

// ── Single-action gesture types ───────────────────────────────────────────────
enum class SingleActionGesture(val label: String) {
    None("Yok"),
    Seek("İleri/Geri"),
    PlayPause("Oynat/Duraklat"),
    Switch("Bölüm Değiştir"),
    Custom("Özel"),
}
