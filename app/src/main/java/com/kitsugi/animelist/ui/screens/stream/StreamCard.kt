package com.kitsugi.animelist.ui.screens.stream

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Movie
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.ui.theme.KitsugiColors

/**
 * A single stream card showing quality/cache/addon badges and triggering playback on tap.
 *
 * Layout (left → right):
 *   [Thumbnail 72×72dp, only if thumbnailUrl exists]
 *   [Text column weight(1f) — badges row, source name, subtitle]
 *   [Download icon + Play circle button]
 *
 * The text column uses weight(1f) so it always fills the remaining space
 * correctly whether a thumbnail is present or not — no text overflow or
 * overlapping with the thumbnail or action buttons.
 */
@Composable
fun StreamCard(
    source: StreamSource,
    accentColor: Color,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val (quality, size) = remember(source) { parseStreamQuality(source) }
    val langType = remember(source) { detectStreamLang(source) }
    val cacheState = remember(source) { getCacheState(source) }

    // Shimmer for thumbnail placeholder
    val shimmerTransition = rememberInfiniteTransition(label = "thumbShimmer")
    val shimmerAlpha by shimmerTransition.animateFloat(
        initialValue = 0.15f,
        targetValue  = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thumbAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = KitsugiColors.SurfaceStrong.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Thumbnail (optional, 72×72dp) ────────────────────────────────
            if (!source.thumbnailUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KitsugiColors.Surface)
                ) {
                    AsyncImage(
                        model = source.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Subtle bottom gradient so badges/text overlay reads cleanly if needed
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                                )
                            )
                    )
                }
            }

            // ── Text column (weight=1f keeps it from pushing buttons off-screen) ──
            Column(modifier = Modifier.weight(1f)) {
                // Badge row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    val qualityColor = when {
                        quality.contains("4K", ignoreCase = true) || quality.contains("2160", ignoreCase = true) -> KitsugiColors.AccentRed
                        quality.contains("1080", ignoreCase = true) -> KitsugiColors.AccentBlue
                        quality.contains("720", ignoreCase = true)  -> KitsugiColors.AccentGreen
                        else -> KitsugiColors.AccentOrange
                    }
                    StreamBadge(text = quality, color = qualityColor, bgAlpha = 0.15f, bgColor = qualityColor)

                    val (langText, langColor) = when (langType) {
                        StreamLangType.DUB     -> "🎙️ Dublaj"     to KitsugiColors.AccentOrange
                        StreamLangType.SUB     -> "💬 Altyazılı"  to KitsugiColors.AccentBlue
                        StreamLangType.DUAL    -> "🌐 Dual"       to KitsugiColors.AccentPurple
                        StreamLangType.UNKNOWN -> "🎬 Standart"   to KitsugiColors.TextSecondary
                    }
                    StreamBadge(text = langText, color = langColor, bgAlpha = 0.15f, bgColor = langColor)

                    StreamBadge(
                        text = source.addonName,
                        color = KitsugiColors.AccentPurple,
                        bgAlpha = 0.10f,
                        bgColor = KitsugiColors.AccentPurple
                    )

                    val (cacheText, cacheColor) = when (cacheState) {
                        DebridCacheState.CACHED     -> "Önbellekte"    to KitsugiColors.AccentGreen
                        DebridCacheState.NOT_CACHED -> "İndirilecek"   to KitsugiColors.AccentOrange
                        DebridCacheState.P2P        -> "Torrent (P2P)" to KitsugiColors.AccentBlue
                    }
                    StreamBadge(text = cacheText, color = cacheColor, bgAlpha = 0.15f, bgColor = cacheColor)

                    if (size.isNotBlank()) {
                        Text(text = size, color = KitsugiColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Source name
                Text(
                    text = source.name,
                    color = KitsugiColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle / detail text
                if (source.title.isNotBlank() && source.title != source.name) {
                    Text(
                        text = source.title.trim(),
                        color = KitsugiColors.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }

            // ── Action buttons (fixed width, never pushed) ───────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "İndir",
                        tint = KitsugiColors.TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Oynat",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamBadge(text: String, color: Color, bgAlpha: Float, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor.copy(alpha = bgAlpha))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

