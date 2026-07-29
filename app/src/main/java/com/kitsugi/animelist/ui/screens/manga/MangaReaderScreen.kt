package com.kitsugi.animelist.ui.screens.manga

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.kitsugi.animelist.ui.components.KitsugiSheetOrDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.manga.MangaChapter
import com.kitsugi.animelist.data.manga.MangaSource
import com.kitsugi.animelist.data.manga.ReadingMode
import com.kitsugi.animelist.data.manga.ColorFilterType
import com.kitsugi.animelist.data.manga.MangaFitMode
import com.kitsugi.animelist.ui.screens.manga.components.MangaPagerReader
import com.kitsugi.animelist.ui.screens.manga.components.MangaWebtoonReader
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext

/**
 * Manga Okuyucu Ana Ekranı
 *
 * — Pager modu (LTR / RTL / Dikey) → MangaPagerReader
 * — Webtoon modu (sürekli dikey) → MangaWebtoonReader
 * — Üst ve alt barlar merkeze tıklanınca gizlenir/gösterilir (immersive)
 * — Alt çekme menüsünden okuma modu anında değiştirilebilir
 * — Sayfa ilerlemesi ve bölüm bilgisi alt çubukta gösterilir
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaReaderScreen(
    chapter: MangaChapter,
    mangaDetails: com.kitsugi.animelist.data.manga.MangaDetails,
    source: MangaSource,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: MangaReaderViewModel = viewModel(
        key = "${source.name}_${mangaDetails.url}_${chapter.url}",
        factory = MangaReaderViewModel.Factory(
            context = context,
            source = source,
            mangaDetails = mangaDetails,
            initialChapter = chapter
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val pages by viewModel.pages.collectAsState()

    val currentChapter = uiState.currentChapter
    val readingMode = uiState.readingMode
    val currentPage = uiState.currentPageIndex
    val loader = viewModel.loader

    val scope      = rememberCoroutineScope()
    var menuVisible     by remember { mutableStateOf(true) }
    var showSettings    by remember { mutableStateOf(false) }

    val pagerState    = rememberPagerState(initialPage = currentPage) { pages.size }
    val listState     = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)

    var hasRestoredProgress by remember(currentChapter.url) { mutableStateOf(false) }

    LaunchedEffect(currentChapter.url) {
        hasRestoredProgress = false
    }

    LaunchedEffect(pages, currentPage) {
        if (pages.isNotEmpty() && !hasRestoredProgress) {
            hasRestoredProgress = true
            val targetPage = currentPage.coerceIn(0, pages.size - 1)
            if (readingMode == ReadingMode.Webtoon) {
                listState.scrollToItem(targetPage)
            } else {
                pagerState.scrollToPage(targetPage)
            }
        }
    }

    // Pager'daki sayfa değişimini takip et — V2 prefetch tetikle
    LaunchedEffect(pagerState.currentPage) {
        if (hasRestoredProgress && pages.isNotEmpty()) {
            viewModel.onPageChanged(currentChapter.url, pagerState.currentPage, pages.size)
            // R2: Sayfa değiştikçe V2 loader'da 3 ileri prefetch'i başlat
            loader.onPageChanged(pagerState.currentPage)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ─── Okuyucu İçeriği ─────────────────────────────────────────────────
        if (uiState.isLoadingPages) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFF7C4DFF),
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Sayfa listesi yükleniyor...", color = Color.White.copy(0.6f), fontSize = 13.sp)
                }
            }
        } else if (uiState.pagesError != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = Color.White.copy(0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = uiState.pagesError ?: "Bir hata oluştu",
                        color = Color.White.copy(0.7f),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.retryLoadPages() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                    ) {
                        Text("Tekrar Dene", color = Color.White)
                    }
                }
            }
        } else if (pages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Herhangi bir sayfa bulunamadı.", color = Color.White.copy(0.5f))
            }
        } else {
            when (readingMode) {
                ReadingMode.Webtoon -> MangaWebtoonReader(
                    pages        = pages,
                    listState    = listState,
                    loader       = loader,
                    colorFilterType = uiState.colorFilterType,
                    fitMode      = uiState.fitMode,
                    hasNextChapter = viewModel.hasNextChapter,
                    nextChapterName = viewModel.nextChapterName,
                    onGoToNextChapter = { viewModel.goToNextChapter() },
                    onCenterTap  = { menuVisible = !menuVisible },
                    onPageChanged = { idx ->
                        if (hasRestoredProgress) {
                            viewModel.onPageChanged(currentChapter.url, idx, pages.size)
                            // R2: Webtoon scroll'da V2 prefetch tetikle
                            loader.onPageChanged(idx)
                        }
                    },
                    onNextPage   = {
                        val current = listState.firstVisibleItemIndex
                        if (current < pages.size - 1) {
                            scope.launch {
                                listState.animateScrollToItem(current + 1)
                            }
                        } else {
                            viewModel.goToNextChapter()
                        }
                    },
                    onPrevPage   = {
                        val current = listState.firstVisibleItemIndex
                        if (current > 0) {
                            scope.launch {
                                listState.animateScrollToItem(current - 1)
                            }
                        } else {
                            viewModel.goToPreviousChapter()
                        }
                    }
                )
                else -> MangaPagerReader(
                    pages        = pages,
                    pagerState   = pagerState,
                    loader       = loader,
                    readingMode  = readingMode,
                    colorFilterType = uiState.colorFilterType,
                    fitMode      = uiState.fitMode,
                    onCenterTap  = { menuVisible = !menuVisible },
                    onNextPage   = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            viewModel.goToNextChapter()
                        }
                    },
                    onPrevPage   = {
                        if (pagerState.currentPage > 0) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        } else {
                            viewModel.goToPreviousChapter()
                        }
                    }
                )
            }
        }

        // ─── Özel Parlaklık Katmanı ───────────────────────────────────────────
        if (uiState.customBrightness < 1.0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 1.0f - uiState.customBrightness))
            )
        }

        // ─── Üst Bar ─────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = menuVisible,
            enter   = fadeIn() + slideInVertically { -it },
            exit    = fadeOut() + slideOutVertically { -it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(0.85f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            mangaDetails.title,
                            color     = Color.White,
                            fontSize  = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines  = 1,
                            overflow  = TextOverflow.Ellipsis
                        )
                        Text(
                            currentChapter.name,
                            color    = Color.White.copy(0.65f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Okuyucu Ayarları",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // ─── Alt Bar (Aniyomi Parity) ──────────────────────────────────────────
        AnimatedVisibility(
            visible = menuVisible && pages.isNotEmpty(),
            enter   = fadeIn() + slideInVertically { it },
            exit    = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val barBackgroundColor = Color(0xFF161622).copy(alpha = 0.95f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barBackgroundColor)
                    .navigationBarsPadding()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. ChapterNavigator (Aniyomi Style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Chapter Button
                    IconButton(
                        onClick = { viewModel.goToPreviousChapter() },
                        enabled = viewModel.hasPreviousChapter
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Önceki Bölüm",
                            tint = if (viewModel.hasPreviousChapter) Color.White else Color.White.copy(0.3f)
                        )
                    }

                    // Slider Container
                    if (pages.size > 1) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF26263B))
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currentPage + 1}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            var sliderValue by remember(currentPage) { mutableStateOf((currentPage + 1).toFloat()) }
                            Slider(
                                value = sliderValue,
                                onValueChange = {
                                    sliderValue = it
                                    val targetPage = it.toInt() - 1
                                    scope.launch {
                                        if (readingMode == ReadingMode.Webtoon) {
                                            listState.scrollToItem(targetPage.coerceIn(0, pages.size - 1))
                                        } else {
                                            pagerState.scrollToPage(targetPage.coerceIn(0, pages.size - 1))
                                        }
                                    }
                                },
                                valueRange = 1f..pages.size.toFloat().coerceAtLeast(1f),
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color(0xFF7C4DFF),
                                    inactiveTrackColor = Color(0xFF3D3D5C),
                                    thumbColor = Color(0xFF7C4DFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )

                            Text(
                                text = "${pages.size}",
                                color = Color.White.copy(0.6f),
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Next Chapter Button
                    IconButton(
                        onClick = { viewModel.goToNextChapter() },
                        enabled = viewModel.hasNextChapter
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Sonraki Bölüm",
                            tint = if (viewModel.hasNextChapter) Color.White else Color.White.copy(0.3f)
                        )
                    }
                }

                // 2. BottomReaderBar (Aniyomi Style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Reading Mode cycle
                    val readingModeIcon = when (readingMode) {
                        ReadingMode.RightToLeft -> Icons.AutoMirrored.Filled.MenuBook
                        ReadingMode.LeftToRight -> Icons.Default.SwapHoriz
                        ReadingMode.Vertical    -> Icons.Default.ViewStream
                        ReadingMode.Webtoon     -> Icons.Default.ViewStream
                    }
                    IconButton(onClick = {
                        val nextMode = when (readingMode) {
                            ReadingMode.RightToLeft -> ReadingMode.LeftToRight
                            ReadingMode.LeftToRight -> ReadingMode.Vertical
                            ReadingMode.Vertical    -> ReadingMode.Webtoon
                            ReadingMode.Webtoon     -> ReadingMode.RightToLeft
                        }
                        viewModel.setReadingMode(nextMode)
                    }) {
                        Icon(
                            imageVector = readingModeIcon,
                            contentDescription = "Okuma Yönü",
                            tint = Color.White
                        )
                    }

                    // Quick Orientation cycle
                    IconButton(onClick = {
                        val activity = context.findActivity()
                        if (activity != null) {
                            val currentOrientation = activity.requestedOrientation
                            if (currentOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Ekran Döndürme",
                            tint = Color.White
                        )
                    }

                    // Quick Fit Mode cycle
                    IconButton(onClick = {
                        val nextFit = when (uiState.fitMode) {
                            MangaFitMode.FitScreen -> MangaFitMode.FitWidth
                            MangaFitMode.FitWidth  -> MangaFitMode.FitHeight
                            MangaFitMode.FitHeight -> MangaFitMode.FitScreen
                        }
                        viewModel.setFitMode(nextFit)
                    }) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = "Sığdırma Modu",
                            tint = Color.White
                        )
                    }

                    // Settings Dialog Trigger
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ayarlar",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // ─── Okuma Modu Ayarlar Paneli ────────────────────────────────────────────
    if (showSettings) {
        KitsugiSheetOrDialog(
            onDismiss = { showSettings = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "Okuyucu Ayarları",
                    color     = Color.White,
                    fontSize  = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier  = Modifier.padding(bottom = 16.dp)
                )

                // Okuma Yönü
                Text(
                    "Okuma Yönü",
                    color = Color.White.copy(0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        ReadingMode.RightToLeft to "RTL (Manga)",
                        ReadingMode.LeftToRight to "LTR",
                        ReadingMode.Vertical    to "Dikey",
                        ReadingMode.Webtoon     to "Webtoon"
                    )
                    modes.forEach { (mode, label) ->
                        FilterChip(
                            selected = readingMode == mode,
                            onClick  = { viewModel.setReadingMode(mode) },
                            label    = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor   = Color(0xFF7C4DFF).copy(0.2f),
                                selectedLabelColor       = Color(0xFF7C4DFF),
                                containerColor           = Color(0xFF2A2A40),
                                labelColor               = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Görsel Sığdırma Modu
                Text(
                    "Görsel Sığdırma Modu",
                    color = Color.White.copy(0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fits = listOf(
                        MangaFitMode.FitScreen to "Ekrana Sığdır",
                        MangaFitMode.FitWidth  to "Genişlik",
                        MangaFitMode.FitHeight to "Yükseklik"
                    )
                    fits.forEach { (fit, label) ->
                        FilterChip(
                            selected = uiState.fitMode == fit,
                            onClick  = { viewModel.setFitMode(fit) },
                            label    = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor   = Color(0xFF7C4DFF).copy(0.2f),
                                selectedLabelColor       = Color(0xFF7C4DFF),
                                containerColor           = Color(0xFF2A2A40),
                                labelColor               = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Renk Filtresi
                Text(
                    "Renk Filtresi",
                    color = Color.White.copy(0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        ColorFilterType.Normal    to "Normal",
                        ColorFilterType.Grayscale to "Siyah-Beyaz",
                        ColorFilterType.Sepia     to "Sepya",
                        ColorFilterType.Invert    to "Ters Negatif"
                    )
                    filters.forEach { (filter, label) ->
                        FilterChip(
                            selected = uiState.colorFilterType == filter,
                            onClick  = { viewModel.setColorFilter(filter) },
                            label    = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor   = Color(0xFF7C4DFF).copy(0.2f),
                                selectedLabelColor       = Color(0xFF7C4DFF),
                                containerColor           = Color(0xFF2A2A40),
                                labelColor               = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Özel Parlaklık Sürgüsü
                Text(
                    "Okuyucu Parlaklığı",
                    color = Color.White.copy(0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BrightnessLow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                    Slider(
                        value = uiState.customBrightness,
                        onValueChange = { viewModel.setCustomBrightness(it) },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF7C4DFF),
                            inactiveTrackColor = Color(0xFF2A2A40),
                            thumbColor = Color(0xFF7C4DFF)
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.BrightnessHigh,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun ReadingMode.label() = when (this) {
    ReadingMode.RightToLeft -> "Sağdan Sola"
    ReadingMode.LeftToRight -> "Soldan Sağa"
    ReadingMode.Vertical    -> "Dikey"
    ReadingMode.Webtoon     -> "Webtoon"
}

private fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}
