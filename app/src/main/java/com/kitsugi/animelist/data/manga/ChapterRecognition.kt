package com.kitsugi.animelist.data.manga

/**
 * Bir bölümün ADINDAN doğru bölüm NUMARASINI ayıklar.
 *
 * Mihon/Tachiyomi'nin `ChapterRecognition` mantığından birebir port edilmiştir.
 * Çoğu HTML manga kaynağı `chapter_number` alanını -1 döndürür; bu durumda
 * bölüm adından ("Vol.1 Ch. 4: Misrepresentation" → 4) numara çıkarılır.
 *
 * Ayrıca alt-bölümleri de doğru handle eder:
 *   - "12.5" → 12.5
 *   - extra → x.99, omake → x.98, special → x.97
 *   - "x.a" → x.1, "x.b" → x.2 ...
 *
 * -R> = regex conversion (örnek dönüşüm).
 */
object ChapterRecognition {

    private const val NUMBER_PATTERN = """([0-9]+)(\.[0-9]+)?(\.?[a-z]+)?"""

    /**
     * "Ch.xx" durumları:
     * Mokushiroku Alice Vol.1 Ch. 4: Misrepresentation -R> 4
     */
    private val basic = Regex("""(?<=ch\.) *$NUMBER_PATTERN""")

    /**
     * Örnek: Bleach 567: Down With Snowwhite -R> 567
     */
    private val number = Regex(NUMBER_PATTERN)

    /**
     * İstenmeyen etiketleri temizler.
     * Örnek: Prison School 12 v.1 vol004 version1243 volume64 -R> Prison School 12
     */
    private val unwanted = Regex("""\b(?:v|ver|vol|version|volume|season|s)[^a-z]?[0-9]+""")

    /**
     * İstenmeyen boşlukları temizler.
     * Örnek: One Piece 12 special -R> One Piece 12special
     */
    private val unwantedWhiteSpace = Regex("""\s(?=extra|special|omake)""")

    fun parseChapterNumber(
        mangaTitle: String,
        chapterName: String,
        chapterNumber: Double? = null,
    ): Double {
        // Bölüm numarası zaten biliniyorsa döndür.
        if (chapterNumber != null && (chapterNumber == -2.0 || chapterNumber > -1.0)) {
            return chapterNumber
        }

        // Bölüm başlığını küçük harfe çevir.
        val cleanChapterName = chapterName.lowercase()
            // Manga başlığını bölüm başlığından çıkar.
            .replace(mangaTitle.lowercase(), "").trim()
            // Virgül ve tireleri noktaya çevir.
            .replace(',', '.')
            .replace('-', '.')
            // İstenmeyen boşlukları kaldır.
            .replace(unwantedWhiteSpace, "")

        val numberMatch = number.findAll(cleanChapterName)

        when {
            numberMatch.none() -> {
                return chapterNumber ?: -1.0
            }
            numberMatch.count() > 1 -> {
                // İstenmeyen etiketleri kaldır.
                unwanted.replace(cleanChapterName, "").let { name ->
                    // Temel durum ch.xx kontrolü.
                    basic.find(name)?.let { return getChapterNumberFromMatch(it) }

                    // İlk numara silinmiş olabilir, tekrar ara.
                    number.find(name)?.let { return getChapterNumberFromMatch(it) }
                }
            }
        }

        // İlk bulunan numarayı döndür.
        return getChapterNumberFromMatch(numberMatch.first())
    }

    private fun getChapterNumberFromMatch(match: MatchResult): Double {
        return match.let {
            val initial = it.groups[1]?.value?.toDouble()!!
            val subChapterDecimal = it.groups[2]?.value
            val subChapterAlpha = it.groups[3]?.value
            val addition = checkForDecimal(subChapterDecimal, subChapterAlpha)
            initial.plus(addition)
        }
    }

    private fun checkForDecimal(decimal: String?, alpha: String?): Double {
        if (!decimal.isNullOrEmpty()) {
            return decimal.toDouble()
        }

        if (!alpha.isNullOrEmpty()) {
            if (alpha.contains("extra")) {
                return 0.99
            }

            if (alpha.contains("omake")) {
                return 0.98
            }

            if (alpha.contains("special")) {
                return 0.97
            }

            val trimmedAlpha = alpha.trimStart('.')
            if (trimmedAlpha.length == 1) {
                return parseAlphaPostFix(trimmedAlpha[0])
            }
        }

        return 0.0
    }

    /**
     * x.a -> x.1, x.b -> x.2, vb.
     */
    private fun parseAlphaPostFix(alpha: Char): Double {
        val number = alpha.code - ('a'.code - 1)
        if (number >= 10) return 0.0
        return number / 10.0
    }
}
