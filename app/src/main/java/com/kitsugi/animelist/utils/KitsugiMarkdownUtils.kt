package com.kitsugi.animelist.utils

import com.kitsugi.animelist.ui.components.KitsugiMarkdownUriHandler
import java.net.URLEncoder

/**
 * AniList/MAL markdown ve BBCode temizleme + dönüştürme yardımcısı.
 *
 * Pipeline:
 *  1. Kullanıcı bio'sunu temizle (JSON payload, boş parantezler)
 *  2. BBCode → Markdown dönüştür
 *  3. AniList özel syntax'ları → standart/scheme Markdown
 *     - img(url)           → ![](url)  tıklanabilir galeri linki
 *     - ~!spoiler!~        → [Spoiler](kitsugi-spoiler://encoded)
 *     - ~~~center~~~       → düz metin
 *     - __bold__           → **bold**
 *     - <br>               → \n\n
 *     - ~~strikethrough~~  → ~~strikethrough~~ (zaten standart)
 */
object KitsugiMarkdownUtils {

    // ── Regex tanımları ──────────────────────────────────────────────────────
    private val spoilerRegex    = Regex("~!(.*?)!~", RegexOption.DOT_MATCHES_ALL)
    private val centerRegex     = Regex("~~~(.*?)~~~", RegexOption.DOT_MATCHES_ALL)
    private val boldUnderscoreRegex = Regex("__(.*?)__")
    private val htmlBrRegex     = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)

    /** AniList img syntax: img(url) veya img300%(url) vb. */
    private val aniListImgRegex = Regex("""img\d*%*\((.*?)\)""")

    /** Eski spoiler formatı: > ⚠️ *Spoiler:* ... */
    private val oldSpoilerRegex = Regex(""">\s*⚠️\s*\*?Spoiler:\*?\s*(.*)""", RegexOption.IGNORE_CASE)

    // Kullanıcı bio temizleme regex'leri
    private val jsonPayloadRegex    = Regex("""(?i)\bjson[A-Za-z0-9+/=_-]{8,}""")
    private val jsonBlockRegex      = Regex("""(?i)\bjson\s*\{.*?\}""", RegexOption.DOT_MATCHES_ALL)
    private val emptyParensRegex    = Regex("""\(\s*\)""")
    private val emptyBracketsRegex  = Regex("""\[\s*\]""")

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Kullanıcı profil/yorum metnini temizler:
     * extension JSON payload'larını ve boş parantezleri siler.
     */
    fun String.cleanUserAboutText(): String {
        if (isBlank()) return ""
        return this
            .replace(jsonPayloadRegex, "")
            .replace(jsonBlockRegex, "")
            .replace(emptyParensRegex, "")
            .replace(emptyBracketsRegex, "")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    /**
     * AniList/MAL metnini tam pipeline'dan geçirir ve
     * [KitsugiMarkdownText] için hazır standart Markdown üretir.
     *
     * Spoilerlar → `kitsugi-spoiler://` scheme linklere
     * Resimler   → `![](url)` + `kitsugi-image://` galeri linki
     */
    fun String.formatAniListMarkdown(): String {
        val cleaned = this.cleanUserAboutText()
        val bbConverted = cleaned.convertBBCodeToMarkdown()
        return bbConverted
            .replace(htmlBrRegex, "\n\n")
            .replace(centerRegex) { it.groupValues[1].trim() }
            .replace(boldUnderscoreRegex) { "**${it.groupValues[1]}**" }
            // AniList resim syntax → çift satır markdown resim + tıklanabilir galeri link
            .formatAniListImageTags()
            // Spoiler dönüşümü
            .replace(spoilerRegex) { match ->
                val raw     = match.groupValues[1]
                val cleaned2 = cleanSpoilerContent(raw)
                val encoded  = safeUrlEncode(cleaned2)
                "\n[⚠️ Spoiler - Görmek için tıkla](${KitsugiMarkdownUriHandler.SPOILER_SCHEME}$encoded)\n"
            }
            .replace(oldSpoilerRegex) { match ->
                val raw     = match.groupValues[1]
                val cleaned2 = cleanSpoilerContent(raw)
                val encoded  = safeUrlEncode(cleaned2)
                "\n[⚠️ Spoiler - Görmek için tıkla](${KitsugiMarkdownUriHandler.SPOILER_SCHEME}$encoded)\n"
            }
    }

    /**
     * Spoiler içeriğini temizler (URL decode gerekiyorsa yapar).
     */
    fun cleanSpoilerContent(raw: String): String {
        val trimmed = raw.trim()
        return runCatching {
            var s = trimmed
            if (s.contains("%") || s.contains("+")) {
                s = java.net.URLDecoder.decode(s.replace("+", " "), "UTF-8")
            }
            s
        }.getOrElse { trimmed }
    }

    // ── Private yardımcılar ───────────────────────────────────────────────────

    /**
     * AniList `img(url)` tag'larını standart Markdown resim + galeri linki çiftine çevirir.
     *
     * Çıktı örneği:
     * ```
     * [![](url)](kitsugi-image://0%7Curl)
     * ```
     * Bu sayede `Coil3ImageTransformerImpl` resmi gösterir,
     * `KitsugiMarkdownUriHandler` ise tıklamada galeriyi açar.
     */
    private fun String.formatAniListImageTags(): String {
        // Tüm resim URL'lerini topla — galeri için sıralı liste
        val allUrls = aniListImgRegex.findAll(this).map { it.groupValues[1].trim() }.toList()

        return replace(aniListImgRegex) { match ->
            val url = match.groupValues[1].trim()
            val idx = allUrls.indexOf(url).coerceAtLeast(0)
            // Pipe ile ayrılmış galeri payload: INDEX|URL1|URL2|...
            val galleryPayload = buildString {
                append(idx)
                allUrls.forEach { u -> append("|"); append(u) }
            }
            val encodedPayload = safeUrlEncode(galleryPayload)
            // Tıklanabilir resim: [![alt](url)](kitsugi-image://encoded)
            "\n[![](${url})](${KitsugiMarkdownUriHandler.IMAGE_SCHEME}${encodedPayload})\n"
        }
    }

    private fun safeUrlEncode(value: String): String = runCatching {
        URLEncoder.encode(value, "UTF-8")
    }.getOrElse { value }

    /**
     * MAL/forum BBCode'u standart Markdown'a çevirir.
     * Nested tag'ler için do-while döngüsü kullanır.
     */
    private fun String.convertBBCodeToMarkdown(): String {
        var result = this

        // Basit liste dönüşümleri
        result = result.replace("[list]", "").replace("[/list]", "")
        result = result.replace("[*]", "- ")

        val boldRegex          = Regex("\\[b\\](.*?)\\[/b\\]",          RegexOption.DOT_MATCHES_ALL)
        val italicRegex        = Regex("\\[i\\](.*?)\\[/i\\]",          RegexOption.DOT_MATCHES_ALL)
        val underlineRegex     = Regex("\\[u\\](.*?)\\[/u\\]",          RegexOption.DOT_MATCHES_ALL)
        val strikeRegex        = Regex("\\[s\\](.*?)\\[/s\\]",          RegexOption.DOT_MATCHES_ALL)
        val centerRegexBB      = Regex("\\[center\\](.*?)\\[/center\\]", RegexOption.DOT_MATCHES_ALL)
        val sizeRegex          = Regex("\\[size=[^\\]]*\\](.*?)\\[/size\\]", RegexOption.DOT_MATCHES_ALL)
        val colorRegex         = Regex("\\[color=[^\\]]*\\](.*?)\\[/color\\]", RegexOption.DOT_MATCHES_ALL)
        val spoilerSimpleRegex = Regex("\\[spoiler\\](.*?)\\[/spoiler\\]", RegexOption.DOT_MATCHES_ALL)
        val spoilerParamRegex  = Regex("\\[spoiler=[^\\]]*\\](.*?)\\[/spoiler\\]", RegexOption.DOT_MATCHES_ALL)
        val quoteRegex         = Regex("\\[quote\\](.*?)\\[/quote\\]",  RegexOption.DOT_MATCHES_ALL)
        val urlParamRegex      = Regex("\\[url=(.*?)\\](.*?)\\[/url\\]", RegexOption.DOT_MATCHES_ALL)
        val urlSimpleRegex     = Regex("\\[url\\](.*?)\\[/url\\]",      RegexOption.DOT_MATCHES_ALL)
        val imgBBRegex         = Regex("\\[img[^\\]]*\\](.*?)\\[/img\\]", RegexOption.DOT_MATCHES_ALL)

        var previous: String
        do {
            previous = result
            result = result
                .replace(boldRegex)          { "**${it.groupValues[1]}**" }
                .replace(italicRegex)        { "*${it.groupValues[1]}*" }
                .replace(underlineRegex)     { "__${it.groupValues[1]}__" }
                .replace(strikeRegex)        { "~~${it.groupValues[1]}~~" }
                .replace(centerRegexBB)      { it.groupValues[1] }
                .replace(sizeRegex)          { it.groupValues[1] }
                .replace(colorRegex)         { it.groupValues[1] }
                .replace(spoilerSimpleRegex) { "~!${it.groupValues[1]}!~" }
                .replace(spoilerParamRegex)  { "~!${it.groupValues[1]}!~" }
                .replace(quoteRegex)         { "\n> ${it.groupValues[1].replace("\n", "\n> ")}\n" }
                .replace(urlParamRegex)      { "[${it.groupValues[2]}](${it.groupValues[1]})" }
                .replace(urlSimpleRegex)     { "[${it.groupValues[1]}](${it.groupValues[1]})" }
                .replace(imgBBRegex)         { "img(${it.groupValues[1]})" } // → AniList img format'ına çevir, sonra formatAniListImageTags işler
        } while (result != previous)

        return result
    }
}
