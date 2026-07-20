package com.kitsugi.animelist.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.TextUnit
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.kitsugi.animelist.utils.KitsugiMarkdownUtils.formatAniListMarkdown

/**
 * A Composable that renders AniList / MAL markdown content properly.
 * It pre-processes AniList-specific syntax (spoilers, center, br tags, __ bold)
 * before passing the result to the full markdown renderer.
 *
 * Use this everywhere you display synopsis, biography, review, or any
 * other user-created text from AniList/Jikan.
 */
@Composable
fun KitsugiMarkdownText(
    text: String?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
    lineHeight: TextUnit = LocalTextStyle.current.lineHeight,
) {
    val rendered = text?.formatAniListMarkdown().orEmpty()

    Markdown(
        content = rendered,
        typography = markdownTypography(
            text = MaterialTheme.typography.bodyMedium.copy(
                fontSize = fontSize,
                lineHeight = lineHeight,
            )
        ),
        modifier = modifier,
    )
}
