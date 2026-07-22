package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.KitsugiStreamingEpisode
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.utils.tvClickable

// ---------------------------------------------------------------------------
//  Bölümler (Episodes) Tab  –  Accordion Sezon Görünümü
// ---------------------------------------------------------------------------

@Composable
fun EpisodesTabContent(
    state: DetailTabState<List<KitsugiStreamingEpisode>>,
    onEpisodeClick: (episode: KitsugiStreamingEpisode) -> Unit,
    episodeRatings: Map<Pair<Int, Int>, Double> = emptyMap(),
    targetSeason: Int? = null,
    totalSeasons: Int? = null,
    onSeasonSelected: ((Int) -> Unit)? = null,
    onRatingClick: ((season: Int, episode: Int) -> Unit)? = null
) {
    val accentColor = LocalKitsugiAccent.current

    when (state) {
        is DetailTabState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        }
        is DetailTabState.Success -> {
            val episodes = state.data

            // Eğer birden fazla sezon varsa → Accordion görünüm
            if (totalSeasons != null && totalSeasons > 1) {
                SeasonAccordion(
                    episodes = episodes,
                    totalSeasons = totalSeasons,
                    targetSeason = targetSeason,
                    episodeRatings = episodeRatings,
                    onSeasonSelected = onSeasonSelected,
                    onEpisodeClick = onEpisodeClick,
                    onRatingClick = onRatingClick,
                    accentColor = accentColor
                )
            } else {
                // Tek sezon → Düz liste
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (episodes.isEmpty()) {
                        EmptyEpisodesMessage()
                    } else {
                        episodes.forEachIndexed { index, episode ->
                            val rating = resolveRating(episode, targetSeason, episodeRatings)
                            EpisodeRow(
                                episode = episode,
                                index = index,
                                accentColor = accentColor,
                                imdbRating = rating,
                                onClick = { onEpisodeClick(episode) },
                                onRatingClick = if (onRatingClick != null && episode.episodeNumber != null) {
                                    { onRatingClick(targetSeason ?: episode.seasonNumber ?: 1, episode.episodeNumber) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
        is DetailTabState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Bölümler yüklenirken bir hata oluştu.",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        else -> Unit
    }
}

// ---------------------------------------------------------------------------
//  Accordion: Sezon başlıkları + bölümler
// ---------------------------------------------------------------------------

@Composable
private fun SeasonAccordion(
    episodes: List<KitsugiStreamingEpisode>,
    totalSeasons: Int,
    targetSeason: Int?,
    episodeRatings: Map<Pair<Int, Int>, Double>,
    onSeasonSelected: ((Int) -> Unit)?,
    onEpisodeClick: (KitsugiStreamingEpisode) -> Unit,
    onRatingClick: ((season: Int, episode: Int) -> Unit)?,
    accentColor: Color
) {
    // Hangi sezon açık? Başlangıçta aktif sezon (targetSeason) açık gelsin
    var expandedSeason by remember(targetSeason) {
        mutableStateOf(targetSeason ?: 1)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (season in 1..totalSeasons) {
            val isExpanded = expandedSeason == season

            SeasonAccordionItem(
                seasonNumber = season,
                isExpanded = isExpanded,
                episodes = episodes.filter {
                    it.seasonNumber == season || (season == (targetSeason ?: 1) && it.seasonNumber == null)
                },
                episodeRatings = episodeRatings,
                targetSeason = season,
                accentColor = accentColor,
                onHeaderClick = {
                    // Aynı sezona basılırsa aç/kapat; farklı sezon basılırsa sadece onu aç
                    if (isExpanded) {
                        // İstersen kapatmak yerine başka sezon seçilmesini bekle
                        // Şu an kapatma pasif (sadece yeni sezon seçince kapanır)
                        // Eğer tıkla-kapat istiyorsan: expandedSeason = -1
                        expandedSeason = -1
                    } else {
                        expandedSeason = season
                        onSeasonSelected?.invoke(season)
                    }
                },
                onEpisodeClick = onEpisodeClick,
                onRatingClick = onRatingClick
            )
        }
    }
}

@Composable
private fun SeasonAccordionItem(
    seasonNumber: Int,
    isExpanded: Boolean,
    episodes: List<KitsugiStreamingEpisode>,
    episodeRatings: Map<Pair<Int, Int>, Double>,
    targetSeason: Int,
    accentColor: Color,
    onHeaderClick: () -> Unit,
    onEpisodeClick: (KitsugiStreamingEpisode) -> Unit,
    onRatingClick: ((season: Int, episode: Int) -> Unit)?
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "arrowRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isExpanded) accentColor.copy(alpha = 0.08f) else KitsugiColors.Surface)
            .border(
                width = if (isExpanded) 1.5.dp else 0.5.dp,
                color = if (isExpanded) accentColor.copy(alpha = 0.5f) else KitsugiColors.SurfaceStrong,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // ── Sezon Başlığı (tıklanabilir) ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tvClickable(shape = RoundedCornerShape(16.dp), onClick = onHeaderClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sezon numarası rozeti
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isExpanded) accentColor else KitsugiColors.SurfaceStrong)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$seasonNumber",
                        color = if (isExpanded) Color.White else KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black
                    )
                }

                Column {
                    Text(
                        text = "Sezon $seasonNumber",
                        color = if (isExpanded) accentColor else KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (episodes.isNotEmpty()) {
                        Text(
                            text = "${episodes.size} bölüm",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Kapat" else "Aç",
                tint = if (isExpanded) accentColor else KitsugiColors.TextSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(arrowRotation)
            )
        }

        // ── Bölümler (Animated Expand/Collapse) ─────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(200))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (episodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bu sezon için bölüm bilgisi bulunamadı",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    episodes.forEachIndexed { index, episode ->
                        val rating = resolveRating(episode, targetSeason, episodeRatings)
                        EpisodeRow(
                            episode = episode,
                            index = index,
                            accentColor = accentColor,
                            imdbRating = rating,
                            onClick = { onEpisodeClick(episode) },
                            onRatingClick = if (onRatingClick != null && episode.episodeNumber != null) {
                                { onRatingClick(targetSeason, episode.episodeNumber) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Yardımcı: Boş Liste Mesajı
// ---------------------------------------------------------------------------

@Composable
private fun EmptyEpisodesMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Bölüm bilgisi bulunamadı",
            color = KitsugiColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Bu kaynak bölüm listesi sağlamıyor olabilir",
            color = KitsugiColors.TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// ---------------------------------------------------------------------------
//  Yardımcı: Rating çözücü
// ---------------------------------------------------------------------------

private fun resolveRating(
    episode: KitsugiStreamingEpisode,
    targetSeason: Int?,
    episodeRatings: Map<Pair<Int, Int>, Double>
): Double? {
    val epNum = episode.episodeNumber ?: return null
    val sNum = targetSeason ?: episode.seasonNumber ?: 1

    // 1. Doğrudan sezon numarası ve bölüm numarası eşleşmesi
    val direct = episodeRatings[sNum to epNum]
        ?: episodeRatings[1 to epNum]
    if (direct != null) return direct

    // 2. Herhangi bir sezonda aynı bölüm numarasının doğrudan bulunması
    val simpleMatch = episodeRatings.keys.firstOrNull { it.second == epNum }?.let { episodeRatings[it] }
    if (simpleMatch != null) return simpleMatch

    // 3. Mutlak bölüm numarası (Absolute Episode Number) → Sezon İçi Bölüm Hesaplama
    // (Örn: MHA 26. bölüm → 2. Sezon 13. Bölüm gibi birikimli sezon aralıklarıyla eşleştirme)
    if (episodeRatings.isNotEmpty() && epNum > 0) {
        val seasonMaxEpisodes = episodeRatings.keys
            .filter { it.first >= 1 }
            .groupBy { it.first }
            .mapValues { entry -> entry.value.maxOf { it.second } }
            .toSortedMap()

        if (seasonMaxEpisodes.isNotEmpty()) {
            var cumulative = 0
            for ((season, maxEpInSeason) in seasonMaxEpisodes) {
                if (epNum <= cumulative + maxEpInSeason) {
                    val inSeasonEp = epNum - cumulative
                    val mappedRating = episodeRatings[season to inSeasonEp]
                    if (mappedRating != null) return mappedRating
                    break
                }
                cumulative += maxEpInSeason
            }
        }
    }

    return null
}

// ---------------------------------------------------------------------------
//  Episode Row
// ---------------------------------------------------------------------------

@Composable
private fun EpisodeRow(
    episode: KitsugiStreamingEpisode,
    index: Int,
    accentColor: androidx.compose.ui.graphics.Color,
    imdbRating: Double? = null,
    onClick: () -> Unit,
    onRatingClick: (() -> Unit)? = null
) {
    val hasThumbnail = !episode.thumbnail.isNullOrBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KitsugiColors.SurfaceStrong)
            .tvClickable(shape = RoundedCornerShape(14.dp), onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail veya bölüm numarası
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KitsugiColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            if (hasThumbnail) {
                AsyncImage(
                    model = episode.thumbnail,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KitsugiColors.Background.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = "Oynat",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = accentColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Başlık + site + IMDb rozeti
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = episode.title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!episode.site.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = episode.site,
                            color = accentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (imdbRating != null && imdbRating > 0.0) {
                    ImdbEpisodeRatingBadge(rating = imdbRating, onClick = onRatingClick)
                }
            }
        }

        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = "Oynat",
            tint = accentColor.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
        )
    }
}

// ---------------------------------------------------------------------------
//  IMDb Rozeti
// ---------------------------------------------------------------------------

@Composable
private fun ImdbEpisodeRatingBadge(
    rating: Double,
    onClick: (() -> Unit)? = null
) {
    val (backgroundColor, textColor) = when {
        rating >= 9.5 -> Color(0xFF2B83FA) to Color.White
        rating >= 9.0 -> Color(0xFF00C853) to Color.White
        rating >= 8.0 -> Color(0xFF2E7D32) to Color.White
        rating >= 7.0 -> Color(0xFFFBC02D) to Color.Black
        rating >= 6.0 -> Color(0xFFF57C00) to Color.White
        rating >= 5.0 -> Color(0xFFD32F2F) to Color.White
        else          -> Color(0xFF7B1FA2) to Color.White
    }

    Row(
        modifier = if (onClick != null) {
            Modifier.clip(RoundedCornerShape(4.dp)).tvClickable(shape = RoundedCornerShape(4.dp), onClick = onClick)
        } else {
            Modifier.clip(RoundedCornerShape(4.dp))
        },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF5C518))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "IMDb",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                letterSpacing = 0.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(backgroundColor)
                .padding(horizontal = 5.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "%.1f".format(rating),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
