package com.kitsugi.animelist.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow


import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone

@Composable
fun AppBottomBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    val accentColor = LocalKitsugiAccent.current

    Column {
        HorizontalDivider(
            color = KitsugiColors.Border.copy(alpha = 0.25f),
            thickness = 0.5.dp
        )
        NavigationBar(
            containerColor = KitsugiColors.BackgroundElevated.copy(alpha = 0.85f),
            contentColor = KitsugiColors.TextPrimary,
            modifier = Modifier.height(72.dp)
        ) {
            MainTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = {
                        onTabSelected(tab)
                    },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.labelRes)
                        )
                    },
                    label = {
                        Text(text = stringResource(tab.labelRes))
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KitsugiColors.Background,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor,
                        unselectedIconColor = KitsugiColors.TextSecondary,
                        unselectedTextColor = KitsugiColors.TextSecondary
                    )
                )
            }
        }
    }
}

@Composable
fun AppNavigationRail(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val accentColor = LocalKitsugiAccent.current
    val isTv = LocalIsTv.current

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    // Auto-size width of left bar according to screen size
    val railWidth = when {
        screenWidth >= 1200 -> 96.dp
        screenWidth >= 840  -> 80.dp
        else                -> 64.dp
    }

    // Hide labels on very short screens to maximize vertical space and prevent clipping
    val showLabels = screenHeight >= 480

    // Dynamic paddings, top spacers, and vertical item spacings
    val verticalPadding = if (screenHeight >= 600) 20.dp else 12.dp
    val topSpacerHeight = if (screenHeight >= 600) 28.dp else 14.dp
    val itemSpacingHeight = if (screenHeight >= 600) 14.dp else 6.dp

    val borderColor = KitsugiColors.Border.copy(alpha = 0.15f)

    androidx.compose.material3.Surface(
        color = KitsugiColors.BackgroundElevated.copy(alpha = 0.90f),
        contentColor = KitsugiColors.TextPrimary,
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = borderColor,
                    start = Offset(size.width - strokeWidth / 2, 0f),
                    end = Offset(size.width - strokeWidth / 2, size.height),
                    strokeWidth = strokeWidth
                )
            }
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Elegant branding logo
            Text(
                text = "Kitsugi",
                color = accentColor,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.titleMedium,
                modifier = androidx.compose.ui.Modifier.padding(bottom = 16.dp)
            )

            Spacer(modifier = androidx.compose.ui.Modifier.height(topSpacerHeight))

            // Main navigation items except Settings
            val mainTabs = listOf(
                MainTab.Explore,
                MainTab.MyList,
                MainTab.Search,
                MainTab.Profile
            )

            mainTabs.forEach { tab ->
                val isSelected = selectedTab == tab
                androidx.compose.material3.NavigationRailItem(
                    selected = isSelected,
                    onClick = {
                        onTabSelected(tab)
                    },
                    modifier = androidx.compose.ui.Modifier.onFocusChanged { state ->
                        if (isTv && state.isFocused) {
                            onTabSelected(tab)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.labelRes)
                        )
                    },
                    label = if (showLabels) {
                        {
                            Text(
                                text = stringResource(tab.labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else null,
                    colors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                        selectedIconColor = KitsugiColors.Background,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor,
                        unselectedIconColor = KitsugiColors.TextSecondary,
                        unselectedTextColor = KitsugiColors.TextSecondary
                    )
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(itemSpacingHeight))
            }

            // Settings Item
            val settingsTab = MainTab.Settings
            val isSettingsSelected = selectedTab == settingsTab
            androidx.compose.material3.NavigationRailItem(
                selected = isSettingsSelected,
                onClick = {
                    onTabSelected(settingsTab)
                },
                modifier = androidx.compose.ui.Modifier.onFocusChanged { state ->
                    if (isTv && state.isFocused) {
                        onTabSelected(settingsTab)
                    }
                },
                icon = {
                    Icon(
                        imageVector = settingsTab.icon,
                        contentDescription = stringResource(settingsTab.labelRes)
                    )
                },
                label = if (showLabels) {
                    {
                        Text(
                            text = stringResource(settingsTab.labelRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else null,
                colors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                    selectedIconColor = KitsugiColors.Background,
                    selectedTextColor = accentColor,
                    indicatorColor = accentColor,
                    unselectedIconColor = KitsugiColors.TextSecondary,
                    unselectedTextColor = KitsugiColors.TextSecondary
                )
            )
        }
    }
}


