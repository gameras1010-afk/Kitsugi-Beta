package com.kitsugi.animelist.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Arama çubuğu bileşeni.
 * AniHyou'nun ExploreSearchBar Material3 SearchBar yaklaşımından ilham alınarak
 * Kitsugi'nun design sistemine (KitsugiColors) uyarlanmıştır.
 *
 * - Metin girildiğinde sağda animasyonlu X (temizle) ve Ara butonları beliriyor.
 * - X butonuna tıklandığında hem metin hem sonuçlar sıfırlanıyor → geçmiş görünümüne dönüş.
 */
@Composable
fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearQuery: () -> Unit,
    onFilterClick: () -> Unit,
    hasActiveFilters: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalKitsugiAccent.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isTvDevice = com.kitsugi.animelist.ui.theme.LocalIsTvDevice.current
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(22.dp))
            .onFocusChanged { state -> isFocused = state.hasFocus }
            .border(
                width = if (isFocused && isTvDevice) 2.dp else 0.dp,
                color = if (isFocused && isTvDevice) accentColor else Color.Transparent,
                shape = RoundedCornerShape(22.dp)
            )
            .background(KitsugiColors.Surface)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Ara",
                tint = KitsugiColors.TextMuted,
                modifier = Modifier.size(20.dp)
            )

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                cursorBrush = SolidColor(accentColor),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = KitsugiColors.TextPrimary,
                    fontWeight = FontWeight.Normal
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch()
                        keyboardController?.hide()
                    }
                ),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Anime, dizi veya film ara...",
                            color = KitsugiColors.TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    innerTextField()
                }
            )

            // X — Temizle butonu (AniHyou'daki gibi animasyonlu)
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(
                    onClick = {
                        onClearQuery()
                        keyboardController?.hide()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Temizle",
                        tint = KitsugiColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Ara butonu
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(
                    onClick = {
                        onSearch()
                        keyboardController?.hide()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Ara",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Filtrele Butonu
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (hasActiveFilters) accentColor.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .border(
                        width = if (hasActiveFilters) 1.5.dp else 0.dp,
                        color = if (hasActiveFilters) accentColor else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Filtrele",
                    tint = if (hasActiveFilters) accentColor else KitsugiColors.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
