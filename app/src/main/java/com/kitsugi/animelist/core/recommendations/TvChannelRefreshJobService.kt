package com.kitsugi.animelist.core.recommendations

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "TvChannelSync"
private const val JOB_ID_PERIODIC = 11_010
private const val JOB_ID_IMMEDIATE = 11_011
// JobScheduler minimum 15 dakika
private const val PERIODIC_INTERVAL_MS = 15 * 60_000L

/**
 * B1.10 - TvChannelRefreshJobService: Uygulama on planda olmadigi zamanlarda
 * "Continue Watching" kanalini arka planda taze tutar.
 *
 * Hilt'in direkt @AndroidEntryPoint destegi yok (JobService icin);
 * bu nedenle Hilt EntryPoint pattern'i kullanilir.
 *
 * Periyodik job (15dk): schedulePeriodic() ile kaydedilir; TvChannelSyncService.start()'ta cagrilir.
 * Anlik job (5sn): scheduleImmediate() ile boot/initialize_programs sonrasi tetiklenir.
 */
class TvChannelRefreshJobService : JobService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TvChannelJobEntryPoint {
        fun channelSyncService(): TvChannelSyncService
        fun channelManager(): AndroidTvChannelManager
    }

    private var jobScope: CoroutineScope? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext, TvChannelJobEntryPoint::class.java
        )
        val syncService = entryPoint.channelSyncService()
        val manager = entryPoint.channelManager()

        if (!manager.isSupported()) {
            jobFinished(params, false)
            return false
        }

        jobScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        jobScope!!.launch {
            try {
                syncService.reconcileFromDatabase()
                Log.d(TAG, "Background job: reconciled from database")
            } catch (e: Exception) {
                Log.w(TAG, "Background job reconcile failed", e)
            } finally {
                jobFinished(params, false)
            }
        }
        return true // async
    }

    override fun onStopJob(params: JobParameters): Boolean {
        jobScope?.cancel()
        return true // reschedule on stop
    }

    companion object {
        fun schedulePeriodic(context: Context) {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            if (scheduler.allPendingJobs.any { it.id == JOB_ID_PERIODIC }) return
            val job = JobInfo.Builder(
                JOB_ID_PERIODIC,
                ComponentName(context, TvChannelRefreshJobService::class.java)
            )
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIODIC_INTERVAL_MS)
                .setPersisted(true)
                .build()
            scheduler.schedule(job)
            Log.d(TAG, "Scheduled periodic TV channel refresh every ${PERIODIC_INTERVAL_MS / 60_000}min")
        }

        fun scheduleImmediate(context: Context) {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val job = JobInfo.Builder(
                JOB_ID_IMMEDIATE,
                ComponentName(context, TvChannelRefreshJobService::class.java)
            )
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setOverrideDeadline(5_000)
                .build()
            scheduler.schedule(job)
            Log.d(TAG, "Scheduled immediate TV channel refresh")
        }
    }
}
