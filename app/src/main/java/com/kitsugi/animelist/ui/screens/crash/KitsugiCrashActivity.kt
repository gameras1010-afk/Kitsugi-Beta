package com.kitsugi.animelist.ui.screens.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.BuildConfig
import com.kitsugi.animelist.data.settings.AppSettings
import com.kitsugi.animelist.data.settings.SettingsDataStore
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiAccentForThemeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class KitsugiCrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val intentCrashReport = intent.getStringExtra("crash_report")
        
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            
            val settingsDataStore = remember {
                SettingsDataStore(context.applicationContext)
            }
            val appSettings by settingsDataStore.settingsFlow.collectAsState(
                initial = AppSettings()
            )
            val activeAccentColor = KitsugiAccentForThemeId(appSettings.selectedThemeId)

            val crashReport = remember {
                if (!intentCrashReport.isNullOrBlank()) {
                    intentCrashReport
                } else {
                    val file = File(context.filesDir, "crash_log.txt")
                    if (file.exists()) file.readText() else "Hiçbir hata detayına ulaşılamadı."
                }
            }

            CompositionLocalProvider(
                LocalKitsugiAccent provides activeAccentColor
            ) {
                Scaffold(
                    containerColor = KitsugiColors.Background
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))

                        // Warning Icon
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = KitsugiColors.SurfaceSoft,
                            border = BorderStroke(1.dp, activeAccentColor.copy(alpha = 0.5f)),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BugReport,
                                    contentDescription = "Hata",
                                    tint = KitsugiColors.AccentRed,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        // Header Text
                        Text(
                            text = "Eyvah! Bir Hata Oluştu",
                            color = KitsugiColors.TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Kitsugi beklenmedik bir şekilde durduruldu. Hata detaylarını aşağıda görebilir, kopyalayabilir veya geliştiriciye gönderebilirsiniz.",
                            color = KitsugiColors.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Console Terminal for Trace details
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, KitsugiColors.Border, RoundedCornerShape(14.dp)),
                            color = KitsugiColors.Surface
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                // Monospace text inside scroll view
                                val scrollState = rememberScrollState()
                                SelectionContainer {
                                    Text(
                                        text = crashReport,
                                        color = KitsugiColors.TextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(scrollState)
                                    )
                                }
                            }
                        }

                        // Action Buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Copy Button
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Kitsugi Crash Report", crashReport)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Hata raporu kopyalandı", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = KitsugiColors.SurfaceSoft,
                                        contentColor = KitsugiColors.TextPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, KitsugiColors.Border),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Kopyala", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Kopyala", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                // Share Button
                                Button(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, crashReport)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Hata Raporunu Paylaş"))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = KitsugiColors.SurfaceSoft,
                                        contentColor = KitsugiColors.TextPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, KitsugiColors.Border),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = "Paylaş", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Paylaş", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Send to Developer Button
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val filesToShare = ArrayList<android.net.Uri>()

                                            // 1. Cihaz Bilgisi
                                            val infoFile = File(context.filesDir, "device_info.txt")
                                            val infoText = buildString {
                                                append("Uygulama Sürümü: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
                                                append("Cihaz Markası: ${android.os.Build.BRAND}\n")
                                                append("Cihaz Modeli: ${android.os.Build.MODEL}\n")
                                                append("Android Sürümü: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
                                                append("Üretici: ${android.os.Build.MANUFACTURER}\n")
                                                append("Zaman Dilimi: ${java.util.TimeZone.getDefault().id}\n")
                                                append("Yerel Saat: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
                                            }
                                            infoFile.writeText(infoText)
                                            val infoUri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "com.kitsugi.animelist.fileprovider",
                                                infoFile
                                            )
                                            filesToShare.add(infoUri)

                                            // 2. Çökme Raporu
                                            val crashFile = File(context.filesDir, "crash_log.txt")
                                            if (crashFile.exists()) {
                                                val crashUri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "com.kitsugi.animelist.fileprovider",
                                                    crashFile
                                                )
                                                filesToShare.add(crashUri)
                                            }

                                            // 3. Arka Plan Günlükleri
                                            val logFiles = listOf("app_logs.txt", "app_logs.txt.1", "app_logs.txt.2")
                                            for (fileName in logFiles) {
                                                val logFile = File(context.filesDir, fileName)
                                                if (logFile.exists() && logFile.length() > 0) {
                                                    val logUri = androidx.core.content.FileProvider.getUriForFile(
                                                        context,
                                                        "com.kitsugi.animelist.fileprovider",
                                                        logFile
                                                    )
                                                    filesToShare.add(logUri)
                                                }
                                            }

                                            // 4. Canlı Logcat Dökümü
                                            val currentLogcatFile = File(context.filesDir, "current_logcat.txt")
                                            withContext(Dispatchers.IO) {
                                                try {
                                                    val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "*:D"))
                                                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                                                    val writer = currentLogcatFile.bufferedWriter()
                                                    var line: String?
                                                    while (reader.readLine().also { line = it } != null) {
                                                        writer.write(line)
                                                        writer.newLine()
                                                    }
                                                    writer.close()
                                                    reader.close()
                                                } catch (e: Exception) {
                                                    currentLogcatFile.writeText("Anlık log alınamadı: ${e.localizedMessage}")
                                                }
                                            }
                                            if (currentLogcatFile.exists() && currentLogcatFile.length() > 0) {
                                                val currentUri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "com.kitsugi.animelist.fileprovider",
                                                    currentLogcatFile
                                                )
                                                filesToShare.add(currentUri)
                                            }

                                            if (filesToShare.isNotEmpty()) {
                                                val shareIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND_MULTIPLE
                                                    type = "text/plain"
                                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, filesToShare)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Hata Raporunu Geliştiriciye Gönder"))
                                            } else {
                                                Toast.makeText(context, "Paylaşılacak log dosyası bulunamadı", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Paylaşım Hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KitsugiColors.SurfaceSoft,
                                    contentColor = activeAccentColor
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, activeAccentColor.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Geliştiriciye Gönder", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Geliştiriciye Gönder", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            // Restart Button
                            Button(
                                onClick = {
                                    val restartIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                    if (restartIntent != null) {
                                        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        context.startActivity(restartIntent)
                                    }
                                    finish()
                                    android.os.Process.killProcess(android.os.Process.myPid())
                                    System.exit(0)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = activeAccentColor,
                                    contentColor = KitsugiColors.Background
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Yeniden Başlat", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Uygulamayı Yeniden Başlat", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}
