package com.kitsugi.animelist.core.player

/**
 * TASK_011 / TASK_033 — PlaybackState + PlayerListener
 *
 * Tüm oynatıcı motor durumlarını (ExoPlayer, MPV) soyutlayan
 * üst seviye sealed class. PlayerEngine.State ile bire bir
 * eşlenerek ViewModel'e aktarılır.
 *
 * Kaynak referans: NuvioTV PlayerRuntimeController + CloudStream IPlayer.kt
 * Kitsugi uyarlaması: KitsugiPlayerViewModel.playerState StateFlow'u bu tipi kullanır.
 */
sealed class PlaybackState {
    /** Oynatıcı hazır değil / henüz başlatılmadı */
    object Idle : PlaybackState()

    /** Veri tamponlanıyor */
    object Buffering : PlaybackState()

    /** Tamponlama tamamlandı, oynatmaya hazır */
    object Ready : PlaybackState()

    /** Aktif olarak oynatılıyor */
    object Playing : PlaybackState()

    /** Kullanıcı tarafından duraklatıldı */
    object Paused : PlaybackState()

    /** İçerik sona erdi */
    object Ended : PlaybackState()

    /** Kurtarılamaz hata — detay için [errorMessage] */
    data class Error(val errorMessage: String?, val errorCode: Int = -1) : PlaybackState()
}

/** true ise oynatıcı aktif oynatma modunda */
val PlaybackState.isActivelyPlaying: Boolean
    get() = this is PlaybackState.Playing

/** true ise oynatıcı hata ya da bekleme durumunda değil */
val PlaybackState.isReady: Boolean
    get() = this is PlaybackState.Playing || this is PlaybackState.Paused || this is PlaybackState.Ready

/**
 * TASK_033 — PlayerListener
 *
 * PlaybackState değişikliklerini dinleyen üst seviye arayüz.
 * Tüm metotlar opsiyonel override edilebilir (default boş gövde).
 *
 * Kaynak: CloudStream IPlayer.kt → PlayerListener
 */
interface PlayerListener {
    fun onPlaybackStateChanged(state: PlaybackState) {}
    fun onPositionChanged(positionMs: Long) {}
    fun onDurationChanged(durationMs: Long) {}
    fun onBufferedPositionChanged(bufferedPositionMs: Long) {}
    fun onError(state: PlaybackState.Error) {}
    fun onEnded() {}
}

/**
 * TASK_033 — PlayerManagerListener
 *
 * PlayerListener'ı genişleten yönetici seviyesi dinleyici.
 * Motor geçişi ve fatal hata bildirimleri ekler.
 *
 * Kaynak: NuvioTV PlayerRuntimeController → PlayerManagerListener
 */
interface PlayerManagerListener : PlayerListener {
    /** Oynatıcı motoru değiştirildiğinde (ExoPlayer ↔ MPV) */
    fun onPlayerSwitched(from: com.kitsugi.animelist.core.player.engine.PlayerEngineType,
                         to: com.kitsugi.animelist.core.player.engine.PlayerEngineType) {}

    /** Tüm fallback denemeleri tükendi, oynatma artık imkânsız */
    fun onFatalError(errorCode: Int, errorMsg: String) {}
}
