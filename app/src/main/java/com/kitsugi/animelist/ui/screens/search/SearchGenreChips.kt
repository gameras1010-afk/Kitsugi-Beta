package com.kitsugi.animelist.ui.screens.search

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

/**
 * Popüler tür çipleri bölümü.
 * AniHyou'nun SearchView.kt'sindeki MediaSearchGenresChips bileşeninden
 * ve MoeList'in genre filtresi yaklaşımından ilham alınarak Kitsugi'ya uyarlanmıştır.
 *
 * Arama yapılmamış boş ekranda gösterilir. Bir türe tıklandığında
 * o türü arama çubuğuna yazıp otomatik arama tetikler.
 */

data class SearchGenre(
    val label: String,
    val emoji: String,
    val color: Color
)

private val popularGenres = listOf(
    SearchGenre("Aksiyon",    "⚔️",  Color(0xFFEF4444)),
    SearchGenre("Komedi",     "😂",  Color(0xFFF59E0B)),
    SearchGenre("Romantizm",  "💕",  Color(0xFFEC4899)),
    SearchGenre("Fantastik",  "🧙",  Color(0xFF8B5CF6)),
    SearchGenre("Macera",     "🗺️",  Color(0xFF10B981)),
    SearchGenre("Dram",       "🎭",  Color(0xFF6366F1)),
    SearchGenre("Korku",      "👻",  Color(0xFF374151)),
    SearchGenre("Sci-Fi",     "🚀",  Color(0xFF0EA5E9)),
    SearchGenre("Spor",       "⚽",  Color(0xFF16A34A)),
    SearchGenre("Psikoloji",  "🧠",  Color(0xFF7C3AED)),
    SearchGenre("Doğaüstü",  "✨",  Color(0xFFF97316)),
    SearchGenre("Müzik",      "🎵",  Color(0xFF06B6D4)),
    SearchGenre("Okul",       "🏫",  Color(0xFF84CC16)),
    SearchGenre("Tarihi",     "🏯",  Color(0xFF78716C)),
    SearchGenre("Mecha",      "🤖",  Color(0xFF475569)),
)

@Composable
fun SearchGenreChips(
    onGenreClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalKitsugiAccent.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Başlık
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "🔥",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Popüler Türler",
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Yatay kaydırılabilir çip satırı — 1. satır
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            popularGenres.take(8).forEach { genre ->
                GenreChip(
                    genre = genre,
                    accentColor = accentColor,
                    onClick = { onGenreClick(genre.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Yatay kaydırılabilir çip satırı — 2. satır
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            popularGenres.drop(8).forEach { genre ->
                GenreChip(
                    genre = genre,
                    accentColor = accentColor,
                    onClick = { onGenreClick(genre.label) }
                )
            }
        }
    }
}

@Composable
private fun GenreChip(
    genre: SearchGenre,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(genre.color.copy(alpha = 0.12f))
            .tvClickable(shape = RoundedCornerShape(20.dp), onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = genre.emoji,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = genre.label,
                color = genre.color.copy(alpha = 0.9f).let {
                    // Açık temada okunaksız olmaması için minimum parlaklık uygula
                    Color(
                        red   = (it.red   * 0.85f + KitsugiColors.TextPrimary.red   * 0.15f),
                        green = (it.green * 0.85f + KitsugiColors.TextPrimary.green * 0.15f),
                        blue  = (it.blue  * 0.85f + KitsugiColors.TextPrimary.blue  * 0.15f),
                        alpha = 1f
                    )
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
