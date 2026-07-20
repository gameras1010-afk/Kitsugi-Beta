package eu.kanade.tachiyomi.source

/**
 * Birden fazla kaynak döndüren eklentiler için fabrika arayüzü.
 * Bazı APK'lar tek bir sınıf içinde birden fazla dil/kaynak döndürebilir.
 */
interface SourceFactory {
    fun createSources(): List<Source>
}
