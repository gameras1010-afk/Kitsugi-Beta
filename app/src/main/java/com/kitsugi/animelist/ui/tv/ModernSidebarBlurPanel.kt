package com.kitsugi.animelist.ui.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

import com.kitsugi.animelist.ui.tv.navigation.TvDestination
import com.kitsugi.animelist.ui.tv.navigation.TvNavItem

@Composable
internal fun ModernSidebarBlurPanel(
    drawerItems: List<TvNavItem>,
    selectedDrawerRoute: TvDestination,
    keepSidebarFocusDuringCollapse: Boolean,
    sidebarLabelAlpha: Float,
    sidebarIconScale: Float,
    sidebarExpandProgress: Float,
    isSidebarExpanded: Boolean,
    sidebarCollapsePending: Boolean,
    blurEnabled: Boolean,
    sidebarHazeState: HazeState,
    panelShape: RoundedCornerShape,
    drawerItemFocusRequesters: Map<TvDestination, FocusRequester>,
    contentFocusRequester: FocusRequester,
    onDrawerItemFocused: (Int) -> Unit,
    onDrawerItemClick: (TvDestination) -> Unit,
    activeProfileName: String,
    activeProfileAvatarImageUrl: String?,
    showProfileSelector: Boolean,
    onSwitchProfile: () -> Unit
) {
    val delayedBlurProgress = ((sidebarExpandProgress - 0.34f) / 0.66f).coerceIn(0f, 1f)
    val showPanelBlur = blurEnabled && isSidebarExpanded && !sidebarCollapsePending && delayedBlurProgress > 0f

    val expandedPanelBlurModifier = if (showPanelBlur) {
        Modifier.hazeChild(
            state = sidebarHazeState,
            shape = panelShape,
            tint = Color.Unspecified,
            blurRadius = 24.dp * delayedBlurProgress,
            noiseFactor = 0.04f * delayedBlurProgress
        )
    } else {
        Modifier
    }

    val bgElevated = KitsugiColors.BackgroundElevated
    val surf = KitsugiColors.Surface
    val borderCol = KitsugiColors.Border

    val panelBackgroundBrush = remember(blurEnabled, bgElevated, surf) {
        if (blurEnabled) {
            Brush.verticalGradient(
                listOf(
                    Color(0x3310131D),
                    Color(0x2210131D),
                    Color(0x33171B28)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    bgElevated.copy(alpha = 0.92f),
                    surf.copy(alpha = 0.92f)
                )
            )
        }
    }

    val panelBorderColor = remember(blurEnabled, borderCol) {
        if (blurEnabled) Color.White.copy(alpha = 0.14f) else borderCol.copy(alpha = 0.9f)
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .graphicsLayer {
                val p = sidebarExpandProgress
                alpha = p
                val s = 0.97f + (0.03f * p)
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .then(expandedPanelBlurModifier)
            .graphicsLayer {
                shape = panelShape
                clip = true
            }
            .clip(panelShape)
            .background(brush = panelBackgroundBrush, shape = panelShape)
            .border(width = 1.dp, color = panelBorderColor, shape = panelShape)
            .padding(horizontal = KitsugiTvTokens.Spacing.md, vertical = KitsugiTvTokens.Spacing.screenVertical)
    ) {
        // Profile Info or Header
        if (showProfileSelector && activeProfileName.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = KitsugiTvTokens.Spacing.sm),
                contentAlignment = Alignment.Center
            ) {
                SidebarProfileItem(
                    profileName = activeProfileName,
                    profileAvatarImageUrl = activeProfileAvatarImageUrl,
                    focusEnabled = keepSidebarFocusDuringCollapse,
                    labelAlpha = sidebarLabelAlpha,
                    contentFocusRequester = contentFocusRequester,
                    onFocusChanged = { focused ->
                        if (focused) onDrawerItemFocused(drawerItems.size)
                    },
                    onClick = onSwitchProfile,
                    modifier = Modifier.fillMaxWidth(0.92f)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = KitsugiTvTokens.Spacing.sm)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "NUVIO",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.graphicsLayer { alpha = sidebarLabelAlpha }
                )
            }
        }

        Spacer(modifier = Modifier.height(KitsugiTvTokens.Spacing.rowGap))

        // Navigation Items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.offset(y = (-12).dp),
                verticalArrangement = Arrangement.spacedBy(KitsugiTvTokens.Spacing.sm)
            ) {
                drawerItems.forEachIndexed { index, item ->
                    key(item.destination) {
                        val focusRequester = drawerItemFocusRequesters[item.destination] ?: remember { FocusRequester() }
                        SidebarNavigationItem(
                            label = item.label,
                            icon = item.icon,
                            selected = selectedDrawerRoute == item.destination,
                            focusEnabled = keepSidebarFocusDuringCollapse,
                            labelAlpha = sidebarLabelAlpha,
                            iconScale = sidebarIconScale,
                            contentFocusRequester = contentFocusRequester,
                            onFocusChanged = { focused ->
                                if (focused) {
                                    onDrawerItemFocused(index)
                                }
                            },
                            onClick = { onDrawerItemClick(item.destination) },
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .focusRequester(focusRequester)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarNavigationItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    focusEnabled: Boolean,
    labelAlpha: Float,
    iconScale: Float,
    contentFocusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            selected -> KitsugiColors.SurfaceStrong
            isFocused -> Color.White.copy(alpha = 0.1f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "sidebarItemBackground"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.White.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "sidebarItemBorder"
    )

    val contentColor = if (selected) KitsugiColors.Accent else KitsugiColors.TextPrimary
    val iconCircleColor = if (selected) Color.White.copy(alpha = 0.1f) else KitsugiColors.SurfaceSoft

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Card(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged {
                isFocused = it.hasFocus
                onFocusChanged(it.hasFocus)
            }
            .focusProperties {
                canFocus = focusEnabled
                if (isRtl) {
                    left = contentFocusRequester
                } else {
                    right = contentFocusRequester
                }
            },
        colors = CardDefaults.colors(
            containerColor = backgroundColor,
            focusedContainerColor = backgroundColor,
        ),
        border = CardDefaults.border(
            border = androidx.tv.material3.Border.None,
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(999.dp)
            )
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(999.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(KitsugiTvTokens.Layout.sidebarIconSize)
                    .clip(CircleShape)
                    .background(iconCircleColor)
                    .padding(6.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(KitsugiTvTokens.Spacing.itemGap))

            Text(
                text = label,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = labelAlpha }
            )
        }
    }
}

@Composable
private fun SidebarProfileItem(
    profileName: String,
    profileAvatarImageUrl: String?,
    focusEnabled: Boolean,
    labelAlpha: Float,
    contentFocusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = if (isFocused) Color.White.copy(alpha = 0.1f) else Color.Transparent
    val borderColor = if (isFocused) Color.White.copy(alpha = 0.3f) else Color.Transparent

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Card(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged {
                isFocused = it.hasFocus
                onFocusChanged(it.hasFocus)
            }
            .focusProperties {
                canFocus = focusEnabled
                if (isRtl) {
                    left = contentFocusRequester
                } else {
                    right = contentFocusRequester
                }
            },
        colors = CardDefaults.colors(
            containerColor = backgroundColor,
            focusedContainerColor = backgroundColor,
        ),
        border = CardDefaults.border(
            border = androidx.tv.material3.Border.None,
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(999.dp)
            )
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(999.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(KitsugiTvTokens.Layout.sidebarIconSize),
                contentAlignment = Alignment.Center
            ) {
                if (!profileAvatarImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profileAvatarImageUrl,
                        contentDescription = profileName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(KitsugiColors.SurfaceStrong),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profileName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(KitsugiTvTokens.Spacing.itemGap))

            Text(
                text = profileName,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = labelAlpha }
            )
        }
    }
}
