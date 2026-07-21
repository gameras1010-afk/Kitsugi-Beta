package com.kitsugi.animelist.core.recommendations

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.tvprovider.media.tv.TvContractCompat
import com.kitsugi.animelist.data.settings.SettingsDataStore
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
                if (settings.airingNotificationsEnabled || settings.aniListNotificationsEnabled || settings.malNotificationsEnabled || settings.simklNotificationsEnabled) {
                    com.kitsugi.animelist.core.notifications.NotificationScheduler.schedule(appCtx, settings.notificationInterval)
                    android.util.Log.d("BootReceiver", "Boot sonrası WorkManager bildirimleri planlandı.")
                } else {
                    com.kitsugi.animelist.core.notifications.NotificationScheduler.cancel(appCtx)
                    android.util.Log.d("BootReceiver", "Bildirimler kapalı — planlama iptal edildi.")
                }
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "Boot airing alarm hatası: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
