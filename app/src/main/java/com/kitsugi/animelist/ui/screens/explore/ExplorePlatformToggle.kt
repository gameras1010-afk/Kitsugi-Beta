package com.kitsugi.animelist.ui.screens.explore

import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

/**
 * MAL | AniList platform seçici toggle.
 * AniHyou'nun ExploreSearchBar yaklaşımından ilham alınarak Kitsugi tasarım diline uyarlanmıştır.
 */
@Composable
fun ExplorePlatformToggle(
    selectedPlatform: ExplorePlatform,
    onPlatformSelected: (ExplorePlatform) -> Unit,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false
) {
    val accentColor = LocalKitsugiAccent.current

    if (isVertical) {
        androidx.compose.foundation.layout.Column(
            modifier = modifier
                .clip(RoundedCornerShape(22.dp))
                .background(KitsugiColors.Surface)
        ) {
            ExplorePlatform.entries.reversed().forEach { platform ->
                val isSelected = selectedPlatform == platform

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isSelected) accentColor else KitsugiColors.Surface)
                        .tvClickable(shape = RoundedCornerShape(22.dp)) { onPlatformSelected(platform) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = platform.label,
                        color = if (isSelected) KitsugiColors.Background else KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                    )
                }
            }
        }
    } else {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(22.dp))
                .background(KitsugiColors.Surface)
        ) {
            ExplorePlatform.entries.reversed().forEach { platform ->
                val isSelected = selectedPlatform == platform

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isSelected) accentColor else KitsugiColors.Surface)
                        .tvClickable(shape = RoundedCornerShape(22.dp)) { onPlatformSelected(platform) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = platform.label,
                        color = if (isSelected) KitsugiColors.Background else KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                    )
                }
            }
        }
    }
}
