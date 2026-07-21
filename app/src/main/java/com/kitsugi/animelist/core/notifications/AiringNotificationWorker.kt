package com.kitsugi.animelist.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kitsugi.animelist.MainActivity
import com.kitsugi.animelist.data.auth.ExternalAuthManager
import com.kitsugi.animelist.data.auth.SimklImportManager
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.local.MediaEntryRepository
import com.kitsugi.animelist.data.local.toDomain
import com.kitsugi.animelist.data.remote.KitsugiAniListNotificationClient
import com.kitsugi.animelist.data.remote.KitsugiAiringCalendarClient
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.model.WatchStatus
import kotlinx.coroutines.flow.first

class AiringNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "AiringWorker"
        private const val CHANNEL_ID = "Kitsugi_airing_channel"
        private const val PREFS_NAME = "Kitsugi_notified_prefs"
        private const val KEY_NOTIFIED_ANILIST = "notified_anilist_ids"
        private const val KEY_NOTIFIED_MAL = "notified_mal_keys"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "AiringNotificationWorker background check started")

        // Load settings
        val settings = SettingsDataStore(context).settingsFlow.first()

        val isAiringEnabled = settings.airingNotificationsEnabled
        val isAniListEnabled = settings.aniListNotificationsEnabled
        val isMalEnabled = settings.malNotificationsEnabled
        val isSimklEnabled = settings.simklNotificationsEnabled

        // If no notifications are enabled, stop immediately
        if (!isAiringEnabled && !isAniListEnabled && !isMalEnabled && !isSimklEnabled) {
            Log.d(TAG, "No notification channels are enabled. Stopping worker.")
            return Result.success()
        }

        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Fetch watching entries from DB
        val db = KitsugiDatabase.getDatabase(context)
        val watchingEntries = try {
            val allEntities = db.mediaEntryDao().getAll()
            allEntities.filter { entity ->
                entity.status == WatchStatus.Watching.name ||
                entity.status == WatchStatus.Repeating.name
            }.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load database watching entries: ${e.message}")
            emptyList()
        }

        // 2. AniList Polling
        if (isAniListEnabled) {
            val aniListToken = ExternalAuthManager.getAniListToken(context)
            if (!aniListToken.isNullOrBlank()) {
                try {
                    val client = KitsugiAniListNotificationClient()
                    val page = client.fetchNotifications(aniListToken, page = 1, perPage = 20)
                    val notifiedIds = sharedPrefs.getStringSet(KEY_NOTIFIED_ANILIST, emptySet())?.toMutableSet() ?: mutableSetOf()

                    for (notif in page.notifications) {
                        val notifIdStr = notif.id.toString()
                        if (!notifiedIds.contains(notifIdStr)) {
                            val title = notif.mediaTitle ?: notif.userName ?: "AniList Bildirimi"
                            val body = when (notif.type) {
                                "AIRING" -> "Bölüm ${notif.episode} artık yayında! 🎬"
                                else -> notif.context ?: "Yeni bir bildiriminiz var"
                            }
                            showNotification(notif.id, title, body)
                            notifiedIds.add(notifIdStr)
                        }
                    }
                    val keptIds = notifiedIds.toList().takeLast(200).toSet()
                    sharedPrefs.edit().putStringSet(KEY_NOTIFIED_ANILIST, keptIds).apply()
                } catch (e: Exception) {
                    Log.e(TAG, "AniList notifications fetch failed: ${e.message}", e)
                }
            }
        }

        // 3. MyAnimeList (Airing Calendar) Polling
        if (isMalEnabled || isAiringEnabled) {
            try {
                val calendarClient = KitsugiAiringCalendarClient()
                val weekSchedule = calendarClient.fetchWeeklySchedule()
                val now = System.currentTimeMillis()
                val allEntries = weekSchedule.values.flatten()

                val matched = allEntries.filter { entry ->
                    watchingEntries.any { me ->
                        (entry.malId != null && me.malId == entry.malId) ||
                        (me.source == "anilist" && me.malId == 100_000_000 + entry.aniListId)
                    }
                }

                val notifiedMalKeys = sharedPrefs.getStringSet(KEY_NOTIFIED_MAL, emptySet())?.toMutableSet() ?: mutableSetOf()

                for (entry in matched) {
                    val triggerMs = entry.airingAt * 1000L
                    // Recently aired (within past 24 hours)
                    if (triggerMs <= now && triggerMs > now - 24 * 60 * 60 * 1000L) {
                        val malKey = "${entry.malId}_${entry.episode}"
                        if (!notifiedMalKeys.contains(malKey)) {
                            val title = entry.title
                            val body = "Bölüm ${entry.episode} artık yayında! 🎬"
                            val malId = entry.malId ?: 0
                            val notifId = (malId * 1000 + entry.episode) and Int.MAX_VALUE
                            showNotification(notifId, title, body)
                            notifiedMalKeys.add(malKey)
                        }
                    }
                }
                val keptMalKeys = notifiedMalKeys.toList().takeLast(200).toSet()
                sharedPrefs.edit().putStringSet(KEY_NOTIFIED_MAL, keptMalKeys).apply()
            } catch (e: Exception) {
                Log.e(TAG, "MAL Airing Calendar fetch failed: ${e.message}", e)
            }
        }

        // 4. Simkl Polling (Watchlist Sync)
        if (isSimklEnabled) {
            val simklToken = ExternalAuthManager.getSimklToken(context)
            if (!simklToken.isNullOrBlank()) {
                try {
                    val importedEntries = SimklImportManager.fetchAllLists(simklToken)
                    val repository = MediaEntryRepository(db.mediaEntryDao())
                    repository.deleteBySource("simkl")
                    repository.insertAll(importedEntries)
                    Log.d(TAG, "Simkl background sync successful: ${importedEntries.size} entries")
                } catch (e: Exception) {
                    Log.e(TAG, "Simkl background sync failed: ${e.message}", e)
                }
            }
        }

        return Result.success()
    }

    private fun showNotification(id: Int, title: String, bodyText: String) {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot show notification: permission missing: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Yayın Bildirimleri",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "İzlediğiniz animelerin yeni bölümleri yayınlandığında bildirim alırsınız."
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
