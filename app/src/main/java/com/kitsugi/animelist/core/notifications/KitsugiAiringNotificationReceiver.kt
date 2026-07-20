package com.kitsugi.animelist.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Yayın bildirimi gösteren BroadcastReceiver.
 * KitsugiAiringNotificationScheduler tarafından AlarmManager ile tetiklenir.
 */
class KitsugiAiringNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title   = intent.getStringExtra(EXTRA_TITLE)   ?: return
        val episode = intent.getIntExtra(EXTRA_EPISODE, 0)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)

        val bodyText = "Bölüm $episode artık yayında! 🎬"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS izni verilmemişse sessizce geç
            android.util.Log.w(TAG, "Bildirim gönderilemedi (izin yok): ${e.message}")
        }
    }

    companion object {
        const val CHANNEL_ID     = "Kitsugi_airing_channel"
        const val CHANNEL_NAME   = "Yayın Bildirimleri"
        const val EXTRA_TITLE    = "airing_title"
        const val EXTRA_EPISODE  = "airing_episode"
        const val EXTRA_NOTIF_ID = "airing_notif_id"
        private const val TAG    = "KitsugiAiringReceiver"
    }
}
