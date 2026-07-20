package com.kitsugi.animelist.core.player.engine

import android.content.Context
import android.view.View
import com.kitsugi.animelist.core.player.SubtitleInput
import com.kitsugi.animelist.ui.screens.fullscreen.components.SubtitleStyleSettings
import com.kitsugi.animelist.ui.screens.fullscreen.components.TrackOption
import com.kitsugi.animelist.ui.screens.fullscreen.components.StreamInfoData
import com.kitsugi.animelist.data.settings.AppSettings
import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.MPVNode

class MpvPlayerEngine(
    private val context: Context,
    private val settings: AppSettings
) : PlayerEngine, MPV.EventObserver {

    private val TAG = "MpvPlayerEngine"
    private val listeners = mutableListOf<PlayerEngine.Listener>()
    private var mpvView: KitsugiMpvSurfaceView? = null

    override val engineType: PlayerEngineType = PlayerEngineType.MPV
    override var currentState: PlayerEngine.State = PlayerEngine.State.IDLE
        private set

    override var currentPosition: Long = 0L
        private set

    override var duration: Long = 0L
        private set

    override var isPlaying: Boolean = false
        private set

    private var _currentSpeed: Float = 1.0f
    override val currentSpeed: Float
        get() = _currentSpeed

    private var _currentVolume: Float = 1.0f
    override val currentVolume: Float
        get() = _currentVolume

    override var subtitleDelayMs: Long = 0L
        private set

    override var audioDelayMs: Long = 0L
        private set

    private var _isSubtitleDisabled: Boolean = false
    override val isSubtitleDisabled: Boolean
        get() = _isSubtitleDisabled

    private var currentAddonName: String? = null
    private var streamTitle: String? = null
    private var videoUrl: String? = null
    private var pendingHeaders: Map<String, String> = emptyMap()
    private var pendingSubtitles: List<SubtitleInput> = emptyList()
    private var pendingStartPositionMs: Long = 0L

    override val activeStreamInfo: StreamInfoData
        get() {
            val vCodec = runCatching { mpvView?.mpv?.getPropertyString("video-codec") }.getOrNull()
            val aCodec = runCatching { mpvView?.mpv?.getPropertyString("audio-codec") }.getOrNull()
            return StreamInfoData(
                addonName = currentAddonName ?: "Dahili",
                streamName = "MPV",
                streamDescription = streamTitle,
                filename = videoUrl?.substringAfterLast('/'),
                playerEngine = "MPV Oynatıcı",
                videoWidth = mpvView?.width,
                videoHeight = mpvView?.height,
                videoCodec = vCodec,
                audioCodec = aCodec
            )
        }

    override fun addListener(listener: PlayerEngine.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: PlayerEngine.Listener) {
        listeners.remove(listener)
    }

    override suspend fun prepare(
        videoUrl: String,
        audioUrl: String?,
        headers: Map<String, String>,
        subtitles: List<SubtitleInput>,
        startPositionMs: Long,
        addonName: String?,
        isCS: Boolean,
        streamTitle: String?,
        qualityValue: Int?
    ) {
        this.videoUrl = videoUrl
        this.currentAddonName = addonName
        this.streamTitle = streamTitle

        updateState(PlayerEngine.State.BUFFERING)

        this.pendingHeaders = headers
        this.pendingSubtitles = subtitles
        this.pendingStartPositionMs = startPositionMs

        val view = mpvView
        if (view != null) {
            view.setMedia(videoUrl, headers, startPositionMs)
            view.applySubtitleLanguagePreferences(settings.preferredSubtitleLanguages, null)
            subtitles.forEach { sub ->
                view.addAndSelectExternalSubtitle(sub.url, sub.name, sub.lang)
            }
            view.applyHardwareDecodeMode(MpvHardwareDecodeMode.AUTO_SAFE)
            isPlaying = true
        }
    }

    override fun play() {
        mpvView?.setPaused(false)
        isPlaying = true
        updateState(PlayerEngine.State.READY)
    }

    override fun pause() {
        mpvView?.setPaused(true)
        isPlaying = false
        updateState(PlayerEngine.State.READY)
    }

    override fun seekTo(positionMs: Long) {
        mpvView?.seekToMs(positionMs)
        currentPosition = positionMs
        notifyPositionChanged()
    }

    override fun setPlaybackSpeed(speed: Float) {
        _currentSpeed = speed
        mpvView?.setPlaybackSpeed(speed)
    }

    override fun setVolume(volume: Float) {
        _currentVolume = volume
        if (volume > 1.0f) {
            runCatching {
                mpvView?.mpv?.setPropertyDouble("volume", 100.0)
            }
            val boost = volume - 1.0f
            val db = (boost * 6f).toInt().coerceIn(0, 6)
            mpvView?.applyAudioAmplificationDb(db)
        } else {
            runCatching {
                mpvView?.mpv?.setPropertyDouble("volume", (volume * 100.0).coerceIn(0.0, 100.0))
            }
            mpvView?.applyAudioAmplificationDb(0)
        }
    }

    override fun setSubtitleDelay(delayMs: Long) {
        subtitleDelayMs = delayMs
        runCatching {
            mpvView?.mpv?.setPropertyDouble("sub-delay", delayMs / 1000.0)
        }
    }

    override fun setAudioDelay(delayMs: Long) {
        audioDelayMs = delayMs
        runCatching {
            mpvView?.mpv?.setPropertyDouble("audio-delay", delayMs / 1000.0)
        }
    }

    override fun setSubtitleStyle(style: SubtitleStyleSettings) {
        mpvView?.applySubtitleStyle(style)
    }

    override fun setResizeMode(resizeMode: Int) {
        val aspectMode = when (resizeMode) {
            0 -> AspectMode.ORIGINAL   // RESIZE_MODE_FIT
            4 -> AspectMode.FULL_SCREEN // RESIZE_MODE_ZOOM
            3 -> AspectMode.STRETCH    // RESIZE_MODE_FILL
            else -> AspectMode.ORIGINAL
        }
        mpvView?.applyAspectMode(aspectMode)
    }

    override fun setAspectMode(mode: com.kitsugi.animelist.core.player.PlayerAspectMode) {
        val aspectProp = com.kitsugi.animelist.core.player.PlayerAspectScaleUtils.getMpvAspectProperty(mode)
        runCatching {
            mpvView?.mpv?.setPropertyString("video-aspect-override", aspectProp)
        }
    }

    override fun selectTrack(trackOption: TrackOption) {
        if (trackOption.groupIndex == 0) {
            mpvView?.selectAudioTrackById(trackOption.trackIndex)
        } else if (trackOption.groupIndex == 1) {
            mpvView?.selectSubtitleTrackById(trackOption.trackIndex)
        }
        updateTracks()
    }

    override fun disableSubtitles() {
        mpvView?.disableSubtitles()
        updateTracks()
    }
    override fun createVideoView(context: Context): View {
        if (mpvView == null) {
            mpvView = KitsugiMpvSurfaceView(context).apply {
                ensureInitialized()
                // Register this engine as observer on the underlying MPV instance
                mpv.addObserver(this@MpvPlayerEngine)
                // Observe the properties we need
                mpv.observeProperty("time-pos", MPV.mpvFormat.MPV_FORMAT_DOUBLE)
                mpv.observeProperty("duration", MPV.mpvFormat.MPV_FORMAT_DOUBLE)
                mpv.observeProperty("pause", MPV.mpvFormat.MPV_FORMAT_FLAG)
                mpv.observeProperty("paused-for-cache", MPV.mpvFormat.MPV_FORMAT_FLAG)
                mpv.observeProperty("core-idle", MPV.mpvFormat.MPV_FORMAT_FLAG)
                mpv.observeProperty("track-list", MPV.mpvFormat.MPV_FORMAT_NONE)

                // TASK-002: Observe speed, volume, and sub-visibility
                mpv.observeProperty("speed", MPV.mpvFormat.MPV_FORMAT_DOUBLE)
                mpv.observeProperty("volume", MPV.mpvFormat.MPV_FORMAT_DOUBLE)
                mpv.observeProperty("sub-visibility", MPV.mpvFormat.MPV_FORMAT_FLAG)

                val pendingUrl = videoUrl
                if (!pendingUrl.isNullOrBlank()) {
                    setMedia(pendingUrl, pendingHeaders, pendingStartPositionMs)
                    applySubtitleLanguagePreferences(settings.preferredSubtitleLanguages, null)
                    pendingSubtitles.forEach { sub ->
                        addAndSelectExternalSubtitle(sub.url, sub.name, sub.lang)
                    }
                    applyHardwareDecodeMode(MpvHardwareDecodeMode.AUTO_SAFE)
                    isPlaying = true
                }
            }
        }
        return mpvView!!
    }

    override fun release() {
        runCatching { mpvView?.mpv?.removeObserver(this) }
        runCatching { mpvView?.releasePlayer() }
        mpvView = null
        listeners.clear()
    }
    // ──── MPV.EventObserver callbacks ────────────────────────────────────────

    override fun eventProperty(property: String) {
        if (property == "track-list") {
            updateTracks()
        }
    }
    override fun eventProperty(property: String, value: Boolean) {
        when (property) {
            "pause" -> {
                isPlaying = !value
                updateState(PlayerEngine.State.READY)
            }
            "paused-for-cache", "core-idle" -> {
                checkBufferingState()
            }
            "sub-visibility" -> {
                _isSubtitleDisabled = !value
            }
        }
    }

    override fun eventProperty(property: String, value: Double) {
        when (property) {
            "time-pos" -> {
                val posMs = (value * 1000).toLong().coerceAtLeast(0L)
                if (posMs != currentPosition) {
                    currentPosition = posMs
                    notifyPositionChanged()
                }
            }
            "duration" -> {
                val durMs = (value * 1000).toLong().coerceAtLeast(0L)
                if (durMs != duration) {
                    duration = durMs
                    notifyPositionChanged()
                }
            }
            "speed" -> {
                _currentSpeed = value.toFloat()
            }
            "volume" -> {
                val mpvVol = (value / 100.0).toFloat()
                if (_currentVolume <= 1.0f || mpvVol < 1.0f) {
                    _currentVolume = mpvVol
                }
            }
        }
    }
    override fun eventProperty(property: String, value: Long) {}
    override fun eventProperty(property: String, value: String) {}
    override fun eventProperty(property: String, value: MPVNode) {}

    // New API: event now carries MPVNode as second arg
    override fun event(eventId: Int, data: MPVNode) {
        when (eventId) {
            MPV.mpvEvent.MPV_EVENT_FILE_LOADED -> {
                updateState(PlayerEngine.State.READY)
                updateTracks()
            }
            MPV.mpvEvent.MPV_EVENT_END_FILE -> {
                updateState(PlayerEngine.State.ENDED)
            }
        }
    }

    // ──── Internal helpers ────────────────────────────────────────────────────

    private fun updateState(newState: PlayerEngine.State) {
        if (currentState != newState) {
            currentState = newState
            listeners.forEach { it.onStateChanged(newState) }
        }
    }

    private fun checkBufferingState() {
        val view = mpvView ?: return
        val isBuffering = view.isPausedForCacheNow() || view.isCoreIdleNow()
        if (isBuffering) {
            updateState(PlayerEngine.State.BUFFERING)
        } else {
            updateState(PlayerEngine.State.READY)
        }
    }

    private fun notifyPositionChanged() {
        listeners.forEach { it.onPositionChanged(currentPosition, duration) }
    }

    private fun updateTracks() {
        val view = mpvView ?: return
        val snapshot = view.readTrackSnapshot()
        val dummyGroup = createDummyGroup()

        val audioOptions = snapshot.audioTracks.map { track ->
            TrackOption(
                group = dummyGroup,
                groupIndex = 0,
                trackIndex = track.id,
                label = track.name,
                isSelected = track.isSelected
            )
        }

        val subtitleOptions = snapshot.subtitleTracks.map { track ->
            TrackOption(
                group = dummyGroup,
                groupIndex = 1,
                trackIndex = track.id,
                label = track.name,
                isSelected = track.isSelected
            )
        }

        listeners.forEach { it.onTracksChanged(audioOptions, subtitleOptions) }
    }

    private fun createDummyGroup(): androidx.media3.common.Tracks.Group {
        val format = androidx.media3.common.Format.Builder().build()
        val trackGroup = androidx.media3.common.TrackGroup(format)
        return androidx.media3.common.Tracks.Group(
            trackGroup,
            false,
            intArrayOf(androidx.media3.common.C.FORMAT_HANDLED),
            booleanArrayOf(false)
        )
    }
}
