package com.kitsugi.animelist.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens

import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayScore
import com.kitsugi.animelist.utils.toFriendlySourceLabel

@Composable
fun KitsugiExploreMediaCard(
    result: JikanSearchResult,
    alreadyInList: Boolean = false,
    mediaEntry: com.kitsugi.animelist.model.MediaEntry? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    titleLanguage: String = "ROMAJI",
    scoreFormat: String = "POINT_10",
    hideScores: Boolean = false,
    blurAdultMedia: Boolean = false,
    forceVertical: Boolean = false
) {
    val accentColor = LocalKitsugiAccent.current
    val displayTitle = result.getDisplayTitle(titleLanguage)
    val isTv = LocalIsTv.current
    // TV'de her zaman dikey poster, isLandscape yoksayılır
    // Mobilde orijinal landscape/portrait mantığı korunur
    val isLandscape = !isTv && !forceVertical &&
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // TV: KitsugiTvTokens referanslı kompakt shape
    val cardShape = if (isTv) KitsugiTvTokens.Shapes.posterCard else RoundedCornerShape(24.dp)
    val focusScale = if (isTv) KitsugiTvTokens.Cards.focusedScale else 1.08f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .tvClickable(
                shape = cardShape,
                scaleFocused = focusScale,
                onLongClick = onLongClick,
                onClick = onClick
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = KitsugiColors.Surface
        )
    ) {
        // ── TV: Her zaman kompakt dikey poster (2:3) ─────────────────────────
        if (isTv) {
            Column {
                // Poster görseli — tam genişlik, sabit yükseklik
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KitsugiTvTokens.Cards.posterHeight)
                        .clip(KitsugiTvTokens.Shapes.posterCard)
                        .background(KitsugiColors.SurfaceSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (!result.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = result.imageUrl,
                            contentDescription = displayTitle,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (blurAdultMedia && result.isAdult) Modifier.blur(24.dp)
                                    else Modifier
                                ),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = displayTitle.take(2).uppercase(),
                            color = accentColor,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }

                    KitsugiSourceBadge(
                        source = result.source,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )

                    if (mediaEntry != null) {
                        StatusBadge(
                            status = mediaEntry.status,
                            showIconOnly = true,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                        )
                    }
                }

                // Başlık + meta bilgi
                Column(
                    modifier = Modifier.padding(
                        horizontal = KitsugiTvTokens.Spacing.sm,
                        vertical = KitsugiTvTokens.Spacing.sm
                    )
                ) {
                    Text(
                        text = displayTitle,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (mediaEntry != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = entryProgressText(mediaEntry),
                                color = statusColor(mediaEntry.status),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (mediaEntry.score != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "• ★${mediaEntry.score}",
                                    color = KitsugiColors.TextMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        EpisodeProgressBar(
                            progress = mediaEntry.progress,
                            total = mediaEntry.total,
                            color = statusColor(mediaEntry.status),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = buildMetaText(result, scoreFormat, hideScores),
                            color = if (alreadyInList) KitsugiColors.AccentGreen else accentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        // ── MOBİL LANDSCAPE ──────────────────────────────────────────────────
        } else if (isLandscape) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val screenWidthDp = LocalConfiguration.current.screenWidthDp
                val posterWidth = remember(screenWidthDp) {
                    when {
                        screenWidthDp >= 1200 -> 110.dp
                        screenWidthDp >= 800 -> 100.dp
                        else -> 90.dp
                    }
                }
                val posterHeight = remember(screenWidthDp) {
                    when {
                        screenWidthDp >= 1200 -> 160.dp
                        screenWidthDp >= 800 -> 145.dp
                        else -> 130.dp
                    }
                }
                Box(
                    modifier = Modifier
                        .size(width = posterWidth, height = posterHeight)
                        .clip(RoundedCornerShape(16.dp))
                        .background(KitsugiColors.SurfaceSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (!result.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = result.imageUrl,
                            contentDescription = displayTitle,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (blurAdultMedia && result.isAdult) Modifier.blur(24.dp)
                                    else Modifier
                                ),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = displayTitle.take(2).uppercase(),
                            color = accentColor,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }

                    KitsugiSourceBadge(
                        source = result.source,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )

                    if (mediaEntry != null) {
                        StatusBadge(
                            status = mediaEntry.status,
                            showIconOnly = true,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = displayTitle,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = result.subtitle,
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (mediaEntry != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entryProgressText(mediaEntry),
                                color = statusColor(mediaEntry.status),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (mediaEntry.score != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ★${mediaEntry.score}",
                                    color = KitsugiColors.TextMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        EpisodeProgressBar(
                            progress = mediaEntry.progress,
                            total = mediaEntry.total,
                            color = statusColor(mediaEntry.status),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = buildMetaText(result, scoreFormat, hideScores, includeTypeAndYear = false),
                            color = if (alreadyInList) {
                                KitsugiColors.AccentGreen
                            } else {
                                accentColor
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Yayın geri sayımı — sadece AniList kaynaklı, nextAiringEpisode dolu ise
                    if (!result.nextAiringEpisode.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(5.dp))
                        NextAiringChip(
                            nextAiringEpisode = result.nextAiringEpisode
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                val screenWidthDp = LocalConfiguration.current.screenWidthDp
                val posterHeight = remember(screenWidthDp) {
                    when {
                        screenWidthDp >= 1200 -> 240.dp
                        screenWidthDp >= 800 -> 225.dp
                        else -> 210.dp
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(posterHeight)
                        .clip(RoundedCornerShape(20.dp))
                        .background(KitsugiColors.SurfaceSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (!result.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = result.imageUrl,
                            contentDescription = displayTitle,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (blurAdultMedia && result.isAdult) Modifier.blur(24.dp)
                                    else Modifier
                                ),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = displayTitle.take(2).uppercase(),
                            color = accentColor,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black
                        )
                    }

                    KitsugiSourceBadge(
                        source = result.source,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )

                    if (mediaEntry != null) {
                        StatusBadge(
                            status = mediaEntry.status,
                            showIconOnly = false,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = displayTitle,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = result.subtitle,
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (mediaEntry != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entryProgressText(mediaEntry),
                            color = statusColor(mediaEntry.status),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (mediaEntry.score != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ★${mediaEntry.score}",
                                color = KitsugiColors.TextMuted,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    EpisodeProgressBar(
                        progress = mediaEntry.progress,
                        total = mediaEntry.total,
                        color = statusColor(mediaEntry.status),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = buildMetaText(result, scoreFormat, hideScores, includeTypeAndYear = false),
                        color = accentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Yayın geri sayımı — sadece AniList kaynaklı, nextAiringEpisode dolu ise
                if (!result.nextAiringEpisode.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    NextAiringChip(
                        nextAiringEpisode = result.nextAiringEpisode
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: com.kitsugi.animelist.model.WatchStatus,
    showIconOnly: Boolean,
    modifier: Modifier = Modifier
) {
    val color = statusColor(status)
    val icon = when (status) {
        com.kitsugi.animelist.model.WatchStatus.Watching   -> Icons.Rounded.PlayArrow
        com.kitsugi.animelist.model.WatchStatus.Completed  -> Icons.Rounded.Check
        com.kitsugi.animelist.model.WatchStatus.Planned    -> Icons.Rounded.Schedule
        com.kitsugi.animelist.model.WatchStatus.Dropped    -> Icons.Rounded.Close
        com.kitsugi.animelist.model.WatchStatus.Paused     -> Icons.Rounded.Pause
        com.kitsugi.animelist.model.WatchStatus.Repeating  -> Icons.Rounded.Repeat
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 10.dp))
            .background(color.copy(alpha = 0.92f))
            .padding(
                horizontal = if (showIconOnly) 5.dp else 6.dp,
                vertical = if (showIconOnly) 5.dp else 3.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showIconOnly) {
            Icon(
                imageVector = icon,
                contentDescription = status.label,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        } else {
            Text(
                text = status.label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun EpisodeProgressBar(
    progress: Int,
    total: Int?,
    color: Color = LocalKitsugiAccent.current,
    modifier: Modifier = Modifier
) {
    val fraction = remember(progress, total) {
        val maxVal = if (total != null && total > 0) total else if (progress > 0) progress else 1
        (progress.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(KitsugiColors.SurfaceSoft)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color)
        )
    }
}

private fun statusColor(status: com.kitsugi.animelist.model.WatchStatus): Color {
    return when (status) {
        com.kitsugi.animelist.model.WatchStatus.Watching   -> KitsugiColors.AccentBlue
        com.kitsugi.animelist.model.WatchStatus.Completed  -> KitsugiColors.AccentGreen
        com.kitsugi.animelist.model.WatchStatus.Planned    -> KitsugiColors.AccentOrange
        com.kitsugi.animelist.model.WatchStatus.Dropped    -> KitsugiColors.AccentRed
        com.kitsugi.animelist.model.WatchStatus.Paused     -> KitsugiColors.AccentPurple
        com.kitsugi.animelist.model.WatchStatus.Repeating  -> KitsugiColors.AccentBlue
    }
}

private fun entryProgressText(entry: com.kitsugi.animelist.model.MediaEntry): String {
    val unit = when (entry.type) {
        com.kitsugi.animelist.model.MediaType.Anime -> "bölüm"
        com.kitsugi.animelist.model.MediaType.Manga -> "chapter"
        else -> "bölüm"
    }

    val totalText = entry.total?.toString() ?: "?"
    return "${entry.progress}/$totalText $unit"
}

private fun buildMetaText(
    result: JikanSearchResult,
    scoreFormat: String,
    hideScores: Boolean,
    includeTypeAndYear: Boolean = true
): String {
    val parts = buildList {
        if (includeTypeAndYear) {
            val typeLabel = when (result.type) {
                MediaType.Anime -> "ANIME"
                MediaType.Manga -> "MANGA"
                MediaType.Movie -> "FİLM"
                MediaType.TvShow -> "DİZİ"
            }
            add(typeLabel)
            if (result.year != null) add(result.year.toString())
        }
        if (!hideScores) {
            val scoreStr = result.getDisplayScore(scoreFormat, hideScores)
            if (scoreStr.isNotEmpty() && scoreStr != "N/A" && scoreStr != "0" && scoreStr != "unrated") {
                val formattedScore = if (!scoreStr.contains("★") && !scoreStr.contains("☆") && !scoreStr.contains("😊") && !scoreStr.contains("😐") && !scoreStr.contains("🙁")) {
                    "★ $scoreStr"
                } else {
                    scoreStr
                }
                add(formattedScore)
            }
        }

        val sourceLabel = result.source.toFriendlySourceLabel()
        add("$sourceLabel #${result.malId}")
        if (result.isAdult) add("+18")
    }

    return parts.joinToString(" • ")
}