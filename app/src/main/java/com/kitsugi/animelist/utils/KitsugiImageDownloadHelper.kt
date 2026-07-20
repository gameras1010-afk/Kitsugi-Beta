package com.kitsugi.animelist.utils

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.kitsugi.animelist.data.settings.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object KitsugiImageDownloadHelper {

    fun hasWritePermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            emptyArray()
        }
    }

    fun downloadImage(context: Context, url: String, title: String, customUriString: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val uriToUse = customUriString ?: runCatching {
                SettingsDataStore(context).settingsFlow.first().customImageDownloadUri
            }.getOrDefault("")

            if (uriToUse.isNotBlank()) {
                try {
                    val treeUri = Uri.parse(uriToUse)
                    val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
                    if (pickedDir != null && pickedDir.exists() && pickedDir.isDirectory) {
                        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9_]"), "_")
                        val filename = "Kitsugi_${sanitizedTitle}_${System.currentTimeMillis()}.jpg"
                        val imageFile = pickedDir.createFile("image/jpeg", filename)
                        if (imageFile != null) {
                            val connection = java.net.URL(url).openConnection()
                            connection.connectTimeout = 15000
                            connection.readTimeout = 15000
                            connection.inputStream.use { input ->
                                context.contentResolver.openOutputStream(imageFile.uri)?.use { output ->
                                    input.copyTo(output)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Resim özel klasöre kaydedildi: $filename",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fallback to DownloadManager
            withContext(Dispatchers.Main) {
                downloadWithDownloadManager(context, url, title)
            }
        }
    }

    private fun downloadWithDownloadManager(context: Context, url: String, title: String) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(url)
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val filename = "Kitsugi_${sanitizedTitle}_${System.currentTimeMillis()}.jpg"

            val request = DownloadManager.Request(uri)
                .setTitle("Kitsugi Resim İndirme")
                .setDescription(title)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Kitsugi/$filename")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadManager.enqueue(request)
            Toast.makeText(context, "İndirme başlatıldı: $filename", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "İndirme başarısız oldu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
