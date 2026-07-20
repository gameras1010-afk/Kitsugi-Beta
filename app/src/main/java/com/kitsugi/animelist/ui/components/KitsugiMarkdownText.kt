package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.utils.KitsugiMarkdownUtils.formatAniListMarkdown
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography

/**
 * A Composable that renders AniList / MAL markdown content properly.
 * It pre-processes AniList-specific syntax (spoilers, center, br tags, __ bold)
 * before passing the result to the full markdown renderer.
 */
@Composable
fun KitsugiMarkdownText(
    text: String?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
    lineHeight: TextUnit = LocalTextStyle.current.lineHeight,
) {
    val rendered = text?.formatAniListMarkdown().orEmpty()

    if (!rendered.contains("[[SPOILER:")) {
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
    } else {
        Column(modifier = modifier) {
            val parts = remember(rendered) { parseMarkdownSpoilers(rendered) }
            parts.forEach { part ->
                when (part) {
                    is SpoilerPart.Text -> {
                        if (part.content.isNotBlank()) {
                            Markdown(
                                content = part.content,
                                typography = markdownTypography(
                                    text = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = fontSize,
                                        lineHeight = lineHeight,
                                    )
                                )
                            )
                        }
                    }
                    is SpoilerPart.Spoiler -> {
                        InteractiveSpoilerBox(spoilerText = part.content)
                    }
                }
            }
        }
    }
}

private sealed interface SpoilerPart {
    data class Text(val content: String) : SpoilerPart
    data class Spoiler(val content: String) : SpoilerPart
}

private fun parseMarkdownSpoilers(input: String): List<SpoilerPart> {
    val result = mutableListOf<SpoilerPart>()
    var currentIndex = 0
    val startTag = "[[SPOILER:"
    val endTag = "]]"

    while (currentIndex < input.length) {
        val startIndex = input.indexOf(startTag, currentIndex)
        if (startIndex == -1) {
            result.add(SpoilerPart.Text(input.substring(currentIndex)))
            break
        }
        if (startIndex > currentIndex) {
            result.add(SpoilerPart.Text(input.substring(currentIndex, startIndex)))
        }
        val contentStart = startIndex + startTag.length
        val endIndex = input.indexOf(endTag, contentStart)
        if (endIndex == -1) {
            result.add(SpoilerPart.Spoiler(input.substring(contentStart)))
            break
        }
        result.add(SpoilerPart.Spoiler(input.substring(contentStart, endIndex)))
        currentIndex = endIndex + endTag.length
    }
    return result
}

@Composable
fun InteractiveSpoilerBox(
    spoilerText: String,
    modifier: Modifier = Modifier
) {
    var isRevealed by remember { mutableStateOf(false) }
    val accentColor = LocalKitsugiAccent.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .border(
                width = 1.dp,
                color = if (isRevealed) accentColor.copy(alpha = 0.5f) else KitsugiColors.AccentOrange.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { isRevealed = !isRevealed }
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isRevealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = if (isRevealed) accentColor else KitsugiColors.AccentOrange,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isRevealed) "Spoiler (Gizlemek için dokunun)" else "⚠️ Spoiler İçerik (Görmek için dokunun)",
                    color = if (isRevealed) accentColor else KitsugiColors.AccentOrange,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (isRevealed) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = spoilerText,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
