package com.lagradost.cloudstream3

import android.app.Application
import android.content.Context
import java.lang.ref.WeakReference

open class CloudStreamApp : Application() {

    override fun onCreate() {
        super.onCreate()
        _context = WeakReference(applicationContext)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (base != null) {
            _context = WeakReference(base)
        }
    }

    companion object {
        private var _context: WeakReference<Context>? = null

        @JvmStatic
        var context: Context?
            get() = _context?.get() ?: com.kitsugi.animelist.KitsugiApplication.getInstance()?.applicationContext
            set(value) {
                if (value != null) {
                    _context = WeakReference(value)
                }
            }

        @PublishedApi
        internal fun getPrefs(folder: String): android.content.SharedPreferences? {
            val ctx = context ?: return null
            return ctx.getSharedPreferences("cloudstream_datastore_$folder", Context.MODE_PRIVATE)
        }

        @PublishedApi
        internal fun getPrefs(): android.content.SharedPreferences? {
            val ctx = context ?: return null
            return ctx.getSharedPreferences("cloudstream_datastore", Context.MODE_PRIVATE)
        }

        @JvmStatic
        fun removeKeys(folder: String): Int? {
            val prefs = getPrefs(folder) ?: return null
            val size = prefs.all.size
            prefs.edit().clear().apply()
            return size
        }

        @JvmStatic
        fun <T> setKey(path: String, value: T) {
            val prefs = getPrefs() ?: return
            val json = com.google.gson.Gson().toJson(value)
            prefs.edit().putString(path, json).apply()
        }

        @JvmStatic
        fun <T> setKey(folder: String, path: String, value: T) {
            val prefs = getPrefs(folder) ?: return
            val json = com.google.gson.Gson().toJson(value)
            prefs.edit().putString(path, json).apply()
        }

        @JvmStatic
        inline fun <reified T : Any> getKey(path: String, defVal: T?): T? {
            val prefs = getPrefs() ?: return defVal
            val json = prefs.getString(path, null) ?: return defVal
            return try {
                com.google.gson.Gson().fromJson(json, T::class.java)
            } catch (e: Exception) {
                defVal
            }
        }

        @JvmStatic
        inline fun <reified T : Any> getKey(path: String): T? {
            return getKey(path, null)
        }

        @JvmStatic
        inline fun <reified T : Any> getKey(folder: String, path: String): T? {
            return getKey(folder, path, null)
        }

        @JvmStatic
        inline fun <reified T : Any> getKey(folder: String, path: String, defVal: T?): T? {
            val prefs = getPrefs(folder) ?: return defVal
            val json = prefs.getString(path, null) ?: return defVal
            return try {
                com.google.gson.Gson().fromJson(json, T::class.java)
            } catch (e: Exception) {
                defVal
            }
        }

        @JvmStatic
        fun getKeys(folder: String): List<String>? {
            val prefs = getPrefs(folder) ?: return null
            return prefs.all.keys.toList()
        }

        @JvmStatic
        fun removeKey(folder: String, path: String) {
            val prefs = getPrefs(folder) ?: return
            prefs.edit().remove(path).apply()
        }

        @JvmStatic
        fun removeKey(path: String) {
            val prefs = getPrefs() ?: return
            prefs.edit().remove(path).apply()
        }
    }
}
