package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.R
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.utils.toFriendlySourceLabel

// ─────────────────────────────────────────────────────────────────────────────
// V2-A03 – ImdbRatingSourceLabel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * IMDb / Simkl / AniList / MAL kaynaklı puanı küçük bir pill olarak gösterir.
 *
 * @param rating    Gösterilecek puan (ör. 8.4)
 * @param source    Kaynak adı: "imdb", "simkl", "anilist", "mal"
 */
@Composable
fun ImdbRatingSourceLabel(
    rating: Double,
    source: String = "imdb",
    modifier: Modifier = Modifier
) {
    val friendlySource = source.toFriendlySourceLabel()
    val (bgColor, textColor, label) = when (friendlySource.lowercase()) {
        "imdb"        -> Triple(Color(0xFFF5C518), Color.Black, "IMDb")
        "simkl"       -> Triple(Color(0xFF1F2744), Color.White, "SIMKL")
        "anilist"     -> Triple(Color(0xFF02A9FF), Color.White, "AniList")
        "myanimelist" -> Triple(Color(0xFF2E51A2), Color.White, "MyAnimeList")
        else          -> Triple(KitsugiColors.SurfaceSoft, KitsugiColors.TextPrimary, friendlySource)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(10.dp)
        )
        Text(
            text = "%.1f".format(rating),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 11.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.75f),
            fontSize = 10.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// V2-A03 – SourceStatusFilterChip
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Library/keşif ekranı filtrelerinde kullanılan kaynak bazlı FilterChip.
 * Seçilince dolgu rengiyle aktif görünür.
 */
@Composable
fun SourceStatusFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = KitsugiColors.Accent,
    unselectedColor: Color = KitsugiColors.SurfaceSoft
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.sp
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedColor,
            selectedLabelColor = Color.White,
            containerColor = unselectedColor,
            labelColor = KitsugiColors.TextSecondary
        ),
        border = if (selected) null else FilterChipDefaults.filterChipBorder(
            borderColor = KitsugiColors.Border,
            enabled = true,
            selected = false
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// V2-A03 – StreamBadgeChips
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Stream kaynaklarını (ör. "1080p", "DUB", "SUB", "HDR") küçük badge olarak
 * gösterir. StreamSelector'da kaynak kalitesi gösterimi için kullanılır.
 */
@Composable
fun QualityBadge(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = KitsugiColors.AccentMuted,
    contentColor: Color = KitsugiColors.Accent
) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = contentColor,
        fontSize = 9.sp,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(containerColor)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

@Composable
fun SubDubBadge(
    isDub: Boolean,
    modifier: Modifier = Modifier
) {
    val text = if (isDub) "DUB" else "SUB"
    val bg = if (isDub) Color(0xFF3E2060) else Color(0xFF1A3060)
    val fg = if (isDub) Color(0xFFD4A4FF) else Color(0xFF82B4FF)

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = fg,
        fontSize = 9.sp,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

@Composable
fun HdrBadge(modifier: Modifier = Modifier) {
    Text(
        text = "HDR",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFFFFD700),
        fontSize = 9.sp,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF3A2800))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// V2-A03 – CollectionCardGlow
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Koleksiyon/playlist kartının odaklandığında veya hover'da neon glow efekti
 * ekleyen Modifier extension'ı.
 */
fun Modifier.collectionCardGlow(
    glowColor: Color,
    enabled: Boolean = true,
    elevation: Dp = 12.dp
): Modifier = if (enabled) {
    this
        .shadow(
            elevation = elevation,
            shape = RoundedCornerShape(16.dp),
            ambientColor = glowColor.copy(alpha = 0.6f),
            spotColor = glowColor.copy(alpha = 0.8f)
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.7f),
                    glowColor.copy(alpha = 0.2f)
                )
            ),
            shape = RoundedCornerShape(16.dp)
        )
} else this
