package com.kitsugi.animelist.core.recommendations

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.tvprovider.media.tv.TvContractCompat
import com.kitsugi.animelist.core.notifications.KitsugiAiringNotificationScheduler
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.local.toDomain
import com.kitsugi.animelist.data.remote.KitsugiAiringCalendarClient
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.model.WatchStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * B1.10 - TvChannelBootReceiver: Cihaz yeniden baslatildiginda veya
 * Android TV launcher ACTION_INITIALIZE_PROGRAMS yayini geldiginde
 * kanal sync job'unu planlar.
 *
 * T3.3 - Boot sonrası yayın takvimi alarmlarını yeniden planlar.
 * Android, cihaz yeniden başlatıldığında tüm AlarmManager alarmlarını siler.
 * Bu receiver BOOT_COMPLETED'de yeniden kurma işlemini üstlenir.
 *
 * AndroidManifest'te kayıtlıdır:
 *   - android.intent.action.BOOT_COMPLETED
 *   - androidx.tvprovider.media.tv.action.INITIALIZE_PROGRAMS
 */
class TvChannelBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            TvContractCompat.ACTION_INITIALIZE_PROGRAMS -> {
                // TV kanallarını yeniden planla
                TvChannelRefreshJobService.scheduleImmediate(context)

                // T3.3: Yayın takvimi alarmlarını yeniden planla (boot tüm alarmları siler)
                rescheduleAiringAlarms(context)
            }
        }
    }

    /**
     * Boot sonrası airing notification alarmlarını yeniden planlar.
     * goAsync() ile async DB işlemi yapılır, receiver süresi dolmadan önce tamamlanır.
     */
    private fun rescheduleAiringAlarms(context: Context) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val appCtx = context.applicationContext

                // Bildirim ayarını oku
                val settings = SettingsDataStore(appCtx).settingsFlow.first()
                if (!settings.airingNotificationsEnabled) {
                    android.util.Log.d("BootReceiver", "Yayın bildirimleri kapalı — alarm planlanmadı.")
                    return@launch
                }

                // Kullanıcının izlediği listeleri al
                val db = KitsugiDatabase.getDatabase(appCtx)
                val allEntries = db.mediaEntryDao().getAll().map { it.toDomain() }
                val watchingEntries = allEntries.filter { it.status == WatchStatus.Watching }

                if (watchingEntries.isEmpty()) {
                    android.util.Log.d("BootReceiver", "İzleme listesi boş — alarm planlanmadı.")
                    return@launch
                }

                // Bu haftanın yayın takvimine göre alarmları kur
                val client = KitsugiAiringCalendarClient()
                val weekSchedule = client.fetchWeeklySchedule()

                KitsugiAiringNotificationScheduler.createNotificationChannel(appCtx)
                KitsugiAiringNotificationScheduler.schedule(
                    context = appCtx,
                    weekSchedule = weekSchedule,
                    watchingEntries = watchingEntries,
                    cancelPrevious = false // Boot'ta zaten boş, iptal gerekmez
                )

                android.util.Log.d("BootReceiver",
                    "Boot sonrası alarm yeniden planlandı: ${watchingEntries.size} izlenen anime için.")
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "Boot airing alarm hatası: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
