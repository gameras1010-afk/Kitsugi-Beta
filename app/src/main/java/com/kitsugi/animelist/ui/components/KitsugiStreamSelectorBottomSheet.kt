package com.kitsugi.animelist.ui.components

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.repository.AddonStreamRepository
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.data.remote.DebridResolver
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class DebridCacheState {
    CACHED,
    NOT_CACHED,
    P2P
}

fun getCacheState(stream: StreamSource): DebridCacheState {
    val nameLower = stream.name.lowercase(Locale.ROOT)
    val titleLower = stream.title.lowercase(Locale.ROOT)
    
    return when {
        nameLower.contains("[rd+]") || nameLower.contains("rd+") || nameLower.contains("cached") ||
        titleLower.contains("[rd+]") || titleLower.contains("rd+") ||
        nameLower.contains("[tb+]") || nameLower.contains("tb+") ||
        nameLower.contains("[pm+]") || nameLower.contains("pm+") ||
        nameLower.contains("[ad+]") || nameLower.contains("ad+") -> DebridCacheState.CACHED
        
        nameLower.contains("[rd~]") || nameLower.contains("download") ||
        titleLower.contains("[rd~]") || titleLower.contains("download") -> DebridCacheState.NOT_CACHED
        
        stream.infoHash != null && stream.url == null -> DebridCacheState.P2P
        
        else -> DebridCacheState.CACHED
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiStreamSelectorBottomSheet(
    malId: Int?,
    aniListId: Int?,
    episodeNumber: Int,
    mediaTitle: String,
    onStreamSelected: (url: String, title: String, source: StreamSource) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { AddonStreamRepository(context) }

    var isLoading by remember { mutableStateOf(true) }
    var streams by remember { mutableStateOf<List<StreamSource>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resolvingSource by remember { mutableStateOf<StreamSource?>(null) }

    // Fetch streams
    LaunchedEffect(malId, aniListId, episodeNumber) {
        isLoading = true
        errorMessage = null
        try {
            val fetched = repository.getStreamsForEpisode(
                malId = malId,
                aniListId = aniListId,
                season = 1,
                episode = episodeNumber
            )
            streams = fetched
            if (fetched.isEmpty()) {
                errorMessage = "Herhangi bir akış kaynağı bulunamadı. Eklentilerinizi veya RealDebrid ayarlarınızı kontrol edin."
            }
        } catch (e: Exception) {
            errorMessage = "Akışlar yüklenirken bir hata oluştu: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    KitsugiSheetOrDialog(
        onDismiss = onDismiss
    ) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            // Landscape: Header+Tabs sol, Akış listesi sağ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sol panel: başlık + kapatma + addon sekmeleri
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mediaTitle,
                                color = KitsugiColors.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bölüm $episodeNumber • Akış Seçenekleri",
                                color = KitsugiColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = KitsugiColors.SurfaceStrong
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Kapat",
                                tint = KitsugiColors.TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isLoading && errorMessage == null && resolvingSource == null) {
                        val addonNames = remember(streams) {
                            listOf("Tümü") + streams.map { it.addonName }.distinct()
                        }
                        var selectedAddonLandscape by remember { mutableStateOf("Tümü") }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            addonNames.forEach { addon ->
                                val count = if (addon == "Tümü") streams.size else streams.count { it.addonName == addon }
                                val isSelected = selectedAddonLandscape == addon
                                val backgroundColor = if (isSelected) LocalKitsugiAccent.current else KitsugiColors.SurfaceStrong
                                val contentColor = if (isSelected) Color.White else KitsugiColors.TextPrimary

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(backgroundColor)
                                        .tvClickable(shape = RoundedCornerShape(12.dp), onClick = { selectedAddonLandscape = addon })
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = addon,
                                            color = contentColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White.copy(alpha = 0.2f) else KitsugiColors.Surface.copy(alpha = 0.6f))
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = count.toString(),
                                                color = if (isSelected) Color.White else KitsugiColors.TextSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Dikey ayırıcı
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(KitsugiColors.Border)
                )

                // Sağ panel: akış listesi
                Box(modifier = Modifier.weight(0.6f)) {
                    when {
                        isLoading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = LocalKitsugiAccent.current)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Eklentiler taranıyor...",
                                    color = KitsugiColors.TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        errorMessage != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = KitsugiColors.AccentRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        resolvingSource != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = KitsugiColors.AccentOrange)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Link Çözümleniyor...",
                                    color = KitsugiColors.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "RealDebrid üzerinden bağlantı alınıyor.",
                                    color = KitsugiColors.TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        else -> {
                            val addonNamesR = remember(streams) {
                                listOf("Tümü") + streams.map { it.addonName }.distinct()
                            }
                            var selectedAddonR by remember { mutableStateOf("Tümü") }
                            val filteredStreamsR = remember(streams, selectedAddonR) {
                                if (selectedAddonR == "Tümü") streams else streams.filter { it.addonName == selectedAddonR }
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredStreamsR) { stream ->
                                    StreamItemRow(
                                        stream = stream,
                                        onClick = {
                                            val isTorrent = !stream.infoHash.isNullOrBlank() || stream.url?.startsWith("magnet:") == true
                                            if (isTorrent && DebridResolver(context).getApiKey().isNullOrBlank()) {
                                                errorMessage = "Debrid API anahtarı gerekli."
                                                return@StreamItemRow
                                            }
                                            resolvingSource = stream
                                            coroutineScope.launch {
                                                val resolvedUrl = repository.resolveStreamUrl(stream)
                                                if (resolvedUrl != null) {
                                                    onStreamSelected(resolvedUrl, stream.title, stream)
                                                } else {
                                                    errorMessage = "Akış linki çözümlenemedi (Debrid hatası veya yetkisiz token)."
                                                    resolvingSource = null
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Portrait: mevcut dikey düzen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaTitle,
                            color = KitsugiColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Bölüm $episodeNumber • Akış Seçenekleri",
                            color = KitsugiColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = KitsugiColors.SurfaceStrong
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Kapat",
                            tint = KitsugiColors.TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = LocalKitsugiAccent.current)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Eklentiler taranıyor, akışlar aranıyor...",
                            color = KitsugiColors.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = KitsugiColors.AccentRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            lineHeight = 20.sp
                        )
                    }
                } else if (resolvingSource != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = KitsugiColors.AccentOrange)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Link Çözümleniyor...",
                            color = KitsugiColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "RealDebrid üzerinden indirme bağlantısı alınıyor.",
                            color = KitsugiColors.TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    val addonNames = remember(streams) {
                        listOf("Tümü") + streams.map { it.addonName }.distinct()
                    }
                    var selectedAddon by remember { mutableStateOf("Tümü") }
                    val filteredStreams = remember(streams, selectedAddon) {
                        if (selectedAddon == "Tümü") streams else streams.filter { it.addonName == selectedAddon }
                    }

                    val tabScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(tabScrollState)
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        addonNames.forEach { addon ->
                            val count = if (addon == "Tümü") streams.size else streams.count { it.addonName == addon }
                            val isSelected = selectedAddon == addon
                            val backgroundColor = if (isSelected) LocalKitsugiAccent.current else KitsugiColors.SurfaceStrong
                            val contentColor = if (isSelected) Color.White else KitsugiColors.TextPrimary

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(backgroundColor)
                                    .tvClickable(shape = RoundedCornerShape(12.dp), onClick = { selectedAddon = addon })
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = addon,
                                        color = contentColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White.copy(alpha = 0.2f) else KitsugiColors.Surface.copy(alpha = 0.6f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            color = if (isSelected) Color.White else KitsugiColors.TextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredStreams) { stream ->
                            StreamItemRow(
                                stream = stream,
                                onClick = {
                                    val isTorrent = !stream.infoHash.isNullOrBlank() || stream.url?.startsWith("magnet:") == true
                                    if (isTorrent && DebridResolver(context).getApiKey().isNullOrBlank()) {
                                        errorMessage = "Debrid API anahtarı gerekli."
                                        return@StreamItemRow
                                    }
                                    resolvingSource = stream
                                    coroutineScope.launch {
                                        val resolvedUrl = repository.resolveStreamUrl(stream)
                                        if (resolvedUrl != null) {
                                            onStreamSelected(resolvedUrl, stream.title, stream)
                                        } else {
                                            errorMessage = "Akış linki çözümlenemedi (Debrid hatası veya yetkisiz token)."
                                            resolvingSource = null
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamItemRow(
    stream: StreamSource,
    onClick: () -> Unit
) {
    val parsedInfo = remember(stream.title) { parseStreamTitle(stream.title) }
    val accentColor = LocalKitsugiAccent.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = KitsugiColors.SurfaceStrong.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Quality and Addon label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quality badge
                    val qualityColor = when (parsedInfo.quality) {
                        "4K", "2160p" -> KitsugiColors.AccentRed
                        "1080p" -> KitsugiColors.AccentBlue
                        "720p" -> KitsugiColors.AccentGreen
                        else -> KitsugiColors.SurfaceStrong
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(qualityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = parsedInfo.quality,
                            color = if (qualityColor == KitsugiColors.SurfaceStrong) KitsugiColors.TextSecondary else qualityColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Addon Name Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(KitsugiColors.AccentPurple.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stream.addonName,
                            color = KitsugiColors.AccentPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Cache Status Badge
                    val cacheState = remember(stream) { getCacheState(stream) }
                    val (cacheText, cacheColor) = when (cacheState) {
                        DebridCacheState.CACHED -> "Önbellekte" to KitsugiColors.AccentGreen
                        DebridCacheState.NOT_CACHED -> "İndirilecek" to KitsugiColors.AccentOrange
                        DebridCacheState.P2P -> "Torrent (P2P)" to KitsugiColors.AccentBlue
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(cacheColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = cacheText,
                            color = cacheColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (parsedInfo.size.isNotBlank()) {
                        Text(
                            text = parsedInfo.size,
                            color = KitsugiColors.TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title
                Text(
                    text = stream.title,
                    color = KitsugiColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Extra details like dual audio or seeders
                if (stream.name.isNotBlank()) {
                    Text(
                        text = stream.name.trim(),
                        color = KitsugiColors.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Play Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Oynat",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private data class ParsedStreamInfo(
    val quality: String,
    val size: String
)

private fun parseStreamTitle(title: String): ParsedStreamInfo {
    val titleLower = title.lowercase(Locale.ROOT)
    
    // Quality
    val quality = when {
        titleLower.contains("2160") || titleLower.contains("4k") -> "4K"
        titleLower.contains("1080") -> "1080p"
        titleLower.contains("720") -> "720p"
        titleLower.contains("480") -> "480p"
        else -> "HD"
    }

    // Size regex like "1.2 GB" or "750 MB" or "1.2gb"
    val sizeRegex = Regex("""(\d+(?:\.\d+)?\s*(?:gb|mb|gib|mib))""", RegexOption.IGNORE_CASE)
    val match = sizeRegex.find(title)
    val size = match?.value?.uppercase(Locale.ROOT) ?: ""

    return ParsedStreamInfo(quality, size)
}
