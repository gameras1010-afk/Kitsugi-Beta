package com.kitsugi.animelist.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.data.remote.KitsugiAiringCalendarClient
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.local.MediaEntryEntity
import com.kitsugi.animelist.data.local.toDomain
import com.kitsugi.animelist.model.WatchStatus

/**
 * T3.3 – AiringNotificationWorker
 *
 * AlarmManager tabanlı periyodik yayın bildirimi koordinatörü.
 *
 * Görevi: Kullanıcının "İzliyorum" (ve "Yeniden İzleniyor") listesindeki animeler için
 * bu haftanın yayın alarmlarını planlar ya da iptal eder.
 *
 * Tetikleyici yollar:
 *  1. [KitsugiApplication] başlangıcında `scheduleIfEnabled(context)` çağrısı.
 *  2. [KitsugiAiringAlarmBootReceiver] üzerinden cihaz yeniden başlatıldığında.
 *
 * Tasarım notu: Projeye WorkManager dahil olmadığından AlarmManager kullanılır.
 * Yayın zamanı gelince [KitsugiAiringNotificationReceiver] devreye girer.
 */
object AiringNotificationWorker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Ayarlar açıksa haftalık yayın alarmlarını kurar, kapalıysa iptal eder.
     *
     * @param context Application context
     */
    fun scheduleIfEnabled(context: Context) {
        scope.launch {
            try {
                val settings = SettingsDataStore(context.applicationContext)
                    .settingsFlow
                    .first()

                if (!settings.airingNotificationsEnabled) {
                    KitsugiAiringNotificationScheduler.cancelAll(context)
                    android.util.Log.d("AiringWorker", "Bildirimler devre dışı — alarmlar iptal edildi.")
                    return@launch
                }

                // Yayın takvimini çek
                val calendarClient = KitsugiAiringCalendarClient()
                val weekSchedule = calendarClient.fetchWeeklySchedule()

                // Kullanıcının izlediği animeleri DB'den çek
                val db = KitsugiDatabase.getDatabase(context.applicationContext)
                val allEntities: List<MediaEntryEntity> = db.mediaEntryDao().getAll()
                val watchingEntries = allEntities
                    .filter { entity: MediaEntryEntity ->
                        entity.status == WatchStatus.Watching.name ||
                        entity.status == WatchStatus.Repeating.name
                    }
                    .map { entity: MediaEntryEntity -> entity.toDomain() }

                // Bildirimleri planla
                KitsugiAiringNotificationScheduler.schedule(
                    context = context,
                    weekSchedule = weekSchedule,
                    watchingEntries = watchingEntries,
                    cancelPrevious = true
                )

                android.util.Log.d(
                    "AiringWorker",
                    "Alarmlar kuruldu: ${weekSchedule.values.sumOf { list -> list.size }} yayın, " +
                    "${watchingEntries.size} izlenen anime"
                )

            } catch (e: Exception) {
                android.util.Log.e("AiringWorker", "Yayın bildirimi planlanırken hata: ${e.message}", e)
            }
        }
    }

    /**
     * Bildirimleri hemen iptal eder.
     * Kullanıcı ayarlardan bildirimleri kapattığında çağrılır.
     */
    fun cancel(context: Context) {
        KitsugiAiringNotificationScheduler.cancelAll(context)
        android.util.Log.d("AiringWorker", "Tüm yayın bildirimleri iptal edildi.")
    }
}

/**
 * Cihaz yeniden başlatıldığında ve haftalık alarm tetiklendiğinde çalışır.
 * AlarmManager alarmlarını yeniden kurar.
 *
 * AndroidManifest.xml'de BOOT_COMPLETED alıcısı olarak kayıtlı olmalı.
 */
class KitsugiAiringAlarmBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == WEEKLY_REFRESH_ACTION) {
            android.util.Log.d("AiringWorker", "Boot/refresh alındı ($action) — yayın alarmları yeniden kuruluyor.")
            AiringNotificationWorker.scheduleIfEnabled(context)
        }
    }

    companion object {
        const val WEEKLY_REFRESH_ACTION = "com.kitsugi.animelist.AIRING_WEEKLY_REFRESH"
    }
}
