package com.kitsugi.animelist.core.player.subtitle

import android.text.SpannableStringBuilder
import android.text.Spanned

/**
 * V2-E03 – CustomSrtParser
 *
 * SRT (SubRip) altyazı dosyası ayrıştırıcı.
 * Satır sonu varyantları ve kodlama sorunlarını tolere eder.
 * CloudStream CustomSubripParser referans alındı.
 */
object CustomSrtParser {

    private val SRT_BLOCK_PATTERN = Regex(
        """(\d+)\r?\n(\d{2}:\d{2}:\d{2},\d{3})\s*-->\s*(\d{2}:\d{2}:\d{2},\d{3})\r?\n([\s\S]*?)(?=\r?\n\r?\n|\z)""",
        RegexOption.MULTILINE
    )

    data class SrtEntry(
        val index: Int,
        val startMs: Long,
        val endMs: Long,
        val text: String
    )

    /**
     * Ham SRT metnini parse ederek entry listesi döner.
     */
    fun parse(raw: String): List<SrtEntry> {
        val normalized = raw.replace("\r\n", "\n").replace("\r", "\n").trim()
        val entries = mutableListOf<SrtEntry>()

        SRT_BLOCK_PATTERN.findAll(normalized).forEach { match ->
            try {
                val index = match.groupValues[1].toInt()
                val startMs = parseTimecode(match.groupValues[2])
                val endMs = parseTimecode(match.groupValues[3])
                val rawText = match.groupValues[4].trim()
                val cleanText = stripHtmlTags(rawText)
                entries.add(SrtEntry(index, startMs, endMs, cleanText))
            } catch (_: Exception) {
                // Bozuk bloğu atla
            }
        }

        return entries.sortedBy { it.startMs }
    }

    /**
     * "HH:MM:SS,mmm" → milisaniye
     */
    fun parseTimecode(tc: String): Long {
        val parts = tc.split(":", ",")
        if (parts.size < 4) return 0L
        val h = parts[0].toLongOrNull() ?: 0L
        val m = parts[1].toLongOrNull() ?: 0L
        val s = parts[2].toLongOrNull() ?: 0L
        val ms = parts[3].toLongOrNull() ?: 0L
        return h * 3_600_000L + m * 60_000L + s * 1_000L + ms
    }

    /**
     * Basit HTML tag temizleyici: <b>, <i>, <u>, <font> vb.
     */
    private val HTML_TAG_PATTERN = Regex("<[^>]+>")

    fun stripHtmlTags(text: String): String =
        HTML_TAG_PATTERN.replace(text, "").trim()

    /**
     * Milisaniyeyi "HH:MM:SS,mmm" formatına çevirir (yeniden yazma için).
     */
    fun formatTimecode(ms: Long): String {
        val h = ms / 3_600_000L
        val m = (ms % 3_600_000L) / 60_000L
        val s = (ms % 60_000L) / 1_000L
        val millis = ms % 1_000L
        return "%02d:%02d:%02d,%03d".format(h, m, s, millis)
    }

    /**
     * Entry listesini SRT formatına dönüştürür.
     */
    fun serialize(entries: List<SrtEntry>): String {
        return entries.joinToString("\n\n") { entry ->
            "${entry.index}\n${formatTimecode(entry.startMs)} --> ${formatTimecode(entry.endMs)}\n${entry.text}"
        }
    }

    /**
     * Tüm entrylere offset (ms) uygular.
     */
    fun applyOffset(entries: List<SrtEntry>, offsetMs: Long): List<SrtEntry> {
        return entries.map { entry ->
            entry.copy(
                startMs = (entry.startMs + offsetMs).coerceAtLeast(0L),
                endMs = (entry.endMs + offsetMs).coerceAtLeast(0L)
            )
        }
    }
}
