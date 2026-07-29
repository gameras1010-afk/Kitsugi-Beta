package com.kitsugi.animelist.data.local

import android.content.Context
import android.content.Intent
import android.os.Build
import com.kitsugi.animelist.data.model.AnimeDownload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object AnimeDownloadManager {
    private lateinit var store: AnimeDownloadStore
    private val _downloads = MutableStateFlow<List<AnimeDownload>>(emptyList())
    val downloads: StateFlow<List<AnimeDownload>> = _downloads.asStateFlow()

    fun init(context: Context) {
        store = AnimeDownloadStore(context.applicationContext)
        _downloads.value = store.getDownloads()
    }

    private fun saveAndEmit(list: List<AnimeDownload>) {
        _downloads.value = list
        store.saveDownloads(list)
    }

    fun addDownload(
        context: Context,
        animeId: String,
        animeTitle: String,
        posterUrl: String?,
        episode: Int,
        season: Int,
        url: String,
        quality: String,
        requestHeaders: Map<String, String> = emptyMap()
    ) {
        val current = _downloads.value.toMutableList()
        val exists = current.any { it.animeId == animeId && it.episode == episode }
        if (exists) return

        val newDownload = AnimeDownload(
            animeId = animeId,
            animeTitle = animeTitle,
            posterUrl = posterUrl,
            episode = episode,
            season = season,
            url = url,
            quality = quality,
            requestHeaders = requestHeaders,
            status = AnimeDownload.Status.QUEUE
        )
        current.add(newDownload)
        saveAndEmit(current)
        startService(context)
    }

    fun pauseDownload(animeId: String, episode: Int) {
        val current = _downloads.value.map {
            if (it.animeId == animeId && it.episode == episode) {
                it.copy(status = AnimeDownload.Status.PAUSED)
            } else it
        }
        saveAndEmit(current)
    }

    fun resumeDownload(context: Context, animeId: String, episode: Int) {
        val current = _downloads.value.map {
            if (it.animeId == animeId && it.episode == episode) {
                it.copy(status = AnimeDownload.Status.QUEUE)
            } else it
        }
        saveAndEmit(current)
        startService(context)
    }

    fun deleteDownload(context: Context, animeId: String, episode: Int) {
        val toDelete = _downloads.value.find { it.animeId == animeId && it.episode == episode }
        val current = _downloads.value.filterNot { it.animeId == animeId && it.episode == episode }
        saveAndEmit(current)

        toDelete?.localPath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateProgress(animeId: String, episode: Int, progress: Int, downloadedBytes: Long, totalBytes: Long) {
        val current = _downloads.value.map {
            if (it.animeId == animeId && it.episode == episode) {
                it.copy(
                    progress = progress,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes
                )
            } else it
        }
        _downloads.value = current
    }

    fun updateStatus(animeId: String, episode: Int, status: AnimeDownload.Status, localPath: String? = null) {
        val current = _downloads.value.map {
            if (it.animeId == animeId && it.episode == episode) {
                it.copy(
                    status = status,
                    localPath = localPath ?: it.localPath
                )
            } else it
        }
        saveAndEmit(current)
    }

    fun getNextDownload(): AnimeDownload? {
        return _downloads.value.firstOrNull { it.status == AnimeDownload.Status.QUEUE }
    }

    fun startService(context: Context) {
        val serviceIntent = Intent(context, com.kitsugi.animelist.core.download.AnimeDownloadService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
