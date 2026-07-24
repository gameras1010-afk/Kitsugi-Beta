package com.kitsugi.animelist.data.remote

/**
 * Galeri öğesi — görselin URL'si, kaynağı ve kategorisi.
 *
 * [source] → "Fanart.tv" | "TMDB" | "Jikan" | "Simkl" | "AniList" | "Kitsu"
 * [category] → [GalleryCategory] ile sınıflandırılır; galeri filtre sekmeleri için kullanılır.
 */
data class GalleryItem(
    val url: String,
    val source: String,
    val category: GalleryCategory = GalleryCategory.OTHER
)

enum class GalleryCategory(val label: String) {
    LOGO("Logo"),
    BACKDROP("Arka Plan"),
    POSTER("Poster"),
    CHARACTER("Karakter"),
    THUMBNAIL("Küçük Resim"),
    BANNER("Afiş"),
    OTHER("Diğer")
}
