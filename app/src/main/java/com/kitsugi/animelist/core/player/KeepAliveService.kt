package com.kitsugi.animelist.core.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * T1.8 – KeepAliveService
 * Oynatım esnasında uygulamanın arka planda işletim sistemi tarafından sonlandırılmasını engellemek için
 * Foreground Service olarak çalışır.
 */
class KeepAliveService : Service() {

    private val binder = KeepAliveBinder()

    inner class KeepAliveBinder : Binder() {
        fun getService(): KeepAliveService = this@KeepAliveService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "KeepAliveService oluşturuldu")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "KeepAliveService başlatıldı")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        Log.d(TAG, "KeepAliveService durduruldu")
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val iconRes = android.R.drawable.ic_media_play
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kitsugi Player")
            .setContentText("Oynatıcı arka planda aktif tutuluyor...")
            .setSmallIcon(iconRes)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kitsugi Playback Keep-Alive",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Oynatma sırasında uygulamanın arka planda kapanmasını engeller."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "KeepAliveService"
        private const val CHANNEL_ID = "keep_alive_channel"
        private const val NOTIFICATION_ID = 9912

        fun start(context: Context) {
            try {
                val intent = Intent(context, KeepAliveService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "KeepAliveService başlatılamadı", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, KeepAliveService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "KeepAliveService durdurulamadı", e)
            }
        }
    }
}
