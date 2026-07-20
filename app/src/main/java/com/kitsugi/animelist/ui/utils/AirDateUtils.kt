package com.kitsugi.animelist.ui.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * V2-B02: AirDateUtils
 *
 * Yayın tarihlerini kullanıcı dostu Türkçe metin olarak formatlar.
 * NuvioTV AirDateUtils.kt referans alındı.
 */
object AirDateUtils {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val fullTrFormat = SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR"))
    private val shortTrFormat = SimpleDateFormat("d MMM yyyy", Locale("tr", "TR"))

    /**
     * ISO 8601 tarih string'ini kısa Türkçe formata çevirir.
     * "2024-04-06" → "6 Nis 2024"
     */
    fun formatShort(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "Bilinmiyor"
        return try {
            val date = isoFormat.parse(isoDate) ?: return isoDate
            shortTrFormat.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    /**
     * ISO 8601 tarih string'ini uzun Türkçe formata çevirir.
     * "2024-04-06" → "6 Nisan 2024"
     */
    fun formatFull(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "Bilinmiyor"
        return try {
            val date = isoFormat.parse(isoDate) ?: return isoDate
            fullTrFormat.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    /**
     * Kalan zamanı göreli olarak döner.
     * Gelecekteyse "X gün sonra", geçmişteyse "X gün önce"
     */
    fun formatRelative(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "Bilinmiyor"
        return try {
            val date = isoFormat.parse(isoDate) ?: return "Bilinmiyor"
            val now = Calendar.getInstance().time
            val diffMs = date.time - now.time
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()

            when {
                diffDays == 0  -> "Bugün"
                diffDays == 1  -> "Yarın"
                diffDays == -1 -> "Dün"
                diffDays > 1   -> "$diffDays gün sonra"
                diffDays < -1  -> "${-diffDays} gün önce"
                else           -> formatShort(isoDate)
            }
        } catch (e: Exception) {
            isoDate ?: "Bilinmiyor"
        }
    }

    /**
     * Unix timestamp'i kısa Türkçe tarih formatına çevirir.
     */
    fun fromUnix(timestamp: Long): String {
        val date = Date(timestamp * 1000L)
        return shortTrFormat.format(date)
    }

    /**
     * Sadece yılı döner. "2024-04-06" → "2024"
     */
    fun extractYear(isoDate: String?): String? {
        if (isoDate.isNullOrBlank()) return null
        return isoDate.take(4).takeIf { it.all { c -> c.isDigit() } }
    }

    /**
     * Sezon yayın bilgisini formatlı döner.
     * season=1, year=2024 → "1. Sezon · 2024"
     */
    fun formatSeason(season: Int?, year: Int?): String = buildString {
        if (season != null && season > 0) append("$season. Sezon")
        if (year != null && year > 0) {
            if (isNotEmpty()) append(" · ")
            append(year)
        }
    }
}
