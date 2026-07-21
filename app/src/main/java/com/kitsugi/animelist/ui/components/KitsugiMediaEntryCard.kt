package com.kitsugi.animelist.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.kitsugi.animelist.ui.utils.tvClickable
import com.kitsugi.animelist.ui.theme.LocalIsTv
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.MediaType
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.utils.tvClickable

import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayTitle
import com.kitsugi.animelist.utils.PreferenceHelpers.getDisplayScore

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KitsugiMediaEntryCard(
    entry: MediaEntry,
    layoutId: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onIncrementClick: () -> Unit = {},
    titleLanguage: String = "ROMAJI",
    scoreFormat: String = "POINT_10",
    hideScores: Boolean = false,
    blurAdultMedia: Boolean = false,
    onPosterLongClick: ((String) -> Unit)? = null
) {
    when (layoutId) {
        "compact" -> CompactMediaEntryCard(
            entry = entry,
            modifier = modifier,
            onClick = onClick,
            onIncrementClick = onIncrementClick,
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia,
            onPosterLongClick = onPosterLongClick
        )

        "large" -> LargeMediaEntryCard(
            entry = entry,
            modifier = modifier,
            onClick = onClick,
            onIncrementClick = onIncrementClick,
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia,
            onPosterLongClick = onPosterLongClick
        )

        "minimalist" -> MinimalistMediaEntryCard(
            entry = entry,
            modifier = modifier,
            onClick = onClick,
            onIncrementClick = onIncrementClick,
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia,
            onPosterLongClick = onPosterLongClick
        )

        "grid_2col" -> PosterGridMediaEntryCard(
            entry = entry,
            modifier = modifier,
            onClick = onClick,
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia,
            onPosterLongClick = onPosterLongClick
        )

        else -> ComfortableMediaEntryCard(
            entry = entry,
            modifier = modifier,
            onClick = onClick,
            onIncrementClick = onIncrementClick,
            titleLanguage = titleLanguage,
            scoreFormat = scoreFormat,
            hideScores = hideScores,
            blurAdultMedia = blurAdultMedia,
            onPosterLongClick = onPosterLongClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactMediaEntryCard(
    entry: MediaEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onIncrementClick: () -> Unit,
    titleLanguage: String,
    scoreFormat: String,
    hideScores: Boolean,
    blurAdultMedia: Boolean,
    onPosterLongClick: ((String) -> Unit)?
) {
    val statusColor = statusColor(entry.status)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(20.dp), scaleFocused = 1.08f, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = KitsugiColors.Surface
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosterView(
                entry = entry,
                width = 50,
                height = 70,
                blurAdultMedia = blurAdultMedia,
                onPosterLongClick = onPosterLongClick,
                onPosterClick = onClick
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mediaTypeLabel(entry),
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (entry.year != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = entry.year.toString(),
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (entry.source != "manual") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = entry.source.uppercase(),
                            color = LocalKitsugiAccent.current,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (entry.isFavorite) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "★",
                            color = LocalKitsugiAccent.current,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = entry.getDisplayTitle(titleLanguage),
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(statusColor.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = entry.status.label,
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = entryProgressText(entry),
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (!hideScores) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ScorePill(
                            score = entry.getDisplayScore(scoreFormat, hideScores),
                            scoreFormat = scoreFormat
                        )
                    }
                }
            }

            val isTv = LocalIsTv.current
            if (!isTv) {
                Spacer(modifier = Modifier.width(10.dp))
                QuickIncrementButton(
                    onClick = onIncrementClick
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComfortableMediaEntryCard(
    entry: MediaEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onIncrementClick: () -> Unit,
    titleLanguage: String,
    scoreFormat: String,
    hideScores: Boolean,
    blurAdultMedia: Boolean,
    onPosterLongClick: ((String) -> Unit)?
) {
    val statusColor = statusColor(entry.status)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(24.dp), scaleFocused = 1.08f, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = KitsugiColors.Surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosterView(
                entry = entry,
                width = 72,
                height = 112,
                blurAdultMedia = blurAdultMedia,
                onPosterLongClick = onPosterLongClick,
                onPosterClick = onClick
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mediaTypeLabel(entry),
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (entry.source != "manual") {
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = entry.source.uppercase(),
                            color = LocalKitsugiAccent.current,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (entry.year != null) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = entry.year.toString(),
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (entry.isFavorite) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "★ Favori",
                            color = LocalKitsugiAccent.current,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = entry.getDisplayTitle(titleLanguage),
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = entry.subtitle,
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!entry.synopsis.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = entry.synopsis,
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                EpisodeProgressBar(
                    progress = entry.progress,
                    total = entry.total,
                    color = statusColor,
                    modifier = Modifier.fillMaxWidth(0.95f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        text = entry.status.label,
                        color = statusColor
                    )

                    Text(
                        text = entryProgressText(entry),
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (!hideScores) {
                        ScorePill(
                            score = entry.getDisplayScore(scoreFormat, hideScores),
                            scoreFormat = scoreFormat
                        )
                    }
                }
            }

            val isTv = LocalIsTv.current
            if (!isTv) {
                Spacer(modifier = Modifier.width(10.dp))
                QuickIncrementButton(
                    onClick = onIncrementClick
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LargeMediaEntryCard(
    entry: MediaEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onIncrementClick: () -> Unit,
    titleLanguage: String,
    scoreFormat: String,
    hideScores: Boolean,
    blurAdultMedia: Boolean,
    onPosterLongClick: ((String) -> Unit)?
) {
    val statusColor = statusColor(entry.status)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Card(
        modifier = modifier
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(28.dp), scaleFocused = 1.08f, onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = KitsugiColors.Surface
        )
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol tarafta poster görseli
                Box(
                    modifier = Modifier
                        .size(width = 140.dp, height = 210.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(statusColor.copy(alpha = 0.24f))
                ) {
                    val imageUrl = entry.imageUrl
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = entry.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (blurAdultMedia && entry.isAdult) Modifier.blur(24.dp)
                                    else Modifier
                                ),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = entry.title.take(2).uppercase(),
                            color = statusColor,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    StatusPill(
                        text = entry.status.label,
                        color = statusColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                // Sağ tarafta bilgiler
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mediaTypeLabel(entry),
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (entry.source != "manual") {
                            Text(
                                text = entry.source.uppercase(),
                                color = LocalKitsugiAccent.current,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (entry.year != null) {
                            Text(
                                text = entry.year.toString(),
                                color = KitsugiColors.TextMuted,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (entry.isFavorite) {
                            Text(
                                text = "★ Favori",
                                color = LocalKitsugiAccent.current,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = entry.getDisplayTitle(titleLanguage),
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = entry.subtitle,
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!entry.synopsis.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = entry.synopsis,
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = entryProgressText(entry),
                                color = KitsugiColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (!hideScores) {
                                ScorePill(
                                    score = entry.getDisplayScore(scoreFormat, hideScores),
                                    scoreFormat = scoreFormat
                                )
                            }
                        }

                        val isTv = LocalIsTv.current
                        if (!isTv) {
                            QuickIncrementButton(
                                onClick = onIncrementClick
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                LargePosterView(
                    entry = entry,
                    statusColor = statusColor,
                    blurAdultMedia = blurAdultMedia,
                    onPosterLongClick = onPosterLongClick,
                    onPosterClick = onClick
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = mediaTypeLabel(entry),
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (entry.source != "manual") {
                        Text(
                            text = entry.source.uppercase(),
                            color = LocalKitsugiAccent.current,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (entry.year != null) {
                        Text(
                            text = entry.year.toString(),
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (entry.isFavorite) {
                        Text(
                            text = "★ Favori",
                            color = LocalKitsugiAccent.current,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = entry.getDisplayTitle(titleLanguage),
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = entry.subtitle,
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!entry.synopsis.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = entry.synopsis,
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = entryProgressText(entry),
                            color = KitsugiColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (!hideScores) {
                            ScorePill(
                                score = entry.getDisplayScore(scoreFormat, hideScores),
                                scoreFormat = scoreFormat
                            )
                        }
                    }

                    val isTv = LocalIsTv.current
                    if (!isTv) {
                        QuickIncrementButton(
                            onClick = onIncrementClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickIncrementButton(
    onClick: () -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(
                width = 1.5.dp,
                color = accentColor.copy(alpha = 0.55f),
                shape = RoundedCornerShape(999.dp)
            )
            .tvClickable(shape = RoundedCornerShape(999.dp), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "+1 Ekle",
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PosterView(
    entry: MediaEntry,
    width: Int,
    height: Int,
    blurAdultMedia: Boolean = false,
    onPosterLongClick: ((String) -> Unit)? = null,
    onPosterClick: (() -> Unit)? = null
) {
    val statusColor = statusColor(entry.status)
    val imageUrl = entry.imageUrl
    val isTv = LocalIsTv.current
    val posterModifier = if (isTv) {
        Modifier
    } else if (onPosterLongClick != null && !imageUrl.isNullOrBlank()) {
        Modifier.combinedClickable(
            onLongClick = { onPosterLongClick(imageUrl) },
            onClick = { onPosterClick?.invoke() }
        )
    } else if (onPosterClick != null) {
        Modifier.tvClickable(shape = RoundedCornerShape(14.dp)) { onPosterClick() }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .width(width.dp)
            .height(height.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(statusColor.copy(alpha = 0.22f))
            .then(posterModifier),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = entry.title,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (blurAdultMedia && entry.isAdult) Modifier.blur(24.dp)
                        else Modifier
                    ),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = entry.title.take(1).uppercase(),
                color = statusColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LargePosterView(
    entry: MediaEntry,
    statusColor: Color,
    blurAdultMedia: Boolean = false,
    onPosterLongClick: ((String) -> Unit)? = null,
    onPosterClick: (() -> Unit)? = null
) {
    val imageUrl = entry.imageUrl
    val isTv = LocalIsTv.current
    val posterModifier = if (isTv) {
        Modifier
    } else if (onPosterLongClick != null && !imageUrl.isNullOrBlank()) {
        Modifier.combinedClickable(
            onLongClick = { onPosterLongClick(imageUrl) },
            onClick = { onPosterClick?.invoke() }
        )
    } else if (onPosterClick != null) {
        Modifier.tvClickable(shape = RoundedCornerShape(22.dp)) { onPosterClick() }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(statusColor.copy(alpha = 0.24f))
            .then(posterModifier)
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = entry.title,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (blurAdultMedia && entry.isAdult) Modifier.blur(24.dp)
                        else Modifier
                    ),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = entry.title.take(2).uppercase(),
                color = statusColor,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        StatusPill(
            text = entry.status.label,
            color = statusColor,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
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

@Composable
private fun ScorePill(
    score: String,
    scoreFormat: String
) {
    val accentColor = LocalKitsugiAccent.current
    val label = when {
        score == "unrated" -> "unrated"
        scoreFormat == "POINT_5" || scoreFormat == "POINT_3" -> score
        else -> "★ $score"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accentColor.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = accentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

private fun statusColor(status: WatchStatus): Color {
    return when (status) {
        WatchStatus.Watching   -> KitsugiColors.AccentBlue
        WatchStatus.Completed  -> KitsugiColors.AccentGreen
        WatchStatus.Planned    -> KitsugiColors.AccentOrange
        WatchStatus.Dropped    -> KitsugiColors.AccentRed
        WatchStatus.Paused     -> KitsugiColors.AccentPurple
        WatchStatus.Repeating  -> KitsugiColors.AccentBlue   // Rewatching → mavi (aktif izleme gibi)
    }
}

private fun mediaTypeLabel(entry: MediaEntry): String {
    return when (entry.type) {
        MediaType.Anime -> "ANIME"
        MediaType.Manga -> "MANGA"
        MediaType.Movie -> "MOVIE"
        MediaType.TvShow -> "TV SHOW"
    }
}

private fun compactMetaText(entry: MediaEntry): String {
    val parts = buildList {
        add(entryProgressText(entry))
        if (entry.year != null) add(entry.year.toString())
        if (entry.source != "manual") add(entry.source.uppercase())
    }

    return parts.joinToString(" • ")
}

private fun entryProgressText(entry: MediaEntry): String {
    val unit = when (entry.type) {
        MediaType.Anime -> "bölüm"
        MediaType.Manga -> "chapter"
        else -> "bölüm"
    }

    val totalText = entry.total?.toString() ?: "?"
    return "${entry.progress}/$totalText $unit"
}

private fun scoreText(entry: MediaEntry): String {
    return if (entry.score == null) {
        "Puan yok"
    } else {
        "${entry.score}/10"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinimalistMediaEntryCard(
    entry: MediaEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onIncrementClick: () -> Unit,
    titleLanguage: String,
    scoreFormat: String,
    hideScores: Boolean,
    blurAdultMedia: Boolean,
    onPosterLongClick: ((String) -> Unit)?
) {
    val statusColor = statusColor(entry.status)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(24.dp), scaleFocused = 1.08f, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = KitsugiColors.Surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosterView(
                entry = entry,
                width = 72,
                height = 112,
                blurAdultMedia = blurAdultMedia,
                onPosterLongClick = onPosterLongClick,
                onPosterClick = onClick
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mediaTypeLabel(entry),
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (entry.source != "manual") {
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = entry.source.uppercase(),
                            color = LocalKitsugiAccent.current,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (entry.year != null) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = entry.year.toString(),
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (entry.isFavorite) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "★ Favori",
                            color = LocalKitsugiAccent.current,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = entry.getDisplayTitle(titleLanguage),
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = entry.subtitle,
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        text = entry.status.label,
                        color = statusColor
                    )

                    Text(
                        text = entryProgressText(entry),
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (!hideScores) {
                        ScorePill(
                            score = entry.getDisplayScore(scoreFormat, hideScores),
                            scoreFormat = scoreFormat
                        )
                    }
                }
            }

            val isTv = LocalIsTv.current
            if (!isTv) {
                Spacer(modifier = Modifier.width(10.dp))
                QuickIncrementButton(
                    onClick = onIncrementClick
                )
            }
        }
    }
}

// ─── 2-Sütun Grid Poster Kartı ──────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PosterGridMediaEntryCard(
    entry: MediaEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    titleLanguage: String,
    scoreFormat: String,
    hideScores: Boolean,
    blurAdultMedia: Boolean,
    onPosterLongClick: ((String) -> Unit)?
) {
    val statusColor = statusColor(entry.status)
    val accentColor = LocalKitsugiAccent.current
    val imageUrl = entry.imageUrl
    val isTv = LocalIsTv.current

    val posterModifier = if (isTv) {
        Modifier
    } else if (onPosterLongClick != null && !imageUrl.isNullOrBlank()) {
        Modifier.combinedClickable(
            onLongClick = { onPosterLongClick(imageUrl) },
            onClick = onClick
        )
    } else {
        Modifier.tvClickable(shape = RoundedCornerShape(20.dp), onClick = onClick)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .tvClickable(shape = RoundedCornerShape(16.dp), scaleFocused = 1.05f, onClick = onClick)
    ) {
        // ─── Poster görseli ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(statusColor.copy(alpha = 0.22f))
                .then(posterModifier)
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = entry.getDisplayTitle(titleLanguage),
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (blurAdultMedia && entry.isAdult) Modifier.blur(24.dp)
                            else Modifier
                        ),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = entry.title.take(2).uppercase(),
                    color = statusColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Durum rozeti — sağ üst köşe
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(statusColor.copy(alpha = 0.88f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = entry.status.label,
                    color = KitsugiColors.Background,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }

            // Puan rozeti — sol alt köşe (gizlenmemişse)
            if (!hideScores) {
                val scoreText = entry.getDisplayScore(scoreFormat, hideScores)
                if (scoreText != "unrated") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(5.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(KitsugiColors.Background.copy(alpha = 0.80f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "★ $scoreText",
                            color = accentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            // Favori yıldızı — sol üst köşe
            if (entry.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(accentColor.copy(alpha = 0.88f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "★",
                        color = KitsugiColors.Background,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // ─── Başlık + İlerleme ──────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Text(
                text = entry.getDisplayTitle(titleLanguage),
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entryProgressText(entry),
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}