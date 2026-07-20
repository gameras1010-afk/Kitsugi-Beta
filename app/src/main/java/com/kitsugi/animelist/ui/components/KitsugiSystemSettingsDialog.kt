package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.BuildConfig
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiSystemSettingsDialog(
    totalEntryCount: Int,
    onExportFileClick: () -> Unit,
    onImportFileClick: () -> Unit,
    onDeleteAllClick: () -> Unit,
    dnsChoice: Int,
    onDnsChoiceSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()

    KitsugiSheetOrDialog(
        onDismiss = onDismiss,
        heightFraction = 0.85f
    ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sistem & Veri Ayarları",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = KitsugiColors.TextSecondary)
                    }
                }
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = KitsugiColors.Surface,
                    contentColor = accentColor
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        text = {
                            Text(
                                "Veri Yönetimi",
                                fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        text = {
                            Text(
                                "DNS (DoH)",
                                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Body
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) { page ->
                when (page) {
                    0 -> DataManagementTab(
                        totalEntryCount = totalEntryCount,
                        onExportFileClick = onExportFileClick,
                        onImportFileClick = onImportFileClick,
                        onDeleteAllClick = onDeleteAllClick,
                        accentColor = accentColor
                    )
                    1 -> DnsSettingsTab(
                        dnsChoice = dnsChoice,
                        onDnsChoiceSelected = onDnsChoiceSelected,
                        accentColor = accentColor
                    )
                }
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Tamam", color = accentColor, fontWeight = FontWeight.SemiBold)
                }
            }
    }
}


@Composable
private fun DataManagementTab(
    totalEntryCount: Int,
    onExportFileClick: () -> Unit,
    onImportFileClick: () -> Unit,
    onDeleteAllClick: () -> Unit,
    accentColor: Color
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        KitsugiSettingsSection(title = "Yedekleme & Sıfırlama") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Yerel Room veritabanındaki liste kayıtlarını JSON dosyası olarak dışa aktarabilir veya yedek dosyasından geri yükleyebilirsin.",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Toplam kayıt: $totalEntryCount",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Not: Dışa/içe aktarma sadece liste kayıtlarını kapsar. Tema ve uygulama ayarları ayrı saklanır.",
                    color = KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    enabled = totalEntryCount > 0,
                    onClick = onExportFileClick,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("JSON Dosyası Olarak Dışa Aktar", color = KitsugiColors.Background, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = onImportFileClick,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("JSON Dosyasından İçe Aktar", color = KitsugiColors.Background, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    enabled = totalEntryCount > 0,
                    onClick = onDeleteAllClick,
                    colors = ButtonDefaults.buttonColors(containerColor = KitsugiColors.AccentRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tüm Listeyi Sil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DnsSettingsTab(
    dnsChoice: Int,
    onDnsChoiceSelected: (Int) -> Unit,
    accentColor: Color
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        KitsugiSettingsSection(title = "DNS over HTTPS (DoH)") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Türkiye'deki servis sağlayıcı engellerini aşmak ve anime/manga kaynaklarına daha güvenli, kesintisiz erişmek için bir DoH sağlayıcısı seçin.",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                dnsOptions.forEach { dnsOpt ->
                    val isSelected = dnsOpt.id == dnsChoice.toString()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSelected) KitsugiColors.SurfaceSoft else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .tvClickable(shape = RoundedCornerShape(12.dp)) { onDnsChoiceSelected(dnsOpt.id.toIntOrNull() ?: 0) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onDnsChoiceSelected(dnsOpt.id.toIntOrNull() ?: 0) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = KitsugiColors.TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = dnsOpt.title,
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = dnsOpt.description,
                                color = KitsugiColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private val dnsOptions = listOf(
    KitsugiChoiceOption(id = "0", title = "Sistem Varsayılanı", description = "İnternet sağlayıcınızın varsayılan DNS adresini kullanır."),
    KitsugiChoiceOption(id = "1", title = "Google DNS", description = "Güvenli ve hızlı Google DoH sunucularını kullanır."),
    KitsugiChoiceOption(id = "2", title = "Cloudflare DNS", description = "Gizlilik odaklı ve hızlı Cloudflare DoH sunucularını kullanır."),
    KitsugiChoiceOption(id = "3", title = "AdGuard DNS", description = "Reklam ve takipçi engelleyici özellikli AdGuard DoH sunucularını kullanır."),
    KitsugiChoiceOption(id = "4", title = "DNS.WATCH", description = "Sansürsüz ve bağımsız DNS.WATCH DoH sunucularını kullanır."),
    KitsugiChoiceOption(id = "5", title = "Quad9 DNS", description = "Zararlı yazılım korumalı ve güvenli Quad9 DoH sunucularını kullanır."),
    KitsugiChoiceOption(id = "6", title = "DNS.SB", description = "Gizlilik odaklı, log tutmayan DNS.SB DoH sunucularını kullanır."),
    KitsugiChoiceOption(id = "7", title = "Canadian Shield", description = "CIRA tarafından sunulan Kanada merkezli korumalı DoH sunucularını kullanır.")
)
