package com.kitsugi.animelist.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * V2-A01: AutoResizeText
 *
 * Metni içinde bulunduğu alana sığdırmak için yazı tipi boyutunu otomatik
 * olarak küçülten Composable. NuvioTV AutoResizeText.kt referans alındı.
 *
 * Kullanım:
 *   AutoResizeText(
 *       text = "Çok uzun bir başlık metni",
 *       fontSizeRange = FontSizeRange(min = 10.sp, max = 18.sp),
 *       maxLines = 2
 *   )
 */
data class FontSizeRange(
    val min: TextUnit = 10.sp,
    val max: TextUnit = 18.sp,
    val step: TextUnit = 1.sp
) {
    init {
        require(min < max) { "AutoResizeText: min ($min) must be < max ($max)" }
        require(step.value > 0) { "AutoResizeText: step must be > 0" }
    }
}

@Composable
fun AutoResizeText(
    text: String,
    fontSizeRange: FontSizeRange = FontSizeRange(),
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = LocalTextStyle.current
) {
    var fontSizeValue by remember(text, fontSizeRange) {
        mutableStateOf(fontSizeRange.max.value)
    }
    var readyToDraw by remember(text, fontSizeRange) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        maxLines = maxLines,
        fontWeight = fontWeight,
        textAlign = textAlign,
        overflow = overflow,
        style = style.copy(fontSize = fontSizeValue.sp),
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        onTextLayout = { result ->
            if (result.didOverflowHeight && fontSizeValue > fontSizeRange.min.value) {
                val nextSize = fontSizeValue - fontSizeRange.step.value
                fontSizeValue = nextSize.coerceAtLeast(fontSizeRange.min.value)
                readyToDraw = false
            } else {
                readyToDraw = true
            }
        }
    )
}
