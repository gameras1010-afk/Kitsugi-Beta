package com.kitsugi.animelist.utils

import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.data.remote.JikanSearchResult
import com.kitsugi.animelist.data.remote.KitsugiRelation
import com.kitsugi.animelist.data.remote.KitsugiCharacterMediaAppearance
import com.kitsugi.animelist.data.remote.KitsugiStaffMediaWork

object PreferenceHelpers {
    fun getDisplayTitle(
        title: String,
        titleEnglish: String?,
        titleJapanese: String?,
        titleLanguage: String
    ): String {
        return when (titleLanguage) {
            "ENGLISH" -> titleEnglish?.takeIf { it.isNotBlank() } ?: title
            "NATIVE", "JAPANESE_STAFF" -> titleJapanese?.takeIf { it.isNotBlank() } ?: title
            else -> title // ROMAJI or default
        }
    }

    fun MediaEntry.getDisplayTitle(titleLanguage: String): String {
        return PreferenceHelpers.getDisplayTitle(title, titleEnglish, titleJapanese, titleLanguage)
    }

    fun JikanSearchResult.getDisplayTitle(titleLanguage: String): String {
        return PreferenceHelpers.getDisplayTitle(title, titleEnglish, titleJapanese, titleLanguage)
    }

    fun KitsugiRelation.getDisplayTitle(titleLanguage: String): String {
        return PreferenceHelpers.getDisplayTitle(title, titleEnglish, titleJapanese, titleLanguage)
    }

    fun KitsugiCharacterMediaAppearance.getDisplayTitle(titleLanguage: String): String {
        return PreferenceHelpers.getDisplayTitle(title, titleEnglish, titleJapanese, titleLanguage)
    }

    fun KitsugiStaffMediaWork.getDisplayTitle(titleLanguage: String): String {
        return PreferenceHelpers.getDisplayTitle(mediaTitle, titleEnglish, titleJapanese, titleLanguage)
    }

    fun formatScore(score: Int?, scoreFormat: String, hideScores: Boolean): String {
        if (hideScores) return "-"
        if (score == null || score == 0) return "unrated"
        return when (scoreFormat) {
            "POINT_100" -> "${score * 10}"
            "POINT_10_DECIMAL" -> "${score}.0/10"
            "POINT_5" -> {
                val fullStars = score / 2
                val hasHalf = (score % 2) != 0
                val emptyStars = 5 - fullStars - (if (hasHalf) 1 else 0)
                buildString {
                    repeat(fullStars) { append("★") }
                    if (hasHalf) append("½")
                    repeat(emptyStars) { append("☆") }
                }
            }
            "POINT_3" -> {
                when {
                    score >= 8 -> "😊"
                    score >= 5 -> "😐"
                    else -> "🙁"
                }
            }
            "STARS" -> {
                buildString {
                    repeat(score) { append("★") }
                    repeat(10 - score) { append("☆") }
                }
            }
            else -> "$score/10"
        }
    }

    fun MediaEntry.getDisplayScore(scoreFormat: String, hideScores: Boolean): String {
        return formatScore(score, scoreFormat, hideScores)
    }

    fun JikanSearchResult.getDisplayScore(scoreFormat: String, hideScores: Boolean): String {
        return formatScore(score, scoreFormat, hideScores)
    }
}
