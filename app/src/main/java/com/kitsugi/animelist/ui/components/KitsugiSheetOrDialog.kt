package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.kitsugi.animelist.ui.tv.focus.TvFocusRestoration.safeRequestFocus
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import com.kitsugi.animelist.ui.theme.LocalIsTv
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.KitsugiTvTokens

/**
 * Smart wrapper that renders as a ModalBottomSheet on mobile devices,
 * and as a full-screen centered Dialog on Android TV (for D-pad / remote control navigation).
 *
 * TV dialog'da içerik boyutuna göre esnek yükseklik kullanılır.
 * LazyColumn içeren sheet'lerde D-pad scroll doğal olarak çalışır.
 *
 * [innerScrollState]: Opsiyonel iç LazyListState. Verilirse, sheet içerik en üstte
 * olmadığında (scroll aşağı kaydırılmışken) aşağı sürüklemeyle kapatma engellenir.
 *
 * Usage: replace ModalBottomSheet { ... } with KitsugiSheetOrDialog(onDismiss = ...) { ... }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiSheetOrDialog(
    onDismiss: () -> Unit,
    heightFraction: Float = 0.9f,
    fillMaxHeight: Boolean = false,
    innerScrollState: LazyListState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isTv = LocalIsTv.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    if (isTv) {
        // TV: geniş ortalanmış dialog, D-pad navigasyonu için optimize
        val focusRequester = remember { FocusRequester() }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            focusRequester.requestFocusAfterFrames(frames = 3)
        }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .widthIn(min = 480.dp, max = 640.dp)
                    .wrapContentHeight()
                    .heightIn(
                        max = screenHeight * 0.85f
                    )
                    .focusRequester(focusRequester)
                    .focusable()
                    .clip(KitsugiTvTokens.Shapes.dialog)
                    .background(KitsugiColors.Surface)
                    .padding(bottom = 8.dp),
                content = content
            )
        }
    } else {
        // Mobil: içerik en üstte olmadığında sürüklemeyle kapatmayı engelle
        val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val effectiveHeightFraction = if (isLandscape) 0.95f else heightFraction

        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { targetValue ->
                if (targetValue == SheetValue.Hidden && innerScrollState != null) {
                    val atTop = innerScrollState.firstVisibleItemIndex == 0 &&
                        innerScrollState.firstVisibleItemScrollOffset == 0
                    atTop
                } else {
                    true
                }
            }
        )
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = KitsugiColors.Surface,
            contentColor = KitsugiColors.TextPrimary,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(KitsugiColors.TextMuted.copy(alpha = 0.4f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .then(
                        if (fillMaxHeight) {
                            Modifier.fillMaxHeight(effectiveHeightFraction)
                        } else {
                            Modifier
                                .wrapContentHeight()
                                .heightIn(max = screenHeight * effectiveHeightFraction)
                        }
                    )
                    .navigationBarsPadding(),
                content = content
            )
        }
    }
}
