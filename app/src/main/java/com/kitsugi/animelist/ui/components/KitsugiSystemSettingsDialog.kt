package com.kitsugi.animelist.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.kitsugi.animelist.BuildConfig
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    download: com.kitsugi.animelist.ui.screens.settings.DownloadSettings,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val dataManagementScrollState = rememberScrollState()
    val dnsSettingsScrollState = rememberScrollState()
    val storageSettingsScrollState = rememberScrollState()

    val activeScrollState = when (pagerState.currentPage) {
        0 -> dataManagementScrollState
        1 -> dnsSettingsScrollState
        else -> storageSettingsScrollState
    }

    KitsugiSheetOrDialog(
        onDismiss = onDismiss,
        heightFraction = 0.85f,
        innerColumnScrollState = activeScrollState,
        sheetGesturesEnabled = false
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
                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(2) }
                    },
                    text = {
                        Text(
                            "İndirmeler",
                            fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Normal,
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
                    accentColor = accentColor,
                    scrollState = dataManagementScrollState
                )
                1 -> DnsSettingsTab(
                    dnsChoice = dnsChoice,
                    onDnsChoiceSelected = onDnsChoiceSelected,
                    accentColor = accentColor,
                    scrollState = dnsSettingsScrollState
                )
                2 -> StorageSettingsTab(
                    download = download,
                    accentColor = accentColor,
                    scrollState = storageSettingsScrollState
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
    accentColor: Color,
    scrollState: ScrollState = rememberScrollState()
) {
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
    accentColor: Color,
    scrollState: ScrollState = rememberScrollState()
) {
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

@Composable
private fun StorageSettingsTab(
    download: com.kitsugi.animelist.ui.screens.settings.DownloadSettings,
    accentColor: Color,
    scrollState: ScrollState = rememberScrollState()
) {
    val context = LocalContext.current
    val pm = context.packageManager

    // Launcher for directory picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            download.onCustomImageDownloadUriChanged(it.toString())
        }
    }

    val videoFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            download.onVideoDownloadUriChanged(it.toString())
        }
    }

    val displayPath = if (download.customImageDownloadUri.isBlank()) {
        "Varsayılan (İndirilenler / Kitsugi)"
    } else {
        try {
            val treeUri = Uri.parse(download.customImageDownloadUri)
            val docFile = DocumentFile.fromTreeUri(context, treeUri)
            docFile?.name ?: treeUri.lastPathSegment ?: download.customImageDownloadUri
        } catch (e: Exception) {
            download.customImageDownloadUri
        }
    }

    val displayVideoPath = if (download.videoDownloadUri.isBlank()) {
        "Varsayılan (İndirilenler / Kitsugi / Video)"
    } else {
        try {
            val treeUri = Uri.parse(download.videoDownloadUri)
            val docFile = DocumentFile.fromTreeUri(context, treeUri)
            docFile?.name ?: treeUri.lastPathSegment ?: download.videoDownloadUri
        } catch (e: Exception) {
            download.videoDownloadUri
        }
    }

    // Dynamic external downloader detection
    val is1dmInstalled = remember(context) {
        isPackageInstalled("idm.internet.download.manager", pm) ||
        isPackageInstalled("idm.internet.download.manager.plus", pm)
    }
    val isAdmInstalled = remember(context) {
        isPackageInstalled("com.dv.adm", pm) ||
        isPackageInstalled("com.dv.adm.pay", pm)
    }

    val localDownloaderOptions = remember(is1dmInstalled, isAdmInstalled) {
        listOf(
            "INTERNAL" to "Dahili İndirici (FFmpeg)",
            "EXTERNAL_1DM" to "1DM / 1DM+" + (if (is1dmInstalled) " (Yüklü)" else " (Yüklü Değil)"),
            "EXTERNAL_ADM" to "ADM" + (if (isAdmInstalled) " (Yüklü)" else " (Yüklü Değil)"),
            "EXTERNAL_SYSTEM" to "Sistem Varsayılanı / İndirici Seçici"
        )
    }

    var activePicker by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. İndirme Konumları ---
        KitsugiSettingsSection(title = "İndirme Konumları") {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Resim indirme konumu
                Text(
                    text = "Resim İndirme Konumu",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KitsugiColors.SurfaceSoft, shape = RoundedCornerShape(12.dp))
                        .tvClickable(shape = RoundedCornerShape(12.dp)) { folderPickerLauncher.launch(null) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(displayPath, color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
                if (download.customImageDownloadUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { download.onCustomImageDownloadUriChanged("") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Resim Konumunu Sıfırla", color = KitsugiColors.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Video indirme konumu
                Text(
                    text = "Video İndirme Konumu",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KitsugiColors.SurfaceSoft, shape = RoundedCornerShape(12.dp))
                        .tvClickable(shape = RoundedCornerShape(12.dp)) { videoFolderPickerLauncher.launch(null) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(displayVideoPath, color = KitsugiColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
                if (download.videoDownloadUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { download.onVideoDownloadUriChanged("") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Video Konumunu Sıfırla", color = KitsugiColors.TextSecondary)
                    }
                }
            }
        }

        // --- 2. Genel İndirme Ayarları ---
        KitsugiSettingsSection(title = "Genel İndirme Ayarları") {
            Column(modifier = Modifier.fillMaxWidth()) {
                KitsugiSwitchSettingItem(
                    title = "Sadece Wi-Fi üzerinden indir",
                    description = "Mobil veri kullanımını önlemek için indirmeleri Wi-Fi ile sınırlar.",
                    checked = download.downloadOnlyOverWifi,
                    onCheckedChange = download.onDownloadOnlyOverWifiChanged
                )

                KitsugiSwitchSettingItem(
                    title = "Anime için her zaman harici indirme yöneticisi kullan",
                    description = "Aktif edildiğinde anime indirmeleri doğrudan harici yöneticiye yönlendirilir.",
                    checked = download.useExternalDownloader,
                    onCheckedChange = download.onUseExternalDownloaderChanged
                )

                KitsugiClickableSettingItem(
                    title = "Harici indirme yöneticisi tercihi",
                    description = "Tercih ettiğiniz harici indirme aracını seçin.",
                    currentValue = localDownloaderOptions.firstOrNull { it.first == download.downloaderPreference }?.second,
                    onClick = { activePicker = "downloader" }
                )

                KitsugiClickableSettingItem(
                    title = "Hız sınırı",
                    description = "Maksimum indirme hızını sınırlayın.",
                    currentValue = speedLimitOptions.firstOrNull { it.first == download.downloadSpeedLimit }?.second,
                    onClick = { activePicker = "speed_limit" }
                )

                KitsugiSwitchSettingItem(
                    title = "Bölümleri CBZ olarak kaydet",
                    description = "Manga bölümlerini arşivlenmiş CBZ formatında saklar.",
                    checked = download.saveChaptersAsCBZ,
                    onCheckedChange = download.onSaveChaptersAsCBZChanged
                )

                KitsugiSwitchSettingItem(
                    title = "Uzun resimleri böl",
                    description = "Webtoon/Tall manga sayfalarını daha rahat okumak için parçalara ayırır.",
                    checked = download.splitTallImages,
                    onCheckedChange = download.onSplitTallImagesChanged
                )

                KitsugiClickableSettingItem(
                    title = "Aynı anda indirilenler",
                    description = "Paralel olarak indirilecek maksimum dosya sayısı.",
                    currentValue = simultaneousDownloadsOptions.firstOrNull { it.first == download.numberOfDownloads }?.second,
                    onClick = { activePicker = "simultaneous" }
                )
            }
        }

        // --- 3. Otomatik İndirme ---
        KitsugiSettingsSection(title = "Otomatik İndirme") {
            Column(modifier = Modifier.fillMaxWidth()) {
                KitsugiSwitchSettingItem(
                    title = "Yeni bölümleri indir",
                    description = "Manga kütüphaneniz güncellendiğinde yeni bölümleri otomatik indirir.",
                    checked = download.downloadNewChapters,
                    onCheckedChange = download.onDownloadNewChaptersChanged
                )

                if (download.downloadNewChapters) {
                    KitsugiClickableSettingItem(
                        title = "Kategorileri filtrele (Dahil Edilenler)",
                        description = "Sadece bu kategorilerdeki mangaları indir.",
                        currentValue = formatCategories(download.downloadNewChapterCategories),
                        onClick = { activePicker = "include_manga_download" }
                    )
                    KitsugiClickableSettingItem(
                        title = "Kategorileri filtrele (Hariç Tutulanlar)",
                        description = "Bu kategorilerdeki mangaları indirme.",
                        currentValue = formatCategories(download.downloadNewChapterCategoriesExclude),
                        onClick = { activePicker = "exclude_manga_download" }
                    )
                    KitsugiSwitchSettingItem(
                        title = "Sadece okunmamış bölümleri indir",
                        checked = download.downloadNewUnreadChaptersOnly,
                        onCheckedChange = download.onDownloadNewUnreadChaptersOnlyChanged
                    )
                }

                KitsugiSwitchSettingItem(
                    title = "Yeni videoları indir",
                    description = "Anime kütüphaneniz güncellendiğinde yeni bölümleri otomatik indirir.",
                    checked = download.downloadNewEpisodes,
                    onCheckedChange = download.onDownloadNewEpisodesChanged
                )

                if (download.downloadNewEpisodes) {
                    KitsugiClickableSettingItem(
                        title = "Anime kategorilerini filtrele (Dahil Edilenler)",
                        description = "Sadece bu kategorilerdeki animeleri indir.",
                        currentValue = formatCategories(download.downloadNewEpisodeCategories),
                        onClick = { activePicker = "include_anime_download" }
                    )
                    KitsugiClickableSettingItem(
                        title = "Anime kategorilerini filtrele (Hariç Tutulanlar)",
                        description = "Bu kategorilerdeki animeleri indirme.",
                        currentValue = formatCategories(download.downloadNewEpisodeCategoriesExclude),
                        onClick = { activePicker = "exclude_anime_download" }
                    )
                    KitsugiSwitchSettingItem(
                        title = "Sadece izlenmemiş videoları indir",
                        checked = download.downloadNewUnseenEpisodesOnly,
                        onCheckedChange = download.onDownloadNewUnseenEpisodesOnlyChanged
                    )
                }

                KitsugiClickableSettingItem(
                    title = "Okurken önceden indir",
                    description = "Manga okurken sıradaki bölümleri arka planda önceden indirir.",
                    currentValue = autoDownloadReadingOptions.firstOrNull { it.first == download.autoDownloadWhileReading }?.second,
                    onClick = { activePicker = "auto_download_reading" }
                )

                KitsugiClickableSettingItem(
                    title = "İzlerken önceden indir",
                    description = "Anime izlerken sıradaki bölümleri arka planda önceden indirir.",
                    currentValue = autoDownloadWatchingOptions.firstOrNull { it.first == download.autoDownloadWhileWatching }?.second,
                    onClick = { activePicker = "auto_download_watching" }
                )
            }
        }

        // --- 4. Otomatik Silme ---
        KitsugiSettingsSection(title = "Otomatik Silme") {
            Column(modifier = Modifier.fillMaxWidth()) {
                KitsugiSwitchSettingItem(
                    title = "Okunduktan sonra sil",
                    description = "Bir manga bölümünü okumayı bitirdiğinizde yerel depolamadan siler.",
                    checked = download.removeAfterMarkedAsRead,
                    onCheckedChange = download.onRemoveAfterMarkedAsReadChanged
                )

                KitsugiClickableSettingItem(
                    title = "Bölümleri otomatik sil",
                    description = "Belirtilen sınıra göre eski bölümleri temizler.",
                    currentValue = removeAfterReadOptions.firstOrNull { it.first == download.removeAfterReadSlots }?.second,
                    onClick = { activePicker = "remove_after_read" }
                )

                KitsugiSwitchSettingItem(
                    title = "Yer imi eklenmiş bölümleri sil",
                    description = "Aktif edilirse yer imi eklenmiş bölümler de otomatik silinir.",
                    checked = download.removeBookmarkedChapters,
                    onCheckedChange = download.onRemoveBookmarkedChaptersChanged
                )

                KitsugiSwitchSettingItem(
                    title = "Filler olarak işaretlenmiş bölümleri indir",
                    description = "Dolgu (Filler) olarak işaretlenen bölümlerin otomatik indirilmesini sağlar.",
                    checked = download.downloadFillermarkedItems,
                    onCheckedChange = download.onDownloadFillermarkedItemsChanged
                )

                KitsugiClickableSettingItem(
                    title = "Silinirken hariç tutulacak kategoriler (Manga)",
                    description = "Bu kategorideki mangaların indirmeleri asla silinmez.",
                    currentValue = formatCategories(download.removeExcludeCategories),
                    onClick = { activePicker = "exclude_manga_delete" }
                )

                KitsugiClickableSettingItem(
                    title = "Silinirken hariç tutulacak kategoriler (Anime)",
                    description = "Bu kategorideki animelerin indirmeleri asla silinmez.",
                    currentValue = formatCategories(download.removeExcludeAnimeCategories),
                    onClick = { activePicker = "exclude_anime_delete" }
                )
            }
        }
    }

    // --- Choice Dialogs ---
    when (activePicker) {
        "downloader" -> {
            KitsugiChoicePickerDialog(
                title = "İndirme Aracı Tercihi",
                options = localDownloaderOptions,
                selectedOption = download.downloaderPreference,
                onOptionSelected = download.onDownloaderPreferenceSelected,
                onDismiss = { activePicker = null }
            )
        }
        "speed_limit" -> {
            KitsugiChoicePickerDialog(
                title = "Hız Sınırı",
                options = speedLimitOptions,
                selectedOption = download.downloadSpeedLimit,
                onOptionSelected = download.onDownloadSpeedLimitChanged,
                onDismiss = { activePicker = null }
            )
        }
        "simultaneous" -> {
            KitsugiChoicePickerDialog(
                title = "Aynı Anda İndirilenler",
                options = simultaneousDownloadsOptions,
                selectedOption = download.numberOfDownloads,
                onOptionSelected = download.onNumberOfDownloadsSelected,
                onDismiss = { activePicker = null }
            )
        }
        "remove_after_read" -> {
            KitsugiChoicePickerDialog(
                title = "Bölümleri Otomatik Sil",
                options = removeAfterReadOptions,
                selectedOption = download.removeAfterReadSlots,
                onOptionSelected = download.onRemoveAfterReadSlotsSelected,
                onDismiss = { activePicker = null }
            )
        }
        "auto_download_reading" -> {
            KitsugiChoicePickerDialog(
                title = "Okurken Önceden İndir",
                options = autoDownloadReadingOptions,
                selectedOption = download.autoDownloadWhileReading,
                onOptionSelected = download.onAutoDownloadWhileReadingSelected,
                onDismiss = { activePicker = null }
            )
        }
        "auto_download_watching" -> {
            KitsugiChoicePickerDialog(
                title = "İzlerken Önceden İndir",
                options = autoDownloadWatchingOptions,
                selectedOption = download.autoDownloadWhileWatching,
                onOptionSelected = download.onAutoDownloadWhileWatchingSelected,
                onDismiss = { activePicker = null }
            )
        }
        "exclude_manga_delete" -> {
            KitsugiMultiSelectPickerDialog(
                title = "Silinirken Hariç Tutulacaklar (Manga)",
                options = statusCategoryOptions,
                selectedOptions = download.removeExcludeCategories,
                onSave = download.onRemoveExcludeCategoriesChanged,
                onDismiss = { activePicker = null }
            )
        }
        "exclude_anime_delete" -> {
            KitsugiMultiSelectPickerDialog(
                title = "Silinirken Hariç Tutulacaklar (Anime)",
                options = statusCategoryOptions,
                selectedOptions = download.removeExcludeAnimeCategories,
                onSave = download.onRemoveExcludeAnimeCategoriesChanged,
                onDismiss = { activePicker = null }
            )
        }
        "include_manga_download" -> {
            KitsugiMultiSelectPickerDialog(
                title = "Yeni Bölümleri İndirilecekler (Manga)",
                options = statusCategoryOptions,
                selectedOptions = download.downloadNewChapterCategories,
                onSave = download.onDownloadNewChapterCategoriesChanged,
                onDismiss = { activePicker = null }
            )
        }
        "exclude_manga_download" -> {
            KitsugiMultiSelectPickerDialog(
                title = "Hariç Tutulacak Kategoriler (Manga)",
                options = statusCategoryOptions,
                selectedOptions = download.downloadNewChapterCategoriesExclude,
                onSave = download.onDownloadNewChapterCategoriesExcludeChanged,
                onDismiss = { activePicker = null }
            )
        }
        "include_anime_download" -> {
            KitsugiMultiSelectPickerDialog(
                title = "Yeni Videoları İndirilecekler (Anime)",
                options = statusCategoryOptions,
                selectedOptions = download.downloadNewEpisodeCategories,
                onSave = download.onDownloadNewEpisodeCategoriesChanged,
                onDismiss = { activePicker = null }
            )
        }
        "exclude_anime_download" -> {
            KitsugiMultiSelectPickerDialog(
                title = "Hariç Tutulacak Kategoriler (Anime)",
                options = statusCategoryOptions,
                selectedOptions = download.downloadNewEpisodeCategoriesExclude,
                onSave = download.onDownloadNewEpisodeCategoriesExcludeChanged,
                onDismiss = { activePicker = null }
            )
        }
    }
}

@Composable
fun <T> KitsugiChoicePickerDialog(
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KitsugiColors.Surface,
        titleContentColor = KitsugiColors.TextPrimary,
        textContentColor = KitsugiColors.TextSecondary,
        shape = RoundedCornerShape(26.dp),
        title = {
            Text(
                text = title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { (value, label) ->
                    val isSelected = value == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSelected) KitsugiColors.SurfaceSoft else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .tvClickable(shape = RoundedCornerShape(12.dp)) {
                                onOptionSelected(value)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onOptionSelected(value)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = KitsugiColors.TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = accentColor, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun <T> KitsugiMultiSelectPickerDialog(
    title: String,
    options: List<Pair<T, String>>,
    selectedOptions: Set<T>,
    onSave: (Set<T>) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    var tempSelected by remember { mutableStateOf(selectedOptions) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KitsugiColors.Surface,
        titleContentColor = KitsugiColors.TextPrimary,
        textContentColor = KitsugiColors.TextSecondary,
        shape = RoundedCornerShape(26.dp),
        title = {
            Text(
                text = title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { (value, label) ->
                    val isChecked = tempSelected.contains(value)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isChecked) KitsugiColors.SurfaceSoft else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .tvClickable(shape = RoundedCornerShape(12.dp)) {
                                tempSelected = if (isChecked) {
                                    tempSelected - value
                                } else {
                                    tempSelected + value
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                tempSelected = if (checked) {
                                    tempSelected + value
                                } else {
                                    tempSelected - value
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = accentColor,
                                uncheckedColor = KitsugiColors.TextMuted,
                                checkmarkColor = KitsugiColors.Background
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(tempSelected)
                    onDismiss()
                }
            ) {
                Text("Kaydet", color = accentColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = KitsugiColors.TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun KitsugiSwitchSettingItem(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalKitsugiAccent.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(12.dp)) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = KitsugiColors.Background,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = KitsugiColors.TextMuted,
                uncheckedTrackColor = KitsugiColors.SurfaceSoft
            )
        )
    }
}

@Composable
fun KitsugiClickableSettingItem(
    title: String,
    description: String? = null,
    currentValue: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(12.dp)) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (currentValue != null) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = currentValue,
                color = LocalKitsugiAccent.current,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun isPackageInstalled(packageName: String, packageManager: android.content.pm.PackageManager): Boolean {
    return try {
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: Exception) {
        false
    }
}

private fun formatCategories(categories: Set<String>): String {
    if (categories.isEmpty()) return "Hiçbiri"
    return categories.mapNotNull { key ->
        statusCategoryOptions.firstOrNull { it.first == key }?.second
    }.joinToString(", ")
}

private val speedLimitOptions = listOf(
    0 to "Sınırsız",
    50 * 1024 to "50 KB/s",
    100 * 1024 to "100 KB/s",
    500 * 1024 to "500 KB/s",
    1024 * 1024 to "1 MB/s",
    2 * 1024 * 1024 to "2 MB/s",
    5 * 1024 * 1024 to "5 MB/s",
    10 * 1024 * 1024 to "10 MB/s"
)

private val simultaneousDownloadsOptions = listOf(
    1 to "1 İndirme",
    2 to "2 İndirme",
    3 to "3 İndirme",
    4 to "4 İndirme",
    5 to "5 İndirme"
)

private val removeAfterReadOptions = listOf(
    -1 to "Devre Dışı",
    0 to "Son okunan bölüm",
    1 to "Son 1 bölüm",
    2 to "Son 2 bölüm",
    3 to "Son 3 bölüm",
    5 to "Son 5 bölüm",
    10 to "Son 10 bölüm"
)

private val autoDownloadReadingOptions = listOf(
    0 to "Devre Dışı",
    1 to "Sonraki 1 bölüm",
    2 to "Sonraki 2 bölüm",
    3 to "Sonraki 3 bölüm",
    5 to "Sonraki 5 bölüm",
    10 to "Sonraki 10 bölüm"
)

private val autoDownloadWatchingOptions = listOf(
    0 to "Devre Dışı",
    1 to "Sonraki 1 video",
    2 to "Sonraki 2 video",
    3 to "Sonraki 3 video"
)

private val statusCategoryOptions = listOf(
    "CURRENT" to "İzleniyor / Okunuyor",
    "PLANNING" to "Planlanıyor",
    "COMPLETED" to "Tamamlandı",
    "ON_HOLD" to "Duraklatıldı",
    "DROPPED" to "Bırakıldı"
)

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
