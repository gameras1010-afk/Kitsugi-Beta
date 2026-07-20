package com.kitsugi.animelist.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInBrowser
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.data.remote.KitsugiExternalLink
import com.kitsugi.animelist.data.remote.KitsugiStudio
import com.kitsugi.animelist.data.remote.KitsugiTag
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

// ---------------------------------------------------------------------------
//  Stüdyo / Yapımcı chips bölümü
// ---------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KitsugiStudiosCard(
    studios: List<KitsugiStudio>,
    producers: List<KitsugiStudio>,
    onStudioClick: (KitsugiStudio) -> Unit,
    onProducerClick: (KitsugiStudio) -> Unit
) {
    if (studios.isEmpty() && producers.isEmpty()) return
    val accentColor = LocalKitsugiAccent.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (studios.isNotEmpty()) {
            SectionLabel("Stüdyolar")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                studios.forEach { studio ->
                    KitsugiChip(
                        text = studio.name,
                        color = accentColor,
                        solid = true,
                        onClick = { onStudioClick(studio) }
                    )
                }
            }
        }
        if (producers.isNotEmpty()) {
            SectionLabel("Yapımcılar")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                producers.forEach { producer ->
                    KitsugiChip(
                        text = producer.name,
                        color = accentColor,
                        solid = false,
                        onClick = { onProducerClick(producer) }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Etiketler (Tags) - Spoiler toggle + daha fazla expander
// ---------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KitsugiTagsCard(
    tags: List<KitsugiTag>,
    onTagClick: (String) -> Unit
) {
    if (tags.isEmpty()) return
    val accentColor = LocalKitsugiAccent.current

    var showSpoilers by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val visibleTags = tags
        .filter { showSpoilers || !it.isSpoiler }
        .let { if (!expanded) it.take(10) else it }

    val hasSpoilers = tags.any { it.isSpoiler }
    val hasMore = tags.filter { showSpoilers || !it.isSpoiler }.size > 10

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("Etiketler")
            if (hasSpoilers) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .tvClickable(shape = RoundedCornerShape(12.dp)) { showSpoilers = !showSpoilers }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = KitsugiColors.TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (showSpoilers) "Spoiler'ı Gizle" else "Spoiler'ı Göster",
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visibleTags.forEach { tag ->
                TagChip(tag = tag, accentColor = accentColor, onClick = { onTagClick(tag.name) })
            }
        }

        if (hasMore) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .tvClickable(shape = RoundedCornerShape(12.dp)) { expanded = !expanded }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (expanded) "Daha az" else "Daha fazla",
                    color = accentColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TagChip(tag: KitsugiTag, accentColor: Color, onClick: (() -> Unit)? = null) {
    val chipColor = if (tag.isSpoiler) KitsugiColors.AccentRed else accentColor
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(chipColor.copy(alpha = 0.10f))
            .border(0.5.dp, chipColor.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
            .then(
                if (onClick != null) Modifier.tvClickable(shape = RoundedCornerShape(999.dp), onClick = onClick) else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (tag.rank != null) {
            Text(
                text = "${tag.rank}%",
                color = chipColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
        Text(
            text = tag.name,
            color = chipColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ---------------------------------------------------------------------------
//  Yayıncı Platformlar + Harici Bağlantılar
// ---------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KitsugiLinksCard(
    streamingLinks: List<KitsugiExternalLink>,
    externalLinks: List<KitsugiExternalLink>
) {
    if (streamingLinks.isEmpty() && externalLinks.isEmpty()) return
    val uriHandler = LocalUriHandler.current
    val accentColor = LocalKitsugiAccent.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (streamingLinks.isNotEmpty()) {
            SectionLabel("Yayınlayan platformlar")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                streamingLinks.forEach { link ->
                    LinkChip(
                        link = link,
                        color = accentColor,
                        solid = true,
                        onClick = { runCatching { uriHandler.openUri(link.url) } }
                    )
                }
            }
        }
        if (externalLinks.isNotEmpty()) {
            SectionLabel("Harici bağlantılar")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                externalLinks.forEach { link ->
                    LinkChip(
                        link = link,
                        color = KitsugiColors.TextSecondary,
                        solid = false,
                        onClick = { runCatching { uriHandler.openUri(link.url) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkChip(
    link: KitsugiExternalLink,
    color: Color,
    solid: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (solid) color.copy(alpha = 0.14f) else KitsugiColors.SurfaceSoft)
            .tvClickable(shape = RoundedCornerShape(999.dp), onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = link.site,
            color = if (solid) color else KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (!link.language.isNullOrBlank()) {
            Text(
                text = link.language,
                color = (if (solid) color else KitsugiColors.TextSecondary).copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            imageVector = Icons.Rounded.OpenInBrowser,
            contentDescription = null,
            tint = (if (solid) color else KitsugiColors.TextSecondary).copy(alpha = 0.6f),
            modifier = Modifier.size(13.dp)
        )
    }
}

// ---------------------------------------------------------------------------
//  Shared helpers
// ---------------------------------------------------------------------------
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = KitsugiColors.TextPrimary,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun KitsugiChip(
    text: String,
    color: Color,
    solid: Boolean,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (solid) color.copy(alpha = 0.15f) else KitsugiColors.SurfaceSoft)
            .border(
                width = if (!solid) 0.5.dp else 0.dp,
                color = if (!solid) color.copy(alpha = 0.30f) else Color.Transparent,
                shape = RoundedCornerShape(999.dp)
            )
            .then(
                if (onClick != null) Modifier.tvClickable(shape = RoundedCornerShape(999.dp), onClick = onClick) else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = if (solid) color else KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
