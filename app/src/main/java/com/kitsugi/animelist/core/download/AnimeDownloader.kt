package com.kitsugi.animelist.core.download

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.documentfile.provider.DocumentFile
import com.kitsugi.animelist.core.network.KitsugiHttpClient
import com.kitsugi.animelist.core.player.OfflinePlaybackHelper
import com.kitsugi.animelist.data.model.AnimeDownload
import com.kitsugi.animelist.data.settings.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import okhttp3.Request
import java.io.File
import kotlin.coroutines.coroutineContext

class AnimeDownloader(private val context: Context) {

    suspend fun download(
        download: AnimeDownload,
        onProgress: (Int, Long, Long, Int) -> Unit,
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
        onProgress: (Int, Long, Long, Int) -> Unit,
        onStatusChanged: (AnimeDownload.Status, String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        onStatusChanged(AnimeDownload.Status.DOWNLOADING, null)

        val settings = SettingsDataStore(context).settingsFlow.first()
        val mediaId = "${download.animeId}_ep${download.episode}"
        val rootDir = OfflinePlaybackHelper.getDownloadsDir(context)
        val destDir = File(rootDir, mediaId).also { it.mkdirs() }
        val localFile = File(destDir, "video.mp4")

        try {
            val isHls = download.url.contains(".m3u8", ignoreCase = true)
            if (isHls) {
                // Fetch the main playlist
                val playlistContent = getUrlContent(download.url, download.requestHeaders)
                
                // Parse lines
                val lines = playlistContent.lines()
                val isMasterPlaylist = lines.any { it.startsWith("#EXT-X-STREAM-INF") }
                
                var mediaPlaylistUrl = download.url
                var mediaPlaylistContent = playlistContent
                
                if (isMasterPlaylist) {
                    var bestUrl: String? = null
                    var maxBandwidth = 0L
                    var i = 0
                    while (i < lines.size) {
                        val line = lines[i].trim()
                        if (line.startsWith("#EXT-X-STREAM-INF")) {
                            val bandwidthRegex = Regex("""BANDWIDTH=(\d+)""")
                            val match = bandwidthRegex.find(line)
                            val bandwidth = match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                            
                            var nextLineIndex = i + 1
                            while (nextLineIndex < lines.size && lines[nextLineIndex].trim().startsWith("#")) {
                                nextLineIndex++
                            }
                            if (nextLineIndex < lines.size) {
                                val subUrl = lines[nextLineIndex].trim()
                                if (subUrl.isNotEmpty()) {
                                    if (bandwidth > maxBandwidth || bestUrl == null) {
                                        maxBandwidth = bandwidth
                                        bestUrl = subUrl
                                    }
                                }
                            }
                        }
                        i++
                    }
                    if (bestUrl != null) {
                        mediaPlaylistUrl = resolveUrl(download.url, bestUrl)
                        mediaPlaylistContent = getUrlContent(mediaPlaylistUrl, download.requestHeaders)
                    }
                }
                
                // Parse segments
                val segments = mutableListOf<String>()
                var initSegmentUrl: String? = null
                
                mediaPlaylistContent.lines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) return@forEach
                    if (trimmed.startsWith("#")) {
                        if (trimmed.startsWith("#EXT-X-MAP:")) {
                            val uriRegex = Regex("""URI=["']([^"']+)["']""")
                            val match = uriRegex.find(trimmed)
                            val uri = match?.groupValues?.get(1)
                            if (uri != null) {
                                initSegmentUrl = resolveUrl(mediaPlaylistUrl, uri)
                            }
                        }
                        return@forEach
                    }
                    segments.add(resolveUrl(mediaPlaylistUrl, trimmed))
                }
                
                if (segments.isEmpty()) {
                    throw Exception("No segments found in HLS playlist")
                }
                
                val hasFile = localFile.exists() && localFile.length() > 0
                val startSegmentIndex = if (hasFile) download.downloadedSegments else 0
                val appendMode = startSegmentIndex > 0 && hasFile
                
                java.io.FileOutputStream(localFile, appendMode).use { outputStream ->
                    if (startSegmentIndex == 0 && initSegmentUrl != null) {
                        downloadSegmentToStream(initSegmentUrl!!, download.requestHeaders, outputStream)
                    }
                    val totalSegments = segments.size
                    for (index in startSegmentIndex until totalSegments) {
                        coroutineContext.ensureActive()
                        val segmentUrl = segments[index]
                        downloadSegmentToStream(segmentUrl, download.requestHeaders, outputStream)
                        
                        val progress = (100 * (index + 1) / totalSegments).coerceIn(0, 100)
                        onProgress(progress, localFile.length(), 0L, index + 1)
                    }
                }
            } else {
                // Direct file download
                downloadDirectFile(download.url, download.requestHeaders, localFile, { p, b, t ->
                    onProgress(p, b, t, 0)
                }, download)
            }

            // ── Resolve and download subtitles ────────────────────────────────
            val allResolvedSubtitles = mutableListOf<com.kitsugi.animelist.core.player.SubtitleInput>()
            
            // First, add existing subtitles passed with the download
            if (!download.subtitles.isNullOrEmpty()) {
                allResolvedSubtitles.addAll(download.subtitles)
            }
            
            // Fetch external and OpenSubtitles in the background
            try {
                val dMalId = download.malId
                val dAniListId = download.aniListId
                val dTmdbId = download.tmdbId ?: if (dMalId == null && dAniListId == null) download.animeId.toIntOrNull() else null
                
                if (dMalId != null || dAniListId != null || dTmdbId != null) {
                    val resolvedIds = com.kitsugi.animelist.data.remote.KitsugiIdResolver.resolveIds(
                        malId = dMalId,
                        aniListId = dAniListId,
                        tmdbId = dTmdbId
                    )
                    val imdbId = resolvedIds.imdbId
                    val kitsuId = resolvedIds.kitsuId
                    
                    val isMovieType = download.season == 0 && download.episode <= 1
                    val type = if (isMovieType) "movie" else "series"
                    val queryIds = mutableListOf<String>()
                    if (!imdbId.isNullOrBlank()) {
                        queryIds.add(if (isMovieType) imdbId else "$imdbId:${download.season}:${download.episode}")
                    }
                    if (kitsuId != null) {
                        queryIds.add(if (isMovieType) "kitsu:$kitsuId" else "kitsu:$kitsuId:${download.episode}")
                    }
                    
                    if (queryIds.isNotEmpty()) {
                        val guessedFilename = download.url.let { url ->
                            try {
                                val lastSeg = android.net.Uri.parse(url).lastPathSegment
                                if (!lastSeg.isNullOrBlank() && lastSeg.contains(".")) lastSeg else null
                            } catch (_: Exception) { null }
                        }
                        val cleanedFilename = guessedFilename?.substringBefore("\n")?.substringBefore("\r")?.trim()
                        
                        val subRepo = com.kitsugi.animelist.data.repository.SubtitleRepositoryImpl(context)
                        val remoteSubs = mutableListOf<com.kitsugi.animelist.core.player.model.Subtitle>()
                        for (queryId in queryIds) {
                            try {
                                val subs = subRepo.getSubtitles(
                                    type = type,
                                    id = queryId,
                                    videoUrl = download.url,
                                    videoHeaders = download.requestHeaders,
                                    filename = cleanedFilename
                                )
                                remoteSubs.addAll(subs)
                            } catch (e: Exception) {
                                android.util.Log.e("AnimeDownloader", "Failed to fetch subtitles for queryId=$queryId", e)
                            }
                        }
                        
                        for (sub in remoteSubs.distinctBy { it.url }) {
                            val friendlyLangName = com.kitsugi.animelist.core.player.PlayerSubtitleUtils.getFriendlyLanguageName(sub.lang)
                            allResolvedSubtitles.add(
                                com.kitsugi.animelist.core.player.SubtitleInput(
                                    url = sub.url,
                                    name = "$friendlyLangName (${sub.addonName})",
                                    lang = sub.lang
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AnimeDownloader", "Failed to resolve subtitles in downloader", e)
            }

            val dedupedSubs = allResolvedSubtitles.distinctBy { it.url }
            val dlLangs = settings.subtitleDownloadLanguages
                .split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }

            val filteredSubs = if (dlLangs.isEmpty()) {
                dedupedSubs
            } else {
                dedupedSubs.filter { sub ->
                    dlLangs.any { targetLang ->
                        com.kitsugi.animelist.core.player.PlayerSubtitleUtils.matchesLanguageCode(sub.lang ?: "", targetLang)
                    }
                }
            }

            if (filteredSubs.isNotEmpty()) {
                val subsDir = File(destDir, "subs").also { it.mkdirs() }
                for (subtitle in filteredSubs) {
                    try {
                        val extension = when {
                            subtitle.url.contains(".vtt", ignoreCase = true) -> "vtt"
                            subtitle.url.contains(".ass", ignoreCase = true) -> "ass"
                            subtitle.url.contains(".ssa", ignoreCase = true) -> "ssa"
                            else -> "srt"
                        }
                        val safeLang = subtitle.lang.lowercase().filter { it.isLetterOrDigit() }.takeIf { it.isNotEmpty() } ?: "en"
                        // Keep spaces and parentheses for filename readability, only strip illegal characters: \ / : * ? " < > |
                        val safeName = subtitle.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                        val subFile = File(subsDir, "${safeLang}_${safeName}.$extension")
                        
                        // Only use video request headers if the subtitle URL belongs to the same host
                        val useHeaders = try {
                            val subHost = java.net.URI(subtitle.url).host
                            val videoHost = java.net.URI(download.url).host
                            if (subHost != null && subHost.equals(videoHost, ignoreCase = true)) {
                                download.requestHeaders
                            } else {
                                emptyMap()
                            }
                        } catch (_: Exception) {
                            emptyMap()
                        }
                        
                        downloadSubtitleFile(subtitle.url, useHeaders, subFile)
                    } catch (e: Exception) {
                        android.util.Log.e("AnimeDownloader", "Failed to download subtitle: ${subtitle.name}", e)
                    }
                }
            }

            // Create metadata.json for OfflinePlaybackHelper
            val metaFile = File(destDir, "metadata.json")
            val metaJson = """
                {
                  "mediaId": "$mediaId",
                  "title": "${download.animeTitle} - Bölüm ${download.episode}",
                  "durationMs": 0,
                  "downloadedAtMs": ${System.currentTimeMillis()}
                }
            """.trimIndent()
            metaFile.writeText(metaJson)

            // Copy to user custom folder if set
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
            onStatusChanged(AnimeDownload.Status.ERROR, null)
        }
    }

    private suspend fun getUrlContent(url: String, headers: Map<String, String>): String = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }
        KitsugiHttpClient.client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch HLS playlist: ${response.code} ${response.message}")
            }
            response.body?.string() ?: throw Exception("Empty playlist content")
        }
    }

    private suspend fun downloadSegmentToStream(
        url: String,
        headers: Map<String, String>,
        outputStream: java.io.OutputStream
    ) = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        for (attempt in 1..3) {
            try {
                val requestBuilder = Request.Builder().url(url)
                headers.forEach { (k, v) -> requestBuilder.header(k, v) }
                KitsugiHttpClient.client.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Failed to download segment: ${response.code} ${response.message}")
                    }
                    val body = response.body ?: throw Exception("Empty segment body")
                    body.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                return@withContext // Success!
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                lastException = e
                android.util.Log.w("AnimeDownloader", "Segment download attempt $attempt failed: ${e.message}")
                if (attempt < 3) {
                    delay(1000L * attempt)
                }
            }
        }
        throw lastException ?: Exception("Unknown error downloading segment")
    }

    private suspend fun downloadDirectFile(
        url: String,
        headers: Map<String, String>,
        localFile: File,
        onProgress: (Int, Long, Long) -> Unit,
        download: AnimeDownload
    ) = withContext(Dispatchers.IO) {
        var attempts = 0
        val maxAttempts = 3
        while (attempts < maxAttempts) {
            attempts++
            try {
                val existingLength = if (localFile.exists()) localFile.length() else 0L
                val requestBuilder = Request.Builder().url(url)
                headers.forEach { (k, v) -> requestBuilder.header(k, v) }
                
                if (existingLength > 0) {
                    requestBuilder.header("Range", "bytes=$existingLength-")
                }
                
                KitsugiHttpClient.client.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        if (response.code == 416) {
                            localFile.delete()
                            throw Exception("416 Range Not Satisfiable")
                        }
                        throw Exception("Failed to download file: ${response.code} ${response.message}")
                    }
                    
                    val isRange = response.code == 206
                    val body = response.body ?: throw Exception("Empty response body")
                    val contentLength = body.contentLength()
                    val totalBytes = if (isRange) contentLength + existingLength else contentLength
                    val appendMode = isRange && existingLength > 0
                    
                    body.byteStream().use { inputStream ->
                        java.io.FileOutputStream(localFile, appendMode).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var bytesDownloaded = if (appendMode) existingLength else 0L
                            
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                coroutineContext.ensureActive()
                                outputStream.write(buffer, 0, bytesRead)
                                bytesDownloaded += bytesRead
                                
                                val progress = if (totalBytes > 0) {
                                    (100 * bytesDownloaded / totalBytes).toInt().coerceIn(0, 100)
                                } else {
                                    0
                                }
                                onProgress(progress, bytesDownloaded, totalBytes)
                            }
                        }
                    }
                }
                return@withContext // Success!
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("AnimeDownloader", "Direct download attempt $attempts failed: ${e.message}")
                if (attempts >= maxAttempts) {
                    throw e
                }
                delay(2000L * attempts)
            }
        }
    }

    private suspend fun downloadSubtitleFile(
        url: String,
        headers: Map<String, String>,
        destFile: File
    ) = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }
        KitsugiHttpClient.client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to download subtitle: ${response.code} ${response.message}")
            }
            val body = response.body ?: throw Exception("Empty subtitle response body")
            body.byteStream().use { inputStream ->
                destFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        try {
            val baseUri = java.net.URI(baseUrl)
            val resolvedUri = baseUri.resolve(relativeUrl)
            return resolvedUri.toString()
        } catch (e: Exception) {
            if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
                return relativeUrl
            }
            val baseWithoutQuery = baseUrl.substringBefore("?")
            val lastSlash = baseWithoutQuery.lastIndexOf('/')
            return if (lastSlash != -1) {
                val directory = baseWithoutQuery.substring(0, lastSlash + 1)
                directory + relativeUrl
            } else {
                relativeUrl
            }
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
