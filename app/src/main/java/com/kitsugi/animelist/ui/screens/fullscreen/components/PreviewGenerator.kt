package com.kitsugi.animelist.ui.screens.fullscreen.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class PreviewGenerator(
    private val context: Context,
    private val videoUrl: String,
    private val headers: Map<String, String> = emptyMap()
) {
    companion object {
        private const val TAG = "PreviewGenerator"
        private const val PREVIEW_WIDTH = 160
        private const val PREVIEW_HEIGHT = 90
        private const val MAX_PREVIEWS = 120
        private const val MIN_INTERVAL_MS = 5000L
        private const val MAX_CACHE_SIZE_BYTES = 150 * 1024 * 1024L // 150 MB
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var extractJob: Job? = null

    private val urlHash = md5(videoUrl)
    private val cacheDir = File(File(context.filesDir, "previews"), urlHash)

    private var durationMs: Long = 0L
    private var totalFrames: Int = 0
    private var stepMs: Long = 0L

    // In-memory cache of decoded Bitmaps
    private var images = Array<Bitmap?>(0) { null }
    private var isInitialized = false

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        runLruCleanup()
    }

    fun start(playerDurationMs: Long) {
        if (playerDurationMs <= 0) return
        if (isInitialized) return

        durationMs = playerDurationMs
        // Calculate number of frames to extract: every 10s, but bounded between 10 and MAX_PREVIEWS
        val calculatedFrames = (durationMs / 10000L).toInt()
        totalFrames = calculatedFrames.coerceIn(10, MAX_PREVIEWS)
        stepMs = durationMs / totalFrames

        images = Array(totalFrames) { null }
        isInitialized = true

        extractJob = scope.launch {
            try {
                // If cache directory already has completed thumbnails, we don't need to extract
                val files = cacheDir.listFiles()?.filter { it.isFile && it.name.endsWith(".jpg") }
                val cachedCount = files?.size ?: 0
                
                // If we have at least 80% of target frames cached, reuse them
                if (cachedCount >= totalFrames * 0.8) {
                    Log.d(TAG, "Cache hit: found $cachedCount cached preview frames for $urlHash")
                    return@launch
                }

                Log.d(TAG, "Cache miss or incomplete: extracting $totalFrames frames for $urlHash")
                
                val retriever = MediaMetadataRetriever()
                if (headers.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    retriever.setDataSource(videoUrl, headers)
                } else {
                    retriever.setDataSource(videoUrl)
                }

                // Verify duration from media details if possible
                val metaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                if (metaDuration != null && metaDuration > 0) {
                    durationMs = metaDuration
                    stepMs = durationMs / totalFrames
                }

                for (i in 0 until totalFrames) {
                    if (!isActive) break

                    val frameFile = File(cacheDir, "$i.jpg")
                    if (frameFile.exists() && frameFile.length() > 0) {
                        continue
                    }

                    val timeUs = (i * stepMs) * 1000L
                    val bitmap = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            retriever.getScaledFrameAtTime(
                                timeUs,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                                PREVIEW_WIDTH,
                                PREVIEW_HEIGHT
                            )
                        } else {
                            val raw = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            raw?.let {
                                val scaled = Bitmap.createScaledBitmap(it, PREVIEW_WIDTH, PREVIEW_HEIGHT, true)
                                if (scaled != it) {
                                    it.recycle()
                                }
                                scaled
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to extract frame at $timeUs us: ${e.message}")
                        null
                    }

                    if (bitmap != null) {
                        // Save to cache dir
                        try {
                            FileOutputStream(frameFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to save frame $i to cache", e)
                        }
                        
                        // Keep in-memory if near start, or recycle to save memory
                        // We will lazily load from disk when requested, so we recycle immediately
                        bitmap.recycle()
                    }
                }
                retriever.release()
                // Update directory lastModified so LRU knows it was recently active
                cacheDir.setLastModified(System.currentTimeMillis())
                Log.d(TAG, "Completed preview extraction for $urlHash")
            } catch (e: Exception) {
                Log.e(TAG, "Error in preview extraction loop", e)
            }
        }
    }

    fun getPreviewImage(fraction: Float): Bitmap? {
        if (!isInitialized || totalFrames <= 0) return null
        val idx = (fraction * totalFrames).toInt().coerceIn(0, totalFrames - 1)

        synchronized(images) {
            // 1. Check in-memory cache
            val memoryBmp = images[idx]
            if (memoryBmp != null && !memoryBmp.isRecycled) {
                return memoryBmp
            }

            // 2. Check disk cache
            val frameFile = File(cacheDir, "$idx.jpg")
            if (frameFile.exists() && frameFile.length() > 0) {
                try {
                    val bitmap = BitmapFactory.decodeFile(frameFile.absolutePath)
                    if (bitmap != null) {
                        images[idx] = bitmap
                        // Clean up other cached bitmaps to control RAM consumption
                        cleanMemoryCache(exceptIndex = idx)
                        return bitmap
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode cached frame $idx", e)
                }
            }
        }
        return null
    }

    private fun cleanMemoryCache(exceptIndex: Int) {
        // Keep up to 3 frames in memory (previous, current, next) and recycle others
        for (i in images.indices) {
            if (i < exceptIndex - 1 || i > exceptIndex + 1) {
                images[i]?.let {
                    if (!it.isRecycled) {
                        it.recycle()
                    }
                }
                images[i] = null
            }
        }
    }

    fun release() {
        extractJob?.cancel()
        scope.cancel()
        synchronized(images) {
            for (i in images.indices) {
                images[i]?.let {
                    if (!it.isRecycled) {
                        it.recycle()
                    }
                }
                images[i] = null
            }
        }
        Log.d(TAG, "Released PreviewGenerator for $urlHash")
    }

    private fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input.hashCode().toString()
        }
    }

    private fun runLruCleanup() {
        scope.launch {
            val rootDir = File(context.filesDir, "previews")
            if (!rootDir.exists()) return@launch
            val folders = rootDir.listFiles()?.filter { it.isDirectory } ?: return@launch
            
            var totalSize = folders.sumOf { folder ->
                folder.listFiles()?.sumOf { it.length() } ?: 0L
            }

            if (totalSize <= MAX_CACHE_SIZE_BYTES) return@launch

            Log.d(TAG, "Previews cache size ($totalSize bytes) exceeds limit, running cleanup...")

            // Sort folders by lastModified (least recently modified first)
            val sorted = folders.sortedBy { it.lastModified() }
            for (folder in sorted) {
                val size = folder.listFiles()?.sumOf { it.length() } ?: 0L
                if (deleteDir(folder)) {
                    totalSize -= size
                    Log.d(TAG, "Deleted preview cache: ${folder.name}")
                    if (totalSize <= MAX_CACHE_SIZE_BYTES) break
                }
            }
        }
    }

    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { deleteDir(it) }
        }
        return dir.delete()
    }
}
