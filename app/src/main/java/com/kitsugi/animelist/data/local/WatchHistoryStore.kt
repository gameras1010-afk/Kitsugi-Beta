package com.kitsugi.animelist.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kitsugi.animelist.data.model.WatchHistoryEntry

class WatchHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("watch_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getHistory(): List<WatchHistoryEntry> {
        val json = prefs.getString("history_list", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WatchHistoryEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveHistory(entries: List<WatchHistoryEntry>) {
        prefs.edit().putString("history_list", gson.toJson(entries)).apply()
    }
}
