package com.kitsugi.animelist.ui.components

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.utils.KitsugiImageDownloadHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KitsugiImageGalleryDialog(
    imageUrls: List<String>,
    initialIndex: Int = 0,
    title: String,
    onDismiss: () -> Unit
) {
    if (imageUrls.isEmpty()) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { imageUrls.size })
    val accentColor = LocalKitsugiAccent.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Slide-up and fade transitions state
    var isAnimatedVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isAnimatedVisible = true
    }

    val dismissWithAnimation = {
        scope.launch {
            isAnimatedVisible = false
            delay(280) // wait for exit animation to finish
            onDismiss()
        }
    }

    // Permission launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            val currentUrl = imageUrls.getOrNull(pagerState.currentPage)
            if (currentUrl != null) {
                KitsugiImageDownloadHelper.downloadImage(context, currentUrl, title)
            }
        } else {
            android.widget.Toast.makeText(context, "Depolama izni verilmedi.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Download button pulse animation
    val downloadPulse = rememberInfiniteTransition(label = "download_pulse")
    val downloadGlow by downloadPulse.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "download_glow"
    )

    Dialog(
        onDismissRequest = { dismissWithAnimation() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        AnimatedVisibility(
            visible = isAnimatedVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 300, easing = EaseOutCubic)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 280, easing = EaseInCubic)
            ) + fadeOut(animationSpec = tween(280))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                KitsugiColors.Surface.copy(alpha = 0.97f),
                                KitsugiColors.Background.copy(alpha = 0.99f)
                            ),
                            radius = 1800f
                        )
                    )
            ) {
                // Ambient glow behind current image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.04f),
                                    Color.Transparent,
                                    accentColor.copy(alpha = 0.03f)
                                )
                            )
                        )
                )

                // ─────────────────────────────────────
                // UNIFIED LAYOUT: Ana resim ortada, thumbnail altta
                // ─────────────────────────────────────

                // Ana Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = if (isLandscape) 12.dp else 16.dp,
                    verticalAlignment = Alignment.CenterVertically
                ) { page ->
                    GalleryImagePage(
                        imageUrl = imageUrls[page],
                        title = title,
                        page = page,
                        pagerState = pagerState,
                        onDismiss = { dismissWithAnimation() }
                    )
                }

                // Header (üst)
                KitsugiGalleryHeader(
                    title = title,
                    currentPage = pagerState.currentPage,
                    totalPages = imageUrls.size,
                    accentColor = accentColor,
                    downloadGlow = downloadGlow,
                    onDownload = {
                        val currentUrl = imageUrls.getOrNull(pagerState.currentPage)
                        if (currentUrl != null) {
                            if (KitsugiImageDownloadHelper.hasWritePermission(context)) {
                                KitsugiImageDownloadHelper.downloadImage(context, currentUrl, title)
                            } else {
                                launcher.launch(KitsugiImageDownloadHelper.getRequiredPermissions())
                            }
                        }
                    },
                    onDismiss = { dismissWithAnimation() },
                    isLandscape = isLandscape
                )

                // Thumbnail Strip (alt)
                if (imageUrls.size > 1) {
                    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

                    LaunchedEffect(pagerState.currentPage) {
                        val listInfo = listState.layoutInfo
                        val viewportWidth = listInfo.viewportEndOffset - listInfo.viewportStartOffset
                        if (viewportWidth > 0) {
                            val itemWidthPx = with(density) { (if (isLandscape) 40.dp else 52.dp).roundToPx() }
                            val targetOffset = (viewportWidth - itemWidthPx) / 2
                            listState.animateScrollToItem(pagerState.currentPage, -targetOffset)
                        } else {
                            listState.animateScrollToItem(pagerState.currentPage)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        KitsugiColors.Background.copy(alpha = if (isLandscape) 0.8f else 0.9f),
                                        KitsugiColors.Surface.copy(alpha = if (isLandscape) 0.90f else 0.96f)
                                    )
                                )
                            )
                            .navigationBarsPadding()
                            .padding(
                                bottom = if (isLandscape) 8.dp else 20.dp,
                                top = if (isLandscape) 12.dp else 24.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        LazyRow(
                            state = listState,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(imageUrls) { index, url ->
                                val isSelected = pagerState.currentPage == index
                                val thumbScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.12f else 0.88f,
                                    animationSpec = spring(dampingRatio = 0.6f),
                                    label = "thumb_scale_$index"
                                )
                                val thumbAlpha by animateFloatAsState(
                                    targetValue = if (isSelected) 1f else 0.38f,
                                    label = "thumb_alpha_$index"
                                )

                                Box(
                                    modifier = Modifier
                                        .graphicsLayer(scaleX = thumbScale, scaleY = thumbScale, alpha = thumbAlpha)
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Thumbnail $index",
                                        modifier = Modifier
                                            .size(
                                                width = if (isLandscape) 40.dp else 48.dp,
                                                height = if (isLandscape) 52.dp else 64.dp
                                            )
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) accentColor else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .tvClickable(shape = RoundedCornerShape(10.dp)) {
                                                scope.launch { pagerState.animateScrollToPage(index) }
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                    // Seçili sayfa indikatörü (küçük nokta)
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .width(if (isLandscape) 12.dp else 16.dp)
                                                .height(if (isLandscape) 2.dp else 3.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(accentColor)
                                                .align(Alignment.BottomCenter)
                                                .offset(y = if (isLandscape) 4.dp else 6.dp)
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

// Custom gesture detector helper to allow page swipes when not zoomed in
// Custom gesture detector helper to allow page swipes when not zoomed in
suspend fun PointerInputScope.detectTransformGesturesCustom(
    onGesture: (pan: Offset, zoom: Float) -> Unit,
    shouldConsume: () -> Boolean
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown()
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    onGesture(panChange, zoomChange)
                    if (shouldConsume()) {
                        event.changes.forEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryImagePage(
    imageUrl: String,
    title: String,
    page: Int,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onDismiss: () -> Unit
) {
    var zoomScale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(pagerState.currentPage) {
        zoomScale = 1f
        offset = Offset.Zero
    }

    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
    val isZoomed = zoomScale > 1f

    val dominoScale = if (isZoomed) 1f else (1f - (kotlin.math.abs(pageOffset) * 0.15f)).coerceIn(0.75f, 1f)
    val dominoAlpha = if (isZoomed) 1f else (1f - (kotlin.math.abs(pageOffset) * 0.45f)).coerceIn(0.35f, 1f)
    val dominoTranslationX = if (isZoomed) 0f else (pageOffset * 60.dp.value)
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (zoomScale > 1f) {
                                zoomScale = 1f
                                offset = Offset.Zero
                            } else {
                                zoomScale = 2.5f
                                offset = Offset.Zero
                            }
                        },
                        onTap = {
                            if (zoomScale == 1f) {
                                onDismiss()
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGesturesCustom(
                        onGesture = { pan, zoom ->
                            val newZoom = (zoomScale * zoom).coerceIn(1f, 5f)
                            zoomScale = newZoom
                            if (newZoom > 1f) {
                                val maxX = (widthPx * (newZoom - 1f)) / 2f
                                val maxY = (heightPx * (newZoom - 1f)) / 2f
                                offset = Offset(
                                    (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    (offset.y + pan.y).coerceIn(-maxY, maxY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        },
                        shouldConsume = { zoomScale > 1f }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "$title - Resim $page",
                modifier = Modifier
                    .fillMaxSize(0.88f)
                    .graphicsLayer(
                        scaleX = zoomScale * dominoScale,
                        scaleY = zoomScale * dominoScale,
                        translationX = offset.x + dominoTranslationX,
                        translationY = offset.y,
                        alpha = dominoAlpha
                    )
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun KitsugiGalleryHeader(
    title: String,
    currentPage: Int,
    totalPages: Int,
    accentColor: Color,
    downloadGlow: Float,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    isLandscape: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        // Glassmorphism header background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            KitsugiColors.Surface.copy(alpha = if (isLandscape) 0.82f else 0.90f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol: Başlık + Sayfa göstergesi
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // İkon kutusu
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = accentColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = accentColor.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            color = KitsugiColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (totalPages > 1) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${currentPage + 1}",
                                    color = accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "/ $totalPages",
                                    color = KitsugiColors.TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Sağ: Butonlar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // İndirme Butonu — accent renkli, animasyonlu
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = accentColor.copy(alpha = 0.18f * downloadGlow),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = accentColor.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .tvClickable(shape = RoundedCornerShape(12.dp), onClick = onDownload),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "İndir",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Kapatma Butonu — nötr, SurfaceSoft tabanlı
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = KitsugiColors.SurfaceStrong.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = KitsugiColors.Border,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .tvClickable(shape = RoundedCornerShape(12.dp), onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Kapat",
                            tint = KitsugiColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
