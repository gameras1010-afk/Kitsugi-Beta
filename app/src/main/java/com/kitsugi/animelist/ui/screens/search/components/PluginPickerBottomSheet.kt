package com.kitsugi.animelist.ui.screens.search.components

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kitsugi.animelist.data.cloudstream.CsPluginLoader
import com.kitsugi.animelist.data.cloudstream.CsStreamRunner
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.ui.components.KitsugiSheetOrDialog
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// TvType → Türkçe etiket grupları
// ─────────────────────────────────────────────────────────────────────────────

private data class PluginTypeGroup(
    val label: String,
    val emoji: String,
    val types: List<TvType>
)

private val pluginTypeGroups = listOf(
    PluginTypeGroup("Filmler", "🎬", listOf(TvType.Movie, TvType.AnimeMovie, TvType.Cartoon)),
    PluginTypeGroup("Diziler", "📺", listOf(TvType.TvSeries, TvType.AsianDrama)),
    PluginTypeGroup("Animasyon", "🎌", listOf(TvType.Anime, TvType.OVA)),
    PluginTypeGroup("Belgeseller", "📰", listOf(TvType.Documentary)),
    PluginTypeGroup("Canlı", "📡", listOf(TvType.Live)),
    PluginTypeGroup("Diğer", "🔖", listOf(TvType.Others))
)

// ─────────────────────────────────────────────────────────────────────────────
// PluginPickerBottomSheet (Eklenti Portalı & Arama Merkezi)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PluginPickerBottomSheet(
    onDismissRequest: () -> Unit,
    onPluginSelected: (apiName: String?) -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var activeApis by remember { mutableStateOf<List<MainAPI>>(emptyList()) }
    var isLoadingActiveApis by remember { mutableStateOf(true) }
    var selectedGroupLabel by remember { mutableStateOf<String?>(null) }

    // Search Portal States
    var queryText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Pair<MainAPI, SearchResponse>>>(emptyList()) }
    var isLoadingResults by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var activeDetailItem by remember { mutableStateOf<Pair<MainAPI, SearchResponse>?>(null) }

    val listState = rememberLazyListState()

    // Aktif eklentileri yükle
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val db = KitsugiDatabase.getDatabase(context.applicationContext)
                val enabledPlugins = db.csPluginDao().getEnabledPlugins()
                for (plugin in enabledPlugins) {
                    try { CsPluginLoader.loadExtension(context, plugin.id) }
                    catch (e: Exception) {
                        Log.e("PluginPickerSheet", "Load failed: ${plugin.name} — ${e.message}")
                    }
                }
                val enabledIds = enabledPlugins.map { it.id }.toSet()
                activeApis = APIHolder.allProviders.filter { api ->
                    val pluginId = java.io.File(api.sourcePlugin ?: "").nameWithoutExtension
                    enabledIds.contains(pluginId)
                }.sortedBy { it.name.lowercase() }
            } catch (e: Exception) {
                Log.e("PluginPickerSheet", "Error loading extensions: ${e.message}")
            }
            isLoadingActiveApis = false
        }
    }

    // Seçili eklentileri tipe göre filtrele
    val filteredApis = remember(activeApis, selectedGroupLabel) {
        if (selectedGroupLabel == null) {
            activeApis
        } else {
            val group = pluginTypeGroups.firstOrNull { it.label == selectedGroupLabel }
            if (group == null) activeApis
            else activeApis.filter { api ->
                api.supportedTypes.any { group.types.contains(it) }
            }
        }
    }

    // Parallel Cross-Provider Search
    val performSearch = {
        if (queryText.isNotBlank()) {
            isLoadingResults = true
            hasSearched = true
            keyboardController?.hide()
            scope.launch {
                val results = withContext(Dispatchers.IO) {
                    val apiList = activeApis
                    val list = mutableListOf<Pair<MainAPI, SearchResponse>>()
                    supervisorScope {
                        val jobs = apiList.map { api ->
                            async {
                                try {
                                    val searchRes = CsStreamRunner.safeSearch(api, queryText)
                                    synchronized(list) {
                                        searchRes.forEach { list.add(api to it) }
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }
                        jobs.forEach {
                            try {
                                it.await()
                            } catch (_: Exception) {
                            }
                        }
                    }
                    list
                }
                searchResults = results
                isLoadingResults = false
            }
        }
    }

    KitsugiSheetOrDialog(
        onDismiss = onDismissRequest,
        heightFraction = 0.95f,
        fillMaxHeight = true,
        innerScrollState = listState
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KitsugiColors.Background)
        ) {
            // ── Üst Başlık & Geri ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Geri",
                        tint = KitsugiColors.TextPrimary
                    )
                }
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Eklenti Portalı",
                    color = KitsugiColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${activeApis.size} aktif",
                    color = KitsugiColors.TextMuted,
                    fontSize = 13.sp
                )
            }

            // ── Premium Arama Çubuğu ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KitsugiColors.Surface)
                    .border(1.dp, KitsugiColors.Border, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = KitsugiColors.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    BasicTextField(
                        value = queryText,
                        onValueChange = {
                            queryText = it
                            if (it.isBlank()) {
                                searchResults = emptyList()
                                hasSearched = false
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = KitsugiColors.TextPrimary,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(accentColor),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (queryText.isNotBlank()) {
                                performSearch()
                            }
                        }),
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (queryText.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                queryText = ""
                                searchResults = emptyList()
                                hasSearched = false
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Temizle",
                                tint = KitsugiColors.TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── İçerik Alanı ──────────────────────────────────────────────
            if (queryText.isBlank()) {
                // NORMAL MOD: Kategoriler ve Eklenti Listesi
                val availableGroups = remember(activeApis) {
                    pluginTypeGroups.filter { group ->
                        activeApis.any { api -> api.supportedTypes.any { group.types.contains(it) } }
                    }
                }

                if (availableGroups.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PluginTypeChip(
                            label = "Tümü",
                            emoji = "🌐",
                            selected = selectedGroupLabel == null,
                            accentColor = accentColor,
                            onClick = { selectedGroupLabel = null }
                        )
                        availableGroups.forEach { group ->
                            PluginTypeChip(
                                label = group.label,
                                emoji = group.emoji,
                                selected = selectedGroupLabel == group.label,
                                accentColor = accentColor,
                                onClick = {
                                    selectedGroupLabel = if (selectedGroupLabel == group.label) null else group.label
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                HorizontalDivider(color = KitsugiColors.Border.copy(alpha = 0.5f))

                when {
                    isLoadingActiveApis -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accentColor, strokeWidth = 2.dp)
                        }
                    }
                    filteredApis.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedGroupLabel != null)
                                    "Bu kategoride aktif eklenti yok."
                                else
                                    "Aktif eklenti bulunamadı.\nEklentiler ekranından eklenti etkinleştirin.",
                                color = KitsugiColors.TextMuted,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item {
                                AllPluginsListItem(
                                    accentColor = accentColor,
                                    onClick = {
                                        onPluginSelected(null)
                                        onDismissRequest()
                                    }
                                )
                            }

                            items(filteredApis, key = { "${java.io.File(it.sourcePlugin ?: "").nameWithoutExtension}_${it.name}" }) { api ->
                                PluginListItem(
                                    api = api,
                                    accentColor = accentColor,
                                    onClick = {
                                        onPluginSelected(api.name)
                                        onDismissRequest()
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // ARAMA MODU: Tüm Aktif Eklentilerden Paralel Sonuçlar
                when {
                    isLoadingResults -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accentColor, strokeWidth = 2.dp)
                        }
                    }
                    searchResults.isEmpty() && hasSearched -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sonuç bulunamadı.",
                                color = KitsugiColors.TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                    else -> {
                        val grouped = remember(searchResults) {
                            searchResults.groupBy { it.first }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            grouped.forEach { (api, items) ->
                                item(key = api.name) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        // Plugin Header Row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val langFlag = SubtitleHelperCompat.getFlagFromIso(api.lang) ?: "🌐"
                                            Text(text = langFlag, fontSize = 16.sp)
                                            Text(
                                                text = api.name,
                                                color = KitsugiColors.TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = "${items.size} sonuç",
                                                color = KitsugiColors.TextMuted,
                                                fontSize = 12.sp
                                            )
                                        }

                                        // Horizontal row of media response cards
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(items) { (_, response) ->
                                                PortalResultItemCard(
                                                    title = response.name,
                                                    imageUrl = response.posterUrl,
                                                    apiName = api.name,
                                                    quality = null,
                                                    onClick = {
                                                        activeDetailItem = api to response
                                                    }
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
        }
    }

    // ── Detail Dialog ────────────────────────────────────────────────────────
    activeDetailItem?.let { (api, response) ->
        KitsugiAddonDetailDialog(
            api = api,
            url = response.url,
            onDismissRequest = { activeDetailItem = null }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bileşenler
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PluginTypeChip(
    label: String,
    emoji: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor else KitsugiColors.Background,
        animationSpec = tween(180), label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor else KitsugiColors.Border,
        animationSpec = tween(180), label = "chipBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else KitsugiColors.TextSecondary,
        animationSpec = tween(180), label = "chipText"
    )
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$emoji $label",
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PluginListItem(
    api: MainAPI,
    accentColor: Color,
    onClick: () -> Unit
) {
    val langFlag = try {
        SubtitleHelperCompat.getFlagFromIso(api.lang) ?: "🌐"
    } catch (_: Exception) { "🌐" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = langFlag, fontSize = 18.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = api.name,
                color = KitsugiColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = api.mainUrl,
                color = KitsugiColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val typeLabel = api.supportedTypes.take(2).joinToString(", ") { tvTypeLabel(it) }
        if (typeLabel.isNotEmpty()) {
            Text(
                text = typeLabel,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AllPluginsListItem(
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🌐", fontSize = 18.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Tüm Eklentiler",
                color = KitsugiColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Aktif olan tüm eklentilerde ara",
                color = KitsugiColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = "Hepsi",
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PortalResultItemCard(
    title: String,
    imageUrl: String?,
    apiName: String,
    quality: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    val context = LocalContext.current
                    val imageRequest = remember(imageUrl) {
                        coil.request.ImageRequest.Builder(context)
                            .data(imageUrl)
                            .addHeader(
                                "Referer",
                                try {
                                    val uri = android.net.Uri.parse(imageUrl)
                                    "${uri.scheme}://${uri.host}/"
                                } catch (_: Exception) { imageUrl }
                            )
                            .addHeader(
                                "User-Agent",
                                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
                            )
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = KitsugiColors.TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                if (!quality.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = quality,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = title,
                    color = KitsugiColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = apiName,
                    color = KitsugiColors.TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun tvTypeLabel(type: TvType): String = when (type) {
    TvType.Movie, TvType.AnimeMovie -> "Film"
    TvType.TvSeries -> "Dizi"
    TvType.Anime -> "Anime"
    TvType.AsianDrama -> "Asya"
    TvType.Cartoon -> "Çizgi film"
    TvType.Documentary -> "Belgesel"
    TvType.Live -> "Canlı"
    TvType.OVA -> "OVA"
    else -> type.name
}

private object SubtitleHelperCompat {
    fun getFlagFromIso(iso: String): String? = try {
        com.lagradost.cloudstream3.utils.SubtitleHelper.getFlagFromIso(iso)
    } catch (_: Exception) { null }
}
