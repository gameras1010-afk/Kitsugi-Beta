package com.kitsugi.animelist.ui.screens.explore

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.remote.JikanSearchResult
import kotlinx.coroutines.delay
import com.kitsugi.animelist.ui.components.KitsugiNsfwImage
import com.kitsugi.animelist.ui.components.KitsugiShimmerBlock
import com.kitsugi.animelist.ui.components.LocalShimmerBrush
import com.kitsugi.animelist.ui.components.rememberKitsugiShimmerBrush
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.automirrored.rounded.ArrowForward


@Composable
fun ExploreCategoryChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KitsugiColors.Accent.copy(alpha = 0.08f))
            .border(1.dp, KitsugiColors.Accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = KitsugiColors.Accent,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
fun AiringSoonCountdownText(
    nextAiringEpisode: String?,
    modifier: Modifier = Modifier
) {
    if (nextAiringEpisode.isNullOrBlank()) return

    val parts = remember(nextAiringEpisode) { nextAiringEpisode.split("|") }
    val episode = remember(parts) { parts.getOrNull(0)?.toIntOrNull() } ?: return
    val targetEpoch = remember(parts) { parts.getOrNull(1)?.toLongOrNull() } ?: return

    // episode == -1: TMDB "upcoming" listesinden gelen film/dizi (nextAiringEpisode = "-1|epoch")
    val isTmdbUpcoming = episode == -1

    var countdownText by remember(episode, targetEpoch) { mutableStateOf("") }

    LaunchedEffect(episode, targetEpoch) {
        while (true) {
            val now = System.currentTimeMillis() / 1000L
            val remaining = targetEpoch - now
            countdownText = when {
                remaining <= 0L -> {
                    // Zaten yayınlandı — TMDB upcoming listeleri için boş bırak
                    ""
                }
                remaining < 3600L -> {
                    val mins = (remaining / 60).toInt()
                    if (isTmdbUpcoming) "${mins} dk sonra çıkıyor"
                    else "${mins} dk sonra yayınlanacak"
                }
                remaining < 86400L -> {
                    val hours = (remaining / 3600).toInt()
                    if (isTmdbUpcoming) "${hours} saat sonra çıkıyor"
                    else "${hours} saat sonra yayınlanacak"
                }
                else -> {
                    val days = (remaining / 86400).toInt()
                    if (days > 6) {
                        val weeks = days / 7
                        if (weeks > 4) {
                            val months = days / 30
                            if (isTmdbUpcoming) "$months ay sonra çıkıyor" else "$months ay sonra yayınlanacak"
                        } else {
                            if (isTmdbUpcoming) "$weeks hafta sonra çıkıyor" else "$weeks hafta sonra yayınlanacak"
                        }
                    } else {
                        if (isTmdbUpcoming) "$days gün sonra çıkıyor" else "$days gün sonra yayınlanacak"
                    }
                }
            }
            if (remaining <= 0L) break
            val delayMs = if (remaining < 3600L) 1_000L
                          else if (remaining < 86400L) 10_000L
                          else 60_000L
            delay(delayMs)
        }
    }

    if (countdownText.isNotBlank()) {
        // episode > 0: AniList countdown → turuncu; TMDB upcoming → mavi/accent
        val textColor = if (isTmdbUpcoming) KitsugiColors.Accent else KitsugiColors.AccentOrange
        Text(
            text = countdownText,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }
}


@Composable
fun AiringSoonHorizontalCard(
    result: JikanSearchResult,
    alreadyInList: Boolean,
    onItemClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    titleLanguage: String = "ROMAJI",
    blurAdultMedia: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayTitle = when (titleLanguage) {
        "ENGLISH" -> result.titleEnglish ?: result.title
        else      -> result.title
    }

    val displayScore = when {
        result.rawScoreDouble != null -> "★ ${result.rawScoreDouble}"
        result.score != null -> {
            if (result.score > 10) "★ ${result.score}%" else "★ ${result.score}"
        }
        else -> null
    }

    Row(
        modifier = modifier
            .width(280.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(KitsugiColors.Surface)
            .then(
                if (alreadyInList)
                    Modifier.border(1.dp, KitsugiColors.AccentGreen.copy(0.45f), RoundedCornerShape(12.dp))
                else Modifier
            )
            .tvClickable(
                shape = RoundedCornerShape(12.dp),
                onLongClick = onLongClick,
                onClick = onItemClick
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(KitsugiColors.SurfaceSoft),
            contentAlignment = Alignment.Center
        ) {
            if (!result.imageUrl.isNullOrBlank()) {
                KitsugiNsfwImage(
                    model = result.imageUrl,
                    contentDescription = displayTitle,
                    isAdult = result.isAdult,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = displayTitle.take(2).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = KitsugiColors.Accent
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Sağ Taraf: Detaylar
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = KitsugiColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            AiringSoonCountdownText(nextAiringEpisode = result.nextAiringEpisode)

            if (displayScore != null) {
                Text(
                    text = displayScore,
                    style = MaterialTheme.typography.labelSmall,
                    color = KitsugiColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }

        // İzliyorum ikonu overlay
        if (alreadyInList) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Rounded.PlayCircle,
                contentDescription = "İzliyorum",
                tint = KitsugiColors.AccentGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AiringSoonHorizontalCardPlaceholder(modifier: Modifier = Modifier) {
    val brush = LocalShimmerBrush.current ?: rememberKitsugiShimmerBrush()
    Row(
        modifier = modifier
            .width(280.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(KitsugiColors.Surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KitsugiShimmerBlock(
            brush = brush,
            modifier = Modifier
                .width(60.dp)
                .fillMaxHeight(),
            cornerRadius = 8.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KitsugiShimmerBlock(
                brush = brush,
                modifier = Modifier.fillMaxWidth(0.9f).height(14.dp),
                cornerRadius = 4.dp
            )
            KitsugiShimmerBlock(
                brush = brush,
                modifier = Modifier.fillMaxWidth(0.6f).height(10.dp),
                cornerRadius = 4.dp
            )
            KitsugiShimmerBlock(
                brush = brush,
                modifier = Modifier.fillMaxWidth(0.4f).height(10.dp),
                cornerRadius = 4.dp
            )
        }
    }
}

@Composable
fun ExploreAiringSoonSection(
    title: String,
    airingSoonAnime: List<JikanSearchResult>,
    isLoading: Boolean,
    alreadyInList: (JikanSearchResult) -> Boolean,
    onItemClick: (JikanSearchResult) -> Unit,
    onLongClickItem: (JikanSearchResult) -> Unit,
    onOpenAiringCalendar: () -> Unit,
    accentColor: Color,
    titleLanguage: String = "ROMAJI",
    blurAdultMedia: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onOpenAiringCalendar) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Yayın Takvimi",
                    tint = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val lazyListState = remember { androidx.compose.foundation.lazy.LazyListState() }
        LazyRow(
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                lazyListState = lazyListState,
                snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Start
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (airingSoonAnime.isEmpty() && isLoading) {
                // Yükleniyor: iskelet placeholder kartlar
                items(6) {
                    AiringSoonHorizontalCardPlaceholder()
                }
            } else {
                items(airingSoonAnime.size) { index ->
                    val result = airingSoonAnime[index]
                    AiringSoonHorizontalCard(
                        result = result,
                        alreadyInList = alreadyInList(result),
                        onItemClick = { onItemClick(result) },
                        onLongClick = { onLongClickItem(result) },
                        titleLanguage = titleLanguage,
                        blurAdultMedia = blurAdultMedia
                    )
                }
            }
        }
    }
}
