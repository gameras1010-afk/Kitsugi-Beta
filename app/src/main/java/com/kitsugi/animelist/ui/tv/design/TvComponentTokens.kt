package com.kitsugi.animelist.ui.tv.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class KitsugiCardComponentTokens(
    val width: Dp,
    val height: Dp,
    val cornerRadius: Dp,
    val contentPadding: Dp,
    val focusedBorderWidth: Dp,
    val focusedScale: Float
)

@Immutable
data class KitsugiRowComponentTokens(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val itemSpacing: Dp,
    val titleBottomSpacing: Dp
)

@Immutable
data class KitsugiSidebarComponentTokens(
    val legacyCollapsedWidth: Dp,
    val legacyExpandedWidth: Dp,
    val collapsedWidth: Dp,
    val expandedWidth: Dp,
    val itemHeight: Dp,
    val itemWidth: Dp,
    val iconSize: Dp,
    val leadingVisual: Dp,
    val panelRadius: Dp,
    val contentGap: Dp
)

@Immutable
data class KitsugiDialogComponentTokens(
    val maxWidth: Dp,
    val contentPadding: Dp,
    val cornerRadius: Dp,
    val actionSpacing: Dp
)

@Immutable
data class KitsugiPlayerComponentTokens(
    val overlayHorizontalPadding: Dp,
    val overlayVerticalPadding: Dp,
    val controlSize: Dp,
    val sidePanelWidth: Dp,
    val railWidth: Dp,
    val progressHeight: Dp
)

@Immutable
data class KitsugiSettingsComponentTokens(
    val containerRadius: Dp,
    val secondaryCardRadius: Dp,
    val railItemHeight: Dp,
    val workspacePadding: Dp,
    val rowGap: Dp
)

@Immutable
data class KitsugiComponentTokens(
    val posterCard: KitsugiCardComponentTokens,
    val backdropCard: KitsugiCardComponentTokens,
    val collectionCard: KitsugiCardComponentTokens,
    val continueWatchingCard: KitsugiCardComponentTokens,
    val episodeCard: KitsugiCardComponentTokens,
    val row: KitsugiRowComponentTokens,
    val sidebar: KitsugiSidebarComponentTokens,
    val dialog: KitsugiDialogComponentTokens,
    val sidePanel: KitsugiDialogComponentTokens,
    val settings: KitsugiSettingsComponentTokens,
    val player: KitsugiPlayerComponentTokens,
    val buttonHeight: Dp,
    val chipHeight: Dp,
    val badgeHeight: Dp,
    val skeletonCornerRadius: Dp
)

object KitsugiComponents {
    val tokens = KitsugiComponentTokens(
        posterCard = KitsugiCardComponentTokens(
            width = 126.dp,
            height = 189.dp,
            cornerRadius = 12.dp,
            contentPadding = 8.dp,
            focusedBorderWidth = 2.dp,
            focusedScale = 1.02f
        ),
        backdropCard = KitsugiCardComponentTokens(
            width = 320.dp,
            height = 180.dp,
            cornerRadius = 16.dp,
            contentPadding = 16.dp,
            focusedBorderWidth = 2.dp,
            focusedScale = 1.02f
        ),
        collectionCard = KitsugiCardComponentTokens(
            width = 320.dp,
            height = 180.dp,
            cornerRadius = 16.dp,
            contentPadding = 16.dp,
            focusedBorderWidth = 2.dp,
            focusedScale = 1.02f
        ),
        continueWatchingCard = KitsugiCardComponentTokens(
            width = 260.dp,
            height = 146.dp,
            cornerRadius = 12.dp,
            contentPadding = 12.dp,
            focusedBorderWidth = 2.dp,
            focusedScale = 1.02f
        ),
        episodeCard = KitsugiCardComponentTokens(
            width = 320.dp,
            height = 207.dp,
            cornerRadius = 16.dp,
            contentPadding = 16.dp,
            focusedBorderWidth = 2.dp,
            focusedScale = 1.02f
        ),
        row = KitsugiRowComponentTokens(
            horizontalPadding = 48.dp,
            verticalPadding = 6.dp,
            itemSpacing = 12.dp,
            titleBottomSpacing = 14.dp
        ),
        sidebar = KitsugiSidebarComponentTokens(
            legacyCollapsedWidth = 72.dp,
            legacyExpandedWidth = 196.dp,
            collapsedWidth = 184.dp,
            expandedWidth = 262.dp,
            itemHeight = 52.dp,
            itemWidth = 148.dp,
            iconSize = 22.dp,
            leadingVisual = 34.dp,
            panelRadius = 30.dp,
            contentGap = 14.dp
        ),
        dialog = KitsugiDialogComponentTokens(
            maxWidth = 720.dp,
            contentPadding = 28.dp,
            cornerRadius = 24.dp,
            actionSpacing = 12.dp
        ),
        sidePanel = KitsugiDialogComponentTokens(
            maxWidth = 420.dp,
            contentPadding = 20.dp,
            cornerRadius = 20.dp,
            actionSpacing = 12.dp
        ),
        settings = KitsugiSettingsComponentTokens(
            containerRadius = 28.dp,
            secondaryCardRadius = 18.dp,
            railItemHeight = 56.dp,
            workspacePadding = 20.dp,
            rowGap = 16.dp
        ),
        player = KitsugiPlayerComponentTokens(
            overlayHorizontalPadding = 52.dp,
            overlayVerticalPadding = 36.dp,
            controlSize = 44.dp,
            sidePanelWidth = 360.dp,
            railWidth = 280.dp,
            progressHeight = 4.dp
        ),
        buttonHeight = 52.dp,
        chipHeight = 32.dp,
        badgeHeight = 20.dp,
        skeletonCornerRadius = 10.dp
    )
}
