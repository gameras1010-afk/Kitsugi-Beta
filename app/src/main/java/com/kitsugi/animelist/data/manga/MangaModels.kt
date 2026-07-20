package com.kitsugi.animelist.data.manga

/**
 * Bir manga başlığına (title) ait meta bilgileri taşır.
 * Eklenti tarafından döndürülür.
 */
data class MangaDetails(
    val url: String,
    val title: String,
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val genre: List<String> = emptyList(),
    val thumbnailUrl: String? = null,
    val status: MangaStatus = MangaStatus.Unknown,
    val source: String = ""
)

/**
 * Bir bölümün (chapter) temel bilgilerini taşır.
 */
data class MangaChapter(
    val url: String,
    val name: String,
    val chapterNumber: Float = -1f,
    val scanlator: String? = null,
    val uploadDate: Long = 0L,
    val mangaUrl: String = ""
)

/**
 * Bir bölümdeki tek bir sayfayı temsil eder.
 */
data class MangaPage(
    val index: Int,
    val url: String,
    var imageUrl: String? = null,
    var stream: (() -> java.io.InputStream)? = null,
    @Volatile var status: MangaPageStatus = MangaPageStatus.Queue
)

/**
 * Okuma modunu belirleyen enum.
 * Pager: Yatay/Dikey sayfalama (Manga, Manhua)
 * Webtoon: Sürekli dikey kaydırma (Manhwa, Webtoon)
 */
enum class ReadingMode {
    LeftToRight,   // Soldan Sağa (Manhua)
    RightToLeft,   // Sağdan Sola (Manga - Japonca)
    Vertical,      // Dikey Pager
    Webtoon        // Kesintisiz dikey kaydırma (Manhwa)
}

enum class ColorFilterType {
    Normal,
    Grayscale,
    Sepia,
    Invert
}

enum class MangaFitMode {
    FitScreen,
    FitWidth,
    FitHeight
}

enum class MangaStatus {
    Ongoing,
    Completed,
    Licensed,
    PublicationComplete,
    Cancelled,
    OnHiatus,
    Unknown
}

enum class MangaPageStatus {
    Queue,          // İndirilmeyi bekliyor
    LoadPage,       // Sayfa URL'si çözümleniyor
    DownloadImage,  // Resim indiriliyor
    Ready,          // Hazır, okunabilir
    Error           // Hata oluştu
}
