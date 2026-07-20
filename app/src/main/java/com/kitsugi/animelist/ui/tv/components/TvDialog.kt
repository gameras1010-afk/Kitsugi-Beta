package com.kitsugi.animelist.ui.tv.components

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.focusable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.tv.design.TvTheme
import com.kitsugi.animelist.ui.tv.focus.TvFocusRestoration.safeRequestFocus
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    width: Dp = 520.dp,
    titleTextAlign: TextAlign = TextAlign.Start,
    suppressFirstKeyUp: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var isReady by remember { mutableStateOf(!suppressFirstKeyUp) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocusAfterFrames(frames = 3)
    }

    val dialogTokens = TvTheme.components.dialog
    val resolvedWidth = if (width == 520.dp) dialogTokens.maxWidth.coerceAtMost(width) else width
    val cardShape = RoundedCornerShape(dialogTokens.cornerRadius)
    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.85f).coerceAtLeast(320.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false // Prevent accidental D-pad dismiss on outside clicks
        )
    ) {
        Box(
            modifier = modifier
                .width(resolvedWidth)
                .heightIn(max = maxDialogHeight)
                .clip(cardShape)
                .background(KitsugiColors.Surface, cardShape)
                .border(1.dp, KitsugiColors.Border, cardShape)
                .padding(dialogTokens.contentPadding)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    val native = event.nativeKeyEvent
                    if (native.keyCode == AndroidKeyEvent.KEYCODE_BACK) {
                        if (native.action == AndroidKeyEvent.ACTION_UP) {
                            onDismiss()
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (isSelectKey(native.keyCode) || native.keyCode == AndroidKeyEvent.KEYCODE_MENU) {
                        if (native.action == AndroidKeyEvent.ACTION_DOWN && native.repeatCount == 0) {
                            isReady = true
                        }
                        if (!isReady) {
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dialogTokens.actionSpacing)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = KitsugiColors.TextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = titleTextAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KitsugiColors.TextSecondary
                    )
                }

                content()
            }
        }
    }
}

private fun isSelectKey(keyCode: Int): Boolean {
    return keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
            keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
            keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
}
