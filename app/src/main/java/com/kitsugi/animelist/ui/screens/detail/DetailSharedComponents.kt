package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.KitsugiColors
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SearchOff

/**
 * Shared pill/chip composable used across all detail pages within this package.
 * Consolidates the previously duplicated private definitions in StaffDetailComponents,
 * CharacterDetailComponents, KitsugiDetailHeroSection, and StudioDetailPage.
 */
@Composable
internal fun DetailPill(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black
        )
    }
}

/**
 * Shared info-row composable (label + value) used across detail pages.
 */
@Composable
internal fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = KitsugiColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

internal data class AiringInfo(val episode: Int?, val targetEpoch: Long?, val rawText: String?)

internal fun parseNextAiring(raw: String): AiringInfo {
    if (raw.contains("|")) {
        val parts = raw.split("|")
        val ep = parts.getOrNull(0)?.toIntOrNull()
        val epoch = parts.getOrNull(1)?.toLongOrNull()
        if (ep != null && epoch != null) {
            return AiringInfo(ep, epoch, null)
        }
    }
    return AiringInfo(null, null, raw)
}

@Composable
internal fun rememberAiringCountdownText(nextAiring: String?): String {
    if (nextAiring.isNullOrBlank()) return ""
    val info = remember(nextAiring) { parseNextAiring(nextAiring) }
    var displayText by remember(info) { mutableStateOf("") }

    if (info.targetEpoch != null && info.episode != null) {
        val targetEpoch = info.targetEpoch
        val episode = info.episode

        LaunchedEffect(targetEpoch, episode) {
            while (true) {
                val now = System.currentTimeMillis() / 1000L
                val remaining = targetEpoch - now
                if (remaining <= 0) {
                    displayText = "Bölüm $episode yayınlandı!"
                    break
                }

                val days = remaining / 86400
                displayText = if (days >= 1) {
                    "Bölüm $episode, $days gün sonra yayında"
                } else {
                    val hours = remaining / 3600
                    val minutes = (remaining % 3600) / 60
                    String.format("Bölüm %d, %02d:%02d sonra yayınlanacak", episode, hours, minutes)
                }
                val delayTime = if (days >= 1) 60000L else 10000L
                kotlinx.coroutines.delay(delayTime)
            }
        }
    } else {
        displayText = info.rawText ?: ""
    }
    return displayText
}

@Composable
internal fun AiringCountdownCard(
    nextAiring: String,
    modifier: Modifier = Modifier
) {
    val info = remember(nextAiring) { parseNextAiring(nextAiring) }
    var displayText by remember(info) { mutableStateOf("") }

    if (info.targetEpoch != null && info.episode != null) {
        val targetEpoch = info.targetEpoch
        val episode = info.episode

        LaunchedEffect(targetEpoch, episode) {
            while (true) {
                val now = System.currentTimeMillis() / 1000L
                val remaining = targetEpoch - now
                if (remaining <= 0) {
                    displayText = "Bölüm $episode yayınlandı!"
                    break
                }

                val days = remaining / 86400
                displayText = if (days >= 1) {
                    "Bölüm $episode, $days gün sonra yayında"
                } else {
                    val hours = remaining / 3600
                    val minutes = (remaining % 3600) / 60
                    String.format("Bölüm %d, %02d:%02d sonra yayınlanacak", episode, hours, minutes)
                }
                val delayTime = if (days >= 1) 60000L else 10000L
                kotlinx.coroutines.delay(delayTime)
            }
        }
    } else {
        displayText = info.rawText ?: ""
    }

    if (displayText.isBlank()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .border(1.dp, KitsugiColors.Accent.copy(0.15f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KitsugiColors.Accent.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = KitsugiColors.Accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = "Yaklaşan Yayın",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = KitsugiColors.Accent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = KitsugiColors.TextPrimary
                )
            }
        }
    }
}

@Composable
internal fun DataUnavailableScreen(
    title: String,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
            .padding(24.dp)
    ) {
        // Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Geri",
                tint = KitsugiColors.TextPrimary
            )
        }

        // Center Warning Panel
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Styled Warning Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(KitsugiColors.AccentRed.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SearchOff,
                    contentDescription = null,
                    tint = KitsugiColors.AccentRed,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = KitsugiColors.TextPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Text(
                text = "Medya detay bilgisi şu anda yüklenemedi. Lütfen internet bağlantınızı kontrol edip tekrar deneyin veya daha sonra tekrar deneyin.",
                style = MaterialTheme.typography.bodyMedium,
                color = KitsugiColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Premium Kitsugi Button
            Button(
                onClick = onRetryClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KitsugiColors.Accent,
                    contentColor = KitsugiColors.Background
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Tekrar Dene",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
