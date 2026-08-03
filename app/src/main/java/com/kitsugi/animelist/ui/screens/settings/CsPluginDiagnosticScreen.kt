package com.kitsugi.animelist.ui.screens.settings

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitsugi.animelist.data.cloudstream.CsPluginDiagnosticRunner
import com.kitsugi.animelist.data.cloudstream.CsPluginDiagnosticViewModel
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import java.io.File

/**
 * In-App CS Plugin Tanı Ekranı.
 *
 * Uygulama açıkken tüm 201 Türkçe eklentiyi E2E test eder.
 * Arkaplan testinden farklı olarak CloudflareKiller aktif session cookie'leri ile
 * çalıştığından CF/WAF engellerini büyük ölçüde aşar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsPluginDiagnosticScreen(
    onDismiss: () -> Unit,
    vm: CsPluginDiagnosticViewModel = viewModel()
) {
    val context    = LocalContext.current
    val accent     = LocalKitsugiAccent.current
    val progress   by vm.progress.collectAsState()
    val results    by vm.results.collectAsState()
    val isRunning  by vm.isRunning.collectAsState()
    val reportPath by vm.reportPath.collectAsState()

    // Özet sayaçlar
    val working   = results.count { it.streamCount > 0 }
    val noStream  = results.count { it.loaded && it.searchCount > 0 && it.streamCount == 0 }
    val cfBlocked = results.count { it.loaded && it.searchCount == 0 }
    val dead      = results.count { !it.loaded }

    Dialog(
        onDismissRequest = {
            if (!isRunning) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isRunning
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(KitsugiColors.Surface)
            ) {
                // ── Header ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KitsugiColors.SurfaceSoft)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.BugReport,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Plugin Tanı Modu",
                            color = KitsugiColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        val subtitleText = if (vm.onlyInstalled) {
                            "CF bypass aktif • Sadece yüklü eklentiler"
                        } else {
                            "CF bypass aktif • ${CsPluginDiagnosticRunner.REPOS.size} repo • Tüm havuz"
                        }
                        Text(
                            subtitleText,
                            color = KitsugiColors.TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    if (!isRunning) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = KitsugiColors.TextSecondary)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── Toggle Option ────────────────────────────────────────
                    if (!isRunning) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(KitsugiColors.SurfaceSoft)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Sadece Yüklü Eklentiler",
                                        color = KitsugiColors.TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "GitHub'dan indirmeden sadece yerel eklentileri hızlıca test eder.",
                                        color = KitsugiColors.TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = vm.onlyInstalled,
                                    onCheckedChange = { vm.onlyInstalled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = KitsugiColors.Surface,
                                        checkedTrackColor = accent
                                    )
                                )
                            }
                        }
                    }

                    // ── Progress card ────────────────────────────────────────
                    item {
                        AnimatedVisibility(visible = isRunning || progress != null) {
                            ProgressCard(progress = progress, isRunning = isRunning, accent = accent)
                        }
                    }

                    // ── Summary cards ────────────────────────────────────────
                    if (results.isNotEmpty()) {
                        item {
                            SummaryCards(working, noStream, cfBlocked, dead, accent)
                        }
                    }

                    // ── Info banner (başlamadan önce) ────────────────────────
                    if (!isRunning && results.isEmpty() && progress == null) {
                        item {
                            InfoBanner(accent, vm.onlyInstalled)
                        }
                    }

                    // ── Results list ─────────────────────────────────────────
                    if (results.isNotEmpty()) {
                        item {
                            Text(
                                "Detaylı Sonuçlar (${results.size} eklenti)",
                                color = KitsugiColors.TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                        items(results, key = { it.pluginId + it.repoSlug }) { result ->
                            ResultRow(result = result, accent = accent)
                        }
                    }
                }

                // ── Bottom buttons ───────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KitsugiColors.SurfaceSoft)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isRunning) {
                        Button(
                            onClick = { vm.startDiagnostic(context) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (results.isEmpty()) "🔬 Tanıyı Başlat" else "🔄 Yeniden Başlat", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { vm.cancelDiagnostic() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = KitsugiColors.AccentRed)
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Durdur", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Raporu paylaş butonu
                    if (reportPath != null && !isRunning) {
                        OutlinedButton(
                            onClick = {
                                val file = File(reportPath!!)
                                if (file.exists()) {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Raporu Paylaş"))
                                    } catch (e: Exception) {
                                        android.util.Log.e("CsPluginDiagnostic", "Rapor paylaşılamadı: ${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Raporu Paylaş (.md)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun ProgressCard(
    progress: CsPluginDiagnosticRunner.DiagnosticProgress?,
    isRunning: Boolean,
    accent: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KitsugiColors.SurfaceSoft)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isRunning) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = pulseAlpha))
                )
            }
            Text(
                if (isRunning) "Test Çalışıyor..." else "Test Tamamlandı",
                color = if (isRunning) accent else KitsugiColors.AccentGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (progress != null) {
            // Plugin adı
            Text(
                "▶ ${progress.currentPlugin} — ${progress.phase}",
                color = KitsugiColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Progress bar
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.15f)
            )

            // Sayaç
            Text(
                "${progress.current} / ${progress.total} eklenti",
                color = KitsugiColors.TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SummaryCards(
    working: Int, noStream: Int, cfBlocked: Int, dead: Int,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip("✅", working.toString(), "Çalışıyor", KitsugiColors.AccentGreen, Modifier.weight(1f))
        SummaryChip("⚠️", noStream.toString(), "0 Stream", KitsugiColors.AccentOrange, Modifier.weight(1f))
        SummaryChip("🔍", cfBlocked.toString(), "CF Engel", KitsugiColors.AccentBlue, Modifier.weight(1f))
        SummaryChip("❌", dead.toString(), "Bozuk", KitsugiColors.AccentRed, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryChip(
    emoji: String,
    count: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(count, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text(label, color = KitsugiColors.TextMuted, fontSize = 9.sp)
    }
}

@Composable
private fun InfoBanner(accent: Color, onlyInstalled: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🔬 In-App Plugin Tanı", color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            "Bu tanı, uygulama açıkken çalışır — CloudflareKiller aktif session cookies ile CF/WAF engellerini " +
            "büyük ölçüde aşar. Arkaplan (ADB) testinden çok daha fazla plugin geçer.",
            color = KitsugiColors.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        val bulletPoints = if (onlyInstalled) {
            "• Sadece yüklü/aktif eklentiler yerel olarak test edilir (hızlı)\n" +
            "• Search → Load → Stream zinciri (İndirme aşaması atlanır)\n" +
            "• Sonuç: .md rapor + uygulama içi görünüm"
        } else {
            "• 201 eklenti paralel test edilir (MAX=${ com.kitsugi.animelist.data.cloudstream.CsPluginDiagnosticRunner.REPOS.size } repo)\n" +
            "• Download → Search → Load → Stream zinciri\n" +
            "• Sonuç: .md rapor + uygulama içi görünüm"
        }
        Text(
            bulletPoints,
            color = KitsugiColors.TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun ResultRow(
    result: CsPluginDiagnosticRunner.DiagnosticResult,
    accent: Color
) {
    val (statusIcon, statusColor) = when (result.status) {
        CsPluginDiagnosticRunner.ResultStatus.WORKING     -> "✅" to KitsugiColors.AccentGreen
        CsPluginDiagnosticRunner.ResultStatus.NO_STREAMS  -> "⚠️" to KitsugiColors.AccentOrange
        CsPluginDiagnosticRunner.ResultStatus.CF_BLOCKED  -> "🔍" to KitsugiColors.AccentBlue
        CsPluginDiagnosticRunner.ResultStatus.LOAD_FAILED -> "⚠️" to KitsugiColors.AccentOrange
        CsPluginDiagnosticRunner.ResultStatus.DEAD        -> "❌" to KitsugiColors.AccentRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KitsugiColors.SurfaceSoft)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(statusIcon, fontSize = 16.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                result.displayName,
                color = KitsugiColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                result.repoSlug.substringBefore("/"),
                color = KitsugiColors.TextMuted,
                fontSize = 10.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (result.streamCount > 0) {
                Text(
                    "${result.streamCount} stream",
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (result.error != null) {
                Text(
                    result.error.take(20),
                    color = statusColor,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "🔍${result.searchCount}",
                color = KitsugiColors.TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
