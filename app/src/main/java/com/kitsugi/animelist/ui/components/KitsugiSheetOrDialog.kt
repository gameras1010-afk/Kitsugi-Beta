package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.launch

val LocalDismissAnimated = staticCompositionLocalOf<() -> Unit> { {} }

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
 * [innerGridScrollState]: Opsiyonel iç LazyGridState. LazyVerticalGrid içeren sheet'ler için.
 *
 * [innerColumnScrollState]: Opsiyonel iç ScrollState. Column + verticalScroll içeren sheet'ler için.
 *
 * [enableSwipeToDismiss]: Sürüklemeyle kapatmayı zorla engellemek için.
 *
 * [sheetGesturesEnabled]: ModalBottomSheet gesture'larını (swipe-to-dismiss dahil) tamamen
 * kontrol etmek için. HorizontalPager içeren sheet'lerde false veya dinamik geçirilmeli.
 *
 * [fullScreen]: true olduğunda mobilde tam ekran Dialog olarak gösterilir (BottomSheet yerine).
 * Ayarlar sayfaları gibi içerik yoğun paneller için önerilir.
 *
 * Usage: replace ModalBottomSheet { ... } with KitsugiSheetOrDialog(onDismiss = ...) { ... }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitsugiSheetOrDialog(
    onDismiss: () -> Unit,
    heightFraction: Float = 0.9f,
    fillMaxHeight: Boolean = false,
    fullScreen: Boolean = false,
    innerScrollState: LazyListState? = null,
    innerGridScrollState: LazyGridState? = null,
    innerColumnScrollState: androidx.compose.foundation.ScrollState? = null,
    enableSwipeToDismiss: Boolean = true,
    /**
     * null = auto-compute from scroll states (AniHyou style: gestures disabled when scrolled down)
     * true = always enable gestures
     * false = always disable gestures (use for HorizontalPager dialogs)
     */
    sheetGesturesEnabled: Boolean? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isTv = LocalIsTv.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    if (isTv) {
        val focusRequester = remember { FocusRequester() }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            focusRequester.requestFocusAfterFrames(frames = 3)
        }

        val dismissAnimated = remember(onDismiss) { onDismiss }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            CompositionLocalProvider(LocalDismissAnimated provides dismissAnimated) {
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
        }
    } else if (fullScreen) {
        // ── Full-screen Dialog mode (for settings pages) ──────────────────────
        val dismissAnimated = remember(onDismiss) { onDismiss }

        androidx.activity.compose.BackHandler {
            onDismiss()
        }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                decorFitsSystemWindows = false
            )
        ) {
            CompositionLocalProvider(LocalDismissAnimated provides dismissAnimated) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KitsugiColors.Surface)
                        .navigationBarsPadding(),
                    content = content
                )
            }
        }
    } else {
        val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val effectiveHeightFraction = if (isLandscape) 0.95f else heightFraction

        val scope = rememberCoroutineScope()

        // One-shot dismiss guard: prevents double-dismiss when the swipe gesture
        // completes and onDismissRequest fires at the same time (e.g., after an
        // accidental horizontal scroll that the system interprets as a dismiss swipe).
        val isDismissing = remember { mutableStateOf(false) }

        // Compute whether the content is at the top (AniHyou style)
        val isAtTop by remember {
            derivedStateOf {
                when {
                    innerScrollState != null ->
                        innerScrollState.firstVisibleItemIndex == 0 &&
                                innerScrollState.firstVisibleItemScrollOffset == 0
                    innerGridScrollState != null ->
                        innerGridScrollState.firstVisibleItemIndex == 0 &&
                                innerGridScrollState.firstVisibleItemScrollOffset == 0
                    innerColumnScrollState != null ->
                        innerColumnScrollState.value == 0
                    else -> true
                }
            }
        }

        // Effective gestures: explicit override wins; otherwise auto from scroll position
        val effectiveGesturesEnabled = sheetGesturesEnabled ?: (isAtTop && enableSwipeToDismiss)

        fun safeDismiss() {
            if (!isDismissing.value) {
                isDismissing.value = true
                onDismiss()
            }
        }

        // Scroll-aware dismissal: block if content is not at top
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { targetValue ->
                if (targetValue == SheetValue.Hidden) {
                    // Only allow hide if gestures enabled AND at top
                    enableSwipeToDismiss && isAtTop
                } else {
                    true
                }
            }
        )

        val dismissAnimated: () -> Unit = remember(sheetState, onDismiss) {
            {
                if (!isDismissing.value) {
                    isDismissing.value = true
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onDismiss()
                    }
                }
            }
        }

        androidx.activity.compose.BackHandler(enabled = sheetState.isVisible) {
            if (!isDismissing.value) {
                isDismissing.value = true
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onDismiss()
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { safeDismiss() },
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
            CompositionLocalProvider(LocalDismissAnimated provides dismissAnimated) {
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
}
