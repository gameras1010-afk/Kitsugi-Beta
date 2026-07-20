package com.kitsugi.animelist.ui.screens.manga.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.manga.MangaPage
import com.kitsugi.animelist.data.manga.MangaPageStatus
import com.kitsugi.animelist.data.manga.ReadingMode
import com.kitsugi.animelist.data.manga.loader.MangaPageLoaderV2
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.kitsugi.animelist.data.manga.ColorFilterType
import com.kitsugi.animelist.data.manga.MangaFitMode

/**
 * Yatay (LTR/RTL) ve dikey pager okuma modları için tek sayfa bileşeni.
 *
 * — Telephoto'nun ZoomableAsyncImage bileşeni ile:
 *     • Çift tıklama ile yakınlaştırma/uzaklaştırma
 *     • Parmakla pinch-to-zoom
 *     • Büyük çözünürlüklü resimlerin kiremit döşemeli (subsampling) çizimi
 *     • OOM (Out of Memory) riskini sıfıra indirme
 *
 * — Sayfa Ready olana kadar yükleme göstergesi gösterilir.
 * — Hata durumunda "Tekrar Dene" butonu görünür.
 */
@Composable
fun MangaPageItem(
    page: MangaPage,
    loader: MangaPageLoaderV2,
    readingMode: ReadingMode = ReadingMode.RightToLeft,
    colorFilterType: ColorFilterType = ColorFilterType.Normal,
    fitMode: MangaFitMode = MangaFitMode.FitScreen,
    onCenterTap: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onPrevPage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Sayfa nesnesi ilk kez görüldüğünde yüklemeyi başlat.
    // Key olarak identityHashCode kullanıyoruz: aynı sayfa nesnesi varsa yeniden tetiklenmez,
    // ama yeni bir bölüme geçildiğinde (yeni page nesneleri) effect yeniden çalışır.
    LaunchedEffect(System.identityHashCode(page)) {
        loader.loadPage(page)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(readingMode) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    val leftEdge = width * 0.33f
                    val rightEdge = width * 0.66f

                    if (offset.x < leftEdge) {
                        if (readingMode == ReadingMode.RightToLeft) {
                            onNextPage()
                        } else {
                            onPrevPage()
                        }
                    } else if (offset.x > rightEdge) {
                        if (readingMode == ReadingMode.RightToLeft) {
                            onPrevPage()
                        } else {
                            onNextPage()
                        }
                    } else {
                        onCenterTap()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (page.status) {
            MangaPageStatus.Ready -> {
                val imageUrl = page.imageUrl ?: page.url
                val cacheFile = loader.cache.getImageFile(imageUrl)
                val modelToLoad: Any = if (cacheFile.exists()) cacheFile else imageUrl

                val context = LocalContext.current
                val imageRequest = remember(modelToLoad) {
                    ImageRequest.Builder(context)
                        .data(modelToLoad)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .crossfade(200)
                        .build()
                }

                val zoomableState = rememberZoomableState()
                val imageState    = rememberZoomableImageState(zoomableState)

                val contentScale = when (fitMode) {
                    MangaFitMode.FitScreen -> ContentScale.Fit
                    MangaFitMode.FitWidth -> ContentScale.FillWidth
                    MangaFitMode.FitHeight -> ContentScale.FillHeight
                }

                val composeColorFilter = when (colorFilterType) {
                    ColorFilterType.Normal -> null
                    ColorFilterType.Grayscale -> {
                        val matrix = ColorMatrix(
                            floatArrayOf(
                                0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                                0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                                0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                                0f,      0f,      0f,      1f, 0f
                            )
                        )
                        ColorFilter.colorMatrix(matrix)
                    }
                    ColorFilterType.Sepia -> {
                        val matrix = ColorMatrix(
                            floatArrayOf(
                                0.393f, 0.769f, 0.189f, 0f, 0f,
                                0.349f, 0.686f, 0.168f, 0f, 0f,
                                0.272f, 0.534f, 0.131f, 0f, 0f,
                                0f,     0f,     0f,     1f, 0f
                            )
                        )
                        ColorFilter.colorMatrix(matrix)
                    }
                    ColorFilterType.Invert -> {
                        val matrix = ColorMatrix(
                            floatArrayOf(
                                -1f,  0f,  0f, 0f, 1f,
                                 0f, -1f,  0f, 0f, 1f,
                                 0f,  0f, -1f, 0f, 1f,
                                 0f,  0f,  0f, 1f, 0f
                            )
                        )
                        ColorFilter.colorMatrix(matrix)
                    }
                }

                ZoomableAsyncImage(
                    model       = imageRequest,
                    contentDescription = "Manga sayfası ${page.index + 1}",
                    state       = imageState,
                    contentScale = contentScale,
                    colorFilter = composeColorFilter,
                    modifier    = Modifier.fillMaxSize()
                )
            }

            MangaPageStatus.Error -> {
                ErrorPageContent(
                    pageIndex = page.index,
                    onRetry   = { loader.retryPage(page) }
                )
            }

            else -> {
                LoadingPageContent(page = page)
            }
        }

        // Sol alt köşede sayfa numarası
        Text(
            text     = "${page.index + 1}",
            color    = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        )
    }
}

@Composable
private fun LoadingPageContent(page: MangaPage) {
    Box(
        modifier          = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .background(Color(0xFF0A0A0A)),
        contentAlignment  = Alignment.Center
    ) {
        when (page.status) {
            MangaPageStatus.DownloadImage,
            MangaPageStatus.LoadPage -> {
                CircularProgressIndicator(
                    color    = Color(0xFF7C4DFF),
                    modifier = Modifier.size(48.dp)
                )
            }
            MangaPageStatus.Queue -> {
                Icon(
                    imageVector         = Icons.Default.HourglassBottom,
                    contentDescription  = null,
                    tint                = Color.White.copy(alpha = 0.3f),
                    modifier            = Modifier.size(40.dp)
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun ErrorPageContent(pageIndex: Int, onRetry: () -> Unit) {
    Box(
        modifier          = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(Color(0xFF120A0A)),
        contentAlignment  = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector        = Icons.Default.BrokenImage,
                contentDescription = null,
                tint               = Color(0xFFEF5350),
                modifier           = Modifier.size(52.dp)
            )
            Text(
                text      = "Sayfa ${pageIndex + 1} yüklenemedi",
                color     = Color.White.copy(alpha = 0.7f),
                fontSize  = 13.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(top = 8.dp)
            )
            TextButton(onClick = onRetry) {
                Text("Tekrar Dene", color = Color(0xFF7C4DFF))
            }
        }
    }
}
