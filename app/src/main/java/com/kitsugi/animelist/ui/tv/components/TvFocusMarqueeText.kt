package com.kitsugi.animelist.ui.tv.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

private val MarqueeVelocity = 45.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvFocusMarqueeText(
    text: String,
    focused: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = if (focused) {
            modifier.basicMarquee(iterations = Int.MAX_VALUE, velocity = MarqueeVelocity)
        } else {
            modifier
        },
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = if (focused) TextOverflow.Clip else TextOverflow.Ellipsis,
        textAlign = textAlign,
    )
}
