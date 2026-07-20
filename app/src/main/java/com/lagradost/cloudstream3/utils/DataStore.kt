package com.lagradost.cloudstream3.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

const val PREFERENCES_NAME = "rebuild_preference"

@PublishedApi
internal val _dataStoreGson = Gson()

/** Serialize any value to a JSON string. */
internal fun Any?.toJsonLiteralInternal(): String = _dataStoreGson.toJson(this)

/** Deserialize a JSON string to T. */
internal fun <T : Any> parseJsonInternal(json: String, clazz: Class<T>): T =
    _dataStoreGson.fromJson(json, clazz)

/** Deserialize a JSON string to T using an inline reified type. */
@PublishedApi
internal inline fun <reified T : Any> parseJsonInternal(json: String): T =
    _dataStoreGson.fromJson(json, object : TypeToken<T>() {}.type)

object DataStore {
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    fun Context.getSharedPrefs(): SharedPreferences {
        return getPreferences(this)
    }

    fun getFolderName(folder: String, path: String): String {
        return "${folder}/${path}"
    }

    fun Context.getDefaultSharedPrefs(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(this)
    }

    fun Context.getKeys(folder: String): List<String> {
        val fixedFolder = folder.trimEnd('/') + "/"
        return this.getSharedPrefs().all.keys.filter { it.startsWith(fixedFolder) }
    }

    fun Context.removeKey(folder: String, path: String) {
        removeKey(getFolderName(folder, path))
    }

    fun Context.containsKey(folder: String, path: String): Boolean {
        return containsKey(getFolderName(folder, path))
    }

    fun Context.containsKey(path: String): Boolean {
        val prefs = getSharedPrefs()
        return prefs.contains(path)
    }

    fun Context.removeKey(path: String) {
        try {
            val prefs = getSharedPrefs()
            if (prefs.contains(path)) {
                prefs.edit {
                    remove(path)
                }
            }
        } catch (_: Exception) {}
    }

    fun Context.removeKeys(folder: String): Int {
        val keys = getKeys("$folder/")
        return try {
            getSharedPrefs().edit {
                keys.forEach { value ->
                    remove(value)
                }
            }
            keys.size
        } catch (_: Exception) {
            0
        }
    }

    fun <T> Context.setKey(path: String, value: T) {
        try {
            getSharedPrefs().edit {
                putString(path, value?.toJsonLiteralInternal())
            }
        } catch (_: Exception) {}
    }

    fun <T : Any> Context.getKey(path: String, valueType: Class<T>): T? {
        return try {
            val json: String = getSharedPrefs().getString(path, null) ?: return null
            parseJsonInternal(json, valueType)
        } catch (_: Exception) {
            null
        }
    }

    fun <T> Context.setKey(folder: String, path: String, value: T) {
        setKey(getFolderName(folder, path), value)
    }

    inline fun <reified T : Any> String.toKotlinObject(): T {
        return parseJsonInternal(this)
    }

    fun <T : Any> String.toKotlinObject(valueType: Class<T>): T {
        return parseJsonInternal(this, valueType)
    }

    inline fun <reified T : Any> Context.getKey(path: String, defVal: T?): T? {
        return try {
            val json: String = getSharedPrefs().getString(path, null) ?: return defVal
            json.toKotlinObject()
        } catch (_: Exception) {
            null
        }
    }

    inline fun <reified T : Any> Context.getKey(path: String): T? {
        return getKey(path, null)
    }

    inline fun <reified T : Any> Context.getKey(folder: String, path: String): T? {
        return getKey(getFolderName(folder, path), null)
    }

    inline fun <reified T : Any> Context.getKey(folder: String, path: String, defVal: T?): T? {
        return getKey(getFolderName(folder, path), defVal) ?: defVal
    }
}
