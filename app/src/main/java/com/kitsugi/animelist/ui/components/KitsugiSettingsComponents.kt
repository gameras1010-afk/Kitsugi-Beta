package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun KitsugiSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title.uppercase(),
            color = KitsugiColors.TextMuted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = KitsugiColors.Surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

@Composable
fun KitsugiSettingsItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isTv = LocalIsTv.current
    val accentColor = LocalKitsugiAccent.current
    var isFocused by remember { mutableStateOf(false) }

    val focusModifier = if (isTv) {
        Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                if (isFocused) accentColor.copy(alpha = 0.12f)
                else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .then(focusModifier)
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(12.dp), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(
            icon = icon,
            color = iconColor
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "›",
            color = KitsugiColors.TextMuted,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun KitsugiSettingsSwitchItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isTv = LocalIsTv.current
    val accentColor = LocalKitsugiAccent.current
    var isFocused by remember { mutableStateOf(false) }

    val focusModifier = if (isTv) {
        Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                if (isFocused && enabled) accentColor.copy(alpha = 0.12f)
                else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .then(focusModifier)
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(12.dp), enabled = enabled) {
                onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val finalIconColor = if (enabled) iconColor else KitsugiColors.TextMuted
        SettingsIcon(
            icon = icon,
            color = finalIconColor
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = if (enabled) KitsugiColors.TextPrimary else KitsugiColors.TextMuted,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                color = if (enabled) KitsugiColors.TextSecondary else KitsugiColors.TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = KitsugiColors.Background,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = KitsugiColors.TextSecondary,
                uncheckedTrackColor = KitsugiColors.SurfaceStrong
            )
        )
    }
}

@Composable
fun KitsugiSettingsListItem(
    title: String,
    description: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isTv = LocalIsTv.current
    val accentColor = LocalKitsugiAccent.current
    var isFocused by remember { mutableStateOf(false) }

    val focusModifier = if (isTv) {
        Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                if (isFocused) accentColor.copy(alpha = 0.12f)
                else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .then(focusModifier)
            .fillMaxWidth()
            .tvClickable(shape = RoundedCornerShape(12.dp), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(
            icon = icon,
            color = iconColor
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Title + description + value stacked in one column — no layout fight
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = description,
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Value badge — accent-tinted pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = value,
                    color = accentColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "›",
            color = KitsugiColors.TextMuted,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Kompakt, premium görünümlü dropdown menü wrapper'ı.
 * Tüm ayar dropdown'larında standart olarak kullanılır.
 */
@Composable
fun KitsugiDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .background(
                color = KitsugiColors.SurfaceStrong,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp)),
        content = content
    )
}

/**
 * Kompakt dropdown item. [selected] true ise sağda checkmark gösterir.
 */
@Composable
fun KitsugiDropdownItem(
    text: String,
    onClick: () -> Unit,
    selected: Boolean = false
) {
    val accentColor = LocalKitsugiAccent.current
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                color = if (selected) accentColor else KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        onClick = onClick,
        trailingIcon = if (selected) ({
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }) else null,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        colors = MenuDefaults.itemColors(
            textColor = KitsugiColors.TextPrimary,
            leadingIconColor = accentColor,
            trailingIconColor = accentColor
        ),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
fun KitsugiSettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 74.dp)
            .background(KitsugiColors.Border)
    )
}

@Composable
private fun SettingsIcon(
    icon: ImageVector,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                color = color,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}