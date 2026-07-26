package com.kitsugi.animelist.core.player.engine

import android.content.Context
import android.view.View
import com.kitsugi.animelist.core.player.SubtitleInput
import com.kitsugi.animelist.ui.screens.fullscreen.components.SubtitleStyleSettings
import com.kitsugi.animelist.ui.screens.fullscreen.components.TrackOption
import com.kitsugi.animelist.ui.screens.fullscreen.components.StreamInfoData

interface PlayerEngine {

    enum class State {
        IDLE,
        BUFFERING,
        READY,
        ENDED
    }

    interface Listener {
        fun onStateChanged(state: State)
        fun onPlaybackError(errorCode: Int, errorMsg: String, cause: Throwable?)
        fun onPositionChanged(positionMs: Long, durationMs: Long) {}
        fun onTracksChanged(audioTracks: List<TrackOption>, subtitleTracks: List<TrackOption>)
        fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {}
    }

    val engineType: PlayerEngineType
    val currentState: State
    val currentPosition: Long
    val duration: Long
    val isPlaying: Boolean
    val currentSpeed: Float
    val currentVolume: Float
    val subtitleDelayMs: Long
    val audioDelayMs: Long
    val isSubtitleDisabled: Boolean
    val activeStreamInfo: StreamInfoData

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    /**
     * Prepares and starts playback of a video stream.
     * This is a suspend function so heavy resolution work (DV policy, codec probing)
     * can be safely offloaded to IO dispatchers without blocking the main thread.
     */
    suspend fun prepare(
        videoUrl: String,
        audioUrl: String? = null,
        headers: Map<String, String> = emptyMap(),
        subtitles: List<SubtitleInput> = emptyList(),
        startPositionMs: Long = 0L,
        addonName: String? = null,
        isCS: Boolean = false,
        streamTitle: String? = null,
        qualityValue: Int? = null
    )

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun setSubtitleDelay(delayMs: Long)
    fun setAudioDelay(delayMs: Long)
    fun setSubtitleStyle(style: SubtitleStyleSettings)
    fun setResizeMode(resizeMode: Int) // Matches AspectRatioFrameLayout.RESIZE_MODE_*
    fun setAspectMode(mode: com.kitsugi.animelist.core.player.PlayerAspectMode)

    fun selectTrack(trackOption: TrackOption)
    fun disableSubtitles()

    /**
     * Creates or returns the View instance where video will be rendered.
     */
    fun createVideoView(context: Context): View

    /**
     * Stop and release resources.
     */
    fun release()
}
