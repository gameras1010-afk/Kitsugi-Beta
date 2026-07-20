package com.kitsugi.animelist.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.kitsugi.animelist.core.network.KitsugiHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

sealed class DownloadState {
    object Idle : DownloadState()
    data class Progress(val percentage: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
    data class Finished(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ApkInstallerHelper(private val context: Context) {

    companion object {
        private const val TAG = "ApkInstallerHelper"
        private const val FILE_PROVIDER_AUTHORITY = "com.kitsugi.animelist.fileprovider"
    }

    fun downloadApk(downloadUrl: String): Flow<DownloadState> = flow {
        emit(DownloadState.Progress(0f, 0L, 0L))
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            val response = KitsugiHttpClient.client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(DownloadState.Error("İndirme sunucu hatası: HTTP ${response.code}"))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(DownloadState.Error("İndirme içeriği boş."))
                return@flow
            }

            val totalBytes = body.contentLength()
            val updatesDir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
            val apkFile = File(updatesDir, "kitsugi_update.apk")

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var downloadedBytes = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (totalBytes > 0) {
                            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else 0.5f

                        emit(DownloadState.Progress(progress, downloadedBytes, totalBytes))
                    }
                    output.flush()
                }
            }

            Log.d(TAG, "APK download completed: ${apkFile.absolutePath} (${apkFile.length()} bytes)")
            emit(DownloadState.Finished(apkFile))

        } catch (e: Exception) {
            Log.e(TAG, "APK download failed", e)
            emit(DownloadState.Error("İndirme hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}"))
        }
    }.flowOn(Dispatchers.IO)

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun installApk(file: File): Boolean {
        return try {
            if (!file.exists()) {
                Log.e(TAG, "Install target APK file does not exist")
                return false
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                FILE_PROVIDER_AUTHORITY,
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start package installation intent", e)
            false
        }
    }
}
