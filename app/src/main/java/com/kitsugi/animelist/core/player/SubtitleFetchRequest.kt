package com.kitsugi.animelist.core.player

/**
 * S03 – Startup altyazı fetch isteği veri modeli.
 *
 * KitsugiTV-dev PlayerRuntimeControllerInitialization buildSubtitleFetchRequest() portu.
 * Video oynatma başladığında SubtitleRepositoryImpl.getSubtitles() çağrısına geçilir.
 */
data class SubtitleFetchRequest(
    /** Stremio content type: "series" veya "movie" */
    val type: String,
    /** Stremio canonical ID: "tt1234567:1:5" veya "kitsu:12345:5" */
    val id: String,
    /** Video stream URL (OpenSubtitles hash için; null = hash atlanır) */
    val videoUrl: String? = null,
    /** Video stream HTTP header'ları */
    val videoHeaders: Map<String, String>? = null,
    /** Dosya adı tahmini (addon ipucu için) */
    val filename: String? = null
)
