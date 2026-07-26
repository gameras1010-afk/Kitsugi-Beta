package com.kitsugi.animelist.ui.screens.fullscreen.runtime

import android.util.Log
import com.kitsugi.animelist.core.player.engine.PlayerEngine
import com.kitsugi.animelist.ui.screens.fullscreen.components.TrackOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * T1.6: PlayerTrackController
 *
 * Ses ve altyazı parça seçimi işlemlerini PlayerEngine interface üzerinden yönetir.
 * TrackSelectionOverride ile ExoPlayer'a, mpv property ile MPV Engine'e konuşur.
 */
class PlayerTrackController(
    private val scope: CoroutineScope,
    private val getEngine: () -> PlayerEngine?
) {
    private val TAG = "TrackController"

    private val _audioTracks = MutableStateFlow<List<TrackOption>>(emptyList())
    val audioTracks: StateFlow<List<TrackOption>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackOption>>(emptyList())
    val subtitleTracks: StateFlow<List<TrackOption>> = _subtitleTracks.asStateFlow()

    private val _selectedAudioTrack = MutableStateFlow<TrackOption?>(null)
    val selectedAudioTrack: StateFlow<TrackOption?> = _selectedAudioTrack.asStateFlow()

    private val _selectedSubtitleTrack = MutableStateFlow<TrackOption?>(null)
    val selectedSubtitleTrack: StateFlow<TrackOption?> = _selectedSubtitleTrack.asStateFlow()

    /** Engine'den gelen track listesini güncelle (Listener.onTracksChanged callback'te çağrılır) */
    fun updateTracks(audioTracks: List<TrackOption>, subtitleTracks: List<TrackOption>) {
        _audioTracks.value = audioTracks
        _subtitleTracks.value = subtitleTracks
        Log.d(TAG, "Tracks updated: audio=${audioTracks.size}, subs=${subtitleTracks.size}")
    }

    /** Ses parçası seç */
    fun selectAudio(track: TrackOption) {
        runCatching {
            getEngine()?.selectTrack(track)
            _selectedAudioTrack.value = track
            Log.d(TAG, "Audio track selected: ${track.label}")
        }.onFailure { Log.e(TAG, "selectAudio failed", it) }
    }

    /** Altyazı parçası seç */
    fun selectSubtitle(track: TrackOption) {
        runCatching {
            getEngine()?.selectTrack(track)
            _selectedSubtitleTrack.value = track
            Log.d(TAG, "Subtitle track selected: ${track.label}")
        }.onFailure { Log.e(TAG, "selectSubtitle failed", it) }
    }

    /** Altyazıyı devre dışı bırak */
    fun disableSubtitles() {
        runCatching {
            getEngine()?.disableSubtitles()
            _selectedSubtitleTrack.value = null
            Log.d(TAG, "Subtitles disabled")
        }.onFailure { Log.e(TAG, "disableSubtitles failed", it) }
    }

    /** Altyazı gecikmesini ayarla (ms) */
    fun setSubtitleDelay(delayMs: Long) {
        runCatching {
            getEngine()?.setSubtitleDelay(delayMs)
        }.onFailure { Log.e(TAG, "setSubtitleDelay($delayMs) failed", it) }
    }

    /** Ses gecikmesini ayarla (ms) */
    fun setAudioDelay(delayMs: Long) {
        runCatching {
            getEngine()?.setAudioDelay(delayMs)
        }.onFailure { Log.e(TAG, "setAudioDelay($delayMs) failed", it) }
    }
}
