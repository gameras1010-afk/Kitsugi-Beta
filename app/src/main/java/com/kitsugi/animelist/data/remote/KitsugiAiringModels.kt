package com.kitsugi.animelist.data.remote

import java.util.Calendar

/**
 * Tek bir anime bölümünün yayın takvimi kaydı.
 * AniList `airingSchedules` endpoint'inden gelir.
 */
data class AiringEntry(
    val aniListId: Int,
    val malId: Int?,
    /** Romaji başlık (tercih edilen) */
    val title: String,
    val titleEnglish: String?,
    val titleNative: String?,
    val coverUrl: String?,
    /** Bu bölümün numarası (1-indexed) */
    val episode: Int,
    /** Unix epoch saniye cinsinden yayın zamanı */
    val airingAt: Long,
    /**
     * Haftanın günü: Calendar.MONDAY (2) … Calendar.SUNDAY (1).
     * Kullanıcının yerel saat dilimine göre hesaplanır.
     */
    val dayOfWeek: Int
) {
    /** Yayın saatini okunabilir "HH:mm" formatında döndürür. */
    fun formattedTime(): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = airingAt * 1000L
        }
        return String.format(
            "%02d:%02d",
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE)
        )
    }

    /** Bölümün yayınlanıp yayınlanmadığı (şu anki zamana göre). */
    fun hasAired(): Boolean = airingAt * 1000L < System.currentTimeMillis()

    fun toJikanSearchResult(): JikanSearchResult {
        return JikanSearchResult(
            malId = malId ?: aniListId,
            title = title,
            subtitle = titleEnglish ?: "",
            type = com.kitsugi.animelist.model.MediaType.Anime,
            total = null,
            score = null,
            isAdult = false,
            imageUrl = coverUrl,
            year = null,
            source = "anilist"
        )
    }
}
