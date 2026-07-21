package com.kitsugi.animelist.core.notifications

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"
    private const val WORK_NAME = "airing_notifications_work"

    /**
     * Schedules a unique periodic work request with constraints.
     */
    fun schedule(context: Context, intervalMinutes: Int) {
        val workManager = WorkManager.getInstance(context)

        // Constraints: require internet connectivity to fetch updates from external APIs
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // WorkManager minimum periodic interval is 15 minutes.
        val periodicInterval = intervalMinutes.coerceAtLeast(15).toLong()

        val workRequest = PeriodicWorkRequestBuilder<AiringNotificationWorker>(
            periodicInterval,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        Log.d(TAG, "Scheduling periodic airing notification work: interval = $periodicInterval mins")

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Update keeps it current with the new interval
            workRequest
        )
    }

    /**
     * Cancels the scheduled work request.
     */
    fun cancel(context: Context) {
        Log.d(TAG, "Canceling periodic airing notification work")
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_NAME)
    }
}
