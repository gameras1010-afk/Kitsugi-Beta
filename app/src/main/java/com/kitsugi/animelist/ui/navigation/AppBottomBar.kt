package com.kitsugi.animelist.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
            contentColor = KitsugiColors.TextPrimary
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

    androidx.compose.material3.NavigationRail(
        containerColor = KitsugiColors.BackgroundElevated.copy(alpha = 0.85f),
        contentColor = KitsugiColors.TextPrimary,
        modifier = modifier,
        header = {
            Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        }
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MainTab.entries.forEach { tab ->
                androidx.compose.material3.NavigationRailItem(
                    selected = selectedTab == tab,
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
                    label = {
                        Text(text = stringResource(tab.labelRes))
                    },
                    colors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                        selectedIconColor = KitsugiColors.Background,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor,
                        unselectedIconColor = KitsugiColors.TextSecondary,
                        unselectedTextColor = KitsugiColors.TextSecondary
                    )
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
            }
        }
    }
}

