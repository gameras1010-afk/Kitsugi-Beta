@file:Suppress("UNUSED_PARAMETER")
package com.kitsugi.animelist.ui.screens.search.components

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.kitsugi.animelist.data.cloudstream.CsPluginStatusTracker
import com.kitsugi.animelist.data.cloudstream.CsStreamRunner
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// ─────────────────────────────────────────────────────────────────────────────
// In-memory cache
// ─────────────────────────────────────────────────────────────────────────────

object AddonExploreCache {
    private val cache = ConcurrentHashMap<String, List<HomePageList>>()
    fun get(apiName: String): List<HomePageList>? = cache[apiName]
    fun put(apiName: String, data: List<HomePageList>) { cache[apiName] = data }
    fun clear(apiName: String) { cache.remove(apiName) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Dialog
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddonExploreDialog(
    api: MainAPI,
    onDismissRequest: () -> Unit,
    onSeeAllClick: ((title: String, mainPageData: String, horizontalImages: Boolean, initialItems: List<SearchResponse>) -> Unit)? = null
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<SearchResponse>>(emptyList()) }
    var isHomeLoading by remember { mutableStateOf(false) }
    var homeLists by remember { mutableStateOf<List<HomePageList>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    var activeDetailUrl by remember { mutableStateOf<String?>(null) }

    val loadHomeFeed = { forceRefresh: Boolean ->
        isHomeLoading = true
        scope.launch {
            val cached = if (forceRefresh) null else AddonExploreCache.get(api.name)
            if (cached != null) {
                homeLists = cached
                isHomeLoading = false
            } else {
                val dataList = withContext(Dispatchers.IO) {
                    val list = mutableListOf<HomePageList>()
                    try {
                        for (pageData in api.mainPage) {
                            try {
                                val req = MainPageRequest(pageData.name, pageData.data, pageData.horizontalImages)
                                api.getMainPage(1, req)?.let { list.addAll(it.items) }
                            } catch (t: Throwable) {
                                Log.e("AddonExploreDialog", "Page load failed: ${pageData.name} — ${t.message}")
                            }
                        }
                    } catch (t: Throwable) {
                        Log.e("AddonExploreDialog", "mainPage list failed: ${t.message}")
                    }
                    list
                }
                homeLists = dataList
                AddonExploreCache.put(api.name, dataList)
                isHomeLoading = false
            }
        }
    }

    LaunchedEffect(api.name) { loadHomeFeed(false) }

    val performSearch = {
        if (searchQuery.isNotBlank()) {
            isSearchLoading = true
            hasSearched = true
            keyboardController?.hide()
            scope.launch {
                val results = withContext(Dispatchers.IO) {
                    try { CsStreamRunner.safeSearch(api, searchQuery) }
                    catch (t: Throwable) { emptyList() }
                }
                searchResults = results
                isSearchLoading = false
            }
        }
    }

    val heroItems = remember(homeLists) {
        homeLists.firstOrNull { it.list.isNotEmpty() }?.list?.take(8) ?: emptyList()
    }
    val isBlocked = remember(api.name) { CsPluginStatusTracker.isBlocked(api.name) }
    val isCfProtected = remember(api.name) {
        CsStreamRunner.CF_PROTECTED_PLUGINS.contains(api.name) || api.usesWebView
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = KitsugiColors.Background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // ── Content area ───────────────────────────────────────────
                when {
                    isHomeLoading && homeLists.isEmpty() -> {
                        // Initial loading spinner
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = accentColor)
                                Spacer(Modifier.height(12.dp))
                                Text("${api.name} yükleniyor...", color = KitsugiColors.TextMuted, fontSize = 14.sp)
                            }
                        }
                    }

                    hasSearched -> {
                        // Search results
                        when {
                            isSearchLoading -> {
                                Box(
                                    Modifier.fillMaxSize().padding(top = 88.dp),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator(color = accentColor) }
                            }
                            searchResults.isEmpty() -> {
                                Box(
                                    Modifier.fillMaxSize().padding(top = 88.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("\"$searchQuery\" için sonuç bulunamadı.", color = KitsugiColors.TextMuted)
                                }
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(top = 88.dp),
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    item {
                                        Text(
                                            "\"$searchQuery\" — ${searchResults.size} sonuç",
                                            color = KitsugiColors.TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                    items(searchResults.chunked(3)) { row ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            row.forEach { item ->
                                                Box(Modifier.weight(1f)) {
                                                    ResultItemCard(
                                                        title = item.name,
                                                        imageUrl = item.posterUrl,
                                                        apiName = api.name,
                                                        quality = item.quality?.name,
                                                        onClick = {
                                                            activeDetailUrl = item.url
                                                        }
                                                    )
                                                }
                                            }
                                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // Home feed — CS3 style
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            // Hero banner carousel (full-width, no top padding — sits behind search bar)
                            item(key = "hero") {
                                AddonHeroBannerCarousel(
                                    items = heroItems,
                                    apiName = api.name,
                                    onItemClick = { item ->
                                        activeDetailUrl = item.url
                                    }
                                )
                            }

                            // Warning pill (CF / blocked)
                            if (isBlocked || isCfProtected) {
                                item(key = "warning") {
                                    AddonStatusPill(
                                        isBlocked = isBlocked,
                                        blockReason = if (isBlocked)
                                            CsPluginStatusTracker.getErrorMessage(api.name) ?: "Ağ hatası"
                                        else null
                                    )
                                }
                            }

                            // Empty state
                            if (homeLists.isEmpty() && !isHomeLoading) {
                                item(key = "empty") {
                                    Box(
                                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Keşfet içeriği yüklenemedi.", color = KitsugiColors.TextMuted)
                                    }
                                }
                            }

                            // Category rows
                            items(homeLists, key = { "cat_${homeLists.indexOf(it)}_${it.name}" }) { homeList ->
                                if (homeList.list.isNotEmpty()) {
                                    AddonCategoryRow(
                                        homeList = homeList,
                                        api = api,
                                        accentColor = accentColor,
                                        onSeeAllClick = onSeeAllClick,
                                        context = context,
                                        onItemClick = { url -> activeDetailUrl = url }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Floating search bar (always on top) ────────────────────
                AddonFloatingSearchBar(
                    apiName = api.name,
                    searchQuery = searchQuery,
                    hasSearched = hasSearched,
                    onQueryChange = { searchQuery = it },
                    onSearch = { performSearch() },
                    onClear = {
                        searchQuery = ""
                        searchResults = emptyList()
                        hasSearched = false
                        keyboardController?.hide()
                    },
                    onBack = onDismissRequest,
                    onRefresh = { loadHomeFeed(true) },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }

    // ── Detail Dialog (opens when a content card is tapped) ──────────────────
    activeDetailUrl?.let { detailUrl ->
        KitsugiAddonDetailDialog(
            api = api,
            url = detailUrl,
            onDismissRequest = { activeDetailUrl = null }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero Banner Carousel — CS3 Ana Sayfa Hero (otomatik kaydırmalı vitrin)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddonHeroBannerCarousel(
    items: List<SearchResponse>,
    apiName: String,
    onItemClick: (SearchResponse) -> Unit
) {
    if (items.isEmpty()) {
        // Boş durum
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
                .background(KitsugiColors.Surface)
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()

    // 5 saniyede bir otomatik sayfa geçişi
    LaunchedEffect(pagerState) {
        while (true) {
            delay(5000L)
            val nextPage = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            AddonHeroBannerPage(
                item = item,
                apiName = apiName,
                onItemClick = { onItemClick(item) }
            )
        }

        // Alt sayfa göstergesi (indicator dots)
        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(items.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White
                                else Color.White.copy(alpha = 0.35f)
                            )
                            .size(if (isSelected) 7.dp else 5.dp)
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddonHeroBannerPage(
    item: SearchResponse,
    apiName: String,
    onItemClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onItemClick() }   // Banner'a tıklayınca bilgi sayfası
    ) {
        // Poster görseli
        if (!item.posterUrl.isNullOrBlank()) {
            val imageReq = remember(item.posterUrl) {
                coil.request.ImageRequest.Builder(context)
                    .data(item.posterUrl)
                    .addHeader("Referer", try {
                        val u = android.net.Uri.parse(item.posterUrl)
                        "${u.scheme}://${u.host}/"
                    } catch (_: Exception) { item.posterUrl!! })
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = imageReq,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(KitsugiColors.Surface))
        }

        // Alt gradient (başlık için)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.4f to Color.Black.copy(0.25f),
                        1f to Color.Black.copy(0.92f)
                    )
                )
        )

        // Üst gradient (arama barı okunabilirliği)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(0.72f), Color.Transparent)
                    )
                )
        )

        // Sağ üst — eklenti ismi badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(0.6f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = apiName,
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Alt içerik — başlık + butonlar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 14.dp)
            )
            // Buton satırı: Ekle | ▶ Oynat | Bilgi
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroIconButton(
                    icon = { Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(22.dp)) },
                    label = "Ekle"
                ) { /* İleride listeye ekle */ }

                // Birincil "Oynat" butonu — beyaz arka plan, siyah yazı
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .clickable { onItemClick() }
                        .padding(horizontal = 26.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Text("Oynat", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Bilgi butonu — tıklayınca detay sayfası açar
                HeroIconButton(
                    icon = { Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(22.dp)) },
                    label = "Bilgi"
                ) { onItemClick() }
            }
        }
    }
}

@Composable
private fun HeroIconButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        icon()
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Floating Search Bar — üstte sabit, transparan arka plan
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AddonFloatingSearchBar(
    apiName: String,
    searchQuery: String,
    hasSearched: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalKitsugiAccent.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Geri / Temizle butonu
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(0.45f))
                .clickable { if (hasSearched) onClear() else onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowBack, "Geri", tint = Color.White, modifier = Modifier.size(20.dp))
        }

        // Arama alanı
        Box(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(Color.Black.copy(0.55f))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.65f), modifier = Modifier.size(18.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    cursorBrush = SolidColor(accentColor),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) Text("Ara...", color = Color.White.copy(0.45f), style = MaterialTheme.typography.bodyMedium)
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(Color.White.copy(0.2f)).clickable { onClear() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Yenile butonu
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(0.45f))
                .clickable { onRefresh() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Refresh, "Yenile", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status Warning Pill — küçük uyarı satırı
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AddonStatusPill(
    isBlocked: Boolean,
    blockReason: String?
) {
    val bgColor = if (isBlocked) KitsugiColors.AccentRed.copy(0.14f) else KitsugiColors.AccentOrange.copy(0.14f)
    val borderColor = if (isBlocked) KitsugiColors.AccentRed else KitsugiColors.AccentOrange
    val text = if (isBlocked)
        "⚠️ Eklenti engellendi: ${blockReason ?: "Bilinmeyen hata"}"
    else
        "🔐 Cloudflare korumalı eklenti. İlk açılışta tarayıcıda doğrulama gerekebilir."

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(text, color = borderColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category Row — CS3 yatay satır (başlık + → ok + LazyRow)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AddonCategoryRow(
    homeList: HomePageList,
    api: MainAPI,
    accentColor: androidx.compose.ui.graphics.Color,
    onSeeAllClick: ((title: String, mainPageData: String, horizontalImages: Boolean, initialItems: List<SearchResponse>) -> Unit)?,
    context: android.content.Context,
    onItemClick: (url: String) -> Unit = {}
) {
    val matchingPage = remember(homeList.name) {
        api.mainPage.firstOrNull { it.name == homeList.name }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // Başlık satırı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = homeList.name,
                color = KitsugiColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (onSeeAllClick != null && matchingPage != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onSeeAllClick(
                                homeList.name,
                                matchingPage.data,
                                matchingPage.horizontalImages,
                                homeList.list
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Tümü",
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Yatay poster listesi
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(homeList.list) { item ->
                ResultItemCard(
                    title = item.name,
                    imageUrl = item.posterUrl,
                    apiName = api.name,
                    quality = item.quality?.name,
                    onClick = {
                        onItemClick(item.url)
                    }
                )
            }
        }
    }
}

