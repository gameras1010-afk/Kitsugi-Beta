package com.kitsugi.animelist.ui.screens.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperLogsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var logLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var crashReport by remember { mutableStateOf<String?>(null) }

    val fetchLogs = suspend {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                // Fetch crash report if exists
                val crashFile = File(context.filesDir, "crash_log.txt")
                crashReport = if (crashFile.exists()) crashFile.readText() else null

                // Fetch logcat
                val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "*:D"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val lines = mutableListOf<String>()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    lines.add(line!!)
                }
                reader.close()
                logLines = lines.takeLast(1500).reversed()
            } catch (e: Exception) {
                logLines = listOf("Loglar okunamadı: ${e.localizedMessage}")
            } finally {
                isLoading = false
            }
        }
    }

    val clearLogs = suspend {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                Runtime.getRuntime().exec(arrayOf("logcat", "-c"))
                val crashFile = File(context.filesDir, "crash_log.txt")
                if (crashFile.exists()) crashFile.delete()
                crashReport = null
                logLines = emptyList()
            } catch (e: Exception) {
                logLines = listOf("Temizleme başarısız: ${e.localizedMessage}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchLogs()
    }

    val filteredLines = remember(logLines, searchQuery) {
        if (searchQuery.isBlank()) {
            logLines
        } else {
            logLines.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = KitsugiColors.Surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Geliştirici Günlükleri",
                            color = KitsugiColors.TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Uygulama içi Logcat ve hata kayıtları",
                            color = KitsugiColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { scope.launch { fetchLogs() } },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = KitsugiColors.SurfaceStrong)
                        ) {
                            Icon(Icons.Rounded.Refresh, "Yenile", tint = KitsugiColors.TextPrimary)
                        }

                        IconButton(
                            onClick = {
                                val textToCopy = buildString {
                                    if (crashReport != null) {
                                        append("=== SON ÇÖKME RAPORU ===\n")
                                        append(crashReport)
                                        append("\n=========================\n\n")
                                    }
                                    append(filteredLines.joinToString("\n"))
                                }
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                Toast.makeText(context, "Loglar panoya kopyalandı", Toast.LENGTH_SHORT).show()
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = KitsugiColors.SurfaceStrong)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, "Kopyala", tint = KitsugiColors.TextPrimary)
                        }

                        IconButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val filesToShare = ArrayList<android.net.Uri>()

                                        // 1. Cihaz ve Uygulama Bilgisi
                                        val infoFile = File(context.filesDir, "device_info.txt")
                                        val infoText = buildString {
                                            append("Uygulama Sürümü: ${com.kitsugi.animelist.BuildConfig.VERSION_NAME} (${com.kitsugi.animelist.BuildConfig.VERSION_CODE})\n")
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

                                        // 2. Son Çökme Kaydı
                                        val crashFile = File(context.filesDir, "crash_log.txt")
                                        if (crashFile.exists()) {
                                            val crashUri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "com.kitsugi.animelist.fileprovider",
                                                crashFile
                                            )
                                            filesToShare.add(crashUri)
                                        }

                                        // 3. Arka Planda Dönen Log Dosyaları (Rotasyon Dahil)
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

                                        // 4. Canlı Anlık Logcat Dökümü
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
                                            val intent = android.content.Intent().apply {
                                                action = android.content.Intent.ACTION_SEND_MULTIPLE
                                                type = "text/plain"
                                                putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, filesToShare)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(intent, "Logları Paylaş"))
                                        } else {
                                            Toast.makeText(context, "Paylaşılacak log dosyası bulunamadı", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Paylaşım Hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = KitsugiColors.SurfaceStrong)
                        ) {
                            Icon(Icons.Rounded.Share, "Paylaş", tint = KitsugiColors.TextPrimary)
                        }

                        IconButton(
                            onClick = { scope.launch { clearLogs() } },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = KitsugiColors.SurfaceStrong)
                        ) {
                            Icon(Icons.Rounded.Delete, "Temizle", tint = KitsugiColors.AccentRed)
                        }

                        IconButton(
                            onClick = onDismiss,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = KitsugiColors.SurfaceStrong)
                        ) {
                            Icon(Icons.Rounded.Close, "Kapat", tint = KitsugiColors.TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Loglarda ara (örn. TurkAnime, Hata, Extractor...)", color = KitsugiColors.TextMuted, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = KitsugiColors.TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, null, tint = KitsugiColors.TextSecondary)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = KitsugiColors.Border,
                        focusedContainerColor = KitsugiColors.Background,
                        unfocusedContainerColor = KitsugiColors.Background,
                        focusedTextColor = KitsugiColors.TextPrimary,
                        unfocusedTextColor = KitsugiColors.TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Show Crash Report Warning if exists
                if (crashReport != null) {
                    var isCrashExpanded by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .tvClickable(shape = RoundedCornerShape(12.dp)) { isCrashExpanded = !isCrashExpanded },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = KitsugiColors.AccentRed.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Warning, null, tint = KitsugiColors.AccentRed)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Son Çökme Raporu Algılandı",
                                        color = KitsugiColors.AccentRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (isCrashExpanded) "Kapatmak için tıklayın" else "Detayları görmek için tıklayın",
                                        color = KitsugiColors.TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (isCrashExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                SelectionContainer {
                                    Text(
                                        text = crashReport ?: "",
                                        color = KitsugiColors.TextPrimary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                    )
                                }
                            }
                        }
                    }
                }

                // Log display
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(KitsugiColors.Background)
                        .padding(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = accentColor,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (filteredLines.isEmpty()) {
                        Text(
                            text = "Eşleşen log kaydı bulunamadı.",
                            color = KitsugiColors.TextMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        SelectionContainer {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(filteredLines) { line ->
                                    val color = when {
                                        line.contains(" E/") || line.contains(" E ") || line.contains("KRİTİK HATA") || line.contains("FATAL") -> KitsugiColors.AccentRed
                                        line.contains(" W/") || line.contains(" W ") -> KitsugiColors.AccentOrange
                                        line.contains(" D/") || line.contains(" D ") -> KitsugiColors.AccentBlue
                                        line.contains(" I/") || line.contains(" I ") -> KitsugiColors.AccentPurple
                                        else -> KitsugiColors.TextSecondary
                                    }
                                    Text(
                                        text = line,
                                        color = color,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
