package com.kitsugi.animelist.ui.tv.manga

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.manga.MangaSource
import com.kitsugi.animelist.data.manga.model.SourceHealthStatus
import com.kitsugi.animelist.data.manga.model.SourceRuntimeStats
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames

/**
 * TV-native Kaynak Sağlığı Ekranı.
 *
 * Tüm kurulu manga kaynaklarının sağlık durumunu gösterir:
 * - Hızlı kontrol (Enter)
 * - Runtime istatistikleri (success/failure sayıları, ortalama süreler)
 * - Sağlık durumu badge'leri (Healthy, Degraded, Broken, CAPTCHA, …)
 *
 * @param sources            Kurulu manga kaynakları
 * @param onGetSourceHealth  Kaynak sağlık durumunu döndürür
 * @param onGetRuntimeStats  Kaynak runtime istatistiklerini döndürür
 * @param onQuickCheckSource Anlık sağlık kontrolü tetikler
 * @param onRefreshMirror    Kaynak yedek URL'sini yeniler
 * @param onClearDiagnostics Kaynak tanı verilerini sıfırlar
 * @param onBack             Geri navigasyon callback'i
 */
@Composable
fun TvMangaSourceHealthScreen(
    sources: List<MangaSource> = emptyList(),
    checkingSource: MangaSource? = null,
    onGetSourceHealth: (MangaSource) -> SourceHealthStatus = { SourceHealthStatus.Unknown },
    onGetRuntimeStats: (MangaSource) -> SourceRuntimeStats = { SourceRuntimeStats() },
    onQuickCheckSource: (MangaSource) -> Unit = {},
    onRefreshMirror: (MangaSource) -> Unit = {},
    onClearDiagnostics: (MangaSource) -> Unit = {},
    onClearAllDiagnostics: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    BackHandler(enabled = true) { onBack() }

    var selectedSource by remember { mutableStateOf<MangaSource?>(null) }
    val listFocusRequester = remember { FocusRequester() }

    // İlk kaynak focus: layout tree'ye attach olduktan sonra focus iste
    LaunchedEffect(Unit) {
        listFocusRequester.requestFocusAfterFrames(frames = 2)
    }

    // Özet sayıları
    val healthyCount = remember(sources) { sources.count { onGetSourceHealth(it) == SourceHealthStatus.Healthy } }
    val degradedCount = remember(sources) { sources.count { onGetSourceHealth(it) == SourceHealthStatus.Degraded } }
    val brokenCount = remember(sources) {
        sources.count {
            onGetSourceHealth(it) in listOf(
                SourceHealthStatus.Broken,
                SourceHealthStatus.CaptchaRequired,
                SourceHealthStatus.RateLimited
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
    ) {
        // Arka plan gradyanı
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0D2010).copy(alpha = 0.5f), Color.Transparent),
                        radius = 900f
                    )
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // ── Sol: Kaynak Listesi ────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .background(KitsugiColors.SurfaceSoft.copy(alpha = 0.85f))
                    .focusRequester(listFocusRequester),
                contentPadding = PaddingValues(top = 40.dp, bottom = 24.dp, start = 12.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Başlık
                item {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.HealthAndSafety,
                                null,
                                tint = KitsugiColors.AccentGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                "Kaynak Sağlığı",
                                color = KitsugiColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        // Özet rozetleri
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TvHealthSummaryBadge("✓ $healthyCount", KitsugiColors.AccentGreen)
                            TvHealthSummaryBadge("~ $degradedCount", Color(0xFFFF9800))
                            TvHealthSummaryBadge("✗ $brokenCount", KitsugiColors.AccentRed)
                        }
                        Spacer(Modifier.height(8.dp))
                        // Tümünü Temizle
                        TvHealthActionRow(
                            icon = Icons.Rounded.ClearAll,
                            label = "Tüm Tanıları Sıfırla",
                            color = KitsugiColors.TextMuted,
                            onClick = onClearAllDiagnostics
                        )
                        Spacer(Modifier.height(4.dp))
                        // Geri
                        TvHealthActionRow(
                            icon = Icons.Rounded.ArrowBack,
                            label = "Geri",
                            color = KitsugiColors.TextMuted,
                            onClick = onBack
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // Boş durum
                if (sources.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.SearchOff,
                                    null,
                                    tint = KitsugiColors.TextMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text("Kurulu kaynak bulunamadı", color = KitsugiColors.TextMuted)
                            }
                        }
                    }
                }

                // Kaynak listesi — sağlık durumuna göre sıralı
                items(
                    sources.sortedWith(
                        compareBy {
                            when (onGetSourceHealth(it)) {
                                SourceHealthStatus.Broken -> 0
                                SourceHealthStatus.CaptchaRequired -> 1
                                SourceHealthStatus.RateLimited -> 2
                                SourceHealthStatus.Degraded -> 3
                                SourceHealthStatus.Unknown -> 4
                                SourceHealthStatus.Healthy -> 5
                                SourceHealthStatus.Disabled -> 6
                            }
                        }
                    ),
                    key = { it.name }
                ) { source ->
                    val health = onGetSourceHealth(source)
                    val isChecking = checkingSource?.name == source.name
                    val isSelected = selectedSource?.name == source.name

                    TvHealthSourceRow(
                        source = source,
                        health = health,
                        isChecking = isChecking,
                        isSelected = isSelected,
                        onSelect = { selectedSource = source }
                    )
                }
            }

            // ── Sağ: Detay Paneli ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 32.dp, vertical = 40.dp)
            ) {
                if (selectedSource == null) {
                    // Boş seçim durumu
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.HealthAndSafety,
                                null,
                                tint = KitsugiColors.TextMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                "Detayları görmek için\nbir kaynak seçin",
                                color = KitsugiColors.TextMuted,
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                } else {
                    val src = selectedSource!!
                    val health = onGetSourceHealth(src)
                    val stats = onGetRuntimeStats(src)
                    val isChecking = checkingSource?.name == src.name

                    TvHealthDetailPanel(
                        source = src,
                        health = health,
                        stats = stats,
                        isChecking = isChecking,
                        onQuickCheck = { onQuickCheckSource(src) },
                        onRefreshMirror = { onRefreshMirror(src) },
                        onClearDiagnostics = { onClearDiagnostics(src) }
                    )
                }
            }
        }
    }
}

// ── SummaryBadge ───────────────────────────────────────────────────────────

@Composable
private fun TvHealthSummaryBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ── ActionRow (sidebar) ────────────────────────────────────────────────────

@Composable
private fun TvHealthActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (hasFocus) 1.03f else 1f, tween(150))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(if (hasFocus) color.copy(alpha = 0.15f) else Color.Transparent)
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusable()
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                    event.nativeKeyEvent.keyCode in listOf(
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER
                    )
                ) { onClick(); true } else false
            }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ── SourceRow (sol liste) ──────────────────────────────────────────────────

@Composable
private fun TvHealthSourceRow(
    source: MangaSource,
    health: SourceHealthStatus,
    isChecking: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        when {
            isSelected -> KitsugiColors.AccentBlue.copy(alpha = 0.18f)
            hasFocus -> KitsugiColors.SurfaceStrong
            else -> Color.Transparent
        },
        tween(150)
    )

    val (_, healthColor) = healthLabel(health)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                1.dp,
                if (isSelected) KitsugiColors.AccentBlue.copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusable()
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                    event.nativeKeyEvent.keyCode in listOf(
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_DPAD_RIGHT
                    )
                ) { onSelect(); true } else false
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sağlık renk çubuğu
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(healthColor)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                source.name,
                color = KitsugiColors.TextPrimary,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                source.lang.uppercase(),
                color = KitsugiColors.TextMuted,
                fontSize = 10.sp
            )
        }
        if (isChecking) {
            CircularProgressIndicator(
                color = KitsugiColors.AccentBlue,
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp
            )
        } else {
            val (label, color) = healthLabel(health)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── DetailPanel (sağ panel) ────────────────────────────────────────────────

@Composable
private fun TvHealthDetailPanel(
    source: MangaSource,
    health: SourceHealthStatus,
    stats: SourceRuntimeStats,
    isChecking: Boolean,
    onQuickCheck: () -> Unit,
    onRefreshMirror: () -> Unit,
    onClearDiagnostics: () -> Unit
) {
    val (healthText, healthColor) = healthLabel(health)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Başlık + sağlık
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(KitsugiColors.AccentBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.MenuBook,
                    null,
                    tint = KitsugiColors.AccentBlue,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    source.name,
                    color = KitsugiColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    "Dil: ${source.lang.uppercase()} · ${source.baseUrl}",
                    color = KitsugiColors.TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Sağlık badge'i
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(healthColor.copy(alpha = 0.15f))
                    .border(1.dp, healthColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isChecking) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            color = KitsugiColors.AccentBlue,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Kontrol ediliyor...", color = KitsugiColors.AccentBlue, fontSize = 11.sp)
                    }
                } else {
                    Text(healthText, color = healthColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Aksiyon butonları
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TvHealthDetailButton(
                label = "Hızlı Kontrol",
                icon = Icons.Rounded.PlayArrow,
                color = KitsugiColors.AccentBlue,
                onClick = onQuickCheck
            )
            TvHealthDetailButton(
                label = "Ayna Yenile",
                icon = Icons.Rounded.Refresh,
                color = Color(0xFFFF9800),
                onClick = onRefreshMirror
            )
            TvHealthDetailButton(
                label = "Tanıları Sıfırla",
                icon = Icons.Rounded.ClearAll,
                color = KitsugiColors.TextMuted,
                onClick = onClearDiagnostics
            )
        }

        // İstatistik kartları
        Text(
            "Runtime İstatistikleri",
            color = KitsugiColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(KitsugiColors.SurfaceSoft)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Başarı / Başarısız
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TvHealthStatBox(
                    label = "Başarılı",
                    value = "${stats.successCount}",
                    color = KitsugiColors.AccentGreen,
                    modifier = Modifier.weight(1f)
                )
                TvHealthStatBox(
                    label = "Başarısız",
                    value = "${stats.failureCount}",
                    color = KitsugiColors.AccentRed,
                    modifier = Modifier.weight(1f)
                )
            }
            // Ortalama süreler
            if (stats.avgSearchMs > 0 || stats.avgDetailsMs > 0) {
                TvHealthStatRow("Ortalama Arama", "${stats.avgSearchMs} ms")
                TvHealthStatRow("Ortalama Detay", "${stats.avgDetailsMs} ms")
                TvHealthStatRow("Ortalama Bölüm Listesi", "${stats.avgChapterMs} ms")
                TvHealthStatRow("Ortalama Sayfa Listesi", "${stats.avgPageMs} ms")
                TvHealthStatRow("Ortalama Görsel", "${stats.avgImageMs} ms")
            } else {
                Text(
                    "Henüz istatistik yok. Hızlı kontrol yapın.",
                    color = KitsugiColors.TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Sağlık durumu açıklaması
        val healthDescription = when (health) {
            SourceHealthStatus.Healthy -> "Bu kaynak tam olarak çalışıyor. Arama, bölüm listesi ve sayfa yükleme başarılı."
            SourceHealthStatus.Degraded -> "Kaynak yavaş ya da tutarsız yanıt veriyor. Bazı istekler zaman aşımına uğruyor olabilir."
            SourceHealthStatus.Broken -> "Kaynak şu an erişilemiyor. URL değişmiş veya site kapalı olabilir."
            SourceHealthStatus.CaptchaRequired -> "Kaynak CAPTCHA doğrulaması gerektiriyor. WebView ile açılması gerekebilir."
            SourceHealthStatus.RateLimited -> "Kaynak çok fazla istek yapıldığı için geçici olarak engelledi. Birkaç dakika bekleyin."
            SourceHealthStatus.Disabled -> "Bu kaynak devre dışı bırakılmış."
            SourceHealthStatus.Unknown -> "Sağlık durumu henüz kontrol edilmedi. Hızlı kontrol yapın."
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KitsugiColors.SurfaceSoft)
                .padding(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    Icons.Rounded.Info,
                    null,
                    tint = KitsugiColors.TextMuted,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Text(healthDescription, color = KitsugiColors.TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

// ── DetailButton ───────────────────────────────────────────────────────────

@Composable
private fun TvHealthDetailButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (hasFocus) 1.05f else 1f, tween(150))

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(if (hasFocus) color.copy(alpha = 0.2f) else color.copy(alpha = 0.1f))
            .border(1.dp, if (hasFocus) color.copy(alpha = 0.7f) else Color.Transparent, RoundedCornerShape(10.dp))
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusable()
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                    event.nativeKeyEvent.keyCode in listOf(
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER
                    )
                ) { onClick(); true } else false
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── StatBox ve StatRow ─────────────────────────────────────────────────────

@Composable
private fun TvHealthStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = KitsugiColors.TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun TvHealthStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = KitsugiColors.TextSecondary, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(KitsugiColors.SurfaceStrong)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(value, color = KitsugiColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Yardımcı: healthLabel ──────────────────────────────────────────────────

@Composable
private fun healthLabel(health: SourceHealthStatus): Pair<String, Color> = when (health) {
    SourceHealthStatus.Healthy -> "Sağlıklı" to KitsugiColors.AccentGreen
    SourceHealthStatus.Degraded -> "Yavaş" to Color(0xFFFF9800)
    SourceHealthStatus.Broken -> "Bozuk" to KitsugiColors.AccentRed
    SourceHealthStatus.CaptchaRequired -> "CAPTCHA" to Color(0xFFFF9800)
    SourceHealthStatus.RateLimited -> "Limit Aşıldı" to Color(0xFFFF9800)
    SourceHealthStatus.Disabled -> "Devre Dışı" to KitsugiColors.TextMuted
    SourceHealthStatus.Unknown -> "Bilinmiyor" to KitsugiColors.TextMuted
}
