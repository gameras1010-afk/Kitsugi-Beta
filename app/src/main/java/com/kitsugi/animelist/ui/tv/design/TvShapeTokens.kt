package com.kitsugi.animelist.ui.tv.design

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class KitsugiRadiusTokens(
    val none: Dp,
    val xxs: Dp,
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val panel: Dp,
    val full: Dp
)

@Immutable
data class KitsugiShapeTokens(
    val posterCard: Shape,
    val backdropCard: Shape,
    val collectionCard: Shape,
    val button: Shape,
    val iconButton: Shape,
    val chip: Shape,
    val badge: Shape,
    val dialog: Shape,
    val sidePanel: Shape,
    val sidebar: Shape,
    val navItem: Shape,
    val progress: Shape,
    val slider: Shape,
    val field: Shape,
    val menu: Shape,
    val circle: Shape
)

object KitsugiRadii {
    val tokens = KitsugiRadiusTokens(
        none = 0.dp,
        xxs = 2.dp,
        xs = 4.dp,
        sm = 8.dp,
        md = 12.dp,
        lg = 14.dp,
        xl = 16.dp,
        xxl = 20.dp,
        panel = 28.dp,
        full = 999.dp
    )
}

object KitsugiShapes {
    val tokens = KitsugiShapeTokens(
        posterCard = RoundedCornerShape(KitsugiRadii.tokens.md),
        backdropCard = RoundedCornerShape(KitsugiRadii.tokens.xl),
        collectionCard = RoundedCornerShape(KitsugiRadii.tokens.xl),
        button = RoundedCornerShape(KitsugiRadii.tokens.md),
        iconButton = RoundedCornerShape(KitsugiRadii.tokens.md),
        chip = RoundedCornerShape(KitsugiRadii.tokens.full),
        badge = RoundedCornerShape(KitsugiRadii.tokens.xs),
        dialog = RoundedCornerShape(KitsugiRadii.tokens.xl),
        sidePanel = RoundedCornerShape(KitsugiRadii.tokens.xxl),
        sidebar = RoundedCornerShape(30.dp),
        navItem = RoundedCornerShape(KitsugiRadii.tokens.full),
        progress = RoundedCornerShape(KitsugiRadii.tokens.xxs),
        slider = RoundedCornerShape(KitsugiRadii.tokens.full),
        field = RoundedCornerShape(KitsugiRadii.tokens.md),
        menu = RoundedCornerShape(KitsugiRadii.tokens.lg),
        circle = CircleShape
    )
}
