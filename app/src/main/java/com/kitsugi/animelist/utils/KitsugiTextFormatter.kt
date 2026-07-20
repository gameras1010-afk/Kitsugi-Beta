package com.kitsugi.animelist.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.kitsugi.animelist.ui.theme.KitsugiColors
import java.util.regex.Pattern

object KitsugiTextFormatter {

    fun formatMarkdown(text: String?, accentColor: androidx.compose.ui.graphics.Color): AnnotatedString {
        if (text.isNullOrBlank()) return AnnotatedString("")

        val normalizedText = text
            .replace("<br>", "\n")
            .replace("<br />", "\n")
            .replace("<br/>", "\n")

        val intervals = mutableListOf<MatchInterval>()

        // 1. Find links
        val linkMatcher = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)").matcher(normalizedText)
        while (linkMatcher.find()) {
            intervals.add(
                MatchInterval(
                    linkMatcher.start(),
                    linkMatcher.end(),
                    "LINK",
                    linkMatcher.group(1) ?: "",
                    linkMatcher.group(2) ?: ""
                )
            )
        }

        // 2. Find spoilers
        val spoilerMatcher = Pattern.compile("~!(.*?)!~").matcher(normalizedText)
        while (spoilerMatcher.find()) {
            intervals.add(
                MatchInterval(
                    spoilerMatcher.start(),
                    spoilerMatcher.end(),
                    "SPOILER",
                    spoilerMatcher.group(1) ?: ""
                )
            )
        }

        // 3. Find bold
        val boldMatcher = Pattern.compile("(__|\\*\\*)(.*?)\\1").matcher(normalizedText)
        while (boldMatcher.find()) {
            intervals.add(
                MatchInterval(
                    boldMatcher.start(),
                    boldMatcher.end(),
                    "BOLD",
                    boldMatcher.group(2) ?: ""
                )
            )
        }

        // 4. Find italic
        val italicMatcher = Pattern.compile("(_|\\*)(.*?)\\1").matcher(normalizedText)
        while (italicMatcher.find()) {
            intervals.add(
                MatchInterval(
                    italicMatcher.start(),
                    italicMatcher.end(),
                    "ITALIC",
                    italicMatcher.group(2) ?: ""
                )
            )
        }

        // Sort intervals by start position.
        intervals.sortBy { it.start }

        // Filter out overlapping intervals (keep the first one that starts)
        val activeIntervals = mutableListOf<MatchInterval>()
        var lastEnd = 0
        for (interval in intervals) {
            if (interval.start >= lastEnd) {
                activeIntervals.add(interval)
                lastEnd = interval.end
            }
        }

        // Build the AnnotatedString
        return buildAnnotatedString {
            var currentIdx = 0
            for (interval in activeIntervals) {
                // Append text before interval
                if (interval.start > currentIdx) {
                    append(normalizedText.substring(currentIdx, interval.start))
                }

                // Append styled interval content
                val startPos = length
                when (interval.type) {
                    "LINK" -> {
                        append(interval.group1) // Display the label
                        addStyle(
                            style = SpanStyle(
                                color = accentColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Bold
                            ),
                            start = startPos,
                            end = length
                        )
                        addStringAnnotation(
                            tag = "URL",
                            annotation = interval.group2, // The URL itself
                            start = startPos,
                            end = length
                        )
                    }
                    "SPOILER" -> {
                        append(interval.group1)
                        addStyle(
                            style = SpanStyle(
                                color = KitsugiColors.AccentRed,
                                fontWeight = FontWeight.SemiBold
                            ),
                            start = startPos,
                            end = length
                        )
                    }
                    "BOLD" -> {
                        append(interval.group1)
                        addStyle(
                            style = SpanStyle(fontWeight = FontWeight.Bold),
                            start = startPos,
                            end = length
                        )
                    }
                    "ITALIC" -> {
                        append(interval.group1)
                        addStyle(
                            style = SpanStyle(fontStyle = FontStyle.Italic),
                            start = startPos,
                            end = length
                        )
                    }
                }
                currentIdx = interval.end
            }

            // Append remaining text
            if (currentIdx < normalizedText.length) {
                append(normalizedText.substring(currentIdx))
            }
        }
    }

    private class MatchInterval(
        val start: Int,
        val end: Int,
        val type: String,
        val group1: String,
        val group2: String = ""
    )
}
