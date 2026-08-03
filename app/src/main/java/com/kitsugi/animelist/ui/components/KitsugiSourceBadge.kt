package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.sp

/**
 * Platform kaynak rozeti — AniHyou + MoeList referans tasarımından ilham alınmıştır.
 * Poster görselinin sol-alt köşesine yerleştirilmek üzere tasarlanmıştır.
 *
 * Desteklenen kaynaklar:
 *  - "anilist"           → mavi arka plan, "AL" etiketi
 *  - "mal" / "jikan"     → lacivert arka plan, "MAL" etiketi
 *  - "tmdb"              → koyu lacivert arka plan, "TMDB" etiketi
 *  - "simkl"             → koyu arka plan, "SK" etiketi
 *  - "kitsu"             → turuncu arka plan, "KT" etiketi
 *  - "shikimori"         → mavi arka plan, "SHI" etiketi
 *  - diğerleri           → şeffaf / gösterilmez
 */
@Composable
fun KitsugiSourceBadge(
    source: String,
    modifier: Modifier = Modifier
) {
    val normalized = source.lowercase()

    // Platform renklerini ve etiketlerini tanımla
    val (label, bgColor) = when (normalized) {
        "anilist"          -> "AL"   to Color(0xFF02A9FF) // AniList mavi
        "mal", "jikan"     -> "MAL"  to Color(0xFF2E51A2) // MyAnimeList lacivert
        "tmdb"             -> "TMDB" to Color(0xFF0D253F) // TMDB koyu lacivert
        "simkl"            -> "SK"   to Color(0xFF1F1F1F) // Simkl koyu
        "kitsu"            -> "KT"   to Color(0xFFE35A02) // Kitsu turuncu
        "shikimori"        -> "SHI"  to Color(0xFF4C86C8) // Shikimori mavi
        else -> return                                      // Bilinmeyen kaynak → gösterme
    }

    // Ön plan metin rengi — her zaman beyaz, yeterli kontrast sağlar
    val textColor = Color.White

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topEnd = 10.dp, bottomStart = 6.dp))
            .background(bgColor.copy(alpha = 0.92f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.5.sp
        )
    }
}
