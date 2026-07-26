package com.kitsugi.animelist.core.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * T2.3 – PlayerMediaSessionHelper
 *
 * Android MediaSessionCompat entegrasyonu:
 * - Kilit ekranı kontrolleri (Play / Pause / Skip Next / Skip Prev)
 * - Sistem bildirim paneli oynatıcı kartı (Media Notification)
 * - PiP modunda RemoteActions butonlarına veri sağlar
 *
 * Kullanım:
 *   val helper = PlayerMediaSessionHelper(activity, "Anime Başlığı")
 *   helper.setMetadata(title, episode, posterBitmap)
 *   helper.updatePlaybackState(isPlaying, positionMs, durationMs)
 *   helper.release()
 *
 * Notlar:
 * - Bu yardımcı kendi başına transport kontrollerini gerçekleştirmez.
 *   Callback'lere (play/pause/skipToNext/skipToPrevious) verilen lambdaları
 *   ViewModel / Activity üzerinden bağla.
 */
class PlayerMediaSessionHelper(
    private val context: Context,
    private var title: String = "",
    private val onPlay: () -> Unit = {},
    private val onPause: () -> Unit = {},
    private val onSkipNext: () -> Unit = {},
    private val onSkipPrevious: () -> Unit = {}
) {

    companion object {
        private const val TAG = "PlayerMediaSessionHelper"
        const val CHANNEL_ID = "Kitsugi_player_channel"
        const val NOTIFICATION_ID = 9001
        const val SESSION_TAG = "KitsugiMediaSession"
    }

    // ── MediaSession ──────────────────────────────────────────────────────────
    val mediaSession: MediaSessionCompat = MediaSessionCompat(context, SESSION_TAG).apply {
        setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                Log.d(TAG, "MediaSession: onPlay")
                this@PlayerMediaSessionHelper.onPlay()
            }
            override fun onPause() {
                Log.d(TAG, "MediaSession: onPause")
                this@PlayerMediaSessionHelper.onPause()
            }
            override fun onSkipToNext() {
                Log.d(TAG, "MediaSession: onSkipToNext")
                this@PlayerMediaSessionHelper.onSkipNext()
            }
            override fun onSkipToPrevious() {
                Log.d(TAG, "MediaSession: onSkipToPrevious")
                this@PlayerMediaSessionHelper.onSkipPrevious()
            }
            override fun onStop() {
                Log.d(TAG, "MediaSession: onStop")
                this@PlayerMediaSessionHelper.onPause()
            }
        })
        isActive = true
    }

    // ── Notification Channel ──────────────────────────────────────────────────
    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kitsugi Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Anime oynatma bildirimleri"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    // ── Metadata ──────────────────────────────────────────────────────────────

    /**
     * Oynatılan içeriğin meta verilerini günceller.
     *
     * @param title       Anime adı (+ bölüm bilgisi)
     * @param subtitle    "Bölüm X" gibi ek açıklama
     * @param artwork     Poster bitmap (null olabilir)
     * @param durationMs  İçerik süresi ms cinsinden
     */
    fun setMetadata(
        title: String,
        subtitle: String = "",
        artwork: Bitmap? = null,
        durationMs: Long = 0L
    ) {
        this.title = title
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, subtitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Kitsugi")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)

        artwork?.let {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, it)
        }

        mediaSession.setMetadata(builder.build())
        Log.d(TAG, "Metadata updated: title=$title duration=${durationMs}ms")
    }

    // ── Playback State ────────────────────────────────────────────────────────

    /**
     * Oynatma durumunu MediaSession'a bildirir.
     * Kilit ekranı ve bildirim kartı buna göre güncellenir.
     */
    fun updatePlaybackState(
        isPlaying: Boolean,
        positionMs: Long = 0L,
        playbackSpeed: Float = 1f,
        hasNext: Boolean = false,
        hasPrevious: Boolean = false
    ) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED

        var actions = PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO

        if (hasNext) actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        if (hasPrevious) actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, positionMs, playbackSpeed)
            .setActions(actions)
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    // ── Notification ──────────────────────────────────────────────────────────

    /**
     * Oynatıcı bildirimini oluşturur ve gösterir.
     * Kilit ekranı ve bildirim çekmecesinde görünür.
     *
     * @param activityClass  Tıklandığında açılacak Activity sınıfı
     * @param isPlaying      Şu an oynatılıyor mu?
     * @param artwork        Poster bitmap
     * @param hasNext        Sonraki bölüm var mı?
     */
    fun showNotification(
        activityClass: Class<*>,
        isPlaying: Boolean,
        artwork: Bitmap? = null,
        hasNext: Boolean = false
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, activityClass).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val token = mediaSession.sessionToken

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setAutoCancel(!isPlaying)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        artwork?.let { builder.setLargeIcon(it) }

        // MediaStyle — kilit ekranı + bildirim paneli controls
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(token)
            .setShowActionsInCompactView(0, 1)  // PlayPause, Next

        builder.setStyle(mediaStyle)

        // Aksiyon butonları
        if (hasNext) {
            val skipNextIntent = PendingIntent.getBroadcast(
                context,
                1,
                Intent("com.kitsugi.animelist.SKIP_NEXT"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_media_next,
                "Sonraki",
                skipNextIntent
            )
        }

        val playPauseIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(if (isPlaying) "com.kitsugi.animelist.PAUSE" else "com.kitsugi.animelist.PLAY"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Duraklat" else "Oynat",
            playPauseIntent
        )

        val notification = builder.build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
        return notification
    }

    /**
     * Bildirimi kaldırır.
     */
    fun cancelNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * MediaSession'ı devre dışı bırakır ve kaynakları serbest bırakır.
     * Activity.onDestroy() içinde çağırılmalı.
     */
    fun release() {
        cancelNotification()
        mediaSession.isActive = false
        mediaSession.release()
        Log.d(TAG, "MediaSession released")
    }
}
