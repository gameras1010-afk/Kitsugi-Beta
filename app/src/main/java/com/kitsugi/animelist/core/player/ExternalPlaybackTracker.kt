package com.kitsugi.animelist.core.player

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * T1.8: ExternalPlaybackTracker
 *
 * Harici oynatıcıda (VLC, MX Player, JustPlayer vb.) izleme geçmişini saklar.
 * ExternalPlayerLauncher → launch → return → buradaki pozisyonu kaydet → progress sync.
 *
 * SharedPreferences tabanlı (hafif, Room gerektirmiyor).
 */
object ExternalPlaybackTracker {

    private const val TAG = "ExternalPlaybackTracker"
    private const val PREFS_NAME = "kitsugi_external_playback"
    private const val KEY_LAST_POSITION_MS = "last_position_ms"
    private const val KEY_LAST_MEDIA_ID = "last_media_id"
    private const val KEY_LAST_EPISODE = "last_episode"
    private const val KEY_LAST_PLAYER_PKG = "last_player_pkg"
    private const val KEY_LAST_TIMESTAMP = "last_timestamp"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Harici oynatıcıya gönderilmeden önce çağrılır.
     * Hangi medya + bölüm için harici oynatıcı açıldığını kaydeder.
     */
    fun recordLaunch(
        context: Context,
        mediaId: String,
        episode: Int,
        playerPackage: String
    ) {
        prefs(context).edit()
            .putString(KEY_LAST_MEDIA_ID, mediaId)
            .putInt(KEY_LAST_EPISODE, episode)
            .putString(KEY_LAST_PLAYER_PKG, playerPackage)
            .putLong(KEY_LAST_TIMESTAMP, System.currentTimeMillis())
            .putLong(KEY_LAST_POSITION_MS, 0L)
            .apply()
        Log.d(TAG, "Launched external: mediaId=$mediaId ep=$episode pkg=$playerPackage")
    }

    /**
     * Harici oynatıcıdan dönüşte, oynatıcının döndürdüğü pozisyonu (ms) kaydet.
     */
    fun recordReturn(context: Context, positionMs: Long) {
        val mediaId = prefs(context).getString(KEY_LAST_MEDIA_ID, null) ?: return
        prefs(context).edit()
            .putLong(KEY_LAST_POSITION_MS, positionMs)
            .apply()
        Log.d(TAG, "Returned from external: mediaId=$mediaId pos=${positionMs}ms")
    }

    /**
     * Son izleme pozisyonunu döndürür.
     * @return Pair(mediaId, positionMs) veya null
     */
    fun getLastPosition(context: Context): Pair<String, Long>? {
        val mediaId = prefs(context).getString(KEY_LAST_MEDIA_ID, null) ?: return null
        val positionMs = prefs(context).getLong(KEY_LAST_POSITION_MS, 0L)
        if (positionMs <= 0L) return null
        return mediaId to positionMs
    }

    /**
     * Son harici oynatma oturumunun tam bilgisini döndürür.
     */
    data class Session(
        val mediaId: String,
        val episode: Int,
        val positionMs: Long,
        val playerPackage: String,
        val timestampMs: Long
    )

    fun getLastSession(context: Context): Session? {
        val p = prefs(context)
        val mediaId = p.getString(KEY_LAST_MEDIA_ID, null) ?: return null
        return Session(
            mediaId = mediaId,
            episode = p.getInt(KEY_LAST_EPISODE, 0),
            positionMs = p.getLong(KEY_LAST_POSITION_MS, 0L),
            playerPackage = p.getString(KEY_LAST_PLAYER_PKG, "") ?: "",
            timestampMs = p.getLong(KEY_LAST_TIMESTAMP, 0L)
        )
    }

    /** Kaydedilen oturumu temizler */
    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
