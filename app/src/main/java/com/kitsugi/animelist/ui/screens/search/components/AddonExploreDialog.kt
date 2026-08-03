package com.kitsugi.animelist.ui.screens.search.components

import android.content.Context
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.kitsugi.animelist.data.cloudstream.CsStreamRunner
import com.kitsugi.animelist.data.cloudstream.CsPluginStatusTracker
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache for addon explore homepages.
 * Avoids reloading the home feed repeatedly when user goes back and forth.
 */
object AddonExploreCache {
    private val cache = ConcurrentHashMap<String, List<HomePageList>>()

    fun get(apiName: String): List<HomePageList>? = cache[apiName]
    fun put(apiName: String, data: List<HomePageList>) {
        cache[apiName] = data
    }
    fun clear(apiName: String) {
        cache.remove(apiName)
    }
}

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

    // Load home page feed
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
                        val mainPages = api.mainPage
                        for (pageData in mainPages) {
                            try {
                                val req = MainPageRequest(pageData.name, pageData.data, pageData.horizontalImages)
                                val response = api.getMainPage(1, req)
                                if (response != null) {
                                    list.addAll(response.items)
                                }
                            } catch (t: Throwable) {
                                // Catch Throwable to also handle NoClassDefFoundError from
                                // plugins that depend on libraries not bundled in the host APK.
                                Log.e("AddonExploreDialog", "Failed to load page ${pageData.name}: ${t.javaClass.simpleName}: ${t.message}")
                            }
                        }
                    } catch (t: Throwable) {
                        Log.e("AddonExploreDialog", "Failed to fetch mainPage list: ${t.javaClass.simpleName}: ${t.message}")
                    }
                    list
                }
                homeLists = dataList
                AddonExploreCache.put(api.name, dataList)
                isHomeLoading = false
            }
        }
    }

    LaunchedEffect(api.name) {
        loadHomeFeed(false)
    }

    val performSearch = {
        if (searchQuery.isNotBlank()) {
            isSearchLoading = true
            hasSearched = true
            keyboardController?.hide()
            scope.launch {
                val results = withContext(Dispatchers.IO) {
                    try {
                        CsStreamRunner.safeSearch(api, searchQuery)
                    } catch (t: Throwable) {
                        Log.e("AddonExploreDialog", "Search failed: ${t.javaClass.simpleName}: ${t.message}")
                        emptyList()
                    }
                }
                searchResults = results
                isSearchLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = KitsugiColors.Background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = KitsugiColors.TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${api.name} Keşfet",
                        color = KitsugiColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { loadHomeFeed(true) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Yenile",
                            tint = KitsugiColors.TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Proactive Warnings for Blocked/Cloudflare Providers
                val isBlocked = remember(api.name) { CsPluginStatusTracker.isBlocked(api.name) }
                val isCfProtected = remember(api.name) { CsStreamRunner.CF_PROTECTED_PLUGINS.contains(api.name) || api.usesWebView }

                if (isBlocked) {
                    val blockReason = remember(api.name) { CsPluginStatusTracker.getErrorMessage(api.name) ?: "Bilinmeyen ağ veya ayrıştırma hatası" }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(KitsugiColors.AccentRed.copy(alpha = 0.15f))
                            .border(1.dp, KitsugiColors.AccentRed, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️ Bu eklenti geçici olarak engellendi: $blockReason. Eklenti sunucusu veya yapısı bozulmuş olabilir.",
                            color = KitsugiColors.AccentRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (isCfProtected) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(KitsugiColors.AccentOrange.copy(alpha = 0.15f))
                            .border(1.dp, KitsugiColors.AccentOrange, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "🔐 Bu eklenti Cloudflare korumalıdır. Video oynatılamazsa veya arama başarısız olursa WebView üzerinden captcha/doğrulamayı tamamladığınızdan emin olun.",
                            color = KitsugiColors.AccentOrange,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Search Bar within the Addon
                var isFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(
                            width = if (isFocused) 1.5.dp else 1.dp,
                            color = if (isFocused) accentColor else KitsugiColors.Border,
                            shape = RoundedCornerShape(22.dp)
                        )
                        .background(KitsugiColors.Surface)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Ara",
                            tint = KitsugiColors.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            cursorBrush = SolidColor(accentColor),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = KitsugiColors.TextPrimary,
                                fontWeight = FontWeight.Normal
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    performSearch()
                                }
                            ),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "${api.name} içinde ara...",
                                        color = KitsugiColors.TextMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    searchResults = emptyList()
                                    hasSearched = false
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Temizle",
                                    tint = KitsugiColors.TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display Search Results or Home Feed
                if (isSearchLoading || isHomeLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                } else if (hasSearched) {
                    // Show search results in a grid
                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sonuç bulunamadı.",
                                color = KitsugiColors.TextMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "Arama Sonuçları",
                                    color = KitsugiColors.TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }

                            // Render search results horizontally or vertically
                            // To match Cloudstream explore look, we can show them in horizontal grids or rows
                            val chunkedResults = searchResults.chunked(3)
                            items(chunkedResults) { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            ResultItemCard(
                                                title = item.name,
                                                imageUrl = item.posterUrl,
                                                apiName = api.name,
                                                quality = item.quality?.name,
                                                onClick = {
                                                    com.kitsugi.animelist.ui.screens.stream.KitsugiStreamActivity.start(
                                                        context = context,
                                                        malId = null,
                                                        aniListId = null,
                                                        episode = 1,
                                                        season = 1,
                                                        title = item.name,
                                                        posterUrl = item.posterUrl,
                                                        cs3Url = item.url,
                                                        cs3ApiName = api.name
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    // Add spacer placeholders if row is not full
                                    if (rowItems.size < 3) {
                                        for (i in 0 until (3 - rowItems.size)) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Show Home explore feed (rows of horizontal media lists)
                    if (homeLists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Keşfet içeriği yüklenemedi.",
                                color = KitsugiColors.TextMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            items(homeLists) { homeList ->
                                if (homeList.list.isNotEmpty()) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // ── Category header + See All button ──────────────
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = homeList.name,
                                                color = KitsugiColors.TextPrimary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (onSeeAllClick != null) {
                                                val matchingPage = api.mainPage
                                                    .firstOrNull { it.name == homeList.name }
                                                if (matchingPage != null) {
                                                    TextButton(
                                                        onClick = {
                                                            onSeeAllClick(
                                                                homeList.name,
                                                                matchingPage.data,
                                                                matchingPage.horizontalImages,
                                                                homeList.list
                                                            )
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                    ) {
                                                        Text(
                                                            text = "Tümünü Gör",
                                                            color = accentColor,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                                            malId = null,
                                                            aniListId = null,
                                                            episode = 1,
                                                            season = 1,
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
                            }
                        }
                    }
                }
            }
        }
    }
}
