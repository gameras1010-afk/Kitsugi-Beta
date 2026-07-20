package com.kitsugi.animelist.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import com.kitsugi.animelist.core.network.IPv4FirstDns
import java.io.IOException
import java.util.concurrent.TimeUnit

class DebridResolver(private val context: Context) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .dns(IPv4FirstDns())
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val sharedPrefs by lazy {
        context.applicationContext.getSharedPreferences("MyWebViewPrefs", Context.MODE_PRIVATE)
    }

    fun getApiKey(): String? {
        return sharedPrefs.getString("debrid_api_key", null)
    }

    fun setApiKey(key: String?) {
        sharedPrefs.edit().putString("debrid_api_key", key).apply()
    }

    /**
     * Resolves a torrent infoHash into a direct streaming link via RealDebrid.
     */
    suspend fun resolveHash(infoHash: String, fileIndex: Int?): String? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            Log.w("DebridResolver", "RealDebrid API key is not set.")
            return@withContext null
        }

        try {
            // Step 1: Add Magnet to RealDebrid
            val magnetUrl = "magnet:?xt=urn:btih:$infoHash"
            val addRequest = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/torrents/addMagnet")
                .post(FormBody.Builder().add("magnet", magnetUrl).build())
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            val torrentId = client.newCall(addRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("DebridResolver", "addMagnet failed: ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext null
                val result = gson.fromJson(bodyStr, Map::class.java)
                result["id"] as? String
            } ?: return@withContext null

            // Step 2: Select Files
            val fileSelectStr = if (fileIndex != null) {
                (fileIndex + 1).toString()
            } else {
                "all"
            }

            val selectRequest = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/torrents/selectFiles/$torrentId")
                .post(FormBody.Builder().add("files", fileSelectStr).build())
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            client.newCall(selectRequest).execute().use { response ->
                if (!response.isSuccessful && response.code != 204) {
                    Log.e("DebridResolver", "selectFiles failed: ${response.code}")
                }
            }

            // Step 3: Get Torrent Info to retrieve unrestricted links
            val infoRequest = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/torrents/info/$torrentId")
                .get()
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            val rdLink = client.newCall(infoRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("DebridResolver", "torrents/info failed: ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext null
                val infoMap = gson.fromJson(bodyStr, Map::class.java) ?: return@withContext null
                
                // Get the links list
                val links = infoMap["links"] as? List<*> ?: return@withContext null
                if (links.isEmpty()) return@withContext null

                // If we selected a single file, it will be at index 0. Otherwise, use fileIndex.
                val targetIndex = if (fileIndex != null && fileSelectStr != "all") 0 else (fileIndex ?: 0)
                val safeIndex = if (targetIndex >= 0 && targetIndex < links.size) targetIndex else 0
                links[safeIndex] as? String
            } ?: return@withContext null

            // Step 4: Unrestrict the link to get direct download/streaming URL
            val unrestrictRequest = Request.Builder()
                .url("https://api.real-debrid.com/rest/1.0/unrestrict/link")
                .post(FormBody.Builder().add("link", rdLink).build())
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            client.newCall(unrestrictRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("DebridResolver", "unrestrict/link failed: ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext null
                val unrestrictMap = gson.fromJson(bodyStr, Map::class.java) ?: return@withContext null
                unrestrictMap["download"] as? String
            }
        } catch (e: Exception) {
            Log.e("DebridResolver", "Exception during debrid resolution", e)
            null
        }
    }
}
