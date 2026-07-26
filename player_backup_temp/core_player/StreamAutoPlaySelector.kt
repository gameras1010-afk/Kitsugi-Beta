package com.kitsugi.animelist.core.player

import com.kitsugi.animelist.data.repository.StreamSource

/**
 * T1.8 – StreamAutoPlaySelector (Geliştirilmiş)
 *
 * Oynatılacak en uygun stream kaynağını otomatik seçer.
 *
 * Seçim önceliği (task dosyası tanımına uygun):
 * 1. Aynı addon / provider'dan gelen stream
 * 2. En yüksek kalite değerine sahip stream ([StreamSource.qualityValue])
 * 3. İlk mevcut stream (fallback)
 *
 * Kaynak: NuvioTV AutoPlaySelector + CloudStream StreamUtils
 */
object StreamAutoPlaySelector {

    /**
     * Otomatik oynatma için bir sonraki bölümün stream'leri arasından
     * en uygun kaynağı seçer.
     *
     * @param currentAddonName    Mevcut bölümde kullanılan addon adı
     * @param currentStreamSource Mevcut oynatılan [StreamSource] (ek fallback için)
     * @param nextEpisodeStreams   Sonraki bölüm için kullanılabilir stream listesi
     * @param preferHighQuality   true = addon eşleşmesi yoksa kalite önceliklendir
     */
    fun selectBestStream(
        currentAddonName: String?,
        currentStreamSource: StreamSource?,
        nextEpisodeStreams: List<StreamSource>,
        preferHighQuality: Boolean = true
    ): StreamSource? {
        if (nextEpisodeStreams.isEmpty()) return null

        // 1. Önce aynı addon adıyla eşleşen stream'i bul
        val targetAddon = currentAddonName ?: currentStreamSource?.addonName
        if (targetAddon != null) {
            val matchingAddon = nextEpisodeStreams.filter {
                it.addonName.equals(targetAddon, ignoreCase = true)
            }
            if (matchingAddon.isNotEmpty()) {
                // Aynı addon içinde kalitesi en yüksek olanı seç
                return if (preferHighQuality) {
                    matchingAddon.maxByOrNull { it.qualityValue ?: 0 }
                        ?: matchingAddon.first()
                } else {
                    matchingAddon.first()
                }
            }
        }

        // 2. Eşleşen addon yoksa en yüksek kaliteli stream'i seç
        if (preferHighQuality) {
            val byQuality = nextEpisodeStreams
                .filter { !it.url.isNullOrBlank() }
                .maxByOrNull { it.qualityValue ?: 0 }
            if (byQuality != null) return byQuality
        }

        // 3. Fallback: ilk mevcut stream
        return nextEpisodeStreams.firstOrNull()
    }
}

