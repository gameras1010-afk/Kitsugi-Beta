package com.kitsugi.animelist.core.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kitsugi.animelist.R
import com.kitsugi.animelist.data.model.AnimeDownload
import com.kitsugi.animelist.data.local.AnimeDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AnimeDownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var downloadJob: Job? = null
    private var activeDownloadJob: Job? = null
    private var currentDownloadingAnimeId: String? = null
    private var currentDownloadingEpisode: Int? = null
    private lateinit var downloader: AnimeDownloader
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val CHANNEL_ID = "anime_downloads_channel"
        private const val NOTIFICATION_ID = 10002
    }

    override fun onCreate() {
        super.onCreate()
        downloader = AnimeDownloader(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        serviceScope.launch {
            AnimeDownloadManager.downloads.collect { list ->
                val activeId = currentDownloadingAnimeId
                val activeEp = currentDownloadingEpisode
                if (activeId != null && activeEp != null) {
                    val stillExistsAndDownloading = list.any {
                        it.animeId == activeId &&
                        it.episode == activeEp &&
                        (it.status == AnimeDownload.Status.DOWNLOADING || it.status == AnimeDownload.Status.QUEUE)
                    }
                    if (!stillExistsAndDownloading) {
                        activeDownloadJob?.cancel()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("İndirme sırası hazırlanıyor...", 0, true))
        
        if (downloadJob == null || downloadJob?.isCompleted == true) {
            startLoop()
        }
        
        return START_NOT_STICKY
    }

    private fun startLoop() {
        downloadJob = serviceScope.launch {
            while (isActive) {
                val next = AnimeDownloadManager.getNextDownload()
                if (next == null) {
                    // No more downloads, stop foreground service
                    delay(2000L) // Wait a brief moment
                    if (AnimeDownloadManager.getNextDownload() == null) {
                        stopSelf()
                        break
                    }
                } else {
                    currentDownloadingAnimeId = next.animeId
                    currentDownloadingEpisode = next.episode

                    val activeJob = launch {
                        try {
                            var lastNotificationTime = 0L
                            downloader.download(
                                download = next,
                                onProgress = { progress, size, duration, segmentsDownloaded ->
                                    AnimeDownloadManager.updateProgress(
                                        animeId = next.animeId,
                                        episode = next.episode,
                                        progress = progress,
                                        downloadedBytes = size,
                                        totalBytes = duration,
                                        downloadedSegments = segmentsDownloaded
                                    )
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastNotificationTime >= 1000L || progress == 100) {
                                        lastNotificationTime = currentTime
                                        val text = "${next.animeTitle} - Bölüm ${next.episode} (%$progress)"
                                        notificationManager.notify(NOTIFICATION_ID, buildNotification(text, progress, false))
                                    }
                                },
                                onStatusChanged = { status, localPath ->
                                    AnimeDownloadManager.updateStatus(
                                        animeId = next.animeId,
                                        episode = next.episode,
                                        status = status,
                                        localPath = localPath
                                    )
                                    if (status == AnimeDownload.Status.COMPLETED) {
                                        val text = "${next.animeTitle} - Bölüm ${next.episode} indirildi"
                                        showCompletedNotification(next, text)
                                    } else if (status == AnimeDownload.Status.ERROR) {
                                        val text = "${next.animeTitle} - Bölüm ${next.episode} indirilirken hata oluştu"
                                        showCompletedNotification(next, text)
                                    }
                                }
                            )
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            if (isActive) {
                                AnimeDownloadManager.updateStatus(
                                    animeId = next.animeId,
                                    episode = next.episode,
                                    status = AnimeDownload.Status.ERROR
                                )
                            }
                        }
                    }

                    activeDownloadJob = activeJob
                    try {
                        activeJob.join()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        currentDownloadingAnimeId = null
                        currentDownloadingEpisode = null
                        activeDownloadJob = null
                    }
                }
                delay(1000L)
            }
        }
    }

    private fun buildNotification(text: String, progress: Int, indeterminate: Boolean): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Anime İndiriliyor")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showCompletedNotification(download: AnimeDownload, text: String) {
        val completeNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("İndirme Tamamlandı")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        val notificationId = download.animeId.hashCode() + download.episode
        notificationManager.notify(notificationId, completeNotification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Anime İndirmeleri",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Anime indirme ilerlemesini gösterir."
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
