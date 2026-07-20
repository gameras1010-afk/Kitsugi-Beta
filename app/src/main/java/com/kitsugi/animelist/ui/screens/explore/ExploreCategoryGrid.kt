package com.kitsugi.animelist.ui.screens.explore

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens

/**
 * Tek bir keşfet kategori kartı.
 * AniHyou'nun IconCard bileşeninden ilham alınarak Kitsugi'nun dark premium tasarımına uyarlanmıştır.
 */
@Composable
fun ExploreCategoryCard(
    title: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isTv = com.kitsugi.animelist.ui.theme.LocalIsTv.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // TV: KitsugiTvTokens referanslı kompakt değerler
    val verticalPadding  = if (isTv) KitsugiTvTokens.Spacing.md   else if (isLandscape) 8.dp  else 16.dp
    val horizontalPadding = if (isTv) KitsugiTvTokens.Spacing.md   else if (isLandscape) 12.dp else 14.dp
    val iconSize          = if (isTv) KitsugiTvTokens.Shapes.let { 20.dp }                  else if (isLandscape) 18.dp else 24.dp
    val spacerHeight      = if (isTv) 6.dp                        else if (isLandscape) 4.dp  else 8.dp

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "card_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            // TV: KitsugiTvTokens.Shapes.categoryCard (14dp), mobil 18dp
            .clip(if (isTv) KitsugiTvTokens.Shapes.categoryCard else RoundedCornerShape(18.dp))
            .background(brush = Brush.linearGradient(gradientColors))
            .tvClickable(shape = if (isTv) KitsugiTvTokens.Shapes.categoryCard else RoundedCornerShape(18.dp)) {
                pressed = false
                onClick()
            }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.height(spacerHeight))
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                // TV: kompakt font boyutu
                fontSize = if (isTv) 12.sp else if (isLandscape) 12.sp else 13.sp
            )
        }
    }
}

/**
 * Anime kategorileri için dinamik grid.
 * Yatay çiftler veya üçlüler hâlinde düzenlenir.
 */
@Composable
fun ExploreCategoryGrid(
    title: String,
    items: List<ExploreCategoryItem>,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalKitsugiAccent.current
    val isTv = com.kitsugi.animelist.ui.theme.LocalIsTv.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // TV: KitsugiTvTokens.Layout.exploreCategoryColumns = 4
    val columns = if (isTv) {
        KitsugiTvTokens.Layout.exploreCategoryColumns
    } else if (isLandscape) {
        if (items.size % 4 == 0) 4 else 3
    } else {
        2
    }
    // TV: KitsugiTvTokens.Cards.categoryHeight = 64dp
    val cardHeight = if (isTv) KitsugiTvTokens.Cards.categoryHeight else if (isLandscape) 64.dp else 90.dp
    // TV: satırlar arasi bosluk token'dan
    val rowGap = if (isTv) KitsugiTvTokens.Spacing.sm else 10.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Dinamik kolon sayısına göre satırları grupla
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = rowGap),
                horizontalArrangement = Arrangement.spacedBy(
                    if (isTv) KitsugiTvTokens.Spacing.sm else 10.dp
                )
            ) {
                rowItems.forEach { item ->
                    ExploreCategoryCard(
                        title = item.title,
                        icon = item.icon,
                        gradientColors = item.gradientColors,
                        modifier = Modifier.weight(1f).height(cardHeight),
                        onClick = item.onClick
                    )
                }
                // Boşlukları doldur
                if (rowItems.size < columns) {
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

data class ExploreCategoryItem(
    val title: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val onClick: () -> Unit
)
