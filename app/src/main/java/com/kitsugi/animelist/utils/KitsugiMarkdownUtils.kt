package com.kitsugi.animelist.utils

import java.net.URLEncoder

object KitsugiMarkdownUtils {

    private val spoilerRegex = Regex("~!(.*?)!~", RegexOption.DOT_MATCHES_ALL)
    private val centerRegex = Regex("~~~(.*?)~~~", RegexOption.DOT_MATCHES_ALL)
    private val boldUnderscoreRegex = Regex("__(.*?)__")
    private val htmlBrRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)

    /**
     * Converts AniList-flavored markdown and MAL BBCode to standard Markdown that
     * multiplatform-markdown-renderer can properly render.
     *
     * Handles:
     *  - ~!spoiler!~  → [View spoiler](anihyouspoiler://encoded)
     *  - ~~~center~~~  → center (removes centering markers)
     *  - __bold__  → **bold** (standardize to ** syntax)
     *  - <br> / <br /> → \n\n (proper line breaks)
     *  - [link](url) → passed through (already standard)
     *  - BBCode tags ([b], [i], [u], [url], [spoiler], etc.) → converted to markdown
     */
    fun String.formatAniListMarkdown(): String {
        val withBBCodeConverted = this.convertBBCodeToMarkdown()
        return withBBCodeConverted
            .replace(htmlBrRegex, "\n\n")
            .replace(centerRegex) { it.groupValues[1].trim() }
            .replace(boldUnderscoreRegex) { "**${it.groupValues[1]}**" }
            .replace(spoilerRegex) { matchResult ->
                val content = matchResult.groupValues[1]
                val encoded = runCatching { URLEncoder.encode(content.trim(), "UTF-8") }
                    .getOrElse { content }
                "\n> ⚠️ *Spoiler:* $encoded\n"
            }
    }

    private fun String.convertBBCodeToMarkdown(): String {
        var result = this

        // Convert list and list items
        result = result.replace("[list]", "").replace("[/list]", "")
        result = result.replace("[*]", "- ")

        // RegExes for nested/simple tags
        val boldRegex = Regex("\\[b\\](.*?)\\[/b\\]", RegexOption.DOT_MATCHES_ALL)
        val italicRegex = Regex("\\[i\\](.*?)\\[/i\\]", RegexOption.DOT_MATCHES_ALL)
        val underlineRegex = Regex("\\[u\\](.*?)\\[/u\\]", RegexOption.DOT_MATCHES_ALL)
        val centerRegexBB = Regex("\\[center\\](.*?)\\[/center\\]", RegexOption.DOT_MATCHES_ALL)
        val sizeRegex = Regex("\\[size=[^\\]]*\\](.*?)\\[/size\\]", RegexOption.DOT_MATCHES_ALL)
        val colorRegex = Regex("\\[color=[^\\]]*\\](.*?)\\[/color\\]", RegexOption.DOT_MATCHES_ALL)
        val spoilerRegexSimple = Regex("\\[spoiler\\](.*?)\\[/spoiler\\]", RegexOption.DOT_MATCHES_ALL)
        val spoilerRegexParam = Regex("\\[spoiler=[^\\]]*\\](.*?)\\[/spoiler\\]", RegexOption.DOT_MATCHES_ALL)
        val quoteRegex = Regex("\\[quote\\](.*?)\\[/quote\\]", RegexOption.DOT_MATCHES_ALL)
        val urlRegexParam = Regex("\\[url=(.*?)\\](.*?)\\[/url\\]", RegexOption.DOT_MATCHES_ALL)
        val urlRegexSimple = Regex("\\[url\\](.*?)\\[/url\\]", RegexOption.DOT_MATCHES_ALL)
        val imgRegex = Regex("\\[img[^\\]]*\\](.*?)\\[/img\\]", RegexOption.DOT_MATCHES_ALL)

        var previous: String
        do {
            previous = result
            result = result
                .replace(boldRegex) { "**${it.groupValues[1]}**" }
                .replace(italicRegex) { "*${it.groupValues[1]}*" }
                .replace(underlineRegex) { "__${it.groupValues[1]}__" }
                .replace(centerRegexBB) { it.groupValues[1] }
                .replace(sizeRegex) { it.groupValues[1] }
                .replace(colorRegex) { it.groupValues[1] }
                .replace(spoilerRegexSimple) { "~!${it.groupValues[1]}!~" }
                .replace(spoilerRegexParam) { "~!${it.groupValues[1]}!~" }
                .replace(quoteRegex) { "\n> ${it.groupValues[1].replace("\n", "\n> ")}\n" }
                .replace(urlRegexParam) { "[${it.groupValues[2]}](${it.groupValues[1]})" }
                .replace(urlRegexSimple) { "[${it.groupValues[1]}](${it.groupValues[1]})" }
                .replace(imgRegex) { "![](${it.groupValues[1]})" }
        } while (result != previous)

        return result
    }
}
