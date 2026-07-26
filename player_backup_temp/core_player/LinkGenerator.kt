package com.kitsugi.animelist.core.player

import com.kitsugi.animelist.data.repository.StreamSource

/**
 * FP-30 – LinkGenerator interface
 * Eklentilerden stream linklerini çekmek ve sonraki/önceki bölümler arasında
 * geçiş yönetimi için üst düzey arayüz tanımlar.
 */
interface LinkGenerator {
    /** Sonraki bölümün oynatılabilir olup olmadığı */
    val hasNext: Boolean

    /** Önceki bölümün oynatılabilir olup olmadığı */
    val hasPrev: Boolean

    /**
     * Sonraki bölüm için en uygun stream kaynağını yükler ve döner.
     */
    suspend fun getNextEpisodeLink(): StreamSource?

    /**
     * Önceki bölüm için en uygun stream kaynağını yükler ve döner.
     */
    suspend fun getPrevEpisodeLink(): StreamSource?
}
