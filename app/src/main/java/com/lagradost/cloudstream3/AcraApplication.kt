package com.lagradost.cloudstream3

import android.app.Application
import android.content.Context

class AcraApplication : Application() {
    companion object {
        @JvmStatic
        val context: Context?
            get() = CloudStreamApp.context

        @JvmStatic
        fun removeKeys(folder: String): Int? =
            CloudStreamApp.removeKeys(folder)

        @JvmStatic
        fun <T> setKey(path: String, value: T) =
            CloudStreamApp.setKey(path, value)

        @JvmStatic
        fun <T> setKey(folder: String, path: String, value: T) =
            CloudStreamApp.setKey(folder, path, value)

        @JvmStatic
        inline fun <reified T : Any> getKey(path: String, defVal: T?): T? =
            CloudStreamApp.getKey(path, defVal)

        @JvmStatic
        inline fun <reified T : Any> getKey(path: String): T? =
            CloudStreamApp.getKey(path)

        @JvmStatic
        inline fun <reified T : Any> getKey(folder: String, path: String): T? =
            CloudStreamApp.getKey(folder, path)

        @JvmStatic
        inline fun <reified T : Any> getKey(folder: String, path: String, defVal: T?): T? =
            CloudStreamApp.getKey(folder, path, defVal)
    }
}
