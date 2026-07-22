package com.kitsugi.animelist.ui.screens.more

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.LocalKitsugiColors
import com.kitsugi.animelist.ui.utils.tvClickable
import com.kitsugi.animelist.ui.screens.settings.DeveloperLogsDialog

import com.kitsugi.animelist.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@Composable
fun AboutScreen(
    autoUpdateCheckEnabled: Boolean,
    onAutoUpdateCheckEnabledChanged: (Boolean) -> Unit,
    onCheckForUpdatesClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val KitsugiColors = LocalKitsugiColors.current
    val accentColor = LocalKitsugiAccent.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val version = BuildConfig.VERSION_NAME
    val developer = "Kitsugi Team"
    val githubRepo = "https://github.com/gameras1010-afk/Kitsugi-Beta"

    var showDeveloperLogs by remember { mutableStateOf(false) }

    val libraries = listOf(
        LibraryInfo("CloudStream Engine", "Eklenti tabanlı medya sağlayıcısı", "https://github.com/recloudstream/cloudstream"),
        LibraryInfo("Kotatsu Reader", "Gelişmiş manga okuyucu motoru", "https://github.com/KotatsuApp/Kotatsu"),
        LibraryInfo("Supabase client", "Bulut veri eşitleme ve kimlik doğrulama", "https://supabase.com"),
        LibraryInfo("Jetpack Compose", "Android için modern bildirimsel UI kütüphanesi", "https://developer.android.com/compose"),
        LibraryInfo("Media3 & ExoPlayer", "Google Media3 tabanlı güçlü video oynatıcı", "https://github.com/google/ExoPlayer"),
        LibraryInfo("Room Database", "Çevrimdışı öncelikli veri tabanı mimarisi", "https://developer.android.com/training/data-storage/room")
    )

    fun openUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Geri",
                        tint = KitsugiColors.textPrimary
                    )
                }

                Text(
                    text = "Uygulama Hakkında",
                    color = KitsugiColors.textPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        containerColor = KitsugiColors.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))

                // App Brand
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Kitsugi Anime & Manga",
                    color = KitsugiColors.textPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Sürüm $version",
                    color = KitsugiColors.textMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Info Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = KitsugiColors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Hakkında",
                            color = KitsugiColors.textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Kitsugi, anime ve manga listelerinizi en popüler platformlar (AniList, MAL, Simkl) ile gerçek zamanlı eşitleyen, entegre medya oynatıcı ve manga okuyucusu barındıran üst düzey bir takip uygulamasıdır.",
                            color = KitsugiColors.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Divider(color = KitsugiColors.surfaceStrong, thickness = 1.dp)

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .tvClickable { openUrl(githubRepo) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                tint = KitsugiColors.textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "GitHub Repository",
                                    color = KitsugiColors.textPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Gelişime katkıda bulun veya hata bildirimi yap",
                                    color = KitsugiColors.textMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Updates Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = KitsugiColors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Uygulama Güncellemeleri",
                            color = KitsugiColors.textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Yeni sürümleri otomatik veya manuel olarak kontrol edin.",
                            color = KitsugiColors.textSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .background(
                                        color = KitsugiColors.surfaceSoft,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Otomatik Kontrol",
                                        color = KitsugiColors.textPrimary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (autoUpdateCheckEnabled) "Açık" else "Kapalı",
                                        color = KitsugiColors.textMuted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Switch(
                                    checked = autoUpdateCheckEnabled,
                                    onCheckedChange = onAutoUpdateCheckEnabledChanged,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = KitsugiColors.background,
                                        checkedTrackColor = accentColor
                                    )
                                )
                            }

                            Button(
                                onClick = onCheckForUpdatesClick,
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Text(
                                    text = "Şimdi Denetle",
                                    color = KitsugiColors.background,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Debug and Logs Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = KitsugiColors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Geliştirici & Hata Logları",
                            color = KitsugiColors.textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { showDeveloperLogs = true },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Geliştirici Günlüklerini Gör", color = KitsugiColors.background, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val filesToShare = ArrayList<Uri>()

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
                                            val intent = Intent().apply {
                                                action = Intent.ACTION_SEND_MULTIPLE
                                                type = "text/plain"
                                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, filesToShare)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Hata Raporunu Paylaş"))
                                        } else {
                                            Toast.makeText(context, "Paylaşılacak log dosyası bulunamadı", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Paylaşım Hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Hata Raporunu Geliştiriciye Gönder", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Libraries Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Açık Kaynak Kütüphaneler",
                        color = KitsugiColors.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Libraries List
            items(libraries.size) { index ->
                val lib = libraries[index]
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = KitsugiColors.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .tvClickable { openUrl(lib.url) }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = lib.name,
                            color = KitsugiColors.textPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lib.desc,
                            color = KitsugiColors.textMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Made with ❤️ by $developer",
                    color = KitsugiColors.textMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showDeveloperLogs) {
        DeveloperLogsDialog(
            onDismiss = { showDeveloperLogs = false }
        )
    }
}

data class LibraryInfo(
    val name: String,
    val desc: String,
    val url: String
)
