package com.kitsugi.animelist.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kitsugi.animelist.data.model.AnimeDownload

class AnimeDownloadStore(context: Context) {
    private val preferences = context.getSharedPreferences("active_downloads", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getDownloads(): List<AnimeDownload> {
        val json = preferences.getString("downloads_list", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<AnimeDownload>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveDownloads(downloads: List<AnimeDownload>) {
        val json = gson.toJson(downloads)
        preferences.edit().putString("downloads_list", json).apply()
    }
}
