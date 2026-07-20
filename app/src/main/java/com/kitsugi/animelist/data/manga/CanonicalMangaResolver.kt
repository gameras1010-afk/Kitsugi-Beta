package com.kitsugi.animelist.data.manga

import com.kitsugi.animelist.data.manga.model.CanonicalTitleSet
import java.text.Normalizer
import java.util.Locale

object CanonicalMangaResolver {

    private val stopWords = setOf(
        "manga", "manhwa", "manhua", "novel", "light", "webtoon",
        "oku", "read", "chapter", "chapters", "bolum", "bölüm", "bolumler", "bölümler",
        "turkce", "türkçe", "turkish", "english", "eng", "raw", "scan", "scans"
    )

    fun resolve(query: String): CanonicalTitleSet {
        val raw = query.trim()
        val cleaned = clean(raw)
        val ascii = ascii(cleaned)
        val compact = ascii.replace(" ", "")
        val core = removeStopWords(ascii)
        val tokens = (if (core.isNotBlank()) core else ascii)
            .split(' ')
            .map { it.trim() }
            .filter { it.length >= 2 }
            .toSet()

        val aliases = linkedSetOf<String>()
        aliases += raw.lowercase(Locale.ROOT)
        if (cleaned.isNotBlank()) aliases += cleaned
        if (ascii.isNotBlank()) aliases += ascii
        if (compact.isNotBlank()) aliases += compact
        if (core.isNotBlank()) aliases += core

        addVariantAliases(cleaned, aliases)
        addVariantAliases(ascii, aliases)

        return CanonicalTitleSet(
            raw = raw,
            cleaned = cleaned,
            ascii = ascii,
            compact = compact,
            core = core,
            aliases = aliases.filter { it.isNotBlank() }.toSet(),
            tokens = tokens,
        )
    }

    private fun addVariantAliases(value: String, target: MutableSet<String>) {
        if (value.isBlank()) return

        listOf(':', '-', '(', '[', '/').forEach { separator ->
            val part = value.substringBefore(separator).trim()
            if (part.length >= 3) target += part
        }

        val noNumbers = value.replace(Regex("\\b\\d+\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (noNumbers.length >= 3) target += noNumbers
    }

    private fun removeStopWords(value: String): String {
        return value.split(' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it !in stopWords }
            .joinToString(" ")
            .trim()
    }

    private fun clean(value: String): String {
        return value
            .trim()
            .lowercase(Locale.ROOT)
            .replace('’', '\'')
            .replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun ascii(value: String): String {
        val turkishSafe = value
            .replace("ç", "c")
            .replace("ğ", "g")
            .replace("ı", "i")
            .replace("ö", "o")
            .replace("ş", "s")
            .replace("ü", "u")

        return Normalizer.normalize(turkishSafe, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
