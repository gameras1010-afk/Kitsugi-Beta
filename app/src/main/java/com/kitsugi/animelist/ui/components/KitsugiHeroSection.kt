package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.kitsugi.animelist.data.remote.KitsugiEpisodeRatingsRepository
import com.kitsugi.animelist.utils.copyOnDoubleTap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.matches
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalIsTvDevice
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val HERO_BACKGROUND_PARALLAX = 0.055f
private const val HERO_BACKGROUND_SCALE = 1.14f
private const val HERO_CONTENT_PARALLAX = 0.18f

private data class HeroPageLayer(
    val page: Int,
    val visibility: Float,
    val offset: Float,
)

internal data class HomeHeroLayout(
    val isTablet: Boolean,
    val heroHeight: Dp,
    val contentMaxWidth: Dp,
    val contentWidthFraction: Float,
    val contentHorizontalPadding: Dp,
    val contentVerticalPadding: Dp,
    val bottomFadeHeight: Dp,
    val logoWidthFraction: Float,
)

@Composable
fun KitsugiHeroSection(
    items: List<JikanSearchResult>,
    alreadyInList: (JikanSearchResult) -> Boolean,
    onInfoClick: (JikanSearchResult) -> Unit,
    modifier: Modifier = Modifier,
    titleLanguage: String = "ROMAJI",
    scoreFormat: String = "POINT_10",
    hideScores: Boolean = false,
    showAnimeLogos: Boolean = false,
    isVisible: Boolean = true,
    blurAdultMedia: Boolean = false
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()
    val accentColor = LocalKitsugiAccent.current

    var logos by remember { mutableStateOf<Map<Int, String?>>(emptyMap()) }

    LaunchedEffect(items, showAnimeLogos) {
        if (!showAnimeLogos) {
            logos = emptyMap()
            return@LaunchedEffect
        }
        val logoMap = mutableMapOf<Int, String?>()

        val priorityIndices = buildList {
            val cur = pagerState.currentPage.coerceIn(items.indices)
            add(cur)
            items.indices.filter { it != cur }.forEach { add(it) }
        }

        for (idx in priorityIndices) {
            val item = items[idx]
            val stableId = item.malId
            val logoUrl = when {
                item.source.equals("tmdb", ignoreCase = true) -> {
                    if (stableId > 0) KitsugiEpisodeRatingsRepository.getLogoUrl(stableId) else null
                }
                item.source.equals("anilist", ignoreCase = true) -> {
                    if (stableId >= 100_000_000) {
                        val aniListId = stableId - 100_000_000
                        KitsugiEpisodeRatingsRepository.getLogoUrlByAniListId(
                            aniListId = aniListId,
                            fallbackMalId = item.realMalId
                        )
                    } else {
                        KitsugiEpisodeRatingsRepository.getLogoUrlByMalId(stableId)
                    }
                }
                else -> {
                    val aniListFallback = if (stableId >= 100_000_000) stableId - 100_000_000 else null
                    KitsugiEpisodeRatingsRepository.getLogoUrlByMalId(
                        malId = stableId,
                        fallbackAniListId = aniListFallback
                    )
                }
            }
            logoMap[item.malId] = logoUrl
        }
        // Tüm logolar yüklendikten sonra tek seferinde state güncellemesi yap
        // (her logo için ayrı recomposition tetiklemek yerine)
        logos = logoMap.toMap()
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isTablet = screenWidthDp >= 600f

    val isTvDevice = LocalIsTvDevice.current
    val isTv = isTvDevice
    val layout = remember(screenWidthDp, screenHeightDp, isTv) {
        if (isTv) {
            HomeHeroLayout(
                isTablet = true,
                heroHeight = (screenHeightDp * 0.72f).coerceIn(400f, 500f).dp,
                contentMaxWidth = 720.dp,
                contentWidthFraction = 0.58f,
                contentHorizontalPadding = 48.dp,
                contentVerticalPadding = 32.dp,
                bottomFadeHeight = 220.dp,
                logoWidthFraction = 0.5f,
            )
        } else if (isTablet) {
            when {
                screenWidthDp >= 1200f -> HomeHeroLayout(
                    isTablet = true,
                    heroHeight = (screenWidthDp * 0.42f).coerceIn(360f, 440f).dp,
                    contentMaxWidth = 640.dp,
                    contentWidthFraction = 0.56f,
                    contentHorizontalPadding = 56.dp,
                    contentVerticalPadding = 22.dp,
                    bottomFadeHeight = 190.dp,
                    logoWidthFraction = 0.58f,
                )
                screenWidthDp >= 840f -> HomeHeroLayout(
                    isTablet = true,
                    heroHeight = (screenWidthDp * 0.46f).coerceIn(340f, 420f).dp,
                    contentMaxWidth = 560.dp,
                    contentWidthFraction = 0.62f,
                    contentHorizontalPadding = 40.dp,
                    contentVerticalPadding = 20.dp,
                    bottomFadeHeight = 180.dp,
                    logoWidthFraction = 0.56f,
                )
                else -> HomeHeroLayout(
                    isTablet = true,
                    heroHeight = (screenWidthDp * 0.58f).coerceIn(320f, 380f).dp,
                    contentMaxWidth = 520.dp,
                    contentWidthFraction = 0.72f,
                    contentHorizontalPadding = 32.dp,
                    contentVerticalPadding = 18.dp,
                    bottomFadeHeight = 170.dp,
                    logoWidthFraction = 0.54f,
                )
            }
        } else {
            val viewportDrivenHeight = screenHeightDp * 0.82f
            val baseHeight = viewportDrivenHeight
            val cappedHeight = baseHeight - 140f
            val finalHeight = cappedHeight.coerceIn(360f, 760f).dp
            HomeHeroLayout(
                isTablet = false,
                heroHeight = finalHeight,
                contentMaxWidth = 480.dp,
                contentWidthFraction = 1f,
                contentHorizontalPadding = 20.dp,
                contentVerticalPadding = 20.dp,
                bottomFadeHeight = 220.dp,
                logoWidthFraction = 0.62f,
            )
        }
    }

    val currentPage = pagerState.currentPage.coerceIn(items.indices)
    val visiblePages = listOf(
        currentPage,
        (currentPage - 1).coerceIn(items.indices),
        (currentPage + 1).coerceIn(items.indices)
    ).distinct()
        .mapNotNull { index ->
            val pageOffset = heroPageOffset(pagerState, index)
            val visibility = (1f - abs(pageOffset)).coerceIn(0f, 1f)
            if (visibility <= 0f) {
                null
            } else {
                HeroPageLayer(
                    page = index,
                    visibility = visibility,
                    offset = pageOffset
                )
            }
        }
        .sortedBy { it.visibility }

    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible, isFocused, pagerState.currentPage, pagerState.isScrollInProgress, items.size) {
        if (items.size > 1 && isVisible && !isFocused && !pagerState.isScrollInProgress) {
            delay(5000L)
            val nextPage = (pagerState.currentPage + 1) % items.size
            coroutineScope.launch {
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(layout.heroHeight)
            .then(
                if (isTv) {
                    Modifier
                        .focusable()
                        .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionLeft -> {
                                        if (pagerState.currentPage > 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                            true
                                        } else false
                                    }
                                    Key.DirectionRight -> {
                                        if (pagerState.currentPage < items.size - 1) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                            true
                                        } else false
                                    }
                                    Key.DirectionCenter,
                                    Key.Enter,
                                    Key.NumPadEnter -> {
                                        onInfoClick(items[pagerState.currentPage])
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                } else {
                    Modifier.homeHeroPagerGesture(
                        pagerState = pagerState,
                        itemCount = items.size,
                        coroutineScope = coroutineScope
                    )
                }
            )
            .then(
                if (isTv) {
                    Modifier.border(
                        width = 3.dp,
                        color = if (isFocused) accentColor else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(KitsugiColors.Surface)
    ) {
        val heroWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        // Parallax scroll efekti kaldırıldı — her px değişiminde recomposition yapıyordu.
        // Statik scale değerleri kullanılıyor, performans önemli ölçüde arttı.
        val heroScrollScale = 1f
        val heroScrollTranslationY = 0f

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.01f }
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }

        visiblePages.forEach { layer ->
            val item = items[layer.page]
            val displayTitle = item.getDisplayTitle(titleLanguage)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isTv) {
                            Modifier
                        } else {
                            Modifier.tvClickable(
                                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                                scaleOnFocus = false
                            ) { onInfoClick(item) }
                        }
                    )
            ) {
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                val heroImageModel = if (isLandscape) item.backdropUrl ?: item.imageUrl else item.imageUrl
                if (!heroImageModel.isNullOrBlank()) {
                    AsyncImage(
                        model = heroImageModel,
                        contentDescription = displayTitle,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (blurAdultMedia && item.isAdult) Modifier.blur(24.dp)
                                else Modifier
                            )
                            .graphicsLayer {
                                alpha = layer.visibility
                                translationX = -layer.offset * heroWidthPx * HERO_BACKGROUND_PARALLAX
                                translationY = heroScrollTranslationY
                                scaleX = HERO_BACKGROUND_SCALE * heroScrollScale
                                scaleY = HERO_BACKGROUND_SCALE * heroScrollScale
                            },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = layer.visibility
                                translationX = -layer.offset * heroWidthPx * HERO_BACKGROUND_PARALLAX
                                translationY = heroScrollTranslationY
                                scaleX = HERO_BACKGROUND_SCALE * heroScrollScale
                                scaleY = HERO_BACKGROUND_SCALE * heroScrollScale
                            }
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayTitle.take(2).uppercase(),
                            color = accentColor,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            KitsugiColors.Background.copy(alpha = 0.02f),
                            KitsugiColors.Background.copy(alpha = 0.12f),
                            KitsugiColors.Background.copy(alpha = 0.34f),
                            KitsugiColors.Background.copy(alpha = 0.78f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.bottomFadeHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            KitsugiColors.Background.copy(alpha = 0f),
                            KitsugiColors.Background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    horizontal = layout.contentHorizontalPadding,
                    vertical = layout.contentVerticalPadding
                ),
            horizontalAlignment = if (layout.isTablet) Alignment.Start else Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(layout.contentWidthFraction)
                    .widthIn(max = layout.contentMaxWidth),
                contentAlignment = if (layout.isTablet) Alignment.CenterStart else Alignment.Center
            ) {
                visiblePages.forEach { layer ->
                    val item = items[layer.page]
                    val displayTitle = item.getDisplayTitle(titleLanguage)
                    val alreadyInList = alreadyInList(item)

                    val context = LocalContext.current
                    val copyTitleGesture = Modifier.copyOnDoubleTap(context, displayTitle)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = layer.visibility
                                translationX = -layer.offset * heroWidthPx * HERO_CONTENT_PARALLAX
                            }
                            .then(
                                if (isTv) {
                                    Modifier
                                } else {
                                    Modifier.tvClickable(
                                        shape = RoundedCornerShape(12.dp),
                                        scaleOnFocus = false
                                    ) { onInfoClick(item) }
                                }
                            ),
                        horizontalAlignment = if (layout.isTablet) Alignment.Start else Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (layout.isTablet) {
                                Arrangement.spacedBy(8.dp, Alignment.Start)
                            } else {
                                Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                            }
                        ) {
                            Text(
                                text = "ÖNE ÇIKAN",
                                color = accentColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black
                            )

                            if (alreadyInList) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(KitsugiColors.AccentGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "LİSTEDE",
                                        color = KitsugiColors.AccentGreen,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val logoUrl = if (showAnimeLogos) logos[item.malId] else null
                        if (!logoUrl.isNullOrBlank()) {
                            var logoFailed by remember(logoUrl) { mutableStateOf(false) }
                            if (!logoFailed) {
                                AsyncImage(
                                    model = logoUrl,
                                    contentDescription = displayTitle,
                                    modifier = Modifier
                                        .align(if (layout.isTablet) Alignment.Start else Alignment.CenterHorizontally)
                                        .height(if (layout.isTablet) 90.dp else 70.dp)
                                        .fillMaxWidth(0.9f)
                                        .then(copyTitleGesture),
                                    contentScale = ContentScale.Fit,
                                    onError = {
                                        logoFailed = true
                                    }
                                )
                            } else {
                                Text(
                                    text = displayTitle,
                                    color = KitsugiColors.TextPrimary,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = if (layout.isTablet) TextAlign.Start else TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(copyTitleGesture)
                                )
                            }
                        } else {
                            Text(
                                text = displayTitle,
                                color = KitsugiColors.TextPrimary,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = if (layout.isTablet) TextAlign.Start else TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(copyTitleGesture)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.subtitle,
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = if (layout.isTablet) TextAlign.Start else TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = buildHeroMeta(item, scoreFormat, hideScores),
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = if (layout.isTablet) TextAlign.Start else TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (!layout.isTablet) {
                Spacer(modifier = Modifier.height(14.dp))
                val currentItem = visiblePages
                    .lastOrNull()
                    ?.page
                    ?.let(items::get)
                    ?: items[currentPage]
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(40.dp))
                        .background(KitsugiColors.TextPrimary)
                        .tvClickable(
                            shape = RoundedCornerShape(40.dp)
                        ) { onInfoClick(currentItem) }
                        .padding(horizontal = 28.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Detayları Gör",
                        color = KitsugiColors.Background,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (items.size > 1) {
                Spacer(modifier = Modifier.height(if (layout.isTablet) 14.dp else 12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, _ ->
                        val activeFraction = heroPageVisibility(pagerState, index)
                        Box(
                            modifier = Modifier
                                .then(
                                    if (isTv) {
                                        Modifier
                                    } else {
                                        Modifier.tvClickable(
                                            shape = CircleShape
                                        ) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                    }
                                )
                                .clip(CircleShape)
                                .background(if (isTv && isFocused && activeFraction > 0.5f) accentColor else KitsugiColors.TextPrimary)
                                .graphicsLayer {
                                    alpha = 0.35f + (0.57f * activeFraction)
                                }
                                .width(8.dp + (24.dp * activeFraction))
                                .height(8.dp)
                        )
                    }
                }
            }
        }
    }
}