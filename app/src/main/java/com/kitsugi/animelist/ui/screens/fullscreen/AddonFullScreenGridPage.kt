@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kitsugi.animelist.ui.screens.fullscreen

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.cloudstream.CsPluginLoader
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.ui.app.AddonFullScreenGridState
import com.kitsugi.animelist.ui.components.KitsugiEmptyState
import com.kitsugi.animelist.ui.screens.search.components.ResultItemCard
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-screen grid page for a single Cloudstream addon category.
 *
 * Replicates the feel of [FullScreenMediaGridPage] but sources content from a Cloudstream
 * [com.lagradost.cloudstream3.MainAPI] using [MainPageRequest]-based pagination instead of
 * Jikan/AniList/TMDB APIs.
 */
@Composable
fun AddonFullScreenGridPage(
    state: AddonFullScreenGridState,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val scope = rememberCoroutineScope()

    // ── Layout ──────────────────────────────────────────────────────────────
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val columnCount = remember(isLandscape, screenWidthDp) {
        if (isLandscape) {
            when {
                screenWidthDp >= 1200 -> 5
                screenWidthDp >= 800  -> 4
                else                  -> 3
            }
        } else {
            when {
                screenWidthDp >= 900 -> 5
                screenWidthDp >= 600 -> 4
                else                 -> 3
            }
        }
    }

    // ── State ────────────────────────────────────────────────────────────────
    var loadedItems by remember { mutableStateOf(state.initialItems) }
    var currentPage by remember { mutableIntStateOf(if (state.initialItems.isEmpty()) 0 else 1) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMorePages by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var apiReady by remember { mutableStateOf(false) }
    var activeDetailItem by remember { mutableStateOf<com.lagradost.cloudstream3.SearchResponse?>(null) }
    var activeDetailApiName by remember { mutableStateOf<String?>(null) }

    // Resolve the MainAPI instance (may need to load the plugin first)
    LaunchedEffect(state.apiName) {
        withContext(Dispatchers.IO) {
            runCatching {
                val db = KitsugiDatabase.getDatabase(context.applicationContext)
                val plugins = db.csPluginDao().getEnabledPlugins()
                for (plugin in plugins) {
                    runCatching { CsPluginLoader.loadExtension(context, plugin.id) }
                }
            }
        }
        apiReady = true
        if (loadedItems.isEmpty()) {
            // trigger first page load
        }
    }

    fun resolveApi() = APIHolder.allProviders.firstOrNull { it.name.equals(state.apiName, ignoreCase = true) }

    fun loadNextPage() {
        if (isLoadingMore || !hasMorePages) return
        val api = resolveApi() ?: run {
            loadError = "Eklenti bulunamadı: ${state.apiName}"
            return
        }
        isLoadingMore = true
        loadError = null
        scope.launch {
            try {
                val nextPage = currentPage + 1
                val request = MainPageRequest(state.title, state.mainPageData, state.horizontalImages)
                val response = withContext(Dispatchers.IO) {
                    runCatching { api.getMainPage(nextPage, request) }.getOrNull()
                }
                if (response != null) {
                    val newItems = response.items.flatMap { it.list }
                    if (newItems.isNotEmpty()) {
                        loadedItems = (loadedItems + newItems).distinctBy { it.url }
                        currentPage = nextPage
                    }
                    // Respect the plugin's own hasNext flag (CS3 standard)
                    hasMorePages = response.hasNext
                } else {
                    hasMorePages = false
                }
            } catch (e: Exception) {
                Log.e("AddonFullScreenGridPage", "Pagination failed: ${e.message}")
                loadError = e.message ?: "Yükleme hatası"
            } finally {
                isLoadingMore = false
            }
        }
    }

    // Initial load if no items were pre-seeded
    LaunchedEffect(apiReady) {
        if (apiReady && loadedItems.isEmpty()) {
            loadNextPage()
        }
    }

    // ── Grid scroll + auto-load trigger ─────────────────────────────────────
    val gridState = rememberLazyGridState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            last.index >= gridState.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore && apiReady) loadNextPage() }

    val showFloatingHeader by remember {
        derivedStateOf { gridState.firstVisibleItemIndex >= 1 }
    }
    val showScrollToTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > 3 }
    }

    // ── UI ───────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KitsugiColors.Background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            item(key = "addon_grid_header", span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
                    // Geri butonu
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onBackClick) {
                            Text("Geri", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Başlık + eklenti adı badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.title,
                            color = KitsugiColors.TextPrimary,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f)
                        )
                        // Eklenti adı badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(KitsugiColors.Surface)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = state.apiName,
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${loadedItems.size} içerik",
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Empty state ──────────────────────────────────────────────────
            if (loadedItems.isEmpty() && !isLoadingMore) {
                item(key = "addon_grid_empty", span = { GridItemSpan(maxLineSpan) }) {
                    KitsugiEmptyState(
                        title = "Henüz içerik yok",
                        subtitle = "Bu kategoride gösterilecek içerik bulunamadı.",
                        icon = Icons.Rounded.SearchOff
                    )
                }
            }

            // ── Items ────────────────────────────────────────────────────────
            itemsIndexed(
                loadedItems,
                key = { idx, item -> "${state.apiName}_${item.url}_$idx" }
            ) { _, item ->
                ResultItemCard(
                    title = item.name,
                    imageUrl = item.posterUrl,
                    apiName = state.apiName,
                    quality = item.quality?.name,
                    onClick = {
                        activeDetailItem = item
                        activeDetailApiName = state.apiName
                    }
                )
            }

            // ── Loading indicator ─────────────────────────────────────────────
            if (isLoadingMore) {
                item(key = "addon_grid_loading", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = accentColor) }
                }
            }

            // ── Error / retry ────────────────────────────────────────────────
            if (loadError != null) {
                item(key = "addon_grid_error", span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = loadError ?: "Bir hata oluştu.",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { loadNextPage() }) {
                            Text("Tekrar Dene", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Floating header ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showFloatingHeader,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(KitsugiColors.Surface.copy(alpha = 0.92f))
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Geri",
                        tint = KitsugiColors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.title,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Eklenti adı — floating header'da
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(KitsugiColors.Surface)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = state.apiName,
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Scroll to top FAB ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor)
                    .tvClickable(shape = RoundedCornerShape(16.dp)) {
                        scope.launch { gridState.animateScrollToItem(0) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Yukarı Git",
                    tint = KitsugiColors.Background,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    // ── Detail Dialog ───────────────────────────────────────────────────
    activeDetailItem?.let { detailResponse ->
        val detailApi = remember(activeDetailApiName) {
            APIHolder.allProviders.firstOrNull { it.name.equals(activeDetailApiName, ignoreCase = true) }
        }
        if (detailApi != null) {
            com.kitsugi.animelist.ui.screens.search.components.KitsugiAddonDetailDialog(
                api = detailApi,
                url = detailResponse.url,
                onDismissRequest = { activeDetailItem = null; activeDetailApiName = null }
            )
        }
    }
}
