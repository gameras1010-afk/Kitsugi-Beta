package com.kitsugi.animelist.data.manga

import android.util.Log
import java.util.Locale

/**
 * Manga başlıklarının arama sorgusu ile benzerliğini ölçen eşleştirme motoru.
 * Alakasız sonuçların listelenmesini engellemek için kullanılır.
 */
object MangaTitleMatcher {

    private const val TAG = "MangaTitleMatcher"

    /**
     * Verilen arama sorgusu ile manga başlığı arasındaki benzerlik skorunu (0.0 - 1.25+) hesaplar.
     */
    // F10: Türkçe locale kullan — İ→i, Ş→ş gibi dönüşümler doğru yapılır
    private val TR_LOCALE = Locale("tr")

    fun getSimilarityScore(query: String, candidateName: String): Double {
        val q = query.lowercase(TR_LOCALE).trim()
        val c = candidateName.lowercase(TR_LOCALE).trim()

        if (q.isBlank() || c.isBlank()) return 0.0
        if (q == c) return 1.0

        val qSimple = simplifyTitle(q)
        val cSimple = simplifyTitle(c)
        if (qSimple == cSimple && qSimple.isNotEmpty()) return 1.0

        val qAscii = toAsciiTitle(q)
        val cAscii = toAsciiTitle(c)
        if (qAscii == cAscii && qAscii.isNotEmpty()) return 1.0

        val simNormal = getSimilarity(q, c)
        val simSimple = getSimilarity(qSimple, cSimple)
        val simAscii = getSimilarity(qAscii, cAscii)
        val maxSim = maxOf(simNormal, simSimple, simAscii)

        // Kelime örtüşme bonusu (Jaccard Index)
        val qWords = qAscii.split(Regex("\\s+")).filter { it.length >= 3 }.toSet()
        val cWords = cAscii.split(Regex("\\s+")).filter { it.length >= 3 }.toSet()
        var wordOverlapBonus = 0.0
        if (qWords.isNotEmpty() && cWords.isNotEmpty()) {
            val shared = qWords.intersect(cWords).size
            val union = (qWords + cWords).size
            if (union > 0) {
                wordOverlapBonus = (shared.toDouble() / union) * 0.25
            }
        }

        // İçerme bonusu (e.g. candidate contains query)
        val containmentBonus = if (cAscii.contains(qAscii) || qAscii.contains(cAscii)) {
            0.15
        } else {
            0.0
        }

        val finalScore = maxSim + wordOverlapBonus + containmentBonus
        Log.d(TAG, "Matching: Query='$query' Candidate='$candidateName' Score=$finalScore (sim=$maxSim overlap=$wordOverlapBonus cont=$containmentBonus)")
        return finalScore
    }

    private fun simplifyTitle(title: String): String {
        return title
            .replace(Regex("[^a-zA-Z0-9\\sçÇğĞıİöÖşŞüÜ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun toAsciiTitle(title: String): String {
        return title
            .replace("ç", "c").replace("Ç", "C")
            .replace("ğ", "g").replace("Ğ", "G")
            .replace("ı", "i").replace("İ", "I")
            .replace("ö", "o").replace("Ö", "O")
            .replace("ş", "s").replace("Ş", "S")
            .replace("ü", "u").replace("Ü", "U")
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun getSimilarity(s1: String, s2: String): Double {
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0 && len2 == 0) return 1.0
        if (len1 == 0 || len2 == 0) return 0.0

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        val maxLen = maxOf(len1, len2)
        return (maxLen - dp[len1][len2]).toDouble() / maxLen
    }
}
