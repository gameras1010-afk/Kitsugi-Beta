package com.kitsugi.animelist.core.player

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.ByteBuffer
import java.nio.ByteOrder

object OpenSubtitlesHasher {
    private const val HASH_CHUNK_SIZE = 65536 // 64 KB

    data class HashResult(val hash: String, val size: Long)

    fun compute(url: String, headers: Map<String, String>? = null): HashResult? {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return null
        }
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            // 1. Get content length (size)
            val headRequest = Request.Builder()
                .url(url)
                .apply {
                    headers?.forEach { (k, v) -> header(k, v) }
                }
                .head()
                .build()

            val size = client.newCall(headRequest).execute().use { response ->
                if (!response.isSuccessful) return null
                response.header("Content-Length")?.toLongOrNull()
            } ?: return null

            if (size < HASH_CHUNK_SIZE * 2) return null

            // 2. Fetch first 64KB
            val firstRangeRequest = Request.Builder()
                .url(url)
                .apply {
                    headers?.forEach { (k, v) -> header(k, v) }
                    header("Range", "bytes=0-${HASH_CHUNK_SIZE - 1}")
                }
                .build()

            val firstBytes = client.newCall(firstRangeRequest).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.bytes()
            } ?: return null

            if (firstBytes.size < HASH_CHUNK_SIZE) return null

            // 3. Fetch last 64KB
            val lastRangeRequest = Request.Builder()
                .url(url)
                .apply {
                    headers?.forEach { (k, v) -> header(k, v) }
                    header("Range", "bytes=${size - HASH_CHUNK_SIZE}-${size - 1}")
                }
                .build()

            val lastBytes = client.newCall(lastRangeRequest).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.bytes()
            } ?: return null

            if (lastBytes.size < HASH_CHUNK_SIZE) return null

            // Calculate checksum
            var head = size

            val firstBuffer = ByteBuffer.wrap(firstBytes).order(ByteOrder.LITTLE_ENDIAN)
            val longBuffer = firstBuffer.asLongBuffer()
            while (longBuffer.hasRemaining()) {
                head += longBuffer.get()
            }

            val lastBuffer = ByteBuffer.wrap(lastBytes).order(ByteOrder.LITTLE_ENDIAN)
            val lastLongBuffer = lastBuffer.asLongBuffer()
            while (lastLongBuffer.hasRemaining()) {
                head += lastLongBuffer.get()
            }

            return HashResult(String.format("%016x", head), size)
        } catch (e: Exception) {
            Log.e("OpenSubtitlesHasher", "Error computing hash for $url", e)
            return null
        }
    }
}
