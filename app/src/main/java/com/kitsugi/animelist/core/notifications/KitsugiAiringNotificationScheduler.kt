package com.kitsugi.animelist.core.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kitsugi.animelist.data.remote.AiringEntry
import com.kitsugi.animelist.model.MediaEntry

/**
 * AlarmManager tabanlı yayın bildirim planlayıcı.
 *
 * Kullanım:
 * ```kotlin
 * KitsugiAiringNotificationScheduler.schedule(context, weekSchedule, watchingEntries)
 * ```
 *
 * Her anime için ayrı bir `setExactAndAllowWhileIdle` alarmı kurulur.
 * Bildirim zamanı gelince [KitsugiAiringNotificationReceiver] devreye girer.
 */
object KitsugiAiringNotificationScheduler {

    /** Bildirim kanalını oluşturur (API 26+). Uygulama başlangıcında çağrılabilir. */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                KitsugiAiringNotificationReceiver.CHANNEL_ID,
                KitsugiAiringNotificationReceiver.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "İzlediğiniz animelerin yeni bölümleri yayınlandığında bildirim alırsınız."
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Kullanıcının listesindeki animeler için bu haftanın alarmlarını kurar.
     *
     * @param weekSchedule  Gün → yayın listesi (KitsugiAiringCalendarClient'tan gelir).
     * @param watchingEntries Kullanıcının "İzliyorum" statüsündeki girişleri.
     * @param cancelPrevious Önceki tüm alarmları iptal et (false = mevcut alarmlar korunur).
     */
    fun schedule(
        context: Context,
        weekSchedule: Map<Int, List<AiringEntry>>,
        watchingEntries: List<MediaEntry>,
        cancelPrevious: Boolean = true
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (cancelPrevious) {
            cancelAll(context)
        }

        val now = System.currentTimeMillis()
        val allEntries = weekSchedule.values.flatten()

        // Sadece kullanıcının izlediği animeleri filtrele
        val matched = allEntries.filter { entry ->
            watchingEntries.any { me ->
                (entry.malId != null && me.malId == entry.malId) ||
                        (me.source == "anilist" && me.malId == 100_000_000 + entry.aniListId)
            }
        }

        for (entry in matched) {
            val triggerMs = entry.airingAt * 1000L
            // Geçmişte kalan yayınlar için alarm kurma
            if (triggerMs <= now) continue

            val notifId = (entry.aniListId * 1000 + entry.episode) and Int.MAX_VALUE

            val intent = Intent(context, KitsugiAiringNotificationReceiver::class.java).apply {
                putExtra(KitsugiAiringNotificationReceiver.EXTRA_TITLE, entry.title)
                putExtra(KitsugiAiringNotificationReceiver.EXTRA_EPISODE, entry.episode)
                putExtra(KitsugiAiringNotificationReceiver.EXTRA_NOTIF_ID, notifId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMs,
                            pendingIntent
                        )
                    } else {
                        // Exact alarm izni yoksa yaklaşık alarm kur
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMs,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMs,
                        pendingIntent
                    )
                }
                android.util.Log.d(
                    "AiringScheduler",
                    "Alarm kuruldu: ${entry.title} Bölüm ${entry.episode} @ ${entry.formattedTime()}"
                )
            } catch (e: SecurityException) {
                android.util.Log.w("AiringScheduler", "Alarm kurulamadı: ${e.message}")
            }
        }
    }

    /** Tüm yayın alarmlarını iptal eder. */
    fun cancelAll(context: Context) {
        // Alarmları takip etmek için SharedPreferences'ta saklanan ID'leri temizle
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIds = prefs.getStringSet(KEY_ALARM_IDS, emptySet()) ?: emptySet()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (idStr in savedIds) {
            val id = idStr.toIntOrNull() ?: continue
            val intent = Intent(context, KitsugiAiringNotificationReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: continue
            alarmManager.cancel(pi)
        }
        prefs.edit().remove(KEY_ALARM_IDS).apply()
    }

    private const val PREFS_NAME   = "Kitsugi_airing_prefs"
    private const val KEY_ALARM_IDS = "alarm_ids"
}
