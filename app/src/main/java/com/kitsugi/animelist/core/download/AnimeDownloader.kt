package com.kitsugi.animelist.core.download

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.documentfile.provider.DocumentFile
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.Level
import com.arthenica.ffmpegkit.LogCallback
import com.arthenica.ffmpegkit.StatisticsCallback
import com.kitsugi.animelist.core.player.OfflinePlaybackHelper
import com.kitsugi.animelist.data.model.AnimeDownload
import com.kitsugi.animelist.data.local.AnimeDownloadManager
import com.kitsugi.animelist.data.settings.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AnimeDownloader(private val context: Context) {

    suspend fun download(
        download: AnimeDownload,
        onProgress: (Int, Long, Long) -> Unit,
        onStatusChanged: (AnimeDownload.Status, String?) -> Unit
    ) {
        val settings = SettingsDataStore(context).settingsFlow.first()
        val downloaderPref = settings.downloaderPreference

        if (downloaderPref == "INTERNAL") {
            downloadInternal(download, onProgress, onStatusChanged)
        } else {
            downloadExternal(download, downloaderPref, onStatusChanged)
        }
    }

    private suspend fun downloadInternal(
        download: AnimeDownload,
        onProgress: (Int, Long, Long) -> Unit,
        onStatusChanged: (AnimeDownload.Status, String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        onStatusChanged(AnimeDownload.Status.DOWNLOADING, null)

        val mediaId = "${download.animeId}_ep${download.episode}"
        val rootDir = OfflinePlaybackHelper.getDownloadsDir(context)
        val destDir = File(rootDir, mediaId).also { it.mkdirs() }
        val localFile = File(destDir, "video.mp4")

        // Build FFmpeg -headers string from requestHeaders (same as player uses them)
        val headersArg = if (download.requestHeaders.isNotEmpty()) {
            val headerLines = download.requestHeaders.entries.joinToString("\r\n") { (k, v) -> "$k: $v" }
            "-headers \"$headerLines\r\n\""
        } else ""

        try {
            // Get duration using ffprobe
            val ffprobeCommand = "$headersArg -v quiet -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 \"${download.url}\""
            val probeSession = FFmpegKit.execute(ffprobeCommand)
            val output = probeSession.output
            val durationSec = output?.trim()?.toDoubleOrNull()?.toLong() ?: 0L
            val durationMs = durationSec * 1000L

            // Run FFmpeg to mux/download (with headers for Cloudflare-protected streams)
            val ffmpegCommand = "-y $headersArg -i \"${download.url}\" -c copy -bsf:a aac_adtstoasc \"${localFile.absolutePath}\""

            suspendCancellableCoroutine<Unit> { continuation ->
                val session = FFmpegKit.executeAsync(
                    ffmpegCommand,
                    { completedSession ->
                        if (completedSession.returnCode.isValueSuccess) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(Exception("FFmpeg exited with error: ${completedSession.failStackTrace}"))
                        }
                    },
                    { /* LogCallback */ },
                    { stats ->
                        val timeMs = stats.time
                        val progress = if (durationMs > 0) {
                            (100 * timeMs / durationMs).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        onProgress(progress, stats.size, durationMs)
                    }
                )

                continuation.invokeOnCancellation {
                    session.cancel()
                }
            }

            // Create metadata.json for OfflinePlaybackHelper
            val metaFile = File(destDir, "metadata.json")
            val metaJson = """
                {
                  "mediaId": "$mediaId",
                  "title": "${download.animeTitle} - Bölüm ${download.episode}",
                  "durationMs": $durationMs,
                  "downloadedAtMs": ${System.currentTimeMillis()}
                }
            """.trimIndent()
            metaFile.writeText(metaJson)

            // Copy to user custom folder if set
            val settings = SettingsDataStore(context).settingsFlow.first()
            val customDirUriStr = settings.videoDownloadUri
            if (customDirUriStr.isNotBlank()) {
                try {
                    val customDirDoc = DocumentFile.fromTreeUri(context, Uri.parse(customDirUriStr))
                    if (customDirDoc != null && customDirDoc.exists()) {
                        val cleanTitle = download.animeTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                        val outFilename = "${cleanTitle}_Bölüm_${download.episode}.mp4"

                        // Delete existing duplicate
                        customDirDoc.findFile(outFilename)?.delete()

                        val newFileDoc = customDirDoc.createFile("video/mp4", outFilename)
                        newFileDoc?.uri?.let { destUri ->
                            context.contentResolver.openOutputStream(destUri)?.use { outStream ->
                                localFile.inputStream().use { inStream ->
                                    inStream.copyTo(outStream)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AnimeDownloader", "Failed to copy video to custom SAF directory", e)
                }
            }

            onStatusChanged(AnimeDownload.Status.COMPLETED, localFile.absolutePath)

        } catch (e: Exception) {
            android.util.Log.e("AnimeDownloader", "Download failed", e)
            localFile.delete()
            onStatusChanged(AnimeDownload.Status.ERROR, null)
        }
    }

    private fun downloadExternal(
        download: AnimeDownload,
        downloaderPref: String,
        onStatusChanged: (AnimeDownload.Status, String?) -> Unit
    ) {
        val pm = context.packageManager
        val cleanTitle = download.animeTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val filename = "${cleanTitle}_Bölüm_${download.episode}.mp4"

        try {
            val intent: Intent
            when (downloaderPref) {
                "EXTERNAL_1DM" -> {
                    val pkgName = if (isPackageInstalled(pm, "idm.internet.download.manager.plus")) {
                        "idm.internet.download.manager.plus"
                    } else {
                        "idm.internet.download.manager"
                    }
                    if (!isPackageInstalled(pm, pkgName)) {
                        throw Exception("1DM not installed")
                    }
                    intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(download.url)
                        setPackage(pkgName)
                        putExtra("extra_filename", filename)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                "EXTERNAL_ADM" -> {
                    val pkgName = if (isPackageInstalled(pm, "com.dv.adm.pay")) {
                        "com.dv.adm.pay"
                    } else {
                        "com.dv.adm"
                    }
                    if (!isPackageInstalled(pm, pkgName)) {
                        throw Exception("ADM not installed")
                    }
                    intent = Intent(Intent.ACTION_VIEW).apply {
                        component = ComponentName(pkgName, "$pkgName.AEditor")
                        putExtra("com.dv.get.ACTION_LIST_ADD", "${download.url}<info>$filename")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                else -> {
                    // System Browser / Default
                    intent = Intent(Intent.ACTION_VIEW).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        setDataAndType(Uri.parse(download.url), "video/*")
                        putExtra("extra_filename", filename)
                    }
                }
            }
            context.startActivity(intent)
            onStatusChanged(AnimeDownload.Status.COMPLETED, null)
        } catch (e: Exception) {
            android.util.Log.e("AnimeDownloader", "Failed to launch external downloader", e)
            onStatusChanged(AnimeDownload.Status.ERROR, null)
        }
    }

    private fun isPackageInstalled(pm: android.content.pm.PackageManager, pkgName: String): Boolean {
        return try {
            pm.getPackageInfo(pkgName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
