package com.kitsugi.animelist.ui.screens.stream

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.ui.theme.KitsugiColors

/**
 * A single stream card showing quality/cache/addon badges and triggering playback on tap.
 */
@Composable
fun StreamCard(
    source: StreamSource,
    accentColor: Color,
    onClick: () -> Unit
) {
    val (quality, size) = remember(source) { parseStreamQuality(source) }
    val langType = remember(source) { detectStreamLang(source) }
    val cacheState = remember(source) { getCacheState(source) }

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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Badge row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    val qualityColor = when {
                        quality.contains("4K", ignoreCase = true) || quality.contains("2160", ignoreCase = true) -> KitsugiColors.AccentRed
                        quality.contains("1080", ignoreCase = true) -> KitsugiColors.AccentBlue
                        quality.contains("720", ignoreCase = true)  -> KitsugiColors.AccentGreen
                        else -> KitsugiColors.AccentOrange
                    }
                    StreamBadge(
                        text = quality,
                        color = qualityColor,
                        bgAlpha = 0.15f,
                        bgColor = qualityColor
                    )

                    // Sub / Dub Badge
                    val (langText, langColor) = when (langType) {
                        StreamLangType.DUB  -> "🎙️ Dublaj" to KitsugiColors.AccentOrange
                        StreamLangType.SUB  -> "💬 Altyazılı" to KitsugiColors.AccentBlue
                        StreamLangType.DUAL -> "🌐 Dual" to KitsugiColors.AccentPurple
                        StreamLangType.UNKNOWN -> "🎬 Standart" to KitsugiColors.TextSecondary
                    }
                    StreamBadge(
                        text = langText,
                        color = langColor,
                        bgAlpha = 0.15f,
                        bgColor = langColor
                    )

                    StreamBadge(
                        text = source.addonName,
                        color = KitsugiColors.AccentPurple,
                        bgAlpha = 0.10f,
                        bgColor = KitsugiColors.AccentPurple
                    )
                    val (cacheText, cacheColor) = when (cacheState) {
                        DebridCacheState.CACHED     -> "Önbellekte" to KitsugiColors.AccentGreen
                        DebridCacheState.NOT_CACHED -> "İndirilecek" to KitsugiColors.AccentOrange
                        DebridCacheState.P2P        -> "Torrent (P2P)" to KitsugiColors.AccentBlue
                    }
                    StreamBadge(text = cacheText, color = cacheColor, bgAlpha = 0.15f, bgColor = cacheColor)
                    if (size.isNotBlank()) {
                        Text(text = size, color = KitsugiColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Text(
                    text = source.name,
                    color = KitsugiColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (source.title.isNotBlank() && source.title != source.name) {
                    Text(
                        text = source.title.trim(),
                        color = KitsugiColors.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

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
