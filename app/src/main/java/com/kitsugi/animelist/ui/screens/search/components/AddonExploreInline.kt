package com.kitsugi.animelist.ui.screens.search.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kitsugi.animelist.data.cloudstream.CsPluginStatusTracker
import com.kitsugi.animelist.data.cloudstream.CsStreamRunner
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// Inline Plugin Explore — LazyListScope extension
// Arama ekranının LazyColumn'una doğrudan item'lar ekler (diyalog açmaz)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * State holder — SearchScreen seviyesinde remember ile tutulur.
 * Böylece eklenti değişince tamamen sıfırlanır.
 */
class AddonExploreInlineState(
    val apiName: String,
    val scope: CoroutineScope
) {
    var homeLists by mutableStateOf<List<HomePageList>>(emptyList())
    var isLoading by mutableStateOf(false)
    var isBlocked by mutableStateOf(false)
    var isCfProtected by mutableStateOf(false)

    fun load(api: MainAPI, forceRefresh: Boolean = false) {
        if (isLoading) return
        isLoading = true
        isBlocked = CsPluginStatusTracker.isBlocked(api.name)
        isCfProtected = CsStreamRunner.CF_PROTECTED_PLUGINS.contains(api.name) || api.usesWebView
        scope.launch {
            val cached = if (forceRefresh) null else AddonExploreCache.get(api.name)
            if (cached != null) {
                homeLists = cached
                isLoading = false
                return@launch
            }
            val dataList = withContext(Dispatchers.IO) {
                val list = mutableListOf<HomePageList>()
                try {
                    for (pageData in api.mainPage) {
                        try {
                            val req = MainPageRequest(pageData.name, pageData.data, pageData.horizontalImages)
                            api.getMainPage(1, req)?.let { list.addAll(it.items) }
                        } catch (t: Throwable) {
                            Log.e("AddonExploreInline", "Page load failed: ${pageData.name} — ${t.message}")
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("AddonExploreInline", "mainPage list failed: ${t.message}")
                }
                list
            }
            homeLists = dataList
            AddonExploreCache.put(api.name, dataList)
            isLoading = false
        }
    }
}

@Composable
fun rememberAddonExploreInlineState(apiName: String): AddonExploreInlineState {
    val scope = rememberCoroutineScope()
    return remember(apiName) {
        AddonExploreInlineState(apiName = apiName, scope = scope)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LazyListScope extension — SearchScreen'e item'lar ekler
// ─────────────────────────────────────────────────────────────────────────────

fun LazyListScope.addonExploreInlineSections(
    state: AddonExploreInlineState,
    api: MainAPI,
    onSeeAllClick: ((apiName: String, title: String, mainPageData: String, horizontalImages: Boolean, initialItems: List<SearchResponse>) -> Unit)? = null
) {
    // ── Hero Banner ─────────────────────────────────────────────────────────
    item(key = "inline_hero_${api.name}") {
        val heroItem = state.homeLists.firstOrNull { it.list.isNotEmpty() }?.list?.firstOrNull()
        val accentColor = LocalKitsugiAccent.current
        val context = LocalContext.current

        InlineHeroBanner(
            item = heroItem,
            apiName = api.name,
            accentColor = accentColor,
            onPlayClick = {
                heroItem?.let { item ->
                    com.kitsugi.animelist.ui.screens.stream.KitsugiStreamActivity.start(
                        context = context,
                        malId = null, aniListId = null,
                        episode = 1, season = 1,
                        title = item.name,
                        posterUrl = item.posterUrl,
                        cs3Url = item.url,
                        cs3ApiName = api.name
                    )
                }
            },
            onRefresh = { state.load(api, forceRefresh = true) }
        )
    }

    // ── Uyarı Pill'i (CF / Engel) ──────────────────────────────────────────
    if (state.isBlocked || state.isCfProtected) {
        item(key = "inline_warning_${api.name}") {
            val text = if (state.isBlocked)
                "⚠️ Eklenti engellendi: ${CsPluginStatusTracker.getErrorMessage(api.name) ?: "Bilinmeyen hata"}"
            else
                "🔐 Cloudflare korumalı eklenti. İlk açılışta doğrulama gerekebilir."
            val borderColor = if (state.isBlocked) KitsugiColors.AccentRed else KitsugiColors.AccentOrange
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(borderColor.copy(0.12f))
                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(text, color = borderColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    // ── Yükleniyor ──────────────────────────────────────────────────────────
    if (state.isLoading && state.homeLists.isEmpty()) {
        item(key = "inline_loading_${api.name}") {
            val accentColor = LocalKitsugiAccent.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = accentColor)
                    Spacer(Modifier.height(12.dp))
                    Text("${api.name} yükleniyor...", color = KitsugiColors.TextMuted, fontSize = 14.sp)
                }
            }
        }
    }

    // ── Boş Durum ──────────────────────────────────────────────────────────
    if (!state.isLoading && state.homeLists.isEmpty()) {
        item(key = "inline_empty_${api.name}") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Keşfet içeriği yüklenemedi.", color = KitsugiColors.TextMuted)
            }
        }
    }

    // ── Kategori Satırları ──────────────────────────────────────────────────
    items(state.homeLists, key = { "inline_cat_${api.name}_${it.name}" }) { homeList ->
        if (homeList.list.isNotEmpty()) {
            val accentColor = LocalKitsugiAccent.current
            val context = LocalContext.current
            val matchingPage = remember(homeList.name) {
                api.mainPage.firstOrNull { it.name == homeList.name }
            }
            InlineCategoryRow(
                homeList = homeList,
                api = api,
                accentColor = accentColor,
                context = context,
                matchingPage = matchingPage,
                onSeeAllClick = onSeeAllClick
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bileşenler
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InlineHeroBanner(
    item: SearchResponse?,
    apiName: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onPlayClick: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        // Poster
        if (item != null && !item.posterUrl.isNullOrBlank()) {
            val req = remember(item.posterUrl) {
                ImageRequest.Builder(context)
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
                model = req,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(KitsugiColors.Surface))
        }

        // Alt gradient
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.45f to Color.Black.copy(0.2f),
                    1f to Color.Black.copy(0.88f)
                )
            )
        )

        // Sağ üst: eklenti adı + yenile
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(0.55f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(0.45f))
                    .clickable { onRefresh() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Refresh, "Yenile", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        // Alt: başlık + oynat
        if (item != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable { onPlayClick() }
                        .padding(horizontal = 28.dp, vertical = 10.dp)
                ) {
                    Text("▶  Oynat", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun InlineCategoryRow(
    homeList: HomePageList,
    api: MainAPI,
    accentColor: androidx.compose.ui.graphics.Color,
    context: android.content.Context,
    matchingPage: com.lagradost.cloudstream3.MainPageData?,
    onSeeAllClick: ((apiName: String, title: String, mainPageData: String, horizontalImages: Boolean, initialItems: List<SearchResponse>) -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Başlık + Tümü →
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
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
                                api.name,
                                homeList.name,
                                matchingPage.data,
                                matchingPage.horizontalImages,
                                homeList.list
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Tümü", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                        com.kitsugi.animelist.ui.screens.stream.KitsugiStreamActivity.start(
                            context = context,
                            malId = null, aniListId = null,
                            episode = 1, season = 1,
                            title = item.name,
                            posterUrl = item.posterUrl,
                            cs3Url = item.url,
                            cs3ApiName = api.name
                        )
                    }
                )
            }
        }
    }
}
